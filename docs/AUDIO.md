# Audio cues

Every sound the mod owns is an original synthesis; nothing is downloaded or borrowed, so there are no third-party licences or credits to carry. Table: `AudioCues.java` (volume, category, who hears it, cooldown). Files: `src/main/resources/assets/gravesandglory/sounds/`, registered in `sounds.json` with subtitles in `lang/en_us.json`.

Rebuild: `python tools/audio/make_cues.py [cue_id ...]` (numpy and ffmpeg on PATH; mono 44.1 kHz Ogg Vorbis, q4, deterministic seeds). The six older files (battle_intro, unit_reveal, victory, three horns) come from `tools/create_battle_audio.py`. Change a level in `AudioCues`, not in the file.

| File | Trigger | How it was made |
| --- | --- | --- |
| ui_confirm.ogg | A War Table press that turns something on (ready, confirm) | Soft low bell tick plus a faint iron click, 0.3 s |
| ui_back.ogg | Back from a War Table page, or a toggle turned off | Dull wood tick with a muted iron click, 0.2 s |
| ui_deny.ogg | A refused Armoury action | Two muted wooden thunks, 0.3 s |
| countdown_tick.ogg | Each of the last three seconds of a lobby or pad countdown | Low war-drum stroke with a wood knock, 0.4 s |
| battle_intro.ogg | Battle entrance camera | Drums, two horn chords and bell (create_battle_audio.py) |
| unit_reveal.ogg | A new unit joins your banner | Rising bell chimes (create_battle_audio.py) |
| victory.ogg | Round ends, winner or draw | Three-note horn rise over a drum (create_battle_audio.py) |
| defeat.ogg | Round ends, losing side | Low toll, falling dirge horn pair, sinking drum, reverb tail, 4 s |
| horn_charge / horn_rout / horn_rally.ogg | The commander's charge, retreat and rally orders | Horn notes with drum (create_battle_audio.py) |
| capture_start.ogg | A point is contested | Nine dragged chain-mail clicks into an iron clank, 1.4 s |
| capture_won.ogg | Your side takes a point (banner raised) | Two rising horn notes, bell, drum, 3 s |
| capture_lost.ogg | Your side loses a point (banner lowered) | Falling low horn pair, heavy drum, low bell, 2.8 s |
| gate_hit.ogg | A ram strikes the siege gate | Deep wood thunk, iron clank, rumble, 1.6 s |
| gate_shut.ogg | The siege gate shuts | Chain run, iron slam, long reverb tail, 2.6 s |
| gate_breach.ogg | The gate is breached | Oak crack, deep boom, rumble and falling stones, 3.8 s |
| convoy_roll.ogg | The wagon starts rolling | Wheel groan with cart-rut thuds, 1.7 s |
| convoy_stop.ogg | The wagon stops | Short creak dying into a wooden thunk, 0.8 s |
| convoy_rollback.ogg | The wagon rolls back | Reversed creak with rut thuds and a low thump, 1.4 s |
| convoy_checkpoint.ogg | The convoy reaches a checkpoint | Two bell strokes over a low hum, 3.6 s |
| wave_warning.ogg | Survival warns of the next wave (soul-fire sites glow) | One long cold horn with a flat second voice and a drum, 3.7 s |
| wave_rise.ogg | The warned wave rises | Swelling drum roll, then a full horn line and big drum, 4.2 s |

Not yet cued: troop deaths and per-unit combat sounds still use vanilla events in `Abilities`, `BattleEffects` and `Tactics`; they are positional and want a separate pass. The Banner page has no drag sounds.
