package org.jenkinsci.gradle.plugins.jpi

import org.custommonkey.xmlunit.XMLUnit
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JpiPomCustomizerSpec extends Specification {
    Project project = ProjectBuilder.builder().build()
    Node pom = new Node(null, 'project')

    def setup() {
        XMLUnit.ignoreWhitespace = true
    }

    def 'minimal POM'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
        }
        (project as ProjectInternal).evaluate()

        when:
        new JpiPomCustomizer(project).customizePom(pom)

        then:
        compareXml('minimal-pom.xml', pom)
    }

    def 'POM with all metadata'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            description = 'lorem ipsum'
            jenkinsPlugin {
                coreVersion = '1.580.1'
                url = 'https://lorem-ipsum.org'
                gitHubUrl = 'https://github.com/lorem/ipsum'
                developers {
                    developer {
                        id 'abayer'
                        name 'Andrew Bayer'
                        email 'andrew.bayer@gmail.com'
                    }
                }
            }
            repositories {
                maven {
                    name 'lorem-ipsum'
                    url 'https://repo.lorem-ipsum.org/'
                }
            }
        }
        (project as ProjectInternal).evaluate()

        when:
        new JpiPomCustomizer(project).customizePom(pom)

        then:
        compareXml('complex-pom.xml', pom)
    }

    def 'gitHubUrl not pointing to GitHub'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.580.1'
                gitHubUrl = 'https://bitbucket.org/lorem/ipsum'
            }
        }
        (project as ProjectInternal).evaluate()

        when:
        new JpiPomCustomizer(project).customizePom(pom)

        then:
        compareXml('bitbucket-pom.xml', pom)
    }

    def 'mavenLocal is ignored'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            repositories {
                mavenLocal()
            }
        }
        (project as ProjectInternal).evaluate()

        when:
        new JpiPomCustomizer(project).customizePom(pom)

        then:
        compareXml('minimal-pom.xml', pom)
    }

    def 'mavenCentral is ignored'() {
        setup:
        project.with {
            apply plugin: 'jpi'
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            repositories {
                mavenCentral()
            }
        }
        (project as ProjectInternal).evaluate()

        when:
        new JpiPomCustomizer(project).customizePom(pom)

        then:
        compareXml('minimal-pom.xml', pom)
    }

    private static boolean compareXml(String fileName, Node node) {
        XMLUnit.compareXML(readXml(fileName), toXml(node)).similar()
    }

    private static String readXml(String fileName) {
        JpiPomCustomizerSpec.getResourceAsStream(fileName).text
    }

    private static String toXml(Node node) {
        Writer buffer = new StringWriter()
        new XmlNodePrinter(new PrintWriter(buffer)).print(node)
        buffer.toString()
    }
}
