package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import net.minecraft.client.render.entity.animation.Animation;
import java.util.Map;

/**
 * The hound rig's 18 clips (wolves, polar bears, ravagers). body pivots at the hips and carries the chest, head, front legs
 * and tail; the back legs hang from the root. A positive leg pitch swings the foot back, a negative body pitch lifts the
 * front, and a positive jaw pitch opens the mouth. Lying on a side is a root roll about the outer edge of the feet.
 */
public final class HoundClips {
    private HoundClips() {}
    private static final String FL="front_left_leg",FR="front_right_leg",BL="back_left_leg",BR="back_right_leg";
    public static final Map<String,Animation> CLIPS=new TroopClips.Set18("hound")
        .put(new Clip("idle").wave("tail",1,0,10,0,2).rot("body",0,0,0,0, 1.5f,-1,0,0, 3,0,0,0).rot("head",0,0,0,0, 1.5f,1,0,0, 3,0,0,0))
        .put(new Clip("walk").wave(FL,0,0,25,0,1).wave(BR,0,0,25,0,1).wave(FR,0,0,-25,0,1).wave(BL,0,0,-25,0,1).wave("tail",1,0,6,0,1))
        // The bound: the front pair reaches as the back pair pushes, the body rocking.
        .put(new Clip("run").wave(FL,0,0,-40,0,1).wave(FR,0,0,-40,0,1).wave(BL,0,0,40,0,1).wave(BR,0,0,40,0,1)
            .wave("body",0,0,-5,0,1).wave("head",0,0,5,0,1))
        // The lunge: the chest 2 px forward and the jaw open, then the bite.
        .put(new Clip("attack").pos("chest",0,0,0,0, .15f,0,0,-2, .25f,0,0,-2, .5f,0,0,0)
            .rot("jaw",0,0,0,0, .15f,30,0,0, .25f,20,0,0, .35f,0,0,0, .5f,0,0,0)
            .rot("head",0,0,0,0, .15f,-10,0,0, .25f,10,0,0, .5f,0,0,0)
            .rot(BL,0,0,0,0, .15f,15,0,0, .5f,0,0,0).rot(BR,0,0,0,0, .15f,15,0,0, .5f,0,0,0))
        .put(new Clip("hit").rot("head",0,0,0,0, .08f,0,15,0, .3f,0,0,0).rot("body",0,0,0,0, .08f,-3,0,0, .3f,0,0,0))
        // Rolls onto its left side about the outer edge of its left feet (2.5, 24); a small bounce at 0.78 s.
        .put(new Clip("death",true).rot("root",0,0,0,0, .2f,0,0,0, .45f,0,0,45, .7f,0,0,90, .78f,0,0,85, .9f,0,0,90)
            .pos("root",0,0,0,0, .2f,0,-1,0, .45f,17.7f,-4.8f,0, .7f,26.5f,-20.5f,0, .78f,26.2f,-18.4f,0, .9f,26.5f,-20.5f,0)
            .rot(FL,0,0,0,0, .2f,-15,0,0, .7f,-20,0,0, .9f,-20,0,0).rot(FR,0,0,0,0, .2f,-15,0,0, .7f,-10,0,0, .9f,-10,0,0)
            .rot(BL,0,0,0,0, .2f,15,0,0, .7f,20,0,0, .9f,20,0,0).rot(BR,0,0,0,0, .2f,15,0,0, .7f,10,0,0, .9f,10,0,0)
            .rot("head",0,0,0,0, .7f,10,0,0, .9f,10,0,0).rot("jaw",0,0,0,0, .7f,12,0,0, .9f,12,0,0).rot("tail",0,0,0,0, .7f,-20,0,0, .9f,-20,0,0))
        .put(new Clip("shield_up").rot("head",0,0,0,0, .2f,20,0,0))
        // The pounce crouch: the chest 2 px down, the legs folded 20, held, then springing back.
        .put(new Clip("bow_draw").pos("chest",0,0,0,0, .3f,0,-2,0, .9f,0,-2,0, 1.2f,0,0,0)
            .rot(FL,0,0,0,0, .3f,-20,0,0, .9f,-20,0,0, 1.2f,0,0,0).rot(FR,0,0,0,0, .3f,-20,0,0, .9f,-20,0,0, 1.2f,0,0,0)
            .rot(BL,0,0,0,0, .3f,20,0,0, .9f,20,0,0, 1.2f,0,0,0).rot(BR,0,0,0,0, .3f,20,0,0, .9f,20,0,0, 1.2f,0,0,0))
        // The howl: head up 50, jaw open 30, both swaying 5.
        .put(new Clip("cast").wave("head",0,-50,5,0,1).wave("jaw",0,30,5,.25f,1))
        // Lies down: the body 4 px lower, the legs folded until the feet rest on the ground again.
        .put(new Clip("kneel").pos("root",0,0,0,0, .6f,0,-4,0).rot(FL,0,0,0,0, .6f,-60,0,0).rot(FR,0,0,0,0, .6f,-60,0,0)
            .rot(BL,0,0,0,0, .6f,60,0,0).rot(BR,0,0,0,0, .6f,60,0,0).rot("head",0,0,0,0, .6f,5,0,0))
        // The pointer: nose up and forward, the tail straight back, a forepaw lifted.
        .put(new Clip("point").rot("head",0,0,0,0, .4f,-15,0,0).pos("chest",0,0,0,0, .4f,0,0,-1)
            .rot("tail",0,0,0,0, .4f,50,0,0).rot(FR,0,0,0,0, .4f,-50,0,0))
        // The wolf sit: the hips down, the front up 35 on straight forelegs, the haunches folded under.
        .put(new Clip("sit").pos("root",0,0,0,0, .6f,0,-6.5f,0).rot("body",0,0,0,0, .6f,-35,0,0)
            .rot(FL,0,0,0,0, .6f,35,0,0).rot(FR,0,0,0,0, .6f,35,0,0).rot(BL,0,0,0,0, .6f,-80,0,0).rot(BR,0,0,0,0, .6f,-80,0,0)
            .rot("head",0,0,0,0, .6f,25,0,0).rot("tail",0,0,0,0, .6f,30,0,0))
        // Rears on the back legs (and the polar bear's warning).
        .put(new Clip("raise").rot("body",0,0,0,0, .5f,-45,0,0).rot(FL,0,0,0,0, .5f,10,0,0).rot(FR,0,0,0,0, .5f,10,0,0)
            .rot("head",0,0,0,0, .5f,30,0,0).rot("tail",0,0,0,0, .5f,20,0,0))
        // Lies on its right side (death's roll, the other way).
        .put(new Clip("fall",true).rot("root",0,0,0,0, .25f,0,0,0, .5f,0,0,-45, .8f,0,0,-90)
            .pos("root",0,0,0,0, .25f,0,-1,0, .5f,-17.7f,-4.8f,0, .8f,-26.5f,-20.5f,0)
            .rot(FL,0,0,0,0, .25f,-10,0,0, .8f,-15,0,0).rot(FR,0,0,0,0, .25f,-10,0,0, .8f,-15,0,0)
            .rot(BL,0,0,0,0, .25f,10,0,0, .8f,15,0,0).rot(BR,0,0,0,0, .25f,10,0,0, .8f,15,0,0).rot("head",0,0,0,0, .8f,10,0,0))
        .put(new Clip("stand").rot("root",0,0,0,-90, .3f,0,0,-45, .6f,0,0,-10, .8f,0,0,0)
            .pos("root",0,-26.5f,-20.5f,0, .3f,-17.7f,-4.8f,0, .6f,-4.2f,0,0, .8f,0,0,0)
            .rot(FL,0,-15,0,0, .6f,-10,0,0, .8f,0,0,0).rot(FR,0,-15,0,0, .6f,-10,0,0, .8f,0,0,0)
            .rot(BL,0,15,0,0, .6f,10,0,0, .8f,0,0,0).rot(BR,0,15,0,0, .6f,10,0,0, .8f,0,0,0).rot("head",0,10,0,0, .8f,0,0,0))
        // The crouch: legs folded 15, the head low.
        .put(new Clip("brace").pos("root",0,0,-1,0, 2,0,-1,0).hold(FL,-15,0,0).hold(FR,-15,0,0).hold(BL,15,0,0).hold(BR,15,0,0)
            .wave("head",0,10,1.5f,0,1))
        .put(new Clip("hold").wave("head",0,-12,1,0,1).rot("body",0,0,0,0, 1,-1,0,0, 2,0,0,0))
        .put(new Clip("fall_back").wave(FL,0,0,-20,0,1).wave(BR,0,0,-20,0,1).wave(FR,0,0,20,0,1).wave(BL,0,0,20,0,1).hold("head",8,0,0))
        .done();
}
