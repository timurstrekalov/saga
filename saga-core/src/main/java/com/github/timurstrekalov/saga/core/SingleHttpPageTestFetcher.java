package com.github.timurstrekalov.saga.core;

import java.net.URI;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SingleHttpPageTestFetcher implements TestFetcher {

    private static final Logger logger = LoggerFactory.getLogger(SingleHttpPageTestFetcher.class);

    @Override
    public List<URI> fetch(final URI baseUri, final String includes, final String excludes) {
        if (StringUtils.isNotBlank(includes) || StringUtils.isNotBlank(excludes)) {
            logger.warn("Including and excluding tests by patterns only makes sense in the context of tests run off the filesystem, ignoring");
        }

        return ImmutableList.of(baseUri);
    }

}
