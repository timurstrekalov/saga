package com.github.timurstrekalov.saga.core.instrumentation;

import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.HtmlUnitContextFactory;
import com.github.timurstrekalov.saga.core.Data;
import com.github.timurstrekalov.saga.core.InstanceFieldPerPropertyConfig;
import com.github.timurstrekalov.saga.core.ScriptData;
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

        final ScriptInstrumenter instrumenter = new ScriptInstrumenter(new InstanceFieldPerPropertyConfig(), factory);
        instrumenter.preProcess(htmlPage, Data.getClassJsSourceCode(), sourceName, 1, htmlElement);

        assertEquals(1, instrumenter.getScriptDataList().size());

        final ScriptData classJsData = instrumenter.getScriptDataList().get(0);
        assertEquals("Class.js__from_6_13_to_6_28", classJsData.getSourceUriAsString());
        assertEquals(5, classJsData.getLineNumberOfFirstStatement());
        assertEquals(113, classJsData.getNumberOfStatements());
        assertEquals(Data.getClassJsInstrumented(), classJsData.getInstrumentedSourceCode());
    }

}
