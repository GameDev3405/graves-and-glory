package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.file.*;
import java.util.*;

/**
 * The chapter 1 campaign progress reset (brief M1, Kort D5, 2026-11-09): old saves made when the campaign was the six march-* battles load
 * through Campaign.readProfile's path (ProfileMigration.upgradeFile, Gson, Profile.sanitize) with chapter 1 at 0 of 6, no bonus claimed and
 * no chapter Pact paid, while the Pacts held, the troops owned (Legendaries included), the gold (now troop writs and gear shards), shards and gear stay. Clearing the oath
 * chapter then pays exactly one Pact. A save already past the reset (schema 7 now) is never reset again. Never shipped.
 */
public final class CampaignResetCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create(); // Campaign.JSON's settings

    /** Campaign.readProfile, on a file. */
    static Profile load(Path file) throws Exception {
        Profile p = ProfileMigration.upgradeFile(file, t -> JSON.fromJson(t, ProfileMigration.V1.class), t -> JSON.fromJson(t, Profile.class), JSON::toJson, RosterMath.Catalog.fromSpec());
        if (p == null) p = JSON.fromJson(Files.readString(file), Profile.class);
        p.sanitize();
        return p;
    }

    static JsonArray array(String... ids) { JsonArray a = new JsonArray(); for (String id : ids) a.add(id); return a; }

    /** A schema 5 save as the march-era build wrote it: all six march battles won, every march bonus claimed, the chapter 1 Pact paid. */
    static JsonObject marchSave(int writs) {
        Profile p = new Profile();
        Gear.grantStarter(p);
        p.sanitize();
        p.owned.put("gl_marshal", new Profile.Owned()); // a Legendary bought with a Pact
        p.shards.put("gilded", 17);
        JsonObject j = JSON.toJsonTree(p).getAsJsonObject();
        for (String k : new String[] {"banners", "activeBanner", "pacts", "troopWrits", "writGold", "gearShards", "notice"}) j.remove(k); // a march-era file has no schema 7 keys
        j.addProperty("schema", 5);
        j.addProperty("gold", 321); // the legacy purse and line, as the march-era build wrote them
        JsonArray line = new JsonArray(); for (String id : RosterMath.starterBanner()) line.add(id); j.add("banner", line);
        j.add("battlesWon", array("march-1", "march-2", "march-3", "march-4", "march-5", "march-6"));
        j.add("bonusClaimed", array("b1_hold_all", "b2_gate_by", "b3_lose8", "b4_never_rolled", "b5_hold_all", "b6_kill_hollowking"));
        j.addProperty("campaignCleared", 6);
        j.addProperty("writs", writs);
        JsonArray paid = new JsonArray(); paid.add(1); j.add("writChapters", paid);
        return j;
    }

    /** Wins every battle of chapter 1 in order, as Campaign settles them, and grants the Pacts as Campaign.pacts does; returns the Pacts granted. */
    static int clearChapterOne(Profile p) {
        for (var row : MarchMath.byOrder(Spec.CAMPAIGNBATTLES)) {
            if (row.chapter() != 1) continue;
            var lock = MarchMath.lockedBy(row, p.battlesWon, Spec.CAMPAIGNBATTLES);
            check(lock == null, "clear: " + row.id() + " is open in order");
            var settled = MarchMath.settle(row, MarchMath.bonus(row, Spec.BONUSOBJECTIVES), true, true, p.battlesWon, p.bonusClaimed, Profile.houseOrder(), Spec.CAMPAIGNBATTLES);
            check(settled.firstWin() && settled.bonusPaid(), "clear: " + row.id() + " is a first win and pays its new bonus (no old claim blocks it)");
            p.campaignCleared = settled.cleared();
        }
        var chapters = RosterMath.pactChapters(Arrays.asList(Spec.CAMPAIGNBATTLES), p.campaignCleared, p.writChapters);
        for (int c : chapters) { p.pacts++; p.writChapters.add(c); }
        return chapters.size();
    }

    static void resetHolds(Profile p, String name, int writs) {
        check(p.schema == Profile.SCHEMA && Profile.SCHEMA == 7 && !p.readOnly, name + ": loads at schema 7, writable");
        check(p.battlesWon.isEmpty() && p.campaignCleared == 0 && MarchMath.nextUnwon(p.battlesWon, Spec.CAMPAIGNBATTLES).id().equals("oath-1"), name + ": chapter 1 at 0 of 6, next battle oath-1: " + p.battlesWon);
        check(p.bonusClaimed.isEmpty(), name + ": no bonus claimed: " + p.bonusClaimed);
        check(!p.writChapters.contains(1), name + ": chapter 1's Pact is not marked paid, so clearing The Oath pays it: " + p.writChapters);
        check(p.pacts == writs, name + ": Pacts held stay " + writs + ", got " + p.pacts);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("gg-campaign-reset-check");
        try {
            // 1. schema 5, the last march-era shape: a Pact held, a Legendary owned
            Path five = dir.resolve("five.json");
            Files.writeString(five, JSON.toJson(marchSave(1)));
            Profile p = load(five);
            resetHolds(p, "schema-5", 1);
            check(p.owned.containsKey("gl_marshal") && p.troopWrits == 3 && p.gearShards == 21 && p.shards.get("gilded") == 17 && !p.gear.isEmpty(), "schema-5: the Legendary, the gold (321 is 3 troop writs and 21 gear shards), shards and gear stay");
            check(clearChapterOne(p) == 1 && p.pacts == 2 && p.writChapters.equals(Set.of(1)), "schema-5: clearing The Oath pays exactly one Pact (held 2): " + p.pacts);
            check(RosterMath.pactChapters(Arrays.asList(Spec.CAMPAIGNBATTLES), p.campaignCleared, p.writChapters).isEmpty(), "schema-5: once paid, the chapter pays no second Pact");
            // the reset runs once: the saved schema 7 profile loads unchanged
            Path six = dir.resolve("six.json");
            Files.writeString(six, JSON.toJson(p));
            Profile again = load(six);
            check(again.battlesWon.size() == 6 && again.campaignCleared == 6 && again.bonusClaimed.size() == 6 && again.writChapters.equals(Set.of(1)) && again.pacts == 2, "schema-7: a save past the reset keeps its oath progress and Pacts");
            check(JSON.toJson(again).equals(JSON.toJson(p)), "schema-7: loads and saves back unchanged");
            // 2. schema 5 with the Pact already spent on a Legendary: writs 0, the troop kept, one Pact earned again
            Path spent = dir.resolve("spent.json");
            Files.writeString(spent, JSON.toJson(marchSave(0)));
            Profile s = load(spent);
            resetHolds(s, "spent-pact", 0);
            check(s.owned.containsKey("gl_marshal") && clearChapterOne(s) == 1 && s.pacts == 1, "spent-pact: the Legendary stays and The Oath pays one Pact");
            // 3. the shipped schema 3 fixture (march-1 won, campaignCleared 1)
            Path v3 = dir.resolve("v3.json");
            Files.copy(Paths.get("test-tools/resources/fixtures/profile-v3-veteran.json"), v3);
            var fixture = JSON.fromJson(Files.readString(v3), JsonObject.class);
            Profile vet = load(v3);
            resetHolds(vet, "v3-fixture", fixture.get("writs").getAsInt());
            check(vet.owned.keySet().containsAll(JSON.fromJson(fixture.get("owned"), JsonObject.class).keySet().stream().filter(id -> Arrays.stream(Spec.TROOPS).anyMatch(t -> t.id().equals(id))).toList()), "v3-fixture: every owned troop stays");
            // 4. a schema 2 save with the whole march cleared: v2toV3 rebuilds battlesWon from campaignCleared, and the reset still empties it
            Path two = dir.resolve("two.json");
            JsonObject j2 = marchSave(1); j2.addProperty("schema", 2); j2.remove("battlesWon"); j2.remove("bonusClaimed");
            Files.writeString(two, JSON.toJson(j2));
            Profile old = load(two);
            resetHolds(old, "schema-2", 1);
            // 5. a fresh profile is untouched by the step
            Profile fresh = new Profile(); fresh.sanitize();
            check(fresh.schema == 7 && fresh.battlesWon.isEmpty() && fresh.pacts == 0, "fresh: schema 7, nothing won");
            // 6. the step alone, in place
            Profile direct = JSON.fromJson(JSON.toJson(marchSave(3)), Profile.class);
            check(ProfileMigration.v5toV6(direct) == direct && direct.schema == 6 && direct.battlesWon.isEmpty() && direct.bonusClaimed.isEmpty() && direct.campaignCleared == 0 && direct.writChapters.isEmpty() && direct.writs == 3 && direct.owned.containsKey("gl_marshal"), "v5toV6: in place; clears wins, claims, the cleared count and chapter 1's paid mark; writs and troops kept");
        } finally {
            try (var walk = Files.walk(dir)) { for (Path f : walk.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(f); }
        }
        System.out.println("CampaignResetCheck: " + checks + " checks passed");
    }

    static Profile copy(Profile p) { Profile c = JSON.fromJson(JSON.toJson(p), Profile.class); c.sanitize(); return c; }
}
