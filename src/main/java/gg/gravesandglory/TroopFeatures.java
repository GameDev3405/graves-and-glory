package gg.gravesandglory;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import java.util.List;

/**
 * The layers TroopRenderer draws over a troop's skin: the outer layer, the team cloth, the glow, the weapon model and the
 * painted off-hand shield. Each draws only for the troop TroopRenderer is drawing (its Look is set), and none for an
 * invisible troop; the weapon and the shield also wait while the arms are folded.
 */
final class TroopFeatures {
    private TroopFeatures() {}
    static final int FULL_BRIGHT=0xF00000;

    /** Whether every part of the path is shown (a hidden arm hides what its hand holds). */
    static boolean shown(List<ModelPart> path){if(path.isEmpty())return false;for(ModelPart p:path)if(!p.visible)return false;return true;}

    /** <troop>_o on the rig's boxes dilated by the rig's outer (drowned and strays), posed as the model is. */
    static final class Outer<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Outer(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            TroopModel<T> model=getContextModel();if(r.look==null||e.isInvisible()||model.outerTop==null||model.outer==null)return;
            for(var entry:model.outerParts().entrySet()){ModelPart own=model.bone(entry.getKey());entry.getValue().copyTransform(own);entry.getValue().visible=own.visible;}
            model.outerTop.render(m,v.getBuffer(RenderLayer.getEntityCutoutNoCull(model.outer)),light,LivingEntityRenderer.getOverlay(e,0f));
        }
    }

    /**
     * The team bones in the cloth of the troop's side (team_cloth_<side>), of its dress when it has no side (a portrait wears
     * the company's), or linen; the pennant and standard draw only for a troop on a side whose row flies one.
     */
    static final class Cloth<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Cloth(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            var l=r.look;if(l==null||e.isInvisible())return;
            Identifier cloth=TroopModels.cloth(TroopModels.clothOf(e,l));
            getContextModel().renderCloth(m,v.getBuffer(RenderLayer.getEntityCutout(cloth)),light,LivingEntityRenderer.getOverlay(e,0f));
        }
    }

    /** <troop>_e at full brightness from the troop's glowFrom stage (RenderLayer.getEyes), on the skin bones only. */
    static final class Glow<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Glow(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            var l=r.look;if(l==null||e.isInvisible())return;Identifier glow=TroopModels.glow(l);if(glow==null)return;
            getContextModel().render(m,v.getBuffer(RenderLayer.getEyes(glow)),FULL_BRIGHT,OverlayTexture.DEFAULT_UV,1f,1f,1f,1f);
        }
    }

    /** The troop's own weapon model at its hand bone, and the weapon's glow. */
    static final class Weapon<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Weapon(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            TroopModel<T> model=getContextModel();if(r.look==null||e.isInvisible()||model.folded)return;
            var w=TroopModels.weapon(model.weaponId());List<ModelPart> path=model.handPath(model.hand());if(w==null||!shown(path))return;
            m.push();for(ModelPart p:path)p.rotate(m);
            w.part().render(m,v.getBuffer(RenderLayer.getEntityCutout(w.texture())),light,LivingEntityRenderer.getOverlay(e,0f));
            if(w.glow()!=null)w.part().render(m,v.getBuffer(RenderLayer.getEyes(w.glow())),FULL_BRIGHT,OverlayTexture.DEFAULT_UV);
            m.pop();
        }
    }

    /**
     * The off-hand shield, in the hand opposite the weapon, through HeldItemRenderer so its painted patterns and the vanilla
     * blocking model show; held as vanilla's HeldItemFeatureRenderer holds it (2 px down and 2 px forward of the hand bone).
     */
    static final class Shield<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;final HeldItemRenderer held;Shield(TroopRenderer<T> r,HeldItemRenderer held){super(r);this.r=r;this.held=held;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            TroopModel<T> model=getContextModel();if(r.look==null||e.isInvisible()||model.folded)return;
            ItemStack stack=e.getOffHandStack();if(stack.isEmpty())return;
            boolean left=!"left".equals(model.hand());List<ModelPart> path=model.handPath(left?"left":"right");if(!shown(path))return;
            m.push();for(ModelPart p:path)p.rotate(m);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));m.translate(0f,0.125f,-0.125f);
            held.renderItem(e,stack,left?ModelTransformationMode.THIRD_PERSON_LEFT_HAND:ModelTransformationMode.THIRD_PERSON_RIGHT_HAND,left,m,v,light);
            m.pop();
        }
    }
}
