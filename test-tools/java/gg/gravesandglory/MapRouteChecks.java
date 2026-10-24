package gg.gravesandglory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.util.*;

/**
 * Opt-in disposable-lab tour: twenty actual pathfinding mobs over every crossing of the chosen maps.
 * It sets up its own conditions, so a result describes the route and nothing else: a round with no AI commander,
 * one army of twenty knights, and troops placed only after the round intro is over. Whatever came before
 * (the suite's solo battle, the previous map) is sent back to camp first, so no later camp return undoes a placement.
 * Maps come from control.json {"action":"routes","maps":[0,1]}; with no list all five maps 0-4 are toured.
 * Kingsfield (map 4) used to be left out: switching to it from a running client threw in GravesAndGloryClient.map (its HUD indexed the
 * three-entry objective arrays with five points); that was fixed in 6ecbfb0. crossed counts the 20/20 crossings per map (three when a map passes).
 * House mode (Phase 3 Task 8b, the three-argument constructor): the goals of a map are the yards of its supply houses, in sheet order, each from two starts
 * (house-<id>-base from the spawn of the faction whose half holds the house, house-<id>-point from 12 blocks out of the point nearest the yard); a goal passes when all
 * twenty knights stand within 10 blocks of the yard centre on the level of the house, within 3600 ticks. reached counts the passes per map (twice the house rows when a map passes).
 */
final class MapRouteChecks {
    final MinecraftClient client;final int[] maps;final Map<Integer,Integer> crossed=new HashMap<>();
    /** House mode: the passes per map, and the line of each goal (id-base 20/20, ...) for the verdict of the suite. */
    /** GRAVES_ROUTE_TRACE=1: every ten seconds of a house goal, where the knights that are not at the yard stand. GRAVES_HOUSE_ONLY=<house id>[,<id>]: only the goals of those houses (a debugging run: the suite's goal count then fails). */
    static final boolean TRACE=System.getenv("GRAVES_ROUTE_TRACE")!=null;static final String ONLY=System.getenv("GRAVES_HOUSE_ONLY");
    final boolean houses;final Map<Integer,Integer> reached=new HashMap<>();final List<String> lines=new ArrayList<>();
    /** House mode: a yard to reach (the goal), from where (sx, sz) the knights start. */
    record Goal(Spec.SupplyHousesRow house,String kind,double sx,double sz){}
    /** Phase 3 Task 9 fix 1: the pocket of the Hollowwood chapel ruin (map 3) where a march of the supply suite wedged under load: the cell (81,102) of the south-east corner, walls to the east (x 82), north (z 101) and south (z 104, x 80..82). Two goals of the house hw-supply-se start from it, with every other knight already in the yard:
     *  nook-even puts the slots 2 and 6 in it (the two found there), nook-odd the slots 3 and 7 (the control: the same cell, the other parity of the sidestep of Tactics.walk). Each has 1200 ticks, not 3600: a wedged knight does not leave.
     *  Fix 2: nook-far is nook-even with the order to a point 200 blocks past the pocket on the line to the yard (the other eighteen stand at that point): a goal that far has no whole path in the 2560 cells of the detour, so only the detour toward a cell 80 blocks on (Wedge.REACH) frees the two; they pass when both are 40 blocks out of the pocket.
     *  The red of these goals: GRAVES_NO_DETOUR=1 (the same build with the detour off: nook-even, nook-odd and nook-far fail 18/20), or the build of f54c49c for nook-far (nook-even and nook-odd pass there). A goal also fails, at once, when the pocket is not there: the walls north, east and south of its cell must have a collision shape at the height of the knights' feet, or the goals would pass with no pocket. */
    static final String NOOK_HOUSE="hw-supply-se";static final double[][] NOOK={{81.7,102.5},{81.1,102.5}};static final int[] NOOK_EVEN={2,6},NOOK_ODD={3,7};static final double NOOK_Y=65.0;
    static final int[][] NOOK_WALLS={{81,101},{82,102},{81,104}};static final double NOOK_FAR_BLOCKS=200,NOOK_OUT_BLOCKS=40;
    static int nookGoals(int map){return map==3?3:0;}
    static int[] nookSlots(String kind){return kind.equals("nook-even")||kind.equals("nook-far")?NOOK_EVEN:kind.equals("nook-odd")?NOOK_ODD:null;}
    /** The pocket of the nook goals is still there: the cell of the knights is open and the three walls have a collision shape at the height of their feet. Null when it is, else what is missing. */
    static String pocketMissing(GravesAndGlory.Battle b){
        var w=b.world;
        var cell=new BlockPos((int)Math.floor(NOOK[0][0]),(int)NOOK_Y,(int)Math.floor(NOOK[0][1]));
        if(!w.getBlockState(cell).getCollisionShape(w,cell).isEmpty())return "pocket gone: its cell "+cell.toShortString()+" is no longer open";
        for(int[] c:NOOK_WALLS){var bp=new BlockPos(c[0],(int)NOOK_Y,c[1]);if(w.getBlockState(bp).getCollisionShape(w,bp).isEmpty())return "pocket gone: no wall at "+bp.toShortString();}
        return null;
    }
    String pocket;
    List<Goal> goals=List.of();
    int index,bridge,phase,retries,crossings;long at;boolean all=true,done;
    Campaign.Profile backup;GravesAndGlory.Squad squad;boolean practice,fill;int size;
    MapRouteChecks(MinecraftClient c,int[] requested){this(c,requested,false);}
    MapRouteChecks(MinecraftClient c,int[] requested,boolean houses){
        client=c;this.houses=houses;
        maps=requested!=null&&requested.length>0?requested:new int[]{0,1,2,3,4};
    }
    static List<Spec.StructuresRow> bridges(){return Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();}
    void tick(){
        var b=GravesAndGlory.battle;if(b==null||done||client.player==null)return;
        ServerPlayerEntity p=b.server.getPlayerManager().getPlayer(client.player.getUuid());if(p==null)return;
        int map=maps[index];
        if(phase==0){
            // Camp return first: it clears the running round, every troop and any AI commander, and it moves the player.
            if(backup==null){backup=Campaign.JSON.fromJson(Campaign.JSON.toJson(b.campaign.profile(p.getUuid())),Campaign.Profile.class);practice=b.campaign.practice;fill=b.campaign.fill;size=b.campaign.size;}
            b.campaign.returnLobby();
            // Not practice, no bot fill: prepareRound then adds no AI commander to either team.
            b.campaign.practice=false;b.campaign.fill=false;b.campaign.size=1;
            if(b.campaign.map!=map)b.campaign.switchMap(map);
            phase=1;return;
        }
        if(phase==1){
            if(!b.ready||b.campaign.map!=map||CampaignMaps.selected!=map)return; // the arena is still being built
            var profile=b.campaign.profile(p.getUuid());
            for(var t:Spec.TROOPS)if(t.faction().equals("dawn"))profile.army.put(t.id(),t.id().equals("knight")?20:0);
            squad=b.squads.get(p.getUuid());if(squad==null)return;
            squad.team=0;squad.seated=true;
            b.startRound();
            b.squads.values().removeIf(s->s!=squad);
            b.units.removeIf(u->{if(u.squad!=squad){u.mob.discard();return true;}return false;});
            phase=2;return;
        }
        if(!b.running){ // something sent everyone back to camp: set the map up again, up to three times
            if(++retries>3){
                String name="map-"+map+"-setup";MeltyTest.report(name,false,"the round kept ending before the crossings were measured");System.out.println("[Graves and Glory routes] "+name+" FAILED");
                all=false;nextMap(b,p);return;
            }
            phase=0;return;
        }
        if(phase==2){
            if(b.time<b.campaign.introUntil)return; // units do not move before the intro ends, and the player is held at base
            long mine=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()).count(),knights=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&u.spec.id().equals("knight")).count(),foreign=b.units.stream().filter(u->u.squad!=squad&&u.mob.isAlive()).count();
            boolean army=mine==20&&knights==20&&foreign==0;all&=army;
            String name="map-"+map+"-setup";MeltyTest.report(name,army,"army="+mine+" knights="+knights+" foreign="+foreign);System.out.println("[Graves and Glory routes] "+name+" army="+mine+" knights="+knights+" foreign="+foreign);
            if(houses){goals=houseGoals(map);if(goals.isEmpty()){all=false;MeltyTest.report("map-"+map+"-houses",false,"no supply house on this map");nextMap(b,p);return;}}
            prepare(b,p);phase=3;return;
        }
        if(houses){houseGoal(b,p,map);return;}
        var crossingList=bridges();var route=crossingList.get(bridge);
        long passed=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&u.mob.getX()>route.x()+route.width()/2.0+4&&u.mob.getY()>63).count();
        if(passed==20||b.time-at>1200){
            boolean pass=passed==20;all&=pass;crossings++;if(pass)crossed.merge(map,1,Integer::sum);String name="map-"+map+"-bridge-"+bridge;MeltyTest.report(name,pass,"crossed "+passed+"/20");System.out.println("[Graves and Glory routes] "+name+" "+passed+"/20; elapsed="+(b.time-at)/20.0+"s");
            int currentMap=map,currentBridge=bridge;client.execute(()->ScreenshotRecorder.saveScreenshot(client.runDirectory,"route-map-"+currentMap+"-bridge-"+currentBridge+".png",client.getFramebuffer(),text->{}));
            bridge++;if(bridge<crossingList.size()){prepare(b,p);return;}
            nextMap(b,p);
        }
    }
    void nextMap(GravesAndGlory.Battle b,ServerPlayerEntity p){
        bridge=0;retries=0;index++;phase=0;
        if(index<maps.length)return;
        b.campaign.returnLobby();
        b.campaign.profiles.put(p.getUuid(),backup);b.campaign.save(p.getUuid());
        b.campaign.practice=practice;b.campaign.fill=fill;b.campaign.size=size;
        MeltyTest.report(houses?"all-house-navigation":"all-map-navigation",all,crossings+(houses?" supply house goals of twenty knights over ":" actual twenty-troop crossings over ")+maps.length+" maps");
        System.out.println("[Graves and Glory routes] ALL MAPS COMPLETE success="+all+(houses?" house goals=":" crossings=")+crossings);done=true;
    }
    /** Places the army west of the next crossing, only now that the round is running, and sends it east. */
    void prepare(GravesAndGlory.Battle b,ServerPlayerEntity p){
        if(houses){prepareHouse(b,p);return;}
        var span=bridges().get(bridge);double start=span.x()-span.width()/2.0-14,westEdge=span.x()-span.width()/2.0-3;
        squad.assignments.clear();squad.order="move";squad.formation="line";squad.target=new Vec3d(span.x()+span.width()/2.0+18,65,span.z());squad.yaw=-90;
        for(var u:b.units)if(u.squad==squad){
            Vec3d cell=standingCell(b,start-(u.slot/5)*2,span.z()+(u.slot%5-2)*1.8,westEdge);
            u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(cell.x,cell.y,cell.z,-90,0);u.nextPath=0;u.stuckTicks=0;u.lastPosition=u.mob.getPos();
        }
        p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,start-14,78,span.z()+24,-60,22);at=b.time;
        client.execute(()->client.setScreen(null));
    }
    /** House mode: the goals of the map in sheet order of its supplyHouses rows, two per house: from the spawn of the faction whose half holds the house, and from 12 blocks out of the point nearest the yard, toward the yard. */
    static List<Goal> houseGoals(int map){
        var out=new ArrayList<Goal>();
        for(var h:Spec.SUPPLYHOUSES){
            if(h.map()!=map)continue;
            if(ONLY!=null&&!ONLY.isEmpty()&&!java.util.Arrays.asList(ONLY.split(",")).contains(h.id()))continue;
            var f=Spec.FACTIONS[h.yardX()<0?0:1];
            out.add(new Goal(h,"base",f.spawnX(),f.spawnZ()));
            Spec.PointsRow near=null;double best=Double.MAX_VALUE;
            for(var pt:Spec.POINTS){double d=Math.hypot(pt.x()-h.yardX(),pt.z()-h.yardZ());if(d<best){best=d;near=pt;}}
            double dx=h.yardX()-near.x(),dz=h.yardZ()-near.z(),l=Math.hypot(dx,dz);
            out.add(new Goal(h,"point",near.x()+dx/l*12,near.z()+dz/l*12));
            if(h.id().equals(NOOK_HOUSE)){out.add(new Goal(h,"nook-even",NOOK[0][0],NOOK[0][1]));out.add(new Goal(h,"nook-odd",NOOK[0][0],NOOK[0][1]));out.add(new Goal(h,"nook-far",NOOK[0][0],NOOK[0][1]));}
        }
        return out;
    }
    /** House mode: the twenty knights in a block of five by four about the start, facing the yard, order move to the yard centre; the spectator 16 blocks from the yard toward the start and 18 up, looking at the yard. */
    void prepareHouse(GravesAndGlory.Battle b,ServerPlayerEntity p){
        var g=goals.get(bridge);var h=g.house();double tx=h.yardX()+.5,tz=h.yardZ()+.5;
        double dx=tx-g.sx(),dz=tz-g.sz(),l=Math.hypot(dx,dz),ux=dx/l,uz=dz/l;float yaw=(float)Math.toDegrees(Math.atan2(-ux,uz));
        squad.assignments.clear();squad.order="move";squad.formation="block";squad.target=new Vec3d(tx,h.elevation()+1,tz);squad.yaw=yaw;
        int[] nook=nookSlots(g.kind());boolean far=g.kind().equals("nook-far");
        pocket=nook==null?null:pocketMissing(b);
        Vec3d farCell=far?standingCell(b,g.sx()+ux*NOOK_FAR_BLOCKS,g.sz()+uz*NOOK_FAR_BLOCKS,Double.MAX_VALUE):null;
        if(far)squad.target=farCell;
        for(var u:b.units)if(u.squad==squad){
            double rank=u.slot/5,file=u.slot%5-2;
            Vec3d cell=standingCell(b,g.sx()-ux*rank*2.0-uz*file*1.8,g.sz()-uz*rank*2.0+ux*file*1.8,Double.MAX_VALUE);
            if(nook!=null){ // the yard for all but the two slots of the pocket, which stand in it, one at each of its two cells
                int at=-1;for(int i=0;i<nook.length;i++)if(nook[i]==u.slot)at=i;
                cell=at>=0?new Vec3d(NOOK[at][0],NOOK_Y,NOOK[at][1]):far?standingCell(b,farCell.x+(u.slot%5-2)*1.5,farCell.z+(u.slot/5-1.5)*1.5,Double.MAX_VALUE):standingCell(b,tx+(u.slot%5-2)*1.5,tz+(u.slot/5-1.5)*1.5,Double.MAX_VALUE);
            }
            u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(cell.x,cell.y,cell.z,yaw,0);u.nextPath=0;u.stuckTicks=0;u.lastPosition=u.mob.getPos();
        }
        double px=tx-ux*16,pz=tz-uz*16,py=h.elevation()+18;
        float watchYaw=(float)Math.toDegrees(Math.atan2(-(tx-px),tz-pz)),watchPitch=(float)Math.toDegrees(Math.atan2(py-(h.elevation()+1),16.0));
        p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,px,py,pz,watchYaw,watchPitch);at=b.time;
        client.execute(()->client.setScreen(null));
    }
    /** House mode: has the army come to the yard of the goal (all twenty living knights within 10 blocks of its centre, above its ground), or are the 3600 ticks over? Reports and goes on to the next goal or map. */
    void houseGoal(GravesAndGlory.Battle b,ServerPlayerEntity p,int map){
        var g=goals.get(bridge);var h=g.house();double cx=h.yardX()+.5,cz=h.yardZ()+.5;
        long alive=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()).count();
        long there=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&Math.hypot(u.mob.getX()-cx,u.mob.getZ()-cz)<=10.0&&u.mob.getY()>h.elevation()+0.5).count();
        boolean far=g.kind().equals("nook-far");
        if(far){ // the two knights of the pocket are 40 blocks out of it (the others stand at the far point)
            there=b.units.stream().filter(u->u.squad==squad&&u.mob.isAlive()&&java.util.Arrays.stream(NOOK_EVEN).anyMatch(s->s==u.slot)&&Math.hypot(u.mob.getX()-NOOK[0][0],u.mob.getZ()-NOOK[0][1])>=NOOK_OUT_BLOCKS).count();
        }
        int want=far?NOOK_EVEN.length:20;
        boolean pass=alive==20&&there==want&&pocket==null;
        if(TRACE&&!pass&&(b.time-at)%200==0){StringBuilder t=new StringBuilder();for(var u:b.units)if(u.squad==squad&&u.mob.isAlive()&&!(Math.hypot(u.mob.getX()-cx,u.mob.getZ()-cz)<=10.0&&u.mob.getY()>h.elevation()+0.5))t.append(" s").append(u.slot).append('(').append(String.format("%.0f,%.0f,%.0f",u.mob.getX(),u.mob.getY(),u.mob.getZ())).append(')');System.out.println("[Graves and Glory routes] trace "+h.id()+"-"+g.kind()+" t="+(b.time-at)/20+"s not there:"+t);}
        if(!pass&&pocket==null&&b.time-at<=(nookSlots(g.kind())!=null?1200:3600))return;
        all&=pass;crossings++;if(pass)reached.merge(map,1,Integer::sum);
        String name="map-"+map+"-house-"+h.id()+"-"+g.kind(),text=(pocket!=null?pocket+"; ":"")+"reached "+there+"/"+want+(far?" out of the pocket":"")+" ("+alive+" alive); elapsed="+(b.time-at)/20.0+"s; step hops so far "+Tactics.stepHops+", detours so far "+Tactics.detours+" (searches "+Tactics.detourSearches+", misses "+Tactics.detourMisses+String.format(", longest %.1f ms, all %.1f ms",Tactics.detourMaxNanos/1e6,Tactics.detourNanos/1e6)+")"+(nookSlots(g.kind())!=null&&!Tactics.DETOUR?" [GRAVES_NO_DETOUR=1: the control, it is meant to fail]":"");
        MeltyTest.report(name,pass,text);System.out.println("[Graves and Glory routes] "+name+" "+text);lines.add(h.id()+"-"+g.kind()+" "+there+"/"+want);
        if(TRACE&&!pass)for(var u:b.units)if(u.squad==squad&&u.mob.isAlive()&&!(Math.hypot(u.mob.getX()-cx,u.mob.getZ()-cz)<=10.0&&u.mob.getY()>h.elevation()+0.5)){
            System.out.println("[Graves and Glory routes] surroundings of slot "+u.slot+", heading to "+u.lastGoal+": each cell is the ground height against the unit's feet and T where a block at the ground+1 or +2 has a collision shape, x across from "+((int)Math.floor(u.mob.getX())-7)+", z down from "+((int)Math.floor(u.mob.getZ())-7));
            int ux=(int)Math.floor(u.mob.getX()),uz=(int)Math.floor(u.mob.getZ()),uy=(int)Math.floor(u.mob.getY());
            var path=u.mob.getNavigation().getCurrentPath();
            if(path==null)System.out.println("[Graves and Glory routes] slot "+u.slot+" has no current path");
            else{StringBuilder pt=new StringBuilder("slot "+u.slot+" path of "+path.getLength()+" nodes at "+path.getCurrentNodeIndex()+", finished "+path.isFinished()+", reaches target "+path.reachesTarget()+":");
                for(int i=Math.max(0,path.getCurrentNodeIndex()-1);i<Math.min(path.getLength(),path.getCurrentNodeIndex()+5);i++){var n=path.getNode(i);var bp=new BlockPos(n.x,n.y,n.z);pt.append(" [").append(i).append(": ").append(n.x).append(',').append(n.y).append(',').append(n.z).append(" feet ").append(b.world.getBlockState(bp).getBlock().getTranslationKey().replace("block.minecraft.","")).append(" head ").append(b.world.getBlockState(bp.up()).getBlock().getTranslationKey().replace("block.minecraft.","")).append(" below ").append(b.world.getBlockState(bp.down()).getBlock().getTranslationKey().replace("block.minecraft.","")).append(']');}
                System.out.println("[Graves and Glory routes] "+pt);}
            System.out.println("[Graves and Glory routes] slot "+u.slot+" blocks round the feet at y "+uy+": "+blocksAround(b,ux,uy,uz));
            if(path!=null&&path.getCurrentNodeIndex()<path.getLength()){var n=path.getCurrentNode();
                System.out.println("[Graves and Glory routes] slot "+u.slot+" columns from y "+(uy-1)+" up (a stone brick is a letter, air a dot): unit "+column(b,ux,uz,uy-1,uy+5)+" next node "+column(b,n.x,n.z,uy-1,uy+5)+" over the unit "+column(b,ux,uz,uy+2,uy+8)
                    +"; unit at ("+u.mob.getX()+","+u.mob.getY()+","+u.mob.getZ()+") velocity "+u.mob.getVelocity()+" box "+u.mob.getBoundingBox());}
            for(int dz=-7;dz<=7;dz++){StringBuilder row=new StringBuilder();for(int dx=-7;dx<=7;dx++){
                int gx=ux+dx,gz=uz+dz,gy=Arena.ground(gx,gz);boolean t=false;
                for(int k=1;k<=2;k++){var bp=new BlockPos(gx,gy+k,gz);if(!b.world.getBlockState(bp).getCollisionShape(b.world,bp).isEmpty())t=true;}
                row.append(String.format("%+3d",gy+1-uy)).append(t?"T":" ").append(dx==0&&dz==0?"*":" ");}
                System.out.println("[Graves and Glory routes] "+row);}
        }
        if(!pass)for(var u:b.units)if(u.squad==squad&&u.mob.isAlive()&&!(Math.hypot(u.mob.getX()-cx,u.mob.getZ()-cz)<=10.0&&u.mob.getY()>h.elevation()+0.5))
            System.out.println("[Graves and Glory routes] straggler "+name+" slot "+u.slot+" at ("+String.format("%.1f,%.1f,%.1f",u.mob.getX(),u.mob.getY(),u.mob.getZ())+") "+String.format("%.1f",Math.hypot(u.mob.getX()-cx,u.mob.getZ()-cz))+" from the yard centre, stuckTicks "+u.stuckTicks+", navigation idle "+u.mob.getNavigation().isIdle()+", on ground "+u.mob.isOnGround());
        client.execute(()->ScreenshotRecorder.saveScreenshot(client.runDirectory,"route-"+name+".png",client.getFramebuffer(),text2->{}));
        bridge++;if(bridge<goals.size()){prepare(b,p);return;}
        nextMap(b,p);
    }
    /** The blocks of the column (x, z) from y0 to y1 as names, a dot for air (the trace of a knight that does not move). */
    static String column(GravesAndGlory.Battle b,int x,int z,int y0,int y1){
        StringBuilder t=new StringBuilder("["+x+","+z+":");
        for(int y=y0;y<=y1;y++){var st=b.world.getBlockState(new BlockPos(x,y,z));t.append(' ').append(st.isAir()?".":st.getBlock().getTranslationKey().replace("block.minecraft.","")+(st.getCollisionShape(b.world,new BlockPos(x,y,z)).isEmpty()?"(soft)":""));}
        return t.append(']').toString();
    }
    /** The 3 by 3 of blocks round (x, y, z) at feet and head height, as names, for the trace of a knight that does not move. */
    static String blocksAround(GravesAndGlory.Battle b,int x,int y,int z){
        StringBuilder t=new StringBuilder();
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){var bp=new BlockPos(x+dx,y,z+dz);t.append(dx).append(',').append(dz).append('=').append(b.world.getBlockState(bp).getBlock().getTranslationKey().replace("block.minecraft.","")).append('/').append(b.world.getBlockState(bp.up()).getBlock().getTranslationKey().replace("block.minecraft.","")).append(' ');}
        return t.toString();
    }
    /** The nearest place to (x,z) a knight can stand: something to stand on, nothing solid at feet or head, no water, and not past maxX. Keeps walls, trees and the river out of the placement. */
    static Vec3d standingCell(GravesAndGlory.Battle b,double x,double z,double maxX){
        int cx=(int)Math.floor(x),cz=(int)Math.floor(z);
        for(int r=0;r<=8;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            int tx=cx+dx,tz=cz+dz,ty=Arena.ground(tx,tz)+1;
            if(tx+.5>maxX||Arena.river(tx,tz))continue;
            var below=b.world.getBlockState(new BlockPos(tx,ty-1,tz));var feet=b.world.getBlockState(new BlockPos(tx,ty,tz));var head=b.world.getBlockState(new BlockPos(tx,ty+1,tz));
            boolean floor=!below.getCollisionShape(b.world,new BlockPos(tx,ty-1,tz)).isEmpty()&&below.getFluidState().isEmpty();
            boolean open=feet.getCollisionShape(b.world,new BlockPos(tx,ty,tz)).isEmpty()&&feet.getFluidState().isEmpty()&&head.getCollisionShape(b.world,new BlockPos(tx,ty+1,tz)).isEmpty()&&head.getFluidState().isEmpty();
            if(floor&&open)return new Vec3d(tx+.5,ty,tz+.5);
        }
        return new Vec3d(cx+.5,Arena.ground(cx,cz)+1,cz+.5);
    }
}
