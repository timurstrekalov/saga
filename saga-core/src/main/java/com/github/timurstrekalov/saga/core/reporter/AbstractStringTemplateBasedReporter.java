package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.IOException;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.RunStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.misc.STMessage;

abstract class AbstractStringTemplateBasedReporter extends AbstractReporter {

    protected static final LoggingStringTemplateErrorListener listener = new LoggingStringTemplateErrorListener();
    protected static final STGroup stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');

    private static final Object lock = new Object();

    public AbstractStringTemplateBasedReporter(final ReportFormat format) {
        super(format);
    }

    @Override
    protected final void writeReportInternal(final File outputFile, final RunStats runStats) throws IOException {
        synchronized (lock) {
            writeReportThreadSafe(outputFile, runStats);
        }
    }

    protected abstract void writeReportThreadSafe(File outputFile, RunStats runStats) throws IOException;

    private static final class LoggingStringTemplateErrorListener implements STErrorListener {

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

}
