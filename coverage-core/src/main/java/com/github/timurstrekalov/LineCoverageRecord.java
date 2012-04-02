package com.github.timurstrekalov;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

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

    public final int lineNr;
    public final int coverage;
    public final String line;
    public final boolean executable;
    public final String cssClass;

    LineCoverageRecord(final int lineNr, final int coverage, final String line, final boolean executable) {
        this.lineNr = lineNr;
        this.coverage = coverage;

        String styledLine = jsStringPattern.matcher(line).replaceAll("<span class=\"string\">$1</span>");
        styledLine = jsNumberPattern.matcher(styledLine).replaceAll("<span class=\"number\">$1</span>");
        styledLine = reservedKeywordsPattern.matcher(styledLine).replaceAll("<span class=\"keyword\">$1</span>");

        this.line = styledLine;
        this.executable = executable;

        if (!executable) {
            cssClass = "not-executable";
        } else if (coverage > 0) {
            cssClass = "covered";
        } else {
            cssClass = "not-covered";
        }
    }
}
