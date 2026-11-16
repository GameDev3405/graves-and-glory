package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import org.lwjgl.glfw.GLFW;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Unshipped media shoot, GRAVES_PHASE1_CHECKS=film. Stages a battle on a map, raises a hill for the commander, and films it
 *  through FilmRig: stills and clips at GRAVES_FILM_W x GRAVES_FILM_H, HUD hidden, shaders as the game is set. Which shots run
 *  is chosen by GRAVES_FILM_MODE (scout: one set of stills at every objective of every map; shoot: the takes in GRAVES_FILM_ONLY).
 *  Free camera, pose and frame hooks are testing/mixin/Film*Mixin and TitleDemo*Mixin. Never included in releases. */
public final class MediaShoot implements Phase1Checks.Suite,Phase1Checks.ServerWatch,ClientModInitializer {
    @Override public void onInitializeClient(){Phase1Checks.SUITES.put("film",MediaShoot::new);}

    static int envInt(String k,int d){try{return Integer.parseInt(System.getenv().getOrDefault(k,""+d));}catch(Exception e){return d;}}
    static final String OUT=System.getenv().getOrDefault("GRAVES_FILM_OUT","C:/Users/kortt/Melty/bannerbound/work/media");
    static final int W=envInt("GRAVES_FILM_W",2560),H=envInt("GRAVES_FILM_H",1440),FPS=envInt("GRAVES_FILM_FPS",60),WX=envInt("GRAVES_FILM_X",0),WY=envInt("GRAVES_FILM_Y",0),VIEW=envInt("GRAVES_FILM_VIEW",28);
    static final String MODE=System.getenv().getOrDefault("GRAVES_FILM_MODE","scout"),ONLY=System.getenv().getOrDefault("GRAVES_FILM_ONLY","");
    static final int HILL_HEIGHT=6,SAFE=30;
    static final float[] POINT={-1.50f,0f,-0.12f};
    /** One staged battle: map, which objective it centres on, time of day, rain, each side's banners, how the lines face, how far the commander stands back. */
    /** distance 0: the commander stands behind the tail of his own column. gap is the distance from the objective to each column's front rank, cols the troops abreast, spacing the rank spacing. */
    record Scene(String id,int map,int point,int time,boolean rain,String[] dawn,String[] dusk,boolean lateral,int distance,double gap,String posture0,String posture1,String armor,int size,int cols,double spacing){}
    static final double COLSP=1.6,SQGAP=2.2,HEROLEAD=2.4;
    static final String[] GILDED_HARD={"gilded_hard","gilded_normal","gilded_easy"},GRAVE_HARD={"graveborn_hard","graveborn_normal","graveborn_easy"},
        IRON_HARD={"ironclad_hard","ironclad_normal","ironclad_easy"},TIDE_HARD={"tidecallers_hard","tidecallers_normal","tidecallers_easy"};
    /** The five battles of SHOOT-PLAN-2. dawn is the commander's army (he stands behind it), dusk the army across from it. */
    static Scene preset(String id,int map,int point,int time,boolean rain,String[] mine,String[] theirs,String armor){
        return new Scene(id,map,point,envInt("GRAVES_FILM_TIME",time),rain,mine,theirs,false,envInt("GRAVES_FILM_DIST",0),envInt("GRAVES_FILM_GAP",12),"attack","attack",armor,envInt("GRAVES_FILM_SIZE",5),envInt("GRAVES_FILM_COLS",12),envInt("GRAVES_FILM_SPACING",18)/10.0);
    }
    /** Map indexes are the Spec.MAPS order (0 Rivergate, 1 Ironhold, 2 Frostmarch, 3 Hollowwood, 4 Kingsfield); the objective of each is one the field scan found dry for two columns. */
    static List<Scene> presets(){return List.of(
        preset("rivergate",0,2,300,true,TIDE_HARD,GILDED_HARD,"diamond"),
        preset("ironhold",1,0,-1,false,IRON_HARD,GILDED_HARD,"iron"),
        preset("frostmarch",2,0,-1,false,IRON_HARD,TIDE_HARD,"iron"),
        preset("hollowwood",3,0,12350,false,GRAVE_HARD,GILDED_HARD,"netherite"),
        preset("kingsfield",4,1,-1,false,GILDED_HARD,GRAVE_HARD,"gold"),
        preset("media",0,2,-1,false,GILDED_HARD,GRAVE_HARD,"gold"));}
    record Shot(FilmRig.Take take,String stage){}

    // ---- state -------------------------------------------------------------------------------------------------
    volatile UUID player;volatile boolean serverReady,finished;volatile int pointCount;
    volatile double bx,by,bz,sx,sy,sz,fx,fz,rx,rz;volatile float lookYaw,lookPitch;
    volatile double colL0,colL1,fieldCost;volatile int fieldWet;volatile double hx,hy,hz;volatile boolean heroOk;volatile int swingEvery;
    volatile String posture0="attack",posture1="attack",form0="line",form1="line";volatile boolean protect=true,standOk;
    final Map<BlockPos,BlockState> undo=new LinkedHashMap<>();
    final ConcurrentLinkedQueue<Step> jobs=new ConcurrentLinkedQueue<>();
    interface Step{boolean run(GravesAndGlory.Battle b);}
    int phase,wait,mapIdx,ptIdx,takeIdx,sceneIdx,curMap=-1;boolean steve,sized,stagedNow;Identifier steveTexture;List<Shot> shots=List.of();Scene scene;
    final boolean plan=!(MODE.equals("scout")||MODE.equals("fields"));final List<Scene> sceneList=presets().stream().filter(sc->wanted(sc.id())).toList();
    final int[] MAPS=parseMaps();
    int[] parseMaps(){String s=System.getenv().getOrDefault("GRAVES_FILM_MAPS","0,1,2,3,4");return Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();}
    ServerPlayerEntity sp(GravesAndGlory.Battle b){return player==null?null:b.server.getPlayerManager().getPlayer(player);}

    // ---- client ------------------------------------------------------------------------------------------------
    public void step(MinecraftClient c,int tick){
        if(tick==2||tick==40){long h=c.getWindow().getHandle();
            GLFW.glfwSetWindowAttrib(h,GLFW.GLFW_DECORATED,GLFW.GLFW_FALSE);GLFW.glfwSetWindowPos(h,WX,WY);GLFW.glfwSetWindowSize(h,W,H);
            c.options.getMaxFps().setValue(FPS);c.options.getEnableVsync().setValue(false);c.options.getViewDistance().setValue(VIEW);
            c.options.getEntityDistanceScaling().setValue(2.0);c.options.getBobView().setValue(false);
            if(System.getenv("GRAVES_FILM_GAMMA")!=null)c.options.getGamma().setValue(envInt("GRAVES_FILM_GAMMA",50)/100.0);}
        if(tick==30&&envInt("GRAVES_FILM_SHADER",-1)>=0)Presentation.selectShader(envInt("GRAVES_FILM_SHADER",-1));
        if(tick==20){c.setScreen(null);player=c.player.getUuid();phase=1;}
        if(tick>40000&&!finished){finished=true;FilmRig.take=null;Phase1Checks.report("film-timeout",false,"stuck in phase "+phase+" map "+curMap);Phase1Checks.finish(c,"film");return;}
        if(phase<1||finished)return;
        if(tick%10==0){boolean now=classicSteve(c);if(now&&!steve)Phase1Checks.report("film-steve",true,"classic Steve");steve|=now;}
        hold(c);
        if(FilmRig.take!=null||!jobs.isEmpty())return;
        if(wait>0){wait--;return;}
        switch(phase){
            case 1->{ // a new map: start the skirmish on it
                if(plan){
                    if(sceneIdx>=sceneList.size()){finish(c);return;}
                    scene=sceneList.get(sceneIdx);
                    if(scene.map()!=curMap){curMap=scene.map();serverReady=false;jobs.add(skirmish(curMap,scene.size()));}
                    phase=2;return;
                }
                if(mapIdx>=MAPS.length){finish(c);return;}
                int map=MAPS[mapIdx];
                if(map!=curMap){curMap=map;serverReady=false;jobs.add(skirmish(map,envInt("GRAVES_FILM_SIZE",5)));}
                phase=2;
            }
            case 2->{ // the objective to stand on
                if(!plan){
                    if(ptIdx>=pointCount){mapIdx++;ptIdx=0;phase=1;return;}
                    scene=sceneFor(curMap,ptIdx);
                    if(!wanted(scene.id())){ptIdx++;return;}
                }
                System.out.println("[film] scene "+scene.id());
                jobs.add(atmosphere(scene));jobs.add(commander(scene));jobs.add(stand(scene));
                if(!plan)jobs.add(muster(scene,scene.posture0()));
                wait=plan?40:160;phase=3;
            }
            case 3->{ // films
                if(!standOk){if(plan){sceneIdx++;phase=1;}else{ptIdx++;phase=2;}return;}
                if(takeIdx==0&&!stagedNow)shots=shotsFor(scene);
                if(takeIdx>=shots.size()){takeIdx=0;stagedNow=false;if(plan){sceneIdx++;phase=1;}else{ptIdx++;phase=2;}return;}
                var sh=shots.get(takeIdx);
                if(!stagedNow&&!sh.stage().equals("none")){ // put the armies back in their lines, then let them fight or wait for the release
                    boolean hold=sh.stage().equals("hold");
                    jobs.add(muster(scene,hold?"hold":"attack"));wait=hold?50:150;stagedNow=true;return;
                }
                stagedNow=false;takeIdx++;
                System.out.println("[film] take "+sh.take().name());FilmRig.start(sh.take());wait=3;
            }
            default->{}
        }
    }
    boolean wanted(String id){if(ONLY.isBlank())return true;for(String s:ONLY.split(","))if(id.startsWith(s.trim()))return true;return false;}
    void finish(MinecraftClient c){finished=true;FilmRig.take=null;TitleDemos.pose=null;Phase1Checks.finish(c,"film");}
    public void cleanup(MinecraftClient c){FilmRig.take=null;TitleDemos.pose=null;c.options.hudHidden=false;FilmRig.noHud=false;}
    /** Every tick: no HUD, no screens, no toasts, no grabbed mouse, third person so the commander is drawn, the commander looking at the fight. */
    void hold(MinecraftClient c){
        c.options.hudHidden=false;FilmRig.noHud=true;
        var tk=FilmRig.take;boolean pano=tk!=null&&tk.name().contains("-pano-");
        Perspective want=pano||tk!=null&&tk.name().contains("-pov")?Perspective.FIRST_PERSON:Perspective.THIRD_PERSON_BACK;
        c.gameRenderer.setRenderHand(!pano);
        if(c.options.getPerspective()!=want)c.options.setPerspective(want);
        if(c.currentScreen!=null)c.setScreen(null);
        c.getToastManager().clear();
        if(c.mouse.isCursorLocked())c.mouse.unlockCursor();
        var p=c.player;float yaw=lookYaw,pitch=lookPitch;
        p.setYaw(yaw);p.prevYaw=yaw;p.setPitch(pitch);p.prevPitch=pitch;p.setHeadYaw(yaw);p.prevHeadYaw=yaw;p.setBodyYaw(yaw);p.prevBodyYaw=yaw;
        if(!sized){sized=true;Phase1Checks.report("film-size",c.getFramebuffer().textureWidth==W&&c.getFramebuffer().textureHeight==H,"framebuffer "+c.getFramebuffer().textureWidth+"x"+c.getFramebuffer().textureHeight+" wanted "+W+"x"+H);}
    }
    boolean classicSteve(MinecraftClient c){
        try{
            var entry=c.getNetworkHandler()==null?null:c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
            if(entry==null)return false;
            // 1.21.1: a PlayerListEntry holds a Supplier<SkinTextures>; swap it for the vanilla classic (wide) Steve.
            if(steveTexture==null)steveTexture=Identifier.ofVanilla("textures/entity/player/wide/steve.png");
            var steve=new net.minecraft.client.util.SkinTextures(steveTexture,null,null,null,net.minecraft.client.util.SkinTextures.Model.WIDE,false);
            var textures=PlayerListEntry.class.getDeclaredField("texturesSupplier");textures.setAccessible(true);
            textures.set(entry,(java.util.function.Supplier<net.minecraft.client.util.SkinTextures>)()->steve);
            var now=c.player.getSkinTextures();
            return steveTexture.equals(now.texture())&&now.model()==net.minecraft.client.util.SkinTextures.Model.WIDE;
        }catch(Exception e){System.out.println("[film] classic Steve failed: "+e);return false;}
    }

    // ---- scenes and shots ---------------------------------------------------------------------------------------
    /** time -1: the sun is put low on the commander's facing side, so the army is backlit from his hill. */
    Scene sceneFor(int map,int point){return new Scene("m"+map+"p"+point,map,point,envInt("GRAVES_FILM_TIME",-1),false,GILDED_HARD,GRAVE_HARD,envInt("GRAVES_FILM_LATERAL",0)==1,envInt("GRAVES_FILM_DIST",0),envInt("GRAVES_FILM_GAP",12),"attack","attack",System.getenv().getOrDefault("GRAVES_FILM_ARMOR","netherite"),envInt("GRAVES_FILM_SIZE",5),envInt("GRAVES_FILM_COLS",12),envInt("GRAVES_FILM_SPACING",18)/10.0);}
    Vec3d S(){return new Vec3d(sx,sy,sz);}
    Vec3d M(){return new Vec3d(bx,by,bz);}
    Vec3d at(Vec3d o,double f,double r,double u){return o.add(new Vec3d(fx,0,fz).multiply(f)).add(new Vec3d(rx,0,rz).multiply(r)).add(0,u,0);}
    FilmRig.Take still(String name,double[] cam,FilmRig.Pose pose){return still(name,cam,pose,1.5);}
    FilmRig.Take still(String name,double[] cam,FilmRig.Pose pose,double clearance){return still(name,FilmRig.hold(cam),pose,clearance);}
    FilmRig.Take still(String name,FilmRig.Path path,FilmRig.Pose pose,double clearance){return new FilmRig.Take(name,FPS,24,3,path,pose,null,Map.of(1,name+".png"),Map.of(),clearance);}
    /** A clip; release is the recorded frame where both armies are ordered to charge (-1: never); GRAVES_FILM_PREVIEW=N swaps the encode for a PNG every N frames. */
    FilmRig.Take clip(String name,double seconds,FilmRig.Path path,FilmRig.Pose pose,int release,double clearance){return clip(name,seconds,path,pose,release,clearance,Map.of());}
    FilmRig.Take clip(String name,double seconds,FilmRig.Path path,FilmRig.Pose pose,int release,double clearance,Map<Integer,Runnable> more){return clipAt(FPS,name,seconds,path,pose,release,clearance,more);}
    /** A clip captured at its own frame rate (the slow-motion take runs the game at 120 and is slowed to half speed when it is encoded). */
    FilmRig.Take clipAt(int fps,String name,double seconds,FilmRig.Path path,FilmRig.Pose pose,int release,double clearance,Map<Integer,Runnable> more){
        int frames=(int)Math.round(seconds*fps),pv=envInt("GRAVES_FILM_PREVIEW",0);boolean norec=System.getenv("GRAVES_FILM_NOREC")!=null||pv>0;
        Map<Integer,String> stills=new HashMap<>();if(pv>0)for(int f=0;f<frames;f+=pv)stills.put(f,String.format("%s-f%04d.png",name,f));
        Map<Integer,Runnable> events=new HashMap<>(more);if(release>=0)events.put(release,this::release);
        return new FilmRig.Take(name,fps,90*fps/FPS,frames,path,pose,norec?null:new File(OUT+"/clips/"+name+".mp4"),stills,events,clearance);}
    /** A chase camera on the Marshal-class hero of the commander's army: behind, beside and above him, looking down the field past him, smoothed so his steps do not shake it. */
    FilmRig.Path follow(double back,double side,double up,double ahead,double dropLook,double fov){return follow(back,side,up,ahead,dropLook,fov,0,0,0,0);}
    /** From riseFrom to riseTo seconds the camera climbs to endUp and slides to endBack, still looking at the hero. */
    FilmRig.Path follow(double back0,double side,double up0,double ahead,double dropLook,double fov,double riseFrom,double riseTo,double endUp,double endBack){
        double[] sm=new double[3];boolean[] init={false};
        return t->{double x=hx,y=hy,z=hz;double r=riseTo>riseFrom?smooth((t-riseFrom)/(riseTo-riseFrom)):0,up=up0+(endUp-up0)*r,back=back0+(endBack-back0)*r;if(!heroOk&&!init[0]){x=bx-fx*20;y=by;z=bz-fz*20;}
            if(!init[0]){sm[0]=x;sm[1]=y;sm[2]=z;init[0]=true;}
            if(heroOk){double k=.10;sm[0]+=(x-sm[0])*k;sm[1]+=(y-sm[1])*k;sm[2]+=(z-sm[2])*k;}
            return FilmRig.aim(new Vec3d(sm[0]-fx*back+rx*side,sm[1]+up,sm[2]-fz*back+rz*side),new Vec3d(sm[0]+fx*ahead,sm[1]+dropLook,sm[2]+fz*ahead),fov);};
    }
    static final float[] SWORD={-2.95f,0f,-0.12f};
    boolean shotWanted(String key){String o=System.getenv().getOrDefault("GRAVES_FILM_SHOTS","");if(o.isBlank())return true;for(String k:o.split(","))if(key.equals(k.trim()))return true;return false;}
    List<Shot> shotsFor(Scene sc){
        String id=sc.id();Vec3d m=M().add(0,1.5,0),eye=S().add(0,1.7,0);FilmRig.Pose point=t->POINT,sword=t->SWORD;
        double g=sc.gap(),face=Math.toDegrees(Math.atan2(-fx,fz));
        var out=new ArrayList<Shot>();
        if(MODE.equals("fields"))return out;
        if(!plan){ // scouting: one of each candidate still
            for(var t:List.of(still(id+"-a-behind",FilmRig.aim(at(S(),-12,0.6,5.2),m,42),point),still(id+"-c-hero",FilmRig.aim(at(S(),9,0.4,0.2),eye.add(0,-.1,0),45),point),
                still(id+"-d-enemy",FilmRig.aim(at(M(),g+9,-6,2.4),at(M(),-g,4,2.4),48),point),still(id+"-f-clash",FilmRig.aim(at(M(),-9,-4,2.0),m,50),point),
                still(id+"-g-crane",FilmRig.aim(at(M(),-34,0,34),m,50),point),still(id+"-h-drone",FilmRig.aim(at(S(),-10,0,16),m,50),point)))out.add(new Shot(t,"none"));
            return out;
        }
        // geometry: the objective is the origin of "at(M(),f,r,u)", f runs from the commander's army toward the enemy; our column fills f from -g to -tail0, theirs from g to tail1
        double L0=colL0,L1=colL1,tail0=g+L0,tail1=g+L1;
        // stills: the two columns before the clash, then the fight itself
        if(shotWanted("s8"))out.add(new Shot(still(id+"-s8-lines",FilmRig.aim(at(S(),-9,0,4.5),m,45),point),"hold"));
        if(shotWanted("s9"))out.add(new Shot(still(id+"-s9-vanguard",FilmRig.aim(at(M(),-g+7,2.5,1.5),at(S(),0,0,2.2),48),point,.5),"hold"));
        if(shotWanted("s10"))out.add(new Shot(still(id+"-s10-topdown",FilmRig.aim(at(M(),-6,0,64),at(M(),0,0,0),58),point,.5),"hold"));
        if(shotWanted("s11"))out.add(new Shot(still(id+"-s11-flank",FilmRig.aim(at(M(),-6,-56,7),at(M(),0,0,1.5),44),point,1.0),"hold"));
        // muster stills: the two armies standing in formation before the horn (media shoot 3)
        if(shotWanted("m1"))out.add(new Shot(still(id+"-m1-between",FilmRig.aim(at(M(),-1.5,0.5,1.35),at(M(),g+9,0,1.6),56),point,.3),"hold"));
        if(shotWanted("m2"))out.add(new Shot(still(id+"-m2-ranks",FilmRig.aim(at(M(),-g-1,-11.5,0.9),at(M(),-g-L0*.75,1.5,1.5),38),point,.3),"hold"));
        if(shotWanted("m3"))out.add(new Shot(still(id+"-m3-vanguardface",FilmRig.aim(at(M(),-g+4.5,1.8,1.45),at(M(),-g,-0.5,1.7),44),point,.3),"hold"));
        if(shotWanted("m4"))out.add(new Shot(still(id+"-m4-enemyside",FilmRig.aim(at(M(),g+L1+7,-3,3.4),at(M(),g+L1*.4,0,1.6),52),point,.8),"hold"));
        if(shotWanted("m5"))out.add(new Shot(still(id+"-m5-hero",t->FilmRig.aim(new Vec3d(hx+fx*4.2+rx*1.4,hy+1.55,hz+fz*4.2+rz*1.4),new Vec3d(hx,hy+1.75,hz),38),point,.5),"hold"));
        if(shotWanted("m6"))out.add(new Shot(still(id+"-m6-highwide",FilmRig.aim(at(M(),-g-L0-14,-26,19),at(M(),-4,0,1.0),50),point,1.0),"hold"));
        if(shotWanted("m7"))out.add(new Shot(still(id+"-m7-gap",FilmRig.aim(at(M(),-g-5,-14,5.5),at(M(),g,6,1.5),46),point,1.0),"hold"));
        if(shotWanted("m8"))out.add(new Shot(still(id+"-m8-tailup",FilmRig.aim(at(M(),-g-L0-5,2,1.2),at(M(),-g-L0*.4,0,3.4),52),point,.3),"hold"));
        if(shotWanted("m1b"))out.add(new Shot(still(id+"-m1b-between",FilmRig.aim(at(M(),-3.0,-4.2,1.4),at(M(),g+9,1,1.6),54),point,.3),"hold"));
        if(shotWanted("m1c"))out.add(new Shot(still(id+"-m1c-between",FilmRig.aim(at(M(),-3.0,5.0,1.4),at(M(),g+9,-1,1.6),54),point,.3),"hold"));
        if(shotWanted("m5b"))out.add(new Shot(still(id+"-m5b-hero",t->FilmRig.aim(new Vec3d(hx+fx*3.2-rx*3.0,hy+1.6,hz+fz*3.2-rz*3.0),new Vec3d(hx,hy+1.75,hz),40),point,.5),"hold"));
        if(shotWanted("m5c"))out.add(new Shot(still(id+"-m5c-hero",t->FilmRig.aim(new Vec3d(hx-fx*3.4+rx*2.4,hy+1.2,hz-fz*3.4+rz*2.4),new Vec3d(hx+fx*3,hy+1.8,hz+fz*3),42),point,.5),"hold"));
        if(shotWanted("m5d"))out.add(new Shot(still(id+"-m5d-hero",t->FilmRig.aim(new Vec3d(hx+fx*3.0+rx*3.2,hy+1.5,hz+fz*3.0+rz*3.2),new Vec3d(hx,hy+1.75,hz),40),point,.5),"hold"));
        if(shotWanted("m3b"))out.add(new Shot(still(id+"-m3b-front",FilmRig.aim(at(M(),-g+6.5,-5.5,2.0),at(M(),-g-3,-1.5,1.6),46),point,.3),"hold"));
        if(shotWanted("m3c"))out.add(new Shot(still(id+"-m3c-front",FilmRig.aim(at(M(),-g+5.5,5.5,2.0),at(M(),-g-3,2,1.6),46),point,.3),"hold"));
        boolean first=true;
        String[] stillKeys={"s1","s2","s3","s4","s5","s6","s7"};
        FilmRig.Take[] st={
            still(id+"-s1-behind",FilmRig.aim(at(S(),-12,0.6,5.2),m,42),point),
            still(id+"-s2-shoulder",FilmRig.aim(at(S(),-4.2,1.5,2.3),m,48),point),
            still(id+"-s3-hero",FilmRig.aim(at(S(),4.0,0.4,-0.7),at(S(),0,0,1.7),46),sword,.5),
            still(id+"-s4-enemy",FilmRig.aim(at(M(),tail1+9,-5,4.2),at(M(),-g-L0*.35,1.5,1.5),50),point),
            still(id+"-s5-clash",FilmRig.aim(at(M(),-9,-4,2.0),m,50),point),
            still(id+"-s6-crane",FilmRig.aim(at(M(),-(g+L0*.5)-26,-16,36),m,52),point),
            still(id+"-s7-drone",FilmRig.aim(at(S(),-10,0,16),m,50),point)};
        for(int i=0;i<st.length;i++)if(shotWanted(stillKeys[i])){out.add(new Shot(st[i],first?"clash":"none"));first=false;}
        // 180 degree panorama from the commander's eyes: stills every 15 degrees, stitched afterwards (tools in the scratchpad: stitch.py)
        if(shotWanted("p1")){Vec3d head=S().add(0,1.75,0);boolean firstPano=true;
            for(int d=-90;d<=90;d+=15){out.add(new Shot(still(String.format("%s-pano-%+04d",id,d),new double[]{head.x,head.y,head.z,face+d,0,50},point,0.5),firstPano?"hold":"none"));firstPano=false;}}
        // clips
        var armSeq=FilmRig.arm(0,0.2, 2.2,0.2, 3.4,-2.95, 5.2,-2.95, 5.9,-1.5, 999,-1.5);
        var armQuick=FilmRig.arm(0,0.2, 1.6,0.2, 2.8,-2.95, 4.3,-2.95, 5.0,-1.5, 999,-1.5);
        if(shotWanted("c1"))out.add(new Shot(clip(id+"-c1-crane",12,FilmRig.rail(12,.2,
            new Vec3d[]{at(S(),2.8,.7,1.6),at(S(),1.2,3.8,2.2),at(S(),-4.5,3.2,3.8),at(S(),-13,0,8.5)},
            new Vec3d[]{eye,at(S(),0,0,1.8),at(S(),12,0,1.2),m},new double[]{36,40,46,52}),armSeq,285,1.8),"hold"));
        if(shotWanted("c2"))out.add(new Shot(clip(id+"-c2-orbit",12,FilmRig.orbitEx(12,.2,S(),6.0,7.5,1.8,3.4,face-25,face+200,eye.add(0,-.2,0),at(S(),9,0,2.4),50,48),armQuick,280,1.8),"hold"));
        // along the column over their heads, across the clash, then looking back at it
        if(shotWanted("c3"))out.add(new Shot(clip(id+"-c3-flyover",12,FilmRig.rail(12,.2,
            new Vec3d[]{at(M(),-tail0-10,0,10),at(M(),-tail0*.55,1.5,8),at(M(),-g-2,-1,8.5),at(M(),6,-5,10),at(M(),24,-10,13)},
            new Vec3d[]{at(M(),-tail0*.55,0,1),at(M(),-g,0,1),at(M(),0,0,1.2),at(M(),2,0,1.2),at(M(),-8,0,1.4)},new double[]{62,62,64,62,58}),point,200,1.8),"hold"));
        if(shotWanted("c4"))out.add(new Shot(clip(id+"-c4-dive",10,FilmRig.rail(10,.25,
            new Vec3d[]{at(M(),-tail0-12,16,50),at(M(),-g-L0*.4,12,28),at(M(),-g-6,7,10),at(M(),-8,5,3.6)},new Vec3d[]{m,m,at(M(),-2,-3,1.8),at(M(),-1,-5,1.8)},new double[]{64,60,56,54}),point,0,1.8),"hold"));
        if(shotWanted("c5"))out.add(new Shot(clip(id+"-c5-follow",8,follow(-6.0,2.2,1.9,0,1.5,52,4.0,7.0,7.5,-3.5),point,20,1.2),"hold"));
        if(shotWanted("c6"))out.add(new Shot(clip(id+"-c6-pov",10,follow(-1.0,-1.8,1.65,14,1.2,72),point,0,1.0,Map.of(90,()->swingEvery=13,590,()->swingEvery=0)),"hold"));
        // new shots: the map view, the glide over the army, the orbit of the clash, the tracking shot, the slow-motion clash, the reverse
        if(shotWanted("c7"))out.add(new Shot(clip(id+"-c7-topdown",10,FilmRig.rail(10,.2,
            new Vec3d[]{at(M(),-8,0,74),at(M(),-8,0,62),at(M(),-10,0,50)},new Vec3d[]{at(M(),0,0,0),at(M(),0,0,0),at(M(),0,0,0)},new double[]{62,60,56}),point,120,0.5),"hold"));
        if(shotWanted("c8"))out.add(new Shot(clip(id+"-c8-glide",10,follow(18,0,7.5,16,1.0,62),point,30,1.2),"hold"));
        if(shotWanted("c9"))out.add(new Shot(clip(id+"-c9-clashorbit",10,FilmRig.orbitEx(10,.15,M(),17,11,5.5,3.4,face+165,face-40,m,m,52,46),point,60,1.0),"hold"));
        if(shotWanted("c10"))out.add(new Shot(clip(id+"-c10-sidetrack",8,follow(0,9,1.9,2,1.4,54),point,20,1.2),"hold"));
        if(shotWanted("c11"))out.add(new Shot(clipAt(120,id+"-c11-slowmo",6.5,FilmRig.orbitEx(6.5,.2,M(),10,8,2.8,2.0,face+200,face+245,m,m,50,46),point,0,0.8,Map.of()),"hold"));
        // title-screen slow motion (board 4F.3): both armies charge on recorded frame 0, slow time (GRAVES_FILM_SLOW, 4) starts
        // shortly before contact, so each clip is a short real-speed lead-in then a long slowed stretch to trim into a loop
        int sf=envInt("GRAVES_FILM_SLOW",4),slowAt=envInt("GRAVES_FILM_SLOW_AT",80);
        Map<Integer,Runnable> slowEv=Map.of(slowAt,()->FilmRig.slow=sf);
        // t1 volley: low beside our rear (archer) squad, looking up the line of fire over the column at the clash
        if(shotWanted("t1"))out.add(new Shot(clipAt(FPS,id+"-t1-volley",12,FilmRig.rail(12,.15,
            new Vec3d[]{at(M(),-tail0+4,-7,1.2),at(M(),-tail0+7,-8,1.6)},new Vec3d[]{at(M(),-g*.5,0,9),at(M(),0,0,6)},new double[]{58,54}),point,0,0.6,slowEv),"hold"));
        // t2 shield-line clash: low side view across the meeting point, slow lateral drift
        if(shotWanted("t2"))out.add(new Shot(clipAt(FPS,id+"-t2-clash",11,FilmRig.rail(11,.15,
            new Vec3d[]{at(M(),-5,-11,1.3),at(M(),2,-10,1.5)},new Vec3d[]{at(M(),-2,0,1.4),at(M(),1,0,1.5)},new double[]{46,44}),point,0,0.6,slowEv),"hold"));
        // t3 wide: high slow orbit over the clash for an establishing loop
        if(shotWanted("t3"))out.add(new Shot(clipAt(FPS,id+"-t3-wide",12,FilmRig.orbitEx(12,.15,M(),22,18,9,7,face+120,face+150,m,m,54,52),point,0,1.0,slowEv),"hold"));
        if(shotWanted("c12"))out.add(new Shot(clip(id+"-c12-reverse",12,FilmRig.rail(12,.2,
            new Vec3d[]{at(M(),tail1+10,0,10),at(M(),tail1*.55,-1.5,8),at(M(),g+2,1,8.5),at(M(),-6,5,10),at(M(),-24,10,13)},
            new Vec3d[]{at(M(),tail1*.55,0,1),at(M(),g,0,1),at(M(),0,0,1.2),at(M(),-2,0,1.2),at(M(),8,0,1.4)},new double[]{62,62,64,62,58}),point,200,1.8),"hold"));
        return out;
    }

    // ---- server ------------------------------------------------------------------------------------------------
    public void serverTick(GravesAndGlory.Battle b){
        Step s=jobs.peek();
        if(s!=null){try{if(s.run(b))jobs.poll();}catch(Exception e){Phase1Checks.report("film-exception",false,e.toString());e.printStackTrace();jobs.poll();}}
        if(!serverReady||!b.running)return;
        Vec3d m=M();
        for(var q:b.squads.values())if(q.seated){q.bot=false;q.assignments.clear();String o=q.team==0?posture0:posture1;q.order=o;q.formation=q.team==0?form0:form1;q.target=m;
            }
        var p=sp(b);if(p==null)return;
        heroOk=false;
        for(var u:b.units)if(u.squad.team==0&&u.mob.isAlive()&&u.spec.rarity().equals("legendary")){hx=u.mob.getX();hy=u.mob.getY();hz=u.mob.getZ();heroOk=true;break;}
        if(swingEvery>0&&b.time%swingEvery==0)p.swingHand(Hand.MAIN_HAND,true);
        if(protect)for(var u:b.units)if(u.squad.team==1&&u.mob.isAlive()&&u.mob.squaredDistanceTo(p)<SAFE*SAFE)u.mob.discard();
        if(p.squaredDistanceTo(sx,sy,sz)>0.25)b.relocate(p,sx,sy,sz,lookYaw,lookPitch);
        p.setHealth(p.getMaxHealth());
    }
    Step skirmish(int map,int size){return b->{
        var p=sp(b);if(p==null)return false;
        if(!Phase1Checks.skirmish(b,p,map,size,"normal"))return false;
        undo.clear();pointCount=b.points.size();
        b.world.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false,b.server);
        b.world.getGameRules().get(GameRules.DO_WEATHER_CYCLE).set(false,b.server);
        p.changeGameMode(GameMode.ADVENTURE);p.setInvulnerable(true);
        System.out.println("[film] map "+map+" ready, "+pointCount+" objectives");return true;};}
    Step atmosphere(Scene sc){return b->{
        if(sc.time()>=0)b.world.setTimeOfDay(sc.time());
        ((ServerWorld)b.world).setWeather(sc.rain()?0:20*60*30,sc.rain()?20*60*30:0,sc.rain(),false);
        return true;};}
    Step commander(Scene sc){return b->{
        var p=sp(b);if(p==null)return false;
        p.getInventory().clear();
        Item[] kit=switch(sc.armor()){
            case "netherite"->new Item[]{Items.NETHERITE_HELMET,Items.NETHERITE_CHESTPLATE,Items.NETHERITE_LEGGINGS,Items.NETHERITE_BOOTS,Items.NETHERITE_SWORD};
            case "gold"->new Item[]{Items.GOLDEN_HELMET,Items.GOLDEN_CHESTPLATE,Items.GOLDEN_LEGGINGS,Items.GOLDEN_BOOTS,Items.GOLDEN_SWORD};
            case "iron"->new Item[]{Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS,Items.IRON_SWORD};
            case "diamond"->new Item[]{Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS,Items.DIAMOND_SWORD};
            default->null;};
        if(kit!=null){p.equipStack(EquipmentSlot.HEAD,new ItemStack(kit[0]));p.equipStack(EquipmentSlot.CHEST,new ItemStack(kit[1]));p.equipStack(EquipmentSlot.LEGS,new ItemStack(kit[2]));p.equipStack(EquipmentSlot.FEET,new ItemStack(kit[3]));p.equipStack(EquipmentSlot.MAINHAND,new ItemStack(kit[4]));}
        return true;};}
    /** Puts back the last stand's blocks, then raises the hill on the scene's objective and stands the commander on its summit. */
    Step stand(Scene sc){return b->{
        for(var e:undo.entrySet())b.world.setBlockState(e.getKey(),e.getValue(),2);
        undo.clear();serverReady=false;standOk=false;
        var pt=b.points.get(Math.min(sc.point(),b.points.size()-1)).spec;
        bx=pt.x()+.5;bz=pt.z()+.5;by=Arena.ground((int)Math.floor(bx),(int)Math.floor(bz))+1;
        colL0=columnLength(b,sc,0);colL1=columnLength(b,sc,1);
        double facing=Double.NaN;int base=sc.distance()>0?sc.distance():(int)Math.ceil(sc.gap()+colL0+14),dist=base;
        for(int attempt=0;attempt<3&&Double.isNaN(facing);attempt++){dist=base+attempt*7;facing=chooseFacing(b,sc,dist,attempt>0);}
        if(Double.isNaN(facing)){System.out.println("[film] "+sc.id()+": no dry hill site; skipped");return true;}
        double yaw=Math.toRadians(facing);
        fx=-Math.sin(yaw);fz=Math.cos(yaw);rx=-Math.cos(yaw);rz=-Math.sin(yaw);
        int px=(int)Math.floor(bx-fx*dist),pz=(int)Math.floor(bz-fz*dist);
        int top=hill(b,px,pz);
        sx=px+.5;sy=top+1;sz=pz+.5;
        double dx=bx-sx,dz=bz-sz,dy=(by+.5)-(sy+1.62);
        lookYaw=(float)Math.toDegrees(Math.atan2(-dx,dz));lookPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        clearView(b);
        var p=sp(b);if(p!=null)b.relocate(p,sx,sy,sz,lookYaw,lookPitch);
        int when=sc.time()>=0?sc.time():(fx<0?11900:300);
        if(sc.time()<0)b.world.setTimeOfDay(when);
        if(when>=13000&&when<=23000)lanterns(b);
        standOk=true;
        System.out.println(String.format("[film] %s stand: facing %.1f dist %d summit %d at %.1f %.1f, fight at %.1f %.1f %.1f, columns %.1f and %.1f long, field cost %.0f wet %d",sc.id(),facing,dist,top,sx,sz,bx,by,bz,colL0,colL1,fieldCost,fieldWet));
        return true;};}
    /** One squad of a column: its troops front to back (hero, melee, ranged), the ranks they fill and where its front rank stands, measured from the objective. */
    record Rank(GravesAndGlory.Squad squad,List<Spec.TroopsRow> troops,int rows,int body,double start){}
    static boolean legendary(Spec.TroopsRow t){return t.rarity().equals("legendary");}
    /** The squads of one army stacked one behind another along the line of the charge: the hero's squad leads, archers last. */
    List<Rank> lineup(GravesAndGlory.Battle b,Scene sc,int team){
        String[] banners=team==0?sc.dawn():sc.dusk();
        var armies=b.squads.values().stream().filter(s->s.seated&&s.team==team).toList();
        List<Rank> raw=new ArrayList<>();
        for(int k=0;k<armies.size();k++){
            List<Spec.TroopsRow> row=new ArrayList<>();
            for(String id:Campaign.bannerRow(banners[k%banners.length])){var spec=Campaign.troop(id);if(spec!=null)row.add(spec);}
            row.sort(Comparator.comparingInt(x->legendary(x)?0:x.ranged()?2:1));
            int body=(int)row.stream().filter(x->!legendary(x)).count();
            raw.add(new Rank(armies.get(k),row,Math.max(1,(body+sc.cols()-1)/sc.cols()),body,0));
        }
        raw.sort(Comparator.comparingDouble(r->(r.troops().stream().anyMatch(MediaShoot::legendary)?-1.0:0.0)+r.troops().stream().filter(Spec.TroopsRow::ranged).count()/(double)Math.max(1,r.troops().size())));
        List<Rank> out=new ArrayList<>();double at=sc.gap();
        for(var r:raw){out.add(new Rank(r.squad(),r.troops(),r.rows(),r.body(),at));at+=r.rows()*sc.spacing()+SQGAP;}
        return out;
    }
    double columnLength(GravesAndGlory.Battle b,Scene sc,int team){var l=lineup(b,sc,team);if(l.isEmpty())return 0;var last=l.get(l.size()-1);return last.start()+last.rows()*sc.spacing()-sc.gap();}
    Step muster(Scene sc,String posture){return b->{
        if(!standOk)return true;
        Projectiles.clear(b);for(var u:b.units)u.mob.discard();b.units.clear();
        double ax=fx,az=fz,qx=-az,qz=ax;
        posture0=posture;posture1=posture;
        for(int t=0;t<2;t++){int side=t==0?-1:1;
            for(var r:lineup(b,sc,t)){var s=r.squad();int slot=0;
                s.bot=false;s.assignments.clear();s.order=posture;s.formation="line";s.target=new Vec3d(bx,by,bz);
                s.yaw=(float)Math.toDegrees(Math.atan2(side*ax,-side*az));
                for(var spec:r.troops()){
                    boolean hero=legendary(spec);int row=slot/sc.cols(),inRow=Math.min(sc.cols(),r.body()-row*sc.cols());
                    double depth=hero?r.start()-HEROLEAD:r.start()+row*sc.spacing(),across=hero?0:((slot%sc.cols())-(inRow-1)/2.0)*COLSP;
                    double x=bx+side*ax*depth+qx*across,z=bz+side*az*depth+qz*across;
                    b.spawn(s,spec,slot,new Vec3d(x,Arena.ground((int)Math.floor(x),(int)Math.floor(z))+1,z));if(!hero)slot++;
                }
                for(String group:Morale.GROUPS)Morale.fielded(b,s,group);
            }
        }
        serverReady=true;
        System.out.println("[film] muster ("+posture+"): "+b.units.size()+" troops, round running "+b.running);return true;};}
    /** The horn and the charge, from the client's frame events. */
    void release(){jobs.add(b->{posture0="charge";posture1="charge";
        for(var q:b.squads.values())if(q.seated){boolean go=b.startsCharge(q,"all");q.order="charge";if(go)b.markCharge(q,"all");}
        return true;});}
    void lanterns(GravesAndGlory.Battle b){
        for(var p:b.points){var at=p.spec;for(int dx:new int[]{-3,3})for(int dz:new int[]{-3,3}){int x=at.x()+dx,z=at.z()+dz,y=Arena.ground(x,z)+1;
            set(x,y,z,Blocks.DARK_OAK_FENCE,b);set(x,y+1,z,Blocks.LANTERN,b);}}
    }

    // ---- the hill (from TitleDemos, with the site chosen from the objective instead of a fixed table) ----------------
    double chooseFacing(GravesAndGlory.Battle b,Scene sc,int dist,boolean relaxed){
        double best=Double.NaN;double bestScore=Double.MAX_VALUE;
        for(double yaw=0;yaw<360;yaw+=5){
            double t=Math.toRadians(yaw),ffx=-Math.sin(t),ffz=Math.cos(t);
            int px=(int)Math.floor(bx-ffx*dist),pz=(int)Math.floor(bz-ffz*dist);
            if(dry(px,pz,relaxed)!=null)continue;
            int blocked=obstructions(b,px+.5,Arena.ground(px,pz)+HILL_HEIGHT+3.7,pz+.5,ffx,ffz,bx,by+1.2,bz);
            int[] f=field(b,sc,ffx,ffz,dist);
            double score=blocked+2*rear(b,px,pz,ffx,ffz)+3.0*(f[0]*10+f[1]*2+f[2]);
            if(score<bestScore){bestScore=score;best=yaw;fieldCost=f[0]*10+f[1]*2+f[2];fieldWet=f[0];}
        }
        return best;
    }
    /** What the two columns would stand and run on, from the commander's hill to the far end of the enemy column: {water cells, blocked cells (buildings, rocks, trees), steps of more than two blocks}. */
    int[] field(GravesAndGlory.Battle b,Scene sc,double ffx,double ffz,int dist){
        double half=sc.cols()*COLSP/2+3.5,far=sc.gap()+colL1+6;int wet=0,blocked=0,steep=0;
        for(double f=-(dist-8);f<=far;f+=2){int last=Integer.MIN_VALUE;
            for(double w=-half;w<=half;w+=2){
                int x=(int)Math.floor(bx+ffx*f-ffz*w),z=(int)Math.floor(bz+ffz*f+ffx*w),g=Arena.ground(x,z);
                if(GravesAndGlory.Battle.wet(x,z))wet++;
                var st=b.world.getBlockState(new BlockPos(x,g+1,z));
                if(!st.isAir()&&!st.isReplaceable()&&!st.isIn(BlockTags.FLOWERS)&&st.getFluidState().isEmpty())blocked++;
                for(var s:Spec.STRUCTURES)if(!s.kind().equals("bridge")&&Math.abs(x-s.x())<=s.width()/2+2&&Math.abs(z-s.z())<=s.depth()/2+2){blocked+=2;break;}
                if(last!=Integer.MIN_VALUE&&Math.abs(g-last)>2)steep++;
                last=g;}}
        return new int[]{wet,blocked,steep};
    }
    /** Solid blocks in the open box behind the commander's hill (buildings, cliffs): what the camera sees when it swings round to his face. */
    static int rear(GravesAndGlory.Battle b,int px,int pz,double ffx,double ffz){
        int n=0,top=Arena.ground(px,pz)+HILL_HEIGHT;
        for(int d=5;d<=26;d+=3)for(int w=-9;w<=9;w+=3)for(int h=1;h<=9;h+=2){
            var st=b.world.getBlockState(BlockPos.ofFloored(px+.5-ffx*d-ffz*w,top+h,pz+.5-ffz*d+ffx*w));
            if(st.isAir()||st.isReplaceable()||st.isIn(BlockTags.LOGS)||st.isIn(BlockTags.LEAVES)||st.isIn(BlockTags.FLOWERS)||!st.getFluidState().isEmpty())continue;
            n++;}
        return n;
    }
    static String dry(int px,int pz,boolean relaxed){
        int g=Arena.ground(px,pz);
        for(int x=px-12;x<=px+12;x++)for(int z=pz-12;z<=pz+12;z++){if((x-px)*(x-px)+(z-pz)*(z-pz)>144)continue;
            if(Arena.river(x,z))return "river";
            for(var s:Spec.STRUCTURES)if(Math.abs(x-s.x())<=s.width()/2+3&&Math.abs(z-s.z())<=s.depth()/2+3)return s.id();
            if(!relaxed&&Math.abs(Arena.ground(x,z)-g)>3)return "uneven";}
        return null;
    }
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
    void clearView(GravesAndGlory.Battle b){
        double len=Math.hypot(bx-sx,bz-sz)+scene.gap()+colL1+6;
        for(double f=0;f<=len;f+=.8){for(int w=-10;w<=10;w++){int x=(int)Math.floor(sx+fx*f+rx*w),z=(int)Math.floor(sz+fz*f+rz*w);
            for(int y=60;y<110;y++){var pos=new BlockPos(x,y,z);var st=b.world.getBlockState(pos);if(st.isIn(BlockTags.LOGS)||st.isIn(BlockTags.LEAVES))set(pos,Blocks.AIR.getDefaultState(),b);}}}
    }
    static long key(int x,int z){return((long)x<<32)^(z&0xffffffffL);}
    static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    static int hash(int x,int z,int salt){return Math.floorMod(x*7349+z*9151+x*z*31+salt*104729,100);}
    static Block stone(int x,int y,int z){int k=hash(x+y*13,z-y*7,3);return k<60?Blocks.STONE:k<80?Blocks.ANDESITE:k<92?Blocks.COBBLESTONE:Blocks.MOSSY_COBBLESTONE;}
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
        int lx=(int)Math.floor(cx-fx*4.5-rx*3),lz=(int)Math.floor(cz-fz*4.5-rz*3),ly=tops.getOrDefault(key(lx,lz),Arena.ground(lx,lz));
        for(int a=1;a<=2;a++)set(lx,ly+a,lz,Blocks.DARK_OAK_FENCE,b);
        set(lx,ly+3,lz,Blocks.LANTERN,b);
        for(var e:tops.entrySet()){int x=(int)(e.getKey()>>32),z=e.getKey().intValue(),top=e.getValue();
            double dx=x+.5-cx,dz=z+.5-cz,f=dx*fx+dz*fz,q=dx*rx+dz*rz,d=Math.hypot(f/(f>0?9.0:10.5),q/9.0);
            if(onPath.contains(e.getKey())||d<.45||Math.abs(x-lx)+Math.abs(z-lz)<=1)continue;
            var pos=new BlockPos(x,top+1,z);if(!w.getBlockState(pos.down()).isOf(Blocks.GRASS_BLOCK))continue;
            int k=hash(x,z,7);
            if(k<16)set(pos,Blocks.SHORT_GRASS.getDefaultState(),b);
            else if(k<19)set(pos,Blocks.FERN.getDefaultState(),b);
            else if(k<21){set(pos,Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF,DoubleBlockHalf.LOWER),b);set(pos.up(),Blocks.TALL_GRASS.getDefaultState().with(TallPlantBlock.HALF,DoubleBlockHalf.UPPER),b);}
            else if(k==21)set(pos,Blocks.POPPY.getDefaultState(),b);
            else if(k==22)set(pos,Blocks.DANDELION.getDefaultState(),b);
        }
        return summit;
    }
    static double segment(double x,double y,double ax,double ay,double bx,double by){
        double vx=bx-ax,vy=by-ay,t=Math.max(0,Math.min(1,((x-ax)*vx+(y-ay)*vy)/Math.max(1e-9,vx*vx+vy*vy)));
        return Math.hypot(x-ax-t*vx,y-ay-t*vy);
    }
}
