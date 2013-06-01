package com.github.timurstrekalov.saga.core;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.Callable;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.instrumentation.InstrumentingBrowser;
import com.github.timurstrekalov.saga.core.instrumentation.InstrumentingBrowserFactory;
import com.github.timurstrekalov.saga.core.model.ScriptCoverageStatistics;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestRunCoverageStatisticsCallable implements Callable<TestRunCoverageStatistics> {

    private static final Logger logger = LoggerFactory.getLogger(TestRunCoverageStatisticsCallable.class);
    private static final ThreadLocal<InstrumentingBrowser> localBrowser = new ThreadLocal<InstrumentingBrowser>();

    private final Config config;
    private final URI test;
    private final OutputStrategy outputStrategy;

    public TestRunCoverageStatisticsCallable(final Config config, final URI test, final OutputStrategy outputStrategy) {
        this.config = config;
        this.test = test;
        this.outputStrategy = outputStrategy;
    }

    @Override
    public TestRunCoverageStatistics call() {
        try {
            logger.info("Running test at {}", test.toString());

            try {
                final TestRunCoverageStatistics runStats = runTest(test);

                if (runStats == TestRunCoverageStatistics.EMPTY) {
                    logger.warn("No actual test run for file: {}", test);
                } else if (outputStrategy.contains(OutputStrategy.PER_TEST)) {
                    if (UriUtil.isFileUri(test)) {
                        new WritesStatistics().write(config, runStats);
                    } else {
                        logger.warn("Output strategy PER_TEST only makes sense in the context of tests run off the filesystem, ignoring");
                    }
                }

                return runStats;
            } catch (final IOException e) {
                return TestRunCoverageStatistics.EMPTY;
            } catch (final RuntimeException e) {
                logger.warn("Error running test {}: {}", test.toString(), e.getMessage());
                throw e;
            }
        } finally {
            if (localBrowser.get() != null) {
                logger.info("Quitting browser");
                localBrowser.get().quit();
            }
        }
    }

    private TestRunCoverageStatistics runTest(final URI test) throws IOException {
        final InstrumentingBrowser browser = getLocalBrowser();

        browser.get(test.toASCIIString());

        final Map<String, Map<String, Long>> coverageData = browser.extractCoverageDataVariable();

        if (coverageData == null) {
            return TestRunCoverageStatistics.EMPTY;
        }

        return collectAndWriteRunStats(test, coverageData);
    }

    private TestRunCoverageStatistics collectAndWriteRunStats(
            final URI test, final Map<String, Map<String, Long>> coverageDataForAllScripts) throws IOException {
        final TestRunCoverageStatistics runStats = new TestRunCoverageStatistics(test);
        runStats.setSortBy(config.getSortBy());
        runStats.setOrder(config.getOrder());

        final URI baseUri = config.getBaseUri();

        for (final ScriptData data : getLocalBrowser().getScriptDataList()) {
            final String sourceUri = data.getSourceUriAsString();

            @SuppressWarnings("unchecked")
            final Map<String, Long> coverageDataForScript = coverageDataForAllScripts.get(sourceUri);
            final ScriptCoverageStatistics scriptCoverageStatistics = data.generateScriptCoverageStatistics(baseUri, coverageDataForScript);

            runStats.add(scriptCoverageStatistics);
        }

        return runStats;
    }

    private InstrumentingBrowser getLocalBrowser() {
        if (localBrowser.get() == null) {
            localBrowser.set(InstrumentingBrowserFactory.newInstance(config));
        }

        return localBrowser.get();
    }

}
