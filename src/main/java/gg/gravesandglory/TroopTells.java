package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import gg.gravesandglory.TroopClips.Kind;
import net.minecraft.client.render.entity.animation.Animation;
import java.util.*;
import java.util.function.Consumer;

/**
 * The troops' idle tells and the clips that are one troop's own (Phase 2 Task 5c), and the one lookup every clip goes through.
 * <p>
 * A tell is a once clip of 0.8 to 2.0 s, two to four keyframes a bone, that moves rig bones only, so it fits every troop of the
 * rig whatever its parts. It is drawn from the troop's unit identity Tell line (the particles and sounds in those lines stay with
 * the House tasks) and adds to the stance: the carries (a shield arm, arms forward) stay under it, so an angle is a move from how
 * the troop stands. TroopAnimator plays it over idle only (still, no overlay, no scene pose): it starts on the tick that
 * (age + entityId % STAGGER_TICKS) % PERIOD_TICKS is 0. Both are presentation timings.
 * <p>
 * An override is a clip of one troop's own in place of its rig's: it is as long as the clip it replaces and of the same kind (a
 * loop ends on its first frame, a once clip at rest), which is what TroopAnimator's blends and the bow's landmarks
 * (TroopClips.BOW_TIMES) assume. Tells and overrides are written once, for the troop as the brief has it; the animator
 * mirrors an override for a troop the other way round exactly as it mirrors the rig's clip (TroopAnimator.HANDED, bow_draw).
 */
public final class TroopTells {
    private TroopTells() {}
    /** The clip name a tell is played and forced under. */
    public static final String TELL="tell";
    /** Ticks between one tell and the next of a troop, and the spread of the starts: an entity id mod STAGGER_TICKS shifts a troop's tell, so a line of troops does not move in step. */
    public static final int PERIOD_TICKS=160,STAGGER_TICKS=40;
    /**
     * The longest gap between two frames of a troop (ticks) across which a tell that fell due in it still starts, on the first frame after the boundary (a hitch, a frame rate under 20).
     * A longer gap is a troop that was not drawn (culled off screen): it starts nothing on its first frame back and waits for its own next boundary, so a line of troops that comes back
     * into view does not start in step.
     */
    public static final int CATCH_UP_TICKS=20;
    /** The shortest and longest a tell lasts, in seconds. */
    public static final float MIN_SECONDS=.8f,MAX_SECONDS=2f;
    /** Each troop's tell, by troop id (every Spec.TROOPS row has one). */
    public static final Map<String,Animation> TELLS;
    /** The mage's wrist turn during its tell, in degrees (the staff's head goes forward). */
    static final float MAGE_WRIST=75f;
    /** The archer's hand takes this much of the carry's 45 degree rest yaw off in its tell (the draw's turn without its last 20 degrees), so the bow stands upright before the eye. The 20 degrees do not turn its face to the three-quarter front: the bow is edge-on from 315, so the archer's cell is shot from 270. */
    static final float ARCHER_YAW_OFF=TroopAnimator.BOW_REST_YAW-20f;
    /** The second of each tell at which every bone it moves is at its headline angle (the still shots read it there). */
    public static final Map<String,Float> PEAKS;
    /** The clips of one troop in place of its rig's: troop id, then clip name. */
    public static final Map<String,Map<String,Animation>> OVERRIDES;
    private static final Map<String,Map<String,Animation>> MIRRORED_OVERRIDES;
    private static final Map<String,Animation> MIRRORED_VARIANTS;

    /** One tell: its length, the second its bones are all at their angles, and what it moves. */
    private static void tell(Map<String,Animation> out,Map<String,Float> peaks,String troop,float len,float peak,Consumer<Clip> moves){
        Clip c=new Clip(TELL,Kind.ONCE,len,true);moves.accept(c);
        if(len<MIN_SECONDS||len>MAX_SECONDS)throw new IllegalStateException(troop+" tell lasts "+len+" s");
        if(out.put(troop,c.build())!=null)throw new IllegalStateException("two tells for "+troop);
        peaks.put(troop,peak);
    }
    /** A bone from rest to (pitch, yaw, roll) by second a, held there to b, and back at rest by the clip's end: four keys. */
    private static void hold(Clip c,String bone,float a,float b,float pitch,float yaw,float roll){c.rot(bone,0,0,0,0, a,pitch,yaw,roll, b,pitch,yaw,roll, c.len,0,0,0);}
    /** A bone lifted px up (model pixels, y up) from rest by second a, held to b, and back at rest by the clip's end: four keys. */
    private static void lift(Clip c,String bone,float a,float b,float px){c.pos(bone,0,0,0,0, a,0,px,0, b,0,px,0, c.len,0,0,0);}
    /** A bone from rest to (pitch, yaw, roll) by second a, and back at rest by the clip's end: three keys. */
    private static void to(Clip c,String bone,float a,float pitch,float yaw,float roll){c.rot(bone,0,0,0,0, a,pitch,yaw,roll, c.len,0,0,0);}

    static{
        Map<String,Animation> t=new LinkedHashMap<>();Map<String,Float> p=new LinkedHashMap<>();
        // Gilded
        // The shield arm already carries the shield 45 degrees forward, upright with its top at the chin (TroopAnimator.carries): the knight's and the sentinel's
        // tells add 15 degrees to that and lift the arm 1 px, so the shield rises about the chin instead of tipping over the head (fix round 1: -50 and -40 tipped it flat).
        tell(t,p,"knight",1.6f,.8f,c->{hold(c,"left_arm",.45f,1.15f,-15,0,0);lift(c,"left_arm",.45f,1.15f,1);hold(c,"head",.45f,1.15f,-5,0,0);});  // the shield to the chin
        // The bow arm rises to -85 and its hand takes the draw's turn (pitch 90, roll 90, and 25 of the carry's 45 degrees of rest yaw taken off: 20 stay, so the bow stands upright
        // before the eye and still shows its face from the three-quarter front; fix round 1: at -70 with the hand at rest it lay along the forearm, with all 45 off it was edge-on).
        tell(t,p,"archer",1.8f,.75f,c->{hold(c,"left_arm",.5f,1f,-85,0,0);hold(c,"left_hand",.5f,1f,90,-ARCHER_YAW_OFF,90);hold(c,"head",.5f,1f,-5,0,0);}); // the bow to the eye, and down
        tell(t,p,"sentinel",1.4f,.65f,c->{hold(c,"left_arm",.5f,.8f,-15,0,0);lift(c,"left_arm",.5f,.8f,1);hold(c,"body",.5f,.8f,3,0,0);});       // the shield lifted, the feet set
        tell(t,p,"longbow",1.8f,.9f,c->hold(c,"left_arm",.5f,1.3f,-60,0,25));                                                     // sights with the bow canted
        // The staff stands upright in the hand, so the arm's lift alone tips its head back behind the hood: the wrist turns it forward (fix round 1).
        tell(t,p,"mage",1.6f,.5f,c->{hold(c,"right_arm",.5f,1.1f,-40,0,0);hold(c,"right_hand",.5f,1.1f,MAGE_WRIST,0,0);c.rot("head",0,0,0,0, .5f,8,0,0, 1f,0,0,0);}); // the staff forward, a nod as it comes
        // The left arm adds 65 to the shield carry: 110 in all (fix round 1: the brief's -110 on top of the carry was 155 and threw the shield overhead).
        tell(t,p,"gl_marshal",1.8f,.9f,c->{hold(c,"left_arm",.55f,1.25f,-65,0,0);hold(c,"head",.55f,1.25f,-15,0,0);});           // the horn to the lips
        // Graveborn
        tell(t,p,"revenant",1.4f,.5f,c->{hold(c,"head",.5f,1f,0,0,20);c.rot("right_arm",0,0,0,0, .5f,-10,0,0, .75f,0,0,0);});     // the head lolls, the arm twitches
        tell(t,p,"bonebow",1.4f,.7f,c->hold(c,"right_arm",.5f,.9f,0,-30,0));                                                      // eases the string
        tell(t,p,"reaper",1.8f,.9f,c->{hold(c,"right_arm",.5f,1.3f,-60,0,0);hold(c,"body",.5f,1.3f,0,10,0);});                    // rolls the scythe on the shoulder
        tell(t,p,"stalker",1.2f,.6f,c->{hold(c,"body",.4f,.8f,5,0,0);hold(c,"right_arm",.4f,.8f,0,-10,0);hold(c,"left_arm",.4f,.8f,0,10,0);}); // pulls the mantle close
        tell(t,p,"hexer",1.6f,.85f,c->hold(c,"right_arm",.55f,1.15f,-70,0,0));                                                    // lifts the lantern
        tell(t,p,"gb_hollowking",1.8f,.9f,c->hold(c,"head",.6f,1.2f,-10,0,-15));                                                  // tilts the crown
        // Wildmoot
        tell(t,p,"wm_hound",1.4f,.7f,c->{hold(c,"head",.4f,1f,20,0,0);hold(c,"jaw",.4f,1f,25,0,0);});                             // the growl
        tell(t,p,"wm_hunter",1.6f,.4f,c->c.rot("head",0,0,0,0, .4f,-20,-15,0, .9f,-20,15,0, 1.6f,0,0,0));                         // sniffs the air, left then right
        tell(t,p,"wm_stalker",1f,.3f,c->c.rot("left_leg_1",0,0,0,0, .3f,0,0,-30, .55f,0,0,-6, 1f,0,0,0));                         // taps a front leg
        tell(t,p,"wm_tusk",1.2f,.5f,c->{hold(c,"right_arm",.35f,.7f,-20,0,0);hold(c,"body",.35f,.7f,5,0,0);});                    // plants the spear butt
        tell(t,p,"wm_seer",1.4f,.65f,c->hold(c,"head",.4f,.9f,-40,0,0));                                                          // the short howl
        tell(t,p,"wm_oldhorn",2f,1f,c->hold(c,"body",.7f,1.4f,-15,0,0));                                                          // half rears
        // Ironclad
        tell(t,p,"ic_hearthguard",1.6f,.8f,c->{hold(c,"arms_folded",.5f,1.1f,-8,0,0);hold(c,"head",.5f,1.1f,8,0,0);});            // settles the folded arms
        tell(t,p,"ic_arbalist",1.4f,.35f,c->c.rot("right_arm",0,0,0,0, .35f,0,0,20, .8f,0,0,-20, 1.4f,0,0,0));                    // cranks the crossbow
        tell(t,p,"ic_breaker",1.4f,.7f,c->hold(c,"body",.45f,.95f,0,0,8));                                                        // rolls the anvil shoulder
        tell(t,p,"ic_ballista",1.2f,.3f,c->c.rot("right_arm",0,0,0,0, .3f,-40,0,0, .55f,-15,0,0, 1f,0,0,0));                      // taps the frame
        tell(t,p,"ic_mortar",1.6f,.8f,c->hold(c,"head",.5f,1.1f,-30,0,0));                                                        // checks the barrel
        tell(t,p,"ic_colossus",1.4f,.7f,c->{hold(c,"right_arm",.5f,.9f,-30,0,0);hold(c,"left_arm",.5f,.9f,-30,0,0);});            // the arms swing up a little
        // Tidecallers
        tell(t,p,"tc_raider",1.2f,.55f,c->{hold(c,"body",.4f,.7f,10,0,0);hold(c,"right_arm",.4f,.7f,-10,0,0);hold(c,"left_arm",.4f,.7f,-10,0,0);}); // a half-step lunge
        tell(t,p,"tc_harpooner",1.8f,.9f,c->hold(c,"right_arm",.6f,1.2f,-150,0,0));                                               // hefts the harpoon
        tell(t,p,"tc_tideguard",1.8f,.9f,c->hold(c,"right_arm",.6f,1.2f,-160,0,0));                                               // the trident overhead
        tell(t,p,"tc_mist",1.6f,.8f,c->hold(c,"arms_folded",.5f,1.1f,-15,0,0));                                                   // lifts the folded arms
        tell(t,p,"tc_brute",1f,.4f,c->hold(c,"head",.3f,.5f,20,0,0));                                                             // the head-butt feint
        tell(t,p,"tc_admiral",1.8f,.9f,c->hold(c,"right_arm",.6f,1.2f,-160,0,0));                                                 // the trident overhead
        TELLS=Collections.unmodifiableMap(t);PEAKS=Collections.unmodifiableMap(p);

        Map<String,Map<String,Animation>> o=new LinkedHashMap<>();
        // the slow plod: the stride short, the arms hardly swing, the body bobs half a pixel a step
        // the Order swordsmen and the mage play their weapon's set (BipedClips.sword and staff: the blade and the staff held clear of the body);
        // the Sentinel's walk is the slow plod (the stride short, the arms hardly swing, the body bobs half a pixel a step)
        o.put("knight",BipedClips.sword(false));o.put("gl_marshal",BipedClips.sword(false));o.put("sentinel",BipedClips.sword(true));
        o.put("mage",BipedClips.staff());o.put("archer",BipedClips.bow());
        {Map<String,Animation> lb=new LinkedHashMap<>(BipedClips.bow());lb.put("bow_draw",BipedClips.draw(25).build());o.put("longbow",Collections.unmodifiableMap(lb));}
        // the lantern held up in the right hand, swaying a little; the left hand up beside it
        o.put("hexer",Map.of("cast",new Clip("cast").hold("right_arm",-120,0,0).hold("left_arm",-30,0,0).wave("right_arm",0,0,4,0,1).build()));
        o.put("wm_seer",Map.of("cast",new Clip("cast").hold("head",-50,0,0).hold("right_arm",-30,0,0).hold("left_arm",-30,0,0).wave("head",0,0,3,0,1).build()));
        // the bow clips keep the biped's landmarks: the full draw at 0.65 s (held between shots), the loose at 0.8 s
        o.put("ic_arbalist",Map.of("bow_draw",new Clip("bow_draw").rot("left_arm",0,-90,30,0, .65f,-90,30,0, .8f,-96,30,0, 1.2f,-90,30,0)
            .rot("right_arm",0,-90,-30,0, .65f,-90,-30,0, .8f,-96,-30,0, 1.2f,-90,-30,0).wave("right_arm",2,0,12,0,2).build()));
        o.put("ic_ballista",Map.of("bow_draw",new Clip("bow_draw").rot("left_arm",0,-70,0,0, .65f,-70,0,0, .8f,-40,0,0, 1.2f,-70,0,0)
            .rot("right_arm",0,-70,0,0, .65f,-70,0,0, .8f,-40,0,0, 1.2f,-70,0,0).build()));
        o.put("ic_mortar",Map.of("bow_draw",new Clip("bow_draw").rot("left_arm",0,-150,0,0, .65f,-150,0,0, .8f,-30,0,0, 1.2f,-150,0,0)
            .rot("right_arm",0,-150,0,0, .65f,-150,0,0, .8f,-30,0,0, 1.2f,-150,0,0).build()));
        o.put("tc_harpooner",Map.of("bow_draw",new Clip("bow_draw").rot("right_arm",0,-100,0,0, .65f,-170,0,0, .8f,-60,0,0, 1.2f,-100,0,0).build()));
        o.put("tc_tideguard",Map.of("attack",trident()));
        o.put("tc_admiral",Map.of("attack",trident()));
        OVERRIDES=Collections.unmodifiableMap(o);

        Map<String,Map<String,Animation>> mo=new LinkedHashMap<>();
        for(var e:OVERRIDES.entrySet()){Map<String,Animation> m=new LinkedHashMap<>();for(var c:e.getValue().entrySet())m.put(c.getKey(),TroopClips.mirror(c.getValue()));mo.put(e.getKey(),Collections.unmodifiableMap(m));}
        Map<String,Animation> mv=new LinkedHashMap<>();for(var e:HouseAttacks.VARIANTS.entrySet())mv.put(e.getKey(),TroopClips.mirror(e.getValue()));
        MIRRORED_OVERRIDES=Collections.unmodifiableMap(mo);MIRRORED_VARIANTS=Collections.unmodifiableMap(mv);
    }
    /** The Trident Guard's attack, which the Admiral shares: the trident overhead at 0.12 s, thrust to level at the contact, the body leaning into it. */
    private static Animation trident(){
        return new Clip("attack").rot("right_arm",0,0,0,0, .12f,-170,0,0, .25f,-100,0,0, .5f,0,0,0).rot("body",0,0,0,0, .25f,10,0,0, .5f,0,0,0).build();
    }

    /**
     * The clip a troop plays under a name, in order: the troop's own override; else its House's attack variant (the biped attack only;
     * beasts and the colossus keep their rig's); else its rig's clip. "tell" is the troop's tell, or null for a troop without one (a bare
     * rig). troop may be "" and house null (a bare rig); null for a name the rig has no clip of.
     */
    public static Animation clip(String troop,String house,String rig,String clip){return clip(troop,house,rig,clip,false);}
    /** clip, mirrored left for right (what a troop the other way round plays); a tell is no handed clip, so it is the same either way (it is written for the arm the troop's tell line names). */
    public static Animation clip(String troop,String house,String rig,String clip,boolean mirrored){
        String id=troop==null?"":troop;
        if(clip.equals(TELL))return TELLS.get(id);
        Animation own=(mirrored?MIRRORED_OVERRIDES:OVERRIDES).getOrDefault(id,Map.of()).get(clip);
        if(own!=null)return own;
        if(clip.equals("attack")&&"biped".equals(rig)&&house!=null){Animation v=(mirrored?MIRRORED_VARIANTS:HouseAttacks.VARIANTS).get(house);if(v!=null)return v;}
        return (mirrored?TroopClips.mirrored(rig):TroopClips.of(rig)).get(clip);
    }
    /** The clips a troop plays that its rig's table does not hold: its tell, then its overrides and its House's attack variant (a biped), plain and mirrored; TroopModel.cullReach samples them. */
    static List<Animation> own(String troop,String house,String rig){
        List<Animation> out=new ArrayList<>();String id=troop==null?"":troop;
        for(boolean mirrored:new boolean[]{false,true}){
            if(!mirrored){Animation t=TELLS.get(id);if(t!=null)out.add(t);}
            out.addAll((mirrored?MIRRORED_OVERRIDES:OVERRIDES).getOrDefault(id,Map.of()).values());
            Animation v=house!=null&&"biped".equals(rig)?(mirrored?MIRRORED_VARIANTS:HouseAttacks.VARIANTS).get(house):null;if(v!=null)out.add(v);
        }
        return out;
    }
}
