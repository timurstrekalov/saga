package com.github.timurstrekalov;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;

import static com.github.timurstrekalov.Util.sum;
import static com.github.timurstrekalov.Util.toCoverage;

class RunStats {

    public final String runName;

    private final Map<String, FileStats> fileStatsMap = Maps.newTreeMap();

    RunStats(final String runName) {
        this.runName = runName;
    }

    void add(final FileStats newStats) {
        final String key = newStats.name;
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
        return sum(Collections2.transform(fileStatsMap.values(), new Function<FileStats, Integer>() {
            @Override
            public Integer apply(final FileStats input) {
                return input.getStatements();
            }
        }));
    }

    public int getTotalExecuted() {
        return sum(Collections2.transform(fileStatsMap.values(), new Function<FileStats, Integer>() {
            @Override
            public Integer apply(final FileStats input) {
                return input.getExecuted();
            }
        }));
    }

    public int getTotalCoverage() {
        return toCoverage(getTotalStatements(), getTotalExecuted());
    }

}
