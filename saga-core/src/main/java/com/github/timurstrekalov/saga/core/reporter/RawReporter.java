package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.LineCoverageRecord;
import com.github.timurstrekalov.saga.core.model.ScriptCoverageStatistics;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;

import java.io.File;
import java.io.IOException;

public class RawReporter extends AbstractStringTemplateBasedReporter {

    public RawReporter() {
        super(ReportFormat.RAW);
    }

    @Override
    protected void writeReportThreadSafe(final File outputFile, final TestRunCoverageStatistics runStats) throws IOException {
        stringTemplateGroup.getInstanceOf("runStatsRaw")
                .add("stats", runStats)
                .write(outputFile, listener);
    }

}
