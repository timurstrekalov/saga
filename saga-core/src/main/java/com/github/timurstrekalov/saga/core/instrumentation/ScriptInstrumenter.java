package com.github.timurstrekalov.saga.core.instrumentation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.ScriptPreProcessor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
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
import com.google.common.io.Files;
import net.sourceforge.htmlunit.corejs.javascript.CompilerEnvirons;
import net.sourceforge.htmlunit.corejs.javascript.Parser;
import net.sourceforge.htmlunit.corejs.javascript.ast.AstRoot;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.timurstrekalov.saga.core.cfg.Config.COVERAGE_VARIABLE_NAME;

public final class ScriptInstrumenter implements ScriptPreProcessor {

    private static final AtomicInteger evalCounter = new AtomicInteger();

    private static final Logger logger = LoggerFactory.getLogger(ScriptInstrumenter.class);

    private static final Pattern inlineScriptRe = Pattern.compile("script in (.+) from \\((\\d+), (\\d+)\\) to \\((\\d+), (\\d+)\\)");
    private static final Pattern evalRe = Pattern.compile("(.+)#(\\d+\\(eval\\))");
    private static final Pattern nonFileRe = Pattern.compile("JavaScriptStringJob");

    private static final ConcurrentMap<URI, ScriptData> instrumentedScriptCache = Maps.newConcurrentMap();
    private static final Set<URI> writtenToDisk = Sets.newHashSet();

    private final HtmlUnitContextFactory contextFactory;
    private final String initializingCode;
    private final String arrayInitializer;

    private final Config config;
    private final List<ScriptData> scriptDataList = Lists.newLinkedList();
    private Collection<Pattern> ignorePatterns;
    private File instrumentedFileDirectory;

    public ScriptInstrumenter(final Config config, final HtmlUnitContextFactory contextFactory) {
        this.config = config;
        this.contextFactory = contextFactory;

        initializingCode = String.format("%s = window.%s || {};%n", COVERAGE_VARIABLE_NAME, COVERAGE_VARIABLE_NAME);
        arrayInitializer = String.format("%s['%%s'][%%d] = 0;%n", COVERAGE_VARIABLE_NAME);
    }

    @Override
    public String preProcess(
            final HtmlPage htmlPage,
            final String sourceCode,
            final String sourceName,
            final int lineNumber,
            final HtmlElement htmlElement) {
        try {
            final String normalizedSourceName = handleEvals(handleInlineScripts(sourceName));

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

            final ScriptData data = new ScriptData(sourceUri, sourceCode, separateFile);
            scriptDataList.add(data);

            final CompilerEnvirons environs = new CompilerEnvirons();
            environs.initFromContext(contextFactory.enterContext());

            final AstRoot root = new Parser(environs).parse(data.getSourceCode(), data.getSourceUriAsString(), lineNumber);
            root.visit(new InstrumentingVisitor(data, lineNumber - 1));

            final String treeSource = root.toSource();
            final StringBuilder buf = new StringBuilder(
                    initializingCode.length() +
                    data.getNumberOfStatements() * arrayInitializer.length() +
                    treeSource.length());

            buf.append(initializingCode);
            buf.append(String.format("%s['%s'] = {};%n", COVERAGE_VARIABLE_NAME, escapePath(data.getSourceUriAsString())));

            for (final Integer i : data.getLineNumbersOfAllStatements()) {
                buf.append(String.format(arrayInitializer, escapePath(data.getSourceUriAsString()), i));
            }

            buf.append(treeSource);

            final String instrumentedCode = buf.toString();
            data.setInstrumentedSourceCode(instrumentedCode);

            if (config.isCacheInstrumentedCode()) {
                instrumentedScriptCache.putIfAbsent(sourceUri, data);
            }

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

            return instrumentedCode;
        } catch (final RuntimeException e) {
            logger.error("Exception caught while instrumenting code", e);
            return sourceCode;
        }
    }

    private String escapePath(final String path) {
        return path.replaceAll("\\\\", "\\\\\\\\");
    }

    private boolean isSeparateFile(final String sourceName, final String normalizedSourceName) {
        return normalizedSourceName.equals(sourceName) && !nonFileRe.matcher(normalizedSourceName).matches();
    }

    private String handleInlineScripts(final String sourceName) {
        return inlineScriptRe.matcher(sourceName).replaceAll("$1__from_$2_$3_to_$4_$5");
    }

    private String handleEvals(final String sourceName) {
        final Matcher matcher = evalRe.matcher(sourceName);

        if (matcher.find()) {
            // assign a unique count to an eval statement because they might have the same name, which is bad for us
            return sourceName + "(" + evalCounter.getAndIncrement() + ")";
        }

        return sourceName;
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

    public File getInstrumentedFileDirectory() {
        return instrumentedFileDirectory;
    }

}
