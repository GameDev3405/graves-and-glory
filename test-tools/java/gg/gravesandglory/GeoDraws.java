package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Harness only: the draw record of the troops GeoTroopRenderer draws (the GeckoLib renderer, which every approved troop takes), the
 * Geo counterpart of TroopRenderer.drawn. TroopRenderer hands an approved troop to GeoTroopRenderer and returns before its own draw
 * records (TroopRenderer.Drawn, its pose snapshot, BodyCache), so a check that reads those sees nothing for such a troop; this reads what
 * the Geo draw really did, from GeoTroopRenderer.PROBE (called after each Geo draw with the baked model, whose bones still hold that draw):
 * how often each entity drew, the face bones it showed, its bones' pose, and the face, armour tier and texture GeoTroopRenderer chose
 * (its private fields, read by reflection right after the draw). Install before reading; the probe chains whatever probe was set first.
 */
final class GeoDraws {
    private GeoDraws() {}

    /** One entity's Geo draws: how many, and what the last one showed. Written on the render thread, read after a frame (the client thread). */
    static final class Drawn {
        volatile int count;
        /** The face GeoTroopRenderer chose (TroopFaces.pick), the armour tier it drew (1 to 3); -1 when unread. */
        volatile int variant = -1, armour = -1;
        /** The texture file it bound (&lt;troop&gt;_geo/t&lt;tier&gt;_&lt;side&gt;.png); "-" when unread. */
        volatile String texture = "-";
        /** The face bones (face_v&lt;n&gt;) shown in the last draw. */
        volatile List<String> faces = List.of();
        /** Every bone of the last draw, in the baked model's order: position x, y, z then rotation x, y, z (radians), as TroopModel.snapshot lays a bone out. */
        float[] pose;
    }

    private static final Map<Entity, Drawn> DRAWN = new MapMaker().weakKeys().makeMap();

    /** The probe: records the draw, then calls the probe it replaced. */
    private record Chain(GeoTroopRenderer.Probe next) implements GeoTroopRenderer.Probe {
        @Override public void seen(Entity e, BakedGeoModel model, float tickDelta) {
            try { record(e, model); } catch (RuntimeException ex) { ex.printStackTrace(); }
            if (next != null) next.seen(e, model, tickDelta);
        }
    }

    /** Installs the probe (once; again after another check cleared or replaced it). */
    static void install() {
        var p = GeoTroopRenderer.PROBE;
        if (!(p instanceof Chain)) GeoTroopRenderer.PROBE = new Chain(p);
    }

    /** The Geo draws of an entity, or null for one never drawn so; a framed portrait mob reads its frame's copy (Portrait.twin), as TroopRenderer.drawn does. */
    static Drawn drawn(Entity e) {
        if (e == null) return null;
        Drawn d = DRAWN.get(e);
        if (d != null) return d;
        Entity t = Portrait.latest(e);
        return t == null ? null : DRAWN.get(t);
    }

    /** How many Geo draws the entity has had (0 for none). */
    static int count(Entity e) { Drawn d = drawn(e); return d == null ? 0 : d.count; }

    /** Whether this client draws the entity through GeckoLib (GeoTroopRenderer.handles of its Look). */
    static boolean geo(Entity e) { return e != null && GeoTroopRenderer.handles(TroopModels.resolve(e)); }

    /** Whether this client draws the troop through GeckoLib (GeoTroopRenderer.handles of a stage 1 Look of it). */
    static boolean geo(String troop) {
        var row = ArmyCards.spec(troop);
        return row != null && GeoTroopRenderer.handles(new TroopModels.Look(row, 1, false, null));
    }

    /**
     * The first of these troops still drawn by the legacy renderer (its own TroopModel, not GeoTroopRenderer), for a check that measures a
     * legacy-only mechanism (BodyCache's recorded buffers, its per-part fade switches, TroopMotion's death pick); null when every one is Geo.
     */
    static String legacy(String... troops) {
        for (String t : troops) if (TroopModels.model(t) != null && !geo(t)) return t;
        return null;
    }

    // ---- the record ----
    private static Field rendererGeo, face, armour, model, texture;
    private static boolean looked;

    private static void look() {
        if (looked) return;
        looked = true;
        try {
            rendererGeo = TroopRenderer.class.getDeclaredField("geo"); rendererGeo.setAccessible(true);
            face = GeoTroopRenderer.class.getDeclaredField("face"); face.setAccessible(true);
            armour = GeoTroopRenderer.class.getDeclaredField("armour"); armour.setAccessible(true);
            model = GeoTroopRenderer.class.getDeclaredField("geo"); model.setAccessible(true);
            texture = GeoTroopRenderer.Model.class.getDeclaredField("texture"); texture.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            System.out.println("[geo-draws] could not read GeoTroopRenderer's draw state: " + ex);
            rendererGeo = null;
        }
    }

    private static void record(Entity e, BakedGeoModel baked) {
        Drawn d = DRAWN.computeIfAbsent(e, k -> new Drawn());
        List<String> faces = new ArrayList<>();
        List<GeoBone> bones = new ArrayList<>();
        for (GeoBone b : baked.topLevelBones()) walk(b, bones);
        float[] pose = d.pose != null && d.pose.length == bones.size() * 6 ? d.pose : new float[bones.size() * 6];
        for (int i = 0; i < bones.size(); i++) {
            GeoBone b = bones.get(i);
            pose[6 * i] = b.getPosX(); pose[6 * i + 1] = b.getPosY(); pose[6 * i + 2] = b.getPosZ();
            pose[6 * i + 3] = b.getRotX(); pose[6 * i + 4] = b.getRotY(); pose[6 * i + 5] = b.getRotZ();
            if (b.getName().startsWith("face_v") && !b.isHidden()) faces.add(b.getName());
        }
        d.pose = pose;
        d.faces = List.copyOf(faces);
        look();
        if (rendererGeo != null) {
            try {
                var look = TroopModels.resolve(e);
                var r = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e);
                Object g = look == null || !(r instanceof TroopRenderer<?>) ? null : ((Map<?, ?>) rendererGeo.get(r)).get(look.troop().id());
                if (g != null) {
                    d.variant = face.getInt(g);
                    d.armour = armour.getInt(g);
                    Object m = model.get(g);
                    Object tex = m == null ? null : texture.get(m);
                    String path = tex == null ? "-" : tex.toString();
                    d.texture = path.substring(path.lastIndexOf('/', path.lastIndexOf('/') - 1) + 1);
                }
            } catch (ReflectiveOperationException | ClassCastException ex) {
                System.out.println("[geo-draws] could not read a draw's state: " + ex);
            }
        }
        d.count++;
    }

    private static void walk(GeoBone b, List<GeoBone> out) {
        out.add(b);
        for (GeoBone c : b.getChildBones()) walk(c, out);
    }
}
