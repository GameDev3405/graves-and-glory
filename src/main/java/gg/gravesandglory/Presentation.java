package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.nio.file.*;

/** Local visual preferences. Never changes the simulation, server clock or player camera mode. */
public final class Presentation {
    static int impact=1;
    static String notice="Choose a look. Your choice affects only your screen.";
    static boolean loaded,changing,packsInitialized,armoryPrepared,armoryInitialized;static int resourceState;
    static long hitAt,lastHit;static float hitStrength;static int receivedImpacts;
    static final String[] SHADERS={"Off / fastest","Reimagined","Unbound","MakeUp Ultra Fast"};
    static final String[] FILES={"","ComplementaryReimagined_r5.5.1.zip","ComplementaryUnbound_r5.5.1.zip","MakeUp-UltraFast-9.5g.zip"};
    static final String FRESH="file/FreshAnimations_v1.10.4.zip",FAITHFUL="file/Faithful 32x - 1.20.1.zip",EXCAL="file/Excalibur_V1.21.10.zip",OLD_EXCAL="file/Excalibur_V1.21.11.zip",CLARENT="file/Clarent_12110_1201_v3.zip",PATCH="file/EFA_1.10.4 Hotfix 3.zip";
    static final java.util.Set<String> MANAGED=java.util.Set.of(FRESH,FAITHFUL,EXCAL,OLD_EXCAL,CLARENT,PATCH,"gravesandglory:armory");
    static final String[] TEXTURES={"Minecraft + Fresh","Faithful 32x + Fresh","Excalibur + Fresh"};
    static Path file(){return MinecraftClient.getInstance().runDirectory.toPath().resolve("gravesandglory/presentation.json");}
    static void load(){if(loaded)return;loaded=true;try{var j=JsonParser.parseString(Files.readString(file())).getAsJsonObject();impact=Math.max(0,Math.min(2,j.get("impact").getAsInt()));packsInitialized=j.has("packsInitialized")&&j.get("packsInitialized").getAsBoolean();armoryInitialized=j.has("armoryInitialized")&&j.get("armoryInitialized").getAsBoolean();}catch(Exception ignored){}}
    static void save(){try{Files.createDirectories(file().getParent());Files.writeString(file(),"{\"impact\":"+impact+",\"packsInitialized\":"+packsInitialized+",\"armoryInitialized\":"+armoryInitialized+"}");}catch(Exception e){notice="Your visual preference could not be saved.";}}
    static boolean prepareResources(MinecraftClient c){
        if(resourceState==2)return true;if(resourceState==1)return false;
        if(packsInitialized&&armoryInitialized&&!c.getResourcePackManager().getEnabledNames().contains(OLD_EXCAL)){resourceState=2;return true;}
        c.getResourcePackManager().scanPacks();if(!c.getResourcePackManager().getNames().contains(FRESH)){resourceState=2;return true;}
        // Select defaults once; adding the armory must not preempt first-launch
        // Excalibur selection or change an existing player's texture choice.
        int index=packsInitialized?textures():c.getResourcePackManager().getNames().containsAll(java.util.List.of(EXCAL,CLARENT,PATCH))?2:0;
        armoryPrepared=true;resourceState=1;selectTextures(index);return false;
    }
    static int textures(){var names=MinecraftClient.getInstance().getResourcePackManager().getEnabledNames();return names.contains(EXCAL)||names.contains(OLD_EXCAL)?2:names.contains(FAITHFUL)?1:0;}
    static void selectTextures(int index){
        if(index<0||index>2||changing){resourceState=2;return;}var c=MinecraftClient.getInstance();c.getResourcePackManager().scanPacks();
        var packs=new java.util.ArrayList<String>();if(index==1)packs.add(FAITHFUL);if(index==2){packs.add(EXCAL);packs.add(CLARENT);}packs.add(FRESH);if(index==2)packs.add(PATCH);packs.add("gravesandglory:armory");
        if(!c.getResourcePackManager().getNames().containsAll(packs)){notice="This texture choice is missing from the installation.";resourceState=2;return;}
        var previous=new java.util.ArrayList<>(c.getResourcePackManager().getEnabledNames());var previousIncompatible=new java.util.ArrayList<>(c.options.incompatibleResourcePacks);var enabled=new java.util.ArrayList<>(previous);enabled.removeIf(MANAGED::contains);enabled.addAll(packs);
        changing=true;c.getResourcePackManager().setEnabledProfiles(enabled);c.options.resourcePacks.removeIf(MANAGED::contains);c.options.resourcePacks.addAll(packs);
        // The authors' Clarent backport adapts Excalibur to 1.20.1. Remember
        // acceptance of the unchanged base pack's newer metadata across restarts.
        c.options.incompatibleResourcePacks.removeIf(MANAGED::contains);for(String name:packs){var pack=c.getResourcePackManager().getProfile(name);if(pack!=null&&!pack.getCompatibility().isCompatible())c.options.incompatibleResourcePacks.add(name);}
        c.options.write();notice="Loading textures and troop animations...";
        c.reloadResources().whenComplete((nothing,error)->c.execute(()->{
            changing=false;resourceState=2;
            if(error==null){packsInitialized=true;armoryInitialized=true;save();notice=TEXTURES[index]+" enabled. Your choice is saved.";}
            else {c.getResourcePackManager().setEnabledProfiles(previous);c.options.resourcePacks.clear();c.options.resourcePacks.addAll(previous);c.options.incompatibleResourcePacks.clear();c.options.incompatibleResourcePacks.addAll(previousIncompatible);c.options.write();c.reloadResources();notice="Could not load that look. Restored your previous textures.";}
            if(c.currentScreen instanceof PresentationScreen screen)screen.refresh();
        }));
    }
    static int shader(){
        try{Class<?> iris=Class.forName("net.irisshaders.iris.Iris");Object config=iris.getMethod("getIrisConfig").invoke(null);if(!(Boolean)config.getClass().getMethod("areShadersEnabled").invoke(config))return 0;
            Object name=config.getClass().getMethod("getShaderPackName").invoke(config);String s=((java.util.Optional<?>)name).map(Object::toString).orElse("");for(int i=1;i<FILES.length;i++)if(FILES[i].equals(s))return i;
        }catch(Exception ignored){}return 0;
    }
    static void selectShader(int index){
        if(index<0||index>=FILES.length||changing)return;changing=true;
        try{
            var c=MinecraftClient.getInstance();if(index>0&&!Files.isRegularFile(c.runDirectory.toPath().resolve("shaderpacks").resolve(FILES[index]))){notice="This shader is missing from this installation.";return;}
            Class<?> iris=Class.forName("net.irisshaders.iris.Iris");Object config=iris.getMethod("getIrisConfig").invoke(null);Class<?> type=config.getClass();
            type.getMethod("setShadersEnabled",boolean.class).invoke(config,index>0);if(index>0)type.getMethod("setShaderPackName",String.class).invoke(config,FILES[index]);type.getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);
            var error=(java.util.Optional<?>)iris.getMethod("getStoredError").invoke(null);
            if(index>0&&(error.isPresent()||(Boolean)iris.getMethod("isFallback").invoke(null))){type.getMethod("setShadersEnabled",boolean.class).invoke(config,false);type.getMethod("save").invoke(config);iris.getMethod("reload").invoke(null);notice="That shader could not run here. Restored shaders off.";}
            else notice=index==0?"Shaders off. Your choice is saved.":SHADERS[index]+" enabled. Your choice is saved.";
        }catch(Exception e){notice="Shaders could not be changed. Your game is still running.";System.err.println("[Graves and Glory] Shader switch failed: "+e.getClass().getSimpleName());}finally{changing=false;}
    }
    static void impact(float strength){load();long now=System.nanoTime();if(impact==0||CampaignClient.cinematic()||now-lastHit<250_000_000L)return;lastHit=hitAt=now;hitStrength=Math.max(0,Math.min(1,strength));}
    public static float kick(){if(impact==0||CampaignClient.cinematic())return 0;double t=(System.nanoTime()-hitAt)/130_000_000.0;return t<0||t>1?0:(float)(Math.sin(t*Math.PI)*(1-t)*hitStrength*(impact==2?1.1:.35));}
    static void hud(DrawContext g,int w,int h){
        if(impact==0||CampaignClient.cinematic())return;double t=(System.nanoTime()-hitAt)/150_000_000.0;if(t<0||t>1)return;
        int cx=w/2,cy=h/2,a=(int)(220*(1-t));int col=(a<<24)|0xe8c38a;
        // A short ink-and-gold contact accent; no flashing loop and no simulation hit-stop.
        for(int sign:new int[]{-1,1})for(int z:new int[]{-1,1})for(int i=4;i<9;i++)g.fill(cx+sign*i,cy+z*i,cx+sign*i+1,cy+z*i+1,col);
        if(impact==2&&t<.32){int alpha=(int)(25*(1-t/.32));g.fill(0,0,w,h,(alpha<<24)|0xd3b882);}
    }
}
