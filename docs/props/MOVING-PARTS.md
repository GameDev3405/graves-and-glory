# Moving prop parts

Kort approved moving parts on 2026-11-14 ("Yes, build moving parts"): swaying banners and pennants, a trebuchet arm, a turning spit.
This page is the design. The first example is `great_banner` (its cloth sways in the wind). Code: `PropDef.Motion` (def schema),
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
| `pivot` | the point the part turns about, model px in the prop frame (`art/props/README.md`): the same numbers you read in `model.py` |
| `amplitude`, `bias` | degrees (amplitude 0 < a <= 90; \|bias\| <= 90) |
| `period` | ticks per cycle, 20..2400 (rotate: 40..2400, so a keyframe turns at most a quarter) |
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
  10-tick window it sends on), so 30 banners spread their packets over the ticks.
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
- **Data reload**: a changed def is picked up when the ticker sees a new `Live.def` (it re-finds the part entities).

## 4. Cost

One keyframe = one `EntityTrackerUpdateS2C` packet per moving part per tracking player, carrying translation (12 B), left rotation (16 B)
and start_interpolation: about 45 bytes on the wire with framing.

| scene | packets/s per player | bytes/s |
|---|---|---|
| 1 banner (1 moving part) | 2 | ~90 |
| 30 banners in range | 60 | ~2.7 KB |
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
- Pennants on a pole: the flag is its own part, pivot on the pole axis, `axis: y`, `sway` of 10-20 degrees.

## 6. Later: trebuchet arm and spit

- **Spit (`rotate`).** The spit and its roast are one part turning about the axle. Put the part's entity on the axle with the def
  `offset` (model the spit in the real frame; the offset is subtracted when it is stored, as for split props), so the pivot is the entity
  origin and the client's slerp alone turns it with no translation drift. Period about 80-120 ticks; a keyframe turns at most a quarter.
- **Trebuchet arm (`swing`, idle).** The arm (and its sling, if separate) is a part pivoting on the axle, `axis` across the frame,
  `swing` with a small amplitude for an idle creak, or `bias` to park it cocked. A big prop's arm crosses split regions; give the arm its
  own part(s) with the same motion entry per part (each turns about the same pivot, so they stay joined). A one-shot throw (fast release,
  slow settle) is a later kind (`throw` with keyframed angles) and needs a trigger from gameplay, which is Kort's call.
- Sounds stay vanilla (a `minecraft:block.wood.*` creak as an ordinary S5 effect on the same prop).
