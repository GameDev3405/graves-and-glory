package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** The Arsenal (page gear, the gear screen plan): the commander's gear on its Arsenal tab and the blacksmith on its Forge tab. It
 *  draws in canvas pixels like the other War Table pages and records the boxes it drew for the harness (rects). Every click sends
 *  a gear: or forge: command through CampScreen.action; the server decides. */
final class WarTableGear {
    static final String TITLE = "Arsenal";
    private static final Map<String, int[]> rectsDrawn = new LinkedHashMap<>();
    private static final Map<String, int[]> rects = Collections.unmodifiableMap(rectsDrawn);
    /** The boxes {x,y,w,h} in canvas px the last frame drew, by name. */
    static Map<String, int[]> rects() { return rects; }

    final CampScreen screen;
    String tab;
    final Map<String, UiButton> buttons = new LinkedHashMap<>();

    WarTableGear(CampScreen screen, String tab) {
        this.screen = screen;
        this.tab = "forge".equals(tab) ? "forge" : "armoury";
    }

    UiButton add(String name, UiButton b) { buttons.put(name, b); return screen.add(b); }

    void init() {
        buttons.clear();
        add("back", WarTable.plaque(56, 120, "icon-table", TITLE, "Esc", 85, 0, b -> screen.close()).quiet());
    }

    void tick() {}

    void render(DrawContext g) {
        rectsDrawn.clear();
        UiCanvas.begin(g);
        try {
            WarTable.drawChrome(g, "gear");
            rectsDrawn.putAll(WarTable.drawn());
        } finally {
            UiCanvas.end(g);
        }
    }

    void overlay(DrawContext g) {}

    boolean mouseClicked(double x, double y, int button) { return false; }

    boolean mouseScrolled(double x, double y, double amount) { return false; }

    /** Esc and the gear key close the screen. */
    boolean keyPressed(int key, int scan) {
        var gearKey = GravesAndGloryClient.gearKey;
        if (key == GLFW.GLFW_KEY_ESCAPE || gearKey != null && gearKey.matchesKey(key, scan)) { screen.close(); return true; }
        return false;
    }
}
