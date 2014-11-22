package com.github.timurstrekalov.saga.core.sourcepreloader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import com.github.timurstrekalov.saga.core.OutputStrategy;
import com.github.timurstrekalov.saga.core.cfg.Config;
import com.github.timurstrekalov.saga.core.instrumentation.HtmlUnitBasedScriptInstrumenter;
import com.github.timurstrekalov.saga.core.instrumentation.ScriptInstrumenter;
import com.github.timurstrekalov.saga.core.model.ScriptData;
import com.github.timurstrekalov.saga.core.model.TestRunCoverageStatistics;
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
    public void preloadSources(final Config config, final TestRunCoverageStatistics totalStats) throws IOException {
        final ScriptInstrumenter instrumenter = new HtmlUnitBasedScriptInstrumenter(config);
        final String sourcesToPreload = config.getSourcesToPreload();
        final URI baseUri = config.getBaseUri();

        if (sourcesToPreload == null || !config.getOutputStrategy().contains(OutputStrategy.TOTAL)) {
            return;
        }
        
        URI baseDir;
        if (UriUtil.isFileUri(baseUri)) {
        	baseDir = baseUri;
        } else {
            try {
				baseDir = new URI("file:" + config.getSourceDirs().get(0));
			} catch (URISyntaxException e) {
				logger.error("Excetion when creating baseDir ({}):{}", config.getSourceDirs().get(0), e.toString());
				return;
			}
        }

        final String sourcesToPreloadEncoding = config.getSourcesToPreloadEncoding();
        logger.info("Using {} to preload sources, from: {}", sourcesToPreloadEncoding, baseDir);

        final List<File> filesToPreload = FileUtils.getFiles(new File(baseDir), sourcesToPreload, null);

        logger.info("Preloading {} files", filesToPreload.size());

        for (final File file : filesToPreload) {
            //logger.debug("Preloading {}", file);

            final String source = CharStreams.toString(Files.newReaderSupplier(file, Charset.forName(sourcesToPreloadEncoding)));
            String baseFileUrl = file.toURI().toString();
            if (!UriUtil.isFileUri(baseUri)) {
            	baseFileUrl = baseUri.toString() + "/" + file.toURI().toString().replaceFirst(baseDir.toString(), "");
            	
            }
            logger.debug("Preloading {}, baseFileUrl: {}, {}, {}", file, baseFileUrl, file.toURI().toString(), baseDir.toString());
            instrumenter.instrument(source, baseFileUrl, 0);
        }

        for (final ScriptData data : instrumenter.getScriptDataList()) {
            final Map<String, Long> coverageData = Maps.newHashMap();

            for (final Integer lineNumber : data.getLineNumbersOfAllStatements()) {
                coverageData.put(String.valueOf(lineNumber), 0L);
            }

            totalStats.add(data.generateScriptCoverageStatistics(baseUri, coverageData));
        }
    }

}
