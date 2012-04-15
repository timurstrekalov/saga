package com.github.timurstrekalov.saga.core;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import org.apache.commons.lang.Validate;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.misc.STMessage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class CoverageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);

    private static final IncorrectnessListener quietIncorrectnessListener = new IncorrectnessListener() {
        @Override
        public void notify(final String message, final Object origin) {
            logger.debug(message);
        }
    };

    private static final ThreadLocal<WebClient> localClient = new ThreadLocal<WebClient>() {
        @Override
        protected WebClient initialValue() {
            final WebClient client = new WebClient(BrowserVersion.FIREFOX_3_6);
            client.setIncorrectnessListener(quietIncorrectnessListener);
            client.setJavaScriptEnabled(true);
            client.setAjaxController(new NicelyResynchronizingAjaxController());
            return client;
        }
    };

    private final File baseDir;
    private final String includes;
    private final String excludes;
    private final File outputDir;

    private Collection<String> noInstrumentPatterns = Collections.emptyList();
    private boolean outputInstrumentedFiles;

    private final STGroup stringTemplateGroup;

    private String coverageVariableName = "__coverage_data";

    private String reportName = "total";
    private String instrumentedFileDirectoryName = "instrumented";
    private boolean cacheInstrumentedCode = true;

    private OutputStrategy outputStrategy = OutputStrategy.TOTAL;

    private int threadCount = Runtime.getRuntime().availableProcessors();

    public CoverageGenerator(final File baseDir, final String includes, final File outputDir) {
        this(baseDir, includes, null, outputDir);
    }

    public CoverageGenerator(final File baseDir, final String includes, final String excludes, final File outputDir) {
        this.baseDir = baseDir;
        this.includes = includes;
        this.excludes = excludes;
        this.outputDir = outputDir;

        stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');
    }

    public void run() throws IOException {
        FileUtils.mkdir(outputDir.getAbsolutePath());

        @SuppressWarnings("unchecked")
        final List<File> tests = FileUtils.getFiles(baseDir, includes, excludes);

        if (tests.isEmpty()) {
            logger.warn("No tests found, exiting");
            return;
        }

        logger.info("Using up to {} threads", threadCount);
        logger.info("Output strategy set to {}", outputStrategy);

        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CompletionService<RunStats> completionService = new ExecutorCompletionService<RunStats>(executorService);

        for (final File test : tests) {
            completionService.submit(new Callable<RunStats>() {
                @Override
                public RunStats call() {
                    logger.info("Running {}", test.getAbsoluteFile().toURI().normalize().getPath());

                    try {
                        final RunStats runStats = runTest(test);

                        if (outputStrategy.contains(OutputStrategy.PER_TEST)) {
                            writeRunStats(runStats);
                        }

                        return runStats;
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        final List<RunStats> allRunStats = Lists.newLinkedList();

        for (int i = 0; i < tests.size(); i++) {
            try {
                final Future<RunStats> future = completionService.take();
                final RunStats runStats = future.get();

                allRunStats.add(runStats);
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }
        }

        logger.info("Test run finished");

        executorService.shutdown();

        if (outputStrategy.contains(OutputStrategy.TOTAL)) {
            final RunStats totalStats = new RunStats(new File(outputDir, reportName), "Total coverage report");

            for (final RunStats runStats : allRunStats) {
                for (final FileStats fileStats : runStats) {
                    totalStats.add(fileStats);
                }
            }

            writeRunStats(totalStats);
        }
    }

    private RunStats runTest(final File test) throws IOException {
        final WebClient client = localClient.get();

        final File instrumentedFileDirectory = new File(outputDir, instrumentedFileDirectoryName);
        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(client.getJavaScriptEngine().getContextFactory(),
                coverageVariableName);

        instrumenter.setIgnorePatterns(noInstrumentPatterns);

        if (outputInstrumentedFiles) {
            FileUtils.mkdir(instrumentedFileDirectory.getAbsolutePath());

            instrumenter.setOutputDir(instrumentedFileDirectory);
            instrumenter.setOutputInstrumentedFiles(outputInstrumentedFiles);
        }

        instrumenter.setCacheInstrumentedCode(cacheInstrumentedCode);

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test.toURI().toURL());

        if (page instanceof HtmlPage) {
            final HtmlPage htmlPage = (HtmlPage) page;

            client.waitForBackgroundJavaScript(30000);
            client.setScriptPreProcessor(null);

            final NativeObject coverageData = (NativeObject) htmlPage.executeJavaScript(coverageVariableName)
                    .getJavaScriptResult();

            return collectAndWriteRunStats(test, instrumenter, coverageData);
        }

        return null;
    }

    private RunStats collectAndWriteRunStats(
            final File test,
            final ScriptInstrumenter instrumenter,
            final NativeObject allCoverageData) throws IOException {

        final RunStats runStats = new RunStats(test);

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Scanner in = new Scanner(data.getSourceCode());
            final NativeObject coverageData = (NativeObject) allCoverageData.get(data.getSourceName());

            final String jsFileName = data.getSourceName();

            final List<LineCoverageRecord> lineCoverageRecords = Lists.newArrayList();

            for (int lineCount = 1, lineNr = data.getLineNumberOfFirstStatement(), lengthCountdown = 0; in.hasNext();
                 lineCount++, lineNr++) {

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
                lineCoverageRecords.add(new LineCoverageRecord(lineCount, timesLineExecuted, line));
            }

            try {
                final String name = jsFileName.startsWith("script in ") ? jsFileName :
                        new URI(jsFileName).normalize().toString();

                runStats.add(new FileStats(name, lineCoverageRecords));
            } catch (final URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        return runStats;
    }

    private void writeRunStats(final RunStats stats) throws IOException {
        final URI relativeTestUri = baseDir.toURI().relativize(stats.test.toURI());
        final File fileOutputDir = new File(new File(outputDir.toURI().resolve(relativeTestUri)).getParent());

        FileUtils.mkdir(fileOutputDir.getAbsolutePath());

        final File outputFile = new File(fileOutputDir, stats.getReportName());

        logger.info("Writing coverage report: {}", outputFile.getAbsoluteFile());

        synchronized (stringTemplateGroup) {
            stringTemplateGroup.getInstanceOf("runStats")
                    .add("stats", stats)
                    .write(outputFile, new ErrorLogger());
        }
    }

    public void setNoInstrumentPatterns(final Collection<String> noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            this.noInstrumentPatterns = noInstrumentPatterns;
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

    private static final class ErrorLogger implements STErrorListener {
        @Override
        public void compileTimeError(final STMessage msg) {
            logger.error(msg.toString());
        }

        @Override
        public void runTimeError(final STMessage msg) {
            logger.error(msg.toString());
        }

        @Override
        public void IOError(final STMessage msg) {
            logger.error(msg.toString());
        }

        @Override
        public void internalError(final STMessage msg) {
            logger.error(msg.toString());
        }
    }
}
