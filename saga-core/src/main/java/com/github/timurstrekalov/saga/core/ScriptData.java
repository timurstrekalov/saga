package com.github.timurstrekalov.saga.core;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

class ScriptData {

    private final String sourceName;
    private final String sourceCode;

    private final Map<Integer, Integer> statementsWithLengths = Maps.newTreeMap();

    private String instrumentedSourceCode;

    ScriptData(final String sourceName, final String sourceCode) {
        this.sourceName = sourceName;
        this.sourceCode = sourceCode;
    }

    void addExecutableLine(final Integer lineNr, final Integer length) {
        statementsWithLengths.put(lineNr, length);
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public Set<Integer> getLineNumbersOfAllStatements() {
        return statementsWithLengths.keySet();
    }

    public int getNumberOfStatements() {
        return statementsWithLengths.size();
    }

    public boolean hasStatement(final int lineNr) {
        return statementsWithLengths.containsKey(lineNr);
    }

    public Integer getStatementLength(final int lineNr) {
        return statementsWithLengths.get(lineNr);
    }

    /**
     * For inline scripts, first statement's line number might not be 1 (it will be the actual line number in the
     * HTML)
     */
    public Integer getLineNumberOfFirstStatement() {
        return getLineNumbersOfAllStatements().iterator().next();
    }

    public void setInstrumentedSourceCode(final String instrumentedSourceCode) {
        this.instrumentedSourceCode = instrumentedSourceCode;
    }

    public String getInstrumentedSourceCode() {
        return instrumentedSourceCode;
    }
}
