package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Key hints from the player's own bindings (UI spec 2.4): a template names an action's key as {key:(id)}, and each client fills
 *  in its own binding, so a rebound or unbound key reads right everywhere, server notices and the camp sign included. Pure, so
 *  the standalone check runs it. */
public final class HintText {
    private HintText() {}

    /** A well-formed token: {key:} round an id of lower-case letters, digits and underscores. Anything else stays text. */
    private static final Pattern TOKEN = Pattern.compile("[{]key:([a-z0-9_]+)[}]");

    /** The template with each {key:(id)} replaced by keyName.apply(id); text that is not a well-formed token (an unclosed
     *  "{key:menu", say) stays as it is. */
    public static String expand(String template, Function<String,String> keyName) {
        List<String> parts = tokens(template);
        StringBuilder out = new StringBuilder(parts.get(0));
        for (int i = 1; i < parts.size(); i += 2) out.append(keyName.apply(parts.get(i))).append(parts.get(i + 1));
        return out.toString();
    }

    /** The template split into text and key ids, alternating: text, id, text, ... and so always of odd length. A text may be
     *  empty ("{key:map}" is "", "map", ""); a malformed token stays inside its text. */
    public static List<String> tokens(String template) {
        List<String> parts = new ArrayList<>();
        Matcher m = TOKEN.matcher(template);
        int at = 0;
        while (m.find()) {
            parts.add(template.substring(at, m.start()));
            parts.add(m.group(1));
            at = m.end();
        }
        parts.add(template.substring(at));
        return parts;
    }

    /** A mouse button's chip by its code (GLFW's, as InputUtil.Type.MOUSE numbers them): LMB, RMB and MMB for buttons 0, 1 and 2
     *  in every language; null for the rest, which keep the game's own name. */
    public static String mouseChip(int button) {
        return switch (button) {
            case 0 -> "LMB";
            case 1 -> "RMB";
            case 2 -> "MMB";
            default -> null;
        };
    }

    /** A mouse button's short name for a crowded key row: its chip, else MB4, MB5 and on (button 3 is the fourth). */
    public static String mouseShort(int button) {
        String chip = mouseChip(button);
        return chip != null ? chip : "MB" + (button + 1);
    }

    /** An unbound key's short name for a crowded key row; uncrowded, the game's own words for it. */
    public static final String UNBOUND_SHORT = "None";

    /** A key's short name where it is shorter than its full one (in characters), else the full name: a printable key's name
     *  is short already (the keypad's 4 reads "4" in game, shorter than Num4). */
    public static String shorter(String full, String shortName) {
        return shortName != null && shortName.length() < full.length() ? shortName : full;
    }

    /** A keyboard key's short name for a crowded key row, by its GLFW code (UI Task 8a, the Task 3 re-review's thin trims: Page Down
     *  read "Pag..."): PgUp, RShift, Num4 and the like for the long names; null for a key whose name is short already. */
    public static String keyShort(int code) {
        if (code >= 320 && code <= 329) return "Num" + (code - 320);
        return switch (code) {
            case 256 -> "Esc";
            case 259 -> "Bksp";
            case 260 -> "Ins";
            case 261 -> "Del";
            case 262 -> "Right";
            case 263 -> "Left";
            case 264 -> "Down";
            case 265 -> "Up";
            case 266 -> "PgUp";
            case 267 -> "PgDn";
            case 280 -> "Caps";
            case 281 -> "ScrLk";
            case 282 -> "NumLk";
            case 283 -> "PrtSc";
            case 330 -> "Num.";
            case 331 -> "Num/";
            case 332 -> "Num*";
            case 333 -> "Num-";
            case 334 -> "Num+";
            case 335 -> "NumEnt";
            case 336 -> "Num=";
            case 340 -> "LShift";
            case 341 -> "LCtrl";
            case 342 -> "LAlt";
            case 343 -> "LWin";
            case 344 -> "RShift";
            case 345 -> "RCtrl";
            case 346 -> "RAlt";
            case 347 -> "RWin";
            default -> null;
        };
    }

    /** The translation key of the binding behind a hint id: key.attack for confirm and hit (an aimed order confirms on the attack
     *  button), key.use for cancel and use, and key.gravesandglory.(id) for the mod's own (menu, map, hints...). */
    public static String bindingKey(String id) {
        return switch (id) {
            case "confirm", "hit" -> "key.attack";
            case "cancel", "use" -> "key.use";
            default -> "key.gravesandglory." + id;
        };
    }

    /** Whether a binding is one bindingKey names (key.attack, key.use or the mod's own key.gravesandglory.(id)), whose keybind
     *  texts KeybindTextMixin names as the chips do. */
    public static boolean hinted(String bindingKey) {
        return bindingKey.equals("key.attack") || bindingKey.equals("key.use") || bindingKey.startsWith("key.gravesandglory.");
    }
}
