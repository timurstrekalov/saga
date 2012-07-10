package com.github.timurstrekalov.saga.core;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.*;

class RunStats implements Iterable<FileStats> {

    public static final RunStats EMPTY = new RunStats(null, null);

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

    String getRawReportName() {
        return test.getName() + "-coverage.dat";
    }

    void add(final FileStats newStats) {
        final String key = newStats.getFullName();
        final FileStats oldStats = fileStatsMap.get(key);

        if (oldStats != null) {
            fileStatsMap.put(key, FileStats.merge(newStats, oldStats));
        } else {
            fileStatsMap.put(key, newStats);
        }
    }

    public Collection<FileStats> getFileStats() {
        final List<FileStats> result = new ArrayList<FileStats>(fileStatsMap.values());
        
        Collections.sort(result, new Comparator<FileStats>() {
            @Override
            public int compare(final FileStats s1, final FileStats s2) {
                return Integer.valueOf(s1.getCoverage()).compareTo(s2.getCoverage());
            }
        });

        return result;
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

    public boolean getHasStatements() {
        return getTotalStatements() > 0;
    }

    public String getBarColor() {
        return Util.getColor(getTotalCoverage());
    }

    @Override
    public Iterator<FileStats> iterator() {
        return getFileStats().iterator();
    }

}
