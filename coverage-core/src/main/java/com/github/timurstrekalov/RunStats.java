package com.github.timurstrekalov;

import com.google.common.collect.Lists;

import java.util.List;

class RunStats {

    public final String runName;

    private final List<FileStats> fileStats = Lists.newLinkedList();

    private int totalStatements;
    private int totalExecuted;

    RunStats(final String runName) {
        this.runName = runName;
    }

    public FileStats add(
            final String jsFileName,
            final String href,
            final int statements,
            final int executed,
            final List<LineCoverageRecord> lineCoverageRecords) {

        final FileStats stats = new FileStats(jsFileName, href, statements, executed, toCoverage(statements, executed),
                lineCoverageRecords);

        fileStats.add(stats);

        totalStatements += stats.statements;
        totalExecuted += stats.executed;

        return stats;
    }

    public List<FileStats> getFileStats() {
        return fileStats;
    }

    public int getTotalStatements() {
        return totalStatements;
    }

    public int getTotalExecuted() {
        return totalExecuted;
    }

    public int getTotalCoverage() {
        return toCoverage(totalStatements, totalExecuted);
    }

    private static int toCoverage(final int totalStatements, final int totalExecuted) {
        return (int) ((double) totalExecuted / totalStatements * 100);
    }

}
