package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenerateTestHpl extends DefaultTask {
    @OutputDirectory
    DirectoryProperty hplDir

    GenerateTestHpl() {
        this.hplDir = newOutputDirectory()
    }

    @TaskAction
    void generateTestHpl() {
        hplDir.file('the.hpl').get().asFile.withOutputStream { new JpiHplManifest(project).write(it) }
    }
}
