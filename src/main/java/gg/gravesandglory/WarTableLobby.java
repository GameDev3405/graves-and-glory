package gg.gravesandglory;

import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table lobby (plan 3.4 and 4C.3; Kort, 2026-10-30): one page where a room of up to eight commanders, four a side, takes
 *  its seats (CampScreen page lobby). On the left the council sheet: the two sides' four seats each (a commander's House seal,
 *  name, role and ready mark; a seat bots will fill says BOT; a seat beyond the room's size is closed), then the viewer's own
 *  House and role (the set he wears most of; gear is changed in the Arsenal), the Ready plate and the countdown line. On the right the orders note: the mode, the map, the seats a side and
 *  the bot fill, which only the host changes and everyone sees. Every commander ready starts the server's ten-second countdown
 *  (SocialHub.countdown); anyone standing down stops it. The state is the server's (Campaign.socialSnapshot): the page only sends
 *  social: commands. Everything is drawn in canvas pixels; page coordinates are offset by WarTable.column(COMPACT). */
final class WarTableLobby {
    /** The sheet and the note on the 960 page: the pair stands centred, 24 apart, both papers 124 from the top. */
    static final int SHEET_X = 150, NOTE_X = 578, TOP = 124, SHEET_W = 404, NOTE_W = 232, PAPER_H = 373;
    /** The compact page (WarTable.compact): the pair moves left 53 and up 32, as the troop sheet's does. */
    static final int[] COMPACT = {-53, -32};
    /** On the sheet (sheet-relative): text edge and width, the seat columns, the seats' rows, the picks and the Ready plate. */
    static final int PAD = 22, TEXT_W = 360, COL_W = 174, COL_GAP = 12, SEATS = 4, TEAM_Y = 40, SEAT_Y = 56, SEAT_H = 24, SEAT_STEP = 27,
            HOST_Y = 166, HOUSE_HEAD_Y = 184, HOUSE_Y = 202, CHIP_PAD = 6, CHIP_SEAL = 16, CHIP_SEAL_GAP = 4, CHIP_GAP = 4, ROLE_HEAD_Y = 236, ROLE_Y = 254,
            READY_Y = 288, STATUS_Y = 334, NOTICE_Y = 346, NOTICE_STEP = 10, NOTICE_LINES = 2, PAPER_INSET = 6, STEP = 12;
    /** The room's code line on the sheet: the Copy and Invite tags beside the heading, and (not in a room) the Join by code field and its tag. SUMMARY_Y is the chosen mode's one line of rules, in the gap above the Ready plate. START_W is Start now beside Ready. */
    static final int HEAD_TAG_H = 20, COPY_W = 48, INVITE_W = 56, SUMMARY_Y = 270, START_W = 112, CODE_LENGTH = WarTablePages.CODE_LENGTH, JOIN_Y = 150, FIT_TICKS = 60;
    /** The invite pop-over: its panel (sheet-relative), the rows of friends it lists and each row's height. */
    static final int POP_X = 40, POP_Y = 44, POP_W = 324, POP_ROWS = 5, POP_ROW_H = 28, POP_HEAD = 40, POP_FOOT = 40;
    /** On the note (note-relative): its text edge and width, and the rows of the orders. */
    static final int NPAD = 18, NOTE_TEXT_W = 196, MODE_Y = 44, MAP_Y = 116, CARD_Y = 132, SIZE_Y = 252, FILL_Y = 284, TIER_Y = 312, LEAVE_Y = 340,
            MODE_COLS = 2, MODE_H = 24, MODE_GAP = 3, TIER_GAP = 3;
    // The mode tabs wrap two to a row (four modes since Survival did not fit one row of the note's 196 px); the rows below the tabs sit 24 px lower than the single row had them.
    /** The host's two tags on the last row of the note, Pass host and Leave, each this wide. */
    static final int HOST_TAG_W = 96;

    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> colorsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> texturesDrawn = new LinkedHashMap<>();
    private static final List<String> sentCommands = new ArrayList<>();
    /** Boxes {x,y,w,h} in canvas px the last frame drew: table, rail, sheet, note, seat-(team)-(i), house-(id), ready,
     *  status, mode-(id), map-card, map-prev, map-next, size-down, size-up, fill, leave, notice. */
    static Map<String, int[]> rects() { return Collections.unmodifiableMap(rectsDrawn); }
    /** Words drawn: room, host, seat-(team)-(i) (a commander's name, "BOT", "Open" or "Closed"), seat-(team)-(i)-state ("ready",
     *  "waiting", "bot", "open", "closed"), seat-(team)-(i)-house, seat-(team)-(i)-role, house (the viewer's pick), role, gear, ready
     *  (the plate's label), status, countdown, mode, map, size, fill, orders ("host" or the host's name), notice. */
    static Map<String, String> texts() { return Collections.unmodifiableMap(textsDrawn); }
    static Map<String, Integer> colors() { return Collections.unmodifiableMap(colorsDrawn); }
    static Map<String, String> textures() { return Collections.unmodifiableMap(texturesDrawn); }
    /** The commands the page handed the server (after CampScreen.action's throttle), the last WarTablePages.SENT_KEPT. */
    static List<String> sent() { return Collections.unmodifiableList(sentCommands); }

    /** One seat as the sheet shows it. kind: human, bot, open or closed. */
    record Seat(String kind, String name, String house, String role, boolean ready, boolean online, boolean host, boolean me) {}

    final CampScreen screen;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    long ticks;
    boolean asked;
    /** The tick a room request (social:lobby) went out and none has come back since; -1 when none is pending. After REQUEST_TICKS the page says it could not open one. */
    long requestAt = -1;
    boolean wasInRoom;
    static final int REQUEST_TICKS = 60;
    /** The host pressed Pass host: the other online commanders' seats now hand him the host's seat (social:host:<name>). */
    boolean choosingHost;
    /** Whether each House chip carries its seal this build (UiMath.chipRow), by House id. */
    final Map<String, Boolean> chipSeals = new HashMap<>();
    int noticeSerialSeen;
    String noticeText = "";
    long noticeAt = -1000;
    /** The room code being written (Join by code, not in a room) and whether the line takes the keys. */
    String code = "";
    boolean typing;
    /** The friends pop-over (Invite) is open, and which of its rows lead. */
    boolean inviting;
    int inviteOffset;
    /** Set by the Friends page's Join: the room it asked for is on its way, so this page does not open a room of its own. */
    static boolean joining;
    /** The control the server's last forced change moved ("map" or "mode"), the serial seen, and when (ticks): "changed to fit" shows FIT_TICKS beside it. */
    int fitSerialSeen;
    String fitWhat = "";
    long fitAt = -1000;
    /** Whether the last build laid Start now out: the page is rebuilt when that changes, since the button moves Ready aside. */
    boolean startBuilt;
    /** Whether the last build laid the map-wait view out: the page is rebuilt when that changes. */
    boolean waitBuilt;

    WarTableLobby(CampScreen screen) {
        this.screen = screen;
        noticeSerialSeen = CampaignClient.noticeSerial;
        fitSerialSeen = num("fitSerial", 0);
    }

    static int[] columnAt() { return WarTable.column(COMPACT[0], COMPACT[1]); }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    // ---- the server's state, as the snapshot carries it ----
    static JsonObject social() { return CampaignClient.social; }
    static String str(String k, String fallback) { return social().has(k) ? social().get(k).getAsString() : fallback; }
    static int num(String k, int fallback) { return social().has(k) ? social().get(k).getAsInt() : fallback; }
    static boolean flag(String k) { return social().has(k) && social().get(k).getAsBoolean(); }
    static boolean flag(JsonObject r, String k) { return r.has(k) && r.get(k).getAsBoolean(); }
    static String str(JsonObject r, String k) { return r.has(k) ? r.get(k).getAsString() : ""; }
    static boolean inRoom() { return !str("room", "").isEmpty(); }
    static boolean host() { return flag("host"); }
    static boolean idle() { return inRoom() && !flag("playing"); }
    static int size() { return Math.max(1, Math.min(SEATS, num("size", SocialHub.LOBBY_SIZE))); }
    static List<JsonObject> commanders() {
        List<JsonObject> out = new ArrayList<>();
        if (social().has("commanders")) for (var e : social().getAsJsonArray("commanders")) out.add(e.getAsJsonObject());
        return out;
    }
    static JsonObject me() { for (var r : commanders()) if (flag(r, "me")) return r; return null; }
    static int myTeam() { var m = me(); return m == null || !m.has("team") ? -1 : m.get("team").getAsInt(); }
    /** Whether the plate reads Stand down: this commander is ready, or the test preview of the map wait shows the plate as a ready player sees it (the room is only built once everyone is ready). */
    static boolean standing() { return myReady() || previewWait >= 0; }
    static boolean myReady() { var m = me(); return m != null && flag(m, "ready"); }
    static String myHouse() { var m = me(); String h = m == null ? "" : str(m, "house"); return h.isEmpty() ? RosterMath.rollHouse(CampaignClient.profile.banner) : h; }
    /** The role the viewer fights in: the set he wears most pieces of (GearStats), as the server tells the room. */
    static String myRole() { var m = me(); String r = m == null ? "" : str(m, "role"); return r.isEmpty() ? GearStats.of(CampaignClient.profile).role() : r; }
    /** Units my banner fields under my House pick (the server's count; -1 before it says). */
    static int myTroops() { var m = me(); return m == null || !m.has("troops") ? -1 : m.get("troops").getAsInt(); }
    /** Why I cannot ready ("" when I can): my banner holds no troop of the House I picked. The server refuses the same. */
    static String houseRefusal() {
        if (myTroops() != 0 || myReady()) return "";
        return "Your banner holds no " + RosterMath.houseName(myHouse()) + " troops. Pick another House.";
    }
    /** A House chip's word: the House's first word (Gilded, Graveborn, Wildmoot, Ironclad, Tidecallers), always whole. */
    static String chipName(String id) { String n = RosterMath.houseName(id); int sp = n.indexOf(' '); return sp < 0 ? n : n.substring(0, sp); }
    static String hostName() { for (var r : commanders()) if (flag(r, "host")) return str(r, "name"); return ""; }
    static List<String> modes() {
        List<String> out = new ArrayList<>();
        if (social().has("modes")) for (var e : social().getAsJsonArray("modes")) out.add(e.getAsString());
        if (out.isEmpty()) out.addAll(SocialHub.MODES);
        return out;
    }
    static String modeName(String id) { return id.isEmpty() ? "" : Character.toUpperCase(id.charAt(0)) + id.substring(1); }
    /** The mode's one line of rules and usual length (the sheet's modes row, summary); empty when the row is missing. */
    static String summary(String id) { for (var m : Spec.MODES) if (m.id().equals(id)) return m.summary(); return ""; }
    /** The room's bot difficulty (a Spec.BOTCOMMAND id); the server's setting before the room says. */
    static String tier() { return str("difficulty", CampaignClient.difficulty); }
    /** Start now: the host's, when the room is idle and nothing but the countdown stands between it and the battle (every human ready, the seats right). */
    static boolean startNow() { return host() && idle() && !flag("preparing") && str("waiting", "x").isEmpty() && !commanders().isEmpty(); }
    /** Test hook (ui-lobby): 0 to 100 draws the map-wait view with that percent as if the server were preparing this room's map; -1 leaves it to the server. */
    static int previewWait = -1;
    /** Whether the room is waiting for its map: the server is preparing it (the snapshot's preparing flag), the percent of the build is in GravesAndGloryClient.message. */
    static boolean waiting() { return inRoom() && !flag("playing") && (flag("preparing") || previewWait >= 0); }
    /** How far the map build is, 0 to 100: the number the server's "Preparing <map> / N%" message ends with. */
    static int waitPercent() {
        if (previewWait >= 0) return Math.min(100, previewWait);
        var m = java.util.regex.Pattern.compile("(\\d+)%\\s*$").matcher(GravesAndGloryClient.message);
        return m.find() ? Math.min(100, Integer.parseInt(m.group(1))) : 0;
    }
    /** The tip this wait shows: an index into Spec.TIPS chosen at random when the wait begins, -1 while no wait runs. */
    int tipIndex = -1;
    static final java.util.Random TIP_RANDOM = new java.util.Random();
    /** Every tip as it reads on this client, the key hints filled in from the player's own bindings. */
    static List<String> tipTexts() {
        List<String> out = new ArrayList<>();
        for (var t : Spec.TIPS) out.add(HintText.expand(t.text(), GravesAndGloryClient::hint));
        return out;
    }
    /** Whether the code written is a whole code. */
    boolean codeWhole() { return code.length() == CODE_LENGTH; }
    /** The camp's other commanders the pop-over lists: name, whether they stand in a party, and the room code they hold when it is open. */
    static List<JsonObject> camp() {
        List<JsonObject> out = new ArrayList<>();
        if (social().has("camp")) for (var e : social().getAsJsonArray("camp")) out.add(e.getAsJsonObject());
        return out;
    }

    /** The four seats of a side, in order: its commanders, then the seats bots will fill (bot fill on) or that stay open, then the
     *  seats beyond the room's size, closed. */
    static List<Seat> seats(int team) {
        List<Seat> out = new ArrayList<>();
        for (var r : commanders()) {
            if (!r.has("team") || r.get("team").getAsInt() != team || out.size() >= SEATS) continue;
            out.add(new Seat("human", str(r, "name"), str(r, "house"), str(r, "role"), flag(r, "ready"), !r.has("online") || flag(r, "online"), flag(r, "host"), flag(r, "me")));
        }
        boolean fill = !social().has("fill") || flag("fill");
        while (out.size() < SEATS) out.add(out.size() < size() ? new Seat(fill ? "bot" : "open", fill ? "BOT" : "Open seat", "", "", false, true, false, false) : new Seat("closed", "Closed", "", "", false, true, false, false));
        return out;
    }

    /** Builds the widgets (CampScreen.init on page lobby): again whenever the camp state changes. The first build without a room asks the server for one (social:lobby). */
    void init() {
        buttons.clear();
        if (!inRoom() && !asked) { asked = true; if (!joining && send("social:lobby")) requestAt = ticks; }
        joining = false;
        int[] o = columnAt();
        add("back", WarTable.backPlaque(b -> back()));
        int sx = o[0] + SHEET_X, sy = o[1] + TOP, nx = o[0] + NOTE_X, ny = o[1] + TOP;
        if (!inRoom()) {
            inviting = false;
            int jy = sy + JOIN_Y;
            add("code", new UiButton(sx + PAD, jy + 14, TEXT_W - 108, 24, Text.literal("Room code"), b -> typing = true, (g, x, y, w, h, hot) -> paintCode(g, x, y, w, h)));
            add("join", new UiButton(sx + PAD + TEXT_W - 100, jy + 14, 100, 24, Text.literal("Join"), b -> join(), (g, x, y, w, h, hot) -> tag(g, "join", "Join", false, x, y, w, h))).active = codeWhole();
            add("open", new UiButton(sx + (SHEET_W - 240) / 2, sy + READY_Y, 240, 40, Text.literal("Open the council"), b -> { if (send("social:lobby")) requestAt = ticks; }, (g, x, y, w, h, hot) -> plate(g, "open", "Open the council", true, x, y)));
            return;
        }
        typing = false;
        if (inviting) { initInvite(sx, sy); return; }
        if (waiting()) { // the map is being built: the wait view has the Stand down plate and nothing else to press
            choosingHost = false;
            var stand = add("ready", new UiButton(sx + (SHEET_W - 240) / 2, sy + READY_Y, 240, 40, Text.literal(readyLabel()), x -> { UiSounds.toggled(!myReady()); send("social:ready"); }, (g, x, y, w, h, hot) -> plate(g, "ready", readyLabel(), !standing(), x, y)));
            stand.active = idle();
            return;
        }
        boolean idle = idle(), host = host() && idle;
        if (!host || !heirs()) choosingHost = false;
        boolean choosing = choosingHost;
        int mine = myTeam();
        for (int t = 0; t < 2; t++) {
            var seats = seats(t);
            for (int i = 0; i < SEATS; i++) {
                final int team = t, index = i;
                var seat = seats.get(i);
                boolean heir = choosing && heir(seat);
                var b = add("seat-" + t + "-" + i, new UiButton(sx + PAD + (COL_W + COL_GAP) * t, sy + SEAT_Y + SEAT_STEP * i, COL_W, SEAT_H, Text.literal(heir ? "Make " + seat.name() + " host" : seat.name()), x -> pressSeat(team, index), (g, x, y, w, h, hot) -> paintSeat(g, team, index, x, y, w, h)));
                b.active = choosing ? heir : idle && team != mine && (seat.kind().equals("bot") || seat.kind().equals("open"));
            }
        }
        var houses = new ArrayList<>(RosterMath.fieldableHouses());
        var row = chipRow(houses);
        chipSeals.clear();
        for (int i = 0, cx = sx + PAD; i < houses.size(); cx += row.widths()[i] + CHIP_GAP, i++) {
            String id = houses.get(i);
            chipSeals.put(id, row.seals()[i]);
            var b = add("house-" + id, new UiButton(cx, sy + HOUSE_Y, row.widths()[i], WarTablePages.CHIP_H, Text.literal(RosterMath.houseName(id)), x -> send("social:house:" + id), (g, x, y, w, h, hot) -> paintHouse(g, id, x, y, w, h)));
            b.active = idle && !choosing;
        }
        String room = str("room", "");
        int hx = sx + PAD + TEXT_W;
        add("invite", new UiButton(hx - INVITE_W, sy + 10, INVITE_W, HEAD_TAG_H, Text.literal("Invite"), b -> { inviting = true; inviteOffset = 0; screen.rebuild(); }, (g, x, y, w, h, hot) -> tag(g, "invite", "Invite", false, x, y, w, h))).active = idle && !choosing;
        add("copy", new UiButton(hx - INVITE_W - 4 - COPY_W, sy + 10, COPY_W, HEAD_TAG_H, Text.literal("Copy"), b -> { MinecraftClient.getInstance().keyboard.setClipboard(room); noticeText = "Room code copied."; noticeAt = ticks; }, (g, x, y, w, h, hot) -> tag(g, "copy", "Copy", false, x, y, w, h))).active = true;
        boolean start = startNow() && !choosing;
        startBuilt = start;
        String label = readyLabel();
        int readyX = start ? sx + PAD : sx + (SHEET_W - 240) / 2;
        if (start) add("start", new UiButton(sx + PAD + 248, sy + READY_Y, START_W, 40, Text.literal("Start now"), b -> send("social:start"), (g, x, y, w, h, hot) -> tag(g, "start", "Start now", false, x, y, w, h)));
        var ready = add("ready", new UiButton(readyX, sy + READY_Y, 240, 40, Text.literal(label), b -> { UiSounds.toggled(!myReady()); send("social:ready"); }, (g, x, y, w, h, hot) -> plate(g, "ready", readyLabel(), !myReady(), x, y)));
        ready.active = idle && !choosing && houseRefusal().isEmpty();
        var modes = modes();
        for (int i = 0; i < modes.size(); i++) {
            String id = modes.get(i);
            int[] tab = UiMath.tabBox(i, MODE_COLS, NOTE_TEXT_W, MODE_GAP, MODE_H);
            var b = add("mode-" + id, new UiButton(nx + NPAD + tab[0], ny + MODE_Y + 14 + tab[1], tab[2], MODE_H, Text.literal(modeName(id)), x -> send("social:mode:" + id), (g, x, y, w, h, hot) -> tag(g, "mode-" + id, modeName(id), id.equals(str("mode", "")), x, y, w, h)));
            b.active = host;
        }
        int cardX = nx + (NOTE_W - 139) / 2, map = num("map", 0), n = Spec.MAPS.length;
        add("map-prev", new UiButton(cardX - 28, ny + CARD_Y + 30, 24, 24, Text.literal("Previous map"), b -> send("social:map:" + Math.floorMod(map - 1, n)), (g, x, y, w, h, hot) -> tag(g, "map-prev", "<", false, x, y, w, h))).active = host;
        add("map-next", new UiButton(cardX + 139 + 4, ny + CARD_Y + 30, 24, 24, Text.literal("Next map"), b -> send("social:map:" + Math.floorMod(map + 1, n)), (g, x, y, w, h, hot) -> tag(g, "map-next", ">", false, x, y, w, h))).active = host;
        int size = size();
        add("size-down", new UiButton(nx + NPAD + NOTE_TEXT_W - 24 - 52 - 24, ny + SIZE_Y, 24, 24, Text.literal("Fewer seats"), b -> send("social:size:" + (size - 1)), (g, x, y, w, h, hot) -> tag(g, "size-down", "-", false, x, y, w, h))).active = host && size > 1;
        add("size-up", new UiButton(nx + NPAD + NOTE_TEXT_W - 24, ny + SIZE_Y, 24, 24, Text.literal("More seats"), b -> send("social:size:" + (size + 1)), (g, x, y, w, h, hot) -> tag(g, "size-up", "+", false, x, y, w, h))).active = host && size < SEATS;
        add("fill", new UiButton(nx + NPAD + 44, ny + FILL_Y, NOTE_TEXT_W - 44, 24, Text.literal(fillLabel()), b -> send("social:fill"), (g, x, y, w, h, hot) -> tag(g, "fill", fillLabel(), flag("fill"), x, y, w, h))).active = host;
        int tw = (NOTE_TEXT_W - 2 * TIER_GAP) / 3;
        for (int i = 0; i < Spec.BOTCOMMAND.length && i < 3; i++) {
            String id = Spec.BOTCOMMAND[i].id(), word = CampaignClient.tierName(id);
            add("tier-" + id, new UiButton(nx + NPAD + (tw + TIER_GAP) * i, ny + TIER_Y, tw, 24, Text.literal(word), b -> send("social:difficulty:" + id), (g, x, y, w, h, hot) -> tag(g, "tier-" + id, word, id.equals(tier()), x, y, w, h))).active = host;
        }
        if (host) { // only the host has Pass host; it stands beside Leave, and Leave narrows to share the row
            add("pass-host", new UiButton(nx + NPAD, ny + LEAVE_Y, HOST_TAG_W, 24, Text.literal(passLabel()), b -> { choosingHost = !choosingHost; screen.rebuild(); }, (g, x, y, w, h, hot) -> tag(g, "pass-host", passLabel(), choosingHost, x, y, w, h))).active = heirs();
            add("leave", new UiButton(nx + NPAD + NOTE_TEXT_W - HOST_TAG_W, ny + LEAVE_Y, HOST_TAG_W, 24, Text.literal("Leave room"), b -> send("social:exit"), (g, x, y, w, h, hot) -> tag(g, "leave", "Leave room", false, x, y, w, h))).active = idle && !choosingHost;
        } else
            add("leave", new UiButton(nx + NPAD + (NOTE_TEXT_W - 120) / 2, ny + LEAVE_Y, 120, 24, Text.literal("Leave room"), b -> send("social:exit"), (g, x, y, w, h, hot) -> tag(g, "leave", "Leave room", false, x, y, w, h))).active = idle;
    }

    /** The House chip row for this frame's pick: every name whole, seals where they fit (UiMath.chipRow within the sheet's text width). */
    UiMath.ChipRow chipRow(List<String> houses) {
        var font = font();
        int[] w = new int[houses.size()];
        for (int i = 0; i < w.length; i++) w[i] = font.getWidth(DarkUi.body(chipName(houses.get(i))));
        return UiMath.chipRow(w, houses.indexOf(myHouse()), TEXT_W, CHIP_PAD, CHIP_SEAL, CHIP_SEAL_GAP, CHIP_GAP);
    }

    static String passLabel() { return "Pass host"; }

    /** A seat the host can hand his seat to: another commander, online. */
    static boolean heir(Seat seat) { return seat.kind().equals("human") && !seat.me() && seat.online(); }

    /** Whether any seat could take the host's seat. */
    static boolean heirs() { for (int t = 0; t < 2; t++) for (var s : seats(t)) if (heir(s)) return true; return false; }

    /** A seat pressed: while passing the host, hands it on (social:host:<name>); else takes the seat (social:team). */
    void pressSeat(int team, int index) {
        var seat = seats(team).get(index);
        if (choosingHost) { if (heir(seat)) { send("social:host:" + seat.name()); choosingHost = false; } screen.rebuild(); return; }
        send("social:team:" + team);
    }

    static String fillLabel() { return flag("fill") ? "Bots fill seats" : "Seats stay empty"; }

    /** Join by code: the code as written, when it is whole; the server finds the room or refuses. */
    void join() {
        typing = false;
        if (codeWhole()) send("social:join:" + code);
    }

    /** The pop-over's widgets: a row per other commander in camp (Invite, the room invitation), More when there are more rows than fit, and Close. Nothing under the panel stays a widget. */
    void initInvite(int sx, int sy) {
        var camp = camp();
        if (inviteOffset >= camp.size()) inviteOffset = 0;
        for (int k = 0; k < POP_ROWS && inviteOffset + k < camp.size(); k++) {
            String name = str(camp.get(inviteOffset + k), "name"), key = "invite-" + k;
            boolean free = str(camp.get(inviteOffset + k), "room").isEmpty();
            add(key, new UiButton(sx + POP_X + POP_W - 16 - 72, sy + POP_Y + 40 + POP_ROW_H * k, 72, 24, Text.literal("Invite " + name), b -> send("social:roominvite:" + name), (g, x, y, w, h, hot) -> tag(g, key, "Invite", false, x, y, w, h))).active = free;
        }
        if (camp.size() > POP_ROWS) add("invite-more", new UiButton(sx + POP_X + 16, sy + POP_Y + popH() - 36, 64, 24, Text.literal("More"), b -> { inviteOffset = inviteOffset + POP_ROWS < camp.size() ? inviteOffset + POP_ROWS : 0; screen.rebuild(); }, (g, x, y, w, h, hot) -> tag(g, "invite-more", "More", false, x, y, w, h)));
        add("invite-close", new UiButton(sx + POP_X + POP_W - 16 - 72, sy + POP_Y + popH() - 36, 72, 24, Text.literal("Close"), b -> { inviting = false; screen.rebuild(); }, (g, x, y, w, h, hot) -> tag(g, "invite-close", "Close", false, x, y, w, h)));
    }

    /** The pop-over's height: the title, the rows or the empty-camp lines, and the Close row. */
    static int popH() {
        var camp = camp();
        int body = camp.isEmpty() ? 13 * (emptyLines().size() + 1) + 4 : POP_ROW_H * Math.min(POP_ROWS, camp.size()) + 4;
        return POP_HEAD + body + POP_FOOT;
    }

    static List<String> emptyLines() {
        var font = font();
        return UiMath.lines("Nobody else is in this camp yet. Friends who join this game can use this code on their war council page:", POP_W - 32, 3, t -> font.getWidth(DarkUi.body(t)));
    }

    static String readyLabel() {
        if (flag("playing")) return "In battle";
        return standing() ? "Stand down" : "Ready";
    }

    void back() { UiSounds.back(); screen.page("home"); }

    boolean send(String cmd) {
        if (!screen.action(cmd)) return false;
        if (sentCommands.size() >= WarTablePages.SENT_KEPT) sentCommands.remove(0);
        sentCommands.add(cmd);
        return true;
    }

    void tick() {
        ticks++;
        boolean in = inRoom();
        if (in) requestAt = -1;
        if (wasInRoom && !in) { noticeText = ""; noticeAt = -1000; } // left the room: its notices go with it
        wasInRoom = in;
        if (CampaignClient.noticeSerial != noticeSerialSeen) {
            noticeSerialSeen = CampaignClient.noticeSerial;
            noticeText = CampaignClient.notice;
            if (!noticeText.isEmpty()) noticeAt = ticks;
        }
        if (num("fitSerial", 0) != fitSerialSeen) {
            fitSerialSeen = num("fitSerial", 0);
            fitWhat = str("fitted", "");
            if (!fitWhat.isEmpty()) fitAt = ticks;
        }
        if (!waiting()) tipIndex = -1;
        if (waiting() != waitBuilt) { waitBuilt = waiting(); screen.rebuild(); }
        boolean startWanted = inRoom() && startNow() && !choosingHost && !waiting();
        if (startWanted != startBuilt) { startBuilt = startWanted; screen.rebuild(); }
    }

    /** Whether the code line takes the keys (CampScreen asks before anything else, so Backspace repeats as it is held). */
    boolean typing(int key) {
        if (!typing) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) return false;
        if (key == GLFW.GLFW_KEY_TAB) { typing = false; return false; }
        if (key == GLFW.GLFW_KEY_BACKSPACE) { if (!code.isEmpty()) { code = code.substring(0, code.length() - 1); refreshJoin(); } return true; }
        if (net.minecraft.client.gui.screen.Screen.isPaste(key)) { for (char ch : MinecraftClient.getInstance().keyboard.getClipboard().toCharArray()) put(ch); refreshJoin(); return true; }
        return true;
    }

    boolean charTyped(char ch) {
        if (!typing) return false;
        put(ch);
        refreshJoin();
        return true;
    }

    /** Join takes a press only for a whole code. */
    void refreshJoin() { var j = buttons.get("join"); if (j != null) j.active = codeWhole(); }

    void put(char ch) {
        boolean ok = ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
        if (ok && code.length() < CODE_LENGTH) code += Character.toUpperCase(ch);
    }

    /** A click anywhere: off the code line it stops the writing. */
    void clicked(double x, double y) {
        var slot = buttons.get("code");
        if (typing && (slot == null || !slot.overArt(x, y))) typing = false;
    }

    /** Esc goes home; Enter or Space presses the widget the keyboard's focus is on, and Enter with none presses Ready. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (typing) typing = false;
            else if (inviting) { inviting = false; screen.rebuild(); }
            else back();
            return true;
        }
        if (typing && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER)) { join(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            UiButton target = null;
            if (MinecraftClient.getInstance().getNavigationType().isKeyboard()) for (var b : buttons.values()) if (b.isFocused() && b.visible) target = b;
            if (target == null && key != GLFW.GLFW_KEY_SPACE) target = buttons.get(inviting ? "invite-close" : inRoom() ? "ready" : "open");
            if (target != null && target.visible && target.active) WarTableRecruit.press(target);
            return true;
        }
        return false;
    }

    void render(DrawContext g) {
        rectsDrawn.clear(); textsDrawn.clear(); colorsDrawn.clear(); texturesDrawn.clear();
        UiCanvas.begin(g);
        try {
            int[] o = columnAt();
            WarTable.drawChrome(g, "friends");
            rectsDrawn.putAll(WarTable.drawn());
            texturesDrawn.put("table", WarTable.drawnTable());
            if (inviting && inRoom()) { // the pop-over stands over the dimmed page
                // the lobby stays drawn behind it, dimmed; what it drew is not the pop-over's to be checked against, so its records are put back as they were
                var rects = new java.util.HashMap<>(rectsDrawn); var texts = new java.util.HashMap<>(textsDrawn); var colors = new java.util.HashMap<>(colorsDrawn); var textures = new java.util.HashMap<>(texturesDrawn);
                sheet(g, o[0] + SHEET_X, o[1] + TOP);
                note(g, o[0] + NOTE_X, o[1] + TOP);
                int[] sheetBox = rectsDrawn.get("sheet"), noteBox = rectsDrawn.get("note");
                rectsDrawn.clear(); rectsDrawn.putAll(rects); textsDrawn.clear(); textsDrawn.putAll(texts); colorsDrawn.clear(); colorsDrawn.putAll(colors); texturesDrawn.clear(); texturesDrawn.putAll(textures);
                rectsDrawn.put("sheet", sheetBox); rectsDrawn.put("note", noteBox);
                g.fill(0, 0, UiCanvas.width(), UiCanvas.height(), 0x99000000);
                popover(g, o[0] + SHEET_X, o[1] + TOP);
            } else if (waiting()) {
                waitSheet(g, o[0] + SHEET_X, o[1] + TOP);
                waitNote(g, o[0] + NOTE_X, o[1] + TOP);
            } else {
                sheet(g, o[0] + SHEET_X, o[1] + TOP);
                note(g, o[0] + NOTE_X, o[1] + TOP);
            }
        } finally {
            UiCanvas.end(g);
        }
    }

    // ---- drawing helpers, as the troop sheet's ----
    void head(DrawContext g, String key, String word, String right, int x, int y, int w) {
        var font = font();
        g.drawText(font, DarkUi.heading(word), x, y, DarkUi.INK_TEXT, false);
        g.fill(x, y + 12, x + w, y + 13, DarkUi.SEPIA_LINE);
        if (right != null && !right.isEmpty()) {
            int rw = font.getWidth(DarkUi.body(right));
            g.drawText(font, DarkUi.body(right), x + w - rw, y + 1, DarkUi.SEPIA, false);
            rectsDrawn.put(key, new int[]{x + w - rw, y + 1, rw, 11});
            textsDrawn.put(key, right);
            colorsDrawn.put(key, DarkUi.SEPIA);
        }
    }

    int line(DrawContext g, String key, Text t, String s, int x, int y, int color) {
        var font = font();
        int w = font.getWidth(t);
        g.drawText(font, t, x, y, color, false);
        if (key != null) { rectsDrawn.put(key, new int[]{x, y, w, 11}); textsDrawn.put(key, s); colorsDrawn.put(key, color); }
        return w;
    }

    void underline(DrawContext g, int x, int y, int width, int color) { g.fill(x, y, x + width, y + 1, color); }

    static Spec.HousesRow houseRow(String id) { for (var h : Spec.HOUSES) if (h.id().equals(id)) return h; return null; }
    static Spec.GearSetsRow setRow(String id) { for (var r : Spec.GEARSETS) if (r.id().equals(id)) return r; return null; }
    /** The shared seat icon of a House set (Kort 2026-11-04: classes -, so no class art to borrow) until it has its own. */
    static final String HOUSE_SET_ICON = "table/icon-banner";
    /** Whether a gearSets row is one of the House sets (no starting class), not one of the four starter roles. */
    static boolean houseSet(Spec.GearSetsRow r) { return r != null && r.classes().equals("-"); }
    /** A role's word: a starter role's id capitalised (Bruiser, Healer, Ranged, Commander), a House set's own name (Barrow Warden). */
    static String roleName(String id) {
        var r = setRow(id);
        if (houseSet(r)) return r.name();
        return id.isEmpty() ? "" : Character.toUpperCase(id.charAt(0)) + id.substring(1);
    }
    /** The seat icon of a role: the art the first class of its set had (the table has no role icons yet), HOUSE_SET_ICON for a House set,
     *  or null for an unknown role. */
    static String roleIcon(String id) {
        var r = setRow(id);
        if (r == null) return null;
        return houseSet(r) ? HOUSE_SET_ICON : "table/icon-class-" + r.classes().split(",")[0];
    }

    void seal(DrawContext g, String house, int x, int y, int size) {
        var h = houseRow(house);
        if (h != null) g.drawTexture(new Identifier(h.glyph()), x, y, 0f, 0f, size, size, size, size);
    }

    // ---- the council sheet ----
    void sheet(DrawContext g, int x, int y) {
        var font = font();
        rectsDrawn.put("sheet", UiTex.draw(g, "table/sheet-banner", x, y));
        int lx = x + PAD;
        if (!inRoom()) {
            head(g, "room", "War council", null, lx, y + 18, TEXT_W);
            String said = requestAt < 0 ? "" : ticks - requestAt <= REQUEST_TICKS ? "Opening a room for your party..." : "Could not open a room. Try again.";
            if (!said.isEmpty()) line(g, "status", DarkUi.body(said), said, lx, y + 40, DarkUi.SEPIA);
            head(g, "join-head", "Join by code", null, lx, y + JOIN_Y, TEXT_W);
            line(g, "join-hint", DarkUi.body("A friend's code opens their war council."), "A friend's code opens their war council.", lx, y + JOIN_Y + 42, DarkUi.SEPIA);
            notice(g, lx, y);
            return;
        }
        // the heading, then the room's code (in Minecraft's face, whose 0 and O differ) left of the Copy and Invite tags
        head(g, "room", "War council", null, lx, y + 18, TEXT_W);
        String roomCode = str("room", "");
        int codeRight = lx + TEXT_W - INVITE_W - 4 - COPY_W - 8, codeW = WarTablePages.codeWidth(roomCode);
        var codeLabel = DarkUi.body("Room");
        g.drawText(font, codeLabel, codeRight - codeW - 5 - font.getWidth(codeLabel), y + 17, DarkUi.SEPIA, false);
        WarTablePages.drawCode(g, font, roomCode, codeRight - codeW, y + 17, DarkUi.INK_TEXT);
        rectsDrawn.put("code", new int[]{codeRight - codeW, y + 17, codeW, 9});
        textsDrawn.put("code", roomCode);
        textsDrawn.put("room", "Room " + roomCode);
        for (int t = 0; t < 2; t++) {
            int tx = lx + (COL_W + COL_GAP) * t;
            DarkUi.teamMark(g, t, tx, y + TEAM_Y + 2);
            g.drawText(font, DarkUi.heading(t == 0 ? "Dawn" : "Dusk"), tx + 11, y + TEAM_Y, DarkUi.INK_TEXT, false);
            int humans = (int) seats(t).stream().filter(s -> s.kind().equals("human")).count();
            String count = humans + " of " + size();
            int cw = font.getWidth(DarkUi.body(count));
            line(g, "count-" + t, DarkUi.body(count), count, tx + COL_W - cw, y + TEAM_Y + 1, DarkUi.SEPIA);
        }
        String host = hostName();
        String hostLine = choosingHost ? "Choose a commander's seat to make them host." : host() ? "You host this council: you set the orders." : "Host: " + host + ". The host sets the orders.";
        line(g, "host", DarkUi.body(hostLine), hostLine, lx, y + HOST_Y, DarkUi.SEPIA);
        String house = myHouse();
        var hr = houseRow(house);
        int troops = myTroops();
        head(g, "house", "Your House", hr == null ? "" : hr.name() + (troops > 0 ? ", " + troops + (troops == 1 ? " troop" : " troops") : troops == 0 ? ", no troops" : ""), lx, y + HOUSE_HEAD_Y, TEXT_W);
        String role = myRole();
        var sr = setRow(role);
        head(g, "role", "Your role", sr == null ? "" : houseSet(sr) ? sr.name() : roleName(role) + ", " + sr.name(), lx, y + ROLE_HEAD_Y, TEXT_W);
        int worn = sr == null ? 0 : GearStats.of(CampaignClient.profile).setCounts().getOrDefault(role, 0);
        String gearLine = UiMath.trim(worn + " of 4 " + (sr == null ? "" : sr.name()) + " pieces worn. Change gear and relics in the Arsenal.", TEXT_W, t -> font.getWidth(DarkUi.body(t)));
        line(g, "gear", DarkUi.body(gearLine), gearLine, lx, y + ROLE_Y, DarkUi.SEPIA);
        String sum = UiMath.trim(summary(str("mode", "")), TEXT_W, t -> font.getWidth(DarkUi.body(t)));
        if (!sum.isEmpty()) line(g, "summary", DarkUi.body(sum), sum, lx, y + SUMMARY_Y, DarkUi.INK_TEXT);
        status(g, x, y);
        notice(g, lx, y);
    }

    // ---- the map wait: the sheet and the note while the server builds the map ----
    /** Sheet-relative rows of the wait view: the progress bar, the side headings, the first line-up row and each row's step, the tip's heading and its first line. */
    static final int WAIT_BAR_Y = 40, WAIT_BAR_H = 8, WAIT_TEAM_Y = 62, WAIT_ROW_Y = 82, WAIT_ROW_STEP = 22, WAIT_TIP_Y = 184, WAIT_TIP_TEXT_Y = 204, WAIT_TIP_STEP = 13, WAIT_TIP_LINES = 3;
    /** Note-relative rows of the wait view: the card, and the mode heading with its summary below. */
    static final int WAIT_CARD_Y = 44, WAIT_MODE_Y = 176, WAIT_SUMMARY_Y = 196, WAIT_SUMMARY_LINES = 4;

    /** The sheet while the map builds: the map's name and percent, the bar, both line-ups (a commander's seal, name and House; bots say Bot) and one tip. */
    void waitSheet(DrawContext g, int x, int y) {
        var font = font();
        rectsDrawn.put("sheet", UiTex.draw(g, "table/sheet-banner", x, y));
        int lx = x + PAD, map = Math.max(0, Math.min(Spec.MAPS.length - 1, num("map", 0)));
        String name = "Preparing " + Spec.MAPS[map].name(), pct = waitPercent() + "%";
        head(g, "wait-percent", name, pct, lx, y + 18, TEXT_W);
        rectsDrawn.put("wait-head", new int[]{lx, y + 18, font.getWidth(DarkUi.heading(name)), 11});
        textsDrawn.put("wait-head", name);
        int by = y + WAIT_BAR_Y, fillW = (TEXT_W - 2) * waitPercent() / 100;
        g.fill(lx, by, lx + TEXT_W, by + WAIT_BAR_H, DarkUi.SEPIA_LINE);
        g.fill(lx + 1, by + 1, lx + TEXT_W - 1, by + WAIT_BAR_H - 1, DarkUi.VELLUM);
        g.fill(lx + 1, by + 1, lx + 1 + fillW, by + WAIT_BAR_H - 1, DarkUi.BRASS);
        rectsDrawn.put("wait-bar", new int[]{lx, by, TEXT_W, WAIT_BAR_H});
        textsDrawn.put("wait-bar", String.valueOf(waitPercent()));
        for (int t = 0; t < 2; t++) {
            int tx = lx + (COL_W + COL_GAP) * t;
            DarkUi.teamMark(g, t, tx, y + WAIT_TEAM_Y + 2);
            String side = t == 0 ? "Dawn" : "Dusk";
            g.drawText(font, DarkUi.heading(side), tx + 11, y + WAIT_TEAM_Y, DarkUi.INK_TEXT, false);
            rectsDrawn.put("wait-side-" + t, new int[]{tx, y + WAIT_TEAM_Y, 11 + font.getWidth(DarkUi.heading(side)), 11});
            textsDrawn.put("wait-side-" + t, side);
            int row = 0;
            for (var seat : seats(t)) {
                boolean human = seat.kind().equals("human");
                if (!human && !seat.kind().equals("bot")) continue;
                int ry = y + WAIT_ROW_Y + WAIT_ROW_STEP * row;
                String key = "wait-seat-" + t + "-" + row, who = human ? seat.name() : "Bot", house = human && houseRow(seat.house()) != null ? chipName(seat.house()) : "";
                int hw = house.isEmpty() ? 0 : font.getWidth(DarkUi.body(house)), nx = tx + CHIP_SEAL + CHIP_SEAL_GAP;
                if (human) seal(g, seat.house(), tx, ry, CHIP_SEAL);
                String shown = UiMath.trim(who, COL_W - CHIP_SEAL - CHIP_SEAL_GAP - (hw == 0 ? 0 : hw + 6), s -> font.getWidth(DarkUi.body(s)));
                g.drawText(font, DarkUi.body(shown), nx, ry + 4, human ? DarkUi.INK_TEXT : DarkUi.SEPIA, false);
                if (hw > 0) g.drawText(font, DarkUi.body(house), tx + COL_W - hw, ry + 4, DarkUi.SEPIA, false);
                g.fill(tx, ry + CHIP_SEAL + 2, tx + COL_W, ry + CHIP_SEAL + 3, DarkUi.SEPIA_L);
                rectsDrawn.put(key, new int[]{tx, ry, COL_W, CHIP_SEAL});
                textsDrawn.put(key, who);
                textsDrawn.put(key + "-house", house);
                row++;
            }
        }
        head(g, "wait-tip-head", "Tip", null, lx, y + WAIT_TIP_Y, TEXT_W);
        var tips = tipTexts();
        if (tipIndex < 0 && !tips.isEmpty()) tipIndex = TIP_RANDOM.nextInt(tips.size());
        if (tipIndex >= 0 && tipIndex < tips.size()) {
            var lines = UiMath.lines(tips.get(tipIndex), TEXT_W, WAIT_TIP_LINES, s -> font.getWidth(DarkUi.body(s)));
            int widest = 0;
            for (int i = 0; i < lines.size(); i++) {
                g.drawText(font, DarkUi.body(lines.get(i)), lx, y + WAIT_TIP_TEXT_Y + WAIT_TIP_STEP * i, DarkUi.INK_TEXT, false);
                widest = Math.max(widest, font.getWidth(DarkUi.body(lines.get(i))));
            }
            rectsDrawn.put("wait-tip", new int[]{lx, y + WAIT_TIP_TEXT_Y, widest, WAIT_TIP_STEP * (lines.size() - 1) + 11});
            textsDrawn.put("wait-tip", tips.get(tipIndex));
            textsDrawn.put("wait-tip-drawn", String.join(" ", lines));
        }
        status(g, x, y);
        notice(g, lx, y);
    }

    /** The note while the map builds: the map's card with its name, and the mode with its one line of rules. */
    void waitNote(DrawContext g, int x, int y) {
        var font = font();
        rectsDrawn.put("note", UiTex.draw(g, "table/note-room", x, y));
        int lx = x + NPAD, map = Math.max(0, Math.min(Spec.MAPS.length - 1, num("map", 0)));
        var m = Spec.MAPS[map];
        head(g, "wait-map-head", "Map", null, lx, y + 18, NOTE_TEXT_W);
        String card = "table/map-" + m.id();
        int cardX = x + (NOTE_W - 139) / 2;
        rectsDrawn.put("wait-card", UiTex.draw(g, card, cardX, y + WAIT_CARD_Y));
        texturesDrawn.put("wait-card", card);
        textsDrawn.put("wait-card", m.id());
        var nameT = DarkUi.heading(m.name());
        WarTableHome.centred(g, nameT, x + NOTE_W / 2, y + WAIT_CARD_Y + 89, DarkUi.INK_TEXT);
        int nw = font.getWidth(nameT);
        rectsDrawn.put("wait-map-name", new int[]{x + NOTE_W / 2 - nw / 2, y + WAIT_CARD_Y + 89, nw, 11});
        textsDrawn.put("wait-map-name", m.name());
        String mode = str("mode", "");
        head(g, "wait-mode-head", modeName(mode), null, lx, y + WAIT_MODE_Y, NOTE_TEXT_W);
        textsDrawn.put("wait-mode-head", modeName(mode));
        var lines = UiMath.lines(summary(mode), NOTE_TEXT_W, WAIT_SUMMARY_LINES, s -> font.getWidth(DarkUi.body(s)));
        int widest = 0;
        for (int i = 0; i < lines.size(); i++) {
            g.drawText(font, DarkUi.body(lines.get(i)), lx, y + WAIT_SUMMARY_Y + 13 * i, DarkUi.SEPIA, false);
            widest = Math.max(widest, font.getWidth(DarkUi.body(lines.get(i))));
        }
        if (!lines.isEmpty()) {
            rectsDrawn.put("wait-mode-summary", new int[]{lx, y + WAIT_SUMMARY_Y, widest, 13 * (lines.size() - 1) + 11});
            textsDrawn.put("wait-mode-summary", String.join(" ", lines));
        }
    }

    /** A friend-list pop-over on the sheet: a parchment panel on the House chips' nine-slice (title, a row per other commander in camp whose Invite tag is a widget, and what to do when none), sized to its rows. */
    void popover(DrawContext g, int x, int y) {
        var font = font();
        int px = x + POP_X, py = y + POP_Y, ph = popH();
        UiTex.nine(g, "table/chip-house", px, py, POP_W + 1, ph + 2);
        texturesDrawn.put("popover", "table/chip-house");
        rectsDrawn.put("popover", new int[]{px, py, POP_W, ph});
        g.drawText(font, DarkUi.heading("Invite a commander"), px + 16, py + 14, DarkUi.INK_TEXT, false);
        g.fill(px + 16, py + 27, px + POP_W - 16, py + 28, DarkUi.SEPIA_LINE);
        textsDrawn.put("popover", "Invite a commander");
        var camp = camp();
        if (camp.isEmpty()) {
            var lines = emptyLines();
            for (int i = 0; i < lines.size(); i++) g.drawText(font, DarkUi.body(lines.get(i)), px + 16, py + 40 + 13 * i, DarkUi.SEPIA, false);
            String roomCode = str("room", "");
            int codeY = py + 40 + 13 * lines.size();
            WarTablePages.drawCode(g, font, roomCode, px + 16, codeY, DarkUi.INK_TEXT);
            rectsDrawn.put("popover-code", new int[]{px + 16, codeY, WarTablePages.codeWidth(roomCode), 9});
            textsDrawn.put("popover-empty", String.join(" ", lines) + " " + roomCode);
            return;
        }
        for (int k = 0; k < POP_ROWS && inviteOffset + k < camp.size(); k++) {
            var row = camp.get(inviteOffset + k);
            String name = UiMath.trim(str(row, "name"), POP_W - 32 - 72 - 90, t -> font.getWidth(t));
            int ry = py + 40 + POP_ROW_H * k;
            boolean free = str(row, "room").isEmpty();
            g.drawText(font, Text.literal(name), px + 16, ry + 8, free ? DarkUi.INK_TEXT : DarkUi.SEPIA, false);
            if (!free) g.drawText(font, DarkUi.body("in a room"), px + 16 + font.getWidth(name) + 8, ry + 8, DarkUi.SEPIA, false);
            textsDrawn.put("popover-row-" + k, str(row, "name"));
        }
    }

    /** The code line of the join field: the code in INK in Minecraft's face on a written line, the caret while it is being written, and until it is whole how much of it is written; what to do, in SEPIA, until writing starts. */
    void paintCode(DrawContext g, int x, int y, int w, int h) {
        var font = font();
        var b = buttons.get("code");
        boolean hot = WarTableRecruit.hot(b);
        g.fill(x, y + h - 3, x + w, y + h - 2, typing || hot ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        if (code.isEmpty() && !typing) g.drawText(font, DarkUi.body("Click to write the code"), x + 4, y + 7, DarkUi.SEPIA, false);
        else {
            WarTablePages.drawCode(g, font, code, x + 4, y + 9, DarkUi.INK_TEXT);
            if (typing && ticks / 10 % 2 == 0) {
                int cx = x + 5 + WarTablePages.codeWidth(code);
                g.fill(cx, y + 7, cx + 1, y + 18, DarkUi.INK_TEXT);
            }
            if (code.length() < CODE_LENGTH) {
                String count = code.length() + " of " + CODE_LENGTH;
                int cw = font.getWidth(count);
                g.drawText(font, Text.literal(count), x + w - 4 - cw, y + 9, DarkUi.SEPIA, false);
                textsDrawn.put("code-count", count);
            }
        }
        rectsDrawn.put("code-line", new int[]{x, y, w, h});
        textsDrawn.put("room-code", code);
    }

    void paintSeat(DrawContext g, int team, int index, int x, int y, int w, int h) {
        var font = font();
        var seat = seats(team).get(index);
        String key = "seat-" + team + "-" + index;
        var b = buttons.get(key);
        boolean closed = seat.kind().equals("closed");
        String state;
        if (closed) {
            g.fill(x + 4, y + h / 2, x + w - 4, y + h / 2 + 1, DarkUi.SEPIA_LINE);
            int cw = font.getWidth(DarkUi.body("Closed"));
            g.fill(x + (w - cw) / 2 - 4, y + 4, x + (w + cw) / 2 + 4, y + h - 4, DarkUi.VELLUM);
            g.drawText(font, DarkUi.body("Closed"), x + (w - cw) / 2, y + 7, DarkUi.SEPIA, false);
            state = "closed";
        } else {
            String plate = seat.me() ? "table/chip-house-on" : "table/chip-house";
            UiTex.nine(g, plate, x, y, w + 1, h + 2);
            texturesDrawn.put(key, plate);
            int ink = seat.kind().equals("human") && seat.online() ? DarkUi.INK_TEXT : DarkUi.SEPIA;
            switch (seat.kind()) {
                case "human" -> {
                    seal(g, seat.house(), x + 5, y + 4, 16);
                    int right = x + w - 6;
                    if (b != null && b.active && choosingHost) { // passing the host: the seat says so
                        String hint = "Make host";
                        int hw = font.getWidth(DarkUi.body(hint));
                        g.drawText(font, DarkUi.body(hint), right - hw, y + 7, DarkUi.INK_TEXT, false);
                        underline(g, right - hw, y + 18, hw, WarTableRecruit.hot(b) ? DarkUi.INK_TEXT : DarkUi.SEPIA_LINE);
                        textsDrawn.put(key + "-hint", hint);
                        right -= hw + 6;
                    }
                    if (seat.ready()) { UiTex.draw(g, "table/ready-check", right - 9, y + 8); right -= 13; }
                    String icon = roleIcon(seat.role());
                    if (icon != null) { UiTex.draw(g, icon, right - 14, y + 5); right -= 18; }
                    String tagWord = seat.host() ? "Host" : !seat.online() ? "Away" : "";
                    if (!tagWord.isEmpty()) { int tw = font.getWidth(DarkUi.body(tagWord)); g.drawText(font, DarkUi.body(tagWord), right - tw, y + 7, DarkUi.SEPIA, false); right -= tw + 5; }
                    String name = UiMath.trim(seat.name(), right - (x + 25), s -> font.getWidth(DarkUi.body(s)));
                    g.drawText(font, DarkUi.body(name), x + 25, y + 7, ink, false);
                    if (seat.me()) g.fill(x + 25, y + 18, x + 25 + font.getWidth(DarkUi.body(name)), y + 19, DarkUi.INK_TEXT);
                    textsDrawn.put(key + "-house", seat.house());
                    textsDrawn.put(key + "-role", seat.role());
                    state = seat.ready() ? "ready" : "waiting";
                }
                case "bot" -> {
                    g.drawText(font, DarkUi.heading("BOT"), x + 8, y + 7, DarkUi.SEPIA, false);
                    String hint = b != null && b.active ? "Take this seat" : "Filled at the start";
                    int hw = font.getWidth(DarkUi.body(hint));
                    g.drawText(font, DarkUi.body(hint), x + w - 6 - hw, y + 7, DarkUi.SEPIA, false);
                    if (WarTableRecruit.hot(b)) underline(g, x + w - 6 - hw, y + 18, hw, DarkUi.SEPIA);
                    state = "bot";
                }
                default -> {
                    g.drawText(font, DarkUi.body("Open seat"), x + 8, y + 7, DarkUi.SEPIA, false);
                    if (b != null && b.active) {
                        String hint = "Take this seat";
                        int hw = font.getWidth(DarkUi.body(hint));
                        g.drawText(font, DarkUi.body(hint), x + w - 6 - hw, y + 7, DarkUi.SEPIA, false);
                        if (WarTableRecruit.hot(b)) underline(g, x + w - 6 - hw, y + 18, hw, DarkUi.SEPIA);
                    }
                    state = "open";
                }
            }
            colorsDrawn.put(key, ink);
        }
        rectsDrawn.put(key, new int[]{x, y, w, h});
        textsDrawn.put(key, seat.name());
        textsDrawn.put(key + "-state", state);
    }

    void paintHouse(DrawContext g, String id, int x, int y, int w, int h) {
        var font = font();
        boolean on = id.equals(myHouse());
        int dy = on ? 1 : 0;
        String tex = on ? "table/chip-house-on" : "table/chip-house";
        UiTex.nine(g, tex, x, y, w + 1, on ? h + WarTablePages.CHIP_RULE : h + 2);
        boolean withSeal = chipSeals.getOrDefault(id, false);
        int tx = x + CHIP_PAD;
        if (withSeal) {
            seal(g, id, x + CHIP_PAD, y + 4 + dy, CHIP_SEAL);
            if (on) UiTex.draw(g, "table/ready-check", x + CHIP_PAD + 8, y + 13 + dy);
            tx += CHIP_SEAL + CHIP_SEAL_GAP;
        }
        String name = chipName(id); // whole: the row was laid out to fit every name (chipRow)
        int ink = buttons.get("house-" + id) != null && buttons.get("house-" + id).active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        int nw = font.getWidth(DarkUi.body(name));
        g.drawText(font, DarkUi.body(name), tx, y + 6 + dy, ink, false);
        if (WarTableRecruit.hot(buttons.get("house-" + id))) underline(g, tx, y + 18 + dy, nw, ink);
        rectsDrawn.put("house-" + id + "-name", new int[]{tx, y + 6 + dy, nw, 11});
        textsDrawn.put("house-" + id + "-name", name);
        textsDrawn.put("house-" + id + "-seal", String.valueOf(withSeal));
        rectsDrawn.put("house-" + id, new int[]{x, y, w, h});
        texturesDrawn.put("house-" + id, tex);
        if (on) textsDrawn.put("house", id);
    }

    /** A 240x40 plate: brass while it calls to act (Ready, Open), iron once pressed (Stand down), dull while it cannot be pressed. */
    void plate(DrawContext g, String key, String label, boolean call, int x, int y) {
        var font = font();
        var b = buttons.get(key);
        boolean live = b != null && b.active;
        String tex = !live ? "table/btn-dull-240x40" : call ? "table/btn-brass-240x40" : "table/btn-iron-240x40";
        UiTex.draw(g, tex, x, y);
        var t = DarkUi.heading(label);
        int tw = font.getWidth(t), ink = live ? DarkUi.PLATE_INK : DarkUi.MUTED; // muted on the dull plate reads at 5.4:1, where sepia was 1.3:1 (Kort, 2026-10-31)
        if (!call && live) ink = DarkUi.IVORY;
        g.drawText(font, t, x + (240 - tw) / 2, y + 15, ink, false);
        if (WarTableRecruit.hot(b)) underline(g, x + (240 - tw) / 2, y + 27, tw, ink);
        rectsDrawn.put(key, new int[]{x, y, 240, 40});
        texturesDrawn.put(key, tex);
        textsDrawn.put(key, label);
        colorsDrawn.put(key, ink);
    }

    /** A vellum tag with a word; on (the mode chosen, the bots on) carries the heavy bar under it; while only the host may press it, it is sepia for the others. */
    void tag(DrawContext g, String key, String label, boolean on, int x, int y, int w, int h) {
        var font = font();
        var b = buttons.get(key);
        boolean live = b != null && b.active;
        String plate = "table/btn-vellum-88x24";
        UiTex.nine(g, plate, x, y, w + 1, h + 2);
        if (!live && !on) DarkUi.disabledVeil(g, x + 1, y + 1, w - 1, h); // veiled as well as sepia while it cannot be pressed (4F.5)
        var t = DarkUi.heading(label);
        int tw = font.getWidth(t), lx = x + (w - tw) / 2, ink = live || on ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        int ty = h > 24 ? y + (h - 12) / 2 : y + 6; // a tag as tall as a plate (Start now) centres its word
        g.drawText(font, t, lx, ty, ink, false);
        if (on) g.fill(lx, ty + 12, lx + tw, ty + 14, DarkUi.INK_TEXT);
        else if (WarTableRecruit.hot(b)) underline(g, lx, ty + 12, tw, ink);
        rectsDrawn.put(key, new int[]{x, y, w, h});
        texturesDrawn.put(key, plate);
        colorsDrawn.put(key, ink);
    }

    /** Under the Ready plate: the countdown while it runs (the focal font), else what the room still waits for. */
    void status(DrawContext g, int x, int y) {
        int seconds = num("countdown", 0);
        String s;
        int ink;
        Text t;
        if (flag("playing")) { s = "Your room is in battle."; ink = DarkUi.SEPIA; t = DarkUi.body(s); }
        else if (waiting()) { s = "Preparing the battlefield. Stand down to stop."; ink = DarkUi.INK_TEXT; t = DarkUi.body(s); }
        else if (seconds > 0) { s = "Battle begins in " + seconds; ink = DarkUi.INK_TEXT; t = DarkUi.focal(s); textsDrawn.put("countdown", String.valueOf(seconds)); }
        else if (!houseRefusal().isEmpty()) { s = houseRefusal(); ink = DarkUi.INK_TEXT; t = DarkUi.body(s); }
        else {
            String waiting = str("waiting", "");
            s = waiting.isEmpty() ? "Everyone is ready." : waiting.equals("Every commander must ready up.") ? readyCount() : waiting;
            ink = DarkUi.SEPIA; t = DarkUi.body(s);
        }
        var font = font();
        int w = font.getWidth(t), mid = startBuilt && seconds > 0 ? PAD + 120 : SHEET_W / 2, lx = x + mid - w / 2; // beside Start now the line sits under the Ready plate
        g.drawText(font, t, lx, y + STATUS_Y - (seconds > 0 ? 3 : 0), ink, false);
        rectsDrawn.put("status", new int[]{lx, y + STATUS_Y, w, 11});
        textsDrawn.put("status", s);
        colorsDrawn.put("status", ink);
    }

    static String readyCount() {
        var all = commanders();
        long ready = all.stream().filter(r -> flag(r, "ready")).count();
        return ready + " of " + all.size() + " ready. The battle begins ten seconds after the last.";
    }

    // ---- the orders note ----
    void note(DrawContext g, int x, int y) {
        var font = font();
        rectsDrawn.put("note", UiTex.draw(g, "table/note-room", x, y));
        if (!inRoom()) return;
        int lx = x + NPAD;
        String who = host() ? "You set these" : "Set by " + hostName();
        head(g, "orders", "Orders", who, lx, y + 18, NOTE_TEXT_W);
        textsDrawn.put("orders", host() ? "host" : hostName());
        line(g, null, DarkUi.body("Mode"), "Mode", lx, y + MODE_Y, DarkUi.SEPIA);
        textsDrawn.put("mode", str("mode", ""));
        boolean fit = !fitWhat.isEmpty() && ticks - fitAt < FIT_TICKS;
        if (fit && fitWhat.equals("mode")) fitNote(g, "fit-mode", "changed to fit the map", x + NPAD + NOTE_TEXT_W, y + MODE_Y);
        line(g, null, DarkUi.body("Map"), "Map", lx, y + MAP_Y, DarkUi.SEPIA);
        if (fit && fitWhat.equals("map")) fitNote(g, "fit-map", "changed to fit " + modeName(str("mode", "")), x + NPAD + NOTE_TEXT_W, y + MAP_Y);
        int map = Math.max(0, Math.min(Spec.MAPS.length - 1, num("map", 0)));
        var m = Spec.MAPS[map];
        int cardX = x + (NOTE_W - 139) / 2;
        String card = "table/map-" + m.id();
        rectsDrawn.put("map-card", UiTex.draw(g, card, cardX, y + CARD_Y));
        texturesDrawn.put("map-card", card);
        WarTableHome.centred(g, DarkUi.heading(m.name()), x + NOTE_W / 2, y + CARD_Y + 89, DarkUi.INK_TEXT);
        String theme = UiMath.trim(m.theme(), NOTE_TEXT_W, s -> font.getWidth(DarkUi.body(s)));
        WarTableHome.centred(g, DarkUi.body(theme), x + NOTE_W / 2, y + CARD_Y + 102, DarkUi.SEPIA);
        textsDrawn.put("map", m.id());
        line(g, null, DarkUi.body("Seats"), "Seats", lx, y + SIZE_Y + 7, DarkUi.SEPIA);
        String size = size() + " v " + size();
        WarTableHome.centred(g, DarkUi.focal(size), x + NPAD + NOTE_TEXT_W - 24 - 26, y + SIZE_Y + 8, DarkUi.INK_TEXT);
        textsDrawn.put("size", String.valueOf(size()));
        line(g, null, DarkUi.body("Bots"), "Bots", lx, y + FILL_Y + 7, DarkUi.SEPIA);
        textsDrawn.put("fill", String.valueOf(flag("fill")));
        textsDrawn.put("tier", tier());
    }

    /** "changed to fit X", right-aligned on a control's label row in ink, for FIT_TICKS after the server moved the control. */
    void fitNote(DrawContext g, String key, String words, int right, int y) {
        var font = font();
        String shown = UiMath.trim(words, NOTE_TEXT_W - 48, t -> font.getWidth(DarkUi.body(t)));
        int w = font.getWidth(DarkUi.body(shown));
        g.drawText(font, DarkUi.body(shown), right - w, y, DarkUi.INK_TEXT, false);
        rectsDrawn.put(key, new int[]{right - w, y, w, 11});
        textsDrawn.put(key, shown);
        colorsDrawn.put(key, DarkUi.INK_TEXT);
    }

    /** The server's last notice, under the countdown line, shown WarTableHome.NOTICE_TICKS and then faded. */
    void notice(DrawContext g, int x, int y) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= WarTableHome.NOTICE_TICKS + WarTableHome.NOTICE_FADE_TICKS) return;
        if (inRoom() && myTeam() >= 0 && noticeText.contains(" Choose a team and ready up.")) noticeText = noticeText.replace(" Choose a team and ready up.", " Ready up when you are set."); // a seated guest has chosen
        if (inRoom() && noticeText.startsWith("Your banner holds no") && !houseRefusal().isEmpty()) return; // the status line under Ready says it already
        if (inRoom() && num("countdown", 0) > 0 && noticeText.startsWith("All commanders ready")) return; // the countdown line says it already
        var font = font();
        float fade = age < WarTableHome.NOTICE_TICKS ? 1 : 1 - (age - WarTableHome.NOTICE_TICKS) / (float) WarTableHome.NOTICE_FADE_TICKS;
        var lines = UiMath.lines(HintText.expand(noticeText, GravesAndGloryClient::hint), TEXT_W, NOTICE_LINES, s -> font.getWidth(DarkUi.body(s)));
        if (lines.isEmpty()) return;
        int alpha = Math.max(4, Math.round(fade * 255)), ink = (alpha << 24) | (DarkUi.INK_TEXT & 0xffffff);
        int w = 0;
        for (int i = 0; i < lines.size(); i++) { g.drawText(font, DarkUi.body(lines.get(i)), x, y + NOTICE_Y + NOTICE_STEP * i, ink, false); w = Math.max(w, font.getWidth(DarkUi.body(lines.get(i)))); }
        rectsDrawn.put("notice", new int[]{x, y + NOTICE_Y, w, NOTICE_STEP * (lines.size() - 1) + 9});
        textsDrawn.put("notice", String.join(" ", lines));
    }
}
