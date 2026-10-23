package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table's other pages (UI plan Task 7), each on its own stretch of the oak under the back rail: Banner (the owned roll,
 *  the twenty deployment seats on their sheet and the commander's class), Contracts (the three dailies, the reroll and the
 *  weekly), Chapter (the Founders reward track, as Kort approved it on 2026-10-18: the hero letter, the chapter block with the XP
 *  rail and the Claim plate, and the track of plaques in two rows), Friends (the camp's commanders, the party, invitations and
 *  the private room) and the Field (the map vote with each map's painted card, and the bots' tier). Each page has the back plaque
 *  (Esc) and the rail's other plaques; the open page's plaque is drawn lit and takes no press. Every page but the Chapter carries
 *  one focal title in IVORY with its shadow, centred on x 480 at top 84; the Chapter is named by its chapter block ("Chapter 1",
 *  "The Founders"), where the approved layout puts it. Every press sends the command the old camp pages sent, through
 *  CampScreen.action's throttle; a notice the server sends back shows on the page's notice line.
 *
 *  Page coordinates (the 960x540 page) are offset by WarTable.origin(), the rail's plaques by its x only; everything is drawn in
 *  canvas pixels. What the last frame drew is kept for the checks, as the home and recruit pages keep theirs. */
final class WarTablePages {
    /** The pages this class draws. The field has no plaque on the rail: home's Change opens it. */
    static final List<String> NAMES = List.of("banner", "contracts", "chapter", "friends", "field");
    /** Each page's focal title (the Chapter's is its chapter block). */
    static final Map<String, String> TITLES = Map.of("banner", "Your Banner", "contracts", "Contracts", "chapter", "Founders Chapter", "friends", "Friends", "field", "The Field");
    static final int TITLE_X = 480, TITLE_Y = 84;
    // The rail's camp plaques are home's (WarTableHome.RAIL, RAIL_X, RAIL_LABELS, RAIL_W): one source, no copy here.
    /** The notice line: its first line's top, 12 px under the lowest content of every page; its lines 13 px apart, centred on the
     *  page, as wide as home's and at most two (the second ends 12 px above the page's foot). On the Chapter it is right-aligned
     *  to the track's right end at top 500, opposite the Oath's line. */
    static final int NOTICE_Y = 504, NOTICE_STEP = 13, NOTICE_W = 440, NOTICE_LINES = 2;
    static final int CHAPTER_NOTICE_R = 888, CHAPTER_NOTICE_Y = 500, CHAPTER_NOTICE_W = 400;
    static final String VOTE_SAVED = "Vote saved: ";

    // Banner: the roll (recruit/muster-roll, rows as the recruit page has them), each row's Add and Remove tags, the sheet and its
    // 5 x 4 seats (front row first), the armed troop's band under the seats, and the commander classes on the right.
    static final int ROLL_X = 56, ROLL_Y = 124, ROW_TOP = 140, ROW_H = 56, ROWS = 6, ROW_W = 168, NAME_W = 104;
    static final int TAG_X = 228, TAG_W = 60, TAG_H = 22, ADD_DY = 4, REMOVE_DY = 30;
    static final int SHEET_X = 328, SHEET_Y = 124, SEAT_X = 388, SEAT_Y = 180, SEAT_STEP = 56, SEAT = 48, SEATS = 20;
    static final int ARMED_X = 388, ARMED_Y = 412, CLEAR_X = 596, CLEAR_Y = 424;
    static final int CLASS_X = 760, CLASS_Y = 140, CLASS_STEP = 40, CLASS_W = 120, BONUS_Y = 264, BONUS_W = 144;
    /** What a seat click places when the Clear tag is armed: the server's word for an empty seat. */
    static final String EMPTY = "empty";
    /** In the roll's last place, at its foot, while the House has troops not yet recruited (their places ruled and empty above
     *  it): where they come from, centred on the rows. */
    static final String ROLL_HINT = "More of your House's troops join the roll at the recruit table.";
    static final int ROLL_HINT_CX = 116, ROLL_HINT_W = 200;

    // Contracts: the three daily notes in a row centred on the page, Reroll under the middle one, the weekly note under it.
    static final int NOTE_X = 140, NOTE_Y = 140, NOTE_STEP = 240, REROLL_X = 436, REROLL_Y = 252, WEEKLY_X = 328, WEEKLY_Y = 300, RESET_Y = 412;

    // Chapter (work/ui-concepts/track/pages.py): ten tiers shown, plaques 68 x 80 on a 76 pitch, the current tier's Roll plaque
    // lifted 8 px and 88 tall; the rod, the tier tags on it, the Roll's top and the Oath's top; the hero letter and the Claim plate.
    static final int TIERS = Progression.TIERS, SHOWN = 10, EVERY = 5;
    static final int X0 = 136, PITCH = 76, PW = 68, PH = 80, RH = 88, ROD_Y = 296, TAG_Y = 294, ROLL_TOP = 320, OATH_TOP = 408;
    static final int HERO_X = 56, HERO_Y = 72, CLAIM_X = 664, CLAIM_Y = 232;
    /** After a claim the track slides one plaque left over this many ticks, easing out. */
    static final int SLIDE_TICKS = 6;
    /** One line of copy for each title on the hero letter. */
    static final Map<String, String> TITLE_COPY = Map.of("Scout", "First out, first to see the field.", "Pathfinder", "You find the road the others lost.",
        "Captain", "They hold because you hold.", "Vanguard", "First into the line, last out of it.", "Veteran", "Old scars, steady hands.",
        "Bannerlord", "The banner answers to you.");
    /** The Oath's reward on each odd tier (NOTES.md, "The Oath row"): the icon's stem, the name and the second line. The even tiers
     *  carry shards, but the portrait frame at 10, the cloak at 20 and the Oathsworn title at 30 (oath). */
    static final Map<Integer, String[]> OATH_ODD = Map.ofEntries(
        Map.entry(1, new String[]{"pennant-fess", "Pennant", "Fess"}), Map.entry(3, new String[]{"pennant-chief", "Pennant", "Chief"}),
        Map.entry(5, new String[]{"pennant-bordure", "Pennant", "Bordure"}), Map.entry(7, new String[]{"pennant-saltire", "Pennant", "Saltire"}),
        Map.entry(9, new String[]{"pennant-quarterly", "Pennant", "Quarterly"}), Map.entry(11, new String[]{"tabard", "Tabard", "livery"}),
        Map.entry(13, new String[]{"pennant-pale", "Pennant", "pattern"}), Map.entry(15, new String[]{"cloth", "Banner", "cloth"}),
        Map.entry(17, new String[]{"pennant-bend", "Pennant", "pattern"}), Map.entry(19, new String[]{"pennant-cross", "Pennant", "pattern"}),
        Map.entry(21, new String[]{"cloth-gold", "Banner", "cloth"}), Map.entry(23, new String[]{"pennant-gyronny", "Pennant", "pattern"}),
        Map.entry(25, new String[]{"cloth-crowns", "Banner", "cloth"}), Map.entry(27, new String[]{"pennant-pily", "Pennant", "pattern"}),
        Map.entry(29, new String[]{"cloth-night", "Banner", "cloth"}));
    /** Under the Oath row while no House takes oaths (Phase 1): the honest line in place of the mock's "Swear to a House". */
    static final String UNSWORN = "No House takes oaths yet";

    // Friends: the camp's commanders on the left, the party's note in the middle with a seat for each of its four places, the
    // private room's note on the right: out of a room the short note (as tall as the party's), with Create and the code's line
    // under "Join a room"; in one the tall note, its tags in groups 16 px apart (the code's, the teams', the settings', the start)
    // and the rows in a group 8 px apart. Offsets from the note's top.
    static final int CAMP_X = 56, CAMP_W = 228, INVITE_Y = 144, INVITE_STEP = 32, INVITES = 4, PARTY_SEATS = 4;
    static final int PARTY_X = 300, PARTY_Y = 124, PARTY_NAME_W = 108, ROOM_X = 676, ROOM_Y = 124, ROOM_W = 196, PAIR_W = 94, PAIR_STEP = 102, CODE_LENGTH = 8;
    static final int CREATE_DY = 76, JOIN_HEAD_DY = 128, CODE_DY = 146, JOIN_DY = 178, RULES_DY = 226;
    static final int COPY_DY = 66, TEAMS_DY = 106, NAMES_DY = 122, TEAM_DY = 176, MAP_DY = 216, SIZE_DY = 234, FILL_DY = 266, READY_DY = 306, READY_LINE_DY = 338;
    /** The party's rule, beside the seats level with the first (the commander's own, which never carries a tag) and on the
     *  column of the leader's tags, and the camp column's line on how friends reach a camp this machine runs: how to share it,
     *  or, once it is open on the network, where friends find it. */
    static final int RULE_X = 188, RULE_W = 156;
    static final String PARTY_RULE = "A party of up to four friends always fights on one team.";
    /** Under the seats while in a party. */
    static final String PARTY_LASTS = "The party lasts through matches and restarts.";
    static final String CAMP_HOW = "To play together, host a camp on this network from Play with friends on the title screen; friends pick Join a server there.";
    static final String CAMP_OPEN = "This camp is open on your network: friends pick Play with friends, then Join a server, on their title screen.";

    // Field: the sheet; on its left a row per map (its name, then Vote) and under them the war gates' line and the vote's rule;
    // on its right the card of the map in view with its name and description, and under them the bots' tier, its three tags
    // centred under the card on the band of the war gates' lines. Both columns start 32 px under the sheet's top and end on
    // that band, 40 px above its foot.
    static final int FIELD_X = 220, FIELD_Y = 124, MAP_X = 236, MAP_Y = 156, MAP_STEP = 44, MAP_W = 176, VOTE_X = 420, STATUS_Y = 380;
    static final int CARD_X = 544, CARD_Y = 156, CARD_CX = 612, CARD_TEXT_W = 208, BOTS_HEAD_Y = 362, BOTS_Y = STATUS_Y;
    static final String[] TIER_IDS = {"easy", "normal", "hard"};
    static final int[] TIER_X = {504, 576, 656}, TIER_W = {64, 72, 64};

    /** A portrait the last frame drew: where (row-(i), seat-(i), armed-seat, hero, member-(i)), whose (a troop id or a name) and
     *  what Portrait.draw passed to drawEntity. */
    record PortraitDraw(String where, String who, Portrait.Drawn drawn) {}
    /** A member of the party as the Friends note seats them. */
    record Seat(String name, boolean online, boolean me, boolean leader) {}

    // What the last frame drew, refilled every frame and read by the checks through the views below.
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> textsDrawn = new LinkedHashMap<>();
    private static final Map<String, String> texturesDrawn = new LinkedHashMap<>();
    private static final Map<String, Integer> colorsDrawn = new LinkedHashMap<>();
    private static final List<PortraitDraw> portraitsDrawn = new ArrayList<>();
    private static final Set<String> underlinedDrawn = new LinkedHashSet<>();
    private static final List<String> sentCommands = new ArrayList<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    private static final Map<String, String> texts = Collections.unmodifiableMap(textsDrawn);
    private static final Map<String, String> textures = Collections.unmodifiableMap(texturesDrawn);
    private static final Map<String, Integer> colors = Collections.unmodifiableMap(colorsDrawn);
    private static final List<PortraitDraw> portraits = Collections.unmodifiableList(portraitsDrawn);
    private static final Set<String> underlined = Collections.unmodifiableSet(underlinedDrawn);
    private static final List<String> sent = Collections.unmodifiableList(sentCommands);

    /** Boxes {x,y,w,h} in canvas px as drawn: floor, table and rail (drawChrome's), here (the lit plaque), title, notice; Banner:
     *  roll, mark, row-(i), roll-hint, sheet, seat-(i), armed-seat, class-(id); Contracts: note-(i), progress-(i) (the count),
     *  stamp-(i), weekly, progress-weekly, stamp-weekly, reroll; Chapter: hero, seal, xp-rail, roll-(t) and oath-(t) for each
     *  tier on the track (t the tier), claim; Friends: party, member-(i), party-rule, room, room-why, code; Field: sheet, card, map-(i);
     *  and every vellum tag by its widget's name. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn: here, title, notice; Banner: row-(i) (the troop id), count-(i), roll-hint, seat-(i) (the troop id, or empty),
     *  armed, armed-hint, seated, bonus, class-(id); Contracts: contract-(i), progress-(i), weekly, progress-weekly, reroll-hint,
     *  reset; Chapter: tier, first (the first tier on the track), xp, to-next, hero (its tier), hero-name, hero-state, roll-(t)
     *  and oath-(t) (their two lines), unsworn; Friends: camp-how, invite-hint, invitation, lobby-invitation, member-(i),
     *  party-rule, party-line, room-why, code, room-code, code-count; Field: map-(i), card-name, status, bots-current; every vellum tag's
     *  label by its widget's name. */
    static Map<String, String> texts() { return texts; }
    /** The texture each part was drawn from: table, here, class-(id), claim (the Claim plate), roll-(t), stamp-(t), stamp-(i),
     *  stamp-weekly, earlier, later, card (or "seals", home's fallback for a map with no card, or "none"). */
    static Map<String, String> textures() { return textures; }
    /** The colour each text was drawn in, keyed as texts() is. */
    static Map<String, Integer> colors() { return colors; }
    static List<PortraitDraw> portraits() { return portraits; }
    /** The widgets the last frame drew underlined (hot: under the pointer, or holding the keyboard's focus). */
    static Set<String> underlined() { return underlined; }
    /** The last SENT_KEPT commands the pages handed to the server (after CampScreen.action's throttle let it through), oldest
     *  first. A check counting sends by its size counts right while fewer than SENT_KEPT have gone. */
    static List<String> sent() { return sent; }
    static final int SENT_KEPT = 256;

    final CampScreen screen;
    /** The page shown: one of NAMES. */
    final String page;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();
    /** The page's clock (client ticks), and the notice line as the recruit page keeps it: shown when the serial moves, so a notice
     *  already waiting when the page opened stays hidden. */
    long ticks;
    int noticeSerialSeen;
    String noticeText = "";
    long noticeAt = -1000;
    /** The badges the rail's plaques were built with: a change rebuilds the page. */
    int builtContracts = -1, builtFriends = -1;
    /** Banner: what a seat click places, a troop id or EMPTY (the Clear tag). */
    String armed;
    /** Friends: the first commander of the camp's list shown, the room code being written and whether it is being written. */
    int offset;
    String code = "";
    boolean typing;
    /** Field: the map whose card shows, and the map the war gates last took this commander's vote for (-1: none). */
    int viewed, voted = -1;
    /** Chapter: whether the arrows moved the track off its place, and where to; the first tier the last tick showed; the tier this
     *  page last claimed (its stamp is the wet one); and the tick the track began to slide. */
    boolean paged;
    int pagedFirst = 1, shownFirst = -1, freshTier = -1;
    double slideAt = -1000;

    WarTablePages(CampScreen screen, String page) {
        this.screen = screen;
        this.page = page;
        noticeSerialSeen = CampaignClient.noticeSerial;
        viewed = Math.max(0, Math.min(Spec.MAPS.length - 1, CampaignClient.map));
    }

    /** Opens a page: on the War Table already open, or on a new one. */
    static void open(String page) {
        var c = MinecraftClient.getInstance();
        if (c.currentScreen instanceof CampScreen s) s.page(page);
        else c.setScreen(new CampScreen(page));
    }

    /** The widget a name stands for, or null: back, recruit, banner, contracts, chapter, friends (the rail's, all but the open
     *  page's); Banner: row-(i), add-(i), remove-(i), seat-(i), clear, class-(id); Contracts: claim-(i), reroll, claim-weekly;
     *  Chapter: claim, earlier, later, title-(t); Friends: invite-(k), more, accept, decline, lobby-accept, lobby-decline, lead-(i),
     *  kick-(i), create, leave, disband, claim-lead, room-create, code, room-join, copy, room-leave, team-0, team-1, size-(n), fill,
     *  map, ready, start; Field: map-(i), vote-(i), bots-(tier). */
    UiButton button(String name) { return buttons.get(name); }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    static TextRenderer font() { return MinecraftClient.getInstance().textRenderer; }

    /** Builds the widgets (CampScreen.init on the page): again whenever the camp state or a rail badge changes. */
    void init() {
        buttons.clear();
        builtContracts = WarTableHome.contractsBadge();
        builtFriends = WarTableHome.friendsBadge();
        add("back", WarTable.backPlaque(b -> back()));
        for (int i = 0; i < WarTableHome.RAIL.length; i++) {
            if (WarTableHome.RAIL[i].equals(page)) continue; // the open page's plaque is drawn lit (here), not a widget
            final String target = WarTableHome.RAIL[i];
            int badge = target.equals("contracts") ? builtContracts : target.equals("friends") ? builtFriends : 0;
            add(target, WarTable.plaque(WarTableHome.RAIL_X[i], WarTableHome.RAIL_W, "icon-" + target, WarTableHome.RAIL_LABELS[i], null, 0, badge, b -> screen.page(target)));
        }
        switch (page) {
            case "banner" -> initBanner();
            case "contracts" -> initContracts();
            case "chapter" -> initChapter();
            case "friends" -> initFriends();
            default -> initField();
        }
        refresh();
    }

    /** Puts the widgets in step with the camp state (what can be pressed now, what shows): every tick and after init. */
    void refresh() {
        switch (page) {
            case "banner" -> refreshBanner();
            case "contracts" -> refreshContracts();
            case "chapter" -> refreshChapter();
            case "friends" -> refreshFriends();
            default -> refreshField();
        }
    }

    void tick() {
        ticks++;
        if (WarTableHome.contractsBadge() != builtContracts || WarTableHome.friendsBadge() != builtFriends) screen.rebuild();
        if (CampaignClient.noticeSerial != noticeSerialSeen) {
            noticeSerialSeen = CampaignClient.noticeSerial;
            noticeText = CampaignClient.notice;
            if (!noticeText.isEmpty()) noticeAt = ticks;
            for (int i = 0; i < Spec.MAPS.length; i++) if (noticeText.equals(VOTE_SAVED + Spec.MAPS[i].name())) voted = i;
        }
        if (page.equals("chapter")) slide();
        refresh();
    }

    void back() { screen.page("home"); }

    /** Hands a command to the server through the screen's throttle; recorded, and true, when it goes. */
    boolean send(String cmd) {
        if (!screen.action(cmd)) return false;
        if (sentCommands.size() >= SENT_KEPT) sentCommands.remove(0);
        sentCommands.add(cmd);
        return true;
    }

    static boolean battleFree() { return WarTableRecruit.battleFree(); }

    /** A key's press; CampScreen hands over only the press (a held key's repeats go no further). Esc stops writing the room code,
     *  else goes home. Enter or Space presses the widget the keyboard's focus is on; with none, Enter on the Chapter is Claim.
     *  While the room code is being written Enter joins with it and Space does nothing. Left and Right on the Chapter turn the
     *  track a page, as its arrows do. */
    boolean keyPressed(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (typing) typing = false;
            else back();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            if (typing) {
                if (key != GLFW.GLFW_KEY_SPACE) join();
                return true;
            }
            var target = keyboardFocus();
            if (target == null && key != GLFW.GLFW_KEY_SPACE && page.equals("chapter")) target = buttons.get("claim");
            if (target != null && target.visible && target.active) WarTableRecruit.press(target);
            return true;
        }
        if (page.equals("chapter") && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
            turn(key == GLFW.GLFW_KEY_LEFT ? -1 : 1);
            return true;
        }
        return false;
    }

    /** A key while the room code is being written, ahead of the held keys and the page's keys (so Backspace repeats as it is
     *  held): Backspace takes the last character off, paste puts in the clipboard's letters and digits, Tab ends the writing and
     *  goes on to move the focus, Esc and Enter go on to keyPressed. Every other key is taken here, so a letter of the code does
     *  not also act as a key (B, M, the menu key); the letters themselves arrive through charTyped. */
    boolean typing(int key) {
        if (!typing) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) return false;
        if (key == GLFW.GLFW_KEY_TAB) { typing = false; return false; }
        if (key == GLFW.GLFW_KEY_BACKSPACE) { if (!code.isEmpty()) code = code.substring(0, code.length() - 1); return true; }
        if (Screen.isPaste(key)) { for (char ch : MinecraftClient.getInstance().keyboard.getClipboard().toCharArray()) put(ch); return true; }
        return true;
    }

    /** A character typed while the room code is being written: letters (made upper case) and digits, up to CODE_LENGTH. */
    boolean charTyped(char ch) {
        if (!typing) return false;
        put(ch);
        return true;
    }

    void put(char ch) {
        boolean ok = ch >= '0' && ch <= '9' || ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z';
        if (ok && code.length() < CODE_LENGTH) code += Character.toUpperCase(ch);
    }

    /** A click with the other mouse button: on a seat of the Banner it empties the seat. */
    boolean secondaryClick(double x, double y) {
        if (!page.equals("banner")) return false;
        for (int i = 0; i < SEATS; i++) {
            var b = buttons.get("seat-" + i);
            if (b != null && b.visible && b.active && b.overArt(x, y)) { place(i, EMPTY); return true; }
        }
        return false;
    }

    /** A click anywhere: off the room code's line it stops the writing (a click on the line starts it, through its widget). */
    void clicked(double x, double y) {
        var slot = buttons.get("code");
        if (typing && (slot == null || !slot.overArt(x, y))) typing = false;
    }

    /** The page's widget the keyboard's focus is on (Tab or the arrows put it there), or null; see WarTableRecruit.keyboardFocus. */
    UiButton keyboardFocus() {
        if (!MinecraftClient.getInstance().getNavigationType().isKeyboard()) return null;
        for (var b : buttons.values()) if (b.isFocused() && b.visible) return b;
        return null;
    }

    static boolean hot(UiButton b) { return WarTableRecruit.hot(b); }

    /** Underlines width px of a label at (x, y) in its ink, and records that the widget drew it. */
    static void underline(DrawContext g, String widget, int x, int y, int width, int color) {
        g.fill(x, y, x + width, y + 1, color);
        underlinedDrawn.add(widget);
    }

    /** Draws a text at (x, y), recording the words and the colour under key. */
    static void text(DrawContext g, String key, Text t, String words, int x, int y, int color) {
        g.drawText(font(), t, x, y, color, false);
        textsDrawn.put(key, words);
        colorsDrawn.put(key, color);
    }

    /** Wrapped body text: at most max lines no wider than w from (x, y), 13 px apart. Returns the lines drawn. */
    static List<String> lines(DrawContext g, String s, int x, int y, int w, int max, int color) {
        var font = font();
        var out = UiMath.lines(s, w, max, t -> font.getWidth(DarkUi.body(t)));
        for (int i = 0; i < out.size(); i++) g.drawText(font, DarkUi.body(out.get(i)), x, y + 13 * i, color, false);
        return out;
    }

    /** A vellum tag w x h with its label, a heading centred on it: INK_TEXT while it can be pressed, SEPIA while not, underlined
     *  while hot. 64, 80 and 88 by 24 are drawn whole; another size is the 64's nine-slice (its PNG is the box and the shadow,
     *  1 px right and 2 down). A tag marked current carries the check at its left and the label after it. */
    void vellum(DrawContext g, String widget, int x, int y, int w, int h, String label, boolean current) {
        vellum(g, widget, x, y, w, h, label, current, false);
    }

    /** As vellum, with figures set: the label in Minecraft's own face, whose figures line up (a team size reads 1v1; the heading
     *  face's old-style 1 reads as an I). */
    void vellum(DrawContext g, String widget, int x, int y, int w, int h, String label, boolean current, boolean figures) {
        var font = font();
        var b = buttons.get(widget);
        int ink = b != null && b.active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        if (w == 64 && h == 24) UiTex.draw(g, "table/btn-vellum-64x24", x, y);
        else if (w == 80 && h == 24) UiTex.draw(g, "table/btn-vellum-80x24", x, y);
        else if (w == 88 && h == 24) UiTex.draw(g, "table/btn-vellum-88x24", x, y);
        else UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
        Text t = figures ? Text.literal(label) : DarkUi.heading(label);
        int line = figures ? 9 : 12, top = y + (h - (figures ? 8 : 12)) / 2, tw = font.getWidth(t), lx;
        if (current) {
            UiTex.draw(g, "table/ready-check", x + 7, y + (h - 8) / 2);
            lx = x + 19;
            g.drawText(font, t, lx, top, ink, false);
        } else lx = WarTableHome.centred(g, t, x + w / 2, top, ink);
        if (hot(b)) underline(g, widget, lx, top + line, tw, ink);
        rectsDrawn.put(widget, new int[]{x, y, w, h});
        textsDrawn.put(widget, label);
        colorsDrawn.put(widget, ink);
    }

    /** A vellum tag widget, 24 tall, that runs press. */
    UiButton tag(String name, int x, int y, int w, String label, Runnable press) {
        return add(name, new UiButton(x, y, w, 24, Text.literal(label), b -> press.run(), (g, bx, by, bw, bh, hot) -> vellum(g, name, bx, by, bw, bh, label, false)));
    }

    // ===================================================================================================================== drawing

    void render(DrawContext g) {
        rectsDrawn.clear(); textsDrawn.clear(); texturesDrawn.clear(); colorsDrawn.clear(); portraitsDrawn.clear(); underlinedDrawn.clear();
        UiCanvas.begin(g);
        try {
            int[] o = WarTable.origin();
            int ox = o[0], oy = o[1];
            WarTable.drawChrome(g, page);
            rectsDrawn.putAll(WarTable.drawn()); // floor, table and rail, as drawChrome drew them
            texturesDrawn.put("table", WarTable.drawnTable());
            here(g, ox);
            title(g, ox, oy);
            switch (page) {
                case "banner" -> banner(g, ox, oy);
                case "contracts" -> contracts(g, ox, oy);
                case "chapter" -> chapter(g, ox, oy);
                case "friends" -> friends(g, ox, oy);
                default -> field(g, ox, oy);
            }
            notice(g, ox, oy);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** The open page's plaque on the rail, lit (plaque-112-here, its name in RAIL_KEY): the page you are on. */
    void here(DrawContext g, int ox) {
        int i = Arrays.asList(WarTableHome.RAIL).indexOf(page);
        if (i < 0) return;
        int x = ox + WarTableHome.RAIL_X[i];
        UiTex.draw(g, "table/plaque-112-here", x, 12);
        rectsDrawn.put("here", new int[]{x, 12, WarTableHome.RAIL_W, 28}); // the plaque's box, as the rail's widgets have theirs (the PNG adds a shadow)
        String icon = "table/icon-" + WarTableHome.RAIL[i]; // built, as WarTable.plaque builds it
        UiTex.draw(g, icon, x + 8, 19);
        String label = WarTableHome.RAIL_LABELS[i];
        text(g, "here", DarkUi.heading(label), label, x + 28, 20, DarkUi.RAIL_KEY);
        texturesDrawn.put("here", "table/plaque-112-here");
    }

    /** The page's focal title, IVORY with its shadow, centred on the page at TITLE_Y; none on the Chapter (its chapter block). On a
     *  canvas shorter than the page (854x480, which crops it) TITLE_Y would fall under the rail's moulding, so the title keeps to
     *  the planks just under the rail. */
    void title(DrawContext g, int ox, int oy) {
        if (page.equals("chapter")) return;
        var font = font();
        String words = TITLES.get(page);
        var t = DarkUi.focal(words);
        int w = font.getWidth(t), x = ox + TITLE_X - w / 2, y = Math.max(oy + TITLE_Y, UiTex.entry("table/rail").h());
        g.drawText(font, t, x, y, DarkUi.IVORY, true);
        rectsDrawn.put("title", new int[]{x, y, w, 16});
        textsDrawn.put("title", words);
        colorsDrawn.put("title", DarkUi.IVORY);
    }

    /** The server's notice (a refusal, a claim's reward, a saved vote) in IVORY, on two lines when one is too short, shown
     *  WarTableHome.NOTICE_TICKS and then faded over NOTICE_FADE_TICKS, as home's and the recruit page's are. */
    void notice(DrawContext g, int ox, int oy) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= WarTableHome.NOTICE_TICKS + WarTableHome.NOTICE_FADE_TICKS) return;
        var font = font();
        boolean right = page.equals("chapter");
        float fade = age < WarTableHome.NOTICE_TICKS ? 1 : 1 - (age - WarTableHome.NOTICE_TICKS) / (float) WarTableHome.NOTICE_FADE_TICKS;
        var lines = UiMath.lines(HintText.expand(noticeText, GravesAndGloryClient::hint), right ? CHAPTER_NOTICE_W : NOTICE_W, NOTICE_LINES, s -> font.getWidth(DarkUi.body(s)));
        if (lines.isEmpty()) return;
        int alpha = Math.max(4, Math.round(fade * 255)), ink = (alpha << 24) | (DarkUi.IVORY & 0xffffff);
        int left = Integer.MAX_VALUE, end = Integer.MIN_VALUE, top = oy + (right ? CHAPTER_NOTICE_Y : NOTICE_Y);
        for (int i = 0; i < lines.size(); i++) {
            var t = DarkUi.body(lines.get(i));
            int w = font.getWidth(t), x = right ? ox + CHAPTER_NOTICE_R - w : ox + 480 - w / 2;
            g.drawText(font, t, x, top + NOTICE_STEP * i, ink, false);
            left = Math.min(left, x);
            end = Math.max(end, x + w);
        }
        rectsDrawn.put("notice", new int[]{left, top, end - left, NOTICE_STEP * (lines.size() - 1) + 11});
        textsDrawn.put("notice", String.join(" ", lines));
        colorsDrawn.put("notice", ink);
    }

    // ====================================================================================================================== Banner

    /** The House the commander fights for, as the old army page took it: the snapshot's team, Dawn before a team is known. */
    static int team() { return Math.max(0, Math.min(Spec.FACTIONS.length - 1, GravesAndGloryClient.team)); }

    /** The troops of the commander's House he has recruited, in roll order (at most ROWS: Phase 1 has six to a House). */
    static List<RecruitView.Row> owned() {
        List<RecruitView.Row> out = new ArrayList<>();
        for (var r : RecruitView.roll(Spec.FACTIONS[team()].id())) if (r.owned() && out.size() < ROWS) out.add(r);
        return out;
    }

    /** The seats a troop holds in the deployment (the army's count of it). */
    static int seats(String troop) { return CampaignClient.profile.army.getOrDefault(troop, 0); }

    static String troopName(String id) {
        var s = ArmyCards.spec(id);
        return s == null ? id : s.name();
    }

    void initBanner() {
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        for (int i = 0; i < ROWS; i++) {
            final int index = i;
            int y = oy + ROW_TOP + ROW_H * i;
            String add = "add-" + i, remove = "remove-" + i;
            add("row-" + i, new UiButton(ox + ROLL_X, y, ROW_W, ROW_H, Text.literal(""), b -> arm(index), (g, x, by, w, h, hot) -> rowHot(g, index, x, by)));
            add(add, new UiButton(ox + TAG_X, y + ADD_DY, TAG_W, TAG_H, Text.literal("Add"), b -> army(index, "add"), (g, x, by, w, h, hot) -> vellum(g, add, x, by, w, h, "Add", false)));
            add(remove, new UiButton(ox + TAG_X, y + REMOVE_DY, TAG_W, TAG_H, Text.literal("Remove"), b -> army(index, "remove"), (g, x, by, w, h, hot) -> vellum(g, remove, x, by, w, h, "Remove", false)));
        }
        for (int i = 0; i < SEATS; i++) {
            final int seat = i;
            add("seat-" + i, new UiButton(ox + SEAT_X + SEAT_STEP * (i % 5), oy + SEAT_Y + SEAT_STEP * (i / 5), SEAT, SEAT, Text.literal("Seat " + (i + 1)), b -> place(seat, armed), (g, x, y, w, h, hot) -> seatHot(g, seat, x, y)));
        }
        tag("clear", ox + CLEAR_X, oy + CLEAR_Y, 64, "Clear", () -> armed = EMPTY);
        for (int i = 0; i < Spec.CLASSES.length; i++) {
            var cl = Spec.CLASSES[i];
            add("class-" + cl.id(), new UiButton(ox + CLASS_X, oy + CLASS_Y + CLASS_STEP * i, CLASS_W, 28, Text.literal(cl.name()), b -> send("class:" + cl.id()), (g, x, y, w, h, hot) -> paintClass(g, cl, x, y)));
        }
    }

    /** The rows follow the roll; Add while a seat is empty, Remove while the troop holds one; Clear while a troop is armed; a class
     *  plaque while it is not the class in use. The armed troop falls back to the roll's first when it leaves the roll. */
    void refreshBanner() {
        var roll = owned();
        if (armed == null || !armed.equals(EMPTY) && roll.stream().noneMatch(r -> r.troopId().equals(armed))) armed = roll.isEmpty() ? EMPTY : roll.get(0).troopId();
        boolean free = battleFree(), room = CampaignClient.profile.count(team()) < SEATS;
        for (int i = 0; i < ROWS; i++) {
            boolean shown = i < roll.size();
            String troop = shown ? roll.get(i).troopId() : "";
            var row = buttons.get("row-" + i);
            var add = buttons.get("add-" + i);
            var remove = buttons.get("remove-" + i);
            if (row != null) { row.visible = shown; row.setMessage(Text.literal(shown ? roll.get(i).name() : "")); }
            if (add != null) { add.visible = shown; add.active = shown && free && room; }
            if (remove != null) { remove.visible = shown; remove.active = shown && free && seats(troop) > 0; }
        }
        for (int i = 0; i < SEATS; i++) {
            var s = buttons.get("seat-" + i);
            if (s != null) s.active = free;
        }
        var clear = buttons.get("clear");
        if (clear != null) clear.active = !EMPTY.equals(armed);
        for (var cl : Spec.CLASSES) {
            var b = buttons.get("class-" + cl.id());
            if (b != null) b.active = free && !cl.id().equals(CampaignClient.profile.commander);
        }
    }

    /** A click on row i arms its troop: a seat click then places it. */
    void arm(int i) {
        var roll = owned();
        if (i < roll.size()) armed = roll.get(i).troopId();
        refresh();
    }

    /** Add or Remove on row i: army:(troop):add takes the first empty seat, :remove empties the last seat the troop holds. */
    void army(int i, String how) {
        var roll = owned();
        if (i < roll.size()) send("army:" + roll.get(i).troopId() + ":" + how);
    }

    void place(int seat, String troop) { if (troop != null) send("slot:" + seat + ":" + troop); }

    void banner(DrawContext g, int ox, int oy) {
        var roll = owned();
        int x = ox + ROLL_X, sel = -1;
        rectsDrawn.put("roll", UiTex.draw(g, "recruit/muster-roll", x, oy + ROLL_Y));
        for (int i = 0; i < roll.size(); i++) if (roll.get(i).troopId().equals(armed)) sel = i;
        if (sel >= 0) rectsDrawn.put("mark", UiTex.draw(g, "recruit/row-mark", x, oy + ROW_TOP + ROW_H * sel));
        boolean more = RecruitView.roll(Spec.FACTIONS[team()].id()).stream().anyMatch(r -> !r.owned());
        int places = more ? ROWS : roll.size(); // the House's troops not yet recruited keep their places on the roll, ruled and empty
        for (int i = 1; i < places; i++) if (i != sel) UiTex.draw(g, "recruit/rule-208", x + 16, oy + ROW_TOP + ROW_H * i);
        for (int i = 0; i < roll.size(); i++) rollRow(g, roll.get(i), i, x, oy + ROW_TOP + ROW_H * i);
        if (more && roll.size() < ROWS) { // where the rest of the House's troops come from: in the roll's last place, at its foot
            var hint = UiMath.lines(ROLL_HINT, ROLL_HINT_W, 2, s -> font().getWidth(DarkUi.body(s)));
            int top = oy + ROW_TOP + ROW_H * (ROWS - 1) + (ROW_H + 4 - 13 * hint.size()) / 2;
            for (int i = 0; i < hint.size(); i++) WarTableHome.centred(g, DarkUi.body(hint.get(i)), x + ROLL_HINT_CX, top + 13 * i, DarkUi.SEPIA);
            textsDrawn.put("roll-hint", String.join(" ", hint));
            rectsDrawn.put("roll-hint", new int[]{x + ROLL_HINT_CX - ROLL_HINT_W / 2, top, ROLL_HINT_W, 13 * hint.size() - 4});
        }
        sheet(g, ox + SHEET_X, oy + SHEET_Y, ox, oy);
        armedBand(g, ox + ARMED_X, oy + ARMED_Y);
        classes(g, ox + CLASS_X, oy);
    }

    /** Roll row i: the troop's live model in the frame, its name and the seats it holds (the Add and Remove tags are widgets). */
    void rollRow(DrawContext g, RecruitView.Row r, int i, int x, int y) {
        var font = font();
        UiTex.draw(g, "table/frame-back-48", x + 8, y + 8);
        var e = WarTableRecruit.model(r.troopId());
        if (e != null) {
            Portrait.draw(g, e, x + 8, y + 8, 40, 0);
            portraitsDrawn.add(new PortraitDraw("row-" + i, r.troopId(), Portrait.last()));
        }
        UiTex.draw(g, "table/frame-rim-48", x + 4, y + 4);
        String name = UiMath.trim(r.name(), NAME_W, s -> font.getWidth(DarkUi.heading(s)));
        g.drawText(font, DarkUi.heading(name), x + 60, y + 12, DarkUi.INK_TEXT, false);
        int n = seats(r.troopId());
        String count = n == 0 ? "No seats" : n == 1 ? "1 seat" : n + " seats";
        g.drawText(font, DarkUi.body(count), x + 60, y + 30, DarkUi.SEPIA, false);
        rectsDrawn.put("row-" + i, new int[]{x, y, ROW_W, ROW_H});
        textsDrawn.put("row-" + i, r.troopId());
        textsDrawn.put("count-" + i, count);
    }

    /** Row i's widget: its name underlined while hot, unless it is the armed troop's row (a click on it changes nothing). */
    void rowHot(DrawContext g, int i, int x, int y) {
        var roll = owned();
        var b = buttons.get("row-" + i);
        if (i >= roll.size() || roll.get(i).troopId().equals(armed) || !hot(b)) return;
        var font = font();
        String name = UiMath.trim(roll.get(i).name(), NAME_W, s -> font.getWidth(DarkUi.heading(s)));
        underline(g, "row-" + i, x + 60, y + 24, font.getWidth(DarkUi.heading(name)), DarkUi.INK_TEXT);
    }

    /** The sheet: the House's name with its mark, how many seats are filled, Front and Rear by the first and last rows, and the
     *  twenty seats, each drawn as home draws a party seat (the seat widgets underline only). */
    void sheet(DrawContext g, int x, int y, int ox, int oy) {
        var font = font();
        rectsDrawn.put("sheet", UiTex.draw(g, "table/sheet-banner", x, y));
        int t = team();
        int nx = WarTableHome.centred(g, DarkUi.heading(Spec.FACTIONS[t].name()), x + 200, y + 16, DarkUi.INK_TEXT);
        DarkUi.teamMark(g, t, nx - 11, y + 18);
        String seated = CampaignClient.profile.count(t) + " of " + SEATS + " seats filled";
        WarTableHome.centred(g, DarkUi.body(seated), x + 200, y + 32, DarkUi.SEPIA);
        textsDrawn.put("seated", seated);
        g.drawText(font, DarkUi.body("Front"), x + 16, oy + SEAT_Y + 19, DarkUi.SEPIA, false);
        g.drawText(font, DarkUi.body("Rear"), x + 16, oy + SEAT_Y + 3 * SEAT_STEP + 19, DarkUi.SEPIA, false);
        var line = CampaignClient.profile.lineup(t);
        for (int i = 0; i < SEATS; i++)
            seat(g, "seat-" + i, i < line.size() ? line.get(i) : EMPTY, ox + SEAT_X + SEAT_STEP * (i % 5), oy + SEAT_Y + SEAT_STEP * (i / 5));
    }

    /** A seat, as home seats the party: the frame's back (the empty one for an empty seat), the troop's live model and the rim. */
    void seat(DrawContext g, String where, String troop, int x, int y) {
        boolean empty = troop.equals(EMPTY);
        if (empty) UiTex.draw(g, "table/frame-back-empty-48", x + 4, y + 4);
        else UiTex.draw(g, "table/frame-back-48", x + 4, y + 4);
        var e = empty ? null : WarTableRecruit.model(troop);
        if (e != null) {
            Portrait.draw(g, e, x + 4, y + 4, 40, 0);
            portraitsDrawn.add(new PortraitDraw(where, troop, Portrait.last()));
        }
        UiTex.draw(g, "table/frame-rim-48", x, y);
        rectsDrawn.put(where, new int[]{x, y, SEAT, SEAT});
        textsDrawn.put(where, troop);
    }

    /** Seat i's widget: a line under the seat while hot. */
    void seatHot(DrawContext g, int i, int x, int y) {
        if (hot(buttons.get("seat-" + i))) underline(g, "seat-" + i, x + 4, y + 51, 40, DarkUi.INK_TEXT);
    }

    /** The armed troop under the seats: its seat with its model, its name and what a seat click does (Clear is a widget). */
    void armedBand(DrawContext g, int x, int y) {
        boolean clear = EMPTY.equals(armed);
        seat(g, "armed-seat", clear ? EMPTY : armed, x, y);
        String name = clear ? "Clear" : troopName(armed), hint = clear ? "Click a seat to empty it" : "Click a seat to place it";
        text(g, "armed", DarkUi.heading(name), name, x + 56, y + 6, DarkUi.INK_TEXT);
        text(g, "armed-hint", DarkUi.body(hint), hint, x + 56, y + 22, DarkUi.SEPIA);
    }

    /** The commander classes' heading and the bonus of the class under the pointer or the keyboard's focus, else of the class in
     *  use (the plaques are widgets, paintClass). */
    void classes(DrawContext g, int x, int oy) {
        g.drawText(font(), DarkUi.heading("Commander class"), x, oy + 124, DarkUi.BRASS, false);
        var shown = Spec.CLASSES[0];
        for (var cl : Spec.CLASSES) if (cl.id().equals(CampaignClient.profile.commander)) shown = cl;
        for (var cl : Spec.CLASSES) if (hot(buttons.get("class-" + cl.id()))) shown = cl;
        textsDrawn.put("bonus", String.join(" ", lines(g, shown.bonus(), x, oy + BONUS_Y, BONUS_W, 4, DarkUi.MUTED)));
    }

    /** A class plaque: plaque-120 with the class's icon and name, the class in use lit (plaque-120-here, RAIL_KEY, no press). */
    void paintClass(DrawContext g, Spec.ClassesRow cl, int x, int y) {
        boolean mine = cl.id().equals(CampaignClient.profile.commander);
        String key = "class-" + cl.id(), plate = mine ? "table/plaque-120-here" : "table/plaque-120", icon = "table/icon-class-" + cl.id();
        UiTex.draw(g, plate, x, y);
        UiTex.draw(g, icon, x + 8, y + 7);
        int ink = mine ? DarkUi.RAIL_KEY : hot(buttons.get(key)) ? DarkUi.BRASS_HI : DarkUi.IVORY;
        text(g, key, DarkUi.heading(cl.name()), cl.name(), x + 28, y + 8, ink);
        rectsDrawn.put(key, new int[]{x, y, CLASS_W, 28});
        texturesDrawn.put(key, plate);
    }

    // =================================================================================================================== Contracts

    static Progression.State progression() { return CampaignClient.profile.progression; }

    void initContracts() {
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            tag("claim-" + i, ox + NOTE_X + NOTE_STEP * i + 124, oy + NOTE_Y + 64, 64, "Claim", () -> send("progress:daily:" + slot));
        }
        add("reroll", new UiButton(ox + REROLL_X, oy + REROLL_Y, 88, 24, Text.literal("Reroll"), b -> send("progress:reroll"), this::paintReroll));
        tag("claim-weekly", ox + WEEKLY_X + 228, oy + WEEKLY_Y + 64, 64, "Claim", () -> send("progress:weekly"));
    }

    /** Claim shows while its contract can be claimed (Progression.claimable, the rule the Contracts badge counts); Reroll takes a
     *  press until the day's reroll is used or the third contract is claimed. */
    void refreshContracts() {
        var p = progression();
        for (int i = 0; i < 3; i++) {
            var b = buttons.get("claim-" + i);
            if (b != null) b.visible = b.active = Progression.claimable(p, "daily:" + i);
        }
        var w = buttons.get("claim-weekly");
        if (w != null) w.visible = w.active = Progression.claimable(p, "weekly");
        var r = buttons.get("reroll");
        if (r != null) r.active = !p.rerolled && !p.claims.contains("daily:2");
    }

    void contracts(DrawContext g, int ox, int oy) {
        var p = progression();
        String[] notes = {"table/note-contract", "table/note-contract-b", "table/note-contract-c"};
        for (int i = 0; i < 3; i++) {
            int target = Progression.target(p, i);
            note(g, "note-" + i, notes[i], ox + NOTE_X + NOTE_STEP * i, oy + NOTE_Y, 188, Progression.contract(p, i), (i == 1 ? 40 : 30) + " gold and 75 XP",
                Math.min(Progression.progress(p, i), target), target, p.claims.contains("daily:" + i), "contract-" + i, "progress-" + i, i == 1 ? "table/stamp-hollow-b" : "table/stamp-hollow", "stamp-" + i);
        }
        String hint = p.rerolled ? "Rerolled today" : p.claims.contains("daily:2") ? "The third contract is already claimed" : "Swap the third contract, once a day";
        text(g, "reroll-hint", DarkUi.body(hint), hint, ox + REROLL_X + 96, oy + REROLL_Y + 6, DarkUi.MUTED);
        int weekly = Progression.WEEKLY_BATTLES;
        note(g, "weekly", "table/note-weekly", ox + WEEKLY_X, oy + WEEKLY_Y, 292, "Complete " + weekly + " battles this week", "150 gold and 300 XP",
            Math.min(p.weeklyBattles, weekly), weekly, p.claims.contains("weekly"), "weekly", "progress-weekly", "table/stamp-hollow", "stamp-weekly");
        String reset = "Daily contracts reset at 00:00 UTC, the weekly on Monday";
        WarTableHome.centred(g, DarkUi.body(reset), ox + 480, oy + RESET_Y, DarkUi.MUTED);
        textsDrawn.put("reset", reset);
    }

    /** A contract's note: its name (a heading, its counts in words: UiMath.spellSmall) and its reward (SEPIA), the progress bar
     *  and the count in 8 px right-aligned to right, and once claimed the Hollow's mark under the count, as the Chapter stamps a
     *  claimed tier. Claim is a widget. */
    void note(DrawContext g, String key, String tex, int x, int y, int right, String name, String reward, int done, int target, boolean claimed,
              String nameKey, String countKey, String stamp, String stampKey) {
        var font = font();
        rectsDrawn.put(key, UiTex.draw(g, tex, x, y));
        String shown = UiMath.trim(UiMath.spellSmall(name), right - 12, s -> font.getWidth(DarkUi.heading(s)));
        text(g, nameKey, DarkUi.heading(shown), shown, x + 12, y + 12, DarkUi.INK_TEXT);
        g.drawText(font, DarkUi.body(reward), x + 12, y + 30, DarkUi.SEPIA, false);
        UiTex.draw(g, "hud/hp-channel", x + 12, y + 52);
        UiTex.bar(g, "hud/hp-full-dawn", x + 13, y + 53, UiMath.barWidth(target <= 0 ? 0 : done / (float) target), false);
        String count = claimed ? "Claimed" : done + " / " + target;
        int countW = font.getWidth(count);
        text(g, countKey, Text.literal(count), count, x + right - countW, y + 51, DarkUi.INK_TEXT);
        rectsDrawn.put(countKey, new int[]{x + right - countW, y + 51, countW, 8});
        if (claimed) {
            rectsDrawn.put(stampKey, UiTex.draw(g, stamp, x + right - 32, y + 62));
            texturesDrawn.put(stampKey, stamp);
        }
    }

    /** Reroll: a vellum tag with the die and the word; sepia, and no press, once the day's reroll cannot be used. */
    void paintReroll(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = font();
        var b = buttons.get("reroll");
        int ink = b != null && b.active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        var label = DarkUi.heading("Reroll");
        UiTex.draw(g, "table/btn-vellum-88x24", x, y);
        UiTex.draw(g, "recruit/die", x + 12, y + 7);
        g.drawText(font, label, x + 26, y + 6, ink, false);
        if (hot(b)) underline(g, "reroll", x + 26, y + 18, font.getWidth(label), ink);
        rectsDrawn.put("reroll", new int[]{x, y, w, h});
        textsDrawn.put("reroll", "Reroll");
        colorsDrawn.put("reroll", ink);
    }

    // ===================================================================================================================== Chapter

    static boolean claimed(int t) { return progression().claims.contains(Progression.SEASON + ":" + t); }

    static int tier() { return Progression.tier(progression()); }

    /** The lowest tier not yet claimed (TIERS + 1 once every tier is). */
    static int lowestUnclaimed() {
        for (int t = 1; t <= TIERS; t++) if (!claimed(t)) return t;
        return TIERS + 1;
    }

    /** The lowest tier reached and not yet claimed, which Claim takes, or -1. */
    static int claimable() {
        int top = tier();
        for (int t = 1; t <= top; t++) if (!claimed(t)) return t;
        return -1;
    }

    /** The first tier on the track: where the arrows left it, else the tier before the lowest unclaimed (UiMath.trackFirst), so
     *  the last claim stays in view at the left. */
    int first() { return paged ? pagedFirst : UiMath.trackFirst(lowestUnclaimed(), TIERS, SHOWN); }

    void initChapter() {
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        add("claim", new UiButton(ox + CLAIM_X, oy + CLAIM_Y, 240, 40, Text.literal("Claim"), b -> claim(), this::paintClaim));
        add("earlier", new UiButton(ox + X0 - 20, oy + ROD_Y - 3, 15, 15, Text.literal("Earlier tiers"), b -> turn(-1), (g, x, y, w, h, hot) -> paintArrow(g, "earlier", "chapter/arrow-left", "chapter/arrow-left-dim", x, y)));
        add("later", new UiButton(ox + X0 + PITCH * SHOWN - 7, oy + ROD_Y - 3, 15, 15, Text.literal("Later tiers"), b -> turn(1), (g, x, y, w, h, hot) -> paintArrow(g, "later", "chapter/arrow-right", "chapter/arrow-right-dim", x, y)));
        int first = first(), tier = tier();
        for (int k = 0; k < SHOWN; k++) { // a claimed title on the track is worn (or taken off) from its Roll plaque
            int t = first + k;
            if (t % EVERY != 0 || !claimed(t)) continue;
            String title = Progression.TITLES[t / EVERY - 1];
            boolean cur = t == tier;
            add("title-" + t, new UiButton(ox + X0 + PITCH * k, oy + (cur ? ROLL_TOP - 8 : ROLL_TOP), PW, cur ? RH : PH, Text.literal(title), b -> wear(title), (g, x, y, w, h, hot) -> titleHot(g, t, x, y)));
        }
    }

    /** Claim takes a press while a tier waits; an arrow while the track can turn its way. */
    void refreshChapter() {
        var claim = buttons.get("claim");
        if (claim != null) claim.active = claimable() > 0;
        int first = first();
        var earlier = buttons.get("earlier");
        var later = buttons.get("later");
        if (earlier != null) earlier.active = first > 1;
        if (later != null) later.active = first < TIERS - SHOWN + 1;
    }

    /** Claim: the lowest tier reached and not yet claimed. The track goes back to its place, to slide on when the claim lands. */
    void claim() {
        int t = claimable();
        if (t <= 0 || !send("progress:" + Progression.SEASON + ":" + t)) return;
        freshTier = t;
        if (paged) { paged = false; screen.rebuild(); } // the title plaques follow the track back
    }

    /** A title plaque: wears its title, or takes it off (back to Recruit) when it is the one worn. */
    void wear(String title) { send("progress:title:" + (title.equals(progression().title) ? "Recruit" : title)); }

    /** The arrows: the track a page of SHOWN tiers back or on, within the chapter (UiMath.trackPage). */
    void turn(int pages) {
        int now = first(), next = UiMath.trackPage(now, pages, TIERS, SHOWN);
        if (next == now) return;
        pagedFirst = next;
        paged = true;
        slideAt = -1000;
        shownFirst = next;
        screen.rebuild(); // the title plaques follow the track
    }

    /** Each tick: when a claim moves the track on one plaque (its first tier one higher, the arrows not used), it slides. */
    void slide() {
        int f = first();
        if (shownFirst >= 0 && !paged && f == shownFirst + 1) slideAt = ticks;
        shownFirst = f;
    }

    /** How far right of its place the track is drawn while it slides: PITCH as the slide begins, easing out to 0. */
    int slideOffset() {
        double since = ticks + MinecraftClient.getInstance().getTickDelta() - slideAt;
        if (since < 0 || since >= SLIDE_TICKS) return 0;
        return (int) Math.round(PITCH * Math.pow(1 - since / SLIDE_TICKS, 3));
    }

    void chapter(DrawContext g, int ox, int oy) {
        var font = font();
        int tier = tier(), first = first();
        hero(g, ox, oy, UiMath.heroTier(first, SHOWN, TIERS, EVERY), tier);
        block(g, ox, oy, tier, progression());
        g.drawText(font, "Tier", ox + 56, oy + ROD_Y + 2, DarkUi.MUTED, false);
        g.drawText(font, DarkUi.heading("The Roll"), ox + 56, oy + ROLL_TOP + 28, DarkUi.BRASS, false);
        g.drawText(font, "for all", ox + 56, oy + ROLL_TOP + 44, DarkUi.MUTED, false);
        g.drawText(font, DarkUi.heading("The Oath"), ox + 56, oy + OATH_TOP + 28, DarkUi.SEPIA_L, false);
        g.drawText(font, "not sworn", ox + 56, oy + OATH_TOP + 44, DarkUi.MUTED, false);
        UiTex.draw(g, "chapter/track-rod", ox + X0 - 6, oy + ROD_Y);
        track(g, ox, oy, first, tier);
        text(g, "unsworn", DarkUi.body(UNSWORN), UNSWORN, ox + X0, oy + OATH_TOP + 92, DarkUi.MUTED);
        textsDrawn.put("tier", String.valueOf(tier));
        textsDrawn.put("first", String.valueOf(first));
    }

    /** The ten tiers from first (and, while the track slides, the one leaving at the left, cut to the track): each tier's Roll and
     *  Oath plaques, the current tier's column last so its lifted plaque's shadow falls on its neighbours, then the tags on the
     *  rod over the chains' tops, the current tier's a wax pin (the chapter's seal carries its number). */
    void track(DrawContext g, int ox, int oy, int first, int tier) {
        var font = font();
        int offset = slideOffset();
        List<Integer> ks = new ArrayList<>();
        for (int k = offset > 0 ? -1 : 0; k < SHOWN; k++) if (first + k >= 1 && first + k <= TIERS) ks.add(k);
        ks.sort(Comparator.comparing(k -> first + k == tier));
        if (offset > 0) UiCanvas.scissor(g, ox + X0 - 4, oy + ROD_Y - 8, PITCH * SHOWN - 4, OATH_TOP + 96 - ROD_Y);
        try {
            for (int k : ks) {
                int t = first + k, x = ox + X0 + PITCH * k + offset;
                rollPlaque(g, t, x, oy, tier);
                oathPlaque(g, t, x, oy);
            }
            for (int k : ks) {
                int t = first + k, x = ox + X0 + PITCH * k + offset;
                if (t == tier) UiTex.draw(g, "chapter/pin-tier", x + 25, oy + ROD_Y - 4);
                else {
                    UiTex.draw(g, "chapter/tag-tier", x + 24, oy + TAG_Y);
                    String n = String.valueOf(t);
                    g.drawText(font, n, x + 35 - font.getWidth(n) / 2, oy + TAG_Y + 4, DarkUi.PLATE_INK, false);
                }
            }
        } finally {
            if (offset > 0) UiCanvas.endScissor(g);
        }
    }

    /** A Roll plaque (pages.py's plaque_html): claimed, it carries the Hollow's mark (wet on the tier this page just claimed);
     *  reached, a bright frame; not reached, the dull frame under the iron hasp on dull chains. The current tier's hangs lifted on
     *  short chains, 8 px taller. A gold tier names its 25 gold, a title tier its title ("worn" while it is the one worn). */
    void rollPlaque(DrawContext g, int t, int x, int oy, int tier) {
        var font = font();
        boolean cur = t == tier, done = claimed(t), locked = t > tier, title = t % EVERY == 0;
        int y = oy + (cur ? ROLL_TOP - 8 : ROLL_TOP), h = cur ? RH : PH, ly = 47 + oy + ROLL_TOP - y; // one label line across the row
        String chain = locked ? "chapter/chain-dull-18" : cur ? "chapter/chain-10" : "chapter/chain-18";
        UiTex.draw(g, chain, x + 8, oy + ROD_Y + 6);
        UiTex.draw(g, chain, x + PW - 14, oy + ROD_Y + 6);
        String plate = cur ? "chapter/plaque-tier-raised" : locked ? (t % 2 == 1 ? "chapter/plaque-tier-dull" : "chapter/plaque-tier-dull-b")
            : (t % 2 == 1 ? "chapter/plaque-tier" : "chapter/plaque-tier-b");
        UiTex.draw(g, plate, x, y);
        icon(g, title ? "chapter/ic-title" : "chapter/ic-coins", x, y, ly);
        if (locked) UiTex.draw(g, "chapter/hasp", x - 1, y + ly - 12);
        String l1 = title ? Progression.TITLES[t / EVERY - 1] : "Gold", l2 = title ? (done && l1.equals(progression().title) ? "worn" : "title") : "25";
        WarTableHome.centred(g, DarkUi.heading(l1), x + PW / 2, y + ly, DarkUi.IVORY);
        g.drawText(font, l2, x + PW / 2 - font.getWidth(l2) / 2, y + ly + 15, DarkUi.MUTED, false);
        if (done) {
            String stamp = t == freshTier ? "table/stamp-hollow-fresh" : t % 2 == 1 ? "table/stamp-hollow" : "table/stamp-hollow-b";
            UiTex.draw(g, stamp, x + PW - 29, y - 3);
            texturesDrawn.put("stamp-" + t, stamp);
        }
        rectsDrawn.put("roll-" + t, new int[]{x, y, PW, h});
        textsDrawn.put("roll-" + t, l1 + " / " + l2);
        texturesDrawn.put("roll-" + t, plate);
    }

    /** An Oath plaque, sepia under "not sworn" (no House takes oaths in this phase): the House-wide reward the tier will give. */
    void oathPlaque(DrawContext g, int t, int x, int oy) {
        var font = font();
        int y = oy + OATH_TOP;
        UiTex.draw(g, "chapter/chain-sepia-10", x + 8, y - 11);
        UiTex.draw(g, "chapter/chain-sepia-10", x + PW - 14, y - 11);
        UiTex.draw(g, "chapter/plaque-tier-sepia", x, y);
        String[] o = oath(t);
        icon(g, o[0].equals("title") ? "chapter/ic-title-sepia" : "chapter/ic-o-" + o[0] + "-sepia", x, y, 47);
        WarTableHome.centred(g, DarkUi.heading(o[1]), x + PW / 2, y + 47, DarkUi.SEPIA_L);
        g.drawText(font, o[2], x + PW / 2 - font.getWidth(o[2]) / 2, y + 62, DarkUi.SEPIA_LINE, false);
        rectsDrawn.put("oath-" + t, new int[]{x, y, PW, PH});
        textsDrawn.put("oath-" + t, o[1] + " / " + o[2]);
    }

    /** The Oath's reward at tier t: {icon stem, line 1, line 2} (pages.py's oath). */
    static String[] oath(int t) {
        if (t == 10) return new String[]{"frame", "Portrait", "frame"};
        if (t == 20) return new String[]{"cloak", "Cloak", "livery"};
        if (t == 30) return new String[]{"title", "Oathsworn", "title"};
        if (t % 2 == 0) return new String[]{"shards", "Shards", "+10"};
        return OATH_ODD.get(t);
    }

    /** An icon centred on a plaque in the space above its label line ly (pages.py rounds half to even, as Math.rint does). */
    static void icon(DrawContext g, String icon, int x, int y, int ly) {
        var e = UiTex.entry(icon);
        UiTex.draw(g, icon, x + (int) Math.rint(PW / 2.0 - e.w() / 2.0), y + (int) Math.rint((ly + 3) / 2.0 - e.h() / 2.0));
    }

    /** A title plaque's widget: the title's name underlined while hot. */
    void titleHot(DrawContext g, int t, int x, int y) {
        if (!hot(buttons.get("title-" + t))) return;
        int ly = 47 + WarTable.origin()[1] + ROLL_TOP - y, w = font().getWidth(DarkUi.heading(Progression.TITLES[t / EVERY - 1]));
        underline(g, "title-" + t, x + PW / 2 - w / 2, y + ly + 12, w, DarkUi.IVORY);
    }

    /** An arrow plaque at an end of the rod: dim, and no press, when the track cannot turn that way. */
    void paintArrow(DrawContext g, String widget, String on, String off, int x, int y) {
        var b = buttons.get(widget);
        String tex = b != null && b.active ? on : off;
        UiTex.draw(g, tex, x, y);
        if (hot(b)) underline(g, widget, x, y + 17, 15, DarkUi.BRASS_HI);
        texturesDrawn.put(widget, tex);
    }

    /** The hero letter: the commander's live model in the gilt frame, and the next title the track does not show yet (the last
     *  title once the track shows the chapter's end): its tier, name, kind, line, and where the commander stands with it. */
    void hero(DrawContext g, int ox, int oy, int t, int tier) {
        var c = MinecraftClient.getInstance();
        var font = font();
        int x = ox + HERO_X, y = oy + HERO_Y, cx = x + 300;
        rectsDrawn.put("hero", UiTex.draw(g, "chapter/hero-letter", x, y));
        UiTex.draw(g, "table/frame-back-160", x + 20, y + 20);
        if (c.player != null) {
            Portrait.draw(g, c.player, x + 20, y + 20, 160, 0);
            portraitsDrawn.add(new PortraitDraw("hero", c.player.getGameProfile().getName(), Portrait.last()));
        }
        UiTex.draw(g, "table/frame-ornate-184", x + 8, y + 8);
        String name = Progression.TITLES[t / EVERY - 1], kicker = "Tier " + t, kind = "Title, worn in camp";
        boolean got = claimed(t), worn = got && name.equals(progression().title);
        String state = worn ? "Worn in camp" : got ? "Claimed" : t <= tier ? "Yours to claim" : "Unlocks at " + UiMath.thousands(t * Progression.XP_PER_TIER) + " XP";
        g.drawText(font, kicker, cx - font.getWidth(kicker) / 2, y + 34, DarkUi.SEPIA, false);
        WarTableHome.centred(g, DarkUi.focal(name), cx, y + 48, DarkUi.INK_TEXT);
        WarTableHome.centred(g, DarkUi.body(kind), cx, y + 70, DarkUi.SEPIA);
        var copy = UiMath.lines(TITLE_COPY.getOrDefault(name, ""), 160, 3, s -> font.getWidth(DarkUi.body(s)));
        for (int i = 0; i < copy.size(); i++) WarTableHome.centred(g, DarkUi.body(copy.get(i)), cx, y + 92 + 13 * i, DarkUi.INK_TEXT);
        WarTableHome.centred(g, DarkUi.body(state), cx, y + 166, DarkUi.SEPIA);
        UiTex.draw(g, "chapter/seal-title", ox + 340, oy + 60);
        textsDrawn.put("hero", String.valueOf(t));
        textsDrawn.put("hero-name", name);
        textsDrawn.put("hero-state", state);
    }

    /** The chapter block: its name, the tier on the wax seal, the XP poured along the rail with the totals under its ends (Claim
     *  is a widget, paintClaim). */
    void block(DrawContext g, int ox, int oy, int tier, Progression.State p) {
        var font = font();
        g.drawText(font, "Chapter 1", ox + 480, oy + 80, DarkUi.MUTED, false);
        g.drawText(font, DarkUi.heading("The Founders"), ox + 480, oy + 92, DarkUi.BRASS, false);
        textsDrawn.put("chapter", "Chapter 1 / The Founders");
        rectsDrawn.put("seal", UiTex.draw(g, "chapter/seal-tier-big", ox + 480, oy + 144));
        String n = String.valueOf(tier);
        g.drawText(font, n, ox + 502 - font.getWidth(n) / 2, oy + 162, DarkUi.CHIP_TEXT, false);
        int[] rail = UiTex.draw(g, "chapter/xp-rail", ox + 536, oy + 156);
        rectsDrawn.put("xp-rail", rail);
        int full = Progression.XP_PER_TIER * TIERS, poured = UiMath.xpPoured(p.xp, Progression.XP_PER_TIER, TIERS, 360);
        if (poured > 0) { // xp-fill's columns, then its head and the lip of shadow past it, as tools/ui_art pours them (kit_track.xp_poured)
            int px = rail[0] + 4, py = rail[1] + 7;
            if (poured > 1) UiTex.part(g, "chapter/xp-fill", px, py, 0, 0, poured - 1, 6);
            UiTex.part(g, "chapter/xp-fill", px + poured - 1, py, 360, 0, 1, 6);
            UiTex.part(g, "chapter/xp-fill", px + poured, py, 361, 0, 1, 6);
        }
        String xp = UiMath.thousands(Math.min(p.xp, full)) + " / " + UiMath.thousands(full) + " XP";
        String next = tier >= TIERS ? "Every tier reached" : UiMath.thousands((tier + 1) * Progression.XP_PER_TIER - p.xp) + " XP to tier " + (tier + 1);
        text(g, "xp", Text.literal(xp), xp, ox + 536, oy + 184, DarkUi.IVORY);
        text(g, "to-next", Text.literal(next), next, ox + 904 - font.getWidth(next), oy + 184, DarkUi.MUTED);
    }

    /** The Claim plate: brass with the Enter key while a tier waits to be claimed, dull (its key and word grey) when none does. */
    void paintClaim(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = font();
        var b = buttons.get("claim");
        boolean bright = claimable() > 0;
        String plate = bright ? "table/btn-brass-240x40" : "table/btn-dull-240x40";
        UiTex.draw(g, plate, x, y);
        if (bright) UiTex.draw(g, "table/key-brass-37", x + 14, y + 13);
        else UiTex.draw(g, "table/key-dull-37", x + 14, y + 13);
        g.drawText(font, "Enter", x + 18, y + 16, bright ? DarkUi.KEY_INK : DarkUi.DULL_KEY, false);
        int ink = bright ? DarkUi.PLATE_INK : DarkUi.MUTED;
        var label = DarkUi.heading("Claim");
        int lx = WarTableHome.centred(g, label, x + 120, y + 14, ink);
        if (hot(b)) underline(g, "claim", lx, y + 26, font.getWidth(label), ink);
        rectsDrawn.put("claim", new int[]{x, y, w, h});
        texturesDrawn.put("claim", plate);
        textsDrawn.put("claim", "Claim");
        colorsDrawn.put("claim", ink);
    }

    // ===================================================================================================================== Friends

    static JsonObject social() { return CampaignClient.social; }

    static boolean flag(String name) { var s = social(); return s.has(name) && s.get(name).getAsBoolean(); }

    static String value(String name) { var s = social(); return s.has(name) ? s.get(name).getAsString() : ""; }

    static int number(String name, int fallback) { var s = social(); return s.has(name) ? s.get(name).getAsInt() : fallback; }

    static JsonArray rows(String name) { var s = social(); return s.has(name) ? s.getAsJsonArray(name) : new JsonArray(); }

    static int number(JsonObject row, String name, int fallback) { return row.has(name) && !row.get(name).isJsonNull() ? row.get(name).getAsInt() : fallback; }

    static boolean flag(JsonObject row, String name) { return row.has(name) && !row.get(name).isJsonNull() && row.get(name).getAsBoolean(); }

    static String me() {
        var c = MinecraftClient.getInstance();
        return c.player != null ? c.player.getGameProfile().getName() : c.getSession().getUsername();
    }

    /** The party as the note seats them: this commander first, then the others in the snapshot's order; alone without a party. */
    static List<Seat> seats() {
        String leader = value("leader");
        List<Seat> out = new ArrayList<>(), others = new ArrayList<>();
        for (var e : rows("members")) {
            var r = e.getAsJsonObject();
            String name = r.get("name").getAsString();
            boolean me = flag(r, "me");
            (me ? out : others).add(new Seat(name, me || flag(r, "online"), me, name.equals(leader)));
        }
        if (out.isEmpty()) out.add(new Seat(me(), true, true, flag("partyLead")));
        out.addAll(others);
        return out.subList(0, Math.min(4, out.size()));
    }

    /** A custom lobby's invitation waits for this commander (the old war council's Custom lobby tab answered it). */
    static boolean lobbyInvited() {
        var p = CampaignClient.party;
        return p.has("member") && !p.get("member").getAsBoolean() && p.has("inviteSeconds") && p.get("inviteSeconds").getAsInt() > 0;
    }

    /** This commander's ready in the room, as its snapshot lists him. */
    static boolean mineReady() {
        String me = me();
        for (var e : rows("commanders")) {
            var r = e.getAsJsonObject();
            if (r.get("name").getAsString().equals(me)) return flag(r, "ready");
        }
        return false;
    }

    void initFriends() {
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        boolean party = flag("party"), lead = flag("partyLead");
        var available = rows("available");
        if (offset >= available.size()) offset = 0;
        for (int k = 0; k < INVITES && offset + k < available.size(); k++) {
            String name = available.get(offset + k).getAsString(), key = "invite-" + k;
            add(key, new UiButton(ox + CAMP_X, oy + INVITE_Y + INVITE_STEP * k, CAMP_W, 24, Text.literal("Invite " + name), b -> send("social:invite:" + name), (g, x, y, w, h, hot) -> paintInvite(g, key, name, x, y, w, h)));
        }
        if (available.size() > INVITES) tag("more", ox + CAMP_X, oy + INVITE_Y + INVITE_STEP * INVITES, 88, "More", () -> { offset = offset + INVITES < available.size() ? offset + INVITES : 0; screen.rebuild(); });
        if (!value("inviter").isEmpty()) {
            tag("accept", ox + CAMP_X, oy + 344, 112, "Accept invitation", () -> send("social:accept"));
            tag("decline", ox + CAMP_X + 120, oy + 344, 72, "Decline", () -> send("social:decline"));
        }
        if (lobbyInvited()) {
            tag("lobby-accept", ox + CAMP_X, oy + 404, 72, "Accept", () -> send("party:accept"));
            tag("lobby-decline", ox + CAMP_X + 80, oy + 404, 72, "Decline", () -> send("party:decline"));
        }
        var seats = seats();
        for (int i = 0; i < seats.size(); i++) {
            var m = seats.get(i);
            if (!lead || m.me()) continue;
            int y = oy + PARTY_Y + 32 + 56 * i;
            tag("lead-" + i, ox + 488, y, 88, "Make leader", () -> send("social:transfer:" + m.name()));
            tag("kick-" + i, ox + 584, y, 64, "Remove", () -> send("social:kick:" + m.name()));
        }
        int px = ox + PARTY_X + 16, py = oy + PARTY_Y + 264;
        if (!party) tag("create", px, py, 112, "Create party", () -> send("social:create"));
        else {
            tag("leave", px, py, 104, "Leave party", () -> send("social:leave"));
            if (lead) tag("disband", px + 112, py, 112, "Disband party", () -> send("social:disband"));
            else tag("claim-lead", px + 112, py, 136, "Claim offline lead", () -> send("social:claim"));
        }
        int rx = ox + ROOM_X + 16, ry = oy + ROOM_Y;
        String room = value("room");
        if (room.isEmpty()) {
            tag("room-create", rx, ry + CREATE_DY, ROOM_W, "Create private room", () -> send("social:room"));
            add("code", new UiButton(rx, ry + CODE_DY, ROOM_W, 24, Text.literal("Room code"), b -> typing = true, (g, x, y, w, h, hot) -> paintCode(g, x, y, w, h)));
            tag("room-join", rx, ry + JOIN_DY, ROOM_W, "Join with party", this::join);
            return;
        }
        tag("copy", rx, ry + COPY_DY, PAIR_W, "Copy code", () -> MinecraftClient.getInstance().keyboard.setClipboard(room));
        tag("room-leave", rx + PAIR_STEP, ry + COPY_DY, PAIR_W, "Leave room", () -> send("social:exit"));
        tag("team-0", rx, ry + TEAM_DY, PAIR_W, "Party to Dawn", () -> send("social:team:0"));
        tag("team-1", rx + PAIR_STEP, ry + TEAM_DY, PAIR_W, "Party to Dusk", () -> send("social:team:1"));
        for (int n = 1; n <= 4; n++) {
            final int size = n;
            String key = "size-" + n, label = n + "v" + n;
            add(key, new UiButton(rx + 50 * (n - 1), ry + SIZE_DY, 46, 24, Text.literal(label), b -> send("social:size:" + size), (g, x, y, w, h, hot) -> vellum(g, key, x, y, w, h, label, size == number("size", 2), true)));
        }
        tag("fill", rx, ry + FILL_DY, PAIR_W, flag("fill") ? "Bot fill on" : "Bot fill off", () -> send("social:fill"));
        tag("map", rx + PAIR_STEP, ry + FILL_DY, PAIR_W, "Next map", () -> send("social:map:" + ((number("map", 0) + 1) % Spec.MAPS.length)));
        tag("ready", rx, ry + READY_DY, PAIR_W, mineReady() ? "Cancel ready" : "Ready", () -> send("social:ready"));
        tag("start", rx + PAIR_STEP, ry + READY_DY, PAIR_W, flag("playing") ? "Match on" : flag("preparing") ? "Preparing map" : "Start together", () -> send("social:start"));
    }

    /** What the party and the room allow now, as the server rules them: invitations from the party's leader, a room made or
     *  joined by a party's leader (or a commander alone), the room's settings by its idle host, the start by the host once ready. */
    void refreshFriends() {
        boolean lead = flag("partyLead"), alone = !flag("party"), host = flag("host"), playing = flag("playing");
        for (int k = 0; k < INVITES; k++) {
            var b = buttons.get("invite-" + k);
            if (b != null) b.active = lead;
        }
        boolean rooms = UiMath.roomOpen(!alone, lead); // the room's note says why when not (room, UiMath.roomWhy)
        var create = buttons.get("room-create");
        if (create != null) create.active = rooms;
        var join = buttons.get("room-join");
        if (join != null) join.active = rooms && code.length() == CODE_LENGTH;
        for (String n : new String[]{"size-1", "size-2", "size-3", "size-4", "fill", "map"}) {
            var b = buttons.get(n);
            if (b != null) b.active = host && !playing;
        }
        for (String n : new String[]{"team-0", "team-1", "ready", "room-leave"}) {
            var b = buttons.get(n);
            if (b != null) b.active = !playing;
        }
        var start = buttons.get("start");
        if (start != null) start.active = host && !playing && GravesAndGloryClient.ready && !GravesAndGloryClient.running;
    }

    /** Join with party: the code as written, when it is whole; the server finds the room or refuses. */
    void join() {
        typing = false;
        if (code.length() == CODE_LENGTH) send("social:join:" + code);
    }

    void friends(DrawContext g, int ox, int oy) {
        camp(g, ox, oy);
        party(g, ox, oy);
        room(g, ox, oy);
    }

    /** The camp's commanders who are in no party (their Invite tags are widgets), the line when there are none or they cannot be
     *  invited yet, and the invitations waiting: the party's and a custom lobby's. */
    void camp(DrawContext g, int ox, int oy) {
        var font = font();
        int x = ox + CAMP_X;
        g.drawText(font, DarkUi.heading("In this camp"), x, oy + 124, DarkUi.BRASS, false);
        if (rows("available").isEmpty()) {
            var first = lines(g, "Commanders appear here after joining this game server.", x, oy + INVITE_Y, CAMP_W, 2, DarkUi.MUTED);
            var server = MinecraftClient.getInstance().getServer();
            if (server != null) { // this machine runs the camp: how friends reach it (isRemote: open on the network)
                var how = lines(g, server.isRemote() ? CAMP_OPEN : CAMP_HOW, x, oy + INVITE_Y + 13 * first.size() + 8, CAMP_W, 4, DarkUi.MUTED);
                textsDrawn.put("camp-how", String.join(" ", how));
            }
        } else if (!flag("partyLead")) {
            String hint = flag("party") ? "Only the party leader invites." : "Create a party to invite them.";
            text(g, "invite-hint", DarkUi.body(hint), hint, x, oy + 304, DarkUi.MUTED);
        }
        if (!value("inviter").isEmpty()) {
            String line = UiMath.trim("Invitation from " + value("inviter") + " (" + number("inviteSeconds", 0) + " s)", CAMP_W, s -> font.getWidth(DarkUi.body(s)));
            text(g, "invitation", DarkUi.body(line), line, x, oy + 324, DarkUi.IVORY);
        }
        if (lobbyInvited()) {
            var p = CampaignClient.party;
            String line = UiMath.trim("Custom lobby from " + p.get("inviter").getAsString() + " (" + p.get("inviteSeconds").getAsInt() + " s)", CAMP_W, s -> font.getWidth(DarkUi.body(s)));
            text(g, "lobby-invitation", DarkUi.body(line), line, x, oy + 384, DarkUi.IVORY);
        }
    }

    /** An Invite tag: the vellum the width of the column, the plus, "Invite" in the heading face and the commander's name after it
     *  in Minecraft's face (UI Task 8b: a name is an identifier, and the heading face's old-style 1 reads as an I, so "Steve1" and
     *  "SteveI" would look alike; Minecraft's face is drawn 2 px lower to sit on the heading's line, as the room's code does);
     *  sepia while only a leader may invite. */
    void paintInvite(DrawContext g, String key, String name, int x, int y, int w, int h) {
        var font = font();
        var b = buttons.get(key);
        int ink = b != null && b.active ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        UiTex.nine(g, "table/btn-vellum-64x24", x, y, w + 1, h + 2);
        UiTex.draw(g, "table/invite-plus", x + 8, y + 8);
        var verb = DarkUi.heading("Invite");
        int nx = x + 22 + font.getWidth(verb) + 4;
        String shown = UiMath.trim(name, x + w - 8 - nx, s -> font.getWidth(s));
        g.drawText(font, verb, x + 22, y + 6, ink, false);
        g.drawText(font, shown, nx, y + 8, ink, false);
        if (hot(b)) underline(g, key, x + 22, y + 18, nx + font.getWidth(shown) - (x + 22), ink);
        rectsDrawn.put(key, new int[]{x, y, w, h});
        textsDrawn.put(key, "Invite " + shown);
        colorsDrawn.put(key, ink);
    }

    /** The party's note: a seat per member as home seats the party (the live model, or the skin's face for one out of sight),
     *  their names (in Minecraft's face, as paintInvite) and standing (the leader's seat says who leads), the party's rule beside
     *  them and, in a party, that it lasts under them. Make leader, Remove and
     *  the party's tags are widgets. */
    void party(DrawContext g, int ox, int oy) {
        var c = MinecraftClient.getInstance();
        var font = font();
        int nx = ox + PARTY_X, ny = oy + PARTY_Y;
        rectsDrawn.put("party", UiTex.draw(g, "table/note-friends", nx, ny));
        var seats = seats();
        for (int i = 0; i < seats.size(); i++) {
            var m = seats.get(i);
            int x = nx + 16, y = ny + 16 + 56 * i;
            UiTex.draw(g, "table/frame-back-48", x + 4, y + 4);
            LivingEntity e = m.me() ? c.player : null;
            if (!m.me() && c.world != null) for (var p : c.world.getPlayers()) if (p.getGameProfile().getName().equals(m.name())) e = p;
            if (e != null) {
                Portrait.draw(g, e, x + 4, y + 4, 40, 0);
                portraitsDrawn.add(new PortraitDraw("member-" + i, m.name(), Portrait.last()));
            } else {
                var entry = c.getNetworkHandler() == null ? null : c.getNetworkHandler().getPlayerListEntry(m.name());
                if (entry != null) PlayerSkinDrawer.draw(g, entry.getSkinTexture(), x + 4, y + 4, 40);
            }
            UiTex.draw(g, "table/frame-rim-48", x, y);
            String name = UiMath.trim(m.name(), PARTY_NAME_W, s -> font.getWidth(s)); // Minecraft's face: its 1 is no I (paintInvite)
            text(g, "member-" + i, Text.literal(name), name, x + 56, y + 14, m.online() ? DarkUi.INK_TEXT : DarkUi.SEPIA);
            g.drawText(font, DarkUi.body(status(m)), x + 56, y + 28, DarkUi.SEPIA, false);
            rectsDrawn.put("member-" + i, new int[]{x, y, SEAT, SEAT});
        }
        for (int i = seats.size(); i < PARTY_SEATS; i++) { // the places still open, as the Banner draws an empty seat
            int x = nx + 16, y = ny + 16 + 56 * i;
            UiTex.draw(g, "table/frame-back-empty-48", x + 4, y + 4);
            UiTex.draw(g, "table/frame-rim-48", x, y);
            text(g, "open-" + i, DarkUi.heading("Open seat"), "Open seat", x + 56, y + 18, DarkUi.SEPIA);
            rectsDrawn.put("open-" + i, new int[]{x, y, SEAT, SEAT});
        }
        var rule = lines(g, PARTY_RULE, nx + RULE_X, ny + 28, RULE_W, 4, DarkUi.SEPIA); // four at most: clear of row 1's tags at 88
        textsDrawn.put("party-rule", String.join(" ", rule));
        rectsDrawn.put("party-rule", new int[]{nx + RULE_X, ny + 28, RULE_W, 13 * rule.size() - 4});
        if (!flag("party")) return;
        // who leads is said once, on the leader's seat ("You lead the party", or "Leader, online"): the seats hold every member
        // (a party has at most PARTY_SEATS), so this line does not name the leader again (UI Task 8b)
        String line = UiMath.trimWords(PARTY_LASTS, 328, s -> font.getWidth(DarkUi.body(s)));
        text(g, "party-line", DarkUi.body(line), line, nx + 16, ny + 240, DarkUi.SEPIA);
    }

    static String status(Seat m) {
        if (m.me()) return !flag("party") ? "No party yet" : m.leader() ? "You lead the party" : "You";
        String on = m.online() ? "online" : "offline";
        return m.leader() ? "Leader, " + on : m.online() ? "Online" : "Offline";
    }

    /** The private room's note. Out of a room (the short note): what a room is, then "Join a room" over the code's line (Create,
     *  the line and Join are widgets) and the room's rule. In a room (the tall note): its code in Minecraft's face (whose 0 and O,
     *  1 and I differ) and its host, each team's commanders (a check by those ready), the map in play, and how many are ready of
     *  how many (every commander readies before the host can start). Its tags are widgets. */
    void room(DrawContext g, int ox, int oy) {
        var font = font();
        int x = ox + ROOM_X, y = oy + ROOM_Y, lx = x + 16;
        String room = value("room"), note = room.isEmpty() ? "table/note-room-short" : "table/note-room";
        rectsDrawn.put("room", UiTex.draw(g, note, x, y));
        texturesDrawn.put("room", note);
        g.drawText(font, DarkUi.heading("Private room"), lx, y + 12, DarkUi.INK_TEXT, false);
        if (room.isEmpty()) {
            lines(g, "Your own battle, with friends on the teams you choose.", lx, y + 30, ROOM_W, 2, DarkUi.SEPIA);
            g.drawText(font, DarkUi.heading("Join a room"), lx, y + JOIN_HEAD_DY, DarkUi.INK_TEXT, false);
            // a party member who does not lead sees Create and Join dull: the word why, under Join, ahead of the rule (UI Task 8b)
            String why = UiMath.roomWhy(flag("party"), flag("partyLead"));
            int ruleY = y + RULES_DY;
            if (!why.isEmpty()) {
                var said = lines(g, why, lx, ruleY, ROOM_W, 2, DarkUi.SEPIA);
                textsDrawn.put("room-why", String.join(" ", said));
                colorsDrawn.put("room-why", DarkUi.SEPIA);
                rectsDrawn.put("room-why", new int[]{lx, ruleY, ROOM_W, 13 * said.size() - 4});
                ruleY += 13 * said.size() + 6;
            }
            lines(g, "Each commander readies; the host picks the map.", lx, ruleY, ROOM_W, 3, DarkUi.SEPIA);
            return;
        }
        var label = DarkUi.body("Code");
        g.drawText(font, label, lx, y + 30, DarkUi.SEPIA, false);
        text(g, "code", Text.literal(room), room, lx + font.getWidth(label) + 5, y + 32, DarkUi.INK_TEXT);
        g.drawText(font, DarkUi.body(flag("host") ? "You host this room" : "A friend hosts this room"), lx, y + 44, DarkUi.SEPIA, false);
        for (int t = 0; t < 2; t++) {
            int tx = lx + PAIR_STEP * t;
            DarkUi.teamMark(g, t, tx, y + TEAMS_DY + 2);
            g.drawText(font, DarkUi.heading(t == 0 ? "Dawn" : "Dusk"), tx + 10, y + TEAMS_DY, DarkUi.INK_TEXT, false);
        }
        int[] n = {0, 0};
        int ready = 0, all = 0;
        for (var e : rows("commanders")) {
            var r = e.getAsJsonObject();
            all++;
            if (flag(r, "ready")) ready++;
            int t = number(r, "team", -1);
            if (t < 0 || t > 1 || n[t] >= 4) continue;
            int tx = lx + PAIR_STEP * t, cy = y + NAMES_DY + 12 * n[t]++;
            if (flag(r, "ready")) UiTex.draw(g, "table/ready-check", tx, cy);
            boolean online = flag(r, "online"); // an offline commander carries the word, as the party note writes it
            var off = DarkUi.body("offline");
            int offW = online ? 0 : font.getWidth(off) + 4;
            String name = UiMath.trim(r.get("name").getAsString(), PAIR_W - 14 - offW, s -> font.getWidth(s));
            g.drawText(font, name, tx + 12, cy, online ? DarkUi.INK_TEXT : DarkUi.SEPIA, false);
            if (!online) g.drawText(font, off, tx + 16 + font.getWidth(name), cy - 2, DarkUi.SEPIA, false);
        }
        var mapLabel = DarkUi.body("Map");
        int mx = lx + font.getWidth(mapLabel) + 6;
        String map = Spec.MAPS[Math.max(0, Math.min(Spec.MAPS.length - 1, number("map", 0)))].name();
        map = UiMath.trim(map, lx + ROOM_W - mx, s -> font.getWidth(DarkUi.heading(s)));
        g.drawText(font, mapLabel, lx, y + MAP_DY, DarkUi.SEPIA, false);
        text(g, "room-map", DarkUi.heading(map), map, mx, y + MAP_DY, DarkUi.INK_TEXT);
        String readyLine = ready + " of " + all + " ready";
        text(g, "room-ready", DarkUi.body(readyLine), readyLine, lx, y + READY_LINE_DY, DarkUi.SEPIA);
    }

    /** The room code's line: the code in INK in Minecraft's face on a written line, with the caret while it is being written,
     *  and until the code is whole how much of it is written ("7 of 8", SEPIA) at the line's end; what to do, in SEPIA, until
     *  writing starts. */
    void paintCode(DrawContext g, int x, int y, int w, int h) {
        var font = font();
        var b = buttons.get("code");
        boolean hot = hot(b);
        g.fill(x, y + h - 3, x + w, y + h - 2, typing || hot ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        if (hot) underlinedDrawn.add("code");
        if (code.isEmpty() && !typing) g.drawText(font, DarkUi.body("Click to write the code"), x + 4, y + 7, DarkUi.SEPIA, false);
        else {
            var t = Text.literal(code);
            g.drawText(font, t, x + 4, y + 9, DarkUi.INK_TEXT, false);
            if (typing && ticks / 10 % 2 == 0) {
                int cx = x + 5 + font.getWidth(t);
                g.fill(cx, y + 7, cx + 1, y + 18, DarkUi.INK_TEXT);
            }
            if (code.length() < CODE_LENGTH) { // how much of the code is written, at the line's end: Join waits for a whole one
                String count = code.length() + " of " + CODE_LENGTH;
                text(g, "code-count", Text.literal(count), count, x + w - 4 - font.getWidth(count), y + 9, DarkUi.SEPIA);
            }
        }
        rectsDrawn.put("code", new int[]{x, y, w, h});
        textsDrawn.put("room-code", code);
    }

    // ======================================================================================================================= Field

    void initField() {
        int[] o = WarTable.origin();
        int ox = o[0], oy = o[1];
        for (int i = 0; i < Spec.MAPS.length; i++) {
            final int index = i;
            int y = oy + MAP_Y + MAP_STEP * i;
            String vote = "vote-" + i;
            add("map-" + i, new UiButton(ox + MAP_X, y, MAP_W, 28, Text.literal(Spec.MAPS[i].name()), b -> viewed = index, (g, x, by, w, h, hot) -> paintMapRow(g, index, x, by)));
            add(vote, new UiButton(ox + VOTE_X, y + 2, 64, 24, Text.literal("Vote"), b -> send("vote:" + index), (g, x, by, w, h, hot) -> vellum(g, vote, x, by, w, h, index == voted && CampaignClient.padMine >= 0 ? "Voted" : "Vote", false)));
        }
        for (int i = 0; i < TIER_IDS.length; i++) {
            String id = TIER_IDS[i], key = "bots-" + id, label = CampaignClient.tierName(id);
            add(key, new UiButton(ox + TIER_X[i], oy + BOTS_Y, TIER_W[i], 24, Text.literal(label), b -> send("difficulty:" + id), (g, x, y, w, h, hot) -> vellum(g, key, x, y, w, h, label, id.equals(CampaignClient.difficulty))));
        }
    }

    /** Vote takes a press only on the war gates with no battle running (the server's rule); the bots' tier while none runs. */
    void refreshField() {
        boolean gate = CampaignClient.padMine >= 0 && !GravesAndGloryClient.running;
        for (int i = 0; i < Spec.MAPS.length; i++) {
            var b = buttons.get("vote-" + i);
            if (b != null) b.active = gate;
        }
        for (String id : TIER_IDS) {
            var b = buttons.get("bots-" + id);
            if (b != null) b.active = !GravesAndGloryClient.running;
        }
    }

    /** The sheet: the map rows (widgets, paintMapRow), the war gates' line and the vote's rule under them; the card of the map in
     *  view with its name and description, and the bots' heading over their tiers (widgets) under it. */
    void field(DrawContext g, int ox, int oy) {
        var font = font();
        rectsDrawn.put("sheet", UiTex.draw(g, "table/sheet-field", ox + FIELD_X, oy + FIELD_Y));
        int index = Math.max(0, Math.min(Spec.MAPS.length - 1, viewed));
        var map = Spec.MAPS[index];
        String card = "table/map-" + map.id(); // each map's painted card (tools/ui_art/kit_maps.py)
        boolean painted = UiTex.has(card), seals = !painted && index == CampaignMaps.selected;
        if (painted) rectsDrawn.put("card", UiTex.draw(g, card, ox + CARD_X, oy + CARD_Y));
        else if (seals) WarTableHome.seals(g, ox + CARD_X, oy + CARD_Y); // no card yet (a defect): home's fallback, the built map's seals
        texturesDrawn.put("card", painted ? card : seals ? "seals" : "none");
        WarTableHome.centred(g, DarkUi.heading(map.name()), ox + CARD_CX, oy + CARD_Y + 92, DarkUi.INK_TEXT);
        textsDrawn.put("card-name", map.name());
        var desc = UiMath.lines(map.description(), CARD_TEXT_W, 3, s -> font.getWidth(DarkUi.body(s)));
        for (int i = 0; i < desc.size(); i++) WarTableHome.centred(g, DarkUi.body(desc.get(i)), ox + CARD_CX, oy + CARD_Y + 110 + 13 * i, DarkUi.SEPIA);
        boolean gate = CampaignClient.padMine >= 0;
        String status = gate ? "War gates: Dawn " + CampaignClient.padDawn + " / " + CampaignClient.padSize + ", Dusk " + CampaignClient.padDusk + " / " + CampaignClient.padSize
            + (CampaignClient.padCountdown > 0 ? ", " + CampaignClient.padCountdown + " s" : "") : "Stand on the war gates to vote";
        text(g, "status", DarkUi.body(status), status, ox + MAP_X, oy + STATUS_Y, gate ? DarkUi.INK_TEXT : DarkUi.SEPIA);
        g.drawText(font, DarkUi.body("Most votes wins; a tie goes to the map listed first."), ox + MAP_X, oy + STATUS_Y + 13, DarkUi.SEPIA, false);
        WarTableHome.centred(g, DarkUi.heading("Bots"), ox + CARD_CX, oy + BOTS_HEAD_Y, DarkUi.INK_TEXT);
        textsDrawn.put("bots-current", CampaignClient.tierName(CampaignClient.difficulty));
    }

    /** A map's row: its name, INK while a vote can be cast and SEPIA while not, a tack by the map whose card shows; a click shows
     *  its card. */
    void paintMapRow(DrawContext g, int i, int x, int y) {
        var font = font();
        var b = buttons.get("map-" + i);
        int ink = CampaignClient.padMine >= 0 && !GravesAndGloryClient.running ? DarkUi.INK_TEXT : DarkUi.SEPIA;
        if (i == viewed) UiTex.draw(g, "table/tack", x + 2, y + 11);
        String name = UiMath.trim(Spec.MAPS[i].name(), MAP_W - 20, s -> font.getWidth(DarkUi.heading(s)));
        g.drawText(font, DarkUi.heading(name), x + 14, y + 8, ink, false);
        if (i != viewed && hot(b)) underline(g, "map-" + i, x + 14, y + 20, font.getWidth(DarkUi.heading(name)), ink);
        rectsDrawn.put("map-" + i, new int[]{x, y, MAP_W, 28});
        textsDrawn.put("map-" + i, name);
        colorsDrawn.put("map-" + i, ink);
    }
}
