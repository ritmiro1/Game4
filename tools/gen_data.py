"""Recipes, loot tables, advancements, worldgen (dimensions/biomes/types), tags, language."""
from __future__ import annotations

import json
import os

from content import (ADVANCEMENTS, BLOCKS, ENTITIES, ITEMS, LAYERS, LOOT_TABLES, MOD_ID)

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
DATA = os.path.join(ROOT, "data", MOD_ID)
ASSETS = os.path.join(ROOT, "assets", MOD_ID)


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


# Names in the recipe/loot tables that belong to vanilla rather than to this mod.
VANILLA_IDS = {
    "glass", "glass_pane", "glass_bottle", "iron_ingot", "iron_nugget", "stick", "flint",
    "paper", "string", "bone", "bone_meal", "charcoal", "coal", "amethyst_shard", "kelp",
    "sugar", "gunpowder", "bowl", "leather", "deepslate", "stone", "stone_bricks",
    "cobblestone", "sand", "chain", "torch", "glowstone_dust", "oak_log", "crying_obsidian",
}


def item(name):
    """`hollowlight:foo` for mod ids, `minecraft:foo` for the vanilla materials used above."""
    if ":" in name:
        return name
    if name in VANILLA_IDS:
        return f"minecraft:{name}"
    return f"{MOD_ID}:{name}"


def vanilla(name):
    return f"minecraft:{name}"


def rid(name):
    return item(name)


# ============================================================================= worldgen ==

def worldgen():
    for layer in LAYERS:
        index = layer["index"]
        fog = layer["fog"]
        write_json(os.path.join(DATA, "dimension_type", layer["name"] + ".json"), {
            "ultrawarm": False,
            "natural": False,
            "piglin_safe": False,
            "respawn_anchor_works": False,
            "bed_works": False,
            "has_raids": False,
            "has_skylight": False,
            "has_ceiling": True,
            "coordinate_scale": 1.0,
            "ambient_light": layer["ambient_light"],
            "logical_height": 128,
            "min_y": 0,
            "height": 128,
            "monster_spawn_light_level": 0,
            "monster_spawn_block_light_limit": 0,
            "infiniburn": "#minecraft:infiniburn_overworld",
            "effects": "minecraft:overworld",
        })

        write_json(os.path.join(DATA, "dimension", layer["name"] + ".json"), {
            "type": f"{MOD_ID}:{layer['name']}",
            "generator": {
                "type": f"{MOD_ID}:labyrinth",
                "layer": index,
                "floor_y": layer["floor_y"],
                "biome_source": {"type": "minecraft:fixed", "biome": f"{MOD_ID}:{layer['name']}"},
            },
        })

        write_json(os.path.join(DATA, "worldgen", "biome", layer["name"] + ".json"), {
            "has_precipitation": False,
            "temperature": layer["biome_temperature"],
            "downfall": 0.0,
            "effects": {
                "fog_color": (fog[0] << 16) | (fog[1] << 8) | fog[2],
                "sky_color": (layer["sky"][0] << 16) | (layer["sky"][1] << 8) | layer["sky"][2],
                "water_color": (layer["water"][0] << 16) | (layer["water"][1] << 8) | layer["water"][2],
                "water_fog_color": (layer["water_fog"][0] << 16) | (layer["water_fog"][1] << 8)
                                   | layer["water_fog"][2],
                "mood_sound": {
                    "sound": f"{MOD_ID}:ambience.{['halls', 'drowned', 'living', 'memorial', 'heart'][index - 1]}",
                    "tick_delay": 4200,
                    "block_search_extent": 8,
                    "offset": 2.0,
                },
                "ambient_sound": f"{MOD_ID}:ambient.wind",
                # 1.20.5+ wants a weighted list of music tracks, not a bare MusicSound:
                # {"data": MusicSound, "weight": n}. A bare object fails with
                # "No key weight in MapLike[...]" and kills the whole biome registry load.
                "music": [
                    {"weight": 1,
                     "data": {"sound": f"{MOD_ID}:music.{['halls', 'drowned', 'living', 'memorial', 'heart'][index - 1]}",
                              "min_delay": 12000, "max_delay": 24000, "replace_current_music": True}}
                ],
            },
            "carvers": [],
            "features": [],
            "spawners": {},
            "spawn_costs": {},
        })


# ============================================================================== recipes ==

SHAPED = [
    ("lamplighter_lantern", [" G ", "GEG", " G "], {"G": vanilla("glass"), "E": "hollow_ember"}, 1),
    ("luminous_flask", [" G ", "GEG", "GBG"], {"G": vanilla("glass_pane"), "E": "hollow_ember",
                                                "B": vanilla("glass_bottle")}, 2),
    ("cracked_lens", [" G ", "GSG", " G "], {"G": vanilla("glass_pane"),
                                             "S": vanilla("amethyst_shard")}, 1),
    ("wickbow", ["GS ", "G W", "GS "], {"G": vanilla("stick"), "S": vanilla("string"),
                                        "W": "oiled_wick"}, 1),
    ("glass_bolt", ["  G", " F ", "F  "], {"G": vanilla("glass_pane"), "F": vanilla("flint")}, 4),
    ("miner_shiv", ["F", "S"], {"F": vanilla("flint"), "S": vanilla("stick")}, 1),
    ("rusted_pickhammer", ["III", " S ", " S "], {"I": vanilla("iron_ingot"),
                                                   "S": vanilla("stick")}, 1),
    ("lantern_chain", ["NIN", " C ", " S "], {"N": vanilla("iron_nugget"), "I": vanilla("chain"),
                                               "C": "hollow_ember", "S": vanilla("stick")}, 1),
    ("lockpick", ["  I", " I ", "I  "], {"I": vanilla("iron_nugget")}, 2),
    ("hollow_lantern", [" I ", "IEI", " T "], {"I": vanilla("iron_ingot"),
                                               "E": "hollow_ember", "T": vanilla("torch")}, 2),
    ("memory_lamp", ["GEG", "GLG", "GEG"], {"G": vanilla("glass"), "E": "hollow_ember",
                                             "L": "lamp_glass"}, 1),
    ("lamp_glass", [" G ", "GDG", " G "], {"G": vanilla("glass"),
                                             "D": vanilla("glowstone_dust")}, 4),
    ("memory_altar", ["DSD", "SES", "DSD"], {"D": vanilla("deepslate"), "S": "salt_of_hollowmere",
                                              "E": "hollow_ember"}, 1),
    ("memory_pedestal", ["SSS", " P ", "SSS"], {"S": vanilla("stone_bricks"),
                                                 "P": "pale_crystal"}, 2),
    ("inscription_stone", ["SSS", "SCS", "SSS"], {"S": vanilla("stone"),
                                                   "C": vanilla("charcoal")}, 4),
    ("whispering_stone", ["CCC", "CBC", "CCC"], {"C": vanilla("cobblestone"),
                                                  "B": "bone_stitch"}, 2),
    ("pale_crystal", [" A ", "AGA", " A "], {"A": vanilla("amethyst_shard"),
                                             "G": vanilla("glass")}, 1),
    ("root_planks", ["LL", "LL"], {"L": vanilla("oak_log")}, 4),
    ("shrine_bricks", ["SS", "SS"], {"S": vanilla("stone_bricks")}, 4),
    ("sealed_gate", ["III", "I I", "III"], {"I": vanilla("iron_ingot")}, 2),
    ("carved_token", ["BS", "SB"], {"B": vanilla("bone"), "S": "salt_of_hollowmere"}, 2),
    ("memory_shard", ["GG", "GA"], {"G": "lamp_glass", "A": vanilla("amethyst_shard")}, 1),
]

SHAPELESS = [
    ("lamp_oil", ["hollow_ember", "salt_of_hollowmere", "glass_bottle"], 2),
    ("oiled_wick", ["living_fiber", "string"], 2),
    ("wax_bandage", ["paper", "living_fiber"], 2),
    ("bitterroot_tea", ["salt_of_hollowmere", "kelp", "bowl"], 1),
    ("sweet_ash", ["sugar", "grave_dust"], 2),
    ("drowned_breath", ["pale_scale", "kelp", "glass_bottle"], 1),
    ("memory_draught", ["memory_shard", "sweet_ash", "glass_bottle"], 1),
    ("broken_music_box", ["torn_journal_page", "root_planks", "living_fiber"], 1),
    ("glass_bolt", ["glass_pane", "flint", "bone_stitch"], 4),
    ("seventh_seam_dust", ["grave_dust", "salt_of_hollowmere", "coal"], 2),
    ("living_fiber", ["corrupted_growth", "corrupted_growth"], 3),
    ("bone_stitch", ["bone", "string"], 4),
    ("grave_dust", ["bone_meal", "sand", "sand"], 3),
    ("salt_of_hollowmere", ["sugar", "gunpowder"], 2),
]

SMELTING = [
    ("salt_of_hollowmere", "grave_dust", 0.35, 200),
    ("glowstone_dust", "sweet_ash", 0.2, 200),
]

BLASTING = [
    ("hollow_ember", "memory_shard", 0.7, 100),
    ("hollow_ember", "seventh_seam_dust", 0.5, 100),
]


def recipes():
    for name, pattern, keys, count in SHAPED:
        write_json(os.path.join(DATA, "recipe", name + ".json"), {
            "type": "minecraft:crafting_shaped",
            "category": "misc",
            "pattern": pattern,
            "key": {symbol: {"item": item(value)} for symbol, value in keys.items()},
            "result": {"id": item(name), "count": count},
        })
    for name, materials, count in SHAPELESS:
        write_json(os.path.join(DATA, "recipe", name + "_shapeless.json"), {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [{"item": item(material)} for material in materials],
            "result": {"id": item(name), "count": count},
        })
    for index, (result, input_item, xp, time) in enumerate(SMELTING):
        write_json(os.path.join(DATA, "recipe", f"{result}_smelting_{index}.json"), {
            "type": "minecraft:smelting",
            "category": "misc",
            "ingredient": {"item": item(input_item)},
            "result": {"id": item(result), "count": 1},
            "experience": xp,
            "cookingtime": time,
        })
    for index, (result, input_item, xp, time) in enumerate(BLASTING):
        write_json(os.path.join(DATA, "recipe", f"{result}_blasting_{index}.json"), {
            "type": "minecraft:blasting",
            "category": "misc",
            "ingredient": {"item": item(input_item)},
            "result": {"id": item(result), "count": 1},
            "experience": xp,
            "cookingtime": time,
        })


# =========================================================================== loot tables ==

def entry(name, weight, count=(1, 1), table=None):
    node = {"type": table or "minecraft:item", "name": item(name), "weight": weight}
    if count != (1, 1):
        node["functions"] = [{"function": "minecraft:set_count",
                              "count": {"min": count[0], "max": count[1]}}]
    return node


def pool(rolls, entries):
    return {"rolls": rolls, "entries": entries}


CHESTS = {
    "chests/shrine_starter": ("minecraft:chest", [
        pool(1, [entry("lamplighter_lantern", 1)]),
        pool(1, [entry("lamplighters_journal", 1)]),
        pool(2, [entry("lamp_oil", 1, (1, 2)), entry("oiled_wick", 1, (2, 3)),
                 entry("wax_bandage", 1, (1, 3)), entry("torn_journal_page", 1),
                 entry("sweet_ash", 1, (1, 2))]),
    ]),
    "chests/layer_1": ("minecraft:chest", [
        pool(2, [entry("lamp_oil", 3, (1, 2)), entry("oiled_wick", 3, (2, 4)),
                 entry("wax_bandage", 2, (1, 2)), entry("carved_token", 1),
                 entry("torn_journal_page", 2), entry("salt_of_hollowmere", 2, (1, 3)),
                 entry("miner_shiv", 1), entry("grave_dust", 2, (2, 4))]),
        pool(1, [entry("lamplighters_journal", 1), entry("sweet_ash", 2, (1, 2)),
                 entry("lamp_glass", 1, (2, 3)), entry("hollow_ember", 2, (1, 2))]),
    ]),
    "chests/layer_2": ("minecraft:chest", [
        pool(2, [entry("pale_scale", 3, (1, 3)), entry("drowned_breath", 2, (1, 2)),
                 entry("lamp_oil", 2, (1, 2)), entry("water_stained_photograph", 1),
                 entry("unsent_letter", 1), entry("salt_of_hollowmere", 2, (2, 4)),
                 entry("bone_stitch", 2, (1, 3))]),
        pool(1, [entry("memory_shard", 1), entry("wickbow", 1), entry("drowned_key", 1)]),
    ]),
    "chests/layer_3": ("minecraft:chest", [
        pool(2, [entry("living_fiber", 3, (2, 4)), entry("bone_stitch", 2, (2, 4)),
                 entry("memory_shard", 2), entry("salt_of_hollowmere", 2, (2, 3)),
                 entry("lamp_oil", 2, (1, 2)), entry("miner_shiv", 1),
                 entry("lamplighters_band", 1)]),
        pool(1, [entry("lantern_chain", 1), entry("memory_draught", 1),
                 entry("torn_journal_page", 2, (1, 2))]),
    ]),
    "chests/layer_4": ("minecraft:chest", [
        pool(2, [entry("memory_shard", 3, (1, 2)), entry("sweet_ash", 2, (1, 2)),
                 entry("water_stained_photograph", 1), entry("unsent_letter", 1),
                 entry("broken_music_box", 1), entry("wax_bandage", 2, (1, 3)),
                 entry("oiled_wick", 2, (2, 4))]),
        pool(1, [entry("heart_key", 1), entry("memory_draught", 1, (1, 2)),
                 entry("iver_whistle", 1)]),
    ]),
    "chests/layer_5": ("minecraft:chest", [
        pool(3, [entry("memory_shard", 3, (1, 3)), entry("hollow_ember", 3, (2, 4)),
                 entry("memory_draught", 2), entry("lamplighters_band", 1)]),
    ]),
    "chests/safe_room": ("minecraft:chest", [
        pool(2, [entry("wax_bandage", 3, (1, 3)), entry("sweet_ash", 2, (1, 2)),
                 entry("lamp_oil", 3, (1, 3)), entry("oiled_wick", 2, (2, 4)),
                 entry("bitterroot_tea", 2, (1, 2)), entry("lamp_glass", 1, (2, 3))]),
    ]),
    "chests/lore_vault": ("minecraft:chest", [
        pool(2, [entry("torn_journal_page", 3), entry("water_stained_photograph", 2),
                 entry("unsent_letter", 2), entry("carved_token", 2),
                 entry("memory_shard", 2), entry("broken_music_box", 1)]),
    ]),
    "chests/library": ("minecraft:chest", [
        pool(3, [entry("torn_journal_page", 4, (1, 2)), entry("paper", 2, (2, 5)),
                 entry("memory_shard", 2), entry("carved_token", 1),
                 entry("lamp_oil", 1, (1, 2))]),
    ]),
    "chests/chapel": ("minecraft:chest", [
        pool(2, [entry("memory_shard", 3, (1, 2)), entry("lamplighters_band", 1),
                 entry("sweet_ash", 2, (1, 3)), entry("lamp_oil", 2, (1, 2)),
                 entry("torn_journal_page", 2)]),
    ]),
    "chests/trap_room": ("minecraft:chest", [
        pool(2, [entry("hollow_ember", 3, (2, 4)), entry("memory_shard", 2),
                 entry("lamp_oil", 2, (1, 3)), entry("lantern_chain", 1),
                 entry("glass_bolt", 2, (4, 8))]),
    ]),
    "chests/memorial": ("minecraft:chest", [
        pool(2, [entry("water_stained_photograph", 2), entry("carved_token", 2),
                 entry("broken_music_box", 2), entry("iver_whistle", 1),
                 entry("unsent_letter", 2), entry("wax_bandage", 2, (1, 2))]),
    ]),
    "chests/drowned": ("minecraft:chest", [
        pool(2, [entry("pale_scale", 3, (2, 4)), entry("drowned_breath", 2),
                 entry("salt_of_hollowmere", 2, (2, 4)), entry("bone_stitch", 2, (1, 3)),
                 entry("lamp_oil", 1, (1, 2))]),
    ]),
    "chests/boss_arena": ("minecraft:chest", [
        pool(3, [entry("heart_of_the_hollow", 1), entry("memory_draught", 2, (1, 2)),
                 entry("memory_shard", 3, (2, 4)), entry("lamplighters_band", 1)]),
    ]),
    "chests/key_rusted": ("minecraft:chest", [pool(1, [entry("rusted_key", 1)])]),
    "chests/key_drowned": ("minecraft:chest", [pool(1, [entry("drowned_key", 1),
                                                        entry("lockpick", 1)])]),
    "chests/key_heart": ("minecraft:chest", [pool(1, [entry("heart_key", 1),
                                                      entry("memory_draught", 1)])]),
}

ENTITY_DROPS = {
    "entities/hollowed": ("minecraft:entity", [
        pool(1, [entry("seventh_seam_dust", 2, (1, 3)), entry("grave_dust", 1, (1, 2)),
                 entry("carved_token", 1)]),
    ]),
    "entities/paperface": ("minecraft:entity", [
        pool(1, [entry("torn_journal_page", 2), entry("paper", 1, (2, 4))]),
        pool(1, [entry("memory_shard", 1)]),
    ]),
    "entities/stitcher": ("minecraft:entity", [
        pool(1, [entry("bone_stitch", 3, (2, 4)), entry("living_fiber", 2, (1, 3))]),
        pool(1, [entry("lantern_chain", 1)]),
    ]),
    "entities/drowned_choir": ("minecraft:entity", [
        pool(1, [entry("pale_scale", 3, (1, 3)), entry("drowned_breath", 1)]),
    ]),
    "entities/weeping_one": ("minecraft:entity", [
        pool(3, [entry("memory_shard", 1)]),
        pool(1, [entry("water_stained_photograph", 1)]),
    ]),
    "entities/lampkeeper": ("minecraft:entity", [
        pool(1, [entry("heart_of_the_hollow", 1)]),
        pool(4, [entry("memory_shard", 1)]),
    ]),
}


def loot_tables():
    for name, (kind, pools) in list(CHESTS.items()) + list(ENTITY_DROPS.items()):
        write_json(os.path.join(DATA, "loot_table", name + ".json"),
                   {"type": kind, "pools": pools})
    missing = [name for name in LOOT_TABLES if name not in CHESTS and name not in ENTITY_DROPS]
    assert not missing, f"loot tables defined in content.py but not generated: {missing}"


# ========================================================================== advancements ==

def normalize_icon(icon):
    """content.py writes icons as texture-ish paths (`hollowlight:block/forgotten_bricks`)."""
    if ":" not in icon:
        icon = item(icon)
    namespace, _, path = icon.partition(":")
    for prefix in ("block/", "item/"):
        if path.startswith(prefix):
            path = path[len(prefix):]
    return f"{namespace}:{path}"


def advancement_frame(path, hidden):
    if path.startswith("ending/"):
        return "challenge"
    if path.startswith("hidden/") or hidden:
        return "goal"
    return "task"


def advancements():
    for path, title, description, icon, parent, hidden in ADVANCEMENTS:
        node = {
            "parent": parent if "/" not in parent or "." in parent else f"{MOD_ID}:{parent}",
            "display": {
                "icon": {"id": normalize_icon(icon)},
                "title": {"translate": f"advancement.{MOD_ID}.{path.replace('/', '.')}.title"},
                "description": {"translate": f"advancement.{MOD_ID}.{path.replace('/', '.')}.description"},
                "frame": advancement_frame(path, hidden),
                "show_toast": True,
                "announce_to_chat": True,
                "hidden": hidden,
            },
            "criteria": {"code": {"trigger": "minecraft:impossible"}},
            "requirements": [["code"]],
        }
        if path == "depth/root":
            node.pop("parent", None)
            node["display"]["background"] = f"{MOD_ID}:textures/gui/advancement_background.png"
            node["display"]["frame"] = "task"
        write_json(os.path.join(DATA, "advancement", path + ".json"), node)


# ================================================================================= tags ==

def tags():
    pickaxe = [name for name in BLOCKS if name not in
               ("corrupted_growth", "bioluminescent_growth", "root_planks", "grave_dust")]
    write_json(os.path.join(DATA, "tags", "block", "mineable", "pickaxe.json"),
               {"values": [f"{MOD_ID}:{name}" for name in pickaxe]})
    write_json(os.path.join(DATA, "tags", "block", "mineable", "axe.json"),
               {"values": [f"{MOD_ID}:root_planks"]})
    write_json(os.path.join(DATA, "tags", "block", "mineable", "shovel.json"),
               {"values": [f"{MOD_ID}:grave_dust"]})
    write_json(os.path.join(DATA, "tags", "block", "mineable", "hoe.json"),
               {"values": [f"{MOD_ID}:corrupted_growth", f"{MOD_ID}:bioluminescent_growth"]})
    # Everything in the mod that must not be destroyed by an explosion in a story room.
    write_json(os.path.join(DATA, "tags", "block", "labyrinth_structure.json"),
               {"values": [f"{MOD_ID}:{name}" for name in
                           ("threshold_gate", "sealed_gate", "memory_altar", "inscription_stone")]})
    write_json(os.path.join(DATA, "tags", "item", "lamps.json"),
               {"values": [f"{MOD_ID}:lamplighter_lantern", f"{MOD_ID}:luminous_flask",
                           f"{MOD_ID}:lamp_oil"]})


# ================================================================================= lang ==

NAMES = {
    # blocks
    "forgotten_stone": "Forgotten Stone",
    "forgotten_bricks": "Forgotten Bricks",
    "cracked_forgotten_bricks": "Cracked Forgotten Bricks",
    "forgotten_pillar": "Forgotten Pillar",
    "drowned_stone": "Drowned Stone",
    "drowned_bricks": "Drowned Bricks",
    "barnacled_stone": "Barnacled Stone",
    "living_wall": "Living Wall",
    "veined_living_stone": "Veined Living Stone",
    "corrupted_growth": "Corrupted Growth",
    "memorial_stone": "Memorial Stone",
    "pale_memorial_tile": "Pale Memorial Tile",
    "tally_marks": "Tally Marks",
    "heart_stone": "Heart Stone",
    "heart_crystal_block": "Heart Crystal Block",
    "rusted_grate": "Rusted Grate",
    "root_planks": "Root Planks",
    "grave_dust": "Grave Dust",
    "pale_crystal": "Pale Crystal",
    "bioluminescent_growth": "Bioluminescent Growth",
    "lamp_glass": "Lamp Glass",
    "hollow_lantern": "Hollow Lantern",
    "memory_lamp": "Memory Lamp",
    "memory_altar": "Memory Altar",
    "memory_pedestal": "Memory Pedestal",
    "sealed_gate": "Sealed Gate",
    "inscription_stone": "Inscribed Stone",
    "whispering_stone": "Whispering Stone",
    "threshold_gate": "Threshold",
    "shrine_bricks": "Shrine Bricks",
    "shrine_lantern": "Shrine Lantern",
    # items
    "hollow_ember": "Hollow Ember",
    "pale_scale": "Pale Scale",
    "living_fiber": "Living Fibre",
    "bone_stitch": "Bone Stitch",
    "salt_of_hollowmere": "Salt of Hollowmere",
    "oiled_wick": "Oiled Wick",
    "lamp_oil": "Lamp Oil",
    "memory_shard": "Memory Shard",
    "seventh_seam_dust": "Dust of the Seventh Seam",
    "lamplighter_lantern": "Lamplighter's Lantern",
    "cracked_lens": "Cracked Lens",
    "lockpick": "Bent Lockpick",
    "lamplighters_journal": "Lamplighter's Journal",
    "rusted_key": "Rusted Key",
    "drowned_key": "Drowned Key",
    "heart_key": "Heart Key",
    "miner_shiv": "Miner's Shiv",
    "rusted_pickhammer": "Rusted Pickhammer",
    "lantern_chain": "Lantern Chain",
    "wickbow": "Wickbow",
    "glass_bolt": "Glass Bolt",
    "wax_bandage": "Wax Bandage",
    "bitterroot_tea": "Bitterroot Tea",
    "sweet_ash": "Sweet Ash",
    "luminous_flask": "Flask of Lamplight",
    "drowned_breath": "Breath of the Drowned",
    "memory_draught": "Memory Draught",
    "torn_journal_page": "Torn Journal Page",
    "unsent_letter": "Unsent Letter",
    "water_stained_photograph": "Water-Stained Photograph",
    "carved_token": "Carved Miner's Token",
    "broken_music_box": "Broken Music Box",
    "iver_whistle": "Iver's Tin Whistle",
    "lamplighters_band": "Lamplighter's Band",
    "heart_of_the_hollow": "Heart of the Hollow",
}

FLAVOUR = {
    "memory_shard": "A splinter of lamp glass with somebody's afternoon caught in it.",
    "torn_journal_page": "Somebody wrote this down in the dark, in a hurry.",
    "unsent_letter": "Folded four times, worn soft at the creases. Never posted.",
    "water_stained_photograph": "Two children, two pairs of boots, no faces.",
    "carved_token": "Number seven, scratched by hand. Never handed in.",
    "rusted_key": "Cut for one door, a long time ago.",
    "drowned_key": "The metal has gone the colour of deep water.",
    "heart_key": "It is warm when you are not looking at it.",
    "miner_shiv": "A flint edge and a stick. Enough to make somebody hesitate.",
    "rusted_pickhammer": "Heavy, unbalanced, and the only thing with a point on it.",
    "lantern_chain": "Lamplit links on a length of chain. It jingles before it lands.",
    "wickbow": "A bowstring of wick. It does not fire arrows.",
    "lamplighter_lantern": "A lamp is a promise, not a light.",
    "cracked_lens": "It shows you the ground you have already given your feet to.",
    "lockpick": "Bent, and proud of it.",
    "lamplighters_journal": "Every page she could still read, in her own hand.",
    "luminous_flask": "A small sun you can throw at the floor.",
    "bitterroot_tea": "Hot, bitter, and it makes the walls stop moving.",
    "sweet_ash": "Sweet and grey. You feel braver than you are.",
    "wax_bandage": "Waxed cloth. It smells faintly of somebody's kitchen.",
    "drowned_breath": "Tastes of river ice. Your lungs forget to complain.",
    "memory_draught": "Somebody else's calm, borrowed without asking.",
    "broken_music_box": "Four notes and a comb that will not turn.",
    "iver_whistle": "Two notes, both wrong, and the only voice that was ever kind.",
    "lamplighters_band": "Iron, worn shiny on the inside. It was somebody's comfort.",
    "heart_of_the_hollow": "The thing she could not give away, because it was the reason.",
    "hollow_ember": "Something that burned here before the stone cooled.",
    "pale_scale": "It is still faintly warm to the touch.",
    "living_fiber": "It moves when the light does.",
    "bone_stitch": "A needle of bone and a thread of something that was alive.",
    "salt_of_hollowmere": "The village was paid for out of the ground once.",
    "seventh_seam_dust": "Fine, grey, and it does not settle.",
    "oiled_wick": "Trimmed and ready. Trimming a wick is almost a prayer.",
    "lamp_oil": "Rendered from something that did not want to be oil.",
    "glass_bolt": "A sliver of glass that carries light down its whole length.",
}

ADVANCEMENT_NAMES = {}  # filled from content.ADVANCEMENTS


def lang():
    values = {}

    for name in BLOCKS:
        values[f"block.{MOD_ID}.{name}"] = NAMES.get(name, name.replace("_", " ").title())
    for name in ITEMS:
        values[f"item.{MOD_ID}.{name}"] = NAMES.get(name, name.replace("_", " ").title())
    for entity in ENTITIES:
        pretty = entity.replace("_", " ").title()
        values[f"entity.{MOD_ID}.{entity}"] = pretty
    values["entity.hollowlight.lampkeeper.name"] = "The Lamplighter"

    values.update({
        "itemGroup.hollowlight.main": "Hollowlight",
        "container.hollowlight.memory_altar": "Memory Altar",

        # keys and categories
        "category.hollowlight": "Hollowlight",
        "key.hollowlight.journal": "Open the Journal",
        "key.hollowlight.map": "Open the Lens",

        # chapters
        "hollowlight.chapter.surface": "Above",
        "hollowlight.chapter.halls": "I. Halls",
        "hollowlight.chapter.drowned": "II. Drowned",
        "hollowlight.chapter.living": "III. Living",
        "hollowlight.chapter.memorial": "IV. Memorial",
        "hollowlight.chapter.heart": "V. Heart",

        # layers
        "hollowlight.layer.1": "I — The Forgotten Halls",
        "hollowlight.layer.2": "II — The Drowned Corridors",
        "hollowlight.layer.3": "III — The Living Walls",
        "hollowlight.layer.4": "IV — The Memorial Depths",
        "hollowlight.layer.5": "V — The Heart",

        # lore categories
        "hollowlight.lore.category.journal": "Journal",
        "hollowlight.lore.category.letter": "Letters and Ledgers",
        "hollowlight.lore.category.inscription": "Inscriptions",
        "hollowlight.lore.category.room": "Rooms",
        "hollowlight.lore.category.item": "Objects",
        "hollowlight.lore.category.voice": "Voices",

        # journal
        "gui.hollowlight.journal.title": "The Lamplighter's Journal",
        "gui.hollowlight.fragments": "fragments",
        "gui.hollowlight.empty": "Nothing in this chapter yet.",
        "gui.hollowlight.undiscovered": "You have not found this yet. Somewhere below, it is still written.",
        "gui.hollowlight.close": "Close",
        "gui.hollowlight.map.title": "The Cracked Lens",
        "gui.hollowlight.map.surface": "Above ground",
        "gui.hollowlight.map.footer": "walked %s of %s cells",
        "gui.hollowlight.ending.title": "The Last Lamp",
        "gui.hollowlight.ending.body": "She is not asking the dark. She has never been asking the dark. Choose what the lamp does next.",
        "gui.hollowlight.ending.release": "Put the lamp out",
        "gui.hollowlight.ending.keep": "Take up the wick",
        "gui.hollowlight.ending.return": "Give the Heart a name",
        "gui.hollowlight.ending.wait": "Not yet",

        # tooltips
        "hollowlight.tooltip.read": "Right-click to read",
        "hollowlight.tooltip.journal_hint": "Filed in the journal",
        "hollowlight.tooltip.open_journal": "Right-click to open the journal",
        "hollowlight.tooltip.open_map": "Right-click to look through the lens",
        "hollowlight.tooltip.key_depth": "Cut for depth %s",
        "hollowlight.tooltip.sanity_gain": "Restores %s composure",
        "hollowlight.tooltip.sanity_loss": "Costs %s composure",
        "hollowlight.tooltip.flavour": "",

        # lantern
        "item.hollowlight.lamplighter_lantern.use": "Right-click to set it down or hold the flame",
        "item.hollowlight.lamplighter_lantern.oil": "Oil: %s",
        "item.hollowlight.lamplighter_lantern.empty": "The wick is dry. You need lamp oil.",
        "item.hollowlight.wickbow.empty": "No glass bolts.",
        "item.hollowlight.wickbow.ammo": "Fires glass bolts",
        "item.hollowlight.broken_music_box.plays": "Four notes, and a fifth one that is not there.",
        "item.hollowlight.iver_whistle.alone": "Nothing answers. Not yet.",

        # interaction feedback
        "block.hollowlight.memory_altar.hum": "The altar hums, very slowly, and waits.",
        "block.hollowlight.sealed_gate.locked": "The gate does not know you.",
        "block.hollowlight.sealed_gate.opens": "The gate remembers the key and steps aside.",
        "hollowlight.key.wrong_place": "The key turns in your hand and finds nothing to turn.",
        "hollowlight.key.refuses": "This lock was cut deeper than the key.",
        "hollowlight.lore.found": "Filed in the journal: %s",
        "hollowlight.lore.reread": "You have read this before. It says the same thing.",
        "hollowlight.journal.progress": "%s of %s fragments",
        "hollowlight.gate.sealed": "The threshold will not open yet. Something below is not finished.",
        "hollowlight.descend.1": "The air changes. Dust, and old lamp smoke.",
        "hollowlight.descend.2": "Your boots go quiet. There is water in the walls now.",
        "hollowlight.descend.3": "The stone is warm. It moves when you lean on it.",
        "hollowlight.descend.4": "A window. A curtain. A table set for three.",
        "hollowlight.descend.5": "You can hear a lamp burning. It is the biggest sound you have ever heard.",
        "hollowlight.ascend.surface": "You come up into daylight that does not suit you anymore.",

        # sanity
        "hollowlight.sanity.thin": "The corridors feel thinner than they did.",
        "hollowlight.sanity.low": "Something is walking behind you. Probably.",
        "hollowlight.sanity.recovering": "Your hands stop shaking, mostly.",

        # entities
        "hollowlight.weeping.notice": "Somewhere nearby, someone is crying.",
        "hollowlight.weeping.struck": "It does not fight back. It only cries harder.",
        "hollowlight.weeping.death": "You killed the only thing down here that never hurt anybody.",
        "hollowlight.weeping.spared": "You walk past. It stops crying for a moment, which is worse.",
        "hollowlight.lampkeeper.phase2": "The Lamplighter stops. She turns her head as if she has heard something upstairs.",
        "hollowlight.lampkeeper.phase3": "She stops asking. She starts taking.",
        "hollowlight.lampkeeper.death": "The lamp goes out of her hands, and for one breath she is only a woman in the dark.",

        # endings
        "hollowlight.ending.offer": "The Heart is open. Decide.",
        "hollowlight.ending.release.one": "You put your hand on the wick.",
        "hollowlight.ending.release.two": "She looks at you with eyes she gave away eleven years ago.",
        "hollowlight.ending.release.three": "She says the name correctly, once, to your face.",
        "hollowlight.ending.release.epilogue": "The lamps go out, upstream of her, like people leaving a room they were only waiting in. In the morning the shrine is full of daylight and the mine is only a mine.",
        "hollowlight.ending.keep.one": "You lift the wick out of her hands. It does not go out.",
        "hollowlight.ending.keep.two": "The labyrinth settles around you like a house.",
        "hollowlight.ending.keep.three": "Somewhere below, a voice that is almost yours begins to say a name.",
        "hollowlight.ending.keep.epilogue": "Above, the villagers notice the shrine lamp has been relit. They do not notice it burns differently now, nor that whoever keeps it never comes up for bread.",
        "hollowlight.ending.return.one": "You say his name into the Heart.",
        "hollowlight.ending.return.two": "It is the one thing the Heart cannot keep: something that was already being carried.",
        "hollowlight.ending.return.three": "The dust goes quiet. Somebody, somewhere, stops waiting.",
        "hollowlight.ending.return.epilogue": "You climb for a long time. It is lighter than going down, and at the ninth step there is bread. You eat it, because it is for you.",
        "hollowlight.ending.return.missing": "You would have to be carrying something of his for that to work.",
    })

    # item flavour and advancement text
    for name, text in FLAVOUR.items():
        values[f"item.{MOD_ID}.{name}.flavour"] = text
    for path, title, description, icon, parent, hidden in ADVANCEMENTS:
        key = path.replace("/", ".")
        values[f"advancement.{MOD_ID}.{key}.title"] = title
        values[f"advancement.{MOD_ID}.{key}.description"] = description

    # subtitles for every generated sound event
    sounds_path = os.path.join(ASSETS, "sounds.json")
    if os.path.exists(sounds_path):
        with open(sounds_path, encoding="utf-8") as handle:
            sounds = json.load(handle)
        for event, node in sounds.items():
            subtitle = node.get("subtitle")
            if subtitle and subtitle not in values:
                values[subtitle] = subtitle_text(event)

    write_json(os.path.join(ASSETS, "lang", "en_us.json"), dict(sorted(values.items())))
    return values


SUBTITLE_TEXT = {
    "ambience.halls": "Stone corridors breathe",
    "ambience.drowned": "Water moves in the walls",
    "ambience.living": "Something pulses nearby",
    "ambience.memorial": "A room remembers itself",
    "ambience.heart": "A lamp burns below",
    "ambience.safe": "A quiet, warm room",
    "ambient.drip": "Water drips",
    "ambient.rumble": "The deep stone shifts",
    "ambient.wind": "Air moves in a corridor",
    "ambient.stone_settle": "Stone settles",
    "ambient.whisper": "A whisper",
    "ambient.whisper_close": "Someone whispers a name",
    "ambient.breath": "Breathing that is not yours",
    "ambient.heartbeat": "Your heart beats",
    "ambient.bell": "A bell, far away",
    "entity.hollowed.idle": "The hollowed breathes",
    "entity.hollowed.hurt": "The hollowed is struck",
    "entity.hollowed.death": "The hollowed falls apart",
    "entity.hollowed.step": "Something walks",
    "entity.paperface.watch": "Pages turn",
    "entity.paperface.attack": "Pages tear",
    "entity.stitcher.ambient": "Thread pulls tight",
    "entity.stitcher.attack": "Something stitches",
    "entity.weeping.cry": "Someone weeps",
    "entity.weeping.soft": "Quiet crying",
    "entity.choir.sing": "The drowned recite a list of names",
    "entity.moth.flutter": "Wings",
    "entity.echo.voice": "An echo answers",
    "entity.lampkeeper.roar": "The Lamplighter raises her voice",
    "entity.lampkeeper.phase": "The Lamplighter remembers something",
    "entity.lampkeeper.death": "The last lamp gutters",
    "block.gate_open": "Gate opens",
    "block.gate_locked": "Lock refuses",
    "block.altar_activate": "Altar hums",
    "block.trap_trigger": "Trap springs",
    "item.page_turn": "Page turns",
    "item.key_insert": "Key turns",
    "item.lantern_light": "Lamp catches",
    "item.lantern_empty": "Wick gutters out",
    "item.lore_discover": "A fragment is remembered",
    "item.memory_return": "Memory returns",
    "horror.stinger_soft": "The air thins",
    "horror.stinger_deep": "Something is very close",
    "music.halls": "Hollowlight",
    "music.drowned": "Hollowlight",
    "music.living": "Hollowlight",
    "music.memorial": "Hollowlight",
    "music.heart": "Hollowlight",
    "music.boss": "The Lamplighter",
    "music.ending": "Hollowlight",
}


def subtitle_text(event):
    return SUBTITLE_TEXT.get(event, event.replace(".", " ").title())


if __name__ == "__main__":
    worldgen()
    recipes()
    loot_tables()
    advancements()
    tags()
    values = lang()
    print(f"lang keys: {len(values)}")
