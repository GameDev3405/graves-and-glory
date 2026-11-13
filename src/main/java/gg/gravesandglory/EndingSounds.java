package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The match ending's sounds (ending polish spec, section 3): the cues are played by their AudioCues name, and a cue that does not exist yet
 * (the audio lead adds ENDING_DRAW, PODIUM_DRUM, PODIUM_REVEAL, REWARD_TICK, REWARD_TOTAL, LEVEL_UP and AWARD on its own branch) plays nothing,
 * so its landing switches them on without touching this file. Local to this client, at the master category. Client thread only.
 */
final class EndingSounds {
    private EndingSounds() {}

    /** The fewest client ticks between two reward ticks while the rows count up. */
    static final int TICK_GAP = 3;
    static long lastTick = Long.MIN_VALUE / 2;
    /** Harness only: the cue names asked for, in order, and whether each existed ("VICTORY", "-REWARD_TICK" for a missing one). */
    static final List<String> asked = new ArrayList<>();

    /** Plays the cue named, or nothing when no such cue (or no such sound event) exists; true when it played. */
    static boolean play(String name) {
        var cue = MatchEnding.cue(name);
        var event = cue == null ? null : Registries.SOUND_EVENT.get(new Identifier("gravesandglory", cue.id));
        record(event == null ? "-" + name : name);
        if (event == null) return false;
        var c = MinecraftClient.getInstance();
        if (c == null) return false;
        c.getSoundManager().play(PositionedSoundInstance.master(event, cue.pitch, cue.volume));
        return true;
    }
    static void record(String s) { if (asked.size() > 200) asked.remove(0); asked.add(s); }

    /** The banner's sting: VICTORY or DEFEAT, and for a draw ENDING_DRAW (VICTORY until that cue exists, as before the polish). */
    static void banner(String outcome) {
        switch (outcome) {
            case "VICTORY" -> play("VICTORY");
            case "DEFEAT" -> play("DEFEAT");
            default -> { if (!play("ENDING_DRAW")) play("VICTORY"); }
        }
    }
    static void award() { play("AWARD"); }
    /** One reward tick while the rows count up, at most one every TICK_GAP ticks of the given clock. */
    static void rewardTick(long now) {
        if (now - lastTick < TICK_GAP) return;
        lastTick = now; play("REWARD_TICK");
    }
    static void rewardTotal() { play("REWARD_TOTAL"); }
}
