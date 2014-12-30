package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

/**
 * Adds metadata to the JPI's POM.
 *
 * The POM is parsed by the <a href="https://github.com/jenkinsci/backend-update-center2">Jenkins Update Center
 * Generator</a> to extract the following information:
 * <ul>
 *     <li>The URL to the wiki page (<code>/project/url</code>)
 *     <li>The SCM Host (<code>/project/scm/connection</code>)
 *     <li>The project name (<code>/project/name</code>)
 *     <li>An excerpt (<code>/project/description</code>)
 * </ul>
 */
class JpiPomCustomizer {
    private final Project project
    private final JpiExtension jpiExtension

    JpiPomCustomizer(Project project) {
        this.project = project
        this.jpiExtension = project.extensions.findByType(JpiExtension)
    }

    void customizePom(Node pom) {
        pom.appendNode('name', jpiExtension.displayName)
        if (jpiExtension.url) {
            pom.appendNode('url', jpiExtension.url)
        }
        if (project.description) {
            pom.appendNode('description', project.description)
        }
        if (jpiExtension.gitHubUrl) {
            pom.append(makeScmNode())
        }
        if (!jpiExtension.developers.isEmpty()) {
            pom.appendNode('developers', jpiExtension.developers.collect { JpiDeveloper d -> makeDeveloperNode(d) })
        }
        if (repositories) {
            pom.appendNode('repositories', repositories.collect { makeRepositoryNode(it) })
        }
    }

    private Node makeScmNode() {
        Node scmNode = new Node(null, 'scm')
        scmNode.appendNode('url', jpiExtension.gitHubUrl)
        if (jpiExtension.gitHubUrl =~ /^https:\/\/github\.com/) {
            scmNode.appendNode('connection', jpiExtension.gitHubUrl.replaceFirst(~/https:/, 'scm:git:git:') + '.git')
        }
        scmNode
    }

    private List<MavenArtifactRepository> getRepositories() {
        project.repositories.withType(MavenArtifactRepository).findAll {
            it.name != 'MavenRepo' && it.name != 'MavenLocal'
        }
    }

    private static Node makeRepositoryNode(MavenArtifactRepository repository) {
        Node repositoryNode = new Node(null, 'repository')
        repositoryNode.appendNode('id', repository.name)
        repositoryNode.appendNode('url', repository.url)
        repositoryNode
    }

    private static Node makeDeveloperNode(JpiDeveloper developer) {
        Node developerNode = new Node(null, 'developer')
        JpiDeveloper.LEGAL_FIELDS.each { String key ->
            def value = developer[key]
            if (value) {
                developerNode.appendNode(key, value)
            }
        }
        developerNode
    }
}
