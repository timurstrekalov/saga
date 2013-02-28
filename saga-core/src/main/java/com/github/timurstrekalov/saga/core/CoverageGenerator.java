package com.github.timurstrekalov.saga.core;

import java.io.IOException;
import java.util.Collection;

public interface CoverageGenerator {

    void run() throws IOException;

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

    void setReportFormats(String reportFormatString);

}
