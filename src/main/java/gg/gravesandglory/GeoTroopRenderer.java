package gg.gravesandglory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
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
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Draws a rebuilt troop through GeckoLib (unit animation rebuild, docs/anim-rebuild/PLAN.md); TroopRenderer hands it the troops it
 * covers (handles). The rig is one model for every tier, face and side: the per-bone rules in troop_rig/&lt;troop&gt;.json (written by
 * art/anim/&lt;troop&gt;/build_rig.py) choose which bones show for the troop's armour tier, weapon tier, face and shield, and the texture
 * is the tier's sheet in the side's cloth. -Dgravesandglory.geoTroops=false draws the old model instead; =all also draws the PREVIEW troops.
 */
final class GeoTroopRenderer<E extends MobEntity> extends GeoReplacedEntityRenderer<E, GeoTroop> {
    private static final String MOD = "gravesandglory";
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("gravesandglory/geo");
    static final boolean ENABLED = !"false".equals(System.getProperty("gravesandglory.geoTroops"));
    /** The troops drawn this way: those Kort approved through the review booth (docs/anim-rebuild/REVIEW-BOOTH.md). Every other troop
     *  keeps the old model and clips until its batch is approved; then it moves from PREVIEW to here. */
    private static final Set<String> APPROVED = Set.of("knight");
    /** Rebuilt troops not yet approved: drawn this way only with -Dgravesandglory.geoTroops=all (lane testing). */
    private static final Set<String> PREVIEW = Set.of("sentinel", "revenant", "tc_raider", "archer", "longbow", "wm_hunter", "bonebow", "stalker", "ic_arbalist", "wm_tusk", "tc_tideguard", "tc_admiral", "ic_hearthguard", "ic_breaker", "gl_marshal", "tc_harpooner", "mage", "hexer", "tc_mist", "wm_seer", "wm_hound", "gb_hollowking");
    private static final boolean ALL = "all".equals(System.getProperty("gravesandglory.geoTroops"));
    /** Sides with a painted sheet; any other side draws in linen. */
    private static final Set<String> SIDES = Set.of("gold", "violet", "crimson", "teal", "company", "linen");

    /** Whether this troop draws through GeckoLib. */
    static boolean handles(TroopModels.Look l) { return ENABLED && l != null && (APPROVED.contains(l.troop().id()) || ALL && PREVIEW.contains(l.troop().id())); }

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

    GeoTroopRenderer(EntityRendererFactory.Context ctx, String troop) {
        this(ctx, new Model(troop), troop);
    }
    private GeoTroopRenderer(EntityRendererFactory.Context ctx, Model geo, String troop) {
        super(ctx, geo, new GeoTroop(troop));
        this.geo = geo;
        this.shadowRadius = .5f;
    }

    /** Draws one troop (its Look resolved and not held). */
    void draw(E e, TroopModels.Look l, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        rules();
        armour = MathHelper.clamp(l.armourTier(), 1, 3);
        weaponTier = MathHelper.clamp(l.weaponTier(), 1, 3);
        face = TroopFaces.pick(e.getUuid(), faces, -1, 0, TroopModels.dressed(e), TroopModels.forcedFace(e));
        shieldShown = !e.getOffHandStack().isEmpty();
        String cloth = TroopModels.clothOf(e, l), side = cloth != null && SIDES.contains(cloth) ? cloth : "linen";
        geo.texture = new Identifier(MOD, "textures/entity/troop/" + animatable.troop + "_geo/t" + armour + "_" + side + ".png");
        render(e, yaw, tickDelta, matrices, consumers, light);
        Probe pr = PROBE;
        if (pr != null) pr.seen(e, geo.getBakedModel(geo.getModelResource(animatable)), tickDelta);
    }

    /** Test harness only (null in play): called after each troop draw with the entity's baked model, whose bones then hold this draw's matrices once a probe asks for them. */
    interface Probe { void seen(net.minecraft.entity.Entity e, BakedGeoModel model, float tickDelta); }
    static volatile Probe PROBE;

    @Override
    public void preRender(MatrixStack poseStack, GeoTroop animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer,
                          boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        // Parents first (the rig file's order), so a shown bone never sits under a hidden one's hidden children.
        for (Rule r : rules) {
            boolean show = (!r.shield || shieldShown)
                && (r.stages.isEmpty() || r.stages.contains(r.weapon || r.frame >= 0 ? weaponTier : armour))
                && TroopFaces.shows(r.variants, face, faces);
            model.getBone(r.bone).ifPresent(b -> b.setHidden(!show));
        }
    }

    /** Turns the troop by its body yaw only, as the old renderer did: no vanilla death tip (the death clips will lay it down). */
    @Override
    protected void applyRotations(GeoTroop animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - rotationYaw));
    }

    /** Reads troop_rig/&lt;troop&gt;.json once per renderer (renderers are rebuilt on every resource reload). */
    private void rules() {
        if (rules != null) return;
        List<Rule> out = new ArrayList<>();
        int count = 1;
        Identifier id = new Identifier(MOD, "troop_rig/" + animatable.troop + ".json");
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
            model = new Identifier(MOD, "geo/troop/" + troop + ".geo.json");
            animations = new Identifier(MOD, "animations/troop/" + troop + ".animation.json");
            texture = new Identifier(MOD, "textures/entity/troop/" + troop + "_geo/t1_linen.png");
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
            CoreGeoBone draw = drawBones.isEmpty() ? null : getAnimationProcessor().getBone("draw");
            if (draw != null) {
                int frame = MathHelper.clamp(Math.round(Math.abs(draw.getPosX())), 0, FRAMES.size() - 1);
                for (var d : drawBones.entrySet()) if (d.getValue() != frame) { CoreGeoBone b = getAnimationProcessor().getBone(d.getKey()); if (b != null) b.setHidden(true); }
            }
            CoreGeoBone head = getAnimationProcessor().getBone("head");
            EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
            if (head == null || data == null) return;
            if (state.getData(DataTickets.ENTITY) instanceof net.minecraft.entity.LivingEntity l && (l.isDead() || l.deathTime > 0)) return;
            head.setRotX(head.getRotX() + MathHelper.clamp(data.headPitch(), -30f, 30f) * MathHelper.RADIANS_PER_DEGREE);
            head.setRotY(head.getRotY() + MathHelper.clamp(data.netHeadYaw(), -50f, 50f) * MathHelper.RADIANS_PER_DEGREE);
        }
    }
}
