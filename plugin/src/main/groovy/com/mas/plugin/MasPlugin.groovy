package com.mas.plugin

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class MasPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create("monitor", MasExtension)
        

        AppExtension appExtension = project.extensions.findByType(AppExtension.class)
        appExtension.registerTransform(new MasTransform(project))

        project.afterEvaluate {
            Logger.setDebug(project.monitor.debug)
        }
    }
}