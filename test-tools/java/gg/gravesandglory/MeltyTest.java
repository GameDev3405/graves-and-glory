package gg.gravesandglory;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.*;

public final class MeltyTest {
    private static Path checksFile;

    public static void start() {
        String explicit = System.getProperty("gravesandglory.localChecks");
        if(explicit!=null&&net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment()){checksFile=Path.of(explicit);report("mod-loads",true,null);return;}
        String local = System.getenv("LOCALAPPDATA");
        if (local == null) return;
        Path test = Paths.get(local, "Melty", "test", "gravesandglory.json");
        if (!Files.exists(test)) return; // not a test run
        try {
            String json = new String(Files.readAllBytes(test), StandardCharsets.UTF_8);
            Matcher match = Pattern.compile("\"checks\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
            if (match.find()) checksFile = Paths.get(match.group(1).replace("\\\\", "\\"));
        } catch (Exception e) { return; }
        report("mod-loads", true, null);
    }

    public static void report(String name, boolean passed, String why) {
        if (checksFile == null) return;
        String line = (passed ? "pass " : "fail ") + name + (!passed && why != null ? ": " + why : "") + System.lineSeparator();
        try { Files.write(checksFile, line.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND); }
        catch (Exception e) { /* the run says the line never came */ }
    }
    public static boolean active(){return checksFile!=null;}
}
