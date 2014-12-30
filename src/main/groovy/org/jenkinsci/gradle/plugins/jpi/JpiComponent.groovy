package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.artifacts.DependencySet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.Usage

class JpiComponent implements SoftwareComponentInternal {
    private final Usage jpiUsage = new JpiUsage()
    private final Set<PublishArtifact> jpiArtifacts
    private final Iterable<DependencySet> jpiDependencies

    final String name = 'jpi'
    final Set<Usage> usages = [jpiUsage]

    JpiComponent(PublishArtifact jpiArtifact, Iterable<DependencySet> jpiDependencies) {
        this.jpiArtifacts = [jpiArtifact]
        this.jpiDependencies = jpiDependencies
    }

    private class JpiUsage implements Usage {
        final String name = 'jpi'

        Set<PublishArtifact> getArtifacts() {
            jpiArtifacts
        }

        Set<ModuleDependency> getDependencies() {
            jpiDependencies*.withType(ModuleDependency).flatten()
        }
    }
}
