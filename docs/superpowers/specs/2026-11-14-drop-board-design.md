# Drop board design (2026-11-14)

Status: APPROVED by Kort 2026-11-14 ("Approved, build it"). Picks: slot table A bell; spot A east of the hearth court; giant board, standard timing; no queue, relic rolls on the board (multiplier scales the duplicate refund). Page: https://claude.ai/artifact/6XQKp3QQrZe5a1GM2uPy13. Brief: `Melty/status/drop-board-brief.md`. Branch `ui/drop-board`, base main 36838cb4.
Mock-ups: `docs/superpowers/specs/drop-board/*.jpg` (concept pictures, built by `mockups.py`). Odds proof: `python tools/dropboard_odds.py`.

## 1. What it is

A recruitment roll (War Table, Enlist) and a Forge roll (Roll helm, chest, legs, boots, weapon) no longer show a flat card. The camera
flies to a giant relic altar in the lobby world, the commander slides a stamped bronze coin along the top rail and drops it, it bounces
through bone pegs into one of nine rune slots (x10, x5, x3, x2, x1, x2, x3, x5, x10), the slot flares, and the troop or piece rises
out of it in its rarity colour. A click skips to the result at any point.

## 2. Fairness and the odds

### 2.1 Rule

1. The server takes the payment and, in the same step, draws the slot from the slot table and then the item with that slot's
   boosted chances. The result is written to the profile and saved before anything is shown. Nothing the client sends later
   can change it.
2. Boost: every tier in the boosted set (rare, epic, legendary) has its chance multiplied by `m / mean`, where `mean` is the
   slot table's average multiplier. The tiers below (common, uncommon) share what is left in their old proportions.
3. The bounces are a real 2D simulation on the server (gravity, round pegs, restitution, a little spin noise). The server runs
   it from the commander's chosen start point with fresh random noise until a run ends in the drawn slot, and sends that path.
   So every start point has exactly the slot table's odds: there is no trick spot, and the path is still real physics from
   where the coin was placed. (Retries are capped; after the cap a small steering nudge per bounce is added so a far slot is
   always reachable. Tested: 9 start points x 9 slots all resolve.)

### 2.2 Slot table (proposed A; all numbers live in `sheets/design.json`, table `dropBoard`)

| Slot | x10 | x5 | x3 | x2 | x1 | x2 | x3 | x5 | x10 |
|---|---|---|---|---|---|---|---|---|---|
| A bell (recommended) | 1% | 4% | 10% | 20% | 30% | 20% | 10% | 4% | 1% |
| B wild | 2% | 6% | 12% | 20% | 20% | 20% | 12% | 6% | 2% |

Mean multiplier: A 2.30, B 2.72. Base factor (what an x1 gives against today): A 0.435, B 0.368.

A gentler table than A is not possible: gear's rare-and-above share is 22%, and x10 must stay under 100%, so the mean must be
at least 2.2. A is within 5% of the gentlest table that works.

### 2.3 Proof the average is unchanged (exact fractions, `tools/dropboard_odds.py --check`)

Gear, table A (today: common 50, uncommon 28, rare 15, epic 6, legendary 1):

| Slot | Chance | Common | Uncommon | Rare | Epic | Legendary |
|---|---|---|---|---|---|---|
| x1 | 30% | 57.97% | 32.46% | 6.52% | 2.61% | 0.43% |
| x2 | 40% | 51.84% | 29.03% | 13.04% | 5.22% | 0.87% |
| x3 | 20% | 45.71% | 25.60% | 19.57% | 7.83% | 1.30% |
| x5 | 8% | 33.44% | 18.73% | 32.61% | 13.04% | 2.17% |
| x10 | 2% | 2.79% | 1.56% | 65.22% | 26.09% | 4.35% |
| **Average** | | **50.00%** | **28.00%** | **15.00%** | **6.00%** | **1.00%** |
| Today | | 50.00% | 28.00% | 15.00% | 6.00% | 1.00% |

Units, table A, every House (today: common 86, rare 12, epic 2; Legendaries never roll):

| Slot | Chance | Common | Rare | Epic |
|---|---|---|---|---|
| x1 | 30% | 93.91% | 5.22% | 0.87% |
| x2 | 40% | 87.83% | 10.43% | 1.74% |
| x3 | 20% | 81.74% | 15.65% | 2.61% |
| x5 | 8% | 69.57% | 26.09% | 4.35% |
| x10 | 2% | 39.13% | 52.17% | 8.70% |
| **Average** | | **86.00%** | **12.00%** | **2.00%** |

The match is exact, not rounded. And because the slot is drawn fresh on every roll, each roll's chances averaged over the slots
are today's, so the whole run of results has the same distribution as today, pity included: gear still gives an Epic or better
every 7.856 rolls on average, units an Epic every 27.715. The board changes what a roll feels like, not what a hundred rolls
give. An x1 is a real 0.43x roll and an x10 a real 4.35x roll.

### 2.4 Edge rules (need the army lead's agreement)

- Pity roll (gear: 10 misses; units: roll 40): the forced Epic-or-better stays forced. The coin still drops; on gear the
  multiplier boosts Legendary against Epic by the same `m / mean` (average unchanged: 1 in 7).
- Relic roll: relics have no rarity. Proposed: the board plays, and the multiplier scales the duplicate refund by `m / mean`
  (average refund unchanged). Open question for Kort.
- Chosen buys, star-ups and Pacts are not rolls: no board.
- `Forge.roll` and `RosterMath.roll` gain a multiplier argument; the old signatures stay as the x-mean case for sims and tests.
- No save schema change: the result is banked at pay time, as today. The pending drop is server memory only.

## 3. The board

Giant size (recommended): 11 blocks wide, 13 tall, 2 deep, on a 15 x 8 court. Large size: 7 x 9.

- Slab: dark worked stone in big courses, a bronze frame with rivets, a glimpur crystal (violet-white) in the lintel.
  Giant relic, never named (Act I never names the giants).
- Rail: a bronze rail under the lintel; the coin rides it.
- Pegs: 8 staggered rows of bone studs (10 and 9 a row), 1 block apart, 4 px across.
- Slots: nine rune cells, 1 block wide, bronze dividers. Each rune carries the giant numeral and the plain "x5" under it.
  Colours climb with the multiplier on a glimpur ramp (ash, pale blue, sea green, violet, white gold), kept apart from the
  rarity colours (grey, green, blue, purple, orange) which only the reveal uses.
- Coin: a stamped bronze coin 10 px across, a hand on one face and a spire on the other; it spins as it falls.
- Dressing: two crystal braziers, the existing giant bone and stone hand props, a worn stone court.

Props (built here, prop kit formats, `art/props/<id>/model.py`, CMD blocks agreed with the props lead):
`drop_altar` (split big prop: slab, frame, plinth, glow parts for crystal and nine runes), `drop_coin`, `drop_brazier`.
The coin, the rune flare and the rising item are drawn by the client each frame (as the podium figures are), not as display
entities, so they move at frame rate.

## 4. Where it stands

Three candidate spots (`lobby.jpg`); the maps lead owns lobby blocks and must agree:

- A (recommended): east of the hearth court, about 40 blocks from the War Table, facing west. Seen from the table; players can
  walk up and watch a drop.
- B: beyond the graveyard at the end of the north axis, facing south.
- C: its own stage off the island beside the podium stage, built by code like `PodiumStage`. Touches no lobby blocks, but
  nobody can walk to it.

The spot is one row in the sheet (origin, facing), so it moves when the Old Hold lobby replaces the camp.

## 5. The sequence (standard timing; whole moment about 11 s, skippable)

| Step | Time | What happens |
|---|---|---|
| Fly in | 0 to 1.6 s | The screen closes; the camera eases from the commander to the altar (smoothstep dolly, FOV 50, letterbox), braziers flare, low stone rumble. |
| Aim | up to 8 s | The coin follows the mouse along the rail (clamped, eased). Click or Space drops. A ring on the coin drains; at 0 it drops where it is. |
| Drop | about 5 s | The server's path plays back, interpolated every frame. The camera tracks the coin gently downward. Each peg hit rings (pitch by speed) and sparks. |
| Flare | 1.0 s | The coin lands, the slot's rune flares in its colour and a beam rises; the other runes dim. x5 and x10 shake the camera and hit harder. |
| Reveal | 2.5 s | The troop (live model) or the piece rises out of the slot, turning, lit in its rarity colour, with the name plate, rarity, duplicate/star line and a sting. |
| Return | 0.8 s | The camera eases back; the War Table reopens on the result as today. |

- Skip: a click, Escape or the menu key after the drop starts jumps to the Reveal's last frame, then returns. A second press returns at once.
- The camera reuses the podium's approach (an invisible camera entity, the FOV mixin, hud hidden, hands hidden).
- Smoothness rules: all motion is sampled per render frame from continuous time; no per-tick stepping is visible. The path is
  sent as 30 Hz samples plus bounce events and replayed with cubic interpolation. The suite checks the playback has no jump
  over 0.35 block between frames at 60 fps.

## 6. Lights and effects

- Idle: crystal and runes glow softly; brazier particles.
- Drop: a faint trail behind the coin; a spark and one bright frame on the struck peg.
- Flare: the slot's glow part goes to full, a vertical beam quad fades up, light level rises around the altar (client light
  only), and particles in the multiplier colour.
- x10: all runes pulse outward from the slot, the crystal blazes, a horn.
- Reveal: a rarity-coloured halo and rising motes; Legendary adds a ground ring.

## 7. Sounds (free-only rule: Pixabay Content Licence or CC0, credited in `docs/AUDIO-CREDITS.md`, imported with `tools/audio/import_pixabay.py`)

`drop_fly` (stone rumble), `drop_rail` (coin sliding on bronze, looped while moving), `drop_release`, `drop_peg` (3 bone/metal
ticks, pitch 0.8 to 1.4 by speed), `drop_land`, `drop_flare_1/2/3/5/10` (rising chime to choir hit), `drop_jackpot` (lobby-wide
horn), reveal stings by rarity (reuse `unit_reveal`; add epic and legendary stings). The audio lead reviews the mix.

## 8. Multiplayer

- Watchers: anyone within 48 blocks of the altar sees the coin, the flare and the item rise, with the roller's name over the rail.
- Two at once (recommended: no queue): each roll is its own drop. The roller always sees his own in the cinematic; watchers see
  every active drop, each coin tinted and named. Nobody waits.
- Lobby-wide: an x10, or a Legendary piece, sends one chat line and one sound to everyone in the lobby
  ("Kort struck x10 on the altar", "Kort drew a Legendary Warhelm").
- Disconnect: the result was saved at pay time. If the roller leaves mid-drop the drop ends for watchers with the flare, and
  on rejoin the War Table shows the result card as today. Never lost, never doubled (the pending drop holds no value).
- A match starting during a drop: the drop is cut and the result shown as a card.
- Protocol: one client action `drop:place:<x>`; server packets DROP_BEGIN, DROP_PATH, DROP_END. New packets are a protocol
  change: covered by Kort's approval of this design.

## 9. Code plan

| Piece | Kind | Notes |
|---|---|---|
| `DropMath` | pure | slot draw, boosted weights, exact with the sheet |
| `DropSim` | pure | deterministic 2D physics, path search to a slot, path encoding |
| `DropBoard` (server) | game | pending drops, timers, auto-drop, watchers, broadcasts |
| `DropClient`, `DropRender` | client | camera, aim, playback, coin, flare, reveal, skip |
| `DropLayout`, `DropStage` | world | where it stands, placing the altar props |
| sheet `dropBoard` | data | slots, chances, timings, origin; `Spec.java` regenerated |
| props, sounds | art | section 3 and 7 |

Tests first: `DropMathCheck` (average exact for every House and gear, cap), `DropSimCheck` (determinism, every start reaches
every slot, 100k drops match the table within tolerance from 9 starts, duration 4 to 6 s, no tunnelling), `DropFlowCheck`
(pay-then-bank, disconnect at each step, double-send, auto-drop, skip, two rollers), and a client suite `drop-board` through
`tools/checkq.py` (shots of each step, playback smoothness, skip). `tools/verify.py fast` green.

## 10. Decisions (Kort, 2026-11-14)

1. Slot table A bell. 2. Spot A, east of the hearth court. 3. Giant board (11 x 13). 4. Standard timing (5 s drop, 8 s aim timer).
5. No queue. 6. Relic rolls use the board; the multiplier scales the duplicate refund by m / mean. New drop packets and the roll-code change approved.

## 11. Build notes

- 2026-11-13, DropSim: a peg half a block from a wall wedges the coin against it (36% of plain drops from the far left never
  finished). Each odd peg row therefore has a wall buttress: a ramp from the wall, 0.6 above the row, down to the outermost
  peg. The altar prop models these buttresses. Measured over 20,000 plain drops a start: none unfinished, mean 5.2 s, about
  23 peg hits; 40 of 324 searched drops needed the lean (far corners only).
- 2026-11-13, path samples: 60 a second with straight-line blending between them (not 30 with a curve): a bounce is a corner, and a
  curve through it rounds the hit away.
- 2026-11-13, commands: the War Table sends `drop:roll:<house>` and `drop:forge:<slot>`; the old `roll:` and `forge:roll:` still roll
  with no board (the existing suites use them, and the server falls back to them where the altar cannot be seen).
- 2026-11-13, the altar's art: made with the prop workflow (art/props/drop_altar, drop_coin) but drawn by the client from the mod's
  own assets (DropAltar, tools/dropboard_assets.py), because the prop resource pack only reaches the builder server's clients. The
  props' defs are kept beside the art (def.json), not in gg_props, so they take no CustomModelData block. The server keeps the
  altar's space solid with barriers.
- 2026-11-13, sounds: eight cues of its own (AudioCues DROP_*; three peg variants), free Pixabay recordings Kort allowed that day,
  converted by tools/audio/import_dropboard.py and credited in docs/AUDIO-CREDITS.md. Picked from names and waveforms, not by ear.
