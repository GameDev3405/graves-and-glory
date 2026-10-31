package gg.gravesandglory.mixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Entity's shared flags (on fire 0, invisible 5, glowing 6): BodyCache reads them in one tracker read. */
@Mixin(Entity.class)
public interface EntityFlagsAccessor {
    @Accessor("FLAGS") static TrackedData<Byte> gravesandglory$flags(){throw new AssertionError();}
}
