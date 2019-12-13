import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

plugins {
    groovy
    `maven-publish`
    signing
    codenarc
    id("com.gradle.plugin-publish") version "0.10.1"
    `java-gradle-plugin`
}

repositories {
    // using JCenter for dependency resolution is recommended, see https://plugins.gradle.org/docs/publish-plugin
    jcenter()
}

java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

val sezpoz = "net.java.sezpoz:sezpoz:1.13"

dependencies {
    annotationProcessor(sezpoz)
    implementation(gradleApi())
    implementation("org.jvnet.localizer:maven-localizer-plugin:1.24")
    implementation("org.jenkins-ci:version-number:1.1")
    implementation(sezpoz)
    implementation(localGroovy())
    testAnnotationProcessor(sezpoz)
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
        exclude(module = "groovy-all") // use the version that is distributed with Gradle
    }
    testImplementation("org.xmlunit:xmlunit-core:2.6.3")
    testImplementation("org.apache.commons:commons-text:1.8")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            pom {
                name.set("Gradle JPI Plugin")
                description.set("The Gradle JPI plugin is a Gradle plugin for building Jenkins plugins")
                url.set("http://github.com/jenkinsci/gradle-jpi-plugin")
                scm {
                    url.set("https://github.com/jenkinsci/gradle-jpi-plugin")
                }
                licenses {
                    license {
                        name.set("Apache 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("abayer")
                        name.set("Andrew Bayer")
                    }
                    developer {
                        id.set("kohsuke")
                        name.set("Kohsuke Kawaguchi")
                    }
                    developer {
                        id.set("daspilker")
                        name.set("Daniel Spilker")
                    }
                    developer {
                        id.set("sghill")
                        name.set("Steve Hill")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val path = if (version.toString().endsWith("SNAPSHOT")) "snapshots" else "releases"
            name = "JenkinsCommunity"
            url = uri("https://repo.jenkins-ci.org/${path}")
            credentials {
                username = project.stringProp("jenkins.username")
                password = project.stringProp("jenkins.password")
            }
        }
    }
}

signing {
    useGpgCmd()
    setRequired { setOf("jenkins.username", "jenkins.password").all { project.hasProperty(it) } }
    sign(publishing.publications["pluginMaven"])
}

tasks.addRule("Pattern: testGradle<ID>") {
    val taskName = this
    if (!taskName.startsWith("testGradle")) return@addRule
    tasks.register<Test>(taskName) {
        val t = this
        val gradleVersion = taskName.substringAfter("testGradle")
        t.systemProperty("gradle.under.test", gradleVersion)
        t.useJUnit {
            includeCategories("org.jenkinsci.gradle.plugins.jpi.UsesGradleTestKit")
        }
        t.onlyIf {
            gradleVersion.startsWith('4') && System.getProperty("java.specification.version") == "1.8" ||
                    gradleVersion.startsWith('5')
        }
    }
}

val check = tasks.getByPath("check")
setOf("4.10.3", "5.6.4")
        .map { tasks.named("testGradle$it") }
        .forEach { check.dependsOn(it) }

tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = FULL
    }
}

codenarc {
    toolVersion = "1.1"
    configFile = file("config/codenarc/rules.groovy")
}

project.withGroovyBuilder {
    "codenarcTest" {
        setProperty("configFile", file("config/codenarc/rules-test.groovy"))
    }
}

group = "org.jenkins-ci.tools"
description = "Gradle plugin for building and packaging Jenkins plugins"

gradlePlugin {
    plugins {
        create("pluginMaven") {
            id = "org.jenkins-ci.jpi"
            implementationClass = "org.jenkinsci.gradle.plugins.jpi.JpiPlugin"
            displayName = "A plugin for building Jenkins plugins"
        }
    }
}

pluginBundle {
    website = "https://wiki.jenkins-ci.org/display/JENKINS/Gradle+JPI+Plugin"
    vcsUrl = "https://github.com/jenkinsci/gradle-jpi-plugin"
    description = "A plugin for building Jenkins plugins"
    tags = listOf("jenkins")
}

fun Project.stringProp(named: String): String? = findProperty(named) as String?
