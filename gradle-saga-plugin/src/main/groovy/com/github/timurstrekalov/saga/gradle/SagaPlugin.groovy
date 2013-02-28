package com.github.timurstrekalov.saga.gradle
import com.github.timurstrekalov.saga.core.CoverageGenerators
import org.gradle.api.Plugin
import org.gradle.api.Project

class SagaPlugin implements Plugin<Project> {

    private static final String PLUGIN_NAME = 'saga'

    @Override
    void apply(final Project project) {
        project.extensions.create(PLUGIN_NAME, SagaPluginExtension)
        project.task('coverage') << {
            final SagaPluginExtension cfg = (SagaPluginExtension) project[PLUGIN_NAME]

            final def gen = CoverageGenerators.newInstance(cfg.baseDir, cfg.includes, cfg.excludes, cfg.outputDir)

            gen.outputInstrumentedFiles = cfg.outputInstrumentedFiles
            gen.cacheInstrumentedCode = cfg.cacheInstrumentedCode
            gen.noInstrumentPatterns = cfg.noInstrumentPatterns
            gen.outputStrategy = cfg.outputStrategy
            gen.threadCount = cfg.threadCount
            gen.includeInlineScripts = cfg.includeInlineScripts
            gen.backgroundJavaScriptTimeout = cfg.backgroundJavaScriptTimeout
            gen.sourcesToPreload = cfg.sourcesToPreload
            gen.sourcesToPreloadEncoding = cfg.sourcesToPreloadEncoding
            gen.browserVersion = cfg.browserVersion
            gen.reportFormats = cfg.reportFormats

            gen.run()
        }
    }

}
