package gg.gravesandglory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * What each viewer is told an entity or map block is (plain clients, port plan P3; the P2 spike's looks). Polymer asks per viewer
 * when it writes a packet, possibly off the server thread (the mixins of gg.gravesandglory.polymer.mixin call these).
 * <ul>
 * <li>Troops: a modpack viewer gets the real type. A plain viewer gets a vanilla mob by role: a bow troop a skeleton, a crossbow a
 *     pillager, support a witch, an axe a vindicator; anything else keeps its server mob (P4.1 widens the table).</li>
 * <li>The supply wagon and the two map blocks: the real entry only to a modpack viewer whose raw ids match the server's
 *     (VersionGate.rawIds); everyone else gets the stand-in (a chest minecart, a barrier), which every client can decode.</li>
 * </ul>
 */
public final class PlainLooks {
    private PlainLooks() {}
    /** The troop row of each tracked army mob by entity id: filled on the server thread when a player starts tracking it (read when
     *  Polymer writes the spawn packet), dropped when the entity unloads. */
    static final Map<Integer, Spec.TroopsRow> TROOPS = new ConcurrentHashMap<>();

    static void init() {
        EntityTrackingEvents.START_TRACKING.register((entity, player) -> {
            var b = GravesAndGlory.battle;
            if (b == null || !ArmyMobs.isTroop(entity) || TROOPS.containsKey(entity.getId())) return;
            if (b.spawning != null && b.spawning.mob == entity) { TROOPS.put(entity.getId(), b.spawning.spec); return; }
            for (var u : b.units) if (u.mob == entity) { TROOPS.put(entity.getId(), u.spec); return; }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> TROOPS.remove(entity.getId()));
    }

    /** A troop's type as this viewer sees it. */
    public static EntityType<?> look(Entity e, ServerPlayerEntity viewer) {
        EntityType<?> real = e.getType();
        if (viewer == null || !VersionGate.plain(viewer)) return real;
        Spec.TroopsRow t = TROOPS.get(e.getId());
        if (t == null && e.hasCustomName()) t = ArmyMobs.named(e.getCustomName().getString());
        return t == null ? real : byRole(t, real);
    }

    /** The plain look of a troop row (pure: the role table). */
    static EntityType<?> byRole(Spec.TroopsRow t, EntityType<?> real) {
        String w = t.weapon();
        if (t.ranged() && w.contains("crossbow")) return EntityType.PILLAGER;
        if (t.ranged() && w.endsWith(":bow")) return EntityType.SKELETON;
        if (t.group().equals("support")) return EntityType.WITCH;
        if (w.contains("_axe")) return EntityType.VINDICATOR;
        return real;
    }

    /** Whether this viewer is sent the real wagon and map blocks. No viewer (a packet built for nobody in particular): the stand-in. */
    public static boolean realBlocks(ServerPlayerEntity viewer) {
        return viewer != null && VersionGate.rawIds(viewer);
    }

    public static EntityType<?> wagon(ServerPlayerEntity viewer) {
        return realBlocks(viewer) ? WagonEntity.TYPE : EntityType.CHEST_MINECART;
    }
}
