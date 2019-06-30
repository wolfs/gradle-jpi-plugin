workflow "Update gradlew" {
  on = "schedule(0 0 * * *)"
  resolves = ["GitHub Action for Maven"]
}

action "GitHub Action for Maven" {
  uses = "rahulsom/gradle-up@master"
  secrets = ["GITHUB_TOKEN"]
}
