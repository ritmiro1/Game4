"""Checks every Minecraft/Fabric type the mod imports against real reference sources.

* Minecraft names are checked against the Yarn 1.21.4 mappings (class names only — signatures
  would need the obfuscated jar, which this environment cannot download).
* Fabric API names are checked against the source of fabric-api 0.119.4+1.21.4.

Both trees are downloaded from GitHub, which is reachable even though Maven is not.
"""
from __future__ import annotations

import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC = os.path.join(ROOT, "src", "main", "java")
YARN = os.environ.get("YARN_DIR", "/tmp/refs/yarn-1.21.4/mappings")
FABRIC = os.environ.get("FABRIC_DIR", "/tmp/refs/fabric-api-0.119.4-1.21.4")

problems: list[str] = []


def yarn_classes():
    names = set()
    for base, _, files in os.walk(YARN):
        for name in files:
            if not name.endswith(".mapping"):
                continue
            with open(os.path.join(base, name), encoding="utf-8") as handle:
                for line in handle:
                    if line.startswith("CLASS "):
                        parts = line.split()
                        if len(parts) >= 3:
                            names.add(parts[2].strip().replace("/", "."))
                        elif len(parts) == 2:
                            # A class whose named form equals its intermediary name
                            # (e.g. net/minecraft/server/MinecraftServer) has no third column.
                            names.add(parts[1].strip().replace("/", "."))
    return names


def fabric_classes():
    names = set()
    if not os.path.isdir(FABRIC):
        return names
    for base, _, files in os.walk(FABRIC):
        if "src" not in base.split(os.sep) or "test" in base.split(os.sep):
            continue
        for name in files:
            if not name.endswith(".java"):
                continue
            path = os.path.join(base, name)
            rel = os.path.relpath(path, FABRIC).replace(os.sep, "/")
            package = rel.split("/")[0]  # module directory, not the java package
            with open(path, encoding="utf-8", errors="replace") as handle:
                head = handle.read(4000)
            match = re.search(r"^package\s+([\w.]+);", head, re.MULTILINE)
            if not match:
                continue
            names.add(match.group(1) + "." + name[:-5])
    return names


def our_imports():
    found = set()
    for base, _, files in os.walk(SRC):
        for name in files:
            if not name.endswith(".java"):
                continue
            with open(os.path.join(base, name), encoding="utf-8") as handle:
                for line in handle:
                    match = re.match(r"import\s+(?:static\s+)?([\w.]+);", line.strip())
                    if match:
                        found.add(match.group(1))
    return found


def check():
    minecraft = yarn_classes()
    fabric = fabric_classes()
    imports = our_imports()
    missing_mc, missing_fabric = [], []
    for imp in sorted(imports):
        if imp.startswith("net.minecraft."):
            if imp not in minecraft:
                missing_mc.append(imp)
        elif imp.startswith("net.fabricmc."):
            if imp.startswith(("net.fabricmc.api.", "net.fabricmc.loader.",
                               "net.fabricmc.mappingio.")):
                continue
            if fabric and imp not in fabric:
                missing_fabric.append(imp)
    for imp in missing_mc:
        problems.append(f"MINECRAFT class not in Yarn 1.21.4: {imp}")
    for imp in missing_fabric:
        problems.append(f"FABRIC class not in fabric-api 0.119.4: {imp}")
    print(f"imports checked: {len(imports)} (minecraft: "
          f"{sum(1 for i in imports if i.startswith('net.minecraft.'))}, "
          f"fabric: {sum(1 for i in imports if i.startswith('net.fabricmc.'))})")
    print(f"yarn classes indexed: {len(minecraft)}, fabric classes indexed: {len(fabric)}")
    return missing_mc, missing_fabric


if __name__ == "__main__":
    check()
    if problems:
        print("\n".join(problems))
        print(f"\n{len(problems)} problem(s)")
        sys.exit(1)
    print("all imports resolve")
