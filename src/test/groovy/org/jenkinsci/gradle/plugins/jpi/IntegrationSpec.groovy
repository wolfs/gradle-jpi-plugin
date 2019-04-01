package org.jenkinsci.gradle.plugins.jpi

import groovy.transform.CompileStatic
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.experimental.categories.Category
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

@CompileStatic
@Category(UsesGradleTestKit)
class IntegrationSpec extends Specification {
    @Rule
    protected final TemporaryFolder projectDir = new TemporaryFolder()

    protected GradleRunner gradleRunner() {
        def runner = GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir.root)
        if (System.getProperty('gradle.under.test')) {
            return runner.withGradleVersion(System.getProperty('gradle.under.test'))
        }
        runner
    }
}
