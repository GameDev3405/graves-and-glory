# Army control polish: design

Date: 2026-11-10. Author: polish lead. Status: approved in outline by Kort on 2026-11-10 (audit items A1 to A9 and F7, `docs/reports/polish-audit-2026-11-10.md`); the answers he gave are marked "Kort". Built after the 1.21.1 port, because it changes packets and mouse handling that the port rewrites.

Reviewers before build: AI lead (order meaning, `Tactics`), UI lead (panel, HUD, input).

## 1. Goal

A commander can say what they mean in one key press, always knows who will obey and what each group is doing, is told why when an order is refused, and never has to drop their guard to give an order.

## 2. Rules that do not change

- The six orders keep one meaning each: Move, Hold, Attack, Charge, Follow, Retreat. This spec adds no order. It gives Attack a target and a place.
- Troops never rout and always do what they are told (2026-10-22).
- Players and bots order through one path with the same checks and cooldown. Anything a player can now say, a bot can say through the same call.
- Enemy picks and positions stay hidden from clients.
- A fallen commander cannot give orders (Kort, F7). Section 5 only makes the game say so.

## 3. Orders

### 3.1 Attack with a target and a place (A1)

One key, Z, read by what the crosshair is on when the key is released:

| Crosshair on | Order sent | Troops do |
|---|---|---|
| An enemy troop or commander, within order range | Attack with that enemy's group as target | Go to that group and fight it. On the way a troop strikes back only when it is hit or an enemy stands within 3 blocks of its path. |
| Ground, within order range | Attack with a point | March to the point, fight anything that comes inside engage radius on the way, stop at the point. |
| Sky, or beyond order range, or Z pressed twice within 300 ms | Attack with no target (today's order) | Hunt map-wide from where they stand. |

After a focused enemy group is dead, or an attack-move arrives, the group's order becomes Hold at its current place and facing (Kort). The HUD card shows the change.

A focused target is a group, not one troop: the client sends the id of the troop under the crosshair and the server resolves it to that troop's group (owner plus group id). If the target group is already dead when the order arrives, the server replies with refusal `target_gone` (section 5).

The focused enemy group gets the existing outline for `outlineLitTicks`, visible to the ordering player only.

Packet: `tactical_v2` already carries x, z and an enemy id. The client starts sending them for Attack. No new field. The server stops discarding the id for Attack (`OrderRules.java:76` keeps it today; `CommandSelection.order` is what sends −1).

Troop AI (reviewed by the AI lead, 2026-11-10). In the troop layer this is one rule: an Attack order carries an optional place and an optional target group, plus an end condition that turns it into Hold. Attack with neither is today's order. It lands in the AI lead's plan step 6 (the unit-AI spec's Soldier step 5, with `Orders.issue` in its step 6). Details settled with them:

- A group target is new code. Today's focus is one entity and needs line of sight (`Tactics.java:271`). Troops pick their own enemy inside the target group and spread across it.
- If the focused group leaves the sight of every troop in the ordered group, the troops go to where it was last seen and, after 5 s without seeing it, Hold there.
- The switch to Hold must not leave archers idle: Hold engages only within 10 blocks today, against archer reach of 22 to 30. The AI lead's archer-reach fix lands in the same step.
- Bots use the same order, and refusals come back to a bot as return values. Kort (2026-11-11): a bot commander has a body on the field that can be seen, chased and killed, and gives no orders until it respawns. So for a bot with a body:
  - Order range is measured from the bot's body, exactly as for a player. A bot has no crosshair; it names the point or the enemy group directly, and the same range check applies.
  - `down` applies to a bot: while its body is respawning every order is refused with `down`, and its troops keep their last orders, as a player's do.
  - The body is built after the port and after the Warlord animation chain (AI lead's design, `2026-11-10-human-bot-brain-design.md` sections 2A and 6, branch `ai/lead`). Until it exists a bot keeps today's rule: range is measured from its army's centre and `down` never applies.

### 3.2 Hold (A4, part)

- B held: aim a hold point (as today). Releasing sends it.
- B tapped twice within 300 ms: "hold where you stand". Each selected group holds at its own current centre, facing as it faces now.
- B tapped once: opens the shape tray for the selected groups without choosing a point (section 4.3).

### 3.3 Charge (A6)

Meaning unchanged. The preview changes: both the tap and the hold path draw the real lane, an arrow of `chargeRunBlocks` from the group's centre along the facing, with slots at the far end. Keeping the line after first contact is the AI lead's item and is not specified here.

An order to charge with a group that has no living troops is refused (`no_troops`) and does not start the cooldown.

### 3.4 Follow, Retreat, Move

Unchanged. Move stays a bot and internal order with no player key; attack-move covers the player's need.

## 4. Giving orders

### 4.1 Hold to aim, release to send (A5)

Every aimed order works the way X does today:

- Key down: the aim preview appears (ground ring, slots, facing arrow, or enemy outline).
- Key up: the order is sent for what is under the crosshair.
- Pressing Backspace, or the same key's cancel (right click is no longer a cancel), while the key is held: cancels.
- A tap of Z (down and up within 150 ms) sends at once at the crosshair. So a quick Z on an enemy is "attack that" in one press. B is the exception: a tap of B never sends a hold point (it opens the shape tray, section 4.3), so a slip of the finger cannot move the army.

The left and right mouse buttons are never taken by an aim. A commander can swing and block while aiming.

The panel (R) stays as a mouse route and a reference. While it is open the cursor is free and clicks go to the panel, as today; that is a choice the player makes by opening it. Closing it returns the mouse to combat at once.

### 4.2 Selection (A3)

- Number keys select groups at any time, panel open or closed (Kort): 1 Infantry, 2 Archers, 3 Riders, 4 Support, 5 Siege, 6 to 8 custom groups in their saved order, and the key left of 1 for All.
- Pressing a number selects only that group. Shift plus a number adds or removes it. Shift keeps its other meaning (an order given with Shift held leaves the panel open); the two do not collide because one is Shift with a number and the other Shift with an order key.
- The key left of 1 is shown by its bound name, since it differs by keyboard layout. The panel pills are renumbered to match these keys.
- Pressing the number of the only selected group twice within 300 ms selects All (a quick way back).
- Relics move to Q, E and F while a battle is running. Drop, inventory and swap-hands have no use in battle (the hotbar is pinned) and are suppressed there. In camp the keys do what they always do. All three are rebindable.
- The HUD group cards are the selection display: a selected card has a gold outline and a lighter fill, strong enough to read at 854 wide; unselected cards are plain. Two selected groups show as two lit cards, never as "all".
- With more than five cards (halves and custom groups can make eight or more) the cards switch to one-line rows so the column still fits at 854 x 480.
- Every card always shows its order glyph and, for Hold and Follow, its shape glyph. Not only the selected one.
- Selection does not reset by itself. Because it is now always visible on the cards, it does not need to.
- Clicking a troop with the panel open selects its group only if the troop is yours (fixes the ally bug, A9).
- Custom group cards count living troops, not banner slots.

### 4.3 Shapes and facing (A4)

- Shape is stored per group on the server (it already is, on the group's `Assignment`). The tray shows the selected group's shape; with several groups selected that differ, it shows none lit.
- Choosing a shape sends a shape-only order (the server supports it, `OrderRules.java:67`). It does not move the group.
- An Attack, Follow or Retreat order no longer carries a shape. It leaves each group's shape as it is.
- Facing: while B is held, the camera's turn is locked and moving the mouse left or right turns the facing arrow instead; the point is fixed at key down. So: look at the spot, hold B, sweep toward where the line should face, release. [ and ] still turn in steps, and a turn made before the aim is kept.
- Map orders use the same stored facing.

### 4.4 Split and merge (A7)

Kort: both pre-set groups and splitting in battle.

- Pre-set: custom groups are made on the banner page, by marking banner slots with a group number 6, 7 or 8. They are saved with each preset (economy spec, five presets), in the same save change.
- In battle: G halves each selected group. Infantry becomes Infantry I and Infantry II, each with its own card, number (the half takes the first free number from 6 to 8, or none if all are used) and orders. Halves are made by formation rank so both halves have front and rear troops. A half can be halved once more. G with two halves of one group selected merges them.
- A group of fewer than four troops cannot be halved (`too_few`).
- Halves last until merged or the round ends. Under best of five they do not carry between rounds; pre-set groups do.

Touches rules, packets and saves. It is the largest item here and can be built last.

### 4.5 Finding your groups (A8)

Kort: markers and a minimap.

- Marker: a small pennant floats over each of your groups' centres with the group's icon and order glyph. When the centre is off screen the pennant slides to the nearest screen edge and shows the distance in blocks. Markers hide within 12 blocks so they never cover a fight you are in.
- Minimap: a 96 px round map in the top right corner, north up, showing capture points with owner colours, your groups as their icons, your commander, and allies' commanders. Enemies appear only while a troop of your side sees them, by the same rule the full map uses. M still opens the full map.
- Both can be switched off in the Field page.
- The minimap is sized in HUD canvas pixels and checked at 854 wide. Both hide with the HUD (F1) and under the charge camera. Edge markers keep out of the open panel's area.
- The look of the cards, the charge ring, the markers and the minimap is the UI lead's: they make a mockup for Kort first. Behaviour and input are specified here.

## 5. Feedback (A2, F7)

Every order gets exactly one answer.

Accepted: the chime and shout as today, and one action bar line in words: "Archers: attack the Dusk riders", "Infantry and Riders: advance to the mill", "All: follow me". The line `archers / hold / 120, -44` goes.

Refused: the deny sound and one line. The server sends a refusal code on `command_v1`; the client owns the words.

| Code | When | Line |
|---|---|---|
| `down` | commander is respawning | "You are down. Orders wait until you return (N s)." |
| `intro` | round intro | "Wait for the horn." |
| `no_troops` | selected groups have no living troops | "No riders left." |
| `charge_cooldown` | charge not ready | "Charge ready in N s." (exists) |
| `target_gone` | focused group already dead | "That enemy is gone." |
| `out_of_range` | point beyond order range or outside the arena | "Too far to order." |
| `too_few` | split of a group under four | "Too few to split." |

A point in water is not refused. The approved unit-AI spec (2026-10-18, section 4) moves it to the nearest open cell, and this spec follows that: the "Choose dry ground or a crossing" refusal in `OrderRules.java:113` goes, and the preview ring snaps to the cell the order will really use. `down` never applies to a bot.

The 4-tick cooldown between orders stays silent: it is shorter than a key press and a message would be noise. The client holds a key release that lands inside it and sends it when the cooldown ends, so no press is lost.

While the commander is down, the respawn card adds a second line, "Your troops keep their last orders", so the block is stated before the player tries.

The charge cooldown shows at all times as a ring on the X hint beside the group cards, not only on the open panel.

## 6. What it touches

| Part | Change | Owner |
|---|---|---|
| `CommandSelection`, `CommandMode`, input mixins | hold-to-aim, number keys, relic keys, split key | polish lead builds, UI lead reviews |
| `CommandPanel`, `BattleHud` | per-group shape, always-on glyphs, selection on cards, charge ring, markers, minimap | polish lead builds, UI lead reviews |
| `OrderRules`, `command_v1` | refusal codes, attack point and target, no cooldown on empty charge, shape no longer rides along | polish lead builds, AI lead reviews |
| `Tactics` | attack-move, focus then hold | AI lead |
| `CustomGroups`, profile save | groups in presets, halves | with the economy spec's preset save change |
| `sheets/design.json` | order text; new values `doubleTapMs` 300, `tapMs` 150, `markerHideBlocks` 12, `splitMinimum` 4 | through `tools/preflight.py` |
| Tutorial | lessons rewritten for the new keys | with the build |

Build base (UI lead's review, 2026-11-10): this work starts from `ui/command-panel-polish` once that branch has landed. It moves the charge seconds into the Charge pennant, so the `charge_cooldown` line must read live seconds. The panel checks `cu-panel-number-picks` and `cu-panel-keys-swallowed` assume numbers act only with the panel open and are rewritten. The panel overlap checks are rerun against the gold readout.

## 7. Order of building

1. Feedback (section 5). Small, and it makes every later step testable by a player.
2. Hold to aim and selection on number keys (4.1, 4.2).
3. Attack with a target and a place (3.1), with the AI lead.
4. Shapes and facing (4.3), hold where you stand (3.2), charge preview (3.3).
5. Markers, then minimap (4.5).
6. Split, merge and pre-set groups (4.4), with the preset save change.

## 8. How it is checked

- Rule checks for each refusal code, for empty-group charge not starting the cooldown, and for focus then hold.
- `GRAVES_COMMAND_UI_CHECKS` suites extended: a key press with the mouse buttons held still swings; number keys select with the panel closed; two selected groups raise two cards.
- A bot simulation run to confirm bots can issue attack-move and focus through the same path.
- A live match by Kort for feel: aim timing (150 ms tap, 300 ms double tap) is tuned from his play.

## 9. Open points

- Q, E and F for relics is the polish lead's choice (Kort left it open). If a played match shows E for relic 2 is hit by habit, swap to Q, F, G.
- Whether allies' groups show on the minimap as well as allies' commanders. Proposed: commanders only, to keep it readable.
