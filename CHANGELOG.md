## 0.7.0 (2014-10-23)

  * updated Gradle to version 1.12
  * added support for `Plugin-Developers` manifest attribute
  * added support for `Support-Dynamic-Loading` manifest attribute
  * set `stapler.jelly.noCache` system property to `true` when running `server` task
  * removed `v` attribute from manifest
  * copy plugin dependencies from `jenkinsPlugins` configuration into working directory when running `server` task
    [JENKINS-25219](https://issues.jenkins-ci.org/browse/JENKINS-25219)
  * ignore non-existing source directories in `localizer` task
  * ignore non-existing library paths in `server` task
  * the value set for the `snapshotRepoUrl` option is no longer ignored
  * added `findbugs:annotations:1.0.0` to `jenkinsCore` configuration to avoid compiler warnings
    [JENKINS-14400](https://issues.jenkins-ci.org/browse/JENKINS-14400)
  * the Maven `connection` and `developerConnection` SCM information is no longer generated into the POM
  * removed `gitHubSCMConnection` and `gitHubSCMDevConnection` read-only options 
  * replaced usages of deprecated `groovy` configuration by `compile` configuration
  * added a missing setter for `shortName`
  * added `org.jenkins-ci.jpi` as alternative qualified plugin id for Gradle plugin portal inclusion
  * removed `WEB_APP_GROUP` constant from `org.jenkinsci.gradle.plugins.jpi.JpiPlugin`
  * changed visibility of `org.jenkinsci.gradle.plugins.jpi.JpiPlugin.configureConfigurations` to private
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
