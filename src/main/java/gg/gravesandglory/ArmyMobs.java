package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.*;
import java.util.function.Function;

/**
 * The troop entities (spec section 0). The troops sheet names a mob in its entity column; this class turns that id into the Army*
 * subclass that fights as it, so nothing switches on the id: a House adds its mobs by registering a factory. It also holds the tags a
 * troop carries into the counter table and the stage names its custom name shows.
 */
public final class ArmyMobs {
    private ArmyMobs() {}
    /** Sheet entity id to the factory of the Army* mob that fights as it. Husk, skeleton and wither skeleton are the Gilded and Graveborn mobs; each later House registers its own in its task. */
    static final Map<String, Function<World, MobEntity>> FACTORIES = new LinkedHashMap<>();
    static {
        register("husk", GravesAndGlory.ArmyHusk::new);
        register("skeleton", GravesAndGlory.ArmySkeleton::new);
        register("wither_skeleton", GravesAndGlory.ArmyWither::new);
    }

    /** Registers (or replaces) the factory of a sheet entity id. */
    public static void register(String entity, Function<World, MobEntity> factory) { FACTORIES.put(entity, factory); }

    /** A new troop mob for a sheet entity id. An id nobody registered is a bug in the sheet or a House that is not built yet, so it throws. */
    public static MobEntity create(String entity, World w) {
        Function<World, MobEntity> factory = FACTORIES.get(entity);
        if (factory == null) throw new IllegalStateException("No army mob is registered for the troop entity '" + entity + "'");
        return factory.apply(w);
    }

    /**
     * A mob for a portrait (the recruitment cards, the barracks): the vanilla mob of the sheet's entity id, never an Army* class, so a
     * card draws any troop of any House before its House can take the field. An id the game does not know, or that is not a mob, draws a husk.
     */
    public static MobEntity portrait(String entity, World w) {
        switch (entity) {
            case "husk": return new HuskEntity(EntityType.HUSK, w);
            case "skeleton": return new SkeletonEntity(EntityType.SKELETON, w);
            case "wither_skeleton": return new WitherSkeletonEntity(EntityType.WITHER_SKELETON, w);
            default:
                Entity made = Registries.ENTITY_TYPE.get(new Identifier(entity)).create(w);
                return made instanceof MobEntity mob ? mob : new HuskEntity(EntityType.HUSK, w);
        }
    }

    /** Whether the entity is a troop of the army (an ArmyMob). */
    public static boolean isTroop(Entity e) { return e instanceof ArmyMob; }

    private static final Map<Spec.TroopsRow, Set<String>> TAGS = new java.util.concurrent.ConcurrentHashMap<>();
    /**
     * The tags a troop brings to the counter table (spec 3): its troops.tags column, its ability id and its group. A Hit adds the dynamic tags
     * (melee, arrow, shot, magic, nonsiege, charging, holding) on top. The set is shared and read-only; Unit.tags copies it.
     */
    public static Set<String> tags(Spec.TroopsRow t) {
        return TAGS.computeIfAbsent(t, row -> {
            Set<String> out = new LinkedHashSet<>();
            for (String tag : row.tags().split(",")) if (!tag.isBlank() && !tag.trim().equals("none")) out.add(tag.trim());
            if (!row.ability().equals("none")) out.add(row.ability());
            out.add(row.group());
            return Collections.unmodifiableSet(out);
        });
    }

    /** A troop's custom name at an evolution stage: the troop name at stage 1, the evolutions row's name at stage 2 or 3, and the troop name for a troop with no line (Epics and Legendaries). */
    public static String stageName(Spec.TroopsRow t, int stage) {
        if (stage >= 2) for (var e : Spec.EVOLUTIONS) if (e.troop().equals(t.evolveLine()) && e.stage() == stage) return e.name();
        return t.name();
    }

    private static Spec.TroopsRow[] namedTroops;
    private static Spec.EvolutionsRow[] namedEvolutions;
    private static Map<String, Spec.TroopsRow> names = Map.of();
    /** The troop whose name, or whose evolved stage name, is the given custom name; null for any other name. A troop name wins over a stage name that matches it. */
    public static synchronized Spec.TroopsRow named(String name) {
        if (namedTroops != Spec.TROOPS || namedEvolutions != Spec.EVOLUTIONS) {
            Map<String, Spec.TroopsRow> map = new HashMap<>();
            for (var t : Spec.TROOPS) map.put(t.name(), t);
            for (var t : Spec.TROOPS) for (var e : Spec.EVOLUTIONS) if (e.troop().equals(t.evolveLine())) map.putIfAbsent(e.name(), t);
            names = map;
            namedTroops = Spec.TROOPS;
            namedEvolutions = Spec.EVOLUTIONS;
        }
        return name == null ? null : names.get(name);
    }
}
