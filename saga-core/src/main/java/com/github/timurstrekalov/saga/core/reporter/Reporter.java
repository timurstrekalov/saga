package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;

public interface Reporter {

    void writeReport(Config config, TestRunCoverageStatistics runStats) throws IOException;

}
