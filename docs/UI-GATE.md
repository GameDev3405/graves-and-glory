# UI gate for Kort

Written 2026-10-23 by the no-game half of UI Task 8. No Minecraft ran. Every score below comes from a reviewer's written report; nothing is marked graded unless a reviewer graded it. The in-game regression and the final screenshots wait for a client ("pending client check").

Paths: `MOCK` = `C:\Users\kortt\Melty\bannerbound\work\ui-concepts\`. `SHOT` = `C:\Users\kortt\Melty\bannerbound\work\p1\`; a shot lives in `<run>\run\screenshots\`. Kort's bar is 9 of 10.

| Screen | Mockup | Latest screenshot | Score so far | Status |
|---|---|---|---|---|
| Group cards (`ui-hud-cards`) | `MOCK` `v4-hud-iron-ledger.png` | `SHOT` `ui-8c-green-233227\...\p1-ui-hud-cards.png` (Compact chips, run stopped early) | Implementer 10, provisional. The reviewer graded only the older 2x-ledger HUD (9.0 to 10), which Compact replaced. | pending client check |
| Feed (`ui-feed-flood`) | same | `SHOT` `ui-8a-final-201524\...\p1-ui-feed-flood.png` (before Compact) | Feed kinds 10 and 10 (Task 8a review, old size) | pending client check |
| Hints (`ui-hud-hints`) | same | `SHOT` `ui-8a-final-201524\...\p1-ui-hud-hints.png`; Compact: `ui-8c-green-233227\...\p1-ui-hud-720-gui2-hints.png` | Old size 9.5 to 10 (Task 8a review) | pending client check |
| Title (`ui-title-day`) | none final: you rejected both painted titles; the old try is `MOCK` `title\title-hill-day.png` | none | none | pending; Task 4 is held with the media shoot |
| War Table home (`ui-home`) | `MOCK` `v4-ui-war-table-home.png` | `SHOT` `ui-fix1-green-172533\...\p1-ui-home-1080.png` | 9 (Task 5 review); the five map cards 9.0 to 9.5 (Task 7 review) | graded by a reviewer (the party-seat name face changed in 8b-1 and was not shot) |
| Recruit (`ui-recruit`) | `MOCK` `v4-ui-war-table-recruit.png` | `SHOT` `ui-fix1-green-172533\...\p1-ui-recruit-1080.png` | 9.5 (Task 6 review) | graded by a reviewer (iron plate highlight change not shot) |
| Banner page | none; built from the plan's layout | `...\p1-ui-page-banner-1080.png` in the same run | 9.5, then 10 (re-review) | graded by a reviewer |
| Contracts page | none | `...\p1-ui-page-contracts-1080.png`, `-claim.png` | 9.0 (Task 7 review) | graded by a reviewer |
| Chapter page | `MOCK` `track\track-01-sworn.png`, `-02-unsworn.png`, `-03-claim.png` | `...\p1-ui-page-chapter-1080.png`, `-claimable.png`, `-claimed.png` | 9.5 (Task 7 review) | graded by a reviewer |
| Friends page | none | `...\p1-ui-page-friends-1080.png`, `-room.png` | 8.5, then 9.5 (re-review). The in-party state was never shot. | graded by a reviewer, in-party state pending client check |
| Field page | none | `...\p1-ui-page-field-1080.png`, `-gates.png`, `-voted.png` | 9.5 (Task 7 review) | graded by a reviewer |
| Task 8c frames, five sizes: 1280x720 GUI 3, 1366x768 GUI 2, 1920x1080 GUI 4, 1920x1080 GUI 2, 854x480 GUI 2 | `MOCK` `v4-hud-iron-ledger.png` | Compact only, from the stopped run: `ui-8c-green-233227\...\p1-ui-hud-1280.png`, `-1366.png`, `-1080-gui4.png`, `-1080-gui2.png`, `-small.png`. Partial sheet: `C:\Users\kortt\Melty\bannerbound-ui\work\p1\ui-8c-before-after-partial.png` | Implementer, provisional: 8.5 to 10 (the two 1280x720 shots at 8.5 predate a chat fix). No reviewer grade. | pending client check, all five; Standard and Large frames do not exist yet |

The battle report page keeps its old layout; its War Table look is parked. The pages are graded at 1080p. Since Task 8b-2 the 854x480 window gets the compact page (everything fits, no crop) and the oak runs to every edge; a window under 854x480 still crops (docs/PARKED.md).

## Questions still open for you

1. **What should "Large" mean at 720p and 854x480?** Today it draws the same as Standard there, and the button says so ("Large (Standard size here)"). Recommend: leave Large as "as big as fits" and revisit after real play. Large only becomes its own step at 2560x1440 and up.
2. **The Dawn and Dusk team inks are one notch darker** so they hold 5.2:1 on the slip. Recommend: keep it, and look at it in the client run; the 8c report has a one-line revert.
3. **The feed leaf's edge.** Its second text row partly sits on the leaf's mottled edge, contrast 4.26 to 4.37 on about a tenth of that row. Recommend: accept; if it bothers you in game, darken the ink one more step rather than repaint the leaf.
4. **Compact as the default.** You chose it; judge it against the old HUD in the client run. Standard at 1080p GUI 4 covers more than the old HUD did (10.9% against 7.8%); it is not the default. Recommend: leave it, trim 2 to 4 px later.
5. **Title screen.** You pick a still once units and maps are final. Until then the gate runs without it.

Answered already, no action: chat above the cards, the Chapter Roll shows what Phase 1 grants, everything must fit at 854x480.
