package gg.gravesandglory;

import net.minecraft.server.world.ServerWorld;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Map adoption (plan task 2A.5): after a battlefield is generated, its baked overlay (a gg-mapforge-runs/1 file of
 * kind overlay, made by tools/mapforge adopt) is written over it, a budget of cells per tick. The overlay is refused,
 * and the generated map kept, when its format, kind, map, generator stamp, body hash or base digest does not match.
 * The player jar ships no overlay until Kort approves one, so this is a no-op there. Overlays live at
 * data/gravesandglory/adoption/(map id).runs.gz; -Dgravesandglory.adoptionDev=true reads the unshipped test-tools copy
 * at data/gravesandglory/adoption-dev/ instead. */
public final class MapAdoption {
    /** Bump whenever Arena, Structures, CampaignMaps or MapDressing change the blocks a map generates: every baked
     * overlay then refuses until it is re-made against the new base. */
    static final String GENERATOR="arena-2026-11-01";
    static final int CHECK_BUDGET=1<<18,APPLY_BUDGET=1<<17;
    enum Phase{IDLE,CHECK,APPLY,DONE}
    static Phase phase=Phase.DONE;static int map=-1;static MapRuns runs;static MapRuns.WorldDigest check;static MapRuns.Apply apply;
    static String status="none";static long startedAt;

    /** sha256 of what generates a map: the generator version, the map, and the Spec tables Arena and Structures read
     * (POINTS, STRUCTURES and FACTIONS as CampaignMaps.select left them for the selected map). */
    static String stamp(int index){
        var md=MapRuns.sha();
        Object[][] tables={Spec.MAPS,Spec.RULES,Spec.POINTS,Spec.STRUCTURES,Spec.FACTIONS,Spec.TERRAIN,Spec.TERRAINSTYLE,Spec.SCENERY,Spec.DECORATIONS,Spec.PIECES,Spec.LANDMARKS,Spec.LANDMARKLINES,Spec.SUPPLYHOUSES};
        md.update((GENERATOR+"\n"+index+"\n"+Spec.MAPS[index].id()+"\n").getBytes(StandardCharsets.UTF_8));
        for(var t:tables)md.update((Arrays.toString(t)+"\n").getBytes(StandardCharsets.UTF_8));
        return MapRuns.hex(md.digest());
    }
    static boolean dev(){return Boolean.getBoolean("gravesandglory.adoptionDev");}
    static String resource(int index){return "/data/gravesandglory/"+(dev()?"adoption-dev/":"adoption/")+Spec.MAPS[index].id()+".runs.gz";}

    /** Called when the columns of a map are built, before its landmarks go down. */
    static void reset(int index){map=index;phase=Phase.IDLE;runs=null;check=null;apply=null;}

    /** One tick of adoption once the landmarks are placed; true when the map is final (adopted, refused or nothing to adopt). */
    static boolean step(ServerWorld w){
        switch(phase){
            case IDLE->{
                phase=Phase.DONE;
                InputStream in=map<0?null:MapAdoption.class.getResourceAsStream(resource(map));
                if(in==null){status="none";return true;}
                startedAt=System.nanoTime();
                try(in){begin(MapRuns.read(in),w);}catch(Exception e){refuse("unreadable: "+e.getMessage());}
                return phase==Phase.DONE;
            }
            case CHECK->{
                if(!check.step(w,CHECK_BUDGET))return false;
                if(!check.result.equals(runs.h("baseDigest"))){refuse("the generated map differs from the overlay's base (base digest "+check.result.substring(0,12)+", overlay wants "+String.valueOf(runs.h("baseDigest")).substring(0,Math.min(12,String.valueOf(runs.h("baseDigest")).length()))+")");return true;}
                try{apply=runs.new Apply(null);}catch(IllegalArgumentException e){refuse(e.getMessage());return true;}
                phase=Phase.APPLY;return false;
            }
            case APPLY->{
                if(!apply.step(w,APPLY_BUDGET))return false;
                phase=Phase.DONE;status="adopted "+runs.cells()+" cells ("+apply.changed+" changed) in "+(System.nanoTime()-startedAt)/1_000_000+" ms";
                System.out.println("[Graves and Glory] map adoption: "+Spec.MAPS[map].id()+" "+status);
                Diagnostics.report("map-adoption",true,null);runs=null;apply=null;check=null;
                return true;
            }
            default->{return true;}
        }
    }
    /** Checks a read overlay against the selected map and, when it passes, starts the base digest check. */
    static void begin(MapRuns r,ServerWorld w){
        String why=refusal(r,map);
        if(why!=null){refuse(why);return;}
        runs=r;check=r.new WorldDigest();phase=Phase.CHECK;status="checking";
    }
    /** Why an overlay may not go on map index, or null when its header matches. */
    static String refusal(MapRuns r,int index){
        if(!"overlay".equals(r.kind()))return "kind "+r.kind()+" is not overlay";
        if(!Spec.MAPS[index].id().equals(r.h("map")))return "overlay is for map "+r.h("map")+", not "+Spec.MAPS[index].id();
        if(!stamp(index).equals(r.h("stamp")))return "generator stamp differs: the overlay was made against another version of the map's generator; re-export and re-bake it";
        if(r.h("baseDigest")==null)return "overlay has no base digest";
        return null;
    }
    static void refuse(String why){
        phase=Phase.DONE;runs=null;check=null;apply=null;status="refused: "+why;
        System.out.println("[Graves and Glory] map adoption refused for "+(map<0?"?":Spec.MAPS[map].id())+": "+why+"; the generated map is kept");
        Diagnostics.report("map-adoption",false,why);
    }
}
