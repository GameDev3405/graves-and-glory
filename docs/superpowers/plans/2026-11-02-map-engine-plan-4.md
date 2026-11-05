# Map Engine Plan 4: the signature building library

**Goal:** Kort (or the AI) places approved buildings from a library onto a design. The editor shows each piece's footprint and door side before and after placing; the checker flags overlaps, blocked doors and footprints off the canvas; the builder stamps the piece's blocks into the tiles on a levelled pad, so the built world, its walk check and the mod's read-back all see the building.

**Spec:** `docs/superpowers/specs/2026-11-01-map-engine-design.md` (signature buildings, locking, AI export format). **Base:** `main` `173a747`, branch `work/map-engine-plan-4`.

## Boundaries

- Python and the browser app only. No Java, gameplay, packet, save or `Spec.java` change.
- The library is the pieces in `src/main/resources/data/gravesandglory/structures/` (the mod's approved pieces). New pieces join only by Kort adding them there; the index is regenerated, never hand-written.
- Tiles carry block states only (the mod's `EngineMaps` reads states), so a stamped piece loses its block-entity data: chests are empty, signs blank, banners plain. A `buildings` list in `map.json` records each placement for a later mod reader that could place the full piece; that reader is a separate change for Kort to approve.
- Tests and a headless build only; no game client.

## Task 1: library index

- `mapengine/library.py` scans every `.nbt`: `id` (`group/name`), `group`, `size`, `footprint` (x, z), `door` (`north`/`east`/`south`/`west`, `open` when the piece has no roofed interior, `none` when its interior cannot be walked into), `doorSpan` (the opening's cells along that side), block and block-entity counts and the file's sha256.
- Door side: a 2D flood over columns whose two layers above the floor are free, from the edge cells of each side; the side whose flood reaches the most roofed cells wins. `library_overrides.json` corrects a guess by hand.
- `python tools/mapengine library` writes `tools/mapengine/library.json`; `--check` fails when it is stale. Test: the committed index matches a fresh scan.

## Task 2: building kind

- `design.KINDS` gains `building`: `piece` (a library id), integer `x`, `z` on the canvas (the piece's centre block, as `Structures.place`), `rotation` 0/90/180/270 (clockwise, as the mod). `by` and `state` as every object: Kort's are locked (Plan 3 rule), AI ones arrive as proposals.

## Task 3: footprint and pad

- `mapengine/buildings.py`: `footprint(o)` (cells, by `mapforge.structure.footprint`), `door_front(o)` (the cells just outside the door opening), `issues(d, field)`.
- `terrain.heightfield` levels each footprint to the ground at its centre and drains water there, last, so the editor preview, the 3D view and the builder agree.

## Task 4: checker

- `check.check` adds design issues: `building_overlap` (two footprints share a cell), `building_bounds` (a footprint leaves the canvas), `door_blocked` (no door-front cell is on the canvas, dry, outside every footprint and within one step of the floor), `building_point` (a point's centre or a spawn lies inside a footprint).

## Task 5: build

- The builder stamps each building, in design order, into the tile arrays: piece states rotated by `mapforge.structure.rotate_state`, layer 0 at the pad's surface, structure void and air left as the terrain. `map.json` gains `buildings`.
- `columns.bin` and the check's tile read use the mod's read-back rule (air, barrier, sea lantern, fences and wool are not ground).
- Tests: a stamped block lands at the rotated cell; same design, same bytes; check reports nothing for a clean placement and flags the three faults.

## Task 6: editor

- Building tool (key 8): the panel picks the piece; the map view draws the footprint and door under the cursor; R rotates; one click places. Placed buildings draw as footprints with a door tick, selectable and draggable; the panel edits piece and rotation. Server `GET /api/library`.

## Task 7: AI handoff

- `BRIEF.md` lists the library and the building schema; `top.png` draws footprints. Import is the generic Plan 3 merge, so buildings follow the locking rule.

## Task 8: docs

- `docs/MAP-ENGINE.md`: the Building tool, the library command and the block-entity limit.
