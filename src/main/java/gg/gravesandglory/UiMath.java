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
        if (hintRowWidth(keys, words, wordGap, width) <= maxW) return keys;
        int lo = 0, hi = 0;
        for (String k : keys) hi = Math.max(hi, width.applyAsInt(k));
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (hintRowWidth(capped(keys, mid, width), words, wordGap, width) <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return capped(keys, lo, width);
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
        List<String> fitted = fitKeys(keys, shortWords, CARD_ROW_END - CARD_KEY_AT[0], CARD_ROW_GAP, width);
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
}
