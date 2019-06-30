workflow "Daily" {
  on = "schedule(0 0 * * *)"
  resolves = ["Update Gradle Wrapper"]
}

action "Update Gradle Wrapper" {
  uses = "rahulsom/gradle-up@master"
  secrets = ["GITHUB_TOKEN"]
}
