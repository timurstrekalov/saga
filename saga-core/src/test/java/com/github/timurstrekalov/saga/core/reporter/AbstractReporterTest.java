package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.cfg.ReporterConfig;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import org.hamcrest.Matcher;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * User: dervish
 * Date: 4/4/13
 * Time: 5:10 PM
 *
 * Verify that RawReporter is writing data into single configurable file and
 * all other reporters don't miss capability to write reports into separate test folders.
 *
 */
@Ignore
public class AbstractReporterTest {

    /**
     * Test that Raw reporter is writing output into single file named according
     * to configuration specified by internal structures
     *
     * @throws Exception
     */
    @Test
    public void testWriteReportRaw() throws Exception {
        AbstractReporter toTest = new RawReporter();
        URI stubTest = URI.create("test");
        TestRunCoverageStatistics testStats = new TestRunCoverageStatistics(stubTest);
        ReporterConfig testConfig = new ReporterConfig();
        testConfig.setRawName("");
        toTest.writeReport(testConfig,testStats);
        String reportPath = "";
        File outputFolder = new File(reportPath);
//        assertThat(outputFolder,containsProperRawReport());
    }

    private Matcher<? super File> containsProperRawReport() {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }


    /**
     * Test that Html reporter is still capable of writing reports based on executed
     * tests file names
     *
     * @throws Exception
     */
    @Test
    public void testWriteReportHtml() throws Exception {
//TODO: implement
        AbstractReporter toTest = new HtmlReporter();
        URI stubTest = URI.create("test");
        TestRunCoverageStatistics testStats = new TestRunCoverageStatistics(stubTest);
        ReporterConfig testConfig = new ReporterConfig();
        testConfig.setRawName("");
        toTest.writeReport(testConfig,testStats);
//        assertThat(outputFolder,containsProperHtmlReport());
    }
}
