package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input

class TestDependenciesTask extends Copy {
    public static final String TASK_NAME = 'resolveTestDependencies'

    private Configuration configuration

    protected Map<String, String> mapping = [:]

    TestDependenciesTask() {
        include('*.hpi')
        include('*.jpi')

        into {
            JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
            new File(javaConvention.sourceSets.test.output.resourcesDir, 'test-dependencies')
        }

        rename { mapping[it] }

        doLast {
            List<String> baseNames = source*.name.collect { mapping[it] }.collect { it[0..it.lastIndexOf('.') - 1] }
            new File(destinationDir, 'index').setText(baseNames.join('\n'), 'UTF-8')
        }
    }

    @Override
    protected void copy() {
        configuration.resolvedConfiguration.resolvedArtifacts.each {
            mapping[it.file.name] = "${it.name}.${it.extension}"
        }

        super.copy()
    }

    @Input
    void setConfiguration(Configuration configuration) {
        this.configuration = configuration
        this.from(configuration)
    }
}
