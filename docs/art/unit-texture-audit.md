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

Six criteria, each scored 0 to 10. A troop's grade is the mean. The project's player-facing threshold is 9 (WORKFLOW.md); anything under 8 is a refinement candidate. Scores rank; the written findings are what a repaint acts on.

| # | Criterion | 10 looks like | 5 looks like |
|---|---|---|---|
| A | **Value structure and shading direction** | One consistent top-front light on every part; each form has a clear light, mid and shadow band; darkest values sit in recesses (under the helm brim, armpits, between legs, under belts); the head and torso read lighter than the legs. | Flat fills or noise in place of form; highlights on random sides; the same value everywhere so the figure reads as a cut-out. |
| B | **Material read** | Steel, mail, leather, cloth, fur, bone and skin are told apart by value contrast and highlight shape alone: steel has hard, bright specular texels and dark reflected bands; mail a regular tight pattern; leather warm mid-values with soft edge light and stitching; cloth folds with soft ramps; fur clumped strands; bone warm off-white with cracks. | Two or more materials share a ramp; metal reads as grey cloth; leather reads as wood; patterns are random noise. |
| C | **Palette discipline and House identity** | A limited ramp per material (3 to 5 values, hue-shifted: warm lights, cool shadows or the House's own shift); no stray hues; the House signature (Gilded gold and white on steel, Graveborn bone and grave-green, Wildmoot hide and fur with blood red, Ironclad soot iron and brass, Tidecallers sea-teal, barnacle and salt-bleached cloth) is visible on every troop in the same proportions. | Muddy or over-saturated ramps; colours borrowed from another House; the House signature missing or inconsistent with its siblings. |
| D | **Tier progression** | I → II → III is a clear, cumulative upgrade (more and better armour, richer trim, a stronger silhouette accent), each step readable at 16 blocks, with tier III unmistakably elite but still the same soldier. | Tiers differ only by a recolour, or a step backwards (tier II plainer than I), or tier III a different character. |
| E | **Readability at 16 blocks** | At the true-size game16 render the role reads (shield, bow, caster, beast, crew), the House reads from colour blocking, and the tier is guessable. Key shapes are 2+ texels wide; contrast between big masses is high. | The figure is a grey or brown blob; detail collapses into noise; tiers are indistinguishable. |
| F | **Seams and stray pixels** | Patterns continue across box edges and around the body; no single-pixel orphans, no transparent holes, no wrong-tier leftovers; face and limb seams line up; back and sides are painted with the same care as the front. | Visible breaks at box edges, orphan pixels, untextured or mirrored-wrong sides, back left plain. |

## 3. Grades

(filled in below)
