package gg.gravesandglory;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Where each camp NPC stands on one map (Kort 2026-11-16: placement eggs). Pure: no Minecraft, so CampNpcPlacementsCheck runs it standalone.
 * A map's file holds the NPCs placed there, relative to the map's origin (the camp's origin on the live camp, 0,0 on a builder canvas):
 * {"version":1,"map":"camp-4","npcs":{"tailor":{"dx":16.5,"y":77,"dz":-58.5,"yaw":0}}}.
 * Precedence: the world save's file (an egg or a reset writes it at once), then the file shipped in the mod beside the map overlay
 * (data/gravesandglory/adoption/<map>.npcs.json, baked by tools/mapforge adopt npcs), then the campStations row.
 */
final class CampNpcPlacements {
    static final int VERSION = 1;
    /** Every coordinate a placement may hold, from the map's origin: well past any canvas or the camp island, so a typo cannot fling an NPC off the world. */
    static final double REACH = 4096, MIN_Y = -64, MAX_Y = 320;

    record Place(double dx, double y, double dz, float yaw) {}

    /** The world save's file name for a map: one file per map, so the lobby world and every builder canvas keep their own. */
    static String fileName(String map) { return "gg-npcs-" + map + ".json"; }
    /** The shipped file's resource path for a map, beside its block overlay. */
    static String resource(String map) { return "/data/gravesandglory/adoption/" + map + ".npcs.json"; }

    /** The NPC ids an egg can place: every campStations row with a villager. */
    static Set<String> ids() {
        var out = new TreeSet<String>();
        for (var s : Spec.CAMPSTATIONS) if (!s.npc().equals("-")) out.add(s.id());
        return out;
    }

    /** A map id a file may carry: lower case letters, digits, - and _, as the overlay ids. */
    static boolean validMap(String map) { return map != null && map.matches("[a-z0-9_-]{1,64}"); }

    /**
     * The placements of a file for map. Throws IllegalArgumentException on a file that is not this version, names another map, or holds a
     * coordinate that is not finite or out of reach. An id that is no longer an NPC (a row removed from the sheet) is skipped, not an error.
     */
    static Map<String, Place> parse(String json, String map) {
        JsonObject o;
        try { o = JsonParser.parseString(json).getAsJsonObject(); } catch (RuntimeException e) { throw new IllegalArgumentException("not a JSON object: " + e.getMessage()); }
        if (!o.has("version") || o.get("version").getAsInt() != VERSION) throw new IllegalArgumentException("version is not " + VERSION);
        String in = o.has("map") ? o.get("map").getAsString() : "";
        if (!in.equals(map)) throw new IllegalArgumentException("the file is for map '" + in + "', not '" + map + "'");
        var out = new TreeMap<String, Place>();
        if (!o.has("npcs")) return out;
        var known = ids();
        for (var e : o.getAsJsonObject("npcs").entrySet()) {
            if (!known.contains(e.getKey())) continue;
            var p = e.getValue().getAsJsonObject();
            double dx = num(p, "dx", e.getKey()), y = num(p, "y", e.getKey()), dz = num(p, "dz", e.getKey()), yaw = num(p, "yaw", e.getKey());
            if (Math.abs(dx) > REACH || Math.abs(dz) > REACH || y < MIN_Y || y > MAX_Y) throw new IllegalArgumentException(e.getKey() + ": out of reach (" + dx + ", " + y + ", " + dz + ")");
            out.put(e.getKey(), new Place(dx, y, dz, wrap((float) yaw)));
        }
        return out;
    }
    static double num(JsonObject p, String key, String id) {
        if (!p.has(key)) throw new IllegalArgumentException(id + ": no " + key);
        double v = p.get(key).getAsDouble();
        if (!Double.isFinite(v)) throw new IllegalArgumentException(id + ": " + key + " is not finite");
        return v;
    }

    /** The file for map: sorted ids, so a placement changes one block of the file in a diff. */
    static String write(String map, Map<String, Place> places) {
        var o = new JsonObject();
        o.addProperty("version", VERSION);
        o.addProperty("map", map);
        var npcs = new JsonObject();
        for (var e : new TreeMap<>(places).entrySet()) {
            var p = new JsonObject();
            p.addProperty("dx", e.getValue().dx()); p.addProperty("y", e.getValue().y()); p.addProperty("dz", e.getValue().dz()); p.addProperty("yaw", e.getValue().yaw());
            npcs.add(e.getKey(), p);
        }
        o.add("npcs", npcs);
        return new GsonBuilder().setPrettyPrinting().create().toJson(o) + "\n";
    }

    /** Where an NPC stands: the world save's placement, else the shipped one, else null (the campStations row). */
    static Place pick(String id, Map<String, Place> world, Map<String, Place> shipped) {
        var p = world.get(id);
        return p != null ? p : shipped.get(id);
    }

    /** A copy of places with id at p (p null: the placement removed, so the NPC goes back to the shipped spot or its row). */
    static Map<String, Place> with(Map<String, Place> places, String id, Place p) {
        var out = new LinkedHashMap<>(places);
        if (p == null) out.remove(id); else out.put(id, p);
        return out;
    }

    /** The yaw an NPC placed by a player takes: it faces the player (their yaw turned round), snapped to the nearest 45 degrees, in -180 to 180. */
    static float facing(float playerYaw) { return wrap(Math.round((playerYaw + 180f) / 45f) * 45f); }
    static float wrap(float yaw) { float y = ((yaw % 360f) + 360f) % 360f; return y > 180f ? y - 360f : y; }
}
