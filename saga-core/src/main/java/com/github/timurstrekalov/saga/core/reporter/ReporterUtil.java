package com.github.timurstrekalov.saga.core.reporter;

import java.io.File;
import java.net.URI;

import com.github.timurstrekalov.saga.core.RunStats;

final class ReporterUtil {

    private ReporterUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static File getFileOutputDir(final URI baseUri, final File outputDir, final RunStats runStats) {
        final URI relativeTestUri = baseUri.relativize(runStats.test);
        return new File(outputDir, relativeTestUri.toString()).getParentFile().getAbsoluteFile();
    }

}
