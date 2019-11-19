package org.jenkinsci.gradle.plugins.jpi

import spock.lang.Unroll

class JpiIntegrationSpec extends IntegrationSpec {
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

    def 'uses hpi file extension by default'() {
        when:
        gradleRunner()
                .withArguments('jpi')
                .build()

        then:
        new File(projectDir.root, "build/libs/${projectName}.hpi").exists()
    }

    @Unroll
    def 'uses #declaration'(String declaration, String expected) {
        given:
        build << """
            jenkinsPlugin {
                $declaration
            }
            """.stripIndent()

        when:
        gradleRunner()
                .withArguments('jpi')
                .build()

        then:
        new File(projectDir.root, "build/libs/${projectName}.${expected}").exists()

        where:
        declaration             | expected
        'fileExtension = "hpi"' | 'hpi'
        'fileExtension "hpi"'   | 'hpi'
        'fileExtension = "jpi"' | 'jpi'
        'fileExtension "jpi"'   | 'jpi'
    }

    def 'uses project name as shortName by default'() {
        when:
        gradleRunner()
                .withArguments('jpi')
                .build()

        then:
        new File(projectDir.root, "build/libs/${projectName}.hpi").exists()
    }

    def 'uses project name with trimmed -plugin as shortName by default'() {
        given:
        def expected = 'test-333'
        settings.text = "rootProject.name = '$expected-plugin'"

        when:
        gradleRunner()
                .withArguments('jpi')
                .build()

        then:
        new File(projectDir.root, "build/libs/${expected}.hpi").exists()
    }

    @Unroll
    def 'uses #shortName'(String shortName, String expected) {
        given:
        build << """
            jenkinsPlugin {
                $shortName
            }
            """.stripIndent()

        when:
        gradleRunner()
                .withArguments('jpi')
                .build()

        then:
        new File(projectDir.root, "build/libs/${expected}.hpi").exists()

        where:
        shortName                     | expected
        "shortName = 'apple'"         | 'apple'
        "shortName 'banana'"          | 'banana'
        "shortName = 'carrot-plugin'" | 'carrot-plugin'
        "shortName 'date'"            | 'date'
    }
}
