package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.world.GameMode;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Two real development clients over a loopback LAN: the in-match progression Task 10 step 4 (never shipped; the plan's p2-rank-late-join "in the real
 * network path"). The host starts a 2v2 practice-less skirmish, levels and gears some of its own soldiers, opens the world to LAN and waits. The guest
 * joins mid-battle (AdmissionPolicy gives him a bot seat of the host's team, so he spawns at the host's base among soldiers the server already tracks),
 * then reads what its own client holds: the ranks (rank_v1), the chevrons and tiers it draws, and the Armoury its state_v9 carries (his own army's, not
 * the host's). A level up and a gear buy after the join check the live path. Both roles write their p2-net-* ids through Phase2Checks.report into
 * their own run root's evidence file.
 *
 * Roles come from -Dgravesandglory.rankNet=host|join (JAVA_TOOL_OPTIONS on each launch) and the shared folder from -Dgravesandglory.rankNetRoot.
 * Files in the shared folder: address.txt (host), guest-ready.txt (guest), expect-1.txt and armoury-1.txt (host), guest-1.txt (guest), expect-2.txt (host),
 * guest-done.txt (guest). Lines of expect-N.txt: entityId level weaponTier armourTier troopId.
 */
public final class RankNetworkChecks implements ClientModInitializer {
    String role = System.getProperty("gravesandglory.rankNet", "");
    final Path control = Path.of(System.getProperty("gravesandglory.rankNetRoot", "work/ranknet"));
    int clock, wait, step;
    boolean started, shot1;

    static void check(String id, boolean pass, String why) { Phase2Checks.report("p2-net-" + id, pass, why); }
    static boolean has(Path dir, String name) { return Files.exists(dir.resolve(name)); }
    static void put(Path dir, String name, String text) { try { Files.createDirectories(dir); Files.writeString(dir.resolve(name), text); } catch (Exception e) { throw new RuntimeException(e); } }
    static String get(Path dir, String name) { try { return Files.readString(dir.resolve(name)).trim(); } catch (Exception e) { throw new RuntimeException(e); } }
    static String rank(RankSync.Rank r) { return r == null ? "none" : r.level() + "/" + r.weaponTier() + "/" + r.armourTier(); }
    static void server(MinecraftClient c, java.util.function.Consumer<GravesAndGlory.Battle> f) {
        c.getServer().execute(() -> {
            var b = GravesAndGlory.battle; if (b == null) return;
            try { f.accept(b); } catch (Exception e) { check("server-exception", false, e.toString()); e.printStackTrace(); }
        });
    }

    @Override public void onInitializeClient() {
        if (!role.equals("host") && !role.equals("join")) return;
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            try {
                c.options.pauseOnLostFocus = false;
                clock++;
                if (c.getOverlay() != null) return;
                if (role.equals("host")) host(c); else guest(c);
            } catch (Exception e) { check("client-exception", false, e.toString()); e.printStackTrace(); c.scheduleStop(); }
        });
    }

    // ---- host ----

    volatile boolean skirmished, prepared, joinedSeen, relocated, live;
    final List<String> lines = new ArrayList<>();
    GravesAndGlory.Unit k1, k2, k3, a1;
    UUID hostId, guestId;
    String hostRows = "", guestRows = "";
    int port, quiet;

    void host(MinecraftClient c) {
        if (!started) {
            if (c.currentScreen instanceof FrontMenuScreen && clock >= 60) { started = true; GravesAndGloryClient.startCamp(c, false); }
            return;
        }
        if (c.player == null || c.getServer() == null || !GravesAndGloryClient.ready) return;
        hostId = c.player.getUuid();
        wait++;
        switch (step) {
            case 0 -> { // the skirmish: 2v2 with bot fill (so the guest can take a bot's seat), then the host's own banner
                if (!skirmished) server(c, b -> { var p = b.server.getPlayerManager().getPlayer(hostId); if (p != null && !skirmished && Phase1Checks.skirmish(b, p, 0, 2, "normal")) { skirmished = true; wait = 0; } });
                if (skirmished && wait > 30) {
                    server(c, b -> { var own = b.squads.get(hostId); Phase2Checks.field(b, own, Phase2Checks.banner("knight*6,archer*4,mage*2")); });
                    wait = 0; step = 1;
                }
            }
            case 1 -> {
                if (wait < 60) return;
                server(c, this::prepare);
                wait = 0; step = 2;
            }
            case 2 -> { // the world opens to LAN once the soldiers are levelled and geared
                if (!prepared) return;
                server(c, b -> {
                    try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); } catch (Exception e) { throw new RuntimeException(e); }
                    // The enemy bots' seats are closed for the guest's admission only: he takes the host's ally bot, so he spawns at the host's base among the soldiers the server tracks already.
                    for (var s : b.squads.values()) if (s.bot && s.team != b.squads.get(hostId).team) s.seated = false;
                    check("lan-open", b.server.openToLan(GameMode.ADVENTURE, false, port), "the host's integrated server opened to LAN on 127.0.0.1:" + port);
                    put(control, "address.txt", "127.0.0.1:" + port);
                });
                wait = 0; step = 3;
            }
            case 3 -> { // the guest joins the running battle
                if (c.getServer().getPlayerManager().getPlayerList().size() < 2) return;
                if (!joinedSeen) {
                    joinedSeen = true;
                    server(c, b -> {
                        for (var p : b.server.getPlayerManager().getPlayerList()) if (!p.getUuid().equals(hostId)) guestId = p.getUuid();
                        var gs = b.squads.get(guestId);
                        for (var s : b.squads.values()) if (s.bot && s.team != b.squads.get(hostId).team && !s.seated) s.seated = true; // the enemy seats open again
                        var own = b.squads.get(hostId);
                        check("guest-seated", gs != null && gs.seated && !gs.bot && gs.team == own.team && gs != own, "the guest joined the running 2v2 and holds a seat of his own on the host's team: squad " + (gs == null ? "none" : "team " + gs.team + ", seated " + gs.seated + ", bot " + gs.bot) + ", the host's team " + own.team);
                    });
                }
                if (!has(control, "guest-ready.txt")) return;
                wait = 0; step = 4;
            }
            case 4 -> { // the guest stands in front of the line; the server's ranks and the guest's own Armoury go to the shared folder
                if (wait == 1) server(c, b -> {
                    var g = b.server.getPlayerManager().getPlayer(guestId); var gs = b.squads.get(guestId); var own = b.squads.get(hostId);
                    double x = k2.mob.getX() + 1, z = k2.mob.getZ() + 9; // the line runs along x and faces south (yaw 0): the guest stands south of it, looking north (yaw 180)
                    b.relocate(g, x, Arena.ground((int) Math.floor(x), (int) Math.floor(z)) + 1, z, 180f, 6f);
                    var hostArmoury = Veterans.armouryState(b, own, b.server.getPlayerManager().getPlayer(hostId)); var guestArmoury = Veterans.armouryState(b, gs, g);
                    hostRows = hostArmoury.rows().toString(); guestRows = guestArmoury.rows().toString();
                    put(control, "armoury-1.txt", "guest=" + guestRows + "\nhost=" + hostRows);
                    put(control, "expect-1.txt", String.join("\n", snapshot(b)));
                });
                if (wait == 3) { put(control, "go-1.txt", "go"); wait = 0; step = 5; }
            }
            case 5 -> { // the guest has read it; a level up and a gear buy after the join check the live path
                if (!has(control, "guest-1.txt")) return;
                server(c, b -> {
                    var own = b.squads.get(hostId);
                    Veterans.credit(b, a1, LevelChecks.L2); // the archer reaches level 2: his first chevron
                    Veterans.purchase(b, own, Campaign.troop("knight"), LevelMath.ARMOUR, 3, LevelMath.price(Campaign.troop("knight").rarity(), 3)); // the level-3 squire wears armour III
                    put(control, "expect-2.txt", String.join("\n", List.of(line(a1), line(k3), line(k2), line(k1))));
                });
                wait = 0; step = 6;
            }
            case 6 -> {
                if (has(control, "guest-done.txt") || wait > 2400) {
                    check("host-saw-guest-done", has(control, "guest-done.txt"), "the guest finished its checks and wrote guest-done.txt (waited " + wait + " ticks)");
                    wait = 0; step = 7;
                }
            }
            case 7 -> { if (wait > 40) c.scheduleStop(); }
            default -> {}
        }
    }

    static String line(GravesAndGlory.Unit u) { var r = RankSync.of(u); return u.mob.getId() + " " + r.level() + " " + r.weaponTier() + " " + r.armourTier() + " " + u.spec.id(); }

    /** The host's soldiers (alive, not temporary), one line each: the four levelled ones first, then the rest of the host's army. */
    List<String> snapshot(GravesAndGlory.Battle b) {
        var own = b.squads.get(hostId); List<String> out = new ArrayList<>(List.of(line(k3), line(k2), line(k1), line(a1)));
        for (var u : b.units) if (u.squad == own && u.mob.isAlive() && u != k1 && u != k2 && u != k3 && u != a1) out.add(line(u));
        return out;
    }

    /** Levels and gear the host's own soldiers: K3 level 3, K2 level 2, K1 and A1 level 1; Weapon II and III and Armour II bought. The four are frozen in a line on open ground near the base. */
    void prepare(GravesAndGlory.Battle b) {
        var own = b.squads.get(hostId);
        List<GravesAndGlory.Unit> knights = new ArrayList<>(), archers = new ArrayList<>();
        for (var u : b.units) if (u.squad == own && u.mob.isAlive() && !u.temporary) { if (u.spec.id().equals("knight")) knights.add(u); else if (u.spec.id().equals("archer")) archers.add(u); }
        check("host-fielded", knights.size() >= 3 && !archers.isEmpty(), "the host's own banner fielded " + knights.size() + " Order Squires and " + archers.size() + " Order Archers (want 3 and 1 or more)");
        k3 = knights.get(0); k2 = knights.get(1); k1 = knights.get(2); a1 = archers.get(0);
        var base = GravesAndGlory.Battle.base(own.team);
        double bx = base.x + (own.team == 0 ? 14 : -14), bz = base.z; // clear ground in front of the base
        var all = List.of(k1, k2, k3, a1);
        for (int i = 0; i < all.size(); i++) {
            var u = all.get(i); double x = bx + i * 2 - 3;
            u.mob.refreshPositionAndAngles(x + .5, Arena.ground((int) Math.floor(x), (int) Math.floor(bz)) + 1, bz + .5, 0f, 0f);
            Phase1Checks.freeze(u); u.mob.setAiDisabled(true);
        }
        own.gold = 5000;
        var knight = Campaign.troop("knight");
        Veterans.credit(b, k3, LevelChecks.L3); Veterans.credit(b, k2, LevelChecks.L2);
        Veterans.purchase(b, own, knight, LevelMath.WEAPON, 2, LevelMath.price(knight.rarity(), 2));
        Veterans.purchase(b, own, knight, LevelMath.WEAPON, 3, LevelMath.price(knight.rarity(), 3));
        Veterans.purchase(b, own, knight, LevelMath.ARMOUR, 2, LevelMath.price(knight.rarity(), 2));
        check("host-levelled", k3.level == 3 && k3.weaponTier == 3 && k3.armourTier == 2 && k2.level == 2 && k2.weaponTier == 2 && k2.armourTier == 2 && k1.level == 1 && k1.weaponTier == 1 && k1.armourTier == 1 && a1.level == 1,
            "before the guest joins: Squire K3 " + line(k3) + ", K2 " + line(k2) + ", K1 " + line(k1) + ", Archer " + line(a1) + " (want 3/3/2, 2/2/2, 1/1/1 and level 1)");
        prepared = true;
    }

    // ---- guest ----

    final List<String[]> want = new ArrayList<>();
    boolean armouryDone, firstDone;
    net.minecraft.entity.mob.MobEntity portraitK, portraitA;
    long since;

    void guest(MinecraftClient c) {
        if (!started) {
            if (c.currentScreen instanceof FrontMenuScreen && clock >= 60 && has(control, "address.txt")) {
                started = true;
                String address = get(control, "address.txt");
                ConnectScreen.connect(new FrontMenuScreen(), c, ServerAddress.parse(address), new ServerInfo("Rank network check", address, false), false);
            }
            return;
        }
        if (step == 4) { if (++wait > 60) c.scheduleStop(); return; } // the host closes its world when it is done, so the guest may be disconnected by now
        if (c.player == null || c.world == null || !GravesAndGloryClient.ready) return;
        wait++;
        switch (step) {
            case 0 -> { // in the battle: ask the host to place us
                if (!GravesAndGloryClient.battleActive() || wait < 40) return;
                // portraits are client-only mobs with client-made ids (the camp's troop cards); a rank set on one must never reach a world troop of the same id (Task 9 review I1)
                portraitK = ArmyCards.portrait(Campaign.troop("knight"), 3, 2); portraitA = ArmyCards.portrait(Campaign.troop("archer"), 1, 1);
                put(control, "guest-ready.txt", "ready");
                wait = 0; step = 1;
            }
            case 1 -> {
                if (!has(control, "go-1.txt")) return;
                if (want.isEmpty()) for (String l : get(control, "expect-1.txt").split("\n")) want.add(l.trim().split(" "));
                wait = 0; step = 2;
            }
            case 2 -> { // the ranks, tiers and chevrons of the four levelled soldiers, as drawn by this client
                if (wait < 20) return;
                boolean ok = true, drew = true; StringBuilder why = new StringBuilder(); int present = 0, equal = 0, others = 0, othersEqual = 0;
                for (int i = 0; i < want.size(); i++) {
                    String[] w = want.get(i); int id = Integer.parseInt(w[0]); var want3 = new RankSync.Rank(Integer.parseInt(w[1]), Integer.parseInt(w[2]), Integer.parseInt(w[3]));
                    Entity e = c.world.getEntityById(id); var got = e == null ? null : RankSync.client(e);
                    if (i < 4) {
                        boolean good = e != null && want3.equals(got);
                        if (e != null) present++; if (good) equal++;
                        var d = e == null ? null : TroopRenderer.drawn(e); var last = d == null ? null : d.last;
                        var mark = e == null ? null : TroopChevrons.drawn(e);
                        int chevrons = want3.level() - 1;
                        boolean tiers = last != null && last.look().level() == want3.level() && last.look().weaponTier() == want3.weaponTier() && last.look().armourTier() == want3.armourTier();
                        boolean chev = chevrons == 0 ? mark == null || mark.chevrons() == 0 : mark != null && mark.chevrons() == chevrons;
                        ok &= good; drew &= tiers && chev;
                        why.append(w[4]).append(' ').append(rank(got)).append(good ? "" : " (want " + rank(want3) + ")").append(", draws ").append(last == null ? "nothing" : "look " + last.look().level() + "/" + last.look().weaponTier() + "/" + last.look().armourTier()).append(" with ").append(mark == null ? 0 : mark.chevrons()).append(" chevron(s)").append(tiers && chev ? "" : " (want " + chevrons + ")").append("; ");
                    } else if (e != null) { others++; if (want3.equals(got) || got == null && want3.equals(RankSync.Rank.FRESH)) othersEqual++; }
                }
                boolean seen = present == 4 && equal == 4 && drew;
                if (!seen && wait < 900) return; // the entities load and draw over a few seconds
                if (!shot1) { shot1 = true; Phase2Checks.shot(c, "net-guest-ranks"); }
                check("rank-late-join", ok && present == 4, "this guest joined a battle in progress and its own client holds, for the host's four levelled soldiers: " + why.toString().trim() + "; rank_v1 packets read " + RankSync.received + "; the rest of the host's army that this client loaded: " + othersEqual + " of " + others + " hold their server rank");
                check("rank-draw", drew, "the troop renderer and the chevrons on this client draw each soldier at his rank (look level, tiers; one chevron at level 2, two at level 3, none at level 1): " + why.toString().trim());
                var pk = portraitK == null ? null : RankSync.client(portraitK); var pa = portraitA == null ? null : RankSync.client(portraitA);
                boolean clash = false; for (String[] w : want) if (portraitK != null && Integer.parseInt(w[0]) == portraitK.getId() || portraitA != null && Integer.parseInt(w[0]) == portraitA.getId()) clash = true;
                check("portrait-ranks", portraitK != null && portraitA != null && new RankSync.Rank(1, 3, 2).equals(pk) && RankSync.Rank.FRESH.equals(pa) && ok, "this guest's card portraits (client-made ids " + (portraitK == null ? "none" : portraitK.getId() + " and " + portraitA.getId()) + ", sharing an id with a host soldier: " + clash + ") hold their own ranks Squire " + rank(pk) + " (want 1/3/2) and Archer " + rank(pa) + " (want 1/1/1), and the host's soldiers above still hold theirs");
                armoury(c);
                put(control, "guest-1.txt", "checked");
                wait = 0; step = 3;
            }
            case 3 -> { // after the host's level up and gear buy: the live path
                if (!has(control, "expect-2.txt")) return;
                List<String[]> two = new ArrayList<>(); for (String l : get(control, "expect-2.txt").split("\n")) two.add(l.trim().split(" "));
                boolean ok = true, drew = true; StringBuilder why = new StringBuilder();
                for (String[] w : two) {
                    var want3 = new RankSync.Rank(Integer.parseInt(w[1]), Integer.parseInt(w[2]), Integer.parseInt(w[3]));
                    Entity e = c.world.getEntityById(Integer.parseInt(w[0])); var got = e == null ? null : RankSync.client(e);
                    var d = e == null ? null : TroopRenderer.drawn(e); var last = d == null ? null : d.last; var mark = e == null ? null : TroopChevrons.drawn(e);
                    boolean good = want3.equals(got), tiers = last != null && last.look().level() == want3.level() && last.look().armourTier() == want3.armourTier() && last.look().weaponTier() == want3.weaponTier();
                    boolean chev = want3.level() == 1 ? mark == null || mark.chevrons() == 0 : mark != null && mark.chevrons() == want3.level() - 1;
                    ok &= good; drew &= tiers && chev;
                    why.append(w[4]).append(' ').append(rank(got)).append(good ? "" : " (want " + rank(want3) + ")").append(", draws ").append(last == null ? "nothing" : last.look().level() + "/" + last.look().weaponTier() + "/" + last.look().armourTier()).append(" with ").append(mark == null ? 0 : mark.chevrons()).append(" chevron(s); ");
                }
                if (!(ok && drew) && wait < 900) return;
                Phase2Checks.shot(c, "net-guest-live");
                check("rank-live", ok && drew, "after the host's Archer reached level 2 and his Armour III was bought (rank_v1 on a change, no new join): " + why.toString().trim());
                put(control, "guest-done.txt", "done");
                wait = 0; step = 4;
            }
            default -> {}
        }
    }

    /** The Armoury this client's state_v9 carries: the guest's own army's rows, which are not the host's. */
    void armoury(MinecraftClient c) {
        String[] files = get(control, "armoury-1.txt").split("\n");
        String guestWant = files[0].substring("guest=".length()), hostRows = files[1].substring("host=".length());
        String got = CampaignClient.armoury.rows().toString();
        check("armoury-own", got.equals(guestWant) && !got.equals(hostRows) && !got.isEmpty(), "the Armoury rows this guest's state_v9 carries are his own army's (the server's rows for his squad), not the host's: client " + got + "; server's guest rows " + guestWant + "; host's rows " + hostRows);
    }
}
