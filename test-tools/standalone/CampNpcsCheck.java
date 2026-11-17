package gg.gravesandglory;

/**
 * The camp NPC name match (lobby NPC spec 5, the resolve check): every campStations row with a look resolves by its villager's name to that
 * look; a row without a look, an unknown name, no name and a villager with its AI on all draw vanilla. Never shipped.
 */
public final class CampNpcsCheck {
    static int checks, failed;

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) { failed++; System.out.println("FAIL " + message); }
    }

    public static void main(String[] args) {
        int looks = 0;
        for (var s : Spec.CAMPSTATIONS) {
            if (s.npc().equals("-")) { check(CampNpcs.look(s) == null, s.id() + ": no npc, no look"); continue; }
            String name = CampNpcs.name(s);
            check(!name.contains(" / ") && !name.isBlank(), s.id() + ": villager name '" + name + "' is the label before ' / '");
            var want = CampNpcs.look(s);
            var got = CampNpcs.resolve(name, true);
            if (s.look().equals("-")) check(got == null, s.id() + ": look - draws vanilla, resolved " + (got == null ? null : got.id()));
            else {
                looks++;
                check(want != null && want.id().equals(s.look()), s.id() + ": look " + s.look() + " is a campNpcs row");
                check(got == want, s.id() + ": '" + name + "' resolves to " + s.look() + ", got " + (got == null ? null : got.id()));
                check(CampNpcs.resolve(name, false) == null, s.id() + ": the same name with AI on (a renamed village villager) draws vanilla");
            }
        }
        check(looks >= 1, "at least one station has a look (the blacksmith template)");
        var smith = CampNpcs.resolve("Blacksmith", true);
        check(smith != null && smith.id().equals("blacksmith"), "the forge villager 'Blacksmith' draws the blacksmith look");
        check(smith != null && smith.scale() > 1 && CampNpcs.hitBox(smith), "the blacksmith is bigger than a villager and has an interaction box");
        check(smith != null && CampNpcs.lines(smith).length == 3, "the blacksmith has his three lines");
        check(CampNpcs.resolve(null, true) == null, "an unnamed villager draws vanilla");
        check(CampNpcs.resolve("Villager", true) == null && CampNpcs.resolve("blacksmith", true) == null && CampNpcs.resolve("Blacksmith ", true) == null,
            "an unknown or near-miss name draws vanilla (the match is exact)");
        // the staff NPCs of lobby NPC spec 4.2 to 4.5, the Blacksmith and Madame Fleece (Kort 2026-11-16), by their villager names, with the spec's vanilla profession and skin
        String[][] staff = {{"quartermaster", "armorer", "desert"}, {"archivist", "librarian", "snow"}, {"steward", "cartographer", "swamp"}, {"drillmaster", "weaponsmith", "savanna"}, {"blacksmith", "toolsmith", "taiga"}, {"tailor", "shepherd", "savanna"}};
        for (String[] w : staff) {
            Spec.CampStationsRow row = null;
            for (var s : Spec.CAMPSTATIONS) if (s.id().equals(w[0])) row = s;
            check(row != null, w[0] + ": a campStations row");
            if (row == null) continue;
            check(row.look().equals(w[0]) && row.npc().equals(w[1]) && row.villagerType().equals(w[2]), w[0] + ": look " + w[0] + ", " + w[1] + ", " + w[2] + " (got " + row.look() + ", " + row.npc() + ", " + row.villagerType() + ")");
            var l = CampNpcs.resolve(CampNpcs.name(row), true);
            check(l != null && l.id().equals(w[0]), w[0] + ": its villager name '" + CampNpcs.name(row) + "' resolves to its look");
            if (l == null) continue;
            int n = CampNpcs.lines(l).length;
            check(n >= 2 && n <= 3, w[0] + ": 2 or 3 lines, has " + n);
            check(!l.lines().toLowerCase().contains("giant"), w[0] + ": no line names the giants");
            check(!l.name().isBlank() && CampNpcs.plainLine(row.page()).startsWith(Character.toUpperCase(l.name().charAt(0)) + l.name().substring(1) + ": ")
                && CampNpcs.plainLine(row.page()).contains("needs the Graves and Glory modpack"), w[0] + ": the plain chat line names " + l.name() + ": " + CampNpcs.plainLine(row.page()));
        }
        check(CampNpcs.hasNpc(CampNpcs.WARDROBE) && CampNpcs.plainLine(CampNpcs.WARDROBE).startsWith("Madame Fleece: "), "the Wardrobe is Madame Fleece's: " + CampNpcs.plainLine(CampNpcs.WARDROBE));
        check(CampNpcs.wardrobeSoon().equals("Madame Fleece: the Wardrobe opens soon."), "her modded line: " + CampNpcs.wardrobeSoon());
        check(CampNpcs.storeLine("").endsWith("link coming soon.") && CampNpcs.storeLine("https://store.example").endsWith("Store: https://store.example"), "her store line, with and without a URL");
        check(CampNpcs.hasNpc("contracts") && CampNpcs.hasNpc("friends") && CampNpcs.hasNpc("forge") && !CampNpcs.hasNpc("match") && !CampNpcs.hasNpc("home") && !CampNpcs.hasNpc("chapter"),
            "only staff NPC pages get the chat line; gates, welcome and founders keep the action-bar line");
        check(CampNpcs.plainLine("nope").equals("This station needs the Graves and Glory modpack."), "a page with no villager gets the bare line");
        for (var r : Spec.CAMPNPCS) check(r.scale() > 0 && (r.hitWidth() == 0) == (r.hitHeight() == 0), r.id() + ": scale above 0, hit box both or neither");
        System.out.println(checks + " checks, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
