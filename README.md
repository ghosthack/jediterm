JediTerm
========

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

[![Build Status](https://travis-ci.org/JetBrains/jediterm.png?branch=master)](https://travis-ci.org/JetBrains/jediterm)


The main purpose of the project is to provide a pure Java terminal widget that can be easily embedded 
into an IDE.
It supports terminal sessions both for SSH connections and local PTY on Mac OSX, Linux and Windows.


The library is used by JetBrains IDEs like PyCharm, IDEA, PhpStorm, WebStorm, AppCode, CLion, and Rider.

Since version 2.5 there is a standalone version of the JediTerm terminal, provided as Mac OSX distribution.


The name JediTerm origins from J(from `Java`) + edi(reversed `IDE`) + Term(obviously from `terminal`).
Also the word Jedi itself gives some confidence and hope in the Universe of thousands of different terminal implementations.


Run
-------

To run the standalone JediTerm terminal from sources just execute _jediterm.sh_ or _jediterm.bat_.
Or use the binary distribution from the [Releases](https://github.com/JetBrains/jediterm/releases/) page.



Build
-----

Gradle is used to build this project. The project consists of 4 sub-projects:
* **terminal**

    The core library that provides VT100 compatible terminal emulator and Java Swing based implementation of terminal panel UI.

* **pty**

    The jediterm-pty.jar library that, by using the [Pty4J](https://github.com/traff/pty4j) library, enables a terminal for local PTY terminal sessions.

* **JediTerm**

    The standalone version of the JediTerm terminal distributed as a .dmg for Mac OSX.


Features
--------
* Local terminal for Unix, Mac and Windows using [Pty4J](https://github.com/traff/pty4j)
* Xterm emulation - passes most of tests from vttest
* Xterm 256 colours
* Scrolling
* Copy/Paste
* Mouse support
* Terminal resizing from client or server side
* Terminal tabs



Authors
-------
Dmitry Trofimov <dmitry.trofimov@jetbrains.com>, Clément Poulain



Links
-----
 * Terminal protocol description: http://invisible-island.net/xterm/ctlseqs/ctlseqs.html
 * Terminal Character Set Terminology and Mechanics: http://www.columbia.edu/kermit/k95manual/iso2022.html
 * VT420 Programmer Reference Manual: http://manx.classiccmp.org/collections/mds-199909/cd3/term/vt420rm2.pdf
 * Pty4J library: https://github.com/traff/pty4j
 * JSch library: http://www.jcraft.com/jsch
 * UTF8 Demo: http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-demo.txt
 * Control sequences visualization: http://www.gnu.org/software/teseq/
 * Terminal protocol tests: http://invisible-island.net/vttest/



Open Source Origin and History
------
The initial version of the JediTerm was a reworked terminal emulator Gritty, which was in it's own turn a reworked JCTerm 
terminal implementation. Now there is nothing in the source code left from Gritty and JCTerm. Everything was 
rewritten from scratch. A lot of new features were added.

Character sets designation and mapping implementation is based on
respective classes from jVT220 (https://github.com/jawi/jVT220, Apache 2.0 licensed) by J.W. Janssen.


Standalone distribution relies heavily on customized Swing UI widgets taken from IntelliJ Community platform repository
(https://github.com/JetBrains/intellij-community) by JetBrains.


Licenses
-------
JediTerm is dual-licensed under both the LGPLv3 (found in the LICENSE-LGPLv3.txt file in the root directory) and Apache 2.0 License (found in the LICENSE-APACHE-2.0.txt file in the root directory). 
You may select, at your option, one of the above-listed licenses.


This fork
-------
This is `github.com/ghosthack/jediterm`, branch `ghosthack/base-3.74`: a private fork consumed
by a downstream project as `org.jetbrains.jediterm:jediterm-core`, version `3.74-ghosthack.N`,
published to this fork's own GitHub Packages registry (not Maven Central, not JetBrains'
`intellij-dependencies`).

### Publishing a new version

1. Make the change under `core/`, add/update a test under `core/tests/`, and commit.
2. Push the commit to `origin/ghosthack/base-3.74` — GitHub Packages requires the artifact's
   source to be reachable, and it's just good hygiene to not publish unpushed work.
3. Publish with a bumped `forkVersion` (`3.74-ghosthack.N` — check the current max with the `gh`
   command below and increment):
   ```
   GITHUB_ACTOR=<user> GITHUB_TOKEN=<token> ./gradlew :core:publishMavenJavaPublicationToGitHubPackagesRepository -PforkVersion=3.74-ghosthack.N
   ```
   Credentials: GitHub Packages needs a PAT with `write:packages` for user `ghosthack`. On this
   machine one already lives in `~/.m2/settings.xml` under server id `github-jediterm-fork`
   (it's there for the *consumer* side — Maven reads it when a downstream project resolves the
   dependency — but the same value works here). Ordinary credential hygiene applies: don't
   print the file's contents or copy the value into a new file on disk; read it straight into
   the two env vars for the one command that needs them and let it go out of scope after.
4. **Use `:core:publishMavenJavaPublicationToGitHubPackagesRepository`, not the bare `:core:publish`
   task.** `publish` also targets the JetBrains `intellij-dependencies` repository declared in
   `core/build.gradle.kts` (upstream's repo, for `INTELLIJ_DEPENDENCIES_BOT`/`_TOKEN` — creds we
   don't have and don't want), and that half of the task graph fails even though the GitHub
   Packages half already succeeded. Targeting the specific task avoids the false failure.
5. GitHub Packages will not let you overwrite an existing version — republishing the same
   `forkVersion` fails with `409 Conflict`. That's often a sign the previous attempt actually
   succeeded (e.g. if it failed later for an unrelated reason, like step 4's false failure).
   Verify what's actually published before assuming a publish didn't happen:
   ```
   gh api /users/ghosthack/packages/maven/org.jetbrains.jediterm.jediterm-core/versions --jq '.[].name'
   ```
6. Bump the version in the downstream project's `pom.xml` (`org.jetbrains.jediterm:jediterm-core`)
   and commit there too.
