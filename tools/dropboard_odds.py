#!/usr/bin/env python3
"""Drop board odds: exact proof that the board leaves every rarity's chance unchanged.

Rule: a roll lands in a slot with multiplier m (chance p_m). Every tier in the boosted set (rare and above) has its
chance multiplied by m / E[m]; the tiers below share what is left in their old proportions. Because the slot is drawn
independently on every roll, the chance of each tier per roll, averaged over slots, equals today's chance exactly,
so the whole sequence of results (pity counters included) has the same distribution as today.

Usage: python tools/dropboard_odds.py            prints the tables for the proposed slot tables
       python tools/dropboard_odds.py --check    exits 1 unless every table is exact, capped and pity-neutral
Reads sheets/design.json for today's weights.
"""
import json, sys
from fractions import Fraction as F
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MULTS = [10, 5, 3, 2, 1, 2, 3, 5, 10]
# slot chances in percent, left to right; symmetric
TABLES = {
    "A bell (30/20/10/4/1)": [1, 4, 10, 20, 30, 20, 10, 4, 1],
    # A gentler table is not possible: gear's rare-and-above share is 22%, so x10 needs a mean multiplier of at least 2.2.
    "B wild (20/20/12/6/2)": [2, 6, 12, 20, 20, 20, 12, 6, 2],
}
BOOSTED = {"rare", "epic", "legendary"}


def today():
    d = json.loads((ROOT / "sheets/design.json").read_text(encoding="utf-8"))
    gear = {r["id"]: F(r["rollWeight"]) for r in d["gearRarities"]}
    unit_w = {r["id"]: r["rollWeight"] for r in d["rarities"]}
    houses = {}
    for t in d["troops"]:
        houses.setdefault(t["house"], {}).setdefault(t["rarity"], 0)
        houses[t["house"]][t["rarity"]] += 1
    units = {}
    for h, n in houses.items():
        w = {r: F(n.get(r, 0) * unit_w[r]) for r in unit_w if n.get(r, 0) * unit_w[r]}
        units[h] = w
    forge = d["forge"][0]
    rec = d["recruitment"][0]
    return gear, units, forge["pity"], rec["epicPity"]


def norm(w):
    s = sum(w.values())
    return {k: v / s for k, v in w.items()}


def boosted(q, m, mean):
    """Chances of each tier on a roll that landed on multiplier m."""
    f = F(m) / mean
    hi = {k: v * f for k, v in q.items() if k in BOOSTED}
    rest = 1 - sum(hi.values())
    lo_total = sum(v for k, v in q.items() if k not in BOOSTED)
    out = dict(hi)
    for k, v in q.items():
        if k not in BOOSTED:
            out[k] = rest * v / lo_total if lo_total else F(0)
    return out, rest


def table(name, pct, q, label, show=True):
    p = [F(x) / 100 for x in pct]
    assert sum(p) == 1, name
    mean = sum(pi * m for pi, m in zip(p, MULTS))
    ok = True
    avg = {k: F(0) for k in q}
    rows = []
    for m in sorted(set(MULTS)):
        pm = sum(pi for pi, mm in zip(p, MULTS) if mm == m)
        b, rest = boosted(q, m, mean)
        if rest < 0:
            ok = False
        for k in q:
            avg[k] += pm * b[k]
        rows.append((m, pm, b))
    exact = all(avg[k] == q[k] for k in q)
    if show:
        print(f"\n{label} | table {name} | mean multiplier {float(mean):.3f} | base factor {float(1 / mean):.4f}")
        tiers = list(q)
        print("  slot   chance   " + "  ".join(f"{t:>10}" for t in tiers))
        for m, pm, b in rows:
            print(f"  x{m:<4} {float(pm) * 100:6.2f}%   " + "  ".join(f"{float(b[t]) * 100:9.2f}%" for t in tiers))
        print("  average          " + "  ".join(f"{float(avg[t]) * 100:9.2f}%" for t in tiers))
        print("  today            " + "  ".join(f"{float(q[t]) * 100:9.2f}%" for t in tiers))
        print(f"  exact match: {exact}; no chance over 100%: {ok}")
    return exact and ok


def pity_rate(q_top, pity):
    """Long-run share of rolls that give a top tier with a pity that forces one after `pity` misses (exact)."""
    # expected cycle length: sum_{k=0}^{pity} P(no top in first k rolls)
    miss = 1 - q_top
    length = sum(miss ** k for k in range(pity + 1))
    return 1 / length


def sheet():
    """The table the game uses: sheets/design.json dropSlots (chances in thousandths), checked against the board's multipliers."""
    rows = json.loads((ROOT / "sheets/design.json").read_text(encoding="utf-8")).get("dropSlots", [])
    if not rows:
        return
    if [r["multiplier"] for r in rows] != MULTS:
        sys.exit("dropSlots multipliers %s are not the board's %s" % ([r["multiplier"] for r in rows], MULTS))
    TABLES["the sheet's dropSlots (in the game)"] = [F(r["chance"], 10) for r in rows]


def main():
    sheet()
    check = "--check" in sys.argv
    gear, units, gear_pity, epic_pity = today()
    good = True
    qg = norm(gear)
    for name, pct in TABLES.items():
        good &= table(name, pct, qg, "GEAR", not check)
        first = True
        for h, w in units.items():
            good &= table(name, pct, norm(w), f"UNITS ({h})", not check and first)
            first = False
    if not check:
        top = qg["epic"] + qg["legendary"]
        print(f"\nPity: the slot is drawn fresh on every roll, so each roll's chance of a top tier is today's on average")
        print(f"  gear: {float(top) * 100:.2f}% a roll, forced after {gear_pity} misses -> one top piece every {float(1 / pity_rate(top, gear_pity)):.3f} rolls, with or without the board")
        u = norm(next(iter(units.values())))
        print(f"  units: {float(u['epic']) * 100:.2f}% a roll, forced on roll {epic_pity} -> one Epic every {float(1 / pity_rate(u['epic'], epic_pity - 1)):.3f} rolls, with or without the board")
    if check and not good:
        print("drop board odds: FAIL")
        return 1
    if check:
        print("drop board odds: all tables exact and capped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
