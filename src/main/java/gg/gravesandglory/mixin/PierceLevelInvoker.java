package gg.gravesandglory.mixin;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** 1.21.1 makes setPierceLevel private; the pierce hook in GearCombat calls it through this. */
@Mixin(PersistentProjectileEntity.class)
public interface PierceLevelInvoker {
    @Invoker("setPierceLevel")
    void gravesandglory$setPierceLevel(byte level);
}
