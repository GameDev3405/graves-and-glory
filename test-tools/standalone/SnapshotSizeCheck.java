package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.*;

/** The worst saved profile must still fit the camp snapshot (Campaign.SNAPSHOT_LIMIT, 16000 characters: the client drops its connection on a longer string) and
 *  the 32000-character import cap. Worst: the most banners, each a full table of the longest troop id, 16-character names, every troop owned at its top stars, and a full gear inventory. */
public final class SnapshotSizeCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }

    static final int SNAPSHOT_LIMIT = 16000, IMPORT_LIMIT = 32000;

    public static void main(String[] args) {
        var c = RosterMath.Catalog.fromSpec();
        String longest = c.house().keySet().stream().max(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder())).orElseThrow();
        Profile p = new Profile();
        p.sanitize();
        p.banners = new ArrayList<>();
        for (int i = 0; i < RosterMath.maxBanners(); i++) {
            SortedMap<Integer, String> t = new TreeMap<>();
            for (int cell = 0; cell < RosterMath.maxTroops(); cell++) t.put(cell, longest);
            p.banners.add(new Profile.Banner("N".repeat(Profile.MAX_BANNER_NAME), WarGrid.format(t)));
        }
        for (var e : p.owned.entrySet()) { e.getValue().copies = 99; e.getValue().stars = 3; e.getValue().kills = 99999; e.getValue().survived = 99999; e.getValue().mastery = 99999; }
        for (String id : c.house().keySet()) if (!p.owned.containsKey(id)) { var o = new Profile.Owned(); o.copies = 99; o.stars = 3; o.kills = 99999; o.survived = 99999; o.mastery = 99999; p.owned.put(id, o); }
        p.troopWrits = p.gearShards = 1_000_000; p.pacts = 99999; p.matches = 99999; p.wins = 99999;
        p.notice = "Your banner no longer fits the table; 40 troops were taken off the back rows.";
        int slots = Spec.FORGE[0].maxSlots();
        p.inventorySize = slots;
        String item = Spec.GEARITEMS[0].id(), rarity = Spec.GEARRARITIES[Spec.GEARRARITIES.length - 1].id();
        for (var it : Spec.GEARITEMS) if (it.id().length() > item.length()) item = it.id();
        for (int i = 0; i < slots; i++) p.gear.add(new Gear.Piece(1_000_000 + i, item, rarity, 100, 100));

        Gson pretty = new GsonBuilder().setPrettyPrinting().create(), compact = new Gson();
        // the client's copy, as Campaign.forClient makes it: no match grants, no gear list (it travels on gear_v1)
        JsonObject j = pretty.toJsonTree(p).getAsJsonObject();
        var progression = j.getAsJsonObject(Profile.PROGRESSION);
        if (progression != null) progression.remove(Progression.State.MATCH_GRANTS);
        j.remove(Profile.GEAR);
        int prettyLen = pretty.toJson(j).length(), compactLen = compact.toJson(j).length();
        int importLen = pretty.toJson(p).length();
        System.out.println("worst profile: client pretty " + prettyLen + ", client compact " + compactLen + ", import " + importLen + " (" + slots + " gear pieces, longest troop id " + longest + ")");
        check(compactLen <= SNAPSHOT_LIMIT, "The client copy of the worst profile fits SNAPSHOT_LIMIT compact: " + compactLen);
        check(importLen <= IMPORT_LIMIT, "The import JSON of the worst profile fits the 32000 cap: " + importLen);
        System.out.println("SnapshotSizeCheck passed: " + count + " checks");
    }
}
