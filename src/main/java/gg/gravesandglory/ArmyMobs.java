package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
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
    /** Sheet entity id to the factory of the Army* mob that fights as it. Husk, skeleton, wither skeleton, zombie and stray are the Gilded and Graveborn mobs, vindicator, pillager and iron golem the Ironclad's, drowned, evoker and ravager the Tidecallers', wolf, zombified piglin, spider and polar bear the Wildmoot's. */
    static final Map<String, Function<World, MobEntity>> FACTORIES = new LinkedHashMap<>();
    static {
        register("husk", GravesAndGlory.ArmyHusk::new);
        register("skeleton", GravesAndGlory.ArmySkeleton::new);
        register("wither_skeleton", GravesAndGlory.ArmyWither::new);
        register("zombie", GravesAndGlory.ArmyZombie::new);
        register("stray", GravesAndGlory.ArmyStray::new);
        register("vindicator", GravesAndGlory.ArmyVindicator::new);
        register("pillager", GravesAndGlory.ArmyPillager::new);
        register("iron_golem", GravesAndGlory.ArmyGolem::new);
        register("drowned", GravesAndGlory.ArmyDrowned::new);
        register("evoker", GravesAndGlory.ArmyEvoker::new);
        register("ravager", GravesAndGlory.ArmyRavager::new);
        register("wolf", GravesAndGlory.ArmyWolf::new);
        register("zombified_piglin", GravesAndGlory.ArmyBoarkin::new);
        register("spider", GravesAndGlory.ArmySpider::new);
        register("polar_bear", GravesAndGlory.ArmyBear::new);
    }

    /** Registers (or replaces) the factory of a sheet entity id. */
    public static void register(String entity, Function<World, MobEntity> factory) { FACTORIES.put(entity, factory); }

    /**
     * A new troop mob for a sheet entity id. An id nobody registered is a bug in the sheet or a House that is not built yet, so it throws.
     * The mob's own built-in armour is cleared: a troop's armour is its worn gear and nothing else (spec 1.3, "HP is the health column, before
     * worn armour"). Vanilla gives the whole zombie family a base armour of 2 (the husk, the drowned) and the skeletons none, so left alone a
     * mob type would decide how hard a House is to hurt, in no sheet and out of reach of a gate's tuning. A House that wants heavier troops
     * says so in the troops sheet, with gear.
     */
    public static MobEntity create(String entity, World w) {
        Function<World, MobEntity> factory = FACTORIES.get(entity);
        if (factory == null) throw new IllegalStateException("No army mob is registered for the troop entity '" + entity + "'");
        MobEntity mob = factory.apply(w);
        EntityAttributeInstance armour = mob.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armour != null) armour.setBaseValue(0);
        return mob;
    }

    /**
     * A mob for a portrait (the recruitment cards, the barracks): the vanilla mob of the sheet's entity id, never an Army* class, so a
     * card draws any troop of any House before its House can take the field. An id the game does not know, or that is not a mob, draws a husk.
     * The registry is a defaulted one (an unknown id would answer with a pig), so the id is looked up with containsId first.
     */
    public static MobEntity portrait(String entity, World w) {
        Identifier id = Identifier.tryParse(entity);
        if (id != null && Registries.ENTITY_TYPE.containsId(id) && Registries.ENTITY_TYPE.get(id).create(w) instanceof MobEntity mob) return mob;
        return new HuskEntity(EntityType.HUSK, w);
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
    /** The suffix a Mastery II troop's custom name carries (its veteran trim, in-match progression design 4.4); TroopModels.resolve reads it. */
    public static final String VETERAN = " (Veteran)";

    /** The troop whose name, or whose evolved stage name, is the given custom name (a trailing VETERAN ignored); null for any other name. A troop name wins over a stage name that matches it. */
    public static synchronized Spec.TroopsRow named(String name) {
        if (name != null && name.endsWith(VETERAN)) name = name.substring(0, name.length() - VETERAN.length());
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
