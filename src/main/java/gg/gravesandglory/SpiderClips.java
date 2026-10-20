package gg.gravesandglory;

import gg.gravesandglory.TroopClips.Clip;
import net.minecraft.client.render.entity.animation.Animation;
import java.util.Map;

/**
 * The spider rig's 18 clips. Each leg is left_leg_N or right_leg_N, 1 the front pair; at rest the legs splay down and out.
 * For a left leg a positive yaw swings it forward and a negative roll lifts it; a right leg is the mirror (negative yaw
 * forward, positive roll up). The gait moves alternating fours: L1, R2, L3 and R4, then R1, L2, R3 and L4.
 */
public final class SpiderClips {
    private SpiderClips() {}
    /** 1 for the left legs, -1 for the right: yaw times it swings a leg forward, roll times it lowers it. */
    private static int s(String side){return side.equals("left")?1:-1;}
    private static String leg(String side,int n){return side+"_leg_"+n;}
    /** Group A of the gait: L1, R2, L3 and R4. */
    private static boolean a(String side,int n){return (n%2==1)==side.equals("left");}
    /** The gait: each four swings forward lifted (yaw amp, lift up) while the other four push back on the ground; back walks the other way. */
    private static Clip gait(Clip c,float amp,float lift,boolean back){
        for(String side:new String[]{"left","right"})for(int n=1;n<=4;n++){
            int k=s(side);boolean first=a(side,n)!=back; // swings forward in the first half
            float f=first?amp:-amp; // the yaw at a quarter, in "forward" units
            float liftA=first!=back?-lift*k:0,liftB=first!=back?0:-lift*k;
            c.rot(leg(side,n),0,0,0,0, c.len/4,0,f*k,liftA, c.len/2,0,0,0, c.len*3/4,0,-f*k,liftB, c.len,0,0,0);
        }
        return c;
    }
    /** Every leg rolled up by up degrees (negative curls it down) and swung by fwd (legs 1 and 2) or back (legs 3 and 4). */
    private static Clip legs(Clip c,float t,float up,float fwd,float back){
        for(String side:new String[]{"left","right"})for(int n=1;n<=4;n++){int k=s(side);c.rot(leg(side,n),0,0,0,0, t,0,(n<=2?fwd:-back)*k,-up*k);}
        return c;
    }
    private static Clip legsHeld(Clip c,float up,float fwd,float back){
        for(String side:new String[]{"left","right"})for(int n=1;n<=4;n++){int k=s(side);c.hold(leg(side,n),0,(n<=2?fwd:-back)*k,-up*k);}
        return c;
    }
    private static Clip idle(){
        Clip c=new Clip("idle").bob("abdomen",1,0,-.3f,0,1);
        for(int n=1;n<=4;n++){c.wave(leg("left",n),2,0,3,n/8f,1);c.wave(leg("right",n),2,0,-3,n/8f+.5f,1);}
        return c;
    }
    /** The curl of the falls: the body down 5 px, every leg rolled up 60 over the body, the abdomen sagging. */
    private static Clip curl(String name,float end){
        Clip c=new Clip(name,true).pos("root",0,0,0,0, end/3,0,-1,0, end*2/3,0,-5,0, end,0,-5,0).rot("abdomen",0,0,0,0, end*2/3,10,0,0, end,10,0,0);
        for(String side:new String[]{"left","right"})for(int n=1;n<=4;n++){int k=s(side);
            c.rot(leg(side,n),0,0,0,0, end/3,0,(n%2==0?10:-10)*k,-20*k, end*2/3,0,0,-60*k, end,0,0,-60*k);}
        return c;
    }
    private static Clip front(Clip c,float t1,float t2,float up){
        for(int n=1;n<=2;n++){c.rot(leg("left",n),0,0,0,0, t1,0,0,-up, t2,0,0,-up, c.len,0,0,0);c.rot(leg("right",n),0,0,0,0, t1,0,0,up, t2,0,0,up, c.len,0,0,0);}
        return c;
    }
    private static Clip flinch(){
        Clip c=new Clip("hit");
        for(String side:new String[]{"left","right"})for(int n=1;n<=4;n++){int k=s(side);c.rot(leg(side,n),0,0,0,0, .08f,0,0,-15*k, .3f,0,0,0);}
        return c;
    }
    private static Clip raise(){
        Clip c=new Clip("raise");
        for(int n=1;n<=2;n++){c.rot(leg("left",n),0,0,0,0, .5f,0,0,-70);c.rot(leg("right",n),0,0,0,0, .5f,0,0,70);}
        return c;
    }
    private static Clip cast(){
        Clip c=new Clip("cast");
        for(int n=1;n<=2;n++){c.hold(leg("left",n),0,0,-40).wave(leg("left",n),1,0,15,n==1?0:.5f,1);c.hold(leg("right",n),0,0,40).wave(leg("right",n),1,0,-15,n==1?0:.5f,1);}
        return c;
    }
    private static Clip stand(){
        Clip c=new Clip("stand").pos("root",0,0,-5,0, .4f,0,-3,0, .8f,0,0,0).rot("abdomen",0,10,0,0, .4f,5,0,0, .8f,0,0,0);
        for(String side:new String[]{"left","right"})for(int n=1;n<=4;n++){int k=s(side);c.rot(leg(side,n),0,0,0,-60*k, .4f,0,0,-25*k, .8f,0,0,0);}
        return c;
    }
    public static final Map<String,Animation> CLIPS=new TroopClips.Set18("spider")
        .put(idle())
        .put(gait(new Clip("walk"),20,10,false))
        .put(gait(new Clip("run"),35,15,false).bob("root",1,0,.5f,0,2))
        // The front pair up 40, then the fangs strike.
        .put(new Clip("attack").rot("left_leg_1",0,0,0,0, .15f,0,0,-40, .3f,0,0,-40, .5f,0,0,0).rot("right_leg_1",0,0,0,0, .15f,0,0,40, .3f,0,0,40, .5f,0,0,0)
            .rot("fangs",0,0,0,0, .15f,0,0,0, .25f,20,0,0, .4f,0,0,0, .5f,0,0,0).rot("head",0,0,0,0, .15f,-10,0,0, .25f,10,0,0, .5f,0,0,0))
        // The flinch: every leg drawn up 15, the body sagging a pixel.
        .put(flinch().pos("root",0,0,0,0, .08f,0,-1,0, .3f,0,0,0).rot("abdomen",0,0,0,0, .08f,-5,0,0, .3f,0,0,0))
        .put(curl("death",.9f))
        .put(new Clip("shield_up").rot("left_leg_1",0,0,0,0, .2f,0,0,-30).rot("right_leg_1",0,0,0,0, .2f,0,0,30))
        // Rears: the front pairs up 50 and the body tipped back, held, then down again.
        .put(front(new Clip("bow_draw"),.3f,.9f,50).rot("thorax",0,0,0,0, .3f,-10,0,0, .9f,-10,0,0, 1.2f,0,0,0))
        .put(cast())
        // Settles: legs flattened 30 so the body sits 3 px lower.
        .put(legs(new Clip("kneel"),.6f,30,0,0).pos("root",0,0,0,0, .6f,0,-3,0))
        .put(new Clip("point").rot("left_leg_1",0,0,0,0, .4f,0,20,-60))
        // Splays: the front legs forward and the back legs back 30, flattened 20, the body 3 px down.
        .put(legs(new Clip("sit"),.6f,20,30,30).pos("root",0,0,0,0, .6f,0,-3,0))
        .put(raise().rot("thorax",0,0,0,0, .5f,-15,0,0))
        .put(curl("fall",.8f))
        .put(stand())
        // Crouched: the body 2 px down, the legs flattened and splayed 15, a slow sway.
        .put(legsHeld(new Clip("brace"),15,15,15).pos("root",0,0,-2,0, 2,0,-2,0).wave("thorax",2,0,1,0,1))
        .put(legsHeld(new Clip("hold"),0,12,12).bob("abdomen",1,0,-.3f,0,1))
        .put(gait(new Clip("fall_back"),20,10,true))
        .done();
}
