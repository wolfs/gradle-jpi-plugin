package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class TestDependenciesTaskIntegrationSpec extends Specification {
    @Rule
    final TemporaryFolder project = new TemporaryFolder()

    def 'resolves test dependencies'() {
        given:
        project.newFile('build.gradle') << getClass().getResource('resolveTestDependencies.gradle').text

        when:
        def result = GradleRunner.create()
                .withProjectDir(project.root)
                .withPluginClasspath()
                .withArguments('processTestResources')
                .build()

        then:
        result.task(':resolveTestDependencies').outcome == TaskOutcome.SUCCESS
        result.task(':processTestResources').outcome == TaskOutcome.UP_TO_DATE
        File dir = new File(project.root, 'build/resources/test/test-dependencies')
        dir.directory
        new File(dir, 'index').text == [
                'structs-1.1', 'config-file-provider-2.8.1', 'cloudbees-folder-4.4',
                'token-macro-1.5.1', 'credentials-1.9.1',
        ].join('\n')
        new File(dir, 'structs-1.1.hpi').exists()
        new File(dir, 'config-file-provider-2.8.1.hpi').exists()
        new File(dir, 'cloudbees-folder-4.4.hpi').exists()
        new File(dir, 'token-macro-1.5.1.hpi').exists()
        new File(dir, 'credentials-1.9.1.hpi').exists()
    }
}
