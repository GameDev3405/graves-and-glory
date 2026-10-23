package gg.gravesandglory;

/** The shape of the camp island, with no Minecraft in it, so that a standalone check can read it (CampTerrainCheck): the height of every column, the ground the pieces are laid on, which top and under block a column gets, and where a spruce stands.
 *  CampWorld writes what this computes. The island is a bowl (the floor, y floorY) with a ridge bank up to the plateau (plateauY) at the north, a slope behind the plateau, the flanks east and west, and the south rim; every slope has a foot that
 *  wanders, a profile that is not one plane, and bumps of its own, so that no slope reads as a stack of full-width one-block terraces and no edge as a ruled line (the grade of 2026-10-23, fix round 4). The places other pieces stand on are kept as they were:
 *  the columns of the two ridge stairs (their script was cut to the old bank), the floor of the bowl where the yard, the drillmaster and the recruiter stand, and the rim behind the War Gates (the crest the arena's margin meets there). */
final class CampTerrain {
    private CampTerrain(){}
    /** Part of the camp's hash (CampWorld.hash) and of the arena margin's revision (LayoutMarker.MARGIN): a change to the way the terrain is shaped or written makes every world rebuild the camp, and every map its margin, once. */
    static final String REVISION="terrain-6";
    /** Where the plateau begins and where its bank starts at the foot (dz, from the origin of the camp), beside the stairs: the bank rises over the 13 rows between them. Away from the stairs the foot and the top wander (bank). */
    static final int RIDGE_TOP_DZ=-51,RIDGE_FOOT_DZ=-38;
    /** The southern rim behind the War Gates: from RIM_FOOT_DZ (the end of the apron of the gates) the ground climbs RIM_SLOPE blocks per block, to RIM_RISE above the floor, at the last row of the rim (rimEnd). It is the camp's half of the southern seam (CampWorld.taper).
     *  Beyond the gatehouse's apron (|dx| from GATE_SIDE) the rim starts at RIM_LONG_FOOT_DZ (GATE_FOOT_DZ beside the gatehouse) instead and climbs as a long slope to a crest that wanders around the same height. */
    static final int RIM_FOOT_DZ=37,RIM_RISE=15,RIM_LONG_FOOT_DZ=25;
    /** Beside the War Gates: the last |dx| of the gatehouse's apron (the piece is 37 wide, its apron 2 more), the row the rim's foot comes forward to there, and how fast the ground rises from the apron outward (blocks a block). */
    static final int GATE_SIDE=20,GATE_FOOT_DZ=23;
    static final double RIM_SLOPE=2.5,SIDE_RISE=2.0;
    /** The kinds of block a column's top or under block can be (CampWorld turns them into blocks): the flat block (grass), coarse dirt, stone, dirt. */
    static final int FLAT=0,COARSE=1,STONE=2,DIRT=3,ANDESITE=4,ROOTED=5;

    /** The last row (dz) the terrain writes: the south edge of the island, or the row before the first column of the arena when that is further south. */
    static int rimEnd(Spec.CampRow c){return Math.max(c.islandMaxDz(),-Spec.RULES[0].worldHalfZ()-1-c.originZ());}
    static boolean inBowl(Spec.CampRow c,int dx,int dz){return dx>=c.minDx()&&dx<=c.maxDx()&&dz>=c.minDz()&&dz<=c.maxDz();}

    // ---- noise ----
    /** A number from 0 to 1 that depends on the column and on the seed alone, with no pattern along either axis. */
    static double hash01(int x,int z,int seed){
        int h=x*374761393+z*668265263+seed*362437;h=(h^(h>>>13))*1274126177;h^=h>>>16;
        return (h&0xffff)/65536.0;
    }
    /** Smooth value noise, 0 to 1: hash01 on a lattice scale blocks apart, eased between the lattice points. */
    static double noise(double x,double z,double scale,int seed){
        double gx=x/scale,gz=z/scale;int ix=(int)Math.floor(gx),iz=(int)Math.floor(gz);double fx=gx-ix,fz=gz-iz;fx=fx*fx*(3-2*fx);fz=fz*fz*(3-2*fz);
        double a=hash01(ix,iz,seed),b=hash01(ix+1,iz,seed),c=hash01(ix,iz+1,seed),d=hash01(ix+1,iz+1,seed);
        return (a*(1-fx)+b*fx)*(1-fz)+(c*(1-fx)+d*fx)*fz;
    }
    /** -1 to 1: three octaves of noise (the scale, about half and about a quarter of it). */
    static double fbm(double x,double z,double scale,int seed){
        return 0.6*(2*noise(x,z,scale,seed)-1)+0.3*(2*noise(x,z,scale/2.1,seed+11)-1)+0.1*(2*noise(x,z,scale/4.3,seed+23)-1);
    }
    static double clamp01(double t){return t<0?0:t>1?1:t;}
    static double smooth(double t){t=clamp01(t);return t*t*(3-2*t);}

    // ---- the height ----
    /** The height of the island at (dx,dz) from the origin of the camp, before the pieces: the floor, the bank to the plateau, and the lifts of the back slope, the flanks and the rim (which meet as hills, not as a sum: softUnion), with their bumps,
     *  the spurs of the flanks, and outside the bowl a low swell. */
    static double heightRaw(Spec.CampRow c,int dx,int dz){
        double rise=c.plateauY()-c.floorY();
        double h=c.floorY()+rise*bank(c,dx,dz)+softUnion(back(c,dx,dz),flankLift(c,dx,dz),rimLift(c,dx,dz));
        double out=Math.max(Math.max(Math.abs(dx)-c.maxDx(),c.minDz()-dz),dz-c.maxDz());
        return h+smooth(out/6.0)*2.2*fbm(dx,dz,11,81)+flank(c,dx,dz)+1.8*outcrop(c,dx,dz);
    }
    static int height(Spec.CampRow c,int dx,int dz){return (int)Math.round(heightRaw(c,dx,dz));}
    /** Two or three slopes that meet: the highest, and 0.4 of the others (a sum stacked the flank on the back slope into cones over 115). */
    static double softUnion(double a,double b,double r){double m=Math.max(a,Math.max(b,r));return m+0.4*(a+b+r-m);}
    /** A knob of rock standing out of a slope, 0 to 1 (heightRaw raises the ground by up to 1.8 there and top lays stone or andesite on it): where a noise of about 6 blocks is high, on ground that is lifted 2 or more above the floor (the bank, the slopes),
     *  not on the plateau inside the bowl, not beside the ridge stairs, and nowhere on the floor of the bowl. */
    static double outcrop(Spec.CampRow c,int dx,int dz){
        double rise=c.plateauY()-c.floorY(),b=bank(c,dx,dz);
        if(b>=1&&inBowl(c,dx,dz)&&Math.abs(dx)<=edge(c,dz,dx<0?-1:1))return 0; // the plateau
        double lift=rise*b+softUnion(back(c,dx,dz),flankLift(c,dx,dz),rimLift(c,dx,dz));
        if(lift<1||dz>rimEnd(c)-3)return 0; // and not on the last rows of the rim, which the arena's margin meets
        double n=noise(dx,dz,6,101)+0.25*(noise(dx,dz,2.5,102)-0.5);
        return smooth((lift-1)/3.0)*stairs(dx)*clamp01((n-0.66)/0.2);
    }

    /** 0 on the columns of a ridge stair and the one beyond each side, 1 from three further out: where the bank may leave the profile the stair's script was cut to. */
    static double stairs(int dx){
        double w=1;
        for(var row:Spec.CAMPPIECES)if(row.template().equals("camp/ridge_stair")){
            int half=0;for(var p:Spec.PIECES)if(p.id().equals(row.template()))half=p.width()/2;
            w=Math.min(w,smooth((Math.abs(dx-row.dx())-(half+1))/3.0));
        }
        return w;
    }
    /** 0 on the core columns of a ridge stair (its two retaining walls and the flight between them, 3 either side of its middle), 1 from 5 out: the cheeks of a stair (the void columns of its piece outside the walls) may lie
     *  lower than the profile its script was cut to (the walls stand from the floor up, so a lower hillside shows more of them), never higher (that would bury the coping). */
    static double cheek(int dx){
        double w=1;
        for(var row:Spec.CAMPPIECES)if(row.template().equals("camp/ridge_stair"))w=Math.min(w,smooth((Math.abs(dx-row.dx())-3)/2.0));
        return w;
    }
    /** 0 where a station of the floor stands just before the bank (the recruiter: dz from RIDGE_FOOT_DZ to 8 after it) and 3 columns round it, 1 from 4 further out: the foot of the bank does not come down over him. */
    static double clearOfFoot(int dx){
        double w=1;
        for(var s:Spec.CAMPSTATIONS)if(s.z()>=RIDGE_FOOT_DZ&&s.z()<=RIDGE_FOOT_DZ+8)w=Math.min(w,smooth((Math.abs(dx+0.5-s.x())-3)/4.0));
        return w;
    }
    /** The share of the rise to the plateau at (dx,dz), 0 on the floor to 1 on the plateau. On the core columns of the stairs (cheek 0) it is the straight bank of 13 rows their script was cut to. Away from the stairs and the recruiter the foot comes 3.5 to 8 rows
     *  further south and, from |dx| 26 out, the top 1.5 to 4.5 rows further north (both wander along x), so the hill is longer and gentler, on the outer stretches (|dx| from 30) spurs run up to 9 rows further out into the bowl, and the profile is a hillside: gentle grass slopes at the foot and the top and a crag that
     *  takes most of the rise in a few rows (its place, height and sharpness wander along x), with spurs and gullies from a noise along both axes. Beside the stairs (stairs 0, cheek 1: the cheeks and the three columns beyond) the profile
     *  bends below the straight bank, a gentle grass foot under a scarp at the plateau's lip, so the stair's walls stand out of the hillside; it blends into the crag profile over three columns. Nowhere a stack of one-block terraces (fix round 5). */
    static double bank(Spec.CampRow c,int dx,int dz){
        double w=stairs(dx),wd=cheek(dx),keep=clearOfFoot(dx);
        double outer=smooth((Math.abs(dx)-30)/6.0),spur=outer*9*clamp01((noise(dx,0,12,40)-0.35)/0.65); // on the outer stretches the bank sends spurs up to 9 rows out into the bowl
        double foot=RIDGE_FOOT_DZ+w*keep*(3.5+4.5*noise(dx,0,9,41))+spur, // away from the stairs' feet and the recruiter the foot comes 3.5 to 8 rows forward: a longer, gentler hill
               top=RIDGE_TOP_DZ-w*smooth((Math.abs(dx)-26)/4.0)*(1.5+3.0*noise(dx,0,11,43)); // and from |dx| 26 out (clear of the stairs, the walk along the plateau's lip and the tent) the top goes 1.5 to 4.5 rows back into the plateau
        double lean=w*Math.max(keep,smooth((foot-dz-1.5)/2.5))*3.0*fbm(dx,dz*0.3,5,49); // the rows lean: a contour runs a row or two north or south of its neighbour a few columns on (behind the recruiter only from the third row up)
        double z=dz-lean;
        if(z>=foot)return 0;if(z<=top)return 1;
        double t=(foot-z)/(foot-top);
        // a band of crag: most of the rise in a few rows (centre and sharpness wander along x), gentle grass slopes below and above it. Between the stairs (mid, |dx| up to 7, gone by 10) the crag is always there and sharper, a rock face behind the recruiter over a gentle foot
        double mid=1-smooth((Math.abs(dx)-7)/3.0);
        double centre=0.25+0.55*noise(dx,0,10,44),sharp=0.04+0.05*noise(dx,0,8,46),share=0.3+0.35*noise(dx,0,14,48); // share is the gentle part of the profile (spread over the rows), the rest the crag; the crag's place weaves up and down the hill
        centre+=mid*(0.5+0.1*noise(dx,dz,4,60)-centre);sharp*=1-0.35*mid;share*=1-0.9*mid;
        double s0=1/(1+Math.exp(centre/sharp)),s1=1/(1+Math.exp(-(1-centre)/sharp)),crag=(1/(1+Math.exp(-(t-centre)/sharp))-s0)/(s1-s0);
        double away=share*t+(1-share)*crag+Math.sin(Math.PI*t)*(0.16*fbm(dx,dz,6,45)+0.06*(2*noise(dx,0,13,47)-1));
        double p=2.4+0.6*noise(dx,0,7,62),near=Math.min(t,Math.pow(t,p)+Math.sin(Math.PI*t)*0.06*fbm(dx,dz,5,63)); // beside the stairs: under the straight bank, gentle low down, a scarp under the lip
        double shaped=near+w*(away-near);
        return clamp01(t+wd*(shaped-t));
    }
    /** The slope behind the plateau (dz before minDz): its foot wanders by 4 rows along x, it rises 13 to 19 blocks over 28 rows with an eased foot and top. */
    static double back(Spec.CampRow c,int dx,int dz){
        double d=c.minDz()-dz+4*(2*noise(dx,0,17,51)-1);
        if(d<=0)return 0;
        return (15+3*(2*noise(dx,0,23,53)-1))*smooth(d/28.0);
    }
    /** Where the lawn of the bowl ends on one side (|dx|), at row dz: maxDx pushed out by up to 5.5 blocks, and pulled in by up to 4.5 where no piece of the floor stands near the side (north of dz -19 and south of dz 31; the yard and the archive
     *  stand between), so the side is no straight line. */
    static double edge(Spec.CampRow c,int dz,int side){
        double out=1+6*noise(dz,side*50,11,57)+2.5*noise(dz,side*50,4,58),in=5*noise(dz,side*50,13,59);
        double free=1-Math.min(smooth((dz+19)/6.0),smooth((31-dz)/6.0));
        return c.maxDx()+out-free*in;
    }
    /** The flank on one side: from the wandering edge of the bowl an eased rise of 17 to 24 blocks over 24 to 36 blocks (both wander along z), with knolls and hollows of up to about 3 blocks that grow in over the first 8 blocks. */
    static double flankLift(Spec.CampRow c,int dx,int dz){
        int side=dx<0?-1:1;double d=Math.abs(dx)-edge(c,dz,side);
        if(d<=0)return 0;
        double high=18+3.5*(2*noise(dz,side*100,21,61)-1),run=30+6*(2*noise(dz,side*100,17,63)-1);
        return Math.max(0,high*smooth(d/run)+smooth(d/8.0)*2.8*fbm(dx,dz,8,65));
    }
    /** The rim at (dx,dz). Behind the gatehouse (|dx| up to GATE_SIDE) it is as it was: RIM_SLOPE a block from RIM_FOOT_DZ to RIM_RISE (the gatehouse hides it, and the arena's margin meets that crest). Beyond it is a long slope:
     *  the foot at RIM_LONG_FOOT_DZ to 3 rows after it (brought forward to GATE_FOOT_DZ beside the gatehouse, up to |dx| 26, fading by 34), a climb of about one block a row (half straight, half eased) to a crest of RIM_RISE give or take 2.2 at rimEnd,
     *  with knolls of up to about 2.4 on the way; near the gatehouse it is held under a ramp that rises SIDE_RISE a block from the apron outward, so the gates stand in a notch of the hill, not against a stepped wall (fix round 5). Never under the old line behind the gates. */
    static double rimLift(Spec.CampRow c,int dx,int dz){
        double a=Math.abs(dx);
        int end=rimEnd(c);
        if(a<=GATE_SIDE)return Math.min(RIM_RISE,Math.max(0,RIM_SLOPE*(dz-RIM_FOOT_DZ)));
        // beside the gatehouse the old line behind it (the back of the notch the gates stand in) bends into the slope over 4 columns
        double lin=Math.min(RIM_RISE,Math.max(0,RIM_SLOPE*(dz-RIM_FOOT_DZ)))*(1-smooth((a-GATE_SIDE)/4.0));
        // the slope's foot comes forward to the row of the pads beside the gatehouse (GATE_FOOT_DZ, it wanders by 2), and beside the apron (its rows, to RIM_FOOT_DZ) the ground is held under a ramp that rises SIDE_RISE a block from the apron outward, which lets go over the three rows behind it
        double near=1-smooth((a-GATE_SIDE-6)/8.0);
        double foot=RIM_LONG_FOOT_DZ-2+5*noise(dx,0,12,71)+3*noise(dx,0,5,72),crest=RIM_RISE+2.2*(2*noise(dx,0,19,73)-1);
        foot+=near*(GATE_FOOT_DZ+2*noise(dx,0,6,76)-foot);
        double t=(dz-foot)/(end-foot),wide=t<=0?0:crest*(0.5*clamp01(t)+0.5*smooth(t))+(t<1?Math.sin(Math.PI*t)*2.4*fbm(dx,dz,7,75):0);
        double side=SIDE_RISE*(a-GATE_SIDE-0.5)+1.5*noise(dx,dz,5,77)+2.0*Math.max(0,dz-RIM_FOOT_DZ);
        double g=Math.max(0,wide),k=2,hk=Math.max(k-Math.abs(g-side),0)/k,m=Math.min(g,side)-hk*hk*k*0.25; // the lower of the two (rounded where they meet)
        return Math.max(lin,Math.max(0,m));
    }
    /** The spurs and gullies of the flanks along their length (three waves; the west side has its own phase): nothing at the wall of the bowl, full from 22 blocks out, fading over the six rows before the rim. At most 5.1 up or down. */
    static double flank(Spec.CampRow c,int dx,int dz){
        double f=Math.min(1,Math.max(0,(Math.abs(dx)-c.maxDx())/22.0)),g=Math.min(1,Math.max(0,(RIM_FOOT_DZ-dz)/6.0));
        if(f==0||g==0)return 0;
        double a=Math.abs(dx),phase=dx<0?1.7:0;
        return f*g*(2.3*Math.sin(dz*.13+a*.05+1.3+phase)+1.2*Math.sin(dz*.27-a*.08+.4-phase)+1.6*Math.sin(dz*.07+phase*2));
    }
    /** The crest of the rim at dx: the height of its last row, the one the arena's margin has to meet. */
    static int rimTop(Spec.CampRow c,int dx){return height(c,Math.max(c.islandMinDx(),Math.min(c.islandMaxDx(),dx)),rimEnd(c));}

    // ---- the ground the pieces are laid on ----
    /** The ground of every column of the island box (index i*depth+k, i from islandMinDx, k from islandMinDz to rimEnd): the height, with the rows of campPieces laid over it: a flatten row's footprint takes its floor y, a flatten=false
     *  footprint keeps the ground, and a cell within two of a flatten row's footprint takes the highest such floor. boxes holds per row {minX, maxX, minZ, maxZ} in world coordinates. */
    static int[] ground(Spec.CampRow c,int[][] boxes,boolean[] flatten,int[] floorY){
        int width=c.islandMaxDx()-c.islandMinDx()+1,last=rimEnd(c),depth=last-c.islandMinDz()+1;int[] ground=new int[width*depth];
        for(int i=0;i<width;i++)for(int k=0;k<depth;k++){
            int dx=c.islandMinDx()+i,dz=c.islandMinDz()+k,x=c.originX()+dx,z=c.originZ()+dz,h=height(c,dx,dz);
            Integer flat=null,apron=null;boolean inside=false;
            for(int r=0;r<boxes.length;r++){
                int[] b=boxes[r];
                if(x>=b[0]&&x<=b[1]&&z>=b[2]&&z<=b[3]){inside=true;if(flatten[r])flat=flat==null?floorY[r]:Math.max(flat,floorY[r]);}
                else if(flatten[r]&&x>=b[0]-2&&x<=b[1]+2&&z>=b[2]-2&&z<=b[3]+2)apron=apron==null?floorY[r]:Math.max(apron,floorY[r]);
            }
            ground[i*depth+k]=flat!=null?flat:inside?h:apron!=null?apron:h;
        }
        return ground;
    }

    // ---- the blocks ----
    /** How steep the ground is at column (i,k) of the ground array, 0 to 1: see slopeRock; the height differences are those of the columns two away along z and along x, scaled to four blocks. */
    static double steepness(int[] ground,int width,int depth,int i,int k){
        int z0=Math.max(0,k-2),z1=Math.min(depth-1,k+2),x0=Math.max(0,i-2),x1=Math.min(width-1,i+2);
        double dz=z1>z0?Math.abs(ground[i*depth+z1]-ground[i*depth+z0])*4.0/(z1-z0):0,dx=x1>x0?Math.abs(ground[x1*depth+k]-ground[x0*depth+k])*4.0/(x1-x0):0;
        return slopeRock(Math.max(dz,dx));
    }
    /** The camp's steepness for its blocks, 0 to 1: grass up to 2.2 over four blocks (0.55 a block), all rock from 5.8 (1.45 a block). A one-block-a-row slope is about half grass: the hillside shows green tops between its faces; the crags are rock. */
    static double slopeRock(double perStride){return Math.min(1,Math.max(0,(perStride-2.2)/3.6));}
    /** The 0 to 1 steepness (the arena's margin reads this one) of a slope that climbs perStride blocks over four blocks: 0 up to 1.5 (0.375 a block), 1 from 4.5 (1.125 a block), between them in a line. */
    static double rockOf(double perStride){return Math.min(1,Math.max(0,(perStride-1.5)/3.0));}
    /** Patches of about six blocks, 0 to 1: two waves across each other. They are what makes the loose blocks of a slope lie in groups. */
    static double patch(int x,int z){return 0.5+0.25*(Math.sin(x*.43+z*.29)+Math.sin(x*.27-z*.51));}
    /** The block of a slope: the flat block where the ground is level; where it is steep, rock (0 to 1) is the share of the columns, taken in patches and by hash, that show stone or coarse dirt instead, the steeper the more of them stone. */
    static int mix(double rock,int x,int z){
        if(rock<=0||0.55*hash01(x,z,1)+0.45*patch(x,z)>=rock)return FLAT;
        return hash01(x,z,2)<rock*rock*0.55?STONE:COARSE;
    }
    // The camp's own palette (fix round 5): rock, soil and worn ground lie in patches of a few blocks set by smooth noise, never by a hash per column, so a crag is not checkered with single cells of dirt and a lawn has no squares.
    // (mix and under above stay as they were: the arena's margin lays them.)
    /** 0 to 1, smooth over about five blocks: how stony the ground of a column is. Rock shows where the column's rockiness passes 0.3 + 0.6 of it. */
    static double rockPatch(int x,int z){return 0.8*noise(x,z,5,201)+0.2*noise(x,z,2,202);}
    /** The rock of a crag at (x,y,z): stone, with lenses of andesite of about six blocks that dip across the slope. */
    static int rockKind(int x,int y,int z){return noise(x+0.6*y,z-0.4*y,6,203)>0.62?ANDESITE:STONE;}
    /** The rockiness of a camp column, 0 to 1: its steepness, rising with height from y 84 (all rock from 100), and 0.9 on an outcrop. */
    static double rockiness(int h,double rock,int x,int z){
        var c=Spec.CAMP[0];
        int dx=x-c.originX(),dz=z-c.originZ();
        double r=Math.max(rock,clamp01((h-84)/16.0));
        if(notchBack(dx,dz))r*=0.62; // the back of the gatehouse's notch is steep by need (six rows to the crest the margin meets): grass and rock there, not bare rock
        return outcrop(c,dx,dz)>0.15?Math.max(r,0.9):r;
    }
    /** Behind the War Gates' apron (|dx| up to GATE_SIDE+2, from RIM_FOOT_DZ): the back of the notch the gatehouse stands in. */
    static boolean notchBack(int dx,int dz){return Math.abs(dx)<=GATE_SIDE+2&&dz>=RIM_FOOT_DZ;}
    /** The top block of a camp column of height h: rock (rockKind) where its rockiness passes the patch's threshold, a fringe of coarse dirt where it just reaches it, and on the bank's grass (not the plateau's lawn) worn patches of coarse and rooted dirt
     *  of about five blocks where a noise is high; else the flat block. The high ground is forest and rock, not bare stone from y 80. */
    static int top(int h,double rock,int x,int z){
        var c=Spec.CAMP[0];
        int dx=x-c.originX(),dz=z-c.originZ();
        double r=rockiness(h,rock,x,z),thr=0.3+0.6*rockPatch(x,z);
        if(r>thr+0.07)return rockKind(x,h,z);
        if(r>thr)return COARSE;
        double b=bank(c,dx,dz);
        if(b>0.03&&b<0.97&&noise(x,z,4.5,205)+0.25*noise(x,z,1.8,206)>0.82)return noise(x,z,7,207)<0.5?COARSE:ROOTED;
        return FLAT;
    }
    /** The top kinds of every column of the ground array (index i*depth+k): top() of each, then one pass that takes away the specks: a column of rock with soil on three or four sides, or of soil with rock on three or four sides,
     *  takes the commonest kind of its four neighbours (read from the first pass, so the order does not matter). CampWorld lays these. */
    static int[] tops(int[] ground,int width,int depth){
        var c=Spec.CAMP[0];int[] first=new int[width*depth];
        for(int i=0;i<width;i++)for(int k=0;k<depth;k++)first[i*depth+k]=top(ground[i*depth+k],steepness(ground,width,depth,i,k),c.originX()+c.islandMinDx()+i,c.originZ()+c.islandMinDz()+k);
        int[] out=first.clone();
        for(int i=1;i<width-1;i++)for(int k=1;k<depth-1;k++){
            int me=first[i*depth+k];int[] nb={first[(i-1)*depth+k],first[(i+1)*depth+k],first[i*depth+k-1],first[i*depth+k+1]};
            int other=0;for(int v:nb)if(isRock(v)!=isRock(me))other++;
            if(other<3)continue;
            int best=nb[0],most=0;for(int v:nb){int n=0;for(int u:nb)if(u==v)n++;if(n>most){most=n;best=v;}}
            out[i*depth+k]=best;
        }
        return out;
    }
    static boolean isRock(int kind){return kind==STONE||kind==ANDESITE;}
    /** The block under the top block of a camp column (the face seen between two rows): under rock the same rock, under soil and grass dirt (and stone where the column is steep and its patch is stony). */
    static int underAt(int top,int h,double rock,int x,int z){
        if(top==STONE||top==ANDESITE)return rockKind(x,h-1,z);
        double r=rockiness(h,rock,x,z),thr=0.3+0.6*rockPatch(x,z);
        return r>thr-0.15&&r>0.3?rockKind(x,h-1,z):DIRT;
    }
    /** The block under the top block: dirt on the flat, and on steep ground (it is the face seen between two rows) dirt, coarse dirt or stone by hash. */
    static int under(double rock,int x,int z){
        if(rock<0.25)return DIRT;
        double p=hash01(x,z,3);
        return p<rock*0.35?STONE:p<0.6?COARSE:DIRT;
    }

    // ---- the spruce ----
    /** The spruce at (dx,dz) of ground h and steepness rock: {height, width} or null. The island is cut into cells of 5 by 5; a cell holds at most one tree, at a place in it set by hash (0 to 3 on each axis, so that no two trunks touch
     *  and no two cells repeat), with a chance that follows a noise of about 14 blocks (stands and clearings). Only on the slopes (the flanks beyond the edge of the bowl, the back slope, the rim, and thinly the bank from |dx| 28 out): not on the lawn, the plateau or the bank between the stairs, the trunk 3 or more from a piece and the crown 2 or more (crownNearPiece; beside and behind the gatehouse
     *  the chance is high, so spruce frame the gates),
     *  not on rock (steepness above 0.85), not above y 97, and the crown kept out of the arena's columns. Height 7 to 12 by hash; the small ones are 3 wide, the tall ones 7. */
    static int[] tree(Spec.CampRow c,int dx,int dz,int h,double rock){
        int cx=Math.floorDiv(dx,5),cz=Math.floorDiv(dz,5);
        if(dx!=cx*5+(int)(hash01(cx,cz,91)*4)||dz!=cz*5+(int)(hash01(cx,cz,92)*4))return null;
        double stand=noise(dx,dz,14,93),chance=stand<0.32?0.06:Math.min(0.95,0.3+(stand-0.32)*1.6);
        if(dz>RIM_LONG_FOOT_DZ)chance=Math.min(0.95,chance+0.3); // the rim is forest: it is the backdrop of the parade and the gates
        if(dz>=GATE_FOOT_DZ&&Math.abs(dx)<=GATE_SIDE+16)chance=0.95; // and the gatehouse stands framed by spruce on the slopes either side of it and behind it
        if(hash01(cx,cz,94)>=chance)return null;
        if((rock>0.85&&!notchBack(dx,dz))||h>=97||h<=c.floorY()+1)return null; // (behind the gatehouse spruce stand on the steep back of its notch too)
        boolean slope=!inBowl(c,dx,dz)||Math.abs(dx)>edge(c,dz,dx<0?-1:1)+2||dz>RIM_LONG_FOOT_DZ+2||dz>=GATE_FOOT_DZ; // the flanks, the back slope, the rim: not the lawn or the plateau
        double b=bank(c,dx,dz);
        boolean outerBank=Math.abs(dx)>=28&&b>0.1&&b<0.95&&hash01(cx,cz,96)<0.55; // and, more thinly, the outer stretches of the bank
        if(!slope&&!outerBank)return null;
        int height=7+(int)(hash01(dx,dz,95)*6),width=height<=8?3:height>=11?7:5;
        if(nearPiece(c,dx,dz,2)||crownNearPiece(c,dx,dz,width/2))return null;
        if(dz+width/2>rimEnd(c)-1)return null;
        return new int[]{height,width};
    }
    /** A crown of radius r round (dx,dz) comes within one block of a piece's footprint: the trunks stand 3 or more from a piece, and no leaf grows into or against one (a tree may stand close beside the gatehouse, its crown clear of it). */
    static boolean crownNearPiece(Spec.CampRow c,int dx,int dz,int r){
        for(int a=-r;a<=r;a++)for(int b=-r;b<=r;b++)if(nearPiece(c,dx+a,dz+b,1))return true;
        return false;
    }
    /** Within margin blocks of the footprint of a campPieces row (the centre block at dx,dz of the row, the size of its pieces row, turned by its rotation). */
    static boolean nearPiece(Spec.CampRow c,int dx,int dz,int margin){
        for(var row:Spec.CAMPPIECES){
            int sx=0,sz=0;for(var p:Spec.PIECES)if(p.id().equals(row.template())){sx=row.rotation()%180==0?p.width():p.depth();sz=row.rotation()%180==0?p.depth():p.width();}
            int x0=row.dx()-(sx-1)/2,z0=row.dz()-(sz-1)/2;
            if(dx>=x0-margin&&dx<=x0+sx-1+margin&&dz>=z0-margin&&dz<=z0+sz-1+margin)return true;
        }
        return false;
    }
    /** The plant on a column of ground h whose top is kind: 0 none, 1 grass, 2 fern. Only on grass, on the slopes and the bank (not on the lawn of the bowl or the plateau, which the House ring and the yard will dress, and not within 2 of a piece),
     *  below y 97: grass on about one column in seven, ferns on one in thirty, both thicker in the patches of a noise. */
    static int plant(Spec.CampRow c,int dx,int dz,int h,int kind){
        if(kind!=FLAT||h>=97||nearPiece(c,dx,dz,2))return 0;
        if(inBowl(c,dx,dz)&&(h==c.floorY()||h==c.plateauY()))return 0;
        double p=hash01(dx,dz,97),thick=0.6+0.8*noise(dx,dz,9,98);
        return p<0.035*thick?2:p<0.15*thick?1:0;
    }
}
