package com.github.timurstrekalov.saga.core;

import java.net.URI;

class TestFetcherFactory {

    private TestFetcherFactory() {
        throw new UnsupportedOperationException("Utility class");
    }

    static TestFetcher newInstance(final URI baseDir) {
        if (UriUtil.isFileUri(baseDir)) {
            return new FileSystemTestFetcher();
        }

        return new SingleHttpPageTestFetcher();
    }

}
