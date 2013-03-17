package com.github.timurstrekalov.saga.core;

import java.io.IOException;

import com.github.timurstrekalov.saga.core.cfg.Config;

public interface CoverageGenerator {

    void instrumentAndGenerateReports() throws IOException;

    Config getConfig();

}
