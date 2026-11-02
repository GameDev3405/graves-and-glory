# Map design engine

Status: DRAFT for Kort's approval. Brainstormed with Kort on 2026-11-01.

## Why

Two problems with past Graves and Glory maps, Ironhold among them (Kort's words, summarised):

1. **The built map didn't match the picture in his head.** Getting it right took many rounds of explaining.
2. **Changes were slow.** Moving a capture point, a river or a building meant big rework instead of a quick edit.

Kort wants an engine for designing maps:
- terrain shape and height, rivers, roads, capture points, building spots and spawns;
- no block-by-block placing, but detailed control over shapes and heights;
- a way to hand the design to an AI, which builds detail on top of it.

Success means Kort can sit down, paint a map, walk it, change it cheaply, get AI detail he can review piece by piece, and end up with a built map that plays the way the design says.

## Decisions Kort made

- **Built fresh.** Don't build on the existing mapforge pipeline or the layout-marker format. They can be read for lessons, but the engine doesn't depend on them.
- **Two ways of working:** painting top-down and walking through it.
- **Screen:** one editor with two views of the same map. **Tab flips between them.**
  - **Map view (top-down):**
    - a big canvas;
    - a tool bar on the left: terrain, height, river, road, capture point, building, spawn;
    - a settings panel on the right for whatever is selected.
  - **3D walk view:**
    - the same area as a full-screen walk or fly-through, in block style;
    - the same tools work here, so Kort can nudge things while standing in the map.
- **The detail Kort controls by hand is shapes and heights:**
  - the outline of a hill and how steep it is;
  - a river's width and depth;
  - everything editable by dragging.

  Block placement, materials and fine detail are generated from those.
- **Every feature is its own editable object** (this river, this hill, point B). Moving one updates the map; nothing is baked in.
- **Approved extras** (proposed by Claude, confirmed by Kort):
  - undo and redo, plus saved versions to go back to;
  - a grid in real block units;
  - an optional panel with fairness and distance checks (spawn to point distances, balance warnings, open ground without cover);
  - fix notes made while walking in game that land back in the design.
- **AI handoff:**
  - Kort pins **notes on areas of the map** ("ruined chapel here, overgrown") and writes **one overall brief** for the map's mood.
  - The export bundles shapes, heights, objects, notes and the brief.
  - The AI returns a **detailed draft into the engine** (building interiors, terrain texture, props), not a built world.
- **Reviewing the draft:** AI additions are **highlighted, like tracked changes**. Kort accepts, rejects or edits each piece.
- **The Graves and Glory look** (all four chosen):
  - **Per-map palettes,** applied by zone (Ironhold stone, Frostmarch snow and so on).
  - **Signature buildings:** a library of approved buildings and ruins that the engine and the AI place, rather than inventing new ones each time.
  - **Worn, lived-in detail:** paths worn by use, rubble near walls, battle damage, overgrowth.
  - **Souls-like scale:** exaggerated landmarks, steep cliffs and looming silhouettes, not rolling Minecraft hills.
- **Tech is Claude's choice** (Kort: "You pick"). See below.

## Anti-goals (Kort)

The engine has failed, even if it technically works, if it is:
- **Fiddly to use:** too many menus or settings before he can just start painting.
- **Generic-looking:** built maps look like stock Minecraft terrain.
- **Different in game:** what looks right in the engine plays or builds differently in Minecraft.

## Non-goals

- Placing individual blocks by hand as the main way of working. Small fixes are fine.
- The AI building straight into the world without Kort reviewing its draft.
- Replacing the mod's own battle and objective code. The engine produces map data and worlds; the mod plays them.

## Claude's calls (Kort can override)

- **Platform:** a local app that opens in the browser, run from the repo with one command. No separate install beyond what the repo already needs (Python is present).
  - The 3D walk view is drawn in the browser (WebGL, for example three.js) as block-style voxels, at a level of detail that keeps a full map responsive.
- **Design file:** one human-readable design file per map, plus generated data.
  - The design file is the source of truth.
  - It holds features as objects: shape outlines and heights, rivers and roads as paths with width and depth, points with radius and rules, building placements referencing the library, spawns, notes, brief, palette per zone.
  - It also records who placed each object (Kort or the AI) and its review state.
- **Locking:** anything Kort places or edits is locked. The AI may add around it but never moves or changes it.
- **Into the game:** after Kort approves the draft, a deterministic builder (no AI) turns the design into a Minecraft world. The same design always gives the same blocks.
  - After building, it checks the built world against the design: spawn and point positions, distances, point sizes, walkable paths.
  - It reports any mismatch. This answers the "different in game" anti-goal.
- **AI export format:** the design file plus rendered top-down images and the notes, packaged so a Claude session can read and write the design file directly.
  - The AI's changes go back in as unaccepted objects for review.
- **Signature buildings:** stored as reusable pieces (structure files plus a footprint and a door side).
  - The library starts from buildings already approved for existing maps where possible.
  - New ones need Kort's approval before joining the library.

## Answers to the builder check

Some answers come from the repo; the rest are Claude's calls, marked (call).

- **What the mod loads today:**
  - Battlefields are 700 by 500 blocks.
  - `CampaignMaps.java` builds them from the sheet rows in `Spec` (Points, Structures, Scenery, Terrain), which `tools/preflight.py` generates, plus hand-written code per map.
  - Authored pieces are vanilla structure `.nbt` files in `data/gravesandglory/structures/GROUP/NAME.nbt`, loaded by `Structures.java`.
- **Engine output (call):**
  - **A map data file:** points (id, name, x, z, radius, rules), spawns, roads and routes, zones and palette.
  - **Structure `.nbt` pieces** for the terrain and buildings, in the existing structures layout.

  The mod gets a reader that builds a map from these instead of from code written per map. Nothing about gameplay changes: modes, objectives and spawn rules are untouched. Existing maps stay as they are until Kort chooses to redo one in the engine.
- **Map size and height:** the current 700 by 500 battlefield is the default canvas. The height range follows the existing maps around the y 64 ground level, with room above for Souls-like cliffs.
- **Writing blocks without the game (call):**
  - A Python builder writes the structure files.
  - It may reuse mapforge's NBT and structure reading and writing code as a library. "Built fresh" means fresh design and editor, not rewriting a file format.
  - The block palette is vanilla 1.20.1 block states.
  - Determinism: same design file in, same bytes out, with a test for it.
- **App packaging (call):**
  - No Node build step.
  - three.js is vendored into the repo at a pinned version, so the app works offline.
  - Python serves the app locally.
- **Building library:** starts from the approved pieces already in `data/gravesandglory/structures/`: 8 groups, among them ironhold, kingsfield, frostmarch, hollowwood, rivergate and camp.
- **AI draft rules (call):**
  - The design file has a schema the engine checks on load.
  - An AI edit that touches a locked object, or fails the schema, is rejected and listed in the review panel. It never applies silently.
- **In-game fix notes (call):**
  - Not in the first release.
  - They need a small dev-only command in the mod. That command comes later, as its own change for Kort to approve.

## Left to the builder

- The UI style details within the anti-goal "not fiddly": aim for painting within seconds of opening.
- How terrain is generated from outlines and heights (noise, erosion, smoothing), provided the Souls-like scale is achievable and shapes stay exactly where Kort drew them.
- Performance approach for the 3D view.
- How walk-mode fix notes are captured in Minecraft (a mod command or an item) and synced back.
- The order of building, and what a first usable version contains. A suggested first slice: paint shapes and heights, place points and spawns, flip to the 3D view, save and load, then build into a world and check it.

## Constraints

- The project's rules still apply:
  - Graves and Glory runs on Fabric 1.20.1;
  - follow the visual standards and art rules in docs/;
  - no push or publish;
  - Kort approves anything that changes gameplay or saves.
- Maps must fit the mod's existing battle modes, objectives and spawn rules. The builder must emit whatever the mod needs to run a battle on the map.
