package com.github.timurstrekalov;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

import static org.apache.commons.lang.Validate.isTrue;

class LineCoverageRecord {

    private static final String[] reservedKeywords = {
            "break",
            "case",
            "catch",
            "continue",
            "debugger",
            "default",
            "delete",
            "do",
            "else",
            "false",
            "finally",
            "for",
            "function",
            "if",
            "in",
            "instanceof",
            "new",
            "null",
            "return",
            "switch",
            "this",
            "true",
            "throw",
            "try",
            "typeof",
            "var",
            "void",
            "while",
            "with"
    };

    private static final Pattern reservedKeywordsPattern = Pattern.compile(String.format("\\b(%s)\\b",
            StringUtils.join(reservedKeywords, '|')));

    private static final Pattern jsStringPattern = Pattern.compile("('.*'|\".*\")");
    private static final Pattern jsNumberPattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");

    private int lineNr;
    private int timesExecuted;
    private String line;
    private boolean executable;

    LineCoverageRecord(final int lineNr, final int timesExecuted, final String line, final boolean executable) {
        this.lineNr = lineNr;
        this.timesExecuted = timesExecuted;
        this.line = line;
        this.executable = executable;
    }

    private LineCoverageRecord() {

    }

    public static LineCoverageRecord merge(final LineCoverageRecord l1, final LineCoverageRecord l2) {
        isTrue(l1.lineNr == l2.lineNr);
        isTrue(l1.executable == l2.executable);
        isTrue(l1.line.equals(l2.line));

        final LineCoverageRecord merged = new LineCoverageRecord();

        merged.lineNr = l1.lineNr;
        merged.timesExecuted = l1.timesExecuted + l2.timesExecuted;
        merged.line = l1.line;
        merged.executable = l1.executable;

        return merged;
    }

    public int getLineNr() {
        return lineNr;
    }

    public int getTimesExecuted() {
        return timesExecuted;
    }

    public String getLineSource() {
        return StringEscapeUtils.escapeHtml(StringEscapeUtils.escapeJavaScript(line));
    }

    public boolean isExecutable() {
        return executable;
    }

    public int getExecutableAsNumber() {
        return executable ? 1 : 0;
    }

}
