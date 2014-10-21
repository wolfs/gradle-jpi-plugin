package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiHplManifestSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    def 'basics'() {
        setup:
        project.with {
            apply plugin: 'jpi'
        }
        def libraries = [
                new File(project.projectDir, 'src/main/resources'),
                new File(project.buildDir, 'classes/main'),
                new File(project.buildDir, 'resources/main'),
        ]
        libraries*.mkdirs()

        when:
        JpiHplManifest manifest = new JpiHplManifest(project)

        then:
        manifest['Resource-Path'] == new File(project.projectDir, 'src/main/webapp').path
        manifest['Libraries'] == libraries*.path.join(',')
    }

    def 'non-existing libraries are ignored'() {
        setup:
        project.with {
            apply plugin: 'jpi'
        }

        when:
        JpiHplManifest manifest = new JpiHplManifest(project)

        then:
        manifest['Resource-Path'] == new File(project.projectDir, 'src/main/webapp').path
        manifest['Libraries'] == ''
    }
}
