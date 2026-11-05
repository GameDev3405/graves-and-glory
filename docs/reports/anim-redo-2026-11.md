# Animation redo, 2026-11 (art/anim-redo) — in progress

Kort, 2026-11-05: redo every troop animation from scratch with the `/animate-troop` workflow. Decisions: Fresh
Animations-style motion on the Excalibur-style art; waist (`chest`) and ankle (`*_foot`) bones on every biped; procedural
walk/run kept and retuned; idle/guard stance clips authored per family; one clip set per shared family.

## Inventory (main at 1d3f2edd)

30 troops, 5 Houses, 18 clip families, 93 clips. Timing columns: length in ticks, `impN` = attackVariants `impactTicks`
(gameplay; unchanged unless Kort approves). Stance keys (MotionEngine.stanceKey): idle, carry, guard (Hold, enemy near,
charge), shield, brace, wall, aim, rest; a family without a stance clip falls back down `stanceChain`, then to the
procedural engine only. Walk and run are procedural (MotionBody), not clips.

| rig/family | troops | n | clips |
|---|---|---|---|
| biped/axe | ic_breaker(ir) | 3 | axe_haft_jab 20t imp7, axe_overhead_chop 26t imp12, axe_side_hew 25t imp12 |
| biped/axe_shield | ic_hearthguard(ir) | 2 | axe_shield_hook_hew 21t imp8, axe_shield_overhead_chop 24t imp10 |
| biped/bow | archer(gi) longbow(gi) bonebow(gr) stalker(gr) wm_hunter(wi) | 5 | aim 60t loop, bow_direct 36t imp16, bow_draw 24t, bow_volley 36t imp16, idle 80t loop |
| biped/caster | mage(gi) hexer(gr) wm_seer(wi) tc_mist(ti) | 1 | caster_cast 24t imp10 |
| biped/common | all bipeds | 31 | death 28t, death_arrow 36t, death_arrow_2 28t, death_back 28t, death_blunt 32t, death_blunt_2 38t, death_left 28t, death_magic 38t, death_magic_2 28t, death_right 28t, death_slash_back 28t, death_slash_back_2 38t, death_slash_front 28t, death_slash_front_2 38t, death_slash_left 28t, death_slash_left_2 38t, death_slash_right 28t, death_slash_right_2 38t, death_stab 34t, death_stab_2 28t, fall 24t, hit_body 11t, hit_body_2 12t, hit_body_3 13t, hit_head 10t, hit_head_2 10t, hit_head_3 11t, hit_legs 12t, hit_legs_2 13t, hit_legs_3 14t, raise 20t |
| biped/crew | ic_mortar(ir) | 1 | crew_fire 26t imp8 |
| biped/crossbow | ic_arbalist(ir) ic_ballista(ir) | 1 | crossbow_loose 31t imp4 |
| biped/greatsword | gb_hollowking(gr) | 3 | greatsword_overhead_cleave 26t imp12, greatsword_side_cut 25t imp12, greatsword_thrust 22t imp7 |
| biped/scythe | reaper(gr) | 2 | scythe_overhead_hook 26t imp12, scythe_reap 26t imp12 |
| biped/spear | wm_tusk(wi) tc_tideguard(ti) tc_admiral(ti) | 3 | spear_high_thrust 22t imp7, spear_low_sweep 22t imp8, spear_thrust 22t imp7 |
| biped/sword | revenant(gr) tc_raider(ti) | 3 | sword_low_cut 22t imp8, sword_overhead 24t imp10, sword_thrust 22t imp7 |
| biped/sword_shield | knight(gi) sentinel(gi) gl_marshal(gi) | 25 | block 10t, block_2 12t, block_head 11t, block_legs 12t, brace 60t loop, carry 40t loop, guard 48t loop, hit_body 13t, hit_body_2 14t, hit_body_3 15t, hit_head 11t, hit_head_2 11t, hit_head_3 12t, hit_legs 13t, hit_legs_2 14t, hit_legs_3 15t, idle 80t loop, shield 40t loop, sword_shield_low_cut 22t imp8, sword_shield_low_cut_wall 22t, sword_shield_overhead 24t imp10, sword_shield_overhead_wall 24t, sword_shield_thrust 25t imp7, sword_shield_thrust_wall 25t, wall 60t loop |
| biped/thrower | tc_harpooner(ti) | 1 | thrower_throw 24t imp8 |
| colossus/colossus | ic_colossus(ir) | 2 | colossus_backhand 24t imp8, colossus_slam 30t imp12 |
| hound/bear | wm_oldhorn(wi) | 2 | bear_bite 24t imp8, bear_maul 26t imp10 |
| hound/hound | wm_hound(wi) | 2 | hound_bite 24t imp8, hound_lunge 26t imp8 |
| hound/ravager | tc_brute(ti) | 2 | ravager_bite 24t imp8, ravager_charge 28t imp10 |
| spider/spider | wm_stalker(wi) | 2 | spider_bite 24t imp8, spider_lunge 26t imp8 |

New stance clips to author (families with none today): idle, carry, guard and, where the family braces, brace, for
sword, spear, axe, axe_shield, greatsword, scythe, crossbow, thrower, caster, crew; guard and rest for bow; idle for the
hound, spider and colossus rigs.

House order (first House that uses a shared family redoes it):
1. Gilded: common (hits, deaths, fall, raise), sword_shield, bow, caster.
2. Graveborn: sword, scythe, greatsword.
3. Wildmoot: spear, hound, spider, bear.
4. Ironclad: axe_shield, axe, crossbow, crew, colossus.
5. Tidecallers: thrower, ravager (spear, sword already done).

Known defects to fix on the way (docs/reports/art-qa-ingame-attacks-2026-11.md): bow hidden at the side view on the five
bow troops; tc_brute rest pose dangles below the feet.
