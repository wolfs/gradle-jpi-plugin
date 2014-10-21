package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiDeveloperSpec extends Specification {
    Logger logger = Mock(Logger)

    def 'set and get properties'() {
        setup:
        JpiDeveloper developer = new JpiDeveloper(logger)

        when:
        developer.foo = 'bar'
        developer.name = 'me'

        then:
        developer.foo == 'bar'
        developer.name == 'me'
    }

    def 'supported methods'() {
        setup:
        JpiDeveloper developer = new JpiDeveloper(logger)

        when:
        developer.id('test')
        developer.name('me')
        developer.email('me@example.org')
        developer.url('https://example.org/me')
        developer.organization('Example Inc.')
        developer.organizationUrl('https://example.org')
        developer.timezone('UTC')

        then:
        developer.id == 'test'
        developer.name == 'me'
        developer.email == 'me@example.org'
        developer.url == 'https://example.org/me'
        developer.organization == 'Example Inc.'
        developer.organizationUrl == 'https://example.org'
        developer.timezone == 'UTC'
    }

    def 'unsupported methods'() {
        setup:
        JpiDeveloper developer = new JpiDeveloper(logger)

        when:
        developer.foo('bar')

        then:
        developer.foo == null
        1 * logger.log(LogLevel.WARN, 'JPI POM developer field foo not implemented.')
    }

    def 'configure'() {
        setup:
        JpiDeveloper developer = new JpiDeveloper(logger)

        when:
        developer.configure {
            id = 'test'
            name('me')
        }

        then:
        developer.id == 'test'
        developer.name == 'me'
    }
}
