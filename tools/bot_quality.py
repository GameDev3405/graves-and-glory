#!/usr/bin/env python3
"""Bot quality report: botsim/ai-metrics.csv against the sim-metric targets of
docs/reports/ai-audit-and-design-2026-11-09.md section 7.8 (and the retreat and tick-cost targets of
docs/drafts/bot-quality-targets.md and section 7.7).

    python tools/bot_quality.py <botsim dir or sim server root>

Report-only: it prints each metric as PASS, FAIL or INFO and always exits 0 (no gate yet).
PASS/FAIL is decided on the whole run (every match, both teams pooled); the Dawn and Dusk columns show where a number comes from.
Inputs written by BotSimulation (test-tools): ai-metrics.csv (one row per bot army and per team, per match), and when present results.csv and decisions.csv.
"""
import csv
import statistics
import sys
from pathlib import Path


def find_dir(arg):
    p = Path(arg)
    for cand in (p, p / "botsim"):
        if (cand / "ai-metrics.csv").is_file():
            return cand
    return None


def num(v, default=0.0):
    try:
        return float(v)
    except (TypeError, ValueError):
        return default


def has(v):
    return v not in ("", None)


def pct(x):
    return "n/a" if x is None else f"{100 * x:.1f}%"


def main(argv):
    if len(argv) != 2:
        print(__doc__)
        return 0
    d = find_dir(argv[1])
    if d is None:
        print(f"bot_quality: no ai-metrics.csv under {argv[1]} (or its botsim/)")
        return 0
    with open(d / "ai-metrics.csv", newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    teams = [r for r in rows if r["scope"] == "team"]
    armies = [r for r in rows if r["scope"] == "army"]
    matches = sorted({r["match"] for r in rows}, key=int)
    seats = ["dawn", "dusk"]

    def by(seat, rs=teams):
        return [r for r in rs if r["team"] == seat]

    def ratio(rs, top, bottom):
        t = sum(num(r[top]) for r in rs)
        b = sum(num(r[bottom]) for r in rs)
        return None if b == 0 else t / b

    def share(rs, col, weight="troopSeconds"):
        tot = sum(num(r[weight]) for r in rs if has(r[col]))
        return None if tot == 0 else sum(num(r[col]) * num(r[weight]) for r in rs if has(r[col])) / tot

    out = []  # (metric, target, all, dawn, dusk, verdict)

    def add(metric, target, value, dawn, dusk, verdict):
        out.append((metric, target, value, dawn, dusk, verdict))

    def verdict(value, ok):
        return "INFO" if value is None else ("PASS" if ok(value) else "FAIL")

    # idle
    v = share(teams, "idleShare")
    add("idle share (troop-seconds)", "mean < 5%", pct(v), pct(share(by("dawn"), "idleShare")), pct(share(by("dusk"), "idleShare")), verdict(v, lambda x: x < 0.05))
    worst = max((num(r["idleMaxGroupS"]) for r in teams), default=None)
    wd = max((num(r["idleMaxGroupS"]) for r in by("dawn")), default=None)
    wk = max((num(r["idleMaxGroupS"]) for r in by("dusk")), default=None)
    add("longest idle stretch, any group (s)", "<= 20 s", "n/a" if worst is None else f"{worst:.0f}", "n/a" if wd is None else f"{wd:.0f}", "n/a" if wk is None else f"{wk:.0f}", verdict(worst, lambda x: x <= 20))
    hv = share(teams, "holdIdleShare")
    add("  of which idle on Hold", "(info)", pct(hv), pct(share(by("dawn"), "holdIdleShare")), pct(share(by("dusk"), "holdIdleShare")), "INFO")
    fv = share(teams, "fightShare")
    add("fight share", "(info)", pct(fv), pct(share(by("dawn"), "fightShare")), pct(share(by("dusk"), "fightShare")), "INFO")
    # objective
    v = share(teams, "objectiveShare")
    add("objective share (troop-seconds)", ">= 70%", pct(v), pct(share(by("dawn"), "objectiveShare")), pct(share(by("dusk"), "objectiveShare")), verdict(v, lambda x: x >= 0.70))
    # blind picks
    bl = sum(num(r["blindPicks"]) for r in teams)
    pk = sum(num(r["picks"]) for r in teams)
    add("blind picks (no line of sight)", "0", f"{bl:.0f} of {pk:.0f}", f"{sum(num(r['blindPicks']) for r in by('dawn')):.0f}", f"{sum(num(r['blindPicks']) for r in by('dusk')):.0f}", "PASS" if bl == 0 else "FAIL")
    # trickle
    v = ratio(teams, "trickleAlone", "trickleTotal")
    tot = sum(num(r["trickleTotal"]) for r in teams)
    add("trickle index (refills arriving alone)", "< 10%", f"{pct(v)} of {tot:.0f}", pct(ratio(by("dawn"), "trickleAlone", "trickleTotal")), pct(ratio(by("dusk"), "trickleAlone", "trickleTotal")), verdict(v, lambda x: x < 0.10))
    # wasted
    v = ratio(teams, "wastedCasts", "casts")
    tot = sum(num(r["casts"]) for r in teams)
    add("wasted abilities (casts hitting nothing)", "< 10%", f"{pct(v)} of {tot:.0f}", pct(ratio(by("dawn"), "wastedCasts", "casts")), pct(ratio(by("dusk"), "wastedCasts", "casts")), verdict(v, lambda x: x < 0.10))
    # stuck
    st = sum(num(r["stuck30"]) for r in teams)
    add("troops stuck over 30 s", "0", f"{st:.0f}", f"{sum(num(r['stuck30']) for r in by('dawn')):.0f}", f"{sum(num(r['stuck30']) for r in by('dusk')):.0f}", "PASS" if st == 0 else "FAIL")
    # retreats
    rmax = max((num(r["retreats"]) for r in armies), default=None)
    rsum = sum(num(r["retreats"]) for r in armies)
    add("retreats (total; worst army in a match)", "<= 2 per army per round", f"{rsum:.0f} (worst {0 if rmax is None else rmax:.0f})", f"{sum(num(r['retreats']) for r in by('dawn')):.0f}", f"{sum(num(r['retreats']) for r in by('dusk')):.0f}", verdict(rmax, lambda x: x <= 2))
    # switches
    nm = max(1, len(matches))

    def per_army(rs, col):
        n = max(1, len([r for r in rs if True]))
        return sum(num(r[col]) for r in rs) / n

    add("commander target switches per army per match", "(info; audit baseline 1.9)", f"{per_army(armies, 'commanderSwitches'):.1f}",
        f"{per_army(by('dawn', armies), 'commanderSwitches'):.1f}", f"{per_army(by('dusk', armies), 'commanderSwitches'):.1f}", "INFO")
    add("troop target switches per army per match", "(info)", f"{per_army(armies, 'troopSwitches'):.0f}",
        f"{per_army(by('dawn', armies), 'troopSwitches'):.0f}", f"{per_army(by('dusk', armies), 'troopSwitches'):.0f}", "INFO")
    # gold
    def gold(rs):
        return sum(num(r["unspentGold"]) for r in rs) / max(1, len(rs))

    add("unspent gold at match end (per team)", "(info)", f"{gold(teams):.0f}", f"{gold(by('dawn')):.0f}", f"{gold(by('dusk')):.0f}", "INFO")
    # tick cost
    means = [num(r["tickMeanMs"]) for r in teams if has(r["tickMeanMs"])]
    p99s = [num(r["tickP99Ms"]) for r in teams if has(r["tickP99Ms"])]
    if means:
        add("server tick, mean ms (whole server, not only AI)", "< 4 ms at 160 troops", f"{statistics.mean(means):.2f}", "", "", verdict(statistics.mean(means), lambda x: x < 4))
        add("server tick, p99 ms (worst match)", "< 8 ms at 160 troops", f"{max(p99s):.2f}", "", "", verdict(max(p99s), lambda x: x < 8))
    else:
        add("server tick, mean and p99 ms", "< 4 / < 8 ms", "no probe", "", "", "INFO")
    # not measured here
    add("purpose coverage", "100% of troop-seconds", "not measured", "", "", "INFO")
    add("orders per minute, reaction time", "Kort's own play", "not measured", "", "", "INFO")
    res = d / "results.csv"
    if res.is_file():
        with open(res, newline="", encoding="utf-8") as f:
            rr = list(csv.DictReader(f))
        dw = sum(1 for r in rr if r["winner"] == "dawn")
        dk = sum(1 for r in rr if r["winner"] == "dusk")
        n = len(rr)
        add("win spread (needs seat-swapped pair)", "40-60% per pair", f"dawn {dw}, dusk {dk} of {n}", "", "", "INFO")

    w = max(len(o[0]) for o in out)
    tw = max(len(o[1]) for o in out)
    print(f"bot quality: {d}  ({len(matches)} matches, {len(armies)} army rows)")
    print(f"{'metric'.ljust(w)}  {'target'.ljust(tw)}  {'all'.ljust(24)}  {'dawn'.ljust(8)}  {'dusk'.ljust(8)}  verdict")
    for m, t, a, dn, dk, vd in out:
        print(f"{m.ljust(w)}  {t.ljust(tw)}  {a.ljust(24)}  {dn.ljust(8)}  {dk.ljust(8)}  {vd}")
    c = {k: sum(1 for o in out if o[5] == k) for k in ("PASS", "FAIL", "INFO")}
    print(f"PASS {c['PASS']}  FAIL {c['FAIL']}  INFO {c['INFO']}  (report only; exit 0)")
    dec = d / "decisions.csv"
    if dec.is_file():
        with open(dec, newline="", encoding="utf-8") as f:
            n = sum(1 for _ in f) - 1
        print(f"decisions.csv: {n} commander decisions logged")
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv))
    except Exception as e:  # report-only: never fail a pipeline
        print(f"bot_quality: could not read the metrics: {e}")
        sys.exit(0)
