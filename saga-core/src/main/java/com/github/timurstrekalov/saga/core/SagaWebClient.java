package com.github.timurstrekalov.saga.core;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.HttpWebConnection;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.WebResponseData;
import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;

class SagaWebClient extends ThreadLocal<WebClient> {

    private static final IncorrectnessListener quietIncorrectnessListener = new QuietIncorrectnessListener();
    private static final JavaScriptErrorListener loggingJsErrorListener = new QuietJavaScriptErrorListener();
    private static final HTMLParserListener quietHtmlParserListener = new QuietHtmlParserListener();
    private static final SilentCssErrorHandler quietCssErrorHandler = new SilentCssErrorHandler();

    private BrowserVersion browserVersion = BrowserVersion.FIREFOX_3_6;

    @Override
    protected WebClient initialValue() {
        @SuppressWarnings("serial")
        final WebClient client = new WebClient(this.browserVersion) {
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

    public void setBrowserVersion(BrowserVersion browserVersion){
        this.browserVersion = browserVersion;
    }
}
