package com.github.timurstrekalov.saga.core.model;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedSet;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import static com.github.timurstrekalov.saga.core.model.LineCoverageRecord.LINE_MISSED;
import static com.github.timurstrekalov.saga.core.model.LineCoverageRecord.LINE_NO_STATEMENT;

public final class ScriptData {

    private final URI sourceUri;
    private final String sourceCode;
    private final boolean separateFile;

    private final SortedSet<Integer> linesWithStatements = Sets.newTreeSet();

    private String instrumentedSourceCode;

    public ScriptData(final URI sourceUri, final String sourceCode, final boolean separateFile) {
        this.sourceUri = sourceUri;
        this.sourceCode = sourceCode;
        this.separateFile = separateFile;
    }

    public void addExecutableLine(final Integer lineNr) {
        linesWithStatements.add(lineNr);
    }

    public URI getSourceUri() {
        return sourceUri;
    }

    public String getSourceUriAsString() {
        return sourceUri.toString();
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public SortedSet<Integer> getLineNumbersOfAllStatements() {
        return Sets.newTreeSet(linesWithStatements);
    }

    public int getNumberOfStatements() {
        return linesWithStatements.size();
    }

    private boolean hasStatement(final int lineNr) {
        return linesWithStatements.contains(lineNr);
    }

    /**
     * For inline scripts, first statement's line number might not be 1 (it will be the actual line number in the
     * HTML)
     */
    public int getLineNumberOfFirstStatement() {
        return linesWithStatements.first();
    }

    public void setInstrumentedSourceCode(final String instrumentedSourceCode) {
        this.instrumentedSourceCode = instrumentedSourceCode;
    }

    public String getInstrumentedSourceCode() {
        return instrumentedSourceCode;
    }

    public boolean isSeparateFile() {
        return separateFile;
    }

    // TODO clean up and test this mess
    public ScriptCoverageStatistics generateScriptCoverageStatistics(final URI baseUri, final Map<Integer, Double> coverageData) {
        final Scanner in = new Scanner(getSourceCode());

        final List<LineCoverageRecord> lineCoverageRecords = Lists.newArrayList();

        if (!linesWithStatements.isEmpty()) {
            // pad with extra line coverage records if first executable statement is not the first line (comments at the start of files)
            for (int lineNr = 1; lineNr < getLineNumberOfFirstStatement() && in.hasNext(); lineNr++) {
                lineCoverageRecords.add(new LineCoverageRecord(lineNr, LINE_NO_STATEMENT, in.nextLine()));
            }

            for (int lineNr = getLineNumberOfFirstStatement(); in.hasNext(); lineNr++) {
                final String line = in.nextLine();

                final Double coverageEntry = coverageData.get(lineNr);
                final int timesLineExecuted;

                if (coverageEntry == null) {
                    timesLineExecuted = hasStatement(lineNr) ? LINE_MISSED : LINE_NO_STATEMENT;
                } else {
                    timesLineExecuted = coverageEntry.intValue();
                }

                lineCoverageRecords.add(new LineCoverageRecord(lineNr, timesLineExecuted, line));
            }
        } else {
            for (int lineNr = 1; in.hasNext(); lineNr++) {
                lineCoverageRecords.add(new LineCoverageRecord(lineNr, LINE_NO_STATEMENT, in.nextLine()));
            }
        }

        return new ScriptCoverageStatistics(baseUri, getSourceUri(), lineCoverageRecords, isSeparateFile());
    }

}
