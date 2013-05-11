package com.github.timurstrekalov.saga.core.instrumentation;

import java.util.Map;

public interface InstrumentingBrowser extends ScriptInstrumenter {

    Map<String, Map<String, Long>> extractCoverageDataVariable();

    void get(String url);

    void quit();

}
