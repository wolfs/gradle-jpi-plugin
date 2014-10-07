package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiPluginSpec extends Specification {
    def 'tasks exist'(String taskName, Class taskClass) {
        when:
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'jpi'

        then:
        taskClass.isInstance(project.tasks[taskName])

        where:
        taskName    | taskClass
        'jpi'       | Jpi
        'server'    | ServerTask
        'localizer' | LocalizerTask
        'stapler'   | StaplerGroovyStubsTask
    }
}
