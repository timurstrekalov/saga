package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.cfg.ReporterConfig;
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
    public final void writeReport(final ReporterConfig reporterConfig, final TestRunCoverageStatistics runStats) throws IOException {
        final File fileOutputDir = ReporterUtil.getFileOutputDir(reporterConfig.getBaseUri(), reporterConfig.getOutputDir(), runStats);

        FileUtils.mkdir(fileOutputDir.getAbsolutePath());

        final File outputFile = new File(fileOutputDir, getReportName(reporterConfig, runStats));

        logger.info("Writing {} coverage report: {}", format.name(), outputFile.getAbsoluteFile());

        writeReportInternal(outputFile, runStats);
    }

    protected abstract void writeReportInternal(File outputFile, TestRunCoverageStatistics runStats) throws IOException;

    /**
     * Obtain report file name based on configuration and runner type.
     * If raw runner is used and raw name specified use configuration to compose
     * file name.
     * Otherwise use default way to compose file name based on running test name
     *
     * @param reporterConfig - configuration that specified in maven mojo
     * @param runStats       - statistics obtained from test processing
     * @return - filename with ${name}-${suffix}.${extension} | %s-%s.%s format
     */
    private String getReportName(final ReporterConfig reporterConfig, final TestRunCoverageStatistics runStats) {
        String result = String.format("%s-%s.%s", runStats.getTestName(), format.getSuffix(), format.getExtension());
        if (this instanceof RawReporter && reporterConfig.getRawName() != null) {
            result = String.format("%s-%s.%s", reporterConfig.getRawName(), format.getSuffix(), format.getExtension());
        }
        return result;
    }

}
