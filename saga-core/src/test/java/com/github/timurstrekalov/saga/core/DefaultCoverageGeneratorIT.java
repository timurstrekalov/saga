package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.timurstrekalov.saga.core.cfg.InstanceFieldPerPropertyConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DefaultCoverageGeneratorIT {

    private static final Pattern EXECUTED_STATEMENTS_PATTERN = Pattern.compile("<td class=\"executed\">(\\d+)</td>");

    private InstanceFieldPerPropertyConfig config;
    private DefaultCoverageGenerator generator;

    @Test
    public void test_instrumentAndGenerateReports() throws Exception {
        config = new InstanceFieldPerPropertyConfig();
        config.setBaseDir(new File(getClass().getResource("/tests").toURI()).getAbsolutePath());
        config.setOutputDir(new File(Data.getProperty("build.directory") + "/coverage-htmlunit"));
        config.setIncludes("**/*Test*.html");
        config.setBackgroundJavaScriptTimeout(5000L);

        generator = new DefaultCoverageGenerator(config);

        generator.instrumentAndGenerateReports();

        assertThat(config.getOutputDir().list().length, is(greaterThan(0)));

        final File html = new File(config.getOutputDir(), "total-report.html");
        final String htmlAsString = Files.toString(html, Charset.forName("UTF-8"));

        final Matcher m = EXECUTED_STATEMENTS_PATTERN.matcher(htmlAsString);
        assertThat(m.find(), is(true));
        assertThat(Integer.parseInt(m.group(1)), greaterThanOrEqualTo(85));

        assertThat(html.exists(), is(true));

        final File lcov = new File(config.getOutputDir(), "total-coverage.dat");
        assertThat(lcov.exists(), is(true));

        final List<String> lcovLines = CharStreams.readLines(new InputSupplier<FileReader>() {

            @Override
            public FileReader getInput() throws IOException {
                return new FileReader(lcov);
            }
        });

        assertThat(lcovLines.get(0), is(equalTo("SF:ClassTest.js")));
        assertThat(lcovLines.get(4), is(equalTo("SF:pkg/ClassTest2.js")));
    }

    @Test
    @Ignore
    public void test_instrumentAndGenerateReports_with_phantomjs() throws Exception {
        final FileServer fileServer = new FileServer(new File(getClass().getResource("/tests").toURI()).getAbsolutePath());
        final int fileServerPort = fileServer.start();

        config = new InstanceFieldPerPropertyConfig();
        // localhost/127.0.0.1 don't work - requests never hit the proxy, and there's no way to tell phantomjs to proxy local requests
        config.setBaseDir("http://192.168.1.100:" + fileServerPort + "/ClassTest.html");
        config.setOutputDir(new File(Data.getProperty("build.directory") + "/coverage-phantomjs"));
        config.setBackgroundJavaScriptTimeout(2000L);

        generator = new DefaultCoverageGenerator(config);

        config.setWebDriverClassName("org.openqa.selenium.phantomjs.PhantomJSDriver");
        config.setWebDriverCapabilities(ImmutableMap.of("phantomjs.binary.path", "/opt/boxen/homebrew/bin/phantomjs"));

        generator.instrumentAndGenerateReports();

        assertThat(config.getOutputDir().list().length, is(greaterThanOrEqualTo(2)));

        final File html = new File(config.getOutputDir(), "total-report.html");
        final String htmlAsString = Files.toString(html, Charset.forName("UTF-8"));

        final Matcher m = EXECUTED_STATEMENTS_PATTERN.matcher(htmlAsString);
        assertThat(m.find(), is(true));
        assertThat(Integer.parseInt(m.group(1)), greaterThanOrEqualTo(85));

        assertThat(html.exists(), is(true));
        assertThat(new File(config.getOutputDir(), "total-coverage.dat").exists(), is(true));
    }

}
