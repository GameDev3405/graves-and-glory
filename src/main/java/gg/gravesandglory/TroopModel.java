package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import java.util.*;

/**
 * One troop's own model (or a bare rig), built by TroopModelLoader from its generated models/troop JSON. Proportions are
 * baked in that file; this class only shows and hides parts per stage, fold and helm, and poses the rest pose with the head
 * look until Task 5b's clips. Team bones (panels, pennant, standard) are hidden for the skin pass and drawn by
 * TroopFeatures.Cloth from the cloth sheet. One instance serves every entity of the troop, so prepare and setAngles run
 * right before each draw.
 */
public final class TroopModel<T extends MobEntity> extends SinglePartEntityModel<T> {
    /** A bone as the generated file describes it. */
    record Bone(String name,String parent,String part,boolean team,String tag,Set<Integer> stages){}
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
    private final Map<Integer,Identifier> skins=new TreeMap<>();
    private final Map<String,ModelPart> parts=new LinkedHashMap<>();
    private final Map<String,Bone> info=new LinkedHashMap<>();
    private final Map<String,List<ModelPart>> paths=new HashMap<>();
    private final List<List<ModelPart>> teamPaths=new ArrayList<>();
    private final List<Bone> teamBones=new ArrayList<>();
    private final boolean[] teamOn;
    private final Map<String,String> hands;
    private final Box rest;
    /** The frustum test's reach about the entity, in blocks: horizontal radius, and the bottom and top over its feet. */
    private final double cullRadius,cullBottom,cullTop;
    /** The outer layer's twin (rig boxes dilated by the rig's outer), or null; its bones copy this model's pose. */
    final ModelPart outerTop;private final Map<String,ModelPart> outerParts=new HashMap<>();
    boolean folded;
    private long shown;

    private TroopModel(){
        super(RenderLayer::getEntityCutout);top=new ModelPart(List.of(),Map.of());troop=rig=hand=idleArms=portrait=weapon="";
        veteran=glow=outer=null;glowFrom=1;teamOn=new boolean[0];hands=Map.of();rest=new Box(0,0,0,0,0,0);outerTop=null;
        cullRadius=cullBottom=cullTop=0;
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
        this.top=top;this.hands=Map.copyOf(hands);this.outerTop=outerTop;
        troop=str(json,"troop");rig=str(json,"rig");hand=str(json,"hand");idleArms=json.has("idleArms")?str(json,"idleArms"):"hang";
        portrait=str(json,"portrait");weapon=str(json,"weapon");
        veteran=id(str(json,"veteran"));glow=id(str(json,"glow"));outer=id(str(json,"outer"));
        glowFrom=json.has("glowFrom")?json.get("glowFrom").getAsInt():1;
        if(json.has("skins"))for(var e:json.getAsJsonObject("skins").entrySet())skins.put(Integer.parseInt(e.getKey()),id(e.getValue().getAsString()));
        JsonArray bones=json.getAsJsonArray("bones");
        if(bones.size()>MAX_BONES)throw new IllegalArgumentException(bones.size()+" bones, more than "+MAX_BONES);
        Map<String,double[][]> rot=new HashMap<>();Map<String,double[]> at=new HashMap<>();Map<String,List<double[]>> corners=new HashMap<>();
        double[] lo={Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE},hi={-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
        Set<String> restHidden=new HashSet<>();
        for(JsonElement el:bones){
            JsonObject b=el.getAsJsonObject();String name=str(b,"name"),parent=str(b,"parent");
            Set<Integer> stages=new TreeSet<>();if(b.has("stages"))for(var s:b.getAsJsonArray("stages"))stages.add(s.getAsInt());
            Bone bone=new Bone(name,parent,str(b,"part"),b.has("team")&&b.get("team").getAsBoolean(),str(b,"tag"),Collections.unmodifiableSet(stages));
            ModelPart p=parent.isEmpty()?top.getChild(name):parts.get(parent).getChild(name);
            parts.put(name,p);info.put(name,bone);
            List<ModelPart> path=new ArrayList<>(parent.isEmpty()?List.of(top):paths.get(parent));path.add(p);paths.put(name,List.copyOf(path));
            if(bone.team()){teamPaths.add(paths.get(name));teamBones.add(bone);}
            if(outerTop!=null&&bone.part().isEmpty()){ModelPart twin=parent.isEmpty()?outerTop.getChild(name):outerParts.get(parent).getChild(name);outerParts.put(name,twin);}
            // every bone's boxes at rest, in model space: the rest bounds take the skin bones shown at stage 1 in the idle stance
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
            boolean hidden=bone.team()||!stages.isEmpty()&&!stages.contains(1)||restHidden.contains(parent)
                ||("folded".equals(idleArms)?name.equals("right_arm")||name.equals("left_arm"):name.equals("arms_folded"));
            if(hidden){restHidden.add(name);continue;}
            for(double[] w:mine)for(int i=0;i<3;i++){lo[i]=Math.min(lo[i],w[i]);hi[i]=Math.max(hi[i],w[i]);}
        }
        teamOn=new boolean[teamPaths.size()];
        rest=lo[0]>hi[0]?new Box(0,0,0,0,0,0):new Box(lo[0],lo[1],lo[2],hi[0],hi[1],hi[2]);
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
    }
    private double reachOf(String bone,double[] from,Map<String,List<double[]>> corners){
        double r=0;for(var b:info.values())if(b.name().equals(bone)||descends(b.name(),bone))for(double[] w:corners.get(b.name()))r=Math.max(r,dist(from,w));return r;
    }
    private boolean descends(String bone,String ancestor){for(String p=info.get(bone).parent();!p.isEmpty();p=info.get(p).parent())if(p.equals(ancestor))return true;return false;}
    private static double[] sphere(double[] c,double r){return new double[]{c[0],c[1],c[2],r};}
    private static double dist(double[] a,double[] b){double x=a[0]-b[0],y=a[1]-b[1],z=a[2]-b[2];return Math.sqrt(x*x+y*y+z*z);}

    @Override public ModelPart getPart(){return top;}
    public ModelPart bone(String name){return parts.get(name);}
    public Set<String> bones(){return Collections.unmodifiableSet(parts.keySet());}
    /** The skin bones shown at stage 1, at rest, in model pixels (y down, the ground at y 24). */
    public Box restBounds(){return rest;}
    /** Every team bone's path from the root part to the bone, in file order. */
    public List<List<ModelPart>> teamPaths(){return Collections.unmodifiableList(teamPaths);}
    /** The path from the root part to a bone (empty for a bone the model lacks). */
    public List<ModelPart> path(String bone){return paths.getOrDefault(bone,List.of());}
    /** The path from the root part to the rig's hand bone for "right" or "left" (empty when the rig has none). */
    public List<ModelPart> handPath(String hand){String bone=hands.get(hand);return bone==null||!paths.containsKey(bone)?List.of():paths.get(bone);}
    /** The box the frustum test keeps an entity of this model by: its feet, every part and flag, and the reach of its head, arms and weapon. */
    public Box cullBox(Entity e){return new Box(e.getX()-cullRadius,e.getY()+cullBottom,e.getZ()-cullRadius,e.getX()+cullRadius,e.getY()+cullTop,e.getZ()+cullRadius);}
    Identifier skin(int stage){Identifier out=null;for(var e:skins.entrySet())if(e.getKey()<=stage||out==null)out=e.getValue();return out;}
    String hand(){return hand;}
    String idleArms(){return idleArms;}
    String weaponId(){return weapon;}
    Map<String,ModelPart> outerParts(){return outerParts;}

    /**
     * Sets visibility for one draw: parts shown at the Look's stage, the arms or arms_folded by the fold, helm parts unless
     * helmOff, and every team bone hidden for the skin pass (renderCloth shows the ones that draw: a pennant or standard
     * only for a troop on a side whose row flies one).
     */
    public void prepare(TroopModels.Look l,boolean folded,boolean helmOff){
        int stage=l==null?1:l.stage();this.folded=folded&&"folded".equals(idleArms);
        for(Bone b:info.values()){
            boolean on=b.stages().isEmpty()||b.stages().contains(stage);if(helmOff&&"helm".equals(b.tag()))on=false;
            parts.get(b.name()).visible=on&&!b.team();
        }
        ModelPart right=parts.get("right_arm"),left=parts.get("left_arm"),fold=parts.get("arms_folded");
        if(fold!=null){fold.visible=this.folded;if(right!=null)right.visible=!this.folded;if(left!=null)left.visible=!this.folded;}
        boolean flies=l!=null&&l.side()!=null&&l.troop().pennant();
        for(int i=0;i<teamBones.size();i++){Bone b=teamBones.get(i);teamOn[i]=(b.stages().isEmpty()||b.stages().contains(stage))&&(!FLAGS.contains(b.part())||flies)&&(!helmOff||!"helm".equals(b.tag()));}
        long mask=0;int i=0,t=0;
        for(Bone b:info.values()){
            List<ModelPart> path=paths.get(b.name());boolean on=b.team()?teamOn[t++]:path.get(path.size()-1).visible;
            for(int k=1;on&&k<path.size()-1;k++)on=path.get(k).visible;
            if(on)mask|=1L<<i;i++;
        }
        shown=mask;
    }
    /** Whether the team bone of this part id draws in the current draw (after prepare). */
    boolean teamShown(String part){for(int i=0;i<teamBones.size();i++)if(teamBones.get(i).part().equals(part)&&teamOn[i])return true;return false;}
    /** The bones the current draw shows (after prepare), as bits in file order; shownBones names them. */
    long shownMask(){return shown;}
    /** The names of the bones whose bits a shownMask holds, in file order. */
    public List<String> shownBones(long mask){List<String> out=new ArrayList<>();int i=0;for(String name:info.keySet()){if((mask>>>i&1)!=0)out.add(name);i++;}return out;}

    /** Draws the team bones prepare left on, each through its ancestors' transforms, into the cloth sheet's consumer. */
    void renderCloth(MatrixStack m,VertexConsumer vc,int light,int overlay){
        for(int i=0;i<teamPaths.size();i++){
            if(!teamOn[i])continue;List<ModelPart> path=teamPaths.get(i);boolean shown=true;
            for(int k=0;k<path.size()-1;k++)shown&=path.get(k).visible;
            if(!shown)continue;
            m.push();for(int k=0;k<path.size()-1;k++)path.get(k).rotate(m);
            ModelPart bone=path.get(path.size()-1);bone.visible=true;bone.render(m,vc,light,overlay);bone.visible=false;
            m.pop();
        }
    }

    /** Until Task 5b: the rest pose (arms forward for idleArms forward) plus the head look, yaw within 30 degrees and pitch only down. */
    @Override public void setAngles(T entity,float limbAngle,float limbDistance,float animationProgress,float headYaw,float headPitch){
        top.traverse().forEach(ModelPart::resetTransform);
        ModelPart head=parts.get("head");
        if(head!=null){head.yaw+=MathHelper.clamp(MathHelper.wrapDegrees(headYaw),-HEAD_YAW,HEAD_YAW)*RAD;head.pitch+=MathHelper.clamp(headPitch,0,HEAD_DOWN)*RAD;}
        if("forward".equals(idleArms))for(String arm:List.of("right_arm","left_arm")){ModelPart p=parts.get(arm);if(p!=null)p.pitch-=90*RAD;}
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
