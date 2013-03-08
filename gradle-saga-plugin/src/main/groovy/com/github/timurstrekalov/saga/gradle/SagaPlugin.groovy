package com.github.timurstrekalov.saga.gradle

import com.github.timurstrekalov.saga.core.CoverageGeneratorFactory
import org.gradle.api.Plugin
import org.gradle.api.Project

class SagaPlugin implements Plugin<Project> {

    private static final String PLUGIN_NAME = 'saga'

    @Override
    void apply(final Project project) {
        project.extensions.create(PLUGIN_NAME, SagaPluginExtension)
        project.task('coverage') << {
            final SagaPluginExtension cfg = (SagaPluginExtension) project[PLUGIN_NAME]

            final def gen = CoverageGeneratorFactory.newInstance(cfg.baseDir, cfg.outputDir)
            final def config = gen.config

            // TODO pretty sure we can do this dynamically without having to list every property
            config.includes = cfg.includes
            config.excludes = cfg.excludes
            config.outputInstrumentedFiles = cfg.outputInstrumentedFiles
            config.cacheInstrumentedCode = cfg.cacheInstrumentedCode
            config.noInstrumentPatterns = cfg.noInstrumentPatterns
            config.outputStrategy = cfg.outputStrategy
            config.threadCount = cfg.threadCount
            config.includeInlineScripts = cfg.includeInlineScripts
            config.backgroundJavaScriptTimeout = cfg.backgroundJavaScriptTimeout
            config.sourcesToPreload = cfg.sourcesToPreload
            config.sourcesToPreloadEncoding = cfg.sourcesToPreloadEncoding
            config.browserVersion = cfg.browserVersion
            config.reportFormats = cfg.reportFormats
            config.sortBy = cfg.sortBy
            config.order = cfg.order

            gen.instrumentAndGenerateReports()
        }
    }

}
