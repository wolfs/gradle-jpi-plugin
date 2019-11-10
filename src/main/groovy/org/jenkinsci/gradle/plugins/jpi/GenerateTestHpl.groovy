package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GradleVersion

class GenerateTestHpl extends DefaultTask {
    private static final GradleVersion GRADLE_5_0 = GradleVersion.version('5.0')
    @OutputDirectory
    final DirectoryProperty hplDir

    GenerateTestHpl() {
        if (GradleVersion.current() >= GRADLE_5_0) {
            this.hplDir = services.get(ObjectFactory).directoryProperty()
        } else {
            this.hplDir = newOutputDirectory()
        }
    }

    @TaskAction
    void generateTestHpl() {
        hplDir.file('the.hpl').get().asFile.withOutputStream { new JpiHplManifest(project).write(it) }
    }
}
