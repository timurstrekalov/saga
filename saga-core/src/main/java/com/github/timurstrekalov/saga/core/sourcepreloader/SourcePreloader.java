package com.github.timurstrekalov.saga.core.sourcepreloader;

import java.io.IOException;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;

interface SourcePreloader {

    /**
     * Preload the sources as specified by the config, adding them to the total run statistics.
     */
    void preloadSources(Config config, TestRunCoverageStatistics totalStats) throws IOException;

}
