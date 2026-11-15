package gg.gravesandglory;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

/**
 * Local test tooling only (never shipped). 1.20.1's CommandManager.executeWithPrefix returned the command's integer result; in 1.21.1 it is void.
 * This runs the same call and captures the result through the source's ReturnValueConsumer, so a check that asserts on a command's result still can.
 * The result is 0 when the command failed, as before. A command is run to completion inside executeWithPrefix, so the value is set when it returns.
 */
final class HarnessCommand {
    private HarnessCommand() {}

    static int run(MinecraftServer server, ServerCommandSource source, String command) {
        int[] result = {0};
        server.getCommandManager().executeWithPrefix(source.withReturnValueConsumer((success, value) -> result[0] = success ? value : 0), command);
        return result[0];
    }
}
