package com.github.timurstrekalov.saga.cli;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.github.timurstrekalov.saga.core.CoverageGenerator;
import com.github.timurstrekalov.saga.core.CoverageGenerators;
import com.github.timurstrekalov.saga.core.OutputStrategy;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws IOException, ParseException {
        logger.debug("Starting...");

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

        final Option sourcesToPreloadOpt = new Option("p", "preload-sources", true,
                "Comma-separated list of Ant-style paths to files to preload");

        final Option sourcesToPreloadEncodingOpt = new Option(null, "preload-sources-encoding", true,
                "Encoding to use when preloading sources");

        final Option threadCountOpt = new Option("t", "thread-count", true,
                "The maximum number of threads to use (defaults to the number of cores)");

        final Option outputStrategyOpt = new Option("s", "output-strategy", true,
                "Coverage report output strategy. One of " + Arrays.toString(OutputStrategy.values()));

        final Option includeInlineScriptsOpt = new Option("d", "include-inline-scripts", false,
                "Whether to include inline scripts into instrumentation by default (default is false)");

        final Option backgroundJavaScriptTimeoutOpt = new Option("j", "background-javascript-timeout", true,
                "How long to wait for background JavaScript to finish running (in milliseconds, default is 5 minutes)");

        final Option browserVersionOpt = new Option("v", "browser-version", true,
                "Determines the browser and version profile that HtmlUnit will simulate");

        final Option reportFormatsOpt = new Option(null, "report-formats", true,
                "A comma-separated list of formats of the reports to be generated. Valid values are: HTML, RAW, CSV, PDF");

        final Option sortByOpt = new Option(null, "sort-by", true,
                "The column to sort by, one of 'file', 'statements', 'executed' or 'coverage' (default is 'coverage')");

        final Option orderOpt = new Option(null, "order", true,
                "The order of sorting, one of 'asc' or 'ascending', 'desc' or 'descending' (default is 'ascending')");

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
        options.addOption(sourcesToPreloadOpt);
        options.addOption(sourcesToPreloadEncodingOpt);
        options.addOption(backgroundJavaScriptTimeoutOpt);
        options.addOption(browserVersionOpt);
        options.addOption(reportFormatsOpt);
        options.addOption(sortByOpt);
        options.addOption(orderOpt);

        logger.debug("Finished configuring options");

        try {
            CommandLineParser parser = new GnuParser();
            CommandLine line = parser.parse(options, args, false);

            logger.debug("Parsed the arguments, take 1");

            baseDirOpt.setRequired(true);
            includeOpt.setRequired(true);
            outputDirOpt.setRequired(true);

            options.addOption(baseDirOpt);
            options.addOption(includeOpt);
            options.addOption(outputDirOpt);

            if (line.hasOption(helpOpt.getLongOpt())) {
                printHelpAndExit(options);
            }

            parser = new GnuParser();
            line = parser.parse(options, args);

            logger.debug("Parsed the arguments, take 2");

            final File baseDir = new File(line.getOptionValue(baseDirOpt.getLongOpt()));
            final String includes = line.getOptionValue(includeOpt.getLongOpt());
            final String excludes = line.getOptionValue(excludeOpt.getLongOpt());
            final File outputDir = new File(line.getOptionValue(outputDirOpt.getLongOpt()));

            final CoverageGenerator gen = CoverageGenerators.newInstance(baseDir, includes, excludes, outputDir);

            if (line.hasOption(outputInstrumentedFilesOpt.getLongOpt())) {
                gen.setOutputInstrumentedFiles(true);
            }

            gen.setNoInstrumentPatterns(line.getOptionValues(noInstrumentPatternOpt.getLongOpt()));
            gen.setSourcesToPreload(line.getOptionValue(sourcesToPreloadOpt.getLongOpt()));
            gen.setOutputStrategy(line.getOptionValue(outputStrategyOpt.getLongOpt()));

            final String threadCount = line.getOptionValue(threadCountOpt.getLongOpt());
            if (threadCount != null) {
                try {
                    gen.setThreadCount(Integer.parseInt(threadCount));
                } catch (final Exception e) {
                    System.err.println("Invalid thread count");
                    printHelpAndExit(options);
                }
            }

            if (line.hasOption(includeInlineScriptsOpt.getLongOpt())) {
                gen.setIncludeInlineScripts(true);
            }

            final String backgroundJavaScriptTimeout = line.getOptionValue(backgroundJavaScriptTimeoutOpt.getLongOpt());
            if (backgroundJavaScriptTimeout != null) {
                try {
                    gen.setBackgroundJavaScriptTimeout(Long.valueOf(backgroundJavaScriptTimeout));
                } catch (final Exception e) {
                    System.err.println("Invalid timeout");
                    printHelpAndExit(options);
                }
            }

            gen.setBrowserVersion(line.getOptionValue(browserVersionOpt.getLongOpt()));
            gen.setReportFormats(line.getOptionValue(reportFormatsOpt.getLongOpt()));
            gen.setSortBy(line.getOptionValue(sortByOpt.getLongOpt()));
            gen.setOrder(line.getOptionValue(orderOpt.getLongOpt()));

            logger.debug("Configured the coverage generator, running");

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
