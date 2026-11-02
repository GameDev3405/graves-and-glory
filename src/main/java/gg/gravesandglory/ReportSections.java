package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.*;

/** The battle report's mode sections (plan 4F.2): the result of the mode that was played, as rows of a label and a value on the
 *  report's note (WarTableReport). A mode's section is a function from the report to rows, registered under the mode's id; the
 *  report asks for the section of the id in its mode block (Campaign.reportMode) and falls back to the generic section, which
 *  reads the mode's two ledger numbers whatever they are. A new mode (Survival) adds its section with one register call from its
 *  own code and touches nothing in the report's layout. */
final class ReportSections {
    private ReportSections() {}

    /** One line of a section: what it counts and the figure. */
    record Row(String label, String value) {}

    /** Builds a mode's rows from the report (the whole JSON, mode block included). */
    @FunctionalInterface interface Source { List<Row> rows(JsonObject report); }

    /** The most rows a section may take: the note's room for them (WarTableReport.SECTION_ROWS); a longer list is cut at this. */
    static final int MAX_ROWS = 8;

    private static final Map<String, Source> sections = new LinkedHashMap<>();

    /** Adds (or replaces) the section of the mode with this id. */
    static void register(String modeId, Source source) { sections.put(modeId, source); }

    /** Whether the mode has a section of its own. */
    static boolean has(String modeId) { return sections.containsKey(modeId); }

    static {
        register(Modes.DOMINATION, ReportSections::domination);
        register(Modes.SIEGE, ReportSections::siege);
        register(Modes.CONVOY, ReportSections::convoy);
    }

    /** The mode block of a report, or an empty one for a report from a server that did not send it. */
    static JsonObject mode(JsonObject report) { return report.has("mode") && report.get("mode").isJsonObject() ? report.getAsJsonObject("mode") : new JsonObject(); }

    static String id(JsonObject report) { var m = mode(report); return m.has("id") ? m.get("id").getAsString() : ""; }

    /** The section's title: the mode's name (the Domination of a report with no mode block). */
    static String title(JsonObject report) {
        var m = mode(report);
        if (m.has("name")) return m.get("name").getAsString();
        return Modes.DOMINATION.equals(id(report)) || id(report).isEmpty() ? "Domination" : id(report);
    }

    /** The mode's rule line as the round stated it, or "". */
    static String rule(JsonObject report) { var m = mode(report); return m.has("rule") ? m.get("rule").getAsString() : ""; }

    /** Whether the round played the capture points (the commanders table's On points column shows only then); true when the report does not say. */
    static boolean points(JsonObject report) { var m = mode(report); return !m.has("points") || m.get("points").getAsBoolean(); }

    /** The section's rows, at most MAX_ROWS: the mode's own, or the generic ones. */
    static List<Row> rows(JsonObject report) {
        String id = id(report);
        Source s = sections.get(id.isEmpty() ? Modes.DOMINATION : id);
        List<Row> out = s == null ? generic(report) : s.rows(report);
        return out.size() > MAX_ROWS ? new ArrayList<>(out.subList(0, MAX_ROWS)) : out;
    }

    // ---- a ledger number as the mode reads it ----
    static int number(JsonObject mode, String array, int i, int fallback) {
        if (!mode.has(array) || !mode.get(array).isJsonArray()) return fallback;
        JsonArray a = mode.getAsJsonArray(array);
        return i < a.size() ? a.get(i).getAsInt() : fallback;
    }

    static String format(JsonObject mode, int i) {
        if (!mode.has("format") || !mode.get("format").isJsonArray()) return "plain";
        JsonArray a = mode.getAsJsonArray("format");
        return i < a.size() ? a.get(i).getAsString() : "plain";
    }

    /** Ledger number i in its format (ModeMath.ledgerText: 1/3, 64%, or 12). */
    static String ledger(JsonObject report, int i) {
        var m = mode(report);
        return ModeMath.ledgerText(format(m, i), number(m, "ledger", i, 0), number(m, "max", i, 0));
    }

    // ---- the sections ----
    /** Domination: each side's score toward the target, then the seconds each side held each point. */
    static List<Row> domination(JsonObject report) {
        var m = mode(report);
        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Dawn score", number(m, "ledger", 0, 0) + " of " + number(m, "max", 0, 0)));
        rows.add(new Row("Dusk score", number(m, "ledger", 1, 0) + " of " + number(m, "max", 1, 0)));
        if (report.has("points")) for (var e : report.getAsJsonArray("points")) {
            var p = e.getAsJsonObject();
            rows.add(new Row(p.get("name").getAsString(), p.get("dawn").getAsInt() + "s / " + p.get("dusk").getAsInt() + "s"));
        }
        return rows;
    }

    /** Siege: how many stages fell and the strength left in the stage under attack. */
    static List<Row> siege(JsonObject report) {
        return List.of(new Row("Stages taken", ledger(report, 0)), new Row("Stage strength", ledger(report, 1)));
    }

    /** Convoy: how far the wagon came by road and how many stops the defence still held. */
    static List<Row> convoy(JsonObject report) {
        var m = mode(report);
        return List.of(new Row("Escort progress", ledger(report, 0)), new Row("Stops held", number(m, "ledger", 1, 0) + " of " + number(m, "max", 1, 0)));
    }

    /** A mode without a section of its own: its two ledger numbers in their formats. */
    static List<Row> generic(JsonObject report) {
        return List.of(new Row("Dawn", ledger(report, 0)), new Row("Dusk", ledger(report, 1)));
    }
}
