package com.github.timurstrekalov;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import java.util.List;

import static com.github.timurstrekalov.Util.sum;
import static com.github.timurstrekalov.Util.toCoverage;
import static org.apache.commons.lang.Validate.isTrue;

class FileStats {

    public final String name;
    public final String href;

    public final List<LineCoverageRecord> lineCoverageRecords;

    FileStats(final String name,
            final String href,
            final List<LineCoverageRecord> lineCoverageRecords) {

        this.name = name;
        this.href = href;

        this.lineCoverageRecords = lineCoverageRecords;
    }

    public List<LineCoverageRecord> getLineCoverageRecords() {
        return lineCoverageRecords;
    }

    public int getStatements() {
        return Collections2.filter(lineCoverageRecords, new Predicate<LineCoverageRecord>() {
            @Override
            public boolean apply(final LineCoverageRecord input) {
                return input.isExecutable();
            }
        }).size();
    }

    public int getExecuted() {
        return sum(Collections2.transform(lineCoverageRecords, new Function<LineCoverageRecord, Integer>() {
            @Override
            public Integer apply(final LineCoverageRecord input) {
                return input.getTimesExecuted() > 0 ? 1 : 0;
            }
        }));
    }

    public int getCoverage() {
        return toCoverage(getStatements(), getExecuted());
    }

    static FileStats merge(final FileStats s1, final FileStats s2) {
        final List<LineCoverageRecord> r1 = s1.getLineCoverageRecords();
        final List<LineCoverageRecord> r2 = s2.getLineCoverageRecords();

        isTrue(s1.name.equals(s2.name));
        isTrue(r1.size() == r2.size());

        final List<LineCoverageRecord> mergedRecords = Lists.newLinkedList();

        for (int i = 0; i < r1.size(); i++) {
            final LineCoverageRecord l1 = r1.get(i);
            final LineCoverageRecord l2 = r2.get(i);

            mergedRecords.add(LineCoverageRecord.merge(l1, l2));
        }

        return new FileStats(s1.name, s1.href, mergedRecords);
    }
}
