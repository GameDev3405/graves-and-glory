package gg.gravesandglory;

import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Keyframe;
import net.minecraft.client.render.entity.animation.Transformation;
import org.joml.Vector3f;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The troop rigs' keyframe clips (Phase 2 Task 5b): one Animation per clip per rig, on Minecraft's own animation system
 * (Animation, Keyframe and Transformation, applied by AnimationHelper.animate). Angles are degrees (an arm pitch of -90
 * points the arm forward, a positive body pitch leans forward, a positive head pitch looks down); offsets are model pixels
 * with y up, as Blockbench writes them. A loop clip ends on its first frame, a once clip ends at rest, and a hold clip
 * starts at rest and stops on its last frame. No clip scales a bone.
 *
 * The biped clips hold the weapon in the right hand, the shield in the left and a bow in the left (the brief's table);
 * TroopAnimator plays them mirrored for a troop the other way round (mirrored).
 */
public final class TroopClips {
    private TroopClips() {}
    public enum Kind {LOOP, ONCE, HOLD}
    /** The 18 clips, in the table's order. */
    public static final List<String> NAMES=List.of("idle","walk","run","attack","hit","death","shield_up","bow_draw","cast",
        "kneel","point","sit","raise","fall","stand","brace","hold","fall_back");
    public static final Map<String,Kind> KINDS;
    /** Each clip's length in seconds. */
    public static final Map<String,Float> LENGTHS;
    static{
        Map<String,Kind> k=new LinkedHashMap<>();Map<String,Float> l=new LinkedHashMap<>();
        Object[][] table={{"idle",Kind.LOOP,3f},{"walk",Kind.LOOP,1f},{"run",Kind.LOOP,.6f},{"attack",Kind.ONCE,.5f},{"hit",Kind.ONCE,.3f},
            {"death",Kind.HOLD,.9f},{"shield_up",Kind.HOLD,.2f},{"bow_draw",Kind.LOOP,1.2f},{"cast",Kind.LOOP,1f},{"kneel",Kind.HOLD,.6f},
            {"point",Kind.HOLD,.4f},{"sit",Kind.HOLD,.6f},{"raise",Kind.HOLD,.5f},{"fall",Kind.HOLD,.8f},{"stand",Kind.ONCE,.8f},
            {"brace",Kind.LOOP,2f},{"hold",Kind.LOOP,2f},{"fall_back",Kind.LOOP,1f}};
        for(Object[] row:table){k.put((String)row[0],(Kind)row[1]);l.put((String)row[0],(Float)row[2]);}
        KINDS=Collections.unmodifiableMap(k);LENGTHS=Collections.unmodifiableMap(l);
    }
    /** When a once clip peaks: the attack's contact, the hit's recoil, the stand half risen (still shots show it there). */
    public static final Map<String,Float> PEAKS=Map.of("attack",.25f,"hit",.08f,"stand",.4f);
    /**
     * Each rig's bow_draw landmarks in seconds: the full draw (the pose held between shots) and the loose (the frame a shot shows, the
     * moment of the arrow). The rest of the clip is the loose settling back to the nock; the draw hold ends at the draw. A hound or
     * a spider has no string: its draw is the crouch or the rear, its loose the spring.
     */
    public static final Map<String,float[]> BOW_TIMES=Map.of("biped",new float[]{.65f,.8f},"hound",new float[]{.9f,.9f},"spider",new float[]{.9f,.9f},"colossus",new float[]{.8f,.8f});

    /** A rig's clips by name: biped, hound, spider or colossus; empty for any other rig. */
    public static Map<String,Animation> of(String rig){
        return switch(rig){case "biped"->BipedClips.CLIPS;case "hound"->HoundClips.CLIPS;case "spider"->SpiderClips.CLIPS;case "colossus"->ColossusClips.CLIPS;default->Map.of();};
    }
    private static final Map<String,Map<String,Animation>> MIRRORED=new ConcurrentHashMap<>();
    /** A rig's clips mirrored left for right: bones named right and left swapped, yaw, roll and the x offset negated. */
    public static Map<String,Animation> mirrored(String rig){return MIRRORED.computeIfAbsent(rig,r->{Map<String,Animation> out=new LinkedHashMap<>();for(var e:of(r).entrySet())out.put(e.getKey(),mirror(e.getValue()));return Collections.unmodifiableMap(out);});}
    static Animation mirror(Animation a){
        Map<String,List<Transformation>> bones=new LinkedHashMap<>();
        for(var e:a.boneAnimations().entrySet()){
            List<Transformation> list=new ArrayList<>();
            for(Transformation t:e.getValue()){
                boolean rot=t.target()==Transformation.Targets.ROTATE;Keyframe[] f=new Keyframe[t.keyframes().length];
                for(int i=0;i<f.length;i++){Keyframe k=t.keyframes()[i];Vector3f v=k.target();f[i]=new Keyframe(k.timestamp(),rot?new Vector3f(v.x(),-v.y(),-v.z()):new Vector3f(-v.x(),v.y(),v.z()),k.interpolation());}
                list.add(new Transformation(t.target(),f));
            }
            bones.put(side(e.getKey()),List.copyOf(list));
        }
        return new Animation(a.lengthInSeconds(),a.looping(),Collections.unmodifiableMap(bones));
    }
    /** A bone's name on the other side: right and left swapped (front_right_leg is front_left_leg). */
    static String side(String bone){return bone.replace("right","~").replace("left","right").replace("~","left");}
    /** The time a still shot shows a clip at, in seconds: a loop at a quarter of its length (bow_draw at its full draw), a once clip at its peak, a hold clip at its end. */
    public static float shotTime(String clip){
        Kind k=KINDS.get(clip);float len=LENGTHS.get(clip);
        if(clip.equals("bow_draw"))return BOW_TIMES.get("biped")[0]; // the full draw: the pose a bowman holds
        return k==Kind.LOOP?len/4:k==Kind.ONCE?PEAKS.getOrDefault(clip,len/2):len;
    }

    /**
     * Writes one clip. rot and pos take keyframes in fours (the time in seconds, then pitch, yaw and roll in degrees, or x, y
     * up and z in pixels); wave and bob write a sine about a base, sampled every eighth of the clip, so a loop of them ends on
     * its first frame. Loops and holds ease (Catmull-Rom); once clips, and holds built linear (the falls), run linear.
     */
    static final class Clip {
        final String name;final float len;final Kind kind;final Transformation.Interpolation curve;
        private final Map<String,List<Transformation>> bones=new LinkedHashMap<>();
        Clip(String name){this(name,KINDS.get(name)==Kind.ONCE);}
        Clip(String name,boolean linear){
            this.name=name;kind=KINDS.get(name);Float l=LENGTHS.get(name);
            if(kind==null||l==null)throw new IllegalArgumentException("no clip "+name);
            len=l;curve=linear?Transformation.Interpolations.LINEAR:Transformation.Interpolations.CUBIC;
        }
        Clip rot(String bone,float... k){return add(bone,true,k);}
        Clip pos(String bone,float... k){return add(bone,false,k);}
        /** One rotation held all clip long. */
        Clip hold(String bone,float pitch,float yaw,float roll){return rot(bone,0,pitch,yaw,roll,len,pitch,yaw,roll);}
        /** A sine on one rotation axis (0 pitch, 1 yaw, 2 roll): base + amp sin(2 pi (cycles t / len + phase)). */
        Clip wave(String bone,int axis,float base,float amp,float phase,int cycles){return add(bone,true,sine(axis,base,amp,phase,cycles));}
        /** A sine on one offset axis (0 x, 1 y up, 2 z). */
        Clip bob(String bone,int axis,float base,float amp,float phase,int cycles){return add(bone,false,sine(axis,base,amp,phase,cycles));}
        private float[] sine(int axis,float base,float amp,float phase,int cycles){
            float[] k=new float[9*4];
            for(int i=0;i<=8;i++){double s=Math.sin(2*Math.PI*(cycles*i/8.0+phase));k[4*i]=len*i/8f;k[4*i+1+axis]=(float)(base+amp*s);}
            return k;
        }
        private Clip add(String bone,boolean rotation,float[] k){
            if(k.length==0||k.length%4!=0)throw new IllegalArgumentException(name+" "+bone+": keyframes come in fours");
            Keyframe[] f=new Keyframe[k.length/4];float last=-1;
            for(int i=0;i<f.length;i++){
                float t=k[4*i];if(t<last||t>len+1e-4f)throw new IllegalArgumentException(name+" "+bone+": keyframe time "+t+" out of order or past "+len);last=t;
                Vector3f v=rotation?AnimationHelper.createRotationalVector(k[4*i+1],k[4*i+2],k[4*i+3]):AnimationHelper.createTranslationalVector(k[4*i+1],k[4*i+2],k[4*i+3]);
                f[i]=new Keyframe(t,v,curve);
            }
            bones.computeIfAbsent(bone,b->new ArrayList<>()).add(new Transformation(rotation?Transformation.Targets.ROTATE:Transformation.Targets.TRANSLATE,f));
            return this;
        }
        Animation build(){
            Animation.Builder b=Animation.Builder.create(len);if(kind==Kind.LOOP)b.looping();
            for(var e:bones.entrySet())for(Transformation t:e.getValue())b.addBoneAnimation(e.getKey(),t);
            return b.build();
        }
    }
    /** Collects a rig's clips and refuses a set that misses one of the 18 or has one twice. */
    static final class Set18 {
        private final String rig;private final Map<String,Animation> clips=new HashMap<>();
        Set18(String rig){this.rig=rig;}
        Set18 put(Clip c){if(clips.put(c.name,c.build())!=null)throw new IllegalStateException(rig+" has two "+c.name+" clips");return this;}
        Map<String,Animation> done(){
            Map<String,Animation> out=new LinkedHashMap<>();
            for(String n:NAMES){Animation a=clips.get(n);if(a==null)throw new IllegalStateException(rig+" has no "+n+" clip");out.put(n,a);}
            return Collections.unmodifiableMap(out);
        }
    }
}
