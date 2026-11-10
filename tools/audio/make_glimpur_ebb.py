"""Make the three glimpur_ebb takes: a faint gutter of failing magic (chapter 1 ebb, ~1.5 s each).

Shape follows the game's flicker (MarchMath.Ebb: 30 ticks): the glow sputters for 12 ticks, stays dim,
then climbs back over the last 6. Raw material: the CC0 "Sparkly" shimmer by opticaillusions
(https://freesound.org/people/opticaillusions/sounds/554312/, CC0 1.0), so the output needs no credit.

Usage (needs numpy, scipy, soundfile and ffmpeg; the music venv in work/tools/midi has them):
    python tools/audio/make_glimpur_ebb.py SOURCE.ogg OUT_DIR
Writes OUT_DIR/glimpur_ebb_1.ogg .. _3.ogg (mono, 44.1 kHz, Ogg Vorbis). Seeds are fixed, so reruns match.
"""
import re, subprocess, sys, tempfile
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt

FFMPEG = "C:/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg.exe"
SR = 44100
LEN = 1.5            # seconds; 30 game ticks
SPUTTER = 0.6        # 12 ticks of sputtering
CLIMB = 0.3          # last 6 ticks
TARGET_LUFS = -28.0  # faint on purpose; the cue volume does the rest. All three takes are matched to this.
MAX_PEAK_DB = -3.0


def decode(src):
    with tempfile.TemporaryDirectory() as d:
        wav = Path(d) / "s.wav"
        subprocess.run([FFMPEG, "-hide_banner", "-loglevel", "error", "-y", "-i", src, "-ac", "1", "-ar", str(SR), str(wav)], check=True)
        x, _ = sf.read(wav, dtype="float64")
    return x


def best_windows(x, n=3, sep=2.0):
    """The n loudest, non-overlapping 1.5 s windows (hop 0.25 s)."""
    w, hop = int(LEN * SR), int(0.25 * SR)
    scores = [(np.sqrt(np.mean(x[i:i + w] ** 2)), i) for i in range(0, len(x) - w, hop)]
    scores.sort(reverse=True)
    picked = []
    for _, i in scores:
        if all(abs(i - j) > sep * SR for j in picked):
            picked.append(i)
        if len(picked) == n:
            break
    return sorted(picked)


def smooth(g, ms):
    k = max(1, int(SR * ms / 1000))
    return np.convolve(g, np.ones(k) / k, mode="same")


def take(x, start, seed):
    rng = np.random.default_rng(seed)
    n = int(LEN * SR)
    seg = x[start:start + n].copy()
    t = np.arange(n) / SR
    hp = sosfilt(butter(2, [1200, 9000], "bandpass", fs=SR, output="sos"), seg)
    lp = sosfilt(butter(2, 2200, "lowpass", fs=SR, output="sos"), hp)
    # filter closes while the glow dims, opens again on the climb
    w = np.interp(t, [0, SPUTTER, 1.2, LEN], [0.0, 0.5, 0.85, 0.3])
    y = hp * (1 - w) + lp * w
    # sputter gate: random on/off bursts, on-probability falling from 0.9 to 0.25
    gate = np.full(n, 0.08)
    i = 0
    while i < int(SPUTTER * SR):
        seg_len = int(rng.uniform(0.02, 0.07) * SR)
        p = np.interp(i / SR, [0, SPUTTER], [0.9, 0.25])
        if rng.random() < p:
            gate[i:i + seg_len] = 1.0
        i += seg_len
    gate[int(SPUTTER * SR):] = 1.0
    gate = smooth(gate, 4)
    # level: full, decaying to dim, then back up over the last 0.3 s
    env = np.interp(t, [0, SPUTTER, LEN - CLIMB, LEN - 0.03, LEN], [1.0, 0.35, 0.12, 0.5, 0.0])
    y = y * gate * env
    fade = int(0.01 * SR)
    y[:fade] *= np.linspace(0, 1, fade)
    return y / (np.max(np.abs(y)) or 1.0) * 0.35


def lufs(wav):
    r = subprocess.run([FFMPEG, "-hide_banner", "-nostats", "-i", str(wav), "-af", "ebur128", "-f", "null", "-"], capture_output=True, text=True)
    return float(re.findall(r"I:\s+(-?[\d.]+) LUFS", r.stderr)[-1])


def main(src, out):
    out = Path(out); out.mkdir(parents=True, exist_ok=True)
    x = decode(src)
    for k, start in enumerate(best_windows(x), start=1):
        y = take(x, start, seed=k)
        with tempfile.TemporaryDirectory() as d:
            wav = Path(d) / "t.wav"
            sf.write(wav, y, SR, subtype="PCM_16")
            gain_db = TARGET_LUFS - lufs(wav)
            gain_db = min(gain_db, MAX_PEAK_DB - 20 * np.log10(np.max(np.abs(y))))
            sf.write(wav, y * 10 ** (gain_db / 20), SR, subtype="PCM_16")
            subprocess.run([FFMPEG, "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav), "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                            str(out / f"glimpur_ebb_{k}.ogg")], check=True)
        print("wrote", out / f"glimpur_ebb_{k}.ogg", f"(source window {start / SR:.2f} s)")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    main(sys.argv[1], sys.argv[2])
