package gg.gravesandglory;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.util.math.Vec3d;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * The animation rework's server-to-client motion events (spec 5 and 6.4): what a troop swings (attack_v1), what strikes it (hit_v1) and how it
 * stands (stance_v2). The server is authoritative; the client's motion engine (Task 13) reads the queues in Client. Every tick is the world's
 * time (getTime), the clock MotionInput reads: a swing's startTick is the world tick it began on, which is Unit.swungAt on the battle clock.
 */
public final class MotionEvents {
    private MotionEvents() {}
    /** A troop's swing: a varInt entity id, a varInt index into its family's attackVariants rows (Tactics.variants, Battle's pick) and a varLong world tick it began on. */
    public static final Identifier ATTACK = new Identifier("gravesandglory", "attack_v1");
    /** A blow that struck a troop: a varInt entity id, byte kind (KINDS), byte zone (ZONES), float signed bearing (BattleMath.signedBearing), byte weight (0-2), byte flags (1 blocked, 2 lethal), varLong world tick. */
    public static final Identifier HIT = new Identifier("gravesandglory", "hit_v1");
    /** A troop's stance: a varInt entity id, byte index into STANCES and a float aimPitch (BattleMath.launchPitch, degrees up; 0 unless ranged_aim). Replaces stance_v1. */
    public static final Identifier STANCE = new Identifier("gravesandglory", "stance_v2");
    public static final List<String> STANCES = BattleMath.STANCES, KINDS = BattleMath.KINDS, ZONES = BattleMath.ZONES;
    /** Packets sent since the server started, for the live checks: attacks, hits, stances on a change, stances and attacks on START_TRACKING, and on a late channel registration. */
    static volatile int attackSends, hitSends, stanceSends, trackingSends, registerSends;
    /** Above zero while Combat.resolve deals its damage: the hit it sends then is the troop's own, so HitEventMixin sends none for the same damage call. */
    static int resolving;
    /** A stance as the client holds it. */
    public record Stance(String name, float aimPitch) { public static final Stance NONE = new Stance("", 0); }

    static Collection<ServerPlayerEntity> watchers(LivingEntity e) { return PlayerLookup.tracking(e); }

    /** Sends a troop's swing to every player tracking it: variantIndex into Tactics.variants(its family), startTick the world tick it began on. Returns how many it sent. */
    public static int sendAttack(GravesAndGlory.Unit u, int variantIndex, long startTick) { return sendAttack(u, variantIndex, startTick, watchers(u.mob)); }
    static int sendAttack(GravesAndGlory.Unit u, int variantIndex, long startTick, Collection<ServerPlayerEntity> players) {
        int sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, ATTACK)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(u.mob.getId()); buf.writeVarInt(variantIndex); buf.writeVarLong(startTick);
            ServerPlayNetworking.send(p, ATTACK, buf); sent++;
        }
        attackSends += sent; return sent;
    }
    /** Sends a blow on a troop to every player tracking it, stamped with the world tick now. Returns how many it sent. */
    public static int sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal) {
        return sendHit(victim, kind, zone, bearing, weight, blocked, lethal, watchers(victim));
    }
    static int sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, Collection<ServerPlayerEntity> players) {
        int k = Math.max(0, KINDS.indexOf(kind)), z = ZONES.indexOf(zone), sent = 0;
        if (z < 0) z = 1;
        long tick = victim.getWorld().getTime();
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, HIT)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(victim.getId()); buf.writeByte(k); buf.writeByte(z); buf.writeFloat(bearing);
            buf.writeByte(Math.max(0, Math.min(2, weight))); buf.writeByte((blocked ? 1 : 0) | (lethal ? 2 : 0)); buf.writeVarLong(tick);
            ServerPlayNetworking.send(p, HIT, buf); sent++;
        }
        hitSends += sent; return sent;
    }
    /** Sends a troop's stance (STANCES) and its aim pitch to every player tracking it. Returns how many it sent. */
    public static int sendStance(GravesAndGlory.Unit u, String stance, float aimPitch) { return sendStance(u, stance, aimPitch, watchers(u.mob)); }
    static int sendStance(GravesAndGlory.Unit u, String stance, float aimPitch, Collection<ServerPlayerEntity> players) {
        int index = Math.max(0, STANCES.indexOf(stance)), sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, STANCE)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(u.mob.getId()); buf.writeByte(index); buf.writeFloat(aimPitch);
            ServerPlayNetworking.send(p, STANCE, buf); sent++;
        }
        stanceSends += sent; return sent;
    }
    /**
     * A troop struck by something other than a troop's blow (HitEventMixin, on LivingEntity.damage returning true; Combat.resolve sends its own, so a damage
     * call inside it is left alone): a fall is blunt to the legs, fire, magic or wither magic, an explosion blunt, a projectile an arrow at the zone of its
     * height, a player's blow his weapon's kind (BattleMath.weaponKind), a troop's damage outside resolve (a bleed) a slash, anything else blunt. Its
     * signed bearing is from the source's position (0 for none), its weight its share of the troop's maximum health; nothing outside resolve is blocked.
     */
    public static void damaged(LivingEntity self, DamageSource source, float amount) {
        var b = GravesAndGlory.battle;
        if (resolving > 0 || b == null || !b.running || self.getWorld().isClient() || self.getWorld() != b.world || !ArmyMobs.isTroop(self)) return;
        String kind = "blunt", zone = "body";
        if (source.isIn(DamageTypeTags.IS_FALL)) zone = "legs";
        else if (source.isIn(DamageTypeTags.IS_FIRE) || source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC) || source.isOf(DamageTypes.WITHER)) kind = "magic";
        else if (source.isIn(DamageTypeTags.IS_EXPLOSION)) kind = "blunt";
        else if (source.isIn(DamageTypeTags.IS_PROJECTILE)) { kind = "arrow"; Entity shot = source.getSource(); if (shot != null) zone = BattleMath.zone(shot.getY(), self.getY(), self.getHeight()); }
        else if (source.getAttacker() instanceof PlayerEntity p) kind = BattleMath.weaponKind(Registries.ITEM.getId(p.getMainHandStack().getItem()).toString());
        else if (source.isOf(Combat.TROOP)) kind = "slash";
        Vec3d from = source.getPosition();
        float bearing = from == null ? 0 : (float) BattleMath.signedBearing(self.getYaw(), from.x - self.getX(), from.z - self.getZ());
        var D = Spec.COMBATDISCIPLINE[0];
        sendHit(self, kind, zone, bearing, BattleMath.weight(amount, self.getMaxHealth(), D.mediumHitShare(), D.heavyHitShare()), false, !self.isAlive());
    }
    /**
     * What a player who starts tracking a troop (or registers the channels late) is owed: its stance when it has one, and its newest swing when it
     * has swung, its startTick moved from the battle clock to the world's (the world tick now less the battle ticks since the swing). Returns how many it sent.
     */
    static int catchUp(GravesAndGlory.Battle b, GravesAndGlory.Unit u, ServerPlayerEntity p) {
        int sent = 0; var to = List.of(p);
        if (!u.stance.isEmpty()) sent += sendStance(u, u.stance, u.aimPitch, to);
        if (u.swungAt >= 0 && u.lastVariant >= 0) sent += sendAttack(u, u.lastVariant, u.mob.getWorld().getTime() - (b.time - u.swungAt), to);
        return sent;
    }
    /** catchUp for every troop the player tracks: for a client that registered the channels after its trackers started. */
    static int sendTracked(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        int sent = 0;
        for (GravesAndGlory.Unit u : b.units) if (PlayerLookup.tracking(u.mob).contains(p)) sent += catchUp(b, u, p);
        return sent;
    }

    /**
     * The client's side (GravesAndGloryClient registers the receivers): each troop's recent swings and blows, newest last and the oldest dropped past
     * KEEP, and its stance. A packet that arrives on START_TRACKING comes before the entity's spawn packet, so the entries are kept for an id the world
     * does not have yet; an unloaded entity's entries go (forget) and a disconnect clears them all. Concurrent: the integrated server's checks read them.
     */
    public static final class Client {
        private Client() {}
        static final int KEEP = 8;
        static final Map<Integer, Deque<MotionInput.Attack>> ATTACKS = new ConcurrentHashMap<>();
        static final Map<Integer, Deque<MotionInput.Hit>> HITS = new ConcurrentHashMap<>();
        static final Map<Integer, Stance> STANCES = new ConcurrentHashMap<>();
        /** The troop's recent swings, oldest first (empty for none). */
        public static Deque<MotionInput.Attack> attacks(int id) { return ATTACKS.getOrDefault(id, new ArrayDeque<>()); }
        /** The blows that recently struck the troop, oldest first (empty for none). */
        public static Deque<MotionInput.Hit> hits(int id) { return HITS.getOrDefault(id, new ArrayDeque<>()); }
        /** The troop's stance (Stance.NONE for none). */
        public static Stance stance(int id) { return STANCES.getOrDefault(id, Stance.NONE); }
        static <T> void push(Map<Integer, Deque<T>> map, int id, T value) {
            Deque<T> q = map.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>()); q.addLast(value);
            while (q.size() > KEEP) q.pollFirst();
        }
        static void attack(int id, int variant, long startTick) { push(ATTACKS, id, new MotionInput.Attack(variant, startTick)); }
        static void hit(int id, int kind, int zone, float bearing, int weight, int flags, long tick) {
            String k = kind >= 0 && kind < KINDS.size() ? KINDS.get(kind) : "blunt", z = zone >= 0 && zone < ZONES.size() ? ZONES.get(zone) : "body";
            push(HITS, id, new MotionInput.Hit(k, z, bearing, weight, (flags & 1) != 0, (flags & 2) != 0, tick));
        }
        static void stance(int id, int index, float aimPitch) {
            String name = index >= 0 && index < MotionEvents.STANCES.size() ? MotionEvents.STANCES.get(index) : "";
            if (name.isEmpty()) STANCES.remove(id); else STANCES.put(id, new Stance(name, aimPitch));
        }
        static void forget(int id) { ATTACKS.remove(id); HITS.remove(id); STANCES.remove(id); }
        static void clear() { ATTACKS.clear(); HITS.clear(); STANCES.clear(); }
    }
}
