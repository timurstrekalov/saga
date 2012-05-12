package com.github.timurstrekalov.saga.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.STMessage;

final class LoggingStringTemplateErrorListener implements STErrorListener {

    private static final Logger logger = LoggerFactory.getLogger(LoggingStringTemplateErrorListener.class);
    
    @Override
    public void compileTimeError(final STMessage msg) {
        logger.error(msg.toString());
    }

    @Override
    public void runTimeError(final STMessage msg) {
        logger.error(msg.toString());
    }

    @Override
    public void IOError(final STMessage msg) {
        logger.error(msg.toString());
    }

    @Override
    public void internalError(final STMessage msg) {
        logger.error(msg.toString());
    }
    
}
