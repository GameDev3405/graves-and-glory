#!/usr/bin/env python3
"""Converts the Pixabay sounds and music (docs/AUDIO.md, Credits) into the mod's Ogg files.

Usage: python tools/audio/import_pixabay.py <folder with the downloaded mp3s> [out_name ...]

One-shots: mono 44.1 kHz Ogg Vorbis q4, leading silence trimmed, cut to a useful length (under 1.5 s for combat),
a short fade at both ends, and a common loudness inside each group (gated RMS of the active part, then a soft peak limit)
so the variants of one sound match each other. Music: stereo 44.1 kHz q4, leading silence trimmed, EBU R128 loudnorm
(two passes), a fade at the end. numpy and ffmpeg on PATH. Output: src/main/resources/assets/gravesandglory/sounds/.
"""
import json, subprocess, sys
from pathlib import Path
import numpy as np

SR = 44100
OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/gravesandglory/sounds"

# out name -> (source file, start seconds or None = first onset, length seconds, fade-out seconds, group)
# A start of None finds the first onset 40 dB under the clip's peak and begins 10 ms before it.
ONE_SHOTS = {
    "shield_block_1": ("yodguard-shield_impact-1-382410.mp3", None, 0.70, 0.20, "shield"),
    "shield_block_2": ("yodguard-shield_impact-4-382413.mp3", None, 0.70, 0.20, "shield"),
    "shield_block_3": ("yodguard-shield_impact-5-382415.mp3", None, 0.70, 0.20, "shield"),
    "heavy_metal_hit_1": ("yodguard-a-massive-axe-impact-hitting-metal-3-450254.mp3", None, 1.40, 0.60, "heavy"),
    "sword_swing_1": ("dragon-studio-sword-slice-393847.mp3", None, 0.80, 0.25, "swing"),
    "sword_swing_2": ("dragon-studio-sword-slice-2-393845.mp3", None, 0.80, 0.25, "swing"),
    "sword_clash_1": ("dragon-studio-sword-clashhit-393837.mp3", None, 0.80, 0.25, "clash"),
    "sword_clash_2": ("freesound_community-sword-against-sword-6341 (2).mp3", 10.27, 0.90, 0.30, "clash"),
    "sword_clash_3": ("freesound_community-sword-against-sword-6341 (2).mp3", 20.44, 0.90, 0.30, "clash"),
    "sword_clash_4": ("freesound_community-sword-against-sword-6341 (2).mp3", 33.13, 0.90, 0.30, "clash"),
    "armor_hit_1": ("dragon-studio-armor-impact-from-sword-393843.mp3", None, 0.90, 0.30, "armor"),
    "bow_shoot_1": ("u_2c8g87fmmx-bow-sound-596323.mp3", 1.56, 0.90, 0.30, "bow"),
    "arrow_hit_1": ("dennish18-arrow-body-impact-146419.mp3", None, 0.55, 0.12, "arrow"),
    "magic_cast_1": ("daviddumaisaudio-magical-spell-cast-190272.mp3", None, 1.40, 0.50, "cast"),
    "magic_impact_1": ("rescopicsound-elemental-magic-spell-impact-outgoing-228342.mp3", None, 1.40, 0.55, "mimpact"),
    "magic_impact_2": ("dragon-studio-earth-spell-impact-478381.mp3", 2.30, 1.40, 0.60, "mimpact"),
    "clock_tick_1": ("oxidvideos-clock-ticking-582761.mp3", 5.04, 0.40, 0.15, "clock"),
    "clock_tick_2": ("oxidvideos-clock-ticking-582761.mp3", 7.04, 0.40, 0.15, "clock"),
    "battle_drums": ("dragon-studio-ancient-war-drums-463214.mp3", None, 6.00, 1.50, "drums"),
}
# Gated RMS target of the active part, in dBFS, per group (the AudioCues volume balances the groups against each other).
TARGET = {"shield": -17, "heavy": -17, "swing": -20, "clash": -17, "armor": -18, "bow": -18, "arrow": -18,
          "cast": -19, "mimpact": -18, "clock": -22, "drums": -18}
MUSIC = {
    "music_calm_long_day": "purrplecat-long-day-518602.mp3",
    "music_epic_rebel_moon": "audioatlant-rebel-moon-inspiring-emotional-epic-cinematic-trailer-short-1-628477.mp3",
    "music_battle_dark_grim": "alex_besss-dark-and-grim-epic-trailer-short-628100.mp3",
    "music_battle_drums_stomp": "miromaxmusic-epic-drums-stomp-percussion-no-copyright-628251.mp3",
}
MUSIC_LUFS = -21
MUSIC_FADE = 2.0


def decode(path, channels):
    r = subprocess.run(["ffmpeg", "-v", "error", "-i", str(path), "-ac", str(channels), "-ar", str(SR), "-f", "f32le", "-"], capture_output=True, check=True)
    x = np.frombuffer(r.stdout, dtype=np.float32)
    return x.reshape(-1, channels) if channels > 1 else x


def envelope_db(x, window=0.01):
    w = int(SR * window)
    n = len(x) // w
    return 20 * np.log10(np.sqrt((x[:n * w].reshape(n, w) ** 2).mean(1)) + 1e-9)


def onset(x):
    db = envelope_db(x)
    return max(0.0, int(np.argmax(db > db.max() - 40)) * 0.01 - 0.01)


def gated_rms_db(x):
    w = int(SR * 0.01)
    n = len(x) // w
    frames = x[:n * w].reshape(n, w)
    power = (frames ** 2).mean(1)
    db = 10 * np.log10(power + 1e-12)
    keep = db > db.max() - 30
    return 10 * np.log10(power[keep].mean() + 1e-12)


def soft_limit(x, knee=0.7):
    a = np.abs(x)
    over = a > knee
    out = x.copy()
    out[over] = np.sign(x[over]) * (knee + (1 - knee) * np.tanh((a[over] - knee) / (1 - knee)))
    return out


def encode(samples, channels, dest, extra=()):
    dest.parent.mkdir(parents=True, exist_ok=True)
    cmd = ["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", str(channels), "-i", "-", *extra,
           "-c:a", "libvorbis", "-q:a", "4", "-ar", str(SR), "-ac", str(channels), "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", str(dest)]
    subprocess.run(cmd, input=np.ascontiguousarray(samples, dtype=np.float32).tobytes(), check=True)


def one_shot(src, start, length, fade, group, dest):
    x = decode(src, 1)
    begin = onset(x) if start is None else start
    clip = x[int(begin * SR):int((begin + length) * SR)].copy()
    clip *= 10 ** ((TARGET[group] - gated_rms_db(clip)) / 20)
    clip = soft_limit(clip) * 0.97
    n_in = int(0.004 * SR)
    clip[:n_in] *= np.linspace(0, 1, n_in)
    n_out = min(len(clip), int(fade * SR))
    clip[-n_out:] *= np.cos(np.linspace(0, np.pi / 2, n_out)) ** 2
    encode(clip, 1, dest)
    return len(clip) / SR


def music(src, dest):
    x = decode(src, 2)
    mono = x.mean(1)
    begin = onset(mono)
    end = len(mono) / SR
    x = x[int(begin * SR):]
    raw = np.ascontiguousarray(x, dtype=np.float32).tobytes()
    fade_start = len(x) / SR - MUSIC_FADE
    common = ["-f", "f32le", "-ar", str(SR), "-ac", "2", "-i", "-"]
    first = subprocess.run(["ffmpeg", "-v", "info", "-y", *common, "-af", f"loudnorm=I={MUSIC_LUFS}:TP=-2:LRA=9:print_format=json", "-f", "null", "-"], input=raw, capture_output=True, check=True)
    text = first.stderr.decode("utf8", "replace")
    stats = json.loads(text[text.rindex("{"):text.rindex("}") + 1])
    flt = (f"loudnorm=I={MUSIC_LUFS}:TP=-2:LRA=9:measured_I={stats['input_i']}:measured_TP={stats['input_tp']}:measured_LRA={stats['input_lra']}"
           f":measured_thresh={stats['input_thresh']}:offset={stats['target_offset']}:linear=true,afade=t=in:d=0.05,afade=t=out:st={fade_start:.2f}:d={MUSIC_FADE}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", *common, "-af", flt, "-c:a", "libvorbis", "-q:a", "4", "-ar", str(SR), "-ac", "2", "-map_metadata", "-1", "-fflags", "+bitexact", "-flags:a", "+bitexact", str(dest)], input=raw, check=True)
    return (end - begin)


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    folder = Path(sys.argv[1])
    only = set(sys.argv[2:])
    for name, (file, start, length, fade, group) in ONE_SHOTS.items():
        if only and name not in only:
            continue
        seconds = one_shot(folder / file, start, length, fade, group, OUT / f"{name}.ogg")
        print(f"{name}.ogg  {seconds:.2f} s")
    for name, file in MUSIC.items():
        if only and name not in only:
            continue
        seconds = music(folder / file, OUT / f"{name}.ogg")
        print(f"{name}.ogg  {seconds:.1f} s")


if __name__ == "__main__":
    main()
