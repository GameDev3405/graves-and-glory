package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.nio.file.*;

/** Local visual preferences. Never changes the simulation, server clock or player camera mode.
 *  The modpack ships no third-party resource packs (Kort, 2026-10-18), so there is no texture choice here:
 *  Fabric enables the built-in armory pack itself (DEFAULT_ENABLED in GravesAndGloryClient). */
public final class Presentation {
    static int impact=1;
    static String notice="Choose a look. Your choice affects only your screen.";
    static boolean loaded,changing;
    static long hitAt,lastHit;static float hitStrength;static int receivedImpacts;
    static final String[] SHADERS={"Off / fastest","Reimagined","Unbound","MakeUp Ultra Fast"};
    static final String[] FILES={"","ComplementaryReimagined_r5.5.1.zip","ComplementaryUnbound_r5.5.1.zip","MakeUp-UltraFast-9.5g.zip"};
    static Path file(){return MinecraftClient.getInstance().runDirectory.toPath().resolve("gravesandglory/presentation.json");}
    static void load(){if(loaded)return;loaded=true;try{var j=JsonParser.parseString(Files.readString(file())).getAsJsonObject();impact=Math.max(0,Math.min(2,j.get("impact").getAsInt()));}catch(Exception ignored){}}
    static void save(){try{Files.createDirectories(file().getParent());Files.writeString(file(),"{\"impact\":"+impact+"}");}catch(Exception e){notice="Your visual preference could not be saved.";}}
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
