package gg.gravesandglory.cmdui;

import gg.gravesandglory.BattleHud;
import gg.gravesandglory.CommandSelection;
import gg.gravesandglory.CommandUiChecks;
import gg.gravesandglory.GravesAndGloryClient;
import gg.gravesandglory.KeyMigration;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * The command keys' one-time move (KeyMigration): an options.txt from before the Z X C V B keys (Attack G, Follow me V; the harness seeds one
 * when GRAVES_KEYS_SEEDED=1) comes up on Z and C, written back to options.txt, with the marker left; a later deliberate G is never touched
 * again (the marker is there); with no marker the move runs once more and only on an old default. Shots keys-migrated at 854, 1920 and 2560
 * of the hint slip and the open pennant panel. Never included in releases.
 */
public final class KeyMigrateSuite implements CommandUiChecks.Suite {
    Path shots;
    final List<String> shotLog = new ArrayList<>();
    String startup = "";
    boolean startupOk;

    @Override public int limit() { return 700; }

    static KeyBinding find(MinecraftClient c, String key) { for (KeyBinding k : c.options.allKeys) if (k.getTranslationKey().equals(key)) return k; return null; }
    static String bound(MinecraftClient c, String key) { var k = find(c, key); return k == null ? "none" : k.getBoundKeyTranslationKey(); }

    @Override public void tick(MinecraftClient c, int tick) {
        if (tick == 1) {
            shots = FabricLoader.getInstance().getGameDir().resolve("command-ui").resolve("shots");
            try { Files.createDirectories(shots); } catch (Exception ignored) { }
            boolean seeded = "1".equals(System.getenv("GRAVES_KEYS_SEEDED"));
            String opts = "";
            try { opts = Files.readString(c.runDirectory.toPath().resolve("options.txt"), StandardCharsets.UTF_8); } catch (Exception ignored) { }
            boolean onDisk = opts.contains("key_key.gravesandglory.attack:key.keyboard.z") && opts.contains("key_key.gravesandglory.follow:key.keyboard.c");
            boolean live = bound(c, "key.gravesandglory.attack").equals("key.keyboard.z") && bound(c, "key.gravesandglory.follow").equals("key.keyboard.c");
            boolean marker = Files.exists(KeyMigration.marker());
            startupOk = live && marker && KeyMigration.runs == 1 && (!seeded || (KeyMigration.moved.containsAll(List.of("attack", "follow")) && onDisk));
            startup = "seeded G/V=" + seeded + ": at startup the move ran " + KeyMigration.runs + "x, moved " + KeyMigration.moved + "; Attack " + bound(c, "key.gravesandglory.attack") + ", Follow me " + bound(c, "key.gravesandglory.follow")
                + ", options.txt on Z/C=" + onDisk + ", marker " + marker;
            // a later deliberate G: the marker stands, nothing moves
            var attack = find(c, "key.gravesandglory.attack");
            c.options.setKeyCode(attack, InputUtil.fromTranslationKey("key.keyboard.g")); KeyBinding.updateKeysByCode();
            boolean again = KeyMigration.run(c);
            boolean kept = bound(c, "key.gravesandglory.attack").equals("key.keyboard.g") && !again;
            // with no marker (a temp one), the move runs and takes only the old default
            Path temp = FabricLoader.getInstance().getGameDir().resolve("keys-migrate-check.done");
            try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
            boolean ran = KeyMigration.run(c, temp);
            boolean fresh = ran && bound(c, "key.gravesandglory.attack").equals("key.keyboard.z") && bound(c, "key.gravesandglory.follow").equals("key.keyboard.c") && KeyMigration.moved.equals(List.of("attack")) && Files.exists(temp);
            try { Files.deleteIfExists(temp); } catch (Exception ignored) { }
            CommandUiChecks.report("cu-keys-migrate", startupOk && kept && fresh, startup + "; a deliberate G after it: run again changed=" + again + ", Attack still G=" + kept
                + "; no marker: changed=" + ran + ", moved " + KeyMigration.moved + ", Attack " + bound(c, "key.gravesandglory.attack") + ", Follow me " + bound(c, "key.gravesandglory.follow"));
            CommandUiChecks.seatAndStart(c);
        }
        if (tick == 40) {
            if (!GravesAndGloryClient.battleActive()) { CommandUiChecks.report("cu-keys-labels", false, "no battle after seatAndStart"); CommandUiChecks.finish(c); return; }
            CommandSelection.reset();
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
        }
        int[][] sizes = {{60, 1920, 1080}, {100, 854, 480}, {140, 2560, 1440}};
        for (int[] s : sizes) {
            if (tick == s[0] - 18 && s[0] != 60) { CommandSelection.setPanelOpen(false); GLFW.glfwSetWindowSize(c.getWindow().getHandle(), s[1], s[2]); }
            if (tick == s[0]) shot(c, "keys-hints-" + s[1] + ".png");
            if (tick == s[0] + 2) CommandUiChecks.press(c, PanelSuite.find(c, "key.gravesandglory.menu"));
            if (tick == s[0] + 10) shot(c, "keys-panel-" + s[1] + ".png");
        }
        if (tick == 160) {
            String a = GravesAndGloryClient.hint("attack"), f = GravesAndGloryClient.hint("follow"), r = GravesAndGloryClient.hint("retreat");
            var rows = BattleHud.hintRows();
            boolean ok = a.equals("Z") && f.equals("C") && r.equals("V") && !a.equals(r) && !f.equals(r);
            CommandUiChecks.report("cu-keys-labels", ok, "labels after the move: Attack " + a + ", Follow me " + f + ", Retreat " + r + " (want Z, C, V, none shared); hint rows " + rows + "; shots " + String.join(", ", shotLog));
            CommandSelection.setPanelOpen(false);
            GLFW.glfwSetWindowSize(c.getWindow().getHandle(), 1920, 1080);
            CommandUiChecks.finish(c);
        }
    }

    void shot(MinecraftClient c, String name) {
        try (NativeImage img = ScreenshotRecorder.takeScreenshot(c.getFramebuffer())) {
            img.writeTo(shots.resolve(name));
            shotLog.add(name + " " + img.getWidth() + "x" + img.getHeight());
        } catch (Exception e) { shotLog.add(name + " FAILED " + e); }
    }

    @Override public void cleanup(MinecraftClient c) { CommandSelection.reset(); }
}
