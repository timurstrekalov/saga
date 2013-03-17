package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class InstanceFieldPerPropertyConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(InstanceFieldPerPropertyConfig.class);

    private URI baseUri;
    private String baseDir;
    private File outputDir;

    private String includes;
    private String excludes;

    private Set<String> noInstrumentPatterns = Sets.newHashSet();
    private boolean outputInstrumentedFiles;

    private boolean cacheInstrumentedCode = true;

    private OutputStrategy outputStrategy = OutputStrategy.TOTAL;

    private int threadCount = Runtime.getRuntime().availableProcessors();

    private boolean includeInlineScripts = false;

    private long backgroundJavaScriptTimeout = 5 * 60 * 1000;

    private String sourcesToPreload;
    private String sourcesToPreloadEncoding = "UTF-8";

    private BrowserVersion browserVersion = BrowserVersion.FIREFOX_3_6;

    private Set<ReportFormat> reportFormats = ImmutableSet.of(ReportFormat.HTML, ReportFormat.RAW);

    private SortBy sortBy;
    private Order order;

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

}
