package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.RunStats;

import java.io.File;
import java.io.IOException;

public class CsvReporter extends AbstractStringTemplateBasedReporter {

    public CsvReporter() {
        super(ReportFormat.CSV);
    }

    @Override
    protected void writeReportThreadSafe(final File outputFile, final RunStats runStats) throws IOException {
        stringTemplateGroup.getInstanceOf("runStatsCsv")
                .add("stats", runStats)
                .write(outputFile, listener);
    }

}
