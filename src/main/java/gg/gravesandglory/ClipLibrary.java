package gg.gravesandglory;

import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Reader;
import java.util.*;

/**
 * Loads the hand-keyed clips (tools/clips.py writes clips/<rig>/<family>/<clip>.json) on every client resource reload. A
 * clip that fails to parse logs "[clips] <file>: <reason>" and is left out. variants(family) lists a family's attack clips
 * in the sheet's attackVariants order, which the constructor takes (family to variant ids); a variant with no clip is
 * logged and skipped.
 */
public final class ClipLibrary implements SimpleSynchronousResourceReloadListener {
    static final String FOLDER="clips";
    static final Logger LOG=LoggerFactory.getLogger("gravesandglory");
    private static volatile Map<String,Clip> clips=Map.of();
    private static volatile Map<String,List<Clip>> variants=Map.of();
    private final Map<String,List<String>> order;

    public ClipLibrary(Map<String,List<String>> variantOrder){order=Map.copyOf(variantOrder);}

    @Override public Identifier getFabricId(){return new Identifier(TroopModels.NAMESPACE,"clips");}

    @Override public void reload(ResourceManager manager){
        Map<String,Clip> loaded=new TreeMap<>();
        Map<String,String> rigOf=new HashMap<>();
        for(Map.Entry<Identifier,Resource> e:manager.findResources(FOLDER,id->id.getNamespace().equals(TroopModels.NAMESPACE)&&id.getPath().endsWith(".json")).entrySet()){
            String path=e.getKey().getPath();
            String[] p=path.substring(FOLDER.length()+1,path.length()-".json".length()).split("/");
            if(p.length!=3){LOG.warn("[clips] {}: not clips/<rig>/<family>/<clip>.json",path);continue;}
            try(Reader r=e.getValue().getReader()){
                Clip clip=Clip.parse(JsonParser.parseReader(r).getAsJsonObject());
                if(!clip.name().equals(p[2]))throw new IllegalArgumentException("its clip is "+clip.name()+", not "+p[2]);
                loaded.put(p[0]+"/"+p[1]+"/"+p[2],clip);
                String other=rigOf.putIfAbsent(p[1]+"/"+p[2],p[0]);
                if(other!=null&&!other.equals(p[0]))LOG.warn("[clips] {}/{} is keyed for rigs {} and {}; variants() uses {}",p[1],p[2],other,p[0],other);
            }catch(Exception ex){LOG.warn("[clips] {}: {}",path,ex.toString());}
        }
        Map<String,List<Clip>> byFamily=new HashMap<>();
        order.forEach((family,ids)->{
            List<Clip> list=new ArrayList<>();
            for(String id:ids){
                String rig=rigOf.get(family+"/"+id);
                if(rig==null){LOG.warn("[clips] attack variant {} of {} has no clip",id,family);continue;}
                list.add(loaded.get(rig+"/"+family+"/"+id));
            }
            byFamily.put(family,List.copyOf(list));
        });
        clips=Collections.unmodifiableMap(loaded);variants=Map.copyOf(byFamily);
        LOG.info("[clips] loaded {} clips",loaded.size());
    }

    /** The clip, or null when no resource pack has it. */
    public static Clip get(String rig,String family,String clip){return clips.get(rig+"/"+family+"/"+clip);}

    /** A family's attack clips in the sheet's attackVariants order; empty when it has none. */
    public static List<Clip> variants(String family){return variants.getOrDefault(family,List.of());}
}
