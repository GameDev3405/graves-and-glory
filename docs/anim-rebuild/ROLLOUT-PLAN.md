# Knight-feel rollout plan (Kort approved, 2026-11-07)

Kort: carry over "the whole feel and flow of [the Knight's] movements, its weight shifts, and style" to every unit that can take it. The Knight's v2 style and v4 deaths (KNIGHT-REVIEW.md) are the template. STYLE.md and Kort's reference reel are still the bar.

## Groups

| Group | Units | Builder | What carries over |
|---|---|---|---|
| A. Shield fighters | sentinel, gl_marshal, ic_hearthguard | knight (`V2 and SHIELD`), axe | Everything: v2 style plus the 12 v4 deaths with their own gear |
| B. One hand, no shield | revenant, tc_raider | knight (no SHIELD) | v2 drive, graded hits, idle weight and distinct stances, rebuilt for an empty off hand. Deaths drop one weapon. |
| C. Two-handed heavy | wm_tusk, tc_tideguard, tc_admiral, ic_breaker | spear (+greataxe) | Pelvis-driven blows, graded hits, idle and stance weight shifts. Deaths drop a two-handed weapon. |
| D1. Bows | archer, longbow, wm_hunter, bonebow, stalker | bow | Graded hits, idle weight, stances, deaths that drop the bow. The shot stays. |
| D2. Other ranged | ic_arbalist, tc_harpooner | crossbow, thrower | As for D1 |
| E. Casters | wm_seer (done), tc_mist, mage, hexer | caster | Caster v2 (CASTER-V2-DESIGN.md). Hits graded; deaths fold the robe and drop the prop. |
| F. No GeckoLib set yet | wm_hound, wm_stalker, wm_oldhorn, tc_brute, ic_ballista, ic_mortar, ic_colossus, reaper, gb_hollowking | new | The spirit only (weight, anticipation, graded reactions). Each is new work. |

## Order

1. Finish what's running: group A (deaths, random death pick, v2 style); tc_mist, then mage, then hexer, one at a time.
2. Shared deaths: move the knight's v4 death system (`d3_*`, `_d3_beside`) from `art/anim/knight/build_anims.py` into `art/anim/common/`. Any builder can then call it with its own gear list and hands. This waits until group A has released the knight builder.
3. Family passes, in parallel. Each family runs in its own worktree and branch. Finished families are merged locally into `anim/geckolib-rebuild` (Kort approved local merges; nothing is pushed).
   - C and D1 start right away with the style pass. Each adds deaths once step 2 lands.
   - B starts after step 2.
   - D2 starts when a harness slot frees.
4. Group F: beasts, then siege, then the colossus, then reaper and gb_hollowking. Existing units come first if time runs short (Kort).

## Each unit

1. Build and probe: clipping, floor, and gear in the body.
2. Reel. One Sonnet high review. At most 2 fix rounds; record the minors.
3. Harness: geo-knight checks with an isolated root, at most 4 clients at a time. Pass = 22 `[p1] p1-` lines and 0 `=false`.
4. Commit, and add the result to the unit's review md.

One video per family goes to Kort when the family is finished (Kort's choice).
