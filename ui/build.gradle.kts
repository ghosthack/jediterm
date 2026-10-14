// Modified by ghosthack. Fork changes are licensed under Apache-2.0; see NOTICE.
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
  kotlin("jvm") version "2.4.0"
  `java-library`
  `maven-publish`
}

sourceSets {
  main {
    java.srcDirs("src")
  }
  test {
    java.srcDirs("tests/src")
  }
}

java {
  withSourcesJar()
  withJavadocJar()
}

dependencies {
  implementation(project(":core"))
  implementation("org.slf4j:slf4j-api:2.0.9")
  implementation("org.jetbrains:annotations:24.0.1")
  testImplementation("junit:junit:4.13.2")
}

tasks {
  val jvmTarget = JvmTarget.JVM_11
  compileJava {
    sourceCompatibility = jvmTarget.target
    targetCompatibility = jvmTarget.target
  }
  compileKotlin {
    compilerOptions.jvmTarget = jvmTarget
  }
  test {
    testLogging {
      events(
        TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED,
        TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR
      )
      exceptionFormat = TestExceptionFormat.FULL
      showExceptions = true
      showCauses = true
      showStackTraces = true
      showStandardStreams = true
    }
  }
  jar {
    manifest {
      attributes(
        "Build-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(ZonedDateTime.now()),
        "Created-By" to "Gradle ${gradle.gradleVersion}",
        "Build-Jdk" to System.getProperty("java.runtime.version"),
        "Build-OS" to "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
      )
    }
  }
}

tasks.withType<JavaCompile> {
  options.encoding = Charsets.UTF_8.name()
}

val resultArchiveBaseName = "jediterm-ui"

tasks.withType<Jar> {
  archiveBaseName = resultArchiveBaseName
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifactId = resultArchiveBaseName
      pom {
        name = "JediTerm"
        description = "Pure Java Terminal Emulator"
        url = "https://github.com/ghosthack/jediterm"
        licenses {
          license {
            name = "Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution = "repo"
          }
        }
        scm {
          connection = "scm:git:https://github.com/ghosthack/jediterm.git"
          developerConnection = "scm:git:ssh://git@github.com/ghosthack/jediterm.git"
          url = "https://github.com/ghosthack/jediterm"
        }
      }
    }
  }
  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
      credentials {
        username = System.getenv("INTELLIJ_DEPENDENCIES_BOT")
        password = System.getenv("INTELLIJ_DEPENDENCIES_TOKEN")
      }
    }
    // Legacy GitHub Packages registry (see https://github.com/ghosthack/jediterm).
    maven {
      name = "GitHubPackages"
      url = uri("https://maven.pkg.github.com/ghosthack/jediterm")
      credentials {
        username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
        password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.token") as String?
      }
    }
  }
}

// Java API documentation also resolves the Kotlin classes compiled in this module.
tasks.javadoc {
  dependsOn(tasks.classes)
  classpath += sourceSets.main.get().output
  (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
