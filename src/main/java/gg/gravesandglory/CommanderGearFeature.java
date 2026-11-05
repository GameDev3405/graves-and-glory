package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import java.util.EnumMap;
import java.util.Map;

/**
 * Draws the commander gear a player wears with its own models (GearModels), in place of the vanilla stand-ins (GearLooks): each
 * armour piece rides on the biped parts it names, its skin bones in the rarity's sheet, its team bones in the cloth of the
 * wearer's side (linen with none), its glow bones lit full bright; each held weapon draws its mesh on its parent part (a hand
 * weapon on its arm, the quiver on the body), a banner in its side's cloth. The vanilla armour layer and held item draw nothing
 * for a stack this feature draws (ArmorFeatureMixin, HeldItemFeatureMixin). The gear screen's frame draws the player's worn
 * loadout through preview() outside battle, where nothing is equipped. First person keeps the stand-in item.
 */
public final class CommanderGearFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    static final int FULL_BRIGHT = 0xF00000;
    static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    /** The loadout the gear screen frame shows on the local player while it draws (null otherwise). */
    private static Map<EquipmentSlot, ItemStack> preview;
    /** Harness only: the armour pieces and weapons drawn by the last frame, by item id, and the frames drawn. */
    static final java.util.Set<String> drawn = java.util.concurrent.ConcurrentHashMap.newKeySet();
    static volatile long frames;

    public CommanderGearFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) { super(ctx); }

    /** Whether this feature draws a stack's own model, so the vanilla layer must not (the mixins ask). */
    public static boolean drawsOwn(ItemStack s) {
        var a = GearLooks.art(s);
        return a != null && (GearModels.armour(a.item()) != null || GearModels.weapon(a.item()) != null);
    }

    /** Runs draw with the local player shown in a loadout (equipped: gear slot -> piece uid, as GearWire.View holds it). */
    static void preview(Map<String, Integer> equipped, java.util.List<Gear.Piece> pieces, Runnable draw) {
        Map<EquipmentSlot, ItemStack> m = new EnumMap<>(EquipmentSlot.class);
        for (var e : equipped.entrySet()) {
            Gear.Piece p = null; for (var q : pieces) if (q.uid == e.getValue()) p = q;
            EquipmentSlot slot = switch (e.getKey()) { case "helm" -> EquipmentSlot.HEAD; case "chest" -> EquipmentSlot.CHEST; case "legs" -> EquipmentSlot.LEGS;
                case "boots" -> EquipmentSlot.FEET; case "main" -> EquipmentSlot.MAINHAND; case "off" -> EquipmentSlot.OFFHAND; default -> null; };
            if (slot != null && p != null) m.put(slot, GearLooks.stack(p));
        }
        preview = m;
        try { draw.run(); } finally { preview = null; }
    }

    static ItemStack stack(Entity e, EquipmentSlot slot) {
        var mc = MinecraftClient.getInstance();
        if (preview != null && mc != null && e == mc.player) return preview.getOrDefault(slot, ItemStack.EMPTY);
        return e instanceof AbstractClientPlayerEntity p ? p.getEquippedStack(slot) : ItemStack.EMPTY;
    }

    /** Hides the skin's outer layer under each worn piece this feature draws, so it cannot show through or flicker against the
     *  armour (the hat layer sits where a helm does). Runs after setAngles, past setModelPose; 0 age is the first-person arm. */
    public static void cover(PlayerEntityModel<?> m, net.minecraft.entity.LivingEntity e, float age) {
        if (age == 0f) return;
        if (drawsOwn(stack(e, EquipmentSlot.HEAD))) m.hat.visible = false;
        if (drawsOwn(stack(e, EquipmentSlot.CHEST))) { m.jacket.visible = false; m.leftSleeve.visible = false; m.rightSleeve.visible = false; }
        if (drawsOwn(stack(e, EquipmentSlot.LEGS))) { m.leftPants.visible = false; m.rightPants.visible = false; }
    }

    ModelPart part(String name) {
        var m = getContextModel();
        return switch (name) { case "head" -> m.head; case "body" -> m.body; case "right_arm" -> m.rightArm; case "left_arm" -> m.leftArm;
            case "right_leg" -> m.rightLeg; case "left_leg" -> m.leftLeg; default -> null; };
    }

    @Override public void render(MatrixStack m, VertexConsumerProvider v, int light, AbstractClientPlayerEntity e, float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch) {
        if (e.isInvisible()) return;
        frames++; drawn.clear();
        String side = TroopModels.side(e);
        int overlay = LivingEntityRenderer.getOverlay(e, 0f);
        for (EquipmentSlot slot : SLOTS) {
            ItemStack s = stack(e, slot); var a = GearLooks.art(s); if (a == null) continue;
            var armour = GearModels.armour(a.item());
            if (armour != null) { armour(m, v, light, overlay, armour, a.rarity(), side); drawn.add(a.item()); continue; }
            var w = GearModels.weapon(a.item());
            if (w != null && weapon(m, v, light, overlay, e, w, a.rarity(), side)) drawn.add(a.item());
        }
    }

    void armour(MatrixStack m, VertexConsumerProvider v, int light, int overlay, GearModels.Armour a, int rarity, String side) {
        Identifier skin = a.skins().getOrDefault(rarity, a.skins().get(1)), glow = a.glow().get(rarity);
        // three passes over the same tree, each showing only its own bones' boxes: skin, cloth, glow
        pass(m, v.getBuffer(RenderLayer.getEntityCutoutNoCull(skin)), light, overlay, a, rarity, false);
        if (a.team()) pass(m, v.getBuffer(RenderLayer.getEntityCutoutNoCull(TroopModels.cloth(a.cloth(), side))), light, overlay, a, rarity, true);
        if (glow != null) pass(m, v.getBuffer(RenderLayer.getEyes(glow)), FULL_BRIGHT, OverlayTexture.DEFAULT_UV, a, rarity, false);
    }

    void pass(MatrixStack m, net.minecraft.client.render.VertexConsumer vc, int light, int overlay, GearModels.Armour a, int rarity, boolean team) {
        for (var b : a.bones()) { b.part().visible = b.stages().isEmpty() || b.stages().contains(rarity); b.part().hidden = b.team() != team; }
        for (var r : a.roots().entrySet()) {
            ModelPart own = part(r.getKey()); if (own == null || !own.visible) continue;
            m.push(); own.rotate(m); r.getValue().render(m, vc, light, overlay); m.pop();
        }
    }

    boolean weapon(MatrixStack m, VertexConsumerProvider v, int light, int overlay, AbstractClientPlayerEntity e, GearModels.Weapon w, int rarity, String side) {
        var r = w.rarities().getOrDefault(rarity, w.rarities().get(1)); ModelPart own = part(w.parent());
        if (r == null || own == null || !own.visible) return false;
        m.push(); own.rotate(m);
        float[] wr = CommanderClips.wrist(e);
        if (wr != null && (w.parent().equals("right_arm") || w.parent().equals("left_arm"))) { // the clip's wrist turns the weapon about the fist
            float x = (w.parent().equals("right_arm") ? -1 : 1) / 16f, y = 8 / 16f;
            m.translate(x, y, 0); m.multiply(new org.joml.Quaternionf().rotationZYX(wr[2], wr[1], wr[0])); m.translate(-x, -y, 0);
        }
        r.mesh().render(m.peek(), v.getBuffer(RenderLayer.getEntityCutoutNoCull(w.texture(r, side))), light, overlay, 1f, 1f, 1f, 1f, Integer.MAX_VALUE);
        if (r.glow() != null) r.mesh().render(m.peek(), v.getBuffer(RenderLayer.getEyes(r.glow())), FULL_BRIGHT, OverlayTexture.DEFAULT_UV, 1f, 1f, 1f, 1f, Integer.MAX_VALUE);
        m.pop();
        return true;
    }
}
