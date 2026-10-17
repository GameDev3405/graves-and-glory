package gg.gravesandglory;
import net.minecraft.client.MinecraftClient;
/** Optional diagnostics contract. The player jar contains no provider or test runner. */
public final class Diagnostics {
    public interface Probe {
        void start(); boolean active(); void report(String name,boolean passed,String why);
        void tick(MinecraftClient client); boolean hideHud();
    }
    private static final Probe probe=java.util.ServiceLoader.load(Probe.class).findFirst().orElse(null);
    public static void start(){if(probe!=null)probe.start();}
    public static boolean active(){return probe!=null&&probe.active();}
    public static void report(String name,boolean passed,String why){if(probe!=null)probe.report(name,passed,why);}
    static void tick(MinecraftClient c){if(probe!=null)probe.tick(c);}
    static boolean hideHud(){return probe!=null&&probe.hideHud();}
}
