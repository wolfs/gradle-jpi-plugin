package org.jenkinsci.gradle.plugins.jpi

import groovy.xml.MarkupBuilder
import groovy.xml.QName
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.local.model.DefaultProjectComponentIdentifier

class LicenseTask extends DefaultTask {
    @Internal
    Set<Configuration> configurations

    @Internal
    Set<Configuration> providedConfigurations

    @OutputDirectory
    File outputDirectory

    @InputFiles
    FileCollection getInputFiles() {
        project.files(configurations*.incoming.files, providedConfigurations*.incoming.files)
    }

    @TaskAction
    void generateLicenseInfo() {
        JpiExtension jpiExtension = project.extensions.getByType(JpiExtension)
        Set<ResolvedArtifact> pomArtifacts = collectPomArtifacts()

        new File(outputDirectory, 'licenses.xml').withWriter { Writer writer ->
            MarkupBuilder xmlMarkup = new MarkupBuilder(writer)

            xmlMarkup.'l:dependencies'(
                    'xmlns:l': 'licenses', version: project.version, artifactId: project.name, groupId: project.group,
            ) {
                'l:dependency'(
                        version: project.version, artifactId: project.name, groupId: project.group,
                        name: jpiExtension.displayName, url: jpiExtension.url,
                ) {
                    'l:description'(project.description)
                    jpiExtension.licenses.each { license ->
                        'l:license'(url: license.url, name: license.name)
                    }
                }

                pomArtifacts.each { ResolvedArtifact pomArtifact ->
                    Node pom = new XmlParser().parse(pomArtifact.file)

                    ModuleVersionIdentifier gav = pomArtifact.moduleVersion.id
                    String name = pom[QName.valueOf('name')].text()
                    String description = pom[QName.valueOf('description')].text()
                    String url = pom[QName.valueOf('url')].text()
                    NodeList licenses = pom[QName.valueOf('licenses')]

                    'l:dependency'(
                            version: gav.version, artifactId: gav.name, groupId: gav.group, name: name, url: url,
                    ) {
                        'l:description'(description)
                        licenses[QName.valueOf('license')].each { Node license ->
                            String licenseUrl = license[QName.valueOf('url')].text()
                            String licenseName = license[QName.valueOf('name')].text()
                            'l:license'(url: licenseUrl, name: licenseName)
                        }
                    }
                }
            }
        }
    }

    private Set<ResolvedArtifact> collectPomArtifacts() {
        Configuration pomConfiguration = project.configurations.create("poms-${System.nanoTime()}")

        collectDependencies().each { ResolvedArtifact artifact ->
            ModuleVersionIdentifier id = artifact.moduleVersion.id
            if (!(artifact.id.componentIdentifier instanceof DefaultProjectComponentIdentifier)) {
                project.dependencies.add(pomConfiguration.name, "${id.group}:${id.name}:${id.version}@pom")
            }
        }

        pomConfiguration.resolvedConfiguration.resolvedArtifacts
    }

    private Set<ResolvedArtifact> collectDependencies() {
        Set<ResolvedArtifact> artifacts = []

        configurations.each { artifacts += it.resolvedConfiguration.resolvedArtifacts }
        providedConfigurations.each { artifacts -= it.resolvedConfiguration.resolvedArtifacts }

        artifacts
    }
}
