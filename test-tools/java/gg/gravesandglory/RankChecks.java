package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

import static gg.gravesandglory.Phase2Checks.report;
import static gg.gravesandglory.Phase2Checks.server;
import static gg.gravesandglory.Phase2Checks.shot;

/**
 * Suite ranks of the in-match progression plan (Task 9, lane C): tiers drawn from rank_v1, the art stopgap, the rank chevrons and
 * portraits. Never included in releases. Phase2Checks.SUITES takes it through register (GRAVES_PHASE2_CHECKS=ranks), and the checks
 * print through Phase2Checks.report. It runs on the art suites' sky platform (Phase2Checks.Stage), gold against violet, the troops
 * posted with Battle.spawn and ranked on the server through Stage.stage (RankSync.changed), so every rank the client draws came
 * over rank_v1.
 * <p>The Squire (troop knight) draws through GeckoLib (GeoTroopRenderer.handles), the renderer going forward: its draws, the sheet
 * it binds and the rig bones its tiers show are read from GeoTroopRenderer.PROBE (geo), never from TroopRenderer.drawn, which only
 * the legacy path records. The Revenant and the Sunweaver still draw through the legacy renderer, and their checks
 * keep reading TroopRenderer.drawn.
 */
final class RankChecks {
    private RankChecks() {}

    /** Adds this file's suites to Phase2Checks.SUITES. */
    static void register(Map<String, Supplier<Phase2Checks.Suite>> suites) {
        suites.put("ranks", Ranks::new);
    }

    static final String TEX = "gravesandglory:textures/entity/troop/";
    static final String SWORD = "arming_sword", SWORD_T2 = "arming_sword_t2", SWORD_T3 = "arming_sword_t3";

    /**
     * The ranks suite. Scene A (12 blocks from the camera): a level-2 gold Squire, a level-3 violet Squire, a level-1 gold Squire
     * and a level-3 violet Revenant (a Graveborn troop, drawn by the legacy renderer with its own model, not GeckoLib). Scene B (5 blocks):
     * three Squires at 1/1, 2/2 and 3/3, then the Sunweaver (an Epic, its tiers painted since 6a041963), then the cost row.
     */
    static final class Ranks extends Phase2Checks.Stage {
        String name() { return "ranks"; }
        static final double FAR = 12;
        GravesAndGlory.Unit gk2, vk3, gk1, vr3, mage;
        final List<GravesAndGlory.Unit> tiers = new ArrayList<>(), crowd = new ArrayList<>();
        volatile boolean syncSet, syncBack, sceneB, mageUp, crowdUp, vetSet, cleared; boolean syncReported;
        volatile int registerBefore, clearBefore, onceQuiet = -1, onceLoud = -1, onceWant; volatile boolean onceDone;
        int marks, seenAt = -1, t0;
        long markTime;
        final Map<Entity, Integer> counts = new HashMap<>();
        Phase2Checks.PortraitProbe probe; MobEntity portrait;
        long nanosBefore, drawsBefore, flushBefore;

        void cast(GravesAndGlory.Battle b) {
            gk2 = post(b, gold, "knight", 3, FAR, 180); stage(gk2, 2);
            vk3 = post(b, violet, "knight", 1, FAR, 180); stage(vk3, 3);
            gk1 = post(b, gold, "knight", -1, FAR, 180);
            vr3 = post(b, violet, "revenant", -3, FAR, 180); stage(vr3, 3);
            camera(b, 0, 1.6, 0, 0, 4);
        }
        static Entity e(MinecraftClient c, GravesAndGlory.Unit u) { return u == null ? null : client(c, u.mob); }
        static TroopRenderer.Draw last(Entity e) { var d = e == null ? null : TroopRenderer.drawn(e); return d == null ? null : d.last; }
        /** Every draw of the entity, through either path: the legacy model's (TroopRenderer.drawn) and GeckoLib's (geo). */
        static int drawCount(Entity e) { if (e == null) return 0; var d = TroopRenderer.drawn(e); var g = GEO.get(e); return (d == null ? 0 : d.count) + (g == null ? 0 : g.count); }

        /** The rig bones whose showing depends on the troop's tiers (troop_rig/knight.json): the helms by armour tier, the swords by weapon tier. */
        static final List<String> TIERED = List.of("helm_squire", "helm_great", "helm_crown", "crest", "sword_t1", "sword_t2", "sword_t3");
        /** One GeckoLib draw as GeoTroopRenderer.PROBE saw it, right after it: the Look drawn, the sheet bound and the tiered bones shown (TIERED order). */
        record GeoDraw(TroopModels.Look look, Identifier texture, List<String> shown) {
            String swords() { return String.join(" ", shown.stream().filter(b -> b.startsWith("sword")).toList()); }
            String helms() { return String.join(" ", shown.stream().filter(b -> !b.startsWith("sword")).toList()); }
            /** The sheet's folder and file, e.g. knight_geo/t3_gold.png. */
            String sheet() { if (texture == null) return "none"; String p = texture.getPath(); return p.substring(p.lastIndexOf('/', p.lastIndexOf('/') - 1) + 1); }
        }
        static final class GeoSeen { int count; GeoDraw last; }
        /** GeckoLib draws per entity, by identity (a portrait may share a world troop's id). */
        static final Map<Entity, GeoSeen> GEO = new com.google.common.collect.MapMaker().weakKeys().makeMap();
        static GeoTroopRenderer.Probe probeBefore; static boolean probing;
        static java.lang.reflect.Field geoRenderers;
        /** The entity's last GeckoLib draw, or null when GeckoLib never drew it. */
        static GeoDraw geo(Entity e) { var g = e == null ? null : GEO.get(e); return g == null ? null : g.last; }
        /** Hooks GeoTroopRenderer.PROBE (handing every draw on to a probe already there) to count each GeckoLib draw and keep what it drew. */
        static void installProbe() {
            if (probing) return; probing = true; var prev = probeBefore = GeoTroopRenderer.PROBE;
            GeoTroopRenderer.PROBE = (Entity e, BakedGeoModel model, float td) -> {
                if (prev != null) prev.seen(e, model, td);
                var l = TroopModels.resolve(e); if (l == null) return;
                List<String> shown = new ArrayList<>(); for (String b : TIERED) model.getBone(b).ifPresent(x -> { if (!x.isHidden()) shown.add(b); });
                var s = GEO.computeIfAbsent(e, k -> new GeoSeen()); s.count++; s.last = new GeoDraw(l, geoTexture(e, l), shown);
            };
        }
        static void removeProbe() { if (!probing) return; GeoTroopRenderer.PROBE = probeBefore; probeBefore = null; probing = false; }
        /** The sheet GeoTroopRenderer bound for this draw: its model's texture, set by draw just before it rendered (TroopRenderer keeps one GeoTroopRenderer per troop id). */
        @SuppressWarnings("unchecked")
        static Identifier geoTexture(Entity e, TroopModels.Look l) {
            try {
                var r = MinecraftClient.getInstance().getEntityRenderDispatcher().getRenderer(e); if (!(r instanceof TroopRenderer<?> tr)) return null;
                if (geoRenderers == null) { var f = TroopRenderer.class.getDeclaredField("geo"); f.setAccessible(true); geoRenderers = f; }
                var g = ((Map<String, GeoTroopRenderer<?>>) geoRenderers.get(tr)).get(l.troop().id());
                return g == null ? null : ((GeoTroopRenderer.Model) g.getGeoModel()).texture;
            } catch (ReflectiveOperationException ex) { return null; }
        }
        /** The helm bones the knight rig shows at each armour tier (troop_rig/knight.json). */
        static String helmsAt(int armour) { return armour >= 3 ? "helm_great helm_crown crest" : armour == 2 ? "helm_great" : "helm_squire"; }
        static String geoText(GeoDraw g) { return g == null ? "no GeckoLib draw" : "sheet " + g.sheet() + ", helm " + g.helms() + ", sword " + g.swords(); }
        static String rank(RankSync.Rank r) { return r == null ? "none" : r.level() + "/" + r.weaponTier() + "/" + r.armourTier(); }
        static String file(Identifier id) { if (id == null) return "none"; String p = id.getPath(); return p.substring(p.lastIndexOf('/') + 1); }
        static String mark(TroopChevrons.Mark m) { return m == null ? "no chevron" : m.chevrons() + " chevron(s) in #" + String.format("%06X", m.colour()) + String.format(Locale.ROOT, " at %.1f blocks", m.distance()); }
        /** The top of a GeckoLib troop rig's helm crown and crest cubes at rest (geo/troop/<id>.geo.json, 16 px a block), in blocks over the feet; -1 when unread. */
        static double geoCrown(MinecraftClient c, String troop) {
            var res = c.getResourceManager().getResource(new Identifier(TroopModels.NAMESPACE, "geo/troop/" + troop + ".geo.json")); if (res.isEmpty()) return -1;
            try (var in = new InputStreamReader(res.get().getInputStream(), StandardCharsets.UTF_8)) {
                double top = -1;
                for (var bone : JsonParser.parseReader(in).getAsJsonObject().getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
                    var b = bone.getAsJsonObject(); String n = b.get("name").getAsString(); if (!n.equals("helm_crown") && !n.equals("crest") || !b.has("cubes")) continue;
                    for (var cube : b.getAsJsonArray("cubes")) { var q = cube.getAsJsonObject(); top = Math.max(top, (q.getAsJsonArray("origin").get(1).getAsDouble() + q.getAsJsonArray("size").get(1).getAsDouble()) / 16.0); }
                }
                return top;
            } catch (Exception ex) { return -1; }
        }
        /** Marks every scene entity's draw count, so a later check reads only draws made after this tick. */
        void markDraws(MinecraftClient c, List<GravesAndGlory.Unit> us) { counts.clear(); for (var u : us) { var x = e(c, u); if (x != null) counts.put(x, drawCount(x)); } }
        /** Whether each of these entities drew at least n times since markDraws. */
        boolean drewSince(MinecraftClient c, List<GravesAndGlory.Unit> us, int n) { for (var u : us) { var x = e(c, u); if (x == null || drawCount(x) - counts.getOrDefault(x, 0) < n) return false; } return true; }
        /** A copy of a weapon model under another id: a tier model the art steps have not drawn yet. */
        static TroopModels.Weapon renamed(TroopModels.Weapon w, String id) { return new TroopModels.Weapon(id, w.frames(), w.far(), w.pull(), w.grip(), w.nocks(), w.texture(), w.glow(), w.rot()); }
        /** The shipped arming_sword_t2 (121baca paints it), held while the stopgap check takes it away, and put back after: a later suite in the same client draws the real weapon, not none. */
        static TroopModels.Weapon shippedT2; static boolean tookT2;
        /** Puts arming_sword_t2 back as it shipped (none when it did not ship). */
        static void restoreT2() { TroopModels.addWeapon(SWORD_T2, shippedT2); }

        public void step(MinecraftClient c, int tick) {
            installProbe();
            if (tick == 20) { c.setScreen(null); UUID id = c.player.getUuid(); server(c, b -> { player = id; stage = 1; }); }
            if (finished) return;
            if (tick > 12000) { report("p2-ranks-timeout", false, "stuck at client step " + cs + ", server stage " + stage); end(c); return; }
            switch (cs) {
                case 0 -> { // the platform lit and scene A on this client, every troop drawn with its rank in
                    boolean lit = litStart(c);
                    if (lit && ready && posted.size() == 4 && posted.stream().allMatch(u -> { var x = e(c, u); return x != null && x.getCustomName() != null && RankSync.client(x) != null; })) { c.options.hudHidden = true; wait = 0; cs = 1; }
                    else if (++wait % 600 == 0) System.out.println("[p2-wait] ranks step 0 after " + wait + " ticks: ready=" + ready + ", posted " + posted.size() + "; " + litWhy(c));
                }
                case 1 -> { wait++; // p2-rank-sync: the server sets the level-1 Squire to 3/2/3; within 2 ticks this client reads it
                    if (wait == 1) localIdentity(c);
                    if (wait == 20) server(c, b -> { stage(gk1, 3, 2, 3); syncSet = true; });
                    if (syncSet && t0 == 0) t0 = wait; // the first client tick that sees the server's set done
                    if (syncSet && seenAt < 0 && new RankSync.Rank(3, 2, 3).equals(RankSync.client(e(c, gk1)))) seenAt = wait;
                    if (syncSet && (seenAt >= 0 || wait - t0 > 40) && !syncReported) { syncReported = true;
                        int after = seenAt < 0 ? -1 : seenAt - t0;
                        report("p2-rank-sync", seenAt >= 0 && after <= 2, "the server set the Squire (entity " + gk1.mob.getId() + ") to 3/2/3; this client's RankSync.client read " + rank(RankSync.client(e(c, gk1)))
                            + (seenAt < 0 ? " and never 3/2/3 in 40 ticks" : " " + after + " client tick(s) after the set was done (want 2 or fewer)"));
                        server(c, b -> { stage(gk1, 1, 1, 1); syncBack = true; });
                    }
                    if (syncBack && Objects.equals(RankSync.client(e(c, gk1)), RankSync.Rank.FRESH)) { markDraws(c, posted); wait = 0; cs = 2; }
                }
                case 2 -> { wait++; // p2-chevrons-both-teams and the 12-block shot
                    if (wait % 600 == 0) System.out.println("[p2-wait] ranks step 2 after " + wait + " ticks: Squires drew (legacy or GeckoLib) since the mark=" + drewSince(c, List.of(gk2, vk3, gk1), 5) + ", Revenant chevron " + mark(TroopChevrons.drawn(e(c, vr3))));
                    if (wait >= 40 && drewSince(c, List.of(gk2, vk3, gk1), 5)) { // the Revenant draws through the legacy renderer (its own model), not GeckoLib
                        if (!shotReady(c, "ranks-chevrons")) { wait--; return; }
                        var a = TroopChevrons.drawn(e(c, gk2)); var bb = TroopChevrons.drawn(e(c, vk3)); var one = TroopChevrons.drawn(e(c, gk1)); var r = TroopChevrons.drawn(e(c, vr3));
                        int goldC = TroopChevrons.colour("gold"), violetC = TroopChevrons.colour("violet");
                        long now = c.world.getTime();
                        boolean fresh = a != null && bb != null && r != null && now - a.time() < 5 && now - bb.time() < 5 && now - r.time() < 5;
                        boolean ok = fresh && a.chevrons() == 1 && a.colour() == goldC && bb.chevrons() == 2 && bb.colour() == violetC && (one == null || now - one.time() > 30)
                            && r.chevrons() == 2 && r.colour() == violetC && geo(e(c, vr3)) == null && Math.abs(a.distance() - FAR) < 1.0
                            && geo(e(c, gk2)) != null && geo(e(c, vk3)) != null;
                        var tex = c.getResourceManager().getResource(TroopChevrons.TEXTURE); int[] size = {0, 0};
                        if (tex.isPresent()) try (var in = tex.get().getInputStream(); var img = net.minecraft.client.texture.NativeImage.read(in)) { size[0] = img.getWidth(); size[1] = img.getHeight(); } catch (Exception ex) { size[0] = -1; }
                        boolean texOk = size[0] == TroopChevrons.TEX_W && size[1] == TroopChevrons.TEX_H;
                        shot(c, "ranks-chevrons");
                        report("p2-chevrons-both-teams", ok && texOk, "level-2 gold Squire: " + mark(a) + " (want 1 in gold #" + String.format("%06X", goldC) + "); level-3 violet Squire: " + mark(bb) + " (want 2 in violet #" + String.format("%06X", violetC)
                            + "); level-1 gold Squire: " + (one == null || now - one.time() > 30 ? "no chevron" : mark(one) + " (BAD)") + "; Squires drawn by GeckoLib=" + (geo(e(c, gk2)) != null && geo(e(c, vk3)) != null) + "; level-3 violet Revenant (not GeckoLib: legacy model drawn=" + (TroopRenderer.drawn(e(c, vr3)) != null) + ", GeckoLib drawn=" + (geo(e(c, vr3)) != null) + "): " + mark(r)
                            + "; chevron.png " + size[0] + "x" + size[1] + " (want " + TroopChevrons.TEX_W + "x" + TroopChevrons.TEX_H + "); picture p2-ranks-chevrons.png at about " + (int) FAR + " blocks");
                        // C15-F2: the level-3 violet Squire wears armour tier 3 and draws through GeckoLib; its chevrons sit clear of the rig's crown and crest
                        double crown = geoCrown(c, "knight"); boolean geo = GeoTroopRenderer.handles(TroopModels.resolve(e(c, vk3)));
                        report("p2-chevron-above-crown", geo && bb != null && crown > 0 && bb.base() >= crown, "level-3 violet Squire drawn by GeckoLib=" + geo + ": its first chevron's bottom edge "
                            + (bb == null ? "not drawn" : String.format(Locale.ROOT, "%.3f", bb.base())) + String.format(Locale.ROOT, " blocks over its feet; the Geo rig's crown and crest top %.3f blocks (want the edge at or above it)", crown));
                        TroopDepth.enabled = false; markTime = now; wait = 0; cs = 3;
                    }
                }
                case 3 -> { wait++; // p2-chevron-depth-off: with the Visuals switch off the chevrons still draw
                    if (wait == 20) {
                        var a = TroopChevrons.drawn(e(c, gk2)); var bb = TroopChevrons.drawn(e(c, vk3));
                        boolean ok = !TroopDepth.enabled && a != null && bb != null && a.time() > markTime && bb.time() > markTime && a.chevrons() == 1 && bb.chevrons() == 2;
                        report("p2-chevron-depth-off", ok, "with TroopDepth.enabled false for 20 ticks: gold Squire " + mark(a) + " last at world time " + (a == null ? "-" : a.time()) + ", violet Squire " + mark(bb) + " last at " + (bb == null ? "-" : bb.time()) + " (the switch went off at " + markTime + ")");
                        TroopDepth.enabled = true;
                        server(c, b -> camera(b, 0, 1.6, FAR - 23, 0, 3)); wait = 0; cs = 4;
                    }
                }
                case 4 -> { wait++; // p2-chevron-range: drawn at 23 blocks (shot), none at 26 while the troops still draw
                    if (wait == 40) { if (!shotReady(c, "ranks-chevrons-24")) { wait--; return; }
                        var a = TroopChevrons.drawn(e(c, gk2)); var bb = TroopChevrons.drawn(e(c, vk3)); long now = c.world.getTime();
                        boolean near = a != null && bb != null && now - a.time() < 5 && now - bb.time() < 5 && a.distance() <= TroopChevrons.range() && a.distance() > 22;
                        shot(c, "ranks-chevrons-24"); t0 = near ? 1 : 0;
                        System.out.println("[p2] ranks: at the range edge: gold " + mark(a) + ", violet " + mark(bb));
                        server(c, b -> camera(b, 0, 1.6, FAR - 26, 0, 3)); markTime = now;
                    }
                    if (wait == 50) markDraws(c, List.of(gk2, vk3));
                    if (wait == 80) {
                        var a = TroopChevrons.drawn(e(c, gk2)); var bb = TroopChevrons.drawn(e(c, vk3)); var x = e(c, gk2);
                        double d = x == null || c.player == null ? -1 : Math.sqrt(c.gameRenderer.getCamera().getPos().squaredDistanceTo(x.getPos()));
                        boolean drawn = drewSince(c, List.of(gk2, vk3), 5);
                        long stale = c.world.getTime() - 30;
                        boolean far = a != null && bb != null && a.time() < stale && bb.time() < stale;
                        report("p2-chevron-range", t0 == 1 && drawn && far && d > TroopChevrons.range(), "inside the range at about 23 blocks both drew their chevrons=" + (t0 == 1) + " (picture p2-ranks-chevrons-24.png); with the camera "
                            + String.format(Locale.ROOT, "%.1f", d) + " blocks from the gold Squire (range " + TroopChevrons.range() + ") both troops still drew=" + drawn + " and their last chevrons are from world time " + (a == null ? "-" : a.time()) + " and " + (bb == null ? "-" : bb.time()) + " (before " + stale + ")=" + far);
                        wait = 0; cs = 5;
                    }
                }
                case 5 -> { wait++; // p2-rank-late-join: the store emptied, then the channel registers late (the guest's path); every tracked soldier's rank comes back
                    if (wait == 1) { server(c, b -> camera(b, 0, 1.6, 0, 0, 4)); }
                    if (wait == 30) { RankSync.clear(); registerBefore = RankSync.registerSends;
                        server(c, b -> { var p = sp(b); if (p != null) S2CPlayChannelEvents.REGISTER.invoker().onChannelRegister(p.networkHandler, ServerPlayNetworking.getSender(p), b.server, List.of(RankSync.CHANNEL)); }); }
                    if (wait == 50) {
                        boolean ok = true; StringBuilder why = new StringBuilder();
                        for (var u : posted) { var x = e(c, u); var r = x == null ? null : RankSync.client(x); var want = RankSync.of(u); boolean good = want.equals(r); ok &= good; why.append(u.spec.id()).append(" ").append(rank(r)).append(good ? "" : " (want " + rank(want) + ")").append("; "); }
                        int sent = RankSync.registerSends - registerBefore;
                        report("p2-rank-late-join", ok && sent == posted.size(), "after RankSync.clear() on this client and a late rank_v1 registration (S2CPlayChannelEvents.REGISTER), the server sent " + sent + " rank(s) (want " + posted.size() + ", every tracked troop) and the store reads " + why.toString().trim());
                        server(c, b -> { clearPosted(b); tiers.clear();
                            for (int s = 1; s <= 3; s++) { var u = post(b, gold, "knight", (2 - s) * 2.0, Phase2Checks.GildedArt.STUDIO, 180 + 20); stage(u, s, s, s); tiers.add(u); }
                            camera(b, 0, 1.6, Phase2Checks.GildedArt.STUDIO - 5, 0, 8); sceneB = true; });
                        wait = 0; cs = 6;
                    }
                }
                case 6 -> { wait++; // p2-armour-tier-skin and the ranks-tiers shot: the Squire at 1/1, 2/2 and 3/3; through GeckoLib each tier binds its own sheet (knight_geo/t<n>_gold) and shows its own helm and sword bones (sword_t1, _t2, _t3)
                    if (!sceneB || tiers.size() < 3 || tiers.stream().anyMatch(u -> e(c, u) == null)) { wait = 0; return; }
                    if (wait == 2) markDraws(c, tiers);
                    if (wait >= 50 && drewSince(c, tiers, 5)) { if (!shotReady(c, "ranks-tiers")) { wait--; return; }
                        boolean ok = true; StringBuilder why = new StringBuilder();
                        for (var u : tiers) { var x = e(c, u); var g = geo(x); int t = u.armourTier;
                            boolean good = g != null && g.look().armourTier() == t && g.look().weaponTier() == t && g.sheet().equals("knight_geo/t" + t + "_gold.png") && g.helms().equals(helmsAt(t)) && g.swords().equals("sword_t" + t) && Objects.equals(RankSync.client(x), RankSync.of(u));
                            ok &= good; why.append(rank(RankSync.client(x))).append(": ").append(geoText(g)).append(good ? "" : " (BAD)").append("; "); }
                        shot(c, "ranks-tiers");
                        report("p2-armour-tier-skin", ok, "Squires at rank_v1 1/1/1, 2/2/2 and 3/3/3 drawn by GeckoLib bind knight_geo/t1_gold, t2_gold and t3_gold, show helm_squire, helm_great and helm_great with helm_crown and crest, and swords sword_t1, sword_t2 and sword_t3: " + why.toString().trim() + " picture p2-ranks-tiers.png");
                        markDraws(c, tiers); wait = 0; cs = 7;
                    }
                }
                case 7 -> { wait++; // p2-weapon-tier-model: tiers 1, 2 and 3 show their own sword bones; without the tier 2 model the legacy weapon lookup falls to tier I (the rig keeps its own)
                    if (wait >= 10 && drewSince(c, tiers, 3) && marks == 0) {
                        String[] got = new String[3]; for (int i = 0; i < 3; i++) { var g = geo(e(c, tiers.get(i))); got[i] = g == null ? "none" : g.swords(); }
                        var three = TroopModels.weapon(SWORD, 3); int art3 = TroopModels.weaponArt(SWORD, 3);
                        boolean with = "sword_t1".equals(got[0]) && "sword_t2".equals(got[1]) && "sword_t3".equals(got[2]) && three != null && SWORD_T3.equals(three.id()) && art3 == 3;
                        report("p2-weapon-tier-model", with, "the Squires at weapon tiers 1, 2 and 3 drawn by GeckoLib show " + Arrays.toString(got) + " (want sword_t1, sword_t2, sword_t3, one each); for the legacy renderer TroopModels.weapon(arming_sword, 3) is " + (three == null ? "none" : three.id()) + ", weaponArt 3 -> " + art3 + " (want arming_sword_t3, 3)");
                        shippedT2 = TroopModels.weapon(SWORD_T2); tookT2 = true; TroopModels.addWeapon(SWORD_T2, null); markDraws(c, tiers); marks = 1;
                    }
                    if (marks == 1 && drewSince(c, tiers, 3)) {
                        var g = geo(e(c, tiers.get(1))); var w = TroopModels.weapon(SWORD, 2);
                        boolean back = w != null && SWORD.equals(w.id()) && TroopModels.weaponArt(SWORD, 2) == 1 && g != null && "sword_t2".equals(g.swords());
                        report("p2-weapon-tier-stopgap", back, "without arming_sword_t2 the legacy renderer's weapon for tier 2, TroopModels.weapon(arming_sword, 2), is " + (w == null ? "none" : w.id()) + ", weaponArt 2 -> " + TroopModels.weaponArt(SWORD, 2)
                            + " (want arming_sword, 1: a missing tier draws the highest tier below it); the GeckoLib tier 2 Squire, whose rig carries its own tier swords, still shows " + (g == null ? "none" : g.swords()) + " (want sword_t2)");
                        restoreT2(); marks = 0; server(c, b -> { var u = tiers.get(2); u.mob.setCustomName(net.minecraft.text.Text.literal(ArmyMobs.levelName(u.spec, 3) + ArmyMobs.VETERAN)); stage(u, 3, 3, 3); vetSet = true; });
                        markDraws(c, tiers); wait = 0; cs = 8;
                    }
                }
                case 8 -> { wait++; // p2-veteran-over-tier: a Mastery II Squire at armour III draws its tier III sheet and helm as a Veteran (the GeckoLib knight draws no trim layer)
                    var vx = e(c, tiers.get(2));
                    if (!vetSet || vx == null || vx.getCustomName() == null || !vx.getCustomName().getString().endsWith(ArmyMobs.VETERAN)) { wait = 0; return; }
                    if (wait == 2) markDraws(c, tiers);
                    if (wait >= 10 && drewSince(c, tiers, 3)) {
                        var x = e(c, tiers.get(2)); var g = geo(x); var l = last(x);
                        boolean ok = g != null && g.look().veteran() && g.look().armourTier() == 3 && g.sheet().equals("knight_geo/t3_gold.png") && g.helms().equals(helmsAt(3)) && l == null;
                        report("p2-veteran-over-tier", ok, "\"" + (x == null || x.getCustomName() == null ? "" : x.getCustomName().getString()) + "\" at rank " + rank(RankSync.client(x)) + " drawn by GeckoLib as Veteran=" + (g != null && g.look().veteran()) + ": " + geoText(g)
                            + ", legacy model drawn=" + (l != null) + " (want knight_geo/t3_gold with helm_great, helm_crown and crest as a Veteran, and no legacy draw, so no trim: the GeckoLib knight has no trim layer; p2-veteran-trim-tier draws a trim over tier III)");
                        server(c, b -> { mage = post(b, gold, "mage", -4.5, Phase2Checks.GildedArt.STUDIO, 180 + 20); mage.mob.setCustomName(net.minecraft.text.Text.literal(ArmyMobs.levelName(mage.spec, 3) + ArmyMobs.VETERAN)); stage(mage, 3, 3, 3); mageUp = true; });
                        wait = 0; cs = 9;
                    }
                }
                case 9 -> { wait++; // p2-tier-stopgap: the Sunweaver at armour III draws its own painted _s3 (6a041963; every troop model now has skins 1 to 3, so the stopgap has no live case); its two chevrons still show the level
                    if (!mageUp || e(c, mage) == null) { wait = 0; return; }
                    if (wait == 2) markDraws(c, List.of(mage));
                    if (wait >= 30 && drewSince(c, List.of(mage), 5)) {
                        var x = e(c, mage); var l = last(x); var m = TroopModels.model("mage"); var ch = TroopChevrons.drawn(x);
                        boolean ok = l != null && l.look().armourTier() == 3 && file(l.skin()).equals("mage_s3.png") && m != null && m.armourArt(3) == 3 && m.ownSkins().contains(3)
                            && (l.relief() == null || l.relief().equals("3")) && ch != null && ch.chevrons() == 2 && c.world.getTime() - ch.time() < 5 && "sun_staff_t3".equals(l.weaponId());
                        boolean trim = l != null && l.look().veteran() && l.look().armourTier() == 3 && file(l.skin()).equals("mage_s3.png") && String.valueOf(l.trim()).replace("_far.png", ".png").equals(TEX + "mage/mage_veteran.png");
                        report("p2-veteran-trim-tier", trim, "the Sunweaver, a Mastery II Veteran at armour III, drew skin " + (l == null ? "none" : file(l.skin())) + " with trim " + (l == null ? "none" : String.valueOf(l.trim())) + " (want mage_s3, the tier's own skin since 6a041963, under " + TEX + "mage/mage_veteran.png: a troop's trim draws over whichever armour tier it wears)");
                        report("p2-tier-stopgap", ok, "the Sunweaver at rank " + rank(RankSync.client(x)) + " (own skins " + (m == null ? "-" : m.ownSkins()) + ") drew skin " + (l == null ? "none" : file(l.skin())) + ", relief key " + (l == null ? "-" : l.relief()) + ", weapon " + (l == null ? "none" : l.weaponId())
                            + " (want mage_s3, armourArt 3, relief 3 or flat, sun_staff_t3: the Sunweaver's tier III skin is painted since 6a041963 and every troop model has skins 1 to 3, so the missing-tier stopgap has no live case) and " + mark(ch) + " (want 2)");
                        wait = 0; cs = 10;
                    }
                }
                case 10 -> { // p2-weapon-parts-tier: a weaponTiers part set switches with the weapon tier (no weaponless troop has a model yet: a fixture of the Squire's model)
                    weaponParts(c); wait = 0; cs = 11;
                }
                case 11 -> { wait++; // p2-portrait-tiers: ArmyCards.portrait(knight, 2, 3) draws, through GeckoLib, the tier-3 sheet and the tier-2 sword
                    if (wait == 1) { var w = TroopModels.weapon(SWORD); if (w != null && TroopModels.weapon(SWORD_T2) == null) TroopModels.addWeapon(SWORD_T2, renamed(w, SWORD_T2)); // the shipped one when it is there, else a fixture
                        portrait = ArmyCards.portrait(ArmyCards.spec("knight"), 2, 3); probe = new Phase2Checks.PortraitProbe(List.of(portrait)); c.setScreen(probe); marks = drawCount(portrait); }
                    if (wait >= 20 && drawCount(portrait) - marks >= 5) {
                        var g = geo(portrait); var plain = ArmyCards.portrait(ArmyCards.spec("knight"), 1, 1);
                        boolean ok = g != null && g.look().weaponTier() == 2 && g.look().armourTier() == 3 && g.sheet().startsWith("knight_geo/t3_") && g.helms().equals(helmsAt(3)) && "sword_t2".equals(g.swords())
                            && new RankSync.Rank(1, 2, 3).equals(RankSync.client(portrait)) && plain != portrait && TroopChevrons.drawn(portrait) == null;
                        report("p2-portrait-tiers", ok, "the portrait at weapon 2, armour 3 (RankSync.client " + rank(RankSync.client(portrait)) + ") drawn by GeckoLib: " + geoText(g)
                            + " (want a knight_geo/t3 sheet, the tier III helm and sword_t2), no chevron=" + (TroopChevrons.drawn(portrait) == null) + ", cached apart from the 1/1 portrait=" + (plain != portrait));
                        restoreT2(); c.setScreen(null); marks = 0; wait = 0; cs = 12;
                        server(c, b -> { for (int i = 0; i < 24; i++) { var u = post(b, i % 2 == 0 ? gold : violet, "knight", (i % 8 - 3.5) * 1.4, Phase2Checks.GildedArt.STUDIO + 3 + (i / 8) * 1.6, 180); stage(u, 3); crowd.add(u); } crowdUp = true; });
                    }
                }
                case 12 -> { wait++; // p2-chevron-cost: 24 level-3 soldiers (27 veterans in view): what the chevrons add to a frame
                    if (!crowdUp || crowd.stream().anyMatch(u -> e(c, u) == null)) { wait = 0; return; }
                    if (wait == 40) { nanosBefore = TroopChevrons.nanos; drawsBefore = TroopChevrons.draws; flushBefore = TroopChevrons.flushNanos; }
                    if (wait == 240) {
                        // The chevron's own cost leaves out the draw of the troop's last layer that getBuffer makes (TroopChevrons.flushNanos): the troop's next
                        // layer switch would make it anyway. Since the hand-authored Gilded (121baca) the knight's shield is its own bone, so its last layer is
                        // its sword's entity cutout, which has no fixed buffer; the vanilla shield item it held before ended on a fixed layer, drawn later.
                        long draws = TroopChevrons.draws - drawsBefore, flush = TroopChevrons.flushNanos - flushBefore, own = TroopChevrons.nanos - nanosBefore - flush;
                        double us = draws == 0 ? -1 : own / 1e3 / draws, flushUs = draws == 0 ? -1 : flush / 1e3 / draws;
                        double perTick = own / 1e6 / 200;
                        boolean ok = draws >= 1000 && us >= 0 && us <= 25 && perTick <= 0.75;
                        report("p2-chevron-cost", ok, String.format(Locale.ROOT, "over 200 client ticks with %d veterans in view: %d chevron draws, %.1f us each of the chevron's own (want 25 or less), %.3f ms of chevron drawing a tick (want 0.75 or less); apart from that, %.1f us each (%.3f ms a tick) drawing the troop's last layer in getBuffer; %d fps at the end", crowd.size() + 3, draws, us, perTick, flushUs, flush / 1e6 / 200, c.getCurrentFps()));
                        clearBefore = RankSync.clearSends; cleared = false;
                        server(c, b -> { b.clearProgression(); cleared = true; });
                        wait = 0; cs = 13;
                    }
                }
                case 13 -> { wait++; // p2-rank-clear: a round boundary (Battle.clearProgression) empties this client's store; the troops draw at 1/1/1 again
                    if (cleared && wait == 20) {
                        var x = e(c, crowd.get(0)); var l = geo(x);
                        boolean ok = RankSync.RANKS.isEmpty() && RankSync.clearSends > clearBefore && RankSync.client(x) == null && l != null && l.look().armourTier() == 1 && l.look().level() == 1 && "sword_t1".equals(l.swords()) && l.helms().equals(helmsAt(1));
                        report("p2-rank-clear", ok, "after Battle.clearProgression the server sent " + (RankSync.clearSends - clearBefore) + " CLEAR packet(s); this client's store holds " + RankSync.RANKS.size() + " rank(s) (want 0) and a level-3 Squire now draws at " + (l == null ? "-" : l.look().level() + "/" + l.look().weaponTier() + "/" + l.look().armourTier()) + " (want 1/1/1), GeckoLib " + geoText(l) + " (want helm_squire, sword_t1)");
                        server(c, b -> { // p2-rank-clear-once: with nothing sent since that clear a second one sends nothing; after a rank change the next one does
                            int base = RankSync.clearSends; b.clearProgression(); onceQuiet = RankSync.clearSends - base;
                            stage(crowd.get(0), 2); b.clearProgression(); onceLoud = RankSync.clearSends - base - onceQuiet;
                            onceWant = 0; for (var pl : b.server.getPlayerManager().getPlayerList()) if (ServerPlayNetworking.canSend(pl, RankSync.CHANNEL)) onceWant++;
                            onceDone = true; });
                        wait = 0; cs = 14;
                    }
                }
                case 14 -> { wait++;
                    if (onceDone && wait == 20) {
                        boolean ok = onceQuiet == 0 && onceWant > 0 && onceLoud == onceWant && RankSync.RANKS.isEmpty();
                        report("p2-rank-clear-once", ok, "a second Battle.clearProgression with no rank sent since the last one sent " + onceQuiet + " CLEAR packet(s) (want 0); after a rank change the next one sent " + onceLoud + " to the " + onceWant + " client(s) that take rank_v1 (want 1 each) and this client's store holds " + RankSync.RANKS.size() + " rank(s) (want 0)");
                        server(c, b -> serverDone = true); end(c);
                    }
                }
                default -> {}
            }
        }

        /**
         * p2-rank-local-identity (Task 9 review I1): a client-only mob (a portrait) that shares its entity id with a world troop
         * never lends that troop its local rank. Entity.equals and hashCode compare by id, so a map keyed by equality mixes the two up;
         * on a multiplayer guest a portrait's client-made id can equal a world troop's network id. Built here by giving a fresh
         * client mob the level-2 Squire's id: two distinct Entity objects, equal by equals.
         */
        void localIdentity(MinecraftClient c) {
            var x = e(c, gk2); if (x == null) { report("p2-rank-local-identity", false, "the level-2 Squire is not in this client's world"); return; }
            var twin = new net.minecraft.entity.mob.ZombieEntity(net.minecraft.entity.EntityType.ZOMBIE, c.world); twin.setId(x.getId());
            RankSync.Rank before = RankSync.client(x), lent = new RankSync.Rank(1, 3, 1);
            RankSync.setLocal(twin, lent);
            RankSync.Rank world = RankSync.client(x), own = RankSync.client(twin);
            RankSync.setLocal(twin, null);
            boolean ok = twin.equals(x) && twin != x && Objects.equals(world, before) && new RankSync.Rank(2, 1, 2).equals(before) && lent.equals(own);
            report("p2-rank-local-identity", ok, "a client-only mob with the level-2 Squire's id " + x.getId() + " (equals the Squire=" + twin.equals(x) + ", same object=" + (twin == x) + ") set to local rank " + rank(lent)
                + ": the Squire reads " + rank(world) + " (want its own " + rank(before) + ", 2/1/2), the mob reads " + rank(own) + " (want " + rank(lent) + ")");
        }

        /** The Squire's generated model with these weaponTiers on its bones (stages cleared on them), built as TroopModelLoader does. */
        TroopModel<MobEntity> partsFixture(MinecraftClient c, Map<String, int[]> arms) throws Exception {
            var res = c.getResourceManager().getResource(new Identifier(TroopModels.NAMESPACE, TroopModelLoader.FOLDER + "knight.json"));
            if (res.isEmpty()) throw new IllegalStateException("models/troop/knight.json is missing");
            JsonObject j; try (var in = new InputStreamReader(res.get().getInputStream(), StandardCharsets.UTF_8)) { j = JsonParser.parseReader(in).getAsJsonObject(); }
            for (var el : j.getAsJsonArray("bones")) { var b = el.getAsJsonObject(); int[] t = arms.get(b.get("name").getAsString()); if (t == null) continue;
                b.add("stages", new com.google.gson.JsonArray()); var a = new com.google.gson.JsonArray(); for (int k : t) a.add(k); b.add("weaponTiers", a); }
            var rig = TroopModels.rig("biped");
            return new TroopModel<>(j, TroopModelLoader.parse(j).createModel(), Map.of("right", rig == null ? "right_hand" : rig.handBone("right"), "left", rig == null ? "left_hand" : rig.handBone("left")), null, List.of());
        }

        /**
         * p2-weapon-parts-tier: weaponTiers part sets on the Squire's model (no weaponless troop has a model yet). A whole set
         * (head: helm_squire at tier I, helm_crown at III; the hand-authored knight's own bones, Task 8) switches with the weapon tier, tier II drawing I; a partial one (the same
         * plus belt at tier I on the body, which has no tier III part) draws tier I at III (Task 9 review M7).
         */
        void weaponParts(MinecraftClient c) {
            try {
                var row = ArmyCards.spec("knight"); StringBuilder why = new StringBuilder();
                var whole = partsFixture(c, Map.of("helm_squire", new int[] {1}, "helm_crown", new int[] {3}));
                boolean ok = whole.weaponPart("helm_crown") && whole.weaponPart("helm_squire") && !whole.weaponPart("belt");
                boolean[][] want = {{true, false}, {true, false}, {false, true}}; // cap, crest at weapon tiers 1, 2 (no tier II parts: draws I) and 3
                for (int t = 1; t <= 3; t++) {
                    whole.prepare(new TroopModels.Look(row, 1, t, 1, false, "gold"), false, false); var shown = whole.shownBones(whole.shownMask());
                    boolean cap = shown.contains("helm_squire"), crest = shown.contains("helm_crown"), skirt = shown.contains("belt");
                    boolean good = cap == want[t - 1][0] && crest == want[t - 1][1] && skirt && whole.weaponPartTier(t) == (t == 3 ? 3 : 1); ok &= good;
                    why.append("whole set, weapon tier ").append(t).append(" (parts draw as ").append(whole.weaponPartTier(t)).append("): cap ").append(cap ? "shown" : "hidden").append(", crest ").append(crest ? "shown" : "hidden").append(good ? "" : " (BAD)").append("; ");
                }
                var partial = partsFixture(c, Map.of("helm_squire", new int[] {1}, "helm_crown", new int[] {3}, "belt", new int[] {1}));
                partial.prepare(new TroopModels.Look(row, 1, 3, 1, false, "gold"), false, false); var shown = partial.shownBones(partial.shownMask());
                boolean good = partial.weaponPartTier(3) == 1 && shown.contains("helm_squire") && shown.contains("belt") && !shown.contains("helm_crown"); ok &= good;
                why.append("partial set (belt [1] on the body, no tier III body part), weapon tier 3: parts draw as ").append(partial.weaponPartTier(3)).append(", crest ").append(shown.contains("helm_crown") ? "shown" : "hidden").append(good ? "" : " (BAD)").append(";");
                report("p2-weapon-parts-tier", ok, "the Squire's model as a weaponTiers fixture (the Old Horn and the other weaponless troops have no model yet): " + why.toString().trim());
            } catch (Exception ex) { report("p2-weapon-parts-tier", false, ex.toString()); ex.printStackTrace(); }
        }

        @Override public void cleanup(MinecraftClient c) { TroopDepth.enabled = true; removeProbe(); if (tookT2) restoreT2(); super.cleanup(c); }
    }
}
