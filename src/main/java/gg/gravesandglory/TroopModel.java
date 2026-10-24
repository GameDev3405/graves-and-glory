package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;
import java.util.*;

/**
 * One troop's own model (or a bare rig), built by TroopModelLoader from its generated models/troop JSON. Proportions are
 * baked in that file; this class shows and hides parts per armour tier (the stages key), weapon tier (weaponTiers), fold and helm, and TroopAnimator poses it from its rig's
 * clips (Task 5b). Team bones (panels, pennant, standard) are hidden for the skin pass and drawn by TroopFeatures.Cloth
 * from the cloth sheet. One instance serves every entity of the troop, so prepare and setAngles run right before each draw.
 */
public final class TroopModel<T extends MobEntity> extends SinglePartEntityModel<T> {
    /** A bone as the generated file describes it. variants is empty for every bone but a face bone, which shows only for the face variants it names (TroopFaces.shows). */
    record Bone(String name,String parent,String part,boolean team,String tag,Set<Integer> stages,Set<Integer> weaponTiers,Set<Integer> variants){}
    static final float RAD=MathHelper.RADIANS_PER_DEGREE;
    /** Head look limits in degrees: yaw both ways, pitch only downwards, so a helm never swings into the pennant pole behind it. */
    static final float HEAD_YAW=30,HEAD_DOWN=30;
    /** The reach of an off-hand shield from its hand bone, in model pixels (vanilla's held shield, held at its far corner). */
    static final double SHIELD_REACH=24;
    /** Bones a model may have: prepare records the bones a draw shows as bits of one long. */
    static final int MAX_BONES=64;
    private static final Set<String> FLAGS=Set.of("pennant","standard");
    final ModelPart top;
    final String troop,rig,hand,idleArms,portrait,weapon;
    final Identifier veteran,glow,outer;final int glowFrom;
    /** Texels a model px of this troop's PNGs carry (the file's "density", 1 unless it says; texel-3 plan): above 1 the build also ships a one-texel-a-px twin of each PNG (TroopModels.twin). */
    final int density;
    /** The faces of this troop (texel-3 faces design F2): how many face bones its head has (0 for a troop with none), the face a card portrait wears, and the face a singular character is pinned to (-1 for none). */
    final int faceCount,faceCard,faceFixed;
    private final Map<Integer,Identifier> skins=new TreeMap<>();
    /** Every weapon tier some weapon part (weaponTiers) shows at: the tiers whose weapon-part art the troop has. */
    private final Set<Integer> partTiers=new TreeSet<>();
    private final Map<String,ModelPart> parts=new LinkedHashMap<>();
    private final Map<String,Bone> info=new LinkedHashMap<>();
    private final Map<String,List<ModelPart>> paths=new HashMap<>();
    private final List<List<ModelPart>> teamPaths=new ArrayList<>();
    private final List<Bone> teamBones=new ArrayList<>();
    private final boolean[] teamOn;
    private final Map<String,String> hands;
    private final Box rest;
    /** The highest point at rest of the skin bones of every armour and weapon tier (no team cloth), in model pixels (y down), or NaN for a model without one: where the rank chevrons sit, so no tier's crest reaches them. */
    private final double crown;
    /** The frustum test's reach about the entity, in blocks: horizontal radius, and the bottom and top over its feet. */
    private final double cullRadius,cullBottom,cullTop;
    /** The outer layer's twin (rig boxes dilated by the rig's outer), or null; its bones copy this model's pose. */
    final ModelPart outerTop;private final Map<String,ModelPart> outerParts=new HashMap<>();
    boolean folded;
    private long shown;
    /** The bits of the team bones in a shownMask. */
    private final long teamBits;
    /** The weapon's box corners in its hand bone's frame (model pixels), for the sampled cull box. */
    private final List<double[]> weaponPoints;
    /** How much taller than its rig this troop stands (rootScale); set by TroopModelLoader from the rig. */
    private float rootK=1f;
    /** The frustum test's reach (radius, bottom and top in blocks) with the clips sampled: normal, and posed with the whole-body clips; worked out on first use. */
    private double[] normalCull,posedCull;
    /** The inputs of the last setAngles: age, limb angle, limb distance, head yaw and head pitch (the harness reads them). */
    final float[] lastInputs=new float[5];
    /** The relief layer (troop depth design, section 3): per key ("1", "2" or "3") the mesh of each skin bone that has one, in file order. */
    private final Map<String,Map<String,TroopMesh>> relief;
    /** Per key, the bones the relief walk visits: the ones with a mesh and their ancestors. */
    private final Map<String,Set<String>> reliefWalk;
    /** Each bone's children in file order, "" holding the roots. */
    private final Map<String,List<String>> kids=new HashMap<>();
    /** The most relief quads a skin may have; the build fails above it and so does the load. */
    static final int MAX_RELIEF_QUADS=256;
    /** The key the next render draws relief for (armRelief), spent by that one pass; null for none. */
    private String armed;
    /** What the last render did with the arm: whether it was spent, and the quads it drew (TroopRenderer.record reads them). */
    boolean reliefSpent;int reliefDrawn;

    private TroopModel(){
        super(RenderLayer::getEntityCutout);top=new ModelPart(List.of(),Map.of());troop=rig=hand=idleArms=portrait=weapon="";
        veteran=glow=outer=null;glowFrom=1;density=1;faceCount=0;faceCard=0;faceFixed=-1;teamOn=new boolean[0];hands=Map.of();rest=new Box(0,0,0,0,0,0);outerTop=null;
        cullRadius=cullBottom=cullTop=0;crown=Double.NaN;teamBits=0;weaponPoints=List.of();relief=Map.of();reliefWalk=Map.of();
    }
    /** An empty placeholder: TroopRenderer is built before TroopModelLoader may have loaded anything. */
    static <T extends MobEntity> TroopModel<T> empty(){return new TroopModel<>();}

    /**
     * @param json the generated model or rig file; @param top the part TroopModelLoader.parse built from it;
     * @param hands the rig's hand bones by side; @param outerTop the outer twin, or null;
     * @param weaponPoints the corners of the weapon's boxes in its hand bone's frame (model pixels), empty for none.
     */
    TroopModel(JsonObject json,ModelPart top,Map<String,String> hands,ModelPart outerTop,List<double[]> weaponPoints){
        super(RenderLayer::getEntityCutout);
        this.top=top;this.hands=Map.copyOf(hands);this.outerTop=outerTop;this.weaponPoints=List.copyOf(weaponPoints);
        troop=str(json,"troop");rig=str(json,"rig");hand=str(json,"hand");idleArms=json.has("idleArms")?str(json,"idleArms"):"hang";
        portrait=str(json,"portrait");weapon=str(json,"weapon");
        veteran=id(str(json,"veteran"));glow=id(str(json,"glow"));outer=id(str(json,"outer"));
        glowFrom=json.has("glowFrom")?json.get("glowFrom").getAsInt():1;
        density=json.has("density")?json.get("density").getAsInt():1;
        if(density<1||density>4)throw new IllegalArgumentException("density "+density+" is not 1 to 4");
        JsonObject faces=json.has("faces")&&json.get("faces").isJsonObject()?json.getAsJsonObject("faces"):null;
        faceCount=faces==null?0:faces.get("count").getAsInt();faceCard=faces==null||!faces.has("card")?0:faces.get("card").getAsInt();faceFixed=faces==null||!faces.has("fixed")?-1:faces.get("fixed").getAsInt();
        if(json.has("skins"))for(var e:json.getAsJsonObject("skins").entrySet())skins.put(Integer.parseInt(e.getKey()),id(e.getValue().getAsString()));
        JsonArray bones=json.getAsJsonArray("bones");
        if(bones.size()>MAX_BONES)throw new IllegalArgumentException(bones.size()+" bones, more than "+MAX_BONES);
        Map<String,double[][]> rot=new HashMap<>();Map<String,double[]> at=new HashMap<>();Map<String,List<double[]>> corners=new HashMap<>();
        double[] lo={Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE},hi={-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
        Set<String> restHidden=new HashSet<>(),crownHidden=new HashSet<>();Map<String,Set<Integer>> armsByBone=new LinkedHashMap<>();double crownY=Double.MAX_VALUE;
        int[] size=json.has("size")?new int[]{json.getAsJsonArray("size").get(0).getAsInt(),json.getAsJsonArray("size").get(1).getAsInt()}:new int[]{64,64};
        Map<String,Map<String,TroopMesh>> rel=new TreeMap<>();
        for(JsonElement el:bones){
            JsonObject b=el.getAsJsonObject();String name=str(b,"name"),parent=str(b,"parent");
            kids.computeIfAbsent(parent,k->new ArrayList<>()).add(name);
            if(b.has("relief"))for(var re:b.getAsJsonObject("relief").entrySet()){
                TroopMesh mesh;try{mesh=TroopMesh.parse(re.getValue().getAsJsonArray(),size[0],size[1]);}catch(IllegalArgumentException e){throw new IllegalArgumentException("bone "+name+" relief "+re.getKey()+": "+e.getMessage(),e);}
                if(mesh.quads()>0)rel.computeIfAbsent(re.getKey(),k->new LinkedHashMap<>()).put(name,mesh);
            }
            Set<Integer> stages=new TreeSet<>();if(b.has("stages"))for(var s:b.getAsJsonArray("stages"))stages.add(s.getAsInt());
            Set<Integer> arms=new TreeSet<>();if(b.has("weaponTiers"))for(var s:b.getAsJsonArray("weaponTiers"))arms.add(s.getAsInt());if(!arms.isEmpty())armsByBone.computeIfAbsent(parent,k->new TreeSet<>()).addAll(arms);
            Set<Integer> variants=new TreeSet<>();if(b.has("variants"))for(var s:b.getAsJsonArray("variants"))variants.add(s.getAsInt());
            Bone bone=new Bone(name,parent,str(b,"part"),b.has("team")&&b.get("team").getAsBoolean(),str(b,"tag"),Collections.unmodifiableSet(stages),Collections.unmodifiableSet(arms),Collections.unmodifiableSet(variants));
            ModelPart p=parent.isEmpty()?top.getChild(name):parts.get(parent).getChild(name);
            parts.put(name,p);info.put(name,bone);
            List<ModelPart> path=new ArrayList<>(parent.isEmpty()?List.of(top):paths.get(parent));path.add(p);paths.put(name,List.copyOf(path));
            if(bone.team()){teamPaths.add(paths.get(name));teamBones.add(bone);}
            if(outerTop!=null&&bone.part().isEmpty()){ModelPart twin=parent.isEmpty()?outerTop.getChild(name):outerParts.get(parent).getChild(name);outerParts.put(name,twin);}
            // every bone's boxes at rest, in model space: the rest bounds take the skin bones shown at armour and weapon tier I in the idle stance
            double[][] r=rotation(b.has("rotation")?vec(b,"rotation"):new double[3]);double[] pivot=vec(b,"pivot");
            double[][] pr=parent.isEmpty()?identity():rot.get(parent);double[] pt=parent.isEmpty()?new double[3]:at.get(parent);
            double[] t=add(pt,apply(pr,pivot));double[][] wr=mul(pr,r);rot.put(name,wr);at.put(name,t);
            List<double[]> mine=new ArrayList<>();
            for(JsonElement be:b.getAsJsonArray("boxes")){
                JsonObject x=be.getAsJsonObject();double[] o=vec(x,"origin"),s=vec(x,"size"),d=x.has("dilation")?vec(x,"dilation"):new double[3];
                for(int c=0;c<8;c++){
                    double[] corner={(c&1)==0?o[0]-d[0]:o[0]+s[0]+d[0],(c&2)==0?o[1]-d[1]:o[1]+s[1]+d[1],(c&4)==0?o[2]-d[2]:o[2]+s[2]+d[2]};
                    mine.add(add(t,apply(wr,corner)));
                }
            }
            corners.put(name,mine);
            boolean swapped="folded".equals(idleArms)?name.equals("right_arm")||name.equals("left_arm"):name.equals("arms_folded");
            if(bone.team()||swapped||crownHidden.contains(parent))crownHidden.add(name);else for(double[] w:mine)crownY=Math.min(crownY,w[1]);
            boolean hidden=bone.team()||!stages.isEmpty()&&!stages.contains(1)||!arms.isEmpty()&&!arms.contains(1)||!variants.isEmpty()&&!variants.contains(0)||restHidden.contains(parent)
                ||("folded".equals(idleArms)?name.equals("right_arm")||name.equals("left_arm"):name.equals("arms_folded"));
            if(hidden){restHidden.add(name);continue;}
            for(double[] w:mine)for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],w[i]);hi[i]=Math.max(hi[i],w[i]);}
        }
        teamOn=new boolean[teamPaths.size()];
        long team=0;int bit=0;for(Bone b:info.values()){if(b.team())team|=1L<<bit;bit++;}teamBits=team;
        rest=lo[0]>hi[0]?new Box(0,0,0,0,0,0):new Box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]);
        crown=crownY==Double.MAX_VALUE?Double.NaN:crownY;
        // a weapon tier of parts is whole only when every bone carrying a tier I weapon part carries one of that tier too (Task 9
        // review M7; build_skins.part_tiers is the release gate's twin): a tier II cap without its tier II guard draws tier I
        for(var tiers:armsByBone.values())for(int t:tiers)if(armsByBone.values().stream().allMatch(o->!o.contains(1)||o.contains(t)))partTiers.add(t);
        // The frustum test's reach (TroopRenderer.shouldRender): every box of every stage, team part and flag at rest, the head
        // turned any way about its pivot, and each arm swung any way about its shoulder with what its hand holds (the weapon in
        // the weapon hand, a shield's reach in the other), so the head look and Task 5b's arm clips stay inside it.
        double radius=0,low=-Double.MAX_VALUE,high=Double.MAX_VALUE; // y down: high is the top (the lowest y)
        for(List<double[]> list:corners.values())for(double[] w:list){radius=Math.max(radius,Math.hypot(w[0],w[2]));high=Math.min(high,w[1]);low=Math.max(low,w[1]);}
        List<double[]> spheres=new ArrayList<>(); // x, y, z, r
        if(info.containsKey("head"))spheres.add(sphere(at.get("head"),reachOf("head",at.get("head"),corners)));
        String weaponBone=this.hands.get(hand);
        for(String side:List.of("right","left")){
            String h=this.hands.get(side);Bone hb=h==null?null:info.get(h);if(hb==null||hb.parent().isEmpty())continue;
            double[] shoulder=at.get(hb.parent()),held=at.get(h);double item=0;
            if(h.equals(weaponBone))for(double[] p:weaponPoints)item=Math.max(item,Math.sqrt(p[0]*p[0]+p[1]*p[1]+p[2]*p[2]));
            else item=SHIELD_REACH;
            spheres.add(sphere(shoulder,Math.max(reachOf(hb.parent(),shoulder,corners),dist(shoulder,held)+item)));
        }
        for(double[] s:spheres){radius=Math.max(radius,Math.hypot(s[0],s[2])+s[3]);high=Math.min(high,s[1]-s[3]);low=Math.max(low,s[1]+s[3]);}
        boolean any=low>-Double.MAX_VALUE;
        cullRadius=any?radius/16+.25:1;cullTop=any?1.501-high/16+.25:2;cullBottom=any?1.501-low/16-.25:0;
        Map<String,Set<String>> walk=new TreeMap<>();
        for(var k:rel.entrySet()){
            String key=k.getKey();int total=0;
            if(!key.matches("[1-9][0-9]*")||!skins.containsKey(Integer.parseInt(key)))throw new IllegalArgumentException("relief for key "+key+", which is no skin of the troop (the veteran trim takes none)");
            Set<String> visit=new HashSet<>();
            for(var m:k.getValue().entrySet()){total+=m.getValue().quads();for(String n=m.getKey();!n.isEmpty()&&visit.add(n);n=info.get(n).parent());}
            if(total>MAX_RELIEF_QUADS)throw new IllegalArgumentException("relief "+key+" has "+total+" quads, more than "+MAX_RELIEF_QUADS);
            walk.put(key,visit);
        }
        relief=rel;reliefWalk=walk;
    }
    private double reachOf(String bone,double[] from,Map<String,List<double[]>> corners){
        double r=0;for(var b:info.values())if(b.name().equals(bone)||descends(b.name(),bone))for(double[] w:corners.get(b.name()))r=Math.max(r,dist(from,w));return r;
    }
    private boolean descends(String bone,String ancestor){for(String p=info.get(bone).parent();!p.isEmpty();p=info.get(p).parent())if(p.equals(ancestor))return true;return false;}
    private static double[] sphere(double[] c,double r){return new double[]{c[0],c[1],c[2],r};}
    private static double dist(double[] a,double[] b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return Math.sqrt(x*x+y*y+z*z);}

    @Override public ModelPart getPart(){return top;}
    /** A bone by its name, for AnimationHelper (the default would take "root" for the unnamed top part). */
    @Override public Optional<ModelPart> getChild(String name){return Optional.ofNullable(parts.get(name));}
    public ModelPart bone(String name){return parts.get(name);}
    public Set<String> bones(){return Collections.unmodifiableSet(parts.keySet());}
    /** The skin bones shown at armour and weapon tier I, at rest, in model pixels (y down, the ground at y 24). */
    public Box restBounds(){return rest;}
    /** The highest point at rest over every armour and weapon tier's skin bones (no team cloth), in model pixels (y down, the ground at y 24), or NaN. */
    public double crown(){return crown;}
    /** Every team bone's path from the root part to the bone, in file order. */
    public List<List<ModelPart>> teamPaths(){return Collections.unmodifiableList(teamPaths);}
    /** The path from the root part to a bone (empty for a bone the model lacks). */
    public List<ModelPart> path(String bone){return paths.getOrDefault(bone,List.of());}
    /** The path from the root part to the rig's hand bone for "right" or "left" (empty when the rig has none). */
    public List<ModelPart> handPath(String hand){String bone=hands.get(hand);return bone==null||!paths.containsKey(bone)?List.of():paths.get(bone);}
    /** The rig's hand bone for "right" or "left" (null when the rig has none). */
    public String handBone(String side){return hands.get(side);}
    /**
     * The box the frustum test keeps an entity of this model by: its feet, every part and flag, the reach of its head, arms and
     * weapon, and every base and overlay clip; a dead or posed troop (TroopAnimator.posed) also every whole-body clip.
     */
    public Box cullBox(Entity e){double[] c=cullReach(TroopAnimator.posed(e));return new Box(e.getX()-c[0],e.getY()+c[1],e.getZ()-c[0],e.getX()+c[0],e.getY()+c[2],e.getZ()+c[0]);}
    /** The cull reach (radius, bottom and top over the feet, in blocks), the whole-body clips in it for posed. */
    public double[] cullReach(boolean posed){
        if(normalCull==null){
            double[] n={cullRadius,cullBottom,cullTop};
            Map<String,Animation> plain=TroopClips.of(rig);
            if(!plain.isEmpty()){
                for(String clip:TroopClips.NAMES)if(!WHOLE.contains(clip))sample(clip,n);
                Spec.TroopsRow row=TroopAnimator.row(this);
                for(Animation a:TroopTells.own(troop,row==null?null:row.house(),rig))sample(a,n); // its tell, overrides and House attack: none is a whole-body clip
                double[] p=n.clone();for(String clip:WHOLE)sample(clip,p);
                top.traverse().forEach(ModelPart::resetTransform);normalCull=n;posedCull=p;
            }else{normalCull=n;posedCull=n;}
        }
        return (posed?posedCull:normalCull).clone();
    }
    /** The clips only a dead or posed troop plays. */
    private static final Set<String> WHOLE=Set.of("death","kneel","point","sit","fall","stand");
    /** Grows a reach by one of the rig's clips, plain and mirrored. */
    private void sample(String clip,double[] reach){
        for(Map<String,Animation> set:List.of(TroopClips.of(rig),TroopClips.mirrored(rig))){
            Animation a=set.get(clip);if(a!=null)sample(a,reach);
        }
    }
    /** Grows a reach by one clip, every 50 ms: every box, the weapon at its hand and a shield's reach at the other. */
    private void sample(Animation a,double[] reach){
        for(float t=0;t<=a.lengthInSeconds()+1e-4f;t+=.05f){
            top.traverse().forEach(ModelPart::resetTransform);TroopAnimator.animate(this,a,t,1f);TroopAnimator.rootScale(this);
            double[] e=extent();reach[0]=Math.max(reach[0],e[0]+.25);reach[2]=Math.max(reach[2],e[2]+.25);reach[1]=Math.min(reach[1],e[1]-.25);
        }
    }
    /**
     * How far the pose the bones are in now reaches about the feet, in blocks, as cullReach reads it (no margin): the horizontal
     * radius, the bottom and the top (over the feet). Every box counts, shown or not, the weapon at its hand bone and a shield's
     * reach about the other hand. The harness measures drawn poses against the cull reach with it.
     */
    double[] extent(){
        double[] r={0,Double.MAX_VALUE,-Double.MAX_VALUE}; // radius, top (lowest y), bottom, in model pixels
        Vector3f v=new Vector3f();
        top.forEachCuboid(new MatrixStack(),(entry,path,i,q)->{for(int c=0;c<8;c++){
            entry.getPositionMatrix().transformPosition((c&1)==0?q.minX/16f:q.maxX/16f,(c&2)==0?q.minY/16f:q.maxY/16f,(c&4)==0?q.minZ/16f:q.maxZ/16f,v);grow(r,v.x*16,v.y*16,v.z*16,0);}});
        String weaponBone=hands.get(hand);
        for(String side:List.of("right","left")){
            String h=hands.get(side);if(h==null||!paths.containsKey(h))continue;
            MatrixStack ms=new MatrixStack();for(ModelPart p:paths.get(h))p.rotate(ms);
            if(h.equals(weaponBone))for(double[] w:weaponPoints){ms.peek().getPositionMatrix().transformPosition((float)w[0]/16f,(float)w[1]/16f,(float)w[2]/16f,v);grow(r,v.x*16,v.y*16,v.z*16,0);}
            else{ms.peek().getPositionMatrix().transformPosition(0,0,0,v);grow(r,v.x*16,v.y*16,v.z*16,SHIELD_REACH);}
        }
        return new double[]{r[0]/16,1.501-r[2]/16,1.501-r[1]/16};
    }
    private static void grow(double[] r,double x,double y,double z,double radius){r[0]=Math.max(r[0],Math.hypot(x,z)+radius);r[1]=Math.min(r[1],y-radius);r[2]=Math.max(r[2],y+radius);}
    /** Sets rootScale from this troop's rig: (24 - its root's pivot y) / (24 - the rig's). */
    void rig(TroopModel<?> rigModel){
        ModelPart mine=parts.get("root"),theirs=rigModel==null?null:rigModel.bone("root");
        if(mine!=null&&theirs!=null){float r=24-theirs.getDefaultTransform().pivotY;rootK=r<=0?1f:(24-mine.getDefaultTransform().pivotY)/r;}
        normalCull=posedCull=null;
    }
    /** How much taller than its rig this troop stands: TroopAnimator scales the root's offsets by it, so its falls land on the ground. */
    public float rootScale(){return rootK;}
    // ---- the relief layer (troop depth design, sections 3 and 4) ----
    /** The relief mesh of a bone for a key; empty when the bone has none. */
    public TroopMesh relief(String bone,String key){var m=relief.get(key);var mesh=m==null?null:m.get(bone);return mesh==null?TroopMesh.EMPTY:mesh;}
    /** The keys this model has relief for, one per own skin: "1", "2" and "3". */
    public Set<String> reliefKeys(){return Collections.unmodifiableSet(relief.keySet());}
    /** The quads of a key over all its bones. */
    public int reliefQuads(String key){var m=relief.get(key);int n=0;if(m!=null)for(TroopMesh t:m.values())n+=t.quads();return n;}
    /** The relief key of the skin a Look binds (the armour tier whose skin file draws, armourArt; a Veteran's trim takes none); null when the model has no relief for it. */
    public String reliefKey(TroopModels.Look l){
        if(relief.isEmpty()||l==null)return null;
        String key=String.valueOf(skinStage(l.armourTier()));
        return relief.containsKey(key)?key:null;
    }
    /** Arms the next render to draw the relief of a key after the skin (null disarms). One render spends it. */
    public void armRelief(String key){armed=key;reliefSpent=false;reliefDrawn=0;}
    /**
     * Draws the relief of a key through each shown bone into the consumer: the bones in file order through the matrix stack as
     * ModelPart.render walks them (each part's rotate; a part that is not visible takes its whole subtree with it; a part that is
     * hidden skips only its own mesh). Returns the quads drawn.
     */
    int renderRelief(String key,MatrixStack m,VertexConsumer vc,int light,int overlay,float r,float g,float b,float a){
        Map<String,TroopMesh> meshes=relief.get(key);Set<String> walk=reliefWalk.get(key);if(meshes==null||walk==null||!top.visible)return 0;
        m.push();top.rotate(m);int n=0;
        for(String root:kids.getOrDefault("",List.of()))if(walk.contains(root))n+=walkRelief(root,meshes,walk,m,vc,light,overlay,r,g,b,a);
        m.pop();return n;
    }
    private int walkRelief(String name,Map<String,TroopMesh> meshes,Set<String> walk,MatrixStack m,VertexConsumer vc,int light,int overlay,float r,float g,float b,float a){
        ModelPart p=parts.get(name);if(!p.visible)return 0;
        m.push();p.rotate(m);int n=0;TroopMesh mesh=meshes.get(name);
        if(mesh!=null&&!p.hidden){mesh.render(m.peek(),vc,light,overlay,r,g,b,a);n+=mesh.quads();}
        for(String child:kids.getOrDefault(name,List.of()))if(walk.contains(child))n+=walkRelief(child,meshes,walk,m,vc,light,overlay,r,g,b,a);
        m.pop();return n;
    }
    /** The skin pass, then the relief of the key the draw armed: the same consumer, light, overlay and tint, so the relief is one more layer of the skin. */
    @Override public void render(MatrixStack m,VertexConsumer vc,int light,int overlay,float r,float g,float b,float a){
        super.render(m,vc,light,overlay,r,g,b,a);
        String key=armed;armed=null;
        if(key!=null){reliefDrawn=renderRelief(key,m,vc,light,overlay,r,g,b,a);reliefSpent=true;}
    }
    /** Every bone's pose (pivot x, y and z, pitch, yaw and roll) in file order, 6 floats a bone; into reuse when it fits. */
    public float[] snapshot(float[] reuse){
        float[] out=reuse!=null&&reuse.length==parts.size()*6?reuse:new float[parts.size()*6];int i=0;
        for(ModelPart p:parts.values()){out[i++]=p.pivotX;out[i++]=p.pivotY;out[i++]=p.pivotZ;out[i++]=p.pitch;out[i++]=p.yaw;out[i++]=p.roll;}
        return out;
    }
    /** Puts back a snapshot's pose (the harness measures a frame's bones with it). */
    public void applyPose(float[] pose){
        if(pose==null||pose.length!=parts.size()*6)return;int i=0;
        for(ModelPart p:parts.values()){p.pivotX=pose[i++];p.pivotY=pose[i++];p.pivotZ=pose[i++];p.pitch=pose[i++];p.yaw=pose[i++];p.roll=pose[i++];}
    }
    /** The bone names in file order (a snapshot's order). */
    public List<String> boneOrder(){return List.copyOf(parts.keySet());}
    /** The armour tier whose skin file draws at a tier: the highest own skin not above it, else the lowest (the stopgap, in-match progression design 4.4). */
    int skinStage(int stage){Integer out=null;for(int k:skins.keySet())if(k<=stage||out==null)out=k;return out==null?1:out;}
    /** The armour tier a worn tier draws as: its skin, relief, stage parts and glow are that tier's (skinStage: the highest tier at or below it with its own skin). */
    public int armourArt(int worn){return skinStage(worn);}
    /** The weapon tier a worn tier's weapon parts draw as: the highest tier at or below it that the weaponTiers parts have whole (every bone with a tier I weapon part has one of it), tier I without one (the stopgap). */
    public int weaponPartTier(int worn){int out=1;for(int t:partTiers)if(t<=worn)out=Math.max(out,t);return out;}
    /** The armour tiers this troop has its own skin for. */
    public Set<Integer> ownSkins(){return Collections.unmodifiableSet(skins.keySet());}
    /** Whether a bone is a weapon part (it has a weaponTiers list). */
    public boolean weaponPart(String bone){var b=info.get(bone);return b!=null&&!b.weaponTiers().isEmpty();}
    Identifier skin(int stage){return skins.get(skinStage(stage));}
    String hand(){return hand;}
    String idleArms(){return idleArms;}
    String weaponId(){return weapon;}
    Map<String,ModelPart> outerParts(){return outerParts;}

    /**
     * Sets visibility for one draw: parts shown at the armour tier the Look's art draws as (armourArt, its stages key) and at
     * the weapon tier its weapon parts draw as (weaponPartTier, their weaponTiers key), the arms or arms_folded by the fold
     * (any model with an arms_folded bone folds when asked; TroopAnimator.folds asks), helm parts unless
     * helmOff, and every team bone hidden for the skin pass (renderCloth shows the ones that draw: a pennant or standard
     * only for a troop on a side whose row flies one).
     */
    public void prepare(TroopModels.Look l,boolean folded,boolean helmOff){prepare(l,folded,helmOff,0);}
    /**
     * As above, wearing this face: of a troop's face bones only the one that names the variant shows (TroopFaces.shows, the variant
     * floor-modded into the troop's face count, so any number shows exactly one; a troop with no faces ignores it).
     */
    public void prepare(TroopModels.Look l,boolean folded,boolean helmOff,int variant){
        int stage=armourArt(l==null?1:l.armourTier()),arms=weaponPartTier(l==null?1:l.weaponTier());this.folded=folded&&parts.containsKey("arms_folded");
        for(Bone b:info.values()){
            boolean on=(b.stages().isEmpty()||b.stages().contains(stage))&&(b.weaponTiers().isEmpty()||b.weaponTiers().contains(arms));if(helmOff&&"helm".equals(b.tag()))on=false;
            on&=TroopFaces.shows(b.variants(),variant,faceCount);
            parts.get(b.name()).visible=on&&!b.team();
        }
        ModelPart right=parts.get("right_arm"),left=parts.get("left_arm"),fold=parts.get("arms_folded");
        if(fold!=null){fold.visible=this.folded;if(right!=null)right.visible=!this.folded;if(left!=null)left.visible=!this.folded;}
        boolean flies=l!=null&&l.side()!=null&&l.troop().pennant();
        for(int i=0;i<teamBones.size();i++){Bone b=teamBones.get(i);teamOn[i]=(b.stages().isEmpty()||b.stages().contains(stage))&&(b.weaponTiers().isEmpty()||b.weaponTiers().contains(arms))&&(!FLAGS.contains(b.part())||flies)&&(!helmOff||!"helm".equals(b.tag()));}
        long mask=0;int i=0,t=0;
        for(Bone b:info.values()){
            List<ModelPart> path=paths.get(b.name());boolean on=b.team()?teamOn[t++]:path.get(path.size()-1).visible;
            for(int k=1;on&&k<path.size()-1;k++)on=path.get(k).visible;
            if(on)mask|=1L<<i;i++;
        }
        shown=mask;
    }
    /** The bones the current draw shows (after prepare), as bits in file order; shownBones names them. */
    long shownMask(){return shown;}
    /** The skin bones the current draw shows (shownMask without the team bones, which renderCloth reports as it draws them). */
    long skinMask(){return shown&~teamBits;}
    /** The names of the bones whose bits a shownMask holds, in file order. */
    public List<String> shownBones(long mask){List<String> out=new ArrayList<>();int i=0;for(String name:info.keySet()){if((mask>>>i&1)!=0)out.add(name);i++;}return out;}

    /**
     * Draws the team bones prepare left on, each through its ancestors' transforms, into the cloth sheet's consumer.
     * @return the bones it drew, as bits in file order (a shownMask's bits)
     */
    long renderCloth(MatrixStack m,VertexConsumer vc,int light,int overlay){
        long drawn=0;int bit=0,i=0;
        for(Bone b:info.values()){
            if(b.team()&&teamOn[i++]){
                List<ModelPart> path=teamPaths.get(i-1);boolean shown=true;
                for(int k=0;k<path.size()-1;k++)shown&=path.get(k).visible;
                if(shown){
                    m.push();for(int k=0;k<path.size()-1;k++)path.get(k).rotate(m);
                    ModelPart bone=path.get(path.size()-1);bone.visible=true;bone.render(m,vc,light,overlay);bone.visible=false;
                    m.pop();drawn|=1L<<bit;
                }
            }
            bit++;
        }
        return drawn;
    }

    /**
     * Resets every part to its file pose, then TroopAnimator poses it for the entity: its rig's clips (base, overlays and
     * whole-body poses, blended), the carries (shield arm, bow twist, arms forward) and the head look, yaw within 30 degrees
     * and pitch only down.
     */
    @Override public void setAngles(T entity,float limbAngle,float limbDistance,float animationProgress,float headYaw,float headPitch){
        lastInputs[0]=animationProgress;lastInputs[1]=limbAngle;lastInputs[2]=limbDistance;lastInputs[3]=headYaw;lastInputs[4]=headPitch;
        top.traverse().forEach(ModelPart::resetTransform);
        TroopAnimator.pose(this,entity,limbAngle,limbDistance,animationProgress,headYaw,headPitch);
    }

    private static String str(JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():"";}
    private static Identifier id(String s){return s.isEmpty()?null:new Identifier(s);}
    private static double[] vec(JsonObject o,String key){JsonArray a=o.getAsJsonArray(key);double[] v=new double[a.size()];for(int i=0;i<v.length;i++)v[i]=a.get(i).getAsDouble();return v;}
    private static double[][] identity(){return new double[][]{{1,0,0},{0,1,0},{0,0,1}};}
    /** The ModelPart rotation (pitch, yaw, roll in degrees) as a matrix: Z then Y then X, as Quaternionf.rotationZYX. */
    static double[][] rotation(double[] deg){
        double x=Math.toRadians(deg[0]),y=Math.toRadians(deg[1]),z=Math.toRadians(deg[2]);
        double[][] rx={{1,0,0},{0,Math.cos(x),-Math.sin(x)},{0,Math.sin(x),Math.cos(x)}};
        double[][] ry={{Math.cos(y),0,Math.sin(y)},{0,1,0},{-Math.sin(y),0,Math.cos(y)}};
        double[][] rz={{Math.cos(z),-Math.sin(z),0},{Math.sin(z),Math.cos(z),0},{0,0,1}};
        return mul(mul(rz,ry),rx);
    }
    static double[][] mul(double[][] a,double[][] b){double[][] o=new double[3][3];for(int i=0;i<3;i++)for(int j=0;j<3;j++)for(int k=0;k<3;k++)o[i][j]+=a[i][k]*b[k][j];return o;}
    static double[] apply(double[][] m,double[] v){double[] o=new double[3];for(int i=0;i<3;i++)for(int k=0;k<3;k++)o[i]+=m[i][k]*v[k];return o;}
    private static double[] add(double[] a,double[] b){return new double[]{a[0]+b[0],a[1]+b[1],a[2]+b[2]};}
}
