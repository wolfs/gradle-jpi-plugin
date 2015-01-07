/*
 * Copyright 2011 the original author or authors.
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

import org.gradle.util.GFileUtils

import java.util.jar.JarFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskAction

/**
 * Task that starts Jenkins in place with the current plugin.
 *
 * @author Kohsuke Kawaguchi
 */
class ServerTask extends DefaultTask {
    @TaskAction
    def start() {
        def c = project.configurations.getByName(JpiPlugin.WAR_DEPENDENCY_CONFIGURATION_NAME)
        def files = c.resolve()
        if (files.isEmpty()) {
            throw new GradleException('No jenkins.war dependency is specified')
        }
        File war = files.toArray()[0]

        generateHpl()
        copyPluginDependencies()

        def conv = project.extensions.getByType(JpiExtension)
        System.setProperty('JENKINS_HOME', conv.workDir.absolutePath)
        System.setProperty('stapler.trace', 'true')
        System.setProperty('stapler.jelly.noCache', 'true')
        System.setProperty('debug.YUI', 'true')

        def cl = new URLClassLoader([war.toURI().toURL()] as URL[])
        def mainClass = new JarFile(war).manifest.mainAttributes.getValue('Main-Class')
        cl.loadClass(mainClass).main()

        // make the thread hang
        Thread.currentThread().join()
    }

    void generateHpl() {
        def m = new JpiHplManifest(project)
        def conv = project.extensions.getByType(JpiExtension)

        def hpl = new File(conv.workDir, "plugins/${conv.shortName}.hpl")
        hpl.parentFile.mkdirs()
        hpl.withOutputStream { m.write(it) }
    }

    private copyPluginDependencies() {
        // create new configuration with plugin dependencies, ignoring the (jar) extension to get the HPI/JPI files
        Configuration plugins = project.configurations.create('plugins')
        project.configurations.getByName(JpiPlugin.PLUGINS_DEPENDENCY_CONFIGURATION_NAME).dependencies.each {
            project.dependencies.add(plugins.name, "${it.group}:${it.name}:${it.version}")
        }

        // copy the resolved HPI/JPI files to the plugins directory
        def workDir = project.extensions.getByType(JpiExtension).workDir
        plugins.resolvedConfiguration.resolvedArtifacts.findAll { it.extension in ['hpi', 'jpi'] }.each {
            GFileUtils.copyFile(it.file, new File(workDir, "plugins/${it.name}.${it.extension}"))
        }
    }

    public static final String TASK_NAME = 'server'
}
