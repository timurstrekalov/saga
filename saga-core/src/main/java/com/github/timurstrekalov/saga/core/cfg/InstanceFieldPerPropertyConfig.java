package com.github.timurstrekalov.saga.core.cfg;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.github.timurstrekalov.saga.core.Order;
import com.github.timurstrekalov.saga.core.OutputStrategy;
import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.SortBy;
import com.github.timurstrekalov.saga.core.instrumentation.InstrumentingBrowser;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
    private String webDriverClassName = Config.DEFAULT_WEB_DRIVER_CLASS_NAME;
    private InstrumentingBrowser browser = null;
    private Map<String, String> webDriverCapabilities = Maps.newHashMap();

    private List<String> sourceDirs;
    private String completionExpression;

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

                setBrowserVersion((BrowserVersion) BrowserVersion.class.getField(browserVersion).get(BrowserVersion.class));
            } catch (final Exception e) {
                logger.error("Invalid browser version: {}", browserVersion);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setBrowserVersion(final BrowserVersion browserVersion) {
        this.browserVersion = browserVersion;
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

    @Override
    public void setWebDriverClassName(final String webDriverClassName) {
        if (webDriverClassName == null) {
            return;
        }

        this.webDriverClassName = webDriverClassName;
    }
    
    @Override
    public void setInstrumentingBrowser(final InstrumentingBrowser browser) {
    	this.browser = browser;
    }

    @Override
    public String getWebDriverClassName() {
        return webDriverClassName;
    }
    
    @Override
    public InstrumentingBrowser getInstrumentingBrowser() {
    	return browser;
    }

    @Override
    public File getInstrumentedFileDirectory() {
        return new File(outputDir, INSTRUMENTED_FILE_DIRECTORY_NAME);
    }

    @Override
    public Collection<Pattern> getIgnorePatterns() {
        return Collections2.transform(getNoInstrumentPatterns(), new Function<String, Pattern>() {
            @Override
            public Pattern apply(final String input) {
                return Pattern.compile(input);
            }
        });
    }

    @Override
    public void setWebDriverCapabilities(final Map<String, String> webDriverCapabilities) {
        if (webDriverCapabilities == null) {
            return;
        }

        this.webDriverCapabilities = webDriverCapabilities;
    }

    @Override
    public Map<String, String> getWebDriverCapabilities() {
        return webDriverCapabilities;
    }

    @Override
    public void setSourceDir(List<String> sourceDirs) {
        this.sourceDirs = sourceDirs;
    }

    @Override
    public List<String> getSourceDirs() {
        return sourceDirs;
    }

    @Override
    public String getCompletionExpression() {
        return completionExpression;
    }

    @Override
    public void setCompletionExpression(String completionExpression) {
        this.completionExpression = completionExpression;
    }

}
