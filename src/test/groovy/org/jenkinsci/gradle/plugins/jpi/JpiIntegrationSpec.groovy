package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.TaskOutcome
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

    @Unroll
    def '#task should run #dependency'(String task, String dependency, TaskOutcome outcome) {
        when:
        def result = gradleRunner()
                .withArguments(task)
                .build()

        then:
        result.task(dependency).outcome == outcome

        where:
        task                   | dependency                 | outcome
        'jar'                  | ':configureManifest'       | TaskOutcome.SUCCESS
        'war'                  | ':configureManifest'       | TaskOutcome.SUCCESS
        'processTestResources' | ':resolveTestDependencies' | TaskOutcome.NO_SOURCE
        'jpi'                  | ':war'                     | TaskOutcome.SUCCESS
        'compileTestJava'      | ':insertTest'              | TaskOutcome.SKIPPED
    }

    @Unroll
    def 'compileTestJava should run :insertTest as #outcome (configured: #value)'(boolean value, TaskOutcome outcome) {
        given:
        build << """
            jenkinsPlugin {
                coreVersion = '2.190.2'
                disabledTestInjection = $value
            }
            """.stripIndent()

        when:
        def result = gradleRunner()
                .withArguments('compileTestJava')
                .build()

        then:
        result.task(':insertTest').outcome == outcome

        where:
        value | outcome
        true  | TaskOutcome.SKIPPED
        false | TaskOutcome.SUCCESS
    }

    def 'set buildDirectory system property in test'() {
        given:
        build << '''
            repositories { mavenCentral() }
            dependencies {
                testImplementation 'junit:junit:4.12'
            }
            '''.stripIndent()
        projectDir.newFolder('src', 'test', 'java')
        def actualFile = projectDir.newFile()
        def normalizedPath = actualFile.absolutePath.replaceAll('\\\\', '/')
        def file = projectDir.newFile('src/test/java/ExampleTest.java')
        file << """
            public class ExampleTest {
                @org.junit.Test
                public void shouldHaveSystemPropertySet() throws Exception {
                    java.nio.file.Files.write(
                        java.nio.file.Paths.get("${normalizedPath}"),
                        java.util.Collections.singletonList(System.getProperty("buildDirectory")),
                        java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            """.stripIndent()

        when:
        gradleRunner()
                .withArguments('test')
                .build()

        then:
        def expected = new File(projectDir.root, 'build').toPath().toRealPath().toString()
        actualFile.text.trim() == expected
    }
}
