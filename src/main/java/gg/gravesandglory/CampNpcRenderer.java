package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.VillagerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * Every villager's renderer on a modded client (lobby NPC spec 3.1, Q3), registered over the vanilla one as TroopRenderers wraps the troops'
 * mobs: a camp NPC (CampNpcs.resolve: its custom name names a look and its AI is off) draws through its look's GeckoLib rig, and every other
 * villager (villages, campaign maps, babies) draws exactly as vanilla, since this is the vanilla renderer with only render and shouldRender
 * taking the camp NPC's branch. No entity type and no packet: the server's villager is all a plain client ever sees.
 * A swing of the villager's arm (the server's CampWorld.swing when its station opens; vanilla's entity-animation packet) starts the look's
 * interact clip (GeoCampNpc); the flag is taken here, so the next swing starts it again.
 */
public final class CampNpcRenderer extends VillagerEntityRenderer {
    /** One GeckoLib renderer per look id, made on first draw (renderers are rebuilt on every resource reload). */
    private final Map<String, Geo> geo = new HashMap<>();
    private final EntityRendererFactory.Context ctx;
    /** Villagers whose swing has not yet started their interact clip. */
    private static final Map<Entity, Boolean> SWUNG = new MapMaker().weakKeys().makeMap();
    /** Harness counters: camp NPC draws through a look, interact clips started, and the look the last drawn camp NPC wore (null before any). */
    static volatile long npcDraws, interactsPlayed;
    static volatile String lastLook;

    public CampNpcRenderer(EntityRendererFactory.Context ctx) { super(ctx); this.ctx = ctx; }

    /** The look a villager draws as, or null for vanilla. */
    static Spec.CampNpcsRow look(VillagerEntity e) {
        var name = e.getCustomName();
        return CampNpcs.resolve(name == null ? null : name.getString(), e.isAiDisabled());
    }

    @Override public void render(VillagerEntity e, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        Spec.CampNpcsRow l = look(e);
        if (l == null) { super.render(e, yaw, tickDelta, matrices, consumers, light); return; }
        if (e.handSwinging) { e.handSwinging = false; SWUNG.put(e, Boolean.TRUE); }
        geo.computeIfAbsent(l.id(), id -> new Geo(ctx, l)).render(e, yaw, tickDelta, matrices, consumers, light);
        npcDraws++; lastLook = l.id();
    }

    /** Whether this villager swung since its interact clip last started (taken once, by GeoCampNpc's controller). */
    static boolean takeInteract(Entity e) { return SWUNG.remove(e) != null; }

    /** A look's model reaches past the villager's box (a 1.3x smith, his anvil), so its frustum test takes a wider box. */
    @Override public boolean shouldRender(VillagerEntity e, Frustum frustum, double x, double y, double z) {
        Spec.CampNpcsRow l = look(e);
        if (l == null) return super.shouldRender(e, frustum, x, y, z);
        if (!e.shouldRender(x, y, z)) return false;
        double grow = Math.max(1, l.scale()) + 0.25;
        return e.ignoreCameraFrustum || frustum.isVisible(e.getBoundingBox().expand(grow, grow, grow));
    }

    /** The GeckoLib draw of one look: geo/camp_npc/&lt;rig&gt;.geo.json, animations/camp_npc/&lt;rig&gt;.animation.json and textures/entity/camp_npc/&lt;rig&gt;.png, at the row's scale. */
    static final class Geo extends GeoReplacedEntityRenderer<VillagerEntity, GeoCampNpc> {
        Geo(EntityRendererFactory.Context ctx, Spec.CampNpcsRow l) {
            super(ctx, new Model(l.rig()), new GeoCampNpc(l));
            withScale((float) l.scale());
            this.shadowRadius = .5f * (float) l.scale();
        }
    }

    static final class Model extends GeoModel<GeoCampNpc> {
        private final Identifier model, animations, texture;
        Model(String rig) {
            model = Identifier.of("gravesandglory", "geo/camp_npc/" + rig + ".geo.json");
            animations = Identifier.of("gravesandglory", "animations/camp_npc/" + rig + ".animation.json");
            texture = Identifier.of("gravesandglory", "textures/entity/camp_npc/" + rig + ".png");
        }
        @Override public Identifier getModelResource(GeoCampNpc animatable) { return model; }
        @Override public Identifier getTextureResource(GeoCampNpc animatable) { return texture; }
        @Override public Identifier getAnimationResource(GeoCampNpc animatable) { return animations; }
    }
}
