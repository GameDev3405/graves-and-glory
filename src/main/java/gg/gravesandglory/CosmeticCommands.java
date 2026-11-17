package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * /gg cosmetics (cosmetics render plan 7), under the /gg owner gate (AdminCommands.allowed). An online player only, by name:
 * list [player] (owned and equipped), grant player id|all, revoke player id (debug), equip player slot id|none, title player title|none.
 * Each change is saved and sent to the player's modded trackers (CosmeticSync.changed); a failed save puts the profile back.
 */
final class CosmeticCommands {
    private CosmeticCommands() {}

    /** The cosmetics node AdminCommands hangs under /gg. */
    static LiteralArgumentBuilder<ServerCommandSource> node() {
        return CommandManager.literal("cosmetics")
            .then(CommandManager.literal("list").executes(c -> list(c.getSource(), c.getSource().getPlayer()))
                .then(CommandManager.argument("player", StringArgumentType.word()).executes(c -> list(c.getSource(), player(c)))))
            .then(CommandManager.literal("grant").then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("id", StringArgumentType.word())
                .executes(c -> change(c, p -> grant(p, StringArgumentType.getString(c, "id")))))))
            .then(CommandManager.literal("revoke").then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("id", StringArgumentType.word())
                .executes(c -> change(c, p -> revoke(p, StringArgumentType.getString(c, "id")))))))
            .then(CommandManager.literal("equip").then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("slot", StringArgumentType.word()).then(CommandManager.argument("id", StringArgumentType.word())
                .executes(c -> change(c, p -> Cosmetics.equip(p.cosmetics, p.cosmeticSlots, StringArgumentType.getString(c, "slot"), StringArgumentType.getString(c, "id"))))))))
            .then(CommandManager.literal("title").then(CommandManager.argument("player", StringArgumentType.word()).then(CommandManager.argument("title", StringArgumentType.greedyString())
                .executes(c -> change(c, p -> title(p, StringArgumentType.getString(c, "title")))))));
    }

    static ServerPlayerEntity player(CommandContext<ServerCommandSource> c) {
        return c.getSource().getServer().getPlayerManager().getPlayer(StringArgumentType.getString(c, "player"));
    }

    static int list(ServerCommandSource s, ServerPlayerEntity target) {
        var b = GravesAndGlory.battle;
        if (b == null || target == null) return AdminCommands.say(s, "No such online player.");
        Profile p = b.campaign.profile(target.getUuid());
        return AdminCommands.say(s, target.getName().getString() + ": owns " + (p.cosmetics.isEmpty() ? "none" : String.join(", ", p.cosmetics))
            + "; wears " + (p.cosmeticSlots.isEmpty() ? "none" : p.cosmeticSlots.toString()) + "; title " + p.progression.title + ".");
    }

    /** Runs an edit on an online player's profile, saves it (a failed save restores it) and sends the new set; says the edit's result. */
    static int change(CommandContext<ServerCommandSource> c, java.util.function.Function<Profile, String> edit) {
        var b = GravesAndGlory.battle;
        ServerPlayerEntity target = player(c);
        if (b == null || target == null) return AdminCommands.say(c.getSource(), "No such online player.");
        UUID id = target.getUuid();
        Profile p = b.campaign.profile(id);
        String before = Campaign.JSON.toJson(p);
        String result = edit.apply(p);
        p.cosmeticSlots = Cosmetics.normalize(p.cosmetics, p.cosmeticSlots);
        if (!b.campaign.save(id)) { b.campaign.profiles.put(id, Profile.restored(Campaign.JSON.fromJson(before, Profile.class))); return AdminCommands.say(c.getSource(), "Not saved; nothing changed."); }
        CosmeticSync.changed(target);
        CosmeticBanners.dress(b, target, p);
        CosmeticBanners.replant(b, target.getUuid());
        return AdminCommands.say(c.getSource(), target.getName().getString() + ": " + result);
    }

    static String grant(Profile p, String id) {
        if (id.equals("all")) { for (var i : Cosmetics.CATALOGUE) if (p.cosmetics.add(i.id())) proven(p, i.id()); return "granted every cosmetic."; }
        if (Cosmetics.item(id) == null) return "unknown cosmetic " + id + ".";
        if (!p.cosmetics.add(id)) return "already owns " + Cosmetics.name(id) + ".";
        proven(p, id);
        return "granted " + Cosmetics.name(id) + ".";
    }

    /** A newly granted Proven skin goes on, as an earned one does (CosmeticGrants.apply). */
    static void proven(Profile p, String id) {
        if (id.equals("skin_proven")) p.cosmeticSlots.put(Cosmetics.SKIN, id);
    }

    static String revoke(Profile p, String id) {
        return p.cosmetics.remove(id) ? "revoked " + id + " (debug: a profile still past its threshold earns it again on the next load)." : "does not own " + id + ".";
    }

    static String title(Profile p, String title) {
        String t = title.equals(Cosmetics.NONE) ? Cosmetics.DEFAULT_TITLE : title;
        if (!p.progression.titles.contains(t)) return "does not own the title " + t + ".";
        p.progression.title = t;
        return "title set to " + t + ".";
    }
}
