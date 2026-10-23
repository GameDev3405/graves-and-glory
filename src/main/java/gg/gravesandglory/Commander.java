package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The commander engine (spec section 5): six classes from the classes sheet, each with one active ability on the ability key and one
 * passive. Every number comes from the class row. The code knows only the effect kinds, named by the sheet's passive column (champion,
 * reaver, warden, marshal, warlock, quartermaster) and active column (stand_fast, bloodrush, mark_prey, rally_horn, grave_bloom,
 * forward_supply); tools/preflight.py keeps both columns to those closed lists. What an active does is registered in ACTIVES by the
 * House tasks (register): Stand Fast, Muster (the sheet's rally_horn), Grave Bloom, Forward Supply and Mark Prey so far. An active nobody registered refuses with NOT_READY and starts no cooldown.
 */
public final class Commander {
    private Commander() {}

    /**
     * One active ability. start returns false to refuse (no target in reach, say) and then no cooldown starts. tick runs every server
     * tick while the ability lasts (the row's durationTicks). end runs once when it stops: at expiry, at round start, in the lobby, and
     * when its commander leaves or his seat changes hands. Per-use state lives in Squad.abilityData, which is cleared after end.
     * Conventions the effects below read: Mark Prey keeps the marked entity (the LivingEntity or its UUID) in abilityData; Stand Fast's
     * damage share is takenFactor's (its active adds the Resistance I and the ring).
     */
    public interface Active {
        boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c);
        void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c);
        void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s);
    }

    /** The active abilities this build carries, by the classes sheet's active id; the House tasks fill it at mod init. */
    public static final Map<String, Active> ACTIVES = new ConcurrentHashMap<>();

    /** The notice for an active this build has no behaviour for yet. */
    public static final String NOT_READY = "This command is not ready in this build.";

    /** Fixed ids of the two attribute modifiers applyPlayer manages, so a new one always replaces the old. */
    static final UUID HEALTH = UUID.fromString("e01516bb-07f8-49b1-86c4-873d30106609"), ATTACK = UUID.fromString("8352fccf-a081-4a35-a122-2f5e5de6e1de");

    /** The fixed id of Muster's speed modifier on its army's troops. */
    static final UUID RALLY = UUID.fromString("8382ceb7-bd85-41fd-b662-34b5f1d36d95");

    /** Puts this build's actives into ACTIVES (mod init): the Champion's Stand Fast, the Marshal's Muster (the sheet's active id rally_horn), the Warlock's Grave Bloom, the Quartermaster's Forward Supply, the Warden's Mark Prey and the Reaver's Bloodrush. */
    static void register() {
        ACTIVES.put("stand_fast", new StandFast());
        ACTIVES.put("rally_horn", new Muster());
        ACTIVES.put("grave_bloom", new GraveBloom());
        ACTIVES.put("forward_supply", new ForwardSupply());
        ACTIVES.put("mark_prey", new MarkPrey());
        ACTIVES.put("bloodrush", new Bloodrush());
    }

    /**
     * The Champion's Stand Fast: for durationTicks his troops within radius take 1 - value1 (takenFactor), and he has Resistance I. A ring in
     * his side's colour marks the radius around him while it runs. It always starts. (No group routs, so it holds nobody: the no-routing design.)
     */
    static final class StandFast implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, c.durationTicks(), 0));
            Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            var p = player(b, s);
            if (p != null && b.time % 10 == 0) Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
        }
        /** The Resistance goes with it (it would run out now anyway at expiry; at round start, the lobby or a leave it goes early). */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
            var p = b.server.getPlayerManager().getPlayer(s.owner);
            if (p != null) p.removeStatusEffect(StatusEffects.RESISTANCE);
        }
    }

    /**
     * The Marshal's Muster (the sheet's active rally_horn, the Phase 1 Rally Horn): no horn sounds in the field (his horn is silent dress; the
     * commanding player alone hears the rally feedback, BattleEffects.feedback); every group takes a Move order to where he stands, through the
     * order path a player's order takes (Battle.issue), so the spot keeps the same rules: the ground there, and no Move at all while he stands in
     * river water ("Choose dry ground or a crossing.") or inside the arena's 10-block edge margin ("Too near the edge of the arena."); and his
     * troops move x(1 + value1) faster for durationTicks, a real +value1 on their ground (BattleMath.speedModifier), troops that join meanwhile too.
     * Once at him the groups fight as on Hold (Tactics.arrived). value2 is 0 and unused. It always starts.
     */
    static final class Muster implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            Vec3d to = p.getPos();
            BattleEffects.feedback(b, p, "rally");
            if (GravesAndGlory.Battle.inArena(to.x, to.z) && !GravesAndGlory.Battle.wet(to.x, to.z)) {
                Vec3d spot = GravesAndGlory.Battle.groundAt(to.x, to.z);
                for (var g : Spec.GROUPS) if (!g.id().equals("all")) b.issue(s, g.id(), "move", b.storedAssignment(s, g.id()).formation, spot, p.getYaw(), -1);
            } else b.campaign.notice(s.owner, GravesAndGlory.Battle.inArena(to.x, to.z) ? "Choose dry ground or a crossing." : "Too near the edge of the arena."); // river water, or the edge margin
            hasten(b, s, c);
            b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, to.x, to.y + 1, to.z, 20, 1, .6, 1, 0);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) { hasten(b, s, c); }
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
            for (var u : b.units) if (u.squad == s) { var speed = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED); if (speed != null) speed.removeModifier(RALLY); }
        }
        /** The speed modifier on every living troop of the army: the attribute change that makes a real x(1 + value1) on its ground (BattleMath.speedModifier: the square root on land, as it stands in water), added where it lacks and replaced where the ground changed it. */
        static void hasten(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            for (var u : b.units) {
                if (u.squad != s || !u.mob.isAlive()) continue;
                var speed = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
                if (speed == null) continue;
                double value = BattleMath.speedModifier(1 + c.value1(), u.mob.isTouchingWater());
                var have = speed.getModifier(RALLY);
                if (have != null && have.getValue() == value) continue;
                if (have != null) speed.removeModifier(RALLY);
                speed.addTemporaryModifier(new EntityAttributeModifier(RALLY, "Graves and Glory muster", value, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    /** The troop Grave Bloom raises, by its troops sheet id: a Risen. */
    static final String RISEN = "revenant";

    /**
     * The Warlock's Grave Bloom: the point he looks at (a raycast of the row's range, the end of it when he looks at nothing) raises up to
     * value2 of the troops of either side that fell within radius of it in the last value3 ticks (Battle.deaths), the ones nearest that point
     * first, as Risen of his own army. Each is a temporary troop (it never counts toward the slot check, a House tier, morale, mastery or the
     * report's losses) in a slot past the army's own, at value1 of its health where the fallen one lay, and is discarded after the row's
     * durationTicks (Houses.expire, and Houses.reset at a round's end). A fallen troop rises only once: it leaves the log. With none in reach the
     * cast is refused with a notice and no cooldown.
     */
    static final class GraveBloom implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            Vec3d aim = aim(p, c.range());
            double reach = c.radius() * c.radius();
            List<GravesAndGlory.Death> fallen = b.deaths.stream()
                .filter(d -> b.time - d.time() <= c.value3() && GravesAndGlory.Battle.horizontalDistance(d.at(), aim) <= reach)
                .sorted(Comparator.comparingDouble(d -> GravesAndGlory.Battle.horizontalDistance(d.at(), aim)))
                .limit(Math.round(c.value2())).toList();
            if (fallen.isEmpty()) { b.campaign.notice(s.owner, "No fallen troops there."); return false; }
            var spec = Campaign.troop(RISEN);
            Set<Integer> used = new HashSet<>();
            for (var u : b.units) if (u.squad == s) used.add(u.slot);
            int slot = GravesAndGlory.R.squadSize();
            for (var d : fallen) {
                while (used.contains(slot)) slot++;
                used.add(slot);
                var u = b.spawn(s, spec, slot, d.at());
                u.temporary = true;
                u.expiresAt = b.time + c.durationTicks();
                u.mob.setHealth((float) (c.value1() * u.mob.getMaxHealth()));
                BattleEffects.soul(b, d.at());
                b.deaths.remove(d);
            }
            Houses.recompute(s);
            return true;
        }
        /**
         * The point a commander looks at within range blocks: where his view ray first meets a block, else the end of the ray. It is worked out
         * from the player's yaw and pitch as they stand now; the entity's own raycast reads a rotation that lags a teleport by a tick.
         */
        static Vec3d aim(ServerPlayerEntity p, double range) {
            Vec3d eye = p.getEyePos(), end = eye.add(p.getRotationVector().multiply(range));
            return p.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, p)).getPos();
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {}
        /** The Risen it raised are not its to take back: each goes at its own expiresAt, and every one goes with the round. */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {}
    }

    /**
     * The Quartermaster's Forward Supply: a supply banner of value1 health (an ArmySupplyBanner) at the commander's feet, at least range blocks from
     * his own base. While it stands, and no enemy is within radius of it, troops trained for his army deploy at it instead of the barracks
     * (Battle.reinforcementAnchors); it stands for the row's durationTicks and goes when the active ends. Refused, with a notice and no cooldown,
     * too near his base (measured where the banner itself will stand, which can be a step behind his feet), outside the arena's margin or in river
     * water, or where no banner will stand.
     */
    static final class ForwardSupply implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            Vec3d at = p.getPos();
            var spot = Abilities.spot(b, at, p.getYaw());
            Vec3d where = spot == null ? at : new Vec3d(spot.getX() + .5, at.y, spot.getZ() + .5); // the banner's own place: its stand is in the middle of its block
            if (GravesAndGlory.Battle.horizontalDistance(where, GravesAndGlory.Battle.base(s.team)) < c.range() * c.range()) {
                b.campaign.notice(s.owner, "Too near your base. Plant it " + Math.round(c.range()) + " or more blocks away.");
                return false;
            }
            if (!GravesAndGlory.Battle.inArena(at.x, at.z) || GravesAndGlory.Battle.wet(at.x, at.z)) {
                b.campaign.notice(s.owner, "Choose dry ground inside the arena.");
                return false;
            }
            if (spot == null) { b.campaign.notice(s.owner, "No room for a banner here."); return false; }
            var banner = GravesAndGlory.ArmySupplyBanner.place(b, s, spot, p.getYaw(), (float) c.value1());
            if (banner == null) { b.campaign.notice(s.owner, "No room for a banner here."); return false; }
            s.abilityData = banner;
            Abilities.ring(b, banner.getPos(), c.radius(), b.side(s.team).color());
            b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, banner.getX(), banner.getY() + 1, banner.getZ(), 16, .5, .8, .5, 0);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {}
        /** The banner goes with the active: at expiry, at a round start, in the lobby, and when its commander leaves or his seat changes hands. */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
            for (var st : new ArrayList<>(b.structures)) if (st.squad() == s) st.discard();
        }
    }

    /**
     * The Warden's Mark Prey: the enemy troop or seated enemy commander his view ray meets first within the row's range (blocks in the way stop it)
     * glows for the row's durationTicks, with the vanilla Glowing effect, so both sides see it (a glow for his own side only needs a client renderer
     * that state_v8 does not carry: docs/PARKED.md), and takes x(1 + value1) from his troops and from himself (marked, which Combat.resolve and
     * Battle.allowDamage read through abilityData). Refused, with a notice and no cooldown, when the ray meets no foe. The glow goes when the active ends.
     */
    static final class MarkPrey implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            LivingEntity prey = pick(b, p, s, c.range());
            if (prey == null) { b.campaign.notice(s.owner, "No foe in sight."); return false; }
            prey.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, c.durationTicks(), 0, false, false));
            s.abilityData = prey;
            Abilities.ring(b, prey.getPos(), 1.5, b.side(s.team).color());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {}
        /** The glow goes with the mark, at expiry, at a round start, in the lobby, and when its commander leaves or his seat changes hands. */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
            if (s.abilityData instanceof LivingEntity prey && prey.isAlive()) prey.removeStatusEffect(StatusEffects.GLOWING);
        }
        /**
         * The nearest enemy troop or seated enemy commander on the commander's view ray within range blocks, a hand's breadth of margin round its box, that no block
         * stands in front of (the ray stops at the first collision shape); null when there is none. A supply banner is not prey.
         */
        static LivingEntity pick(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, double range) {
            Vec3d eye = p.getEyePos(), end = eye.add(p.getRotationVector().multiply(range));
            var wall = p.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
            double nearest = wall.getType() == net.minecraft.util.hit.HitResult.Type.MISS ? range : eye.distanceTo(wall.getPos());
            LivingEntity best = null;
            for (var e : Abilities.enemies(b, s.team)) {
                if (Combat.structure(e)) continue;
                var hit = e.getBoundingBox().expand(MARGIN).raycast(eye, end);
                if (hit.isEmpty()) continue;
                double d = eye.distanceTo(hit.get());
                if (d < nearest) { nearest = d; best = e; }
            }
            return best;
        }
        /** The margin round a foe's box the view ray may miss by and still pick it (blocks): a presentation number, so a mark lands on a troop the crosshair is on or beside. */
        static final double MARGIN = 0.3;
    }

    /**
     * The Reaver's Bloodrush: for durationTicks he has Speed II, his own blows by hand feed him value1 of what they deal (lifesteal, from Battle.allowDamage),
     * and each enemy troop he kills by hand (playerKill, from Battle's AFTER_DEATH) brings one troop back for nothing: the cheapest his army lost this round
     * and has not had back (Squad.fallen), at the back of his training queue (Battle.queueFree), at most value2 per use (the count in abilityData). It always starts.
     */
    static final class Bloodrush implements Active {
        /** Speed II: the fixed effect kind the spec names. */
        static final int SPEED_II = 1;
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, c.durationTicks(), SPEED_II));
            s.abilityData = new int[]{0};
            b.world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.PLAYERS, 1f, .6f);
            b.world.spawnParticles(ParticleTypes.CRIT, p.getX(), p.getBodyY(.5), p.getZ(), 24, .6, .6, .6, .2);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {}
        /** The Speed goes with it (it would run out now anyway at expiry; at round start, the lobby or a leave it goes early). */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
            var p = b.server.getPlayerManager().getPlayer(s.owner);
            if (p != null) p.removeStatusEffect(StatusEffects.SPEED);
        }
        /** One troop back for a kill: the cheapest fallen troop that the banner and the army's room allow, after the last of the queue; nothing past value2 refunds, or when none fits. */
        static void refund(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.ClassesRow c) {
            if (!(s.abilityData instanceof int[] given) || given[0] >= c.value2()) return;
            List<Spec.TroopsRow> fallen = new ArrayList<>();
            for (var e : s.fallen.entrySet()) if (e.getValue() > 0) { var t = Campaign.troop(e.getKey()); if (t != null) fallen.add(t); }
            fallen.sort(Comparator.comparingInt(Spec.TroopsRow::cost)); // a tie keeps the order they fell in
            for (var t : fallen) {
                if (!b.queueFree(s, t)) continue;
                s.fallen.merge(t.id(), -1, Integer::sum);
                given[0]++;
                b.campaign.notice(s.owner, "Bloodrush: " + t.name() + " returns to your banner for nothing.");
                return;
            }
        }
    }

    /** The Reaver's lifesteal (Bloodrush): his own blow by hand (the source is himself, not an arrow or a spell) heals him value1 of the amount while it runs. Battle.allowDamage calls it with the amount the target takes (a troop's damageScale applied). */
    static void lifesteal(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Entity attacker, DamageSource source, float amount) {
        if (!(attacker instanceof ServerPlayerEntity p) || source.getSource() != attacker || !running(b, s, "bloodrush")) return;
        p.heal((float) (amount * row(s).value1()));
    }

    /** An enemy troop the commander killed by his own hand (Battle's AFTER_DEATH, after the kill's gold): while his Bloodrush runs, one troop comes back (Bloodrush.refund). */
    static void playerKill(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { if (running(b, s, "bloodrush")) Bloodrush.refund(b, s, row(s)); }

    /** The class row a squad fights under: its commander id, or the sheet's first class for an unknown id. */
    public static Spec.ClassesRow row(GravesAndGlory.Squad s) { return row(s == null ? "" : s.commander); }

    /** The class row by id, or the sheet's first class for an unknown id. */
    public static Spec.ClassesRow row(String id) {
        for (var c : Spec.CLASSES) if (c.id().equals(id)) return c;
        return Spec.CLASSES[0];
    }

    /** The class whose active ability this is, or null (the state packet names the active, not the class). */
    public static Spec.ClassesRow byActive(String active) {
        for (var c : Spec.CLASSES) if (c.active().equals(active)) return c;
        return null;
    }

    /** An active id as the HUD tile and the class cards name it: stand_fast reads Stand Fast, bloodrush Bloodrush; rally_horn reads Muster (the no-routing design renamed it; the sheet id stays). */
    public static String activeName(String id) {
        if (id.equals("rally_horn")) return "Muster";
        StringBuilder out = new StringBuilder();
        for (String word : id.split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(" ");
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    /** Whether the class's group covers this troop: its own group, or every troop for the group all. */
    static boolean covers(Spec.ClassesRow c, Spec.TroopsRow t) { return c.group().equals("all") || c.group().equals(t.group()); }

    /** The commander in the field for a squad: its seated human owner, online in the battle world, alive, not a spectator and not waiting to respawn. Null for a bot. */
    public static ServerPlayerEntity player(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s == null || s.bot || !s.seated || b.respawns.containsKey(s.owner)) return null;
        var p = b.server.getPlayerManager().getPlayer(s.owner);
        return p == null || !p.isAlive() || p.isSpectator() || p.getWorld() != b.world ? null : p;
    }

    /** Whether the squad's active ability runs now. */
    public static boolean running(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return s != null && b.time < s.abilityUntil; }

    /** Whether the squad's active ability is this one and runs now. */
    static boolean running(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String active) { return running(b, s) && row(s).active().equals(active); }

    /**
     * The ability key (the "ability" command). Refused without a word while the commander is unseated, a bot, dead or waiting to
     * respawn, or the round is not running (its entrance included). An active this build has no behaviour for gives NOT_READY; one on
     * cooldown says when it is ready. Otherwise its start runs and, if it accepts, the cooldown and the duration begin now.
     */
    public static void activate(GravesAndGlory.Battle b, ServerPlayerEntity p) {
        var s = b.squads.get(p.getUuid());
        if (s == null || !b.running || b.time < b.campaign.introUntil || player(b, s) != p) return;
        var c = row(s);
        Active active = ACTIVES.get(c.active());
        if (active == null) { b.campaign.notice(p.getUuid(), NOT_READY); return; }
        if (b.time < s.abilityReadyAt) { b.campaign.notice(p.getUuid(), activeName(c.active()) + " is ready in " + (s.abilityReadyAt - b.time + 19) / 20 + " s."); return; }
        if (running(b, s) || !active.start(b, p, s, c)) return;
        s.abilityReadyAt = b.time + c.cooldownTicks();
        s.abilityUntil = b.time + c.durationTicks();
    }

    /** Runs each squad's active ability for its duration and stops it on time. Battle.tick calls it every tick of a running round after the entrance. */
    public static void tick(GravesAndGlory.Battle b) {
        for (var s : new ArrayList<>(b.squads.values())) {
            if (s.abilityUntil <= 0) continue;
            var c = row(s);
            Active active = ACTIVES.get(c.active());
            if (b.time >= s.abilityUntil || active == null) stop(b, s);
            else active.tick(b, s, c);
        }
    }

    /** Stops a squad's running active now (its end runs once) and clears abilityData. The cooldown stands. */
    public static void stop(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s == null) return;
        boolean was = s.abilityUntil > 0;
        s.abilityUntil = 0;
        Active active = ACTIVES.get(row(s).active());
        try { if (was && active != null) active.end(b, s); }
        finally { s.abilityData = null; }
    }

    /** A fresh round or the lobby: the active stops and the cooldown is clear. */
    public static void reset(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        stop(b, s);
        if (s != null) s.abilityReadyAt = 0;
    }

    /** Ticks until the squad's active may start again: 0 when ready, -1 when this build has no behaviour for its class active. The state packet sends it. */
    public static int readyIn(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s == null) return 0;
        if (!ACTIVES.containsKey(row(s).active())) return -1;
        return (int) Math.max(0, s.abilityReadyAt - b.time);
    }

    /** Ticks the squad's active still runs; 0 when it is not running. */
    public static int activeFor(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return s == null ? 0 : (int) Math.max(0, s.abilityUntil - b.time); }

    /**
     * The commander share of a troop's blow, on the attacker's side (Combat.resolve): the Champion's infantry within passiveRadius of
     * him x(1 + passiveValue2), the Warden's ranged troops x(1 + passiveValue), and x(1 + value1) on the entity his running Mark Prey marked.
     */
    public static double factor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target) {
        var s = attacker.squad;
        var c = row(s);
        double f = 1;
        if (c.passive().equals("champion") && covers(c, attacker.spec)) {
            var p = player(b, s);
            if (p != null && GravesAndGlory.Battle.horizontalDistance(p.getPos(), attacker.mob.getPos()) <= c.passiveRadius() * c.passiveRadius()) f *= 1 + c.passiveValue2();
        }
        if (c.passive().equals("warden") && covers(c, attacker.spec)) f *= 1 + c.passiveValue();
        return f * marked(b, s, target);
    }

    /** Mark Prey's share: x(1 + value1) when the squad's running mark_prey marked this entity (abilityData holds it or its UUID), else 1. */
    static double marked(GravesAndGlory.Battle b, GravesAndGlory.Squad s, LivingEntity target) {
        if (target == null || !running(b, s, "mark_prey")) return 1;
        Object mark = s.abilityData;
        return mark == target || target.getUuid().equals(mark) ? 1 + row(s).value1() : 1;
    }

    /** The commander share on the receiving side: x(1 - value1) for a troop whose squad's Stand Fast runs while it stands within radius of its commander, else 1. */
    public static double takenFactor(GravesAndGlory.Battle b, LivingEntity target) {
        var u = Combat.unitOf(b, target);
        if (u == null || !running(b, u.squad, "stand_fast")) return 1;
        var c = row(u.squad);
        var p = player(b, u.squad);
        return p != null && GravesAndGlory.Battle.horizontalDistance(p.getPos(), u.mob.getPos()) <= c.radius() * c.radius() ? 1 - c.value1() : 1;
    }

    /** The commander share of a blow a commander strikes himself (Battle.allowDamage): his own Mark Prey on its mark, and the target side's Stand Fast. */
    public static double playerFactor(GravesAndGlory.Battle b, GravesAndGlory.Squad attacker, LivingEntity target) { return marked(b, attacker, target) * takenFactor(b, target); }

    /** The Warden's extra reach for a troop of his group (passiveValue2 blocks); 0 otherwise. Abilities.range adds it. */
    public static double rangeBonus(GravesAndGlory.Squad s, GravesAndGlory.Unit u) {
        var c = row(s);
        return c.passive().equals("warden") && covers(c, u.spec) ? c.passiveValue2() : 0;
    }

    /** The share of the sheet training time a troop takes: the Marshal's 1 - passiveValue, else 1. */
    public static double trainingFactor(GravesAndGlory.Squad s) { return trainingFactor(row(s)); }
    static double trainingFactor(Spec.ClassesRow c) { return c.passive().equals("marshal") ? 1 - c.passiveValue() : 1; }

    /** The share of the sheet cost a troop costs in battle: the Warlock's 1 - passiveValue on magic troops, else 1. */
    public static double costFactor(GravesAndGlory.Squad s, Spec.TroopsRow t) { return costFactor(row(s), t); }
    static double costFactor(Spec.ClassesRow c, Spec.TroopsRow t) { return c.passive().equals("warlock") && t.magic() ? 1 - c.passiveValue() : 1; }

    /** The round gold a troop costs this squad: the sheet cost times costFactor, rounded. */
    public static int cost(GravesAndGlory.Squad s, Spec.TroopsRow t) { return cost(row(s), t); }
    /** The same for a class row (the barracks screen, from the class the state packet names). */
    public static int cost(Spec.ClassesRow c, Spec.TroopsRow t) { return BattleMath.classCost(t.cost(), costFactor(c, t)); }

    /** Ticks a troop trains for this squad: the sheet seconds times 20 times trainingFactor, rounded. */
    public static long trainingTicks(GravesAndGlory.Squad s, Spec.TroopsRow t) { return trainingTicks(row(s), t); }
    /** The same for a class row. */
    public static long trainingTicks(Spec.ClassesRow c, Spec.TroopsRow t) { return BattleMath.trainingTicks(t.trainingSeconds(), trainingFactor(c)); }

    /** The Quartermaster's extra round gold per held point per income tick (passiveValue, rounded); 0 otherwise. */
    public static int pointGoldBonus(GravesAndGlory.Squad s) {
        var c = row(s);
        return c.passive().equals("quartermaster") ? (int) Math.round(c.passiveValue()) : 0;
    }

    /** Seconds a fallen commander waits to return: the Reaver's passiveValue2, else rules.respawnSeconds. */
    public static int respawnSeconds(GravesAndGlory.Squad s) {
        var c = row(s);
        return c.passive().equals("reaver") ? (int) Math.round(c.passiveValue2()) : GravesAndGlory.R.respawnSeconds();
    }

    /**
     * The Champion's extra health and the Reaver's extra attack on the commander himself, as temporary attribute modifiers (never
     * saved with the player). The old ones always go first; then a seated human squad's class adds its own. A null or unseated squad
     * (the lobby, a class change in camp) leaves none. Health above the new maximum is cut back to it.
     */
    public static void applyPlayer(ServerPlayerEntity p, GravesAndGlory.Squad s) {
        var health = p.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        var attack = p.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (health != null) health.removeModifier(HEALTH);
        if (attack != null) attack.removeModifier(ATTACK);
        if (s != null && s.seated && !s.bot) {
            var c = row(s);
            if (health != null && c.passive().equals("champion") && c.passiveValue() != 0)
                health.addTemporaryModifier(new EntityAttributeModifier(HEALTH, "Graves and Glory commander health", c.passiveValue(), EntityAttributeModifier.Operation.ADDITION));
            if (attack != null && c.passive().equals("reaver") && c.passiveValue() != 0)
                attack.addTemporaryModifier(new EntityAttributeModifier(ATTACK, "Graves and Glory commander attack", c.passiveValue(), EntityAttributeModifier.Operation.ADDITION));
        }
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    }
}
