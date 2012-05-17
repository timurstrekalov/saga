package com.github.timurstrekalov.saga.core;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

class LineCoverageRecord {

    private int lineNr;
    private int timesExecuted;
    private String line;

    LineCoverageRecord(final int lineNr, final int timesExecuted, final String line) {
        this.lineNr = lineNr;
        this.timesExecuted = timesExecuted;
        this.line = line;
    }

    private LineCoverageRecord() {

    }

    public static LineCoverageRecord merge(final LineCoverageRecord l1, final LineCoverageRecord l2) {
        Validate.isTrue(l1.lineNr == l2.lineNr);
        Validate.isTrue(l1.line.equals(l2.line));

        final LineCoverageRecord merged = new LineCoverageRecord();

        merged.lineNr = l1.lineNr;
        merged.timesExecuted = l1.timesExecuted == -1 ? -1 : l1.timesExecuted + l2.timesExecuted;
        merged.line = l1.line;

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
        return timesExecuted > -1;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
                .append("lineNr", lineNr)
                .append("timesExecuted", timesExecuted)
                .append("line", line)
                .toString();
    }

}
