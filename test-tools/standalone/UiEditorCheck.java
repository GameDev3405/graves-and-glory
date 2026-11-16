package gg.gravesandglory;

import java.time.Instant;
import java.util.*;

/** The UI editor, phase 1 (words; UI editor design 5.2 to 5.9): the template grammar (UiTemplate), the gate and its layers (UiText),
 *  the validation the server and every client apply (UiSheet.problem, the client's own clamp of an untrusted sheet), the sheet's
 *  JSON, Publish, Roll back, Reset, the history cap, renames, orphans and the bake. Never shipped. */
public final class UiEditorCheck {
    static int checks;
    static void check(boolean pass, String message) { if (!pass) throw new AssertionError(message); checks++; }
    static boolean throwsOn(Runnable r) { try { r.run(); return false; } catch (RuntimeException e) { return true; } }

    static UiSheet.Piece piece(String id, String def, String... slots) { return new UiSheet.Piece(id, def, new LinkedHashSet<>(List.of(slots)), UiTemplate.hasKeyHint(def), ""); }

    public static void main(String[] args) {
        // ---- grammar ----
        check(UiTemplate.expand("Wave {wave} of {total}", "wave", 3, "total", 5).equals("Wave 3 of 5"), "slots fill");
        check(UiTemplate.expand("Plain line").equals("Plain line"), "plain text unchanged");
        check(UiTemplate.expand("a {{b}} c").equals("a {b} c"), "doubled braces are literal");
        check(UiTemplate.expand("Open {key:menu} now").equals("Open {key:menu} now"), "key hints are kept for HintText");
        check(HintText.expand(UiTemplate.expand("Open {key:menu}"), id -> "M").equals("Open M"), "a kept key hint still expands");
        check(UiTemplate.expand("{n}{n}", "n", 7).equals("77"), "a slot may repeat");
        check(UiTemplate.expand("Hi {who}").equals("Hi {who}"), "a slot with no value draws as written");
        check(UiTemplate.expand("50% {x").equals("50% {x"), "a lone brace draws as typed");
        check(UiTemplate.expand("{Wave}").equals("{Wave}"), "an upper-case name is no slot");
        check(UiTemplate.slots("{a} and {b} and {a}").equals(new LinkedHashSet<>(List.of("a", "b"))), "slot names in order");
        check(UiTemplate.problem("{x", Set.of(), false) != null, "a lone brace is refused when typed");
        check(UiTemplate.problem("x}", Set.of(), false) != null, "a lone closing brace is refused");
        check(UiTemplate.problem("{{x}}", Set.of(), false) == null, "escaped braces are fine");
        check(UiTemplate.problem("{wave}", Set.of("total"), false).contains("{wave}"), "an unoffered slot is refused by name");
        check(UiTemplate.problem("{key:menu}", Set.of(), false) != null, "a key hint where the line shows none is refused");
        check(UiTemplate.problem("{key:menu}", Set.of(), true) == null, "a key hint where the line shows them is fine");
        check(UiTemplate.dropped("Wave {wave} of {total}", "Wave {wave}").equals(Set.of("total")), "a deleted slot is reported");

        // ---- validation (5.5) ----
        var wave = piece("hud.wave", "Wave {wave} of {total}", "wave", "total");
        check(UiSheet.problem(wave, "Round {wave}/{total}") == null, "a good edit passes");
        check(UiSheet.problem(wave, "Round {round}") != null, "an unknown slot fails");
        check(UiSheet.problem(wave, "x".repeat(UiSheet.TEXT_CAP + 1)) != null, "the length cap holds");
        check(UiSheet.problem(wave, "§cRed") != null, "formatting codes are refused, so text is never interpreted");
        check(UiSheet.problem(wave, "two\nlines") != null, "control characters are refused");
        check(UiSheet.problem(null, "x") != null, "an unknown piece fails");

        // ---- the gate and its layers ----
        UiText.setBuiltin(Map.of()); UiText.setServer(Map.of()); UiText.setDraft(Map.of()); UiText.previewDraft(false);
        check(UiText.t("hud.wave", "Wave {wave} of {total}", "wave", 1, "total", 4).equals("Wave 1 of 4"), "no edit: the default, filled");
        check(UiText.t("camp.title", "Camp").equals("Camp"), "no edit, no slots");
        UiText.setBuiltin(Map.of("camp.title", "Banner camp"));
        check(UiText.t("camp.title", "Camp").equals("Banner camp"), "a built-in edit wins over the code");
        UiText.setServer(Map.of("camp.title", "War camp", "hud.wave", "Round {wave}/{total}"));
        check(UiText.t("camp.title", "Camp").equals("War camp"), "the published sheet wins over the built-in");
        check(UiText.t("hud.wave", "Wave {wave} of {total}", "wave", 2, "total", 4).equals("Round 2/4"), "a published template keeps its numbers live");
        check(UiText.t("hud.wave", "Wave {wave} of {total}", "wave", 3, "total", 4).equals("Round 3/4"), "and they change with the game");
        // the client never trusts the sheet: a slot the call does not supply, a formatting code, a lone brace -> the default
        UiText.setServer(Map.of("hud.wave", "Round {round}", "camp.title", "§kX", "camp.sub", "50% {"));
        check(UiText.t("hud.wave", "Wave {wave} of {total}", "wave", 2, "total", 4).equals("Wave 2 of 4"), "an untrusted unknown slot falls back to the default");
        check(UiText.t("camp.title", "Camp").equals("Banner camp"), "an untrusted formatting code falls back (to the built-in edit)");
        check(UiText.t("camp.sub", "Sub").equals("Sub"), "an untrusted lone brace falls back");
        UiText.setServer(Map.of("camp.title", "W".repeat(5000)));
        check(UiText.t("camp.title", "Camp").equals("Banner camp"), "an untrusted overlong line falls back");
        // the owner's draft replaces the published sheet only while he previews it
        UiText.setServer(Map.of("camp.title", "War camp"));
        UiText.setDraft(Map.of("camp.title", "Draft camp"));
        check(UiText.t("camp.title", "Camp").equals("War camp"), "a draft is not drawn unless previewed");
        UiText.previewDraft(true);
        check(UiText.t("camp.title", "Camp").equals("Draft camp") && UiText.source("camp.title").equals("draft"), "a previewed draft is drawn");
        UiText.setDraft(Map.of());
        check(UiText.t("camp.title", "Camp").equals("Banner camp"), "a draft without the edit shows the built-in, not the published one");
        UiText.previewDraft(false);
        // recording for the editor
        UiText.recording(true); UiText.nextFrame();
        UiText.t("hud.wave", "Wave {wave} of {total}", "wave", 2, "total", 4);
        UiText.locked("hud.troop.name", "Footman", "the design sheet");
        var seen = UiText.seen("hud.wave");
        check(seen != null && seen.shown.equals("Wave 2 of 4") && seen.slots.equals(Set.of("wave", "total")), "the editor sees a drawn piece with its slots");
        check(UiText.seen("hud.troop.name").locked && UiText.seen("hud.troop.name").source.equals("the design sheet"), "a locked piece says where it comes from");
        UiText.recording(false);
        check(UiText.seen("hud.wave") == null, "recording off forgets");
        UiText.setBuiltin(Map.of()); UiText.setServer(Map.of());

        // ---- the sheet, publish, history (5.4, 5.6, 5.7) ----
        Map<String, UiSheet.Piece> pieces = new LinkedHashMap<>();
        pieces.put("hud.wave", wave);
        pieces.put("camp.title", piece("camp.title", "Camp"));
        UiSheet s = new UiSheet();
        Instant t0 = Instant.parse("2026-11-16T10:00:00Z");
        s.publish(Map.of("camp.title", "War camp"), pieces, "Kort", t0);
        check(s.version == 1 && s.edits.get("camp.title").equals("War camp") && s.history.isEmpty(), "first publish: version 1, nothing before it");
        check(s.who.equals("Kort") && s.summary.contains("1 line"), "the current version says who and what");
        s.publish(Map.of("camp.title", "Camp of war", "hud.wave", "Round {wave}"), pieces, "Kort", t0.plusSeconds(60));
        check(s.version == 2 && s.history.size() == 1 && s.history.get(0).version == 1 && s.history.get(0).edits.get("camp.title").equals("War camp"), "the previous version goes to the history");
        check(s.history.get(0).time.equals(t0.toString()), "a history entry keeps its own publish time");
        check(throwsOn(() -> s.publish(Map.of("hud.wave", "{nope}"), pieces, "Kort", t0)), "publish validates again");
        check(s.version == 2, "a refused publish changes nothing");
        int histBefore = s.history.size();
        check(throwsOn(() -> s.publish(new HashMap<>(s.edits), pieces, "Kort", t0)) && s.version == 2 && s.history.size() == histBefore, "a publish that changes nothing is refused and adds no version");
        Map<String, String> withOrphan = new HashMap<>(s.edits); withOrphan.put("gone.piece", "old");
        s.publish(withOrphan, pieces, "Kort", t0.plusSeconds(120));
        check(s.edits.containsKey("gone.piece") && UiSheet.orphans(s.edits, pieces).equals(List.of("gone.piece")), "an orphan is kept and listed");
        check(!UiSheet.drawable(s.edits, pieces).containsKey("gone.piece"), "an orphan is not drawn");
        s.rollback(1, "Kort", t0.plusSeconds(180));
        check(s.version == 4 && s.edits.equals(Map.of("camp.title", "War camp")) && s.summary.contains("version 1"), "roll back republishes an old version");
        check(throwsOn(() -> s.rollback(99, "Kort", t0)), "rolling back to an unknown version fails");
        s.reset("console", t0.plusSeconds(240));
        check(s.version == 5 && s.edits.isEmpty() && s.history.get(0).version == 4, "reset empties the sheet and keeps the old one in the history");
        for (int i = 0; i < 80; i++) s.publish(Map.of("camp.title", "v" + i), pieces, "Kort", t0.plusSeconds(300 + i));
        check(s.history.size() == UiSheet.HISTORY_CAP, "the history keeps the last " + UiSheet.HISTORY_CAP);
        UiSheet back = UiSheet.parse(s.toJson(true));
        check(back.version == s.version && back.edits.equals(s.edits) && back.history.size() == s.history.size() && back.who.equals("Kort"), "the sheet round-trips its JSON");
        UiSheet sent = UiSheet.parse(s.toJson(false));
        check(sent.history.isEmpty() && sent.edits.equals(s.edits), "players are sent no history");
        check(throwsOn(() -> UiSheet.parse("[1,2]")), "a sheet that is not an object is malformed");
        check(throwsOn(() -> UiSheet.parse("{\"edits\":{\"a.b\":3}}")), "an edit that is not an object is malformed");
        check(throwsOn(() -> UiSheet.parse("{\"format\":99}")), "a newer format is refused, not misread");
        check(UiSheet.parse("{\"edits\":{\"a.b\":{\"text\":\"x\",\"dx\":4}}}").edits.equals(Map.of("a.b", "x")), "fields of a later phase are skipped");

        // ---- pieces, renames, bake (5.8, 5.9) ----
        var parsed = UiSheet.parsePieces("{\"pieces\":[{\"id\":\"a.b\",\"default\":\"Hi {n}\",\"slots\":[\"n\"],\"keys\":false},{\"id\":\"a.c\",\"locked\":true,\"source\":\"design sheet\"}]}");
        check(parsed.size() == 1 && parsed.get("a.b").slots().equals(Set.of("n")), "the piece list parses; locked pieces take no edits");
        check(throwsOn(() -> UiSheet.parsePieces("{\"pieces\":[{\"id\":\"a.b\",\"default\":\"x\"},{\"id\":\"a.b\",\"default\":\"y\"}]}")), "a duplicate id is refused");
        check(UiSheet.renamed(Map.of("old.id", "x"), Map.of("old.id", "new.id")).equals(Map.of("new.id", "x")), "a rename moves the edit");
        check(UiSheet.renamed(Map.of("old.id", "x", "new.id", "y"), Map.of("old.id", "new.id")).equals(Map.of("new.id", "y")), "an edit already under the new id wins");
        var baked = UiSheet.baked(Map.of("camp.title", "Banner camp"), Map.of("hud.wave", "Round {wave}", "camp.title", "Camp"), pieces);
        check(baked.equals(Map.of("hud.wave", "Round {wave}")), "bake: published edits join the built-in ones; one equal to the code default drops out");
        check(UiSheet.differing(Map.of("hud.wave", "Round {wave}", "x.y", "z"), baked).equals(Map.of("x.y", "z")), "after a bake the server keeps only what differs");

        // army control's order lines (Spec orderLines) drawn through the gate: with no edit, word for word the sheet's (refusal, downCard)

        UiText.setBuiltin(Map.of()); UiText.setServer(Map.of()); UiText.setDraft(Map.of());

        for (String code : OrderReply.CODES) check(OrderReply.refusalLine(code, 7, "riders").equals(OrderReply.refusal(code, 7, "riders")), "order line " + code + " matches the sheet");

        check(OrderReply.downCardLine().equals(OrderReply.downCard()), "the down card line matches the sheet");

        System.out.println("UiEditorCheck: " + checks + " checks passed");
    }
}
