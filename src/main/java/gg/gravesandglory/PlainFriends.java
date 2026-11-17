package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's Friends page and its war council (WarTableLobby) as a plain player's chest menus (plain clients P4, plan decisions 5
 * and 6). Both read the server's own social snapshot (Campaign.socialSnapshot, the facts a modpack client is sent) and send the
 * modpack pages' social: commands; the rules and refusals stay SocialHub's. Invites pick from the commanders in camp (no typing), so
 * joining a room by a typed code and copying the code stay modpack-only: a room is joined from its holder's row.
 * The menu redraws only after a click, so each view has a Refresh button.
 */
final class PlainFriends {
    private PlainFriends() {}

    /** Camp rows per page on the Friends view and the council's invite view. */
    static final int PER_PAGE = 8;
    /** Seats a side (WarTableLobby.SEATS). */
    static final int SEATS = 4;

    static final class View { int offset; boolean choosingHost, inviting; }

    static void open(ServerPlayerEntity p) {
        View v = new View();
        PlainMenu.open(p, Text.literal("Friends").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 6, (m, pl) -> friends(m, pl, v));
    }

    static void council(ServerPlayerEntity p) {
        View v = new View();
        PlainMenu.open(p, Text.literal("War council").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 6, (m, pl) -> council(m, pl, v));
    }

    // ---- the snapshot ----
    static JsonObject snap(ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        return b == null ? new JsonObject() : b.campaign.socialSnapshot(p.getUuid());
    }
    static boolean flag(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }
    static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    static int num(JsonObject o, String k, int fallback) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : fallback; }
    static List<JsonObject> rows(JsonObject o, String k) {
        List<JsonObject> out = new ArrayList<>();
        if (o.has(k) && o.get(k).isJsonArray()) for (var e : (JsonArray) o.get(k)) out.add(e.getAsJsonObject());
        return out;
    }

    static ItemStack head(String name, Formatting colour, List<String> lore) { return PlainBanner.item(Items.PLAYER_HEAD, name, colour, lore); }
    static ItemStack off(String name, String why) { return PlainBanner.item(Items.GRAY_DYE, name, Formatting.DARK_GRAY, PlainBanner.wrap(why, 40)); }
    static ItemStack on(Item item, String name, String... lore) { return PlainMenu.named(item, name, Formatting.GREEN, lore); }
    static void refresh(PlainMenu m, int slot) { m.button(slot, PlainMenu.named(Items.CLOCK, "Refresh", Formatting.WHITE, "Show the latest party and room state."), pl -> {}); }

    // ---- Friends ----
    static void friends(PlainMenu m, ServerPlayerEntity p, View v) {
        JsonObject s = snap(p);
        boolean party = flag(s, "party"), lead = flag(s, "partyLead");
        String room = str(s, "room");
        PlainWarTable.back(m, 0);
        refresh(m, 8);
        m.icon(4, head(party ? "Party led by " + str(s, "leader") : "No party", Formatting.GOLD,
            List.of(party ? "Your group stays together between matches." : "Create a party to keep friends together.", "Up to " + SocialHub.PARTY_LIMIT + " commanders.")));
        // the party's seats: this commander first, then the others; under each other member the leader's Make leader and Remove
        List<JsonObject> members = rows(s, "members");
        List<JsonObject> seats = new ArrayList<>();
        for (var r : members) if (flag(r, "me")) seats.add(r);
        for (var r : members) if (!flag(r, "me")) seats.add(r);
        if (seats.isEmpty()) m.icon(9, head(p.getGameProfile().getName(), Formatting.WHITE, List.of("You")));
        for (int i = 0; i < seats.size() && i < SocialHub.PARTY_LIMIT; i++) {
            var r = seats.get(i);
            String name = str(r, "name");
            boolean me = flag(r, "me"), online = me || flag(r, "online"), leader = name.equals(str(s, "leader"));
            List<String> lore = new ArrayList<>();
            if (me) lore.add("You");
            if (leader) lore.add("Leader");
            lore.add(online ? "In camp" : "Offline");
            m.icon(9 + i, head(name, leader ? Formatting.GOLD : online ? Formatting.WHITE : Formatting.GRAY, lore));
            if (lead && !me) {
                m.command(18 + i, on(Items.GOLDEN_HELMET, "Make " + name + " leader", "Hand the party's lead to " + name + "."), "social:transfer:" + name);
                m.command(27 + i, PlainMenu.named(Items.BARRIER, "Remove " + name, Formatting.RED, "Take " + name + " out of the party."), "social:kick:" + name);
            }
        }
        if (!party) m.command(14, on(Items.WHITE_BANNER, "Create party"), "social:create");
        else {
            m.command(14, PlainMenu.named(Items.OAK_DOOR, "Leave party", Formatting.RED), "social:leave");
            if (lead) m.command(15, PlainMenu.named(Items.TNT, "Disband party", Formatting.RED), "social:disband");
            else m.command(15, on(Items.GOLDEN_HELMET, "Claim offline lead", "Take the lead when the leader has been", "offline past the reconnect window."), "social:claim");
        }
        m.button(17, PlainMenu.named(Items.LECTERN, room.isEmpty() ? "Open the war council" : "Back to the war council", Formatting.GOLD,
            room.isEmpty() ? "Opens a room for your group." : "Room " + room), pl -> {
                if (str(snap(pl), "room").isEmpty()) PlainBanner.send(pl, "social:lobby");
                council(pl);
            });
        // a party invitation waiting
        String inviter = str(s, "inviter");
        if (!inviter.isEmpty()) {
            m.command(23, on(Items.LIME_DYE, "Accept invitation", "From " + inviter + " (" + num(s, "inviteSeconds", 0) + " s left)"), "social:accept");
            m.command(24, PlainMenu.named(Items.RED_DYE, "Decline", Formatting.RED, "From " + inviter), "social:decline");
        }
        // the camp: Invite on each commander's head, Join under it when he holds an open room
        List<JsonObject> camp = rows(s, "camp");
        if (v.offset >= camp.size()) v.offset = 0;
        m.icon(35, PlainMenu.named(Items.CAMPFIRE, "In camp", Formatting.GOLD, camp.isEmpty() ? "No one else is in camp yet." : camp.size() + " other commanders"));
        for (int k = 0; k < PER_PAGE && v.offset + k < camp.size(); k++) {
            var r = camp.get(v.offset + k);
            String name = str(r, "name"), theirs = str(r, "room");
            boolean inParty = flag(r, "party");
            String why = !party ? "Create a party first, then invite." : !lead ? "Only the party leader can invite." : inParty ? name + " is already in a party." : "";
            if (why.isEmpty()) m.command(36 + k, head("Invite " + name, Formatting.GREEN, List.of("Invite to your party (60 seconds).")), "social:invite:" + name);
            else m.icon(36 + k, head(name, Formatting.GRAY, PlainBanner.wrap(why, 40)));
            if (!theirs.isEmpty()) {
                boolean canJoin = (!party || lead) && room.isEmpty();
                if (canJoin) m.button(45 + k, on(Items.OAK_DOOR, "Join " + name + "'s room", "Room " + theirs), pl -> { PlainBanner.send(pl, "social:join:" + theirs); council(pl); });
                else m.icon(45 + k, off("Join " + name + "'s room", !room.isEmpty() ? "Leave your room first." : "Only your party leader can join a room."));
            }
        }
        if (camp.size() > PER_PAGE) m.button(44, PlainMenu.named(Items.ARROW, "More", Formatting.WHITE, "Next commanders in camp"), pl -> v.offset = v.offset + PER_PAGE < camp.size() ? v.offset + PER_PAGE : 0);
    }

    // ---- the war council (WarTableLobby) ----
    record Seat(String kind, String name, String house, boolean ready, boolean online, boolean host, boolean me) {}

    static List<Seat> seats(JsonObject s, int team) {
        List<Seat> out = new ArrayList<>();
        for (var r : rows(s, "commanders")) {
            if (!r.has("team") || r.get("team").getAsInt() != team || out.size() >= SEATS) continue;
            out.add(new Seat("human", str(r, "name"), str(r, "house"), flag(r, "ready"), !r.has("online") || flag(r, "online"), flag(r, "host"), flag(r, "me")));
        }
        boolean fill = !s.has("fill") || flag(s, "fill");
        int size = num(s, "size", 2);
        while (out.size() < SEATS) out.add(out.size() < size ? new Seat(fill ? "bot" : "open", fill ? "BOT" : "Open seat", "", false, true, false, false) : new Seat("closed", "Closed", "", false, true, false, false));
        return out;
    }

    static JsonObject me(JsonObject s) { for (var r : rows(s, "commanders")) if (flag(r, "me")) return r; return null; }

    static boolean heir(Seat seat) { return seat.kind().equals("human") && !seat.me() && seat.online(); }

    static String modeName(String id) { return id.isEmpty() ? "" : Character.toUpperCase(id.charAt(0)) + id.substring(1); }

    static String summary(String id) { for (var mo : Spec.MODES) if (mo.id().equals(id)) return mo.summary(); return ""; }

    /** Troops of a House the battle banner fields under that House pick (the lobby chip's count). */
    static int houseTroops(Profile pr, String house) {
        var cat = RosterMath.Catalog.fromSpec();
        return RosterMath.units(RosterMath.houseBanner(pr.line(), cat, house, RosterMath.fieldableHouses()), cat);
    }

    static Item seatItem(Seat seat, int team) {
        return switch (seat.kind()) {
            case "human" -> Items.PLAYER_HEAD;
            case "bot" -> Items.ZOMBIE_HEAD;
            case "open" -> team == 0 ? Items.WHITE_STAINED_GLASS_PANE : Items.LIGHT_GRAY_STAINED_GLASS_PANE;
            default -> Items.BLACK_STAINED_GLASS_PANE;
        };
    }

    static void council(PlainMenu m, ServerPlayerEntity p, View v) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        JsonObject s = snap(p);
        String room = str(s, "room");
        m.button(0, PlainMenu.named(Items.ARROW, "Back to Friends", Formatting.WHITE), PlainFriends::open);
        refresh(m, 8);
        if (room.isEmpty()) { noRoom(m, s); return; }
        if (v.inviting) { invite(m, s, v); return; }
        boolean playing = flag(s, "playing"), preparing = flag(s, "preparing"), idle = !playing, host = flag(s, "host") && idle;
        var mine = me(s);
        boolean myReady = mine != null && flag(mine, "ready");
        int myTeam = mine == null || !mine.has("team") ? -1 : mine.get("team").getAsInt();
        // the head: the code, the state, Invite and Leave
        m.icon(2, PlainMenu.named(Items.NAME_TAG, "Room " + room, Formatting.GOLD, "Friends join from your row on their Friends page.", "Typing a code needs the modpack."));
        String waiting = str(s, "waiting");
        int countdown = num(s, "countdown", 0);
        String state = playing ? "In battle" : preparing ? "Preparing your map" : countdown > 0 ? "Battle in " + countdown + " s" : waiting.isEmpty() ? "Ready to start" : waiting;
        m.icon(4, PlainMenu.named(Items.BELL, state, Formatting.WHITE, PlainBanner.wrap(waiting, 40).toArray(String[]::new)));
        if (preparing) { // the map wait: the Stand down plate and nothing else
            ready(m, s, b, p, myReady, idle);
            return;
        }
        if (idle && !v.choosingHost) m.button(6, PlainMenu.named(Items.PAPER, "Invite", Formatting.GREEN, "Invite a commander in camp to this room."), pl -> { v.inviting = true; v.offset = 0; });
        else m.icon(6, off("Invite", playing ? "Your room is in battle." : "Finish passing the host first."));
        if (idle && !v.choosingHost) m.command(7, PlainMenu.named(Items.OAK_DOOR, "Leave room", Formatting.RED), "social:exit");
        else m.icon(7, off("Leave room", playing ? "Finish the match before leaving the room." : "Finish passing the host first."));
        // the seats: team 0 on the left, team 1 on the right; a click takes a free seat on the other side, or while passing the host hands it on
        for (int t = 0; t < 2; t++) {
            var seats = seats(s, t);
            m.icon(t == 0 ? 9 : 14, PlainMenu.named(t == 0 ? Items.BLUE_BANNER : Items.RED_BANNER, t == 0 ? "Dawn" : "Dusk", Formatting.GOLD, "Team " + (t + 1)));
            for (int i = 0; i < SEATS; i++) {
                var seat = seats.get(i);
                int slot = (t == 0 ? 18 : 23) + i; // row 3: four seats a side
                List<String> lore = new ArrayList<>();
                if (seat.kind().equals("human")) {
                    if (seat.me()) lore.add("You");
                    if (seat.host()) lore.add("Host");
                    if (!seat.house().isEmpty()) lore.add(RosterMath.houseName(seat.house()));
                    lore.add(seat.ready() ? "Ready" : "Not ready");
                    if (!seat.online()) lore.add("Reconnecting");
                }
                Formatting colour = seat.ready() ? Formatting.GREEN : seat.kind().equals("human") ? Formatting.WHITE : Formatting.GRAY;
                if (v.choosingHost && heir(seat)) {
                    String name = seat.name();
                    lore.add("Click: make " + name + " host");
                    m.button(slot, PlainBanner.glint(PlainBanner.item(seatItem(seat, t), "Make " + name + " host", Formatting.GOLD, lore), true), pl -> { v.choosingHost = false; PlainBanner.send(pl, "social:host:" + name); });
                } else if (!v.choosingHost && idle && t != myTeam && (seat.kind().equals("bot") || seat.kind().equals("open"))) {
                    lore.add("Click: move your group to this side");
                    m.command(slot, PlainBanner.item(seatItem(seat, t), seat.name(), colour, lore), "social:team:" + t);
                } else m.icon(slot, PlainBanner.item(seatItem(seat, t), seat.name(), colour, lore));
            }
        }
        // the House picks and the banner
        var b2 = b.campaign;
        Profile pr = b2.profile(p.getUuid());
        String myHouse = mine == null || str(mine, "house").isEmpty() ? RosterMath.rollHouse(pr.line()) : str(mine, "house");
        int col = 0;
        for (String id : RosterMath.fieldableHouses()) {
            if (col > 5) break;
            boolean on = id.equals(myHouse);
            int troops = houseTroops(pr, id);
            List<Text> lore = new ArrayList<>();
            lore.add(Text.literal(troops + " troops on your banner").formatted(Formatting.GRAY));
            if (troops == 0) lore.add(Text.literal("Your banner has no " + RosterMath.houseName(id) + " troops.").formatted(Formatting.RED));
            lore.add(Text.literal(on ? "Your pick" : "Click to pick").formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY));
            Item banner = switch (id) { case "gilded" -> Items.YELLOW_BANNER; case "graveborn" -> Items.BLACK_BANNER; case "ironclad" -> Items.GRAY_BANNER; case "tidecallers" -> Items.CYAN_BANNER; default -> Items.GREEN_BANNER; };
            ItemStack chip = PlainBanner.glint(PlainRecruit.named(banner, Text.empty().append(PlainRecruit.crest(id)).append(Text.literal(RosterMath.houseName(id)).formatted(on ? Formatting.GOLD : Formatting.WHITE)), lore), on);
            if (idle && !v.choosingHost) m.command(27 + col, chip, "social:house:" + id); else m.icon(27 + col, chip);
            col++;
        }
        if (idle && !v.choosingHost) m.button(35, PlainMenu.named(Items.WHITE_BANNER, "Change banner", Formatting.GOLD, "Open the Banner page"), PlainBanner::open);
        else m.icon(35, off("Change banner", "Your room is in battle."));
        // the host's settings: mode, map, seats, bots fill and tier
        String hostWhy = playing ? "Your room is in battle." : "Only the host changes this.";
        List<String> modes = new ArrayList<>();
        if (s.has("modes")) for (var e : s.getAsJsonArray("modes")) modes.add(e.getAsString());
        if (modes.isEmpty()) modes.addAll(SocialHub.MODES);
        String mode = str(s, "mode");
        for (int i = 0; i < modes.size() && i < 4; i++) {
            String id = modes.get(i);
            boolean on = id.equals(mode);
            List<String> lore = new ArrayList<>(PlainBanner.wrap(summary(id), 40));
            lore.add(on ? "Chosen" : host ? "Click to choose" : hostWhy);
            ItemStack tab = PlainBanner.glint(PlainBanner.item(Items.GOAT_HORN, modeName(id), on ? Formatting.GOLD : Formatting.WHITE, lore), on);
            if (host && !on) m.command(36 + i, tab, "social:mode:" + id); else m.icon(36 + i, tab);
        }
        int n = Spec.MAPS.length, map = Math.max(0, Math.min(n - 1, num(s, "map", 0)));
        var mapRow = Spec.MAPS[map];
        if (host) m.command(40, PlainMenu.named(Items.ARROW, "Previous map", Formatting.WHITE, Spec.MAPS[Math.floorMod(map - 1, n)].name()), "social:map:" + Math.floorMod(map - 1, n));
        else m.icon(40, off("Previous map", hostWhy));
        List<String> mapLore = new ArrayList<>(PlainBanner.wrap(mapRow.description(), 40));
        if ("map".equals(str(s, "fitted"))) mapLore.add("Changed to fit the mode");
        m.icon(41, PlainBanner.item(Items.FILLED_MAP, mapRow.name(), Formatting.GOLD, mapLore));
        if (host) m.command(42, PlainMenu.named(Items.ARROW, "Next map", Formatting.WHITE, Spec.MAPS[Math.floorMod(map + 1, n)].name()), "social:map:" + Math.floorMod(map + 1, n));
        else m.icon(42, off("Next map", hostWhy));
        int size = num(s, "size", 2);
        if (host && size > 1) m.command(43, PlainMenu.named(Items.RED_DYE, "Fewer seats", Formatting.WHITE, (size - 1) + " a side"), "social:size:" + (size - 1));
        else m.icon(43, off("Fewer seats", host ? "One seat a side is the least." : hostWhy));
        m.icon(44, PlainMenu.named(Items.OAK_SIGN, size + " a side", Formatting.GOLD, "Seats on each team"));
        if (host && size < SEATS) m.command(45, PlainMenu.named(Items.LIME_DYE, "More seats", Formatting.WHITE, (size + 1) + " a side"), "social:size:" + (size + 1));
        else m.icon(45, off("More seats", host ? SEATS + " seats a side is the most." : hostWhy));
        boolean fill = !s.has("fill") || flag(s, "fill");
        var fillStack = PlainBanner.item(fill ? Items.ZOMBIE_HEAD : Items.SKELETON_SKULL, fill ? "Bots fill seats" : "Seats stay empty", Formatting.WHITE, List.of(host ? "Click to switch" : hostWhy));
        if (host) m.command(46, fillStack, "social:fill"); else m.icon(46, fillStack);
        String tier = str(s, "difficulty");
        Item[] swords = {Items.WOODEN_SWORD, Items.IRON_SWORD, Items.DIAMOND_SWORD};
        for (int i = 0; i < Spec.BOTCOMMAND.length && i < 3; i++) {
            String id = Spec.BOTCOMMAND[i].id();
            boolean on = id.equals(tier);
            ItemStack t = PlainBanner.glint(PlainBanner.item(swords[i], "Bots: " + PlainField.tierName(id), on ? Formatting.GOLD : Formatting.WHITE, List.of(on ? "Chosen" : host ? "Click to choose" : hostWhy)), on);
            if (host && !on) m.command(47 + i, t, "social:difficulty:" + id); else m.icon(47 + i, t);
        }
        // Pass host (the host's), Ready, Start now
        if (host) {
            boolean heirs = false;
            for (int t = 0; t < 2; t++) for (var seat : seats(s, t)) if (heir(seat)) heirs = true;
            if (heirs) m.button(51, PlainBanner.glint(PlainMenu.named(Items.GOLDEN_HELMET, v.choosingHost ? "Passing the host: pick a seat" : "Pass host", Formatting.GOLD,
                v.choosingHost ? "Click a glowing seat, or here to stop." : "Hand the host's seat to another commander."), v.choosingHost), pl -> v.choosingHost = !v.choosingHost);
            else m.icon(51, off("Pass host", "No other commander online in the room."));
        } else v.choosingHost = false;
        boolean startNow = host && !preparing && waiting.isEmpty() && !rows(s, "commanders").isEmpty() && !v.choosingHost;
        if (startNow) m.command(52, on(Items.GOLDEN_SWORD, "Start now", "Every commander is ready."), "social:start");
        if (!v.choosingHost) ready(m, s, b, p, myReady, idle);
    }

    /** The Ready plate (slot 53): Ready or Stand down, refused while the House pick fields no troop (the server refuses the same). */
    static void ready(PlainMenu m, JsonObject s, GravesAndGlory.Battle b, ServerPlayerEntity p, boolean myReady, boolean idle) {
        var mine = me(s);
        int troops = mine == null || !mine.has("troops") ? -1 : mine.get("troops").getAsInt();
        String label = flag(s, "playing") ? "In battle" : myReady ? "Stand down" : "Ready";
        if (!idle) { m.icon(53, off(label, "Your room is in battle.")); return; }
        if (troops == 0 && !myReady) {
            Profile pr = b.campaign.profile(p.getUuid());
            String house = str(mine, "house").isEmpty() ? RosterMath.rollHouse(pr.line()) : str(mine, "house");
            m.icon(53, off("Ready", "Your banner has no " + RosterMath.houseName(house) + " troops."));
            return;
        }
        m.command(53, PlainBanner.glint(PlainMenu.named(myReady ? Items.RED_DYE : Items.LIME_DYE, label, myReady ? Formatting.YELLOW : Formatting.GREEN,
            myReady ? "Take your ready back." : "The battle begins ten seconds after everyone is ready."), myReady), "social:ready");
    }

    /** No room yet: Open the council (social:lobby), and the open rooms of the camp to join (a typed code needs the modpack). */
    static void noRoom(PlainMenu m, JsonObject s) {
        boolean party = flag(s, "party"), lead = flag(s, "partyLead");
        m.command(4, on(Items.LECTERN, "Open the council", "A room for your group, three seats a side."), "social:lobby");
        int k = 0;
        for (var r : rows(s, "camp")) {
            String code = str(r, "room"), name = str(r, "name");
            if (code.isEmpty() || k >= 27) continue;
            if (!party || lead) m.command(18 + k, on(Items.OAK_DOOR, "Join " + name + "'s room", "Room " + code), "social:join:" + code);
            else m.icon(18 + k, off("Join " + name + "'s room", "Only your party leader can join a room."));
            k++;
        }
        if (k == 0) m.icon(22, PlainMenu.named(Items.PAPER, "No open rooms in camp", Formatting.GRAY, "Joining by a typed code needs the modpack."));
    }

    /** The room invitation view: a row per other commander in camp (social:roominvite), More, and Close. */
    static void invite(PlainMenu m, JsonObject s, View v) {
        List<JsonObject> camp = rows(s, "camp");
        if (v.offset >= camp.size()) v.offset = 0;
        m.button(4, PlainMenu.named(Items.BARRIER, "Close", Formatting.WHITE, "Back to the room"), pl -> v.inviting = false);
        if (camp.isEmpty()) m.icon(22, PlainMenu.named(Items.PAPER, "No one else is in camp yet.", Formatting.GRAY, "Friends can join from your row on their Friends page."));
        int perPage = 27;
        for (int k = 0; k < perPage && v.offset + k < camp.size(); k++) {
            var r = camp.get(v.offset + k);
            String name = str(r, "name");
            boolean free = str(r, "room").isEmpty();
            if (free) m.command(18 + k, head("Invite " + name, Formatting.GREEN, List.of("Tell " + name + " your room is open.")), "social:roominvite:" + name);
            else m.icon(18 + k, head(name, Formatting.GRAY, List.of("In a room already.")));
        }
        if (camp.size() > perPage) m.button(49, PlainMenu.named(Items.ARROW, "More", Formatting.WHITE), pl -> v.offset = v.offset + perPage < camp.size() ? v.offset + perPage : 0);
    }
}
