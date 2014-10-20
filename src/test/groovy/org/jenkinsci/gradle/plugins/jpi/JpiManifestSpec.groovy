package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class JpiManifestSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    def 'basics'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('basics.mf').text
    }

    def 'dependency'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.0'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
            dependencies {
                jenkinsPlugins 'org.jenkins-ci.plugins:ant:1.2@jar'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('dependency.mf').text
    }

    def 'dependencies'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.0'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
            dependencies {
                jenkinsPlugins 'org.jenkinsci.plugins:git:1.1.15@jar'
                jenkinsPlugins 'org.jenkins-ci.plugins:ant:1.2@jar'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('dependencies.mf').text
    }

    def 'optional dependency'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.0'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
            dependencies {
                optionalJenkinsPlugins 'org.jenkins-ci.plugins:ant:1.2@jar'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('optional-dependency.mf').text
    }

    def 'optional dependencies'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.0'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
            dependencies {
                optionalJenkinsPlugins 'org.jenkinsci.plugins:git:1.1.15@jar'
                optionalJenkinsPlugins 'org.jenkins-ci.plugins:ant:1.2@jar'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('optional-dependencies.mf').text
    }

    def 'complex dependencies'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.0'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
            dependencies {
                jenkinsPlugins 'org.jenkinsci.plugins:git:1.1.15@jar'
                jenkinsPlugins 'org.jenkins-ci.plugins:ant:1.2@jar'
                optionalJenkinsPlugins 'org.jenkins-ci.plugins:cloudbees-folder:4.2@jar'
                optionalJenkinsPlugins 'org.jenkins-ci.plugins:credentials:1.9.4@jar'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('complex-dependencies.mf').text
    }

    def 'compatible since version'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
                compatibleSinceVersion = '1.1'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('compatible-since-version.mf').text
    }

    def 'mask classes'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
                maskClasses = 'org.example.test'
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('mask-classes.mf').text
    }

    def 'plugin first class loader'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
                pluginFirstClassLoader = true
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == JpiManifestSpec.getResourceAsStream('plugin-first-class-loader.mf').text
    }
}
