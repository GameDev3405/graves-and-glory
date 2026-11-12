package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.io.Reader;
import java.util.*;

/**
 * The commander weapon clips (art/hand/gear/clips.py export writes gear_clips.json), played on the player model after vanilla
 * setAngles (PlayerModelMixin): a weapon's strike clip over the vanilla swing, its use clip in the first moments of the item
 * cooldown its use set (a staff's heal; the cooldown reaches only the local player's client), its hold clip while sneaking, held
 * on its last key; otherwise a two-hander takes its idle pose from the weapon model. A clip sets the right arm, a two-hander's left
 * arm, and the wrist, which CommanderGearFeature turns the weapon by about the fist. A missing or broken file logs
 * "[gear-clips] <reason>" and leaves every arm to vanilla.
 */
public final class CommanderClips {
    static final Identifier FILE = new Identifier(TroopModels.NAMESPACE, "gear_clips.json");
    static final int FORMAT = 1;

    record Key(float t, float[] rightArm, float[] rightHand, float[] leftArm) {}
    record Clip(String name, boolean hold, float length, List<Key> keys) {}

    private static volatile Map<String, Map<String, Clip>> items = Map.of();
    private static final Map<Integer, Float> sneakFrom = new HashMap<>();
    /** The wrist (pitch, yaw, roll in radians) the last posed entity holds its weapon at; CommanderGearFeature reads it in the same render. */
    private static LivingEntity wristOf;
    private static final float[] wrist = new float[3];
    /** Harness only: the clip and share the last posed entity played ("" for none), and every clip played since cleared. */
    static volatile String playing = "";
    static volatile float playingAt;
    static final Set<String> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();

    static int count() { int n = 0; for (var m : items.values()) n += m.size(); return n; }

    static void reload(ResourceManager manager) {
        Map<String, Map<String, Clip>> out = new HashMap<>();
        var res = manager.getResource(FILE);
        if (res.isEmpty()) { TroopModelLoader.LOG.warn("[gear-clips] {} is missing", FILE); items = Map.of(); return; }
        try (Reader r = res.get().getReader()) {
            JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
            TroopModelLoader.need(j.has("format") && j.get("format").getAsInt() == FORMAT, "format is not " + FORMAT);
            for (var it : j.getAsJsonObject("items").entrySet()) {
                Map<String, Clip> trig = new HashMap<>();
                for (var t : it.getValue().getAsJsonObject().entrySet()) {
                    JsonObject c = t.getValue().getAsJsonObject(); List<Key> keys = new ArrayList<>();
                    for (var k : c.getAsJsonArray("keys")) {
                        JsonObject o = k.getAsJsonObject();
                        keys.add(new Key(o.get("t").getAsFloat(), rad(o.getAsJsonArray("right_arm")), rad(o.getAsJsonArray("right_hand")), o.has("left_arm") ? rad(o.getAsJsonArray("left_arm")) : null));
                    }
                    TroopModelLoader.need(keys.size() >= 2, it.getKey() + " " + t.getKey() + " has fewer than two keys");
                    trig.put(t.getKey(), new Clip(TroopModelLoader.str(c, "clip"), TroopModelLoader.str(c, "kind").equals("hold"), c.get("length").getAsFloat(), List.copyOf(keys)));
                }
                out.put(it.getKey(), Map.copyOf(trig));
            }
        } catch (Exception ex) { TroopModelLoader.LOG.warn("[gear-clips] {}", ex.getMessage()); out.clear(); }
        items = Map.copyOf(out);
        TroopModelLoader.LOG.info("[gear-clips] {} clips on {} weapons", count(), out.size());
    }

    static float[] rad(JsonArray a) { return new float[]{a.get(0).getAsFloat() * MathHelper.RADIANS_PER_DEGREE, a.get(1).getAsFloat() * MathHelper.RADIANS_PER_DEGREE, a.get(2).getAsFloat() * MathHelper.RADIANS_PER_DEGREE}; }

    /** The wrist an entity's weapon turns by, or null for none. */
    static float[] wrist(LivingEntity e) { return e == wristOf ? wrist : null; }

    /** Poses the arms after vanilla setAngles. age is the model's animation progress in ticks; 0 is the first-person arm, left alone. */
    public static void pose(PlayerEntityModel<?> m, LivingEntity e, float age) {
        wristOf = null; playing = "";
        if (!e.isInSneakingPose()) sneakFrom.remove(e.getId()); // C17-F2: before every return, so a swap or unload mid-sneak cannot leave it stale
        if (age == 0f) return;
        var a = GearLooks.art(CommanderGearFeature.stack(e, EquipmentSlot.MAINHAND)); if (a == null) return;
        var clips = items.get(a.item()); var w = GearModels.weapon(a.item());
        Clip clip = null; float t = 0;
        if (clips != null) {
            Clip strike = clips.get("strike"), use = clips.get("use"), sneak = clips.get("sneak");
            float used = used(e, a.item(), use);
            if (strike != null && m.handSwingProgress > 0 && e.preferredHand == Hand.MAIN_HAND) { clip = strike; t = m.handSwingProgress; }
            else if (use != null && used >= 0) { clip = use; t = used; }
            else if (sneak != null && e.isInSneakingPose()) {
                float from = sneakFrom.computeIfAbsent(e.getId(), id -> age);
                clip = sneak; t = Math.min(1, (age - from) / 20f / sneak.length);
            }
        }
        if (clip != null) {
            Key at = sample(clip, t);
            set(m.rightArm, at.rightArm); if (at.leftArm != null) set(m.leftArm, at.leftArm);
            wristOf = e; System.arraycopy(at.rightHand, 0, wrist, 0, 3);
            playing = clip.name; playingAt = t; seen.add(clip.name);
        } else if (w != null && w.hands() == 2 && w.pose().containsKey("idle")) {
            var idle = w.pose().get("idle");
            if (idle.containsKey("right_arm")) set(m.rightArm, deg(idle.get("right_arm")));
            if (idle.containsKey("left_arm")) set(m.leftArm, deg(idle.get("left_arm")));
        } else return;
        m.rightSleeve.copyTransform(m.rightArm); m.leftSleeve.copyTransform(m.leftArm);
    }

    /** The share of a use clip played since the item's cooldown began, or -1 outside it. */
    static float used(LivingEntity e, String item, Clip use) {
        if (use == null || !(e instanceof PlayerEntity p)) return -1;
        var stack = CommanderGearFeature.stack(e, EquipmentSlot.MAINHAND);
        float left = p.getItemCooldownManager().getCooldownProgress(stack.getItem(), 0f);
        if (left <= 0) return -1;
        int total = 0;
        for (var row : Spec.GEARITEMS) if (row.id().equals(item)) { var f = GearStats.family(row.family()); if (f != null) total = Math.max(1, f.cooldownTicks()); }
        float s = (1 - left) * total / 20f / use.length;
        return total > 0 && s < 1 ? s : -1;
    }

    /** The pose at share t of a clip: the keys either side eased with smoothstep. */
    static Key sample(Clip c, float t) {
        var ks = c.keys; if (t <= ks.get(0).t) return ks.get(0);
        for (int i = 1; i < ks.size(); i++) {
            Key a = ks.get(i - 1), b = ks.get(i); if (t > b.t) continue;
            float u = b.t > a.t ? (t - a.t) / (b.t - a.t) : 1; u = u * u * (3 - 2 * u);
            return new Key(t, lerp(a.rightArm, b.rightArm, u), lerp(a.rightHand, b.rightHand, u), a.leftArm != null && b.leftArm != null ? lerp(a.leftArm, b.leftArm, u) : null);
        }
        return ks.get(ks.size() - 1);
    }

    static float[] lerp(float[] a, float[] b, float u) { return new float[]{a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u, a[2] + (b[2] - a[2]) * u}; }
    static float[] deg(float[] d) { return new float[]{d[0] * MathHelper.RADIANS_PER_DEGREE, d[1] * MathHelper.RADIANS_PER_DEGREE, d[2] * MathHelper.RADIANS_PER_DEGREE}; }
    static void set(net.minecraft.client.model.ModelPart p, float[] r) { p.pitch = r[0]; p.yaw = r[1]; p.roll = r[2]; }
}
