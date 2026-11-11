package gg.gravesandglory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * The 2026-11-11 title rename on an old save: a profile written before the rename, holding the chapter title "Veteran" and a
 * Mastery title "Order Squire Veteran", loads through the same steps as Campaign.readProfile with the titles renamed in place,
 * the worn title following, nothing else changed, and a second load changing nothing. Never shipped.
 */
public final class TitleRenameCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create(); // same settings as Campaign.JSON

    static Profile load(Path file) throws Exception {
        Profile p = ProfileMigration.upgradeFile(file, text -> JSON.fromJson(text, ProfileMigration.V1.class), text -> JSON.fromJson(text, Profile.class), JSON::toJson, RosterMath.Catalog.fromSpec());
        if (p == null) p = JSON.fromJson(Files.readString(file), Profile.class);
        if (p == null) throw new IllegalStateException("Empty profile");
        p.sanitize();
        return p;
    }
    /** The fixture with the given titles earned and the given one worn, as an old build would have saved it. */
    static Path old(Path dir, String name, List<String> titles, String worn) throws Exception {
        JsonObject j = JsonParser.parseString(Files.readString(Paths.get("test-tools/resources/fixtures/profile-v3-veteran.json"))).getAsJsonObject();
        JsonObject g = j.getAsJsonObject("progression");
        JsonArray a = new JsonArray();
        for (String t : titles) a.add(t);
        g.add("titles", a);
        g.addProperty("title", worn);
        Path file = dir.resolve(name);
        Files.writeString(file, JSON.toJson(j), StandardCharsets.UTF_8);
        return file;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("gg-title-rename");
        try {
            Profile plain = load(old(dir, "plain.json", List.of("Recruit"), "Recruit"));

            Profile a = load(old(dir, "chapter.json", List.of("Recruit", "Scout", "Captain", "Veteran", "Order Squire Veteran", "Oathsworn"), "Veteran"));
            check(new ArrayList<>(a.progression.titles).equals(List.of("Recruit", "Scout", "Captain", "Banneret", "Order Squire Storied", "Oathsworn")),
                "an old save's titles are renamed in place and keep their order: " + a.progression.titles);
            check(a.progression.title.equals("Banneret"), "the worn chapter title follows the rename: " + a.progression.title);

            Profile b = load(old(dir, "mastery.json", List.of("Recruit", "Order Squire Veteran"), "Order Squire Veteran"));
            check(b.progression.title.equals("Order Squire Storied") && b.progression.titles.contains("Order Squire Storied") && !b.progression.titles.contains("Order Squire Veteran"),
                "the worn Mastery title follows the rename: " + b.progression.title + " of " + b.progression.titles);

            for (Profile p : List.of(a, b))
                for (String t : p.progression.titles) check(!t.contains("Veteran"), "no title says Veteran after the load: " + t);

            // nothing but the titles differs from the same save without them
            JsonObject pa = JSON.toJsonTree(a).getAsJsonObject(), pp = JSON.toJsonTree(plain).getAsJsonObject();
            pa.getAsJsonObject("progression").remove("titles"); pa.getAsJsonObject("progression").remove("title");
            pp.getAsJsonObject("progression").remove("titles"); pp.getAsJsonObject("progression").remove("title");
            check(pa.equals(pp), "the rename changes the titles and nothing else in the profile");

            // saved and loaded again: the same profile, so the rename is settled after one load
            Path again = dir.resolve("again.json");
            Files.writeString(again, JSON.toJson(a), StandardCharsets.UTF_8);
            Profile c = load(again);
            check(JSON.toJsonTree(c).equals(JSON.toJsonTree(a)), "a second load of the renamed save changes nothing");

            // a save that never had the old titles is untouched, and a title that only contains the word is left alone
            Profile d = load(old(dir, "other.json", List.of("Recruit", "Bannerlord", "Veterans Guard"), "Bannerlord"));
            check(new ArrayList<>(d.progression.titles).equals(List.of("Recruit", "Bannerlord", "Veterans Guard")) && d.progression.title.equals("Bannerlord"),
                "titles that are not the two renamed forms are kept: " + d.progression.titles);
        } finally {
            try (var walk = Files.walk(dir)) { walk.sorted(Comparator.reverseOrder()).forEach(f -> f.toFile().delete()); }
        }
        System.out.println("TitleRenameCheck: " + checks + " checks passed");
    }
}
