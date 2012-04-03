package com.github.timurstrekalov;

import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.Map;
import java.util.Set;

class ScriptData {

    private final String sourceName;
    private final String sourceCode;
    private final String hashedSourceName;

    private final Map<Integer, Integer> statementsWithLengths = Maps.newTreeMap();

    ScriptData(final String sourceName, final String sourceCode) {
        this.sourceName = sourceName;
        this.sourceCode = sourceCode;

        hashedSourceName = DigestUtils.md5Hex(sourceName);
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

    public String getHashedSourceName() {
        return hashedSourceName;
    }
}
