package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The War Table's other pages (UI plan Task 7), each on its own stretch of the oak under the back rail: Banner (the owned roll,
 *  the twenty deployment seats on their sheet and the commander's role), Contracts (the three dailies, the reroll and the
 *  weekly), Chapter (the Founders reward track, as Kort approved it on 2026-10-18: the hero letter, the chapter block with the XP
 *  rail and the Claim plate, and the track of plaques in two rows), Friends (the camp's commanders, the party, invitations and
 *  the private room) and the Field (each map's painted card, and the bots' tier). Each page has the back plaque
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
    static final Map<String, String> TITLES = Map.of("banner", "Your Banner", "contracts", "Contracts", "chapter", "Founders Season Pass", "friends", "Friends", "field", "The Field");
    static final int TITLE_X = 480, TITLE_Y = 84;
    // The rail's camp plaques are home's (WarTableHome.RAIL, RAIL_X, RAIL_LABELS, RAIL_W): one source, no copy here.
    /** The notice line: its first line's top, 12 px under the lowest content of every page; its lines 13 px apart, centred on the
     *  page, as wide as home's and at most two (the second ends 12 px above the page's foot). On the Chapter it is right-aligned
     *  to the track's right end at top 500, opposite the Oath's line. */
    static final int NOTICE_Y = 504, NOTICE_STEP = 13, NOTICE_W = 440, NOTICE_LINES = 2;
    static final int CHAPTER_NOTICE_R = 888, CHAPTER_NOTICE_Y = 500, CHAPTER_NOTICE_W = 400;

    // Banner: the roll (recruit/muster-roll, rows as the recruit page has them), each row's Add and Remove tags, the sheet and its
    // 5 x 4 seats (front row first), the armed troop's band under the seats, and the commander's roles on the right.
    static final int ROLL_X = 56, ROLL_Y = 124, ROW_TOP = 140, ROW_H = 56, ROWS = 6, ROW_W = 168, NAME_W = 104;
    static final int TAG_X = 228, TAG_W = 60, TAG_H = 22, ADD_DY = 4, REMOVE_DY = 30;
    static final int SHEET_X = 328, SHEET_Y = 124, SEAT_X = 388, SEAT_Y = 180, SEAT_STEP = 56, SEAT = 48, SEATS = 20;
    static final int ARMED_X = 388, ARMED_Y = 412, CLEAR_X = 596, CLEAR_Y = 424;
    static final int CLASS_X = 760, CLASS_Y = 140, CLASS_STEP = 40, CLASS_W = 120, BONUS_Y = 374, BONUS_W = 144;
    /** What a seat click places when the Clear tag is armed: the server's word for an empty seat. */
    static final String EMPTY = "empty";
    /** In the roll's last place, at its foot, while the House has troops not yet recruited (their places ruled and empty above
     *  it): where they come from, centred on the rows. */
    static final String ROLL_HINT = "More of your House's troops join the roll at the recruit table.";
    static final String ROLL_LOCKED = "A battle is on: the banner is locked until it ends.";
    static final int ROLL_HINT_CX = 116, ROLL_HINT_W = 200;
    /** The Banner's House row sits in the sheet's head: the chosen House shows seal, tick and name, the other four their seal only
     *  (five names are 414 px wide and the sheet's paper holds 372); each chip's tooltip names its House. */
    static final int BANNER_CHIPS_DY = 10, BANNER_CHIPS_CX = 202, BANNER_HEAD_DY = 44, BANNER_PAPER_X = 20, BANNER_PAPER_W = 364;
    /** The banner colour row, in the sheet's head line between the side's name and the seat count (colour:<side> sets what the commander
     *  asks to fly): a "Colour" word and one swatch chip per side, each 24 x 14, from the sheet's x 192 and 2 px above the head line, so the
     *  row lies in the 14 px between the House chips and the first seat row. */
    static final int COLOUR_LABEL_X = 150, COLOUR_X = 192, COLOUR_DY = -2, COLOUR_W = 24, COLOUR_H = 14, COLOUR_GAP = 3;

    // Contracts: the three daily notes in a row centred on the page, Reroll under the middle one, the weekly note under it.
    static final int NOTE_X = 140, NOTE_Y = 140, NOTE_STEP = 240, REROLL_X = 436, REROLL_Y = 252, WEEKLY_X = 328, WEEKLY_Y = 300, RESET_Y = 412;
    /** A daily note keeps a second reward line for the shards of the daily that completes all three, so its bar, count and Claim sit
     *  lower than the weekly's (which pays its shards on its one line): offsets from the note's top. */
    static final int DAILY_BAR_DY = 56, DAILY_STAMP_DY = 64, DAILY_CLAIM_DY = 66;
    /** The House row (claims with a House, War Table port Task 1): five chips 24 tall, seal then name, 3 px apart, the House the
     *  shards of the next claim go to pressed in with a tick, a rule under it and its word in the reward line. The Contracts row is
     *  centred on the page under the reset line, the Chapter's starts at the chapter block's text column under its name. */
    static final int CAPTION_GAP = 10;
    static final int CHIP_H = 24, CHIP_GAP = 3, CHIP_SEAL = 16, CHIP_PAD = 5, CHIP_SEAL_GAP = 4, CHIP_RULE = 5, CHIPS_Y = 436, CHAPTER_CHIPS_Y = 110;
    /** The Chapter's Rare chip (tier 20) and the lines of what the waiting tier gives, both left of the Claim plate. */
    static final int RARE_W = 124, GIVES_W = 170, GIVES_GAP = 10;

    // Chapter (work/ui-concepts/track/pages.py): ten tiers shown, plaques 68 x 80 on a 76 pitch, the current tier's Roll plaque
    // lifted 8 px and 88 tall; the rod, the tier tags on it, the Roll's top and the Oath's top; the hero letter and the Claim plate.
    static final int TIERS = Progression.TIERS, SHOWN = 10, EVERY = 5;
    /** Where tier t sits in Progression.TITLE_TIERS (Phase 2's chapter: titles at 5, 10, 15, 25 and 30, no Vanguard), or -1 for a tier with no title. */
    static int titleAt(int t) { for (int i = 0; i < Progression.TITLE_TIERS.length; i++) if (Progression.TITLE_TIERS[i] == t) return i; return -1; }
    /** The first title tier at or past t (the last one when t is beyond them): the tier the hero letter shows. */
    static int titleFrom(int t) { for (int x : Progression.TITLE_TIERS) if (x >= t) return x; return Progression.TITLE_TIERS[Progression.TITLE_TIERS.length - 1]; }
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
    // war council's short note on the right (the room itself is run from the war council page, WarTableLobby), with the tag that
    // opens it. Each commander in the camp has an Invite tag and, when holding an open room, a Join tag. Offsets from the note's top.
    static final int CAMP_X = 56, CAMP_W = 228, INVITE_Y = 144, INVITE_STEP = 32, INVITES = 4, PARTY_SEATS = 4, JOIN_W = 60;
    static final int PARTY_X = 300, PARTY_Y = 124, PARTY_NAME_W = 108, ROOM_X = 676, ROOM_Y = 124, ROOM_W = 196, CODE_LENGTH = 8;
    static final int CREATE_DY = 76, ROOM_TAG_DY = 32; // the war council tag sits right under its heading while no room is open
    /** The party's rule, beside the seats level with the first (the commander's own, which never carries a tag) and on the
     *  column of the leader's tags, and the camp column's line on how friends reach a camp this machine runs: how to share it,
     *  or, once it is open on the network, where friends find it. */
    static final int RULE_X = 188, RULE_W = 156;
    static final String PARTY_RULE = "A party of up to four fights as one team.";
    /** Under the seats while in a party. */
    static final String PARTY_LASTS = "The party lasts through matches and restarts.";
    static final String CAMP_HOW = "Host a camp on this network from Play with friends on the title screen; friends pick Join a server.";
    static final String CAMP_OPEN = "Open on your network: friends pick Play with friends, then Join a server on the title screen.";

    // Field: the sheet; on its left a row per map (its name) and under them the line that says the war council picks the map;
    // on its right the card of the map in view with its name and description, and under them the bots' tier, its three tags
    // centred under the card on the band of that line. Both columns start 32 px under the sheet's top and end on
    // that band, 40 px above its foot.
    static final int FIELD_X = 220, FIELD_Y = 124, MAP_X = 236, MAP_Y = 156, MAP_STEP = 44, MAP_W = 176, STATUS_Y = 380;
    static final int CARD_X = 544, CARD_Y = 156, CARD_CX = 612, CARD_TEXT_W = 208, BOTS_HEAD_Y = 362, BOTS_Y = STATUS_Y;
    /** The Statuses paper (polish T7), standing right of the sheet on the same parchment (a cut of sheet-field), the sheet's height less its torn rim: 140 wide so the compact page's right edge holds it 12 px clear; a row per status with its glyph, name and line. */
    static final int GLOSS_X = FIELD_X + 534, GLOSS_Y = FIELD_Y, GLOSS_W = 140, GLOSS_H = 320, FIELD_DX = -77, GLOSS_ROW_Y = 34, GLOSS_STEP = 46, GLOSS_LINES = 3, GLOSS_GAP = 7;
    static final String[] TIER_IDS = {"easy", "normal", "hard"};
    static final int[] TIER_X = {504, 576, 656}, TIER_W = {64, 72, 64};

    /** The compact page (UI Task 8b-2, WarTable.compact; Kort, 2026-10-22: everything fits, no crop): each column's offset from
     *  its place on the 960 page, so a box at page (x, y) lands at the compact page's (x + dx, y + dy). Banner: the roll, the sheet
     *  and the roles close up (roll box at 27, sheet at 288, roles at 698) and rise 36 px; the notice stands in the role
     *  column under the bonus. Contracts and the Field move as one, the notice under them. Friends: the camp column at 12 and 200
     *  wide, the party and the room close up beside it; the notice under the party's note. Chapter: the hero letter at the top
     *  left (its title seal on the letter, clear of the rail), the chapter block, the XP rail and Claim right of it with the
     *  notice between the XP line and Claim, and the track under both, nine tiers shown, the labels at the page's edge, "no oaths"
     *  in two even lines under The Oath in place of "not sworn"; the letter and the chapter block 4 px under the rail. */
    static final int[] C_ROLL = {-29, -36}, C_SHEET = {-40, -36}, C_CLASS = {-62, -36}, C_CONTRACTS = {-55, -40}, C_FIELD = {-130, -36};
    static final int[] C_CAMP = {-44, -36}, C_PARTY = {-76, -36}, C_ROOM = {-76, -36}, C_HERO = {-44, -2}, C_BLOCK = {-65, -10}, C_BLOCK_HEAD = {-60, -10};
    static final int C_CAMP_W = 200, C_BANNER_NOTICE_Y = 396, C_BANNER_NOTICE_LINES = 4, C_CONTRACTS_NOTICE_Y = 436, C_FIELD_NOTICE_Y = 425;
    static final int C_FRIENDS_NOTICE_Y = 400, C_FRIENDS_NOTICE_CX = 406, C_FRIENDS_NOTICE_W = 340, C_CHAPTER_NOTICE_Y = 188;
    /** The compact Chapter's track, in compact page px: nine tiers from x 150, the rod at 276, the Roll at 300 and the Oath at 388
     *  (its foot 12 px above the page's), the labels at x 12; and the hero letter's title seal on the letter's top right. */
    static final int C_SHOWN = 9, C_X0 = 148, C_ROD_Y = 276, C_TAG_Y = 274, C_ROLL_TOP = 300, C_OATH_TOP = 388, C_LABEL_X = 12, C_SEAL_DX = 350, C_SEAL_DY = 4;

    static int[] col(int[] c) { return WarTable.column(c[0], c[1]); }

    /** The Chapter's track on the page shown: the first plaque's x, the rod's top, the tags' top, the Roll's and the Oath's tops,
     *  the tiers shown and the labels' x, in canvas px. */
    record Track(int x0, int rodY, int tagY, int rollTop, int oathTop, int shown, int labelX) {}

    static Track track() {
        if (WarTable.compact()) {
            int[] c = WarTable.compactOrigin();
            return new Track(c[0] + C_X0, c[1] + C_ROD_Y, c[1] + C_TAG_Y, c[1] + C_ROLL_TOP, c[1] + C_OATH_TOP, C_SHOWN, c[0] + C_LABEL_X);
        }
        int[] o = WarTable.origin();
        return new Track(o[0] + X0, o[1] + ROD_Y, o[1] + TAG_Y, o[1] + ROLL_TOP, o[1] + OATH_TOP, SHOWN, o[0] + 56);
    }

    static int shown() { return WarTable.compact() ? C_SHOWN : SHOWN; }

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

    /** Boxes {x,y,w,h} in canvas px as drawn: table and rail (drawChrome's), here (the lit plaque), title, notice; Banner:
     *  roll, mark, row-(i), roll-hint, sheet, seat-(i), armed-seat, role-(id); Contracts: note-(i), progress-(i) (the count),
     *  stamp-(i), weekly, progress-weekly, stamp-weekly, reroll; Chapter: hero, seal, xp-rail, roll-(t) and oath-(t) for each
     *  tier on the track (t the tier), claim; Friends: party, member-(i), party-rule, room, room-why, code; Field: sheet, card, map-(i);
     *  and every vellum tag by its widget's name. */
    static Map<String, int[]> rects() { return rects; }
    /** Words drawn: here, title, notice; Banner: row-(i) (the troop id), count-(i), roll-hint, seat-(i) (the troop id, or empty),
     *  armed, armed-hint, seated, bonus, role-(id); Contracts: contract-(i), progress-(i), weekly, progress-weekly, reroll-hint,
     *  reset; Chapter: tier, first (the first tier on the track), xp, to-next, hero (its tier), hero-name, hero-state, roll-(t)
     *  and oath-(t) (their two lines), unsworn; Friends: camp-how, invite-hint, invitation, member-(i),
     *  party-rule, party-line, room-why, code, room-code, code-count; Field: map-(i), card-name, status, bots-current; every vellum tag's
     *  label by its widget's name. */
    static Map<String, String> texts() { return texts; }
    /** The texture each part was drawn from: table, here, role-(id), claim (the Claim plate), roll-(t), stamp-(t), stamp-(i),
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
    /** Banner drag and drop, all in GUI coordinates (as the mouse events come): the press that may become a drag (on a roll row, or on a
     *  seat, whose click waits for the release), the troop and the seat it came from, the pointer, and where the card was grabbed (canvas px). */
    boolean pressed, dragging, moved, dragFromRoll;
    int pressSeat = -1, dragSeat = -1;
    String dragTroop = "";
    double pressX, pressY, curX, curY, grabX, grabY;
    /** The banner the drag began on, and what every seat would do with a drop of the card (fromRoll or move), and the roll (unseat). */
    List<String> dragBase = List.of();
    final BannerDrop.Plan[] seatPlan = new BannerDrop.Plan[SEATS];
    BannerDrop.Plan unseatPlan;
    /** A drop in flight: the plan's commands go out one by one, each once the banner shows the step before; the banner it ends on
     *  stands in for the server's while a plan has several steps, so the seats do not flicker. */
    BannerDrop.Plan pend;
    List<String> pendBase = List.of();
    int pendStep;
    long pendAt;
    static List<String> optimistic;
    /** The card's flight at the end of a drag: from the pointer to the seat it lands on, or back to where it was lifted (canvas px). */
    long flyAt;
    double flyFromX, flyFromY, flyToX, flyToY;
    boolean flyBack;
    String dropWhy = "";
    static final int DRAG_START = 6, PEND_TICKS = 100, FLY_MS = 150;
    /** Friends: the first commander of the camp's list shown. */
    int offset;
    /** Field: the map whose card shows. */
    int viewed;
    /** Chapter: whether the arrows moved the track off its place, and where to; the first tier the last tick showed; the tier this
     *  page last claimed (its stamp is the wet one); and the tick the track began to slide. */
    boolean paged;
    int pagedFirst = 1, shownFirst = -1, freshTier = -1;
    /** The tooltips the Claim plate and the Rare chip carry now (set again only when their words change). */
    String claimTip = "", rareTip = "";
    double slideAt = -1000;

    WarTablePages(CampScreen screen, String page) {
        this.screen = screen;
        this.page = page;
        noticeSerialSeen = CampaignClient.noticeSerial;
        optimistic = null;
        viewed = Math.max(0, Math.min(Spec.MAPS.length - 1, CampaignClient.map));
    }

    /** Opens a page: on the War Table already open, or on a new one. */
    static void open(String page) {
        var c = MinecraftClient.getInstance();
        if (c.currentScreen instanceof CampScreen s) s.page(page);
        else c.setScreen(new CampScreen(page));
    }

    /** The widget a name stands for, or null: back, recruit, banner, contracts, chapter, friends (the rail's, all but the open
     *  page's); Banner: row-(i), add-(i), remove-(i), seat-(i), clear; Contracts: claim-(i), reroll, claim-weekly;
     *  Chapter: claim, earlier, later, title-(t); Friends: invite-(k), more, accept, decline, lead-(i),
     *  kick-(i), create, leave, disband, claim-lead, join-(k) (Join on a friend's row), council; Field: map-(i), bots-(tier). */
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
            add(target, WarTable.plaque(WarTableHome.RAIL_X[i], WarTableHome.RAIL_W, WarTableHome.RAIL_ICONS[i], WarTableHome.RAIL_LABELS[i], null, 0, badge, b -> screen.page(target)));
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
        }
        if (page.equals("chapter")) slide();
        if (page.equals("banner")) dragTick();
        refresh();
    }

    void back() { UiSounds.back(); screen.page("home"); }

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
            if (pressed || dragging) cancelDrag();
            else back();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
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

    /** A click with the other mouse button: on a seat of the Banner it empties the seat. */
    boolean secondaryClick(double x, double y) {
        if (!page.equals("banner")) return false;
        if (pressed || dragging) { cancelDrag(); return true; }
        for (int i = 0; i < SEATS; i++) {
            var b = buttons.get("seat-" + i);
            if (b != null && b.visible && b.active && b.overArt(x, y)) { place(i, EMPTY); return true; }
        }
        return false;
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
        if (b != null && !b.active && !current) DarkUi.disabledVeil(g, x + 1, y + 1, w - 1, h); // a tag that cannot be pressed is veiled as well as sepia (4F.5)
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
            rectsDrawn.putAll(WarTable.drawn()); // table and rail, as drawChrome drew them
            texturesDrawn.put("table", WarTable.drawnTable());
            here(g);
            title(g, ox, oy);
            switch (page) {
                case "banner" -> banner(g);
                case "contracts" -> { int[] c = col(C_CONTRACTS); contracts(g, c[0], c[1]); }
                case "chapter" -> chapter(g);
                case "friends" -> friends(g);
                default -> { int[] c = fieldCol(); field(g, c[0], c[1]); }
            }
            notice(g, ox, oy);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** The open page's plaque on the rail, lit (plaque-112-here, its name in RAIL_KEY): the page you are on. */
    void here(DrawContext g) {
        int i = Arrays.asList(WarTableHome.RAIL).indexOf(page);
        if (i < 0) return;
        int x = WarTable.railX(WarTableHome.RAIL_X[i]);
        UiTex.draw(g, "table/plaque-112-here", x, 12);
        rectsDrawn.put("here", new int[]{x, 12, WarTableHome.RAIL_W, 28}); // the plaque's box, as the rail's widgets have theirs (the PNG adds a shadow)
        String icon = "table/" + WarTableHome.RAIL_ICONS[i]; // built, as WarTable.plaque builds it
        UiTex.draw(g, icon, x + 8, 19);
        String label = WarTableHome.RAIL_LABELS[i];
        text(g, "here", DarkUi.heading(label), label, x + 28, 20, DarkUi.RAIL_KEY);
        texturesDrawn.put("here", "table/plaque-112-here");
    }

    /** The page's focal title, IVORY with its shadow, centred on the page at TITLE_Y; none on the Chapter (its chapter block). On a
     *  canvas shorter than the page (the compact page, 854x480) TITLE_Y would fall under the rail's moulding, so the title keeps
     *  to the planks just under the rail. */
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

    /** The server's notice (a refusal, a claim's reward) in IVORY, on two lines when one is too short, shown
     *  WarTableHome.NOTICE_TICKS and then faded over NOTICE_FADE_TICKS, as home's and the recruit page's are. */
    void notice(DrawContext g, int ox, int oy) {
        long age = ticks - noticeAt;
        if (noticeText.isEmpty() || age >= WarTableHome.NOTICE_TICKS + WarTableHome.NOTICE_FADE_TICKS) return;
        var font = font();
        int[] at = noticePlace(ox, oy); // {anchor x, top, width, lines, 0 centred / 1 right-aligned / -1 left from the anchor}
        float fade = age < WarTableHome.NOTICE_TICKS ? 1 : 1 - (age - WarTableHome.NOTICE_TICKS) / (float) WarTableHome.NOTICE_FADE_TICKS;
        var lines = UiMath.lines(HintText.expand(noticeText, GravesAndGloryClient::hint), at[2], at[3], s -> font.getWidth(DarkUi.body(s)));
        if (lines.isEmpty()) return;
        int alpha = Math.max(4, Math.round(fade * 255)), ink = (alpha << 24) | (DarkUi.IVORY & 0xffffff);
        int left = Integer.MAX_VALUE, end = Integer.MIN_VALUE, top = at[1];
        for (int i = 0; i < lines.size(); i++) {
            var t = DarkUi.body(lines.get(i));
            int w = font.getWidth(t), x = at[4] > 0 ? at[0] - w : at[4] < 0 ? at[0] : at[0] - w / 2;
            g.drawText(font, t, x, top + NOTICE_STEP * i, ink, false);
            left = Math.min(left, x);
            end = Math.max(end, x + w);
        }
        rectsDrawn.put("notice", new int[]{left, top, end - left, NOTICE_STEP * (lines.size() - 1) + 11});
        textsDrawn.put("notice", String.join(" ", lines));
        colorsDrawn.put("notice", ink);
    }

    /** The notice line's place on this page: {anchor x, top, width, lines, 0 centred on the anchor / 1 right-aligned to it / -1
     *  left from it}. The 960 page centres it on 480 under the content (the Chapter right-aligns it to the track's end); the
     *  compact page puts it where its page has room (the C_..._NOTICE constants). */
    int[] noticePlace(int ox, int oy) {
        if (!WarTable.compact()) return page.equals("chapter") ? new int[]{ox + CHAPTER_NOTICE_R, oy + CHAPTER_NOTICE_Y, CHAPTER_NOTICE_W, NOTICE_LINES, 1} : new int[]{ox + 480, oy + NOTICE_Y, NOTICE_W, NOTICE_LINES, 0};
        int[] c = WarTable.compactOrigin();
        return switch (page) {
            case "banner" -> new int[]{col(C_CLASS)[0] + CLASS_X, c[1] + C_BANNER_NOTICE_Y, BONUS_W, C_BANNER_NOTICE_LINES, -1};
            case "contracts" -> new int[]{c[0] + WarTable.COMPACT_W / 2, c[1] + C_CONTRACTS_NOTICE_Y, NOTICE_W, NOTICE_LINES, 0};
            case "chapter" -> new int[]{col(C_BLOCK)[0] + 904, c[1] + C_CHAPTER_NOTICE_Y, CHAPTER_NOTICE_W, NOTICE_LINES, 1};
            case "friends" -> new int[]{c[0] + C_FRIENDS_NOTICE_CX, c[1] + C_FRIENDS_NOTICE_Y, C_FRIENDS_NOTICE_W, NOTICE_LINES, 0};
            default -> new int[]{c[0] + WarTable.COMPACT_W / 2, c[1] + C_FIELD_NOTICE_Y, NOTICE_W, NOTICE_LINES, 0};
        };
    }

    // ====================================================================================================================== Banner

    /** The House the commander fights for, as the old army page took it: the snapshot's team, Dawn before a team is known. */
    static int team() { return Math.max(0, Math.min(Spec.FACTIONS.length - 1, GravesAndGloryClient.team)); }

    /** The troops of the shown House he has recruited, in roll order (at most ROWS: a House holds six troops). */
    static List<RecruitView.Row> owned() {
        List<RecruitView.Row> out = new ArrayList<>();
        for (var r : RecruitView.roll(shownHouse())) if (r.owned() && out.size() < ROWS) out.add(r);
        return out;
    }

    /** The banner as the server will have it once the commands already sent or queued land: the snapshot's, with each Add, Remove and
     *  seat click applied (BannerDrop.apply, the server's own rule) until the snapshot catches up or PLANNED_MS pass. A click resolves its
     *  seat from this, so rapid clicks before the next snapshot do not all pick the same stale seat. */
    static List<String> line() {
        var snap = CampaignClient.profile.banner;
        if (planned != null && (planned.equals(snap) || System.currentTimeMillis() - plannedAt > PLANNED_MS)) planned = null;
        return planned != null ? planned : snap;
    }
    private static List<String> planned;
    private static long plannedAt;
    private static final long PLANNED_MS = 2500;
    /** Notes a slot command just sent or queued: the projected banner moves on by it. */
    static void planSlot(int slot, String id) { planned = BannerDrop.apply(line(), slot, id, dropRules()); plannedAt = System.currentTimeMillis(); }

    /** The copies of a troop in the deployment. */
    static int copies(String troop) { return java.util.Collections.frequency(CampaignClient.profile.banner, troop); }
    /** The seats a troop takes in the deployment: each copy takes its slot weight (an Epic two, a Legendary four). */
    static int seats(String troop) { return copies(troop) * weight(troop); }
    /** The seats one copy of a troop takes (the troops sheet's slotWeight, at least 1). */
    static int weight(String troop) { return Math.max(1, RosterMath.Catalog.fromSpec().weight(troop)); }
    /** The seats the banner uses: every seat that is not empty, the later halves of wide troops (MARKER) included. */
    static int used() { int n = 0; for (var id : CampaignClient.profile.banner) if (!id.equals(RosterMath.EMPTY)) n++; return n; }
    /** The seat a wide troop starts in when seat i is one of its later halves, else i itself. */
    static int unitSeat(int i) {
        var line = line();
        int j = Math.min(i, line.size() - 1);
        while (j > 0 && RosterMath.MARKER.equals(line.get(j))) j--;
        return Math.max(0, j);
    }
    /** The first seat of a run of w free seats in a row, or -1 when no run is long enough. */
    static int firstRun(int w) {
        var line = line();
        for (int i = 0; i + w <= line.size(); i++) {
            boolean run = true;
            for (int k = 0; k < w && run; k++) run = line.get(i + k).equals(EMPTY);
            if (run) return i;
        }
        return -1;
    }
    /** Why Add would be refused for this troop (the server's own rules, RosterMath), or "" when it would take. */
    static String addWhy(String troop) {
        var c = RosterMath.Catalog.fromSpec();
        String rarity = c.rarityOf(troop);
        if (rarity != null) {
            int units = 0;
            for (var id : line()) if (rarity.equals(c.rarityOf(id))) units++;
            int max = RosterMath.bannerMax(rarity);
            if (units >= max) return "A banner holds " + max + " " + rarity + " troop" + (max == 1 ? "" : "s") + ".";
        }
        int w = weight(troop);
        if (firstRun(w) < 0) return w > 1 ? "No " + w + " free seats together." : "Every seat is taken.";
        return "";
    }
    /** The troop a seat shows: a wide troop's later half shows the troop, an empty seat EMPTY. */
    static String drawnTroop(int i) {
        var line = shownLine();
        String id = i < line.size() ? line.get(i) : EMPTY;
        if (!id.equals(RosterMath.MARKER)) return id;
        String head = line.get(BannerDrop.head(line, i));
        return RosterMath.MARKER.equals(head) ? EMPTY : head;
    }
    /** The banner the seats show: the one a drop in flight ends on while it has steps to go, else the profile's. */
    static List<String> shownLine() { return optimistic != null ? optimistic : CampaignClient.profile.banner; }
    /** Integration: the seats the banner fills (not empty, not a marker of a wide troop). */
    static int filled() { int n = 0; for (var id : CampaignClient.profile.banner) if (!id.equals(RosterMath.EMPTY) && !id.equals(RosterMath.MARKER)) n++; return n; }

    static String troopName(String id) {
        var s = ArmyCards.spec(id);
        return s == null ? id : s.name();
    }

    void initBanner() {
        int[] r = col(C_ROLL), sh = col(C_SHEET);
        for (var h : RecruitView.houses()) { // the House row first, so Tab meets it first; layoutBannerChips puts the chips in their places
            final String id = h.id(), name = h.name();
            var chip = add("house-" + id, new UiButton(sh[0] + SHEET_X, sh[1] + SHEET_Y, 26, CHIP_H, Text.literal(name), bb -> { bannerPick = id; refresh(); }, (g, bx, by, bw, bh, hot) -> paintBannerChip(g, id, name, bx, by, bw, bh)));
            chip.setTooltip(Tooltip.of(Text.literal("Troops of " + name)));
        }
        for (int i = 0; i < Spec.FACTIONS.length; i++) { // the banner colour: after the House row, before the roll
            final var side = Spec.FACTIONS[i];
            var chip = add("colour-" + side.id(), new UiButton(sh[0] + SHEET_X + COLOUR_X + i * (COLOUR_W + COLOUR_GAP), sh[1] + SHEET_Y + BANNER_HEAD_DY + COLOUR_DY, COLOUR_W, COLOUR_H,
                Text.literal(side.name()), bb -> send("colour:" + side.id()), (g, bx, by, bw, bh, hot) -> paintColour(g, side, bx, by, bw, bh)));
            chip.setTooltip(Tooltip.of(Text.literal("Fly the " + side.name() + ", unless the other team flies it first")));
        }
        for (int i = 0; i < ROWS; i++) {
            final int index = i;
            int y = r[1] + ROW_TOP + ROW_H * i;
            String add = "add-" + i, remove = "remove-" + i;
            add("row-" + i, new UiButton(r[0] + ROLL_X, y, ROW_W, ROW_H, Text.literal(""), b -> arm(index), (g, x, by, w, h, hot) -> rowHot(g, index, x, by)));
            add(add, new UiButton(r[0] + TAG_X, y + ADD_DY, TAG_W, TAG_H, Text.literal("Add"), b -> army(index, "add"), (g, x, by, w, h, hot) -> vellum(g, add, x, by, w, h, "Add", false)));
            add(remove, new UiButton(r[0] + TAG_X, y + REMOVE_DY, TAG_W, TAG_H, Text.literal("Remove"), b -> army(index, "remove"), (g, x, by, w, h, hot) -> vellum(g, remove, x, by, w, h, "Remove", false)));
        }
        for (int i = 0; i < SEATS; i++) {
            final int seat = i;
            add("seat-" + i, new UiButton(sh[0] + SEAT_X + SEAT_STEP * (i % 5), sh[1] + SEAT_Y + SEAT_STEP * (i / 5), SEAT, SEAT, Text.literal("Seat " + (i + 1)), b -> place(seat, armed), (g, x, y, w, h, hot) -> seatHot(g, seat, x, y)));
        }
        tag("clear", sh[0] + CLEAR_X, sh[1] + CLEAR_Y, 64, "Clear", () -> armed = EMPTY);
    }

    /** The rows follow the roll; Add while a seat is empty, Remove while the troop holds one; Clear while a troop is armed. The armed troop falls back to the roll's first when it leaves the roll. */
    void refreshBanner() {
        var roll = owned();
        if (armed == null || !armed.equals(EMPTY) && roll.stream().noneMatch(r -> r.troopId().equals(armed))) armed = roll.isEmpty() ? EMPTY : roll.get(0).troopId();
        boolean free = battleFree();
        layoutBannerChips();
        for (int i = 0; i < ROWS; i++) {
            boolean shown = i < roll.size();
            String troop = shown ? roll.get(i).troopId() : "";
            var row = buttons.get("row-" + i);
            var add = buttons.get("add-" + i);
            var remove = buttons.get("remove-" + i);
            if (row != null) { row.visible = shown; row.setMessage(Text.literal(shown ? roll.get(i).name() : "")); }
            String why = !shown ? "" : !free ? "Not while a battle is on." : addWhy(troop);
            if (add != null) {
                add.visible = shown; add.active = shown && free && why.isEmpty();
                String tip = !shown ? "" : add.active ? (weight(troop) > 1 ? "Take " + weight(troop) + " free seats together" : "Take the first free seat") : why;
                if (!tip.equals(addTips[i])) { addTips[i] = tip; add.setTooltip(tip.isEmpty() ? null : Tooltip.of(Text.literal(tip))); }
            }
            if (remove != null) { remove.visible = shown; remove.active = shown && free && copies(troop) > 0; }
        }
        for (int i = 0; i < SEATS; i++) {
            var s = buttons.get("seat-" + i);
            if (s != null) s.active = free;
        }
        var clear = buttons.get("clear");
        if (clear != null) clear.active = !EMPTY.equals(armed);
        for (var side : Spec.FACTIONS) { var chip = buttons.get("colour-" + side.id()); if (chip != null) chip.active = free; } // the server holds colour: too while a battle is on
    }

    private final String[] addTips = new String[ROWS];

    /** The House chips in the sheet's head: the chosen one wide (seal, tick and name), the others a seal each, centred on the paper. */
    void layoutBannerChips() {
        int[] sh = col(C_SHEET);
        var houses = RecruitView.houses();
        String on = shownHouse();
        int total = -CHIP_GAP;
        for (var h : houses) total += chipSpan(h.id().equals(on), h.name()) + CHIP_GAP;
        int x = sh[0] + SHEET_X + BANNER_CHIPS_CX - total / 2, y = sh[1] + SHEET_Y + BANNER_CHIPS_DY;
        for (var h : houses) {
            var b = buttons.get("house-" + h.id());
            int w = chipSpan(h.id().equals(on), h.name());
            if (b != null) b.moveTo(x, y, w, CHIP_H);
            x += w + CHIP_GAP;
        }
    }

    /** A Banner chip's width: the seal alone, or seal and name for the House shown. */
    static int chipSpan(boolean on, String name) { return on ? chipWidth(name) : 2 * CHIP_PAD + CHIP_SEAL; }

    /** A click on row i arms its troop: a seat click then places it. */
    void arm(int i) {
        var roll = owned();
        if (i < roll.size()) armed = roll.get(i).troopId();
        refresh();
    }

    /** Add or Remove on row i: army:(troop):add takes the first empty seat, :remove empties the last seat the troop holds. */
    void army(int i, String how) {
        var roll = owned();
        if (i >= roll.size()) return;
        String troop = roll.get(i).troopId(); var line = line(); // integration: p2 takes slot:<i>:<id>, not army:
        int at = how.equals("add") ? firstRun(weight(troop)) : line.lastIndexOf(troop);
        String id = how.equals("add") ? troop : EMPTY;
        if (at >= 0 && send("slot:" + at + ":" + id)) planSlot(at, id);
    }

    /** A seat click: the troop goes in the seat; emptying a later half of a wide troop empties the troop's own seat (the server repacks). */
    void place(int seat, String troop) {
        if (troop == null) return;
        int at = EMPTY.equals(troop) ? unitSeat(seat) : seat;
        if (send("slot:" + at + ":" + troop)) planSlot(at, troop);
    }

    // ============================================================================================================ drag and drop

    static BannerDrop.Rules dropRules() { return new BannerDrop.Rules(RosterMath.Catalog.fromSpec(), CampaignClient.profile.owned.keySet(), RosterMath.fieldableHouses()); }

    /** A left press (GUI px) on the Banner page. A press on a seat is taken here: its click (place the armed troop) goes on the release,
     *  so that a drag from an occupied seat does not first place the armed troop. A press on a roll row goes on to arm the row, and may
     *  become a drag of that troop. Returns whether the press is taken. */
    boolean press(double x, double y) {
        if (!page.equals("banner") || dragging || !battleFree()) return false;
        pressed = false; moved = false; pressSeat = -1;
        for (int i = 0; i < SEATS; i++) {
            var b = buttons.get("seat-" + i);
            if (b == null || !b.visible || !b.active || !b.overArt(x, y)) continue;
            pressed = true; pressSeat = i; pressX = x; pressY = y; curX = x; curY = y;
            int[] box = b.canvasBox();
            grabX = UiCanvas.toCanvas(x) - box[0]; grabY = UiCanvas.toCanvas(y) - box[1];
            return true;
        }
        var roll = owned();
        for (int i = 0; i < ROWS && i < roll.size(); i++) {
            var b = buttons.get("row-" + i);
            if (b == null || !b.visible || !b.active || !b.overArt(x, y)) continue;
            pressed = true; pressX = x; pressY = y; curX = x; curY = y; dragFromRoll = true; dragTroop = roll.get(i).troopId(); dragSeat = -1;
            int[] box = b.canvasBox();
            grabX = UiCanvas.toCanvas(x) - (box[0] + 4); grabY = UiCanvas.toCanvas(y) - (box[1] + 4);
            return false;
        }
        return false;
    }

    /** The pointer moved with the left button down (GUI px). Past DRAG_START canvas px the press becomes a drag: the troop's card is
     *  lifted and every seat is judged for it. Returns whether the move is taken. */
    boolean dragged(double x, double y) {
        if (!pressed && !dragging) return false;
        curX = x; curY = y;
        if (dragging) return true;
        if (Math.hypot(UiCanvas.toCanvas(x - pressX), UiCanvas.toCanvas(y - pressY)) < DRAG_START) return pressSeat >= 0;
        moved = true;
        if (pressSeat >= 0) { // a seated troop is lifted by any of its seats; an empty seat has nothing to lift
            var line = CampaignClient.profile.banner;
            if (pressSeat >= line.size() || line.get(pressSeat).equals(EMPTY) || line.get(pressSeat).equals(RosterMath.MARKER) && line.get(BannerDrop.head(line, pressSeat)).equals(EMPTY)) { pressed = false; pressSeat = -1; return true; }
            dragFromRoll = false; dragSeat = BannerDrop.head(line, pressSeat); dragTroop = line.get(dragSeat);
        }
        flyAt = 0; dropWhy = "";
        if (!classify()) { cancelDrag(); return true; }
        dragging = true; pressed = false; UiSounds.play(AudioCues.Cue.BANNER_DRAG);
        return true;
    }

    /** The button came up (GUI px). A drag drops its card: on a seat or, from a seat, on the roll. A press on a seat that never became
     *  a drag clicks it now. Returns whether the release is taken. */
    boolean released(double x, double y, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !pressed && !dragging) return false;
        curX = x; curY = y;
        if (dragging) { dragging = false; pressed = false; pressSeat = -1; drop(x, y); return true; }
        int seat = pressSeat;
        pressed = false; pressSeat = -1;
        if (seat < 0) return false;
        var b = buttons.get("seat-" + seat);
        if (!moved && b != null && b.visible && b.active && b.overArt(x, y)) {
            b.playDownSound(MinecraftClient.getInstance().getSoundManager());
            b.onPress();
        }
        return true;
    }

    /** Ends a press or a drag with nothing done: the card flies back to where it was lifted. */
    void cancelDrag() {
        if (dragging) fly(false, null);
        dragging = false; pressed = false; pressSeat = -1; moved = false;
    }

    /** Judges every seat for the dragged troop against the banner as it is now; false when the banner has nothing to judge. */
    boolean classify() {
        dragBase = new ArrayList<>(CampaignClient.profile.banner);
        if (dragBase.size() < SEATS || dragTroop.isEmpty()) return false;
        var r = dropRules();
        for (int s = 0; s < SEATS; s++) seatPlan[s] = dragFromRoll ? BannerDrop.fromRoll(dragBase, dragTroop, s, r) : BannerDrop.move(dragBase, dragSeat, s, r);
        unseatPlan = dragFromRoll ? null : BannerDrop.unseat(dragBase, dragSeat, r);
        return true;
    }

    /** The seat under a GUI point, 4 px of canvas slack round each (the seats are 8 apart), or -1. */
    int seatAt(double x, double y) {
        double cx = UiCanvas.toCanvas(x), cy = UiCanvas.toCanvas(y);
        for (int i = 0; i < SEATS; i++) {
            var b = buttons.get("seat-" + i);
            if (b == null || !b.visible) continue;
            int[] bx = b.canvasBox();
            if (cx >= bx[0] - 4 && cy >= bx[1] - 4 && cx < bx[0] + bx[2] + 4 && cy < bx[1] + bx[3] + 4) return i;
        }
        return -1;
    }

    /** Whether a GUI point is on the roll's paper (the place a seated troop is dropped to take it off). */
    boolean rollAt(double x, double y) {
        var r = rectsDrawn.get("roll");
        if (r == null) return false;
        double cx = UiCanvas.toCanvas(x), cy = UiCanvas.toCanvas(y);
        return cx >= r[0] && cy >= r[1] && cx < r[0] + r[2] && cy < r[1] + r[3];
    }

    /** What dropping the card at a GUI point would do against the banner now: the plan, or null when the point is no target. */
    BannerDrop.Plan planAt(double x, double y) {
        var line = new ArrayList<>(CampaignClient.profile.banner);
        if (line.size() < SEATS) return null;
        var r = dropRules();
        int s = seatAt(x, y);
        if (s >= 0) return dragFromRoll ? BannerDrop.fromRoll(line, dragTroop, s, r) : BannerDrop.move(line, dragSeat, s, r);
        if (!dragFromRoll && rollAt(x, y)) return BannerDrop.unseat(line, dragSeat, r);
        return null;
    }

    /** The card is let go at a GUI point: a legal drop sends its plan's commands, anything else snaps the card back. */
    void drop(double x, double y) {
        var plan = battleFree() ? planAt(x, y) : null;
        if (plan == null || !plan.ok()) {
            dropWhy = plan == null ? "" : plan.why();
            if (!dropWhy.isEmpty()) { noticeText = dropWhy; noticeAt = ticks; }
            fly(false, null);
            return;
        }
        planned = null; pend = plan; pendBase = new ArrayList<>(CampaignClient.profile.banner); pendStep = 0; pendAt = ticks;
        optimistic = plan.commands().size() > 1 ? plan.result() : null;
        int s = seatAt(x, y);
        fly(true, s >= 0 ? buttons.get("seat-" + s) : null);
        pump();
    }

    /** Sends the next command of the drop in flight once the banner shows the step before it; the drop ends when the banner shows
     *  its result, or when the server has not answered in PEND_TICKS. */
    void pump() {
        if (pend == null) return;
        var line = CampaignClient.profile.banner;
        if (pendStep >= pend.commands().size() && line.equals(pend.result()) || ticks - pendAt > PEND_TICKS) { pend = null; optimistic = null; return; }
        if (pendStep >= pend.commands().size()) return;
        var expect = pendStep == 0 ? pendBase : pend.states().get(pendStep - 1);
        if (line.equals(expect) && send(pend.commands().get(pendStep))) pendStep++;
    }

    void dragTick() {
        pump();
        if (!dragging) return;
        var line = CampaignClient.profile.banner;
        if (!battleFree() || !dragFromRoll && (dragSeat >= line.size() || !line.get(dragSeat).equals(dragTroop))) { cancelDrag(); return; }
        if (!line.equals(dragBase) && !classify()) cancelDrag();
    }

    /** Whether a drag or a drop's flight is on screen. */
    boolean dragShown() { return dragging || flyAt != 0 && System.nanoTime() - flyAt < FLY_MS * 1_000_000L; }

    /** The card's flight: to the seat it lands on (land), or back to where it was lifted. */
    void fly(boolean land, UiButton seat) {
        flyFromX = UiCanvas.toCanvas(curX) - grabX; flyFromY = UiCanvas.toCanvas(curY) - grabY;
        int[] to = null;
        if (land && seat != null) to = seat.canvasBox();
        else if (!land) {
            var from = dragFromRoll ? null : buttons.get("seat-" + dragSeat);
            if (from != null) to = from.canvasBox();
            else if (dragFromRoll) {
                var roll = owned();
                for (int i = 0; i < roll.size(); i++) if (roll.get(i).troopId().equals(dragTroop)) {
                    var row = buttons.get("row-" + i);
                    if (row != null) { int[] bx = row.canvasBox(); to = new int[]{bx[0] + 4, bx[1] + 4}; }
                }
            }
        }
        if (to == null) { flyAt = 0; return; }
        flyToX = to[0]; flyToY = to[1]; flyBack = !land; flyAt = System.nanoTime();
    }

    /** The troop's House colour (its accent), or brass. */
    static int accentOf(String troop) {
        for (var t : Spec.TROOPS) if (t.id().equals(troop)) { var h = ArmyCards.house(t); if (h != null) return DarkUi.opaque(h.accent()); }
        return DarkUi.BRASS_HI;
    }

    /** A ring of `thick` px in `color` round the box, a dark line outside it so a House as bright as the parchment still reads. */
    static void ring(DrawContext g, int x, int y, int w, int h, int color, int thick) {
        for (int t = 0; t < thick; t++) g.drawBorder(x - 1 - t, y - 1 - t, w + 2 + 2 * t, h + 2 + 2 * t, color);
        g.drawBorder(x - 1 - thick, y - 1 - thick, w + 2 + 2 * thick, h + 2 + 2 * thick, DarkUi.OAK0);
    }

    /** What the drag shows over the page and its widgets (CampScreen draws it after them): every seat judged for the card (a legal one
     *  ringed in the troop's House colour, an illegal one dimmed), the seats a drop would fill under the pointer filled in that colour, the
     *  card itself raised under the pointer with a word on what the drop would do, and the card's flight at the end. */
    void overlay(DrawContext g) {
        if (!page.equals("banner") || !dragShown()) return;
        UiCanvas.begin(g);
        try {
            int acc = accentOf(dragTroop);
            double gx, gy;
            String word = "";
            boolean legal = false;
            if (dragging) {
                gx = UiCanvas.toCanvas(curX) - grabX; gy = UiCanvas.toCanvas(curY) - grabY;
                var here = planAt(curX, curY);
                int hs = seatAt(curX, curY);
                legal = here != null && here.ok();
                var lifted = dragFromRoll ? List.<Integer>of() : BannerDrop.seatsOf(dragBase, dragSeat);
                for (int i = 0; i < SEATS; i++) {
                    var b = buttons.get("seat-" + i);
                    if (b == null || !b.visible) continue;
                    int[] bx = b.canvasBox();
                    var p = seatPlan[i];
                    if (lifted.contains(i)) { g.fill(bx[0], bx[1], bx[0] + SEAT, bx[1] + SEAT, DarkUi.DROP_VEIL); continue; }
                    if (p != null && p.ok()) ring(g, bx[0], bx[1], SEAT, SEAT, (0xb0 << 24) | (acc & 0xffffff), 1);
                    else g.fill(bx[0], bx[1], bx[0] + SEAT, bx[1] + SEAT, DarkUi.DROP_DIM);
                }
                if (legal && hs >= 0) for (int j : here.changed()) { // the seats the drop fills, under the pointer: filled and ringed brighter
                    var b = buttons.get("seat-" + j);
                    if (b == null) continue;
                    int[] bx = b.canvasBox();
                    g.fill(bx[0], bx[1], bx[0] + SEAT, bx[1] + SEAT, (0x55 << 24) | (acc & 0xffffff));
                    ring(g, bx[0], bx[1], SEAT, SEAT, acc, 2);
                }
                var roll = rectsDrawn.get("roll");
                if (!dragFromRoll && roll != null && unseatPlan != null && unseatPlan.ok()) { // a seated troop taken off by dropping it back on the roll
                    boolean over = rollAt(curX, curY);
                    if (over) g.fill(roll[0], roll[1], roll[0] + roll[2], roll[1] + roll[3], (0x30 << 24) | (acc & 0xffffff));
                    ring(g, roll[0], roll[1], roll[2], roll[3], over ? acc : (0xb0 << 24) | (acc & 0xffffff), over ? 2 : 1);
                }
                if (here != null) word = here.ok() ? dropWord(hs) : here.why();
            } else {
                double t = Math.min(1, (System.nanoTime() - flyAt) / (FLY_MS * 1e6));
                t = 1 - (1 - t) * (1 - t) * (1 - t);
                gx = flyFromX + (flyToX - flyFromX) * t; gy = flyFromY + (flyToY - flyFromY) * t;
            }
            int x = (int) Math.round(gx), y = (int) Math.round(gy);
            g.fill(x + 4, y + 6, x + SEAT + 4, y + SEAT + 6, DarkUi.DROP_LIFT); // the card lifted off the sheet
            seat(g, "ghost", dragTroop, dragTroop, x, y, null);
            ring(g, x, y, SEAT, SEAT, acc, 1);
            if (dragging && !word.isEmpty()) ghostWord(g, x, y, word, legal);
        } finally {
            UiCanvas.end(g);
        }
    }

    /** A word on what a legal drop does. */
    String dropWord(int seat) {
        if (seat < 0) return "Take off";
        boolean occupied = !dragBase.get(seat).equals(EMPTY);
        if (dragFromRoll) return occupied ? "Replace" : "Seat";
        return occupied ? "Trade places" : "Move to the end";
    }

    /** A strip with a word beside the card (left of it when the right would leave the canvas). */
    void ghostWord(DrawContext g, int x, int y, String word, boolean legal) {
        var font = font();
        var t = DarkUi.body(word);
        int w = font.getWidth(t) + 10, tx = x + SEAT + 6;
        if (tx + w > UiCanvas.width() - 12) tx = x - 6 - w;
        int ty = y + (SEAT - 13) / 2;
        g.fill(tx, ty, tx + w, ty + 13, DarkUi.PREVIEW_BAND);
        g.drawText(font, t, tx + 5, ty + 2, legal ? DarkUi.IVORY : DarkUi.ALARM_TEXT, false);
        rectsDrawn.put("ghost-word", new int[]{tx, ty, w, 13});
        textsDrawn.put("ghost-word", word);
    }

    void banner(DrawContext g) {
        int[] ro = col(C_ROLL), sh = col(C_SHEET), cl = col(C_CLASS);
        int ox = ro[0], oy = ro[1];
        var roll = owned();
        int x = ox + ROLL_X, sel = -1;
        rectsDrawn.put("roll", UiTex.draw(g, "recruit/muster-roll", x, oy + ROLL_Y));
        for (int i = 0; i < roll.size(); i++) if (roll.get(i).troopId().equals(armed)) sel = i;
        if (sel >= 0) rectsDrawn.put("mark", UiTex.draw(g, "recruit/row-mark", x, oy + ROW_TOP + ROW_H * sel));
        boolean more = RecruitView.roll(shownHouse()).stream().anyMatch(r -> !r.owned());
        int places = more ? ROWS : roll.size(); // the House's troops not yet recruited keep their places on the roll, ruled and empty
        for (int i = roll.isEmpty() ? 2 : 1; i < places; i++) if (i != sel) UiTex.draw(g, "recruit/rule-208", x + 16, oy + ROW_TOP + ROW_H * i);
        for (int i = 0; i < roll.size(); i++) rollRow(g, roll.get(i), i, x, oy + ROW_TOP + ROW_H * i);
        if (more && roll.size() < ROWS) { // where the rest of the House's troops come from: in the roll's last place, at its foot
            var hint = UiMath.lines(GravesAndGloryClient.running ? ROLL_LOCKED : ROLL_HINT, ROLL_HINT_W, 2, s -> font().getWidth(DarkUi.body(s)));
            int top = oy + ROW_TOP + ROW_H * (ROWS - 1) + (ROW_H + 4 - 13 * hint.size()) / 2;
            for (int i = 0; i < hint.size(); i++) WarTableHome.centred(g, DarkUi.body(hint.get(i)), x + ROLL_HINT_CX, top + 13 * i, DarkUi.SEPIA);
            textsDrawn.put("roll-hint", String.join(" ", hint));
            rectsDrawn.put("roll-hint", new int[]{x + ROLL_HINT_CX - ROLL_HINT_W / 2, top, ROLL_HINT_W, 13 * hint.size() - 4});
        }
        if (roll.isEmpty()) { // a House none of whose troops is recruited yet: the first place says so
            var say = UiMath.lines("You have recruited no " + RosterMath.houseName(shownHouse()) + " troop yet.", ROLL_HINT_W, 2, s -> font().getWidth(DarkUi.body(s)));
            int top = oy + ROW_TOP + ROW_H + 16 - 13 * say.size() / 2;
            for (var row : Spec.HOUSES) if (row.id().equals(shownHouse()))
                g.drawTexture(new Identifier(row.glyph()), x + ROLL_HINT_CX - 8, top - 26, 0f, 0f, 16, 16, 16, 16);
            for (int i = 0; i < say.size(); i++) WarTableHome.centred(g, DarkUi.body(say.get(i)), x + ROLL_HINT_CX, top + 13 * i, DarkUi.INK_TEXT);
            textsDrawn.put("roll-empty", String.join(" ", say));
            rectsDrawn.put("roll-empty", new int[]{x + ROLL_HINT_CX - ROLL_HINT_W / 2, top, ROLL_HINT_W, 13 * say.size() - 4});
        }
        sheet(g, sh[0] + SHEET_X, sh[1] + SHEET_Y, sh[0], sh[1]);
        armedBand(g, sh[0] + ARMED_X, sh[1] + ARMED_Y);
        roles(g, cl[0] + CLASS_X, cl[1]);
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
        accent(g, r.troopId(), x + 8, y + 8);
        UiTex.draw(g, "table/frame-rim-48", x + 4, y + 4);
        String name = UiMath.trim(r.name(), NAME_W, s -> font.getWidth(DarkUi.heading(s)));
        g.drawText(font, DarkUi.heading(name), x + 60, y + 12, DarkUi.INK_TEXT, false);
        int n = seats(r.troopId()), w = weight(r.troopId());
        String count = n == 0 ? "No seats" : n == 1 ? "1 seat" : n + " seats";
        g.drawText(font, DarkUi.body(count), x + 60, y + 30, DarkUi.SEPIA, false);
        if (w > 1) { // a wide troop says so in two words (the Add tag's tip says how many seats a copy takes)
            String wide = "Wide troop";
            g.drawText(font, DarkUi.body(wide), x + 60, y + 42, DarkUi.INK_TEXT, false);
            textsDrawn.put("weight-" + i, wide);
        }
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
        int hy = y + BANNER_HEAD_DY;
        DarkUi.teamMark(g, t, x + BANNER_PAPER_X, hy);
        g.drawText(font, DarkUi.heading(Spec.FACTIONS[t].name()), x + BANNER_PAPER_X + 14, hy, DarkUi.INK_TEXT, false);
        String seated = used() + "/" + SEATS + " seats";
        int sw = font.getWidth(DarkUi.body(seated));
        g.drawText(font, DarkUi.body(seated), x + BANNER_PAPER_X + BANNER_PAPER_W - sw, hy, DarkUi.SEPIA, false);
        textsDrawn.put("seated", seated);
        g.drawText(font, DarkUi.body("Colour"), x + COLOUR_LABEL_X, hy, DarkUi.SEPIA, false);
        textsDrawn.put("colour-label", "Colour");
        rectsDrawn.put("colour-label", new int[]{x + COLOUR_LABEL_X, hy, font.getWidth(DarkUi.body("Colour")), 9});
        rectsDrawn.put("seated", new int[]{x + BANNER_PAPER_X + BANNER_PAPER_W - sw, hy, sw, 9});
        g.drawText(font, DarkUi.body("Front"), x + 16, oy + SEAT_Y + 19, DarkUi.SEPIA, false);
        g.drawText(font, DarkUi.body("Rear"), x + 16, oy + SEAT_Y + 3 * SEAT_STEP + 19, DarkUi.SEPIA, false);
        var line = shownLine();
        for (int i = 0; i < SEATS; i++) {
            String id = i < line.size() ? line.get(i) : EMPTY, label = null;
            if (id.equals(RosterMath.MARKER)) { int u = BannerDrop.head(line, i); label = (i - u + 1) + " of " + weight(line.get(u)); }
            else if (!id.equals(EMPTY) && weight(id) > 1) label = "1 of " + weight(id);
            seat(g, "seat-" + i, id, drawnTroop(i), ox + SEAT_X + SEAT_STEP * (i % 5), oy + SEAT_Y + SEAT_STEP * (i / 5), label); // a wide troop's later half draws the troop and its place in it
        }
    }

    /** A seat, as home seats the party: the frame's back (the empty one for an empty seat), the troop's live model and the rim. */
    void seat(DrawContext g, String where, String troop, int x, int y) { seat(g, where, troop, troop, x, y, null); }

    /** A seat that holds `troop` in the profile's banner and draws `drawn`, with "1 of 2" on the halves of a wide troop. */
    void seat(DrawContext g, String where, String troop, String drawn, int x, int y, String label) {
        boolean empty = drawn.equals(EMPTY);
        if (empty) UiTex.draw(g, "table/frame-back-empty-48", x + 4, y + 4);
        else UiTex.draw(g, "table/frame-back-48", x + 4, y + 4);
        var e = empty ? null : WarTableRecruit.model(drawn);
        if (e != null) {
            Portrait.draw(g, e, x + 4, y + 4, 40, 0);
            portraitsDrawn.add(new PortraitDraw(where, drawn, Portrait.last()));
        }
        if (!empty) accent(g, drawn, x + 4, y + 4);
        UiTex.draw(g, "table/frame-rim-48", x, y);
        if (label != null) { // the wide troop's place in it, in ivory on a dark strip so it reads over any portrait
            var font = font();
            int lw = font.getWidth(DarkUi.body(label));
            g.fill(x + SEAT - lw - 10, y + SEAT - 16, x + SEAT - 2, y + SEAT - 4, DarkUi.PREVIEW_BAND);
            g.drawText(font, DarkUi.body(label), x + SEAT - lw - 6, y + SEAT - 14, DarkUi.IVORY, false);
            textsDrawn.put("wide-" + where, label);
            rectsDrawn.put("wide-" + where, new int[]{x + SEAT - lw - 10, y + SEAT - 16, lw + 8, 12});
        }
        rectsDrawn.put(where, new int[]{x, y, SEAT, SEAT});
        textsDrawn.put(where, troop);
    }

    /** The troop's House colour as a two-pixel ring just inside its 40-pixel portrait, over a dark line at the portrait's edge: the line
     *  keeps a House whose colour is the rim's own brass (Gilded) from vanishing into the rim. */
    void accent(DrawContext g, String troop, int x, int y) {
        for (var t : Spec.TROOPS) if (t.id().equals(troop)) {
            var h = ArmyCards.house(t);
            if (h != null) {
                g.drawBorder(x, y, 40, 40, DarkUi.OAK0);
                g.drawBorder(x + 1, y + 1, 38, 38, DarkUi.opaque(h.accent()));
                g.drawBorder(x + 2, y + 2, 36, 36, DarkUi.opaque(h.accent()));
            }
            return;
        }
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

    /** The roles' heading and plaques (not buttons: gear sets the role, in the Arsenal): the four gear sets, the one the commander wears most of
     *  lit, each with its worn pieces; under them what sets the role. */
    void roles(DrawContext g, int x, int oy) {
        var font = font();
        g.drawText(font, DarkUi.heading("Your role"), x, oy + 124, DarkUi.BRASS, false);
        var mine = GearStats.of(CampaignClient.profile);
        var shown = shownRoles(mine.role());
        int step = shown.size() <= 4 ? CLASS_STEP : CLASS_STEP * 4 / shown.size(); // a fifth plaque packs the column into the four's height
        for (int i = 0; i < shown.size(); i++) paintRole(g, shown.get(i), mine, x, oy + CLASS_Y + step * i);
        int top = oy + CLASS_Y + CLASS_STEP * 4 + 8;
        var bonus = lines(g, "Wear two or four pieces of one set to unlock its bonuses. Gear and relics are changed in the Arsenal.", x, top, BONUS_W, 4, DarkUi.MUTED);
        textsDrawn.put("bonus", String.join(" ", bonus));
        int limit = rectsDrawn.containsKey("sheet") ? rectsDrawn.get("sheet")[1] + rectsDrawn.get("sheet")[3] - 10 : top + 200;
        if (WarTable.compact()) limit = Math.min(limit, WarTable.compactOrigin()[1] + C_BANNER_NOTICE_Y - 4); // the notice stands under the bonus on the compact page
        traits(g, x, top + 13 * bonus.size() + 8, limit);
    }

    /** One House of the banner for the trait rows: the House, the slots the banner fields of it, its thresholds and the tier they reach. */
    record TraitRow(Spec.HousesRow house, int slots, int[] thresholds, int tier) {}
    static final int TRAIT_HEAD = 14, TRAIT_NAME = 14, TRAIT_PIPS = 12, TRAIT_LINE = 11, TRAIT_GAP = 6;

    /** The Houses the banner shown fields at least one slot of, the most slots first (House order among equals); a House with none is left out. */
    static List<TraitRow> traitRows() {
        var slots = RosterMath.houseSlots(shownLine(), RosterMath.Catalog.fromSpec());
        List<TraitRow> out = new ArrayList<>();
        for (var h : Spec.HOUSES) {
            int n = slots.getOrDefault(h.id(), 0);
            int[] thr = Houses.thresholds(h.id());
            if (n > 0 && thr.length > 0) out.add(new TraitRow(h, n, thr, RosterMath.traitTier(n, thr)));
        }
        out.sort((a, b) -> Integer.compare(b.slots(), a.slots()));
        return out;
    }

    /** The text of a House's trait at a tier, "" for none. */
    static String traitText(String house, int tier) {
        var t = CounterMath.trait(Spec.HOUSETRAITS, house, tier);
        return t == null ? "" : t.text();
    }

    private List<String> traitLines(String s, int w) {
        var font = font();
        return s.isEmpty() ? List.of() : UiMath.lines(s, w, 8, t -> font.getWidth(DarkUi.small(t)));
    }

    /** What a row's detail says at a level: 2 the tier reached and the next tier greyed, 1 the tier reached only, 0 nothing. */
    private List<String> traitCurrent(TraitRow r, int w) {
        return traitLines(r.tier() > 0 ? traitText(r.house().id(), r.tier()) : "No trait yet.", w);
    }

    private int traitHeight(TraitRow r, int level, int w) {
        int h = TRAIT_NAME + TRAIT_PIPS;
        if (level >= 1) h += 2 + TRAIT_LINE * traitCurrent(r, w).size();
        if (level >= 2 && r.tier() < 3) h += 2 + TRAIT_LINE * traitLines(traitText(r.house().id(), r.tier() + 1), w).size();
        return h;
    }

    /** The House trait rows under the roles' note, from top down to bottom: per House its name and tier, the slots fielded against the next
     *  threshold, three pips for the thresholds, the trait text of the tier reached and, greyed, the next tier's. The most detail that fits
     *  every row is drawn; at the least a row is its name, progress and pips, and a row that does not fit is left out. */
    void traits(DrawContext g, int x, int top, int bottom) {
        var rows = traitRows();
        if (rows.isEmpty()) return;
        var font = font();
        int w = BONUS_W - 4, y0 = top + TRAIT_HEAD, level = 2, count = rows.size();
        for (; level >= 0; level--) {
            int sum = 0;
            for (var r : rows) sum += traitHeight(r, level, w) + TRAIT_GAP;
            if (y0 + sum - TRAIT_GAP <= bottom) break;
        }
        if (level < 0) { // not even the bare rows fit: keep those that do
            level = 0; int sum = 0; count = 0;
            for (var r : rows) { sum += traitHeight(r, 0, w) + TRAIT_GAP; if (y0 + sum - TRAIT_GAP > bottom) break; count++; }
            if (count == 0) return;
        }
        g.drawText(font, DarkUi.heading("House traits"), x, top, DarkUi.BRASS, false);
        int y = y0;
        for (int i = 0; i < count; i++) {
            var r = rows.get(i);
            String id = r.house().id();
            int accent = UiMath.accentText(r.house().accent()), top0 = y;
            g.drawTexture(new Identifier(r.house().glyph()), x, y, 12, 12, 0f, 0f, 16, 16, 16, 16);
            String name = UiMath.trim(RosterMath.houseName(id) + (r.tier() > 0 ? " " + MatchStats.roman(r.tier()) : ""), w - 16, t -> font.getWidth(DarkUi.heading(t)));
            g.drawText(font, DarkUi.heading(name), x + 16, y + 1, r.tier() > 0 ? accent : DarkUi.MUTED, false);
            String progress = r.tier() >= 3 ? r.slots() + " slots" : r.slots() + " / " + r.thresholds()[r.tier()];
            y += TRAIT_NAME;
            int px = x + 16;
            for (int t = 0; t < r.thresholds().length; t++) {
                boolean met = r.slots() >= r.thresholds()[t];
                if (met) g.fill(px, y + 1, px + 8, y + 9, DarkUi.opaque(r.house().accent()));
                g.drawBorder(px, y + 1, 8, 8, met ? DarkUi.BRASS_HI : DarkUi.MUTED);
                String n = Integer.toString(r.thresholds()[t]);
                g.drawText(font, DarkUi.body(n), px + 11, y + 1, met ? DarkUi.IVORY : DarkUi.MUTED, false);
                px += 11 + font.getWidth(DarkUi.body(n)) + 8;
            }
            // the progress shares the pips' row, right-aligned and clear of them (the short form, the slots alone, when the long one would touch the last number)
            int pw = font.getWidth(DarkUi.body(progress));
            String drawn = progress;
            if (px - 8 + 6 + pw > x + w) { drawn = Integer.toString(r.slots()); pw = font.getWidth(DarkUi.body(drawn)); }
            g.drawText(font, DarkUi.body(drawn), x + w - pw, y + 1, DarkUi.IVORY, false);
            y += TRAIT_PIPS;
            if (level >= 1) {
                y += 2;
                var cur = traitCurrent(r, w);
                for (var line : cur) { g.drawText(font, DarkUi.small(line), x, y, DarkUi.IVORY, false); y += TRAIT_LINE; }
                textsDrawn.put("trait-text-" + id, String.join(" ", cur));
            }
            if (level >= 2 && r.tier() < 3) {
                y += 2;
                for (var line : traitLines(traitText(id, r.tier() + 1), w)) { g.drawText(font, DarkUi.small(line), x, y, DarkUi.MUTED, false); y += TRAIT_LINE; }
            }
            rectsDrawn.put("trait-" + id, new int[]{x, top0, w, y - top0});
            textsDrawn.put("trait-" + id, progress);
            y += TRAIT_GAP;
        }
        textsDrawn.put("trait-level", Integer.toString(level));
    }

    /** The role plaques: the four starter roles, and the commander's own role under them when it is a House set (the column holds five, not nine). */
    static List<Spec.GearSetsRow> shownRoles(String role) {
        List<Spec.GearSetsRow> out = new ArrayList<>();
        for (var s : Spec.GEARSETS) if (!WarTableLobby.houseSet(s) || s.id().equals(role)) out.add(s);
        return out;
    }

    /** A role plaque: plaque-120 with the set's role name and the pieces worn, the commander's own role lit (plaque-120-here, RAIL_KEY). */
    void paintRole(DrawContext g, Spec.GearSetsRow set, GearStats.Loadout mine, int x, int y) {
        boolean on = set.id().equals(mine.role());
        String key = "role-" + set.id(), plate = on ? "table/plaque-120-here" : "table/plaque-120", icon = WarTableLobby.roleIcon(set.id());
        UiTex.draw(g, plate, x, y);
        if (icon != null && UiTex.has(icon)) UiTex.draw(g, icon, x + 8, y + 7);
        int ink = on ? DarkUi.RAIL_KEY : DarkUi.IVORY;
        String worn = mine.setCounts().getOrDefault(set.id(), 0) + "/4";
        int ww = font().getWidth(DarkUi.body(worn));
        String name = UiMath.trim(WarTableLobby.roleName(set.id()), CLASS_W - 34 - ww - 4, s -> font().getWidth(DarkUi.heading(s))); // Barrow Warden beside its count
        text(g, key, DarkUi.heading(name), name, x + 28, y + 8, ink);
        g.drawText(font(), DarkUi.body(worn), x + CLASS_W - 6 - ww, y + 9, on ? DarkUi.RAIL_KEY : DarkUi.MUTED, false);
        rectsDrawn.put(key, new int[]{x, y, CLASS_W, 28});
        texturesDrawn.put(key, plate);
    }

    // =================================================================================================================== Contracts

    static Progression.State progression() { return CampaignClient.profile.progression; }

    // ===================================================================================================================== Claims with a House

    /** The House and the Rare picked on the Contracts and Chapter rows; null while none is (the banner's House, and that House's first Rare). */
    private static String claimPick, rarePick, bannerPick;
    private static List<String> bannerSeen;
    private static String bannerHouse = "";

    /** The House the banner leans on (RosterMath.rollHouse), worked out again only when the banner changes. */
    static String bannerHouse() {
        var banner = CampaignClient.profile.banner;
        if (!banner.equals(bannerSeen)) { bannerSeen = new ArrayList<>(banner); bannerHouse = RosterMath.rollHouse(banner); }
        return bannerHouse;
    }

    /** The House the shards of the next claim go to: the chip picked, else the banner's House. */
    static String claimHouse() { return claimPick != null ? claimPick : bannerHouse(); }

    static void pickHouse(String house) { claimPick = house; rarePick = null; }

    /** The House whose roll the Banner page lists: the chip picked, else the one the banner leans on. */
    static String shownHouse() { return bannerPick != null ? bannerPick : bannerHouse(); }

    /** The Rare troop tier 20 would give: the one picked among the Rares of the House picked, else the House's first ("" when it has none). */
    static String rarePick() {
        var rares = RecruitPages.rares(claimHouse());
        return rares.isEmpty() ? "" : rarePick != null && rares.contains(rarePick) ? rarePick : rares.get(0);
    }

    /** The next Rare of the House picked, round to the first again. */
    static void cycleRare() {
        var rares = RecruitPages.rares(claimHouse());
        if (!rares.isEmpty()) rarePick = rares.get((rares.indexOf(rarePick()) + 1) % rares.size());
    }

    /** Puts the picks back to none (the checks start from the defaults). */
    static void forgetPicks() { claimPick = null; rarePick = null; bannerPick = null; }

    static Progression.State copyOf(Progression.State p) {
        var s = new Progression.State();
        s.schema = p.schema; s.xp = p.xp; s.totalBattles = p.totalBattles; s.totalWins = p.totalWins; s.weeklyBattles = p.weeklyBattles;
        s.day = p.day; s.week = p.week; s.title = p.title; s.rerolled = p.rerolled;
        s.dailyBattles = p.dailyBattles; s.dailyCaptures = p.dailyCaptures; s.dailyOrders = p.dailyOrders;
        s.claims = new HashSet<>(p.claims); s.titles = new LinkedHashSet<>(p.titles); s.matchGrants = new LinkedHashSet<>(p.matchGrants);
        return s;
    }

    /** What claiming key (daily:<slot>, weekly or founders:<tier>) would pay with arg (a House, or at tier 20 a Rare troop): Progression.claim run
     *  on a copy of the state with the contract finished and the tier reached, so the reward words come from the game's own rule. */
    static Progression.Grant payOf(String key, String arg) {
        var now = progression();
        if (payRevision != CampaignClient.revision || payFor != now) { payCache.clear(); payRevision = CampaignClient.revision; payFor = now; }
        String at = arg == null ? key : key + "|" + arg;
        var hit = payCache.get(at);
        if (hit == null) { hit = payOfNow(key, arg); payCache.put(at, hit); }
        return hit;
    }

    /** payOf's answers for the snapshot they were worked out from (CampaignClient.revision and the progression it carried): the Contracts page asks four times a frame and the state only moves with a snapshot. */
    private static final Map<String, Progression.Grant> payCache = new HashMap<>();
    private static int payRevision = -1;
    private static Progression.State payFor;

    private static Progression.Grant payOfNow(String key, String arg) {
        var s = copyOf(progression());
        s.dailyBattles = Math.max(s.dailyBattles, 3); s.dailyCaptures = Math.max(s.dailyCaptures, 1); s.dailyOrders = Math.max(s.dailyOrders, 10);
        s.weeklyBattles = Math.max(s.weeklyBattles, Progression.WEEKLY_BATTLES);
        s.xp = Math.max(s.xp, Progression.XP_PER_TIER * TIERS);
        s.claims.remove(key);
        return Progression.claim(s, arg == null ? key : key + ":" + arg);
    }

    /** A grant as words: "30 gold and 75 XP", "150 gold, 300 XP and 10 Ironclad shards" (shards only when asked for; houseName null leaves the House out). */
    static String words(Progression.Grant g, boolean shards, String houseName) {
        List<String> parts = new ArrayList<>();
        if (g.gold() > 0) parts.add(g.gold() + " gold");
        if (g.xp() > 0) parts.add(g.xp() + " XP");
        if (shards && g.shards() > 0) parts.add(g.shards() + " " + (houseName == null ? "" : houseName + " ") + "shards");
        return switch (parts.size()) {
            case 0 -> "";
            case 1 -> parts.get(0);
            case 2 -> parts.get(0) + " and " + parts.get(1);
            default -> String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
        };
    }

    /** Whether a claim that pays shards is still to come on the Contracts page: the daily that completes all three, or the weekly. */
    static boolean shardsPending(Progression.State p) {
        return !p.claims.contains("weekly") || !(p.claims.contains("daily:0") && p.claims.contains("daily:1") && p.claims.contains("daily:2"));
    }

    /** Whether the next Chapter claim pays shards to a House, or is the Rare of tier 20 (the chips show then). */
    static boolean pickWanted() { int t = claimable(); return t > 0 && (Progression.SHARD_TIERS.contains(t) || t == Progression.RARE_TIER); }

    /** The Chapter's Claim plate when no tier waits: the reason (Progression.refusal) is its tooltip, one word its plate. */
    static String claimWhy() {
        if (claimable() > 0) return "";
        return Progression.refusal(progression(), Progression.SEASON + ":" + Math.min(lowestUnclaimed(), TIERS));
    }

    static String claimWord() { return claimable() > 0 ? "Claim" : lowestUnclaimed() > TIERS ? "Claimed" : "Locked"; }

    /** What a tier gives, in the reward's own words (Progression.reward), a shard tier naming the House the shards go to. */
    static String rewardOf(int t, String houseName) {
        String r = Progression.reward(t);
        return Progression.SHARD_TIERS.contains(t) ? r.replace("House shards", houseName + " shards") : r;
    }

    /** A Roll plaque's picture and its two lines, from the tier's reward words: a number leads ("30 gold" is Gold, 30), else the
     *  noun and the kind ("a Rare troop" is Rare, troop; "Pennant pattern"). {icon, line 1, line 2}. */
    static String[] rewardPlaque(int t) {
        String[] w = Progression.reward(t).split(" ");
        String l1, l2;
        if (w[0].matches("\\d+")) { l1 = Progression.cap(w[w.length - 1]); l2 = w[0]; }
        else { int i = w[0].equals("a") || w[0].equals("an") ? 1 : 0; l1 = w[i]; l2 = w.length > i + 1 ? w[w.length - 1] : ""; }
        String icon = Progression.GOLD_TIERS.contains(t) ? "chapter/ic-coins" : Progression.SHARD_TIERS.contains(t) ? "chapter/ic-shards"
            : t == Progression.RARE_TIER ? "recruit/wax-star" : "pennant_pattern".equals(Progression.COSMETIC_TIERS.get(t)) ? "chapter/ic-pennant"
            : Progression.COSMETIC_TIERS.containsKey(t) ? "chapter/ic-frame" : "chapter/ic-coins";
        return new String[]{icon, l1, l2};
    }

    /** One chip's width: padding, the seal, the name, padding. */
    static int chipWidth(String name) { return CHIP_PAD + CHIP_SEAL + CHIP_SEAL_GAP + font().getWidth(DarkUi.body(name)) + CHIP_PAD; }

    static int chipRowWidth() {
        int w = 0;
        for (var h : RecruitView.houses()) w += chipWidth(h.name()) + CHIP_GAP;
        return w - CHIP_GAP;
    }

    /** The five House chips from x at y, one widget each (house-<id>), in the Houses' order. */
    void houseChips(int x, int y) {
        for (var h : RecruitView.houses()) {
            final String id = h.id(), name = h.name();
            int w = chipWidth(name);
            var b = add("house-" + id, new UiButton(x, y, w, CHIP_H, Text.literal(name), bb -> pickHouse(id), (g, bx, by, bw, bh, hot) -> paintChip(g, id, name, bx, by, bw, bh)));
            b.setTooltip(Tooltip.of(Text.literal("Shards from claims go to " + name)));
            x += w + CHIP_GAP;
        }
    }

    /** A chip: the House's seal and its name on a parchment tag; the House the claim goes to is set into the table (darker, no drop
     *  shadow), carries a tick on its seal and a rule under it, so it is told by shape and depth and not by colour. */
    void paintChip(DrawContext g, String id, String name, int x, int y, int w, int h) {
        var font = font();
        var b = buttons.get("house-" + id);
        boolean on = id.equals(claimHouse());
        int dy = on ? 1 : 0;
        String tex = on ? "table/chip-house-on" : "table/chip-house";
        UiTex.nine(g, tex, x, y, w + 1, on ? h + CHIP_RULE : h + 2);
        for (var row : Spec.HOUSES)
            if (row.id().equals(id)) g.drawTexture(new Identifier(row.glyph()), x + CHIP_PAD, y + 4 + dy, 0f, 0f, CHIP_SEAL, CHIP_SEAL, CHIP_SEAL, CHIP_SEAL);
        if (on) UiTex.draw(g, "table/ready-check", x + CHIP_PAD + 8, y + 4 + dy + 9);
        var label = DarkUi.body(name);
        int tx = x + CHIP_PAD + CHIP_SEAL + CHIP_SEAL_GAP, ty = y + 6 + dy;
        g.drawText(font, label, tx, ty, DarkUi.INK_TEXT, false);
        if (hot(b)) underline(g, "house-" + id, tx, ty + 12, font.getWidth(label), DarkUi.INK_TEXT);
        rectsDrawn.put("house-" + id, new int[]{x, y, w, h});
        textsDrawn.put("house-" + id, name);
        texturesDrawn.put("house-" + id, tex);
        colorsDrawn.put("house-" + id, DarkUi.INK_TEXT);
        if (on) textsDrawn.put("house-chosen", id);
    }

    /** A banner colour chip: the side's colour as a swatch on a parchment chip; the colour the commander asks for (the profile's
     *  bannerColour) is set into the table, darker and ticked, as the chosen House chip is, so it is told by depth and not by colour alone. */
    void paintColour(DrawContext g, Spec.FactionsRow side, int x, int y, int w, int h) {
        var b = buttons.get("colour-" + side.id());
        boolean on = side.id().equals(CampaignClient.profile.bannerColour);
        String tex = on ? "table/chip-house-on" : "table/chip-house";
        UiTex.nine(g, tex, x, y, w + 1, h + 2);
        g.fill(x + 4, y + 3, x + w - 4, y + h - 3, DarkUi.OAK0);
        g.fill(x + 5, y + 4, x + w - 5, y + h - 4, DarkUi.opaque(side.color()));
        if (on) UiTex.draw(g, "table/ready-check", x + w - 12, y + 3);
        if (hot(b)) underline(g, "colour-" + side.id(), x + 4, y + h + 1, w - 8, DarkUi.INK_TEXT);
        rectsDrawn.put("colour-" + side.id(), new int[]{x, y, w, h});
        textsDrawn.put("colour-" + side.id(), side.name());
        texturesDrawn.put("colour-" + side.id(), tex);
        if (on) textsDrawn.put("colour-chosen", side.id());
    }

    /** A Banner chip: as paintChip, but the name is written only on the House shown; the others are a seal (the tooltip names them). */
    void paintBannerChip(DrawContext g, String id, String name, int x, int y, int w, int h) {
        var font = font();
        var b = buttons.get("house-" + id);
        boolean on = id.equals(shownHouse());
        int dy = on ? 1 : 0;
        String tex = on ? "table/chip-house-on" : "table/chip-house";
        UiTex.nine(g, tex, x, y, w + 1, on ? h + CHIP_RULE : h + 2);
        for (var row : Spec.HOUSES)
            if (row.id().equals(id)) g.drawTexture(new Identifier(row.glyph()), x + CHIP_PAD, y + 4 + dy, 0f, 0f, CHIP_SEAL, CHIP_SEAL, CHIP_SEAL, CHIP_SEAL);
        if (on) UiTex.draw(g, "table/ready-check", x + CHIP_PAD + 8, y + 4 + dy + 9);
        if (on) {
            var label = DarkUi.body(name);
            int tx = x + CHIP_PAD + CHIP_SEAL + CHIP_SEAL_GAP, ty = y + 6 + dy;
            g.drawText(font, label, tx, ty, DarkUi.INK_TEXT, false);
            if (hot(b)) underline(g, "house-" + id, tx, ty + 12, font.getWidth(label), DarkUi.INK_TEXT);
        } else if (hot(b)) underline(g, "house-" + id, x + CHIP_PAD, y + 4 + dy + CHIP_SEAL + 2, CHIP_SEAL, DarkUi.INK_TEXT);
        rectsDrawn.put("house-" + id, new int[]{x, y, w, h});
        textsDrawn.put("house-" + id, name);
        texturesDrawn.put("house-" + id, tex);
        colorsDrawn.put("house-" + id, DarkUi.INK_TEXT);
        if (on) textsDrawn.put("house-chosen", id);
    }

    void initContracts() {
        int[] o = col(C_CONTRACTS);
        int ox = o[0], oy = o[1];
        houseChips(ox + 480 - chipRowWidth() / 2, oy + CHIPS_Y); // before the Claims: the House is picked first, so Tab meets it first
        for (int i = 0; i < 3; i++) {
            final int slot = i;
            tag("claim-" + i, ox + NOTE_X + NOTE_STEP * i + 124, oy + NOTE_Y + DAILY_CLAIM_DY, 64, "Claim", () -> claimDaily(slot));
        }
        add("reroll", new UiButton(ox + REROLL_X, oy + REROLL_Y, 88, 24, Text.literal("Reroll"), b -> send("progress:reroll"), this::paintReroll));
        tag("claim-weekly", ox + WEEKLY_X + 228, oy + WEEKLY_Y + 64, 64, "Claim", () -> send(RecruitPages.contractClaim("weekly", claimHouse())));
    }

    /** A daily's Claim: the daily that completes all three names the House its shards go to (Progression.claim's rule, as RecruitPages.contractShards
     *  reads it); the other two carry none. */
    void claimDaily(int slot) {
        String key = "daily:" + slot;
        send(RecruitPages.contractShards(progression(), slot).isEmpty() ? "progress:" + key : RecruitPages.contractClaim(key, claimHouse()));
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
        for (var h : RecruitView.houses()) { // the row stays while a claim that pays shards is still to come
            var chip = buttons.get("house-" + h.id());
            if (chip != null) chip.visible = chip.active = shardsPending(p);
        }
    }

    void contracts(DrawContext g, int ox, int oy) {
        var p = progression();
        String[] notes = {"table/note-contract", "table/note-contract-b", "table/note-contract-c"};
        String house = claimHouse(), houseName = RosterMath.houseName(house);
        for (int i = 0; i < 3; i++) {
            int target = Progression.target(p, i);
            boolean done = p.claims.contains("daily:" + i);
            var pay = payOf("daily:" + i, house);
            String extra = !done && pay.shards() > 0 ? "+ " + pay.shards() + " " + houseName + " shards" : "";
            note(g, "note-" + i, notes[i], ox + NOTE_X + NOTE_STEP * i, oy + NOTE_Y, 188, Progression.contract(p, i), words(pay, false, null), extra, true,
                Math.min(Progression.progress(p, i), target), target, done, "contract-" + i, "progress-" + i, i == 1 ? "table/stamp-hollow-b" : "table/stamp-hollow", "stamp-" + i);
        }
        String hint = p.rerolled ? "Rerolled today" : p.claims.contains("daily:2") ? "The third contract is already claimed" : "Swap the third contract, once a day";
        text(g, "reroll-hint", DarkUi.body(hint), hint, ox + REROLL_X + 96, oy + REROLL_Y + 6, DarkUi.MUTED);
        int weekly = Progression.WEEKLY_BATTLES;
        boolean weeklyDone = p.claims.contains("weekly");
        note(g, "weekly", "table/note-weekly", ox + WEEKLY_X, oy + WEEKLY_Y, 292, "Complete " + weekly + " battles this week", words(payOf("weekly", house), true, weeklyDone ? null : houseName), "", false,
            Math.min(p.weeklyBattles, weekly), weekly, weeklyDone, "weekly", "progress-weekly", "table/stamp-hollow", "stamp-weekly");
        String reset = "Daily contracts reset at 00:00 UTC, the weekly on Monday";
        WarTableHome.centred(g, DarkUi.body(reset), ox + 480, oy + RESET_Y, DarkUi.MUTED);
        textsDrawn.put("reset", reset);
        if (shardsPending(p)) { // what the House row is for, left of it
            String cap = "Shards for";
            int cw = font().getWidth(DarkUi.body(cap)), cx = ox + 480 - chipRowWidth() / 2 - CAPTION_GAP - cw;
            text(g, "chips-caption", DarkUi.body(cap), cap, cx, oy + CHIPS_Y + 6, DarkUi.MUTED);
            rectsDrawn.put("chips-caption", new int[]{cx, oy + CHIPS_Y + 6, cw, 11});
        }
    }

    /** A contract's note: its name (a heading, its counts in words: UiMath.spellSmall) and its reward (SEPIA; the daily that completes all
     *  three adds a second line for its shards, which name the House picked), the progress bar and the count in 8 px right-aligned
     *  to right, and once claimed the Hollow's mark under the count, as the Chapter stamps a claimed tier. A tall note (the dailies)
     *  keeps its bar, count and mark lower, under the second line. Claim is a widget. */
    void note(DrawContext g, String key, String tex, int x, int y, int right, String name, String reward, String extra, boolean tall, int done, int target, boolean claimed,
              String nameKey, String countKey, String stamp, String stampKey) {
        var font = font();
        String id = key.startsWith("note-") ? key.substring(5) : key;
        rectsDrawn.put(key, UiTex.draw(g, tex, x, y));
        String shown = UiMath.trim(UiMath.spellSmall(name), right - 12, s -> font.getWidth(DarkUi.heading(s)));
        text(g, nameKey, DarkUi.heading(shown), shown, x + 12, y + 12, DarkUi.INK_TEXT);
        text(g, "reward-" + id, DarkUi.body(reward), reward, x + 12, y + 30, DarkUi.SEPIA);
        rectsDrawn.put("reward-" + id, new int[]{x + 12, y + 30, font.getWidth(DarkUi.body(reward)), 11});
        if (!extra.isEmpty()) {
            text(g, "shards-" + id, DarkUi.body(extra), extra, x + 12, y + 43, DarkUi.SEPIA);
            rectsDrawn.put("shards-" + id, new int[]{x + 12, y + 43, font.getWidth(DarkUi.body(extra)), 11});
        }
        int bar = y + (tall ? DAILY_BAR_DY : 52);
        UiTex.draw(g, "hud/hp-channel", x + 12, bar);
        UiTex.bar(g, "hud/hp-full-dawn", x + 13, bar + 1, UiMath.barWidth(target <= 0 ? 0 : done / (float) target), false);
        String count = claimed ? "Claimed" : done + " / " + target;
        int countW = font.getWidth(count);
        text(g, countKey, Text.literal(count), count, x + right - countW, bar - 1, DarkUi.INK_TEXT);
        rectsDrawn.put(countKey, new int[]{x + right - countW, bar - 1, countW, 8});
        if (claimed) {
            rectsDrawn.put(stampKey, UiTex.draw(g, stamp, x + right - 32, y + (tall ? DAILY_STAMP_DY : 62)));
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
    int first() { return paged ? pagedFirst : UiMath.trackFirst(lowestUnclaimed(), TIERS, shown()); }

    void initChapter() {
        int[] b0 = col(C_BLOCK);
        var tr = track();
        int n = tr.shown();
        houseChips(col(C_BLOCK_HEAD)[0] + 480, b0[1] + CHAPTER_CHIPS_Y); // the House and the Rare are picked before the Claim: Tab meets them first
        add("rare", new UiButton(b0[0] + CLAIM_X - GIVES_GAP - RARE_W, b0[1] + CLAIM_Y, RARE_W, CHIP_H, Text.literal("Rare troop"), b -> cycleRare(), this::paintRare));
        add("claim", new UiButton(b0[0] + CLAIM_X, b0[1] + CLAIM_Y, 240, 40, Text.literal("Claim"), b -> claim(), this::paintClaim));
        add("earlier", new UiButton(tr.x0() - 20, tr.rodY() - 3, 15, 15, Text.literal("Earlier tiers"), b -> turn(-1), (g, x, y, w, h, hot) -> paintArrow(g, "earlier", "chapter/arrow-left", "chapter/arrow-left-dim", x, y)));
        add("later", new UiButton(tr.x0() + PITCH * n - 7, tr.rodY() - 3, 15, 15, Text.literal("Later tiers"), b -> turn(1), (g, x, y, w, h, hot) -> paintArrow(g, "later", "chapter/arrow-right", "chapter/arrow-right-dim", x, y)));
        int first = first(), tier = tier();
        for (int k = 0; k < n; k++) { // a claimed title on the track is worn (or taken off) from its Roll plaque
            int t = first + k;
            if (titleAt(t) < 0 || !claimed(t)) continue;
            String title = Progression.TITLES[titleAt(t)];
            boolean cur = t == tier;
            add("title-" + t, new UiButton(tr.x0() + PITCH * k, cur ? tr.rollTop() - 8 : tr.rollTop(), PW, cur ? RH : PH, Text.literal(title), b -> wear(title), (g, x, y, w, h, hot) -> titleHot(g, t, x, y)));
        }
    }

    /** Claim takes a press while a tier waits; an arrow while the track can turn its way. */
    void refreshChapter() {
        var claim = buttons.get("claim");
        if (claim != null) {
            claim.active = claimable() > 0;
            String why = claimWhy();
            if (!why.equals(claimTip)) { claimTip = why; claim.setTooltip(why.isEmpty() ? null : Tooltip.of(Text.literal(why))); }
        }
        boolean pick = pickWanted(), rareWanted = claimable() == Progression.RARE_TIER;
        for (var h : RecruitView.houses()) {
            var chip = buttons.get("house-" + h.id());
            if (chip != null) chip.visible = chip.active = pick;
        }
        var rare = buttons.get("rare");
        if (rare != null) {
            rare.visible = rare.active = rareWanted && !rarePick().isEmpty();
            String tip = "Next Rare troop of " + RosterMath.houseName(claimHouse());
            if (!tip.equals(rareTip)) { rareTip = tip; rare.setTooltip(Tooltip.of(Text.literal(tip))); }
        }
        int first = first();
        var earlier = buttons.get("earlier");
        var later = buttons.get("later");
        if (earlier != null) earlier.active = first > 1;
        if (later != null) later.active = first < TIERS - shown() + 1;
    }

    /** Claim: the lowest tier reached and not yet claimed, with the House its shards go to or the Rare tier 20 gives (RecruitPages.foundersClaim:
     *  a gold, title or cosmetic tier carries neither). The track goes back to its place, to slide on when the claim lands. */
    void claim() {
        int t = claimable();
        if (t <= 0 || t == Progression.RARE_TIER && rarePick().isEmpty()) return;
        if (!send(RecruitPages.foundersClaim(t, claimHouse(), rarePick()))) return;
        freshTier = t;
        if (paged) { paged = false; screen.rebuild(); } // the title plaques follow the track back
    }

    /** A title plaque: wears its title, or takes it off (back to Recruit) when it is the one worn. */
    void wear(String title) { send("progress:title:" + (title.equals(progression().title) ? "Recruit" : title)); }

    /** The arrows: the track a page of SHOWN tiers back or on, within the chapter (UiMath.trackPage). */
    void turn(int pages) {
        int now = first(), next = UiMath.trackPage(now, pages, TIERS, shown());
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

    void chapter(DrawContext g) {
        var font = font();
        int tier = tier(), first = first();
        var tr = track();
        int[] h0 = col(C_HERO), b0 = col(C_BLOCK);
        hero(g, h0[0], h0[1], titleFrom(UiMath.heroTier(first, tr.shown(), TIERS, EVERY)), tier);
        block(g, col(C_BLOCK_HEAD)[0], b0[0], b0[1], tier, progression());
        gives(g, b0[0], b0[1]);
        int lx = tr.labelX();
        g.drawText(font, DarkUi.heading("Tier"), lx, tr.rodY() + 2, DarkUi.BRASS, false);
        g.drawText(font, DarkUi.heading("The Roll"), lx, tr.rollTop() + 28, DarkUi.BRASS, false);
        g.drawText(font, "for all", lx, tr.rollTop() + 44, DarkUi.MUTED, false);
        g.drawText(font, DarkUi.heading("The Oath"), lx, tr.oathTop() + 28, DarkUi.SEPIA_L, false);
        if (!WarTable.compact()) g.drawText(font, "not sworn", lx, tr.oathTop() + 44, DarkUi.MUTED, false);
        var rodTex = UiTex.entry("chapter/track-rod");
        int rod = PITCH * tr.shown(), rodW = rodTex.w();
        if (rod >= rodW) UiTex.draw(g, "chapter/track-rod", tr.x0() - 6, tr.rodY());
        else { // a shorter track (the compact page's nine tiers): the rod cut short of its right cap, the cap on its end
            UiTex.part(g, "chapter/track-rod", tr.x0() - 6, tr.rodY(), 0, 0, rod - 8, rodTex.h());
            UiTex.part(g, "chapter/track-rod", tr.x0() - 6 + rod - 8, tr.rodY(), rodW - 8, 0, 8, rodTex.h());
        }
        rectsDrawn.put("rod", new int[]{tr.x0() - 6, tr.rodY(), Math.min(rod, rodW), rodTex.h()});
        track(g, tr, first, tier);
        if (WarTable.compact()) { // in the label column, in place of "not sworn" (one fact said once), as two even lines
            java.util.function.ToIntFunction<String> wide = t -> font.getWidth(DarkUi.body(t));
            int uw = tr.x0() - 24 - lx, w = (wide.applyAsInt(UNSWORN) + 1) / 2;
            while (w < uw && !String.join(" ", UiMath.lines(UNSWORN, w, 2, wide)).equals(UNSWORN)) w++;
            var un = lines(g, UNSWORN, lx, tr.oathTop() + 44, w, 2, DarkUi.MUTED);
            textsDrawn.put("unsworn", String.join(" ", un));
            colorsDrawn.put("unsworn", DarkUi.MUTED);
            rectsDrawn.put("unsworn", new int[]{lx, tr.oathTop() + 44, w, 13 * un.size() - 2});
        } else text(g, "unsworn", DarkUi.body(UNSWORN), UNSWORN, tr.x0(), tr.oathTop() + 92, DarkUi.MUTED);
        textsDrawn.put("tier", String.valueOf(tier));
        textsDrawn.put("first", String.valueOf(first));
    }

    /** The ten tiers from first (and, while the track slides, the one leaving at the left, cut to the track): each tier's Roll and
     *  Oath plaques, the current tier's column last so its lifted plaque's shadow falls on its neighbours, then the tags on the
     *  rod over the chains' tops, the current tier's a wax pin (the chapter's seal carries its number). */
    void track(DrawContext g, Track tr, int first, int tier) {
        var font = font();
        int offset = slideOffset();
        List<Integer> ks = new ArrayList<>();
        for (int k = offset > 0 ? -1 : 0; k < tr.shown(); k++) if (first + k >= 1 && first + k <= TIERS) ks.add(k);
        ks.sort(Comparator.comparing(k -> first + k == tier));
        if (offset > 0) UiCanvas.scissor(g, tr.x0() - 4, tr.rodY() - 8, PITCH * tr.shown() - 4, tr.oathTop() + 96 - tr.rodY());
        try {
            for (int k : ks) {
                int t = first + k, x = tr.x0() + PITCH * k + offset;
                rollPlaque(g, t, x, tr, tier);
                oathPlaque(g, t, x, tr);
            }
            for (int k : ks) {
                int t = first + k, x = tr.x0() + PITCH * k + offset;
                if (t == tier) UiTex.draw(g, "chapter/pin-tier", x + 25, tr.rodY() - 4);
                else {
                    UiTex.draw(g, "chapter/tag-tier", x + 24, tr.tagY());
                    String n = String.valueOf(t);
                    g.drawText(font, n, x + 35 - font.getWidth(n) / 2, tr.tagY() + 4, DarkUi.PLATE_INK, false);
                }
            }
        } finally {
            if (offset > 0) UiCanvas.endScissor(g);
        }
    }

    /** A Roll plaque (pages.py's plaque_html, its words and picture from the tier's reward: rewardPlaque): claimed, it carries the Hollow's mark (wet on the tier this page just claimed);
     *  reached, a bright frame; not reached, the dull frame under the iron hasp on dull chains. The current tier's hangs lifted on
     *  short chains, 8 px taller. Each tier names what it gives (30 gold, 10 shards, a Rare troop, a pattern or frame), a title tier its title ("worn" while it is the one worn). */
    void rollPlaque(DrawContext g, int t, int x, Track tr, int tier) {
        var font = font();
        boolean cur = t == tier, done = claimed(t), locked = t > tier, title = titleAt(t) >= 0;
        int y = cur ? tr.rollTop() - 8 : tr.rollTop(), h = cur ? RH : PH, ly = 47 + tr.rollTop() - y; // one label line across the row
        String chain = locked ? "chapter/chain-dull-18" : cur ? "chapter/chain-10" : "chapter/chain-18";
        UiTex.draw(g, chain, x + 8, tr.rodY() + 6);
        UiTex.draw(g, chain, x + PW - 14, tr.rodY() + 6);
        String plate = cur ? "chapter/plaque-tier-raised" : locked ? (t % 2 == 1 ? "chapter/plaque-tier-dull" : "chapter/plaque-tier-dull-b")
            : (t % 2 == 1 ? "chapter/plaque-tier" : "chapter/plaque-tier-b");
        UiTex.draw(g, plate, x, y);
        String[] reward = title ? null : rewardPlaque(t);
        icon(g, title ? "chapter/ic-title" : reward[0], x, y, ly);
        if (locked) UiTex.draw(g, "chapter/hasp", x - 1, y + ly - 12);
        String l1 = title ? Progression.TITLES[titleAt(t)] : reward[1], l2 = title ? (done && l1.equals(progression().title) ? "worn" : "title") : reward[2];
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
    void oathPlaque(DrawContext g, int t, int x, Track tr) {
        var font = font();
        int y = tr.oathTop();
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
        int ly = 47 + track().rollTop() - y, w = font().getWidth(DarkUi.heading(Progression.TITLES[titleAt(t)]));
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
        String name = Progression.TITLES[titleAt(t)], kicker = "Tier " + t, kind = "Title, worn in camp";
        boolean got = claimed(t), worn = got && name.equals(progression().title);
        String state = worn ? "Worn in camp" : got ? "Claimed" : t <= tier ? "Yours to claim" : "Unlocks at " + UiMath.thousands(t * Progression.XP_PER_TIER) + " XP";
        g.drawText(font, kicker, cx - font.getWidth(kicker) / 2, y + 34, DarkUi.SEPIA, false);
        WarTableHome.centred(g, DarkUi.focal(name), cx, y + 48, DarkUi.INK_TEXT);
        WarTableHome.centred(g, DarkUi.body(kind), cx, y + 70, DarkUi.SEPIA);
        var copy = UiMath.lines(TITLE_COPY.getOrDefault(name, ""), 160, 3, s -> font.getWidth(DarkUi.body(s)));
        for (int i = 0; i < copy.size(); i++) WarTableHome.centred(g, DarkUi.body(copy.get(i)), cx, y + 92 + 13 * i, DarkUi.INK_TEXT);
        WarTableHome.centred(g, DarkUi.body(state), cx, y + 166, DarkUi.SEPIA);
        if (WarTable.compact()) UiTex.draw(g, "chapter/seal-title", x + C_SEAL_DX, y + C_SEAL_DY); // on the letter: above it is the rail
        else UiTex.draw(g, "chapter/seal-title", ox + 340, oy + 60);
        textsDrawn.put("hero", String.valueOf(t));
        textsDrawn.put("hero-name", name);
        textsDrawn.put("hero-state", state);
    }

    /** The chapter block (its name and seal from hx, the rest from ox; the same on the 960 page, 6 px apart on the compact,
     *  where the XP rail ends 12 px from the page's edge and the seal keeps clear of the hero letter): its name, the tier on the wax seal, the XP poured along the rail with the totals under its ends (Claim
     *  is a widget, paintClaim). */
    void block(DrawContext g, int hx, int ox, int oy, int tier, Progression.State p) {
        var font = font();
        g.drawText(font, "Season pass", hx + 480, oy + 80, DarkUi.MUTED, false);
        g.drawText(font, DarkUi.heading("The Founders"), hx + 480, oy + 92, DarkUi.BRASS, false);
        textsDrawn.put("chapter", "Season pass / The Founders");
        rectsDrawn.put("seal", UiTex.draw(g, "chapter/seal-tier-big", hx + 480, oy + 144));
        String n = String.valueOf(tier);
        g.drawText(font, n, hx + 502 - font.getWidth(n) / 2, oy + 162, DarkUi.CHIP_TEXT, false);
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
        String word = claimWord();
        var label = DarkUi.heading(word);
        int lx = WarTableHome.centred(g, label, x + 120, y + 14, ink);
        if (hot(b)) underline(g, "claim", lx, y + 26, font.getWidth(label), ink);
        rectsDrawn.put("claim", new int[]{x, y, w, h});
        texturesDrawn.put("claim", plate);
        textsDrawn.put("claim", word);
        colorsDrawn.put("claim", ink);
        String why = claimWhy();
        if (!why.isEmpty()) textsDrawn.put("claim-why", why);
    }

    /** The Rare chip (tier 20): the Rare troop to be given, a parchment tag with its name and an arrow; a press shows the House's next Rare. */
    void paintRare(DrawContext g, int x, int y, int w, int h, boolean hotIgnored) {
        var font = font();
        var b = buttons.get("rare");
        String id = rarePick(), name = UiMath.trim(troopName(id), w - 30, s -> font.getWidth(DarkUi.body(s)));
        UiTex.nine(g, "table/chip-house", x, y, w + 1, h + 2);
        var label = DarkUi.body(name);
        g.drawText(font, label, x + 8, y + 6, DarkUi.INK_TEXT, false);
        g.drawText(font, DarkUi.body(">"), x + w - 14, y + 6, DarkUi.SEPIA, false);
        if (hot(b)) underline(g, "rare", x + 8, y + 18, font.getWidth(label), DarkUi.INK_TEXT);
        rectsDrawn.put("rare", new int[]{x, y, w, h});
        textsDrawn.put("rare", name);
        textsDrawn.put("rare-id", id);
        colorsDrawn.put("rare", DarkUi.INK_TEXT);
    }

    /** Left of the Claim plate, right-aligned to it: what the tier waiting gives (a shard tier names the House picked), or at tier 20 the line
     *  under the Rare chip that says whose Rare it is and which of how many. Nothing while no tier waits. */
    void gives(DrawContext g, int ox, int oy) {
        int t = claimable();
        if (t <= 0) return;
        var font = font();
        int right = ox + CLAIM_X - GIVES_GAP, top = oy + CLAIM_Y;
        String houseName = RosterMath.houseName(claimHouse());
        if (t == Progression.RARE_TIER) {
            var rares = RecruitPages.rares(claimHouse());
            if (rares.isEmpty()) return;
            String note = "Rare of " + houseName + ", " + (rares.indexOf(rarePick()) + 1) + " of " + rares.size();
            int nw = font.getWidth(DarkUi.body(note));
            text(g, "rare-note", DarkUi.body(note), note, right - nw, top + 28, DarkUi.IVORY);
            rectsDrawn.put("rare-note", new int[]{right - nw, top + 28, nw, 11});
            return;
        }
        String head = "Tier " + t + " gives";
        text(g, "gives-head", DarkUi.body(head), head, right - font.getWidth(DarkUi.body(head)), top + 2, DarkUi.MUTED);
        var lines = UiMath.lines(rewardOf(t, houseName), GIVES_W, 2, s -> font.getWidth(DarkUi.body(s)));
        int left = right, low = top + 15;
        for (int i = 0; i < lines.size(); i++) {
            int w = font.getWidth(DarkUi.body(lines.get(i)));
            g.drawText(font, DarkUi.body(lines.get(i)), right - w, top + 15 + 13 * i, DarkUi.IVORY, false);
            left = Math.min(left, right - w);
            low = top + 15 + 13 * i + 11;
        }
        textsDrawn.put("gives", String.join(" ", lines));
        colorsDrawn.put("gives", DarkUi.IVORY);
        rectsDrawn.put("gives", new int[]{left, top + 15, right - left, low - top - 15});
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


    /** This commander's ready in the room, as its snapshot lists him. */
    static boolean mineReady() {
        String me = me();
        for (var e : rows("commanders")) {
            var r = e.getAsJsonObject();
            if (r.get("name").getAsString().equals(me)) return flag(r, "ready");
        }
        return false;
    }

    static int campW() { return WarTable.compact() ? C_CAMP_W : CAMP_W; }

    void initFriends() {
        int[] o = col(C_CAMP);
        int ox = o[0], oy = o[1];
        boolean party = flag("party"), lead = flag("partyLead");
        var camp = rows("camp");
        if (offset >= camp.size()) offset = 0;
        for (int k = 0; k < INVITES && offset + k < camp.size(); k++) {
            var row = camp.get(offset + k).getAsJsonObject();
            String name = row.get("name").getAsString(), key = "invite-" + k, theirs = row.has("room") ? row.get("room").getAsString() : "";
            int width = theirs.isEmpty() ? campW() : campW() - JOIN_W - 4;
            add(key, new UiButton(ox + CAMP_X, oy + INVITE_Y + INVITE_STEP * k, width, 24, Text.literal("Invite " + name), b -> send("social:invite:" + name), (g, x, y, w, h, hot) -> paintInvite(g, key, name, x, y, w, h)));
            if (!theirs.isEmpty()) tag("join-" + k, ox + CAMP_X + campW() - JOIN_W, oy + INVITE_Y + INVITE_STEP * k, JOIN_W, "Join", () -> joinFriend(theirs));
        }
        if (camp.size() > INVITES) tag("more", ox + CAMP_X, oy + INVITE_Y + INVITE_STEP * INVITES, 88, "More", () -> { offset = offset + INVITES < camp.size() ? offset + INVITES : 0; screen.rebuild(); });
        if (!value("inviter").isEmpty()) {
            tag("accept", ox + CAMP_X, oy + 344, 112, "Accept invitation", () -> send("social:accept"));
            tag("decline", ox + CAMP_X + 120, oy + 344, 72, "Decline", () -> send("social:decline"));
        }
        int[] po = col(C_PARTY);
        ox = po[0]; oy = po[1];
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
        int[] ro = col(C_ROOM);
        tag("council", ro[0] + ROOM_X + 16, ro[1] + ROOM_Y + (value("room").isEmpty() ? ROOM_TAG_DY : CREATE_DY), ROOM_W, value("room").isEmpty() ? "Open the war council" : "Back to the war council", () -> screen.page("lobby"));
    }

    /** What the party allows now, as the server rules it: invitations from the party's leader to a commander in no party, a room joined by a party's leader (or a commander alone) who is in none. */
    void refreshFriends() {
        boolean lead = flag("partyLead"), alone = !flag("party");
        var camp = rows("camp");
        for (int k = 0; k < INVITES; k++) {
            var b = buttons.get("invite-" + k);
            if (b != null && offset + k < camp.size()) b.active = lead && !flag(camp.get(offset + k).getAsJsonObject(), "party");
            var join = buttons.get("join-" + k);
            if (join != null) join.active = UiMath.roomOpen(!alone, lead) && value("room").isEmpty();
        }
    }

    /** Join on a friend's row: the room's code goes to the server, and the war council page opens on it. */
    void joinFriend(String theirs) {
        WarTableLobby.joining = true;
        send("social:join:" + theirs);
        screen.page("lobby");
    }

    void friends(DrawContext g) {
        int[] c = col(C_CAMP), p = col(C_PARTY), r = col(C_ROOM);
        camp(g, c[0], c[1]);
        party(g, p[0], p[1]);
        room(g, r[0], r[1]);
    }

    /** The camp's commanders who are in no party (their Invite tags are widgets), the line when there are none or they cannot be
     *  invited yet, and the party invitation waiting. */
    void camp(DrawContext g, int ox, int oy) {
        var font = font();
        int x = ox + CAMP_X;
        g.drawText(font, DarkUi.heading("In this camp"), x, oy + 124, DarkUi.BRASS, false);
        if (rows("camp").isEmpty()) {
            var server = MinecraftClient.getInstance().getServer();
            if (server != null) { // this machine runs the camp: how friends reach it (isRemote: open on the network)
                var how = lines(g, server.isRemote() ? CAMP_OPEN : CAMP_HOW, x, oy + INVITE_Y, campW(), 4, DarkUi.MUTED);
                textsDrawn.put("camp-how", String.join(" ", how));
            } else lines(g, "Commanders who join this server appear here.", x, oy + INVITE_Y, campW(), 2, DarkUi.MUTED); // no how-to to carry it
        } else if (!flag("partyLead")) {
            String hint = flag("party") ? "Only the party leader invites." : "Create a party to invite them.";
            text(g, "invite-hint", DarkUi.body(hint), hint, x, oy + 304, DarkUi.MUTED);
        }
        if (!value("inviter").isEmpty()) {
            String line = UiMath.trim("Invitation from " + value("inviter") + " (" + number("inviteSeconds", 0) + " s)", campW(), s -> font.getWidth(DarkUi.body(s)));
            text(g, "invitation", DarkUi.body(line), line, x, oy + 324, DarkUi.IVORY);
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
            String name = UiMath.trim(m.name(), PARTY_NAME_W, s -> font.getWidth(DarkUi.body(s))); // the page's body face, as the war council's seats draw a name
            text(g, "member-" + i, DarkUi.body(name), name, x + 56, y + 14, m.online() ? DarkUi.INK_TEXT : DarkUi.SEPIA);
            text(g, "status-" + i, DarkUi.body(status(m)), status(m), x + 56, y + 28, DarkUi.SEPIA);
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

    /** The war council's note: the room is run from the war council page (its code, teams, bots and start), so this note says so and carries the one tag that opens it (a widget). */
    void room(DrawContext g, int ox, int oy) {
        var font = font();
        int x = ox + ROOM_X, y = oy + ROOM_Y, lx = x + 16;
        String room = value("room");
        rectsDrawn.put("room", UiTex.draw(g, "table/note-room-short", x, y));
        texturesDrawn.put("room", "table/note-room-short");
        g.drawText(font, DarkUi.heading("War council"), lx, y + 12, DarkUi.INK_TEXT, false);
        if (!room.isEmpty()) {
            var lead = DarkUi.body("You are in room");
            g.drawText(font, lead, lx, y + 30, DarkUi.SEPIA, false);
            drawCode(g, font, room, lx + font.getWidth(lead) + 5, y + 30, DarkUi.SEPIA);
            textsDrawn.put("room-line", "You are in room " + room + ".");
        }
    }

    // ======================================================================================================================= Field

    /** A room code drawn in Minecraft's face with one fixed cell per character (the body font spaces its letters unevenly): the cell's width, the run's width, and the draw. */
    static final int CODE_CELL = 8;
    static int codeWidth(String code) { return CODE_CELL * code.length(); }
    static void drawCode(DrawContext g, TextRenderer font, String code, int x, int y, int color) {
        for (int i = 0; i < code.length(); i++) {
            var t = Text.literal(String.valueOf(code.charAt(i)));
            g.drawText(font, t, x + CODE_CELL * i + (CODE_CELL - font.getWidth(t)) / 2, y, color, false);
        }
    }

    /** The Field page's origin: the sheet and the Statuses paper (674 px with the gap) stand as one pair centred under the title (page centre 480 on the 960 page; the compact page's C_FIELD does the same). */
    static int[] fieldCol() {
        int[] o = col(C_FIELD);
        return WarTable.compact() ? o : new int[]{o[0] + FIELD_DX, o[1]};
    }

    /** A status line wrapped for the paper, a number and its unit ("3 s") kept on one line (a no-break space the wrap helper does not cut at, drawn and measured as a space). */
    static List<String> glossLines(String line, java.util.function.ToIntFunction<String> width) {
        String bound = line.replaceAll("(\\d) s\\b", "$1\u00a0s");
        var out = new java.util.ArrayList<String>();
        for (String l : UiMath.lines(bound, GLOSS_W - 20, GLOSS_LINES, v -> width.applyAsInt(v.replace('\u00a0', ' ')))) out.add(l.replace('\u00a0', ' '));
        return out;
    }

    void initField() {
        int[] o = fieldCol();
        int ox = o[0], oy = o[1];
        for (int i = 0; i < Spec.MAPS.length; i++) {
            final int index = i;
            int y = oy + MAP_Y + MAP_STEP * i;
            add("map-" + i, new UiButton(ox + MAP_X, y, MAP_W, 28, Text.literal(Spec.MAPS[i].name()), b -> viewed = index, (g, x, by, w, h, hot) -> paintMapRow(g, index, x, by)));
        }
        for (int i = 0; i < TIER_IDS.length; i++) {
            String id = TIER_IDS[i], key = "bots-" + id, label = CampaignClient.tierName(id);
            add(key, new UiButton(ox + TIER_X[i], oy + BOTS_Y, TIER_W[i], 24, Text.literal(label), b -> send("difficulty:" + id), (g, x, y, w, h, hot) -> vellum(g, key, x, y, w, h, label, id.equals(CampaignClient.difficulty))));
        }
    }

    /** The bots' tier takes a press while no battle runs (the server's rule). */
    void refreshField() {
        for (String id : TIER_IDS) {
            var b = buttons.get("bots-" + id);
            if (b != null) b.active = !GravesAndGloryClient.running;
        }
    }

    /** The sheet: the map rows (widgets, paintMapRow) and the line under them that says where the map is picked; the card of the map in
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
        String status = GravesAndGloryClient.running ? "A battle is on: maps wait until it ends" : "The war council host picks the map";
        text(g, "status", DarkUi.body(status), status, ox + MAP_X, oy + STATUS_Y, DarkUi.INK_TEXT);
        WarTableHome.centred(g, DarkUi.heading("Bots"), ox + CARD_CX, oy + BOTS_HEAD_Y, DarkUi.INK_TEXT);
        textsDrawn.put("bots-current", CampaignClient.tierName(CampaignClient.difficulty));
        statuses(g, ox, oy);
    }

    /** The Statuses glossary: a paper of the sheet's parchment right of the sheet, a head, and a row per status (Statuses.Glossary) with its glyph, its name and its line wrapped to the paper. Recorded as statuses (the paper), status-(id) (the row), status-(id)-icon, and the texts status-(id) (the name) and status-(id)-line. */
    void statuses(DrawContext g, int ox, int oy) {
        var font = font();
        int x = ox + GLOSS_X, y = oy + GLOSS_Y;
        UiTex.part(g, "table/sheet-field", x, y, 190, 0, GLOSS_W, GLOSS_H); // the sheet's own rows, so its top and torn rim line up with the main paper's
        UiTex.draw(g, "table/tack", x + 6, y + 4);
        UiTex.draw(g, "table/tack", x + GLOSS_W - 12, y + 4);
        rectsDrawn.put("statuses", new int[]{x, y, GLOSS_W, GLOSS_H});
        g.drawText(font, DarkUi.heading("Statuses"), x + 10, y + 10, DarkUi.INK_TEXT, false);
        g.fill(x + 10, y + 24, x + GLOSS_W - 10, y + 25, DarkUi.SEPIA_LINE);
        textsDrawn.put("statuses-head", "Statuses");
        int prevLines = 1, ry = y + GLOSS_ROW_Y - GLOSS_STEP; // each row starts below the last row's measured text and a clear gap, so a long line never meets the next name
        for (var e : Statuses.Glossary.entries()) {
            var lines = glossLines(e.line(), v -> font.getWidth(DarkUi.body(v)));
            ry += ry == y + GLOSS_ROW_Y - GLOSS_STEP ? GLOSS_STEP : 13 + 11 * Math.max(1, prevLines) + GLOSS_GAP;
            prevLines = lines.size();
            int ix = x + 10;
            for (int r = 0; r < e.glyph().length; r++)
                for (int c = 0; c < 9; c++) {
                    char ch = e.glyph()[r].charAt(c);
                    if (ch != ' ') g.fill(ix + c, ry + 1 + r, ix + c + 1, ry + 2 + r, ch == 'k' ? DarkUi.INK_TEXT : 0xff000000 | e.colour());
                }
            rectsDrawn.put("status-" + e.id() + "-icon", new int[]{ix, ry + 1, 9, e.glyph().length});
            g.drawText(font, DarkUi.heading(e.name()), x + 24, ry, DarkUi.INK_TEXT, false);
            int right = x + 24 + font.getWidth(DarkUi.heading(e.name()));
            for (int i = 0; i < lines.size(); i++) {
                g.drawText(font, DarkUi.body(lines.get(i)), x + 10, ry + 13 + 11 * i, DarkUi.INK_TEXT, false);
                right = Math.max(right, x + 10 + font.getWidth(DarkUi.body(lines.get(i))));
            }
            rectsDrawn.put("status-" + e.id(), new int[]{ix, ry, right - ix, 13 + 11 * Math.max(1, lines.size()) - 2});
            textsDrawn.put("status-" + e.id(), e.name());
            textsDrawn.put("status-" + e.id() + "-line", String.join(" ", lines));
        }
    }

    /** A map's row: its name in INK, a tack by the map whose card shows; a click shows its card. */
    void paintMapRow(DrawContext g, int i, int x, int y) {
        var font = font();
        var b = buttons.get("map-" + i);
        int ink = DarkUi.INK_TEXT;
        if (i == viewed) UiTex.draw(g, "table/tack", x + 2, y + 11);
        String name = UiMath.trim(Spec.MAPS[i].name(), MAP_W - 20, s -> font.getWidth(DarkUi.heading(s)));
        g.drawText(font, DarkUi.heading(name), x + 14, y + 8, ink, false);
        if (i != viewed && hot(b)) underline(g, "map-" + i, x + 14, y + 20, font.getWidth(DarkUi.heading(name)), ink);
        rectsDrawn.put("map-" + i, new int[]{x, y, MAP_W, 28});
        textsDrawn.put("map-" + i, name);
        colorsDrawn.put("map-" + i, ink);
    }
}
