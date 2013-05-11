package com.github.timurstrekalov.saga.core.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpHeaderValues;
import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlets.ProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Most of the code here is copied directly from ProxyServlet, since it doesn't provide the hooks we need.
public final class InstrumentingProxyServlet extends ProxyServlet {

    public static final String INSTRUMENTER = InstrumentingProxyServlet.class.getName() + ".instrumenter";

    private static final Logger logger = LoggerFactory.getLogger(InstrumentingProxyServlet.class);

    private ScriptInstrumenter instrumenter;

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        this.instrumenter = (ScriptInstrumenter) config.getServletContext().getAttribute(INSTRUMENTER);
    }

    @Override
    public void service(final ServletRequest req, final ServletResponse res) throws ServletException, IOException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;

        if ("CONNECT".equalsIgnoreCase(request.getMethod())) {
            handleConnect(request, response);
            return;
        }

        final InputStream in = request.getInputStream();
        final Continuation continuation = ContinuationSupport.getContinuation(request);

        if (!continuation.isInitial()) {
            response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT); // Need better test that isInitial
            return;
        }

        String uri = request.getRequestURI();
        if (request.getQueryString() != null) {
            uri += "?" + request.getQueryString();
        }

        final HttpURI url = proxyHttpURI(request, uri);

        logger.debug("proxy {}-->{}", uri, url);

        if (url == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        final HttpExchange exchange = new CustomHttpExchange(continuation, response, request);

        exchange.setScheme(HttpSchemes.HTTPS.equals(request.getScheme()) ? HttpSchemes.HTTPS_BUFFER : HttpSchemes.HTTP_BUFFER);
        exchange.setMethod(request.getMethod());
        exchange.setURL(url.toString());
        exchange.setVersion(request.getProtocol());

        logger.debug("{} {} {}", request.getMethod(), url, request.getProtocol());

        // check connection header
        String connectionHdr = request.getHeader("Connection");
        if (connectionHdr != null) {
            connectionHdr = connectionHdr.toLowerCase(Locale.ENGLISH);
            if (!connectionHdr.contains("keep-alive") && !connectionHdr.contains("close")) {
                connectionHdr = null;
            }
        }

        // force host
        if (_hostHeader != null) {
            exchange.setRequestHeader("Host", _hostHeader);
        }

        // copy headers
        boolean xForwardedFor = false;
        boolean hasContent = false;
        long contentLength = -1;
        Enumeration<?> enm = request.getHeaderNames();
        while (enm.hasMoreElements()) {
            // TODO could be better than this!
            String hdr = (String) enm.nextElement();
            String lhdr = hdr.toLowerCase(Locale.ENGLISH);

            if ("transfer-encoding".equals(lhdr)) {
                if (request.getHeader("transfer-encoding").contains("chunk")) {
                    hasContent = true;
                }
            }

            if (_DontProxyHeaders.contains(lhdr)) {
                continue;
            }
            if (connectionHdr != null && connectionHdr.contains(lhdr)) {
                continue;
            }
            if (_hostHeader != null && "host".equals(lhdr)) {
                continue;
            }

            if ("content-type".equals(lhdr)) {
                hasContent = true;
            } else if ("content-length".equals(lhdr)) {
                contentLength = request.getContentLength();
                exchange.setRequestHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength));
                if (contentLength > 0) {
                    hasContent = true;
                }
            } else if ("x-forwarded-for".equals(lhdr)) {
                xForwardedFor = true;
            }

            Enumeration<?> vals = request.getHeaders(hdr);
            while (vals.hasMoreElements()) {
                String val = (String) vals.nextElement();
                if (val != null) {
                    logger.debug("{}: {}", hdr, val);

                    exchange.setRequestHeader(hdr, val);
                }
            }
        }

        // Proxy headers
        exchange.setRequestHeader("Via", "1.1 (jetty)");
        if (!xForwardedFor) {
            exchange.addRequestHeader("X-Forwarded-For", request.getRemoteAddr());
            exchange.addRequestHeader("X-Forwarded-Proto", request.getScheme());
            exchange.addRequestHeader("X-Forwarded-Host", request.getHeader("Host"));
            exchange.addRequestHeader("X-Forwarded-Server", request.getLocalName());
        }

        if (hasContent) {
            exchange.setRequestContentSource(in);
        }

        customizeExchange(exchange, request);

        /*
         * we need to set the timeout on the continuation to take into
         * account the timeout of the HttpClient and the HttpExchange
         */
        long ctimeout = (_client.getTimeout() > exchange.getTimeout()) ? _client.getTimeout() : exchange.getTimeout();

        // continuation fudge factor of 1000, underlying components
        // should fail/expire first from exchange
        if (ctimeout == 0) {
            continuation.setTimeout(0);  // ideally never times out
        } else {
            continuation.setTimeout(ctimeout + 1000);
        }

        customizeContinuation(continuation);

        continuation.suspend(response);
        _client.send(exchange);
    }

    private static InputStream newInputStreamForResponse(
            final HttpServletResponse response, final byte[] remoteResponseBody) throws IOException {
        final InputStream result = new ByteArrayInputStream(remoteResponseBody);

        if (shouldBeGzipEncoded(response)) {
            return new GZIPInputStream(result);
        }

        return result;
    }

    private static boolean shouldBeGzipEncoded(final HttpServletResponse response) {
        return "gzip".equalsIgnoreCase(response.getHeader(HttpHeaders.CONTENT_ENCODING));
    }

    private final class CustomHttpExchange extends HttpExchange {

        private final Continuation continuation;
        private final HttpServletResponse response;
        private final HttpServletRequest request;

        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        public CustomHttpExchange(
                final Continuation continuation,
                final HttpServletResponse response,
                final HttpServletRequest request) {
            this.continuation = continuation;
            this.response = response;
            this.request = request;
        }

        @Override
        protected void onResponseComplete() throws IOException {
            final byte[] in;
            final byte[] remoteResponseBody = baos.toByteArray();

            if (shouldBeInstrumented(request, response)) {
                final Charset charset = getCharsetFrom(response);
                final String body = CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
                    @Override
                    public InputStream getInput() throws IOException {
                        return newInputStreamForResponse(response, remoteResponseBody);
                    }
                }, charset));

                final String uri;

                if (request instanceof Request) {
                    uri = ((Request) request).getUri().toString();
                } else {
                    uri = request.getRequestURI();
                }

                final String instrumentedString = instrumenter.instrument(body, uri, 1);
                final byte[] instrumentedBytes = instrumentedString.getBytes(charset);

                if (shouldBeGzipEncoded(response)) {
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);

                    ByteStreams.write(instrumentedBytes, newOutputSupplier(gzipOutputStream));

                    gzipOutputStream.flush();
                    gzipOutputStream.close();

                    in = baos.toByteArray();
                } else {
                    in = instrumentedBytes;
                }
            } else {
                in = remoteResponseBody;
            }

            if (response.getHeader(HttpHeaders.CONTENT_LENGTH) != null) {
                response.setContentLength(in.length);
            }

            try {
                ByteStreams.write(in, newOutputSupplier(response.getOutputStream()));

                response.getOutputStream().flush();
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }

            logger.debug("complete");
            continuation.complete();
        }

        private OutputSupplier<? extends OutputStream> newOutputSupplier(final OutputStream outputStream) {
            return new OutputSupplier<OutputStream>() {
                @Override
                public OutputStream getOutput() throws IOException {
                    return outputStream;
                }
            };
        }

        @Override
        protected void onResponseContent(final Buffer content) throws IOException {
            logger.debug("content {}", content.length());
            content.writeTo(baos);
        }

        @Override
        protected void onResponseStatus(final Buffer version, final int status, final Buffer reason) throws IOException {
            logger.debug("{}, {}, {}", version, status, reason);

            if (reason != null && reason.length() > 0) {
                response.setStatus(status, reason.toString());
            } else {
                response.setStatus(status);
            }
        }

        @Override
        protected void onResponseHeader(final Buffer name, final Buffer value) throws IOException {
            final String nameString = name.toString();
            final String s = nameString.toLowerCase(Locale.ENGLISH);

            if (!_DontProxyHeaders.contains(s) || (HttpHeaders.CONNECTION_BUFFER.equals(name) && HttpHeaderValues.CLOSE_BUFFER.equals(value))) {
                logger.debug("{}: {}", name, value);

                final String filteredHeaderValue = filterResponseHeaderValue(nameString, value.toString(), request);
                if (filteredHeaderValue != null && filteredHeaderValue.trim().length() > 0) {
                    logger.debug("{}: (filtered): {}", name, filteredHeaderValue);
                    if ("content-type".equals(s)) {
                        response.setHeader(nameString, filteredHeaderValue);
                    } else {
                        response.addHeader(nameString, filteredHeaderValue);
                    }
                }
            } else {
                logger.debug("{} ! {}", name, value);
            }
        }

        @Override
        protected void onConnectionFailed(final Throwable ex) {
            handleOnConnectionFailed(ex, request, response);

            // it is possible this might trigger before the
            // continuation.suspend()
            if (!continuation.isInitial()) {
                continuation.complete();
            }
        }

        @Override
        protected void onException(final Throwable ex) {
            handleOnException(ex, request, response);

            // it is possible this might trigger before the
            // continuation.suspend()
            if (!continuation.isInitial()) {
                continuation.complete();
            }
        }

        @Override
        protected void onExpire() {
            handleOnExpire(request, response);
            continuation.complete();
        }

        private boolean shouldBeInstrumented(final HttpServletRequest request, final HttpServletResponse response) {
            return response.getStatus() == HttpServletResponse.SC_OK && request.getRequestURI().toLowerCase(Locale.ENGLISH).endsWith(".js");
        }

        private Charset getCharsetFrom(final HttpServletResponse response) {
            final String characterEncoding = response.getCharacterEncoding();
            return characterEncoding != null ? Charset.forName(characterEncoding) : Charset.forName("UTF-8");
        }

    }
}
