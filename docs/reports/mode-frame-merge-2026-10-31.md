# 2C.1 mode-frame merge branch, 2026-10-31

Branch `work/mode-frame-merge` from `main` bf920af. It carries the frame and the hidden Siege stub (cherry-picks of c5a1716, 2854933 and e290a03). The HANDOFF conflict was resolved by keeping main's text. The Task 4 world pieces (5f2aecc, d343eaf, 9498dcf) are not included; they wait for WORLD-01. Not merged into main. Kort reviews the branch first.

## Approvals recorded (decided on Kort's behalf under his delegation, as the mode-frame review describes them)
- The per-round world-save marker `<save>/gravesandglory/mode-marker.json`, plus `mode-saved/*.nbt` for Siege under the test property.
- The `battlemode:` lobby command and the `battleMode` block in the camp snapshot string. The binary STATE layout is unchanged.
- The sheet change from rules to modes (`rules` loses winScore, scoreInterval and roundSeconds; new `modes` sheet).
None of these changes a network packet, the player profile save format, or a custom entity type.

## Review blockers fixed
1. Phase 1 suites run on this tree (see the evidence below).
2. `social:start` copies `room.mode` into `battleMode`. The room's mode list is `Modes.ids()`.
3. `startRound` calls `mode.teardown` first and no longer clears `saves`. Siege keeps an unrestored gate snapshot instead of saving over it.
4. `teardown` is final and returns whether every saved piece went back. finish/returnLobby and switchMap delete the marker only when it returns true. An unrestored piece stays in `saves` and in the next marker.
5. New `BattleMode` hooks: `capturesPoints` (Siege false, so captureTick skips), `anchors(b,team)` (held points only while points play), `overtime(b)` (the timeout waits), and `recover(b,marker)` (dispatched by `Modes.recover` on the marker's mode).

New checks: p4 `mode-dead-points`, `mode-teardown-kept`, `mode-room`, `mode-swap`; LobbyRulesCheck accepts a registered mode.

## Still open (backlog)
- The marker stores absolute file paths (review bug 5).
- The HUD track max is hard-coded (bug 6).
- No bot-plan hook and no morale-off hook yet; Convoy needs both.
- The Convoy spec's line that ConvoyMode "needs no new hook" is wrong. It needs `capturesPoints`, `anchors`, `overtime` and `recover` (now present), plus bot and morale hooks. Its road kinds also need a preflight change.
- Fixed 2026-10-31 (Kort approved): returnLobby restores the battleMode a room round replaced; check p4-mode-room-clears.

## Evidence (run folders under the session scratchpad, modemerge/p1 and modemerge/p4)
- Phase 1 suites combat, morale, reinforce, bots, tutorial, report and ui-lobby: every `p1-*-suite=true`, with no false ids. The snapshot "ran to" lines come only from p1-report-snapshot's deliberate worst case, which passes.
- p4 `modes`: 10 ids true, including the four new ones, and `p4-modes-suite=true`.
