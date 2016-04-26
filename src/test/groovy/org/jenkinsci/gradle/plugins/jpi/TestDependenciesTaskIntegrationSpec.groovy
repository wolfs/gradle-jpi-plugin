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
        prepareFile('build.gradle') << getClass().getResource('resolveTestDependencies.gradle').text

        when:
        def result = GradleRunner.create()
                .withProjectDir(project.root)
                .withPluginClasspath(pluginClasspath)
                .withArguments('processTestResources')
                .build()

        then:
        result.task(':resolveTestDependencies').outcome == TaskOutcome.SUCCESS
        result.task(':processTestResources').outcome == TaskOutcome.UP_TO_DATE
        File dir = new File(project.root, 'build/resources/test/test-dependencies')
        dir.directory
        new File(dir, 'index').text == 'structs\nconfig-file-provider\ncloudbees-folder\ntoken-macro\ncredentials'
        new File(dir, 'structs.hpi').exists()
        new File(dir, 'config-file-provider.hpi').exists()
        new File(dir, 'cloudbees-folder.hpi').exists()
        new File(dir, 'token-macro.hpi').exists()
        new File(dir, 'credentials.hpi').exists()
    }

    private File prepareFile(String relativeFilePath) {
        def newFile = new File(project.root, relativeFilePath)
        newFile.parentFile.mkdirs()
        newFile.createNewFile()
        newFile
    }

    private List<File> getPluginClasspath() {
        getClass()
                .classLoader
                .getResource('plugin-classpath.txt')
                .readLines()
                .collect { new File(it) }
    }
}
