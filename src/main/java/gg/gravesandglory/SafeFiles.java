package gg.gravesandglory;

import java.io.IOException;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Atomic writes and fall-back reads for every file the mod persists (Kort approved 2026-11-01, Q21). Pure: no Minecraft classes,
 * so PersistedDataCorruptionCheck runs it as it is.
 * A write goes to a sibling temp file first and is then moved over the target, so a crash or full disk leaves either the old file
 * or the new one, never a torn mix. A read that fails falls back to the .bak that the backed-up write keeps, then to defaults, with a
 * logged warning; the damaged file is moved aside as name.corrupt (or name.corrupt-N), never deleted, so nothing is lost.
 */
public final class SafeFiles {
    private SafeFiles() {}

    /** Reads a file into a value; may throw anything (parse failures, IOException, the loader's own refusals). */
    @FunctionalInterface public interface Loader<T> { T load(Path file) throws Exception; }

    /** The warnings go to the server log by default; the check swaps in a collector. */
    static Consumer<String> warn = line -> System.err.println("[Graves and Glory] " + line);

    public static Path backup(Path file) { return file.resolveSibling(file.getFileName() + ".bak"); }
    static Path temp(Path file) { return file.resolveSibling(file.getFileName() + ".tmp"); }

    /** Writes text to file through a temp file and a move (atomic where the filesystem allows). The temp file never outlives a failure. */
    public static void write(Path file, String text) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path tmp = temp(file);
        try {
            Files.writeString(tmp, text);
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
        } catch (IOException | RuntimeException e) {
            try { if (!Files.isDirectory(tmp)) Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            throw e;
        }
    }

    /** As write, but the previous file (when there is one) is kept as name.bak first, for load to fall back to. */
    public static void writeWithBackup(Path file, String text) throws IOException {
        if (Files.isRegularFile(file)) Files.copy(file, backup(file), StandardCopyOption.REPLACE_EXISTING);
        write(file, text);
    }

    /** write for best-effort files (markers, preferences): false and a warning instead of an exception. */
    public static boolean tryWrite(Path file, String text, String what) {
        try { write(file, text); return true; }
        catch (Exception e) { warn.accept(what + " could not be written: " + e); return false; }
    }

    /**
     * Loads file, falling back to its .bak, then to defaults. Never throws. A missing file is defaults without a warning. When the
     * file fails, it is moved aside (name.corrupt) and the .bak is copied into its place and loaded, so the next save carries on from
     * the backup; when that fails too, the copy is moved aside as well and defaults are returned.
     */
    public static <T> T load(Path file, Loader<T> loader, Supplier<T> defaults, String what) {
        if (!Files.exists(file)) return defaults.get();
        Exception first;
        try { return loader.load(file); } catch (Exception e) { first = e; }
        Path kept = quarantine(file);
        Path bak = backup(file);
        if (kept == null && Files.exists(file)) {
            // Could not move the damaged file aside: never copy over it. Read the backup where it lies instead.
            try {
                T value = loader.load(bak);
                warn.accept(what + " could not be read (" + first + ") or moved aside; read from " + bak.getFileName() + ".");
                return value;
            } catch (Exception second) {
                warn.accept(what + " could not be read (" + first + ") or moved aside, and its backup failed (" + second + "); using defaults.");
                return defaults.get();
            }
        }
        if (Files.isRegularFile(bak)) {
            try {
                Files.copy(bak, file, StandardCopyOption.REPLACE_EXISTING);
                T value = loader.load(file);
                warn.accept(what + " could not be read (" + first + "); restored from " + bak.getFileName() + ". The damaged copy is kept as " + name(kept) + ".");
                return value;
            } catch (Exception second) {
                Path keptBak = quarantine(file);
                warn.accept(what + " and its backup could not be read (" + first + "; " + second + "); using defaults. Damaged copies kept as " + name(kept) + " and " + name(keptBak) + ".");
                return defaults.get();
            }
        }
        warn.accept(what + " could not be read (" + first + ") and has no backup; using defaults. The damaged copy is kept as " + name(kept) + ".");
        return defaults.get();
    }

    static String name(Path p) { return p == null ? "(could not be moved aside)" : p.getFileName().toString(); }

    /** Moves file aside to the first free name.corrupt, name.corrupt-2, ...; null when it is gone or cannot be moved. */
    static Path quarantine(Path file) {
        if (!Files.exists(file)) return null;
        for (int n = 1; n < 1000; n++) {
            Path to = file.resolveSibling(file.getFileName() + (n == 1 ? ".corrupt" : ".corrupt-" + n));
            if (Files.exists(to)) continue;
            try { return Files.move(file, to); } catch (IOException e) { return null; }
        }
        return null;
    }
}
