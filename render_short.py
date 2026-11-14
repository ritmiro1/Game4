#!/usr/bin/env python3
"""Render a vertical, original-visuals tribute edit for Metropolis (1927)."""
from __future__ import annotations

import argparse
import math
import random
import shutil
import subprocess
import sys
import time
import wave
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "assets"
BUILD = ROOT / "build"
DEFAULT_OUTPUT = ROOT / "output" / "metropolis_shorts.mp4"

WIDTH, HEIGHT, FPS = 1080, 1920, 30
SAMPLE_RATE = 48_000
BPM = 100
BEAT = 60.0 / BPM
DURATION = 31.2  # 13 bars at 100 BPM
FRAME_COUNT = round(DURATION * FPS)

IVORY = (244, 235, 218, 255)
GOLD = (232, 183, 99, 255)
RUST = (189, 91, 57, 255)
INK = (8, 8, 8, 255)

FONT_SANS = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_SANS_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"


@dataclass(frozen=True)
class Line:
    text: str
    size: int
    face: str = "sans"
    color: tuple[int, int, int, int] = IVORY


@dataclass(frozen=True)
class Scene:
    start: float
    end: float
    image: str
    index: int
    label: str
    lines: tuple[Line, ...]
    text_y: int
    zoom_start: float = 1.0
    zoom_end: float = 1.08
    pan_x_start: float = 0.0
    pan_x_end: float = 0.0
    pan_y_start: float = 0.0
    pan_y_end: float = 0.0
    title_card: bool = False
    flash: float = 0.28


SCENES = (
    Scene(0.0, 4.8, "metropolis_01_city.png", 1, "БУДУЩЕЕ / 1927",
          (Line("БУДУЩЕЕ", 116, "serif"), Line("УЖЕ БЫЛО ЗДЕСЬ.", 68, "sans", GOLD)),
          1010, 1.00, 1.075, -8, 12, 58, -18, flash=0.0),
    Scene(4.8, 9.6, "metropolis_01_city.png", 2, "ГОРОД НАВЕРХУ",
          (Line("НАВЕРХУ —", 101, "serif"), Line("ГОРОД, КОТОРЫЙ", 72), Line("НЕ СПИТ.", 94, "sans", GOLD)),
          930, 1.025, 1.105, 18, -28, -10, -90, flash=0.32),
    Scene(9.6, 14.4, "metropolis_02_machine.png", 3, "ГОРОД ВНИЗУ",
          (Line("ВНИЗУ — ТЕ,", 98, "serif"), Line("КТО ДЕРЖИТ ГОРОД.", 70, "sans", GOLD)),
          975, 1.00, 1.095, 0, 8, 48, -20, flash=0.38),
    Scene(14.4, 19.2, "metropolis_02_machine.png", 4, "ВРЕМЯ МАШИНЫ",
          (Line("ИХ ВРЕМЯ", 107, "serif"), Line("ОТМЕРЯЕТ", 94), Line("МАШИНА.", 104, "serif", GOLD)),
          915, 1.035, 1.12, -20, 36, 20, -62, flash=0.34),
    Scene(19.2, 24.0, "metropolis_03_bridge.png", 5, "МОСТ МЕЖДУ МИРАМИ",
          (Line("МЕЖДУ ГОЛОВОЙ", 82, "sans"), Line("И РУКАМИ…", 112, "serif", GOLD)),
          575, 1.00, 1.075, 2, -10, -115, -55, flash=0.44),
    Scene(24.0, 26.4, "metropolis_03_bridge.png", 6, "СЕРДЦЕ",
          (Line("ДОЛЖНО БЫТЬ", 85, "sans"), Line("СЕРДЦЕ.", 124, "serif", GOLD)),
          605, 1.075, 1.11, -10, 16, -46, -8, flash=0.48),
    Scene(26.4, 31.2, "metropolis_03_bridge.png", 7, "ФИНАЛЬНЫЙ КАДР",
          (), 0, 1.10, 1.16, 0, 10, 0, -18, title_card=True, flash=0.18),
)


def font(size: int, face: str = "sans") -> ImageFont.FreeTypeFont:
    path = {"sans": FONT_SANS, "regular": FONT_SANS_REGULAR, "serif": FONT_SERIF}[face]
    return ImageFont.truetype(path, size=size)


def tracked_text(draw: ImageDraw.ImageDraw, xy: tuple[int, int], text: str,
                 use_font: ImageFont.FreeTypeFont, fill: tuple[int, int, int, int],
                 spacing: int = 3) -> None:
    x, y = xy
    for char in text:
        draw.text((x, y), char, font=use_font, fill=fill)
        x += int(draw.textlength(char, font=use_font)) + spacing


def text_bbox(font_obj: ImageFont.FreeTypeFont, text: str) -> tuple[int, int, int, int]:
    return font_obj.getbbox(text, stroke_width=2)


def fit_font(text: str, size: int, face: str, max_width: int) -> ImageFont.FreeTypeFont:
    result = font(size, face)
    while result.getlength(text) > max_width and size > 32:
        size -= 2
        result = font(size, face)
    return result


def cropped_layer(layer: Image.Image) -> tuple[Image.Image, tuple[int, int]]:
    bbox = layer.getbbox()
    if bbox is None:
        return Image.new("RGBA", (1, 1), (0, 0, 0, 0)), (0, 0)
    # A small transparent margin gives the animated text room to move vertically.
    l, t, r, b = bbox
    margin = 14
    bounds = (max(0, l - margin), max(0, t - margin),
              min(WIDTH, r + margin), min(HEIGHT, b + margin))
    return layer.crop(bounds), (bounds[0], bounds[1])


def build_hud(scene: Scene) -> tuple[Image.Image, tuple[int, int]]:
    layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    label_font = font(24, "sans")
    meta_font = font(19, "regular")
    index_font = font(34, "serif")

    tracked_text(draw, (100, 126), "METROPOLIS", label_font, IVORY, spacing=4)
    draw.text((100, 165), "ФРИЦ ЛАНГ   ·   1927", font=meta_font,
              fill=(232, 183, 99, 235))
    right = f"{scene.index:02d}  /  07"
    draw.text((880, 134), right, font=index_font, fill=(244, 235, 218, 220), anchor="ra")
    draw.line((100, 215, 880, 215), fill=(238, 227, 206, 80), width=2)
    draw.line((100, 215, 175, 215), fill=(232, 183, 99, 245), width=4)
    return cropped_layer(layer)


def build_caption(scene: Scene) -> tuple[Image.Image, tuple[int, int]]:
    layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)

    if scene.title_card:
        # An intentionally quiet, centered end card with plenty of hold time.
        draw.line((175, 677, 905, 677), fill=(232, 183, 99, 195), width=3)
        draw.line((175, 677, 260, 677), fill=(244, 235, 218, 255), width=3)
        title = "МЕТРОПОЛИС"
        title_font = fit_font(title, 132, "serif", 820)
        tw = draw.textlength(title, font=title_font)
        tx = (WIDTH - tw) / 2
        ty = 790
        draw.text((tx + 4, ty + 8), title, font=title_font,
                  fill=(0, 0, 0, 220), stroke_width=5, stroke_fill=(0, 0, 0, 190))
        draw.text((tx, ty), title, font=title_font, fill=IVORY,
                  stroke_width=2, stroke_fill=(8, 8, 8, 190))
        sub = "ФРИЦ ЛАНГ  ·  1927"
        sub_font = font(31, "sans")
        draw.text((WIDTH / 2, 964), sub, font=sub_font, fill=GOLD, anchor="ma")
        draw.line((330, 1035, 750, 1035), fill=(232, 183, 99, 125), width=2)
        tag = "99 ЛЕТ СПУСТЯ — ВСЁ ЕЩЁ ПРО НАС."
        tag_font = fit_font(tag, 34, "regular", 820)
        draw.text((WIDTH / 2, 1081), tag, font=tag_font,
                  fill=(244, 235, 218, 235), anchor="ma")
        draw.text((WIDTH / 2, 1190), "СОХРАНИ ЭТУ КЛАССИКУ В СВОЁМ СПИСКЕ",
                  font=font(19, "regular"), fill=(244, 235, 218, 180), anchor="ma")
        return cropped_layer(layer)

    label = scene.label.upper()
    kicker_font = font(23, "sans")
    draw.text((110, scene.text_y - 60), label, font=kicker_font, fill=RUST)
    # Fine editorial marker to the left of the copy.
    block_height = sum(int(line.size * 1.15) for line in scene.lines)
    draw.rounded_rectangle((96, scene.text_y + 4, 102, scene.text_y + block_height - 16),
                           radius=3, fill=(232, 183, 99, 225))

    y = scene.text_y
    for line in scene.lines:
        line_font = fit_font(line.text, line.size, line.face, 825)
        bbox = text_bbox(line_font, line.text)
        actual_y = y - bbox[1]
        draw.text((113 + 3, actual_y + 5), line.text, font=line_font,
                  fill=(0, 0, 0, 235), stroke_width=5, stroke_fill=(0, 0, 0, 210))
        draw.text((113, actual_y), line.text, font=line_font, fill=line.color,
                  stroke_width=2, stroke_fill=(6, 6, 6, 220))
        y += int(line.size * 1.15)

    # Small tracking line under the block adds a repeatable editorial signature.
    draw.line((113, y + 17, 285, y + 17), fill=(232, 183, 99, 200), width=3)
    draw.ellipse((295, y + 12, 305, y + 22), fill=(189, 91, 57, 225))
    return cropped_layer(layer)


def make_vignette() -> Image.Image:
    xs = np.linspace(-1.0, 1.0, WIDTH, dtype=np.float32)[None, :]
    ys = np.linspace(-1.0, 1.0, HEIGHT, dtype=np.float32)[:, None]
    radius = np.sqrt(xs * xs + ys * ys)
    alpha = np.clip((radius - 0.45) / 0.92, 0.0, 1.0) ** 1.7
    alpha = (alpha * 128).astype(np.uint8)
    rgba = np.zeros((HEIGHT, WIDTH, 4), dtype=np.uint8)
    rgba[..., 3] = alpha
    return Image.fromarray(rgba, "RGBA")


def prepare_artwork() -> dict[str, Image.Image]:
    prepared: dict[str, Image.Image] = {}
    base_size = (round(WIDTH * 1.24), round(HEIGHT * 1.24))
    for name in {scene.image for scene in SCENES}:
        path = ASSETS / name
        if not path.exists():
            raise FileNotFoundError(f"Missing generated artwork: {path}")
        im = Image.open(path).convert("RGB")
        im = ImageOps.fit(im, base_size, method=Image.Resampling.LANCZOS,
                          centering=(0.5, 0.5))
        im = ImageEnhance.Contrast(im).enhance(1.08)
        im = ImageEnhance.Brightness(im).enhance(0.90)
        im = ImageEnhance.Color(im).enhance(0.88)
        # A restrained amber wash ties the three original illustrations together.
        warm = Image.new("RGB", im.size, (132, 91, 48))
        im = Image.blend(im, warm, 0.055)
        prepared[name] = im
    return prepared


def ease_in_out(x: float) -> float:
    x = min(1.0, max(0.0, x))
    return x * x * (3.0 - 2.0 * x)


def scene_frame(art: Image.Image, scene: Scene, local: float, t: float) -> Image.Image:
    p = min(1.0, max(0.0, local / (scene.end - scene.start)))
    eased = ease_in_out(p)
    zoom = scene.zoom_start + (scene.zoom_end - scene.zoom_start) * eased
    cw = max(1, round(WIDTH / zoom))
    ch = max(1, round(HEIGHT / zoom))
    cx = art.width / 2 + scene.pan_x_start + (scene.pan_x_end - scene.pan_x_start) * eased
    cy = art.height / 2 + scene.pan_y_start + (scene.pan_y_end - scene.pan_y_start) * eased

    # A very restrained gate-weave bump lands on each beat without distracting from copy.
    beat_position = t / BEAT
    beat_phase = beat_position - math.floor(beat_position)
    impact = math.exp(-beat_phase * 27.0)
    shake = 3.0 * impact * math.sin(beat_position * math.pi * 2.4)
    cx += shake
    cy += shake * 0.45

    left = int(round(cx - cw / 2))
    top = int(round(cy - ch / 2))
    left = min(max(0, left), art.width - cw)
    top = min(max(0, top), art.height - ch)
    frame = art.crop((left, top, left + cw, top + ch))
    return frame.resize((WIDTH, HEIGHT), Image.Resampling.LANCZOS)


def blend_overlay(frame: Image.Image, overlay: tuple[Image.Image, tuple[int, int]],
                  opacity: float = 1.0, shift_y: int = 0) -> None:
    layer, (x, y) = overlay
    if layer.width <= 1 or layer.height <= 1 or opacity <= 0.0:
        return
    if opacity >= 0.999:
        frame.alpha_composite(layer, (x, y + shift_y))
        return
    alpha = layer.getchannel("A").point(lambda value: round(value * opacity))
    faded = layer.copy()
    faded.putalpha(alpha)
    frame.alpha_composite(faded, (x, y + shift_y))


def create_frame(t: float, art: dict[str, Image.Image], vignette: Image.Image,
                 huds: dict[int, tuple[Image.Image, tuple[int, int]]],
                 captions: dict[int, tuple[Image.Image, tuple[int, int]]]) -> Image.Image:
    scene = next(s for s in SCENES if s.start <= t < s.end or t >= DURATION - 1e-7 and s.end == DURATION)
    local = max(0.0, t - scene.start)
    bg = scene_frame(art[scene.image], scene, local, t)

    # Opening/closing fades and a tiny warm shutter-flash at beat-synced cuts.
    if t < 0.48:
        bg = Image.blend(Image.new("RGB", (WIDTH, HEIGHT), (0, 0, 0)), bg,
                         ease_in_out(t / 0.48))
    if t > DURATION - 0.42:
        bg = Image.blend(bg, Image.new("RGB", (WIDTH, HEIGHT), (0, 0, 0)),
                         ease_in_out((t - (DURATION - 0.42)) / 0.42))
    if scene.start > 0 and local < 0.12:
        amount = scene.flash * math.exp(-local * 24.0)
        bg = Image.blend(bg, Image.new("RGB", (WIDTH, HEIGHT), (241, 211, 163)), amount)

    frame = bg.convert("RGBA")
    frame.alpha_composite(vignette)

    # A few drifting flecks add life to the still artwork; the source illustrations
    # already carry the primary grain and scratches.
    dust = ImageDraw.Draw(frame)
    seed = scene.index * 911
    for i in range(15):
        x0 = (seed * 17 + i * 193) % WIDTH
        y0 = (seed * 31 + i * 317) % HEIGHT
        x = int((x0 + (i % 4 - 1.5) * t * 9) % WIDTH)
        y = int((y0 - (2 + i % 5) * t * 8) % HEIGHT)
        a = (220, 190, 139, 70 + (i % 3) * 12)
        r = 1 if i % 4 else 2
        dust.ellipse((x - r, y - r, x + r, y + r), fill=a)

    # Kinetic copy enters on the downbeat, then holds cleanly for readability.
    duration = scene.end - scene.start
    fade_in = min(1.0, local / 0.16)
    fade_out = min(1.0, max(0.0, (duration - local) / 0.12))
    text_alpha = ease_in_out(fade_in) * ease_in_out(fade_out)
    offset = round((1.0 - ease_in_out(fade_in)) * 18)
    blend_overlay(frame, huds[scene.index], min(1.0, text_alpha + 0.1))
    blend_overlay(frame, captions[scene.index], text_alpha, -offset)
    return frame.convert("RGB")


def midi_hz(note: int) -> float:
    return 440.0 * (2.0 ** ((note - 69) / 12.0))


def add_slice(target: np.ndarray, start: int, signal: np.ndarray, gain: float = 1.0) -> None:
    if start >= len(target):
        return
    end = min(len(target), start + len(signal))
    if end > start:
        target[start:end] += signal[:end - start] * gain


def make_score(path: Path) -> None:
    """Synthesize an original, beat-aligned industrial/orchestral score."""
    sample_count = round(DURATION * SAMPLE_RATE)
    left = np.zeros(sample_count, dtype=np.float32)
    right = np.zeros(sample_count, dtype=np.float32)
    rng = np.random.default_rng(1927)

    # Chord bed: a dark D-minor arc that opens into a restrained final resolution.
    chord_events = [
        (0.0, 4.8, (50, 57, 62, 65, 69)),       # Dm
        (4.8, 9.6, (46, 53, 58, 62, 65)),       # Bb
        (9.6, 14.4, (43, 50, 55, 58, 62)),      # Gm
        (14.4, 19.2, (45, 52, 55, 61, 64)),     # A7
        (19.2, 24.0, (50, 57, 62, 65, 69)),    # Dm
        (24.0, 26.4, (46, 53, 58, 62, 65)),    # Bb
        (26.4, 31.2, (50, 57, 62, 65, 69)),    # Dm resolve
    ]
    for start_s, end_s, notes in chord_events:
        start = round(start_s * SAMPLE_RATE)
        end = min(sample_count, round(end_s * SAMPLE_RATE))
        count = end - start
        tau = np.arange(count, dtype=np.float32) / SAMPLE_RATE
        event_duration = end_s - start_s
        attack = np.minimum(1.0, tau / 0.8)
        release = np.minimum(1.0, (event_duration - tau) / 0.75)
        env = np.clip(attack, 0, 1) * np.clip(release, 0, 1)
        env = (0.52 + 0.48 * env) * 0.72
        for j, note in enumerate(notes):
            hz = midi_hz(note)
            detune = 2 ** (0.0035 / 12.0)
            phase = tau * (2.0 * math.pi * hz)
            # Soft partials keep the harmony warm rather than synthetic-bright.
            l = (np.sin(phase) + 0.22 * np.sin(phase * 2.0 + 0.16) +
                 0.06 * np.sin(phase * 3.0 + 0.31))
            rphase = tau * (2.0 * math.pi * hz * detune)
            r = (np.sin(rphase + 0.09) + 0.22 * np.sin(rphase * 2.0 + 0.24) +
                 0.06 * np.sin(rphase * 3.0 + 0.39))
            voice_gain = 0.033 if j < 3 else 0.026
            left[start:end] += (l * env * voice_gain).astype(np.float32)
            right[start:end] += (r * env * voice_gain).astype(np.float32)

    # Low, pulsing machine-room drone gently shifts with the chord roots.
    for start_s, end_s, midi in [(0, 4.8, 38), (4.8, 9.6, 34), (9.6, 14.4, 31),
                                  (14.4, 19.2, 33), (19.2, 24, 38), (24, 26.4, 34),
                                  (26.4, 31.2, 38)]:
        start, end = round(start_s * SAMPLE_RATE), round(end_s * SAMPLE_RATE)
        tau = np.arange(end - start, dtype=np.float32) / SAMPLE_RATE
        hz = midi_hz(midi)
        slow_pulse = 0.78 + 0.22 * np.sin(2 * np.pi * 0.31 * tau + 0.4)
        tone = (np.sin(2 * np.pi * hz * tau) + 0.19 * np.sin(2 * np.pi * hz * 2 * tau))
        add_slice(left, start, (tone * slow_pulse * 0.11).astype(np.float32))
        add_slice(right, start, (tone * slow_pulse * 0.105).astype(np.float32))

    # Beat-aligned low drum: a soft metal-and-heart pulse, stronger at each bar.
    beat_total = int(round(DURATION / BEAT))
    kick_len = int(0.46 * SAMPLE_RATE)
    kt = np.arange(kick_len, dtype=np.float32) / SAMPLE_RATE
    phase = 2 * np.pi * (48.0 * kt + (80.0 / 25.0) * (1.0 - np.exp(-25.0 * kt)))
    kick_body = np.sin(phase) * np.exp(-kt * 7.5)
    click_noise = rng.normal(0.0, 1.0, kick_len).astype(np.float32) * np.exp(-kt * 95.0)
    kick = (kick_body + 0.10 * click_noise).astype(np.float32)
    for beat_idx in range(beat_total):
        t0 = beat_idx * BEAT
        start = round(t0 * SAMPLE_RATE)
        accent = 1.0 if beat_idx % 4 == 0 else (0.48 if beat_idx % 2 == 0 else 0.25)
        strength = 0.19 * accent
        add_slice(left, start, kick, strength)
        add_slice(right, start, kick, strength * 0.94)

    # Bell-like motif, two beats apart, with slightly different pans in each ear.
    motif = [74, 81, 77, 84, 82, 77, 74, 69, 74, 77, 81, 86, 84, 81, 77, 74, 69, 74, 77, 81, 86, 89, 86, 81, 77, 74]
    bell_len = int(0.92 * SAMPLE_RATE)
    bt = np.arange(bell_len, dtype=np.float32) / SAMPLE_RATE
    bell_env = np.exp(-bt * 3.9) * (1.0 - np.exp(-bt * 48.0))
    for i, beat_idx in enumerate(range(1, beat_total, 2)):
        note = motif[i % len(motif)]
        hz = midi_hz(note)
        gain = 0.075 * (0.75 + 0.25 * min(1.0, beat_idx / 36.0))
        bell_l = (np.sin(2 * np.pi * hz * bt) +
                  0.30 * np.sin(2 * np.pi * hz * 2.71 * bt + 0.4) +
                  0.12 * np.sin(2 * np.pi * hz * 4.16 * bt + 0.8)) * bell_env
        bell_r = (np.sin(2 * np.pi * hz * 1.002 * bt + 0.08) +
                  0.28 * np.sin(2 * np.pi * hz * 2.70 * bt + 0.52) +
                  0.10 * np.sin(2 * np.pi * hz * 4.18 * bt + 0.92)) * bell_env
        start = round((beat_idx * BEAT) * SAMPLE_RATE)
        add_slice(left, start, bell_l.astype(np.float32), gain)
        add_slice(right, start, bell_r.astype(np.float32), gain)

    # Metallic transition swells / impacts coincide with the visual cuts.
    for boundary in [4.8, 9.6, 14.4, 19.2, 24.0, 26.4]:
        # A short reverse-noise rise just before the cut.
        rise_len = int(0.48 * SAMPLE_RATE)
        rt = np.arange(rise_len, dtype=np.float32) / SAMPLE_RATE
        noise = rng.normal(0.0, 1.0, rise_len).astype(np.float32)
        rise_env = (rt / rt[-1]) ** 2 * 0.045
        rise = noise * rise_env
        start = max(0, round((boundary - 0.48) * SAMPLE_RATE))
        add_slice(left, start, rise.astype(np.float32), 1.0)
        add_slice(right, start, rise.astype(np.float32), 0.82)

        hit_len = int(0.85 * SAMPLE_RATE)
        ht = np.arange(hit_len, dtype=np.float32) / SAMPLE_RATE
        ring_env = np.exp(-ht * 4.7)
        clang = (0.48 * np.sin(2 * np.pi * 196.0 * ht) +
                 0.29 * np.sin(2 * np.pi * 293.7 * ht + 0.2) +
                 0.18 * np.sin(2 * np.pi * 411.0 * ht + 0.5) +
                 0.05 * rng.normal(0.0, 1.0, hit_len).astype(np.float32)) * ring_env
        start = round(boundary * SAMPLE_RATE)
        impact_gain = 0.20 if boundary < 19.2 else 0.24
        add_slice(left, start, clang.astype(np.float32), impact_gain)
        add_slice(right, start, np.roll(clang, int(0.013 * SAMPLE_RATE)).astype(np.float32), impact_gain * 0.88)

    # Light, short echo gives the score a cavernous machine-room tail.
    dry_l, dry_r = left.copy(), right.copy()
    for delay_s, gain in [(0.11, 0.16), (0.23, 0.10), (0.37, 0.055)]:
        delay = round(delay_s * SAMPLE_RATE)
        left[delay:] += dry_r[:-delay] * gain
        right[delay:] += dry_l[:-delay] * gain

    # Fade in/out, then soft-limit to a clean upload-safe peak.
    t = np.arange(sample_count, dtype=np.float32) / SAMPLE_RATE
    fade = np.minimum(1.0, t / 0.65) * np.minimum(1.0, (DURATION - t) / 0.92)
    mix = np.column_stack((left, right)) * fade[:, None]
    peak = max(1e-8, float(np.max(np.abs(mix))))
    mix = np.tanh(mix * (0.82 / peak)) / np.tanh(0.82)
    pcm = np.clip(mix * 32767.0, -32768, 32767).astype("<i2")

    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(2)
        wav.setsampwidth(2)
        wav.setframerate(SAMPLE_RATE)
        wav.writeframes(pcm.tobytes())


def get_ffmpeg() -> str:
    try:
        import imageio_ffmpeg
        return imageio_ffmpeg.get_ffmpeg_exe()
    except Exception:
        executable = shutil.which("ffmpeg")
        if executable:
            return executable
        raise RuntimeError("FFmpeg was not found. Install imageio-ffmpeg or ffmpeg.")


def render(output: Path) -> None:
    output = output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    BUILD.mkdir(parents=True, exist_ok=True)
    score_path = BUILD / "original_score.wav"

    print("Preparing original artwork…", flush=True)
    artwork = prepare_artwork()
    vignette = make_vignette()
    huds = {scene.index: build_hud(scene) for scene in SCENES}
    captions = {scene.index: build_caption(scene) for scene in SCENES}

    print("Composing original score…", flush=True)
    make_score(score_path)

    ffmpeg = get_ffmpeg()
    command = [
        ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
        "-f", "rawvideo", "-pixel_format", "rgb24",
        "-video_size", f"{WIDTH}x{HEIGHT}", "-framerate", str(FPS), "-i", "pipe:0",
        "-i", str(score_path),
        "-map", "0:v:0", "-map", "1:a:0",
        "-c:v", "libx264", "-preset", "medium", "-crf", "18",
        "-profile:v", "high", "-pix_fmt", "yuv420p", "-r", str(FPS),
        "-color_primaries", "bt709", "-color_trc", "bt709", "-colorspace", "bt709",
        "-c:a", "aac", "-b:a", "192k", "-ar", str(SAMPLE_RATE),
        "-metadata", "title=METROPOLIS — короткий кино-эдит",
        "-metadata", "artist=Original visual and score edit",
        "-metadata", "comment=Original generated artwork and synthesized score; no film footage or OST used.",
        "-movflags", "+faststart", "-shortest", str(output),
    ]
    print(f"Rendering {FRAME_COUNT} frames at {WIDTH}x{HEIGHT} / {FPS} fps…", flush=True)
    start_time = time.time()
    process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL,
                               stderr=subprocess.PIPE)
    assert process.stdin is not None
    try:
        for frame_index in range(FRAME_COUNT):
            t = frame_index / FPS
            frame = create_frame(t, artwork, vignette, huds, captions)
            process.stdin.write(frame.tobytes())
            if (frame_index + 1) % 120 == 0 or frame_index + 1 == FRAME_COUNT:
                print(f"  {frame_index + 1:>4}/{FRAME_COUNT} frames", flush=True)
        process.stdin.close()
    except BrokenPipeError:
        pass
    finally:
        if process.stdin and not process.stdin.closed:
            process.stdin.close()
    stderr = process.stderr.read().decode("utf-8", errors="replace") if process.stderr else ""
    return_code = process.wait()
    score_path.unlink(missing_ok=True)
    if return_code != 0:
        raise RuntimeError(f"FFmpeg failed ({return_code}):\n{stderr}")
    elapsed = time.time() - start_time
    print(f"\nDone: {output} ({output.stat().st_size / (1024 * 1024):.1f} MB, {elapsed:.1f}s)", flush=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT,
                        help=f"MP4 path (default: {DEFAULT_OUTPUT.relative_to(ROOT)})")
    args = parser.parse_args()
    render(args.output)


if __name__ == "__main__":
    main()
