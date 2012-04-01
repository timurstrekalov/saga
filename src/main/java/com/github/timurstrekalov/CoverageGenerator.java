package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.ErrorBuffer;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

public class CoverageGenerator {

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

    private static final String COVERAGE_REPORT_TEMPLATE;

    static {
        try {
            COVERAGE_REPORT_TEMPLATE = IOUtils.toString(CoverageGenerator.class.getResource("/coverage-report.stg"));
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

    public CoverageGenerator(final String coverageVariableName, final List<URI> tests) {
        this.coverageVariableName = coverageVariableName;
        this.tests = tests;
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
                StringWriter body = new StringWriter();
                final PrintWriter out = new PrintWriter(body);

                out.println("<table>");

                final Scanner in = new Scanner(entry.getValue());
                final Map<Integer, Integer> lineLengths = preProcessor.getExecutableLines();

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

                    padAndWrite(out, lineNr, line, coverage);
                }

                out.println("</table>");

                IOUtils.closeQuietly(out);

                final ST st = new ST(COVERAGE_REPORT_TEMPLATE, '$', '$');
                st.add("body", body);

                final STErrorListener buf = new ErrorBuffer();

                st.write(new File(new File(entry.getKey() + ".html").getName()), buf);

                System.out.println(buf.toString());
            }
        }
    }

    private static void padAndWrite(final PrintWriter out, final int lineNr, final String line, final int coverage) {
        final String cssClass;

        if (coverage == -1) {
            cssClass = "";
        } else if (coverage > 0) {
            cssClass = "covered";
        } else {
            cssClass = "not-covered";
        }

        String styledLine = jsStringPattern.matcher(line).replaceAll("<span class=\"string\">$1</span>");
        styledLine = jsNumberPattern.matcher(styledLine).replaceAll("<span class=\"number\">$1</span>");
        styledLine = reservedKeywordsPattern.matcher(styledLine).replaceAll("<span class=\"keyword\">$1</span>");

        if (coverage > -1) {
            out.printf("<tr class=\"%s\">%n" +
                       "    <th><div>%d</div></th>%n" +
                       "    <td class=\"coverage\"><div>%d</div></td>%n" +
                       "    <td><pre>%s</pre></td>%n" +
                       "</tr>%n", cssClass, lineNr, coverage, styledLine);
        } else {
            out.printf("<tr>%n" +
                       "    <th><div>%d</div></th>%n" +
                       "    <td class=\"coverage\"><div></div></td>%n" +
                       "    <td><pre>%s</pre></td>%n" +
                       "</tr>%n", lineNr, styledLine);
        }
    }

    public void setIgnorePatterns(final List<String> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

}
