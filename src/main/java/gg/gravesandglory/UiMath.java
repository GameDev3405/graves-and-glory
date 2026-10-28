package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/** Pure UI maths for the canvas, text fitting and colour checks; no Minecraft types, so the standalone check runs it. */
public final class UiMath {
    private UiMath() {}

    /** Screen pixels per design pixel: the 960x540 canvas grows in whole steps with the framebuffer, never past the player's GUI Scale. */
    public static int pixelScale(int fbW, int fbH, int guiScale) {
        return Math.min(guiScale, Math.max(1, (int) Math.floor(Math.min(fbW / 960.0, fbH / 540.0))));
    }

    /** The text unchanged if it fits in maxW, else its longest prefix plus "..." that fits ("" when not even "..." fits). */
    public static String trim(String s, int maxW, ToIntFunction<String> width) {
        if (width.applyAsInt(s) <= maxW) return s;
        for (int n = s.length() - 1; n >= 0; n--) {
            if (n > 0 && Character.isHighSurrogate(s.charAt(n - 1))) continue; // never split a surrogate pair
            String t = s.substring(0, n) + "...";
            if (width.applyAsInt(t) <= maxW) return t;
        }
        return "";
    }

    /** The text unchanged if it fits in maxW, else cut after its last whole word that leaves room for "...", with no space or
     *  stop before the dots; a first word too long for that is cut inside it (trim). */
    public static String trimWords(String s, int maxW, ToIntFunction<String> width) {
        if (width.applyAsInt(s) <= maxW) return s;
        for (int at = s.lastIndexOf(' '); at > 0; at = s.lastIndexOf(' ', at - 1)) {
            int n = at;
            while (n > 0 && " .,;:".indexOf(s.charAt(n - 1)) >= 0) n--;
            String t = s.substring(0, n) + "...";
            if (n > 0 && width.applyAsInt(t) <= maxW) return t;
        }
        return trim(s, maxW, width);
    }

    /** The text in at most `lines` lines no wider than maxW. A line too long breaks after the last sentence end (". ", "! ", "? ")
     *  that fits, else at the last space that fits; the last line, or a line with no space that fits, ends as trimWords ends it.
     *  Joined with spaces, the lines are the text again when nothing was trimmed. */
    public static List<String> lines(String s, int maxW, int lines, ToIntFunction<String> width) {
        List<String> out = new ArrayList<>();
        String rest = s.strip();
        while (!rest.isEmpty() && out.size() < lines) {
            if (out.size() == lines - 1 || width.applyAsInt(rest) <= maxW) { out.add(trimWords(rest, maxW, width)); break; }
            int space = -1, sentence = -1;
            for (int at = rest.indexOf(' '); at > 0 && width.applyAsInt(rest.substring(0, at)) <= maxW; at = rest.indexOf(' ', at + 1)) {
                space = at;
                if (".!?".indexOf(rest.charAt(at - 1)) >= 0) sentence = at;
            }
            int cut = sentence > 0 ? sentence : space;
            if (cut <= 0) { out.add(trimWords(rest, maxW, width)); break; }
            out.add(rest.substring(0, cut));
            rest = rest.substring(cut + 1).strip();
        }
        return out;
    }

    /** WCAG 2 contrast ratio of two colours (alpha ignored): 1 for equal colours, 21 for black on white. */
    public static double contrast(int argbA, int argbB) {
        double a = luminance(argbA), b = luminance(argbB);
        return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
    }

    static double luminance(int argb) {
        return 0.2126 * channel(argb >> 16 & 0xff) + 0.7152 * channel(argb >> 8 & 0xff) + 0.0722 * channel(argb & 0xff);
    }

    static double channel(int c) {
        double v = c / 255.0;
        return v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    /** Width of a brass key cap round its label: 13 for one character, else the label plus 9, at most 48 (key-brass-13 to key-brass-48). */
    public static int brassKeyWidth(int textW, int chars) {
        return chars == 1 ? 13 : Math.min(48, textW + 9);
    }

    /** Width of an ink key chip round its label: the label plus 4, at least 10. */
    public static int chipWidth(int textW) {
        return Math.max(10, textW + 4);
    }

    /** A key's name trimmed to maxW. A name of several words keeps its last word whole after the dots, with as much of its start as
     *  fits before them (Right Shift as R...Shift, Keypad 4 as Key...4), so keys that share a start still read apart; a one-word
     *  name, or one too long for that, keeps its start (Back...). No space is left before the dots; "" when not even "..." fits. */
    public static String trimKey(String s, int maxW, ToIntFunction<String> width) {
        if (width.applyAsInt(s) <= maxW) return s;
        int space = s.lastIndexOf(' ');
        if (space > 0) {
            String last = s.substring(space + 1), head = s.substring(0, space).stripTrailing();
            for (int n = head.length(); n >= 1; n--) {
                if (Character.isHighSurrogate(head.charAt(n - 1))) continue; // never split a surrogate pair
                String t = head.substring(0, n).stripTrailing() + "..." + last;
                if (!last.isEmpty() && width.applyAsInt(t) <= maxW) return t;
            }
        }
        for (int n = s.length() - 1; n >= 0; n--) {
            if (n > 0 && Character.isHighSurrogate(s.charAt(n - 1))) continue;
            String t = s.substring(0, n).stripTrailing() + "...";
            if (width.applyAsInt(t) <= maxW) return t;
        }
        return "";
    }

    /** A hint-slip row's width: each key chip (chipWidth) and 2 px after it, each word (null for none) and 8 px after it, less the
     *  gap after the last. */
    public static int hintRowWidth(List<String> keys, List<String> words, ToIntFunction<String> width) {
        return hintRowWidth(keys, words, 8, width);
    }

    /** A key row's width as hintRowWidth measures it, with wordGap px after each word in place of the slip's 8 (a tall card's row). */
    public static int hintRowWidth(List<String> keys, List<String> words, int wordGap, ToIntFunction<String> width) {
        int w = 0, gap = 0;
        for (int k = 0; k < keys.size(); k++) {
            w += chipWidth(width.applyAsInt(keys.get(k))) + 2;
            gap = 2;
            String word = k < words.size() ? words.get(k) : null;
            if (word != null) {
                w += width.applyAsInt(word) + wordGap;
                gap = wordGap;
            }
        }
        return w - gap;
    }

    /** A hint-slip row's key names fitted to maxW: as they are when the row fits, else each trimmed by trimKey to the widest common
     *  cap at which the row fits, so the longest names give way first, and alike. A narrower cap never widens a name, so the cap is
     *  found by halving. */
    public static List<String> fitKeys(List<String> keys, List<String> words, int maxW, ToIntFunction<String> width) {
        return fitKeys(keys, words, maxW, 8, width);
    }

    /** fitKeys for a key row with wordGap px after each word in place of the slip's 8 (a tall card's row). */
    public static List<String> fitKeys(List<String> keys, List<String> words, int maxW, int wordGap, ToIntFunction<String> width) {
        return fitKeys(keys, keys, words, maxW, wordGap, width);
    }

    /** A key row's names fitted to maxW, given each key's short name as well (HintText.keyShort: Right Shift as RShift, Page Up as
     *  PgUp). The full names when the row fits; else the short names when they fit; else the short names trimmed to the widest
     *  common cap at which the row fits, as fitKeys trims. A narrower cap never widens a name, so the cap is found by halving. */
    public static List<String> fitKeys(List<String> keys, List<String> shorts, List<String> words, int maxW, int wordGap, ToIntFunction<String> width) {
        if (hintRowWidth(keys, words, wordGap, width) <= maxW) return keys;
        if (hintRowWidth(shorts, words, wordGap, width) <= maxW) return shorts;
        int lo = 0, hi = 0;
        for (String k : shorts) hi = Math.max(hi, width.applyAsInt(k));
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (hintRowWidth(capped(shorts, mid, width), words, wordGap, width) <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return capped(shorts, lo, width);
    }

    static List<String> capped(List<String> keys, int cap, ToIntFunction<String> width) {
        List<String> out = new ArrayList<>();
        for (String k : keys) out.add(trimKey(k, cap, width));
        return out;
    }

    /** The x on a tall card where its key row must end, from the card's left edge: where the state word and the count end. */
    public static final int CARD_ROW_END = 166;
    /** The px a tall card's key row keeps clear after each word, before the next chip. */
    public static final int CARD_ROW_GAP = 3;
    /** The mock's places for a tall card's three key chips, x from the card's left edge (UI plan: 10, 58 and 105). */
    private static final int[] CARD_KEY_AT = {10, 58, 105};

    /** A tall card's key row as drawn: each chip's text (its key's name, or a trim of it), the word beside it, and the chip's x
     *  from the card's left edge. */
    public record CardRow(List<String> keys, List<String> words, List<Integer> at) {}

    /** A tall card's key row for its three order keys, given their words and short words for a crowded row. Each chip sits at the
     *  mock's place while every pair (the chip, 2 px, its word) ends CARD_ROW_GAP px short of the next chip and the last by
     *  CARD_ROW_END: one-character keys, the defaults. Else the pairs flow from x 10, CARD_ROW_GAP px apart, with the short words,
     *  and long names give way alike as on the hint slip (fitKeys), so no pair runs into the next and the row ends by
     *  CARD_ROW_END. */
    public static CardRow cardRow(List<String> keys, List<String> words, List<String> shortWords, ToIntFunction<String> width) {
        return cardRow(keys, keys, words, shortWords, width);
    }

    /** cardRow given each key's short name as well: a flowing row takes the full names when they fit, else the short names, as
     *  fitKeys picks them. */
    public static CardRow cardRow(List<String> keys, List<String> shorts, List<String> words, List<String> shortWords, ToIntFunction<String> width) {
        boolean fixed = true;
        for (int k = 0; k < keys.size(); k++) {
            int end = CARD_KEY_AT[k] + chipWidth(width.applyAsInt(keys.get(k))) + 2 + width.applyAsInt(words.get(k));
            if (end > (k + 1 < keys.size() ? CARD_KEY_AT[k + 1] - CARD_ROW_GAP : CARD_ROW_END)) fixed = false;
        }
        List<Integer> at = new ArrayList<>();
        if (fixed) {
            for (int k = 0; k < keys.size(); k++) at.add(CARD_KEY_AT[k]);
            return new CardRow(List.copyOf(keys), List.copyOf(words), List.copyOf(at));
        }
        List<String> fitted = fitKeys(keys, shorts, shortWords, CARD_ROW_END - CARD_KEY_AT[0], CARD_ROW_GAP, width);
        int x = CARD_KEY_AT[0];
        for (int k = 0; k < fitted.size(); k++) {
            at.add(x);
            x += chipWidth(width.applyAsInt(fitted.get(k))) + 2 + width.applyAsInt(shortWords.get(k)) + CARD_ROW_GAP;
        }
        return new CardRow(List.copyOf(fitted), List.copyOf(shortWords), List.copyOf(at));
    }

    /** Centre x of point i's seal of n: a 28 px pitch centred on the canvas (452, 480, 508 for three at 960). */
    public static int sealCentre(int i, int n, int canvasW) {
        return canvasW / 2 + 28 * i - 14 * (n - 1);
    }

    /** A card's head count: alive of the round's most, as "9/12". */
    public static String countText(int alive, int full) {
        return alive + "/" + full;
    }

    /** Round clock as m:ss (7:42); negative time reads 0:00. */
    public static String clock(int seconds) {
        int s = Math.max(0, seconds);
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    /** Filled pixels of a 54 px score track: round(54*score/win), clamped to 0..54, 0 when there is no win score. */
    public static int trackFill(int score, int win) {
        if (win <= 0) return 0;
        return (int) Math.max(0, Math.min(54, Math.round(54.0 * score / win)));
    }

    /** Filled pixels of a card's 102 px strength bar. */
    public static int barWidth(float strength) {
        return Math.round(Math.max(0f, Math.min(1f, strength)) * 102);
    }

    /** How long an event feed line lives at the Compact HUD size (UI Task 8c), and at Standard and Large (the spec's 8 s), and the
     *  last stretch over which it fades out (presentation timings). */
    public static final long FEED_LIFE_COMPACT_MS = 6000, FEED_LIFE_MS = 8000, FEED_FADE_MS = 2000;
    /** The most lines the feed shows at once at the Compact HUD size, and at Standard and Large (the spec's five). */
    public static final int FEED_LINES_COMPACT = 3, FEED_LINES = 5;

    /** A feed line's life at a HUD size (HUD_COMPACT, HUD_STANDARD or HUD_LARGE). */
    public static long feedLife(int size) { return size == HUD_COMPACT ? FEED_LIFE_COMPACT_MS : FEED_LIFE_MS; }

    /** The most feed lines shown at once at a HUD size. */
    public static int feedLines(int size) { return size == HUD_COMPACT ? FEED_LINES_COMPACT : FEED_LINES; }

    /** An event feed line's alpha at ageMs of a lifeMs life: 1 until its last FEED_FADE_MS, then down in a straight line to 0. */
    public static float feedAlpha(long ageMs, long lifeMs) {
        long left = lifeMs - ageMs;
        if (left <= 0) return 0f;
        return left >= FEED_FADE_MS ? 1f : left / (float) FEED_FADE_MS;
    }

    /** A live portrait's model size (InventoryScreen.drawEntity's size, px per block) in a square window `window` px wide: the
     *  window shows 20 model px, from 4 above the head to 8 below the shoulders, plus `tall` rows for a Tall +N troop (unit
     *  identity rule 8): 2 px per model px in a 40 px window, 8 in a 160 px one. */
    public static int portraitSize(int window, int tall) {
        return (int) Math.round(16.0 * window / (20.0 + tall));
    }

    /** Where a live portrait's feet go, px below the window's top: the 32 model px of a player plus the 4 above the head, at
     *  portraitSize's scale, so the head sits 4 model px under the window's top. */
    public static int portraitFeet(int window, int tall) {
        return (int) Math.round((36.0 + tall) * window / (20.0 + tall));
    }

    /** The x of the label on the 240 px Ride plate, from the plate's left: centred on the plate while that keeps it clear of the
     *  Enter key (which ends at x 51), else from x 57. */
    public static int plateLabelX(int textW) {
        return textW <= 126 ? 120 - textW / 2 : 57;
    }

    /** The War Table's order sheets hand the focus over in SHEET_TICKS client ticks (150 ms, UI spec 4.2). */
    public static final int SHEET_TICKS = 3;

    /** How far the focused order sheet has risen from flat (0) to raised (1), `ticksSinceFocus` whole client ticks plus `partial`
     *  of one after the focus reached it: an ease-out over SHEET_TICKS ticks. The sheet that lost the focus eases flat on the same
     *  curve (1 less this). */
    public static float sheetRise(long ticksSinceFocus, float partial) {
        double t = Math.max(0.0, Math.min(1.0, (ticksSinceFocus + (double) partial) / SHEET_TICKS));
        double u = 1.0 - t;
        return (float) (1.0 - u * u * u);
    }

    /** The wax seal's stamp takes the last SEAL_TICKS ticks of the rise (100 ms). */
    public static final int SEAL_TICKS = 2;

    /** The wax seal stamping down on the risen sheet: 0 until the last SEAL_TICKS ticks of the rise, then 0 to 1, falling faster
     *  as it goes (an ease-in, so it lands with a hit). */
    public static float sealStamp(long ticksSinceFocus, float partial) {
        double t = Math.max(0.0, Math.min(1.0, (ticksSinceFocus + (double) partial - (SHEET_TICKS - SEAL_TICKS)) / SEAL_TICKS));
        return (float) (t * t);
    }

    /** The recruit ladder's rung in Phase 1 (UI plan Task 6): the House's troops still to be drawn, one letter each. */
    public static String lettersLine(int left) {
        if (left <= 0) return "No letters left";
        return left == 1 ? "1 letter left" : left + " letters left";
    }

    /** The rung once Phase 2 counts toward the next Epic: `left` letters to go, 1 meaning the next one is sure to be it. */
    public static String pityLine(int left) {
        return left <= 1 ? "Epic in the next letter" : "Epic within " + left + " letters";
    }

    /** The first line of the Odds note: each of the `remaining` troops not yet drawn is equally likely (no duplicates). */
    public static String oddsLine(int remaining) {
        if (remaining <= 0) return "Nothing left to draw";
        return String.format(java.util.Locale.ROOT, "Each new troop: %.1f%%", 100.0 / remaining);
    }

    /** A list's place after a turn of the wheel (UI plan Task 6, the recruit roll): the first row shown, and the part of a row's
     *  turn carried toward the next. */
    public record Wheel(int first, double carry) {}

    /** The wheel turned by `amount` (what Minecraft hands Screen.mouseScrolled: positive away from the player, and 0 or a fraction
     *  as well as whole notches) over a list whose first row shown is `first`, from 0 to `last`, with `carry` left from the turns
     *  before: a row down for each whole unit turned toward the player, a row up for each away, and the fraction carried, so a
     *  touchpad's small turns add up to a row. A turn of 0 (a sideways-only scroll) moves nothing; nothing carries past either
     *  end, so a turn back from an end moves at once. */
    public static Wheel wheel(int first, int last, double carry, double amount) {
        if (amount == 0 || !Double.isFinite(amount)) return new Wheel(first, carry);
        double total = carry - amount;
        long rows = (long) (total + Math.copySign(1e-9, total)); // toward zero; ten turns of 0.1 make a row
        double rest = total - rows;
        if (Math.abs(rest) < 1e-9) rest = 0;
        long to = first + rows;
        int next = (int) Math.max(0, Math.min(last, to));
        boolean end = to != next || next >= last && rest > 0 || next <= 0 && rest < 0;
        return new Wheel(next, end ? 0 : rest);
    }

    /** A count with thousands separators, as the Chapter page writes its XP ("2,870"). */
    public static String thousands(int n) {
        return String.format(java.util.Locale.ROOT, "%,d", n);
    }

    private static final String[] SMALL = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen", "twenty"};
    private static final java.util.regex.Pattern COUNT = java.util.regex.Pattern.compile("(?<![\\w.,])(\\d{1,2})(?![\\w%]|[.,]\\d)");

    /** A heading's small counts (0 to 20) in words ("Complete 1 battle" reads "Complete one battle"): the heading face draws
     *  old-style figures, its 1 and 0 close to an I and an O. A number joined to letters or to another number (a code, 1v1, 2.5,
     *  1,000) stays as it is, and so does a larger count. */
    public static String spellSmall(String s) {
        var m = COUNT.matcher(s);
        var out = new StringBuilder();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            boolean word = n <= 20 && !(m.group(1).length() > 1 && m.group(1).charAt(0) == '0');
            m.appendReplacement(out, word ? SMALL[n] : m.group(1));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** The Chapter track's first tier shown (UI plan Task 7, hero rule H1): the tier before the lowest one not yet claimed, so the
     *  last claim stays in view at the left and the track slides one plaque with each claim; never before tier 1, never past the
     *  last `shown` tiers of `tiers`. */
    public static int trackFirst(int lowestUnclaimed, int tiers, int shown) {
        return Math.max(1, Math.min(tiers - shown + 1, lowestUnclaimed - 1));
    }

    /** The track's first tier after the arrows turn `pages` pages of `shown` tiers (negative: back), within the same ends. */
    public static int trackPage(int first, int pages, int tiers, int shown) {
        return Math.max(1, Math.min(tiers - shown + 1, first + pages * shown));
    }

    /** The hero's tier: the first tier on a multiple of `every` (a title in Phase 1) past the track's right end, so the hero never
     *  repeats a plaque; the last tier once the track shows it. */
    public static int heroTier(int first, int shown, int tiers, int every) {
        int t = ((first + shown - 1) / every + 1) * every;
        return Math.min(tiers, t);
    }

    /** Columns of the XP rail's channel poured for `xp` of a chapter of `tiers` tiers at `perTier` XP each, rounded. */
    public static int xpPoured(int xp, int perTier, int tiers, int columns) {
        double full = (double) perTier * tiers;
        return (int) Math.round(columns * Math.max(0, Math.min(full, xp)) / full);
    }

    /** The cooldown sweep's pixel class at (x,y) of a size x size square with `remain` (0..1) of the time left:
     *  0 clear, 1 veil over the part of the turn still to come (clockwise from twelve), 2 the bright leading edge,
     *  3 the start line at twelve. A port of the cooldown painter in tools/ui_art/v4a_hud.py. */
    public static int sweep(int x, int y, int size, double remain) {
        double c = (size - 1) / 2.0, start = 2 * Math.PI * (1 - remain);
        double ex = Math.sin(start), ey = -Math.cos(start), dx = x - c, dy = y - c;
        double ang = Math.atan2(dx, -dy);
        if (ang < 0) ang += 2 * Math.PI;
        int k = ang >= start ? 1 : 0;
        double along = dx * ex + dy * ey, across = Math.abs(dx * ey - dy * ex);
        if (along > 0.5 && across < 0.62) k = 2;
        if (k > 0 && x == (int) Math.rint(c) && y <= c) k = 3;
        return k;
    }

    /** The Friends page's private room (UI Task 8b): a commander alone or leading their party makes and joins rooms, as the server
     *  rules it (SocialHub.lead: no party, or its leader); a party member who does not lead cannot, so Create private room and
     *  Join with party are dull for them and the room's note carries roomWhy beside them, not tone alone. */
    public static boolean roomOpen(boolean inParty, boolean leads) { return !inParty || leads; }

    /** The room note's word for a party member who does not lead (189 px of the body face, one line of the note's 196). */
    public static final String ROOM_LEADER_ONLY = "Your party leader makes or joins rooms.";

    /** Why the room's tags are dull for this commander: ROOM_LEADER_ONLY while roomOpen is false, else nothing (""). */
    public static String roomWhy(boolean inParty, boolean leads) { return roomOpen(inParty, leads) ? "" : ROOM_LEADER_ONLY; }

    // The battle HUD's size and layout (UI Task 8c, which replaces Task 8a's scale rule: Kort found too much of the screen blocked).
    // BattleHud draws every plate where hudLayout puts it, hudFits lays the HUD out by the same method, and the ui-hud suite checks
    // that what a frame drew is what hudLayout gives for that frame.

    /** The HUD size setting (HudSize): Compact, the default, draws the HUD at the kit scale on every window and GUI Scale; Standard
     *  and Large are one and two steps larger, where the whole HUD fits beside vanilla's bars. */
    public static final int HUD_COMPACT = 0, HUD_STANDARD = 1, HUD_LARGE = 2;

    /** The battle HUD's screen pixels per canvas pixel at a HUD size. Compact: the kit scale (pixelScale), the screens' own, 1 on a
     *  1280x720 or 1366x768 window and 2 at 1080p. Standard: the kit scale plus one, and Large plus two, each a step less wherever
     *  the whole HUD would not fit there in every state hudFits lays out, never below the kit scale. Never below 1, so the canvas
     *  (the framebuffer divided by it) never divides by zero; a GUI Scale of 0 or less counts as 1, any other size as Compact. */
    public static int hudScale(int fbW, int fbH, int guiScale, int size) {
        int gui = Math.max(1, guiScale), kit = pixelScale(fbW, fbH, gui);
        for (int k = kit + (size == HUD_LARGE ? 2 : size == HUD_STANDARD ? 1 : 0); k > kit; k--) if (hudFits(fbW, fbH, gui, k, size)) return k;
        return kit;
    }

    /** The sizes by name, as the Visuals and sound screen's Battle HUD button shows them and hud.json keeps them (HudSize). */
    public static final String[] HUD_SIZE_NAMES = {"Compact", "Standard", "Large"};

    /** The size a window shows a HUD size at (UI Task 8c fix round 1): the smallest size hudScale draws at the same scale there, so
     *  the size itself wherever it is a step larger than the one below it. At their Auto GUI Scale, Large shows as Standard at 1080p,
     *  1440p and 4K, and Standard and Large as Compact at 1280x720 and smaller (UiMathCheck pins the windows). Only the scale: shown
     *  at Compact's, Standard and Large keep their five-line feed and the hint layer's 60 s. Any other size counts as Compact. */
    public static int hudShownAs(int fbW, int fbH, int guiScale, int size) {
        int s = size == HUD_STANDARD || size == HUD_LARGE ? size : HUD_COMPACT, k = hudScale(fbW, fbH, guiScale, s);
        for (int smaller = HUD_COMPACT; smaller < s; smaller++) if (hudScale(fbW, fbH, guiScale, smaller) == k) return smaller;
        return s;
    }

    /** The Battle HUD button's words for a size this window shows at shownAs (hudShownAs): "Battle HUD: Large", or where the
     *  window draws it at a smaller size's scale, that too (hudShownHere): "Battle HUD: Large (Standard size here)". */
    public static String hudSizeLabel(int size, int shownAs) {
        String label = "Battle HUD: " + hudSizeName(size);
        return shownAs < size ? label + " (" + hudShownHere(shownAs) + " here)" : label;
    }

    /** The screen's notice once a size is saved: "Battle HUD: Large. Your choice is saved.", and where the window draws it at a
     *  smaller size's scale, " In this window it draws at Standard size." after it (hudShownHere). */
    public static String hudSizeNotice(int size, int shownAs) {
        String notice = "Battle HUD: " + hudSizeName(size) + ". Your choice is saved.";
        return shownAs < size ? notice + " In this window it draws at " + hudShownHere(shownAs) + "." : notice;
    }

    /** What a size shown at a smaller one's scale is, in words true to it (UI Task 8b, the Task 8c review's Minor A): at Standard's
     *  scale Large is Standard in full (the same feed, hints and scale), so "Standard size"; at Compact's scale Standard and Large
     *  keep their five-line feed and 60 s of hints, so only "Compact scale". */
    static String hudShownHere(int shownAs) { return hudSizeName(shownAs) + (shownAs == HUD_COMPACT ? " scale" : " size"); }

    static String hudSizeName(int size) { return HUD_SIZE_NAMES[size >= 0 && size < HUD_SIZE_NAMES.length ? size : HUD_COMPACT]; }

    private static final java.util.regex.Pattern HUD_SIZE_KEY = java.util.regex.Pattern.compile("\"size\"\\s*:\\s*\"([^\"\\\\]*)\"");

    /** The byte order mark a text file may start with (Windows Notepad's "UTF-8" save writes one). */
    static final char BOM = (char) 0xFEFF;

    /** The size a hud.json's text names (UI Task 8c): {"size":"Standard"} as HudSize writes it, white space allowed, the name
     *  matched exactly. Compact for no file (null), text that is not one flat {...} object, no "size" string in it, or a name it
     *  does not know. A leading byte order mark (BOM) is skipped, as gson skipped it (UI Task 8b, the Task 8c review's Minor C).
     *  Pure, so the standalone check covers the fallback (HudSize.reload reads the file). */
    public static int hudSizeOf(String json) {
        if (json == null) return HUD_COMPACT;
        String t = json.strip();
        if (!t.isEmpty() && t.charAt(0) == BOM) t = t.substring(1).strip();
        if (!t.startsWith("{") || t.indexOf('{', 1) >= 0 || t.indexOf('}') != t.length() - 1) return HUD_COMPACT;
        java.util.regex.Matcher m = HUD_SIZE_KEY.matcher(t);
        if (!m.find()) return HUD_COMPACT;
        for (int i = 0; i < HUD_SIZE_NAMES.length; i++) if (HUD_SIZE_NAMES[i].equals(m.group(1))) return i;
        return HUD_COMPACT;
    }

    /** How long a round's start shows the hint layer: the spec's first 60 s at Standard and Large (UI spec 4.5), 15 s at Compact
     *  (UI Task 8c); presentation timings. Holding the hints key brings it back at any size. */
    public static final long HINT_LAYER_MS = 60000, HINT_LAYER_COMPACT_MS = 15000;

    /** The hint layer's time at a round's start at a HUD size. */
    public static long hintLayerMs(int size) { return size == HUD_COMPACT ? HINT_LAYER_COMPACT_MS : HINT_LAYER_MS; }

    // The left column on its iron spine (UI Task 8c, Kort's choice): the selected group keeps its ledger card with its order,
    // formation and key row; every other group hangs as a chip one line high; while the whole army is selected every group's
    // chip is brass and one key strip hangs under the bottom chip. Canvas px.
    /** The column's cards and chips start at x 24, 176 wide; the spine at x 12 and the hooks between, so the column lies in x 12
     *  to 200. */
    public static final int CARD_X = 24, CARD_W = 176, COLUMN_LEFT = 12, COLUMN_RIGHT = 200;
    /** Heights: the selected group's ledger card, a chip, and the army's key strip. */
    public static final int CARD_H = 62, CHIP_H = 18, KEYS_H = 16;
    /** The px between two plates of the column, and between the column and a vanilla bar below it. */
    public static final int COLUMN_GAP = 4;
    /** The column's foot: 18 px above the canvas's bottom, as the mock hangs the cards. */
    public static final int COLUMN_FOOT = 18;
    /** The hint slip, 164 x 40, its box 48 px above the column's top plate: 8 px between, the spine's top in them. */
    public static final int HINTS_W = 164, HINTS_H = 40, HINTS_ABOVE = 48;
    /** A chip's strength bar: 64 px in its 66 px channel. */
    public static final int CHIP_BAR = 64;

    /** Filled pixels of a chip's 64 px strength bar: barWidth's rule (clamped to 0..1, NaN as 0). */
    public static int chipBar(float strength) {
        return Math.round(Math.max(0f, Math.min(1f, strength)) * CHIP_BAR);
    }

    /** Tops of plates of these heights stacked upward, gap px apart, the last ending at `bottom`. */
    public static int[] cardTops(int bottom, int[] heights, int gap) {
        int[] tops = new int[heights.length];
        for (int i = heights.length - 1; i >= 0; i--) {
            tops[i] = bottom - heights[i];
            bottom = tops[i] - gap;
        }
        return tops;
    }

    /** The column's foot in canvas px: COLUMN_FOOT above the canvas's bottom, raised to COLUMN_GAP above any of vanilla's bars that
     *  reaches into the column's x 12 to 200 (the offhand slot at 854x480 GUI 2, the hotbar on a narrow canvas at a large scale),
     *  so no plate of the column sits on one. */
    public static int columnBottom(int fbW, int fbH, int gui, int k) {
        int g = Math.max(1, gui), kk = Math.max(1, k), bottom = fbH / kk - COLUMN_FOOT;
        for (int[] b : vanillaBars(-Math.floorDiv(-fbW, g), -Math.floorDiv(-fbH, g), g, kk, false))
            if (b[0] < COLUMN_RIGHT && b[0] + b[2] > COLUMN_LEFT) bottom = Math.min(bottom, b[1] - COLUMN_GAP);
        return bottom;
    }

    /** Vanilla's bars in canvas px (rounded outward) on a GUI sw x sh at GUI Scale gui and k screen px per canvas px: the hotbar, the
     *  selected slot's frame on any slot, the offhand slot (left of the hotbar while the offhand holds something), the hearts, the
     *  armour row, the experience bar and, with `crosshair`, the crosshair. */
    public static List<int[]> vanillaBars(int sw, int sh, int gui, int k, boolean crosshair) {
        List<int[]> out = new ArrayList<>();
        for (int[] b : new int[][]{{sw / 2 - 91, sh - 22, 182, 22}, {sw / 2 - 92, sh - 23, 184, 22}, {sw / 2 - 120, sh - 23, 29, 24}, {sw / 2 - 91, sh - 39, 81, 9},
            {sw / 2 - 91, sh - 49, 81, 9}, {sw / 2 - 91, sh - 29, 182, 5}}) out.add(guiToCanvas(b, gui, k));
        if (crosshair) out.add(guiToCanvas(new int[]{(sw - CROSSHAIR) / 2, (sh - CROSSHAIR) / 2, CROSSHAIR, CROSSHAIR}, gui, k));
        return out;
    }

    /** The lesson note (the tutorial's lesson, UI Task 8c): one short block at x 12, y 72, below the seals (which end at y 68) and
     *  off the centre band, 280 px wide; its copy in 11 px Lato, 264 px a line, the first line after its LESSON k/4 tag and 6 px. */
    public static final int LESSON_X = 12, LESSON_Y = 72, LESSON_W = 280, LESSON_TEXT_W = 264, LESSON_TAG_GAP = 6;

    /** The lesson note's height for `lines` lines of copy (at least one): 11 px a line and 11 more, 5 above the first line and 6
     *  under the last. */
    public static int lessonHeight(int lines) { return 11 * Math.max(1, lines) + 11; }

    /** The lesson copy in lines no wider than `width`, the first `indent` px narrower (the tag before it), each broken at the last
     *  space that fits. A word too long for a line stands alone on its line: nothing is cut, and the lines joined with spaces are
     *  the copy (its runs of spaces as one). */
    public static List<String> lessonLines(String copy, int indent, int width, ToIntFunction<String> w) {
        List<String> out = new ArrayList<>();
        String line = "";
        for (String word : copy.strip().split(" +")) {
            if (word.isEmpty()) continue;
            String next = line.isEmpty() ? word : line + " " + word;
            if (line.isEmpty() || w.applyAsInt(next) <= (out.isEmpty() ? width - indent : width)) line = next;
            else { out.add(line); line = word; }
        }
        if (!line.isEmpty()) out.add(line);
        return out;
    }

    /** The feed leaf's box on a w x h canvas at a HUD size: Compact's leaf of three entries, 166 x 44, 16 px in from the right edge
     *  and centred on the canvas's middle; Standard's and Large's leaf of the spec's five, 172 x 74, as UI Task 3 hung it. */
    public static int[] feedBox(int w, int h, int size) {
        return size == HUD_COMPACT ? new int[]{w - 182, h / 2 - 22, 166, 44} : new int[]{w - 188, h / 2 - 38, 172, 74};
    }

    /** How far a feed line's text runs from x 20 of its leaf: 144 px on Compact's leaf, 148 on the five-entry leaf, 2 and 4 px short of
     *  the binding on either. */
    public static int feedTextW(int size) { return size == HUD_COMPACT ? 144 : 148; }

    /** A feed row's y in its leaf, row 0 at the top: Compact's leaf rules its entries from y 7, the five-entry leaf from y 9, 12 px
     *  apart on both. */
    public static int feedRowY(int size, int row) { return (size == HUD_COMPACT ? 7 : 9) + 12 * row; }

    /** Vanilla's crosshair top in canvas px: InGameHud draws the 15 px icon at GUI y (scaledH - 15) / 2, and a GUI px is gui screen
     *  px, a canvas px k. */
    public static int crosshairTop(int scaledH, int gui, int k) { return Math.floorDiv((scaledH - 15) / 2 * gui, Math.max(1, k)); }

    /** The respawn card's top: 40 px above the canvas's middle as in the mock, raised where that would leave less than 4 px between
     *  the 28 px card and vanilla's crosshair below it (crossTop, canvas px), as when the GUI Scale is above the canvas scale. */
    public static int respawnY(int canvasH, int crossTop) { return Math.min(canvasH / 2 - 40, crossTop - 4 - 28); }

    /** Rows two shadowed plates keep between their inks where their columns meet (inksApart); nearer, the lesson note gives way to
     *  the respawn card (lessonYields), and hudFits fails a layout. */
    public static final int CARD_GAP = 4;

    /** Whether two plates keep their inks CARD_GAP rows apart where their columns meet: the upper one's shadow (inkBelow) and the
     *  lower one's top edge (inkAbove). Plates whose columns do not meet always do. */
    public static boolean inksApart(String ka, int[] a, String kb, int[] b) {
        if (a[0] >= b[0] + b[2] || b[0] >= a[0] + a[2]) return true;
        if (a[1] > b[1]) return inksApart(kb, b, ka, a);
        return b[1] - inkAbove(kb) - (a[1] + a[3] + inkBelow(ka)) >= CARD_GAP;
    }

    /** Whether the lesson note gives way to the respawn card while the commander is down: where its inks would come within CARD_GAP
     *  rows of the card's (inksApart), as on a canvas too short for both. The countdown is what the player needs then; the lesson
     *  is back when the commander is. */
    public static boolean lessonYields(int[] lesson, int[] respawn) { return !inksApart("lesson", lesson, "respawn", respawn); }

    /** One frame of the battle HUD as hudLayout lays it out: the points (a seal each), the groups the round fielded in their order
     *  (infantry, ranged, support), the selection ("all" for the whole army, else a group's id; anything else selects none), the
     *  hint layer up (the hint slip), the lesson's lines (0 for none: no tutorial, or an order being aimed), the commander down (the
     *  respawn card) and a feed line alive (the feed leaf). */
    public record HudState(int points, List<String> groups, String selected, boolean hints, int lessonLines, boolean respawn, boolean feed) {}

    /** The default battle state the footprint budget is measured in (UI Task 8c): three points, the three groups fielded, the whole
     *  army selected (as a round starts), the hint layer down, a feed line alive, no lesson, the commander up. */
    public static final HudState DEFAULT_STATE = new HudState(3, List.of("infantry", "ranged", "support"), "all", false, 0, false, true);

    /** Every plate of one frame of the battle HUD, {x, y, w, h} in canvas px, on a fbW x fbH window at GUI Scale gui with k screen
     *  px per canvas px and at a HUD size, keyed as BattleHud.rects keys them: score, dawn-boss, dusk-boss and seal-(i); card-(group)
     *  for each group fielded, the selected group's card CARD_H tall and every other group's chip CHIP_H, stacked up from the
     *  column's foot (columnBottom) COLUMN_GAP apart; keys, the army's key strip under the bottom chip while the whole army is
     *  selected; spine, from 4 px above the top plate to 4 px below the foot; hints, the hint slip in the hint layer; lesson, unless
     *  it gives way to the respawn card (lessonYields); respawn; feed (feedBox). Not the aimed point's name, whose width is its
     *  text's, nor the ability slot, which sits in vanilla's GUI px. BattleHud draws by it; hudFits tries it in every state. */
    public static java.util.Map<String, int[]> hudLayout(int fbW, int fbH, int gui, int k, int size, HudState s) {
        int g = Math.max(1, gui), kk = Math.max(1, k), w = fbW / kk, h = fbH / kk, cx = w / 2, sh = -Math.floorDiv(-fbH, g);
        var r = new java.util.LinkedHashMap<String, int[]>();
        r.put("score", new int[]{cx - 128, 13, 256, 24});
        r.put("dawn-boss", new int[]{cx - 140, 12, 27, 28});
        r.put("dusk-boss", new int[]{cx + 115, 12, 27, 28});
        for (int i = 0; i < s.points(); i++) r.put("seal-" + i, new int[]{sealCentre(i, s.points(), w) - 13, 37, 27, 31});
        List<String> gs = s.groups();
        int n = gs.size(), bottom = columnBottom(fbW, fbH, g, kk);
        boolean army = "all".equals(s.selected()) && n > 0;
        int[] heights = new int[n];
        for (int i = 0; i < n; i++) heights[i] = !army && gs.get(i).equals(s.selected()) ? CARD_H : CHIP_H;
        int[] tops = cardTops(army ? bottom - KEYS_H - COLUMN_GAP : bottom, heights, COLUMN_GAP);
        int first = n > 0 ? tops[0] : bottom;
        if (n > 0) r.put("spine", new int[]{COLUMN_LEFT, first - 4, 9, bottom - first + 8});
        for (int i = 0; i < n; i++) r.put("card-" + gs.get(i), new int[]{CARD_X, tops[i], CARD_W, heights[i]});
        if (army) r.put("keys", new int[]{CARD_X, bottom - KEYS_H, CARD_W, KEYS_H});
        if (s.hints()) r.put("hints", new int[]{CARD_X, first - HINTS_ABOVE, HINTS_W, HINTS_H});
        int[] respawn = s.respawn() ? new int[]{cx - 100, respawnY(h, crosshairTop(sh, g, kk)), 200, 28} : null;
        if (s.lessonLines() > 0) {
            int[] lesson = {LESSON_X, LESSON_Y, LESSON_W, lessonHeight(s.lessonLines())};
            if (respawn == null || !lessonYields(lesson, respawn)) r.put("lesson", lesson);
        }
        if (respawn != null) r.put("respawn", respawn);
        if (s.feed()) r.put("feed", feedBox(w, h, size));
        return r;
    }

    /** GUI px the lifted chat keeps clear of the battle HUD and of vanilla's crosshair, below it and above it (ChatPlacement). */
    public static final int CHAT_GAP = 4;
    /** Vanilla's crosshair icon, a square this many GUI px wide at the screen's centre. The icon only: vanilla's attack indicator
     *  below it (the screen's middle + 9) may meet the closed chat's top line for a moment after a swing, accepted by controller
     *  ruling (UI Task 8a), as ChatPlacement says. */
    public static final int CROSSHAIR = 15;
    /** Where the line backgrounds of vanilla's widest chat end (Chat Width 100%, 320 px, at Chat Text Size 100%: GUI x 332, as
     *  ChatPlacement.right gives it), the chat hudFits lays out. */
    public static final double CHAT_RIGHT = 332;

    /** Whether the lifted chat stays clear of the HUD element with this rect key (BattleHud.rects): the score bar, the bosses, the
     *  seals, the aimed point's name, the lesson note, the respawn card, the feed and the ability slot beside the hotbar (UI Task
     *  8b-2: on a GUI under 456 px wide, as 1280x720 GUI 3, it reaches under vanilla's widest chat, and the open chat at vanilla's
     *  bottom covered it). The group column is below the chat by its place. */
    public static boolean overChat(String key) {
        return key.equals("score") || key.endsWith("-boss") || key.startsWith("seal-") || key.equals("aim-name") || key.equals("lesson") || key.equals("respawn") || key.equals("feed") || key.equals("ability");
    }

    /** The camp's two slips on the HUD canvas of a fbW x fbH window at k screen px per canvas px (UI Task 8b-2), each a notice
     *  slip's box (164x40; its PNG's shadow reaches 5 px right and 6 below): "camp-head", the camp's name, gold and title at the
     *  top left; "camp-hint", the war table's and the stations' keys at the bottom right, its shadow 12 px inside the canvas, off
     *  vanilla's bars and the closed chat (both end left of it). Where the slip would reach over the items a player can switch on
     *  right of the hotbar (a left-handed player's offhand slot, GUI x sw/2 + 91 to + 120 from sh - 23, and the Hotbar-mode attack
     *  indicator inside that span), as at 1080p GUI 4 with Standard or Large, it stands 4 px above them instead. gui is the GUI
     *  Scale. */
    public static java.util.Map<String, int[]> campHud(int fbW, int fbH, int gui, int k) {
        int w = fbW / k, h = fbH / k, sw = -Math.floorDiv(-fbW, gui), sh = -Math.floorDiv(-fbH, gui);
        var r = new java.util.LinkedHashMap<String, int[]>();
        r.put("camp-head", new int[]{12, 12, 164, 40});
        int x = w - 12 - 169, y = h - 12 - 46;
        int[] right = guiToCanvas(new int[]{sw / 2 + 91, sh - 23, 29, 24}, gui, k);
        if (meets(new int[]{x, y, 169, 46}, right)) y = Math.min(y, right[1] - 4 - 46);
        r.put("camp-hint", new int[]{x, y, 164, 40});
        return r;
    }

    /** The ability slot's box in canvas px on a GUI sw x sh at gui GUI Scale, k screen px per canvas px: BattleHud draws it in
     *  vanilla's GUI px, 50x32 at (sw/2 + 104, sh - 47), beside the hotbar's right end. */
    public static int[] abilityRect(int sw, int sh, int gui, int k) {
        return guiToCanvas(new int[]{sw / 2 + 104, sh - 47, 50, 32}, gui, k);
    }

    /** The rows a HUD plate's PNG paints above its box, by rect key (atlas.json oy): 1 for the score card, the lesson note, the
     *  respawn card and the key strip, 2 for a card or chip (its loss flash's rule; 1 otherwise), 3 for the feed leaf; the seals,
     *  the bosses, the hint slip and the aimed point's name paint inside or below their boxes. */
    static int inkAbove(String key) {
        if (key.startsWith("card-")) return 2;
        return switch (key) { case "feed" -> 3; case "score", "lesson", "respawn", "keys" -> 1; default -> 0; };
    }

    /** The rows of drop shadow a HUD plate's PNG paints below its box (atlas.json: the PNG's height less oy and the box's): 5 for the
     *  score card, the lesson note, the respawn card and the key strip, 6 for a card or chip (in its loss flash; 5 otherwise) and
     *  for the hint slip, 7 for the feed leaf, none for the rest. */
    static int inkBelow(String key) {
        if (key.startsWith("card-")) return 6;
        return switch (key) { case "feed" -> 7; case "hints" -> 6; case "score", "lesson", "respawn", "keys" -> 5; default -> 0; };
    }

    /** The pairs whose inks keep CARD_GAP rows apart (inksApart): the lesson note, the respawn card or the feed leaf with one
     *  another, or with the hint slip or a plate of the column. The column's own plates hang closer by design. */
    static boolean shadowed(String a, String b) {
        boolean ca = a.equals("lesson") || a.equals("respawn") || a.equals("feed"), cb = b.equals("lesson") || b.equals("respawn") || b.equals("feed");
        return ca && (cb || column(b)) || cb && column(a);
    }

    static boolean column(String key) { return key.equals("hints") || key.equals("keys") || key.startsWith("card-"); }

    /** columnTop's answer for a frame that drew no column (camp, an entrance, a screen over the HUD). */
    public static final int NO_COLUMN = Integer.MIN_VALUE;

    /** The top of the battle HUD's left column (the spine above the top plate, or the hint slip) in canvas px, which a window too
     *  short for the column puts above the canvas; NO_COLUMN when a frame drew none. */
    public static int columnTop(java.util.Map<String, int[]> r) {
        int top = NO_COLUMN;
        for (var e : r.entrySet()) {
            String k = e.getKey();
            if (k.equals("spine") || k.equals("hints") || k.equals("keys") || k.startsWith("card-")) top = top == NO_COLUMN ? e.getValue()[1] : Math.min(top, e.getValue()[1]);
        }
        return top;
    }

    /** The lifted chat: its bottom in GUI px, the lines it shows, and whether its lowest line is on the screen. */
    public record ChatSlot(int bottom, int lines, boolean onScreen) {}

    /** The chat over one frame of the battle HUD (rects in canvas px, f GUI px per canvas px) on a GUI sw x sh: a chat whose line
     *  backgrounds end at GUI x `right`, lineH GUI px a line, vanilla's count `vanilla`. Each element overChat names that the chat's
     *  width reaches is a span {top, bottom, box bottom} in GUI px, its PNG's edge and shadow included (inkAbove, inkBelow), and
     *  so, while the chat is closed (focused false), are vanilla's crosshair and those of vanilla's bars its width reaches (the
     *  hotbar, the selected slot, the offhand slot, the hearts, the armour row and the experience bar, UI Task 8c). Null without a
     *  column: the chat keeps vanilla's place. */
    public static ChatSlot chatSlot(java.util.Map<String, int[]> rects, double f, int sw, int sh, double right, double lineH, int vanilla, boolean focused) {
        int top = columnTop(rects);
        if (top == NO_COLUMN) return null;
        List<double[]> spans = new ArrayList<>();
        for (var e : rects.entrySet()) {
            String k = e.getKey();
            int[] a = e.getValue();
            if (overChat(k) && a[0] * f < right) spans.add(new double[]{(a[1] - inkAbove(k)) * f, (a[1] + a[3] + inkBelow(k)) * f, (a[1] + a[3]) * f});
        }
        if (!focused && right > (sw - CROSSHAIR) / 2) {
            int y = (sh - CROSSHAIR) / 2;
            spans.add(new double[]{y, y + CROSSHAIR, y + CROSSHAIR});
        }
        // Closed, its lowest line keeps off vanilla's bars under its width too (UI Task 8c): where the compact column leaves the
        // chat at vanilla's own place, as at 1280x720 GUI 3, that line would sit on the armour row and the hearts.
        if (!focused) for (int[] b : vanillaBars(sw, sh, 1, 1, false)) if (b[0] < right) spans.add(new double[]{b[1], b[1] + b[3], b[1] + b[3]});
        return chatSlot(vanilla, Math.min((int) Math.floor(top * f) - CHAT_GAP, sh - 40), spans, lineH);
    }

    /** Where the lifted chat sits and what it shows (Kort's answer: above the group cards, small but readable). Its bottom starts at
     *  `bottom` (CHAT_GAP above the column's top) and moves up past each span that would come under its lowest line or within
     *  CHAT_GAP below it, so that line never covers anything the HUD paints. It shows as many lineH lines as fit between its bottom
     *  and the lowest box above it, CHAT_GAP clear, from 1 to vanilla's count; a drop shadow above may reach the one row of
     *  background over a line's text. Should that push the lowest line off the screen's top (a window too small for the HUD at
     *  its scale, which hudScale never picks above the kit scale), the chat keeps its first place, or the screen's top line where
     *  even that is off the screen, and one line: onScreen false. */
    public static ChatSlot chatSlot(int vanilla, int bottom, List<double[]> spans, double lineH) {
        int start = bottom;
        for (boolean moved = true; moved; ) {
            moved = false;
            for (double[] s : spans) if (s[0] < bottom + CHAT_GAP && s[1] > bottom - lineH) { bottom = (int) Math.floor(s[0]) - CHAT_GAP; moved = true; }
        }
        if (bottom - lineH < 0) return new ChatSlot(Math.max(start, (int) Math.ceil(lineH)), 1, false);
        double above = 0;
        for (double[] s : spans) if (s[0] < bottom + CHAT_GAP) above = Math.max(above, Math.ceil(s[2]));
        int fit = (int) Math.floor((bottom - above - CHAT_GAP) / lineH);
        return new ChatSlot(bottom, Math.max(1, Math.min(vanilla, fit)), true);
    }

    /** Two boxes {x, y, w, h} share a pixel. */
    public static boolean meets(int[] a, int[] b) { return a[0] < b[0] + b[2] && b[0] < a[0] + a[2] && a[1] < b[1] + b[3] && b[1] < a[1] + a[3]; }

    /** A GUI box in canvas px at gui GUI Scale and k screen px per canvas px, rounded outward. */
    static int[] guiToCanvas(int[] a, int gui, int k) {
        int x0 = Math.floorDiv(a[0] * gui, k), y0 = Math.floorDiv(a[1] * gui, k);
        return new int[]{x0, y0, -Math.floorDiv(-(a[0] + a[2]) * gui, k) - x0, -Math.floorDiv(-(a[1] + a[3]) * gui, k) - y0};
    }

    /** How far the aimed point's name may reach past the first and last seals' centres, hudFits's room for the longest name. */
    static final int AIM_REACH = 80;

    /** What one frame of the HUD (rects in canvas px on a w x h canvas) breaks, empty when nothing: a plate (the ability slot apart,
     *  which sits in vanilla's GUI px) less than 12 px inside the canvas; two plates that touch, but a card on the spine and a boss
     *  on the score card; a plate on one of vanilla's bars (by name); and the lesson note, the respawn card or the feed leaf with
     *  its inks within CARD_GAP rows of another plate's (shadowed, inksApart). hudFits asks it of every state it lays out, and the
     *  ui-hud suite of every frame it measures. */
    public static List<String> frameFaults(java.util.Map<String, int[]> r, int w, int h, java.util.Map<String, int[]> bars) {
        List<String> bad = new ArrayList<>();
        List<String> keys = new ArrayList<>(r.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String ka = keys.get(i);
            int[] a = r.get(ka);
            if (!ka.equals("ability") && (a[0] < 12 || a[1] < 12 || a[0] + a[2] > w - 12 || a[1] + a[3] > h - 12)) bad.add(ka + " " + java.util.Arrays.toString(a) + " is not 12 px inside the " + w + "x" + h + " canvas");
            for (var b : bars.entrySet()) if (meets(a, b.getValue())) bad.add(ka + " " + java.util.Arrays.toString(a) + " meets " + b.getKey() + " " + java.util.Arrays.toString(b.getValue()));
            for (int j = i + 1; j < keys.size(); j++) {
                String kb = keys.get(j);
                int[] b = r.get(kb);
                if (!mayTouch(ka, kb) && meets(a, b)) bad.add(ka + " " + java.util.Arrays.toString(a) + " meets " + kb + " " + java.util.Arrays.toString(b));
                else if (shadowed(ka, kb) && !inksApart(ka, a, kb, b)) bad.add(ka + " " + java.util.Arrays.toString(a) + " and " + kb + " " + java.util.Arrays.toString(b) + " come within " + CARD_GAP + " rows of each other's ink");
            }
        }
        return bad;
    }

    /** vanillaBars by name, for frameFaults. */
    static java.util.Map<String, int[]> namedBars(int sw, int sh, int gui, int k) {
        var bars = vanillaBars(sw, sh, gui, k, true);
        String[] names = {"the hotbar", "the selected slot", "the offhand slot", "the hearts", "the armour row", "the experience bar", "the crosshair"};
        var out = new java.util.LinkedHashMap<String, int[]>();
        for (int i = 0; i < names.length; i++) out.put(names[i], bars.get(i));
        return out;
    }

    /** Whether the battle HUD fits a fbW x fbH window at GUI Scale gui with k screen px per canvas px at a HUD size, in its fullest
     *  states: three or five points; the whole army or one group selected; no lesson, the lesson (two lines), or the aimed point's
     *  name in its place (aiming raises the hint layer); the hint layer down or up; the commander up or down (the lesson giving way
     *  where lessonYields says, as BattleHud draws them); a feed line and the ability slot always. In each, frameFaults finds
     *  nothing, and the lifted chat (vanilla's widest, closed and open) keeps its lowest line on the screen. */
    public static boolean hudFits(int fbW, int fbH, int gui, int k, int size) {
        if (gui < 1 || k < 1) return false;
        int w = fbW / k, h = fbH / k, sw = -Math.floorDiv(-fbW, gui), sh = -Math.floorDiv(-fbH, gui);
        if (w < 1 || h < 1) return false;
        double f = k / (double) gui;
        var bars = namedBars(sw, sh, gui, k);
        int[] ability = abilityRect(sw, sh, gui, k);
        List<String> groups = DEFAULT_STATE.groups();
        for (int n = 3; n <= 5; n += 2) for (String sel : new String[]{"all", "infantry"}) for (int content = 0; content < 3; content++)
            for (int hint = content == 2 ? 1 : 0; hint < 2; hint++) for (int down = 0; down < 2; down++) {
                var r = hudLayout(fbW, fbH, gui, k, size, new HudState(n, groups, sel, hint == 1, content == 1 ? 2 : 0, down == 1, true));
                if (content == 2) r.put("aim-name", new int[]{sealCentre(0, n, w) - AIM_REACH, 68, sealCentre(n - 1, n, w) - sealCentre(0, n, w) + 2 * AIM_REACH, 9});
                r.put("ability", ability);
                if (!frameFaults(r, w, h, bars).isEmpty()) return false;
                for (int open = 0; open < 2; open++) if (!chatSlot(r, f, sw, sh, CHAT_RIGHT, 9, open == 1 ? 20 : 10, open == 1).onScreen()) return false;
            }
        return true;
    }

    /** A card may hang on the spine and a boss may sit on the score card; nothing else of the HUD may touch. */
    static boolean mayTouch(String a, String b) {
        if (a.startsWith("card-") && b.equals("spine") || b.startsWith("card-") && a.equals("spine")) return true;
        return a.endsWith("-boss") && b.equals("score") || b.endsWith("-boss") && a.equals("score");
    }

    /** The plates the footprint counts (UI Task 8c): the group cards and chips, the army's key strip, the feed leaf, the hint slip,
     *  the lesson note and the respawn card. */
    public static boolean plate(String key) {
        return key.startsWith("card-") || key.equals("keys") || key.equals("feed") || key.equals("hints") || key.equals("lesson") || key.equals("respawn");
    }

    /** The share of a fbW x fbH screen the HUD's plates cover (plate), from their boxes in canvas px at k screen px each; plates never
     *  touch one another (frameFaults), so their areas add. 0 for an empty screen. */
    public static double footprint(java.util.Map<String, int[]> rects, int k, int fbW, int fbH) {
        long a = 0;
        for (var e : rects.entrySet()) if (plate(e.getKey())) a += (long) e.getValue()[2] * e.getValue()[3];
        return fbW <= 0 || fbH <= 0 ? 0 : a * (double) k * k / ((double) fbW * fbH);
    }

    /** The centre band, {x, y, w, h} on a w x h canvas: the middle 40% of its width, from 30% to 62% of its height. */
    public static int[] centreBand(int w, int h) {
        int x0 = (int) Math.floor(w * 0.3), x1 = (int) Math.ceil(w * 0.7), y0 = (int) Math.floor(h * 0.3), y1 = (int) Math.ceil(h * 0.62);
        return new int[]{x0, y0, x1 - x0, y1 - y0};
    }

    /** The plates in the centre band, empty when it is clear; the respawn card is let in, as the commander is down while it shows. */
    public static List<String> inBand(java.util.Map<String, int[]> rects, int w, int h) {
        int[] band = centreBand(w, h);
        List<String> in = new ArrayList<>();
        for (var e : rects.entrySet()) if (plate(e.getKey()) && !e.getKey().equals("respawn") && meets(e.getValue(), band)) in.add(e.getKey());
        return in;
    }
}
