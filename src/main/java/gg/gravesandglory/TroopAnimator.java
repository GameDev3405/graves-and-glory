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
import java.util.function.ToDoubleFunction;

/**
 * Poses a troop's model each frame from its rig's clips (Phase 2 Task 5b). plan picks the clips from the troop's state,
 * its stance (the server's stance_v2 packet, MotionEvents) and a scene's pose; pose blends them, every change over BLEND_TICKS, and keeps
 * per entity the clips playing (State, weak keys by identity). A frame, after TroopModel.setAngles resets every bone:
 * 1. the base: the still clip at 1 - m and the move clip at m (m = min(2.5 limbDistance, 1)), the move clip on the limb
 *    angle (100 ms per unit, as vanilla's animateMovement); a shield arm keeps a third of it; then the idle tell, if one plays: it adds
 *    to the base (TroopTells: it starts only while the troop stands still, with no overlay or scene pose, once every PERIOD_TICKS);
 * 2. a scene pose or death over the base, the whole body;
 * 3. the overlays, lowest first (hit, raise, shield_up, cast, bow_draw, attack): each first moves the bones it names toward
 *    rest by its weight, then plays on them; hit plays only on bones no other overlay holds;
 * 4. the carries, on bones nothing else holds: the shield arm 45 degrees forward, a bow turned in its hand (string back at
 *    rest, string to the archer while it draws) and idleArms forward;
 * 5. a taller troop's root offsets scaled to its height (TroopModel.rootScale);
 * 6. the head look (yaw 30 either way, pitch 0 to 30 down), faded out while posed or dead.
 * Every clip is looked up through TroopTells.clip: the troop's own override, else its House's attack variant, else its rig's.
 * A model without a troop (a bare rig) plays only what is forced on it.
 */
public final class TroopAnimator {
    private TroopAnimator() {}
    static final float RAD=MathHelper.RADIANS_PER_DEGREE;
    /** Ticks an overlay, a scene pose, a stance change or the head look takes to blend in or out (a presentation timing, animate's weight). */
    public static final int BLEND_TICKS=3;
    /** Ticks a once overlay (attack, hit) takes to blend in, and a restarted bow_draw to leave the draw for the loose: its peak (hit at 0.08 s, the attack's wind-up at 0.12 s) must show in full. */
    static final int SNAP_TICKS=1;
    /**
     * The turn of a bow in its hand at rest, degrees of yaw (troop depth design section 2: 45, so a flat sprite's curve reads at 71
     * percent from the front and from the side; the hand still turns the bow upright into the plane of the shot at the draw).
     */
    public static final float BOW_REST_YAW=45;
    /** The stance the server last sent for each troop entity id (Tactics.STANCES); no entry is "". */
    public static final Map<Integer,String> STANCES=new ConcurrentHashMap<>();
    /**
     * Harness hook: one clip frozen at one time (seconds) on an entity, in its own role (base, overlay or pose), without the head look.
     * "tell" is the troop's idle tell, which adds to the stance (the carries stay under it). House, when not null, is the House whose
     * attack variant a forced "attack" plays in place of the troop's own (the harness shows the five variants on one knight).
     */
    public record Forced(String clip,float seconds,String house){public Forced(String clip,float seconds){this(clip,seconds,null);}}
    public static final Map<Integer,Forced> forced=new ConcurrentHashMap<>();
    /** The scene pose of each entity id (setScenePose). */
    private static final Map<Integer,String> SCENE=new ConcurrentHashMap<>();
    public record View(String rig,boolean dead,boolean swingStart,boolean hurtStart,boolean attacking,boolean ranged,boolean magic,boolean spellcasting,
                       boolean shieldUp,boolean rearing,float limbDistance,String stance,String scenePose,String lastPose){}
    /** The clips of a frame: a posed or dead troop has its whole-body clip in still and no move clip. */
    public record Choice(String still,String move,String overlay,boolean reverse,boolean headLook){}
    /** An overlay a frame wants: the clip, and whether this frame starts it afresh (a swing, a shot, a hurt start). */
    public record Overlay(String clip,boolean restart){}
    /**
     * What a frame plays, from a View: the clips (Choice), whether the troop is posed or dead (no overlays play, however its swing or hurt reads),
     * every overlay wanted, in priority order, and whether it stands still (idle: no pose, no overlay, not attacking, no stance, not moving),
     * which is when its idle tell may start.
     */
    public record Plan(String rig,Choice choice,boolean posed,List<Overlay> overlays,boolean still){
        /** The same plan with no tell to start (the harness's reference pose has only the base). */
        Plan noTell(){return new Plan(rig,choice,posed,overlays,false);}
    }
    /** The limb distance under which a troop stands still (the walk is in at 5 percent here, which no eye catches). */
    static final float STILL_LIMB=.02f;
    /** The poses stand plays backwards. */
    static final Set<String> REVERSIBLE=Set.of("kneel","sit","point","raise");
    static final List<String> BASE=List.of("idle","hold","brace","walk","run","fall_back"),OVERLAYS=List.of("hit","raise","shield_up","cast","bow_draw","attack"),
        POSES=List.of("death","kneel","point","sit","raise","fall","stand");
    /** The overlays that play once from a swing or a hurt (a restart each time); the rest are held while their condition holds. */
    private static final Set<String> ONCE_OVERLAYS=Set.of("attack","hit");
    private static final int BOW=OVERLAYS.indexOf("bow_draw");
    /** Clips written for the weapon in the right hand that a troop holding it in the left plays mirrored. */
    static final Set<String> HANDED=Set.of("attack","shield_up","hold","brace","point","raise");
    private static final List<String> ARMS=List.of("right_arm","left_arm");
    private static final Map<Entity,State> STATES=new MapMaker().weakKeys().makeMap();
    private static final Map<String,Optional<Spec.TroopsRow>> ROWS=new ConcurrentHashMap<>();
    private static final Vector3f TMP=new Vector3f();

    /** The first rule that matches: death; a scene pose; the base with the first overlay that applies. Pure. */
    public static Choice choose(View v){return plan(v).choice();}
    /**
     * The one rule for what plays (choose reports its first overlay, State.update plays all of it). Death, then a scene pose, then the base
     * with the overlays. A scene pose or death plays no overlay, whether or not the pose has a clip of its own (helm_off with nothing
     * before it, stand with nothing to reverse, plays the base). Pure.
     */
    public static Plan plan(View v){
        if(v.dead())return new Plan(v.rig(),new Choice("death",null,null,false,false),true,List.of(),false);
        if(v.scenePose()!=null){
            String pose=v.scenePose(),clip;boolean reverse=false;
            if(pose.equals("stand")){String last=poseClip(v.lastPose());if("fallen".equals(v.lastPose()))clip="stand";else if(last!=null&&REVERSIBLE.contains(last)){clip=last;reverse=true;}else clip=null;}
            else if(pose.equals("helm_off"))clip=poseClip(v.lastPose());
            else clip=poseClip(pose);
            if(clip!=null)return new Plan(v.rig(),new Choice(clip,null,null,reverse,false),true,List.of(),false);
            return new Plan(v.rig(),new Choice(still(v.stance()),move(v.stance()),null,false,false),true,List.of(),false);
        }
        List<Overlay> on=overlays(v);
        boolean standing=on.isEmpty()&&!v.attacking()&&(v.stance()==null||v.stance().isEmpty())&&v.limbDistance()<STILL_LIMB;
        return new Plan(v.rig(),new Choice(still(v.stance()),move(v.stance()),on.isEmpty()?null:on.get(0).clip(),false,true),false,on,standing);
    }
    /** The clip a scene pose holds: fallen falls, the title demos' sword_up raises, a clip's name plays that clip; stand and helm_off hold none of their own. */
    static String poseClip(String pose){
        if(pose==null)return null;
        return switch(pose){case "fallen"->"fall";case "sword_up"->"raise";case "stand","helm_off"->null;default->TroopClips.KINDS.containsKey(pose)?pose:null;};
    }
    /** Every overlay a standing troop's View wants, in priority order (attack, bow_draw, cast, shield_up, raise, hit). */
    static List<Overlay> overlays(View v){
        List<Overlay> on=new ArrayList<>(3);
        if(!v.ranged()&&v.swingStart())on.add(new Overlay("attack",true));
        if(v.ranged()&&!v.magic()&&v.attacking())on.add(new Overlay("bow_draw",v.swingStart()));
        if(v.magic()&&v.attacking()||v.spellcasting())on.add(new Overlay("cast",v.swingStart()));
        if(v.shieldUp())on.add(new Overlay("shield_up",false));
        if(v.rearing())on.add(new Overlay("raise",false));
        if(v.hurtStart())on.add(new Overlay("hit",true));
        return on;
    }
    static String still(String stance){return switch(stance==null?"":stance){case "brace"->"brace";case "hold"->"hold";default->"idle";};}
    static String move(String stance){return switch(stance==null?"":stance){case "run"->"run";case "fall_back"->"fall_back";default->"walk";};}

    /**
     * The time bow_draw shows for a play that started at offset seconds (0 for a draw, the loose time for a shot) after elapsed seconds:
     * a draw plays up to the full draw and holds there; a shot shows the loose (the arrow leaves on the frame it starts), plays the
     * rest of the clip out to the nock, draws again and holds at the full draw until the next shot. Pure.
     */
    static float bowSeconds(String rig,float offset,float elapsed){
        float[] b=TroopClips.BOW_TIMES.getOrDefault(rig,TroopClips.BOW_TIMES.get("biped"));float draw=b[0],len=TroopClips.LENGTHS.get("bow_draw"),u=offset+elapsed;
        if(offset<draw)return Math.min(u,draw);
        return u<len?u:Math.min(u-len,draw);
    }

    /** One play of a clip: started at start (ticks of age) from offset seconds into it, fading in from inAt over inTicks, fading out from outAt over outTicks. */
    static final class Track {
        final String clip;final boolean reverse;final float start,inAt,offset;int inTicks=BLEND_TICKS,outTicks=BLEND_TICKS;float outAt=Float.NaN,outFrom;
        Track(String clip,boolean reverse,float start,float inAt,float offset){this.clip=clip;this.reverse=reverse;this.start=start;this.inAt=inAt;this.offset=offset;}
        float weight(float age){float w=MathHelper.clamp((age-inAt)/inTicks,0f,1f);return live()?w:outFrom*MathHelper.clamp(1f-(age-outAt)/outTicks,0f,1f);}
        void fadeOut(float age){fadeOut(age,BLEND_TICKS);}
        void fadeOut(float age,int ticks){if(live()){outFrom=weight(age);outAt=age;outTicks=ticks;}}
        boolean live(){return Float.isNaN(outAt);}
        boolean gone(float age){return !live()&&age-outAt>=outTicks;}
        float elapsed(float age){return Math.max(0f,(age-start)/20f);}
        float seconds(float age){return offset+elapsed(age);}
    }
    /** The plays of one role: the live one, and those still fading out. */
    static final class Channel {
        final List<Track> tracks=new ArrayList<>(3);
        Track live(){for(int i=tracks.size()-1;i>=0;i--)if(tracks.get(i).live())return tracks.get(i);return null;}
        /** Plays the clip, unless it already plays (the same way round); null fades out what plays. */
        void want(String clip,boolean reverse,float age,float inAt){Track cur=live();if(cur!=null&&cur.clip.equals(clip)&&cur.reverse==reverse)return;if(cur!=null)cur.fadeOut(age);if(clip!=null)tracks.add(new Track(clip,reverse,age,inAt,0f));}
        /** Plays the clip again from offset seconds into it, the old play fading out; snap is the ticks both blends take. */
        void restart(String clip,float age,float inAt,float offset,int snap){Track cur=live();if(cur!=null)cur.fadeOut(age,snap);Track t=new Track(clip,false,age,inAt,offset);t.inTicks=snap;tracks.add(t);}
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
        final Channel still=new Channel(),move=new Channel(),pose=new Channel(),tell=new Channel();final Map<String,Channel> overlays=new HashMap<>();
        final Fade look=new Fade(1);final float phase;final int id;boolean fresh=true;
        boolean swinging;int swingTicks,hurt;String scene,lastPose;boolean armHeld;
        /** The tick of the frame before: a tell starts when a period boundary lies between it and this frame's tick, so a hitch that skips the boundary's own tick does not skip the tell (across TroopTells.CATCH_UP_TICKS or fewer). */
        int lastTick=Integer.MIN_VALUE;
        /** The idle phase is offset by the entity id, so a line of troops does not breathe in step; so is the tell (TroopTells.STAGGER_TICKS). */
        public State(int id){this.id=id;phase=Math.floorMod(id*7919,3000);}
        Channel overlay(String name){return overlays.computeIfAbsent(name,k->new Channel());}
        /** Takes a frame's Plan: the whole-body clip or the base, and the overlays it wants (once clips restart, held ones start, restart on a shot, and stop when no longer wanted). */
        public void update(Plan p,float age){
            Choice c=p.choice();float in=fresh?age-BLEND_TICKS:age;
            if(c.move()==null)pose.want(c.still(),c.reverse(),age,in);
            else{pose.clear(age);still.want(c.still(),false,age,in);move.want(c.move(),false,age,in);}
            if(p.posed()){for(Channel o:overlays.values())o.clear(age);}
            else for(String name:OVERLAYS){
                Overlay w=null;for(Overlay o:p.overlays())if(o.clip().equals(name))w=o;
                if(ONCE_OVERLAYS.contains(name)){if(w!=null)overlay(name).restart(name,age,in,0f,SNAP_TICKS);}
                else hold(p.rig(),name,w,age,in);
            }
            if(fresh)look.now(c.headLook()?1:0);else look.set(c.headLook()?1:0,age);
            for(var e:overlays.entrySet()){
                // a once clip ends at rest: it fades out over its last BLEND_TICKS, so the bones it named return to the base without a pop
                if(TroopClips.KINDS.get(e.getKey())==TroopClips.Kind.ONCE)for(Track t:e.getValue().tracks)if(t.seconds(age)>=TroopClips.LENGTHS.get(e.getKey())-BLEND_TICKS/20f)t.fadeOut(age);
                e.getValue().prune(age);
            }
            still.prune(age);move.prune(age);pose.prune(age);tell(p,age);fresh=false;
        }
        /**
         * The idle tell (TroopTells): while the troop stands still, with no pose or overlay on it (none still fading out either), a tell
         * starts on the first frame at or after each tick at which (tick + id % STAGGER_TICKS) % PERIOD_TICKS is 0 (the first frame after the boundary
         * when a hitch or a slow frame rate skipped its own tick, for a gap of TroopTells.CATCH_UP_TICKS or less between frames: a longer gap is a troop that was not
         * drawn, which starts nothing on its first frame back and waits for its own next boundary, so troops coming back into view are not in step). A tell that plays when
         * the troop moves, strikes, is hurt or is posed fades out over BLEND_TICKS; one that ends keeps its last frame, which is at rest.
         */
        private void tell(Plan p,float age){
            int tick=(int)Math.floor(age),shift=Math.floorMod(id,TroopTells.STAGGER_TICKS),before=lastTick==Integer.MIN_VALUE?tick-1:lastTick; // a first frame counts as the tick before it
            boolean boundary=tick-before<=TroopTells.CATCH_UP_TICKS&&Math.floorDiv(tick+shift,TroopTells.PERIOD_TICKS)>Math.floorDiv(before+shift,TroopTells.PERIOD_TICKS);
            lastTick=tick;
            boolean quiet=p.still()&&pose.weight(age)<=0&&overlays.values().stream().allMatch(o->o.weight(age)<=0);
            if(!quiet)tell.clear(age);
            else if(boundary)tell.restart(TroopTells.TELL,age,age,0f,SNAP_TICKS);
            for(Track t:tell.tracks)if(t.live()&&t.seconds(age)>=TroopTells.MAX_SECONDS)t.fadeOut(age);
            tell.prune(age);
        }
        /** A held overlay: starts when wanted, plays on while it is, fades out when it is not. A shot restarts bow_draw at its loose, from one tick's blend; any other restart starts at the clip's start. */
        private void hold(String rig,String name,Overlay w,float age,float in){
            Channel o=overlay(name);Track cur=o.live();
            if(w==null){if(cur!=null)cur.fadeOut(age);return;}
            boolean bow=name.equals("bow_draw");
            if(cur==null||w.restart()){
                if(bow&&w.restart())o.restart(name,age,in,TroopClips.BOW_TIMES.getOrDefault(rig,TroopClips.BOW_TIMES.get("biped"))[1],SNAP_TICKS);
                else o.restart(name,age,in,0f,BLEND_TICKS);
            }
        }
        /** The weight of the poses and overlays on a bone (the carries give way to it). */
        float held(TroopModel<?> m,String bone,boolean bow,float age){
            float w=0;
            for(Track t:pose.tracks){Animation a=clip(m,t.clip,bow);if(a!=null&&a.boneAnimations().containsKey(bone))w+=t.weight(age);}
            for(var e:overlays.entrySet()){Animation a=clip(m,e.getKey(),bow);if(a!=null&&a.boneAnimations().containsKey(bone))w+=e.getValue().weight(age);}
            return Math.min(1f,w);
        }
        /** Harness: what the State plays at an age, the clips of each role that are live (not fading out): "still walk, overlays [attack, hit]". */
        public String playing(float age){
            Track s=still.live(),mv=move.live(),po=pose.live();List<String> on=new ArrayList<>();
            for(var e:overlays.entrySet()){Track t=e.getValue().live();if(t!=null&&t.weight(age)>0)on.add(e.getKey());}
            Collections.sort(on);
            return "pose "+(po==null?"-":po.clip+(po.reverse?" reversed":""))+", still "+(s==null?"-":s.clip)+", move "+(mv==null?"-":mv.clip)+", overlays "+on;
        }
        /** Harness: the age the live tell started at, or NaN when none plays. */
        public float tellStart(){Track t=tell.live();return t==null?Float.NaN:t.start;}
        /** Harness: the seconds into its clip the live tell shows at an age, or -1 when none plays. */
        public float tellSeconds(float age){Track t=tell.live();return t==null?-1f:t.seconds(age);}
        /** Harness: the weight of an overlay at an age (0 when it is not playing). */
        public float overlayWeight(String name,float age){Channel o=overlays.get(name);return o==null?0f:o.weight(age);}
        /** Harness: the time (seconds into its clip) the live play of an overlay shows at an age, or -1. */
        public float overlaySeconds(TroopModel<?> m,String name,float age){Channel o=overlays.get(name);Track t=o==null?null:o.live();return t==null?-1f:seconds(m.rig,name,t,age);}
    }
    /** The time a play of an overlay shows at an age: bow_draw holds at its full draw between shots, any other clip runs from its start. */
    static float seconds(String rig,String name,Track t,float age){return name.equals("bow_draw")?bowSeconds(rig,t.offset,t.elapsed(age)):t.seconds(age);}

    /**
     * The time the live bow_draw play of an entity shows, seconds into the clip, or -1 when nothing draws (a clip forced on it that
     * is not bow_draw counts as nothing). Weapon.frameAt picks the bow's pull frame from it.
     */
    public static float drawSeconds(MobEntity e,TroopModel<?> m,float age){
        Forced f=forced.get(e.getId());
        if(f!=null)return f.clip().equals("bow_draw")?f.seconds():-1f;
        State s=STATES.get(e);return s==null?-1f:s.overlaySeconds(m,"bow_draw",age);
    }

    /**
     * A clip as this troop plays it (TroopTells.clip: its own override, else its House's attack, else its rig's; "tell" is its tell): mirrored for a
     * bow in the right hand (bow_draw holds it in the left), and the handed clips for a weapon in the left.
     */
    static Animation clip(TroopModel<?> m,String clip,boolean bow){return clip(m,clip,bow,null);}
    /** clip, with house (when not null) in place of the troop's own House, for the attack variant (the harness's forced House attack). */
    static Animation clip(TroopModel<?> m,String clip,boolean bow,String house){
        boolean left="left".equals(m.hand()),mirror=clip.equals("bow_draw")?bow&&!left:left&&HANDED.contains(clip);
        Spec.TroopsRow row=house==null?row(m):null;
        return TroopTells.clip(m.troop,house!=null?house:row==null?null:row.house(),m.rig,clip,mirror);
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
            shieldBase(m,shieldArm);
            for(Track t:s.tell.tracks)animate(m,clip(m,TroopTells.TELL,bow),t.seconds(age),t.weight(age)*(1-poseW));
        }
        for(Track t:s.pose.tracks){Animation a=clip(m,t.clip,bow);if(a==null)continue;float sec=t.seconds(age);if(t.reverse)sec=Math.max(0f,a.lengthInSeconds()-sec);animate(m,a,sec,t.weight(age));}
        float[] w=new float[OVERLAYS.size()];
        for(int i=0;i<w.length;i++){Channel o=s.overlays.get(OVERLAYS.get(i));w[i]=o==null?0:o.weight(age);}
        for(int i=0;i<w.length;i++){
            if(w[i]<=0)continue;String name=OVERLAYS.get(i);Animation a=clip(m,name,bow);if(a==null)continue;
            if(i==0){Set<String> others=new HashSet<>();for(int j=1;j<w.length;j++)if(w[j]>0){Animation b=clip(m,OVERLAYS.get(j),bow);if(b!=null)others.addAll(b.boneAnimations().keySet());}a=without(a,others);}
            for(String bone:a.boneAnimations().keySet()){ModelPart p=m.bone(bone);if(p!=null)damp(p,1-w[i]);}
            for(Track t:s.overlays.get(name).tracks)animate(m,a,seconds(m.rig,name,t,age),t.weight(age));
        }
        if(!bare)carries(m,bow,left,shieldArm,w[BOW],bone->s.held(m,bone,bow,age));
        rootScale(m);
        if(!bare)look(m,headYaw,headPitch,s.look.get(age)*(1-poseW));
        s.armHeld=s.held(m,"right_arm",bow,age)>0||s.held(m,"left_arm",bow,age)>0;
    }
    /** The shield arm keeps a third of the base's swing (the shield is carried, not swung). */
    static void shieldBase(TroopModel<?> m,String shieldArm){ModelPart arm=shieldArm==null?null:m.bone(shieldArm);if(arm!=null)damp(arm,.3f);}
    /**
     * The carries, one rule for the frame and for a forced clip: on each bone, what a pose or an overlay does not hold (held gives that
     * weight, 0 to 1): the shield arm 45 degrees forward, a bow turned in its hand (draw is bow_draw's weight) and idleArms forward.
     */
    static void carries(TroopModel<?> m,boolean bow,boolean left,String shieldArm,float draw,ToDoubleFunction<String> held){
        if(shieldArm!=null){ModelPart p=m.bone(shieldArm);if(p!=null)p.pitch-=45*RAD*(1-(float)held.applyAsDouble(shieldArm));}
        if(bow)twist(m,left,draw);
        if("forward".equals(m.idleArms()))for(String arm:ARMS){ModelPart p=m.bone(arm);if(p!=null)p.pitch-=80*RAD*(1-(float)held.applyAsDouble(arm));}
    }
    /**
     * A bow in its hand: its handle through the middle of the fist (the weapon's grip offset is zero), the hand turned so that at rest the bow hangs
     * upright at BOW_REST_YAW to the troop's front with its string to the back and inward, and while it draws (the arm raised to the front) upright
     * with the string to the archer.
     * The same turn for either hand: the handle is on the hand's axis, so no side goes out.
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
        View v=view(m,e,s,limbDistance,row);s.update(plan(v),age);
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
    /** A forced clip at full weight in its own role: a base clip as the base (the shield arm keeping a third), an overlay or a pose on the bones it names; the carries (the frame's own) on the rest. */
    static void applyForced(TroopModel<?> m,Forced f,Spec.TroopsRow row,boolean shield){
        boolean bare=row==null,bow=bow(row),left="left".equals(m.hand());
        Animation a=clip(m,f.clip(),bow,f.house());if(a==null)return;
        boolean base=BASE.contains(f.clip());
        animate(m,a,f.seconds(),1f);
        if(!bare){
            Set<String> named=base||f.clip().equals(TroopTells.TELL)?Set.of():a.boneAnimations().keySet();String shieldArm=shield?(left?"right_arm":"left_arm"):null;
            if(base)shieldBase(m,shieldArm);
            carries(m,bow,left,shieldArm,f.clip().equals("bow_draw")?1f:0f,bone->named.contains(bone)?1:0);
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
        s.update(plan(v).noTell(),age);apply(m,s,row,!e.getOffHandStack().isEmpty(),limbAngle,limbDistance,age,headYaw,headPitch);
        return m.snapshot(null);
    }
    /** Harness: one frame of a State on a model (bare: no base, carries or head look; else its troop's row), after resetting its bones. */
    public static void simulate(TroopModel<?> m,State s,View v,float age,boolean bare){
        m.getPart().traverse().forEach(ModelPart::resetTransform);
        s.update(plan(v),age);apply(m,s,bare?null:row(m),false,0,v.limbDistance(),age,0,0);
    }
}
