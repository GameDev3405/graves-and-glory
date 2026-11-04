package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

/**
 * Picks the mod's streamed music (AudioCues MUSIC_*) on a Graves and Glory server and keeps vanilla music quiet while it plays. Client thread only.
 * <ul>
 * <li>In the Banner Camp (lobby, War Table): the calm track, with a pause between plays.</li>
 * <li>After a round the local side won (outcome VICTORY): the epic track once, then back to calm.</li>
 * <li>In a running battle: the two battle tracks in turn, the first chosen by the round's serial; silent through the entrance camera, which has the horns.</li>
 * </ul>
 * The tracks are in the music category, so the player's Music slider governs them. Vanilla's MusicTracker is told to stop on every tick ours plays
 * (its next-song timer then never runs out), so the two never stack; once ours has ended or the player leaves, vanilla resumes by its own timer.
 */
public final class MusicDirector {
    private MusicDirector() {}

    /** The battle tracks in the order they alternate. */
    private static final AudioCues.Cue[] BATTLE = {AudioCues.Cue.MUSIC_BATTLE_GRIM, AudioCues.Cue.MUSIC_BATTLE_DRUMS};
    private static final int FADE_IN_TICKS = 40, FADE_OUT_TICKS = 30, CALM_GAP_TICKS = 20 * 45, BATTLE_GAP_TICKS = 20 * 4;

    /** A music stream the director can fade: the volume is the cue's own times the fade level. */
    private static final class Track extends AbstractSoundInstance {
        final AudioCues.Cue cue;
        float level;
        boolean fadingOut;

        Track(AudioCues.Cue cue, net.minecraft.sound.SoundEvent event) {
            super(event, SoundCategory.MUSIC, SoundInstance.createRandom());
            this.cue = cue;
            this.volume = cue.volume;
            this.pitch = cue.pitch;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }

        @Override public float getVolume() { return super.getVolume() * level; }
    }

    private static Track track;
    private static int gap, battleTurn;
    private static int roundOfTurn = Integer.MIN_VALUE;
    private static boolean sawBattle, epicPending;
    private static String seenResult = "";

    /** One client tick with a player in a world. */
    public static void tick(MinecraftClient c) {
        if (!GravesAndGloryClient.ready) { stop(c); return; }
        noteResult();
        AudioCues.Cue want = want();
        if (track != null) {
            boolean playing = c.getSoundManager().isPlaying(track);
            if (!track.fadingOut && want != track.cue) track.fadingOut = true;
            if (track.fadingOut) {
                track.level -= 1f / FADE_OUT_TICKS;
                if (track.level <= 0 || !playing) { c.getSoundManager().stop(track); track = null; }
            } else if (!playing) {
                gap = want == AudioCues.Cue.MUSIC_CALM ? CALM_GAP_TICKS : BATTLE_GAP_TICKS;
                if (want != null && isBattle(want)) battleTurn++;
                track = null;
            } else track.level = Math.min(1f, track.level + 1f / FADE_IN_TICKS);
            if (track != null) c.getMusicTracker().stop(); // vanilla stays silent while ours plays
            return;
        }
        if (want == null) return;
        if (gap > 0) { gap--; return; }
        var event = Audio.event(want);
        if (event == null) return;
        c.getMusicTracker().stop();
        if (want == AudioCues.Cue.MUSIC_EPIC) epicPending = false; // asked for once; want() keeps it while it plays
        track = new Track(want, event);
        c.getSoundManager().play(track);
    }

    /** Leaves the world or the server: ours stops at once and the state forgets the round. */
    public static void reset(MinecraftClient c) {
        stop(c);
        gap = 0; battleTurn = 0; roundOfTurn = Integer.MIN_VALUE; sawBattle = false; epicPending = false; seenResult = "";
    }

    private static void stop(MinecraftClient c) {
        if (track != null) { c.getSoundManager().stop(track); track = null; }
    }

    private static boolean isBattle(AudioCues.Cue cue) { return cue == BATTLE[0] || cue == BATTLE[1]; }

    /** A new report whose outcome is VICTORY, after a battle this client watched, asks for the epic track once. */
    private static void noteResult() {
        if (GravesAndGloryClient.battleActive()) sawBattle = true;
        var r = CampaignClient.result;
        String id = r != null && r.has("id") ? r.get("id").getAsString() : "";
        if (id.isEmpty() || id.equals(seenResult)) return;
        seenResult = id;
        String outcome = r.has("outcome") ? r.get("outcome").getAsString() : "";
        if (sawBattle && outcome.equals("VICTORY")) epicPending = true;
        // The result lands with the ending sting (victory for a win or a draw, defeat otherwise): the next track waits the sting out rather than starting over it.
        if (sawBattle) gap = Math.max(gap, outcome.equals("BATTLE COMPLETE") ? AudioCues.DEFEAT_STING_TICKS : AudioCues.VICTORY_STING_TICKS);
        sawBattle = false;
    }

    /** The track the moment calls for, or null for none. */
    private static AudioCues.Cue want() {
        if (CampaignClient.cinematic()) return null;
        if (GravesAndGloryClient.battleActive()) {
            if (CampaignClient.serial != roundOfTurn) { roundOfTurn = CampaignClient.serial; battleTurn = Math.floorMod(CampaignClient.serial, BATTLE.length); gap = 0; }
            return BATTLE[battleTurn % BATTLE.length];
        }
        if (GravesAndGloryClient.inCamp()) {
            boolean epicPlaying = track != null && track.cue == AudioCues.Cue.MUSIC_EPIC && !track.fadingOut;
            return epicPending || epicPlaying ? AudioCues.Cue.MUSIC_EPIC : AudioCues.Cue.MUSIC_CALM;
        }
        return null;
    }
}
