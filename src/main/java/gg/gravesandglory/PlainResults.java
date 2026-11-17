package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The War Table's battle report (WarTableReport) as a plain player's chest menu (plain clients P4, plan decision 5), read-only from the
 * report the server keeps for him (Campaign.results, the snapshot's result): the outcome, map and length, the mode's section, what the
 * battle paid, the commanders (score, rank, MVP, awards), the Houses of his banner, his troops' Ranks and the Long March story. Its
 * buttons are the report's own tags (Contracts, Chapter, Back to camp or Return to room); while his match ending still runs, Play again
 * and Skip (ending:again, ending:skip) as on the ending screen.
 */
final class PlainResults {
    private PlainResults() {}

    static void open(ServerPlayerEntity p) {
        PlainMenu.open(p, Text.literal("Results").styled(st -> st.withFont(PlainPack.TITLE_FONT)), 6, PlainResults::draw);
    }

    static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }
    static int num(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsInt() : 0; }
    static boolean flag(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() && o.get(k).getAsBoolean(); }
    static JsonArray arr(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonArray() ? o.getAsJsonArray(k) : new JsonArray(); }
    static String clock(int seconds) { return seconds / 60 + ":" + String.format("%02d", seconds % 60); }

    static void draw(PlainMenu m, ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        var c = b.campaign;
        PlainWarTable.back(m, 0);
        // the report's tags
        m.button(46, PlainMenu.named(Items.PAPER, "Contracts", Formatting.GOLD, "Open the Contracts page"), PlainContracts::open);
        m.button(47, PlainMenu.named(Items.NETHER_STAR, "Chapter", Formatting.GOLD, "Open the Chapter page"), PlainChapter::open);
        var social = c.socialSnapshot(p.getUuid());
        boolean room = social.has("room") && !social.get("room").getAsString().isEmpty();
        if (room) m.button(49, PlainMenu.named(Items.LECTERN, "Return to room", Formatting.GREEN, "Back to your war council"), PlainFriends::council);
        else m.button(49, PlainMenu.named(Items.CAMPFIRE, "Back to camp", Formatting.GREEN, "Back to the War Table"), PlainWarTable::open);
        if (c.endingJson(p.getUuid()) != null) {
            m.command(51, PlainMenu.named(Items.GOLDEN_SWORD, "Play again", Formatting.GREEN, room ? "End the sequence and ready up in your room." : "End the battle's closing sequence."), "ending:again");
            m.command(52, PlainMenu.named(Items.FEATHER, "Skip", Formatting.WHITE, "Skip the rest of the closing sequence."), "ending:skip");
        }
        JsonObject r = c.results.getOrDefault(p.getUuid(), new JsonObject());
        if (!r.has("id")) { PlainWarTable.note(m, 22, "No battle to report yet.", List.of()); return; }

        // the note: outcome, map and length
        String outcome = str(r, "outcome");
        List<String> head = new ArrayList<>();
        head.add(str(r, "map") + ", " + clock((int) Math.round(r.has("seconds") ? r.get("seconds").getAsDouble() : 0)));
        if (!str(r, "line").isEmpty()) head.addAll(PlainBanner.wrap(PlainText.plain(str(r, "line")), 40));
        Item flagItem = outcome.equals("VICTORY") ? Items.GOLDEN_HELMET : outcome.equals("DEFEAT") ? Items.SKELETON_SKULL : Items.IRON_HELMET;
        m.icon(4, PlainBanner.item(flagItem, outcome.isEmpty() ? "Battle" : outcome, outcome.equals("VICTORY") ? Formatting.GOLD : outcome.equals("DEFEAT") ? Formatting.RED : Formatting.WHITE, head));
        // the mode's section
        List<String> mode = new ArrayList<>(PlainBanner.wrap(ReportSections.rule(r), 40));
        for (var row : ReportSections.rows(r)) mode.add(row.label() + ": " + row.value());
        m.icon(2, PlainBanner.item(Items.WHITE_BANNER, ReportSections.title(r), Formatting.GOLD, mode));
        // what the battle paid
        m.icon(6, PlainBanner.item(Items.GOLD_INGOT, "Rewards", Formatting.YELLOW, rewards(r)));
        // the story of a Long March battle
        var march = r.has("march") && r.get("march").isJsonObject() ? r.getAsJsonObject("march") : null;
        if (march != null) {
            List<String> story = new ArrayList<>();
            if (!str(march, "after").isEmpty()) story.addAll(PlainBanner.wrap(PlainText.plain(str(march, "after")), 40));
            if (!str(march, "chapterEnd").isEmpty()) { story.add(""); story.addAll(PlainBanner.wrap(PlainText.plain(str(march, "chapterEnd")), 40)); }
            if (!story.isEmpty()) m.icon(8, PlainBanner.item(Items.WRITTEN_BOOK, "After the battle", Formatting.GOLD, story));
        }
        // the commanders, your team first (row 2, then row 3)
        JsonArray squads = arr(r, "squads");
        m.icon(9, PlainBanner.item(Items.OAK_SIGN, "Commanders", Formatting.GOLD, List.of(squads.size() + (squads.size() == 1 ? " army" : " armies"))));
        for (int i = 0; i < squads.size() && i < 16; i++) {
            var o = squads.get(i).getAsJsonObject();
            boolean you = flag(o, "you"), mvp = flag(o, "mvp"), bot = flag(o, "bot");
            List<String> lore = new ArrayList<>();
            lore.add("Team " + (num(o, "team") + 1) + (you ? ", you" : "") + (mvp ? ", MVP" : "") + (flag(o, "left") ? ", left the battle" : ""));
            if (!str(o, "house").isEmpty()) lore.add(RosterMath.houseName(str(o, "house")));
            lore.add("Score " + num(o, "score") + (num(o, "rank") > 0 ? ", rank " + num(o, "rank") + " of " + squads.size() : ""));
            lore.add("Kills " + num(o, "kills") + "  lost " + num(o, "lost") + "  kept " + num(o, "kept"));
            lore.add("Commander kills " + num(o, "ck") + "  deaths " + num(o, "cd") + "  downs " + num(o, "downs"));
            lore.add("Gold earned " + num(o, "gold") + "  on points " + num(o, "objective") + " s");
            if (!str(o, "award").isEmpty()) { lore.add(str(o, "award")); lore.addAll(PlainBanner.wrap(str(o, "awardText"), 40)); }
            Item it = bot ? Items.ZOMBIE_HEAD : Items.PLAYER_HEAD;
            m.icon(10 + i + (i >= 8 ? 1 : 0), PlainBanner.glint(PlainBanner.item(it, str(o, "name"), mvp ? Formatting.GOLD : you ? Formatting.GREEN : Formatting.WHITE, lore), mvp));
        }
        // the Houses of the banner (row 4)
        JsonArray houses = arr(r, "houses");
        for (int i = 0; i < houses.size() && i < 8; i++) {
            var h = houses.get(i).getAsJsonObject();
            String id = str(h, "house");
            int tier = num(h, "tier");
            List<Text> lore = List.of(Text.literal(tier > 0 ? "Trait " + MatchStats.roman(tier) + " held " + num(h, "traitSeconds") + "s" : "No trait tier").formatted(tier > 0 ? Formatting.WHITE : Formatting.GRAY),
                Text.literal("+" + num(h, "shards") + " shards  +" + num(h, "renown") + " Renown").formatted(Formatting.GRAY));
            Item banner = switch (id) { case "gilded" -> Items.YELLOW_BANNER; case "graveborn" -> Items.BLACK_BANNER; case "ironclad" -> Items.GRAY_BANNER; case "tidecallers" -> Items.CYAN_BANNER; default -> Items.GREEN_BANNER; };
            m.icon(28 + i, PlainRecruit.named(banner, Text.empty().append(PlainRecruit.crest(id)).append(Text.literal(RosterMath.houseName(id)).formatted(Formatting.GOLD)), lore));
        }
        // the troops' Ranks
        List<String> levels = new ArrayList<>();
        for (var e : arr(r, "levels")) {
            var o = e.getAsJsonObject();
            var t = PlainBanner.troop(str(o, "troop"));
            if (t == null) continue;
            var row = new MatchStats.LevelRow(t.id(), num(o, "highestLevel"), num(o, "level3Count"), num(o, "weapon"), num(o, "armour"));
            levels.add(t.name() + ": " + MatchStats.levelText(row, t.name(), LevelMath.levelName(t, row.highest())));
        }
        if (levels.isEmpty()) levels.add("No troop Ranks this battle.");
        m.icon(37, PlainBanner.item(Items.IRON_SWORD, "Troops in battle", Formatting.GOLD, levels.size() > 24 ? levels.subList(0, 24) : levels));
        // the chapter, as the report's Chapter face sums it
        var prog = c.profile(p.getUuid()).progression;
        int tier = Progression.tier(prog), xp = Math.min(prog.xp, 7500);
        String next = tier >= 30 ? "Chapter complete. Troops and titles stay yours." : "Next, tier " + (tier + 1) + ": " + Progression.reward(tier + 1) + ". " + (250 - xp % 250) + " XP to go.";
        List<String> chapter = new ArrayList<>();
        chapter.add("Tier " + tier + " of 30, " + xp + " XP");
        chapter.addAll(PlainBanner.wrap(next, 40));
        m.icon(39, PlainBanner.item(Items.EXPERIENCE_BOTTLE, "Founders", Formatting.GOLD, chapter));
    }

    /** The report's rewards in their static form: the saved rows, or the plain line and the Long March rows, or why nothing was saved. */
    static List<String> rewards(JsonObject r) {
        List<String> out = new ArrayList<>();
        boolean saved = flag(r, "saved"), eligible = flag(r, "eligible");
        if (!saved) { out.add("Rewards not saved"); out.add("Progress unchanged. Check disk space before playing again."); return out; }
        var rows = arr(r, "rewards");
        if (r.has("rewards")) {
            for (var e : rows) { var o = e.getAsJsonObject(); out.add("+" + num(o, "amount") + " " + str(o, "label") + (str(o, "reason").isEmpty() ? "" : " (" + str(o, "reason") + ")")); }
            if (out.isEmpty()) out.add(eligible ? "Nothing earned this battle." : "No match rewards: play at least 3 minutes to qualify.");
            return out;
        }
        out.add("+" + num(r, "writs") + " writs  +" + num(r, "gearShards") + " gear shards  +" + num(r, "xp") + " chapter XP");
        var march = r.has("march") && r.get("march").isJsonObject() ? r.getAsJsonObject("march") : null;
        if (march != null) {
            String state = str(march, "bonusState");
            if (state.equals("paid")) out.add("Bonus kept: +" + num(march, "bonusShards") + " shards, +" + num(march, "bonusGold") + " gear shards");
            else if (state.equals("claimed")) out.add("Bonus claimed before");
            else if (!state.equals("none") && !state.equals("lost") && !state.isEmpty()) out.add("Bonus missed");
            int pacts = num(march, "pacts"), held = num(march, "pactsHeld");
            if (pacts > 0) out.add((pacts == 1 ? "A Pact earned" : pacts + " Pacts earned") + ", held " + held);
            else if (held > 0) out.add("Pacts held: " + held);
            if (!eligible) out.add("No rewards: under 3 minutes.");
        } else out.add(eligible ? "Rewards saved. Contract and tier rewards are claimed separately." : "No match rewards: play at least 3 minutes to qualify.");
        return out;
    }
}
