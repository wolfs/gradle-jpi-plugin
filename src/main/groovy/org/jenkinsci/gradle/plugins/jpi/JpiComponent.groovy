package org.jenkinsci.gradle.plugins.jpi

import org.gradle.api.DomainObjectSet
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.Usage

class JpiComponent implements SoftwareComponentInternal {
    private final Usage jpiUsage = new JpiUsage()
    private final Set<PublishArtifact> jpiArtifacts
    private final DomainObjectSet jpiDependencies

    final String name = 'jpi'
    final Set<Usage> usages = [jpiUsage]

    JpiComponent(PublishArtifact jpiArtifact, DomainObjectSet jpiDependencies) {
        this.jpiArtifacts = [jpiArtifact]
        this.jpiDependencies = jpiDependencies
    }

    private class JpiUsage implements Usage {
        final String name = 'jpi'

        Set<PublishArtifact> getArtifacts() {
            jpiArtifacts
        }

        Set<ModuleDependency> getDependencies() {
            // copy to new set, otherwise the result will contain duplicates
            new HashSet<ModuleDependency>(jpiDependencies.withType(ModuleDependency))
        }
    }
}
