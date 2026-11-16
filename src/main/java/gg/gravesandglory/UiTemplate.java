package gg.gravesandglory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The UI editor's text template grammar (UI editor design 5.3, phase 1: words). Pure: no Minecraft classes, so the standalone check
 * runs it.
 * <ul>
 *   <li>{name}: a live value the code supplies (a slot). A name is a lower-case letter then letters, digits or underscores.</li>
 *   <li>{key:id}: a key hint (HintText). Kept as written, so the code that expanded it before still expands it after.</li>
 *   <li>{{ and }}: a literal brace.</li>
 *   <li>Anything else is plain text. A lone brace is refused by {@link #problem}; at run time it is drawn as typed.</li>
 * </ul>
 * Formatting codes are not part of the grammar: an edit holding the section sign is refused (UiSheet), so text is never interpreted.
 */
public final class UiTemplate {
    private UiTemplate() {}

    /** One part of a parsed template: literal text, a slot, or a key hint kept as written. */
    public record Part(String text, String slot) {
        public boolean isSlot() { return slot != null; }
    }

    private static final Map<String, List<Part>> CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_CAP = 4096;

    static boolean slotName(String s) {
        if (s.isEmpty() || s.length() > 32) return false;
        char c0 = s.charAt(0);
        if (c0 < 'a' || c0 > 'z') return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    static boolean keyId(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    /** The template split into parts (cached). Never throws: a malformed brace stays text. */
    public static List<Part> parse(String template) {
        List<Part> hit = CACHE.get(template);
        if (hit != null) return hit;
        List<Part> parts = Collections.unmodifiableList(parseNow(template, null));
        if (CACHE.size() > CACHE_CAP) CACHE.clear();
        CACHE.put(template, parts);
        return parts;
    }

    /** Parses; when problems is not null, each malformed brace adds a line there. */
    static List<Part> parseNow(String t, List<String> problems) {
        List<Part> out = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int i = 0, n = t.length();
        while (i < n) {
            char c = t.charAt(i);
            if (c == '{' && i + 1 < n && t.charAt(i + 1) == '{') { text.append('{'); i += 2; continue; }
            if (c == '}' && i + 1 < n && t.charAt(i + 1) == '}') { text.append('}'); i += 2; continue; }
            if (c == '{') {
                int close = t.indexOf('}', i + 1);
                String inside = close < 0 ? null : t.substring(i + 1, close);
                if (inside != null && inside.startsWith("key:") && keyId(inside.substring(4))) {
                    text.append(t, i, close + 1); // a key hint: kept for HintText
                    i = close + 1;
                    continue;
                }
                if (inside != null && slotName(inside)) {
                    if (text.length() > 0) { out.add(new Part(text.toString(), null)); text.setLength(0); }
                    out.add(new Part(null, inside));
                    i = close + 1;
                    continue;
                }
                if (problems != null) problems.add("a '{' at " + (i + 1) + " opens no slot; write {{ for a brace");
                text.append(c); i++;
                continue;
            }
            if (c == '}') {
                if (problems != null) problems.add("a '}' at " + (i + 1) + " closes no slot; write }} for a brace");
                text.append(c); i++;
                continue;
            }
            text.append(c); i++;
        }
        if (text.length() > 0) out.add(new Part(text.toString(), null));
        return out;
    }

    /** The slot names the template uses, in order of first use. */
    public static Set<String> slots(String template) {
        Set<String> out = new LinkedHashSet<>();
        for (Part p : parse(template)) if (p.isSlot()) out.add(p.slot());
        return out;
    }

    /** Whether the template holds a {key:id} hint. */
    public static boolean hasKeyHint(String template) {
        for (Part p : parse(template)) if (!p.isSlot() && p.text().contains("{key:")) return true;
        return false;
    }

    /** The template with each slot filled from the pairs (name, value, name, value...); a slot with no value is drawn as {name}. */
    public static String expand(String template, Object... pairs) {
        List<Part> parts = parse(template);
        if (parts.size() == 1 && !parts.get(0).isSlot()) return parts.get(0).text();
        StringBuilder out = new StringBuilder();
        for (Part p : parts) {
            if (!p.isSlot()) { out.append(p.text()); continue; }
            Object v = value(pairs, p.slot());
            out.append(v == NONE ? "{" + p.slot() + "}" : String.valueOf(v));
        }
        return out.toString();
    }

    private static final Object NONE = new Object();

    static Object value(Object[] pairs, String name) {
        if (pairs == null) return NONE;
        for (int i = 0; i + 1 < pairs.length; i += 2) if (name.equals(pairs[i])) return pairs[i + 1];
        return NONE;
    }

    /** The slot names a call supplies: the even entries of the pairs. */
    public static Set<String> supplied(Object... pairs) {
        Set<String> out = new LinkedHashSet<>();
        if (pairs != null) for (int i = 0; i + 1 < pairs.length; i += 2) if (pairs[i] instanceof String s) out.add(s);
        return out;
    }

    /** Why the template is unusable for a piece offering these slots (and key hints when keysAllowed), or null when it is fine. */
    public static String problem(String template, Set<String> offered, boolean keysAllowed) {
        List<String> problems = new ArrayList<>();
        List<Part> parts = parseNow(template, problems);
        if (!problems.isEmpty()) return problems.get(0);
        for (Part p : parts) {
            if (p.isSlot() && !offered.contains(p.slot())) return "{" + p.slot() + "} is not a live value this line offers" + (offered.isEmpty() ? " (it offers none)" : " (it offers " + braced(offered) + ")");
            if (!p.isSlot() && !keysAllowed && p.text().contains("{key:")) return "this line does not show key hints";
        }
        return null;
    }

    /** The slots the default shows that the edit no longer shows (a deleted live value; allowed, but the editor says so). */
    public static Set<String> dropped(String def, String edit) {
        Set<String> out = new LinkedHashSet<>(slots(def));
        out.removeAll(slots(edit));
        return out;
    }

    public static String braced(Set<String> names) {
        StringBuilder b = new StringBuilder();
        for (String s : names) { if (b.length() > 0) b.append(' '); b.append('{').append(s).append('}'); }
        return b.toString();
    }
}
