## 0.6.1 (unreleased)

  * updated Gradle to version 1.12
  * added support for `Plugin-Developers` manifest attribute
  * added support for `Support-Dynamic-Loading` manifest attribute
  * replaced usages of deprecated `groovy` configuration by `compile` configuration
  * added `org.jenkins-ci.jpi` as alternative qualified plugin id for Gradle plugin portal inclusion
  * `jpiDeployUser` and `jpiDeployPassword` properties from `org.jenkinsci.gradle.plugins.jpi.JpiExtension` were not
    used and have been removed
  * `org.jenkinsci.gradle.plugins.jpi.JpiPluginConvention` was not used and has been removed

## 0.6.0 (2014-10-01)

  * do not exclude org.jenkins-ci.modules:instance-identity from jenkinsTest configuration
    [JENKINS-23603](https://issues.jenkins-ci.org/browse/JENKINS-23603)
  * set up the build to use http://repo.jenkins-ci.org/public/ similar to the Maven POMs
    [JENKINS-19942](https://issues.jenkins-ci.org/browse/JENKINS-19942)
  * register input files for the localizer task to avoid that the task is skipped when the inputs change
    [JENKINS-24298](https://issues.jenkins-ci.org/browse/JENKINS-24298)
  * added pluginFirstClassLoader attribute
    [JENKINS-24808](https://issues.jenkins-ci.org/browse/JENKINS-24808)
  * removed deprecation warnings by using newer API which has been introduced in Gradle 1.6 

## 0.5.2 (unreleased)

  * use ui-samples-plugin version 2.0 in jenkinsTests configuration when using Jenkins core 1.533 or later
    [JENKINS-21431](https://issues.jenkins-ci.org/browse/JENKINS-21431)
