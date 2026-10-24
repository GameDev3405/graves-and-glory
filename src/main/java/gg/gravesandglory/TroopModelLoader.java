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
import net.minecraft.util.math.Vec3d;
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
    static final int FORMAT=3;
    /** The most quads one weapon frame may have (the build fails above it, and so does the load). */
    static final int MAX_WEAPON_QUADS=128;
    /** The most quads a cube weapon's far version may have (art pipeline v2, section 8: the fourth cut). */
    static final int MAX_FAR_QUADS=32;
    /** The most faces a troop may have, its canonical one included (the build's FACES_MAX). */
    static final int MAX_FACES=8;
    static final Logger LOG=LoggerFactory.getLogger("gravesandglory");
    @Override public Identifier getFabricId(){return new Identifier(TroopModels.NAMESPACE,"troop_models");}

    /** The three lists of index.json: rigs, weapons and troops, every member a name. */
    record Index(List<String> rigs,List<String> weapons,List<String> troops){}

    @Override public void reload(ResourceManager manager){
        Map<String,TroopModel<?>> troops=new LinkedHashMap<>(),rigs=new LinkedHashMap<>();Map<String,TroopModels.Weapon> weapons=new LinkedHashMap<>();
        Map<String,JsonObject> rigFiles=new HashMap<>();
        Index index;
        try{index=index(read(manager,"index.json"));}catch(Exception e){log("index.json",e);TroopModels.publish(troops,rigs,weapons);return;}
        for(String rig:index.rigs())try{
            JsonObject j=read(manager,"rig/"+rig+".json");need(rig.equals(str(j,"rig")),"its rig is "+str(j,"rig")+", not "+rig);
            rigs.put(rig,new TroopModel<>(j,parse(j).createModel(),hands(j),null,List.of()));rigFiles.put(rig,j);
        }catch(Exception e){log("rig/"+rig+".json",e);}
        for(String weapon:index.weapons())try{
            JsonObject j=read(manager,"weapon/"+weapon+".json");need(weapon.equals(str(j,"weapon")),"its weapon is "+str(j,"weapon")+", not "+weapon);
            TroopModels.Weapon w=parseWeapon(j);
            texture(manager,w.texture());if(w.glow()!=null)texture(manager,w.glow());
            weapons.put(weapon,w);
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
            // a troop of density above 1 ships a one-texel-a-px twin of each of its PNGs (<name>_far.png) for the draws past TroopDepth.NEAR_BLOCKS and the small cards
            int density=j.has("density")?j.get("density").getAsInt():1;need(density>=1&&density<=4,"density "+density+" is not 1 to 4");
            if(density>1){
                for(var e:j.getAsJsonObject("skins").entrySet())texture(manager,TroopModels.twin(new Identifier(e.getValue().getAsString())));
                for(String key:List.of("veteran","glow","outer"))if(!str(j,key).isEmpty())texture(manager,TroopModels.twin(new Identifier(str(j,key))));
            }
            boolean team=false;for(JsonElement b:j.getAsJsonArray("bones"))team|=b.getAsJsonObject().has("team")&&b.getAsJsonObject().get("team").getAsBoolean();
            need(!team||missingCloth==null,"the cloth sheet "+missingCloth+" is missing");
            float outer=rig.get("outer").getAsFloat();
            ModelPart twin=str(j,"outer").isEmpty()?null:build(j,outer,true).createModel();
            TroopModel<?> model=new TroopModel<>(j,parse(j).createModel(),hands(rig),twin,weapon.equals("none")?List.of():tierPoints(weapons,weapon));
            model.rig(rigs.get(str(j,"rig")));troops.put(troop,model);
        }catch(Exception e){log(troop+".json",e);}
        TroopModels.publish(troops,rigs,weapons);
    }

    /** index.json's lists, each member a name; a missing list is empty, anything else is refused (and the whole index with it). */
    static Index index(JsonObject index){
        need(index.has("format")&&index.get("format").getAsInt()==FORMAT,"format is not "+FORMAT);
        return new Index(names(index,"rigs"),names(index,"weapons"),names(index,"troops"));
    }
    /** What a troop file needs beyond its bones: a skins object with an armour tier 1 skin, every key an armour tier and every value a texture id. */
    static void checkTroop(JsonObject troop){
        need(troop.has("skins")&&troop.get("skins").isJsonObject(),"no skins");
        JsonObject skins=troop.getAsJsonObject("skins");need(skins.has("1"),"no armour tier 1 skin");
        for(var e:skins.entrySet()){need(e.getKey().matches("[1-9][0-9]*"),"a skin for armour tier "+e.getKey());need(e.getValue().isJsonPrimitive()&&Identifier.tryParse(e.getValue().getAsString())!=null,"the armour tier "+e.getKey()+" skin is not a texture id");}
        checkFaces(troop);
    }
    /**
     * A troop's face variants (tools/build_skins.py split_faces): a "faces" object holds the count (2 to MAX_FACES), the card face
     * and the pinned face (optional), and the bones that name "variants" are exactly one a face, each a face of the count. A troop
     * without "faces" has no bone that names variants.
     */
    static void checkFaces(JsonObject troop){
        JsonArray bones=troop.getAsJsonArray("bones");
        if(!troop.has("faces")){
            if(bones!=null)for(JsonElement e:bones)need(!e.getAsJsonObject().has("variants"),"bone "+str(e.getAsJsonObject(),"name")+" names variants, but the troop has no faces");
            return;
        }
        need(troop.get("faces").isJsonObject(),"faces is not an object");JsonObject f=troop.getAsJsonObject("faces");
        need(f.has("count")&&f.get("count").isJsonPrimitive(),"faces has no count");int count=f.get("count").getAsInt();
        need(count>=2&&count<=MAX_FACES,"faces count "+count+" is not 2 to "+MAX_FACES);
        int card=f.has("card")?f.get("card").getAsInt():0,fixed=f.has("fixed")?f.get("fixed").getAsInt():-1;
        need(card>=0&&card<count,"faces card "+card+" is not a face from 0 to "+(count-1));need(fixed>=-1&&fixed<count,"faces fixed "+fixed+" is not a face from 0 to "+(count-1));
        int[] bonesOf=new int[count];
        if(bones!=null)for(JsonElement e:bones){
            JsonObject b=e.getAsJsonObject();if(!b.has("variants"))continue;
            need(b.get("variants").isJsonArray()&&b.getAsJsonArray("variants").size()==1,"bone "+str(b,"name")+" must name one variant");
            int k=b.getAsJsonArray("variants").get(0).getAsInt();need(k>=0&&k<count,"bone "+str(b,"name")+" names variant "+k+", not a face from 0 to "+(count-1));bonesOf[k]++;
        }
        for(int k=0;k<count;k++)need(bonesOf[k]==1,"face "+k+" has "+bonesOf[k]+" bones, not one");
    }
    /** The corners of a weapon's boxes and of each of its tier models' (<weapon>_t2, _t3 that loaded): the cull box holds the biggest tier the troop may hold. */
    static List<double[]> tierPoints(Map<String,TroopModels.Weapon> weapons,String weapon){
        List<double[]> out=new ArrayList<>(weapons.get(weapon).points());
        for(int t=2;t<=TroopModels.MAX_TIER;t++){var w=weapons.get(TroopModels.tierId(weapon,t));if(w!=null)out.addAll(w.points());}
        return out;
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

    /**
     * A weapon (format 3, troop depth design section 2 and art pipeline v2 section 2.1): the meshes of its frames on the sheet (rest
     * first, then one per pull), the pull times and each frame's nock, the grip that places the model in the hand bone's frame, its
     * texture and glow. A "sprite" weapon is an extruded PNG: every frame opens with its two big faces (north, then south), which a
     * flat draw keeps. A "cube" weapon is a Blockbench model: its frames hold any quads (13 or 20 numbers) and it carries a far
     * version of at most 32 quads, a sprite extrusion that opens with its two big faces, which a troop outside TroopDepth.near draws.
     */
    static TroopModels.Weapon parseWeapon(JsonObject w){
        need(w.has("format")&&w.get("format").getAsInt()==FORMAT,"format is not "+FORMAT);
        String kind=str(w,"kind");need(kind.equals("sprite")||kind.equals("cube"),"kind is "+kind+", not sprite or cube");boolean cube=kind.equals("cube");
        int[] size=ints(w,"size",2);need(size[0]>0&&size[1]>0,"size must be above 0");
        JsonObject grip=w.getAsJsonObject("grip");need(grip!=null,"no grip");
        float[] p=floats(grip,"offset",3),r=floats(grip,"rotation",3);String glow=str(w,"glow");
        float[] pull=new float[0];
        if(w.has("pull")){
            need(w.get("pull").isJsonArray(),"pull must be a list of times");JsonArray a=w.getAsJsonArray("pull");pull=new float[a.size()];
            for(int i=0;i<pull.length;i++){pull[i]=a.get(i).getAsFloat();need(pull[i]>=0&&pull[i]<=1.2f,"pull time "+pull[i]+" is not between 0 and 1.2 s");need(i==0||pull[i]>pull[i-1],"pull times must rise");}
            need(pull.length==0||pull.length==3,"pull must hold none or three times");
        }
        JsonArray frames=w.getAsJsonArray("frames");need(frames!=null&&frames.size()==1+pull.length,"frames must hold the rest frame and one for each pull");
        List<TroopMesh> meshes=new ArrayList<>();List<Vec3d> nocks=new ArrayList<>();
        for(int i=0;i<frames.size();i++){
            need(frames.get(i).isJsonObject(),"a frame that is not an object");JsonObject f=frames.get(i).getAsJsonObject();
            String want=i==0?"rest":"pull_"+(i-1);need(want.equals(str(f,"name")),"frame "+i+" is "+str(f,"name")+", not "+want);
            need(f.has("quads")&&f.get("quads").isJsonArray(),"frame "+want+" has no quads");
            TroopMesh mesh;try{mesh=TroopMesh.parse(f.getAsJsonArray("quads"),size[0],size[1]);}catch(IllegalArgumentException e){throw new IllegalArgumentException("frame "+want+": "+e.getMessage(),e);}
            need(mesh.quads()>=2&&mesh.quads()<=MAX_WEAPON_QUADS,"frame "+want+" has "+mesh.quads()+" quads, not 2 to "+MAX_WEAPON_QUADS);
            if(!cube)bigFacesFirst(mesh,"frame "+want);
            meshes.add(mesh);
            if(pull.length>0){
                need(f.has("nock")&&f.get("nock").isJsonArray()&&f.getAsJsonArray("nock").size()==3,"frame "+want+" has no nock");
                JsonArray n=f.getAsJsonArray("nock");nocks.add(new Vec3d(n.get(0).getAsDouble(),n.get(1).getAsDouble(),n.get(2).getAsDouble()));
            }
        }
        TroopMesh far=null;
        if(cube){
            need(w.has("far")&&w.get("far").isJsonArray(),"a cube weapon has no far version");
            try{far=TroopMesh.parse(w.getAsJsonArray("far"),size[0],size[1]);}catch(IllegalArgumentException e){throw new IllegalArgumentException("far version: "+e.getMessage(),e);}
            need(far.quads()>=2&&far.quads()<=MAX_FAR_QUADS,"the far version has "+far.quads()+" quads, not 2 to "+MAX_FAR_QUADS);
            bigFacesFirst(far,"the far version");
        }else need(!w.has("far")||w.get("far").isJsonArray()&&w.getAsJsonArray("far").isEmpty(),"a sprite weapon has no far version");
        return new TroopModels.Weapon(str(w,"weapon"),List.copyOf(meshes),far,pull,ModelTransform.of(p[0],p[1],p[2],r[0]*MathHelper.RADIANS_PER_DEGREE,r[1]*MathHelper.RADIANS_PER_DEGREE,r[2]*MathHelper.RADIANS_PER_DEGREE),
            List.copyOf(nocks),new Identifier(str(w,"texture")),glow.isEmpty()?null:new Identifier(glow));
    }

    /** A sprite's mesh (a sprite weapon's frame, a cube weapon's far version) opens with its two big faces, north then south, each spanning the mesh in x and y: a flat draw keeps the first two quads. */
    static void bigFacesFirst(TroopMesh mesh,String what){
        need(mesh.direction(0)==2&&mesh.direction(1)==3,what+" must open with its two big faces (north, then south), not directions "+mesh.direction(0)+" and "+mesh.direction(1));
        float[] bb=mesh.bounds();
        for(int q=0;q<2;q++){float[] ps=mesh.positions(q);float x0=Float.MAX_VALUE,y0=Float.MAX_VALUE,x1=-Float.MAX_VALUE,y1=-Float.MAX_VALUE;
            for(int k=0;k<4;k++){x0=Math.min(x0,ps[3*k]);x1=Math.max(x1,ps[3*k]);y0=Math.min(y0,ps[3*k+1]);y1=Math.max(y1,ps[3*k+1]);}
            need(x0==bb[0]&&x1==bb[3]&&y0==bb[1]&&y1==bb[4],what+": quad "+q+" does not span the mesh (its x and y extent is not the mesh's)");}
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
