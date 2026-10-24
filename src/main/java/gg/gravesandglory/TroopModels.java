package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import java.util.*;

/**
 * The troops' own models (Phase 2 Task 5; unit identity section 0), as TroopModelLoader last loaded them from
 * models/troop/, and how a mob on the client maps to one. A troop is a plain vanilla mob named by Battle.spawn: its custom
 * name gives the troop and its level (ArmyMobs.named), a name ending " (Veteran)" the Veteran skin, and its gg_<side>
 * scoreboard team the cloth its panels and pennant are drawn in.
 */
public final class TroopModels {
    private TroopModels() {}
    /**
     * What a troop entity looks like: its row, armour tier, Veteran skin, and its side: the side of its gg_ team, else
     * the side this client last resolved for it (the server takes a troop off its team as it dies, and the body stays a second
     * longer in its colours, with its flag), else null.
     */
    public record Look(Spec.TroopsRow troop, int armourTier, boolean veteran, String side) {}
    /**
     * A weapon model (troop depth design, section 2; art pipeline v2, section 2.1): its id, the meshes of its frames (rest first,
     * then pull_0 to pull_2 of a draw weapon), the far version (at most 32 quads, built from the rest frame; null for a sprite weapon,
     * whose frames are already thin), the times of the pulls in seconds of bow_draw (empty without a pull), the grip that places the
     * model in the hand bone's frame (the model's origin is the grip), the nock of each frame (the middle of the string in
     * the weapon frame, model px; empty without a pull), its texture and its glow (null for none). rot is the grip's turn as a quaternion,
     * worked out once (null for no turn); build a Weapon without it and the constructor below fills it in.
     */
    public record Weapon(String id, java.util.List<TroopMesh> frames, TroopMesh far, float[] pull, net.minecraft.client.model.ModelTransform grip, java.util.List<Vec3d> nocks, Identifier texture, Identifier glow, org.joml.Quaternionf rot) {
        public Weapon(String id, java.util.List<TroopMesh> frames, TroopMesh far, float[] pull, net.minecraft.client.model.ModelTransform grip, java.util.List<Vec3d> nocks, Identifier texture, Identifier glow) {
            this(id, frames, far, pull, grip, nocks, texture, glow, turn(grip));
        }
        /** Whether this is a cube weapon (a Blockbench model, with a far version), which draws whole at every depth setting; a sprite weapon draws its two big faces with troop depth off. */
        public boolean cube() { return far != null; }
        private static org.joml.Quaternionf turn(net.minecraft.client.model.ModelTransform g) {
            return g.pitch != 0f || g.yaw != 0f || g.roll != 0f ? new org.joml.Quaternionf().rotationZYX(g.roll, g.yaw, g.pitch) : null;
        }
        /**
         * The frame a draw shows: the rest frame (0) when the weapon has no pull, below pull[0], from the loose on, and when nothing
         * draws (seconds -1); otherwise the last pull whose start has passed, counted from frame 1.
         */
        public static int frameAt(float[] pull, float seconds, float loose) {
            if (pull == null || pull.length == 0 || seconds < pull[0] || seconds >= loose) return 0;
            int k = 0;
            for (int i = 0; i < pull.length; i++) if (seconds >= pull[i]) k = i;
            return k + 1;
        }
        /** Moves the stack from the hand bone's frame to the sprite's: the grip's offset, then its turn, as ModelPart.rotate does. */
        public void grip(net.minecraft.client.util.math.MatrixStack m) {
            m.translate(grip.pivotX / 16f, grip.pivotY / 16f, grip.pivotZ / 16f);
            if (rot != null) m.multiply(rot);
        }
        /** The corners of every frame's bounds in the hand bone's frame (the grip applied), model px: the cull box takes the weapon's reach from them. */
        public java.util.List<double[]> points() {
            double[][] rot = TroopModel.rotation(new double[]{Math.toDegrees(grip.pitch), Math.toDegrees(grip.yaw), Math.toDegrees(grip.roll)});
            java.util.List<double[]> out = new java.util.ArrayList<>();
            for (TroopMesh f : frames) {
                if (f.quads() == 0) continue;
                float[] b = f.bounds();
                for (int c = 0; c < 8; c++) {
                    double[] corner = {(c & 1) == 0 ? b[0] : b[3], (c & 2) == 0 ? b[1] : b[4], (c & 4) == 0 ? b[2] : b[5]};
                    double[] q = TroopModel.apply(rot, corner);
                    out.add(new double[]{q[0] + grip.pivotX, q[1] + grip.pivotY, q[2] + grip.pivotZ});
                }
            }
            return out;
        }
    }
    /** The rig of each sheet entity (tools/build_skins.py RIGS). */
    public static final Map<String,String> RIGS = Map.ofEntries(
        Map.entry("husk","biped"),Map.entry("zombie","biped"),Map.entry("skeleton","biped"),Map.entry("stray","biped"),
        Map.entry("wither_skeleton","biped"),Map.entry("drowned","biped"),Map.entry("zombified_piglin","biped"),
        Map.entry("vindicator","biped"),Map.entry("pillager","biped"),Map.entry("evoker","biped"),
        Map.entry("wolf","hound"),Map.entry("polar_bear","hound"),Map.entry("ravager","hound"),
        Map.entry("spider","spider"),Map.entry("iron_golem","colossus"));
    static final String VETERAN=ArmyMobs.VETERAN,TEAM="gg_",NAMESPACE="gravesandglory",CLOTH="textures/entity/troop/team_cloth_";
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
     * The Look of a troop entity: null for a mob that is no troop (no custom name, or a name no troop or level has) and for a
     * troop whose House has no model yet, which therefore draws through its vanilla renderer. A troop without a gg_ team keeps
     * the side this client last resolved for it, for as long as the entity exists.
     */
    public static Look resolve(Entity e){
        if(!(e instanceof MobEntity)||e.getCustomName()==null)return null;
        String name=e.getCustomName().getString();boolean veteran=name.endsWith(VETERAN);
        if(veteran)name=name.substring(0,name.length()-VETERAN.length());
        Spec.TroopsRow t=ArmyMobs.named(name);if(t==null||!troops.containsKey(t.id()))return null;
        int armourTier=1; // stopgap until Task 9 reads RankSync: a level name stands for its level, and the level picks the armour tier
        if(!name.equals(t.name()))for(var row:Spec.LEVELNAMES)if(row.troop().equals(t.evolveLine())&&row.name().equals(name)){armourTier=row.level();break;}
        var team=e.getScoreboardTeam();String side=null;
        if(team!=null&&team.getName().startsWith(TEAM))side=team.getName().substring(TEAM.length());
        Seen seen=SEEN.computeIfAbsent(e,k->new Seen(k.getWorld()==null?0:k.getWorld().getTime()));
        if(side!=null)seen.side=side;else side=seen.side;
        return new Look(t,armourTier,veteran,side);
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

    /** The skin of a Look: its armour tier's skin (a tier without its own skin draws the next lower tier's). A Veteran wears the same tier, with trim over it. */
    public static Identifier skin(Look l){return skin(l,false);}
    /** The skin of a Look, or with far its one-texel-a-px twin when the troop's density is above 1 (see far, twin). */
    public static Identifier skin(Look l,boolean far){var m=troops.get(l.troop().id());return m==null?null:pick(m,m.skin(l.armourTier()),far);}
    /** The veteran trim (<troop>_veteran, in-match progression design 4.4) a Look draws over its skin: null unless it is a Veteran and the troop has a trim. */
    public static Identifier trim(Look l){return trim(l,false);}
    public static Identifier trim(Look l,boolean far){var m=troops.get(l.troop().id());return m==null||!l.veteran()?null:pick(m,m.veteran,far);}
    /** The glow layer (<troop>_e) of a Look, null below the troop's glowFrom stage or for a troop without one. */
    public static Identifier glow(Look l){return glow(l,false);}
    public static Identifier glow(Look l,boolean far){var m=troops.get(l.troop().id());return m==null||m.glow==null||l.armourTier()<m.glowFrom?null:pick(m,m.glow,far);}

    /** The suffix of a density D troop PNG's one-texel-a-px twin (tools/build_skins.py FAR_SUFFIX): knight_s3.png has knight_s3_far.png. */
    static final String FAR_SUFFIX="_far";
    /** A card portrait is drawn from the twins when it spans fewer than this many screen px a block for each texel of density a px: 16 x density, so 48 for a density 3 troop (at 48 a texel is one screen px; under it a nearest-sampled 3x skin crawls and speckles). */
    static final int FAR_CARD_PER_DENSITY=16;
    /** The twin of a texture id: the same path with _far before the .png (a path that is no png is its own twin). */
    static Identifier twin(Identifier id){
        String p=id.getPath();return p.endsWith(".png")?new Identifier(id.getNamespace(),p.substring(0,p.length()-4)+FAR_SUFFIX+".png"):id;
    }
    /** The texture a draw binds: the id, or for a density above 1 troop drawing far, its twin. */
    private static Identifier pick(TroopModel<?> m,Identifier id,boolean far){return far&&m.density>1&&id!=null?twin(id):id;}
    /** The screen px a block spans where a dressed mob was last drawn (ArmyCards.draw records it). Weak keys by identity. */
    private static final Map<Entity,Integer> SIZED=new MapMaker().weakKeys().makeMap();
    /** Records the scale (screen px a block) a portrait mob is about to be drawn at. */
    static void sized(Entity e,int pxPerBlock){SIZED.put(e,pxPerBlock);}
    /**
     * Whether the draw of this troop binds the one-texel-a-px twins of its PNGs (texel-3 plan Q2a): the troop's density is above 1 and it
     * is a world troop at least TroopDepth.NEAR_BLOCKS from the camera, or a card portrait last drawn at fewer than FAR_CARD_PER_DENSITY
     * x density px a block. A troop of density 1 never does.
     */
    static boolean far(Entity e,Look l){
        var m=troops.get(l.troop().id());if(m==null||m.density<=1)return false;
        if(dressed(e)){Integer px=SIZED.get(e);return px!=null&&px<FAR_CARD_PER_DENSITY*m.density;}
        return TroopDepth.beyond(e);
    }
    /** The cloth sheet of a side (a factions id), of the company ("company"), or linen for null. */
    public static Identifier cloth(String side){return new Identifier(NAMESPACE,CLOTH+(side==null?"linen":side)+".png");}
    public static Weapon weapon(String id){return id==null?null:weapons.get(id);}
    /** N for a troop framed "tall N", else 0 (the UI plan's Portrait.draw tall argument). */
    public static int portraitTall(String troop){String f=framing(troop);return f!=null&&f.startsWith("tall ")?Integer.parseInt(f.substring(5).trim()):0;}
    /** The raw portrait framing of a troop: standard, tall N, wide N or beast N [yaw D]; null without a model. */
    public static String framing(String troop){var m=troops.get(troop);return m==null?null:m.portrait;}

    /** Whether the mob wears a dress (an ArmyCards portrait). */
    static boolean dressed(Entity e){return DRESS.containsKey(e);}
    /** Puts a team-less mob in a cloth other than linen (ArmyCards: the company's, "company"); null takes it off again. */
    static void dress(Entity e,String cloth){if(cloth==null)DRESS.remove(e);else DRESS.put(e,cloth);}
    /** The cloth key a Look draws its team parts in: its side, else the mob's dress, else null (linen). */
    static String clothOf(Entity e,Look l){return l.side()!=null?l.side():DRESS.get(e);}
}
