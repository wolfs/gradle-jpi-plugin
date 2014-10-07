package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.GradleException
import org.gradle.api.Project
import spock.lang.Specification

class JpiExtensionSpec extends Specification {
    Project project = Mock(Project)
    JpiExtension jpiExtension = new JpiExtension(project)

    def 'short name is project name when not set'() {
        when:
        project.name >> 'awesome'

        then:
        jpiExtension.shortName == 'awesome'
    }

    def 'short name is project name without -plugin suffix when not set'() {
        when:
        project.name >> 'test-plugin'

        then:
        jpiExtension.shortName == 'test'
    }

    def 'short name is used when set'() {
        when:
        jpiExtension.shortName = 'acme'

        then:
        jpiExtension.shortName == 'acme'
    }

    def 'display name is project name when not set'() {
        when:
        project.name >> 'awesome'

        then:
        jpiExtension.displayName == 'awesome'
    }

    def 'display name is project name without -plugin suffix when not set'() {
        when:
        project.name >> 'test-plugin'

        then:
        jpiExtension.displayName == 'test'
    }

    def 'display name is short name when not set'() {
        when:
        jpiExtension.shortName = 'acme'

        then:
        jpiExtension.displayName == 'acme'
    }

    def 'display name is used when set'() {
        when:
        jpiExtension.displayName = 'foo'

        then:
        jpiExtension.displayName == 'foo'
    }

    def 'file extenstion defaults to hpi if not set'(String value) {
        when:
        jpiExtension.fileExtension = value

        then:
        jpiExtension.fileExtension == 'hpi'

        where:
        value << [null, '']
    }

    def 'file extension is used when set'() {
        when:
        jpiExtension.fileExtension = 'jpi'

        then:
        jpiExtension.fileExtension == 'jpi'
    }

    def 'core versions earlier than 1.420 are not supported'(String version) {
        when:
        jpiExtension.coreVersion = version

        then:
        thrown(GradleException)

        where:
        version << ['1.419.99', '1.390', '1.1']
    }
}
