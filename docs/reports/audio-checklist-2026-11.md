# Audio checklist, plan 4F.4 (2026-11-01)

Source of truth for volume, pitch, category, audience and cooldown: `AudioCues.java` (one table). `Audio.java` plays it on the server, `UiSounds.java` on the client, `sounds.json` maps each event (vanilla remaps use `"type": "event"`, so no audio files were added). The 16 vanilla targets were checked against the 1.20.1 asset index.

Checked column: **T** = asserted by `AudioCuesCheck` (fast verify: table, sounds.json, lang, files, cooldowns, every cue has a caller); **E** = ear check in a client, not done (no client launched). Nothing here is accepted by ear yet.

## New and rewired cues

| Event | Sound | Vol / pitch | Heard by | Cooldown | Checked |
|---|---|---|---|---|---|
| Capture start (challenger begins a point) | vanilla item.armor.equip_chain | .6 / 1.0 | all seated players | 10 s, one per round | T, E |
| Capture won | block.beacon.activate | .6 / 1.3 | taker's team | 1 s per point | T, E |
| Capture lost | block.beacon.deactivate | .7 / .9 | the other team | 1 s per point | T, E |
| Siege yard falls | capture won / lost cues | as above | per team | as above | T, E |
| Gate hit | block.anvil.hit | .5 / .55 | everyone near the gate | 0.4 s | T, E |
| Gate shut (portcullis) | block.iron_trapdoor.close | .9 / .6 | all seated | one-off | T, E |
| Gate breach | entity.generic.explode | .5 / .6 | all seated | one-off | T, E |
| Wagon rolls (first move) | entity.minecart.riding | .4 / .8 | all seated | 10 s | T, E |
| Wagon stopped | block.wooden_trapdoor.close | .6 / .7 | all seated | 10 s | T, E |
| Wagon rolls back | block.piston.contract | .5 / .7 | all seated | 10 s | T, E |
| Convoy checkpoint | block.bell.use | .6 / 1.2 | all seated | one-off | T, E |
| Wave warning (hook only) | event.raid.horn | .7 / 1.0 | all seated | 5 s | T; no caller until Survival calls `Audio.all(b, Cue.WAVE_WARNING, key)` |
| Victory sting | own file victory.ogg (also a draw) | .8 / 1.0 | each participant alone | one-off | T, E |
| Defeat sting (losing side) | block.respawn_anchor.deplete | .7 / .75 | each losing participant alone | one-off | T, E |
| Countdown tick (last 3 s, room and pad) | block.note_block.bit | .5 / 1.0 | that room or pad's members | none (3 per start) | T, E |
| UI confirm (Ready on) | block.note_block.chime | .5 / 1.3 | local | 0.2 s | T, E |
| UI back (War Table back, Ready off) | ui.button.click | .5 / .8 | local | 0.2 s | T, E |
| UI deny (locked Armoury buy) | block.note_block.bass | .6 / .6 | local | 0.3 s | T, E |

## Existing cues, unchanged (own files)

| Event | Vol / pitch | Heard by | Limit | Checked |
|---|---|---|---|---|
| battle_intro | .8 / 1.0 (now read from the table) | local, at the intro | one per round | T, E |
| unit_reveal | .7 / 1.0 | the recruiting player | one per recruit | T, E |
| horn_charge / horn_rout / horn_rally | combatDiscipline.hornVolume (1.6) / 1.0 | the commanding player only | 8 s per kind per player | T, E |

## Existing vanilla calls, left as they are (battle texture)

Abilities: firecharge .8/.9, shield block 1/.7, anvil land .35/.7, polar bear 1/.8, wolf growl 1.5/1.0, wolf howl 1.5/.8, evoker prepare .6/1.3, drowned 1/.6. Commander growl 1/.6. Soul escape 1/.9. Veteran flash (level-up) 1.5/.9. Amethyst chime .6/1.4. Melee cues .3 with random pitch; shots .25/1.1. Command chime .6/1.1 to the commander. Banner structure wool hit .8/1.0 and break 1/.8. All positional, hostile or player category, none edited.

## Fixed along the way

The three mode-win toasts and the checkpoint level-up played at `BlockPos.ORIGIN`, so nobody on the map heard them. Removed; the stings and the checkpoint cue now reach the players.

## Mix check

- Loudness: every new cue is at most .9 (checked), most .4 to .7; vanilla combat is .25 to 1.5, so new cues sit under the fight. Loudest risks: wolf howl/growl at 1.5 and the veteran flash at 1.5 will mask capture cues mid-fight. Unchanged, flag for the ear pass.
- Overlaps: the victory or defeat sting plays as the round ends and nothing else is scheduled then. In Siege the yard-capture pair, the gate-shut cue (portcullisSeconds later) and the breach are separate events seconds apart in different timbres. Not heard together in a client yet.
- Back and Esc: a mouse press on a back plaque layers the vanilla click with ui_back (two quiet clicks). Fix later by moving ui_back to the Esc path only, if it reads as a double.
- Spam limits: capture start 10 s per round; capture won/lost 1 s per point; gate hit 0.4 s (a swarm on the gate rings at most 2.5 times a second); convoy roll/stop/rollback 10 s each (rollback and stop can flap, so they are limited here, not just by the feed's own 20 s); wave warning 5 s; UI cues 0.2 to 0.3 s. Countdown, stings and checkpoints are bounded by their events.
- Not covered: refusal notices from the server (not enough gold, etc.) have no deny cue, because notices carry no kind. Page-turn at .3 is the only existing UI sound besides button clicks.
