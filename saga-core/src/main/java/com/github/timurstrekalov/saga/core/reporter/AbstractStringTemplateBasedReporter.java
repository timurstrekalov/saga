package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.RunStats;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.misc.STMessage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

abstract class AbstractStringTemplateBasedReporter implements Reporter {

    protected static final LoggingStringTemplateErrorListener listener = new LoggingStringTemplateErrorListener();
    protected static final STGroup stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');
    protected static final Properties config;

    static {
        try {
            config = new Properties();
            config.load(AbstractStringTemplateBasedReporter.class.getResourceAsStream("/app.properties"));
        } catch (final IOException e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    private static final Object lock = new Object();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ReportFormat format;

    public AbstractStringTemplateBasedReporter(final ReportFormat format) {
        this.format = format;
    }

    protected File getFileOutputDir(final File baseDir, final File outputDir, final RunStats runStats) {
        final URI relativeTestUri = baseDir.toURI().relativize(runStats.test.toURI());
        return new File(new File(outputDir.toURI().resolve(relativeTestUri)).getParent());
    }

    @Override
    public final void writeReport(final File baseDir, final File outputDir, final RunStats runStats) throws IOException {
        final File fileOutputDir = getFileOutputDir(baseDir, outputDir, runStats);

        FileUtils.mkdir(fileOutputDir.getAbsolutePath());

        final File outputFile = new File(fileOutputDir, getReportName(runStats));

        logger.info("Writing {} coverage report: {}", format.name(), outputFile.getAbsoluteFile());

        synchronized (lock) {
            writeReportInternal(outputFile, runStats);
        }
    }

    private String getReportName(final RunStats runStats) {
        return String.format("%s-%s.%s", runStats.getTestName(), format.getSuffix(), format.getExtension());
    }

    protected abstract void writeReportInternal(final File outputFile, final RunStats runStats) throws IOException;

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
