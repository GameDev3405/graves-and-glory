package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/** The client's UI cues (plan 4F.4): confirm, back and deny, from the AudioCues table, heard by the local player only. Client thread only. */
public final class UiSounds {
    private UiSounds() {}

    private static final AudioCues.Throttle THROTTLE = new AudioCues.Throttle();

    public static void confirm() { play(AudioCues.Cue.UI_CONFIRM); }
    public static void back() { play(AudioCues.Cue.UI_BACK); }
    public static void deny() { play(AudioCues.Cue.UI_DENY); }

    /** A press that turns something on confirms; one that turns it off steps back. */
    public static void toggled(boolean on) { if (on) confirm(); else back(); }

    static void play(AudioCues.Cue cue) {
        var client = MinecraftClient.getInstance();
        if (client == null || !THROTTLE.allow(cue, "ui", System.currentTimeMillis() / 50)) return;
        var event = Registries.SOUND_EVENT.get(new Identifier("gravesandglory", cue.id));
        if (event != null) client.getSoundManager().play(PositionedSoundInstance.master(event, cue.pitch, cue.volume));
    }
}
