"""Blockstates, block models and item models."""
from __future__ import annotations

import json
import os

from content import BLOCKS, CROSSES, CUBE_ALL, CUSTOM, ITEMS, MOD_ID, PILLARS

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", MOD_ID)
BLOCKSTATES = os.path.join(ROOT, "blockstates")
BLOCK_MODELS = os.path.join(ROOT, "models", "block")
ITEM_MODELS = os.path.join(ROOT, "models", "item")

for d in (BLOCKSTATES, BLOCK_MODELS, ITEM_MODELS):
    os.makedirs(d, exist_ok=True)


def write(path, data):
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2)
        handle.write("\n")


def tex(name):
    return f"{MOD_ID}:block/{name}"


def simple_block(name, parent="minecraft:block/cube_all", texture=None, extra=None):
    model = {"parent": parent, "textures": {"all": texture or tex(name)} if parent.endswith("cube_all") else {}}
    if extra:
        model["textures"].update(extra)
    write(os.path.join(BLOCK_MODELS, name + ".json"), model)


def blockstates():
    for name in CUBE_ALL:
        simple_block(name)
        write(os.path.join(BLOCKSTATES, name + ".json"),
              {"variants": {"": {"model": f"{MOD_ID}:block/{name}"}}})

    for name in PILLARS:
        write(os.path.join(BLOCK_MODELS, name + ".json"), {
            "parent": "minecraft:block/cube_column",
            "textures": {"end": tex(name + "_top"), "side": tex(name + "_side")},
        })
        write(os.path.join(BLOCKSTATES, name + ".json"), {
            "variants": {
                "axis=y": {"model": f"{MOD_ID}:block/{name}"},
                "axis=z": {"model": f"{MOD_ID}:block/{name}", "x": 90},
                "axis=x": {"model": f"{MOD_ID}:block/{name}", "x": 90, "y": 90},
            }
        })

    for name in CROSSES:
        write(os.path.join(BLOCK_MODELS, name + ".json"), {
            "parent": "minecraft:block/cross",
            "textures": {"cross": tex(name)},
        })
        write(os.path.join(BLOCKSTATES, name + ".json"),
              {"variants": {"": {"model": f"{MOD_ID}:block/{name}"}}})

    # ---- the memory altar: a low stone table with a warm top ---------------------------
    write(os.path.join(BLOCK_MODELS, "memory_altar.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {
            "top": tex("memory_altar_top"),
            "side": tex("memory_altar_side"),
            "bottom": tex("memory_altar_side"),
        },
    })
    write(os.path.join(BLOCKSTATES, "memory_altar.json"),
          {"variants": {"": {"model": f"{MOD_ID}:block/memory_altar"}}})

    for name in ("inscription_stone",):
        write(os.path.join(BLOCK_MODELS, name + ".json"), {
            "parent": "minecraft:block/cube_all", "textures": {"all": tex(name)}})
        write(os.path.join(BLOCKSTATES, name + ".json"),
              {"variants": {"": {"model": f"{MOD_ID}:block/{name}"}}})

    # ---- threshold gate: a dark frame with a light in it --------------------------------
    write(os.path.join(BLOCK_MODELS, "threshold_gate.json"), {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": tex("threshold_gate")},
    })
    write(os.path.join(BLOCKSTATES, "threshold_gate.json"), {
        "variants": {f"link={value}": {"model": f"{MOD_ID}:block/threshold_gate"} for value in range(6)}
    })

    # ---- sealed gate: a door of salt-iron that opens into an empty frame ----------------
    write(os.path.join(BLOCK_MODELS, "sealed_gate.json"), {
        "parent": "minecraft:block/cube_all", "textures": {"all": tex("sealed_gate")}})
    frame = {
        "textures": {
            "particle": tex("sealed_gate"),
            "frame": tex("sealed_gate"),
        },
        "elements": [
            {"from": [0, 0, 0], "to": [16, 3, 16], "faces": {"all": {"texture": "#frame"}}},
            {"from": [0, 13, 0], "to": [16, 16, 16], "faces": {"all": {"texture": "#frame"}}},
            {"from": [0, 3, 0], "to": [3, 13, 16], "faces": {"all": {"texture": "#frame"}}},
            {"from": [13, 3, 0], "to": [16, 13, 16], "faces": {"all": {"texture": "#frame"}}},
        ],
    }
    write(os.path.join(BLOCK_MODELS, "sealed_gate_open.json"), frame)
    write(os.path.join(BLOCKSTATES, "sealed_gate.json"), {
        "variants": {
            "open=false": {"model": f"{MOD_ID}:block/sealed_gate"},
            "open=true": {"model": f"{MOD_ID}:block/sealed_gate_open"},
        }
    })


def item_models():
    for name in ITEMS:
        write(os.path.join(ITEM_MODELS, name + ".json"), {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"{MOD_ID}:item/{name}"},
        })
    # block items reuse the block model, except cross-shaped growth which needs the sprite
    for name in BLOCKS:
        if name in CROSSES:
            write(os.path.join(ITEM_MODELS, name + ".json"), {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": tex(name)},
            })
        else:
            write(os.path.join(ITEM_MODELS, name + ".json"),
                  {"parent": f"{MOD_ID}:block/{name}"})


if __name__ == "__main__":
    blockstates()
    item_models()
    print("models written")
