package gg.gravesandglory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's Contracts page (the modpack's WarTablePages Contracts part) as a plain player's 5-row chest menu (plain clients P4).
 * Row 2: the three daily notes, each a Claim when done (progress:daily:(slot), with :(House) on the daily that completes all three);
 * row 3: Reroll (progress:reroll); row 4: the weekly (progress:weekly:(House)); row 5: the House the shards go to, while a claim that pays
 * shards is still to come (the banner's House until one is picked, as on the modpack page). Read from the server's Progression state
 * after Progression.reset, as Campaign.progressSnapshot does; every refusal is Progression's.
 */
final class PlainContracts {
    private PlainContracts() {}

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Contracts").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 5, new View());
    }

    static final class View implements PlainMenu.Page {
        /** The House picked for shards; null: the banner's House. */
        String house;

        @Override public void draw(PlainMenu m, ServerPlayerEntity p) {
            var b = GravesAndGlory.battle;
            if (b == null) return;
            Profile pr = b.campaign.profile(p.getUuid());
            var s = pr.progression;
            Progression.reset(s, Progression.today());
            String h = house != null ? house : RosterMath.rollHouse(pr.line()), hn = RosterMath.houseName(h);
            PlainWarTable.back(m, 0);
            m.icon(4, PlainMenu.named(Items.CLOCK, "Contracts", Formatting.GOLD, "Daily contracts reset at 00:00 UTC,", "the weekly on Monday."));
            for (int i = 0; i < 3; i++) {
                int target = Progression.target(s, i), done = Math.min(Progression.progress(s, i), target);
                String key = "daily:" + i;
                boolean claimed = s.claims.contains(key);
                var pay = pay(s, key, h);
                List<String> lore = new ArrayList<>();
                lore.add("Progress " + done + " / " + target);
                lore.add("Pays " + words(pay, hn));
                String why = Progression.refusal(s, key);
                String name = Progression.contract(s, i);
                if (claimed) { lore.add("Claimed"); m.icon(11 + 2 * i, PlainMenu.named(Items.FILLED_MAP, name, Formatting.DARK_GRAY, lore.toArray(String[]::new))); }
                else if (!why.isEmpty()) { lore.add(why); m.icon(11 + 2 * i, PlainMenu.named(Items.PAPER, name, Formatting.WHITE, lore.toArray(String[]::new))); }
                else {
                    lore.add("Click to claim");
                    m.command(11 + 2 * i, PlainMenu.named(Items.MAP, name, Formatting.GREEN, lore.toArray(String[]::new)), pay.shards() > 0 ? "progress:" + key + ":" + h : "progress:" + key);
                }
            }
            if (s.rerolled) m.icon(22, PlainMenu.named(Items.GRAY_DYE, "Reroll", Formatting.DARK_GRAY, "Rerolled today."));
            else if (s.claims.contains("daily:2")) m.icon(22, PlainMenu.named(Items.GRAY_DYE, "Reroll", Formatting.DARK_GRAY, "The third contract is already claimed."));
            else m.command(22, PlainMenu.named(Items.ENDER_EYE, "Reroll", Formatting.AQUA, "Swaps the third contract for", "\"Issue 10 tactical orders\". One free reroll a day."), "progress:reroll");
            int wt = Progression.WEEKLY_BATTLES;
            var wpay = pay(s, "weekly", h);
            List<String> wl = new ArrayList<>(List.of("Progress " + Math.min(s.weeklyBattles, wt) + " / " + wt, "Pays " + words(wpay, hn)));
            if (wpay.gold() >= 100 && !s.claims.contains("weekly")) wl.add("+ " + Progression.writs(wpay.gold() / 100));
            String wwhy = Progression.refusal(s, "weekly");
            String wname = "Complete " + wt + " battles this week";
            if (s.claims.contains("weekly")) { wl.add("Claimed"); m.icon(31, PlainMenu.named(Items.FILLED_MAP, wname, Formatting.DARK_GRAY, wl.toArray(String[]::new))); }
            else if (!wwhy.isEmpty()) { wl.add(wwhy); m.icon(31, PlainMenu.named(Items.WRITABLE_BOOK, wname, Formatting.WHITE, wl.toArray(String[]::new))); }
            else { wl.add("Click to claim"); m.command(31, PlainMenu.named(Items.WRITTEN_BOOK, wname, Formatting.GREEN, wl.toArray(String[]::new)), "progress:weekly:" + h); }
            if (shardsPending(s)) houseChips(m, 36, h, id -> house = id);
        }
    }

    /** Whether a claim that pays shards is still to come: the daily that completes all three, or the weekly. */
    static boolean shardsPending(Progression.State s) {
        return !s.claims.contains("weekly") || !(s.claims.contains("daily:0") && s.claims.contains("daily:1") && s.claims.contains("daily:2"));
    }

    /** The row of House chips from slot first: the one the shards go to is marked; a click picks another. */
    static void houseChips(PlainMenu m, int first, String on, java.util.function.Consumer<String> pick) {
        m.icon(first, PlainMenu.named(Items.NAME_TAG, "House shards go to", Formatting.GOLD, RosterMath.houseName(on), "Pick a House on the right."));
        int col = 2;
        for (var row : PlainRecruit.houses()) {
            if (col > 8) break;
            String id = row.id();
            boolean sel = id.equals(on);
            var st = PlainRecruit.named(banner(id), Text.empty().append(PlainRecruit.crest(id)).append(Text.literal(row.name()).formatted(sel ? Formatting.GREEN : Formatting.WHITE)),
                List.of(Text.literal(sel ? "Shards go here" : "Click to send shards here").formatted(sel ? Formatting.GREEN : Formatting.GRAY)));
            m.button(first + col, st, sel ? null : pl -> pick.accept(id));
            col++;
        }
    }

    static net.minecraft.item.Item banner(String house) {
        return switch (house) { case "gilded" -> Items.YELLOW_BANNER; case "graveborn" -> Items.BLACK_BANNER; case "ironclad" -> Items.GRAY_BANNER; case "tidecallers" -> Items.CYAN_BANNER; default -> Items.GREEN_BANNER; };
    }

    /** What claiming key would pay with arg: Progression.claim run on a copy with the contract finished and the tier reached (the modpack page's payOf). */
    static Progression.Grant pay(Progression.State p, String key, String arg) {
        var s = new Progression.State();
        s.schema = p.schema; s.xp = Math.max(p.xp, Progression.XP_PER_TIER * Progression.TIERS); s.totalBattles = p.totalBattles; s.totalWins = p.totalWins;
        s.weeklyBattles = Math.max(p.weeklyBattles, Progression.WEEKLY_BATTLES);
        s.day = p.day; s.week = p.week; s.title = p.title; s.rerolled = p.rerolled;
        s.dailyBattles = Math.max(p.dailyBattles, 3); s.dailyCaptures = Math.max(p.dailyCaptures, 1); s.dailyOrders = Math.max(p.dailyOrders, 10);
        s.claims = new HashSet<>(p.claims); s.titles = new LinkedHashSet<>(p.titles); s.matchGrants = new LinkedHashSet<>(p.matchGrants);
        s.claims.remove(key);
        return Progression.claim(s, arg == null ? key : key + ":" + arg);
    }

    /** A grant as words: "30 gear shards, 75 XP and 3 Ironclad shards". */
    static String words(Progression.Grant g, String houseName) {
        List<String> parts = new ArrayList<>();
        if (g.gold() > 0) parts.add(g.gold() + " gear shards");
        if (g.xp() > 0) parts.add(g.xp() + " XP");
        if (g.shards() > 0) parts.add(g.shards() + " " + houseName + " shards");
        if (parts.isEmpty()) return "nothing";
        if (parts.size() == 1) return parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }
}
