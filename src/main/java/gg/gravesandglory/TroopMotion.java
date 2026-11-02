package gg.gravesandglory;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The client adapter between a troop entity and {@link MotionEngine} (animation rework Task 13). Each frame, after
 * TroopModel.setAngles reset every bone, pose:
 * 1. builds the troop's MotionInput: render time on the world clock, its limbs, head and body turn, the stance the server sent
 *    (stance_v2, none while it dies: carry B12), its newest swing and blow (MotionEvents.Client), whether it is dead, and its seed;
 * 2. runs the engine on the troop's Family (its rig's clips/<rig>/<family>/ folder over clips/<rig>/common/) and adds the pose to
 *    the bones (degrees and pixels);
 * 3. clamps every bone with a joint limit (carry C13: the mage's shins);
 * 4. adds the carries on what no clip holds: the shield arm 45 degrees forward, a bow turned in its hand (string back at rest,
 *    to the archer while it draws or aims) and idleArms forward;
 * 5. scales a taller troop's root offsets to its height (TroopModel.rootScale).
 * A body lying past its fall whose engine state has gone still keeps its pose (lying), which BodyCache records once.
 * State is kept per entity id and dropped on unload (forget) and disconnect (clear). A forced clip (force, the harness) or a
 * scene pose (setScenePose) plays one clip whole-body over the engine.
 */
public final class TroopMotion {
    private TroopMotion() {}
    static final float RAD=MathHelper.RADIANS_PER_DEGREE;
    /**
     * The turn of a bow in its hand at rest, degrees of yaw (troop depth design section 2: 45, so a flat sprite's curve reads at 71
     * percent from the front and from the side; the hand turns the bow upright into the plane of the shot as it draws).
     */
    public static final float BOW_REST_YAW=45;
    /** The bow's pull timeline Weapon.frameAt reads (seconds): the full draw, and the loose, at and after which the bow is at rest. */
    public static final float BOW_FULL=.65f,BOW_LOOSE=.8f;
    /** The margin a cull reach keeps round a pose's extent. */
    static final double CULL_MARGIN=.25;
    /** What the procedural layers may move an arm's pivot (pixels): the shrug and the novelty settle, at most 1.66 together. */
    static final double LIMB_PX=3;
    /** The clip folder every family of a rig falls back to. */
    static final String COMMON="common";
    static final List<String> ARMS=List.of("right_arm","left_arm","right_forearm","left_forearm");
    static final List<String> STANCES=List.of("idle","carry","guard","brace","wall","shield","aim","rest");
    static final Set<String> DIRECTIONS=Set.of("front","back","left","right");
    private static final Pattern HIT=Pattern.compile("hit_(head|body|legs)(_\\d+)?"),BLOCK=Pattern.compile("block(?:_(head|body|legs))?(_\\d+)?");
    /** Seconds a scene pose takes to blend in or out. */
    static final double SCENE_BLEND=.25;

    /** Harness hook: one clip frozen at one time (seconds) on an entity, whole-body, with the carries on what it does not key. */
    public record Forced(String clip,double seconds){}
    private static final Map<Integer,Forced> FORCED=new ConcurrentHashMap<>();
    /** The scene pose of each entity id (setScenePose). */
    private static final Map<Integer,String> SCENE=new ConcurrentHashMap<>();
    private static final Map<Integer,Body> BODIES=new ConcurrentHashMap<>();
    private static final Map<String,Optional<Spec.TroopsRow>> ROWS=new ConcurrentHashMap<>();
    private static final Map<String,Fam> FAMILIES=new ConcurrentHashMap<>();

    /** One entity's engine state and what the adapter keeps beside it. */
    static final class Body {
        final UUID uuid;final MotionMath.Seed seed;final MotionEngine.State state=new MotionEngine.State();final Pose pose=new Pose(),scene=new Pose();
        /** Whether a clip held an arm in the last frame (folded arms open). */
        boolean armHeld;
        /** Scratch for the tracks' shares of the fold, worked out once a frame (shares). */
        private double[] shares=new double[MotionEngine.MAX_TRACKS+1];
        /** The pose of a body lying still past its fall (a TroopModel snapshot), the model it was taken on and its cull reach; null until then. */
        float[] lying;TroopModel<?> lyingModel;double[] lyingReach;
        /** The scene pose's clip, when it began (render seconds) and its blend (0 to 1). */
        String sceneClip;double sceneStart,sceneW,sceneLast=Double.NaN;
        Body(UUID uuid){this.uuid=uuid;seed=MotionMath.seed(uuid.getMostSignificantBits(),uuid.getLeastSignificantBits());}
        /** The tracks' shares of the fold this frame (MotionStack.shares) in the body's scratch, one per track. */
        double[] shares(){
            List<MotionStack.Track> t=state.tracks;if(shares.length<t.size())shares=new double[t.size()];
            MotionStack.shares(t,t.size(),shares);return shares;
        }
    }

    /** A rig and family's clips as the engine takes them, and for each server attack variant index the engine's index (-1: no clip). */
    /**
     * A rig and family's clips as the engine takes them, for each server attack variant index the engine's index (-1: no clip), the
     * each bone's own translation reach in pixels (bonePx: every bone a clip keys, the root and the arms).
     */
    record Fam(MotionEngine.Family family,int[] attackIndex,int generation,Map<String,Clip> all,Map<String,double[]> bonePx){}

    // ---- families ----
    /** The Family of a troop's model, or null for a bare rig. Rebuilt when the clips reload. */
    static Fam family(TroopModel<?> m){
        Spec.TroopsRow row=row(m);if(row==null)return null;
        String key=m.rig+"/"+row.family();int gen=ClipLibrary.generation();
        Fam f=FAMILIES.get(key);
        if(f==null||f.generation()!=gen){f=build(m.rig,row.family(),gen);FAMILIES.put(key,f);}
        return f;
    }
    static Fam build(String rig,String family,int gen){
        Map<String,Clip> all=new TreeMap<>(ClipLibrary.folder(rig,COMMON));all.putAll(ClipLibrary.folder(rig,family));
        Map<String,Clip> stances=new HashMap<>();for(String s:STANCES)if(all.containsKey(s))stances.put(s,all.get(s));
        Map<String,Clip> own=ClipLibrary.folder(rig,family);
        List<Spec.AttackVariantsRow> rows=Tactics.variants(family);int[] index=new int[rows.size()];List<Clip> attacks=new ArrayList<>();
        for(int i=0;i<rows.size();i++){Clip c=own.get(rows.get(i).id());if(c==null)index[i]=-1;else{index[i]=attacks.size();attacks.add(c);}}
        // <id>_wall: the attack thrown from the wall or brace hold (Task 16), parallel to attacks; a row without one keeps its plain clip
        List<Clip> wall=new ArrayList<>();boolean anyWall=false;
        for(int i=0;i<rows.size();i++)if(index[i]>=0){Clip w=own.get(rows.get(i).id()+"_wall");anyWall|=w!=null;wall.add(w!=null?w:attacks.get(index[i]));}
        if(!anyWall)wall.clear();
        Map<String,List<Clip>> hits=new HashMap<>();Map<String,List<Clip>> deaths=new HashMap<>();
        for(var e:all.entrySet()){
            String n=e.getKey();Matcher h=HIT.matcher(n);
            if(h.matches())hits.computeIfAbsent(h.group(1),k->new ArrayList<>()).add(e.getValue());
            else if((h=BLOCK.matcher(n)).matches())hits.computeIfAbsent(h.group(1)==null||h.group(1).equals("body")?"block":"block_"+h.group(1),k->new ArrayList<>()).add(e.getValue());
            else{String d=deathKey(n);if(d!=null)deaths.computeIfAbsent(d,k->new ArrayList<>()).add(e.getValue());}
        }
        MotionEngine.Family fam=new MotionEngine.Family(family,stances,attacks,wall,hits,deaths);
        // each bone's own: the engine's per-bone reach, and any clip of the folders at the hit gain over the procedural bound (a scene or forced clip)
        Set<String> bones=new TreeSet<>(List.of("root","right_arm","left_arm"));for(Clip c:all.values())bones.addAll(c.bones());
        Map<String,double[]> px=new HashMap<>();
        for(String bone:bones){
            double proc=procPx(bone);double[] r=MotionEngine.reachPx(fam,bone,proc);
            for(Clip c:all.values()){double[] k=c.reach(bone);for(int i=0;i<3;i++)r[i]=Math.max(r[i],proc+k[3+i]*MotionEngine.HIT_GAIN);}
            px.put(bone,r);
        }
        return new Fam(fam,index,gen,Collections.unmodifiableMap(all),Collections.unmodifiableMap(px));
    }
    /** A death clip's key in Family.deaths: death is "any", death_<dir> "any/<dir>", death_<kind> "<kind>", death_<kind>_<dir> "<kind>/<dir>"; null for any other clip. */
    static String deathKey(String name){return MotionEngine.deathKey(name);}
    /**
     * What the procedural layers may move a bone's pivot, per axis (pixels): a root PROC_PX (gait, crouch, weight shift, the feet's
     * roll and dip, the novelty step, the hit pulse's lunge), the arms LIMB_PX (the shrug and the novelty settle) and nothing on any
     * other bone (the planted shins' translation only eases toward zero).
     */
    static double procPx(String bone){return bone.equals("root")?MotionEngine.PROC_PX:bone.equals("right_arm")||bone.equals("left_arm")?LIMB_PX:0;}
    /**
     * The largest |tx|, |ty| and |tz| (pixels) the motion engine can give one bone of this model (Fam.bonePx; procPx alone for a bone no
     * clip keys, or for a bare rig). Shared: read only.
     */
    static double[] bonePx(TroopModel<?> m,String bone){
        Fam f=family(m);double[] r=f==null?null:f.bonePx().get(bone);
        if(r!=null)return r;double p=procPx(bone);return new double[]{p,p,p};
    }

    static Spec.TroopsRow row(TroopModel<?> m){
        if(m.troop.isEmpty())return null;
        return ROWS.computeIfAbsent(m.troop,id->Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)).findFirst()).orElse(null);
    }
    static boolean bow(Spec.TroopsRow row){return row!=null&&row.ranged()&&!row.magic();}

    // ---- the frame ----
    /** Called by TroopModel.setAngles after it reset every bone: this frame's pose of the entity (the class comment gives the order). */
    public static void pose(TroopModel<?> m,MobEntity e,float limbAngle,float limbDistance,float age,float headYaw,float headPitch){
        Forced forced=FORCED.get(e.getId());
        if(forced!=null){applyForced(m,forced.clip(),forced.seconds(),!e.getOffHandStack().isEmpty());return;}
        Spec.TroopsRow row=row(m);Fam fam=row==null?null:family(m);if(fam==null)return;
        Body b=body(e);boolean lies=lies(e);
        if(!lies)b.lying=null;
        else if(b.lying!=null&&b.lyingModel==m){m.applyPose(b.lying);return;} // a lying body's pose no longer changes: put it back
        MotionInput in=input(e,fam,b,limbAngle,limbDistance,age,headYaw,headPitch);
        MotionEngine.pose(in,b.state,fam.family(),b.pose);
        scene(b,fam,e.getId(),in.time());
        if(b.sceneClip!=null)MotionFeet.ground(b.pose,b.state.vec); // fix r3: a scene clip's legs keep the soles out of the ground too
        apply(m,b.pose);
        limits(m);
        boolean shield=!e.getOffHandStack().isEmpty();
        // the action tracks' shares once a frame, read by the bow's draw, the carries and the fold
        double[] shares=b.shares();Clip sc=b.sceneClip==null?null:fam.all().get(b.sceneClip);double sw=sc==null?0:b.sceneW;
        carries(m,bow(row),shield,draw(b.state,in.time(),bow(row),shares),bone->Math.min(1,stanceHeld(b.state,bone)+actionHeld(b.state,bone,shares))+sw*keys(sc,bone));
        rootScale(m);
        b.armHeld=false;for(String arm:ARMS)if(actionHeld(b.state,arm,shares)>0||sw>0&&keys(sc,arm)>0)b.armHeld=true;
        m.refold(folds(m.idleArms,e.isAttacking(),b.armHeld)); // M1: the fold this frame's pose reads, not the last frame's
        if(lies&&still(b.state,in.time())){
            b.lying=m.snapshot(null);b.lyingModel=m;double[] x=m.extent();
            b.lyingReach=new double[]{x[0]+CULL_MARGIN,x[1]-CULL_MARGIN,x[2]+CULL_MARGIN};
        }
    }
    static Body body(Entity e){
        Body b=BODIES.get(e.getId());
        if(b==null||!b.uuid.equals(e.getUuid())){b=new Body(e.getUuid());BODIES.put(e.getId(),b);}
        return b;
    }
    /** Render time in seconds on the world clock the motion events' ticks are on: (world ticks + partial tick) / 20. */
    static double time(Entity e,float age){
        long world=e.getWorld()==null?e.age:e.getWorld().getTime();
        return (world+(double)(age-e.age))/20.0;
    }
    /** The entity's MotionInput for this frame (the class comment, step 1). */
    static MotionInput input(MobEntity e,Fam fam,Body b,float limbAngle,float limbDistance,float age,float headYaw,float headPitch){
        int id=e.getId();boolean dead=!e.isAlive()||e.deathTime>0;
        // carry B12: a dying troop's stance no longer counts (the engine fades the stance to rest on death)
        MotionEvents.Stance s=dead?MotionEvents.Stance.NONE:MotionEvents.Client.stance(id);
        float partial=age-e.age;
        double yawRate=MathHelper.wrapDegrees(e.bodyYaw-e.prevBodyYaw)*20.0;
        MotionInput.Attack a=MotionEvents.Client.attacks(id).peekLast();
        if(a!=null){int v=a.variant();int i=v>=0&&v<fam.attackIndex().length?fam.attackIndex()[v]:-1;a=i<0?null:new MotionInput.Attack(i,a.startTick());}
        boolean shieldUp=e.isUsingItem()&&e.getActiveHand()==Hand.OFF_HAND;
        // carry B10: aimPitch arrives positive-down (stance_v2) and the engine takes it so, unchanged
        return new MotionInput(time(e,age),limbAngle,limbDistance,MathHelper.wrapDegrees(headYaw),headPitch,yawRate,s.name(),s.aimPitch(),shieldUp,
            e.isAttacking(),dead,dead?(e.deathTime+partial)/20.0:0,a,MotionEvents.Client.hits(id).peekLast(),b.seed,ground(e,partial));
    }
    /** Where the entity stands this frame (MotionInput.Ground): its render position and body yaw, lerped as the renderer lerps them. */
    static MotionInput.Ground ground(MobEntity e,float partial){
        return new MotionInput.Ground(MathHelper.lerp(partial,e.prevX,e.getX()),MathHelper.lerp(partial,e.prevY,e.getY()),MathHelper.lerp(partial,e.prevZ,e.getZ()),
            MathHelper.lerpAngleDegrees(partial,e.prevBodyYaw,e.bodyYaw),e.isOnGround());
    }
    /** Adds the pose to the bones: degrees to the rotation, pixels to the pivot. */
    static void apply(TroopModel<?> m,Pose pose){
        for(int i=0,n=pose.size();i<n;i++){
            ModelPart p=m.bone(pose.name(i));if(p==null)continue;double[] r=pose.row(i);
            p.pitch+=(float)(r[0]*RAD);p.yaw+=(float)(r[1]*RAD);p.roll+=(float)(r[2]*RAD);p.pivotX+=(float)r[3];p.pivotY+=(float)r[4];p.pivotZ+=(float)r[5];
        }
    }
    /** Carry C13: a bone with a joint limit keeps its pitch within [min, max] degrees of its file pose. noLimits (harness) skips it: mo-limits' red side. */
    static volatile boolean noLimits;
    static void limits(TroopModel<?> m){
        if(noLimits)return;
        for(var e:m.limits().entrySet()){
            ModelPart p=m.bone(e.getKey());if(p==null)continue;float d=p.getDefaultTransform().pitch;float[] l=e.getValue();
            p.pitch=MathHelper.clamp(p.pitch,d+l[0]*RAD,d+l[1]*RAD);
        }
    }
    /**
     * A scene pose's clip (setScenePose: fallen falls, sword_up raises, a clip's name plays that clip; stand and helm_off hold none)
     * blends over the engine's pose whole-body in SCENE_BLEND, and back out when the pose clears.
     */
    static void scene(Body b,Fam fam,int id,double now){
        String clip=poseClip(SCENE.get(id));
        double dt=Double.isNaN(b.sceneLast)?0:Math.max(0,Math.min(now-b.sceneLast,.1));b.sceneLast=now;
        if(clip!=null&&fam.all().containsKey(clip)&&!clip.equals(b.sceneClip)){b.sceneClip=clip;b.sceneStart=now;b.sceneW=0;}
        boolean on=clip!=null&&clip.equals(b.sceneClip);
        b.sceneW=MathHelper.clamp(b.sceneW+(on?dt:-dt)/SCENE_BLEND,0,1);
        if(b.sceneClip==null)return;
        if(b.sceneW<=0&&!on){b.sceneClip=null;return;}
        Clip c=fam.all().get(b.sceneClip);if(c==null){b.sceneClip=null;return;}
        double w=MotionMath.ease(b.sceneW);
        b.scene.clear();c.add(now-b.sceneStart,b.scene,1,b.state.tmp);
        Pose out=b.pose;
        for(int i=0,n=out.size();i<n;i++){double[] r=out.row(i);for(int k=0;k<6;k++)r[k]*=1-w;}
        out.add(b.scene,w);
    }
    static String poseClip(String pose){
        if(pose==null)return null;
        return switch(pose){case "fallen"->"fall";case "sword_up"->"raise";case "stand","helm_off"->null;default->pose;};
    }
    /** 1 when the clip keys the bone, else 0 (0 for no clip). */
    static double keys(Clip c,String bone){return c!=null&&c.bones().contains(bone)?1:0;}
    /** How much the stance clips playing hold a bone (each layer's share times its clips' coefficients), 0 to 1. */
    static double stanceHeld(MotionEngine.State st,String bone){
        double carry=1,w=0;
        for(int i=st.stances.size()-1;i>=0&&carry>0;i--){
            MotionStack.Layer l=st.stances.get(i);double lw=MotionMath.ease(l.raw),e=lw*carry;carry*=1-lw;
            for(int c=0;c<l.n;c++)if(l.clips[c]!=null&&l.clips[c].bones().contains(bone))w+=e*l.coef[c];
        }
        return Math.min(1,w);
    }
    /**
     * How much the action tracks playing hold a bone (each clip's share of the fold times its gain, as MotionEngine.playing gives it),
     * 0 to 1, from the tracks' shares worked out once for the frame (MotionStack.shares). A merged track holds nothing.
     */
    static double actionHeld(MotionEngine.State st,String bone,double[] shares){
        double w=0;
        for(int i=0,n=st.tracks.size();i<n;i++){
            MotionStack.Track t=st.tracks.get(i);if(t.kind==MotionEngine.MERGED)continue;
            if(t.a.bones().contains(bone))w+=t.gain*(1-t.mix)*shares[i];
            if(t.b!=null&&t.b.bones().contains(bone))w+=t.gain*t.mix*shares[i];
        }
        return Math.min(1,w);
    }
    /** How far a bow is drawn, 0 to 1: the attacks playing (a shot draws, looses and recovers), else how much the aim stance shows. */
    static double draw(MotionEngine.State st,double now,boolean bow,double[] shares){
        if(!bow)return 0;
        double w=0;
        for(int i=0,n=st.tracks.size();i<n;i++){MotionStack.Track t=st.tracks.get(i);if(t.kind==MotionEngine.ATTACK&&t.time(now)<t.length())w+=shares[i];}
        return Math.min(1,Math.max(w,st.frame.aim));
    }

    // ---- the carries (one rule for the frame and for a forced clip) ----
    interface Held{double of(String bone);}
    /**
     * On each bone, what no clip holds (held gives that weight, 0 to 1): the shield arm 45 degrees forward, a bow turned in its hand
     * (draw: 0 at rest, 1 drawn) and idleArms forward.
     */
    static void carries(TroopModel<?> m,boolean bow,boolean shield,double draw,Held held){
        if(row(m)==null)return;
        boolean left="left".equals(m.hand);
        String shieldArm=shield?(left?"right_arm":"left_arm"):null;
        if(shieldArm!=null){ModelPart p=m.bone(shieldArm);if(p!=null)p.pitch-=(float)(45*RAD*(1-held.of(shieldArm)));}
        if(bow)twist(m,left,(float)draw);
        if("forward".equals(m.idleArms))for(String arm:List.of("right_arm","left_arm")){ModelPart p=m.bone(arm);if(p!=null)p.pitch-=(float)(80*RAD*(1-held.of(arm)));}
    }
    /**
     * A bow in its hand: its handle through the middle of the fist, the hand turned so that at rest the bow hangs upright at BOW_REST_YAW
     * to the troop's front with its string to the back and inward, and while it draws upright with the string to the archer.
     */
    static void twist(TroopModel<?> m,boolean left,float draw){
        ModelPart h=m.bone(m.handBone(left?"left":"right"));if(h==null)return;
        h.pitch+=90*draw*RAD;h.yaw+=BOW_REST_YAW*(1-draw)*RAD;h.roll+=90*draw*RAD;
    }
    /** A taller troop (its root above the rig's) scales the root's offsets by its height over the rig's, so its falls land on the ground. */
    static void rootScale(TroopModel<?> m){
        ModelPart root=m.bone("root");float k=m.rootScale();if(root==null||k==1f)return;
        ModelTransform d=root.getDefaultTransform();
        root.pivotX=d.pivotX+(root.pivotX-d.pivotX)*k;root.pivotY=d.pivotY+(root.pivotY-d.pivotY)*k;root.pivotZ=d.pivotZ+(root.pivotZ-d.pivotZ)*k;
    }
    /**
     * Whether the engine state has nothing left to move: every weight ramped out (a dead unit's are), no hit pulse, one stance layer,
     * and every track a death clip past its end.
     */
    static boolean still(MotionEngine.State st,double now){
        for(double w:st.w)if(w!=0)return false;
        if(!st.pulses.isEmpty()||st.stances.size()>1)return false;
        for(MotionStack.Track t:st.tracks)if(t.kind==MotionEngine.MERGED||t.kind!=MotionEngine.DEATH||now<t.end())return false;
        return true;
    }

    // ---- a forced clip (the harness) ----
    /** The clip a name resolves to on a model: its folders' clip, else the aliases (attack, cast and bow_draw: the first attack; hit: a body hit; death: a death). */
    static Clip resolve(TroopModel<?> m,String name){
        Fam f=family(m);if(f==null)return null;
        Clip c=f.all().get(name);if(c!=null)return c;
        MotionEngine.Family fam=f.family();
        switch(name){
            case "attack","cast","bow_draw":return fam.attacks().isEmpty()?null:fam.attacks().get(0);
            case "hit":{List<Clip> l=fam.hits().get("body");return l==null||l.isEmpty()?null:l.get(0);}
            case "death":{if(fam.deaths().isEmpty())return null;Clip d=MotionEngine.deathClip(fam,"any","front");return d!=null?d:fam.deaths().values().iterator().next().get(0);}
            default:return null;
        }
    }
    /**
     * A forced clip at full weight, whole-body, at seconds; the joint limits; the carries on the bones it does not key; the root scaled.
     * A name "stance+clip" (Task 16 r5, the harness) lays the clip over the family's stance as the engine sums them in battle (the
     * stance chain's first clip at its start, the action added on top), so an attack is shot in its in-game pose.
     */
    static void applyForced(TroopModel<?> m,String name,double seconds,boolean shield){
        int plus=name.indexOf('+');Clip under=plus<0?null:stanceOf(m,name.substring(0,plus));if(plus>=0)name=name.substring(plus+1);
        Clip c=resolve(m,name);Pose p=new Pose();
        if(under!=null)under.add(0,p,1,new double[6]);
        if(c!=null)c.add(seconds,p,1,new double[6]);
        MotionFeet.ground(p,new double[3]); // fix r3: a forced clip's soles stay out of the ground as the engine's do
        apply(m,p);limits(m);
        Spec.TroopsRow row=row(m);Fam f=family(m);
        boolean drawn=name.equals("bow_draw")||c!=null&&f!=null&&(f.family().attacks().contains(c)||f.family().wallAttacks().contains(c));
        carries(m,bow(row),shield,drawn?1:0,bone->Math.max(keys(c,bone),keys(under,bone)));
        rootScale(m);
    }
    /** The clip a stance key gives a model's family (MotionEngine.stanceChain's first present), or null. */
    static Clip stanceOf(TroopModel<?> m,String key){
        Fam f=family(m);if(f==null)return null;
        for(String k:MotionEngine.stanceChain(key)){Clip c=f.family().stances().get(k);if(c!=null)return c;}
        return null;
    }

    // ---- the cull reach (Task 16 M2) ----
    /** M2: what the cull reach keeps past the sampled bounds and CULL_MARGIN for what the sampling runs do not drive (the crowd's shove, the cloth springs, uneven ground), blocks. */
    static final double PROC_PAD=.25;
    /** M2: the largest seed the sampling runs drive the engine with (amp and tempo at the top of their spread). */
    static final MotionMath.Seed BOUNDS_SEED=new MotionMath.Seed(0,1.1,1.1,1.15,0,0,0);
    /** M2: the clock the sampling runs start on (seconds). */
    private static final double BOUNDS_T0=100;
    /** M2: the longest any bounds took to sample (milliseconds), for the harness. */
    static volatile double boundsMs;
    /**
     * M2 (Task 16): the cull reach of a model's family (radius, bottom and top over the feet, in blocks, the margins in), sampled
     * from what the engine can pose instead of every bone turned any way. Two passes, each pose measured with TroopModel.extent:
     * 1. every clip of the folders (the mirrored hits too) forced whole-body at 20 Hz from 0 to its length at its gain extreme
     *    (attacks ATTACK_GAIN, hits HIT_GAIN, a stance or scene 1, a death end-pivoted at 0.9 and 1.1), shield down and up;
     * 2. the engine at BOUNDS_SEED for the procedural layers no clip holds: each attack running and turning at 720 degrees a
     *    second with the head at its limits, each hit zone at weight 2 from the front, back and both sides, a death from each side,
     *    and every server stance at the aim extremes, shield down and up.
     * Grown by CULL_MARGIN + PROC_PAD. Null for a bare rig. Leaves every bone reset. Once per (model, clip generation).
     */
    static double[] bounds(TroopModel<?> m){
        Fam f=family(m);if(f==null)return null;
        long t0=System.nanoTime();
        double[] r={0,Double.MAX_VALUE,-Double.MAX_VALUE};
        try{
            MotionEngine.Family fam=f.family();Spec.TroopsRow row=row(m);boolean bow=bow(row);
            Pose p=new Pose();double[] tmp=new double[6];
            List<Clip> clips=new ArrayList<>(f.all().values());
            for(List<Clip> l:fam.hitsMirror().values())clips.addAll(l);
            for(Clip c:clips){
                boolean attack=fam.attacks().contains(c)||fam.wallAttacks().contains(c),death=deathKey(c.name())!=null;
                boolean hit=!attack&&!death&&(HIT.matcher(c.name()).matches()||BLOCK.matcher(c.name()).matches()||c.name().endsWith("~m"));
                double[] gains=death?new double[]{.9,1.1}:new double[]{attack?MotionEngine.ATTACK_GAIN:hit?MotionEngine.HIT_GAIN:1};
                for(int i=0;i*.05<=c.length()+1e-9;i++)for(double g:gains)for(boolean sh:new boolean[]{false,true}){
                    m.getPart().traverse().forEach(ModelPart::resetTransform);
                    p.clear();c.add(i*.05,p,g,tmp);if(death&&g!=1)c.endPose(p,1-g);
                    apply(m,p);limits(m);carries(m,bow,sh,attack?1:0,bone->keys(c,bone));rootScale(m);
                    grow(r,m.extent());
                }
            }
            long tick0=Math.round(BOUNDS_T0*Clip.TICKS)+2;
            for(int a=0;a<fam.attacks().size();a++)for(int k=0;k<2;k++)for(boolean sh:new boolean[]{false,true}){
                MotionInput.Attack at=new MotionInput.Attack(a,tick0);double sp=k==0?1.5:0,yr=k==0?0:720;
                int frames=(int)Math.ceil(fam.attacks().get(a).length()/BOUNDS_SEED.tempo()/.05)+8;
                drive(m,r,sh,frames,(i,now)->boundsIn(now,i*sp,sp,85,60,yr,"hold",0,false,true,false,0,at,null));
            }
            if(!fam.wallAttacks().isEmpty())for(int a=0;a<fam.wallAttacks().size();a++)for(String s:List.of("wall","brace"))for(boolean sh:new boolean[]{false,true}){
                MotionInput.Attack at=new MotionInput.Attack(a,tick0);
                int frames=(int)Math.ceil(fam.wallAttacks().get(a).length()/BOUNDS_SEED.tempo()/.05)+8;
                drive(m,r,sh,frames,(i,now)->boundsIn(now,0,0,85,60,0,s,0,false,true,false,0,at,null));
            }
            for(String zone:List.of("head","body","legs"))for(float bearing:new float[]{0,90,180,-90}){
                MotionInput.Hit h=new MotionInput.Hit("slash",zone,bearing,2,false,false,tick0);
                drive(m,r,true,30,(i,now)->boundsIn(now,0,0,85,60,0,"hold",0,false,true,false,0,null,h));
            }
            for(float bearing:new float[]{0,90,180,-90}){
                MotionInput.Hit h=new MotionInput.Hit("slash","body",bearing,2,false,true,tick0);
                drive(m,r,false,80,(i,now)->boundsIn(now,0,0,0,0,0,"hold",0,false,i<4,i>=4,i>=4?(i-4)*.05:0,null,h));
            }
            for(String s:BattleMath.STANCES)for(float aim:new float[]{-60,60})for(boolean up:new boolean[]{false,true}){
                double sp=s.equals("run")||s.equals("fall_back")||s.equals("shield_move")?1:0;
                drive(m,r,up,30,(i,now)->boundsIn(now,i*sp,sp,-85,-60,0,s,aim,up,up,false,0,null,null));
            }
        }finally{m.getPart().traverse().forEach(ModelPart::resetTransform);}
        boundsMs=Math.max(boundsMs,(System.nanoTime()-t0)/1e6);
        double pad=CULL_MARGIN+PROC_PAD;
        return new double[]{r[0]+pad,r[1]-pad,r[2]+pad};
    }
    private interface BoundsFrames{MotionInput at(int frame,double now);}
    private static MotionInput boundsIn(double now,double limbAngle,double speed,double headYaw,double headPitch,double yawRate,String stance,float aim,
                                        boolean shieldUp,boolean enemy,boolean dead,double deathS,MotionInput.Attack a,MotionInput.Hit h){
        return new MotionInput(now,limbAngle,speed,headYaw,headPitch,yawRate,stance,aim,shieldUp,enemy,dead,deathS,a,h,BOUNDS_SEED);
    }
    private static void drive(TroopModel<?> m,double[] r,boolean shield,int frames,BoundsFrames f){
        MotionEngine.State st=new MotionEngine.State();
        for(int i=0;i<frames;i++){simulate(m,st,f.at(i,BOUNDS_T0+i*.05),shield);grow(r,m.extent());}
    }
    /** Widens r (radius, lowest bottom, highest top) by one extent (TroopModel.extent: radius, bottom, top). */
    private static void grow(double[] r,double[] x){r[0]=Math.max(r[0],x[0]);r[1]=Math.min(r[1],x[1]);r[2]=Math.max(r[2],x[2]);}

    // ---- what the renderer and its features read ----
    /**
     * The time the bow's pull shows, on the BOW_FULL / BOW_LOOSE timeline Weapon.frameAt reads, or -1 when nothing draws: a forced
     * bow_draw shows its time; the newest attack playing with draw and full markers (the bow shots,
     * Task 16 r3b) rests until draw, pulls through the frames to full and holds to its impact (the loose), then rests; one without them
     * draws to the full draw at its impact. The aim stance holds the string at rest (the shot reaches for a fresh arrow).
     */
    public static float drawSeconds(MobEntity e,TroopModel<?> m,float age){
        Forced f=FORCED.get(e.getId());
        if(f!=null)return f.clip().equals("bow_draw")?(float)f.seconds():-1f;
        if(!bow(row(m)))return -1f;
        Body b=BODIES.get(e.getId());if(b==null||!b.uuid.equals(e.getUuid()))return -1f;
        MotionEngine.State st=b.state;double now=time(e,age);
        for(int i=st.tracks.size()-1;i>=0;i--){
            MotionStack.Track t=st.tracks.get(i);if(t.kind!=MotionEngine.ATTACK)continue;
            double s=t.time(now);if(s>=t.length())break;
            if(t.impact>0&&s>=t.impact)return (float)(BOW_LOOSE+(s-t.impact));
            Double d0=t.a.markers().get("draw"),d1=t.a.markers().get("full");
            if(d0!=null&&d1!=null&&d1>d0)return (float)(s<d0?0:BOW_FULL*Math.min(1,(s-d0)/(d1-d0)));
            if(t.impact>0)return (float)(s/t.impact*BOW_LOOSE);
            return (float)Math.min(s/Math.max(t.length(),1e-3)*BOW_FULL,BOW_FULL);
        }
        return -1f;
    }
    /** A troop's body past its fall, with no scene pose or forced clip on it (Task 7: it lies in its death clip's last frame). */
    static boolean lies(Entity e){
        return e instanceof LivingEntity le&&le.isDead()&&le.deathTime>BattleMath.DEATH_FALL_TICKS&&!SCENE.containsKey(e.getId())&&!FORCED.containsKey(e.getId());
    }
    /** A body that lies with its final pose kept for this model, so pose only puts it back and nothing about its draw moves (BodyCache). */
    static boolean settled(MobEntity e,TroopModel<?> m){
        if(!lies(e))return false;Body b=BODIES.get(e.getId());return b!=null&&b.uuid.equals(e.getUuid())&&b.lying!=null&&b.lyingModel==m;
    }
    /** The cull reach of a body lying still whose final pose this client has kept: its own pose's extent; null for any other entity. */
    public static double[] lyingReach(Entity e,TroopModel<?> m){return e instanceof MobEntity me&&settled(me,m)?BODIES.get(e.getId()).lyingReach:null;}
    /** Folded arms show while the troop is not attacking and no action or pose holds an arm (unit identity rule 3). */
    public static boolean folds(String idleArms,boolean attacking,boolean armHeld){return "folded".equals(idleArms)&&!attacking&&!armHeld;}
    /** folds for an entity, from its last frame (or the clip forced on it). */
    static boolean folds(TroopModel<?> m,MobEntity e){
        Forced f=FORCED.get(e.getId());boolean armHeld;
        if(f!=null){String fc=f.clip().substring(f.clip().indexOf('+')+1);Clip c=resolve(m,fc);armHeld=false;for(String arm:ARMS)if(keys(c,arm)>0&&!STANCES.contains(fc))armHeld=true;}
        else{Body b=BODIES.get(e.getId());armHeld=b!=null&&b.uuid.equals(e.getUuid())&&b.armHeld;}
        return folds(m.idleArms,e.isAttacking(),armHeld);
    }
    /** Sets an entity's scene pose (kneel, fallen, stand, helm_off, sword_up, point, sit, raise or a clip's name); null clears it. */
    public static void setScenePose(int entityId,String pose){if(pose==null)SCENE.remove(entityId);else SCENE.put(entityId,pose);}
    public static String scenePose(int entityId){return SCENE.get(entityId);}
    /** Whether the entity's helm parts hide (the helm_off scene pose). */
    public static boolean helmOff(Entity e){return "helm_off".equals(SCENE.get(e.getId()));}
    /** The engine state of an entity id, or null (TroopTells reads its novelty idle). */
    static MotionEngine.State state(int id){Body b=BODIES.get(id);return b==null?null:b.state;}

    // ---- harness hooks ----
    /** Forces one clip at one time (seconds) on an entity; a null clip clears it. */
    public static void force(int id,String clip,double t){if(clip==null)FORCED.remove(id);else FORCED.put(id,new Forced(clip,t));}
    public static Forced forced(int id){return FORCED.get(id);}
    public static void clearForced(){FORCED.clear();}
    /** The pose of a forced clip on a model (bones reset first), as pose would apply it. */
    public static void applyForced(TroopModel<?> m,String clip,double t){
        m.getPart().traverse().forEach(ModelPart::resetTransform);applyForced(m,clip,t,false);
    }
    /**
     * One engine frame of a state on a model (bones reset first) with the model's family, as pose would apply it: the limits, carries
     * (shield as given) and root scale. Returns the Family used, null for a bare rig (only reset).
     */
    public static MotionEngine.Family simulate(TroopModel<?> m,MotionEngine.State st,MotionInput in,boolean shield){
        m.getPart().traverse().forEach(ModelPart::resetTransform);
        Fam f=family(m);if(f==null)return null;
        Pose p=new Pose();MotionEngine.pose(in,st,f.family(),p);apply(m,p);limits(m);
        double[] shares=new double[st.tracks.size()];MotionStack.shares(st.tracks,shares.length,shares);
        carries(m,bow(row(m)),shield,draw(st,in.time(),bow(row(m)),shares),bone->Math.min(1,stanceHeld(st,bone)+actionHeld(st,bone,shares)));rootScale(m);
        return f.family();
    }
    /** The server's attack variant index mapped to the engine's on this model's family, -1 when it has no clip. */
    static int attackIndex(TroopModel<?> m,int variant){Fam f=family(m);return f==null||variant<0||variant>=f.attackIndex().length?-1:f.attackIndex()[variant];}

    /** Drops what this client keeps for an entity id (on unload). */
    public static void forget(int entityId){SCENE.remove(entityId);FORCED.remove(entityId);BODIES.remove(entityId);}
    /** Drops everything (on disconnect). */
    public static void clear(){SCENE.clear();FORCED.clear();BODIES.clear();}
}
