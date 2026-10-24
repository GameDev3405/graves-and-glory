package gg.gravesandglory;

import com.google.common.collect.MapMaker;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.Map;
import java.util.Objects;

/**
 * Draws a troop with its own model (TroopModels.resolve, from its custom name and gg_ team), and every other mob of the
 * entity type through the vanilla renderer it was registered over. It never draws vanilla armour, held items or the elytra
 * (unit identity rule 3, Needs Kort 10): only the troop's skin, its outer layer, its team cloth, glow, weapon model and the
 * painted off-hand shield (TroopFeatures). No vanilla pose code reaches a troop: TroopModel.setAngles poses it through
 * TroopAnimator (Task 5b), and setupTransforms turns it by its body yaw only.
 * A troop whose side has not reached this client yet is not drawn at all (TroopModels.held), so no frame shows it in linen.
 */
public final class TroopRenderer<T extends MobEntity> extends LivingEntityRenderer<T, TroopModel<T>> {
    private final EntityRenderer<? super T> vanilla;
    /** The Look of the troop being drawn, for the features; null between draws. */
    TroopModels.Look look;
    /** Draws through a troop's own model since the client started (the harness reads it). */
    static long troopDraws;
    /**
     * One draw of a troop, as observed while it drew: its Look, the skin the skin pass bound, the cloth sheet the Cloth layer
     * bound (null when it did not draw), the bones drawn (the skin bones shown and the team bones the Cloth layer drew, as
     * TroopModel.shownBones reads them), whether the weapon and the shield layers drew, the relief key the skin pass drew after
     * the skin (null for a flat draw), the weapon frame drawn (-1 when no weapon drew), whether it drew its far version (a cube
     * weapon on a troop outside TroopDepth.weaponNear) and the veteran trim the Trim layer bound over the skin (null when none drew).
     */
    public record Draw(TroopModels.Look look,Identifier skin,Identifier clothSheet,long bones,boolean weapon,boolean shield,String relief,int weaponFrame,boolean weaponFar,Identifier trim){
        /** The cloth key of the bound sheet (team_cloth_<key>.png): null for linen, "(none)" when no cloth drew. */
        public String cloth(){
            if(clothSheet==null)return "(none)";String path=clothSheet.getPath(),key=path.substring(path.lastIndexOf('/')+1);
            key=key.startsWith("team_cloth_")&&key.endsWith(".png")?key.substring(11,key.length()-4):"?"+path;return key.equals("linen")?null:key;
        }
    }
    /**
     * The draws of one entity: how many, the first and the last; the bones' pose (TroopModel.snapshot) and setAngles' inputs
     * of the last draw; the tilt of the last draw and the most of any draw (degrees off upright after setupTransforms, which a
     * vanilla death tilt would show). Written on the render thread, read after a frame (the harness does).
     */
    public static final class Drawn { public volatile int count;public volatile Draw first,last;public float[] pose,inputs=new float[5];public float tilt,maxTilt;
        /** The face (TroopFaces) the last draw wore: 0 for a troop with no faces. */
        public volatile int variant;
        /** The TroopDepth frame this troop's relief and weapon quads were last counted in (one count a frame, whatever passes drew it). */
        int countedFrame; }
    private static final Map<Entity,Drawn> DRAWN=new MapMaker().weakKeys().makeMap();
    /** The draws of an entity through its own model, or null for one never drawn so. */
    public static Drawn drawn(Entity e){return DRAWN.get(e);}
    /** Whether this draw binds the one-texel-a-px twins of a density above 1 troop's PNGs (TroopModels.far; set for the draw, read by getTexture and the layers). */
    boolean far;
    /** The face the current draw wears (TroopModels.face). */
    int variant;
    /** What the layers bound and drew in the current draw (TroopFeatures writes them), and the matrix it started with. */
    Identifier boundSkin,clothSheet,trimSheet;long clothMask;boolean weaponDrawn,shieldDrawn,weaponFar;String reliefKey;int weaponFrame=-1,weaponQuads;private float tilt;private final Matrix4f start=new Matrix4f(),delta=new Matrix4f();
    private final Vector3f up=new Vector3f();

    /** Built with an empty placeholder model: entity renderers and TroopModelLoader reload in either order. */
    public TroopRenderer(EntityRendererFactory.Context ctx,EntityRenderer<? super T> vanilla,float shadowRadius){
        super(ctx,TroopModel.empty(),shadowRadius);this.vanilla=vanilla;
        addFeature(new TroopFeatures.Outer<>(this));addFeature(new TroopFeatures.Trim<>(this));addFeature(new TroopFeatures.Cloth<>(this));addFeature(new TroopFeatures.Glow<>(this));
        addFeature(new TroopFeatures.Weapon<>(this));addFeature(new TroopFeatures.Shield<>(this,ctx.getHeldItemRenderer()));
    }
    /** The vanilla renderer every non-troop mob of this type draws through. */
    public EntityRenderer<? super T> vanilla(){return vanilla;}

    @Override public void render(T e,float yaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light){
        TroopModels.Look l=TroopModels.resolve(e);TroopModel<T> m=l==null?null:TroopModels.modelOf(l.troop().id());
        if(m==null){vanilla.render(e,yaw,tickDelta,matrices,consumers,light);return;}
        if(TroopModels.held(e,l))return;
        model=m;look=l;far=TroopModels.far(e,l);
        // Folded arms (idleArms folded) open while the troop attacks or a clip holds an arm (unit identity rule 3); a helm_off scene pose hides the helm.
        variant=TroopModels.face(e,m);
        m.prepare(l,TroopAnimator.folds(m,e),TroopAnimator.helmOff(e),variant);
        boundSkin=clothSheet=trimSheet=null;clothMask=0;weaponDrawn=shieldDrawn=weaponFar=false;reliefKey=null;weaponFrame=-1;weaponQuads=0;tilt=0;start.set(matrices.peek().getPositionMatrix());
        // Within reach of the camera (TroopDepth.near) the skin pass draws the relief of the skin it binds after the skin; the model spends the arm in that one pass.
        String relief=TroopDepth.near(e)?m.reliefKey(l):null;m.armRelief(relief);
        try{super.render(e,yaw,tickDelta,matrices,consumers,light);reliefKey=m.reliefSpent?relief:null;troopDraws++;record(e,l,m);}finally{m.armRelief(null);look=null;far=false;}
    }
    private void record(Entity e,TroopModels.Look l,TroopModel<?> m){
        Drawn d=DRAWN.computeIfAbsent(e,k->new Drawn());Draw last=d.last;long bones=m.skinMask()|clothMask;
        Draw now=last!=null&&last.look().equals(l)&&Objects.equals(last.skin(),boundSkin)&&Objects.equals(last.clothSheet(),clothSheet)&&last.bones()==bones
            &&last.weapon()==weaponDrawn&&last.shield()==shieldDrawn&&Objects.equals(last.relief(),reliefKey)&&last.weaponFrame()==weaponFrame&&last.weaponFar()==weaponFar&&Objects.equals(last.trim(),trimSheet)?last:new Draw(l,boundSkin,clothSheet,bones,weaponDrawn,shieldDrawn,reliefKey,weaponFrame,weaponFar,trimSheet);
        d.pose=m.snapshot(d.pose);System.arraycopy(m.lastInputs,0,d.inputs,0,5);d.tilt=tilt;d.maxTilt=Math.max(d.maxTilt,tilt);d.variant=variant;
        if(d.count==0)d.first=now;d.last=now;d.count++;
        if(d.countedFrame!=TroopDepth.frame()){d.countedFrame=TroopDepth.frame();TroopDepth.count(reliefKey==null?0:m.reliefDrawn,weaponQuads);}
    }
    @Override public Identifier getTexture(T e){
        TroopModels.Look l=look!=null?look:TroopModels.resolve(e);Identifier skin=l==null?null:TroopModels.skin(l,look!=null&&far);
        Identifier out=skin!=null?skin:vanilla.getTexture(e);if(look!=null)boundSkin=out;return out;
    }
    /** Measures the draw's tilt: how far the model's up axis is from upright, after setupTransforms and the vanilla flip (no scale is applied). */
    @Override protected void scale(T e,MatrixStack matrices,float tickDelta){
        if(look==null)return;
        delta.set(start).invert().mul(matrices.peek().getPositionMatrix()).transformDirection(0,1,0,up);
        float len=up.length();tilt=len<1e-6f?180f:(float)Math.toDegrees(Math.acos(Math.max(-1f,Math.min(1f,-up.y/len))));
    }
    /** Only the body yaw: never the vanilla death tilt, sleep or riptide spin (Task 5b's clips own those). */
    @Override protected void setupTransforms(T e,MatrixStack matrices,float animationProgress,float bodyYaw,float tickDelta){matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-bodyYaw));}
    /** As MobEntityRenderer: a hidden custom name shows only on the mob under the crosshair. */
    @Override protected boolean hasLabel(T e){return super.hasLabel(e)&&(e.shouldRenderName()||e.hasCustomName()&&e==dispatcher.targetedEntity);}
    /**
     * A troop's frustum test takes its model's whole reach (TroopModel.cullBox: flags, weapon, head and arm swings), not the
     * vanilla box, so a troop whose standard or blade is in view is drawn; a held troop is not drawn at all.
     */
    @Override public boolean shouldRender(T e,Frustum frustum,double x,double y,double z){
        TroopModels.Look l=TroopModels.resolve(e);TroopModel<T> m=l==null?null:TroopModels.modelOf(l.troop().id());
        if(m==null)return vanilla.shouldRender(e,frustum,x,y,z);
        if(TroopModels.held(e,l)||!e.shouldRender(x,y,z))return false;
        return e.ignoreCameraFrustum||frustum.isVisible(m.cullBox(e));
    }
    @Override public Vec3d getPositionOffset(T e,float tickDelta){return vanilla.getPositionOffset(e,tickDelta);}
}
