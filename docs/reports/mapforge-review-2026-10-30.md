# Map Forge review: section-based maps, 2026-10-30

Proposal only. No code changed. Checkout `bannerbound-mapforge`, branch `work/mapforge-2a3` (Map Forge at 057b256).
Sources read: WORKFLOW.md, PLAN-TO-1.0 tasks 2A.1 to 2A.8 and 7.3, `tools/mapforge/*`, the two samples, the 2A.3 report,
`Arena.java`, `MapDressing.java`, `CampaignMaps.java`, the patch scripts in `tools/`, the structure pieces, and the
five 180-degree panoramas in the main checkout's `work/media/panoramas` (there are no map shots under `docs/media`).

## 1. Why the maps read as AI-generated and sloppy

The maps are not built. They are computed, whole, from a few dozen numbers, by code nobody looks at block by block.

- **Formula terrain.** `Arena.rawHeight` sums a few round Gaussian hills plus two sine waves. Every hill is the same
  smooth dome, so in the Ironhold and Hollowwood panoramas the hills read as stepped pyramids: even one-block
  terraces, regular rings, no ridges, cliffs, gullies or rock. The far rim is the same formula raised at the border.
- **One surface block per map.** `Arena.natural` picks grass, snow or podzol for the whole map; stone only above
  y 94. Big fields are one flat colour with no worn ground, gravel, coarse dirt, moss or mud.
- **Even scatter.** Plants go on every Nth column by a hash; scenery boulders are the same two-block cubes;
  trees are one formula (spruce cone or oak ball), so Hollowwood has rows of identical cones. Nothing clusters,
  thins out, or follows a slope, a wall or a path the way real growth does.
- **Buildings drawn in code.** Most landmarks are `box()` calls in `Arena.structure` and `MapDressing`: one wall
  block, flat faces, no depth, trim, overhang or wear. The Ironhold ruin's broken tops come from
  `floorMod(a*17,6)`, which is why the walls show a neat sawtooth. Shrine pillars end in sea lanterns, flags are fence
  poles with a sea lantern on top. These are placeholder shapes that were never replaced.
- **Hard edges.** Roads, yards, capture rings and structure pads are flattened with a smooth blend, then stopped
  at a straight line. Grass meets path, pad meets field and wall meets ground at a ruler edge.
- **No plan above the blocks.** Layout lives as coordinates in `Spec` rows and in Python scripts that patch Java
  strings (`refine_map_identity.py`, `refine_landmarks.py`). There is no drawing of zones, routes or sight lines, no
  reference image, and no per-area review. Each pass touched the whole map, so no single area got real attention.
- **No review image.** Nobody, human or agent, sees a top-down view before a change lands. Problems were only seen
  in late media shots.

The 38 hand-made `.nbt` pieces (camp tents, Ironhold keep and gate, Hollowwood chapel and so on) are the best-looking
part of the maps. That is the lesson: hand-placed, reviewed work looks deliberate; formulas look generated.

## 2. Section-based workflow

**Step 1: master layout (one per map).** A top-down sketch at 1 cell = 4 blocks, kept as JSON plus a rendered
image: map bounds, zones (spawn, approach, contested middle, flanks, rim), the main and side paths with widths,
capture points and spawns (locked, plan 7.3), key sight lines (lines that must stay open or blocked), cover
density per zone, and a height sketch (where the high ground is). Kort signs off the layout before any blocks.

**Step 2: cut into sections.** Each section is roughly 48x48 to 64x64 blocks, sized so one review image shows it
clearly. A battle map is 700x500 (`arenaHalfX` 350, `arenaHalfZ` 250), so about 85 sections at 64x64;
quiet rim sections can share one light pass. Every section has:

| field | meaning |
|---|---|
| `id`, `name` | `ironhold-s07`, "east curtain breach" |
| `bounds` | min and max box; sections tile the map, with a shared edge strip of 4 to 6 blocks |
| `zone`, `role` | from the master layout: what this place is for in a fight |
| `brief` | 3 to 6 lines: mood, story, landmark, cover, what the player sees first |
| `palette` | which House or biome palette applies (section 3, item 3) |
| `reference` | 1 to 3 images (real places, art, Minecraft builds) |
| `fixed` | spawns, capture rings, stations, roads that enter and leave, with their edge heights |
| `status` | `planned`, `blocked-out`, `detailed`, `review`, `signed-off`, `locked` |
| `digest`, `signedBy`, `signedAt` | region digest at sign-off |

**Step 3: build each section in passes.** Blockout (terrain shape and footprints), then structure (pieces and
walls), then natural (erosion, edge breakup, scatter), then detail (small props, wear, lighting). Each pass is a plan,
gets a before/after render, and passes lint before it goes to review.

**Step 4: seams.** After two neighbours are signed off, a seam plan works only on the shared strip, so roads,
heights and palettes meet without a visible line.

**Step 5: whole-map check.** Walk, route, capture and bot checks (2A.5, 2A.7) plus a full top-down render. Kort
approves the map; then export (7.3).

**What Map Forge must add to support this.**

- **Map manifest** (`gg-mapforge-map/1`): map id, world, base digest, master layout file, section list as above,
  and the protect list (spawns, captures, stations) in the same file. This also answers 2A.3 question 2: the protect
  file lives inside the manifest, kept by whoever owns the map, changed only with Kort's approval.
- **Section-scoped plans.** A plan names `"section": "ironhold-s07"` and `"pass": "natural"`. Its allow lock is
  derived from the section bounds; the plan may only narrow it. A seam plan names both sections and gets only the
  strip. A plan cannot touch a `signed-off` or `locked` section unless it is a recorded reopen.
- **Region digests.** Today `expectWorld` is a hash of the whole world, so any edit in section 3 makes every
  pending plan for section 7 refuse. Add `expectRegion` (hash of the section box only). Sign-off stores it; any later
  change, by an agent or by Kort on the builder server, shows as drift on that section.
- **Status and history.** `mapforge status MAP` lists each section, its status, its last plan, and drift.
  `mapforge sign SECTION` records the digest and the review image path. Undo files are grouped per section.
- **Before and after renders per plan** (the `render` command below), saved next to the undo file, so every
  review is one image pair.

## 3. Tool improvements, ranked by value

Size: S under a day, M one to three days, L about a week. "must" means section work should not start without it.

| # | item | tag | size |
|---|---|---|---|
| 1 | **Render command.** `mapforge render WORLD --box ... --view top\|iso --out PNG`: top-down (1 px or 4 px a block, shaded by height, with a grid and the section, lock and road overlays) and isometric (fixed 45-degree view, simple face shading). Stdlib-only PNG writer. Block colours from one table generated from vanilla map colours. Before/after pair and a diff image (changed cells highlighted). This is the review image for every step and for Kort. | must | M |
| 2 | **Map manifest, section locks, region digests, status, sign-off** (section 2). | must | M |
| 3 | **Palettes and material rules.** Per biome and per House: weighted surface mixes (grass 70, coarse dirt 10, moss 8...), slope rules (steep goes stone or gravel), wall mixes (stone bricks with cracked, mossy, andesite), accent blocks, banned blocks (sea lantern, quartz on battle maps unless named). Ops take `"palette": "ironhold-field"` instead of one block; a noise picks weighted blocks so mixes clump instead of salt-and-pepper. | must | M |
| 4 | **Quality lint.** `mapforge lint PLAN --world`: flat areas over N blocks with one surface block, straight edges longer than 8 blocks between two materials, repeated identical blocks in a wall face, empty space (no cover or prop in a 16x16 tile of a contested zone), trees or props in a regular grid, floating blocks, blocks outside the palette, step heights a squad can not walk, sight lines from the manifest that got blocked. Warnings show on the render; must-fix items block `review`. | must | M |
| 5 | **Natural-terrain helpers.** Ops: `noise` (layered, seeded, masked to a shape), `erode` (simple thermal and water smoothing so slopes get gullies and ridges, not terraces), `breakup` (wobble a boundary between two materials or heights by a few blocks), `scatter` (place pieces or blocks by density with rules: min spacing, clustering, only on a slope range, stay N blocks off roads and capture rings, avoid a lock). Seeded, so dry-run stays deterministic. | must | L |
| 6 | **Path and road splines.** `path` op: control points, width, falloff, material palette with worn centre and ragged edge, grade limit (auto ramps or steps), optional kerb or ditch. Replaces straight `fill` roads and lets the manifest's routes become real paths. | should | M |
| 7 | **Detail pass helpers.** Prop pieces (crates, carts, fences, banners, lamps, rubble, dead trees) as small `.nbt` kits per House and biome, plus a `decorate` op that places a kit along an edge or around a point with variation (rotation, missing parts). Replaces the hash-placed props in `MapDressing`. | should | M |
| 8 | **Shape ops beyond boxes.** `cylinder`, `sphere`/blob, `line` (thick 3D line for beams, walls, ramps), `stairs` between two heights, `wall` along a polyline with crenels and damage profile. Today every shape is a box. | should | M |
| 9 | **Brief and reference kit.** A `section new` command that writes the brief template and a contact sheet (render of the section now, references, neighbours' edges). Agents start from it every time. | should | S |
| 10 | **Tree and rock library.** Several hand-authored `.nbt` trees and boulders per biome, in three sizes, placed by `scatter`. Formula trees are a large part of the "generated" look. | should | M |
| 11 | **Heightmap import.** Read a greyscale PNG as a `terrain` stamp, so Kort or an agent can paint the master height sketch in any paint tool. | should | S |
| 12 | **Sight line and route preview.** Draw manifest sight lines and routes on the render, and flag blocked lines; full bot routes remain 2A.5/2A.7. | should | S |
| 13 | **Builder-server pull.** Import a section box from the builder world (via 2A.2 `export`) so Kort's hand edits become the "before" for the next plan and show as drift. | later | M (needs 2A.2) |
| 14 | **Lighting check.** Render or lint dark spots at night (light level below 7 on paths). | later | S |
| 15 | **Plan includes and variables.** Reuse a sub-plan (a fence run, a ruin kit) with parameters, so plans stay short and readable. | later | S |
| 16 | **Shader-quality preview.** Real in-game screenshots from fixed cameras per section, through the existing harness. Needed for final sign-off, not for each pass. | later | M |

Suggested order: 1 and 2 together, then 3 and 4, then 5. Try the whole flow on one camp section (2A.6) before
Ironhold. Rough total for the must items: about 3 to 4 weeks of agent work.

## 4. Questions for Kort

1. **Section size.** (a) about 48x48, many small sections, most attention each; (b) about 64x64, my default;
   (c) by feature, not grid (a keep, a ford, a field), with uneven bounds.
2. **Who signs off a section.** (a) you sign every section; (b) an agent review signs blockout and detail
   passes, you sign only the final section and the master layout; (c) you sign the master layout and the
   whole map only.
3. **Review image you want to see.** (a) top-down render only; (b) top-down plus isometric; (c) both plus an
   in-game screenshot per section (slower, needs a client).
4. **Where the master layout comes from.** (a) the agent drafts it from the current map and your notes, you
   correct it; (b) you sketch it (paper photo or paint program), the agent turns it into the manifest;
   (c) start fresh from your map vision notes, ignoring today's layout.
5. **Existing generated maps.** (a) keep today's generator as the base and overlay sections on top (plan 7.2 as
   approved); (b) flatten each section to a neutral base first, then rebuild it fully by hand-planned passes
   (more work, cleanest result; may push toward the 2A.4 snapshot fallback).
6. **Style references.** (a) I collect a reference set per map for you to pick from; (b) you supply 3 to 5
   images per map; (c) one shared style guide for all maps plus per-House accents.
7. **First target.** (a) one camp section (lowest risk, 2A.6); (b) one Ironhold section (the slice map, 7.1);
   (c) the whole Ironhold master layout first, then its sections.
