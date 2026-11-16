package gg.gravesandglory;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.Set;

/**
 * The UI editor's typing box for one line (UI editor design 5.3 and 5.6, phase 1: words). Shows the piece's id, where its wording
 * comes from, its default, and the live values it offers as {slots}; the field holds the template, checked as it is typed with the
 * same rules the server applies (UiSheet.problem). Save sends it to the owner's draft; Default puts back the code default (over a built-in edit too). A locked piece
 * (a design-sheet name, server text) only says where it comes from. The editor's own controls are not editable pieces.
 */
public final class UiEditScreen extends Screen {
    final Screen parent;
    final UiText.Seen piece;
    TextFieldWidget field;
    ButtonWidget save;

    UiEditScreen(Screen parent, UiText.Seen piece) {
        super(Text.literal("Edit UI line"));
        this.parent = parent;
        this.piece = piece;
    }

    int left() { return Math.max(8, width / 2 - 200); }
    int boxW() { return Math.min(400, width - 16); }

    @Override protected void init() {
        int x = left(), w = boxW(), y = height / 2 - 10;
        if (piece.locked) {
            addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close()).dimensions(x, y + 40, 80, 20).build());
            return;
        }
        String current = UiText.draft().getOrDefault(piece.id, UiText.builtin().getOrDefault(piece.id, piece.def));
        field = new TextFieldWidget(textRenderer, x, y, w, 20, Text.literal("Wording"));
        field.setMaxLength(UiSheet.TEXT_CAP);
        field.setText(current);
        field.setChangedListener(t -> check());
        addDrawableChild(field);
        setInitialFocus(field);
        save = addDrawableChild(ButtonWidget.builder(Text.literal("Save to draft"), b -> save()).dimensions(x, y + 60, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Default"), b -> { UiEditorClient.set(piece.id, piece.def, true); close(); }).dimensions(x + 116, y + 60, 80, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> close()).dimensions(x + 202, y + 60, 80, 20).build());
        check();
    }

    UiSheet.Piece asPiece() { return new UiSheet.Piece(piece.id, piece.def, piece.slots, piece.keys, ""); }

    String problem() { return field == null ? null : UiSheet.problem(asPiece(), field.getText()); }

    void check() { if (save != null) save.active = problem() == null; }

    void save() {
        if (problem() != null) return;
        String text = field.getText();
        String before = UiText.draft().get(piece.id);
        if (!text.equals(before == null ? UiText.builtin().getOrDefault(piece.id, piece.def) : before)) UiEditorClient.set(piece.id, text, true);
        close();
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if ((key == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || key == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) && field != null && field.isFocused()) { save(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void render(DrawContext g, int mx, int my, float delta) {
        super.render(g, mx, my, delta);
        int x = left(), w = boxW(), y = height / 2 - 10;
        int ink = 0xffe8d8a8, dim = 0xffa89878, warn = 0xffff8a6a;
        g.drawText(textRenderer, "UI line: " + piece.id, x, y - 62, 0xffffd75a, false);
        if (piece.locked) {
            g.drawText(textRenderer, "Locked. Its wording comes from: " + piece.source + ".", x, y - 46, ink, false);
            g.drawText(textRenderer, textRenderer.trimToWidth("Shows: " + piece.shown, w), x, y - 32, dim, false);
            return;
        }
        g.drawText(textRenderer, "Now from: " + UiText.source(piece.id), x, y - 48, dim, false);
        g.drawText(textRenderer, textRenderer.trimToWidth("Default: " + piece.def, w), x, y - 36, dim, false);
        Set<String> slots = piece.slots;
        g.drawText(textRenderer, slots.isEmpty() ? "Live values: none" : "Live values: " + UiTemplate.braced(slots), x, y - 24, ink, false);
        g.drawText(textRenderer, "Type {{ or }} for a brace. Enter saves.", x, y - 12 + 0, dim, false);
        String p = problem();
        if (p != null) g.drawText(textRenderer, textRenderer.trimToWidth("Cannot save: " + p, w), x, y + 26, warn, false);
        else {
            Set<String> dropped = UiTemplate.dropped(piece.def, field.getText());
            if (!dropped.isEmpty()) g.drawText(textRenderer, textRenderer.trimToWidth("No longer shown: " + UiTemplate.braced(dropped), w), x, y + 26, warn, false);
            else g.drawText(textRenderer, textRenderer.trimToWidth("Shows now: " + piece.shown, w), x, y + 26, dim, false);
        }
    }

    @Override public void close() { client.setScreen(parent); }
}
