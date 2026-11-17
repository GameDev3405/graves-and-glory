package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.model.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import java.io.Reader;
import java.util.*;

/**
 * The commander gear models (art/hand/gear/build_gear.py export and export-weapons write them), loaded on every client resource
 * reload: models/gear/<item>.json for each armour piece and models/gear/weapon/<item>.json for each weapon. An armour piece is a
 * tree of ModelParts under one empty root per biped part it rides on (head, body, the arms and legs), each bone posed in that
 * part's frame; CommanderGearFeature draws a root after the wearer's part. A weapon is one TroopMesh a rarity, in its parent
 * part's frame. A file that fails to load logs "[gear-models] <file>: <reason>" and is left out, so its piece keeps the vanilla
 * stand-in look (GearLooks).
 */
public final class GearModels implements SimpleSynchronousResourceReloadListener {
    static final String FOLDER = "models/gear";
    static final int FORMAT = 1;
    static final List<String> PARTS = List.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");
    /** The sides a gear cloth sheet is painted for (art/hand/common/handlib.SIDES). */
    static final List<String> SIDES = List.of("gold", "violet", "crimson", "teal", "company", "linen");
    @Override public Identifier getFabricId() { return Identifier.of(TroopModels.NAMESPACE, "gear_models"); }

    /** A gear bone: its part, whether it draws in the side's cloth, and the rarities it shows at (empty: every one). */
    record Bone(ModelPart part, boolean team, Set<Integer> stages) {}
    /** An armour piece: the roots by biped part, its bones, and its sheets by rarity (skins, glow) and side (cloth prefix). */
    record Armour(String item, Map<String, ModelPart> roots, List<Bone> bones, Map<Integer, Identifier> skins, Map<Integer, Identifier> glow, String cloth) {
        boolean team() { for (Bone b : bones) if (b.team) return true; return false; }
    }
    /** One rarity of a weapon: the mesh in the parent part's frame, its sheet (a banner's per side: texture is then the prefix) and glow. */
    record Rarity(TroopMesh mesh, String texture, Identifier glow) {}
    /** A weapon: the biped part it rides on, the hands it takes, whether its sheet is per side, its rarities, and a two-hander's arm poses (clip -> part -> pitch, yaw, roll in degrees). */
    record Weapon(String item, String parent, int hands, boolean side, Map<Integer, Rarity> rarities, Map<String, Map<String, float[]>> pose) {
        Identifier texture(Rarity r, String team) { return side ? TroopModels.cloth(r.texture, team) : Identifier.of(r.texture); }
    }

    private static volatile Map<String, Armour> armour = Map.of();
    private static volatile Map<String, Weapon> weapons = Map.of();
    static Armour armour(String item) { return item == null ? null : armour.get(item); }
    static Weapon weapon(String item) { return item == null ? null : weapons.get(item); }
    static Set<String> ids() { var s = new TreeSet<>(armour.keySet()); s.addAll(weapons.keySet()); return s; }

    @Override public void reload(ResourceManager manager) {
        Map<String, Armour> a = new LinkedHashMap<>(); Map<String, Weapon> w = new LinkedHashMap<>();
        var found = new TreeMap<>(manager.findResources(FOLDER, id -> id.getNamespace().equals(TroopModels.NAMESPACE) && id.getPath().endsWith(".json")));
        for (var e : found.entrySet()) {
            String file = e.getKey().getPath().substring(FOLDER.length() + 1);
            try (Reader r = e.getValue().getReader()) {
                JsonObject j = JsonParser.parseReader(r).getAsJsonObject();
                TroopModelLoader.need(j.has("format") && j.get("format").getAsInt() == FORMAT, "format is not " + FORMAT);
                String item = TroopModelLoader.str(j, "item");
                TroopModelLoader.need(file.equals(item + ".json") || file.equals("weapon/" + item + ".json"), "its item is " + item);
                if (file.startsWith("weapon/")) w.put(item, parseWeapon(manager, j));
                else a.put(item, parseArmour(manager, j));
            } catch (Exception ex) { TroopModelLoader.LOG.warn("[gear-models] {}: {}", file, ex.getMessage()); }
        }
        armour = Collections.unmodifiableMap(a); weapons = Collections.unmodifiableMap(w);
        CommanderClips.reload(manager);
        CosmeticExtras.reload(); // the banner flags it read from these files
        TroopModelLoader.LOG.info("[gear-models] {} armour pieces, {} weapons", a.size(), w.size());
    }

    static Armour parseArmour(ResourceManager manager, JsonObject j) {
        int[] size = TroopModelLoader.ints(j, "size", 2);
        ModelData data = new ModelData(); Map<String, ModelPartData> made = new HashMap<>();
        for (String p : PARTS) made.put(p, data.getRoot().addChild(p, ModelPartBuilder.create(), ModelTransform.NONE));
        List<String> names = new ArrayList<>(); List<String> parents = new ArrayList<>(); List<Boolean> team = new ArrayList<>(); List<Set<Integer>> stages = new ArrayList<>();
        for (JsonElement el : j.getAsJsonArray("bones")) {
            JsonObject b = el.getAsJsonObject(); String name = TroopModelLoader.str(b, "name"), parent = TroopModelLoader.str(b, "parent");
            TroopModelLoader.need(!name.isEmpty() && !made.containsKey(name), "a bone named " + name + " twice or not at all");
            TroopModelLoader.need(made.containsKey(parent), name + " has parent " + parent + ", neither a biped part nor a bone before it");
            int[] sheet = b.has("sheet") ? TroopModelLoader.ints(b, "sheet", 2) : size;
            ModelPartBuilder builder = ModelPartBuilder.create();
            for (JsonElement be : b.getAsJsonArray("boxes")) {
                JsonObject x = be.getAsJsonObject();
                float[] o = TroopModelLoader.floats(x, "origin", 3), d = x.has("dilation") ? TroopModelLoader.floats(x, "dilation", 3) : new float[3];
                int[] s = TroopModelLoader.ints(x, "size", 3), uv = TroopModelLoader.ints(x, "uv", 2);
                builder.uv(uv[0], uv[1]).mirrored(x.has("mirror") && x.get("mirror").getAsBoolean())
                    .cuboid(o[0], o[1], o[2], s[0], s[1], s[2], new Dilation(d[0], d[1], d[2]), sheet[0] / (float) size[0], sheet[1] / (float) size[1]);
            }
            float[] p = TroopModelLoader.floats(b, "pivot", 3), rot = b.has("rotation") ? TroopModelLoader.floats(b, "rotation", 3) : new float[3];
            made.put(name, made.get(parent).addChild(name, builder, ModelTransform.of(p[0], p[1], p[2], rot[0] * MathHelper.RADIANS_PER_DEGREE, rot[1] * MathHelper.RADIANS_PER_DEGREE, rot[2] * MathHelper.RADIANS_PER_DEGREE)));
            Set<Integer> st = new HashSet<>(); if (b.has("stages")) for (JsonElement s : b.getAsJsonArray("stages")) st.add(s.getAsInt());
            names.add(name); parents.add(parent); team.add(b.has("team") && b.get("team").getAsBoolean()); stages.add(st);
        }
        ModelPart root = TexturedModelData.of(data, size[0], size[1]).createModel();
        Map<String, ModelPart> parts = new HashMap<>(), roots = new LinkedHashMap<>();
        for (String p : PARTS) parts.put(p, root.getChild(p));
        List<Bone> bones = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            ModelPart part = parts.get(parents.get(i)).getChild(names.get(i)); parts.put(names.get(i), part);
            bones.add(new Bone(part, team.get(i), Set.copyOf(stages.get(i))));
            if (PARTS.contains(parents.get(i))) roots.put(parents.get(i), parts.get(parents.get(i)));
        }
        Map<Integer, Identifier> skins = sheets(manager, j, "skins"), glow = sheets(manager, j, "glow");
        TroopModelLoader.need(!skins.isEmpty(), "no skins");
        String cloth = TroopModelLoader.str(j, "cloth");
        Armour out = new Armour(TroopModelLoader.str(j, "item"), roots, List.copyOf(bones), skins, glow, cloth);
        if (out.team()) for (String side : SIDES) TroopModelLoader.texture(manager, TroopModels.cloth(cloth, side));
        return out;
    }

    static Weapon parseWeapon(ResourceManager manager, JsonObject j) {
        String parent = TroopModelLoader.str(j, "parent");
        TroopModelLoader.need(PARTS.contains(parent), "parent " + parent + " is not a biped part");
        boolean side = j.has("side") && j.get("side").getAsBoolean();
        Map<Integer, Rarity> rar = new HashMap<>();
        for (var e : j.getAsJsonObject("rarities").entrySet()) {
            JsonObject r = e.getValue().getAsJsonObject(); int[] sheet = TroopModelLoader.ints(r, "sheet", 2);
            TroopMesh mesh = TroopMesh.parse(r.getAsJsonArray("quads"), sheet[0], sheet[1]);
            String tex = TroopModelLoader.str(r, "texture");
            if (side) for (String s : SIDES) TroopModelLoader.texture(manager, TroopModels.cloth(tex, s));
            else TroopModelLoader.texture(manager, Identifier.of(tex));
            Identifier glow = TroopModelLoader.str(r, "glow").isEmpty() ? null : Identifier.of(TroopModelLoader.str(r, "glow"));
            if (glow != null) TroopModelLoader.texture(manager, glow);
            rar.put(Integer.parseInt(e.getKey()), new Rarity(mesh, tex, glow));
        }
        Map<String, Map<String, float[]>> pose = new HashMap<>();
        if (j.has("pose")) for (var c : j.getAsJsonObject("pose").entrySet()) {
            Map<String, float[]> parts = new HashMap<>();
            for (var p : c.getValue().getAsJsonObject().entrySet()) {
                JsonArray a = p.getValue().getAsJsonArray(); parts.put(p.getKey(), new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()});
            }
            pose.put(c.getKey(), Map.copyOf(parts));
        }
        return new Weapon(TroopModelLoader.str(j, "item"), parent, j.get("hands").getAsInt(), side, Map.copyOf(rar), Map.copyOf(pose));
    }

    /** A rarity -> texture object of the file (skins or glow), every texture checked present; empty when the key is absent. */
    static Map<Integer, Identifier> sheets(ResourceManager manager, JsonObject j, String key) {
        Map<Integer, Identifier> out = new HashMap<>();
        if (j.has(key)) for (var e : j.getAsJsonObject(key).entrySet()) {
            Identifier id = Identifier.of(e.getValue().getAsString()); TroopModelLoader.texture(manager, id); out.put(Integer.parseInt(e.getKey()), id);
        }
        return Map.copyOf(out);
    }
}
