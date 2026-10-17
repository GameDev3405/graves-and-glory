package gg.gravesandglory;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.util.UUID;

/** Copy-on-upgrade: original data survives, and existing current files always win. */
public final class LegacyDataMigration {
    private LegacyDataMigration() {}

    public static void copyMissing(Path oldFile, Path currentFile) throws IOException {
        if (Files.exists(currentFile) || !Files.isRegularFile(oldFile, LinkOption.NOFOLLOW_LINKS)) return;
        Files.createDirectories(currentFile.getParent());
        Path temp = currentFile.resolveSibling(currentFile.getFileName() + ".migration-" + UUID.randomUUID());
        try {
            Files.copy(oldFile, temp);
            // Without REPLACE_EXISTING: never overwrite progress created in the meantime.
            try { Files.move(temp, currentFile); }
            catch (FileAlreadyExistsException alreadyMigrated) { /* Current data wins. */ }
        } finally { Files.deleteIfExists(temp); }
    }

    public static void profiles(Path oldDirectory, Path currentDirectory) throws IOException {
        if (!Files.isDirectory(oldDirectory, LinkOption.NOFOLLOW_LINKS)) return;
        try (var files = Files.list(oldDirectory)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList())
                copyMissing(file, currentDirectory.resolve(file.getFileName()));
        }
    }

    public static void instance(Path directory, boolean development) throws IOException {
        String collection = development ? "test-collections" : "collections";
        profiles(directory.resolve("bannerbound/" + collection), directory.resolve("gravesandglory/" + collection));
        copyMissing(directory.resolve("bannerbound/presentation.json"), directory.resolve("gravesandglory/presentation.json"));
    }

    /** Copy an inactive legacy world before opening it; never rename/delete the original. */
    public static String world(Path saves) throws IOException {
        Path current = saves.resolve("Graves and Glory"), old = saves.resolve("Bannerbound");
        if (Files.isRegularFile(current.resolve("level.dat")) || !Files.isRegularFile(old.resolve("level.dat")))
            return "Graves and Glory";
        if (Files.exists(current)) throw new IOException("Graves and Glory save exists without level.dat; inspect it before upgrading");
        Path lockPath = old.resolve("session.lock");
        if (Files.exists(lockPath)) {
            try (FileChannel channel = FileChannel.open(lockPath, StandardOpenOption.WRITE)) {
                try (FileLock lock = channel.tryLock()) {
                    if (lock == null) throw new IOException("Close the legacy Bannerbound world before upgrading");
                    copyWorld(old, current);
                } catch (java.nio.channels.OverlappingFileLockException busy) {
                    throw new IOException("Close the legacy Bannerbound world before upgrading", busy);
                }
            }
        } else copyWorld(old, current);
        return "Graves and Glory";
    }

    private static void copyWorld(Path old, Path current) throws IOException {
        Path staging = Files.createTempDirectory(current.getParent(), ".gravesandglory-migration-");
        boolean published = false;
        try {
            try (var tree = Files.walk(old)) {
                for (Path from : tree.toList()) {
                    if (from.getFileName().toString().equals("session.lock")) continue;
                    if (Files.isSymbolicLink(from)) throw new IOException("World migration does not follow symbolic links: " + from);
                    Path to = staging.resolve(old.relativize(from));
                    if (Files.isDirectory(from)) Files.createDirectories(to);
                    else {
                        Files.copy(from, to);
                        if (Files.mismatch(from, to) != -1) throw new IOException("World changed during migration: " + from);
                    }
                }
            }
            Files.move(staging, current); // Publish only the complete copy; never replace a current world.
            published = true;
        } finally {
            if (!published) {
                // Only the unique staging directory created above is removed.
                try (var tree = Files.walk(staging)) {
                    for (Path file : tree.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(file);
                }
            }
        }
    }
}
