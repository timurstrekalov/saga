package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

import java.io.IOException;

class SagaWebClient extends ThreadLocal<WebClient> {

    private static final IncorrectnessListener quietIncorrectnessListener = new QuietIncorrectnessListener();
    private static final JavaScriptErrorListener loggingJsErrorListener = new QuietJavaScriptErrorListener();
    private static final HTMLParserListener quietHtmlParserListener = new QuietHtmlParserListener();
    private static final SilentCssErrorHandler quietCssErrorHandler = new SilentCssErrorHandler();

    @Override
    protected WebClient initialValue() {
        final WebClient client = new WebClient(BrowserVersion.FIREFOX_3_6) {
            @Override
            public WebResponse loadWebResponse(final WebRequest webRequest) throws IOException {
                return new WebResponseProxy(super.loadWebResponse(webRequest));
            }
        };

        client.setIncorrectnessListener(quietIncorrectnessListener);
        client.setJavaScriptErrorListener(loggingJsErrorListener);
        client.setHTMLParserListener(quietHtmlParserListener);
        client.setCssErrorHandler(quietCssErrorHandler);

        client.getOptions().setJavaScriptEnabled(true);
        client.setAjaxController(new NicelyResynchronizingAjaxController());
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);
        client.getOptions().setPrintContentOnFailingStatusCode(false);
        client.setWebConnection(new HttpWebConnection(client) {
            @Override
            protected WebResponse newWebResponseInstance(final WebResponseData responseData, final long loadTime,
                                                         final WebRequest request) {
                return new WebResponseProxy(super.newWebResponseInstance(responseData, loadTime, request));
            }
        });

        return client;
    }

}
