package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;

import java.io.File;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Frame capture for the drop-board film (suites drop-film and drop-film-watch). Every rendered frame is read back in the game through a
 * three-slot PBO ring and piped to ffmpeg, so one game frame is one file frame (the readback and the ffmpeg line are the media shoot's,
 * branch media/shoot-2). The first `warm` frames after start() are not written, so ffmpeg's start-up and the first shadows do not show.
 * Never shipped.
 */
public final class DropFilm {
    private DropFilm() {}

    static File file; static int fps = 60, warm = 90; static long seen, pushed; static Recorder rec;
    static final ArrayList<Long> times = new ArrayList<>(); static long lastNanos;
    /** True once the warm frames are over and frames are being written. */
    public static volatile boolean live;
    public static volatile String stats = "";
    public static int width, height;

    static int envInt(String k, int d) { try { return Integer.parseInt(System.getenv().getOrDefault(k, "" + d).trim()); } catch (NumberFormatException e) { return d; } }
    static void say(String s) { System.out.println("[p1] drop-film: " + s); }

    /** The window at the film size, undecorated at the screen's corner, vsync off and the frame rate capped (call on ticks 2 and 40, as the media shoot did). */
    public static void prepare(MinecraftClient c, int w, int h, int cap) {
        long handle = c.getWindow().getHandle();
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
        GLFW.glfwSetWindowPos(handle, envInt("GRAVES_FILM_X", 0), envInt("GRAVES_FILM_Y", 0));
        GLFW.glfwSetWindowSize(handle, w, h);
        GLFW.glfwSetWindowAttrib(handle, GLFW.GLFW_FLOATING, GLFW.GLFW_TRUE); // on top, so the desktop never throttles it as an occluded window
        GLFW.glfwFocusWindow(handle);
        c.options.getEnableVsync().setValue(false);
        c.options.getMaxFps().setValue(cap);
        c.options.pauseOnLostFocus = false;
    }

    public static boolean recording() { return file != null; }

    /** Opens the recorder at the framebuffer's size. Call on the render thread (a client tick is). */
    public static void start(MinecraftClient c, File f, int frameRate, int warmFrames) {
        if (file != null) stop();
        Framebuffer fb = c.getFramebuffer();
        width = fb.textureWidth; height = fb.textureHeight;
        try { rec = new Recorder(f, width, height, frameRate); } catch (Exception e) { say("recorder failed: " + e); rec = null; return; }
        file = f; fps = frameRate; warm = warmFrames; seen = 0; pushed = 0; times.clear(); lastNanos = 0; live = false; stats = "";
    }

    /** Called at the end of every rendered frame (DropFilmFrameMixin). */
    public static void afterFrame(MinecraftClient c) {
        if (file == null || rec == null) return;
        seen++;
        if (seen <= warm) return;
        live = true;
        long now = System.nanoTime();
        if (lastNanos != 0) times.add(now - lastNanos);
        lastNanos = now;
        rec.push(c.getFramebuffer());
        pushed++;
    }

    /** Closes the file and returns its statistics as JSON (also written beside the video). */
    public static String stop() {
        if (file == null) return stats;
        File f = file; file = null; live = false;
        if (rec != null) { rec.close(); rec = null; }
        var ms = new ArrayList<Double>(); for (long n : times) ms.add(n / 1e6);
        double budget = 1000.0 / fps;
        long slow = ms.stream().filter(d -> d > budget * 1.35).count();
        var bad = new StringBuilder(); for (int i = 0; i < ms.size(); i++) if (ms.get(i) > budget * 1.35) bad.append(i).append('=').append(Math.round(ms.get(i))).append(' ');
        Collections.sort(ms);
        double avg = ms.stream().mapToDouble(d -> d).average().orElse(0), p99 = ms.isEmpty() ? 0 : ms.get((int) (ms.size() * .99)), max = ms.isEmpty() ? 0 : ms.get(ms.size() - 1);
        stats = String.format(Locale.ROOT, "{\"file\":\"%s\",\"width\":%d,\"height\":%d,\"fps\":%d,\"frames\":%d,\"avg_ms\":%.2f,\"p99_ms\":%.2f,\"max_ms\":%.2f,\"over_135pct\":%d,\"slow\":\"%s\"}",
            f.getName(), width, height, fps, pushed, avg, p99, max, slow, bad.toString().trim());
        say(stats);
        try { java.nio.file.Files.writeString(new File(f.getPath() + ".json").toPath(), stats); } catch (Exception ignored) {}
        return stats;
    }

    static final class Recorder {
        static final int SLOTS = 3;
        final Process proc; final OutputStream out; final Thread writer; volatile Throwable error;
        final ArrayBlockingQueue<byte[]> full = new ArrayBlockingQueue<>(12), free = new ArrayBlockingQueue<>(12);
        final int n; final int[] pbo = new int[SLOTS]; long issued, harvested;
        Recorder(File file, int w, int h, int fps) throws Exception {
            n = w * h * 4;
            file.getParentFile().mkdirs(); for (int i = 0; i < 12; i++) free.add(new byte[n]);
            for (int i = 0; i < SLOTS; i++) { pbo[i] = GL15.glGenBuffers(); GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo[i]); GL15.glBufferData(GL21.GL_PIXEL_PACK_BUFFER, n, GL15.GL_STREAM_READ); }
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            String ff = System.getenv().getOrDefault("GRAVES_FILM_FFMPEG", "C:/ffmpeg/ffmpeg-8.1-full_build/bin/ffmpeg.exe");
            var cmd = new ArrayList<>(List.of(ff, "-y", "-hide_banner", "-loglevel", "warning", "-f", "rawvideo", "-pix_fmt", "rgba", "-s", w + "x" + h, "-framerate", "" + fps, "-i", "-",
                "-vf", "vflip,scale=out_color_matrix=bt709:out_range=tv,format=yuv420p", "-c:v", "h264_nvenc", "-preset", "p5", "-tune", "hq", "-rc", "vbr", "-cq", "15", "-b:v", "0", "-maxrate", "250M", "-bufsize", "500M",
                "-profile:v", "high", "-color_primaries", "bt709", "-color_trc", "bt709", "-colorspace", "bt709", "-color_range", "tv", "-movflags", "+faststart", file.getPath()));
            var pb = new ProcessBuilder(cmd); pb.redirectErrorStream(true); pb.redirectOutput(new File(file.getPath() + ".ffmpeg.log"));
            proc = pb.start(); out = proc.getOutputStream();
            writer = new Thread(() -> { try { while (true) { byte[] a = full.take(); if (a.length == 0) break; out.write(a); free.put(a); } } catch (Throwable e) { error = e; } }, "drop-film-writer"); writer.start();
        }
        void push(Framebuffer fb) {
            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo[(int) (issued % SLOTS)]);
            fb.beginRead(); GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L); fb.endRead();
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
            issued++;
            if (issued - harvested >= SLOTS - 1) harvest();
        }
        void harvest() {
            try {
                byte[] a = free.take();
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pbo[(int) (harvested % SLOTS)]);
                ByteBuffer m = GL30.glMapBufferRange(GL21.GL_PIXEL_PACK_BUFFER, 0, n, GL30.GL_MAP_READ_BIT);
                if (m != null) { m.get(0, a, 0, n); GL15.glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER); }
                GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
                harvested++; full.put(a);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
        void close() {
            while (harvested < issued) harvest();
            for (int b : pbo) GL15.glDeleteBuffers(b);
            try { full.put(new byte[0]); writer.join(); out.close(); proc.waitFor(); } catch (Exception e) { System.out.println("[p1] drop-film: recorder close: " + e); }
            if (error != null) System.out.println("[p1] drop-film: writer error: " + error);
        }
    }
}
