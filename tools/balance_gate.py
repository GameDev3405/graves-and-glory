#!/usr/bin/env python3
"""Phase 2 balance gate: how often banner <bannerId> beats its opponent over two seat-swapped bot-simulation runs.

    python tools/balance_gate.py <csv1> <csv2> <bannerId> [--detail]

csv1 and csv2 are the botsim/results.csv files of the two runs in docs/superpowers/plans/2026-10-17-phase-2-houses.md
("Bot balance run"): the same two banners, with <bannerId> on one team in the first run and on the other team in the
second, so the seat (Dawn's or Dusk's base) cancels out. The banners come from the banner0 and banner1 columns that
BotSimulation writes (-PsimBanner0 and -PsimBanner1).

P is the percentage of decided rounds <bannerId> won; a draw counts half a win and an aborted round (a round that stopped
without Campaign.finish) is left out, and named after the verdict as " aborted=N". The gate passes when 40 <= P <= 60,
P as printed (rounded to one decimal, a trailing .0 dropped). Output, on one line:

    gate <a> vs <b> win=<P>% pass|fail

--detail first prints the per-run and per-map tallies. Exit status: 0 pass, 1 fail, 2 unusable input.
"""
import csv
import pathlib
import sys

SEATS = ('dawn', 'dusk')
PASS_MIN, PASS_MAX = 40.0, 60.0


def die(message):
    print('balance_gate: ' + message, file=sys.stderr)
    sys.exit(2)


def load(path):
    file = pathlib.Path(path)
    if not file.is_file():
        die(f'{path}: no such file')
    with file.open(newline='', encoding='utf-8') as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        die(f'{path}: no match rows')
    for column in ('map', 'winner', 'banner0', 'banner1'):
        if column not in rows[0]:
            die(f'{path}: no {column} column (a results.csv written before -PsimBanner0 and -PsimBanner1 existed?)')
    for number, row in enumerate(rows, 2):
        if row['winner'] not in SEATS + ('draw', 'aborted'):
            die(f'{path}: line {number}: winner {row["winner"]!r} is not dawn, dusk, draw or aborted')
    return rows


def seat_of(rows, banner, path, order):
    """The team that fields the banner in every row of this run; in a mirror run (both teams field it) the run's order decides."""
    seats = set()
    for number, row in enumerate(rows, 2):
        held = [team for team, column in enumerate(('banner0', 'banner1')) if row[column] == banner]
        if not held:
            die(f'{path}: line {number}: neither team fields {banner} (banner0={row["banner0"]}, banner1={row["banner1"]})')
        seats.add(held[0] if len(held) == 1 else order)
    if len(seats) != 1:
        die(f'{path}: {banner} sits on different teams in different rounds')
    return seats.pop()


def tally(rows, seat):
    won = lost = drawn = aborted = 0
    for row in rows:
        winner = row['winner']
        if winner == 'aborted':
            aborted += 1
        elif winner == 'draw':
            drawn += 1
        elif winner == SEATS[seat]:
            won += 1
        else:
            lost += 1
    return won, lost, drawn, aborted


def main(argv):
    detail = '--detail' in argv
    args = [a for a in argv if a != '--detail']
    if len(args) != 3:
        die('usage: balance_gate.py <csv1> <csv2> <bannerId> [--detail]')
    first, second, banner = args
    runs = []
    for order, path in enumerate((first, second)):
        rows = load(path)
        runs.append((path, rows, seat_of(rows, banner, path, order)))
    if runs[0][2] == runs[1][2]:
        die(f'{banner} sits on team {runs[0][2]} in both runs; the second run must swap the seats')
    opponents = {row[('banner1', 'banner0')[seat]] for _, rows, seat in runs for row in rows}
    if len(opponents) != 1:
        die(f'the runs field more than one opponent for {banner}: {", ".join(sorted(opponents))}')
    opponent = opponents.pop()
    won = lost = drawn = aborted = 0
    for path, rows, seat in runs:
        w, l, d, a = tally(rows, seat)
        won, lost, drawn, aborted = won + w, lost + l, drawn + d, aborted + a
        if detail:
            print(f'{path}: {banner} on {SEATS[seat]}: won={w} lost={l} drawn={d} aborted={a}')
    decided = won + lost + drawn
    if decided == 0:
        die('every round was aborted; nothing to judge')
    if detail:
        for path, rows, seat in runs:
            for map_id in sorted({row['map'] for row in rows}, key=int):
                w, l, d, a = tally([row for row in rows if row['map'] == map_id], seat)
                print(f'  map {map_id} ({SEATS[seat]}): won={w} lost={l} drawn={d} aborted={a}')
        print(f'rounds={decided + aborted} decided={decided} won={won} lost={lost} drawn={drawn}')
    percent = round(100.0 * (won + 0.5 * drawn) / decided, 1)
    passed = PASS_MIN <= percent <= PASS_MAX
    print(f'gate {banner} vs {opponent} win={percent:g}% {"pass" if passed else "fail"}' + (f' aborted={aborted}' if aborted else ''))
    return 0 if passed else 1


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:]))
