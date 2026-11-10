# Prop workflow

How a Graves and Glory prop goes from an idea to a placed, reviewed object. Written so a Sonnet worker can run every step
from this page, `art/props/README.md` (frame and look rules) and the tools below. Policy (models, approvals, clients) is
`docs/WORKFLOW.md`; this page never overrides it.

Props serve three uses (Kort, 2026-11-09):

| Use | Seen from | What matters most |
|---|---|---|
| `dressing` (battle maps) | 15-40 m, while fighting | silhouette and colour read at distance; never blocks a lane by accident |
| `lobby` (Old Hold, hub) | 2-6 m, lingering | close-up story; detail that rewards a look |
| `gameplay` (cover, barricades, siege pieces, objectives) | any | the collision is the design; any rule change needs Kort |

## The pipeline at a glance

```
1 brief        python tools/new_prop.py <id> --use ... --size SX SY SZ --name "..."   -> fill art/props/<id>/brief.md
2 model        edit art/props/<id>/model.py (proplib boxes and materials; the script is the source)
3 card         python tools/prop_card.py <id>      -> work/props/cards/<id>/card.png, exit 0 = no FAIL
4 in game      python tools/prop_shot.py <id>      -> real client, Props.place, shots into the card
5 review       fresh Sonnet-high reviewer on card.png + brief.md -> PASS or numbered defects (2 fix rounds max)
6 Kort         cards batched on one page; Kort approves the look (and any gameplay effect)
7 ship         def + art already registered; commit on the branch; merge only with Kort's yes; place with the prop menu
```

The **prop card** is the one thing that gets reviewed: header with the numbers and every automatic check, the prop at 1x
beside the Knight (game scale), a 4x turnaround (front, both 3/4 views, side, back, top), a collision map per layer,
and up to four in-game shots.

## Who runs what

| Step | Who | Model and effort |
|---|---|---|
| Prop list, use and footprint | lead, Kort approves the list | lead session |
| 1 brief | builder (lead may pre-fill) | same as the builder |
| 2-4 model, card, shots | builder | Sonnet 5.5 high for routine props; Opus 5.5 medium for flagship or story props (Kort's art rule). Which class a prop is goes on the list. |
| 5 review | a **fresh** reviewer per prop and per round, never the builder | Sonnet 5.5 high |
| 6 approval | Kort | - |
| 7 ship | lead | Sonnet 5.5 high |

Routing confirmed by Kort (2026-11-09) after a blind test: Sonnet 5.5 high and Opus 5.5 medium built the same prop from
the same brief and landed at the same review score for similar tokens; Kort picked the Sonnet build. So Sonnet 5.5 high
is the default builder; Opus 5.5 medium builds only flagship and story props (the O rows in `PROP-LIST.md`).

Never a Fable model. One writer per checkout; builders work in their own worktree or on disjoint `art/props/<id>/` folders
and defs. At most four game clients across the workspace; `prop_shot.py` is one client and refuses to start when a client
from the same checkout is running.

## Step 1: brief

```
python tools/new_prop.py <id> --use dressing|lobby|gameplay --size SX SY SZ --name "Display Name" [--setting "..."] [--parts a,b] [--glow b]
```

It writes `art/props/<id>/brief.md`, a placeholder `art/props/<id>/model.py` and the def
`src/main/resources/data/gravesandglory/gg_props/<id>.json` with the next free custom_model_data block. Fill every TODO in
the brief before modelling; the card fails while a TODO is left. Rules for the brief:

- **Footprint** in whole cells (x east, y up, z south). The front faces north. Keep it as small as the idea allows.
- **Scale against a troop:** a troop is 2 blocks (about 32-35 px). Furniture is troop-scaled (a table top at 12-14 px, a
  door at 32-40 px); "giant" things say how many troops tall they stand.
- **Silhouette at 15 m** and **close-up story at 3 m:** one sentence each. Dressing leans on the first, lobby on the second.
- **Materials:** name proplib materials (`MAT` keys) or say which new ramp (`make_ramp("RRGGBB")`) and why.
- **Collision:** which cells are solid (barriers). A troop is 2 cells tall; a cell at y=2 or above blocks only jumps and arrows.
- **Gameplay effect** (gameplay use only): what the prop should do in a battle. This is design only; Kort decides.

## Step 2: model and texture

Model in `art/props/<id>/model.py`; `build()` returns `proplib.Prop`. The frame and look rules are `art/props/README.md`;
read it first. The short version a builder needs:

- Units are model px (16 = 1 block). Origin is the min corner of the anchor cell `((sx-1)//2, 0, (sz-1)//2)`; the
  footprint in px is printed at the top of the scaffolded `model.py`. **The front faces north (-z, smaller z).**
- One element: `E((x1,y1,z1), (x2,y2,z2), "material", skip="down", name="what it is", faces={"up": "other"}, rot=("y", 22.5, (ox,oy,oz)))`.
  Whole-px coordinates only (density check). Rotations: one axis, -45, -22.5, 22.5 or 45 degrees. Put the rotation
  origin on the element's bottom edge on the side it leans toward (for a stake leaning north, its north-bottom edge);
  any other origin swings a corner below the floor or out of the footprint and the card fails. `skip` the faces nobody sees
  (bottoms on the floor, faces buried in another element).
- A part's elements stay inside -16..32 on every axis (3 blocks). A taller prop uses a second part stored shifted:
  `Prop(id, {"base": base, "top": top}, shift={"top": (0, 32, 0)})`. Model **both parts in the real prop frame** (the top
  part's elements at y 32..64); the builder subtracts the shift when it stores the part and the game adds it back. Every
  element of a part must lie inside -16..32 after the shift, so an element crossing y=32 is split in two. Wider props
  split the same way on x or z. A prop exactly 3 cells tall (y 0..48) can instead stay one part with `shift={"<id>": (0, 16, 0)}`
  (stored y -16..32). Glowing parts (coals, lantern glass) are their own part listed in `glow=(...)` and in the def.
- Materials come from `proplib.MAT`: `stone`, `stone_top`, `soot`, `iron`, `steel`, `anvil`, `gilt`, `wood_x|y|z`
  (grain along the axis), `swood_*` (stained), `plank_x|z`, `leather`, `linen`, `water`, `stump`, `endgrain`, `grit`,
  `coal`, `hotwall`. A new material is `Mat(make_ramp("RRGGBB"), 2, pattern)`; patterns are small functions returning a
  tone delta (-1, 0, +1). House colours are `HOUSE[house]["field"|"charge"]`. Every texel must come from a ramp
  (palette check); never paint raw colours or gradients.
- Custom texture (a map, papers, a crest): `E` also takes a painter object instead of a material, as `ShieldPaint` in
  `build_props.py` does. A painter must list the ramps it uses in a `ramps` attribute or the palette check fails.
- Face orientation: on the north (front) face, texture column 0 is at max x, which is the viewer's left; lettering and
  maps painted by column come out mirrored unless you count from the right. The card's front view is drawn as seen from
  the north, so higher x is on the left.
- Examples: the six forge-set props are functions in `art/props/build_props.py` (read `forge` and `anvil`); `stone_hand`
  and `stake_barricade` are `model.py` examples.
- Light is baked by `Mat` (top +1, north and east lit, south and west -1, underside -2). Do not add shading by hand.
- Look: chunky, readable, troop scale. A few big shapes first; detail is 1-2 px insets, bands, pegs, nails. No noise,
  no single-pixel speckle fields, no smooth gradients, no thin 1 px parts longer than about 8 px (they shimmer in game). Flat 1 px boards or papers seen face-on are fine; the rule is about rods and wires.
- Budget: 64 elements target (96 hard), sheet 128 px (256 only over 8 cells). The forge (the flagship) is 61.
- If a build stops on an `AssertionError` naming an element outside -16..32, the part's `.json` is left half written;
  fix the element and build again (the next build rewrites it).
- Build and look: `python art/props/build_props.py <id>` writes `<part>.json`, `<id>.png` and `<id>.bbmodel`
  (open the `.bbmodel` in Blockbench to look around; edits there are lost, the script is the source).
- Set the def's `solid` cells to match the model (the card's collision map shows fill per cell), and `lights` /
  `effects` only if the brief asks. Vanilla particles and sounds only; effects run within 24 blocks of a player.

## Step 3: card and automatic checks

`python tools/prop_card.py <id>` rebuilds the prop and writes `work/props/cards/<id>/card.png` and `card.json`.
Exit 1 on any FAIL; a prop is not sent to review until it exits 0.

| Check | FAIL when | WARN when |
|---|---|---|
| def | def parts, exported parts and `model.py` parts differ; glow parts differ | - |
| density | any face is not 1 texel per model px | - |
| frame | a stored element leaves -16..32 | - |
| footprint | the model leaves the footprint by more than 2 px, or goes below the floor | - |
| palette | a texel colour is not from a proplib ramp | - |
| budget | over 96 elements; sheet over its cap | over 64 elements |
| collision | - | a solid cell the model hardly fills (invisible wall); an open cell the model mostly fills (walk-through) |
| brief | no brief, or a TODO left | - |
| in-game | - | no in-game shots yet |

A WARN is not a failure, but the builder states in its report why each one is right (the forge hood fills an open
cell at y=2: it is above head height, so it is right).

## Step 4: in-game shot

`python tools/prop_shot.py <id>[,<id>...]` builds the prop pack, starts one isolated client (never a player save),
starts a practice battle, lays a platform, places each prop **through `Props.place`** (anchor, barriers, lights, display
parts, effects: the same path as the prop menu and maps), stands a Knight beside it and takes near (3 m) and far (15 m)
shots by day and lantern-lit night, plus a lineup. The shots land in `work/props/shots/<id>/`; rerun `prop_card.py <id>`
to put them on the card. Takes about 5-10 minutes (most of it the Gradle build). It fails when a placement is refused
(e.g. overlapping cells) or a shot is missing.

A new def is part of the jar's data, so the client must be rebuilt for it; `prop_shot.py` always runs a fresh build.

## Step 5: review

A fresh Sonnet-high reviewer per prop and per round. It gets: `card.png`, `brief.md`, `art/props/README.md` "Look", and
`docs/reports/prop-kit-art-sheet.png` (the approved forge set) as the bar. It never gets the builder's opinion. It returns
`PASS` or a numbered list of concrete defects, each with the view (e.g. "4x 3/4 front-east") and the place on the prop.
Checklist:

1. **Brief fit:** it is the thing the brief describes, at the stated scale against the Knight, for its use (silhouette
   reads at 1x for dressing; close-up story present for lobby; collision matches the design for gameplay).
2. **Front:** the face meant to be seen is the front view (north).
3. **Style:** chunky 16 px a block like the troops and the forge set; light from top-front-left; no noise, gradients,
   smooth or "AI-rendered" surfaces; patterns line up across boxes; materials read as what they are.
4. **Construction:** no floating or interpenetrating parts that look wrong, no z-fighting faces, no holes where a face
   was skipped but is visible (check back and top views), nothing poking out of the footprint.
5. **Collision map:** solid cells cover what a troop should bump into; no invisible walls; no walk-through mass.
6. **In game:** it sits on the ground (no float, no sinking), looks the same as the turnaround, reads by day and at
   night, and the Knight beside it confirms the scale. Glow parts glow; nothing renders magenta-black.
7. **Numbers:** within budget; any WARN is explained.

Up to two fix rounds (builder fixes the list, a new reviewer checks). If defects remain after two rounds, the prop is
marked blocked with its card and the open list, and the lead decides.

## Step 6: Kort

Cards are batched on one private page per milestone (cards, the prop list, open questions). Kort approves the look of
each prop; for gameplay props he also decides the effect. Nothing merges into `main` without his yes on the visuals.

## Step 7: registration and placement

- Registration is already done by the files: the def in `gg_props/<id>.json` (loaded on data reload), the art in
  `art/props/<id>/`, packed by `python tools/build_prop_pack.py` into `dist/props/gg-props-<sha8>.zip`. No registry entries,
  no Java change: a new prop needs only a new server resource pack (prop kit anti-goal).
- Commit only the prop's files (`art/props/<id>/*`, the def), one commit per prop: `art(props): <id>`.
- After Kort's yes the lead merges; the builder server gets the new pack (`/prop pack reload` on the server after the
  file is replaced; see `docs/BUILDER-SERVER.md`).
- Placing: builders use the prop menu (pick, ghost, scroll to rotate, click to place, hit to remove). Maps carry props as
  anchors through mapforge export and `Structures.place`; battles treat a prop's solid cells as walls.

## Gameplay props: what exists and what needs Kort

Exists (approved 2026-11-08, S9): solid cells are barrier blocks, so troops path around a prop, arrows and projectiles
hit it, and map checks treat it as solid. A barricade or cover piece therefore already blocks movement and arrows with no
new code. Anything more (destructible, a cover bonus, a capture objective, a siege engine that acts) is a gameplay change:
the brief's "Gameplay effect" is a proposal for Kort, never code.

## Lessons from the forge and smithy set (2026-11-08)

What worked and stays: the script-as-source builder (`proplib`) with fixed ramps, the light rule and world-aligned
patterns gave a consistent look that Kort approved on the first pass; the density check; the contact sheet beside a Knight.

What cost time or quality, and the fix in this workflow:

- The prop list was hard-coded in four places (`build_props.ALL`, `prop_sheet.ORDER`, the prop-look harness, the def
  check), so a new prop meant editing tools and tests. Now props are discovered from `art/props/<id>/model.py` and the
  harness reads the loaded defs.
- In-game shots used hand-written summon lines with a calibration guess and a manually enabled pack, not the real
  placement path. Now `prop_shot.py` sets the pack and places through `Props.place`, so the shot also tests registration,
  barriers, lights and effects.
- The custom_model_data rule and the block-atlas issue were found at the art/systems hand-off. Now `new_prop.py` assigns
  the CMD block from the defs and the pack builder handles the atlas.
- There was no written purpose, scale or collision intent per prop, and the review saw only pictures. Now the brief
  states them and the card puts numbers, checks and a collision map next to the pictures.
- In-game shots placed the Knight so it overlapped some props and the far shots were too far for small props; the card
  keeps the in-game shots secondary to the turnaround and the reviewer checks scale on both.

## Sonnet test (2026-11-09): where the instructions failed

A Sonnet-high worker built `muster_board` (lobby) from this page alone, in its own worktree, in about 9 minutes and 125k
tokens; card passed and in-game shots passed on the second try. Where it had to read code instead of this page (all fixed
above unless noted):

1. A part's -16..32 limit did not say how to handle a prop exactly 3 cells tall (single part, shift 16).
2. Custom painters were not mentioned; it had to read `ShieldPaint`.
3. The palette check failed on its own ramps; the `ramps` attribute on painters was undocumented.
4. North-face column order (column 0 = viewer's left) was undocumented; its map came out mirrored once.
5. "Props live in model.py" had no worked example; the forge set lives in `build_props.py`.
6. The thin-part rule did not say whether flat papers count.
7. Not fixed: the brief's collision prompt has no example for thin boards over open cells.
8. Not fixed: the first `prop_shot` run crashed on a test-tools `NoClassDefFoundError` (`BuildMode`, `Veterans$Tally`)
   unrelated to the prop; a rerun passed. Rerun once before debugging. Step 4 has no progress output.

The lead's own slip: the first draft said shifted parts are modelled at y 0..32; they are modelled in the real frame.
The worker and the stone hand both hit it before the fix.

## Parallel builders: assign CMD blocks

`new_prop.py` picks the next free CMD block on its own branch only, so two worktrees pick the same one (muster_board
and stone_hand both took block 6; fixed by moving muster_board to 9). When builders work in parallel, the lead assigns
each prop a block and the builder scaffolds with `--block N`. Blocks in use: 0-5 forge set, 6 stone_hand,
7 stake_barricade, 8 drowned_boat, 9 muster_board. Next free: 10.
