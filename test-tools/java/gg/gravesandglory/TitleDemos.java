package gg.gravesandglory;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/** Unshipped title-screen demo photographer, GRAVES_PHASE1_CHECKS=titledemo. Bot armies clash on Rivergate; a hill of real
 *  blocks is raised at the edge of the fight with the player (classic Steve skin, diamond sword) on its summit facing the
 *  battle, and he is photographed at 1920x1080 from several cameras, times of day and arm poses, as
 *  title-demo-<n>-<description>.png. Free cameras and the raised arm go through harness-only render hooks
 *  (testing/mixin/TitleDemo*Mixin); while camera and pose are null the client draws vanilla F5 and vanilla arms.
 *  Every block the suite places is put back before it builds the next standpoint. Never included in releases. */
public final class TitleDemos implements Phase1Checks.Suite,Phase1Checks.ServerWatch,ClientModInitializer {
    /** Entrypoint of the test-tools mod: offers this suite to Phase1Checks without touching its suite table. */
    @Override public void onInitializeClient(){Phase1Checks.SUITES.put("titledemo",TitleDemos::new);}
    /** Read by TitleDemoCameraMixin every frame: x, y, z, yaw, pitch. Null: the vanilla camera. */
    public static volatile double[] camera;
    /** Read by TitleDemoPoseMixin for the local player only: right arm pitch, yaw, roll in radians. Null: the vanilla arm. */
    public static volatile float[] pose;

    static final int MAP=0,ARMIES=3,HILL_HEIGHT=6,STAGE_DISTANCE=24,WIDTH=1920,HEIGHT=1080,SAFE=30;
    /** Right-arm pose for the render hook. The game's armory diamond sword sits 135 degrees from the arm, so its blade
     *  stands straight up when the arm is 45 degrees forward of vertical (pitch -2.36); the roll takes the arm a little
     *  out to the side. Batch 1 compared -2.12 and -2.62 from the side: the blade leans forward or back. */
    static final float[] SWORD_UP={-2.36f,0f,-0.25f};
    /** Where the armies meet (mx, mz), the range of facings searched for the clearest view from the hill, Steve's distance
     *  from the fight, and whether the armies close across his view (lateral) or along the bridge road (world x). */
    record View(String name,double mx,double mz,double facingFrom,double facingTo,int distance,boolean lateral){}
    static final View[] VIEWS={
        new View("bridge",0.5,0.5,-162.5,-127.5,36,false),      // point B on the bridge; sun behind his left shoulder
        new View("sunset",32.5,0.5,77.5,102.5,34,true),         // the east bridgehead, Steve looking west into the sunset
        new View("bridgehead",-26.5,0.5,-50,-20,34,false)};     // the west bridgehead, sun behind his right shoulder
    /** One photograph: the standpoint, time of day, whether the armies muster afresh first, whether the point lanterns are
     *  placed, the camera, the arm (up = the sword straight up through the hook, swing = a real vanilla swing in three
     *  frames a/b/c), the field of view, and the ticks to wait before the capture. */
    record Shot(int view,String name,int time,boolean muster,boolean lanterns,String cam,String arm,int fov,int ticks){}
    static final List<Shot> PLAN=List.of(
        new Shot(0,"bridge-golden-shoulder-up",12500,true,false,"shoulder","up",60,300),
        new Shot(0,"bridge-golden-over-up",12500,false,false,"over","up",60,30),
        new Shot(0,"bridge-golden-high-up",12500,false,false,"high","up",60,30),
        new Shot(0,"bridge-golden-threeq-up",12500,false,false,"threeq","up",70,30),
        new Shot(0,"bridge-golden-shoulder-swing",12500,false,false,"shoulder","swing",60,20),
        new Shot(0,"bridge-night-shoulder-up",18000,true,true,"shoulder","up",60,260),
        new Shot(0,"bridge-night-over-up",18000,false,false,"over","up",60,30),
        new Shot(0,"bridge-night-high-up",18000,false,false,"high","up",60,30),
        new Shot(0,"bridge-night-threeq-up",18000,false,false,"threeq","up",70,30),
        new Shot(1,"sunset-12000-shoulder-up",12000,true,false,"shoulder","up",60,300),
        new Shot(1,"sunset-12000-over-up",12000,false,false,"over","up",60,30),
        new Shot(1,"sunset-12000-high-up",12000,false,false,"high","up",60,30),
        new Shot(1,"sunset-12000-threeq-up",12000,false,false,"threeq","up",70,30),
        new Shot(1,"sunset-12000-shoulder-swing",12000,false,false,"shoulder","swing",60,20),
        new Shot(1,"sunset-12500-shoulder-up",12500,true,false,"shoulder","up",60,260),
        new Shot(1,"sunset-12500-high-up",12500,false,false,"high","up",60,30),
        new Shot(2,"bridgehead-golden-shoulder-up",12500,true,false,"shoulder","up",60,300),
        new Shot(2,"bridgehead-golden-over-up",12500,false,false,"over","up",60,30),
        new Shot(2,"bridgehead-golden-high-up",12500,false,false,"high","up",60,30),
        new Shot(2,"bridgehead-golden-threeq-up",12500,false,false,"threeq","up",70,30));
    /** Camera spots in Steve's frame (forward, right from his feet); the hill keeps its tufts away from them. */
    static final double[][] CAMERA_SPOTS={{-3.6,1.1},{-2.3,.85},{-2.6,-3.0}};

    volatile int stage,wantView=-1,viewBuilt=-1;volatile UUID player;
    /** The current standpoint: where the armies meet, Steve's feet, his facing F and right R, and his look. Set on the server. */
    volatile double bx,by,bz,sx,sy,sz,fx,fz,rx,rz;volatile float lookYaw,lookPitch;volatile boolean lateral;
    volatile int wantTime=12500,wantMuster,wantLanterns;int timeSet=-1,musterDone,lanternsDone;
    int index,shotStart=-1,doneAt=-1,battleShots,busyShots,arrowShots;boolean steve,sized,finished;Identifier steveTexture;
    /** The original state of every block the suite changed, put back before the next standpoint is built. */
    final Map<BlockPos,BlockState> undo=new LinkedHashMap<>();

    ServerPlayerEntity sp(GravesAndGlory.Battle b){return player==null?null:b.server.getPlayerManager().getPlayer(player);}

    // ---- client: window, look, camera, pose and the photographs -------------------------------------------
    public void step(MinecraftClient c,int tick){
        if(tick==2||tick==40){long h=c.getWindow().getHandle(); // borderless at the screen's corner, so a 1080 p screen can hold it
            GLFW.glfwSetWindowAttrib(h,GLFW.GLFW_DECORATED,GLFW.GLFW_FALSE);GLFW.glfwSetWindowPos(h,0,0);GLFW.glfwSetWindowSize(h,WIDTH,HEIGHT);}
        if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();Phase1Checks.server(c,b->{player=id;stage=1;});}
        if(tick>7000&&!finished){finished=true;camera=null;pose=null;Phase1Checks.report("p1-titledemo-timeout",false,"stuck at stage "+stage+", shot "+index);Phase1Checks.finish(c,"titledemo");return;}
        if(stage<3||finished)return;
        if(tick%10==0){boolean now=classicSteve(c);if(now&&!steve)Phase1Checks.report("p1-titledemo-steve",true,"local player drawn with the vanilla wide Steve skin");steve|=now;} // kept on in case a late skin lookup lands
        hold(c);
        if(index>=PLAN.size()){
            if(doneAt<0)doneAt=tick;
            if(tick==doneAt+40){finished=true;camera=null;pose=null;
                Phase1Checks.report("p1-titledemo-shaders",Presentation.shader()==1,"Iris pack index "+Presentation.shader()+" (1 = Complementary Reimagined)");
                Phase1Checks.report("p1-titledemo-battle",busyShots==battleShots&&battleShots>0,busyShots+" of "+battleShots+" photographs had at least 12 troops of each side within 30 blocks of the fight");
                Phase1Checks.report("p1-titledemo-arrows",arrowShots>0,arrowShots+" photographs with arrows in flight");
                Phase1Checks.finish(c,"titledemo");}
            return;
        }
        Shot s=PLAN.get(index);
        if(viewBuilt!=s.view()){wantView=s.view();camera=null;return;} // the server is (re)building the hill
        if(shotStart<0){shotStart=tick;begin(c,s);}
        int at=tick-shotStart;
        if(!s.arm().equals("swing")){if(at==s.ticks()){capture(c,s,"");next();}return;}
        if(at==s.ticks()-3)c.player.swingHand(Hand.MAIN_HAND); // the swing peaks about three ticks later
        if(at>=s.ticks()&&at<=s.ticks()+2){capture(c,s,"-"+(char)('a'+at-s.ticks()));if(at==s.ticks()+2)next();}
    }
    void next(){index++;shotStart=-1;}
    public void cleanup(MinecraftClient c){camera=null;pose=null;c.options.hudHidden=false;}
    void begin(MinecraftClient c,Shot s){
        if(s.time()!=wantTime){wantTime=s.time();if(c.world!=null)c.world.setTimeOfDay(s.time());}
        if(s.muster())wantMuster++;
        if(s.lanterns())wantLanterns++;
        c.options.getFov().setValue(s.fov());
        pose=s.arm().equals("swing")?null:SWORD_UP;
        camera=camera(s.cam());
        System.out.println("[p1] titledemo begin "+s.name()+" camera="+(camera==null?"vanilla-f5":String.format("%.1f %.1f %.1f yaw %.1f pitch %.1f",camera[0],camera[1],camera[2],camera[3],camera[4])));
    }
    /** Every tick: no HUD, third person behind, no screens or toasts, and Steve looking at the battle. */
    void hold(MinecraftClient c){
        c.options.hudHidden=true;
        if(c.options.getPerspective()!=Perspective.THIRD_PERSON_BACK)c.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        if(c.currentScreen!=null)c.setScreen(null);
        c.getToastManager().clear();
        var p=c.player;float yaw=lookYaw,pitch=lookPitch;
        p.setYaw(yaw);p.prevYaw=yaw;p.setPitch(pitch);p.prevPitch=pitch;p.setHeadYaw(yaw);p.prevHeadYaw=yaw;p.setBodyYaw(yaw);p.prevBodyYaw=yaw;
    }
    /** Camera placements in Steve's own frame (F toward the battle, R to his right, from his feet). The fight sits right of
     *  centre for the cameras on his right and left of centre for the one on his left. */
    double[] camera(String kind){
        Vec3d s=new Vec3d(sx,sy,sz),f=new Vec3d(fx,0,fz),r=new Vec3d(rx,0,rz),m=new Vec3d(bx,by+1,bz);
        return switch(kind){
            case "shoulder"->aim(s.add(f.multiply(-3.6)).add(r.multiply(1.1)).add(0,2.7,0),m,-12,0);
            case "over"->aim(s.add(f.multiply(-2.3)).add(r.multiply(.85)).add(0,2.05,0),m,-10,-1);
            case "high"->aim(s.add(f.multiply(-9)).add(r.multiply(3.2)).add(0,6.5,0),m,-6,6);
            case "threeq"->aim(s.add(f.multiply(-2.6)).add(r.multiply(-3.0)).add(0,2.1,0),m,12,2);
            default->null; // vanilla third person
        };
    }
    static double[] aim(Vec3d from,Vec3d to,double yawOffset,double pitchOffset){
        double dx=to.x-from.x,dy=to.y-from.y,dz=to.z-from.z;
        return new double[]{from.x,from.y,from.z,Math.toDegrees(Math.atan2(-dx,dz))+yawOffset,-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)))+pitchOffset};
    }
    void capture(MinecraftClient c,Shot s,String suffix){
        String name="title-demo-"+(index+1)+"-"+s.name()+suffix;
        int w=c.getWindow().getFramebufferWidth(),h=c.getWindow().getFramebufferHeight();
        ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),t->{});
        if(!sized){sized=true;Phase1Checks.report("p1-titledemo-size",w==WIDTH&&h==HEIGHT,"framebuffer "+w+"x"+h);}
        boolean battle=!suffix.endsWith("b")&&!suffix.endsWith("c");
        Phase1Checks.server(c,b->{
            int[] near=new int[2];for(var u:b.units)if(u.mob.isAlive()&&u.mob.squaredDistanceTo(bx,by,bz)<30*30)near[u.squad.team]++;
            int arrows=Phase1Checks.tagged(b);
            if(battle){battleShots++;if(near[0]>=12&&near[1]>=12)busyShots++;}
            if(arrows>0)arrowShots++;
            System.out.println("[p1] titledemo shot "+name+" time="+b.world.getTimeOfDay()+" dawn="+near[0]+" dusk="+near[1]+" arrows="+arrows+" fb="+w+"x"+h+" fov="+s.fov()+" running="+b.running);
        });
    }
    /** The local player wears the vanilla wide Steve skin, read from the game's own default pack so a resource pack's
     *  steve.png cannot replace it. Returns true once the renderer reports that skin. */
    boolean classicSteve(MinecraftClient c){
        try{
            var entry=c.getNetworkHandler()==null?null:c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
            if(entry==null)return false;
            if(steveTexture==null){
                var stream=c.getDefaultResourcePack().open(ResourceType.CLIENT_RESOURCES,new Identifier("minecraft","textures/entity/player/wide/steve.png"));
                if(stream==null)return false;
                try(var in=stream.get()){var id=new Identifier("gravesandglory_test","title_demo/steve");c.getTextureManager().registerTexture(id,new NativeImageBackedTexture(NativeImage.read(in)));steveTexture=id;}
            }
            entry.getSkinTexture(); // starts the profile's own skin lookup first, so it cannot replace ours afterwards
            var textures=PlayerListEntry.class.getDeclaredField("textures");textures.setAccessible(true);
            @SuppressWarnings("unchecked") var map=(Map<MinecraftProfileTexture.Type,Identifier>)textures.get(entry);
            map.put(MinecraftProfileTexture.Type.SKIN,steveTexture);
            var model=PlayerListEntry.class.getDeclaredField("model");model.setAccessible(true);model.set(entry,"default");
            return steveTexture.equals(c.player.getSkinTexture())&&"default".equals(c.player.getModel());
        }catch(Exception e){System.out.println("[p1] titledemo classic Steve failed: "+e);return false;}
    }

    // ---- server: battle, hill, the player on it -----------------------------------------------------------
    public void serverTick(GravesAndGlory.Battle b){
        if(stage==1){var p=sp(b);if(p!=null&&Phase1Checks.skirmish(b,p,MAP,ARMIES,"normal"))stage=2;return;}
        if(stage==2){var p=sp(b);p.changeGameMode(GameMode.ADVENTURE);p.setInvulnerable(true);p.getInventory().clear(); // no armour: Steve's own clothes
            for(int i=0;i<9;i++)p.getInventory().setStack(i,new ItemStack(Items.DIAMOND_SWORD));stage=3;return;}
        if(stage!=3||!b.running)return;
        if(wantView!=viewBuilt&&wantView>=0){build(b,wantView);return;}
        if(viewBuilt<0)return;
        if(wantTime!=timeSet){b.world.setTimeOfDay(wantTime);timeSet=wantTime;}
        if(wantLanterns>lanternsDone){lanterns(b);lanternsDone=wantLanterns;}
        if(wantMuster>musterDone){muster(b);musterDone=wantMuster;}
        // Every army, the player's included, keeps attacking the fight; no bot decision or order moves them elsewhere.
        var target=new Vec3d(bx,by,bz);
        for(var s:b.squads.values())if(s.seated){s.bot=false;s.assignments.clear();s.order="attack";s.formation="block";s.target=target;
            s.lastStand.clear();s.lastStandAt.clear();}
        var p=sp(b);if(p==null)return;
        // Troops engage within 24 blocks: no Dusk troop is let within 30 of the man on the hill.
        for(var u:b.units)if(u.squad.team==1&&u.mob.isAlive()&&u.mob.squaredDistanceTo(p)<SAFE*SAFE)u.mob.discard();
        if(p.squaredDistanceTo(sx,sy,sz)>0.25)b.relocate(p,sx,sy,sz,lookYaw,lookPitch);
        p.setHealth(p.getMaxHealth());
    }
    /** Puts back the last standpoint's blocks, then raises the hill for view v and stands Steve on its summit. */
    void build(GravesAndGlory.Battle b,int v){
        for(var e:undo.entrySet())b.world.setBlockState(e.getKey(),e.getValue(),2);
        int restored=undo.size();undo.clear();lanternsDone=wantLanterns;
        var view=VIEWS[v];
        bx=view.mx();bz=view.mz();by=Arena.ground((int)Math.floor(bx),(int)Math.floor(bz))+1;lateral=view.lateral();
        double facing=chooseFacing(b,view),yaw=Math.toRadians(facing);
        fx=-Math.sin(yaw);fz=Math.cos(yaw);rx=-Math.cos(yaw);rz=-Math.sin(yaw);
        int px=(int)Math.floor(bx-fx*view.distance()),pz=(int)Math.floor(bz-fz*view.distance());
        int top=hill(b,px,pz);
        sx=px+.5;sy=top+1;sz=pz+.5;
        double dx=bx-sx,dz=bz-sz,dy=(by+.5)-(sy+1.62);
        lookYaw=(float)Math.toDegrees(Math.atan2(-dx,dz));lookPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        clearView(b);
        var p=sp(b);if(p!=null)b.relocate(p,sx,sy,sz,lookYaw,lookPitch);
        boolean ok=b.world.getBlockState(BlockPos.ofFloored(sx,sy-1,sz)).isOf(Blocks.DIRT_PATH)&&top>=Arena.ground(px,pz)+HILL_HEIGHT-1;
        Phase1Checks.report("p1-titledemo-hill-"+view.name(),ok,String.format("facing %.1f, summit %d over ground %d at %.1f %.1f, fight at %.1f %.1f %.1f, %d blocks put back",facing,top,Arena.ground(px,pz),sx,sz,bx,by,bz,restored));
        viewBuilt=v;
    }
    /** The facing in the view's range with the clearest line from the summit to the fight (no obelisk, post or wall in it),
     *  on dry, even ground clear of every structure; nearer the middle of the range breaks ties. */
    double chooseFacing(GravesAndGlory.Battle b,View v){
        double best=Double.NaN,mid=(v.facingFrom()+v.facingTo())/2;int bestScore=Integer.MAX_VALUE;StringBuilder log=new StringBuilder();
        for(double yaw=v.facingFrom();yaw<=v.facingTo()+1e-9;yaw+=2.5){
            double t=Math.toRadians(yaw),ffx=-Math.sin(t),ffz=Math.cos(t);
            int px=(int)Math.floor(v.mx()-ffx*v.distance()),pz=(int)Math.floor(v.mz()-ffz*v.distance());
            String why=dry(px,pz);if(why!=null){log.append(String.format(" %.1f:%s",yaw,why));continue;}
            int blocked=obstructions(b,px+.5,Arena.ground(px,pz)+HILL_HEIGHT+3.7,pz+.5,ffx,ffz,v.mx(),by+1.2,v.mz());
            int score=blocked*4+(int)Math.round(Math.abs(yaw-mid)/2.5);
            log.append(String.format(" %.1f:%d",yaw,blocked));
            if(score<bestScore){bestScore=score;best=yaw;}
        }
        System.out.println("[p1] titledemo "+v.name()+" facings (blocked cells):"+log);
        return Double.isNaN(best)?mid:best;
    }
    static String dry(int px,int pz){
        int g=Arena.ground(px,pz);
        for(int x=px-12;x<=px+12;x++)for(int z=pz-12;z<=pz+12;z++){if((x-px)*(x-px)+(z-pz)*(z-pz)>144)continue;
            if(Arena.river(x,z))return "river";
            for(var s:Spec.STRUCTURES)if(Math.abs(x-s.x())<=s.width()/2+3&&Math.abs(z-s.z())<=s.depth()/2+3)return s.id();
            if(Math.abs(Arena.ground(x,z)-g)>3)return "uneven";}
        return null;
    }
    /** Solid cells (not air, plants, water, logs or leaves) on seven lines from a camera over the summit to a fan across the fight. */
    static int obstructions(GravesAndGlory.Battle b,double ex,double ey,double ez,double ffx,double ffz,double mx,double my,double mz){
        Set<BlockPos> hit=new HashSet<>();
        for(int j=-3;j<=3;j++){double tx=mx-ffz*j*4,tz=mz+ffx*j*4,dx=tx-ex,dy=my-ey,dz=tz-ez,len=Math.sqrt(dx*dx+dy*dy+dz*dz);
            for(double s=10;s<len-6;s+=.5){var pos=BlockPos.ofFloored(ex+dx*s/len,ey+dy*s/len,ez+dz*s/len);var st=b.world.getBlockState(pos);
                if(st.isAir()||st.isReplaceable()||st.isIn(BlockTags.LOGS)||st.isIn(BlockTags.LEAVES)||st.isIn(BlockTags.FLOWERS)||!st.getFluidState().isEmpty())continue;
                hit.add(pos.toImmutable());}}
        return hit.size();
    }
    void set(BlockPos pos,BlockState state,GravesAndGlory.Battle b){pos=pos.toImmutable();undo.putIfAbsent(pos,b.world.getBlockState(pos));b.world.setBlockState(pos,state,2);}
    void set(int x,int y,int z,Block block,GravesAndGlory.Battle b){set(new BlockPos(x,y,z),block.getDefaultState(),b);}
    /** Fresh full lineups for every seated army, in rows either side of the fight, all ordered to attack it. Dawn stands on
     *  the left of Steve's view (or west on the bridge road), Dusk on the right (or east). */
    void muster(GravesAndGlory.Battle b){
        Projectiles.clear(b);for(var u:b.units)u.mob.discard();b.units.clear();
        double ax=lateral?rx:1,az=lateral?rz:0,qx=-az,qz=ax;
        for(int t=0;t<2;t++){final int team=t;
            var armies=b.squads.values().stream().filter(s->s.seated&&s.team==team).toList();
            int side=t==0?-1:1;
            for(int k=0;k<armies.size();k++){var s=armies.get(k);double off=(k-(armies.size()-1)/2.0)*8.5;int slot=0;
                s.bot=false;s.assignments.clear();s.order="attack";s.formation="block";s.target=new Vec3d(bx,by,bz);
                s.yaw=(float)Math.toDegrees(Math.atan2(side*ax,-side*az));s.lastStand.clear();s.lastStandAt.clear();
                // Phase 2: each side musters the easy bot banner of its default House (Gilded for team 0, Graveborn for team 1), twenty units.
                for(String id:Campaign.bannerRow(t==0?"gilded_easy":"graveborn_easy")){var spec=Campaign.troop(id);if(spec==null)continue;
                    double depth=STAGE_DISTANCE+(slot/5)*1.8,across=off+(slot%5-2)*1.7,x=bx+side*ax*depth+qx*across,z=bz+side*az*depth+qz*across;
                    b.spawn(s,spec,slot,new Vec3d(x,Arena.ground((int)Math.floor(x),(int)Math.floor(z))+1,z));slot++;
                }
                for(String group:Morale.GROUPS)Morale.fielded(b,s,group);
            }
        }
        System.out.println("[p1] titledemo muster: "+b.units.size()+" troops at time "+b.time);
    }
    /** A lantern on a post at the four corners round every point. */
    void lanterns(GravesAndGlory.Battle b){
        for(var p:b.points){var at=p.spec;for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}){int x=at.x()+dx,z=at.z()+dz,y=Arena.ground(x,z)+1;
            set(x,y,z,Blocks.DARK_OAK_FENCE,b);set(x,y+1,z,Blocks.LANTERN,b);}}
    }
    /** No tree trunk or crown between the hill and the fight. */
    void clearView(GravesAndGlory.Battle b){
        for(int i=0;i<=90;i++){double t=i/90.0;for(int w=-10;w<=10;w++){int x=(int)Math.floor(sx+(bx-sx)*t+rx*w),z=(int)Math.floor(sz+(bz-sz)*t+rz*w);
            for(int y=60;y<96;y++){var pos=new BlockPos(x,y,z);var st=b.world.getBlockState(pos);if(st.isIn(BlockTags.LOGS)||st.isIn(BlockTags.LEAVES))set(pos,Blocks.AIR.getDefaultState(),b);}}}
    }
    static long key(int x,int z){return((long)x<<32)^(z&0xffffffffL);}
    static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    static int hash(int x,int z,int salt){return Math.floorMod(x*7349+z*9151+x*z*31+salt*104729,100);}
    static Block stone(int x,int y,int z){int k=hash(x+y*13,z-y*7,3);return k<60?Blocks.STONE:k<80?Blocks.ANDESITE:k<92?Blocks.COBBLESTONE:Blocks.MOSSY_COBBLESTONE;}
    /** One hill of real blocks with its summit on Steve's block (px,pz): six high, about 18 across, steeper toward the fight
     *  (stone shows where a column stands three over its neighbour), gentler behind, a dirt path up its back from the field
     *  behind his right, grass tufts and a few flowers and stones on the slopes (none on the summit or by a camera), and a
     *  lantern post behind his left shoulder. Returns the summit block's y. */
    int hill(GravesAndGlory.Battle b,int px,int pz){
        var w=b.world;double cx=px+.5,cz=pz+.5;Map<Long,Integer> tops=new HashMap<>();
        for(int x=px-12;x<=px+12;x++)for(int z=pz-12;z<=pz+12;z++){
            double dx=x+.5-cx,dz=z+.5-cz,f=dx*fx+dz*fz,q=dx*rx+dz*rz,d=Math.hypot(f/(f>0?9.0:10.5),q/9.0);
            if(d>=1)continue;
            int h=(int)Math.round(HILL_HEIGHT*(1-smooth((d-.2)/.8))+(d>.3&&d<.9?(hash(x,z,1)/100.0-.5)*.8:0));
            if(h<=0)continue;
            int g=Arena.ground(x,z),top=g+h;tops.put(key(x,z),top);
            for(int y=g+1;y<=top;y++)set(x,y,z,y==top?Blocks.GRASS_BLOCK:y>=top-2?Blocks.DIRT:stone(x,y,z),b);
            for(int y=top+1;y<=top+16;y++)if(!w.getBlockState(new BlockPos(x,y,z)).isAir())set(x,y,z,Blocks.AIR,b);
        }
        for(var e:tops.entrySet()){int x=(int)(e.getKey()>>32),z=e.getKey().intValue(),top=e.getValue(),low=top;
            for(int[] n:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})low=Math.min(low,tops.getOrDefault(key(x+n[0],z+n[1]),Arena.ground(x+n[0],z+n[1])));
            if(top-low>=3)for(int y=top-2;y<top;y++)set(x,y,z,stone(x,y,z),b);}
        // The path, in Steve's frame (forward, right): from the field behind his right, up the gentle back, to the summit.
        double[][] path={{-15,8},{-12,6.5},{-9.5,5},{-7,3.6},{-4.6,2.3},{-2.4,1.1},{0,0}};
        Set<Long> onPath=new HashSet<>();
        for(int x=px-16;x<=px+16;x++)for(int z=pz-16;z<=pz+16;z++){
            double dx=x+.5-cx,dz=z+.5-cz,f=dx*fx+dz*fz,q=dx*rx+dz*rz,near=99;
            for(int i=0;i+1<path.length;i++)near=Math.min(near,segment(f,q,path[i][0],path[i][1],path[i+1][0],path[i+1][1]));
            if(near>.8)continue;
            Integer top=tops.get(key(x,z));int y=top!=null?top:Arena.ground(x,z);
            var pos=new BlockPos(x,y,z);var st=w.getBlockState(pos);
            if(!st.isOf(Blocks.GRASS_BLOCK)&&!st.isOf(Blocks.DIRT)&&!st.isOf(Blocks.COARSE_DIRT))continue;
            set(pos,Blocks.DIRT_PATH.getDefaultState(),b);onPath.add(key(x,z));
            for(int a=1;a<=3;a++)if(!w.getBlockState(pos.up(a)).isAir())set(pos.up(a),Blocks.AIR.getDefaultState(),b);
        }
        int summit=tops.getOrDefault(key(px,pz),Arena.ground(px,pz));
        set(px,summit,pz,Blocks.DIRT_PATH,b);onPath.add(key(px,pz));
        // Lantern post behind his left shoulder: out of every camera's view, close enough to light him at night.
        int lx=(int)Math.floor(cx-fx*4.5-rx*3),lz=(int)Math.floor(cz-fz*4.5-rz*3),ly=tops.getOrDefault(key(lx,lz),Arena.ground(lx,lz));
        for(int a=1;a<=2;a++)set(lx,ly+a,lz,Blocks.DARK_OAK_FENCE,b);
        set(lx,ly+3,lz,Blocks.LANTERN,b);
        // Tufts, a few flowers and two stones on the slopes; the summit and the ground under each camera stay clear.
        for(var e:tops.entrySet()){int x=(int)(e.getKey()>>32),z=e.getKey().intValue(),top=e.getValue();
            double dx=x+.5-cx,dz=z+.5-cz,f=dx*fx+dz*fz,q=dx*rx+dz*rz,d=Math.hypot(f/(f>0?9.0:10.5),q/9.0);
            if(onPath.contains(e.getKey())||d<.45||Math.abs(x-lx)+Math.abs(z-lz)<=1)continue;
            boolean byCamera=false;for(double[] c:CAMERA_SPOTS)if(Math.hypot(f-c[0],q-c[1])<3.2)byCamera=true;
            if(byCamera)continue;
            var pos=new BlockPos(x,top+1,z);if(!w.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK))continue;
            int k=hash(x,z,7);
            if(k<16)set(pos,Blocks.GRASS.getDefaultState(),b);
            else if(k<19)set(pos,Blocks.FERN.getDefaultState(),b);
            else if(k<21){set(pos,Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF,DoubleBlockHalf.LOWER),b);set(pos.up(),Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF,DoubleBlockHalf.UPPER),b);}
            else if(k==21)set(pos,Blocks.POPPY.getDefaultState(),b);
            else if(k==22)set(pos,Blocks.DANDELION.getDefaultState(),b);
            else if(k==23)set(pos,Blocks.CORNFLOWER.getDefaultState(),b);
        }
        for(double[] rock:new double[][]{{4.5,-5.5},{-5,6.5}}){int x=(int)Math.floor(cx+fx*rock[0]+rx*rock[1]),z=(int)Math.floor(cz+fz*rock[0]+rz*rock[1]);
            Integer top=tops.get(key(x,z));if(top!=null&&!onPath.contains(key(x,z)))set(x,top+1,z,Blocks.MOSSY_COBBLESTONE,b);}
        System.out.println("[p1] titledemo hill: "+tops.size()+" columns, "+onPath.size()+" path blocks, lantern post at "+lx+" "+(ly+3)+" "+lz);
        return summit;
    }
    static double segment(double x,double y,double ax,double ay,double bx,double by){
        double vx=bx-ax,vy=by-ay,t=Math.max(0,Math.min(1,((x-ax)*vx+(y-ay)*vy)/Math.max(1e-9,vx*vx+vy*vy)));
        return Math.hypot(x-ax-t*vx,y-ay-t*vy);
    }
}
