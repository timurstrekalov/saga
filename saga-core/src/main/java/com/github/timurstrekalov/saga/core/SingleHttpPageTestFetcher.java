package com.github.timurstrekalov.saga.core;

import java.net.URI;
import java.util.List;

import com.google.common.collect.ImmutableList;

class SingleHttpPageTestFetcher implements TestFetcher {

    @Override
    public List<URI> fetch(final URI baseUri, final String includes, final String excludes) {
        return ImmutableList.of(baseUri);
    }

}
