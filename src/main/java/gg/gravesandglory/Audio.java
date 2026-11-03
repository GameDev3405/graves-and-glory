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
            case "music" -> SoundCategory.MUSIC;
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
        return at(b, cue, at, key, 1f);
    }

    /** As above, the pitch scaled (a siege bolt's twang lower than an archer's). A cue with a per-tick cap also gets a small random pitch spread, so a volley never sounds like one sample. */
    public static boolean at(GravesAndGlory.Battle b, AudioCues.Cue cue, Vec3d at, String key, float pitchScale) {
        var e = event(cue);
        if (b == null || b.world == null || e == null || !allow(cue, key, b.time)) return false;
        float spread = cue.maxPerTick > 0 ? .94f + b.world.random.nextFloat() * .12f : 1f;
        b.world.playSound(null, BlockPos.ofFloored(at), e, category(cue), cue.volume, cue.pitch * pitchScale * spread);
        return true;
    }

    /** What a troop swings or shoots, for choosing its combat sound. */
    public enum Arm { NONE, BLADE, HEAVY, BOW, MAGIC }

    /** A troop's arm: casters cast, ranged troops with a weapon loose it, melee troops with an axe, mace or hammer (or the Colossus) hit heavy, other armed melee troops are blades; beasts and the Mortar have none. */
    public static Arm arm(GravesAndGlory.Unit u) {
        if (u == null || u.spec == null) return Arm.NONE;
        String weapon = u.spec.weapon();
        boolean armed = weapon != null && !weapon.endsWith(":air");
        if (u.spec.magic()) return Arm.MAGIC;
        if (u.spec.ranged()) return armed ? Arm.BOW : Arm.NONE;
        if (!armed) return u.spec.id().equals("ic_colossus") ? Arm.HEAVY : Arm.NONE;
        return weapon.endsWith("axe") || weapon.endsWith("mace") || weapon.endsWith("hammer") ? Arm.HEAVY : Arm.BLADE;
    }

    /** Whether a troop wears metal armour on the chest (chainmail, iron, gold, diamond or netherite), for the armour-struck sound. */
    public static boolean armoured(GravesAndGlory.Unit u) {
        if (u == null || u.spec == null) return false;
        String chest = u.spec.chest();
        return chest != null && (chest.contains("chainmail") || chest.contains("iron") || chest.contains("gold") || chest.contains("diamond") || chest.contains("netherite"));
    }

    /** A soldier's swing of a blade as the blow starts (heavy weapons and beasts stay quiet until they land). */
    public static void swing(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        if (u != null && u.slot % 2 == 0 && arm(u) == Arm.BLADE) at(b, AudioCues.Cue.SWORD_SWING, u.mob.getPos(), "s" + u.mob.getId());
    }

    /** A ranged troop's shot as it leaves: a spell for a caster, a bowstring for the rest (lower for a siege crew's bolt). */
    public static void shot(GravesAndGlory.Battle b, GravesAndGlory.Unit u) {
        var arm = arm(u);
        if (arm == Arm.MAGIC) at(b, AudioCues.Cue.MAGIC_CAST, u.mob.getPos(), "c" + u.mob.getId());
        else if (arm == Arm.BOW) at(b, AudioCues.Cue.BOW_SHOOT, u.mob.getPos(), "b" + u.mob.getId(), u.tags.contains("siege") ? .6f : 1f);
    }

    /**
     * A landed blow (Combat.resolve, not a cleave, pierce or splash second hit): a raised shield blocks melee and missiles alike; otherwise a caster's
     * missile or blight strikes as magic, a missile as an arrow, a heavy weapon thuds, a blade clashes with another weapon or rings off armour.
     */
    public static void hit(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, net.minecraft.entity.LivingEntity target, GravesAndGlory.Unit victim, String kind, boolean shieldRaised) {
        if (b == null || attacker == null || target == null) return;
        Vec3d at = target.getPos().add(0, 1, 0);
        String key = "h" + attacker.mob.getId();
        var arm = arm(attacker);
        boolean melee = kind.equals("melee"), missile = kind.equals("arrow") || kind.equals("bolt");
        if (shieldRaised && (melee || missile)) at(b, AudioCues.Cue.SHIELD_BLOCK, at, "k" + target.getId());
        else if (arm == Arm.MAGIC && !melee) at(b, AudioCues.Cue.MAGIC_IMPACT, at, "m" + attacker.mob.getId());
        else if (missile) at(b, AudioCues.Cue.ARROW_HIT, at, key);
        else if (melee && arm == Arm.HEAVY) at(b, AudioCues.Cue.HEAVY_METAL_HIT, at, key);
        else if (melee && arm == Arm.BLADE) {
            var theirs = arm(victim);
            if (theirs == Arm.BLADE || theirs == Arm.HEAVY) at(b, AudioCues.Cue.SWORD_CLASH, at, key);
            else if (armoured(victim)) at(b, AudioCues.Cue.ARMOR_HIT, at, key);
        }
    }

    /** The round's last ten seconds: a tick a second to every seated player (the caller passes the ticks left on the round clock). */
    public static void clock(GravesAndGlory.Battle b, long ticksLeft) {
        if (b != null && ticksLeft > 0 && ticksLeft <= 200 && ticksLeft % 20 == 0) all(b, AudioCues.Cue.CLOCK_TICK, "clock");
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
