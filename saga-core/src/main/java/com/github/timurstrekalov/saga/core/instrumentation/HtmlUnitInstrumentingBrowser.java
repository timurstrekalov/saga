package com.github.timurstrekalov.saga.core.instrumentation;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.htmlunit.InstrumentingPreProcessor;
import com.github.timurstrekalov.saga.core.htmlunit.WebClientFactory;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

public final class HtmlUnitInstrumentingBrowser implements InstrumentingBrowser {

    private final Config config;
    private Driver driver;
    private ScriptInstrumenter instrumenter;
    private InstrumentingPreProcessor preProcessor;
    private boolean initialized = false;

    public HtmlUnitInstrumentingBrowser(final Config config) {
        this.config = config;
        this.driver = new Driver(config.getBrowserVersion());
        initialize();
    }

    public HtmlUnitInstrumentingBrowser(final Config config, Driver driver) {
        // This version of the constructor is for external test runners that want to instantiate
        // the web driver themselves.  Since the config object may not yet be fully initialized,
        // defer initialization until first use.
        this.config = config;
        this.driver = driver;
    }
    
    private void initialize() {
        instrumenter = new HtmlUnitBasedScriptInstrumenter(config);
        instrumenter.setIgnorePatterns(config.getIgnorePatterns());

        if (config.isOutputInstrumentedFiles()) {
            instrumenter.setInstrumentedFileDirectory(config.getInstrumentedFileDirectory());
        }

        preProcessor = new InstrumentingPreProcessor(instrumenter);
        driver.init(config, preProcessor);
        driver.enableInstrumentation();
        initialized = true;
    }
    
    @Override
    public String instrument(final String sourceCode, final String sourceName, final int lineNumber) {
        return instrumenter.instrument(sourceCode, sourceName, lineNumber);
    }

    @Override
    public void setIgnorePatterns(final Collection<Pattern> ignorePatterns) {
        instrumenter.setIgnorePatterns(ignorePatterns);
    }

    @Override
    public void setInstrumentedFileDirectory(final File instrumentedFileDirectory) {
        instrumenter.setInstrumentedFileDirectory(instrumentedFileDirectory);
    }

    @Override
    public List<ScriptData> getScriptDataList() {
        return instrumenter.getScriptDataList();
    }

    @Override
    public void get(final String url) {
        if (!initialized) {
            initialize();
        }
        driver.get(url);
    }

    @Override
    public void quit() {
        driver.quit();
    }

    @Override
    public Map<String, Map<String, Long>> extractCoverageDataVariable() {
        try {
            driver.disableInstrumentation();

            @SuppressWarnings("unchecked")
            final Map<String, Map<Long, Long>> data = (Map<String, Map<Long, Long>>) driver.executeScript(
                    "return window." + ScriptInstrumenter.COVERAGE_VARIABLE_NAME);

            if (data == null) {
                return null;
            }

            return Maps.transformValues(data, new Function<Map<Long, Long>, Map<String, Long>>() {
                @Override
                public Map<String, Long> apply(final java.util.Map<Long, Long> input) {
                    final Map<String, Long> result = Maps.newHashMap();
                    for (final Map.Entry<Long, Long> e : input.entrySet()) {
                        result.put(String.valueOf(e.getKey()), e.getValue());
                    }

                    return result;
                }
            });
        } finally {
            driver.enableInstrumentation();
        }
    }

    public static class Driver extends HtmlUnitDriver {
        private Config config;
        private InstrumentingPreProcessor preProcessor;

        public Driver(final BrowserVersion browserVersion) {
            super(browserVersion);
            setJavascriptEnabled(true);
        }
        
        public Driver(final Capabilities caps) {
            super(caps);
            setJavascriptEnabled(true);
        }
        
        private void init(Config config, InstrumentingPreProcessor preProcessor) {
            this.config = config;
            this.preProcessor = preProcessor;
        }
        
        @Override
        protected WebClient newWebClient(final BrowserVersion version) {
            return WebClientFactory.newInstance(version);
        }

        @Override
        protected void get(final URL fullUrl) {
            super.get(fullUrl);
            getWebClient().waitForBackgroundJavaScript(
                    config != null ? config.getBackgroundJavaScriptTimeout() : Config.DEFAULT_BACKGROUND_JAVASCRIPT_TIMEOUT);
        }

        public void enableInstrumentation() {
            getWebClient().setScriptPreProcessor(preProcessor);
        }

        public void disableInstrumentation() {
            getWebClient().setScriptPreProcessor(null);
        }

    }

}
