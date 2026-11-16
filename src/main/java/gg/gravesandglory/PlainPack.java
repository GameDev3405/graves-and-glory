package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

/**
 * The plain clients' server resource pack (port plan P3, from the P2 spike): the mod's sounds, its fonts, the five House crests and
 * five order icons as a glyph font (U+E000.. crests, U+E010.. orders), the order icons as paper models, and a language file that
 * names every key.gravesandglory.* binding the way a plain player gives that order (PlainText.key), so a shared keybind text (a camp
 * lesson board, say) never shows a raw binding id. Polymer builds the zip (PolymerBridge); PropPack serves it, to plain players only.
 * It holds no mob textures.
 */
public final class PlainPack {
    private PlainPack() {}
    public static final String MOD = "gravesandglory";
    public static final Identifier GLYPHS = Identifier.of(MOD, "plain"), TITLE_FONT = Identifier.of(MOD, "title");
    /** The hotbar orders in slot order (PlainClients.HOTBAR: slots 1 to 5, keys 2 to 6). */
    public static final String[] ORDERS = {"attack", "charge", "hold", "follow", "retreat"};
    static final String[] ICON_FILES = {"icon_attack", "icon_charge", "icon_formation", "icon_follow", "icon_retreat"};
    static final String[] HOUSES = {"gilded", "graveborn", "ironclad", "tidecallers", "wildmoot"};

    static int houseGlyph(String house) {
        for (int i = 0; i < HOUSES.length; i++) if (HOUSES[i].equals(house)) return 0xE000 + i;
        return -1;
    }
    static int orderGlyph(String order) {
        for (int i = 0; i < ORDERS.length; i++) if (ORDERS[i].equals(order)) return 0xE010 + i;
        return 0xE010;
    }

    /** Adds the pack's files (path in the zip, bytes); called by Polymer while it builds the pack. */
    public static void addFiles(BiConsumer<String, byte[]> add) {
        var root = FabricLoader.getInstance().getModContainer(MOD).orElseThrow().findPath("assets/" + MOD).orElseThrow();
        int n = 0;
        try {
            for (String sub : new String[]{"sounds", "font", "textures/gui/house", "textures/gui/command"}) {
                try (var walk = Files.walk(root.resolve(sub))) {
                    for (Path f : walk.filter(Files::isRegularFile).toList()) {
                        add.accept("assets/" + MOD + "/" + root.relativize(f).toString().replace('\\', '/'), Files.readAllBytes(f));
                        n++;
                    }
                }
            }
            add.accept("assets/" + MOD + "/sounds.json", Files.readAllBytes(root.resolve("sounds.json")));
            for (int i = 0; i < ORDERS.length; i++) {
                add.accept("assets/" + MOD + "/textures/item/plain_" + ORDERS[i] + ".png", Files.readAllBytes(root.resolve("textures/gui/command/" + ICON_FILES[i] + ".png")));
                add.accept("assets/" + MOD + "/models/item/plain_" + ORDERS[i] + ".json",
                    ("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + MOD + ":item/plain_" + ORDERS[i] + "\"}}").getBytes(StandardCharsets.UTF_8));
            }
            add.accept("assets/" + MOD + "/lang/en_us.json", lang(Files.readString(root.resolve("lang/en_us.json"), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[Graves and Glory] Plain pack: could not read the mod's assets: " + e);
        }
        StringBuilder font = new StringBuilder("{\"providers\":[");
        for (int i = 0; i < HOUSES.length; i++) font.append(provider("gui/house/" + HOUSES[i] + ".png", (char) (0xE000 + i))).append(',');
        for (int i = 0; i < ORDERS.length; i++) font.append(provider("gui/command/" + ICON_FILES[i] + ".png", (char) (0xE010 + i))).append(i + 1 < ORDERS.length ? "," : "");
        font.append("]}");
        add.accept("assets/" + MOD + "/font/plain.json", font.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("[Graves and Glory] Plain pack: " + n + " mod asset files, glyph font, " + ORDERS.length + " order icons, plain key names");
    }

    /** The pack's en_us.json: each key.gravesandglory.(id) of the mod's language file, named as a plain player gives it. */
    static String lang(String modLang) {
        JsonObject out = new JsonObject();
        for (String k : JsonParser.parseString(modLang).getAsJsonObject().keySet())
            if (k.startsWith("key.gravesandglory.")) out.addProperty(k, PlainText.key(k.substring("key.gravesandglory.".length())));
        return out.toString();
    }

    static String provider(String file, char c) {
        return "{\"type\":\"bitmap\",\"file\":\"" + MOD + ":" + file + "\",\"ascent\":8,\"height\":9,\"chars\":[\"" + String.format("\\u%04X", (int) c) + "\"]}";
    }
}
