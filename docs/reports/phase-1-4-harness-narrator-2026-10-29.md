# Phase 1.4 harness Narrator parity - 2026-10-29

Status: implemented and checked. Internal harness work; no visual gate and no gameplay change.

## Outcome

A fresh test root has no `options.txt`, so Minecraft opens the accessibility onboarding ("Narrator") screen on first launch. The Phase 2 checks waited on the front menu behind that screen and the run stalled. Phase 3 already dismissed it; Phase 2 did not, and two render lanes carried hand-written launchers without the `onboardAccessibility:false` seed.

Fixed in both places:

1. `test-tools/java/gg/gravesandglory/Phase2Checks.java` dismisses the screen on the client tick before the campaign auto-start:
   `if(c.currentScreen instanceof AccessibilityOnboardingScreen s){s.close();return;}`
   This is the same pattern Phase 3 uses, so a root with no options file no longer depends on a seeded setting.
2. `tools/verify.py` (game tier) and `tools/launch_playtest_clients.py` seed `onboardAccessibility:false` in each run root. `main` already had this; it was missing from the animation and House-model checkouts and has been carried to both.

## Checkouts carrying the change

| Checkout | Branch | Head | State at report time |
|---|---|---|---|
| `bannerbound` | `main` | `1f6c799` | Phase 2 dismissal committed |
| `bannerbound-wt-rebase` | `work/war-table-rebase` | `85f095e` | the checked run used an uncommitted working-tree copy, then the tree was restored clean. The identical line is committed on `main` |
| `bannerbound-anim` | `work/anim-pass-1` | `9fe0fcf` | dismissal and launcher seed uncommitted on the WIP branch |
| `bannerbound-models` | `work/troop-models-house` | `b9884ff` | dismissal and launcher seed uncommitted on the WIP branch |

The two WIP lanes keep the edit uncommitted on purpose: their next step is a rebase onto `main`, which brings the committed fix with it. Do not merge those dirty trees as-is.

## Check

Run in `bannerbound-wt-rebase` at `85f095e` (`work/war-table-rebase`) with the Phase 2 screens suite, from a **new** run root:

- Run root: `work/phase2/narrator-20261029-000951/run`. No `options.txt` existed before launch; the game wrote its own at 00:10:25, after the 00:09:51 start.
- Result: `p2-screens-suite=true all checks passed`, 22 `[p2] ...=true` lines, `BUILD SUCCESSFUL in 2m 21s`, `EXIT=0`.
- Covered in that run: screens navigation and restore, roll card, troop card, recruit outcomes, veteran skin and the campaign screens.
- Log: `work/phase2/narrator-20261029-000951/gradle.log`.

The suite therefore completed without any pre-seeded options file, which is the condition that used to stall.

## Not covered

- `main`'s own tree has not had a separate fresh-root run; the added line is identical to the one exercised on the rebased tree.
- No visual review is required for harness code. Not accepted as art or gameplay evidence.

## Assumptions

- The Phase 2 dismissal is safe to run on every client tick: it only fires while that screen is the current screen and closes it immediately, matching Phase 3.
- Seeding `onboardAccessibility:false` is still worth keeping in launchers for profiles that already exist, so the two fixes are complementary rather than duplicates.
