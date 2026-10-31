# Map Forge (task 2A.3)

Offline build plans for Graves and Glory maps. Python stdlib only. It never touches a Minecraft save, the builder
server or `tools/builder-server`: it works on mapforge world files (`gg-mapforge-world/1` JSON), and refuses a path
inside a folder holding `level.dat`, `builder-server` or `GGBuild`. The in-game `mapforge` command surface is 2A.2.

```
python tools/mapforge validate PLAN
python tools/mapforge dry-run  PLAN --world WORLD.json [--protect PROTECT.json] [--json]
python tools/mapforge apply    PLAN --world WORLD.json [--protect PROTECT.json] [--undo UNDO.json]
python tools/mapforge rollback UNDO.json --world WORLD.json
python tools/mapforge flat     --out WORLD.json --min X Z --max X Z --y Y [--depth N] [--block B]
python tools/mapforge import   PIECE.nbt --out WORLD.json [--origin X Y Z]
python tools/mapforge export   --world WORLD.json --min X Y Z --max X Y Z --out OUT.nbt [--skip-air]
python tools/mapforge info     PIECE.nbt [--raw]
```

Exit codes: 0 ok, 1 invalid plan, 2 refused (locks, limits, base mismatch, rollback conflict).

## Plan (`gg-mapforge-plan/1`)

```json
{
 "format": "gg-mapforge-plan/1",
 "id": "camp-watchpost-sample",          // [A-Za-z0-9_-]; names the default undo file
 "world": "camp",                         // builder world it is meant for
 "author": "...", "purpose": "...",
 "expectWorld": "<sha256>",               // optional: apply refuses unless the world digest matches
 "limits": {"maxChangedBlocks": 20000},   // optional, default 65536
 "locks": [ {"id": "...", "mode": "allow" | "deny", "min": [x,y,z], "max": [x,y,z], "reason": "..."} ],
 "operations": [ ... ]
}
```

Boxes are inclusive block coordinates; x east, y up, z south; y within -64..319; at most 2,000,000 blocks per box.

**Region locks.** At least one `allow` region is required. Every changed block must lie in an allow region and in no
`deny` region. `--protect` adds world-owned deny regions (`gg-mapforge-protect/1`: `{"format", "locks": [{id, min,
max, reason}]}`) for stations, spawns and capture points, which plan 7.3 forbids agents to change. Any violation
refuses the whole plan.

**Operations** run in order; a later one sees the result of an earlier one. `note` is optional on each.

| op | keys | effect |
|---|---|---|
| `set` | `pos`, `block` | one block |
| `fill` | `min`, `max`, `block`, `shape` (`solid` default, `hollow` = shell with air inside, `outline` = shell only), `replace` (list) | box; with `replace`, only cells whose current block matches a name (`minecraft:grass_block`) or an exact state (`oak_log[axis=y]`) |
| `clear` | `min`, `max` | box to air |
| `structure` | `piece` (`camp/archive`, from `src/main/resources/data/gravesandglory/structures`) or `file` (relative to the plan), `at`, `rotation` (0/90/180/270), `includeAir` (default true) | places a structure file the way `Structures.place` does: the centre block (middle column of the footprint, ground layer) lands on `at`, the piece turns clockwise about it, air clears, structure void and missing cells keep the world; block entities are copied, entities are not placed |
| `terrain` | `origin` [x,z], `base`, `floor`, `heights`, `surface`, `under`, `underDepth` (3), `core`, `clearTo` | heightmap stamp: `heights[row z][col x]` is an offset from `base` (null skips the column). Each column is filled from `floor` to `base+h`: `surface` on top, `under` for the next `underDepth` blocks, `core` below; air from the top up to `clearTo` |

Block states: `name[prop=value,...]`; a missing namespace means `minecraft:`; properties are sorted on read.
Rotation turns `facing`, `axis`, `rotation` (signs, banners), rail `shape` and the north/east/south/west side
properties; other properties are kept.

## Dry-run, apply, rollback

Dry-run compiles the plan against the world without changing it and prints: world digest before and predicted after,
changed-block count and box, per-operation counts, blocks placed and removed by name, and lock results (`--json` for
the full record). Only cells whose final value differs from the world count as changes.

Apply refuses on any violation, writes the undo record first (`gg-mapforge-undo/1`: plan id and sha256, author,
world digests before and after, UTC time, and every changed cell with its before and after value), never overwrites
an existing undo file, then replaces the world file atomically.

Rollback restores the before values of the undo record. It refuses, changing nothing, if any of those cells no longer
holds what the plan wrote; edits elsewhere are kept.

## Files

- `nbt.py` NBT reader/writer (byte-identical round-trip of every shipped structure file; gzip with mtime 0).
- `structure.py` structure template files, block states, rotation and the `Structures.java` placement maths.
- `world.py` offline world (sparse cells, sha256 digest, JSON file, import from and export to structure files).
- `plan.py` validation, compilation, locks, summary, apply, rollback. `cli.py` commands.
- `samples/camp-watchpost.json` sample batch on a flat 64x64 world; `samples/camp-protect.json` extra deny regions.
- Tests: `test-tools/standalone/test_mapforge.py` (run by `tools/verify.py fast`).
