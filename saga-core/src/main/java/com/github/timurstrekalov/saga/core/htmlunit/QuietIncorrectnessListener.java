package com.github.timurstrekalov.saga.core.htmlunit;

import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class QuietIncorrectnessListener implements IncorrectnessListener {

    private static final Logger logger = LoggerFactory.getLogger(QuietIncorrectnessListener.class);

    @Override
    public void notify(final String message, final Object origin) {
        logger.debug(message);
    }

}
