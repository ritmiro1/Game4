"""Renders every sound file and writes sounds.json."""
from __future__ import annotations

import json
import os

import numpy as np
import soundfile as sf

import synth
from synth import (RATE, bandpass, drone, env, exp_decay, fade, highpass, lowpass, noise,
                   normalize, ping, reverb, sine, soften, whisper_seed)

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets",
                    "hollowlight")
SOUND_DIR = os.path.join(ROOT, "sounds")
os.makedirs(SOUND_DIR, exist_ok=True)

SOUNDS = {}


def write(path, signal, peak=0.8, fade_out=0.35, stream=False):
    directory = os.path.join(SOUND_DIR, os.path.dirname(path))
    os.makedirs(directory, exist_ok=True)
    data = np.asarray(signal, dtype=np.float32)
    data = synth.fade(data, 0.03, fade_out)
    data = normalize(data, peak)
    full = os.path.join(SOUND_DIR, path + ".ogg")
    # Written in five-second blocks: this libsndfile build trips over single large Vorbis
    # writes, and block-wise writing is also how the encoder is meant to be driven.
    data = np.ascontiguousarray(data, dtype=np.float32)
    with sf.SoundFile(full, "w", samplerate=RATE, channels=1, format="OGG",
                      subtype="VORBIS") as handle:
        step = RATE * 5
        for start in range(0, len(data), step):
            handle.write(data[start:start + step])
    # sounds.json is keyed by the SoundEvent identifier path, which uses dots, while the file
    # lives in a directory tree. "entity/hollowed/idle" -> "entity.hollowed.idle".
    event = path.replace("/", ".")
    entry = SOUNDS.setdefault(event, {"sounds": [], "subtitle": "subtitles.hollowlight." + event})
    entry["sounds"].append({"name": "hollowlight:" + path, **({"stream": True} if stream else {})})
    return full


def add_into(buffer, index, length, samples, gain=1.0):
    """Mix `samples` into `buffer` at `index`, never overrunning and never mis-shaping."""
    if index < 0 or index >= len(buffer):
        return
    length = min(length, len(buffer) - index, len(samples))
    if length <= 0:
        return
    buffer[index:index + length] += samples[:length] * gain


# ================================================================ ambient layers ========

def ambience_halls():
    seconds = 26.0
    t = synth.t_axis(seconds)
    base = drone(58.0, seconds, partials=(1.0, 1.5, 2.0), detune=0.25, lfo=0.04) * 0.5
    air = lowpass(noise(seconds, 11), 700) * 0.12
    drips = np.zeros_like(t)
    for moment in (3.2, 9.7, 15.1, 21.6):
        index = int(moment * RATE)
        length = min(len(t) - index, int(1.4 * RATE))
        if length > 0:
            add_into(drips, index, length, ping(880 + moment * 12, length / RATE, decay=9.0), 0.06)
    return reverb(base + air + drips, mix=0.4)


def ambience_drowned():
    seconds = 26.0
    t = synth.t_axis(seconds)
    water = lowpass(noise(seconds, 3), 420) * 0.22
    base = drone(46.0, seconds, partials=(1.0, 2.0, 2.5), detune=0.4, lfo=0.05) * 0.4
    bubbles = np.zeros_like(t)
    for moment in (4.5, 8.0, 13.4, 19.2, 23.5):
        index = int(moment * RATE)
        length = min(len(t) - index, int(0.6 * RATE))
        if length > 0:
            add_into(bubbles, index, length, ping(320 + moment * 20, length / RATE, decay=20.0), 0.05)
    return reverb(water + base + bubbles, mix=0.5)


def ambience_living():
    seconds = 26.0
    t = synth.t_axis(seconds)
    pulse_rate = 0.55
    pulse = (0.5 + 0.5 * np.sin(2 * np.pi * pulse_rate * t)) ** 2
    wet = lowpass(noise(seconds, 7), 300) * 0.18
    base = drone(34.0, seconds, partials=(1.0, 1.25, 1.75), detune=0.6, lfo=0.09) * 0.45
    heartbeat = lowpass(ping(52.0, seconds, decay=0.0, harmonics=(1.0, 0.2)), 120) * pulse * 0.25
    return reverb(wet + base + heartbeat, mix=0.35)


def ambience_memorial():
    seconds = 30.0
    t = synth.t_axis(seconds)
    base = drone(72.0, seconds, partials=(1.0, 1.5, 2.0, 3.0), detune=0.2, lfo=0.03) * 0.35
    air = lowpass(noise(seconds, 21), 900) * 0.05
    # four notes, the music box tune, very far away
    tune = np.zeros_like(t)
    for index, freq in enumerate((440.0, 392.0, 349.2, 440.0)):
        moment = 5.0 + index * 2.6
        start = int(moment * RATE)
        length = min(len(t) - start, int(3.0 * RATE))
        if length > 0:
            add_into(tune, start, length, ping(freq, length / RATE, decay=1.6), 0.05)
    return reverb(base + air + tune, mix=0.6)


def ambience_heart():
    seconds = 30.0
    base = drone(28.0, seconds, partials=(1.0, 1.5, 2.0, 4.0), detune=0.9, lfo=0.02) * 0.5
    shimmer = drone(196.0, seconds, partials=(1.0, 2.5, 5.0), detune=1.2, lfo=0.11) * 0.12
    return reverb(base + shimmer, mix=0.55)


def ambience_safe():
    seconds = 24.0
    t = synth.t_axis(seconds)
    base = drone(110.0, seconds, partials=(1.0, 1.5, 2.0), detune=0.1, lfo=0.06) * 0.22
    crackle = highpass(noise(seconds, 5), 3000) * 0.02 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.3 * t))
    return reverb(base + crackle, mix=0.3)


# ================================================================== small ambience =======

def drip():
    seconds = 1.8
    body = ping(1180.0, seconds, decay=9.0)
    tail = lowpass(noise(seconds, 41), 2000) * 0.02
    return reverb(body * 0.5 + tail, mix=0.7)


def rumble():
    seconds = 6.0
    body = lowpass(noise(seconds, 13), 90) * envelope_slow(seconds)
    return reverb(body, mix=0.4)


def envelope_slow(seconds, peak=0.5):
    t = synth.t_axis(seconds)
    return np.exp(-((t - seconds * peak) ** 2) / (2 * (seconds * 0.28) ** 2))


def wind():
    seconds = 8.0
    body = bandpass(noise(seconds, 19), 200, 900)
    t = synth.t_axis(seconds)
    shape = 0.5 + 0.5 * np.sin(2 * np.pi * 0.08 * t + 0.4)
    return reverb(body * shape * 0.5, mix=0.5)


def stone_settle():
    seconds = 2.6
    body = lowpass(noise(seconds, 23), 240) * exp_decay(seconds, 2.2)
    crack = bandpass(noise(seconds, 29), 900, 2600) * exp_decay(seconds, 9.0) * 0.25
    return reverb(body + crack, mix=0.6)


def whisper(name_seed, seconds=3.6, close=False):
    body = whisper_seed(seconds, name_seed, 500 if not close else 400, 2200 if not close else 3000)
    return fade(np.tanh(body * 2.0) * 0.35, 1.2, 1.4)


def breath():
    seconds = 3.2
    inhale = bandpass(noise(seconds, 31), 300, 1200) * np.linspace(0, 1, int(RATE * seconds)) ** 2
    return fade(inhale * 0.25, 0.6, 1.0)


def heartbeat():
    seconds = 1.6
    first = lowpass(ping(48.0, seconds, decay=7.0, harmonics=(1.0, 0.15)), 160)
    out = first.copy()
    shift = int(0.28 * RATE)
    add_into(out, shift, len(first) - shift, first[:len(first) - shift], 0.6)
    return out * 0.9


def bell():
    seconds = 6.0
    out = ping(196.0, seconds, decay=1.4) * 0.5
    add_into(out, 0, int(RATE * seconds), ping(392.0, seconds, decay=2.2), 0.2)
    add_into(out, 0, int(RATE * seconds), ping(263.0, seconds, decay=1.8), 0.15)
    return reverb(out, mix=0.7)


# ======================================================================= entities ========

def hollowed_idle():
    seconds = 2.4
    t = synth.t_axis(seconds)
    growl = lowpass(noise(seconds, 51), 260) * 0.5
    tone = sine(np.linspace(74, 62, len(t)), seconds) * 0.4
    return fade(soften(growl + tone) * env(seconds, 0.4, 0.6, 0.5, 0.8), 0.4, 0.7)


def hollowed_hurt():
    seconds = 1.1
    t = synth.t_axis(seconds)
    tone = sine(np.linspace(150, 96, len(t)), seconds) * 0.5
    return fade(soften(tone + lowpass(noise(seconds, 53), 700) * 0.3), 0.02, 0.5)


def hollowed_death():
    seconds = 2.8
    t = synth.t_axis(seconds)
    tone = sine(np.linspace(120, 40, len(t)), seconds) * 0.5
    return fade(soften(tone) * exp_decay(seconds, 1.4), 0.05, 1.2)


def hollowed_step():
    seconds = 0.5
    body = lowpass(noise(seconds, 57), 380) * exp_decay(seconds, 26.0)
    return reverb(body * 0.7, mix=0.3)


def paperface_watch():
    seconds = 2.2
    rustle = bandpass(noise(seconds, 61), 1800, 5200) * 0.25
    page = bandpass(noise(seconds, 67), 900, 2400) * exp_decay(seconds, 3.0) * 0.3
    return fade(rustle * env(seconds, 0.6, 0.4, 0.4, 0.8) + page, 0.5, 0.8)


def paperface_attack():
    seconds = 0.9
    return fade(bandpass(noise(seconds, 71), 1200, 6000) * exp_decay(seconds, 8.0) * 0.6, 0.01, 0.4)


def stitcher_ambient():
    seconds = 3.4
    t = synth.t_axis(seconds)
    thread = bandpass(noise(seconds, 73), 2500, 6000) * (0.4 + 0.4 * np.sin(2 * np.pi * 5.0 * t))
    low = drone(41.0, seconds, partials=(1.0, 2.0), detune=0.5) * 0.3
    return fade((thread * 0.12 + low) * env(seconds, 0.7, 0.5, 0.5, 1.0), 0.7, 0.9)


def stitcher_attack():
    seconds = 1.5
    scrape = bandpass(noise(seconds, 79), 700, 3000) * exp_decay(seconds, 4.0) * 0.6
    return fade(soften(scrape), 0.01, 0.7)


def weeping_cry():
    seconds = 5.5
    t = synth.t_axis(seconds)
    pitch = 320 + 40 * np.sin(2 * np.pi * 0.35 * t) - 60 * t / seconds
    sob = sine(pitch, seconds) * (0.4 + 0.3 * np.sin(2 * np.pi * 0.8 * t))
    air = bandpass(noise(seconds, 83), 500, 2000) * 0.12
    shape = env(seconds, 0.9, 1.2, 0.45, 1.8)
    return fade((sob * 0.3 + air) * shape, 0.9, 1.6)


def weeping_soft():
    seconds = 4.0
    t = synth.t_axis(seconds)
    breathy = bandpass(noise(seconds, 89), 400, 1500) * 0.18
    hum = sine(np.linspace(210, 198, len(t)), seconds) * 0.12
    return fade((breathy + hum) * env(seconds, 1.2, 0.6, 0.5, 1.4), 1.0, 1.2)


def choir_sing():
    seconds = 7.0
    t = synth.t_axis(seconds)
    out = np.zeros_like(t)
    for index, freq in enumerate((110.0, 146.83, 164.81, 220.0)):
        vibrato = 1.0 + 0.004 * np.sin(2 * np.pi * (4.5 + index) * t)
        out += np.sin(2 * np.pi * freq * t * vibrato) * (0.25 / (index + 1))
    return fade(reverb(out * env(seconds, 1.6, 1.0, 0.6, 2.4), mix=0.6), 1.4, 2.2)


def moth_flutter():
    seconds = 0.8
    t = synth.t_axis(seconds)
    flap = bandpass(noise(seconds, 97), 1200, 4000) * (0.5 + 0.5 * np.sin(2 * np.pi * 22 * t))
    return fade(flap * 0.25, 0.02, 0.3)


def echo_voice():
    seconds = 3.0
    t = synth.t_axis(seconds)
    voice = sine(np.linspace(240, 210, len(t)), seconds) * 0.3
    breathy = bandpass(noise(seconds, 101), 600, 2400) * 0.1
    return fade(reverb((voice + breathy) * env(seconds, 0.4, 0.8, 0.4, 1.2), mix=0.8), 0.4, 1.0)


def lampkeeper_roar():
    seconds = 4.5
    t = synth.t_axis(seconds)
    low = sine(np.linspace(64, 44, len(t)), seconds) * 0.6
    grit = lowpass(noise(seconds, 103), 500) * 0.4
    return fade(soften(low + grit, 0.6) * env(seconds, 0.5, 1.0, 0.5, 1.6), 0.4, 1.5)


def lampkeeper_phase():
    seconds = 5.0
    t = synth.t_axis(seconds)
    rising = sine(np.linspace(70, 210, len(t)), seconds) * 0.4
    shimmer = drone(420.0, seconds, partials=(1.0, 1.5, 2.0), detune=2.0) * 0.2
    return fade(reverb((rising + shimmer) * env(seconds, 1.0, 1.0, 0.6, 1.8), mix=0.6), 1.0, 1.8)


def lampkeeper_death():
    seconds = 9.0
    t = synth.t_axis(seconds)
    fall = sine(np.linspace(220, 40, len(t)), seconds) * 0.4
    wind = lowpass(noise(seconds, 107), 500) * 0.25
    return fade(reverb((fall + wind) * env(seconds, 0.8, 2.0, 0.4, 4.0), mix=0.7), 0.8, 3.5)


# ==================================================================== interaction =======

def gate_open():
    seconds = 2.2
    scrape = bandpass(noise(seconds, 109), 300, 2200) * exp_decay(seconds, 2.4)
    thunk = lowpass(ping(70.0, seconds, decay=8.0), 200) * 0.5
    return fade(reverb(scrape * 0.5 + thunk, mix=0.5), 0.02, 0.8)


def gate_locked():
    seconds = 1.2
    click = bandpass(noise(seconds, 113), 1500, 5000) * exp_decay(seconds, 22.0)
    body = lowpass(ping(120.0, seconds, decay=16.0), 400) * 0.4
    return fade(click * 0.5 + body, 0.01, 0.5)


def altar_activate():
    seconds = 3.0
    hum = drone(220.0, seconds, partials=(1.0, 2.0, 3.0), detune=0.6) * 0.25
    return fade(reverb(hum * env(seconds, 0.6, 0.6, 0.6, 1.2), mix=0.5), 0.5, 1.2)


def trap_trigger():
    seconds = 1.4
    snap = bandpass(noise(seconds, 127), 800, 4000) * exp_decay(seconds, 14.0)
    return fade(snap * 0.6, 0.01, 0.6)


def page_turn():
    seconds = 0.7
    return fade(bandpass(noise(seconds, 131), 1500, 6000) * exp_decay(seconds, 12.0) * 0.45, 0.01, 0.3)


def key_insert():
    seconds = 1.1
    metal = bandpass(noise(seconds, 137), 2000, 7000) * exp_decay(seconds, 18.0) * 0.4
    return fade(metal + lowpass(ping(340.0, seconds, decay=14.0), 900) * 0.2, 0.01, 0.4)


def lantern_light():
    seconds = 1.6
    ignite = bandpass(noise(seconds, 139), 400, 2600) * exp_decay(seconds, 5.0) * 0.5
    warm = ping(392.0, seconds, decay=4.0) * 0.15
    return fade(reverb(ignite + warm, mix=0.4), 0.02, 0.8)


def lantern_empty():
    seconds = 1.2
    puff = lowpass(noise(seconds, 149), 700) * exp_decay(seconds, 7.0) * 0.4
    return fade(puff, 0.02, 0.6)


def lore_discover():
    seconds = 4.0
    out = ping(523.25, seconds, decay=1.4) * 0.3
    add_into(out, 0, int(RATE * seconds * 0.8), ping(392.0, seconds * 0.8, decay=1.8), 0.2)
    return fade(reverb(out, mix=0.7), 0.05, 1.6)


def memory_return():
    seconds = 3.2
    out = ping(659.25, seconds, decay=2.0) * 0.25
    add_into(out, 0, int(RATE * seconds), ping(987.77, seconds, decay=3.0), 0.12)
    return fade(reverb(out, mix=0.8), 0.1, 1.4)


def stinger_soft():
    seconds = 5.0
    t = synth.t_axis(seconds)
    cluster = drone(146.83, seconds, partials=(1.0, 1.06, 1.5), detune=0.8, lfo=0.05) * 0.25
    return fade(cluster * env(seconds, 1.8, 1.0, 0.5, 1.8), 1.6, 1.8)


def stinger_deep():
    seconds = 6.0
    t = synth.t_axis(seconds)
    cluster = drone(73.42, seconds, partials=(1.0, 1.03, 2.0, 3.0), detune=1.2, lfo=0.03) * 0.35
    return fade(cluster * env(seconds, 2.2, 1.2, 0.5, 2.0), 2.0, 2.2)


# ========================================================================== music =======

def pad_chord(freqs, seconds, amplitude=0.2, detune=0.6):
    out = np.zeros(int(RATE * seconds))
    for freq in freqs:
        for offset in (-detune, 0.0, detune):
            t = synth.t_axis(seconds)
            vibrato = 1.0 + 0.0015 * np.sin(2 * np.pi * 0.13 * t + freq)
            out += np.sin(2 * np.pi * (freq + offset) * t * vibrato) * amplitude / len(freqs)
    return out


def melody(notes, seconds, amplitude=0.18, decay=1.4, octave=1.0):
    out = np.zeros(int(RATE * seconds))
    for start, duration, freq in notes:
        index = int(start * RATE)
        length = min(len(out) - index, int(duration * RATE))
        if length <= 0:
            continue
        add_into(out, index, length, ping(freq * octave, length / RATE, decay=decay), amplitude)
    return out


def music_halls():
    seconds = 62.0
    out = pad_chord([110.0, 164.81, 220.0], seconds, 0.16) * synth.fade(np.ones(int(RATE * seconds)), 4, 6)
    out += melody([(4, 3.0, 440.0), (9, 2.2, 392.0), (14, 4.0, 329.63), (22, 3.0, 440.0),
                   (27, 2.0, 493.88), (31, 4.0, 392.0), (40, 3.0, 329.63), (46, 5.0, 246.94)],
                  seconds, 0.12, 1.2)
    return reverb(out, mix=0.55)


def music_drowned():
    seconds = 66.0
    out = pad_chord([98.0, 146.83, 196.0], seconds, 0.15) * synth.fade(np.ones(int(RATE * seconds)), 5, 7)
    out += pad_chord([87.31, 130.81, 174.61], seconds, 0.1)
    out += melody([(6, 4.0, 293.66), (12, 3.0, 261.63), (18, 5.0, 220.0), (28, 4.0, 196.0),
                   (36, 3.0, 261.63), (44, 6.0, 174.61)], seconds, 0.1, 1.0)
    return reverb(out, mix=0.65)


def music_living():
    seconds = 58.0
    t = synth.t_axis(seconds)
    out = pad_chord([58.27, 87.31, 116.54], seconds, 0.18)
    pulse = (0.5 + 0.5 * np.sin(2 * np.pi * 0.45 * t)) ** 3
    out += lowpass(ping(58.27, seconds, decay=0.0, harmonics=(1.0, 0.4)), 160) * pulse * 0.18
    out += melody([(10, 4.0, 233.08), (18, 4.0, 174.61), (26, 6.0, 146.83)], seconds, 0.08, 0.8)
    return reverb(out * synth.fade(np.ones(int(RATE * seconds)), 4, 6), mix=0.5)


def music_memorial():
    seconds = 72.0
    out = pad_chord([130.81, 164.81, 196.0], seconds, 0.14) * synth.fade(np.ones(int(RATE * seconds)), 5, 8)
    out += melody([(3, 2.0, 523.25), (6, 2.0, 440.0), (9, 3.0, 392.0), (14, 2.0, 440.0),
                   (17, 4.0, 349.23), (24, 3.0, 392.0), (28, 2.0, 523.25), (32, 5.0, 440.0),
                   (40, 3.0, 349.23), (45, 4.0, 293.66), (52, 6.0, 261.63)], seconds, 0.13, 1.3)
    return reverb(out, mix=0.6)


def music_heart():
    seconds = 68.0
    t = synth.t_axis(seconds)
    out = pad_chord([55.0, 82.41, 110.0, 164.81], seconds, 0.2, detune=1.4)
    shimmer = pad_chord([440.0, 554.37, 659.25], seconds, 0.05, detune=2.0)
    swell = 0.6 + 0.4 * np.sin(2 * np.pi * 0.02 * t)
    return reverb((out + shimmer) * swell * synth.fade(np.ones(int(RATE * seconds)), 6, 9), mix=0.6)


def music_boss():
    seconds = 58.0
    t = synth.t_axis(seconds)
    out = pad_chord([49.0, 73.42, 98.0], seconds, 0.22, detune=1.0)
    beat = (0.5 + 0.5 * np.sin(2 * np.pi * 1.6 * t)) ** 6
    out += lowpass(ping(49.0, seconds, decay=0.0, harmonics=(1.0, 0.5)), 140) * beat * 0.3
    out += melody([(8, 3.0, 196.0), (14, 2.0, 233.08), (18, 2.0, 196.0), (24, 4.0, 174.61),
                   (32, 3.0, 196.0), (38, 5.0, 146.83)], seconds, 0.12, 0.9)
    return reverb(out * synth.fade(np.ones(int(RATE * seconds)), 3, 8), mix=0.5)


def music_ending():
    seconds = 78.0
    out = pad_chord([110.0, 164.81, 220.0, 261.63], seconds, 0.16) * synth.fade(np.ones(int(RATE * seconds)), 6, 10)
    out += melody([(2, 3.0, 440.0), (7, 2.5, 493.88), (11, 4.0, 523.25), (18, 3.0, 440.0),
                   (24, 4.0, 392.0), (30, 3.0, 440.0), (36, 5.0, 523.25), (44, 4.0, 659.25),
                   (50, 6.0, 587.33), (60, 8.0, 440.0)], seconds, 0.14, 1.5)
    return reverb(out, mix=0.65)


def main():
    # ambient layers
    write("ambience/halls", ambience_halls(), 0.62, 3.0)
    write("ambience/drowned", ambience_drowned(), 0.62, 3.0)
    write("ambience/living", ambience_living(), 0.6, 3.0)
    write("ambience/memorial", ambience_memorial(), 0.6, 3.0)
    write("ambience/heart", ambience_heart(), 0.6, 3.0)
    write("ambience/safe", ambience_safe(), 0.55, 3.0)
    # small ambience
    write("ambient/drip", drip(), 0.55)
    write("ambient/rumble", rumble(), 0.55)
    write("ambient/wind", wind(), 0.5)
    write("ambient/stone_settle", stone_settle(), 0.6)
    write("ambient/whisper", whisper(17), 0.5, 1.2)
    write("ambient/whisper_close", whisper(23, close=True), 0.55, 1.2)
    write("ambient/breath", breath(), 0.5)
    write("ambient/heartbeat", heartbeat(), 0.7)
    write("ambient/bell", bell(), 0.55)
    # entities
    write("entity/hollowed/idle", hollowed_idle(), 0.6)
    write("entity/hollowed/hurt", hollowed_hurt(), 0.6)
    write("entity/hollowed/death", hollowed_death(), 0.6)
    write("entity/hollowed/step", hollowed_step(), 0.5)
    write("entity/paperface/watch", paperface_watch(), 0.55)
    write("entity/paperface/attack", paperface_attack(), 0.6)
    write("entity/stitcher/ambient", stitcher_ambient(), 0.55)
    write("entity/stitcher/attack", stitcher_attack(), 0.65)
    write("entity/weeping/cry", weeping_cry(), 0.55, 1.4)
    write("entity/weeping/soft", weeping_soft(), 0.5, 1.4)
    write("entity/choir/sing", choir_sing(), 0.55, 2.4)
    write("entity/moth/flutter", moth_flutter(), 0.4)
    write("entity/echo/voice", echo_voice(), 0.5, 1.2)
    write("entity/lampkeeper/roar", lampkeeper_roar(), 0.75)
    write("entity/lampkeeper/phase", lampkeeper_phase(), 0.7)
    write("entity/lampkeeper/death", lampkeeper_death(), 0.7, 3.0)
    # interaction
    write("block/gate_open", gate_open(), 0.6)
    write("block/gate_locked", gate_locked(), 0.6)
    write("block/altar_activate", altar_activate(), 0.55)
    write("block/trap_trigger", trap_trigger(), 0.6)
    write("item/page_turn", page_turn(), 0.5)
    write("item/key_insert", key_insert(), 0.55)
    write("item/lantern_light", lantern_light(), 0.6)
    write("item/lantern_empty", lantern_empty(), 0.5)
    write("item/lore_discover", lore_discover(), 0.55)
    write("item/memory_return", memory_return(), 0.55)
    write("horror/stinger_soft", stinger_soft(), 0.5, 1.8)
    write("horror/stinger_deep", stinger_deep(), 0.5, 2.0)
    # music
    write("music/halls", music_halls(), 0.55, 6.0, stream=True)
    write("music/drowned", music_drowned(), 0.55, 6.0, stream=True)
    write("music/living", music_living(), 0.55, 6.0, stream=True)
    write("music/memorial", music_memorial(), 0.55, 7.0, stream=True)
    write("music/heart", music_heart(), 0.55, 7.0, stream=True)
    write("music/boss", music_boss(), 0.6, 6.0, stream=True)
    write("music/ending", music_ending(), 0.55, 8.0, stream=True)

    with open(os.path.join(ROOT, "sounds.json"), "w", encoding="utf-8") as handle:
        json.dump(SOUNDS, handle, indent=2)
        handle.write("\n")
    print(f"wrote {len(SOUNDS)} sound events")


if __name__ == "__main__":
    main()
