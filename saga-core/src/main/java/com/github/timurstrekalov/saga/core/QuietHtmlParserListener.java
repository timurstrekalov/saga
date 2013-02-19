package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.html.HTMLParserListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

class QuietHtmlParserListener implements HTMLParserListener {

    private static final Logger logger = LoggerFactory.getLogger(QuietHtmlParserListener.class);

    @Deprecated
    public void error(final String message, final URL url, final int line, final int column, final String key) {
        this.error(message, url, null, line, column, key);
    }

    @Deprecated
    public void warning(final String message, final URL url, final int line, final int column, final String key) {
    	this.warning(message, url, null, line, column, key);
    }

    @Override
    public void error(final String message, final URL url, final String html, final int line, final int column, final String key) {
        logger.debug(message);
    }

    @Override
    public void warning(final String message, final URL url, final String html, final int line, final int column, final String key) {
        logger.debug(message);
    }

}
