package com.github.timurstrekalov.cli;

import com.github.timurstrekalov.CoverageGenerator;
import com.google.common.collect.ImmutableList;
import org.apache.commons.cli.ParseException;
import org.apache.tools.ant.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(final String[] args) throws IOException, ParseException {
//        final Option baseDir = new Option("b", "base-dir", true, "Base directory for test search");
//
//        final Option tests = new Option("t", "test-path", true, "Ant-style paths to the tests to run");
//        tests.setRequired(true);
//
//        final Option outputDir = new Option("o", "output-dir", true, "The output directory for coverage reports");
//        outputDir.setRequired(true);
//
//        final Option outputInstrumentedFiles = new Option("f", "output-instrumented-files", true, "Whether to output instrumented files");
//        final Option noInstrumentPattern = new Option("n", "no-instrument-pattern", true, "no-instrument-pattern");
//
//        final Options options = new Options();
//
//        options.addOption(baseDir);
//        options.addOption(tests);
//        options.addOption(outputDir);
//        options.addOption(outputInstrumentedFiles);
//        options.addOption(noInstrumentPattern);
//
//        final CommandLineParser parser = new GnuParser();
//
//        try {
//            final CommandLine line = parser.parse(options, args);
//
//            for (final Iterator i = line.iterator(); i.hasNext();) {
//                System.out.println(i.next());
//            }
//        } catch (final ParseException e) {
//            System.err.println(e.getMessage());
//            System.exit(1);
//        }

        final CoverageGenerator gen = new CoverageGenerator();

//        gen.setTests(findTests("/Users/timur/Desktop/static-resources/target/general", ImmutableList.of("**/*-ManualSpecRunner.html")));
        gen.setTests(findTests(".", ImmutableList.of("**/ClassTest.html")));
        gen.setOutputDir(new File("/Users/timur/Desktop/coverage"));

        gen.setOutputInstrumentedFiles(true);
//        gen.setNoInstrumentPatterns(of("^.+Test.*$", "^script in .+from \\(\\d+, \\d+\\) to \\(\\d+, \\d+\\)$"));

        gen.run();
    }

    private static File[] findTests(final String baseDir, final List<String> tests) {
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(baseDir);
        scanner.setIncludes(tests.toArray(new String[tests.size()]));
        scanner.scan();

        final String[] matches = scanner.getIncludedFiles();
        final File[] result = new File[matches.length];
        final File parent = new File(baseDir);

        for (int i = 0; i < matches.length; i++) {
            result[i] = new File(parent, matches[i]).getAbsoluteFile();
        }

        return result;
    }

}
