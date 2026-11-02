#!/usr/bin/env python3
"""Per-mode bot-sim gate.

    python tools/mode_gate.py <results.csv> <mode>

Modes: siege, domination, convoy. Prints `mode_gate <mode> matches=<N> dawn=<P>% <pass|fail>` (plus ` aborted=<A>` when A>0),
one or two summary lines, then one reason line per failed check. Exit 0 pass, 1 fail, 2 unusable input (message on stderr).
Rows with winner=aborted are left out of every count; rows of another mode are skipped.
"""
import csv
import sys

# Columns every mode needs, and the ones each checker reads on top (results.csv of BotSimulation).
BASE_COLUMNS = ("winner", "mode")
MODE_COLUMNS = {
    "siege": ("stage", "gateBlows", "seconds"),
    "domination": ("dawnScore", "duskScore"),
    "convoy": ("checkpoints", "idleMax"),
}
# Draft bands (docs/drafts/bot-quality-targets.md and the Convoy spec's sim gate); Kort edits them there.
WIN_MIN, WIN_MAX = 0.3, 0.7
CONVOY_MIN_CHECKPOINTS = 1.5
CONVOY_MAX_STALL_SECONDS = 60
# Siege's yard clock (sheets/design.json modes.siege roundSeconds, Kort's 185 s). The bots' mean yard-fall time is 168-184 s, so a
# match the defence wins on that clock at stage 0 is a close yard fight, not a stuck attack. Such clock losses are allowed up to
# 1 - WIN_MIN of the matches (the most the win band leaves the defence); a stage-0 ending any other way still fails.
SIEGE_YARD_CLOCK_SECONDS = 185
SIEGE_MAX_CLOCK_STAGE0 = 1 - WIN_MIN


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
    clock = [r["winner"] == "dusk" and float(r["seconds"] or 0) >= SIEGE_YARD_CLOCK_SECONDS for r in rows]
    stuck = sum(1 for s, c in zip(stages, clock) if s < 1 and not c)
    if stuck:
        reasons.append("%d of %d match(es) never got past the yard (stage 0) before the %d s clock" % (stuck, n, SIEGE_YARD_CLOCK_SECONDS))
    timed = sum(1 for s, c in zip(stages, clock) if s < 1 and c)
    if timed > SIEGE_MAX_CLOCK_STAGE0 * n + 1e-9:
        reasons.append("%d of %d match(es) ran out the yard clock at stage 0, over %d%%" % (timed, n, round(100 * SIEGE_MAX_CLOCK_STAGE0)))
    counts = [sum(1 for s in stages if s == k) for k in range(4)]
    summary = ["stages 0=%d 1=%d 2=%d 3=%d" % tuple(counts), "mean stage %.2f" % (sum(stages) / n),
               "stage 0 on the yard clock %d" % timed]
    return reasons, summary


def check_domination(rows):
    """Dawn takes 30%-70% of the matches (a draw counts half), and no match ends with both scores at 0 (the bots never took a point)."""
    n = len(rows)
    wins = sum(1 for r in rows if r["winner"] == "dawn")
    draws = sum(1 for r in rows if r["winner"] == "draw")
    share = wins + 0.5 * draws
    reasons = []
    if not WIN_MIN * n <= share <= WIN_MAX * n:
        reasons.append("dawn won %g of %d (draws count half), outside 30%%-70%%" % (share, n))
    dead = sum(1 for r in rows if float(r["dawnScore"] or 0) + float(r["duskScore"] or 0) <= 0)
    if dead:
        reasons.append("%d match(es) ended with both scores at 0" % dead)
    mean = sum(float(r["dawnScore"] or 0) + float(r["duskScore"] or 0) for r in rows) / n
    return reasons, ["draws %d" % draws, "mean total score %.1f" % mean]


def check_convoy(rows):
    """The escort (dawn) wins 30%-70%, the wagon passes at least 1.5 checkpoints a match on average, and no match stalls it (Idle or
    Rolling back with two or more escort groups alive) for more than 60 s."""
    n = len(rows)
    wins = sum(1 for r in rows if r["winner"] == "dawn")
    reasons = []
    if not WIN_MIN * n <= wins <= WIN_MAX * n:
        reasons.append("the escort won %d of %d, outside 30%%-70%%" % (wins, n))
    checkpoints = [int(float(r["checkpoints"])) for r in rows]
    mean = sum(checkpoints) / n
    if mean < CONVOY_MIN_CHECKPOINTS:
        reasons.append("mean checkpoints %.2f is under %.1f" % (mean, CONVOY_MIN_CHECKPOINTS))
    stalls = [int(float(r["idleMax"] or 0)) for r in rows]
    stalled = sum(1 for v in stalls if v > CONVOY_MAX_STALL_SECONDS)
    if stalled:
        reasons.append("%d match(es) stalled the wagon over %d s (longest %d s)" % (stalled, CONVOY_MAX_STALL_SECONDS, max(stalls)))
    summary = ["checkpoints " + " ".join("%d=%d" % (k, checkpoints.count(k)) for k in sorted(set(checkpoints))),
               "mean checkpoints %.2f" % mean, "longest stall %d s" % max(stalls)]
    return reasons, summary


CHECKERS = {"siege": check_siege, "domination": check_domination, "convoy": check_convoy}


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
            missing = [c for c in BASE_COLUMNS + MODE_COLUMNS[mode] if c not in (reader.fieldnames or [])]
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
