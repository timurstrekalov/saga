package com.github.timurstrekalov.saga.core;

import java.io.IOException;

public interface CoverageGenerator {

    void instrumentAndGenerateReports() throws IOException;

    Config getConfig();

}
