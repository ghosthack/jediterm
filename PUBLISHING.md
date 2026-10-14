# Publishing the JediTerm fork

This uses the same Gradle publishing strategy as Mockito: Gradle's built-in
`maven-publish` and `signing`, with `io.github.gradle-nexus.publish-plugin:2.0.0`
handling Central staging and release. Central uses its OSSRH compatibility API,
not the retired oss.sonatype.org service.

## Local validation

Run with JDK 17 or newer. Gradle compiles both libraries for Java 11.

```sh
./gradlew :core:test :ui:test \
  :core:publishMavenJavaPublicationToLocalReleaseRepository \
  :ui:publishMavenJavaPublicationToLocalReleaseRepository \
  -PpublicationGroup=io.github.ghosthack -PforkVersion=3.76-ghosthack.2 --no-daemon
python3 scripts/verify-release.py 3.76-ghosthack.2
```

The Maven repository is under `.gradleBuild/jediterm/release-repository`.
This command only writes the local repository. It does not upload anything.
With `GPG_PRIVATE_KEY` and `GPG_PASSPHRASE` provided through the environment,
add `-PrequireSigning=true` and verify with `--require-signatures` to exercise
the same in-memory signing used in CI. Never print or commit those values.

## Release

1. Validate both modules, generated POMs, documentation/source JARs, notices,
   and signatures locally before pushing changes.
2. Push the release commit to `ghosthack/base-3.76`; wait for CI to pass.
3. Publish a non-prerelease GitHub Release with tag `v3.76-ghosthack.2`,
   targeting that exact commit. The release workflow first validates unsigned
   artifacts on Linux, macOS, and Windows. A separate job rebuilds from the
   same tag, signs and verifies both modules, then uploads and releases them.
4. Confirm both module versions resolve anonymously from Maven Central.

The workflow uses these repository secrets, installed by the existing
`gpg-backup-audit/set-maven-central-secrets.sh` helper:
`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `GPG_PRIVATE_KEY`,
and `GPG_PASSPHRASE`. It publishes only the `io.github.ghosthack` namespace.

Core and UI must always use the same release version. Keep the runtime UI
POM dependency on `io.github.ghosthack:jediterm-core` at that exact version.
Do not republish an existing Central version; versions are immutable.

## Legacy GitHub Packages

Without `-PpublicationGroup`, coordinates remain `org.jetbrains.jediterm`.
Use the explicit GitHub Packages tasks to publish legacy builds; do not
invoke the bare `publish` task, which also targets upstream's repository.
New Central consumers require no GitHub token or extra repository entry.
