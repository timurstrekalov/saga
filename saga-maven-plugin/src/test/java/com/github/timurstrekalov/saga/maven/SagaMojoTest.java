package com.github.timurstrekalov.saga.maven;

import com.github.timurstrekalov.saga.core.cfg.ReporterConfig;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.File;
import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * User: dervish
 * Date: 4/4/13
 * Time: 3:13 PM
 *
 * Basic testing harness unit test to verify that plugin configuration works
 * as expected.
 * see http://maven.apache.org/plugin-testing/maven-plugin-testing-harness/getting-started/index.html
 * for more information
 */

public class SagaMojoTest extends AbstractMojoTestCase {

    private static final String TEST_POM_PATH = "src/test/resources/pluginTestPom.xml";
    private SagaMojo mojo;


    protected void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();
    }


    public void testMojoLookup() throws Exception {
        mojo = (SagaMojo) lookupMojo("coverage", new File(getBasedir(), TEST_POM_PATH));
        assertNotNull(mojo);
    }


    public void testMojoConfigPopulated() throws Exception {
        mojo = (SagaMojo) lookupMojo("coverage", new File(getBasedir(), TEST_POM_PATH));
        ReporterConfig expectedReporterConfig = new ReporterConfig();
        expectedReporterConfig.setBaseUri(URI.create("test"));
        expectedReporterConfig.setOutputDir(new File("test"));
        expectedReporterConfig.setRawName("testRaw");
        expectedReporterConfig.setRelativePathBase("testRelative");
        assertThat(mojo.getReporterConfig(), matches(expectedReporterConfig));
    }

    @Factory
    private Matcher<? super ReporterConfig> matches(final ReporterConfig expectedReporterConfig) {
        return new TypeSafeMatcher<ReporterConfig>() {
            @Override
            protected boolean matchesSafely(ReporterConfig reporterConfig) {
                boolean result = false;
                if (reporterConfig.getRawName().equals(expectedReporterConfig.getRawName()) &&
                        reporterConfig.getRelativePathBase().equals(expectedReporterConfig.getRelativePathBase()) &&
                        reporterConfig.getBaseUri().toString().equals(expectedReporterConfig.getBaseUri().toString()) &&
                        reporterConfig.getOutputDir().equals(expectedReporterConfig.getOutputDir())) {
                    result = true;
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("total reporterConfig should be ").appendValue(expectedReporterConfig);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown()
            throws Exception {
        // required
        super.tearDown();
    }

}
