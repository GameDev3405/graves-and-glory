package gg.gravesandglory.mixin;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.command.CommandOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the private output of a command source: AdminCommands treats a source as the console only when its output is the server itself (a command block renamed "Server" is not). Server-safe. */
@Mixin(ServerCommandSource.class)
public interface ServerCommandSourceAccessor {
    @Accessor("output")
    CommandOutput gravesandgloryOutput();
}
