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
    teams = [r for r in rows if r.get("scope") == "team"]
    armies = [r for r in rows if r.get("scope") == "army"]
    matches = sorted({r.get("match", "0") for r in rows}, key=lambda m: int(num(m)))
    seats = ["dawn", "dusk"]

    def by(seat, rs=teams):
        return [r for r in rs if r.get("team") == seat]

    def ratio(rs, top, bottom):
        t = sum(num(r[top]) for r in rs)
        b = sum(num(r[bottom]) for r in rs)
        return None if b == 0 else t / b

    def share(rs, col, weight="troopSeconds"):
        tot = sum(num(r[weight]) for r in rs if has(r[col]))
        return None if tot == 0 else sum(num(r[col]) * num(r[weight]) for r in rs if has(r[col])) / tot

    out = []  # (metric, target, all, dawn, dusk, verdict)
    header = set(rows[0].keys()) if rows else set()

    def add(metric, target, value, dawn, dusk, verdict):
        out.append((metric, target, value, dawn, dusk, verdict))

    def verdict(value, ok):
        return "INFO" if value is None else ("PASS" if ok(value) else "FAIL")

    def metric(name, target, fn):
        """Runs one metric; a missing column or a failed read makes only this metric INFO 'not measured'."""
        try:
            add(name, target, *fn())
        except (KeyError, ValueError, TypeError, ZeroDivisionError):
            add(name, target, "not measured", "", "", "INFO")

    def need(*cols):
        for c in cols:
            if c not in header:
                raise KeyError(c)

    def sums(rs, col):
        need(col)
        return sum(num(r[col]) for r in rs)

    def share3(col, ok, weight="troopSeconds"):
        need(col, weight)
        v = share(teams, col, weight)
        return pct(v), pct(share(by("dawn"), col, weight)), pct(share(by("dusk"), col, weight)), verdict(v, ok)

    def maxcol(rs, col):
        need(col)
        return max((num(r[col]) for r in rs), default=None)

    def fmt(x):
        return "n/a" if x is None else f"{x:.0f}"

    # idle: the FAIL is on idle that is not Hold at an own point or objective; idle on Hold there is shown separately
    metric("idle share, not on Hold (troop-seconds)", "mean < 5%", lambda: share3("idleShare", lambda x: x < 0.05))
    metric("longest idle stretch, any group, not Hold (s)", "<= 20 s",
           lambda: (fmt(maxcol(teams, "idleMaxGroupS")), fmt(maxcol(by("dawn"), "idleMaxGroupS")), fmt(maxcol(by("dusk"), "idleMaxGroupS")), verdict(maxcol(teams, "idleMaxGroupS"), lambda x: x <= 20)))
    metric("  idle on Hold at own point or objective", "(info)", lambda: share3("holdIdleShare", lambda x: True) [:3] + ("INFO",))
    metric("fight share", "(info)", lambda: share3("fightShare", lambda x: True)[:3] + ("INFO",))
    metric("objective share (troop-seconds)", ">= 70%", lambda: share3("objectiveShare", lambda x: x >= 0.70))

    def blind():
        need("blindPicks", "picks")
        if not teams:
            return "no rows", "", "", "INFO"
        bl, pk = sums(teams, "blindPicks"), sums(teams, "picks")
        return f"{bl:.0f} of {pk:.0f}", f"{sums(by('dawn'), 'blindPicks'):.0f}", f"{sums(by('dusk'), 'blindPicks'):.0f}", "PASS" if bl == 0 else "FAIL"

    metric("blind picks (no line of sight)", "0", blind)

    def ratio3(num_col, den_col, ok):
        need(num_col, den_col)
        v = ratio(teams, num_col, den_col)
        tot = sums(teams, den_col)
        return f"{pct(v)} of {tot:.0f}", pct(ratio(by("dawn"), num_col, den_col)), pct(ratio(by("dusk"), num_col, den_col)), verdict(v, ok)

    metric("trickle index (refills arriving alone)", "< 10%", lambda: ratio3("trickleAlone", "trickleTotal", lambda x: x < 0.10))
    metric("wasted abilities (casts hitting nothing)", "< 10%", lambda: ratio3("wastedCasts", "casts", lambda x: x < 0.10))

    def stuck():
        need("stuck30")
        if not teams:
            return "no rows", "", "", "INFO"
        st = sums(teams, "stuck30")
        return f"{st:.0f}", f"{sums(by('dawn'), 'stuck30'):.0f}", f"{sums(by('dusk'), 'stuck30'):.0f}", "PASS" if st == 0 else "FAIL"

    metric("troops stuck over 30 s", "0", stuck)
    # rolling 10 s window (under 2 blocks toward the spot), whole stretches bucketed; baseline re-run 2026-11-13 on 8d579205 (see AiMetrics.stuckRead)
    metric("no-progress share, 10 s+ (troop-seconds)", "<= 6% (New Kingsfield before step 1: 29%)", lambda: share3("stuck10Share", lambda x: x <= 0.06))
    metric("no-progress share, 30 s+ (troop-seconds)", "<= 2% (before step 1: 20%)", lambda: share3("stuck30Share", lambda x: x <= 0.02))
    metric("no-progress share, 60 s+ (troop-seconds)", "0 (before step 1: 14%)", lambda: share3("stuck60Share", lambda x: x == 0))

    def retreats():
        need("retreats")
        if not armies:
            return "no rows", "", "", "INFO"
        rmax = maxcol(armies, "retreats")
        return f"{sums(armies, 'retreats'):.0f} (worst {rmax:.0f})", f"{sums(by('dawn', armies), 'retreats'):.0f}", f"{sums(by('dusk', armies), 'retreats'):.0f}", verdict(rmax, lambda x: x <= 2)

    metric("retreats (total; worst army in a match)", "<= 2 per army per round", retreats)

    def per_army(col, f):
        need(col)
        def one(rs):
            return f"{sum(num(r[col]) for r in rs) / max(1, len(rs)):{f}}"
        return one(armies), one(by("dawn", armies)), one(by("dusk", armies)), "INFO"

    metric("commander target switches per army per match", "(info; audit baseline 1.9)", lambda: per_army("commanderSwitches", ".1f"))
    metric("troop target switches per army per match", "(info)", lambda: per_army("troopSwitches", ".0f"))

    def gold():
        need("unspentGold")
        def one(rs):
            return f"{sum(num(r['unspentGold']) for r in rs) / max(1, len(rs)):.0f}"
        return one(teams), one(by("dawn")), one(by("dusk")), "INFO"

    metric("unspent gold at match end (per team)", "(info)", gold)

    # tick cost
    if "tickMeanMs" in header and "tickP99Ms" in header:
        means = [num(r["tickMeanMs"]) for r in teams if has(r["tickMeanMs"])]
        p99s = [num(r["tickP99Ms"]) for r in teams if has(r["tickP99Ms"])]
    else:
        means = p99s = []
    if means and p99s:
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
    # troop metrics (2026-11-11 fixes): archer hit rate by range, Attack approach to melee contact, pacing at spawn
    tm = d / "troop-metrics.csv"
    if tm.is_file():
        with open(tm, newline="", encoding="utf-8") as f:
            tr = list(csv.DictReader(f))

        def tby(seat):
            return [r for r in tr if r.get("team") == seat]

        def hits(rs, b):
            fired = sum(num(r[f"fired{b}"]) for r in rs); hit = sum(num(r[f"hit{b}"]) for r in rs); lost = sum(num(r[f"lost{b}"]) for r in rs)
            judged = fired - lost
            return f"{pct(hit / judged) if judged > 0 else 'n/a'} of {judged:.0f}"
        for b, name in ((0, "under 8"), (8, "8-16"), (16, "16-24"), (24, "24+")):
            add(f"archer hit rate, {name} blocks", "60-80% short, falling", hits(tr, b), hits(tby("dawn"), b).split(" of")[0], hits(tby("dusk"), b).split(" of")[0], "INFO")

        def appr(rs, col):
            n = sum(num(r["approaches"]) for r in rs)
            if n == 0:
                return "n/a"
            if col == "n":
                return f"{n:.0f}"
            return f"{sum(num(r[col]) * num(r['approaches']) for r in rs if has(r[col])) / n:.2f}"
        add("Attack: pick to melee contact, mean s", "(info)", appr(tr, "approachMeanS") + f" over {appr(tr, 'n')}", appr(tby("dawn"), "approachMeanS"), appr(tby("dusk"), "approachMeanS"), "INFO")
        add("Attack: closing speed, blocks/s", "(info)", appr(tr, "approachSpeed"), appr(tby("dawn"), "approachSpeed"), appr(tby("dusk"), "approachSpeed"), "INFO")
        add("Attack: share of approach crawling", "0", appr(tr, "approachCrawl"), appr(tby("dawn"), "approachCrawl"), appr(tby("dusk"), "approachCrawl"), "INFO")

        def total(rs, col):
            return f"{sum(num(r[col]) for r in rs):.0f}"
        add("pacing at spawn: troop reversals", "0", total(tr, "spawnReversals"), total(tby("dawn"), "spawnReversals"), total(tby("dusk"), "spawnReversals"), "INFO")
        if tr and "spawnReversalsOpening" in tr[0]:
            add("  of which in the round's first 20 s", "(info)", total(tr, "spawnReversalsOpening"), total(tby("dawn"), "spawnReversalsOpening"), total(tby("dusk"), "spawnReversalsOpening"), "INFO")
        add("pacing at spawn: army facing flips", "0", total(tr, "baseYawFlips"), total(tby("dawn"), "baseYawFlips"), total(tby("dusk"), "baseYawFlips"), "INFO")

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
