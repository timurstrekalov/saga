package com.github.timurstrekalov;

import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import net.sourceforge.htmlunit.corejs.javascript.NativeObject;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

    private static final Pattern reservedKeywordsPattern = Pattern.compile("\\b(break|case|catch|continue|debugger|default|delete|do|else|finally|for|function|if|in|instanceof|new|return|switch|this|throw|try|typeof|var|void|while|with)\\b");

    private final List<URI> tests;
    private List<String> ignorePatterns;

    public CoverageGenerator(final List<URI> tests) {
        this.tests = tests;
    }

    public void run() throws IOException {
        for (final URI test : tests) {
            runTest(test);
        }
    }

    private static void writeHead(final PrintWriter out) {
        out.println("<head>");
        out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
        out.println("<title>Coverage</title>");
        out.println("<style>");

        out.println("    table {");
        out.println("        border-collapse: collapse;");
        out.println("    }");

        out.println("    th div, td div {");
        out.println("        text-align: center;");
        out.println("    }");

        out.println("    th, td, pre {");
        out.println("        font-size: 14px;");
        out.println("        font-weight: normal;");
        out.println("        font-family: Courier New;");
        out.println("    }");

        out.println("    th, td.coverage {");
        out.println("         padding: 0 8px;");
        out.println("         border-radius: 8px;");
        out.println("    }");

        out.println("    td {");
        out.println("         padding-left: 20px;");
        out.println("    }");

        out.println("    tr.covered td.coverage {");
        out.println("         background-color: lightgreen;");
        out.println("    }");

        out.println("    tr.not-covered td.coverage {");
        out.println("         background-color: lightpink;");
        out.println("    }");

        out.println("    span.reserved {");
        out.println("        color: #000080;");
        out.println("        font-weight: bold;");
        out.println("    }");

        out.println("</style>");
        out.println("</head>");
    }

    private void runTest(final URI test) throws IOException {
        final WebClient client = localClient.get();
        client.setIncorrectnessListener(quietIncorrectnessListener);

        final InstrumentingJavascriptPreProcessor preProcessor = new InstrumentingJavascriptPreProcessor(ignorePatterns);
        client.setScriptPreProcessor(preProcessor);

        final Page page = client.getPage(test.toURL());

        if (page instanceof HtmlPage) {
            final HtmlPage htmlPage = (HtmlPage) page;

            client.waitForBackgroundJavaScript(30000);
            client.setScriptPreProcessor(null);

            final NativeObject cov = (NativeObject) htmlPage.executeJavaScript("_COV").getJavaScriptResult();

            for (final Map.Entry<String, String> entry : preProcessor.getSourceCodeMap().entrySet()) {
                final PrintWriter out = new PrintWriter(new File(entry.getKey() + ".html").getName());

                out.println("<!DOCTYPE html");
                out.println("<html>");
                writeHead(out);
                out.println("<body>");
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
                out.println("</body>");
                out.println("</html>");

                IOUtils.closeQuietly(out);
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

        final String styledLine = reservedKeywordsPattern.matcher(line).replaceAll("<span class=\"reserved\">$1</span>");

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
