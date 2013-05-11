package com.github.timurstrekalov.saga.core.instrumentation;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.github.timurstrekalov.saga.core.model.ScriptData;

/**
 * Represents an entity capable of instrumenting JavaScript sources, creating a window-scoped JavaScript variable with name defined by
 * {@link #COVERAGE_VARIABLE_NAME}.
 */
public interface ScriptInstrumenter {

    String COVERAGE_VARIABLE_NAME = "__saga_coverage_data";
    String TIMEOUTS_VARIABLE_NAME = "__saga_timeouts";

    /**
     * Instruments the given source code.
     * @param sourceCode the JavaScript source code to instrument
     * @param sourceName the "name" of the source - will be used in uniquely identifying this script among the others instrumented by this
     *                   {@link ScriptInstrumenter}
     * @param lineNumber the actual number of the first line of this source code in the underlying file (usually 1, different for inline
     *                   scripts)
     * @return the instrumented source code
     */
    String instrument(String sourceCode, String sourceName, int lineNumber);

    void setIgnorePatterns(Collection<Pattern> ignorePatterns);

    void setInstrumentedFileDirectory(File instrumentedFileDirectory);

    List<ScriptData> getScriptDataList();

}
