package gg.gravesandglory.testing.mixin;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
/** Unshipped headless experiment pacing. Never active for a normal server or visual client. */
@Mixin(MinecraftServer.class)
public abstract class BalanceTickMixin {
 @ModifyConstant(method="runServer",constant=@Constant(longValue=50L),require=4)
 private long balanceTickMillis(long original){
  if(System.getProperty("gravesandglory.botSim")==null)return original;
  return Math.max(5,Math.min(50,Long.getLong("gravesandglory.simTickMillis",50L)));
 }
}
