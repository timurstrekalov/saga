package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

class SagaWebClient extends ThreadLocal<WebClient> {

    private static final IncorrectnessListener quietIncorrectnessListener = new QuietIncorrectnessListener();
    private static final JavaScriptErrorListener loggingJsErrorListener = new QuietJavaScriptErrorListener();
    private static final HTMLParserListener quietHtmlParserListener = new QuietHtmlParserListener();
    private static final SilentCssErrorHandler quietCssErrorHandler = new SilentCssErrorHandler();

    @Override
    protected WebClient initialValue() {
        final WebClient client = new WebClient(BrowserVersion.FIREFOX_3_6);

        client.setIncorrectnessListener(quietIncorrectnessListener);
        client.setJavaScriptErrorListener(loggingJsErrorListener);
        client.setHTMLParserListener(quietHtmlParserListener);
        client.setCssErrorHandler(quietCssErrorHandler);

        client.setJavaScriptEnabled(true);
        client.setAjaxController(new NicelyResynchronizingAjaxController());
        client.setThrowExceptionOnScriptError(false);
        client.setThrowExceptionOnFailingStatusCode(false);
        client.setPrintContentOnFailingStatusCode(false);

        return client;
    }

}
