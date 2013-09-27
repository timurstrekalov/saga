package com.github.timurstrekalov.saga.core.reporter;

import java.util.Map;

import com.github.timurstrekalov.saga.core.ReportFormat;
import com.google.common.collect.ImmutableMap;

public final class ReporterFactory {

    private static final Map<ReportFormat, Class<? extends Reporter>> reporters = ImmutableMap.<ReportFormat, Class<? extends Reporter>>builder()
            .put(ReportFormat.HTML, HtmlReporter.class)
            .put(ReportFormat.RAW, RawReporter.class)
            .put(ReportFormat.CSV, CsvReporter.class)
            .put(ReportFormat.PDF, PdfReporter.class)
            .put(ReportFormat.COBERTURA, CoberturaReporter.class)
            .build();

    private ReporterFactory() {
        throw new UnsupportedOperationException("Non-instantiable");
    }

    public static Reporter reporterFor(final ReportFormat reportFormat) {
        if (!reporters.containsKey(reportFormat)) {
            throw new IllegalStateException("Missing reporter for format: " + reportFormat);
        }

        try {
            return reporters.get(reportFormat).newInstance();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

}
