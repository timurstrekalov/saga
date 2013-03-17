package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.IOException;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;

public class HtmlReporter extends AbstractStringTemplateBasedReporter {

    public HtmlReporter() {
        super(ReportFormat.HTML);
    }

    @Override
    protected void writeReportThreadSafe(final File outputFile, final TestRunCoverageStatistics runStats) throws IOException {
        stringTemplateGroup.getInstanceOf("runStats")
                .add("stats", runStats)
                .add("name", config.getProperty("app.name"))
                .add("version", config.getProperty("app.version"))
                .add("url", config.getProperty("app.url"))
                .write(outputFile, listener);
    }

}
