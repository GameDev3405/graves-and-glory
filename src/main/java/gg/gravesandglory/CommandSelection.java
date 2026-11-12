package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The player's command selection and panel state (command UI plan Task 4): which groups the next order goes to, whether the panel
 * and the formation tray are open, and the single entry every key and click uses to give an order. Client only; the server owns the
 * orders themselves (OrderRules). The selection survives closing and reopening the panel; a disconnect or a new round resets it
 * to the whole army.
 *
 * Keys: Attack, Follow me and Retreat go out at once; Charge and Formation start an aim that Enter or a click confirms and Backspace cancels
 * (CommandMode keeps the aim's raycast and preview). After an order the panel closes unless Shift is held.
 */
public final class CommandSelection {
    private CommandSelection() {}

    /** One group pill: its id ("all", a group id, later "custom:n"), its label, its living members and whether the player built it. */
    public record Pill(String id, String label, int members, boolean custom) {}

    /** The tray's four shapes in the order of the number keys 1 to 4: Line, Shield wall (block), Wedge, Loose. */
    public static final String[] SHAPES = {"line", "block", "wedge", "loose"};

    private static final List<String> selected = new ArrayList<>(List.of("all"));
    private static boolean panelOpen, trayOpen;
    /** Set when the order that began the current aim was given with Shift held, so confirming it leaves the panel open. */
    private static boolean aimKeepOpen;

    /** The selected group ids in the order they were picked; ["all"] is the whole army. A copy. */
    public static List<String> selected() { return List.copyOf(selected); }
    public static boolean isSelected(String id) { return selected.contains("all") || selected.contains(id); }

    /** Picks a group: add=false replaces the selection, add=true toggles the group in (from the whole army it starts a new selection);
     *  "all" selects the whole army alone, and an emptied selection falls back to it. */
    public static void select(String groupId, boolean add) {
        if (groupId == null || groupId.isEmpty()) return;
        if (groupId.equals("all") || !add) { selected.clear(); selected.add(groupId); }
        else {
            selected.remove("all");
            if (!selected.remove(groupId)) selected.add(groupId);
            if (selected.isEmpty()) selected.add("all");
        }
        mirror();
    }

    /** CommandMode.selected is the battle HUD's single highlighted group (BattleHud, until its panel is rebuilt): "all", or the one group selected. */
    private static void mirror() { CommandMode.selected = selected.size() == 1 ? selected.get(0) : "all"; }

    /** The pills in panel order: Infantry (key 1), Archers (2), Riders (3), All (4), then Support and Siege when fielded (no key), then the
     *  commander's custom groups (Task 12, Profile.customGroups) labelled "&lt;n&gt; &lt;name&gt;". Counts come from the state packet's groups, read by id;
     *  a custom group counts its troop types' slots in the banner (the packet has no per-type counts). */
    public static List<Pill> pills() {
        List<Pill> out = new ArrayList<>();
        for (String id : new String[]{"infantry", "ranged", "cavalry"}) out.add(new Pill(id, label(id), members(id), false));
        out.add(new Pill("all", "All", members("all"), false));
        for (String id : new String[]{"support", "siege"}) if (fielded(id)) out.add(new Pill(id, label(id), members(id), false));
        var profile = CampaignClient.profile;
        if (profile != null && profile.customGroups != null) for (var g : profile.customGroups)
            if (g != null) out.add(new Pill(CustomGroups.PREFIX + g.number(), g.number() + " " + g.name(), bannerCount(profile, g.troopIds()), true));
        return out;
    }
    /** The banner slots holding one of these troop types: a custom group's strength as fielded. */
    private static int bannerCount(Profile profile, List<String> troopIds) {
        int n = 0;
        if (profile.banner != null && troopIds != null) for (String id : profile.banner) if (troopIds.contains(id)) n++;
        return n;
    }
    private static String label(String id) {
        for (var g : Spec.GROUPS) if (g.id().equals(id)) return g.label();
        return id;
    }
    private static int members(String id) {
        int n = 0;
        for (var g : CommandMode.groups) if (id.equals("all") || g.id().equals(id)) n += g.count();
        return n;
    }
    private static boolean fielded(String id) {
        for (var g : CommandMode.groups) if (g.id().equals(id)) return g.count() > 0 || g.full() > 0;
        return false;
    }

    public static boolean panelOpen() { return panelOpen; }
    /** Opens or shuts the panel; shutting it folds the formation tray as well. */
    public static void setPanelOpen(boolean open) { panelOpen = open && !(PanelLayout.hidden && !CommandPanel.editing()); if (!panelOpen) trayOpen = false; } // a hidden panel never opens
    public static boolean trayOpen() { return trayOpen; }
    public static void setTrayOpen(boolean open) { trayOpen = open; }

    /** The order being aimed: "charge", "hold" (the Formation order) or "" for none. */
    public static String aiming() { return CommandMode.pending.equals("charge") || CommandMode.pending.equals("hold") ? CommandMode.pending : ""; }

    /**
     * Gives an order to the selected groups. attack, follow and retreat go out at once (the server reads the player's position for the
     * last two and the group centres for the first); charge and hold begin an aim that confirm() sends; formation opens (or folds) the tray,
     * opening the panel with it. Afterwards the panel closes unless keepOpen.
     */
    public static void order(String action, boolean keepOpen) {
        var c = MinecraftClient.getInstance();
        if (c.player == null || !GravesAndGloryClient.battleActive()) return;
        aimKeepOpen = false; // a new order supersedes the last one's aim flag
        switch (action) {
            case "attack", "follow", "retreat" -> {
                CommandMode.cancel();
                issueSelected(action, CommandMode.formation, c.player.getPos(), c.player.getYaw(), -1);
                afterOrder(keepOpen);
            }
            case "charge" -> {
                if (CommandNet.Client.chargeTicksLeft() > 0) { CommandMode.cancel(); CommandMode.refuse("Charge is not ready yet"); return; } // no seconds: they would freeze in the feed while the Charge pennant counts down
                trayOpen = false;
                CommandMode.begin("charge");
                aimKeepOpen = keepOpen;
            }
            case "hold" -> { trayOpen = false; CommandMode.begin("hold"); aimKeepOpen = keepOpen; }
            case "formation" -> {
                if (PanelLayout.hidden && !CommandPanel.editing()) { order("hold", keepOpen); return; } // keyboard-only mode: no tray to open, B aims the Formation order in the current shape
                CommandMode.cancel();
                if (!panelOpen) { panelOpen = true; trayOpen = true; } else trayOpen = !trayOpen;
            }
            default -> { }
        }
    }

    /** The tray's choice: the shape becomes the current one and the Formation order starts aiming with it. */
    public static void chooseShape(String shape, boolean keepOpen) {
        for (String s : SHAPES) if (s.equals(shape)) { CommandMode.formation = shape; order("hold", keepOpen); return; }
    }

    /** The mouse wheel while the Formation order is aimed: the shape steps by dir (+1 the next in SHAPES, -1 the one before, wrapping)
     *  and the aim's preview follows it. Nothing is sent: the shape goes out with the order when it is confirmed. False when no
     *  Formation order is aimed, so the wheel is left to the hotbar. */
    public static boolean cycleShape(int dir) {
        if (!aiming().equals("hold") || dir == 0) return false;
        int at = 0;
        for (int i = 0; i < SHAPES.length; i++) if (SHAPES[i].equals(CommandMode.formation)) at = i;
        CommandMode.formation = SHAPES[Math.floorMod(at + Integer.signum(dir), SHAPES.length)];
        return true;
    }

    /** A click on a group card's formation (the battle HUD, the panel open): that group is selected (Shift adds it) and the Formation
     *  tray opens for it, as the Formation pennant opens it; a tray already open stays open. */
    public static void openTrayFor(String groupId, boolean add) {
        select(groupId, add);
        if (!trayOpen) order("formation", true);
    }

    /** A number key with the panel open: 1 to 4 pick that pill, or that shape while the tray is open. */
    static void pick(int number, boolean add, boolean keepOpen) {
        if (trayOpen) { if (number >= 1 && number <= SHAPES.length) chooseShape(SHAPES[number - 1], keepOpen); return; }
        var pills = pills();
        if (number >= 1 && number <= 4 && number <= pills.size()) select(pills.get(number - 1).id(), add);
    }

    /** What an order that has been sent does to the panel: its tray folds, and it slides shut unless Shift was held. */
    static void afterOrder(boolean keepOpen) {
        trayOpen = false; aimKeepOpen = false;
        if (!keepOpen) panelOpen = false;
    }

    /** CommandMode.cancel and begin: an aim that ends or starts anew drops its Shift flag. */
    static void clearAimKeep() { aimKeepOpen = false; }

    /** Whether the order that began the aim keeps the panel open when confirmed (Shift at either moment). */
    static boolean keepOpenOnConfirm() { return aimKeepOpen || shiftHeld(MinecraftClient.getInstance()); }

    /** Sends the order to every selected group as ONE tactical_v2 packet ("all" is the whole army), which the server applies as one order (one
     *  cooldown, one Charge for the commander). A Charge's start is each group's own centre; Attack's target the server takes from each group's
     *  centre, Follow me and Retreat from the player, so `at` matters for hold only. */
    static void issueSelected(String action, String formation, Vec3d at, float yaw, int target) {
        List<CommandMode.Aim> groups = new ArrayList<>();
        if (selected.contains("all")) groups.add(new CommandMode.Aim("all", at));
        else for (String id : selected) groups.add(new CommandMode.Aim(id, action.equals("charge") ? groupCentre(id, at) : at));
        CommandMode.issue(groups, action, formation, yaw, target);
    }

    /** One group's living centre from the last state packet, or the fallback when the group has no living members there. */
    private static Vec3d groupCentre(String id, Vec3d fallback) {
        for (var g : CommandMode.groups) if (g.id().equals(id) && g.count() > 0) return new Vec3d(g.cx(), fallback.y, g.cz());
        return fallback;
    }

    /** The living centre of the selected groups from the last state packet (member-weighted), or the player's position when none is known. */
    static Vec3d selectedCentre() {
        double x = 0, z = 0; int n = 0;
        for (var g : CommandMode.groups) if (isSelected(g.id()) && g.count() > 0) { x += g.cx() * g.count(); z += g.cz() * g.count(); n += g.count(); }
        var p = MinecraftClient.getInstance().player;
        if (n == 0) return p == null ? Vec3d.ZERO : p.getPos();
        return new Vec3d(x / n, p == null ? 65 : p.getY(), z / n);
    }

    /** Shift held: either physical Shift key, or the sneak binding while it is on a Shift key (so a harness can hold it as a binding). */
    public static boolean shiftHeld(MinecraftClient c) {
        if (c == null || c.getWindow() == null) return false;
        long w = c.getWindow().getHandle();
        if (InputUtil.isKeyPressed(w, GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(w, GLFW.GLFW_KEY_RIGHT_SHIFT)) return true;
        String bound = c.options.sneakKey.getBoundKeyTranslationKey();
        return c.options.sneakKey.isPressed() && (bound.equals("key.keyboard.left.shift") || bound.equals("key.keyboard.right.shift"));
    }

    /** Every client tick in battle: the panel cannot stay open once the battle is over, and a custom group deleted in the Armoury
     *  (C04-F3) leaves the selection, which falls back to the whole army when nothing else is left. */
    static void tick() {
        if (CampaignClient.received && selected.removeIf(id -> id.startsWith(CustomGroups.PREFIX) && pills().stream().noneMatch(p -> p.id().equals(id)))) {
            if (selected.isEmpty()) selected.add("all");
            mirror();
        }
        if (panelOpen && !GravesAndGloryClient.battleActive()) setPanelOpen(false);
    }

    /** A disconnect or a new round: panel and tray shut, no aim, the whole army selected, so a stale selection or a stale charge aim never carries over. */
    public static void reset() {
        CommandMode.cancel();
        selected.clear(); selected.add("all");
        panelOpen = false; trayOpen = false; aimKeepOpen = false;
        mirror();
    }
}
