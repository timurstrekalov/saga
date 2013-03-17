package com.github.timurstrekalov.saga.gradle

import com.github.timurstrekalov.saga.core.CoverageGeneratorFactory
import com.github.timurstrekalov.saga.core.cfg.InstanceFieldPerPropertyConfig
import org.gradle.api.Plugin
import org.gradle.api.Project

class SagaPlugin implements Plugin<Project> {

    private static final String PLUGIN_NAME = 'saga'

    @Override
    void apply(final Project project) {
        project.extensions.create(PLUGIN_NAME, InstanceFieldPerPropertyConfig)
        project.task('coverage') << {
            final InstanceFieldPerPropertyConfig cfg = (InstanceFieldPerPropertyConfig) project[PLUGIN_NAME]

            CoverageGeneratorFactory.newInstance(cfg).instrumentAndGenerateReports()
        }
    }

}
