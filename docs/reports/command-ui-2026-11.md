# Command UI build report (2026-11-05)

Branch `feat/command-ui`, worktree `C:/Users/kortt/Melty/bannerbound-cmdui`. Not pushed and not merged to main; the merge is Kort's decision.

## Outcome

Every plan task in `docs/superpowers/plans/2026-11-04-command-ui.md` (Tasks 1–13, plus 3b) is built and committed, and each passed a task review. The final whole-branch review found one Important issue (I1) and four Minor ones. One fix wave addressed I1, M3 and M4, and the scoped re-review approved it with no new Critical or Important findings. M1 and M2 are recorded below. Main (`1d3f2edd`, which contains `aaabecd7`) is merged, and main has not moved since.

- **Gates:** fast, game and release pass (exit 0). The command UI harness passes 98/0.
- **HoldLine:** 14/16. `hl-arc` is a known failure that predates this branch. `hl-corpse-load` is a noisy frame-time check: it failed in this run and passed in Task 13's game tier (see Open defects).

WIP commit `84178ab7` (Task 5 fix round 1) was reviewed. It was kept, squashed with its follow-up fix and reworded as `ca1a4abe`.

## Commits (`main..feat/command-ui`, first parent)

| Commit | What |
|---|---|
| 82f31428, 1dcf979b, f083739a | Design draft, docs merge, approved design and plan |
| c5b7f84d | Command UI sheet numbers |
| 670b5b6b, 0f82a253 | Harness runner, smoke suite, harness fixes |
| c6a67f16 | Pennant, pill, tray and order icon art |
| 6fa99cfa | Server order intake (hunt, directed charge, retreat to the commander, outlines) |
| 71a8b539, e3016891 | Keys Z X C V B, persistent selection, R screen retired; first-launch toolbar unbind |
| 5bc15b6a | Multi-group order packet; per-commander Charge cooldown |
| 0b0a696b | Attack hunts, Charge runs a direction, Retreat returns to the commander |
| ca1a4abe | Formation holds its slots; Retreat keeps its pace with shields up (contains reworded WIP 84178ab7) |
| 542be975, 96825343 | Shield wall, Wedge and Loose rules; charge first-hit bonus; fix round |
| 9c69866d, d4218542 | Left-edge pennant panel, pills, tray, cooldown sweep; fix round |
| 41e3287f | Merge main (1d3f2edd) |
| 36c670c9, 73e9d39b | Outlines, team ghosts, horn, shout, raised weapons; fix round |
| 2eb62a55 | Overhead charge camera and accessibility toggle |
| 6cb8b1fb, f9f970ff | Bots respect the charge cooldown, lessons; restored the progressEvent call |
| 3926576c, 9758ff01 | Custom numbered groups (Profile schema 5); fix round |
| 86f7244a | End-to-end suite, docs (HANDOFF, HOW-TO-EDIT), verification |
| 6a029982 | Final fix wave: the legacy order_v4 charge is held to the Charge cooldown and gets chargeDir and thenFollow; troop keys cached |
| (this report) | docs/reports/command-ui-2026-11.md |

## Checks at 6a029982 (2026-11-05, JDK 17)

| Check | Command | Exit | Result |
|---|---|---|---|
| Fast gate | `python tools/verify.py fast --offline` | 0 | 60 PASS, 0 FAIL (`work/final-checks/fast.log`) |
| Command UI harness, all 11 suites | `GRAVES_COMMAND_UI_CHECKS=Smoke,OrderIntake,Keys,OrderBehaviour,FormationRules,Panel,Feedback,ChargeCamera,BotOrders,CustomGroups,EndToEnd ... runClient -PlocalIntegration -I test-tools/isolated-checks.gradle` | 0 | 98 PASS, 0 FAIL (`work/final-checks/cmdui/run/command-ui/results.txt`) |
| Game tier | `python tools/verify.py game --offline` | 0 | 61 PASS (`work/verify/20261105-073540-51008`) |
| Release tier, first run | `python tools/verify.py release --offline` | 1 | Package failed: the git-ignored `dependencies/resourcepacks/Excalibur_V1.20.zip` was missing from this worktree. An environment gap, not a code failure (`work/verify/20261105-074619-27176`) |
| Release tier, rerun | same | 0 | 62 PASS, including package and package-readback (`work/verify/20261105-081348-33336`) |
| HoldLine | `GRAVES_PHASE2_CHECKS=holdline ... runClient` | 0 | 14/16 true. `hl-arc` false (known). `hl-corpse-load` false: client frame B-A median 0.972 ms against a 0.75 ms limit, spread 1.215; server tick B/A 1.089, which passes. `hl-wall`, `hl-wall-loss`, `hl-wall-loss2` and `hl-wall-knock` pass (`work/final-checks/holdline.log`) |

To let the release tier package, the git-ignored `dependencies/mods`, `shaders`, `sources` and `resourcepacks` were copied in from sibling checkouts. They are not committed.

Every harness run used one client at a time. No other java process was touched.

## Screenshots

`work/cmdui-shots/` (not committed; under work/):

- `panel-closed-1920.png` and `panel-closed-854.png`: rest state.
- `panel-hover-1920.png` and `panel-hover-854.png`: hover.
- `panel-open-1920.png` and `panel-open-854.png`: panel open.
- `panel-tray-1920.png` and `panel-tray-854.png`: selected (Formation pennant active, tray open, Wall picked).
- `panel-cooldown-1920.png` and `panel-cooldown-854.png`: disabled (Charge cooldown).
- `panel-pills-1920.png`, `outline-aim.png`, `rally-pose.png`, `charge-camera.png` and `custom-groups.png`.

## Status, kept apart

| Area | Implemented | Tested in game (harness) | Visually checked |
|---|---|---|---|
| Order intake, keys, multi-group packet, cooldown | yes | yes (Smoke, OrderIntake, Keys) | n/a |
| Attack, Charge, Follow me, Retreat, Formation behaviour | yes | yes (OrderBehaviour, EndToEnd) | no |
| Shield wall, Wedge, Loose rules; charge bonus | yes | yes (FormationRules, HoldLine) | no |
| Pennant panel, pills, tray, cooldown sweep | yes | yes (Panel) | yes, by the controller, at 1920 and 854 against `panel-mockup-v2.md`. Kort has not reviewed it |
| Outlines, ghosts, horn, shout, rally pose | yes | yes (Feedback; two clients) | outlines yes (screenshots). The rally pose was tuned without looking at it, and Kort has not reviewed it |
| Charge camera and toggle | yes | yes (ChargeCamera) | screenshot only |
| Bot cooldown and lessons | yes | yes (BotOrders) | no |
| Custom groups (Armoury editor, schema 5) | yes | yes (CustomGroups; migration checks in fast) | Armoury editor screenshot only |

### Controller visual check against `panel-mockup-v2.md`

The layout matches: pole and pills on the left edge, five swallowtail pennants in the right order with icons and keys, the active pennant longer with gold ink, and the 4-cell tray beside Formation with the picked cell in gold. The cooldown greys Charge and shows the seconds. The cloth is the House colour (gold here) rather than the mockup's blue example.

Gaps:

- **Hover is weak.** At 1920 it is subtle. At 854 it is barely distinguishable from open.
- **Pills.** They are slate rather than glass. At 854 the "All" pill wraps to a second row.
- **Tray at 854.** It wraps to 2×2, and its bottom row reaches the vanilla armour row ("Wedge Loose" touches it). The tray also enters the centre third at that size; the mockup's 40% bound already allows this.
- **Labels at 1920.** "Wedge" and "Loose" sit about 3 px apart.
- **Cooldown at 854.** The pennant shows 17s while the hint toast still says 19s. The toast is a snapshot from when it was issued.

## Open defects and parked items

**Final review, not fixed:**

- **M1.** The Charge start for a spec group is the client's centre with only an arena bound, so a modified client can start a Charge far from its troops. Fix: clamp it to the server-computed centre.
- **M2.** The EndToEnd X-hold re-asserts the key every tick, which would hide a future unpress regression. Not player-facing.
- **Recommendation for Kort (protocol):** retire bare order ids on legacy `order_v4`.

**Test health:**

- `hl-corpse-load` is noisy. It failed here and in the merge-1 runs, and passed in Task 13's game tier. It needs a rerun on a quiet machine before anyone calls it a regression.
- `hl-arc` is a known failure.

**Parked from task reviews:**

- **Feedback:**
  - Replace-by-owner uses a 150 ms heuristic.
  - Formation outlines are static squares.
  - ACK attribution is a guess.
- **Charge camera:**
  - The toggle key ships unbound, with no on/off display.
  - A server-set rotation during a hold is reverted.
  - Camera reset on death is untested.
- **Charge and groups:**
  - chargedAt is still per spec group.
  - In a split custom group, the HUD and anchor follow the newest order.
  - Teammate troops map to your own group.
- **Custom groups:**
  - stateKey has an allocation and cubic-cost note while unlatched.
  - There is no rate limit on group saves.
  - GroupTag.refresh runs in paint.
  - The New group tag height is off.
- **Task 6 minors:**
  - M1: HitLog omits the block and crash factors.
  - M2: hardcoded "block".
  - M3: no square hysteresis.
  - M4: wedge axis estimate.
  - M5: test isolation.
  - N1–N3.
- **Panel visuals:** hover strength, pill opacity and the Wedge/Loose gap, as listed above.

## Next

Kort reviews the branch tip `6a029982` plus this report's commit, and the visuals (panel, rally pose) in game. Then he decides on the merge to main.
