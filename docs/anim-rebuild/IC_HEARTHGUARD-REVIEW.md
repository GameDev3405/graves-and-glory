# ic_hearthguard review record (2026-11-06)

The first axe unit, the Iron Clans hearthguard: a hand axe and a round shield (`axe_shield`). Its builder reuses the knight's shield guard, gait, stances, hits, block and falls, and replaces only the attacks: `GG_AXE_TROOP=ic_hearthguard python art/anim/axe/build_axe_anims.py`. Its rig comes from `art/anim/common/troop_rig.py ic_hearthguard`, with the axe rolled so the bit faces the foe in guard and leads every chop.

- **Attacks:** overhead_chop (impact 10, head) and hook_hew (impact 8, body), each with an `_legs` clip, inside a 24-tick clip under the variant's 28-tick cooldown. The timeline `impact` marker sits on the server's impact tick.
- **Cloth:** the knight's cape lag is turned onto the tabard (1.0) and the beard braid (0.6); the flare is dropped so the tabard never swings into the thighs.
- **Knight builder:** `SHIELD` now includes ic_hearthguard, so the shield guard, block and shield-arm falls apply. Other troops are unchanged.
- **Round 1 fix: the axe stayed above the floor.** In the chop's follow-through the axe dipped under the soles. The bite now stops above them; `AXE_PROBE=1` prints the axe's lowest point per swing.
- **Round 1 fix: overhead_chop.** The drop now reads over two frames (t9 mid-arc), the bit lands at chest height on the impact tick, the follow-through carries it down to the bite, and the gather lifts it rather than letting it dangle.
- **Round 1 fix: hook_hew.** The hew comes across from high right so the bit is crossing the body on the impact tick, carries past the left hip, then the elbow folds and the bit hooks back toward the belly.
- **Round 1 fix: left death.** Rolling face down over the shield arm, the round shield stood up off the back. A dedicated `ROLL_LEFT` wrist turns it the other way, so it lies flat over the back.

## Review

- **Round 1 (Sonnet high): SHIP WITH MINORS.** The fixes above.
- **Round 2 (Sonnet high, reel `reel_hg_v2`): PASS. No blockers.**
- **In-game checks** (round 2 clips): PASS, 22 of 22 checks, 0 false. The first run failed only on two harness assumptions (3 attack clips of 22 ticks); `GeoKnightChecks` now reads the troop's attack variant count and clip length.
- **Verdict: finished.** Video: `ic_hearthguard_v2.mp4`.

## Open minors (polish, not blocking)

- **overhead_chop** (t303→t304): the drop is a one-frame jump with no smear.
- **hook_hew** (t338–344, side view): the bit stays near chest height.
- **Shield:** no secondary lag in either attack.
- The first and last hit reactions are weaker than the middle ones.
- The ready idle's weight shift is small.
- The death framing crops part of the body in the reel.
