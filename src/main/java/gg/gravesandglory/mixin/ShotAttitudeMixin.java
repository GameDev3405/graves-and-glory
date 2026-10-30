package gg.gravesandglory.mixin;

import gg.gravesandglory.BattleMath;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A battle shot's arrow or trident points along its flight, nose first, every tick (R25). Projectiles flies them no-clip and without gravity and
 * steers their velocity each tick; vanilla turns a no-clip arrow's yaw backwards (atan2(-vx, -vz)) and eases both angles only a fifth of the way
 * to the new heading each tick, so an arcing shot stood on end. After vanilla's tick, on the server and on the client alike (both flags are synced
 * to the client; Projectiles' command tag is not), the attitude is set straight from the velocity in vanilla's own convention for a flying arrow
 * (BattleMath.arrowYaw and arrowPitch). The previous attitude, set at the start of the tick, is left as it is, so the renderer eases from last
 * tick's true heading to this one's; the yaw is taken the short way round from it, so the easing never spins the arrow.
 */
@Mixin(PersistentProjectileEntity.class)
abstract class ShotAttitudeMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    void gravesandgloryNoseFirst(CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        if (!self.isNoClip() || !self.hasNoGravity()) return;
        var v = self.getVelocity();
        if (v.lengthSquared() < 1e-8) return;
        float yaw = (float) BattleMath.arrowYaw(v.x, v.z);
        self.setYaw(self.prevYaw + MathHelper.wrapDegrees(yaw - self.prevYaw));
        self.setPitch((float) BattleMath.arrowPitch(v.x, v.y, v.z));
    }
}
