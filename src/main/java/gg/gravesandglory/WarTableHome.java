package gg.gravesandglory;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table home (UI spec 4.2, plan Task 5, mock v4-ui-war-table-home): the back rail, the party's seats, three order
 *  sheets, the field card and the Ride plate on the oak, with a candle and an inkwell at the table's edges. Page coordinates
 *  (the 960x540 page) are offset by WarTable.origin(); everything is drawn in canvas pixels.
 *
 *  Focus follows the pointer and the arrow keys (Kort, 2026-10-18): the sheet under the pointer, or the keyboard's choice,
 *  is the raised sealed one and sets CampaignClient.homeMode; the other two settle flat. The handover animates (the leaving
 *  sheet eases flat and the arriving one rises over UiMath.SHEET_TICKS, its seal stamps in the last 100 ms with a one-frame
 *  squash and the page-turn sound, the ribbon settles two frames behind), and the table keeps a little idle life: the candle
 *  flickers and its light breathes, the quill twitches now and then, the Ride plate glints once when it gains the focus. */
final class WarTableHome {
    // Presentation timings, in client ticks of 50 ms.
    /** Each of the candle flame's four frames. */
    static final int FLAME_TICKS = 6;
    /** One breath of the candle's light on the oak, and the light's alpha at the bottom of a breath. */
    static final int GLOW_BREATH_TICKS = 64;
    static final float GLOW_LOW = 0.8f;
    /** The quill twitches every 4 to 7 s: its tip held over for QUILL_TWITCH_TICKS, then half back for QUILL_BACK_TICKS. */
    static final int QUILL_MIN_TICKS = 80, QUILL_MAX_TICKS = 140, QUILL_TWITCH_TICKS = 2, QUILL_BACK_TICKS = 2;
    /** The ribbon settles this long behind the seal: two frames of the 20-a-second animation. */
    static final int RIBBON_LAG_TICKS = 2;
    /** How far above its place the ribbon starts, hidden under the seal. */
    static final int RIBBON_DROP = 10;
    /** The seal's squash when it lands: one frame, a little wider and lower. */
    static final int SQUASH_TICKS = 1;
    static final float SQUASH_X = 1.15f, SQUASH_Y = 0.85f;
    /** The seal's size as its stamp begins; it comes down to 1 as UiMath.sealStamp reaches 1. */
    static final float SEAL_DROP = 1.6f;
    /** The page turn under the stamp. */
    static final float PAGE_TURN_VOLUME = 0.3f;
    /** The Ride plate's glint, one sweep across its face. */
    static final int GLINT_TICKS = 8;
    /** A notice the server sends while the table is open shows under the Ride plate this long, then fades. */
    static final int NOTICE_TICKS = 100, NOTICE_FADE_TICKS = 20;

    /** The props at the table's edges (page coordinates of each frame's top left), at the kit's pixel scale (Kort, 2026-10-18
     *  late), and their solid parts {x, y, w, h} inside a frame: the flame, wax, brass, glass, quill and ink drops, not the
     *  light or the shadows. Solid parts stay 12 px inside the page and clear of the content: the candle, burned low, stands
     *  under a full party of four, the inkwell under the Last battle tag. FLAME is kit_props.FLAME_AT, where the light centres:
     *  tools/build_ui_art.py --verify fails when the two differ. */
    static final int CANDLE_X = 48, CANDLE_Y = 422, INKWELL_X = 812, INKWELL_Y = 386;
    static final int[] CANDLE_SOLID = {8, 10, 102, 96}, INKWELL_SOLID = {8, 5, 120, 137}, FLAME = {50, 26};
    /** How far inside the canvas edge a prop's solid part keeps. On a canvas smaller than the page (the default 854x480 window)
     *  a prop that would come closer is left off, with its light, rather than cut by the edge. */
    static final int PROP_EDGE = 12;

    static final String[] MODES = {"practice", "skirmish", "campaign"};
    static final String[] NAMES = {"Practice", "Skirmish", "Campaign"};
    static final String[] SUBTITLES = {"train against bots", "one battle, your rules", "the story, battle by battle"};
    /** The order sheets' centres on the page. */
    static final int[] CENTRES = {304, 480, 656};
    /** The field card's corner on the page: 8 px right of the Campaign sheet when it is raised (176 wide about 656, so its edge is
     *  at 744); the mock's 740 put the raised Campaign sheet 4 px over the card, a state the mock never drew. Change and Last battle
     *  keep their places under the card. */
    static final int FIELD_X = 752, FIELD_Y = 188;
    /** A seat's name ends this far right of its text start, clear of the raised Practice sheet. */
    static final int NAME_W = 92;
    /** The Ride plate's label is trimmed to this width (UiMath.plateLabelX keeps it clear of the Enter key). */
    static final int RIDE_LABEL_W = 175;

    /** Compact pages keep native-size paper, packing the sheets with 8 px gutters instead of cropping the field card. */
    static boolean compact() { return UiCanvas.width() < 960; }
    static int fieldX() { return compact() ? 720 : FIELD_X; }
    int sheetCentre(int index) {
        if (!compact()) return CENTRES[index];
        int left = 216;
        for (int i = 0; i < index; i++) left += (i == focused ? 176 : 152) + 8;
        return left + (index == focused ? 88 : 76);
    }

    /** A party member as the home seats them. */
    record Member(String name, boolean online, boolean me) {}
    /** A portrait drawn by the last frame: the member's name, the window's centre x and the feet y in canvas px, the model size,
     *  and whether it was the live model (false: the skin's face from the player list). */
    record PortraitDraw(String name, int x, int feet, int size, boolean model) {}

    // What the last frame drew, refilled every frame and read by the checks through the views below.
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final List<PortraitDraw> portraitsDrawn = new ArrayList<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String, String> texts = Collections.unmodifiableMap(textsDrawn);
    private static final List<PortraitDraw> portraits = Collections.unmodifiableList(portraitsDrawn);

    /** Boxes {x,y,w,h} in canvas px: floor, table, maproll and rail (as WarTable.drawChrome drew them), glow, candle (its solid part), inkwell (its solid part), seat-(i), ready-(i),
     *  invite, practice, skirmish and campaign (the sheets as drawn this frame, between their flat and raised boxes), seal,
     *  ribbon, field, last, ride-label and notice. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn: seat-(i), field-name, field-rule, field-teams, ride (the plate's label as drawn) and notice. */
    static Map<String, String> texts() { return texts; }
    static List<PortraitDraw> portraits() { return portraits; }

    final CampScreen screen;
    /** The raised sheet, 0 to 2, and the table clock (ticks plus the partial tick) when it took the focus: the seal and the
     *  ribbon run from it. */
    int focused;
    double focusStart = -1000;
    /** Per sheet: its rise when its last change began, where it is going (0 flat, 1 raised) and when that change began. */
    final float[] from = new float[3], to = new float[3];
    final double[] changedAt = {-1000, -1000, -1000};
    /** Client ticks while the table has been open. */
    long ticks;
    double stampedFor = -1000, glintAt = 0, quillAt = -1000;
    long quillNext;
    /** The server's notice serial the table has shown, the notice it showed and the tick it appeared: a notice shows when the
     *  serial moves, so the same text sent again shows again, and one already waiting when the table opened stays hidden. */
    int noticeSerialSeen;
    String noticeText = "";
    long noticeAt = -1000;
    int builtFriends = -1;
    final Random random = new Random();
    final UiButton[] sheets = new UiButton[3];
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    UiButton ride;

    WarTableHome(CampScreen screen) {
        this.screen = screen;
        focused = Math.max(0, Arrays.asList(MODES).indexOf(CampaignClient.homeMode));
        for (int i = 0; i < 3; i++) from[i] = to[i] = i == focused ? 1 : 0;
        noticeSerialSeen = CampaignClient.noticeSerial;
        quillNext = nextTwitch();
    }

    long nextTwitch() { return ticks + QUILL_MIN_TICKS + random.nextInt(QUILL_MAX_TICKS - QUILL_MIN_TICKS + 1); }

    /** The table's own clock: client ticks while open plus the partial tick. */
    double clock() { return ticks + MinecraftClient.getInstance().getTickDelta(); }

    /** The widget a name stands for (camp, recruit, banner, contracts, chapter, friends, invite, practice, skirmish, campaign,
     *  change, ride, last), or null. */
    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    /** Builds the widgets (CampScreen.init on page home): again whenever the camp state or a badge changes. */
    void init() {
        buttons.clear();
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        builtFriends = friendsBadge();
        add("camp", WarTable.plaque(56, 96, "icon-camp", "Camp", "Esc", 61, 0, b -> screen.close()));
        add("recruit", WarTable.plaque(184, 112, "icon-recruit", "Recruit", null, 0, 0, b -> screen.page("recruit")));
        add("banner", WarTable.plaque(304, 112, "icon-banner", "Banner", null, 0, 0, b -> screen.page("banner")));
        add("contracts", WarTable.plaque(424, 112, "icon-contracts", "Contracts", null, 0, contractsBadge(), b -> screen.page("contracts")));
        add("chapter", WarTable.plaque(544, 112, "icon-chapter", "Chapter", null, 0, 0, b -> screen.page("chapter")));
        add("friends", WarTable.plaque(664, 112, "icon-friends", "Friends", null, 0, builtFriends, b -> screen.page("friends")));
        int seated = members().size();
        if (seated < 4) add("invite", new UiButton(ox + 56, oy + 206 + 56 * seated, 104, 48, Text.literal("Invite"), b -> screen.page("friends"), this::paintInvite));
        for (int i = 0; i < 3; i++) {
            final int index = i;
            sheets[i] = add(MODES[i], new UiButton(0, 0, 1, 1, Text.literal(NAMES[i]), b -> focus(index), (g, x, y, w, h, hot) -> {}));
        }
        placeSheets();
        add("change", new UiButton(ox + fieldX() + 44, oy + FIELD_Y + 152, 80, 24, Text.literal("Change"), b -> screen.page("field"), this::paintChange));
        ride = add("ride", new UiButton(ox + 360, oy + 388, 240, 40, Text.literal(rideLabel()), b -> ride(), this::paintRide));
        ride.active = !GravesAndGloryClient.running;
        if (CampaignClient.result.has("id")) add("last", new UiButton(ox + fieldX() + 40, oy + 392, 88, 24, Text.literal("Last battle"), b -> screen.page("results"), this::paintLast));
    }

    /** The focused sheet's widget takes its raised box and the others their flat boxes, at once (the drawing animates). */
    void placeSheets() {
        int[] o = WarTable.origin();
        for (int i = 0; i < 3; i++) {
            if (sheets[i] == null) continue;
            int c = sheetCentre(i);
            if (i == focused) sheets[i].moveTo(o[0] + c - 88, o[1] + 188, 176, 160);
            else sheets[i].moveTo(o[0] + c - 76, o[1] + 200, 152, 136);
        }
    }

    /** Raises sheet i: the pointer over it, an arrow key, a click or the keyboard focus on it. */
    void focus(int i) {
        if (i == focused || i < 0 || i > 2) return;
        double now = clock();
        int old = focused; // only the two sheets whose change restarts are rebased; a third one still easing keeps its curve
        from[old] = rise(old, now); from[i] = rise(i, now);
        to[old] = 0; changedAt[old] = now;
        to[i] = 1; changedAt[i] = now;
        focused = i; focusStart = now;
        CampaignClient.homeMode = MODES[i];
        placeSheets();
        if (ride != null) ride.setMessage(Text.literal(rideLabel()));
    }

    /** How far sheet k is raised at time now, 0 flat to 1 raised, eased by UiMath.sheetRise from where its last change began. */
    float rise(int k, double now) {
        double since = Math.max(0, now - changedAt[k]);
        long whole = (long) Math.floor(since);
        return from[k] + (to[k] - from[k]) * UiMath.sheetRise(whole, (float) (since - whole));
    }

    static float stamp(double since) {
        if (since < 0) return 0;
        long whole = (long) Math.floor(since);
        return UiMath.sealStamp(whole, (float) (since - whole));
    }

    /** The party as the seats show it: the local player first, then the other members in the snapshot's order, at most four. */
    static List<Member> members() {
        var c = MinecraftClient.getInstance();
        List<Member> out = new ArrayList<>();
        out.add(new Member(c.player != null ? c.player.getGameProfile().getName() : c.getSession().getUsername(), true, true));
        if (CampaignClient.social.has("members"))
            for (var e : CampaignClient.social.getAsJsonArray("members")) {
                var row = e.getAsJsonObject();
                if (row.has("me") && row.get("me").getAsBoolean()) continue;
                out.add(new Member(row.get("name").getAsString(), row.has("online") && row.get("online").getAsBoolean(), false));
            }
        return out.subList(0, Math.min(4, out.size()));
    }

    /** The Claim buttons the Contracts page would show: daily contracts done and not claimed, and the weekly once it can be. */
    static int contractsBadge() {
        var p = CampaignClient.profile.progression;
        int n = 0;
        for (int i = 0; i < 3; i++) if (Progression.claimable(p, "daily:" + i)) n++;
        if (Progression.claimable(p, "weekly")) n++;
        return n;
    }

    /** 1 while an invitation waits (the party's or the camp's), else 0. */
    static int friendsBadge() {
        boolean party = CampaignClient.party.has("inviteSeconds") && CampaignClient.party.get("inviteSeconds").getAsInt() > 0;
        boolean social = CampaignClient.social.has("inviteSeconds") && CampaignClient.social.get("inviteSeconds").getAsInt() > 0;
        return party || social ? 1 : 0;
    }

    /** The Long March battle the Campaign sheet rides to: the one after the last cleared, or the last once all are cleared. */
    static Spec.CampaignBattlesRow next() {
        int want = CampaignClient.profile.campaignCleared + 1;
        for (var row : Spec.CAMPAIGNBATTLES) if (row.order() == want) return row;
        return Spec.CAMPAIGNBATTLES[Spec.CAMPAIGNBATTLES.length - 1];
    }

    String rideLabel() {
        if (GravesAndGloryClient.running) return "A battle is under way";
        return switch (focused) { case 0 -> "Begin practice"; case 2 -> "Ride to " + next().name(); default -> "Ride to the war gates"; };
    }

    /** The Ride plate: practice starts at once, the war gates set the commander on the ready pad and close the table, and the
     *  Long March prepares its battle's map first. */
    void ride() {
        if (GravesAndGloryClient.running) return;
        switch (focused) {
            case 0 -> screen.action("camp:practice");
            case 2 -> screen.action("campaign:" + next().id());
            default -> { screen.action("camp:gates"); screen.close(); }
        }
    }

    void tick() {
        ticks++;
        if (friendsBadge() != builtFriends) screen.rebuild();
        if (CampaignClient.noticeSerial != noticeSerialSeen) {
            noticeSerialSeen = CampaignClient.noticeSerial; noticeText = CampaignClient.notice;
            if (!noticeText.isEmpty()) noticeAt = ticks;
        }
        if (ticks >= quillNext) { quillAt = ticks; quillNext = nextTwitch(); }
    }

    /** The pointer over a sheet raises it; nothing else on the table moves under the pointer. */
    void mouseMoved(double x, double y) {
        for (int i = 0; i < 3; i++) if (sheets[i] != null && sheets[i].overArt(x, y)) { focus(i); return; }
    }

    /** Left and Right move the raised sheet; Enter presses the Ride plate, whatever holds the keyboard focus. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_LEFT) { focus(Math.max(0, focused - 1)); return true; }
        if (key == GLFW.GLFW_KEY_RIGHT) { focus(Math.min(2, focused + 1)); return true; }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (ride != null && ride.active) { ride.playDownSound(MinecraftClient.getInstance().getSoundManager()); ride.onPress(); }
            return true;
        }
        return false;
    }

    /** The keyboard focus moved (Tab, the arrows' navigation or a click): onto a sheet it raises it, onto the Ride plate it glints. */
    void focusChanged(Element e) {
        for (int i = 0; i < 3; i++) if (e != null && e == sheets[i]) focus(i);
        if (e != null && e == ride) glintAt = clock();
    }

    void render(DrawContext g) {
        rectsDrawn.clear(); textsDrawn.clear(); portraitsDrawn.clear();
        double now = clock();
        UiCanvas.begin(g);
        try {
            int[] o = WarTable.origin();
            int ox = o[0], oy = o[1];
            WarTable.drawChrome(g, "home");
            rectsDrawn.putAll(WarTable.drawn()); // floor, table, maproll and rail, as drawChrome drew them
            props(g, ox, oy, now);
            party(g, ox, oy);
            field(g, ox + fieldX(), oy + FIELD_Y);
            g.draw();
            Integer[] order = {0, 1, 2};
            float[] r = {rise(0, now), rise(1, now), rise(2, now)};
            Arrays.sort(order, Comparator.comparingDouble(k -> r[k]));
            for (int k : order) { sheet(g, k, ox, oy, r[k], now); g.draw(); }
            notice(g, ox, oy);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** The candle's light under everything, breathing; the candle on its flame frame; the inkwell on its quill frame. Each prop
     *  draws only where its solid part keeps PROP_EDGE inside the canvas. */
    void props(DrawContext g, int ox, int oy, double now) {
        var glow = UiTex.entry("table/prop-glow"); // the light's size is the atlas's
        int cx = ox + CANDLE_X, cy = oy + CANDLE_Y, gx = cx + FLAME[0] - glow.w() / 2, gy = cy + FLAME[1] - glow.h() / 2;
        int[] candle = {cx + CANDLE_SOLID[0], cy + CANDLE_SOLID[1], CANDLE_SOLID[2], CANDLE_SOLID[3]};
        int[] inkwell = {ox + INKWELL_X + INKWELL_SOLID[0], oy + INKWELL_Y + INKWELL_SOLID[1], INKWELL_SOLID[2], INKWELL_SOLID[3]};
        if (inside(candle)) {
            float breath = GLOW_LOW + (1 - GLOW_LOW) * (float) (0.5 + 0.5 * Math.sin(2 * Math.PI * now / GLOW_BREATH_TICKS));
            RenderSystem.setShaderColor(1, 1, 1, breath);
            try { UiTex.draw(g, "table/prop-glow", gx, gy); } finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
            UiTex.frame(g, "table/prop-candle", (int) (ticks / FLAME_TICKS) % 4, cx, cy);
            rectsDrawn.put("glow", new int[]{gx, gy, glow.w(), glow.h()});
            rectsDrawn.put("candle", candle);
        }
        if (inside(inkwell)) {
            double since = ticks - quillAt;
            int quill = since < QUILL_TWITCH_TICKS ? 1 : since < QUILL_TWITCH_TICKS + QUILL_BACK_TICKS ? 2 : 0;
            UiTex.frame(g, "table/prop-inkwell", quill, ox + INKWELL_X, oy + INKWELL_Y);
            rectsDrawn.put("inkwell", inkwell);
        }
    }

    /** Whether a solid box {x, y, w, h} keeps PROP_EDGE inside the canvas. */
    static boolean inside(int[] b) {
        return b[0] >= PROP_EDGE && b[1] >= PROP_EDGE && b[0] + b[2] <= UiCanvas.width() - PROP_EDGE && b[1] + b[3] <= UiCanvas.height() - PROP_EDGE;
    }

    /** "Party" over a seat per member, the live model in each window; the Invite seat is a widget (paintInvite). */
    void party(DrawContext g, int ox, int oy) {
        var c = MinecraftClient.getInstance();
        var font = c.textRenderer;
        g.drawText(font, DarkUi.heading("Party"), ox + 56, oy + 186, DarkUi.BRASS, false);
        var seated = members();
        for (int i = 0; i < seated.size(); i++) {
            var m = seated.get(i);
            int x = ox + 56, y = oy + 206 + 56 * i;
            UiTex.draw(g, "table/frame-back-48", x + 4, y + 4);
            LivingEntity e = m.me() ? c.player : null;
            if (!m.me() && c.world != null) for (var p : c.world.getPlayers()) if (p.getGameProfile().getName().equals(m.name())) e = p;
            if (e != null) {
                Portrait.draw(g, e, x + 4, y + 4, 40, 0);
                var d = Portrait.last(); // what reached drawEntity
                portraitsDrawn.add(new PortraitDraw(m.name(), d.x(), d.feet(), d.size(), true));
            } else {
                var entry = c.getNetworkHandler() == null ? null : c.getNetworkHandler().getPlayerListEntry(m.name());
                if (entry != null) {
                    PlayerSkinDrawer.draw(g, entry.getSkinTexture(), x + 4, y + 4, 40);
                    portraitsDrawn.add(new PortraitDraw(m.name(), x + 24, y + 44, 40, false));
                }
            }
            UiTex.draw(g, "table/frame-rim-48", x, y);
            String name = UiMath.trim(m.name(), NAME_W, s -> font.getWidth(DarkUi.heading(s)));
            g.drawText(font, DarkUi.heading(name), x + 60, y + 12, DarkUi.IVORY, false);
            if (m.online()) { UiTex.draw(g, "table/ready-check", x + 60, y + 29); rectsDrawn.put("ready-" + i, new int[]{x + 60, y + 29, 9, 8}); }
            rectsDrawn.put("seat-" + i, new int[]{x, y, 48, 48});
            textsDrawn.put("seat-" + i, name);
        }
    }

    void paintInvite(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "table/frame-back-empty-48", x + 4, y + 4);
        UiTex.draw(g, "table/frame-rim-48", x, y);
        UiTex.draw(g, "table/invite-plus", x + 20, y + 20);
        g.drawText(font, DarkUi.heading("Invite"), x + 60, y + 18, hot ? DarkUi.IVORY : DarkUi.MUTED, false);
        rectsDrawn.put("invite", new int[]{x, y, w, h});
    }

    /** The field card: the map the Ride plate goes to (the next Long March battle's on the Campaign sheet), the rule and the
     *  sides, and Change (a widget, paintChange). */
    void field(DrawContext g, int x, int y) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "table/field-card", x, y);
        UiTex.draw(g, "table/tack", x + 6, y + 4);
        UiTex.draw(g, "table/tack", x + 155, y + 4);
        int index = Math.max(0, Math.min(Spec.MAPS.length - 1, focused == 2 ? next().map() : CampaignClient.map));
        var map = Spec.MAPS[index];
        centred(g, DarkUi.heading(map.name()), x + 84, y + 12, DarkUi.INK_TEXT);
        if (map.id().equals("rivergate")) { // the art's seals, lettered as the mock letters them
            UiTex.draw(g, "table/map-rivergate", x + 16, y + 30);
            g.drawText(font, "A", x + 63, y + 46, DarkUi.CHIP_TEXT, false);
            g.drawText(font, "B", x + 81, y + 67, DarkUi.CHIP_TEXT, false);
            g.drawText(font, "C", x + 100, y + 88, DarkUi.CHIP_TEXT, false);
        } else if (index == CampaignMaps.selected) { // bare card: a seal on each point, centred where the point lies on the field
            for (var p : Spec.POINTS) {
                int px = x + 16 + (p.x() + 350) * 136 / 700, py = y + 30 + (p.z() + 250) * 80 / 500;
                UiTex.draw(g, "table/map-seal", px - 8, py - 8);
                g.drawText(font, p.id(), px - font.getWidth(p.id()) / 2 + 1, py - 3, DarkUi.CHIP_TEXT, false);
            }
        }
        int n = switch (focused) { case 0 -> 1; case 2 -> next().size(); default -> CampaignClient.padSize; };
        String tier = CampaignClient.tierName(focused == 2 ? next().difficulty() : CampaignClient.difficulty);
        String rule = "Domination to " + GravesAndGlory.R.winScore(), teams = n + " v " + n + ", " + tier + (n == 1 ? " bot" : " bots");
        centred(g, DarkUi.body(rule), x + 84, y + 120, DarkUi.SEPIA);
        centred(g, DarkUi.body(teams), x + 84, y + 132, DarkUi.SEPIA);
        rectsDrawn.put("field", new int[]{x, y, 168, 188});
        textsDrawn.put("field-name", map.name());
        textsDrawn.put("field-rule", rule);
        textsDrawn.put("field-teams", teams);
    }

    static int centred(DrawContext g, Text t, int cx, int top, int color) {
        var font = MinecraftClient.getInstance().textRenderer;
        int w = font.getWidth(t), x = cx - w / 2;
        g.drawText(font, t, x, top, color, false);
        return x;
    }

    /** Sheet i at rise r: between its flat box (order-lo, the name a heading) and its raised one (order-hi, the name the focal
     *  line), the raised art scaled up from the flat size while the flat art fades off it. The focused sheet carries the ribbon
     *  and the seal once the stamp begins. */
    void sheet(DrawContext g, int i, int ox, int oy, float r, double now) {
        var font = MinecraftClient.getInstance().textRenderer;
        int c = sheetCentre(i);
        float fx = lerp(c - 76, c - 88, r) + ox, fy = lerp(200, 188, r) + oy, fw = lerp(152, 176, r), fh = lerp(136, 160, r);
        if (compact()) {
            fx = ox + 216;
            for (int k = 0; k < i; k++) fx += lerp(152, 176, rise(k, now)) + 8;
        }
        int bx = Math.round(fx), by = Math.round(fy), bw = Math.round(fw), bh = Math.round(fh);
        String flat = i == 2 ? "table/order-lo2" : "table/order-lo";
        if (r <= 0) UiTex.draw(g, flat, bx, by);
        else if (r >= 1) UiTex.draw(g, "table/order-hi", bx, by);
        else {
            scaled(g, "table/order-hi", fx, fy, fw / 176f, fh / 160f, 1f);
            scaled(g, flat, fx, fy, fw / 152f, fh / 136f, 1f - r);
        }
        String ink = switch (i) { case 0 -> "table/ink-practice"; case 1 -> "table/ink-skirmish"; default -> "table/ink-campaign"; };
        var art = UiTex.entry(ink);
        int mid = bx + bw / 2;
        UiTex.draw(g, ink, mid - art.w() / 2, by + Math.round(lerp(88, 112, r)) - art.h());
        centred(g, r < 0.5f ? DarkUi.heading(NAMES[i]) : DarkUi.focal(NAMES[i]), mid, by + Math.round(lerp(94, 116, r)), DarkUi.INK_TEXT);
        int sw = font.getWidth(SUBTITLES[i]);
        g.drawText(font, SUBTITLES[i], mid - sw / 2, by + Math.round(lerp(112, 138, r)), DarkUi.SEPIA, false);
        rectsDrawn.put(MODES[i], new int[]{bx, by, bw, bh});
        if (i != focused) return;
        double since = now - focusStart;
        float ribbon = stamp(since - RIBBON_LAG_TICKS), seal = stamp(since);
        if (ribbon > 0) {
            int ry = by + 4 - Math.round((1 - ribbon) * RIBBON_DROP);
            RenderSystem.setShaderColor(1, 1, 1, ribbon);
            try { UiTex.draw(g, "table/ribbon-teal", bx + 78, ry); } finally { RenderSystem.setShaderColor(1, 1, 1, 1); }
            rectsDrawn.put("ribbon", new int[]{bx + 78, ry, 20, 28});
        }
        if (seal > 0) {
            int sx = bx + 71, sy = by - 17;
            if (since >= UiMath.SHEET_TICKS && since < UiMath.SHEET_TICKS + SQUASH_TICKS) // the landing frame, squashed on its base
                scaled(g, "table/wax-crown", sx + 18.5f * (1 - SQUASH_X), sy + 38 * (1 - SQUASH_Y), SQUASH_X, SQUASH_Y, 1f);
            else if (seal < 1) { // coming down: larger and fainter, centred on its place
                float s = SEAL_DROP - (SEAL_DROP - 1) * seal;
                scaled(g, "table/wax-crown", sx + 18.5f * (1 - s), sy + 19 * (1 - s), s, s, seal);
            } else UiTex.draw(g, "table/wax-crown", sx, sy);
            rectsDrawn.put("seal", new int[]{sx, sy, 37, 38});
            if (since >= UiMath.SHEET_TICKS && stampedFor != focusStart) {
                stampedFor = focusStart;
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1f, PAGE_TURN_VOLUME));
            }
        }
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** A texture drawn at (x, y) scaled by (sx, sy) about that corner, at the given alpha. */
    static void scaled(DrawContext g, String name, float x, float y, float sx, float sy, float alpha) {
        var m = g.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(sx, sy, 1);
        RenderSystem.setShaderColor(1, 1, 1, alpha);
        try { UiTex.draw(g, name, 0, 0); } finally { RenderSystem.setShaderColor(1, 1, 1, 1); m.pop(); }
    }

    void paintChange(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "table/btn-vellum-80x24", x, y);
        UiTex.draw(g, "table/quill", x + 8, y + 6);
        var label = DarkUi.heading("Change");
        g.drawText(font, label, x + 24, y + 6, DarkUi.INK_TEXT, false);
        if (hot) g.fill(x + 24, y + 18, x + 24 + font.getWidth(label), y + 19, DarkUi.INK_TEXT);
    }

    void paintLast(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        UiTex.draw(g, "table/btn-vellum-88x24", x, y);
        var label = DarkUi.heading("Last battle");
        int lx = centred(g, label, x + 44, y + 6, DarkUi.INK_TEXT);
        if (hot) g.fill(lx, y + 18, lx + font.getWidth(label), y + 19, DarkUi.INK_TEXT);
        rectsDrawn.put("last", new int[]{x, y, w, h});
        textsDrawn.put("last", "Last battle");
    }

    /** The one primary action: brass with the Enter key and the label, or iron and idle while a battle runs. */
    void paintRide(DrawContext g, int x, int y, int w, int h, boolean hot) {
        var font = MinecraftClient.getInstance().textRenderer;
        if (GravesAndGloryClient.running) {
            UiTex.draw(g, "table/btn-iron-240x40", x, y);
            String idle = "A battle is under way";
            int lx = centred(g, DarkUi.heading(idle), x + 120, y + 14, DarkUi.IVORY);
            textsDrawn.put("ride", idle);
            rectsDrawn.put("ride-label", new int[]{lx, y + 14, font.getWidth(DarkUi.heading(idle)), 11});
            return;
        }
        UiTex.draw(g, "table/btn-brass-240x40", x, y);
        UiTex.draw(g, "table/key-brass-37", x + 14, y + 13);
        g.drawText(font, "Enter", x + 18, y + 16, DarkUi.KEY_INK, false);
        String label = UiMath.trim(rideLabel(), RIDE_LABEL_W, s -> font.getWidth(DarkUi.heading(s)));
        int tw = font.getWidth(DarkUi.heading(label)), lx = x + UiMath.plateLabelX(tw);
        g.drawText(font, DarkUi.heading(label), lx, y + 14, DarkUi.PLATE_INK, false);
        if (hot) g.fill(lx, y + 26, lx + tw, y + 27, DarkUi.PLATE_INK);
        textsDrawn.put("ride", label);
        rectsDrawn.put("ride-label", new int[]{lx, y + 14, tw, 11});
        double glint = clock() - glintAt;
        if (glint >= 0 && glint < GLINT_TICKS) { // one sweep of light across the face, cut to it
            int gx = x - 22 + (int) Math.round((w + 22) * glint / GLINT_TICKS);
            UiCanvas.scissor(g, x + 3, y + 3, w - 6, h - 6);
            try { UiTex.draw(g, "table/plate-glint", gx, y + 2); } finally { UiCanvas.endScissor(g); }
        }
    }

    /** A notice the server sent while the table is open (a refusal, the map being prepared), under the Ride plate. */
    void notice(DrawContext g, int ox, int oy) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= NOTICE_TICKS + NOTICE_FADE_TICKS) return;
        var font = MinecraftClient.getInstance().textRenderer;
        float fade = age < NOTICE_TICKS ? 1 : 1 - (age - NOTICE_TICKS) / (float) NOTICE_FADE_TICKS;
        String text = UiMath.trim(HintText.expand(noticeText, GravesAndGloryClient::hint), 440, s -> font.getWidth(DarkUi.body(s)));
        int alpha = Math.max(4, Math.round(fade * 255));
        int x = centred(g, DarkUi.body(text), ox + 480, oy + 440, (alpha << 24) | (DarkUi.IVORY & 0xffffff));
        textsDrawn.put("notice", text);
        rectsDrawn.put("notice", new int[]{x, oy + 440, font.getWidth(DarkUi.body(text)), 11});
    }
}
