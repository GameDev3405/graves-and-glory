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
import org.joml.Quaternionf;
import java.util.List;

/**
 * The layers TroopRenderer draws over a troop's skin: the outer layer, the veteran trim, the team cloth, the glow, the weapon model and the
 * painted off-hand shield. Each draws only for the troop TroopRenderer is drawing (its Look is set), and none for an
 * invisible troop; the weapon and the shield also wait while the arms are folded. Each reports to TroopRenderer what it
 * bound and drew (the harness reads the draws as observed, never re-derived).
 */
final class TroopFeatures {
    private TroopFeatures() {}
    static final int FULL_BRIGHT=0xF00000;

    /** Harness only: the weapon ids whose models are not drawn (the turnaround's bare shots). */
    static final java.util.Set<String> HIDDEN=java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Harness only: the weapon glow passes drawn since the client started. */
    static volatile long weaponGlowDraws;
    /** Whether every part of the path is shown (a hidden arm hides what its hand holds). */
    static boolean shown(List<ModelPart> path){if(path.isEmpty())return false;for(ModelPart p:path)if(!p.visible)return false;return true;}

    /** <troop>_o on the rig's boxes dilated by the rig's outer (drowned and strays), posed as the model is. */
    static final class Outer<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Outer(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            TroopModel<T> model=getContextModel();if(r.look==null||e.isInvisible()||model.outerTop==null||model.outer==null)return;
            for(var entry:model.outerParts().entrySet()){ModelPart own=model.bone(entry.getKey());entry.getValue().copyTransform(own);entry.getValue().visible=own.visible;}
            model.outerTop.render(m,v.getBuffer(RenderLayer.getEntityCutoutNoCull(r.far&&model.density>1?TroopModels.twin(model.outer):model.outer)),light,LivingEntityRenderer.getOverlay(e,0f));
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
            Identifier cloth=TroopModels.cloth(getContextModel().cloth,TroopModels.clothOf(e,l));
            long drawn=getContextModel().renderCloth(m,v.getBuffer(RenderLayer.getEntityCutout(cloth)),light,LivingEntityRenderer.getOverlay(e,0f));
            r.clothSheet=cloth;r.clothMask=drawn;
        }
    }

    /** The veteran trim (<troop>_veteran) over the worn armour tier, lit as the skin is, on the skin bones only (in-match progression design 4.4). */
    static final class Trim<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Trim(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            var l=r.look;if(l==null||e.isInvisible())return;Identifier trim=TroopModels.trim(l,r.far);if(trim==null)return;
            getContextModel().render(m,v.getBuffer(RenderLayer.getEntityCutoutNoCull(trim)),light,LivingEntityRenderer.getOverlay(e,0f),1f,1f,1f,1f);
            r.trimSheet=trim;
        }
    }

    /** <troop>_e at full brightness from the troop's glowFrom stage (RenderLayer.getEyes), on the skin bones only. */
    static final class Glow<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Glow(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            var l=r.look;if(l==null||e.isInvisible())return;Identifier glow=TroopModels.glow(l,r.far);if(glow==null)return;
            getContextModel().render(m,v.getBuffer(RenderLayer.getEyes(glow)),FULL_BRIGHT,OverlayTexture.DEFAULT_UV,1f,1f,1f,1f);
        }
    }

    /** The troop's own weapon model at its hand bone, at the weapon tier it wears (<weapon>_t<n>; a tier without a model draws the highest tier below it), and the weapon's glow. */
    static final class Weapon<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;Weapon(TroopRenderer<T> r){super(r);this.r=r;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            TroopModel<T> model=getContextModel();if(r.look==null||e.isInvisible()||model.folded)return;
            var w=TroopModels.weapon(model.weaponId(),r.look.weaponTier());List<ModelPart> path=model.handPath(model.hand());if(w==null||!shown(path)||HIDDEN.contains(w.id())||HIDDEN.contains(model.weaponId()))return;
            // a draw weapon shows the pull frame its bow_draw time is at (the rest frame when nothing draws). A cube weapon draws whole within
            // TroopDepth.weaponNear and its far version (at most 32 quads) outside it, depth off included; a sprite weapon draws whole, or with troop depth off only its two big faces
            int frame=w.pull().length==0?0:TroopModels.Weapon.frameAt(w.pull(),TroopAnimator.drawSeconds(e,model,age),TroopClips.BOW_TIMES.getOrDefault(model.rig,TroopClips.BOW_TIMES.get("biped"))[1]);
            boolean far=w.cube()&&!TroopDepth.weaponNear(e);
            var mesh=far?w.far():w.frames().get(Math.min(frame,w.frames().size()-1));int limit=w.cube()||TroopDepth.enabled?Integer.MAX_VALUE:2;
            m.push();for(ModelPart p:path)p.rotate(m);w.grip(m);
            int quads=mesh.render(m.peek(),v.getBuffer(RenderLayer.getEntityCutout(w.texture())),light,LivingEntityRenderer.getOverlay(e,0f),1f,1f,1f,1f,limit);
            // the glow pass draws the same mesh again through the eyes layer: weaponQuads counts the mesh once, weaponGlowDraws the extra passes
            if(w.glow()!=null){mesh.render(m.peek(),v.getBuffer(RenderLayer.getEyes(w.glow())),FULL_BRIGHT,OverlayTexture.DEFAULT_UV,1f,1f,1f,1f,limit);weaponGlowDraws++;}
            m.pop();r.weaponDrawn=true;r.weaponId=w.id();r.weaponFrame=frame;r.weaponFar=far;r.weaponQuads=quads;
        }
    }

    /**
     * The off-hand shield, in the hand opposite the weapon, through HeldItemRenderer so its painted patterns show. It is
     * carried face out at all times: from the hand frame vanilla's HeldItemFeatureRenderer uses (2 px down and 2 px forward of
     * the hand bone), the vanilla shield_blocking model's third-person left-hand transform (mirrored for a shield in the right
     * hand), drawn with no display transform of its own. So the face covers the troop's front at rest (the shield arm carries
     * it 45 degrees forward) and on guard, never edge-on as vanilla's lowered hold reads from the side. A model with its own
     * shield (TroopModel.ownShield) draws that on its own arm bone with its skin and cloth, and the item draws nothing.
     */
    static final class Shield<T extends MobEntity> extends FeatureRenderer<T,TroopModel<T>> {
        final TroopRenderer<T> r;final HeldItemRenderer held;Shield(TroopRenderer<T> r,HeldItemRenderer held){super(r);this.r=r;this.held=held;}
        @Override public void render(MatrixStack m,VertexConsumerProvider v,int light,T e,float limbAngle,float limbDistance,float tickDelta,float age,float headYaw,float headPitch){
            TroopModel<T> model=getContextModel();if(r.look==null||e.isInvisible()||model.folded)return;
            ItemStack stack=e.getOffHandStack();if(stack.isEmpty())return;
            boolean left=!"left".equals(model.hand());List<ModelPart> path=model.handPath(left?"left":"right");if(!shown(path))return;
            if(model.ownShield()){r.shieldDrawn=true;return;}
            m.push();for(ModelPart p:path)p.rotate(m);
            m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));m.translate(0f,0.125f,-0.125f);
            float side=left?-1:1;m.translate(side*13.51f/16f,3f/16f,5f/16f);
            m.multiply(new Quaternionf().rotationXYZ(45*TroopModel.RAD,side*135*TroopModel.RAD,0));
            held.renderItem(e,stack,ModelTransformationMode.NONE,left,m,v,light);
            m.pop();r.shieldDrawn=true;
        }
    }
}
