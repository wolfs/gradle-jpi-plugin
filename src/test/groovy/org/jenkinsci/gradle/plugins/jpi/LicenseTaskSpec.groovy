package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.TaskOutcome
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input

class LicenseTaskSpec extends IntegrationSpec {

    def 'compute license information'() {
        given:
        File projectFolder = projectDir.newFolder('bar')
        new File(projectFolder, 'build.gradle') << getClass().getResource('licenseInfo.gradle').text

        when:
        def result = gradleRunner()
                .withProjectDir(projectFolder)
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
