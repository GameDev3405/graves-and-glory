package gg.gravesandglory;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.nio.file.*;

/** File-driven shots in disposable development instances, never enabled for players. */
final class CampaignDevControl {
    static long last;static int ticks;static MapRouteChecks routes;
    static void tick(MinecraftClient c){
        if(!FabricLoader.getInstance().isDevelopmentEnvironment()||System.getProperty("gravesandglory.localChecks")==null||c.player==null||c.getServer()==null||++ticks%10!=0)return;
        if(routes!=null&&!routes.done)c.getServer().execute(routes::tick);
        Path control=c.runDirectory.toPath().resolve("../evidence/control.json");
        try{long at=Files.getLastModifiedTime(control).toMillis();if(at==last)return;last=at;JsonObject j=JsonParser.parseString(Files.readString(control)).getAsJsonObject();String action=j.get("action").getAsString();
            if(action.equals("preview")){
                c.setScreen(null);c.options.hudHidden=true;
                var camera=new net.minecraft.entity.decoration.ArmorStandEntity(c.world,-70,100,-195);
                camera.refreshPositionAndAngles(-70,100,-195,-30,22);camera.prevX=camera.getX();camera.prevY=camera.getY();camera.prevZ=camera.getZ();camera.prevYaw=-30;camera.prevPitch=22;
                c.setCameraEntity(camera);return;
            }
            if(action.equals("restore")){c.setCameraEntity(c.player);c.options.hudHidden=false;return;}
            if(action.equals("quit")){c.scheduleStop();return;}
            if(action.equals("hud")){c.options.hudHidden=j.get("hidden").getAsBoolean();return;}
            if(action.equals("barracks")){c.setScreen(new GravesAndGloryClient.BarracksScreen());return;}
            if(action.equals("resize")){org.lwjgl.glfw.GLFW.glfwSetWindowSize(c.getWindow().getHandle(),j.get("width").getAsInt(),j.get("height").getAsInt());return;}
            if(action.equals("routes")){int[] maps=null;if(j.has("maps")){var list=j.getAsJsonArray("maps");maps=new int[list.size()];for(int i=0;i<maps.length;i++)maps[i]=list.get(i).getAsInt();}routes=new MapRouteChecks(c,maps);return;}
            if(action.equals("shot")){ScreenshotRecorder.saveScreenshot(c.runDirectory,j.get("name").getAsString()+".png",c.getFramebuffer(),text->{});return;}
            if(action.equals("presentation")){c.setScreen(new PresentationScreen());return;}
            if(action.equals("shader")){Presentation.selectShader(j.get("index").getAsInt());return;}
            if(action.equals("impact")){Presentation.impact(1);return;}
            if(action.equals("ui")){var s=new GravesAndGloryClient.LobbyScreen();c.setScreen(s);s.showTab(j.has("tab")?j.get("tab").getAsInt():0);return;}
            if(action.equals("perspective")){c.options.setPerspective(j.get("value").getAsString().equals("front")?net.minecraft.client.option.Perspective.THIRD_PERSON_FRONT:net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);return;}
            if(action.equals("visual")||action.equals("visual-clear")){c.setScreen(null);c.options.hudHidden=true;c.options.setPerspective(net.minecraft.client.option.Perspective.THIRD_PERSON_BACK);c.getServer().execute(()->visualFixture(c,action.equals("visual")));return;}
            c.setScreen(null);c.getServer().execute(()->{var b=GravesAndGlory.battle;if(b==null)return;var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());
                if(action.equals("map")){b.campaign.returnLobby();b.campaign.handle(p,"map:"+j.get("index").getAsInt());}
                if(action.equals("start")){b.campaign.handle(p,"mode:"+(j.has("mode")?j.get("mode").getAsString():"4"));b.campaign.handle(p,"start");}
                if(action.equals("lobby"))b.campaign.returnLobby();
                if(action.equals("view")){p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,j.get("x").getAsDouble(),j.get("y").getAsDouble(),j.get("z").getAsDouble(),j.get("yaw").getAsFloat(),j.get("pitch").getAsFloat());}
                if(action.equals("battle")){
                    if(!b.running)b.startRound();b.campaign.introUntil=b.time;int n=0;
                    for(var s:b.squads.values()){int sign=s.team==0?-1:1;int lane=n++/2;double sx=sign*20,z=(lane-1)*28;for(var u:b.units)if(u.squad==s){double x=sx+sign*(u.slot/5)*2,dz=z+(u.slot%5-2)*2;u.mob.refreshPositionAndAngles(x,Arena.ground((int)x,(int)dz)+1,dz,0,0);}s.assignments.clear();s.order="attack";s.target=new Vec3d(0,65,z);}
                    p.changeGameMode(GameMode.SPECTATOR);p.teleport(b.world,-26,78,22,-132,24);
                }
            });
        }catch(java.nio.file.NoSuchFileException ignored){}catch(Exception e){System.err.println("[Graves and Glory dev control] "+e);}
    }
    /** Dev-only 3D check: a husk, a skeleton and a wither skeleton stand five blocks ahead of the player with AI off. */
    static void visualFixture(MinecraftClient c,boolean spawn){
        var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());
        if(p==null)return;
        var world=(net.minecraft.server.world.ServerWorld)p.getWorld();
        if(!spawn){for(var e:world.iterateEntities())if(e.getCommandTags().contains("visual-check"))e.discard();return;}
        double yaw=Math.toRadians(p.getYaw());
        double fx=-Math.sin(yaw),fz=Math.cos(yaw),rx=-Math.cos(yaw),rz=-Math.sin(yaw);
        double bx=p.getX()+fx*5,bz=p.getZ()+fz*5;
        var types=new net.minecraft.entity.EntityType<?>[]{net.minecraft.entity.EntityType.HUSK,net.minecraft.entity.EntityType.SKELETON,net.minecraft.entity.EntityType.WITHER_SKELETON};
        double[] side={-2.2,0,2.2};
        for(int i=0;i<types.length;i++){
            var mob=(net.minecraft.entity.mob.MobEntity)types[i].create(world);
            if(mob==null)continue;
            mob.refreshPositionAndAngles(bx+rx*side[i],p.getY(),bz+rz*side[i],(float)p.getYaw()+180,0);
            mob.setHeadYaw((float)p.getYaw()+180);
            mob.setAiDisabled(true);
            mob.addCommandTag("visual-check");
            world.spawnEntity(mob);
        }
    }
}
