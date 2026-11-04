# Command panel and troop orders redesign

Status: APPROVED by Kort 2026-11-04
Mockups: https://claude.ai/artifact/CG8htdqYoRbqCWeEfEnozJ (variation 2, "Pennants down the left edge", was picked).

## Why

Kort's complaints about today's command UI (BattleHud cards "Attack / Follow / Formation"):

- It's hard to find the right order.
- The orders feel too much alike.
- Nothing animates, so clicking feels like nothing.

The War Table's big banner UI can't be reused because it would cover the battle. The new panel must look like Graves and Glory, stay off the centre of the screen, and make every order feel distinct and weighty.

## Goals

- A panel on the left edge that leaves the middle of the battle visible.
- Five orders, each clearly different in what troops do: Attack, Charge, Follow me, Retreat, Formation.
- Ground outlines showing exactly where troops will stand before you commit.
- Strong feedback on every order: visual, sound, and the troops reacting.

## Non-goals

- New troop types, balance retuning beyond the order rules below. Balance is closed (Kort 2026-11-02); the numbers here are starting values for playtesters to tune.
- Changing map pings. They stay as team markers, separate from orders.

## Anti-goals

- Anything that blocks the view of the fight in the centre of the screen.
- Opaque UI. The banners must be see-through.
- Orders that look or behave the same.

## Panel (decided by Kort)

- **Opening:** the existing `menu` key, R by default, toggles it. While it's open the player can still walk. Right-mouse drag turns the camera, and left click selects troops.
- **Look:** see-through swallowtail pennants (about 55% opacity) in the House colour, with gold trim, flying from a pole on the left edge. There is one pennant per order, each with an icon, its name and its hotkey.
- **Group pills:** glass pills sit above the pennants.
- **Formation tray:** the Formation pennant opens a glass tray to its right with the four shapes. It folds away after you choose.
- **Selection:**
  - Click a group pill or a troop to select it. Shift-click adds more.
  - When the panel opens, the last selection is still picked.
- **Groups:**
  - Made for you: Infantry, Archers, Riders, All.
  - Players can also build custom numbered groups in the Armoury.
- **Keys:**
  - Orders: Attack Z, Charge X, Follow me C, Retreat V, Formation B. These work even with the panel closed, acting on the last selection. All can be rebound.
  - Number keys 1 to 4 pick groups only while the panel is open, or pick shapes while the formation tray is open. With the panel closed they are the normal hotbar.
- **After an order:** the panel slides shut, unless Shift is held so you can give more orders.

## Order behaviour (decided by Kort)

| Order | What troops do |
|---|---|
| Attack (Z) | From where they stand, each troop goes for the nearest enemy and keeps fighting until every enemy they can see is dead. Then they hunt further across the map. |
| Charge (X) | The camera lifts and hovers over the selected troops, and an arrow on the ground sets the direction. On release the camera snaps back; an accessibility toggle keeps it overhead until a key is pressed. Troops sprint (about 1.25x), and the first hit after the run-up deals bonus damage and knocks back. After impact they switch to Attack. Archers and mages advance with the charge, stopping to shoot rather than entering melee. 20-second cooldown per commander. |
| Follow me (C) | Troops form up behind the player in their current formation. |
| Retreat (V) | Troops run to the player without fighting, then switch to Follow me. |
| Formation (B) | Pick Line, Shield wall, Wedge or Loose. An outline of each troop's spot is painted on the ground at the cursor, facing the player's look direction. Clicking sends them there. Once in place, ranged troops shoot anything in range and melee troops hold position. |

Move and Hold are removed. Formation covers both.

### Formations

| Formation | Rules |
|---|---|
| Line | One or two ranks, side by side. No special rules. |
| Shield wall | Front-rank shields block most arrows from the front, and troops walk slower. With no clear front (enemies on several sides) it closes into a square with shields facing out on every side. |
| Wedge | A point at the front. Walking into enemies shoves them aside and breaks their line. |
| Loose | Wide spacing, so arrows and area spells hit fewer troops. |

## Feedback (decided by Kort: all four)

1. The pennant presses in with a gold flash, and a short horn or drum plays for each order.
2. A pulse ripples across the ground from the troops' target, and the outline stays lit for about 1 second.
3. The selected troops raise their weapons, and an officer shouts once.
4. The panel slides away, unless Shift is held.

## Multiplayer

- You and your teammates see your formation outlines; teammates see them faint. Enemies never see them.
- Pings stay as they are.

## Clarifications from the builder check

- **Selection is by whole group.** Clicking a troop selects that troop's group; there is no per-soldier ordering. Shift-click adds more groups. Kort's answer was "click a group".
- **Attack has no range limit.** Once no visible enemy is left, troops hunt across the whole map (Kort: "hunt further"). It replaces today's 24-block engage radius.

## Builder's calls (not decided by Kort, change if wrong)

- **Retreat:** troops don't attack while retreating. No new damage rules apply; normal shield blocking still works.
- **Charge target:** the arrow sets a direction. Troops run up to 32 blocks along it (today's charge radius) or until they meet an enemy, whichever comes first, then switch to Attack. Bots and AI commanders get the same 20-second cooldown.
- **Old orders:**
  - Move, Hold and Compact leave the player UI. Their order ids stay loadable so older saves still work.
  - A saved Hold or Move loads as Formation Line at that spot. A saved Compact loads as Shield wall.
  - The camp lessons and tutorial text get updated to match.
- **Keys and screens:**
  - Z X C V B replace today's attack (G), follow (V) and formation (B) bindings.
  - The new panel replaces the old R menu screen and today's CommandMode aiming flow. Enter and Backspace stay as extra confirm and cancel keys.
- **Custom groups** are stored with the army's Armoury loadout, so they last between battles.
- **Outline sharing:** the aiming preview exists only on your own screen. When you confirm, the server sends teammates the outline. They see it faintly until the troops arrive, or for at most 10 seconds.
- **Charge cooldown:** shown as a darkened pennant with a sweep.
- **Charge camera:** server-authoritative orders. The overhead camera is client-only.
- **Key conflicts:** in vanilla, C and X are the creative toolbar save and load keys, so they clash in creative. Players can rebind them in Controls.
- **Bots and AI commanders** use the same order set.
- **Sounds:** order sounds reuse the existing horn_charge, horn_rally and horn_rout, plus candidates from question 107. Missing sounds come from the audio-cue pipeline.
- **Numbers:** the bonus damage and knockback, the Shield wall arrow block and speed penalty, and the Loose spacing live in Spec (tools/preflight.py), so playtesters can tune them.

## Constraints

- Fabric client and server split: the server owns order state, and the client renders the panel, outlines and camera.
- Spec.java is generated by tools/preflight.py and is never hand-edited.
- Order and formation changes touch the save format or protocol, so they need Kort's approval gate per WORKFLOW.md. This document is that request.
- Every behaviour must be proven by real game tests through the client harness, never computer-use.
- The visual standard is the existing UI art rules.

## Left to the builder

- Pennant animation (sway, slide-in timing).
- How outlines render (decals or particles).
- Exact rank spacing per formation.
- How custom groups are stored in the Armoury, as long as existing saves load.
