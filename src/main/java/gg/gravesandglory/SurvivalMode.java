package gg.gravesandglory;

import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Survival (Phase 4 spec 6, plan Task 6): the humans and their allied bots defend the shrine on team 0, one to four banners;
 * team 1 is the director, an army with no commander that buys nothing and lives outside Battle.squads, so Campaign's bot
 * buying and orders and the round's income, reinforcements and lineup never touch it. Its waves rise at the rise sites after a
 * soul-fire warning; the next wave rises when the current one's timer ends, cleared or not, and a cleared wave pays every
 * defending banner its gold. The defenders win when the last wave is cleared or its timer runs out with the shrine held; the
 * dead win once they hold the shrine lossSeconds with no defender on it. The night falls at setup and lifts at teardown.
 * Every number is a sheet row: modeRules survival, the waves and waveGroups of its waveSet, the modeSites survival rows of
 * the map (shrine, court, rise sites) and the botCommand tier's waveScale and waveTraitTier. A new map (the Map Forge
 * Hollowwood) needs only its own modeSites survival rows.
 */
public final class SurvivalMode extends BattleMode {
    /** The defenders' team; the director's is the other. */
    public static final int DEFENDERS = 0, DEAD = 1;
    /** The director army's owner id, the same every round. */
    public static final UUID DIRECTOR = UUID.nameUUIDFromBytes("gravesandglory:director".getBytes(StandardCharsets.UTF_8));
    /** The tag each risen troop carries, so recover can find one a crash left in a loaded chunk. */
    public static final String TAG = "gg-survival-dead";
    /** The world time of the night the round plays in. */
    public static final long NIGHT = 18000;

    static Spec.ModeRulesRow rules() {
        for (var r : Spec.MODERULES) if (r.id().equals(Modes.SURVIVAL)) return r;
        throw new IllegalStateException("no modeRules survival row");
    }
    static final Spec.ModeRulesRow R = rules();
    static final int WARNING_TICKS = R.riseWarning() * 20;

    /** The waves of the rule row's set, in order. */
    static List<Spec.WavesRow> waves() {
        var out = new ArrayList<Spec.WavesRow>();
        for (var w : Spec.WAVES) if (w.set().equals(R.waveSet())) out.add(w);
        out.sort((a, c) -> Integer.compare(a.wave(), c.wave()));
        return out;
    }
    static final List<Spec.WavesRow> WAVES = waves();
    /** One wave's groups, in sheet order. */
    static List<Spec.WaveGroupsRow> groups(Spec.WavesRow wave) {
        var out = new ArrayList<Spec.WaveGroupsRow>();
        for (var g : Spec.WAVEGROUPS) if (g.wave().equals(wave.id())) out.add(g);
        return out;
    }
    /** A modeSites survival row of a map by kind (the first; preflight requires one shrine and one court). */
    static Spec.ModeSitesRow site(int map, String kind) {
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SURVIVAL) && s.map() == map && s.kind().equals(kind)) return s;
        throw new IllegalStateException("no modeSites survival " + kind + " row on map " + map);
    }
    /** A rise site of a map by its key, the end of its id (survival_hollowwood_rise_chapel is chapel). */
    static Spec.ModeSitesRow rise(int map, String key) {
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SURVIVAL) && s.map() == map && s.kind().equals("rise") && s.id().endsWith("_rise_" + key)) return s;
        throw new IllegalStateException("no modeSites survival rise row " + key + " on map " + map);
    }
    /** The bot tier the round's waves take: the March battle's, else the lobby's. */
    static Spec.BotCommandRow tier(GravesAndGlory.Battle b) {
        String id = b.campaign.activeBattle != null ? b.campaign.activeBattle.difficulty() : b.campaign.difficulty;
        for (var r : Spec.BOTCOMMAND) if (r.id().equals(id)) return r;
        return Spec.BOTCOMMAND[0];
    }

    /** The director: team 1's army, never in Battle.squads. */
    GravesAndGlory.Squad director;
    /** The wave in play, 0 before the first rises; the wave being warned of (current + 1) while warnUntil is set. */
    int current;
    /** The tick the warned wave rises (0 when none is warned), and the tick the current wave's timer ends. */
    long riseAt, waveEnds;
    /** The current wave: troops it raised, whether it was cleared and paid. */
    int risen; boolean cleared;
    /** The troops still to rise this wave while maxDeadAlive is reached: troop id and site. */
    final ArrayDeque<Object[]> pending = new ArrayDeque<>();
    /** Unbroken seconds the dead have held the shrine with no defender on it. */
    int lost;
    /** The slot the next risen troop takes (cycled over the squad size, for formation offsets). */
    int slot;
    boolean over, started;
    /** The world time before the night, put back at teardown; and whether this round raised the court's pole. */
    long timeBefore = -1; boolean pole;
    /** The defending banners counted at setup, and the shrine and court of the map. */
    int banners = 1;
    Spec.ModeSitesRow shrine, court;

    SurvivalMode(Spec.ModesRow row) {
        super(row);
    }

    @Override public int ruleN() { return WAVES.size(); }
    @Override public boolean capturesPoints(GravesAndGlory.Battle b) { return false; }
    /** The round ends itself (last wave or shrine lost); the frame's clock never decides it. */
    @Override public boolean overtime(GravesAndGlory.Battle b) { return !over; }
    @Override public int timeoutWinner(GravesAndGlory.Battle b) { return DEFENDERS; }
    @Override public String howToSide(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return "defend"; }
    @Override public String startLine(GravesAndGlory.Battle b) { return "Hold the shrine through " + WAVES.size() + " waves"; }

    Spec.ModeSitesRow shrine(GravesAndGlory.Battle b) { if (shrine == null) shrine = site(b.campaign.map, "shrine"); return shrine; }
    Spec.ModeSitesRow court(GravesAndGlory.Battle b) {
        if (court == null) for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SURVIVAL) && s.map() == b.campaign.map && s.kind().equals("base") && s.stage() == DEFENDERS) court = s;
        if (court == null) throw new IllegalStateException("no modeSites survival court on map " + b.campaign.map);
        return court;
    }
    /** The shrine's centre at the world's surface. */
    Vec3d shrineCentre(GravesAndGlory.Battle b) { var s = shrine(b); return surface(b, s.x(), s.z()); }
    static Vec3d surface(GravesAndGlory.Battle b, double x, double z) {
        int y = b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
        return new Vec3d(x, y, z);
    }
    /** The court is the defenders' base, respawn and barracks; the dead fall back on the shrine they want. */
    @Override public Vec3d fieldBase(GravesAndGlory.Battle b, int team) {
        if (team == DEFENDERS) { var c = court(b); return surface(b, c.x(), c.z()); }
        return shrineCentre(b);
    }
    @Override public Vec3d fieldBarracks(GravesAndGlory.Battle b, int team) { return team == DEFENDERS ? fieldBase(b, team) : super.fieldBarracks(b, team); }
    /** Allied bots stand on the shrine (Campaign.botDecide holds them there, as defenders). */
    @Override public Vec3d botTarget(GravesAndGlory.Battle b, GravesAndGlory.Squad s) { return s.team == DEFENDERS ? shrineCentre(b) : null; }
    @Override public Vec3d botGoal(GravesAndGlory.Battle b, GravesAndGlory.Squad s, Vec3d centre, Spec.BotCommandRow tier) { return s.team == DEFENDERS ? shrineCentre(b) : null; }
    /** The director's Graveborn trait is its tier's waveTraitTier, not the slots its living dead add up to. */
    @Override public void traits(GravesAndGlory.Battle b, GravesAndGlory.Squad s) {
        if (s != director) return;
        s.traits.clear();
        int t = tier(b).waveTraitTier();
        if (t > 0) s.traits.put("graveborn", t);
    }

    /**
     * Seats the round (every seated human defends; team 1 has no banner; allied bots fill team 0 to the room size, at most four
     * banners), takes the flags down, makes the night, raises the court's pole and makes the director. Runs before the round's
     * first reinforce, so the defenders form at the court.
     */
    @Override public void setup(GravesAndGlory.Battle b) {
        var c = b.campaign;
        b.squads.values().removeIf(s -> s.bot && s.team == DEAD);
        int humans = 0;
        for (var s : b.squads.values()) if (s.seated && !s.bot) { s.team = DEFENDERS; humans++; }
        int want = Math.min(4, Math.max(Math.max(1, humans), c.practice ? 1 : c.size));
        var bots = new ArrayList<GravesAndGlory.Squad>();
        for (var s : b.squads.values()) if (s.seated && s.bot && s.team == DEFENDERS) bots.add(s);
        for (int i = bots.size() - 1; i >= 0 && humans + bots.size() > want; i--) b.squads.remove(bots.remove(i).owner);
        for (var s : b.squads.values()) if (s.seated) s.target = fieldBase(b, DEFENDERS);
        c.chooseSides();
        banners = 0;
        for (var s : b.squads.values()) if (s.seated && s.team == DEFENDERS) banners++;
        banners = Math.max(1, banners);
        for (var p : b.points) Arena.unflag(b.world, p.spec);
        if (timeBefore < 0) timeBefore = b.world.getTimeOfDay();
        b.world.setTimeOfDay(NIGHT);
        raisePole(b);
        director = new GravesAndGlory.Squad(DIRECTOR, DEAD, shrineCentre(b));
        director.bot = true; director.order = "attack"; director.target = shrineCentre(b); director.tier = tier(b).id();
        traits(b, director);
        b.roundSeconds = R.riseWarning();
        for (var w : WAVES) b.roundSeconds += w.seconds();
    }

    /** The marker keeps the time before the night, so a crash wakes the world at its own hour. */
    @Override public void mark(GravesAndGlory.Battle b) {
        Modes.mark(b, timeBefore >= 0 ? timeBefore : b.world.getTimeOfDay(), true, b.saves.stream().map(s -> s.toJson().toString()).toList());
    }

    /** The court's banner pole, only where every block of it is air, so it never cuts into the shrine. */
    void raisePole(GravesAndGlory.Battle b) {
        var c = court(b);
        int x = (int) Math.floor(c.x()), z = (int) Math.floor(c.z()), y = b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        for (int dy = 1; dy <= Arena.POINT_POLE + 1; dy++) if (!b.world.getBlockState(new BlockPos(x, y + dy, z)).isAir()) return;
        for (int dx = 1; dx <= 3; dx++) for (int dy = Arena.POINT_POLE - 2; dy < Arena.POINT_POLE; dy++) if (!b.world.getBlockState(new BlockPos(x + dx, y + dy, z)).isAir()) return;
        Arena.flag(b.world, x, y, z, DEFENDERS);
        pole = true;
    }
    /** Takes the court's pole down where it stands (a fence at its foot), whether this object raised it or a crashed round did. */
    static void lowerPole(GravesAndGlory.Battle b, int map) {
        Spec.ModeSitesRow c = null;
        for (var s : Spec.MODESITES) if (s.mode().equals(Modes.SURVIVAL) && s.map() == map && s.kind().equals("base") && s.stage() == DEFENDERS) c = s;
        if (c == null) return;
        int x = (int) Math.floor(c.x()), z = (int) Math.floor(c.z());
        for (int y = b.world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z); y > b.world.getBottomY() + 1; y--) {
            if (!b.world.getBlockState(new BlockPos(x, y, z)).isOf(Blocks.SEA_LANTERN)) continue;
            int foot = y - Arena.POINT_POLE - 1;
            if (b.world.getBlockState(new BlockPos(x, foot + 1, z)).isOf(Blocks.OAK_FENCE)) Arena.unflag(b.world, x, foot, z);
            return;
        }
    }

    /** The director's living dead. */
    int standing(GravesAndGlory.Battle b) {
        int n = 0;
        for (var u : b.units) if (u.squad == director && u.mob.isAlive()) n++;
        return n;
    }

    /** Each tick: the warning's soul fire, the rise, the queue under maxDeadAlive, and each second the wave's clear and the shrine. */
    @Override public void tick(GravesAndGlory.Battle b) {
        if (over || director == null) return;
        if (!started) { started = true; warn(b, b.time); }
        if (riseAt > 0) {
            if (b.time % 5 == 0) for (var key : sites(WAVES.get(current))) {
                var s = rise(b.campaign.map, key);
                Vec3d at = surface(b, s.x(), s.z());
                b.world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 0.5, at.z, 10, s.radius() / 2, 0.5, s.radius() / 2, 0.02);
            }
            if (b.time >= riseAt) rise(b);
        } else if (current < WAVES.size() && b.time >= waveEnds - WARNING_TICKS) warn(b, waveEnds);
        if (!pending.isEmpty() && b.time % 10 == 0) drain(b);
        if (b.time % 20 != 0 || current == 0) return;
        int alive = standing(b);
        var wave = WAVES.get(current - 1);
        if (!cleared && pending.isEmpty() && alive == 0) {
            cleared = true;
            for (var s : b.squads.values()) if (s.seated && s.team == DEFENDERS) s.earn(wave.gold());
            feed(b, ModeMath.restLine(current, wave.gold()));
        }
        int[] in = shrineCount(b);
        int was = lost;
        lost = ModeMath.shrineLost(lost, in[DEAD], in[DEFENDERS]);
        if (was == 0 && lost > 0) feed(b, ModeMath.SHRINE_LINE);
        int result = ModeMath.survivalResult(current, WAVES.size(), cleared, riseAt == 0 && b.time >= waveEnds, lost, shrine(b).seconds());
        if (result >= 0) end(b, result);
    }

    /** The defenders (troops and seated players) and the dead inside the shrine ring. */
    int[] shrineCount(GravesAndGlory.Battle b) {
        int[] in = {0, 0};
        var s = shrine(b);
        Vec3d c = new Vec3d(s.x(), 65, s.z());
        double r2 = s.radius() * s.radius();
        for (var u : b.units) if (u.mob.isAlive() && GravesAndGlory.Battle.horizontalDistance(u.mob.getPos(), c) < r2) in[u.squad == director ? DEAD : DEFENDERS]++;
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) {
            var q = b.squads.get(p.getUuid());
            if (q != null && q.seated && q.team == DEFENDERS && p.isAlive() && !p.isSpectator() && GravesAndGlory.Battle.horizontalDistance(p.getPos(), c) < r2) in[DEFENDERS]++;
        }
        return in;
    }

    /** The rise sites of a wave, in group order without repeats. */
    static List<String> sites(Spec.WavesRow wave) {
        var out = new ArrayList<String>();
        for (var g : groups(wave)) if (!out.contains(g.site())) out.add(g.site());
        return out;
    }

    /** Warns of the next wave: its sites' names on the feed and the soul sound there; it rises at the tick given (or riseWarning from now). */
    void warn(GravesAndGlory.Battle b, long at) {
        var wave = WAVES.get(current);
        riseAt = at <= b.time ? b.time + WARNING_TICKS : at;
        var names = new ArrayList<String>();
        for (var key : sites(wave)) {
            var s = rise(b.campaign.map, key);
            names.add(s.name());
            b.world.playSound(null, s.x(), surface(b, s.x(), s.z()).y, s.z(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 4f, 0.6f);
        }
        for (var line : ModeMath.riseLines(names)) feed(b, line);
        Audio.all(b, AudioCues.Cue.WAVE_WARNING, "wave");
    }

    /** The warned wave rises: its groups' counts scaled for the banners and the tier, up to maxDeadAlive standing, the rest queued. */
    void rise(GravesAndGlory.Battle b) {
        var wave = WAVES.get(current);
        current++; riseAt = 0; waveEnds = b.time + wave.seconds() * 20L; risen = 0; cleared = false;
        Audio.all(b, AudioCues.Cue.WAVE_RISE, "rise");
        double scale = tier(b).waveScale();
        for (var g : groups(wave)) {
            int n = ModeMath.waveCount(g.count(), banners, R.survivalPerBanner(), scale);
            for (int i = 0; i < n; i++) pending.add(new Object[]{g.troop(), g.site()});
            risen += n;
        }
        drain(b);
    }

    /** Raises queued troops while fewer than maxDeadAlive stand. */
    void drain(GravesAndGlory.Battle b) {
        int alive = standing(b);
        while (!pending.isEmpty() && alive < R.maxDeadAlive()) {
            var next = pending.poll();
            var spec = Campaign.troop((String) next[0]);
            if (spec == null) continue;
            var s = rise(b.campaign.map, (String) next[1]);
            double r = s.radius(), x = s.x() + (b.world.random.nextDouble() * 2 - 1) * r, z = s.z() + (b.world.random.nextDouble() * 2 - 1) * r;
            var u = b.spawn(director, spec, slot++ % Spec.RULES[0].squadSize(), surface(b, x, z).add(0, 0.1, 0));
            u.mob.addCommandTag(TAG);
            alive++;
        }
        traits(b, director);
    }

    void end(GravesAndGlory.Battle b, int winner) {
        over = true;
        feed(b, winner == DEFENDERS ? ModeMath.STANDS_LINE : ModeMath.HOLD_LINE);
        b.campaign.finish(winner);
        b.world.playSound(null, BlockPos.ORIGIN, winner == DEFENDERS ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 2, 1);
    }

    /** The objective panel's clock: seconds to the next wave (the warning's while one is warned). */
    @Override public int clock(GravesAndGlory.Battle b) {
        if (!b.running) return WAVES.isEmpty() ? 0 : WAVES.get(0).seconds();
        long until = riseAt > 0 ? riseAt : waveEnds;
        return (int) Math.max(0, (until - b.time + 19) / 20);
    }
    /** The wave in play and the dead standing (queued ones count). */
    @Override public int[] ledger(GravesAndGlory.Battle b) { return new int[]{current, director == null ? 0 : standing(b) + pending.size()}; }
    @Override public int[] max(GravesAndGlory.Battle b) { return new int[]{WAVES.size(), risen}; }
    @Override public String[] formats() { return new String[]{"fraction", "plain"}; }
    @Override public JsonObject snapshot(GravesAndGlory.Battle b, GravesAndGlory.Squad viewer) {
        var j = super.snapshot(b, viewer);
        j.addProperty("wave", current); j.addProperty("waves", WAVES.size()); j.addProperty("nextIn", clock(b));
        return j;
    }

    /** A Survival line on every player's feed, in Dawn's colour. */
    static void feed(GravesAndGlory.Battle b, String text) {
        for (ServerPlayerEntity p : b.server.getPlayerManager().getPlayerList()) FeedEvents.send(p, "point", DEFENDERS, text);
    }

    /** After teardown: the dead go, the flags rise, the court's pole comes down and the world wakes at its own hour. Idempotent. */
    @Override protected void undo(GravesAndGlory.Battle b) {
        if (director != null) b.units.removeIf(u -> { if (u.squad == director) { GravesAndGlory.Battle.leaveSide(u.mob); u.mob.discard(); return true; } return false; });
        pending.clear();
        removeStray(b);
        for (var p : b.points) Arena.flag(b.world, p.spec, -1);
        if (pole) { lowerPole(b, b.campaign.map); pole = false; }
        if (timeBefore >= 0) { b.world.setTimeOfDay(timeBefore); timeBefore = -1; }
        over = true;
    }
    /** A crash left the dead in loaded chunks and the court's pole up (Modes.recover put the time and the flags back): remove them. */
    @Override public void recover(GravesAndGlory.Battle b, JsonObject marker) {
        removeStray(b);
        if (!marker.has("map") || marker.get("map").getAsInt() == b.campaign.map) lowerPole(b, b.campaign.map); // another map's world was rebuilt
    }
    static void removeStray(GravesAndGlory.Battle b) {
        for (var e : b.world.getEntitiesByType(TypeFilter.instanceOf(MobEntity.class), e -> e.getCommandTags().contains(TAG))) e.discard();
    }
}
