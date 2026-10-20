package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.animation.Transformation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SpellcastingIllagerEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Poses a troop's model each frame from its rig's clips (Phase 2 Task 5b). choose picks the clips from the troop's state,
 * its stance (the server's stance_v1 packet) and a scene's pose; pose blends them, every change over BLEND_TICKS, and keeps
 * per entity the clips playing (State, weak keys by identity). A frame, after TroopModel.setAngles resets every bone:
 * 1. the base: the still clip at 1 - m and the move clip at m (m = min(2.5 limbDistance, 1)), the move clip on the limb
 *    angle (100 ms per unit, as vanilla's animateMovement); a shield arm keeps a third of it;
 * 2. a scene pose or death over the base, the whole body;
 * 3. the overlays, lowest first (hit, raise, shield_up, cast, bow_draw, attack): each first moves the bones it names toward
 *    rest by its weight, then plays on them; hit plays only on bones no other overlay holds;
 * 4. the carries, on bones nothing else holds: the shield arm 45 degrees forward, a bow turned in its hand (string back at
 *    rest, string to the archer while it draws) and idleArms forward;
 * 5. a taller troop's root offsets scaled to its height (TroopModel.rootScale);
 * 6. the head look (yaw 30 either way, pitch 0 to 30 down), faded out while posed or dead.
 * A model without a troop (a bare rig) plays only what is forced on it.
 */
public final class TroopAnimator {
    private TroopAnimator() {}
    static final float RAD=MathHelper.RADIANS_PER_DEGREE;
    /** Ticks an overlay, a scene pose, a stance change or the head look takes to blend in or out (a presentation timing, animate's weight). */
    public static final int BLEND_TICKS=3;
    /** The stance the server last sent for each troop entity id (Tactics.STANCES); no entry is "". */
    public static final Map<Integer,String> STANCES=new ConcurrentHashMap<>();
    /** Harness hook: one clip frozen at one time (seconds) on an entity, in its own role (base, overlay or pose), without the head look. */
    public record Forced(String clip,float seconds){}
    public static final Map<Integer,Forced> forced=new ConcurrentHashMap<>();
    /** The scene pose of each entity id (setScenePose). */
    private static final Map<Integer,String> SCENE=new ConcurrentHashMap<>();
    public record View(String rig,boolean dead,boolean swingStart,boolean hurtStart,boolean attacking,boolean ranged,boolean magic,boolean spellcasting,
                       boolean shieldUp,boolean rearing,float limbDistance,String stance,String scenePose,String lastPose){}
    /** The clips of a frame: a posed or dead troop has its whole-body clip in still and no move clip. */
    public record Choice(String still,String move,String overlay,boolean reverse,boolean headLook){}
    /** The poses stand plays backwards. */
    static final Set<String> REVERSIBLE=Set.of("kneel","sit","point","raise");
    static final List<String> BASE=List.of("idle","hold","brace","walk","run","fall_back"),OVERLAYS=List.of("hit","raise","shield_up","cast","bow_draw","attack"),
        POSES=List.of("death","kneel","point","sit","raise","fall","stand");
    private static final int BOW=OVERLAYS.indexOf("bow_draw");
    /** Clips written for the weapon in the right hand that a troop holding it in the left plays mirrored. */
    static final Set<String> HANDED=Set.of("attack","shield_up","hold","brace","point","raise");
    private static final List<String> ARMS=List.of("right_arm","left_arm");
    private static final Map<Entity,State> STATES=new MapMaker().weakKeys().makeMap();
    private static final Map<String,Optional<Spec.TroopsRow>> ROWS=new ConcurrentHashMap<>();
    private static final Vector3f TMP=new Vector3f();

    /** The first rule that matches: death; a scene pose; the base with the first overlay that applies. Pure. */
    public static Choice choose(View v){
        if(v.dead())return new Choice("death",null,null,false,false);
        if(v.scenePose()!=null){
            String pose=v.scenePose(),clip;boolean reverse=false;
            if(pose.equals("stand")){String last=poseClip(v.lastPose());if("fallen".equals(v.lastPose()))clip="stand";else if(last!=null&&REVERSIBLE.contains(last)){clip=last;reverse=true;}else clip=null;}
            else if(pose.equals("helm_off"))clip=poseClip(v.lastPose());
            else clip=poseClip(pose);
            if(clip!=null)return new Choice(clip,null,null,reverse,false);
            return new Choice(still(v.stance()),move(v.stance()),null,false,false);
        }
        return new Choice(still(v.stance()),move(v.stance()),overlay(v),false,true);
    }
    /** The clip a scene pose holds: fallen falls, the title demos' sword_up raises, a clip's name plays that clip; stand and helm_off hold none of their own. */
    static String poseClip(String pose){
        if(pose==null)return null;
        return switch(pose){case "fallen"->"fall";case "sword_up"->"raise";case "stand","helm_off"->null;default->TroopClips.KINDS.containsKey(pose)?pose:null;};
    }
    static String overlay(View v){
        if(!v.ranged()&&v.swingStart())return "attack";
        if(v.ranged()&&!v.magic()&&v.attacking())return "bow_draw";
        if(v.magic()&&v.attacking()||v.spellcasting())return "cast";
        if(v.shieldUp())return "shield_up";
        if(v.rearing())return "raise";
        if(v.hurtStart())return "hit";
        return null;
    }
    static String still(String stance){return switch(stance==null?"":stance){case "brace"->"brace";case "hold"->"hold";default->"idle";};}
    static String move(String stance){return switch(stance==null?"":stance){case "run"->"run";case "fall_back"->"fall_back";default->"walk";};}

    /** One play of a clip: started at start (ticks of age), fading in from inAt, fading out from outAt. */
    static final class Track {
        final String clip;final boolean reverse;final float start,inAt;float outAt=Float.NaN,outFrom;
        Track(String clip,boolean reverse,float start,float inAt){this.clip=clip;this.reverse=reverse;this.start=start;this.inAt=inAt;}
        float weight(float age){float w=MathHelper.clamp((age-inAt)/BLEND_TICKS,0f,1f);return live()?w:outFrom*MathHelper.clamp(1f-(age-outAt)/BLEND_TICKS,0f,1f);}
        void fadeOut(float age){if(live()){outFrom=weight(age);outAt=age;}}
        boolean live(){return Float.isNaN(outAt);}
        boolean gone(float age){return !live()&&age-outAt>=BLEND_TICKS;}
        float seconds(float age){return Math.max(0f,(age-start)/20f);}
    }
    /** The plays of one role: the live one, and those still fading out. */
    static final class Channel {
        final List<Track> tracks=new ArrayList<>(3);
        Track live(){for(int i=tracks.size()-1;i>=0;i--)if(tracks.get(i).live())return tracks.get(i);return null;}
        /** Plays the clip, unless it already plays (the same way round); null fades out what plays. */
        void want(String clip,boolean reverse,float age,float inAt){Track cur=live();if(cur!=null&&cur.clip.equals(clip)&&cur.reverse==reverse)return;if(cur!=null)cur.fadeOut(age);if(clip!=null)tracks.add(new Track(clip,reverse,age,inAt));}
        /** Plays the clip from its start, the old play fading out. */
        void restart(String clip,float age,float inAt){Track cur=live();if(cur!=null)cur.fadeOut(age);tracks.add(new Track(clip,false,age,inAt));}
        void clear(float age){for(Track t:tracks)t.fadeOut(age);}
        float weight(float age){float w=0;for(Track t:tracks)w+=t.weight(age);return Math.min(1f,w);}
        void prune(float age){tracks.removeIf(t->t.gone(age));}
    }
    /** A weight that moves to its target over BLEND_TICKS (the head look). */
    static final class Fade {
        float from,to,at=-1e9f;Fade(float v){from=to=v;}
        float get(float age){return from+(to-from)*MathHelper.clamp((age-at)/BLEND_TICKS,0f,1f);}
        void set(float target,float age){if(target!=to){from=get(age);to=target;at=age;}}
        void now(float v){from=to=v;at=-1e9f;}
    }
    /** One entity's clips, and what its last frame saw (the swing, the hurt time, the scene pose). A new State starts its clips fully in. */
    public static final class State {
        final Channel still=new Channel(),move=new Channel(),pose=new Channel();final Map<String,Channel> overlays=new HashMap<>();
        final Fade look=new Fade(1);final float phase;boolean fresh=true;
        boolean swinging;int swingTicks,hurt;String scene,lastPose;boolean armHeld;
        /** The idle phase is offset by the entity id, so a line of troops does not breathe in step. */
        public State(int id){phase=Math.floorMod(id*7919,3000);}
        Channel overlay(String name){return overlays.computeIfAbsent(name,k->new Channel());}
        public void update(Choice c,View v,float age){
            float in=fresh?age-BLEND_TICKS:age;boolean posed=c.move()==null;
            if(posed){pose.want(c.still(),c.reverse(),age,in);for(Channel o:overlays.values())o.clear(age);}
            else{
                pose.clear(age);still.want(c.still(),false,age,in);move.want(c.move(),false,age,in);
                if(!v.ranged()&&v.swingStart())overlay("attack").restart("attack",age,in);
                if(v.hurtStart())overlay("hit").restart("hit",age,in);
                hold("bow_draw",v.ranged()&&!v.magic()&&v.attacking(),v.swingStart(),age,in);
                hold("cast",v.magic()&&v.attacking()||v.spellcasting(),v.swingStart(),age,in);
                hold("shield_up",v.shieldUp(),false,age,in);
                hold("raise",v.rearing(),false,age,in);
            }
            if(fresh)look.now(c.headLook()?1:0);else look.set(c.headLook()?1:0,age);
            for(var e:overlays.entrySet()){
                // a once clip ends at rest: it fades out over its last BLEND_TICKS, so the bones it named return to the base without a pop
                if(TroopClips.KINDS.get(e.getKey())==TroopClips.Kind.ONCE)for(Track t:e.getValue().tracks)if(t.seconds(age)>=TroopClips.LENGTHS.get(e.getKey())-BLEND_TICKS/20f)t.fadeOut(age);
                e.getValue().prune(age);
            }
            still.prune(age);move.prune(age);pose.prune(age);fresh=false;
        }
        private void hold(String name,boolean on,boolean restart,float age,float in){Channel o=overlay(name);Track cur=o.live();if(!on){if(cur!=null)cur.fadeOut(age);}else if(cur==null||restart)o.restart(name,age,in);}
        /** The weight of the poses and overlays on a bone (the carries give way to it). */
        float held(TroopModel<?> m,String bone,boolean bow,float age){
            float w=0;
            for(Track t:pose.tracks){Animation a=clip(m,t.clip,bow);if(a!=null&&a.boneAnimations().containsKey(bone))w+=t.weight(age);}
            for(var e:overlays.entrySet()){Animation a=clip(m,e.getKey(),bow);if(a!=null&&a.boneAnimations().containsKey(bone))w+=e.getValue().weight(age);}
            return Math.min(1f,w);
        }
    }

    /** A clip as this troop plays it: mirrored for a bow in the right hand (bow_draw holds it in the left), and the handed clips for a weapon in the left. */
    static Animation clip(TroopModel<?> m,String clip,boolean bow){
        boolean left="left".equals(m.hand()),mirror=clip.equals("bow_draw")?bow&&!left:left&&HANDED.contains(clip);
        return (mirror?TroopClips.mirrored(m.rig):TroopClips.of(m.rig)).get(clip);
    }
    static void animate(TroopModel<?> m,Animation a,float seconds,float w){if(a!=null&&w>1e-4f)AnimationHelper.animate(m,a,(long)(seconds*1000f),w,TMP);}
    /** Moves a bone toward its rest: keep 0 puts it at rest, 1 leaves it. */
    static void damp(ModelPart p,float keep){
        ModelTransform d=p.getDefaultTransform();
        p.pivotX=d.pivotX+(p.pivotX-d.pivotX)*keep;p.pivotY=d.pivotY+(p.pivotY-d.pivotY)*keep;p.pivotZ=d.pivotZ+(p.pivotZ-d.pivotZ)*keep;
        p.pitch=d.pitch+(p.pitch-d.pitch)*keep;p.yaw=d.yaw+(p.yaw-d.yaw)*keep;p.roll=d.roll+(p.roll-d.roll)*keep;
    }
    private static final Map<Animation,Map<String,Animation>> WITHOUT=new MapMaker().weakKeys().makeMap();
    /** The clip without the named bones (hit, under another overlay). */
    static Animation without(Animation a,Set<String> bones){
        if(Collections.disjoint(a.boneAnimations().keySet(),bones))return a;
        String key=String.join(",",new TreeSet<>(bones));
        return WITHOUT.computeIfAbsent(a,k->new ConcurrentHashMap<>()).computeIfAbsent(key,k->{
            Map<String,List<Transformation>> out=new LinkedHashMap<>();for(var e:a.boneAnimations().entrySet())if(!bones.contains(e.getKey()))out.put(e.getKey(),e.getValue());
            return new Animation(a.lengthInSeconds(),a.looping(),Collections.unmodifiableMap(out));});
    }
    static Spec.TroopsRow row(TroopModel<?> m){
        if(m.troop.isEmpty())return null;
        return ROWS.computeIfAbsent(m.troop,id->Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)).findFirst()).orElse(null);
    }
    static boolean bow(Spec.TroopsRow row){return row!=null&&row.ranged()&&!row.magic();}

    /** One frame of a State on a model whose bones are at rest; row null is a bare rig (no base, carries or head look). */
    static void apply(TroopModel<?> m,State s,Spec.TroopsRow row,boolean shield,float limbAngle,float limbDistance,float age,float headYaw,float headPitch){
        boolean bare=row==null,bow=bow(row),left="left".equals(m.hand());
        float poseW=s.pose.weight(age),mv=Math.min(2.5f*limbDistance,1f);
        String shieldArm=shield&&!bare?(left?"right_arm":"left_arm"):null;
        if(!bare){
            float base=1-poseW;
            for(Track t:s.still.tracks)animate(m,clip(m,t.clip,bow),(age*50f+s.phase)/1000f,t.weight(age)*(1-mv)*base);
            for(Track t:s.move.tracks)animate(m,clip(m,t.clip,bow),limbAngle/10f,t.weight(age)*mv*base);
            ModelPart arm=shieldArm==null?null:m.bone(shieldArm);if(arm!=null)damp(arm,.3f);
        }
        for(Track t:s.pose.tracks){Animation a=clip(m,t.clip,bow);if(a==null)continue;float sec=t.seconds(age);if(t.reverse)sec=Math.max(0f,a.lengthInSeconds()-sec);animate(m,a,sec,t.weight(age));}
        float[] w=new float[OVERLAYS.size()];
        for(int i=0;i<w.length;i++){Channel o=s.overlays.get(OVERLAYS.get(i));w[i]=o==null?0:o.weight(age);}
        for(int i=0;i<w.length;i++){
            if(w[i]<=0)continue;String name=OVERLAYS.get(i);Animation a=clip(m,name,bow);if(a==null)continue;
            if(i==0){Set<String> others=new HashSet<>();for(int j=1;j<w.length;j++)if(w[j]>0){Animation b=clip(m,OVERLAYS.get(j),bow);if(b!=null)others.addAll(b.boneAnimations().keySet());}a=without(a,others);}
            for(String bone:a.boneAnimations().keySet()){ModelPart p=m.bone(bone);if(p!=null)damp(p,1-w[i]);}
            for(Track t:s.overlays.get(name).tracks)animate(m,a,t.seconds(age),t.weight(age));
        }
        if(!bare){
            if(shieldArm!=null){ModelPart p=m.bone(shieldArm);if(p!=null)p.pitch-=45*RAD*(1-s.held(m,shieldArm,bow,age));}
            if(bow)twist(m,left,w[BOW]);
            if("forward".equals(m.idleArms()))for(String arm:ARMS){ModelPart p=m.bone(arm);if(p!=null)p.pitch-=80*RAD*(1-s.held(m,arm,bow,age));}
        }
        rootScale(m);
        if(!bare)look(m,headYaw,headPitch,s.look.get(age)*(1-poseW));
        s.armHeld=s.held(m,"right_arm",bow,age)>0||s.held(m,"left_arm",bow,age)>0;
    }
    /** A bow in its hand: at rest turned so the string faces back and the grip out (not mirrored for the left hand: only the grip goes out); while drawing, upright with the string to the archer. */
    static void twist(TroopModel<?> m,boolean left,float draw){
        ModelPart h=m.bone(m.handBone(left?"left":"right"));if(h==null)return;
        h.pitch+=90*draw*RAD;h.yaw+=(left?-90:90)*(1-draw)*RAD;h.roll+=90*draw*RAD;
    }
    /** A taller troop (its root above the rig's) scales the root's offsets by its height over the rig's, so its falls land on the ground. */
    static void rootScale(TroopModel<?> m){
        ModelPart root=m.bone("root");float k=m.rootScale();if(root==null||k==1f)return;
        ModelTransform d=root.getDefaultTransform();
        root.pivotX=d.pivotX+(root.pivotX-d.pivotX)*k;root.pivotY=d.pivotY+(root.pivotY-d.pivotY)*k;root.pivotZ=d.pivotZ+(root.pivotZ-d.pivotZ)*k;
    }
    static void look(TroopModel<?> m,float headYaw,float headPitch,float w){
        ModelPart head=m.bone("head");if(head==null||w<=0)return;
        head.yaw+=MathHelper.clamp(MathHelper.wrapDegrees(headYaw),-TroopModel.HEAD_YAW,TroopModel.HEAD_YAW)*RAD*w;
        head.pitch+=MathHelper.clamp(headPitch,0,TroopModel.HEAD_DOWN)*RAD*w;
    }

    /** Called by TroopModel.setAngles after it reset every bone: this frame's pose of the entity (the class comment gives the order). */
    public static void pose(TroopModel<?> m,MobEntity e,float limbAngle,float limbDistance,float age,float headYaw,float headPitch){
        if(TroopClips.of(m.rig).isEmpty()){look(m,headYaw,headPitch,1);return;}
        Spec.TroopsRow row=row(m);boolean shield=!e.getOffHandStack().isEmpty();
        Forced f=forced.get(e.getId());
        if(f!=null){applyForced(m,f,row,shield);return;}
        if(row==null)return;
        State s=STATES.computeIfAbsent(e,k->new State(k.getId()));
        View v=view(m,e,s,limbDistance,row);s.update(choose(v),v,age);
        apply(m,s,row,shield,limbAngle,limbDistance,age,headYaw,headPitch);
    }
    /** What the entity shows this frame, against what its State saw last frame (swing and hurt starts, the scene pose before this one). */
    static View view(TroopModel<?> m,MobEntity e,State s,float limbDistance,Spec.TroopsRow row){
        boolean swingStart=e.handSwinging&&(!s.swinging||e.handSwingTicks<s.swingTicks);s.swinging=e.handSwinging;s.swingTicks=e.handSwingTicks;
        boolean hurtStart=e.hurtTime>s.hurt;s.hurt=e.hurtTime;
        String scene=SCENE.get(e.getId());
        if(!Objects.equals(scene,s.scene)){if(scene==null)s.lastPose=null;else if(s.scene!=null&&!s.scene.equals("helm_off"))s.lastPose=s.scene;s.scene=scene;}
        return new View(m.rig,!e.isAlive()||e.deathTime>0,swingStart,hurtStart,e.isAttacking(),row.ranged(),row.magic(),e instanceof SpellcastingIllagerEntity sp&&sp.isSpellcasting(),
            e.isUsingItem()&&e.getActiveHand()==Hand.OFF_HAND,e instanceof PolarBearEntity pb&&pb.isWarning(),limbDistance,STANCES.getOrDefault(e.getId(),""),scene,s.lastPose);
    }
    /** A forced clip at full weight in its own role: a base clip as the base (the shield arm keeping a third), an overlay or a pose on the bones it names; the carries on the rest. */
    static void applyForced(TroopModel<?> m,Forced f,Spec.TroopsRow row,boolean shield){
        boolean bare=row==null,bow=bow(row),left="left".equals(m.hand());
        Animation a=clip(m,f.clip(),bow);if(a==null)return;
        boolean base=BASE.contains(f.clip());
        animate(m,a,f.seconds(),1f);
        if(!bare){
            Set<String> named=base?Set.of():a.boneAnimations().keySet();String shieldArm=shield?(left?"right_arm":"left_arm"):null;
            if(shieldArm!=null&&m.bone(shieldArm)!=null){ModelPart p=m.bone(shieldArm);if(base)damp(p,.3f);if(!named.contains(shieldArm))p.pitch-=45*RAD;}
            if(bow)twist(m,left,f.clip().equals("bow_draw")?1:0);
            if("forward".equals(m.idleArms()))for(String arm:ARMS){ModelPart p=m.bone(arm);if(p!=null&&!named.contains(arm))p.pitch-=80*RAD;}
        }
        rootScale(m);
    }

    /** Sets an entity's scene pose (kneel, fallen, stand, helm_off, sword_up, point, sit, raise or a clip's name); null clears it. Phase 4's ScenePoses calls it for scene_v1 pose ops. */
    public static void setScenePose(int entityId,String pose){if(pose==null)SCENE.remove(entityId);else SCENE.put(entityId,pose);}
    public static String scenePose(int entityId){return SCENE.get(entityId);}
    /** Whether the entity's helm parts hide (the helm_off scene pose). */
    public static boolean helmOff(Entity e){return "helm_off".equals(SCENE.get(e.getId()));}
    /** Whether the entity may be anywhere its whole-body clips reach (dead, posed, or forced to a pose): its cull box is the posed one. */
    public static boolean posed(Entity e){
        if(!e.isAlive()||e instanceof LivingEntity le&&le.deathTime>0||SCENE.containsKey(e.getId()))return true;
        Forced f=forced.get(e.getId());return f!=null&&POSES.contains(f.clip());
    }
    /** Folded arms show while the troop is not attacking and no overlay or pose holds an arm (unit identity rule 3). */
    public static boolean folds(String idleArms,boolean attacking,boolean armHeld){return "folded".equals(idleArms)&&!attacking&&!armHeld;}
    /** folds for an entity, from the clips its last frame played. */
    static boolean folds(TroopModel<?> m,MobEntity e){
        State s=STATES.get(e);Forced f=forced.get(e.getId());
        boolean armHeld=s!=null&&s.armHeld||f!=null&&!BASE.contains(f.clip())&&clip(m,f.clip(),bow(row(m)))!=null&&ARMS.stream().anyMatch(clip(m,f.clip(),bow(row(m))).boneAnimations()::containsKey);
        return folds(m.idleArms(),e.isAttacking(),armHeld);
    }
    /** Drops what this client keeps for an entity id (on unload). */
    public static void forget(int entityId){STANCES.remove(entityId);SCENE.remove(entityId);forced.remove(entityId);}
    /** Drops everything (on disconnect). */
    public static void clear(){STANCES.clear();SCENE.clear();forced.clear();}

    /** Harness: the pose the entity would have with nothing playing but its base, as from a fresh State, as TroopModel.snapshot gives it. */
    public static float[] reference(TroopModel<?> m,MobEntity e,float limbAngle,float limbDistance,float age,float headYaw,float headPitch){
        m.getPart().traverse().forEach(ModelPart::resetTransform);
        Spec.TroopsRow row=row(m);if(row==null)return m.snapshot(null);State s=new State(e.getId());
        View v=new View(m.rig,false,false,false,e.isAttacking(),row.ranged(),row.magic(),false,false,false,limbDistance,STANCES.getOrDefault(e.getId(),""),null,null);
        s.update(choose(v),v,age);apply(m,s,row,!e.getOffHandStack().isEmpty(),limbAngle,limbDistance,age,headYaw,headPitch);
        return m.snapshot(null);
    }
    /** Harness: one frame of a State on a model (bare: no base, carries or head look), after resetting its bones. */
    public static void simulate(TroopModel<?> m,State s,View v,float age,boolean bare){
        m.getPart().traverse().forEach(ModelPart::resetTransform);
        s.update(choose(v),v,age);apply(m,s,bare?null:row(m),false,0,v.limbDistance(),age,0,0);
    }
}
