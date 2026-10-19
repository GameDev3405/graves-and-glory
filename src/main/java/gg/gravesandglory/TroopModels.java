package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import java.util.*;

/**
 * The troops' own models (Phase 2 Task 5; unit identity section 0), as TroopModelLoader last loaded them from
 * models/troop/, and how a mob on the client maps to one. A troop is a plain vanilla mob named by Battle.spawn: its custom
 * name gives the troop and the stage (ArmyMobs.named), a name ending " (Veteran)" the Veteran skin, and its gg_<side>
 * scoreboard team the cloth its panels and pennant are drawn in.
 */
public final class TroopModels {
    private TroopModels() {}
    /**
     * What a troop entity looks like: its row, evolution stage, Veteran skin, and its side: the side of its gg_ team, else
     * the side this client last resolved for it (the server takes a troop off its team as it dies, and the body stays a second
     * longer in its colours, with its flag), else null.
     */
    public record Look(Spec.TroopsRow troop, int stage, boolean veteran, String side) {}
    /** A weapon model: its part (the grip already placed in the hand bone's frame), its texture and its glow (null for none). */
    public record Weapon(ModelPart part, Identifier texture, Identifier glow) {}
    /** The rig of each sheet entity (tools/build_skins.py RIGS). */
    public static final Map<String,String> RIGS = Map.ofEntries(
        Map.entry("husk","biped"),Map.entry("zombie","biped"),Map.entry("skeleton","biped"),Map.entry("stray","biped"),
        Map.entry("wither_skeleton","biped"),Map.entry("drowned","biped"),Map.entry("zombified_piglin","biped"),
        Map.entry("vindicator","biped"),Map.entry("pillager","biped"),Map.entry("evoker","biped"),
        Map.entry("wolf","hound"),Map.entry("polar_bear","hound"),Map.entry("ravager","hound"),
        Map.entry("spider","spider"),Map.entry("iron_golem","colossus"));
    static final String VETERAN=" (Veteran)",TEAM="gg_",NAMESPACE="gravesandglory",CLOTH="textures/entity/troop/team_cloth_";
    /** Ticks a troop in the world waits undrawn for its side's team (the spawn packet can reach a client a frame before the team packet); after that it draws in linen. */
    static final int HOLD_TICKS=40;
    private static volatile Map<String,TroopModel<?>> troops=Map.of(),rigs=Map.of();
    private static volatile Map<String,Weapon> weapons=Map.of();
    /** The cloth a team-less mob wears instead of linen (ArmyCards puts its portraits in the company's cloth). Weak keys by identity: a client-built portrait mob can share a network id with a world mob. */
    private static final Map<Entity,String> DRESS=new MapMaker().weakKeys().makeMap();
    /** What this client last saw of a troop entity: the side it last resolved and when it first resolved it (world time). Weak keys by identity. */
    private static final Map<Entity,Seen> SEEN=new MapMaker().weakKeys().makeMap();
    private static final class Seen { volatile String side;final long first;Seen(long first){this.first=first;} }

    /** Called by TroopModelLoader at the end of each resource reload. */
    static void publish(Map<String,TroopModel<?>> troopModels,Map<String,TroopModel<?>> rigModels,Map<String,Weapon> weaponModels){
        troops=Collections.unmodifiableMap(new LinkedHashMap<>(troopModels));rigs=Collections.unmodifiableMap(new LinkedHashMap<>(rigModels));
        weapons=Collections.unmodifiableMap(new LinkedHashMap<>(weaponModels));
    }

    /**
     * The Look of a troop entity: null for a mob that is no troop (no custom name, or a name no troop or stage has) and for a
     * troop whose House has no model yet, which therefore draws through its vanilla renderer. A troop without a gg_ team keeps
     * the side this client last resolved for it, for as long as the entity exists.
     */
    public static Look resolve(Entity e){
        if(!(e instanceof MobEntity)||e.getCustomName()==null)return null;
        String name=e.getCustomName().getString();boolean veteran=name.endsWith(VETERAN);
        if(veteran)name=name.substring(0,name.length()-VETERAN.length());
        Spec.TroopsRow t=ArmyMobs.named(name);if(t==null||!troops.containsKey(t.id()))return null;
        int stage=1;
        if(!name.equals(t.name()))for(var row:Spec.EVOLUTIONS)if(row.troop().equals(t.evolveLine())&&row.name().equals(name)){stage=row.stage();break;}
        var team=e.getScoreboardTeam();String side=null;
        if(team!=null&&team.getName().startsWith(TEAM))side=team.getName().substring(TEAM.length());
        Seen seen=SEEN.computeIfAbsent(e,k->new Seen(k.getWorld()==null?0:k.getWorld().getTime()));
        if(side!=null)seen.side=side;else side=seen.side;
        return new Look(t,stage,veteran,side);
    }
    /**
     * Whether a troop waits undrawn this frame: it has no side yet (its team has not reached this client), wears no dress (a
     * portrait), and was first resolved less than HOLD_TICKS ago. The first frame that draws it then shows its side's colours.
     */
    static boolean held(Entity e,Look l){
        if(l.side()!=null||DRESS.containsKey(e)||e.getWorld()==null)return false;
        Seen seen=SEEN.get(e);return seen!=null&&e.getWorld().getTime()-seen.first<HOLD_TICKS;
    }
    public static TroopModel<?> model(String troop){return troops.get(troop);}
    @SuppressWarnings("unchecked") static <T extends MobEntity> TroopModel<T> modelOf(String troop){return (TroopModel<T>)troops.get(troop);}
    /** The bare rig (every bone, no parts), for Task 5b's clips. */
    public static TroopModel<?> rig(String rig){return rigs.get(rig);}
    /** The troops with a model. */
    public static Set<String> ids(){return troops.keySet();}

    /** The skin of a Look: the Veteran skin when it is a Veteran and the troop has one, else its stage's (a stage without its own skin draws the next lower stage's). */
    public static Identifier skin(Look l){var m=troops.get(l.troop().id());if(m==null)return null;return l.veteran()&&m.veteran!=null?m.veteran:m.skin(l.stage());}
    /** The glow layer (<troop>_e) of a Look, null below the troop's glowFrom stage or for a troop without one. */
    public static Identifier glow(Look l){var m=troops.get(l.troop().id());return m==null||m.glow==null||l.stage()<m.glowFrom?null:m.glow;}
    /** The cloth sheet of a side (a factions id), of the company ("company"), or linen for null. */
    public static Identifier cloth(String side){return new Identifier(NAMESPACE,CLOTH+(side==null?"linen":side)+".png");}
    public static Weapon weapon(String id){return id==null?null:weapons.get(id);}
    /** N for a troop framed "tall N", else 0 (the UI plan's Portrait.draw tall argument). */
    public static int portraitTall(String troop){String f=framing(troop);return f!=null&&f.startsWith("tall ")?Integer.parseInt(f.substring(5).trim()):0;}
    /** The raw portrait framing of a troop: standard, tall N, wide N or beast N [yaw D]; null without a model. */
    public static String framing(String troop){var m=troops.get(troop);return m==null?null:m.portrait;}

    /** Puts a team-less mob in a cloth other than linen (ArmyCards: the company's, "company"); null takes it off again. */
    static void dress(Entity e,String cloth){if(cloth==null)DRESS.remove(e);else DRESS.put(e,cloth);}
    /** The cloth key a Look draws its team parts in: its side, else the mob's dress, else null (linen). */
    static String clothOf(Entity e,Look l){return l.side()!=null?l.side():DRESS.get(e);}
}
