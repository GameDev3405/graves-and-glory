#!/usr/bin/env python3
"""Judges a battle-map plan sim (BotSimulation -PsimPlan / -PsimOrder) from its botsim directory.

    python tools/plan_sim_check.py <botsim dir or server root> [...]

Reads plan-sim.csv (what was set, and where) and decisions.csv (one row per bot commander decision, with the plan columns).
  point plan  PASS when every non-retreating decision of the plan's team after the plan has its target on the planned point,
              or on another point the plan table also holds as ATTACK (there is only one plan here, so that is the point itself),
              and the team owned the point in at least one decision row of the round.
  direct order  PASS when every non-retreating decision of that bot after the order has its target at the ordered place.
Retreating rows are counted and left out of the share, as the plan says they take priority. Prints one line per plan and exits 1 on any FAIL.
"""
import csv
import math
import sys
from pathlib import Path


def rows(p):
    with open(p, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def find(arg):
    p = Path(arg)
    for c in (p, p / "botsim"):
        if (c / "plan-sim.csv").is_file():
            return c
    raise SystemExit(f"no plan-sim.csv under {arg}")


def check(d):
    plans = rows(d / "plan-sim.csv")
    dec = rows(d / "decisions.csv")
    seat = {"0": "dawn", "1": "dusk"}
    out = []
    for pl in plans:
        m, team, t0 = pl["match"], pl["team"], int(pl["second"])
        mine = [r for r in dec if r["match"] == m and int(r["second"]) >= t0]
        if pl["kind"] == "point":
            mine = [r for r in mine if r["team"] == seat[team]]
            p = int(pl["point"])
            px, pz = float(pl["x"]), float(pl["z"])
            live = [r for r in mine if r["retreating"] != "true"]
            on = [r for r in live if r["targetX"] and math.hypot(float(r["targetX"]) - px, float(r["targetZ"]) - pz) <= 2.5]
            allm = [r for r in dec if r["match"] == m]
            owned = any(r["owners"].split(";")[p] == team for r in allm)
            first = next((int(r["second"]) for r in allm if r["owners"].split(";")[p] == team), None)
            planned = sum(1 for r in live if r["planPoint"] == str(p))
            ok = bool(live) and len(on) == len(live) and owned
            out.append(dict(match=m, kind=f"point {pl['job']} p{p}", checked=len(live), retreat=len(mine) - len(live), on=len(on),
                            extra=f"plan columns on {planned}/{len(live)}; first owned at {first}s", owned=owned, ok=ok))
        else:
            army = pl["army"]
            mine = [r for r in mine if r["army"] == army]
            x, z = float(pl["x"]), float(pl["z"])
            live = [r for r in mine if r["retreating"] != "true"]
            on = [r for r in live if r["targetX"] and math.hypot(float(r["targetX"]) - x, float(r["targetZ"]) - z) <= 2.5]
            direct = sum(1 for r in live if r["planDirect"] == "true")
            ok = bool(live) and len(on) == len(live)
            out.append(dict(match=m, kind=f"order {pl['job']} bot{pl['botIndex']}", checked=len(live), retreat=len(mine) - len(live), on=len(on),
                            extra=f"plan columns direct on {direct}/{len(live)}", owned=None, ok=ok))
    return out


def main(argv):
    bad = 0
    for a in argv[1:]:
        d = find(a)
        for r in check(d):
            share = f"{r['on']}/{r['checked']}"
            own = "n/a" if r["owned"] is None else ("yes" if r["owned"] else "no")
            print(f"{'PASS' if r['ok'] else 'FAIL'}  {d}  {r['kind']}  checked {r['checked']}  retreat rows {r['retreat']}  on target {share}  owned {own}  ({r['extra']})")
            bad += 0 if r["ok"] else 1
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
