package gg.gravesandglory;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;

/**
 * Plays the AudioCues table (plan 4F.4). Server side: a cue goes to one player's connection, to a team of the round's seated players, to all of
 * them, or to everyone near a place; every volume, pitch and cooldown is the cue's own. Bots hear nothing (they are no connection). The wave
 * warning is a hook for Survival: Audio.all(battle, Cue.WAVE_WARNING, "wave") is the call. Server thread only.
 */
public final class Audio {
    private Audio() {}

    private static final AudioCues.Throttle THROTTLE = new AudioCues.Throttle();

    /** Registers every event of the table (GravesAndGlory.onInitialize); the ids are the sounds.json keys. */
    public static void register() {
        for (String path : AudioCues.ids()) {
            var id = new Identifier("gravesandglory", path);
            Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
        }
    }

    /** The registered event of a cue, or null before registration. */
    public static SoundEvent event(AudioCues.Cue cue) {
        return Registries.SOUND_EVENT.get(new Identifier("gravesandglory", cue.id));
    }

    static SoundCategory category(AudioCues.Cue cue) {
        return switch (cue.category) {
            case "players" -> SoundCategory.PLAYERS;
            case "hostile" -> SoundCategory.HOSTILE;
            case "neutral" -> SoundCategory.NEUTRAL;
            case "ambient" -> SoundCategory.AMBIENT;
            default -> SoundCategory.MASTER;
        };
    }

    /** Whether the cue may play now for this key (see AudioCues.Throttle); a cue with no cooldown always may. */
    public static boolean allow(AudioCues.Cue cue, String key, long now) {
        return THROTTLE.allow(cue, key, now);
    }

    /** A new round: the cooldowns start empty (a clock that goes back also frees them). */
    public static void reset() { THROTTLE.clear(); }

    /** The cue to one player's connection alone. */
    public static void to(ServerPlayerEntity p, AudioCues.Cue cue) {
        var e = event(cue);
        if (p != null && e != null) p.playSound(e, category(cue), cue.volume, cue.pitch);
    }

    /** The cue to every seated player of the round's team (team -1: both teams), once per key per cooldown. Returns whether it played. */
    public static boolean team(GravesAndGlory.Battle b, AudioCues.Cue cue, int team, String key) {
        if (b == null || b.server == null || !allow(cue, key, b.time)) return false;
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var s = b.squads.get(p.getUuid());
            if (s != null && s.seated && (team < 0 || s.team == team)) to(p, cue);
        }
        return true;
    }

    /** The cue to every seated player. */
    public static boolean all(GravesAndGlory.Battle b, AudioCues.Cue cue, String key) {
        return team(b, cue, -1, key);
    }

    /** A point changed hands: its new owner's side hears the win, the other side the loss. */
    public static void captured(GravesAndGlory.Battle b, String point, int team) {
        team(b, AudioCues.Cue.CAPTURE_WON, team, point);
        team(b, AudioCues.Cue.CAPTURE_LOST, 1 - team, point);
    }

    /** The cue at a place for everyone near it (the world's own sound packet), once per key per cooldown. */
    public static boolean at(GravesAndGlory.Battle b, AudioCues.Cue cue, Vec3d at, String key) {
        var e = event(cue);
        if (b == null || b.world == null || e == null || !allow(cue, key, b.time)) return false;
        b.world.playSound(null, BlockPos.ofFloored(at), e, category(cue), cue.volume, cue.pitch);
        return true;
    }

    /** The end of a round to one participant: the victory sting for a winner (and for a draw), the defeat sting for the losing side. */
    public static void ending(ServerPlayerEntity p, int winner, int team) {
        to(p, winner >= 0 && team >= 0 && team != winner ? AudioCues.Cue.DEFEAT : AudioCues.Cue.VICTORY);
    }

    /** A countdown second to each member (the last three seconds; the caller passes the ticks left, stepped by ten). */
    public static void countdown(GravesAndGlory.Battle b, Iterable<UUID> members, int ticksLeft) {
        if (b == null || b.server == null || ticksLeft <= 0 || ticksLeft > 60 || ticksLeft % 20 != 0) return;
        for (UUID m : members) to(b.server.getPlayerManager().getPlayer(m), AudioCues.Cue.COUNTDOWN_TICK);
    }
}
