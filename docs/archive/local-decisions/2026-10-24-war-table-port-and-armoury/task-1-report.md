# Task 1 report (lane P1): claims with a House, the Rare pick, chapter labels

**Status: DONE, committed `5dcf3da` on `ui-port-claims` (worktree `C:\Users\kortt\Melty\bannerbound-ui-p1`). Nothing pushed.**
Needs Kort's eyes: the three screenshots below (look, not behaviour). Needs an in-game check by a person: none beyond that.

## What changed

- `WarTablePages.java`: a row of five House chips (seal + name, 24 tall, 3 px apart) on Contracts ("Shards for", centred under the reset line) and on Chapter (under "The Founders"). The chip pressed in is where the shards of the next claim go: the banner's House (`RosterMath.rollHouse`, worked out again only when the banner changes) until another is pressed. The row shows only while a claim that pays shards is still to come (the daily that completes all three, the weekly, a shard tier, tier 20).
  - Contracts sends `progress:daily:0` and `:1` bare; `progress:daily:2:<house>` for the daily that completes all three; `progress:weekly:<house>`. Reward lines come from `Progression.claim` run on a copy of the state: "30 gold and 75 XP", a second line "+ 3 Ironclad shards" on that daily, "150 gold, 300 XP and 10 Ironclad shards" on the weekly (the House left out once it is claimed).
  - Chapter sends `progress:founders:<t>:<house>` on `SHARD_TIERS`, `progress:founders:20:<troop>` at tier 20 (a Rare chip cycling `RecruitPages.rares(house)`, with "Rare of <House>, n of m"), the bare key for gold, title and cosmetic tiers. "Tier N gives <Progression.reward with the House named>" sits left of the Claim plate.
  - Roll plaques take their picture and two lines from `Progression.reward` (Gold 30, Shards 10, Rare troop, Pennant pattern, Banner frame, "Pathfinder title" as before) in place of "Gold 25".
  - An inactive Claim carries `Progression.refusal` as its tooltip and "Locked" or "Claimed" on its plate.
  - The compact Contracts notice moved to y 436 (the row takes 396 to 420 there). Widgets are added chips first, so Tab meets the House, then the Rare, then Claim.
- `tools/ui_art/wt_claims.py` (one import line in `build_ui_art.py`): `table/chip-house`, `table/chip-house-on` (pressed face, inked edge, no shadow, light rule 2 px under it), and the Roll icons `chapter/ic-shards`, `ic-pennant`, `ic-frame` (the Oath row's own painters). Atlas regenerated, 265 textures.
- Checks: new `ClaimChecks` (nested in `Phase2Checks`, so the jar leak guard already covers it; run from `Screens` case 4) and the rewritten `p1-ui-page-contracts` / `-chapter`.

## Commands and results (all in the worktree)

| Command | Result |
|---|---|
| `bash work/p1/compile.sh red1` (checks written, page code still old) | exit 0 |
| Red run 1: Phase 2 `screens` (`work/p2/r-red1-233340`) | fails: `p2-roll-card`, `p2-notice-clears` (Task 4's, as the matrix says), the seven new `p2-claim-*` ids (daily:2 refused: "Choose a House", shards 0 to 0), `p2-screens-suite` |
| Red run 1: Phase 1 `ui-pages` (`work/p1/r-red1-233627`) | fails exactly `p1-ui-page-banner`, `p1-ui-page-contracts`, `p1-ui-page-chapter`, `p1-ui-pages-suite` (the matrix's four) |
| Implement, `bash work/p1/compile.sh` | exit 0 |
| Green: Phase 2 `screens` (`work/p2/r-g4-235955`, final tree) | `pass p2-claim-default, -house, -weekly, -founders-shards, -founders-rare, -sends, -fits`; still red: `p2-roll-card`, `p2-notice-clears` (Task 4) and the roll-up `p2-screens-suite` (needs Task 4) |
| Green: Phase 1 `ui-pages` (`work/p1/r-g2-235036`) | `p1-ui-page-contracts` and `-chapter` pass, `-scales` pass; `p1-ui-page-banner` and `-pages-suite` red until Task 3 |
| `gradle build --offline -x test` | exit 0 |
| `python tools/build_ui_art.py --verify` | `265 textures verified` |
| `python test-tools/standalone/test_ui_standard.py` | `clean` |
| `bash work/p1/standalone.sh` | every javac check passes (0 FAIL lines); unittests pass (`test_release_integrity` failed once on the new class `ClaimChecks`; fixed by nesting it in `Phase2Checks`, then OK); `build_skins --verify` 118 outputs |
| `GRAPHIFY_BACKEND=none graphify update .` | rebuilt (7385 nodes) |

`p2-claim-house` reads the server back: daily 0 and 1 sent with no House, `progress:daily:2:ironclad` stored `daily:0..2`, Ironclad shards 0 to 3 (`DAILY_SHARDS`), no other House changed, gold +100 equal to `Progression.claim`'s own grants. `-weekly`: Tidecallers +10 and the row gone once nothing pays shards. `-founders-shards`: tier 2 with Wildmoot +10. `-founders-rare`: the chip offers exactly the Gilded Rares in turn (each a Rare of the House), the second is claimed and owned, the other is not. `-sends`: the six strings in order. `-fits`: at 960x540 and 854x480 on both pages, chips 24 px or more each way, 12 px inside the canvas, nothing over anything, none under the rail, each reward line inside its note, Tab order chips, Rare, Claim.

## Screenshots (copies in `.superpowers/sdd/2026-10-24-war-table-port-and-armoury/shots-p1/`, grey copies `_grey`)

- `wt-contracts-claim.png` (960x540) and `wt-contracts-claim-compact.png` (854x480): Ironclad pressed, the third daily with "+ 3 Ironclad shards", the weekly with "10 Ironclad shards".
- `wt-chapter-pick.png` / `-compact.png`: tier 2 waiting, Wildmoot pressed, "Tier 2 gives 10 Wildmoot shards".
- `wt-chapter-rare.png` / `-compact.png`: tier 20, Gilded, the Rare chip "Order Longbow >" and "Rare of Gilded Order, 2 of 2".
- Mockups they were built from: `mock-wt-*.png` in the same folder.
- Run copies: `work/p2/r-g4-235955/run/screenshots/p2-wt-*.png`.

Grades against `work/ui-concepts/GRADING-RUBRIC.md` (own reading, real screenshot beside the mock and the grey copy): Contracts 9.5, Chapter pick 9, Chapter Rare 9. Points lost: Chapter has no "Shards for" caption (no room left of the row, the "Tier N gives" line beside Claim carries it); the House named in the chip and again in the reward line is a deliberate double cue (rubric line 2).

## Deviations from the plan (for Kort or the controller)

1. The chosen chip carries no "for" word. A word on the chip does not fit the 417 to 424 px row. It is told by pressed art, a tick on the seal, a light rule under it, the caption "Shards for" left of the row (Contracts) and the House named in the reward line, so never by colour alone.
2. The House suffix on a daily goes on the daily that **completes all three** (server rule `last`), not literally slot 2: whichever daily is claimed last carries it; `RecruitPages.contractShards` is the one source. The checks claim 0, 1, 2 in order.
3. The Rare chip reuses `recruit/wax-star` on the Roll plaque; three new Roll icons were painted (see above).
4. `CampScreen`'s old `claimHouse`/`cycleRare` and `RecruitPages.HouseChip` are untouched for Task 9 to retire.
5. A new test class would have tripped `test_release_integrity` (the jar leak guard in `tools/release_config.py` lists harness class names), so `ClaimChecks` is nested in `Phase2Checks` instead; no edit to `release_config.py`.
6. The old Screens shots `screens-contracts` and `screens-chapter` (nothing reads them) are replaced by the `wt-*` ones.

## Frozen or foreign files

None edited. `sheets/preflight.json` and `Spec.java` show CRLF-only diffs after `tools/preflight.py`; not committed.
