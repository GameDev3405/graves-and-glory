package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** Resolve a bundled mod's conflicting default without replacing custom bindings. */
final class PackControls {
    static void prepare(MinecraftClient client){
        var menu=GravesAndGloryClient.menu;
        for(var key:client.options.allKeys){
            if(!key.getTranslationKey().equals("iris.keybind.reload")||!key.isDefault()||!menu.isDefault()
                    ||!key.getBoundKeyTranslationKey().equals(menu.getBoundKeyTranslationKey()))continue;
            var replacement=InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_F8);
            for(var other:client.options.allKeys)if(other!=key&&other.getBoundKeyTranslationKey().equals(replacement.getTranslationKey())){
                replacement=InputUtil.UNKNOWN_KEY;break;
            }
            key.setBoundKey(replacement);KeyBinding.updateKeysByCode();client.options.write();
            System.out.println("[Graves and Glory] Resolved Iris reload default conflict; war council remains R.");
        }
    }
}
