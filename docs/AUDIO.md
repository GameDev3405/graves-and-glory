# Audio cues

Credits for every file installed from Kort's picks (2026-11-04) are in docs/AUDIO-CREDITS.md and supersede the Pixabay table below where they differ; round 2 picks (2026-11-04) are listed under "Round 2" below. ui_deny is still generated, awaiting Kort's own sourcing.

The 23 older cues are original syntheses. The combat, magic, clock, drum and music cues are royalty-free Pixabay recordings (Pixabay Content Licence: use in a game is allowed, no attribution required; credited below anyway), converted by `tools/audio/import_pixabay.py`. Table: `AudioCues.java` (volume, category, who hears it, cooldown). Files: `src/main/resources/assets/gravesandglory/sounds/`, registered in `sounds.json` with subtitles in `lang/en_us.json`.

Rebuild: `python tools/audio/make_cues.py [cue_id ...]` (numpy and ffmpeg on PATH; mono 44.1 kHz Ogg Vorbis, q4, deterministic seeds). The six older files (battle_intro, unit_reveal, victory, three horns) come from `tools/create_battle_audio.py`. Change a level in `AudioCues`, not in the file.

| File | Trigger | How it was made |
| --- | --- | --- |
| ui_confirm.ogg | A War Table press that turns something on (ready, confirm) | Soft low bell tick plus a faint iron click, 0.3 s |
| ui_deny.ogg | A refused Armoury action | Two muted wooden thunks, 0.3 s |
| countdown_tick.ogg | Each of the last three seconds of a lobby or pad countdown | Low war-drum stroke with a wood knock, 0.4 s |
| battle_intro.ogg | Battle entrance camera | Drums, two horn chords and bell (create_battle_audio.py) |
| unit_reveal.ogg | A new unit joins your banner | Rising bell chimes (create_battle_audio.py) |
| victory.ogg | Round ends, winner or draw | Three-note horn rise over a drum (create_battle_audio.py) |
| horn_charge / horn_rout / horn_rally.ogg | The commander's charge, retreat and rally orders | Horn notes with drum (create_battle_audio.py) |
| capture_start.ogg | A point is contested | Nine dragged chain-mail clicks into an iron clank, 1.4 s |
| capture_won.ogg | Your side takes a point (banner raised) | Two rising horn notes, bell, drum, 3 s |
| capture_lost.ogg | Your side loses a point (banner lowered) | Falling low horn pair, heavy drum, low bell, 2.8 s |
| gate_hit.ogg | A ram strikes the siege gate | Deep wood thunk, iron clank, rumble, 1.6 s |
| convoy_roll.ogg | The wagon starts rolling | Wheel groan with cart-rut thuds, 1.7 s |
| convoy_stop.ogg | The wagon stops | Short creak dying into a wooden thunk, 0.8 s |
| convoy_rollback.ogg | The wagon rolls back | Reversed creak with rut thuds and a low thump, 1.4 s |
| convoy_checkpoint.ogg | The convoy reaches a checkpoint | Two bell strokes over a low hum, 3.6 s |
| wave_warning.ogg | Survival warns of the next wave (soul-fire sites glow) | One long cold horn with a flat second voice and a drum, 3.7 s |
| wave_rise.ogg | The warned wave rises | Swelling drum roll, then a full horn line and big drum, 4.2 s |
| ending_draw.ogg | Match ends in a draw (results screen) | Held open fifth on two horns, one bell, soft drum, 2.1 s; -22 LUFS |
| podium_drum.ogg | Podium reveal | Drum roll that swells and quickens, ending on one heavy stroke for the MVP, 4.5 s; -26 LUFS |
| podium_reveal.ogg | One hit per podium place (three, 1.5 s apart) | Deep drum, brass stab, low bell, 1.5 s; -22 LUFS |
| reward_tick_1..3.ogg | Coin or tally tick (up to about 15 a second) | A tiny coin on iron, 70 ms, three pitches; no subtitle |
| reward_total.ogg | The reward total line lands | Heavy drum, iron settle, low bell, coin shimmer, 1.6 s; -24.5 LUFS |
| level_up.ogg | A progress bar crosses a level or Renown tier | Rising bell run into a bright horn chord, 2.2 s; -22 LUFS |
| award.ogg | MVP and award chips appear | Two-note horn call over a soft drum, 1.6 s; -22 LUFS |

## Pixabay cues

Each id is a variant group in `sounds.json`; the game picks one file at random. Combat cues are positional (Audio.at), keyed per soldier, with a cooldown and a per-tick cap in `AudioCues` so thirty troops stay a murmur; pitch varies by about 6 percent. One-shots are mono, 44.1 kHz, Ogg Vorbis q4, trimmed of leading silence and cut under 1.5 s, matched in loudness inside each group.

| Event | Trigger | Files |
| --- | --- | --- |
| combat.shield_block | A hit on a raised shield (melee or missile), at the blow's contact frame; the Knight's shield bash | shield_block_1 to 3 |
| combat.heavy_metal_hit | A landed blow by an axe, mace or hammer troop or the Colossus; a slam | heavy_metal_hit_1 |
| combat.sword_swing | A blade troop's swing (every second slot) | sword_swing_1, 2 |
| combat.sword_clash | A blade hitting an armed melee troop | sword_clash_1 to 4 |
| combat.armor_hit | A blade hitting a metal-armoured troop with no weapon | armor_hit_1 |
| combat.bow_shoot | An archer or crossbow shot leaves (lower for siege bolts) | bow_shoot_1, 2 |
| combat.arrow_hit | An arrow or bolt lands | arrow_hit_1 |
| magic.cast | A caster's shot as the spell begins (the Mist Caller's cloud sounds magic.impact when it lands) | magic_cast_1 |
| magic.impact | A caster's missile or blight lands | magic_impact_1, 3, 4 (2 was dropped) |
| clock.tick | Each of the last ten seconds of a round's clock | clock_tick_1, 2 |
| battle.drums | Battle entrance camera, layered under battle_intro (kept) | battle_drums |
| music.calm | Banner Camp, lobby, War Table; 45 s gap between plays | music_calm_long_day |
| music.epic | Once, after a round the local side won | music_epic_rebel_moon |
| music.battle_grim, music.battle_drums | A running battle, alternating; the round's serial picks the first | music_battle_dark_grim, music_battle_drums_stomp |

Music is in the music category, so the Music slider governs it, and `MusicDirector` stops vanilla music while it plays (fades 2 s in, 0.75 s out). It is silent through the entrance camera.

## Ambience and music layers

`MusicDirector` runs two streams. The music track is as above, with two swaps: while the campaign briefing is open (the War Table's campaign sheet, or the old train page; `CampScreen.briefing()`) it plays `campaign_briefing_music` instead of `music.calm`, and while a Convoy round is in overtime it plays `overtime_music` instead of the battle tracks. Both loop until the moment passes and start at once, ignoring the calm gap.

Under it a separate looping bed plays in the ambient category, so the vanilla Ambient slider governs it. It fades in over 4 s and out over 2.5 s when the moment changes, and `AudioCues.bed(inCamp, mode, theme)` picks it. It keeps playing through the entrance camera.

| Moment | Bed |
| --- | --- |
| Banner Camp, lobby, War Table | camp_room_tone |
| Survival battle (any map) | survival_wind |
| Battle, river map | ambience_water |
| Battle, fortress map | ambience_crows |
| Battle, snow map | ambience_snow |
| Battle, forest map | ambience_birds |
| Battle, any other theme | ambience_wind |

The theme is `Spec.MAPS[CampaignClient.map].theme()`.

Protocol: the `battleMode` object in the campaign snapshot (STATE packet) gains a boolean `overtime`, true while a Convoy round's clock is out and the wagon holds it open (`Battle.inOvertime()`, which the overtime drum pulse also uses). It is additive, so PROTOCOL stays 1 and an older client ignores it. AudioCuesCheck and test_audio_events.py prove every event but the five below has a play site.

## Round 2 (Kort's picks, 2026-11-04)

Installed by `tools/install_audio.py` from `work/audio-cues/picks/round2`; credits in docs/AUDIO-CREDITS.md. `ui_back_7`, `gate_shut_8` and `gate_breach_5` replace the generated files (removed). `defeat_music_5` (35.7 s, streamed) is now the only variant of `defeat`, replacing the 5 s sting `defeat_4`; `AudioCues.DEFEAT_STING_TICKS` went from 100 to 714 so the music waits it out (AudioCuesCheck holds it within 1 tick of the file). `GATE_SHUT_LEAD_TICKS` went from 7 to 46 so the slam in `gate_shut_8` (peak 2.3 s in) lands as the portcullis meets the ground.

Played from `BattleEffects.feedback` under the horns' own once-per-kind-per-commander cooldown (`hornCooldownSeconds`). Kort, 2026-11-04: order sounds are heard on the battlefield, not only by the commander. The horns (horn_charge, horn_rout, horn_rally) and both shouts play as world sounds at the commander's position in the PLAYERS category, so every player in range hears them, enemies included. Ranges: horns 64 blocks (`combatDiscipline.hornVolume` 4.0), shouts 16 blocks (cue volume 1.0); range is about 16 x volume. A bot's orders play no sound. This replaces the old rule that the commander alone heard them:

| Event | Take | Where it plays |
| --- | --- | --- |
| order_charge_shout | formation_shout_7 | A Charge order, with horn_charge |
| order_hold_shout | formation_shout_5 | A Hold or Move order, or a formation change (kind "hold"; replaces the order chime for Hold and Move; no horn) |

Registered with files, subtitles and an `AudioCues` entry, but not played yet (no hook chosen): capture_rising (capture_rising_6), coin_clink (coin_clink_5), active_rally (commander_actives take active_rally_3), relic_use_light (relic_use_light_5), relic_use_lightning (relic_use_lightning_3, 5), veteran_levelup (veteran_levelup_5, would replace the vanilla level-up in `Veterans.java`), wagon_wheels (wagon_wheels_6), title_music (title_music_6, 3; streamed, no title-screen player yet). AudioCuesCheck lists them as not yet played.

`morale_break` is not a cue: the game has no morale (the no-routing design), so none is sourced.

## Kort sourcing

Kort is finding these himself; the current files stay in the game until he does.

- beast_bite: the beast attack sounds (hound_attack, brute_attack, tusk_attack) have no distinct bite; the candidates were dog snarls.
- ui_deny: the generated two-thunk file `ui_deny.ogg`, played for a refused Armoury action.

## Timing

Each cue plays at its moment (report: docs/reports/audio-cue-timing-2026-11-02.md; the check is AudioCuesCheck).

- Melee damage resolves on the tick a swing starts, but the blade meets its target at the attack clip's contact frame, so the hit sound (clash, armour, heavy, shield) waits `AudioCues.contactTicks` (House strike seconds x 20: gilded 4, graveborn 6, wildmoot 5, ironclad 7, tidecallers 6, other 5). The queue is `AudioCues.Schedule`, run by `Audio.tick` each server tick and cleared by `Audio.reset` at a round's start and end.
- Missiles and spells sound when they land (the projectile's landing tick), a bow when it is loosed. The strike sat late in three approved samples (bow_shoot_1 release 0.26 s in, arrow_hit_1 impact about 0.33 s, armor_hit_1 ring 0.15 s); Kort approved trimming them on 2026-11-03 (`LEAD` in `tools/audio/import_pixabay.py`, 10 ms under the strike), each re-levelled to the old file's EBU R128 integrated loudness within 0.2 LU. Now 0.65 s, 0.23 s and 0.76 s long.
- gate_shut starts 7 ticks before the portcullis drops (the crash is 0.35 s in); wave_rise starts 30 ticks before the wave rises (the hit is 1.5 s in); the gate hit and the breach sound with the blow that causes them.
- The music director waits the length of the victory (64 ticks) or defeat (714 ticks, the 35.7 s defeat music) sting before the next track starts.

## Credits

Source: pixabay.com, downloaded 2026-11-02 (Pixabay Content Licence). Author is the Pixabay account named in the source filename.

| Author | Source file | Used as |
| --- | --- | --- |
| yodguard | yodguard-shield_impact-1-382410.mp3, -4-382413, -5-382415 | shield_block_1 to 3 |
| yodguard | yodguard-a-massive-axe-impact-hitting-metal-3-450254.mp3 | heavy_metal_hit_1 |
| dragon-studio | dragon-studio-sword-slice-393847.mp3, sword-slice-2-393845 | sword_swing_1, 2 |
| dragon-studio | dragon-studio-sword-clashhit-393837.mp3 | sword_clash_1 |
| freesound_community | freesound_community-sword-against-sword-6341 (2).mp3 | sword_clash_2 to 4 (three hits cut from it) |
| dragon-studio | dragon-studio-armor-impact-from-sword-393843.mp3 | armor_hit_1 |
| u_2c8g87fmmx | u_2c8g87fmmx-bow-sound-596323.mp3 | bow_shoot_1 |
| dennish18 | dennish18-arrow-body-impact-146419.mp3 | arrow_hit_1 |
| daviddumaisaudio | daviddumaisaudio-magical-spell-cast-190272.mp3 | magic_cast_1 |
| rescopicsound | rescopicsound-elemental-magic-spell-impact-outgoing-228342.mp3 | magic_impact_1 |
| dragon-studio | dragon-studio-earth-spell-impact-478381.mp3 | magic_impact_2 |
| oxidvideos | oxidvideos-clock-ticking-582761.mp3 | clock_tick_1, 2 |
| dragon-studio | dragon-studio-ancient-war-drums-463214.mp3 | battle_drums |
| purrplecat | purrplecat-long-day-518602.mp3 | music_calm_long_day |
| audioatlant | audioatlant-rebel-moon-inspiring-emotional-epic-cinematic-trailer-short-1-628477.mp3 | music_epic_rebel_moon |
| alex_besss | alex_besss-dark-and-grim-epic-trailer-short-628100.mp3 | music_battle_dark_grim |
| miromaxmusic | miromaxmusic-epic-drums-stomp-percussion-no-copyright-628251.mp3 | music_battle_drums_stomp |

## Hooked by the audio/hooks pass

Server cues play through `Audio.at` (a small random pitch spread, a per-key cooldown); client cues through `UiSounds.play`.

| Cue | Where it plays |
| --- | --- |
| hound_attack | Abilities.pounce (the hound's pounce; was the vanilla wolf growl) |
| brute_attack | Abilities.trample (the Brute's trample; was the polar-bear warning) |
| tusk_attack | Audio.swing, for the Wildmoot Tusk's blade-arm swing |
| spear_thrust | Audio.swing, for a spear-family or trident melee troop |
| ballista_release, mortar_fire, harpoon_throw | Audio.shot, by troop id (the Mortar had no arm, so no shot sound at all) |
| dummy_thud | GravesAndGlory structure hit and fell cue (was wool hit/break); the gate keeps gate_hit/gate_breach |
| firepot_burn | Abilities, the Firepot's mist lands (was the vanilla fire charge) |
| healer_cast | Statuses cleanse (was the amethyst chime) |
| soul_leaving | BattleEffects.soul (Rise Again, Grave Bloom) and SurvivalMode's wave warning at each rise site (was the vanilla soul escape) |
| relic_use_dark, relic_use_ice, relic_use_earth, relic_use_fire | Bloodrush, Dash, Shove and War Cry in RelicEffects (the relics are not elemental, so this is a flavour mapping); Volley keeps the vanilla arrow shot |
| overtime_drum_pulse | GravesAndGlory battle loop: a Convoy round whose clock is out and whose wagon holds it open, to every seat, once per 7 s |
| stamp_seal, page_turn | WarTableHome: the wax seal landing (was the vanilla page turn) and a sheet rising |
| banner_drag | WarTablePages, when a Banner page press becomes a drag |
| equip, break_down, repair_hammer, blacksmith_roll | WarTableGear actions (the Roll button no longer plays the UI confirm) |
| rarity_common ... rarity_epic | WarTableGear, the forge reveal card (Legendary takes the epic sting) |
| forge_marks | WarTableGear, when the gear shards count rises |

## Registered, not played

These have files, events and an `AudioCues` entry, but no clear game moment, and the check lists them so a later hook removes the name there. Each hook is one `Audio.at`, `Audio.all` or `UiSounds.play` call.

- troop_voices, march_leather: no per-troop voice or footstep system exists; adding one is its own design.
- siege_ram_swing: no ram unit. gate_chains: the gate's shutting is already gate_shut.
- banner_flap: no banner idle moment.
- ending_draw, podium_drum, podium_reveal, reward_tick, reward_total, level_up, award: the match-ending cues (Kort, 2026-11-13, "Sound"); all `Scope.LOCAL`. The `ui/ending-polish` branch plays them by name; once a literal `Cue.NAME` play site lands, remove the name from `OFF` in `test_audio_events.py` and from `unplayed` in `AudioCuesCheck.java`.

## Still missing

- Troop deaths and hurt grunts (vanilla mob sounds), shell bursts.
- Formation changes, capture-point progress.
- Wagon wheels loop, survival wave ambience beyond the two horns.
- Hooks for the Round 2 events that are registered but not played.
- Veterans still use vanilla sounds (level-up chime, wither spawn).

## Installing new candidates

The candidate takes live in `work/audio-cues/` (not in the game yet). Once Kort picks, write `work/audio-cues/PICKS.json` (shape: `tools/audio/PICKS.example.json`) and run `python tools/install_audio.py --dry-run`, then without the flag. It copies the files, updates `sounds.json` and appends the credits to `docs/AUDIO-CREDITS.md`. `--report` lists the vanilla sound calls a cue would replace; those code changes are made by hand. New events still need an `AudioCues` entry and a subtitle.
