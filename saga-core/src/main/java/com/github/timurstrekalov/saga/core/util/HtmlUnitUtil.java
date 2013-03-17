package com.github.timurstrekalov.saga.core.util;

import java.util.logging.Level;

import org.apache.commons.logging.LogFactory;

public final class HtmlUnitUtil {

    private HtmlUnitUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Make HtmlUnit's logger shut up for good.
     */
    public static void silenceHtmlUnitLogging() {
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
    }

}
