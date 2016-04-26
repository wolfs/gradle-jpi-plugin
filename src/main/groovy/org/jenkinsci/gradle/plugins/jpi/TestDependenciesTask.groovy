package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import static org.gradle.util.GFileUtils.copyFile

class TestDependenciesTask extends DefaultTask {
    public static final String TASK_NAME = 'resolveTestDependencies'

    @InputFiles
    Configuration pluginsConfiguration

    @OutputDirectory
    File testDependenciesDir

    TestDependenciesTask() {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        testDependenciesDir = new File(javaConvention.sourceSets.test.output.resourcesDir, 'test-dependencies')
    }

    @TaskAction
    void resolveTestDependencies() {
        List<String> artifacts = []
        pluginsConfiguration.resolvedConfiguration.resolvedArtifacts.findAll { it.extension in ['hpi', 'jpi'] }.each {
            copyFile(it.file, new File(testDependenciesDir, "${it.name}.${it.extension}"))
            artifacts << it.name
        }
        new File(testDependenciesDir, 'index').setText(artifacts.join('\n'), 'UTF-8')
    }
}
