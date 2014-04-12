package com.github.timurstrekalov.saga.core.instrumentation;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.github.timurstrekalov.saga.core.server.InstrumentingProxyServer;
import com.github.timurstrekalov.saga.core.webdriver.SafeJavascriptWait;
import com.github.timurstrekalov.saga.core.webdriver.WebDriverUtils;
import com.google.common.base.Predicate;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class GenericInstrumentingBrowser implements InstrumentingBrowser {

    private static final Logger logger = LoggerFactory.getLogger(GenericInstrumentingBrowser.class);

    private final Config config;
    private final WebDriver driver;
    private final ScriptInstrumenter instrumenter;
    private final int proxyServerPort;

    public GenericInstrumentingBrowser(final Config config) {
        this.config = config;
        instrumenter = new HtmlUnitBasedScriptInstrumenter(config);

        final InstrumentingProxyServer proxyServer = new InstrumentingProxyServer(instrumenter);
        proxyServerPort = proxyServer.start();

        driver = newDriver(getWebDriverClass());
    }

    private String completionExpression() {
        if (config.getCompletionExpression() == null) {
            return String.format("window.%s.length === 0", TIMEOUTS_VARIABLE_NAME);
        } else {
            return config.getCompletionExpression();
        }
    }

    @Override
    public void get(final String url) {
        driver.get(url);

        final JavascriptExecutor js = (JavascriptExecutor) driver;

        WebDriverUtils.waitForWindowJavaScriptVariableToBePresent(js, TIMEOUTS_VARIABLE_NAME);

        new SafeJavascriptWait(js)
                .withTimeout(config.getBackgroundJavaScriptTimeout(), TimeUnit.MILLISECONDS)
                .until(new Predicate<JavascriptExecutor>() {
                    @Override
                    public boolean apply(final JavascriptExecutor input) {
                        logger.debug("Waiting for background JavaScript jobs to stop...");
                        return Boolean.TRUE.equals(input.executeScript("return " + completionExpression()));
                    }
                });
    }

    @Override
    public void quit() {
        driver.quit();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Long>> extractCoverageDataVariable() {
        final JavascriptExecutor js = (JavascriptExecutor) driver;
        return (Map<String, Map<String, Long>>) js.executeScript("return window." + ScriptInstrumenter.COVERAGE_VARIABLE_NAME);
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

    private WebDriver newDriver(final Class<WebDriver> driverClass) {
        try {
            try {
                return driverClass.getConstructor(Capabilities.class).newInstance(getCapabilities());
            } catch (final NoSuchMethodException e) {
                return driverClass.newInstance();
            }
        } catch (final Exception e) {
            throw new RuntimeException("Could not create driver", e);
        }
    }

    private Capabilities getCapabilities() {
        final String proxyUrl = "localhost:" + proxyServerPort;

        final Proxy proxy = new Proxy()
                .setProxyType(Proxy.ProxyType.MANUAL)
                .setHttpProxy(proxyUrl)
                .setSslProxy(proxyUrl);

        final DesiredCapabilities desiredCapabilities = new DesiredCapabilities(config.getWebDriverCapabilities());
        desiredCapabilities.setCapability(CapabilityType.PROXY, proxy);
        desiredCapabilities.setJavascriptEnabled(true);

        return desiredCapabilities;
    }

    @SuppressWarnings("unchecked")
    private Class<WebDriver> getWebDriverClass() {
        final Class<WebDriver> driverClass;

        try {
            driverClass = (Class<WebDriver>) Class.forName(config.getWebDriverClassName());
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("Could not load class for the driver", e);
        }

        return driverClass;
    }

}
