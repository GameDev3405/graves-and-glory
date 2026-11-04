package gg.gravesandglory;

import java.nio.file.*;

/**
 * The autosave policy Kort approved on 2026-11-02: vanilla's periodic overworld autosave is skipped while a battle runs and paid
 * once when the round returns to the lobby; shutdown, BuildMode and the mod's own saves are untouched. Never shipped.
 * The pure policy runs as it is; the Minecraft wiring is checked in the source, where it cannot be run without a game.
 */
public final class ArenaAutosaveCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static Path root() {
        for (Path at = Paths.get("").toAbsolutePath(); at != null; at = at.getParent())
            if (Files.isDirectory(at.resolve("src/main/java/gg/gravesandglory"))) return at;
        throw new AssertionError("run from the repository: src/main/java/gg/gravesandglory not found");
    }

    public static void main(String[] args) throws Exception {
        // The gate.
        ArenaAutosave.reset();
        check(ArenaAutosave.allow(false), "the lobby autosaves as vanilla does");
        check(!ArenaAutosave.owed() && !ArenaAutosave.settle(), "a lobby autosave owes nothing, so the lobby return does not save again");
        check(!ArenaAutosave.allow(true), "a running battle skips the autosave");
        check(ArenaAutosave.owed(), "the skipped autosave is owed");
        check(!ArenaAutosave.allow(true) && ArenaAutosave.owed(), "a long battle skips every autosave and still owes one");
        check(ArenaAutosave.settle(), "the lobby return pays the debt");
        check(!ArenaAutosave.settle() && !ArenaAutosave.owed(), "the debt is paid once");
        check(!ArenaAutosave.allow(true), "the next battle skips again");
        check(ArenaAutosave.allow(false) && !ArenaAutosave.settle(), "a lobby autosave before the return covers the debt");

        Path root = root();
        Path src = root.resolve("src/main/java/gg/gravesandglory");
        String mixin = Files.readString(src.resolve("mixin/AutosaveMixin.java"));
        String campaign = Files.readString(src.resolve("Campaign.java"));
        String build = Files.readString(src.resolve("BuildMode.java"));
        String mixins = Files.readString(root.resolve("src/main/resources/gravesandglory.mixins.json"));

        // Wiring: only MinecraftServer.tick's saveAll is redirected, on the server side of both dedicated and integrated servers.
        check(mixin.contains("@Mixin(MinecraftServer.class)"), "the mixin targets MinecraftServer");
        check(mixin.contains("@Redirect(method=\"tick\",at=@At(value=\"INVOKE\",target=\"Lnet/minecraft/server/MinecraftServer;saveAll(ZZZ)Z\"))"),
            "only the saveAll inside tick (the periodic autosave) is redirected; stop/shutdown and commands keep theirs");
        check(mixin.contains("ArenaAutosave.allow(battle!=null&&battle.running&&battle.server==server)"), "the gate is the battle's running flag");
        check(mixin.contains("return server.saveAll(suppressLogs,flush,force);"), "an allowed autosave runs with vanilla's arguments");
        check(java.util.regex.Pattern.compile("\"mixins\": \\[[^\\]]*\"AutosaveMixin\"").matcher(mixins).find(), "AutosaveMixin is a common mixin (dedicated servers load it)");

        // Wiring: the lobby return pays the debt after the armies are gone; a stopping server leaves it to the shutdown save.
        int lobby = campaign.indexOf("void returnLobby(){"), end = campaign.indexOf("void left(UUID id)", lobby);
        String ret = campaign.substring(lobby, end);
        int paid = ret.indexOf("if(ArenaAutosave.settle()&&b.server.isRunning())");
        check(paid > 0, "returnLobby saves the owed world while the server runs");
        check(paid > ret.indexOf("b.running=false") && paid > ret.indexOf("u.mob.discard()") && paid > ret.indexOf("toLobby(p)"), "the save follows the teardown and the move to camp");
        check(ret.contains("b.server.saveAll(true,false,false)"), "the lobby save is the same save the autosave would have made");

        // BuildMode keeps its full save.
        check(build.contains("server.saveAll(true,true,true);"), "BuildMode still saves the finished map");

        System.out.println("ArenaAutosaveCheck: " + checks + " checks passed");
    }
}
