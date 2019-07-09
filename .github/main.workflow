workflow "Daily" {
  resolves = ["Update Gradle Wrapper"]
  on = "schedule(0 0 * * *)"
}

action "Update Gradle Wrapper" {
  uses = "rahulsom/gradle-up@master"
  secrets = ["GITHUB_TOKEN"]
}
