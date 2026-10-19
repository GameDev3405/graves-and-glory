package gg.gravesandglory;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Unshipped storybook still photographer, GRAVES_PHASE1_CHECKS=storystills. Starts a practice round on a chosen map with
 *  every unit and bot purchase removed, so the battlefield is an empty set; then plays control files dropped in
 *  <test root>/control/*.cmd, one line at a time: server commands (/time, /weather, /fill, /setblock, /summon, /kill)
 *  run at operator level through the game's own dispatcher, and harness lines place the camera (the title demos'
 *  free-camera hook, TitleDemos.camera, with the player a spectator at the same spot so chunks and entities load),
 *  pose actors (testing/mixin/StoryStillsPoseMixin, keyed by entity id) and take 1920x1080 screenshots with the HUD,
 *  chat, hand and crosshair hidden. Every line and its result is written to <file>.log beside the control file.
 *  Never included in releases. */
public final class StoryStills implements Phase1Checks.Suite,Phase1Checks.ServerWatch,ClientModInitializer {
    @Override public void onInitializeClient(){Phase1Checks.SUITES.put("storystills",StoryStills::new);}
    /** Read by StoryStillsPoseMixin on the render thread: entity id, then part name (head, body, rarm, larm, rleg, lleg)
     *  to {pitch, yaw, roll} in radians, set after the model's own setAngles. */
    public static final Map<Integer,Map<String,float[]>> POSES=new ConcurrentHashMap<>();
    static final int WIDTH=1920,HEIGHT=1080,LIMIT=20*60*28; // the suite stops the client itself after 28 minutes

    /** Server side: the map to start (or -1), whether the empty round is up, and where the spectator stands (x y z yaw pitch). */
    volatile int wantMap=-1;volatile boolean mapStarted,neutral;volatile UUID player;volatile double[] stand;
    /** Client side: the control file being played and its remaining lines. */
    final ArrayDeque<String> lines=new ArrayDeque<>();Path playing;final StringBuilder log=new StringBuilder();
    int waitUntil,mapAsked=-1;volatile boolean serverBusy;boolean finished;

    static final class Said implements CommandOutput {
        final List<String> lines=new ArrayList<>();
        @Override public void sendMessage(Text message){lines.add(message.getString());}
        @Override public boolean shouldReceiveFeedback(){return true;}
        @Override public boolean shouldTrackOutput(){return true;}
        @Override public boolean shouldBroadcastConsoleToOps(){return false;}
    }
    Path control(MinecraftClient c){return c.runDirectory.toPath().resolve("../control").normalize();}
    void note(String line){synchronized(log){log.append(line).append('\n');}System.out.println("[sb] "+line);}

    // ---- client ---------------------------------------------------------------------------------------------
    public void step(MinecraftClient c,int tick){
        if(tick==2||tick==40){long h=c.getWindow().getHandle(); // borderless at the screen's corner, as the title demos
            GLFW.glfwSetWindowAttrib(h,GLFW.GLFW_DECORATED,GLFW.GLFW_FALSE);GLFW.glfwSetWindowPos(h,0,0);GLFW.glfwSetWindowSize(h,WIDTH,HEIGHT);}
        if(tick==20){player=c.player.getUuid();c.options.getBobView().setValue(false);c.options.getFovEffectScale().setValue(0.0);
            c.options.getEntityDistanceScaling().setValue(5.0);try{Files.createDirectories(control(c));}catch(Exception e){note("control dir: "+e);}
            note("ready; control directory "+control(c));}
        if(tick<20||finished)return;
        hold(c);
        if(tick>LIMIT){note("time limit reached");done(c);return;}
        if(serverBusy||tick<waitUntil)return;
        if(mapAsked>=0){if(!mapStarted)return;note("map "+mapAsked+" built; empty practice round running, player spectating");mapAsked=-1;}
        if(lines.isEmpty()){if(playing!=null)close();if(tick%10==0)poll(c);return;}
        // Several instant lines run in one tick; a wait, a server line or a shot ends the tick.
        for(int n=0;n<64&&!lines.isEmpty()&&!serverBusy&&tick>=waitUntil&&mapAsked<0&&!finished;n++){
            String line=lines.poll().trim();
            if(line.isEmpty()||line.startsWith("#"))continue;
            note("> "+line);
            try{if(run(c,line,tick))break;}catch(Exception e){note("ERROR "+e);e.printStackTrace();}
        }
    }
    /** Every tick: no HUD, chat, hand or crosshair; first person; no screen, toast or entrance cinematic. */
    void hold(MinecraftClient c){
        c.options.hudHidden=true;c.options.pauseOnLostFocus=false;
        if(CampaignClient.cinematic())CampaignClient.end(c);
        if(c.options.getPerspective()!=Perspective.FIRST_PERSON)c.options.setPerspective(Perspective.FIRST_PERSON);
        if(c.currentScreen!=null)c.setScreen(null);
        c.getToastManager().clear();
    }
    void poll(MinecraftClient c){
        try(var list=Files.list(control(c))){
            var next=list.filter(p->p.getFileName().toString().endsWith(".cmd")).sorted().findFirst().orElse(null);
            if(next==null)return;
            var lines=Files.readAllLines(next);Path run=next.resolveSibling(next.getFileName().toString().replaceAll("\\.cmd$",".run"));
            Files.move(next,run,StandardCopyOption.REPLACE_EXISTING);playing=run;this.lines.addAll(lines);
            synchronized(log){log.setLength(0);}note("playing "+next.getFileName()+" ("+lines.size()+" lines)");
        }catch(Exception e){note("poll: "+e);}
    }
    void close(){
        try{String base=playing.getFileName().toString().replaceAll("\\.run$","");
            synchronized(log){Files.writeString(playing.resolveSibling(base+".log"),log.toString());}
            Files.move(playing,playing.resolveSibling(base+".done"),StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception e){System.out.println("[sb] close: "+e);}playing=null;
    }
    void done(MinecraftClient c){if(finished)return;finished=true;if(playing!=null)close();Phase1Checks.finish(c,"storystills");}
    public void cleanup(MinecraftClient c){TitleDemos.camera=null;POSES.clear();c.options.hudHidden=false;}

    /** Runs one line. Returns true when the rest of the file must wait for a later tick. */
    boolean run(MinecraftClient c,String line,int tick)throws Exception{
        if(line.startsWith("/")){server(c,line.substring(1));return true;}
        String[] a=line.split("\\s+");
        switch(a[0]){
            case "map"->{int m=Integer.parseInt(a[1]);mapStarted=false;neutral=false;mapAsked=m;wantMap=m;return true;}
            case "wait"->{waitUntil=tick+Integer.parseInt(a[1]);return true;}
            case "cam"->{camera(c,d(a[1]),d(a[2]),d(a[3]),(float)d(a[4]),(float)d(a[5]));return false;}
            case "look"->{double x=d(a[1]),y=d(a[2]),z=d(a[3]),dx=d(a[4])-x,dy=d(a[5])-y,dz=d(a[6])-z;
                camera(c,x,y,z,(float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));return false;}
            case "fov"->{c.options.getFov().setValue(Integer.parseInt(a[1]));return false;}
            case "rd"->{c.options.getViewDistance().setValue(Integer.parseInt(a[1]));return false;}
            case "shot"->{shot(c,a[1]);return true;}
            case "pose"->{pose(c,a);return true;}
            case "unpose"->{POSES.clear();return false;}
            case "shaderopt"->{shaderOptions(c,a);return true;}
            case "probe"->{probe(c,a);return true;}
            case "topdown"->{topDown(c,a[1]);return false;}
            case "quit"->{done(c);return true;}
            default->{note("unknown line");return false;}
        }
    }
    static double d(String s){return Double.parseDouble(s);}
    /** The free camera at the exact spot, and the spectator player there too (feet 1.62 below the eye) so the server sends
     *  the chunks and entities around it. */
    void camera(MinecraftClient c,double x,double y,double z,float yaw,float pitch){
        TitleDemos.camera=new double[]{x,y,z,yaw,pitch};stand=new double[]{x,y-1.62,z,yaw,pitch};
        note(String.format(Locale.ROOT,"camera %.2f %.2f %.2f yaw %.2f pitch %.2f",x,y,z,yaw,pitch));
    }
    void shot(MinecraftClient c,String name){
        int w=c.getWindow().getFramebufferWidth(),h=c.getWindow().getFramebufferHeight();
        ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),t->{});
        double[] v=TitleDemos.camera;
        note(String.format(Locale.ROOT,"shot %s %dx%d time=%d rain=%.2f fov=%d rd=%d shader=%d camera=%s",name,w,h,c.world.getTimeOfDay(),c.world.getRainGradient(1),
            c.options.getFov().getValue(),c.options.getViewDistance().getValue(),Presentation.shader(),v==null?"none":String.format(Locale.ROOT,"%.2f %.2f %.2f %.2f %.2f",v[0],v[1],v[2],v[3],v[4])));
    }
    /** pose <tag> <part> <pitch> <yaw> <roll> [<part> <pitch> <yaw> <roll>]... in degrees, for every entity carrying the tag. */
    void pose(MinecraftClient c,String[] a){
        String tag=a[1];Map<String,float[]> parts=new HashMap<>();
        for(int i=2;i+3<a.length;i+=4)parts.put(a[i],new float[]{(float)Math.toRadians(d(a[i+1])),(float)Math.toRadians(d(a[i+2])),(float)Math.toRadians(d(a[i+3]))});
        serverBusy=true;
        c.getServer().execute(()->{try{int n=0;
            for(Entity e:GravesAndGlory.battle.world.iterateEntities())if(e.getCommandTags().contains(tag)){POSES.computeIfAbsent(e.getId(),k->new ConcurrentHashMap<>()).putAll(parts);n++;}
            note("posed "+n+" entities tagged "+tag);}catch(Exception e){note("ERROR pose "+e);}finally{serverBusy=false;}});
    }
    void server(MinecraftClient c,String command){
        serverBusy=true;
        c.getServer().execute(()->{
            try{var b=GravesAndGlory.battle;var said=new Said();
                int r=b.server.getCommandManager().getDispatcher().execute(command,b.server.getCommandSource().withLevel(4).withOutput(said));
                note("  = "+r+" "+String.join(" | ",said.lines));
            }catch(CommandSyntaxException e){note("  ERROR "+e.getMessage());}
            catch(Exception e){note("  ERROR "+e);}
            finally{serverBusy=false;}
        });
    }
    /** shaderopt KEY=VALUE ... writes the pack's own option file (as its settings screen does) and reloads Iris. VALUE "default" removes the key. */
    void shaderOptions(MinecraftClient c,String[] a)throws Exception{
        Path f=c.runDirectory.toPath().resolve("shaderpacks/ComplementaryReimagined_r5.5.1.zip.txt");Properties p=new Properties();
        if(Files.exists(f))try(var in=Files.newInputStream(f)){p.load(in);}
        for(int i=1;i<a.length;i++){String[] kv=a[i].split("=",2);if(kv[1].equals("default"))p.remove(kv[0]);else p.setProperty(kv[0],kv[1]);}
        try(var out=Files.newOutputStream(f)){p.store(out,null);}
        Class<?> iris=Class.forName("net.irisshaders.iris.Iris");iris.getMethod("reload").invoke(null);
        note("shader options now "+p+"; Iris reloaded, pack index "+Presentation.shader());
    }
    /** probe x1 z1 x2 z2 step: the top solid block (y and name) on a grid, from the server world. */
    void probe(MinecraftClient c,String[] a){
        int x1=Integer.parseInt(a[1]),z1=Integer.parseInt(a[2]),x2=Integer.parseInt(a[3]),z2=Integer.parseInt(a[4]),s=Integer.parseInt(a[5]);
        serverBusy=true;
        c.getServer().execute(()->{try{var w=GravesAndGlory.battle.world;StringBuilder sb=new StringBuilder();
            for(int z=z1;z<=z2;z+=s){sb.append(String.format(Locale.ROOT,"z%5d:",z));
                for(int x=x1;x<=x2;x+=s){int y=w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,x,z)-1;var st=w.getBlockState(new BlockPos(x,y,z));
                    String id=Registries.BLOCK.getId(st.getBlock()).getPath();sb.append(String.format(Locale.ROOT," %d/%s",y,id.length()>8?id.substring(0,8):id));}
                sb.append('\n');}
            note("probe x "+x1+".."+x2+" z "+z1+".."+z2+" step "+s+"\n"+sb);}catch(Exception e){note("ERROR probe "+e);}finally{serverBusy=false;}});
    }
    /** One pixel per block from the tactical map's own colours, as MapTour draws it, into screenshots/<name>.png. */
    void topDown(MinecraftClient c,String name){
        int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ();var img=new BufferedImage(hx*2+1,hz*2+1,BufferedImage.TYPE_INT_RGB);
        for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++)img.setRGB(x+hx,z+hz,Arena.mapColor(x,z)&0xffffff);
        try{var out=c.runDirectory.toPath().resolve("screenshots/"+name+".png");Files.createDirectories(out.getParent());javax.imageio.ImageIO.write(img,"png",out.toFile());note("top-down "+out);}catch(Exception e){note("ERROR topdown "+e);}
    }

    // ---- server ---------------------------------------------------------------------------------------------
    public void serverTick(GravesAndGlory.Battle b){
        ServerPlayerEntity p=player==null?null:b.server.getPlayerManager().getPlayer(player);
        if(p==null)return;
        if(wantMap>=0){
            if(Phase1Checks.skirmish(b,p,wantMap,1,"easy")){b.roundSeconds=1_000_000;neutral=true;wantMap=-1;mapStarted=true;}
            return;
        }
        // An empty set: no troop of either army, no bot purchase, no score and no round end.
        if(neutral&&b.running){
            if(!b.units.isEmpty()){for(var u:b.units)u.mob.discard();b.units.clear();}
            Projectiles.clear(b);
            for(var s:b.squads.values()){s.training.clear();if(s.bot)s.gold=0;}
            b.scores[0]=0;b.scores[1]=0;
        }
        if(mapStarted){
            if(!p.isSpectator())p.changeGameMode(GameMode.SPECTATOR);
            double[] s=stand;
            if(s!=null&&p.squaredDistanceTo(s[0],s[1],s[2])>0.0025)b.relocate(p,s[0],s[1],s[2],(float)s[3],(float)s[4]);
        }
    }
}
