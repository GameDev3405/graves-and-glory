# Audio cues

The 23 older cues are original syntheses. The combat, magic, clock, drum and music cues are royalty-free Pixabay recordings (Pixabay Content Licence: use in a game is allowed, no attribution required; credited below anyway), converted by `tools/audio/import_pixabay.py`. Table: `AudioCues.java` (volume, category, who hears it, cooldown). Files: `src/main/resources/assets/gravesandglory/sounds/`, registered in `sounds.json` with subtitles in `lang/en_us.json`.

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

## Pixabay cues

Each id is a variant group in `sounds.json`; the game picks one file at random. Combat cues are positional (Audio.at), keyed per soldier, with a cooldown and a per-tick cap in `AudioCues` so thirty troops stay a murmur; pitch varies by about 6 percent. One-shots are mono, 44.1 kHz, Ogg Vorbis q4, trimmed of leading silence and cut under 1.5 s, matched in loudness inside each group.

| Event | Trigger | Files |
| --- | --- | --- |
| combat.shield_block | A hit on a raised shield (melee or missile), at the blow's contact frame; the Knight's shield bash | shield_block_1 to 3 |
| combat.heavy_metal_hit | A landed blow by an axe, mace or hammer troop or the Colossus; a slam | heavy_metal_hit_1 |
| combat.sword_swing | A blade troop's swing (every second slot) | sword_swing_1, 2 |
| combat.sword_clash | A blade hitting an armed melee troop | sword_clash_1 to 4 |
| combat.armor_hit | A blade hitting a metal-armoured troop with no weapon | armor_hit_1 |
| combat.bow_shoot | An archer or crossbow shot leaves (lower for siege bolts) | bow_shoot_1 |
| combat.arrow_hit | An arrow or bolt lands | arrow_hit_1 |
| magic.cast | A caster's shot as the spell begins (the Mist Caller's cloud sounds magic.impact when it lands) | magic_cast_1 |
| magic.impact | A caster's missile or blight lands | magic_impact_1, 2 |
| clock.tick | Each of the last ten seconds of a round's clock | clock_tick_1, 2 |
| battle.drums | Battle entrance camera, layered under battle_intro (kept) | battle_drums |
| music.calm | Banner Camp, lobby, War Table; 45 s gap between plays | music_calm_long_day |
| music.epic | Once, after a round the local side won | music_epic_rebel_moon |
| music.battle_grim, music.battle_drums | A running battle, alternating; the round's serial picks the first | music_battle_dark_grim, music_battle_drums_stomp |

Music is in the music category, so the Music slider governs it, and `MusicDirector` stops vanilla music while it plays (fades 2 s in, 0.75 s out). It is silent through the entrance camera.

## Timing

Each cue plays at its moment (report: docs/reports/audio-cue-timing-2026-11-02.md; the check is AudioCuesCheck).

- Melee damage resolves on the tick a swing starts, but the blade meets its target at the attack clip's contact frame, so the hit sound (clash, armour, heavy, shield) waits `AudioCues.contactTicks` (House strike seconds x 20: gilded 4, graveborn 6, wildmoot 5, ironclad 7, tidecallers 6, other 5). The queue is `AudioCues.Schedule`, run by `Audio.tick` each server tick and cleared by `Audio.reset` at a round's start and end.
- Missiles and spells sound when they land (the projectile's landing tick), a bow when it is loosed. The strike sat late in three approved samples (bow_shoot_1 release 0.26 s in, arrow_hit_1 impact about 0.33 s, armor_hit_1 ring 0.15 s); Kort approved trimming them on 2026-11-03 (`LEAD` in `tools/audio/import_pixabay.py`, 10 ms under the strike), each re-levelled to the old file's EBU R128 integrated loudness within 0.2 LU. Now 0.65 s, 0.23 s and 0.76 s long.
- gate_shut starts 7 ticks before the portcullis drops (the crash is 0.35 s in); wave_rise starts 30 ticks before the wave rises (the hit is 1.5 s in); the gate hit and the breach sound with the blow that causes them.
- The music director waits the length of the victory (64 ticks) or defeat (84 ticks) sting before the next track starts.

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

## Still missing

- Troop deaths and hurt grunts (vanilla mob sounds), beast attacks (hounds, tusk, Brute), Mortar and Ballista firing and shell bursts, the Firepot burn, spear thrusts and harpoon throws.
- Footsteps and marching, formation changes, banner flapping, capture-point progress.
- Siege ram swing, gate chains in motion, wagon wheels loop, survival wave ambience beyond the two horns.
- Victory/defeat music for the losing side (defeat has only its sting), a tense track for overtime, a menu track.
- Banner page drag sounds; Survival and Veterans still use vanilla sounds (soul escape, level-up chime, wither spawn).

## Installing new candidates

The candidate takes live in `work/audio-cues/` (not in the game yet). Once Kort picks, write `work/audio-cues/PICKS.json` (shape: `tools/audio/PICKS.example.json`) and run `python tools/install_audio.py --dry-run`, then without the flag. It copies the files, updates `sounds.json` and appends the credits to `docs/AUDIO-CREDITS.md`. `--report` lists the vanilla sound calls a cue would replace; those code changes are made by hand. New events still need an `AudioCues` entry and a subtitle.
