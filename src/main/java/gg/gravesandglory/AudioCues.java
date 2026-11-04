package gg.gravesandglory;

import java.util.*;

/**
 * The mod's sound table (plan 4F.4): every cue's registered event id, what it plays, its volume and pitch, its sound category, who hears it and
 * how often it may repeat. Pure data with no Minecraft types, so the standalone checks read it; Audio plays it, GravesAndGlory.onInitialize
 * registers it. A cue with a vanilla remap plays that vanilla event through sounds.json ("type": "event"), so no audio file is added; a cue without
 * one (every cue now) has one or more .ogg files under assets/gravesandglory/sounds (several = a random variant group; music is streamed). Change a volume or pitch here and nowhere else (the horns' volume is the sheet's
 * combatDiscipline.hornVolume; BattleEffects reads it).
 */
public final class AudioCues {
    private AudioCues() {}

    /** Who hears a cue. */
    public enum Scope {
        /** One player's own connection (no one else hears it). */
        OWNER,
        /** The seated players of one team. */
        TEAM,
        /** Every seated player of the round. */
        ALL,
        /** Everyone near a place in the world. */
        NEARBY,
        /** Client side only: the local player, from a screen or a received packet. */
        LOCAL
    }

    public enum Cue {
        // Own audio files, kept as they were.
        BATTLE_INTRO("battle_intro", null, .8f, 1f, "master", Scope.LOCAL, 0, "Battle horns sound"),
        UNIT_REVEAL("unit_reveal", null, .7f, 1f, "players", Scope.OWNER, 0, "A new unit joins your banner"),
        VICTORY("victory", null, .8f, 1f, "players", Scope.OWNER, 0, "Battle ends"),
        // Own synthesised files (tools/audio/make_cues.py); see docs/AUDIO.md.
        DEFEAT("defeat", null, .7f, 1f, "players", Scope.OWNER, 0, "Battle lost"),
        CAPTURE_START("capture_start", null, .6f, 1f, "players", Scope.ALL, 200, "A point is contested", 1),
        CAPTURE_WON("capture_won", null, .6f, 1f, "players", Scope.TEAM, 20, "Your side takes a point"),
        CAPTURE_LOST("capture_lost", null, .7f, 1f, "players", Scope.TEAM, 20, "Your side loses a point"),
        GATE_HIT("gate_hit", null, .5f, 1f, "hostile", Scope.NEARBY, 8, "The gate is struck"),
        GATE_SHUT("gate_shut", null, .9f, 1f, "players", Scope.ALL, 0, "The gate shuts"),
        GATE_BREACH("gate_breach", null, .5f, 1f, "players", Scope.ALL, 0, "The gate is breached"),
        CONVOY_ROLL("convoy_roll", null, .4f, 1f, "players", Scope.ALL, 200, "The wagon rolls"),
        CONVOY_STOP("convoy_stop", null, .6f, 1f, "players", Scope.ALL, 200, "The wagon stops"),
        CONVOY_ROLLBACK("convoy_rollback", null, .5f, 1f, "players", Scope.ALL, 200, "The wagon rolls back"),
        CONVOY_CHECKPOINT("convoy_checkpoint", null, .6f, 1f, "players", Scope.ALL, 0, "The convoy reaches a checkpoint"),
        WAVE_WARNING("wave_warning", null, .7f, 1f, "players", Scope.ALL, 100, "A wave approaches"),
        WAVE_RISE("wave_rise", null, .8f, 1f, "players", Scope.ALL, 100, "A wave rises"),
        COUNTDOWN_TICK("countdown_tick", null, .5f, 1f, "master", Scope.OWNER, 0, ""),
        UI_CONFIRM("ui_confirm", null, .5f, 1f, "master", Scope.LOCAL, 4, ""),
        UI_BACK("ui_back", null, .45f, 1f, "master", Scope.LOCAL, 4, ""),
        UI_DENY("ui_deny", null, .55f, 1f, "master", Scope.LOCAL, 6, ""),
        // Third-party Pixabay recordings (tools/audio/import_pixabay.py, credited in docs/AUDIO.md), each a variant group the game picks from at random.
        // Combat cues are positional (NEARBY), keyed per soldier and capped per tick so thirty troops fighting stay a murmur, not a wall of noise.
        SHIELD_BLOCK("combat.shield_block", null, .55f, 1f, "hostile", Scope.NEARBY, 8, "A shield blocks", 3),
        HEAVY_METAL_HIT("combat.heavy_metal_hit", null, .5f, 1f, "hostile", Scope.NEARBY, 10, "A heavy blow lands", 2),
        SWORD_SWING("combat.sword_swing", null, .3f, 1f, "hostile", Scope.NEARBY, 10, "A blade swings", 3),
        SWORD_CLASH("combat.sword_clash", null, .4f, 1f, "hostile", Scope.NEARBY, 8, "Swords clash", 3),
        ARMOR_HIT("combat.armor_hit", null, .4f, 1f, "hostile", Scope.NEARBY, 8, "Armour is struck", 3),
        BOW_SHOOT("combat.bow_shoot", null, .3f, 1f, "hostile", Scope.NEARBY, 12, "A bow is loosed", 3),
        ARROW_HIT("combat.arrow_hit", null, .35f, 1f, "hostile", Scope.NEARBY, 6, "An arrow strikes", 3),
        MAGIC_CAST("magic.cast", null, .4f, 1f, "hostile", Scope.NEARBY, 15, "A spell is cast", 2),
        MAGIC_IMPACT("magic.impact", null, .5f, 1f, "hostile", Scope.NEARBY, 10, "A spell strikes", 2),
        // The last ten seconds of a round's clock: one tick a second to the seated players.
        CLOCK_TICK("clock.tick", null, .5f, 1f, "players", Scope.ALL, 15, "The clock ticks"),
        // War drums under the battle entrance camera, beside battle_intro (client only).
        BATTLE_DRUMS("battle.drums", null, .45f, 1f, "master", Scope.LOCAL, 0, "War drums beat"),
        // Event hooks added with the audio/hooks pass: every one is a variant group of its own files (see docs/AUDIO.md for where each plays).
        HOUND_ATTACK("hound_attack", null, .5f, 1f, "hostile", Scope.NEARBY, 10, "A hound lunges", 2),
        BRUTE_ATTACK("brute_attack", null, .55f, 1f, "hostile", Scope.NEARBY, 12, "A brute charges", 2),
        TUSK_ATTACK("tusk_attack", null, .55f, 1f, "hostile", Scope.NEARBY, 12, "A tusked beast gores", 2),
        SPEAR_THRUST("spear_thrust", null, .35f, 1f, "hostile", Scope.NEARBY, 10, "A spear thrusts", 3),
        BALLISTA_RELEASE("ballista_release", null, .55f, 1f, "hostile", Scope.NEARBY, 12, "A ballista fires", 2),
        MORTAR_FIRE("mortar_fire", null, .6f, 1f, "hostile", Scope.NEARBY, 15, "A mortar fires", 2),
        HARPOON_THROW("harpoon_throw", null, .45f, 1f, "hostile", Scope.NEARBY, 12, "A harpoon is thrown", 2),
        DUMMY_THUD("dummy_thud", null, .45f, 1f, "hostile", Scope.NEARBY, 6, "A dummy is struck", 3),
        FIREPOT_BURN("firepot_burn", null, .5f, 1f, "hostile", Scope.NEARBY, 15, "A firepot burns", 2),
        HEALER_CAST("healer_cast", null, .5f, 1f, "hostile", Scope.NEARBY, 15, "A healing spell", 2),
        SOUL_LEAVING("soul_leaving", null, .5f, 1f, "hostile", Scope.NEARBY, 20, "A soul departs", 2),
        RELIC_USE_DARK("relic_use_dark", null, .6f, 1f, "hostile", Scope.NEARBY, 20, "A dark relic stirs", 2),
        RELIC_USE_EARTH("relic_use_earth", null, .6f, 1f, "hostile", Scope.NEARBY, 20, "An earthen relic stirs", 2),
        RELIC_USE_FIRE("relic_use_fire", null, .6f, 1f, "hostile", Scope.NEARBY, 20, "A fiery relic flares", 2),
        RELIC_USE_ICE("relic_use_ice", null, .6f, 1f, "hostile", Scope.NEARBY, 20, "A frozen relic chills", 2),
        // Server cue to every seated player: the convoy's overtime beat.
        OVERTIME_DRUM_PULSE("overtime_drum_pulse", null, .6f, 1f, "players", Scope.ALL, 140, "Overtime drums beat"),
        // Client only, from the War Table screens (UiSounds.play): master category, no spatial position.
        BANNER_DRAG("banner_drag", null, .4f, 1f, "master", Scope.LOCAL, 6, "A banner is dragged"),
        PAGE_TURN("page_turn", null, .4f, 1f, "master", Scope.LOCAL, 6, "A page turns"),
        STAMP_SEAL("stamp_seal", null, .5f, 1f, "master", Scope.LOCAL, 10, "A seal is stamped"),
        EQUIP("equip", null, .5f, 1f, "master", Scope.LOCAL, 6, "Gear is equipped"),
        BREAK_DOWN("break_down", null, .5f, 1f, "master", Scope.LOCAL, 6, "Gear is broken down"),
        BLACKSMITH_ROLL("blacksmith_roll", null, .5f, 1f, "master", Scope.LOCAL, 10, "The forge rolls"),
        REPAIR_HAMMER("repair_hammer", null, .5f, 1f, "master", Scope.LOCAL, 10, "Gear is repaired"),
        FORGE_MARKS("forge_marks", null, .5f, 1f, "master", Scope.LOCAL, 6, "Forge marks are earned"),
        RARITY_COMMON("rarity_common", null, .5f, 1f, "master", Scope.LOCAL, 10, "A common find"),
        RARITY_UNCOMMON("rarity_uncommon", null, .5f, 1f, "master", Scope.LOCAL, 10, "An uncommon find"),
        RARITY_RARE("rarity_rare", null, .55f, 1f, "master", Scope.LOCAL, 10, "A rare find"),
        RARITY_EPIC("rarity_epic", null, .6f, 1f, "master", Scope.LOCAL, 10, "An epic find"),
        // Registered, not yet played (no clear game moment; docs/AUDIO.md lists why): they have files and events so a later hook is one line.
        SIEGE_RAM_SWING("siege_ram_swing", null, .55f, 1f, "hostile", Scope.NEARBY, 15, "A ram swings", 2),
        GATE_CHAINS("gate_chains", null, .6f, 1f, "players", Scope.ALL, 0, "Gate chains rattle"),
        BANNER_FLAP("banner_flap", null, .3f, 1f, "hostile", Scope.NEARBY, 40, "A banner flaps", 2),
        MARCH_LEATHER("march_leather", null, .3f, 1f, "hostile", Scope.NEARBY, 10, "Leather boots march", 3),
        TROOP_VOICES("troop_voices", null, .4f, 1f, "hostile", Scope.NEARBY, 40, "Troops call out", 2),
        // Streamed loops and music not yet played (MusicDirector carries one track at a time): music and ambient categories, no subtitle.
        AMBIENCE_WIND("ambience_wind", null, .5f, 1f, "ambient", Scope.LOCAL, 0, ""),
        AMBIENCE_BIRDS("ambience_birds", null, .5f, 1f, "ambient", Scope.LOCAL, 0, ""),
        AMBIENCE_WATER("ambience_water", null, .5f, 1f, "ambient", Scope.LOCAL, 0, ""),
        AMBIENCE_CROWS("ambience_crows", null, .5f, 1f, "ambient", Scope.LOCAL, 0, ""),
        AMBIENCE_SNOW("ambience_snow", null, .5f, 1f, "ambient", Scope.LOCAL, 0, ""),
        CAMP_ROOM_TONE("camp_room_tone", null, .4f, 1f, "ambient", Scope.LOCAL, 0, ""),
        SURVIVAL_WIND("survival_wind", null, .5f, 1f, "ambient", Scope.LOCAL, 0, ""),
        CAMPAIGN_BRIEFING_MUSIC("campaign_briefing_music", null, .8f, 1f, "music", Scope.LOCAL, 0, ""),
        OVERTIME_MUSIC("overtime_music", null, .8f, 1f, "music", Scope.LOCAL, 0, ""),
        // Streamed music (client only, MusicDirector): the category is music, so the player's Music slider governs it and no subtitle shows.
        MUSIC_CALM("music.calm", null, .8f, 1f, "music", Scope.LOCAL, 0, ""),
        MUSIC_EPIC("music.epic", null, .8f, 1f, "music", Scope.LOCAL, 0, ""),
        MUSIC_BATTLE_GRIM("music.battle_grim", null, .8f, 1f, "music", Scope.LOCAL, 0, ""),
        MUSIC_BATTLE_DRUMS("music.battle_drums", null, .8f, 1f, "music", Scope.LOCAL, 0, "");

        /** The registered event's path under gravesandglory. */
        public final String id;
        /** The vanilla event it plays (namespaced), or null when it has its own file. */
        public final String remap;
        public final float volume, pitch;
        /** A net.minecraft.sound.SoundCategory name, lower case. */
        public final String category;
        public final Scope scope;
        /** Ticks that must pass before the same cue with the same key plays again; 0 means no limit (a one-off event). */
        public final int cooldownTicks;
        /** Its subtitle, or empty for none (UI and countdown cues show none, like the vanilla button click). */
        public final String subtitle;
        /** Most copies of this cue that may start in one tick across all keys (a battle of thirty troops); 0 means no cap. A capped cue also gets a small random pitch spread. */
        public final int maxPerTick;

        Cue(String id, String remap, float volume, float pitch, String category, Scope scope, int cooldownTicks, String subtitle) {
            this(id, remap, volume, pitch, category, scope, cooldownTicks, subtitle, 0);
        }

        Cue(String id, String remap, float volume, float pitch, String category, Scope scope, int cooldownTicks, String subtitle, int maxPerTick) {
            this.id = id; this.remap = remap; this.volume = volume; this.pitch = pitch; this.category = category;
            this.scope = scope; this.cooldownTicks = cooldownTicks; this.subtitle = subtitle; this.maxPerTick = maxPerTick;
        }

        public boolean music() { return category.equals("music"); }
    }


    // ---- Timing (docs/reports/audio-cue-timing-2026-11-02.md) ----------------------------------------------------------------------------------------
    // A melee blow that resolves on the tick its swing starts (a troop without attack variants) waits for the blade to meet its target. A blow from
    // Tactics.swing already lands on its clip's impact frame and waits nothing (Audio.contact). The ticks below are the Houses' old clip strikes (x20).
    /** Ticks from a swing's start to the blade meeting its target, by House id. */
    public static final Map<String, Integer> CONTACT_TICKS = Map.of("gilded", 4, "graveborn", 6, "wildmoot", 5, "ironclad", 7, "tidecallers", 6);
    /** The contact tick of a melee troop with no House attack of its own. */
    public static final int DEFAULT_CONTACT_TICKS = 5;

    /** Ticks between a melee swing starting and its hit sound for a troop of this House (null or unknown: the default). */
    public static int contactTicks(String house) { return house == null ? DEFAULT_CONTACT_TICKS : CONTACT_TICKS.getOrDefault(house, DEFAULT_CONTACT_TICKS); }

    /** How long the victory and defeat stings run, in ticks (3.2 s, inside the three victory takes' range; 5 s): the music waits them out so the next track never starts over the sting. */
    public static final int VICTORY_STING_TICKS = 64, DEFEAT_STING_TICKS = 100;
    /** The portcullis drops this many ticks after the gate_shut cue starts, so the crash lands as the gate meets the ground (the file's thud is 0.35 s in). */
    public static final int GATE_SHUT_LEAD_TICKS = 7;
    /** The wave rises this many ticks after the wave_rise cue starts (the file swells for 1.5 s before its hit). */
    public static final int WAVE_RISE_LEAD_TICKS = 30;

    /** Whether the round clock, with this many ticks left, ticks now: once a second through the last ten seconds (ticks 200, 180 ... 20), never at 0. */
    public static boolean clockSecond(long ticksLeft) { return ticksLeft > 0 && ticksLeft <= 200 && ticksLeft % 20 == 0; }

    /** Whether a pre-battle countdown with this many ticks left ticks now: once a second through the last three seconds (60, 40, 20). */
    public static boolean countdownSecond(long ticksLeft) { return ticksLeft > 0 && ticksLeft <= 60 && ticksLeft % 20 == 0; }

    /**
     * Cues that wait for their moment (a melee blow's contact frame, the gate's crash, a wave's rise): add(now, delay, play) runs play after delay ticks, at once
     * when the delay is not positive; tick(now) runs what has come due, in the order added. An entry due further ahead than any delay (a clock that went back,
     * a new round) is dropped; clear() drops all.
     */
    public static final class Schedule {
        public static final int MAX_DELAY = 400;
        private final List<long[]> due = new ArrayList<>();
        private final List<Runnable> plays = new ArrayList<>();

        public void add(long now, int delayTicks, Runnable play) {
            if (delayTicks <= 0) { play.run(); return; }
            due.add(new long[] {now + delayTicks});
            plays.add(play);
        }

        public void tick(long now) {
            for (int i = 0; i < due.size(); ) {
                long at = due.get(i)[0];
                if (at > now + MAX_DELAY) { due.remove(i); plays.remove(i); }
                else if (at <= now) { due.remove(i); var play = plays.remove(i); play.run(); }
                else i++;
            }
        }

        public int size() { return due.size(); }

        public void clear() { due.clear(); plays.clear(); }
    }

    /** The command horns: own files, volume from combatDiscipline.hornVolume, throttled by BattleEffects.feedback, to the commanding player only. */
    public static final List<String> HORNS = List.of("horn_charge", "horn_rout", "horn_rally");

    /** Every event id the mod registers, in order. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (Cue c : Cue.values()) out.add(c.id);
        out.addAll(HORNS);
        return out;
    }

    /**
     * One sliding limit per (cue, key) plus a cap on copies per tick for the cues that have one: allow answers whether the cue may play now and, when it
     * may, records it. A refused call records nothing. A clock that went back (a new round) allows.
     */
    public static final class Throttle {
        private final Map<String, Long> last = new HashMap<>();
        private final Map<String, long[]> tick = new HashMap<>();

        public boolean allow(Cue cue, String key, long now) {
            String k = cue.id + "/" + key;
            if (cue.cooldownTicks > 0) {
                Long before = last.get(k);
                if (before != null && now >= before && now - before < cue.cooldownTicks) return false;
            }
            if (cue.maxPerTick > 0) {
                long[] t = tick.computeIfAbsent(cue.id, x -> new long[] {Long.MIN_VALUE, 0});
                if (t[0] != now) { t[0] = now; t[1] = 0; }
                if (t[1] >= cue.maxPerTick) return false;
                t[1]++;
            }
            if (cue.cooldownTicks > 0) last.put(k, now);
            return true;
        }

        public void clear() { last.clear(); tick.clear(); }
    }
}
