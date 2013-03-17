package com.github.timurstrekalov.saga.core;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.github.timurstrekalov.saga.core.model.FileStats;
import com.github.timurstrekalov.saga.core.util.MiscUtil;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public final class RunStats implements Iterable<FileStats> {

    public static final RunStats EMPTY = new RunStats(null, null);

    public final URI test;
    public final String title;

    private SortBy sortBy = SortBy.COVERAGE;
    private Order order = Order.ASC;

    private final Map<URI, FileStats> fileStatsMap = Maps.newTreeMap();

    public RunStats(final URI test) {
        this(test, String.format("Coverage report for \"%s\"", test));
    }

    public RunStats(final URI test, final String title) {
        this.test = test;
        this.title = title;
    }

    public String getTestName() {
        return UriUtil.getLastSegmentOrHost(test);
    }

    public void add(final FileStats newStats) {
        final URI key = newStats.getFileUri();
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
                return (getOrder() == Order.ASC ? 1 : -1) * getSortBy().compare(s1, s2);
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
        return MiscUtil.sum(fileStatsMap.values(), new Function<FileStats, Integer>() {

            @Override
            public Integer apply(final FileStats input) {
                return input.getStatements();
            }
        });
    }

    public int getTotalExecuted() {
        return MiscUtil.sum(fileStatsMap.values(), new Function<FileStats, Integer>() {

            @Override
            public Integer apply(final FileStats input) {
                return input.getExecuted();
            }
        });
    }

    public int getTotalCoverage() {
        return MiscUtil.toCoverage(getTotalStatements(), getTotalExecuted());
    }

    public boolean getHasStatements() {
        return getTotalStatements() > 0;
    }

    public String getBarColor() {
        return MiscUtil.getColor(getTotalCoverage());
    }

    public int getBarColorAsArgb() {
        return MiscUtil.getColorAsArgb(getTotalCoverage());
    }

    @Override
    public Iterator<FileStats> iterator() {
        return getFileStats().iterator();
    }

    public void setSortBy(final SortBy sortBy) {
        if (sortBy == null) {
            return;
        }

        this.sortBy = sortBy;
    }

    public SortBy getSortBy() {
        return sortBy;
    }

    public void setOrder(final Order order) {
        if (order == null) {
            return;
        }

        this.order = order;
    }

    public Order getOrder() {
        return order;
    }

}
