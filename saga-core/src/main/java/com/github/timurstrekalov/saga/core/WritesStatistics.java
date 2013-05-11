package com.github.timurstrekalov.saga.core;

import java.io.IOException;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import com.github.timurstrekalov.saga.core.reporter.ReporterFactory;

public final class WritesStatistics {

    public void write(final Config config, final TestRunCoverageStatistics stats) throws IOException {
        for (final ReportFormat reportFormat : config.getReportFormats()) {
            ReporterFactory.reporterFor(reportFormat).writeReport(config.getBaseUri(), config.getOutputDir(), stats);
        }
    }

}
