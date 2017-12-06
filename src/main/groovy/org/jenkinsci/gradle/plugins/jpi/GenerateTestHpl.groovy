package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class GenerateTestHpl extends DefaultTask {
    @OutputFile
    DirectoryProperty hplDir

    GenerateTestHpl() {
        this.hplDir = newOutputDirectory()
    }

    @TaskAction
    void generateTestHpl() {
        hplDir.file('the.hpl').get().asFile.withOutputStream { new JpiHplManifest(project).write(it) }
    }
}
