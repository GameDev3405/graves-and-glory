# Map Forge (task 2A.3)

Offline build plans for Graves and Glory maps. Python stdlib only, except `render`, `apply --render` and `colours`,
which import Pillow when they run and say so clearly if it is missing (`python -m pip install pillow`). It never touches a Minecraft save, the builder
server or `tools/builder-server`: it works on mapforge world files (`gg-mapforge-world/1` JSON), and refuses a path
inside a folder holding `level.dat`, `builder-server` or `GGBuild`. The in-game `mapforge` command surface is 2A.2.

```
python tools/mapforge validate PLAN
python tools/mapforge dry-run  PLAN --world WORLD.json [--map MAP.json] [--json]
python tools/mapforge apply    PLAN --world WORLD.json [--map MAP.json] [--undo UNDO.json] [--render]
python tools/mapforge status   MAP.json [--world WORLD.json] [--digests]
python tools/mapforge sign     MAP.json SECTION --pass P --world WORLD.json --by NAME [--verdict pass|changes]
                               [--notes TEXT] [--image PNG]... [--plan PLAN]...
python tools/mapforge sign     MAP.json --whole --world WORLD.json --by Kort
python tools/mapforge render-section MAP.json SECTION --world WORLD.json --out-dir DIR [--margin N] [--label T]
                               [--diff BEFORE.json | --undo UNDO.json]
python tools/mapforge rollback UNDO.json --world WORLD.json
python tools/mapforge flat     --out WORLD.json --min X Z --max X Z --y Y [--depth N] [--block B]
python tools/mapforge import   PIECE.nbt --out WORLD.json [--origin X Y Z]
python tools/mapforge export   --world WORLD.json --min X Y Z --max X Y Z --out OUT.nbt [--skip-air]
python tools/mapforge info     PIECE.nbt [--raw]
python tools/mapforge render   WORLD.json --out OUT.png [--view top|iso] [--min X [Y] Z] [--max X [Y] Z] [--scale N]
                               [--no-grid] [--diff BEFORE.json | --undo UNDO.json] [--title T] [--report-unknown]
python tools/mapforge gaps     WORLD.json [--min X Z] [--max X Z] [--limit N]
python tools/mapforge colours  --jar minecraft-client.jar
```

Exit codes: 0 ok, 1 invalid plan, 2 refused (locks, limits, pass order, section mismatch, rollback conflict, bad
manifest or sign-off).

## Surface-only maps (Kort, 2026-10-31)

Kort's ruling: "We only need the top layer for maps, blocks aren't breakable so what's underneath doesn't matter, just
make sure the top layer looks good and go from there." A map plan builds only what can be seen: the top layer plus a
shallow fill wherever a gap, a cliff face or a cut bank would show. No solid interiors.

- A column is its top block and a few blocks under it (Ironhold: 4 in all). Where a lower neighbour shows a cliff
  face, the column reaches down to that neighbour's top. The `terrain` op takes an optional `floors` grid (same rows
  and columns as `heights`; an integer y per column, null to use `floor`) so each column can start at its own height.
- The `seal` helper runs after the water and roads have reshaped the ground and closes the gaps the shaping opened.
- `gaps WORLD` is the check: it lists every air cell a column shows from outside (exit code 1 if any).
- Walls, towers and structures are not hollowed by this rule: they are what the player sees. Keep fills tight: start a wall
  `floor` at the ground it stands on, not at the world floor.
- Per-plan cap: `limits.maxChangedBlocks` 150000 for a 64 x 64 section plan (surface-only plans run at about 20000;
  the old solid plans reached 105000 in a section with a tall rim).
- Sections already built solid are not rebuilt just to hollow them; the rule applies to new and reopened plans.

## Plan (`gg-mapforge-plan/1`)

```json
{
 "format": "gg-mapforge-plan/1",
 "id": "camp-watchpost-sample",          // [A-Za-z0-9_-]; names the default undo file
 "world": "camp",                         // builder world it is meant for
 "author": "...", "purpose": "...",
 "section": "E3", "pass": "blockout",     // section of the map manifest and the pass this plan does
 "expectSection": "<sha256>",             // optional: refused unless the section digest still matches
 "reopen": "why",                         // optional: allows redoing an earlier pass of the section
 "limits": {"maxChangedBlocks": 20000},   // optional, default 65536
 "locks": [ {"id": "...", "mode": "allow" | "deny", "min": [x,y,z], "max": [x,y,z], "reason": "..."} ],
 "operations": [ ... ]
}
```

Boxes are inclusive block coordinates; x east, y up, z south; y within -64..319; at most 2,000,000 blocks per box.

**Region locks.** Every changed block must lie in an allow region and in no `deny` region; any violation refuses the
whole plan. A plan that names a `section` needs `--map`: its allow region is the section's bounds, cut down to the
plan's own `allow` locks when it has any (so `locks` may hold only denies, or be left out). A plan without a section
needs at least one allow lock of its own. The manifest's `protect` locks are added as denies to every plan run with
`--map`. `expectWorld` (a whole-world digest) is gone: give `expectSection` instead, so edits in other sections do not
invalidate a plan. `status MAP --world W --digests` prints the section digests.

**Pass order.** Each section goes through `blockout`, `structure`, `natural`, `detail`, then `done`. A plan's `pass`
must be the section's next one (status `todo` -> `blockout`, `blockout` -> `structure`, ...); `reopen` with a reason
lets a plan redo an earlier pass. The status only moves when an agent records a passing review with `sign`.

## Map manifest (`gg-mapforge-map/1`)

One file per map, owned by the map lane (for example `docs/maps/ironhold/manifest.json`):

```json
{
 "format": "gg-mapforge-map/1",
 "id": "ironhold", "name": "...",           // id equals plan.world
 "palette": "ironhold",                     // palette reference
 "bounds": {"min": [x,y,z], "max": [x,y,z]},
 "layout": {...},                           // optional: signed layout doc, who signed, picks
 "sections": [ {"id": "E3", "name": "...", "bounds": {"min": [...], "max": [...]},
                "status": "todo" | "blockout" | "structure" | "natural" | "detail" | "done",
                "reviews": [ {"pass", "verdict": "pass" | "changes", "by", "at", "digest", "plans", "images", "notes"} ]} ],
 "protect": [ {"id", "kind": "station" | "spawn" | "capture" | "siege" | "other", "reason", "source",
               "min": [...], "max": [...]}                                     // a box, or
              {"id", "kind", "reason", "center": [x,z], "radius": r, "y": [y0,y1]} ],  // an upright cylinder
 "signed": {"by", "at", "world"}            // written by sign --whole
}
```

Sections lie inside the map bounds and must not overlap. Protect locks replace the old `--protect` file.

`sign` records one agent review of a section pass: the section digest of the world, the plans and images reviewed and
notes. `--verdict pass` (default) moves the status to that pass; `changes` records the review and keeps the status. A
pass can only be reviewed once the previous one passed; the current pass can be reviewed again after a `reopen` plan reworked it. `status` lists every touched section with its last review and,
with `--world`, flags **drift**: the section changed since its last passing review. `sign --whole` is Kort's sign-off
of the finished map and needs every section `done`. Agents review the passes; Kort signs only the layout and the whole
map. `render-section` writes `<map>-<section>-top.png` and `-iso.png` of the section plus a margin (default 4 blocks),
or a diff with `--diff`/`--undo`.

**Operations** run in order; a later one sees the result of an earlier one. `note` is optional on each.

| op | keys | effect |
|---|---|---|
| `set` | `pos`, `block` | one block |
| `fill` | `min`, `max`, `block`, `shape` (`solid` default, `hollow` = shell with air inside, `outline` = shell only), `replace` (list) | box; with `replace`, only cells whose current block matches a name (`minecraft:grass_block`) or an exact state (`oak_log[axis=y]`) |
| `clear` | `min`, `max` | box to air |
| `structure` | `piece` (`camp/archive`, from `src/main/resources/data/gravesandglory/structures`) or `file` (relative to the plan), `at`, `rotation` (0/90/180/270), `includeAir` (default true) | places a structure file the way `Structures.place` does: the centre block (middle column of the footprint, ground layer) lands on `at`, the piece turns clockwise about it, air clears, structure void and missing cells keep the world; block entities are copied, entities are not placed |
| `terrain` | `origin` [x,z], `base`, `floor`, `floors` (optional per-column grid), `heights`, `surface`, `under`, `underDepth` (3), `core`, `clearTo` | heightmap stamp: `heights[row z][col x]` is an offset from `base` (null skips the column). Each column is filled from `floor` to `base+h`: `surface` on top, `under` for the next `underDepth` blocks, `core` below; air from the top up to `clearTo` |

**Terrain helpers** (`terrain.py`) are operations too, so locks, protect locks, limits, dry-run counts, apply and
rollback treat them exactly like `fill`: a helper that would touch a denied or protected cell refuses the whole plan.
They read the overlay, so they see earlier operations. All are deterministic for a given `seed` (int or string,
default 0). A column's ground top is its highest block in `scan` (default [-64, 160]) that is not air or water;
raising a column repeats the block under its old top and keeps its surface block on top, digging keeps the surface.
Common keys: `min`/`max` [x, z] clip box (required for noise, erode, breakup), `exclude` [{min, max}] boxes left
alone, `only` surface names the op may change. A block spec is a state or a weighted mix `[[state, weight], ...]`.

| op | keys | effect |
|---|---|---|
| `noise` | `amplitude`, `scale`, `octaves` (3), `persistence` (0.5), `offset`, `falloff`, `clamp` [y0,y1] | layered value noise in [-1, 1] times amplitude added to each column top; `falloff` fades it to 0 over that many blocks from the clip edge; tops clamped |
| `erode` | `mode` (`thermal` / `smooth`), `iterations` (4), `talus` (1.0), `rate` (0.5), `jitter`, `clamp` | thermal: material slides to lower neighbours (8-way) where the drop is steeper than talus blocks per block, talus jittered per column so slopes do not stair-step evenly; smooth: relax toward the neighbour mean. Columns outside the clip are fixed neighbours |
| `breakup` | `replace`, `mix`, `density` (0.5), `scale` (3, clump size; 1 = per block), `near` + `width`, `depth` (1), `exposed` | swaps surface blocks named in `replace` for the mix in clumps; with `near`, only within `width` of those blocks (water counts under its surface), fading with distance; `depth` reaches down the column, `exposed` keeps to blocks with air or water beside them (cliff faces) |
| `wall` | `points` [[x,z(,top)]...], `thickness` (3), `top`, `floor`, `block`, `closed`, `merlons` | columns within thickness/2 of the polyline filled from floor to top (or each point's top, interpolated); `merlons` adds every other block on the edges |
| `path` | `points` [[x,z(,y)]...], `width`, `falloff`, `y`, `surface`, `under`, `edge`, `edgeDensity` (0.6), `wobble`, `wobbleScale` (8) | road or causeway: core within width/2 surfaced and, with `y`, graded to it; the falloff band eases back to the ground (an embankment above it, a cutting below) and is speckled with `edge`; `wobble` makes the edges ragged |
| `river` | `points` [[x,z(,width)]...], `width`, `depth` (2), `water`, `floor`, `bank` (0), `bankSurface`, `bed` (gravel), `faces`, `wobble`, `wobbleScale` | channel: bed `depth` below `water` at the centre line and 1 at the edge, water to `water`, air above; the next `bank` blocks slope down to the water (0 = a cut cliff, banks are only lowered); `faces` lines the exposed cliff and bank cells with `bankSurface` |
| `seal` | `min`, `max` [x, z], `under` | surface-only maps: for each column whose ground top is above a neighbour's, fills the air cells between the lowest neighbour top and its own top with `under` (default the block under its top). Neighbours without ground are ignored; water is not ground, so a bank beside a river is sealed down to the bed |

Dry-run prints, per operation, the changed blocks and the columns they lie in.

Block states: `name[prop=value,...]`; a missing namespace means `minecraft:`; properties are sorted on read.
Rotation turns `facing`, `axis`, `rotation` (signs, banners), rail `shape` and the north/east/south/west side
properties; other properties are kept.

## Dry-run, apply, rollback

Dry-run compiles the plan against the world without changing it and prints: world digest before and predicted after,
changed-block count and box, per-operation counts, blocks placed and removed by name, and lock results (`--json` for
the full record). Only cells whose final value differs from the world count as changes.

Apply refuses on any violation, writes the undo record first (`gg-mapforge-undo/1`: plan id and sha256, author,
world digests before and after, UTC time, for a sectioned plan the section, pass and section digests before and after,
and every changed cell with its before and after value), never overwrites
an existing undo file, then replaces the world file atomically.

Rollback restores the before values of the undo record. It refuses, changing nothing, if any of those cells no longer
holds what the plan wrote; edits elsewhere are kept.

## Render (review images)

`render` draws one PNG of a world, with a caption naming the box, the view and the scale.

- `--view top` (default): north up, east right, one square per column in the colour of its top block. Shading:
  3.5% brighter or darker per block above or below the median height, plus slope light from the north-west, so
  mounds and pits read. A post, plant, lantern or banner on top is drawn inset over the ground below it; a hairline
  marks where the surface block changes; empty columns show a dark checker. A 16-block grid with coordinate labels
  is on unless `--no-grid`.
- `--view iso`: 2:1 isometric from the south-east (north-west at the back). Top, south and east faces at 100, 80
  and 62% light; slabs, carpets, snow layers, fences, plants, torches and similar use their model's shape.
- `--min/--max X Z` crops columns (all heights); `X Y Z` also crops height, for a section box. The default box is
  the world's bounds. `--scale` is pixels per block (top, default fits about 770 px) or per half tile (iso, default
  fits about 1400 px wide).
- Diff: `--diff BEFORE.json` compares WORLD (after) with an earlier world; `--undo UNDO.json` rebuilds the before and
  after of an undo record on WORLD (works whether WORLD is applied or rolled back). Unchanged blocks turn pale grey.
  Top-down colours each changed column by what happened to its surface: green rose (added), red fell (removed),
  amber same height with different blocks (changed). Isometric tints each changed block and draws removed blocks as
  red outlines. The legend counts columns (top) or blocks (iso).
- `apply --render` checks Pillow before changing anything, then writes `<undo>.before-top.png`, `after-top`,
  `diff-top`, `before-iso`, `after-iso` and `diff-iso` beside the undo record, cropped to the changed area plus 8.
- Input and output paths get the same live-folder guard as the other commands; the output must be a `.png`.

**Colours.** `colours.json` holds a top colour, a side colour and a model box for all 999 Minecraft 1.20.1 blocks,
generated by `colours` from the client jar Loom caches (`~/.gradle/caches/fabric-loom/1.20.1/minecraft-client.jar`):
each block's first model is resolved through its parents and the textures on its up and south/east faces are
averaged by alpha, grass and leaves tinted with the plains colours, water with the default water colour. Only the
averages are stored. Logs on their side swap bark and rings; slab and trapdoor halves, double slabs and snow layers
follow the state. A block not in the table (a modded block, a newer one) is magenta, and `--report-unknown` names
every such block in the box with its count. The tests check that the table covers every block in the shipped
structure files.

## Map adoption (tasks 2A.2, 2A.4 and 2A.5; `gg-mapforge-runs/1`)

Kort, 2026-10-28: a map ships as its generated base plus an edit overlay. The format is `runs.py` (Python) and
`MapRuns.java` (game): one gzip text file (mtime 0, so the same blocks give the same bytes), a sorted-key JSON header,
a palette in first-use order, maximal runs per column (`C x z y n p...`, columns sorted by x then z) and base64
block-entity NBT. `digest` is sha256 over every run, `body` the sha256 of the text after the header; a file whose
body, cell count or digest does not match is refused. Kinds: `capture` (every cell of a box, air included),
`overlay` (the cells that differ from a base capture, with `baseDigest` over the base's cells under it), `batch`
(an agent's proposed change, with `locks.allow` and `locks.deny`) and `undo`.

The game side, in test-tools and the unshipped `mapforge-builder-DO-NOT-SHIP.jar` only (`MapForge.java`, run on the
builder or with `-PmapForge=SCRIPT`, which hosts the harness world and drives a script, `MapForgeHarness.java`):

```
/mapforge status | backup | protects OUT | walk [TAG]
/mapforge export NAME x0 y0 z0 x1 y1 z1 [camp]   capture a box (map or camp stamp)
/mapforge diff BASE FINAL OUT                    overlay; refuses a changed generator stamp
/mapforge verify FILE                            count the cells the world holds differently
/mapforge plan BATCH                             dry run: locks, capture rings, spawn columns, camp stations, deletion
/mapforge apply BATCH                            only after a passing plan of the same bytes; backup first, writes BATCH.undo.runs.gz
/mapforge undo FILE                              only when the world still matches what apply left; backup first
/mapforge adopt OVERLAY | snapshot CAPTURE       timed adoption and full-snapshot writes (the 2A.4 spike)
```

Offline: `python tools/mapforge adopt batch|validate|info|diff|apply|bake`. `bake --target dev` copies a validated
overlay (map, stamp and baseDigest checked against its base capture) to
`test-tools/resources/data/gravesandglory/adoption-dev/`, read only with `-PadoptionDev`; `--target release` writes
`src/main/resources/data/gravesandglory/adoption/` and refuses without `--approved-by` (Kort's approval). In game,
`MapAdoption.java` writes a map's overlay after its landmarks, a budget of cells per tick, and keeps the generated
map when the format, kind, map, generator stamp (`MapAdoption.stamp`: `GENERATOR` plus the Spec tables the
generator reads) or base digest does not match. Bump `GENERATOR` whenever the generator's blocks change.

## Files

- `nbt.py` NBT reader/writer (byte-identical round-trip of every shipped structure file; gzip with mtime 0).
- `structure.py` structure template files, block states, rotation and the `Structures.java` placement maths.
- `world.py` offline world (sparse cells, sha256 digest, JSON file, import from and export to structure files).
- `plan.py` validation, compilation, locks, summary, apply, rollback. `terrain.py` the terrain helper operations. `manifest.py` map manifests: sections, pass
  order, protect locks, reviews, status. `cli.py` commands.
- `gaps.py` the surface-only gap check. `render.py` top-down, isometric, diff and contact-sheet images (Pillow). `colours.py` and `colours.json` the block
  colour table and its generator.
- `samples/camp-map.json` sample manifest (two sections, protect boxes and a cylinder); `samples/camp-watchpost.json`
  a blockout plan for its section s0 on a flat 64x64 world.
- `runs.py` and `adopt.py` map adoption files and commands.
- Tests: `test-tools/standalone/test_mapforge.py` and `test_adoption.py` (run by `tools/verify.py fast`).
