package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy

class TestDependenciesTask extends Copy {
    public static final String TASK_NAME = 'resolveTestDependencies'

    TestDependenciesTask() {
        include('*.hpi')
        include('*.jpi')

        into {
            JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
            new File(javaConvention.sourceSets.test.output.resourcesDir, 'test-dependencies')
        }

        doLast {
            List<String> baseNames = source*.name.collect { it[0..it.lastIndexOf('.') - 1] }
            new File(destinationDir, 'index').setText(baseNames.join('\n'), 'UTF-8')
        }
    }
}
