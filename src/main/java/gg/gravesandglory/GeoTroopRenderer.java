package gg.gravesandglory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws a rebuilt troop through GeckoLib (unit animation rebuild, docs/anim-rebuild/PLAN.md); TroopRenderer hands it the troops it
 * covers (handles). The rig is one model for every tier, face and side: the per-bone rules in troop_rig/&lt;troop&gt;.json (written by
 * art/anim/&lt;troop&gt;/build_rig.py) choose which bones show for the troop's armour tier, weapon tier, face and shield, and the texture
 * is the tier's sheet in the side's cloth. A veteran (Proven, Mastery II: Look.veteran) also gets its tier's trim overlay,
 * &lt;troop&gt;_geo/veteran_t&lt;tier&gt;.png (built by tools/build_geo_trim.py from art/geo_trim/&lt;troop&gt;.json), redrawn over the
 * sheet on the same bones by the Trim layer; a troop or tier without that file draws plain. -Dgravesandglory.geoTroops=false draws the old model instead; =all also draws the PREVIEW troops (GeoTroops).
 */
final class GeoTroopRenderer<E extends MobEntity> extends GeoReplacedEntityRenderer<E, GeoTroop> {
    private static final String MOD = "gravesandglory";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("gravesandglory/geo");
    /** The approved and preview sets and the geoTroops flag live in GeoTroops (server-loadable: the bolt origin and the sweep follow them too). */
    static final boolean ENABLED = GeoTroops.ENABLED;
    /** Sides with a painted sheet; any other side draws in linen. */
    private static final Set<String> SIDES = Set.of("gold", "violet", "crimson", "teal", "company", "linen");

    /** Whether this troop draws through GeckoLib. */
    static boolean handles(TroopModels.Look l) { return l != null && GeoTroops.drawn(l.troop().id()); }

    /** One bone's show rule: armour or weapon stages (empty for always), face variants (empty for every face), and whether it is the shield's. */
    private record Rule(String bone, Set<Integer> stages, Set<Integer> variants, boolean weapon, boolean shield, int frame) {}
    /** A bow's draw frames (tag "draw", bone bow_t&lt;n&gt;_&lt;frame&gt;): the one shown is the "draw" bone's x position, which the clips key. */
    private static final List<String> FRAMES = List.of("rest", "pull_0", "pull_1", "pull_2");

    private final Model geo;
    private List<Rule> rules;
    private int faces;
    /** The current draw's choices (set by draw, read by preRender). */
    private int armour, weaponTier, face;
    private boolean shieldShown;
    /** The current draw's veteran trim (null for none), and the trim the Trim layer really drew in the last draw (harness: GeoDraws). */
    private Identifier trim;
    Identifier trimDrawn;
    /** Per armour tier (1 to 3): whether the troop ships a trim overlay for it; null until looked up (once per renderer, so per reload). */
    private final Boolean[] trims = new Boolean[4];

    GeoTroopRenderer(EntityRendererFactory.Context ctx, String troop) {
        this(ctx, new Model(troop), troop);
    }
    private GeoTroopRenderer(EntityRendererFactory.Context ctx, Model geo, String troop) {
        super(ctx, geo, new GeoTroop(troop));
        this.geo = geo;
        this.shadowRadius = .5f;
        addRenderLayer(new Trim(this));
    }

    /** The veteran trim over the worn tier's sheet (TroopFeatures.Trim's Geo counterpart): the same bones redrawn cutout with the overlay, lit as the sheet is. */
    private static final class Trim extends GeoRenderLayer<GeoTroop> {
        private final GeoTroopRenderer<?> r;
        Trim(GeoTroopRenderer<?> r) { super(r); this.r = r; }
        @Override
        public void render(MatrixStack poseStack, GeoTroop animatable, BakedGeoModel bakedModel, RenderLayer renderType, VertexConsumerProvider bufferSource,
                           VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
            Identifier t = r.trim;
            if (t == null) return;
            RenderLayer layer = RenderLayer.getEntityCutoutNoCull(t);
            r.reRender(bakedModel, poseStack, bufferSource, animatable, layer, bufferSource.getBuffer(layer), partialTick, packedLight, packedOverlay, 0xFFFFFFFF); // GeckoLib 4.9: one packed ARGB colour (white)
            r.trimDrawn = t;
        }
    }

    /** The veteran trim a Look draws through this renderer: its armour tier's overlay when it is a veteran and the troop has one, else null. */
    private Identifier trimFor(TroopModels.Look l) {
        if (!l.veteran()) return null;
        Identifier id = Identifier.of(MOD, "textures/entity/troop/" + animatable.troop + "_geo/veteran_t" + armour + ".png");
        if (trims[armour] == null) trims[armour] = MinecraftClient.getInstance().getResourceManager().getResource(id).isPresent();
        return trims[armour] ? id : null;
    }

    /** Draws one troop (its Look resolved and not held). */
    void draw(E e, TroopModels.Look l, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        rules();
        armour = MathHelper.clamp(l.armourTier(), 1, 3);
        weaponTier = MathHelper.clamp(l.weaponTier(), 1, 3);
        face = TroopFaces.pick(e.getUuid(), faces, -1, 0, TroopModels.dressed(e), TroopModels.forcedFace(e));
        shieldShown = !e.getOffHandStack().isEmpty();
        String cloth = TroopModels.clothOf(e, l), side = cloth != null && SIDES.contains(cloth) ? cloth : "linen";
        geo.texture = Identifier.of(MOD, "textures/entity/troop/" + animatable.troop + "_geo/t" + armour + "_" + side + ".png");
        trim = e.isInvisible() ? null : trimFor(l);
        trimDrawn = null;
        render(e, yaw, tickDelta, matrices, consumers, light);
        Probe pr = PROBE;
        if (pr != null) pr.seen(e, geo.getBakedModel(geo.getModelResource(animatable)), tickDelta);
    }

    /** Test harness only (null in play): called after each troop draw with the entity's baked model, whose bones then hold this draw's matrices once a probe asks for them. */
    interface Probe { void seen(net.minecraft.entity.Entity e, BakedGeoModel model, float tickDelta); }
    static volatile Probe PROBE;

    @Override
    public void preRender(MatrixStack poseStack, GeoTroop animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        // A re-render (the trim layer) draws the bones exactly as the main pass left them: setting the rules again would show a bow's
        // draw frames that setCustomAnimations hid after this pass's preRender.
        if (isReRender) return;
        // Parents first (the rig file's order), so a shown bone never sits under a hidden one's hidden children.
        for (Rule r : rules) {
            boolean show = (!r.shield || shieldShown)
                && (r.stages.isEmpty() || r.stages.contains(r.weapon || r.frame >= 0 ? weaponTier : armour))
                && TroopFaces.shows(r.variants, face, faces);
            model.getBone(r.bone).ifPresent(b -> b.setHidden(!show));
        }
    }

    /** Turns the troop by its body yaw only, as the old renderer did: no vanilla death tip (the death clips will lay it down).
     *  GeckoLib 4.9 calls only this 6-argument form (with nativeScale); the old 5-argument override would be skipped silently. */
    @Override
    protected void applyRotations(GeoTroop animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - rotationYaw));
    }

    /** Reads troop_rig/&lt;troop&gt;.json once per renderer (renderers are rebuilt on every resource reload). */
    private void rules() {
        if (rules != null) return;
        List<Rule> out = new ArrayList<>();
        int count = 1;
        Identifier id = Identifier.of(MOD, "troop_rig/" + animatable.troop + ".json");
        var res = MinecraftClient.getInstance().getResourceManager().getResource(id);
        if (res.isPresent()) {
            try (Reader reader = res.get().getReader()) {
                JsonObject rig = JsonParser.parseReader(reader).getAsJsonObject();
                count = rig.has("faces") ? rig.get("faces").getAsInt() : 1;
                for (var entry : rig.getAsJsonObject("bones").entrySet()) {
                    JsonObject b = entry.getValue().getAsJsonObject();
                    String tag = b.has("tag") ? b.get("tag").getAsString() : "";
                    int frame = -1;
                    if (tag.equals("draw")) for (int i = 0; i < FRAMES.size(); i++) if (entry.getKey().endsWith("_" + FRAMES.get(i))) frame = i;
                    out.add(new Rule(entry.getKey(), ints(b, "stages"), ints(b, "variants"), tag.equals("weapon"), tag.equals("shield"), frame));
                }
            } catch (Exception ex) {
                LOG.error("Could not read {}", id, ex);
            }
        } else LOG.error("Missing troop rig {}", id);
        faces = count;
        rules = out;
        java.util.Map<String, Integer> draws = new java.util.HashMap<>();
        for (Rule r : out) if (r.frame >= 0) draws.put(r.bone, r.frame);
        geo.drawBones = draws;
    }

    private static Set<Integer> ints(JsonObject o, String key) {
        Set<Integer> s = new HashSet<>();
        if (o.has(key)) for (JsonElement e : o.getAsJsonArray(key)) s.add(e.getAsInt());
        return s;
    }

    /** The rig's files: geo/troop/&lt;troop&gt;.geo.json, animations/troop/&lt;troop&gt;.animation.json, and the draw's texture. */
    static final class Model extends GeoModel<GeoTroop> {
        private final Identifier model, animations;
        Identifier texture;
        /** The bow's draw-frame bones and their frame (0 rest to 3 full draw); empty for a troop without a bow. Set by rules(). */
        java.util.Map<String, Integer> drawBones = java.util.Map.of();
        Model(String troop) {
            model = Identifier.of(MOD, "geo/troop/" + troop + ".geo.json");
            animations = Identifier.of(MOD, "animations/troop/" + troop + ".animation.json");
            texture = Identifier.of(MOD, "textures/entity/troop/" + troop + "_geo/t1_linen.png");
        }
        @Override public Identifier getModelResource(GeoTroop animatable) { return model; }
        @Override public Identifier getTextureResource(GeoTroop animatable) { return texture; }
        @Override public Identifier getAnimationResource(GeoTroop animatable) { return animations; }

        /**
         * The head looks where the troop looks, on top of the clip's own head motion (the clips counter-stabilise the head against
         * the body; this adds the aim). Every upper clip keys the head each frame, so adding never accumulates. Clamped to what a
         * neck can do; a dead troop's head stays with the clip.
         */
        @Override
        public void setCustomAnimations(GeoTroop animatable, long instanceId, AnimationState<GeoTroop> state) {
            // The bow shows the draw frame the clips key on the "draw" bone's x position. This runs per troop after its controllers
            // (preRender runs before them and shows the tier's frames), so it only hides the frames not drawn now.
            GeoBone draw = drawBones.isEmpty() ? null : getAnimationProcessor().getBone("draw");
            if (draw != null) {
                int frame = MathHelper.clamp(Math.round(Math.abs(draw.getPosX())), 0, FRAMES.size() - 1);
                for (var d : drawBones.entrySet()) if (d.getValue() != frame) { GeoBone b = getAnimationProcessor().getBone(d.getKey()); if (b != null) b.setHidden(true); }
            }
            GeoBone head = getAnimationProcessor().getBone("head");
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (head == null || data == null) return;
            if (state.getData(DataTickets.ENTITY) instanceof net.minecraft.entity.LivingEntity l && (l.isDead() || l.deathTime > 0)) return;
            head.setRotX(head.getRotX() + MathHelper.clamp(data.headPitch(), -30f, 30f) * MathHelper.RADIANS_PER_DEGREE);
            head.setRotY(head.getRotY() + MathHelper.clamp(data.netHeadYaw(), -50f, 50f) * MathHelper.RADIANS_PER_DEGREE);
        }
    }
}
