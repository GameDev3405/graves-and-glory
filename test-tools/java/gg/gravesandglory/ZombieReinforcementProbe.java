package gg.gravesandglory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import java.util.*;

/**
 * Unshipped. Only with -Dgravesandglory.zombieProbe=1 (JAVA_TOOL_OPTIONS) beside any test-tools server run. Audit proposal F: with the server on
 * hard difficulty and mob spawning on, no troop that is a vanilla zombie (zombie, husk, drowned) may call reinforcements when hurt.
 * For every sheet troop whose mob is a ZombieEntity it prints the ZOMBIE_SPAWN_REINFORCEMENTS attribute, then hurts a fresh copy 400 times
 * from a mob attacker (hurt invulnerability cleared each time) and counts the vanilla zombies that appear that are not troops. A control copy
 * with the attribute forced to 1.0 shows the test could see reinforcements if there were any. Prints [zprobe] lines, then stops the server.
 */
public final class ZombieReinforcementProbe implements ModInitializer {
    boolean done;
    int waited;

    @Override public void onInitialize() {
        if (!"1".equals(System.getProperty("gravesandglory.zombieProbe"))) return;
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    void tick(MinecraftServer server) {
        if (done) return;
        var b = GravesAndGlory.battle;
        if (b == null || !b.ready) { if (++waited > 6000) { System.out.println("[zprobe] battle never ready"); done = true; server.stop(false); } return; }
        if (++waited < 100) return;
        done = true;
        try { run(server, b.world); } catch (Throwable t) { System.out.println("[zprobe] FAILED " + t); t.printStackTrace(); }
        server.stop(false);
    }

    void run(MinecraftServer server, ServerWorld w) {
        server.setDifficulty(Difficulty.HARD, true);
        w.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(true, server);
        System.out.println("[zprobe] difficulty " + w.getDifficulty() + ", doMobSpawning " + w.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING));
        BlockPos spawn = w.getSpawnPos();
        int x = spawn.getX(), z = spawn.getZ();
        int y = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        System.out.println("[zprobe] test spot " + x + "," + y + "," + z + " chunk loaded " + w.isChunkLoaded(x >> 4, z >> 4));
        Box wide = new Box(x - 64, y - 32, z - 64, x + 64, y + 64, z + 64);
        MobEntity attacker = ArmyMobs.create(Spec.TROOPS[0].entity(), w);
        attacker.refreshPositionAndAngles(x + 1.5, y, z, 0, 0);
        w.spawnEntity(attacker);
        Set<String> seen = new TreeSet<>();
        int zombies = 0, totalHits = 0, totalNew = 0;
        double maxAttr = 0;
        for (var t : Spec.TROOPS) {
            if (!seen.add(t.entity())) continue;
            MobEntity m;
            try { m = ArmyMobs.create(t.entity(), w); } catch (RuntimeException e) { continue; }
            if (!(m instanceof ZombieEntity zm)) continue;
            zombies++;
            EntityAttributeInstance attr = zm.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS);
            double value = attr == null ? Double.NaN : zm.getAttributeValue(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS);
            maxAttr = Math.max(maxAttr, value);
            int before = count(w, wide);
            zm.refreshPositionAndAngles(x, y, z, 0, 0);
            zm.setHealth(1000);
            if (zm.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH) != null) zm.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100000);
            zm.setHealth(100000);
            w.spawnEntity(zm);
            int hits = hurt(zm, attacker, 400);
            int after = count(w, wide);
            totalHits += hits; totalNew += after - before;
            System.out.println("[zprobe] " + t.entity() + " (" + zm.getClass().getSimpleName() + "): attribute " + value + ", " + hits + " hits landed, new vanilla zombies " + (after - before));
            zm.discard();
        }
        // Control: the same family with the attribute forced to 1.0 must call help, or the probe proves nothing.
        MobEntity ctl = ArmyMobs.create("zombie", w);
        if (ctl instanceof ZombieEntity zc) {
            zc.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(1.0);
            zc.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(100000);
            zc.setHealth(100000);
            zc.refreshPositionAndAngles(x, y, z, 0, 0);
            w.spawnEntity(zc);
            int before = count(w, wide);
            int hits = hurt(zc, attacker, 5);
            int after = count(w, wide);
            System.out.println("[zprobe] CONTROL zombie with attribute 1.0: " + hits + " hits landed, new vanilla zombies " + (after - before));
            zc.discard();
        }
        attacker.discard();
        for (var e : w.getEntitiesByClass(ZombieEntity.class, wide, e -> !(e instanceof ArmyMob))) e.discard();
        System.out.println("[zprobe] RESULT zombie-family troop entities " + zombies + ", max attribute " + maxAttr + ", hits " + totalHits + ", reinforcements spawned " + totalNew);
    }

    static int hurt(ZombieEntity target, MobEntity attacker, int times) {
        int landed = 0;
        for (int i = 0; i < times; i++) {
            target.timeUntilRegen = 0; target.hurtTime = 0;
            if (target.damage(target.getWorld().getDamageSources().mobAttack(attacker), 1f)) landed++;
        }
        return landed;
    }

    static int count(ServerWorld w, Box box) { return w.getEntitiesByClass(ZombieEntity.class, box, e -> !(e instanceof ArmyMob)).size(); }
}
