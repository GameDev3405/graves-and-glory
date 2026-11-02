# Map Engine Plan 3: the AI handoff

**Goal:** Kort pins notes on areas and writes one brief; `export` packs the design, top-down renders, the notes and the brief into a folder a Claude session can read; the session writes `draft.json` back; `import` brings it in as proposed objects tagged `by: ai`, which the editor shows with a tracked-changes tint and Kort accepts, rejects or edits one by one. Kort's objects are locked: a draft that changes or deletes one has that change refused with a message. A draft that fails the schema is refused whole.

**Spec:** `docs/superpowers/specs/2026-11-01-map-engine-design.md` (AI handoff, reviewing the draft, locking, AI draft rules). **Base:** `main` `6de0b53`, branch `work/map-engine-plan-3`.

## Boundaries

- Python and the browser app only, plus one small opt-in Java fix (Task 5). No gameplay, packet, save or `Spec.java` change.
- Proposals still draw in the preview heightfield (as in slice 1), but `build` refuses while any proposal is unreviewed: the builder only builds what Kort approved.
- The AI may propose any existing kind (hill, river, road, point, spawn, zone). Buildings and props wait for Plan 4's building library.

## Locking rule

An object is **locked** when `by` is `kort` or `state` is `accepted` (Kort placed it, edited it or accepted it). In the editor, any edit to an AI object other than Accept makes it `by: kort, accepted`. Import applies, per object:

| Draft object | Result |
| --- | --- |
| new id | added as `by: ai, state: proposed`, whatever the draft says |
| same id as a proposal | replaced, still a proposal |
| same id as a locked object, unchanged | kept |
| same id as a locked object, changed | refused: `<id>: locked (Kort's), the draft's change was not applied` |
| locked object missing from the draft | kept, refused: `<id>: locked (Kort's), the draft may not delete it` |
| proposal missing from the draft | withdrawn (removed) |
| top-level `name`, `width`, `depth`, `baseY`, `brief` changed | refused, Kort's value kept |
| any object that makes the merged design invalid (e.g. a clashing point name) | dropped, refused with the schema message |

The whole draft is refused (nothing changes) when it is not valid JSON, fails `design.validate`, or has another `id`. Refusals are stored in the design's `review` list and shown in the review panel until the next import.

## Task 1: schema for notes, brief and review

- `design.KINDS` gains `note`: `x`, `z` inside the canvas, `radius` 4 to 200, `text` 1 to 2000 characters.
- Optional top-level `brief` (string, up to 8000) and `review` (list of strings).
- `locked(o)`; `build_problems` adds `N AI proposals not reviewed: accept or reject them before building`.
- Tests: `test_mapengine_plan3.py` `test_note_schema`, `test_build_refuses_proposals`.

## Task 2: export bundle

- `mapengine/handoff.py` `export(d, out_dir)` writes `design.json`, `BRIEF.md` (the brief, every note with its marker number and coordinates, the locking rule, the schema of each kind, where to write `draft.json`), `top.png` (relief at 2 px a block with water, roads, points, spawns and numbered note markers; proposals in magenta) and `notes/<id>.png` (a 4 px a block crop around each note). Deterministic PNGs from stdlib `zlib`.
- `python tools/mapengine export <id>` writes to `work/mapengine/<id>/handoff/`.
- Tests: `test_export_bundle`, `test_export_deterministic`.

## Task 3: import with locking

- `handoff.merge(current, draft) -> (merged, report)` per the table; `DraftError` for a whole-draft refusal.
- `python tools/mapengine import <id> [draft.json]` (default `work/mapengine/<id>/handoff/draft.json`); server `POST /api/design/<id>/export` and `POST /api/design/<id>/import` (body `{"draft": {...}}`).
- Tests: `test_import_tags_ai`, `test_locked_change_refused`, `test_locked_delete_refused`, `test_proposal_replaced_and_withdrawn`, `test_bad_schema_refused_whole`, `test_clash_dropped`, `test_top_level_kept`, `test_server_import_export`.

## Task 4: review in the editor

- Map view: proposals drawn in a magenta tracked-changes tint (dashed, translucent fill).
- Panel: a Review list (Accept, Reject per proposal, Accept all, Reject all, and the refusals); a selected proposal shows Accept and Reject; editing it makes it Kort's. A Note tool (key 7) and a Brief box when nothing is selected. Top bar: Export for AI, Import draft (file picker).
- Browser check on a scratch copy of `maps/sample`.

## Task 5: camp edge step

- Plan 2 repeats the north edge column of the canvas across the 16-row margin (z -251 to -266), so the engine map can meet the camp rim with a step. `EngineMaps.step` now lifts or lowers each north-margin column by `CampWorld.taper` of the edge height, as the hand-made maps do, and drops water in a lifted column so it cannot spill.
