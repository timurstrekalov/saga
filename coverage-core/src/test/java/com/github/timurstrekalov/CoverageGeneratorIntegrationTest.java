package com.github.timurstrekalov;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

public class CoverageGeneratorIntegrationTest {

    private CoverageGenerator gen;

    @Test
    public void endToEnd() throws IOException {
        final List<URL> tests = ImmutableList.of(getClass().getResource("/ClassTest.html"));
        final List<String> ignore = ImmutableList.of(/*"^.+Test.js$", "^script in .+from \\(\\d+, \\d+\\) to \\(\\d+, \\d+\\)$"*/);

        final File outputDir = new File("target/coverage");

        gen = new CoverageGenerator("_COV", tests, outputDir);

        gen.setIgnorePatterns(ignore);
        gen.setOutputInstrumentedFiles(true);

        gen.run();

        final String expected = IOUtils.toString(getClass().getResource("/Class.js.pregenerated.html"));
        final String actual = IOUtils.toString(new File(outputDir, "ClassTest.html-Class.js.html").toURI());

//        assertEquals(expected, actual);
    }

}
