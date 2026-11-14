package gg.gravesandglory;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

/** Phase A of the admin tools (2026-11-14 spec section 4): the join ledger (first seen, first match, written once), admin.json (OG window, added, removed, owners), the derived OG rule, the owner gate and the file robustness. Never shipped. */
public final class AdminOgCheck {
    static int checks;
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
        checks++;
    }
    static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a"), B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    static final long T0 = Instant.parse("2026-12-01T00:00:00Z").toEpochMilli(), T1 = Instant.parse("2026-12-31T23:59:59Z").toEpochMilli();

    static AdminOg.Config window(String start, String end) {
        var c = new AdminOg.Config();
        c.ogWindow = new AdminOg.Window();
        c.ogWindow.start = start; c.ogWindow.end = end;
        return c;
    }
    static boolean badCopy(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) { return s.anyMatch(p -> p.getFileName().toString().contains(".bad")); }
    }
    static void clearBad(Path dir) throws Exception {
        try (Stream<Path> s = Files.list(dir)) { for (Path p : (Iterable<Path>) s.filter(p -> p.getFileName().toString().contains(".bad"))::iterator) Files.delete(p); }
    }

    public static void main(String[] args) throws Exception {
        // Derived status.
        var w = window("2026-12-01T00:00:00Z", "2026-12-31T23:59:59Z");
        check(AdminOg.isOg(w, A, T0), "first match exactly at the start is inside");
        check(AdminOg.isOg(w, A, T1), "first match exactly at the end is inside");
        check(!AdminOg.isOg(w, A, T0 - 1), "one ms before the start is outside");
        check(!AdminOg.isOg(w, A, T1 + 1), "one ms after the end is outside");
        check(!AdminOg.isOg(w, A, 0), "stamp 0 (no match yet) is never inside");
        check(!AdminOg.isOg(new AdminOg.Config(), A, T0), "no window: not OG by date");
        var half = window("2026-12-01T00:00:00Z", null);
        check(!AdminOg.isOg(half, A, T0 + 5), "a window with only one bound counts as no window");
        w.ogAdded.add(A.toString());
        check(AdminOg.isOg(w, A, 0), "added counts with no match");
        check(AdminOg.isOg(w, A, T0 - 1_000_000), "added counts outside the window");
        var noWindow = new AdminOg.Config(); noWindow.ogAdded.add(A.toString());
        check(AdminOg.isOg(noWindow, A, 0), "no window: added still counts");
        var rm = window("2026-12-01T00:00:00Z", "2026-12-31T23:59:59Z"); rm.ogRemoved.add(B.toString());
        check(!AdminOg.isOg(rm, B, T0 + 1) && AdminOg.isOg(rm, A, T0 + 1), "removed blocks the date rule for that player only");
        rm.ogAdded.add(B.toString());
        check(AdminOg.isOg(rm, B, 0), "added wins over removed");
        check(AdminOg.reason(rm, B, 0).startsWith("OG") && AdminOg.reason(window("2026-12-01T00:00:00Z", "2026-12-31T23:59:59Z"), A, 0).contains("no match"), "reasons name the case");

        // Add and remove edit both lists so the last command wins.
        var e = new AdminOg.Config();
        e.remove(A); e.add(A);
        check(e.ogAdded.equals(List.of(A.toString())) && e.ogRemoved.isEmpty(), "add after remove");
        e.add(A);
        check(e.ogAdded.size() == 1, "add twice keeps one");
        e.remove(A);
        check(e.ogAdded.isEmpty() && e.ogRemoved.equals(List.of(A.toString())), "remove after add");

        // Window parsing.
        check(AdminOg.parseBound("2026-12-01T00:00:00Z", false).equals(Instant.parse("2026-12-01T00:00:00Z")), "instant parses");
        check(AdminOg.parseBound("2026-12-01", false).equals(Instant.parse("2026-12-01T00:00:00Z")), "a date start is the start of that UTC day");
        check(AdminOg.parseBound("2026-12-31", true).equals(Instant.parse("2026-12-31T23:59:59.999Z")), "a date end is the end of that UTC day");
        check(AdminOg.parseBound("soon", false) == null && AdminOg.parseBound("", true) == null, "garbage is null");

        // Owner gate: console, or an owner uuid. Never the permission level.
        var o = new AdminOg.Config();
        check(AdminOg.mayAdmin(o, true, null), "console is always allowed");
        check(!AdminOg.mayAdmin(o, false, A), "no owners: a player is refused");
        o.owners.add(A.toString());
        check(AdminOg.mayAdmin(o, false, A) && !AdminOg.mayAdmin(o, false, B), "only listed owners");
        check(!AdminOg.mayAdmin(o, false, null), "a non-player non-console source is refused");

        Path dir = Files.createTempDirectory("adminog");
        Path sub = dir.resolve("sub");
        Path file = sub.resolve("admin.json");
        // File robustness.
        var missing = AdminOg.load(file);
        check(missing.ogAdded.isEmpty() && missing.owners.isEmpty() && missing.ogWindow == null, "missing file is empty");
        w.owners.add(B.toString());
        AdminOg.save(file, w);
        var back = AdminOg.load(file);
        check(back.ogAdded.equals(w.ogAdded) && back.owners.equals(w.owners) && back.ogWindow.start.equals("2026-12-01T00:00:00Z") && AdminOg.isOg(back, A, 0), "round trip");
        check(!Files.exists(sub.resolve("admin.json.tmp")), "no temp left");
        for (String bad : new String[] {"{not json", "[]", "{\"ogAdded\":\"x\"}", "{\"ogAdded\":[\"nope\"]}", "{\"ogWindow\":{\"start\":\"x\",\"end\":\"y\"}}", "{\"owners\":[1]}"}) {
            Files.writeString(file, bad);
            var c = AdminOg.load(file);
            check(c.ogAdded.isEmpty() && c.owners.isEmpty() && c.ogWindow == null, "malformed reads as empty: " + bad);
            check(badCopy(sub), "a .bad copy is kept: " + bad);
            check(Files.readString(file).equals(bad), "load never rewrites the malformed file: " + bad);
            clearBad(sub);
        }
        Files.writeString(file, "");
        check(AdminOg.load(file).ogAdded.isEmpty() && !badCopy(sub), "an empty file is empty, nothing to keep");
        Files.writeString(file, "{\"ogAdded\":[\"" + A + "\"],\"extra\":5}");
        check(AdminOg.load(file).ogAdded.equals(List.of(A.toString())), "unknown keys are ignored");

        // Ledger: stamps written once.
        Path lf = dir.resolve("joins.json");
        var l = JoinLedger.load(lf);
        check(l.entries.isEmpty(), "missing ledger is empty");
        check(l.seen(A, "Alice", 1000), "first sight stamps");
        check(!l.seen(A, "Alice2", 2000) && l.get(A).firstSeenAt == 1000, "second sight keeps the stamp");
        check(l.get(A).lastName.equals("Alice2"), "the name follows the player");
        check(!l.seen(B, "Bob", 0) && l.get(B) == null, "a zero clock never stamps");
        check(l.matched(A, "Alice2", 5000) && l.get(A).firstMatchAt == 5000, "first match stamps");
        check(!l.matched(A, "Alice2", 9000) && !l.matched(A, "Alice2", 1) && l.get(A).firstMatchAt == 5000, "match stamp never changes, also if the clock goes back");
        check(l.matched(B, "Bob", 7000) && l.get(B).firstSeenAt == 0 && l.get(B).firstMatchAt == 7000, "a match without a seen stamp still stamps (existing testers)");
        check(l.seen(B, "Bob", 8000) && l.get(B).firstSeenAt == 8000 && l.get(B).firstMatchAt == 7000, "seen after match fills only the seen stamp");
        l.save(lf);
        var l2 = JoinLedger.load(lf);
        check(l2.get(A).firstSeenAt == 1000 && l2.get(A).firstMatchAt == 5000 && l2.get(B).lastName.equals("Bob"), "ledger round trip");
        check(!Files.exists(dir.resolve("joins.json.tmp")), "no ledger temp left");
        check(A.equals(l2.byName("alice2")) && B.equals(l2.byName("BOB")) && l2.byName("nobody") == null, "names resolve offline, case-insensitive");
        check(!l2.matched(A, "x", 99999), "reload keeps stamps");
        Files.writeString(lf, "{broken");
        var l3 = JoinLedger.load(lf);
        check(l3.entries.isEmpty() && Files.readString(lf).equals("{broken") && Files.exists(dir.resolve("joins.json.bad")), "a malformed ledger reads empty, keeps a .bad copy and is not rewritten by load");
        Files.writeString(lf, "{\"" + A + "\":{\"firstSeenAt\":-5,\"firstMatchAt\":3}}");
        check(JoinLedger.load(lf).get(A).firstSeenAt == 0, "negative stamps read as 0");

        // What counts as finishing a match: present at the end and his own seated seconds reach the reward minimum.
        int min = Spec.CAMPAIGN[0].minimumRewardSeconds();
        check(JoinLedger.finished(true, min, min) && JoinLedger.finished(true, min + 600, min), "present and seated the minimum");
        check(!JoinLedger.finished(true, min - 1, min), "a late joiner seated less than the minimum is not stamped, however long the match ran");
        check(!JoinLedger.finished(false, min + 600, min), "dropped and never returned: no");
        check(!JoinLedger.finished(true, 0, min), "no seated time: no");

        // The crash path: players away at the checkpoint, or a checkpoint with no away list, stamp nobody.
        Map<UUID, Long> played = new LinkedHashMap<>(); played.put(A, (long) min + 5); played.put(B, (long) min + 5);
        check(JoinLedger.crashStamps(played, Set.of(B), min).equals(Set.of(A)), "crash: the player who was away is not stamped");
        check(JoinLedger.crashStamps(played, Set.of(), min).equals(Set.of(A, B)), "crash: present players are stamped");
        check(JoinLedger.crashStamps(played, null, min).isEmpty(), "crash: an old checkpoint without the away list stamps nobody");
        played.put(A, (long) min - 1);
        check(JoinLedger.crashStamps(played, Set.of(), min).equals(Set.of(B)), "crash: played less than the minimum is not stamped");

        // The gate: console only when the output is the server and no entity runs it; else an owner uuid.
        var gate = new AdminOg.Config();
        check(AdminOg.allowed(gate, true, false, null), "console: output is the server, no entity");
        check(!AdminOg.allowed(gate, false, false, null), "a command block (entity-less, other output), even one renamed Server, is refused");
        check(!AdminOg.allowed(gate, true, true, null), "an entity-run source is not the console");
        check(!AdminOg.allowed(gate, false, true, A), "a player who is not an owner is refused");
        gate.owners.add(A.toString());
        check(AdminOg.allowed(gate, false, true, A) && !AdminOg.allowed(gate, false, true, B), "a listed owner passes, another player does not");

        // admin.json through the store: cached by modification time, .bad once, no write while damaged, unknown keys kept.
        Path sd = Files.createTempDirectory("adminstore");
        Path af = sd.resolve("admin.json");
        var store = new AdminOg.Store(af);
        check(store.get().ogAdded.isEmpty() && store.problem() == null && store.reads == 1, "missing file: empty, one read");
        store.get(); store.get();
        check(store.reads == 1, "an unchanged file is not read again");
        Files.writeString(af, "{\"ogAdded\":[\"" + A + "\"],\"keepMe\":{\"x\":1}}");
        Files.setLastModifiedTime(af, java.nio.file.attribute.FileTime.fromMillis(1_000_000));
        check(store.get().ogAdded.size() == 1 && store.reads == 2, "a changed file is read again");
        var edit = store.copy(); edit.add(B);
        store.save(edit);
        check(Files.readString(af).contains("keepMe") && store.get().ogAdded.contains(B.toString()), "a command write keeps unknown keys");
        Files.writeString(af, "{oops");
        Files.setLastModifiedTime(af, java.nio.file.attribute.FileTime.fromMillis(2_000_000));
        int bads = 0;
        for (int n = 0; n < 5; n++) { store.get(); store.problem(); }
        try (Stream<Path> st = Files.list(sd)) { bads = (int) st.filter(p -> p.getFileName().toString().contains(".bad")).count(); }
        check(store.problem() != null && store.get().ogAdded.isEmpty() && bads == 1, "malformed: read as empty, one .bad copy however often it is asked");
        boolean refused = false;
        try { store.save(new AdminOg.Config()); } catch (IllegalStateException refuse) { refused = refuse.getMessage().contains("damaged"); }
        check(refused && Files.readString(af).equals("{oops"), "a write is refused while the file is damaged, and the file is untouched");
        Files.writeString(af, "{}");
        Files.setLastModifiedTime(af, java.nio.file.attribute.FileTime.fromMillis(3_000_000));
        check(store.problem() == null, "fixed file is accepted");

        // joins.json: damaged or blank existing file is never overwritten; a missing one starts empty.
        for (String bad : new String[] {"{broken", "", "   ", "[]", "{\"zzz\":{}}"}) {
            Path jf = sd.resolve("joins-" + Math.abs(bad.hashCode()) + ".json");
            Files.writeString(jf, bad);
            var bl = JoinLedger.load(jf);
            check(bl.broken && bl.entries.isEmpty(), "damaged ledger is broken: '" + bad + "'");
            bl.seen(A, "Alice", 5);
            boolean no = false;
            try { bl.save(jf); } catch (IllegalStateException refuse) { no = true; }
            check(no && Files.readString(jf).equals(bad), "a broken ledger refuses to save and leaves the file: '" + bad + "'");
        }
        // A repaired ledger saves the merged stamps at once, not at the next stamp.
        Path rp = sd.resolve("repair.json");
        Files.writeString(rp, "{broken");
        var broke = JoinLedger.load(rp); broke.matched(A, "Alice", 55);
        Files.writeString(rp, "{\"" + B + "\":{\"firstSeenAt\":9,\"firstMatchAt\":0,\"lastName\":\"Bob\"}}");
        var healed = JoinLedger.reload(broke, rp);
        var onDisk = JoinLedger.load(rp);
        check(!healed.broken && !onDisk.broken && onDisk.get(A) != null && onDisk.get(A).firstMatchAt == 55 && onDisk.get(B).firstSeenAt == 9, "repaired ledger: the file already holds the merged stamp, with no further stamp");
        var still = JoinLedger.load(sd.resolve("repair2.json")); // missing: not broken, reload keeps it
        check(JoinLedger.reload(still, sd.resolve("repair2.json")) == still, "a ledger that is not broken is returned as it is");

        // Unknown keys inside ogWindow survive a command write.
        Path wf = sd.resolve("window.json");
        Files.writeString(wf, "{\"ogWindow\":{\"start\":\"2026-12-01T00:00:00Z\",\"end\":\"2026-12-31T23:59:59Z\",\"note\":\"keep\"},\"top\":1}");
        var ws = new AdminOg.Store(wf);
        var wc = ws.copy(); wc.ogWindow.end = "2027-01-31T23:59:59Z";
        ws.save(wc);
        String wtext = Files.readString(wf);
        check(wtext.contains("\"note\"") && wtext.contains("2027-01-31") && wtext.contains("\"top\"") && !wtext.contains("2026-12-31"), "ogWindow keeps its unknown keys; known fields change");
        check(!JoinLedger.load(sd.resolve("nothing.json")).broken, "a missing ledger file starts empty and is not broken");
        Path fixed = sd.resolve("fixable.json");
        Files.writeString(fixed, "{x");
        var fl = JoinLedger.load(fixed); fl.matched(A, "Alice", 77);
        Files.writeString(fixed, "{\"" + B + "\":{\"firstSeenAt\":9,\"firstMatchAt\":0,\"lastName\":\"Bob\"}}");
        var fresh = JoinLedger.load(fixed);
        check(!fresh.broken && fl.broken, "a repaired file loads clean");
        fresh.absorb(fl);
        check(fresh.get(A).firstMatchAt == 77 && fresh.get(B).firstSeenAt == 9, "stamps taken while damaged are kept after the repair");
        check(JoinLedger.limited("t", 1_000_000) && !JoinLedger.limited("t", 1_030_000) && JoinLedger.limited("t", 1_061_000), "the save error logs at most once a minute");

        // Admin data never reaches a client: the snapshot code lives in Campaign (Minecraft), so the source is read.
        Path src = Paths.get("").toAbsolutePath();
        while (!Files.isDirectory(src.resolve("src/main/java/gg/gravesandglory"))) src = src.getParent();
        src = src.resolve("src/main/java/gg/gravesandglory");
        String commands = Files.readString(src.resolve("AdminCommands.java"));
        check(!Files.readString(src.resolve("Profile.java")).contains("firstMatchAt"), "the profile carries no stamps");
        check(commands.contains("AdminOg.allowed") && commands.contains("ServerCommandSourceAccessor") && !commands.contains("hasPermissionLevel") && !commands.contains("\"Server\""), "commands gate on the owner rule and the real console output, not a name or the permission level");
        check(!commands.contains("getUserCache"), "no user cache lookup");
        String mixins = Files.readString(src.getParent().getParent().getParent().resolve("resources/gravesandglory.mixins.json"));
        check(mixins.substring(0, mixins.indexOf("\"client\"")).contains("ServerCommandSourceAccessor"), "the accessor mixin is registered for the server side (before the client list)");
        check(Files.readString(src.resolve("Campaign.java")).contains("\"away\"") && Files.readString(src.resolve("Campaign.java")).contains("crashStamps"), "the crash checkpoint records who was away");
        System.out.println("AdminOgCheck: " + checks + " checks passed");
    }
}
