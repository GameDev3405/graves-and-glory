package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Supplier;

import static gg.gravesandglory.Phase2Checks.report;
import static gg.gravesandglory.Phase2Checks.server;

/**
 * The troop faces suite (texel-3 faces design F2, task-5d-faces-design.md; GRAVES_PHASE2_CHECKS=faces). Never included in releases.
 * <p>Data checks first, on the loaded models: p2-faces-model (every troop with faces has 4 face bones carrying one variant each; a draw wearing any
 * number, however far outside 0 to 3, shows exactly the one face bone of that variant mod 4, at stages 1 to 3, and the rest of the model shown is
 * the same for every variant; the four face bones draw the same box from the four slots of the sheet, read off the loaded ModelPart cuboids' UVs on the face bones' own sheet: faces are
 * painted at 2 texels a px (F4), so a density 3 troop's face bones have a sheet of 2 texels a px, a density 1 troop's reduced faces 1, and every UV corner lands on a whole texel) and
 * p2-faces-pick (what TroopModels.face gives: a card portrait the model's card face whatever its UUID, a pinned troop its pinned face, any other the face
 * its UUID hashes to, every face reached over 400 UUIDs).
 * <p>Then the shots, with the harness-only forced variant (TroopModels.forceFace) on a row of four gold troops of one kind (variants 0 to 3, left to
 * right as the camera sees them): faces-TROOP-D for D = 3, 6, 24 and 36 blocks at stage 1, faces-TROOP-s3-D for D = 3 and 6 at stage 3 (full gear over the
 * face), faces-TROOP-card-160 and faces-TROOP-card-36 (the cards' portrait at the card face), and faces-mixed (eight troops of one kind, nothing forced,
 * wearing the faces their UUIDs hash to). Checks: p2-faces-shots (every shot taken lit, the camera where it was set, each troop of the row drawn
 * wearing the variant asked and showing exactly its face bone), p2-faces-skin (the density 3 troop binds its _far twin from 24 blocks out, so the twin
 * carries the faces; a density 1 troop never does), p2-faces-cards (a card draws the card face, one face bone) and p2-faces-mixed (natural faces equal
 * TroopFaces.variant of the UUID, at least two different ones in a row of eight). "[p2-faces-fact]" lines give each shot's drawn variants, face bones and skin.
 * GRAVES_PHASE2_FACES=knight,mage limits the troops (default: every troop with faces).
 */
final class FacesChecks {
    private FacesChecks() {}

    /** Adds this file's suite to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("faces", Faces::new);
    }

    static final class Faces extends Phase2Checks.Stage {
        static final float YAW = 180;
        /** The camera's eye over the standing layer; how high the troops not in view are parked; the least and the most gap of a row (blocks). */
        static final double EYE = 1.2, PARK = 60, GAP = 1.7, GAP_MAX = 2.0;
        /** The floor's half width: a camera 36 blocks in front of the row stands on it. */
        static final int HALF = 38, MIXED_N = 8, FACES = 4;
        static final String MIXED = "(mixed)";
        /** A shot: its name, the group of troops it shows (a troop id or MIXED), their stage, the camera's distance in blocks (0 for a card) and the card's px a block (0 for a world shot). */
        record Shot(String name, String group, int stage, int dist, int card) {}
        /** What one world shot drew: the variants each troop of the row wore, the face bones each showed, the skin the first bound and whether every troop bound the same. */
        record Fact(String shot, String troop, int stage, int dist, List<Integer> variants, List<String> bones, String skin, boolean far) {}

        final List<String> troops = new ArrayList<>();
        final String mixedTroop;
        final Map<String, List<GravesAndGlory.Unit>> rows = new LinkedHashMap<>();
        final Map<String, Integer> staged = new HashMap<>();
        final List<Shot> shots = new ArrayList<>();
        final List<Fact> facts = new ArrayList<>();
        final List<String> bad = new ArrayList<>(), cardBad = new ArrayList<>(), mixedFacts = new ArrayList<>();
        int si, held, steady, settle, cardChecks, mixedKinds;
        String on = "";
        int[] marks = new int[0];
        MobEntity cardMob;
        int cardMark;
        boolean mixedOk = true;

        String name() { return "faces"; }

        Faces() {
            Phase2Checks.Turn.hook();
            String only = System.getProperty("gravesandglory.faceTroops", "");
            for (String id : new TreeSet<>(TroopModels.ids())) {
                var m = TroopModels.model(id);
                if (m != null && m.faceCount > 1 && (only.isBlank() || List.of(only.split(",")).contains(id))) troops.add(id);
            }
            mixedTroop = troops.contains("knight") ? "knight" : troops.isEmpty() ? "" : troops.get(0);
            for (String t : troops) {
                for (int d : new int[]{3, 6, 24, 36}) shots.add(new Shot("faces-" + t + "-" + d, t, 1, d, 0));
                if (hasStage3(t)) for (int d : new int[]{3, 6}) shots.add(new Shot("faces-" + t + "-s3-" + d, t, 3, d, 0)); // a troop with no level 3 name (the mage, the Marshal) never leaves stage 1
                shots.add(new Shot("faces-" + t + "-card-160", t, 1, 0, Phase2Checks.Turn.T3_LARGE));
                shots.add(new Shot("faces-" + t + "-card-36", t, 1, 0, Phase2Checks.Turn.T3_SMALL));
            }
            if (!mixedTroop.isEmpty()) shots.add(new Shot("faces-mixed", MIXED, 1, 8, 0));
        }

        /** Whether the troop has a level 3 name (Spec.LEVELNAMES), the only way a harness troop reaches armour tier 3 (TroopModels.resolve). */
        static boolean hasStage3(String troop) {
            var row = ArmyCards.spec(troop);
            if (row == null) return false;
            for (var n : Spec.LEVELNAMES) if (n.troop().equals(row.evolveLine()) && n.level() == 3) return true;
            return false;
        }

        int expected() { return troops.size() * FACES + (mixedTroop.isEmpty() ? 0 : MIXED_N); }

        // ---- server: the fixture ----
        void cast(GravesAndGlory.Battle b) {
            force(b, site.add(-HALF - 2, 0, -HALF - 2), site.add(HALF + 2, 0, HALF + 2));
            floor(b, HALF);
            for (String t : troops) {
                List<GravesAndGlory.Unit> row = new ArrayList<>();
                for (int k = 0; k < FACES; k++) row.add(post(b, gold, t, 0, 0, YAW));
                rows.put(t, row);
            }
            if (!mixedTroop.isEmpty()) {
                List<GravesAndGlory.Unit> row = new ArrayList<>();
                for (int k = 0; k < MIXED_N; k++) row.add(post(b, gold, mixedTroop, 0, 0, YAW));
                rows.put(MIXED, row);
            }
            int gi = 0;
            for (String g : rows.keySet()) place(g, false, gi++);
            camera(b, 0, EYE, -6, 0, 0);
        }

        /** Server: a group in a row at the platform's centre facing north, variant 0 on the camera's left (it looks along +z, so its left is +x), or parked high over the floor out of every view. */
        void place(String g, boolean up, int gi) {
            var row = rows.get(g);
            double w = 0;
            for (var u : row) w = Math.max(w, u.mob.getWidth());
            double gap = Math.min(GAP_MAX, Math.max(GAP, w + .45)); // far enough apart that a shield or a swung arm does not hide the next face, near enough that the row of four fits the 3 block shot
            for (int k = 0; k < row.size(); k++) {
                Vec3d where = up ? at(((row.size() - 1) / 2.0 - k) * gap, 0) : at(3 * gi + .7 * k - 20, -4).add(0, PARK, 0);
                var mob = row.get(k).mob;
                pins.removeIf(p -> p.mob() == mob);
                pins.add(new Pin(mob, where, YAW));
                pin(mob, where, YAW);
            }
        }

        // ---- client: the steps ----
        boolean everyoneThere(MinecraftClient c) {
            List<GravesAndGlory.Unit> units;
            try { units = new ArrayList<>(posted); } catch (java.util.ConcurrentModificationException e) { return false; }
            if (units.size() != expected()) return false;
            for (var u : units) if (client(c, u.mob) == null) return false;
            return true;
        }

        public void step(MinecraftClient c, int tick) {
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); server(c, b -> { player = id; stage = 1; }); }
            if (finished) return;
            if (tick > 24000) { report("p2-faces-timeout", false, "stuck at shot " + si + " of " + shots.size() + ", client step " + cs + ", server stage " + stage); end(c); return; }
            switch (cs) {
                case 0 -> {
                    boolean lit = litStart(c);
                    if (troops.isEmpty()) { report("p2-faces-model", false, "no troop model has faces"); serverDone = true; end(c); return; }
                    if (lit && ready && everyoneThere(c)) { models(c); picks(c); c.options.hudHidden = true; wait = 0; cs = 1; }
                    else if (++wait % 600 == 0) System.out.println("[p2-wait] faces step 0 after " + wait + " ticks: ready=" + ready + ", posted " + posted.size() + " of " + expected() + "; " + litWhy(c));
                }
                case 1 -> shoot(c);
                case 2 -> { if (++wait == 5) { finals(c); serverDone = true; end(c); } }
                default -> { }
            }
        }

        public void cleanup(MinecraftClient c) {
            Phase2Checks.Turn.noTarget = false;
            for (var row : rows.values()) for (var u : row) { var e = client(c, u.mob); if (e != null) TroopModels.forceFace(e, -1); }
            super.cleanup(c);
        }

        void next() { si++; wait = 0; steady = 0; held = 0; }

        // ---- the data checks ----
        @SuppressWarnings("unchecked")
        static Map<String, ModelPart> partsOf(TroopModel<?> m) {
            try { Field f = TroopModel.class.getDeclaredField("parts"); f.setAccessible(true); return (Map<String, ModelPart>) f.get(m); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        }

        static Field field(Class<?> k, String name) throws NoSuchFieldException { Field f = k.getDeclaredField(name); f.setAccessible(true); return f; }

        /** A part's cuboids, in the order its bone lists its boxes, or null when the reflection fails. */
        static List<?> cuboidsOf(ModelPart part) {
            try { return (List<?>) field(ModelPart.class, "cuboids").get(part); }
            catch (ReflectiveOperationException e) { return null; }
        }

        /** The UVs one cuboid draws, as the bounding rectangle {u0, v0, u1, v1} in px of a w x h sheet, or null when the reflection fails. */
        static float[] uvBounds(Object cuboid, int w, int h) {
            try {
                float u0 = 1e9f, v0 = 1e9f, u1 = -1e9f, v1 = -1e9f;
                for (Object quad : (Object[]) field(cuboid.getClass(), "sides").get(cuboid)) {
                    for (Object vertex : (Object[]) field(quad.getClass(), "vertices").get(quad)) {
                        float u = field(vertex.getClass(), "u").getFloat(vertex), v = field(vertex.getClass(), "v").getFloat(vertex);
                        u0 = Math.min(u0, u); v0 = Math.min(v0, v); u1 = Math.max(u1, u); v1 = Math.max(v1, v);
                    }
                }
                return new float[]{u0 * w, v0 * h, u1 * w, v1 * h};
            } catch (ReflectiveOperationException e) { return null; }
        }

        /** One cuboid's box, {minX, minY, minZ, maxX, maxY, maxZ}, or null. */
        static float[] boxOf(Object cuboid) {
            try {
                String[] names = {"minX", "minY", "minZ", "maxX", "maxY", "maxZ"};
                float[] out = new float[6];
                for (int i = 0; i < 6; i++) out[i] = field(cuboid.getClass(), names[i]).getFloat(cuboid);
                return out;
            } catch (ReflectiveOperationException e) { return null; }
        }

        void models(MinecraftClient c) {
            List<String> wrong = new ArrayList<>();
            StringBuilder ok = new StringBuilder();
            int draws = 0, uvs = 0;
            for (String t : troops) {
                var m = TroopModels.model(t);
                var row = ArmyCards.spec(t);
                JsonObject j;
                try { j = Phase2Checks.Depth.modelJson(c, t); } catch (Exception x) { wrong.add(t + ": the model file: " + x); continue; }
                int count = j.getAsJsonObject("faces").get("count").getAsInt();
                if (count != FACES || m.faceCount != count) wrong.add(t + ": faces.count " + count + ", the loaded model " + m.faceCount + " (want " + FACES + ")");
                int w = j.has("size") ? j.getAsJsonArray("size").get(0).getAsInt() : 64, h = j.has("size") ? j.getAsJsonArray("size").get(1).getAsInt() : 64;
                int dens = j.has("density") ? j.get("density").getAsInt() : 1;   // texels a layout px; the texture is size x density texels
                int[][] sheet = new int[count][];                                // per variant: the face bone's own sheet, in its own px (a face px is the face density's texel)
                String[] names = new String[count];
                int[][][] uv = new int[count][][];   // per variant, per box of the face bone: the box's uv
                int[][] box = null;                  // per box: its size, the same in every face bone
                int boxes = 0;
                for (var el : j.getAsJsonArray("bones")) {
                    JsonObject b = el.getAsJsonObject();
                    if (!b.has("variants")) continue;
                    JsonArray vs = b.getAsJsonArray("variants");
                    if (vs.size() != 1 || vs.get(0).getAsInt() < 0 || vs.get(0).getAsInt() >= count) { wrong.add(t + ": bone " + b.get("name").getAsString() + " carries variants " + vs); continue; }
                    int v = vs.get(0).getAsInt();
                    if (names[v] != null) wrong.add(t + ": two bones carry variant " + v);
                    names[v] = b.get("name").getAsString();
                    var bxs = b.getAsJsonArray("boxes");
                    if (box == null) { boxes = bxs.size(); box = new int[boxes][]; }
                    else if (bxs.size() != boxes) { wrong.add(t + ": face bone of variant " + v + " has " + bxs.size() + " boxes, not " + boxes); continue; }
                    sheet[v] = b.has("sheet") ? new int[]{b.getAsJsonArray("sheet").get(0).getAsInt(), b.getAsJsonArray("sheet").get(1).getAsInt()} : new int[]{w, h};
                    uv[v] = new int[boxes][];
                    for (int k = 0; k < boxes; k++) {
                        JsonObject bx = bxs.get(k).getAsJsonObject();
                        uv[v][k] = new int[]{bx.getAsJsonArray("uv").get(0).getAsInt(), bx.getAsJsonArray("uv").get(1).getAsInt()};
                        int[] sz = {bx.getAsJsonArray("size").get(0).getAsInt(), bx.getAsJsonArray("size").get(1).getAsInt(), bx.getAsJsonArray("size").get(2).getAsInt()};
                        if (box[k] == null) box[k] = sz; else if (box[k][0] != sz[0] || box[k][1] != sz[1] || box[k][2] != sz[2]) wrong.add(t + ": face bone of variant " + v + " has another size for its box " + k);
                    }
                }
                boolean complete = true;
                for (int v = 0; v < count; v++) if (names[v] == null || uv[v] == null) { wrong.add(t + ": no bone carries variant " + v); complete = false; }
                if (!complete || box == null) continue;
                if (boxes < 1 || boxes > 2) wrong.add(t + ": the face bones hold " + boxes + " boxes (the head, and with a beard block the beard volume)");
                int fileFixed = j.getAsJsonObject("faces").has("fixed") ? j.getAsJsonObject("faces").get("fixed").getAsInt() : -1;
                if (m.faceFixed != fileFixed) wrong.add(t + ": the loaded model pins face " + m.faceFixed + ", the file says " + fileFixed);
                // a draw wearing any number shows exactly the one face bone of that number mod count, the same model otherwise
                for (int stage = 1; stage <= 3; stage++) {
                    var look = new TroopModels.Look(row, stage, false, null);
                    m.prepare(look, false, false, 0);
                    List<String> zero = new ArrayList<>(m.shownBones(m.shownMask()));
                    zero.removeIf(n -> n.startsWith("face_v"));
                    for (int v = -3; v <= count + 5; v++) {
                        m.prepare(look, false, false, v);
                        List<String> shown = new ArrayList<>(m.shownBones(m.shownMask()));
                        List<String> faces = new ArrayList<>(shown);
                        faces.removeIf(n -> !n.startsWith("face_v"));
                        shown.removeIf(n -> n.startsWith("face_v"));
                        String want = names[Math.floorMod(v, count)];
                        if (!faces.equals(List.of(want))) wrong.add(t + " stage " + stage + " wearing " + v + ": shows " + faces + ", want [" + want + "]");
                        if (!shown.equals(zero)) wrong.add(t + " stage " + stage + " wearing " + v + ": the rest of the model differs from variant 0's");
                        draws++;
                    }
                }
                // the four face bones draw one box from the four slots of the sheet, on one sheet of their own: a whole number of texels a face px
                // (2 texels on a 3-texel troop, 1 on a 1x troop), every uv corner of every face quad on a whole texel of the texture (nothing bleeds into a neighbour)
                int texW = w * dens, texH = h * dens, fdens = 0;
                for (int v = 0; v < count; v++) {
                    if (sheet[v][0] != sheet[0][0] || sheet[v][1] != sheet[0][1]) wrong.add(t + ": " + names[v] + " has the sheet " + java.util.Arrays.toString(sheet[v]) + ", " + names[0] + " has " + java.util.Arrays.toString(sheet[0]));
                }
                if (texW % sheet[0][0] != 0 || texH % sheet[0][1] != 0 || texW / sheet[0][0] != texH / sheet[0][1]) wrong.add(t + ": the face sheet " + java.util.Arrays.toString(sheet[0]) + " is not a whole number of texels a px of the " + texW + "x" + texH + " texture");
                else fdens = texW / sheet[0][0];
                int wantDens = dens >= 2 ? 2 : 1;
                if (fdens != wantDens) wrong.add(t + ": the face density is " + fdens + " texels a px, want " + wantDens + " (2 on a 3-texel troop, 1 on a 1x troop)");
                var parts = partsOf(m);
                var cuboids0 = cuboidsOf(parts.get(names[0]));
                for (int v = 0; v < count; v++) {
                    var cuboids = cuboidsOf(parts.get(names[v]));
                    if (cuboids == null || cuboids0 == null || cuboids.size() != boxes || cuboids0.size() != boxes) { wrong.add(t + ": " + names[v] + " does not read back " + boxes + " cuboids"); continue; }
                    for (int k = 0; k < boxes; k++) {
                        float[] bounds = uvBounds(cuboids.get(k), sheet[v][0], sheet[v][1]), b = boxOf(cuboids.get(k)), box0 = boxOf(cuboids0.get(k));
                        if (bounds == null || b == null || box0 == null) { wrong.add(t + ": cuboid " + k + " of " + names[v] + " could not be read"); continue; }
                        int fw = 2 * (box[k][0] + box[k][2]), fh = box[k][2] + box[k][1];
                        float[] want = {uv[v][k][0], uv[v][k][1], uv[v][k][0] + fw, uv[v][k][1] + fh};
                        for (int i = 0; i < 4; i++) if (Math.abs(bounds[i] - want[i]) > 0.01f) { wrong.add(t + ": cuboid " + k + " of " + names[v] + " draws sheet px " + java.util.Arrays.toString(bounds) + ", want " + java.util.Arrays.toString(want)); break; }
                        if (fdens > 0) for (int i = 0; i < 4; i++) {
                            float texel = bounds[i] * fdens;
                            if (Math.abs(texel - Math.round(texel)) > 0.01f) { wrong.add(t + ": cuboid " + k + " of " + names[v] + " has a uv corner off the texel grid: " + java.util.Arrays.toString(bounds) + " x " + fdens); break; }
                        }
                        for (int i = 0; i < 6; i++) if (Math.abs(b[i] - box0[i]) > 1e-4f) { wrong.add(t + ": cuboid " + k + " of " + names[v] + " has another box than " + names[0]); break; }
                        uvs++;
                    }
                }
                ok.append(t).append(" (").append(String.join(", ", names)).append(", ").append(fdens).append(" texel").append(fdens == 1 ? "" : "s").append(" a face px on the sheet ").append(java.util.Arrays.toString(sheet[0])).append("), ");
            }
            // a troop with no faces block (count 1 in the art file, the Templar's great helm) loads with no face bones and no faces key
            StringBuilder bare = new StringBuilder();
            for (String t : new TreeSet<>(TroopModels.ids())) {
                var m = TroopModels.model(t);
                if (m == null || m.faceCount > 0 || ArmyCards.spec(t) == null) continue;
                var parts = partsOf(m);
                for (String n : parts.keySet()) if (n.startsWith("face_v")) wrong.add(t + " has no faces block but loads the face bone " + n);
                var look = new TroopModels.Look(ArmyCards.spec(t), 1, false, null);
                m.prepare(look, false, false, 2);
                for (String n : m.shownBones(m.shownMask())) if (n.startsWith("face_v")) wrong.add(t + " shows the face bone " + n);
                bare.append(t).append(", ");
            }
            // leave every model as a draw would next prepare it
            report("p2-faces-model", wrong.isEmpty() && draws > 0 && uvs > 0, troops.size() + " troops with faces: " + ok + draws + " prepare calls (variants -3 to " + (FACES + 5) + " at stages 1 to 3) each showed exactly the one face bone of the variant mod " + FACES
                + " and the same other bones as variant 0; " + uvs + " face bones read back from their ModelPart cuboids draw their slot's rectangle of the face sheet (each corner on a whole texel) and one shared box per bone box (head, beard); no face bones on the faceless models: " + bare + (wrong.isEmpty() ? "" : "; wrong: " + wrong));
        }

        void picks(MinecraftClient c) {
            List<String> wrong = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            Random rng = new Random(5);
            for (String t : troops) {
                var m = TroopModels.model(t);
                var row = ArmyCards.spec(t);
                MobEntity p = ArmyCards.portrait(row, 1, 1);
                if (p == null) { wrong.add(t + ": no portrait"); continue; }
                UUID keep = p.getUuid();
                int[] seen = new int[FACES];
                for (int i = 0; i < 64; i++) { p.setUuid(new UUID(rng.nextLong(), rng.nextLong())); int f = TroopModels.face(p, m); if (f != m.faceCard) wrong.add(t + " card portrait wears " + f + ", not the card face " + m.faceCard); }
                // undressed, the same client-only mob is a world troop: a pinned troop keeps its face, any other wears the face of its UUID, and all of them turn up
                TroopModels.dress(p, null);
                for (int i = 0; i < 400; i++) {
                    UUID id = new UUID(rng.nextLong(), rng.nextLong());
                    p.setUuid(id);
                    int f = TroopModels.face(p, m);
                    int want = m.faceFixed >= 0 ? m.faceFixed : TroopFaces.variant(id, m.faceCount);
                    if (f != want) wrong.add(t + " world troop " + id + " wears " + f + ", want " + want);
                    seen[f]++;
                }
                TroopModels.dress(p, "company");
                p.setUuid(keep);
                int kinds = 0;
                for (int n : seen) if (n > 0) kinds++;
                if (m.faceFixed >= 0 ? kinds != 1 : kinds != FACES) wrong.add(t + ": over 400 UUIDs " + kinds + " different faces (" + java.util.Arrays.toString(seen) + ")");
                // the posted troops, nothing forced: their natural faces
                for (var u : rows.get(t)) {
                    var e = client(c, u.mob);
                    int f = TroopModels.face(e, m), want = m.faceFixed >= 0 ? m.faceFixed : TroopFaces.variant(e.getUuid(), m.faceCount);
                    if (f != want || TroopModels.forcedFace(e) != -1) wrong.add(t + " posted " + e.getUuid() + " wears " + f + ", want " + want);
                }
                sb.append(t).append(m.faceFixed >= 0 ? " pinned to " + m.faceFixed : " " + java.util.Arrays.toString(seen)).append(", card ").append(m.faceCard).append("; ");
            }
            report("p2-faces-pick", wrong.isEmpty(), "a card portrait wears the card face for 64 random UUIDs; undressed it wears the pinned face or the face of its UUID for 400 (faces reached per troop: " + sb + ")" + (wrong.isEmpty() ? "" : "; wrong: " + wrong));
        }

        // ---- the shots ----
        void shoot(MinecraftClient c) {
            if (si >= shots.size()) { c.setScreen(null); cs = 2; wait = 0; return; }
            if (awayFor > 0) { litStart(c); return; } // a light recovery has the spectator 400 blocks away: count its 40 ticks here
            Shot s = shots.get(si);
            if (s.card() > 0) { card(c, s); return; }
            var row = rows.get(s.group());
            List<Entity> es = new ArrayList<>();
            for (var u : row) es.add(client(c, u.mob));
            boolean mixed = s.group().equals(MIXED);
            String troop = mixed ? mixedTroop : s.group();
            var m = TroopModels.model(troop);
            if (m == null || es.stream().anyMatch(e -> e == null)) { if (++held > 400) { bad.add(s.name() + ": no client entity or model"); next(); } return; }
            wait++;
            for (int k = 0; k < es.size(); k++) TroopModels.forceFace(es.get(k), mixed ? -1 : k); // an entity the client rebuilds loses its forced face
            if (wait == 1) {
                String g = s.group();
                int st = s.stage(), dist = s.dist();
                on = g;
                staged.put(g, st);
                server(c, b -> {
                    int gi = 0;
                    for (String k : rows.keySet()) place(k, k.equals(g), gi++);
                    for (var u : rows.get(g)) stage(u, st);
                    camera(b, 0, EYE, -dist, 0, 0);
                });
                marks = new int[es.size()];
                for (int k = 0; k < es.size(); k++) { var d = TroopRenderer.drawn(es.get(k)); marks[k] = d == null ? 0 : d.count; }
                settle = 12; steady = 0; held = 0;
                return;
            }
            if (wait < 6 + settle) return;
            List<String> why = new ArrayList<>();
            List<Integer> variants = new ArrayList<>();
            List<String> bones = new ArrayList<>();
            String skin = "-";
            boolean far = false, sameSkin = true;
            for (int k = 0; k < es.size(); k++) {
                var e = es.get(k);
                var d = TroopRenderer.drawn(e);
                if (d == null || d.last == null || d.count <= marks[k]) { why.add("troop " + k + " not drawn since the setup"); continue; }
                int want = mixed ? TroopFaces.pick(e.getUuid(), m.faceCount, m.faceFixed, m.faceCard, false, -1) : k;
                variants.add(d.variant);
                if (d.variant != want) why.add("troop " + k + " wears " + d.variant + ", want " + want);
                if (d.last.look().armourTier() != s.stage()) why.add("troop " + k + " at stage " + d.last.look().armourTier());
                List<String> faces = new ArrayList<>(m.shownBones(d.last.bones()));
                faces.removeIf(n -> !n.startsWith("face_v"));
                bones.add(String.join("+", faces));
                if (faces.size() != 1 || !faces.get(0).equals("face_v" + Math.floorMod(want, m.faceCount))) why.add("troop " + k + " shows face bones " + faces);
                String sk = d.last.skin() == null ? "-" : d.last.skin().getPath().substring(d.last.skin().getPath().lastIndexOf('/') + 1);
                if (k == 0) { skin = sk; far = sk.endsWith("_far.png"); } else if (!sk.equals(skin)) sameSkin = false;
            }
            var p = c.player;
            boolean there = Math.abs(p.getX() - (site.getX() + .5)) < .05 && Math.abs(p.getEyeY() - (site.getY() + EYE)) < .05 && Math.abs(p.getZ() - (site.getZ() + .5 - s.dist())) < .05
                && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw())) < 1 && Math.abs(p.getPitch()) < 1;
            if (!there) why.add("camera not where it was set");
            boolean faced = true;
            for (var e : es) faced &= e instanceof net.minecraft.entity.LivingEntity le && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(le.getBodyYaw() - YAW)) < 2 && Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(le.prevBodyYaw - YAW)) < 2;
            if (!faced) why.add("a troop is still turning to face the camera");
            if (!sameSkin) why.add("the row bound different skins");
            steady = why.isEmpty() ? steady + 1 : 0;
            held++;
            if (steady >= 3 || held > 300) {
                if (!shotReady(c, s.name())) return;
                if (steady < 3) bad.add(s.name() + ": taken anyway after 300 ticks (" + why + ")");
                Phase2Checks.shot(c, s.name());
                double dist = Phase2Checks.Turn.distance(c, es.get(0));
                Fact f = new Fact(s.name(), troop, s.stage(), s.dist(), variants, bones, skin, far);
                facts.add(f);
                if (mixed) {
                    java.util.Set<Integer> kinds = new java.util.TreeSet<>(variants);
                    mixedKinds = kinds.size();
                    mixedFacts.add(variants.toString());
                    mixedOk = steady >= 3 && kinds.size() >= 2;
                }
                System.out.println(String.format(Locale.ROOT, "[p2-faces-fact] %s: %s stage %d, %d blocks (first troop %.2f from the camera), variants drawn %s, face bones %s, skin %s", s.name(), troop, s.stage(), s.dist(), dist, variants, bones, skin));
                next();
            }
        }

        /** Client: a card shot: the troop's portrait at stage 1 on the cards' ground at a px a block, drawn the way the cards draw it (ArmyCards.draw). */
        void card(MinecraftClient c, Shot s) {
            wait++;
            var m = TroopModels.model(s.group());
            if (wait == 1) {
                TroopDepth.enabled = true; // the relief on, as the cards draw it (a flat setting left by another suite would change the card)
                var row = ArmyCards.spec(s.group());
                cardMob = row == null ? null : ArmyCards.portrait(row, 1, 1);
                if (cardMob == null || m == null) { cardBad.add(s.name() + ": no card portrait"); next(); return; }
                cardMob.age = c.player == null ? 0 : c.player.age;
                var d = TroopRenderer.drawn(cardMob);
                cardMark = d == null ? 0 : d.count;
                c.setScreen(new Phase2Checks.Turn.T3Card(cardMob, ArmyCards.size(cardMob, s.card())));
                return;
            }
            if (wait == 30) {
                var d = TroopRenderer.drawn(cardMob);
                if (d == null || d.last == null || d.count - cardMark < 5) cardBad.add(s.name() + ": the portrait drew " + (d == null ? 0 : d.count - cardMark) + " times");
                else {
                    List<String> faces = new ArrayList<>(m.shownBones(d.last.bones()));
                    faces.removeIf(n -> !n.startsWith("face_v"));
                    String skin = d.last.skin() == null ? "-" : d.last.skin().getPath().substring(d.last.skin().getPath().lastIndexOf('/') + 1);
                    int px = ArmyCards.size(cardMob, s.card());
                    boolean wantFar = m.density > 1 && px < Phase2Checks.Turn.T3_CARD_PER_DENSITY * m.density;
                    if (d.variant != m.faceCard) cardBad.add(s.name() + ": the card wears face " + d.variant + ", not the card face " + m.faceCard);
                    if (!faces.equals(List.of("face_v" + m.faceCard))) cardBad.add(s.name() + ": the card shows face bones " + faces);
                    if (skin.endsWith("_far.png") != wantFar) cardBad.add(s.name() + ": the card (" + px + " px a block) bound " + skin + ", want " + (wantFar ? "the _far twin" : "the full skin"));
                    cardChecks++;
                    System.out.println(String.format(Locale.ROOT, "[p2-faces-fact] %s: card %d px a block, variant drawn %d, face bones %s, skin %s", s.name(), px, d.variant, faces, skin));
                }
                Phase2Checks.shot(c, s.name());
                c.setScreen(null);
                next();
            }
        }

        void finals(MinecraftClient c) {
            int worlds = facts.size() - (mixedTroop.isEmpty() ? 0 : 1);
            report("p2-faces-shots", bad.isEmpty() && worlds > 0, worlds + " world shots (" + troops + " at 3, 6, 24 and 36 blocks at stage 1, and at 3 and 6 blocks at stage 3 for the troops that have a level 3), each with its four troops forced to variants 0 to 3 drawn wearing them, one face bone shown each, the camera where it was set (0.05 blocks, 1 degree) and the troops facing it"
                + (bad.isEmpty() ? "" : ", not as asked: " + bad));
            List<String> wrong = new ArrayList<>();
            int far = 0, full = 0;
            boolean anyDense = false;
            for (String t : troops) anyDense |= TroopModels.model(t).density > 1;
            for (Fact f : facts) {
                var m = TroopModels.model(f.troop());
                boolean want = m.density > 1 && f.dist() >= (int) Phase2Checks.Turn.T3_NEAR;
                if (f.far() != want) wrong.add(f.shot() + " bound " + f.skin() + ", want " + (want ? "the _far twin" : "the full skin"));
                if (f.far()) far++; else full++;
            }
            report("p2-faces-skin", wrong.isEmpty() && (!anyDense || far > 0), far + " shots bound a _far twin and " + full + " the full skin: a density above 1 troop binds the twin from " + (int) Phase2Checks.Turn.T3_NEAR + " blocks out (its faces are in the twin: the build composites the patches before it shrinks the skin) and a density 1 troop never does"
                + (wrong.isEmpty() ? "" : "; wrong: " + wrong) + (anyDense && far == 0 ? " (no far shot: BAD)" : ""));
            report("p2-faces-cards", cardBad.isEmpty() && cardChecks > 0, cardChecks + " card portraits (160 px and 36 px) each drew the card face and exactly its face bone, the _far twin under " + Phase2Checks.Turn.T3_CARD_PER_DENSITY + " x density px a block" + (cardBad.isEmpty() ? "" : ", wrong: " + cardBad));
            if (!mixedTroop.isEmpty())
                report("p2-faces-mixed", mixedOk && mixedFacts.size() == 1, "eight " + mixedTroop + " with nothing forced wore " + mixedFacts + " (each its UUID's TroopFaces.variant; " + mixedKinds + " different faces, at least 2 wanted)");
        }
    }
}
