"""Grading evidence sheets for one motion-grade run (Task 15, R65-R67).

    python tools/contact_sheet.py <run folder> [--repo <checkout>] [--template]

The run folder is what the motion-grade check wrote: test-tools/work/grade/<family>/<timestamp>/ with manifest.json and the
raw shots. This script, in order:

1. writes the grading template (no Pillow needed): grading.md in the run folder, and docs/reports/grading/<family>.md in the
   checkout when that file does not exist yet (or always with --template). One table per clip/state, one row per rubric line
   (R65), with the grade, the evidence file and the observable findings to fill in.
2. with Pillow, builds the labelled sheets a reviewer reads (R66): per clip a contact sheet (8 fixed frames, front row over
   side row) and a turnaround (8 angles at the key time); per state a strip sheet (every 4th of the 120 capture frames) and a
   GIF of the capture (half size, 20 fps); adds them to manifest.json with simple pixel checks (identical consecutive capture
   frames, identical front frames).

Exit 0 when done, 3 when Pillow is missing (the template is written; the game then draws the sheets with ImageIO), 1 on error.
"""
import argparse
import json
import pathlib
import sys

RUBRIC = [
    ("weight", "Weight and timing", ("strip", "contact")),
    ("arcs", "Arcs", ("contact",)),
    ("anticipation", "Anticipation and follow-through", ("contact", "strip")),
    ("silhouette", "Silhouette at distance", ("crowd",)),
    ("variation", "Variation (no clones)", ("crowd_zoom", "crowd")),
    ("contact", "Contact (no foot slide, no ground sinking)", ("strip", "contact")),
    ("clipping", "No clipping (body, weapon, shield, neighbour)", ("turnaround", "close")),
    ("blend", "Blend continuity (no pops)", ("strip", "gif")),
]
STRIP_EVERY = 4


def font(size):
    from PIL import ImageFont
    for f in ("C:/Windows/Fonts/arialbd.ttf", "C:/Windows/Fonts/arial.ttf", "DejaVuSans-Bold.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(f, size)
        except OSError:
            pass
    return ImageFont.load_default()


def pairs(m):
    """The clip/state pairs the template grades: every state with its clip, then every clip no state shows."""
    out, used = [], set()
    for s in m["states"]:
        out.append((s["clip"], s))
        used.add(s["clip"])
    for c in m["clips"]:
        if c["name"] not in used:
            out.append((c["name"], None))
    return out


def evidence(m, clip, state, gif):
    """kind -> the run-relative file a reviewer opens for this clip/state. The sheet paths are fixed (Pillow and the game's
    ImageIO fallback write the same ones); the GIF exists only with Pillow."""
    ev = {"contact": "clips/%s/contact.png" % clip, "turnaround": "clips/%s/turnaround.png" % clip}
    if state:
        s = state["name"]
        ev.update({"crowd": "states/%s/crowd.png" % s, "crowd_zoom": "states/%s/crowd_zoom.png" % s,
                   "close": "states/%s/close.png" % s, "strip": "states/%s/strip.png" % s})
        if gif:
            ev["gif"] = "states/%s/capture.gif" % s
    return ev


def template(m, run_rel, gif):
    cam = m["camera"]
    tl = m["timeline"]
    lines = [
        "# Motion grading: %s" % m["family"],
        "",
        "Family `%s` (engine family `%s`, troop `%s`, rig `%s`)%s." % (
            m["family"], m["engineFamily"], m["troop"], m["rig"],
            ", synthetic stub clips" if m.get("stub") else ""),
        "Evidence run: `%s` (manifest.json lists every file by clip, state and kind)." % run_rel,
        "",
        "Grade each rubric line 1 to 10. A clip/state passes only when every line is 10 (R65). Write what you can see in",
        "the named file: the frame, the angle or the unit, and the fault. A grade with no observable finding does not count.",
        "",
        "Camera (fixed, family-agnostic; distances scale with the troop's height, scale %.2f):" % cam["scale"],
        "",
        "- Solo: %s." % cam["solo"],
        "- Close (trio): %s." % cam["close"],
        "- Crowd (20 units, distinct seeds): %s." % cam["crowd"],
        "- Capture: %d frames at %d fps; events right after frame %d (attacks and hits again after %d); strip = every %dth frame." % (
            tl["captureFrames"], tl["fps"], tl["event1AfterFrame"], tl["event2AfterFrame"], tl["stripEvery"]),
        "",
        "Rubric (R65): " + "; ".join(r[1] for r in RUBRIC) + ".",
        "",
    ]
    for clip, state in pairs(m):
        info = next((c for c in m["clips"] if c["name"] == clip), {})
        title = "## %s / %s" % (clip, state["name"] if state else "(forced only)")
        lines.append(title)
        lines.append("")
        meta = "Clip `%s` (%s, %.2f s, %s, key %.2f s by %s)." % (
            clip, info.get("category", "?"), info.get("length", 0), "loop" if info.get("loop") else "one-shot",
            info.get("keyTime", 0), info.get("keySource", "?"))
        if state:
            meta += " State `%s`: server stance `%s` from `%s`, crowd still at frame %d." % (
                state["name"], state.get("stance") or "none", state.get("preStance") or "none", state.get("crowdFrame", 0))
        else:
            meta += " No live state shows this clip; grade it from the forced shots, and silhouette, variation and blend as n/a."
        lines.append(meta)
        lines.append("")
        lines.append("| Rubric line | Grade | Evidence file | Observable findings |")
        lines.append("|---|---|---|---|")
        ev = evidence(m, clip, state, gif)
        for key, label, kinds in RUBRIC:
            files = [ev[k] for k in kinds if k in ev]
            cell = ", ".join("`%s`" % f for f in files) if files else "n/a (no live state)"
            lines.append("| %s | __/10 | %s | _to fill: frame or angle, what is seen_ |" % (label, cell))
        lines.append("")
    return "\n".join(lines)


def label_bar(img_w, h, text, size, bg=(24, 24, 24)):
    from PIL import Image, ImageDraw
    bar = Image.new("RGB", (img_w, h), bg)
    ImageDraw.Draw(bar).text((6, (h - size) // 2 - 1), text, fill=(255, 255, 255), font=font(size))
    return bar


def grid(run, tiles, cols, tw, th, title, sub=None):
    """tiles: [(rel path, label)]; a dark sheet with a title bar and each tile under its own label."""
    from PIL import Image, ImageDraw
    lab, head = 26, 40 if sub is None else 62
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * tw, head + rows * (th + lab)), (24, 24, 24))
    d = ImageDraw.Draw(sheet)
    d.text((8, 8), title, fill=(255, 255, 255), font=font(22))
    if sub:
        d.text((8, 36), sub, fill=(200, 200, 200), font=font(17))
    f = font(17)
    missing = []
    for i, (rel, text) in enumerate(tiles):
        x, y = i % cols * tw, head + i // cols * (th + lab)
        p = run / rel
        if p.is_file():
            im = Image.open(p).convert("RGB")
            if im.size != (tw, th):
                im = im.resize((tw, th), Image.LANCZOS)
            sheet.paste(im, (x, y + lab))
        else:
            missing.append(rel)
            d.rectangle((x, y + lab, x + tw - 1, y + lab + th - 1), outline=(255, 0, 0), width=3)
        d.text((x + 6, y + 4), text, fill=(255, 255, 255), font=f)
        d.line((x, y, x, y + lab + th), fill=(60, 60, 60))
    return sheet, missing


def same(a, b):
    from PIL import Image, ImageChops
    with Image.open(a) as x, Image.open(b) as y:
        return ImageChops.difference(x.convert("RGB"), y.convert("RGB")).getbbox() is None


def sheets(run, m):
    from PIL import Image
    files = m["files"]
    new, checks, missing = [], {"identicalCaptureSteps": {}, "identicalFrontFrames": {}}, []
    clips = {c["name"]: c for c in m["clips"]}
    for name, c in clips.items():
        tiles = []
        for view in ("front", "side"):
            for i, t in enumerate(c["frameTimes"]):
                tiles.append(("clips/%s/%s_f%d.png" % (name, view, i), "%s f%d  t=%.2fs" % (view, i, t)))
        sheet, miss = grid(run, tiles, 8, 360, 360,
                           "clip %s  (state %s)  %.2fs %s" % (name, c["state"], c["length"], "loop" if c["loop"] else "one-shot"),
                           "8 fixed frames: front row (facing the troop), side row (its right); key %.2fs (%s)" % (c["keyTime"], c["keySource"]))
        missing += miss
        rel = "clips/%s/contact.png" % name
        sheet.save(run / rel)
        new.append({"kind": "contact", "clip": name, "state": c["state"], "file": rel, "width": sheet.width, "height": sheet.height})
        fronts = [run / t[0] for t in tiles[:8]]
        dup = [i for i in range(1, 8) if fronts[i].is_file() and fronts[i - 1].is_file() and same(fronts[i - 1], fronts[i])]
        if dup:
            checks["identicalFrontFrames"][name] = dup
        tiles = [("clips/%s/turn_%03d.png" % (name, a), "%03d deg  t=%.2fs" % (a, c["keyTime"])) for a in m["camera"]["turnAngles"]]
        sheet, miss = grid(run, tiles, 4, 400, 400, "clip %s  turnaround at key time %.2fs" % (name, c["keyTime"]),
                           "0 = facing the troop, 90 = its right, 180 = behind, 270 = its left")
        missing += miss
        rel = "clips/%s/turnaround.png" % name
        sheet.save(run / rel)
        new.append({"kind": "turnaround", "clip": name, "state": c["state"], "file": rel, "width": sheet.width, "height": sheet.height})
    n = m["timeline"]["captureFrames"]
    e1, e2 = m["timeline"]["event1AfterFrame"], m["timeline"]["event2AfterFrame"]
    for s in m["states"]:
        name = s["name"]
        caps = ["states/%s/capture/f%03d.png" % (name, i) for i in range(n)]
        events = [e for e in s["eventsAfterFrames"]]
        tiles = []
        for i in range(0, n, STRIP_EVERY):
            mark = ""
            for e in events:
                if e < i <= e + STRIP_EVERY:
                    mark = "  +%d after event" % (i - e)
            tiles.append((caps[i], "f%03d  t=%.2fs%s" % (i, i / 20.0, mark)))
        sheet, miss = grid(run, tiles, 6, 420, 270, "state %s  (clip %s)  close trio, every %dth frame" % (name, s["clip"], STRIP_EVERY),
                           "events right after %s; server stance %s from %s" % (" and ".join("f%03d" % e for e in events), s["stance"] or "none", s["preStance"] or "none"))
        missing += miss
        rel = "states/%s/strip.png" % name
        sheet.save(run / rel)
        new.append({"kind": "strip", "clip": s["clip"], "state": name, "file": rel, "width": sheet.width, "height": sheet.height})
        frames, dup = [], []
        for i, rel_c in enumerate(caps):
            p = run / rel_c
            if not p.is_file():
                continue
            if i > 0 and (run / caps[i - 1]).is_file() and same(run / caps[i - 1], p):
                dup.append(i)
            with Image.open(p) as im:
                frames.append(im.convert("RGB").resize((im.width // 2, im.height // 2), Image.LANCZOS))
        if dup:
            checks["identicalCaptureSteps"][name] = dup
        if frames:
            rel = "states/%s/capture.gif" % name
            frames[0].save(run / rel, save_all=True, append_images=frames[1:], duration=50, loop=0, optimize=False)
            new.append({"kind": "gif", "clip": s["clip"], "state": name, "file": rel, "width": frames[0].width, "height": frames[0].height})
    return new, checks, missing


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("run")
    ap.add_argument("--repo", default=str(pathlib.Path(__file__).resolve().parent.parent))
    ap.add_argument("--template", action="store_true", help="overwrite docs/reports/grading/<family>.md")
    a = ap.parse_args()
    run, repo = pathlib.Path(a.run).resolve(), pathlib.Path(a.repo).resolve()
    mf = run / "manifest.json"
    m = json.loads(mf.read_text(encoding="utf-8"))
    files = [f for f in m["files"] if f["kind"] not in ("contact", "turnaround", "strip", "gif", "template")]
    m["files"] = files
    try:
        run_rel = run.relative_to(repo).as_posix()
    except ValueError:
        run_rel = run.as_posix()
    try:
        import PIL  # noqa: F401
        pillow = True
    except ImportError:
        pillow = False
    text = template(m, run_rel, pillow)
    (run / "grading.md").write_text(text + "\n", encoding="utf-8", newline="\n")
    files.append({"kind": "template", "file": "grading.md"})
    doc = repo / "docs" / "reports" / "grading" / ("%s.md" % m["family"])
    if a.template or not doc.exists():
        doc.parent.mkdir(parents=True, exist_ok=True)
        doc.write_text(text + "\n", encoding="utf-8", newline="\n")
        m["repoTemplate"] = doc.relative_to(repo).as_posix()
        print("template written:", doc)
    else:
        print("template kept (exists):", doc)
    if not pillow:
        m["pillow"] = False
        mf.write_text(json.dumps(m, indent=2), encoding="utf-8")
        print("no Pillow: sheets left to the game (ImageIO)")
        return 3
    new, checks, missing = sheets(run, m)
    files += new
    m["pillow"] = True
    m["imageChecks"] = checks
    m["sheetMissingTiles"] = missing
    mf.write_text(json.dumps(m, indent=2), encoding="utf-8")
    print("sheets: %d (%s); missing tiles %d; identical capture steps %s; identical front frames %s" % (
        len(new), ", ".join(sorted({x["kind"] for x in new})), len(missing),
        {k: len(v) for k, v in checks["identicalCaptureSteps"].items()} or "none", checks["identicalFrontFrames"] or "none"))
    return 1 if missing else 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as x:  # the game reads the exit code; the log has the trace
        import traceback
        traceback.print_exc()
        sys.exit(1)
