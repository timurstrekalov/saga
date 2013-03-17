package com.github.timurstrekalov.saga.core;

import java.util.Comparator;

import com.github.timurstrekalov.saga.core.model.FileStats;

public enum SortBy implements Comparator<FileStats> {
    FILE {
        @Override
        public int compare(final FileStats s1, final FileStats s2) {
            // TODO refactor this, currently all over the place
            final String n1 = s1.getParentName() != null ? s1.getParentName() + "/" + s1.getFileName() : s1.getFileName();
            final String n2 = s2.getParentName() != null ? s2.getParentName() + "/" + s2.getFileName() : s2.getFileName();

            return n1.compareTo(n2);
        }
    },
    STATEMENTS {
        @Override
        public int compare(final FileStats s1, final FileStats s2) {
            return Integer.valueOf(s1.getStatements()).compareTo(s2.getStatements());
        }
    },
    EXECUTED {
        @Override
        public int compare(final FileStats s1, final FileStats s2) {
            return Integer.valueOf(s1.getExecuted()).compareTo(s2.getExecuted());
        }
    },
    COVERAGE {
        @Override
        public int compare(final FileStats s1, final FileStats s2) {
            return Integer.valueOf(s1.getCoverage()).compareTo(s2.getCoverage());
        }
    };

    public abstract int compare(FileStats s1, FileStats s2);

}
