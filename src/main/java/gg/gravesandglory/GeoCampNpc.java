package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * The GeckoLib animatable of one camp NPC look (lobby NPC spec 3.1): one instance per campNpcs row, each villager drawn with it keyed by its
 * entity id, as GeoTroop is. Client only. One controller: the work loop ({@code idle}) until the villager swings its arm (a station opened, the
 * server's CampWorld.swing, seen by CampNpcRenderer), then {@code interact} once and back to the loop. The clip names are the contract with the
 * look's animation file (animations/camp_npc/&lt;rig&gt;.animation.json).
 */
final class GeoCampNpc implements GeoReplacedEntity {
    /** The work loop and the interact clip every look's animation file carries. */
    static final String IDLE_CLIP = "idle", INTERACT_CLIP = "interact";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(IDLE_CLIP);
    private static final RawAnimation INTERACT = RawAnimation.begin().thenPlay(INTERACT_CLIP).thenLoop(IDLE_CLIP);
    /** Blend between clips, ticks. */
    static final int BLEND = 3;

    /** The campNpcs row this rig draws. */
    final Spec.CampNpcsRow look;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    GeoCampNpc(Spec.CampNpcsRow look) { this.look = look; }

    @Override public EntityType<?> getReplacingEntityType() { return EntityType.VILLAGER; }

    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", BLEND, state -> {
            AnimationController<GeoCampNpc> c = state.getController();
            Entity e = state.getData(DataTickets.ENTITY);
            if (e != null && CampNpcRenderer.takeInteract(e)) { c.forceAnimationReset(); CampNpcRenderer.interactsPlayed++; return state.setAndContinue(INTERACT); }
            RawAnimation now = c.getCurrentRawAnimation();
            return state.setAndContinue(now == null ? IDLE : now);
        }));
    }

    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override public double getTick(Object entity) { return entity instanceof Entity e ? e.age : 0; }
}
