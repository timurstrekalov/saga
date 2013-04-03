package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractReporter implements Reporter {

    protected static final Properties config;

    static {
        try {
            config = new Properties();
            config.load(AbstractStringTemplateBasedReporter.class.getResourceAsStream("/app.properties"));
        } catch (final IOException e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    protected final ReportFormat format;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public AbstractReporter(final ReportFormat format) {
        this.format = format;
    }

    @Override
    public final void writeReport(final Config mojoConfig, final TestRunCoverageStatistics runStats) throws IOException {
        final File fileOutputDir = ReporterUtil.getFileOutputDir(mojoConfig.getBaseUri(), mojoConfig.getOutputDir(), runStats);

        FileUtils.mkdir(fileOutputDir.getAbsolutePath());

        final File outputFile = new File(fileOutputDir, getReportName(mojoConfig));

        logger.info("Writing {} coverage report: {}", format.name(), outputFile.getAbsoluteFile());

        writeReportInternal(outputFile, runStats);
    }

    protected abstract void writeReportInternal(File outputFile, TestRunCoverageStatistics runStats) throws IOException;

    private String getReportName(final Config runStats) {
        return String.format("%s-%s.%s", runStats.getRawName(), format.getSuffix(), format.getExtension());
    }

}
