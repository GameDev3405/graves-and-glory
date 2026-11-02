package gg.gravesandglory;

import java.util.*;

/** Slice dry run 2026-11-02 minors: no "moved too quickly" after a deliberate teleport, and a playing room keeps a dropped commander's ready flag. */
public final class TeleportRelocateCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) {
        // Camp to arena is a long jump; the client's first packet after the confirm lands on the new spot.
        double[] camp = {0.5, 70, 0.5}, base = {-120.5, 66, 14.5};
        double[] stale = MovementGuard.referenceAfterTeleport(camp, base, false);
        check(MovementGuard.movedTooQuickly(stale[0], stale[1], stale[2], base[0], base[1], base[2], 0), "an unsynced reference reads the teleport as too quick");
        double[] synced = MovementGuard.referenceAfterTeleport(camp, base, true);
        check(!MovementGuard.movedTooQuickly(synced[0], synced[1], synced[2], base[0], base[1], base[2], 0), "a synced reference accepts the arrival");
        check(!MovementGuard.movedTooQuickly(synced[0], synced[1], synced[2], base[0] + 0.3, base[1], base[2], 0), "walking on from the new spot is fine");
        check(MovementGuard.movedTooQuickly(synced[0], synced[1], synced[2], base[0] + 30, base[1], base[2], 0), "a real speed hack is still caught");

        // Ready flag across a mid-round drop and return (a943850 kept it; this pins it).
        SocialHub hub = new SocialHub();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        long now = 1000;
        hub.presence(Set.of(a, b), now);
        hub.createRoom(a);
        SocialHub.Room room = hub.room(a);
        room.members.add(b); room.teams.put(b, 1); room.teams.put(a, 0);
        room.ready.add(a); room.ready.add(b);
        hub.begin(room);
        hub.presence(Set.of(a), now + 50);
        check(room.ready.contains(b), "a commander who drops mid-round keeps his ready flag");
        hub.presence(Set.of(a, b), now + 1000);
        check(room.ready.contains(b) && room.ready.contains(a), "he is still ready after he returns");
        hub.finish();
        check(room.ready.isEmpty(), "the round's end clears readiness");
        room.ready.add(b);
        hub.presence(Set.of(a), now + 2000);
        check(!room.ready.contains(b), "an idle room still drops a disconnected member's ready");
        System.out.println("TeleportRelocateCheck: " + checks + " checks passed");
    }
}
