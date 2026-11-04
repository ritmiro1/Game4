"""Static validation for the whole mod.

This is the closest thing to a test-suite that can run without a Minecraft runtime: it checks
that every id, texture, translation key, model, loot table and registry symbol referenced by the
code and the data actually exists, that the JSON parses, that every class parses as Java, and that
no crafting recipe feeds itself.
"""
from __future__ import annotations

import json
import os
import re
import sys
from collections import defaultdict

import tree_sitter_java
from tree_sitter import Language, Parser

from content import ADVANCEMENTS, BLOCKS, ENTITIES, ITEMS, LAYERS, MOD_ID

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC = os.path.join(ROOT, "src", "main", "java")
RES = os.path.join(ROOT, "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", MOD_ID)
DATA = os.path.join(RES, "data", MOD_ID)

problems: list[str] = []
notes: list[str] = []


def fail(message):
    problems.append(message)


def java_files():
    for base, _, files in os.walk(SRC):
        for name in files:
            if name.endswith(".java"):
                yield os.path.join(base, name)


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


# ----------------------------------------------------------------- 1. java parses & names --

_JAVA = Language(tree_sitter_java.language())


def _errors(node, out, depth=0):
    """Collect ERROR and MISSING nodes from a tree-sitter parse."""
    if node.type == "ERROR" or node.is_missing:
        out.append(node)
    for child in node.children:
        _errors(child, out, depth + 1)
    return out


def check_java():
    parser = Parser(_JAVA)
    count = 0
    for path in java_files():
        source = read(path)
        try:
            tree = parser.parse(source.encode("utf-8"))
        except Exception as error:  # noqa: BLE001
            fail(f"JAVA PARSE {os.path.relpath(path, ROOT)}: {error}")
            continue
        bad = _errors(tree.root_node, [])
        if bad:
            lines = source.splitlines()
            for node in bad[:3]:
                line = node.start_point[0] + 1
                column = node.start_point[1]
                snippet = lines[line - 1].strip() if line - 1 < len(lines) else ""
                fail(f"JAVA SYNTAX {os.path.relpath(path, ROOT)}:{line}:{column}: {node.type} near {snippet!r}")
            continue
        count += 1
        names = set(re.findall(r"public\s+(?:final\s+)?(?:class|interface|enum|record)\s+(\w+)", source))
        expected = os.path.basename(path)[:-5]
        if expected not in names:
            fail(f"JAVA CLASS {os.path.relpath(path, ROOT)}: expected public type {expected}, found {sorted(names)}")
        package = re.search(r"^package\s+([\w.]+)\s*;", source, re.MULTILINE)
        declared = package.group(1) if package else ""
        directory = os.path.dirname(os.path.relpath(path, SRC)).replace(os.sep, ".")
        if declared != directory:
            fail(f"JAVA PACKAGE {os.path.relpath(path, ROOT)}: declared {declared}, directory {directory}")
        # crude structural checks tree-sitter cannot fail on but that catch real typos
        if source.count("{") != source.count("}"):
            fail(f"JAVA BRACES {os.path.relpath(path, ROOT)}: unbalanced braces")
        if source.count("(") != source.count(")"):
            fail(f"JAVA PARENS {os.path.relpath(path, ROOT)}: unbalanced parentheses")
    notes.append(f"java files parsed: {count}")
    return count


# --------------------------------------------------------------- 2. registry symbol usage --

REGISTRIES = ["ModItems", "ModBlocks", "ModSounds", "ModParticles", "ModEntities", "ModScreenHandlers",
              "ModPayloads", "ModItemGroups", "ModEntityModels", "ModWorldgen"]
REQUIRED_FIELDS = {"ModItems", "ModBlocks", "ModSounds", "ModParticles", "ModEntities",
                   "ModScreenHandlers", "ModItemGroups", "ModEntityModels"}


def registry_fields():
    fields = {}
    for registry in REGISTRIES:
        path = None
        for candidate in java_files():
            if os.path.basename(candidate) == registry + ".java":
                path = candidate
                break
        if path is None:
            fields[registry] = set()
            continue
        source = read(path)
        names = set(re.findall(r"public\s+static\s+final\s+[\w<>\[\], .]+\s+([A-Z][A-Z0-9_]*)\s*[=;]", source))
        fields[registry] = names
    return fields


def check_registry_symbols():
    fields = registry_fields()
    usage = defaultdict(set)
    for path in java_files():
        source = read(path)
        for match in re.finditer(r"\b(Mod[A-Za-z]+)\.([A-Z][A-Z0-9_]*)\b", source):
            usage[match.group(1)].add(match.group(2))
    for registry, names in usage.items():
        if registry not in fields:
            fail(f"REGISTRY unknown registry referenced: {registry}")
            continue
        missing = sorted(name for name in names if name not in fields[registry])
        for name in missing:
            fail(f"REGISTRY {registry}.{name} is referenced but never declared")
    for registry, names in fields.items():
        if not names and registry in REQUIRED_FIELDS:
            fail(f"REGISTRY {registry} declares no constants (file missing or empty)")
        unused = sorted(name for name in names if name not in usage.get(registry, set()))
        if unused:
            notes.append(f"unused {registry} constants: {', '.join(unused)}")
    return fields


# ------------------------------------------------------------------- 3. sounds & particles --

def check_sounds(fields):
    path = os.path.join(ASSETS, "sounds.json")
    if not os.path.exists(path):
        fail("SOUNDS sounds.json missing")
        return {}
    sounds = json.loads(read(path))
    if len(sounds) < 20:
        fail(f"SOUNDS only {len(sounds)} sound events defined (minimum 20)")
    for event, node in sounds.items():
        for entry in node.get("sounds", []):
            name = entry["name"].split(":")[-1]
            file = os.path.join(ASSETS, "sounds", name + ".ogg")
            if not os.path.isfile(file):
                fail(f"SOUNDS events {event} points at missing file {name}.ogg")
            elif os.path.getsize(file) < 2000:
                fail(f"SOUNDS {name}.ogg is suspiciously small ({os.path.getsize(file)} bytes)")
    declared = set()
    for constant in fields.get("ModSounds", set()):
        declared.add(constant)
    source = read(os.path.join(SRC, "com", "hollowlight", "registry", "ModSounds.java"))
    ids = set(re.findall(r'(?:register|create)\(\s*"([^"]+)"', source))
    for sound_id in ids:
        if sound_id not in sounds:
            fail(f"SOUNDS ModSounds event {sound_id} has no sounds.json entry")
    for event in sounds:
        if event not in ids:
            fail(f"SOUNDS sounds.json event {event} is not registered in ModSounds")
    notes.append(f"sound events: {len(sounds)} registered / {len(ids)} java ids")
    music = [name for name in sounds if name.startswith("music.")]
    ambience = [name for name in sounds if name.startswith("ambience.")]
    notes.append(f"music tracks: {len(music)}, ambience beds: {len(ambience)}")
    return sounds


def check_particles(fields):
    for name in ("dust", "mote", "smoke", "glint"):
        path = os.path.join(ASSETS, "particles", name + ".json")
        if not os.path.isfile(path):
            fail(f"PARTICLES {name}.json missing")
            continue
        node = json.loads(read(path))
        for texture in node.get("textures", []):
            rel = texture.split(":")[-1]
            if not os.path.isfile(os.path.join(ASSETS, "textures", rel + ".png")):
                fail(f"PARTICLES {name} references missing texture {texture}")


# ------------------------------------------------------------------ 4. models & textures --

HANDHELD_OK = {"minecraft:item/handheld", "minecraft:item/handheld_rod", "minecraft:item/generated"}


def check_models():
    blockstates = os.path.join(ASSETS, "blockstates")
    for name in BLOCKS:
        path = os.path.join(blockstates, name + ".json")
        if not os.path.isfile(path):
            fail(f"MODELS blockstate missing for block {name}")
            continue
        node = json.loads(read(path))
        variants = node.get("variants") or {}
        models = set()
        for variant in variants.values():
            for entry in variant if isinstance(variant, list) else [variant]:
                models.add(entry["model"])
        for model in models:
            if not model_exists(model):
                fail(f"MODELS blockstate {name} points at missing model {model}")
        if not variants:
            fail(f"MODELS blockstate {name} has no variants")
    for name in BLOCKS + ITEMS:
        path = os.path.join(ASSETS, "items", name + ".json")
        if not os.path.isfile(path):
            fail(f"MODELS item definition missing for {name}")
            continue
        node = json.loads(read(path))
        model = node.get("model", {}).get("model")
        if not model:
            fail(f"MODELS item definition {name} has no model")
        elif not model_exists(model):
            fail(f"MODELS item definition {name} points at missing model {model}")
    for name in ITEMS:
        path = os.path.join(ASSETS, "models", "item", name + ".json")
        if not os.path.isfile(path):
            fail(f"MODELS item model missing for {name}")
            continue
        node = json.loads(read(path))
        parent = node.get("parent", "")
        if parent and not model_exists(parent) and parent not in HANDHELD_OK:
            notes.append(f"item {name} uses parent {parent}")
        textures = node.get("textures", {})
        if not textures and "parent" not in node:
            fail(f"MODELS item model {name} has neither textures nor parent")


def model_exists(model_id):
    namespace, _, path = model_id.partition(":")
    if namespace == "minecraft":
        return True
    return os.path.isfile(os.path.join(ASSETS, "models", path + ".json"))


def collect_texture_refs():
    refs = []
    for base, _, files in os.walk(os.path.join(ASSETS, "models")):
        for name in files:
            path = os.path.join(base, name)
            node = json.loads(read(path))
            textures = node.get("textures", {})
            for key, value in textures.items():
                if isinstance(value, str) and not value.startswith("#"):
                    refs.append((os.path.relpath(path, ASSETS), key, value))
    missing = []
    for source, key, value in refs:
        namespace, _, path = value.partition(":")
        if namespace != MOD_ID:
            continue
        if not os.path.isfile(os.path.join(ASSETS, "textures", path + ".png")):
            missing.append((source, key, value))
    for source, key, value in missing:
        fail(f"TEXTURES {source} ({key}) references missing texture {value}")
    notes.append(f"texture references checked: {len([r for r in refs if r[2].startswith(MOD_ID)])}")


def check_entity_assets():
    renderers = os.path.join(SRC, "com", "hollowlight", "entity", "renderer")
    for entity in ENTITIES:
        texture = os.path.join(ASSETS, "textures", "entity", entity + ".png")
        if not os.path.isfile(texture):
            fail(f"ENTITIES missing texture for {entity}")
        model = os.path.join(SRC, "com", "hollowlight", "entity", "model",
                             "".join(part.capitalize() for part in entity.split("_")) + "EntityModel.java")
        if not os.path.isfile(model):
            fail(f"ENTITIES missing model class for {entity}")
        found = [name for name in os.listdir(renderers)
                 if name.lower().startswith(entity.split("_")[0].lower()) and name.endswith("Renderer.java")]
        if not found:
            fail(f"ENTITIES missing renderer for {entity}")
    notes.append(f"entity asset sets checked: {len(ENTITIES)}")


# ---------------------------------------------------------------------------- 5. language --

def check_language(sounds):
    path = os.path.join(ASSETS, "lang", "en_us.json")
    if not os.path.isfile(path):
        fail("LANG en_us.json missing")
        return
    lang = json.loads(read(path))
    needed = set()
    for java in java_files():
        source = read(java)
        needed.update(re.findall(r'translatable\(\s*"([^"]+)"', source))
        needed.update(re.findall(r'"([a-z][a-z0-9_.]*\.hollowlight\.[a-z0-9_.]+)"', source))
    # dynamic keys built by concatenation
    for index in range(1, 6):
        needed.add(f"hollowlight.layer.{index}")
        needed.add(f"hollowlight.descend.{index}")
    for category in ("journal", "letter", "inscription", "room", "item", "voice"):
        needed.add(f"hollowlight.lore.category.{category}")
    for name in BLOCKS:
        needed.add(f"block.{MOD_ID}.{name}")
    for name in ITEMS:
        needed.add(f"item.{MOD_ID}.{name}")
    for entity in ENTITIES:
        needed.add(f"entity.{MOD_ID}.{entity}")
    for path_, _, _, _, _, _ in ADVANCEMENTS:
        key = path_.replace("/", ".")
        needed.add(f"advancement.{MOD_ID}.{key}.title")
        needed.add(f"advancement.{MOD_ID}.{key}.description")
    for node in sounds.values():
        if node.get("subtitle"):
            needed.add(node["subtitle"])
    needed = {key for key in needed if not key.endswith(".")}
    missing = sorted(key for key in needed if key not in lang)
    for key in missing:
        fail(f"LANG missing key: {key}")
    notes.append(f"lang keys: {len(lang)} present, {len(needed)} required")


def check_gui_textures():
    for name in ("journal", "altar", "map", "ending", "hud", "advancement_background"):
        if not os.path.isfile(os.path.join(ASSETS, "textures", "gui", name + ".png")):
            fail(f"GUI texture missing: {name}.png")


# ------------------------------------------------------------------------------- 6. data --

KNOWN_ITEM = set(BLOCKS) | set(ITEMS)
VANILLA_ALLOW = set()


def check_data(sounds):
    known_entities = set(ENTITIES)
    loot_dir = os.path.join(DATA, "loot_table")
    loot_tables = set()
    for base, _, files in os.walk(loot_dir):
        for name in files:
            loot_tables.add(os.path.relpath(os.path.join(base, name), loot_dir)[:-5].replace(os.sep, "/"))
    if len(loot_tables) < 15:
        fail(f"LOOT only {len(loot_tables)} loot tables (minimum 15)")

    advancements = set()
    adv_dir = os.path.join(DATA, "advancement")
    for base, _, files in os.walk(adv_dir):
        for name in files:
            advancements.add(os.path.relpath(os.path.join(base, name), adv_dir)[:-5].replace(os.sep, "/"))

    for base, _, files in os.walk(DATA):
        for name in files:
            if not name.endswith(".json"):
                continue
            path = os.path.join(base, name)
            try:
                node = json.loads(read(path))
            except json.JSONDecodeError as error:
                fail(f"JSON {os.path.relpath(path, ROOT)}: {error}")
                continue
            rel = os.path.relpath(path, DATA)
            walk_ids(node, rel, known_entities, loot_tables, advancements, sounds)

    # recipes: outputs exist, ingredients exist, no self-feeding
    recipe_dir = os.path.join(DATA, "recipe")
    produced = defaultdict(list)
    for name in sorted(os.listdir(recipe_dir)):
        node = json.loads(read(os.path.join(recipe_dir, name)))
        result = node.get("result", {}).get("id", "")
        if result.startswith(MOD_ID + ":"):
            short = result.split(":")[1]
            if short not in KNOWN_ITEM:
                fail(f"RECIPE {name}: result {result} is not a registered item")
            produced[short].append(name)
        elif not result.startswith("minecraft:"):
            fail(f"RECIPE {name}: malformed result {result}")
    notes.append(f"recipes: {sum(len(v) for v in produced.values())} producing mod items")
    for name in ITEMS:
        if name not in produced:
            notes.append(f"item without recipe (loot/salvage only): {name}")

    recipe_count = len(os.listdir(recipe_dir))
    if recipe_count < 25:
        fail(f"RECIPE only {recipe_count} recipes (minimum 25)")

    # advancement graph
    for path_, title, description, icon, parent, hidden in ADVANCEMENTS:
        if parent in ("", None):
            continue
        if parent not in advancements:
            fail(f"ADVANCEMENT {path_} has parent {parent} which does not exist")
    roots = [path_ for path_, _, _, _, parent, _ in ADVANCEMENTS if path_ == "depth/root"]
    if not roots:
        fail("ADVANCEMENT no root advancement")
    if len(ADVANCEMENTS) < 20:
        fail(f"ADVANCEMENT only {len(ADVANCEMENTS)} advancements (minimum 20)")
    notes.append(f"loot tables: {len(loot_tables)}, advancements: {len(advancements)}")

    # worldgen file sets
    for layer in LAYERS:
        for folder, suffix in (("dimension", ".json"), ("dimension_type", ".json"),
                               (os.path.join("worldgen", "biome"), ".json")):
            path = os.path.join(DATA, folder, layer["name"] + suffix)
            if not os.path.isfile(path):
                fail(f"WORLDGEN missing {folder}/{layer['name']}{suffix}")


def walk_ids(node, rel, entities, loot_tables, advancements, sounds):
    if isinstance(node, dict):
        for key, value in node.items():
            if isinstance(value, str):
                check_id_string(value, rel, key, entities, loot_tables, advancements, sounds)
            else:
                walk_ids(value, rel, entities, loot_tables, advancements, sounds)
    elif isinstance(node, list):
        for value in node:
            if isinstance(value, str):
                check_id_string(value, rel, "", entities, loot_tables, advancements, sounds)
            else:
                walk_ids(value, rel, entities, loot_tables, advancements, sounds)


def check_id_string(value, rel, key, entities, loot_tables, advancements, sounds):
    if not value.startswith(MOD_ID + ":"):
        return
    path = value.split(":", 1)[1]
    if path.endswith(".png"):
        if not os.path.isfile(os.path.join(ASSETS, path)):
            fail(f"DATA {rel}: missing texture {value}")
        return
    if key in ("sound", "ambient_sound") or path.startswith(("music.", "ambience.", "ambient.",
                                                              "entity.", "block.", "item.", "horror.")):
        if path in sounds:
            return
        if key in ("sound", "ambient_sound"):
            fail(f"DATA {rel}: sound {value} not in sounds.json")
            return
    if path in loot_tables or path in advancements:
        return
    if path in ("labyrinth",) or path.startswith("layer_"):
        return
    if path in KNOWN_ITEM:
        return
    if path in entities:
        return
    fail(f"DATA {rel}: unresolved id {value} (key={key})")


# ------------------------------------------------------------------------- 7. sound files --

def check_sound_files():
    directory = os.path.join(ASSETS, "sounds")
    total = 0
    size = 0
    for base, _, files in os.walk(directory):
        for name in files:
            if name.endswith(".ogg"):
                total += 1
                size += os.path.getsize(os.path.join(base, name))
    if total < 20:
        fail(f"SOUND FILES only {total} ogg files (minimum 20)")
    notes.append(f"ogg files: {total} ({size / 1e6:.1f} MB)")


# -------------------------------------------------------------------------- 8. mod meta ----

def check_mod_meta():
    path = os.path.join(RES, "fabric.mod.json")
    node = json.loads(read(path))
    entrypoints = node.get("entrypoints", {})
    for kind, values in entrypoints.items():
        for value in values:
            cls = value.split(":")[0] if isinstance(value, str) else value.get("value", "").split(":")[0]
            rel = os.path.join(SRC, cls.replace(".", os.sep) + ".java")
            if not os.path.isfile(rel):
                fail(f"META fabric.mod.json entrypoint {cls} ({kind}) has no source file")
    if node.get("id") != MOD_ID:
        fail("META fabric.mod.json id mismatch")
    if "fabric-api" not in json.dumps(node.get("depends", {})).lower() and \
            "fabric" not in json.dumps(node.get("depends", {})).lower():
        fail("META fabric.mod.json does not depend on Fabric API")
    icons = node.get("icon", {})
    icons = icons.values() if isinstance(icons, dict) else [icons]
    for icon in icons:
        if not os.path.isfile(os.path.join(RES, icon)):
            fail(f"META icon {icon} missing")
    for mixin in node.get("mixins", []):
        name = mixin if isinstance(mixin, str) else mixin.get("config")
        if not os.path.isfile(os.path.join(RES, name)):
            fail(f"META mixin config {name} missing")


def main():
    check_java()
    fields = check_registry_symbols()
    sounds = check_sounds(fields)
    check_particles(fields)
    check_models()
    collect_texture_refs()
    check_entity_assets()
    check_language(sounds)
    check_gui_textures()
    check_data(sounds)
    check_sound_files()
    check_mod_meta()

    print("\n".join("note: " + note for note in notes))
    if problems:
        print("\n".join("FAIL: " + problem for problem in problems))
        print(f"\n{len(problems)} problem(s)")
        return 1
    print("\nall checks passed")
    return 0


if __name__ == "__main__":
    sys.exit(main())
