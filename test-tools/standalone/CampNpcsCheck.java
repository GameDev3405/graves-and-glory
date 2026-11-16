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
        for (var r : Spec.CAMPNPCS) check(r.scale() > 0 && (r.hitWidth() == 0) == (r.hitHeight() == 0), r.id() + ": scale above 0, hit box both or neither");
        System.out.println(checks + " checks, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
