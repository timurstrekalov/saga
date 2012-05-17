package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.ScriptException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.JavaScriptErrorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;

class QuietJavaScriptErrorListener implements JavaScriptErrorListener {

    private static final Logger logger = LoggerFactory.getLogger(QuietJavaScriptErrorListener.class);

    @Override
    public void scriptException(final HtmlPage htmlPage, final ScriptException e) {
        logger.error("Script exception on page {}, message: {}", htmlPage.getUrl(), e.getMessage());
        logger.debug(e.getMessage(), e);
    }

    @Override
    public void timeoutError(final HtmlPage htmlPage, final long allowedTime, final long executionTime) {
        logger.error("Timeout error on page {}. Allowed time: {}, execution time: {}",
                new Object[] { htmlPage.getUrl(), allowedTime, executionTime });
    }

    @Override
    public void malformedScriptURL(final HtmlPage htmlPage, final String url, final MalformedURLException e) {
        logger.error("Malformed script URL on page {}. URL: {}, message: {}",
                new Object[] { htmlPage.getUrl(), url, e.getMessage()});

        logger.debug(e.getMessage(), e);
    }

    @Override
    public void loadScriptError(final HtmlPage htmlPage, final URL scriptUrl, final Exception e) {
        logger.warn("Error loading script referenced on page {}. Script URL: {}, message: {}",
                new Object[] { htmlPage.getUrl(), scriptUrl, e.getMessage()});

        logger.debug(e.getMessage(), e);
    }

}
