package com.github.timurstrekalov.saga.cli;

import com.github.timurstrekalov.saga.core.CoverageGenerator;
import com.github.timurstrekalov.saga.core.OutputStrategy;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Main {

    public static void main(final String[] args) throws IOException, ParseException {
        final Option baseDirOpt = new Option("b", "base-dir", true, "Base directory for test search");
        final Option includeOpt = new Option("i", "include", true,
                "Comma-separated list of Ant-style paths to the tests to run");
        final Option excludeOpt = new Option("e", "exclude", true,
                "Comma-separated list of Ant-style paths to the tests to exclude from run");

        final Option outputDirOpt = new Option("o", "output-dir", true, "The output directory for coverage reports");

        final Option outputInstrumentedFilesOpt = new Option("f", "output-instrumented-files", false,
                "Whether to output instrumented files (default is false)");

        final Option noInstrumentPatternOpt = new Option("n", "no-instrument-pattern", true,
                "Regular expression patterns to match classes to exclude from instrumentation");
        noInstrumentPatternOpt.setArgs(Option.UNLIMITED_VALUES);

        final Option threadCountOpt = new Option("t", "thread-count", true,
                "The maximum number of threads to use (defaults to the number of cores)");

        final Option outputStrategyOpt = new Option("s", "output-strategy", true,
                "Coverage report output strategy. One of " + Arrays.toString(OutputStrategy.values()));

        final Option includeInlineScriptsOpt = new Option("d", "include-inline-scripts", false,
                "Whether to include inline scripts into instrumentation by default (default is false)");

        final Option backgroundJavaScriptTimeoutOpt = new Option("j", "background-javascript-timeout", false,
                "How long to wait for background JavaScript to finish running (in milliseconds, default is 5 minutes)");

        final Option helpOpt = new Option("h", "help", false, "Print this message");
        final Options options = new Options();

        options.addOption(baseDirOpt);
        options.addOption(includeOpt);
        options.addOption(excludeOpt);
        options.addOption(outputDirOpt);
        options.addOption(outputInstrumentedFilesOpt);
        options.addOption(noInstrumentPatternOpt);
        options.addOption(threadCountOpt);
        options.addOption(outputStrategyOpt);
        options.addOption(includeInlineScriptsOpt);
        options.addOption(helpOpt);
        options.addOption(backgroundJavaScriptTimeoutOpt);

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args, false);

            baseDirOpt.setRequired(true);
            includeOpt.setRequired(true);
            outputDirOpt.setRequired(true);

            options.addOption(baseDirOpt);
            options.addOption(includeOpt);
            options.addOption(outputDirOpt);

            if (line.hasOption('h')) {
                printHelpAndExit(options);
            }

            parser = new GnuParser();
            line = parser.parse(options, args);

            final File baseDir = new File(line.getOptionValue('b'));
            final String includes = line.getOptionValue('i');
            final String excludes = line.getOptionValue('e');
            final File outputDir = new File(line.getOptionValue('o'));

            final CoverageGenerator gen = new CoverageGenerator(baseDir, includes, excludes, outputDir);

            if (line.hasOption('f')) {
                gen.setOutputInstrumentedFiles(true);
            }

            gen.setNoInstrumentPatterns(line.getOptionValues('n'));
            gen.setOutputStrategy(line.getOptionValue('s'));

            final String threadCount = line.getOptionValue('t');
            if (threadCount != null) {
                try {
                    gen.setThreadCount(Integer.parseInt(threadCount));
                } catch (final Exception e) {
                    System.err.println("Invalid thread count");
                    printHelpAndExit(options);
                }
            }


            if (line.hasOption('d')) {
                gen.setIncludeInlineScripts(true);
            }

            final String backgroundJavaScriptTimeout = line.getOptionValue('j');
            if (backgroundJavaScriptTimeout != null) {
                try {
                    gen.setBackgroundJavaScriptTimeout(Long.valueOf(backgroundJavaScriptTimeout));
                } catch (final Exception e) {
                    System.err.println("Invalid timeout");
                    printHelpAndExit(options);
                }
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
        new HelpFormatter().printHelp("java -jar saga-cli-<version>-jar-with-dependencies.jar", options, true);
        System.exit(1);
    }

}
