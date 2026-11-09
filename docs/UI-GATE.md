> Historical (superseded 2026-11-07): see PLAN-TO-1.0.md / WORKFLOW.md

# UI gate for Kort

Written 2026-10-23 by the no-game half of UI Task 8; scores updated the same day by the in-game leftovers run (client on, head 38ffe38 plus one harness commit). Every score below is graded against `work/ui-concepts/GRADING-RUBRIC.md`; nothing under 9 passes. "Reviewer" means a Task review graded it; "gate" means this run's side-by-side (screenshot beside its mockup, plus a greyscale copy) graded it.

Paths: `MOCK` = `C:\Users\kortt\Melty\bannerbound\work\ui-concepts\`. `SHOT` = `C:\Users\kortt\Melty\bannerbound\work\p1\ui8b2-fix1step1-052026\run\screenshots\` (the latest green Step 1 run: 81 pass, 0 fail). `CMP` = `C:\Users\kortt\Melty\bannerbound-ui\work\ui-gate\`: `cmp-<name>.png` has the shot on the left and the mockup on the right (1920 wide each), `cmp-<name>-gray.png` is the same in greyscale. Kort's bar is 9 of 10.

| Screen | Mockup | Latest screenshot | Score | Status |
|---|---|---|---|---|
| Group cards (`ui-hud-cards`) | `MOCK` `v4-hud-iron-ledger.png` | `SHOT` `p1-ui-hud-1080-gui2-battle.png`; `CMP` `cmp-hud-cards.png` | 9.5 (gate). Differs from the mock on purpose: Compact chips for the unselected groups, a one-line feed leaf, no full ledger cards (your lighter-HUD choice). | passes |
| Feed (`ui-feed-flood`) | same | `SHOT` `p1-ui-feed-flood.png`; `CMP` `cmp-feed-flood.png` | 9.5 (gate). Compact holds 3 lines; the mock shows 5. | passes |
| Hints (`ui-hud-hints`) | same | `SHOT` `p1-ui-hud-1080-gui2-hints.png`; `CMP` `cmp-hud-hints.png` | 9 (gate). The key slip sits 4 px under the chat: tight but clear. | passes |
| Title (`ui-title-day`) | none final: you rejected both painted titles | none | none | pending; Task 4 is held with the media shoot |
| War Table home (`ui-home`) | `MOCK` `v4-ui-war-table-home.png` | `SHOT` `p1-ui-home-1080.png`; `CMP` `cmp-home.png` | 9 (gate and Task 5 review) | passes. The mock shows two seated friends and a larger candle and ink well; the shot is the solo state with a "Last battle" tag. |
| Recruit (`ui-recruit`) | `MOCK` `v4-ui-war-table-recruit.png` | `SHOT` `p1-ui-recruit-1080.png`, `p1-ui-recruit-broke.png`; `CMP` `cmp-recruit.png` | 9 (gate); the iron plate on the broke shot 9.5 | passes. The shot has two House tabs (Dawn, Dusk); the mock shows five Houses. Unrecruited troops show a dark silhouette. |
| Banner page | none | `SHOT` `p1-ui-page-banner-1080.png` | 9.5 (gate) | passes |
| Contracts page | none | `SHOT` `p1-ui-page-contracts-1080.png`, `-claim.png` | 9 (gate; the slips are half empty under the bar) | passes |
| Chapter page | `MOCK` `track\track-01-sworn.png`, `-02-unsworn.png`, `-03-claim.png` | `SHOT` `p1-ui-page-chapter-1080.png`, `-claimable.png`, `-claimed.png`; `CMP` `cmp-chapter.png`, `cmp-chapter-claim.png`, `cmp-chapter-claimed.png` | 9.5 (gate). "No House takes oaths yet" replaces the mock's "Swear to a House" line, by design. | passes |
| Friends page | none | `SHOT` `p1-ui-page-friends-1080.png`, `-room.png`; in party: `p1-ui-page-friends-party.png` (960x540 only; "You lead the party" once) | 9 (gate); in party 9 (gate) | passes. No 1080 in-party shot exists. |
| Field page | none | `SHOT` `p1-ui-page-field-1080.png`, `-gates.png`, `-voted.png` | 9 (gate; empty parchment under the map text) | passes |
| Compact page (854x480) | none | `SHOT` `p1-ui-page-chapter-854.png`, `p1-ui-recruit-854.png`, `p1-ui-home-small.png` | 9 to 9.5 (8b-2 fix round 1; four shots at 9.0) | passes; a real 854x480 window was never opened |
| Big canvas (1920x974 and 1920x1080 GUI 1) | none | `SHOT` `p1-ui-home-1920x1080-gui1.png`, `p1-ui-home-1920x974.png` | 9.5 (8b-2 fix round 1) | passes; 1440p and 4K never opened (tiling seam, `docs/PARKED.md`) |
| Camp hint | none | `C:\Users\kortt\Melty\bannerbound\work\p1\ui8b2-fix1camp-052815\run\screenshots\camp-courtyard.png` | 9 (gate) | passes |
| Task 8c HUD frames, Compact | `MOCK` `v4-hud-iron-ledger.png` | `SHOT` `p1-ui-hud-1080-gui2-battle.png` 9.5, `p1-ui-hud-1080-gui4-battle.png` 9, `p1-ui-hud-small-battle.png` (854x480 GUI 2) 9 | as listed | passes |
| **1280x720 GUI 3 and 1366x768 GUI 2** | same | `SHOT` `p1-ui-hud-1280-battle.png`, `p1-ui-hud-1366-battle.png` | **8.5 and 8.5 (gate): FAIL.** The cards, score bar and feed leaf draw at 1x (text about 7 px) beside a 3x chat and hotbar (2x at 1366). Line 1 (legibility) and line 4 (hierarchy) lose half a point each. The open chat now clears the ability slot (fixed in 8b-2). | **your call**: this is the 2x scale rule you dropped. Nothing is broken; the HUD is just small at 720p. |

The battle report (plan 4F.2) is a War Table page: a note (result, the mode's own section, rewards) and a three-faced sheet (Battle, Troops, Chapter); ui-report shots it at 2560x1440 to 854x480. The pages are graded at 1080p. Since Task 8b-2 the 854x480 window gets the compact page (everything fits, no crop) and the oak runs to every edge; a window under 854x480 still crops (docs/PARKED.md).

In-game leftovers run, 2026-10-23: the 8c red runs R1 and R2 ran to the end (every expected check went red, then restored byte for byte), the ui-hud sweep no longer dies in a red run (the round ended mid-sweep; the harness now holds the round's clock and score), and the two-client `CampNetworkChecks` passed 37 of 37. One note: the red-run `chat-bars` break no longer turns `p1-ui-chat-open` red since 8b-2 (the ability-slot cap gives the same 6 lines).

## Questions still open for you

1. **What should "Large" mean at 720p and 854x480?** Today it draws the same as Standard there, and the button says so ("Large (Standard size here)"). Recommend: leave Large as "as big as fits" and revisit after real play. Large only becomes its own step at 2560x1440 and up.
2. **The Dawn and Dusk team inks are one notch darker** so they hold 5.2:1 on the slip. Recommend: keep it, and look at it in the client run; the 8c report has a one-line revert.
3. **The feed leaf's edge.** Its second text row partly sits on the leaf's mottled edge, contrast 4.26 to 4.37 on about a tenth of that row. Recommend: accept; if it bothers you in game, darken the ink one more step rather than repaint the leaf.
4. **Compact as the default.** You chose it; judge it against the old HUD in the client run. Standard at 1080p GUI 4 covers more than the old HUD did (10.9% against 7.8%); it is not the default. Recommend: leave it, trim 2 to 4 px later.
5. **Title screen.** You pick a still once units and maps are final. Until then the gate runs without it.

Answered already, no action: chat above the cards, the Chapter Roll shows what Phase 1 grants, everything must fit at 854x480.
