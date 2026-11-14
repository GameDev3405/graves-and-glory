#!/usr/bin/env python3
"""Converts the drop board's Pixabay sounds (docs/AUDIO-CREDITS.md, Drop board) into the mod's Ogg files.

Usage: python tools/audio/import_dropboard.py <folder with the downloaded mp3s> [out_name ...]

The same treatment as import_pixabay.py's one-shots (mono 44.1 kHz Ogg Vorbis q4, cut, faded, one loudness per group); the cut starts
where the strike is, so a peg rings on the frame the coin meets it. numpy and ffmpeg on PATH.
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import import_pixabay as ip

# out name -> (source file, start seconds or None = first onset, length seconds, fade-out seconds, group)
ONE_SHOTS = {
    "drop_peg_1": ("freesound_community-metal-ping-192khz-2-105051.mp3", None, 0.40, 0.25, "peg"),
    "drop_peg_2": ("freesound_community-metal-ping-192khz-86912.mp3", None, 0.40, 0.25, "peg"),
    "drop_peg_3": ("freesound_community-ting-61753.mp3", 0.47, 0.35, 0.20, "peg"),
    "drop_wall_1": ("freesound_community-high-clank-86142.mp3", None, 0.45, 0.20, "wall"),
    "drop_release_1": ("freesound_community-coin-flip-88793.mp3", None, 1.10, 0.50, "release"),
    "drop_land_1": ("bfcmusic-coin-drop-in-slot-3-sfx-330556.mp3", 0.54, 1.00, 0.45, "land"),
    "drop_fly_1": ("dragon-studio-whoosh-cinematic-376875.mp3", None, 2.00, 0.50, "fly"),
    "drop_flare_1": ("freesound_gamestudio-hero-skill-attack-reveal-13-388903.mp3", None, 2.00, 0.80, "flare"),
    "drop_jackpot_1": ("universfield-bell-logo-reveal-147350.mp3", None, 3.50, 1.20, "jackpot"),
    "drop_reveal_1": ("universfield-clear-bell-chime-487898.mp3", None, 1.90, 0.80, "reveal"),
}
ip.TARGET.update({"peg": -23, "wall": -19, "release": -30, "land": -18, "fly": -20, "flare": -22, "jackpot": -18, "reveal": -18})


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    folder, only = Path(sys.argv[1]), set(sys.argv[2:])
    for name, (file, start, length, fade, group) in ONE_SHOTS.items():
        if only and name not in only:
            continue
        print("%s.ogg  %.2f s" % (name, ip.one_shot(folder / file, start, length, fade, group, ip.OUT / (name + ".ogg"))))


if __name__ == "__main__":
    main()
