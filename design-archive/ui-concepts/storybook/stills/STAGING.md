# Storybook stills: staging

These are real in-game frames. Each set was built in the running dev client by commands, then lit, dressed and
photographed through the game camera with the HUD, chat and crosshair hidden. Every frame is 1920x1080 and was shot
with shaders on. Nothing is painted over.

## Files

| Shot | Main | -alt1 | -alt2 |
|---|---|---|---|
| `fall-03-mound-night` | fov 40, looking up 7° | fov 45 | fov 50 |
| `north-crossing-dusk` | fov 50, time 12750 | fov 60, wider | fov 50 at time 12850, later dusk |
| `training-field-day` | fov 60 | fov 65 | the first take at fov 60: the far post still in the centre third, the map's own road post standing, the lead walker cut by the edge |
| `plate-order-crossings` | fov 60 | fov 50, soldiers larger | fov 70, wider |

- **`options/fall-03-mound-night-roundmoon.png`** is the main Fall frame with one non-default Complementary option:
  Sun/Moon Style "Unbound" (`SUN_MOON_STYLE_DEFINE=2`), which draws a round moon. It is not the shipped look; it is
  there for the artist to choose.
- **`sets/<shot>.cmd`** holds each shot's full command list (the lists below) as a harness control file.
  - The north list is the file that made the published frames.
  - The field, plate and Fall lists merge the files actually played, keeping only the lines the frames depend on.
    Each was then replayed whole in one session (sb3), and the replays match the published frames to within 1.5/255
    mean per-channel difference. The only differences are clouds and particles.

## How every shot was made

### Client

- The mod's dev client: `gradle runClient`, Minecraft 1.20.1 with Fabric.
- Each session ran in a fresh run root (`work/sb/sb2`, `work/sb/sb3`), with the run directory at `<root>/run`.
- Sessions were started by `work/sb/launch-sb.sh <name>`, which runs the StoryStills harness suite
  (`GRAVES_PHASE1_CHECKS=storystills`).

### Shaders

- Iris 1.7.6 and Sodium 0.5.13 go in `run/mods`.
- Complementary Reimagined r5.5.1 goes in `run/shaderpacks` and is enabled in `run/config/iris.properties`
  (`enableShaders=true`, `shaderPack=ComplementaryReimagined_r5.5.1.zip`).
- The pack's options are at their defaults, apart from the round-moon option above.
- The harness logs `shader=1` for every frame.

### Resource packs

- Only `vanilla`, `fabric` and the mod's built-in `gravesandglory:armory` are active. The client log line is
  `Reloading ResourceManager: vanilla, fabric (...), gravesandglory:armory`.
- No third-party packs: no Excalibur, Clarent, Fresh Animations or its patch, or Faithful.
- No EMF or ETF either.

### Options

- Window 1920x1080, with the GUI hidden for every shot.
- `pauseOnLostFocus:false` and `onboardAccessibility:false`.
- Graphics Fancy; render distance 12 for the shots (`rd 12`).
- Entity distance 500% (`entityDistanceScaling:5.0`) so that far actors are drawn.
- Particles All; view bobbing off; FOV effects off; sound off.

### The world

- `map N` starts an empty practice round on campaign map N:
  - the round's own units are discarded, with no training and no scores;
  - the day and weather cycles are frozen;
  - mobs do not spawn;
  - the player is a spectator held at the camera.
- `map N` played again rebuilds the map, but it removes no entities and does not undo every placed block. For
  example, the bridge lanterns survived a second `map 0`. So:
  - every set opens with `/kill @e[tag=set]`, and every actor carries the tag `set`;
  - "Could not set the block" for a block already in place is harmless.

### Actors

- Actors are mobs with `NoAI`, `Silent`, `Invulnerable` and `PersistenceRequired`.
- NoAI mobs have no gravity, so each one is summoned through
  `execute positioned X 100 Z positioned over motion_blocking_no_leaves run summon ...`. That stands it on the ground
  at X Z.
- `Rotation:[yaw, pitch]`: yaw 0 faces south (+z), 90 faces west, 180 faces north, and -90 faces east.

### The mod's models

A husk named `Dawn Knight` or `Dawn Sentinel` is drawn with the mod's Dawn texture (`dawn_guard.png`), as in the
game.

- **Order knights (the Gilded):** Dawn Knight in iron.
- **Maud, the Templar:** Dawn Sentinel.
- **The Hollow company's soldiers:** Dawn Knight in leather dyed navy (`2368580`) with an iron helmet. There is no
  separate Hollow model.

### The pose hook

`pose` holds a staged actor's head, arms and legs at set angles. It uses the harness mixin `StoryStillsPoseMixin`,
which is unshipped and lives in test-tools. Without it, a NoAI husk holds both arms straight out like a zombie.
Phase 4 needs its own way to pose actors, such as posed models or armour stands.

### Harness control-file lines

Files in `<root>/control/*.cmd` play in name order (`.cmd`, then `.run`, then `.done`, with a `.log` beside each).
Lines starting with `#` are skipped.

| Line | Effect |
|---|---|
| `/command` | Runs the command at op level 4 on the integrated server. |
| `map N` | Builds campaign map N and starts the empty practice round. |
| `wait N` | Waits N ticks. |
| `cam x y z yaw pitch` | Puts the eye at x y z (the spectator's feet are 1.62 below). Negative pitch looks up. |
| `look ex ey ez tx ty tz` | Puts the eye at E, aimed at T. |
| `fov N` | Minecraft's FOV setting, vertical, in degrees. |
| `rd N` | Sets the render distance. |
| `pose tag part pitch yaw roll ...` | Parts are `head`, `body`, `rarm`, `larm`, `rleg` and `lleg`; angles in degrees. |
| `unpose` | Clears the poses. |
| `shaderopt KEY=VALUE` | Writes the shader pack's option file and reloads Iris. `default` removes the key. |
| `probe x1 z1 x2 z2 step` | Prints the top block's y and name over a grid. |
| `shot name` | Saves `run/screenshots/name.png` and logs the time, fov, render distance, shader state and camera. |
| `quit` | Closes the client. |

To play a set, put its `sets/<shot>.cmd` in `work/sb/queue/` and run `bash work/sb/launch-sb.sh <new name>`. The
launcher moves the queue into the new root and waits for a free client slot.

---

## 1. fall-03-mound-night

**Map:** 4, Kingsfield Crossroads.

**Time:** 13855, the night of day 0. That is a full moon, due east and about 19° up, sitting behind the climber from
the camera.

**Weather:** clear.

**Camera:**

- **Main:** `cam -86 68.0 -45 -90 -7.0`. Facing due east, looking up 7°, fov 40. The eye is about 1 block above the
  field (the top grass block is at y 66 there).
- **Alt1:** pitch -4.5, fov 45.
- **Alt2:** pitch -3.6, fov 50.
- The mound, moon and climber sit in the upper two thirds. The foreground knights fall outside the caption strip
  (x 360-1560); only the standard's pole reaches into it.

**Ground (from `probe`):** the field's top blocks are at y 65-67. They are y 66-67 under the guard (whose feet are at
67) and y 65-66 under the Tide.

**The set, in the order built:**

1. **Clearing.** Trees go (logs and leaves; x -104..-9, y 64..95, z -70..-20, in 16-wide fills), along with the wild
   flowers in front of the camera (x -92..-60, y 65..68, z -62..-28).
2. **The highest mound,** centred on (-38, -45).
   - The base is dirt filled into air at y 63..65 under the footprint.
   - It has eight terraces, k = 0..7, each with half-width r = 18 - 2k: a coarse dirt course at y 66+2k, under a
     grass course at y 67+2k.
   - Each course is the union of three boxes with half-sizes (r, ⌈r/2⌉), (⌈r/2⌉, r) and (round(0.75r), round(0.75r)),
     so the plan is a stepped octagon.
   - The top course is grass at y 81, about 14 above the field.
3. **Tufts.** Grass, tall grass, fern and large fern cover about one cell in nine of each terrace ring (seeded
   random, seed 7; every setblock is in the list). The summit, and the step below it near the camera axis, are kept
   bare so the skyline behind the climber stays clean.
4. **The Tide** (in the story, the drowned are the Tide).
   - 55 drowned in six ranks, at x -74, -71, -68, -65, -62 and -59 (±0.3).
   - Files run every 3 blocks from z -58.5, with odd ranks shifted +1.5; the last rank only covers z -52..-38.
   - Yaw 90±8, facing west toward the camera, each with a trident.
5. **The climber (Wren with the Horn).**
   - A husk at (-41.5, 82, -45.0) on the summit's west edge, yaw 0 (in profile), with a goat horn in the main hand.
   - She wears a near-black leather cap and tunic (dye `2105376`).
   - Pose: head back 30°, both arms raised to the mouth.
6. **The guard:** four Order knights (Dawn Knight) in iron. All four pieces carry the House's gold trim
   (`Trim:{material:"minecraft:gold",pattern:"minecraft:host"}`). Backs to the camera:

   | Position | Yaw | Right hand | Left hand |
   |---|---|---|---|
   | (-80.5, -48.2) | -92 | lantern, held up | sword |
   | (-77.5, -50.6) | -95 | sword | shield |
   | (-79.5, -41.6) | -86 | sword | lantern, held up |
   | (-76.6, -39.6) | -88 | sword | shield |

7. **Lantern light.** Light blocks, level 10, sit at the two raised lanterns: (-80, 68, -48) and (-79, 68, -43).
8. **The Order standard.**
   - A dark oak fence at (-78, 67, -49), with a white banner at (-78, 68, -49).
   - Rotation 12: it faces east toward the mound, and the camera sees its back. A banner shows the same pattern on
     both faces.
   - Gold cross (`sc`) and gold border (`bo`).
9. **Finish.** `/kill @e[type=item]`, `/time set 13855`, `/weather clear`, the poses, `rd 12`, then the three
   framings.

**Staged for real:**

- the terraced mound;
- the night, the full moon and its position;
- the climber's silhouette against the moon;
- the ranks of the dead with pale glowing eyes;
- the knights, their trim, lanterns and standard;
- the low camera looking up.

**What could not be had, and the workaround:**

- **Low ground mist: not staged.** Complementary's atmospheric fog is cut by up to 75% when the camera is near or
  below its fog altitude, so a camera 1 block off the ground gets almost none. Its border fog is a hard wall at the
  render-distance edge. Raising `ATM_FOG_DISTANCE` (tried at 20 in the sb1 rehearsal) made no visible change, and
  campfire smoke reads as separate puffs, not a layer. The mist needs the paint-over, or a Phase 4 fog effect
  (particles, or a shader option set by the modpack).
- **The moon is square.** Complementary's default Sun/Moon Style draws the vanilla moon texture, which is a square
  disc. A round moon needs the non-default `SUN_MOON_STYLE_DEFINE=2` ("Unbound"); see `options/`.
- **The host of the dead** are vanilla drowned. Their "pale eyes" are Complementary's emissive eyes on the vanilla
  drowned texture, which glow a pale cyan. No resource pack is involved.
- **Holding something in both hands.** A mob holds an item in one hand only. The horn is in the main hand, and the
  pose hook brings the other hand up to it. The climber's vanilla husk skin is never seen; at that distance she is
  only a silhouette.
- **House colours (white enamel, gold trim, steel grey).**
  - The steel grey and gold trim are real: iron armour with gold trim.
  - The white enamel surcoat has no armour equivalent. Iron reads white and steel under the moon.
  - The standard carries the white and gold.
- **Carried lanterns.**
  - A held item gives no light, and with the arm hanging a held lantern is drawn flat and edge-on (it showed as a
    glowing sliver).
  - The two outer knights therefore hold their lantern up and forward, on the hand nearer the mound (pose
    `rarm -95 25 0`, and the mirror `larm -95 -25 0`).
  - Hidden light blocks at the lanterns do the lighting.
- **Arms.** The pose hook lowers the shield knights' arms: right arm 15° forward; left arm 25° forward and 10° in.

**Commands in order** (`sets/fall-03-mound-night.cmd`; consolidated from sb3 `20-fall3.cmd` with
`21-fall3-final.cmd` and replayed whole):

```
# Shot 1, fall-03-mound-night. Kingsfield (map 4), night, full moon. Camera low on the west side, looking due east and up.
# Consolidated from sb3 20-fall3.cmd (set) + 21-fall3-final.cmd (gold host trim), as played; see STAGING.md.
/kill @e[tag=set]
map 4
/kill @e[tag=set]
# clear the trees on the set (logs and leaves only) and the wild flowers in front of the camera
/fill -104 64 -70 -89 95 -20 air replace #minecraft:logs
/fill -104 64 -70 -89 95 -20 air replace #minecraft:leaves
/fill -88 64 -70 -73 95 -20 air replace #minecraft:logs
/fill -88 64 -70 -73 95 -20 air replace #minecraft:leaves
/fill -72 64 -70 -57 95 -20 air replace #minecraft:logs
/fill -72 64 -70 -57 95 -20 air replace #minecraft:leaves
/fill -56 64 -70 -41 95 -20 air replace #minecraft:logs
/fill -56 64 -70 -41 95 -20 air replace #minecraft:leaves
/fill -40 64 -70 -25 95 -20 air replace #minecraft:logs
/fill -40 64 -70 -25 95 -20 air replace #minecraft:leaves
/fill -24 64 -70 -9 95 -20 air replace #minecraft:logs
/fill -24 64 -70 -9 95 -20 air replace #minecraft:leaves
/fill -92 65 -62 -60 68 -28 air replace #minecraft:flowers
# the mound: eight terraces, each a coarse dirt course under a grass course, two high and two in; base filled solid
/fill -56 63 -54 -20 65 -36 dirt replace air
/fill -47 63 -63 -29 65 -27 dirt replace air
/fill -52 63 -59 -24 65 -31 dirt replace air
/fill -56 66 -54 -20 66 -36 coarse_dirt
/fill -47 66 -63 -29 66 -27 coarse_dirt
/fill -52 66 -59 -24 66 -31 coarse_dirt
/fill -56 67 -54 -20 67 -36 grass_block
/fill -47 67 -63 -29 67 -27 grass_block
/fill -52 67 -59 -24 67 -31 grass_block
/fill -54 68 -53 -22 68 -37 coarse_dirt
/fill -46 68 -61 -30 68 -29 coarse_dirt
/fill -50 68 -57 -26 68 -33 coarse_dirt
/fill -54 69 -53 -22 69 -37 grass_block
/fill -46 69 -61 -30 69 -29 grass_block
/fill -50 69 -57 -26 69 -33 grass_block
/fill -52 70 -52 -24 70 -38 coarse_dirt
/fill -45 70 -59 -31 70 -31 coarse_dirt
/fill -48 70 -55 -28 70 -35 coarse_dirt
/fill -52 71 -52 -24 71 -38 grass_block
/fill -45 71 -59 -31 71 -31 grass_block
/fill -48 71 -55 -28 71 -35 grass_block
/fill -50 72 -51 -26 72 -39 coarse_dirt
/fill -44 72 -57 -32 72 -33 coarse_dirt
/fill -47 72 -54 -29 72 -36 coarse_dirt
/fill -50 73 -51 -26 73 -39 grass_block
/fill -44 73 -57 -32 73 -33 grass_block
/fill -47 73 -54 -29 73 -36 grass_block
/fill -48 74 -50 -28 74 -40 coarse_dirt
/fill -43 74 -55 -33 74 -35 coarse_dirt
/fill -46 74 -53 -30 74 -37 coarse_dirt
/fill -48 75 -50 -28 75 -40 grass_block
/fill -43 75 -55 -33 75 -35 grass_block
/fill -46 75 -53 -30 75 -37 grass_block
/fill -46 76 -49 -30 76 -41 coarse_dirt
/fill -42 76 -53 -34 76 -37 coarse_dirt
/fill -44 76 -51 -32 76 -39 coarse_dirt
/fill -46 77 -49 -30 77 -41 grass_block
/fill -42 77 -53 -34 77 -37 grass_block
/fill -44 77 -51 -32 77 -39 grass_block
/fill -44 78 -48 -32 78 -42 coarse_dirt
/fill -41 78 -51 -35 78 -39 coarse_dirt
/fill -42 78 -49 -34 78 -41 coarse_dirt
/fill -44 79 -48 -32 79 -42 grass_block
/fill -41 79 -51 -35 79 -39 grass_block
/fill -42 79 -49 -34 79 -41 grass_block
/fill -42 80 -47 -34 80 -43 coarse_dirt
/fill -40 80 -49 -36 80 -41 coarse_dirt
/fill -41 80 -48 -35 80 -42 coarse_dirt
/fill -42 81 -47 -34 81 -43 grass_block
/fill -40 81 -49 -36 81 -41 grass_block
/fill -41 81 -48 -35 81 -42 grass_block
# tufts on each terrace ring (about one cell in nine)
/setblock -24 68 -58 large_fern[half=lower]
/setblock -24 69 -58 large_fern[half=upper]
/setblock -42 68 -63 grass
/setblock -24 68 -55 fern
/setblock -29 68 -28 grass
/setblock -53 68 -36 grass
/setblock -31 68 -28 tall_grass[half=lower]
/setblock -31 69 -28 tall_grass[half=upper]
/setblock -52 68 -35 large_fern[half=lower]
/setblock -52 69 -35 large_fern[half=upper]
/setblock -55 68 -41 grass
/setblock -44 68 -62 grass
/setblock -38 68 -63 fern
/setblock -24 68 -34 tall_grass[half=lower]
/setblock -24 69 -34 tall_grass[half=upper]
/setblock -47 68 -60 grass
/setblock -43 68 -27 large_fern[half=lower]
/setblock -43 69 -27 large_fern[half=upper]
/setblock -22 68 -36 grass
/setblock -51 68 -54 grass
/setblock -22 68 -54 grass
/setblock -28 68 -31 tall_grass[half=lower]
/setblock -28 69 -31 tall_grass[half=upper]
/setblock -45 68 -27 grass
/setblock -25 68 -54 grass
/setblock -21 68 -48 large_fern[half=lower]
/setblock -21 69 -48 large_fern[half=upper]
/setblock -36 68 -63 fern
/setblock -21 68 -43 grass
/setblock -56 68 -46 tall_grass[half=lower]
/setblock -56 69 -46 tall_grass[half=upper]
/setblock -55 68 -48 large_fern[half=lower]
/setblock -55 69 -48 large_fern[half=upper]
/setblock -48 68 -31 grass
/setblock -55 68 -36 grass
/setblock -20 68 -49 grass
/setblock -53 70 -52 tall_grass[half=lower]
/setblock -53 71 -52 tall_grass[half=upper]
/setblock -41 70 -30 grass
/setblock -54 70 -42 grass
/setblock -23 70 -41 grass
/setblock -22 70 -47 large_fern[half=lower]
/setblock -22 71 -47 large_fern[half=upper]
/setblock -33 70 -61 grass
/setblock -42 70 -29 grass
/setblock -44 70 -61 grass
/setblock -24 70 -37 fern
/setblock -33 70 -60 grass
/setblock -29 70 -33 grass
/setblock -26 70 -34 grass
/setblock -46 70 -57 grass
/setblock -49 70 -35 tall_grass[half=lower]
/setblock -49 71 -35 tall_grass[half=upper]
/setblock -30 70 -58 grass
/setblock -32 70 -29 grass
/setblock -45 70 -29 fern
/setblock -26 70 -55 tall_grass[half=lower]
/setblock -26 71 -55 tall_grass[half=upper]
/setblock -47 70 -56 grass
/setblock -46 70 -34 fern
/setblock -48 70 -56 grass
/setblock -49 70 -57 grass
/setblock -46 70 -30 fern
/setblock -37 70 -61 large_fern[half=lower]
/setblock -37 71 -61 large_fern[half=upper]
/setblock -45 72 -33 grass
/setblock -52 72 -39 large_fern[half=lower]
/setblock -52 73 -39 large_fern[half=upper]
/setblock -51 72 -45 grass
/setblock -50 72 -52 grass
/setblock -34 72 -32 large_fern[half=lower]
/setblock -34 73 -32 large_fern[half=upper]
/setblock -31 72 -31 large_fern[half=lower]
/setblock -31 73 -31 large_fern[half=upper]
/setblock -45 72 -32 tall_grass[half=lower]
/setblock -45 73 -32 tall_grass[half=upper]
/setblock -44 72 -59 tall_grass[half=lower]
/setblock -44 73 -59 tall_grass[half=upper]
/setblock -24 72 -49 tall_grass[half=lower]
/setblock -24 73 -49 tall_grass[half=upper]
/setblock -40 72 -31 grass
/setblock -31 72 -33 tall_grass[half=lower]
/setblock -31 73 -33 tall_grass[half=upper]
/setblock -51 72 -41 large_fern[half=lower]
/setblock -51 73 -41 large_fern[half=upper]
/setblock -24 72 -45 grass
/setblock -45 72 -56 grass
/setblock -36 72 -31 fern
/setblock -40 72 -58 large_fern[half=lower]
/setblock -40 73 -58 large_fern[half=upper]
/setblock -51 72 -52 large_fern[half=lower]
/setblock -51 73 -52 large_fern[half=upper]
/setblock -31 72 -32 grass
/setblock -32 74 -33 grass
/setblock -38 74 -56 tall_grass[half=lower]
/setblock -38 75 -56 tall_grass[half=upper]
/setblock -29 74 -36 fern
/setblock -33 74 -57 large_fern[half=lower]
/setblock -33 75 -57 large_fern[half=upper]
/setblock -26 74 -39 tall_grass[half=lower]
/setblock -26 75 -39 tall_grass[half=upper]
/setblock -42 74 -34 grass
/setblock -32 74 -36 grass
/setblock -34 74 -33 large_fern[half=lower]
/setblock -34 75 -33 large_fern[half=upper]
/setblock -50 74 -46 grass
/setblock -49 74 -42 grass
/setblock -26 74 -46 grass
/setblock -47 74 -52 fern
/setblock -49 74 -43 tall_grass[half=lower]
/setblock -49 75 -43 tall_grass[half=upper]
/setblock -50 74 -43 fern
/setblock -26 74 -41 grass
/setblock -46 76 -37 large_fern[half=lower]
/setblock -46 77 -37 large_fern[half=upper]
/setblock -35 76 -54 grass
/setblock -33 76 -37 large_fern[half=lower]
/setblock -33 77 -37 large_fern[half=upper]
/setblock -47 76 -40 tall_grass[half=lower]
/setblock -47 77 -40 tall_grass[half=upper]
/setblock -42 76 -35 grass
/setblock -47 76 -43 large_fern[half=lower]
/setblock -47 77 -43 large_fern[half=upper]
/setblock -36 76 -36 tall_grass[half=lower]
/setblock -36 77 -36 tall_grass[half=upper]
/setblock -29 76 -42 grass
/setblock -30 76 -40 grass
/setblock -28 76 -47 grass
/setblock -43 76 -55 large_fern[half=lower]
/setblock -43 77 -55 large_fern[half=upper]
/setblock -44 76 -53 fern
/setblock -29 76 -46 grass
/setblock -28 76 -48 grass
/setblock -48 76 -47 tall_grass[half=lower]
/setblock -48 77 -47 tall_grass[half=upper]
/setblock -46 78 -41 grass
/setblock -34 78 -40 grass
/setblock -36 78 -38 grass
/setblock -38 78 -38 large_fern[half=lower]
/setblock -38 79 -38 large_fern[half=upper]
/setblock -31 78 -43 fern
/setblock -30 78 -45 large_fern[half=lower]
/setblock -30 79 -45 large_fern[half=upper]
/setblock -45 78 -42 grass
/setblock -39 78 -37 grass
/setblock -34 78 -39 grass
/setblock -41 78 -53 grass
/setblock -45 78 -49 tall_grass[half=lower]
/setblock -45 79 -49 tall_grass[half=upper]
/setblock -33 80 -44 grass
/setblock -36 80 -50 tall_grass[half=lower]
/setblock -36 81 -50 tall_grass[half=upper]
/setblock -41 80 -41 grass
/setblock -37 80 -50 grass
/setblock -40 80 -51 grass
/setblock -41 80 -51 grass
/setblock -36 80 -51 grass
/setblock -37 82 -45 grass
/setblock -41 82 -44 tall_grass[half=lower]
/setblock -41 83 -44 tall_grass[half=upper]
/setblock -40 82 -44 grass
/setblock -34 82 -47 tall_grass[half=lower]
/setblock -34 83 -47 tall_grass[half=upper]
/setblock -39 82 -43 large_fern[half=lower]
/setblock -39 83 -43 large_fern[half=upper]
/setblock -38 82 -44 fern
/setblock -37 82 -44 grass
# keep the skyline behind the climber clean: no tufts on the top or on the step below it near the camera axis
/fill -42 82 -49 -34 83 -41 air
/fill -44 80 -48 -42 81 -42 air
# the Tide: drowned in six rows facing west toward the guard, tridents in hand
/execute positioned -74.25 100 -58.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[92.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -73.88 100 -55.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[84.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -74.13 100 -52.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[88.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -73.73 100 -49.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[86.9f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -73.96 100 -46.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[87.0f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -74.05 100 -43.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[87.7f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -73.70 100 -40.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[95.8f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -74.18 100 -37.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[87.8f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -74.18 100 -34.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[93.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -73.76 100 -31.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[82.1f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -70.81 100 -57.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[88.8f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -70.77 100 -54.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[88.5f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -71.20 100 -51.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[89.4f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -70.97 100 -48.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[82.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -70.75 100 -45.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[92.3f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -70.93 100 -42.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[83.4f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -71.00 100 -39.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[87.9f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -71.13 100 -36.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[84.3f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -70.74 100 -33.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[90.3f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -71.01 100 -30.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[83.7f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -67.72 100 -58.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[94.9f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -68.22 100 -55.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[85.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -67.71 100 -52.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[97.1f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -68.27 100 -49.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[89.7f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -68.07 100 -46.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[96.8f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -67.93 100 -43.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[96.5f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -68.20 100 -40.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[95.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -68.17 100 -37.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[94.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -67.79 100 -34.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[88.5f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -68.19 100 -31.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[95.3f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -65.06 100 -57.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[85.5f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -65.07 100 -54.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[90.3f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -65.15 100 -51.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[84.0f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -64.76 100 -48.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[93.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -64.96 100 -45.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[82.7f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -65.28 100 -42.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[94.1f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -65.23 100 -39.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[95.4f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -64.97 100 -36.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[91.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -65.12 100 -33.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[92.0f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -64.95 100 -30.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[88.7f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -61.90 100 -58.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[88.8f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.04 100 -55.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[89.1f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -61.93 100 -52.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[82.4f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.16 100 -49.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[89.8f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -61.83 100 -46.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[94.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.19 100 -43.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[89.3f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.24 100 -40.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[89.6f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.04 100 -37.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[84.1f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.03 100 -34.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[83.5f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -62.28 100 -31.50 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[90.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -59.25 100 -51.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[92.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -58.83 100 -48.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[93.7f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -59.27 100 -45.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[90.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -59.07 100 -42.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[90.1f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
/execute positioned -59.22 100 -39.00 positioned over motion_blocking_no_leaves run summon drowned ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["tide","set"],Rotation:[97.2f,0f],HandItems:[{id:"minecraft:trident",Count:1b},{}],HandDropChances:[0f,0f]}
# the climber on the top's west edge, in profile (facing south), the horn in both hands at the mouth
/summon husk -41.5 82 -45.0 {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,Tags:["climber","set"],Rotation:[0f,-25f],HandItems:[{id:"minecraft:goat_horn",Count:1b},{}],ArmorItems:[{},{},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2105376}}},{id:"minecraft:leather_helmet",Count:1b,tag:{display:{color:2105376}}}]}
# the Gilded guard (Order knights: the Dawn Knight model) in iron with gold trim, backs to the camera; the two outer
# knights hold a lantern up on the hand nearer the mound (a held item lies flat when the arm hangs)
/execute positioned -80.5 100 -48.2 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["guard","guardr","set"],Rotation:[-92f,0f],HandItems:[{id:"minecraft:lantern",Count:1b},{id:"minecraft:iron_sword",Count:1b}],ArmorItems:[{id:"minecraft:iron_boots",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_leggings",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_chestplate",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_helmet",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}}]}
/execute positioned -77.5 100 -50.6 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["guard","guards","set"],Rotation:[-95f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{id:"minecraft:shield",Count:1b}],ArmorItems:[{id:"minecraft:iron_boots",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_leggings",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_chestplate",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_helmet",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}}]}
/execute positioned -79.5 100 -41.6 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["guard","guardlh","set"],Rotation:[-86f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{id:"minecraft:lantern",Count:1b}],ArmorItems:[{id:"minecraft:iron_boots",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_leggings",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_chestplate",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_helmet",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}}]}
/execute positioned -76.6 100 -39.6 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["guard","guards","set"],Rotation:[-88f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{id:"minecraft:shield",Count:1b}],ArmorItems:[{id:"minecraft:iron_boots",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_leggings",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_chestplate",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_helmet",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}}]}
# the lanterns' light: hidden light blocks (level 10) at the raised lanterns
/setblock -80 68 -48 light[level=10]
/setblock -79 68 -43 light[level=10]
# the Order standard: white field, gold cross and border, on a single dark oak post left of the mound
/setblock -78 67 -49 dark_oak_fence
/setblock -78 68 -49 white_banner[rotation=12]{Patterns:[{Pattern:"sc",Color:4},{Pattern:"bo",Color:4}]}
/kill @e[type=item]
/time set 13855
/weather clear
pose climber head -30 0 0 rarm -105 -15 0 larm -95 45 0
rd 12
pose guardr rarm -95 25 0 larm -15 0 -4
pose guardlh larm -95 -25 0 rarm -15 0 4
pose guards rarm -15 0 0 larm -25 10 0
fov 40
cam -86 68.0 -45 -90 -7.0
wait 240
shot fall-03-mound-night
fov 45
cam -86 68.0 -45 -90 -4.5
wait 60
shot fall-03-mound-night-alt1
fov 50
cam -86 68.0 -45 -90 -3.6
wait 60
shot fall-03-mound-night-alt2
# option only (not the shipped look): Complementary's round 'Unbound' moon, then back to the default
shaderopt SUN_MOON_STYLE_DEFINE=2
wait 160
fov 40
cam -86 68.0 -45 -90 -7.0
wait 60
shot fall-03-mound-night-roundmoon
shaderopt SUN_MOON_STYLE_DEFINE=default
wait 160
```

---

## 2. north-crossing-dusk

**Map:** 0, Rivergate Valley. The setting is the north crossing, a wood bridge at x -32..10, z -153..-137, with the
camp on the east bank at about (20, -134).

**Time:** 12750, dusk (alt2: 12850).

**Weather:** clear.

**Camera:**

- **Main:** `look 25.5 65.9 -125.0 20.5 66.3 -133.5`, which gives yaw 149.53, pitch -2.32, at fov 50.
  - The fire is a little below centre (about x 960, y 650).
  - Maud and the fire sit above the dialog card (x 556-1364, y 752-960) and clear of the 96 px bars.
- **Alt1:** `look 25.0 65.8 -125.8 20.5 66.2 -133.5`, which gives yaw 149.70, pitch -2.57, at fov 60.
- **Alt2:** the main camera at time 12850.

**The set:**

1. **Bridge lanterns.** A lantern on every log post along both edges of the bridge: y 66, x -32..8 in steps of 4, at
   z -153 and z -138 (22 lanterns).
2. **Clearing.** Wild flowers cleared (x 4..40, y 65..67, z -146..-110).
3. **The fire and seat.** A lit campfire at (20, 65, -134), and a spruce log (axis z) at (22, 65, -135) as a seat.
4. **Maud, the Gilded Templar (Dawn Sentinel).**
   - She rides an invisible marker armour stand at (22.5, 65.7, -134.5), so she sits on the log.
   - Yaw 34.5, which turns her toward the camera's left in a three-quarter turn. Head pitched 15° down.
   - She holds a sheet of paper (the pact-copy).
   - She wears iron boots, leggings and chestplate with gold `host` trim, and no helm, so her face shows.
   - Pose: head 25° down, both arms up holding the paper.
5. **Two Hollow soldiers** across the fire, facing it: (18.66, -134.85) at yaw 317.5, and (20.02, -135.89) at yaw
   343.5. Each has a sword, with arms lowered.
6. **The Hollow standard.**
   - Dark oak fence posts at (18, 65, -132) and (18, 66, -132).
   - A blue banner with a red roundel (`mc`, red) at (18, 67, -132).
   - Rotation 14: it faces the camera.
7. **The sentry** on the bridge at (2.5, -140.5), yaw 80.

**Staged for real:**

- the dusk sky;
- the lit lanterns along the bridge;
- the campfire, its light and smoke;
- the log, Maud, the two soldiers, the standard and the sentry.

**Workarounds:**

- **Sitting.** A mob cannot sit. A marker armour stand carries Maud as a passenger: the rider's hips sit about 0.3
  above the stand, so a stand at y 65.7 seats her on a log at y 65.
- **The navy and red seal.** Vanilla has no navy dye, so the company's colours are a blue banner with a red roundel
  and navy-dyed leather.
- **Smoke.** The smoke column varies from frame to frame (particles).
- **Poses** are from the pose hook.

**Commands in order** (`sets/north-crossing-dusk.cmd`; as played in sb3 `22-north3.cmd`):

```
# Shot 2, north-crossing-dusk. Rivergate (map 0), the north crossing at dusk. Consolidated from sb3 22-north3.cmd, as played.
# Shot 2, north-crossing-dusk: Rivergate (map 0), the north crossing (wood bridge, x -32..10, z -153..-137) at dusk,
# camp on the east bank: a fire, Maud seated on a log reading the pact-copy, two Hollow soldiers across the fire,
# the Hollow standard on a pole, lanterns on every bridge post and a sentry on the bridge. Camera looks north-west.
map 0
/kill @e[tag=set]
# lanterns on top of every log post along both edges of the bridge
/setblock -32 66 -153 lantern
/setblock -32 66 -138 lantern
/setblock -28 66 -153 lantern
/setblock -28 66 -138 lantern
/setblock -24 66 -153 lantern
/setblock -24 66 -138 lantern
/setblock -20 66 -153 lantern
/setblock -20 66 -138 lantern
/setblock -16 66 -153 lantern
/setblock -16 66 -138 lantern
/setblock -12 66 -153 lantern
/setblock -12 66 -138 lantern
/setblock -8 66 -153 lantern
/setblock -8 66 -138 lantern
/setblock -4 66 -153 lantern
/setblock -4 66 -138 lantern
/setblock 0 66 -153 lantern
/setblock 0 66 -138 lantern
/setblock 4 66 -153 lantern
/setblock 4 66 -138 lantern
/setblock 8 66 -153 lantern
/setblock 8 66 -138 lantern
/fill 4 65 -146 40 67 -110 air replace #minecraft:flowers
# the fire, and one log for a seat
/setblock 20 65 -134 campfire[lit=true,facing=north]
/setblock 22 65 -135 spruce_log[axis=z]
# Maud (the Gilded Templar: the Dawn Sentinel model, helm off, iron kit with the House's gold trim) rides an invisible marker on the log so she sits;
# she faces the camera's left, toward the fire, reading the pact-copy
/summon armor_stand 22.5 65.7 -134.5 {Marker:1b,Invisible:1b,NoGravity:1b,Tags:["seat","set"],Passengers:[{id:"minecraft:husk",NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Sentinel"}',Tags:["maud","set"],Rotation:[34.5f,15f],HandItems:[{id:"minecraft:paper",Count:1b},{}],ArmorItems:[{id:"minecraft:iron_boots",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_leggings",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{id:"minecraft:iron_chestplate",Count:1b,tag:{Trim:{material:"minecraft:gold",pattern:"minecraft:host"}}},{}]}]}
# two Hollow soldiers (the Dawn Knight model in the company's navy) across the fire, facing it
/execute positioned 18.66 90 -134.85 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["hollow","set"],Rotation:[317.5f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/execute positioned 20.02 90 -135.89 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["hollow","set"],Rotation:[343.5f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
# the Hollow standard: navy (blue) with the red seal (a roundel), on a two-post pole left of the fire, facing the camera
/setblock 18 65 -132 dark_oak_fence
/setblock 18 66 -132 dark_oak_fence
/setblock 18 67 -132 blue_banner[rotation=14]{Patterns:[{Pattern:"mc",Color:14}]}
# a sentry on the bridge's near half, watching the far bank
/execute positioned 2.50 90 -140.50 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["sentry","set"],Rotation:[80.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/kill @e[type=item]
/time set 12750
/weather clear
pose hollow rarm -20 0 0 larm -10 0 0
pose sentry rarm -10 0 0 larm -5 0 0
pose maud head 25 0 0 rarm -55 -20 0 larm -50 25 0
rd 12
fov 50
look 25.5 65.9 -125.0 20.5 66.3 -133.5
wait 200
shot north-crossing-dusk
fov 60
look 25.0 65.8 -125.8 20.5 66.2 -133.5
wait 60
shot north-crossing-dusk-alt1
/time set 12850
fov 50
look 25.5 65.9 -125.0 20.5 66.3 -133.5
wait 60
shot north-crossing-dusk-alt2
```

---

## 3. training-field-day

**Map:** 0, Rivergate Valley. The east road into the centre crossing (the stone bridge at x 0, z 0), in the morning.

**Time:** 3500. **Weather:** clear.

**Camera:**

- **Main:** `look 64.0 66.4 7.5 0.0 66.5 -1.0`, which gives yaw 97.57, pitch -0.09, at fov 60.
- **Alt1:** the same camera at fov 65.

**Layout:**

- The left third holds the lantern posts.
- The right third holds the soldiers.
- The centre third, under the march card (x 560-1360), holds only the empty road and the far bridge.

**The set:**

1. **Clearing.** Wild flowers cleared (x 40..70, y 65..66, z -6..14).
2. **Lantern post A** (two dark oak fences and a lantern) at (58, 65..67, 10).
3. **The map's own road post** at (53, 65..67, 7) is struck, because it stood in the centre third. Its lantern drops
   and is cleared.
4. **Lantern post B** at (51, 64..66, 12). The ground is one lower there (top block y 63), so it is stacked through
   `positioned over`.
5. **Four Hollow soldiers** on the road, walking toward the camera: (58.63, 2.35) at yaw -96, (55.70, 1.64) at yaw
   -86, (53.12, -1.71) at yaw -93, and (50.06, -1.47) at yaw -83.

**Staged for real:** the crossings in daylight, the lantern posts, the road and the soldiers.

**Workarounds:**

- **Walking.** NoAI mobs stand still, so the stride is a pose: legs ±26-28°, arms swinging against them, in two
  phases (`walka` and `walkb`) so the line does not move in step.
- **Alt2** is the first take (sb2 `12-day.cmd`):
  - post B was at (51, 65..67, 10), which falls in the centre third;
  - the map's post was still standing;
  - the lead walker was at (58.68, 2.00), and its sword is cut by the frame.

**Commands in order** (`sets/training-field-day.cmd`; consolidated from sb2 `12-day.cmd` with `13-field2.cmd` and
replayed whole):

```
# Shot 3, training-field-day. Rivergate (map 0), the east road into the centre crossing, morning.
# Consolidated from sb2 12-day.cmd (first half) + 13-field2.cmd, as played.
map 0
/kill @e[tag=set]
/time set 3500
/weather clear
/fill 40 65 -6 70 66 14 air replace #minecraft:flowers
# lantern post A on the left verge (5 ahead, 3.5 left of the camera axis)
/setblock 58 65 10 dark_oak_fence keep
/setblock 58 66 10 dark_oak_fence keep
/setblock 58 67 10 lantern keep
# strike the map's own road post at 53 65..67 7 (it stood in the centre third; its lantern drops and is cleared below)
/fill 44 64 6 58 70 9 air replace dark_oak_fence
# lantern post B (12 ahead, 6.5 left): block 51 12, stacked on the ground, which is one lower there (top block y 63)
/execute positioned 51 90 12 positioned over motion_blocking_no_leaves run setblock ~ ~ ~ dark_oak_fence
/execute positioned 51 90 12 positioned over motion_blocking_no_leaves run setblock ~ ~ ~ dark_oak_fence
/execute positioned 51 90 12 positioned over motion_blocking_no_leaves run setblock ~ ~ ~ lantern
# four Hollow soldiers walking the road toward the camera in the right third
/execute positioned 58.63 90 2.35 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["walka","set"],Rotation:[-96.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/execute positioned 55.70 90 1.64 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["walkb","set"],Rotation:[-86.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/execute positioned 53.12 90 -1.71 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["walka","set"],Rotation:[-93.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/execute positioned 50.06 90 -1.47 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["walkb","set"],Rotation:[-83.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/kill @e[type=item]
pose walka rleg 28 0 0 lleg -28 0 0 rarm -24 0 0 larm 26 0 0
pose walkb rleg -26 0 0 lleg 26 0 0 rarm 22 0 0 larm -24 0 0
rd 12
fov 60
look 64.0 66.4 7.5 0.0 66.5 -1.0
wait 120
shot training-field-day
fov 65
look 64.0 66.4 7.5 0.0 66.5 -1.0
wait 60
shot training-field-day-alt1
```

---

## 4. plate-order-crossings

**Map:** 0, Rivergate Valley. The north crossing by day, seen from the east bank, looking west down the bridge.

**Time:** 4000. **Weather:** clear.

**Camera:**

- **Main:** `look 24.0 65.9 -145.5 -20.0 66.0 -145.5`, which gives yaw 90.00, pitch -0.13, at fov 60.
- **Alt1:** `look 24.8 65.8 -145.5 -20.0 66.1 -145.5` at fov 50.
- **Alt2:** `look 23.6 66.0 -145.5 -20.0 65.8 -145.5` at fov 70.
- All are full frames. At 256x144 the plate reads as two dark figures with pale helmets framing the bridge deck.

**The set:**

1. The bridge lanterns, as in shot 2.
2. Wild flowers cleared.
3. Two Hollow soldiers in the foreground on the road, backs to the camera, arms down:
   - (20.50, -143.60) at yaw 104, with sword and shield;
   - (19.70, -147.50) at yaw 80, with a sword.

**Staged for real:** all of it.

**Workaround:** the lowered arms come from the pose hook.

**Commands in order** (`sets/plate-order-crossings.cmd`; consolidated from sb2 `11-north.cmd` (lanterns and flowers)
with `12-day.cmd` (second half), and replayed whole):

```
# Shot 4, plate-order-crossings. Rivergate (map 0), the north crossing by day, from the east bank down the bridge.
# Consolidated from sb2 11-north.cmd (bridge lanterns, flowers) + 12-day.cmd (second half), as played.
map 0
/kill @e[tag=set]
# lanterns on top of every log post along both edges of the bridge
/setblock -32 66 -153 lantern
/setblock -32 66 -138 lantern
/setblock -28 66 -153 lantern
/setblock -28 66 -138 lantern
/setblock -24 66 -153 lantern
/setblock -24 66 -138 lantern
/setblock -20 66 -153 lantern
/setblock -20 66 -138 lantern
/setblock -16 66 -153 lantern
/setblock -16 66 -138 lantern
/setblock -12 66 -153 lantern
/setblock -12 66 -138 lantern
/setblock -8 66 -153 lantern
/setblock -8 66 -138 lantern
/setblock -4 66 -153 lantern
/setblock -4 66 -138 lantern
/setblock 0 66 -153 lantern
/setblock 0 66 -138 lantern
/setblock 4 66 -153 lantern
/setblock 4 66 -138 lantern
/setblock 8 66 -153 lantern
/setblock 8 66 -138 lantern
/fill 4 65 -146 40 67 -110 air replace #minecraft:flowers
/time set 4000
/weather clear
# two Hollow soldiers large in the foreground on the road, backs to the camera
/execute positioned 20.50 90 -143.60 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["plate","set"],Rotation:[104.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{id:"minecraft:shield",Count:1b}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
/execute positioned 19.70 90 -147.50 positioned over motion_blocking_no_leaves run summon husk ~ ~ ~ {NoAI:1b,PersistenceRequired:1b,Silent:1b,Invulnerable:1b,CustomName:'{"text":"Dawn Knight"}',Tags:["plate","set"],Rotation:[80.0f,0f],HandItems:[{id:"minecraft:iron_sword",Count:1b},{}],ArmorItems:[{},{id:"minecraft:leather_leggings",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:leather_chestplate",Count:1b,tag:{display:{color:2368580}}},{id:"minecraft:iron_helmet",Count:1b}]}
pose plate rarm -12 0 0 larm -8 0 0
rd 12
fov 60
look 24.0 65.9 -145.5 -20.0 66.0 -145.5
wait 120
shot plate-order-crossings
fov 50
look 24.8 65.8 -145.5 -20.0 66.1 -145.5
wait 60
shot plate-order-crossings-alt1
fov 70
look 23.6 66.0 -145.5 -20.0 65.8 -145.5
wait 60
shot plate-order-crossings-alt2
```

---

## Sessions

| Session | Time | What it produced |
|---|---|---|
| sb1 | 23:44:22 to 00:10:49 | Rehearsal. Excalibur and Clarent were on, so none of its frames are used. |
| sb2 | 00:14:08 to 00:30:37 | All four sets. The field and plate frames published here come from it. |
| sb3 | 00:34:56 to 00:42:05 | The gold-trim trials (shaper, wayfinder, host, silence, sentry; host chosen), the final Fall and north frames, and the replays of the field, plate and Fall lists. |

All iteration frames and the logs of every played file are kept in `work/sb/<session>/run/screenshots` and
`work/sb/<session>/control`.
