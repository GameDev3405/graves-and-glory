# Ironhold master layout (2026-10-30)

Status: **SIGNED by Kort, 2026-10-30, as drawn.** This is the master layout for Ironhold; section passes may start. The section grid and locks live in `manifest.json` (`gg-mapforge-map/1`). The file name keeps "draft" so older links still resolve.

Kort's picks (section 12): **D1a** keep today's coordinates, add the east gorge, Dusk spawn moves to (128,0). **D2a** new Great Keep tower piece to about y 114. **D3a** keep Domination A/B/C. **D4a** a defenders-only keep postern, built by the modes lane. **D5a** the Mire with a causeway. **D6a** north low and wet, south high.

Also decided 2026-10-30: sections about 64x64; each goes blockout, structure, natural, detail; agents review the passes; Kort signs only this layout and the whole finished map. Review images per section are top-down plus isometric; in-game shots come later through the harness. The protect locks belong to the map lane (in the manifest). Grass and leaves biome tint waits for the palette work.

No game code, spawn or capture point has changed yet (plan 7.3): the Dusk spawn move and the postern are for the modes lane.

Files in this folder:
- `layout-blockout.json`: a gg-mapforge-plan/1 blockout with 1281 ops. It validates and applies to a flat 700x500 world.
- `layout-top.png`: top-down view with the 64x64 section grid, labels and legend.
- `layout-iso.png`: isometric view from the south-west.

`[Choice]` marks a place where I picked between options; the reason follows it.

## 1. Frame and size
- The arena box is unchanged: x -350..349, z -250..249, floor y 64. The River Iron stays at x = 0.
- Playable ground is about x -330..+165 by z -190..+200, roughly 495 x 390 blocks.
- `[Choice]` Keep today's coordinates and fill the empty east with a gorge at x ≈ 165. Gate (-37,0), keep (0,34), points A/B/C and all six Siege modeSites rows stay where they are, so no flood or Siege re-tuning is needed. The gorge puts the fortress "on one side" as Kort's vision asks. The one cost is that the east spawn moves (see D1).
- West to east: siege camp, open field, the fortress, then the gorge drop. The long axis is the attack.

## 2. Zones
| Zone | Extent | Role |
|---|---|---|
| Siege camp | x -330..-276, z ±72, shelf y 72 | West (Dawn) spawn: tents, trebuchets, palisade with an east gate |
| West upland | x -275..-230 | Gentle rise (y 67) off the camp; the first mantlets |
| The Mire | x -185..-125, z ±110, mud y 61 | Slow, low middle; the King's Road crosses it on a causeway |
| Killing field | x -125..-80 | Open, low cover, overlooked by the gate towers |
| Fosse + outer curtain | x -80..-70 | Wet ditch (water y 61), causeway at z ±9; curtain top 71 |
| Outer Yard | x -69..-41, z ±93 | Close-quarters gravel yard; Siege stage A ring (-55,0) |
| Gate wall | x -40..-34, z ±23 | Deepslate gate, towers to 81; Siege stage B target |
| Inner yard + keep | x -33..23, z -24..47 | Domination B deck, keep court ring (-8,34), Great Keep tower |
| East bailey | x 6..97 | Andesite yard, defenders' road to the B deck |
| Donjon Court | x 103..160, z ±48 | Proposed east (Dusk) spawn at (128,0), barracks hall |
| Flanks | north z < -100, south z > 100 | Burned village + A (north), Gallows Ridge + C + Watch Hill (south) |
| The gorge | x ≥ 170, floor y 44 | Non-playable drop, parapet at x 166..168 |

## 3. Flow from spawn to objectives
- **Dawn (west), Domination:**
  - The King's Road runs straight to the gate.
  - The north lane goes through the burned village to A (-29,-130). The south lane goes over Gallows Ridge to the south ford and C (29,130).
  - B (0,0) is reached through the front door and Outer Yard, or by a sally gap and the water gates.
- **Dusk (east), Domination:** from the Donjon Court through the east postern and bailey road to the B deck (≈130 blocks). There are short spurs to A along the north wall and to C through the south postern.
- `[Choice]` Spawn-to-B distance is about 315 for Dawn and 130 for Dusk, by design. Dawn gets the faster outer flanks (A and C sit outside the walls). The spawn-wave timing must confirm this balance (D3).
- **Siege (Dawn attacks):**
  - Camp → field → the outer curtain's front door and sally gaps → Outer Yard (stage A, hold 20 s).
  - Then the gate (stage B, 1600 HP, under fire from the tower-top stations).
  - Then the inner yard → the keep's NW land gate → keep court (stage C, hold 30 s).
- **Siege (Dusk defends):** Donjon Court → bailey → B deck → inner yard and the gate stations.
- **Finding to confirm:** with the Siege wards placed, ward-b (the B deck) and gate-ne seem to seal every dry route from the east bank into the inner yard and keep. The defenders would have to ford. The modes lane must confirm this (D4).

## 4. Lanes and flanks
- **Three lanes.** North is low and wet: the village, A, the north ford and the north water gate. Centre is the King's Road and the gate. South is high: Gallows Ridge, the chapel, the south ford and C.
- `[Choice]` Asymmetric flanks (north low and wet, south high). Each flank should feel distinct and give the faction kits a home. Tidecallers use the fords and water gates; archers and Ironclad use the ridge. Mirrored flanks would read as generic (D6).
- **Cross links:**
  - Trench lines at x -150 and -105 link the lanes behind cover.
  - Sally gaps at z ±62 in the outer curtain let defenders strike the flanks.

## 5. Sight lines
- **Kept open:**
  - Camp to the Great Keep tower: it is the skyline landmark.
  - Gate towers over the killing field to about x -110.
  - Keep wall-walk to the B deck.
  - Watch Hill over the south curtain to C.
- **Blocked on purpose:**
  - The north lane is hidden from the gate towers by the village and the north bluff.
  - The Outer Yard is hidden from the field by the outer curtain.
  - The Mire's far edge is hidden from the camp by the upland lip, so no camp-to-gate sniping.

## 6. Cover density
Camp low; field and Mire medium (mantlet rows at x -235, -200, -160, -130, -110, plus trenches); killing field sparse and low; Outer Yard dense close-quarters; inner yard and keep court medium; bailey medium-low (carts and sheds come in the structure pass).

`[Choice]` Cover thins toward the gate so the final push is costly, which matches the "open-field approach" vision.

## 7. Height bands (top of ground or structure)
- Ground: rim 82-88; camp shelf, Gallows Ridge, Watch Hill shoulder 72; north bluff 70; west upland 67; field and fortress floor 63-64; Mire and fosse water 61; gorge floor 44.
- Walls: curtain walk 72; gate towers 81; keep walls 85; Great Keep tower 114, crown 118.

`[Choice]` The points keep elevation 64. The ridges sit beside A and C, not under them, so today's point rows stay valid.

## 8. Landmarks
| Landmark | Description |
|---|---|
| Great Keep tower | x 4..20, z 24..44, to y 114-118. Tall, dark, intact. The skyline anchor seen from the camp (D2) |
| Keep court and walls | x -23..23, z 21..47, walls to 85, four land gates (NW gate = defenders' way in, per the flood contract) |
| Iron Gate | Today's ironhold/gate at (-37,0): passage x -40..-34, z -5..5, portcullis, towers at 81 |
| Outer curtain | x -74..-70, z ±96: cracked front-door towers, two sally gaps |
| North and south curtains | z ±96 from x -74 to 102, with water-gate arches over the river |
| East curtain | Has the east postern and a south postern |
| Approaches | King's Road, the burned village (5 roofless houses), the Gallows Ridge chapel and gallows, the Mire causeway, the fosse causeway |
| Supply houses and watchtowers | Supply houses kept at today's spots; watchtowers at (-180,±85) and (150,±150) |
| Siege camp | Tents, two trebuchets, a palisade |
| The gorge | East backdrop; the fortress's back is to the cliff |

## 9. Siege fit
- **Unchanged:** all six modeSites wards, both Siege rings, the gate target and gate_intact.
- **The blockout keeps the flood contract:** inner yard x -33..-6, z -24..20, with the keep NW land gate open.
- **Defender stations kept:** gate tower tops (feet 13) and the keep north wall-walk (feet 7).
- **New walls that need checks:**
  - The outer curtain and fosse add a pre-yard choke. Stage A's 600 s clock must still be reachable, so check it in the whole-map blockout pass.
  - Web Stalkers can climb the curtain. Tidecallers can use the water gates. Both are intended counterplay; confirm with the modes lane.

## 10. Section grid (64x64; columns A..K from x -350, rows 0..7 from z -250)
Column x starts: A -350, B -289, C -225, D -161, E -97, F -33, G 31, H 95, I 159, J 223, K 287.
Row z starts: 0 -250, 1 -176, 2 -112, 3 -48, 4 16, 5 80, 6 144, 7 208.

Rims and backdrop (light pass): row 0, row 7 and column A are the North Mountains, South Ridge and west rim; columns I..K are the gorge (I holds the parapet edge).

**Core sections:**

| Section | Name | Brief |
|---|---|---|
| B1-B2 | Camp north | Upland slope, north lane start, mantlet row x -235 |
| B3-B4 | Siege camp | Dawn spawn (-314,0), tents, trebuchets, palisade gate |
| B5-B6 | Camp south | South lane start, upland to Gallows Ridge foot |
| C1 | West upland | Bluff foot, lane bend |
| C2 | Mire north | North lane on dry ground, supply house (-140,-72) |
| C3-C4 | The Mire | King's Road causeway, mantlets x -200/-160, trench x -150 |
| C5-C6 | Gallows Ridge west | Ridge climb, chapel approach |
| D1 | North bluff | High ground over the north lane (y 70) |
| D2 | Mire edge north | Supply yard, watchtower (-180,-85) |
| D3-D4 | Killing field west | Mantlets x -130/-110, trench x -105 |
| D5 | Killing field south | Supply house (-125,72), watchtower (-180,85) |
| D6 | Gallows Ridge | Chapel, gallows, south lane crest |
| E1 | Village outskirts | North lane into the burned village |
| E2 | Burned village | Roofless houses, approach to A, north sally gap |
| E3 | Front door | Fosse causeway, cracked towers, Outer Yard north, gate north tower |
| E4 | Outer Yard south | Stage A ring (-55,0) spans E3/E4, gate south tower |
| E5 | South sally | South sally gap, curtain corner |
| E6 | South ford road | South lane down to the ford |
| F1 | Point A | Supply Yard (-29,-130), north ford, river gorge mouth |
| F2 | North water gate | North curtain, river arch |
| F3 | Inner yard and B | Iron Gate inner face, B deck (0,0), wards ward-n/ward-b |
| F4 | Keep court | Keep walls, Great Keep tower, stage C ring (-8,34), ward-s/gate-w/gate-e/gate-ne |
| F5 | South water gate | South curtain, river arch |
| F6 | South ford | Gravel ford z 130 |
| G1 | North reach | Open bank east of A |
| G2-G4 | East bailey | Andesite yard, defenders' road |
| G5 | Point C | Watch Hill (29,130) at y 64, south postern |
| G6 | Watch Hill shoulder | High ground y 72 over C |
| H1, H6 | East watch | Watchtowers (150,±150) |
| H2 | Supply east north | Supply house (125,-72), north-east tower |
| H3 | Donjon Court | Dusk spawn (128,0), barracks hall, east postern |
| H4 | Donjon Court south | Court floor, supply house (140,72) |
| H5 | South-east corner | Curtain corner, postern road |

## 11. Build order
1. Kort signed this layout on 2026-10-30 (decisions below, all option a).
2. Whole-map blockout pass from `layout-blockout.json`, kept only as a plan until signed off. Then route-time and sight-line checks, and the p4-world-wards flood gate re-run.
3. Structure passes, fortress first: E3, F3, F4, E2, E4, G3, H3, then the remaining fortress and curtain sections (F2, F5, E5, G2, G4, H2, H4, H5).
4. Field: D3, D4, C3, C4, E1, D6, C5, F1, F6, G5, G6, then the rest of B..D.
5. Rims and backdrop (row 0, row 7, column A, I..K) as one light pass.
6. Natural, then detail passes per section in the same order. Then seams, then the whole-map check and Kort's final sign-off.

`[Choice]` Fortress first, because the gate and keep set every sight line and the Siege timing.

Regenerate: `python -m tools.mapforge` flat, then apply this plan, then `render --view top --scale 2 --no-grid` (plus grid overlay) or `render --view iso`.

## 12. Decisions (Kort picked a for all six, 2026-10-30)
- **D1. Fortress frame:** (a) today's coordinates + east gorge, Dusk spawn to (128,0); (b) shift the fortress ~100 east (every modeSites row moves, flood re-run); (c) all-new coordinates.
- **D2. Keep height:** (a) new Great Keep tower piece on today's footprint, top ~y 114; (b) today's 22-high keep with a dark crag backdrop; (c) raise the keep ground (Siege station heights change).
- **D3. Domination on Ironhold:** (a) keep A/B/C, balance with spawn timing and route checks; (b) move B to the Outer Yard (-55,0); (c) Ironhold becomes Siege-only.
- **D4. Defenders' east-bank route in Siege:** (a) defenders-only keep postern from the modes lane; (b) defenders respawn west of the river after the yard falls; (c) the B-deck ward becomes a team door.
- **D5. Mid-field character:** (a) the Mire, low mud with a causeway; (b) dry field with trenches only; (c) a burned town mid-field.
- **D6. Flanks:** (a) north low and wet, south high; (b) mirrored flanks; (c) one flank only, the other closed by the rim.
