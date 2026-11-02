# 4B.4 Gilded completion: handoff (2026-11-02)

Branch `art/gilded-tiers` (from `main` a49c9a1), worktree `C:/Users/kortt/Melty/bannerbound-gilded`.

## Done
- Mage (`art/hand/mage/build_mage.py`): tiers I to III. II: steel spaulders edged in gilt, a white shoulder cape with
  the sun, a gilt cowl lip, gilt bell cuffs, a leather belt with gilt plaques. III: gilt spaulders, a sunburst crown
  (circlet and five rays), gilt scroll on the robe tiers, a gilt cape border. Contact renders: `work/gilded/mage_all.png`.
- Marshal (`art/hand/gl_marshal/build_gl_marshal.py`): painters only, half done. `gild`, per-tier `cuirass/arm/leg`,
  `crown`, `sun_disc` and a per-tier `pauldron(part, tier)` written.

## Left
1. Marshal: wire bones to the per-tier painters (`per_tier(cuirass)` and so on), add the fur collar (II, III), the team
   cape with the sun (II, III; gilt border at III), the crown and sun disc (III); drop `pauldron_old`; remove
   `tiers=(1,)`; render and inspect.
2. Veteran checks: knight and sentinel have no veteran trim; add one each. Portraits: sentinel is `tall 4` against
   the spec's Tall +2 (its tier III cross needs about 7 px over the head); question for Kort.
3. `python tools/build_skins.py`, then `--verify`, `python tools/art_hue_check.py`, `python tools/verify.py fast --offline`
   (JAVA_HOME Adoptium 17.0.19.10); revert CRLF-only diffs in sheets/preflight.json and Spec.java.
4. 4x nearest-neighbour contact sheet of every new tier in `work/`; commit with the trailer.
