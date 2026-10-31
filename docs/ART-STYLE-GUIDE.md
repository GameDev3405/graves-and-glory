# Art style guide: troops and weapons

Draft 3, 2026-10-22, for Kort's review: the weapons section (7) carries the weapon look round's outcome (questions
rounds 28 and 29), now split into requirements, approved constructions and other weapons, and section 9 gains the gates
checklist (after an outside review of draft 2). This is the rulebook for every troop skin, relief layer and weapon model. It
replaces our use of the top-down pixel-art skill for troops and weapons (that skill stays for icons, tiles and UI art).
The approved look (War Table and Iron Ledger) and `work/ui-concepts/GRADING-RUBRIC.md` always win over anything here.
Section 10 (2026-10-30, draft for Kort, task 2B.2) adds the per-House art bible and Kort's fixed rules; read 10.0 first.

The guide is written for the Minecraft 3D case: flat faces on boxes, 1 texel per model pixel on skins and relief and 2
on weapons (section 7), lit by the game's own
directional shading (top and bottom faces darker, east and west faces dimmer than north and south) plus whatever the
player's shader does. The paint carries its own light on top of that, from the top left.

## 1. Ramps: 4 to 5 tones per material, hue-shifted

Every material is painted from one ramp of 5 tones, numbered 0 (shadow) to 4 (highlight), with tone 2 the material's
base colour from the troop's Look line (unit identity spec, section 1). A Common may use 4 of the 5; nothing uses more.

How a ramp is made (the spike's `paint.ramp`, which the build tool will carry as `build_skins.py --ramp <hex>`):
- **Value.** Each step is ±22% of the base value (±16% for pale cloth, ±26% for gilt), clamped to [0.06, 1].
- **Hue.** Shadows turn toward purple-blue (268 degrees), lights toward warm yellow (52 degrees), 6 degrees a step, never
  past the target. So steel's shadow is blue-grey, its light warm grey; wood's shadow is plum, its light sand.
- **Saturation.** Darks gain 0.04 a step, lights lose 0.07 a step.
- **Team tint rule.** No tone may have saturation over 0.45 with value over 0.60 inside 45 degrees of any side's hue
  (gold 42, crimson 3, teal 177, violet 264). The ramp clamps saturation to 0.44 above value 0.60, so gilt lights go
  pale rather than loud. `build_skins.py` fails the build on any texel that breaks this.

The Gilded ramps (hex, tones 0 to 4):

| material | 0 shadow | 1 dark | 2 base | 3 light | 4 highlight |
|---|---|---|---|---|---|
| steel `#7C8591` | `#3A3E4B` | `#5A616E` | `#7C8591` | `#A6ADB4` | `#D6D6D7` |
| gilt `#96783A` | `#482E16` | `#6F5026` | `#96783A` | `#BDAA6A` | `#E4D780` |
| wood `#6E5440` | `#3E261F` | `#563C2E` | `#6E5440` | `#867057` | `#9E8E72` |
| stained wood `#563C2E` | `#301B16` | `#3E261F` | `#563C2E` | `#6E5440` | `#867057` |
| linen `#BFB59A` | `#82715E` | `#A0937B` | `#BFB59A` | `#DED9C2` | `#FCFAEF` |
| leather `#5A3E2C` | `#321A15` | `#462B20` | `#5A3E2C` | `#6E553D` | `#826E52` |
| bowstring `#D9D3C1` | `#9C8F7E` | `#BBB19F` | `#D9D3C1` | `#F7F6ED` | `#FFFFFF` |

Stained wood is the wood ramp one step darker (its tones 1 to 4 are wood's tones 0 to 3, with a new tone 0). It is its own
material on the same face table, for limbs that carry gilt, so the gilt reads by value against the wood. In the material
map it is wood.

Each House adds its own base colours from its Look lines (bone, verdigris, rust, kelp, hide) and derives its ramps the
same way. The ramp file is `art/ramps.json` once the build tool carries it; until then, run `paint.py` from the weapon
look round (`bannerbound-p2b/work/p2b/look-round/`, which grew out of the spike's). It also maps each ramp to a material,
which is how the build finds a troop texel's material (Controller's call).

## 2. Light from the top left

One light, above and to the left of the viewer, slightly in front. Everything follows from it:
- a highlight (tone 4) sits on the top-left edge of a form, one texel wide, never a full face;
- the shadow (tone 0) sits on the bottom-right edge and under overhangs (a brow, a pauldron, a guard, a belt);
- a cast shadow is at most one texel wide and only under something that really overhangs;
- a glow (`_e`) is the only thing lit from inside;
- on a weapon at 2 texels per px, steel plate and gilt trim take an edge texel: the top row and left column one tone up,
  the bottom row and right column one tone down, each one texel wide.

## 3. Face brightness

Paint each face of a box at the tone its direction earns, then put the material pattern on it. The names are
Blockbench's, on the model as it is authored (y up, the big faces across z), seen from the camera at yaw 0, which sits
at +z:

| face (Blockbench) | tone | what it is | why |
|---|---|---|---|
| up | 4 | the top | faces the light |
| south | 3 | the front: the face toward the viewer | the viewer's face, carries the pattern |
| west | 3 | the viewer's left | toward the light |
| north | 2 | the back | base |
| east | 1 | the viewer's right | away from the light |
| down | 0 | the underside | shadow |

The converter turns a weapon by a half turn about x, which makes Blockbench's south the game's north: the front here is
the game's north face, so a `.bbmodel` is always painted by the table's names, never the game's. On a troop, "front" is
the face toward the enemy and the lit side is the one on the viewer's left when the troop is seen from the front; on a
weapon drawn upright, the front is the flat the viewer sees. Faces the
game darkens anyway (up and down by 0.5 and 0.6 in vanilla's entity shading) still get their own paint: the paint
reads under shaders and in portraits, where vanilla shading is weaker.

Polished metal is the one exception, and it is a blade's: a blade is two flats and a fuller, and the lit flat (the one
toward the light) is painted at tone 4, the shade flat at tone 3 and the fuller at tone 2, each one tone down on the
back, since a flat blade at tone 3 reads as pewter at six blocks (spike, row 1). Plate steel (a guard, a pommel, a collar)
stays at the face tone. Keep the Look line's steel, `#7C8591`, as tone 2 (Controller's call).

## 4. Material patterns

Each material has one pattern, drawn in the face's own tone ±1. No material has two patterns, and no face has noise.
A mark (a glint, a hammer mark, a pit, wear) is placed by hand where the light and the use would put it, never on a
fixed spacing (section 6).

- **Wood:** grain is one line along the long axis. On a weapon limb it is one texel wide along the back column (the +x
  edge), one tone down from its face: never cross bands, which read as bamboo. A fleck, where one is wanted, is one
  texel a tone up on the light edge.
- **Steel (polished):** sheen. The column nearest the light is tone 4, the column away from it one tone down; a
  one-texel hammer mark one tone down, scattered down the middle. Rims and edges get the tone-4 edge highlight. On a
  blade at 2x each flat carries a bevel line (its outer column stays at the flat's tone and the rest drop one), with
  polish glints, one texel a tone up, near the guard and at the point's shoulder.
- **Iron (dull plate, mail):** as steel but on tones 1 to 3, and mail is a two-texel check of tones 2 and 1.
- **Gilt and brass:** flat tone 3, the top-left texel (two on a wide face) tone 4, the bottom-right texel tone 1.
  Gilt normally covers no more than 20% of a troop's visible texels, or 25% of a weapon's rendered pixels (section 7.1);
  it is trim, subordinate to the silhouette and the House's mark.
- **Cloth (linen, wool, a surcoat):** the weave is a check of tones 2 and 3 on the big faces; a wound wrap (a grip, a
  bandage) is bands across: every other row one tone down (a wrap at the hand is bands of whole pixels, section 7).
- **Leather:** flat tone 2 with scattered pits (one texel, one tone down), placed by hand so they never read as a grid.
- **Bone:** linen's ramp made of ivory, with a crack line (tone 1) along the long axis, never more than one per face.
- **Bowstring:** a single tone-2 line (`#D9D3C1`), 0.4 px thick in a model, 1 px in a sprite. Tone 2, not 3, so the
  string is not the brightest thing on the bow.

## 5. Worked examples

**A blade at 2x, seen from the front, steel.** The lit flat is tone 4, the shade flat tone 3, the fuller tone 2 (polished
rule). On each flat the outer column stays at the flat's tone and the rest drop one, so a bevel line runs down the
blade. A polish glint, one texel a tone up, goes near the guard and at the point's shoulder, not on a row count.
Result: a bright blade with a cool shade flat and a dark groove, read at six blocks as "steel, lit from the left".

**A bow stave, 1 px wide and 2 px long, wood at 2x.** The front face is 2 x 4 texels at tone 3, with the grain as one
texel column along its back edge at tone 2. The east face of the same stave: fill tone 1, its grain line tone 0. The
stave's ends are tone 1, so the joint to the next stave shows no notch.

**A 4 x 4 belt buckle on a relief layer, gilt.** Tone 3 fill, top-left two texels tone 4, bottom-right texel tone 1.
Relief side faces take the texel's colour (design 3), so nothing else is painted.

**A wound grip, 1 x 4 px, linen.** It sits at the hand, so it is painted in whole pixels (2 x 2 texel units): four
bands of two texel rows, tone 3, tone 2, tone 3, tone 2; the collar above it gilt tone 3 with its tone-4 corner, also
in whole pixels.

**A surcoat panel on a team part.** The cloth sheet carries the field greys; paint only the weave on the greys
(tones 2 and 3 of grey), and never a hue. The team colour comes from `build_skins.py`.

## 6. Never

- Never more than 5 tones of one material, never a sixth "in-between".
- Never pure black (`#000000`) or pure white on a troop; the ramp ends are the darkest and lightest.
- Never a highlight on a whole face, and never a highlight on the bottom or right edge.
- Never a hue with saturation over 0.45 and value over 0.60 within 45 degrees of a side colour (the tint rule).
- Never dithering, gradients, noise or anti-aliased edges. Every texel is one of the ramp's tones or transparent.
- Never a texel stretched: on skins and relief one texel is one model pixel; on weapons one texel is half a model pixel.
  Every face's UV is exactly its size at its sheet's density (fractional where the face is).
- Never a face painted in a tone brighter than the face above it (an underside brighter than the top).
- Never a pattern on a face smaller than 2 x 2 texels; such a face is a flat tone.
- Never coplanar faces between different cubes in one frame: two faces on one plane that overlap and face the same way
  z-fight. Offset a cube by 0.05 px. Faces back to back, each hidden inside the other, are fine.
- Never an air gap between parts: every frame is one connected piece (the string may hang free by up to 0.2 px).
- Never a fixed-period repeat: a glint every seventh row, a hammer mark every fifth, a stitch every second, wear on a
  grid. It reads as procedural texture, not wear. Place marks by the light and at edges (a wrap, a weave or a mail check
  is a structure, not a mark, and stays).
- Never team colour on a skin or a weapon; it lives only on the cloth sheet.
- Never a glow texel on a team surface, and never more lit texels than the glow cap (6, or 16 on Epics and Legendaries).
- Never a copy of another mod's texture or model, and never a vanilla item's shape re-coloured.

## 7. Weapons in Blockbench

A weapon is hand-shaped cubes. Within the requirements of 7.1 and the look of sections 1 to 4, an artist may vary
silhouette, proportions, ornament and construction. The constructions in 7.2 are approved starting points, not the only
way to build a weapon: an alternative shows equal or better readability, cohesion and in-game performance (section 9,
Gates). A House's weapon is told from another House's by its shape, not by its paint alone.

Sections 7.1 and 7.2 come from the weapon look round (the Order Squire's arming sword and the Order Archer's bow at tiers
I to III, in `bannerbound-p2b/work/p2b/look-round/`, graded 8 out of 10 by an independent reader), as Kort ruled on it in
questions rounds 28 and 29. Those six files are the worked examples.

### 7.1 Requirements (every weapon)

These are mandatory. Each protects the in-game result, a build check or a ruling of Kort's.

**Budget.** Tiers I and II are at most 16 cubes a frame, tier III at most 18, plus the string on a bow. The string stays
out of the cap because the rig fixes it (one cube at rest, two when drawn, so the drawn string makes a V at the nock);
every drawn quad still counts toward the 128 the renderer allows a frame. Ornament is paint, not cubes: a sun on a boss,
chasing along a limb, grain, wraps, edge texels and a blade's bevel line cost no cubes.

**Texel density (Kort's ruling, round 29).** A weapon is painted at 2 texels per model px, on a 64 x 128 sheet, every
face's UV exactly twice its size. Skins and relief stay at 1 texel per model px. Three rules keep the finer texels from
clashing with the troop's hand and from crawling in motion (vanilla loads entity textures without mipmaps, so at 12 to
24 blocks a texel is about one screen pixel):
- *Whole pixels at the hand.* Within 1 px of the hand (grip wraps, collars, the underside of a guard or boss, the top of
  a pommel), paint in 2 x 2 texel units, so the hand's border stays on the skin's grid. One-texel detail goes on blades,
  limbs and plates away from the hand.
- *One tone step.* A one-texel pattern sits at most one tone step from its face: no gilt line against a colour two tones
  down.
- *One connected piece.* Every frame is one connected piece: no air gap between parts, and no two faces of different
  cubes on one plane (section 6).

**Joints.** A joint shows no notch and no dashed seam: a part that turns at a joint runs back past it far enough to hide
the cut, and cubes that would share a plane are offset by 0.05 px (section 6).

**Hand and tier cues.** Whatever sits at the hand (a guard, a boss, a collar) is sized to the 4 px fist. Tier cues go on
the parts the fist does not cover. The fist is a 4 px box at the grip's origin and hides y -2 to +2, so a tier shows on
the guard, the pommel, the boss at the blade root, and on a bow the collars (from y 2.5), the limbs, the nocks and the
tips. A grip carries its tier's material (leather, linen wire, enamel) for views without a hand, but it is not the cue.
Judge every weapon with the fist box in view, at every size, painted at skin density (1 texel per px, from an Order
troop's arm) so the hand's finer neighbour shows.

**Gilt.** Normally at most 25% of a weapon's rendered pixels, measured on the 45-degree view at 1.5 blocks, bare and in
the fist, the fist's own pixels not counted. Gilt stays subordinate to the weapon's silhouette and the House's mark. A
weapon over 25% passes only if the colour line of the gates (section 9) still holds and its review says why. Gilt pops by
value contrast against dark leather, stained wood or steel, never by more saturation; the gilt clamp (section 1) stays.

**Paint.** Every face has its own texels on the weapon sheet (64 x 128), painted by sections 1 to 4, at 2 texels per
model px. In brief: blade tones are 4 on the lit flat, 3 on the shade flat and 2 in the fuller (section 3); wood grain is
one line along the limb (section 4); the string is at tone 2; limbs that carry gilt are stained wood (section 1).

**Rest group and frames.** Every weapon has a `rest` group, from which the converter builds a far version of at most 32
quads for troops outside the near set (Controller's call). In a `.bbmodel` only `rest` is visible on open; the `pull_*`
groups are saved hidden and are still frames (art pipeline v2 section 2.1). A pull frame draws a bow's string in two
halves that meet at the draw point.

**Size.** Kort's rulings, which only Kort changes: a sword is about 1.5 blocks long and at most 24.5 px (round 28), and a
bow limb is 2 px deep at the grip. Defaults: a blade is 1 px at the core and a staff 2 px.

### 7.2 Approved constructions

Each binds only the weapon it describes. A different sword or bow (a notched sword, a bone bow) may borrow from it or
depart from it under 7.3.

**The Order Archer's bow, tiers I to III.** A limb is five staves as one continuous recurve, each stave about 1.9 to 2.3
px long, the leans stepping from about -6 degrees at the grip to about -44 on the fourth stave and the fifth turning back
to the tip. The profile, grip back to string, is about a fifth as deep as the bow is tall. Depth tapers from 2.0 px at the
grip to 1.2 px at the tips. No tip is flatter than -45 degrees. This replaces the old chain of three cubes a limb. A stave
runs back past its joint by half its width times the sine of the turn, plus 0.05 px, so a joint shows no notch or dashed
seam. Where gilt rides on the limbs (tier III) they are stained wood. The string is a 0.4 px cube at tone 2; on a recurve
it may lie over the knee by about 0.2 px, as on a strung bow. The pull frames flex the limbs 4 degrees a frame and bring
the string back 0.75 px a frame (Controller's call; `p2-depth-bow-nock` tunes it).

**The Order Squire's arming sword, tiers I to III.** A 13 px blade and a 3 px point on the guard, the blade at least 7
times its base width.
- The blade tapers by turned cubes, not steps: each flat is one cube about 0.6 px wide, turned about 1.6 degrees about z on
  its outer base corner, so the outline narrows from 2.0 px at the guard to about 1.3 px at the top. The core stays
  upright, a recessed fuller (a proud spine on tier I). The blade is 1.0 to 1.3 px deep.
- The point is clean: two small edge cubes, turned about 7 degrees, bring the flats' edges onto the point core, and a
  0.45 px tip cube turned 45 degrees and 0.6 px deep caps it. No cube crosses the centre line (x 0), which would draw an X
  and z-fight its twin.
- The guard and pommel grow by tier. Tier I: a 9 px bar 1 px tall with end blocks over a seat block (2.5 px clear of the
  fist a side), and a 1.8 px wheel pommel. Tier II: quillons reaching 4.5 px, swept 22 degrees up toward the blade, with
  1.2 px gilt finials, and a 2.2 px wheel pommel with a gilt boss. Tier III: a straight cross reaching 5 px, a 3 px steel
  boss at the blade root (no wider than the blade plus 1 px a side) with the Order's sun painted on its faces, and a 3 px
  sun pommel.
- A grip is wound: a thin core plus bands, in whole pixels (7.1).

### 7.3 Other weapons

Everything else is made inside 7.1 and is free in the rest: the other Houses' swords and bows, axes, spears, tridents, the
harpoon, staves, wands, the scythe, the crossbow and arbalest, the lantern. Silhouette, proportions, ornament and
construction are the artist's, and invention is welcome. What the weapon has to show:
- *Its own shape.* Greyed out, it is told from every other weapon in the roster at six blocks (the lookalike checks of
  unit identity section 7). A bow or sword of another House does not take the Order's limb or guard profile unchanged.
- *Equal or better.* On the views of 7.1 (the 45-degree view at 1.5 blocks, bare and in the fist) it reads at least as
  well, holds together as well and costs no more in the renderer than the approved construction nearest to it.
- *Tiers on exposed parts.* Its brief names the parts that carry the tier cue, and the fist hides none of them.
- *A record.* The first weapon of a new family (the first axe, the first spear) that passes the gates and that Kort has
  seen is added to 7.2 as an approved construction, for the next ones to start from.

## 8. LabPBR maps

Troops and weapons ship `_n` and `_s` beside their colour sheet (art pipeline v2 design, section 6). The painter paints
neither by hand. A material map `<name>_m.png` marks each texel with its material key colour (the table in that
design), and the build derives `_s` from the key and `_n` from the pattern heights of section 4. For a weapon, the painter script writes the
material map in flat key colours only. For a troop, the `_m` (`<troop>_s<stage>_m.png`, `_s<stage>_r_m.png`) is
never painted: a script generates it from the colour sheet by ramp lookup through `art/ramps.json`, and a colour that
matches no ramp fails the build (Controller's call).

## 9. Beside the grading rubric

The rubric (`work/ui-concepts/GRADING-RUBRIC.md`) grades the result at 9 for player-facing art. Weapons are the one
exception: they pass at 8.5 from the independent grader, and Kort looks at each House's weapon sheet before it ships
(Kort's ruling, round 29; the grader of the 2x trial found 9 out of reach for weapon art at Minecraft scale). Skins,
relief and all other art keep 9. This guide is how the work is made so it can score. The mapping:

| rubric line | this guide |
|---|---|
| 1 Legibility | sections 1 and 3: tone steps of 22% keep faces apart at six blocks |
| 3 Focal point | section 4: gilt is trim, one pattern per material, the mark reads first |
| 6 Colour discipline | section 1's tint rule; team colour only on the cloth sheet |
| 8 Feel | sections 2 and 4: hand-authored, one light, no noise |
| 9 Honesty | each sheet's own density (1 texel per model px on skins and relief, 2 on weapons), real ramps, nothing that cannot ship |

### Gates

A weapon ships when every line of this checklist is a yes, the independent grader's score is at the bar, and Kort has
seen the House's weapon sheet. The checklist is the gate; the score supports it, never overrides a "no" on it, and a score
over the bar waives none of it. Each line is observed on the 45-degree view at 1.5 blocks and in the running game, bare
and in the fist.

1. *Silhouette.* With the paint greyed out it reads at six blocks and is told from every other weapon in the roster and
   from the same weapon one tier up.
2. *Tier.* The difference between tiers shows on exposed parts with the fist box in view.
3. *Clean build.* One connected piece in every frame, no z-fighting, no gap, no notch or seam at a joint, within the cube,
   quad and far-model budgets (7.1).
4. *Animation.* The pull or swing frames play without popping, a bow's string meets at the draw point, and the far model
   reads as the same weapon.
5. *Materials.* One ramp and one pattern per material, gilt subordinate to the silhouette and the House's mark, the tint
   rule held, and the material map complete.
6. *In game.* Inspected in the running game, near and far, under vanilla shading and with a shader on, not only in
   Blockbench.

Skins, relief and the rest of the art use lines 5 and 6 and the rubric's own lines.

Self-review once per art brief, as the repo rules say, and name the rubric line for anything under the bar (9, or 8.5
for a weapon) and the checklist line for any "no".

## 10. House art bible (PLAN-TO-1.0 task 2B.2)

Draft for Kort's review, 2026-10-30. Not approved. Everything here is either **found** (taken from a named file,
decision or commit) or marked **PROPOSED** (invented for this draft). Open decisions are collected in 10.8. Where this
section and sections 1 to 9 disagree, 10.0 says which is newer; nothing in sections 1 to 9 has been rewritten yet.

Sources: `docs/PLAN-TO-1.0.md` (2B.2, section 5, decision 7.4), `docs/DECISIONS.md`, the unit identity spec
(`docs/superpowers/specs/2026-10-17-unit-identity.md`, "spec" below, for Look, mark, Build and Portrait lines),
`sheets/design.json` (`houses`, `troops`), `tools/paint_house_troops.py` (the three stage-1 House troops; the same file
on `main` and `work/troop-models-house`), `docs/MODELS-HANDOFF.md` on `work/troop-models-house`, the hand-authored Gilded
builders on `art/knight-1x` (commits 121baca and 7bfe24e, `art/hand/**`), and Kort's memories on weapon taste, back
flags and texels.

### 10.0 Kort's fixed rules (these come first)

1. **No flags on any troop's back.** No pennant, no standard, no back banner, on any troop (Kort, 2026-10-28 and again
   2026-10-29). The `pennant` and `standard` parts stay deleted. Every "pennant" in the spec's Build lines, the `pennant`
   column in `sheets/design.json` (still `True` for all 30 troops) and spec rule 2's "back pennant" are superseded.
2. **House identity comes only through accents** on shield, weapon, clothing, tabard, trim and plume. No glyph-on-a-pole,
   no back banner, no aura colour does the House's job.
3. **Side cloth carries the player colour; fixed paint carries the House colour** (PLAN 2B.2). Team parts (tabard, kilt,
   crest, headband, wraps, fin) are drawn from the side's cloth sheet; bordures, stitching, cuffs and grips are House
   paint on the skin or weapon.
4. **Souls-like game art**, read at Minecraft scale: not museum-historical, not a generic Minecraft-mod look. Kort kept
   38 of 42 weapon references from fantasy game art (mostly Elden Ring and Dark Souls) and none from mods; for armour,
   game art first with "a little more historical plate where the House is knightly (Gilded, Graveborn)". Each tier is
   visibly grander than the last (memory `kort-weapon-taste`).
5. **Texel density.** New art is 3 texels per model px (Kort, 2026-10-24, `DECISIONS.md`); existing art stays as built;
   faces are simple Minecraft-style faces at 2 texels per px with four variants. Sections 6, 7.1 and 9 above still say 1
   texel on skins and 2 on weapons: that is the older rule for existing art, not the rule for new work.

**Conflict, not resolved here.** Commit 121baca on `art/knight-1x` ("Hand-author the Gilded troops and their weapons at
one texel per pixel", 29 Oct) rebuilds all six Gilded troops and five weapon families (plus the sun staff in 7bfe24e)
by hand at **1 texel per model px**, from `art/hand/<troop>/build_<troop>.py` and `art/hand/common/handlib.py`. On
`main` the knight is a generated 256 x 128 sheet at density 3 with faces at density 2, count 4
(`art/troops/knight.json`); the three House troops on `work/troop-models-house` are also density 3 with four faces at 2.
The 1x set departs from the approved rules in three ways:
- density 1 for skins and weapons, against 3 for new art and 2 for the accepted weapons;
- ramps of 6 and 7 tones (`STEEL`, `GOLD` in handlib), against section 1's limit of 5;
- one face per troop (`faces: {"count": 1}` by default in handlib), against four variants at 2 texels.
It may set a newer standard (it is the later work and is Kort's own branch), or it may be a Gilded-only exception.
Question 1 in 10.8 asks which. Until Kort answers, every House below is written for 3 texels per px, and the Gilded
section describes both the generated (`main`) and the hand (`art/knight-1x`) sets.

### 10.1 What every House section covers

Silhouette language; palette (hex, found unless marked); materials; signature parts (the spec's **mark** for each
troop); tier I, II and III language under decision 7.4 ("gear and trim progression on the same silhouette, plus a
distinct helmet or weapon where designed; no full new silhouette per tier"); faces; portraits (spec rule 8: standard is
20 model px chest-up, yaw 30, camera 5 degrees below eye level; "Tall +N", "Wide N", "Beast N" variants); weapon family
(spec Build lines); and the baseline with its known flaws.

**The common quality bar** (PLAN 2B.2, found): layered plate with rivets and seams, chainmail under plate, patterned
heraldic cloth, fur and leather trim, ornate crests and plumes, four to five tone worn metal, and a clear dark-to-light
read. Kort's verdict on the stage-1 House troops: the general design is right, but they look cheap next to high-detail
CurseForge knight sets.

**House accents (found, with a conflict).** PLAN 2B.2 names the House accent colours: ember (Ironclad), ochre
(Wildmoot), sea blue (Tidecallers), gold (Gilded), pale violet (Graveborn). The `accent` values in `sheets/design.json`
are:

| House | design.json `accent` | PLAN 2B.2 accent | painter ramp (tones 0 to 4) |
|---|---|---|---|
| Gilded | `#DFCE68` | gold | gilt `#482E16 #6F5026 #96783A #BDAA6A #E4D780` (section 1) |
| Graveborn | `#9DBF9E` (grey-green) | pale violet | none yet |
| Wildmoot | `#C9853A` | ochre | `OCHRE #3E2208 #683E14 #8E5C20 #B07C36 #CC9C5E` |
| Ironclad | `#E0703A` | ember | `EMBER #2E0E08 #52190E #7A2814 #9A3C20 #B85E40` |
| Tidecallers | `#4FA89A` (teal) | sea blue | `SEA #0B1528 #122848 #1A3F6A #2A5E8A #5888AA` |

The Graveborn and Tidecallers rows disagree (Question 2). The painter keeps the House accents dark ("dark enough to
keep its hue") so they pass the tint rule against the side hues (gold 42, crimson 3, teal 177, violet 264). The hand
Gilded set's side list is gold, violet, crimson, teal, company and linen (`handlib.SIDES`).

**Shared materials across Houses (found, `paint_house_troops.py`):**
`IRON #1B1D21 #30343A #4C5158 #6F757D #A3A9B0`, `MAIL #16181C #2A2E34 #454A51 #686E76 #959BA3`,
`LEATH #20160E #372619 #533A26 #725337 #937355`, `BONE #7E7664 #A49C86 #C6BEA6 #DDD7C3 #F0ECE0`;
weapon steel `#8C9096`, weapon wood `#6A4E34`, dark wood `#4E3A28`.

---

### 10.2 Gilded Order

Troops (`design.json`): Order Squire `knight` (C), Order Archer `archer` (C), Templar `sentinel` (R), Order Longbow
`longbow` (R), Sunweaver `mage` (E), Lord Marshal Aldric `gl_marshal` (L).

**Silhouette language (found, spec).** "Upright and pale: crests, plumes, white surcoats, painted shields, faces shown."
Upright, heroic scale. Squire upright, shield forward; Templar a tall white block (body [1.05,1.1,1.05], legs 1.15);
Archer slight and hooded; Longbow narrow with a tall quiver; Sunweaver a robed triangle; Marshal broad (body
[1.15,1.05,1.1]). The hand set: "the quiver is the silhouette" (archer), "the hat and the quiver are the silhouette"
(longbow), now that no flag rides on the back.

**Palette (found).** Spec Look lines: enamel `#D9D3C1`, steel `#7C8591`, gilt `#96783A`, linen `#BFB59A`, yeoman brown
`#6B5136`, sun-white `#F2E6C2`, blackened steel `#2E3236`. Section 1 ramps (steel, gilt, wood, stained wood, linen,
leather, bowstring). Shipped skins on `main` sample to `#373B43` and `#BEC6CE` (knight steel), `#BFB59A #A39A80 #DDD5BE`
(archer linen), `#2E3236 #1E2124 #96783A #D9D3C1` (marshal). Hand set ramps (`art/knight-1x`, `handlib.py`, darkest
first): `STEEL #15161F #282C3B #414A5D #617086 #8A97AB #B9C4D1 #EEF3F6`, `GOLD #2C1909 #583513 #89581B #B8852A #DCB145
#F2D877 #FFF5C4`, `MAIL #121319 #23262F #373C4A #525A6B #737D90 #9BA5B5`, `LEATH #1C100B #362114 #54341E #764B2D #9A6A42
#C09068`, `LINEN #3A3024 #675641 #958262 #BFAC86 #DDCFAA #F5EDD8`; the hand weapons ease `GOLD`'s middle lights to
`#96783A #BDAA6A #E4D780` under the tint rule (`build_swords.py`).

**Materials (found).** Polished steel plate, mail, white enamel and linen surcoats, quilted gambeson, leather, gilt
trim; blackened plate for the Marshal; white robe for the Sunweaver. Kort's armour picks lean more historical for this
House (two of his only Minecraft-mod picks, `gilded-armour-07` and `-12`, are Gilded).

**Signature parts (found, spec marks).** Squire: team shield with a white pale. Archer: hood point and fletching over the
shoulder. Templar: flat-topped great helm over the long tabard. Longbow: the bow rising past the head. Sunweaver: a 6 x
6 sun disc on the staff, 4 px above the head. Marshal: his standard, which is now **deleted** under rule 1; his mark
becomes the gilt laurel helm and the sun shield (PROPOSED; Question 4).

**Tier language (found, hand set docstrings, `art/knight-1x`).**
- Squire (`main` json): I kettle helm over a mail coif, short split surcoat; II pauldrons, breastplate, short tassets;
  III crest and folded cape.
- Templar: I initiate in mail and plate; II full harness, comb, visor snout, cape; III gilds it all, a gilt cross over
  team mantling, filigree on the cloth borders.
- Archer: I levy kit; II mail tippet over the hood's cape, sun stamped on the jerkin, steel-capped bracers, boot cuffs;
  III team-dyed hood edged in gilt, studded brigandine jerkin, gilt belt and quiver bands, signal horn.
- Longbow: I felt cap with peak and tail; II steel kettle hat, mail sleeves, jerkin per pale team and white with the
  sun, white sash, steel splints; III gilt-rimmed kettle with a long white feather, gilt studs, sash, bands and cuffs,
  steel knee caps.
- Sunweaver: one armour tier; Mastery II gilds the robe's hems. Marshal: one stage.
- The rule the Gilded set follows, and every House should (PROPOSED as a cross-House rule): I is steel and cloth, II adds
  plate pieces and the House emblem, III spends gilt (or the House's precious accent) and adds one crest-level part.

**Faces (found).** `main`: four variants at 2 texels per px (`knight.json`). Hand set: one face per troop at 1 texel,
painted from ASCII grids (young clean-shaven Squire; grey-bearded Sunweaver and Marshal with beard boxes, a scar on the
Marshal). Archer's face in shadow; Templar closed helm with a painted slit.

**Portraits (found, spec).** Squire, Archer: standard. Templar, Longbow: Tall +2. Marshal: Tall +4. Sunweaver: Tall +6.

**Weapon family (found).** `arming_sword` (Squire), `longsword` (Templar), `order_bow` (Archer), `longbow` (Longbow),
`sun_staff` (Sunweaver), `marshal_sword` (Marshal); three tiers each in the hand set; painted shields `white:cs` and
`white:sc` (`design.json`), the Marshal's `white:sc,light_gray:bo`. Sections 7.2's arming sword and Order bow are the
approved 2x constructions; the hand set rebuilt them at 1x.

**Baseline and known flaws (found).**
- Not part of the 8/10 stage-1 batch. On `main`, four troops have tier I to III skins; the Sunweaver and Marshal have
  tier I only (PLAN section 1).
- 121baca: "The new bows, longbowman and sunweaver have not been checked in game." 7bfe24e then installed the sun
  staff's three tiers (quads cut from 159 and 224 to 99 and 115 against the 128 cap).
- The troop notes on `main` still mention the pennant (MODELS-HANDOFF).
- The Marshal's design hinged on a back standard that rule 1 removes (Question 4).
- Density, ramp and face conflicts listed in 10.0.

---

### 10.3 Ironclad

Troops: Hearthguard `ic_hearthguard` (C, built stage 1), Arbalist `ic_arbalist` (C), Shieldbreaker `ic_breaker` (R),
Ballista Crew `ic_ballista` (R), Ironhold Mortar `ic_mortar` (E), Brassjaw, the Forge Colossus `ic_colossus` (L).

**Silhouette language (found, spec).** "Wide and square, arms folded at rest, beards, engines on the back." Body scale
about [1.2,0.95,1.15], legs [1.1,0.8,1.1]: broad, short, low. Folded arms at rest (Hearthguard, Shieldbreaker). The
Arbalist's crossbow is level, every other bow upright. Engines (bolt case, ammo crate, mortar barrel, smokestacks) ride
the back or shoulder; they are equipment, not flags.

**Palette (found).** Spec: soot iron `#34373B`, brass `#7F6232`, copper beard `#7A4A2E`, scorched leather `#4B3A2C`, oak
`#6A4E30`, ember `#E2702A` ("a light only": a few lit pixels, glow). Painter ramps: `SCORCH #1A1714 #2D2823 #47403A
#655B51 #8C8277` (stage 1 iron), `IRON` (stage 2 and 3), `BRASS #3E2B12 #664A20 #8E6C36 #B3905A #D6BE8E`, `COPPER #35190C
#582C16 #7A4220 #A05E2E #C08248`, `EMBER` (10.1).

**Materials (found).** Soot iron and scale coat, riveted plate, mail sleeves, brass fittings, leather belts, copper
braided beards, oak frames; ember as glow only.

**Signature parts (found, spec marks).** Hearthguard: layered slab pauldrons (slab, two lames, brass boss, spike).
Arbalist: the level crossbow. Shieldbreaker: a 6 x 4 x 6 anvil on the right shoulder. Ballista Crew: the two-wheeled
frame. Mortar: the ember-lit barrel mouth over the shoulder. Brassjaw: twin smokestacks.

**Tier language.** Found for Hearthguard (`hearthguard_mats`): I scorched iron, iron rivets, scorched scale coat; II
clean iron, brass rivets, brass comb, brow and nasal, brass rims on the pauldrons; III brass rims on all plate, brass
edging on the scales, two short bone helm horns. PROPOSED for the other five: the same ladder (scorch, then clean iron
and brass rivets, then brass rims and one horned or crested part), with the spec's stage-3 parts (Arbalist brass shoulder
plate, Shieldbreaker forge apron, Ballista brass crest on the frame).

**Faces (found, Hearthguard `FACE_SETS`).** Skins `#B07E62 #9C6A50 #B88E74 #A27258`; heavy brows, a square beard over the
lower face, four variants (copper, dark brown, grey, red-brown). Spec: Arbalist short beard and brass goggles,
Shieldbreaker shaved head and black beard, Mortar scorched beard, Brassjaw a sunken head with an ember eye slit.

**Portraits (found, spec).** Hearthguard, Arbalist, Shieldbreaker: standard. Mortar: Tall +6. Ballista Crew and
Brassjaw: Wide 28.

**Weapon family (found).** `hearth_axe` (I ash haft, ember leather grip, soot bearded blade, bright bit, back spike; II
brass bands, longer back spike; III brass socket, gilt rune plate, crown and butt spikes), `arbalest` held level,
`great_axe` (Shieldbreaker); the Ballista and Mortar are engines with no weapon model; Brassjaw has none. Shield
`black:bo,light_gray:mc`. Family language PROPOSED: bearded axes and heavy iron, wedge-shaped, brass bands as the tier
cue; no Order guard or limb profile.

**Baseline and known flaws (found).** Hearthguard stage 1, self-scored 8/10 against a 9 gate, not accepted. Its folded
arms hide most of the front glyph (the back glyph shows). Its shield is the server's off-hand vanilla shield and is not
accented. Only the gold side and stage 1 were rendered; tiers II and III, other sides, portrait and face close-ups are
unverified (MODELS-HANDOFF). Phase2Checks cull aim was built around the tall standard and needs a game re-check.

---

### 10.4 Wildmoot

Troops: Moot Hound `wm_hound` (C, beast), Hide Hunter `wm_hunter` (C), Web Stalker `wm_stalker` (R, spider), Tusk Spear
`wm_tusk` (R, built stage 1), Bloodhowl Seer `wm_seer` (E), Old Horn, the Antlered Alpha `wm_oldhorn` (L, bear).

**Silhouette language (found, spec).** "Snouts, ears, low four-legged bodies, antlers and feathers; fur and hide, no
metal." Boar-kin bipeds are hunched (hunch 20, the Seer 30); beasts are low and long. Antlers belong to Wildmoot only
(the Barrow Stalker was denied them).

**Palette (found).** Spec: russet `#7A4127`, bark `#4A3626`, clay `#C8B48A` (warpaint is pale clay, not ochre), chitin
`#23291F`, bone `#C9C1A8`, grey fur `#7D7468`, dried blood `#6E2A22`, grey muzzle `#8A8378`. Painter ramps: `FUR #24170E
#43291A #673F25 #885836 #A97650`, `HIDE #2A1C12 #46301E #6A4A30 #8A6646 #A88660`, `OCHRE` (House accent, 10.1), `BONE`,
`LEATH`; hoof `#1A1410 … #4E4034`; snout `#2A1A14 #3A2620 #5E4034 #7E5A4A #9C786A`.

**Materials (found).** Fur, hide, boiled leather, bone and tusk, clay paint; iron only at the cap bar and the spear
socket. "No metal" (spec) and the painter's iron cap bar on stage 2 and 3 slightly disagree (PROPOSED: allow a single
iron rivet line, nothing more).

**Signature parts (found, spec marks).** Hound: a clay stripe nose to tail. Hunter: a three-feather fan on the left of
the head (five at stage 3). Web Stalker: a white bone mask over the eyes. Tusk Spear: the crossbarred boar-spear head
above the cap. Seer: an antler crown of two 6 px tines. Old Horn: the 16 px antler rack.

**Tier language.** Found for Tusk Spear (`tusk_mats`): I leather bracers, plain cap; II scorched iron bracers,
ochre-stitched cap, iron cap bar; III iron bracers and a crown of three boar tusks. PROPOSED for the rest: more bone and
trophy per tier (a collar, then teeth, then antler or tusk), never plate; beasts gain antler tufts or mandibles as the
spec's stage-3 parts.

**Faces (found, Tusk `FACE_SETS`).** Russet and bark skins, bristled brows, small dark eyes, four variants; big white
tusks as parts. Spec: Hunter clay brow stripes; Seer grey muzzle and clay-glow eyes; Hound dark muzzle; Old Horn
scarred muzzle.

**Portraits (found, spec).** Hunter, Tusk: Tall +4. Seer: Tall +6. Hound: Beast 16. Web Stalker: Beast 18, yaw 20. Old
Horn: Beast 24.

**Weapon family (found).** `hide_bow` (Hunter), `boar_spear` (Tusk: I ash shaft, ochre grip, leaf head over an iron
crossbar, clay tassel, "one head, never three"; II collar and longer lugs; III tusk lugs and a second tassel),
`bone_wand` (Seer); Hound, Web Stalker and Old Horn have no weapon. Family language PROPOSED: wood, sinew, bone and
leaf-shaped iron points, tassels and tusk lugs as tier cues; no Order recurve.

**Baseline and known flaws (found).** Tusk Spear stage 1, self-scored 8/10, not accepted. Its kilt floats in side views. The
front panel stands 1.5 px proud so the hunch does not sink it into the legs (b9884ff); whether that is the cause is not
checked. Only the
gold side and stage 1 rendered. No shield in this House, so "unaccented shields" does not apply.

---

### 10.5 Tidecallers

Troops: Drowned Raider `tc_raider` (C), Harpooner `tc_harpooner` (C), Trident Guard `tc_tideguard` (R, built stage 1),
Mist Caller `tc_mist` (R), Kraken-touched Brute `tc_brute` (E, ravager), Admiral Vael, the Drowned Tide `tc_admiral` (L).

**Silhouette language (found, spec).** "Dripping: kelp trailing from limbs, bleached coral, tridents, hoods." Trident
Guard upright and long-legged (legs 1.05); Raider leans forward with kelp streaming; Mist Caller a robe to the ground;
Admiral a tricorne and calf-length coat tails. Coral belongs to the Trident Guard (the Raider is denied it).

**Palette (found).** Spec: verdigris `#4E7A6A`, barnacle `#CFCBBE`, navy `#1E2C44`, sea-foam glow `#CFF3E4`, gilt
`#96783A` (Admiral). Painter ramps: `VERD #16292A #24433F #3A6458 #5E8A7A #8EB2A2`, `BRONZE #2E200E #4E3818 #74562A
#98764A #BCA070`, `SEA` (House accent, 10.1), `CORAL #857A6A #A99F8E #C7BFAD #DCD6C6 #EEEAE0`, `NAVY #0E1320 #18202F
#232E42 #33425A #4E6078`, drowned skin `SKIN_TIDE #3A4A44 #50645C #6A8278 #84A094 #A2BAAE`; barnacle `#CFCBBE`.

**Materials (found).** Verdigris bronze scale, barnacled mail, bleached coral, shell, driftwood, kelp, oilskin, navy
cloth, drowned skin.

**Signature parts (found, spec marks).** Raider: the green turtle-shell helmet. Harpooner: the sou'wester, brim long at
the back. Trident Guard: coral rising from each shoulder. Mist Caller: a pale barnacle rune band round the hem. Brute:
four 8 px tentacles from the jaw. Admiral: the 12 px tricorne.

**Tier language.** Found for Trident Guard (`tide_mats`): I verdigris scale, plain mail; II sea-blue rims on cuirass,
cap, bracers and greaves, more barnacles on the mail; III verdigris mixed toward warm bronze and a crown of coral sprigs.
PROPOSED for the rest: the sea takes more of them each tier (more barnacle, more kelp, a shell or coral part at III),
with the spec's stage-3 parts (Raider shell barnacles, Harpooner tooth necklace, Mist Caller mist strips).

**Faces (found, Tideguard `FACE_SETS`).** Grey-green skins `#6F8A80 #7C8E86 #64807A #829A8C`, gill slits on the sides,
pale or dark eyes, four variants. Spec: Raider bloated with white eyes; Mist Caller grey face with sea-foam eyes; Admiral
pale with hair adrift.

**Portraits (found, spec).** Raider, Trident Guard, Mist Caller: standard. Harpooner: Tall +2. Admiral: Tall +4. Brute:
Wide 28 from the front left.

**Weapon family (found).** `cutlass` (Raider), `harpoon` (Harpooner), `coral_trident` (Trident Guard: I driftwood shaft,
sea-blue grip, verdigris collar, three coral prongs with steel tips; II coral barbs and a verdigris band; III a pearl
`#D8DCD4` at the fork and middle barbs), `admiral_trident`; Mist Caller and Brute have none. Kort's weapon picks
include Sea of Thieves cutlasses (memory). Family language PROPOSED: three-pronged or barbed, coral and bone over
driftwood, verdigris collars as the tier cue.

**Baseline and known flaws (found).** Trident Guard stage 1, self-scored 8/10, not accepted. Its bronze cuirass blends
into its mail (both mid-value green-grey: `VERD` against `MAIL` mixed 45% toward `VERD`). Only the gold side and stage 1
rendered. The scallop shell sits on the back; it is armour, not a flag, but it is the one back part in a House that also
plans an anchor on the Admiral's back (Question 5).

---

### 10.6 Graveborn

Troops: Risen `revenant` (C), Bone Archer `bonebow` (C), Grave Reaper `reaper` (R), Barrow Stalker `stalker` (R),
Gravecaller `hexer` (E), The Hollow King `gb_hollowking` (L). Nothing built: PLAN section 5 batch 4, "six from scratch;
the undead silhouettes are the most distinct and should not reuse living armour".

**Silhouette language (found, spec).** "Hunched or gaunt, ragged hems, bare bone, no shields; the casters carry light."
Risen stooped with arms forward (hunch 15); Bone Archer a dark hood over stick legs (2 x 12 x 2 limbs); Reaper tall and
hooded; Barrow Stalker a blue-grey wedge; Gravecaller a robe to the ground; Hollow King tall and broad, cloak dragging.

**Palette (found, spec Look lines; no ramps exist yet).** Grave-moss `#4F5A3C`, rust `#6B4A33`, bone `#C9C1A8`,
grave-iron `#2B2826`, frost grey `#93A7AE`, rime `#DCE3E2`, soul-light glow `#CFEFFF`, gilt `#96783A` (Gravecaller's
death mask, Hollow King's jaw wire). Ramps are to be derived from these bases by section 1 at build time (PROPOSED);
none is invented here. House accent: pale violet (PLAN) or `#9DBF9E` (`design.json`), Question 2.

**Materials (found).** Rusted mail and coif, rotted cloth in strips, bare bone, black grave-iron plate, fur cowl and rime
(Stalker), burial robe and candle stubs, gilt only as funeral metal. Kort's armour picks lean historical for this House
too (memory).

**Signature parts (found, spec marks).** Risen: the stoop under a shredded tabard. Bone Archer: a skull pauldron on the
bow shoulder. Reaper: a 10 px curved blade over the hood. Barrow Stalker: the knee-length mantle. Gravecaller: the lit
lantern at arm's length. Hollow King: the lit ribcage under a broken crown.

**Tier language (found as spec stage-3 parts; the rest PROPOSED).** Risen III rusted visor and half-cape; Bone Archer III
one lit eye socket; Reaper III spine plates; Stalker III frost strips. PROPOSED ladder: I rags and rust, II salvaged
grave-iron plate over the rags and the House accent on hems and bindings, III black plate with funeral gilt and one
soul-light point (eye, socket or lantern) within the glow cap.

**Faces (found, spec; PROPOSED variants).** Risen grey-green, slack jaw; Bone Archer bare skull; Reaper skull deep in the
hood with two lit eyes; Stalker rimed skull; Gravecaller gilt death mask; Hollow King jaw bound in gilt wire. Four
variants per Common and Rare at 2 texels (rule 5); PROPOSED: skull variants differ by a missing tooth, a crack or a jaw
set, not by colour.

**Portraits (found, spec).** Risen: standard, head dipped. Bone Archer, Stalker, Gravecaller: standard. Reaper, Hollow
King: Tall +4.

**Weapon family (found).** `notched_sword` (Risen), `bone_bow` (Bone Archer), `scythe` (Reaper), `frost_bow` (Stalker),
`lantern` (Gravecaller, 4 x 6 px, lit), `grave_sword` (Hollow King). No shields. Family language PROPOSED: notched,
pitted, broken and bound; bone and grave-iron, Souls-like greatsword and scythe silhouettes; tier cue in bindings, gilt
wire and soul-light.

**Baseline and known flaws (found).** No House-specific model; the old shared skins remain. Phase 2 review (archive
progress.md) found Graveborn and Ironclad attacks looked alike. Balance runs place Graveborn at 17.5 percent against
Gilded (PLAN section 1), which is not an art flaw but sets how menacing the House should read (PROPOSED).

---

### 10.7 Cross-House checks (PROPOSED)

- A greyed-out lineup of the five Houses' Commons reads as five Houses at 36 px: upright (Gilded), square (Ironclad),
  hunched beast-kin (Wildmoot), upright with growths (Tidecallers), stooped bone (Graveborn).
- Every troop's front team panel is visible in its rest pose (the Hearthguard flaw, generalised).
- Every House shield (Gilded, Ironclad) carries the House accent on its rim or boss.
- No back part of any troop is cloth on a pole. Quivers, engines, shells and anchors are allowed; check each against
  rule 1 in the troop brief.
- Plate and the layer under it differ by at least one full tone step (the Tideguard flaw, generalised).
- Cloth parts sit no more than 0.5 px proud of what they hang on, in every view (the Tusk kilt flaw, generalised).

### 10.8 Questions for Kort

1. **Texel density.** Does the 1-texel hand-authored Gilded set (121baca, 7bfe24e on `art/knight-1x`, with 6 and 7 tone
   ramps and one face per troop) replace the 3-texel rule for new art, or is it a Gilded-only exception? It decides the
   density, the ramp length and the face count for the other 24 troops.
2. **Accent colours.** Graveborn's accent in `design.json` is grey-green `#9DBF9E`, but your rule says pale violet;
   Tidecallers' is teal `#4FA89A`, but your rule and the painter use deep sea blue. Which wins, and should the sheet
   change? Pale violet also sits near the violet player side; is a dark, desaturated violet acceptable?
3. **Banners that are not on the back.** The spec gives Old Horn a team war-banner strung between his antlers and
   Brassjaw a team banner from belt to knee. Are these allowed under the no-flag rule, or should they become clothing
   (a hide mantle, an apron)?
4. **Lord Marshal Aldric's mark.** His mark was the standard, which is now gone. Is the gilt laurel helm with the sun
   shield enough, or does he need a new signature part?
5. **Back equipment.** Quiver, bolt case, mortar barrel, smokestacks, the Seer's totem staff, the Trident Guard's
   scallop shell and the Admiral's anchor all ride the back. Confirm they are allowed as equipment, not flags.
6. **Spec pennant column.** The `pennant` column in `sheets/design.json` is still `True` for every troop. May an agent
   set it false (a sheet change through `tools/preflight.py`), or does it stay for code that has not been removed?
7. **Wildmoot metal.** The spec says "no metal"; the built Tusk Spear has an iron cap bar and iron bracers at II and III.
   Keep that small allowance or strip it?
