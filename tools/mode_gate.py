#!/usr/bin/env python3
"""Per-mode bot-sim gate.

    python tools/mode_gate.py <results.csv> <mode>

Prints `mode_gate <mode> matches=<N> dawn=<P>% <pass|fail>` (plus ` aborted=<A>` when A>0), a `stages` line,
then one reason line per failed check. Exit 0 pass, 1 fail, 2 unusable input (message on stderr).
Rows with winner=aborted are left out of every count; rows of another mode are skipped.
"""
import csv
import sys

STAGE_COLUMNS = ("winner", "mode", "stage", "gateBlows")


def check_siege(rows):
    """Return (failure reasons, extra summary lines) for usable siege rows."""
    n = len(rows)
    stages = [int(float(r["stage"])) for r in rows]
    wins = sum(1 for r in rows if r["winner"] == "dawn")
    reasons = []
    if not 0.3 * n <= wins <= 0.7 * n:
        reasons.append("attackers won %d of %d, outside 30%%-70%%" % (wins, n))
    untouched = sum(1 for r, s in zip(rows, stages) if s >= 1 and float(r["gateBlows"] or 0) <= 0)
    if untouched:
        reasons.append("%d match(es) reached the gate stage with gateBlows=0" % untouched)
    stuck = sum(1 for s in stages if s < 1)
    if stuck:
        reasons.append("%d of %d match(es) never got past the yard (stage 0)" % (stuck, n))
    counts = [sum(1 for s in stages if s == k) for k in range(4)]
    summary = ["stages 0=%d 1=%d 2=%d 3=%d" % tuple(counts), "mean stage %.2f" % (sum(stages) / n)]
    return reasons, summary


CHECKERS = {"siege": check_siege}


def fail_input(msg):
    print("mode_gate: " + msg, file=sys.stderr)
    return 2


def main(argv):
    if len(argv) != 3:
        return fail_input("usage: mode_gate.py <results.csv> <mode>")
    path, mode = argv[1], argv[2]
    if mode not in CHECKERS:
        return fail_input("no gate for mode " + mode)
    try:
        with open(path, newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            missing = [c for c in STAGE_COLUMNS if c not in (reader.fieldnames or [])]
            if missing:
                return fail_input("missing columns: " + ", ".join(missing))
            all_rows = list(reader)
    except OSError as e:
        return fail_input("cannot read %s: %s" % (path, e))
    rows = [r for r in all_rows if r["mode"] == mode]
    aborted = sum(1 for r in rows if r["winner"] == "aborted")
    rows = [r for r in rows if r["winner"] != "aborted"]
    try:
        if not rows:
            return fail_input("no usable rows for mode " + mode)
        reasons, summary = CHECKERS[mode](rows)
    except (ValueError, KeyError) as e:
        return fail_input("bad row data: %s" % e)
    n = len(rows)
    pct = round(100.0 * sum(1 for r in rows if r["winner"] == "dawn") / n, 1)
    pct_text = ("%.1f" % pct).removesuffix(".0")
    line = "mode_gate %s matches=%d dawn=%s%% %s" % (mode, n, pct_text, "fail" if reasons else "pass")
    if aborted:
        line += " aborted=%d" % aborted
    print(line)
    for s in summary + reasons:
        print(s)
    return 1 if reasons else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
