package org.jenkinsci.gradle.plugins.jpi

import groovy.json.JsonSlurper

class ConfigureRepositoriesIntegrationSpec extends IntegrationSpec {
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

            tasks.create('discoverRepos') {
                doLast {
                    def repos = project.repositories
                        .findAll { it instanceof org.gradle.api.artifacts.repositories.MavenArtifactRepository }
                        .collect { [name: it.name, uri: it.url.toASCIIString()] }
                        .toSet()
                    println groovy.json.JsonOutput.toJson([repositories: repos])
                }
            }
            '''.stripIndent()
    }

    def 'adds repositories by default'() {
        when:
        def result = gradleRunner()
                .withArguments('discoverRepos', '--quiet')
                .build()
        def actual = new JsonSlurper().parseText(result.output)['repositories']

        then:
        actual.size == 3
        actual.contains([name: 'MavenRepo', uri: 'https://repo.maven.apache.org/maven2/'])
        actual.contains([name: 'MavenLocal', uri: 'file:' + System.getProperty('user.home') + '/.m2/repository/'])
        actual.contains([name: 'jenkins', uri: 'https://repo.jenkins-ci.org/public/'])
    }

    def 'adds repositories if enabled'() {
        given:
        build << '''
            jenkinsPlugin {
                configureRepositories = true
            }
            '''.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments('discoverRepos', '--quiet')
                .build()
        def actual = new JsonSlurper().parseText(result.output)['repositories']

        then:
        actual.size == 3
        actual.contains([name: 'MavenRepo', uri: 'https://repo.maven.apache.org/maven2/'])
        actual.contains([name: 'MavenLocal', uri: 'file:' + System.getProperty('user.home') + '/.m2/repository/'])
        actual.contains([name: 'jenkins', uri: 'https://repo.jenkins-ci.org/public/'])
    }

    def 'does not add repositories if disabled'() {
        given:
        build << '''
            jenkinsPlugin {
                configureRepositories = false
            }
            '''.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments('discoverRepos', '--quiet')
                .build()
        def actual = new JsonSlurper().parseText(result.output)['repositories']

        then:
        actual.isEmpty()
    }
}
