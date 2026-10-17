package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.world.GameMode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/** Unshipped map photographer: builds every map in turn and photographs spawns,
 * objectives and an overview from fixed, repeatable cameras. -PmapTour only. */
public final class MapTour implements ClientModInitializer {
    record View(String name,double x,double y,double z,float yaw,float pitch){}
    int ticks,map=-1,view,wait,buildWait;boolean hosted,building,done;List<View> views=List.of();String tag;
    static View look(String name,double x,double y,double z,double tx,double ty,double tz){
        double dx=tx-x,dy=ty-y,dz=tz-z;return new View(name,x,y,z,(float)Math.toDegrees(Math.atan2(-dx,dz)),(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz))));
    }
    static List<View> plan(){
        var v=new ArrayList<View>();
        v.add(new View("overview",0,175,250,180,40));
        for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];int s=t==0?-1:1,gy=Arena.height(f.spawnX(),f.spawnZ());
            v.add(look("spawn-"+f.id()+"-high",f.spawnX()+s*30,gy+30,f.spawnZ()+42,f.spawnX()-s*15,gy,f.spawnZ()));
            v.add(look("spawn-"+f.id()+"-ground",f.spawnX()+s*10,gy+4,f.spawnZ()+2,f.spawnX()-s*60,gy+2,f.spawnZ()));}
        for(var p:Spec.POINTS){int e=p.elevation();
            v.add(look("point-"+p.id()+"-high",p.x()-38,e+26,p.z()+38,p.x(),e,p.z()));
            v.add(look("point-"+p.id()+"-ground",p.x()-20,e+6,p.z()+20,p.x(),e+1,p.z()));}
        return v;
    }
    void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"tour-"+tag+"-map"+map+"-"+name+".png",c.getFramebuffer(),t->{});}
    /** One pixel per block, read from the same height and colour functions the tactical map uses. */
    void topDown(MinecraftClient c){
        int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ();var img=new BufferedImage(hx*2+1,hz*2+1,BufferedImage.TYPE_INT_RGB);
        for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++)img.setRGB(x+hx,z+hz,Arena.mapColor(x,z)&0xffffff);
        var g=img.createGraphics();
        for(var p:Spec.POINTS){g.setColor(java.awt.Color.RED);g.drawOval(p.x()+hx-p.radius(),p.z()+hz-p.radius(),p.radius()*2,p.radius()*2);g.drawString(p.id(),p.x()+hx-3,p.z()+hz+4);}
        for(int t=0;t<2;t++){var f=Spec.FACTIONS[t];g.setColor(t==0?java.awt.Color.CYAN:java.awt.Color.MAGENTA);g.fillRect(f.spawnX()+hx-3,f.spawnZ()+hz-3,7,7);}
        g.dispose();
        try{File out=new File(c.runDirectory,"screenshots/tour-"+tag+"-map"+map+"-topdown.png");out.getParentFile().mkdirs();javax.imageio.ImageIO.write(img,"png",out);}catch(Exception e){e.printStackTrace();}
    }
    /** Every road, bridge and capture-zone cell must stay walkable: nothing solid at feet or head height above the ground. */
    /** Roads and bridges must stay walkable: nothing solid at feet or head height. Authored buildings (barracks, gatehouses)
     * are counted apart, and capture zones too, since their cover is placed on purpose. Every hit goes to a file. */
    void walkable(GravesAndGlory.Battle b){
        int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),cells=0,road=0,building=0,point=0;var all=new StringBuilder();
        for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++){boolean inPoint=false,bridge=false;
            for(var p:Spec.POINTS)inPoint|=(x-p.x())*(x-p.x())+(z-p.z())*(z-p.z())<=p.radius()*p.radius();
            for(var s:Spec.STRUCTURES)bridge|=s.kind().equals("bridge")&&Math.abs(x-s.x())<=s.width()/2&&Math.abs(z-s.z())<=s.depth()/2;
            if(!inPoint&&!bridge&&!Arena.road(x,z))continue;cells++;int g=Arena.ground(x,z);String hit=null;
            for(int y=g+1;y<=g+2&&hit==null;y++){var pos=new net.minecraft.util.math.BlockPos(x,y,z);var st=b.world.getBlockState(pos);if(!st.getCollisionShape(b.world,pos).isEmpty())hit=st.getBlock().getTranslationKey().replace("block.minecraft.","");}
            if(hit==null)continue;String kind=!bridge&&MapDressing.onStructure(x,z,0)?"building":inPoint?"point":"road";
            if(kind.equals("road"))road++;else if(kind.equals("building"))building++;else point++;all.append(kind).append(' ').append(x).append(' ').append(z).append(' ').append(hit).append('\n');}
        try{java.nio.file.Files.writeString(new File(MinecraftClient.getInstance().runDirectory,"screenshots/tour-"+tag+"-map"+map+"-blocked.txt").toPath(),all);}catch(Exception e){e.printStackTrace();}
        System.out.println("[Graves and Glory tour] map "+map+" walkable cells="+cells+" blocked road/bridge="+road+" in-building="+building+" in-point="+point);
    }
    void camera(MinecraftClient c,View v){var id=c.player.getUuid();c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(id);if(p!=null){p.changeGameMode(GameMode.SPECTATOR);p.teleport(c.getServer().getOverworld(),v.x(),v.y(),v.z(),v.yaw(),v.pitch());}});}
    @Override public void onInitializeClient(){
        if(!Boolean.getBoolean("gravesandglory.mapTour"))return;
        tag=System.getProperty("gravesandglory.tourTag","now");
        String only=System.getProperty("gravesandglory.tourMaps","0,1,2,3,4");
        Deque<Integer> queue=new ArrayDeque<>();for(String s:only.split(","))queue.add(Integer.parseInt(s.trim()));
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(c.getOverlay()!=null)return;
            if(c.world==null){if(!hosted&&++ticks>60){hosted=true;GravesAndGloryClient.connectOrHost(c);}return;}
            var b=GravesAndGlory.battle;if(b==null||c.getServer()==null||c.player==null)return;
            c.options.pauseOnLostFocus=false;c.options.hudHidden=true;c.options.getViewDistance().setValue(24);c.options.getCloudRenderMode().setValue(net.minecraft.client.option.CloudRenderMode.OFF);
            if(done)return;
            if(building){if(++buildWait<60||!b.ready)return;buildWait=0;building=false;views=plan();view=0;wait=0;
                c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(c.player.getUuid());b.campaign.handle(p,"camp:practice");b.campaign.introUntil=b.time;});
                topDown(c);c.getServer().execute(()->walkable(b));return;}
            if(map<0||view>=views.size()){
                if(map>=0)c.getServer().execute(()->b.campaign.returnLobby());
                if(queue.isEmpty()){System.out.println("[Graves and Glory tour] COMPLETE");done=true;c.scheduleStop();return;}
                map=queue.poll();int target=map;building=true;
                c.getServer().execute(()->{b.campaign.returnLobby();if(b.campaign.map!=target)b.campaign.switchMap(target);else{b.ready=false;b.buildColumn=0;}});
                System.out.println("[Graves and Glory tour] building map "+map);return;
            }
            wait++;
            if(wait==1||wait==60)camera(c,views.get(view)); // again at 60: the practice start teleports the player after the first move
            if(wait>=100&&c.currentScreen!=null)c.setScreen(null);
            if(wait==110){shot(c,views.get(view).name());System.out.println("[Graves and Glory tour] map "+map+" "+views.get(view).name());view++;wait=0;}
        });
    }
}
