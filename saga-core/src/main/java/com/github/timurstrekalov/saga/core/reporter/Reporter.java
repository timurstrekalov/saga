package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.RunStats;

import java.io.File;
import java.io.IOException;

public interface Reporter {

    void writeReport(File baseDir, File outputDir, RunStats runStats) throws IOException;

}
