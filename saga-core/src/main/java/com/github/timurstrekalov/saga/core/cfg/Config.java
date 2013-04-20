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
import com.google.common.collect.ImmutableSet;

/**
 * A midway attempt at externalizing configuration
 */
public interface Config {

    String COVERAGE_VARIABLE_NAME = "__saga_coverage_data";

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
    String DEFAULT_RAW_NAME = "total";

    // TODO stop this configuration setter madness

    void setExcludes(String excludes);

    void setIncludes(String includes);

    void setNoInstrumentPatterns(Collection<String> noInstrumentPatterns);

    void setNoInstrumentPatterns(String[] noInstrumentPatterns);

    void setOutputInstrumentedFiles(Boolean outputInstrumentedFiles);

    void setCacheInstrumentedCode(Boolean cacheInstrumentedCode);

    void setThreadCount(Integer threadCount);

    void setIncludeInlineScripts(Boolean includeInlineScripts);

    void setBackgroundJavaScriptTimeout(Long backgroundJavaScriptTimeout);

    void setSourcesToPreload(String sourcesToPreload);

    void setSourcesToPreloadEncoding(String sourcesToPreloadEncoding);

    void setBrowserVersion(String browserVersionAsString);

    void setReportFormats(String reportFormatString);

    void setSortBy(String sortBy);

    void setOrder(String order);

    String getIncludes();

    String getExcludes();

    Set<String> getNoInstrumentPatterns();

    boolean isOutputInstrumentedFiles();

    boolean isCacheInstrumentedCode();

    int getThreadCount();

    boolean isIncludeInlineScripts();

    long getBackgroundJavaScriptTimeout();

    String getSourcesToPreload();

    String getSourcesToPreloadEncoding();

    BrowserVersion getBrowserVersion();

    Set<ReportFormat> getReportFormats();

    SortBy getSortBy();

    Order getOrder();

    public ReporterConfig getReporterConfig();
    public void setReporterConfig(ReporterConfig reporterConfig);

}
