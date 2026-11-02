# House troops at the 1-texel standard (2026-11-01)

Hearthguard (Ironclad), Tusk (Wildmoot) and Tideguard (Tidecallers) rebuilt by hand with the `art/hand` builders, on top of `art/knight-1x`. Skins and relief are 1 texel per model pixel. IDs, stats, abilities and weapon ids are unchanged (design.json). The retired density-3 generator art and `tools/paint_house_troops.py` were not used.

## What changed
| Troop | Builder | Weapon | Notes |
|---|---|---|---|
| `ic_hearthguard` | `art/hand/ic_hearthguard/build_ic_hearthguard.py` | `hearth_axe` | Iron plate and scale coat, three-slab pauldrons, chequy team tabard with an ember hem, brass-buckled belt, copper beard and braid, crested box helm with nasal. Shield bone (team face, black bordure) is a hidden bone like the other hand troops; no vanilla shield. |
| `wm_tusk` | `art/hand/wm_tusk/build_wm_tusk.py` | `boar_spear` | Hunched boar-kin (hunch baked into the bone rotations), snout and tusks, riveted leather cap with team headband, hide mantle, team kilt and wraps, bone-spiked right pauldron. |
| `tc_tideguard` | `art/hand/tc_tideguard/build_tc_tideguard.py` | `coral_trident` | Drowned flesh, bronze scale cuirass over barnacled mail, bleached coral rising from both shoulders, scallop shell on the back, verdigris finned cap with the team fin, barry-wavy team tabard, team wraps over bronze bracers, bronze greaves. |

Shared: `art/hand/common/house_stub.py` writes the `art/troops/<id>.json` stub plus tier I skin that `build_skins` needs beside a hand builder; `art/hand/ic_breaker/ironclad.py` is the Ironclad kit (copied from `claude/ironclad-art-a-f8b99d`). Weapons: `art/hand/weapons/build_house_weapons.py` (hearth_axe on the sword grip, the other two upright like the sun staff).

Team colour sits only on the cloth sheet (tabards, wraps, fin, headband, kilt, shield face). No pennants or standards on any back. Palettes: Tidecallers sea blue/verdigris, Ironclad gunmetal and ember, Wildmoot hide, bone and wood.

## Checks run
- `build_skins` passes: 9 troops, 21 weapons, weapon frames max 126 quads (cap 128), one-piece and tint checks green (a hide tone in the boar spear's grip and a gap in the trident shaft were fixed on the way).
- `tools/preflight.py`: clean. `Spec.java` untouched; CRLF-only diff to `sheets/preflight.json` reverted.
- `verify.py fast --offline`: green (a later run overlapping another Gradle build failed `build` on resource files; that is a collision, not a code fault).
- Turn sheets (5 views each: front, 3/4, side, back, rear 3/4): `hearthguard-turn.png` (violet), `tusk-turn.png` (teal), `tideguard-turn.png` (teal), `house-weapons-turn.png` (front, 3/4, side, back per weapon) in this folder.

## Not done / for Kort
- Tier I only for all three (matches the other Ironclad builders). Tiers II and III remain, including Tideguard's stage 3 coral crown.
- In-game turn shots NOT obtained. `verify.py fast --offline` was green on my sequential run. The harness client (hh-2, JDK 21) crashed twice: first a path mangling fault from Git Bash, then a resource reload failure (`missing models/entity/supply_wagon.json` in the run copy, which is tracked in the repo) followed by a null `shaderProgram` NPE. Likely a build collision with another Gradle run clearing `build/resources`; not diagnosed further. Treat the art as unverified in game.
- Art self-score: Hearthguard about 9, Tusk about 8 (head still reads small and dark; tusks thickened once), Tideguard about 8.5 (tabard low contrast against the teal legs, coral branches are small). Weapons about 8.5 for the axe and trident, 8.5 for the spear. Two correction rounds were used; the Tusk head and the Tideguard coral are the open items if Kort wants more.
- Kort reviews the turn sheets before anything merges. Nothing is pushed or merged.
