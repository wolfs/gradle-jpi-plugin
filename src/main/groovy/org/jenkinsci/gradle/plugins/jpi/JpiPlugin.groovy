/*
 * Copyright 2009-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.WarPlugin
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.War
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.util.GradleVersion

import static org.gradle.api.logging.LogLevel.INFO
import static org.gradle.api.plugins.JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
import static org.gradle.api.plugins.WarPlugin.PROVIDED_COMPILE_CONFIGURATION_NAME
import static org.gradle.api.plugins.WarPlugin.PROVIDED_RUNTIME_CONFIGURATION_NAME
import static org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import static org.gradle.api.tasks.SourceSet.TEST_SOURCE_SET_NAME
import static org.jenkinsci.gradle.plugins.jpi.JpiManifest.attributesToMap

/**
 * Loads HPI related tasks into the current project.
 *
 * @author Hans Dockter
 * @author Kohsuke Kawaguchi
 * @author Andrew Bayer
 */
class JpiPlugin implements Plugin<Project> {
    /**
     * Represents the dependency to the Jenkins core.
     */
    public static final String CORE_DEPENDENCY_CONFIGURATION_NAME = 'jenkinsCore'

    /**
     * Represents the dependency to the Jenkins war. Test scope.
     */
    public static final String WAR_DEPENDENCY_CONFIGURATION_NAME = 'jenkinsWar'

    /**
     * Represents the dependencies on other Jenkins plugins.
     */
    public static final String PLUGINS_DEPENDENCY_CONFIGURATION_NAME = 'jenkinsPlugins'

    /**
     * Represents the dependencies on other Jenkins plugins.
     *
     * Using a separate configuration until we see GRADLE-1749.
     */
    public static final String OPTIONAL_PLUGINS_DEPENDENCY_CONFIGURATION_NAME = 'optionalJenkinsPlugins'

    /**
     * Represents the Jenkins plugin test dependencies.
     */
    public static final String JENKINS_TEST_DEPENDENCY_CONFIGURATION_NAME = 'jenkinsTest'

    /**
     * Represents the extra dependencies on other Jenkins plugins for the server task.
     */
    public static final String JENKINS_SERVER_DEPENDENCY_CONFIGURATION_NAME = 'jenkinsServer'

    public static final String JPI_TASK_NAME = 'jpi'
    public static final String SOURCES_JAR_TASK_NAME = 'sourcesJar'
    public static final String JAVADOC_JAR_TASK_NAME = 'javadocJar'
    public static final String LICENSE_TASK_NAME = 'generateLicenseInfo'

    void apply(final Project gradleProject) {
        gradleProject.plugins.apply(JavaPlugin)
        gradleProject.plugins.apply(WarPlugin)
        gradleProject.plugins.apply(GroovyPlugin)

        def ext = gradleProject.extensions.create('jenkinsPlugin', JpiExtension, gradleProject)

        gradleProject.tasks.register(ServerTask.TASK_NAME, ServerTask) {
            it.description = 'Run Jenkins in place with the plugin being developed'
            it.group = BasePlugin.BUILD_GROUP // TODO
            it.dependsOn(ext.mainSourceTree().runtimeClasspath)
        }

        // set build directory for Jenkins test harness, JENKINS-26331
        gradleProject.tasks.withType(Test).named('test').configure {
            it.systemProperty('buildDirectory', gradleProject.buildDir.absolutePath)
        }

        configureLocalizer(gradleProject)
        configureLicenseInfo(gradleProject)
        configureInjectedTest(gradleProject)

        gradleProject.tasks.register(SOURCES_JAR_TASK_NAME, Jar) {
            it.dependsOn('classes')
            it.classifier = 'sources'
            it.from gradleProject.sourceSets.main.allSource
        }
        gradleProject.tasks.register(JAVADOC_JAR_TASK_NAME, Jar) {
            it.dependsOn('javadoc')
            it.classifier = 'javadoc'
            it.from gradleProject.javadoc.destinationDir
        }

        if (!gradleProject.logger.isEnabled(INFO)) {
            gradleProject.tasks.withType(JavaCompile).configureEach {
                options.compilerArgs << '-Asezpoz.quiet=true'
            }
            gradleProject.tasks.withType(GroovyCompile).configureEach {
                options.compilerArgs << '-Asezpoz.quiet=true'
            }
        }

        gradleProject.tasks.withType(GroovyCompile).configureEach {
            groovyOptions.javaAnnotationProcessing = true
        }

        configureRepositories(gradleProject)
        configureConfigurations(gradleProject)
        configureManifest(gradleProject)
        configureJpi(gradleProject)
        configureTestDependencies(gradleProject)
        configurePublishing(gradleProject)
        configureTestHpl(gradleProject)
    }

    private static Properties loadDotJenkinsOrg() {
        Properties props = new Properties()
        def dot = new File(new File(System.getProperty('user.home')), '.jenkins-ci.org')
        if (!dot.exists()) {
            throw new GradleException(
                    "Trying to deploy to Jenkins community repository but there's no credential file ${dot}." +
                            ' See https://wiki.jenkins-ci.org/display/JENKINS/Dot+Jenkins+Ci+Dot+Org'
            )
        }
        dot.withInputStream { i -> props.load(i) }
        props
    }

    private static configureManifest(Project project) {
        JavaPluginConvention javaPluginConvention = project.convention.getPlugin(JavaPluginConvention)
        TaskProvider<War> warProvider = project.tasks.named(WarPlugin.WAR_TASK_NAME) as TaskProvider<War>
        TaskProvider<Jar> jarProvider = project.tasks.named(JavaPlugin.JAR_TASK_NAME) as TaskProvider<Jar>

        def configureManifest = project.tasks.register('configureManifest') {
            it.doLast {
                Map<String, ?> attributes = attributesToMap(new JpiManifest(project).mainAttributes)
                warProvider.configure {
                    it.manifest.attributes(attributes)
                    it.inputs.property('manifest', attributes)
                }
                jarProvider.configure {
                    it.manifest.attributes(attributes)
                    it.inputs.property('manifest', attributes)
                }
            }

            it.dependsOn(javaPluginConvention.sourceSets.getByName(MAIN_SOURCE_SET_NAME).output)
        }

        warProvider.configure { it.dependsOn(configureManifest) }
        jarProvider.configure { it.dependsOn(configureManifest) }
    }

    private static configureJpi(Project project) {
        JpiExtension jpiExtension = project.extensions.getByType(JpiExtension)

        def jar = project.tasks.named(JavaPlugin.JAR_TASK_NAME)
        def war = project.tasks.named(WarPlugin.WAR_TASK_NAME)
        project.afterEvaluate {
            war.configure {
                it.description = 'Generates the JPI package'
                def fileName = "${jpiExtension.shortName}.${jpiExtension.fileExtension}"
                def extension = jpiExtension.fileExtension
                if (GradleVersion.current() >= GradleVersion.version('5.1')) {
                    it.archiveFileName.set(fileName)
                    it.archiveExtension.set(extension)
                } else {
                    it.archiveName = fileName
                    it.extension = extension
                }
                it.classpath -= project.sourceSets.main.output
                it.classpath(jar)
            }
        }

        project.tasks.register(JPI_TASK_NAME) {
            it.dependsOn(war)
            it.description = 'Generates the JPI package'
            it.group = BasePlugin.BUILD_GROUP
        }
    }

    private static configureTestDependencies(Project project) {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        Configuration plugins = project.configurations.create('pluginResources')
        plugins.visible = false

        project.afterEvaluate {
            [
                    PLUGINS_DEPENDENCY_CONFIGURATION_NAME,
                    OPTIONAL_PLUGINS_DEPENDENCY_CONFIGURATION_NAME,
                    JENKINS_SERVER_DEPENDENCY_CONFIGURATION_NAME,
                    JENKINS_TEST_DEPENDENCY_CONFIGURATION_NAME,
            ].each {
                project.configurations.getByName(it).dependencies.each {
                    project.dependencies.add(plugins.name, "${it.group}:${it.name}:${it.version}")
                }
            }
        }

        def testDependenciesTask = project.tasks.register(TestDependenciesTask.TASK_NAME, TestDependenciesTask) {
            it.configuration = plugins
        }

        project.tasks.named(javaConvention.sourceSets.test.processResourcesTaskName).configure {
            it.dependsOn(testDependenciesTask)
        }
    }

    private static configureLocalizer(Project project) {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        JpiExtension jpiExtension = project.extensions.getByType(JpiExtension)

        def localizer = project.tasks.register(LocalizerTask.TASK_NAME, LocalizerTask) {
            it.description = 'Generates the Java source for the localizer.'
            it.group = BasePlugin.BUILD_GROUP
            it.sourceDirs = javaConvention.sourceSets.main.resources.srcDirs
            it.conventionMapping.map('destinationDir') {
                jpiExtension.localizerOutputDir
            }
        }
        javaConvention.sourceSets.main.java.srcDir { localizer.get().destinationDir }
        project.tasks.named(javaConvention.sourceSets.main.compileJavaTaskName).configure {
            it.dependsOn(localizer)
        }
    }

    private static configureLicenseInfo(Project project) {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)

        def licenseTask = project.tasks.register(LICENSE_TASK_NAME, LicenseTask) {
            it.description = 'Generates license information.'
            it.group = BasePlugin.BUILD_GROUP
            it.outputDirectory = new File(project.buildDir, 'licenses')
            it.configurations = [
                    project.configurations[javaConvention.sourceSets.main.compileConfigurationName],
                    project.configurations[javaConvention.sourceSets.main.runtimeConfigurationName],
            ]
            it.providedConfigurations = [
                    project.configurations[PROVIDED_COMPILE_CONFIGURATION_NAME],
                    project.configurations[PROVIDED_RUNTIME_CONFIGURATION_NAME],
            ]
        }

        project.tasks.named(WarPlugin.WAR_TASK_NAME).configure {
            it.webInf.from(licenseTask.get().outputDirectory)
            it.dependsOn(licenseTask)
        }
    }

    private static configureInjectedTest(Project project) {
        JpiExtension jpiExtension = project.extensions.getByType(JpiExtension)
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        SourceSet testSourceSet = javaConvention.sourceSets.getByName(TEST_SOURCE_SET_NAME)

        File root = new File(project.buildDir, 'inject-tests')
        testSourceSet.java.srcDirs += root

        def testInsertionTask = project.tasks.register(TestInsertionTask.TASK_NAME, TestInsertionTask) {
            it.onlyIf { !jpiExtension.disabledTestInjection }
        }

        project.tasks.named('compileTestJava').configure { it.dependsOn(testInsertionTask) }

        project.afterEvaluate {
            testInsertionTask.configure {
                it.testSuite = new File(root, "${jpiExtension.injectedTestName}.java")
            }
        }
    }

    private static configureRepositories(Project project) {
        project.afterEvaluate {
            if (project.extensions.getByType(JpiExtension).configureRepositories) {
                project.repositories {
                    mavenCentral()
                    mavenLocal()
                    maven {
                        name 'jenkins'
                        url('https://repo.jenkins-ci.org/public/')
                    }
                }
            }
        }
    }

    private static configureConfigurations(Project project) {
        Configuration core = project.configurations.create(CORE_DEPENDENCY_CONFIGURATION_NAME)
        core.visible = false
        core.description = 'Jenkins core that your plugin is built against'

        Configuration plugins = project.configurations.create(PLUGINS_DEPENDENCY_CONFIGURATION_NAME)
        plugins.visible = false
        plugins.description = 'Jenkins plugins which your plugin is built against'

        Configuration optionalPlugins = project.configurations.create(OPTIONAL_PLUGINS_DEPENDENCY_CONFIGURATION_NAME)
        optionalPlugins.visible = false
        optionalPlugins.description = 'Optional Jenkins plugins dependencies which your plugin is built against'

        Configuration serverPlugins = project.configurations.create(JENKINS_SERVER_DEPENDENCY_CONFIGURATION_NAME)
        serverPlugins.visible = false
        serverPlugins.description = 'Jenkins plugins which will be installed by the server task'

        Configuration test = project.configurations.create(JENKINS_TEST_DEPENDENCY_CONFIGURATION_NAME)
                .exclude(group: 'org.jenkins-ci.modules', module: 'ssh-cli-auth')
                .exclude(group: 'org.jenkins-ci.modules', module: 'sshd')
        test.visible = false
        test.description = 'Jenkins plugin test dependencies.'

        project.configurations.getByName(PROVIDED_COMPILE_CONFIGURATION_NAME).extendsFrom(core)
        project.configurations.getByName(PROVIDED_COMPILE_CONFIGURATION_NAME).extendsFrom(plugins)
        project.configurations.getByName(PROVIDED_COMPILE_CONFIGURATION_NAME).extendsFrom(optionalPlugins)
        project.configurations.getByName(TEST_IMPLEMENTATION_CONFIGURATION_NAME).extendsFrom(test)

        Configuration warDependencies = project.configurations.create(WAR_DEPENDENCY_CONFIGURATION_NAME)
        warDependencies.visible = false
        warDependencies.description = 'Jenkins war that corresponds to the Jenkins core'

        resolvePluginDependencies(project)
    }

    private static configurePublishing(Project project) {
        JpiExtension jpiExtension = project.extensions.getByType(JpiExtension)

        // delay configuration until all settings are available (groupId, shortName, ...)
        project.afterEvaluate {
            if (jpiExtension.configurePublishing) {
                Task jar = project.tasks.getByName(JavaPlugin.JAR_TASK_NAME)
                Task sourcesJar = project.tasks.getByName(SOURCES_JAR_TASK_NAME)
                Task javadocJar = project.tasks.getByName(JAVADOC_JAR_TASK_NAME)

                project.plugins.apply(MavenPublishPlugin)
                PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)
                publishingExtension.publications {
                    mavenJpi(MavenPublication) {
                        artifactId jpiExtension.shortName

                        from(project.components.web)

                        artifact jar
                        artifact sourcesJar
                        artifact javadocJar

                        new JpiPomCustomizer(project).customizePom(pom)
                    }
                }
                publishingExtension.repositories {
                    maven {
                        name 'jenkins'
                        if (project.version.toString().endsWith('-SNAPSHOT')) {
                            url jpiExtension.snapshotRepoUrl
                        } else {
                            url jpiExtension.repoUrl
                        }
                    }
                }
            }
        }

        // load credentials only when publishing
        project.gradle.taskGraph.whenReady { TaskExecutionGraph taskGraph ->
            if (jpiExtension.configurePublishing && taskGraph.hasTask(project.tasks.publish)) {
                def credentials = loadDotJenkinsOrg()
                PublishingExtension publishingExtension = project.extensions.getByType(PublishingExtension)
                publishingExtension.repositories.getByName('jenkins').credentials {
                    username credentials.userName
                    password credentials.password
                }
            }
        }
    }

    private static configureTestHpl(Project project) {
        JavaPluginConvention javaConvention = project.convention.getPlugin(JavaPluginConvention)
        SourceSet testSourceSet = javaConvention.sourceSets.getByName(TEST_SOURCE_SET_NAME)

        // generate test hpl manifest for the current plugin, to be used during unit test
        def outputDir = project.layout.buildDirectory.dir('generated-resources/test')
        project.tasks.register('generate-test-hpl', GenerateTestHpl) {
            it.hplDir.set(outputDir)
        }
        testSourceSet.output.dir(outputDir)
    }

    private static void resolvePluginDependencies(Project project) {
        resolvePluginDependencies(
                project,
                PLUGINS_DEPENDENCY_CONFIGURATION_NAME,
                PROVIDED_COMPILE_CONFIGURATION_NAME
        )
        resolvePluginDependencies(
                project,
                OPTIONAL_PLUGINS_DEPENDENCY_CONFIGURATION_NAME,
                PROVIDED_COMPILE_CONFIGURATION_NAME
        )
        resolvePluginDependencies(
                project,
                JENKINS_TEST_DEPENDENCY_CONFIGURATION_NAME,
                TEST_IMPLEMENTATION_CONFIGURATION_NAME
        )
    }

    private static void resolvePluginDependencies(Project project, String from, String to) {
        ConfigurationContainer configurations = project.configurations
        Configuration fromConfiguration = configurations.getByName(from)
        Configuration toConfiguration = configurations.getByName(to)

        toConfiguration.withDependencies { deps ->
            fromConfiguration.resolvedConfiguration.resolvedArtifacts
                    .findAll { it.type == 'hpi' || it.type == 'jpi' }
                    .collect { toDependency(project, it, from) }
                    .each { deps.add(it) }
        }
    }

    private static Dependency toDependency(Project project, ResolvedArtifact it, String from) {
        project.dependencies.create("${it.moduleVersion.id}@jar") { Dependency d ->
            d.because "JpiPlugin added jar for compilation support (plugin present on $from)"
        }
    }
}
