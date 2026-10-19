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
        int w = 0, gap = 0;
        for (int k = 0; k < keys.size(); k++) {
            w += chipWidth(width.applyAsInt(keys.get(k))) + 2;
            gap = 2;
            String word = k < words.size() ? words.get(k) : null;
            if (word != null) {
                w += width.applyAsInt(word) + 8;
                gap = 8;
            }
        }
        return w - gap;
    }

    /** A hint-slip row's key names fitted to maxW: as they are when the row fits, else each trimmed by trimKey to the widest common
     *  cap at which the row fits, so the longest names give way first, and alike. A narrower cap never widens a name, so the cap is
     *  found by halving. */
    public static List<String> fitKeys(List<String> keys, List<String> words, int maxW, ToIntFunction<String> width) {
        if (hintRowWidth(keys, words, width) <= maxW) return keys;
        int lo = 0, hi = 0;
        for (String k : keys) hi = Math.max(hi, width.applyAsInt(k));
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (hintRowWidth(capped(keys, mid, width), words, width) <= maxW) lo = mid;
            else hi = mid - 1;
        }
        return capped(keys, lo, width);
    }

    static List<String> capped(List<String> keys, int cap, ToIntFunction<String> width) {
        List<String> out = new ArrayList<>();
        for (String k : keys) out.add(trimKey(k, cap, width));
        return out;
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
