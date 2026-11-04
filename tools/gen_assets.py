"""Item model definitions (1.21.4+), particle definitions and the advancement background."""
from __future__ import annotations

import json
import os

import numpy as np
from PIL import Image

import artlib
from content import BLOCKS, CUSTOM, ITEMS

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "hollowlight")

HANDHELD = {"miner_shiv", "rusted_pickhammer", "wickbow", "glass_bolt", "lantern_chain"}
WEARABLE = {"lamplighters_journal"}
SPAWN_LIKE = set()


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        json.dump(data, handle, indent=2, ensure_ascii=False)
        handle.write("\n")


def item_definitions():
    """1.21.4 moved item models to `assets/<ns>/items/<id>.json` (item model definitions)."""
    count = 0
    for name in BLOCKS:
        write_json(os.path.join(ASSETS, "items", name + ".json"),
                   {"model": {"type": "minecraft:model", "model": f"hollowlight:block/{name}"}})
        count += 1
    for name in ITEMS:
        write_json(os.path.join(ASSETS, "items", name + ".json"),
                   {"model": {"type": "minecraft:model", "model": f"hollowlight:item/{name}"}})
        count += 1
    return count


def particle_definitions():
    for name in ("dust", "mote", "smoke", "glint"):
        write_json(os.path.join(ASSETS, "particles", name + ".json"),
                   {"textures": ["hollowlight:particle/mote"]})
    return 4


def advancement_background():
    """A 16x16 dark stone tile, tiled by vanilla behind the advancement tree."""
    rng = np.random.default_rng(4242)
    tile = 16
    image = artlib.new_image((tile, tile), (26, 24, 27, 255))
    pixels = image.load()
    for y in range(tile):
        for x in range(tile):
            jitter = int(rng.integers(-6, 7))
            base = (30, 27, 30)
            pixels[x, y] = (max(0, base[0] + jitter), max(0, base[1] + jitter),
                            max(0, base[2] + jitter), 255)
    # faint mortar lines so the tiling reads as masonry rather than noise
    for x in range(tile):
        pixels[x, 0] = (20, 18, 21, 255)
    for y in range(tile):
        pixels[0, y] = (20, 18, 21, 255)
    for _ in range(6):
        x, y = int(rng.integers(1, tile - 1)), int(rng.integers(1, tile - 1))
        pixels[x, y] = (38, 34, 38, 255)
    os.makedirs(os.path.join(ASSETS, "textures", "gui"), exist_ok=True)
    image.save(os.path.join(ASSETS, "textures", "gui", "advancement_background.png"))


if __name__ == "__main__":
    print("item definitions:", item_definitions())
    print("particle definitions:", particle_definitions())
    advancement_background()
    print("advancement background written")
