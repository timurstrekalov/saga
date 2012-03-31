package com.github.timurstrekalov;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.net.URI;
import java.util.List;

public class CoverageGeneratorTest {

    public static void main(final String[] args) throws Exception {
        final List<URI> tests = ImmutableList.of(new File("test.html").toURI());
        final List<String> ignore = ImmutableList.of("^.+Test.js$");

        final CoverageGenerator gen = new CoverageGenerator(tests);
        gen.setIgnorePatterns(ignore);
        gen.run();
    }

}
