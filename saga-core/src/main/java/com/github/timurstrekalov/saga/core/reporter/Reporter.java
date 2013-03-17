package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;

public interface Reporter {

    void writeReport(URI baseUri, File outputDir, TestRunCoverageStatistics runStats) throws IOException;

}
