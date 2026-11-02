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

## Second pass (this branch, 2026-11-01)
Cherry-picked 55cbe09 onto `art/ironclad-1x`, then: Shieldbreaker helm face reduced to visor, slit and nasal ridge (breath-hole "smile" removed); tabard emblem shrunk to a cloven shield so the team field reads. Arbalist mail softened to a 2-texel check one tone apart, quiver studs dimmed, tabard emblem changed to a single iron bolt with ember fletch, brim narrowed to 11. Crossbow string now a V from the prod tips to the nock (was a flat bar). Axe unchanged.
- Turn sheets: `work/ic/breaker_36.png`, `work/ic/arb_36.png` (git-ignored), weapons `work/ic/weap.png`.
- `build_skins.py --verify` OK (172 outputs). `verify.py fast --offline` GREEN (after cherry-pick cb3aa9d). In-game harness shots not yet taken.
- Honest self-score: ~7.5/10, still under the 9 gate (arbalist back/cape reads checkered; breaker chest emblem small). Not accepted.
