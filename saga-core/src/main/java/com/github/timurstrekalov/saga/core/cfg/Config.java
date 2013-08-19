package com.github.timurstrekalov.saga.core.cfg;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.github.timurstrekalov.saga.core.Order;
import com.github.timurstrekalov.saga.core.OutputStrategy;
import com.github.timurstrekalov.saga.core.ReportFormat;
import com.github.timurstrekalov.saga.core.SortBy;
import com.github.timurstrekalov.saga.core.instrumentation.InstrumentingBrowser;
import com.google.common.collect.ImmutableSet;

/**
 * A midway attempt at externalizing configuration
 */
public interface Config {

    BrowserVersion DEFAULT_BROWSER_VERSION = BrowserVersion.FIREFOX_17;
    OutputStrategy DEFAULT_OUTPUT_STRATEGY = OutputStrategy.TOTAL;
    SortBy DEFAULT_SORT_BY = SortBy.COVERAGE;
    Order DEFAULT_ORDER = Order.DESC;

    Set<ReportFormat> DEFAULT_REPORT_FORMATS = ImmutableSet.of(ReportFormat.HTML, ReportFormat.RAW);

    String DEFAULT_SOURCES_TO_PRELOAD_ENCODING = "UTF-8";

    int DEFAULT_THREAD_COUNT = Runtime.getRuntime().availableProcessors();
    long DEFAULT_BACKGROUND_JAVASCRIPT_TIMEOUT = 5 * 60 * 1000;

    boolean DEFAULT_CACHE_INSTRUMENTED_CODE = true;
    boolean DEFAULT_INCLUDE_INLINE_SCRIPTS = false;

    String INSTRUMENTED_FILE_DIRECTORY_NAME = "instrumented";
    String DEFAULT_WEB_DRIVER_CLASS_NAME = "org.openqa.selenium.htmlunit.HtmlUnitDriver";

    // TODO stop this configuration setter madness

    void setBaseDir(String baseDir);

    void setOutputDir(File outputDir);

    void setExcludes(String excludes);

    void setIncludes(String includes);

    void setNoInstrumentPatterns(Collection<String> noInstrumentPatterns);

    void setNoInstrumentPatterns(String[] noInstrumentPatterns);

    void setOutputInstrumentedFiles(Boolean outputInstrumentedFiles);

    void setCacheInstrumentedCode(Boolean cacheInstrumentedCode);

    void setOutputStrategy(String outputStrategy);

    void setOutputStrategy(OutputStrategy outputStrategy);

    void setThreadCount(Integer threadCount);

    void setIncludeInlineScripts(Boolean includeInlineScripts);

    void setBackgroundJavaScriptTimeout(Long backgroundJavaScriptTimeout);

    void setSourcesToPreload(String sourcesToPreload);

    void setSourcesToPreloadEncoding(String sourcesToPreloadEncoding);

    void setBrowserVersion(String browserVersionAsString);

    void setBrowserVersion(BrowserVersion browserVersion);

    void setReportFormats(String reportFormatString);

    void setSortBy(String sortBy);

    void setOrder(String order);

    void setWebDriverClassName(String webDriverClassName);
    
    void setInstrumentingBrowser(InstrumentingBrowser browser);

    void setWebDriverCapabilities(Map<String, String> webDriverCapabilities);

    URI getBaseUri();

    String getBaseDir();

    File getOutputDir();

    String getIncludes();

    String getExcludes();

    Set<String> getNoInstrumentPatterns();

    boolean isOutputInstrumentedFiles();

    boolean isCacheInstrumentedCode();

    OutputStrategy getOutputStrategy();

    int getThreadCount();

    boolean isIncludeInlineScripts();

    long getBackgroundJavaScriptTimeout();

    String getSourcesToPreload();

    String getSourcesToPreloadEncoding();

    BrowserVersion getBrowserVersion();

    Set<ReportFormat> getReportFormats();

    SortBy getSortBy();

    Order getOrder();

    File getInstrumentedFileDirectory();

    Collection<Pattern> getIgnorePatterns();

    String getWebDriverClassName();
    
    InstrumentingBrowser getInstrumentingBrowser();

    Map<String, String> getWebDriverCapabilities();

}
