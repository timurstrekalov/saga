package com.github.timurstrekalov.saga.core.cfg;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.github.timurstrekalov.saga.core.Order;
import com.github.timurstrekalov.saga.core.OutputStrategy;
import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.SortBy;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceFieldPerPropertyConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(InstanceFieldPerPropertyConfig.class);

    private URI baseUri;
    private String baseDir;
    private File outputDir;

    private String includes;
    private String excludes;

    private Set<String> noInstrumentPatterns = Sets.newHashSet();
    private Set<ReportFormat> reportFormats = Config.DEFAULT_REPORT_FORMATS;

    private boolean outputInstrumentedFiles;

    private boolean cacheInstrumentedCode = Config.DEFAULT_CACHE_INSTRUMENTED_CODE;
    private boolean includeInlineScripts = Config.DEFAULT_INCLUDE_INLINE_SCRIPTS;

    private OutputStrategy outputStrategy = Config.DEFAULT_OUTPUT_STRATEGY;
    private BrowserVersion browserVersion = Config.DEFAULT_BROWSER_VERSION;

    private int threadCount = Config.DEFAULT_THREAD_COUNT;
    private long backgroundJavaScriptTimeout = Config.DEFAULT_BACKGROUND_JAVASCRIPT_TIMEOUT;

    private String sourcesToPreload;
    private String sourcesToPreloadEncoding = Config.DEFAULT_SOURCES_TO_PRELOAD_ENCODING;

    private SortBy sortBy = Config.DEFAULT_SORT_BY;
    private Order order = Config.DEFAULT_ORDER;
    private String rawName = Config.DEFAULT_RAW;

    @Override
    public void setBaseDir(final String baseDir) {
        this.baseDir = baseDir;
        baseUri = UriUtil.toUri(baseDir);
    }

    @Override
    public void setOutputDir(final File outputDir) {
        this.outputDir = outputDir;
    }

    @Override
    public void setExcludes(final String excludes) {
        this.excludes = excludes;
    }

    @Override
    public void setIncludes(final String includes) {
        this.includes = includes;
    }

    @Override
    public void setNoInstrumentPatterns(final Collection<String> noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            this.noInstrumentPatterns = Sets.newHashSet(noInstrumentPatterns);
        }
    }

    @Override
    public void setNoInstrumentPatterns(final String[] noInstrumentPatterns) {
        if (noInstrumentPatterns != null) {
            setNoInstrumentPatterns(ImmutableList.copyOf(noInstrumentPatterns));
        }
    }

    @Override
    public void setOutputInstrumentedFiles(final Boolean outputInstrumentedFiles) {
        if (outputInstrumentedFiles != null) {
            this.outputInstrumentedFiles = outputInstrumentedFiles;
        }
    }

    @Override
    public void setCacheInstrumentedCode(final Boolean cacheInstrumentedCode) {
        if (cacheInstrumentedCode != null) {
            this.cacheInstrumentedCode = cacheInstrumentedCode;
        }
    }

    @Override
    public void setOutputStrategy(final String outputStrategy) {
        if (outputStrategy != null) {
            setOutputStrategy(OutputStrategy.valueOf(outputStrategy.toUpperCase()));
        }
    }

    @Override
    public void setOutputStrategy(final OutputStrategy outputStrategy) {
        if (outputStrategy != null) {
            this.outputStrategy = outputStrategy;
        }
    }

    @Override
    public void setThreadCount(final Integer threadCount) {
        if (threadCount != null) {
            Preconditions.checkArgument(threadCount > 0, "Thread count must be greater than zero");
            this.threadCount = threadCount;
        }
    }

    @Override
    public void setIncludeInlineScripts(final Boolean includeInlineScripts) {
        if (includeInlineScripts != null) {
            this.includeInlineScripts = includeInlineScripts;
        }
    }

    @Override
    public void setBackgroundJavaScriptTimeout(final Long backgroundJavaScriptTimeout) {
        if (backgroundJavaScriptTimeout != null) {
            this.backgroundJavaScriptTimeout = backgroundJavaScriptTimeout;
        }
    }

    @Override
    public void setSourcesToPreload(final String sourcesToPreload) {
        if (sourcesToPreload != null) {
            this.sourcesToPreload = sourcesToPreload;
        }
    }

    @Override
    public void setSourcesToPreloadEncoding(final String sourcesToPreloadEncoding) {
        if (sourcesToPreloadEncoding != null) {
            this.sourcesToPreloadEncoding = sourcesToPreloadEncoding;
        }
    }

    @Override
    public void setBrowserVersion(final String browserVersion) {
        if (browserVersion != null) {
            try {
                logger.info("Setting {} as browser version", browserVersion);

                this.browserVersion = (BrowserVersion) BrowserVersion.class.getField(browserVersion).get(BrowserVersion.class);
            } catch (final Exception e) {
                logger.error("Invalid browser version: {}", browserVersion);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setReportFormats(final String reportFormatString) {
        if (reportFormatString == null) {
            return;
        }

        final Iterable<String> reportFormats = Splitter.on(",")
                .omitEmptyStrings()
                .trimResults()
                .split(reportFormatString);

        logger.info("Setting {} as report formats", Joiner.on(", ").join(reportFormats));

        this.reportFormats = Sets.newHashSet(Iterables.transform(reportFormats, new Function<String, ReportFormat>() {

            @Override
            public ReportFormat apply(final String input) {
                return ReportFormat.valueOf(input.toUpperCase());
            }
        }));
    }

    @Override
    public void setSortBy(final String sortBy) {
        if (sortBy == null) {
            return;
        }

        this.sortBy = SortBy.valueOf(sortBy.trim().toUpperCase());
    }

    @Override
    public void setOrder(final String order) {
        if (order == null) {
            return;
        }

        this.order = Order.fromString(order.trim());
    }

    @Override
    public URI getBaseUri() {
        return baseUri;
    }

    @Override
    public String getBaseDir() {
        return baseDir;
    }

    @Override
    public File getOutputDir() {
        return outputDir;
    }

    @Override
    public String getIncludes() {
        return includes;
    }

    @Override
    public String getExcludes() {
        return excludes;
    }

    @Override
    public Set<String> getNoInstrumentPatterns() {
        return noInstrumentPatterns;
    }

    @Override
    public boolean isOutputInstrumentedFiles() {
        return outputInstrumentedFiles;
    }

    @Override
    public boolean isCacheInstrumentedCode() {
        return cacheInstrumentedCode;
    }

    @Override
    public OutputStrategy getOutputStrategy() {
        return outputStrategy;
    }

    @Override
    public int getThreadCount() {
        return threadCount;
    }

    @Override
    public boolean isIncludeInlineScripts() {
        return includeInlineScripts;
    }

    @Override
    public long getBackgroundJavaScriptTimeout() {
        return backgroundJavaScriptTimeout;
    }

    @Override
    public String getSourcesToPreload() {
        return sourcesToPreload;
    }

    @Override
    public String getSourcesToPreloadEncoding() {
        return sourcesToPreloadEncoding;
    }

    @Override
    public BrowserVersion getBrowserVersion() {
        return browserVersion;
    }

    @Override
    public Set<ReportFormat> getReportFormats() {
        return reportFormats;
    }

    @Override
    public SortBy getSortBy() {
        return sortBy;
    }

    @Override
    public Order getOrder() {
        return order;
    }

    public String getRawName() {
        return rawName;
    }

    @Override
    public void setRawName(String rawName) {
        this.rawName = rawName;
    }

}
