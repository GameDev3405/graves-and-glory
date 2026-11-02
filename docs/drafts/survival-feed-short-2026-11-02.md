# Survival feed lines, shorter drafts (2026-11-02)

Draft copy for Kort's approval. `ModeMath` and `SurvivalMode` are unchanged. Widths are GUI pixels from `FeedFitCheck.width`, the same measure as `ModeHudFitCheck`; the Compact feed row holds 144 px. Check: `test-tools/standalone/SurvivalFeedShortCheck.java` (all proposed lines fit, widest 136 px, every site name kept; add `-Dshort.table=true` to reprint this table).

## Changes
- Shrine line: "The dead take the shrine".
- Win line: "The shrine stands till dawn" (145 px was 1 over).
- Rise line: "Dead rise: the ford", the sheet's site name kept whole. Two or three sites give one feed line each, because three names cannot fit 144 px on one line. This needs a small change in `SurvivalMode.warn` (one `feed` call per site). Compact shows 3 lines, so a three-site rise fills the feed for its 6 s.
- Rest line with gold: "Wave 5 at rest, +100 gold". Without gold it already fits and stays "Wave 7 laid to rest".
- "The dead rise" (no sites) and "The dead hold the shrine" already fit and stay.

| Current line | px | Proposed line | px |
|---|---|---|---|
| The dead are taking the shrine | 157 | The dead take the shrine | 127 |
| The shrine stands until dawn | 145 | The shrine stands till dawn | 136 |
| The dead hold the shrine | 127 | The dead hold the shrine | 127 |
| The dead rise | 70 | The dead rise | 70 |
| Wave 1 laid to rest, +60 gold | 144 | Wave 1 at rest, +60 gold | 123 |
| Wave 1 laid to rest | 95 | Wave 1 laid to rest | 95 |
| Wave 2 laid to rest, +60 gold | 144 | Wave 2 at rest, +60 gold | 123 |
| Wave 2 laid to rest | 95 | Wave 2 laid to rest | 95 |
| Wave 3 laid to rest, +80 gold | 144 | Wave 3 at rest, +80 gold | 123 |
| Wave 3 laid to rest | 95 | Wave 3 laid to rest | 95 |
| Wave 4 laid to rest, +80 gold | 144 | Wave 4 at rest, +80 gold | 123 |
| Wave 4 laid to rest | 95 | Wave 4 laid to rest | 95 |
| Wave 5 laid to rest, +100 gold | 150 | Wave 5 at rest, +100 gold | 129 |
| Wave 5 laid to rest | 95 | Wave 5 laid to rest | 95 |
| Wave 6 laid to rest, +100 gold | 150 | Wave 6 at rest, +100 gold | 129 |
| Wave 6 laid to rest | 95 | Wave 6 laid to rest | 95 |
| Wave 7 laid to rest | 95 | Wave 7 laid to rest | 95 |
| The dead rise at the chapel | 141 | Dead rise: the chapel | 107 |
| The dead rise at the chapel and the ford | 210 | Dead rise: the chapel / Dead rise: the ford | 107 (widest of 2) |
| The dead rise at the chapel, the ford and the deep wood | 288 | Dead rise: the chapel / Dead rise: the ford / Dead rise: the deep wood | 126 (widest of 3) |
| The dead rise at the chapel, the ford and the track | 263 | Dead rise: the chapel / Dead rise: the ford / Dead rise: the track | 107 (widest of 3) |
| The dead rise at the chapel and the deep wood | 239 | Dead rise: the chapel / Dead rise: the deep wood | 126 (widest of 2) |
| The dead rise at the chapel, the deep wood and the ford | 288 | Dead rise: the chapel / Dead rise: the deep wood / Dead rise: the ford | 126 (widest of 3) |
| The dead rise at the chapel, the deep wood and the track | 292 | Dead rise: the chapel / Dead rise: the deep wood / Dead rise: the track | 126 (widest of 3) |
| The dead rise at the chapel and the track | 214 | Dead rise: the chapel / Dead rise: the track | 107 (widest of 2) |
| The dead rise at the chapel, the track and the ford | 263 | Dead rise: the chapel / Dead rise: the track / Dead rise: the ford | 107 (widest of 3) |
| The dead rise at the chapel, the track and the deep wood | 292 | Dead rise: the chapel / Dead rise: the track / Dead rise: the deep wood | 126 (widest of 3) |
| The dead rise at the ford | 131 | Dead rise: the ford | 97 |
| The dead rise at the ford and the chapel | 210 | Dead rise: the ford / Dead rise: the chapel | 107 (widest of 2) |
| The dead rise at the ford, the chapel and the deep wood | 288 | Dead rise: the ford / Dead rise: the chapel / Dead rise: the deep wood | 126 (widest of 3) |
| The dead rise at the ford, the chapel and the track | 263 | Dead rise: the ford / Dead rise: the chapel / Dead rise: the track | 107 (widest of 3) |
| The dead rise at the ford and the deep wood | 229 | Dead rise: the ford / Dead rise: the deep wood | 126 (widest of 2) |
| The dead rise at the ford, the deep wood and the chapel | 288 | Dead rise: the ford / Dead rise: the deep wood / Dead rise: the chapel | 126 (widest of 3) |
| The dead rise at the ford, the deep wood and the track | 282 | Dead rise: the ford / Dead rise: the deep wood / Dead rise: the track | 126 (widest of 3) |
| The dead rise at the ford and the track | 204 | Dead rise: the ford / Dead rise: the track | 101 (widest of 2) |
| The dead rise at the ford, the track and the chapel | 263 | Dead rise: the ford / Dead rise: the track / Dead rise: the chapel | 107 (widest of 3) |
| The dead rise at the ford, the track and the deep wood | 282 | Dead rise: the ford / Dead rise: the track / Dead rise: the deep wood | 126 (widest of 3) |
| The dead rise at the deep wood | 160 | Dead rise: the deep wood | 126 |
| The dead rise at the deep wood and the chapel | 239 | Dead rise: the deep wood / Dead rise: the chapel | 126 (widest of 2) |
| The dead rise at the deep wood, the chapel and the ford | 288 | Dead rise: the deep wood / Dead rise: the chapel / Dead rise: the ford | 126 (widest of 3) |
| The dead rise at the deep wood, the chapel and the track | 292 | Dead rise: the deep wood / Dead rise: the chapel / Dead rise: the track | 126 (widest of 3) |
| The dead rise at the deep wood and the ford | 229 | Dead rise: the deep wood / Dead rise: the ford | 126 (widest of 2) |
| The dead rise at the deep wood, the ford and the chapel | 288 | Dead rise: the deep wood / Dead rise: the ford / Dead rise: the chapel | 126 (widest of 3) |
| The dead rise at the deep wood, the ford and the track | 282 | Dead rise: the deep wood / Dead rise: the ford / Dead rise: the track | 126 (widest of 3) |
| The dead rise at the deep wood and the track | 233 | Dead rise: the deep wood / Dead rise: the track | 126 (widest of 2) |
| The dead rise at the deep wood, the track and the chapel | 292 | Dead rise: the deep wood / Dead rise: the track / Dead rise: the chapel | 126 (widest of 3) |
| The dead rise at the deep wood, the track and the ford | 282 | Dead rise: the deep wood / Dead rise: the track / Dead rise: the ford | 126 (widest of 3) |
| The dead rise at the track | 135 | Dead rise: the track | 101 |
| The dead rise at the track and the chapel | 214 | Dead rise: the track / Dead rise: the chapel | 107 (widest of 2) |
| The dead rise at the track, the chapel and the ford | 263 | Dead rise: the track / Dead rise: the chapel / Dead rise: the ford | 107 (widest of 3) |
| The dead rise at the track, the chapel and the deep wood | 292 | Dead rise: the track / Dead rise: the chapel / Dead rise: the deep wood | 126 (widest of 3) |
| The dead rise at the track and the ford | 204 | Dead rise: the track / Dead rise: the ford | 101 (widest of 2) |
| The dead rise at the track, the ford and the chapel | 263 | Dead rise: the track / Dead rise: the ford / Dead rise: the chapel | 107 (widest of 3) |
| The dead rise at the track, the ford and the deep wood | 282 | Dead rise: the track / Dead rise: the ford / Dead rise: the deep wood | 126 (widest of 3) |
| The dead rise at the track and the deep wood | 233 | Dead rise: the track / Dead rise: the deep wood | 126 (widest of 2) |
| The dead rise at the track, the deep wood and the chapel | 292 | Dead rise: the track / Dead rise: the deep wood / Dead rise: the chapel | 126 (widest of 3) |
| The dead rise at the track, the deep wood and the ford | 282 | Dead rise: the track / Dead rise: the deep wood / Dead rise: the ford | 126 (widest of 3) |
