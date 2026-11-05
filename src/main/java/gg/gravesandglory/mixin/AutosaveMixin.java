package gg.gravesandglory.mixin;

import gg.gravesandglory.ArenaAutosave;
import gg.gravesandglory.GravesAndGlory;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Skips vanilla's periodic autosave (the only saveAll in MinecraftServer.tick, every 6000 ticks) while a battle runs; see
 * ArenaAutosave. Shutdown, /save-all and BuildMode call saveAll elsewhere and are not redirected.
 */
@Mixin(MinecraftServer.class)
abstract class AutosaveMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;saveAll(ZZZ)Z"))
    private boolean gravesandgloryAutosave(MinecraftServer server,boolean suppressLogs,boolean flush,boolean force){
        var battle=GravesAndGlory.battle;
        if(!ArenaAutosave.allow(battle!=null&&battle.running&&battle.server==server))return false;
        return server.saveAll(suppressLogs,flush,force);
    }
}
