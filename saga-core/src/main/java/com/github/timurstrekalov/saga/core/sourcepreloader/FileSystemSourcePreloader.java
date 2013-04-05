package com.github.timurstrekalov.saga.core.sourcepreloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.OutputStrategy;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.github.timurstrekalov.saga.core.util.UriUtil;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemSourcePreloader implements SourcePreloader {

    private static final Logger logger = LoggerFactory.getLogger(FileSystemSourcePreloader.class);

    @Override
    public void preloadSources(final Config config, final ScriptInstrumenter instrumenter, final TestRunCoverageStatistics totalStats)
            throws IOException {
        final String sourcesToPreload = config.getSourcesToPreload();
        final URI baseUri = config.getReporterConfig().getBaseUri();

        if (sourcesToPreload == null || !config.getReporterConfig().getOutputStrategy().contains(OutputStrategy.TOTAL) || !UriUtil.isFileUri(baseUri)) {
            return;
        }

        final String sourcesToPreloadEncoding = config.getSourcesToPreloadEncoding();
        logger.info("Using {} to preload sources", sourcesToPreloadEncoding);

        final List<File> filesToPreload = FileUtils.getFiles(new File(baseUri), sourcesToPreload, null);

        logger.info("Preloading {} files", filesToPreload.size());

        for (final File file : filesToPreload) {
            logger.debug("Preloading {}", file);

            final String source = CharStreams.toString(Files.newReaderSupplier(file, Charset.forName(sourcesToPreloadEncoding)));
            instrumenter.preProcess(null, source, file.toURI().toString(), 0, null);
        }

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Map<Integer, Double> coverageData = Maps.newHashMap();

            for (final Integer lineNumber : data.getLineNumbersOfAllStatements()) {
                coverageData.put(lineNumber, 0.0);
            }

            totalStats.add(data.generateScriptCoverageStatistics(config.getReporterConfig().getRelativePathBase(),baseUri, coverageData));
        }
    }

}
