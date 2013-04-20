package com.github.timurstrekalov.saga.core.model;

import com.github.timurstrekalov.saga.core.util.UriUtil;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * User: dervish
 * Date: 4/4/13
 * Time: 1:39 PM
 */
public class ScriptDataTest {
    private static final URI TEST_URI = UriUtil.toUri("http://localhost:8080/test/foo/bar/test.js");
    private static final String TEST_CODE = "test";
    ScriptData toTest = new ScriptData(TEST_URI,TEST_CODE,false);

    @Test
    public void testGenerateScriptCoverageStatistics() throws Exception {
        Map<Integer, Double> stubCoverage = new HashMap<Integer, Double>();
        stubCoverage.put(1,2d);
        ScriptCoverageStatistics stats = toTest.generateScriptCoverageStatistics("test/foo/",UriUtil.toUri("http://localhost:8080"),stubCoverage);
        assertThat(stats.getRelativePath(),is("bar/test.js"));
    }
}
