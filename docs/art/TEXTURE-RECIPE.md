# Texture recipe: new units and new Houses

For a worker texturing a NEW troop (or a new House) in the hand style: one texel per model pixel, painted by a Python builder. Kort's rule (2026-11-09): **approved troops are not repainted.** This recipe is for new work only. Each House keeps one locked palette.

Read first: `docs/art/unit-texture-audit.md`, sections 2 (the rubric) and 4 (the common failures). Skim `art/hand/common/handlib.py` (`LIT`, `lightx`, `limb`, `Troop`). The template is `art/hand/_template/build_template.py`. Every `# EDIT (Xn):` in it points to a step below.

Paths used throughout:

- `<scratch>`: your scratchpad directory. Never write previews into the repo.
- `<renderer>`: GeckoLib's offline renderer, `tools/anim/render_geo.py` on branch `anim/geckolib-rebuild`. Get a copy with `git show anim/geckolib-rebuild:tools/anim/render_geo.py > <scratch>/render_geo.py`. Do not commit it to an art branch.

**Which rules are enforced by a tool and which are checked by the reviewer:**

- Enforced by a tool:
  - the palette (`house_palette.py check`);
  - the accent hue (`art_hue_check.py`);
  - the template gate (`build_<tid>.py gate`): primary-ramp share, trim and accent share, top-to-bottom light on every tall face, and texels clamped at a ramp end.
- Checked by the reviewer: everything else, including structure, material cues, the tier masses, the read at 16 blocks and personality. Passing the tools does not mean the unit is good: the trial unit passed both checks and was graded 5.1.

---

## A. A new unit in an existing House

### A1. Pick the House and its palette

1. The House is named in your **brief**. The brief also gives the unit's role, look, mark and tiers. For a unit already on the roster, the look and mark come from `docs/superpowers/specs/2026-10-17-unit-identity.md` and the House section of `docs/ART-STYLE-GUIDE.md` 10.x. If the brief has no House, or no look, ask before you start.
2. Look at the House's ramps:
   ```
   mkdir -p <scratch>
   python tools/art/house_palette.py show <house> <scratch>/<house>.png
   ```
   Or open `art/palettes/<house>.png`. Ramp names are module-qualified, for example `graveborn.BONE`, `handlib.STEEL` or `ironclad_tiers.BRASS`.
3. Paint **only** from `palette.ramps(house)` / `palette.require(house, ...)`, and from the team ramps (`palette.ramps("team")`) on the cloth sheet.
   - Any ramp in the House palette may be used, including ramps another troop's builder defined (for example `build_tc_raider.NAVY`).
   - A ramp from another House is off-palette.
   - Never write a new hex value: no `ramp("...")`, `hx(...)` or `(r, g, b)` literal in a troop builder.
4. **Reserved colours.** Some ramps belong to one troop by lore, for example `tidecallers.CORAL`, which is the Trident Guard's only. The House module docstring and ART-STYLE-GUIDE 10.x say which. If your brief asks for a reserved colour, or for a look the House section rules out, **flag it to the lead and wait**. Do not pick a side yourself.

### A2. Copy the template

1. Copy the template into the troop's own folder:
   ```
   mkdir -p art/hand/<tid>
   cp art/hand/_template/build_template.py art/hand/<tid>/build_<tid>.py
   ```
2. Edit the builder:
   - set `TID = "<tid>"` and `HOUSE = "<house>"`;
   - delete the `--house=` argv loop and the other entries of `PRESETS`;
   - rename `TEMPLATE` to the troop;
   - write `NOTE` (one line: look, mark, tier steps).
3. **Weapons are out of scope for a texture task.**
   - Set `meta["weapon"]` to an existing weapon id from another builder's meta (`arming_sword`, `cutlass`, `harpoon`, `boar_spear`, `order_bow`, `notched_sword`, ...; list them with `grep -ho '"weapon": "[a-z_]*"' art/hand/*/build_*.py | sort -u`).
   - If none fits, use `"none"` and write `weapon: TODO <what the brief wants>` in `NOTE` and in your report.
   - A new weapon is a separate task (ART-STYLE-GUIDE 7).
4. Keep the biped skeleton exactly as the template builds it:
   - `limb()` joints;
   - the hand bones and `arms_folded`;
   - the `tabard` bone name, which the engine swings.

   Geometry may grow for a NEW troop, but the animation lead owns the rig contract (`handlib` `ELBOW`/`KNEE`/`HAND`, 64 bones max).
5. **Geometry rules** (see the comment above `bones()` in the template):
   - **Centre gear on its limb.** A forearm or shin box spans x -2..2 and z -2..2 in its bone's frame. Gear goes at `(-2, y, -2)` with width 4 and stands proud through `dil` (0.3–0.4). Do not move it out with offsets.
   - **Mirror with `c`.** `c = -1` is the wearer's right and `+1` the left. Every mirrored number takes `c`: the outward offset `0.5 * c` and the pauldron roll `rot=(0, 0, 14 * c)`, which drops the outer edge on both sides. A flipped or hard-coded sign on one side makes that pauldron a wing. Check the front near render for symmetry.
   - **Layering in front of the tabard.** The body's front face is at z -2, and the team `tabard` panel hangs at z -2.6. Chest gear that must show in front of it needs its front face nearer than z -2.65: a box at z -2 with `dil` ≥ 0.7. Alternatively, keep the tabard below the chest gear, as the template does (tabard y -5..3, breastplate y -12..-6).
   - **Flat panels.** A 0-deep box paints only its outward face. Use a separate spec for the front and back panels, as in `tabard_front` / `tabard_back`; otherwise they z-fight.
   - **Transparent texels.** A painter may return `None` for a texel; it is transparent. Use it only for deliberate cut-outs: a face window, a ragged hem or the gaps in teeth. A hole elsewhere shows the inside of the box. Never change transparency on an existing troop.

### A3. Choose ramps and base indices

Fill `MAT = {role: (ramp name, base index)}`.

1. **The main masses come from the House's primary ramps.** The hood, torso, arms, legs and the tier II and III armour all use the primary armour or cloth ramps in the table below. Set `PRIMARY_ROLES` to the roles that use them.
2. **Trim and accent stay under about 20%** of visible texels together. The gate fails a tier over `TRIM_MAX`.
3. The team cloth (tabard, cape, crest) is the side's colour, not the House's. The House must still read with the cloth hidden.

   | House | primary armour | primary cloth / body | trim | accent (hue check) | reserved, notes |
   |---|---|---|---|---|---|
   | Gilded | `handlib.STEEL` | `handlib.LINEN` (white surcoats), `handlib.MAIL` | `handlib.GOLD` | `handlib.GOLD` (gold 42) | pale and upright; faces shown |
   | Ironclad | `ironclad_tiers.IRON` | `ironclad.OILED`, `handlib.LEATH`, `handlib.MAIL` | `ironclad_tiers.BRASS` | `ironclad_tiers.EMBER` (ember 13) | soot iron with brass |
   | Graveborn | `graveborn.GIRON` (base 4+, it is near black) | `graveborn.ROT`, `graveborn.RUST`, `graveborn.BONE` (the light mass) | `graveborn.GILT` | `graveborn.VIOLET` tones 1–3 (violet 269) | every troop needs one light mass |
   | Wildmoot | `wildmoot.BONE`, `wildmoot.HORN` | `wildmoot.HIDE`, `wildmoot.FUR`, `wildmoot.GREYFUR` | `wildmoot.HORN` | `wildmoot.OCHRE` (ochre 32) | blood red only as wraps |
   | Tidecallers | `tidecallers.BRONZE`; `tidecallers.DEEP` (tier III plate) | `build_tc_raider.NAVY`, `tidecallers.DEEP`, `tidecallers.BARN` | `tidecallers.WARM` | `tidecallers.SEA` (sea blue 210) | `tidecallers.CORAL` reserved for the Trident Guard |

4. **Base index by ramp length.** Painters move a texel from its base by -3 to +2: top face +1, lit column +1, top band +1, pattern ±1, recesses -2. Off the ramp, the tone clamps and the form is lost. The gate fails above 12% clamped.

   | tones | base | note |
   |---|---|---|
   | 4 | 1 | the highlight clamps on top faces; use it for small parts only |
   | 5 | 2 | |
   | 6 | 2 or 3 | 3 for the light mass, 2 for legs |
   | 7 | 3 or 4 | 4 for a near-black ramp such as `GIRON` |

5. **Value order**, from lightest to darkest:
   - head and torso;
   - arms;
   - legs, one step below the torso (the template's hose uses `B("padding") - 1`);
   - boots and recesses.

   Every troop needs **one light mass** that reads at 16 blocks: bone, rime, gilt, linen or flesh. Neighbouring materials sit two or more tones apart where they touch.
6. **Adding a role** (rope, shell, oilskin, fur):
   - add `"rope": ("<ramp>", base)` to `MAT`;
   - paint it with `fill("rope", pattern=rope_twist)`, or with `R("rope")` / `B("rope")` in your own painter;
   - add it to `PRIMARY_ROLES` or `TRIM_ROLES` if it is one of those.

   Two materials never share a ramp. If the House palette has no fitting ramp, see A9.

### A4. 3D depth: paint form, not texture

Build **every** painter on the template's `fill()` / `tone()`. A painter that writes colours directly loses the light, and the gate's gradient test catches it.

1. **Face light.** Add `LIT[face]` to every face: top +1, front and right 0, left and back -1, bottom -2.
2. **Lit column.** Add `lightx(face, x, w)` on the side faces. The wearer's right (the viewer's left) is lit.
3. **Top-to-bottom light on every tall face (h ≥ 6), front AND back.** Use +1 on the top sixth and -1 on the bottom quarter. On short faces, use a +1 rim on the top row and a -1 seam on the bottom row. This fixes audit finding 1, the biggest lever.
   - Limbs are painted as one 12-row face (`Box cut`), so `fill()` lights them from shoulder to wrist and from hip to sole.
   - The template's `hood()` shows a hood lit the same way, with a back seam fold, so the back of the hood is not a slab.
4. **Recesses go dark.** Use 1–2 steps down under a brim, under a belt, between the legs, and under a pauldron.
5. **Legs get a shadow band.** Each leg needs:
   - a knee crease;
   - an inner column, plus the inner side face, one step darker;
   - a lit boot cuff;
   - a darkest sole.

   Copy `leg(side)`. This fixes audit finding 4.
6. **Form, not noise.**
   - A pattern is structure.
   - Never use `random` speckle or per-texel jitter, and never a fixed-period "wear" grid. This fixes audit finding 2.
7. **Material cues.** Each material must read differently by its pattern and its highlight, not only by its hue:

   | material | cue | template helper |
   |---|---|---|
   | plate | a specular column at the lit edge of the front, a bright top rim, a dark foot | `plate()` |
   | mail | a two-texel check, one tone apart | `mail_check` |
   | quilted cloth, gambeson | a darker row every fourth | `quilt_rows` |
   | hanging cloth, robes | two vertical folds, the shadow side down | `folds` |
   | leather | flat mid tones, a soft lit top edge, a dark seam; at most one hand-placed scuff | `fill("leather")` |
   | rope, cord | a diagonal twist: a light strand, a mid tone, a dark groove | `rope_twist` |
   | oilskin, wet hide | a dark matte field with one bright gloss streak along each fold (+2) | `gloss_fold` |
   | shell | a ridged fan from the bottom centre, alternating light and dark, with a bright top rim | `shell(role)` |
   | scales | offset rows, each scale lit on its upper edge | `scale_rows` |
   | fur | clumps 2–3 texels wide in strands along the growth, lit tips; never single-texel dither | write it on `fill()` |
   | bone | the light mass: pale, one crack line along the long axis, darker at the root | write it on `fill()` |

### A5. Personality cues (Kort: "make each character feel alive")

1. The face is read first. Give it at least one specific cue:
   - brows set in a scowl;
   - a scar;
   - a beard, a braid or warpaint;
   - a hollow eye, a jaw or an open visor slit.
2. Give the troop one signature part, its **mark**, from the brief. Paint it with the strongest value contrast on the model.
3. Pose cues are geometry and meta (`HUNCH`, `idleArms`), agreed with the animation lead.

### A6. Tiers I → II → III: each tier adds visible mass

| tier | must add (reads at 16 blocks) | template does |
|---|---|---|
| I | the base soldier: primary-cloth clothes, belt, team tabard, a hood or bare head | primary-cloth hood and gambeson, leather, tabard |
| II | **a new mass in a House PLATE ramp**, with a distinct silhouette in the far render: a breastplate, pauldrons or a helm. Not a stripe, not a grey blob of another material. | plate breastplate, pauldrons, open helm, vambraces, knee cops, mail below the plate |
| III | more mass **plus one continuous trim structure at least 2 texels wide and about 15 texels long**: a rim band right round the breastplate, a baldric, the edge of a mantle. Never scattered trim dots. | breastplate rim band 2 rows deep (`rimmed(..., depth=2)`), second pauldron lame, greaves, team crest and cape |

1. Use `stages=[2, 3]` on new bones, and `per_tier(f, (2, 3))` to repaint shared boxes.
2. Each tier is cumulative and is the same soldier (no new silhouette per tier, decision 7.4).
3. Test: at the `far` render, squint. Can you tell I from II from III by shape alone? If II differs only by a line, add a plate. If III reads as noise, merge the trim into one band. This fixes audit finding 5.

### A7. The back continues the front; team cloth; the accent share

1. **Back.** Paint the back faces with the same care as the front: hair or hood round the head, the nape, folds and hem on the back panel, the plate's rim continuing round.
2. **Team colour lives only on the cloth sheet**, through `cloth_painter`, from `palette.ramps("team")`. Never put team hues on the skin.
3. **House accent** goes in fixed paint on the skin: belts, wrist wraps, piping on the plate foot, cords and stitching. The accent is gold, violet, ochre, ember or sea blue, depending on the House.
   - `tools/art_hue_check.py` must PASS (the band ≥ 10% of saturated texels).
   - Graveborn violet counts only `VIOLET[0..3]`.

### A8. Preview → gate → render → checks → review → (install)

Run from the repo root. In the commands below, `<b>` is `art/hand/<tid>/build_<tid>.py`.

1. **Preview** (the command creates `<scratch>/pv`; the gate prints at the end):
   ```
   python <b> preview <scratch>/pv crimson
   ```
   This writes, per tier, `<tid>_t1..3.png`, `<tid>_t1..3.geo.json` and `skin_s1..3.png`, plus `cloth_<side>.png`.
2. **Gate** (must print `gate PASS`; exit 1 otherwise):
   ```
   python <b> gate
   ```
3. **Render, near and at 16 blocks:**
   ```
   python tools/art/texture_audit_render.py --renderer <scratch>/render_geo.py --out <scratch>/r --builder <b> --house <house>
   ```
   Look at `<scratch>/r/troop_<tid>.png` (close front, 3/4 and back, plus 16 blocks ×2) and at `<scratch>/r/<tid>/t<N>_far.png`, which is true size.

   With no wrapper, render the preview geo directly:
   ```
   python <scratch>/render_geo.py --geo <scratch>/pv/<tid>_t1.geo.json --texture <scratch>/pv/<tid>_t1.png --out <dir> --no-gif --views front,three_quarter,back
   ```
   Add `--distance game16` for the 16-block render.
4. **The 16-block gate.** Open each tier's `t<N>_far.png` at 1x, with no zoom, and write down three answers. If any answer needs zooming or guessing, it is a fail: fix the values or the masses first.
   1. the role (a shield, a caster, an archer, a beast, ...);
   2. the tier;
   3. the House.
5. **Self-check** against the rubric (audit section 2). Score A–F and write the scores down.
   - [ ] A: one top-left light; **every limb and the hood (or helm) show light to dark, on the front AND the back**; recesses dark; head and torso lighter than the legs.
   - [ ] B: every material is told apart by its cue (A4.7), not only by its hue.
   - [ ] C: the main masses are in the House's primary ramps (A3); trim and accent stay under about 20%; the accent is visible.
   - [ ] D: tier II adds a plate-ramp mass with its own silhouette; tier III adds a continuous trim band at least 2 texels wide.
   - [ ] E: the 16-block gate (step 4) passes for every tier.
   - [ ] F: no orphan texels or holes; the back and sides are finished; seams line up at box edges, the elbow and the knee; the pauldrons are symmetric.

   **The self-check is not the grade.** Self-scores ran about 3 points high in the trial. Use the self-check to find what to fix, not to report a score.
6. **Palette check** (must print `OK`):
   ```
   python tools/art/house_palette.py check --dir <scratch>/pv --house <house>
   ```
7. **Hue check** (must print `PASS`):
   ```
   python tools/art_hue_check.py --dir <scratch>/pv --house <house>
   ```
8. **Independent review.** Before anything is shown to Kort, the lead sends the renders (`troop_<tid>.png` and the `t<N>_far.png` files), the brief and the rubric (audit section 2) to a **fresh Sonnet-high reviewer** who has not seen your scores. That reviewer's grade is the grade. Under 8 on any line means a fix round; there are at most two fix rounds (WORKFLOW.md), then you escalate.
9. **Install, only when the lead or Kort asks.** The troop must already be in `sheets/design.json` (a design change; not yours to make):
   ```
   python <b> install
   python tools/build_skins.py --verify
   ```
   `install` writes the `art/troops/<tid>.json` stub and runs `tools/build_skins.py`. `--verify` must pass, then rerun `python tools/art/house_palette.py check <tid>`.

### A9. When to stop and ask

Ask through the lead (or AskUserQuestion when you are the lead) when:

- a material needs a ramp the House palette lacks;
- the brief conflicts with a reserved colour or with the House section;
- the accent check fails and only a ramp change would fix it;
- a geometry or rig change is needed;
- the brief lacks the House, the look or the mark.

Do not work around it with a new colour.

---

## B. A new House

A new House's look is **Kort's decision**. Steps 1–4 are a proposal for him, not a ship.

1. **Write the ramps.** Create `art/hand/common/<house>.py`. Model it on `graveborn.py` or `tidecallers.py`:
   - a docstring naming the House accent, which tones count for the hue check, and any reserved ramps;
   - module-level ramps, `NAME = ramp("#...", ...)`, **5–7 steps, darkest first**. Hue-shift them: shadows cool or purple, lights warm (style guide 1);
   - one ramp per material: a primary armour, a primary cloth or body, an under-layer, leather, a trim metal;
   - **an accent ramp** whose tones 1–3 sit inside the accent band (saturation ≥ 0.30, value ≥ 0.12);
   - the tint rule: no tone with saturation > 0.45 at value > 0.60 within 45° of a side hue (gold 42, crimson 3, teal 177, violet 264);
   - reuse shared ramps (`handlib.LEATH`, `handlib.SKINS.*`) rather than copying them.
2. **Register the palette.** This makes `palette.ramps("<house>")` and `check --house <house>` work before any troop ships:
   ```
   python tools/art/house_palette.py register <house> <house> --with handlib.LEATH,handlib.SKINS.fair
   ```
   - The first `<house>` is the House id; the second is the module name (or a `.py` path).
   - It writes `art/palettes/<house>.json` and the swatch `art/palettes/<house>.png`.
   - It refuses an existing palette. `--force` is only for a new, unshipped House.
   - Once troops ship, the lead runs `extract`.
3. **Add the accent band and the primary row.**
   - Add `CENTRES['<house>'] = <hue in degrees>` to `tools/art_hue_check.py`, at least about 30° from every other House centre.
   - Add the House's row to the A3 table.
   - Add a `PRESETS["<house>"]` entry to the template.
   - A whole-House check also needs the House row in `sheets/design.json`; that is a design change through the lead.
4. **Prove it.**
   - Run the template with `--house=<house>`. Pass the gate, A8.6 and A8.7.
   - Render tiers I–III next to one troop from each existing House.
5. **Kort approves**, as separate questions:
   1. the palette and the accent hue;
   2. the silhouette and tier language (an ART-STYLE-GUIDE 10.x entry);
   3. the first troop's look.

   Until then, nothing is installed and `design.json` is unchanged.

---

## C. Do not

- [ ] Do not repaint, re-tone or "fix" an approved troop. Its builder and its shipped textures stay as they are.
- [ ] Do not change UVs, geometry, box sizes or alpha on existing troops, nor `Spec.java` or `sheets/design.json` from an art task.
- [ ] Do not edit a shared House module's ramps: that recolours six troops at once and is Kort's call.
- [ ] Use no off-palette colour (A1.3), and no reserved colour without the lead's word (A1.4).
- [ ] No random speckle, per-texel jitter, dithering or fixed-period wear marks.
- [ ] Do not let trim, gilt, accent or a team panel be the main mass.
- [ ] No team colour on the skin sheet.
- [ ] No flags or banners on a troop's back (Kort 10.0).
- [ ] Do not report a self-score as the grade.
- [ ] Do not install the template, or add `template` to `design.json`.

## D. Definition of done

- [ ] The builder is at `art/hand/<tid>/build_<tid>.py` and reads colours only through `palette.require(<house>, ...)` and the team ramps.
- [ ] Tiers I, II and III preview; there are ≤ 64 bones; there is no pack overflow.
- [ ] `python <b> gate` passes, `house_palette.py check --dir` is OK, and `art_hue_check.py --dir` passes.
- [ ] The 16-block gate (A8.4) is answered for each tier: role, tier and House.
- [ ] Tier II adds a plate-ramp mass, tier III a continuous trim band; the back is finished; the legs have a shadow band; the pauldrons are symmetric.
- [ ] The weapon is an existing id, or is marked `weapon: TODO`.
- [ ] A fresh Sonnet-high reviewer grades the renders with the rubric. The grade is ≥ 8 on every line, or the open lines are listed after two fix rounds.
- [ ] Nothing goes to Kort before that review.
- [ ] It is not installed unless asked. When installed, `python tools/build_skins.py --verify` passes and `house_palette.py check <tid>` is OK.
