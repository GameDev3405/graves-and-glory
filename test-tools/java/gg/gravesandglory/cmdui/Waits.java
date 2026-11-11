package gg.gravesandglory.cmdui;

import gg.gravesandglory.CommandPanel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Shots and checks that wait for the frame they describe: once a frame newer than the request has been rendered and ready holds (the slide
 * settled, the tray drawn, the cursor's pennant hovered), then runs with true; at the deadline it runs with false. each runs on every tick
 * still waiting (to re-place the cursor, say). Never included in releases.
 */
final class Waits {
    private record W(BooleanSupplier ready, Runnable each, Consumer<Boolean> then, int frame, int deadline) {}
    private final List<W> list = new ArrayList<>();

    void when(int tick, int within, BooleanSupplier ready, Runnable each, Consumer<Boolean> then) {
        list.add(new W(ready, each, then, CommandPanel.frames, tick + within));
        if (each != null) each.run();
    }

    void pump(int tick) {
        for (W w : new ArrayList<>(list)) {
            boolean ok = CommandPanel.frames > w.frame && w.ready.getAsBoolean();
            if (ok || tick >= w.deadline) { list.remove(w); w.then.accept(ok); }
            else if (w.each != null) w.each.run();
        }
    }

    void clear() { list.clear(); }
}
