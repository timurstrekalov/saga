package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.JavaScriptPage;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;
import com.github.timurstrekalov.saga.core.reporter.CsvReporter;
import com.github.timurstrekalov.saga.core.reporter.HtmlReporter;
import com.github.timurstrekalov.saga.core.reporter.PdfReporter;
import com.github.timurstrekalov.saga.core.reporter.RawReporter;
import com.github.timurstrekalov.saga.core.reporter.Reporter;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCoverageGenerator implements CoverageGenerator {

    private static final String TOTAL_REPORT_NAME = "total";
    private static final String INSTRUMENTED_FILE_DIRECTORY_NAME = "instrumented";
    private static final String INLINE_SCRIPT_RE = ".+__from_\\d+_\\d+_to_\\d+_\\d+$";

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoverageGenerator.class);

    private static final Map<ReportFormat, Class<? extends Reporter>> reporters = ImmutableMap.<ReportFormat, Class<? extends Reporter>>builder()
            .put(ReportFormat.HTML, HtmlReporter.class)
            .put(ReportFormat.RAW, RawReporter.class)
            .put(ReportFormat.CSV, CsvReporter.class)
            .put(ReportFormat.PDF, PdfReporter.class)
            .build();

    private static final ThreadLocal<WebClient> localClient = new ThreadLocal<WebClient>();

    static {
        HtmlUnitUtil.silenceHtmlUnitLogging();
    }

    private final Config config = new InstanceFieldPerPropertyConfig();

    @Override
    public void instrumentAndGenerateReports() throws IOException {
        Preconditions.checkNotNull(config.getBaseDir(), "baseDir cannot be null");
        Preconditions.checkNotNull(config.getOutputDir(), "outputDir cannot be null");

        final URI baseUri = config.getBaseUri();
        final List<URI> tests = fetchTests(baseUri);

        if (tests.isEmpty()) {
            logger.warn("No tests found, exiting");
            return;
        }

        final int actualThreadCount = Math.min(config.getThreadCount(), tests.size());
        logger.info("Using up to {} threads", actualThreadCount);

        final OutputStrategy outputStrategy = config.getOutputStrategy();
        logger.info("Output strategy set to {}", outputStrategy);

        if (!config.isIncludeInlineScripts()) {
            config.getNoInstrumentPatterns().add(INLINE_SCRIPT_RE);
            config.getNoInstrumentPatterns().add(".+JavaScriptStringJob");
            config.getNoInstrumentPatterns().add(".+#\\d+\\(eval\\)\\(\\d+\\)");
        }

        if (!config.getNoInstrumentPatterns().isEmpty()) {
            logger.info("Using the following no-instrument patterns:\n\t{}", StringUtils.join(config.getNoInstrumentPatterns(), "\n\t"));
        }

        final File outputDir = config.getOutputDir();
        FileUtils.mkdir(outputDir.getAbsolutePath());

        final Collection<Pattern> ignorePatterns = createPatterns();
        final File instrumentedFileDirectory = new File(outputDir, INSTRUMENTED_FILE_DIRECTORY_NAME);
        final RunStats totalStats = new RunStats(baseUri.relativize(URI.create(TOTAL_REPORT_NAME)), "Total coverage report");
        totalStats.setSortBy(config.getSortBy());
        totalStats.setOrder(config.getOrder());

        maybePreloadSources(ignorePatterns, instrumentedFileDirectory, totalStats);

        final ExecutorService executorService = Executors.newFixedThreadPool(actualThreadCount);
        final CompletionService<RunStats> completionService = new ExecutorCompletionService<RunStats>(executorService);

        for (final URI test : tests) {
            completionService.submit(new Callable<RunStats>() {
                @Override
                public RunStats call() {
                    logger.info("Running test at {}", test.toString());

                    try {
                        final RunStats runStats = runTest(test, ignorePatterns, instrumentedFileDirectory);

                        if (runStats == RunStats.EMPTY) {
                            logger.warn("No actual test run for file: {}", test);
                        } else if (outputStrategy.contains(OutputStrategy.PER_TEST)) {
                            if (UriUtil.isFileUri(test)) {
                                writeRunStats(runStats);
                            } else {
                                logger.warn("Output strategy PER_TEST only makes sense in the context of tests run off the filesystem, ignoring");
                            }
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
            for (final URI test : tests) {
                try {
                    final Future<RunStats> future = completionService.take();
                    final RunStats runStats = future.get();

                    allRunStats.add(runStats);
                } catch (final Exception e) {
                    logger.warn("Error running test {}: {}", test.toString(), e.getMessage());
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

    private void maybePreloadSources(
            final Collection<Pattern> ignorePatterns,
            final File instrumentedFileDirectory,
            final RunStats totalStats) throws IOException {
        final ScriptInstrumenter instrumenter = newInstrumenter(ignorePatterns, instrumentedFileDirectory,
                getThreadLocalWebClient().getJavaScriptEngine().getContextFactory());

        new FileSystemSourcePreloader().preloadSources(config, instrumenter, totalStats);
    }

    private Collection<Pattern> createPatterns() {
        return Collections2.transform(config.getNoInstrumentPatterns(), new Function<String, Pattern>() {
            @Override
            public Pattern apply(final String input) {
                return Pattern.compile(input);
            }
        });
    }

    private RunStats runTest(final URI test, final Collection<Pattern> ignorePatterns, final File instrumentedFileDirectory)
            throws IOException {
        final WebClient client = getThreadLocalWebClient();
        final ScriptInstrumenter instrumenter = newInstrumenter(ignorePatterns, instrumentedFileDirectory,
                client.getJavaScriptEngine().getContextFactory());

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test.toURL());
        final HtmlPage htmlPage;

        if (page instanceof HtmlPage) {
            htmlPage = (HtmlPage) page;
        } else if (page instanceof JavaScriptPage) {
            final JavaScriptPage javaScriptPage = (JavaScriptPage) page;

            final WebResponse webResponse = javaScriptPage.getWebResponse();

            htmlPage = new HtmlPage(javaScriptPage.getUrl(), webResponse, javaScriptPage.getEnclosingWindow());
            client.getJavaScriptEngine().execute(htmlPage, webResponse.getContentAsString(), test.toString(), 1);
        } else {
            throw new RuntimeException("Unsupported page type: " + page.getUrl() + " (of class " + page.getClass() + ")");
        }

        return collectAndRunStats(client, htmlPage, test, instrumenter);
    }

    private ScriptInstrumenter newInstrumenter(
            final Collection<Pattern> ignorePatterns,
            final File instrumentedFileDirectory,
            final HtmlUnitContextFactory contextFactory) {

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(config, contextFactory);

        instrumenter.setIgnorePatterns(ignorePatterns);

        if (config.isOutputInstrumentedFiles()) {
            FileUtils.mkdir(instrumentedFileDirectory.getAbsolutePath());
            instrumenter.setInstrumentedFileDirectory(instrumentedFileDirectory);
        }

        return instrumenter;
    }

    private RunStats collectAndRunStats(
            final WebClient client,
            final HtmlPage htmlPage,
            final URI test,
            final ScriptInstrumenter instrumenter) throws IOException {

        client.waitForBackgroundJavaScript(config.getBackgroundJavaScriptTimeout());
        client.setScriptPreProcessor(null);

        final Object javaScriptResult = htmlPage.executeJavaScript("window." + Config.COVERAGE_VARIABLE_NAME)
                .getJavaScriptResult();

        if (!(javaScriptResult instanceof Undefined)) {
            return collectAndWriteRunStats(test, instrumenter, (NativeObject) javaScriptResult);
        }

        return RunStats.EMPTY;
    }

    @SuppressWarnings("unchecked")
    private RunStats collectAndWriteRunStats(
            final URI test,
            final ScriptInstrumenter instrumenter,
            final NativeObject allCoverageData) throws IOException {
        final RunStats runStats = new RunStats(test);
        runStats.setSortBy(config.getSortBy());
        runStats.setOrder(config.getOrder());

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Map<Integer, Double> coverageData = (Map<Integer, Double>) allCoverageData.get(data.getSourceUriAsString());
            final FileStats fileStats = data.generateFileStats(config.getBaseUri(), coverageData);
            runStats.add(fileStats);
        }

        return runStats;
    }

    private void writeRunStats(final RunStats stats) throws IOException {
        for (final ReportFormat reportFormat : config.getReportFormats()) {
            reporterFor(reportFormat).writeReport(config.getBaseUri(), config.getOutputDir(), stats);
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
    public Config getConfig() {
        return config;
    }

    private WebClient getThreadLocalWebClient() {
        if (localClient.get() == null) {
            localClient.set(WebClientFactory.newInstance(config));
        }

        return localClient.get();
    }

    private List<URI> fetchTests(final URI baseDir) throws IOException {
        final TestFetcher fetcher = TestFetcherFactory.newInstance(baseDir);
        final List<URI> tests = fetcher.fetch(baseDir, config.getIncludes(), config.getExcludes());

        logger.info("{} tests found", tests.size());

        return tests;
    }

}
