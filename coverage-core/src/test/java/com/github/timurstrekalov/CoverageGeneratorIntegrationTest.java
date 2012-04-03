package com.github.timurstrekalov;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

public class CoverageGeneratorIntegrationTest {

    private CoverageGenerator gen;

    @Test
    public void endToEnd() throws IOException {
        final List<String> paths = ImmutableList.of("/ClassTest.html", "/ClassTest2.html");

        final Collection<URL> tests = Collections2.transform(paths, new Function<String, URL>() {
            @Override
            public URL apply(final String input) {
                final URL url = getClass().getResource(input);
//                if (!new File(url.toString()).exists()) {
//                    throw new RuntimeException("Can't find " + url);
//                }

                return url;
            }
        });

        final List<String> ignore = ImmutableList.of("^.+Test.*$", "^script in .+from \\(\\d+, \\d+\\) to \\(\\d+, \\d+\\)$");

        final File outputDir = new File("target/coverage");

        gen = new CoverageGenerator("_COV", tests, outputDir);

//        gen.setIgnorePatterns(ignore);
        gen.setOutputInstrumentedFiles(true);

        gen.run();

//        assertEquals(expected, actual);
    }

}
