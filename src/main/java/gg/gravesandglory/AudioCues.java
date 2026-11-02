package gg.gravesandglory;

import java.util.*;

/**
 * The mod's sound table (plan 4F.4): every cue's registered event id, what it plays, its volume and pitch, its sound category, who hears it and
 * how often it may repeat. Pure data with no Minecraft types, so the standalone checks read it; Audio plays it, GravesAndGlory.onInitialize
 * registers it. A cue with a vanilla remap plays that vanilla event through sounds.json ("type": "event"), so no audio file is added; a cue without
 * one has its own .ogg under assets/gravesandglory/sounds. Change a volume or pitch here and nowhere else (the horns' volume is the sheet's
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
        // Vanilla remaps.
        DEFEAT("defeat", "minecraft:block.respawn_anchor.deplete", .7f, .75f, "players", Scope.OWNER, 0, "Battle lost"),
        CAPTURE_START("capture_start", "minecraft:item.armor.equip_chain", .6f, 1f, "players", Scope.ALL, 200, "A point is contested"),
        CAPTURE_WON("capture_won", "minecraft:block.beacon.activate", .6f, 1.3f, "players", Scope.TEAM, 20, "Your side takes a point"),
        CAPTURE_LOST("capture_lost", "minecraft:block.beacon.deactivate", .7f, .9f, "players", Scope.TEAM, 20, "Your side loses a point"),
        GATE_HIT("gate_hit", "minecraft:block.anvil.hit", .5f, .55f, "hostile", Scope.NEARBY, 8, "The gate is struck"),
        GATE_SHUT("gate_shut", "minecraft:block.iron_trapdoor.close", .9f, .6f, "players", Scope.ALL, 0, "The gate shuts"),
        GATE_BREACH("gate_breach", "minecraft:entity.generic.explode", .5f, .6f, "players", Scope.ALL, 0, "The gate is breached"),
        CONVOY_ROLL("convoy_roll", "minecraft:entity.minecart.riding", .4f, .8f, "players", Scope.ALL, 200, "The wagon rolls"),
        CONVOY_STOP("convoy_stop", "minecraft:block.wooden_trapdoor.close", .6f, .7f, "players", Scope.ALL, 200, "The wagon stops"),
        CONVOY_ROLLBACK("convoy_rollback", "minecraft:block.piston.contract", .5f, .7f, "players", Scope.ALL, 200, "The wagon rolls back"),
        CONVOY_CHECKPOINT("convoy_checkpoint", "minecraft:block.bell.use", .6f, 1.2f, "players", Scope.ALL, 0, "The convoy reaches a checkpoint"),
        WAVE_WARNING("wave_warning", "minecraft:event.raid.horn", .7f, 1f, "players", Scope.ALL, 100, "A wave approaches"),
        COUNTDOWN_TICK("countdown_tick", "minecraft:block.note_block.bit", .5f, 1f, "master", Scope.OWNER, 0, ""),
        UI_CONFIRM("ui_confirm", "minecraft:block.note_block.chime", .5f, 1.3f, "master", Scope.LOCAL, 4, ""),
        UI_BACK("ui_back", "minecraft:ui.button.click", .5f, .8f, "master", Scope.LOCAL, 4, ""),
        UI_DENY("ui_deny", "minecraft:block.note_block.bass", .6f, .6f, "master", Scope.LOCAL, 6, "");

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

        Cue(String id, String remap, float volume, float pitch, String category, Scope scope, int cooldownTicks, String subtitle) {
            this.id = id; this.remap = remap; this.volume = volume; this.pitch = pitch; this.category = category;
            this.scope = scope; this.cooldownTicks = cooldownTicks; this.subtitle = subtitle;
        }
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

    /** One sliding limit per (cue, key): allow answers whether the cue may play now and, when it may, records it. A clock that went back (a new round) allows. */
    public static final class Throttle {
        private final Map<String, Long> last = new HashMap<>();

        public boolean allow(Cue cue, String key, long now) {
            if (cue.cooldownTicks <= 0) return true;
            String k = cue.id + "/" + key;
            Long before = last.get(k);
            if (before != null && now >= before && now - before < cue.cooldownTicks) return false;
            last.put(k, now);
            return true;
        }

        public void clear() { last.clear(); }
    }
}
