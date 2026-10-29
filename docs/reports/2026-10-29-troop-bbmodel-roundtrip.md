# Troop .bbmodel round-trip - 2026-10-29

Status: complete on `art/troop-bbmodel`, based on `main` at `8feae99`. This is the pipeline foundation for the Gilded redesign, not the redesign itself. No game run was needed: the gate is build output.

Until now only weapons could be opened in Blockbench. Troops were JSON boxes (`art/parts/*.json`, `art/troops/<troop>.json`) baked by `tools/build_skins.py`, although the v2 pipeline design (2026-10-21, section 2.2) intended a `.bbmodel` per troop. `tools/bbmodel.py` could read weapon projects only; `bbmodel.write` and `troop_bones` were still the TODO at the top of the module.

## What changed

- `tools/bbmodel.py`: `write` saves a project; `troop_bones` reads a troop project back against its rig and checks every `part:` / `team:` group against the part library. A project keeps the rig's y-down coordinates and puts everything under one `model` display group turned 180 about x, so Blockbench's y-up viewport shows the model upright without changing a cube, a rotation or a UV.
- `tools/build_skins.py`: `bake_bones` was split into `merge_bones` (rig + overrides + parts, before scale/hunch/ground) and the existing finalize step. `--to-bbmodel <troop>` writes `art/troops/<troop>.bbmodel`; from then on `bake` takes that troop's skin geometry from the bone groups, keeps bone scales in the JSON, and still builds part bones from the library. The exporter refuses to overwrite an existing project, because the JSON geometry is deleted after the export.
- `test-tools/standalone/test_bbmodel.py`: three tests for the reader (pivots/boxes, missing bone and wrong turn, part-library mismatch).

## Evidence

| Gate | Result |
|---|---|
| Six troops exported | `knight`, `archer`, `longbow`, `mage`, `sentinel`, `gl_marshal` |
| Byte-identical rebuild | SHA-256 of all 118 generated assets under `src/main/resources/assets/gravesandglory`, before the export and with all six projects loaded: no differences |
| `build_skins.py --verify` | 118 outputs verified, exit 0 |
| `test_bbmodel.py` | 18 tests, OK |

## Migration

`gl_marshal.json` and `mage.json` carried `pivot`/`boxes` overrides for `head`, `right_arm` and `left_arm`. Those values now live in their `.bbmodel` files; the JSON keeps only the bone scales (`gl_marshal`: body 1.15/1.05/1.1, arms 1.1) and `mage` has no `bones` block left. `sentinel` has two `tabard_skirt` team parts, so team groups are `team:<part>[:<name>]` when one library part is used twice.

## Limits

- **Mirrored cubes are rejected.** The biped's `arms_folded` pose has one mirrored box. All six current troops are `idleArms: hang`, so the export is complete today, but a folded troop cannot round-trip until mirror UVs are carried.
- **Parts stay library JSON** (design section 2.3.4). New reusable armour pieces go in `art/parts/`; the `.bbmodel` places them and the build checks their cubes against the library. Troop-specific new geometry *can* be added as extra cubes on a bone group in Blockbench; its `uv_offset` must be a free rectangle on the troop sheet.
- Part placement, rotation and UV still come from `art/troops/<troop>.json`. Editing a `part:` group's transform in Blockbench fails the build against the JSON rather than moving it; bone groups are authoritative.
- Blockbench itself was not launched, so "opens and edits in Blockbench" is checked against the Generic Model format and the project structure, not by a UI session.
- No compile, game run or visual review: no Java changed and the generated assets are byte-identical.

## Next

Author the Gilded troops in the exported projects (or start the from-scratch redesign by adding its bone-group cubes and new library parts), then run the normal art checks. `--to-bbmodel` is one-way; after a troop is converted, its project is the source.
