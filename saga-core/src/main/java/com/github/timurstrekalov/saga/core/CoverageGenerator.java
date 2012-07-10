package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.JavaScriptPage;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import net.sourceforge.htmlunit.corejs.javascript.Undefined;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class CoverageGenerator {

    private static final Configuration config;

    static {
        try {
            config = new PropertiesConfiguration("app.properties");
        } catch (final ConfigurationException e) {
            throw new RuntimeException("Error loading configuration", e);
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);
    private static final ThreadLocal<WebClient> localClient = new SagaWebClient();
    private static final String inlineScriptRe = ".+__from_\\d+_\\d+_to_\\d+_\\d+$";

    private final File baseDir;
    private final String includes;
    private final String excludes;
    private final File outputDir;

    private Set<String> noInstrumentPatterns = Sets.newHashSet();
    private boolean outputInstrumentedFiles;

    private final STGroup stringTemplateGroup;

    private String coverageVariableName = "__coverage_data";

    private String reportName = "total";
    private String instrumentedFileDirectoryName = "instrumented";
    private boolean cacheInstrumentedCode = true;

    private OutputStrategy outputStrategy = OutputStrategy.TOTAL;

    private int threadCount = Runtime.getRuntime().availableProcessors();

    private boolean includeInlineScripts = false;

    public CoverageGenerator(final File baseDir, final String includes, final File outputDir) {
        this(baseDir, includes, null, outputDir);
    }

    public CoverageGenerator(final File baseDir, final String includes, final String excludes, final File outputDir) {
        Validate.isTrue(baseDir.exists(), "baseDir doesn't exist");

        this.baseDir = baseDir;
        this.includes = includes;
        this.excludes = excludes;
        this.outputDir = outputDir;

        stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');

        // make HtmlUnit shut up
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");

        java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(Level.OFF);
        java.util.logging.Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.OFF);
    }

    public void run() throws IOException {
        FileUtils.mkdir(outputDir.getAbsolutePath());

        @SuppressWarnings("unchecked")
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
            noInstrumentPatterns.add(inlineScriptRe);
        }

        if (!noInstrumentPatterns.isEmpty()) {
            logger.info("Using the following no-instrument patterns:\n\t{}", StringUtils.join(noInstrumentPatterns, "\n\t"));
        }

        final Set<Pattern> ignorePatterns = createPatterns();

        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CompletionService<RunStats> completionService = new ExecutorCompletionService<RunStats>(executorService);

        for (final File test : tests) {
            completionService.submit(new Callable<RunStats>() {
                @Override
                public RunStats call() {
                    logger.info("Running {}", test.getAbsoluteFile().toURI().normalize().getPath());

                    try {
                        final RunStats runStats = runTest(test, ignorePatterns);

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
            final RunStats totalStats = new RunStats(new File(outputDir, reportName), "Total coverage report");

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

    private HashSet<Pattern> createPatterns() {
        return new HashSet<Pattern>(Collections2.transform(noInstrumentPatterns, new Function<String, Pattern>() {
            @Override
            public Pattern apply(final String input) {
                return Pattern.compile(input);
            }
        }));
    }

    private RunStats runTest(final File test, final Set<Pattern> ignorePatterns) throws IOException {
        final WebClient client = localClient.get();

        final File instrumentedFileDirectory = new File(outputDir, instrumentedFileDirectoryName);
        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(client.getJavaScriptEngine().getContextFactory(),
                coverageVariableName);

        instrumenter.setIgnorePatterns(ignorePatterns);

        if (outputInstrumentedFiles) {
            FileUtils.mkdir(instrumentedFileDirectory.getAbsolutePath());

            instrumenter.setOutputDir(instrumentedFileDirectory);
            instrumenter.setOutputInstrumentedFiles(outputInstrumentedFiles);
        }

        instrumenter.setCacheInstrumentedCode(cacheInstrumentedCode);

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test.toURI().toURL());
        final HtmlPage htmlPage;

        if (page instanceof HtmlPage) {
            htmlPage = (HtmlPage) page;
        } else if (page instanceof JavaScriptPage) {
            final JavaScriptPage javaScriptPage = (JavaScriptPage) page;

            final WebResponse webResponse = javaScriptPage.getWebResponse();

            htmlPage = new HtmlPage(javaScriptPage.getUrl(), webResponse, javaScriptPage.getEnclosingWindow());

            client.getJavaScriptEngine().execute(htmlPage, webResponse.getContentAsString(),
                    test.getAbsolutePath(), 1);
        } else {
            throw new RuntimeException("Unsupported page type: " + page.getUrl() + " (of class " + page.getClass()
                    + ")");
        }

        return collectAndRunStats(client, htmlPage, test, instrumenter);
    }

    private RunStats collectAndRunStats(
            final WebClient client,
            final HtmlPage htmlPage,
            final File test,
            final ScriptInstrumenter instrumenter) throws IOException {

        client.waitForBackgroundJavaScript(30000);
        client.setScriptPreProcessor(null);

        final Object javaScriptResult = htmlPage.executeJavaScript("window." + coverageVariableName)
                .getJavaScriptResult();

        if (!(javaScriptResult instanceof Undefined)) {
            return collectAndWriteRunStats(test, instrumenter, (NativeObject) javaScriptResult);
        }

        return RunStats.EMPTY;
    }

    private RunStats collectAndWriteRunStats(
            final File test,
            final ScriptInstrumenter instrumenter,
            final NativeObject allCoverageData) throws IOException {

        final RunStats runStats = new RunStats(test);

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Scanner in = new Scanner(data.getSourceCode());
            final NativeObject coverageData = (NativeObject) allCoverageData.get(data.getSourceName());

            final List<LineCoverageRecord> lineCoverageRecords = Lists.newArrayList();

            if (!data.getLineNumbersOfAllStatements().isEmpty()) {
                // pad with extra line coverage records if first executable statement is not the first line (comments
                // at the start of files)
                for (int lineNr = 1; lineNr < data.getLineNumberOfFirstStatement() && in.hasNext(); lineNr++) {
                    lineCoverageRecords.add(new LineCoverageRecord(lineNr, -1, in.nextLine()));
                }

                for (int lineNr = data.getLineNumberOfFirstStatement(), lengthCountdown = 0; in.hasNext(); lineNr++) {
                    final String line = in.nextLine();

                    final Double coverageEntry = (Double) coverageData.get(lineNr);
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
                        lengthCountdown = data.getStatementLength(lineNr);
                    }

                    // using lineCount instead of lineNr, see ScriptData#getLineNumberOfFirstStatement()
                    lineCoverageRecords.add(new LineCoverageRecord(lineNr, timesLineExecuted, line));
                }
            } else {
                for (int lineNr = 1; in.hasNext(); lineNr++) {
                    lineCoverageRecords.add(new LineCoverageRecord(lineNr,  -1, in.nextLine()));
                }
            }

            runStats.add(new FileStats(data.getSourceName(), lineCoverageRecords));
        }

        return runStats;
    }

    private void writeRunStats(final RunStats stats) throws IOException {
        final URI relativeTestUri = baseDir.toURI().relativize(stats.test.toURI());
        final File fileOutputDir = new File(new File(outputDir.toURI().resolve(relativeTestUri)).getParent());

        FileUtils.mkdir(fileOutputDir.getAbsolutePath());

        final File rawOutput = new File(fileOutputDir, stats.getRawReportName());
        final File htmlOutput = new File(fileOutputDir, stats.getReportName());

        synchronized (stringTemplateGroup) {
            final LoggingStringTemplateErrorListener listener = new LoggingStringTemplateErrorListener();

            logger.info("Writing raw coverage report: {}", rawOutput.getAbsoluteFile());
            stringTemplateGroup.getInstanceOf("runStatsRaw")
                    .add("stats", stats)
                    .write(rawOutput, listener);

            logger.info("Writing html coverage report: {}", htmlOutput.getAbsoluteFile());
            stringTemplateGroup.getInstanceOf("runStats")
                    .add("stats", stats)
                    .add("name", config.getString("app.name"))
                    .add("version", config.getString("app.version"))
                    .add("url", config.getString("app.url"))
                    .write(htmlOutput, listener);
        }
    }

    public void setNoInstrumentPatterns(final Collection<String> noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            this.noInstrumentPatterns = new HashSet<String>(noInstrumentPatterns);
        }
    }

    public void setNoInstrumentPatterns(final String[] noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            setNoInstrumentPatterns(ImmutableList.copyOf(noInstrumentPatterns));
        }
    }

    public void setOutputInstrumentedFiles(final Boolean outputInstrumentedFiles) {
        if (outputInstrumentedFiles != null) {
            this.outputInstrumentedFiles = outputInstrumentedFiles;
        }
    }

    public void setReportName(final String reportName) {
        if (reportName != null) {
            this.reportName = reportName;
        }
    }

    public void setInstrumentedFileDirectoryName(final String instrumentedFileDirectoryName) {
        if (instrumentedFileDirectoryName != null) {
            this.instrumentedFileDirectoryName = instrumentedFileDirectoryName;
        }
    }

    public void setCoverageVariableName(final String coverageVariableName) {
        if (coverageVariableName != null) {
            this.coverageVariableName = coverageVariableName;
        }
    }

    public void setCacheInstrumentedCode(final Boolean cacheInstrumentedCode) {
        if (cacheInstrumentedCode != null) {
            this.cacheInstrumentedCode = cacheInstrumentedCode;
        }
    }

    public void setOutputStrategy(final String outputStrategy) {
        if (outputStrategy != null) {
            setOutputStrategy(OutputStrategy.valueOf(outputStrategy.toUpperCase()));
        }
    }

    public void setOutputStrategy(final OutputStrategy outputStrategy) {
        if (outputStrategy != null) {
            this.outputStrategy = outputStrategy;
        }
    }

    public void setThreadCount(final Integer threadCount) {
        if (threadCount != null) {
            Validate.isTrue(threadCount > 0, "Thread count must be greater than zero");
            this.threadCount = threadCount;
        }
    }

    public void setIncludeInlineScripts(final Boolean includeInlineScripts) {
        if (includeInlineScripts != null) {
            this.includeInlineScripts = includeInlineScripts;
        }
    }

}
