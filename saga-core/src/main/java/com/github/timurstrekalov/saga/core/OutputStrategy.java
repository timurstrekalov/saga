package com.github.timurstrekalov.saga.core;

public enum OutputStrategy {
    PER_TEST,
    TOTAL,
    BOTH {
        @Override
        public boolean contains(final OutputStrategy strategy) {
            return true;
        }
    };

    public boolean contains(final OutputStrategy strategy) {
        return this == strategy;
    }

}
