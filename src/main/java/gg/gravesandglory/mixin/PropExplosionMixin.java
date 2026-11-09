package gg.gravesandglory.mixin;

import gg.gravesandglory.PropGameplay;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A vanilla explosion is siege fire on the destructible props within its power (PropGameplay.explosion): barriers never break in one. */
@Mixin(Explosion.class)
abstract class PropExplosionMixin {
    @Shadow @Final private World world;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private float power;
    @Inject(method="collectBlocksAndDamageEntities",at=@At("TAIL"))
    void gravesandgloryProps(CallbackInfo ci){
        if(world instanceof ServerWorld w)PropGameplay.explosion(w,new Vec3d(x,y,z),power);
    }
}
