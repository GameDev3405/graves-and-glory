# Tidecallers tier I handoff (task 4B.2 part 1)

Branch `art/tidecallers-t1` in worktree C:/Users/kortt/Melty/bannerbound-tidecallers (from art/tideguard-sea-blue a60e432). Nothing pushed or merged.

## Done
- `art/hand/common/house_stub.py`: ported Wildmoot's `meta["rig"]` and `stub` command.
- `art/hand/weapons/build_house_weapons.py`: new `cutlass`, `harpoon`, `admiral_trident` (art/weapons/*). Cutlass bow_lo moved to stop a z-fight.
- Builders (Sonnet workers, tier I, 1 texel/px, no back flags, sea-blue accent): tc_raider (7), tc_harpooner (7), tc_mist (7, weapon none, 5 glow texels), tc_brute (6.5, hound rig, weapon none, portrait "wide 28"), tc_admiral (6.5, anchor on back; imports tc_harpooner painters). Stubs in art/troops/.
- `work/tidecallers_turn_sheet.py [side] [ids]` -> `work/tidecallers-t1-turn-4x-teal.png` (labelled rows, 4x NN).
- Brief for workers: `work/TIDECALLERS-BRIEF.md`.

## Checks (2026-11-02)
build_skins --verify: 288 outputs verified. art_hue_check: tidecallers PASS (68% in band, peak 212, 6 skins). verify.py fast --offline: see commit message / follow-up.

## Polish left
Raider kelp dense on back; Harpooner coat dark, brim tail flat; Mist upper body dark, skirt may clip; Brute boxy hind, big flat teal mantle; Admiral tricorne boxy, tails muddy, anchor faint. Weapons not seen in hand.

## Questions for Kort
- Brute: more bulk (shoulder hump, heavier forelegs)? Portrait "wide 28" vs "beast 28".
- Admiral coat: lighter/bluer like the Tideguard, or dark for menace?
