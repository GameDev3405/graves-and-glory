package gg.gravesandglory;

import java.util.ArrayDeque;
import java.util.List;

/** The client half of the battle event feed: the lines BattleHud draws on the feed leaf, newest at the bottom. At the HUD size
 *  (HudSize) a line lives UiMath.feedLife, 6 s at Compact and the spec's 8 s at Standard and Large, and the leaf holds
 *  UiMath.feedLines, three at Compact and five else: one line more pushes the oldest out. Lines come from feed_v1 packets and from
 *  server notices (a changed STATE message while the round runs); the feed empties whenever the round starts or ends and on
 *  disconnect. Client thread only. */
public final class EventFeed {
    private EventFeed() {}

    /** One line: its kind (point, loss, kill, down, up, rout, regroup, deploy or notice), the event's team, its text and when it
     *  arrived (client clock, ms). */
    public record Line(String kind, int team, String text, long bornMs) {}
    private static final ArrayDeque<Line> lines = new ArrayDeque<>();

    public static void push(String kind, int team, String text, long nowMs) {
        lines.addLast(new Line(kind, team, text, nowMs));
        trim(UiMath.feedLines(HudSize.get()));
    }

    /** The living lines at nowMs at the HUD size, oldest first (at most its UiMath.feedLines): a size made smaller since they came
     *  keeps the newest. */
    public static List<Line> lines(long nowMs) {
        int size = HudSize.get();
        lines.removeIf(l -> nowMs - l.bornMs() >= UiMath.feedLife(size));
        trim(UiMath.feedLines(size));
        return List.copyOf(lines);
    }

    private static void trim(int most) { while (lines.size() > most) lines.removeFirst(); }

    public static void clear() { lines.clear(); }

    /** The lines held, expired ones too (lines(now) drops those only when read): for checks that a clear emptied the feed, which
     *  must not pass because its lines aged out. */
    static int size() { return lines.size(); }
}
