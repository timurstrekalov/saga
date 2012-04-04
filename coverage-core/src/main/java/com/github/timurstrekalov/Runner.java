package com.github.timurstrekalov;

import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.IOException;

public class Runner {

    public static void main(String[] args) throws IOException {
        final CoverageGenerator gen = new CoverageGenerator();

        gen.setTests(findTests(".", "**/src/test/resources/*Test*.html"));
        gen.setOutputDir(new File("target/coverage"));

        gen.setOutputInstrumentedFiles(true);
//        gen.setNoInstrumentPatterns(of("^.+Test.*$", "^script in .+from \\(\\d+, \\d+\\) to \\(\\d+, \\d+\\)$"));

        gen.run();
    }

    private static File[] findTests(final String baseDir, final String... tests) {
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(baseDir);
        scanner.setIncludes(tests);
        scanner.scan();

        final String[] matches = scanner.getIncludedFiles();
        final File[] result = new File[matches.length];

        for (int i = 0; i < matches.length; i++) {
            result[i] = new File(matches[i]);
        }

        return result;
    }

}
