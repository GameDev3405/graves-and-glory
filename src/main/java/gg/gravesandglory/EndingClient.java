package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The client side of the match ending (spec 3, 6, 7): the step this player is on (the server's, from the camp snapshot's "ending", or
 * further on after a skip this client sent), the RESULT banner, the freeze (troops stop ticking and their animation clocks stand still),
 * and the PODIUM shot: a fixed camera at PodiumLayout.viewSpot with an 8 s push-in, letterboxed, a night sky, the top three commanders
 * drawn as client-side figures on the plinths with up to three of their troops, and name plates projected under each plinth.
 * STATS and REWARDS are EndingScreen (opened here at STATS, closed at DONE). The first-person hand is never drawn while the ending runs.
 */
public final class EndingClient {
    private EndingClient() {}

    /** The server's step for this player, null while no ending runs for him; its ticks left (may exceed total after a skip) and length. */
    static MatchEnding.Step serverStep;
    static int left, total;
    static String outcome = "", line = "";
    /** The step this client skipped to ahead of the server's answer (null for none): the step shown is the later of the two, as the server's playerStep. */
    static MatchEnding.Step skippedTo;
    /** The report id this ending belongs to ("" before one). */
    static String resultId = "";
    /** Client ticks while in a world; when the shown step began; the world time the animation clocks stand at while frozen (-1 none). */
    static long ticks, stepAt;
    static MatchEnding.Step shown;
    static long freezeTime = -1;
    /** This client's ending finished while the match still runs (others still in their sequence): the camp treats him as waiting. */
    static boolean doneEarly;

    /** The podium camera: non-null from PODIUM until STATS (the same approach as CampaignClient's entrance camera). */
    static net.minecraft.entity.decoration.ArmorStandEntity camera;
    static boolean previousHud;
    public static double cameraX, cameraY, cameraZ;
    public static float cameraYaw, cameraPitch;
    /** The podium shot's vertical field of view, its dolly toward the stage (blocks) and its yaw drift around the stage (degrees, centred on the view spot's yaw). */
    public static final double FOV = 50, PUSH = 2, DRIFT = 6;
    /** The podium reveal (ending polish pass 2), seconds into this client's PODIUM: 3rd place, 2nd, then the MVP; the drum starts at DRUM_AT so its heavy
     *  stroke (about 3.7 s into the file) lands on the MVP. */
    static final double[] REVEAL_AT = {4.5, 3.0, 1.5};
    static final double DRUM_AT = 0.8;
    /** Every plate and figure shown at once (this client skipped into PODIUM); the slots already revealed (bit per slot) and whether the drum started. */
    static boolean revealAll; static int revealed; static boolean drummed;

    /** One podium figure: the commander (an OtherClientPlayerEntity) or one of his troops, with the podium row it belongs to. */
    record Figure(LivingEntity entity, int slot, boolean commander) {}
    static final List<Figure> figures = new ArrayList<>();
    /** The podium rows the figures were built from: rank, name, score, headline, house, team. */
    record Plate(int slot, String name, String stats, String origin, boolean mvp, String award, boolean left) {
        Plate(int slot, String name, String stats, String origin, boolean mvp) { this(slot, name, stats, origin, mvp, "", false); }
        /** The plate's lines under the name: the stats, the award (if any), then where he is from or that he left the battle. */
        List<String> lines() { List<String> l = new ArrayList<>(); l.add(stats); if (!award.isEmpty()) l.add(award); l.add(left ? LEFT : origin); return l; }
    }
    static final String LEFT = "Left the battle";
    static final List<Plate> plates = new ArrayList<>();
    /** Harness only: the plate rectangles last drawn (x0, y0, x1, y1), the banner word last drawn and the frames that drew it. */
    static final List<int[]> plateRects = new ArrayList<>();
    static volatile String bannerDrawn = "";
    static volatile long bannerFrames;
    /** Harness only: the frames the first-person hand was not drawn because the ending runs (EndingViewMixin). */
    public static volatile long handsHidden;
    /** Harness only: how this client's ending began ("packet" for ending_begin, "snapshot" for the camp snapshot) and the client tick it began on. */
    static volatile String beganBy = ""; static volatile long beganAt = -1;
    /** Harness only: System.nanoTime when this ending began here, and when its banner was first drawn (-1 before). */
    static volatile long beganNanos = -1, bannerNanos = -1;
    /** Each podium figure's pose this frame by entity id (posePlayer): the rank's pose name, for the checks. */
    static final java.util.Map<Integer, String> poses = new java.util.HashMap<>();

    // ---- state -------------------------------------------------------------------------------------------------------------------

    /** The step this player sees: the later of the server's and his own skip; null with no ending. */
    public static MatchEnding.Step step() {
        if (serverStep == null) return null;
        return skippedTo == null ? serverStep : MatchEnding.playerStep(serverStep, skippedTo);
    }
    /** Whether the battlefield is frozen for this client: from RESULT until the ending ends. */
    public static boolean frozen() { MatchEnding.Step s = step(); return s != null && s != MatchEnding.Step.DONE; }
    /** Whether the podium camera holds the view. */
    public static boolean cinematic() { return camera != null; }
    /** Whether the ending draws the HUD (InGameHud is replaced by hud()). */
    public static boolean hudActive() { return frozen(); }

    /** What a camp snapshot means for the ending: RUNNING while it runs for this player, DONE once when it ends for him, NONE otherwise. */
    enum Change { NONE, RUNNING, DONE }

    /** Reads the snapshot's "ending" (null when absent) with the report id of the snapshot's result. */
    static Change accept(MinecraftClient c, JsonObject ending, String reportId, boolean running) {
        if (!running) doneEarly = false;
        if (ending != null && ending.has("step")) {
            MatchEnding.Step next;
            try { next = MatchEnding.Step.valueOf(ending.get("step").getAsString()); } catch (RuntimeException e) { return Change.NONE; }
            if (serverStep == null) { skippedTo = null; freezeTime = c.world == null ? -1 : c.world.getTime(); beganBy = "snapshot"; beganAt = ticks; beganNanos = System.nanoTime(); bannerNanos = -1; }
            serverStep = next; resultId = reportId; doneEarly = false;
            left = ending.has("left") ? ending.get("left").getAsInt() : 0; total = ending.has("total") ? ending.get("total").getAsInt() : 0;
            outcome = ending.has("outcome") ? ending.get("outcome").getAsString() : "";
            line = ending.has("line") ? ending.get("line").getAsString() : "";
            sync(c);
            return Change.RUNNING;
        }
        if (serverStep == null) return Change.NONE;
        boolean same = resultId.equals(reportId);
        stop(c);
        if (!same) return Change.NONE;
        doneEarly = running;
        return Change.DONE;
    }

    /** ending_begin (the server's word on the tick the match ends): the freeze and the RESULT banner start now; the next snapshot only confirms them. */
    static void beginEnding(MinecraftClient c, String outcomeWord, String scoreLine) {
        if (serverStep != null || c.player == null || c.world == null) return;
        skippedTo = null; freezeTime = c.world.getTime(); beganBy = "packet"; beganAt = ticks; beganNanos = System.nanoTime(); bannerNanos = -1;
        serverStep = MatchEnding.Step.RESULT; doneEarly = false; resultId = "";
        total = left = Spec.ENDING[0].resultSeconds() * 20;
        outcome = outcomeWord; line = scoreLine;
        sync(c);
    }

    /** Sends one skip: this player alone moves to the next step at once (the server's answer follows within half a second). */
    static void skip(MinecraftClient c) {
        MatchEnding.Step s = step();
        if (s == null || s == MatchEnding.Step.DONE) return;
        GravesAndGloryClient.send("ending:skip");
        skippedTo = MatchEnding.next(s);
        sync(c);
    }

    /** Ending polish pass 3: Play again is offered on REWARDS to a member of a room (the snapshot's social block names one). */
    static boolean playAgainShown() { return step() == MatchEnding.Step.REWARDS && WarTableLobby.inRoom(); }
    /** Play again (the REWARDS button or its key): this player is done at once and the server marks him Ready in his room (ending:again); the room's
     *  all-ready countdown does the rest. Nothing outside a room. */
    static void playAgain(MinecraftClient c) {
        if (!playAgainShown()) return;
        GravesAndGloryClient.send("ending:again");
        UiSounds.toggled(true);
        skippedTo = MatchEnding.Step.DONE; playAgainSent++;
        sync(c);
    }
    /** Harness only: how many Play again presses were sent. */
    static volatile int playAgainSent;

    /** Puts the client in the shown step: the podium camera and figures on entering PODIUM, the camera off from STATS on. */
    static void sync(MinecraftClient c) {
        MatchEnding.Step s = step();
        if (s != shown) {
            shown = s; stepAt = ticks;
            if (s == MatchEnding.Step.RESULT) EndingSounds.banner(outcome.isEmpty() ? "DRAW" : outcome);
            if (s == MatchEnding.Step.PODIUM) { buildFigures(c); begin(c); revealed = 0; drummed = false; revealAll = skippedTo == MatchEnding.Step.PODIUM; reveal(); }
            else if (s == MatchEnding.Step.STATS || s == MatchEnding.Step.REWARDS) { endCamera(c); if (figures.isEmpty()) buildFigures(c); openStats(c); }
            else if (s == MatchEnding.Step.DONE) leave(c);
        }
    }

    /** This player is done ahead of the server's word (a skip out of REWARDS): the camera, figures and screen go at once, while serverStep
     *  stays until the snapshot without "ending" arrives, so accept still reports DONE and the room or the War Table opens. */
    static void leave(MinecraftClient c) { endCamera(c); clearFigures(); closeScreen(c); bannerDrawn = ""; }
    static void closeScreen(MinecraftClient c) { if (c.currentScreen instanceof EndingScreen) c.setScreen(null); }

    /** Escape (EndingKeyMixin): the same skip as the menu key. */
    public static void escape(MinecraftClient c) { skip(c); }

    /** STATS and REWARDS: the podium camera off, the ending's screen over the dimmed stage (it follows the step to REWARDS itself). */
    static void openStats(MinecraftClient c) {
        endCamera(c);
        if (!(c.currentScreen instanceof EndingScreen)) c.setScreen(new EndingScreen());
    }

    static void tick(MinecraftClient c) {
        ticks++;
        if (serverStep == null) return;
        if (c.player == null || c.world == null) { stop(c); return; }
        sync(c);
        if (camera != null) c.getToastManager().clear();
        for (var f : figures) f.entity().age++; // the figures are never in the world: their own clock runs so troops play their idle and stance clips
        if (step() == MatchEnding.Step.PODIUM) reveal();
        MatchEnding.Step s = step();
        if ((s == MatchEnding.Step.STATS || s == MatchEnding.Step.REWARDS) && c.currentScreen == null) openStats(c); // a screen something else closed comes back
    }

    /** Ends everything this client shows for the ending (DONE, a disconnect, or the ending leaving the snapshot). */
    static void stop(MinecraftClient c) {
        endCamera(c); clearFigures(); closeScreen(c);
        serverStep = null; skippedTo = null; shown = null; freezeTime = -1; bannerDrawn = "";
    }
    static void forget(MinecraftClient c) { stop(c); resultId = ""; doneEarly = false; }

    // ---- the freeze ----------------------------------------------------------------------------------------------------------------

    /** The world time troop animation reads: the moment the freeze began while frozen, else the world's own. */
    public static long worldTime(World w) { return frozen() && freezeTime >= 0 ? freezeTime : w.getTime(); }
    /** Whether the client world must not tick this entity: a troop or a projectile while frozen. The figures are never in the world. */
    public static boolean holds(Entity e) {
        if (!frozen()) return false;
        return e instanceof ProjectileEntity || e instanceof MobEntity m && TroopModels.troopOf(m) != null;
    }
    /** GeckoLib's tick for a frozen troop: constant (GeoModel adds the frame's partial tick to it). */
    static double geoTick(Entity e) { return still(e) ? e.age + 1 - MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true) : e.age; }
    /** A podium figure (ids -7100 down to -7111, never in the world). */
    public static boolean figure(Entity e) { int id = e.getId(); return id <= -7100 && id > -7112; }
    /** Whether this entity's animation stands still: frozen, and not a podium figure (the figures play their clips through the ending). */
    public static boolean still(Entity e) { return frozen() && !figure(e); }
    /** Harness only: the clock a troop's clips read now (seconds), as TroopRenderer would draw it this frame. */
    static double clock(Entity e) { return TroopMotion.time(e, e.age + (still(e) ? 1f : MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true))); }

    // ---- the podium camera ---------------------------------------------------------------------------------------------------------

    static void begin(MinecraftClient c) {
        if (c.player == null || c.world == null || camera != null) return;
        double[] v = PodiumLayout.viewSpot();
        previousHud = c.options.hudHidden; c.options.hudHidden = false;
        camera = new net.minecraft.entity.decoration.ArmorStandEntity(c.world, v[0], v[1], v[2]); camera.setInvisible(true);
        if (c.currentScreen != null) c.setScreen(null);
    }
    static void endCamera(MinecraftClient c) {
        if (camera == null) return;
        camera = null; c.options.hudHidden = previousHud;
    }
    /** The push-in: 0 to 1 over the podium step (local clock from when this client entered it), smoothstepped. */
    static double push(float partial) {
        int len = Math.max(1, Spec.ENDING[0].podiumSeconds() * 20);
        double t = Math.max(0, Math.min(1, (ticks - stepAt + partial) / len));
        return t * t * (3 - 2 * t);
    }
    /** Render-frame sampling: the view spot, dollied PUSH blocks toward the stage along its look, then swung around the stage's centre (the plinths' middle)
     *  from -DRIFT/2 to +DRIFT/2 degrees of yaw, turning with it so the stage stays framed. */
    public static void sampleCamera(float partial) {
        double[] v = PodiumLayout.viewSpot();
        double e = push(partial), yaw = Math.toRadians(v[3]);
        double x = v[0] - Math.sin(yaw) * PUSH * e, z = v[2] + Math.cos(yaw) * PUSH * e;
        double[][] pl = PodiumLayout.plinths(); double sx = 0, sz = 0; for (double[] p : pl) { sx += p[0]; sz += p[2]; }
        double cx = sx / pl.length, cz = sz / pl.length, a = Math.toRadians(DRIFT * (e - .5)), ox = x - cx, oz = z - cz;
        // the same turn that takes the look at yaw to the look at yaw + a (look = (-sin, cos)), applied to the offset from the stage
        cameraX = cx + ox * Math.cos(a) - oz * Math.sin(a); cameraY = v[1] + .35; cameraZ = cz + ox * Math.sin(a) + oz * Math.cos(a);
        cameraYaw = (float) (v[3] + Math.toDegrees(a)); cameraPitch = (float) v[4] + 4;
    }

    // ---- the podium reveal (ending polish pass 2) ------------------------------------------------------------------------------

    /** Seconds this client has been in PODIUM. */
    static double podiumSeconds(float partial) { return (ticks - stepAt + partial) / 20.0; }
    /** Whether the podium slot (0 the MVP's centre step, 1 second, 2 third) is shown now: from its REVEAL_AT, or at once after a skip into PODIUM. */
    static boolean shownSlot(int slot) {
        if (step() != MatchEnding.Step.PODIUM || revealAll) return true;
        return podiumSeconds(0) >= REVEAL_AT[Math.max(0, Math.min(2, slot))];
    }
    /** Each tick in PODIUM: the drum at DRUM_AT, then each slot's PODIUM_REVEAL as it appears (the MVP's with AWARD). A slot nobody stands on plays nothing,
     *  so with fewer than three commanders the reveal still ends on the MVP at 4.5 s. After a skip into PODIUM everything shows at once with one reveal. */
    static void reveal() {
        if (!drummed && !revealAll && podiumSeconds(0) >= DRUM_AT) { drummed = true; EndingSounds.play("PODIUM_DRUM"); }
        boolean any = false, mvp = false;
        for (int slot = 2; slot >= 0; slot--) {
            if ((revealed & 1 << slot) != 0 || !shownSlot(slot)) continue;
            revealed |= 1 << slot;
            for (var p : plates) if (p.slot() == slot) { any = true; mvp |= slot == 0 || p.mvp(); if (!revealAll) { EndingSounds.play("PODIUM_REVEAL"); if (slot == 0) EndingSounds.award(); } }
        }
        if (revealAll && any) { EndingSounds.play("PODIUM_REVEAL"); if (mvp) EndingSounds.award(); }
    }
    /** The night the stage is shown at (client only: ClientWorld's time of day while the podium is up). */
    public static boolean night() { MatchEnding.Step s = step(); return s == MatchEnding.Step.PODIUM || s == MatchEnding.Step.STATS || s == MatchEnding.Step.REWARDS; }
    public static final long NIGHT = 18000;

    // ---- the figures ---------------------------------------------------------------------------------------------------------------

    /** A commander figure: his profile (so his skin), every skin layer shown, and never drawn as a spectator (the local player is one at the stage,
     *  and his own figure shares his player-list entry, which would draw the head alone). */
    static final class Statue extends OtherClientPlayerEntity {
        Statue(net.minecraft.client.world.ClientWorld w, GameProfile p) { super(w, p); dataTracker.set(PLAYER_MODEL_PARTS, (byte) 0x7f); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }

    /** A bot's figure: one fixed profile, so it wears the default skin. */
    static final UUID BOT = new UUID(0x67726176657367L, 0x626f74L);

    static void buildFigures(MinecraftClient c) {
        clearFigures();
        if (c.world == null) return;
        JsonArray rows = CampaignClient.result.has("podium") ? CampaignClient.result.getAsJsonArray("podium") : new JsonArray();
        double[][] pl = PodiumLayout.plinths();
        double[] view = PodiumLayout.viewSpot();
        for (int i = 0; i < rows.size() && i < 3; i++) {
            JsonObject r;
            try { r = rows.get(i).getAsJsonObject(); } catch (RuntimeException e) { continue; }
            int rank = r.has("rank") ? r.get("rank").getAsInt() : i + 1, slot = Math.max(0, Math.min(2, rank - 1));
            int team = r.has("team") ? r.get("team").getAsInt() : 0;
            boolean bot = r.has("bot") && r.get("bot").getAsBoolean();
            String name = r.has("name") ? r.get("name").getAsString() : "Commander";
            String side = team >= 0 && team < CampaignClient.sides.length ? CampaignClient.sides[team] : "linen";
            var faction = faction(side);
            double[] at = pl[slot];
            // the commander, a little forward on his plinth so his troops stand behind his shoulders
            GameProfile profile = bot ? new GameProfile(BOT, display(name)) : profile(c, r, name);
            var cmd = new Statue(c.world, profile);
            cmd.setId(-7100 - slot * 4);
            JsonArray gear = r.has("gear") ? r.getAsJsonArray("gear") : new JsonArray();
            boolean any = false;
            for (int k = 0; k < gear.size() && k < CommanderGearFeature.SLOTS.length; k++) {
                String id = gear.get(k).getAsString(); if (id.isEmpty()) continue;
                var item = Registries.ITEM.get(Identifier.of(id)); if (item == Items.AIR) continue;
                cmd.equipStack(CommanderGearFeature.SLOTS[k], new ItemStack(item)); any = true;
            }
            if (!any) { // a bot (or a commander with nothing worn): leather in the side's colour and a sword
                int colour = faction == null ? 0xb0a080 : faction.color();
                cmd.equipStack(EquipmentSlot.CHEST, dyed(Items.LEATHER_CHESTPLATE, colour)); cmd.equipStack(EquipmentSlot.LEGS, dyed(Items.LEATHER_LEGGINGS, colour));
                cmd.equipStack(EquipmentSlot.FEET, dyed(Items.LEATHER_BOOTS, colour)); cmd.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            }
            place(cmd, at[0], at[1], at[2] + .3, view);
            figures.add(new Figure(cmd, slot, true));
            // up to three of his troops in an arc behind him
            JsonArray troops = r.has("troops") ? r.getAsJsonArray("troops") : new JsonArray();
            int n = Math.min(3, troops.size());
            double[][] arcs = {{}, {-62}, {-62, 62}, {-70, 70, 22}};
            for (int j = 0; j < n; j++) {
                var spec = troop(troops.get(j).getAsString()); if (spec == null) continue;
                MobEntity m = ArmyMobs.portrait(spec.entity(), c.world);
                m.setCustomName(Text.literal(ArmyMobs.levelName(spec, 1))); m.setCustomNameVisible(false); m.setSilent(true);
                RankSync.setLocal(m, new RankSync.Rank(1, 1, 1));
                m.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Registries.ITEM.get(Identifier.of(spec.weapon()))));
                m.equipStack(EquipmentSlot.HEAD, new ItemStack(Registries.ITEM.get(Identifier.of(spec.helmet()))));
                var chest = new ItemStack(Registries.ITEM.get(Identifier.of(spec.chest())));
                if (faction != null) GravesAndGlory.Battle.dye(chest, faction.color());
                m.equipStack(EquipmentSlot.CHEST, chest);
                if (ArmyMobs.tags(spec).contains("shield")) m.equipStack(EquipmentSlot.OFFHAND, GravesAndGlory.Battle.shield(spec, faction == null ? GravesAndGlory.Battle.PORTRAIT_FIELD : faction.dye()));
                TroopModels.dress(m, side);
                m.setId(-7100 - slot * 4 - 1 - j);
                double a = Math.toRadians(arcs[n][j]), radius = 1.2;
                // the arc opens away from the camera (north): angle 0 is straight behind, positive to the viewer's right (east)
                place(m, at[0] + Math.sin(a) * radius, at[1], at[2] + .3 - Math.cos(a) * radius, view);
                figures.add(new Figure(m, slot, false));
            }
            String house = houseName(r.has("house") ? r.get("house").getAsString() : "");
            String headline = r.has("headline") ? r.get("headline").getAsString().toUpperCase(Locale.ROOT) : "";
            int score = r.has("score") ? r.get("score").getAsInt() : 0;
            String sideName = faction == null ? "" : faction.name();
            boolean mvp = r.has("mvp") ? r.get("mvp").getAsBoolean() : rank == 1, gone = r.has("left") && r.get("left").getAsBoolean();
            String award = r.has("award") ? r.get("award").getAsString().toUpperCase(Locale.ROOT) : "";
            plates.add(new Plate(slot, display(name), score + (headline.isEmpty() ? "" : " · " + headline),
                house.isEmpty() ? sideName : sideName.isEmpty() ? house : house + " · " + sideName, mvp, award, gone));
        }
    }
    static void clearFigures() {
        for (var f : figures) { TroopMotion.forget(f.entity().getId()); MotionEvents.Client.forget(f.entity().getId()); }
        figures.clear(); plates.clear(); plateRects.clear(); poses.clear();
    }
    /** Stands e at x,y,z facing the view spot, with every previous-frame value the same (a statue under any partial tick). */
    static void place(LivingEntity e, double x, double y, double z, double[] view) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(view[0] - x), view[2] - z));
        e.refreshPositionAndAngles(x, y, z, yaw, 0);
        e.prevX = e.lastRenderX = x; e.prevY = e.lastRenderY = y; e.prevZ = e.lastRenderZ = z;
        e.bodyYaw = e.prevBodyYaw = e.headYaw = e.prevHeadYaw = yaw; e.prevYaw = yaw; e.prevPitch = 0;
    }
    static GameProfile profile(MinecraftClient c, JsonObject r, String name) {
        try {
            UUID id = UUID.fromString(r.get("uuid").getAsString());
            var entry = c.getNetworkHandler() == null ? null : c.getNetworkHandler().getPlayerListEntry(id);
            return entry != null ? entry.getProfile() : new GameProfile(id, name);
        } catch (RuntimeException e) { return new GameProfile(BOT, name); }
    }
    static ItemStack dyed(net.minecraft.item.Item item, int colour) {
        var s = new ItemStack(item); GravesAndGlory.Battle.dye(s, colour); return s;
    }
    static Spec.TroopsRow troop(String id) { for (var t : Spec.TROOPS) if (t.id().equals(id)) return t; return null; }
    static Spec.FactionsRow faction(String id) { for (var f : Spec.FACTIONS) if (f.id().equals(id)) return f; return null; }
    static String houseName(String id) { for (var h : Spec.HOUSES) if (h.id().equals(id)) return h.name(); return ""; }
    /** "normal bot" reads "Normal bot". */
    static String display(String name) { return name.isEmpty() ? name : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1); }

    /** Draws the figures after the world's entities (WorldRenderEvents.AFTER_ENTITIES): never in the world, so nothing ticks or collides. */
    static void renderFigures(net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext ctx) {
        if (figures.isEmpty()) return;
        var c = MinecraftClient.getInstance(); if (c.world == null) return;
        EntityRenderDispatcher d = c.getEntityRenderDispatcher();
        var cam = ctx.camera().getPos(); MatrixStack m = ctx.matrixStack();
        var consumers = c.getBufferBuilders().getEntityVertexConsumers();
        boolean hud = c.options.hudHidden; c.options.hudHidden = true; // no vanilla name tags over the figures: the plates name them
        try {
            for (var f : figures) {
                if (!shownSlot(f.slot())) continue; // not revealed yet (ending polish pass 2)
                var e = f.entity();
                int light = WorldRenderer.getLightmapCoordinates(c.world, BlockPos.ofFloored(e.getX(), e.getY() + 1, e.getZ()));
                light = LightmapTextureManager.pack(Math.max(13, LightmapTextureManager.getBlockLightCoordinates(light)), LightmapTextureManager.getSkyLightCoordinates(light));
                d.render(e, e.getX() - cam.x, e.getY() - cam.y, e.getZ() - cam.z, e.getYaw(), 1f, m, consumers, light);
            }
        } catch (RuntimeException ex) {
            System.err.println("[Graves and Glory] Podium figure could not be drawn: " + ex);
        } finally { c.options.hudHidden = hud; }
        consumers.draw();
    }

    // ---- the HUD -------------------------------------------------------------------------------------------------------------------

    static final int DRAW_INK = 0xffc9c3b4, VICTORY_INK = 0xffe8c26a, SHADOW = 0xc0000000, PLATE_FILL = 0xdc14100c, LINE_INK = 0xfff3ead6;

    /** The ending's HUD, in place of InGameHud while frozen: the banner during RESULT, the letterbox and plates during PODIUM, the hint always. */
    public static void hud(DrawContext g, int w, int h) {
        var c = MinecraftClient.getInstance(); var font = c.textRenderer;
        MatchEnding.Step s = step(); if (s == null || c.currentScreen instanceof EndingScreen) return; // the screen draws its own hint
        float partial = c.getRenderTickCounter().getTickDelta(false); // HUD time (D11)
        String hint = HintText.expand("{key:menu} or Escape to skip", GravesAndGloryClient::hint);
        if (s == MatchEnding.Step.PODIUM) {
            int bar = Math.max(18, h / 10);
            g.fill(0, 0, w, bar, DarkUi.LETTERBOX); g.fill(0, h - bar, w, h, DarkUi.LETTERBOX);
            plates(g, w, h, bar, partial);
            g.drawCenteredTextWithShadow(font, hint, w / 2, h - bar + (bar - 8) / 2, DarkUi.MUTED);
            return;
        }
        if (s == MatchEnding.Step.RESULT) banner(g, w, h, partial);
        g.drawCenteredTextWithShadow(font, hint, w / 2, h - 18, 0xffcfc3a8);
    }

    static int ink(String word) { return switch (word) { case "VICTORY" -> VICTORY_INK; case "DEFEAT" -> DarkUi.ALARM_TEXT; default -> DRAW_INK; }; }

    /** The word (title font, about 11% of the screen's width tall) landing with a 6-tick scale-in, a brass rule under it, then the score line. */
    static void banner(DrawContext g, int w, int h, float partial) {
        var font = MinecraftClient.getInstance().textRenderer;
        String word = outcome.isEmpty() ? "DRAW" : outcome;
        Text t = Text.literal(word).setStyle(Style.EMPTY.withFont(DarkUi.TITLE));
        int tw = Math.max(1, font.getWidth(t));
        float scale = Math.min(w * .11f / 10f, w * .86f / tw);
        double in = Math.max(0, Math.min(1, (ticks - stepAt + partial) / 6.0)), ease = 1 - Math.pow(1 - in, 3);
        float land = (float) (scale * (1.7 - .7 * ease));
        int alpha = (int) (255 * Math.min(1, in * 1.6));
        int top = (int) (h * .24f);
        int ink = (alpha << 24) | (ink(word) & 0xffffff), shade = ((alpha * 3 / 4) << 24);
        var mx = g.getMatrices();
        mx.push(); mx.translate(w / 2f, top + 5 * scale, 0); mx.scale(land, land, 1);
        g.drawText(font, t, -tw / 2, -5 + 1, shade, false);
        g.drawText(font, t, -tw / 2, -5, ink, false);
        mx.pop();
        int y = (int) (top + 5 * scale + 6 * scale);
        if (bannerNanos < 0) bannerNanos = System.nanoTime();
        if (in < 1) { bannerFrames++; bannerDrawn = word; return; }
        // the brass rule: fading out at both ends
        int rw = (int) (w * .40f), x0 = w / 2 - rw / 2;
        for (int i = 0; i < rw; i++) {
            double f = 1 - Math.abs((i - rw / 2.0) / (rw / 2.0)); int a = (int) (255 * Math.min(1, f * 1.8));
            g.fill(x0 + i, y, x0 + i + 1, y + 2, (a << 24) | (DarkUi.BRASS & 0xffffff));
        }
        Text sub = Text.literal(line).setStyle(Style.EMPTY.withFont(DarkUi.TITLE));
        int sw = font.getWidth(sub); float ss = Math.min(1.35f, (w - 24f) / Math.max(1, sw));
        mx.push(); mx.translate(w / 2f, y + 8, 0); mx.scale(ss, ss, 1);
        g.drawText(font, sub, -sw / 2, 1, SHADOW, false); g.drawText(font, sub, -sw / 2, 0, LINE_INK, false);
        mx.pop();
        bannerFrames++; bannerDrawn = word;
    }

    /** Projects a world point to scaled-screen x,y through the podium camera; null behind it. */
    static double[] project(double x, double y, double z, int w, int h, float partial) {
        sampleCamera(partial);
        double yaw = Math.toRadians(cameraYaw), pitch = Math.toRadians(cameraPitch);
        double rx = x - cameraX, ry = y - cameraY, rz = z - cameraZ;
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double sx = -Math.cos(yaw), sz = -Math.sin(yaw); // screen right
        double ux = -Math.sin(yaw) * Math.sin(pitch), uy = Math.cos(pitch), uz = Math.cos(yaw) * Math.sin(pitch);
        double depth = rx * fx + ry * fy + rz * fz; if (depth <= .1) return null;
        double tan = Math.tan(Math.toRadians(FOV) / 2), aspect = (double) w / h;
        double nx = (rx * sx + rz * sz) / depth / (tan * aspect), ny = (rx * ux + ry * uy + rz * uz) / depth / tan;
        return new double[]{w / 2.0 * (1 + nx), h / 2.0 * (1 - ny)};
    }

    /** The name plates: one per podium row under its plinth, on one line above the bottom bar; narrowed (text scaled down) so neighbours never overlap. */
    static void plates(DrawContext g, int w, int h, int bar, float partial) {
        var font = MinecraftClient.getInstance().textRenderer;
        plateRects.clear();
        double[][] pl = PodiumLayout.plinths();
        int n = plates.size(); if (n == 0) return;
        double[] cx = new double[n];
        for (int i = 0; i < n; i++) { var p = plates.get(i); double[] at = pl[p.slot()]; double[] s = project(at[0], at[1], at[2] + 1.5, w, h, partial); cx[i] = s == null ? w / 2.0 : s[0]; }
        int rows = 1; for (var p : plates) rows = Math.max(rows, 1 + p.lines().size());
        int lineH = 11, pad = 5, ph = pad * 2 + lineH * rows - 2, bottom = h - bar - 6, top = bottom - ph;
        for (int i = 0; i < n; i++) {
            var p = plates.get(i);
            if (!shownSlot(p.slot())) continue; // not revealed yet (ending polish pass 2)
            Text name = Text.literal(p.name()).setStyle(Style.EMPTY.withFont(DarkUi.TITLE));
            int chip = p.mvp() ? font.getWidth("MVP") + 6 : 0;
            int want = font.getWidth(name) + chip; for (String l : p.lines()) want = Math.max(want, font.getWidth(l)); want += pad * 2 + 4;
            // the room to the nearest neighbour's centre, split evenly, less a gap
            double room = w - 8;
            for (int j = 0; j < n; j++) if (j != i) room = Math.min(room, Math.abs(cx[j] - cx[i]) - 6);
            room = Math.min(room, 2 * Math.min(cx[i], w - cx[i]) - 4);
            int pw = (int) Math.max(40, Math.min(want, room));
            int x0 = (int) Math.round(cx[i] - pw / 2.0), x1 = x0 + pw;
            g.fill(x0, top, x1, bottom, PLATE_FILL); g.drawBorder(x0, top, pw, ph, DarkUi.BRASS);
            int inner = pw - pad * 2;
            int y = top + pad + (rows - 1 - p.lines().size()) * lineH / 2; // a plate with fewer lines (no award) has its text centred, not a blank line at the bottom
            // line 1: MVP chip and name
            int nw = font.getWidth(name) + chip; float s1 = Math.min(1f, inner / (float) Math.max(1, nw));
            var mx = g.getMatrices();
            mx.push(); mx.translate((float) cx[i] - nw * s1 / 2f, y, 0); mx.scale(s1, s1, 1);
            if (p.mvp()) { g.fill(0, -1, chip - 2, 9, VICTORY_INK); g.drawText(font, "MVP", 2, 0, DarkUi.INK_TEXT, false); }
            g.drawText(font, name, chip, 0, DarkUi.PAPER, false);
            mx.pop(); y += lineH;
            fitted(g, p.stats(), cx[i], y, inner, VICTORY_INK); y += lineH;
            if (!p.award().isEmpty()) { fitted(g, p.award(), cx[i], y, inner, LINE_INK); y += lineH; }
            fitted(g, p.left() ? LEFT : p.origin(), cx[i], y, inner, p.left() ? DarkUi.ALARM_TEXT : 0xffcdbf9f);
            plateRects.add(new int[]{x0, top, x1, bottom});
        }
    }
    static void fitted(DrawContext g, String text, double cx, int y, int inner, int ink) {
        var font = MinecraftClient.getInstance().textRenderer;
        int tw = font.getWidth(text); float s = Math.min(1f, inner / (float) Math.max(1, tw));
        var mx = g.getMatrices(); mx.push(); mx.translate((float) cx - tw * s / 2f, y, 0); mx.scale(s, s, 1);
        g.drawText(font, text, 0, 0, ink, false); mx.pop();
    }

    // ---- the podium poses (ending polish spec 1) -------------------------------------------------------------------------------

    /** After vanilla and the commander clips pose a podium commander (PlayerModelMixin): rank 1 raises his weapon arm high and lifts his chin, ranks 2 and 3
     *  hold the weapon arm across the chest, any other stands as he is. Only arm and head angles on the existing model; the sleeves and hat follow. */
    public static void posePlayer(net.minecraft.client.render.entity.model.PlayerEntityModel<?> m, LivingEntity e, float age) {
        if (figures.isEmpty() || !figure(e) || !(e instanceof Statue)) return;
        int slot = -1; for (var f : figures) if (f.entity() == e) slot = f.slot();
        if (slot < 0) return;
        boolean right = e.getMainArm() == net.minecraft.util.Arm.RIGHT;
        var arm = right ? m.rightArm : m.leftArm; float side = right ? 1 : -1;
        float breathe = (float) Math.sin(age * 0.08) * 0.03f;
        if (slot == 0) {
            arm.pitch = (float) Math.toRadians(-168) + breathe; arm.yaw = 0; arm.roll = side * (float) Math.toRadians(-8);
            m.head.pitch = (float) Math.toRadians(-14); m.head.yaw = 0;
            poses.put(e.getId(), "raised");
        } else if (slot <= 2) {
            arm.pitch = (float) Math.toRadians(-58) + breathe; arm.yaw = side * (float) Math.toRadians(-38); arm.roll = side * (float) Math.toRadians(14);
            m.head.pitch = (float) Math.toRadians(4);
            poses.put(e.getId(), "chest");
        } else { poses.put(e.getId(), "stand"); return; }
        m.hat.copyTransform(m.head);
        m.rightSleeve.copyTransform(m.rightArm); m.leftSleeve.copyTransform(m.leftArm);
    }
}
