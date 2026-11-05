package gg.gravesandglory;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.attribute.EntityAttributes;
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
 * What each relic does (gear-combat Task 5): the bodies by relicTuning effect id, ported unchanged from the six class actives except that
 * they read the relic's tuning row (durationTicks, radius, range, value1..3) where they read the class row, and keep their per-use state in
 * their own slot (Squad.relicState.data[slot]). Commander runs the slots: the keys, the clocks and the stops.
 */
public final class RelicEffects {
    private RelicEffects() {}
    /**
     * One active ability. start returns false to refuse (no target in reach, say) and then no cooldown starts. tick runs every server
     * tick while the ability lasts (the row's durationTicks). end runs once when it stops: at expiry, at round start, in the lobby, and
     * when its commander leaves or his seat changes hands. Per-use state lives in Squad.relicState.data, which is cleared after end.
     * Conventions the effects below read: Mark Prey keeps the marked entity (the LivingEntity or its UUID) in its slot's data; Stand Fast's
     * damage share is takenFactor's (its active adds the Resistance I and the ring).
     */
    public interface Active {
        boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c);
        void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c);
        void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot);
    }

    /** The active abilities this build carries, by the classes sheet's active id; the House tasks fill it at mod init. */
    public static final Map<String, Active> ACTIVES = new ConcurrentHashMap<>();

    /** The notice for an active this build has no behaviour for yet. */
    public static final String NOT_READY = "This command is not ready in this build.";

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
        ACTIVES.put("dash", new Dash());
        ACTIVES.put("shove", new Shove());
        ACTIVES.put("heal_pulse", new HealPulse());
        ACTIVES.put("volley", new Volley());
        ACTIVES.put("war_cry", new WarCry());
        ACTIVES.put("self_heal", new SelfHeal());
        ACTIVES.put("iron_skin", ACTIVES.get("self_heal"));
    }

    /**
     * The Champion's Stand Fast: for durationTicks his troops within radius take 1 - value1 (takenFactor), and he has Resistance I. A ring in
     * his side's colour marks the radius around him while it runs. It always starts. (No group routs, so it holds nobody: the no-routing design.)
     */
    static final class StandFast implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, c.durationTicks(), 0));
            Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            var p = Commander.player(b, s);
            if (p != null && b.time % 10 == 0) Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
        }
        /** The Resistance goes with it (it would run out now anyway at expiry; at round start, the lobby or a leave it goes early). */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
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
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
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
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) { hasten(b, s, c); }
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
            for (var u : b.units) if (u.squad == s) { var speed = u.mob.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED); if (speed != null) speed.removeModifier(RALLY); }
        }
        /** The speed modifier on every living troop of the army: the attribute change that makes a real x(1 + value1) on its ground (BattleMath.speedModifier: the square root on land, as it stands in water), kept by Abilities.keepSpeed (added where it lacks, replaced where the ground changed it). */
        static void hasten(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Spec.RelicTuningRow c) {
            for (var u : b.units) if (u.squad == s && u.mob.isAlive()) Abilities.keepSpeed(u.mob, RALLY, "Graves and Glory muster", BattleMath.speedModifier(1 + c.value1(), u.mob.isTouchingWater()));
        }
    }


    /**
     * The Warlock's Grave Bloom: the point he looks at (a raycast of the row's range, the end of it when he looks at nothing) raises up to
     * value2 of the troops of either side that fell within radius of it in the last value3 ticks (Battle.deaths), the ones nearest that point
     * first, as Risen of his own army. Each is a temporary troop (it never counts toward the slot check, a House tier, morale, mastery or the
     * report's losses) in a slot past the army's own, at value1 of its health where the fallen one lay, and is discarded after the row's
     * durationTicks (Houses.expire, and Houses.reset at a round's end). A fallen troop rises only once: it leaves the log, and its body, if it still
     * lies, is discarded as it rises (Task 7, R33). With none in reach the
     * cast is refused with a notice and no cooldown.
     */
    static final class GraveBloom implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            Vec3d aim = aim(p, c.range());
            double reach = c.radius() * c.radius();
            List<GravesAndGlory.Death> fallen = b.deaths.stream()
                .filter(d -> b.time - d.time() <= c.value3() && GravesAndGlory.Battle.horizontalDistance(d.at(), aim) <= reach)
                .sorted(Comparator.comparingDouble(d -> GravesAndGlory.Battle.horizontalDistance(d.at(), aim)))
                .limit(Math.round(c.value2())).toList();
            if (fallen.isEmpty()) { b.campaign.notice(s.owner, "No fallen troops there."); return false; }
            var spec = Campaign.troop(Commander.RISEN);
            for (var d : fallen) {
                var u = Commander.temporary(b, s, spec, d.at(), b.time + c.durationTicks());
                u.mob.setHealth((float) (c.value1() * u.mob.getMaxHealth()));
                b.deaths.remove(d);
                // its body, if it still lies there, goes as it rises (Task 7, R33): no Risen stands over its own corpse
                if (d.body() != null && !d.body().isRemoved() && d.body() instanceof LivingEntity dead && dead.isDead()) {GravesAndGlory.Battle.leaveSide(dead); dead.discard();}
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
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        /** The Risen it raised are not its to take back: each goes at its own expiresAt, and every one goes with the round. */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }

    /**
     * The Quartermaster's Forward Supply: a supply banner of value1 health (an ArmySupplyBanner) at the commander's feet, at least range blocks from
     * his own base. While it stands, and no enemy is within radius of it, troops trained for his army deploy at it instead of the barracks
     * (Battle.reinforcementAnchors); it stands for the row's durationTicks and goes when the active ends. Refused, with a notice and no cooldown,
     * too near his base (measured where the banner itself will stand, which can be a step behind his feet), outside the arena's margin or in river
     * water, or where no banner will stand.
     */
    static final class ForwardSupply implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            Vec3d at = p.getPos();
            var spot = Abilities.spot(b, at, p.getYaw());
            Vec3d where = spot == null ? at : new Vec3d(spot.getX() + .5, at.y, spot.getZ() + .5); // the banner's own place: its stand is in the middle of its block
            if (GravesAndGlory.Battle.horizontalDistance(where, b.fieldBase(s.team)) < c.range() * c.range()) {
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
            s.relicState.data[slot] = banner;
            Abilities.ring(b, banner.getPos(), c.radius(), b.side(s.team).color());
            b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, banner.getX(), banner.getY() + 1, banner.getZ(), 16, .5, .8, .5, 0);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        /** The banner goes with the active: at expiry, at a round start, in the lobby, and when its commander leaves or his seat changes hands. */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
            for (var st : new ArrayList<>(b.structures)) if (st.squad() == s) st.discard();
        }
    }

    /**
     * The Warden's Mark Prey: the enemy troop or seated enemy commander his view ray meets first within the row's range (blocks in the way stop it)
     * glows for the row's durationTicks, with the vanilla Glowing effect, so both sides see it (a glow for his own side only needs a client renderer
     * that state_v8 does not carry: docs/PARKED.md), and takes x(1 + value1) from his troops and from himself (marked, which Combat.resolve and
     * Battle.allowDamage read through its slot's data). Refused, with a notice and no cooldown, when the ray meets no foe. The glow goes when the active ends.
     */
    static final class MarkPrey implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            LivingEntity prey = pick(b, p, s, c.range());
            if (prey == null) { b.campaign.notice(s.owner, "No foe in sight."); return false; }
            prey.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, c.durationTicks(), 0, false, false));
            s.relicState.data[slot] = prey;
            Abilities.ring(b, prey.getPos(), 1.5, b.side(s.team).color());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        /** The glow goes with the mark, at expiry, at a round start, in the lobby, and when its commander leaves or his seat changes hands; from a body that still lies too (Task 7). */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
            if (s.relicState.data[slot] instanceof LivingEntity prey && !prey.isRemoved()) prey.removeStatusEffect(StatusEffects.GLOWING);
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
     * The Reaver's Bloodrush: for durationTicks he has Speed II, his own blows by hand feed him value1 of what their target really loses (lifesteal, from
     * Battle.allowDamage), and each enemy troop he kills by hand (playerKill, from Battle's AFTER_DEATH: the killing blow's source is himself) brings one troop
     * back for nothing: the cheapest his army lost this round and has not had back (Squad.fallen), at the back of his training queue (Battle.queueFree), at most
     * value2 per use (the count in its State, Squad.relicState.data). It always starts.
     */
    static final class Bloodrush implements Active {
        /** Speed II: the fixed effect kind the spec names. */
        static final int SPEED_II = 1;
        /** What a running Bloodrush keeps (Squad.relicState.data): the refunds given so far, and the battle time the Speed it gave runs out. */
        static final class State { int given; long speedUntil; }
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, c.durationTicks(), SPEED_II));
            var st = new State(); st.speedUntil = b.time + c.durationTicks(); s.relicState.data[slot] = st;
            Audio.at(b, AudioCues.Cue.RELIC_USE_DARK, p.getPos(), "r" + p.getId());
            b.world.spawnParticles(ParticleTypes.CRIT, p.getX(), p.getBodyY(.5), p.getZ(), 24, .6, .6, .6, .2);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        /** The Speed it gave goes with it (it would run out now anyway at expiry; at round start, the lobby or a leave it goes early): the Speed on him is taken off only while it is still that one, a Speed II running no longer than the rush had left; a stronger or longer Speed from elsewhere stays. */
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {
            var p = b.server.getPlayerManager().getPlayer(s.owner);
            if (p == null || !(s.relicState.data[slot] instanceof State st)) return;
            var speed = p.getStatusEffect(StatusEffects.SPEED);
            if (speed != null && speed.getAmplifier() == SPEED_II && b.time + speed.getDuration() <= st.speedUntil + 1) p.removeStatusEffect(StatusEffects.SPEED);
        }
        /** One troop back for a kill: the cheapest fallen troop that the banner and the army's room allow, after the last of the queue; nothing past value2 refunds, or when none fits. */
        static void refund(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            if (!(s.relicState.data[slot] instanceof State st) || st.given >= c.value2()) return;
            List<Spec.TroopsRow> fallen = new ArrayList<>();
            for (var e : s.fallen.entrySet()) if (e.getValue() > 0) { var t = Campaign.troop(e.getKey()); if (t != null) fallen.add(t); }
            fallen.sort(Comparator.comparingInt(Spec.TroopsRow::cost)); // a tie keeps the order they fell in
            for (var t : fallen) {
                if (!b.queueFree(s, t)) continue;
                s.fallen.merge(t.id(), -1, Integer::sum);
                st.given++;
                b.campaign.notice(s.owner, "Bloodrush: " + t.name() + " returns to your banner for nothing.");
                return;
            }
        }
    }

    // ---- The new relics (gear-combat plan Task 6). Each reads only its relicTuning row.

    /** The commander's look laid flat (no pitch), a unit vector; his yaw's when he looks straight up or down. */
    static Vec3d flatLook(ServerPlayerEntity p) {
        Vec3d look = p.getRotationVector(), flat = new Vec3d(look.x, 0, look.z);
        return flat.lengthSquared() < 1e-6 ? Vec3d.fromPolar(0, p.getYaw()) : flat.normalize();
    }

    /** Charger's Spur (dash): his horizontal velocity becomes value1 blocks a tick along his flat look, at once; for durationTicks he takes no fall. It always starts. */
    static final class Dash implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            Vec3d v = flatLook(p).multiply(c.value1());
            p.setVelocity(v.x, Math.max(p.getVelocity().y, 0.1), v.z);
            p.velocityModified = true;
            p.fallDistance = 0;
            b.world.spawnParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + .2, p.getZ(), 10, .3, .1, .3, .02);
            Audio.at(b, AudioCues.Cue.RELIC_USE_ICE, p.getPos(), "r" + p.getId());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            var p = Commander.player(b, s);
            if (p != null) p.fallDistance = 0;
        }
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }

    /** The foes (troops and seated commanders of the other side) within radius of the commander and in front of him (the half his flat look faces). */
    static List<LivingEntity> inFront(GravesAndGlory.Battle b, ServerPlayerEntity p, int team, double radius) {
        Vec3d look = flatLook(p);
        List<LivingEntity> out = new ArrayList<>();
        for (var e : b.world.getOtherEntities(p, p.getBoundingBox().expand(radius, 2, radius))) {
            if (!(e instanceof LivingEntity le) || !GearCombat.foe(b, le, team) || e.squaredDistanceTo(p) > radius * radius) continue;
            Vec3d to = new Vec3d(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
            if (to.lengthSquared() < 1e-6 || to.normalize().dotProduct(look) >= 0) out.add(le);
        }
        return out;
    }

    /**
     * Gatebreaker's Shove (shove, the relic shield_bash): every foe within radius in front of him takes value3 as his own blow (Battle.allowDamage scales it
     * like any blow of his), a knockback of strength value1 away from him and Slowness II for value2 ticks. With no foe there it is refused with a notice and no cooldown.
     */
    static final class Shove implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            var hit = inFront(b, p, s.team, c.radius());
            if (hit.isEmpty()) { b.campaign.notice(s.owner, "No foe in front of you."); return false; }
            for (var le : hit) {
                le.timeUntilRegen = 0;
                le.damage(b.world.getDamageSources().playerAttack(p), (float) c.value3());
                le.takeKnockback(c.value1(), p.getX() - le.getX(), p.getZ() - le.getZ());
                le.velocityModified = true;
                Abilities.slow(b, le, 1, (int) Math.round(c.value2()));
            }
            Vec3d look = flatLook(p);
            b.world.spawnParticles(ParticleTypes.SWEEP_ATTACK, p.getX() + look.x, p.getBodyY(.5), p.getZ() + look.z, 3, .4, .2, .4, 0);
            Audio.at(b, AudioCues.Cue.RELIC_USE_EARTH, p.getPos(), "r" + p.getId());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }

    /** Lantern of Mercy (heal_pulse): he and his own troops within radius heal value1 x his healingFactor at once. It always starts. */
    static final class HealPulse implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            float heal = (float) (c.value1() * GearStats.healingFactor(s.gear));
            p.heal(heal);
            for (var u : b.units) if (u.squad == s && u.mob.isAlive() && u.mob.squaredDistanceTo(p) <= c.radius() * c.radius()) u.mob.heal(heal);
            Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
            b.world.spawnParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 6, .5, .3, .5, 0);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }

    /**
     * Hunter's Volley (volley): value1 arrows of his own (no pickup) from his eyes, fanned evenly across value2 degrees about his look, each at value3 blocks a
     * tick. They hit as his shots (rangedFactor, hawkeye; pierce from GearCombat.onEntityLoad). It always starts.
     */
    static final class Volley implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            int n = (int) Math.max(1, Math.round(c.value1()));
            for (int i = 0; i < n; i++) {
                float yaw = p.getYaw() + (n == 1 ? 0f : (float) (-c.value2() / 2 + c.value2() * i / (n - 1)));
                var a = new ArrowEntity(b.world, p);
                a.setVelocity(p, p.getPitch(), yaw, 0f, (float) c.value3(), 0f);
                a.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                b.world.spawnEntity(a);
            }
            b.world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, .9f);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {}
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }

    /** Battle Cry (war_cry): for durationTicks his own troops within radius of him hit x(1 + value1) (Commander.factor reads it per blow). It always starts. */
    static final class WarCry implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
            Audio.at(b, AudioCues.Cue.RELIC_USE_FIRE, p.getPos(), "r" + p.getId());
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            var p = Commander.player(b, s);
            if (p != null && b.time % 20 == 0) Abilities.ring(b, p.getPos(), c.radius(), b.side(s.team).color());
        }
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }

    /** Second Wind (self_heal, the relic iron_skin): he heals value1 x his healingFactor, spread evenly over durationTicks. Refused with a notice at full health. */
    static final class SelfHeal implements Active {
        @Override public boolean start(GravesAndGlory.Battle b, ServerPlayerEntity p, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            if (p.getHealth() >= p.getMaxHealth()) { b.campaign.notice(s.owner, "You are not hurt."); return false; }
            b.world.spawnParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.8, p.getZ(), 3, .3, .2, .3, 0);
            return true;
        }
        @Override public void tick(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot, Spec.RelicTuningRow c) {
            var p = Commander.player(b, s);
            if (p != null && c.durationTicks() > 0) p.heal((float) (c.value1() * GearStats.healingFactor(s.gear) / c.durationTicks()));
        }
        @Override public void end(GravesAndGlory.Battle b, GravesAndGlory.Squad s, int slot) {}
    }
}
