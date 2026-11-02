package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A battle mode's short ledger and briefing copy as plain data (plan 2C.2). The ledger is the 2 to 4 numbers that decide the round, in
 * the order of the modeLedger rows; the HUD and the report read the same list, so neither keeps its own labels. The briefing is the
 * how-to lines of the modes row. All of it is draft copy in the sheets. Pure (no Minecraft types), so FeedFitCheck runs it.
 * Survival adds its modeLedger rows and overrides BattleMode.ledgerEntries; nothing here names a mode.
 */
public final class ModeLines {
    private ModeLines() {}

    /** One ledger number: its row's id, label and format, the team it counts for (-1 both), and the value and its ceiling (max 0 when
     *  it has none). */
    public record Entry(String id, String label, int team, int value, int max, String format) {
        /** The number as the HUD and the report write it: "120/300", "64%", "1:05" or "12". */
        public String text() { return ModeMath.ledgerText(format, value, max); }
    }

    /** The modeLedger rows of a mode, by slot. */
    public static List<Spec.ModeLedgerRow> rows(String mode) {
        var out = new ArrayList<Spec.ModeLedgerRow>();
        for (var r : Spec.MODELEDGER) if (r.mode().equals(mode)) out.add(r);
        out.sort((a, b) -> Integer.compare(a.slot(), b.slot()));
        return out;
    }

    /** The entry of a modeLedger row (by its id) holding value out of max; an unknown id is a programming error. */
    public static Entry entry(String rowId, int value, int max) {
        for (var r : Spec.MODELEDGER) if (r.id().equals(rowId)) return new Entry(r.id(), r.label(), r.team(), value, max, r.format());
        throw new IllegalArgumentException("No modeLedger row " + rowId);
    }

    /** A mode's briefing how-to lines for a side ("defend" reads howToDefend, anything else howTo): the modes row's parts, raw, so a
     *  {key:id} token is still in them (see the three-argument form). */
    public static List<String> briefing(String mode, String side) {
        for (var m : Spec.MODES) if (m.id().equals(mode)) return ModeMath.howTo(side.equals("defend") ? m.howToDefend() : m.howTo());
        throw new IllegalArgumentException("No modes row for " + mode);
    }

    /** The briefing as one player reads it: each {key:id} filled with the player's own binding (HintText.expand). */
    public static List<String> briefing(String mode, String side, Function<String,String> keyName) {
        var out = new ArrayList<String>();
        for (String line : briefing(mode, side)) out.add(HintText.expand(line, keyName));
        return out;
    }
}
