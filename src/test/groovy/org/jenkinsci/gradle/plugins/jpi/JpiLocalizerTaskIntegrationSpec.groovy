package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class JpiLocalizerTaskIntegrationSpec extends Specification {
    @Rule
    final TemporaryFolder project = new TemporaryFolder()

    def 'single-module project should be able to run LocalizerTask'() {
        given:
        prepareFile('build.gradle') << "plugins { id 'org.jenkins-ci.jpi' }"
        prepareFile('src/main/resources/Messages.properties') << 'key1=value1\nkey2=value2'

        when:
        def result = GradleRunner.create()
                .withProjectDir(project.root)
                .withPluginClasspath(pluginClasspath)
                .withArguments('localizer')
                .build()

        then:
        result.task(':localizer').outcome == TaskOutcome.SUCCESS
        def generatedJavaFile = new File(project.root, 'build/generated-src/localizer/Messages.java')
        generatedJavaFile.exists()
        generatedJavaFile.text.contains('public static String key1()')
        generatedJavaFile.text.contains('public static String key2()')
    }

    def 'multi-module project should be able to run LocalizerTask'() {
        given:
        prepareFile('build.gradle') << ''
        prepareFile('settings.gradle') << 'include ":plugin"'
        prepareFile('plugin/build.gradle') << "plugins { id 'org.jenkins-ci.jpi' }"
        prepareFile('plugin/src/main/resources/Messages.properties') << 'key3=value3\nkey4=value4'

        when:
        def result = GradleRunner.create()
                .withProjectDir(project.root)
                .withPluginClasspath(pluginClasspath)
                .withArguments(':plugin:localizer')
                .build()

        then:
        result.task(':plugin:localizer').outcome == TaskOutcome.SUCCESS
        def generatedJavaFile = new File(project.root, 'plugin/build/generated-src/localizer/Messages.java')
        generatedJavaFile.exists()
        generatedJavaFile.text.contains('public static String key3()')
        generatedJavaFile.text.contains('public static String key4()')
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
