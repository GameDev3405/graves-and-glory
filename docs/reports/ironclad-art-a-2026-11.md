# Ironclad art A: Shieldbreaker and Arbalist (tier I), 2026-11-01

Branch `claude/ironclad-art-a-f8b99d`. Built through the 1-texel handlib generators (not hand-edited JSON):
`art/hand/ic_breaker/` (shared `ironclad.py` kit + Shieldbreaker), `art/hand/ic_arbalist/`, `art/hand/weapons/build_ic_weapons.py`
(`ic_axe`, `ic_crossbow`). Catalog stubs and skin copies in `art/troops/ic_*`; generated assets via `tools/build_skins.py`.

## Status (separate)
- **Implemented:** both troops and both weapons build and install; ember accents, mail under plate, fur, riveted lames,
  heraldic tabard, no back flags (the Arbalist quiver only), team side cloth.
- **Tested:** `build_skins.py --verify` (172 outputs) OK; standalone unit tests 206 OK (tint rule, z-fight, quad caps included).
  `verify.py fast --offline` was **deferred**: a Minecraft client from another worktree was running, so it did not execute. Re-run once it exits.
- **Visually inspected:** offline turn sheets (11 px) and 36 px contact sheets in `work/ironclad-art/` (git-ignored):
  `breaker_big.png`, `arbalist_big.png`, `breaker_36px.png`, `arbalist_36px.png`, `weapons.png`.
  **No in-game turn shots**: the foreign client blocked harness use; not done.
- **Accepted:** no. Needs Kort's House gate (2B.5).

## Self-score vs the 9/10 gate: about 6.5-7 / 10, below the gate
- Good: plume and ember trim, cleft iron shield emblem, layered pauldrons, dark-to-light read, distinct silhouettes.
- Weak: Shieldbreaker chest is cluttered and the sallet face (two eye dots plus nasal bar) reads oddly; the front plates are hidden by the tabard.
  Arbalist mail checker pattern is still strong on the arms; the emblem is low-contrast teal/grey; the crossbow string is light and wide.
- Two automatic correction rounds used; remaining items are recorded here, not iterated further.

## Follow-ups
Chest/helm simplification, mail pattern softening, crossbow string, in-game shots, rerun `verify.py fast --offline`.
