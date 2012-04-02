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

    private final String coverageVariableName;
    private final List<URL> tests;
    private List<String> ignorePatterns;

    private final STGroup stringTemplateGroup;
    private final File outputDir;

    private String wholeRunName = "all";

    private boolean outputInstrumentedFiles;

    public CoverageGenerator(final String coverageVariableName, final List<URL> tests, final File outputDir) {
        this.coverageVariableName = coverageVariableName;
        this.tests = tests;
        this.outputDir = outputDir;

        stringTemplateGroup = new STGroupDir("stringTemplates", '$', '$');
    }

    public void run() throws IOException {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Couldn't create output directory");
        }

        final RunStats totalStats = new RunStats(wholeRunName);

        for (final URL test : tests) {
            runTest(test);
        }

        writeRunStats(wholeRunName, totalStats);
    }

    private void runTest(final URL test) throws IOException {
        final WebClient client = localClient.get();

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(
                coverageVariableName, ignorePatterns, outputDir, outputInstrumentedFiles);

        client.setScriptPreProcessor(instrumenter);

        final Page page = client.getPage(test);

        if (page instanceof HtmlPage) {
            final HtmlPage htmlPage = (HtmlPage) page;

            client.waitForBackgroundJavaScript(30000);
            client.setScriptPreProcessor(null);

            final String testName = new File(test.toString()).getName();
            final RunStats stats = new RunStats(testName);
            final NativeObject coverageData = (NativeObject) htmlPage.executeJavaScript(coverageVariableName)
                    .getJavaScriptResult();

            writeStatsOfAllFiles(testName, instrumenter, stats, coverageData);
            writeRunStats(testName, stats);
        }
    }

    private void writeStatsOfAllFiles(
            final String testName,
            final ScriptInstrumenter instrumenter,
            final RunStats stats,
            final NativeObject allCoverageData) throws IOException {

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Scanner in = new Scanner(data.getSourceCode());
            final NativeObject coverageData = (NativeObject) allCoverageData.get(data.getHashedSourceName());

            int statementsExecuted = 0;
            int statements = data.getNumberOfStatements();

            final String jsFileName = data.getSourceName();
            final String fileCoverageFilename = testName + "-" + new File(jsFileName).getName() + ".html";

            final List<LineCoverageRecord> lineCoverageRecords = Lists.newLinkedList();

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

                    if (timesLineExecuted > 0) {
                        statementsExecuted++;
                    }
                }

                // using lineCount instead of lineNr, see ScriptData#getLineNumberOfFirstStatement()
                lineCoverageRecords.add(new LineCoverageRecord(lineCount, timesLineExecuted, line, executable));
            }

            final FileStats fileStats = stats.add(jsFileName, fileCoverageFilename, statements,
                    statementsExecuted, lineCoverageRecords);

            stringTemplateGroup.getInstanceOf("lineByLineCoverageReport")
                    .add("stats", fileStats)
                    .write(new File(outputDir, fileCoverageFilename), new ErrorLogger());
        }
    }

    private void writeRunStats(final String testName, final RunStats stats) throws IOException {
        stringTemplateGroup.getInstanceOf("runStats")
                .add("stats", stats)
                .write(new File(outputDir, new File(testName).getName() + "-report.html"), new ErrorLogger());
    }

    public void setIgnorePatterns(final List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    public void setOutputInstrumentedFiles(final boolean outputInstrumentedFiles) {
        this.outputInstrumentedFiles = outputInstrumentedFiles;
    }

    public void setWholeRunName(String wholeRunName) {
        this.wholeRunName = wholeRunName;
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
