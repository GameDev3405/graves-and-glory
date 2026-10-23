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

    /** Tops of the group cards stacked upward: the bottom card ends 18 px above the canvas bottom, 8 px between cards. */
    public static int[] cardTops(int canvasH, int[] heights) {
        int[] tops = new int[heights.length];
        int bottom = canvasH - 18;
        for (int i = heights.length - 1; i >= 0; i--) {
            tops[i] = bottom - heights[i];
            bottom = tops[i] - 8;
        }
        return tops;
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

    /** How long an event feed line lives, and the last stretch of that over which it fades out (presentation timings). */
    public static final long FEED_LIFE_MS = 8000, FEED_FADE_MS = 2000;

    /** An event feed line's alpha at ageMs: 1 until its last FEED_FADE_MS, then down in a straight line to 0 at FEED_LIFE_MS. */
    public static float feedAlpha(long ageMs) {
        long left = FEED_LIFE_MS - ageMs;
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

    // The battle HUD's scale and layout (UI Task 8a). BattleHud and ChatPlacement draw by these; hudFits lays the HUD out with them.

    /** Windows this wide or wider keep the battle HUD at the kit scale, pixelScale (controller ruling 2026-10-18: 1366x768 and
     *  1280x720 at GUI Scale 2 get a readable ledger, and the 1080p layout does not grow). */
    public static final int HUD_KIT_WIDTH = 1600;

    /** The battle HUD's screen pixels per canvas pixel. From HUD_KIT_WIDTH up, the kit scale (pixelScale). Narrower, the player's GUI
     *  Scale up to 2 wherever the whole HUD fits at that size beside vanilla's bars (hudFits), else the kit scale, which is 1 there.
     *  Never below 1, so the canvas (the framebuffer divided by it) never divides by zero; a GUI Scale of 0 or less counts as 1. */
    public static int hudScale(int fbW, int fbH, int guiScale) {
        int gui = Math.max(1, guiScale), kit = pixelScale(fbW, fbH, gui);
        if (fbW >= HUD_KIT_WIDTH) return kit;
        for (int k = Math.min(gui, 2); k > kit; k--) if (hudFits(fbW, fbH, gui, k)) return k;
        return kit;
    }

    /** The lesson card's top: the aimed point's name (y 68, 9 tall) and 11 px clear above it. */
    public static final int LESSON_Y = 88;
    /** How far left the lesson card may sit: 16 px right of the group column, whose cards and hint slip end at x 200. */
    public static final int LESSON_MIN_X = 216;

    /** The lesson card's height for `lines` lines of copy: 40, and 11 more for each line past the first. */
    public static int lessonHeight(int lines) { return 40 + 11 * Math.max(0, lines - 1); }

    /** The lesson card's x: centred on the canvas centre cx as the mock lays it out, unless that would reach the group column (a
     *  canvas under 832 px wide, as 1280x720 and 1366x768 are at GUI Scale 2); then LESSON_MIN_X. */
    public static int lessonX(int cx) { return Math.max(cx - 200, LESSON_MIN_X); }

    /** Vanilla's crosshair top in canvas px: InGameHud draws the 15 px icon at GUI y (scaledH - 15) / 2, and a GUI px is gui screen
     *  px, a canvas px k. */
    public static int crosshairTop(int scaledH, int gui, int k) { return Math.floorDiv((scaledH - 15) / 2 * gui, Math.max(1, k)); }

    /** The respawn card's top: 40 px above the canvas's middle as in the mock, raised where that would leave less than 4 px between
     *  the 28 px card and vanilla's crosshair below it (crossTop, canvas px), as when the GUI Scale is above the canvas scale. */
    public static int respawnY(int canvasH, int crossTop) { return Math.min(canvasH / 2 - 40, crossTop - 4 - 28); }

    /** Rows the respawn card's top edge keeps clear of the lesson card's shadow; nearer, the lesson card gives way (lessonYields). */
    public static final int CARD_GAP = 4;

    /** Whether the lesson card (`lines` lines of copy) gives way to the respawn card at y respawnY while the commander is down: where
     *  the respawn card's top edge would come within CARD_GAP of the lesson card's shadow, as at 1280x720 GUI 2, where the HUD's 2x
     *  would hang the respawn card in a two-line lesson's shadow (y 140 against 144). The countdown is what the player needs then;
     *  the lesson is back when the commander is. */
    public static boolean lessonYields(int lines, int respawnY) {
        return respawnY - inkAbove("respawn") - (LESSON_Y + lessonHeight(lines) + inkBelow("lesson")) < CARD_GAP;
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
     *  seals, the aimed point's name, the lesson and respawn cards and the feed. The group column is below the chat by its place,
     *  and the ability slot sits beside the hotbar. */
    public static boolean overChat(String key) {
        return key.equals("score") || key.endsWith("-boss") || key.startsWith("seal-") || key.equals("aim-name") || key.equals("lesson") || key.equals("respawn") || key.equals("feed");
    }

    /** The rows a HUD element's PNG paints above its box, by rect key (atlas.json oy): 1 for the score card and the lesson and respawn
     *  cards, 3 for the feed slip; the seals, the bosses and the aimed point's name paint inside their boxes. */
    static int inkAbove(String key) { return key.equals("feed") ? 3 : key.equals("score") || key.equals("lesson") || key.equals("respawn") ? 1 : 0; }

    /** The rows of drop shadow a HUD element's PNG paints below its box (atlas.json: the PNG's height less oy and the box's): 5 for
     *  the score card and the lesson and respawn cards, 7 for the feed slip, none for the rest. */
    static int inkBelow(String key) { return key.equals("feed") ? 7 : key.equals("score") || key.equals("lesson") || key.equals("respawn") ? 5 : 0; }

    /** columnTop's answer for a frame that drew no column (camp, an entrance, a screen over the HUD). */
    public static final int NO_COLUMN = Integer.MIN_VALUE;

    /** The top of the battle HUD's left column (the spine above the top card, or the hint slip) in canvas px, which a window too
     *  short for the column puts above the canvas; NO_COLUMN when a frame drew none. */
    public static int columnTop(java.util.Map<String, int[]> r) {
        int top = NO_COLUMN;
        for (var e : r.entrySet()) {
            String k = e.getKey();
            if (k.equals("spine") || k.equals("hints") || k.startsWith("card-")) top = top == NO_COLUMN ? e.getValue()[1] : Math.min(top, e.getValue()[1]);
        }
        return top;
    }

    /** The lifted chat: its bottom in GUI px, the lines it shows, and whether its lowest line is on the screen. */
    public record ChatSlot(int bottom, int lines, boolean onScreen) {}

    /** The chat over one frame of the battle HUD (rects in canvas px, f GUI px per canvas px) on a GUI sw x sh: a chat whose line
     *  backgrounds end at GUI x `right`, lineH GUI px a line, vanilla's count `vanilla`. Each element overChat names that the chat's
     *  width reaches is a span {top, bottom, box bottom} in GUI px, its PNG's edge and shadow included (inkAbove, inkBelow), and
     *  so is vanilla's crosshair while the chat is closed (focused false). Null without a column: the chat keeps vanilla's place. */
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

    /** Whether the battle HUD fits a fbW x fbH window at GUI Scale gui with k screen px per canvas px, in its fullest states. In each,
     *  every element sits 12 px or more inside the canvas, none on another but a card on the spine and a boss on the score card,
     *  none on vanilla's hotbar, selected slot, offhand slot, hearts, armour, experience bar or crosshair, and the lifted chat
     *  (vanilla's widest, closed and open) keeps its lowest line on the screen. The states: three or five points; no lesson, the
     *  lesson (two lines), or an aimed point's name in its place (aiming raises the hint layer); the hint layer down (the army
     *  selected) or up (every card tall, the hint slip); the commander up or down (the respawn card, the lesson giving way to it
     *  where lessonYields says, as BattleHud draws them); the feed and the ability slot always. */
    public static boolean hudFits(int fbW, int fbH, int gui, int k) {
        if (gui < 1 || k < 1) return false;
        int w = fbW / k, h = fbH / k, cx = w / 2, sw = -Math.floorDiv(-fbW, gui), sh = -Math.floorDiv(-fbH, gui);
        if (w < 1 || h < 1) return false;
        double f = k / (double) gui;
        List<int[]> bars = new ArrayList<>();
        for (int[] b : new int[][]{{sw / 2 - 91, sh - 22, 182, 22}, {sw / 2 - 92, sh - 23, 24, 22}, {sw / 2 - 120, sh - 23, 29, 24}, {sw / 2 - 91, sh - 39, 81, 9},
            {sw / 2 - 91, sh - 49, 81, 9}, {sw / 2 - 91, sh - 29, 182, 5}, {(sw - CROSSHAIR) / 2, (sh - CROSSHAIR) / 2, CROSSHAIR, CROSSHAIR}}) bars.add(guiToCanvas(b, gui, k));
        int[] ability = guiToCanvas(new int[]{sw / 2 + 104, sh - 47, 50, 32}, gui, k);
        int ry = respawnY(h, crosshairTop(sh, gui, k));
        String[] groups = {"infantry", "ranged", "support"};
        for (int n = 3; n <= 5; n += 2) for (int content = 0; content < 3; content++) for (int hint = content == 2 ? 1 : 0; hint < 2; hint++) for (int down = 0; down < 2; down++) {
            java.util.Map<String, int[]> r = new java.util.LinkedHashMap<>();
            r.put("score", new int[]{cx - 128, 13, 256, 24});
            r.put("dawn-boss", new int[]{cx - 140, 12, 27, 28});
            r.put("dusk-boss", new int[]{cx + 115, 12, 27, 28});
            for (int i = 0; i < n; i++) r.put("seal-" + i, new int[]{sealCentre(i, n, w) - 13, 37, 27, 31});
            if (content == 1 && !(down == 1 && lessonYields(2, ry))) r.put("lesson", new int[]{lessonX(cx), LESSON_Y, 400, lessonHeight(2)});
            if (content == 2) r.put("aim-name", new int[]{sealCentre(0, n, w) - AIM_REACH, 68, sealCentre(n - 1, n, w) - sealCentre(0, n, w) + 2 * AIM_REACH, 9});
            if (down == 1) r.put("respawn", new int[]{cx - 100, ry, 200, 28});
            r.put("feed", new int[]{w - 188, h / 2 - 38, 172, 74});
            int[] heights = hint == 1 ? new int[]{62, 62, 62} : new int[]{48, 48, 62}, tops = cardTops(h, heights);
            for (int i = 0; i < 3; i++) r.put("card-" + groups[i], new int[]{24, tops[i], 176, heights[i]});
            r.put("spine", new int[]{12, tops[0] - 4, 9, tops[2] + heights[2] - tops[0] + 8});
            if (hint == 1) r.put("hints", new int[]{24, tops[0] - 48, 176, 40});
            for (int[] a : r.values()) if (a[0] < 12 || a[1] < 12 || a[0] + a[2] > w - 12 || a[1] + a[3] > h - 12) return false;
            r.put("ability", ability);
            List<String> keys = new ArrayList<>(r.keySet());
            for (int i = 0; i < keys.size(); i++) {
                int[] a = r.get(keys.get(i));
                for (int[] bar : bars) if (meets(a, bar)) return false;
                for (int j = i + 1; j < keys.size(); j++) if (!mayTouch(keys.get(i), keys.get(j)) && meets(a, r.get(keys.get(j)))) return false;
            }
            for (int open = 0; open < 2; open++) if (!chatSlot(r, f, sw, sh, CHAT_RIGHT, 9, open == 1 ? 20 : 10, open == 1).onScreen()) return false;
        }
        return true;
    }

    /** A card may hang on the spine and a boss may sit on the score card; nothing else of the HUD may touch. */
    static boolean mayTouch(String a, String b) {
        if (a.startsWith("card-") && b.equals("spine") || b.startsWith("card-") && a.equals("spine")) return true;
        return a.endsWith("-boss") && b.equals("score") || b.endsWith("-boss") && a.equals("score");
    }
}
