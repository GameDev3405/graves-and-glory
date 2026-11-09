# Prop anchor spike (plan task S1), 2026-11-08

Result: **GO with the command-block anchor** (the plan's fallback). The jigsaw passes a, b, d and e but WorldEdit's `//rotate` does not turn its
`orientation`, so a rotated WorldEdit paste would keep the prop's old facing while its footprint turns. A `minecraft:command_block` with
`facing` = prop facing and `Command` = `gravesandglory:prop/<id>` (`auto:0`) passes everything the jigsaw passes, and WorldEdit turns `facing`.

Throwaway code: branch `prop-anchor-spike` (`SpikeAnchor`: `/ggspike place|jig`, a chunk-load logger). Run root: `prop-kit-spike/work/props/spike/`
(local build-mode server, `-Dgg.build=all`, port 25601, RCON 25611; a vanilla 1.20.1 client launched from the loom cache, no Fabric).
The test piece `test/propspike` (5x3x5, in a world data pack) holds a jigsaw `orientation=north_up` named `gravesandglory:prop/forge` at (1,0,1)
and a command block `facing=north`, `Command` `gravesandglory:prop/anvil` at (3,0,3), under stone bricks.

| Check | Jigsaw | Command block | Evidence |
|---|---|---|---|
| a. `Structures.place` at 0/90/180/270 | pass: name kept; `north_up`, `east_up`, `south_up`, `west_up` at the turned cell | pass: `facing` north, east, south, west | RCON `/ggspike jig` output below |
| b. mapforge `import`/`export` round trip and a runs `capture` | pass: `name` kept | pass: `Command` kept | `work/props/spike/b-offline.txt` |
| b. `MapAdoption` apply | by code: `MapRuns.Apply` sets the state, then `readNbt` on the new block entity; S8 adds the test | same | `MapRuns.java:245` |
| c. WorldEdit `//copy //rotate 90 //paste` | **fail by code**: WorldEdit 7.2.15 `BlockTransformExtent` turns `DirectionalProperty` values and the named enum properties `axis`, `half`, `shape`, `type`, `rotation` only; jigsaw `orientation` is not among them. NBT is copied. | pass by code: `facing` is a `DirectionalProperty` | `javap` of the WorldEdit jar. Not run in game: WorldEdit ignores its commands sent through `/execute`, and typing into a client needs focus on Kort's desktop |
| c. Axiom copy/paste with rotate | not tested (Axiom not installed locally; its download waits on Kort) | not tested | I3 acceptance re-checks on the builder server |
| d. `CHUNK_LOAD` sees it in `chunk.getBlockEntities()` after a restart | pass | pass | server log `[spike] chunk-load` lines |
| e. vanilla 1.20.1 client joins the build-mode server with the current jar | pass: joined, only "Unknown custom payload" warnings (`fabric:registry/sync/direct`, `minecraft:register`); sees item_display | (same server) | `work/props/vanilla/client-VanillaTest.log` |

a. output (centre 200/210/220/230, 100, 200 at 0/90/180/270):
```
199,100,199 jigsaw[orientation=north_up] | 201,100,201 command_block[facing=north]
211,100,199 jigsaw[orientation=east_up]  | 209,100,201 command_block[facing=east]
221,100,201 jigsaw[orientation=south_up] | 219,100,199 command_block[facing=south]
229,100,201 jigsaw[orientation=west_up]  | 231,100,199 command_block[facing=west]
```

## f. Calibration

`DisplayEntityRenderer` turns an item display 180 degrees about Y and the item renderer centres the model (`translate(-0.5)`). So for a model
whose (0,0,0) is the min corner of the anchor cell and whose front faces north (the art track's part frame, 2026-11-08):

- entity position = anchor cell centre `(x+0.5, y+0.5, z+0.5)`,
- entity yaw = `180 + facing` (facing 0, 90, 180, 270 clockwise seen from above), pitch 0,
- `transformation.translation` = 0, scale 1, `item_display` `fixed` (each part's own `display.fixed` carries any shift, e.g. the forge hood's +32 px).

16 model units render as one block. Checked in the vanilla client: `docs/reports/prop-anchor-spike-calibration.png` (top view, north up):
left, facing 0 fills the anchor cell, red front on the north edge, black post at the north-west corner, the 16..32 slab runs east; right,
facing 90 turns about the anchor cell centre, front east, the slab runs south.

## Consequences for the plan

- S3: `PropGeometry.facingOf`/`orientation` map `north/east/south/west` (the command block `facing`) instead of `<dir>_up`.
- S4: the anchor is `command_block[facing=<dir>,conditional=false]` with `Command:"gravesandglory:prop/<id>"`, `auto:0b`, one block under the
  anchor cell. It never runs: the text is not a valid command, it is unpowered, and game servers run with command blocks disabled.
- S8: mapforge `rotate_state` must turn `facing` on command blocks (check it does) rather than jigsaw `orientation`.
- e passing means acceptance item 2 is not blocked by registry sync on the builder server (no G&G entities or items exist there).
