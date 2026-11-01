package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.level.LevelInfo;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/** Unshipped Map Forge walkthrough: puts an exported Map Forge surface (test-tools/standalone/mapwalk_export.py) into a plain flat test world
 * and photographs it from fixed spectator cameras, with the HUD hidden and full brightness. -PmapWalk only; it never touches a Graves and Glory save. */
public final class MapWalk implements ClientModInitializer {
    record View(String name,double x,double y,double z,float yaw,float pitch){}
    static final int BATCH=60000;
    int ticks,placed,wait,view,settle;boolean hosted,loaded,done;
    final List<String> palette=new ArrayList<>();final List<int[]> blocks=new ArrayList<>();List<View> views;String tag;
    static View look(String name,double x,double y,double z,double tx,double ty,double tz){
        double dx=tx-x,dy=ty-y,dz=tz-z;return new View(name,x,y,z,(float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    /** name|x|y|z|tx|ty|tz per line in the walk plan file: a camera at x y z looking at tx ty tz. */
    static List<View> plan(File f)throws Exception{
        var v=new ArrayList<View>();
        for(String line:Files.readAllLines(f.toPath())){line=line.trim();if(line.isEmpty()||line.startsWith("#"))continue;String[] p=line.split("\\|");
            v.add(look(p[0],Double.parseDouble(p[1]),Double.parseDouble(p[2]),Double.parseDouble(p[3]),Double.parseDouble(p[4]),Double.parseDouble(p[5]),Double.parseDouble(p[6])));}
        return v;
    }
    void read(File f)throws Exception{
        for(String line:Files.readAllLines(f.toPath())){
            if(line.startsWith("P ")){String[] p=line.split(" ",3);int i=Integer.parseInt(p[1]);while(palette.size()<=i)palette.add(null);palette.set(i,p[2]);}
            else if(line.startsWith("B ")){String[] p=line.split(" ");blocks.add(new int[]{Integer.parseInt(p[1]),Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4])});}
        }
    }
    void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"walk-"+tag+"-"+name+".png",c.getFramebuffer(),t->{});}
    void camera(MinecraftClient c,View v){var id=c.player.getUuid();c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(id);if(p!=null){p.changeGameMode(GameMode.SPECTATOR);p.teleport(c.getServer().getOverworld(),v.x(),v.y(),v.z(),v.yaw(),v.pitch());}});}
    @Override public void onInitializeClient(){
        if(!Boolean.getBoolean("gravesandglory.mapWalk"))return;
        tag=System.getProperty("gravesandglory.walkTag","now");
        File surface=new File(System.getProperty("gravesandglory.walkFile")),planFile=new File(System.getProperty("gravesandglory.walkPlan"));
        try{read(surface);views=plan(planFile);}catch(Exception e){e.printStackTrace();throw new IllegalStateException(e);}
        System.out.println("[Map walk] "+blocks.size()+" blocks, "+palette.size()+" states, "+views.size()+" views");
        var states=new BlockState[palette.size()];
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.getOverlay()!=null)return;
            if(c.world==null){
                if(!hosted&&++ticks>60){hosted=true;
                    var info=new LevelInfo("MapWalk",GameMode.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),DataConfiguration.SAFE_MODE);
                    c.createIntegratedServerLoader().createAndStart("MapWalk-"+tag,info,new GeneratorOptions(1L,false,false),r->r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder());}
                return;
            }
            var server=c.getServer();if(server==null||c.player==null)return;
            c.options.pauseOnLostFocus=false;c.options.hudHidden=true;c.options.getViewDistance().setValue(32);c.options.getGamma().setValue(15.0);
            c.options.getCloudRenderMode().setValue(net.minecraft.client.option.CloudRenderMode.OFF);
            if(done)return;
            if(!loaded){
                if(placed==0){
                    // Quiet, bright, peaceful; the camera stays out of the map until it is built.
                    server.execute(()->{var w=server.getOverworld();var r=w.getGameRules();
                        r.get(GameRules.DO_DAYLIGHT_CYCLE).set(false,server);r.get(GameRules.DO_WEATHER_CYCLE).set(false,server);r.get(GameRules.DO_MOB_SPAWNING).set(false,server);
                        w.setTimeOfDay(6000);w.setWeather(0,0,false,false);
                        for(int cx=-23;cx<=22;cx++)for(int cz=-16;cz<=16;cz++)w.setChunkForced(cx,cz,true);});
                    for(int i=0;i<palette.size();i++){try{states[i]=BlockArgumentParser.block(Registries.BLOCK.getReadOnlyWrapper(),palette.get(i),false).blockState();}catch(Exception e){System.out.println("[Map walk] bad state "+palette.get(i)+": "+e.getMessage());states[i]=net.minecraft.block.Blocks.STONE.getDefaultState();}}
                    placed=1;return;
                }
                if(++settle<100)return;
                final int from=placed-1,to=Math.min(blocks.size(),from+BATCH);
                server.execute(()->{var w=server.getOverworld();var pos=new BlockPos.Mutable();
                    for(int i=from;i<to;i++){var b=blocks.get(i);pos.set(b[0],b[1],b[2]);w.setBlockState(pos,states[b[3]],Block.FORCE_STATE|Block.NOTIFY_LISTENERS);}});
                placed=to+1;
                if(to>=blocks.size()){loaded=true;settle=0;System.out.println("[Map walk] placed "+to+" blocks");}
                return;
            }
            // Give the light thread and the chunk packets time before the first camera.
            if(settle<400){settle++;return;}
            if(view>=views.size()){System.out.println("[Map walk] COMPLETE");done=true;c.scheduleStop();return;}
            wait++;
            if(wait==1||wait==40)camera(c,views.get(view));
            if(wait>=60&&c.currentScreen!=null)c.setScreen(null);
            if(wait==150){shot(c,views.get(view).name());System.out.println("[Map walk] "+views.get(view).name());view++;wait=0;}
        });
    }
}
