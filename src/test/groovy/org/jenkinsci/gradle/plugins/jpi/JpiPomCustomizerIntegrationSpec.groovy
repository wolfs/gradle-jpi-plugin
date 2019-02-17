package org.jenkinsci.gradle.plugins.jpi

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import spock.lang.Specification

class JpiPomCustomizerIntegrationSpec extends Specification {
    @Rule
    private final TemporaryFolder projectDir = new TemporaryFolder()
    private File settings
    private File build

    def setup() {
        settings = projectDir.newFile('settings.gradle')
        settings << 'rootProject.name = "test"'
        build = projectDir.newFile('build.gradle')
        build << """\
            plugins {
                id 'org.jenkins-ci.jpi'
            }
            """.stripIndent()
    }

    def 'minimal POM'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('minimal-pom.xml', actualPomIn(projectDir))
    }

    def 'POM with all metadata'() {
        setup:
        build << """\
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
                licenses {
                    license {
                        name 'Apache License, Version 2.0'
                        url 'https://www.apache.org/licenses/LICENSE-2.0.txt'
                        distribution 'repo'
                        comments 'A business-friendly OSS license'
                    }
                }
            }
            repositories {
                maven {
                    name 'lorem-ipsum'
                    url 'https://repo.lorem-ipsum.org/'
                }
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('complex-pom.xml', actualPomIn(projectDir))
    }

    def 'gitHubUrl not pointing to GitHub'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
                gitHubUrl = 'https://bitbucket.org/lorem/ipsum'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('bitbucket-pom.xml', actualPomIn(projectDir))
    }

    def 'mavenLocal is ignored'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            repositories {
                mavenLocal()
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('minimal-pom.xml', actualPomIn(projectDir))
    }

    def 'mavenCentral is ignored'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            repositories {
                mavenCentral()
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('minimal-pom.xml', actualPomIn(projectDir))
    }

    def 'plugin dependencies'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            dependencies {
                jenkinsPlugins 'org.jenkins-ci.plugins:credentials:1.9.4'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('plugin-dependencies-pom.xml', actualPomIn(projectDir))
    }

    def 'optional plugin dependencies'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            dependencies {
                optionalJenkinsPlugins 'org.jenkins-ci.plugins:credentials:1.9.4'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('optional-plugin-dependencies-pom.xml', actualPomIn(projectDir))
    }

    def 'compile dependencies'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            dependencies {
                compile 'javax.ejb:ejb:2.1'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('compile-dependencies-pom.xml', actualPomIn(projectDir))
    }

    def 'compile dependencies with excludes'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.580.1'
            }
            dependencies {
                compile('org.bitbucket.b_c:jose4j:0.5.5') {
                    exclude group: 'org.slf4j', module: 'slf4j-api'
                }
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('compile-dependencies-with-excludes-pom.xml', actualPomIn(projectDir))
    }

    def 'updated FindBugs version in 1.618'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '1.618'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('updated-findbugs-pom.xml', actualPomIn(projectDir))
    }

    def 'updated Servlet API version in 2.0'() {
        setup:
        build << """\
            jenkinsPlugin {
                coreVersion = '2.0'
            }
            """.stripIndent()

        when:
        generatePomIn(projectDir)

        then:
        compareXml('updated-servlet-api-pom.xml', actualPomIn(projectDir))
    }

    private static boolean compareXml(String fileName, File actual) {
        !DiffBuilder.compare(Input.fromString(readXml(fileName)))
                .withTest(Input.fromString(toXml(new XmlParser().parse(actual))))
                .checkForSimilar()
                .ignoreWhitespace()
                .build()
                .hasDifferences()
    }

    private static String readXml(String fileName) {
        JpiPomCustomizerIntegrationSpec.getResourceAsStream(fileName).text
    }

    private static String toXml(Node node) {
        Writer buffer = new StringWriter()
        new XmlNodePrinter(new PrintWriter(buffer)).print(node)
        buffer.toString()
    }

    static void generatePomIn(TemporaryFolder projectDir) {
        GradleRunner.create()
                .withPluginClasspath()
                .withProjectDir(projectDir.root)
                .withArguments('generatePomFileForMavenJpiPublication')
                .build()
    }

    static File actualPomIn(TemporaryFolder projectDir) {
        new File(projectDir.root, 'build/publications/mavenJpi/pom-default.xml')
    }

}
