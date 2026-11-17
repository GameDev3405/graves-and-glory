package gg.gravesandglory;

import java.util.Map;

/** The placement egg file (CampNpcPlacements): round trip, precedence, refusals and the facing snap. Never shipped. */
public final class CampNpcPlacementsCheck {
    static int checks, failed;

    static void check(boolean ok, String message) {
        checks++;
        if (!ok) { failed++; System.out.println("FAIL " + message); }
    }
    static boolean refused(String json, String map) {
        try { CampNpcPlacements.parse(json, map); return false; } catch (IllegalArgumentException e) { return true; }
    }

    public static void main(String[] args) {
        var ids = CampNpcPlacements.ids();
        for (String id : new String[]{"blacksmith", "quartermaster", "archivist", "steward", "drillmaster", "tailor"}) check(ids.contains(id), "ids has " + id);
        check(!ids.contains("gates") && ids.stream().allMatch(id -> java.util.Arrays.stream(Spec.CAMPSTATIONS).anyMatch(s -> s.id().equals(id) && !s.npc().equals("-"))), "ids are exactly the rows with a villager: " + ids);

        var a = new CampNpcPlacements.Place(16.5, 77, -58.5, 0f);
        var b = new CampNpcPlacements.Place(-3.5, 65, 12.5, -135f);
        var places = Map.of("tailor", a, "steward", b);
        String text = CampNpcPlacements.write("camp-4", places);
        var back = CampNpcPlacements.parse(text, "camp-4");
        check(back.equals(places), "round trip: " + back);
        check(text.indexOf("\"steward\"") < text.indexOf("\"tailor\""), "ids are written sorted");
        check(CampNpcPlacements.write("camp-4", back).equals(text), "writing a parsed file gives the same text");

        check(refused(text, "lobby"), "a file for another map is refused");
        check(refused("{\"version\":2,\"map\":\"camp-4\",\"npcs\":{}}", "camp-4"), "another version is refused");
        check(refused("not json", "camp-4"), "a broken file is refused");
        check(refused("{\"version\":1,\"map\":\"camp-4\",\"npcs\":{\"tailor\":{\"dx\":1,\"y\":65,\"dz\":1}}}", "camp-4"), "a placement with no yaw is refused");
        check(refused("{\"version\":1,\"map\":\"camp-4\",\"npcs\":{\"tailor\":{\"dx\":99999,\"y\":65,\"dz\":1,\"yaw\":0}}}", "camp-4"), "a placement out of reach is refused");
        check(CampNpcPlacements.parse("{\"version\":1,\"map\":\"camp-4\",\"npcs\":{\"gone\":{\"dx\":1,\"y\":65,\"dz\":1,\"yaw\":0}}}", "camp-4").containsKey("gone"), "an id no longer on the sheet is kept, so a save does not erase it");
        check(refused("{\"version\":1,\"map\":\"camp-4\",\"npcs\":{\"../x\":{\"dx\":1,\"y\":65,\"dz\":1,\"yaw\":0}}}", "camp-4"), "an id that is not an NPC id is refused");
        check(CampNpcPlacements.parse("{\"version\":1,\"map\":\"camp-4\"}", "camp-4").isEmpty(), "a file with no npcs is empty");

        var shipped = Map.of("tailor", b, "archivist", a);
        var world = Map.of("tailor", a);
        check(CampNpcPlacements.pick("tailor", world, shipped) == a, "the world save outranks the shipped file");
        check(CampNpcPlacements.pick("archivist", world, shipped) == a, "the shipped file outranks the sheet");
        check(CampNpcPlacements.pick("steward", world, shipped) == null, "with neither, the sheet row (null)");
        var moved = CampNpcPlacements.with(world, "tailor", b);
        check(moved.get("tailor") == b && world.get("tailor") == a, "placing again moves the NPC and leaves the old map untouched");
        check(CampNpcPlacements.with(moved, "tailor", null).isEmpty(), "a reset removes the placement");

        check(CampNpcPlacements.facing(0f) == 180f || CampNpcPlacements.facing(0f) == -180f, "a player looking south (0) gets an NPC looking north: " + CampNpcPlacements.facing(0f));
        check(CampNpcPlacements.facing(90f) == -90f, "player 90 -> -90: " + CampNpcPlacements.facing(90f));
        check(CampNpcPlacements.facing(-100f) == 90f, "player -100 snaps to 90: " + CampNpcPlacements.facing(-100f));
        check(CampNpcPlacements.facing(200f) == 0f, "player 200 -> 0: " + CampNpcPlacements.facing(200f));
        check(CampNpcPlacements.validMap("camp-4") && CampNpcPlacements.validMap("lobby") && !CampNpcPlacements.validMap("../x") && !CampNpcPlacements.validMap(""), "map ids are file-safe");
        check(CampNpcPlacements.fileName("camp-4").equals("gg-npcs-camp-4.json") && CampNpcPlacements.resource("lobby").equals("/data/gravesandglory/adoption/lobby.npcs.json"), "file names");

        System.out.println("CampNpcPlacementsCheck " + (failed == 0 ? "PASS" : "FAIL") + " " + (checks - failed) + "/" + checks);
        if (failed > 0) System.exit(1);
    }
}
