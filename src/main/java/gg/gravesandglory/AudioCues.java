package gg.gravesandglory;

import java.util.*;

/**
 * The mod's sound table (plan 4F.4): every cue's registered event id, what it plays, its volume and pitch, its sound category, who hears it and
 * how often it may repeat. Pure data with no Minecraft types, so the standalone checks read it; Audio plays it, GravesAndGlory.onInitialize
 * registers it. A cue with a vanilla remap plays that vanilla event through sounds.json ("type": "event"), so no audio file is added; a cue without
 * one (every cue now) has its own .ogg under assets/gravesandglory/sounds. Change a volume or pitch here and nowhere else (the horns' volume is the sheet's
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
        CAPTURE_START("capture_start", null, .6f, 1f, "players", Scope.ALL, 200, "A point is contested"),
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
