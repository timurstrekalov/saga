package com.github.timurstrekalov;

import com.google.common.collect.Lists;

import java.util.List;

class PerRunStatistics {

    public final String testName;

    private final List<PerFileStatistics> fileStats = Lists.newLinkedList();

    private int totalStatements;
    private int totalExecuted;

    PerRunStatistics(final String testName) {
        this.testName = testName;
    }

    public PerFileStatistics add(final String jsFileName, final String href, final int statements, final int executed) {
        final PerFileStatistics stats = new PerFileStatistics(jsFileName, href, statements, executed,
                toCoverage(statements, executed));

        fileStats.add(stats);

        totalStatements += stats.statements;
        totalExecuted += stats.executed;

        return stats;
    }

    public List<PerFileStatistics> getFileStats() {
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
