Releasing the Gradle JPI Plugin
===============================

These are the instructions to release the Gradle JPI Plugin.


Prerequisites
-------------

Ensure you have your signing credentials in `~/.gradle/gradle.properties`:

    signing.keyId=24875D73
    signing.secretKeyRingFile=/Users/me/.gnupg/secring.gpg

You do not need to store your private key password there, the build script will ask for it. See
[The Signing Plugin](https://www.gradle.org/docs/current/userguide/signing_plugin.html) for details.


Steps
-----

* Ensure you have the latest code: `git checkout master && git pull`
* Edit `gradle.properties` to strip `-SNAPSHOT` from version
* Update `CHANGELOG.md`, set the release date
* Update the version in `README.md`
* Ensure everything is checked in: `git commit -S -am "releasing 0.6.0"`
* Tag the source as it is: `git tag -s -a 0.6.0 -m "Gradle JPI Plugin 0.6.0"`
* Build the code: `gradlew clean check install`
* Test the plugin with Jenkins plugin projects using it (e.g. https://github.com/jenkinsci/job-dsl-plugin)
* Deploy: `gradlew -Pjenkins.username=<my-username> -Pjenkins.password=<my-password> publishPluginMavenPublicationToJenkinsCommunityRepository`
* Publish to Gradle plugin portal: `gradlew publishPlugins`
* Increment the version in `gradle.properties` and append `-SNAPSHOT`
* Update `CHANGELOG.md`, add the next version
* Commit the updated version number: `git commit -S -am "bumping version"`
* Push the two new commit and the tag back to GitHub: `git push && git push --tags`
* Close all resolved issues in JIRA: https://issues.jenkins-ci.org/secure/Dashboard.jspa?selectPageId=15444
* Send an email to jenkinsci-dev@googlegroups.com
