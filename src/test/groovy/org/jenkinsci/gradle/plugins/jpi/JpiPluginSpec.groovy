package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
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
        'jpi'        | Task
        'server'     | ServerTask
        'localizer'  | LocalizerTask
        'insertTest' | TestInsertionTask
    }

    def 'publishing has been setup'(String projectVersion, String repositoryUrl, String extension) {
        when:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = projectVersion
            jenkinsPlugin {
                shortName = 'foo'
                fileExtension = extension
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)
        publishingExtension.publications.size() == 1
        publishingExtension.publications.getByName('mavenJpi') instanceof MavenPublication
        MavenPublication mavenPublication = publishingExtension.publications.getByName('mavenJpi') as MavenPublication
        mavenPublication.groupId == 'org.example'
        mavenPublication.artifactId == 'foo'
        mavenPublication.version == projectVersion
        mavenPublication.pom.packaging == extension
        mavenPublication.artifacts.size() == 4
        publishingExtension.repositories.size() == 1
        publishingExtension.repositories.get(0).name == 'jenkins'
        publishingExtension.repositories.get(0) instanceof MavenArtifactRepository
        (publishingExtension.repositories.get(0) as MavenArtifactRepository).url == URI.create(repositoryUrl)

        where:
        projectVersion   | repositoryUrl                           | extension
        '1.0.0'          | 'https://repo.jenkins-ci.org/releases'  | 'jpi'
        '1.0.0-SNAPSHOT' | 'https://repo.jenkins-ci.org/snapshots' | 'jpi'
        '1.0.0'          | 'https://repo.jenkins-ci.org/releases'  | 'hpi'
        '1.0.0-SNAPSHOT' | 'https://repo.jenkins-ci.org/snapshots' | 'hpi'
    }

    def 'jpi task has been setup'() {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                shortName = name
                fileExtension = extension
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        Task warTask = project.tasks[WarPlugin.WAR_TASK_NAME]
        warTask != null
        warTask.description != null
        warTask.group == BasePlugin.BUILD_GROUP

        Task jpiTask = project.tasks[JpiPlugin.JPI_TASK_NAME]
        jpiTask != null
        jpiTask.description != null
        jpiTask.group == BasePlugin.BUILD_GROUP

        where:
        name  | extension
        ''    | 'jpi'
        null  | 'jpi'
        'foo' | 'jpi'
        ''    | 'hpi'
        null  | 'hpi'
        'foo' | 'hpi'
    }

    def 'publishing configuration has been skipped'() {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                configurePublishing = false
            }
        }
        (project as ProjectInternal).evaluate()
        project.extensions.getByType(PublishingExtension)

        then:
        UnknownDomainObjectException ex = thrown()
        ex.message.contains("Extension of type 'PublishingExtension' does not exist.")
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
        javaConvention.sourceSets.main.java.srcDirs.contains(localizerTask.destinationDir)

        where:
        outputDir       | expectedOutputDir
        null            | 'build/generated-src/localizer'
        'foo'           | 'foo'
        new File('bar') | 'bar'
    }

    def 'repositories have been setup'() {
        when:
        project.with {
            apply plugin: 'jpi'
        }
        (project as ProjectInternal).evaluate()

        then:
        project.repositories.size() == 3
        project.repositories.get(0) instanceof MavenArtifactRepository
        project.repositories.get(0).name == 'MavenRepo'
        project.repositories.get(1) instanceof MavenArtifactRepository
        project.repositories.get(1).name == 'MavenLocal'
        project.repositories.get(2) instanceof MavenArtifactRepository
        project.repositories.get(2).name == 'jenkins'
        (project.repositories.get(2) as MavenArtifactRepository).url ==
                URI.create('https://repo.jenkins-ci.org/public/')
    }

    def 'repositories have not been setup'() {
        when:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                configureRepositories = false
            }
        }
        (project as ProjectInternal).evaluate()

        then:
        project.repositories.size() == 0
    }
}
