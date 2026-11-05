package gg.gravesandglory;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Battle seats are independent of the number of connected camp visitors. */
final class AdmissionPolicy {
    record Seat(UUID owner, int team, boolean bot, boolean seated) {}
    record Decision(int team, UUID replacement, boolean seated) {}

    static Decision decide(boolean running, boolean practice, int size, UUID player,
                           List<Seat> seats, Map<UUID, UUID> reservations) {
        return decide(running, practice, size, player, seats, reservations, -1);
    }

    /** rosterTeam is the team the lobby roster lists the player on (-1: none). A joiner without his own held seat takes a bot seat of that team
     *  first, or an empty seat on it, before any other side is tried. */
    static Decision decide(boolean running, boolean practice, int size, UUID player,
                           List<Seat> seats, Map<UUID, UUID> reservations, int rosterTeam) {
        int[] occupied = new int[2];
        int humans = 0;
        for (Seat seat : seats) if (seat.seated() || !running) {
            occupied[seat.team()]++;
            if (!seat.bot()) humans++;
        }
        int team = occupied[0] <= occupied[1] ? 0 : 1;
        if (!running || humans >= (practice ? 1 : size * 2))
            return new Decision(team, null, false);
        UUID held = reservations.get(player);
        for (Seat seat : seats)
            if (seat.seated() && seat.bot() && seat.owner().equals(held))
                return new Decision(seat.team(), seat.owner(), true);
        // Practice's opponent remains AI; a visitor cannot take that army.
        if (practice) return new Decision(team, null, false);
        if (rosterTeam == 0 || rosterTeam == 1) {
            for (Seat seat : seats)
                if (seat.seated() && seat.bot() && seat.team() == rosterTeam && !reservations.containsValue(seat.owner()))
                    return new Decision(seat.team(), seat.owner(), true);
            if (occupied[rosterTeam] < size) return new Decision(rosterTeam, null, true);
            for (Seat seat : seats)
                if (seat.seated() && seat.bot() && seat.team() == rosterTeam)
                    return new Decision(seat.team(), seat.owner(), true);
        }
        for (boolean reserved : new boolean[]{false, true})
            for (Seat seat : seats)
                if (seat.seated() && seat.bot() && reservations.containsValue(seat.owner()) == reserved)
                    return new Decision(seat.team(), seat.owner(), true);
        // Bot fill can be off. Add a human only on a side with an empty seat.
        if (occupied[team] < size) return new Decision(team, null, true);
        return new Decision(team, null, false);
    }
}
