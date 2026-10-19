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

    /** A key's name as a chip shows it: the mouse buttons as LMB, RMB and MMB, any other name unchanged. */
    public static String chip(String keyName) {
        return switch (keyName) {
            case "Left Button" -> "LMB";
            case "Right Button" -> "RMB";
            case "Middle Button" -> "MMB";
            default -> keyName;
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
