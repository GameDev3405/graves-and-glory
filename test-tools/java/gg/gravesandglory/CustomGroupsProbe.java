package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test-only reach into custom groups (command UI plan Task 12) for cmdui.CustomGroupsSuite: the server's profile and saved file, the stored
 * orders per troop, and the client's Armoury page, whose tags the suite presses as a player's clicks would (onPress, charTyped, keyPressed).
 * Never included in releases.
 */
public final class CustomGroupsProbe {
    private CustomGroupsProbe() {}

    // ---- server side (call on the server thread) ----

    /** Empties the commander's custom groups and saves, so the suite starts from none. */
    public static void clearGroups(GravesAndGlory.Battle b, UUID id) { b.campaign.profile(id).customGroups = new ArrayList<>(); b.campaign.save(id); }

    static String describe(List<Profile.CustomGroup> groups) {
        if (groups == null) return "null";
        List<String> out = new ArrayList<>();
        for (var g : groups) out.add(g.number() + ":" + g.name() + g.troopIds());
        return out.toString();
    }
    /** The server profile's groups, as "n:name[ids]". */
    public static String groups(GravesAndGlory.Battle b, UUID id) { return describe(b.campaign.profile(id).customGroups); }
    public static List<String> troops(GravesAndGlory.Battle b, UUID id, int n) { var g = b.campaign.profile(id).customGroup(n); return g == null ? List.of() : List.copyOf(g.troopIds()); }
    public static String name(GravesAndGlory.Battle b, UUID id, int n) { var g = b.campaign.profile(id).customGroup(n); return g == null ? null : g.name(); }
    /** The commander's saved profile file as it is on disk: its schema and its groups (read with the game's own Gson, no sanitize). */
    public static String saved(GravesAndGlory.Battle b, UUID id) {
        try {
            var p = Campaign.JSON.fromJson(Files.readString(b.campaign.directory.resolve(id + ".json")), Profile.class);
            return "schema " + p.schema + " " + describe(p.customGroups);
        } catch (Exception e) { return "unreadable: " + e; }
    }

    /** Each living troop of the commander's squad: entity id to "troopId/group". */
    public static Map<Integer, String> fielded(GravesAndGlory.Battle b, UUID id) {
        Map<Integer, String> out = new LinkedHashMap<>(); var s = b.squads.get(id);
        for (var u : b.units) if (u.squad == s && u.mob.isAlive()) out.put(u.mob.getId(), u.spec.id() + "/" + u.spec.group());
        return out;
    }
    /** Each living troop's order as Battle.assignment reads it: entity id to "order@issued". */
    public static Map<Integer, String> orders(GravesAndGlory.Battle b, UUID id) {
        Map<Integer, String> out = new HashMap<>(); var s = b.squads.get(id);
        for (var u : b.units) if (u.squad == s && u.mob.isAlive()) { var a = b.assignment(u); out.put(u.mob.getId(), a.order + "@" + a.issued); }
        return out;
    }
    /** The squad's stored assignments by key: "order@issued". */
    public static Map<String, String> keys(GravesAndGlory.Battle b, UUID id) {
        Map<String, String> out = new LinkedHashMap<>();
        for (var e : b.squads.get(id).assignments.entrySet()) out.put(e.getKey(), e.getValue().order + "@" + e.getValue().issued);
        return out;
    }
    public static long time(GravesAndGlory.Battle b) { return b.time; }
    public static void returnLobby(GravesAndGlory.Battle b) { if (b.running) b.campaign.returnLobby(); }

    /**
     * Tactics.become under a custom group's order (Task 12 item 3): one shared Charge under troop:A and troop:B and an older Hold under A's group key;
     * a troop of A turns to Attack. Every key that held the Charge must hold the new Attack, the group key must keep its Hold. Restores the squad's
     * assignments after. Returns "" when it holds, else what went wrong.
     */
    public static String becomeCheck(GravesAndGlory.Battle b, UUID id, String troopA, String troopB) {
        var s = b.squads.get(id); GravesAndGlory.Unit u = null;
        for (var m : b.units) if (m.squad == s && m.spec.id().equals(troopA)) { u = m; break; }
        if (u == null) return "no " + troopA + " fielded";
        var saved = new HashMap<>(s.assignments); var arrived = new java.util.HashSet<>(s.arrived);
        try {
            var hold = new GravesAndGlory.Assignment("hold", "line", u.mob.getPos(), 0); hold.issued = b.time - 5;
            var charge = new GravesAndGlory.Assignment("charge", "wedge", u.mob.getPos(), 0); charge.issued = b.time; charge.chargeDir = new Vec3d(1, 0, 0);
            s.assignments.put(u.spec.group(), hold);
            s.assignments.put(CustomGroups.TROOP + troopA, charge); s.assignments.put(CustomGroups.TROOP + troopB, charge);
            var n = Tactics.become(u, b.assignment(u), "attack", b.time + 1);
            var a = s.assignments.get(CustomGroups.TROOP + troopA); var c = s.assignments.get(CustomGroups.TROOP + troopB); var g = s.assignments.get(u.spec.group());
            List<String> bad = new ArrayList<>();
            if (a != n || c != n) bad.add("troop keys " + (a == null ? null : a.order) + "/" + (c == null ? null : c.order) + " not the new " + n.order);
            if (g != hold) bad.add("group key " + u.spec.group() + " became " + (g == null ? null : g.order));
            return String.join("; ", bad);
        } finally { s.assignments.clear(); s.assignments.putAll(saved); s.arrived.clear(); s.arrived.addAll(arrived); }
    }

    // ---- client side (call on the client thread) ----

    public static void openArmoury(MinecraftClient c) { c.setScreen(new RecruitmentScreen(1)); }
    static ArmouryTab tab(MinecraftClient c) { return c.currentScreen instanceof RecruitmentScreen r && r.tab == 1 ? r.page : null; }
    public static boolean armouryOpen(MinecraftClient c) { return tab(c) != null; }
    public static boolean groupsMode(MinecraftClient c) { var t = tab(c); return t != null && t.groupsMode; }
    static ArmouryTab.GroupTag find(MinecraftClient c, String kind, int n, String troop) {
        var t = tab(c); if (t == null) return null;
        if (ArmouryTab.GroupTag.MODE.equals(kind)) return t.modeTag;
        for (var g : t.groupTags) if (g.kind.equals(kind) && g.number == n && (troop == null || g.troop != null && g.troop.id().equals(troop))) return g;
        return null;
    }
    /** Presses a tag of the groups view as a click does; false when the page has no such tag. */
    public static boolean press(MinecraftClient c, String kind, int n, String troop) { var g = find(c, kind, n, troop); if (g == null) return false; g.onPress(); return true; }
    /** Types into the name tag of group n (it must be in typing): each character, then the keys (GLFW codes) in order. */
    public static boolean type(MinecraftClient c, int n, String text, int... keys) {
        var g = find(c, ArmouryTab.GroupTag.NAME, n, null); if (g == null || !g.typing()) return false;
        for (int key : keys) if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) g.keyPressed(key, 0, 0);
        for (char ch : text.toCharArray()) g.charTyped(ch, 0);
        for (int key : keys) if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) g.keyPressed(key, 0, 0);
        return true;
    }
    public static boolean typing(MinecraftClient c, int n) { var g = find(c, ArmouryTab.GroupTag.NAME, n, null); return g != null && g.typing(); }
    /** The screen's own key handling for the Armoury key (as a real press reaches it): whether the screen is still open after. */
    public static boolean armouryKeyKeepsOpen(MinecraftClient c) {
        var screen = c.currentScreen; if (screen == null) return false;
        var k = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.armoury);
        screen.keyPressed(k.getCode(), 0, 0);
        return c.currentScreen == screen;
    }
    /** The troop ids of the Armoury rows on the page now. */
    public static List<String> visibleTroops(MinecraftClient c) {
        var t = tab(c); List<String> out = new ArrayList<>(); if (t == null) return out;
        var rows = CampaignClient.armoury.rows();
        for (int r = t.first; r < Math.min(rows.size(), t.first + Math.max(t.shown, 0)); r++) out.add(rows.get(r).troop());
        return out;
    }
    public static List<String> fit(MinecraftClient c) { var t = tab(c); return t == null ? List.of("no Armoury page") : t.fitFailures(); }
    /** The client's copy of the groups (from the state snapshot). */
    public static String clientGroups() { return describe(CampaignClient.profile.customGroups); }
    /** The order bar's custom pills as "id=label". */
    public static List<String> customPills() {
        List<String> out = new ArrayList<>();
        for (var p : CommandSelection.pills()) if (p.custom()) out.add(p.id() + "=" + p.label());
        return out;
    }
}
