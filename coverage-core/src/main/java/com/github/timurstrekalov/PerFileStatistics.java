package com.github.timurstrekalov;

class PerFileStatistics {

    public final String name;
    public final int statements;
    public final int executed;
    public final int coverage;

    PerFileStatistics(final String name, final int statements, final int executed, final int coverage) {
        this.name = name;
        this.statements = statements;
        this.executed = executed;
        this.coverage = coverage;
    }

}
