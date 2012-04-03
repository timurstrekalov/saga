package com.github.timurstrekalov;

class Util {

    static int toCoverage(final int totalStatements, final int totalExecuted) {
        return (int) ((double) totalExecuted / totalStatements * 100);
    }

    static int sum(final Iterable<Integer> ints) {
        int sum = 0;

        for (final Integer i : ints) {
            sum += i;
        }

        return sum;
    }

}
