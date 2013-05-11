package com.github.timurstrekalov.saga.core.htmlunit;

import com.gargoylesoftware.htmlunit.ScriptPreProcessor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;

public final class InstrumentingPreProcessor implements ScriptPreProcessor {

    private final ScriptInstrumenter instrumenter;

    public InstrumentingPreProcessor(final ScriptInstrumenter instrumenter) {
        this.instrumenter = instrumenter;
    }

    @Override
    public String preProcess(
            final HtmlPage htmlPage,
            final String sourceCode,
            final String sourceName,
            final int lineNumber,
            final HtmlElement htmlElement) {
        return instrumenter.instrument(sourceCode, sourceName, lineNumber);
    }

}
