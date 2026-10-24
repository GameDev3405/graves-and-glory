#!/usr/bin/env python3
"""In-match progression gate (design 9.2 of docs/superpowers/specs/2026-10-22-in-match-progression-design.md; plan Task 11).

    python tools/progression_gate.py --off RUN [RUN ...] --on RUN [RUN ...] [--limit 10] [--min-seconds 300] [--json OUT.json]

A RUN is a headless bot-simulation server root (it holds botsim/results.csv and botsim/levels.csv), a botsim directory, or a results.csv path.
--off holds the baseline condition (-PsimProgression=off), --on the levels condition (-PsimProgression=on): the same round robin of
<house>_normal banners, one RUN per ordered pair (tools/balance_sim.sh). Match numbers restart in every run; a run keeps its own levels.csv.

Rules (design 9.2):
- Shares: every House's share with levels on is within --limit points (10) of its own baseline share. The share is the Bradley-Terry expected
  share against the other Houses (tools/balance_method.py rr): logit P(team 0 wins) = s_i - s_j + h, a draw counts half, an aborted round is left
  out, mirrors only inform the seat term. Intervals are 95%, from the information matrix (delta method for the share and for the difference).
  Rounds under --min-seconds still count in the shares: a fast win is a win.
- Comeback: the pooled rate of decided rounds won by the side trailing at 300 s (results.csv score300_0 and score300_1). The rate on is no more
  than --limit points below the rate off. Left out of the pool, each counted and printed: aborted rounds, draws, rounds under --min-seconds
  (score300 falls back to the final score there), and rounds whose score300 is a tie (nobody trails, so the round has no comeback or hold).
- Houses outside 40 to 60% in both conditions are reported, not failed (they go to the Phase 2 end balance pass).
- Level timings are a tuning aim, never a rule: median second of the first level 2 and the first level 3 per side (a side that never reached
  one is counted separately), the share of living soldiers at level 2 or higher at the end (decided rounds, winner and loser), level 3s per side
  per round. Aborted rounds are left out of the timings; all other rounds count.

Final line: gate progression shares=pass|fail comeback=pass|fail -> pass|fail. Exit status 0 pass, 1 fail, 2 unusable input.
"""
import argparse
import csv
import json
import math
import pathlib
import statistics
import sys

SEATS = ('dawn', 'dusk')
BAND = (0.40, 0.60)
AIM_FIRST_LEVEL2 = (120, 180)       # seconds: "first level 2 at 2 to 3 minutes"
AIM_WINNER_SHARE = (0.27, 0.40)     # "about a third of the winner's living soldiers at level 2 or higher at the end"
AIM_LEVEL3_PER_SIDE = (1.0, 11.99)  # "a few level 3s per round, not none and not a dozen"
EPS = 1e-9


class Unusable(Exception):
    pass


def house(banner):
    return banner[:-7] if banner.endswith('_normal') else banner


# ---------------------------------------------------------------- loading

def find_run(path):
    p = pathlib.Path(path)
    if p.is_file() and p.name == 'results.csv':
        return p, (p.parent / 'levels.csv')
    for folder in (p / 'botsim', p):
        if (folder / 'results.csv').is_file():
            return folder / 'results.csv', folder / 'levels.csv'
    raise Unusable(f'{path}: no botsim/results.csv')


def number(row, column, where):
    try:
        return float(row[column])
    except (KeyError, ValueError, TypeError):
        raise Unusable(f'{where}: column {column} is missing or not a number ({row.get(column)!r})')


def load_condition(paths):
    """The rounds of a condition: one dict per results.csv row with its two levels rows (None when levels.csv has none)."""
    rounds = []
    for run, path in enumerate(paths):
        results, levels = find_run(path)
        with results.open(newline='', encoding='utf-8') as handle:
            rows = list(csv.DictReader(handle))
        if not rows:
            raise Unusable(f'{results}: no match rows')
        by_level = {}
        if levels.is_file():
            with levels.open(newline='', encoding='utf-8') as handle:
                for row in csv.DictReader(handle):
                    by_level[(row['match'], row['team'])] = row
        for row in rows:
            where = f'{results} match {row.get("match")}'
            if row.get('winner') not in SEATS + ('draw', 'aborted'):
                raise Unusable(f'{where}: winner {row.get("winner")!r} is not dawn, dusk, draw or aborted')
            for column in ('banner0', 'banner1'):
                if not row.get(column):
                    raise Unusable(f'{where}: no {column}')
            rounds.append(dict(
                run=run, match=row['match'], banner0=house(row['banner0']), banner1=house(row['banner1']), winner=row['winner'],
                seconds=number(row, 'seconds', where), s0=number(row, 'score300_0', where), s1=number(row, 'score300_1', where),
                levels=[by_level.get((row['match'], seat)) for seat in SEATS]))
    return rounds


# ---------------------------------------------------------------- Bradley-Terry shares

def result_of(rnd):
    return {'dawn': 1.0, 'dusk': 0.0, 'draw': 0.5}[rnd['winner']]


def bt_fit(rounds, names, prior_sd=3.0):
    """tools/balance_method.py bt_fit with the covariance of the ratings kept: ratings s sum to 0, seat term h, weak normal prior."""
    import numpy as np
    k = len(names)
    idx = {n: i for i, n in enumerate(names)}
    C = np.vstack([np.eye(k - 1), -np.ones(k - 1)])
    X = np.zeros((len(rounds), k))
    y = np.array([result_of(r) for r in rounds])
    for n, r in enumerate(rounds):
        if r['banner0'] != r['banner1']:
            X[n, idx[r['banner0']]] += 1
            X[n, idx[r['banner1']]] -= 1
    D = np.hstack([X @ C, np.ones((len(rounds), 1))])
    P = np.zeros((D.shape[1], D.shape[1]))
    P[:-1, :-1] = C.T @ C / prior_sd ** 2
    P[-1, -1] = 1 / 9
    theta = np.zeros(D.shape[1])
    for _ in range(200):
        p = 1 / (1 + np.exp(-(D @ theta)))
        H = D.T @ (D * (p * (1 - p))[:, None]) + P
        step = np.linalg.solve(H, D.T @ (y - p) - P @ theta)
        theta += step
        if np.max(np.abs(step)) < 1e-10:
            break
    p = 1 / (1 + np.exp(-(D @ theta)))
    H = D.T @ (D * (p * (1 - p))[:, None]) + P
    cov = np.linalg.inv(H)
    return C @ theta[:-1], C @ cov[:-1, :-1] @ C.T, theta[-1]


def expected_shares(s, cov):
    """Each entrant's expected share against the others, mean over j of logistic(s_i - s_j), with its standard error by the delta method."""
    import numpy as np
    k = len(s)
    out = []
    for i in range(k):
        g = np.zeros(k)
        total = 0.0
        for j in range(k):
            if j == i:
                continue
            q = 1 / (1 + math.exp(-(s[i] - s[j])))
            total += q
            slope = q * (1 - q)
            g[i] += slope
            g[j] -= slope
        g /= (k - 1)
        out.append((total / (k - 1), math.sqrt(max(0.0, float(g @ cov @ g)))))
    return out


def raw_rates(rounds, names):
    """Plain win rate per House over the non-mirror rounds it played (a draw is half a win)."""
    won = {n: 0.0 for n in names}
    played = {n: 0 for n in names}
    for r in rounds:
        a, b = r['banner0'], r['banner1']
        if a == b:
            continue
        y = result_of(r)
        won[a] += y
        won[b] += 1 - y
        played[a] += 1
        played[b] += 1
    return {n: (won[n] / played[n] if played[n] else float('nan')) for n in names}, played


def share_table(rounds, names):
    kept = [r for r in rounds if r['winner'] != 'aborted']
    s, cov, h = bt_fit(kept, names)
    shares = expected_shares(s, cov)
    raw, played = raw_rates(kept, names)
    return {n: dict(share=shares[i][0], se=shares[i][1], raw=raw[n], played=played[n]) for i, n in enumerate(names)}, h, len(kept)


# ---------------------------------------------------------------- comeback

def wilson(w, n, z=1.96):
    if n == 0:
        return float('nan'), float('nan')
    p = w / n
    d = 1 + z * z / n
    c = (p + z * z / (2 * n)) / d
    half = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / d
    return c - half, c + half


def comeback(rounds, min_seconds):
    out = dict(rounds=len(rounds), aborted=0, draws=0, short=0, ties=0, n=0, won=0)
    for r in rounds:
        if r['winner'] == 'aborted':
            out['aborted'] += 1
        elif r['winner'] == 'draw':
            out['draws'] += 1
        elif r['seconds'] < min_seconds:
            out['short'] += 1
        elif r['s0'] == r['s1']:
            out['ties'] += 1
        else:
            trailing = 'dawn' if r['s0'] < r['s1'] else 'dusk'
            out['n'] += 1
            out['won'] += r['winner'] == trailing
    out['rate'] = out['won'] / out['n'] if out['n'] else float('nan')
    out['lo'], out['hi'] = wilson(out['won'], out['n'])
    return out


# ---------------------------------------------------------------- level timings

def median_or_none(values):
    return None if not values else statistics.median(values)


def mean_or_none(values):
    return None if not values else sum(values) / len(values)


def num_or_none(text):
    try:
        return float(text)
    except (TypeError, ValueError):
        return None


def timings(rounds):
    kept = [r for r in rounds if r['winner'] != 'aborted']
    rows = [(r, seat, lv) for r in kept for seat, lv in enumerate(r['levels']) if lv is not None]
    first2 = [v for _, _, lv in rows for v in [num_or_none(lv['firstLevel2'])] if v is not None]
    first3 = [v for _, _, lv in rows for v in [num_or_none(lv['firstLevel3'])] if v is not None]
    win_share, lose_share, level3 = [], [], []
    for r, seat, lv in rows:
        share = num_or_none(lv['endShareLevel2'])
        if r['winner'] in SEATS and share is not None:
            (win_share if SEATS[seat] == r['winner'] else lose_share).append(share)
        level3.append(num_or_none(lv['level3Count']) or 0.0)
    by_round = {}
    for r, seat, lv in rows:
        by_round.setdefault((r['run'], r['match']), 0.0)
        by_round[(r['run'], r['match'])] += num_or_none(lv['level3Count']) or 0.0
    per_house = {}
    for r, seat, lv in rows:
        h = house(lv['banner']) if lv.get('banner') else r['banner%d' % seat]
        e = per_house.setdefault(h, dict(first2=[], first3=[], share=[], level3=[], sides=0))
        e['sides'] += 1
        for key, col in (('first2', 'firstLevel2'), ('first3', 'firstLevel3'), ('share', 'endShareLevel2')):
            v = num_or_none(lv[col])
            if v is not None:
                e[key].append(v)
        e['level3'].append(num_or_none(lv['level3Count']) or 0.0)
    houses = {h: dict(sides=e['sides'], first_level2_median=median_or_none(e['first2']), first_level3_median=median_or_none(e['first3']),
                      end_share=mean_or_none(e['share']), level3_per_side=mean_or_none(e['level3'])) for h, e in sorted(per_house.items())}
    return dict(
        rounds=len(kept), sides=len(rows),
        first_level2_median=median_or_none(first2), first_level2_max=max(first2) if first2 else None,
        first_level2_never=len(rows) - len(first2),
        first_level3_median=median_or_none(first3), first_level3_max=max(first3) if first3 else None,
        first_level3_never=len(rows) - len(first3),
        winner_end_share=mean_or_none(win_share), loser_end_share=mean_or_none(lose_share),
        level3_per_side=mean_or_none(level3), level3_per_round=mean_or_none(list(by_round.values())) if by_round else 0.0,
        houses=houses)


# ---------------------------------------------------------------- the gate

def analyse(off_paths, on_paths, limit=10.0, min_seconds=300):
    off, on = load_condition(off_paths), load_condition(on_paths)
    names = sorted({r[c] for r in off + on for c in ('banner0', 'banner1')})
    if len(names) < 2:
        raise Unusable('fewer than two Houses in the rounds')
    for label, rounds in (('off', off), ('on', on)):
        present = {r[c] for r in rounds if r['winner'] != 'aborted' for c in ('banner0', 'banner1')}
        if present != set(names):
            raise Unusable(f'condition {label} holds Houses {sorted(present)}, the other {names}')
    t_off, h_off, n_off = share_table(off, names)
    t_on, h_on, n_on = share_table(on, names)
    shares = {}
    for n in names:
        a, b = t_off[n], t_on[n]
        delta = b['share'] - a['share']
        se = math.sqrt(a['se'] ** 2 + b['se'] ** 2)
        shares[n] = dict(off=a['share'], on=b['share'], delta=delta, off_se=a['se'], on_se=b['se'], delta_lo=delta - 1.96 * se, delta_hi=delta + 1.96 * se,
                         raw_off=a['raw'], raw_on=b['raw'], played_off=a['played'], played_on=b['played'])
        shares[n]['ok'] = abs(delta) * 100 <= limit + EPS
        shares[n]['noisy'] = shares[n]['ok'] and max(abs(delta - 1.96 * se), abs(delta + 1.96 * se)) * 100 > limit
    outside = [n for n in names if not BAND[0] <= t_off[n]['share'] <= BAND[1] and not BAND[0] <= t_on[n]['share'] <= BAND[1]]
    c_off, c_on = comeback(off, min_seconds), comeback(on, min_seconds)
    c_delta = c_on['rate'] - c_off['rate']
    c_se = math.sqrt(c_on['rate'] * (1 - c_on['rate']) / c_on['n'] + c_off['rate'] * (1 - c_off['rate']) / c_off['n']) if c_on['n'] and c_off['n'] else float('nan')
    if not (c_on['n'] and c_off['n']):
        raise Unusable('no comeback rounds in a condition (every round short, tied at 300 s, drawn or aborted)')
    rules = dict(shares=all(v['ok'] for v in shares.values()), comeback=c_delta * 100 >= -limit - EPS)
    seat_raw = {k: (sum(r['winner'] == 'dawn' for r in rs), sum(r['winner'] in SEATS for r in rs)) for k, rs in (('off', off), ('on', on))}
    return dict(houses=names, shares=shares, seat=dict(off=h_off, on=h_on), seat_raw=seat_raw, rounds=dict(off=n_off, on=n_on), outside_both=outside,
                comeback=dict(off=c_off, on=c_on, delta=c_delta, delta_lo=c_delta - 1.96 * c_se, delta_hi=c_delta + 1.96 * c_se),
                timings=dict(off=timings(off), on=timings(on)), rules=rules, passed=all(rules.values()), limit=limit, min_seconds=min_seconds,
                runs=dict(off=len(off_paths), on=len(on_paths)))


# ---------------------------------------------------------------- report

def pct(x, digits=1):
    return 'n/a' if x is None or (isinstance(x, float) and math.isnan(x)) else f'{100 * x:.{digits}f}'


def secs(x):
    return 'none' if x is None else f'{x:g}'


def within(x, aim):
    return 'n/a' if x is None else ('inside' if aim[0] <= x <= aim[1] else 'outside')


def render(res):
    out = []
    add = out.append
    lim = res['limit']
    add(f'progression gate: off {res["rounds"]["off"]} rounds in {res["runs"]["off"]} runs, on {res["rounds"]["on"]} rounds in {res["runs"]["on"]} runs '
        f'(aborted rounds left out); limit {lim:g} points; comeback pool drops rounds under {res["min_seconds"]} s')
    add(f'seat: team 0 (Dawn) won {res["seat_raw"]["off"][0]} of {res["seat_raw"]["off"][1]} decided rounds off and {res["seat_raw"]["on"][0]} of {res["seat_raw"]["on"][1]} on; '
        f'fitted seat term, team 0 wins an even pairing: off {100 / (1 + math.exp(-res["seat"]["off"])):.0f}%, on {100 / (1 + math.exp(-res["seat"]["on"])):.0f}%')
    add('')
    add(f'shares (Bradley-Terry expected share against the other Houses, 95% interval; raw = plain win rate over non-mirror rounds)')
    add(f'  {"house":12s} {"off %":>7s} {"(95%)":>14s} {"on %":>7s} {"(95%)":>14s} {"delta":>7s} {"(95%)":>16s} {"raw off":>8s} {"raw on":>7s}  rule (|delta| <= {lim:g})')
    for n in res['houses']:
        v = res['shares'][n]
        lo = lambda m, se: pct(max(0.0, m - 1.96 * se))
        hi = lambda m, se: pct(min(1.0, m + 1.96 * se))
        verdict = ('PASS' if v['ok'] else 'FAIL') + (' (interval reaches past the limit)' if v['noisy'] else '')
        add(f'  {n:12s} {pct(v["off"]):>7s} {"(" + lo(v["off"], v["off_se"]) + " to " + hi(v["off"], v["off_se"]) + ")":>14s} {pct(v["on"]):>7s} '
            f'{"(" + lo(v["on"], v["on_se"]) + " to " + hi(v["on"], v["on_se"]) + ")":>14s} {100 * v["delta"]:+7.1f} '
            f'{"(" + format(100 * v["delta_lo"], "+.1f") + " to " + format(100 * v["delta_hi"], "+.1f") + ")":>16s} {pct(v["raw_off"]):>8s} {pct(v["raw_on"]):>7s}  {verdict}')
    add(f'rule 1, every House within {lim:g} points of its own baseline: {"PASS" if res["rules"]["shares"] else "FAIL"}')
    add('')
    c = res['comeback']
    add(f'comeback (decided rounds won by the side trailing at 300 s; ties at 300 s, draws, aborted and rounds under {res["min_seconds"]} s are left out)')
    for k in ('off', 'on'):
        v = c[k]
        add(f'  {k:4s} {v["won"]} of {v["n"]} = {pct(v["rate"])}% (Wilson 95% {pct(v["lo"])} to {pct(v["hi"])}); '
            f'left out: {v["short"]} under {res["min_seconds"]} s, {v["ties"]} tied at 300 s, {v["draws"]} draws, {v["aborted"]} aborted, of {v["rounds"]} rounds')
    add(f'  difference on - off: {100 * c["delta"]:+.1f} points (95% {100 * c["delta_lo"]:+.1f} to {100 * c["delta_hi"]:+.1f})')
    add(f'rule 2, comeback rate on no more than {lim:g} points below off: {"PASS" if res["rules"]["comeback"] else "FAIL"}')
    add('')
    add('band (design 9.2: a House inside 40 to 60% is balanced; outside it in both conditions goes to the Phase 2 end balance pass, not this plan)')
    for n in res['houses']:
        v = res['shares'][n]
        mark = lambda x: 'in' if BAND[0] <= x <= BAND[1] else 'OUT'
        add(f'  {n:12s} off {pct(v["off"]):>5s}% {mark(v["off"])}, on {pct(v["on"]):>5s}% {mark(v["on"])}' + ('   <- outside in both: report' if n in res['outside_both'] else ''))
    add('')
    t = res['timings']['on']
    add(f'level timings with levels on (tuning aim, not a rule; {t["rounds"]} rounds, {t["sides"]} sides)')
    add(f'  first level 2: median {secs(t["first_level2_median"])} s (aim {AIM_FIRST_LEVEL2[0]} to {AIM_FIRST_LEVEL2[1]}: {within(t["first_level2_median"], AIM_FIRST_LEVEL2)}); '
        f'{t["first_level2_never"]} sides never reached it')
    add(f'  first level 3: median {secs(t["first_level3_median"])} s; {t["first_level3_never"]} sides never reached it')
    add(f'  end share at level 2 or higher: winner {pct(t["winner_end_share"])}% (aim about a third, {AIM_WINNER_SHARE[0] * 100:.0f} to {AIM_WINNER_SHARE[1] * 100:.0f}: '
        f'{within(t["winner_end_share"], AIM_WINNER_SHARE)}), loser {pct(t["loser_end_share"])}%')
    add(f'  level 3s per side per round: {t["level3_per_side"]:.1f} (aim a few, not none and not a dozen: {within(t["level3_per_side"], AIM_LEVEL3_PER_SIDE)}); both sides {t["level3_per_round"]:.1f} a round')
    add(f'  {"house":12s} {"sides":>5s} {"1st lvl 2":>10s} {"1st lvl 3":>10s} {"end share":>10s} {"lvl 3/side":>11s}')
    for n, h in t['houses'].items():
        add(f'  {n:12s} {h["sides"]:5d} {secs(h["first_level2_median"]):>10s} {secs(h["first_level3_median"]):>10s} {pct(h["end_share"]):>9s}% {h["level3_per_side"]:11.1f}')
    t0 = res['timings']['off']
    add(f'  baseline sanity (levels off): {t0["sides"] - t0["first_level2_never"]} sides reached level 2, level 3s per side {t0["level3_per_side"]:.1f}' if t0['sides'] else
        '  baseline sanity (levels off): no levels.csv rows')
    add('')
    add(f'gate progression shares={"pass" if res["rules"]["shares"] else "fail"} comeback={"pass" if res["rules"]["comeback"] else "fail"} -> {"pass" if res["passed"] else "fail"}')
    return '\n'.join(out)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    ap.add_argument('--off', nargs='+', required=True, help='baseline runs (server roots, botsim dirs or results.csv files)')
    ap.add_argument('--on', nargs='+', required=True, help='levels-on runs')
    ap.add_argument('--limit', type=float, default=10.0, help='points (default 10)')
    ap.add_argument('--min-seconds', type=int, default=300, help='rounds shorter than this leave the comeback pool (default 300)')
    ap.add_argument('--json', help='also write the numbers as JSON')
    args = ap.parse_args(argv)
    try:
        res = analyse(args.off, args.on, args.limit, args.min_seconds)
    except Unusable as e:
        print('progression_gate: ' + str(e), file=sys.stderr)
        return 2
    print(render(res))
    if args.json:
        pathlib.Path(args.json).write_text(json.dumps(res, indent=1, default=str), encoding='utf-8')
    return 0 if res['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
