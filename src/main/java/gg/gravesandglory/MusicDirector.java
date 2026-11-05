package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

/**
 * Picks the mod's streamed music (AudioCues MUSIC_*) on a Graves and Glory server and keeps vanilla music quiet while it plays. Client thread only.
 * <ul>
 * <li>In the Banner Camp (lobby, War Table): the calm track, with a pause between plays; on the campaign briefing screen (the War Table's campaign sheet) the briefing track instead, looping.</li>
 * <li>After a round the local side won (outcome VICTORY): the epic track once, then back to calm.</li>
 * <li>In a running battle: the two battle tracks in turn, the first chosen by the round's serial; silent through the entrance camera, which has the horns.
 * While the round is in overtime (CampaignClient.overtime, from the server's battleMode.overtime) the overtime track replaces them, looping, until the round ends.</li>
 * </ul>
 * Under the track runs a second, separate stream, the ambience bed (AudioCues.bed: the camp's room tone, the survival wind, or the battle map's bed), looping in the
 * ambient category (the Ambient slider) and fading in and out when the moment changes; it plays through the entrance camera.
 * The tracks are in the music category, so the player's Music slider governs them. Vanilla's MusicTracker is told to stop on every tick ours plays
 * (its next-song timer then never runs out), so the two never stack; once ours has ended or the player leaves, vanilla resumes by its own timer.
 */
public final class MusicDirector {
    private MusicDirector() {}

    /** The battle tracks in the order they alternate. */
    private static final AudioCues.Cue[] BATTLE = {AudioCues.Cue.MUSIC_BATTLE_GRIM, AudioCues.Cue.MUSIC_BATTLE_DRUMS};
    private static final int FADE_IN_TICKS = 40, FADE_OUT_TICKS = 30, CALM_GAP_TICKS = 20 * 45, BATTLE_GAP_TICKS = 20 * 4;
    private static final int BED_FADE_IN_TICKS = 80, BED_FADE_OUT_TICKS = 50;

    /** A music stream the director can fade: the volume is the cue's own times the fade level. */
    private static final class Track extends AbstractSoundInstance {
        final AudioCues.Cue cue;
        float level;
        boolean fadingOut;

        Track(AudioCues.Cue cue, net.minecraft.sound.SoundEvent event, SoundCategory category, boolean loop) {
            super(event, category, SoundInstance.createRandom());
            this.cue = cue;
            this.repeat = loop;
            this.volume = cue.volume;
            this.pitch = cue.pitch;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
        }

        @Override public float getVolume() { return super.getVolume() * level; }
    }

    private static Track track, bed;
    private static int gap, battleTurn;
    private static int roundOfTurn = Integer.MIN_VALUE;
    private static boolean sawBattle, epicPending, wasBriefing, wasOvertime;
    private static String seenResult = "";

    /** One client tick with a player in a world. */
    public static void tick(MinecraftClient c) {
        if (!GravesAndGloryClient.ready) { stop(c); return; }
        noteResult();
        tickBed(c);
        AudioCues.Cue want = want(c);
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
        track = new Track(want, event, SoundCategory.MUSIC, loops(want));
        c.getSoundManager().play(track);
    }

    /** The briefing and overtime tracks loop (until the screen closes or the round ends); the rest play once with a gap. */
    private static boolean loops(AudioCues.Cue cue) { return cue == AudioCues.Cue.CAMPAIGN_BRIEFING_MUSIC || cue == AudioCues.Cue.OVERTIME_MUSIC; }

    /** The ambience bed: one looping ambient-category stream that fades out when the moment's bed changes and the next fades in; gone with the world. */
    private static void tickBed(MinecraftClient c) {
        AudioCues.Cue want = wantBed();
        if (bed != null) {
            boolean playing = c.getSoundManager().isPlaying(bed);
            if (want != bed.cue) bed.fadingOut = true;
            if (bed.fadingOut) {
                bed.level -= 1f / BED_FADE_OUT_TICKS;
                if (bed.level <= 0 || !playing) { c.getSoundManager().stop(bed); bed = null; }
            } else if (!playing) bed = null; // it ended on its own (a stopped stream): the next tick starts it again
            else bed.level = Math.min(1f, bed.level + 1f / BED_FADE_IN_TICKS);
        }
        if (bed != null || want == null) return;
        var event = Audio.event(want);
        if (event == null) return;
        bed = new Track(want, event, SoundCategory.AMBIENT, true);
        c.getSoundManager().play(bed);
    }

    /** The bed the moment calls for: the camp's room tone, or in a battle the survival wind or the map's bed (AudioCues.bed). */
    private static AudioCues.Cue wantBed() {
        if (GravesAndGloryClient.battleActive()) {
            int m = CampaignClient.map;
            return AudioCues.bed(false, CampaignClient.modeId(), m >= 0 && m < Spec.MAPS.length ? Spec.MAPS[m].theme() : "");
        }
        return GravesAndGloryClient.inCamp() ? AudioCues.bed(true, "", "") : null;
    }

    /** Whether the campaign briefing screen is up: the War Table home with its campaign sheet raised, or the old training page. */
    private static boolean briefingOpen(MinecraftClient c) {
        return c.currentScreen instanceof CampScreen s && s.briefing();
    }

    /** Leaves the world or the server: ours stops at once and the state forgets the round. */
    public static void reset(MinecraftClient c) {
        stop(c);
        gap = 0; battleTurn = 0; roundOfTurn = Integer.MIN_VALUE; sawBattle = false; epicPending = false; seenResult = ""; wasBriefing = false; wasOvertime = false;
    }

    private static void stop(MinecraftClient c) {
        if (track != null) { c.getSoundManager().stop(track); track = null; }
        if (bed != null) { c.getSoundManager().stop(bed); bed = null; }
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
    private static AudioCues.Cue want(MinecraftClient c) {
        boolean overtime = GravesAndGloryClient.battleActive() && CampaignClient.overtime();
        if (overtime && !wasOvertime) gap = 0; // overtime starts its music at once
        wasOvertime = overtime;
        boolean briefing = GravesAndGloryClient.inCamp() && briefingOpen(c);
        if (briefing && !wasBriefing) gap = 0; // opening the briefing starts its music at once
        wasBriefing = briefing;
        if (CampaignClient.cinematic()) return null;
        if (overtime) return AudioCues.Cue.OVERTIME_MUSIC;
        if (GravesAndGloryClient.battleActive()) {
            if (CampaignClient.serial != roundOfTurn) { roundOfTurn = CampaignClient.serial; battleTurn = Math.floorMod(CampaignClient.serial, BATTLE.length); gap = 0; }
            return BATTLE[battleTurn % BATTLE.length];
        }
        if (GravesAndGloryClient.inCamp()) {
            boolean epicPlaying = track != null && track.cue == AudioCues.Cue.MUSIC_EPIC && !track.fadingOut;
            if (epicPending || epicPlaying) return AudioCues.Cue.MUSIC_EPIC;
            return briefing ? AudioCues.Cue.CAMPAIGN_BRIEFING_MUSIC : AudioCues.Cue.MUSIC_CALM;
        }
        return null;
    }
}
