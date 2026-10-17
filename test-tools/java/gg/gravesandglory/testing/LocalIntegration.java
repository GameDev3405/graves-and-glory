package gg.gravesandglory.testing;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

/** Separate, unshipped developer harness. Configures the standard local-server
 * online-mode setting for disposable Fabric development identities, which do
 * not have Microsoft sessions. Production clients and releases never load it. */
public final class LocalIntegration implements ModInitializer {
    @Override public void onInitialize(){
        if(!FabricLoader.getInstance().isDevelopmentEnvironment()||!Boolean.getBoolean("gravesandglory.localIntegration"))throw new IllegalStateException("Local test tools must never run in a player installation");
        ServerLifecycleEvents.SERVER_STARTED.register(server->{server.setOnlineMode(false);System.out.println("[Graves and Glory local integration] Standard offline server mode enabled for local development profiles only.");});
    }
}
