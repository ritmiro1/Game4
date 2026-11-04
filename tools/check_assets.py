#!/usr/bin/env python3
"""Static checks for the things that only break at runtime.

The compiler cannot see resource files or the shape of a ModelPart tree, so these checks cover
the failure modes that actually crashed the game:

  * a block model whose element uses an invalid face name ("all" instead of the six directions);
  * a blockstate pointing at a model that does not exist;
  * a model asking for a texture that is not in the jar;
  * an entity model whose constructor walks a ModelPart path that getTexturedModelData never
    builds ("Can't find part head");
  * a sound event played by the code but missing from sounds.json (or a missing .ogg);
  * a translation key that no lang file defines.

Exit code is non-zero when anything is wrong, so CI fails loudly instead of shipping a jar that
dies on the loading screen.
"""

from __future__ import annotations

import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src/main/resources/assets/hollowlight")
JAVA = os.path.join(ROOT, "src/main/java")
DATA = os.path.join(ROOT, "src/main/resources/data/hollowlight")

VALID_FACES = {"north", "south", "east", "west", "up", "down"}

problems: list[str] = []


def fail(message: str) -> None:
    problems.append(message)


def read(path: str) -> str:
    with open(path, encoding="utf-8") as handle:
        return handle.read()


def load_json(path: str):
    try:
        with open(path, encoding="utf-8") as handle:
            return json.load(handle)
    except Exception as error:  # noqa: BLE001 - we report whatever the parser said
        fail("%s: unreadable JSON: %s" % (os.path.relpath(path, ROOT), error))
        return None


# --------------------------------------------------------------------------- block models ----

def texture_exists(reference: str) -> bool:
    if reference.startswith("#"):
        return True
    if reference.startswith("minecraft:"):
        return True
    if ":" in reference and not reference.startswith("hollowlight:"):
        return True
    path = reference.split(":")[-1]
    return os.path.exists(os.path.join(ASSETS, "textures", path + ".png"))


def check_models() -> set[str]:
    names: set[str] = set()
    for path in glob.glob(os.path.join(ASSETS, "models", "**", "*.json"), recursive=True):
        relative = os.path.relpath(path, ASSETS)
        names.add(relative[:-5])
        model = load_json(path)
        if model is None:
            continue

        parent = model.get("parent")
        if parent and parent.startswith("hollowlight:"):
            wanted = "models/" + parent.split(":", 1)[1] + ".json"
            if not os.path.exists(os.path.join(ASSETS, wanted)):
                fail("%s: parent model %s does not exist" % (relative, parent))

        for index, element in enumerate(model.get("elements", [])):
            for face in element.get("faces", {}):
                if face not in VALID_FACES:
                    fail("%s: element %d uses invalid face name %r" % (relative, index, face))
            texture = element.get("faces", {})
            for face, body in texture.items():
                reference = body.get("texture")
                if reference and not texture_exists(reference):
                    fail("%s: element %d face %s wants missing texture %s"
                         % (relative, index, face, reference))

        for key, reference in model.get("textures", {}).items():
            # A texture variable may point at another variable, which is fine.
            if not texture_exists(reference):
                fail("%s: texture variable %s points at missing file %s"
                     % (relative, key, reference))
    return names


def check_blockstates(models: set[str]) -> None:
    for path in glob.glob(os.path.join(ASSETS, "blockstates", "*.json")):
        blockstate = load_json(path)
        relative = os.path.relpath(path, ASSETS)
        if blockstate is None:
            continue
        entries = []
        for value in (blockstate.get("variants") or {}).values():
            entries.extend(value if isinstance(value, list) else [value])
        for variant in blockstate.get("multipart", []):
            entry = variant.get("apply")
            if isinstance(entry, dict):
                entries.append(entry)
        for entry in entries:
            model = entry.get("model")
            if not model:
                fail("%s: variant without a model" % relative)
                continue
            if model.startswith("hollowlight:"):
                wanted = "models/" + model.split(":", 1)[1] + ".json"
                if not os.path.exists(os.path.join(ASSETS, wanted)):
                    fail("%s: variant points at missing model %s" % (relative, model))


def check_items() -> None:
    source = read(os.path.join(JAVA, "com/hollowlight/registry/ModItems.java"))
    registered = set(re.findall(r'register\(\s*"([a-z0-9_]+)"', source))
    block_source = read(os.path.join(JAVA, "com/hollowlight/registry/ModBlocks.java"))
    registered |= set(re.findall(r'register\(\s*"([a-z0-9_]+)"', block_source))
    have = {os.path.basename(p)[:-5]
            for p in glob.glob(os.path.join(ASSETS, "models", "item", "*.json"))}
    for name in sorted(registered - have):
        # Blocks with a blockstate get their item model from the block model; only report
        # things that have neither.
        if os.path.exists(os.path.join(ASSETS, "blockstates", name + ".json")):
            continue
        fail("item %s has no item model" % name)


# -------------------------------------------------------------------------- entity models ---

def check_entity_models() -> None:
    for path in sorted(glob.glob(os.path.join(JAVA, "com/hollowlight/entity/model/*.java"))):
        source = read(path)
        short = os.path.basename(path)

        # ModelPartData <var> = <parentVar>.addChild("<name>", ...)
        tree: dict[str, str] = {}
        parents: dict[str, str] = {}
        for match in re.finditer(r'ModelPartData\s+(\w+)\s*=\s*(\w+)\.addChild\(\s*"(\w+)"', source):
            var, parent_var, child_name = match.groups()
            tree[child_name] = var
            parents[child_name] = parent_var
        if not tree:
            fail("%s: no addChild calls found" % short)
            continue

        children: dict[str, set[str]] = {}
        for child, parent_var in parents.items():
            parent_name = None if parent_var == "root" else next(
                (name for name, var in tree.items() if var == parent_var), None)
            if parent_var != "root" and parent_name is None:
                fail("%s: part %s hangs off unknown parent variable %s"
                     % (short, child, parent_var))
                continue
            children.setdefault(parent_name, set()).add(child)

        # this.<part> = root.getChild("a").getChild("b")...
        for match in re.finditer(r'this\.(\w+)\s*=\s*root((?:\.getChild\("\w+"\))+);', source):
            part, hops = match.group(1), match.group(2)
            path_parts = re.findall(r'\.getChild\("(\w+)"\)', hops)
            current: str | None = None
            ok = True
            for hop in path_parts:
                if hop not in children.get(current, set()):
                    fail("%s: constructor walks %s but %s has no child %r"
                         % (short, part, "root" if current is None else current, hop))
                    ok = False
                    break
                current = hop
            if ok and current != part:
                fail("%s: constructor assigns %s from a path ending in %r"
                     % (short, part, current))

        declared = set(re.findall(r'private final ModelPart (\w+);', source))
        assigned = set(re.findall(r'this\.(\w+)\s*=\s*root', source))
        for missing in sorted(declared - assigned):
            fail("%s: field %s is never assigned" % (short, missing))


# ------------------------------------------------------------------------------- sounds ----

def check_sounds() -> None:
    path = os.path.join(ASSETS, "sounds.json")
    sounds = load_json(path) or {}
    for event, body in sounds.items():
        for entry in body.get("sounds", []):
            name = entry["name"] if isinstance(entry, dict) else entry
            if not os.path.exists(os.path.join(ASSETS, "sounds", name.split(":")[-1] + ".ogg")):
                fail("sounds.json: event %s references missing file %s" % (event, name))

    source = read(os.path.join(JAVA, "com/hollowlight/registry/ModSounds.java"))
    declared = dict(re.findall(r'([A-Z0-9_]+)\s*=\s*create\(\s*"([a-z0-9_.]+)"', source))
    for field, event in declared.items():
        if event not in sounds:
            fail("ModSounds.%s plays %r, which sounds.json does not define" % (field, event))
    for event in sounds:
        if event not in declared.values():
            fail("sounds.json defines %r but no ModSounds field uses it" % event)


# --------------------------------------------------------------------------------- lang ----

def check_lang() -> None:
    lang = load_json(os.path.join(ASSETS, "lang", "en_us.json")) or {}
    used: set[str] = set()
    for path in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
        source = read(path)
        used |= set(re.findall(r'Text\.translatable\(\s*"([a-z0-9_.]+)"', source))
    for key in sorted(used - set(lang)):
        # Keys built as "prefix." + n are only prefix fragments, not real keys.
        if key.endswith(".") or key.endswith("_"):
            continue
        fail("en_us.json is missing translation key %r" % key)


# ------------------------------------------------------------------------------- recipe -----

def check_loot_tables() -> None:
    for path in glob.glob(os.path.join(DATA, "loot_table", "**", "*.json"), recursive=True):
        table = load_json(path)
        if table is None:
            continue
        for pool in table.get("pools", []):
            for entry in pool.get("entries", []):
                item = entry.get("name")
                if item and item.startswith("hollowlight:"):
                    name = item.split(":", 1)[1]
                    source = (read(os.path.join(JAVA, "com/hollowlight/registry/ModItems.java"))
                              + read(os.path.join(JAVA, "com/hollowlight/registry/ModBlocks.java")))
                    if '"%s"' % name not in source:
                        fail("%s: loot entry references unknown item %s"
                             % (os.path.relpath(path, ROOT), item))



# -------------------------------------------------------------------------- worldgen data ---

DIMENSION_TYPE_FIELDS = {
    "ultrawarm", "natural", "coordinate_scale", "has_skylight", "has_ceiling", "ambient_light",
    "monster_spawn_light_level", "monster_spawn_block_light_limit", "min_y", "height",
    "logical_height", "infiniburn",
}
BIOME_FIELDS = {"has_precipitation", "temperature", "downfall", "effects", "spawners",
                "spawn_costs", "carvers", "features"}


def check_worldgen() -> None:
    sounds = (load_json(os.path.join(ASSETS, "sounds.json")) or {})

    def sound_refs(node, where):
        if isinstance(node, dict):
            for key, value in node.items():
                if key == "sound" and isinstance(value, str) and value.startswith("hollowlight:"):
                    if value.split(":", 1)[1] not in sounds:
                        fail("%s: references undefined sound event %s" % (where, value))
                else:
                    sound_refs(value, where)
        elif isinstance(node, list):
            for value in node:
                sound_refs(value, where)

    for path in sorted(glob.glob(os.path.join(DATA, "dimension_type", "*.json"))):
        name = os.path.basename(path)
        data = load_json(path) or {}
        missing = DIMENSION_TYPE_FIELDS - set(data)
        if missing:
            fail("dimension_type/%s: missing %s" % (name, ", ".join(sorted(missing))))
        if not isinstance(data.get("min_y"), int) or not isinstance(data.get("height"), int):
            fail("dimension_type/%s: min_y and height must be integers" % name)
        elif data["height"] % 16 or data["min_y"] % 16:
            fail("dimension_type/%s: min_y and height must be multiples of 16" % name)
        sound_refs(data.get("effects", {}), "dimension_type/" + name)

    for path in sorted(glob.glob(os.path.join(DATA, "biome", "*.json")) +
                       glob.glob(os.path.join(DATA, "worldgen/biome", "*.json"))):
        name = os.path.relpath(path, DATA)
        data = load_json(path) or {}
        missing = BIOME_FIELDS - set(data)
        if missing:
            fail("%s: missing %s" % (name, ", ".join(sorted(missing))))
        sound_refs(data.get("effects", {}), name)

    generator_types = set(re.findall(r'create\(\s*"([a-z0-9_.]+)"', read(
        os.path.join(JAVA, "com/hollowlight/registry/ModWorldgen.java"))))
    for path in sorted(glob.glob(os.path.join(DATA, "dimension", "*.json"))):
        name = os.path.basename(path)
        data = load_json(path) or {}
        type_id = data.get("type", "")
        if type_id.startswith("hollowlight:"):
            wanted = os.path.join(DATA, "dimension_type", type_id.split(":", 1)[1] + ".json")
            if not os.path.exists(wanted):
                fail("dimension/%s: type %s has no dimension_type file" % (name, type_id))
        generator = data.get("generator", {})
        gen_type = generator.get("type", "")
        if gen_type.startswith("hollowlight:"):
            registered = gen_type.split(":", 1)[1]
            if registered not in generator_types and '"labyrinth"' not in read(
                    os.path.join(JAVA, "com/hollowlight/registry/ModWorldgen.java")):
                fail("dimension/%s: generator type %s is not registered in code" % (name, gen_type))
            if not registered == "labyrinth":
                fail("dimension/%s: unknown generator type %s" % (name, gen_type))
        source = generator.get("biome_source", {})
        biome = source.get("biome", "")
        if biome.startswith("hollowlight:"):
            short = biome.split(":", 1)[1]
            candidates = [os.path.join(DATA, "biome", short + ".json"),
                          os.path.join(DATA, "worldgen/biome", short + ".json")]
            if not any(os.path.exists(c) for c in candidates):
                fail("dimension/%s: biome %s does not exist" % (name, biome))



# ------------------------------------------------------------------ biome / recipe shapes ---

def check_biome_shapes() -> None:
    """Each of these shapes was wrong at least once and each one aborts a registry load."""
    for path in sorted(glob.glob(os.path.join(DATA, "worldgen/biome", "*.json")) +
                       glob.glob(os.path.join(DATA, "biome", "*.json"))):
        name = os.path.relpath(path, DATA)
        data = load_json(path) or {}

        carvers = data.get("carvers")
        if isinstance(carvers, dict):
            fail("%s: carvers must be a list of configured carvers (or a tag string), not a map"
                 % name)
        elif not isinstance(carvers, (list, str)):
            fail("%s: carvers must be a list or a tag string" % name)

        features = data.get("features")
        if features is not None:
            if not isinstance(features, list):
                fail("%s: features must be a list of per-step lists" % name)
            else:
                for step, entry in enumerate(features):
                    if not isinstance(entry, list):
                        fail("%s: features[%d] must be a list" % (name, step))

        for key in ("spawners", "spawn_costs"):
            if key in data and not isinstance(data[key], dict):
                fail("%s: %s must be an object" % (name, key))

        effects = data.get("effects", {})
        music = effects.get("music")
        if music is not None:
            if not isinstance(music, list):
                fail("%s: effects.music must be a list of {data, weight} entries "
                     "(1.20.5+ weighted music); a bare object aborts biome loading" % name)
            else:
                for index, entry in enumerate(music):
                    if not isinstance(entry, dict) or "data" not in entry or "weight" not in entry:
                        fail("%s: effects.music[%d] needs both \'data\' and \'weight\'"
                             % (name, index))
                    else:
                        sound = entry["data"].get("sound")
                        if not isinstance(sound, str):
                            fail("%s: effects.music[%d].data.sound must be a sound id"
                                 % (name, index))

        for key in ("ambient_sound",):
            if key in effects and not isinstance(effects[key], str):
                fail("%s: effects.%s must be a sound id" % (name, key))
        mood = effects.get("mood_sound")
        if mood is not None and (not isinstance(mood, dict) or "sound" not in mood):
            fail("%s: effects.mood_sound needs a \'sound\' id" % name)


def check_recipes() -> None:
    for path in sorted(glob.glob(os.path.join(DATA, "recipe", "**", "*.json"), recursive=True) +
                       glob.glob(os.path.join(DATA, "recipes", "**", "*.json"), recursive=True)):
        name = os.path.relpath(path, DATA)
        recipe = load_json(path)
        if recipe is None:
            continue
        kind = recipe.get("type")
        if not isinstance(kind, str):
            # 1.21.4 keeps the type inside an object: {"type": "minecraft:crafting_shaped", ...}
            fail("%s: missing recipe type" % name)
            continue
        short = kind.split(":", 1)[1] if ":" in kind else kind
        if short == "crafting_shaped":
            if not isinstance(recipe.get("pattern"), list):
                fail("%s: shaped recipe needs a pattern list" % name)
            if not isinstance(recipe.get("key"), dict):
                fail("%s: shaped recipe needs a key object" % name)
        elif short == "crafting_shapeless":
            if not isinstance(recipe.get("ingredients"), list):
                fail("%s: shapeless recipe needs an ingredients list" % name)
        if "result" not in recipe:
            fail("%s: recipe has no result" % name)


def check_advancements() -> None:
    for path in sorted(glob.glob(os.path.join(DATA, "advancement", "**", "*.json"), recursive=True)):
        name = os.path.relpath(path, DATA)
        advancement = load_json(path)
        if advancement is None:
            continue
        criteria = advancement.get("criteria")
        if not criteria:
            fail("%s: advancement has no criteria" % name)
        parent = advancement.get("parent")
        if isinstance(parent, str) and parent.startswith("hollowlight:"):
            wanted = os.path.join(DATA, "advancement", parent.split(":", 1)[1] + ".json")
            if not os.path.exists(wanted):
                fail("%s: parent advancement %s does not exist" % (name, parent))


def check_tags() -> None:
    for path in glob.glob(os.path.join(DATA, "tags", "**", "*.json"), recursive=True):
        name = os.path.relpath(path, DATA)
        tag = load_json(path)
        if tag is None:
            continue
        if not isinstance(tag.get("values"), list):
            fail("%s: tag has no values list" % name)

def main() -> int:
    models = check_models()
    check_blockstates(models)
    check_items()
    check_entity_models()
    check_sounds()
    check_lang()
    check_loot_tables()
    check_worldgen()
    check_biome_shapes()
    check_recipes()
    check_advancements()
    check_tags()

    if problems:
        print("asset checks failed: %d problem(s)" % len(problems))
        for problem in problems[:120]:
            print("  - " + problem)
        return 1
    print("asset checks: models, blockstates, items, entity part trees, sounds, lang, loot all clean")
    return 0


if __name__ == "__main__":
    sys.exit(main())
