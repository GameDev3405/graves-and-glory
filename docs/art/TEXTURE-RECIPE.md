# Texture recipe: new units and new Houses

For a worker texturing a NEW troop (or a new House) in the hand style: one texel per model pixel, painted by a Python builder. Kort's rule (2026-11-09): **approved troops are not repainted.** This recipe is for new work only. Each House keeps one locked palette.

Read first: `docs/art/unit-texture-audit.md`, sections 2 (the rubric) and 4 (the common failures). Skim `art/hand/common/handlib.py` (`LIT`, `lightx`, `limb`, `Troop`). The template is `art/hand/_template/build_template.py`. Every `# EDIT (Xn):` in it points to a step below.

Paths used throughout:

- `<scratch>`: your scratchpad directory. Never write previews into the repo.
- `<renderer>`: GeckoLib's offline renderer, `tools/anim/render_geo.py` on branch `anim/geckolib-rebuild`. Get a copy with `git show anim/geckolib-rebuild:tools/anim/render_geo.py > <scratch>/render_geo.py`. Do not commit it to an art branch.

---

## A. A new unit in an existing House

### A1. Pick the House palette

1. The House is in the troop's row in `sheets/design.json` (`gilded`, `graveborn`, `wildmoot`, `ironclad`, `tidecallers`).
2. Look at its ramps:
   - open `art/palettes/<house>.png`; or
   - run `python tools/art/house_palette.py show <house> <scratch>/<house>.png`.
   Ramp names are module-qualified, for example `graveborn.BONE`, `handlib.STEEL` or `ironclad_tiers.BRASS`.
3. Paint **only** from `palette.ramps(house)` / `palette.require(house, ...)`, and from the team ramps (`palette.ramps("team")`) on the cloth sheet.
   - Do not write a single new hex value. `ramp("...")` in a troop builder is forbidden.
   - If a material has no ramp, stop and ask (step A9). Do not invent one.

### A2. Copy the template

1. Copy the template into the troop's own folder:
   ```
   mkdir art/hand/<tid>
   cp art/hand/_template/build_template.py art/hand/<tid>/build_<tid>.py
   ```
2. Edit the builder:
   - set `TID = "<tid>"` and `HOUSE = "<house>"`;
   - delete the `--house=` argv loop and the other entries of `PRESETS`;
   - rename `TEMPLATE` to the troop;
   - set `meta` (`weapon`, `hand`, `idleArms`, `portrait`) from the unit identity spec;
   - write `NOTE` (one line: look, mark, tier steps).
3. Keep the biped skeleton exactly as the template builds it:
   - `limb()` joints;
   - the hand bones and `arms_folded`;
   - the `tabard` bone name, which the engine swings.

   Geometry may grow for a NEW troop, but the animation lead owns the rig contract (`handlib` `ELBOW`/`KNEE`/`HAND`, 64 bones max).

### A3. Choose a base index per material

Fill `MAT = {role: (ramp name, base index)}`.

1. One ramp per material: skin, hair, plate, mail, padding, leather, trim and accent. Two materials never share a ramp.
2. The base is the tone of a flat, front-facing, mid-height texel.
   - Painters move it by up to -3 and +2.
   - Pick a base with **at least 2 steps above and 1 below**. For a 6-tone ramp that is index 2 or 3; for a 7-tone ramp, 3 or 4.
3. Value order, from lightest to darkest:
   - head and torso;
   - arms;
   - legs, which sit one step below the torso (the template's hose uses `B("padding") - 1`);
   - boots and recesses.
4. Every troop needs **one light mass** that reads at 16 blocks: bone, rime, gilt, linen or flesh. On dark ramps (Graveborn `GIRON`, `ROT` and `MOSS`, which start near black), raise the base by one or two steps (template Graveborn preset: `GIRON` base 4). This fixes audit finding 3, crushed values.
5. Neighbouring materials must sit two or more tones apart where they touch, such as a plate over padding, or a belt over a tunic.

### A4. 3D depth: paint form, not texture

Build every painter on the template's `tone()` / `fill()`. That way the light rules hold everywhere:

1. **Face light.** Add `LIT[face]` to every face: top +1, front and right 0, left and back -1, bottom -2.
2. **Lit column.** Add `lightx(face, x, w)` on the side faces. The wearer's right (the viewer's left) is lit.
3. **Top-to-bottom light on tall faces (h ≥ 6).** Use +1 on the top sixth and -1 on the bottom quarter. On short faces, use a +1 rim on the top row and a -1 seam on the bottom row. This fixes audit finding 1, which is the biggest lever. Limbs are painted as one 12-row face (`Box cut`), so the gradient runs shoulder to wrist and hip to sole.
4. **Recesses go dark.** Use 1–2 steps down under a brim, under a belt, between the legs, and under a pauldron.
5. **Legs get a shadow band.** Each leg needs:
   - a knee crease (2 steps down at row 5);
   - an inner column, plus the inner side face, one step darker;
   - a lit boot cuff;
   - a darkest sole.

   The template's `leg(side)` does this, and no leg ships flat. This fixes audit finding 4.
6. **Form, not noise.**
   - A pattern is structure: a mail check, quilting rows, a scale row, folds that follow the form, rivets, or seams.
   - Never use `random` speckle or per-texel jitter, and never a fixed-period "wear" grid. This fixes audit finding 2.
   - Place marks by hand, where light and use put them: the lit edge and the top rim.
7. Plate gets a specular column on the lit side of the front, a bright top rim and a dark foot.

### A5. Personality cues (Kort: "make each character feel alive")

1. The face is read first. Give it at least one specific cue:
   - brows set in a scowl;
   - a scar;
   - a beard, a braid or warpaint;
   - a hollow eye, a jaw or an open visor slit.
2. Give the troop one signature part, its **mark**, from the unit identity spec. Paint it with the strongest value contrast on the model.
3. Pose cues are geometry and meta (`HUNCH`, `idleArms`), agreed with the animation lead.

### A6. Tiers I → II → III: each tier adds visible mass

| tier | must add (reads at 16 blocks) | template does |
|---|---|---|
| I | the base soldier: clothes, belt, team tabard, bare or simple head | padded gambeson, leather, tabard |
| II | **a new value mass**: a breastplate, pauldrons, a helm or a mantle. Not just a stripe. | breastplate, pauldrons, open helm, vambraces, knee cops, mail below the plate |
| III | **more mass plus the precious trim**: an extra lame, greaves, a cape or crest, trim-metal rims | second pauldron lame, greaves, trim rims (`rimmed`), team crest and cape |

1. Use `stages=[2, 3]` on new bones, and `per_tier(f, (2, 3))` to repaint shared boxes.
2. Each tier is cumulative and is the same soldier (no new silhouette per tier, decision 7.4).
3. Test: at the `far` render, can you tell I from II from III with the colour squinted out? If II only differs by a trim line, add a plate. This fixes audit finding 5.

### A7. The back continues the front; team cloth; the accent share

1. **Back.** Paint the back faces with the same care as the front: hair round the head, the nape, folds and hem on the back panel, the plate's rim continuing round. A flat panel (a 0-deep box) paints only its outward face: use a separate spec for the front and back boxes, as in the template's `tabard_front` / `tabard_back`. Otherwise they z-fight.
2. **Team colour lives only on the cloth sheet**, through `cloth_painter`, from `palette.ramps("team")`. Never put team hues on the skin.
3. **House accent** (gold, violet, ochre, ember or sea blue) goes in fixed paint on the skin: belts, wrist wraps, piping on the plate foot, cords and stitching. Do not use it as a full face or as aura.
   - `tools/art_hue_check.py` must report PASS (the accent band ≥ 10% of saturated texels).
   - Graveborn violet counts only `VIOLET[0..3]`; tones 4 and 5 are too grey. Put tones 1–3 where they show.
   - Gilt or trim is at most about 20% of visible texels: it is trim, not the body.

### A8. Preview → render → self-check → palette → hue → (install)

Run from the repo root. In the commands below, `<b>` is `art/hand/<tid>/build_<tid>.py`.

1. **Preview:**
   ```
   python <b> preview <scratch>/pv crimson
   ```
   This writes, per tier, `<tid>_t1..3.png`, `<tid>_t1..3.geo.json` and `skin_s1..3.png`, plus `cloth_<side>.png`.
2. **Flat contact sheet** (fast, no renderer):
   ```
   python <b> contact <scratch>/contact.png crimson
   ```
3. **Render, near and at 16 blocks:**
   ```
   python tools/art/texture_audit_render.py --renderer <scratch>/render_geo.py --out <scratch>/r --builder <b> --house <house>
   ```
   Look at `<scratch>/r/troop_<tid>.png` (close front, 3/4 and back, plus 16 blocks ×2) and at `<scratch>/r/house_<house>.png`.

   Once the troop is in `design.json`, the classic call works too:
   ```
   python tools/art/texture_audit_render.py --renderer <scratch>/render_geo.py --out <scratch>/r <tid>
   ```
   With no wrapper at all, render the preview geo directly:
   ```
   python <scratch>/render_geo.py --geo <scratch>/pv/<tid>_t1.geo.json --texture <scratch>/pv/<tid>_t1.png --out <dir> --no-gif --views front,three_quarter,back
   ```
   Add `--distance game16` for the 16-block render.
4. **Self-check** against the rubric (audit section 2). Score A–F and write the scores down. Below 8 on any line means fix it before going on.
   - [ ] A: one top-left light; every tall face lighter at the top; recesses dark; head and torso lighter than the legs.
   - [ ] B: each material reads by value and highlight shape (plate specular, mail check, leather soft edge, cloth folds).
   - [ ] C: only House ramps; the accent visible in the House's usual proportion.
   - [ ] D: I → II → III each adds a mass you can see at 16 blocks.
   - [ ] E: at true size, the role, the House and the tier read; key shapes are ≥ 2 texels wide.
   - [ ] F: no orphan texels or holes; the back and sides are finished; seams line up across box edges and at the elbow and knee.
5. **Palette check** (must print `OK`):
   ```
   python tools/art/house_palette.py check --dir <scratch>/pv --house <house>
   ```
6. **Hue check** (must print `PASS`):
   ```
   python tools/art_hue_check.py --dir <scratch>/pv --house <house>
   ```
7. **Install, only when the lead or Kort asks.** The troop must already be in `sheets/design.json` (a design change; not yours to make):
   ```
   python <b> install
   python tools/build_skins.py --verify
   ```
   `install` writes the `art/troops/<tid>.json` stub and runs `tools/build_skins.py`. `--verify` must pass, then rerun `python tools/art/house_palette.py check <tid>`. The palette's `used_by` lists update only when the lead reruns `extract`.

### A9. When to stop and ask

Ask through the lead (or AskUserQuestion when you are the lead) when:

- a material needs a ramp the House palette lacks;
- the accent check fails and only a ramp change would fix it;
- a geometry or rig change is needed;
- the unit's look is not in the identity spec.

Do not work around it with a new colour.

---

## B. A new House

A new House's look is **Kort's decision**. Steps 1–3 are a proposal for him, not a ship.

1. **Write the ramps.** Create `art/hand/common/<house>.py`. Model it on `graveborn.py` or `tidecallers.py`:
   - a docstring that names the House accent and which tones count for the hue check;
   - module-level ramps, `NAME = ramp("#...", ...)`, **5–7 steps, darkest first**. Hue-shift them: shadows cool or purple, lights warm (style guide 1);
   - one ramp per material: skin or flesh, the main armour, an under-layer (mail or hide), cloth, leather, a trim metal;
   - **an accent ramp** whose tones 1–3 sit inside the accent band (saturation ≥ 0.30, value ≥ 0.12);
   - the tint rule: no tone with saturation > 0.45 at value > 0.60 within 45° of a side hue (gold 42, crimson 3, teal 177, violet 264);
   - reuse shared ramps (`handlib.LEATH`, `handlib.SKINS.*`) rather than copying them.
2. **Register the palette.** This makes `palette.ramps("<house>")` and `check --house <house>` work before any troop ships:
   ```
   python tools/art/house_palette.py register <house> <house> --with handlib.LEATH,handlib.SKINS.fair
   ```
   - The first `<house>` is the House id; the second is the module name (or a `.py` path).
   - It writes `art/palettes/<house>.json` (all shades `used_by: []`, `modules: [<house>]`) and the swatch `art/palettes/<house>.png`.
   - It refuses an existing palette. `--force` is only for re-registering a new, unshipped House after a ramp edit.
   - Once troops ship, the lead runs `extract`; it finds the module through the palette's `modules`.
3. **Add the accent band.** In `tools/art_hue_check.py`, add `CENTRES['<house>'] = <hue in degrees>`: the accent ramp's hue, at least about 30° from every other House centre. A whole-House check also needs the House row in `sheets/design.json` (`houses`); that is a design change through the lead.
4. **Prove it.** Add a `PRESETS["<house>"]` entry to a copy of the template, preview it, and pass steps A8.5 and A8.6. Render tiers I–III next to one troop from each existing House (`house_<house>.png`).
5. **Kort approves**, as separate questions:
   1. the House palette and accent hue (the swatch PNG and the renders);
   2. the House silhouette and tier language (an ART-STYLE-GUIDE section 10.x entry);
   3. the first troop's look.

   Until then, nothing is installed and `design.json` is unchanged.

---

## C. Do not

- [ ] Do not repaint, re-tone or "fix" an approved troop. Its builder and its shipped textures stay as they are.
- [ ] Do not change UVs, geometry, box sizes or alpha on existing troops, nor `Spec.java` or `sheets/design.json` from an art task.
- [ ] Do not edit a shared House module's ramps (`graveborn.py` and the others): that recolours six troops at once and is Kort's call.
- [ ] Use no off-palette colour: no `ramp(...)` or `hx(...)` in a troop builder, no `(r, g, b)` literals, no `INK` unless the House palette has it.
- [ ] No random speckle, per-texel jitter, dithering or fixed-period wear marks.
- [ ] No team colour on the skin sheet, and no House accent on the cloth sheet beyond the team palette.
- [ ] No flags or banners on a troop's back (Kort 10.0).
- [ ] Do not install the template, or add `template` to `design.json`.

## D. Definition of done

- [ ] The builder is at `art/hand/<tid>/build_<tid>.py` and reads colours only through `palette.require(<house>, ...)` and the team ramps.
- [ ] Tiers I, II and III preview; the bone count is ≤ 64; there is no pack overflow.
- [ ] `house_palette.py check --dir ... --house <house>` prints OK.
- [ ] `art_hue_check.py --dir ... --house <house>` prints PASS.
- [ ] The renders are attached: `troop_<tid>.png`, near and at 16 blocks, for all tiers, with the self-scores A–F written down and none under 8.
- [ ] Tier II and tier III each add a visible mass at 16 blocks. The back is finished. The legs have a shadow band.
- [ ] One Sonnet-high review of the renders (WORKFLOW.md). Fix rounds are capped at two.
- [ ] It is not installed unless asked. When installed: `python tools/build_skins.py --verify` passes, and `house_palette.py check <tid>` is OK.
