package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.net.URI;
import java.util.List;

import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.github.timurstrekalov.saga.core.reporter.ReporterUtil.getFileOutputDir;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class ReporterUtilTest {

    private static final String FILE_PREFIX = "file:";

    private static final List<String> PARENT_DIR_PATH_PARTS = ImmutableList.of("", "home", "abracadabra", "myproject");
    private static final String PARENT_DIR_PATH = Joiner.on('/').join(PARENT_DIR_PATH_PARTS);

    private static final URI BASE_URI = URI.create(FILE_PREFIX + PARENT_DIR_PATH + "/" + "src");
    private static final File OUTPUT_DIR = new File(PARENT_DIR_PATH, "target");

    @Test
    public void test_getFileOutputDir() throws Exception {
        final URI test = URI.create(FILE_PREFIX + Joiner.on("/").join(PARENT_DIR_PATH, "src", "main", "javascript", "MyTest.html"));
        final TestRunCoverageStatistics stats = new TestRunCoverageStatistics(test);

        final File actual = getFileOutputDir(BASE_URI, OUTPUT_DIR, stats).getAbsoluteFile();
        final File expected = new File(PARENT_DIR_PATH, Joiner.on(File.separatorChar).join("target", "main", "javascript")).getAbsoluteFile();

        assertThat(actual, equalTo(expected));
    }

}
