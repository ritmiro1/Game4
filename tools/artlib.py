"""Small pixel-art toolkit used to build every texture in the mod.

Everything is drawn procedurally from a palette plus a handful of strokes, but each texture is
hand-composed: the noise is seeded per texture, the shading direction is always top-left, and
materials get their own detail language (stone cracks, brick courses, grain, rust, veins).
"""
from __future__ import annotations

import math
import random
from PIL import Image, ImageDraw, ImageFilter

TOP_LIGHT = -1  # light comes from the upper left


def shade(rgb, amount):
    r, g, b = rgb[:3]
    return (
        max(0, min(255, int(r + amount))),
        max(0, min(255, int(g + amount))),
        max(0, min(255, int(b + amount))),
    )


def mix(a, b, t):
    return (
        int(a[0] + (b[0] - a[0]) * t),
        int(a[1] + (b[1] - a[1]) * t),
        int(a[2] + (b[2] - a[2]) * t),
    )


def new_image(size=(16, 16), color=(0, 0, 0, 0)):
    return Image.new("RGBA", size, color)


def noise_fill(img, base, rng, strength=10, alpha=255):
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            n = rng.randint(-strength, strength)
            # A little vertical gradient so surfaces feel lit from above.
            n += int((0.5 - y / max(1, h - 1)) * strength * 0.6)
            r, g, b = shade(base, n)
            px[x, y] = (r, g, b, alpha)
    return img


def speckle(img, rng, count, color, alpha=255):
    px = img.load()
    w, h = img.size
    for _ in range(count):
        x, y = rng.randrange(w), rng.randrange(h)
        c = color if alpha >= 255 else (color[0], color[1], color[2], alpha)
        px[x, y] = c
    return img


def edge_darken(img, amount=26, width=1):
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            if x < width or y < width or x >= w - width or y >= h - width:
                r, g, b, a = px[x, y]
                px[x, y] = (*shade((r, g, b), -amount), a)
    return img


def draw_crack(img, rng, color, steps=6, start=None):
    px = img.load()
    w, h = img.size
    x, y = start if start else (rng.randrange(w), rng.randrange(h))
    for _ in range(steps):
        if 0 <= x < w and 0 <= y < h:
            px[x, y] = (*color, 255)
        x += rng.choice([-1, 0, 1])
        y += rng.choice([0, 1])
    return img


def stone(name_seed, base, rng, cracks=2, speckles=26, veins=None):
    img = new_image()
    noise_fill(img, base, rng, 11)
    speckle(img, rng, speckles, shade(base, -18))
    speckle(img, rng, speckles // 2, shade(base, 14))
    for _ in range(cracks):
        draw_crack(img, rng, shade(base, -30), steps=rng.randint(3, 7))
    if veins:
        for _ in range(3):
            draw_crack(img, rng, veins, steps=rng.randint(4, 9))
    edge_darken(img, 14)
    return img


def brickwork(name_seed, base, mortar, rng, rows=4, course_height=4):
    img = new_image()
    noise_fill(img, base, rng, 8)
    d = ImageDraw.Draw(img)
    w, h = img.size
    for row in range(rows):
        y = row * course_height
        d.line([(0, y), (w, y)], fill=(*mortar, 255))
        offset = 0 if row % 2 == 0 else 4
        for x in range(-offset, w, 8):
            d.line([(x, y), (x, min(h - 1, y + course_height - 1))], fill=(*mortar, 255))
        # per-brick tone
        for bx in range(-offset, w, 8):
            tone = rng.randint(-12, 12)
            for yy in range(y + 1, min(h, y + course_height - 1)):
                for xx in range(max(0, bx + 1), min(w, bx + 7)):
                    r, g, b, a = img.getpixel((xx, yy))
                    img.putpixel((xx, yy), (*shade((r, g, b), tone), a))
    speckle(img, rng, 20, shade(base, -22))
    for _ in range(2):
        draw_crack(img, rng, shade(base, -34), steps=rng.randint(3, 6))
    edge_darken(img, 16)
    return img


def tiles(base, grout, rng, size=4):
    img = new_image()
    noise_fill(img, base, rng, 7)
    d = ImageDraw.Draw(img)
    w, h = img.size
    for y in range(0, h, size):
        for x in range(0, w, size):
            tone = rng.randint(-10, 10)
            for yy in range(y, min(h, y + size - 1)):
                for xx in range(x, min(w, x + size - 1)):
                    r, g, b, a = img.getpixel((xx, yy))
                    img.putpixel((xx, yy), (*shade((r, g, b), tone), a))
            d.line([(x, y + size - 1), (x + size - 1, y + size - 1)], fill=(*grout, 255))
            d.line([(x + size - 1, y), (x + size - 1, y + size - 1)], fill=(*grout, 255))
    speckle(img, rng, 14, shade(base, -20))
    edge_darken(img, 12)
    return img


def planks(base, rng, horizontal=False):
    img = new_image()
    noise_fill(img, base, rng, 9)
    d = ImageDraw.Draw(img)
    w, h = img.size
    if horizontal:
        for y in range(0, h, 4):
            d.line([(0, y), (w, y)], fill=(*shade(base, -30), 255))
            for _ in range(6):
                x = rng.randrange(w)
                d.line([(x, y + 1), (x + rng.randint(2, 5), y + 1)], fill=(*shade(base, 10), 255))
    else:
        for x in range(0, w, 4):
            d.line([(x, 0), (x, h)], fill=(*shade(base, -30), 255))
    edge_darken(img, 15)
    return img


def metal_rust(base, rust, rng):
    img = new_image()
    noise_fill(img, base, rng, 8)
    d = ImageDraw.Draw(img)
    for _ in range(9):
        cx, cy = rng.randrange(16), rng.randrange(16)
        radius = rng.randint(1, 3)
        for yy in range(cy - radius, cy + radius + 1):
            for xx in range(cx - radius, cx + radius + 1):
                if 0 <= xx < 16 and 0 <= yy < 16 and (xx - cx) ** 2 + (yy - cy) ** 2 <= radius * radius:
                    t = rng.random() * 0.8 + 0.2
                    img.putpixel((xx, yy), (*mix(base, rust, t), 255))
    for _ in range(4):
        y = rng.randrange(16)
        d.line([(0, y), (15, y)], fill=(*shade(base, -26), 255))
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), (*shade(base, 40), 255))
    edge_darken(img, 18)
    return img


def organic(base, glow, rng, veins=7):
    img = new_image()
    noise_fill(img, base, rng, 10)
    for _ in range(veins):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(4, 9)):
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (*mix(base, glow, rng.random() * 0.9), 255))
            x += rng.choice([-1, 0, 1])
            y += rng.choice([-1, 0, 1])
    edge_darken(img, 20)
    return img


def crystal(base, glow, rng):
    img = new_image()
    noise_fill(img, base, rng, 6)
    d = ImageDraw.Draw(img)
    for _ in range(4):
        x1, y1 = rng.randrange(16), rng.randrange(16)
        x2, y2 = rng.randrange(16), rng.randrange(16)
        d.line([(x1, y1), (x2, y2)], fill=(*mix(base, glow, 0.85), 255))
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        img.putpixel((x, y), (*glow, 255))
    edge_darken(img, 10)
    return img


def glow_block(base, core, rng, frame=True):
    img = new_image()
    noise_fill(img, base, rng, 6)
    d = ImageDraw.Draw(img)
    # radial core
    for y in range(16):
        for x in range(16):
            distance = math.hypot(x - 7.5, y - 7.5)
            if distance < 5.5:
                t = max(0.0, 1.0 - distance / 5.5)
                img.putpixel((x, y), (*mix(base, core, t * 1.2), 255))
    if frame:
        d.rectangle([0, 0, 15, 15], outline=(*shade(base, -40), 255))
        d.rectangle([2, 2, 13, 13], outline=(*shade(base, 20), 255))
    edge_darken(img, 8)
    return img


def soft_dot(size=8, color=(255, 255, 255)):
    img = new_image((size, size), (0, 0, 0, 0))
    c = (size - 1) / 2.0
    for y in range(size):
        for x in range(size):
            distance = math.hypot(x - c, y - c)
            alpha = max(0, int(255 * (1.0 - distance / (size / 2.0 + 0.2))))
            if alpha > 0:
                img.putpixel((x, y), (*color, alpha))
    return img


# --------------------------------------------------------------------- item scaffolding ---

OUTLINE = (18, 15, 13)


def item_canvas():
    return new_image((16, 16), (0, 0, 0, 0))


def outline_mask(img, color=OUTLINE):
    """Add a one-pixel dark outline around every opaque pixel block."""
    px = img.load()
    out = img.copy()
    op = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] == 0:
                    op[nx, ny] = (*color, 255)
    return out


def fill_rect(img, x1, y1, x2, y2, color, alpha=255):
    d = ImageDraw.Draw(img)
    d.rectangle([x1, y1, x2, y2], fill=(*color[:3], alpha))
    return img


def gradient_rect(img, x1, y1, x2, y2, top, bottom):
    for y in range(y1, y2 + 1):
        t = (y - y1) / max(1, (y2 - y1))
        color = mix(top, bottom, t)
        for x in range(x1, x2 + 1):
            img.putpixel((x, y), (*color, 255))
    return img


def item_noise(img, rng, strength=10):
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            n = rng.randint(-strength, strength)
            px[x, y] = (*shade((r, g, b), n), a)
    return img
