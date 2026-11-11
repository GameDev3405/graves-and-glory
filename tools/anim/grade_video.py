#!/usr/bin/env python3
"""Phone video of a grading round: PREVIOUS | NEW, side view over three-quarter view, from grade_page.py's renders.

  python tools/anim/grade_video.py work/grade/<round> out.mp4 [--impact 10] [--title "knight attack_overhead"]

Three loops at game speed, two at half speed, one at quarter speed, with the tick number on every frame. With no
`prev` renders in the round it shows the new clip alone.
"""
import argparse
import glob
import os
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFont

FFMPEG = os.environ.get("FFMPEG", r"C:\ffmpeg\ffmpeg-8.1-full_build\bin\ffmpeg.exe")
BG, HEAD, GAP = (16, 19, 24), 110, 8
ROWS = ("side", "three_quarter")


def font(size):
    try:
        return ImageFont.truetype("arialbd.ttf", size)
    except OSError:
        return ImageFont.load_default()


def load(root, which):
    out = {}
    for v in ROWS:
        files = sorted(glob.glob(os.path.join(root, which, "render", v, "frame_*.png")))
        if not files:
            return None
        out[v] = [Image.open(f).convert("RGB") for f in files]
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("round_dir")
    ap.add_argument("out")
    ap.add_argument("--impact", type=int)
    ap.add_argument("--title", default="")
    a = ap.parse_args(argv)
    new, prev = load(a.round_dir, "new"), load(a.round_dir, "prev")
    if not new:
        raise SystemExit("no renders under %s/new/render: run grade_page.py first" % a.round_dir)
    cols = [("PREVIOUS", prev), ("NEW", new)] if prev else [("NEW", new)]
    cw = max(s[v][0].width for _, s in cols for v in ROWS)
    rh = {v: max(s[v][0].height for _, s in cols) for v in ROWS}
    W = (len(cols) * cw + (len(cols) - 1) * GAP + 1) // 2 * 2
    H = (HEAD + sum(rh.values()) + GAP + 1) // 2 * 2
    ticks = len(new[ROWS[0]])
    big, small = font(34), font(26)
    proc = subprocess.Popen([FFMPEG, "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s",
                             "%dx%d" % (W, H), "-r", "20", "-i", "-", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf",
                             "24", "-movflags", "+faststart", a.out], stdin=subprocess.PIPE)
    plan = [(t, 1, "") for _ in range(3) for t in range(ticks)]
    plan += [(t, 2, "  HALF SPEED") for _ in range(2) for t in range(ticks)]
    plan += [(t, 4, "  QUARTER SPEED") for t in range(ticks)]
    n = 0
    for t, hold, tag in plan:
        fr = Image.new("RGB", (W, H), BG)
        d = ImageDraw.Draw(fr)
        hit = a.impact is not None and t == a.impact
        d.text((12, 8), a.title, font=big, fill=(240, 240, 240))
        d.text((12, 56), "tick %02d / %d%s%s" % (t, ticks - 1, tag, "   IMPACT" if hit else ""), font=small,
               fill=(230, 180, 60) if hit else (160, 170, 180))
        for k, (label, s) in enumerate(cols):
            x0 = k * (cw + GAP)
            d.text((x0 + cw - 12 - d.textlength(label, font=small), 56), label, font=small,
                   fill=(230, 180, 60) if label == "NEW" else (160, 170, 180))
            y = HEAD
            for v in ROWS:
                im = s[v][min(t, len(s[v]) - 1)]
                fr.paste(im, (x0 + (cw - im.width) // 2, y + (rh[v] - im.height) // 2))
                y += rh[v] + GAP
        for _ in range(hold):
            proc.stdin.write(fr.tobytes())
            n += 1
    for _ in range(10):
        proc.stdin.write(fr.tobytes())
    proc.stdin.close()
    if proc.wait():
        raise SystemExit("ffmpeg failed")
    print("%s  %dx%d  %.1f s  %.1f MB" % (a.out, W, H, (n + 10) / 20.0, os.path.getsize(a.out) / 1e6))
    return 0


if __name__ == "__main__":
    sys.exit(main())
