package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import spock.lang.Specification

class LicenseTaskSpec extends Specification {
    @Rule
    final TemporaryFolder temporaryFolder = new TemporaryFolder()

    def 'compute license information'() {
        given:
        File projectFolder = temporaryFolder.newFolder('bar')
        new File(projectFolder, 'build.gradle') << getClass().getResource('licenseInfo.gradle').text

        when:
        def result = GradleRunner.create()
                .withProjectDir(projectFolder)
                .withPluginClasspath()
                .withArguments('generateLicenseInfo')
                .build()

        then:
        result.task(':generateLicenseInfo').outcome == TaskOutcome.SUCCESS
        File licensesFile = new File(projectFolder, 'build/licenses/licenses.xml')
        licensesFile.exists()
        compareXml(licensesFile.text, getClass().getResource('licenses.xml').text)
    }

    private static boolean compareXml(String actual, String expected) {
        !DiffBuilder.compare(Input.fromString(actual))
                .withTest(Input.fromString(expected))
                .checkForSimilar()
                .ignoreWhitespace()
                .build()
                .hasDifferences()
    }
}
