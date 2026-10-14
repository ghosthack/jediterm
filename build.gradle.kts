// Modified by ghosthack. Fork changes are licensed under Apache-2.0; see NOTICE.
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.time.Duration

plugins {
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

// -PforkVersion=<version> publishes an immutable fork build (e.g. to GitHub Packages,
// which rejects overwriting an existing version) instead of the upstream VERSION-SNAPSHOT.
val projectVersion = (findProperty("forkVersion") as String?) ?: (
  rootProject.projectDir.resolve("VERSION").readText().trim() +
  if (System.getenv("INTELLIJ_DEPENDENCIES_BOT") == null) "-SNAPSHOT" else "")

allprojects {
  version = projectVersion
  group = (rootProject.findProperty("publicationGroup") as String?) ?: "org.jetbrains.jediterm"
  layout.buildDirectory = rootProject.projectDir.resolve(".gradleBuild/" + project.name)
}

subprojects {
  repositories {
    mavenCentral()
  }
}

// Keep legacy GitHub Packages coordinates unless a Central release selects our namespace.
nexusPublishing {
  packageGroup.set("io.github.ghosthack")
  repositoryDescription.set("JediTerm ghosthack fork ${project.version}")
  connectTimeout.set(Duration.ofMinutes(2))
  clientTimeout.set(Duration.ofMinutes(2))
  transitionCheckOptions {
    maxRetries.set(120)
    delayBetween.set(Duration.ofSeconds(10))
  }
  repositories {
    sonatype {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      username.set(providers.environmentVariable("MAVEN_CENTRAL_USERNAME"))
      password.set(providers.environmentVariable("MAVEN_CENTRAL_PASSWORD"))
    }
  }
}

tasks.named("initializeSonatypeStagingRepository") {
  doFirst {
    require(project.group == "io.github.ghosthack") { "Central releases must use -PpublicationGroup=io.github.ghosthack" }
    require(!project.version.toString().endsWith("-SNAPSHOT")) { "Use a non-SNAPSHOT -PforkVersion for a Central release" }
    require(providers.environmentVariable("GPG_PRIVATE_KEY").isPresent) { "A Central release requires GPG_PRIVATE_KEY" }
  }
}

subprojects {
  plugins.withId("maven-publish") {
    apply(plugin = "signing")
    val publications = extensions.getByType<PublishingExtension>().publications
    val signingKey = providers.environmentVariable("GPG_PRIVATE_KEY")
    if (rootProject.findProperty("requireSigning") == "true") {
      require(signingKey.isPresent) { "GPG_PRIVATE_KEY is required for signed release validation" }
    }
    if (signingKey.isPresent) {
      extensions.configure<SigningExtension> {
        useInMemoryPgpKeys(signingKey.get(), providers.environmentVariable("GPG_PASSPHRASE").orNull)
        sign(publications)
      }
    }
    publications.withType<MavenPublication>().configureEach {
      pom {
        developers {
          developer {
            id.set("ghosthack")
            name.set("ghosthack")
            url.set("https://github.com/ghosthack")
          }
        }
        scm {
          tag.set(if (project.version.toString().endsWith("-SNAPSHOT")) "HEAD" else "v${project.version}")
        }
      }
    }
    extensions.configure<PublishingExtension> {
      repositories {
        maven {
          name = "LocalRelease"
          url = uri(rootProject.layout.buildDirectory.dir("release-repository"))
        }
      }
    }
    tasks.withType<Jar>().configureEach {
      from(rootProject.projectDir) {
        include("LICENSE", "LICENSE-APACHE-2.0.txt", "LICENSE-LGPLv3.txt", "NOTICE")
        into("META-INF")
      }
    }
  }
}
