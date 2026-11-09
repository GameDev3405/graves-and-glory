package gg.gravesandglory;

import com.sun.net.httpserver.HttpServer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server resource pack delivery for the props (prop kit plan task S7). System properties:
 *   gg.propPack.file      the pack zip, or a folder holding gg-props-latest.sha1 (dist/props); the sha1 is computed from it at start and on reload
 *   gg.propPack.url       where players download it (ending in "/": a folder, the pack name is added)
 *   gg.propPack.port      optional: serve the file here with the JDK HTTP server at /gg-props-&lt;sha8&gt;.zip; the url then defaults to it
 *   gg.propPack.required  default true in build mode, false elsewhere; a client that declines a required pack disconnects
 *   gg.packwiz.dir        optional: a packwiz pack folder (tools/builder-server/make_packwiz.py) served read-only at /pack/... on gg.propPack.port,
 *                         so the builder pack updates itself; each request reads the file fresh, so publishing is copying the folder in
 * No gg.propPack.file: nothing is sent. The HTTP server starts only with gg.propPack.port and gg.propPack.file or gg.packwiz.dir, so game servers
 * are unchanged. /prop pack reload (op 2) re-reads the file and
 * resends to everyone online. No registry entries.
 */
public final class PropPack {
    private PropPack(){}

    static final String FILE="gg.propPack.file",URL="gg.propPack.url",PORT="gg.propPack.port",REQUIRED="gg.propPack.required",PACKWIZ="gg.packwiz.dir";
    static final String PROMPT="Graves and Glory props";

    static volatile PropPackFile.Pack pack;
    static HttpServer http;
    static String host;
    static int port;

    static boolean configured(){String f=System.getProperty(FILE);return f!=null&&!f.isBlank();}
    static Path packwiz(){String d=System.getProperty(PACKWIZ);return d==null||d.isBlank()?null:Path.of(d.trim());}
    static void log(String text){System.out.println("[Graves and Glory prop pack] "+text);}

    public static void init(){
        CommandRegistrationCallback.EVENT.register((d,access,env)->d.register(CommandManager.literal("prop").requires(s->s.hasPermissionLevel(2))
            .then(CommandManager.literal("pack").then(CommandManager.literal("reload").executes(c->reload(c.getSource()))))));
        if(!configured()&&packwiz()==null){
            if(System.getProperty(URL)!=null||System.getProperty(PORT)!=null)log(URL+"/"+PORT+" set without "+FILE+"; nothing is sent (the sha1 comes from the file)");
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(PropPack::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(s->stop());
        // In build mode BuildMode.settleJoined sends it once the player is settled in a canvas; elsewhere on join.
        if(configured()&&!BuildMode.active())ServerPlayConnectionEvents.JOIN.register((h,sender,s)->send(h.player));
    }

    static void start(MinecraftServer server){
        host=server.getServerIp();
        try{port=PropPackFile.port(System.getProperty(PORT));}catch(IllegalArgumentException e){log(e.getMessage()+"; no embedded server");port=0;}
        if(configured())load();
        Path wiz=packwiz();
        if(wiz!=null&&!Files.isDirectory(wiz)){log(PACKWIZ+" "+wiz+" is not a folder; the packwiz pack is not served");wiz=null;}
        if(wiz!=null&&port<=0)log(PACKWIZ+" set without "+PORT+"; the packwiz pack is not served");
        if(port>0&&(configured()||wiz!=null)){
            try{http=PropPackFile.serve(port,()->pack,wiz);log("serving on port "+port+(wiz==null?"":", packwiz pack "+wiz.toAbsolutePath()+" at /pack/pack.toml"));}
            catch(IOException e){log("could not serve on port "+port+": "+e);port=0;}
        }
        if(!configured())return;
        String url=url();
        if(url==null)log("no "+URL+" and no embedded server; nothing is sent");
        else log("players get "+url+(required()?" (required)":" (optional)"));
    }

    static void stop(){
        if(http!=null){http.stop(0);http=null;log("embedded server stopped");}
        pack=null;
    }

    /** Re-reads the file; on failure the last good pack stays. */
    static String load(){
        try{
            pack=PropPackFile.read(Path.of(System.getProperty(FILE)));
            log("read "+pack.file()+" ("+pack.bytes().length+" bytes, sha1 "+pack.sha1()+")");
            return null;
        }catch(IOException e){log(e.getMessage());return e.getMessage();}
    }

    static String url(){
        var p=pack;
        return p==null?null:PropPackFile.url(System.getProperty(URL),host,http==null?0:port,p.name());
    }
    static boolean required(){return PropPackFile.required(System.getProperty(REQUIRED),BuildMode.active());}

    static boolean send(ServerPlayerEntity player){
        var p=pack;String url=url();
        if(p==null||url==null)return false;
        player.sendResourcePackUrl(url,p.sha1(),required(),Text.literal(PROMPT));
        return true;
    }

    static int reload(ServerCommandSource source){
        if(!configured()){source.sendError(Text.literal("No prop pack configured: start the server with -D"+FILE+"=<zip or folder>"));return 0;}
        String error=load();
        if(error!=null){source.sendError(Text.literal("Prop pack not reloaded: "+error));return 0;}
        var p=pack;String url=url();
        if(url==null){source.sendError(Text.literal("Prop pack read (sha1 "+p.sha1()+") but there is no "+URL+" or "+PORT+" to send it from"));return 0;}
        int n=0;
        for(var player:source.getServer().getPlayerManager().getPlayerList())if(send(player))n++;
        int sent=n;
        source.sendFeedback(()->Text.literal("Prop pack "+p.name()+" (sha1 "+p.sha1()+") sent to "+sent+" player"+(sent==1?"":"s")),true);
        return Math.max(1,sent);
    }
}
