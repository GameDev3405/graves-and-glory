# Phase 3 screenshot gate: the built world

For Kort. Eleven contact sheets (frames 480x270, five to a row, read left to right, top to bottom), then a walk-through in the game. Phase 3, the built world, closes only when you say it does. Nothing below is merged, pushed or uploaded.

**Where the sheets are.** `C:\Users\kortt\Melty\bannerbound-p3\work\p3\task9-sheets\` (the controller also sends them to you). The frames behind them are in `work\p3\task9-shots\` (310 pictures); `work\p3\task9-manifest.txt` names the test root and commit of each. All frames come from the regression run on commit `5ea6d04`.

**What to tick.** Three questions per sheet. Tick Yes or No; write a word next to any No.

1. Reads as built, not generated.
2. Fits its House or map.
3. Nothing floats, clips or blocks a road. The one exception: the bases' barracks and stores still clip roads, which is parked (`docs/PARKED.md`, Question 6, for Phase 4).

**Known on every sheet.**

- The sky and clouds are the game's own; a white square in the sky is the sun.
- Hills far away fade into a blur: that is render distance, not a hole in the world.
- Banners and poles at the control points are white because the points are neutral when the picture is taken.
- Frame names below are the picture file names without their `p3-` and House prefix; `dawn-tower` is `dawn-tower-n-ground`, `shrine` is `shrine-ground`, `warehouse` is `warehouse-ground`.
- The camera is a test camera at fixed spots. A frame that looks odd may be the angle; say so and I will move the angle, not the building.

## Sheet 1: Banner Camp

Path: `work\p3\task9-sheets\camp.png` (28 frames, 5 across, 6 rows, the last row has three).

Rows: aerial, archive, arrival, bank, first-arrival; flank-east, flank-west, gates-inside, gates-outside, house-gilded; house-graveborn, house-ironclad, house-tidecallers, house-wildmoot, map2-aerial; map2-gates-inside, map2-ridge, map2-south-oblique, recruit-gilded, ridge; ring, south-oblique, south-pads, train-page, war-table; yard-boards, yard-lessons, yard.

Two frames are menu screens, not world: `recruit-gilded` and `train-page` (row 4 column 4 and row 5 column 4). Judge the world frames only. The four `map2-*` frames are the same camp on map 2's ground; the rest are map 0's. `gates-outside` is a close look at the grate under the gate arch.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 2: Rivergate Valley, landmarks

Path: `work\p3\task9-sheets\map0.png` (16 frames, 5 across, 4 rows).

Rows: overview-north, overview-south, customs-ground, customs-road, dawn-tower; farm, house-0, house-1, market-ground, market-road; mill-ground, mill-high, shrine, supply-nw-ground, supply-nw-high; warehouse.

The tall stone posts with a white banner in the market and shrine frames are the control points' flag poles. The overview frames are blurred at the far hills (render distance).

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 3: Rivergate Valley, supply houses

Path: `work\p3\task9-sheets\houses0.png` (20 frames, 5 across, 4 rows). One row per house: north-west, south-west, north-east, south-east. Each row: spur (the climb, flag readable from the lane), gate (between its two braziers), yard (ring, with the flag above the roof line), lookout (over the spur), point (from the nearest control point).

**Flags are white and braziers are out.** The houses stay inert buildings until the supply-house rules plan (`docs/PARKED.md`); white is the neutral flag. Do not judge colour.

Look at: the four houses are the same piece placed four times (half-turn symmetry), so the rows look alike by design. In the yard frames the pole runs out of the top of the picture; the cloth shows in the gate and lookout frames.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 4: Ironhold, landmarks

Path: `work\p3\task9-sheets\map1.png` (24 frames, 5 across, 5 rows, the last row has four).

Rows: overview-north, overview-south, curtain-wn1-ground, curtain-wn1-high, curtain-wn1-road; dawn-tower, garrison-ground, garrison-high, garrison-road, gate-ground; gate-high, gate-road, hay-barn-ground, hay-barn-high, hay-barn-road; house-0, house-1, keep-ground, keep-high, keep-road; market-ground, market-road, supply-nw-ground, supply-nw-high.

Look at:

- `overview-north` (first frame) has two white patches in the far hills. They are hills the game had not drawn yet when the picture was taken; the same frame in an earlier run has none. Ignore them.
- `gate-ground` (row 2, last frame): two stacked wooden doors stand on the river bank beside a small stone footing in the foreground. I could not tell which piece they belong to. Say whether they read as a river gate or as debris.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 5: Ironhold, supply houses

Path: `work\p3\task9-sheets\houses1.png` (20 frames, 5 across, 4 rows). Same layout as sheet 3: four houses, five views each. The stone house with its crenellated wall is Ironhold's armoury. Flags white, braziers out, as on sheet 3.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 6: Frostmarch, landmarks

Path: `work\p3\task9-sheets\map2.png` (23 frames, 5 across, 5 rows, the last row has three).

Rows: overview-north, overview-south, customs-ground, customs-road, dawn-tower; house-0, house-1, lodge-front, lodge-ground, lodge-high; lodge-road, refuge-front, refuge-ground, refuge-high, refuge-road; shrine, signal-tower-front, signal-tower-ground, signal-tower-high, signal-tower-road; supply-nw-ground, supply-nw-high, warehouse.

`supply-nw-high` (row 5, second frame) is the terrace cut into the ridge: look at how the cut meets the slope, and whether the house looks set into the hill or dropped on it.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 7: Frostmarch, supply houses

Path: `work\p3\task9-sheets\houses2.png` (20 frames, 5 across, 4 rows). Same layout as sheet 3. This is the terrace on the ridge: the spur frames climb a long stair, and the house is the highest, so its pole is the tallest of the five maps.

Look at: in the spur frames the white flag is small against white snow and hard to find; in the gate and lookout frames it is clear against the sky. White patches lie on the stone of the yard; they look like snow lying on the paving. Flags white, braziers out, as on sheet 3.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 8: Hollowwood, landmarks

Path: `work\p3\task9-sheets\map3.png` (28 frames, 5 across, 6 rows, the last row has three).

Rows: overview-north, overview-south, chapel-front, chapel-ground, chapel-high; chapel-road, customs-ground, customs-road, dawn-tower, house-0; house-1, logging-camp-front, logging-camp-ground, logging-camp-high, logging-camp-road; market-ground, market-road, moon-shrine-front, moon-shrine-ground, moon-shrine-high; moon-shrine-road, sawmill-front, sawmill-ground, sawmill-high, sawmill-road; supply-nw-ground, supply-nw-high, warehouse.

The chapel and the moon shrine are ruins on purpose (cracked and mossy stone, open roof). The chapel stands on the south lane.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 9: Hollowwood, supply houses

Path: `work\p3\task9-sheets\houses3.png` (20 frames, 5 across, 4 rows). Same layout as sheet 3. Flags white, braziers out, as on sheet 3. The lanterns here are the cyan soul lanterns of the map.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 10: Kingsfield, landmarks

Path: `work\p3\task9-sheets\map4.png` (18 frames, 5 across, 4 rows, the last row has three).

Rows: overview-north, overview-south, customs-ground, customs-road, dawn-tower; farm, house-0, house-1, market-ground, market-road; mill-ground, mill-high, outpost-west-ground, outpost-west-high, shrine; supply-nw-ground, supply-nw-high, warehouse.

Kingsfield is the open farm country: look at whether it reads as a place people live and farm, and whether the west outpost (palisade fort on the rise) fits the open country.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## Sheet 11: Kingsfield, supply houses

Path: `work\p3\task9-sheets\houses4.png` (10 frames, 5 across, 2 rows). Kingsfield has two houses, north-west (row 1) and south-east (row 2); five views each, as on sheet 3. Flags white, braziers out, as on sheet 3.

| Question | Yes | No |
|---|:-:|:-:|
| Reads as built, not generated | [ ] | [ ] |
| Fits its House or map | [ ] | [ ] |
| Nothing floats, clips or blocks a road | [ ] | [ ] |

## The walk-through in the game

About 40 minutes. The sheets show fixed angles; the walk shows what the sheets cannot: awkward terrain, a building that sits wrong, a road you cannot use.

1. A fresh world arrives at the yard.
2. Press START on battle 1; back at camp afterwards.
3. Up the ridge to the war table.
4. Look at the five Houses.
5. Out through the War Gates.
6. One skirmish each on Ironhold and Kingsfield.
7. A short walk each on Rivergate, Frostmarch (up and down its terrace) and Hollowwood: past their landmarks and their supply houses, looking for awkward terrain and buildings that sit wrong.

| After the walk | Yes | No |
|---|:-:|:-:|
| The camp and the five Houses read as built places | [ ] | [ ] |
| Both skirmishes ran on terrain that felt right | [ ] | [ ] |
| The three walks showed nothing awkward or wrong | [ ] | [ ] |

Notes (free text):

**The gate passes only on your word.** If you say yes, the controller closes Phase 3. If you say no, say which sheet or which place, and that piece goes back for a fix round.
