package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import net.minecraft.client.render.entity.animation.Animation;
import java.util.Map;

/**
 * The colossus rig's 18 clips (iron golems): long arms from the shoulders, short legs, no weapon. Its falls go forward;
 * the root's offsets keep the feet on the ground and lay its 11 px deep chest on it.
 */
public final class ColossusClips {
    private ColossusClips() {}
    public static final Map<String,Animation> CLIPS=new TroopClips.Set18("colossus")
        .put(new Clip("idle").rot("body",0,0,0,0, 1.5f,1,0,0, 3,0,0,0).rot("head",0,0,0,0, 1.5f,-1,0,0, 3,0,0,0)
            .rot("right_arm",0,0,0,0, 1.5f,0,0,3, 3,0,0,0).rot("left_arm",0,0,0,0, 1.5f,0,0,-3, 3,0,0,0))
        .put(new Clip("walk").wave("right_leg",0,0,-25,0,1).wave("left_leg",0,0,25,0,1).wave("right_arm",0,0,20,0,1).wave("left_arm",0,0,-20,0,1))
        .put(new Clip("run").wave("right_leg",0,0,-40,0,1).wave("left_leg",0,0,40,0,1).wave("right_arm",0,0,35,0,1).wave("left_arm",0,0,-35,0,1)
            .hold("body",5,0,0).hold("head",-5,0,0))
        // Both arms up 120, then the slam.
        .put(new Clip("attack").rot("right_arm",0,0,0,0, .12f,-120,0,0, .25f,-15,0,0, .5f,0,0,0).rot("left_arm",0,0,0,0, .12f,-120,0,0, .25f,-15,0,0, .5f,0,0,0)
            .rot("body",0,0,0,0, .12f,-5,0,0, .25f,10,0,0, .5f,0,0,0))
        .put(new Clip("hit").rot("body",0,0,0,0, .08f,-12,0,0, .3f,0,0,0).rot("head",0,0,0,0, .08f,-6,0,0, .3f,0,0,0))
        // Sags, then falls forward flat about its feet, the arms out at its sides.
        .put(new Clip("death",true).rot("root",0,0,0,0, .2f,0,0,0, .45f,30,0,0, .65f,60,0,0, .85f,90,0,0, .9f,90,0,0)
            .pos("root",0,0,0,0, .2f,0,-1,0, .45f,0,-1.2f,-12, .65f,0,-8,-20.8f, .85f,0,-18,-24, .9f,0,-18,-24)
            .rot("right_arm",0,0,0,0, .2f,-10,0,0, .45f,-30,0,5, .85f,-6,0,12, .9f,-6,0,12)
            .rot("left_arm",0,0,0,0, .2f,-10,0,0, .45f,-30,0,-5, .85f,-6,0,-12, .9f,-6,0,-12)
            .rot("head",0,0,0,0, .2f,15,0,0, .65f,0,0,0, .85f,-35,0,0, .9f,-35,0,0)
            .rot("body",0,0,0,0, .2f,8,0,0, .45f,4,0,0, .85f,0,0,0, .9f,0,0,0))
        // Arms crossed at the chest, the right over the left.
        .put(new Clip("shield_up").rot("right_arm",0,0,0,0, .2f,-75,-50,0).rot("left_arm",0,0,0,0, .2f,-60,50,0))
        .put(new Clip("bow_draw").rot("right_arm",0,-90,0,0, .8f,-80,0,0, 1.2f,-90,0,0).hold("body",0,5,0))
        .put(new Clip("cast").hold("right_arm",-100,0,0).hold("left_arm",-100,0,0).wave("right_arm",1,0,10,0,1).wave("left_arm",1,0,-10,0,1))
        .put(new Clip("kneel").pos("root",0,0,0,0, .6f,0,-5,0).rot("right_leg",0,0,0,0, .6f,60,0,0).rot("left_leg",0,0,0,0, .6f,-45,0,0)
            .rot("body",0,0,0,0, .6f,10,0,0).rot("head",0,0,0,0, .6f,10,0,0).rot("right_arm",0,0,0,0, .6f,-10,0,0).rot("left_arm",0,0,0,0, .6f,-10,0,0))
        .put(new Clip("point").rot("right_arm",0,0,0,0, .4f,-90,0,0))
        .put(new Clip("sit").pos("root",0,0,0,0, .3f,0,-6,0, .6f,0,-12,0).rot("right_leg",0,0,0,0, .3f,-45,0,0, .6f,-90,6,0)
            .rot("left_leg",0,0,0,0, .3f,-45,0,0, .6f,-90,-6,0).rot("right_arm",0,0,0,0, .6f,-20,0,0).rot("left_arm",0,0,0,0, .6f,-20,0,0))
        .put(new Clip("raise").rot("right_arm",0,0,0,0, .5f,-170,0,0).rot("left_arm",0,0,0,0, .5f,-170,0,0).rot("head",0,0,0,0, .5f,-10,0,0))
        .put(new Clip("fall",true).rot("root",0,0,0,0, .2f,0,0,0, .4f,30,0,0, .6f,60,0,0, .8f,90,0,0)
            .pos("root",0,0,0,0, .2f,0,-1,0, .4f,0,-1.2f,-12, .6f,0,-8,-20.8f, .8f,0,-18,-24)
            .rot("right_arm",0,0,0,0, .4f,-30,0,5, .8f,-6,0,12).rot("left_arm",0,0,0,0, .4f,-30,0,-5, .8f,-6,0,-12)
            .rot("head",0,0,0,0, .2f,15,0,0, .8f,-35,0,0))
        .put(new Clip("stand").rot("root",0,90,0,0, .3f,45,0,0, .6f,15,0,0, .8f,0,0,0)
            .pos("root",0,0,-18,-24, .3f,0,-4,-17, .6f,0,0,-6.2f, .8f,0,0,0)
            .rot("right_arm",0,-6,0,12, .3f,-45,0,5, .6f,-20,0,0, .8f,0,0,0).rot("left_arm",0,-6,0,-12, .3f,-45,0,-5, .6f,-20,0,0, .8f,0,0,0)
            .rot("head",0,-35,0,0, .3f,-30,0,0, .6f,-10,0,0, .8f,0,0,0))
        .put(new Clip("brace").pos("root",0,0,-1.5f,0, 2,0,-1.5f,0).hold("right_leg",-10,0,0).hold("left_leg",10,0,0)
            .wave("body",0,8,1,0,1).hold("right_arm",-20,0,0).hold("left_arm",-20,0,0).hold("head",-8,0,0))
        .put(new Clip("hold").hold("right_arm",-12,0,0).hold("left_arm",-12,0,0).rot("body",0,0,0,0, 1,1,0,0, 2,0,0,0))
        .put(new Clip("fall_back").wave("right_leg",0,0,20,0,1).wave("left_leg",0,0,-20,0,1).hold("right_arm",-20,0,0).hold("left_arm",-20,0,0))
        .done();
}
