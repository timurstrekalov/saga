package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.JavaScriptPage;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.github.timurstrekalov.saga.core.reporter.CsvReporter;
import com.github.timurstrekalov.saga.core.reporter.HtmlReporter;
import com.github.timurstrekalov.saga.core.reporter.PdfReporter;
import com.github.timurstrekalov.saga.core.reporter.RawReporter;
import com.github.timurstrekalov.saga.core.reporter.Reporter;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCoverageGenerator implements CoverageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoverageGenerator.class);
    private static final SagaWebClient localClient = new SagaWebClient();
    private static final Map<ReportFormat, Class<? extends Reporter>> reporters = ImmutableMap.<ReportFormat, Class<? extends Reporter>>builder()
            .put(ReportFormat.HTML, HtmlReporter.class)
            .put(ReportFormat.RAW, RawReporter.class)
            .put(ReportFormat.CSV, CsvReporter.class)
            .put(ReportFormat.PDF, PdfReporter.class)
            .build();

    private static final String COVERAGE_VARIABLE_NAME = "__coverage_data";
    private static final String TOTAL_REPORT_NAME = "total";
    private static final String INSTRUMENTED_FILE_DIRECTORY_NAME = "instrumented";
    private static final String INLINE_SCRIPT_RE = ".+__from_\\d+_\\d+_to_\\d+_\\d+$";

    private final File baseDir;
    private final String includes;
    private final String excludes;
    private final File outputDir;

    private Set<String> noInstrumentPatterns = Sets.newHashSet();
    private boolean outputInstrumentedFiles;

    private boolean cacheInstrumentedCode = true;

    private OutputStrategy outputStrategy = OutputStrategy.TOTAL;

    private int threadCount = Runtime.getRuntime().availableProcessors();

    private boolean includeInlineScripts = false;

    private long backgroundJavaScriptTimeout = 5 * 60 * 1000;

    private String sourcesToPreload;
    private String sourcesToPreloadEncoding = "UTF-8";

    private Set<ReportFormat> reportFormats = ImmutableSet.of(ReportFormat.HTML, ReportFormat.RAW);

    DefaultCoverageGenerator(final File baseDir, final String includes, final String excludes, final File outputDir) {
        Preconditions.checkNotNull(baseDir, "baseDir cannot be null");
        Preconditions.checkNotNull(outputDir, "outputDir cannot be null");
        Preconditions.checkNotNull(includes, "includes cannot be null");

        Preconditions.checkState(baseDir.exists(), "baseDir doesn't exist");

        this.baseDir = baseDir;
        this.includes = includes;
        this.excludes = excludes;
        this.outputDir = outputDir;

        // make HtmlUnit shut up
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
    }

    @Override
    public void run() throws IOException {
        FileUtils.mkdir(outputDir.getAbsolutePath());

        final List<File> tests = FileUtils.getFiles(baseDir, includes, excludes);

        if (tests.isEmpty()) {
            logger.warn("No tests found, exiting");
            return;
        }

        logger.info("{} tests found", tests.size());
        threadCount = Math.min(threadCount, tests.size());

        logger.info("Using up to {} threads", threadCount);
        logger.info("Output strategy set to {}", outputStrategy);

        if (!includeInlineScripts) {
            noInstrumentPatterns.add(INLINE_SCRIPT_RE);
            noInstrumentPatterns.add(".+JavaScriptStringJob");
            noInstrumentPatterns.add(".+#\\d+\\(eval\\)\\(\\d+\\)");
        }

        if (!noInstrumentPatterns.isEmpty()) {
            logger.info("Using the following no-instrument patterns:\n\t{}", StringUtils.join(noInstrumentPatterns, "\n\t"));
        }

        final Collection<Pattern> ignorePatterns = createPatterns();
        final File instrumentedFileDirectory = new File(outputDir, INSTRUMENTED_FILE_DIRECTORY_NAME);
        final RunStats totalStats = new RunStats(new File(outputDir, TOTAL_REPORT_NAME), "Total coverage report");

        if (outputStrategy.contains(OutputStrategy.TOTAL) && sourcesToPreload != null) {
            logger.info("Using {} to preload sources", sourcesToPreloadEncoding);

            final List<File> filesToPreload = FileUtils.getFiles(baseDir, sourcesToPreload, null);

            logger.info("Preloading {} files", filesToPreload.size());

            final WebClient webClient = localClient.get();
            final ScriptInstrumenter instrumenter = newInstrumenter(ignorePatterns, instrumentedFileDirectory,
                    webClient.getJavaScriptEngine().getContextFactory());

            for (final File file : filesToPreload) {
                logger.debug("Preloading {}", file);

                final String source = CharStreams.toString(Files.newReaderSupplier(file, Charset.forName(sourcesToPreloadEncoding)));
                instrumenter.preProcess(null, source, file.getAbsolutePath(), 0, null);
            }

            for (final ScriptData data : instrumenter.getScriptDataList()) {
                final Map<Integer, Double> coverageData = Maps.newHashMap();

                for (final Integer lineNumber : data.getLineNumbersOfAllStatements()) {
                    coverageData.put(lineNumber, 0.0);
                }

                final FileStats fileStats = getFileStatsFromScriptData(coverageData, data);
                totalStats.add(fileStats);
            }
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CompletionService<RunStats> completionService = new ExecutorCompletionService<RunStats>(executorService);

        for (final File test : tests) {
            completionService.submit(new Callable<RunStats>() {
                @Override
                public RunStats call() {
                    logger.info("Running {}", test.getAbsoluteFile().toURI().normalize().getPath());

                    try {
                        final RunStats runStats = runTest(test, ignorePatterns, instrumentedFileDirectory);

                        if (runStats == RunStats.EMPTY) {
                            logger.warn("No actual test run for file: {}", test);
                        } else if (outputStrategy.contains(OutputStrategy.PER_TEST)) {
                            writeRunStats(runStats);
                        }

                        return runStats;
                    } catch (final IOException e) {
                        return RunStats.EMPTY;
                    }
                }
            });
        }

        final List<RunStats> allRunStats = Lists.newLinkedList();

        try {
            for (final File test : tests) {
                try {
                    final Future<RunStats> future = completionService.take();
                    final RunStats runStats = future.get();

                    allRunStats.add(runStats);
                } catch (final Exception e) {
                    logger.warn("Error running test {}: {}", test.getAbsolutePath(), e.getMessage());
                    logger.debug(e.getMessage(), e);
                }
            }
        } finally {
            executorService.shutdown();
        }

        logger.info("Test run finished");

        if (outputStrategy.contains(OutputStrategy.TOTAL)) {
            for (final RunStats runStats : allRunStats) {
                if (runStats != RunStats.EMPTY) {
                    for (final FileStats fileStats : runStats) {
                        totalStats.add(fileStats);
                    }
                }
            }

            writeRunStats(totalStats);
        }
    }

    private Collection<Pattern> createPatterns() {
        return Collections2.transform(noInstrumentPatterns, new Function<String, Pattern>() {
            @Override
            public Pattern apply(final String input) {
                return Pattern.compile(input);
            }
        });
    }

    private RunStats runTest(final File test, final Collection<Pattern> ignorePatterns, final File instrumentedFileDirectory)
            throws IOException {
        final WebClient client = localClient.get();
        final ScriptInstrumenter instrumenter = newInstrumenter(ignorePatterns, instrumentedFileDirectory,
                client.getJavaScriptEngine().getContextFactory());

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test.toURI().toURL());
        final HtmlPage htmlPage;

        if (page instanceof HtmlPage) {
            htmlPage = (HtmlPage) page;
        } else if (page instanceof JavaScriptPage) {
            final JavaScriptPage javaScriptPage = (JavaScriptPage) page;

            final WebResponse webResponse = javaScriptPage.getWebResponse();

            htmlPage = new HtmlPage(javaScriptPage.getUrl(), webResponse, javaScriptPage.getEnclosingWindow());
            client.getJavaScriptEngine().execute(htmlPage, webResponse.getContentAsString(), test.getAbsolutePath(), 1);
        } else {
            throw new RuntimeException("Unsupported page type: " + page.getUrl() + " (of class " + page.getClass() + ")");
        }

        return collectAndRunStats(client, htmlPage, test, instrumenter);
    }

    private ScriptInstrumenter newInstrumenter(
            final Collection<Pattern> ignorePatterns,
            final File instrumentedFileDirectory,
            final HtmlUnitContextFactory contextFactory) {

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(contextFactory, COVERAGE_VARIABLE_NAME);

        instrumenter.setIgnorePatterns(ignorePatterns);

        if (outputInstrumentedFiles) {
            FileUtils.mkdir(instrumentedFileDirectory.getAbsolutePath());

            instrumenter.setOutputDir(instrumentedFileDirectory);
            instrumenter.setOutputInstrumentedFiles(outputInstrumentedFiles);
        }

        instrumenter.setCacheInstrumentedCode(cacheInstrumentedCode);

        return instrumenter;
    }

    private RunStats collectAndRunStats(
            final WebClient client,
            final HtmlPage htmlPage,
            final File test,
            final ScriptInstrumenter instrumenter) throws IOException {

        client.waitForBackgroundJavaScript(backgroundJavaScriptTimeout);
        client.setScriptPreProcessor(null);

        final Object javaScriptResult = htmlPage.executeJavaScript("window." + COVERAGE_VARIABLE_NAME)
                .getJavaScriptResult();

        if (!(javaScriptResult instanceof Undefined)) {
            return collectAndWriteRunStats(test, instrumenter, (NativeObject) javaScriptResult);
        }

        return RunStats.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private RunStats collectAndWriteRunStats(
            final File test,
            final ScriptInstrumenter instrumenter,
            final NativeObject allCoverageData) throws IOException {
        final RunStats runStats = new RunStats(test);

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            @SuppressWarnings("rawtypes")
            final Map<Integer, Double> coverageData = (Map) allCoverageData.get(data.getSourceName());
            final FileStats fileStats = getFileStatsFromScriptData(coverageData, data);
            runStats.add(fileStats);
        }

        return runStats;
    }

    private FileStats getFileStatsFromScriptData(final Map<Integer, Double> coverageData, final ScriptData data) {
        final Scanner in = new Scanner(data.getSourceCode());

        final List<LineCoverageRecord> lineCoverageRecords = Lists.newArrayList();

        if (!data.getLineNumbersOfAllStatements().isEmpty()) {
            // pad with extra line coverage records if first executable statement is not the first line (comments at the start of files)
            for (int lineNr = 1; lineNr < data.getLineNumberOfFirstStatement() && in.hasNext(); lineNr++) {
                lineCoverageRecords.add(new LineCoverageRecord(lineNr, -1, in.nextLine()));
            }

            for (int lineNr = data.getLineNumberOfFirstStatement(), lengthCountdown = 0; in.hasNext(); lineNr++) {
                final String line = in.nextLine();

                final Double coverageEntry = coverageData.get(lineNr);
                final int timesLineExecuted;

                if (coverageEntry == null) {
                    final int lineLength = line.trim().length();

                    if (lengthCountdown > 0 && lineLength > 0) {
                        lengthCountdown -= lineLength;
                        timesLineExecuted = -1;
                    } else {
                        timesLineExecuted = data.hasStatement(lineNr) ? 0 : -1;
                    }
                } else {
                    timesLineExecuted = coverageEntry.intValue();

                    if (data.getStatementLength(lineNr) != null) {
                        lengthCountdown = data.getStatementLength(lineNr);
                    }
                }

                lineCoverageRecords.add(new LineCoverageRecord(lineNr, timesLineExecuted, line));
            }
        } else {
            for (int lineNr = 1; in.hasNext(); lineNr++) {
                lineCoverageRecords.add(new LineCoverageRecord(lineNr, -1, in.nextLine()));
            }
        }

        return new FileStats(data.getSourceName(), lineCoverageRecords, data.isSeparateFile());
    }

    private void writeRunStats(final RunStats stats) throws IOException {
        for (final ReportFormat reportFormat : reportFormats) {
            reporterFor(reportFormat).writeReport(baseDir, outputDir, stats);
        }
    }

    private Reporter reporterFor(final ReportFormat reportFormat) {
        if (!reporters.containsKey(reportFormat)) {
            throw new IllegalStateException("Missing reporter for format: " + reportFormat);
        }

        try {
            return reporters.get(reportFormat).newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setNoInstrumentPatterns(final Collection<String> noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            this.noInstrumentPatterns = Sets.newHashSet(noInstrumentPatterns);
        }
    }

    @Override
    public void setNoInstrumentPatterns(final String[] noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            setNoInstrumentPatterns(ImmutableList.copyOf(noInstrumentPatterns));
        }
    }

    @Override
    public void setOutputInstrumentedFiles(final Boolean outputInstrumentedFiles) {
        if (outputInstrumentedFiles != null) {
            this.outputInstrumentedFiles = outputInstrumentedFiles;
        }
    }

    @Override
    public void setCacheInstrumentedCode(final Boolean cacheInstrumentedCode) {
        if (cacheInstrumentedCode != null) {
            this.cacheInstrumentedCode = cacheInstrumentedCode;
        }
    }

    @Override
    public void setOutputStrategy(final String outputStrategy) {
        if (outputStrategy != null) {
            setOutputStrategy(OutputStrategy.valueOf(outputStrategy.toUpperCase()));
        }
    }

    @Override
    public void setOutputStrategy(final OutputStrategy outputStrategy) {
        if (outputStrategy != null) {
            this.outputStrategy = outputStrategy;
        }
    }

    @Override
    public void setThreadCount(final Integer threadCount) {
        if (threadCount != null) {
            Preconditions.checkArgument(threadCount > 0, "Thread count must be greater than zero");
            this.threadCount = threadCount;
        }
    }

    @Override
    public void setIncludeInlineScripts(final Boolean includeInlineScripts) {
        if (includeInlineScripts != null) {
            this.includeInlineScripts = includeInlineScripts;
        }
    }

    @Override
    public void setBackgroundJavaScriptTimeout(final Long backgroundJavaScriptTimeout) {
        if (backgroundJavaScriptTimeout != null) {
            this.backgroundJavaScriptTimeout = backgroundJavaScriptTimeout;
        }
    }

    @Override
    public void setSourcesToPreload(final String sourcesToPreload) {
        if (sourcesToPreload != null) {
            this.sourcesToPreload = sourcesToPreload;
        }
    }

    @Override
    public void setSourcesToPreloadEncoding(final String sourcesToPreloadEncoding) {
        if (sourcesToPreloadEncoding != null) {
            this.sourcesToPreloadEncoding = sourcesToPreloadEncoding;
        }
    }

    @Override
    public void setBrowserVersion(final String browserVersionAsString) {
        if (browserVersionAsString != null) {
            try {
                logger.info("Setting {} as browser version", browserVersionAsString);

                final BrowserVersion browserVersion = (BrowserVersion) BrowserVersion.class.getField(
                        browserVersionAsString).get(BrowserVersion.class);

                localClient.setBrowserVersion(browserVersion);
            } catch (final Exception e) {
                logger.error("Invalid browser version: {}", browserVersionAsString);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setReportFormats(final String reportFormatString) {
        if (reportFormatString == null) {
            return;
        }

        final Iterable<String> reportFormats = Splitter.on(",")
                .omitEmptyStrings()
                .trimResults()
                .split(reportFormatString);

        logger.info("Setting {} as report formats", Joiner.on(", ").join(reportFormats));

        this.reportFormats = Sets.newHashSet(Iterables.transform(reportFormats, new Function<String, ReportFormat>() {
            @Override
            public ReportFormat apply(final String input) {
                return ReportFormat.valueOf(input.trim().toUpperCase());
            }
        }));
    }

}
