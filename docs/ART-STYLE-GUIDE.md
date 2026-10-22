# Art style guide: troops and weapons

Draft 2, 2026-10-22, for Kort's review: the weapons section (7) carries the weapon look round's outcome (questions
rounds 28 and 29). This is the rulebook for every troop skin, relief layer and weapon model. It
replaces our use of the top-down pixel-art skill for troops and weapons (that skill stays for icons, tiles and UI art).
The approved look (War Table and Iron Ledger) and `work/ui-concepts/GRADING-RUBRIC.md` always win over anything here.

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
  Gilt never covers more than 20% of a troop's visible texels, or 25% of a weapon's rendered pixels; it is trim.
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

A weapon is hand-shaped cubes. The recipe below is the weapon look round's (the Order Squire's arming sword and the Order
Archer's bow at tiers I to III, in `bannerbound-p2b/work/p2b/look-round/`, graded 8 out of 10 by an independent
reader), as Kort ruled on it in questions rounds 28 and 29. Those six files are the worked examples.

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

**Bows.** A limb is five staves as one continuous recurve, each stave about 1.9 to 2.3 px long, the leans stepping from
about -6 degrees at the grip to about -44 on the fourth stave and the fifth turning back to the tip. The profile, grip
back to string, is about a fifth as deep as the bow is tall. Depth tapers from 2.0 px at the grip to 1.2 px at the tips
(Kort's 2 px limb ruling holds at the grip). No tip is flatter than -45 degrees. This replaces the old chain of three
cubes a limb. A stave runs back past its joint by half its width times the sine of the turn, plus 0.05 px, so a joint
shows no notch or dashed seam. Where gilt rides on the limbs (tier III) they are stained wood. The string is a 0.4 px
cube at tone 2; on a recurve it may lie over the knee by about 0.2 px, as on a strung bow. The pull frames flex the limbs
4 degrees a frame and bring the string back 0.75 px a frame (Controller's call; `p2-depth-bow-nock` tunes it); a pull
frame draws the string in two halves that meet at the draw point.

**Swords.** A sword is about 1.5 blocks long and at most 24.5 px (Kort's ruling, round 28): a 13 px blade and a 3 px
point on the guard, the blade at least 7 times its base width.
- The blade tapers by turned cubes, not steps: each flat is one cube about 0.6 px wide, turned about 1.6 degrees about z on
  its outer base corner, so the outline narrows from 2.0 px at the guard to about 1.3 px at the top. The core stays
  upright, a recessed fuller (a proud spine on tier I). The blade is 1.0 to 1.3 px deep.
- The point is clean: two small edge cubes, turned about 7 degrees, bring the flats' edges onto the point core, and a
  0.45 px tip cube turned 45 degrees and 0.6 px deep caps it. No cube crosses the centre line (x 0), which would draw an X
  and z-fight its twin.
- The guard and pommel are sized to the 4 px fist, and grow by tier. Tier I: a 9 px bar 1 px tall with end blocks over
  a seat block (2.5 px clear of the fist a side), and a 1.8 px wheel pommel. Tier II: quillons reaching 4.5 px, swept 22
  degrees up toward the blade, with 1.2 px gilt finials, and a 2.2 px wheel pommel with a gilt boss. Tier III: a straight
  cross reaching 5 px, a 3 px steel boss at the blade root (no wider than the blade plus 1 px a side) with the Order's
  sun painted on its faces, and a 3 px sun pommel.
- A grip is wound: a thin core plus bands, in whole pixels (above).

**Where tier cues go.** On the parts the fist does not cover. The fist is a 4 px box at the grip's origin and hides y -2
to +2, so a tier shows on the guard, the pommel, the boss at the blade root, and on a bow the collars (from y 2.5), the
limbs, the nocks and the tips. A grip carries its tier's material (leather, linen wire, enamel) for views without a hand,
but it is not the cue. Judge every weapon with the fist box in view, at every size, painted at skin density (1 texel per
px, from an Order troop's arm) so the hand's finer neighbour shows.

**Gilt.** At most 25% of a weapon's rendered pixels, measured on the 45-degree view at 1.5 blocks, bare and in the fist,
the fist's own pixels not counted. Gilt pops by value contrast against dark leather, stained wood or steel, never by more
saturation; the gilt clamp (section 1) stays.

**Paint, in brief.** Blade tones: 4 on the lit flat, 3 on the shade flat, 2 in the fuller (section 3). Wood grain is one
line along the limb (section 4). The string is at tone 2. Limbs that carry gilt are stained wood (section 1).

Thickness: a blade 1 px at the core, a bow limb 2 px at the grip (Kort's ruling), a staff 2 px.

Every weapon has a `rest` group, from which the converter builds a far version of at most 32 quads for troops outside the
near set (Controller's call). In a `.bbmodel` only `rest` is visible on open; the `pull_*` groups are saved hidden and are
still frames (art pipeline v2 section 2.1).

Every face has its own texels on the weapon sheet (64 x 128), painted by sections 1 to 4, at 2 texels per model px.

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

Self-review once per art brief, as the repo rules say, and name the rubric line for anything under the bar (9, or 8.5
for a weapon).
