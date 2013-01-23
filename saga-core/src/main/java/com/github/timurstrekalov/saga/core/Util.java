package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Function;

import java.io.File;
import java.net.URI;

class Util {

    private static final int[] red = {219, 75, 75};
    private static final int[] yellow = {219, 219, 75};
    private static final int[] green = {174, 219, 75};

    static int toCoverage(final int totalStatements, final int totalExecuted) {
        return (int) ((double) totalExecuted / totalStatements * 100);
    }

    static <T> int sum(final Iterable<T> objects, final Function<T, Integer> transformer) {
        int sum = 0;

        for (final T t : objects) {
            sum += transformer.apply(t);
        }

        return sum;
    }

    static String getColor(final int coverage) {
        final int[] from = coverage < 50 ? red : yellow;
        final int[] to = coverage < 50 ? yellow : green;
        final double prc = (coverage - (coverage < 50 ? 0 : 50)) / 50.0;

        final int[] color = new int[3];
        for (int i = 0; i < 3; i++) {
            color[i] = (int) ((to[i] - from[i]) * prc + from[i]);
        }

        // TODO use join
        return String.format("%d, %d, %d", color[0], color[1], color[2]);
    }

    static String getFullSourcePath(final HtmlPage htmlPage, final String sourceName) {
        try {
            final URI sourceUri = URI.create(sourceName.replaceAll(" ", "%20"));

            if (sourceUri.isAbsolute()) {
                return new File(URI.create(sourceUri.getScheme() + "://" + sourceUri.getPath())).getAbsolutePath();
            }

            final URI uri = htmlPage.getUrl().toURI();
            final URI pageUriWithoutQuery = URI.create(uri.getScheme() + "://" + uri.getPath());

            return new File(new File(pageUriWithoutQuery).getParent(), URI.create(sourceName).getPath()).getAbsolutePath();
        } catch (final Exception e) {
            throw new RuntimeException("Error getting full path for " + sourceName + " at " + htmlPage.getUrl(), e);
        }
    }

}
