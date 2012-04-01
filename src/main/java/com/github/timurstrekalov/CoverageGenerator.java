package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Lists;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupDir;
import org.stringtemplate.v4.misc.STMessage;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class CoverageGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CoverageGenerator.class);

    private static final ThreadLocal<WebClient> localClient = new ThreadLocal<WebClient>() {
        @Override
        protected WebClient initialValue() {
            return new WebClient();
        }
    };

    private static final IncorrectnessListener quietIncorrectnessListener = new IncorrectnessListener() {

        @Override
        public void notify(final String message, final Object origin) {
        }

    };

    private static final String COVERAGE_REPORT_ST;

    static {
        try {
            COVERAGE_REPORT_ST = IOUtils.toString(CoverageGenerator.class.getResource("/st/coverageReport.st"));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final String[] reservedKeywords = {
        "break",
        "case",
        "catch",
        "continue",
        "debugger",
        "default",
        "delete",
        "do",
        "else",
        "finally",
        "for",
        "function",
        "if",
        "in",
        "instanceof",
        "new",
        "return",
        "switch",
        "this",
        "throw",
        "try",
        "typeof",
        "var",
        "void",
        "while",
        "with"
    };

    private static final Pattern reservedKeywordsPattern = Pattern.compile(String.format("\\b(%s)\\b",
            StringUtils.join(reservedKeywords, '|')));

    private static final Pattern jsStringPattern = Pattern.compile("('.*'|\".*\")");
    private static final Pattern jsNumberPattern = Pattern.compile("\\b(\\d+(?:\\.\\d+)?)\\b");

    private final String coverageVariableName;
    private final List<URI> tests;
    private List<String> ignorePatterns;

    private final STGroup stg;

    public CoverageGenerator(final String coverageVariableName, final List<URI> tests) {
        this.coverageVariableName = coverageVariableName;
        this.tests = tests;

        stg = new STGroupDir("st", '$', '$');
    }

    public void run() throws IOException {
        for (final URI test : tests) {
            runTest(test);
        }
    }

    private void runTest(final URI test) throws IOException {
        final WebClient client = localClient.get();
        client.setIncorrectnessListener(quietIncorrectnessListener);

        final InstrumentingJavascriptPreProcessor preProcessor = new InstrumentingJavascriptPreProcessor(
                coverageVariableName, ignorePatterns);

        client.setScriptPreProcessor(preProcessor);

        final Page page = client.getPage(test.toURL());

        if (page instanceof HtmlPage) {
            final HtmlPage htmlPage = (HtmlPage) page;

            client.waitForBackgroundJavaScript(30000);
            client.setScriptPreProcessor(null);

            final NativeObject cov = (NativeObject) htmlPage.executeJavaScript(coverageVariableName)
                    .getJavaScriptResult();

            for (final Map.Entry<String, String> entry : preProcessor.getSourceCodeMap().entrySet()) {
                final Scanner in = new Scanner(entry.getValue());
                final Map<Integer, Integer> lineLengths = preProcessor.getExecutableLines();
                final List<LineCoverage> lines = Lists.newLinkedList();

                for (int lineNr = 1, lengthCountdown = 0; in.hasNext(); lineNr++) {
                    final String line = in.nextLine();

                    final Double coverageEntry = (Double) cov.get(lineNr);
                    final int coverage;

                    if (coverageEntry == null) {
                        final int lineLength = line.trim().length();

                        if (lengthCountdown > 0 && lineLength > 0) {
                            lengthCountdown -= lineLength;
                            coverage = -1;
                        } else if (!lineLengths.containsKey(lineNr)) {
                            coverage = -1;
                        } else {
                            coverage = 0;
                        }
                    } else {
                        coverage = coverageEntry.intValue();
                        lengthCountdown = lineLengths.get(lineNr);
                    }

                    lines.add(newLineCoverage(lineNr, coverage, line));
                }

                final ST st = stg.getInstanceOf("coverageReport");
                st.add("lines", lines);

                st.write(new File(new File(entry.getKey() + ".html").getName()), new STErrorListener() {
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
                });
            }
        }
    }

    private static LineCoverage newLineCoverage(final int lineNr, final int coverage, final String line) {
        String styledLine = jsStringPattern.matcher(line).replaceAll("<span class=\"string\">$1</span>");
        styledLine = jsNumberPattern.matcher(styledLine).replaceAll("<span class=\"number\">$1</span>");
        styledLine = reservedKeywordsPattern.matcher(styledLine).replaceAll("<span class=\"keyword\">$1</span>");

        return new LineCoverage(lineNr, coverage, styledLine);
    }

    public void setIgnorePatterns(final List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    private static final class LineCoverage {

        public final int lineNr;
        public final int coverage;
        public final String line;
        public final boolean executable;
        public final String cssClass;

        LineCoverage(final int lineNr, final int coverage, final String line) {
            this.lineNr = lineNr;
            this.coverage = coverage;
            this.line = line;

            executable = coverage > -1;

            if (coverage == -1) {
                cssClass = "";
            } else if (coverage > 0) {
                cssClass = "covered";
            } else {
                cssClass = "not-covered";
            }
        }
    }

}
