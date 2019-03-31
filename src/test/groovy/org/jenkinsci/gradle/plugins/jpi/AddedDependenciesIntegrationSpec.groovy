package org.jenkinsci.gradle.plugins.jpi

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class AddedDependenciesIntegrationSpec extends Specification {
    @Rule
    private final TemporaryFolder projectDir = new TemporaryFolder()
    private final String projectName = TestDataGenerator.generateName()
    private File settings
    private File build

    def setup() {
        settings = projectDir.newFile('settings.gradle')
        settings << """rootProject.name = \"$projectName\""""
        build = projectDir.newFile('build.gradle')
        build << '''\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            '''.stripIndent()
    }

    def 'resolves test dependencies'() {
        given:
        build << '''\
            dependencies {
                jenkinsPlugins 'org.jenkins-ci.plugins:structs:1.1@jar'
                optionalJenkinsPlugins 'org.jenkins-ci.plugins:config-file-provider:2.8.1@jar'
                jenkinsTest 'org.jenkins-ci.plugins:cloudbees-folder:4.4@jar'
            }
            '''.stripIndent()

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withPluginClasspath()
                .withArguments('processTestResources', '--stacktrace')
                .build()

        then:
        result.task(':resolveTestDependencies').outcome == TaskOutcome.SUCCESS
        result.task(':processTestResources').outcome == TaskOutcome.NO_SOURCE
        File dir = new File(projectDir.root, 'build/resources/test/test-dependencies')
        dir.directory
        new File(dir, 'index').text == [
                'structs', 'config-file-provider', 'cloudbees-folder',
                'token-macro', 'credentials',
        ].join('\n')
        new File(dir, 'structs.hpi').exists()
        new File(dir, 'config-file-provider.hpi').exists()
        new File(dir, 'cloudbees-folder.hpi').exists()
        new File(dir, 'token-macro.hpi').exists()
        new File(dir, 'credentials.hpi').exists()
    }

    def 'testCompileClasspath configuration contains plugin JAR dependencies'() {
        given:
        projectDir.newFolder('build')
        build << '''\
            jenkinsPlugin {
                coreVersion = '1.554.2'
            }
            tasks.register('writeAllResolvedDependencies') {
                def output = new File(project.buildDir, 'resolved-dependencies.json')
                outputs.file(output)
                doLast {
                    output.createNewFile()
                    def artifactsByConfiguration = configurations.findAll { it.canBeResolved }.collectEntries { c ->
                        def artifacts = c.resolvedConfiguration.resolvedArtifacts.collect {
                            it.moduleVersion.id.toString() + '@' + it.extension.toString()
                        }
                        [(c.name): artifacts]
                    }
                    output.text = groovy.json.JsonOutput.toJson(artifactsByConfiguration)
                }
            }
            '''.stripIndent()

        when:
        GradleRunner.create()
                .withProjectDir(projectDir.root)
                .withPluginClasspath()
                .withArguments('writeAllResolvedDependencies')
                .build()
        def resolutionJson = new File(projectDir.root, 'build/resolved-dependencies.json')
        def resolvedDependencies = new JsonSlurper().parse(resolutionJson)

        then:
        def testCompileClasspath = resolvedDependencies['testCompileClasspath']
        'org.jenkins-ci.plugins:ant:1.2@jar' in testCompileClasspath
        'org.jenkins-ci.main:maven-plugin:2.1@jar' in testCompileClasspath
        'org.jenkins-ci.plugins:antisamy-markup-formatter:1.0@jar' in testCompileClasspath
        'org.jenkins-ci.plugins:javadoc:1.0@jar' in testCompileClasspath
        'org.jenkins-ci.plugins:mailer:1.8@jar' in testCompileClasspath
        'org.jenkins-ci.plugins:matrix-auth:1.0.2@jar' in testCompileClasspath
        'org.jenkins-ci.plugins:subversion:1.45@jar' in testCompileClasspath
    }
}
