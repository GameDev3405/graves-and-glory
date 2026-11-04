package gg.gravesandglory.mixin;

import gg.gravesandglory.BattleMath;
import gg.gravesandglory.Projectiles;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A battle shot's arrow or trident points along its flight, nose first, every tick (R25). Projectiles flies them no-clip and without gravity and
 * steers their velocity each tick; vanilla turns a no-clip arrow's yaw backwards (atan2(-vx, -vz)) and eases both angles only a fifth of the way
 * to the new heading each tick, so an arcing shot stood on end. Only battle shots are touched (Projectiles.isShot: the command tag on the server,
 * on the client the synced no-clip, no-gravity and silent flags that fire sets together); every other arrow keeps vanilla's attitude (N8).
 * <p>
 * After vanilla's tick, on the server and on the client alike, both ends of the tick's attitude are set (N1): the renderer eases from the previous
 * attitude to the current one while it draws the tick's move, so the two are set either side of the move's own heading (its chord, in vanilla's
 * convention for a flying arrow: BattleMath.arrowYaw and arrowPitch), each half the turn from the last tick's chord to this one's away from it. Halfway
 * through every tick the nose points exactly along the move, and one tick's end meets the next one's start within half the change in the turn, so
 * the arrow turns smoothly. The first move turns from the attitude the arrow was spawned with (Projectiles.fire: the curve's launch tangent). A
 * tick that moves less than a tenth of a block keeps the attitude it started with and leaves the last chord be: a landed arrow held where it
 * landed, and the client's one tick of the arrow's spawn nudge (Projectiles.SPAWN_NUDGE), which faces it along the launch tangent. The yaw is
 * taken the short way round, so the easing never spins the arrow.
 */
@Mixin(PersistentProjectileEntity.class)
abstract class ShotAttitudeMixin {
    @Unique private float gravesandgloryChordYaw, gravesandgloryChordPitch;
    @Unique private boolean gravesandgloryHasChord;

    @Inject(method = "tick", at = @At("RETURN"))
    void gravesandgloryNoseFirst(CallbackInfo ci) {
        PersistentProjectileEntity self = (PersistentProjectileEntity) (Object) this;
        if (!Projectiles.isShot(self)) return;
        double dx = self.getX() - self.prevX, dy = self.getY() - self.prevY, dz = self.getZ() - self.prevZ;
        if (dx * dx + dy * dy + dz * dz < 0.01) {self.setYaw(self.prevYaw); self.setPitch(self.prevPitch); return;}
        float yaw = (float) BattleMath.arrowYaw(dx, dz), pitch = (float) BattleMath.arrowPitch(dx, dy, dz);
        float halfYaw, halfPitch;
        if (gravesandgloryHasChord) {halfYaw = MathHelper.wrapDegrees(yaw - gravesandgloryChordYaw) / 2; halfPitch = (pitch - gravesandgloryChordPitch) / 2;}
        else {halfYaw = MathHelper.wrapDegrees(yaw - self.prevYaw); halfPitch = pitch - self.prevPitch;}
        // The start stays near last tick's end: the chord's yaw is unwrapped against the previous attitude.
        float startYaw = self.prevYaw + MathHelper.wrapDegrees(yaw - halfYaw - self.prevYaw);
        self.prevYaw = startYaw; self.prevPitch = pitch - halfPitch;
        self.setYaw(startYaw + 2 * halfYaw); self.setPitch(pitch + halfPitch);
        gravesandgloryChordYaw = yaw; gravesandgloryChordPitch = pitch; gravesandgloryHasChord = true;
    }
}
