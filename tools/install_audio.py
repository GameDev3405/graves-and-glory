#!/usr/bin/env python3
"""Installs the picked audio candidates into the mod (docs/AUDIO.md).

Usage:
  python tools/install_audio.py [--picks FILE] [--cues DIR] [--dry-run] [--report] [--example]

The candidates are work/audio-cues/<cue>/<cue>_N.ogg (a few older cues are loose <cue>.ogg files), with the
source, author and licence of each in work/audio-cues/manifest.json. PICKS.json maps a cue to the file names
chosen for it: {"relic_use_fire": ["relic_use_fire_2.ogg"], "mortar": ["mortar_fire_1.ogg", ...]}.
tools/audio/PICKS.example.json is the first take of every cue.

For the picks it:
  1. copies the files into src/main/resources/assets/gravesandglory/sounds/;
  2. adds or updates one sound event per file stem (the name minus the trailing _N) in sounds.json, the picked
     takes as its variants. The stem is the cue's name for every cue whose files share one stem; mortar, ballista,
     footsteps, rarity_stings, commander_actives, map_ambience and beast_attacks have several, one event each.
     An existing event keeps its subtitle; music and ambience are streamed;
  3. appends the third-party credits to docs/CREDITS-AUDIO.md (a row already there is not repeated).
It never touches Java. --report lists the vanilla sound call sites a cue would replace, for a person to change.
--dry-run prints everything and writes nothing. --example prints a PICKS file (first take of each cue).
"""
import argparse, json, re, shutil, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOUNDS_REL = "src/main/resources/assets/gravesandglory/sounds"
JSON_REL = "src/main/resources/assets/gravesandglory/sounds.json"
CREDITS_REL = "docs/CREDITS-AUDIO.md"
NAMESPACE = "gravesandglory"

STREAMED = {"title_music", "campaign_briefing_music", "defeat_music", "overtime_music", "map_ambience",
            "camp_room_tone", "survival_wind"}

# cue -> [(java file glob under src/main/java/gg/gravesandglory, regex of the vanilla sound it would replace)].
# These are candidates found by reading the code; the person changing the call checks the context.
REPLACES = {
    "firepot_burn": [("Abilities.java", r"ITEM_FIRECHARGE_USE")],
    "beast_attacks": [("Abilities.java", r"ENTITY_(POLAR_BEAR_WARNING|WOLF_GROWL|WOLF_HOWL)")],
    "soul_leaving": [("*.java", r"PARTICLE_SOUL_ESCAPE")],
    "veteran_levelup": [("Veterans.java", r"ENTITY_PLAYER_LEVELUP")],
    "page_turn": [("WarTableHome.java", r"ITEM_BOOK_PAGE_TURN")],
    "healer_cast": [("Statuses.java", r"BLOCK_AMETHYST_BLOCK_CHIME")],
    "dummy_thud": [("GravesAndGlory.java", r"BLOCK_WOOL_(BREAK|HIT)")],
}
for _el in ("fire", "ice", "lightning", "earth", "light", "dark"):
    REPLACES["relic_use_" + _el] = [("RelicEffects.java", r"playSound\(.*SoundEvents\.")]


def stem_of(name):
    return re.sub(r"_\d+$", "", Path(name).stem)


def resolve(cues, cue, name):
    """Path of a picked file of a cue, or None. A cue is a folder, or one loose <cue>.ogg."""
    d = cues / cue
    if d.is_dir():
        p = d / name
    else:
        p = cues / name
        if p.stem != cue:
            return None
    return p if p.is_file() else None


def all_cues(cues):
    out = [p.name for p in cues.iterdir() if p.is_dir()]
    out += [p.stem for p in cues.glob("*.ogg")]
    return sorted(out)


def first_takes(cues):
    picks = {}
    for cue in all_cues(cues):
        d = cues / cue
        files = sorted(p.name for p in d.glob("*.ogg")) if d.is_dir() else [cue + ".ogg"]
        seen, first = set(), []  # the first take of every stem, so a multi-event cue keeps one of each
        for f in files:
            if stem_of(f) not in seen:
                seen.add(stem_of(f))
                first.append(f)
        picks[cue] = first
    return picks


def check_picks(cues, picks):
    problems, plan = [], []  # plan: (cue, event, src path)
    for cue, names in picks.items():
        if cue.startswith("_"):
            continue
        if not isinstance(names, list):
            problems.append(f"{cue}: picks must be a list of file names")
            continue
        if not (cues / cue).is_dir() and not (cues / (cue + ".ogg")).is_file():
            problems.append(f"{cue}: no such cue in {cues}")
            continue
        for n in names:
            p = resolve(cues, cue, n)
            if p is None:
                problems.append(f"{cue}: {n} not found")
            else:
                plan.append((cue, stem_of(n), p))
    return problems, plan


def build_events(plan):
    events = {}
    for _cue, ev, p in plan:
        lst = events.setdefault(ev, [])
        if p.name not in [q.name for q in lst]:
            lst.append(p)
    return events


def load_manifest(cues):
    f = cues / "manifest.json"
    return json.loads(f.read_text(encoding="utf-8")) if f.is_file() else {}


def credit_rows(events, manifest):
    rows = []
    cell = lambda s: str(s).replace("|", "/").replace("\n", " ")
    for ev, files in events.items():
        for p in files:
            m = manifest.get(p.stem)
            if not m:
                continue  # an original synthesis of the mod: nothing to credit
            rows.append(f"| {ev} | {p.name} | {cell(m.get('author', ''))} | {cell(m.get('licence', ''))} | "
                        f"{cell(m.get('url', ''))} | {cell(m.get('credit', ''))} |")
    return rows


CREDITS_HEAD = """# Audio credits

Third-party sound effects and music in the game's assets, written by `tools/install_audio.py` from the candidate
manifest. Each row names the sound event, the file in `assets/gravesandglory/sounds/`, the author, the licence, the
source page and the credit line the licence asks for (if any). The older Pixabay cues are credited in docs/AUDIO.md.

| Event | File | Author | Licence | Source | Credit |
| --- | --- | --- | --- | --- | --- |
"""


def update_sounds_json(text, events):
    data = json.loads(text) if text.strip() else {}
    for ev, files in events.items():
        entry = data.get(ev, {})  # an existing entry keeps its subtitle
        entry["sounds"] = [{"name": f"{NAMESPACE}:{p.stem}", "stream": ev in STREAMED} for p in files]
        data[ev] = entry
    return json.dumps(data, indent=2) + "\n", list(events)


def report(repo, cues_to_report, events_by_cue):
    src = repo / "src/main/java/gg/gravesandglory"
    lines = []
    for cue in sorted(cues_to_report):
        sites = []
        for glob, rx in REPLACES.get(cue, []):
            for f in sorted(src.glob(glob)):
                for i, ln in enumerate(f.read_text(encoding="utf-8", errors="replace").splitlines(), 1):
                    if re.search(rx, ln):
                        sites.append(f"{f.relative_to(repo).as_posix()}:{i}: {ln.strip()[:110]}")
        evs = ", ".join(sorted(events_by_cue.get(cue, []))) or cue
        if sites:
            lines.append(f"{cue} (event {evs}) replaces a vanilla sound at:")
            lines += ["    " + s for s in sites]
        else:
            lines.append(f"{cue} (event {evs}): no vanilla call site known.")
    return lines


def main(argv=None, repo=ROOT):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--cues", help="candidate folder (default work/audio-cues)")
    ap.add_argument("--picks", help="picks file (default <cues>/PICKS.json)")
    ap.add_argument("--repo", help="repository root (default: this one)")
    ap.add_argument("--dry-run", action="store_true", help="print what would change, write nothing")
    ap.add_argument("--report", action="store_true", help="list the vanilla call sites the picked cues replace")
    ap.add_argument("--example", action="store_true", help="print a PICKS file with the first take of every cue")
    a = ap.parse_args(argv)
    repo = Path(a.repo).resolve() if a.repo else Path(repo)
    cues = Path(a.cues) if a.cues else repo / "work/audio-cues"
    if not cues.is_dir():
        print(f"error: no candidate folder {cues}", file=sys.stderr)
        return 2
    if a.example:
        print(json.dumps(first_takes(cues), indent=2))
        return 0
    picks_file = Path(a.picks) if a.picks else cues / "PICKS.json"
    if picks_file.is_file():
        picks = json.loads(picks_file.read_text(encoding="utf-8"))
    elif a.report:
        picks = first_takes(cues)
    else:
        print(f"error: no picks file {picks_file} (see tools/audio/PICKS.example.json)", file=sys.stderr)
        return 2
    problems, plan = check_picks(cues, picks)
    if problems:
        print("error: bad picks, nothing written:", file=sys.stderr)
        for p in problems:
            print("  " + p, file=sys.stderr)
        return 1
    events = build_events(plan)
    events_by_cue = {}
    for cue, ev, _ in plan:
        events_by_cue.setdefault(cue, set()).add(ev)
    if a.report:
        print("\n".join(report(repo, events_by_cue.keys(), events_by_cue)))
        return 0

    sounds = repo / SOUNDS_REL
    tag = "[dry run] " if a.dry_run else ""
    for files in events.values():
        for p in files:
            dest = sounds / p.name
            note = " (replaces the existing file)" if dest.exists() else ""
            print(f"{tag}copy {p.name} -> {SOUNDS_REL}/{note}")
            if not a.dry_run:
                sounds.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(p, dest)
    jf = repo / JSON_REL
    new_json, changed = update_sounds_json(jf.read_text(encoding="utf-8") if jf.is_file() else "", events)
    print(f"{tag}sounds.json events: {', '.join(changed)}")
    cf = repo / CREDITS_REL
    cur = cf.read_text(encoding="utf-8") if cf.is_file() else CREDITS_HEAD
    have = set(cur.splitlines())
    rows = [r for r in credit_rows(events, load_manifest(cues)) if r not in have]
    print(f"{tag}{CREDITS_REL}: {len(rows)} new credit row(s)")
    if not a.dry_run:
        jf.parent.mkdir(parents=True, exist_ok=True)
        jf.write_text(new_json, encoding="utf-8", newline="\n")
        if rows or not cf.is_file():
            cf.parent.mkdir(parents=True, exist_ok=True)
            cf.write_text(cur.rstrip("\n") + "\n" + "".join(r + "\n" for r in rows), encoding="utf-8", newline="\n")
    print(f"{tag}done: {len(plan)} file(s), {len(events)} event(s). New events still need an AudioCues entry and subtitle; "
          f"run with --report for the vanilla call sites to change.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
