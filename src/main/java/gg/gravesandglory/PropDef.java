package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A prop def (prop kit plan task S2): one JSON file per prop under {@code data/gravesandglory/gg_props/<id>.json}. Pure: JDK and Gson only.
 *
 * <p>Cell frame: cells are (x east, y up, z south) counted from the footprint's min corner; the anchor cell is the Structures pivot
 * {@code ((sx-1)/2, 0, (sz-1)/2)} and the anchor command block sits one block under it. Effect points (x, y, z) are in blocks in the same
 * frame (0.5 is a cell centre).
 *
 * <p>JSON form:
 * <pre>
 * {"id":"anvil", "category":"smithy", "name":"Anvil", "size":[sx,sy,sz],
 *  "parts":[{"model":"anvil","cmd":7310011,"glow":false}],          // glow optional, default false
 *  "solid":[[x,y,z], ...],                                          // explicit cell list, non-empty, no repeats
 *  "lights":[{"x":0,"y":2,"z":0,"level":12}],                       // optional; never on a solid cell
 *  "effects":[{"kind":"particle|sound","id":"minecraft:...","x":..,"y":..,"z":..,"spread":..,"count":..,"every":..,"chance":..,"volume":..,"pitch":..}]}
 * </pre>
 * {@code solid} is always the explicit list of [x,y,z] cells (no shorthand), so the file reads the same as the record.
 *
 * <p>Custom model data: parts are {@code minecraft:paper} items keyed by CMD = {@link #cmd}{@code (propIndex, partIndex)} =
 * {@code 7310001 + 10*propIndex + partIndex}, propIndex in the contract table's order (forge 0, anvil 1, weapon_rack 2, shield_rack 3,
 * quench_trough 4, grindstone_bench 5). The plan's {@code 7310000 + ...} would give the forge hearth 7310000, outside the prop range
 * {@code 7310001..7310999}, so numbering starts at 7310001 (forge hearth 7310001, hood 7310002, coals 7310003, anvil 7310011, ...). A prop's
 * menu icon is its first part.
 */
public record PropDef(String id,String category,String name,int sx,int sy,int sz,List<Part> parts,List<int[]> solid,List<Light> lights,List<Effect> effects){
    public static final int CMD_MIN=7310001,CMD_MAX=7310999;
    public record Part(String model,int cmd,boolean glow){}
    public record Light(int x,int y,int z,int level){}
    public record Effect(String kind,String id,double x,double y,double z,double spread,int count,int every,double chance,float volume,float pitch){}

    /** The CMD of part {@code part} of the prop at table index {@code prop}: 7310001 + 10*prop + part. */
    public static int cmd(int prop,int part){return CMD_MIN+10*prop+part;}

    public boolean inside(int x,int y,int z){return x>=0&&y>=0&&z>=0&&x<sx&&y<sy&&z<sz;}

    /** Parses and validates a def; throws IllegalArgumentException naming the offending field. */
    public static PropDef parse(String json){
        JsonObject o;
        try{o=JsonParser.parseString(json).getAsJsonObject();}catch(RuntimeException e){throw new IllegalArgumentException("prop def: not a JSON object: "+e.getMessage());}
        String id=str(o,"id");
        if(!id.matches("[a-z0-9_]+"))throw bad(id,"id","must be [a-z0-9_]+");
        String category=str(o,"category"),name=str(o,"name");
        JsonArray size=arr(o,"size",id);
        if(size.size()!=3)throw bad(id,"size","must be [sx,sy,sz]");
        int sx=num(size.get(0),id,"size"),sy=num(size.get(1),id,"size"),sz=num(size.get(2),id,"size");
        if(sx<1||sy<1||sz<1)throw bad(id,"size","must be positive");

        List<Part> parts=new ArrayList<>();Set<String> models=new HashSet<>();
        for(JsonElement e:arr(o,"parts",id)){
            JsonObject p=obj(e,id,"parts");
            String model=str(p,"model");
            if(!model.matches("[a-z0-9_]+"))throw bad(id,"parts.model",model+" must be [a-z0-9_]+");
            if(!models.add(model))throw bad(id,"parts.model","duplicate model "+model);
            int cmd=num(p.get("cmd"),id,"parts.cmd");
            if(cmd<CMD_MIN||cmd>CMD_MAX)throw bad(id,"parts.cmd",cmd+" outside "+CMD_MIN+".."+CMD_MAX);
            boolean glow=p.has("glow")&&p.get("glow").getAsBoolean();
            parts.add(new Part(model,cmd,glow));
        }
        if(parts.isEmpty())throw bad(id,"parts","must not be empty");

        PropDef shape=new PropDef(id,category,name,sx,sy,sz,List.of(),List.of(),List.of(),List.of());
        List<int[]> solid=new ArrayList<>();Set<String> cells=new HashSet<>();
        for(JsonElement e:arr(o,"solid",id)){
            if(!e.isJsonArray()||e.getAsJsonArray().size()!=3)throw bad(id,"solid","each cell must be [x,y,z]");
            JsonArray c=e.getAsJsonArray();
            int x=num(c.get(0),id,"solid"),y=num(c.get(1),id,"solid"),z=num(c.get(2),id,"solid");
            if(!shape.inside(x,y,z))throw bad(id,"solid","cell ("+x+","+y+","+z+") outside size "+sx+"x"+sy+"x"+sz);
            if(!cells.add(x+","+y+","+z))throw bad(id,"solid","cell ("+x+","+y+","+z+") repeated");
            solid.add(new int[]{x,y,z});
        }
        if(solid.isEmpty())throw bad(id,"solid","must not be empty");

        List<Light> lights=new ArrayList<>();
        if(o.has("lights"))for(JsonElement e:arr(o,"lights",id)){
            JsonObject l=obj(e,id,"lights");
            int x=num(l.get("x"),id,"lights"),y=num(l.get("y"),id,"lights"),z=num(l.get("z"),id,"lights"),level=num(l.get("level"),id,"lights");
            if(!shape.inside(x,y,z))throw bad(id,"lights","cell ("+x+","+y+","+z+") outside size");
            if(cells.contains(x+","+y+","+z))throw bad(id,"lights","cell ("+x+","+y+","+z+") is solid");
            if(level<1||level>15)throw bad(id,"lights","level "+level+" outside 1..15");
            lights.add(new Light(x,y,z,level));
        }

        List<Effect> effects=new ArrayList<>();
        if(o.has("effects"))for(JsonElement e:arr(o,"effects",id)){
            JsonObject f=obj(e,id,"effects");
            String kind=str(f,"kind");
            if(!kind.equals("particle")&&!kind.equals("sound"))throw bad(id,"effects.kind",kind+" must be particle or sound");
            String eid=str(f,"id");
            double x=dbl(f,"x",0,id),y=dbl(f,"y",0,id),z=dbl(f,"z",0,id);
            if(x<0||y<0||z<0||x>sx||y>sy||z>sz)throw bad(id,"effects","point ("+x+","+y+","+z+") outside size");
            int count=(int)dbl(f,"count",1,id),every=(int)dbl(f,"every",20,id);
            double chance=dbl(f,"chance",1,id);
            if(count<1)throw bad(id,"effects.count","must be at least 1");
            if(every<1)throw bad(id,"effects.every","must be at least 1");
            if(chance<0||chance>1)throw bad(id,"effects.chance","must be 0..1");
            effects.add(new Effect(kind,eid,x,y,z,dbl(f,"spread",0,id),count,every,chance,(float)dbl(f,"volume",1,id),(float)dbl(f,"pitch",1,id)));
        }
        return new PropDef(id,category,name,sx,sy,sz,List.copyOf(parts),List.copyOf(solid),List.copyOf(lights),List.copyOf(effects));
    }

    static IllegalArgumentException bad(String id,String field,String text){return new IllegalArgumentException("prop "+id+": "+field+": "+text);}
    static String str(JsonObject o,String field){
        JsonElement e=o.get(field);
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString()||e.getAsString().isBlank())
            throw new IllegalArgumentException("prop "+(o.has("id")&&o.get("id").isJsonPrimitive()?o.get("id").getAsString():"?")+": "+field+": missing or not a string");
        return e.getAsString();
    }
    static JsonArray arr(JsonObject o,String field,String id){
        JsonElement e=o.get(field);
        if(e==null||!e.isJsonArray())throw bad(id,field,"missing or not a list");
        return e.getAsJsonArray();
    }
    static JsonObject obj(JsonElement e,String id,String field){
        if(e==null||!e.isJsonObject())throw bad(id,field,"each entry must be an object");
        return e.getAsJsonObject();
    }
    static int num(JsonElement e,String id,String field){
        if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw bad(id,field,"missing or not a number");
        double d=e.getAsDouble();
        if(d!=Math.rint(d))throw bad(id,field,d+" is not a whole number");
        return (int)d;
    }
    static double dbl(JsonObject o,String field,double fallback,String id){
        JsonElement e=o.get(field);
        if(e==null)return fallback;
        if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber())throw bad(id,"effects."+field,"not a number");
        return e.getAsDouble();
    }
}
