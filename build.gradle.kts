// -PforkVersion=<version> publishes an immutable fork build (e.g. to GitHub Packages,
// which rejects overwriting an existing version) instead of the upstream VERSION-SNAPSHOT.
val projectVersion = (findProperty("forkVersion") as String?) ?: (
  rootProject.projectDir.resolve("VERSION").readText().trim() +
  if (System.getenv("INTELLIJ_DEPENDENCIES_BOT") == null) "-SNAPSHOT" else "")

allprojects {
  version = projectVersion
  group = "org.jetbrains.jediterm"
  layout.buildDirectory = rootProject.projectDir.resolve(".gradleBuild/" + project.name)
}

subprojects {
  repositories {
    mavenCentral()
  }
}