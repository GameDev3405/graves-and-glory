#!/usr/bin/env python3
"""Frame grading page: one clip, every tick, for Kort to grade on his phone.

  python tools/anim/grade_page.py --troop knight --clip attack_overhead --base attack_overhead_legs \
      --round knight-attack_overhead-r1 --out work/grade/knight-attack_overhead-r1 \
      [--prev-anim old.animation.json] [--phases "0-2.5 anticipation,2.5-7.6 load,..."] [--note "one line"]
      [--anim work/copy.animation.json] [--tier 3] [--geo corrected.geo.json]

Writes <out>/index.html: every tick rendered near, from the side, three-quarter and front, embedded as JPEG data URIs
(Kort's phone cannot load published files), with a player, and per tick a grade (good / ok / bad), problem tags and a
note. The page saves them in its own database under rounds/<round>/frames/t<NN> and rounds/<round>. Publish it with the
Artifact tool and capabilities {"db": {}}; read the grades back with ArtifactData (list rounds/<round>/frames) and
turn them into a fix list with `grade_page.py --read grades.json`.

It only shows what the renderer draws. focus_check and the scorecard are run first; their result is printed on the page.
"""
from __future__ import annotations

import argparse
import base64
import glob
import io
import json
import os
import re
import subprocess
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
if HERE not in sys.path:
    sys.path.insert(0, HERE)
import focus_check as fc  # noqa: E402
import scorecard as sc  # noqa: E402

REPO = sc.REPO
VIEWS = ("side", "three_quarter", "front")
TAGS = ("clipping", "stale", "weapon facing", "limb wrong", "timing", "pose", "keep this")


def render(anim, troop, clip, base, texture, out, ticks, scale, geo=None, tier=None):
    geo = geo or os.path.join(REPO, "%s/geo/troop/%s.geo.json" % (sc.ASSETS, troop))
    rig = os.path.join(REPO, "%s/troop_rig/%s.json" % (sc.ASSETS, troop))
    frames = {}
    for v in VIEWS:
        d = os.path.join(out, "render", v)
        for f in glob.glob(os.path.join(d, "*.png")):
            os.remove(f)
        cmd = [sys.executable, os.path.join(HERE, "render_geo.py"), "--geo", geo, "--texture", texture]
        if tier is not None and os.path.isfile(rig):      # one armour tier, as the game draws it
            cmd += ["--rig", rig, "--tier", str(tier)]
        for b in list(base) + [clip]:
            cmd += ["--anim", anim, "--clip", b]
        cmd += ["--ticks", "0:%d:1" % (ticks - 1), "--views", v, "--scale", str(scale), "--out", d, "--no-gif"]
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL)
        files = sorted(glob.glob(os.path.join(d, "frame_*.png")))
        if len(files) != ticks:
            raise SystemExit("render gave %d frames for %s, wanted %d" % (len(files), v, ticks))
        frames[v] = files
    return frames


def uri(path, quality, width):
    im = Image.open(path).convert("RGB")
    if im.width > width:
        im = im.resize((width, round(im.height * width / im.width)), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "JPEG", quality=quality, optimize=True)
    return "data:image/jpeg;base64," + base64.b64encode(buf.getvalue()).decode("ascii")


def phases(text, ticks):
    out = [""] * ticks
    for part in filter(None, (p.strip() for p in (text or "").split(","))):
        m = re.match(r"([\d.]+)-([\d.]+)\s+(.+)", part)
        if not m:
            raise SystemExit("bad --phases part %r" % part)
        for t in range(ticks):
            if float(m.group(1)) <= t < float(m.group(2)):
                out[t] = m.group(3)
    return out


def checks(troop, clip, anim):
    res = fc.check_troop(REPO, troop, anim=anim, only=[clip])
    rows = res[0]["results"] if res else []
    bad = [r for r in rows if r["status"] in ("FAIL", "WARN")]
    ran = sum(1 for r in rows if r["status"] != "n/a")
    return {"ran": ran, "fail": sum(1 for r in bad if r["status"] == "FAIL"),
            "rows": [{"check": r["check"], "group": r["group"], "status": r["status"], "value": r["value"],
                      "tick": r["tick"], "detail": r["detail"]} for r in rows if r["status"] != "n/a"]}


def read(path):
    """Grades exported with ArtifactData (a list of frame documents) -> the fix list, worst first."""
    docs = json.load(open(path, encoding="utf-8"))
    docs = docs.get("documents", docs) if isinstance(docs, dict) else docs
    rows = []
    for d in docs:
        b = d.get("data", d)
        if "tick" in b:
            rows.append(b)
    order = {"bad": 0, "ok": 1, "good": 2, None: 3, "": 3}
    for b in sorted(rows, key=lambda b: (order.get(b.get("grade"), 3), b["tick"])):
        print("tick %2d  %-4s  %-40s %s" % (b["tick"], b.get("grade") or "-", ", ".join(b.get("tags", [])),
                                             (b.get("note") or "").replace("\n", " / ")))
    return 0


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--read", help="print the fix list from grades saved with ArtifactData")
    ap.add_argument("--troop")
    ap.add_argument("--clip")
    ap.add_argument("--base", action="append", default=[], help="clip played under it (the legs clip); repeatable")
    ap.add_argument("--anim", help="animation.json to show (default: the troop's exported one)")
    ap.add_argument("--prev-anim", help="animation.json holding the previous version of the clip, for comparison")
    ap.add_argument("--texture")
    ap.add_argument("--geo", help="geo.json to draw (default: the troop's exported one); for a corrected model copy")
    ap.add_argument("--tier", type=int, help="draw only this armour and weapon tier (needs the troop's troop_rig json); "
                                             "without it every tier's cubes are drawn at once")
    ap.add_argument("--round", help="id the grades are saved under, e.g. knight-attack_overhead-r1")
    ap.add_argument("--out")
    ap.add_argument("--phases", default="")
    ap.add_argument("--note", default="")
    ap.add_argument("--title")
    ap.add_argument("--scale", type=int, default=230)
    ap.add_argument("--width", type=int, default=520)
    ap.add_argument("--quality", type=int, default=80)
    a = ap.parse_args(argv)
    if a.read:
        return read(a.read)
    for need in ("troop", "clip", "round", "out"):
        if not getattr(a, need):
            ap.error("--%s is required" % need)
    if not re.fullmatch(r"[A-Za-z0-9_.-]+", a.round):
        ap.error("--round: letters, digits, _ . - only")
    anim = a.anim or os.path.join(REPO, "%s/animations/troop/%s.animation.json" % (sc.ASSETS, a.troop))
    data = sc._read(anim)["animations"][a.clip]
    ticks = int(round(float(data["animation_length"]) * 20))
    impact = None
    for t, v in (data.get("timeline") or {}).items():
        if "impact" in json.dumps(v):
            impact = int(round(float(t) * 20))
    texture = a.texture or sorted(glob.glob(os.path.join(REPO, "%s/textures/entity/troop/%s_geo/*.png"
                                                         % (sc.ASSETS, a.troop))))[0]
    os.makedirs(a.out, exist_ok=True)
    geo = os.path.abspath(a.geo) if a.geo else None
    sets = {"new": render(anim, a.troop, a.clip, a.base, texture, os.path.join(a.out, "new"), ticks, a.scale, geo, a.tier)}
    if a.prev_anim:
        sets["prev"] = render(os.path.abspath(a.prev_anim), a.troop, a.clip, a.base, texture,
                              os.path.join(a.out, "prev"), ticks, a.scale, geo, a.tier)
    payload = {
        "round": a.round, "troop": a.troop, "clip": a.clip, "ticks": ticks, "impact": impact,
        "phases": phases(a.phases, ticks), "note": a.note, "tags": list(TAGS), "views": list(VIEWS),
        "checks": checks(a.troop, a.clip, a.anim),
        "frames": {k: {v: [uri(f, a.quality, a.width) for f in fs] for v, fs in s.items()} for k, s in sets.items()},
    }
    title = a.title or "%s %s grading" % (a.troop.capitalize(), a.clip.replace("_", " "))
    html = open(os.path.join(HERE, "grade_page.html"), encoding="utf-8").read()
    html = html.replace("__TITLE__", title).replace("__DATA__", json.dumps(payload, separators=(",", ":")))
    out = os.path.join(a.out, "index.html")
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(html)
    print("%s  %.1f MB  %d ticks, impact %s, checks: %d run, %d FAIL" % (out, os.path.getsize(out) / 1e6, ticks, impact,
                                                                        payload["checks"]["ran"],
                                                                        payload["checks"]["fail"]))
    return 0


if __name__ == "__main__":
    sys.exit(main())
