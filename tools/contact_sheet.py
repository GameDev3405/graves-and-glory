"""Grading evidence sheets for one motion-grade run (Task 15, R65-R79).

    python tools/contact_sheet.py <run folder> [--repo <checkout>] [--template]

The run folder is what the motion-grade check wrote: test-tools/work/grade/<family>/<timestamp>/ with manifest.json and the
raw shots. The game decides what every sheet shows: manifest.json "sheets" holds one spec per sheet (a grid of tiles, each a
raw shot or a crop of one with its marks, or a plot of series). This script, in order:

1. writes the grading template (no Pillow needed): grading.md in the run folder, and docs/reports/grading/<family>.md in the
   checkout. M3: a new file, or one whose grade cells are all still "__/10", is written whole (so is any with --template);
   once a grade is filled in, a re-run replaces only its "Evidence run:" line.
2. with Pillow, draws every sheet spec (the game's ImageIO fallback draws the same specs without Pillow) and a GIF of each
   state's close capture, adds them to manifest.json, and checks the captures for identical consecutive frames.

Exit 0 when done, 3 when Pillow is missing or GRAVES_GRADE_NO_PILLOW=1 (the template is written; the game then draws the
sheets with ImageIO), 1 on error.
"""
import math
import argparse
import json
import os
import pathlib
import re
import sys

RUBRIC = [
    ("weight", "Weight and timing"),
    ("arcs", "Arcs"),
    ("anticipation", "Anticipation and follow-through"),
    ("silhouette", "Silhouette at distance"),
    ("variation", "Variation (no clones)"),
    ("contact", "Contact (no foot slide, no ground sinking)"),
    ("clipping", "No clipping (body, weapon, shield, neighbour)"),
    ("blend", "Blend continuity (no pops)"),
]
SHEET_KINDS = {"contact", "turnaround", "trail", "strip", "dense", "delta", "loco", "loco_trace", "gif", "template"}
BG, FG, DIM, HL = (24, 24, 24), (255, 255, 255), (170, 170, 170), (255, 220, 0)
_fonts = {}


def font(size, bold=False):
    key = (size, bold)
    if key in _fonts:
        return _fonts[key]
    from PIL import ImageFont
    names = ("C:/Windows/Fonts/arialbd.ttf", "DejaVuSans-Bold.ttf") if bold else ("C:/Windows/Fonts/arial.ttf", "DejaVuSans.ttf")
    f = None
    for n in names:
        try:
            f = ImageFont.truetype(n, size)
            break
        except OSError:
            pass
    _fonts[key] = f or ImageFont.load_default()
    return _fonts[key]


def rgb(hexs):
    try:
        h = hexs.lstrip("#")
        return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
    except (ValueError, AttributeError):
        return FG


# ---- the template ----
def fmt_list(v):
    return ", ".join("%.3f" % x for x in v) if isinstance(v, list) else str(v)


def template(m, run_rel):
    cam, tl = m["camera"], m["timeline"]
    L = [
        "# Motion grading: %s" % m["family"],
        "",
        "Family `%s` (engine family `%s`, troop `%s`, rig `%s`)%s." % (
            m["family"], m["engineFamily"], m["troop"], m["rig"], ", synthetic stub clips" if m.get("stub") else ""),
        "Evidence run: `%s` (manifest.json lists every file by clip, state and kind)." % run_rel,
        "",
        "Grade each rubric line 1 to 10. A state passes only when every line is 10 (R65). Write what you can see in the named",
        "file: the frame, the angle or the unit, and the fault. A grade with no observable finding does not count.",
        "",
        "Re-runs (M3): while every grade cell here is still `__/10`, a re-run rewrites this whole file; once any grade is filled",
        "in, a re-run replaces only the `Evidence run:` line above and leaves the grades and findings alone.",
        "",
        "## How the evidence was made",
        "",
        "- Pipeline (R75): %s" % m["pipeline"],
        "- Solo (R74, R68, R76, R77): %s. Each clip: 8 frames front and side with feet and hands crops, its key poses from 8 angles" % cam["solo"],
        "  (0 front, 45, 90 its right, 135, 180 back, 225, 270 its left, 315), and its weapon-tip and shield-rim trails front and side.",
        "- Marks on solo and loco shots: white ground line with height ticks every 0.5 block, soles green when planted, red when lifted",
        "  and magenta when sunk more than half a texel (1/32 block) below the ground, each over a grey ground tick; hands yellow, weapon tip orange, shield rim cyan. The marks are computed from the drawn pose, not painted by hand.",
        "- Locomotion (R71): %s. Segments: walk, run, shield crawl, backpedal and a 90 degree turn in place, each from idle and back to" % cam["loco"],
        "  idle. Planted slide and sole heights are in manifest.json `loco`, and summarised below.",
        "- Spacing (R73): %s; the wide grid is a clone-check overview only, never evidence of spacing or clipping." % cam["spacing"],
        "- Crowd distance (R78): %s; %s. Cite the level shot for silhouette and the high formation shot for variation." % (cam["high"], cam["level"]),
        "- Capture: %d frames at %d fps; events right after frame %d (attacks and hits again after %d); strip = every %dth frame;" % (
            tl["captureFrames"], tl["fps"], tl["event1AfterFrame"], tl["event2AfterFrame"], tl["stripEvery"]),
        "  dense sheets show every frame from E-10 to E+10 with the per-tick pose delta under each (R72).",
        "- Volleys (M1): every troop gets its attack or hit on the same tick, so crowd and formation shots show synchronised",
        "  volleys. Judge variation by stance phase, amplitude, faces and seeds, not by event timing.",
        "- Deaths (R79): crowd stills at f028 (vanilla death tint, as in game), f050 and f080 (death ticks about 40 and 70).",
        "- Blend (M2): cite the dense sheets and the delta plots, not the GIF.",
        "",
    ]
    if m.get("crowdNearestDist") is not None:
        L.append("Measured: the nearest crowd troop was %.2f blocks from the camera." % m["crowdNearestDist"])
        L.append("")
    if m.get("loco"):
        L += ["| Loco segment | Planted slide R, L (blocks) | Largest single plant R, L | Lowest sole R, L | Body moved |", "|---|---|---|---|---|"]
        for s in m["loco"]:
            if "plantedSlideBlocks" in s:
                L.append("| %s | %s | %s | %s | %.2f |" % (s["name"], fmt_list(s["plantedSlideBlocks"]), fmt_list(s["largestSinglePlantSlideBlocks"]),
                                                       fmt_list(s["lowestSoleHeight"]), s.get("bodyMovedBlocks", 0)))
        L.append("")
    if m.get("largestJumpPerTransition"):
        L += ["| Transition | Event | Largest step (deg/tick) at frame | Largest 2nd difference | x baseline p90 | Flagged |", "|---|---|---|---|---|---|"]
        for k, v in m["largestJumpPerTransition"].items():
            L.append("| %s | %s | %.1f at f%03d (%s) | %.1f | %.1f | %s |" % (k, v["event"], v["largestStepDegPerTick"], v["largestStepFrame"],
                                                                     v.get("largestBoneThere", ""), v["largestSecondDiff"], v["ratioToBaseline"], "yes" if v["flagged"] else "no"))
        L.append("")
    L += ["Rubric (R65): " + "; ".join(r[1] for r in RUBRIC) + ".", ""]
    grading = m.get("grading", {})
    clips = {c["name"]: c for c in m["clips"]}
    shown = set()
    for s in m["states"]:
        info = clips.get(s["clip"], {})
        shown.add(s["clip"])
        L += ["## %s / %s" % (s["name"], s["clip"]), ""]
        L.append("Clip `%s` (%s, %.2f s, %s, key poses %s). State: server stance `%s` from `%s`, stills at %s, spacing %.2f." % (
            s["clip"], info.get("category", "?"), info.get("length", 0), "loop" if info.get("loop") else "one-shot",
            ", ".join("%s %.2fs" % kv for kv in info.get("keyPoses", {}).items()), s.get("stance") or "none", s.get("preStance") or "none",
            ", ".join("f%03d" % f for f in s.get("stillFrames", [])), s.get("cell", 0)))
        L += ["", "| Rubric line | Grade | Evidence file | Observable findings |", "|---|---|---|---|"]
        ev = grading.get(s["name"], {})
        for key, label in RUBRIC:
            files = ev.get(key, [])
            cell = "<br>".join("`%s` (%s)" % (f["file"], f["note"]) for f in files) if files else "n/a"
            L.append("| %s | __/10 | %s | _to fill: frame or angle, what is seen_ |" % (label, cell))
        L.append("")
    for name, c in clips.items():
        if name in shown:
            continue
        d = "clips/%s/" % name
        L += ["## %s (no live state)" % name, "", "No live state shows this clip; grade it from its solo sheets, and silhouette, variation and blend as n/a.", "",
              "| Rubric line | Grade | Evidence file | Observable findings |", "|---|---|---|---|"]
        for key, label in RUBRIC:
            cell = {"weight": "`%scontact.png`" % d, "arcs": "`%strail.png`, `%sturnaround.png`" % (d, d), "anticipation": "`%scontact.png`" % d,
                    "contact": "`%scontact.png`" % d, "clipping": "`%sturnaround.png`" % d}.get(key, "n/a")
            L.append("| %s | __/10 | %s | _to fill: frame or angle, what is seen_ |" % (label, cell))
        L.append("")
    return "\n".join(L)


def write_doc(doc, text, force):
    """M3: whole file when new, forced, or every grade cell still blank; otherwise only the Evidence run: line."""
    if doc.exists() and not force:
        old = doc.read_text(encoding="utf-8")
        grades = re.findall(r"^\|[^|\n]*\|\s*([^|\n]*?)\s*\|", old, flags=re.M)
        filled = [g for g in grades if re.fullmatch(r"\d+\s*/\s*10", g)]
        if filled:
            new_line = next((ln for ln in text.splitlines() if ln.startswith("Evidence run:")), None)
            if new_line is None:
                return "kept"
            out, n = re.subn(r"^Evidence run:.*$", lambda _: new_line, old, count=1, flags=re.M)
            doc.write_text(out, encoding="utf-8", newline="\n")
            return "evidence line updated (%d grades kept)" % len(filled)
    doc.parent.mkdir(parents=True, exist_ok=True)
    doc.write_text(text + "\n", encoding="utf-8", newline="\n")
    return "written whole"


# ---- the sheets ----
class Images:
    def __init__(self, run):
        self.run, self.cache = run, {}

    def get(self, rel):
        if rel not in self.cache:
            from PIL import Image
            if len(self.cache) > 48:
                self.cache.clear()
            p = self.run / rel
            self.cache[rel] = Image.open(p).convert("RGB") if p.is_file() else None
        return self.cache[rel]


def crop_of(t, im):
    if "crop" not in t:
        return 0, 0, im.width, im.height
    x, y, w, h = t["crop"]
    x = max(0, min(im.width - 1, x))
    y = max(0, min(im.height - 1, y))
    return x, y, max(1, min(im.width - x, w)), max(1, min(im.height - y, h))


def dims(t, imgs):
    im = imgs.get(t["src"])
    if im is None:
        return 200, 100
    _, _, w, h = crop_of(t, im)
    return w * t["scale"], h * t["scale"]


def draw_marks(d, marks, cx, cy, sc, ox, oy, box):
    def P(p):
        return (p[0] - cx) * sc + ox, (p[1] - cy) * sc + oy

    def inside(q):
        return box[0] - 2 <= q[0] <= box[2] + 2 and box[1] - 2 <= q[1] <= box[3] + 2

    for mk in marks:
        c = rgb(mk.get("c", "#ffffff"))
        t = mk["t"]
        if t in ("line", "poly"):
            pts = [P(p) for p in mk["pts"]]
            if t == "line":
                pts = [(min(max(x, box[0]), box[2]), min(max(y, box[1]), box[3])) for x, y in pts]
            else:
                pts = [q for q in pts if inside(q)]
            if len(pts) >= 2:
                d.line(pts, fill=c, width=mk.get("w", 1))
            if mk.get("dots"):
                for x, y in pts:
                    d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=c)
        elif t == "dot":
            x, y = P(mk["p"])
            if inside((x, y)):
                r = mk.get("r", 3)
                d.ellipse((x - r, y - r, x + r, y + r), fill=c, outline=(0, 0, 0))
        elif t == "text":
            x, y = P(mk["p"])
            if inside((x, y)):
                d.text((x, y), mk["s"], fill=c, font=font(11))


def grid(sp, imgs):
    from PIL import Image, ImageDraw
    pad, row_lab, tile_lab = 8, 20, 16
    head = 30 + 18 * len(sp.get("sub", []))
    sizes, W, H = [], 900, head
    for row in sp["rows"]:
        w, h = pad, 0
        for t in row["tiles"]:
            tw, th = dims(t, imgs)
            w += tw + pad
            h = max(h, th)
        sizes.append(h)
        W = max(W, w)
        H += (row_lab if row["label"] else 0) + tile_lab + h + pad
    sheet = Image.new("RGB", (W, H + pad), BG)
    d = ImageDraw.Draw(sheet)
    d.text((pad, 6), sp["title"], fill=FG, font=font(18, True))
    y = 30
    for s in sp.get("sub", []):
        d.text((pad, y), s, fill=DIM, font=font(13))
        y += 18
    y = head
    missing = []
    for row, h in zip(sp["rows"], sizes):
        if row["label"]:
            d.text((pad, y + 2), row["label"], fill=FG, font=font(14, True))
            y += row_lab
        x = pad
        for t in row["tiles"]:
            tw, th = dims(t, imgs)
            d.text((x + 2, y + 1), t["label"], fill=HL if t.get("hl") else FG, font=font(12))
            ty = y + tile_lab
            im = imgs.get(t["src"])
            if im is not None:
                cx, cy, cw, ch = crop_of(t, im)
                sub = im.crop((cx, cy, cx + cw, cy + ch))
                if t["scale"] != 1:
                    sub = sub.resize((cw * t["scale"], ch * t["scale"]), Image.NEAREST if t.get("nearest") else Image.LANCZOS)
                sheet.paste(sub, (x, ty))
                draw_marks(d, t.get("marks", []), cx, cy, t["scale"], x, ty, (x, ty, x + tw - 1, ty + th - 1))
            else:
                if not t.get("missing"):
                    missing.append(t["src"])
                d.rectangle((x, ty, x + tw - 1, ty + th - 1), outline=(255, 0, 0), width=3)
                d.text((x + 4, ty + 20), "missing", fill=(255, 80, 80), font=font(12))
            if t.get("hl"):
                d.rectangle((x - 3, ty - 3, x + tw + 2, ty + th + 2), outline=HL, width=3)
            x += tw + pad
        y += tile_lab + h + pad
    return sheet, missing


def nice_step(raw):
    """A 1, 2 or 5 times a power of ten step at least raw, so axis labels are exact."""
    if raw <= 0:
        return 1.0
    e = 10 ** math.floor(math.log10(raw))
    for f in (1, 2, 5, 10):
        if f * e >= raw - 1e-15:
            return f * e
    return 10 * e


def plot(p):
    from PIL import Image, ImageDraw
    W, H, left, right, top, gap = p["w"], p["h"], 80, 20, 40, 46
    panels = p["panels"]
    n = max(1, len(panels))
    ph = (H - top - 30 - gap * (n - 1)) // n
    im = Image.new("RGB", (W, H), BG)
    d = ImageDraw.Draw(im)
    d.text((left, 8), p["title"], fill=FG, font=font(16, True))
    xs_all = [v for pa in panels for s in pa["series"] for v in s["x"]]
    x0, x1 = (min(xs_all), max(xs_all)) if xs_all else (0, 1)
    if x1 <= x0:
        x1 = x0 + 1
    pw = W - left - right

    def X(v):
        return left + (v - x0) / (x1 - x0) * pw

    for i, pa in enumerate(panels):
        py = top + i * (ph + gap)
        ys = [v for s in pa["series"] for v in s["y"]] + [h["y"] for h in pa["hlines"]]
        y0, y1 = min([0] + ys), max([0] + ys)
        if y1 - y0 < 1e-9:
            y1 = y0 + 1
        padv = (y1 - y0) * 0.06
        y0, y1 = y0 - padv, y1 + padv

        def Y(v):
            return py + ph - (v - y0) / (y1 - y0) * ph

        d.rectangle((left, py, left + pw, py + ph), fill=(40, 40, 40), outline=DIM)
        d.text((left + 4, py - 18), "%s  [%s]" % (pa["title"], pa["ylabel"]), fill=DIM, font=font(12))
        step = nice_step((y1 - y0) / 5)
        dec = max(0, -math.floor(math.log10(step) + 1e-9))
        v = math.ceil(y0 / step) * step
        while v <= y1 + 1e-12:
            yy = Y(v)
            d.line((left, yy, left + pw, yy), fill=(70, 70, 70))
            d.text((6, yy - 7), "%.*f" % (dec, 0.0 if abs(v) < step * 1e-6 else v), fill=DIM, font=font(12))
            v += step
        for h in pa["hlines"]:
            yy = Y(h["y"])
            for xx in range(left, left + pw, 10):
                d.line((xx, yy, min(xx + 6, left + pw), yy), fill=rgb(h["c"]))
            d.text((left + pw - 280, yy - 15), h["label"], fill=rgb(h["c"]), font=font(12))
        ly = py + 4
        for s in pa["series"]:
            pts = [(X(a), Y(b)) for a, b in zip(s["x"], s["y"])]
            if len(pts) >= 2:
                d.line(pts, fill=rgb(s["c"]), width=2)
            d.text((left + 8, ly), s["name"], fill=rgb(s["c"]), font=font(12))
            ly += 14
        for v in p["vlines"]:
            xx = X(v["x"])
            d.line((xx, py, xx, py + ph), fill=rgb(v["c"]), width=1)
            if i == 0:
                d.text((xx + 3, py + ph - 18), v["label"], fill=rgb(v["c"]), font=font(12))
        if i == len(panels) - 1:
            xv = (int(x0) // 10 + (1 if x0 % 10 else 0)) * 10
            while xv <= x1:
                d.text((X(xv) - 6, py + ph + 4), str(int(xv)), fill=DIM, font=font(12))
                xv += 10
            d.text((left + pw - 40, py + ph + 16), "frame", fill=DIM, font=font(12))
    return im


def same(a, b):
    from PIL import ImageChops
    return a.size == b.size and ImageChops.difference(a, b).getbbox() is None


def sheets(run, m):
    from PIL import Image
    imgs = Images(run)
    new, missing = [], []
    for sp in m["sheets"]:
        if "plot" in sp:
            im = plot(sp["plot"])
        else:
            im, miss = grid(sp, imgs)
            missing += miss
        p = run / sp["file"]
        p.parent.mkdir(parents=True, exist_ok=True)
        im.save(p)
        e = {"kind": sp["kind"], "file": sp["file"], "width": im.width, "height": im.height, "renderer": "Pillow"}
        for k in ("clip", "state"):
            if k in sp:
                e[k] = sp[k]
        new.append(e)
    checks = {"identicalCaptureSteps": {}}
    n = m["timeline"]["captureFrames"]
    for s in m["states"]:
        name = s["name"]
        frames, dup, prev = [], [], None
        for i in range(n):
            p = run / ("states/%s/capture/f%03d.png" % (name, i))
            if not p.is_file():
                continue
            with Image.open(p) as im:
                cur = im.convert("RGB")
            if prev is not None and same(prev, cur):
                dup.append(i)
            prev = cur
            frames.append(cur.convert("P", palette=Image.ADAPTIVE, colors=255))
        if dup:
            checks["identicalCaptureSteps"][name] = dup
        if frames:
            rel = "states/%s/capture.gif" % name
            frames[0].save(run / rel, save_all=True, append_images=frames[1:], duration=50, loop=0, optimize=False)
            new.append({"kind": "gif", "clip": s["clip"], "state": name, "file": rel, "width": frames[0].width, "height": frames[0].height,
                        "note": "native size; for blend continuity cite the dense sheets and delta plots, not this GIF"})
    return new, checks, missing


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("run")
    ap.add_argument("--repo", default=str(pathlib.Path(__file__).resolve().parent.parent))
    ap.add_argument("--template", action="store_true", help="rewrite docs/reports/grading/<family>.md whole, grades included")
    a = ap.parse_args()
    run, repo = pathlib.Path(a.run).resolve(), pathlib.Path(a.repo).resolve()
    mf = run / "manifest.json"
    m = json.loads(mf.read_text(encoding="utf-8"))
    files = [f for f in m["files"] if f["kind"] not in SHEET_KINDS]
    m["files"] = files
    try:
        run_rel = run.relative_to(repo).as_posix()
    except ValueError:
        run_rel = run.as_posix()
    no_pillow_env = os.environ.get("GRAVES_GRADE_NO_PILLOW", "").strip().lower() in ("1", "true", "yes")
    pillow = not no_pillow_env
    if pillow:
        try:
            import PIL  # noqa: F401
        except ImportError:
            pillow = False
    text = template(m, run_rel)
    (run / "grading.md").write_text(text + "\n", encoding="utf-8", newline="\n")
    files.append({"kind": "template", "file": "grading.md"})
    doc = repo / "docs" / "reports" / "grading" / ("%s.md" % m["family"])
    how = write_doc(doc, text, a.template)
    m["repoTemplate"] = {"file": doc.relative_to(repo).as_posix(), "written": how}
    print("template %s: %s" % (how, doc))
    if not pillow:
        m["pillow"] = False
        mf.write_text(json.dumps(m, indent=2), encoding="utf-8")
        print("no Pillow%s: sheets left to the game (ImageIO)" % (" (GRAVES_GRADE_NO_PILLOW)" if no_pillow_env else ""))
        return 3
    new, checks, missing = sheets(run, m)
    files += new
    m["pillow"] = True
    m["imageChecks"] = checks
    m["sheetMissingTiles"] = missing
    mf.write_text(json.dumps(m, indent=2), encoding="utf-8")
    print("sheets: %d (%s); missing tiles %d; identical capture steps %s" % (
        len(new), ", ".join(sorted({x["kind"] for x in new})), len(missing),
        {k: len(v) for k, v in checks["identicalCaptureSteps"].items()} or "none"))
    return 1 if missing else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception:  # the game reads the exit code; the log has the trace
        import traceback
        traceback.print_exc()
        sys.exit(1)
