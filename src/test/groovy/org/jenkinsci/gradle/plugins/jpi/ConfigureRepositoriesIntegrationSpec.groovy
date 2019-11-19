package org.jenkinsci.gradle.plugins.jpi

import groovy.json.JsonSlurper
import groovy.transform.Canonical
import org.gradle.testkit.runner.BuildResult

class ConfigureRepositoriesIntegrationSpec extends IntegrationSpec {
    static final Repo CENTRAL = Repo.from([name: 'MavenRepo',
                                           uri : 'https://repo.maven.apache.org/maven2/',])
    static final Repo LOCAL = Repo.from([name: 'MavenLocal',
                                         uri : "file:${System.getProperty('user.home')}/.m2/repository/",])
    static final Repo JENKINS = Repo.from([name: 'jenkins',
                                           uri : 'https://repo.jenkins-ci.org/public/',])
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
        def actual = deserializeReposFrom(result)

        then:
        actual.size == 3
        actual.contains(CENTRAL)
        actual.contains(LOCAL)
        actual.contains(JENKINS)
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
        def actual = deserializeReposFrom(result)

        then:
        actual.size == 3
        actual.contains(CENTRAL)
        actual.contains(LOCAL)
        actual.contains(JENKINS)
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
        def actual = deserializeReposFrom(result)

        then:
        actual.isEmpty()
    }

    @Canonical
    private static class Repo {
        String name
        URI uri

        static Repo from(Map<String, String> m) {
            String uri = m['uri']
            new Repo(m['name'], URI.create(uri.endsWith('/') ? uri : uri + '/'))
        }
    }

    private static List<Repo> deserializeReposFrom(BuildResult result) {
        new JsonSlurper().parseText(result.output)['repositories']
                .collect { Map<String, String> m -> Repo.from(m) }
    }
}
