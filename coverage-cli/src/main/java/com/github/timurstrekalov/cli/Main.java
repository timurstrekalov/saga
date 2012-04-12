package com.github.timurstrekalov.cli;

import com.github.timurstrekalov.CoverageGenerator;
import com.google.common.collect.ImmutableList;
import org.apache.commons.cli.*;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(final String[] args) throws IOException, ParseException {
        final Option baseDirOpt = new Option("b", "base-dir", true, "(required) base directory for test search");
        final Option testPathOpt = new Option("t", "test-path", true, "(required) Ant-style paths to the tests to run");
        final Option outputDirOpt = new Option("o", "output-dir", true, "(required) the output directory for coverage reports");

        final Option outputInstrumentedFilesOpt = new Option("f", "output-instrumented-files", true,
                "Whether to output instrumented files (default is false)");

        final Option noInstrumentPatternOpt = new Option("n", "no-instrument-pattern", true,
                "Regular expression patterns to match classes to exclude from instrumentation");

        final Option helpOpt = new Option("h", "help", false, "Print this message");
        final Options options = new Options();

        options.addOption(baseDirOpt);
        options.addOption(testPathOpt);
        options.addOption(outputDirOpt);
        options.addOption(outputInstrumentedFilesOpt);
        options.addOption(noInstrumentPatternOpt);
        options.addOption(helpOpt);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args, false);

            if (line.hasOption('h')) {
                printHelpAndExit(options);
            }

            baseDirOpt.setRequired(true);
            testPathOpt.setRequired(true);
            outputDirOpt.setRequired(true);

            options.addOption(baseDirOpt);
            options.addOption(testPathOpt);
            options.addOption(outputDirOpt);

            parser = new GnuParser();
            line = parser.parse(options, args);

            final CoverageGenerator gen = new CoverageGenerator();

            gen.setTests(findTests(line.getOptionValue('b'), line.getOptionValues('t')));
            gen.setOutputDir(new File(line.getOptionValue('o')));

            final String outputInstrumentedFiles = line.getOptionValue('f');
            if (outputInstrumentedFiles != null) {
                gen.setOutputInstrumentedFiles(true);
            }

            final String[] noInstrumentPatterns = line.getOptionValues('n');
            if (noInstrumentPatterns != null) {
                gen.setNoInstrumentPatterns(ImmutableList.copyOf(noInstrumentPatterns));
            }

            gen.run();
        } catch (final MissingOptionException e) {
            System.err.println(e.getMessage());
            printHelpAndExit(options);
        } catch (final UnrecognizedOptionException e) {
            System.err.println(e.getMessage());
            printHelpAndExit(options);
        } catch (final ParseException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelpAndExit(final Options options) {
        new HelpFormatter().printHelp("java -jar coverage.jar", options);
        System.exit(1);
    }

    private static File[] findTests(final String baseDir, final String[] tests) {
        final DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(baseDir);
        scanner.setIncludes(tests);
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
