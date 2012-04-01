package com.github.timurstrekalov;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CoverageGeneratorIntegrationTest {

    private CoverageGenerator gen;

    @Test
    public void endToEnd() throws IOException {
        final List<URL> tests = ImmutableList.of(getClass().getResource("/test.html"));
        final List<String> ignore = ImmutableList.of("^.+Test.js$");

        final File outputDir = new File("target/coverage");

        gen = new CoverageGenerator("_COV", tests, outputDir);

        gen.setIgnorePatterns(ignore);
        gen.setOutputInstrumentedFiles(true);

        gen.run();

        final String expected = IOUtils.toString(getClass().getResource("/Class.js.pregenerated.html"));
        final String actual = IOUtils.toString(new File(outputDir, "Class.js.html").toURI());

        assertEquals(expected, actual);
    }

}
