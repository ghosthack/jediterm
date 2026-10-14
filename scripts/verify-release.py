#!/usr/bin/env python3
"""Check both local Maven publications without uploading anything."""
import argparse
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

NS = {"m": "http://maven.apache.org/POM/4.0.0"}
ROOT = Path(__file__).resolve().parents[1]


def verify(version, require_signatures=False):
    if not version or version.endswith("-SNAPSHOT") or "/" in version or "\\" in version:
        raise ValueError("A non-SNAPSHOT release version is required")
    repo = ROOT / ".gradleBuild/jediterm/release-repository/io/github/ghosthack"
    for module in ("core", "ui"):
        artifact = "jediterm-" + module
        directory = repo / artifact / version
        prefix = directory / (artifact + "-" + version)
        pom = Path(str(prefix) + ".pom")
        tree = ET.parse(pom).getroot()
        for field, expected in (("groupId", "io.github.ghosthack"), ("artifactId", artifact), ("version", version)):
            if tree.findtext("m:" + field, namespaces=NS) != expected:
                raise ValueError(f"Incorrect {field} in {pom}")
        for field in ("name", "description", "url", "developers/developer/name", "scm/connection", "scm/developerConnection", "scm/url"):
            path = "/".join("m:" + part for part in field.split("/"))
            if not tree.findtext(path, namespaces=NS):
                raise ValueError(f"Missing {field} in {pom}")
        if tree.findtext("m:licenses/m:license/m:name", namespaces=NS) != "Apache License, Version 2.0":
            raise ValueError(f"Incorrect license in {pom}")
        if tree.findtext("m:scm/m:tag", namespaces=NS) != "v" + version:
            raise ValueError(f"Incorrect SCM tag in {pom}")
        if module == "ui":
            deps = tree.findall("m:dependencies/m:dependency", NS)
            core = [d for d in deps if d.findtext("m:artifactId", namespaces=NS) == "jediterm-core"]
            if len(core) != 1 or core[0].findtext("m:groupId", namespaces=NS) != "io.github.ghosthack" or core[0].findtext("m:version", namespaces=NS) != version:
                raise ValueError("UI must depend on the matching public core release")
        files = [pom]
        for suffix in (".jar", "-sources.jar", "-javadoc.jar"):
            jar = Path(str(prefix) + suffix)
            with ZipFile(jar) as archive:
                names = archive.namelist()
                for notice in ("META-INF/LICENSE", "META-INF/LICENSE-APACHE-2.0.txt", "META-INF/LICENSE-LGPLv3.txt", "META-INF/NOTICE"):
                    if notice not in names:
                        raise ValueError(f"Missing {notice} in {jar}")
                if suffix == ".jar" and not any(n.endswith(".class") for n in names):
                    raise ValueError(f"No compiled classes in {jar}")
                if suffix == "-sources.jar" and not any(n.endswith((".java", ".kt")) for n in names):
                    raise ValueError(f"No source files in {jar}")
                if suffix == "-javadoc.jar" and not any(n.endswith("index.html") for n in names):
                    raise ValueError(f"No generated documentation in {jar}")
            files.append(jar)
        if require_signatures:
            for file in files:
                signature = Path(str(file) + ".asc")
                subprocess.run(["gpg", "--batch", "--verify", str(signature), str(file)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        print(f"Verified io.github.ghosthack:{artifact}:{version}" + (" including signatures" if require_signatures else ""))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("version")
    parser.add_argument("--require-signatures", action="store_true")
    args = parser.parse_args()
    verify(args.version, args.require_signatures)
