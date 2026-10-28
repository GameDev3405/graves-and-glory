package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import net.minecraft.client.render.entity.animation.Animation;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The biped rig's 18 clips (husks, skeletons, illagers, piglins): the weapon in the right hand, the shield and a bow in
 * the left. right_hand turns the held weapon (the wrist): the attack brings the blade down onto the target and raise holds it
 * up. The falls are linear; the root's offsets keep the feet on the ground while it tips (it pivots at y 0, 24 px above them).
 */
public final class BipedClips {
    private BipedClips() {}
    /** The bow_draw clip (the comment at its put below), the bow arm rolled canted degrees: 0 for the plain draw, 25 for the Order Longbow's (TroopTells). */
    static Clip draw(float canted){
        return new Clip("bow_draw").hold("left_arm",-90,30,canted).hold("body",0,-25,0).hold("head",0,25,0)
            .rot("right_arm",0,-85,-62,0, .15f,-90,-66,0, .65f,-95,-70,0, .8f,-80,-105,0, 1.2f,-85,-62,0);
    }
    /** Alive at rest: the breath lifts the chest, the weight shifts from foot to foot, the head turns a little and comes back. */
    static Clip idle(){
        return new Clip("idle").rot("body",0,0,0,0, .75f,1.5f,-2,0, 1.5f,2,0,1.5f, 2.25f,1.5f,2,0, 3,0,0,0)
                .rot("head",0,0,0,0, .75f,-1,5,0, 1.5f,-2,0,0, 2.25f,0,-5,0, 3,0,0,0)
                .rot("right_arm",0,0,0,0, 1.5f,3,0,4, 3,0,0,0).rot("left_arm",0,0,0,0, 1.5f,2,0,-4, 3,0,0,0)
                .rot("right_leg",0,0,0,0, 1.5f,0,0,-1.5f, 3,0,0,0).rot("left_leg",0,0,0,0, 1.5f,0,0,1.5f, 3,0,0,0)
                .pos("body",0,0,0,0, 1.5f,.4f,0,0, 3,0,0,0);
    }
    /** The blow rocks it back: the body bends away and twists, the head snaps back, the arms fly out, the knees give; recovers by 0.3 s. */
    static Clip hit(){
        return new Clip("hit").rot("body",0,0,0,0, .08f,-20,10,4, .3f,0,0,0).rot("head",0,0,0,0, .08f,-28,-12,0, .3f,0,0,0)
                .rot("right_arm",0,0,0,0, .08f,25,0,25, .3f,0,0,0).rot("left_arm",0,0,0,0, .08f,30,0,-25, .3f,0,0,0)
                .rot("right_leg",0,0,0,0, .08f,-10,0,0, .3f,0,0,0).rot("left_leg",0,0,0,0, .08f,8,0,0, .3f,0,0,0)
                .pos("root",0,0,0,0, .08f,0,-1,2, .3f,0,0,0);
    }
    /** The knees give, then the troop falls back flat about its feet, the arms flung out; a small bounce at 0.85 s. */
    static Clip death(){
        return new Clip("death",true)
                .rot("root",0,0,0,0, .25f,0,0,0, .4f,-15,0,0, .55f,-40,0,0, .7f,-70,0,0, .8f,-90,0,0, .85f,-86,0,0, .9f,-90,0,0)
                .pos("root",0,0,0,0, .25f,0,-2,0, .4f,0,-2.8f,6.2f, .55f,0,-7.1f,15.4f, .7f,0,-16.3f,22.6f, .8f,0,-21,24, .85f,0,-19.3f,23.9f, .9f,0,-21,24)
                .rot("body",0,0,0,0, .25f,15,0,0, .55f,5,0,0, .8f,0,0,0, .9f,0,0,0)
                .rot("head",0,0,0,0, .25f,20,0,0, .6f,0,0,0, .8f,-10,0,0, .9f,-10,0,0)
                .rot("right_leg",0,0,0,0, .25f,-25,0,0, .55f,-10,0,0, .8f,0,0,0, .9f,0,0,0)
                .rot("left_leg",0,0,0,0, .25f,-20,0,0, .55f,-5,0,0, .8f,3,0,0, .9f,3,0,0)
                .rot("right_arm",0,0,0,0, .25f,-15,0,0, .55f,-40,0,15, .8f,-5,0,35, .9f,-5,0,35)
                .rot("left_arm",0,0,0,0, .25f,-10,0,0, .55f,-30,0,-18, .8f,-5,0,-40, .9f,-5,0,-40);
    }
    /*
     * The weapon sets (animation pass 1): the clips a troop plays for the weapon in its hand, in place of the rig's (TroopTells.OVERRIDES).
     * A weapon model's grip turns it about the hand, so the hold is in the wrist (right_hand): a sword's blade lies up and forward 45 degrees
     * from a hanging hand, and a staff stands along the forearm. Without a wrist turn the blade swings up through the shoulder and the helmet,
     * and the staff runs through the shoulder and the hood (Kort's screenshots, 2026-10-28). In the arm's pitch plane the blade's angle below
     * level ahead is -45 + arm pitch + wrist pitch (degrees; 0 level ahead, 90 straight down, -90 straight up); a staff's is -90 + arm + wrist.
     * The harness check p2-anim-clear (Phase2Checks Turn) samples every clip of a set every 0.05 s against the body's boxes.
     */
    /** The sword's wrist at rest: the blade 30 degrees below level ahead from a hanging arm (-45 + 0 + 75), clear of the leg at the side. */
    static final float SWORD_WRIST=75f;
    /**
     * The sword cut, the biped's attack and the Order swordsmen's: from the guard the arm swings up and out to the right, the blade over the
     * shoulder and clear of the head (0.12 s, the weight on the back foot), cuts down and across to the contact at 0.25 s (the blade level
     * ahead, the body turned into it, the front foot stepped), follows through low to the left of centre (0.33 s) and recovers by 0.5 s.
     * The shield arm comes up to -40 and stays there, as the gilded thrust held it.
     */
    static Clip cut(){
        return new Clip("attack").rot("right_arm",0,0,0,0, .12f,-135,15,45, .19f,-110,10,35, .25f,-70,-10,12, .33f,-40,-25,0, .5f,0,0,0)
            .rot("right_hand",0,0,0,0, .12f,0,0,0, .19f,15,0,0, .25f,40,0,0, .33f,45,0,0, .5f,0,0,0)
            .rot("body",0,0,0,0, .12f,-6,25,0, .25f,12,-18,0, .33f,10,-22,0, .5f,0,0,0)
            .rot("head",0,0,0,0, .12f,-4,-12,0, .25f,6,10,0, .5f,0,0,0)
            .rot("left_arm",0,-40,0,0, .4f,-40,0,0, .5f,0,0,0)
            .rot("right_leg",0,0,0,0, .12f,12,0,0, .25f,-20,0,0, .5f,0,0,0)
            .rot("left_leg",0,0,0,0, .12f,-8,0,0, .25f,12,0,0, .5f,0,0,0);
    }
    /**
     * A sword and shield: the blade carried low ahead at the side in every state, the sword arm swinging half the rig's walk. plod: the
     * Sentinel's slow plod (a short stride, the body bobbing half a pixel a step). The run is the charge: leaned in, the blade ahead.
     * The wrist is a hold track under each clip (tracks on a bone add), so the cut above swings about the carried blade.
     */
    static Map<String,Animation> sword(boolean plod){
        float w=SWORD_WRIST;Map<String,Animation> m=new LinkedHashMap<>();
        m.put("idle",idle().hold("right_arm",-8,0,6).hold("right_hand",w,0,0).build());
        Clip walk=plod?new Clip("walk").wave("right_leg",0,0,-20,0,1).wave("left_leg",0,0,20,0,1).wave("right_arm",0,0,8,0,1).wave("left_arm",0,0,-8,0,1).bob("body",1,0,.5f,0,2)
            :new Clip("walk").wave("right_leg",0,0,-26,0,1).wave("left_leg",0,0,26,0,1).wave("right_arm",0,-10,12,0,1).wave("left_arm",0,0,-20,0,1).wave("body",1,0,3,0,1);
        m.put("walk",walk.hold("right_arm",0,0,6).hold("right_hand",w,0,0).build());
        m.put("run",new Clip("run").wave("right_leg",0,0,-48,0,1).wave("left_leg",0,0,48,0,1).wave("right_arm",0,-30,12,0,1).wave("left_arm",0,0,-30,0,1)
            .hold("right_arm",0,0,8).hold("right_hand",w+5,0,0).hold("body",14,0,0).hold("head",-12,0,0).build());
        m.put("attack",cut().hold("right_arm",-8,0,6).hold("right_hand",w,0,0).build());
        m.put("hit",hit().hold("right_arm",-8,0,6).hold("right_hand",w,0,0).rot("right_hand",0,0,0,0, .08f,20,0,0, .3f,0,0,0).build());
        m.put("death",death().hold("right_hand",w,0,0).build());
        m.put("hold",new Clip("hold").hold("right_arm",-30,0,6).hold("left_arm",-20,0,0).hold("right_hand",w+30,0,0).wave("body",2,0,1,0,1).build());
        m.put("brace",new Clip("brace").pos("root",0,0,-1,0, 2,0,-1,0).hold("right_leg",-15,0,0).hold("left_leg",15,0,0)
            .wave("body",0,15,1,0,1).hold("right_arm",-70,0,6).hold("right_hand",115,0,0).hold("head",-15,0,0).build());
        m.put("fall_back",new Clip("fall_back").wave("right_leg",0,0,25,0,1).wave("left_leg",0,0,-25,0,1)
            .hold("right_arm",-40,0,6).hold("right_hand",w+40,0,0).hold("left_arm",-40,0,0).hold("body",-5,0,0).build());
        return Collections.unmodifiableMap(m);
    }
    /**
     * A staff in the right hand: carried upright ahead of the shoulder, the top 10 degrees forward, in every state; the wrist takes the
     * arm's pitch back (-90 + arm + wrist = -80 keeps the top 10 degrees forward) and its roll back, so the shaft never leans into the hood.
     * The cast: the staff raised before the caster, lifting on the pulse, the off hand open ahead.
     */
    /** The staff's wrist at rest: from the arm at -30 the top 25 degrees forward (-90 - 30 + 55 = -65 below level ahead), clear of the forearm. */
    static final float STAFF_WRIST=55f;
    /**
     * A bow in the hanging hand (the Order Archer's and Longbow's idle, walk, run, hit and death): the wrist turns the bow's top 30 degrees
     * ahead, out of the arm and the hood. The clip is written for both hands, since a troop with the bow in the left plays it mirrored.
     */
    static Map<String,Animation> bow(){
        Map<String,Animation> m=new LinkedHashMap<>();float b=30;
        m.put("idle",idle().hold("left_hand",b,0,0).hold("right_hand",b,0,0).build());
        m.put("walk",new Clip("walk").wave("right_leg",0,0,-30,0,1).wave("left_leg",0,0,30,0,1).wave("right_arm",0,0,25,0,1).wave("left_arm",0,0,-25,0,1).wave("body",1,0,3,0,1)
            .hold("left_hand",b,0,0).hold("right_hand",b,0,0).build());
        m.put("run",new Clip("run").wave("right_leg",0,0,-50,0,1).wave("left_leg",0,0,50,0,1).wave("right_arm",0,0,45,0,1).wave("left_arm",0,0,-45,0,1).hold("body",10,0,0).hold("head",-8,0,0)
            .hold("left_hand",b,0,0).hold("right_hand",b,0,0).build());
        m.put("hit",hit().hold("left_hand",b,0,0).hold("right_hand",b,0,0).build());
        m.put("death",death().hold("left_hand",b+20,0,0).hold("right_hand",b+20,0,0).build());
        return Collections.unmodifiableMap(m);
    }
    static Map<String,Animation> staff(){
        Map<String,Animation> m=new LinkedHashMap<>();
        m.put("idle",idle().hold("right_arm",-30,0,12).hold("right_hand",STAFF_WRIST,0,-12).build());
        m.put("walk",new Clip("walk").wave("right_leg",0,0,-30,0,1).wave("left_leg",0,0,30,0,1).wave("right_arm",0,-30,8,0,1).wave("left_arm",0,0,-25,0,1)
            .wave("body",1,0,3,0,1).hold("right_arm",0,0,12).hold("right_hand",STAFF_WRIST,0,-12).build());
        m.put("run",new Clip("run").wave("right_leg",0,0,-50,0,1).wave("left_leg",0,0,50,0,1).wave("right_arm",0,-35,10,0,1).wave("left_arm",0,0,-45,0,1)
            .hold("right_arm",0,0,12).hold("right_hand",STAFF_WRIST-5,0,-12).hold("body",10,0,0).hold("head",-8,0,0).build());
        m.put("cast",new Clip("cast").hold("right_arm",-60,0,10).hold("right_hand",STAFF_WRIST+30,0,-10).wave("right_arm",0,0,-8,0,1).wave("right_hand",0,0,8,0,1)
            .hold("left_arm",-65,-15,-12).wave("left_arm",0,0,6,.25f,1).hold("body",4,0,0).wave("body",0,0,3,0,1).hold("head",-6,0,0).build());
        m.put("hit",hit().hold("right_arm",-30,0,12).hold("right_hand",STAFF_WRIST,0,-12).rot("right_hand",0,0,0,0, .08f,10,0,-10, .3f,0,0,0).build());
        m.put("death",death().hold("right_hand",STAFF_WRIST+10,0,-12).rot("right_hand",0,0,0,0, .55f,0,0,-5, .8f,0,0,-25, .9f,0,0,-25).build());
        m.put("hold",new Clip("hold").hold("right_arm",-30,0,12).hold("left_arm",-20,0,0).hold("right_hand",STAFF_WRIST,0,-12).wave("body",2,0,1,0,1).build());
        m.put("fall_back",new Clip("fall_back").wave("right_leg",0,0,25,0,1).wave("left_leg",0,0,-25,0,1)
            .hold("right_arm",-40,0,12).hold("right_hand",STAFF_WRIST+10,0,-12).hold("left_arm",-40,0,0).hold("body",-5,0,0).build());
        return Collections.unmodifiableMap(m);
    }
    /**
     * Hooks for the weapons the House troops will carry (axe, spear, mace), played by no troop yet: their models are not in
     * (troop-models-house). Each is an attack of the rig's length with the contact at 0.25 s, the wrist written over a carried blade or
     * head at SWORD_WRIST as the Order swords are; check each with p2-anim-clear once its model is in, then wire it in TroopTells.OVERRIDES.
     * The trident is TroopTells.trident(); the bow and the cast are the rig's.
     */
    static final Map<String,Clip> WEAPON_ATTACKS=Map.of(
        // the overhead chop: up behind the shoulder, down to waist height
        "axe",new Clip("attack").rot("right_arm",0,0,0,0, .14f,-160,0,25, .25f,-55,0,0, .5f,0,0,0).rot("right_hand",0,0,0,0, .14f,25,0,0, .25f,25,0,0, .5f,0,0,0)
            .hold("right_hand",SWORD_WRIST,0,0).rot("body",0,0,0,0, .14f,-8,10,0, .25f,15,0,0, .5f,0,0,0),
        // the thrust: drawn back at the hip, driven out level, the body behind it
        "spear",new Clip("attack").rot("right_arm",0,0,0,0, .12f,-20,15,10, .25f,-90,0,0, .5f,0,0,0).rot("right_hand",0,0,0,0, .12f,-10,0,0, .25f,60,0,0, .5f,0,0,0)
            .hold("right_hand",SWORD_WRIST,0,0).rot("body",0,0,0,0, .12f,0,20,0, .25f,12,-10,0, .5f,0,0,0).rot("right_leg",0,0,0,0, .25f,-20,0,0, .5f,0,0,0),
        // the mace: a short wind over the shoulder, a heavy blow down ahead
        "mace",new Clip("attack").rot("right_arm",0,0,0,0, .14f,-150,10,30, .25f,-45,0,0, .5f,0,0,0).rot("right_hand",0,0,0,0, .14f,-15,0,0, .25f,15,0,0, .5f,0,0,0)
            .hold("right_hand",SWORD_WRIST,0,0).rot("body",0,0,0,0, .14f,-6,15,0, .25f,18,0,0, .5f,0,0,0));
    public static final Map<String,Animation> CLIPS=new TroopClips.Set18("biped")
        .put(idle()).put(hit()).put(death())
        .put(new Clip("walk").wave("right_leg",0,0,-30,0,1).wave("left_leg",0,0,30,0,1)
            .wave("right_arm",0,0,25,0,1).wave("left_arm",0,0,-25,0,1).wave("body",1,0,3,0,1))
        .put(new Clip("run").wave("right_leg",0,0,-50,0,1).wave("left_leg",0,0,50,0,1)
            .wave("right_arm",0,0,45,0,1).wave("left_arm",0,0,-45,0,1).hold("body",10,0,0).hold("head",-8,0,0))
        .put(cut())
        .put(new Clip("shield_up").rot("left_arm",0,0,0,0, .2f,-60,30,0))
        // The bow in the left hand, held out in front with the arm turned in 30 and the body turned 25 the other way (the bow
        // points straight ahead, as the arrow goes); the right arm reaches across to the string (its hand within 2 px of the
        // string line at the draw: p2-anim-bow), draws at 0.65 s, which is held between shots, and looses at 0.8 s: the arm flung back.
        .put(draw(0))
        // The caster's stance: the casting arm raised ahead and out, the staff or the hand lifted, the off hand open at the side; the
        // arm sways and the body leans into it as the loop pulses.
        .put(new Clip("cast").hold("right_arm",-75,0,6).hold("right_hand",80,0,0).hold("left_arm",-50,10,-30).hold("head",-8,0,0)
            .wave("right_arm",0,0,12,0,1).wave("left_arm",0,0,8,.25f,1).wave("body",0,3,3,0,1))
        .put(new Clip("kneel").pos("root",0,0,0,0, .3f,0,-2,0, .6f,0,-4,0)
            .rot("right_leg",0,0,0,0, .3f,25,0,0, .6f,48,0,0).rot("left_leg",0,0,0,0, .3f,-25,0,0, .6f,-48,0,0)
            .rot("body",0,0,0,0, .3f,5,0,0, .6f,10,0,0).rot("head",0,0,0,0, .3f,10,0,0, .6f,20,0,0)
            .rot("right_arm",0,0,0,0, .3f,-15,0,0, .6f,-30,0,0).rot("left_arm",0,0,0,0, .3f,-5,0,0, .6f,-10,0,0))
        .put(new Clip("point").rot("right_arm",0,0,0,0, .4f,-90,8,0).rot("body",0,0,0,0, .4f,0,-8,0).rot("head",0,0,0,0, .4f,0,8,0))
        .put(new Clip("sit").pos("root",0,0,0,0, .3f,0,-5,0, .6f,0,-10,0)
            .rot("right_leg",0,0,0,0, .3f,-55,0,0, .6f,-90,8,0).rot("left_leg",0,0,0,0, .3f,-55,0,0, .6f,-90,-8,0)
            .rot("right_arm",0,0,0,0, .6f,-30,0,0).rot("left_arm",0,0,0,0, .6f,-30,0,0).rot("body",0,0,0,0, .6f,-5,0,0))
        .put(new Clip("raise").rot("right_arm",0,0,0,0, .5f,-170,0,0).rot("right_hand",0,0,0,0, .5f,125,0,0)
            .rot("body",0,0,0,0, .5f,-5,0,0).rot("head",0,0,0,0, .5f,-10,0,0))
        // Stumbles, then falls forward flat about its feet, the arms out ahead.
        .put(new Clip("fall",true)
            .rot("root",0,0,0,0, .2f,5,0,0, .4f,30,0,0, .6f,65,0,0, .8f,90,0,0)
            .pos("root",0,0,0,0, .2f,0,-.6f,-2.1f, .4f,0,-4.2f,-12, .6f,0,-15.9f,-21.8f, .8f,0,-21,-24)
            .rot("body",0,0,0,0, .2f,10,0,0, .4f,15,0,0, .6f,5,0,0, .8f,0,0,0)
            .rot("head",0,0,0,0, .2f,10,0,0, .4f,0,0,0, .8f,-10,0,0)
            .rot("right_leg",0,0,0,0, .2f,-10,0,0, .4f,-5,0,0, .8f,0,0,0).rot("left_leg",0,0,0,0, .2f,-5,0,0, .4f,0,0,0, .8f,0,0,0)
            .rot("right_arm",0,0,0,0, .2f,-40,0,0, .4f,-100,0,5, .6f,-150,0,8, .8f,-175,0,8)
            .rot("left_arm",0,0,0,0, .2f,-40,0,0, .4f,-100,0,-5, .6f,-150,0,-8, .8f,-175,0,-8))
        // From fall's last frame: pushes up on the hands, swings the hips over the feet and stands.
        .put(new Clip("stand")
            .rot("root",0,90,0,0, .3f,45,0,0, .6f,15,0,0, .8f,0,0,0)
            .pos("root",0,0,-21,-24, .3f,0,-8.5f,-17, .6f,0,-1.3f,-6.2f, .8f,0,0,0)
            .rot("body",0,0,0,0, .3f,0,0,0, .6f,5,0,0, .8f,0,0,0)
            .rot("head",0,-10,0,0, .3f,-30,0,0, .6f,-10,0,0, .8f,0,0,0)
            .rot("right_leg",0,0,0,0, .8f,0,0,0).rot("left_leg",0,0,0,0, .8f,0,0,0)
            .rot("right_arm",0,-175,0,8, .3f,-45,0,5, .6f,-20,0,0, .8f,0,0,0)
            .rot("left_arm",0,-175,0,-8, .3f,-45,0,-5, .6f,-20,0,0, .8f,0,0,0))
        // Hold in shieldwall, or a spear: knees bent, leaning into the blow, the weapon arm level.
        .put(new Clip("brace").pos("root",0,0,-1,0, 2,0,-1,0).hold("right_leg",-15,0,0).hold("left_leg",15,0,0)
            .wave("body",0,15,1,0,1).hold("right_arm",-70,0,0).hold("head",-15,0,0))
        .put(new Clip("hold").hold("right_arm",-30,0,0).hold("left_arm",-20,0,0).wave("body",2,0,1,0,1))
        .put(new Clip("fall_back").wave("right_leg",0,0,25,0,1).wave("left_leg",0,0,-25,0,1)
            .hold("right_arm",-40,0,0).hold("left_arm",-40,0,0).hold("body",-5,0,0))
        .done();
}
