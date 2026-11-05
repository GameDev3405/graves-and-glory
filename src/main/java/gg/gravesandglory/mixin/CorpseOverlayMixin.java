package gg.gravesandglory.mixin;

import gg.gravesandglory.BattleMath;
import gg.gravesandglory.TroopBody;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Task 7: a troop's body lies a while after its fall, and vanilla's overlay would keep it red the whole time (deathTime above 0). For a troop
 * the red hurt tint follows BattleMath.corpseHurtTint instead: as vanilla while it is hurt or falls, none once it lies. Every caller of
 * LivingEntityRenderer.getOverlay takes it, LivingEntityRenderer.render and TroopFeatures' layers alike; other entities keep vanilla's.
 * The client holds troops as vanilla mobs (no ArmyMob), so a troop is told by its name, cached on the entity (TroopBody), and only once it lies.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class CorpseOverlayMixin {
    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true)
    private static void gravesandgloryCorpseOverlay(LivingEntity e, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (e.deathTime > BattleMath.DEATH_FALL_TICKS && ((TroopBody) e).gravesandglory$troop())
            cir.setReturnValue(OverlayTexture.packUv(OverlayTexture.getU(whiteOverlayProgress), OverlayTexture.getV(BattleMath.corpseHurtTint(e.deathTime, e.hurtTime))));
    }
}
