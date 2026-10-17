package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.world.GameMode;

/** Repeatable photographs of the real game and real screens, only in explicit test mode. */
final class VisualChecks {
    static int elapsed;
    static boolean began;
    static int showcase;
    static boolean showcaseStarted;
    static int ownedBefore;
    static boolean sawQueue,sawCancelled;
    static boolean placedJoiner;
    static boolean posing(){return showcaseStarted;}
    static boolean hideHud(){return showcaseStarted&&showcase>20&&showcase<420;}
    static void camera(MinecraftClient c,double x,double y,double z,float yaw,float pitch){
        if(c.getServer()==null)return;var id=c.player.getUuid();c.getServer().execute(()->{var p=c.getServer().getPlayerManager().getPlayer(id);if(p!=null){p.changeGameMode(GameMode.SPECTATOR);p.teleport(c.getServer().getOverworld(),x,y,z,yaw,pitch);}});
    }
    static void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,name+".png",c.getFramebuffer(),t->{});}
    static void tick(MinecraftClient c){
        if(!GravesAndGloryClient.ready)return;
        if(c.getServer()==null){
            if(GravesAndGloryClient.worldTicks==200)GravesAndGloryClient.send("barracks");
            if(GravesAndGloryClient.worldTicks==240)shot(c,"joiner-dusk-barracks");
            if(GravesAndGloryClient.worldTicks==260)c.setScreen(new GravesAndGloryClient.MapScreen());
            if(GravesAndGloryClient.worldTicks==300)shot(c,"joiner-dusk-map");
            if(GravesAndGloryClient.worldTicks==320)c.setScreen(null);
            return;
        }
        if(!placedJoiner&&c.getServer().getPlayerManager().getCurrentPlayerCount()>1){
            placedJoiner=true;c.getServer().execute(()->{for(var p:c.getServer().getPlayerManager().getPlayerList())if(!p.getUuid().equals(c.player.getUuid())){var s=GravesAndGlory.battle.squads.get(p.getUuid());if(s!=null){var f=Spec.FACTIONS[s.team];p.teleport(c.getServer().getOverworld(),f.barracksX(),65,f.barracksZ(),0,0);}}});
        }
        if(!began){began=true;elapsed=0;}elapsed++;
        switch(elapsed){
            case 40 -> camera(c,-67,81,-20,-138,27);
            case 120 -> shot(c,"01-village-gameplay");
            case 140 -> net.minecraft.client.option.KeyBinding.onKeyPressed(net.minecraft.client.util.InputUtil.fromKeyCode(org.lwjgl.glfw.GLFW.GLFW_KEY_R,0));
            case 170 -> shot(c,"02-command-screen");
            case 190 -> {if(c.currentScreen instanceof GravesAndGloryClient.CommandScreen)c.currentScreen.mouseClicked(c.currentScreen.width/2.0+100,c.currentScreen.height/2.0-30,0);}
            case 220 -> shot(c,"03-battlefield-map");
            case 240 -> {c.setScreen(null);camera(c,-108,67,-24,-90,10);}
            case 280 -> GravesAndGloryClient.send("barracks");
            case 320 -> shot(c,"04-barracks-roster");
            case 340 -> {c.setScreen(null);camera(c,-25,77,-16,-42,28);}
            case 410 -> shot(c,"05-river-crossing");
            case 430 -> camera(c,-115,88,77,-130,20);
            case 500 -> shot(c,"06-hills-and-valley");
            case 520 -> camera(c,18,81,-19,-133,26);
            case 590 -> shot(c,"07-shrine-and-woodland");
            case 610 -> camera(c,-35,87,1,-47,25);
            case 680 -> shot(c,"08-shattered-keep");
        }
        if(GravesAndGlory.battle!=null&&GravesAndGlory.battle.testStage>=8&&elapsed>700){
            if(!showcaseStarted){showcaseStarted=true;showcase=0;}
            showcase++;
            if(showcase==1){c.getServer().execute(()->GravesAndGlory.battle.prepareVisualSquads(false));camera(c,-76,80,-21,0,35);}
            for(int i=0;i<Spec.FORMATIONS.length;i++){
                if(showcase==20+i*100)GravesAndGloryClient.send("formation:"+Spec.FORMATIONS[i].id());
                if(showcase==100+i*100)shot(c,"formation-"+Spec.FORMATIONS[i].id());
            }
            if(showcase==430){c.setScreen(null);camera(c,-108,67,-24,-90,10);}
            if(showcase==460)GravesAndGloryClient.send("barracks");
            if(showcase==480&&c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen){ownedBefore=GravesAndGloryClient.count;screen.controls.get(0).disband().onPress();}
            if(showcase==500&&c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.controls.get(1).train().onPress();
            if(showcase==520){sawQueue=GravesAndGloryClient.queued==1&&GravesAndGloryClient.count==ownedBefore-1;shot(c,"barracks-training-queue");}
            if(showcase==530&&c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.cancel.onPress();
            if(showcase==550){sawCancelled=GravesAndGloryClient.queued==0;if(c.currentScreen instanceof GravesAndGloryClient.BarracksScreen screen)screen.controls.get(1).train().onPress();}
            if(showcase==660){MeltyTest.report("barracks-ui",sawQueue&&sawCancelled&&GravesAndGloryClient.count==ownedBefore&&GravesAndGloryClient.queued==0,"queueSeen="+sawQueue+", cancelled="+sawCancelled+", troops="+GravesAndGloryClient.count+" expected="+ownedBefore+", queued="+GravesAndGloryClient.queued);c.setScreen(null);c.getServer().execute(()->GravesAndGlory.battle.prepareVisualSquads(true));camera(c,-30,76,23,-122,20);}
            String[] orders={"follow","move","hold","attack","rush"};
            for(int i=0;i<orders.length;i++)if(showcase==680+i*20)GravesAndGloryClient.send(orders[i]);
            if(showcase==790)shot(c,"gravesandglory-battle");
            if(showcase==810)c.setScreen(new GravesAndGloryClient.MapScreen());
            if(showcase==835)shot(c,"map-friendly-battle");
            if(showcase==850)c.setScreen(null);
            if(showcase==880)camera(c,60,86,85,126,30);
            if(showcase==960)shot(c,"09-eastern-crossing");
            if(showcase==1000)camera(c,47,84,84,-138,28);
            if(showcase==1080)shot(c,"10-dusk-ruins");
            if(showcase==1100)camera(c,80,80,44,-125,26);
            if(showcase==1180)shot(c,"11-dusk-base");
            if(showcase==1200)camera(c,-103,67,-21,120,25);
            if(showcase==1280)shot(c,"12-barracks-interior");
            if(showcase==1300)camera(c,0,230,0,0,90);
            if(showcase==1380)shot(c,"13-whole-battlefield");
        }
    }
}
