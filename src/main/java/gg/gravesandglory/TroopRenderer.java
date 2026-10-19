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
import java.util.Map;
import java.util.Objects;

/**
 * Draws a troop with its own model (TroopModels.resolve, from its custom name and gg_ team), and every other mob of the
 * entity type through the vanilla renderer it was registered over. It never draws vanilla armour, held items or the elytra
 * (unit identity rule 3, Needs Kort 10): only the troop's skin, its outer layer, its team cloth, glow, weapon model and the
 * painted off-hand shield (TroopFeatures). No vanilla pose code reaches a troop; until Task 5b it stands in its rest pose.
 * A troop whose side has not reached this client yet is not drawn at all (TroopModels.held), so no frame shows it in linen.
 */
public final class TroopRenderer<T extends MobEntity> extends LivingEntityRenderer<T, TroopModel<T>> {
    private final EntityRenderer<? super T> vanilla;
    /** The Look of the troop being drawn, for the features; null between draws. */
    TroopModels.Look look;
    /** Draws through a troop's own model since the client started (the harness reads it). */
    static long troopDraws;
    /** One draw of a troop: its Look, the cloth key its team bones took (null for linen), and the bones it showed (TroopModel.shownBones). */
    public record Draw(TroopModels.Look look,String cloth,long bones){}
    /** The draws of one entity: how many, the first and the last; read after a frame (the harness does). */
    public static final class Drawn { public volatile int count;public volatile Draw first,last; }
    private static final Map<Entity,Drawn> DRAWN=new MapMaker().weakKeys().makeMap();
    /** The draws of an entity through its own model, or null for one never drawn so. */
    public static Drawn drawn(Entity e){return DRAWN.get(e);}

    /** Built with an empty placeholder model: entity renderers and TroopModelLoader reload in either order. */
    public TroopRenderer(EntityRendererFactory.Context ctx,EntityRenderer<? super T> vanilla,float shadowRadius){
        super(ctx,TroopModel.empty(),shadowRadius);this.vanilla=vanilla;
        addFeature(new TroopFeatures.Outer<>(this));addFeature(new TroopFeatures.Cloth<>(this));addFeature(new TroopFeatures.Glow<>(this));
        addFeature(new TroopFeatures.Weapon<>(this));addFeature(new TroopFeatures.Shield<>(this,ctx.getHeldItemRenderer()));
    }
    /** The vanilla renderer every non-troop mob of this type draws through. */
    public EntityRenderer<? super T> vanilla(){return vanilla;}

    @Override public void render(T e,float yaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light){
        TroopModels.Look l=TroopModels.resolve(e);TroopModel<T> m=l==null?null:TroopModels.modelOf(l.troop().id());
        if(m==null){vanilla.render(e,yaw,tickDelta,matrices,consumers,light);return;}
        if(TroopModels.held(e,l))return;
        model=m;look=l;
        // Folded arms (illagers' idleArms folded) open only to attack or cast (unit identity rule 3; Task 5b times it).
        m.prepare(l,"folded".equals(m.idleArms())&&!e.isAttacking(),false);
        try{super.render(e,yaw,tickDelta,matrices,consumers,light);troopDraws++;record(e,l,m);}finally{look=null;}
    }
    private static void record(Entity e,TroopModels.Look l,TroopModel<?> m){
        Drawn d=DRAWN.computeIfAbsent(e,k->new Drawn());Draw last=d.last;String cloth=TroopModels.clothOf(e,l);long bones=m.shownMask();
        Draw now=last!=null&&last.look().equals(l)&&Objects.equals(last.cloth(),cloth)&&last.bones()==bones?last:new Draw(l,cloth,bones);
        if(d.count==0)d.first=now;d.last=now;d.count++;
    }
    @Override public Identifier getTexture(T e){TroopModels.Look l=look!=null?look:TroopModels.resolve(e);Identifier skin=l==null?null:TroopModels.skin(l);return skin!=null?skin:vanilla.getTexture(e);}
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
