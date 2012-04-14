package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.misc.STMessage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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

    private Collection<String> noInstrumentPatterns;
    private boolean outputInstrumentedFiles;

    private final STGroup stringTemplateGroup;
    
    private String coverageVariableName = "__coverage_data";
    private File[] tests;
    private File outputDir;

    private String reportName = "total";
    private String instrumentedFileDirectoryName = "instrumented";
    private boolean cacheInstrumentedCode = true;

    private OutputStrategy outputStrategy = OutputStrategy.TOTAL;

    public CoverageGenerator() {
        stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');
    }

    public void run() throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Couldn't create output directory");
        }

        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final CompletionService<RunStats> completionService = new ExecutorCompletionService<RunStats>(executorService);

        for (final File test : tests) {
            completionService.submit(new Callable<RunStats>() {
                @Override
                public RunStats call() {
                    logger.info("Running {}", test.getAbsoluteFile().toURI().normalize().getPath());

                    try {
                        final RunStats runStats = runTest(test.toURI().toURL());

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

        for (int i = 0; i < tests.length; i++) {
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
            final RunStats totalStats = new RunStats(reportName);

            for (final RunStats runStats : allRunStats) {
                for (final FileStats fileStats : runStats.getFileStats()) {
                    totalStats.add(fileStats);
                }
            }

            writeRunStats(totalStats);
        }
    }

    private RunStats runTest(final URL test) throws IOException {
        final WebClient client = localClient.get();

        final File instrumentedFileDirectory = new File(outputDir, instrumentedFileDirectoryName);
        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(client.getJavaScriptEngine().getContextFactory(),
                coverageVariableName);

        if (noInstrumentPatterns != null) {
            instrumenter.setIgnorePatterns(noInstrumentPatterns);
        }

        if (outputInstrumentedFiles) {
            if (!instrumentedFileDirectory.exists() && !instrumentedFileDirectory.mkdirs()) {
                throw new RuntimeException("Can't create " + instrumentedFileDirectory);
            }

            instrumenter.setOutputDir(instrumentedFileDirectory);
            instrumenter.setOutputInstrumentedFiles(outputInstrumentedFiles);
        }

        instrumenter.setCacheInstrumentedCode(cacheInstrumentedCode);

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test);

        if (page instanceof HtmlPage) {
            final HtmlPage htmlPage = (HtmlPage) page;

            client.waitForBackgroundJavaScript(30000);
            client.setScriptPreProcessor(null);

            final String runName = new File(test.toString()).getName();
            final NativeObject coverageData = (NativeObject) htmlPage.executeJavaScript(coverageVariableName)
                    .getJavaScriptResult();

            return collectAndWriteRunStats(runName, instrumenter, coverageData);
        }

        return null;
    }

    private RunStats collectAndWriteRunStats(
            final String runName,
            final ScriptInstrumenter instrumenter,
            final NativeObject allCoverageData) throws IOException {

        final RunStats runStats = new RunStats(runName);

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Scanner in = new Scanner(data.getSourceCode());
            final NativeObject coverageData = (NativeObject) allCoverageData.get(data.getSourceName());

            final String jsFileName = data.getSourceName();
            final String fileCoverageFilename = new File(jsFileName).getName();

            final List<LineCoverageRecord> lineCoverageRecords = Lists.newArrayList();

            for (int lineCount = 1, lineNr = data.getLineNumberOfFirstStatement(), lengthCountdown = 0; in.hasNext();
                 lineCount++, lineNr++) {

                final String line = in.nextLine();

                final Double coverageEntry = (Double) coverageData.get(lineNr);
                final int timesLineExecuted;
                final boolean executable;

                if (coverageEntry == null) {
                    final int lineLength = line.trim().length();

                    if (lengthCountdown > 0 && lineLength > 0) {
                        lengthCountdown -= lineLength;
                        executable = false;
                    } else {
                        executable = data.hasStatement(lineNr);
                    }

                    timesLineExecuted = 0;
                } else {
                    timesLineExecuted = coverageEntry.intValue();
                    lengthCountdown = data.getStatementLength(lineNr);
                    executable = true;
                }

                // using lineCount instead of lineNr, see ScriptData#getLineNumberOfFirstStatement()
                lineCoverageRecords.add(new LineCoverageRecord(lineCount, timesLineExecuted, line, executable));
            }

            final FileStats fileStats = new FileStats(jsFileName, fileCoverageFilename, lineCoverageRecords);

            runStats.add(fileStats);
        }

        return runStats;
    }

    private void writeRunStats(final RunStats stats) throws IOException {
        logger.debug("Writing run statistics, name: {}", stats.runName);
        stringTemplateGroup.getInstanceOf("runStats")
                .add("stats", stats)
                .write(new File(outputDir, stats.runName + "-report.html"), new ErrorLogger());
    }

    public void setNoInstrumentPatterns(final Collection<String> noInstrumentPatterns) {
        this.noInstrumentPatterns = noInstrumentPatterns;
    }

    public void setOutputInstrumentedFiles(final boolean outputInstrumentedFiles) {
        this.outputInstrumentedFiles = outputInstrumentedFiles;
    }

    public void setReportName(final String reportName) {
        this.reportName = reportName;
    }

    public void setInstrumentedFileDirectoryName(final String instrumentedFileDirectoryName) {
        this.instrumentedFileDirectoryName = instrumentedFileDirectoryName;
    }

    public void setCoverageVariableName(final String coverageVariableName) {
        this.coverageVariableName = coverageVariableName;
    }

    public void setTests(final File[] tests) {
        this.tests = tests;
    }

    public void setOutputDir(final File outputDir) {
        this.outputDir = outputDir;
    }

    public void setCacheInstrumentedCode(final boolean cacheInstrumentedCode) {
        this.cacheInstrumentedCode = cacheInstrumentedCode;
    }

    public void setOutputStrategy(OutputStrategy outputStrategy) {
        this.outputStrategy = outputStrategy;
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
