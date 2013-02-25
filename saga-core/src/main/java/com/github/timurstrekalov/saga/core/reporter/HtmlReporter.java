package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.RunStats;

import java.io.File;
import java.io.IOException;

public class HtmlReporter extends AbstractStringTemplateBasedReporter {

    public HtmlReporter() {
        super(ReportFormat.HTML);
    }

    @Override
    protected void writeReportInternal(final File outputFile, final RunStats runStats) throws IOException {
        stringTemplateGroup.getInstanceOf("runStats")
                .add("stats", runStats)
                .add("name", config.getProperty("app.name"))
                .add("version", config.getProperty("app.version"))
                .add("url", config.getProperty("app.url"))
                .write(outputFile, listener);
    }

}
