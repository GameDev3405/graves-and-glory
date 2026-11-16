package gg.gravesandglory;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Per-viewer text (plain clients, port plan P3.4). A notice template names a key as {key:(id)} (HintText). A modpack client gets
 * Campaign.keyText (a keybind text: its own binding). A plain client has none of the mod's bindings, and a keybind text would show it
 * the raw id (key.gravesandglory.menu), so it gets plain words: the hotbar key or click that does the same thing for it, and phrases
 * that promise a screen it does not have are reworded. The plain pack's language file uses the same words (PlainPack.lang).
 */
public final class PlainText {
    private PlainText() {}

    /** Whole phrases that are not true for a plain player, and what he reads instead (checked before the key words). */
    private static final String[][] PHRASES = {
        {"{key:menu} opens your war council.", "The book in hotbar slot 8 opens Recruit."},
        {"{key:menu} opens your camp guide", "the book in hotbar slot 8 opens Recruit"},
        {"Your war council is ready. Press {key:menu}.", "Your army is ready. Press 9 or use the compass for a practice battle."},
    };

    /** The template as this viewer reads it. */
    public static Text of(String template, ServerPlayerEntity viewer) {
        return VersionGate.plain(viewer) ? Text.literal(plain(template)) : Campaign.keyText(template);
    }

    /** The template in plain words: no {key:...} token and no binding id is left. */
    public static String plain(String template) {
        String t = template;
        for (String[] p : PHRASES) t = t.replace(p[0], p[1]);
        return HintText.expand(t, PlainText::key);
    }

    /** What a plain player presses for a hint id. The hotbar orders are PlainClients.HOTBAR's (slots 1 to 5 are keys 2 to 6). */
    public static String key(String id) {
        return switch (id) {
            case "attack" -> "key 2";
            case "charge" -> "key 3";
            case "hold" -> "key 4";
            case "follow" -> "key 5";
            case "retreat" -> "key 6";
            case "menu" -> "the book (slot 8)";
            case "use", "cancel" -> "the Use Item key"; // vanilla's own action names (Controls), not a default button
            case "confirm", "hit" -> "the Attack key";
            default -> "(modpack key)";
        };
    }
}
