package com.github.timurstrekalov.saga.core.instrumentation;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import java.util.Collections;
import net.sourceforge.htmlunit.corejs.javascript.Parser;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstRoot;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HtmlUnitBasedScriptInstrumenter implements ScriptInstrumenter {

    private static final String INITIALIZING_CODE = String.format("%s = window.%s || {};%n", COVERAGE_VARIABLE_NAME, COVERAGE_VARIABLE_NAME);
    private static final String ARRAY_INITIALIZER = String.format("    %s['%%s'][%%d] = 0;%n", COVERAGE_VARIABLE_NAME);
    public static final String COMPLETION_MONITOR;

    private static final AtomicInteger evalCounter = new AtomicInteger();

    private static final Logger logger = LoggerFactory.getLogger(HtmlUnitBasedScriptInstrumenter.class);

    private static final Pattern inlineScriptRe = Pattern.compile("script in (.+) from \\((\\d+), (\\d+)\\) to \\((\\d+), (\\d+)\\)");
    private static final Pattern evalRe = Pattern.compile("(.+)(#|%23)(\\d+\\(eval\\))");
    private static final Pattern nonFileRe = Pattern.compile("JavaScriptStringJob");

    private static final ConcurrentMap<URI, ScriptData> instrumentedScriptCache = Maps.newConcurrentMap();
    private static final Set<URI> writtenToDisk = Sets.newHashSet();

    private final Config config;
    private final List<ScriptData> scriptDataList = Collections.synchronizedList( Lists.<ScriptData>newLinkedList() );
    private Collection<Pattern> ignorePatterns;
    private File instrumentedFileDirectory;

    public HtmlUnitBasedScriptInstrumenter(final Config config) {
        this.config = config;
        setIgnorePatterns(config.getIgnorePatterns());
    }

    @Override
    public String instrument(final String sourceCode, final String sourceName, final int lineNumber) {
        try {
            final String normalizedSourceName = handleEvals(handleInvalidUriChars(handleInlineScripts(sourceName)));

            if (shouldIgnore(normalizedSourceName)) {
                return sourceCode;
            }

            final boolean separateFile = isSeparateFile(sourceName, normalizedSourceName);
            final URI sourceUri = URI.create(normalizedSourceName).normalize();

            if (config.isCacheInstrumentedCode() && instrumentedScriptCache.containsKey(sourceUri)) {
                final ScriptData data = instrumentedScriptCache.get(sourceUri);
                scriptDataList.add(data);
                return data.getInstrumentedSourceCode();
            }

            final ScriptData data = addNewScriptData(sourceCode, separateFile, sourceUri);

            final String instrumentedCode = instrument(lineNumber, data);
            data.setInstrumentedSourceCode(instrumentedCode);

            maybeCache(sourceUri, data);
            maybeWriteInstrumentedCodeToDisk(separateFile, sourceUri, instrumentedCode);

            return instrumentedCode;
        } catch (final RuntimeException e) {
            logger.error("Exception caught while instrumenting code", e);
            return sourceCode;
        }
    }

    private String instrument(final int lineNumber, final ScriptData data) {
        final Parser parser = new Parser();

        final String sourceUriAsString = data.getSourceUriAsString();
        final AstRoot root = parser.parse(data.getSourceCode(), sourceUriAsString, lineNumber);
        root.visit(new InstrumentingNodeVisitor(data, lineNumber - 1));

        final String treeSource = root.toSource();
        final StringBuilder buf = new StringBuilder(
                INITIALIZING_CODE.length() +
                data.getNumberOfStatements() * ARRAY_INITIALIZER.length() +
                treeSource.length());

        buf.append(COMPLETION_MONITOR);
        buf.append(INITIALIZING_CODE);
        buf.append(String.format("if(!%s['%s']) {%n", COVERAGE_VARIABLE_NAME, sourceUriAsString));
        buf.append(String.format("    %s['%s'] = {};%n", COVERAGE_VARIABLE_NAME, sourceUriAsString));

        for (final Integer i : data.getLineNumbersOfAllStatements()) {
            buf.append(String.format(ARRAY_INITIALIZER, sourceUriAsString, i));
        }
        buf.append(String.format("}%n"));

        buf.append(treeSource);

        return buf.toString();
    }

    private ScriptData addNewScriptData(final String sourceCode, final boolean separateFile, final URI sourceUri) {
        final ScriptData data = new ScriptData(sourceUri, sourceCode, separateFile);
        scriptDataList.add(data);
        return data;
    }

    private static boolean isSeparateFile(final String sourceName, final String normalizedSourceName) {
        return normalizedSourceName.equals(sourceName) && !nonFileRe.matcher(normalizedSourceName).matches();
    }

    private static String handleInlineScripts(final String sourceName) {
        return inlineScriptRe.matcher(sourceName).replaceAll("$1__from_$2_$3_to_$4_$5");
    }

    private static String handleEvals(final String sourceName) {
        final Matcher matcher = evalRe.matcher(sourceName);

        if (matcher.find()) {
            // assign a unique count to an eval statement because they might have the same name, which is bad for us
            return sourceName + "(" + evalCounter.getAndIncrement() + ")";
        }

        return sourceName;
    }

    /**
     * Doesn't handle a lot of cases right now. So far, handles only invalid '?' and '#' in query string and fragment parts of the URIs.
     */
    private static String handleInvalidUriChars(final String sourceName) {
        final StringBuilder buf = new StringBuilder();

        final int indexOfQueryDelimiter = sourceName.indexOf('?');
        final int indexOfFragmentDelimiter = sourceName.indexOf('#');

        if (indexOfQueryDelimiter != -1) {
            buf.append(sourceName.substring(0, indexOfQueryDelimiter)).append('?');
        } else if (indexOfFragmentDelimiter != -1) {
            buf.append(sourceName.substring(0, indexOfFragmentDelimiter)).append('#');
        } else {
            buf.append(sourceName);
        }

        if (indexOfQueryDelimiter != -1) {
            final int lastIndex = indexOfFragmentDelimiter == -1 ? sourceName.length() : indexOfFragmentDelimiter;
            final String queryString = sourceName.substring(indexOfQueryDelimiter + 1, lastIndex);

            buf.append(queryString.replaceAll("\\?", "%3F"));
        }

        if (indexOfFragmentDelimiter != -1) {
            final String fragment = sourceName.substring(indexOfFragmentDelimiter + 1);

            buf.append(fragment.replaceAll("#", "%23"));
        }

        return buf.toString();
    }

    private void maybeCache(final URI sourceUri, final ScriptData data) {
        if (config.isCacheInstrumentedCode()) {
            instrumentedScriptCache.putIfAbsent(sourceUri, data);
        }
    }

    private void maybeWriteInstrumentedCodeToDisk(final boolean separateFile, final URI sourceUri, final String instrumentedCode) {
        if (config.isOutputInstrumentedFiles() && separateFile) {
            synchronized (writtenToDisk) {
                try {
                    if (!writtenToDisk.contains(sourceUri)) {
                        final String parent = UriUtil.getParent(sourceUri);
                        final String fileName = UriUtil.getLastSegmentOrHost(sourceUri);

                        final File fileOutputDir = new File(instrumentedFileDirectory, Hashing.md5().hashString(parent).toString());
                        FileUtils.mkdir(fileOutputDir.getAbsolutePath());

                        final File outputFile = new File(fileOutputDir, fileName);

                        logger.info("Writing instrumented file: {}", outputFile.getAbsolutePath());
                        ByteStreams.write(instrumentedCode.getBytes("UTF-8"), Files.newOutputStreamSupplier(outputFile));

                        writtenToDisk.add(sourceUri);
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private boolean shouldIgnore(final String sourceName) {
        return ignorePatterns != null && Iterables.any(ignorePatterns, new Predicate<Pattern>() {
            @Override
            public boolean apply(final Pattern input) {
                return input.matcher(sourceName).matches();
            }
        });
    }

    public List<ScriptData> getScriptDataList() {
        return scriptDataList;
    }

    public void setIgnorePatterns(final Collection<Pattern> ignorePatterns) {
        this.ignorePatterns = ignorePatterns;
    }

    public void setInstrumentedFileDirectory(final File instrumentedFileDirectory) {
        this.instrumentedFileDirectory = instrumentedFileDirectory;
    }

    static {
        try {
            COMPLETION_MONITOR = CharStreams.toString(new InputSupplier<Reader>() {

                @Override
                public Reader getInput() throws IOException {
                    return new InputStreamReader(HtmlUnitBasedScriptInstrumenter.class.getResourceAsStream("/completion_monitor.js"),
                            Charset.forName("UTF-8"));
                }
            });
        } catch (final IOException e) {
            throw new RuntimeException("Could not initialize instrumenter", e);
        }
    }


}
