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
 *  name, class and ready mark; a seat bots will fill says BOT; a seat beyond the room's size is closed), then the viewer's own
 *  House and class, the Ready plate and the countdown line. On the right the orders note: the mode, the map, the seats a side and
 *  the bot fill, which only the host changes and everyone sees. Every commander ready starts the server's ten-second countdown
 *  (SocialHub.countdown); anyone standing down stops it. The state is the server's (Campaign.socialSnapshot): the page only sends
 *  social: and class: commands. Everything is drawn in canvas pixels; page coordinates are offset by WarTable.column(COMPACT). */
final class WarTableLobby {
    /** The sheet and the note on the 960 page: the pair stands centred, 24 apart, both papers 124 from the top. */
    static final int SHEET_X = 150, NOTE_X = 578, TOP = 124, SHEET_W = 404, NOTE_W = 232, PAPER_H = 373;
    /** The compact page (WarTable.compact): the pair moves left 53 and up 32, as the troop sheet's does. */
    static final int[] COMPACT = {-53, -32};
    /** On the sheet (sheet-relative): text edge and width, the seat columns, the seats' rows, the picks and the Ready plate. */
    static final int PAD = 22, TEXT_W = 360, COL_W = 174, COL_GAP = 12, SEATS = 4, TEAM_Y = 40, SEAT_Y = 56, SEAT_H = 24, SEAT_STEP = 27,
            HOST_Y = 166, HOUSE_HEAD_Y = 184, HOUSE_Y = 202, CHIP_PAD = 6, CHIP_SEAL = 16, CHIP_SEAL_GAP = 4, CHIP_GAP = 4, CLASS_HEAD_Y = 236, CLASS_Y = 254, TILE_W = 36, TILE_GAP = 4,
            READY_Y = 288, STATUS_Y = 334, NOTICE_Y = 350, STEP = 12;
    /** On the note (note-relative): its text edge and width, and the rows of the orders. */
    static final int NPAD = 18, NOTE_TEXT_W = 196, MODE_Y = 44, MAP_Y = 92, CARD_Y = 108, SIZE_Y = 230, FILL_Y = 278, LEAVE_Y = 330;
    /** The host's two tags on the last row of the note, Pass host and Leave, each this wide. */
    static final int HOST_TAG_W = 96;

    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> colorsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> texturesDrawn = new LinkedHashMap<>();
    private static final List<String> sentCommands = new ArrayList<>();
    /** Boxes {x,y,w,h} in canvas px the last frame drew: table, rail, sheet, note, seat-(team)-(i), house-(id), class-(id), ready,
     *  status, mode-(id), map-card, map-prev, map-next, size-down, size-up, fill, leave, notice. */
    static Map<String, int[]> rects() { return Collections.unmodifiableMap(rectsDrawn); }
    /** Words drawn: room, host, seat-(team)-(i) (a commander's name, "BOT", "Open" or "Closed"), seat-(team)-(i)-state ("ready",
     *  "waiting", "bot", "open", "closed"), seat-(team)-(i)-house, seat-(team)-(i)-class, house (the viewer's pick), class, ready
     *  (the plate's label), status, countdown, mode, map, size, fill, orders ("host" or the host's name), notice. */
    static Map<String, String> texts() { return Collections.unmodifiableMap(textsDrawn); }
    static Map<String, Integer> colors() { return Collections.unmodifiableMap(colorsDrawn); }
    static Map<String, String> textures() { return Collections.unmodifiableMap(texturesDrawn); }
    /** The commands the page handed the server (after CampScreen.action's throttle), the last WarTablePages.SENT_KEPT. */
    static List<String> sent() { return Collections.unmodifiableList(sentCommands); }

    /** One seat as the sheet shows it. kind: human, bot, open or closed. */
    record Seat(String kind, String name, String house, String cls, boolean ready, boolean online, boolean host, boolean me) {}

    final CampScreen screen;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    long ticks;
    boolean asked;
    /** The host pressed Pass host: the other online commanders' seats now hand him the host's seat (social:host:<name>). */
    boolean choosingHost;
    /** Whether each House chip carries its seal this build (UiMath.chipRow), by House id. */
    final Map<String, Boolean> chipSeals = new HashMap<>();
    int noticeSerialSeen;
    String noticeText = "";
    long noticeAt = -1000;

    WarTableLobby(CampScreen screen) {
        this.screen = screen;
        noticeSerialSeen = CampaignClient.noticeSerial;
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
    static boolean myReady() { var m = me(); return m != null && flag(m, "ready"); }
    static String myHouse() { var m = me(); String h = m == null ? "" : str(m, "house"); return h.isEmpty() ? RosterMath.rollHouse(CampaignClient.profile.banner) : h; }
    static String myClass() { return CampaignClient.profile.commander; }
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

    /** The four seats of a side, in order: its commanders, then the seats bots will fill (bot fill on) or that stay open, then the
     *  seats beyond the room's size, closed. */
    static List<Seat> seats(int team) {
        List<Seat> out = new ArrayList<>();
        for (var r : commanders()) {
            if (!r.has("team") || r.get("team").getAsInt() != team || out.size() >= SEATS) continue;
            out.add(new Seat("human", str(r, "name"), str(r, "house"), str(r, "class"), flag(r, "ready"), !r.has("online") || flag(r, "online"), flag(r, "host"), flag(r, "me")));
        }
        boolean fill = !social().has("fill") || flag("fill");
        while (out.size() < SEATS) out.add(out.size() < size() ? new Seat(fill ? "bot" : "open", fill ? "BOT" : "Open seat", "", "", false, true, false, false) : new Seat("closed", "Closed", "", "", false, true, false, false));
        return out;
    }

    /** Builds the widgets (CampScreen.init on page lobby): again whenever the camp state changes. The first build without a room asks the server for one (social:lobby). */
    void init() {
        buttons.clear();
        if (!inRoom() && !asked) { asked = true; send("social:lobby"); }
        int[] o = columnAt();
        add("back", WarTable.backPlaque(b -> back()));
        int sx = o[0] + SHEET_X, sy = o[1] + TOP, nx = o[0] + NOTE_X, ny = o[1] + TOP;
        if (!inRoom()) {
            add("open", new UiButton(sx + (SHEET_W - 240) / 2, sy + READY_Y, 240, 40, Text.literal("Open the council"), b -> send("social:lobby"), (g, x, y, w, h, hot) -> plate(g, "open", "Open the council", true, x, y)));
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
        for (int i = 0; i < Spec.CLASSES.length; i++) {
            var c = Spec.CLASSES[i];
            var b = add("class-" + c.id(), new UiButton(sx + PAD + (TILE_W + TILE_GAP) * i, sy + CLASS_Y, TILE_W, 24, Text.literal(c.name()), x -> send("class:" + c.id()), (g, x, y, w, h, hot) -> paintClass(g, c, x, y, w, h)));
            b.active = idle;
        }
        String label = readyLabel();
        var ready = add("ready", new UiButton(sx + (SHEET_W - 240) / 2, sy + READY_Y, 240, 40, Text.literal(label), b -> { UiSounds.toggled(!myReady()); send("social:ready"); }, (g, x, y, w, h, hot) -> plate(g, "ready", readyLabel(), !myReady(), x, y)));
        ready.active = idle && !choosing && houseRefusal().isEmpty();
        var modes = modes();
        for (int i = 0; i < modes.size(); i++) {
            String id = modes.get(i);
            var b = add("mode-" + id, new UiButton(nx + NPAD + 92 * i, ny + MODE_Y + 14, 88, 24, Text.literal(modeName(id)), x -> send("social:mode:" + id), (g, x, y, w, h, hot) -> tag(g, "mode-" + id, modeName(id), id.equals(str("mode", "")), x, y, w, h)));
            b.active = host;
        }
        int cardX = nx + (NOTE_W - 139) / 2, map = num("map", 0), n = Spec.MAPS.length;
        add("map-prev", new UiButton(cardX - 28, ny + CARD_Y + 30, 24, 24, Text.literal("Previous map"), b -> send("social:map:" + Math.floorMod(map - 1, n)), (g, x, y, w, h, hot) -> tag(g, "map-prev", "<", false, x, y, w, h))).active = host;
        add("map-next", new UiButton(cardX + 139 + 4, ny + CARD_Y + 30, 24, 24, Text.literal("Next map"), b -> send("social:map:" + Math.floorMod(map + 1, n)), (g, x, y, w, h, hot) -> tag(g, "map-next", ">", false, x, y, w, h))).active = host;
        int size = size();
        add("size-down", new UiButton(nx + NPAD, ny + SIZE_Y + 14, 24, 24, Text.literal("Fewer seats"), b -> send("social:size:" + (size - 1)), (g, x, y, w, h, hot) -> tag(g, "size-down", "-", false, x, y, w, h))).active = host && size > 1;
        add("size-up", new UiButton(nx + NPAD + NOTE_TEXT_W - 24, ny + SIZE_Y + 14, 24, 24, Text.literal("More seats"), b -> send("social:size:" + (size + 1)), (g, x, y, w, h, hot) -> tag(g, "size-up", "+", false, x, y, w, h))).active = host && size < SEATS;
        add("fill", new UiButton(nx + NPAD, ny + FILL_Y + 14, NOTE_TEXT_W, 24, Text.literal(fillLabel()), b -> send("social:fill"), (g, x, y, w, h, hot) -> tag(g, "fill", fillLabel(), flag("fill"), x, y, w, h))).active = host;
        if (host) { // only the host has Pass host; it stands beside Leave, and Leave narrows to share the row
            add("pass-host", new UiButton(nx + NPAD, ny + LEAVE_Y, HOST_TAG_W, 24, Text.literal(passLabel()), b -> { choosingHost = !choosingHost; screen.rebuild(); }, (g, x, y, w, h, hot) -> tag(g, "pass-host", passLabel(), choosingHost, x, y, w, h))).active = heirs();
            add("leave", new UiButton(nx + NPAD + NOTE_TEXT_W - HOST_TAG_W, ny + LEAVE_Y, HOST_TAG_W, 24, Text.literal("Leave room"), b -> send("social:exit"), (g, x, y, w, h, hot) -> tag(g, "leave", "Leave room", false, x, y, w, h))).active = idle && !choosingHost;
        } else
            add("leave", new UiButton(nx + NPAD + (NOTE_TEXT_W - 120) / 2, ny + LEAVE_Y, 120, 24, Text.literal("Leave the room"), b -> send("social:exit"), (g, x, y, w, h, hot) -> tag(g, "leave", "Leave the room", false, x, y, w, h))).active = idle;
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

    static String fillLabel() { return flag("fill") ? "Bots fill empty seats" : "Empty seats stay empty"; }

    static String readyLabel() {
        if (flag("playing")) return "In battle";
        return myReady() ? "Stand down" : "Ready";
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
        if (CampaignClient.noticeSerial != noticeSerialSeen) {
            noticeSerialSeen = CampaignClient.noticeSerial;
            noticeText = CampaignClient.notice;
            if (!noticeText.isEmpty()) noticeAt = ticks;
        }
    }

    /** Esc goes home; Enter or Space presses the widget the keyboard's focus is on, and Enter with none presses Ready. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) { back(); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            UiButton target = null;
            if (MinecraftClient.getInstance().getNavigationType().isKeyboard()) for (var b : buttons.values()) if (b.isFocused() && b.visible) target = b;
            if (target == null && key != GLFW.GLFW_KEY_SPACE) target = buttons.get(inRoom() ? "ready" : "open");
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
            sheet(g, o[0] + SHEET_X, o[1] + TOP);
            note(g, o[0] + NOTE_X, o[1] + TOP);
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
    static Spec.ClassesRow classRow(String id) { for (var c : Spec.CLASSES) if (c.id().equals(id)) return c; return null; }

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
            line(g, "status", DarkUi.body("Opening a room for your party..."), "Opening a room for your party...", lx, y + 40, DarkUi.SEPIA);
            notice(g, lx, y);
            return;
        }
        head(g, "room", "War council", "Room " + str("room", ""), lx, y + 18, TEXT_W);
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
        var cr = classRow(myClass());
        head(g, "class", "Your class", cr == null ? "" : cr.name(), lx, y + CLASS_HEAD_Y, TEXT_W);
        status(g, x, y);
        notice(g, lx, y);
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
                    var c = classRow(seat.cls());
                    if (c != null) { String icon = "table/icon-class-" + c.id(); UiTex.draw(g, icon, right - 14, y + 5); right -= 18; }
                    String tagWord = seat.host() ? "Host" : !seat.online() ? "Away" : "";
                    if (!tagWord.isEmpty()) { int tw = font.getWidth(DarkUi.body(tagWord)); g.drawText(font, DarkUi.body(tagWord), right - tw, y + 7, DarkUi.SEPIA, false); right -= tw + 5; }
                    String name = UiMath.trim(seat.name(), right - (x + 25), s -> font.getWidth(DarkUi.body(s)));
                    g.drawText(font, DarkUi.body(name), x + 25, y + 7, ink, false);
                    if (seat.me()) g.fill(x + 25, y + 18, x + 25 + font.getWidth(DarkUi.body(name)), y + 19, DarkUi.INK_TEXT);
                    textsDrawn.put(key + "-house", seat.house());
                    textsDrawn.put(key + "-class", seat.cls());
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

    void paintClass(DrawContext g, Spec.ClassesRow c, int x, int y, int w, int h) {
        boolean on = c.id().equals(myClass());
        String tex = on ? "table/chip-house-on" : "table/chip-house";
        UiTex.nine(g, tex, x, y, w + 1, on ? h + WarTablePages.CHIP_RULE : h + 2);
        String icon = "table/icon-class-" + c.id();
        UiTex.draw(g, icon, x + (w - 14) / 2, y + 5 + (on ? 1 : 0));
        if (on) g.fill(x + 8, y + h, x + w - 8, y + h + 2, DarkUi.INK_TEXT);
        else if (WarTableRecruit.hot(buttons.get("class-" + c.id()))) underline(g, x + 8, y + h - 2, w - 16, DarkUi.INK_TEXT);
        rectsDrawn.put("class-" + c.id(), new int[]{x, y, w, h});
        texturesDrawn.put("class-" + c.id(), tex);
        if (on) textsDrawn.put("class", c.id());
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
        var t = DarkUi.heading(label);
        int tw = font.getWidth(t), lx = x + (w - tw) / 2, ink = live || on ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        g.drawText(font, t, lx, y + 6, ink, false);
        if (on) g.fill(lx, y + 18, lx + tw, y + 20, DarkUi.INK_TEXT);
        else if (WarTableRecruit.hot(b)) underline(g, lx, y + 18, tw, ink);
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
        else if (flag("preparing")) { s = "Preparing the battlefield. Stand down to stop."; ink = DarkUi.INK_TEXT; t = DarkUi.body(s); }
        else if (seconds > 0) { s = "Battle begins in " + seconds; ink = DarkUi.INK_TEXT; t = DarkUi.focal(s); textsDrawn.put("countdown", String.valueOf(seconds)); }
        else if (!houseRefusal().isEmpty()) { s = houseRefusal(); ink = DarkUi.INK_TEXT; t = DarkUi.body(s); }
        else {
            String waiting = str("waiting", "");
            s = waiting.isEmpty() ? "Everyone is ready." : waiting.equals("Every commander must ready up.") ? readyCount() : waiting;
            ink = DarkUi.SEPIA; t = DarkUi.body(s);
        }
        var font = font();
        int w = font.getWidth(t), lx = x + (SHEET_W - w) / 2;
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
        line(g, null, DarkUi.body("Map"), "Map", lx, y + MAP_Y, DarkUi.SEPIA);
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
        line(g, null, DarkUi.body("Seats a side"), "Seats a side", lx, y + SIZE_Y, DarkUi.SEPIA);
        String size = size() + " v " + size();
        WarTableHome.centred(g, DarkUi.focal(size), x + NOTE_W / 2, y + SIZE_Y + 17, DarkUi.INK_TEXT);
        textsDrawn.put("size", String.valueOf(size()));
        line(g, null, DarkUi.body("Bots"), "Bots", lx, y + FILL_Y, DarkUi.SEPIA);
        textsDrawn.put("fill", String.valueOf(flag("fill")));
    }

    /** The server's last notice, under the countdown line, shown WarTableHome.NOTICE_TICKS and then faded. */
    void notice(DrawContext g, int x, int y) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= WarTableHome.NOTICE_TICKS + WarTableHome.NOTICE_FADE_TICKS) return;
        var font = font();
        float fade = age < WarTableHome.NOTICE_TICKS ? 1 : 1 - (age - WarTableHome.NOTICE_TICKS) / (float) WarTableHome.NOTICE_FADE_TICKS;
        var lines = UiMath.lines(HintText.expand(noticeText, GravesAndGloryClient::hint), TEXT_W, 1, s -> font.getWidth(DarkUi.body(s)));
        if (lines.isEmpty()) return;
        int alpha = Math.max(4, Math.round(fade * 255)), ink = (alpha << 24) | (DarkUi.INK_TEXT & 0xffffff);
        g.drawText(font, DarkUi.body(lines.get(0)), x, y + NOTICE_Y, ink, false);
        rectsDrawn.put("notice", new int[]{x, y + NOTICE_Y, font.getWidth(DarkUi.body(lines.get(0))), 11});
        textsDrawn.put("notice", lines.get(0));
    }
}
