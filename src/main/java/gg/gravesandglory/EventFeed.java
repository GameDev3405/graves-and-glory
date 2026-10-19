package gg.gravesandglory;

import java.util.ArrayDeque;
import java.util.List;

/** The client half of the battle event feed: the lines BattleHud draws on the feed slip, newest at the bottom. A line lives
 *  UiMath.FEED_LIFE_MS; a sixth line pushes the oldest out. Lines come from feed_v1 packets and from server notices (a changed
 *  STATE message while the round runs); the feed empties whenever the round starts or ends and on disconnect. Client thread only. */
public final class EventFeed {
    private EventFeed() {}

    /** One line: its kind (point, loss, kill, down, up, rout, regroup, deploy or notice), the event's team, its text and when it
     *  arrived (client clock, ms). */
    public record Line(String kind, int team, String text, long bornMs) {}
    /** The most lines the slip holds. */
    static final int MAX_LINES = 5;
    private static final ArrayDeque<Line> lines = new ArrayDeque<>();

    public static void push(String kind, int team, String text, long nowMs) {
        lines.addLast(new Line(kind, team, text, nowMs));
        while (lines.size() > MAX_LINES) lines.removeFirst();
    }

    /** The living lines at nowMs, oldest first (at most MAX_LINES). */
    public static List<Line> lines(long nowMs) {
        lines.removeIf(l -> nowMs - l.bornMs() >= UiMath.FEED_LIFE_MS);
        return List.copyOf(lines);
    }

    public static void clear() { lines.clear(); }
}
