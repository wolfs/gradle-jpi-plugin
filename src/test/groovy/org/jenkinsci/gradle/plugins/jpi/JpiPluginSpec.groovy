package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiPluginSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    def 'test plugin id'(String pluginId) {
        when:
        project.apply plugin: pluginId

        then:
        project.plugins[pluginId] instanceof JpiPlugin

        where:
        pluginId << ['jpi', 'org.jenkins-ci.jpi']
    }

    def 'tasks exist'(String taskName, Class taskClass) {
        when:
        project.apply plugin: 'jpi'

        then:
        taskClass.isInstance(project.tasks[taskName])

        where:
        taskName     | taskClass
        'jpi'        | Jpi
        'server'     | ServerTask
        'localizer'  | LocalizerTask
        'stapler'    | StaplerGroovyStubsTask
        'insertTest' | TestInsertionTask
    }

    def 'localizer task has been setup'(Object outputDir, String expectedOutputDir) {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                localizerOutputDir = outputDir
            }
        }

        then:
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        LocalizerTask localizerTask = project.tasks.findByName('localizer') as LocalizerTask
        localizerTask != null
        localizerTask.destinationDir == new File(project.rootDir, expectedOutputDir)
        localizerTask.sourceDirs == javaConvention.sourceSets.main.resources.srcDirs
        project.tasks.compileJava.dependsOn.contains(localizerTask)
        javaConvention.sourceSets.main.java.srcDirs.contains(localizerTask.destinationDir)

        where:
        outputDir       | expectedOutputDir
        null            | 'build/generated-src/localizer'
        'foo'           | 'foo'
        new File('bar') | 'bar'
    }

    def 'testCompile configuration contains plugin JAR dependencies'() {
        setup:
        Project project = ProjectBuilder.builder().build()

        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.554.2'
            }
        }

        then:
        def dependencies = collectDependencies(project, 'testCompile')
        'org.jenkins-ci.main:maven-plugin:2.1@jar' in dependencies
        'org.jenkins-ci.plugins:ant:1.2@jar' in dependencies
        'org.jenkins-ci.plugins:antisamy-markup-formatter:1.0@jar' in dependencies
        'org.jenkins-ci.plugins:javadoc:1.0@jar' in dependencies
        'org.jenkins-ci.plugins:mailer:1.8@jar' in dependencies
        'org.jenkins-ci.plugins:matrix-auth:1.0.2@jar' in dependencies
        'org.jenkins-ci.plugins:subversion:1.45@jar' in dependencies
    }

    private static List<String> collectDependencies(Project project, String configuration) {
        project.configurations.getByName(configuration).resolvedConfiguration.resolvedArtifacts.collect {
            "${it.moduleVersion.id}@${it.extension}".toString()
        }
    }
}
