"""A tiny additive/subtractive synthesiser used to render the mod's audio.

Everything is generated from sine partials, filtered noise and slow envelopes. No samples are
used, so every sound in Hollowlight is original. The aesthetic rules for the whole set:

* no transients louder than -6 dBFS (nothing is allowed to startle by volume alone),
* fades always long enough to hide the start (whispers and stingers fade in over a second),
* every sound ends in a decaying tail rather than being cut off.
"""
from __future__ import annotations

import numpy as np

RATE = 44100


def t_axis(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def sine(freq, seconds, phase=0.0):
    t = t_axis(seconds)
    if np.isscalar(freq):
        return np.sin(2 * np.pi * freq * t + phase)
    freq = np.asarray(freq)
    if len(freq) != len(t):
        freq = np.interp(np.linspace(0, 1, len(t)), np.linspace(0, 1, len(freq)), freq)
    phase_accum = 2 * np.pi * np.cumsum(freq) / RATE
    return np.sin(phase_accum + phase)


def noise(seconds, seed=0):
    rng = np.random.default_rng(seed)
    return rng.uniform(-1.0, 1.0, int(RATE * seconds))


def env(seconds, attack=0.02, decay=0.2, sustain=0.6, release=0.4):
    n = int(RATE * seconds)
    a = max(1, int(RATE * attack))
    d = max(1, int(RATE * decay))
    r = max(1, int(RATE * release))
    s = max(1, n - a - d - r)
    return np.concatenate([
        np.linspace(0, 1, a),
        np.linspace(1, sustain, d),
        np.full(s, sustain),
        np.linspace(sustain, 0, r),
    ])[:n]


def exp_decay(seconds, rate=6.0):
    t = t_axis(seconds)
    return np.exp(-rate * t)


def lowpass(signal, cutoff):
    """One-pole lowpass: enough to shape noise into wind, water and breath."""
    if cutoff >= RATE / 2:
        return signal
    alpha = 1.0 - np.exp(-2 * np.pi * cutoff / RATE)
    out = np.zeros_like(signal)
    acc = 0.0
    for index in range(len(signal)):
        acc += alpha * (signal[index] - acc)
        out[index] = acc
    return out


def highpass(signal, cutoff):
    return signal - lowpass(signal, cutoff)


def bandpass(signal, low, high):
    return lowpass(highpass(signal, low), high)


def reverb(signal, taps=((0.07, 0.35), (0.13, 0.22), (0.23, 0.14), (0.41, 0.08)), mix=0.5):
    out = signal.copy()
    for delay, gain in taps:
        shift = int(RATE * delay)
        if shift >= len(signal):
            continue
        tail = np.zeros_like(signal)
        tail[shift:] = signal[:-shift] * gain
        out += tail
    return out / max(1.0, (1.0 + mix))


def normalize(signal, peak=0.82):
    maximum = np.max(np.abs(signal)) or 1.0
    return signal / maximum * peak


def fade(signal, fade_in=0.05, fade_out=0.4):
    n = len(signal)
    a = max(1, int(RATE * fade_in))
    r = max(1, int(RATE * fade_out))
    out = signal.copy()
    out[:a] *= np.linspace(0, 1, a)
    out[-r:] *= np.linspace(1, 0, r)
    return out


def soften(signal, drive=0.3):
    """Very gentle saturation: rounds off digital edges without adding loudness."""
    return np.tanh(signal * (1.0 + drive)) / np.tanh(1.0 + drive)


def ping(freq, seconds, decay=8.0, harmonics=(1.0, 0.5, 0.25)):
    t = t_axis(seconds)
    out = np.zeros_like(t)
    for index, amplitude in enumerate(harmonics):
        out += amplitude * np.sin(2 * np.pi * freq * (index + 1) * t)
    return out * np.exp(-decay * t)


def drone(base, seconds, partials=(1.0, 1.5, 2.0, 3.0), detune=0.4, lfo=0.07):
    t = t_axis(seconds)
    out = np.zeros_like(t)
    for index, ratio in enumerate(partials):
        for offset in (-detune, detune):
            freq = base * ratio + offset * (index + 1)
            wobble = 1.0 + 0.002 * np.sin(2 * np.pi * lfo * (index + 1) * t)
            out += (1.0 / (index + 2)) * np.sin(2 * np.pi * freq * t * wobble + index)
    return out / len(partials)


def whisper_seed(seconds, seed, low=600, high=2400):
    base = noise(seconds, seed)
    shaped = bandpass(base, low, high)
    gate = np.abs(np.sin(2 * np.pi * (0.6 + seed * 0.05) * t_axis(seconds))) ** 3
    return shaped * gate
