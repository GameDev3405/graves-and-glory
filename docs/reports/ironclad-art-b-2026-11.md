# Ironclad art B: Ballista Crew, Mortar, Brassjaw (1 texel)

Branch `claude/quirky-boyd-6057ca`. Rebuilt from scratch on the one-texel `art/hand` pipeline; the earlier 3-texel attempt (`claude/ironclad-art-b-197667`) was used only for silhouettes, kit and naming.

## What was built
| Troop | id | Silhouette |
|---|---|---|
| Ballista Crew | `ic_ballista` | Crewman (cap with ember brow, ember plume, mail, lames, team tabard) behind an oak engine: two iron-shod wheels, axle, stock with brass-headed bolt, rope torsion skeins, sprung arms and cords, windlass with crank bar, team-cloth mantlet. |
| Ironhold Mortar | `ic_mortar` | Scorch-bearded gunner, goggles on the brow, leather apron, big left spaulder, brass-hooped tube slung diagonally on the back with an ember-lit bore, shell bandolier, powder keg. No back flag. |
| Brassjaw | `ic_colossus` | Soot-iron golem on the colossus rig: furnace-grille chest, twin smokestacks, brow over ember eyes, brass jaw, banded arms with brass knuckles, team-cloth apron (front only), oak-and-iron slag maul with a lit vein in the right hand. |

All three are tier I only. Siege engines and the maul are bones on the 1-texel skin sheet (`weapon: "none"`).

## Files
- Builders: `art/hand/ic_ballista/build_ic_ballista.py`, `art/hand/ic_mortar/build_ic_mortar.py`, `art/hand/ic_colossus/build_ic_colossus.py`.
- Shared: `art/hand/ic_ballista/siege_kit.py` (oak, brass, rope, hoops, the crewman), `art/hand/ic_breaker/ironclad.py` (house palette, identical to the copy on `claude/ironclad-art-a-f8b99d`).
- Stubs `art/troops/ic_{ballista,mortar,colossus}.json`; generated models, textures and `models/troop/index.json` entries (only existing textures listed).
- Turn sheets (crimson): `docs/reports/ironclad-art-b-2026-11/ic_{ballista,mortar,colossus}_turn_crimson.png`.

## Checks and status
- Lit (glow) texels: Mortar 9, Brassjaw 10, Ballista 0 (cap 16).
- Team colour lives only on the cloth sheet.
- Fast verify: green (`python tools/verify.py fast --offline`, report work/verify/20261101-184157-49536/result.json).
- Not run: game tier (a client was running; no compilation or harness launch was attempted).

| Status | Ballista | Mortar | Brassjaw |
|---|---|---|---|
| Implemented | yes | yes | yes |
| Tested (fast verify) | pass | pass | pass |
| Visually inspected (turn sheet) | yes | yes | yes |
| In game | no | no | no |
| Accepted | no, awaits Kort | no | no |

## Self-score against the 9 gate
Ballista 8.5, Mortar 8.5, Brassjaw 8.5. Honest read: all three read clearly as Ironclad siege troops from the five turn views, but none is confirmed at 9 until seen in game.

## Unresolved defects
- Mortar tube now tilts forward over the shoulder (second pass) so the ember bore shows in side views; still not seen in game.
- Ballista engine sits close to the crewman; arm and animation clearance (`bow_draw` clip) is unchecked in game.
- Brassjaw maul stands beside the right arm; its swing with the colossus attack clip is unchecked.
- Ballista has no glow; its ember lives in the plume and brow band only.
- Needs in-game views (menu portrait, small size) before Kort's look.

## Comparison with 891904e (the earlier 1-texel attempt)
Coordinator pointed out 891904e was already 1-texel. Rendered both side by side: the fresh builders here are brighter on Brassjaw (arms and legs were near-black in 891904e) and add the maul; the Ballista engine is bulkier and more detailed. 891904e's Mortar shows its ember bore from the front, which this version does not (listed above). Kept the fresh versions for all three; the mortar bore angle is the one thing worth taking from 891904e. Palette module is `ironclad.py` shared with session A, not 891904e's `ic_style.py`.

## Second pass (rebased on main)
Mortar tube retilted to (-28, 0, -26). Fast verify green again. In-game turn check NOT run: about nine Minecraft client processes were running machine-wide, over the cap of 5, and the game tier is only a combat smoke that does not spawn these troops. Self-score unchanged at 8.5 until seen in game.

## Third pass (Kort: push every troop to 9)
Added a bolt quiver to the Ballista crewman (back gear, no flag), brighter wheels, a shorter stock to fit the sheet; Brassjaw gained a brass-banded backplate and two flues so its back is no longer a flat dark slab. Self-grade from the turn sheets: Ballista 8.5, Mortar 8.7, Brassjaw 8.8. Not claimed at 9: none seen in game, and the Ballista wheels and crewman-to-engine relationship are still its weakest read.
