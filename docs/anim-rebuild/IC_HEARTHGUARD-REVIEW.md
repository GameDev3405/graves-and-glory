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

# ic_hearthguard: v2 style + v4 deaths (2026-11-07)

The hearthguard now builds in the v2 shield style (Kort approved v2 as the default for shield units), so its two attacks, upper stances, hits and block changed, not only the deaths. It also has the knight's 12 v4 deaths: `death_{back,forward,left,right}`, each plus `_drop1` (drops the axe) and `_drop2` (drops the axe and the shield). The export goes from 42 to 50 clips. It is the first rig without a cape to take the D3 deaths, so `_d3_tracks` in `art/anim/knight/build_anims.py` now skips keyed bones that are not in the troop's `DEATH_BONES` (without it the axe build fails with KeyError 'cape'). The guard cannot change a troop that built before, since any skipped key would have raised.

- **Supersedes:** the old "death framing crops part of the body" minor above; the v4 reel frames the whole fall.
- **Byte-identical (parsed JSON):** knight, revenant, tc_raider, archer, ic_arbalist, wm_tusk, tc_harpooner, ic_breaker and tc_mist build the same with and without the guard; gl_marshal with the guard matches its committed export. D3 stays gated on `D3_TROOPS`.
- **Review (Sonnet, high effort, reel `ihg_v2`): SHIP WITH MINORS.** It covered the whole set (gait, stances, idle, 2 attacks, 7 hits, 12 deaths) from contact sheets, 12 death sheets and the full reel. No blockers: every death settles on one floor line, the shield always lands flat, and all four falls read in their direction. No fix round was needed.
- **Harness (geo-knight, `GRAVES_GEO_CHECK_TROOP=ic_hearthguard`):**
  - Run 1: 22 p1 and 1 `=false`. That failure was the march run-side check under lag (3 "Can't keep up"). The suite rollup line follows from it.
  - Rerun: 22 p1 and 0 `=false`.
  - The death clips that played included `death_back_drop1`, `death_back_drop2`, `death_forward_drop1`, `death_forward_drop2`, `death_left`, `death_left_drop2`, `death_right` and `death_right_drop2`.

## Open minors (v2 + v4, polish, not blocking)

The shared v2 and v4 minors in `SENTINEL-REVIEW.md` and `GL_MARSHAL-REVIEW.md` apply here as well.

- **death_left and death_right (all variants):** they end as a propped, hunched slump with the torso at about half height, not lying flat. The pattern is the marshal's, but the stocky body and wide pauldrons make it more obvious.
- **death_left (shield kept):** the shield tucks under the torso, so the direction reads from the body, not the gear.
- **death_back (kept gear):** at t=13-15 the axe ends past the head and the shield past the feet; the silhouette is wide and busy.
- **death_back_drop1 and drop2:** the axe lands slightly above the head line at t=13-15 and settles flat by t=18. It is a soft arc, not a pop.
- **death_forward drop variants:** the dropped axe ends partly under the forearm and the shield edge, crowded near the body.
- **death_right_drop2:** the shield lands about a body-width away, with the axe beside it.
- **Not seen in game:** only the harness and the offline reel were run.
