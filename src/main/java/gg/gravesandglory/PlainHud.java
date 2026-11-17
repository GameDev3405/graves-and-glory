package gg.gravesandglory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreResetS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

/**
 * The plain player's battle HUD (plain clients, port plan P3; the P2 spike's HUD): a boss bar (score and clock, or the camp line),
 * an action bar (order, Charge, troops, gold) and a per-player sidebar (points, army), all built from the battle fields the modpack's
 * state_v10 packet carries, with vanilla packets only. The camp's notices, which the modpack shows on its War Table, come to chat in
 * plain words. Refreshed every 10 ticks by PlainClients.
 */
final class PlainHud {
    private PlainHud() {}
    private static final Map<UUID, ServerBossBar> BARS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SIDEBAR_LINES = new ConcurrentHashMap<>();
    private static final Map<UUID, String> LAST_NOTICE = new ConcurrentHashMap<>();

    static Text glyph(int code) { return Text.literal(String.valueOf((char) code)).styled(st -> st.withFont(PlainPack.GLYPHS).withColor(Formatting.WHITE)); }

    static String clock(int seconds) { return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60); }

    /** Drops a player's HUD state (join, leave): his next tick sends a fresh sidebar and boss bar. */
    static void forget(ServerPlayerEntity p) {
        var bar = BARS.remove(p.getUuid());
        if (bar != null) bar.clearPlayers();
        SIDEBAR_LINES.remove(p.getUuid());
        LAST_NOTICE.remove(p.getUuid());
    }

    static void tick(ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        notices(p);
        ServerBossBar bar = BARS.computeIfAbsent(p.getUuid(), k -> new ServerBossBar(Text.empty(), BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        var s = b.squads.get(p.getUuid());
        var pr = b.campaign.profile(p.getUuid());
        List<Text> lines = new ArrayList<>();
        if (b.running && s != null) {
            int[] score = b.mode.ledger(b);
            int clock = b.mode.clock(b);
            var dawn = b.side(0); var dusk = b.side(1);
            bar.setName(Text.literal(dawn.name() + " " + score[0]).formatted(Formatting.AQUA)
                .append(Text.literal("  vs  ").formatted(Formatting.GRAY)).append(Text.literal(score[1] + " " + dusk.name()).formatted(Formatting.LIGHT_PURPLE))
                .append(Text.literal("   " + clock(clock)).formatted(Formatting.WHITE)));
            bar.setColor(s.team == 0 ? BossBar.Color.BLUE : BossBar.Color.PURPLE);
            bar.setPercent(MathHelper.clamp(b.roundSeconds <= 0 ? 1 : clock / (float) b.roundSeconds, 0, 1));
            String order = OrderRules.loadOrder(s.order);
            long chargeLeft = Math.max(0, (s.chargeReadyAt - b.time + 19) / 20);
            long cmdLeft = Math.max(0, OrderRules.R.commandCooldownTicks() - (b.time - s.lastOrder));
            boolean intro = b.time < b.campaign.introUntil;
            MutableText action = Text.empty().append(glyph(PlainPack.orderGlyph(order))).append(Text.literal(" " + PlainClients.label(order) + " · " + OrderRules.formationLabel(PlainClients.shape(p, s))).formatted(Formatting.GOLD))
                .append(Text.literal("  |  Charge " + (chargeLeft > 0 ? "in " + chargeLeft + "s" : "ready")).formatted(chargeLeft > 0 ? Formatting.RED : Formatting.GREEN))
                .append(Text.literal("  |  Troops " + b.fielded(s).count() + "  |  Gold " + s.gold).formatted(Formatting.YELLOW))
                .append(relics(b, s))
                .append(Text.literal(intro ? "  |  Orders open in " + (b.campaign.introUntil - b.time + 19) / 20 + "s" : cmdLeft > 0 ? "  |  wait" : "").formatted(Formatting.GRAY));
            if (b.respawns.containsKey(p.getUuid())) action = Text.literal("Respawning in " + (b.respawns.get(p.getUuid()) - b.time + 19) / 20 + "s").formatted(Formatting.RED);
            p.sendMessage(action, true);
            lines.add(Text.literal("Points").formatted(Formatting.GOLD, Formatting.BOLD));
            for (var c : b.points) {
                String owner = c.owner < 0 ? "neutral" : c.owner == s.team ? "ours" : "theirs";
                Formatting f = c.owner < 0 ? Formatting.GRAY : c.owner == s.team ? Formatting.GREEN : Formatting.RED;
                lines.add(Text.literal(" " + c.spec.id().toUpperCase(Locale.ROOT) + " " + owner + (c.contested ? " (contested)" : c.progress > 0 && c.progress < 100 ? " " + c.progress + "%" : "")).formatted(f));
            }
            lines.add(Text.literal("Army").formatted(Formatting.GOLD, Formatting.BOLD));
            Map<String, Integer> groups = new LinkedHashMap<>();
            for (var u : b.fielded(s).toList()) groups.merge(u.spec.group(), 1, Integer::sum);
            for (var g : groups.entrySet()) lines.add(Text.literal(" " + PlainClients.label(g.getKey()) + " " + g.getValue()).formatted(Formatting.WHITE));
            lines.add(Text.literal("Training " + s.training.size()).formatted(Formatting.GRAY));
        } else {
            bar.setName(Text.literal("Camp: use the compass for a practice battle").formatted(Formatting.GOLD));
            bar.setColor(BossBar.Color.YELLOW);
            bar.setPercent(1);
            if (!LAST_NOTICE.containsKey(p.getUuid()))
                p.sendMessage(Text.literal("Writs " + pr.troopWrits + "  |  Pacts " + pr.pacts + "  |  Book: War Table").formatted(Formatting.YELLOW), true);
            lines.add(Text.literal("Camp").formatted(Formatting.GOLD, Formatting.BOLD));
            lines.add(Text.literal(" Writs " + pr.troopWrits).formatted(Formatting.WHITE));
            lines.add(Text.literal(" Troops owned " + pr.owned.size()).formatted(Formatting.WHITE));
            lines.add(Text.literal(" Wins " + pr.wins + " of " + pr.matches).formatted(Formatting.WHITE));
        }
        sidebar(p, Text.literal("Graves and Glory").styled(st -> st.withFont(PlainPack.TITLE_FONT).withColor(Formatting.GOLD)), lines);
    }

    /** The relic slots' readiness for the action bar (P4: sendRelics reaches modpack players only): "R1 ready", "R2 12s", "R3 on". Empty with no relic set. */
    static MutableText relics(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        MutableText out = Text.empty();
        for (int i = 0; i < s.relicState.size(); i++) {
            String id = Commander.relicIn(s, i);
            if (id == null || id.isEmpty()) continue;
            int ready = Commander.readyIn(b, s, i), active = Commander.activeFor(b, s, i);
            out.append(Text.literal("  |  R" + (i + 1) + " " + (active > 0 ? "on" : ready > 0 ? (ready + 19) / 20 + "s" : "ready")).formatted(active > 0 ? Formatting.AQUA : ready > 0 ? Formatting.RED : Formatting.GREEN));
        }
        return out;
    }

    /** A per-player sidebar: packets to this player only (the server scoreboard is never touched). */
    static void sidebar(ServerPlayerEntity p, Text title, List<Text> lines) {
        var obj = new ScoreboardObjective(new Scoreboard(), "gg_plain", ScoreboardCriterion.DUMMY, title, ScoreboardCriterion.RenderType.INTEGER, false, BlankNumberFormat.INSTANCE);
        Integer had = SIDEBAR_LINES.get(p.getUuid());
        p.networkHandler.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(obj, had == null ? ScoreboardObjectiveUpdateS2CPacket.ADD_MODE : ScoreboardObjectiveUpdateS2CPacket.UPDATE_MODE));
        if (had == null) p.networkHandler.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.SIDEBAR, obj));
        for (int i = 0; i < lines.size(); i++) p.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket("l" + i, "gg_plain", lines.size() - i, Optional.of(lines.get(i)), Optional.empty()));
        for (int i = lines.size(); had != null && i < had; i++) p.networkHandler.sendPacket(new ScoreboardScoreResetS2CPacket("l" + i, "gg_plain"));
        SIDEBAR_LINES.put(p.getUuid(), lines.size());
    }

    /** The camp's current notice to chat once (a refusal, a roll result), in plain words: the modpack shows it on the War Table. */
    static void notices(ServerPlayerEntity p) {
        var b = GravesAndGlory.battle;
        if (b == null) return;
        String n = b.campaign.notices.get(p.getUuid());
        if (n != null && !n.equals(LAST_NOTICE.get(p.getUuid()))) p.sendMessage(Text.literal(PlainText.plain(n)).formatted(Formatting.YELLOW));
        if (n == null) LAST_NOTICE.remove(p.getUuid()); else LAST_NOTICE.put(p.getUuid(), n);
    }
}
