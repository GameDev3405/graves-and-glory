# Audio audit and direction, 2026-11-09

Audio lead, milestone 1. Branch `audio/lead` (cut from `main` a7811c4d), worktree `C:\Users\kortt\Melty\bb-audio`. No game files were changed. Nothing was heard: this audit reads files, code and measurements. Every claim about how a sound *feels* needs Kort's ears.

## 1. Summary

- **What exists.** 90 sound events (`sounds.json`, all registered in `AudioCues`), 143 Ogg files (16.4 MB, 23.7 minutes). 77 events have a play site; 13 are registered but never played. A music director (`MusicDirector`) runs one music track and one looping ambience bed. The cue table, throttles, per-tick caps and contact-frame timing are good engineering and worth keeping.
- **What is weak.** The game has **no House identity in sound**: all five Houses sound the same. Troops still speak with **vanilla mob voices** (husk groans for Gilded knights, piglin grunts, drowned gurgles, ravager roars). The sounds heard most in battle (bow shot, arrow hit, armour hit, heavy hit, spell cast, spell impact) each have **one take**. Music is horizontal only (pick a track, play it, pause), with no response to the fight. Kort has deleted the camp, both battle, briefing and overtime tracks in his checkout, so once that lands **battles and the camp have no music at all**. There is no loudness standard: files range from -13.7 to -30.8 LUFS and **9 files clip** (true peak above 0 dBTP).
- **What I propose.** A short style guide built on the lore ("the land is breathing"), five House sound palettes, a four-tier battle music system driven by one intensity number, map beds with spot sounds, and a data-driven audio layer (JSON) so a season can add a House, unit, map or chapter with sound and no code. The first steps are cheap fixes (clipping, levels, variants, silencing vanilla mob voices); the big step is commissioned House music. Ranked list with costs in section 6.

Grades (A best, F worst), reasons in section 3:

| Area | Grade | One line |
|---|---|---|
| Battle (combat) | C+ | Smart timing and caps, but one take for the most frequent sounds, clipping clashes, no army bed, vanilla deaths |
| Troops (identity, voices, orders) | D | Horns and shouts are in and sensibly throttled; no House identity, vanilla mob voices, 7 troop and battle events unplayed |
| UI (War Table) | B- | A coherent paper-wax-forge family; three takes clip, rarity stings are not in order, deny still generated |
| Lobby and hub | C- | A room-tone bed and countdown ticks; no hub life, no title music player, calm music deleted |
| Campaign | D+ | Briefing music (deleted) and shared stings; nothing per chapter, House or Legendary; ebb sound not sourced |
| Music | D | Levels well matched, but Kort removed most of it; no adaptive layers, no House themes, Content ID risk |
| Ambience | C | One mono loop per map theme with fades; no spot sounds, short loops, one bed clips |

## 2. Inventory

Full per-event and per-file tables are in the appendix (A1, A2). Main facts:

**Code that plays sound** (all in `src/main/java/gg/gravesandglory/`):

- `AudioCues.java`: the one table (event id, volume, pitch, category, who hears it, cooldown, per-tick cap, subtitle), plus timing constants, the `Schedule` that delays hit sounds to the contact frame, the `Throttle`, and `bed()` which picks the ambience bed.
- `Audio.java`: server side. `at` (positional, throttled, ±6% pitch spread when capped), `all`/`team` (to seated players), `to` (one player), `world` (structures), `swing`, `shot`, `hit`/`playHit` (material logic: shield, magic, arrow, heavy, blade-vs-armed clash, blade-vs-armour), `clock`, `ending`, `countdown`, `captured`.
- `UiSounds.java`: client UI cues (confirm, back, deny, and any cue via `play`), throttled.
- `MusicDirector.java`: client music and ambience bed (fades, gaps, epic after a win, briefing and overtime swaps, silence through the entrance camera, stops vanilla music while ours plays).
- `BattleEffects.java`: command horns and troop shouts as world sounds at the commander (64 blocks for horns, 16 for shouts), soul leaving.
- `CampaignClient.java`: battle intro and drums at the entrance camera.
- `PropTicker.java`: data-driven prop sounds from `data/gravesandglory/gg_props/*.json` (only `forge.json` campfire crackle and `anvil.json` anvil use, both vanilla). This is the one data-driven audio path in the game today and a good precedent.
- Play sites in `Abilities`, `Campaign`, `Combat`, `ConvoyMode`, `SiegeMode`, `SurvivalMode`, `GravesAndGlory`, `RelicEffects`, `Statuses`, `Tactics`, `WarTableGear`, `WarTableHome`, `WarTablePages`, `WarTableLobby`, `WarTableRecruit`, `WarTableReport`, `WarTableTroop`, `ArmouryTab`, `CommandMode`, `UiButton` (each listed per event in A1).

**Vanilla sounds still played directly** (not through the table): wolf howl at volume 1.5 (`Abilities.java:751`), drowned water (`Abilities.java:1046`), totem use (`GearCombat.java:330`), arrow shoot for Volley (`RelicEffects.java:366`), player level-up for the veteran flash at 1.5 (`Veterans.java:224/241`), note-block chime for other orders (`OrderRules.java:144`), campfire and anvil on props. And, largest of all, **every troop's own hurt, death and idle sounds**: troops are vanilla mobs (husk 6, drowned 4, wither skeleton 3, zombified piglin 3, pillager 3, vindicator 2, and one each of zombie, skeleton, stray, wolf, spider, polar bear, iron golem, evoker, ravager) and no code silences them (`setSilent` appears only on card portraits, camp villagers and projectiles; static read, not heard in game).

**Registered but never played (13):** `troop_voices`, `march_leather`, `siege_ram_swing`, `gate_chains`, `banner_flap`, `title_music`, `capture_rising`, `coin_clink`, `active_rally`, `relic_use_light`, `relic_use_lightning`, `veteran_levelup`, `wagon_wheels`. The test (`test-tools/standalone/test_audio_events.py`, `OFF` set) asserts they stay unplayed.

**Tests:** `test_audio_events.py` (events registered, subtitles, variant files exist, play sites, overtime flag), `AudioCuesCheck.java` (table, sounds.json, lang, files, cooldowns, sting lengths), `test_install_audio.py` (the picks installer). Tools: `tools/install_audio.py`, `tools/audio/import_pixabay.py`, `tools/audio/make_cues.py`, `tools/create_battle_audio.py`. Docs: `docs/AUDIO.md`, `docs/AUDIO-CREDITS.md`, `docs/reports/audio-checklist-2026-11.md`, `docs/reports/audio-cue-timing-2026-11-02.md`. Candidate library: `bannerbound/work/audio-cues/` (277 takes across 83 cues at the 4 November sourcing note; the manifest now lists 334 entries after round 2, with `manifest.json`, `LICENSES.md`, review pages, and Kort's picks in `picks/kort.json`).

**Open, not touched:** the five files Kort deleted in the `main` checkout, now in `bb-kort-maps` (branch `kort/maps`, uncommitted): `campaign_briefing_music_2.ogg`, `music_battle_drums_1.ogg`, `music_battle_grim_1.ogg`, `music_calm_1.ogg`, `overtime_music_1.ogg`. `sounds.json` still names them, so `test_every_variant_file_exists` fails there (known). In game, the events would play nothing; I have not checked whether `MusicDirector` then retries every few seconds and fills the log (it treats "not playing" as "ended", and the battle gap is 4 s). That should be checked before that change lands.

**Sources and licences:** `docs/AUDIO-CREDITS.md` has a row for 142 of the 143 files; the missing one is `ui_deny.ogg`, a synthesis from `make_cues.py` noted in `docs/AUDIO.md`. Licences: Pixabay Content Licence (most combat, music and many round 2 takes), CC0 (freesound), and **18 files under CC-BY** (11 CC-BY 4.0, 6 CC-BY 3.0, one listed as either): `convoy_stop` 1 to 3, `countdown_tick_2`, `gate_hit_4`, `gate_shut_8`, `horn_rally_2`, `unit_reveal` 2 and 3, `victory` 1 and 2, `wave_warning_1`, `capture_rising_6`, `veteran_levelup_5`, the briefing track ("The End Times" by vandalorum), and three Kevin MacLeod tracks (`defeat_music_5`, `title_music_3`, `title_music_6`). CC-BY needs a visible credit; where the credits ship in game or in the release notes is not checked here. No NC or ND files.

## 3. Audit

### 3.1 Format and loudness (all files, measured)

- **Format:** every file is Ogg Vorbis 44.1 kHz. All one-shots and beds are mono; the four Pixabay music tracks, the briefing track and `overtime_music_1` are stereo. `title_music` (both takes) and `defeat_music_5` are **mono music**: they should sound narrow next to the stereo tracks. Ambience beds are mono, but they play non-positional (`relative`, no attenuation), so stereo beds would be wider at no cost.
- **Clipping (true peak above 0 dBTP), 9 files:** `firepot_burn_2` +2.7, `relic_use_ice_1` +2.0, `ambience_water_1` +1.7, `sword_clash_1` +1.7, `relic_use_ice_2` +0.5, `sword_clash_3` +0.2, `page_turn_2` +0.2, `stamp_seal_1` +0.2, `stamp_seal_2` +0.1. Another 10 sit above -1 dBTP. A water bed that clips loops for a whole Rivergate battle.
- **Loudness spread:** one-shots run from -13.7 LUFS (`relic_use_lightning_5`) to -30.8 (`gate_hit_1`); beds from -22.7 (birds) to -29.0 (wind 2). No target is written down anywhere; the files were matched only within some groups. Twelve files too short for the gated measurement (0.4 s or shorter, so `n/a` in A2; `clock_tick_2` at exactly 0.4 s happened to measure) cannot be measured for integrated loudness and need a short-term or peak check instead.
- **Variant groups that do not match (spread above 3 LU):** `gate_hit` 5.0 LU, `relic_use_fire` 5.1, `relic_use_lightning` 4.8, `sword_clash` 4.3, `unit_reveal` 4.1, `forge_marks` 3.4. A random pick in these groups jumps in loudness, which will probably read as a mistake, not variety. Eight more groups differ by 1.7 to 2.5 LU (`harpoon_throw`, `hound_attack`, `capture_start`, `camp_room_tone`, `ambience_wind`, `troop_voices`, `title_music`, `sword_swing`).
- **The rarity stings are not a ladder.** Taking each file's loudness plus its table volume (LUFS + 20 log10 volume), uncommon plays loudest (about -21.5), then rare (-23.3), epic (-23.6) and common (-28.0). A rarer find should sound bigger, so epic should be the loudest.
- **Music levels are good:** the four Pixabay tracks are all -21.1 LUFS, briefing -20.9, overtime -20.9, defeat -20.3, title -20.5 and -22.2.

### 3.2 Variation and repetition

- 43 played events have one file. Most are fine (horns, stings, UI clicks, one-off gate cues), but six are among the most frequent sounds in a 30-troop battle: `combat.bow_shoot`, `combat.arrow_hit`, `combat.armor_hit`, `combat.heavy_metal_hit`, `magic.cast`, `magic.impact`. `docs/AUDIO.md` lists `magic_impact_1, 2`, but `sounds.json` names only `magic_impact_1`; `magic_impact_2` is credited but not in the game (a doc or install slip).
- Randomisation is pitch only, ±6%, and only on capped cues. There is no volume jitter and no "do not repeat the last take" rule (Minecraft picks variants at random, weighted, with repeats).
- Blade swings sound on every second slot; heavy weapons and beasts make no swing sound, only the hit. Fine for clarity.

### 3.3 Mix and categories

- Combat sounds use the **Hostile Creatures** slider (category `hostile`) for both sides; horns, shouts, stings and mode cues use **Players**; UI uses **Master**; music **Music**; beds **Ambient**. A player who lowers hostile mobs to quiet zombies at home also silences half the battle. There is no slider the player would recognise as "battle".
- **No ducking:** nothing lowers music or beds under horns, objective cues or stings. Minecraft has no bus system, so this needs a small client-side rule in `MusicDirector` (lower its own track and bed while a priority cue plays).
- The known issues in `docs/reports/audio-checklist-2026-11.md` stand: wolf howl and veteran flash at 1.5 can mask objective cues; a back plaque press layers the vanilla click with `ui_back`; server refusals ("not enough gold") have no deny cue.

### 3.4 Grades and reasons

**Battle, C+.** Strong base (from the code and docs; not heard in a client): hit sounds wait for the contact frame, a per-tick cap keeps 30 troops a murmur, materials choose the sound (shield, blade on blade, blade on armour, heavy, arrow, magic), all positional and mono. Against it: one take for the six commonest sounds; two of four sword clashes clip; no army bed, so a big fight probably sounds like a few separate clinks rather than a battle; troop deaths are vanilla mob deaths; no distance layer (a fight 40 blocks away sounds like the same clink, quieter).

**Troops, D.** Orders are the best part of the audio: horns everyone hears, troop shouts on Charge and Hold, sensible cooldowns. But nothing tells the five Houses apart by ear, the troops speak in vanilla mob voices that contradict the lore (Gilded are humans; the the Graveborn are thinking dead, not groaning zombies and skeletons), the special troops share one take each (Tusk, Mortar), and voices, marching, ram swing, banner flap and the veteran sting are registered but silent.

**UI, B-.** The War Table has a real sound language: page turn, wax seal, banner drag, forge roll, hammer, rarity stings. It is diegetic and fits the "war table" fantasy. Against it: three takes clip (`page_turn_2`, `stamp_seal_1`, `stamp_seal_2`), `ui_back` (-29.8 LUFS) is about 6 LU below `ui_confirm` (-23.2) and the War Table's other clicks, the rarity stings are not in order, `ui_deny` is still the generated placeholder, `coin_clink` is unplayed, and there is no hover sound family (fine for 1.0).

**Lobby and hub, C-.** A camp room tone (two takes, 40 s and 13 s) and countdown ticks. The calm music is among Kort's deletions. No title-screen music player (`title_music` exists, two takes, unplayed). No spot sounds for life in the hub apart from two vanilla prop sounds. Nothing yet for the season "tells" the lore plans for the hub (withered grass, pulsing redstone, receding water, the spire light).

**Campaign, D+.** The briefing has its own looping track (deleted by Kort), and recruits get `unit_reveal`. Otherwise the campaign borrows the battle stings. No chapter or House theme, no Legendary motif, no sound under after-battle lines, and chapter 1's approved glimpur ebb (N3, "a faint sound" on casters in battles 4 to 6) has no sound yet.

**Music, D.** The four tracks were level-matched with care and the director's fades and gaps are implemented sensibly in code. But Kort removed five music tracks (calm, both battle tracks, briefing, overtime), leaving only the victory track, the defeat music and the unplayed title tracks, which I read as "this music is not the game's voice" (to confirm with him). The system only swaps whole tracks; it does not follow the fight (only overtime changes it). No House themes. Several are Pixabay trailer tracks, the category most often hit by YouTube Content ID claims (see 4.2), which matters for trailers and streamers.

**Ambience, C.** Each map theme has a bed with fades, Survival and the camp have their own. But each bed is one mono loop (crows only 34 s, so the loop is audible), there are no spot sounds (a gull, a distant bell, a creaking mill), the plains map uses generic wind, and the water bed clips.

## 4. Research (web, 2026-11-09)

Two Sonnet 5.5 research passes; nothing was downloaded. Claims marked (excerpt) rest on search excerpts, not full reads. Not legal advice.

### 4.1 How good battle and tactics games build audio feel

- **Music follows one intensity number, in tiers.** Age of Empires IV uses four layers (exploration, tension, combat, heavy combat) driven by the summed "music importance" of units fighting in view; it rises fast and falls slowly ([Audiokinetic blog](https://blog.audiokinetic.com/en/the-music-of-age-of-empires-iv/), excerpt). Total War: Warhammer 2 ties campaign music to game state with a custom state filter ([Audiokinetic blog](https://blog.audiokinetic.com/en/total-war-warhammer-2-how-we-construct-and-systemically-use-our-music/), excerpt).
- **Layers inside a piece, segments between states.** Vertical layering raises intensity without breaking the piece, so each layer combination must stand alone; horizontal segments (intro, loop, outro, stingers) handle state changes ([GDC 2021 talks summarised on Game Developer](https://www.gamedeveloper.com/game-platforms/pure-vertical-layering-for-game-music-composers-from-spyder-to-sackboy-gdc-2021-)). Transitions wait for bar lines.
- **Factions are told apart by instrument palette plus a motif.** AoE IV gave each civilisation its own composer and instruments ([Dynamedion](https://dynamedion.com/?p=7779)); Battle Brothers uses instrumentation per faction ([GameBanshee](https://www.gamebanshee.com/k4ckc)); Three Kingdoms swapped the whole hit palette for traditional instruments and "ink" sounds, with five element kits as sweeteners ([Game Developer](https://gamedeveloper.com/audio/behind-the-audio-of-i-total-war-three-kingdoms-i-)); Bannerlord has a piece per culture ([TaleWorlds blog](https://www.taleworlds.com/en/Games/Bannerlord/Blog/11)).
- **Ambience is a 2D bed plus 3D spots plus random one-shots** with jittered timing so nothing repeats on a grid ([Audiokinetic](https://audiokinetic.com/ko/blog/designing-ambience-systems-in-wwise), [CRI](https://blog.criware.com/index.php/2020/12/14/ambience-design-in-atom-craft-part-1-2/)). Army scale comes from group vocal "walla" scaled by density, not per soldier (Three Kingdoms: about 3,000 crowd lines).
- **Impacts are layered** (transient, body, short tail), dry, chosen by material pair, with 3 or more variants, no immediate repeats, and small pitch and volume jitter ([guide](https://violetrecording.com/how-to-make-impact-sounds/), vendor). No authoritative jitter range was found; ±10% is a common starting point.
- **Voices are limited by priority**, dropping the quietest and farthest first ([Audiokinetic](https://www.audiokinetic.com/en/how-to-get-a-hold-on-your-voices-optimizing-for-cpu-part-1)). Graves and Glory's per-tick caps already do a simple version of this.
- **UI sounds share one family**: confirm rises, cancel falls, errors soft but distinct, and all quieter than feels natural because they repeat hundreds of times ([Civilization VII GDC session](https://schedule.gdconf.com/session/civilization-vii-impactful-ui-sound-design-at-scale/915690)).
- **The fight stays in front.** For Honor keeps combat foreground and moves music to rhythmic support in multiplayer ([MCV](https://www.mcvuk.com/drums-of-war-for-honors-audio-design/)). Into the Breach found music playing over deployment felt wrong and kept it rhythmic and simple ([NME](https://www.nme.com/features/gaming-features/into-the-breach-rock-the-spacebar-3280988), secondhand).
- **Loudness reference:** about -24 LUFS integrated (±2) for console games, true peak at or below -1 dBTP ([GANG IESD v03.02](https://www.audiogang.org/wp-content/uploads/2015/04/IESD-Mix-Ref-Levels-v03.02.pdf)).
- **Minecraft mods:** boss-music mods map entity ids to tracks in config (Baguette Music); Sound Physics Remastered adds reverb and occlusion (GPL-3.0, so a recommended separate install, never bundled); Presence Footsteps is PolyForm Shield (do not depend on it); AmbientSounds is LGPL-3.0. No library is needed: Minecraft's own `SoundInstance` classes do fades and layers.

### 4.2 Sourcing options

Kort's standing rule (2026-11-04, `bannerbound/work/audio-cues/SOURCING-2026-11-04.md`): **no generated audio**; sounds come from free recordings or compositions. The options below are graded against it.

| Source | Licence fit for a free mod jar | Content ID risk (trailers, streams) | Money | Fits Kort's rule |
|---|---|---|---|---|
| Freesound CC0 | Best: anything goes | Low | 0 | yes |
| Freesound CC-BY | Fine with a credit line | Low | 0 | yes |
| Freesound CC-BY-NC, MusicGen weights | Not allowed (non-commercial) | n/a | 0 | avoid |
| Pixabay Content Licence | Fine inside a game; the "standalone" clause makes raw files in a public repo the risky case ([summary](https://pixabay.com/service/license-summary/)) | **Music: real.** Some uploaders register tracks; claims are disputable with the licence certificate ([Pixabay blog](https://pixabay.com/blog/posts/how-to-clear-a-youtube-content-id-claim-with-a-pix-190/)) | 0 | yes |
| Sonniss GDC bundles (free yearly, tens of GB of pro SFX) | Allowed inside a finished game; no redistribution as sound files ([Sonniss](https://sonniss.com/gameaudiogdc/)) | Low | 0 | yes (recordings) |
| Soundly, Boom Library | Commercial game use (Soundly confirmed; Boom per retailers) | Low | subscription or per pack | yes |
| Epidemic Sound, Artlist, Envato | Game embedding excluded or unclear | Medium | subscription | avoid |
| Kevin MacLeod (incompetech) | CC-BY or $30 per song | **Claims happen** | 0 to 30 | yes |
| Commissioned composer | Clean if the contract says so | None if the contract covers it | about $100 to $1,000 per finished minute; licence-only cheaper than buyout ([GameSoundCon](https://www.gamesoundcon.com/post/2016/03/11/becoming-a-game-music-composer-what-should-i-charge-for-indie-games)) | yes |
| Synthesis (Vital, sfxr, Pure Data) | Output is ours | None | 0 | **no** under the rule |
| AI SFX (ElevenLabs paid) | Commercial use allowed | Low | subscription | **no** under the rule |
| AI music (Suno, Udio, AIVA) | Suno: paid tiers only, no copyright warranty, ongoing label suits; Udio: downloads off; AI-only output likely has no copyright | High legal uncertainty | subscription | **no** |

Reading: for sound effects, CC0 plus the free Sonniss bundles cover almost everything at pro quality for no money and fit Kort's rule. For music, the only route with no Content ID or licence doubt is a commissioned composer. Music is also where the game's identity lives (section 5).

### 4.3 What Minecraft allows (1.20.1 and 1.21.1)

- `sounds.json` fields: `name`, `type` (file or event), `volume`, `pitch`, `weight`, `stream`, `preload`, `attenuation_distance`; event-level `replace` and `subtitle` ([wiki](https://minecraft.wiki/w/Sounds.json)). Ogg only. One bad entry can make the whole `sounds.json` be ignored, so the check in 5.2 matters.
- **Mono attenuates with distance; stereo plays at constant volume** ([Fabric docs](https://docs.fabricmc.net/1.21.1/develop/sounds/custom)). Battle one-shots must stay mono; music and the non-positional beds can be stereo.
- **Channel pool:** 247 sounds at once; past that new sounds are dropped with a log line ([MC-180398](https://mojira.dev/MC-180398)). Streaming sounds have a much smaller share (the split is configurable in a third-party mod; the vanilla number was not confirmed). A layered music system (four stems plus a bed) must be prototyped against that share.
- **Music control:** `MusicTracker` (`play`, `stop`, `isPlayingType`) is the same in 1.20.1 and 1.21.1; vanilla music fading arrives later (1.21.4). Our own `SoundInstance` subclasses (as `MusicDirector` already does) handle fades and layers on both versions.
- **Stem sync is not guaranteed.** Several streams started on one tick can start slightly apart ([MC-277562](https://bugs-legacy.mojang.com/browse/MC-277562)). Before commissioning stems, a one-day prototype must prove drift stays inaudible over a 10-minute battle on both versions; if it does not, fall back to horizontal segments (whole pre-mixed intensity versions, crossfaded on bar lines), which need no sync.
- **1.21.1 port:** `Identifier.of(...)` replaces the constructor; sound events register through the registry as now; sound references move toward `RegistryEntry`. Small, mechanical changes in `Audio`, `UiSounds`, `MusicDirector`, `CampaignClient`, `BattleEffects`, `PropTicker`. 1.21 made jukebox songs data-driven, a pattern but not a tool for us.
- **Plain (vanilla) clients:** sounds can only reach them through a server resource pack (250 MiB limit, one pack before 1.20.3); `/playsound gravesandglory:<id>` then works. Data packs cannot add sounds. Our 16.4 MB fits easily. Relevant only if the post-port spike keeps plain clients.

## 5. Proposed direction

### 5.1 Audio style guide (one page, tied to the lore)

Source: the lore bible draft (`bannerbound-lore/docs/drafts/lore-bible-2026-11-09.md`), `names.md` and `season-framework.md` on `docs/lore-lead`.

**The idea in one line: the land is breathing, and only the player's ears notice first.** Wakeland is the bodies of sleeping giants; glimpur is their last breath. Act I is five Houses fighting over old wrongs while the land quietly shows signs of life. Sound can carry that hint better than any picture, because players feel sound before they name it.

Rules:

1. **Battles are loud, physical and human-scale.** Wood, iron, leather, bone, water, mud. Close, dry impacts; the fight is always in front of the music. No sci-fi whooshes, no modern trailer "braams", no synth risers.
2. **Each House has a palette, not just a theme.** Its instruments, its horn, its voice and its signature sweetener on hits and spells:

| House | Lore voice | Music palette | Horn and voice | Signature sweetener |
|---|---|---|---|---|
| Gilded Order | formal, dutiful; steel, white, gold, the Charter | brass, snare and timpani, low male choir, a tolling bell | bright straight trumpet; drilled shouts in unison | a bell-like ring on parries and Charter spells |
| Wildmoot | loud, feral grief | frame drums, bone rattles, throat singing, low drones, no brass | animal horn (aurochs), howls and roars | breath, snorts, cracking wood and antler |
| Ironclad | blunt, practical; forge, redstone | anvils as percussion, bellows, steam, low strings in fifths | steam whistle or forge horn; flat, clipped calls | metal ring, ratchets, a faint redstone hum |
| Tidecallers | fluid, furious; remade by the sea | low strings swelling like waves, water percussion, conch, wordless high voices | conch shell; voices with a wet, gargled edge | water splash and undertow on hits and spells |
| Graveborn | dry and tired; bone, rust, cut cords | sparse plucked strings, hollow wood, a cracked bell, whispered choir | a cracked, broken horn; dry rasping voices | bone clatter, snapped cords |
| Giants (NPC enemy from Act II; playable House in Act III) | calm, courteous, inevitable, never raised | sub-bass drones, slow stone grinding, very slow choir vowels on the root syllables (orru, meru, daal) | none; they never shout | a deep inhale and exhale |

3. **Glimpur has one sound family, and it ebbs.** Every House's spell shares one soft shimmer layer under its own sweetener: violet-white for the Gilded, Tidecallers, Ironclad and Graveborn casters, but **never for the Wildmoot** (the lore: violet belongs to the giants), whose casters get a breath-and-growl layer instead. Chapter 1's ebb (N3) is that layer stuttering and thinning for a moment, a faint "breath catching". Never use the giants' breath sound on the Wildmoot (the lore's "no violet" rule, in sound).
4. **The giant thread is a breath under everything.** From season 1, every ambience bed may carry an almost subliminal very low "breath" (a slow swell every 20 to 40 s, below conscious notice). Each season it grows a little: season 2 (the lore's tell is withering grass and herds drifting uphill) adds a faint heartbeat in the hub ground as an audio proposal of mine, not in the lore; season 3 a hum in redstone; season 4 the tide pulling back in the water beds; season 5 the spire tone. Act II makes it audible. This turns the season framework's "visual or audio tells" into one consistent device, cheap to build and memorable.
5. **The War Table is paper, wax, iron and wood.** UI sounds stay diegetic: no beeps. Confirm rises, back falls, deny is a soft dull knock, rarity steps up in pitch and size.
6. **The silent commander does not speak.** The player is heard only through horns and the troops' answers.
7. **Mix order** (what wins when everything plays): orders and objective cues, then nearby combat, then music, then distant combat and army bed, then ambience, then UI. UI is quiet because it repeats hundreds of times.
8. **Levels:** one-shots and music are mastered so the whole game sits near -24 LUFS integrated in play (the console reference), each file's true peak at or below -1 dBTP; variants in a group within 1.5 LU of each other.

### 5.2 Data-driven structure (so a season adds audio without code)

Today every cue is an enum entry in `AudioCues.java`, every choice is a `switch` in code (`bed()`, `shotCue()`, `meleeCue()`), and `sounds.json` is edited by a tool. Adding a House means code in six places. Proposal, in three layers that keep the current table and tests working:

**Layer 1: one sound-set file per thing, looked up by id, with fallbacks.** New JSON under `assets/gravesandglory/audio/` (client) and `data/gravesandglory/audio/` (server, for server-played cues). Each points at sound event ids; the files stay in `sounds.json`.

```
audio/houses/<house>.json    { "horn": {...}, "shout": {...}, "voice": "gilded_voice",
                               "music": "gilded", "sweetener": {"hit": "...", "spell": "..."},
                               "victory": "gilded_victory" }
audio/units/<troop>.json     { "voice": "...", "attack": "...", "shot": "...", "hurt": "...", "death": "...",
                               "steps": "plate" }            // any key may be left out
audio/maps/<map>.json        { "bed": [{"event": "rivergate_bed", "volume": 0.5}],
                               "spots": [{"event": "gull", "every": 400, "chance": 0.3, "radius": 40}],
                               "music": "rivergate" }
audio/music/<set>.json       { "bpm": 96, "beatsPerBar": 4,
                               "tiers": {"explore": [...stems], "tension": [...], "combat": [...], "heavy": [...]},
                               "stingers": {"victory": "...", "defeat": "...", "objective": "..."} }
audio/chapters/<id>.json     { "briefing": "...", "after": "...", "ebb": "glimpur_ebb" }
```

Lookup always falls back: unit, then its family (spear, bow, beast), then its House, then the default. A new unit with no file still sounds right; a new House with only a `houses/` file gets its own horn, voices and music. A new map with only a `maps/` file gets its bed and spots. A season ships a jar update (or later a resource pack) with new Ogg files, `sounds.json` entries and these small files; no Java changes.

**Layer 2: move the `AudioCues` table to data.** `data/gravesandglory/audio/cues.json` holds what the enum holds now (volume, category, scope, cooldown, cap, subtitle key). The enum stays as typed names for the cues code fires, read from the file at load. `AudioCuesCheck` and `test_audio_events.py` read the file instead of parsing Java. This also lets Kort change a level without a build.

**Layer 3: a check, not a promise.** A test that loads every audio JSON, resolves every event against `sounds.json`, checks every file exists, every group's loudness spread is within 1.5 LU, every true peak is at or below -1 dBTP, music is stereo, positional one-shots are mono, and every House, map and troop in `Spec` resolves to some sound (by fallback). Seasons then cannot ship a silent unit or a clipping file by accident.

Technical notes for 1.20.1 and 1.21.1 are in 4.3. The prop system (`gg_props/*.json` effects with `every`, `chance`, `volume`) already works this way for props and is the model for map spots.

### 5.3 The battle music system (vertical layers)

One intensity number per client, computed from what the server already sends: troops in contact near the camera, recent deaths, objective state, the clock. Four tiers: **explore** (deployment, manoeuvre), **tension** (armies within range, an objective contested), **combat** (melee joined), **heavy** (a big clash, a gate falling, the last 30 seconds, overtime). Each tier adds stems to the same piece (same tempo and key), so any combination sounds complete. Up fast (one or two bars), down slow (eight bars or more), never flapping. Victory and defeat are stingers that end the piece on a bar line, then the existing epic or defeat track.

Each House gets one battle piece in this shape; a battle plays the local player's House piece (or the map's piece in the campaign, the chapter's House). In Minecraft this is a few looping `SoundInstance`s started on the same tick with their volumes faded by `MusicDirector`; stems must be cut to whole bars of equal length so they stay aligned on every loop. Drift between streams is a known risk that needs a short prototype (see 4.3) before any music is commissioned.

## 6. Ranked polish steps

Cost key. **Tokens:** S under 50k (one Sonnet 5.5 high worker), M 50k to 150k, L over 150k. **Kort:** minutes of listening or deciding. **Money:** dollars. **Effect** is on "battles that look and feel great", my judgement without ears. All sourcing assumes Kort's rule (no generated audio) and needs his go before any download. Steps marked (code) change Java and should land **before or after** the 1.21.1 port, not during it, because the port touches the same audio classes (to agree with the workflow lead). Steps with no code are port-safe (new files plus `sounds.json`).

### For 1.0 (cheap and high value, in order)

| # | Step | Tokens | Kort | Money | Effect |
|---|---|---|---|---|---|
| 1 | **Mix-fix pass** with tools on the machine (ffmpeg): remove the 9 clipping peaks and bring the 10 near-peak files under -1 dBTP, match the 14 variant groups that differ by more than 1.5 LU (six of them by more than 3), lift `ui_back`, make rarity stings rise common to epic, set a true-peak ceiling of -1 dBTP. No new sounds. Script kept in `tools/audio/` so it is repeatable. | S | 10 min listen | 0 | Medium: removes crackle and level jumps nobody asked for but everybody hears |
| 2 | **Audio check in `verify fast`**: peaks, loudness spread per group, mono for one-shots, stereo for music, every House, map and troop resolves to some sound, no cue without a play site (extends `test_audio_events.py`, keeps `AudioCuesCheck`). | S | 0 | 0 | Medium: stops step 1 regressing and stops a season shipping a silent unit |
| 3 | **Music decision** (question 1 below) and its interim: restore the five deleted tracks, or ship 1.0 with the epic and defeat tracks only. Also check what the game does when a music file is missing. | S | 5 min | 0 | High: today, with the deletions applied, camp and battles have no music |
| 4 | **Per-House horns and shouts** (code, small): five horn variants and shout sets chosen by the commander's House, sourced from CC0/Sonniss horns and crowd shouts. You hear which army charges. First user of the House sound set (5.2). | M | 20 min picks | 0 | High: cheapest House identity there is |
| 5 | **Troop hurt, death and idle sounds by family** (code: one mixin that returns our events for tagged troops): human, beast, bone, forge, sea. Replaces husk groans and piglin grunts. | M | 20 min picks | 0 | High: the most heard sound in a battle, currently wrong for the lore |
| 6 | **3 to 4 takes** for the six most frequent combat sounds (bow, arrow hit, armour, heavy, cast, spell impact), plus the second `magic.impact` take already credited. | S | 15 min picks | 0 | High: ends machine-gun repetition in a 30-troop fight |
| 7 | **Army bed and distance layer** (code): a crowd and clash loop whose level follows the number of troops in contact near the camera, a quieter filtered version for fights beyond about 40 blocks. | M | 20 min | 0 | High: a battle sounds like a battle, not separate clinks |
| 8 | **Ducking** (code, small): `MusicDirector` lowers its own track and bed for about 2 s under horns, objective cues and stings. | S | 5 min | 0 | Medium |
| 9 | **Hook the finished-but-silent events** (code, one call each): `veteran_levelup` (replaces vanilla level-up), `active_rally`, `capture_rising`, `wagon_wheels`, `relic_use_light`, `relic_use_lightning`, title music on the title screen. | S | 10 min | 0 | Medium: free value, the files exist |
| 10 | **Map spot sounds and longer or stereo beds** (code plus data): the data-driven map file from 5.2, with 3 to 5 spots per map (gull, mill, distant bell, ice crack, owl), beds of at least 90 s, the water bed's clip fixed. | M | 20 min | 0 | Medium to high: maps feel inhabited |
| 11 | **Glimpur family and the chapter 1 ebb**: one shimmer layer under every spell, and the faint stuttering sound for the ebb (N3). Built with the chapter 1 code, not separately. | S | 10 min | 0 | Medium: the lore's first audible hint |

Totals for the eleven: about 450k to 800k Sonnet tokens in six or seven bounded workers, about 2 hours of Kort time in three batches (one listening batch per sourcing round: horns and troops, combat takes, ambience and spots), $0.

### Needs Kort's money or a bigger decision

| Step | Tokens | Kort | Money | Effect |
|---|---|---|---|---|
| **Stem-sync prototype** (code): four looping streams started together, 10-minute run on 1.20.1 then 1.21.1, drift measured. Gate for everything below. | M | 10 min | 0 | Decides vertical layers or crossfaded whole mixes |
| **Commissioned music**: one hub or camp piece plus one battle piece in the four-tier shape, about 6 to 8 finished minutes. Clean licence, no Content ID risk. | S to brief and integrate | 30 min brief and approvals | about $600 to $8,000 (range from the research: $100 to $1,000 per finished minute; licence-only is cheaper than a buyout) | Highest single lever on "feel" |
| **Four-tier adaptive battle music** (code, after the prototype and the first piece) | L | 30 min | 0 | High, depends on the music above |

### For later seasons (not 1.0)

- **Data-driven cue table** (Layer 2 in 5.2): move the `AudioCues` enum values to JSON. Best done once, between 1.0 and season 2, with steps 4 and 10 as the first users of Layer 1. L tokens (touches many tests).
- **Five House battle pieces and the Graveborn, Wildmoot, Ironclad, Tidecallers palettes in music**, one per season as the chapter introduces the House (about $600 to $8,000 each at 6 to 8 finished minutes).
- **The breath tell**: the subliminal low swell in beds from season 1, growing each season (heartbeat in hub ground, redstone hum, receding water, the spire tone).
- **Giant sound** for Act II: sub-bass drones and stone, root-syllable choir.
- **Troop acknowledgement barks and formation footsteps** (`troop_voices`, `march_leather`): large design, large sourcing.
- **Hover UI sounds**, per-chapter briefing and after-battle music, a Legendary motif for each of Aldric, Old Horn, Brassjaw and Vael.
- **Sound Physics Remastered** (GPL-3.0): recommend it as an optional separate install for players; never bundle it.

## 7. Open items, risks and questions

**Questions for Kort**
1. **Music.** The five music tracks are deleted in your checkout. Did you remove them because you do not like them? For 1.0, do you want (a) them back until something better exists, (b) no battle or camp music until a commissioned piece, or (c) to commission a small set now?
2. **Money for music.** Is there any budget for a composer, and if so a ceiling? With none, everything stays free and music stays the weakest area.
3. **Rule "no generated audio" (2026-11-04).** Does it still stand, including synthesised effects and AI tools? The proposal assumes yes. Only `ui_deny` is generated today.
4. **Go to download.** May a worker fetch candidate takes (CC0 and the free Sonniss GDC bundle) into `work/` for your listening batches? Nothing goes in the repo until you pick.
5. **Direction.** Is the style guide in 5.1 (the land is breathing, five House palettes, the breath tell) the feel you want? One yes or no per House row is enough.

**Not verified (no ears, no game run)**
- Everything about how sounds feel. Grades are from files, code and numbers.
- What `MusicDirector` does when a music file is missing: it may retry every few seconds and log. Check before the deletion lands in `main`.
- The vanilla streaming-channel limit for four stems plus a bed; sync drift; both need the prototype.
- No House or troop sound has been heard in a running client.
- Web claims marked (excerpt) are from search excerpts. Licence points are not legal advice.

**For other leads**
- Workflow lead: five music `.ogg` deletions move with `bb-kort-maps`; `sounds.json` still names them, so `test_every_variant_file_exists` fails until the files return or the entries go. Whoever lands that needs my step 3.
- Lore lead: N3 (the ebb) wants an audio piece (step 11); `season-framework.md` tells for seasons 2 to 5 are best carried by the breath tell.
- Port: audio code steps wait for the port or go first; content-only steps are safe anytime.

## 8. Review (Sonnet 5.5, high effort)

One reviewer checked the inventory against the repo and the proposal against the lore. It re-measured 10 files, spot-checked about 45 play sites and all 87 table rows. Counts, measurements, play sites and the troop-silencing claim were confirmed. Eight errors were found and fixed in this report:

1. Licence list: 18 files are CC-BY (not one), three of them Kevin MacLeod tracks that can draw Content ID claims. Fixed in section 2; the credit-display question is noted there.
2. Stereo files: six, including `overtime_music_1`. Fixed.
3. Unplayed troop and battle events: seven, not five. Fixed in the grade table.
4. UI clipping: three files (`page_turn_2`, `stamp_seal_1`, `stamp_seal_2`). Fixed.
5. Rarity stings: "not in order", not "inverted". Fixed.
6. Style guide: a violet shimmer on every spell contradicted the lore ("no violet" for the Wildmoot). Fixed in 5.1: the Wildmoot get a breath-and-growl layer.
7. Step 1 now covers all 14 groups above 1.5 LU and the 10 near-peak files, matching the check in 5.2.
8. Commission range corrected to $600 to $8,000 per House piece at 6 to 8 minutes.

Doubtful points also adjusted: Graveborn wording, `ui_back` comparison, the 0.4 s measurement cutoff, the giants' Act II and III status, the season 2 heartbeat (marked as my proposal, not in the lore), and several lines that read as heard (now hedged). Not changed: `AudioCues.java` has a stale comment ("Own synthesised files" above DEFEAT); a trivial cleanup for the code step. Unchecked by the reviewer: web claims in section 4.

## Appendix

### A1. Events (90): where each plays

Vol is the `AudioCues` volume (Minecraft range is about 16 blocks x volume for positional sounds); Cd is the cooldown in ticks per key (the horns also have an 8 s per-commander throttle in `BattleEffects`, not shown); Cap is the most copies per tick. `OFF` = registered with files but no play site.

| Event | Area | Files | Vol | Category / scope | Cd | Cap | Play site |
|---|---|---|---|---|---|---|---|
| `battle_intro` | Battle | 3 | 0.8 | master / LOCAL | 0 |  | CampaignClient.java:142, entrance camera, local |
| `unit_reveal` | Campaign/UI | 3 | 0.7 | players / OWNER | 0 |  | Campaign.java:464, a recruit joins |
| `victory` | Battle | 3 | 0.8 | players / OWNER | 0 |  | Audio.ending (Campaign.java:617), round end, win or draw |
| `defeat` | Battle | 1 | 0.7 | players / OWNER | 0 |  | Audio.ending (Campaign.java:617), losing side; 35.7 s music |
| `capture_start` | Modes | 2 | 0.6 | players / ALL | 200 | 1 | GravesAndGlory.java:1261, point contested |
| `capture_won` | Modes | 2 | 0.6 | players / TEAM | 20 |  | Audio.captured (GravesAndGlory.java:1264, SiegeMode.java:352) |
| `capture_lost` | Modes | 2 | 0.7 | players / TEAM | 20 |  | Audio.captured, other team |
| `gate_hit` | Modes | 4 | 0.5 | hostile / NEARBY | 8 |  | SiegeMode.java:134, ram blow |
| `gate_shut` | Modes | 1 | 0.9 | players / ALL | 0 |  | SiegeMode.java:359, portcullis (46-tick lead) |
| `gate_breach` | Modes | 1 | 0.5 | players / ALL | 0 |  | SiegeMode.java:133, :412 |
| `convoy_roll` | Modes | 1 | 0.4 | players / ALL | 200 |  | ConvoyMode.java:258-259 |
| `convoy_stop` | Modes | 3 | 0.6 | players / ALL | 200 |  | ConvoyMode.java:265 |
| `convoy_rollback` | Modes | 1 | 0.5 | players / ALL | 200 |  | ConvoyMode.java:268 |
| `convoy_checkpoint` | Modes | 2 | 0.6 | players / ALL | 0 |  | ConvoyMode.java:279 |
| `wave_warning` | Modes | 1 | 0.7 | players / ALL | 100 |  | SurvivalMode.java:267 |
| `countdown_tick` | Lobby | 1 | 0.5 | master / OWNER | 0 |  | Audio.countdown, Campaign.java:866 (pad), :1011 (room) |
| `ui_confirm` | UI | 1 | 0.5 | master / LOCAL | 4 |  | UiSounds.confirm/toggled: WarTableLobby ready, WarTableGear:177, :735 |
| `ui_back` | UI | 1 | 0.45 | master / LOCAL | 4 |  | UiSounds.back: WarTableLobby:221, WarTablePages:335, WarTableRecruit:244, WarTableReport:138, WarTableTroop:159 |
| `ui_deny` | UI | 1 | 0.55 | master / LOCAL | 6 |  | UiSounds.deny: ArmouryTab:386, CommandMode:71, UiButton:33, WarTableGear:138, :764 |
| `wave_rise` | Modes | 1 | 0.8 | players / ALL | 100 |  | SurvivalMode.java:269 (30-tick lead) |
| `horn_charge` | Troops/orders | 1 | 4.0 | players / WORLD | 0 |  | BattleEffects.java:54-58, Charge order, world sound 64 blocks |
| `horn_rout` | Troops/orders | 1 | 4.0 | players / WORLD | 0 |  | BattleEffects.java:54-58, Retreat order |
| `horn_rally` | Troops/orders | 1 | 4.0 | players / WORLD | 0 |  | BattleEffects.java:54-58, Muster |
| `combat.shield_block` | Battle | 3 | 0.55 | hostile / NEARBY | 8 | 3 | Audio.playHit (Combat.java:129); Abilities.java:390 shield bash |
| `combat.heavy_metal_hit` | Battle | 1 | 0.5 | hostile / NEARBY | 10 | 2 | Audio.playHit; Abilities.java:420 slam |
| `combat.sword_swing` | Battle | 2 | 0.3 | hostile / NEARBY | 10 | 3 | Audio.swing (BattleEffects.java:81), every 2nd slot |
| `combat.sword_clash` | Battle | 4 | 0.4 | hostile / NEARBY | 8 | 3 | Audio.playHit, blade vs armed |
| `combat.armor_hit` | Battle | 1 | 0.4 | hostile / NEARBY | 8 | 3 | Audio.playHit, blade vs armoured unarmed |
| `combat.bow_shoot` | Battle | 1 | 0.3 | hostile / NEARBY | 12 | 3 | Audio.shot (Tactics.java:600) |
| `combat.arrow_hit` | Battle | 1 | 0.35 | hostile / NEARBY | 6 | 3 | Audio.playHit, missile lands |
| `magic.cast` | Battle | 1 | 0.4 | hostile / NEARBY | 15 | 2 | Audio.shot, caster |
| `magic.impact` | Battle | 1 | 0.5 | hostile / NEARBY | 10 | 2 | Audio.playHit; Abilities.java:235, :968 |
| `clock.tick` | Battle | 2 | 0.5 | players / ALL | 15 |  | Audio.clock (GravesAndGlory.java:851), last 10 s |
| `battle.drums` | Battle | 1 | 0.45 | master / LOCAL | 0 |  | CampaignClient.java:143, under battle_intro |
| `music.calm` | Music | 1 | 0.8 | music / LOCAL | 0 |  | MusicDirector: camp/lobby/War Table, 45 s gap |
| `music.epic` | Music | 1 | 0.8 | music / LOCAL | 0 |  | MusicDirector: once after a won round |
| `music.battle_grim` | Music | 1 | 0.8 | music / LOCAL | 0 |  | MusicDirector: battle, alternating |
| `music.battle_drums` | Music | 1 | 0.8 | music / LOCAL | 0 |  | MusicDirector: battle, alternating |
| `ballista_release` | Troops | 2 | 0.55 | hostile / NEARBY | 12 | 2 | Audio.shotCue ic_ballista |
| `banner_drag` | UI | 2 | 0.4 | master / LOCAL | 6 |  | WarTablePages.java:801 |
| `banner_flap` | Troops/Battle | 2 | 0.3 | hostile / NEARBY | 40 | 2 | **OFF: no play site** |
| `hound_attack` | Troops | 3 | 0.5 | hostile / NEARBY | 10 | 2 | Abilities.java:479 pounce |
| `brute_attack` | Troops | 2 | 0.55 | hostile / NEARBY | 12 | 2 | Abilities.java:464 trample |
| `tusk_attack` | Troops | 1 | 0.55 | hostile / NEARBY | 12 | 2 | Audio.meleeCue, wm_tusk swing |
| `blacksmith_roll` | UI | 2 | 0.5 | master / LOCAL | 10 |  | WarTableGear.java:152 |
| `break_down` | UI | 1 | 0.5 | master / LOCAL | 6 |  | WarTableGear.java:139, :186, :765 |
| `camp_room_tone` | Ambience | 2 | 0.4 | ambient / LOCAL | 0 |  | MusicDirector bed: camp |
| `campaign_briefing_music` | Music | 1 | 0.8 | music / LOCAL | 0 |  | MusicDirector: briefing sheet open, loops |
| `dummy_thud` | Battle | 3 | 0.45 | hostile / NEARBY | 6 | 3 | GravesAndGlory.java:392, structure hit/fell |
| `equip` | UI | 1 | 0.5 | master / LOCAL | 6 |  | WarTableGear.java:755 |
| `firepot_burn` | Troops | 2 | 0.5 | hostile / NEARBY | 15 | 2 | Abilities.java:248 |
| `march_leather` | Troops/Battle | 1 | 0.3 | hostile / NEARBY | 10 | 3 | **OFF: no play site** |
| `forge_marks` | UI | 3 | 0.5 | master / LOCAL | 6 |  | WarTableGear.java:214 |
| `gate_chains` | Modes | 1 | 0.6 | players / ALL | 0 |  | **OFF: no play site** |
| `harpoon_throw` | Troops | 2 | 0.45 | hostile / NEARBY | 12 | 2 | Audio.shotCue tc_harpooner |
| `healer_cast` | Troops | 2 | 0.5 | hostile / NEARBY | 15 | 2 | Statuses.java:260 cleanse |
| `ambience_wind` | Ambience | 2 | 0.5 | ambient / LOCAL | 0 |  | MusicDirector bed: other themes (Kingsfield) |
| `ambience_birds` | Ambience | 1 | 0.5 | ambient / LOCAL | 0 |  | MusicDirector bed: forest theme (Hollowwood) |
| `ambience_water` | Ambience | 1 | 0.5 | ambient / LOCAL | 0 |  | MusicDirector bed: river theme (Rivergate) |
| `ambience_crows` | Ambience | 1 | 0.5 | ambient / LOCAL | 0 |  | MusicDirector bed: fortress theme (Ironhold) |
| `ambience_snow` | Ambience | 1 | 0.5 | ambient / LOCAL | 0 |  | MusicDirector bed: snow theme (Frostmarch) |
| `mortar_fire` | Troops | 1 | 0.6 | hostile / NEARBY | 15 | 2 | Audio.shotCue ic_mortar |
| `overtime_drum_pulse` | Modes | 1 | 0.6 | players / ALL | 140 |  | GravesAndGlory.java:852, every 7 s in Convoy overtime |
| `overtime_music` | Music | 1 | 0.8 | music / LOCAL | 0 |  | MusicDirector: Convoy overtime, loops |
| `page_turn` | UI | 2 | 0.4 | master / LOCAL | 6 |  | WarTableHome.java:211 |
| `rarity_common` | UI | 1 | 0.5 | master / LOCAL | 10 |  | WarTableGear.java:198/210 |
| `rarity_uncommon` | UI | 1 | 0.5 | master / LOCAL | 10 |  | WarTableGear.java:195/210 |
| `rarity_rare` | UI | 1 | 0.55 | master / LOCAL | 10 |  | WarTableGear.java:196/210 |
| `rarity_epic` | UI | 1 | 0.6 | master / LOCAL | 10 |  | WarTableGear.java:197/210 (also Legendary) |
| `relic_use_dark` | Battle | 2 | 0.6 | hostile / NEARBY | 20 | 2 | RelicEffects.java:249 Bloodrush |
| `relic_use_earth` | Battle | 2 | 0.6 | hostile / NEARBY | 20 | 2 | RelicEffects.java:331 Shove |
| `relic_use_fire` | Battle | 2 | 0.6 | hostile / NEARBY | 20 | 2 | RelicEffects.java:377 War Cry |
| `relic_use_ice` | Battle | 2 | 0.6 | hostile / NEARBY | 20 | 2 | RelicEffects.java:293 Dash |
| `repair_hammer` | UI | 1 | 0.5 | master / LOCAL | 10 |  | WarTableGear.java:141 |
| `siege_ram_swing` | Modes | 2 | 0.55 | hostile / NEARBY | 15 | 2 | **OFF: no play site** |
| `soul_leaving` | Troops | 2 | 0.5 | hostile / NEARBY | 20 | 2 | BattleEffects.java:28; SurvivalMode.java:264 |
| `spear_thrust` | Troops | 2 | 0.35 | hostile / NEARBY | 10 | 3 | Audio.meleeCue, spear family |
| `stamp_seal` | UI | 2 | 0.5 | master / LOCAL | 10 |  | WarTableHome.java:530 |
| `survival_wind` | Ambience | 2 | 0.5 | ambient / LOCAL | 0 |  | MusicDirector bed: Survival, any map |
| `troop_voices` | Troops/Battle | 4 | 0.4 | hostile / NEARBY | 40 | 2 | **OFF: no play site** |
| `capture_rising` | Modes | 1 | 0.5 | players / ALL | 100 |  | **OFF: no play site** |
| `coin_clink` | UI | 1 | 0.5 | master / LOCAL | 6 |  | **OFF: no play site** |
| `active_rally` | Troops/Battle | 1 | 0.6 | players / TEAM | 20 |  | **OFF: no play site** |
| `order_hold_shout` | Troops/orders | 1 | 1.0 | players / NEARBY | 20 | 2 | BattleEffects.java:52/63, Hold/Move/formation |
| `order_charge_shout` | Troops/orders | 1 | 1.0 | players / NEARBY | 20 | 2 | BattleEffects.java:52/63 via OrderRules.java:144 |
| `relic_use_light` | Troops/Battle | 1 | 0.6 | hostile / NEARBY | 20 | 2 | **OFF: no play site** |
| `relic_use_lightning` | Troops/Battle | 2 | 0.6 | hostile / NEARBY | 20 | 2 | **OFF: no play site** |
| `title_music` | Music | 2 | 0.8 | music / LOCAL | 0 |  | **OFF: no play site** |
| `veteran_levelup` | Troops/Battle | 1 | 0.5 | hostile / NEARBY | 20 | 2 | **OFF: no play site** |
| `wagon_wheels` | Modes | 1 | 0.4 | players / ALL | 200 |  | **OFF: no play site** |

### A2. Files (143): measurements

Measured with ffmpeg `ebur128=peak=true` (EBU R128). I = integrated loudness (LUFS); files shorter than 0.4 s cannot be gated and show `n/a`. TP = true peak (dBTP). All files are Ogg Vorbis, 44.1 kHz. Flags: CLIP = true peak above 0 dBTP (inter-sample overs, audible crackle risk); HOT = above -1 dBTP; GROUP = loudest or quietest take in a variant group whose takes differ by more than 3 LU; MONO-MUSIC = streamed music in mono; DELETED = deleted by Kort in the main checkout (now in `bb-kort-maps`), open.

| File | Event | s | Ch | I (LUFS) | TP | Stream | Flags |
|---|---|---|---|---|---|---|---|
| active_rally_3 | `active_rally` | 5.0 | 1 | -18.7 | -3.0 |  |  |
| ambience_birds_1 | `ambience_birds` | 60.0 | 1 | -22.7 | -7.1 | yes |  |
| ambience_crows_1 | `ambience_crows` | 34.0 | 1 | -25.1 | -9.5 | yes |  |
| ambience_snow_1 | `ambience_snow` | 60.0 | 1 | -27.7 | -8.4 | yes |  |
| ambience_water_1 | `ambience_water` | 57.0 | 1 | -24.8 | 1.7 | yes | CLIP |
| ambience_wind_1 | `ambience_wind` | 60.0 | 1 | -27.1 | -10.2 | yes |  |
| ambience_wind_2 | `ambience_wind` | 60.0 | 1 | -29.0 | -9.2 | yes |  |
| armor_hit_1 | `combat.armor_hit` | 0.76 | 1 | -16.3 | -0.9 |  | HOT |
| arrow_hit_1 | `combat.arrow_hit` | 0.23 | 1 | n/a | -2.3 |  |  |
| ballista_release_2 | `ballista_release` | 1.0 | 1 | -23.4 | -6.0 |  |  |
| ballista_release_3 | `ballista_release` | 1.0 | 1 | -23.9 | -1.6 |  |  |
| banner_drag_2 | `banner_drag` | 0.3 | 1 | n/a | -8.9 |  |  |
| banner_drag_3 | `banner_drag` | 0.3 | 1 | n/a | -10.1 |  |  |
| banner_flap_1 | `banner_flap` | 5.0 | 1 | -25.5 | -1.6 |  |  |
| banner_flap_2 | `banner_flap` | 5.0 | 1 | -25.6 | -2.4 |  |  |
| battle_drums_1 | `battle.drums` | 6.0 | 1 | -20.8 | -3.7 |  |  |
| battle_intro_1 | `battle_intro` | 6.5 | 1 | -22.5 | -5.5 |  |  |
| battle_intro_2 | `battle_intro` | 6.5 | 1 | -23.5 | -2.8 |  |  |
| battle_intro_3 | `battle_intro` | 6.5 | 1 | -22.4 | -6.3 |  |  |
| blacksmith_roll_1 | `blacksmith_roll` | 2.0 | 1 | -16.0 | -0.6 |  | HOT |
| blacksmith_roll_3 | `blacksmith_roll` | 2.0 | 1 | -17.2 | -0.3 |  | HOT |
| bow_shoot_1 | `combat.bow_shoot` | 0.65 | 1 | -18.2 | -0.1 |  | HOT |
| break_down_2 | `break_down` | 1.0 | 1 | -20.0 | -1.2 |  |  |
| brute_attack_1 | `brute_attack` | 1.0 | 1 | -17.6 | -6.4 |  |  |
| brute_attack_3 | `brute_attack` | 1.0 | 1 | -18.4 | -3.6 |  |  |
| camp_room_tone_1 | `camp_room_tone` | 40.0 | 1 | -28.8 | -2.6 | yes |  |
| camp_room_tone_2 | `camp_room_tone` | 13.0 | 1 | -26.4 | -2.0 | yes |  |
| campaign_briefing_music_2 | `campaign_briefing_music` | 70.0 | 2 | -20.9 | -9.2 | yes | DELETED |
| capture_lost_2 | `capture_lost` | 3.0 | 1 | -22.5 | -9.8 |  |  |
| capture_lost_4 | `capture_lost` | 3.0 | 1 | -23.5 | -12.5 |  |  |
| capture_rising_6 | `capture_rising` | 5.45 | 1 | -17.3 | -2.9 |  |  |
| capture_start_3 | `capture_start` | 1.8 | 1 | -21.8 | -2.9 |  |  |
| capture_start_4 | `capture_start` | 1.8 | 1 | -19.4 | -6.4 |  |  |
| capture_won_1 | `capture_won` | 1.71 | 1 | -22.0 | -5.3 |  |  |
| capture_won_2 | `capture_won` | 1.73 | 1 | -21.3 | -6.6 |  |  |
| clock_tick_1 | `clock.tick` | 0.4 | 1 | n/a | -3.9 |  |  |
| clock_tick_2 | `clock.tick` | 0.4 | 1 | -26.3 | -6.1 |  |  |
| coin_clink_5 | `coin_clink` | 0.31 | 1 | n/a | -1.0 |  |  |
| convoy_checkpoint_1 | `convoy_checkpoint` | 2.99 | 1 | -23.4 | -7.8 |  |  |
| convoy_checkpoint_2 | `convoy_checkpoint` | 4.0 | 1 | -24.8 | -5.5 |  |  |
| convoy_roll_3 | `convoy_roll` | 4.0 | 1 | -26.9 | -8.8 |  |  |
| convoy_rollback_1 | `convoy_rollback` | 1.94 | 1 | -23.4 | -6.3 |  |  |
| convoy_stop_1 | `convoy_stop` | 0.36 | 1 | n/a | -3.4 |  |  |
| convoy_stop_2 | `convoy_stop` | 1.2 | 1 | -26.5 | -5.6 |  |  |
| convoy_stop_3 | `convoy_stop` | 0.4 | 1 | n/a | -5.5 |  |  |
| countdown_tick_2 | `countdown_tick` | 0.55 | 1 | -28.8 | -7.4 |  |  |
| defeat_music_5 | `defeat` | 35.69 | 1 | -20.3 | -2.9 | yes | MONO-MUSIC |
| dummy_thud_1 | `dummy_thud` | 0.4 | 1 | n/a | -0.5 |  | HOT |
| dummy_thud_2 | `dummy_thud` | 0.4 | 1 | n/a | -0.1 |  | HOT |
| dummy_thud_3 | `dummy_thud` | 0.4 | 1 | n/a | -2.3 |  |  |
| equip_1 | `equip` | 0.5 | 1 | -19.5 | -1.9 |  |  |
| firepot_burn_1 | `firepot_burn` | 2.0 | 1 | -25.2 | -7.0 |  |  |
| firepot_burn_2 | `firepot_burn` | 2.0 | 1 | -24.9 | 2.7 |  | CLIP |
| forge_marks_1 | `forge_marks` | 0.8 | 1 | -20.0 | -5.0 |  | GROUP |
| forge_marks_2 | `forge_marks` | 0.8 | 1 | -17.8 | -1.7 |  |  |
| forge_marks_3 | `forge_marks` | 0.8 | 1 | -16.6 | -0.2 |  | HOT GROUP |
| formation_shout_5 | `order_hold_shout` | 0.88 | 1 | -17.3 | -3.6 |  |  |
| formation_shout_7 | `order_charge_shout` | 4.63 | 1 | -15.0 | -2.5 |  |  |
| gate_breach_5 | `gate_breach` | 4.65 | 1 | -17.8 | -2.5 |  |  |
| gate_chains_1 | `gate_chains` | 3.0 | 1 | -19.5 | -3.6 |  |  |
| gate_hit_1 | `gate_hit` | 0.65 | 1 | -30.8 | -12.7 |  | GROUP |
| gate_hit_2 | `gate_hit` | 0.9 | 1 | -28.8 | -10.1 |  |  |
| gate_hit_3 | `gate_hit` | 1.2 | 1 | -27.2 | -8.8 |  |  |
| gate_hit_4 | `gate_hit` | 1.04 | 1 | -25.8 | -5.2 |  | GROUP |
| gate_shut_8 | `gate_shut` | 5.64 | 1 | -16.9 | -3.4 |  |  |
| harpoon_throw_1 | `harpoon_throw` | 0.6 | 1 | -22.8 | -5.0 |  |  |
| harpoon_throw_2 | `harpoon_throw` | 0.5 | 1 | -20.3 | -4.3 |  |  |
| healer_cast_1 | `healer_cast` | 1.0 | 1 | -17.7 | -5.8 |  |  |
| healer_cast_2 | `healer_cast` | 1.0 | 1 | -18.0 | -6.0 |  |  |
| heavy_metal_hit_1 | `combat.heavy_metal_hit` | 1.4 | 1 | -19.6 | -3.8 |  |  |
| horn_charge_3 | `horn_charge` | 2.8 | 1 | -21.0 | -10.9 |  |  |
| horn_rally_2 | `horn_rally` | 4.0 | 1 | -25.9 | -10.2 |  |  |
| horn_rout_2 | `horn_rout` | 3.5 | 1 | -21.8 | -7.0 |  |  |
| hound_attack_1 | `hound_attack` | 1.0 | 1 | -18.6 | -4.4 |  |  |
| hound_attack_2 | `hound_attack` | 1.0 | 1 | -19.2 | -5.8 |  |  |
| hound_attack_3 | `hound_attack` | 1.0 | 1 | -16.8 | -5.3 |  |  |
| magic_cast_1 | `magic.cast` | 1.4 | 1 | -20.7 | -7.0 |  |  |
| magic_impact_1 | `magic.impact` | 1.4 | 1 | -19.3 | -6.3 |  |  |
| march_leather_1 | `march_leather` | 3.0 | 1 | -22.9 | -4.9 |  |  |
| mortar_fire_2 | `mortar_fire` | 1.5 | 1 | -19.4 | -1.7 |  |  |
| music_battle_drums_1 | `music.battle_drums` | 91.54 | 2 | -21.1 | -10.0 | yes | DELETED |
| music_battle_grim_1 | `music.battle_grim` | 73.44 | 2 | -21.1 | -4.7 | yes | DELETED |
| music_calm_1 | `music.calm` | 185.8 | 2 | -21.1 | -2.4 | yes | DELETED |
| music_epic_1 | `music.epic` | 52.55 | 2 | -21.1 | -7.1 | yes |  |
| overtime_drum_pulse_2 | `overtime_drum_pulse` | 7.3 | 1 | -19.3 | -1.6 |  |  |
| overtime_music_1 | `overtime_music` | 60.0 | 2 | -20.9 | -7.6 | yes | DELETED |
| page_turn_1 | `page_turn` | 0.4 | 1 | n/a | -1.0 |  |  |
| page_turn_2 | `page_turn` | 0.5 | 1 | -19.1 | 0.2 |  | CLIP |
| rarity_common_1 | `rarity_common` | 0.5 | 1 | -22.0 | -6.1 |  |  |
| rarity_epic_2 | `rarity_epic` | 2.2 | 1 | -19.2 | -5.0 |  |  |
| rarity_rare_2 | `rarity_rare` | 1.5 | 1 | -18.1 | -1.1 |  |  |
| rarity_uncommon_2 | `rarity_uncommon` | 1.0 | 1 | -15.5 | -4.0 |  |  |
| relic_use_dark_1 | `relic_use_dark` | 1.0 | 1 | -23.0 | -7.5 |  |  |
| relic_use_dark_2 | `relic_use_dark` | 1.0 | 1 | -23.3 | -7.4 |  |  |
| relic_use_earth_1 | `relic_use_earth` | 1.0 | 1 | -22.7 | -4.0 |  |  |
| relic_use_earth_2 | `relic_use_earth` | 1.0 | 1 | -21.4 | -4.1 |  |  |
| relic_use_fire_1 | `relic_use_fire` | 1.0 | 1 | -20.7 | -4.9 |  | GROUP |
| relic_use_fire_2 | `relic_use_fire` | 1.0 | 1 | -25.8 | -6.1 |  | GROUP |
| relic_use_ice_1 | `relic_use_ice` | 1.0 | 1 | -15.3 | 2.0 |  | CLIP |
| relic_use_ice_2 | `relic_use_ice` | 1.0 | 1 | -16.3 | 0.5 |  | CLIP |
| relic_use_light_5 | `relic_use_light` | 4.0 | 1 | -17.3 | -3.0 |  |  |
| relic_use_lightning_3 | `relic_use_lightning` | 4.82 | 1 | -18.5 | -1.4 |  | GROUP |
| relic_use_lightning_5 | `relic_use_lightning` | 4.19 | 1 | -13.7 | -2.2 |  | GROUP |
| repair_hammer_3 | `repair_hammer` | 1.0 | 1 | -16.1 | -1.7 |  |  |
| shield_block_1 | `combat.shield_block` | 0.7 | 1 | -20.3 | -1.2 |  |  |
| shield_block_2 | `combat.shield_block` | 0.7 | 1 | -19.8 | -2.8 |  |  |
| shield_block_3 | `combat.shield_block` | 0.7 | 1 | -19.5 | -2.5 |  |  |
| siege_ram_swing_2 | `siege_ram_swing` | 1.0 | 1 | -18.6 | -2.3 |  |  |
| siege_ram_swing_3 | `siege_ram_swing` | 1.0 | 1 | -18.8 | -3.5 |  |  |
| soul_leaving_1 | `soul_leaving` | 1.5 | 1 | -18.8 | -6.8 |  |  |
| soul_leaving_2 | `soul_leaving` | 1.5 | 1 | -19.2 | -3.0 |  |  |
| spear_thrust_1 | `spear_thrust` | 0.5 | 1 | -16.7 | -0.8 |  | HOT |
| spear_thrust_2 | `spear_thrust` | 0.5 | 1 | -18.0 | -2.0 |  |  |
| stamp_seal_1 | `stamp_seal` | 0.5 | 1 | -19.8 | 0.2 |  | CLIP |
| stamp_seal_2 | `stamp_seal` | 0.5 | 1 | -19.6 | 0.1 |  | CLIP |
| survival_wind_1 | `survival_wind` | 60.0 | 1 | -27.1 | -11.3 | yes |  |
| survival_wind_2 | `survival_wind` | 14.0 | 1 | -27.3 | -8.0 | yes |  |
| sword_clash_1 | `combat.sword_clash` | 0.8 | 1 | -15.4 | 1.7 |  | CLIP GROUP |
| sword_clash_2 | `combat.sword_clash` | 0.9 | 1 | -16.8 | -0.5 |  | HOT |
| sword_clash_3 | `combat.sword_clash` | 0.9 | 1 | -16.2 | 0.2 |  | CLIP |
| sword_clash_4 | `combat.sword_clash` | 0.9 | 1 | -19.7 | -1.9 |  | GROUP |
| sword_swing_1 | `combat.sword_swing` | 0.8 | 1 | -18.1 | -0.6 |  | HOT |
| sword_swing_2 | `combat.sword_swing` | 0.8 | 1 | -16.4 | -2.1 |  |  |
| title_music_3 | `title_music` | 100.0 | 1 | -20.5 | -2.9 | yes | MONO-MUSIC |
| title_music_6 | `title_music` | 62.63 | 1 | -22.2 | -2.8 | yes | MONO-MUSIC |
| troop_voices_1 | `troop_voices` | 1.0 | 1 | -19.9 | -9.9 |  |  |
| troop_voices_2 | `troop_voices` | 0.75 | 1 | -19.0 | -5.7 |  |  |
| troop_voices_3 | `troop_voices` | 0.95 | 1 | -18.2 | -5.5 |  |  |
| troop_voices_4 | `troop_voices` | 0.49 | 1 | -19.5 | -9.4 |  |  |
| tusk_attack_2 | `tusk_attack` | 1.0 | 1 | -20.3 | -8.6 |  |  |
| ui_back_7 | `ui_back` | 0.48 | 1 | -29.8 | -6.3 |  |  |
| ui_confirm_2 | `ui_confirm` | 0.5 | 1 | -23.2 | -9.6 |  |  |
| ui_deny | `ui_deny` | 0.34 | 1 | n/a | -4.3 |  |  |
| unit_reveal_1 | `unit_reveal` | 2.5 | 1 | -21.3 | -8.8 |  | GROUP |
| unit_reveal_2 | `unit_reveal` | 2.5 | 1 | -25.4 | -6.6 |  | GROUP |
| unit_reveal_3 | `unit_reveal` | 1.85 | 1 | -23.9 | -4.5 |  |  |
| veteran_levelup_5 | `veteran_levelup` | 0.78 | 1 | -19.5 | -3.1 |  |  |
| victory_1 | `victory` | 4.19 | 1 | -21.3 | -5.8 |  |  |
| victory_2 | `victory` | 3.02 | 1 | -21.4 | -5.4 |  |  |
| victory_3 | `victory` | 4.12 | 1 | -20.6 | -8.3 |  |  |
| wagon_wheels_6 | `wagon_wheels` | 7.97 | 1 | -25.4 | -2.8 |  |  |
| wave_rise_4 | `wave_rise` | 4.26 | 1 | -21.4 | -7.4 |  |  |
| wave_warning_1 | `wave_warning` | 4.0 | 1 | -21.1 | -7.3 |  |  |
