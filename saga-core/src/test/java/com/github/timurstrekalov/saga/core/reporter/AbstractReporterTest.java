package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.OutputStrategy;
import com.github.timurstrekalov.saga.core.cfg.ReporterConfig;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
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
public class AbstractReporterTest {
    TestRunCoverageStatistics testStats;
    File temp;
    File tempResults;

    @Before
    public void setUp () throws  Exception{

        temp = File.createTempFile("saga",".test");
        temp.deleteOnExit();

        URI stubTest = URI.create("test");
        testStats = new TestRunCoverageStatistics(stubTest);

        tempResults = new File(temp.getParentFile().getAbsolutePath() + "/sagaTestOut");
        tempResults.deleteOnExit();
    }

    /**
     * Test that Raw reporter is writing output into single file named according
     * to configuration specified by internal structures
     *
     * @throws Exception
     */
    @Test
    public void testWriteReportRaw() throws Exception {
        AbstractReporter toTest = new RawReporter();
        ReporterConfig testConfig = new ReporterConfig();
        testConfig.setOutputDir(tempResults);
        testConfig.setBaseUri(URI.create(temp.getAbsolutePath()));
        testConfig.setOutputStrategy(OutputStrategy.TOTAL);
        testConfig.setRawName("myTest");
        toTest.writeReport(testConfig,testStats);
        assertThat(tempResults,containsProperRawReport());
    }

    @Factory
    private Matcher<? super File> containsProperRawReport() {
        return new BaseMatcher<File>() {
            @Override
            public boolean matches(Object o) {
                File folder = (File) o;
                boolean result = false;
                File[] folderFiles = folder.listFiles();

                boolean numberMatches = folderFiles.length == 1;
                boolean nameMatches = folderFiles[0].getName().equals("myTest-coverage.dat");
                if (numberMatches && nameMatches) {
                    result = true;
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("folder should contain report files");
            }
        };
    }


    /**
     * Test that Html reporter is still capable of writing reports based on executed
     * tests file names
     *
     * @throws Exception
     */
    @Test
    public void testWriteReportHtml() throws Exception {
        AbstractReporter toTest = new HtmlReporter();
        ReporterConfig testConfig = new ReporterConfig();
        testConfig.setOutputDir(tempResults);
        testConfig.setBaseUri(URI.create(temp.getAbsolutePath()));
        testConfig.setOutputStrategy(OutputStrategy.PER_TEST);
        testConfig.setRawName("myTest");
        toTest.writeReport(testConfig,testStats);
        assertThat(tempResults,containsProperHtmlReport());

    }

    @Factory
    private Matcher<? super File> containsProperHtmlReport() {
        return new BaseMatcher<File>() {
            @Override
            public boolean matches(Object o) {
                File folder = (File) o;
                boolean result = false;
                File[] folderFiles = folder.listFiles();

                boolean numberMatches = folderFiles.length == 1;
                boolean nameMatches = folderFiles[0].getName().equals("test-report.html");
                if (numberMatches && nameMatches) {
                    result = true;
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("html report should contain test specific report name");
            }
        };
    }

    @After
    public void tearDown() throws Exception {
        for (File created : tempResults.listFiles()) {
            created.delete();
        }
        tempResults.delete();
        temp.delete();
    }
}
