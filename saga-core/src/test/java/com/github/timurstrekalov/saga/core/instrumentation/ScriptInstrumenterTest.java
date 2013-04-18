package com.github.timurstrekalov.saga.core.instrumentation;

import java.util.concurrent.atomic.AtomicInteger;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.github.timurstrekalov.saga.core.Data;
import com.github.timurstrekalov.saga.core.cfg.InstanceFieldPerPropertyConfig;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import net.sourceforge.htmlunit.corejs.javascript.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScriptInstrumenterTest {

    private static final AtomicInteger evalCounter = new AtomicInteger();

    @Mock
    private HtmlPage htmlPage;

    @Mock
    private HtmlElement htmlElement;

    @Mock
    private HtmlUnitContextFactory factory;

    @Mock
    private Context context;

    @Before
    public void setUp() throws Exception {
        when(factory.enterContext()).thenReturn(context);
    }

    @Test
    public void preProcess() {
        final String sourceName = "script in Class.js from (6, 13) to (6, 28)";
        final String expectedSourceName = "Class.js__from_6_13_to_6_28";

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(new InstanceFieldPerPropertyConfig(), factory);
        instrumenter.preProcess(htmlPage, Data.getClassJsSourceCode(), sourceName, 1, htmlElement);

        assertEquals(1, instrumenter.getScriptDataList().size());

        final ScriptData classJsData = instrumenter.getScriptDataList().get(0);
        assertEquals(expectedSourceName, classJsData.getSourceUriAsString());
        assertEquals(5, classJsData.getLineNumberOfFirstStatement());
        assertEquals(113, classJsData.getNumberOfStatements());
        assertEquals(Data.getClassJsInstrumented(expectedSourceName), classJsData.getInstrumentedSourceCode());
    }

    @Test
    public void preProcess_eval() {
        final String sourceName = "http://localhost:59743/src/element_mover.js#23(eval)";
        final String expectedSourceName = "http://localhost:59743/src/element_mover.js#23(eval)(" + evalCounter.getAndIncrement() + ")";

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(new InstanceFieldPerPropertyConfig(), factory);
        instrumenter.preProcess(htmlPage, Data.getClassJsSourceCode(), sourceName, 1, htmlElement);

        assertEquals(1, instrumenter.getScriptDataList().size());

        final ScriptData classJsData = instrumenter.getScriptDataList().get(0);
        assertEquals(expectedSourceName, classJsData.getSourceUriAsString());
        assertEquals(5, classJsData.getLineNumberOfFirstStatement());
        assertEquals(113, classJsData.getNumberOfStatements());
        assertEquals(Data.getClassJsInstrumented(expectedSourceName), classJsData.getInstrumentedSourceCode());
    }

    @Test
    public void preProcess_issue_85() {
        final String sourceName = "http://localhost:59664/spec/resources/dojo-release-1.8.3/dojo/dojo.js#222(Function)#1(eval)";
        final String expectedSourceName = "http://localhost:59664/spec/resources/dojo-release-1.8.3/dojo/dojo.js#222(Function)%231(eval)("
                + evalCounter.getAndIncrement() + ")";

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(new InstanceFieldPerPropertyConfig(), factory);
        instrumenter.preProcess(htmlPage, Data.getClassJsSourceCode(), sourceName, 1, htmlElement);

        assertEquals(1, instrumenter.getScriptDataList().size());

        final ScriptData classJsData = instrumenter.getScriptDataList().get(0);
        assertEquals(expectedSourceName, classJsData.getSourceUriAsString());
        assertEquals(5, classJsData.getLineNumberOfFirstStatement());
        assertEquals(113, classJsData.getNumberOfStatements());
        assertEquals(Data.getClassJsInstrumented(expectedSourceName), classJsData.getInstrumentedSourceCode());
    }

}
