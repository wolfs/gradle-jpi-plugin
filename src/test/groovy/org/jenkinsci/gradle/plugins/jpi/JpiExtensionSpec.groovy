package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
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

    def 'file extension defaults to hpi if not set'(String value) {
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

    def 'stapler stub directory defaults to generated-src/stubs if not set'(String value) {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.staplerStubDir = value

        then:
        jpiExtension.staplerStubDir == new File(project.buildDir, 'generated-src/stubs')

        where:
        value << [null, '']
    }

    def 'stapler stub directory is used when set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.staplerStubDir = 'foo'

        then:
        jpiExtension.staplerStubDir == new File(project.buildDir, 'foo')
    }

    def 'localizer destination directory defaults to generated-src/localizer if not set'(String value) {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.localizerDestDir = value

        then:
        jpiExtension.localizerDestDir == new File(project.buildDir, 'generated-src/localizer')

        where:
        value << [null, '']
    }

    def 'localizer destination directory is used when set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.localizerDestDir = 'foo'

        then:
        jpiExtension.localizerDestDir == new File(project.buildDir, 'foo')
    }

    def 'work directory defaults to work if not set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        jpiExtension.workDir = null

        then:
        jpiExtension.workDir == new File(project.rootDir, 'work')
    }

    def 'work directory is used when set'() {
        when:
        Project project = ProjectBuilder.builder().build()
        JpiExtension jpiExtension = new JpiExtension(project)
        File dir = new File('/tmp/foo')
        jpiExtension.workDir = dir

        then:
        jpiExtension.workDir == dir
    }

    def 'repo URL defaults to maven.jenkins-ci.org if not set'(String value) {
        when:
        jpiExtension.repoUrl = value

        then:
        jpiExtension.repoUrl == 'http://maven.jenkins-ci.org:8081/content/repositories/releases'

        where:
        value << [null, '']
    }

    def 'repo URL is used when set'() {
        when:
        jpiExtension.repoUrl = 'https://maven.example.org/'

        then:
        jpiExtension.repoUrl == 'https://maven.example.org/'
    }

    def 'snapshot repo URL defaults to maven.jenkins-ci.org if not set'(String value) {
        when:
        jpiExtension.snapshotRepoUrl = value

        then:
        jpiExtension.snapshotRepoUrl == 'http://maven.jenkins-ci.org:8081/content/repositories/snapshots'

        where:
        value << [null, '']
    }

    def 'snapshot repo URL is used when set'() {
        when:
        jpiExtension.snapshotRepoUrl = 'https://maven.example.org/'

        then:
        jpiExtension.snapshotRepoUrl == 'https://maven.example.org/'
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
