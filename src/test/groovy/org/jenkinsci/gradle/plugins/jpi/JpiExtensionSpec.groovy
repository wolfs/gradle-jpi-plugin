package org.jenkinsci.gradle.plugins.jpi

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
}
