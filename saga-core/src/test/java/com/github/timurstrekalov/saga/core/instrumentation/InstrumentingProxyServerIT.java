package com.github.timurstrekalov.saga.core.instrumentation;

import java.io.File;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.github.timurstrekalov.saga.core.FileServer;
import com.github.timurstrekalov.saga.core.webdriver.WebDriverUtils;
import com.github.timurstrekalov.saga.core.cfg.InstanceFieldPerPropertyConfig;
import com.github.timurstrekalov.saga.core.htmlunit.WebClientFactory;
import com.github.timurstrekalov.saga.core.server.InstrumentingProxyServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

public class InstrumentingProxyServerIT {

    private InstrumentingProxyServer proxyServer;
    private FileServer fileServer;
    private WebDriver driver;

    private int proxyServerPort;
    private int fileServerPort;

    @Before
    public void setUp() throws Exception {
        final InstanceFieldPerPropertyConfig config = new InstanceFieldPerPropertyConfig();

        proxyServer = new InstrumentingProxyServer(new HtmlUnitBasedScriptInstrumenter(config));
        fileServer = new FileServer(new File(getClass().getResource("/tests").toURI()).getAbsolutePath());

        proxyServerPort = proxyServer.start();
        fileServerPort = fileServer.start();

        final String proxyUrl = "localhost:" + proxyServerPort;

        final Proxy proxy = new Proxy()
                .setProxyType(Proxy.ProxyType.MANUAL)
                .setHttpProxy(proxyUrl)
                .setHttpsProxy(proxyUrl);

        final DesiredCapabilities desiredCapabilities = new DesiredCapabilities();
        desiredCapabilities.setCapability(CapabilityType.PROXY, proxy);
        desiredCapabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true);
        desiredCapabilities.setBrowserName(BrowserType.FIREFOX);

        driver = new HtmlUnitDriver(desiredCapabilities) {
            @Override
            protected WebClient newWebClient(final BrowserVersion version) {
                config.setBrowserVersion(version);
                return WebClientFactory.newInstance(config);
            }
        };
    }

    @After
    public void tearDown() {
        driver.quit();
        proxyServer.stop();
        fileServer.stop();
    }

    @Test
    public void test_proxying_works() {
        driver.get("http://localhost:" + fileServerPort + "/ClassTest.html");

        WebDriverUtils.waitForWindowJavaScriptVariableToBePresent((JavascriptExecutor) driver, ScriptInstrumenter.COVERAGE_VARIABLE_NAME);
    }

}
