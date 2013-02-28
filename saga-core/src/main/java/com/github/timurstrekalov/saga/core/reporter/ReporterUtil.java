package com.github.timurstrekalov.saga.core.reporter;

import com.github.timurstrekalov.saga.core.RunStats;

import java.io.File;
import java.net.URI;

final class ReporterUtil {

    private ReporterUtil() {
        // utility class
    }

    public static File getFileOutputDir(final File baseDir, final File outputDir, final RunStats runStats) {
        final URI relativeTestUri = baseDir.toURI().relativize(runStats.test.toURI());
        return new File(new File(outputDir.toURI().resolve(relativeTestUri)).getParent());
    }

}
