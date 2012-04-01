package com.github.timurstrekalov;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.net.URL;
import java.util.List;

public class CoverageGeneratorTest {

    public static void main(final String[] args) throws Exception {
        final List<URL> tests = ImmutableList.of(CoverageGeneratorTest.class.getResource("/test.html"));
        final List<String> ignore = ImmutableList.of("^.+Test.js$");

        final CoverageGenerator gen = new CoverageGenerator("_COV", tests, new File("target/coverage"));

        gen.setIgnorePatterns(ignore);
        gen.setOutputInstrumentedFiles(true);

        gen.run();
    }

}
