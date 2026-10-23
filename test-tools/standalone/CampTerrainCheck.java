package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The camp's ground as the grade of 2026-10-23 asked for it (Phase 3 Task 3, fix round 4) and its re-grade 4 asked for it (fix round 5: no one-block terraces beside the stairs, rock and soil in patches, the rim either side of the gatehouse a slope framed by spruce, a flush parade), checked on the pure shape of the island (CampTerrain), the sheets (Spec) and the parade's script, with no game:
 *  the parade field is a mix of grounds with a ragged edge, not one flat slab in a hard rectangle; the welcome label is in the frame of the arrival view; the ridge bank is a hillside, not full-width one-block terraces;
 *  the edges of the bowl and the stands of spruce are irregular, not ruled lines and rows; the south rim beside the gates and the high corners are slopes, not stepped walls of stone.
 *  It also guards what the grade passed and what other pieces stand on: the ground under the two ridge stairs is the profile their script was cut to, the yard and the drillmaster stand on the floor of the bowl, the Graveborn graveyard lies at the floor between the stairs with the bank beside it where its walls were cut (Task 4),
 *  the rim seam, the bank mix and the flank relief of the camp suite still hold. Run: javac with Spec, CampTerrain and this file; java gg.gravesandglory.CampTerrainCheck <parade.mcfunction>. */
public final class CampTerrainCheck {
    static int count,failed;
    static void check(boolean pass,String text){count++;if(!pass){failed++;System.out.println("FAIL "+text);}else System.out.println("ok   "+text);}
    static final Spec.CampRow C=Spec.CAMP[0];

    public static void main(String[] args) throws Exception{
        parade(Path.of(args.length>0?args[0]:"test-tools/resources/data/gravesandglory_studio/functions/camp/parade.mcfunction"));
        welcome();
        terrain();
        System.out.println(failed==0?"PASS "+count+" of "+count+" camp ground scenarios":"FAILED "+failed+" of "+count+" camp ground scenarios");
        if(failed>0)System.exit(1);
    }

    // ---- the parade (its script: the y 0 layer of camp/parade) ----
    static final Set<String> PAVING=Set.of("stone_bricks","cracked_stone_bricks","mossy_stone_bricks","cobblestone","mossy_cobblestone","polished_andesite","chiseled_stone_bricks","polished_blackstone");
    static void parade(Path script) throws Exception{
        int W=41,D=33;String[][] g=new String[W][D];
        Pattern set=Pattern.compile("^setblock ~(-?\\d+) ~(-?\\d+) ~(-?\\d+) minecraft:([a-z_]+)"),fill=Pattern.compile("^fill ~(-?\\d+) ~(-?\\d+) ~(-?\\d+) ~(-?\\d+) ~(-?\\d+) ~(-?\\d+) minecraft:([a-z_]+)");
        for(String line:Files.readAllLines(script)){
            Matcher m=set.matcher(line);
            if(m.find()){int x=Integer.parseInt(m.group(1)),y=Integer.parseInt(m.group(2)),z=Integer.parseInt(m.group(3));if(y==0)g[x][z]=m.group(4);continue;}
            m=fill.matcher(line);
            if(m.find()){int x0=Integer.parseInt(m.group(1)),y0=Integer.parseInt(m.group(2)),z0=Integer.parseInt(m.group(3)),x1=Integer.parseInt(m.group(4)),z1=Integer.parseInt(m.group(6));
                if(y0==0)for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)g[x][z]=m.group(7);}
        }
        var kinds=new TreeMap<String,Integer>();int field=0;
        for(int x=0;x<W;x++)for(int z=0;z<D;z++)if(g[x][z]!=null&&!PAVING.contains(g[x][z])){kinds.merge(g[x][z],1,Integer::sum);field++;}
        int top=0;String which="-";for(var e:kinds.entrySet())if(e.getValue()>top){top=e.getValue();which=e.getKey();}
        check(field>0&&top*100<=60*field&&kinds.size()>=4,"parade field is a mix of grounds: "+kinds.size()+" kinds over "+field+" cells, the commonest ("+which+") "+(field==0?0:100*top/field)+"% (want at least 4 kinds, none above 60%): "+kinds);
        // the edge: on the west, north and east sides, how deep from the side the first set cell of each row lies (the ways that run to a side count as set), and how much of the outer band of three is left to the lawn
        int[] sides={0,1,2};String[] names={"west","north","east"};
        for(int s:sides){
            var depths=new TreeSet<Integer>();int band=0,voids=0;
            int n=s==1?W:D;
            for(int r=0;r<n;r++){
                int d=0;
                while(d<6){int x=s==0?d:s==2?W-1-d:r,z=s==1?d:r;if(g[x][z]!=null)break;d++;}
                depths.add(d);
                for(int e=0;e<3;e++){int x=s==0?e:s==2?W-1-e:r,z=s==1?e:r;band++;if(g[x][z]==null)voids++;}
            }
            check(depths.size()>=3&&voids*100>=20*band,"parade "+names[s]+" edge is ragged: depths of the first ground cell "+depths+" (want 3 or more different), lawn in the outer three rows "+(100*voids/band)+"% (want at least 20%)");
        }
        boolean ways=true;for(int z=13;z<=15;z++)ways&=g[0][z]!=null&&PAVING.contains(g[0][z])&&g[W-1][z]!=null&&PAVING.contains(g[W-1][z]);
        for(int x=17;x<=23;x++)ways&=g[x][0]!=null&&g[x][D-1]!=null;
        for(int x=2;x<=38;x++)ways&=g[x][D-1]!=null; // the south side meets the paving of the War Gates: no lawn between them
        check(ways,"the cross way reaches both sides, the avenue both ends, and the south side meets the gates' paving with no gap");
        // fix round 5: a dirt path is 15/16 of a block, so its patches sat sunken in the field with black edge lines
        int low=0;for(int x=0;x<W;x++)for(int z=0;z<D;z++)if("dirt_path".equals(g[x][z])||"farmland".equals(g[x][z]))low++;
        check(low==0,"the parade's ground layer is flush: "+low+" cells of dirt path or farmland (want 0)");
    }

    // ---- the welcome label in the arrival view ----
    static void welcome(){
        Spec.CampStationsRow w=null;for(var s:Spec.CAMPSTATIONS)if(s.id().equals("welcome"))w=s;
        double eye=C.spawnY()+1.62,d=Math.hypot(w.x()-C.spawnDx(),w.z()-C.spawnDz()),bottom=w.y()+3.7,topY=bottom+1.0; // two lines of text at scale 1.7 and the panel: about one block
        double up=Math.toDegrees(Math.atan2(topY-eye,d)),low=Math.toDegrees(Math.atan2(bottom-eye,d)),frame=35-C.spawnPitch(); // vertical field of view 70: the top of the frame is 35 degrees above the look, the spawn looks spawnPitch down
        // the parade standard: the banner (local (20,5,2) of the parade, 2 blocks tall) seen from the spawn
        double sx=0.5,sz=8+2-16+0.5,standardTop=Math.toDegrees(Math.atan2(C.floorY()+5+2-eye,Math.hypot(sx-C.spawnDx(),sz-C.spawnDz())));
        check(up<=frame-2,String.format(Locale.ROOT,"the welcome label is in the arrival frame: its top %.1f degrees above the eye at %.1f blocks, the frame's top at %.1f (spawn pitch %.0f; want 2 degrees to spare)",up,d,frame,C.spawnPitch()));
        check(low>=standardTop,String.format(Locale.ROOT,"the welcome label stands above the parade standard in the arrival view: its foot at %.1f degrees, the banner's top at %.1f",low,standardTop));
    }

    // ---- the terrain ----
    static boolean isRock(int kind){return kind==CampTerrain.STONE||kind==CampTerrain.ANDESITE;}
    static int[][] boxes;static boolean[] flat;static int[] floor;
    static int width,depth,last;static int[] ground,K;
    static int g(int dx,int dz){return ground[(dx-C.islandMinDx())*depth+(dz-C.islandMinDz())];}
    static boolean nearPiece(int dx,int dz,int m){int x=C.originX()+dx,z=C.originZ()+dz;for(var b:boxes)if(x>=b[0]-m&&x<=b[1]+m&&z>=b[2]-m&&z<=b[3]+m)return true;return false;}
    static void terrain(){
        var rows=Spec.CAMPPIECES;boxes=new int[rows.length][];flat=new boolean[rows.length];floor=new int[rows.length];
        for(int r=0;r<rows.length;r++){
            Spec.PiecesRow p=null;for(var q:Spec.PIECES)if(q.id().equals(rows[r].template()))p=q;
            int sx=rows[r].rotation()%180==0?p.width():p.depth(),sz=rows[r].rotation()%180==0?p.depth():p.width();
            int x0=C.originX()+rows[r].dx()-(sx-1)/2,z0=C.originZ()+rows[r].dz()-(sz-1)/2;
            boxes[r]=new int[]{x0,x0+sx-1,z0,z0+sz-1};flat[r]=rows[r].flatten();floor[r]=rows[r].y();
        }
        width=C.islandMaxDx()-C.islandMinDx()+1;last=CampTerrain.rimEnd(C);depth=last-C.islandMinDz()+1;
        ground=CampTerrain.ground(C,boxes,flat,floor);K=CampTerrain.tops(ground,width,depth); // the top kinds as CampWorld lays them
        int fy=C.floorY(),rise=C.plateauY()-C.floorY();

        // guards: the stairs' ground, the people on the floor, the seam
        int off=0,above=0;String first="";
        for(var row:rows)if(row.template().equals("camp/ridge_stair"))for(int dx=row.dx()-6;dx<=row.dx()+6;dx++)for(int z=0;z<13;z++){
            int dz=row.dz()-6+z,want=fy+(int)Math.floor(12*(12-z)/13.0+0.5),h=CampTerrain.height(C,dx,dz);
            if(Math.abs(dx-row.dx())<=3){if(h!=want){off++;if(first.isEmpty())first="dx "+dx+" dz "+dz+": "+h+" not "+want;}}
            else if(h>want||h<fy){above++;if(first.isEmpty())first="cheek dx "+dx+" dz "+dz+": "+h+" against "+want;}}
        check(off==0&&above==0,"the ground of both ridge stairs: under the walls and the flight the bank their script was cut to (local y floor(12(12-z)/13+.5)), "+off+" columns off; on the cheeks outside the walls at or under it and not under the floor, "+above+" off"+(first.isEmpty()?"":", first "+first));
        int notFloor=0;String nf="";
        for(int dx=-60;dx<=-36;dx++)for(int dz=-12;dz<=24;dz++)if(CampTerrain.height(C,dx,dz)!=fy){notFloor++;if(nf.isEmpty())nf="dx "+dx+" dz "+dz+" at "+CampTerrain.height(C,dx,dz);}
        check(notFloor==0,"the yard (dx -60..-36, dz -12..24: lessons, dummies, the drillmaster) stands on the floor of the bowl: "+notFloor+" columns off"+(nf.isEmpty()?"":", first "+nf));
        // Phase 3 Task 4: the recruiter is gone; the Graveborn graveyard (camp/graveyard, 23 x 23, centre dx 0 dz -39) fills the notch between the stairs. Its footprint is laid at the floor, and the bank outside its two side walls
        // (dx -12 and 12, the inner columns of the stairs, local z 0..7 of the graveyard) stands where its script cut the walls: at or under BANK[z] above the floor and at most one under it, so the walls meet the hillside
        int[] bank={10,8,6,5,4,3,2,1};int gyOff=0,seam=0;String gf="",sf="";
        boolean gyRow=false;for(var r:rows)if(r.id().equals("graveborn")&&r.template().equals("camp/graveyard")&&r.dx()==0&&r.dz()==-39&&r.flatten())gyRow=true;
        for(int dx=-11;dx<=11;dx++)for(int dz=-50;dz<=-28;dz++)if(g(dx,dz)!=fy){gyOff++;if(gf.isEmpty())gf="dx "+dx+" dz "+dz+" at "+g(dx,dz);}
        for(int side:new int[]{-12,12})for(int z=0;z<8;z++){int h=g(side,-50+z)-fy;if(h>bank[z]||h<bank[z]-1){seam++;if(sf.isEmpty())sf="dx "+side+" dz "+(-50+z)+" at +"+h+" (wall cut to +"+bank[z]+")";}}
        check(gyRow&&gyOff==0&&seam==0,"the graveyard row (graveborn, camp/graveyard at dx 0 dz -39, flatten) is in the sheet ("+gyRow+"), its footprint (dx -11..11, dz -50..-28) lies at the floor ("+gyOff+" columns off"+(gf.isEmpty()?"":", first "+gf)+"), and the bank beside its side walls is the one its script was cut to ("+seam+" off"+(sf.isEmpty()?"":", first "+sf)+")");
        int steps=0,worst=0;for(int dx=C.islandMinDx();dx<C.islandMaxDx();dx++){int d=Math.abs(CampTerrain.rimTop(C,dx)-CampTerrain.rimTop(C,dx+1));worst=Math.max(worst,d);if(d>3)steps++;}
        check(steps==0&&CampTerrain.rimTop(C,0)==fy+CampTerrain.RIM_RISE,"the crest of the rim (the row the arena's margin meets) steps at most 3 between columns (largest "+worst+") and stands at "+(fy+CampTerrain.RIM_RISE)+" behind the gates (it is "+CampTerrain.rimTop(C,0)+")");
        int zsteps=0;for(int dx=C.islandMinDx();dx<=C.islandMaxDx();dx++)for(int dz=last-3;dz<last;dz++)if(Math.abs(g(dx,dz)-g(dx,dz+1))>3)zsteps++;
        check(zsteps==0,"the last rim rows (z -270 to -267) step at most 3 along z: "+zsteps+" steps over 3");

        // the camp suite's bank mix and flank relief, on the same rules
        int mixed=0;var shares=new StringBuilder();
        for(int dz=CampTerrain.RIDGE_TOP_DZ+1;dz<CampTerrain.RIDGE_FOOT_DZ;dz++){
            int[] n=new int[8];int all=0;
            for(int dx=C.minDx()+2;dx<=C.maxDx()-2;dx++){if(nearPiece(dx,dz,1))continue;int i=dx-C.islandMinDx(),k=dz-C.islandMinDz(),h=ground[i*depth+k];n[K[i*depth+k]]++;all++;}
            int top=Arrays.stream(n).max().getAsInt();if(all>0&&top*100<=85*all)mixed++;shares.append(dz).append(':').append(all==0?0:100*top/all).append("% ");
        }
        check(mixed>=9,"p3-camp-bank-mix on the pure terrain: "+mixed+" of 12 bank rows mixed (want 9): "+shares);
        boolean relief=true;var rel=new StringBuilder();
        for(int side:new int[]{-1,1}){int lo=999,hi=-999;for(int dz=-36;dz<=30;dz++){int y=g(side*90,dz);lo=Math.min(lo,y);hi=Math.max(hi,y);}if(hi-lo<6)relief=false;rel.append(side<0?"west ":"east ").append(hi-lo).append(' ');}
        check(relief,"p3-camp-flank-relief on the pure terrain: the crest at x -90 and 90 ranges "+rel+"(want 6 or more)");

        // defect 4: the ridge bank is a hillside, not full-width one-block terraces
        int levels=0,varied=0;var contour=new StringBuilder();
        for(int y=fy+2;y<=fy+rise-2;y++){
            var at=new TreeSet<Integer>();
            for(int dx=-58;dx<=58;dx++){
                boolean nearStair=false;for(var row:rows)if(row.template().equals("camp/ridge_stair")&&Math.abs(dx-row.dx())<=12)nearStair=true;
                if(nearStair||Math.abs(dx)<=3)continue;
                for(int dz=-30;dz>=-60;dz--)if(CampTerrain.height(C,dx,dz)>=y){at.add(dz);break;}
            }
            levels++;if(at.size()>=4&&at.last()-at.first()>=3)varied++;contour.append(y).append(':').append(at.isEmpty()?"-":at.first()+".."+at.last()).append(' ');
        }
        check(varied*10>=levels*8,"the bank's contours wander: at "+varied+" of "+levels+" heights the row where the bank reaches that height varies by 3 rows or more along x (want 80%): "+contour);
        int pairs=0,same=0;
        for(int dz=-50;dz<=-39;dz++)for(int dx=-58;dx<58;dx++){
            boolean nearStair=false;for(var row:rows)if(row.template().equals("camp/ridge_stair")&&(Math.abs(dx-row.dx())<=12||Math.abs(dx+1-row.dx())<=12))nearStair=true;
            if(nearStair)continue;pairs++;if(CampTerrain.height(C,dx,dz)==CampTerrain.height(C,dx+1,dz))same++;
        }
        check(pairs>0&&same*100<=70*pairs,"no full-width terraces on the bank: "+(pairs==0?0:100*same/pairs)+"% of neighbouring columns along a bank row stand at one height (want 70% or less)");
        // fix round 5 (re-grade 4, item 1): no column of the bank climbs as a stair of one-block steps, row after row. A terrace row is a step of exactly 1 whose row before also stepped exactly 1; beside the stairs (the cheeks and the
        // nine columns beyond them) and over the whole bank (|dx| up to 60, all but the stairs' walls and flights), at most 35% of the rising steps may be such rows
        for(int pass=0;pass<2;pass++){
            int rising=0,terrace=0;
            for(int dx=-60;dx<=60;dx++){
                boolean core=false,beside=false;for(var row:rows)if(row.template().equals("camp/ridge_stair")){int d=Math.abs(dx-row.dx());if(d<=3)core=true;else if(d<=15)beside=true;}
                if(core||(pass==0&&!beside))continue;
                int prev=-1;
                for(int dz=-30;dz>-60;dz--){int s=CampTerrain.height(C,dx,dz-1)-CampTerrain.height(C,dx,dz);
                    if(CampTerrain.height(C,dx,dz)>=C.plateauY())break;
                    if(s>0||prev>0){rising++;if(s==1&&prev==1)terrace++;}
                    prev=s;}
            }
            check(rising>0&&terrace*100<=35*rising,(pass==0?"the bank beside the stairs (the cheeks and the columns up to 15 from a stair's middle)":"the whole bank (|dx| up to 60, but the stairs' walls and flights)")+" is a hillside, not a stack of one-block terraces: "+terrace+" of "+rising+" rising rows ("+(rising==0?0:100*terrace/rising)+"%) step by 1 after a step of 1 (want 35% or less)");
        }
        // fix round 5 (re-grade 4, item 2): rock and soil lie in patches, not as single cells: over the lifted ground, few top blocks differ from all four neighbours, and few cells of rock stand among soil or of soil among rock
        int lifted=0,alone=0,rockCells=0,speck=0;
        int[][] kind=new int[width][depth];
        for(int i=0;i<width;i++)for(int k=0;k<depth;k++)kind[i][k]=K[i*depth+k];
        for(int i=1;i<width-1;i++)for(int k=1;k<depth-1;k++){
            if(ground[i*depth+k]<fy+2)continue;lifted++;int me=kind[i][k];
            int[] nb={kind[i-1][k],kind[i+1][k],kind[i][k-1],kind[i][k+1]};
            boolean diff=true;int otherRock=0;for(int v:nb){if(v==me)diff=false;if(isRock(v)!=isRock(me))otherRock++;}
            if(diff)alone++;if(isRock(me))rockCells++;if(otherRock>=3)speck++;
        }
        check(lifted>0&&alone*100<=3*lifted,"the top blocks of the slopes lie in patches: "+alone+" of "+lifted+" lifted columns ("+(lifted==0?0:100.0*alone/lifted)+"%) differ from all four neighbours (want 3% or less)");
        check(rockCells>0&&speck*100<=4*rockCells,"rock is not checkered with soil: "+speck+" columns (against "+rockCells+" of rock) are rock among soil or soil among rock on three or four sides (want 4% or less)");
        int plat=0,worn=0;
        for(int dx=-30;dx<=30;dx++)for(int dz=-72;dz<=-44;dz++){if(nearPiece(dx,dz,2)||g(dx,dz)!=C.plateauY())continue;plat++;int t=kind[dx-C.islandMinDx()][dz-C.islandMinDz()];if(t==CampTerrain.COARSE||t==CampTerrain.ROOTED||t==CampTerrain.DIRT)worn++;}
        check(plat>0&&worn*100<=plat,"the plateau's lawn has no squares of coarse dirt: "+worn+" of "+plat+" plateau-high columns (dx -30..30, dz -72..-44, the lip of the bank too) show worn ground (want 1% or less)");

        // defect 5: the edges of the bowl are not ruled lines
        for(int side:new int[]{-1,1}){
            var at=new TreeSet<Integer>();
            for(int dz=-36;dz<=22;dz++)for(int a=40;a<=96;a++){if(CampTerrain.height(C,side*a,dz)>fy){at.add(a);break;}}
            check(at.size()>=4&&at.last()-at.first()>=4,"the "+(side<0?"west":"east")+" edge of the bowl wanders: where the lawn first rises, |dx| "+at+" (want 4 or more different, over 4 or more blocks)");
        }
        // defect 5: the spruce stand in clumps and clearings, not in rows: no one step to the nearest tree is shared by many trees
        var trees=new ArrayList<int[]>();
        for(int i=0;i<width;i++)for(int k=0;k<depth;k++){int dx=C.islandMinDx()+i,dz=C.islandMinDz()+k;if(CampTerrain.tree(C,dx,dz,ground[i*depth+k],CampTerrain.steepness(ground,width,depth,i,k))!=null)trees.add(new int[]{dx,dz});}
        var stepsOf=new HashMap<String,Integer>();
        for(var t:trees){int best=Integer.MAX_VALUE;String key="";for(var u:trees){if(u==t)continue;int d=(u[0]-t[0])*(u[0]-t[0])+(u[1]-t[1])*(u[1]-t[1]);if(d<best){best=d;key=Math.abs(u[0]-t[0])+","+Math.abs(u[1]-t[1]);}}stepsOf.merge(key,1,Integer::sum);}
        int common=0;String ck="";for(var e:stepsOf.entrySet())if(e.getValue()>common){common=e.getValue();ck=e.getKey();}
        check(trees.size()>=250&&common*100<=20*trees.size(),"the spruce are not in rows: "+trees.size()+" trees, the commonest step to the nearest tree ("+ck+") is shared by "+(trees.isEmpty()?0:100*common/trees.size())+"% (want 20% or less, and 250 trees or more)");
        int onFloor=0;for(var t:trees)if(CampTerrain.height(C,t[0],t[1])<=fy&&CampTerrain.inBowl(C,t[0],t[1]))onFloor++;
        int nearP=0;for(var t:trees){int r=CampTerrain.tree(C,t[0],t[1],g(t[0],t[1]),CampTerrain.steepness(ground,width,depth,t[0]-C.islandMinDx(),t[1]-C.islandMinDz()))[1]/2;
            boolean bad=nearPiece(t[0],t[1],2);for(int a=-r;a<=r;a++)for(int b=-r;b<=r;b++)if(nearPiece(t[0]+a,t[1]+b,1))bad=true;if(bad)nearP++;}
        check(onFloor==0&&nearP==0,"no spruce on the lawn of the bowl ("+onFloor+"), and none with its trunk within 2 of a piece or its crown within 1 ("+nearP+")");
        // fix round 5 (re-grade 4, item 2): spruce frame the War Gates, on the slopes either side of the gatehouse
        int west=0,east=0;for(var t:trees)if(t[1]>=22&&Math.abs(t[0])>=19&&Math.abs(t[0])<=36){if(t[0]<0)west++;else east++;}
        check(west>=3&&east>=3,"spruce frame the War Gates: "+west+" west and "+east+" east of the gatehouse (|dx| 19..36, dz 22 on; want 3 or more on each side)");

        // defect 6: the south rim beside the gates is a slope, not a stepped wall
        int big=0,all=0,maxStep=0;var feet=new TreeSet<Integer>();
        for(int a=28;a<=90;a++)for(int side:new int[]{-1,1}){
            int dx=side*a;boolean found=false;
            for(int dz=14;dz<last;dz++){int d=g(dx,dz+1)-g(dx,dz);if(dz>=20){all++;if(Math.abs(d)>=3)big++;maxStep=Math.max(maxStep,Math.abs(d));}
                if(!found&&a<=58&&CampTerrain.height(C,dx,dz)>fy){feet.add(dz);found=true;}}
        }
        check(big*100<=2*all,"the south rim away from the gates (|dx| 28 to 90) climbs as a slope: "+big+" of "+all+" steps along z are 3 or more (largest "+maxStep+"; want 2% or less)");
        check(feet.size()>=3&&feet.first()<=30,"the foot of the south rim beside the gates wanders and starts well before the island's edge: first rows "+feet+" (want 3 or more different, from dz 30 or nearer)");
        // fix round 5 (re-grade 4, item 3): either side of the gatehouse (|dx| 21 to 45) the rim is a slope too, its foot brought forward beside the gatehouse
        int gbig=0,gall=0,gmax=0;var gfeet=new TreeSet<Integer>();
        for(int a=21;a<=45;a++)for(int side:new int[]{-1,1}){
            int dx=side*a;boolean found=false;
            for(int dz=14;dz<last;dz++){int d=g(dx,dz+1)-g(dx,dz);if(dz>=20){gall++;if(Math.abs(d)>=3)gbig++;gmax=Math.max(gmax,Math.abs(d));}
                if(!found&&a<=30&&g(dx,dz)>fy){gfeet.add(dz);found=true;}}
        }
        check(gbig*100<=2*gall&&!gfeet.isEmpty()&&gfeet.last()<=30,"the south rim either side of the gatehouse (|dx| 21 to 45) climbs as a slope: "+gbig+" of "+gall+" steps along z are 3 or more (largest "+gmax+"; want 2% or less), its foot beside the gatehouse (|dx| 21 to 30) at rows "+gfeet+" (want all by dz 30)");
        // defect 6: the high corners are not grey stepped cones
        int high=0,stone=0,peak=0;
        for(int i=0;i<width;i++)for(int k=0;k<depth;k++){int h=ground[i*depth+k];peak=Math.max(peak,h);if(h>=80){high++;if(K[i*depth+k]==CampTerrain.STONE)stone++;}}
        check(peak<=106&&(high==0||stone*100<=45*high),"the high ground is not bare stone cones: highest column y "+peak+" (want 106 or lower), stone on top of "+(high==0?0:100*stone/high)+"% of the "+high+" columns at y 80 or above (want 45% or less)");
    }
}
