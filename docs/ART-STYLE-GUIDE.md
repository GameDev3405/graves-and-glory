# Art style guide: troops and weapons

Draft 1, 2026-10-21, for Kort's review. This is the rulebook for every troop skin, relief layer and weapon model. It
replaces our use of the top-down pixel-art skill for troops and weapons (that skill stays for icons, tiles and UI art).
The approved look (War Table and Iron Ledger) and `work/ui-concepts/GRADING-RUBRIC.md` always win over anything here.

The guide is written for the Minecraft 3D case: flat faces on boxes, 1 texel per model pixel, lit by the game's own
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

The four Gilded ramps (hex, tones 0 to 4):

| material | 0 shadow | 1 dark | 2 base | 3 light | 4 highlight |
|---|---|---|---|---|---|
| steel `#7C8591` | `#3A3E4B` | `#5A616E` | `#7C8591` | `#A6ADB4` | `#D6D6D7` |
| gilt `#96783A` | `#482E16` | `#6F5026` | `#96783A` | `#BDAA6A` | `#E4D780` |
| wood `#6E5440` | `#3E261F` | `#563C2E` | `#6E5440` | `#867057` | `#9E8E72` |
| linen `#BFB59A` | `#82715E` | `#A0937B` | `#BFB59A` | `#DED9C2` | `#FCFAEF` |
| leather `#5A3E2C` | `#321A15` | `#462B20` | `#5A3E2C` | `#6E553D` | `#826E52` |
| bowstring `#D9D3C1` | `#9C8F7E` | `#BBB19F` | `#D9D3C1` | `#F7F6ED` | `#FFFFFF` |

Each House adds its own base colours from its Look lines (bone, verdigris, rust, kelp, hide) and derives its ramps the
same way. The ramp file is `art/ramps.json` once the build tool carries it; until then, run `paint.py` in the spike. It also maps each ramp to a material, which is how the build finds a troop
texel's material (Controller's call).

## 2. Light from the top left

One light, above and to the left of the viewer, slightly in front. Everything follows from it:
- a highlight (tone 4) sits on the top-left edge of a form, one texel wide, never a full face;
- the shadow (tone 0) sits on the bottom-right edge and under overhangs (a brow, a pauldron, a guard, a belt);
- a cast shadow is at most one texel wide and only under something that really overhangs;
- a glow (`_e`) is the only thing lit from inside.

## 3. Face brightness

Paint each face of a box at the tone its direction earns, then put the material pattern on it:

| face | tone | why |
|---|---|---|
| up (top) | 4 | faces the light |
| north (front) | 3 | the viewer's face, carries the pattern |
| west (the viewer's left) | 3 | toward the light |
| south (back) | 2 | base |
| east (the viewer's right) | 1 | away from the light |
| down (underside) | 0 | shadow |

On a troop, "front" is the face toward the enemy; on a weapon drawn upright, it is the flat the viewer sees. Faces the
game darkens anyway (up and down by 0.5 and 0.6 in vanilla's entity shading) still get their own paint: the paint
reads under shaders and in portraits, where vanilla shading is weaker.

Polished metal is the one exception: the big faces of a blade are painted one tone up (north at 4, south at 3), since a
flat blade at tone 3 reads as pewter at six blocks (spike, row 1). Keep the Look line's steel, `#7C8591`, as tone 2 (Controller's call).

## 4. Material patterns

Each material has one pattern, drawn in the face's own tone ±1. No material has two patterns, and no face has noise.

- **Wood:** grain along the long axis. Every fifth row along the grain is one tone down; every fifth row on the
  light edge carries a one-texel fleck a tone up. On a limb 1 px wide the grain is a tone change every few px, not a
  line.
- **Steel (polished):** sheen. The column nearest the light is tone 4, the column away from it one tone down; a
  one-texel hammer mark one tone down every fifth row down the middle. Rims and edges get the tone-4 edge highlight.
- **Iron (dull plate, mail):** as steel but on tones 1 to 3, and mail is a two-texel check of tones 2 and 1.
- **Gilt and brass:** flat tone 3, the top-left texel (two on a wide face) tone 4, the bottom-right texel tone 1.
  Gilt never covers more than 20% of a troop's visible texels; it is trim.
- **Cloth (linen, wool, a surcoat):** the weave is a check of tones 2 and 3 on the big faces; a wound wrap (a grip, a
  bandage) is bands across: every other row one tone down.
- **Leather:** flat tone 2 with a scattered pit (one texel, tone 1) every seventh texel, in a diagonal so it never
  reads as a grid.
- **Bone:** linen's ramp made of ivory, with a crack line (tone 1) along the long axis, never more than one per face.
- **Bowstring:** a single tone-3 line, 0.4 px thick in a model, 1 px in a sprite.

## 5. Worked examples

**A 2 x 9 blade face (north), steel.** Fill tone 4 (polished rule). Left column stays tone 4, right column tone 3.
Rows 2 and 7 get a tone-3 mark in the middle. Result: a bright blade with a cool right edge, read at six blocks as
"steel, lit from the left".

**A 1 x 7 bow limb face (north), wood.** Fill tone 3. Rows 2 and 7 tone 2. Row 4 tone 4 fleck. The east face of the
same limb: fill tone 1, rows 2 and 7 tone 0.

**A 4 x 4 belt buckle on a relief layer, gilt.** Tone 3 fill, top-left two texels tone 4, bottom-right texel tone 1.
Relief side faces take the texel's colour (design 3), so nothing else is painted.

**A wound grip, 1 x 4, linen.** Rows 0 and 2 tone 3, rows 1 and 3 tone 2; the collar above it gilt tone 3 with its
tone-4 corner.

**A surcoat panel on a team part.** The cloth sheet carries the field greys; paint only the weave on the greys
(tones 2 and 3 of grey), and never a hue. The team colour comes from `build_skins.py`.

## 6. Never

- Never more than 5 tones of one material, never a sixth "in-between".
- Never pure black (`#000000`) or pure white on a troop; the ramp ends are the darkest and lightest.
- Never a highlight on a whole face, and never a highlight on the bottom or right edge.
- Never a hue with saturation over 0.45 and value over 0.60 within 45 degrees of a side colour (the tint rule).
- Never dithering, gradients, noise or anti-aliased edges. Every texel is one of the ramp's tones or transparent.
- Never a texel stretched: one texel is one model pixel, on skins, relief and weapons alike.
- Never a face painted in a tone brighter than the face above it (an underside brighter than the top).
- Never a pattern on a face smaller than 2 x 2; such a face is a flat tone.
- Never team colour on a skin or a weapon; it lives only on the cloth sheet.
- Never a glow texel on a team surface, and never more lit texels than the glow cap (6, or 16 on Epics and Legendaries).
- Never a copy of another mod's texture or model, and never a vanilla item's shape re-coloured.

## 7. Weapons in Blockbench

A weapon is 8 to 16 cubes per frame (cap 21: 126 quads of the 128 the renderer allows), hand-shaped:
- the blade has a ridge (a 1 px core with two 0.5 px edges set 0.3 px back) or a fuller, never a flat slab;
- the point is a cube turned 45 degrees, or two stepped cubes;
- a guard flares: a bar plus two end knobs;
- a grip is wound: a thin core plus two bands;
- a bow is a chain of three cubes a limb (stave, stave, recurved tip), leaning 10, 25 and -65 degrees, with gilt nocks;
  the string is a 0.4 px cube; the pull frames flex the limbs 4 degrees a frame and bring the string back 0.75 px a frame ((Controller's call); `p2-depth-bow-nock` tunes it);
- thickness: a blade 1 px at the core, a bow limb 2 px (Kort's ruling), a staff 2 px.

Every weapon has a `rest` group, from which the converter builds a far version of at most 32 quads for troops outside the
near set (Controller's call).

Every face has its own texels on the weapon sheet (32 x 64), painted by sections 1 to 4, at 1 texel per model px.

## 8. LabPBR maps

Troops and weapons ship `_n` and `_s` beside their colour sheet (art pipeline v2 design, section 6). The painter paints
neither by hand. A material map `<name>_m.png` marks each texel with its material key colour (the table in that
design), and the build derives `_s` from the key and `_n` from the pattern heights of section 4. For a weapon, the painter script writes the
material map in flat key colours only. For a troop, the `_m` (`<troop>_s<stage>_m.png`, `_s<stage>_r_m.png`) is
never painted: a script generates it from the colour sheet by ramp lookup through `art/ramps.json`, and a colour that
matches no ramp fails the build (Controller's call).

## 9. Beside the grading rubric

The rubric (`work/ui-concepts/GRADING-RUBRIC.md`) grades the result at 9 for player-facing art. This guide is how the
work is made so it can score. The mapping:

| rubric line | this guide |
|---|---|
| 1 Legibility | sections 1 and 3: tone steps of 22% keep faces apart at six blocks |
| 3 Focal point | section 4: gilt is trim, one pattern per material, the mark reads first |
| 6 Colour discipline | section 1's tint rule; team colour only on the cloth sheet |
| 8 Feel | sections 2 and 4: hand-authored, one light, no noise |
| 9 Honesty | 1 texel per model px, real ramps, nothing that cannot ship |

Self-review once per art brief, as the repo rules say, and name the rubric line for anything under 9.
