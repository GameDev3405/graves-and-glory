# Lamps and cauldron: 360 review and rebuild advice

Reviewer: Sonnet 5.5, 2026-11-14, branch `props/live-fire` (tip 8571c190). Kort's words: "lanterns and cauldron needs a heavy rework" and "lights just need a flicker to be fixed, the light looks abnormal / odd".

Shots (real client, `python tools/prop_shot.py --offline --turntable street_lamp,street_lamp_wall,cauldron_tripod`):
- `work/props/review/shots/<id>/p1-prop-<id>-turn-000..315.png` (day, 8 angles; 000 = seen from the front/north, 090 = from the east, 180 = from behind, 270 = from the west), `-close-day`, `-close-night`, `-turn-night-000/090/180/270`. No Knight and no fence lanterns, so at night only the prop's own light shows.
- Contact sheets: `work/props/review/<id>-turntable.jpg`. Reference props looked at: brazier (card + in-game), forge, training_dummy, tent_soldier (cards), forge set in `docs/reports/prop-kit-art-ingame-near.png`.

## 0. What the house style has and these three lack

Brazier, forge, training dummy and tents carry 25-41 colours, 30-100 elements, a silhouette you recognise at 15 m, several materials side by side, and an obvious "story" part (forge: glowing hearth + hood tiers + bellows; dummy: shield, rope, straw). Ours: street_lamp 22 elements / 21 colours, street_lamp_wall 19 / 13 (very flat), cauldron_tripod 48 / 31 but the colours are mostly one brown and one blue-grey. Rules of the new builds:
1. One clear silhouette per prop (not a box on a stick).
2. At least three distinct materials with contrast between neighbours (dark iron vs pale stone vs stained wood vs warm glow).
3. The light source is the brightest, warmest, largest-readable thing on the prop, and is visible from at least 3 of 4 sides.
4. Nothing coplanar (z-fight), nothing 1 px floating.

## 1. Lamp light: what looks "abnormal" and the exact fix

Applies to `street_lamp_glass` and `street_lamp_wall_glass` (same `LAMP_FLICKER`, same `lamp_glass` material in `art/props/proplib.py` `_lamp_glass`, `LAMP_GLASS = fixed("B8641A","E0962A","F6C448","FFE27A","FFF4B8")`).

Diagnosis (numbers measured from the generated strip):
- 4 frames, `frametime=2` (0.1 s a frame), `interpolate=True`, order `[0,1,2,0,3,1,1,0,2,3,0,1,2,2]` = 14 steps = **1.4 s loop**. A candle that changes every 0.1 s is a strobe, not a flame. Real flicker drifts over 0.3-1 s.
- Mean glass luminance per frame: f0 214, **f1 165 (-23%)**, **f2 233 (+9%)**, f3 204. The order runs 0 -> 1 -> 2 in 0.4 s, i.e. 165 -> 233 = a **~40% swing in 0.2 s**. Frame 1 is a "deep dip": the whole face becomes flat dark orange (`E0962A`, core `F6C448`), rim and heart collapse to nearly the same colour. Frame 2 "flare" turns the whole face near-white (`FFE27A` rim, `FFF4B8` heart, the top of the palette).
- The pulse is the **whole pane at once** (rim, ring and heart all move together), so it reads as a bulb on a dimmer, not as a flame behind glass. A flame moves only its bright heart; the pane edge stays put.
- Colour: the steady frame is already pale yellow (`FFE27A` rim region, `FFF4B8` heart). In daylight the full-bright glass is almost cream-white, which is the "odd" look beside the vanilla lanterns (amber with a yellow core) in the same shots. The warm amber lives only in the dip frame.
- The block light is constant 14 (street_lamp) and 13 (wall) while the glass pulses: the glow on the ground never matches the glass. Leave the level alone (static light is correct for a steady lantern); the fix is in the glass.
- Interpolating between frames that are 3 palette steps apart makes the blends visible as shimmering mid-tones; the pane seems to "breathe" hard.

Fix (edit `_lamp_glass` and the two `LAMP_FLICKER` lines; rebuild; look at `--frames` shots):
1. **Palette**: stop using `FFF4B8` on the glass at all. Steady pane = amber. Rim 1 px `E0962A`, ring `F6C448`, heart `FFE27A`. Keep `B8641A` as the frame-edge tone (1 px darker line along the pane's outer edge so the glass has an edge against the iron). Result: warm amber lantern, cream only in a 2x4 px heart.
2. **Only the heart moves.** Rim and ring identical in all 4 frames. Frame 0 steady heart. Frame 1 heart one tone down (`F6C448`). Frame 2 heart one row taller (candle stretches up, 1 px) and the ring one tone up only at its centre columns. Frame 3 heart shifted 1 column (sway), top row cooler. Max change between any two frames: one tone on <=8 texels. That is a mean-luminance swing of about 4-6%.
3. **Timing**: `Anim(frames=4, frametime=6, interpolate=True, order=[0,0,0,1,0,0,2,0,0,0,3,0,0,1,0,2,0,0])` = 18 steps x 6 ticks = **5.4 s loop**, steady most of the time, one slow soft change every ~1.5 s, never two quick steps in a row. Interpolation now blends over 0.3 s, so it is smooth.
4. **No hard dips anywhere.** Nothing darker than the steady ring. If it ever looks like it goes out, it is too much.
5. **Glass has body** (optional but good): bottom 2 rows one tone darker than the middle (oil/soot), top 1 row one tone darker too, so the pane reads as a glowing volume and not a flat sticker. Same on all 4 side faces.
6. Wall and street lamp share the material, so one edit fixes both. If a second lantern type appears later give it its own `order` so lamps do not blink in step (street lamps in a row would otherwise pulse together). `Anim` is per part; two parts with different `order` lists is enough.

What "good" looks like: from 15 m the pane is a warm amber lozenge, steady; at 3 m you can see a small brighter heart that drifts a pixel every second or two; by day it does not turn white; nobody notices a flicker unless they stare.

## 2. street_lamp (1x3x1, 48 px, post + footing + lantern)

### What is wrong, by shot
- `p1-prop-street_lamp-turn-000` / `-180` (front/back): a thin pole with a **heavy grey bucket** on top. The lantern is 12 px wide (outer 4..12 with 3..13 eaves) on a 4 px post, ~2.5x the post, and iron is 80% of its face. The visible glass is only the 4 px between the two corner posts (glass 5..11, posts 4..6 and 10..12), 8 px tall: a small amber slot.
- `-turn-090` / `-270` (sides): same slot, even narrower because the corner posts and the eaves cover it from the side. The four stepped **corbels (y 28..32) make a bell-shaped skirt** under the lantern that reads as a grey funnel, bigger than the lantern's glass. The post is a plain stick with two bands; footing is a single 8x8 block.
- `-turn-045 / 135 / 225 / 315` (diagonals): the lantern reads as an iron box with a yellow gap; no visible interior, no depth in the glass; pyramid cap is flat (two plates + 6x6 crown) and tip is a single steel pixel pair.
- `-close-day`, `-close-night`: pane is a flat rectangle with a 2 px lighter core and hard square steps; iron has no variation (`iron` ramp, 4 tone, no rivets or seams); base plate (`platform` 10x10 + `lantern base`) looks like a second hat.
- `-turn-night-*`: glow does not read as light: the lit glass is a small sticker, iron stays dead grey, the ground pool is faint at 3 m. There is no lit-interior cue on the iron next to the glass.
- Silhouette at 15 m (`far` shots in `work/props/shots/street_lamp`): stick + box. Vanilla lanterns hanging from the fence posts next to it read better (amber glass fills most of the lantern).

### Rebuild (keep footprint 16x16 px, 3 cells tall, solid cell (0,0,0) only, light at (0,2,0); keep the shift-16 two-part split)
Recommended new silhouette: **post with a hooked crook arm, the lantern hanging from the arm** (a medieval lamp-post, not a bucket on a stick; asymmetric silhouette, glass visible from three sides, the front faces north so the lantern leans toward the viewer).
- **Footing** (stone): two steps. Lower 10x10 (3..13) x 3 px high, upper 8x8 (4..12) x 2 px high. Total 5 px. Add 2 corner chips: 2x2x1 px stone blocks at the lower step corners (one missing = weathered).
- **Post**: stained wood `swood_y` (darker than the current `wood_y`, so it separates from the footing and iron), 4x4 px (6..10) but moved to the **back half of the cell**: x 6..10, z 9..13, y 5..44. Three iron bands (6x6 x 2 px) at y 9..11, 21..23, 33..35 (bands 1 px proud each side).
- **Crook**: iron, 2 px square section. A **cap** 6x6 x 2 px on the post top (y 44..46, x 5..11, z 8..14). The arm rises 2 px to y 46..48 and runs **north** along z 9..11, from the post to z 3 (x 7..9), then a 2x2 down-hook end at z 3..5, y 44..46. Under the arm a stepped brace (two 2x2 iron boxes, 45 degrees if you use `rot=("x", 45, ...)`) from the post at y 38 to the arm at y 44. Total height 48.
- **Chain**: 2x2, y 41..44 under the hook, two alternating tones (link pattern: 1 px bright, 1 px dark) at x 7..9, z 3..5.
- **Lantern** (hung under the chain, centre x 8, z 4; outer 10 wide x 8 deep so it stays inside the footprint: x 3..13, z 0..8; height 11): 
  - base plate x 3..13, z 0..8, y 30..32 (iron, skip down).
  - four corner posts 2x2 px, y 32..39 at the four corners.
  - glass: front/back panes the full 6 px between the posts (x 5..11) set 1 px behind the post faces (z 1 and z 7) so the posts cast a depth line; side panes the full 4 px between the posts. Glass y 32..39. One 1 px iron cross-bar on the lower third (y 34..35) so it reads as a lantern frame, not a solid sheet.
  - cap: eaves x 3..13, z 0..8, y 39..40 with a 1 px lip; then a pyramid: 8x6 x 1 px (y 40..41), 4x2 x 1 px (y 41..42), ring 2x2 x 1 px (y 42..43) that the chain hangs from.
  - interior cues (glow part, warm `B8641A`/`E0962A` from the lamp ramp): a 1 px "lit" strip on the underside of the cap and the top of the base plate seen through the glass, so the frame looks lit from inside.
  - glass = the new material from section 1, 6x7 px visible on each of the 4 faces.
- Palette: iron with 1 px steel highlights on the arm top and the lantern posts (a few `steel` px), stained wood post, stone footing, glass amber. Aim for 28-34 colours and 40-55 elements.
- Fallback if the crook is rejected: keep a centred lantern on top but (a) remove the four corbels, replace by one 6x6 collar (y 30..32) with two small 45 degree brackets; (b) outer lantern 10x10, posts 2x2, visible glass 6 px wide; (c) taller roof 3 steps + ring; (d) footing in two steps and 3 bands on the post.

### Priorities
- MUST: lantern anatomy (visible glass >= 6x7 px on every face, posts 2 px, no bell skirt), amber glass with the new flicker (section 1), silhouette that is not a symmetric box on a stick.
- SHOULD: crook-and-hanging-lantern silhouette, two-step footing, third band, stained post, steel highlights, lit-interior cue.
- NICE: slight lean/missing stone chip, bird nest of straw under the arm, tiny steel hook.

What good looks like: at 15 m you see a dark post with a warm amber glow hanging off an arm toward you; at 3 m you see the chain, the lantern's posts, its pyramid cap and a candle heart behind glass; it looks like the same family as the brazier's iron.

## 3. street_lamp_wall (1x1x1, the same lantern on a wall bracket)

### What is wrong, by shot
- `-turn-000` (front): a squat grey box with a small chimney ("arm cap" + "chain" + "cap crown" stack = 5 px tall tower on a 6 px crown); the glass is a 4x6 slot. Reads as a bucket, not a lantern.
- `-turn-090 / 270` (sides): this is the angle that matters on a wall. The arm is a thick horizontal slab (2x12 px), the wall plate a vertical sliver at its end; the lantern hangs under the arm tip on a 2x2 px stub that merges with the cap, so there is no chain; the brace (stepped 2x2 blocks) vanishes against the plate. The lantern is dumpy: cap eaves 8 wide x 2, base 8 wide x 1, posts 6 tall, so 60% of the lantern is iron.
- `-turn-180` (back): a "propeller" of plate + arm seen end-on (expected, since it hangs on a wall; the back view is not needed, but skip faces that are never seen: plate's south, arm's south end).
- `-turn-045 / 135 / 225 / 315`: glass shows on only 1 or 2 faces and is covered by corner posts; no cap shaping.
- `-close-day`, `-close-night`: flat sticker glass, hard-edged; no lit-interior cue; plate rivets exist but at 1 px steel on a grey plate read as noise.
- Night: the glow is a tiny rectangle; nothing else around it is lit.

### Rebuild (keep 1x1x1, no collision, light (0,0,0) level 13; keep the back flush at z 16; all dimensions px)
- **Wall plate**: iron 6 wide x 12 tall x 1 deep at x 5..11, y 4..16, z 15..16 (skip south). 4 steel rivets 1x1 at its corners (inset 1 px). Add a 2 px wide x 4 px tall "boss" (2x4x1 px, z 14..15) at the arm root, centred at y 12..14, so the arm grows out of something.
- **Arm**: iron 2 px square at y 13..15, from z 14 to z 5 (x 7..9). A **hook end**: 2x2 down-turn at z 5..7, y 11..13. 
- **Brace**: one diagonal under the arm from the plate (y 6, z 15) up to the arm (y 13, z 7): use `rot=("x", 45, ...)` 2x2 box, or three 2x2 steps (y 7..9 @ z 13..15, y 9..11 @ z 11..13, y 11..13 @ z 9..11), clear of the lantern which hangs at z<=10.
- **Chain**: 2 visible links, 2x2 px at x 7..9, z 5..7, y 10..13 (two alternating tones); the arm hook carries it.
- **Lantern** (hung from the chain, outer 10 wide x 10 deep x 10 high; x 3..13, z 0..10, y 0..10; this is the same anatomy as the street lamp's lantern, share it as a helper function `lantern(x0, z0, y0, w, d, h)` in `art/props/proplib.py` that returns body `E`s and glass `E`s so both props stay consistent): base plate (y 0..2), corner posts 2x2, glass between posts (6 px visible on the north and south faces, 6 px on west/east), lantern height 9 (y 1..10), eaves y 10..11, pyramid y 11..12, ring y 12..13, so the chain runs from the ring up to the arm hook at y 13..15 (everything stays under y 16).
- Interior cue: warm 1 px strip on the cap underside and base top (glow part).
- Palette: iron plate/arm slightly lighter than the lantern iron (use `steel` top highlights) so the bracket separates; rivets in `gilt` or `steel` but 2x2 not 1x1.
- Budget 30-40 elements.

### Priorities
- MUST: real chain gap between arm hook and lantern cap (>= 2 px of chain visible from the side), visible glass >= 6 px on all four faces, kill the chimney stack on the cap, new amber flicker glass.
- SHOULD: diagonal brace that reads from the side, wall-plate boss and 2x2 rivets, lantern 10 wide x 10 tall with 2 px posts.
- NICE: tiny second bracket tab, a drip stain on the plate under the lantern (darker ramp tone, 2x3 px).

What good looks like: from the side you read bracket + chain + lantern as three clearly separate parts; from the front a warm pane with a bright heart under a pyramid cap, at 15 m a small glowing box sticking out of a wall.

## 4. cauldron_tripod (1x2x1, 29 px tall)

### What is wrong, by shot
- `-turn-000` (front): the silhouette is a stepped **brown pyramid / pagoda** (3 poles built from 4 px high stepped lumps, 3 px thick in the lower half) topped by a rope **cube** (`lashing` 4x4x4) with a dark **iron cube** on top that reads as a chimney or hat. It does not read as a tripod: no visible splayed legs, no empty space between legs. The pot is hidden behind two fat leg-stacks at its sides; only a grey slab shows. The fire is a **3-4 px high sliver** under the pot between the two front fire stones.
- `-turn-090 / 270`: legs again clump; the back leg stack (3 px square at the cell's south edge) is a column down the middle; the pot is a grey block half-hidden; flames invisible from the side (the side planes at x 3 and 13 are behind the poles).
- `-turn-180` (back): a fat brown stick (back pole, foot at the cell's south edge) fills the centre of the view; the pot is two grey stubs.
- `-turn-045 / 135 / 225 / 315`: reads as a campfire inside a brown pyramid; no chain, bail or ring is visible; pot is a blue-grey box (`pot` ramp 6E7380, too close to the stone ring).
- `-close-day / -close-night`: pot is a flat trapezoid; its body overlaps the flame planes; **a moire / cross-hatch patch** at the lower right of the fire = z-fighting: `flame front/back/west/east` sit at z=3, z=13, x=3, x=13, exactly in the planes of the `ember bed` faces (3..13 box) for y 2..4. The pot rim is four bars with a hollow centre: from above/low it shows as black. Stew is a 1 px dark brown inset that never shows. Hung pot hardly hangs: its bail is 8 px wide, chain is a 2 px stub, the "hanging ring" above the lashing is a dark cube.
- Particles (def effects): the black `minecraft:smoke` spiral spawns at y 0.85 off to the side (seen at the right of `-turn-090`, `-turn-315`, `-close-day`) and reads as noise; flame particle at z 0.2 is off-centre.
- Night (`-turn-night-*`): fire glow reads, but the iron pot is as light as the stones; no rim-light, no steam.

### Rebuild (keep footprint 16x16 px, y 0..32, solid cell (0,0,0), light at (0,1,0) level 12; effects may be moved)
A true **three-legged tripod, thin legs, wide stance, high lashing, pot hanging low**, fire open underneath so it shows from every side.
- **Fire bed** (glow part): `ember bed` shrink to x 4..12, z 4..12, y 0..2 (skip down); `ember heap` x 5..11, z 5..11, y 2..4. Ground char under the stones: `soot` slab x 2..14, z 2..14, y 0..1 (skip down) as an ash disc.
- **Stones**: 8 stones as now but only 2-3 px high and a bit larger (3x4 px), the ring drawn x 1..15. Use `stone` with a `soot` top face (darker toward the fire) so they separate from the pot. Put one stone at each leg foot so the legs stand behind a stone.
- **Fire**: `flames(8, 8, 2, 12, 5)` (two crossed planes, y 2..12, 10 px wide), **plus a second pair rotated 45 degrees about y** (`rot=("y", 45, (8, 2, 8))`) so it reads as a fire from the diagonals. Move them **inside** the ember heap box (flame planes strictly inside x 5..11 / z 5..11, never coplanar with the ember bed faces; the current z-fight comes from planes at 3/13). Tongues must be visible between the legs and under the pot: the pot's belly bottom at y 9 (see below) leaves y 2..9 open.
- **Legs**: 3 legs, `swood_y` (stained, darker brown) not `wood_y`, **2x2 px** section (3x3 only on the lowest step), stepped in 3 px high steps (9 steps, y 2..29), lean toward the lashing at (8, 8). Feet centres: front-west (1, 3), front-east (15, 3), back (8, 15) (the existing loop `u = (3k+1.5)/27` with `n=2` does it). The front pair must leave the front-centre open (that is where the pot and fire are visible). Add a 2x3 px foot pad (stone or `soot`) under each foot.
- **Lashing**: 2 rope bands (`rope` ramp) 6x6 x 1 px at y 28..29 and y 30..31 (not a 4x4x4 cube), with the leg tips poking 1-2 px above the top band (y 31..32, 2x2): the classic tripod apex. The hanging ring (2x2 iron) hangs *under* the lashing at y 26..28, not on top. Delete the iron cube above.
- **Chain**: 2x2 px, y 18..26, two alternating tones; **S-hook** 4x2 px iron at y 17..19.
- **Pot** (hung low, bottom at y 9, **narrower than the legs at that height or clipping a leg by at most 1 px**; octagon, not a box): 
  - belly 6x6: (5, 9, 5)-(11, 11, 11)
  - body: two crossing boxes (4, 11, 5)-(12, 14, 11) and (5, 11, 4)-(11, 14, 12) (chamfered corners)
  - rim lip: 1 px proud ring at y 14..15 (x 3..13 chamfered the same way), opening 6x6.
  - stew: x 5..11, z 5..11 at y 14, lower than the rim top by 1 px: `stew` with 3-4 lighter bubble texels. Put it in the glow part as a flat top with 2 animation frames of bubble highlights if you want it live.
  - ears: two 1x2x2 px eyes at x 2..3 and 13..14 on the rim, z 7..9, y 13..15; **bail** arc: iron 2 px from the ears up to y 17..19 (x 3..13, a 10 px flat top at y 18..19 with two 2x3 posts), the S-hook catches it.
  - material: `iron` (dark) with a 2 row `soot` band at the bottom of the belly and a 1 px warm underlit line at the belly bottom edge in the glow part (`HOT[2]`); **not** the `pot` blue-grey ramp (6E7380). Make it the darkest object on the prop so the fire and stew pop.
- **Colours**: stone mid-grey, legs stained dark brown, rope tan `7A6038`, pot near-black iron, fire orange/yellow; target 36-42 colours, 50-60 elements.
- **Effects** (`gg_props/cauldron_tripod.json`): smoke/steam above the pot opening: x 0.5, y 1.15, z 0.5 (pot top at y 16 px = 1.0 block), `every 14`, chance 0.5; flame particle x 0.5, y 0.5, z 0.5; lava particle 0.3 at the fire centre (y 0.2); crackle sound unchanged. Remove the off-centre flame at z 0.2.
- Shift the ring and fire stones so that the 4 corners of the footprint are used: today the poles' feet and ring stones all cluster in the middle 12 px.

### Priorities
- MUST: (1) fix the z-fight (flame planes strictly inside the ember boxes) and make the fire visible on all four sides between and under the pot; (2) thin splayed legs with a high lashing instead of the stepped brown pyramid (kills the "hat"); (3) a pot that reads as a pot: octagonal 10 px body, rim lip, ears and bail, dark iron, hung on a visible chain at y 9.
- SHOULD: stained legs vs rope vs iron vs stone contrast, ash disc + soot-topped stones, 45-degree second flame pair, steam particle above the pot, apex with leg tips.
- NICE: animated stew bubbles (glow part, 2-3 frames), a leaning wooden ladle (4x2x12 px) against one stone, a dented pot rim.

What good looks like: from 15 m a clear tripod silhouette (three thin diagonals meeting at a rope knot) with a dark round pot hanging in a ring of warm fire; at 3 m you see the chain, the bail, the stew top with bubbles and flame licking up the pot's sides, from the front, the sides and the back.

## 5. Checks for the rework

- `python tools/prop_card.py <id>` exit 0 (density, palette, footprint, budget), then `python tools/prop_shot.py --offline --turntable <ids>` and look at the three contact sheets again plus `--frames` (flicker moves slowly, never goes dark or white).
- Review all 14 turntable shots; the cauldron especially at 180 and 090 (the angles that were worst).
- Do not edit `Spec.java` or `preflight.json`.
