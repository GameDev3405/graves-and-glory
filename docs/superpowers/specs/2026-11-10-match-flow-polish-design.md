# Match flow polish: design

Date: 2026-11-10. Author: polish lead. Status: approved in outline by Kort on 2026-11-10 (audit items F1 to F8, `docs/reports/polish-audit-2026-11-10.md`); his answers are marked "Kort". Evidence for every "today" statement is in the audit.

Timing: the room page (section 3) is screen work and can be built before the port. Sections 2, 4 and 5 add match states and packets and are built after the port, together with the army economy spec's round flow (`2026-11-09-army-economy-design.md`), because both add states between "fighting" and "lobby".

## 1. Goal

Setting up a match happens in one place. A match ends with a moment worth winning. Another match with the same people is one press away. A lost match can be ended, and leaving does not break it.

## 2. Match end (F1)

Today `Campaign.finish` pays rewards, plays the sting and teleports everyone to camp in the same tick.

New states after the winning score, round limit or surrender:

1. **Ended** (8 s, Kort). Damage stops. Troops stop attacking and stand; the winning side's troops raise weapons (the rally pose) and the losing side's lower theirs. The losing side's banner at its base falls. The victory or defeat sting plays. The HUD score strip grows to the centre with the final score and, under it, the player's own three numbers: kills, troops lost, points taken. Commanders can move and look but not strike or order.
2. **Best commander** (4 s, Kort). The camera of every player cuts to a slow orbit around the match's best commander, with their name, House and the number that won it. Best is the highest of: (commander kills + troop kills) + 5 per point taken, across both sides; ties go to the winning side, then to the earliest seat. A bot can be best.
3. **Results.** Everyone returns to camp and the results page opens.

Rewards are computed at the start of Ended exactly as today (one save per player); only the return is delayed. A player who disconnects during these 12 s keeps their rewards.

Under best of five: a round that is not the last ends with Ended shortened to 4 s and no best commander, then the economy spec's pick phase. The last round gets the full sequence, and "best" is counted over the whole match.

Results page changes:

- Rewards list includes Forge marks earned (missing today) beside gold and XP.
- Buttons: **Play again**, **War council**, **Back to camp**. Contracts and Chapter stay as links under the rewards.
- Play again (Kort): returns the player to the same room with the same mode, map, seats, Houses and bots. Each player's ready flag is cleared; when every human has pressed Ready the normal countdown starts. A player who chooses Back to camp leaves their seat, and the seat becomes a bot or empty by the room's bot setting.
- "Return to room" (today opens the Friends card) becomes War council and opens the room page.

Sizes in `sheets/design.json`: `endedSeconds` 8, `roundEndedSeconds` 4, `bestCommanderSeconds` 4, `bestPointWeight` 5.

Owners: polish lead builds the states and results page; audio lead confirms the stings sit right over 8 s; animation lead confirms the rally pose is fit to be shown to every player (the UI lead notes Kort has not reviewed it); UI lead reviews the score card and results page.

## 3. One room page (F2, F3)

Today there are two room screens with different powers, an old lectern lobby, and quick-match pads.

- The war council page is the only room screen. It gains: the room code with a copy button, a "Join by code" field when not in a room, an Invite button (the friends list as a pop-over), and **Start now** for the host when every human is ready.
- The Friends page keeps the friends list, presence, and Join on a friend's row, which opens the war council.
- The lectern `LobbyScreen` / `CampaignLobbyScreen` path is deleted. The quick-match pads are removed from the camp (Kort: remove both). The camp builder's map file is not edited by this work: the pad blocks stay as decoration until the camp's owner removes them; only their trigger is unhooked. (Flag for the lobby/map owner.)
- Each mode tab shows one line of rules and a usual length ("Hold three points. First to 300. About 8 minutes."), from a new `summary` text on the mode rows.
- When a mode change forces a map change (or the reverse), the control that changed flashes and shows "changed to fit <mode>" for 3 s beside it. Today the only sign is a status line.
- Bot difficulty is a room setting on the page (Easy, Normal, Hard), stored on the room, defaulting to the server's current setting. The server-wide toggle on the Field page becomes the default for new rooms.
- Ready flags: a change to mode, map, team size or teams un-readies everyone, as today. A change to bots on or off, bot difficulty, or a House pick by another player does not.
- Practice on the home page: its card names the bot difficulty and has the same three-way switch.

This section touches screens and a few server room actions (`SocialHub`, `Campaign` room settings). No packet format changes: the room state already travels as a JSON view. It can be built before the port.

## 4. Leaving, surrender, late joiners (F4)

Today a dropped commander is already covered: the army keeps its orders for 60 s, then a bot commands it until the owner returns.

- **Leave match**: a button on the pause screen. It asks once ("Leave the match? A bot takes your army. You can rejoin from the war council.") and then does what a disconnect does, without the 60 s wait: the bot takes over at once and the player returns to camp. Rewards follow the existing share-of-time rule.
- **Rejoin**: the war council shows "Rejoin match" while the player's seat is held by a bot.
- **Surrender** (Kort: team vote, majority): available once one third of the match time or score has passed (under best of five: from round 2). Any human commander opens the vote from the pause screen; teammates get a prompt for 20 s. It passes when more than half of the side's humans agree; a side with one human passes at once after a confirm. A failed vote cannot be called again by that side for 90 s. A passed vote ends the match as a loss for that side through the normal Ended sequence. Bots do not vote.
- **Late joiners**: a player who enters a room whose match is running sees "Match in progress" with two choices: Watch (overhead camera on a point of their choice, no fog beyond what their side sees, no orders) or Wait. If their side has a bot-held seat, a third choice, Take a seat, puts them in at the next round start (best of five) or at once (single round).

Values: `surrenderAfterShare` 0.33, `surrenderVoteSeconds` 20, `surrenderRetrySeconds` 90.

Touches rules and packets. After the port.

## 5. Small items

- **Map build wait (F5).** The "Preparing <map> / N%" screen shows the map's card, both line-ups with Houses, and one tip from a list of 20 (written with the lore lead). Starting the build during the room countdown is a later step and not part of this spec.
- **Reward gate (F6).** The 180 s gate counts the whole match, all rounds together. The results page already says why a reward is zero; it now also lists marks.
- **While down (F7, Kort).** Orders stay blocked during the 12 s respawn. The respawn card says "Your troops keep their last orders", and an attempted order answers "You are down" (army control spec, section 5).
- **Intro skip (F8).** The audit suspects the intro's skip key also toggles the command panel. Confirm in a run; if true, the skip consumes the key press.

## 6. What it touches

| Part | Change | When |
|---|---|---|
| `WarTableLobby`, `WarTablePages` (Friends), `WarTableHome`, `SocialHub`, room settings in `Campaign` | one room page | before the port |
| `Campaign.finish`, match state, `WarTableReport`, camera | Ended, best commander, play again | after the port, with the economy spec's rounds |
| pause screen, `Campaign` seats, vote packets | leave, rejoin, surrender, watch | after the port |
| `sheets/design.json` | the values named above, mode `summary` text | with each part |

## 7. How it is checked

- `ui-lobby` suite: code join, invite and Start now from the war council; no ready reset on a bot toggle.
- A `modes` run per mode that asserts the order Ended, best commander, results, and that rewards are saved once.
- A two-client run for Play again and for a surrender vote.
- Kort plays one match end to end for the feel of the 8 s and 4 s beats.

## 8. Open points

- Whether the best-commander orbit should be skippable by each player for themselves. Proposed: yes, with the same key that skips the intro.
- Whether Watch should show both sides' troops to a spectator who is not seated on either side. Proposed: no, a watcher always watches as a member of one side, so nothing leaks through voice chat.
