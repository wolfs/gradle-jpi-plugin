package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.lang.Unroll

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
        file.text == readManifest('basics.mf')
    }

    def 'plugin class'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
        }
        File directory = new File(project.tasks.compileJava.destinationDir as File, 'META-INF/services')
        directory.mkdirs()
        new File(directory, 'hudson.Plugin').write('org.example.PluginImpl')

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == readManifest('plugin-class.mf')
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
        file.text == readManifest('dependency.mf')
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
        file.text == readManifest('dependencies.mf')
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
        file.text == readManifest('optional-dependency.mf')
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
        file.text == readManifest('optional-dependencies.mf')
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
        file.text == readManifest('complex-dependencies.mf')
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
        file.text == readManifest('compatible-since-version.mf')
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
        file.text == readManifest('mask-classes.mf')
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
        file.text == readManifest('plugin-first-class-loader.mf')
    }

    def 'sandbox status'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
                sandboxStatus = true
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == readManifest('sandbox-status.mf')
    }

    def 'plugin developer'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
                developers {
                    developer {
                        id 'abayer'
                        name 'Andrew Bayer'
                        email 'andrew.bayer@gmail.com'
                    }
                }
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == readManifest('plugin-developer.mf')
    }

    def 'plugin developers'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
                developers {
                    developer {
                        id 'abayer'
                        email 'andrew.bayer@gmail.com'
                    }
                    developer {
                        id 'kohsuke'
                        name 'Kohsuke Kawaguchi'
                    }
                }
            }
        }

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == readManifest('plugin-developers.mf')
    }

    @Unroll
    def 'support dynamic loading #value'(String value) {
        setup:
        project.with {
            apply plugin: 'jpi'
            group = 'org.example'
            version = '1.2'
            jenkinsPlugin {
                coreVersion = '1.509.3'
            }
        }
        byte[] index = JpiManifestSpec.getResourceAsStream("support-dynamic-loading/${value}/hudson.Extension").bytes
        File directory = new File(project.tasks.compileJava.destinationDir as File, 'META-INF/annotations')
        directory.mkdirs()
        new File(directory, 'hudson.Extension').bytes = index

        when:
        File file = temporaryFolder.newFile()
        new JpiManifest(project).writeTo(file)

        then:
        file.text == readManifest("support-dynamic-loading-${value}.mf")

        where:
        value << ['yes', 'maybe', 'no']
    }

    private static String readManifest(String fileName) {
        JpiManifestSpec.getResourceAsStream(fileName).text.replace(System.getProperty('line.separator'), '\r\n')
    }
}
