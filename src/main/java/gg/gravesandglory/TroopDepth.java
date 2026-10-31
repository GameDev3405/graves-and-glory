package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import java.util.Arrays;

/**
 * Which troops draw their relief, and their weapon whole, this frame (troop depth design, section 4; art pipeline v2, section 3). Within NEAR_BLOCKS of the game camera the
 * NEAR_CAP nearest troops (and any troop at exactly the cap-th one's distance: a tie draws too) draw the relief layer of the skin they bind, every other troop draws flat (the same paint on the
 * same skin, so the switch shows only as the 1 px sides appearing, at no cost). A dressed mob (every ArmyCards portrait) is
 * always near. The cutoff is worked out once per frame on WorldRenderEvents.START from the game camera, so Iris's shadow
 * pass and the main pass draw the same geometry. The same set draws a cube weapon whole (the fourth cut): a weapon on a troop
 * outside it draws its far version of at most 32 quads.
 */
public final class TroopDepth {
    private TroopDepth() {}
    /** Troops within this many blocks of the camera may draw relief. */
    public static final double NEAR_BLOCKS=24;
    /** This many of the nearest draw relief in one frame (a tie at the last one's distance draws too, so the count is this or, for exact ties, a few more). */
    public static final int NEAR_CAP=48;
    private static final double NEAR_SQ=NEAR_BLOCKS*NEAR_BLOCKS;
    /** Off: no relief anywhere, sprite weapons draw only their two big faces and cube weapons their far version (the harness's flat shots and its cost baseline). The player's option is Presentation.troopDepth. */
    public static volatile boolean enabled=true;
    /** The last frame's counts, each troop once however many passes drew it: troops that drew relief, relief quads and weapon quads drawn (the weapon's mesh once; its glow pass draws it again and is counted by TroopFeatures.weaponGlowDraws). */
    public static volatile int nearDraws;
    public static volatile long reliefQuads,weaponQuads;
    /** The counts of the frame being drawn, published by the next beginFrame. */
    private static int drawsNow;
    private static long reliefNow,weaponNow;
    /** The frame being drawn (starts at 1, so a Drawn that never counted is behind it). */
    private static int frame=1;
    private static volatile Vec3d camera=Vec3d.ZERO;
    private static volatile double cutoff=NEAR_SQ;
    /** The cutoff for bodies lying past their fall (Task 7): they rank after every living troop, so -1 when the living fill the cap. */
    private static volatile double bodyCutoff=NEAR_SQ;
    /** The squared distances beginFrame gathers each frame, living troops and lying bodies apart; the render thread alone touches them, and the cutoffs sort them where they lie. */
    private static double[] inRange=new double[256],bodiesInRange=new double[64];

    /**
     * Called on WorldRenderEvents.START: publishes the last frame's counts and works out this frame's cutoff from the entities'
     * squared distances to the game camera. One pass over the world's entities; only a named mob inside NEAR_BLOCKS that
     * resolves to a troop with a model is looked at twice.
     */
    public static void beginFrame(Vec3d cam,Iterable<Entity> entities){
        nearDraws=drawsNow;reliefQuads=reliefNow;weaponQuads=weaponNow;drawsNow=0;reliefNow=weaponNow=0;frame++;
        camera=cam;int n=0,nb=0;
        if(enabled)for(Entity e:entities){
            if(!(e instanceof MobEntity))continue;
            if(BodyCache.recent(e)){ // a body BodyCache drew a few frames ago: a lying troop with a model
                double d=distSq(e,cam);if(d>=NEAR_SQ)continue;
                if(nb==bodiesInRange.length)bodiesInRange=Arrays.copyOf(bodiesInRange,nb*2);
                bodiesInRange[nb++]=d;continue;
            }
            if(e.getCustomName()==null)continue;
            double d=distSq(e,cam);if(d>=NEAR_SQ||TroopModels.resolve(e)==null)continue;
            if(lying(e)){
                if(nb==bodiesInRange.length)bodiesInRange=Arrays.copyOf(bodiesInRange,nb*2);
                bodiesInRange[nb++]=d;continue;
            }
            if(n==inRange.length)inRange=Arrays.copyOf(inRange,n*2);
            inRange[n++]=d;
        }
        // the body cutoff counts the living in range before cutoffInPlace reorders them (it only reads them)
        bodyCutoff=BattleMath.bodyCutoffSq(inRange,n,bodiesInRange,nb,NEAR_CAP,NEAR_SQ);
        cutoff=cutoffInPlace(inRange,n,NEAR_CAP,NEAR_SQ);
    }

    /**
     * The squared distance a troop must not pass to draw relief: of the first n values of distSq, those strictly below nearSq
     * are the troops in range; with fewer than cap of them everyone in range is near and the answer is nearSq, else it is the
     * cap-th smallest of them (the cap nearest are near). Pure: it works on a copy, so any thread may call it.
     */
    public static double cutoffSq(double[] distSq,int n,int cap,double nearSq){
        return cutoffInPlace(Arrays.copyOf(distSq,n),n,cap,nearSq);
    }
    /** cutoffSq on an array it may reorder: the in-range values are gathered at its front and sorted there. */
    private static double cutoffInPlace(double[] a,int n,int cap,double nearSq){
        return BattleMath.reliefCutoffSq(a,n,cap,nearSq);
    }
    /** A troop's body lying past its fall (Task 7): it ranks after every living troop for relief and whole weapons. */
    private static boolean lying(Entity e){
        return e instanceof LivingEntity l&&l.isDead()&&l.deathTime>BattleMath.DEATH_FALL_TICKS;
    }

    /** Whether relief is drawn at all: the harness flag and the player's "Troop depth" option (Presentation.troopDepth) are both on. The option gates only the relief; weapons keep their sprite meshes. */
    public static boolean reliefOn(){return enabled&&Presentation.troopDepth;}

    /** Whether this frame's relief is drawn for the entity: on, and a dressed mob, or within NEAR_BLOCKS and among the NEAR_CAP nearest (ties at the cap-th distance included). */
    public static boolean near(Entity e){
        return reliefOn()&&inRange(e);
    }
    /**
     * Whether a cube weapon on this entity draws whole this frame (the fourth cut): troop depth is on (the harness flag; the player's
     * option gates only the relief) and the entity is a dressed mob, or within NEAR_BLOCKS and among the NEAR_CAP nearest. Any other
     * troop draws its weapon's far version.
     */
    public static boolean weaponNear(Entity e){
        return enabled&&inRange(e);
    }
    /** Whether a world troop lies at least NEAR_BLOCKS from the game camera, where a density above 1 troop binds its one-texel-a-px skin twins (TroopModels.far). A dressed mob (a portrait) is never beyond: its cards choose by their own scale. */
    public static boolean beyond(Entity e){
        return !TroopModels.dressed(e)&&distSq(e,camera)>=NEAR_SQ;
    }
    private static boolean inRange(Entity e){
        if(TroopModels.dressed(e))return true;
        double d=distSq(e,camera);return d<NEAR_SQ&&d<=(lying(e)?bodyCutoff:cutoff);
    }
    private static double distSq(Entity e,Vec3d cam){double dx=e.getX()-cam.x,dy=e.getY()-cam.y,dz=e.getZ()-cam.z;return dx*dx+dy*dy+dz*dz;}

    /** The frame being drawn: TroopRenderer.record counts a troop once a frame, whatever passes (Iris's shadow pass among them) drew it. */
    static int frame(){return frame;}
    /** Counts one troop's relief quads and weapon quads for this frame. */
    static void count(int relief,int weapon){
        if(relief>0){drawsNow++;reliefNow+=relief;}
        weaponNow+=weapon;
    }
}
