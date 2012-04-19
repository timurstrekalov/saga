package com.github.timurstrekalov.saga.core;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

class RunStats implements Iterable<FileStats> {

    public final File test;
    public final String title;

    private final Map<String, FileStats> fileStatsMap = Maps.newTreeMap();

    RunStats(final File test) {
        this(test, String.format("Coverage report for \"%s\"", test.getAbsolutePath()));
    }

    RunStats(final File test, final String title) {
        this.test = test;
        this.title = title;
    }

    String getReportName() {
        return test.getName() + "-report.html";
    }

    void add(final FileStats newStats) {
        final String key = newStats.fullName;
        final FileStats oldStats = fileStatsMap.get(key);

        if (oldStats != null) {
            fileStatsMap.put(key, FileStats.merge(newStats, oldStats));
        } else {
            fileStatsMap.put(key, newStats);
        }
    }

    public Collection<FileStats> getFileStats() {
        return fileStatsMap.values();
    }

    public int getTotalStatements() {
        return Util.sum(Collections2.transform(fileStatsMap.values(), new Function<FileStats, Integer>() {
            @Override
            public Integer apply(final FileStats input) {
                return input.getStatements();
            }
        }));
    }

    public int getTotalExecuted() {
        return Util.sum(Collections2.transform(fileStatsMap.values(), new Function<FileStats, Integer>() {
            @Override
            public Integer apply(final FileStats input) {
                return input.getExecuted();
            }
        }));
    }

    public int getTotalCoverage() {
        return Util.toCoverage(getTotalStatements(), getTotalExecuted());
    }

    @Override
    public Iterator<FileStats> iterator() {
        return getFileStats().iterator();
    }
}
