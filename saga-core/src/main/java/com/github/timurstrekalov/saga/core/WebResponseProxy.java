package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import org.apache.http.HttpStatus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

// TODO fix HtmlUnit's HtmlPage.getContentAsStream() to throw IOException rather than catch it and throw RuntimeExceptions when it's not necessary
class WebResponseProxy extends WebResponse {

    private final WebResponse delegate;
    private int statusCode = -1;

    public WebResponseProxy(final WebResponse delegate) {
        super(null, null, -1);
        this.delegate = delegate;

        try {
            delegate.getContentAsStream();
        } catch (final Exception e) {
            if (e.getCause() instanceof FileNotFoundException) {
                if (getStatusCode() == HttpStatus.SC_OK) {
                    statusCode = HttpStatus.SC_NOT_FOUND;
                }
            } else {
                if (getStatusCode() == HttpStatus.SC_OK) {
                    statusCode = HttpStatus.SC_INTERNAL_SERVER_ERROR;
                }
            }
        }
    }

//    @Deprecated
//    public WebRequest getRequestSettings() {
//        //return delegate.getRequestSettings();
//        return delegate.getWebRequest();
//    }

    @Override
    public WebRequest getWebRequest() {
        return delegate.getWebRequest();
    }

    @Override
    public List<NameValuePair> getResponseHeaders() {
        return delegate.getResponseHeaders();
    }

    @Override
    public String getResponseHeaderValue(String headerName) {
        return delegate.getResponseHeaderValue(headerName);
    }

    @Override
    public int getStatusCode() {
        if (statusCode != -1) {
            return statusCode;
        }

        return delegate.getStatusCode();
    }

    @Override
    public String getStatusMessage() {
        return delegate.getStatusMessage();
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public String getContentCharsetOrNull() {
        return delegate.getContentCharsetOrNull();
    }

    @Override
    public String getContentCharset() {
        return delegate.getContentCharset();
    }

    @Override
    public String getContentAsString() {
        return delegate.getContentAsString();
    }

    @Override
    public String getContentAsString(String encoding) {
        return delegate.getContentAsString(encoding);
    }

    @Override
    public InputStream getContentAsStream() throws IOException {
        return delegate.getContentAsStream();
    }


//    @Deprecated
//    public byte[] getContentAsBytes() throws UnsupportedEncodingException {
//        //return delegate.getContentAsBytes();
//        return this.getContentAsString().getBytes();
//    }

    @Override
    public long getLoadTime() {
        return delegate.getLoadTime();
    }

}
