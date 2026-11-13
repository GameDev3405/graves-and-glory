package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.CustomGroupsProbe;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.OrderIntakeProbe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Command UI plan Task 12: custom numbered groups. The Armoury page's own tags make group 1 with two fielded troop types and rename it (pressed
 * and typed as a player would, through CustomGroupsProbe); the group must be on the server profile and its file, and still there after a
 * camp-battle-camp cycle. Group 2 (the first type alone) is made the same way. In the second battle, real tactical_v2 orders to custom:2 and to a troop group prove that the order reaches only the
 * group's troop types and that the newer of a group order and a troop-type order wins (Battle.assignment, by issued). Screenshot custom-groups.png
 * goes to work/cmdui-shots (never committed). Never included in releases.
 */
public final class CustomGroupsSuite implements CommandUiChecks.Suite {
    static final String NAME = "Vanguard", KNIGHT = "knight", ARCHER = "archer", SPLIT = "sentinel";
    volatile net.minecraft.util.math.Vec3d spot, startCentre, endCentre; volatile String splitResult, retreatResult; volatile Map<Integer, String> preRetreat = Map.of();
    UUID id;
    Path shots;
    int step, at;
    volatile Map<Integer, String> fielded = Map.of(), before = Map.of(), mid = Map.of(), after = Map.of(), last = Map.of();
    volatile Map<String, String> keysBefore = Map.of(), keysAfter = Map.of();
    volatile String serverGroups = "", savedFile = "", serverName = "", become = null, secondGroups = "";
    volatile List<String> serverTroops = List.of();
    volatile boolean asked;
    String troopA, troopB, groupG, shot = "not taken";
    final List<String> createNotes = new ArrayList<>();
    boolean createOk = true, keyOk, pressed2, pressedA, pressedB;

    @Override public int limit() { return 3600; }

    void next(int tick) { step++; at = tick; asked = false; }
    void fail(MinecraftClient c, String check, String why) { CommandUiChecks.report(check, false, why); CommandUiChecks.finish(c); }

    @Override public void tick(MinecraftClient c, int tick) {
        int r = tick - at;
        switch (step) {
            case 0 -> {
                id = c.player.getUuid();
                shots = Path.of(System.getProperty("gravesandglory.repoRoot", ".")).resolve("work").resolve("cmdui-shots");
                try { Files.createDirectories(shots); } catch (Exception ignored) { }
                CommandUiChecks.server(c, b -> { CustomGroupsProbe.clearGroups(b, id); CustomGroupsProbe.splitBanner(b, id); }); // knights and Sentinels in infantry (fix I1)
                CommandUiChecks.seatAndStart(c);
                next(tick);
            }
            case 1 -> { // the battle runs; open the Armoury and turn the sheet to the groups
                if (r < 40) return;
                if (!GravesAndGloryClient.battleActive()) { fail(c, "cu-custom-create", "no battle after seatAndStart"); return; }
                CommandUiChecks.server(c, b -> fielded = CustomGroupsProbe.fielded(b, id));
                CustomGroupsProbe.openArmoury(c); next(tick);
            }
            case 2 -> {
                if (r < 10) return;
                if (!CustomGroupsProbe.armouryOpen(c)) { fail(c, "cu-custom-create", "the Armoury page did not open"); return; }
                if (!CustomGroupsProbe.press(c, "mode", 0, null)) { fail(c, "cu-custom-create", "no Custom groups tag on the Armoury note"); return; }
                next(tick);
            }
            case 3 -> {
                if (r < 5) return;
                if (!CustomGroupsProbe.groupsMode(c)) { fail(c, "cu-custom-create", "the sheet did not turn to the groups"); return; }
                if (!CustomGroupsProbe.press(c, "new", 1, null)) { fail(c, "cu-custom-create", "no New group tag in column 1"); return; }
                next(tick);
            }
            case 4 -> { // group 1 reaches the client, then pick two fielded troop types on the page that leave other troops out
                if (!CustomGroupsProbe.clientGroups().contains("1:")) { if (r > 200) fail(c, "cu-custom-create", "group 1 never reached the client: " + CustomGroupsProbe.clientGroups()); return; }
                Set<String> types = new LinkedHashSet<>(); for (var v : fielded.values()) types.add(v.split("/")[0]);
                var page = CustomGroupsProbe.visibleTroops(c);
                if (!page.contains(KNIGHT) || !page.contains(ARCHER) || !types.contains(KNIGHT) || !types.contains(ARCHER) || !types.contains(SPLIT)) { fail(c, "cu-custom-create", "need knight and archer on the page and knight, archer and sentinel fielded: page " + page + ", fielded " + types); return; }
                troopA = KNIGHT; troopB = ARCHER;
                for (var v : fielded.values()) if (v.startsWith(troopA + "/")) groupG = v.split("/")[1];
                if (!pressedA) pressedA = CustomGroupsProbe.press(c, "member", 1, troopA);
                if (!pressedB) pressedB = CustomGroupsProbe.press(c, "member", 1, troopB);
                if (!pressedA || !pressedB) { if (r > 200) fail(c, "cu-custom-create", "no Add tag for " + (pressedA ? troopB : troopA) + " in group 1"); return; }
                next(tick);
            }
            case 5 -> { // both types on the server; then rename through the name tag (the Armoury key must type, not close)
                if (!asked) { asked = true; CommandUiChecks.server(c, b -> { serverTroops = CustomGroupsProbe.troops(b, id, 1); asked = false; }); }
                if (!(serverTroops.contains(troopA) && serverTroops.contains(troopB))) { if (r > 200) fail(c, "cu-custom-create", "server group 1 holds " + serverTroops + ", wanted " + troopA + " and " + troopB); return; }
                if (!CustomGroupsProbe.clientGroups().contains(troopB)) return;
                if (!CustomGroupsProbe.press(c, "name", 1, null) || !CustomGroupsProbe.typing(c, 1)) { fail(c, "cu-custom-create", "the name tag of group 1 did not take typing"); return; }
                keyOk = CustomGroupsProbe.armouryKeyKeepsOpen(c) && CustomGroupsProbe.typing(c, 1);
                CommandUiChecks.report("cu-custom-armoury-key", keyOk, "the Armoury key while group 1's name is typed " + (keyOk ? "keeps the page open and typing" : "closed the page or ended the typing"));
                if (!CustomGroupsProbe.armouryOpen(c)) { CustomGroupsProbe.openArmoury(c); CustomGroupsProbe.press(c, "mode", 0, null); CustomGroupsProbe.press(c, "name", 1, null); }
                int[] keys = new int[8]; for (int i = 0; i < 7; i++) keys[i] = GLFW.GLFW_KEY_BACKSPACE; keys[7] = GLFW.GLFW_KEY_ENTER;
                if (!CustomGroupsProbe.type(c, 1, NAME, keys)) { fail(c, "cu-custom-create", "could not type into group 1's name"); return; }
                next(tick);
            }
            case 6 -> { // the name on the server and the client; the page fits; the pill; the screenshot
                if (!asked) { asked = true; CommandUiChecks.server(c, b -> { serverName = CustomGroupsProbe.name(b, id, 1); asked = false; }); }
                if (!NAME.equals(serverName) || !CustomGroupsProbe.clientGroups().contains(NAME)) { if (r > 200) fail(c, "cu-custom-create", "group 1 is named " + serverName + " on the server, client " + CustomGroupsProbe.clientGroups()); return; }
                if (!CustomGroupsProbe.press(c, "new", 2, null)) { fail(c, "cu-custom-create", "no New group tag in column 2"); return; }
                step = 16; at = tick; asked = false;
            }
            case 16 -> { // group 2 holds troopA alone, so an order to custom:2 must leave troopB's troops alone
                if (!CustomGroupsProbe.clientGroups().contains("2:")) { if (r > 200) fail(c, "cu-custom-create", "group 2 never reached the client"); return; }
                if (!pressed2) { if (!CustomGroupsProbe.press(c, "member", 2, troopA)) { if (r > 200) fail(c, "cu-custom-create", "no Add tag for " + troopA + " in group 2"); return; } pressed2 = true; }
                if (!CustomGroupsProbe.clientGroups().contains("[" + troopA + "]")) { if (r > 200) fail(c, "cu-custom-create", "group 2 is " + CustomGroupsProbe.clientGroups()); return; }
                step = 17; at = tick; asked = false;
            }
            case 17 -> {
                if (r < 10) return; // a few frames of the finished page
                var fit = CustomGroupsProbe.fit(c);
                if (!fit.isEmpty()) { createOk = false; createNotes.add("page fit " + fit); }
                var pills = CustomGroupsProbe.customPills();
                if (!pills.contains("custom:1=1 " + NAME)) { createOk = false; createNotes.add("pills " + pills); }
                try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) { img.writeTo(shots.resolve("custom-groups.png")); shot = "custom-groups.png " + img.getWidth() + "x" + img.getHeight(); }
                catch (Exception e) { shot = "custom-groups.png FAILED " + e; }
                System.out.println("[cu] shot " + shot);
                CommandUiChecks.server(c, b -> { serverGroups = CustomGroupsProbe.groups(b, id); CustomGroupsProbe.returnLobby(b); });
                c.setScreen(null); step = 7; at = tick; asked = false;
            }
            case 7 -> { // camp, then a second battle
                if (r < 20 || GravesAndGloryClient.battleActive()) { if (r > 400) fail(c, "cu-custom-create", "the round did not end"); return; }
                CommandUiChecks.seatAndStart(c); next(tick);
            }
            case 8 -> {
                if (r < 40) return;
                if (!GravesAndGloryClient.battleActive()) { fail(c, "cu-custom-create", "no second battle"); return; }
                CommandUiChecks.server(c, b -> {
                    secondGroups = CustomGroupsProbe.groups(b, id); fielded = CustomGroupsProbe.fielded(b, id);
                    before = CustomGroupsProbe.orders(b, id); keysBefore = CustomGroupsProbe.keys(b, id); OrderIntakeProbe.resetCharge(b, id);
                });
                next(tick);
            }
            case 9 -> { // Attack on custom:1
                if (r < 5) return;
                if (!secondGroups.contains(NAME)) { createOk = false; createNotes.add("second battle groups " + secondGroups); }
                OrderIntakeProbe.sendMany("attack", "block", 0, List.of(new OrderIntakeProbe.At("custom:2", 0, 0)));
                next(tick);
            }
            case 10 -> {
                if (r < 10) return;
                CommandUiChecks.server(c, b -> {
                    after = CustomGroupsProbe.orders(b, id); keysAfter = CustomGroupsProbe.keys(b, id);
                    List<String> bad = new ArrayList<>(); int members = 0, others = 0;
                    for (var e : fielded.entrySet()) {
                        String troop = e.getValue().split("/")[0], now = after.get(e.getKey()), was = before.get(e.getKey());
                        if (now == null) continue;
                        if (troop.equals(troopA)) { members++; if (!now.startsWith("attack@")) bad.add(e.getKey() + " " + troop + " " + now); }
                        else { others++; if (!now.equals(was)) bad.add(e.getKey() + " " + troop + " " + was + " -> " + now); }
                    }
                    for (var k : keysBefore.entrySet()) if (!k.getKey().startsWith("troop:") && !k.getValue().equals(keysAfter.get(k.getKey()))) bad.add("key " + k.getKey() + " " + k.getValue() + " -> " + keysAfter.get(k.getKey()));
                    CommandUiChecks.report("cu-custom-order", bad.isEmpty() && members > 0 && others > 0,
                        "Attack on custom:2 (" + troopA + " only): " + members + " member troops, " + others + " others; keys " + keysAfter + (bad.isEmpty() ? "" : "; wrong " + bad));
                });
                next(tick);
            }
            case 11 -> { // a later group order beats the earlier troop-type order
                if (r < 10) return;
                OrderIntakeProbe.sendMany("follow", "block", 0, List.of(new OrderIntakeProbe.At(groupG, 0, 0)));
                next(tick);
            }
            case 12 -> {
                if (r < 10) return;
                CommandUiChecks.server(c, b -> mid = CustomGroupsProbe.orders(b, id));
                next(tick);
            }
            case 13 -> { // and a later custom order beats that group order
                if (r < 5) return;
                OrderIntakeProbe.sendMany("retreat", "block", 0, List.of(new OrderIntakeProbe.At("custom:2", 0, 0)));
                next(tick);
            }
            case 14 -> {
                if (r < 10) return;
                CommandUiChecks.server(c, b -> {
                    last = CustomGroupsProbe.orders(b, id);
                    List<String> bad = new ArrayList<>(); int a = 0, g = 0;
                    for (var e : fielded.entrySet()) {
                        String troop = e.getValue().split("/")[0], group = e.getValue().split("/")[1], m = mid.get(e.getKey()), l = last.get(e.getKey());
                        if (m == null || l == null) continue;
                        boolean member = troop.equals(troopA);
                        if (group.equals(groupG)) { g++; if (!m.startsWith("follow@")) bad.add("after follow " + troop + " " + m); }
                        else if (member && !m.startsWith("attack@")) bad.add("after follow " + troop + " (other group) " + m);
                        if (member) { a++; if (!l.startsWith("retreat@")) bad.add("after retreat " + troop + " " + l); }
                        else if (group.equals(groupG) && !l.startsWith("follow@")) bad.add("after retreat " + troop + " (not a member) " + l);
                    }
                    CommandUiChecks.report("cu-custom-overlap", bad.isEmpty() && a > 0 && g > 0,
                        "custom:2 Attack, then " + groupG + " Follow me (newer group order wins for " + troopA + "), then custom:2 Retreat (newer troop order wins): "
                            + g + " troops in " + groupG + ", " + a + " members" + (bad.isEmpty() ? "" : "; wrong " + bad));
                    become = CustomGroupsProbe.becomeCheck(b, id, troopA, troopB);
                    CommandUiChecks.report("cu-custom-become", become.isEmpty(), become.isEmpty() ? "a custom Charge turned to Attack on its troop keys only; " + groupG + "'s own order kept" : become);
                    spot = CustomGroupsProbe.spotNear(b, id, troopA); startCentre = CustomGroupsProbe.centreOf(b, id, troopA);
                });
                step = 18; at = tick; asked = false;
            }
            case 18 -> { // the split (fix I1): custom:2 (knights) holds at a spot ten blocks off while the Sentinels keep infantry's Follow me
                if (r < 5) return;
                if (spot == null) { if (r > 100) { CommandUiChecks.report("cu-custom-split", false, "no dry spot near the knights"); retreatResult = "skipped"; step = 20; at = tick; } return; }
                OrderIntakeProbe.sendMany("hold", "block", 0, List.of(new OrderIntakeProbe.At("custom:2", spot.x, spot.z)));
                step = 19; at = tick;
            }
            case 19 -> {
                if (r < 140) return; // seven seconds to walk there
                CommandUiChecks.server(c, b -> {
                    endCentre = CustomGroupsProbe.centreOf(b, id, troopA);
                    String bad = CustomGroupsProbe.split(b, id, troopA, SPLIT, spot);
                    double was = Math.sqrt(GravesAndGlory_dist(startCentre, spot)), now = Math.sqrt(GravesAndGlory_dist(endCentre, spot));
                    if (now > was - 2 && now > 4) bad = (bad.isEmpty() ? "" : bad + "; ") + troopA + " did not walk to the spot: " + Math.round(was) + " then " + Math.round(now) + " blocks";
                    splitResult = bad;
                    CommandUiChecks.report("cu-custom-split", bad.isEmpty(), "custom:2 (" + troopA + " only) Hold at a spot " + Math.round(was) + " blocks off; " + troopA + " now " + Math.round(now)
                        + " blocks from it, in their own slots and key; " + SPLIT + " (same infantry group) kept Follow me, its key and slots" + (bad.isEmpty() ? "" : "; wrong " + bad));
                    preRetreat = CustomGroupsProbe.orders(b, id);
                });
                step = 21; at = tick;
            }
            case 21 -> { // a custom Retreat moves only its members: the rest of the infantry group keeps its order, key and places
                if (r < 5) return;
                if (preRetreat.isEmpty()) { if (r > 100) { CommandUiChecks.report("cu-custom-retreat-split", false, "no order snapshot after the split"); CommandUiChecks.server(c, b -> CustomGroupsProbe.returnLobby(b)); retreatResult = "skipped"; step = 20; at = tick; } return; }
                OrderIntakeProbe.sendMany("retreat", "block", 0, List.of(new OrderIntakeProbe.At("custom:2", 0, 0)));
                step = 22; at = tick;
            }
            case 22 -> {
                if (r < 40) return;
                CommandUiChecks.server(c, b -> {
                    String bad = CustomGroupsProbe.retreatSplit(b, id, troopA, SPLIT, preRetreat);
                    retreatResult = bad;
                    CommandUiChecks.report("cu-custom-retreat-split", bad.isEmpty(), "custom:2 (" + troopA + " only) Retreat: " + troopA + " retreat on their own key; " + SPLIT
                        + " (same infantry group) kept the same order and issue time, the group key and its places" + (bad.isEmpty() ? "" : "; wrong " + bad));
                    CustomGroupsProbe.returnLobby(b);
                });
                step = 20; at = tick;
            }
            case 20 -> { if (r < 5) return; if ((splitResult == null || retreatResult == null) && r < 200) return; step = 15; at = tick; asked = false; }
            case 15 -> { // back in camp: the group is on the profile and in its file
                if (r < 20 || GravesAndGloryClient.battleActive()) { if (r > 400) fail(c, "cu-custom-create", "the second round did not end"); return; }
                if (!asked) { asked = true; CommandUiChecks.server(c, b -> { serverGroups = CustomGroupsProbe.groups(b, id); savedFile = CustomGroupsProbe.saved(b, id); serverName = "done"; }); return; }
                if (!"done".equals(serverName)) return;
                String want = "1:" + NAME + "[" + troopA + ", " + troopB + "]", want2 = "[" + troopA + "]";
                boolean kept = serverGroups.contains(want) && serverGroups.contains(want2) && savedFile.startsWith("schema 5 ") && savedFile.contains(want) && savedFile.contains(want2);
                if (!kept) { createOk = false; createNotes.add("after camp-battle-camp profile " + serverGroups + ", file " + savedFile); }
                CommandUiChecks.report("cu-custom-create", createOk, "Armoury made " + want + "; after camp-battle-camp profile " + serverGroups + ", file " + savedFile + "; shot " + shot + (createNotes.isEmpty() ? "" : "; " + createNotes));
                CommandUiChecks.finish(c);
                step = 99;
            }
            default -> { }
        }
    }

    static double GravesAndGlory_dist(net.minecraft.util.math.Vec3d a, net.minecraft.util.math.Vec3d b) { if (a == null || b == null) return 1e6; double dx = a.x - b.x, dz = a.z - b.z; return dx * dx + dz * dz; }

    @Override public void cleanup(MinecraftClient c) { if (c.currentScreen != null) c.setScreen(null); }
}
