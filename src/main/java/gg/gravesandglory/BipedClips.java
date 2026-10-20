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
    public static final Map<String,Animation> CLIPS=new TroopClips.Set18("biped")
        .put(new Clip("idle").rot("body",0,0,0,0, 1.5f,1.5f,0,0, 3,0,0,0).rot("head",0,0,0,0, 1.5f,-1,0,0, 3,0,0,0)
            .rot("right_arm",0,0,0,0, 1.5f,0,0,2, 3,0,0,0).rot("left_arm",0,0,0,0, 1.5f,0,0,-2, 3,0,0,0))
        .put(new Clip("walk").wave("right_leg",0,0,-30,0,1).wave("left_leg",0,0,30,0,1)
            .wave("right_arm",0,0,25,0,1).wave("left_arm",0,0,-25,0,1).wave("body",1,0,3,0,1))
        .put(new Clip("run").wave("right_leg",0,0,-50,0,1).wave("left_leg",0,0,50,0,1)
            .wave("right_arm",0,0,45,0,1).wave("left_arm",0,0,-45,0,1).hold("body",10,0,0).hold("head",-8,0,0))
        .put(new Clip("attack").rot("right_arm",0,0,0,0, .12f,-110,0,0, .25f,-30,0,0, .33f,20,0,0, .5f,0,0,0)
            .rot("right_hand",0,0,0,0, .12f,0,0,0, .25f,75,0,0, .33f,60,0,0, .5f,0,0,0)
            .rot("body",0,0,0,0, .12f,0,20,0, .25f,0,0,0, .33f,0,-10,0, .5f,0,0,0))
        .put(new Clip("hit").rot("body",0,0,0,0, .08f,-10,0,0, .3f,0,0,0).rot("head",0,0,0,0, .08f,-15,0,0, .3f,0,0,0))
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
        // The bow in the left hand, the body turned side-on (the left shoulder to the target); the right hand nocks at the
        // string, draws out to the cheek and looses at 0.8 s.
        .put(new Clip("bow_draw").hold("left_arm",-90,-20,0).hold("body",0,20,0).hold("head",0,-20,0)
            .rot("right_arm",0,-90,-30,0, .15f,-90,-35,0, .65f,-95,20,0, .8f,-80,30,0, 1.2f,-90,-30,0))
        .put(new Clip("cast").hold("right_arm",0,0,125).hold("left_arm",0,0,-125)
            .wave("right_arm",0,0,10,0,1).wave("left_arm",0,0,10,0,1))
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
