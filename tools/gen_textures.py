"""Generates every block, item, GUI and particle texture in the mod."""
from __future__ import annotations

import math
import os
import random

from PIL import Image, ImageDraw, ImageFilter

from artlib import (OUTLINE, brickwork, crystal, draw_crack, edge_darken, fill_rect, glow_block,
                    gradient_rect, item_canvas, item_noise, metal_rust, mix, new_image, noise_fill,
                    organic, outline_mask, planks, shade, soft_dot, speckle, stone, tiles)

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hollowlight")
BLOCK_DIR = os.path.join(ROOT, "textures", "block")
ITEM_DIR = os.path.join(ROOT, "textures", "item")
GUI_DIR = os.path.join(ROOT, "textures", "gui")
PARTICLE_DIR = os.path.join(ROOT, "textures", "particle")

for d in (BLOCK_DIR, ITEM_DIR, GUI_DIR, PARTICLE_DIR):
    os.makedirs(d, exist_ok=True)


def save(img, directory, name):
    img.save(os.path.join(directory, name + ".png"))


def rng_for(seed):
    return random.Random(hash(seed) & 0xFFFFFFFF)


# ============================================================== block textures ==========

PALETTE = {
    "halls": dict(base=(104, 100, 93), brick=(96, 91, 84), mortar=(66, 62, 57), accent=(120, 112, 98)),
    "drowned": dict(base=(62, 84, 86), brick=(58, 78, 82), mortar=(38, 52, 56), accent=(86, 132, 124)),
    "living": dict(base=(92, 46, 48), brick=(84, 42, 44), mortar=(52, 26, 28), accent=(158, 74, 66)),
    "memorial": dict(base=(150, 142, 122), brick=(142, 133, 114), mortar=(104, 96, 82), accent=(176, 166, 142)),
    "heart": dict(base=(70, 34, 44), brick=(62, 30, 40), mortar=(40, 18, 26), accent=(148, 52, 66)),
}


def block_textures():
    p = PALETTE
    specs = {}

    specs["forgotten_stone"] = stone("forgotten_stone", p["halls"]["base"], rng_for("fs"), cracks=2)
    specs["forgotten_bricks"] = brickwork("forgotten_bricks", p["halls"]["brick"], p["halls"]["mortar"], rng_for("fb"))
    cracked = brickwork("cracked", p["halls"]["brick"], p["halls"]["mortar"], rng_for("cfb"))
    for _ in range(6):
        draw_crack(cracked, rng_for("x" + str(_)), shade(p["halls"]["brick"], -40), steps=6)
    specs["cracked_forgotten_bricks"] = cracked

    pillar_side = stone("pillar_side", shade(p["halls"]["base"], 6), rng_for("ps"), cracks=1)
    d = ImageDraw.Draw(pillar_side)
    d.line([(3, 0), (3, 15)], fill=(*shade(p["halls"]["base"], -26), 255))
    d.line([(12, 0), (12, 15)], fill=(*shade(p["halls"]["base"], -18), 255))
    specs["forgotten_pillar_side"] = pillar_side
    specs["forgotten_pillar_top"] = tiles(shade(p["halls"]["base"], 10), p["halls"]["mortar"], rng_for("pt"), size=5)

    specs["drowned_stone"] = stone("drowned_stone", p["drowned"]["base"], rng_for("ds"), cracks=2,
                                  veins=p["drowned"]["accent"])
    specs["drowned_bricks"] = brickwork("drowned_bricks", p["drowned"]["brick"], p["drowned"]["mortar"], rng_for("db"))
    barnacled = stone("barnacled", p["drowned"]["base"], rng_for("bs"), cracks=1)
    rng = rng_for("barnacles")
    dd = ImageDraw.Draw(barnacled)
    for _ in range(8):
        x, y = rng.randrange(14) + 1, rng.randrange(14) + 1
        dd.ellipse([x - 1, y - 1, x + 1, y + 1], fill=(*shade(p["drowned"]["accent"], 30), 255))
    specs["barnacled_stone"] = barnacled

    specs["living_wall"] = organic(p["living"]["base"], p["living"]["accent"], rng_for("lw"), veins=9)
    specs["veined_living_stone"] = organic(shade(p["living"]["base"], -10),
                                          shade(p["living"]["accent"], 24), rng_for("vls"), veins=12)
    specs["corrupted_growth"] = None  # cross sprite, built below

    specs["memorial_stone"] = stone("memorial_stone", p["memorial"]["base"], rng_for("ms"), cracks=2)
    specs["pale_memorial_tile"] = tiles(p["memorial"]["base"], p["memorial"]["mortar"], rng_for("pmt"), size=4)
    specs["tally_marks"] = None

    specs["heart_stone"] = stone("heart_stone", p["heart"]["base"], rng_for("hs"), cracks=3,
                                 veins=p["heart"]["accent"])
    specs["heart_crystal_block"] = crystal(p["heart"]["base"], (208, 122, 122), rng_for("hcb"))

    specs["rusted_grate"] = metal_rust((84, 78, 72), (128, 82, 44), rng_for("rg"))
    specs["root_planks"] = planks((92, 68, 46), rng_for("rp"))
    specs["grave_dust"] = None
    specs["pale_crystal"] = crystal((118, 116, 138), (170, 186, 208), rng_for("pc"))
    specs["bioluminescent_growth"] = None
    specs["lamp_glass"] = glow_block((182, 152, 88), (252, 226, 160), rng_for("lg"), frame=True)
    specs["hollow_lantern"] = None
    specs["memory_lamp"] = glow_block((196, 158, 92), (255, 236, 186), rng_for("ml"), frame=True)
    specs["memory_altar_side"] = brickwork("altar_side", (66, 74, 62), (40, 46, 38), rng_for("mas"))
    specs["memory_altar_top"] = glow_block((62, 70, 58), (150, 176, 120), rng_for("mat"), frame=True)
    specs["memory_pedestal"] = tiles((86, 84, 78), (58, 56, 52), rng_for("mp"), size=5)
    specs["sealed_gate"] = metal_rust((70, 68, 66), (96, 74, 52), rng_for("sg"))
    specs["inscription_stone"] = None
    specs["whispering_stone"] = organic((58, 56, 60), (104, 96, 120), rng_for("ws"), veins=6)
    specs["threshold_gate"] = None
    specs["shrine_bricks"] = brickwork("shrine_bricks", (116, 108, 96), (76, 70, 62), rng_for("sb"))
    specs["shrine_lantern"] = glow_block((176, 148, 92), (255, 232, 172), rng_for("sl"), frame=True)

    for name, image in specs.items():
        if image is not None:
            save(image, BLOCK_DIR, name)

    # ---------------------------------------------------------------- cross sprites -----
    growth = new_image()
    rng = rng_for("growth")
    for i in range(9):
        x = 2 + i
        y_top = 12 - int(6 * math.sin(i / 9 * math.pi) + rng.randint(0, 2))
        for y in range(y_top, 15):
            t = (y - y_top) / max(1, 15 - y_top)
            color = mix((20, 60, 52), (120, 226, 196), t)
            growth.putpixel((x, y), (*color, 255))
    for _ in range(18):
        growth.putpixel((rng.randrange(16), rng.randrange(10, 16)), (170, 250, 220, 255))
    save(edge_darken(growth, 0), BLOCK_DIR, "bioluminescent_growth")

    corrupt = new_image()
    rng = rng_for("corrupt")
    for i in range(10):
        x1, y1 = rng.randrange(16), rng.randrange(14, 16)
        x2 = max(0, min(15, x1 + rng.randint(-6, 6)))
        y2 = rng.randrange(2, 12)
        d = ImageDraw.Draw(corrupt)
        d.line([(x1, y1), (x2, y2)], fill=(*mix((40, 16, 18), (150, 40, 44), rng.random()), 255), width=1)
    for _ in range(14):
        corrupt.putpixel((rng.randrange(16), rng.randrange(16)), (196, 74, 70, 255))
    save(corrupt, BLOCK_DIR, "corrupted_growth")

    # ---------------------------------------------------------------- grade / dust -----
    dust = new_image()
    noise_fill(dust, (150, 144, 128), rng_for("dust"), 12)
    speckle(dust, rng_for("dust2"), 60, (110, 104, 92))
    save(edge_darken(dust, 14), BLOCK_DIR, "grave_dust")

    # ---------------------------------------------------------------- lantern ----------
    lantern = new_image()
    rng = rng_for("lantern")
    d = ImageDraw.Draw(lantern)
    d.rectangle([3, 1, 12, 14], fill=(52, 48, 44, 255))
    d.rectangle([4, 3, 11, 12], fill=(0, 0, 0, 0))
    for y in range(4, 12):
        for x in range(5, 11):
            t = 1.0 - abs(y - 8) / 6.0
            lantern.putpixel((x, y), (*mix((150, 96, 40), (255, 224, 150), t), 255))
    d.rectangle([3, 1, 12, 14], outline=(30, 28, 26, 255))
    d.line([(7, 0), (8, 0)], fill=(30, 28, 26, 255))
    save(lantern, BLOCK_DIR, "hollow_lantern")

    # ---------------------------------------------------------------- inscriptions -----
    for name, text_seed in (("inscription_stone", "insc"), ("tally_marks", "tally")):
        img = stone(name, (128, 120, 106), rng_for(text_seed), cracks=1)
        d = ImageDraw.Draw(img)
        rng = rng_for(text_seed + "lines")
        if name == "inscription_stone":
            for line in range(3):
                y = 5 + line * 3
                x = 3
                while x < 12:
                    width = rng.randint(1, 3)
                    d.line([(x, y), (x + width, y)], fill=(58, 52, 46, 255))
                    x += width + 2
        else:
            for group in range(3):
                for stroke in range(4):
                    x = 3 + group * 4 + stroke
                    d.line([(x, 4), (x, 11)], fill=(52, 46, 40, 255))
                d.line([(3 + group * 4 - 1, 7 + (group % 2)), (6 + group * 4, 5)], fill=(52, 46, 40, 255))
        save(img, BLOCK_DIR, name)

    # ---------------------------------------------------------------- threshold gate ----
    gate = new_image()
    for y in range(16):
        for x in range(16):
            distance = math.hypot(x - 7.5, y - 7.5)
            t = max(0.0, 1.0 - distance / 8.0)
            gate.putpixel((x, y), (*mix((26, 18, 34), (186, 132, 214), t * 0.95), 235))
    d = ImageDraw.Draw(gate)
    d.ellipse([3, 3, 12, 12], outline=(214, 176, 232, 255))
    d.ellipse([5, 5, 10, 10], outline=(150, 110, 176, 200))
    save(gate, BLOCK_DIR, "threshold_gate")


# =============================================================== item textures ==========

def _finish(img, rng, strength=8):
    item_noise(img, rng, strength)
    return outline_mask(img)


def tex_lantern(rng, lit=True):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.line([(7, 1), (8, 1)], fill=(70, 64, 58, 255))
    d.rectangle([4, 2, 11, 4], fill=(96, 88, 78, 255))
    d.rectangle([3, 14, 12, 14], fill=(84, 76, 68, 255))
    for y in range(5, 14):
        for x in range(5, 11):
            t = 1.0 - abs(y - 9) / 5.0
            colour = mix((120, 74, 30), (255, 226, 148), t) if lit else (60, 58, 56)
            img.putpixel((x, y), (*colour, 255))
    d.rectangle([4, 5, 4, 13], fill=(120, 110, 96, 255))
    d.rectangle([11, 5, 11, 13], fill=(120, 110, 96, 255))
    return _finish(img, rng)


def tex_lens(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 12, 12], fill=(150, 176, 190, 235), outline=(90, 104, 116, 255))
    d.ellipse([4, 4, 10, 10], fill=(198, 222, 232, 210))
    d.line([(7, 3), (9, 9)], fill=(70, 80, 92, 255))
    d.line([(5, 8), (10, 5)], fill=(70, 80, 92, 255))
    d.line([(12, 12), (15, 15)], fill=(86, 72, 52, 255))
    return _finish(img, rng)


def tex_key(rng, color=(168, 138, 70)):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 7, 7], outline=(*color, 255))
    d.line([(6, 6), (13, 13)], fill=(*color, 255))
    d.line([(10, 10), (12, 13)], fill=(*color, 255))
    d.line([(12, 12), (14, 12)], fill=(*color, 255))
    d.line([(3, 3), (5, 3)], fill=(*shade(color, 60), 255))
    return _finish(img, rng)


def tex_book(rng, cover=(96, 62, 44), pages=(206, 194, 166)):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 1, 12, 14], fill=(*cover, 255))
    d.rectangle([4, 2, 11, 13], fill=(*pages, 255))
    d.line([(7, 2), (7, 13)], fill=(*shade(pages, -40), 255))
    d.rectangle([3, 1, 12, 14], outline=(*shade(cover, -30), 255))
    d.line([(4, 4), (6, 4)], fill=(*shade(pages, -50), 255))
    d.line([(8, 4), (10, 4)], fill=(*shade(pages, -50), 255))
    d.line([(4, 6), (6, 6)], fill=(*shade(pages, -50), 255))
    return _finish(img, rng)


def tex_pages(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    for offset, tone in ((0, (206, 198, 176)), (2, (188, 178, 154)), (4, (170, 160, 138))):
        d.polygon([(3 + offset, 2), (12, 1 + offset), (13, 13), (3 + offset, 14)],
                  fill=(*tone, 255), outline=(*shade(tone, -50), 255))
    for line in range(4):
        d.line([(6, 4 + line * 2), (11, 4 + line * 2)], fill=(120, 110, 92, 255))
    d.line([(11, 1), (13, 13)], fill=(120, 106, 86, 255))
    return _finish(img, rng)


def tex_letter(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([1, 4, 14, 12], fill=(212, 202, 180, 255), outline=(140, 130, 108, 255))
    d.line([(1, 4), (7, 9)], fill=(150, 138, 116, 255))
    d.line([(14, 4), (7, 9)], fill=(150, 138, 116, 255))
    d.line([(1, 12), (5, 8)], fill=(150, 138, 116, 255))
    d.line([(14, 12), (9, 8)], fill=(150, 138, 116, 255))
    d.ellipse([6, 6, 8, 8], fill=(158, 62, 56, 255))
    return _finish(img, rng)


def tex_photograph(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 13], fill=(228, 222, 204, 255), outline=(150, 144, 128, 255))
    d.rectangle([3, 3, 12, 9], fill=(126, 128, 122, 255))
    d.rectangle([3, 10, 12, 12], fill=(168, 160, 140, 255))
    # two figures whose faces have run
    d.rectangle([5, 5, 6, 9], fill=(196, 192, 176, 255))
    d.rectangle([8, 6, 9, 9], fill=(184, 180, 166, 255))
    d.ellipse([4, 4, 7, 6], fill=(206, 200, 182, 255))
    d.ellipse([8, 5, 10, 7], fill=(198, 192, 176, 255))
    for _ in range(6):
        d.point((rng.randint(2, 13), rng.randint(8, 12)), fill=(108, 108, 100, 255))
    return _finish(img, rng)


def tex_token(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 2, 13, 13], fill=(196, 186, 158, 255), outline=(126, 118, 96, 255))
    d.ellipse([4, 4, 11, 11], outline=(150, 140, 114, 255))
    d.line([(6, 7), (10, 7)], fill=(96, 88, 70, 255))
    d.line([(8, 5), (8, 10)], fill=(96, 88, 70, 255))
    return _finish(img, rng)


def tex_whistle(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 6, 12, 10], fill=(158, 158, 164, 255), outline=(96, 96, 104, 255))
    d.rectangle([3, 6, 5, 10], fill=(126, 126, 132, 255))
    d.ellipse([9, 5, 13, 9], fill=(178, 178, 184, 255), outline=(96, 96, 104, 255))
    d.point((10, 7), fill=(40, 40, 44, 255))
    d.line([(2, 8), (3, 8)], fill=(96, 96, 104, 255))
    return _finish(img, rng)


def tex_music_box(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([2, 4, 13, 12], fill=(122, 82, 52, 255), outline=(74, 48, 30, 255))
    d.rectangle([2, 4, 13, 6], fill=(146, 104, 66, 255))
    d.rectangle([5, 8, 10, 9], fill=(198, 176, 118, 255))
    d.line([(6, 8), (6, 9)], fill=(90, 70, 40, 255))
    d.line([(8, 8), (8, 9)], fill=(90, 70, 40, 255))
    d.point((12, 3), fill=(212, 190, 130, 255))
    return _finish(img, rng)


def tex_pickhammer(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.line([(4, 14), (11, 4)], fill=(96, 74, 48, 255), width=2)
    d.polygon([(8, 2), (14, 4), (13, 8), (7, 6)], fill=(104, 100, 96, 255))
    d.line([(8, 2), (14, 4)], fill=(146, 142, 136, 255))
    d.line([(9, 5), (12, 6)], fill=(72, 68, 64, 255))
    return _finish(img, rng)


def tex_shiv(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(3, 13), (11, 3), (13, 4), (5, 14)], fill=(158, 160, 166, 255))
    d.line([(11, 3), (13, 4)], fill=(206, 208, 214, 255))
    d.line([(3, 13), (6, 14)], fill=(96, 76, 50, 255))
    d.point((5, 11), fill=(120, 122, 128, 255))
    return _finish(img, rng)


def tex_chain(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    for i in range(5):
        x = 2 + i * 2
        y = 12 - i * 2
        d.ellipse([x, y, x + 3, y + 3], outline=(128, 124, 120, 255))
    d.rectangle([2, 2, 5, 5], fill=(176, 150, 84, 255), outline=(110, 92, 50, 255))
    d.point((3, 3), fill=(255, 236, 176, 255))
    return _finish(img, rng)


def tex_bow(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.arc([1, 1, 14, 14], start=300, end=120, fill=(190, 182, 160, 255), width=1)
    d.line([(12, 3), (12, 12)], fill=(238, 232, 200, 255))
    d.line([(12, 7), (4, 7)], fill=(226, 200, 120, 255))
    d.point((3, 7), fill=(255, 244, 190, 255))
    return _finish(img, rng)


def tex_bolt(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.line([(2, 14), (13, 3)], fill=(198, 214, 226, 255), width=1)
    d.polygon([(13, 3), (11, 6), (9, 4)], fill=(226, 240, 250, 255))
    d.line([(2, 14), (4, 12)], fill=(150, 120, 88, 255))
    d.point((6, 10), fill=(255, 255, 255, 255))
    return _finish(img, rng)


def tex_flask(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([6, 2, 9, 4], fill=(140, 132, 120, 255))
    d.polygon([(5, 5), (10, 5), (12, 13), (3, 13)], fill=(210, 224, 232, 150), outline=(120, 130, 138, 255))
    d.polygon([(4, 9), (11, 9), (12, 13), (3, 13)], fill=(255, 226, 150, 235))
    d.point((7, 11), fill=(255, 250, 220, 255))
    return _finish(img, rng)


def tex_bottle(rng, liquid=(92, 156, 156)):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([6, 1, 9, 4], fill=(120, 116, 108, 255))
    d.rectangle([4, 4, 11, 14], fill=(206, 216, 216, 140), outline=(112, 120, 120, 255))
    d.rectangle([5, 8, 10, 13], fill=(*liquid, 235))
    d.point((6, 9), fill=(220, 240, 240, 255))
    return _finish(img, rng)


def tex_cup(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([3, 6, 11, 12], fill=(196, 188, 170, 255), outline=(120, 114, 100, 255))
    d.ellipse([3, 4, 11, 8], fill=(120, 88, 52, 255))
    d.arc([11, 6, 15, 11], start=270, end=90, fill=(150, 142, 126, 255))
    d.line([(5, 13), (9, 13)], fill=(120, 114, 100, 255))
    d.point((8, 3), fill=(210, 200, 180, 255))
    return _finish(img, rng)


def tex_pile(rng, color, sparkle=None):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(2, 13), (13, 13), (11, 9), (5, 9)], fill=(*color, 255))
    d.polygon([(5, 9), (11, 9), (9, 7), (6, 7)], fill=(*shade(color, 24), 255))
    if sparkle:
        for _ in range(5):
            d.point((rng.randint(4, 11), rng.randint(8, 12)), fill=(*sparkle, 255))
    return _finish(img, rng)


def tex_shard(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.polygon([(7, 1), (12, 7), (9, 14), (4, 8)], fill=(186, 202, 216, 235), outline=(120, 134, 148, 255))
    d.line([(7, 2), (8, 12)], fill=(238, 248, 255, 255))
    d.line([(5, 8), (11, 7)], fill=(160, 176, 190, 255))
    return _finish(img, rng)


def tex_ember(rng):
    img = item_canvas()
    for y in range(3, 13):
        for x in range(3, 13):
            distance = math.hypot(x - 7.5, y - 7.5)
            if distance < 4.6:
                t = max(0.0, 1.0 - distance / 4.6)
                img.putpixel((x, y), (*mix((92, 34, 24), (255, 156, 74), t), 255))
    d = ImageDraw.Draw(img)
    d.point((6, 6), fill=(255, 232, 170, 255))
    return _finish(img, rng, 12)


def tex_scale(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.arc([3, 2, 13, 12], start=200, end=340, fill=(196, 216, 214, 255), width=2)
    d.arc([5, 5, 11, 10], start=200, end=340, fill=(156, 186, 186, 255))
    d.line([(8, 11), (8, 14)], fill=(140, 164, 164, 255))
    return _finish(img, rng)


def tex_fiber(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    for offset in range(3):
        d.line([(3 + offset, 13), (11 - offset, 2 + offset)], fill=(150, 74, 70, 255))
    for _ in range(6):
        d.point((rng.randint(4, 11), rng.randint(3, 12)), fill=(190, 108, 96, 255))
    return _finish(img, rng)


def tex_stitch(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.line([(3, 13), (11, 4)], fill=(212, 206, 186, 255), width=1)
    d.point((11, 3), fill=(240, 236, 222, 255))
    d.line([(4, 11), (8, 13)], fill=(158, 62, 58, 255))
    return _finish(img, rng)


def tex_wick(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    for i in range(3):
        d.arc([2 + i, 2 + i * 3, 13 - i, 11 + i], start=0, end=180, fill=(178, 154, 106, 255))
    d.point((8, 3), fill=(255, 226, 160, 255))
    return _finish(img, rng)


def tex_canister(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.rectangle([4, 4, 11, 14], fill=(126, 118, 104, 255), outline=(76, 70, 62, 255))
    d.rectangle([6, 2, 9, 4], fill=(96, 90, 80, 255))
    d.rectangle([6, 7, 9, 11], fill=(196, 152, 76, 255))
    d.point((7, 8), fill=(236, 206, 140, 255))
    return _finish(img, rng)


def tex_bandage(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([3, 4, 12, 13], fill=(214, 206, 186, 255), outline=(150, 142, 124, 255))
    d.ellipse([6, 7, 9, 10], fill=(178, 170, 152, 255))
    d.line([(3, 6), (12, 11)], fill=(188, 178, 158, 255))
    return _finish(img, rng)


def tex_ring(rng):
    img = item_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([2, 5, 13, 14], outline=(150, 146, 140, 255), width=2)
    d.arc([4, 7, 11, 12], start=200, end=340, fill=(198, 194, 186, 255))
    d.rectangle([7, 2, 9, 3], fill=(168, 164, 158, 255))
    return _finish(img, rng)


def tex_heart(rng):
    img = item_canvas()
    for y in range(16):
        for x in range(16):
            nx = (x - 7.5) / 5.0
            ny = (y - 8.0) / 5.2
            value = nx * nx + ny * ny - 0.6
            if value < 0:
                t = max(0.0, min(1.0, 1.0 + value))
                img.putpixel((x, y), (*mix((92, 26, 36), (226, 106, 96), t), 255))
    d = ImageDraw.Draw(img)
    d.point((6, 5), fill=(255, 214, 190, 255))
    return _finish(img, rng, 6)


def tex_band(rng):  # unused placeholder guard
    return tex_ring(rng)


ITEM_TEXTURES = {
    "lamplighter_lantern": lambda r: tex_lantern(r),
    "cracked_lens": tex_lens,
    "lockpick": lambda r: tex_stitch(r),
    "lamplighters_journal": lambda r: tex_book(r, (74, 52, 40), (208, 198, 172)),
    "rusted_key": lambda r: tex_key(r, (152, 118, 66)),
    "drowned_key": lambda r: tex_key(r, (118, 148, 156)),
    "heart_key": lambda r: tex_key(r, (156, 62, 66)),
    "miner_shiv": tex_shiv,
    "rusted_pickhammer": tex_pickhammer,
    "lantern_chain": tex_chain,
    "wickbow": tex_bow,
    "glass_bolt": tex_bolt,
    "hollow_ember": tex_ember,
    "pale_scale": tex_scale,
    "living_fiber": tex_fiber,
    "bone_stitch": tex_stitch,
    "salt_of_hollowmere": lambda r: tex_pile(r, (216, 214, 206), (238, 238, 232)),
    "seventh_seam_dust": lambda r: tex_pile(r, (118, 92, 74), (172, 140, 104)),
    "oiled_wick": tex_wick,
    "lamp_oil": tex_canister,
    "memory_shard": tex_shard,
    "wax_bandage": tex_bandage,
    "bitterroot_tea": tex_cup,
    "sweet_ash": lambda r: tex_pile(r, (96, 92, 88), (176, 172, 166)),
    "luminous_flask": tex_flask,
    "drowned_breath": lambda r: tex_bottle(r, (86, 148, 152)),
    "memory_draught": lambda r: tex_bottle(r, (150, 106, 176)),
    "torn_journal_page": tex_pages,
    "unsent_letter": tex_letter,
    "water_stained_photograph": tex_photograph,
    "carved_token": tex_token,
    "broken_music_box": tex_music_box,
    "iver_whistle": tex_whistle,
    "lamplighters_band": tex_ring,
    "heart_of_the_hollow": tex_heart,
}


def item_textures():
    rng = random.Random(4321)
    for name, factory in ITEM_TEXTURES.items():
        save(factory(rng), ITEM_DIR, name)


# ===================================================================== GUI textures ======

def gui_textures():
    # ---- journal -----------------------------------------------------------------------
    img = Image.new("RGBA", (320, 200), (26, 22, 20, 245))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 319, 199], outline=(88, 70, 44, 255))
    d.rectangle([3, 3, 316, 196], outline=(58, 46, 32, 255))
    # two pages
    d.rectangle([10, 22, 152, 172], fill=(38, 32, 26, 255))
    d.rectangle([164, 22, 308, 172], fill=(34, 29, 24, 255))
    # spine
    d.line([(157, 16), (157, 178)], fill=(74, 58, 38, 255))
    d.line([(159, 16), (159, 178)], fill=(52, 40, 28, 255))
    # filigree corners
    for cx, cy, sx, sy in ((12, 25, 1, 1), (306, 25, -1, 1), (12, 168, 1, -1), (306, 168, -1, -1)):
        d.line([(cx, cy), (cx + 18 * sx, cy)], fill=(104, 86, 54, 255))
        d.line([(cx, cy), (cx, cy + 12 * sy)], fill=(104, 86, 54, 255))
        d.point((cx + 4 * sx, cy + 4 * sy), fill=(150, 126, 78, 255))
    # faint ruling on the right page
    for y in range(34, 168, 10):
        d.line([(168, y), (304, y)], fill=(48, 40, 32, 255))
    # a lamp doodle in the corner
    d.ellipse([28, 150, 40, 162], outline=(96, 76, 44, 255))
    d.point((34, 156), fill=(198, 156, 84, 255))
    save(img, GUI_DIR, "journal")

    # ---- altar -------------------------------------------------------------------------
    img = Image.new("RGBA", (176, 166), (22, 20, 19, 250))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 175, 165], outline=(84, 74, 56, 255))
    d.rectangle([3, 3, 172, 162], fill=(30, 30, 27, 255), outline=(56, 52, 44, 255))
    # carved border
    for i in range(8, 168, 8):
        d.point((i, 6), fill=(72, 66, 52, 255))
        d.point((i, 159), fill=(72, 66, 52, 255))
    # sockets
    for x, y in ((26, 20), (52, 20), (78, 20), (134, 20)):
        d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(18, 17, 16, 255), outline=(70, 62, 48, 255))
        d.rectangle([x, y, x + 15, y + 15], outline=(44, 40, 34, 255))
    # the altar's own glow between input and output
    for radius in range(10, 0, -1):
        alpha = int(46 * (1 - radius / 10.0))
        d.ellipse([108 - radius, 28 - radius, 108 + radius, 28 + radius], fill=(150, 176, 120, alpha))
    d.line([(104, 20), (112, 20)], fill=(120, 142, 96, 255))
    d.line([(108, 16), (108, 24)], fill=(120, 142, 96, 255))
    # inventory grid
    for row in range(3):
        for column in range(9):
            x = 8 + column * 18
            y = 84 + row * 18
            d.rectangle([x - 1, y - 1, x + 16, y + 16], outline=(46, 42, 36, 255))
    for column in range(9):
        x = 8 + column * 18
        d.rectangle([x - 1, 141, x + 16, 158], outline=(46, 42, 36, 255))
    save(img, GUI_DIR, "altar")

    # ---- map ---------------------------------------------------------------------------
    img = Image.new("RGBA", (256, 232), (26, 24, 20, 246))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 255, 231], outline=(96, 82, 56, 255))
    d.rectangle([3, 3, 252, 228], fill=(30, 28, 23, 255), outline=(58, 50, 38, 255))
    for y in range(6, 226, 4):
        d.line([(6, y), (249, y)], fill=(33, 30, 25, 255))
    # compass rose
    cx, cy = 224, 202
    d.ellipse([cx - 12, cy - 12, cx + 12, cy + 12], outline=(88, 74, 50, 255))
    d.line([(cx, cy - 14), (cx, cy + 14)], fill=(110, 92, 60, 255))
    d.line([(cx - 14, cy), (cx + 14, cy)], fill=(110, 92, 60, 255))
    d.polygon([(cx, cy - 12), (cx + 3, cy), (cx - 3, cy)], fill=(150, 124, 76, 255))
    save(img, GUI_DIR, "map")

    # ---- ending ------------------------------------------------------------------------
    img = Image.new("RGBA", (280, 180), (12, 10, 12, 252))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 279, 179], outline=(70, 60, 44, 255))
    for radius in range(64, 0, -2):
        alpha = int(38 * (1 - radius / 64.0))
        d.ellipse([140 - radius, 96 - radius, 140 + radius, 96 + radius], fill=(198, 150, 80, alpha))
    d.ellipse([138, 94, 142, 98], fill=(255, 226, 160, 255))
    for _ in range(30):
        d.point((random.randint(4, 275), random.randint(4, 175)), fill=(70, 62, 48, 255))
    save(img, GUI_DIR, "ending")

    # ---- hud ---------------------------------------------------------------------------
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.arc([1, 2, 14, 13], start=200, end=340, fill=(226, 200, 148, 255))
    d.arc([1, 5, 14, 16], start=20, end=160, fill=(226, 200, 148, 255))
    d.ellipse([6, 6, 9, 10], fill=(226, 200, 148, 255))
    d.ellipse([7, 7, 8, 9], fill=(60, 40, 30, 255))
    save(img, GUI_DIR, "hud")

    # ---- mod icon ----------------------------------------------------------------------
    icon = Image.new("RGBA", (128, 128), (10, 9, 11, 255))
    d = ImageDraw.Draw(icon)
    for radius in range(56, 0, -2):
        alpha = int(70 * (1 - radius / 56.0))
        d.ellipse([64 - radius, 60 - radius, 64 + radius, 60 + radius], fill=(210, 158, 82, alpha))
    d.rectangle([48, 36, 80, 92], fill=(38, 34, 30, 255), outline=(88, 76, 56, 255))
    for y in range(44, 88):
        t = 1.0 - abs(y - 66) / 44.0
        colour = mix((140, 84, 34), (255, 232, 168), max(0.0, t))
        d.line([(52, y), (76, y)], fill=(*colour, 255))
    d.rectangle([54, 24, 74, 36], fill=(44, 40, 36, 255))
    d.arc([44, 16, 84, 46], start=200, end=340, fill=(96, 84, 64, 255))
    # something standing at the edge of the light
    d.rectangle([24, 76, 34, 116], fill=(16, 15, 17, 255))
    d.ellipse([25, 68, 33, 76], fill=(16, 15, 17, 255))
    icon.save(os.path.join(ROOT, "icon.png"))

    # ---- particle ----------------------------------------------------------------------
    save(soft_dot(8), PARTICLE_DIR, "mote")


if __name__ == "__main__":
    block_textures()
    item_textures()
    gui_textures()
    print("textures written to", ROOT)
