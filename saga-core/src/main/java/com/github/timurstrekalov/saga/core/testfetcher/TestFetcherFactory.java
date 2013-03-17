package com.github.timurstrekalov.saga.core.testfetcher;

import java.net.URI;

import com.github.timurstrekalov.saga.core.util.UriUtil;

public final class TestFetcherFactory {

    private TestFetcherFactory() {
        throw new UnsupportedOperationException("Factory class");
    }

    public static TestFetcher newInstance(final URI baseDir) {
        if (UriUtil.isFileUri(baseDir)) {
            return new FileSystemTestFetcher();
        }

        return new SingleHttpPageTestFetcher();
    }

}
