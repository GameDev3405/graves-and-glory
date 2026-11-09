# Unit texture audit (2026-11-09)

Owner: textures lead. Branch `art/texture-audit` (worktree `C:/Users/kortt/Melty/bb-textures`), cut from `main` at `54e5d339`.
Kort's bar for this pass (2026-11-09): **same style, better craft**. Keep the current pixel style, resolution (1 texel per model pixel, weapons 2, simple faces 2) and every approved look. Improve shading, material read (steel, leather, cloth), palettes, House identity and the tier I to III progression. No restyle, no resolution change, no UV or geometry change without the animation lead's agreement through the coordinator.

## 1. Inventory

| What | Where |
|---|---|
| Troop list, House, display name | `sheets/design.json` → `troops` (30 rows, six per House) |
| Texture builders (one per troop, all tiers) | `art/hand/<id>/build_<id>.py`, shared code in `art/hand/common/` (`handlib.py` palette and paint helpers, `render.py` flat preview renderer, House modules `graveborn.py`, `wildmoot.py`, `tidecallers.py`, `ironclad_tiers.py`, `house_stub.py`) |
| Weapons | `art/hand/weapons/build_*.py` (swords, bows, sun staff, House and Ironclad weapons) |
| Shipped textures | `src/main/resources/assets/gravesandglory/textures/entity/troop/<id>/<id>_s1..s3.png` (+ `_veteran`, `_e` emissive on some), team cloth overlays `team_cloth_<side>.png` (company, crimson, gold, linen, teal, violet) |
| Shipped models (old renderer) | `src/main/resources/assets/gravesandglory/models/troop/<id>.json` |
| GeckoLib geo (rebuild) | `geo/troop/<id>.geo.json` on `anim/geckolib-rebuild` (23 troops; owned by the animation lead) |
| Install and drift check | `python tools/build_skins.py` installs; `--verify` rebuilds into a temp dir and compares. On 2026-11-09: **568 outputs verified**, so the builders reproduce every shipped texture exactly. |
| Offline render | builder `preview <dir> [side]` writes per-tier skin+cloth PNG and a geo.json; `contact <png>` draws a flat five-view sheet. `tools/anim/render_geo.py` (on `anim/geckolib-rebuild`) renders any geo with GeckoLib's baking, at `near` or `game16` (16 blocks in a 1080p window). |
| Audit renders | `tools/art/texture_audit_render.py --renderer <render_geo.py> --out <dir>` builds every troop's preview and renders each tier close (front, 3/4, back) and at 16 blocks (front, 3/4), then writes `troop_<id>.png` and `house_<house>.png` sheets. |

All 30 troops are built by the `art/hand` builders at tiers I, II and III. The audit renders use the crimson team cloth for every troop so Houses compare like for like.

Known gaps in the audit renders: hand-held weapons that live in `art/hand/weapons` and are attached at runtime (bows, crossbows, some staves) do not always appear in the preview geo; the shield and sword do. Weapon art is graded only where it shows.

## 2. Rubric

Six criteria, each scored 0 to 10. A troop's grade is the mean with E (readability at 16 blocks) counted twice: (A+B+C+D+2E+F)/7. Kort's tie-breaker for 1.0 (2026-11-09) is battles that look great, so game-distance read matters most. The project's player-facing threshold is 9 (WORKFLOW.md); anything under 8 is a refinement candidate. Scores rank; the written findings are what a repaint acts on.

| # | Criterion | 10 looks like | 5 looks like |
|---|---|---|---|
| A | **Value structure and shading direction** | One consistent top-front light on every part; each form has a clear light, mid and shadow band; darkest values sit in recesses (under the helm brim, armpits, between legs, under belts); the head and torso read lighter than the legs. | Flat fills or noise in place of form; highlights on random sides; the same value everywhere so the figure reads as a cut-out. |
| B | **Material read** | Steel, mail, leather, cloth, fur, bone and skin are told apart by value contrast and highlight shape alone: steel has hard, bright specular texels and dark reflected bands; mail a regular tight pattern; leather warm mid-values with soft edge light and stitching; cloth folds with soft ramps; fur clumped strands; bone warm off-white with cracks. | Two or more materials share a ramp; metal reads as grey cloth; leather reads as wood; patterns are random noise. |
| C | **Palette discipline and House identity** | A limited ramp per material (3 to 5 values, hue-shifted: warm lights, cool shadows or the House's own shift); no stray hues; the House signature (Gilded gold and white on steel, Graveborn bone and grave-green, Wildmoot hide and fur with blood red, Ironclad soot iron and brass, Tidecallers sea-teal, barnacle and salt-bleached cloth) is visible on every troop in the same proportions. | Muddy or over-saturated ramps; colours borrowed from another House; the House signature missing or inconsistent with its siblings. |
| D | **Tier progression** | I → II → III is a clear, cumulative upgrade (more and better armour, richer trim, a stronger silhouette accent), each step readable at 16 blocks, with tier III unmistakably elite but still the same soldier. | Tiers differ only by a recolour, or a step backwards (tier II plainer than I), or tier III a different character. |
| E | **Readability at 16 blocks** | At the true-size game16 render the role reads (shield, bow, caster, beast, crew), the House reads from colour blocking, and the tier is guessable. Key shapes are 2+ texels wide; contrast between big masses is high. | The figure is a grey or brown blob; detail collapses into noise; tiers are indistinguishable. |
| F | **Seams and stray pixels** | Patterns continue across box edges and around the body; no single-pixel orphans, no transparent holes, no wrong-tier leftovers; face and limb seams line up; back and sides are painted with the same care as the front. | Visible breaks at box edges, orphan pixels, untextured or mirrored-wrong sides, back left plain. |

## 3. Grades, weakest first

Graded on 2026-11-09 from the close and 16-block renders of every tier (crimson cloth). Scores are an independent Sonnet-high grading pass, checked against the textures lead's own pass; one lead adjustment is noted in its row. No troop reaches the player-facing 9; the strongest sit at 8. Ties are broken alphabetically.

House means: Graveborn 4.9, Tidecallers 5.7, Wildmoot 6.4, Gilded 6.7, Ironclad 7.1.

| Rank | Troop | House | A | B | C | D | E | F | Grade | Findings |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | Risen (`revenant`) | Graveborn | 4 | 5 | 5 | 5 | 3 | 5 | 4.3 | T1-T3 whole body: near-black olive/charcoal ramp with almost no value gap between armour, mail and cloth, so the figure is a dark blob at 16 blocks; T2/T3 pauldron spikes and plates are black-on-black noise, with the purple trim stripe repeated on helm, belt and boots at equal weight. |
| 2 | Grave Reaper (`reaper`) | Graveborn | 4 | 4 | 6 | 4 | 4 | 5 | 4.4 | T1-T3 robe/hood: one near-black fill, no fold ramps and no light edge, so the cloth has no material read; T1 to T2 only adds grey pauldrons and T3 only a gold crown, and the hood back is a leftover checker patch that does not match the front. |
| 3 | Gravecaller (`hexer`) | Graveborn | 4 | 5 | 6 | 5 | 4 | 5 | 4.7 | T1-T3 robe and skirt: dark brown-grey lump with no shadow band under the belt; the head is a black cube with a tan face and a lost silhouette; the lantern and its glow are the only values that carry at 16 blocks; T2 adds only dark shoulders. |
| 4 | Barrow Stalker (`stalker`) | Graveborn | 5 | 5 | 4 | 5 | 5 | 5 | 4.9 | T1-T3 body: cool blue-grey speckle noise (not Graveborn bone and grave-green), reads as stone or frost, with random light texels in place of form; T2/T3 dark pauldrons are flat charcoal slabs with no highlight edge. |
| 5 | Harpooner (`tc_harpooner`) | Tidecallers | 5 | 5 | 6 | 5 | 4 | 5 | 4.9 | T1-T3 torso and legs: mottled blue-teal-red noise with no clear light, mid and shadow bands, so cloth, skin and mail share one ramp; the yellow-green helmet is a blank slab and the arm wrap is tan noise; the T2/T3 tan shoulder pads are the only readable upgrade. |
| 6 | Bone Archer (`bonebow`) | Graveborn | 5 | 5 | 6 | 5 | 5 | 4 | 5.0 | T1-T3 legs: thin skeleton legs are patchy grey/purple with no bone ramp, and the red loincloth is a flat slab; T2/T3 dark pauldrons are solid black blocks; the hood is a flat green cube whose back face does not continue the front. |
| 7 | Sunweaver (`mage`) | Gilded | 5 | 5 | 6 | 6 | 5 | 5 | 5.3 | T1 robe: vertical grey-white stripes with no fold shading, and the hood is a blank white-topped cube; T1 lower legs/skirt read flat mid-grey; T2 gold trim and blue shoulders help, but the back is a plain grey panel at every tier. |
| 8 | Admiral Vael, the Drowned Tide (`tc_admiral`) | Tidecallers | 5 | 6 | 6 | 6 | 5 | 5 | 5.4 | T1 robe/legs: dithered blue-grey noise with a pale grey face and chest, so the mass has no value structure; T2/T3 brass sash and shoulder bands are the whole progression, and the T3 hat spikes are single orphan-looking white pixels. |
| 9 | Kraken-touched Brute (`tc_brute`) | Tidecallers | 5 | 6 | 6 | 6 | 5 | 5 | 5.4 | T1-T3 torso and legs: blue-teal mottle noise reads as damp stone, not scales or hide; head is hidden at 16 blocks, so the beast read depends on the red top panel and the T2/T3 brass bands. T3 front chest diagonal pattern breaks at the box edges. |
| 10 | Order Archer (`archer`) | Gilded | 6 | 6 | 6 | 5 | 5 | 6 | 5.6 | T1 vs T2 are almost the same figure (T2 only darkens the face and adds a blue wrist band); the tan hood and sleeves are one flat value at every tier; T3 hood is a solid red slab with no shading, and the legs are dark blobs. |
| 11 | Mist Caller (`tc_mist`) | Tidecallers | 5 | 6 | 6 | 6 | 5 | 6 | 5.6 | T1-T3 robe: uniform navy with fine vertical pinstripe and little light on the shoulders; head cube is a dark slab with a small face; T2/T3 brass shoulder bands carry the upgrade, and the stone base reads well. Lead adjustment (A 6→5, E 6→5): the robe painter has no top-to-bottom light, so the whole robe is one value; the face disappears under the hood at 16 blocks. |
| 12 | Drowned Raider (`tc_raider`) | Tidecallers | 6 | 6 | 6 | 6 | 6 | 6 | 6.0 | T1 helm: flat green cube with a single dark slit and no shell highlight, so it reads as a cap; torso and legs are deep navy with weak shadow bands; T2/T3 brass pauldrons and leg wraps are the progression, but T3 reads as only a gold stripe pass. |
| 13 | Hide Hunter (`wm_hunter`) | Wildmoot | 6 | 6 | 7 | 5 | 6 | 6 | 6.0 | T1-T3 legs: flat dark brown column with no knee or shadow band; arms are flat pink skin; T1 to T2 changes only add bone shoulder spikes and the T3 plume, with the helm slab unchanged. |
| 14 | The Hollow King (`gb_hollowking`) | Graveborn | 6 | 7 | 7 | 7 | 6 | 4 | 6.1 | T1-T3 feet/back: a stray red scrap pokes out below the heel and a red blob sits behind the legs, which reads as a stray floating strip; the back cape is a flat red panel; the grey pauldron spikes are flat with no highlight (the blue ribcage glow and gold crown are good). |
| 15 | Order Longbow (`longbow`) | Gilded | 6 | 6 | 6 | 7 | 6 | 6 | 6.1 | T2/T3 sleeves: blue-grey checker is noisy and does not look like mail or cloth; T1 hat is a flat brown slab; the T3 white plume reads as a floating stick rather than a feather. |
| 16 | Tusk Spear (`wm_tusk`) | Wildmoot | 6 | 6 | 7 | 6 | 6 | 6 | 6.1 | T1-T3 arms and legs: flat pink-brown arms and dark brown legs with no shadow band; the red wrap bands do the colour work; the T3 helm spike set is good but the body is nearly unchanged from T2. |
| 17 | Moot Hound (`wm_hound`) | Wildmoot | 6 | 7 | 7 | 7 | 6 | 5 | 6.3 | Front view: the head and chest collapse into a dark brown-red mass at 16 blocks and the mouth/muzzle is lost; the back-spine spikes and red saddle are cut-out like; T3 antlers and bone armour are a strong upgrade. |
| 18 | Web Stalker (`wm_stalker`) | Wildmoot | 6 | 7 | 6 | 7 | 6 | 6 | 6.3 | T1-T3 legs: black and green banded legs are outside the Wildmoot hide and bone read and fuse into a dark mass; T1 abdomen is dark green flat with no fur clumps; T2/T3 bone plating is a clear step up. |
| 19 | Ironhold Mortar (`ic_mortar`) | Ironclad | 7 | 7 | 7 | 6 | 6 | 6 | 6.4 | T1-T3 torso: flat brown leather tabard with one gold bit, and the mortar tube is a large dark-grey block that hides the head from the back; T1 to T3 only brass trim is added to the same plate shapes. |
| 20 | Arbalist (`ic_arbalist`) | Ironclad | 7 | 7 | 7 | 7 | 6 | 7 | 6.7 | T1 helm: plain flat grey slab with a brown band, with no specular edge; the torso grid of small squares is regular but the red legs are one flat fill with no shadow band; T3 brass is added as stripes at every limb with little shading. |
| 21 | Bloodhowl Seer (`wm_seer`) | Wildmoot | 7 | 7 | 7 | 8 | 6 | 6 | 6.7 | T1 head: grey checker mask block has no value structure and reads as a different material from the hide; legs are flat dark robe; T3 bone shoulder armour and skulls are a good tier jump. |
| 22 | Old Horn, the Antlered Alpha (`wm_oldhorn`) | Wildmoot | 7 | 7 | 8 | 6 | 7 | 6 | 6.9 | T1-T3 body: brown dithered fur is heavy single-pixel noise across the whole flank, with no clump shapes or shadow band under the belly; T1 to T2 is mostly a white trim strip, and T3 adds skulls and antlers. |
| 23 | Trident Guard (`tc_tideguard`) | Tidecallers | 7 | 7 | 7 | 8 | 7 | 6 | 7.0 | T1 helm: blue-dot cap is busy and the face-hole is a tiny grey patch; the red brick tabard is a good focal point but the lower legs/boots are tan with no highlight; T2/T3 shoulder bands are a clear step. |
| 24 | Lord Marshal Aldric (`gl_marshal`) | Gilded | 7 | 7 | 8 | 5 | 8 | 7 | 7.1 | T1 vs T2 front are nearly the same (T2 adds only the back banner), so the tier II step is weak; black plate is a flat charcoal with gold stripes and little steel highlight. |
| 25 | Ballista Crew (`ic_ballista`) | Ironclad | 7 | 8 | 8 | 6 | 7 | 7 | 7.1 | T1 to T3 crew: identical helm and torso shapes, only brass added; the ballista frame brass is bright but flat; red sun roundel is a good focal point. |
| 26 | Brassjaw, the Forge Colossus (`ic_colossus`) | Ironclad | 7 | 7 | 8 | 5 | 8 | 7 | 7.1 | T1 vs T2 vs T3 are nearly identical at 16 blocks (T2 only a gold edge, T3 small studs and bits on the shoulders), so the tier read is weak; the dark legs are flat charcoal blocks with no shadow band. |
| 27 | Shieldbreaker (`ic_breaker`) | Ironclad | 7 | 8 | 8 | 7 | 7 | 7 | 7.3 | T1 torso: dark chest panel with brown straps is muddy at 16 blocks; the red back banner with the hammer emblem is strong; T3 brass piping on legs and shoulders is a clear upgrade. |
| 28 | Templar (`sentinel`) | Gilded | 8 | 8 | 8 | 8 | 8 | 7 | 7.9 | T1 legs: flat red fill with no shadow band; T2/T3 dark steel shoulder tiers have clear light and dark, but the T2 helm slab is plain; the gold cross, cape back and T3 trim are clean. |
| 29 | Hearthguard (`ic_hearthguard`) | Ironclad | 8 | 8 | 8 | 8 | 8 | 8 | 8.0 | T1 legs/boots: grey blocks have little knee or shin shadow; the red chest panel is checkered and slightly noisy; steel highlights, brass trim and the back panel are the best sibling-consistent shading in the set. |
| 30 | Order Squire (`knight`) | Gilded | 8 | 8 | 8 | 9 | 8 | 7 | 8.0 | T2 helm: flat blue-grey slab with a single blocky face cut-out and little steel specular; T1 legs are a flat red fill with no shadow band; the tier chain (mail, plate, gilded) is the clearest in the set. |

## 4. Findings across troops

1. **No top-to-bottom light.** `handlib.lightx` and `LIT` shade by face and by column only; nothing darkens a face from its top row to its bottom row, and few painters add recess shadows. Long robes, cloaks and legs therefore read as one flat value (Mist Caller, Grave Reaper, Gravecaller, most legs). This is the single biggest lever and is fixable per troop inside the existing painters.
2. **Noise in place of form.** Random per-texel speckle stands in for material on the Barrow Stalker, Risen, Harpooner, Brute, Admiral and Old Horn fur. At 16 blocks it averages to grey and hides the silhouette. Material patterns should be regular (mail, quilting, planks of fur) or follow the form (folds, rime on upper edges).
3. **Graveborn values are crushed.** The House ramps (`GIRON`, `ROT`, `MOSS`) start near black and most painters use their bottom three tones, so iron, mail, cloth and flesh share one value band. The House stays dark; each troop needs one light mass (bone, rime, gilt or flesh) and a clear mid band.
4. **Legs and boots are flat everywhere.** Even the strongest troops (Order Squire, Templar, Hearthguard) have a flat leg fill with no knee or shin shadow band.
5. **Tier II is often a trim pass.** Lord Marshal, Brassjaw, Ballista Crew, Order Archer and Grave Reaper change too little from I to II to read at 16 blocks. Tier steps need one new value mass (a plate, a mantle, a hood colour), not only a stripe.
6. **Seams and strays** are rare but present: the Grave Reaper hood back, the Hollow King's red scrap below the heel and flat cape back, single-texel spikes on the Admiral's tier III hat.

## 5. Order of work (proposed, for Kort)

Weakest first, one troop per commit, each with before/after renders and a Sonnet-high review, batched for Kort: the five Graveborn troops (Risen, Grave Reaper, Gravecaller, Barrow Stalker, Bone Archer), then the Tidecallers noise troops (Harpooner, Mist Caller, Brute, Admiral), then the remaining Gilded, Wildmoot and Ironclad troops by rank. The strongest troops get only the cross-troop fixes (leg shadow bands, tier II mass). Shared House ramps (`art/hand/common/graveborn.py` and siblings) are not changed without Kort's say, since they recolour six troops at once.

## 6. Contact sheets

One per House, each row a troop, per tier: close (front, 3/4, back) at half size, the 16-block render at true size, then at 2x nearest.

- [Gilded](texture-audit/house_gilded.png)
- [Graveborn](texture-audit/house_graveborn.png)
- [Wildmoot](texture-audit/house_wildmoot.png)
- [Ironclad](texture-audit/house_ironclad.png)
- [Tidecallers](texture-audit/house_tidecallers.png)

## 7. Proposals for the three weakest (previews, not installed)

Risen (`revenant`), Grave Reaper (`reaper`) and Gravecaller (`hexer`), painted by editing only each troop's builder. Boxes, UVs and transparency are identical to the shipped skins on all nine tier skins (checked by the lead), and the Graveborn violet check passes. The diffs are kept as patches; nothing is applied to the builders on this branch or installed.

- Risen: [before/after](texture-audit/proposals/revenant_before_after.png), [patch](texture-audit/proposals/revenant.patch)
- Grave Reaper: [before/after](texture-audit/proposals/reaper_before_after.png), [patch](texture-audit/proposals/reaper.patch)
- Gravecaller: [before/after](texture-audit/proposals/hexer_before_after.png), [patch](texture-audit/proposals/hexer.patch)

What they show: inside the "no lift" limit, structure improves clearly up close (seams fixed, iron edges, fold light, tier II and III steps), but the gain at 16 blocks is modest because the Graveborn ramps are near-black. Bigger distance gains need lifting the House iron and robe values, which is a look decision for Kort.
