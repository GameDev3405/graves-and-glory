#!/usr/bin/env python3
"""Mix-fix pass for the Graves and Glory sound files (audio audit 2026-11-09, step 1).

Gain and a transparent limiter only, via ffmpeg. No new sounds, no trimming.

What it does, in order of priority:
  1. Peaks: any file whose true peak is above -1.0 dBTP is brought to about
     -1.2 dBTP. Gain down first; the limiter (alimiter, ceiling -1.2 dBFS) is
     used only when gain alone would cost more than LIMITER_AFTER_LU of
     loudness against what the file should be.
  2. Variant groups: every group listed in GROUP_TARGETS (files named
     <event>_<n>.ogg) must sit within 1.5 LU. A file more than GROUP_TRIGGER_LU
     from the group target is moved by gain to within GROUP_LAND_LU of it
     (max +/-MAX_GAIN_DB per file). A raise is never allowed to pass the peak
     ceiling (only hot files and files marked limiter may use the limiter).
  3. FILE_TARGETS: single-file level rules (ui_back, the rarity ladder).
Everything else is left byte-identical. The pass is idempotent: targets are
fixed numbers below (frozen from the audit), a file already inside its band is
never touched, and a file is only rewritten when it needs a change.

Format is kept: Ogg Vorbis, same sample rate, same channel count, same name,
libvorbis -q:a 5 (the audit's existing files average the same bitrate). Tags
are copied. Each changed file is encoded once from the current file; recover
originals with git (git checkout -- <file>); no backups are written.

Usage (run from the repo root):
  python tools/audio/mixfix.py --dry-run          # render to a temp dir, print before/after table, write nothing
  python tools/audio/mixfix.py                    # apply
  python tools/audio/mixfix.py --verify           # measure only, exit 1 if any rule fails
  python tools/audio/mixfix.py --json out.json    # also dump every measurement (before and after)
  python tools/audio/mixfix.py --only sword_clash_1 ui_back_7

ffmpeg and ffprobe are taken from $FFMPEG_DIR, PATH, or C:/ffmpeg/ffmpeg-8.1-full_build/bin.
Measurement is the audit's method: ffmpeg ebur128=peak=true (integrated LUFS and
true peak, 1 decimal), plus a 4x oversampled sample peak (astats, 3 decimals) used as
the real gate for the -1 dBTP ceiling. Files under 0.4 s cannot be gated and are fixed
on peak only.
"""
import argparse
import concurrent.futures as cf
import json
import os
import re
import shutil
import statistics
import subprocess
import sys
import tempfile

# ----------------------------------------------------------------------------- rules

SOUNDS_REL = "src/main/resources/assets/gravesandglory/sounds"

PASS_DBTP = -1.05       # gate for a file we write: its 4x-oversampled peak must be at or below this
REPORT_DBTP = -1.0      # the audit's line: a file above this counts as near-peak
CEILING_DBTP = -1.2     # where we aim, to leave room for the Vorbis re-encode
LIMITER_AFTER_LU = 2.0  # use the limiter only if gain-only would lose more than this vs desired
MAX_GAIN_DB = 6.0       # per-file gain bound for group moves
GROUP_TRIGGER_LU = 0.7  # a file further than this from its group target is moved
GROUP_LAND_LU = 0.4     # ... to this distance from the target (so a group spans <= 1.4 LU)
FILE_TOL_LU = 0.3       # tolerance for FILE_TARGETS
MIN_STEP_DB = 0.3       # never rewrite a file for a smaller change than this (idempotence)
SPREAD_LIMIT_LU = 1.5
VORBIS_Q = "5"

# Group targets in integrated LUFS (the group median in the audit, nudged only where
# peak headroom stops the quiet takes rising). Group = file name without _<n>.
GROUP_TARGETS = {
    "gate_hit": -28.0,
    "relic_use_fire": -23.3,
    "relic_use_lightning": -18.0,   # median would be -16.1, but take 3 has no headroom to rise
    "sword_clash": -19.2,           # every take is hot and take 4 cannot rise; the others come down to it
    "unit_reveal": -23.9,
    "forge_marks": -17.8,
    "harpoon_throw": -21.5,
    "hound_attack": -18.6,
    "capture_start": -20.6,
    "camp_room_tone": -27.6,
    "ambience_wind": -28.0,
    "troop_voices": -19.2,
    "title_music": -21.3,
    "relic_use_ice": -18.8,         # both takes clip; gain alone brings them level (the re-encode needs more headroom than the clipped source)
    "blacksmith_roll": -17.6,       # not in the audit's 14 (spread was 1.2) but both takes are hot, so pin the pair
    "sword_swing": -18.1,           # take 1 is hot, so the group sits a little below the median
}

# Single-file targets: file -> dict(target LUFS, max_gain, limiter allowed for a raise).
# UI family: ui_confirm_2 is -23.2 LUFS at table volume 0.5 (-29.2 effective); ui_back_7 plays at
# 0.45, so a file level of about -23 puts it within 1 LU of that.
# Rarity ladder (file LUFS + 20 log10(table volume), volumes from the audit's A1):
#   common 0.5 -> -25.3, uncommon 0.5 -> -24.3, rare 0.55 -> -23.3, epic 0.6 -> -22.3
#   (rare has no peak headroom, so it stays and the others are placed around it).
FILE_TARGETS = {
    "ui_back_7": dict(target=-23.1, max_gain=12.0, limiter=True),
    "rarity_common_1": dict(target=-19.3, max_gain=6.0, limiter=False),
    "rarity_uncommon_2": dict(target=-18.3, max_gain=6.0, limiter=False),
    "rarity_rare_2": dict(target=-18.1, max_gain=6.0, limiter=False),
    "rarity_epic_2": dict(target=-17.9, max_gain=6.0, limiter=False),
}
# Table volumes (AudioCues, from the audit) used only to check the ladder in --verify.
RARITY_VOLUMES = {"rarity_common_1": 0.5, "rarity_uncommon_2": 0.5, "rarity_rare_2": 0.55, "rarity_epic_2": 0.6}
UI_REF = {"ui_confirm_2": 0.5, "ui_back_7": 0.45}
# Files named <x>_<n> that share a prefix but are different events: not a variant group.
NOT_A_GROUP = {"formation_shout"}
# Never touched, whatever the measurement says (Kort's music files; see task).
UNTOUCHABLE = {"campaign_briefing_music_2", "music_battle_drums_1", "music_battle_grim_1",
               "music_calm_1", "overtime_music_1"}

# ----------------------------------------------------------------------------- ffmpeg


def find_bin(name):
    d = os.environ.get("FFMPEG_DIR")
    if d and os.path.exists(os.path.join(d, name + ".exe")):
        return os.path.join(d, name + ".exe")
    w = shutil.which(name)
    if w:
        return w
    p = "C:/ffmpeg/ffmpeg-8.1-full_build/bin/" + name + ".exe"
    if os.path.exists(p):
        return p
    sys.exit("cannot find " + name + " (set FFMPEG_DIR)")


FFMPEG = FFPROBE = None


def probe(path):
    o = subprocess.run([FFPROBE, "-v", "error", "-show_entries", "stream=sample_rate,channels:format=duration",
                        "-of", "json", path], capture_output=True, text=True).stdout
    j = json.loads(o)
    st = j["streams"][0]
    return float(j["format"]["duration"]), int(st["sample_rate"]), int(st["channels"])


def measure(path):
    """-> dict(I, tp, tpp, dur, sr, ch). I is None when too short to gate."""
    e = subprocess.run([FFMPEG, "-hide_banner", "-nostats", "-i", path, "-af",
                        "ebur128=peak=true,aformat=sample_fmts=dblp,aresample=176400,aformat=sample_fmts=dblp,astats=metadata=0:measure_overall=Peak_level:measure_perchannel=none",
                        "-f", "null", "-"], capture_output=True, text=True).stderr
    tail = e[e.rfind("Summary:"):]
    mi = re.search(r"I:\s+(-?[\d.]+|-inf) LUFS", tail)
    mt = re.search(r"Peak:\s+(-?[\d.]+|-inf) dBFS", tail)
    mp = re.search(r"Peak level dB:\s+(-?[\d.]+|-inf)", e)
    I = float(mi.group(1)) if mi and mi.group(1) != "-inf" else None
    if I is not None and I <= -69.9:
        I = None
    dur, sr, ch = probe(path)
    return dict(I=I, tp=float(mt.group(1)), tpp=float(mp.group(1)), dur=dur, sr=sr, ch=ch)


def encode(src, dst, gain_db, limit_dbfs, meta):
    af = []
    if abs(gain_db) > 1e-6:
        af.append("volume=%.3fdB" % gain_db)
    if limit_dbfs is not None:
        af.append("alimiter=limit=%.4f:attack=5:release=50:level=0" % (10 ** (limit_dbfs / 20.0)))
    cmd = [FFMPEG, "-v", "error", "-y", "-i", src]
    if af:
        cmd += ["-af", ",".join(af)]
    cmd += ["-map_metadata", "0", "-c:a", "libvorbis", "-q:a", VORBIS_Q, "-ar", str(meta["sr"]), "-ac", str(meta["ch"]), dst]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError(r.stderr)


# ----------------------------------------------------------------------------- planning


def group_of(name):
    m = re.match(r"^(.*)_\d+$", name)
    return m.group(1) if m else name


def plan(name, m):
    """Decide what to do for one file. Returns dict(reasons, desired, g_want, mode, g, max_gain, note) or None."""
    if name in UNTOUCHABLE:
        return None
    I, tpp = m["I"], m["tpp"]
    reasons = []
    desired = I
    max_gain = MAX_GAIN_DB
    allow_lim = False
    ft = FILE_TARGETS.get(name)
    grp = group_of(name)
    if ft and I is not None:
        max_gain = ft["max_gain"]
        allow_lim = ft["limiter"]
        if abs(I - ft["target"]) > FILE_TOL_LU:
            desired = ft["target"]
            reasons.append("file level")
    elif grp in GROUP_TARGETS and I is not None:
        t = GROUP_TARGETS[grp]
        if abs(I - t) > GROUP_TRIGGER_LU:
            desired = t + max(-GROUP_LAND_LU, min(GROUP_LAND_LU, I - t))
            reasons.append("group level")
    hot = tpp > PASS_DBTP
    if hot:
        reasons.append("clip" if m["tp"] > 0 else "peak")
    if not reasons:
        return None
    g_want = 0.0 if I is None else max(-max_gain, min(max_gain, desired - I))
    g_cap = CEILING_DBTP - tpp
    out = dict(reasons=reasons, desired=desired, max_gain=max_gain, note="",
               level=any(r.endswith("level") for r in reasons))
    if g_want <= g_cap:
        out.update(mode="gain", g=g_want)
    elif hot or allow_lim:
        loss = 0.0 if I is None else desired - (I + g_cap)
        if (hot and loss <= LIMITER_AFTER_LU) or I is None:
            out.update(mode="gain", g=g_cap, peakcap=True, g_hi=g_want)
        else:
            out.update(mode="limit", g=g_want)
    else:
        if g_cap >= MIN_STEP_DB:
            out.update(mode="gain", g=g_cap, note="raise capped by peak headroom", peakcap=True, g_hi=g_cap)
        else:
            return dict(reasons=reasons, desired=desired, max_gain=max_gain, mode="skip", g=0.0,
                        note="raise blocked, no peak headroom")
    if out["mode"] == "gain" and abs(out["g"]) < MIN_STEP_DB and not hot:
        return None
    return out


def process(name, src, tmpdir, m, p):
    """Render one file to tmpdir and return (path, after-measurement, info). Always encodes from src
    (one generation); iterates the gain (or the limiter ceiling) until the peak gate and the
    loudness aim are met."""
    g = p["g"]
    lim = CEILING_DBTP if p["mode"] == "limit" else None
    peakcap = p.get("peakcap", False)
    g_hi = p.get("g_hi", g)
    info = dict(limiter=lim is not None)
    best = None  # (score, path, g, lim, a)
    seen = set()
    for it in range(8):
        key = (round(g, 2), None if lim is None else round(lim, 2))
        if key in seen:
            break
        seen.add(key)
        dst = os.path.join(tmpdir, "%s.%d.ogg" % (name, it))
        encode(src, dst, g, lim, m)
        a = measure(dst)
        valid = a["tpp"] <= PASS_DBTP and a["tp"] <= REPORT_DBTP
        nxt = None
        if not valid:
            over = a["tpp"] - CEILING_DBTP
            nxt = (g, lim - (over + 0.05)) if lim is not None else (g - (over + 0.05), lim)
        else:
            level = (p.get("level", False) or lim is not None) and m["I"] is not None
            if level:
                err = p["desired"] - a["I"]
                score = -abs(err)
                if abs(err) > (0.1 if lim is not None else 0.15):
                    ng = max(-p["max_gain"], min(p["max_gain"], g + err))
                    if abs(ng - g) >= 0.05:
                        nxt = (ng, lim)
            elif peakcap:
                score = g
                if a["tpp"] < CEILING_DBTP - 0.3 and g < g_hi - 0.05:
                    nxt = (min(g_hi, g + (CEILING_DBTP - a["tpp"])), lim)
            else:
                score = 0
            if best is None or score > best[0]:
                best = (score, dst, g, lim, a)
        if nxt is None:
            break
        g, lim = nxt
    if best is None:
        info["fail"] = "peak ceiling not reached"
        return None, a, info
    _, dst, g, lim, a = best
    info.update(gain=g, lim=lim)
    return dst, a, info


# ----------------------------------------------------------------------------- reporting


def fmt(v, w=6):
    return ("%*s" % (w, "n/a")) if v is None else ("%*.1f" % (w, v))


def groups_of(data):
    g = {}
    for name, m in data.items():
        k = group_of(name)
        if k in NOT_A_GROUP:
            continue
        g.setdefault(k, []).append(name)
    return {k: v for k, v in g.items() if len(v) > 1}


def spread(data, names):
    ls = [data[n]["I"] for n in names if data[n]["I"] is not None]
    return (max(ls) - min(ls)) if len(ls) > 1 else 0.0


def summary(data, title):
    clip = [n for n, m in data.items() if m["tp"] > 0 or m["tpp"] > 0]
    hot = [n for n, m in data.items() if n not in clip and (m["tp"] > REPORT_DBTP or m["tpp"] > REPORT_DBTP)]
    uneven = {k: spread(data, v) for k, v in groups_of(data).items() if spread(data, v) > SPREAD_LIMIT_LU}
    print("%s: %d files, clipping(>0 dBTP) %d, near-peak(>-1.0) %d, groups over %.1f LU: %d" %
          (title, len(data), len(clip), len(hot), SPREAD_LIMIT_LU, len(uneven)))
    return clip, hot, uneven


def ladder_and_ui(data):
    lines = []
    eff = {}
    for n, v in RARITY_VOLUMES.items():
        if data[n]["I"] is not None:
            import math
            eff[n] = data[n]["I"] + 20 * math.log10(v)
    order = ["rarity_common_1", "rarity_uncommon_2", "rarity_rare_2", "rarity_epic_2"]
    steps = [eff[b] - eff[a] for a, b in zip(order, order[1:])]
    lines.append("rarity effective LUFS: " + ", ".join("%s %.1f" % (n.split("_")[1], eff[n]) for n in order)
                 + "; steps " + ", ".join("%+.1f" % s for s in steps))
    import math
    ub = data["ui_back_7"]["I"] + 20 * math.log10(UI_REF["ui_back_7"])
    uc = data["ui_confirm_2"]["I"] + 20 * math.log10(UI_REF["ui_confirm_2"])
    lines.append("ui_back effective %.1f vs ui_confirm %.1f (diff %+.1f)" % (ub, uc, ub - uc))
    ok = all(0.3 <= s <= 1.5 for s in steps) and abs(ub - uc) <= 1.5
    return lines, ok


def check_rules(data):
    problems = []
    for n, m in data.items():
        if n in UNTOUCHABLE:
            continue
        if m["tpp"] > REPORT_DBTP or m["tp"] > REPORT_DBTP:
            problems.append("peak %s tp=%.1f tpp=%.2f" % (n, m["tp"], m["tpp"]))
    for k, v in groups_of(data).items():
        if k in GROUP_TARGETS and spread(data, v) > SPREAD_LIMIT_LU:
            problems.append("spread %s %.1f LU" % (k, spread(data, v)))
    _, ok = ladder_and_ui(data)
    if not ok:
        problems.append("rarity ladder / ui_back out of range")
    return problems


def main():
    global FFMPEG, FFPROBE
    ap = argparse.ArgumentParser(description="Gain/limiter mix-fix for the mod's sounds")
    ap.add_argument("--dry-run", action="store_true", help="render to a temp dir and print the before/after table; write nothing")
    ap.add_argument("--verify", action="store_true", help="measure only; exit 1 if a rule fails")
    ap.add_argument("--json", help="write all measurements to this file")
    ap.add_argument("--only", nargs="*", help="limit to these file stems")
    ap.add_argument("--root", default=None, help="repo root (default: two levels above this script)")
    args = ap.parse_args()
    FFMPEG, FFPROBE = find_bin("ffmpeg"), find_bin("ffprobe")
    root = args.root or os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
    sdir = os.path.join(root, SOUNDS_REL)
    names = sorted(f[:-4] for f in os.listdir(sdir) if f.endswith(".ogg"))
    paths = {n: os.path.join(sdir, n + ".ogg") for n in names}
    with cf.ThreadPoolExecutor(8) as ex:
        before = dict(zip(names, ex.map(lambda n: measure(paths[n]), names)))
    clip0, hot0, un0 = summary(before, "before")
    if args.verify:
        for line in ladder_and_ui(before)[0]:
            print(line)
        probs = check_rules(before)
        for p in probs:
            print("FAIL", p)
        print("verify:", "ok" if not probs else "%d problem(s)" % len(probs))
        if args.json:
            json.dump(dict(before=before), open(args.json, "w"), indent=1)
        sys.exit(1 if probs else 0)

    todo = {}
    for n in names:
        if args.only and n not in args.only:
            continue
        p = plan(n, before[n])
        if p:
            todo[n] = p
    after = dict(before)
    rows = []
    tmp = tempfile.mkdtemp(prefix="mixfix_")
    try:
        def job(n):
            return n, process(n, paths[n], tmp, before[n], todo[n])
        with cf.ThreadPoolExecutor(4) as ex:
            results = dict(ex.map(job, [n for n in todo if todo[n]["mode"] != "skip"]))
        for n, p in todo.items():
            b = before[n]
            if p["mode"] == "skip":
                rows.append((n, "+".join(p["reasons"]), "skip", None, b, b, p["note"]))
                continue
            dst, a, info = results[n]
            if "fail" in info:
                rows.append((n, "+".join(p["reasons"]), "FAIL", None, b, b, info["fail"]))
                continue
            # sanity: format unchanged, length unchanged
            if (a["sr"], a["ch"]) != (b["sr"], b["ch"]) or abs(a["dur"] - b["dur"]) > 0.03:
                rows.append((n, "+".join(p["reasons"]), "FAIL", None, b, b, "format/length changed, not written"))
                continue
            after[n] = a
            mode = ("limit %.1f dBFS" % info["lim"]) if info.get("limiter") else "gain"
            rows.append((n, "+".join(p["reasons"]), "%s %+.1f dB" % (mode, info["gain"]), info["gain"], b, a, p["note"]))
            if not args.dry_run:
                shutil.copyfile(dst, paths[n])
    finally:
        shutil.rmtree(tmp, ignore_errors=True)

    print()
    print("%-26s %-18s %-26s %6s %6s %6s %6s  %s" % ("file", "why", "action", "I from", "I to", "TP from", "TP to", "note"))
    for n, why, act, g, b, a, note in rows:
        print("%-26s %-18s %-26s %s %s %s %s  %s" % (n, why, act, fmt(b["I"]), fmt(a["I"]), fmt(b["tp"], 7), fmt(a["tp"], 6), note))
    print()
    clip1, hot1, un1 = summary(after, "after" + (" (dry run, nothing written)" if args.dry_run else ""))
    for k in sorted(set(un0) | set(un1)):
        print("  group %-22s spread %.1f -> %.1f LU" % (k, un0.get(k, spread(before, groups_of(before)[k])), spread(after, groups_of(after)[k])))
    for line in ladder_and_ui(after)[0]:
        print(line)
    probs = check_rules(after)
    for p in probs:
        print("REMAINING", p)
    if args.json:
        json.dump(dict(before=before, after=after, changed=[r[0] for r in rows if r[2] not in ("skip", "FAIL")]),
                  open(args.json, "w"), indent=1)


if __name__ == "__main__":
    main()
