package gg.gravesandglory.polymer;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** gravesandglory.polymer.mixins.json lists no mixins itself: this plugin names them only when Polymer is loaded, so a JVM without
 *  Polymer never reads a class that implements a Polymer interface. */
public final class PolymerMixins implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() {
        if (!FabricLoader.getInstance().isModLoaded("polymer-core")) return List.of();
        return List.of("ArmyMobPolymerMixin", "WagonPolymerMixin", "MapBlockPolymerMixin", "MapBlockItemPolymerMixin");
    }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
