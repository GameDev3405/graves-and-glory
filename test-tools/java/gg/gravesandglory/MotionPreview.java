package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Offline animation preview (troop animation pass): runs the game's own MotionEngine on one troop's clips, as TroopMotion builds them
 * (its clips/<rig>/<troop>/ folder over clips/<rig>/<family>/ over clips/<rig>/common/, troopAttacks picks, gait clips and idle
 * variants), through a scripted scene, and writes every frame's pose after TroopMotion's carries and joint limits. Pure Java, no
 * client: tools/troop_anim.py draws the frames on the troop's model (tools/troop_render.py). What it leaves out of TroopMotion: root
 * scaling for taller troops, the illager fold and the lying snapshot. Never shipped.
 *
 *   java gg.gravesandglory.MotionPreview <scene.json> <out.json>
 *
 * The scene: {"troop", "fps", "seed", "length", "events": [{"t", "do", ...}]} with do one of
 *   walk {"speed" b/s forward, "left" b/s}, stance {"stance"}, enemy {"on"}, attack {"clip"} (the troop's attack of that clip name,
 *   on the variant whose slot it fills), hit {"zone", "kind", "weight", "bearing", "blocked"}, die {"bearing", "kind"}, revive {},
 *   scene {"clip"} (a scene pose plays that clip whole-body, as setScenePose does; "" ends it), calm {"idle": index} (starts that idle
 *   variant now).
 */
public final class MotionPreview {
    static final String ASSETS = "src/main/resources/assets/gravesandglory/";
    static final Pattern HIT = Pattern.compile("hit_(head|body|legs)(_\\d+)?"), BLOCK = Pattern.compile("block(?:_(head|body|legs))?(_\\d+)?");
    static final List<String> STANCES = List.of("idle", "carry", "guard", "brace", "wall", "shield", "aim", "rest", "rally"), GAITS = List.of("walk", "run");

    record Built(MotionEngine.Family family, Map<String, Clip> all, List<String> attackIds, Map<String, Integer> variantOf) {}

    static Map<String, Clip> folder(String rig, String name) throws Exception {
        Map<String, Clip> out = new TreeMap<>();
        Path dir = Path.of(ASSETS + "clips/" + rig + "/" + name);
        if (!Files.isDirectory(dir)) return out;
        try (var s = Files.list(dir)) {
            for (Path p : s.sorted().toList()) {
                if (!p.toString().endsWith(".json")) continue;
                Clip c = Clip.parse(JsonParser.parseString(Files.readString(p)).getAsJsonObject());
                out.put(c.name(), c);
            }
        }
        return out;
    }

    /** As TroopMotion.build: the troop's family, its attack ids in engine order and each attack's server variant. */
    static Built build(String rig, String family, String troop) throws Exception {
        Map<String, Clip> all = new TreeMap<>(folder(rig, "common"));
        all.putAll(folder(rig, family));
        Map<String, Clip> mine = troop.equals(family) ? Map.of() : folder(rig, troop);
        all.putAll(mine);
        Map<String, Clip> stances = new HashMap<>();
        for (String s : STANCES) if (all.containsKey(s)) stances.put(s, all.get(s));
        for (String s : GAITS) if (all.containsKey(s)) stances.put(s, all.get(s));
        for (var e : all.entrySet()) if (e.getKey().startsWith(MotionBody.IDLE_ALT)) stances.put(e.getKey(), e.getValue());
        Map<String, Clip> own = new TreeMap<>(folder(rig, family));
        own.putAll(mine);
        List<Spec.AttackVariantsRow> rows = new ArrayList<>();
        for (Spec.AttackVariantsRow r : Spec.ATTACKVARIANTS) if (r.family().equals(family)) rows.add(r);
        List<Clip> attacks = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        Map<String, Integer> variantOf = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            List<String> names = new ArrayList<>();
            for (Spec.TroopAttacksRow t : Spec.TROOPATTACKS) if (t.troop().equals(troop) && t.slot().equals(rows.get(i).id()) && mine.containsKey(t.clip())) names.add(t.clip());
            if (names.isEmpty() && own.containsKey(rows.get(i).id())) names.add(rows.get(i).id());
            for (String n : names) { attacks.add(own.get(n)); ids.add(n); variantOf.put(n, i); }
        }
        List<Clip> wall = new ArrayList<>();
        boolean anyWall = false;
        for (int k = 0; k < attacks.size(); k++) { Clip w = own.get(ids.get(k) + "_wall"); anyWall |= w != null; wall.add(w != null ? w : attacks.get(k)); }
        if (!anyWall) wall.clear();
        Map<String, List<Clip>> hits = new HashMap<>(), deaths = new HashMap<>();
        boolean ownHits = mine.keySet().stream().anyMatch(x -> HIT.matcher(x).matches()), ownBlocks = mine.keySet().stream().anyMatch(x -> BLOCK.matcher(x).matches()),
            ownDeaths = mine.keySet().stream().anyMatch(x -> MotionEngine.deathKey(x) != null);
        for (var e : all.entrySet()) {
            String n0 = e.getKey();
            boolean inherited = !mine.containsKey(n0);
            if (inherited && (ownHits && HIT.matcher(n0).matches() || ownBlocks && BLOCK.matcher(n0).matches() || ownDeaths && MotionEngine.deathKey(n0) != null)) continue;
            String n = e.getKey();
            Matcher h = HIT.matcher(n);
            if (h.matches()) hits.computeIfAbsent(h.group(1), k -> new ArrayList<>()).add(e.getValue());
            else if ((h = BLOCK.matcher(n)).matches()) hits.computeIfAbsent(h.group(1) == null || h.group(1).equals("body") ? "block" : "block_" + h.group(1), k -> new ArrayList<>()).add(e.getValue());
            else { String d = MotionEngine.deathKey(n); if (d != null) deaths.computeIfAbsent(d, k -> new ArrayList<>()).add(e.getValue()); }
        }
        return new Built(new MotionEngine.Family(family, stances, attacks, wall, hits, deaths), all, ids, variantOf);
    }

    public static void main(String[] args) throws Exception {
        JsonObject scene = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        String troop = scene.get("troop").getAsString();
        JsonObject model = JsonParser.parseString(Files.readString(Path.of(ASSETS + "models/troop/" + troop + ".json"))).getAsJsonObject();
        String rig = model.get("rig").getAsString(), family = null;
        boolean shield = false, bow;
        for (Spec.TroopsRow r : Spec.TROOPS) if (r.id().equals(troop)) { family = r.family(); shield = !"none".equals(r.shield()); }
        if (family == null) throw new IllegalArgumentException("no troops row " + troop);
        bow = family.equals("bow");
        boolean left = "left".equals(str(model, "hand")), forward = "forward".equals(str(model, "idleArms"));
        Map<String, double[]> limits = new HashMap<>();
        Set<String> bones = new LinkedHashSet<>();
        for (JsonElement e : model.getAsJsonArray("bones")) {
            JsonObject b = e.getAsJsonObject();
            bones.add(b.get("name").getAsString());
            if (b.has("limit")) limits.put(b.get("name").getAsString(), new double[]{b.getAsJsonArray("limit").get(0).getAsDouble(), b.getAsJsonArray("limit").get(1).getAsDouble()});
        }
        Built built = build(rig, family, troop);
        double fps = scene.has("fps") ? scene.get("fps").getAsDouble() : 60, length = scene.get("length").getAsDouble();
        long seedBits = scene.has("seed") ? scene.get("seed").getAsLong() : 1;
        MotionMath.Seed seed = MotionMath.seed(seedBits * 0x9E3779B97F4A7C15L, seedBits ^ 0xC2B2AE3D27D4EB4FL);
        List<JsonObject> events = new ArrayList<>();
        for (JsonElement e : scene.getAsJsonArray("events")) events.add(e.getAsJsonObject());
        events.sort(Comparator.comparingDouble(e -> e.get("t").getAsDouble()));

        MotionEngine.State st = new MotionEngine.State();
        Pose out = new Pose(), sc = new Pose();
        double t0 = 1000, time = t0, limbAngle = 0, limbSpeed = 0, vF = 0, vL = 0, gx = 0, gz = 0, yaw = 0, deathSeconds = 0;
        String stance = "";
        boolean enemy = false, dead = false;
        MotionInput.Attack attack = null;
        MotionInput.Hit hit = null;
        String sceneClip = null;
        double sceneStart = 0, sceneW = 0;
        int next = 0;
        JsonArray frames = new JsonArray();
        double dt = 1 / fps;
        int n = (int) Math.round(length * fps);
        double[] tmp = new double[6];
        for (int f = 0; f <= n; f++) {
            double t = f * dt;
            while (next < events.size() && events.get(next).get("t").getAsDouble() <= t + 1e-9) {
                JsonObject e = events.get(next++);
                long tick = (long) Math.floor(time * 20);
                switch (e.get("do").getAsString()) {
                    case "walk" -> { vF = num(e, "speed", 0); vL = num(e, "left", 0); }
                    case "stance" -> stance = e.get("stance").getAsString();
                    case "enemy" -> enemy = e.get("on").getAsBoolean();
                    case "attack" -> {
                        String clip = e.get("clip").getAsString();
                        int idx = built.attackIds().indexOf(clip);
                        if (idx < 0) throw new IllegalArgumentException(troop + " has no attack " + clip + " (has " + built.attackIds() + ")");
                        attack = new MotionInput.Attack(idx, (long) Math.ceil(time * 20));
                    }
                    case "hit" -> hit = new MotionInput.Hit(str(e, "kind", "slash"), str(e, "zone", "body"), (float) num(e, "bearing", 0), (int) num(e, "weight", 1),
                        e.has("blocked") && e.get("blocked").getAsBoolean(), false, tick);
                    case "die" -> { hit = new MotionInput.Hit(str(e, "kind", "slash"), "body", (float) num(e, "bearing", 0), 2, false, true, tick); dead = true; deathSeconds = 0; }
                    case "revive" -> { dead = false; deathSeconds = 0; }
                    case "scene" -> { String c = e.get("clip").getAsString(); if (c.isEmpty()) sceneClip = null; else if (built.all().containsKey(c)) { sceneClip = c; sceneStart = time; sceneW = 0; } else throw new IllegalArgumentException("no clip " + c); }
                    case "calm" -> { st.noveltyNext = time; }
                    default -> throw new IllegalArgumentException("event " + e);
                }
            }
            double target = Math.min(1, Math.hypot(vF, vL) / 5);
            MotionInput in = new MotionInput(time, limbAngle, limbSpeed, 0, 0, 0, stance, 0f, false, enemy, dead, deathSeconds, attack, hit, seed,
                new MotionInput.Ground(gx, 64, gz, yaw, true));
            MotionEngine.pose(in, st, built.family(), out);
            // a scene pose: the clip whole-body over the engine, blended over SCENE_BLEND (TroopMotion.scene)
            if (sceneClip != null || sceneW > 0) {
                sceneW = Math.max(0, Math.min(1, sceneW + (sceneClip != null ? dt : -dt) / 0.25));
                Clip c = built.all().get(sceneClip != null ? sceneClip : "idle");
                if (c != null && sceneClip != null) {
                    double w = MotionMath.ease(sceneW);
                    sc.clear();
                    c.add(time - sceneStart, sc, 1, tmp);
                    for (int i = 0; i < out.size(); i++) { double[] r = out.row(i); for (int k = 0; k < 6; k++) r[k] *= 1 - w; }
                    for (int i = 0; i < sc.size(); i++) { double[] r = sc.row(i), o = out.get(sc.name(i)); for (int k = 0; k < 6; k++) o[k] += w * r[k]; }
                    MotionFeet.ground(out, st.vec);
                }
            }
            double[] shares = new double[Math.max(1, st.tracks.size())];
            MotionStack.shares(st.tracks, st.tracks.size(), shares);
            JsonObject pose = new JsonObject();
            Map<String, double[]> p = new LinkedHashMap<>();
            for (int i = 0; i < out.size(); i++) p.put(out.name(i), out.row(i).clone());
            // joint limits on pitch (TroopMotion.limits)
            for (var e : limits.entrySet()) { double[] r = p.get(e.getKey()); if (r != null) r[0] = Math.max(e.getValue()[0], Math.min(e.getValue()[1], r[0])); }
            // the carries (TroopMotion.carries)
            Clip scClip = sceneClip == null ? null : built.all().get(sceneClip);
            double sw = scClip == null ? 0 : sceneW;
            java.util.function.ToDoubleFunction<String> held = b -> Math.min(1, stanceHeld(st, b) + actionHeld(st, b, shares)) + sw * (scClip != null && scClip.bones().contains(b) ? 1 : 0);
            if (shield) { String arm = left ? "right_arm" : "left_arm"; p.computeIfAbsent(arm, k -> new double[6])[0] -= 45 * (1 - Math.min(1, held.applyAsDouble(arm))); }
            if (bow) {
                double draw = 0;
                for (int i = 0; i < st.tracks.size(); i++) { MotionStack.Track tr = st.tracks.get(i); if (tr.kind == MotionEngine.ATTACK && tr.time(time) < tr.length()) draw += shares[i]; }
                draw = Math.min(1, Math.max(draw, st.frame.aim));
                double[] h = p.computeIfAbsent(left ? "left_hand" : "right_hand", k -> new double[6]);
                h[0] += 90 * draw; h[1] += TroopMotion_BOW_REST_YAW * (1 - draw); h[2] += 90 * draw;
            }
            if (forward) for (String arm : List.of("right_arm", "left_arm")) p.computeIfAbsent(arm, k -> new double[6])[0] -= 80 * (1 - Math.min(1, held.applyAsDouble(arm)));
            for (var e : p.entrySet()) {
                if (!bones.contains(e.getKey())) continue;
                JsonArray a = new JsonArray();
                for (double v : e.getValue()) a.add(Math.round(v * 1000) / 1000.0);
                pose.add(e.getKey(), a);
            }
            JsonObject fr = new JsonObject();
            fr.addProperty("t", Math.round(t * 1e4) / 1e4);
            fr.add("pose", pose);
            fr.addProperty("psi", st.psi);
            if (bow) fr.addProperty("draw", drawSeconds(st, time));
            fr.addProperty("idle", st.novKind);
            fr.addProperty("gx", gx);
            fr.addProperty("gz", gz);
            frames.add(fr);
            // advance like Minecraft: the limb speed eases to its target and drives the limb angle; the ground moves by the velocity
            limbSpeed = MotionMath.approach(limbSpeed, target, 8, dt);
            if (limbSpeed < 1e-4 && target == 0) limbSpeed = 0;
            limbAngle += limbSpeed * 20 * dt;
            double y = Math.toRadians(yaw);
            gx += (vF * -Math.sin(y) + vL * Math.cos(y)) * dt;
            gz += (vF * Math.cos(y) + vL * Math.sin(y)) * dt;
            time += dt;
            if (dead) deathSeconds += dt;
        }
        JsonObject o = new JsonObject();
        o.addProperty("troop", troop);
        o.addProperty("family", family);
        o.addProperty("fps", fps);
        JsonArray ids = new JsonArray();
        built.attackIds().forEach(ids::add);
        o.add("attacks", ids);
        o.add("frames", frames);
        Gson g = new GsonBuilder().create();
        Files.writeString(Path.of(args[1]), g.toJson(o));
    }

    static final double TroopMotion_BOW_REST_YAW = 45;

    /** TroopMotion.drawSeconds: the bow's pull timeline seconds (-1: at rest). */
    static double drawSeconds(MotionEngine.State st, double now) {
        for (int i = st.tracks.size() - 1; i >= 0; i--) {
            MotionStack.Track t = st.tracks.get(i);
            if (t.kind != MotionEngine.ATTACK) continue;
            double s = t.time(now);
            if (s >= t.length()) break;
            if (t.impact > 0 && s >= t.impact) return 0.8 + (s - t.impact);
            Double d0 = t.a.markers().get("draw"), d1 = t.a.markers().get("full");
            if (d0 != null && d1 != null && d1 > d0) return s < d0 ? 0 : 0.65 * Math.min(1, (s - d0) / (d1 - d0));
            if (t.impact > 0) return s / t.impact * 0.8;
            return Math.min(s / Math.max(t.length(), 1e-3) * 0.65, 0.65);
        }
        return -1;
    }

    static double stanceHeld(MotionEngine.State st, String bone) {
        double carry = 1, w = 0;
        for (int i = st.stances.size() - 1; i >= 0 && carry > 0; i--) {
            MotionStack.Layer l = st.stances.get(i);
            double lw = MotionMath.ease(l.raw), e = lw * carry;
            carry *= 1 - lw;
            for (int c = 0; c < l.n; c++) if (l.clips[c] != null && l.clips[c].bones().contains(bone)) w += e * l.coef[c];
        }
        return Math.min(1, w);
    }

    static double actionHeld(MotionEngine.State st, String bone, double[] shares) {
        double w = 0;
        for (int i = 0, n = st.tracks.size(); i < n; i++) {
            MotionStack.Track t = st.tracks.get(i);
            if (t.kind == MotionEngine.MERGED) continue;
            if (t.a.bones().contains(bone)) w += t.gain * (1 - t.mix) * shares[i];
            if (t.b != null && t.b.bones().contains(bone)) w += t.gain * t.mix * shares[i];
        }
        return Math.min(1, w);
    }

    static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
    static double num(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
}
