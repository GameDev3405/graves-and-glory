package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import net.minecraft.client.render.entity.animation.Animation;
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
    public static final Map<String,Animation> CLIPS=new TroopClips.Set18("biped")
        // Alive at rest: the breath lifts the chest, the weight shifts from foot to foot, the head turns a little and comes back.
        .put(new Clip("idle").rot("body",0,0,0,0, .75f,1.5f,-2,0, 1.5f,2,0,1.5f, 2.25f,1.5f,2,0, 3,0,0,0)
            .rot("head",0,0,0,0, .75f,-1,5,0, 1.5f,-2,0,0, 2.25f,0,-5,0, 3,0,0,0)
            .rot("right_arm",0,0,0,0, 1.5f,3,0,4, 3,0,0,0).rot("left_arm",0,0,0,0, 1.5f,2,0,-4, 3,0,0,0)
            .rot("right_leg",0,0,0,0, 1.5f,0,0,-1.5f, 3,0,0,0).rot("left_leg",0,0,0,0, 1.5f,0,0,1.5f, 3,0,0,0)
            .pos("body",0,0,0,0, 1.5f,.4f,0,0, 3,0,0,0))
        .put(new Clip("walk").wave("right_leg",0,0,-30,0,1).wave("left_leg",0,0,30,0,1)
            .wave("right_arm",0,0,25,0,1).wave("left_arm",0,0,-25,0,1).wave("body",1,0,3,0,1))
        .put(new Clip("run").wave("right_leg",0,0,-50,0,1).wave("left_leg",0,0,50,0,1)
            .wave("right_arm",0,0,45,0,1).wave("left_arm",0,0,-45,0,1).hold("body",10,0,0).hold("head",-8,0,0))
        // The sword cut: wound back over the shoulder with the weight on the back foot (0.12 s), the cut falling to the contact at 0.25 s
        // (the body turned and leaning into it, the front foot stepped), through to the follow-through and the recovery.
        .put(new Clip("attack").rot("right_arm",0,0,0,0, .12f,-155,15,0, .25f,-45,-22,0, .33f,-15,-25,0, .5f,0,0,0)
            .rot("right_hand",0,0,0,0, .12f,-30,0,0, .25f,75,0,0, .33f,60,0,0, .5f,0,0,0)
            .rot("body",0,0,0,0, .12f,-8,25,0, .25f,14,-15,0, .33f,12,-22,0, .5f,0,0,0)
            .rot("head",0,0,0,0, .12f,-4,-10,0, .25f,6,8,0, .5f,0,0,0)
            .rot("left_arm",0,0,0,0, .12f,-15,0,0, .25f,-40,10,0, .5f,0,0,0)
            .rot("right_leg",0,0,0,0, .12f,15,0,0, .25f,-20,0,0, .5f,0,0,0)
            .rot("left_leg",0,0,0,0, .12f,-10,0,0, .25f,12,0,0, .5f,0,0,0))
        // The blow rocks it back: the body bends away and twists, the head snaps back, the arms fly out, the knees give; recovers by 0.3 s.
        .put(new Clip("hit").rot("body",0,0,0,0, .08f,-20,10,4, .3f,0,0,0).rot("head",0,0,0,0, .08f,-28,-12,0, .3f,0,0,0)
            .rot("right_arm",0,0,0,0, .08f,25,0,25, .3f,0,0,0).rot("left_arm",0,0,0,0, .08f,30,0,-25, .3f,0,0,0)
            .rot("right_leg",0,0,0,0, .08f,-10,0,0, .3f,0,0,0).rot("left_leg",0,0,0,0, .08f,8,0,0, .3f,0,0,0)
            .pos("root",0,0,0,0, .08f,0,-1,2, .3f,0,0,0))
        // The knees give, then the troop falls back flat about its feet, the arms flung out; a small bounce at 0.85 s.
        .put(new Clip("death",true)
            .rot("root",0,0,0,0, .25f,0,0,0, .4f,-15,0,0, .55f,-40,0,0, .7f,-70,0,0, .8f,-90,0,0, .85f,-86,0,0, .9f,-90,0,0)
            .pos("root",0,0,0,0, .25f,0,-2,0, .4f,0,-2.8f,6.2f, .55f,0,-7.1f,15.4f, .7f,0,-16.3f,22.6f, .8f,0,-21,24, .85f,0,-19.3f,23.9f, .9f,0,-21,24)
            .rot("body",0,0,0,0, .25f,15,0,0, .55f,5,0,0, .8f,0,0,0, .9f,0,0,0)
            .rot("head",0,0,0,0, .25f,20,0,0, .6f,0,0,0, .8f,-10,0,0, .9f,-10,0,0)
            .rot("right_leg",0,0,0,0, .25f,-25,0,0, .55f,-10,0,0, .8f,0,0,0, .9f,0,0,0)
            .rot("left_leg",0,0,0,0, .25f,-20,0,0, .55f,-5,0,0, .8f,3,0,0, .9f,3,0,0)
            .rot("right_arm",0,0,0,0, .25f,-15,0,0, .55f,-40,0,15, .8f,-5,0,35, .9f,-5,0,35)
            .rot("left_arm",0,0,0,0, .25f,-10,0,0, .55f,-30,0,-18, .8f,-5,0,-40, .9f,-5,0,-40))
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
