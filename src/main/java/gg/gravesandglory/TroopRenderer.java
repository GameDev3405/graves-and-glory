package gg.gravesandglory;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

/**
 * Draws a troop with its own model (TroopModels.resolve, from its custom name and gg_ team), and every other mob of the
 * entity type through the vanilla renderer it was registered over. It never draws vanilla armour, held items or the elytra
 * (unit identity rule 3, Needs Kort 10): only the troop's skin, its outer layer, its team cloth, glow, weapon model and the
 * painted off-hand shield (TroopFeatures). No vanilla pose code reaches a troop; until Task 5b it stands in its rest pose.
 */
public final class TroopRenderer<T extends MobEntity> extends LivingEntityRenderer<T, TroopModel<T>> {
    private final EntityRenderer<? super T> vanilla;
    /** The Look of the troop being drawn, for the features; null between draws. */
    TroopModels.Look look;
    /** Draws through a troop's own model since the client started (the harness reads it). */
    static long troopDraws;

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
        model=m;look=l;
        // Folded arms (illagers' idleArms folded) open only to attack or cast (unit identity rule 3; Task 5b times it).
        m.prepare(l,"folded".equals(m.idleArms())&&!e.isAttacking(),false);
        try{super.render(e,yaw,tickDelta,matrices,consumers,light);troopDraws++;}finally{look=null;}
    }
    @Override public Identifier getTexture(T e){TroopModels.Look l=look!=null?look:TroopModels.resolve(e);Identifier skin=l==null?null:TroopModels.skin(l);return skin!=null?skin:vanilla.getTexture(e);}
    /** Only the body yaw: never the vanilla death tilt, sleep or riptide spin (Task 5b's clips own those). */
    @Override protected void setupTransforms(T e,MatrixStack matrices,float animationProgress,float bodyYaw,float tickDelta){matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-bodyYaw));}
    /** As MobEntityRenderer: a hidden custom name shows only on the mob under the crosshair. */
    @Override protected boolean hasLabel(T e){return super.hasLabel(e)&&(e.shouldRenderName()||e.hasCustomName()&&e==dispatcher.targetedEntity);}
    @Override public boolean shouldRender(T e,Frustum frustum,double x,double y,double z){return vanilla.shouldRender(e,frustum,x,y,z);}
    @Override public Vec3d getPositionOffset(T e,float tickDelta){return vanilla.getPositionOffset(e,tickDelta);}
}
