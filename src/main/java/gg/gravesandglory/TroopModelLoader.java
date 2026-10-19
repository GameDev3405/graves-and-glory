package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.model.*;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Reader;
import java.util.*;

/**
 * Loads the troops' own models on every client resource reload: models/troop/index.json, then each rig, weapon and troop
 * it lists (tools/build_skins.py writes them). The ModelParts are built here, not from an EntityModelLayer, so nothing can
 * swap them for another model. A file that fails to load, or names a texture no resource pack has, logs
 * "[troop-models] <file>: <reason>" and is left out, so its troop keeps drawing through its vanilla renderer; a bad
 * index.json leaves every troop vanilla.
 */
public final class TroopModelLoader implements SimpleSynchronousResourceReloadListener {
    static final String FOLDER="models/troop/";
    static final int FORMAT=1;
    static final Logger LOG=LoggerFactory.getLogger("gravesandglory");
    @Override public Identifier getFabricId(){return new Identifier(TroopModels.NAMESPACE,"troop_models");}

    /** The three lists of index.json: rigs, weapons and troops, every member a name. */
    record Index(List<String> rigs,List<String> weapons,List<String> troops){}

    @Override public void reload(ResourceManager manager){
        Map<String,TroopModel<?>> troops=new LinkedHashMap<>(),rigs=new LinkedHashMap<>();Map<String,TroopModels.Weapon> weapons=new LinkedHashMap<>();
        Map<String,JsonObject> rigFiles=new HashMap<>();Map<String,List<double[]>> grips=new HashMap<>();
        Index index;
        try{index=index(read(manager,"index.json"));}catch(Exception e){log("index.json",e);TroopModels.publish(troops,rigs,weapons);return;}
        for(String rig:index.rigs())try{
            JsonObject j=read(manager,"rig/"+rig+".json");need(rig.equals(str(j,"rig")),"its rig is "+str(j,"rig")+", not "+rig);
            rigs.put(rig,new TroopModel<>(j,parse(j).createModel(),hands(j),null,List.of()));rigFiles.put(rig,j);
        }catch(Exception e){log("rig/"+rig+".json",e);}
        for(String weapon:index.weapons())try{
            JsonObject j=read(manager,"weapon/"+weapon+".json");need(weapon.equals(str(j,"weapon")),"its weapon is "+str(j,"weapon")+", not "+weapon);
            String glow=str(j,"glow");Identifier texture=new Identifier(str(j,"texture")),lit=glow.isEmpty()?null:new Identifier(glow);
            texture(manager,texture);if(lit!=null)texture(manager,lit);
            weapons.put(weapon,new TroopModels.Weapon(parseWeapon(j).createModel(),texture,lit));grips.put(weapon,gripPoints(j));
        }catch(Exception e){log("weapon/"+weapon+".json",e);}
        Set<String> cloths=new TreeSet<>();for(var f:Spec.FACTIONS)cloths.add(f.id());cloths.add("linen");cloths.add("company");
        String missingCloth=null;for(String c:cloths)if(manager.getResource(TroopModels.cloth("linen".equals(c)?null:c)).isEmpty()){missingCloth=TroopModels.cloth("linen".equals(c)?null:c).toString();break;}
        for(String troop:index.troops())try{
            JsonObject j=read(manager,troop+".json");need(troop.equals(str(j,"troop")),"its troop is "+str(j,"troop")+", not "+troop);
            checkTroop(j);
            JsonObject rig=rigFiles.get(str(j,"rig"));need(rig!=null,"its rig "+str(j,"rig")+" did not load");
            String weapon=str(j,"weapon");need(weapon.equals("none")||weapons.containsKey(weapon),"its weapon "+weapon+" did not load");
            for(var e:j.getAsJsonObject("skins").entrySet())texture(manager,new Identifier(e.getValue().getAsString()));
            for(String key:List.of("veteran","glow","outer"))if(!str(j,key).isEmpty())texture(manager,new Identifier(str(j,key)));
            boolean team=false;for(JsonElement b:j.getAsJsonArray("bones"))team|=b.getAsJsonObject().has("team")&&b.getAsJsonObject().get("team").getAsBoolean();
            need(!team||missingCloth==null,"the cloth sheet "+missingCloth+" is missing");
            float outer=rig.get("outer").getAsFloat();
            ModelPart twin=str(j,"outer").isEmpty()?null:build(j,outer,true).createModel();
            troops.put(troop,new TroopModel<>(j,parse(j).createModel(),hands(rig),twin,grips.getOrDefault(weapon,List.of())));
        }catch(Exception e){log(troop+".json",e);}
        TroopModels.publish(troops,rigs,weapons);
    }

    /** index.json's lists, each member a name; a missing list is empty, anything else is refused (and the whole index with it). */
    static Index index(JsonObject index){
        need(index.has("format")&&index.get("format").getAsInt()==FORMAT,"format is not "+FORMAT);
        return new Index(names(index,"rigs"),names(index,"weapons"),names(index,"troops"));
    }
    /** What a troop file needs beyond its bones: a skins object with a stage 1 skin, every key a stage number and every value a texture id. */
    static void checkTroop(JsonObject troop){
        need(troop.has("skins")&&troop.get("skins").isJsonObject(),"no skins");
        JsonObject skins=troop.getAsJsonObject("skins");need(skins.has("1"),"no stage 1 skin");
        for(var e:skins.entrySet()){need(e.getKey().matches("[1-9][0-9]*"),"a skin for stage "+e.getKey());need(e.getValue().isJsonPrimitive()&&Identifier.tryParse(e.getValue().getAsString())!=null,"the stage "+e.getKey()+" skin is not a texture id");}
    }
    static void texture(ResourceManager manager,Identifier id){need(manager.getResource(id).isPresent(),"missing texture "+id);}

    /** The ModelParts of a generated troop or rig file: a root part holding the file's bones by name, each box on its own sheet. */
    static TexturedModelData parse(JsonObject model){return build(model,0,false);}

    /** outer: only the rig's own bones (no parts), every box dilated by that much more, for the outer layer's twin. */
    static TexturedModelData build(JsonObject model,float outer,boolean rigOnly){
        need(model.has("format")&&model.get("format").getAsInt()==FORMAT,"format is not "+FORMAT);
        int[] size=ints(model,"size",2);need(size[0]>0&&size[1]>0,"size must be above 0");
        ModelData data=new ModelData();Map<String,ModelPartData> made=new HashMap<>();Set<String> skipped=new HashSet<>();
        JsonArray bones=model.getAsJsonArray("bones");need(bones!=null&&!bones.isEmpty(),"no bones");need(bones.size()<=TroopModel.MAX_BONES,bones.size()+" bones, more than "+TroopModel.MAX_BONES);
        boolean first=true;
        for(JsonElement el:bones){
            need(el.isJsonObject(),"a bone that is not an object");
            JsonObject b=el.getAsJsonObject();String name=str(b,"name"),parent=str(b,"parent");
            need(!name.isEmpty(),"a bone without a name");need(!made.containsKey(name)&&!skipped.contains(name),"duplicate bone "+name);
            if(first)need(parent.isEmpty(),"the first bone "+name+" has a parent");else need(made.containsKey(parent)||skipped.contains(parent),name+" has parent "+parent+", which is not a bone listed before it");
            first=false;
            if(rigOnly&&(!str(b,"part").isEmpty()||skipped.contains(parent))){skipped.add(name);continue;}
            int[] sheet=b.has("sheet")?ints(b,"sheet",2):size;need(sheet[0]>0&&sheet[1]>0,name+": sheet must be above 0");
            ModelPartBuilder builder=ModelPartBuilder.create();
            JsonArray boxes=b.getAsJsonArray("boxes");need(boxes!=null,name+": no boxes list");
            for(JsonElement be:boxes){
                JsonObject x=be.getAsJsonObject();float[] o=floats(x,"origin",3),d=x.has("dilation")?floats(x,"dilation",3):new float[3];int[] s=ints(x,"size",3),uv=ints(x,"uv",2);
                need(s[0]>=0&&s[1]>=0&&s[2]>=0&&uv[0]>=0&&uv[1]>=0,name+": a box with a negative size or uv");
                builder.uv(uv[0],uv[1]).mirrored(x.has("mirror")&&x.get("mirror").getAsBoolean())
                    .cuboid(o[0],o[1],o[2],s[0],s[1],s[2],new Dilation(d[0]+outer,d[1]+outer,d[2]+outer),sheet[0]/(float)size[0],sheet[1]/(float)size[1]);
            }
            float[] p=floats(b,"pivot",3),r=b.has("rotation")?floats(b,"rotation",3):new float[3];
            ModelPartData parentData=parent.isEmpty()?data.getRoot():made.get(parent);
            made.put(name,parentData.addChild(name,builder,ModelTransform.of(p[0],p[1],p[2],r[0]*MathHelper.RADIANS_PER_DEGREE,r[1]*MathHelper.RADIANS_PER_DEGREE,r[2]*MathHelper.RADIANS_PER_DEGREE)));
        }
        return TexturedModelData.of(data,size[0],size[1]);
    }

    /** A weapon: one part, placed by its grip in the hand bone's frame, holding the weapon's boxes. */
    static TexturedModelData parseWeapon(JsonObject w){
        need(w.has("format")&&w.get("format").getAsInt()==FORMAT,"format is not "+FORMAT);
        int[] size=ints(w,"size",2);JsonObject grip=w.getAsJsonObject("grip");need(grip!=null,"no grip");
        float[] p=floats(grip,"offset",3),r=floats(grip,"rotation",3);
        ModelPartBuilder builder=ModelPartBuilder.create();JsonArray boxes=w.getAsJsonArray("boxes");need(boxes!=null&&!boxes.isEmpty(),"no boxes");
        for(JsonElement be:boxes){JsonObject x=be.getAsJsonObject();float[] o=floats(x,"origin",3),d=x.has("dilation")?floats(x,"dilation",3):new float[3];int[] s=ints(x,"size",3),uv=ints(x,"uv",2);
            builder.uv(uv[0],uv[1]).mirrored(x.has("mirror")&&x.get("mirror").getAsBoolean()).cuboid(o[0],o[1],o[2],s[0],s[1],s[2],new Dilation(d[0],d[1],d[2]));}
        ModelData data=new ModelData();
        data.getRoot().addChild("weapon",builder,ModelTransform.of(p[0],p[1],p[2],r[0]*MathHelper.RADIANS_PER_DEGREE,r[1]*MathHelper.RADIANS_PER_DEGREE,r[2]*MathHelper.RADIANS_PER_DEGREE));
        return TexturedModelData.of(data,size[0],size[1]);
    }
    /** The corners of a weapon's boxes in its hand bone's frame (the grip's offset and rotation applied), in model pixels. */
    static List<double[]> gripPoints(JsonObject w){
        JsonObject grip=w.getAsJsonObject("grip");float[] p=floats(grip,"offset",3),r=floats(grip,"rotation",3);double[][] rot=TroopModel.rotation(new double[]{r[0],r[1],r[2]});
        List<double[]> out=new ArrayList<>();
        for(JsonElement be:w.getAsJsonArray("boxes")){JsonObject x=be.getAsJsonObject();float[] o=floats(x,"origin",3),d=x.has("dilation")?floats(x,"dilation",3):new float[3];int[] s=ints(x,"size",3);
            for(int c=0;c<8;c++){double[] corner={(c&1)==0?o[0]-d[0]:o[0]+s[0]+d[0],(c&2)==0?o[1]-d[1]:o[1]+s[1]+d[1],(c&4)==0?o[2]-d[2]:o[2]+s[2]+d[2]};
                double[] q=TroopModel.apply(rot,corner);out.add(new double[]{q[0]+p[0],q[1]+p[1],q[2]+p[2]});}}
        return out;
    }

    static JsonObject read(ResourceManager manager,String file)throws Exception{
        var res=manager.getResource(new Identifier(TroopModels.NAMESPACE,FOLDER+file));if(res.isEmpty())throw new IllegalArgumentException("missing");
        try(Reader in=res.get().getReader()){return JsonParser.parseReader(in).getAsJsonObject();}
    }
    static Map<String,String> hands(JsonObject rig){JsonObject h=rig.getAsJsonObject("hands");need(h!=null&&h.has("right")&&h.has("left"),"hands must name right and left");return Map.of("right",str(h,"right"),"left",str(h,"left"));}
    static List<String> names(JsonObject index,String key){
        List<String> out=new ArrayList<>();if(!index.has(key))return out;
        need(index.get(key).isJsonArray(),key+" must be a list of names");
        for(var e:index.getAsJsonArray(key)){need(e.isJsonPrimitive()&&e.getAsJsonPrimitive().isString()&&!e.getAsString().isEmpty(),key+" holds "+e+", not a name");out.add(e.getAsString());}
        return out;
    }
    static void log(String file,Exception e){LOG.warn("[troop-models] {}: {}",file,e.getMessage()==null?e.toString():e.getMessage());}
    static void need(boolean ok,String why){if(!ok)throw new IllegalArgumentException(why);}
    static String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    static float[] floats(JsonObject o,String key,int n){JsonArray a=o.getAsJsonArray(key);need(a!=null&&a.size()==n,key+" must hold "+n+" numbers");float[] v=new float[n];for(int i=0;i<n;i++)v[i]=a.get(i).getAsFloat();return v;}
    static int[] ints(JsonObject o,String key,int n){JsonArray a=o.getAsJsonArray(key);need(a!=null&&a.size()==n,key+" must hold "+n+" whole numbers");int[] v=new int[n];for(int i=0;i<n;i++)v[i]=a.get(i).getAsInt();return v;}
}
