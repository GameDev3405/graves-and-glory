package gg.gravesandglory.testing.mixin;
import gg.gravesandglory.MotionChecks;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Unshipped. mo-fade-smoke (R57): every particle the client asks to spawn (before the particle setting thins it out) is shown to
 *  MotionChecks.particle, which counts the poofs near a watched body. */
@Mixin(WorldRenderer.class)
abstract class ParticleCountMixin {
    @Inject(method="spawnParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",at=@At("HEAD"),require=1)
    private void gravesandgloryTest$countParticle(ParticleEffect effect,boolean force,boolean decreased,double x,double y,double z,double vx,double vy,double vz,CallbackInfoReturnable<Particle> cir){
        MotionChecks.particle(effect,x,y,z);
    }
}
