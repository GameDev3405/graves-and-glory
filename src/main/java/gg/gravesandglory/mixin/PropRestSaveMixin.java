package gg.gravesandglory.mixin;
import gg.gravesandglory.Props;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.math.AffineTransformation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** A prop part is always written at its rest pose (docs/props/MOVING-PARTS.md): a chunk save, a structure save or a copy of a moving part never
 *  keeps a mid-sway transform. Every prop part's rest pose is the identity (Props.spawnPart writes none), so a part that never moves writes
 *  exactly what it wrote before. */
@Mixin(DisplayEntity.class)
abstract class PropRestSaveMixin {
    @Inject(method="writeCustomDataToNbt",at=@At("TAIL"))
    void gravesandglory$restPose(NbtCompound nbt,CallbackInfo ci){
        if(!((DisplayEntity)(Object)this).getCommandTags().contains(Props.TAG))return;
        AffineTransformation.ANY_CODEC.encodeStart(NbtOps.INSTANCE,AffineTransformation.identity()).result().ifPresent(t->nbt.put(DisplayEntity.TRANSFORMATION_NBT_KEY,t));
    }
}
