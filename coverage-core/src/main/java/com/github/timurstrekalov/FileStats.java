package com.github.timurstrekalov;

import java.util.List;

class FileStats {

    public final String name;
    public final String href;

    public final int statements;
    public final int executed;
    public final int coverage;

    public final List<LineCoverageRecord> lineCoverageRecords;

    FileStats(
            final String name,
            final String href,
            final int statements,
            final int executed,
            final int coverage,
            final List<LineCoverageRecord> lineCoverageRecords) {

        this.name = name;
        this.href = href;

        this.statements = statements;
        this.executed = executed;
        this.coverage = coverage;

        this.lineCoverageRecords = lineCoverageRecords;
    }

}
