package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
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

public class CoverageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);

    private static final IncorrectnessListener quietIncorrectnessListener = new IncorrectnessListener() {
        @Override
        public void notify(final String message, final Object origin) {
        }
    };

    private static final ThreadLocal<WebClient> localClient = new ThreadLocal<WebClient>() {
        @Override
        protected WebClient initialValue() {
            final WebClient client = new WebClient();
            client.setIncorrectnessListener(quietIncorrectnessListener);
            return client;
        }
    };


    private Collection<String> noInstrumentPatterns;
    private boolean outputInstrumentedFiles;

    private final STGroup stringTemplateGroup;
    
    private String coverageVariableName = "__coverage_data";
    private File[] tests;
    private File outputDir;

    private String reportName = "all";
    private String instrumentedFileDirectoryName = "instrumented";

    private RunStats totalStats;

    public CoverageGenerator() {
        stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');
    }

    public void run() throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Couldn't create output directory");
        }

        totalStats = new RunStats(reportName);

        for (final File test : tests) {
            runTest(test.toURI().toURL());
        }

        writeRunStats(totalStats);
    }

    private void runTest(final URL test) throws IOException {
        final WebClient client = localClient.get();

        final File instrumentedFileDirectory = new File(outputDir, instrumentedFileDirectoryName);
        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(coverageVariableName);

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

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test);

        if (page instanceof HtmlPage) {
            final HtmlPage htmlPage = (HtmlPage) page;

            client.waitForBackgroundJavaScript(30000);
            client.setScriptPreProcessor(null);

            final String runName = new File(test.toString()).getName();
            final NativeObject coverageData = (NativeObject) htmlPage.executeJavaScript(coverageVariableName)
                    .getJavaScriptResult();

            collectAndWriteRunStats(runName, instrumenter, coverageData);
        }
    }

    private void collectAndWriteRunStats(
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
            totalStats.add(fileStats);
        }

        writeRunStats(runStats);
    }

    private void writeRunStats(final RunStats stats) throws IOException {
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
