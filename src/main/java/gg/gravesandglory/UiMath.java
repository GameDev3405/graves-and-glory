package gg.gravesandglory;

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
