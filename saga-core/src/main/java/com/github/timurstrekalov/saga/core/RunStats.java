package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RunStats implements Iterable<FileStats> {

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

    public String getTestName() {
        return test.getName();
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

    public List<FileStats> getFileStats() {
        final List<FileStats> result = Lists.newLinkedList(fileStatsMap.values());
        
        Collections.sort(result, new Comparator<FileStats>() {
            @Override
            public int compare(final FileStats s1, final FileStats s2) {
                return Integer.valueOf(s1.getCoverage()).compareTo(s2.getCoverage());
            }
        });

        return result;
    }

    public Collection<FileStats> getFileStatsWithSeparateFileOnly() {
        return Collections2.filter(getFileStats(), new Predicate<FileStats>() {
            @Override
            public boolean apply(final FileStats stats) {
                return stats.isSeparateFile();
            }
        });
    }

    public int getTotalStatements() {
        return Util.sum(fileStatsMap.values(), new Function<FileStats, Integer>() {

            @Override
            public Integer apply(final FileStats input) {
                return input.getStatements();
            }
        });
    }

    public int getTotalExecuted() {
        return Util.sum(fileStatsMap.values(), new Function<FileStats, Integer>() {
            @Override
            public Integer apply(final FileStats input) {
                return input.getExecuted();
            }
        });
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

    public int getBarColorAsArgb() {
        return Util.getColorAsArgb(getTotalCoverage());
    }

    @Override
    public Iterator<FileStats> iterator() {
        return getFileStats().iterator();
    }

}
