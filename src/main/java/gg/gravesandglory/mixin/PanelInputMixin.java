package gg.gravesandglory.mixin;

import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;

/** Stub (command UI plan Task 4); Task 7 fills it: with the command panel open, left click selects and never swings or uses an item. */
@Mixin(Mouse.class)
abstract class PanelInputMixin {
}
