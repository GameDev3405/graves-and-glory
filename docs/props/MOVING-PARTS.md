# Moving prop parts

Kort approved moving parts on 2026-11-14 ("Yes, build moving parts"): swaying banners and pennants, a trebuchet arm, a turning spit.
This page is the design. Moving now: `great_banner` (cloth sway), pennants on `spear_standard`, `tent_pavilion` and
`tent_pavilion_large` (+ `_dawn`, `_dusk`, `_weathered`), the `trebuchet` arm creak with its hanging box, and the `cook_spit` roast (rotate). Code: `PropDef.Motion` (def schema),
`PropMotion` (pure pose maths), `PropTicker.motion` (keyframes), `mixin/PropRestSaveMixin` (saves at rest). Checks: `PropMotionCheck`
(standalone) and `p1-prop-motion` (combat game suite).

## 1. Declaring a moving part

A def gets an optional `motion` list. A def without it behaves exactly as before.

```json
"motion": [
  {"part": "great_banner_cloth", "kind": "sway", "axis": "x", "pivot": [8, 80, 6], "amplitude": 3, "bias": 4, "period": 96, "phase": 0},
  {"part": "great_banner_cloth", "kind": "sway", "axis": "z", "pivot": [8, 80, 5], "amplitude": 2, "period": 64, "phase": 0.25}
]
```

| field | meaning |
|---|---|
| `part` | a def part's `model`; the whole part turns as one rigid piece |
| `kind` | `sway`: `bias + amplitude*sin(2πu)`. `swing`: the same range, eased so it rests at both ends (an idle arm). `rotate`: `bias + 360u`, a full turn per period (a spit) |
| `axis` | `x`, `y`, `z` or `-x`, `-y`, `-z`, in the prop frame at facing 0 (x east, y up, z south). Positive turns are right-handed |
| `pivot` | the point the part turns about, model px in the prop frame (`art/props/README.md`): the same numbers you read in `model.py`. Finite, and at most 32 px outside the prop's cells |
| `amplitude`, `bias` | degrees (amplitude 0 < a <= 90; \|bias\| <= 90) |
| `period` | whole ticks per cycle, at most 2400. Sway and swing: at least 60 (6 keyframes; the per-anchor scale can shorten it to 0.92x), and `2*pi*amplitude*10/(0.92*period)` at most 45 degrees a keyframe. Rotate: at least 40 (a quarter turn a keyframe) with the pivot on the part's entity origin `[8+ox, 8+oy, 8+oz]`, else at least 120 (30 degrees a keyframe), because the client moves the translation in a straight line while it turns |
| `phase` | 0..1, a share of the period; added to the per-anchor phase |

Several entries on one part compose (first entry outermost). The banner uses two: a forward billow about the crossbar (biased so the cloth
never swings back into the pole) and a slower in-plane drift. Two incommensurate periods make the cloth wander instead of ticking back and
forth, which is the cheap stand-in for a ripple (no extra packets).

## 2. How it plays

Item displays interpolate on the client. Every `PropMotion.KEY` = 10 ticks the server sets the part's `transformation` (translation +
left rotation) to the pose due 10 ticks later, with `interpolation_duration = 10` and `start_interpolation = 0`; the client tweens to it
(lerped translation, slerped rotation) and arrives as the next keyframe lands. So the server sends one entity data update per moving part
twice a second and the client draws smooth motion at its frame rate.

- **Only near players.** The S5 ticker (every 5 ticks) already walks the live props and measures the nearest player. A prop with motion
  plays while a player of that world is within `PropMotion.RANGE` = 48 blocks (dressing is seen from 15-40 m; effects keep their 24).
  When the last player leaves, the parts get the rest pose once (eased over 10 ticks) and the prop is dropped from the ticker's motion
  map: paused, no more packets.
- **Desync.** From the anchor's `PropEffects.seed` each entry gets a phase share and a period scale of 0.92..1.08, so a row of banners
  never sways in step and drifts further apart over time. The seed also picks the prop's keyframe slot (which of the two ticker runs in a
  10-tick window it sends on), so 30 banners spread their packets over the ticks. Entries that are identical apart from `part`
  share the desync of the first such entry (`PropMotion.jitter`), so the split pieces of one moving piece (the trebuchet arm's six
  parts) stay joined; different entries (the banner's two) still get their own.
- **Frames.** A part is an item_display at the anchor cell centre (plus its def `offset`) with yaw 180+facing, and the item renderer turns
  the model a half turn, so model point P sits at entity point `((8-P.x)/16, (P.y-8)/16, (8-P.z)/16)` (P minus the part offset) at every
  facing. `PropMotion.pose` turns about the mapped axis (x and z flip) through the mapped pivot: `translation = R(Q) - q*R(Q)`. One frame
  for all facings, because the yaw carries the facing.

## 3. Remove, scan, saving, templates

- **Rest pose is the identity transformation.** Every prop part is spawned with none (`Props.spawnPart`), so "at rest" needs no stored data.
- **Saves are always at rest.** `PropRestSaveMixin` writes the identity `transformation` for any display entity tagged `gg_prop`. A chunk
  save mid-sway, a builder-world structure save, a mapforge export or a `/data get` copy never keeps a tilted cloth. For a part that never
  moves this writes exactly what vanilla wrote. On load the part is at rest and the ticker picks it up again when a player comes near.
- **Remove** discards the parts as before and also forgets the ticker's motion state (`PropTicker.forget`); a chunk unload forgets its
  props' state too. `ensureParts` and the scanner are unchanged: they match parts by CMD, position and yaw, never by transformation.
- **Map templates** hold only the anchor; parts are respawned at rest by the scanner, so nothing changes for generators or `ANCHOR-NBT.md`.
- **Data reload**: the ticker reads the current def each run. If a reload changes or drops a prop's motion, its old moving parts are eased
  to rest and its motion state is dropped; the new def's moving parts are found again.

## 4. Cost

One keyframe = one `EntityTrackerUpdateS2C` packet per moving part per tracking player, carrying translation (12 B), left rotation (16 B)
and start_interpolation: about 45 bytes on the wire with framing.

| scene | packets/s per player | bytes/s |
|---|---|---|
| 1 banner (1 moving part) | 2 | ~90 |
| 30 banners in range | 60 | ~2.7 KB |
| 30 spear standards + 10 pavilions (1 pennant part each) | 80 | ~3.6 KB |
| 1 trebuchet (arm 6 parts + box 3 parts) | 18 | ~0.8 KB |
| 1 cook spit (roast) | 2 | ~90 |
| for comparison: S5 effects, per effect prop | 0.02 (anvil) .. 5.7 (black smoke vent), mean 2.6 | particle packets ~40-60 B |

So a swaying banner costs about what one smoking prop costs, and 30 of them about what 23 average effect props cost. Server time: a pose is
a few quaternion products per moving part, and the part entities are cached per anchor (one entity box lookup when the prop starts moving),
so the ticker's `/prop stats` mean stays in microseconds; `/prop stats` now also prints the keyframes sent by the last run. A wider prop
set can halve the cost by raising `KEY` to 20 for slow motion, at the price of a coarser path (sway periods should then be 80+ ticks).

## 5. Pivots and modelling (proplib)

- A moving piece must be **its own part** (its own `item_display`), modelled in the real prop frame at its rest pose, as any part. The
  great banner's cloth already was (`great_banner_cloth`, kept separate for team colours), so its model is unchanged.
- The pivot lives in the def, in the same model px as `model.py`. Pick it on the piece's hinge: the banner cloth hangs from the crossbar
  top (y 80) at its back face (z 6), so it turns about x there.
- Check clearance by hand: a part turning about a hinge must not swing into a static part. The banner billow is biased forward
  (1..7 degrees, hem 1-6 px out) because the pole is behind the cloth.
- A real ripple (a wave running down the cloth) needs the cloth cut into 2-3 horizontal strips, each its own part with its pivot at the
  strip above's hem and a phase lag. That triples the packets; left out until Kort asks for it.
- **Split props: moving groups.** `Prop.split(pid, els, size, moving=[(name, elements, slots)])` packs each moving group into the
  fewest 48 px cells (`pack_group` tries grid offsets) and names its parts `<id>_<slot>` (CMD = block base + slot). A slot must be an
  index no static region uses: past the last region if the block has room, else an index left empty. `p.moving[name]` lists the part
  names; `new_prop.py <id> --sync` rewrites the def parts and keeps `motion`. Each moving part is centred on its entity (item display
  light is sampled there).
- **Small props: `offsets=True`.** `Prop(..., shift={part: (dx,dy,dz)}, offsets=True)` stores a part shifted so its entity sits on a
  chosen point (the spit's axle), writing the def `offset`.
- **Clearance.** Sweep the moving part through its whole range against nearby static elements before committing (a scratch sweep did
  it here); hidden contact inside a pole, drum or wound rope is fine.

## 6. Live examples

- **Pennants** (spear_standard, the pavilions). The cloth is its own part (`spear_standard_pennant`, `tent_pavilion_27`,
  `tent_pavilion_large*_88`), three entries: a y sway on the pole axis (14 deg, 84 ticks), a smaller y sway (6 deg, 66 ticks, phase 0.3)
  and a z sway at the hoist top (2 deg, 74 ticks) with a bias: a 2 deg droop on the spear, a 2 deg lift on the tents (a droop touched
  the tent's gilt collar). The hoist stays on the pole; the fly sweeps a few px.
- **Trebuchet** (`swing`). Arm, pull rope and sling cord are six parts (slots 90-95), each `swing` about x at the axle [8,140,-12],
  1.25 deg, 140 ticks. The counterweight box (slots 96-98) carries the same arm entry plus a `sway` about its pin [8,178.9,-50.9],
  1.2 deg, 76 ticks, so it hangs from the arm and swings a little. The frame never moves.
- **Cook spit** (`rotate`, block 93). Spit, roast, twine, bones and crank are `cook_spit_roast` with offset [8,14,0], so its entity sits
  on the axle (pivot [16,22,8]); a turn every 160 ticks. Posts and fire are static; flames, smoke and crackle are the brazier's effects.

## 7. Rules and later work

- **Rotate pivots.** For a `rotate` part, put the part's entity on the axle with the def `offset`
  (model the spit in the real frame; the offset is subtracted when it is stored, as for split props), so the pivot is the entity
  origin and the client's slerp alone turns it with no translation drift. Period 40 ticks or more (a quarter turn a keyframe) with the pivot on the entity origin; a pivot
  off the origin wobbles (straight-line translation tween) and needs a period of 120 or more.
- **Trebuchet throw.** A one-shot throw (fast release,
  slow settle) is a later kind (`throw` with keyframed angles) and needs a trigger from gameplay, which is Kort's call.
- **Previews.** `tools/prop_shot.py <ids> --frames N[:TICKS[:FOCUS]]` shoots N frames TICKS apart and writes
  `work/props/review/<id>-motion.gif`; FOCUS > 0 aims the camera at the mean motion pivot from FOCUS blocks (small cloth on a big tent).
- Sounds stay vanilla (a `minecraft:block.wood.*` creak as an ordinary S5 effect on the same prop).
