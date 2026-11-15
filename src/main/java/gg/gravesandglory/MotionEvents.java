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
import net.minecraft.server.ServerTask;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.BooleanSupplier;

/**
 * The animation rework's server-to-client motion events (spec 5 and 6.4): what a troop swings (attack_v1), what strikes it (hit_v1) and how it
 * stands (stance_v2). The server is authoritative; the client's motion engine (Task 13) reads the queues in Client. Every tick is the world's
 * time (getTime), the clock MotionInput reads: a swing's startTick is the world tick it began on (Unit.swingTick; Unit.swungAt is the same swing on the
 * battle clock). Every angle is in MotionInput's convention: a hit's bearing 0 in front, 180 behind, negative from the troop's left; an aim pitch in
 * degrees, positive down as Minecraft's own pitch, so a lofted volley aims at a negative pitch.
 */
public final class MotionEvents {
    private MotionEvents() {}
    /**
     * A troop's swing: a varInt entity id, a varInt index into its family's attackVariants rows (Tactics.variants, Battle's pick) and a varLong world tick it began on.
     * Variant MotionInput.CANCEL (-1) is a cancel (coordinator 2026-11-11): the announced attack that began on that tick will not land (Projectiles.dropDraw), so the
     * client fades it out. Same layout, same id: an old client drops it as an invalid variant (as hit_v1's newer flag bits are ignored).
     */
    public static final Identifier ATTACK = Identifier.of("gravesandglory", "attack_v1");
    /** A blow that struck a troop: a varInt entity id, byte kind (KINDS), byte zone (ZONES), float signed bearing (BattleMath.signedBearing), byte weight (0-2), byte flags (1 blocked, 2 lethal, 4 knockback: the blow also threw the troop, Abilities.knock; 8 pulled: the blow also dragged the troop toward its
     * striker, Abilities.pull; an old client ignores the bits it does not know), varLong world tick. */
    public static final Identifier HIT = Identifier.of("gravesandglory", "hit_v1");
    /**
     * A troop's stance: a varInt entity id, byte index into STANCES and a float aimPitch (Projectiles.aimPitch: degrees, positive down as MotionInput.aimPitch
     * and Minecraft's pitch, so a lofted volley is negative; 0 unless ranged_aim). The client stores it as sent and MotionInput reads it unchanged. Replaces stance_v1.
     */
    public static final Identifier STANCE = Identifier.of("gravesandglory", "stance_v2");
    /**
     * A troop rose instead of dying (Houses.allowDeath: Rise Again, the Hollow King's Undying King): a varInt entity id, a varLong world tick the rise fired on and a
     * varInt window, the ticks the server keeps it out of the fight (its stun: Unit.stunnedUntil less the battle time; 0 when it fights on at once). Presentation
     * only: a client whose model has fake_death and rise plays them (DeathController) instead of the stun; one without them keeps the stun. An old client never
     * registers the channel, so it is never sent one and keeps the stun; a new client on an old server hears none and keeps the stun.
     */
    public static final Identifier RISE = Identifier.of("gravesandglory", "rise_v1");
    public static final List<String> STANCES = BattleMath.STANCES, KINDS = BattleMath.KINDS, ZONES = BattleMath.ZONES;
    /** Packets sent since the server started, for the live checks: attacks, hits, stances on a change, stances and attacks on START_TRACKING, and on a late channel registration. */
    static volatile int attackSends, hitSends, stanceSends, trackingSends, registerSends, riseSends, pulledSends;
    /**
     * The entities a troop's blow is being dealt to now (Combat.resolve), with how deep: the hit Combat sends is that target's own, so HitEventMixin sends
     * none for damage to it meanwhile. Damage to any other entity inside the resolve (thorns on the attacker, a splash) still sends its own. Server thread only.
     */
    static final Map<LivingEntity, Integer> RESOLVING = new IdentityHashMap<>();
    /** Deals a troop's blow on target through damage (Combat.resolve): HitEventMixin sends no hit for target's damage inside it, Combat does. Returns what damage returned. */
    static boolean resolving(LivingEntity target, BooleanSupplier damage) {
        RESOLVING.merge(target, 1, Integer::sum);
        try { return damage.getAsBoolean(); } finally { RESOLVING.computeIfPresent(target, (k, n) -> n > 1 ? n - 1 : null); }
    }
    /** A rise as the client holds it: the world tick it fired on and the server's window in ticks (0: the troop fights on at once). */
    public record Rise(long tick, int window) {}
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
        return sendHit(victim, kind, zone, bearing, weight, blocked, lethal, false, players);
    }
    static int sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, boolean knockback, Collection<ServerPlayerEntity> players) {
        return sendHit(victim, kind, zone, bearing, weight, blocked, lethal, knockback, false, players);
    }
    /** The same, with the pulled flag (8): the blow also dragged the troop toward its striker (Abilities.pull: the Harpoon, the Undertow). */
    static int sendHit(LivingEntity victim, String kind, String zone, float bearing, int weight, boolean blocked, boolean lethal, boolean knockback, boolean pulled, Collection<ServerPlayerEntity> players) {
        int k = Math.max(0, KINDS.indexOf(kind)), z = ZONES.indexOf(zone), sent = 0;
        if (z < 0) z = 1;
        long tick = victim.getWorld().getTime();
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, HIT)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(victim.getId()); buf.writeByte(k); buf.writeByte(z); buf.writeFloat(bearing);
            buf.writeByte(Math.max(0, Math.min(2, weight))); buf.writeByte((blocked ? 1 : 0) | (lethal ? 2 : 0) | (knockback ? 4 : 0) | (pulled ? 8 : 0)); buf.writeVarLong(tick);
            ServerPlayNetworking.send(p, HIT, buf); sent++;
        }
        hitSends += sent; if (pulled) pulledSends += sent; return sent;
    }
    /** Sends a troop's rise (RISE) to every player tracking it: the world tick it fired on (Unit.riseTick) and the server's window (Unit.riseWindow). Returns how many it sent. */
    public static int sendRise(GravesAndGlory.Unit u) { return sendRise(u, watchers(u.mob)); }
    static int sendRise(GravesAndGlory.Unit u, Collection<ServerPlayerEntity> players) {
        int sent = 0;
        for (ServerPlayerEntity p : players) {
            if (!ServerPlayNetworking.canSend(p, RISE)) continue;
            PacketByteBuf buf = PacketByteBufs.create(); buf.writeVarInt(u.mob.getId()); buf.writeVarLong(u.riseTick); buf.writeVarInt(u.riseWindow);
            ServerPlayNetworking.send(p, RISE, buf); sent++;
        }
        riseSends += sent; return sent;
    }
    /** World ticks past a rise's window (or past its start, for a window of 0) that a player catching up is still sent it; the client plays what is left of it. */
    static final int RISE_CATCHUP = 100;
    /** The troop's rise to a player catching up, while it may still show. Returns how many it sent. */
    static int catchUpRise(GravesAndGlory.Unit u, ServerPlayerEntity p) {
        return u.riseTick >= 0 && u.mob.getWorld().getTime() - u.riseTick < Math.max(u.riseWindow, 0) + RISE_CATCHUP ? sendRise(u, List.of(p)) : 0;
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
     * A troop struck by something other than a troop's blow (HitEventMixin, when LivingEntity.damage returns true or vanilla's shield stopped the blow;
     * Combat.resolve sends its own for its target, so that target's damage inside it is left alone): a fall is blunt to the legs, fire, magic or wither
     * magic, an explosion blunt, a projectile an arrow at the zone of its height, a player's blow his weapon's kind (BattleMath.weaponKind), a troop's
     * damage outside resolve (a bleed) a slash, anything else blunt. Its signed bearing is from the source's position (0 for none); its weight is the
     * share of the troop's maximum health it really lost (lost: health and absorption before less after, so a blow inside the hurt cooldown or on a
     * shield reads light); blocked when vanilla's shield took it (a troop's own blow bypasses shields, so only a player's or a stray one can be).
     */
    public static void damaged(LivingEntity self, DamageSource source, double lost, boolean blocked) {
        var b = GravesAndGlory.battle;
        if (RESOLVING.containsKey(self) || b == null || !b.running || self.getWorld().isClient() || self.getWorld() != b.world || !ArmyMobs.isTroop(self)) return;
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
        sendHit(self, kind, zone, bearing, BattleMath.weight(lost, self.getMaxHealth(), D.mediumHitShare(), D.heavyHitShare()), blocked, !self.isAlive());
    }
    /**
     * Whether the troop's newest swing may still show (BattleMath.swingCurrent): a valid variant that began no more than ATTACK_CLIP_MAX_TICKS ago on the
     * world clock. The server holds no clip lengths; the client plays the swing at its real clip time and skips one whose clip has ended (carry B8).
     */
    static boolean swingCurrent(GravesAndGlory.Unit u) {
        var rows = Tactics.variants(u.spec.family());
        return BattleMath.validVariant(u.catchVariant, rows.size()) && BattleMath.swingCurrent(u.mob.getWorld().getTime(), u.swingTick);
    }
    /** The troop's stance to a player catching up, when it has one. Returns how many it sent. */
    static int catchUpStance(GravesAndGlory.Unit u, ServerPlayerEntity p) { return u.stance.isEmpty() ? 0 : sendStance(u, u.stance, u.aimPitch, List.of(p)); }
    /** The troop's newest swing to a player catching up, with the world tick it began on, while it still shows (swingCurrent). Returns how many it sent. */
    static int catchUpAttack(GravesAndGlory.Unit u, ServerPlayerEntity p) { return swingCurrent(u) ? sendAttack(u, u.catchVariant, u.swingTick, List.of(p)) : 0; }
    /** What a player who registers the channels late is owed for a troop his client already has: its stance and its current swing. Returns how many it sent. */
    static int catchUp(GravesAndGlory.Unit u, ServerPlayerEntity p) { return catchUpStance(u, p) + catchUpAttack(u, p) + catchUpRise(u, p); }
    /**
     * What a player who starts tracking a troop is owed (START_TRACKING, which Fabric fires before the entity's spawn packet): its stance now (the client
     * keeps a stance for an id it does not have yet) and its current swing on a server task, after the spawn packet has gone (the client drops a swing for
     * an id its world does not have). The task sends it only while the player still tracks the living troop. Returns how many it sent now.
     */
    static int startTracking(GravesAndGlory.Unit u, ServerPlayerEntity p) {
        int sent = catchUpStance(u, p) + catchUpRise(u, p); // a rise, like a stance, is kept for an id the client does not have yet
        if (swingCurrent(u)) p.server.send(new ServerTask(p.server.getTicks(), () -> {
            if (!u.mob.isAlive() || !PlayerLookup.tracking(u.mob).contains(p)) return;
            int late = catchUpAttack(u, p); trackingSends += late; Tactics.trackingSends += late;
        }));
        return sent;
    }
    /** catchUp for every troop the player tracks: for a client that registered the channels after its trackers started. */
    static int sendTracked(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        int sent = 0;
        for (GravesAndGlory.Unit u : b.units) if (PlayerLookup.tracking(u.mob).contains(p)) sent += catchUp(u, p);
        return sent;
    }

    /**
     * The client's side (GravesAndGloryClient registers the receivers): each troop's recent swings and blows, newest last and the oldest dropped past
     * KEEP, and its stance. A swing or a blow for an id the client world does not have is dropped, so no queue is made for an unloaded entity (the
     * server sends a catch-up swing after the spawn packet); a swing outside its family's attackVariants rows is dropped and a weight is clamped to 0..2.
     * A stance arrives on START_TRACKING before the entity's spawn packet, so it is kept for an id the world does not have yet. An unloaded entity's
     * entries go (forget) and a disconnect clears them all. Concurrent: the integrated server's checks read them.
     */
    public static final class Client {
        private Client() {}
        static final int KEEP = 8;
        static final Map<Integer, Deque<MotionInput.Attack>> ATTACKS = new ConcurrentHashMap<>();
        static final Map<Integer, Deque<MotionInput.Hit>> HITS = new ConcurrentHashMap<>();
        static final Map<Integer, Stance> STANCES = new ConcurrentHashMap<>();
        /** Each troop's newest rise (rise_v1), kept like a stance for an id the world does not have yet. */
        static final Map<Integer, Rise> RISES = new ConcurrentHashMap<>();
        /** The troop's newest rise, or null. */
        public static Rise rise(int id) { return RISES.get(id); }
        static void rise(int id, long tick, int window) { RISES.put(id, new Rise(tick, Math.max(0, window))); }
        /** The one empty queue attacks and hits return for an id with none, so no frame allocates one: read only, never added to. */
        private static final Deque<?> NONE = new ArrayDeque<>(0);
        /** The troop's recent swings, oldest first (empty for none). Read only. */
        @SuppressWarnings("unchecked")
        public static Deque<MotionInput.Attack> attacks(int id) { Deque<MotionInput.Attack> q = ATTACKS.get(id); return q != null ? q : (Deque<MotionInput.Attack>) NONE; }
        /** The blows that recently struck the troop, oldest first (empty for none). Read only. */
        @SuppressWarnings("unchecked")
        public static Deque<MotionInput.Hit> hits(int id) { Deque<MotionInput.Hit> q = HITS.get(id); return q != null ? q : (Deque<MotionInput.Hit>) NONE; }
        /** The troop's stance (Stance.NONE for none). */
        public static Stance stance(int id) { return STANCES.getOrDefault(id, Stance.NONE); }
        static <T> void push(Map<Integer, Deque<T>> map, int id, T value) {
            Deque<T> q = map.computeIfAbsent(id, k -> new ConcurrentLinkedDeque<>()); q.addLast(value);
            while (q.size() > KEEP) q.pollFirst();
        }
        /** The most attack variants a family may have; a swing for a troop whose family this client cannot name is kept inside it (carry B7). */
        static final int MAX_VARIANTS = 64;
        /** attack's rows for an id the client world has no entity of, and for an entity the client cannot name as a troop. */
        static final int NO_ENTITY = -1, UNKNOWN_TROOP = -2;
        /**
         * A swing: rows is the attackVariants count of the troop's family (0 for a family with none, which drops every swing), UNKNOWN_TROOP when the
         * client cannot name the troop (its custom name is not one it knows: kept within MAX_VARIANTS, and TroopMotion validates the variant against
         * the model's family when it plays it), NO_ENTITY when the client world has no entity of this id (dropped). Dropped out of range, and dropped
         * when it repeats one already kept (carry B6: a catch-up and the live send may both bring the same swing).
         */
        static void attack(int id, int variant, long startTick, int rows) {
            if (rows == NO_ENTITY || rows < UNKNOWN_TROOP || !BattleMath.validVariant(variant, rows == UNKNOWN_TROOP ? MAX_VARIANTS : rows)) return;
            MotionInput.Attack a = new MotionInput.Attack(variant, startTick);
            Deque<MotionInput.Attack> q = ATTACKS.get(id);
            if (q != null && !MotionInput.admits(q, cancels(id), a)) return;
            push(ATTACKS, id, a);
        }
        /** Each troop's cancelled attacks (attack_v1 with variant CANCEL), newest last, the oldest dropped past KEEP. The attack itself stays kept, so AttackController fades it out from where it was. */
        static final Map<Integer, Deque<MotionInput.Cancel>> CANCELS = new ConcurrentHashMap<>();
        /** The troop's cancelled attacks (empty for none). Read only. */
        @SuppressWarnings("unchecked")
        public static Deque<MotionInput.Cancel> cancels(int id) { Deque<MotionInput.Cancel> q = CANCELS.get(id); return q != null ? q : (Deque<MotionInput.Cancel>) NONE; }
        /** The cancel for this attack of the troop, or null. */
        public static MotionInput.Cancel cancelled(int id, MotionInput.Attack a) { Deque<MotionInput.Cancel> q = CANCELS.get(id); return q == null ? null : MotionInput.cancelled(q, a.startTick()); }
        /** Cancels the client kept (cancel), for the checks. */
        static volatile int cancelsAccepted;
        /**
         * A cancel (coordinator 2026-11-11): the server dropped the announced attack of this start tick, heard at world tick now. Kept when MotionInput.cancel says
         * (a kept attack of that start, not cancelled yet, its clip within ATTACK_CLIP_MAX_TICKS of its start); any troop's cancel is obeyed. Returns whether it took.
         */
        static boolean cancel(int id, long startTick, long now) {
            Deque<MotionInput.Attack> q = ATTACKS.get(id); MotionInput.Cancel c = q == null ? null : MotionInput.cancel(q, cancels(id), startTick, now, BattleMath.ATTACK_CLIP_MAX_TICKS);
            if (c == null) return false;
            push(CANCELS, id, c); cancelsAccepted++; return true;
        }
        /** A blow: known when the client world has an entity of this id (else dropped); the weight clamped to 0..2. */
        static void hit(boolean known, int id, int kind, int zone, float bearing, int weight, int flags, long tick) {
            if (!known) return;
            String k = kind >= 0 && kind < KINDS.size() ? KINDS.get(kind) : "blunt", z = zone >= 0 && zone < ZONES.size() ? ZONES.get(zone) : "body";
            push(HITS, id, new MotionInput.Hit(k, z, bearing, BattleMath.weightOrdinal(weight), (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0, tick)); // higher bits (a newer server's) are ignored
        }
        static void stance(int id, int index, float aimPitch) {
            String name = index >= 0 && index < MotionEvents.STANCES.size() ? MotionEvents.STANCES.get(index) : "";
            if (name.isEmpty()) STANCES.remove(id); else STANCES.put(id, new Stance(name, aimPitch));
        }
        static void forget(int id) { ATTACKS.remove(id); CANCELS.remove(id); HITS.remove(id); STANCES.remove(id); RISES.remove(id); }
        static void clear() { ATTACKS.clear(); CANCELS.clear(); HITS.clear(); STANCES.clear(); RISES.clear(); }
    }
}
