package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's Chapter page (the modpack's WarTablePages Chapter part: the Founders reward track and titles) as a plain player's 6-row
 * chest menu (plain clients P4). Row 1: the chapter and XP, Claim (the lowest tier reached and not claimed: progress:founders:(tier), with
 * :(House) on a shard tier and :(Rare) on tier 20, as RecruitPages.foundersClaim builds it), the Rare picked for tier 20 and the title worn;
 * rows 2 and 3: the tier track, a page of 18 tiers with arrows on row 4; row 5: the House the shards go to, while the next claim pays
 * shards; row 6: the titles owned, a click wears one (progress:title:(title), or Recruit to take the worn one off). Read from the server's
 * Progression state; every refusal is Progression's.
 */
final class PlainChapter {
    private PlainChapter() {}

    static final int PAGE = 18, TITLES_SHOWN = 8;

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Chapter").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 6, new View());
    }

    static final class View implements PlainMenu.Page {
        /** The House picked for shards (null: the banner's), the Rare picked for tier 20 (null: the House's first), the track page (-1: the claim's) and the titles page. */
        String house, rare;
        int page = -1, titlePage;

        @Override public void draw(PlainMenu m, ServerPlayerEntity p) {
            var b = GravesAndGlory.battle;
            if (b == null) return;
            Profile pr = b.campaign.profile(p.getUuid());
            var s = pr.progression;
            Progression.reset(s, Progression.today());
            String h = house != null ? house : RosterMath.rollHouse(pr.line()), hn = RosterMath.houseName(h);
            List<String> rares = rares(h);
            String rarePick = rares.isEmpty() ? "" : rare != null && rares.contains(rare) ? rare : rares.get(0);
            int tier = Progression.tier(s), claimable = claimable(s), lowest = lowestUnclaimed(s);
            PlainWarTable.back(m, 0);
            var roster = RosterMath.launchRoster();
            String chapter = Progression.chapter(s, (int) roster.stream().filter(pr.owned::containsKey).count(), roster.size());
            int into = s.xp % Progression.XP_PER_TIER;
            m.icon(2, PlainMenu.named(Items.EXPERIENCE_BOTTLE, "Founders: tier " + tier + " of " + Progression.TIERS, Formatting.GOLD,
                tier >= Progression.TIERS ? "Every tier reached." : "XP " + into + " / " + Progression.XP_PER_TIER + " to tier " + (tier + 1),
                "Chapter " + chapter, "Battles " + s.totalBattles + ", wins " + s.totalWins));
            if (claimable > 0) {
                boolean shard = Progression.SHARD_TIERS.contains(claimable), rareTier = claimable == Progression.RARE_TIER;
                String reward = rewardOf(claimable, hn);
                if (rareTier && rarePick.isEmpty()) m.icon(4, PlainMenu.named(Items.GRAY_DYE, "Claim tier " + claimable, Formatting.DARK_GRAY, hn + " has no Rare troop.", "Pick another House below."));
                else {
                    String cmd = shard ? "progress:founders:" + claimable + ":" + h : rareTier ? "progress:founders:" + claimable + ":" + rarePick : "progress:founders:" + claimable;
                    String gives = rareTier ? Progression.troop(rarePick).name() + " (Rare)" : reward;
                    m.command(4, PlainMenu.named(Items.CHEST, "Claim tier " + claimable, Formatting.GREEN, "Gives " + gives, "Click to claim"), cmd);
                }
            } else {
                String why = lowest > Progression.TIERS ? "Every tier is claimed." : Progression.refusal(s, Progression.SEASON + ":" + lowest);
                m.icon(4, PlainMenu.named(Items.GRAY_DYE, lowest > Progression.TIERS ? "Claimed" : "Locked", Formatting.DARK_GRAY, why));
            }
            if (claimable == Progression.RARE_TIER && !rarePick.isEmpty()) {
                var t = Progression.troop(rarePick);
                String now = rarePick;
                m.button(6, PlainMenu.named(PlainArmoury.egg(t), "Rare troop: " + t.name(), Formatting.AQUA, "Tier " + Progression.RARE_TIER + " gives this " + hn + " Rare.", "Click for the next Rare of " + hn + "."),
                    pl -> rare = rares.get((rares.indexOf(now) + 1) % rares.size()));
            }
            m.icon(8, PlainMenu.named(Items.NAME_TAG, "Title: " + s.title, Formatting.YELLOW, "Wear another from the bottom row."));

            int pages = (Progression.TIERS + PAGE - 1) / PAGE;
            if (page < 0) page = Math.max(0, Math.min(pages - 1, (Math.max(1, Math.min(lowest, Progression.TIERS)) - 1) / PAGE));
            page = Math.max(0, Math.min(page, pages - 1));
            for (int k = 0; k < PAGE; k++) {
                int t = page * PAGE + k + 1;
                if (t > Progression.TIERS) break;
                boolean claimed = s.claims.contains(Progression.SEASON + ":" + t), reached = tier >= t;
                String state = claimed ? "Claimed" : reached ? (t == claimable ? "Ready: press Claim" : "Reached") : "Locked: " + Math.max(0, t * Progression.XP_PER_TIER - s.xp) + " XP to go";
                int ti = titleAt(t);
                List<String> lore = new ArrayList<>(List.of(rewardOf(t, hn), state));
                if (ti >= 0 && claimed) {
                    String title = Progression.TITLES[ti];
                    boolean worn = title.equals(s.title);
                    lore.add(worn ? "Worn. Click to take it off." : "Click to wear this title.");
                    m.command(9 + k, PlainMenu.named(Items.NAME_TAG, "Tier " + t, worn ? Formatting.GREEN : Formatting.YELLOW, lore.toArray(String[]::new)), "progress:title:" + (worn ? "Recruit" : title));
                } else {
                    Item item = claimed ? Items.LIME_STAINED_GLASS_PANE : !reached ? Items.BLACK_STAINED_GLASS_PANE : rewardItem(t);
                    m.icon(9 + k, PlainMenu.named(item, "Tier " + t, claimed ? Formatting.DARK_GREEN : reached ? Formatting.GREEN : Formatting.DARK_GRAY, lore.toArray(String[]::new)));
                }
            }
            if (page > 0) m.button(27, PlainMenu.named(Items.SPECTRAL_ARROW, "Earlier tiers", Formatting.WHITE, "Tiers " + ((page - 1) * PAGE + 1) + " to " + page * PAGE), pl -> page--);
            if (page < pages - 1) m.button(35, PlainMenu.named(Items.SPECTRAL_ARROW, "Later tiers", Formatting.WHITE, "Tiers " + ((page + 1) * PAGE + 1) + " to " + Math.min(Progression.TIERS, (page + 2) * PAGE)), pl -> page++);
            if (claimable > 0 && (Progression.SHARD_TIERS.contains(claimable) || claimable == Progression.RARE_TIER))
                PlainContracts.houseChips(m, 36, h, id -> { house = id; rare = null; });

            List<String> titles = new ArrayList<>(s.titles);
            int tp = Math.max(1, (titles.size() + TITLES_SHOWN - 1) / TITLES_SHOWN);
            titlePage = Math.max(0, Math.min(titlePage, tp - 1));
            for (int k = 0; k < TITLES_SHOWN; k++) {
                int at = titlePage * TITLES_SHOWN + k;
                if (at >= titles.size()) break;
                String title = titles.get(at);
                boolean worn = title.equals(s.title);
                if (worn && title.equals("Recruit")) m.icon(45 + k, PlainMenu.named(Items.PAPER, title, Formatting.GREEN, "Worn."));
                else m.command(45 + k, PlainMenu.named(worn ? Items.NAME_TAG : Items.PAPER, title, worn ? Formatting.GREEN : Formatting.WHITE,
                    worn ? "Worn. Click to take it off." : "Click to wear."), "progress:title:" + (worn ? "Recruit" : title));
            }
            if (tp > 1) m.button(53, PlainMenu.named(Items.SPECTRAL_ARROW, "More titles", Formatting.WHITE, "Page " + (titlePage + 1) + " of " + tp), pl -> titlePage = (titlePage + 1) % tp);
        }
    }

    /** The lowest tier reached and not yet claimed, which Claim takes, or -1. */
    static int claimable(Progression.State s) {
        int top = Progression.tier(s);
        for (int t = 1; t <= top; t++) if (!s.claims.contains(Progression.SEASON + ":" + t)) return t;
        return -1;
    }

    /** The lowest tier not yet claimed (TIERS + 1 once every tier is). */
    static int lowestUnclaimed(Progression.State s) {
        for (int t = 1; t <= Progression.TIERS; t++) if (!s.claims.contains(Progression.SEASON + ":" + t)) return t;
        return Progression.TIERS + 1;
    }

    static int titleAt(int tier) {
        for (int i = 0; i < Progression.TITLE_TIERS.length; i++) if (Progression.TITLE_TIERS[i] == tier) return i;
        return -1;
    }

    /** What a tier gives, a shard tier naming the House the shards go to. */
    static String rewardOf(int t, String houseName) {
        String r = Progression.reward(t);
        return Progression.SHARD_TIERS.contains(t) ? r.replace("House shards", houseName + " shards") : r;
    }

    static Item rewardItem(int t) {
        if (Progression.GOLD_TIERS.contains(t)) return Items.GOLD_NUGGET;
        if (Progression.SHARD_TIERS.contains(t)) return Items.PRISMARINE_SHARD;
        if (t == Progression.RARE_TIER) return Items.EMERALD;
        if (Progression.COSMETIC_TIERS.containsKey(t)) return Items.WHITE_BANNER;
        return Items.NAME_TAG;
    }

    /** The Rares of a House in sheet order: tier 20's choice (RecruitPages.rares). */
    static List<String> rares(String house) {
        List<String> out = new ArrayList<>();
        for (var t : Spec.TROOPS) if (t.house().equals(house) && t.rarity().equals("rare")) out.add(t.id());
        return out;
    }
}
