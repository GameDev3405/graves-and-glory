package gg.gravesandglory;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Vec3d;
import java.util.Arrays;

/**
 * Which troops draw their relief this frame (troop depth design, section 4). Within NEAR_BLOCKS of the game camera the
 * NEAR_CAP nearest troops (and any troop at exactly the cap-th one's distance: a tie draws too) draw the relief layer of the skin they bind, every other troop draws flat (the same paint on the
 * same skin, so the switch shows only as the 1 px sides appearing, at no cost). A dressed mob (every ArmyCards portrait) is
 * always near. The cutoff is worked out once per frame on WorldRenderEvents.START from the game camera, so Iris's shadow
 * pass and the main pass draw the same geometry.
 */
public final class TroopDepth {
    private TroopDepth() {}
    /** Troops within this many blocks of the camera may draw relief. */
    public static final double NEAR_BLOCKS=24;
    /** This many of the nearest draw relief in one frame (a tie at the last one's distance draws too, so the count is this or, for exact ties, a few more). */
    public static final int NEAR_CAP=48;
    private static final double NEAR_SQ=NEAR_BLOCKS*NEAR_BLOCKS;
    /** Off: no relief anywhere and weapons draw only their two big faces (the harness's flat shots and its cost baseline). The player's option is Presentation.troopDepth. */
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
    /** The squared distances beginFrame gathers each frame; the render thread alone touches it, and cutoffInPlace sorts it where it lies. */
    private static double[] inRange=new double[256];

    /**
     * Called on WorldRenderEvents.START: publishes the last frame's counts and works out this frame's cutoff from the entities'
     * squared distances to the game camera. One pass over the world's entities; only a named mob inside NEAR_BLOCKS that
     * resolves to a troop with a model is looked at twice.
     */
    public static void beginFrame(Vec3d cam,Iterable<Entity> entities){
        nearDraws=drawsNow;reliefQuads=reliefNow;weaponQuads=weaponNow;drawsNow=0;reliefNow=weaponNow=0;frame++;
        camera=cam;int n=0;
        if(reliefOn())for(Entity e:entities){
            if(!(e instanceof MobEntity)||e.getCustomName()==null)continue;
            double d=distSq(e,cam);if(d>=NEAR_SQ||TroopModels.resolve(e)==null)continue;
            if(n==inRange.length)inRange=Arrays.copyOf(inRange,n*2);
            inRange[n++]=d;
        }
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
        int k=0;
        for(int i=0;i<n;i++)if(a[i]<nearSq)a[k++]=a[i];
        if(k<cap||cap<1)return nearSq;
        Arrays.sort(a,0,k);
        return a[cap-1];
    }

    /** Whether relief is drawn at all: the harness flag and the player's "Troop depth" option (Presentation.troopDepth) are both on. The option gates only the relief; weapons keep their sprite meshes. */
    public static boolean reliefOn(){return enabled&&Presentation.troopDepth;}

    /** Whether this frame's relief is drawn for the entity: on, and a dressed mob, or within NEAR_BLOCKS and among the NEAR_CAP nearest (ties at the cap-th distance included). */
    public static boolean near(Entity e){
        if(!reliefOn())return false;
        if(TroopModels.dressed(e))return true;
        double d=distSq(e,camera);return d<NEAR_SQ&&d<=cutoff;
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
