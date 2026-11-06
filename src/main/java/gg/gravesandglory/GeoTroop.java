package gg.gravesandglory;

import net.minecraft.entity.EntityType;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The GeckoLib animatable of one rebuilt troop (unit animation rebuild, docs/anim-rebuild/PLAN.md). One instance stands for every
 * troop of that kind: GeckoLib keys each troop's animation state by its entity id. Client only; the server never sees it, so combat
 * timing stays where it was (Tactics.swing, PendingStrike, Combat.resolve).
 */
final class GeoTroop implements GeoReplacedEntity {
    /** The troop id (Spec troops row) this rig draws. */
    final String troop;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    GeoTroop(String troop) { this.troop = troop; }

    @Override public EntityType<?> getReplacingEntityType() { return EntityType.HUSK; }

    /** Blend time between locomotion clips, ticks. */
    static final int BLEND = 4;
    private static final RawAnimation[] LEGS = loops("legs"), UPPER = loops("upper");

    private static RawAnimation[] loops(String layer) {
        return new RawAnimation[] {RawAnimation.begin().thenLoop("idle_" + layer), RawAnimation.begin().thenLoop("walk_" + layer),
                RawAnimation.begin().thenLoop("run_" + layer)};
    }

    /**
     * Two layers per troop, one writer per bone (PLAN.md, Bone ownership): {@code legs} (hips and legs) and {@code upper} (torso,
     * arms, gear, cape, head). Registered legs first: it owns the shared stride clock. Then the {@code attack} layer, the one
     * exception: it weight-blends a swing over what {@code upper} wrote (AttackController). Before it, the {@code stance} layer blends
     * the order's pose (Hold, shield wall, raised shield) over the gait (StanceController), so a swing starts from and returns to it.
     * Then the {@code hit} layer adds blow reactions on top of everything (HitController). Last, the {@code death} layer: once the
     * troop dies it lerps every bone into the fall the killing blow called for (DeathController). Called once per troop entity.
     */
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        double speed = 0;
        for (Spec.TroopsRow r : Spec.TROOPS) if (r.id().equals(troop)) { speed = r.speed(); break; }
        Gait gait = new Gait(speed);
        controllers.add(new GaitController(this, "legs", BLEND, gait, true, s -> s.setAndContinue(LEGS[gait.mode.ordinal()])));
        controllers.add(new GaitController(this, "upper", BLEND, gait, false, s -> s.setAndContinue(UPPER[gait.mode.ordinal()])));
        StanceController stance = new StanceController(this, gait);
        controllers.add(stance);
        controllers.add(new AttackController(this, stance));
        controllers.add(new HitController(this, stance));
        controllers.add(new DeathController(this));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
