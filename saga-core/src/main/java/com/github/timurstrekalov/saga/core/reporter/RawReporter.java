package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.RunStats;

import java.io.File;
import java.io.IOException;

public class RawReporter extends AbstractStringTemplateBasedReporter {

    public RawReporter() {
        super(ReportFormat.RAW);
    }

    @Override
    protected void writeReportThreadSafe(final File outputFile, final RunStats runStats) throws IOException {
        stringTemplateGroup.getInstanceOf("runStatsRaw")
                .add("stats", runStats)
                .write(outputFile, listener);
    }

}
