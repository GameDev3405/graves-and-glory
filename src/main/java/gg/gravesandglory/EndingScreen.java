package gg.gravesandglory;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/**
 * The match ending's STATS and REWARDS steps (spec 3, steps 3 and 4): one parchment sheet (the War Table's sheet-field, in a dark wood
 * frame with a brass line) over the dimmed podium. STATS draws the commanders table and the viewer's four groups (EndingPanels), REWARDS
 * the rewards counting up one row at a time, the total and the claim strip. EndingClient opens it on STATS; it follows EndingClient.step()
 * to REWARDS and is closed on DONE. Escape or the menu key skips this player's step (never the game menu); nothing else closes it.
 * The sheet is drawn on the canvas (UiCanvas) at the largest whole scale that fits the window, so at 1280x720 it is twice the art and at
 * 854x480 (427x240 scaled) once, with the hint under it.
 */
final class EndingScreen extends Screen {
    /** The sheet (table/sheet-field) and the room inside its torn rim; the frame around it; the hint's band under it. */
    static final String PAPER = "table/sheet-field";
    static final int W = 524, H = 325, PAD_X = 26, PAD_TOP = 18, PAD_BOTTOM = 20, FRAME = 6, HINT = 22;
    static final int DIM = 0xd007080c, WOOD = 0xff3a2c21, HINT_INK = 0xffcfc3a8;

    long ticks, rewardsAt = -1;
    /** Harness only: the face last drawn (stats or rewards), the sheet's scale and its box on the canvas. */
    static volatile String drawn = "";
    static volatile int scaleDrawn;
    static volatile int[] sheetDrawn = new int[4];

    EndingScreen() { super(Text.literal("Battle report")); }

    @Override public boolean shouldPause() { return false; }
    @Override public boolean shouldCloseOnEsc() { return false; }

    @Override public void tick() {
        ticks++;
        if (EndingClient.step() == MatchEnding.Step.REWARDS && rewardsAt < 0) rewardsAt = ticks;
    }

    /** Escape or the menu key: this player's step moves on (EndingClient.skip); every other key does nothing here. */
    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE || GravesAndGloryClient.menu != null && GravesAndGloryClient.menu.matchesKey(key, scan)) {
            if (client != null) EndingClient.skip(client);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    boolean rewardsFace() { return EndingClient.step() == MatchEnding.Step.REWARDS; }

    /** The rewards' count-up, 0 to 1 over the step's length as the server gave it (a skip ahead never shortens it below a second). */
    float progress(float partial) {
        if (rewardsAt < 0) return 0;
        int len = Math.max(20, EndingClient.total);
        return Math.max(0, Math.min(1, (ticks - rewardsAt + partial) / len));
    }

    @Override public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, DIM);
        UiCanvas.begin(g);
        try {
            int cw = UiCanvas.width(), ch = UiCanvas.height();
            int s = (int) Math.max(1, Math.floor(Math.min((cw - 16) / (double) (W + FRAME * 2), ch / (double) (H + FRAME * 2 + HINT))));
            int ox = (cw - W * s) / 2, oy = Math.max(FRAME * s + 1, (ch - HINT * s - H * s) / 2);
            scaleDrawn = s; sheetDrawn = new int[]{ox, oy, W * s, H * s};
            var m = g.getMatrices();
            m.push();
            m.translate(ox, oy, 0); m.scale(s, s, 1);
            g.fill(-FRAME - 1, -FRAME - 1, W + FRAME + 1, H + FRAME + 1, DarkUi.BRASS);
            g.fill(-FRAME, -FRAME, W + FRAME, H + FRAME, WOOD);
            UiTex.draw(g, PAPER, 0, 0);
            JsonObject r = CampaignClient.result;
            boolean rewards = rewardsFace();
            if (rewards) rewardsSheet(g, r, delta); else statsSheet(g, r);
            drawn = rewards ? "rewards" : "stats";
            m.pop();
            String hint = HintText.expand("{key:menu} or Escape to skip", GravesAndGloryClient::hint);
            var font = textRenderer;
            int hy = Math.min(ch - 9 * s - 2, oy + H * s + FRAME * s + 4);
            m.push(); m.translate(cw / 2f, hy, 0); m.scale(s, s, 1);
            g.drawCenteredTextWithShadow(font, hint, 0, 0, HINT_INK);
            m.pop();
        } finally {
            UiCanvas.end(g);
        }
    }

    /** The sheet's head: the title left in the heading font, the line right in the small font (fitted to what is left), a rule under both. */
    void head(DrawContext g, String title, String meta) {
        var font = textRenderer;
        int x = PAD_X, w = W - PAD_X * 2, y = PAD_TOP;
        Text t = DarkUi.focal(UiMath.trim(title, w / 2, v -> font.getWidth(DarkUi.focal(v))));
        g.drawText(font, t, x, y, EndingPanels.BROWN, false);
        int tw = font.getWidth(t);
        String shown = UiMath.trim(meta, w - tw - 16, v -> font.getWidth(DarkUi.small(v)));
        Text mt = DarkUi.small(shown);
        g.drawText(font, mt, x + w - font.getWidth(mt), y + 4, DarkUi.SEPIA, false);
        g.fill(x, y + 14, x + w, y + 15, EndingPanels.RULE);
    }

    static String map(JsonObject r) { return EndingPanels.str(r, "map"); }

    void statsSheet(DrawContext g, JsonObject r) {
        int x = PAD_X, w = W - PAD_X * 2;
        if (!r.has("id")) { head(g, "Battle report", ""); g.drawText(textRenderer, DarkUi.body("No battle to report yet."), x, PAD_TOP + 26, DarkUi.SEPIA, false); return; }
        String mode = ReportSections.title(r), line = EndingPanels.str(r, "line");
        if (!map(r).isEmpty()) line = line.replace(" · " + map(r), ""); // the title names the map already ("score · map · length")
        head(g, map(r).isEmpty() ? "Battle report" : "Battle of " + map(r), (mode + (line.isEmpty() ? "" : " · " + line)).toUpperCase(Locale.ROOT));
        int y = EndingPanels.scoreboard(g, r, x, PAD_TOP + 22, w);
        EndingPanels.groups(g, r, x, y + 8, w, H - PAD_BOTTOM - (y + 8));
    }

    void rewardsSheet(DrawContext g, JsonObject r, float partial) {
        int x = PAD_X, w = W - PAD_X * 2;
        String outcome = EndingPanels.str(r, "outcome");
        head(g, "Spoils of war", (outcome + (map(r).isEmpty() ? "" : " · " + map(r))).toUpperCase(Locale.ROOT));
        int top = PAD_TOP + 24;
        EndingPanels.rewards(g, r, x, top, w, H - PAD_BOTTOM - top, progress(partial));
    }
}
