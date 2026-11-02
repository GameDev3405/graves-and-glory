package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The battle modes this build ships, by id, and the sheet rows behind them (Phase 4 Task 2). A mode is added by a
 * factory here plus its modes row; nothing else in the round needs to know its name.
 */
public final class Modes {
    /** The mode a round opens on and every map hosts. */
    public static final String DOMINATION="domination";
    /** The second mode, and the first that hosts one map only (Phase 4 Task 5). */
    public static final String SIEGE="siege";
    /** The supply-wagon escort on Frostmarch (spec 2026-10-31-convoy-escort-design.md). */
    public static final String CONVOY="convoy";
    /** The waves at the shrine on Hollowwood (Phase 4 spec 6, plan Task 6). */
    public static final String SURVIVAL="survival";
    private static final Map<String,Function<Spec.ModesRow,BattleMode>> BUILDERS=new LinkedHashMap<>();
    static{register(DOMINATION,DominationMode::new);register(SIEGE,SiegeMode::new);register(CONVOY,ConvoyMode::new);register(SURVIVAL,SurvivalMode::new);} // Siege is winnable since its stages landed (yard, gate HP, keep); Convoy since its spec (Kort 2026-10-31)
    private Modes(){}
    public static void register(String id,Function<Spec.ModesRow,BattleMode> factory){BUILDERS.put(id,factory);}
    public static boolean registered(String id){return BUILDERS.containsKey(id);}
    /** The registered mode ids, Domination first: the lobby room's mode list (SocialHub.MODES). */
    public static List<String> ids(){return List.copyOf(BUILDERS.keySet());}
    /** A mode's sheet row; an unknown id is a programming error, so the exception names it. */
    public static Spec.ModesRow row(String id){
        for(Spec.ModesRow row:Spec.MODES)if(row.id().equals(id))return row;
        throw new IllegalArgumentException("No modes row for "+id);
    }
    /** A fresh mode object for a round. */
    public static BattleMode create(String id){
        Function<Spec.ModesRow,BattleMode> factory=BUILDERS.get(id);
        if(factory==null)throw new IllegalArgumentException("No battle mode "+id);
        return factory.apply(row(id));
    }
    /** A mode's rule line as the lobby shows it before any round: its sheet template with the mode's own {n}. */
    public static String ruleLine(String id){Spec.ModesRow row=row(id);return ModeMath.ruleLine(row.ruleLine(),row.winTarget());}
    /** Whether a mode may be played on a map: its maps cell lists the map indexes it hosts. */
    public static boolean hosts(String id,int map){
        if(EngineMaps.on(map)&&!id.equals(DOMINATION))return false; // an engine map slot plays Domination only
        for(String part:row(id).maps().split(","))if(!part.isBlank()&&Integer.parseInt(part.trim())==map)return true;
        return false;
    }

    /** The marker's own mode recovers what it left unsaved (BattleMode.recover); a mode no longer registered has nothing to add. */
    static void hook(GravesAndGlory.Battle b,JsonObject marker){
        String id=marker.has("mode")?marker.get("mode").getAsString():"";
        if(!registered(id))return;
        try{create(id).recover(b,marker);}catch(Exception e){System.err.println("[Graves and Glory] "+id+" could not recover its round: "+e);}
    }

    // ---- crash marker: a round in progress leaves what it changed under the world save, so a restart puts the map back ----
    /** The marker of the round in progress, under the world save. */
    static final String MARKER="gravesandglory/mode-marker.json";
    static Path marker(GravesAndGlory.Battle b){return b.server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve(MARKER);}

    /** Writes the round's marker, atomically as a profile is saved. saved names the files a mode wrote and must delete if the map is rebuilt. */
    static void mark(GravesAndGlory.Battle b,long worldTime,boolean flagsDown,List<String> saved){
        Path path=marker(b);
        try{
            Files.createDirectories(path.getParent());
            JsonObject j=new JsonObject();
            j.addProperty("mode",b.mode.id());j.addProperty("map",b.campaign.map);j.addProperty("time",worldTime);j.addProperty("flagsDown",flagsDown);
            JsonArray names=new JsonArray();for(String name:saved)names.add(name);j.add("saved",names);
            Path tmp=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(tmp,Campaign.JSON.toJson(j));
            try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception e){System.err.println("[Graves and Glory] The mode marker could not be saved: "+e);}
    }

    /** Deletes the marker: a round that ended normally leaves nothing to recover. */
    static void unmark(GravesAndGlory.Battle b){
        try{Files.deleteIfExists(marker(b));}
        catch(Exception e){System.err.println("[Graves and Glory] The mode marker could not be deleted: "+e);}
    }

    /**
     * Puts back what an interrupted round changed, once the campaign's map is ready: the world's time of day, and the
     * point flags when the round had taken them down. A marker for another map is dropped with the files it saved,
     * since the rebuilt arena replaced them. Called by Battle.mapReady on every map ready, built or cached.
     */
    static void recover(GravesAndGlory.Battle b){
        Path path=marker(b);if(!Files.exists(path))return;
        try{
            JsonObject j=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            int map=j.has("map")?j.get("map").getAsInt():-1;
            if(map!=b.campaign.map){
                Path savedDir=Structures.savedDir(b.server.getOverworld()).toAbsolutePath().normalize();
                for(var entry:j.getAsJsonArray("saved"))try{
                    var saved=Structures.Saved.fromJson(JsonParser.parseString(entry.getAsString()).getAsJsonObject());
                    Path file=saved.file().toAbsolutePath().normalize();
                    if(file.startsWith(savedDir))Files.deleteIfExists(file); // a marker only deletes inside its own folder
                }catch(Exception plainFile){ // an older marker named its files, not the boxes they cover
                    try{Files.deleteIfExists(path.getParent().resolve(Path.of(entry.getAsString()).getFileName()));}catch(Exception ignored){}
                }
                hook(b,j);
                Files.deleteIfExists(path);
                System.err.println("[Graves and Glory] Dropped a mode marker for map "+map+": map "+b.campaign.map+" stands.");
                return;
            }
            var world=b.server.getOverworld();
            if(j.has("time"))world.setTimeOfDay(j.get("time").getAsLong());
            if(j.has("saved"))for(var entry:j.getAsJsonArray("saved"))try{
                Structures.restore(world,Structures.Saved.fromJson(JsonParser.parseString(entry.getAsString()).getAsJsonObject()));
            }catch(Exception e){System.err.println("[Graves and Glory] A saved piece could not be recovered: "+e);}
            if(j.has("flagsDown")&&j.get("flagsDown").getAsBoolean())for(var p:b.points)Arena.flag(world,p.spec,-1);
            hook(b,j);
            Files.deleteIfExists(path);
            System.out.println("[Graves and Glory] Recovered "+Spec.MAPS[b.campaign.map].name()+" from an interrupted round.");
        }catch(Exception e){System.err.println("[Graves and Glory] The mode marker could not be recovered: "+e);}
    }
}
