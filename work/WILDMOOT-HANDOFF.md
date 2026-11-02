# Wildmoot tier I handoff (task 4B.1 part 1)

Branch `art/wildmoot-t1` in worktree C:/Users/kortt/Melty/bannerbound-wildmoot (from main a49c9a1). Nothing pushed or merged.

## Done
- `art/hand/common/wildmoot.py`: shared Wildmoot kit (palettes incl. OCHRE accent, CLAY warpaint; painters). wm_tusk untouched.
- `art/hand/common/house_stub.py`: stub writes `meta["rig"]` (hound, spider) and has a `stub` command (no build_skins run).
- Builders (Sonnet workers, tier I, 1 texel/px, no back flags, bows reuse `order_bow`):
  wm_hunter (8), wm_seer (7.5, weapon none + modelled bone wand, glow eyes), wm_hound (7.5, hound rig),
  wm_oldhorn (6.5-7, hound rig, no war banner: team mantle instead), wm_stalker (7.5, spider rig). Stubs in art/troops/.
- `work/wildmoot_turn_sheet.py` -> `work/wildmoot-t1-turn-4x-crimson.png` (1x render, 4x nearest-neighbour).

## Checks (2026-11-02)
build_skins --verify: 276 outputs verified. art_hue_check: wildmoot PASS (99% in band, 6 skins). verify.py fast --offline: PASS. CRLF-only preflight.json/Spec.java diffs reverted.

## Left (steps 1, 2 and 4 now done)
1. `python tools/build_skins.py` then `--verify`; `python tools/art_hue_check.py` (Tidecallers FAIL is pre-existing on main).
2. `JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot" python tools/verify.py fast --offline`; revert CRLF-only diffs in sheets/preflight.json and Spec.java.
3. Optional polish: Old Horn body blocky/noisy; Seer back noisy, staff top pokes beside head; Stalker silk sacs plain.
4. Commit with the Opus 5.5 trailer.

## Questions for Kort
- Old Horn's spec war-banner between the tines vs the no-flag rule (built as a team mantle instead).
- Seer's `bone_wand` weapon asset does not exist; wand is modelled into the hand.
