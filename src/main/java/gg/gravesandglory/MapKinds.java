package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The parchment look's block kinds (pure): battlemap/kinds.json parsed once. Rules are tried in order and the first match wins; a rule
 *  matches by exact id, id suffix or id prefix (namespace included). A rule with a "flat" kind gives that kind instead when the column
 *  does not stand {@link #raise} or more blocks above the local terrain, so a stair at ground level is paving, not a roof.
 *  The same rules as tools/battlemap_preview.py (classify and resolve), so the game and the prototype agree. */
public final class MapKinds {
    private record Rule(int kind, int flat, List<String> ids, List<String> suffix, List<String> prefix) {
        boolean matches(String id) {
            if (ids.contains(id)) return true;
            for (String s : suffix) if (id.endsWith(s)) return true;
            for (String p : prefix) if (id.startsWith(p)) return true;
            return false;
        }
    }

    private final String[] kinds;
    private final int raise, fallback;
    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, int[]> cache = new HashMap<>();

    private MapKinds(String[] kinds, int raise, int fallback) { this.kinds = kinds; this.raise = raise; this.fallback = fallback; }

    public static MapKinds parse(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        JsonArray ks = o.getAsJsonArray("kinds");
        String[] kinds = new String[ks.size()];
        Map<String, Integer> code = new HashMap<>();
        for (int i = 0; i < kinds.length; i++) { kinds[i] = ks.get(i).getAsString(); code.put(kinds[i], i); }
        var t = new MapKinds(kinds, o.get("raise").getAsInt(), code.get(o.get("default").getAsString()));
        for (JsonElement e : o.getAsJsonArray("rules")) {
            JsonObject r = e.getAsJsonObject();
            t.rules.add(new Rule(code.get(r.get("kind").getAsString()), r.has("flat") ? code.get(r.get("flat").getAsString()) : -1,
                strings(r, "ids"), strings(r, "suffix"), strings(r, "prefix")));
        }
        return t;
    }

    private static List<String> strings(JsonObject r, String key) {
        List<String> l = new ArrayList<>();
        if (r.has(key)) for (JsonElement e : r.getAsJsonArray(key)) l.add(e.getAsString());
        return l;
    }

    /** The kind names; an index from {@link #kind} points into this. */
    public String[] kinds() { return kinds.clone(); }

    /** How many blocks above the local terrain a column must stand to keep its raised kind (a building) over its flat one. */
    public int raise() { return raise; }

    /** The kind index of a block id. raised: the column stands {@link #raise} or more above the local terrain. */
    public int kind(String blockId, boolean raised) {
        int[] r = cache.computeIfAbsent(blockId, id -> {
            for (Rule rule : rules) if (rule.matches(id)) return new int[] {rule.kind(), rule.flat()};
            return new int[] {fallback, -1};
        });
        return r[1] >= 0 && !raised ? r[1] : r[0];
    }
}
