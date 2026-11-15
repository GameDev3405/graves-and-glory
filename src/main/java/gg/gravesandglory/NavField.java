package gg.gravesandglory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.zip.GZIPInputStream;

/**
 * The route field of an engine map (engine-map pathing plan, step 1): where a troop on foot goes next to reach a goal the vanilla pathfinder cannot see, built from the map's own floors
 * (nav.bin.gz, tools/sectionmap.py: every standable block of every column, the ground under a gatehouse as well as its roof), never from EngineMap.walkable, which reads the top block
 * of a column and so cannot pass under a lintel. No Minecraft types, so NavFieldCheck compiles it standalone.
 *
 * Floors: a floor is a block a troop stands on, steps of at most 1 up or down between 4-neighbour columns. Only floors reachable on foot from the map's spawns and points are kept:
 * a roof or wall top with no stair to it, and a sealed pocket, are not places. Graph: each 4 x 4 patch of columns is cut into its connected pieces of floor (one node each, so the
 * ground under a gatehouse and its roof are two nodes), and two nodes touching across a patch border share an arc. An arc keeps the 1-block cell the crossing is made at (the centre of
 * its widest crossing, the far side) and its clearance: the free width of the passage there (the shorter of the x and z runs of floor through the cell, capped), so a 3-wide side door
 * is an arc of width 3 although the graph is 4 blocks coarse. An arc under PORTAL wide is a portal: a route turns on its cell and never looks past it.
 * Fields: a distance field over the nodes per goal node (a Dijkstra over about 14,000 nodes, a fraction of a millisecond), kept for the goals asked for, pinned for the map's own points
 * and spawns. A lookup finds the troop's cell, tests the straight line to the goal over the floors, and only when that is blocked follows the field a few hops and takes the farthest
 * cell of the way it can see (a waypoint on a real floor, with its height). No search of the map per tick.
 */
public final class NavField {
    /** Columns a node spans on a side. */
    public static final int PATCH = 4;
    /** An arc narrower than this many blocks is a portal. */
    public static final int PORTAL = 8;
    /** Width is counted up to this many blocks. */
    static final int CAP = 12;
    /** The straight line to a goal is trusted over this many blocks (Manhattan) at most. */
    static final int SIGHT = 64;
    /** A waypoint is looked for this many hops along a field, and no farther than AHEAD blocks from the troop. */
    static final int HOPS = 12, AHEAD = 28;
    /** Distance fields kept are bounded by memory, not by a count: CACHE_BYTES of them (fieldBytes each, between CACHE_MIN and CACHE_MAX fields; the pinned ones come on top). Fields built in one tick before lookups answer "no route" until the next:
     *  a round opens with every troop asking for its own slot's field (a goal node apiece), and 6 a tick refused some of them. A build is a fraction of a millisecond (roundStats prints the mean). */
    static final long CACHE_BYTES = 24L << 20;
    static final int CACHE_MIN = 64, CACHE_MAX = 512, BUILDS_PER_TICK = 16;
    /** The cells a search inside one node looks at. */
    static final int INSIDE = 96;
    /** How near a goal must be to a floor to be given a field: columns. */
    static final int GOAL_RADIUS = 8;
    /** A troop this close (blocks) to the portal cell of its first hop is at the door: the way goes on past it. */
    static final double AT_PORTAL = 1.6;
    static final char FAR = 65535;
    static final double SCALE = 16;

    /** A waypoint on a floor: the standing point (feet y), whether it is a portal, and the free width there. */
    public record Waypoint(double x, double y, double z, boolean portal, int width) {}

    public final int ox, oz, w, d;
    /** floors per column, compacted: column c holds ys[start[c] .. start[c+1]); world y of the floor block, lowest first. Reachable floors only. */
    private final int[] start;
    private final short[] ys;
    private final int[] colOf;
    private final int[] nodeOf;
    private final int nodes;
    private final float[] nodeX, nodeZ;
    private final int[] arcStart, arcTo, arcCell, arcRev;
    private final byte[] arcWidth;
    public final long bakeNanos;
    public final int cells, arcs;

    /** A distance field to one goal node: char distance in 1/16 block (FAR: not reachable) and the arc each node leaves by. */
    private static final class Field {
        final char[] dist;
        final int[] arc;
        Field(int n) { dist = new char[n]; arc = new int[n]; }
    }
    private final Map<Integer, Field> pinned = new HashMap<>();
    private final LinkedHashMap<Integer, Field> cache = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Integer, Field> e) { return size() > cacheCap(); }
    };
    /** Fields the cache keeps: CACHE_BYTES of them, so that memory stays bounded whatever the map's size. */
    int cacheCap() { return (int) Math.max(CACHE_MIN, Math.min(CACHE_MAX, CACHE_BYTES / Math.max(1, fieldBytes()))); }
    private long budgetTick = Long.MIN_VALUE;
    private long roundBuilds, roundRefused, roundLookups, roundRouted;
    private int builtThisTick;
    /** Counters for the harness: fields built, their time, lookups, lookups answered with a waypoint, lookups refused by the build budget. */
    public long builds, buildNanos, lookups, routed, refused;

    /**
     * Builds the field from raw floors: start (w*d+1 offsets) and ys (world y of each floor block, per column, lowest first); seeds are {world x, world z, floor y} of the places the walk
     * starts from (spawns and points), and pinned goals are built for them. A seed with no floor within one block of its y is skipped.
     */
    public NavField(int ox, int oz, int w, int d, int[] start0, short[] ys0, int[][] seeds) {
        long t0 = System.nanoTime();
        this.ox = ox; this.oz = oz; this.w = w; this.d = d;
        // reachable floors: a BFS over cells from the seeds, then the table compacted to them
        int total = ys0.length;
        int[] col0 = new int[total];
        for (int c = 0; c < w * d; c++) for (int i = start0[c]; i < start0[c + 1]; i++) col0[i] = c;
        boolean[] reach = new boolean[total];
        int[] queue = new int[total];
        int head = 0, tail = 0;
        for (int[] s : seeds) {
            int x = s[0] - ox, z = s[1] - oz;
            if (x < 0 || x >= w || z < 0 || z >= d) continue;
            int c = z * w + x, best = -1;
            for (int i = start0[c]; i < start0[c + 1]; i++) if (Math.abs(ys0[i] - s[2]) <= 1 && (best < 0 || Math.abs(ys0[i] - s[2]) < Math.abs(ys0[best] - s[2]))) best = i;
            if (best >= 0 && !reach[best]) { reach[best] = true; queue[tail++] = best; }
        }
        int[] DX = {1, -1, 0, 0}, DZ = {0, 0, 1, -1};
        while (head < tail) {
            int i = queue[head++], col = col0[i], x = col % w, z = col / w;
            for (int k = 0; k < 4; k++) {
                int nx = x + DX[k], nz = z + DZ[k];
                if (nx < 0 || nx >= w || nz < 0 || nz >= d) continue;
                int nc = nz * w + nx;
                for (int j = start0[nc]; j < start0[nc + 1]; j++) if (!reach[j] && Math.abs(ys0[j] - ys0[i]) <= 1) { reach[j] = true; queue[tail++] = j; }
            }
        }
        start = new int[w * d + 1];
        int n = 0;
        for (int i = 0; i < total; i++) if (reach[i]) n++;
        ys = new short[n];
        colOf = new int[n];
        int k = 0;
        for (int c = 0; c < w * d; c++) {
            start[c] = k;
            for (int i = start0[c]; i < start0[c + 1]; i++) if (reach[i]) { ys[k] = ys0[i]; colOf[k] = c; k++; }
        }
        start[w * d] = k;
        cells = n;
        // nodes: the connected pieces of floor inside each 4 x 4 patch
        nodeOf = new int[n];
        Arrays.fill(nodeOf, -1);
        int nn = 0;
        int[] stack = new int[n];
        double[] sx = new double[Math.max(16, n / 8)], sz = new double[sx.length];
        int[] cnt = new int[sx.length];
        for (int c0 = 0; c0 < n; c0++) {
            if (nodeOf[c0] >= 0) continue;
            if (nn == sx.length) { sx = Arrays.copyOf(sx, nn * 2); sz = Arrays.copyOf(sz, nn * 2); cnt = Arrays.copyOf(cnt, nn * 2); }
            int sp = 0;
            stack[sp++] = c0;
            nodeOf[c0] = nn;
            int pc = patchOf(c0);
            while (sp > 0) {
                int c = stack[--sp];
                sx[nn] += colOf[c] % w + .5;
                sz[nn] += colOf[c] / w + .5;
                cnt[nn]++;
                for (int dir = 0; dir < 4; dir++) {
                    int nb = step(c, DX[dir], DZ[dir]);
                    if (nb >= 0 && nodeOf[nb] < 0 && patchOf(nb) == pc) { nodeOf[nb] = nn; stack[sp++] = nb; }
                }
            }
            nn++;
        }
        nodes = nn;
        nodeX = new float[nn];
        nodeZ = new float[nn];
        for (int i = 0; i < nn; i++) { nodeX[i] = (float) (sx[i] / cnt[i] + ox); nodeZ[i] = (float) (sz[i] / cnt[i] + oz); }
        // arcs: every pair of neighbouring cells of two nodes is a link; the links of a node pair make one edge
        HashMap<Long, Integer> edgeOf = new HashMap<>();
        int[] linkA = new int[1024], linkB = new int[1024], linkEdge = new int[1024];
        int links = 0;
        int[] edgeA = new int[1024], edgeB = new int[1024];
        int edges = 0;
        for (int c = 0; c < n; c++) {
            for (int dir = 0; dir < 4; dir += 2) { // +x and +z: each neighbouring pair once
                int nb = step(c, DX[dir], DZ[dir]);
                if (nb < 0 || nodeOf[nb] == nodeOf[c]) continue;
                int a = nodeOf[c], b = nodeOf[nb], ca = c, cb = nb;
                if (a > b) { int t = a; a = b; b = t; t = ca; ca = cb; cb = t; }
                long key = (long) a * nn + b;
                Integer e = edgeOf.get(key);
                if (e == null) {
                    e = edges++;
                    if (e == edgeA.length) { edgeA = Arrays.copyOf(edgeA, e * 2); edgeB = Arrays.copyOf(edgeB, e * 2); }
                    edgeA[e] = a; edgeB[e] = b; edgeOf.put(key, e);
                }
                if (links == linkA.length) { linkA = Arrays.copyOf(linkA, links * 2); linkB = Arrays.copyOf(linkB, links * 2); linkEdge = Arrays.copyOf(linkEdge, links * 2); }
                linkA[links] = ca; linkB[links] = cb; linkEdge[links] = e; links++;
            }
        }
        // per edge: the width of each link is the narrower of the passage at its two cells; the edge keeps its widest links and crosses at the middle one
        int[] perEdge = new int[edges + 1];
        for (int l = 0; l < links; l++) perEdge[linkEdge[l] + 1]++;
        for (int e = 0; e < edges; e++) perEdge[e + 1] += perEdge[e];
        int[] order = new int[links], fill = Arrays.copyOf(perEdge, edges);
        for (int l = 0; l < links; l++) order[fill[linkEdge[l]]++] = l;
        int[] crossA = new int[edges], crossB = new int[edges];
        byte[] width = new byte[edges];
        int[] lw = new int[64];
        for (int e = 0; e < edges; e++) {
            int from = perEdge[e], count = perEdge[e + 1] - from;
            if (lw.length < count) lw = new int[count * 2];
            int best = 0;
            for (int i = 0; i < count; i++) { int l = order[from + i]; lw[i] = Math.min(width(linkA[l]), width(linkB[l])); best = Math.max(best, lw[i]); }
            int seen = 0, mid = 0;
            for (int i = 0; i < count; i++) if (lw[i] == best) seen++;
            int want = seen / 2;
            for (int i = 0, s = 0; i < count; i++) if (lw[i] == best) { if (s++ == want) { mid = order[from + i]; break; } }
            crossA[e] = linkA[mid]; crossB[e] = linkB[mid]; width[e] = (byte) best;
        }
        arcs = 2 * edges;
        arcStart = new int[nn + 1];
        for (int e = 0; e < edges; e++) { arcStart[edgeA[e] + 1]++; arcStart[edgeB[e] + 1]++; }
        for (int i = 0; i < nn; i++) arcStart[i + 1] += arcStart[i];
        arcTo = new int[arcs]; arcCell = new int[arcs]; arcRev = new int[arcs]; arcWidth = new byte[arcs];
        int[] at = Arrays.copyOf(arcStart, nn);
        for (int e = 0; e < edges; e++) {
            int fwd = at[edgeA[e]]++, back = at[edgeB[e]]++;
            arcTo[fwd] = edgeB[e]; arcCell[fwd] = crossB[e]; arcRev[fwd] = back; arcWidth[fwd] = width[e];
            arcTo[back] = edgeA[e]; arcCell[back] = crossA[e]; arcRev[back] = fwd; arcWidth[back] = width[e];
        }
        // the map's own points and spawns are goals every order meets: their fields now, not on the first troop's tick
        for (int[] s : seeds) { int c = cellNear(s[0] + .5, s[2] + 1, s[1] + .5, 2, false); if (c >= 0 && !pinned.containsKey(nodeOf[c])) pinned.put(nodeOf[c], build(nodeOf[c])); }
        builds = 0; buildNanos = 0;
        bakeNanos = System.nanoTime() - t0;
    }

    /** A field of a map that has its floors as columns of ys (null or empty: none): for tests and tools. Column index (z - oz) * w + (x - ox). */
    public static NavField fromColumns(int ox, int oz, int w, int d, int[][] columns, int[][] seeds) {
        int[] start = new int[w * d + 1];
        int total = 0;
        for (int c = 0; c < w * d; c++) { start[c] = total; if (columns[c] != null) total += columns[c].length; }
        start[w * d] = total;
        short[] ys = new short[total];
        for (int c = 0; c < w * d; c++) if (columns[c] != null) { int[] s = columns[c].clone(); Arrays.sort(s); for (int i = 0; i < s.length; i++) ys[start[c] + i] = (short) s[i]; }
        return new NavField(ox, oz, w, d, start, ys, seeds);
    }

    /** Reads nav.bin.gz (tools/sectionmap.py write_nav) of a map; IOException with a reason when it is not this map's (its design_sha256) or not a nav file. */
    public static NavField read(InputStream in, int ox, int oz, int w, int d, int floorY, String sha, int[][] seeds) throws IOException {
        byte[] raw;
        try (InputStream body = new GZIPInputStream(in)) { raw = body.readAllBytes(); }
        if (raw.length < 80 || raw[0] != 'N' || raw[1] != 'A' || raw[2] != 'V' || raw[3] != '1') throw new IOException("not a nav file");
        String made = new String(raw, 4, 64, StandardCharsets.US_ASCII);
        if (!made.equals(sha)) throw new IOException("nav.bin.gz was baked for another design (" + made.substring(0, 12) + ", the map is " + sha.substring(0, 12) + "): run python tools/sectionmap.py nav");
        ByteBuffer b = ByteBuffer.wrap(raw, 68, 12).order(ByteOrder.LITTLE_ENDIAN);
        int fw = b.getInt(), fd = b.getInt(), ff = b.getInt();
        if (fw != w || fd != d || ff != floorY) throw new IOException("nav.bin.gz is " + fw + "x" + fd + " at " + ff + ", the map is " + w + "x" + d + " at " + floorY);
        int[] start = new int[w * d + 1];
        int total = 0;
        for (int c = 0; c < w * d; c++) { start[c] = total; total += raw[80 + c] & 255; }
        start[w * d] = total;
        if (raw.length != 80 + w * d + total) throw new IOException("nav.bin.gz holds " + raw.length + " bytes, expected " + (80 + w * d + total));
        short[] ys = new short[total];
        for (int i = 0; i < total; i++) ys[i] = (short) (floorY + (raw[80 + w * d + i] & 255));
        return new NavField(ox, oz, w, d, start, ys, seeds);
    }

    // ---- cells

    private int patchOf(int cell) { int c = colOf[cell]; return (c / w / PATCH) * ((w + PATCH - 1) / PATCH) + (c % w) / PATCH; }

    /** The cell one column over (dx, dz) from cell c that a step of at most one block up or down reaches, or -1. */
    private int step(int c, int dx, int dz) {
        int col = colOf[c], x = col % w + dx, z = col / w + dz;
        if (x < 0 || x >= w || z < 0 || z >= d) return -1;
        int nc = z * w + x, y = ys[c];
        for (int i = start[nc]; i < start[nc + 1]; i++) { int dy = ys[i] - y; if (dy >= -1 && dy <= 1) return i; }
        return -1;
    }

    private int run(int c, int dx, int dz) {
        int n = 1;
        for (int sgn = -1; sgn <= 1; sgn += 2) { int cur = c; while (n < CAP) { int nx = step(cur, dx * sgn, dz * sgn); if (nx < 0) break; cur = nx; n++; } }
        return n;
    }

    /** Free width of the passage at a cell: the shorter of the runs of floor through it along x and along z (up to CAP). */
    private int width(int c) { return Math.min(run(c, 1, 0), run(c, 0, 1)); }

    /** The cell standing at world (x, z) with feet y nearest to the floor under it, looking over radius columns round it; -1 when none. */
    public int cellNear(double x, double feetY, double z, int radius) { return cellNear(x, feetY, z, radius, true); }
    /** strict: a floor more than 2 blocks from the feet is never the match (a troop); not strict (a goal, a seed): only the troop's own column holds to it, a goal's height is the point's, not a floor's. */
    private int cellNear(double x, double feetY, double z, int radius, boolean strict) {
        int bx = (int) Math.floor(x) - ox, bz = (int) Math.floor(z) - oz, target = (int) Math.floor(feetY - .01);
        int best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int r = 0; r <= radius; r++) {
            if (best >= 0 && (r - 1.0) * (r - 1.0) > bestScore) break;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int cx = bx + dx, cz = bz + dz;
                if (cx < 0 || cx >= w || cz < 0 || cz >= d) continue;
                int c = cz * w + cx;
                double h = Math.hypot(Math.max(0, Math.abs(cx + .5 + ox - x) - .5), Math.max(0, Math.abs(cz + .5 + oz - z) - .5));
                for (int i = start[c]; i < start[c + 1]; i++) {
                    int dy = Math.abs(ys[i] - target);
                    if (dy > 2 && (strict || r == 0)) continue; // a roof troop is not matched to the ground below, a troop in the air over a floor is off the field
                    double score = h * h + 4.0 * dy * dy;
                    if (score < bestScore) { bestScore = score; best = i; }
                }
            }
        }
        return best;
    }

    /** a floor block at world (wx, floorY, wz): a troop stands on it at floorY + 1 */
    public boolean floorAt(int wx, int floorY, int wz) { int x = wx - ox, z = wz - oz; if (x < 0 || x >= w || z < 0 || z >= d) return false; for (int i = start[z * w + x]; i < start[z * w + x + 1]; i++) if (ys[i] == floorY) return true; return false; }
    public boolean has(int wx, int wz) { int x = wx - ox, z = wz - oz; return x >= 0 && x < w && z >= 0 && z < d && start[z * w + x] < start[z * w + x + 1]; }

    /** The floor of column (wx, wz) a troop is put on, as {x, z, floor y}: the column's own top block (top) when it is a floor, else its lowest floor (the ground under a roof or a lintel);
     *  none in the column: the nearest column with a floor within radius, ties toward (refX, refZ). Null when there is none: the caller keeps the column. */
    public int[] stand(int wx, int wz, IntBinaryOperator top, double refX, double refZ, int radius) {
        int bx = wx - ox, bz = wz - oz;
        int bestX = 0, bestZ = 0;
        long bestD = Long.MAX_VALUE;
        double bestT = Double.MAX_VALUE;
        for (int r = 0; r <= radius; r++) {
            if (bestD != Long.MAX_VALUE && (long) (r - 1) * (r - 1) > bestD) break;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                long dd = (long) dx * dx + (long) dz * dz;
                if (dd > bestD) continue;
                int cx = bx + dx, cz = bz + dz;
                if (cx < 0 || cx >= w || cz < 0 || cz >= d || start[cz * w + cx] == start[cz * w + cx + 1]) continue;
                double t = Math.hypot(cx + ox + .5 - refX, cz + oz + .5 - refZ);
                if (dd == bestD && t >= bestT) continue;
                bestD = dd; bestT = t; bestX = cx; bestZ = cz;
            }
        }
        if (bestD == Long.MAX_VALUE) return null;
        int c = bestZ * w + bestX, y = ys[start[c]], t = top.applyAsInt(bestX + ox, bestZ + oz);
        for (int i = start[c]; i < start[c + 1]; i++) if (ys[i] == t && width(i) >= 3) y = ys[i]; // the top block only when it is a place to stand (a tent ridge is not): else the lowest floor
        return new int[]{bestX + ox, bestZ + oz, y};
    }

    // ---- fields

    private Field build(int goal) {
        long t0 = System.nanoTime();
        Field f = new Field(nodes);
        Arrays.fill(f.dist, FAR);
        Arrays.fill(f.arc, -1);
        float[] dist = new float[nodes];
        Arrays.fill(dist, Float.MAX_VALUE);
        long[] heap = new long[256];
        int size = 0;
        dist[goal] = 0;
        heap[size++] = ((long) Float.floatToIntBits(0) << 32) | goal;
        while (size > 0) {
            long top = heap[0];
            heap[0] = heap[--size];
            for (int i = 0; ; ) { int l = 2 * i + 1, r = l + 1, m = i; if (l < size && heap[l] < heap[m]) m = l; if (r < size && heap[r] < heap[m]) m = r; if (m == i) break; long t = heap[i]; heap[i] = heap[m]; heap[m] = t; i = m; }
            int n = (int) top;
            float dn = Float.intBitsToFloat((int) (top >>> 32));
            if (dn > dist[n]) continue;
            for (int a = arcStart[n]; a < arcStart[n + 1]; a++) {
                int m = arcTo[a];
                float nd = dn + (float) Math.hypot(nodeX[n] - nodeX[m], nodeZ[n] - nodeZ[m]);
                if (nd < dist[m]) {
                    dist[m] = nd;
                    f.arc[m] = arcRev[a]; // the arc m takes toward n
                    if (size == heap.length) heap = Arrays.copyOf(heap, size * 2);
                    int i = size++;
                    heap[i] = ((long) Float.floatToIntBits(nd) << 32) | m;
                    while (i > 0) { int p = (i - 1) / 2; if (heap[p] <= heap[i]) break; long t = heap[p]; heap[p] = heap[i]; heap[i] = t; i = p; }
                }
            }
        }
        for (int i = 0; i < nodes; i++) if (dist[i] != Float.MAX_VALUE) f.dist[i] = (char) Math.min(FAR - 1, Math.round(dist[i] * SCALE));
        builds++;
        buildNanos += System.nanoTime() - t0;
        return f;
    }

    private Field fieldFor(int goal, long tick) { return fieldFor(goal, tick, false); }
    private Field fieldFor(int goal, long tick, boolean force) {
        Field f = pinned.get(goal);
        if (f != null) return f;
        f = cache.get(goal);
        if (f != null) return f;
        if (force) { f = build(goal); cache.put(goal, f); return f; }
        if (tick != budgetTick) { budgetTick = tick; builtThisTick = 0; }
        if (builtThisTick >= BUILDS_PER_TICK) { refused++; return null; }
        builtThisTick++;
        f = build(goal);
        cache.put(goal, f);
        return f;
    }

    // ---- lookups

    /** The straight line from (ax, az) at floor y (the floor under the troop) to (bx, bz) crosses only floor, each column's floor within one block of the last, within SIGHT columns. */
    boolean line(double ax, double az, int y, double bx, double bz) {
        int x0 = (int) Math.floor(ax) - ox, z0 = (int) Math.floor(az) - oz, x1 = (int) Math.floor(bx) - ox, z1 = (int) Math.floor(bz) - oz;
        int adx = Math.abs(x1 - x0), adz = Math.abs(z1 - z0);
        if (adx + adz > SIGHT) return false;
        int sx = Integer.compare(x1, x0), sz = Integer.compare(z1, z0);
        double dx = bx - ax, dz = bz - az;
        int x = x0, z = z0;
        double tMaxX = dx == 0 ? Double.MAX_VALUE : ((sx > 0 ? x0 + 1 + ox - ax : ax - (x0 + ox)) / Math.abs(dx));
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : ((sz > 0 ? z0 + 1 + oz - az : az - (z0 + oz)) / Math.abs(dz));
        double tDX = dx == 0 ? Double.MAX_VALUE : 1 / Math.abs(dx), tDZ = dz == 0 ? Double.MAX_VALUE : 1 / Math.abs(dz);
        int cy = y;
        for (int steps = adx + adz; steps > 0; steps--) {
            if (tMaxX == tMaxZ && steps > 1) { // the line goes exactly through a corner: either cell beside the corner will do (a wall-hugging cell of a diagonal tunnel still sees along it), then the cell across
                int side = near(x, z + sz, cy);
                if (side == Integer.MIN_VALUE) side = near(x + sx, z, cy);
                if (side == Integer.MIN_VALUE) return false;
                x += sx; z += sz; tMaxX += tDX; tMaxZ += tDZ; steps--; // the troop does not step on the side cell: the cell across is held to the height it left
            } else if (tMaxX < tMaxZ) { x += sx; tMaxX += tDX; } else { z += sz; tMaxZ += tDZ; }
            int found = near(x, z, cy);
            if (found == Integer.MIN_VALUE) return false;
            cy = found;
        }
        return true;
    }

    /** The floor of column (x, z), field coordinates, within one block of y; Integer.MIN_VALUE when there is none or the column is off the field. */
    private int near(int x, int z, int y) {
        if (x < 0 || x >= w || z < 0 || z >= d) return Integer.MIN_VALUE;
        int c = z * w + x;
        for (int i = start[c]; i < start[c + 1]; i++) if (Math.abs(ys[i] - y) <= 1) return ys[i];
        return Integer.MIN_VALUE;
    }

    /** The point a troop at (x, feet y, z) walks to on its way to a goal at (gx, gy, gz): null when it can walk straight at the goal (the line over the floors is clear), when it or the goal
     *  has no floor, when the goal cannot be reached from where it is, or when the build budget of this tick is spent (ask again next tick); else a waypoint on a floor, the farthest cell of the
     *  field's way it can see, or the portal cell when the way goes through a door narrower than PORTAL. tick is the caller's clock, for the build budget only. */
    public synchronized Waypoint route(double x, double y, double z, double gx, double gy, double gz, long tick) {
        lookups++;
        int from = cellNear(x, y, z, 1);
        if (from < 0) return null;
        int to = cellNear(gx, gy, gz, GOAL_RADIUS, false);
        if (to < 0) return null;
        int nf = nodeOf[from], ng = nodeOf[to];
        if (nf == ng) return null;
        if (line(x, z, ys[from], gx, gz)) return null;
        Field f = fieldFor(ng, tick);
        if (f == null || f.dist[nf] == FAR) return null;
        int n = nf;
        Waypoint best = null;
        boolean through = false;
        int door = -1;
        for (int hop = 0; hop < HOPS; hop++) {
            int a = f.arc[n];
            if (a < 0) break;
            int cell = arcCell[a], col = colOf[cell];
            double px = col % w + ox + .5, pz = col / w + oz + .5;
            boolean portal = arcWidth[a] < PORTAL;
            if (hop == 0 && portal && Math.hypot(px - x, pz - z) < AT_PORTAL) { n = arcTo[a]; through = true; door = cell; continue; } // already at the door: on to the far side, a waypoint that is not reached is never given again
            if (hop == 0 && !line(x, z, ys[from], px, pz)) { // the crossing is out of sight (a 4 x 4 node can hold a wall, a tent's jamb): the way inside the node to it first
                Waypoint in = inside(from, arcCell[arcRev[a]], x, z, portal, arcWidth[a]);
                if (in != null) { routed++; return in; }
            }
            if (hop > 0 && !through && (Math.hypot(px - x, pz - z) > AHEAD || !line(x, z, ys[from], px, pz))) break;
            if (through && !line(x, z, ys[from], px, pz)) { // past the door the next crossing is round a corner (a boulder beside the door): the way inside the far node to it first
                Waypoint in = inside(door, arcCell[arcRev[a]], x, z, portal, arcWidth[a]);
                if (in != null) { routed++; return in; }
            }
            through = false;
            best = new Waypoint(px, ys[cell] + 1, pz, portal, arcWidth[a]);
            if (portal) break;
            n = arcTo[a];
        }
        if (best != null) routed++;
        return best;
    }

    /** The way from cell from to the cell near (the near side of the crossing) inside their node: a breadth-first search over the node's own cells (at most INSIDE of them), and the farthest cell of that way the
     *  troop at (x, z) can see, as a waypoint; the portal flag and width of the arc when that cell is the crossing cell itself. Null: from is near already, or no way found (the caller goes on as before). */
    private Waypoint inside(int from, int near, double x, double z, boolean portal, int width) {
        if (from == near) return null;
        int node = nodeOf[from];
        if (nodeOf[near] != node) return null;
        int[] cell = new int[INSIDE], parent = new int[INSIDE];
        int head = 0, tail = 0;
        cell[tail] = from; parent[tail++] = -1;
        int found = -1;
        int[] DX = {1, -1, 0, 0}, DZ = {0, 0, 1, -1};
        while (head < tail && found < 0) {
            int cur = cell[head++];
            for (int k = 0; k < 4 && found < 0; k++) {
                int nb = step(cur, DX[k], DZ[k]);
                if (nb < 0 || nodeOf[nb] != node) continue;
                boolean seen = false;
                for (int i = 0; i < tail; i++) if (cell[i] == nb) { seen = true; break; }
                if (seen || tail == INSIDE) continue;
                cell[tail] = nb; parent[tail] = head - 1; tail++;
                if (nb == near) found = tail - 1;
            }
        }
        if (found < 0) return null;
        int pick = found;
        for (int i = found; i >= 0; i = parent[i]) {
            if (parent[i] < 0) break;
            int col = colOf[cell[i]];
            pick = i;
            if (line(x, z, ys[from], col % w + ox + .5, col / w + oz + .5)) break;
        }
        int c = cell[pick], col = colOf[c];
        boolean end = pick == found;
        return new Waypoint(col % w + ox + .5, ys[c] + 1, col / w + oz + .5, end && portal, end ? width : CAP);
    }

    /** A waypoint at a portal moved sideways into its own lane: lane is the troop's offset in blocks across the way (WarGrid column minus the middle, times its body's room), clamped to half the door's free width less the
     *  half of one block; the cell it lands on must be a floor of the field a step from the waypoint and in sight of it, else the waypoint stays. Any other waypoint comes back as it is. */
    public synchronized Waypoint lane(Waypoint wp, double fromX, double fromZ, double lane) {
        if (wp == null || !wp.portal() || wp.width() < 2) return wp;
        double hx = wp.x() - fromX, hz = wp.z() - fromZ, len = Math.hypot(hx, hz);
        if (len < 1e-6) return wp;
        double max = (wp.width() - 1) / 2.0, off = Math.max(-max, Math.min(max, lane));
        if (Math.abs(off) < .25) return wp;
        double nx = wp.x() - hz / len * off, nz = wp.z() + hx / len * off;
        int fy = (int) Math.round(wp.y()) - 1;
        int c = cellNear(wp.x(), fy + 1, wp.z(), 0);
        if (c < 0) return wp;
        int bx = (int) Math.floor(nx) - ox, bz = (int) Math.floor(nz) - oz;
        if (bx < 0 || bx >= w || bz < 0 || bz >= d) return wp;
        for (int i = start[bz * w + bx]; i < start[bz * w + bx + 1]; i++)
            if (Math.abs(ys[i] - fy) <= 1 && line(wp.x(), wp.z(), fy, nx, nz)) return new Waypoint(nx, ys[i] + 1, nz, true, wp.width());
        return wp;
    }

    /** How far a rescue looks for a floor, in blocks, and how many hops of the route (4 blocks each) a troop on the route is put on. */
    static final int RESCUE_RADIUS = 24, RESCUE_HOPS = 2, RESCUE_MAX_HOPS = 6;
    /** A rescue on the route moves the troop at least this many blocks (up to RESCUE_MAX_HOPS hops), so that it is not put back in the wedge it is stuck in. */
    static final double RESCUE_MIN = 6;

    /** Where a troop that has been stuck for good is put (the stall ladder's last rung): a standing point on a floor of the field, as a waypoint (feet y), or null when there is none to give (the goal has no floor, no route to it from anywhere near).
     *  A troop on a floor with a way to the goal goes RESCUE_HOPS hops along that way (past whatever the field does not know holds it); a troop off the field (water, a roof, a wall top) or on a piece of floor cut off from the goal
     *  goes to the nearest floor, within RESCUE_RADIUS columns, from which the goal can be reached, ties toward the goal. Builds the goal's field if it must (it is asked for after half a minute of standing still, not every tick). */
    public synchronized Waypoint rescue(double x, double y, double z, double gx, double gy, double gz, long tick) { return rescue(x, y, z, gx, gy, gz, tick, null); }

    /** A place the caller will accept for a troop (not in water, not beside an enemy): (x, feet y, z). */
    public interface SpotFilter { boolean ok(double x, double y, double z); }

    /** rescue, skipping every spot the filter turns down (null: none): the route's hops go on past one, the nearest-floor search takes the next nearest. */
    public synchronized Waypoint rescue(double x, double y, double z, double gx, double gy, double gz, long tick, SpotFilter ok) {
        int to = cellNear(gx, gy, gz, GOAL_RADIUS, false);
        if (to < 0) return null;
        Field f = fieldFor(nodeOf[to], tick, true);
        int from = cellNear(x, y, z, 1);
        if (from >= 0 && f.dist[nodeOf[from]] != FAR) {
            int n = nodeOf[from], cell = -1;
            for (int hop = 0; hop < RESCUE_MAX_HOPS; hop++) {
                int a = f.arc[n];
                if (a < 0) break;
                cell = arcCell[a];
                n = arcTo[a];
                if (hop + 1 >= RESCUE_HOPS) {
                    int col = colOf[cell];
                    if (Math.hypot(col % w + ox + .5 - x, col / w + oz + .5 - z) >= RESCUE_MIN && (ok == null || ok.ok(col % w + ox + .5, ys[cell] + 1, col / w + oz + .5))) break; // far enough to be a real move, not a step into the same wedge
                }
            }
            if (cell < 0) cell = to; // already in the goal's node: the goal's own floor
            int col = colOf[cell];
            return new Waypoint(col % w + ox + .5, ys[cell] + 1, col / w + oz + .5, false, CAP);
        }
        int bx = (int) Math.floor(x) - ox, bz = (int) Math.floor(z) - oz, best = -1;
        double bestScore = Double.MAX_VALUE;
        for (int r = 0; r <= RESCUE_RADIUS; r++) {
            if (best >= 0 && (r - 1.0) * (r - 1.0) > bestScore) break;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                int cx = bx + dx, cz = bz + dz;
                if (cx < 0 || cx >= w || cz < 0 || cz >= d) continue;
                int c = cz * w + cx;
                for (int i = start[c]; i < start[c + 1]; i++) {
                    if (f.dist[nodeOf[i]] == FAR || width(i) < 2) continue;
                    if (ok != null && !ok.ok(cx + ox + .5, ys[i] + 1, cz + oz + .5)) continue;
                    double score = (double) dx * dx + (double) dz * dz + 1e-3 * Math.hypot(cx + ox + .5 - gx, cz + oz + .5 - gz);
                    if (score < bestScore) { bestScore = score; best = i; }
                }
            }
        }
        if (best < 0) return null;
        int col = colOf[best];
        return new Waypoint(col % w + ox + .5, ys[best] + 1, col / w + oz + .5, false, CAP);
    }

    // ---- reading it back

    /** Drops the cached (not the pinned) fields: a map was selected. */
    public synchronized void reset() { cache.clear(); budgetTick = Long.MIN_VALUE; builtThisTick = 0; roundBuilds = builds; roundRefused = refused; roundLookups = lookups; roundRouted = routed; }
    /** One line for the log, what this field did since the last call (a round, or a map select): fields built, lookups refused by the budget, lookups, lookups answered with a waypoint, fields held. */
    public synchronized String roundStats() {
        String t = String.format(java.util.Locale.ROOT, "%d lookups (%d with a waypoint), %d fields built (%.2f ms each), %d refused by the build budget, %d cached (cap %d) + %d pinned", lookups - roundLookups, routed - roundRouted, builds - roundBuilds, builds == 0 ? 0.0 : buildNanos / 1e6 / builds, refused - roundRefused, cache.size(), cacheCap(), pinned.size());
        roundBuilds = builds; roundRefused = refused; roundLookups = lookups; roundRouted = routed;
        return t;
    }
    /** Whether a troop at (x, feet y, z) stands on a floor of the field (a floor within 2 blocks of its feet, in its column or a neighbour): false for a roof with no stair, a fall, a wall top. */
    public synchronized boolean onField(double x, double y, double z) { return cellNear(x, y, z, 1) >= 0; }
    /** The distance from a troop to a goal by the fields already held (pinned or cached), never a build: -1 not reachable, NaN none held for that goal or an end off the field. */
    public synchronized double cachedDistance(double x, double y, double z, double gx, double gy, double gz) {
        int a = cellNear(x, y, z, 1), b = cellNear(gx, gy, gz, GOAL_RADIUS, false);
        if (a < 0 || b < 0) return Double.NaN;
        Field f = pinned.get(nodeOf[b]);
        if (f == null) f = cache.get(nodeOf[b]);
        if (f == null) return Double.NaN;
        int n = nodeOf[a];
        if (f.dist[n] == FAR) return -1;
        // continuous (a node is 4 blocks, and a troop crawling in a file moves inside one): the next node's distance plus the way to it; the goal itself in the goal's own node
        int arc = f.arc[n];
        if (arc < 0) return Math.hypot(x - gx, z - gz);
        int m = arcTo[arc];
        return f.dist[m] / SCALE + Math.hypot(x - nodeX[m], z - nodeZ[m]);
    }
    /** Whether a troop at (x, feet y, z) sees (bx, bz) over the floors (the test route() makes of its waypoints). */
    public synchronized boolean sees(double x, double y, double z, double bx, double bz) {
        int c = cellNear(x, y, z, 1);
        return c >= 0 && line(x, z, ys[c], bx, bz);
    }

    public int nodeCount() { return nodes; }
    /** The portal arcs: how many, and how many are narrower than 4 (doors). For the report. */
    public int[] portalCounts() {
        int p = 0, narrow = 0;
        for (int a = 0; a < arcs; a++) if (arcWidth[a] < PORTAL) { p++; if (arcWidth[a] < 4) narrow++; }
        return new int[]{p / 2, narrow / 2};
    }
    /** Whether (wx, wz) has a floor that is part of the reachable map. */
    public boolean standable(int wx, int wz) { return has(wx, wz); }
    /** The distance a field gives from cell to goal, in blocks, or -1: for the checks. */
    public synchronized double distance(double x, double y, double z, double gx, double gy, double gz) {
        int a = cellNear(x, y, z, 1), b = cellNear(gx, gy, gz, GOAL_RADIUS, false);
        if (a < 0 || b < 0) return -1;
        Field f = pinned.getOrDefault(nodeOf[b], cache.get(nodeOf[b]));
        if (f == null) { f = build(nodeOf[b]); cache.put(nodeOf[b], f); }
        return f.dist[nodeOf[a]] == FAR ? -1 : f.dist[nodeOf[a]] / SCALE;
    }
    /** Heap the tables hold, in bytes (not the cached fields). */
    public long bytes() {
        return 4L * start.length + 2L * ys.length + 4L * colOf.length + 4L * nodeOf.length + 8L * nodes + 4L * (arcStart.length + arcTo.length + arcCell.length + arcRev.length) + arcWidth.length;
    }
    /** Bytes one distance field takes. */
    public long fieldBytes() { return 6L * nodes; }
    public int pinnedFields() { return pinned.size(); }
    public String summary() {
        int[] p = portalCounts();
        return String.format(java.util.Locale.ROOT, "%d floor cells, %d nodes, %d arcs, %d portals (%d under 4 wide), %d pinned fields, tables %d KiB, %d KiB a field, built in %.0f ms",
            cells, nodes, arcs, p[0], p[1], pinned.size(), bytes() / 1024, fieldBytes() / 1024, bakeNanos / 1e6);
    }
}
