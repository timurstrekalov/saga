package com.github.timurstrekalov.saga.core;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.cfg.InstanceFieldPerPropertyConfig;
import com.github.timurstrekalov.saga.core.model.ScriptCoverageStatistics;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import com.github.timurstrekalov.saga.core.sourcepreloader.FileSystemSourcePreloader;
import com.github.timurstrekalov.saga.core.testfetcher.TestFetcher;
import com.github.timurstrekalov.saga.core.testfetcher.TestFetcherFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

final class DefaultCoverageGenerator implements CoverageGenerator {

    private static final String TOTAL_REPORT_NAME = "total";
    private static final String INLINE_SCRIPT_RE = ".+__from_\\d+_\\d+_to_\\d+_\\d+$";

    private static final Logger logger = LoggerFactory.getLogger(DefaultCoverageGenerator.class);

    private final Config config;

    DefaultCoverageGenerator() {
        this(new InstanceFieldPerPropertyConfig());
    }

    DefaultCoverageGenerator(final Config config) {
        this.config = config;
    }

    @Override
    public void instrumentAndGenerateReports() throws IOException {
        Preconditions.checkNotNull(config.getBaseDir(), "baseDir cannot be null");
        Preconditions.checkNotNull(config.getOutputDir(), "outputDir cannot be null");

        final URI baseUri = config.getBaseUri();
        final List<URI> tests = fetchTests(baseUri);

        if (tests.isEmpty()) {
            logger.warn("No tests found, exiting");
            return;
        }

        final int actualThreadCount = Math.min(config.getThreadCount(), tests.size());
        logger.info("Using up to {} threads", actualThreadCount);

        final OutputStrategy outputStrategy = config.getOutputStrategy();
        logger.info("Output strategy set to {}", outputStrategy);

        if (!config.isIncludeInlineScripts()) {
            config.getNoInstrumentPatterns().add(INLINE_SCRIPT_RE);
            config.getNoInstrumentPatterns().add(".+JavaScriptStringJob");
            config.getNoInstrumentPatterns().add(".+#\\d+\\(eval\\)\\(\\d+\\)");
            config.getNoInstrumentPatterns().add("injected script");
        }

        if (!config.getNoInstrumentPatterns().isEmpty()) {
            logger.info("Using the following no-instrument patterns:\n\t{}", Joiner.on("\n\t").join(config.getNoInstrumentPatterns()));
        }
        
        if (config.getMinCoveragePercentage() > 100) {
            logger.error("Coverage can not be greater than {}", config.getMinCoveragePercentage());
            throw new IOException("Invalid coverage specified");
        }
        
        if (config.getMinCoveragePercentage() != 0) {
            logger.info("Minimum coverage should be " + config.getMinCoveragePercentage());
        }

        final File outputDir = config.getOutputDir();
        FileUtils.mkdir(outputDir.getAbsolutePath());

        if (config.isOutputInstrumentedFiles()) {
            FileUtils.mkdir(config.getInstrumentedFileDirectory().getAbsolutePath());
        }

        final TestRunCoverageStatistics totalStats = new TestRunCoverageStatistics(baseUri.relativize(URI.create(TOTAL_REPORT_NAME)), "Total coverage report");
        totalStats.setSortBy(config.getSortBy());
        totalStats.setOrder(config.getOrder());
        totalStats.setSourceDirs(config.getSourceDirs());

        maybePreloadSources(totalStats);
        runTests(tests, actualThreadCount, outputStrategy, totalStats);
    }

    private void runTests(final List<URI> tests, final int actualThreadCount, final OutputStrategy outputStrategy, final TestRunCoverageStatistics totalStats) throws IOException {
        final ExecutorService executorService = Executors.newFixedThreadPool(actualThreadCount);
        final CompletionService<TestRunCoverageStatistics> completionService = new ExecutorCompletionService<TestRunCoverageStatistics>(executorService);

        for (final URI test : tests) {
            completionService.submit(new TestRunCoverageStatisticsCallable(config, test, outputStrategy));
        }

        final List<TestRunCoverageStatistics> allRunStats = Lists.newLinkedList();
        final int submittedTasks = tests.size();

        try {
            for (int i = 0; i < submittedTasks; i++) {
                try {
                    final Future<TestRunCoverageStatistics> future = completionService.take();
                    final TestRunCoverageStatistics runStats = future.get();

                    allRunStats.add(runStats);
                } catch (final Exception e) {
                    logger.debug(e.getMessage(), e);
                }
            }
        } finally {
            executorService.shutdown();
        }

        logger.info("Test run finished");

        if (outputStrategy.contains(OutputStrategy.TOTAL)) {
            for (final TestRunCoverageStatistics runStats : allRunStats) {
                if (runStats != TestRunCoverageStatistics.EMPTY) {
                    for (final ScriptCoverageStatistics scriptCoverageStatistics : runStats) {
                        totalStats.add(scriptCoverageStatistics);
                    }
                }
            }

            new WritesStatistics().write(config, totalStats);
            
            testMinimumCoverage(totalStats, allRunStats);
        }
    }

    private void testMinimumCoverage(final TestRunCoverageStatistics totalStats, final List<TestRunCoverageStatistics> allRunStats) throws IOException {
        int totalExecuted = 0;
        int totalAvailable = 0;
        for (final TestRunCoverageStatistics runStats : allRunStats) {
            if (runStats != TestRunCoverageStatistics.EMPTY) {
                for (final ScriptCoverageStatistics scriptCoverageStatistics : runStats) {
                    totalExecuted += scriptCoverageStatistics.getExecuted();
                    totalAvailable += scriptCoverageStatistics.getStatements();
                }
            }
        }
        if (totalAvailable != 0) {
            if ( totalExecuted*100.0/totalAvailable < config.getMinCoveragePercentage() ) {
                throw new RuntimeException("Code coverage less than " + config.getMinCoveragePercentage() + "%");
            }
        }
    }
    
    private void maybePreloadSources(final TestRunCoverageStatistics totalStats) throws IOException {
        new FileSystemSourcePreloader().preloadSources(config, totalStats);
    }

    @Override
    public Config getConfig() {
        return config;
    }

    private List<URI> fetchTests(final URI baseDir) throws IOException {
        final TestFetcher fetcher = TestFetcherFactory.newInstance(baseDir);
        final List<URI> tests = fetcher.fetch(baseDir, config.getIncludes(), config.getExcludes());

        logger.info("{} tests found", tests.size());

        return tests;
    }

}
