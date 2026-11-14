package gg.gravesandglory;

import java.util.List;

/**
 * Moving prop parts (docs/props/MOVING-PARTS.md). Pure: JDK only.
 *
 * <p>A part's pose is an item_display transformation in the entity's own frame: {@code translation} plus {@code left_rotation} (scale 1, right
 * rotation none), the rotation of the part about its pivot. The server sends one pose every {@link #KEY} ticks, the pose due {@code KEY} ticks
 * later, with {@code interpolation_duration=KEY} and {@code start_interpolation=0}, and the client tweens to it, so the part moves smoothly on
 * two packets a second.
 *
 * <p>Frames. The motion axis and pivot are in the prop frame at facing 0 (model px, x east, y up, z south, origin the anchor cell's min corner).
 * An item_display part stands at the anchor cell centre (plus its def offset) with yaw 180+facing, and the item renderer turns the model a half turn
 * about y, so model point P sits at entity-frame point {@code R(P)=((8-P'x)/16,(P'y-8)/16,(8-P'z)/16)} with {@code P'=P-offset}, at every facing
 * (the yaw carries the facing). A turn of angle a about prop axis A through pivot Q is then the entity-frame turn of a about {@code (-Ax,Ay,-Az)}
 * through {@code R(Q)}: rotation q and translation {@code R(Q)-q*R(Q)}.
 *
 * <p>Desync: each anchor gets a phase share and a period scale (0.92..1.08) from its {@link PropEffects#seed}, so a row of banners never sways in
 * step (entries that are the same motion on different parts share theirs, {@link #jitter}), and a keyframe slot (which of the two ticker runs in a KEY window it sends on), so the packets of many props spread over the ticks.
 */
public final class PropMotion {
    private PropMotion(){}
    /** Ticks between keyframes of one prop (two {@link PropEffects#STEP} ticker runs). */
    public static final int KEY=10;
    /** Motion plays while a player of the world is within this many blocks of the anchor cell (props are seen moving from battle distance). */
    public static final double RANGE=48.0;
    /** The rest pose: no translation, no rotation. */
    public static final float[] REST={0,0,0,0,0,0,1};

    /** Whether any part of {@code d} moves. */
    public static boolean moves(PropDef d){return !d.motion().isEmpty();}
    /** Whether part {@code p} of {@code d} moves. */
    public static boolean moves(PropDef d,PropDef.Part p){
        for(PropDef.Motion m:d.motion())if(m.part().equals(p.model()))return true;
        return false;
    }

    /** Whether the prop with this seed sends a keyframe on {@code tick} (a multiple of STEP): one of the KEY/STEP slots, chosen by the seed. */
    public static boolean keyframe(long tick,long seed){
        int slots=KEY/PropEffects.STEP;
        return Math.floorMod(tick/PropEffects.STEP+Long.remainderUnsigned(PropEffects.mix(seed^0x5DEECE66DL),slots),slots)==0;
    }

    /** The anchor's period scale, 0.92..1.08. */
    static double periodScale(long seed,int index){
        return 0.92+0.16*((PropEffects.mix(seed+0x632BE59BD9B4E019L*(index+1))>>>11)*0x1.0p-53);
    }
    /** The anchor's phase share, 0..1. */
    static double phaseShift(long seed,int index){
        return (PropEffects.mix(seed^(0xD1B54A32D192ED03L*(index+1)))>>>11)*0x1.0p-53;
    }

    /** The desync index of entry {@code i}: the first entry with the same motion (everything but the part). A piece split over several parts
     *  (a trebuchet arm) carries the same entry on each part, and they must turn in step to stay joined. */
    public static int jitter(List<PropDef.Motion> all,int i){
        PropDef.Motion m=all.get(i);
        for(int j=0;j<i;j++){
            PropDef.Motion o=all.get(j);
            if(o.kind().equals(m.kind())&&o.axis()==m.axis()&&o.sign()==m.sign()&&o.px()==m.px()&&o.py()==m.py()&&o.pz()==m.pz()
                &&o.amplitude()==m.amplitude()&&o.bias()==m.bias()&&o.period()==m.period()&&o.phase()==m.phase())return j;
        }
        return i;
    }

    /** The angle (degrees) of motion entry {@code index} at {@code tick} for the prop with this seed. */
    public static double angle(PropDef.Motion m,int index,double tick,long seed){
        double period=m.period()*(m.kind().equals("rotate")?1:periodScale(seed,index));
        double u=tick/period+m.phase()+phaseShift(seed,index);
        u-=Math.floor(u);
        return switch(m.kind()){
            case "rotate"->m.bias()+360*u;
            case "swing"->{ // ping-pong with an ease at both ends: -1 at u=0, +1 at u=.5, back to -1
                double v=u<.5?2*u:2-2*u;double s=v*v*(3-2*v);
                yield m.bias()+m.amplitude()*(2*s-1);
            }
            default->m.bias()+m.amplitude()*Math.sin(2*Math.PI*u);
        };
    }

    /** The pose of part {@code p} of {@code d} at {@code tick}: {tx,ty,tz, qx,qy,qz,qw} in the entity frame (blocks); {@link #REST} when it
     *  does not move. Entries on the part compose in def order (the first is outermost). */
    public static float[] pose(PropDef d,PropDef.Part p,double tick,long seed){
        double[] q={0,0,0,1},t={0,0,0};
        List<PropDef.Motion> all=d.motion();
        boolean any=false;
        for(int i=0;i<all.size();i++){
            PropDef.Motion m=all.get(i);
            if(!m.part().equals(p.model()))continue;
            any=true;
            double a=Math.toRadians(angle(m,jitter(all,i),tick,seed))*m.sign();
            double[] axis={0,0,0};axis[m.axis()]=1;
            axis[0]=-axis[0];axis[2]=-axis[2];                   // the item renderer's half turn about y
            double s=Math.sin(a/2);
            double[] qi={axis[0]*s,axis[1]*s,axis[2]*s,Math.cos(a/2)};
            double[] c=entityPoint(p,m.px(),m.py(),m.pz());
            double[] rc=rotate(qi,c);
            double[] ti={c[0]-rc[0],c[1]-rc[1],c[2]-rc[2]};
            // compose: this entry runs inside the ones before it: x -> Q(qi x + ti) + T
            double[] rt=rotate(q,ti);
            t=new double[]{t[0]+rt[0],t[1]+rt[1],t[2]+rt[2]};
            q=mul(q,qi);
        }
        if(!any)return REST.clone();
        return new float[]{(float)t[0],(float)t[1],(float)t[2],(float)q[0],(float)q[1],(float)q[2],(float)q[3]};
    }

    /** Model point (px, prop frame) of part p in its entity's frame (blocks). */
    static double[] entityPoint(PropDef.Part p,double x,double y,double z){
        return new double[]{(8-(x-p.ox()))/16.0,((y-p.oy())-8)/16.0,(8-(z-p.oz()))/16.0};
    }

    /** Where the pose puts model point (x,y,z) of part p, in the entity frame (blocks); for checks. */
    public static double[] apply(float[] pose,PropDef.Part p,double x,double y,double z){
        double[] c=entityPoint(p,x,y,z);
        double[] r=rotate(new double[]{pose[3],pose[4],pose[5],pose[6]},c);
        return new double[]{r[0]+pose[0],r[1]+pose[1],r[2]+pose[2]};
    }

    static double[] mul(double[] a,double[] b){
        return new double[]{
            a[3]*b[0]+a[0]*b[3]+a[1]*b[2]-a[2]*b[1],
            a[3]*b[1]-a[0]*b[2]+a[1]*b[3]+a[2]*b[0],
            a[3]*b[2]+a[0]*b[1]-a[1]*b[0]+a[2]*b[3],
            a[3]*b[3]-a[0]*b[0]-a[1]*b[1]-a[2]*b[2]};
    }
    static double[] rotate(double[] q,double[] v){
        double[] r=mul(mul(q,new double[]{v[0],v[1],v[2],0}),new double[]{-q[0],-q[1],-q[2],q[3]});
        return new double[]{r[0],r[1],r[2]};
    }
}
