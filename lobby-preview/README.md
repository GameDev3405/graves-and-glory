# Banner Camp: lobby preview

A walkable, in-browser model of what the finished Graves and Glory lobby should look like, made for the builder.
Everything is built from Minecraft-style blocks (full blocks, slabs, stairs, walls, fences, panes, lanterns...), with the
nearest vanilla block named for every material, so it can be rebuilt in-game block for block.

It is a design reference, not game code: nothing in `src/main` (the mod) uses it.

## Run it

```bash
cd lobby-preview
./start.sh            # or: python3 -m http.server 8080
```

Then open <http://localhost:8080>. (Windows: double-click `start.bat`.) Chrome, Edge, Firefox and Safari work; it needs WebGL2.
Three.js is vendored in `vendor/`, so it works offline. The page loads the game's fonts from Google Fonts when online and
falls back to Georgia / system fonts when not.

## What you can do

| Key | Action |
|---|---|
| W A S D, Shift, Space | walk, sprint, jump (tap Space twice to fly, C to go down) |
| Mouse | look (click *Enter the camp* to capture the mouse) |
| E | talk to the NPC you are looking at; troops show a stat card automatically |
| Tab or M | top-down blueprint map with a grid in Minecraft coordinates, zone outlines, NPCs and landmarks. Click a zone for notes; double-click to teleport |
| Esc | Tour: every zone with what it does in the game, the builder's notes and a bill of materials |
| Click | copy the coordinates of the block under the crosshair (the HUD also names the vanilla block) |
| 1 .. 9, 0 | jump to the main areas |
| T | cycle time of day: dusk, golden hour, night, dawn, noon |
| B | bright mode (flat light, useful for counting blocks) |
| U | cycle troop displays: mixed / all unlocked / all locked |
| P | save a PNG screenshot |

Coordinates match a Minecraft world: the spawn court floor is **y 64**, the House Ring **y 70**, the Command Terrace **y 80**.
`x` runs east, `z` runs south, the spawn seal is at **x 0, z 30**, north is up on the blueprint.

## The layout in one paragraph

You arrive on the Hollow Seal in the lower court and look north up the Founders' Stair (with the 30-niche Chapter Wall)
to the House Ring, where the five Houses face a mosaic and the Muster Standard: Gilded Oathhall and Wildmoot Lodge to the
west, Tidecallers' Tidepost and the Graveborn Barrow Shrine to the east, the Ironclad Hold forge cut into the cliff in the
middle. Twin stairs climb either side of the Hold to the Command Terrace: the war tent with the relief war table and the
storybook, the Companions' Fire, the Commander's Gallery (six classes) and the great Hollow Standard. In the lower court the
Armory (Quartermaster), the Muster (Recruiter), the Archive (Pell, contracts, the Founders' Stone) and the Training Yard
(Drillmaster, START arch for battle 1) flank the avenue, which ends at the twin War Gates (Dawn teal, Dusk violet), the
Steward's tower and the battle-map board. Beyond the gates a road crosses a bridge toward five beacon towers, one per battlefield.

## How it is made

* `src/blocks.js`: block catalogue (each with its vanilla `mc` name) and a procedurally painted 16 px texture atlas. No game or Minecraft assets are used.
* `src/world.js`: voxel storage, a small building DSL (`Pen`), Minecraft-style sky/block light flood fill, smooth lighting and ambient occlusion, chunk mesher.
* `src/lobby/*.js`: the design itself, one file per area. Each area is plain block-placement code (`fill`, `walls`, `gable`, `disc`...), so it reads like a build plan.
* `src/figures.js`, `src/catalog.js`: the voxel troops, NPCs, beasts and siege engines. `src/data.js`: troop stats and NPC notes taken from `docs/superpowers/specs`.
* `src/entities.js`, `src/particles.js`, `src/ui.js`: signs, banners, labels, particles, HUD, tour and blueprint.

Colours and Houses follow the repo's design documents (Houses spec, story spec, UI spec). Everything beyond what those
documents fix is a proposal, and is called out as such in the builder's notes.
