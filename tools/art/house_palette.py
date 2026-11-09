#!/usr/bin/env python3
"""Locked colour palettes per House, extracted from the approved troop textures, and a check against them.

  python tools/art/house_palette.py extract [--ref 54e5d339]
      Read every shipped troop skin (s1-s3, veteran, e) at a git ref and write art/palettes/<house>.json for each House
      plus art/palettes/team.json (everything in team_cloth_*). A code ramp (a module-level
      list of RGBA tuples in handlib, the House modules or a builder) that matches two or more used colours is stored whole,
      dark to light, under its module-qualified name; shades no troop uses have "used_by": []. The rest of the used
      colours go in "loose". Every colour lists the troops that use it.
  python tools/art/house_palette.py check [ids...] [--ref R [--repo P]] [--dir D --house H] [--json]
      Fail (exit 1) when a texture uses a colour outside its House palette plus the team palette (cloth: team only).
      No ids: every troop in sheets/design.json. --ref reads the PNGs from git (--repo: another worktree) instead of disk.
      --dir D checks a builder's preview output (skin_s*.png, cloth_*.png) against --house.
  python tools/art/house_palette.py show <house|team|all> [out.png]
      Swatch PNG of a palette, ramps as labelled rows (default art/palettes/<house>.png).
  python tools/art/house_palette.py register <house> <module> [--with handlib.LEATH,handlib.SKINS.fair] [--force]
      A NEW House: write art/palettes/<house>.json (+ swatch) from every module-level ramp of art/hand/common/<module>.py
      (or a .py path), plus any named shared ramps, so palette.ramps(<house>) and check --house <house> work before the
      House has troops. Refuses an existing palette (the five are locked) unless --force. docs/art/TEXTURE-RECIPE.md part B.

Opaque means alpha > 0; the shipped troop sheets have no partial alpha.
"""
import argparse, collections, importlib.util, io, json, os, re, subprocess, sys
from PIL import Image, ImageDraw, ImageFont

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
TROOP_DIR = "src/main/resources/assets/gravesandglory/textures/entity/troop"
PAL_DIR = os.path.join(REPO, "art", "palettes")
COMMON = os.path.join(REPO, "art", "hand", "common")
HAND = os.path.join(REPO, "art", "hand")
DEFAULT_REF = "54e5d339"

# House module(s) whose ramps may group that House's colours (handlib is always a candidate)
HOUSE_MODULES = {"gilded": [], "graveborn": ["graveborn"], "wildmoot": ["wildmoot"],
                 "ironclad": ["ironclad_tiers", "ironclad"], "tidecallers": ["tidecallers"]}
TEAM_SIDES = ("gold", "violet", "crimson", "teal", "company", "linen")
SKIN_RE = r"^%s_(s\d+|veteran|e\w*)\.png$"
CLOTH_RE = r"^team_cloth_\w+\.png$"


# ---------------------------------------------------------------------------------------------- helpers
def hexs(c): return "#%02x%02x%02x" % tuple(c[:3])
def unhex(s): s = s.lstrip("#"); return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))
def troops():
    with open(os.path.join(REPO, "sheets", "design.json"), encoding="utf-8") as f:
        return {t["id"]: t["house"] for t in json.load(f)["troops"]}
def houses_of(tr): return sorted(set(tr.values()))
def house_modules(house):
    """The House's own ramp modules: the fixed table, else the "modules" a registered palette recorded."""
    if house in HOUSE_MODULES: return HOUSE_MODULES[house]
    p = os.path.join(PAL_DIR, house + ".json")
    if os.path.exists(p):
        with open(p, encoding="utf-8") as f: return json.load(f).get("modules", [])
    return []

def git(repo, *args, binary=False):
    p = subprocess.run(["git", "-C", repo] + list(args), capture_output=True)
    if p.returncode: raise RuntimeError("git %s: %s" % (" ".join(args), p.stderr.decode("utf-8", "replace").strip()))
    return p.stdout if binary else p.stdout.decode("utf-8", "replace")

class Source:
    """Where PNGs come from: the working tree (ref None) or a git ref of some repo."""
    def __init__(self, ref=None, repo=REPO): self.ref, self.repo = ref, repo
    def list(self, tid):
        d = "%s/%s" % (TROOP_DIR, tid)
        if self.ref:
            out = git(self.repo, "ls-tree", "--name-only", self.ref, d + "/")
            names = [l.split("/")[-1] for l in out.splitlines() if l.strip()]
        else:
            p = os.path.join(self.repo, *d.split("/"))
            names = os.listdir(p) if os.path.isdir(p) else []
        skins = sorted(n for n in names if re.match(SKIN_RE % re.escape(tid), n))
        cloth = sorted(n for n in names if re.match(CLOTH_RE, n))
        return skins, cloth
    def load(self, tid, name):
        d = "%s/%s/%s" % (TROOP_DIR, tid, name)
        if self.ref: data = git(self.repo, "show", "%s:%s" % (self.ref, d), binary=True)
        else:
            with open(os.path.join(self.repo, *d.split("/")), "rb") as f: data = f.read()
        return Image.open(io.BytesIO(data)).convert("RGBA")

def pixels(im):
    w = im.width
    raw = im.tobytes()
    for i in range(0, len(raw), 4):
        if raw[i + 3]:
            j = i // 4
            yield (raw[i], raw[i + 1], raw[i + 2]), j % w, j // w

def colour_usage(im):
    """{rgb: (texels, first x, first y)} of the texels with alpha > 0."""
    u = {}
    for c, x, y in pixels(im):
        if c in u: u[c][0] += 1
        else: u[c] = [1, x, y]
    return u


# ---------------------------------------------------------------------------------------------- code ramps
def is_ramp(v):
    return (isinstance(v, (list, tuple)) and len(v) >= 2 and
            all(isinstance(c, tuple) and len(c) in (3, 4) and all(isinstance(n, int) and 0 <= n <= 255 for n in c) for c in v))

def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod

def module_ramps(modname, mod):
    """Yield (qualified name, ramp) for every module-level ramp; dicts and tuples of ramps (SKINS, TEAM) are walked."""
    for k, v in list(vars(mod).items()):
        if k.startswith("_"): continue
        if is_ramp(v): yield "%s.%s" % (modname, k), v
        elif isinstance(v, dict):
            for k2, v2 in v.items():
                if is_ramp(v2): yield "%s.%s.%s" % (modname, k, k2), v2
                elif isinstance(v2, tuple) and v2 and all(is_ramp(x) for x in v2):
                    for i, x in enumerate(v2):
                        label = ("cloth", "metal")[i] if k == "TEAM" and i < 2 else str(i)
                        yield "%s.%s.%s.%s" % (modname, k, k2, label), x

def catalog(tr):
    """{module: {name: [rgb...]}} for handlib, the common modules, ironclad and every builder. Duplicates (an imported
    ramp, an alias, a slice of an earlier ramp) are dropped so each ramp lives under the module that defines it."""
    for p in (COMMON, os.path.join(HAND, "ic_breaker")):
        if p not in sys.path: sys.path.insert(0, p)
    order = [("handlib", os.path.join(COMMON, "handlib.py"))]
    for n in ("graveborn", "ironclad_tiers", "tidecallers", "wildmoot"): order.append((n, os.path.join(COMMON, n + ".py")))
    order.append(("ironclad", os.path.join(HAND, "ic_breaker", "ironclad.py")))
    for h in sorted(set(tr.values())):   # registered Houses' modules (register)
        for n in house_modules(h):
            if n not in [o[0] for o in order]: order.append((n, os.path.join(COMMON, n + ".py")))
    for tid in tr: order.append(("build_" + tid, os.path.join(HAND, tid, "build_%s.py" % tid)))
    seen_ids, seen_sets, out = set(), [], {}
    real_stdout = sys.stdout
    for modname, path in order:
        if not os.path.exists(path): continue
        sys.stdout = io.StringIO()
        try: mod = load_module(modname, path)
        except Exception as e:
            sys.stdout = real_stdout
            print("warning: could not import %s: %s: %s" % (modname, type(e).__name__, e), file=sys.stderr)
            continue
        finally: sys.stdout = real_stdout
        for name, r in module_ramps(modname, mod):
            if id(r) in seen_ids: continue
            seen_ids.add(id(r))
            rgb = [tuple(c[:3]) for c in r]
            s = set(rgb)
            if any(s <= t for t in seen_sets): continue
            seen_sets.append(s)
            out.setdefault(modname, {})[name] = rgb
    return out


# ---------------------------------------------------------------------------------------------- extract
def lum(c): return 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2]

def build_palette(label, used, candidates):
    """used: {rgb: sorted troop ids}; candidates: {ramp name: [rgb...]} -> palette dict. A ramp that matches at least two
    used colours is stored whole, as defined in code (dark to light); shades no troop uses have "used_by": []."""
    ramps = {}
    for name, r in sorted(candidates.items()):
        if sum(1 for c in r if c in used) >= 2: ramps[name] = r
    grouped = {c for r in ramps.values() for c in r if c in used}
    in_ramps = {c for r in ramps.values() for c in r}
    loose = sorted((c for c in used if c not in grouped), key=lambda c: (lum(c), c))
    def entry(c): return {"hex": hexs(c), "used_by": used.get(c, [])}
    shades = sum(len(r) for r in ramps.values())
    return {
        "name": label,
        "ramps": {n: {"colors": [entry(c) for c in r]} for n, r in ramps.items()},
        "loose": [entry(c) for c in loose],
        "counts": {"colors": len(in_ramps) + len(loose), "used": len(used), "ramps": len(ramps), "ramp_shades": shades,
                   "unused_shades": len([c for r in ramps.values() for c in r if c not in used]), "loose": len(loose)},
    }

def cmd_extract(a):
    tr = troops()
    src = Source(a.ref, REPO)
    cat = catalog(tr)
    common_mods = ["handlib", "graveborn", "ironclad_tiers", "tidecallers", "wildmoot", "ironclad"]
    os.makedirs(PAL_DIR, exist_ok=True)
    team_used = collections.defaultdict(set)
    house_used = {h: collections.defaultdict(set) for h in houses_of(tr)}
    for tid, house in sorted(tr.items()):
        skins, cloth = src.list(tid)
        if not skins: print("warning: no skins for %s at %s" % (tid, a.ref), file=sys.stderr)
        for n in skins:
            for c in colour_usage(src.load(tid, n)): house_used[house][c].add(tid)
        for n in cloth:
            for c in colour_usage(src.load(tid, n)): team_used[c].add(tid)
    def cands(house):
        c = {}
        for m in ["handlib"] + house_modules(house) + ["build_" + t for t, h in tr.items() if h == house]:
            c.update(cat.get(m, {}))
        return c
    meta = {"ref": a.ref, "source": "shipped troop textures at that git ref; ramps imported from the working tree"}
    for house, used in house_used.items():
        used = {c: sorted(t) for c, t in used.items()}
        p = build_palette(house, used, cands(house))
        p.update(meta); p["troops"] = sorted(t for t, h in tr.items() if h == house)
        p["files"] = "<id>_s1..s3, <id>_veteran, <id>_e*"
        write_json(os.path.join(PAL_DIR, house + ".json"), p)
        print_counts(house, p)
    allc = {}
    for m in cat.values(): allc.update(m)
    used = {c: sorted(t) for c, t in team_used.items()}
    p = build_palette("team", used, allc)
    p.update(meta); p["troops"] = sorted(tr); p["files"] = "team_cloth_<side> in every troop folder"
    write_json(os.path.join(PAL_DIR, "team.json"), p)
    print_counts("team", p)
    cross = collections.defaultdict(set)
    for house, u in house_used.items():
        for c in u: cross[c].add(house)
    shared = {c: h for c, h in cross.items() if len(h) > 1}
    print("colours shared by more than one House: %d" % len(shared))
    if not a.no_show:
        for h in list(house_used) + ["team"]: draw_swatch(load_palette(h), os.path.join(PAL_DIR, h + ".png"))

def print_counts(n, p):
    c = p["counts"]
    print("%-12s %3d used; %d ramps (%d shades, %d unused), %d loose" % (n, c["used"], c["ramps"], c["ramp_shades"], c["unused_shades"], c["loose"]))

def write_json(path, obj):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=1, sort_keys=False, ensure_ascii=False); f.write("\n")


# ---------------------------------------------------------------------------------------------- palettes in use
def load_palette(name):
    with open(os.path.join(PAL_DIR, name + ".json"), encoding="utf-8") as f: return json.load(f)
def palette_set(p):
    s = {unhex(e["hex"]) for r in p["ramps"].values() for e in r["colors"]}   # whole ramps, used or not
    s |= {unhex(e["hex"]) for e in p["loose"]}
    return s


# ---------------------------------------------------------------------------------------------- check
def scan(items, allowed):
    """items: [(label, Image)] -> report dict for one troop against the allowed colour set."""
    bad = {}
    for label, im in items:
        for c, (n, x, y) in colour_usage(im).items():
            if c in allowed: continue
            if c in bad: bad[c]["texels"] += n
            else: bad[c] = {"hex": hexs(c), "texels": n, "file": label, "x": x, "y": y}
    top = sorted(bad.values(), key=lambda e: (-e["texels"], e["hex"]))
    return {"ok": not bad, "off_texels": sum(e["texels"] for e in top), "off_colors": len(top), "top": top[:5]}

def cmd_check(a):
    tr = troops()
    team = palette_set(load_palette("team"))
    res = {}
    if a.dir:
        if not a.house: sys.exit("--dir needs --house")
        allowed = palette_set(load_palette(a.house)) | team
        label = a.ids[0] if a.ids else os.path.basename(os.path.abspath(a.dir))
        skins, cloth = [], []
        for n in sorted(os.listdir(a.dir)):
            if not n.endswith(".png"): continue
            im = Image.open(os.path.join(a.dir, n)).convert("RGBA")
            (cloth if n.startswith("cloth_") else skins).append((n, im))
        r = scan(skins, allowed); c = scan(cloth, team)
        res[label] = merge(r, c)
        res[label]["files"] = [n for n, _ in skins + cloth]
    else:
        src = Source(a.ref, a.repo or REPO)
        ids = a.ids or sorted(tr)
        for tid in ids:
            if tid not in tr: sys.exit("unknown troop %s" % tid)
            allowed = palette_set(load_palette(tr[tid])) | team
            skins, cloth = src.list(tid)
            sk = scan([(n, src.load(tid, n)) for n in skins], allowed)
            cl = scan([(n, src.load(tid, n)) for n in cloth], team)
            res[tid] = merge(sk, cl)
            res[tid]["files"] = skins + cloth
            res[tid]["house"] = tr[tid]
            if not skins: res[tid].update(ok=False, note="no skins found")
    ok = all(r["ok"] for r in res.values())
    if a.json:
        print(json.dumps({"ok": ok, "troops": res}, indent=1))
    else:
        for tid, r in res.items():
            if r["ok"]: print("%-14s OK" % tid); continue
            if r.get("note"): print("%-14s FAIL %s" % (tid, r["note"])); continue
            print("%-14s OFF-PALETTE %d texels in %d colours" % (tid, r["off_texels"], r["off_colors"]))
            for e in r["top"]:
                print("    %s  %5d texels  e.g. %s (%d,%d)" % (e["hex"], e["texels"], e["file"], e["x"], e["y"]))
        print("%d troops, %d off-palette" % (len(res), sum(1 for r in res.values() if not r["ok"])))
    sys.exit(0 if ok else 1)

def merge(r, c):
    top = sorted(r["top"] + c["top"], key=lambda e: (-e["texels"], e["hex"]))[:5]
    return {"ok": r["ok"] and c["ok"], "off_texels": r["off_texels"] + c["off_texels"],
            "off_colors": r["off_colors"] + c["off_colors"], "top": top}


# ---------------------------------------------------------------------------------------------- show
def font(size):
    try: return ImageFont.load_default(size=size)
    except TypeError: return ImageFont.load_default()

def draw_swatch(p, out, cell=22, label_w=210, per_row=24):
    f = font(12)
    rows = [(n, [(unhex(e["hex"]), bool(e["used_by"])) for e in r["colors"]]) for n, r in p["ramps"].items()]
    loose = [(unhex(e["hex"]), True) for e in p["loose"]]
    for i in range(0, len(loose), per_row):
        rows.append(("loose" if i == 0 else "", loose[i:i + per_row]))
    width = label_w + cell * max([per_row] + [len(r) for _, r in rows]) + 8
    head = 28
    im = Image.new("RGB", (width, head + cell * len(rows) + 6), (24, 24, 28))
    d = ImageDraw.Draw(im)
    d.text((6, 7), "%s: %d ramps (%d shades, %d unused, crossed), %d loose" % (p["name"], p["counts"]["ramps"], p["counts"]["ramp_shades"], p["counts"]["unused_shades"], p["counts"]["loose"]), fill=(235, 235, 235), font=f)
    for i, (n, cs) in enumerate(rows):
        y = head + i * cell
        d.text((6, y + 4), n, fill=(200, 200, 205), font=f)
        for j, (c, used) in enumerate(cs):
            box = [label_w + j * cell, y + 1, label_w + (j + 1) * cell - 2, y + cell - 2]
            d.rectangle(box, fill=c)
            if not used: d.line([box[0] + 3, box[1] + 3, box[2] - 3, box[3] - 3], fill=(255, 0, 255), width=2)
    im.save(out)

def cmd_show(a):
    tr = troops()
    names = houses_of(tr) + ["team"] if a.name == "all" else [a.name]
    for n in names:
        out = a.out if (a.out and a.name != "all") else os.path.join(PAL_DIR, n + ".png")
        os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
        draw_swatch(load_palette(n), out); print(out)


# ---------------------------------------------------------------------------------------------- register
SHARED_MODULES = ("handlib", "graveborn", "ironclad_tiers", "tidecallers", "wildmoot")

def cmd_register(a):
    house = a.house
    if not re.match(r"^[a-z][a-z0-9_]*$", house) or house == "team": sys.exit("bad House id %r (lower case, not 'team')" % house)
    out = os.path.join(a.pal_dir or PAL_DIR, house + ".json")
    if os.path.exists(out) and not a.force:
        sys.exit("%s exists: palettes are locked; changing an existing House is Kort's decision (--force only for a new, unshipped House)" % out)
    path = a.module if a.module.endswith(".py") else os.path.join(COMMON, a.module + ".py")
    if not os.path.isfile(path): sys.exit("no module %s" % path)
    modname = os.path.splitext(os.path.basename(path))[0]
    if COMMON not in sys.path: sys.path.insert(0, COMMON)
    found = dict(module_ramps(modname, load_module(modname, path)))
    if not found: sys.exit("%s defines no module-level ramps (lists of RGBA tuples, e.g. X = ramp(...))" % path)
    if a.with_:
        shared = {}
        for m in SHARED_MODULES: shared.update(dict(module_ramps(m, load_module(m, os.path.join(COMMON, m + ".py")))))
        for n in a.with_.split(","):
            n = n.strip()
            if n not in shared: sys.exit("no shared ramp %r; e.g. %s" % (n, ", ".join(sorted(shared)[:12])))
            found[n] = shared[n]
    for n, r in found.items():
        if len(r) < 4: print("warning: ramp %s has %d tones; the recipe wants 5 to 7" % (n, len(r)), file=sys.stderr)
        if any(lum(r[i]) > lum(r[i + 1]) for i in range(len(r) - 1)): sys.exit("ramp %s is not dark to light" % n)
    def entry(c): return {"hex": hexs(c), "used_by": []}
    shades = sum(len(r) for r in found.values())
    p = {"name": house, "ramps": {n: {"colors": [entry(c) for c in r]} for n, r in sorted(found.items())}, "loose": [],
         "counts": {"colors": len({tuple(c[:3]) for r in found.values() for c in r}), "used": 0, "ramps": len(found),
                    "ramp_shades": shades, "unused_shades": shades, "loose": 0},
         "modules": [modname], "troops": [],
         "source": "registered from %s before any troop shipped; run extract once its troops ship" % os.path.basename(path)}
    os.makedirs(os.path.dirname(out), exist_ok=True)
    write_json(out, p)
    if not a.no_show: draw_swatch(p, out[:-5] + ".png")
    print("registered %s: %d ramps, %d shades -> %s" % (house, len(found), shades, out))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    e = sub.add_parser("extract"); e.add_argument("--ref", default=DEFAULT_REF); e.add_argument("--no-show", action="store_true")
    c = sub.add_parser("check"); c.add_argument("ids", nargs="*"); c.add_argument("--dir"); c.add_argument("--house")
    c.add_argument("--ref"); c.add_argument("--repo"); c.add_argument("--json", action="store_true")
    s = sub.add_parser("show"); s.add_argument("name"); s.add_argument("out", nargs="?")
    r = sub.add_parser("register"); r.add_argument("house"); r.add_argument("module"); r.add_argument("--with", dest="with_")
    r.add_argument("--force", action="store_true"); r.add_argument("--no-show", action="store_true")
    r.add_argument("--pal-dir", help=argparse.SUPPRESS)   # tests write elsewhere
    a = ap.parse_args()
    {"extract": cmd_extract, "check": cmd_check, "show": cmd_show, "register": cmd_register}[a.cmd](a)

if __name__ == "__main__":
    main()
