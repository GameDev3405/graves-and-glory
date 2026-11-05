package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom numbered groups (command UI plan Task 12). A commander keeps up to Profile.MAX_CUSTOM_GROUPS of them with his Armoury loadout
 * (Profile.customGroups, schema 5), each a short name and a set of troop types. An order to custom:&lt;n&gt; (the group field of tactical_v2)
 * reaches only the issuer's own troops of those types: it is stored per troop type under troop:&lt;id&gt; (one shared Assignment for the whole
 * custom group, so its troops form one formation), and Battle.assignment reads it before the troop's group key, the newer issued winning.
 * Bots have no custom groups. Armoury edits arrive as group: commands (command).
 */
final class CustomGroups {
    private CustomGroups() {}
    /** The group id prefix a custom group order names, and the assignment key prefix of a troop type's own order. */
    static final String PREFIX = "custom:", TROOP = "troop:";

    /** The group number of a custom:&lt;n&gt; id, or -1 when the id is not one. */
    static int number(String group) {
        if (group == null || !group.startsWith(PREFIX)) return -1;
        try { return Integer.parseInt(group.substring(PREFIX.length())); } catch (NumberFormatException e) { return -1; }
    }

    /** The commander's custom group a custom:&lt;n&gt; id names, or null (not an id, a bot, or no such group in his profile). */
    static Profile.CustomGroup group(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String id) {
        int n = number(id);
        if (n < 1 || b == null || s == null || s.bot || s.owner == null) return null;
        return b.campaign.profile(s.owner).customGroup(n);
    }

    /** The units a group id names in this squad (Task 12 interface): a custom group's troop types, else the troops of that group (or every troop for "all"). */
    static List<GravesAndGlory.Unit> resolve(GravesAndGlory.Squad s, String group) { return resolve(GravesAndGlory.battle, s, group); }
    static List<GravesAndGlory.Unit> resolve(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        List<GravesAndGlory.Unit> out = new ArrayList<>();
        if (b == null || s == null || group == null) return out;
        Profile.CustomGroup custom = number(group) > 0 ? group(b, s, group) : null;
        if (number(group) > 0 && custom == null) return out;
        for (var u : b.units) {
            if (u.squad != s) continue;
            if (custom != null ? custom.troopIds().contains(u.spec.id()) : group.equals("all") || u.spec.group().equals(group)) out.add(u);
        }
        return out;
    }

    /** Whether the custom group with this id is this unit's (its troop type is in it). */
    static boolean contains(GravesAndGlory.Battle b, GravesAndGlory.Unit u, String group) {
        Profile.CustomGroup custom = group(b, u.squad, group);
        return custom != null && custom.troopIds().contains(u.spec.id());
    }

    /**
     * Stores one custom group order: one fresh Assignment, shared by every troop type in the group under troop:&lt;id&gt; with issued now (so it
     * beats any older group order, and a later group order beats it), and the group's troops drop their target, spell centre and path as
     * Battle.issue does. Returns the stored assignment, or null when the group is not the commander's.
     */
    static GravesAndGlory.Assignment issue(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group, String order, String formation, Vec3d dest, float yaw, int enemyId) {
        Profile.CustomGroup custom = group(b, s, group);
        if (custom == null) return null;
        var a = new GravesAndGlory.Assignment(order, formation, dest, yaw);
        a.enemy = enemyId;
        a.issued = b.time;
        for (String id : custom.troopIds()) s.assignments.put(TROOP + id, a);
        for (var u : resolve(b, s, group)) {
            s.arrived.remove(u.spec.group());
            Abilities.keepSpeed(u.mob, Tactics.CRAWL_ID, "Graves and Glory crawl", 0);
            u.lockedTarget = null; u.spellCenter = null; u.mob.setTarget(null); u.mob.getNavigation().stop(); u.nextPath = 0; u.stuckTicks = 0;
        }
        return a;
    }

    /** Whether this troop is under its troop type's own order (a custom group order newer than its group's), which Battle.assignment returns. */
    static boolean ordered(GravesAndGlory.Unit u) {
        var t = u.squad.assignments.get(TROOP + u.spec.id());
        var g = u.squad.assignments.get(u.spec.group());
        return t != null && (g == null || t.issued >= g.issued);
    }

    /**
     * Armoury edits (the group: commands), on the commander's own profile, saved at once (the Armoury opens in battle, so an edit is taken there too).
     * group:new adds the lowest free number (at most Profile.MAX_CUSTOM_GROUPS) named "Group n"; group:name:&lt;n&gt;:&lt;text&gt; renames
     * (Profile.groupName clamps it); group:add:&lt;n&gt;:&lt;troop&gt; adds an owned troop type; group:remove:&lt;n&gt;:&lt;troop&gt; takes one out;
     * group:delete:&lt;n&gt; deletes the group. Returns true for every group: command, handled or refused.
     */
    static boolean command(Campaign c, GravesAndGlory.Battle b, ServerPlayerEntity p, String cmd) {
        if (!cmd.startsWith("group:")) return false;
        Profile pr = c.profile(p.getUuid());
        List<Profile.CustomGroup> before = pr.customGroups, next = edit(pr, cmd.substring(6));
        if (next == null) return true;
        if (next.size() > Profile.MAX_CUSTOM_GROUPS) { c.notice(p.getUuid(), "You can keep " + Profile.MAX_CUSTOM_GROUPS + " custom groups."); return true; }
        pr.customGroups = next;
        if (!c.save(p.getUuid())) pr.customGroups = before;
        return true;
    }

    /** The edited group list for one group: command body (verb:number[:arg]), or null when it changes nothing or is malformed. Pure. */
    static List<Profile.CustomGroup> edit(Profile pr, String body) {
        String[] parts = body.split(":", 3);
        String verb = parts[0];
        List<Profile.CustomGroup> out = new ArrayList<>(pr.customGroups == null ? List.of() : pr.customGroups);
        if (verb.equals("new")) {
            for (int n = 1; n <= Profile.MAX_CUSTOM_GROUPS; n++) if (pr.customGroup(n) == null) { out.add(new Profile.CustomGroup(n, Profile.groupName(null, n), List.of())); out.sort(java.util.Comparator.comparingInt(Profile.CustomGroup::number)); return out; }
            out.add(null); // full: one over the cap, which command refuses with a notice
            return out;
        }
        if (parts.length < 2) return null;
        int n;
        try { n = Integer.parseInt(parts[1]); } catch (NumberFormatException e) { return null; }
        Profile.CustomGroup g = pr.customGroup(n);
        if (g == null) return null;
        int at = out.indexOf(g);
        String arg = parts.length > 2 ? parts[2] : "";
        switch (verb) {
            case "delete" -> out.remove(at);
            case "name" -> out.set(at, new Profile.CustomGroup(n, Profile.groupName(arg, n), g.troopIds()));
            case "add" -> {
                if (g.troopIds().contains(arg) || pr.owned == null || !pr.owned.containsKey(arg)) return null;
                List<String> ids = new ArrayList<>(g.troopIds()); ids.add(arg);
                out.set(at, new Profile.CustomGroup(n, g.name(), ids));
            }
            case "remove" -> {
                if (!g.troopIds().contains(arg)) return null;
                List<String> ids = new ArrayList<>(g.troopIds()); ids.remove(arg);
                out.set(at, new Profile.CustomGroup(n, g.name(), ids));
            }
            default -> { return null; }
        }
        return out;
    }

    /** Each custom group named, with its living troops, for an OUTLINE. */
    static Map.Entry<String, Integer> members(GravesAndGlory.Battle b, GravesAndGlory.Squad s, String group) {
        return Map.entry(group, (int) resolve(b, s, group).stream().filter(u -> u.mob.isAlive()).count());
    }
}
