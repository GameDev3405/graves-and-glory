# Title screen: The Hill

Direction "The Hill" (Kort, 2026-10-17); v3 "blocky" (Kort, 2026-10-18). A real battlefield frame from the game is a slowly drifting panorama. Pixel layers authored at GUI scale 1 sit in front of it:

- the hill, built from Minecraft blocks: a crag of bedded stone carrying the Dawn camp (two wool tents, a lantern post, a dead oak), and a grass plateau with a dirt path, a dropped shield and a broken spear;
- the commander: Steve, as the game's player model, in the Dawn commander's armour, a diamond sword raised in his right hand and the Dawn banner on its pole in his left;
- embers and ash rising off the battle.

The lockup sits over the sky, a short text menu with a brass marker sits bottom-left, and the version sits bottom-right.

| File | What |
|---|---|
| `title-hill-day.png` | golden hour, "Ride to camp" focused, 1920x1080 |
| `title-hill-night.png` | night: torches on the points, moon up, violet sky, same layout |
| `title-hill-hover.png` | day, "Play with friends" hovered: marker, brass line, horn cue |
| `title-banner-frames.png` | the 4 wind frames of the banner at 2x, over the panorama |
| `title-check.json` | the check.js result for each page, with the contrast of every text |
| `v2/` | the v2 pages (the hand-drawn hill and commander), kept for comparison |

Build, from this folder (Python 3 with Pillow and numpy; the render step uses the shared Chromium setup of `../render.py`):

```
python title_bg.py         # bg/pano-day.png, bg/pano-night.png from src/frame-b-clash.png
python title_commander.py  # sprites/commander-{day,night}-{0..3}.png, sprites/commander-at.json
python title_hill.py       # sprites/hill-day.png, sprites/hill-night.png (after the commander: he casts a shadow)
python title_ui.py         # sprites/lockup.png, marker.png, horn.png, embers-*.png, volley-*.png
python build_pages.py      # the three HTML pages (version from build/resources/main/gravesandglory-build.json)
cd .. && python title/render_title.py    # PNGs, check.js lint, title-check.json, the banner strip
```

`blocky.py` holds the blocks (16x16 textures and block shapes), the block world and the renderer that the hill and the commander share. `pixel.py` holds the shared helpers: the code-map canvas, polygon fill, polylines and save. `preview.py` and `preview_ui.py` write zoomed inspection sheets to the system temp folder, never into work/. `preview.py` still places the commander at the v2 spot, (608, 110); `build_pages.py` and `render_title.py` read his place from `sprites/commander-at.json`.

## v3: blocky

Kort on v2 (2026-10-18): "both of the title screens day and night both use pixel art that isn't minecraft, don't get me wrong it looks good but it's just not minecraft, make it blocky, make that steve at the top with a diamond sword or something."

v3 keeps the composition, the panorama and its drift, the volley, the embers, the lockup, the menu column and the version. Only the hill (L2) and the commander (L3) are new.

- **One block world, one camera.**
  - The hill is a world of whole blocks placed by hand, seen the way the game shows it: a level camera 3 blocks above the plateau and 25 blocks behind the commander.
  - The horizon is at y 208, by the panorama's haze line, and the vanishing point is right of the screen (x 1000). So every block shows its front and its right-hand side, and below the eye its top, as in an in-game screenshot.
  - Blocks by the commander are 64 px (a texel is 4 px). The crag across the drop is 100 blocks out, where a block is 16 px and a texel 1 px.
- **Textures.**
  - Every face shows one of 21 hand-typed 16x16 textures in `blocky.py`: grass top and side, dirt, coarse dirt, dirt path top and side, gravel, stone, andesite, cobblestone, oak log and its rings, stripped oak and its rings, oak planks (the fences), white and Dawn-teal wool, the lantern, short grass, allium, dead bush.
  - They are drawn in the game's manner: a few tones of hand-placed speckle per block, and stones with a lit top-left edge.
  - They are sampled nearest-texel, so a texel is always a square of whole pixels.
- **The renderer** (`blocky.py`) works out only where each texel lands and how much light it gets.
  - It casts one ray per GUI pixel through the block grid. The grid holds full cubes, the dirt path at 15/16, fence posts with rails where they meet, lanterns, crossed plants, and entity boxes for the props and the commander.
  - Light is the game's, worked out per texel:
    - sky light spreads from open sky, one level less per block, with smooth lighting and corner shading (ambient occlusion);
    - block light spreads from each lantern the same way (15 at the lantern), round corners and out through the tent doors;
    - the low sun (by night the moon) stands up and to the right beyond the hill and casts shadows, as a shader pack adds them; the commander and the props cast shadows too.
- **Day and night** are two palettes over the same codes. A code is a texture tone plus a light level and a warm level, so night is a palette and light swap of one drawing.
  - By day the ramps run from violet shadow to gold sun.
  - By night they run from indigo to silver moonlight, with the lanterns warm.
- **Steve.** `title_commander.py` builds the player model in skin pixels: head 8x8x8, body 8x12x4, arms and legs 4x12x4.
  - The armour is a second layer one pixel larger all round, as the game draws armour:
    - the helm: steel, a riveted brass band, the face open;
    - the chestplate: plate under the teal Dawn tabard with the brass sun, and a belt;
    - its arm pieces (the pauldrons) and the boots.
  - Jeans and skin show where the armour does not cover.
  - His right arm is turned 140 degrees up about the shoulder pivot, as the game turns a limb. His left arm swings 15 degrees forward to grip the pole.
  - Every face is a hand-typed grid, the face included (brown hair, white and violet eyes, the nose, the mouth). It is our own drawing of the default look, not the game's skin file.
- **The diamond sword** is a hand-typed 16x16 item, drawn at 3 GUI px per texel (items are drawn smaller in the hand). It stands upright in the plane of his fist, the guard just clear of his knuckles and the blade continuing the line of the arm.
- **The banner**:
  - an oak pole with an iron foot and a brass finial;
  - an oak crossbar with brass caps;
  - a 12x24 cloth: teal, the brass sun with eight rays, a light band, swallow tails.
- **How he is drawn**: a parallel view along the line the hill camera sees him on, so a skin pixel is exactly 4x4 GUI px. He stands on the hill's own feet position, (667, 400).
- **Checks the scripts assert**:
  - the body is identical in all four frames; only the cloth differs;
  - the hill covers the source frame's HUD panels;
  - under the menu and the version every hill pixel has a luminance of 0.066 or less (4.5:1 for the dimmest text there, the version's grey), with a spread of 0.01 or less;
  - embers and ash stay out of the lockup, the menu, the version and the commander's two zones: his body and banner, and his sword arm.

## Layers

Layers run back to front. Everything from L1 up is pixel art authored at GUI scale 1, so one art pixel lands on screen as a 2x2 block at GUI scale 2. Each sprite is painted as a map of material and tone codes and coloured by a day palette and a night palette. Night is a palette swap, not a second drawing.

| Layer | Kind | Asset (GUI px) | Motion |
|---|---|---|---|
| L0 panorama | static photo layer, a real frame of the game | 1920x1080 still (6 cube faces in the build) | sways ±8 GUI px (±1.5° of yaw on a cube map) over a 48 s eased back-and-forth; never the vanilla full spin, which would break the composition |
| L1 volley | parallax sprites | arrows 11 to 14 px long | a volley of 14 arrows about every 12 s, each sprite moved along its 1:2 flight line in code; moves with L0 because it flies over the field |
| L2 hill | parallax texture | 960x300 placed at (0, 240); 484 colours by day and 478 at night (block textures under 16 light levels and 6 warm levels) | 1.5x the L0 offset; the shipped texture is rendered 12 px wider on each side (984 wide), which only means casting more rays |
| L3 commander | parallax sprite | 192x300 placed at (588, 110), 4 frames, 84 colours | stands on the hill and moves with L2; the wind loop is below |
| L4 embers and ash | particles | 9 sprites: embers e1 to e6 (1x1 to 4x4 px), ash a1 to a3 | spawned in code at the feet of the plumes over the melee, rising up and right with the wind at 6 to 14 GUI px/s, shrinking as they climb and fading over 3 to 5 s; the still shows 50 embers and 18 ash by day, 80 embers and 12 ash at night; 2x the L0 offset |
| L5 UI | fixed | lockup 481x58 (6 colours), marker 15x7, horn 19x14, live text | none; the marker snaps to the hovered line |

Rules that apply across the layers:

- **Pixel snapping**: L1 to L4 move in whole GUI pixels, so they do not shimmer. L0 may move smoothly.
- **Calm ground under text**:
  - Embers and ash stay out of the lockup, the menu, the version and the commander (`KEEP_CLEAR` and `HERO` in `title_ui.py`; the script asserts it).
  - The menu sits on the crag face in shadow: courses of dirt over plain stone, backlit, under the camp.
  - The version sits on the dark dirt face of a one-block step at the plateau's corner.
  - `check_calm` in `title_hill.py` asserts it for both palettes. Under the menu and the version every hill pixel has a luminance of 0.066 or less, with a spread of 0.01 or less. Today the brightest pixel is 0.040 under the menu and 0.016 under the version by day, and 0.016 and 0.006 at night.
  - The camp's lanterns stand three blocks back from the crag's edge, so their light stays on the crest and off the face under the menu.
  - As a result, no text sits on busy or bright pixels.
- **Scene light** (the hill and the commander):
  - The low sun, and by night the moon, stands up and to the right, beyond the hill, as in the frame. The hill and the commander are backlit by it.
  - Faces toward the viewer sit in shadow: the crag face, the fronts of the tents and the commander's front. He is lit as the game lights entities, so his face stays readable.
  - Tops and right-hand faces take the light: the plateau's grass, the crag's crest, the right sides of the tents and the logs.
  - Everything upright casts a shadow toward us and to the left: the commander and his banner across the path, the spear, the tents and the tree.
  - The lanterns light the tent doors and the turf round the brink post, as block light does in the game. At night the warm light carries.
  - The banner cloth is lit through.
  - The ramps are hue-shifted: shadows lean violet by day and indigo at night.
- **UI light**: the lockup, the marker and the horn are lit from above.

## The hill, block by block

`title_hill.py` places every block by hand: typed plans, typed courses and typed lists. Nothing is scattered by noise or a random generator. The parts:

- **The plateau**:
  - A plan of 15 rows (depth) by 12 columns gives, per column, the top block and its height. The top blocks are:
    - grass;
    - the dirt path, running out to his feet;
    - patches of coarse dirt and gravel;
    - stone and andesite breaking out at the brink;
    - a step one block down at the right corner, under the version.
  - Each column is filled down: the top block, two of dirt, then stone.
  - Four tufts of short grass and an allium stand on it.
  - A lantern stands on a fence post at the lip, right of him.
- **The crag**, across the drop on the left:
  - 15 courses are typed from the top, 23 blocks each: grass, dirt, coarse dirt, stone, andesite and cobblestone.
  - A bed of andesite and a bed of mixed cobblestone and stone run low down.
  - The crest steps down once; the left shoulder stands a block higher.
  - Right of the menu, four blocks stand proud of the face and two are taken out.
- **The camp** on the crag, block by block:
  - the Dawn tent: white wool, three blocks wide, two high and three deep, under a teal wool ridge, with a lantern standing in its open door and a fence for its pole;
  - a second tent with the colours turned about (teal walls, white ridge) and a lantern in its door;
  - a lantern on a fence post between them;
  - the dead oak: four oak logs, two stripped logs laid sideways for boughs where the bark has gone, and fences for bare twigs, as players build them;
  - three dead bushes on the crest.
- **The props** are entity boxes with hand-typed faces:
  - the Dawn shield (teal field, brass sun, iron rim), fallen back against the turf with its foot sunk in the grass;
  - the broken spear: its butt end stuck in the ground with a teal rag below the splintered break, and its head and a stub of shaft lying by the path.

The commander is set out under "v3: blocky" above. Two textures repeat rather than being typed for each place: the pole's grain repeats an 8-row tile, and the white and the Dawn wool share one grid under two keys.

## Frame counts

| Asset | Frames | Timing |
|---|---|---|
| commander (banner) | 4 | see below |
| hill | 1 | static |
| arrows | 1 per length (11, 12, 13, 14 px), day and night | moved in code |
| embers and ash | 1 per sprite (9 sprites) | moved and faded in code |
| marker, horn | 1 each | shown on hover |
| panorama | 1 still (6 faces in the build) | drift in code |

The wind loop of the commander:

- Each of the 4 frames holds for 3 client ticks (150 ms), so the loop lasts 600 ms.
- A fold travels down the cloth, from the crossbar to the tails, one band of rows per frame. The tails swing a frame behind (follow-through).
- Only cloth moves: the body, the sword, the pole and the crossbar are identical in all four frames (`title_commander.py` asserts it).

Textures to ship, for each time of day:

- a 768x300 commander sheet (4 frames of 192x300);
- a 984x300 hill;
- 4 arrow sprites.

Shared between both times of day:

- the lockup, the marker and the horn;
- a particle sheet with 9 cells.

## Sounds

| When | Sound | Notes |
|---|---|---|
| title opens | ambient bed, looped: wind on the hill over a distant battle | new asset `gravesandglory:title_ambience`, not in sounds.json yet; the same bed plays at night |
| cursor moves to another line | soft tick: vanilla `ui.button.click`, volume 0.15, pitch 1.8 | fires as the marker moves |
| cursor enters "Play with friends" | `gravesandglory:horn_rally`, volume 0.35 | once per entry, 3 s cooldown; the horn icon is its picture |
| click "Ride to camp" | `gravesandglory:horn_charge` | then fade to the camp load |
| click any other line | vanilla `ui.button.click` | |
| a volley launches, then lands | vanilla `entity.arrow.shoot` (volume 0.2) at launch; a scatter of `entity.arrow.hit` (volume 0.1) about 1.5 s later | quiet and far: the arrows land out of frame |

## Type

The page uses three sizes, all from the v2 tokens in `title.css` (a copy of common.css with its paths fixed):

- **16 Marcellus**: "Ride to camp", the one primary action.
- **11 Marcellus**: the other four lines, and the tagline "Collect an army. Command the battlefield." The tagline is ivory with a 1 px dark shadow.
- **8 Minecraft**: the version, `v0.4.0 / be102e8b` in the current build, in muted grey. Its right edge sits 16 px from the screen edge, and it shares the baseline of "Quit".

Colour and weight carry the hierarchy:

- the menu lines are ivory;
- the hovered line is brass-hi `#dfbd77` and carries the brass spearhead;
- the version is muted `#afb9bc`.

The lockup "GRAVES and GLORY" is a 481x58 texture, not live type. It was built in three steps:

1. Marcellus was rasterised 1-bit at 56 px ("GRAVES", "GLORY") and 22 px ("and").
2. The letters were cut as brass: a 3-step ramp, a bevel on top edges, a 1 px outline and a hard 2 px shadow.
3. A ruled underline with lozenges was added.

The live text uses the Marcellus TTF the mod already ships (`assets/gravesandglory/font/title.json`).

## Layout (GUI px on 960x540)

- **Lockup**: top-left corner at (240, 24).
- **Tagline**: centred on x 480, top 88.
- **Sun** (day): centred at (896, 107), right of the lockup's right end (721), with its halo tightened, so the lockup reads first and the sun's rays fall toward the commander. The moon at night is unchanged, at (795, 56).
- **Menu**:
  - one text column at x 40;
  - line tops at 396, 424, 448, 472 and 496 (24 px apart, 28 under the larger first line);
  - 4 px of air round each line, 6 below for descenders.
- **Marker**: at x 20, centred on the capitals of the hovered line.
- **Horn**: 4 px right of "Play with friends".
- **Version**: right edge at 944, on the baseline of "Quit".
- **Commander**: sprite at (588, 110), 192x300; feet at (667, 400), in the right third; the sword tip at (591, 247), the finial at y 153.
- **Hill**:
  - the crag's crest runs at y 352 along the left shoulder (x 0 to 120) and at y 368 beyond it, to the crag's right end at x 328;
  - under the menu its face is dirt down to y 416, and stone below;
  - the plateau's brink runs at about y 393 by his feet; the plateau fills the lower right and runs off the bottom edge;
  - the step under the version is the plateau's bottom-right corner, one block down.

## Lint

`render_title.py` runs the shared `check.js` on each page:

| Page | Defects | Type sizes | Contrast below 4.5 | Lowest contrast |
|---|---|---|---|---|
| day | 0 | 8, 11, 16 | none | 6.04 (tagline over the sky; 5.4 before the sun moved) |
| night | 0 | 8, 11, 16 | none | 9.66 (the version) |
| hover | 0 | 8, 11, 16 | none | 6.04 (tagline over the sky) |

How the page is marked up for the lint:

- The pixel layers are art, not boxes. They are marked `data-bleed`, which exempts them from the edge and crossing checks.
- Every menu line is a control (`data-ctl`), so it is checked for:
  - clipping;
  - cover;
  - tight padding;
  - distance from the screen edge.

## Honesty

- **Source frame**:
  - The panorama comes from `run/screenshots/bannerbound-dark-fantasy-combat.png`. It is a real frame: Iris and Complementary, point B of the savanna field, Dawn and Dusk mid-clash.
  - It was copied to `src/frame-b-clash.png`.
  - It is scaled x1.5 and lowered 100 px.
  - The map, troops, flags and lanterns are never moved, cloned or painted over.
- **Painted sky**: the sky above the fog line is painted in `title_bg.py` (gradient, sun or moon, cloud slabs, stars). In the source frame the sky sits behind the HUD score bar.
- **Shader look**: `title_bg.py` grades the shader frame. The grade adds:
  - golden-hour split tone and haze;
  - bloom on the sun and the lanterns;
  - light shafts.
- **Night**: a regrade of the same frame, not a night capture. The grade is moonlit violet, with the lanterns relit as warm pools with short torch rays.
- **Capture plan**: the final panorama is to be captured in game with Complementary Reimagined on, at golden hour, 1920x1080 per face. The night panorama is the same camera after dark, with torches lit on the points.
- **Labels**: they follow the brief. `FrontMenuScreen.java` still says "Enter camp / Practice" and "Quit game", and draws the title as text. Building this means renaming those two buttons and drawing the lockup texture instead.
- **Version**: real. It is read from `build/resources/main/gravesandglory-build.json` and formatted the same way the game formats it.
- **Pixel art**:
  - All of it comes from the scripts in this folder, and nothing was retouched in an editor.
  - The hill and the commander are rendered by our own script (`blocky.py`) from blocks and boxes placed by hand.
  - Their textures are hand-typed in the game's manner. They are not the game's texture files, and the render is not an in-game capture.
  - The light copies the game's: sky and block light, smooth lighting and corner shading. The sun's cast shadows are what a shader pack adds, to match the shader-on panorama.
  - Steve's face and skin are our own drawing of the default look, not the game's skin file.
  - The hill could also be built in game and captured, which would replace the render with a real frame. The composition and the zones would stay.
  - The camp is smaller than v2's: two tents and a lantern post instead of three tents, so it stays a background read.
  - The embers and the volley are placed by hand and are unchanged from v2. No layer uses a random generator.
  - The 14 arrows of the volley keep the positions the round-one composition was approved with, now as a typed list.

## Self-score, v3 (rubric, 1 point per line)

| # | Line | Day | Night | Hover | Why |
|---|---|---|---|---|---|
| 1 | Legibility | 1 | 1 | 1 | lint clean; the ground under every menu line and the version is asserted dark and calm (`check_calm`); the tagline sits on clear sky |
| 2 | Information budget | 1 | 1 | 1 | unchanged: lockup, tagline, five lines and the version; the horn shows only on hover |
| 3 | Focal point | 0.5 | 1 | 0.5 | the lockup leads and Steve is the second read: a blocky figure against the lit field, the diamond sword raised on a clean diagonal, the teal banner on the third, and the camp small and set back; but by day the sun in the panorama (L0, unchanged in v3) is still the brightest spot |
| 4 | Hierarchy | 1 | 1 | 1 | three sizes and no boxes; size, brass and the marker carry it |
| 5 | Spacing and alignment | 1 | 1 | 1 | unchanged 4 px grid and one text column; the version sits on the baseline of "Quit"; nothing is within 12 px of the edge |
| 6 | Colour discipline | 1 | 1 | 1 | Dawn teal marks only Dawn things (banner, tabard, the tents' wool, the shield, the rag); the diamond's cyan is the game's own; lowest contrast 6.04 |
| 7 | Readable drama | 1 | 1 | 1 | the raised diamond sword, the clash, the volley and the embers read at a glance; the hovered line reads without its text |
| 8 | Feel | 1 | 1 | 1 | it now looks like the game: whole blocks with 16x16 textures in one perspective, Steve in plate with a diamond sword, wool tents with lanterns in their doors, a dead oak of logs and fences; the UI stays grim and heroic in brass and ivory |
| 9 | Honesty | 0.5 | 0.5 | 0.5 | real frame, real version, buildable layers; but the sky band is painted, the night is a regrade, and the hill is our render of a hand-placed block world, not an in-game capture |
| 10 | Flow | 1 | 1 | 1 | unchanged: one primary action, larger and focused by default; the three screens are one family |
| | **Total** | **9** | **9.5** | **9** | **average 9.2** |

These changes would lift the score, or the work still open:

- **Honesty**: swap in the in-game captures (Complementary Reimagined, golden hour, 1920x1080 per face, and the same camera after dark), or build the hill in game and capture it.
- **Focal point** (day): tone down the sun's bloom in `title_bg.py`. It is outside the v3 brief, which kept L0 as it was.
- **Feel**:
  - By day the embers are small against the pale field. In game, the moving plumes carry them; in the still they read only up close.
  - The crag face behind the menu is kept calm on purpose (asserted), so its blocks read flatter than the crest, the camp and the plateau.
  - The tents are the simplest wool tents, three blocks across; the shield still reads partly as a board.
