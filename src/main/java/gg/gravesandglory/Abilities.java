package gg.gravesandglory;

import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationPropertyHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Troop abilities (spec 2, the abilities sheet). A troop's ability id names a row; the row's mechanic decides what it does. The shield is a tag
 * the shield logic and the counters table read; mend and blight behave here; the stances (steady, ambush, brace) act through the counters table
 * (the ability id is one of the troop's tags, and Combat.resolve sets the attacker group's effective order); pierce hits a second enemy behind
 * the target (secondary); the Oath Banner aura plants a Banner (Battle.banners) that lends its planter's army damage (auraFactor), keeps its
 * groups from routing (OATH_GUARD) and rallies them. Every other mechanic is a no-op until the task of its House.
 * Every number comes from the row, with the combatDiscipline defaults where a row says 0.
 */
final class Abilities {
    private Abilities() {}
    static final Spec.CombatDisciplineRow D = Spec.COMBATDISCIPLINE[0];

    /** The abilities row of a troop's ability id; the sheet's none row for an id it lacks. */
    static Spec.AbilitiesRow row(Spec.TroopsRow t) {
        Spec.AbilitiesRow none = null;
        for (var a : Spec.ABILITIES) { if (a.id().equals(t.ability())) return a; if (a.id().equals("none")) none = a; }
        return none;
    }

    /** The sheet's fearless row (mechanic fearless): amount is the strength share a fearless group breaks at, amount2 the share of the group that must carry it. Null when the sheet has none. */
    static Spec.AbilitiesRow fearlessRow() {
        for (var a : Spec.ABILITIES) if (a.mechanic().equals("fearless")) return a;
        return null;
    }

    /** Whether the troop carries the fearless ability (a Risen does). */
    static boolean carriesFearless(GravesAndGlory.Unit u) { return row(u.spec).mechanic().equals("fearless"); }

    /** The troop's ability row when it is a dread aura (mechanic aura, trigger passive, aimed at enemies: the Hollow King's), else null. */
    static Spec.AbilitiesRow dreadRow(GravesAndGlory.Unit u) {
        var row = row(u.spec);
        return row.mechanic().equals("aura") && row.trigger().equals("passive") && row.target().equals("enemies") ? row : null;
    }

    /** The beat's cast and aura triggers for a troop: a mender heals, a caster's spell bursts when its windup ends. enemy is the troop's current target, or null. */
    static void tick(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy, long time) {
        var row = row(u.spec);
        switch (row.mechanic()) {
            case "mend" -> mend(b, u, row, time);
            case "blight" -> burst(b, u, row, time);
            case "aura" -> aura(b, u, row, time);
            default -> {} // the stances (steady, ambush, brace) act through the counters table; every other mechanic waits for the task of its House
        }
    }

    /** A mender sends a heal to the most hurt ally in sight within the row's radius, once per cooldown. */
    static void mend(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (time < u.abilityAt) return;
        var mob = u.mob;
        double radius = row.radius() > 0 ? row.radius() : D.healRadius();
        var ally = b.units.stream().filter(v -> v.squad.team == u.squad.team && v.mob.isAlive() && v.mob.getHealth() < v.mob.getMaxHealth() && v.mob.squaredDistanceTo(mob) < radius * radius && mob.canSee(v.mob))
            .min(Comparator.comparingDouble(v -> v.mob.getHealth() / v.mob.getMaxHealth())).orElse(null);
        if (ally == null) return;
        double amount = row.amount() > 0 ? row.amount() : D.healAmount();
        Projectiles.fire(b, u, ally.mob, "mend", (float) (amount * b.campaign.damage(u)));
        u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
    }

    /** A caster's spell: the ring marks the centre while the windup runs; when it ends the burst hurts every enemy troop inside the radius and applies the row's status. */
    static void burst(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (u.spellCenter == null) return;
        double radius = row.radius() > 0 ? row.radius() : D.spellRadius();
        for (int i = 0; i < 12; i++) { double angle = i * Math.PI / 6; b.world.spawnParticles(ParticleTypes.WITCH, u.spellCenter.x + Math.cos(angle) * radius, u.spellCenter.y + .2, u.spellCenter.z + Math.sin(angle) * radius, 1, 0, 0, 0, 0); }
        if (time < u.windupUntil) return;
        float base = (float) ((row.amount() > 0 ? row.amount() : u.spec.damage()) * b.campaign.damage(u));
        for (var v : new java.util.ArrayList<>(b.units)) if (v.squad.team != u.squad.team && v.mob.isAlive() && v.mob.squaredDistanceTo(u.spellCenter) < radius * radius) {
            Combat.resolve(b, u, v.mob, base, "blight", false);
            if (row.statusTicks() > 0 && v.mob.isAlive()) v.mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, row.statusTicks(), row.statusLevel()));
        }
        u.spellCenter = null;
    }

    /**
     * The troop's attack when its cooldown is ready and its target is in reach: a caster starts its spell, a mender does not strike, anything
     * else shoots an arrow (a ranged troop) or hits (a melee one) through Combat.resolve.
     */
    static void attack(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity enemy, long time) {
        var row = row(u.spec);
        switch (row.mechanic()) {
            case "blight" -> {
                if (time < u.abilityAt) return;
                u.spellCenter = enemy.getPos();
                u.windupUntil = time + (row.windupTicks() > 0 ? row.windupTicks() : D.spellWindupTicks());
                u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
                Projectiles.bolt(b, u, u.spellCenter);
            }
            case "mend" -> {}
            default -> {
                float amount = (float) baseHit(b, u);
                // The hit is decided now; a ranged hit lands when its visible arrow arrives.
                if (u.spec.ranged()) Projectiles.fire(b, u, enemy, "arrow", amount); else Combat.resolve(b, u, enemy, amount, "melee", false);
            }
        }
    }

    /** A troop's base hit: its sheet damage times its army's progression (and every later damage bonus that belongs to the hit itself), before counters, auras and factors. attack and secondary both start here. */
    static double baseHit(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return u.spec.damage() * b.campaign.damage(u); }

    /**
     * What a landed hit sets off on its attacker's ability: pierce hits the nearest other enemy within the row's radius behind the target for the
     * row's amount (secondary); cleave hits the nearest other enemy within the row's radius of the target, any side of it, for the row's amount
     * (secondary); an onhit_slow row slows the target (slow). dealt is the amount applied. Combat.resolve skips it for a secondary hit. The
     * slams and pounce wait for the tasks of their Houses.
     */
    static void onHit(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity target, float dealt, String kind) {
        var row = row(u.spec);
        switch (row.mechanic()) {
            case "pierce" -> secondary(b, u, target, row.radius(), row.amount(), true, kind);
            case "cleave" -> secondary(b, u, target, row.radius(), row.amount(), false, kind);
            case "onhit_slow" -> { if (target.isAlive()) slow(target, row); }
            default -> {}
        }
    }

    /**
     * The row's slow on a target: Slowness at the row's statusLevel for its statusTicks, unless the target already has that level or more for
     * longer (the longer one stands). The rows' own cooldowns and knockback (Ensnare, Harpoon) belong to the tasks of their Houses.
     */
    static void slow(LivingEntity target, Spec.AbilitiesRow row) {
        var have = target.getStatusEffect(StatusEffects.SLOWNESS);
        if (have != null && have.getAmplifier() >= row.statusLevel() && have.getDuration() >= row.statusTicks()) return;
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, row.statusTicks(), row.statusLevel()));
    }

    /**
     * A second target for pierce and cleave: the nearest other living enemy (a troop, or a seated enemy commander in the field) within radius
     * of the primary target. With behind, only one beyond the target on the shot line: inside the cone of half-angle
     * 180 - morale.flankArcDegrees around the direction from the attacker through the target, the flank arc's "behind" that counters row C16
     * and morale read. It takes share x the attacker's base hit (its sheet damage times its progression) through Combat.resolve as a
     * secondary hit, so its own counters, traits, auras and commander factors apply once and no on-hit ability runs again. Returns the
     * amount applied, 0 when there was no second target or it took nothing.
     */
    static float secondary(GravesAndGlory.Battle b, GravesAndGlory.Unit u, LivingEntity primary, double radius, double share, boolean behind, String kind) {
        Vec3d at = primary.getPos();
        double dx = at.x - u.mob.getX(), dz = at.z - u.mob.getZ(), length = Math.hypot(dx, dz), cone = Math.cos(Math.toRadians(180 - CounterMath.flankDegrees()));
        List<LivingEntity> enemies = new ArrayList<>();
        for (var v : b.units) if (v.squad.team != u.squad.team && v.mob.isAlive()) enemies.add(v.mob);
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var other = b.squads.get(p.getUuid());
            if (other != null && other.seated && other.team != u.squad.team && p.isAlive() && !p.isSpectator() && p.getWorld() == b.world) enemies.add(p);
        }
        LivingEntity best = null;
        double nearest = radius * radius;
        for (var e : enemies) {
            if (e == primary) continue;
            double d = e.squaredDistanceTo(primary);
            if (d > nearest) continue;
            if (behind) {
                double vx = e.getX() - at.x, vz = e.getZ() - at.z, v = Math.hypot(vx, vz);
                if (length < 1e-9 || v < 1e-9 || (vx * dx + vz * dz) / (v * length) <= cone) continue;
            }
            best = e;
            nearest = d;
        }
        if (best == null) return 0;
        float applied = Combat.resolve(b, u, best, (float) (share * baseHit(b, u)), kind, true);
        if (applied > 0) BattleEffects.flash(b, new Vec3d(best.getX(), best.getBodyY(.6), best.getZ()), u.squad.team);
        return applied;
    }

    /** The reach of a troop's attack in blocks: its sheet range plus its commander's bonus (the Warden's ranged troops). Trait range bonuses (siege under Entrench) will add here. */
    static double range(GravesAndGlory.Battle b, GravesAndGlory.Unit u) { return u.spec.range() + Commander.rangeBonus(u.squad, u); }

    /**
     * The aura share of the factor: x(1 + amount) for an attacker of the planting army standing within the radius of its standing banner
     * (the Oath Banner), the largest one only when banners overlap, times x(1 - amount) while a living enemy troop with a dread row (the Hollow
     * King) stands within its radius, the strongest one only. Howl (the attacker's side) joins in the task of its House.
     */
    static double auraFactor(GravesAndGlory.Battle b, GravesAndGlory.Unit attacker, LivingEntity target) {
        double f = 1;
        for (var banner : b.banners) if (covers(b, banner, attacker.squad, attacker.mob.getPos())) f = Math.max(f, 1 + banner.amount());
        double dread = 1;
        for (var e : b.units) {
            if (e.squad.team == attacker.squad.team || !e.mob.isAlive()) continue;
            var row = dreadRow(e);
            if (row != null && GravesAndGlory.Battle.horizontalDistance(e.mob.getPos(), attacker.mob.getPos()) <= row.radius() * row.radius()) dread = Math.min(dread, 1 - row.amount());
        }
        return f * dread;
    }

    /**
     * A planted banner (the Lord Marshal's Oath Banner): the army (squad) whose troop planted it, and so the only one it serves (its aura, guard
     * and rally, as Bulwark and Stand Fast serve their own army: an allied army of the same team gets none), where it stands, the battle time it
     * falls, the radius of its aura and guard, and the damage share it adds (the row's amount). block is the banner block it set, null when no
     * spot was free, and was the block state that block replaced, put back when it falls. Battle.banners keeps the standing ones.
     */
    public record Banner(GravesAndGlory.Squad squad, Vec3d at, long until, double radius, double amount, BlockPos block, BlockState was) {
        /** A banner with no damage share and no block. */
        public Banner(GravesAndGlory.Squad squad, Vec3d at, long until, double radius) { this(squad, at, until, radius, 0, null, null); }
        /** The team its army fights for. */
        public int team() { return squad.team; }
    }

    /** Whether the banner stands now and serves this army at this spot (horizontal distance, as Morale measures). */
    static boolean covers(GravesAndGlory.Battle b, Banner banner, GravesAndGlory.Squad s, Vec3d at) {
        return banner.squad() == s && b.time < banner.until() && GravesAndGlory.Battle.horizontalDistance(at, banner.at()) <= banner.radius() * banner.radius();
    }

    /** The Oath Banner's rout guard: a group of the army that planted a standing banner, whose centre stands within its radius, cannot rout. */
    static final Morale.RoutGuard OATH_GUARD = (b, s, group) -> {
        if (b.banners.isEmpty()) return false;
        Vec3d centre = centre(b, s, group);
        if (centre == null) return false;
        for (var banner : b.banners) if (covers(b, banner, s, centre)) return true;
        return false;
    };

    /** The centre of a squad's group (its living members, as Morale.centre takes it), or null when none of them lives. The rout guards measure from here. */
    static Vec3d centre(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        List<GravesAndGlory.Unit> members = new ArrayList<>();
        for (var u : b.units) if (u.squad == s && u.spec.group().equals(group) && u.mob.isAlive()) members.add(u);
        return members.isEmpty() ? null : Morale.centre(members);
    }

    /**
     * An aura troop's beat. The Oath Banner (trigger hold) plants when the troop's group order, the stored one its commander gave, has turned
     * to Hold since its last beat and its cooldown is ready; the order it finds on its first beat is only remembered, so a round that starts
     * on Hold plants nothing. Howl and Dread wait for the tasks of their Houses.
     */
    static void aura(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        if (!row.trigger().equals("hold")) return;
        String order = b.storedAssignment(u.squad, u.spec.group()).order, was = u.seenOrder;
        u.seenOrder = order;
        if (was != null && !was.equals(order) && order.equals("hold") && time >= u.abilityAt) plant(b, u, row, time);
    }

    /**
     * Plants the Oath Banner: a standing banner block in the nearest dye to its team's colour, one step behind the troop against its group's
     * facing (or where it stands; no block when neither spot is free), that stands for the row's durationTicks with the row's radius and
     * amount. The rally horn sounds, every routing group of its own army inside the radius rallies at once, and the cooldown starts now.
     */
    static void plant(GravesAndGlory.Battle b, GravesAndGlory.Unit u, Spec.AbilitiesRow row, long time) {
        u.abilityAt = time + (row.cooldownTicks() > 0 ? row.cooldownTicks() : D.abilityCooldownTicks());
        float yaw = b.storedAssignment(u.squad, u.spec.group()).yaw;
        BlockPos block = spot(b, u, yaw);
        BlockState was = null;
        Vec3d at = u.mob.getPos();
        if (block != null) {
            was = b.world.getBlockState(block);
            b.world.setBlockState(block, BannerBlock.getForColor(dye(b.side(u.squad.team).color())).getDefaultState().with(BannerBlock.ROTATION, RotationPropertyHelper.fromYaw(yaw)));
            at = new Vec3d(block.getX() + .5, block.getY(), block.getZ() + .5);
        }
        var banner = new Banner(u.squad, at, time + row.durationTicks(), row.radius(), row.amount(), block, was);
        b.banners.add(banner);
        b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1, at.z, 16, .5, .8, .5, 0);
        BattleEffects.horn(b, u.squad, at, "rally");
        rally(b, banner);
    }

    /** One step behind the troop, against the facing yaw, then the spot it stands on: the first that is open (air, or a plant that is not two blocks tall), with air above, firm ground below, within 1.5 blocks of its feet and no other banner. Null when neither is. */
    static BlockPos spot(GravesAndGlory.Battle b, GravesAndGlory.Unit u, float yaw) {
        double r = Math.toRadians(yaw);
        Vec3d feet = u.mob.getPos();
        for (Vec3d at : List.of(feet.add(Math.sin(r) * 1.5, 0, -Math.cos(r) * 1.5), feet)) {
            int x = (int) Math.floor(at.x), z = (int) Math.floor(at.z), y = b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            if (Math.abs(y - feet.y) > 1.5 || b.banners.stream().anyMatch(banner -> p.equals(banner.block()))) continue;
            BlockState state = b.world.getBlockState(p);
            boolean open = state.isAir() || state.isReplaceable() && state.getFluidState().isEmpty() && !(state.getBlock() instanceof TallPlantBlock);
            if (open && b.world.getBlockState(p.up()).isAir() && b.world.getBlockState(p.down()).isSideSolidFullSquare(b.world, p.down(), Direction.UP)) return p;
        }
        return null;
    }

    /** The dye nearest an RGB colour (a banner block has only the sixteen). */
    static DyeColor dye(int rgb) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, blue = (rgb & 255) / 255f;
        DyeColor best = DyeColor.WHITE;
        double nearest = Double.MAX_VALUE;
        for (var d : DyeColor.values()) {
            float[] c = d.getColorComponents();
            double e = (c[0] - r) * (c[0] - r) + (c[1] - g) * (c[1] - g) + (c[2] - blue) * (c[2] - blue);
            if (e < nearest) { nearest = e; best = d; }
        }
        return best;
    }

    /** Ring particles on a banner's edge (a presentation number). */
    static final int RING = 32;

    /** The banner's beat in ticks: how often a standing banner rallies its army's routing groups and shows its particles (a presentation number; the radius and the rout guard are the row's). */
    static final int BEAT = 10;

    /** Every tick of a running round: a banner falls once its time is up (its block goes back); while it stands it rallies its army's routing groups in its radius and shows its particles, every BEAT ticks. */
    static void bannerTick(GravesAndGlory.Battle b) {
        if (b.banners.isEmpty()) return;
        for (var banner : new ArrayList<>(b.banners)) {
            if (b.time >= banner.until()) { lower(b, banner); b.banners.remove(banner); continue; }
            if (b.time % BEAT == 0) { rally(b, banner); show(b, banner); }
        }
    }

    /** Morale.rally for every routing group of the banner's own army (while it is seated) whose centre stands in its radius, with a burst of rally particles at the group. The plant rallies once at once; the scan repeats it while the banner stands. */
    static void rally(GravesAndGlory.Battle b, Banner banner) {
        var s = banner.squad();
        if (!s.seated) return;
        for (String group : Morale.GROUPS) {
            if (!Morale.routing(s, group)) continue;
            Vec3d centre = centre(b, s, group);
            if (centre == null || !covers(b, banner, s, centre)) continue;
            Morale.rally(b, s, group);
            b.world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, centre.x, centre.y + 1, centre.z, 12, 1, .5, 1, 0);
        }
    }

    /** The banner's look while it stands: a ring of its team's colour on the ground at the edge of its radius, and sparks and colour rising at its head. */
    static void show(GravesAndGlory.Battle b, Banner banner) {
        int rgb = b.side(banner.team()).color();
        ring(b, banner.at(), banner.radius(), rgb);
        Vec3d at = banner.at();
        b.world.spawnParticles(ParticleTypes.END_ROD, at.x, at.y + 2.1, at.z, 3, .3, .2, .3, .02);
        b.world.spawnParticles(dust(rgb), at.x, at.y + 1.2, at.z, 4, .3, .6, .3, 0);
    }

    /** A ring of RING dust particles in a colour on the ground at radius around a spot: the reach of a banner or of a commander's active. */
    static void ring(GravesAndGlory.Battle b, Vec3d at, double radius, int rgb) {
        var dust = dust(rgb);
        for (int i = 0; i < RING; i++) {
            double angle = i * 2 * Math.PI / RING, x = at.x + Math.cos(angle) * radius, z = at.z + Math.sin(angle) * radius;
            b.world.spawnParticles(dust, x, b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)) + .2, z, 1, 0, 0, 0, 0);
        }
    }

    /** Dust particles in an RGB colour. */
    static DustParticleEffect dust(int rgb) { return new DustParticleEffect(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.2f); }

    /** Takes a banner's block down: the block it replaced goes back, if the banner block still stands there. */
    static void lower(GravesAndGlory.Battle b, Banner banner) {
        if (banner.block() != null && b.world.getBlockState(banner.block()).getBlock() instanceof BannerBlock)
            b.world.setBlockState(banner.block(), banner.was() == null ? Blocks.AIR.getDefaultState() : banner.was());
    }

    /** Every banner falls now (startRound, returnLobby, switchMap, and the server stopping). */
    static void clearBanners(GravesAndGlory.Battle b) {
        for (var banner : b.banners) lower(b, banner);
        b.banners.clear();
    }

    /** Whether a shooter or its target stands in a Mist cloud (counters row C17). No clouds exist until the Tidecallers. */
    static boolean inMist(GravesAndGlory.Battle b, LivingEntity shooter, LivingEntity target) { return false; }
}
