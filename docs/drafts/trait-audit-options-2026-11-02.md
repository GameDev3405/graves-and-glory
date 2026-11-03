# Vanilla-trait audit: options for Kort (2026-11-02)

Draft, no stat or rule changed. Sources: `docs/reports/vanilla-trait-audit-2026-11.md` (the options) and `docs/reports/audit-measure-2026-11.md` (engine measurement of option A; 720 fights, seed 7, +/-3 points per House). Baseline House win %: Graveborn 60.0, Tidecallers 54.9, Ironclad 45.0, Gilded 39.3 (band 40-60).

The audit labels its options A to G. A was test-only and is done; B to G need your pick.

- **A (done, test only):** Measured the engine with scratch flags for no hurt cooldown and for troop damage ignoring armour; nothing shipped.
- **B (hurt-cooldown rule):** Zeroing the per-victim hit cooldown before each troop blow lifts Gilded 39.3 to 50.0 but drops Tidecallers to 38.8 and Graveborn to 50.4 (Ironclad 59.2); the biggest lever measured, but overshoots, so a partial form (shorter, or ranged only) would need its own run.
- **C (armour):** Folding armour into health (or `troop` bypassing armour) leaves Gilded at 36.6, lifts Tidecallers to 64.3 and lowers Ironclad to 39.6; not a Gilded fix, only worth it for clarity or to rein in Tidecallers (option ii, show the number, is the cheap version).
- **D (magic through armour):** Hexer, Seer, Mist and Mage take the armour cut today; not measurable, Hexer moved 0 to -6 points (noise) and the other three were not in the fight set.
- **E (knockback resistance and arrow knockback):** Zero knockback resistance and decide whether arrows push; not tested, so no evidence for or against.
- **F (retarget or refund shots whose target died in flight):** 6.2% of shots are lost at baseline (about 3% for Gilded archer and longbow, 12.6% ballista, 8.1% harpooner), so it would not move Gilded.
- **G (show armour and effective HP on troop cards, display only):** No gameplay change; armour moves single-troop win rates by 20 or more points (reaper, hearthguard, raider), so it is cheap insurance.

Both B and C together put all four Houses at 48-52 (Graveborn 50.4, Tidecallers 48.2, Ironclad 51.9, Gilded 49.1), but that is a large rule change.

Separate item, not part of this audit: `work/house-audit` holds a stat-only House retune (967e980, Gilded 42.4, Graveborn 58.8, Ironclad 43.7, Tidecallers 54.6 on its own sim base) still awaiting your decision. It was cut from an older main (cc881860) and measured on a different plan (1452 cross-House duels), so its numbers are not directly comparable with the table above.
