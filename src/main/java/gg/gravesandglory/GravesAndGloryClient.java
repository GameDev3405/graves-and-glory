package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.*;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.*;
import net.minecraft.world.gen.*;
import net.minecraft.world.level.LevelInfo;
import org.lwjgl.glfw.GLFW;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class GravesAndGloryClient implements ClientModInitializer {
    static boolean ready,running,nearBarracks;
    static int dawn,dusk,team=-1,count,respawn,gold,queued,trainingSeconds;
    static String message="Preparing battlefield",order="follow",formation="block";
    /** state_v8: the side id each team flies, this army's House trait tiers (tier I or higher), and the commander ability (its active id, ticks until ready or -1 when this build has no behaviour for it, ticks left active; "" while unseated). */
    static String[] sideIds={"",""};static Map<String,Integer> houses=Map.of();static String commanderActive="";static int commanderReady,commanderFor;
    static boolean[] contested={false,false,false};
    record Sight(float x,float z,int age){}
    static List<Sight> sightings=List.of();
    static int[] owners={-1,-1,-1},progress={0,0,0},challengers={-1,-1,-1},roster=new int[Spec.TROOPS.length],pending=new int[Spec.TROOPS.length];
    static List<Marker> markers=List.of();
    record Marker(float x,float z,boolean fighting,boolean own,String name){}
    static KeyBinding menu,attack,follow,cycle,mapKey,rotateLeft,rotateRight,confirmOrder,cancelOrder,ability;
    static Path runtime;
    static String join="",error="";
    static boolean attempted,opened,joined,shareCamp,launcherContext,connectingInvitation;
    static int titleTicks,worldTicks;
    static Identifier mapTexture;
    static boolean preparingMap;
    static final int GOLD=0xffc8a56b,INK=0xf019171d,MUTED=0xffafb9bc,WHITE=0xffeee9df;
    @Override public void onInitializeClient(){
        ClientLoginNetworking.registerGlobalReceiver(VersionGate.CHANNEL,(client,handler,buffer,listeners)->CompletableFuture.completedFuture(VersionGate.response()));
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(new Identifier("gravesandglory","armory"),net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("gravesandglory").orElseThrow(),net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED);
        try { LegacyDataMigration.instance(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir(), Diagnostics.active()); }
        catch (java.io.IOException migration) { error="Could not migrate old collection/settings. Your originals are unchanged. Check storage and permissions."; }
        Presentation.load();
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.IMPACT,(client,handler,buf,responder)->{float strength=buf.readFloat();client.execute(()->{Presentation.receivedImpacts++;Presentation.impact(strength);});});
        // The troops' own models (Phase 2 Task 5): one renderer per sheet entity type, the models reloaded with the resources.
        TroopRenderers.register();
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new TroopModelLoader());
        menu=key("menu",GLFW.GLFW_KEY_R);attack=key("attack",GLFW.GLFW_KEY_G);follow=key("follow",GLFW.GLFW_KEY_V);cycle=key("formation",GLFW.GLFW_KEY_B);mapKey=key("map",GLFW.GLFW_KEY_M);
        rotateLeft=key("rotate_left",GLFW.GLFW_KEY_LEFT_BRACKET);rotateRight=key("rotate_right",GLFW.GLFW_KEY_RIGHT_BRACKET);
        ability=key("ability",GLFW.GLFW_KEY_H); // the commander ability (Commander.activate); G is the attack order
        confirmOrder=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.confirm",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_ENTER,"category.gravesandglory"));
        cancelOrder=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.cancel",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_BACKSPACE,"category.gravesandglory"));
        String local=System.getenv("LOCALAPPDATA");runtime=Path.of(System.getProperty("gravesandglory.runtime",local==null?"gravesandglory-runtime":local+"/gravesandglory"));
        // Optional launcher integration. Ordinary installs need no runtime directory.
        launcherContext=Files.isRegularFile(runtime.resolve("melty.properties"));
        if(launcherContext)try{Files.writeString(runtime.resolve("melty.log"),"");Properties props=new Properties();try(var in=Files.newInputStream(runtime.resolve("melty.properties"))){props.load(in);}join=props.getProperty("join","").trim();}catch(Exception e){System.err.println("[Graves and Glory] Optional launcher invitation could not be read: "+e.getMessage());}
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.CAMP,(client,handler,buf,responder)->{String page=buf.readString(32);client.execute(()->CampScreen.open(page));});
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.OPEN_BARRACKS,(client,handler,buf,responder)->client.execute(()->client.setScreen(running?new BarracksScreen():new LobbyScreen())));
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.PARTY,(client,handler,buf,responder)->client.execute(()->client.setScreen(new LobbyScreen(3))));
        // A troop's stance (stance_v1): TroopAnimator picks its still and move clips by it. The packet a player gets on START_TRACKING comes before
        // the entity's spawn packet (Fabric calls the event first), so the entry is kept for an id the world does not have yet; an unloaded entity's entry goes.
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.STANCE,(client,handler,buf,responder)->{int id=buf.readVarInt(),i=buf.readByte();client.execute(()->{
            String stance=i>=0&&i<Tactics.STANCES.size()?Tactics.STANCES.get(i):"";
            if(stance.isEmpty())TroopAnimator.STANCES.remove(id);else TroopAnimator.STANCES.put(id,stance);});});
        ClientEntityEvents.ENTITY_UNLOAD.register((entity,world)->TroopAnimator.forget(entity.getId()));
        ClientPlayNetworking.registerGlobalReceiver(GravesAndGlory.STATE,(client,handler,buf,responder)->{
            boolean r=buf.readBoolean(),run=buf.readBoolean();int a=buf.readInt(),b=buf.readInt();String msg=buf.readString();int t=buf.readInt(),n=buf.readInt();String o=buf.readString(),f=buf.readString();int resp=buf.readInt();
            int points=buf.readInt();if(points<1||points>8)return;boolean[] contest=new boolean[points];int[] own=new int[points],pr=new int[points],ch=new int[points];for(int i=0;i<points;i++){buf.readString();own[i]=buf.readInt();pr[i]=buf.readInt();ch[i]=buf.readInt();contest[i]=buf.readBoolean();}
            int g=buf.readInt();boolean near=buf.readBoolean();int q=buf.readInt(),eta=buf.readInt(),types=buf.readInt();if(types!=Spec.TROOPS.length)return;
            int[] owned=new int[types],waiting=new int[types];for(int i=0;i<types;i++){owned[i]=buf.readInt();waiting[i]=buf.readInt();}
            int amount=buf.readInt();if(amount<0||amount>160)return;List<Marker> friendly=new ArrayList<>();for(int i=0;i<amount;i++)friendly.add(new Marker(buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean(),buf.readString()));
            int groupCount=buf.readInt();if(groupCount!=3)return;var groups=new ArrayList<CommandMode.Group>();
            for(int i=0;i<groupCount;i++)groups.add(new CommandMode.Group(buf.readString(),buf.readInt(),buf.readFloat(),buf.readString(),buf.readString(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean()));
            int pingCount=buf.readInt();if(pingCount<0||pingCount>4)return;var pings=new ArrayList<CommandMode.Ping>();for(int i=0;i<pingCount;i++)pings.add(new CommandMode.Ping(buf.readString(),buf.readFloat(),buf.readFloat()));int seconds=buf.readInt();
            int seen=buf.readInt();if(seen<0||seen>160)return;var sight=new ArrayList<Sight>();for(int i=0;i<seen;i++)sight.add(new Sight(buf.readFloat(),buf.readFloat(),buf.readInt()));
            String campaignJson=buf.readString(Campaign.SNAPSHOT_LIMIT);
            String sideA=buf.readString(64),sideB=buf.readString(64);int tierCount=buf.readByte();if(tierCount<0||tierCount>Spec.HOUSES.length)return;Map<String,Integer> tiers=new LinkedHashMap<>();for(int i=0;i<tierCount;i++){String house=buf.readString(64);tiers.put(house,(int)buf.readByte());}
            String ability=buf.readString(64);int abilityReady=buf.readInt(),abilityFor=buf.readInt();
            client.execute(()->{sideIds=new String[]{sideA,sideB};houses=Collections.unmodifiableMap(tiers);commanderActive=ability;commanderReady=abilityReady;commanderFor=abilityFor;CampaignClient.accept(client,campaignJson,run);sightings=List.copyOf(sight);contested=contest;CommandMode.pings=java.util.List.copyOf(pings);CommandMode.seconds=seconds;CommandMode.noteGroups(CommandMode.groups,groups,net.minecraft.util.Util.getMeasuringTimeMs());CommandMode.groups=java.util.List.copyOf(groups);ready=r;running=run;dawn=a;dusk=b;message=msg;team=t;count=n;order=o;formation=f;respawn=resp;owners=own;progress=pr;challengers=ch;gold=g;nearBarracks=near;queued=q;trainingSeconds=eta;roster=owned;pending=waiting;markers=List.copyOf(friendly);});
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{CampaignClient.sendProfile(client);if(connectingInvitation&&!joined){joined=true;say("Melty joined: "+join);}});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{if(connectingInvitation&&!joined)say("Melty join failed: connection closed before joining");CampaignClient.end(client);TroopAnimator.clear();ready=false;running=false;team=-1;houses=Map.of();opened=false;worldTicks=0;shareCamp=false;connectingInvitation=false;});
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.getOverlay()==null&&client.currentScreen instanceof TitleScreen){
                if(!attempted){PackControls.prepare(client);if(!Presentation.prepareResources(client))return;attempted=true;}
                client.setScreen(error.isEmpty()?new FrontMenuScreen():new FailureScreen(error));
            }
            if(client.player!=null){
                worldTicks++;if(ready&&worldTicks%20==0)client.getToastManager().clear();prepareMap(client);CampaignClient.tick(client);Diagnostics.tick(client);
                while(menu.wasPressed()){CampaignClient.end(client);CommandMode.cancel();client.setScreen(battleActive()?new CommandScreen():new CampScreen("home"));}
                while(ability.wasPressed())if(battleActive())send("ability");
                while(attack.wasPressed())CommandMode.begin("attack");while(follow.wasPressed())CommandMode.issue("all","follow",CommandMode.formation,client.player.getPos(),client.player.getYaw(),-1);
                while(rotateLeft.wasPressed()){CommandMode.facingSet=true;CommandMode.facing-=Spec.COMMANDINTERFACE[0].facingStep();}
                while(rotateRight.wasPressed()){CommandMode.facingSet=true;CommandMode.facing+=Spec.COMMANDINTERFACE[0].facingStep();}
                CommandMode.tick(client);
                while(confirmOrder.wasPressed())if(CommandMode.active())CommandMode.confirm();
                while(cancelOrder.wasPressed())CommandMode.cancel();while(mapKey.wasPressed())if(battleActive())client.setScreen(new MapScreen(null));
                while(cycle.wasPressed()){int i=0;for(int k=0;k<Spec.FORMATIONS.length;k++)if(Spec.FORMATIONS[k].id().equals(CommandMode.formation))i=k;CommandMode.changeFormation(Spec.FORMATIONS[(i+1)%Spec.FORMATIONS.length].id());}
                if(ready&&!opened&&shareCamp&&client.getServer()!=null){opened=true;try{int port=0;String requested=System.getenv("GRAVESANDGLORY_HOST_PORT");if(requested==null)requested=System.getenv("BANNERBOUND_HOST_PORT");if(requested!=null){port=Integer.parseInt(requested);if(port<1024||port>65535)throw new IllegalArgumentException("Invalid host port");}if(port==0)try(ServerSocket socket=new ServerSocket(0)){port=socket.getLocalPort();}if(client.getServer().openToLan(GameMode.ADVENTURE,false,port)){String address=InetAddress.getLocalHost().getHostAddress()+":"+port;say("Melty join address: "+address);client.player.sendMessage(Text.literal("Camp open on your local network: "+address),false);}else client.player.sendMessage(Text.literal("Could not open camp to the local network."),false);}catch(Exception e){client.player.sendMessage(Text.literal("Could not share camp: "+e.getMessage()),false);}}
            }
        });
        HudRenderCallback.EVENT.register(GravesAndGloryClient::renderHud);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(CommandMode::renderWorld);
        // Troop depth: the frame's cutoff from the game camera, once, before any pass draws (Iris's shadow pass among them).
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(ctx->TroopDepth.beginFrame(ctx.camera().getPos(),ctx.world().getEntities()));
    }
    static KeyBinding key(String name,int key){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory."+name,InputUtil.Type.KEYSYM,key,"category.gravesandglory"));}
    /** The player's own key for an action, as a hint names it: key.attack for hit, key.use for use, key.gravesandglory.(id) for the mod's own
     *  (menu, attack, follow, formation, map, rotate_left, rotate_right, confirm, cancel, ability); "Not bound" when it is unbound or unknown. */
    public static String hint(String id){
        String name=switch(id){case "hit"->"key.attack";case "use"->"key.use";default->"key.gravesandglory."+id;};
        for(KeyBinding k:MinecraftClient.getInstance().options.allKeys)if(k.getTranslationKey().equals(name))return k.isUnbound()?"Not bound":k.getBoundKeyLocalizedText().getString();
        return "Not bound";
    }
    static void say(String line){try{Files.writeString(runtime.resolve("melty.log"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}
    /** The last action send passed on (the harness reads what a screen sent). */
    static volatile String lastSent="";
    static void send(String action){if(ClientPlayNetworking.canSend(GravesAndGlory.ORDER)){lastSent=action;PacketByteBuf b=PacketByteBufs.create();b.writeString(action);ClientPlayNetworking.send(GravesAndGlory.ORDER,b);}}
    static void connectOrHost(MinecraftClient c){
        if(!join.isEmpty()){connectingInvitation=true;ConnectScreen.connect(new FrontMenuScreen("friends"),c,ServerAddress.parse(join),new ServerInfo("Graves and Glory",join,false),false);return;}
        try { LegacyDataMigration.world(c.runDirectory.toPath().resolve("saves")); }
        catch (java.io.IOException migration) { c.setScreen(new FailureScreen("Could not upgrade the Bannerbound world. Originals were kept. "+migration.getMessage()));return; }
        if(Files.exists(c.runDirectory.toPath().resolve("saves/Graves and Glory/level.dat"))){c.createIntegratedServerLoader().start(new TitleScreen(),"Graves and Glory");return;}
        var info=new LevelInfo("Graves and Glory",GameMode.ADVENTURE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE);
        c.createIntegratedServerLoader().createAndStart("Graves and Glory",info,new GeneratorOptions(728619L,false,false),r->r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder());
    }
    static void startCamp(MinecraftClient c,boolean friends){
        // A saved launcher invitation never redirects an explicit local-play choice.
        String invitation=join;join="";shareCamp=friends;opened=false;joined=false;connectingInvitation=false;
        try{connectOrHost(c);}finally{join=invitation;}
    }
    static void invalidateMap(MinecraftClient c){if(mapTexture!=null){c.getTextureManager().destroyTexture(mapTexture);mapTexture=null;}preparingMap=false;}
    static void prepareMap(MinecraftClient c){
        if(preparingMap)return;preparingMap=true;final int generation=CampaignMaps.selected;
        CompletableFuture.supplyAsync(()->{NativeImage image=new NativeImage(281,201,false);for(int x=0;x<281;x++)for(int z=0;z<201;z++){int col=Arena.mapColor((int)((x-140)*2.5),(int)((z-100)*2.5));image.setColor(x,z,(col&0xff00ff00)|((col>>16)&255)|((col&255)<<16));}return image;}).thenAccept(image->c.execute(()->{if(generation!=CampaignMaps.selected){image.close();preparingMap=false;return;}mapTexture=c.getTextureManager().registerDynamicTexture("gravesandglory_map",new NativeImageBackedTexture(image));}));
    }
    static int factionColor(int t){return DarkUi.teamColor(t);}
    // The point arrays refill with the next state packet after a map switch, so until then they can be shorter or longer than Spec.POINTS.
    static boolean contestedAt(int i){var a=contested;return i>=0&&i<a.length&&a[i];}
    static int ownerAt(int i){var a=owners;return i>=0&&i<a.length?a[i]:-1;}
    static int progressAt(int i){var a=progress;return i>=0&&i<a.length?a[i]:0;}
    static int challengerAt(int i){var a=challengers;return i>=0&&i<a.length?a[i]:-1;}
    static void panel(DrawContext g,int x,int y,int w,int h){DarkUi.frame(g,x,y,w,h);}
    static void map(DrawContext g,int x,int y,int w,int h,boolean labels){
        if(mapTexture!=null)g.drawTexture(mapTexture,x,y,w,h,0f,0f,281,201,281,201);else g.fill(x,y,x+w,y+h,0xff34473b);
        g.fill(x,y,x+w,y+h,0x66303c39);
        var pts=Spec.POINTS;for(int i=0;i<pts.length;i++){var p=pts[i];int px=x+(p.x()+350)*w/700,pz=y+(p.z()+250)*h/500;g.fill(px-4,pz-4,px+5,pz+5,0xcc101722);g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,p.id(),px-2,pz-3,factionColor(ownerAt(i)));if(contestedAt(i))g.drawBorder(px-6,pz-6,13,13,0xffffaa55);if(progressAt(i)>0)g.fill(px-5,pz+5,px-5+10*progressAt(i)/p.captureSeconds(),pz+7,factionColor(challengerAt(i)));if(labels)g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,p.name(),px-22,pz+8,WHITE);}
        for(Marker m:markers){int px=x+(int)((m.x()+350)*w/700),pz=y+(int)((m.z()+250)*h/500);int color=m.fighting()?0xffff995d:m.own()?0xfff5e2a8:0xff6ddfe9;g.fill(px-1,pz-1,px+2,pz+2,color);if(m.fighting())g.fill(px-3,pz-3,px+4,pz-2,color);}
        for(var group:CommandMode.groups){int gx=x+(int)((group.cx()+350)*w/700),gz=y+(int)((group.cz()+250)*h/500);boolean selected=CommandMode.selected.equals("all")||CommandMode.selected.equals(group.id());
            if(selected){int dx=x+(int)((group.x()+350)*w/700),dz=y+(int)((group.z()+250)*h/500);for(int k=0;k<=20;k++){int lx=gx+(dx-gx)*k/20,lz=gz+(dz-gz)*k/20;g.fill(lx,lz,lx+1,lz+1,GOLD);}g.drawBorder(dx-3,dz-3,7,7,GOLD);}
            g.fill(gx-4,gz-4,gx+5,gz+5,0xff111923);g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,group.id().substring(0,1).toUpperCase(),gx-2,gz-3,group.fighting()?0xffff9955:GOLD);
        }
        for(var sight:sightings){int px=x+(int)((sight.x()+350)*w/700),pz=y+(int)((sight.z()+250)*h/500);if(sight.age()<20)g.fill(px-2,pz-2,px+3,pz+3,0xffff6860);else g.drawBorder(px-2,pz-2,5,5,0xffa2a5aa);}
        for(var ping:CommandMode.pings){int px=x+(int)((ping.x()+350)*w/700),pz=y+(int)((ping.z()+250)*h/500);g.drawBorder(px-6,pz-6,13,13,0xffffcc55);if(labels)g.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,ping.kind().toUpperCase(),px+8,pz,0xffffcc55);}
        var p=MinecraftClient.getInstance().player;if(p!=null){int px=x+(int)((p.getX()+350)*w/700),pz=y+(int)((p.getZ()+250)*h/500);g.fill(px-1,pz-2,px+2,pz+3,0xffffffff);}
    }
    static String keyHints(){return "Attack "+attack.getBoundKeyLocalizedText().getString()+"   Follow "+follow.getBoundKeyLocalizedText().getString()+"   Formation "+cycle.getBoundKeyLocalizedText().getString()+"   Map "+mapKey.getBoundKeyLocalizedText().getString()+"   Menu "+menu.getBoundKeyLocalizedText().getString();}
    /** Lesson text fits this width inside the 360-pixel lesson panel. */
    static final int LESSON_TEXT_WIDTH=350;
    /** The lesson text lines drawn in the last frame, so a check can compare them with the full copy. */
    static List<String> lessonDrawn=List.of();
    /** The copy of lesson step (0 to 3), naming the player's own key binding. */
    static String lessonText(int step){
        return switch(step){
            case 0->"Press "+menu.getBoundKeyLocalizedText().getString()+", choose a group and Move, then click the ground to send them.";
            case 1->"Press "+cycle.getBoundKeyLocalizedText().getString()+" to change formation. Shieldwall holds a line; Wedge breaks one.";
            case 2->"Press "+attack.getBoundKeyLocalizedText().getString()+" and aim at an enemy or a point to send your army in.";
            default->"Press "+MinecraftClient.getInstance().options.useKey.getBoundKeyLocalizedText().getString()+" at your barracks lectern to recruit. Trained troops march out from the held point nearest the fight.";
        };
    }
    /** The Long March lessons (battles whose sheet row teaches), one at a time, each naming the player's own key binding. The server advances them. The copy wraps; the panel grows a row per line. */
    static void lesson(DrawContext g,net.minecraft.client.font.TextRenderer font,int w){
        int step=CampaignClient.tutorial;if(step<0||step>3)return;
        List<String> lines=font.getTextHandler().wrapLines(lessonText(step),LESSON_TEXT_WIDTH,net.minecraft.text.Style.EMPTY).stream().map(net.minecraft.text.StringVisitable::getString).toList();
        panel(g,w/2-180,60,360,21+11*lines.size());
        g.drawCenteredTextWithShadow(font,"THE LONG MARCH / BATTLE "+CampaignClient.battleOrder()+" / LESSON "+(step+1)+" OF 4",w/2,65,GOLD);
        for(int i=0;i<lines.size();i++)g.drawCenteredTextWithShadow(font,lines.get(i),w/2,78+11*i,WHITE);
        lessonDrawn=lines;
    }
    public static boolean inCamp(){return ready&&!battleActive();}
    public static boolean battleActive(){return ready&&running&&!CampaignClient.waiting;}
    static void campHud(DrawContext g,int w,int h){
        var c=MinecraftClient.getInstance();if(c.currentScreen!=null)return;
        DarkUi.frame(g,12,12,182,40);g.drawText(c.textRenderer,"BANNER CAMP",22,21,DarkUi.GOLD,false);
        g.drawText(c.textRenderer,CampaignClient.profile.gold+" gold / "+CampaignClient.profile.progression.title,22,36,DarkUi.PAPER,false);
        String hint=menu.getBoundKeyLocalizedText().getString()+" / Camp guide    Right-click a station to interact";
        g.drawCenteredTextWithShadow(c.textRenderer,hint,w/2,h-45,DarkUi.PAPER);
        if(CampaignClient.padCountdown>0)g.drawCenteredTextWithShadow(c.textRenderer,"War gates: "+CampaignClient.padCountdown+"s / Step off to cancel",w/2,24,DarkUi.GOLD);
        else if(CampaignClient.waiting)g.drawCenteredTextWithShadow(c.textRenderer,"A battle is underway / Your camp remains open",w/2,24,DarkUi.MUTED);
    }
    /** What the ability tile drew in the last frame (the key chip, the active's name, the state line), so a check can read it. */
    static List<String> abilityDrawn=List.of();
    /** The commander ability tile over the minimap, in three rows: the class active's name; READY, ACTIVE with the seconds left, READY IN
     *  the seconds left, or NOT IN THIS BUILD; and the ability key beside a bar that fills as the cooldown runs out and drains while the
     *  ability runs. The name and the state each have the tile's full width, so the longest name fits whatever the key is called. */
    static void abilityTile(DrawContext g,net.minecraft.client.font.TextRenderer font,int w,int h){
        if(commanderActive.isEmpty()){abilityDrawn=List.of();return;}
        var row=Commander.byActive(commanderActive);int x=w-116,y=h-150;panel(g,x,y,108,46);
        String name=font.trimToWidth(Commander.activeName(commanderActive).toUpperCase(Locale.ROOT),96);g.drawTextWithShadow(font,name,x+6,y+6,WHITE);
        String state;int color;float fill;
        if(commanderReady<0){state="NOT IN THIS BUILD";color=MUTED;fill=0;}
        else if(commanderFor>0){state="ACTIVE "+(commanderFor+19)/20+" s";color=GOLD;fill=row==null||row.durationTicks()<=0?1:Math.min(1,commanderFor/(float)row.durationTicks());}
        else if(commanderReady>0){state="READY IN "+(commanderReady+19)/20+" s";color=MUTED;fill=row==null||row.cooldownTicks()<=0?0:1-Math.min(1,commanderReady/(float)row.cooldownTicks());}
        else{state="READY";color=GOLD;fill=1;}
        state=font.trimToWidth(state,96);g.drawTextWithShadow(font,state,x+6,y+18,color);
        String key=hint("ability");int kw=Math.min(44,font.getWidth(key)+8);String chip=font.trimToWidth(key,kw-4);
        g.fill(x+6,y+30,x+6+kw,y+42,0xff2a2418);g.drawBorder(x+6,y+30,kw,12,GOLD);g.drawCenteredTextWithShadow(font,chip,x+6+kw/2,y+32,GOLD);
        int bar=x+6+kw+5;g.fill(bar,y+35,x+102,y+37,0xff2a3236);g.fill(bar,y+35,bar+Math.round((x+102-bar)*fill),y+37,color);
        abilityDrawn=List.of(chip,name,state);
    }
    static void renderHud(DrawContext g,float delta){
        var c=MinecraftClient.getInstance();if(c.player==null||c.currentScreen instanceof ArmyScreen||Diagnostics.active()&&Diagnostics.hideHud())return;
        float scale=Math.min(1,c.getWindow().getScaledWidth()/640f);g.getMatrices().push();g.getMatrices().scale(scale,scale,1);
        int w=(int)(c.getWindow().getScaledWidth()/scale),h=(int)(c.getWindow().getScaledHeight()/scale);var font=c.textRenderer;
        if(!battleActive()){campHud(g,w,h);g.getMatrices().pop();return;}
        if(CampaignClient.cinematic()){CampaignClient.hud(g,w,h);g.getMatrices().pop();return;}
        panel(g,w/2-175,7,350,37);g.drawCenteredTextWithShadow(font,DarkUi.teamText(0).toUpperCase()+" "+dawn+" / "+GravesAndGlory.R.winScore()+"  | "+String.format("%d:%02d",CommandMode.seconds/60,CommandMode.seconds%60)+" |  "+DarkUi.teamText(1).toUpperCase()+" "+dusk+" / "+GravesAndGlory.R.winScore(),w/2,14,GOLD);
        DarkUi.teamMark(g,0,w/2-168,14);DarkUi.teamMark(g,1,w/2+161,14);
        var pts=Spec.POINTS;int shown=pts.length,step=330/Math.max(1,shown);for(int i=0;i<shown;i++)g.drawTextWithShadow(font,font.trimToWidth(pts[i].id()+": "+(contestedAt(i)?"Contested":progressAt(i)>0?"Capturing":DarkUi.teamText(ownerAt(i))),step-4),w/2-165+i*step,29,factionColor(ownerAt(i)));
        CommandMode.hud(g,w,h);Presentation.hud(g,w,h);
        if(!running)g.drawCenteredTextWithShadow(font,menu.getBoundKeyLocalizedText().getString()+": war council / collection / maps",w/2,48,WHITE);
        // Key hints sit under the score panel, clear of the hotbar; hidden while an order is being aimed.
        else{if(!CommandMode.active()){g.drawCenteredTextWithShadow(font,keyHints(),w/2,48,MUTED);lesson(g,font,w);}if(respawn>0){panel(g,w/2-100,h/2-40,200,26);g.drawCenteredTextWithShadow(font,"Respawn in "+respawn+" s",w/2,h/2-32,GOLD);}}
        panel(g,w-116,h-100,108,92);g.drawTextWithShadow(font,"M / BATTLEFIELD",w-109,h-92,MUTED);map(g,w-110,h-79,96,68,false);abilityTile(g,font,w,h);CampaignClient.hud(g,w,h);g.getMatrices().pop();
    }
    static final class FantasyButton extends ButtonWidget {
        boolean primary;
        FantasyButton(int x,int y,int w,int h,Text text,PressAction action){super(x,y,w,h,text,action,DEFAULT_NARRATION_SUPPLIER);}
        @Override public void renderButton(DrawContext g,int mx,int my,float delta){
            boolean selected=getMessage().getString().startsWith("> "),focus=active&&(isHovered()||isFocused());int x=getX(),y=getY();
            int surface=!active?0xff20272b:primary?(focus?0xffdfbd77:0xffbb9858):focus?0xff39464b:selected?0xff30474a:0xff252f34;
            g.fill(x,y,x+width,y+height,surface);
            g.drawBorder(x,y,width,height,focus||selected?DarkUi.GOLD:primary?0xffdfbd77:0xff455156);
            if(selected)g.fill(x,y,x+3,y+height,DarkUi.GOLD);
            var font=MinecraftClient.getInstance().textRenderer;String label=getMessage().getString();if(selected)label=label.substring(2);
            var styled=font.trimToWidth(DarkUi.body(label),Math.max(1,width-14));g.drawText(font,net.minecraft.util.Language.getInstance().reorder(styled),x+(width-font.getWidth(styled))/2,y+(height-9)/2,!active?0xff879297:primary?0xff171d20:DarkUi.PAPER,false);
        }
    }
    public static boolean menuVisible(){return MinecraftClient.getInstance().currentScreen instanceof ArmyScreen;}
    abstract static class ArmyScreen extends Screen {
        ArmyScreen(String title){super(Text.literal(title));}
        @Override public boolean shouldPause(){return false;}
        @Override public boolean keyPressed(int key,int scan,int mods){if(menu.matchesKey(key,scan)||key==GLFW.GLFW_KEY_ESCAPE){close();return true;}if(mapKey.matchesKey(key,scan)&&battleActive()){client.setScreen(new MapScreen(this));return true;}return super.keyPressed(key,scan,mods);}
        ButtonWidget primary(String label,int x,int y,int w,java.util.function.Consumer<ButtonWidget> action){var b=new FantasyButton(x,y,w,26,Text.literal(label),action::accept);b.primary=true;return addDrawableChild(b);}
        ButtonWidget button(String label,int x,int y,int w,java.util.function.Consumer<ButtonWidget> action){return addDrawableChild(new FantasyButton(x,y,w,20,Text.literal(label),action::accept));}
    }
    public static final class LobbyScreen extends CampaignLobbyScreen {
        LobbyScreen(){this(0);}
        LobbyScreen(int first){super();tab=first;}
    }
    public static final class CommandScreen extends ArmyScreen {
        CommandScreen(){super("Battlefield commands");}
        @Override protected void init(){
            int groupWidth=Math.min(100,(width-24)/Spec.GROUPS.length);int i=0;
            for(var group:Spec.GROUPS){String id=group.id();button((CommandMode.selected.equals(id)?"> ":"")+group.label()+" "+CommandMode.count(id),width/2-Spec.GROUPS.length*groupWidth/2+i*groupWidth,8,groupWidth-3,b->{CommandMode.selected=id;clearAndInit();});i++;}
            i=0;for(String id:new String[]{"move","attack","charge","hold","follow","retreat"}){var order=Arrays.stream(Spec.ORDERS).filter(o->o.id().equals(id)).findFirst().orElseThrow();var btn=button(order.label(),8,40+i*21,72,b->{if(id.equals("follow")){CommandMode.issue(CommandMode.selected,id,CommandMode.formation,client.player.getPos(),client.player.getYaw(),-1);close();}else CommandMode.begin(id);});btn.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(order.meaning())));i++;}
            button("Map",width-76,45,68,b->client.setScreen(new MapScreen(this)));
            int cardWidth=Math.min(105,(width-24)/Spec.FORMATIONS.length);i=0;
            for(var f:Spec.FORMATIONS){String id=f.id();var btn=button((id.equals(CommandMode.formation)?"> ":"")+f.label(),width/2-Spec.FORMATIONS.length*cardWidth/2+i*cardWidth,height-27,cardWidth-3,b->{CommandMode.changeFormation(id);clearAndInit();});btn.setTooltip(net.minecraft.client.gui.tooltip.Tooltip.of(Text.literal(f.purpose())));i++;}
        }
        @Override public void render(DrawContext g,int mx,int my,float delta){
            panel(g,4,35,80,132);panel(g,width-80,39,76,32);
            int cardWidth=Math.min(105,(width-24)/Spec.FORMATIONS.length),i=0;
            for(var f:Spec.FORMATIONS){int x=width/2-Spec.FORMATIONS.length*cardWidth/2+i*cardWidth;panel(g,x,height-65,cardWidth-3,37);if(f.id().equals(CommandMode.formation))g.drawBorder(x,height-65,cardWidth-3,37,GOLD);
                for(int n=0;n<12;n++){int col=f.id().equals("line")||f.id().equals("shieldwall")?6:4;int dx=n%col*7,dy=n/col*6;if(f.wedge()){int rank=(int)Math.sqrt(n);dx=(n-rank*rank-rank)*7+20;dy=rank*6;}if(f.id().equals("loose")){dx*=2;dy*=2;}g.fill(x+12+dx,height-60+dy,x+15+dx,height-57+dy,GOLD);}i++;}
            g.drawCenteredTextWithShadow(textRenderer,"Choose group and order. Aim and confirm in the battlefield.",width/2,32,WHITE);super.render(g,mx,my,delta);
        }
    }
    public static final class BarracksScreen extends RecruitmentScreen { BarracksScreen(){super();} }
    public static final class MapScreen extends ArmyScreen {
        int x,y,w,h;Marker selected;String pingMode="";
        /** The screen that opened the map; Back returns there, or to the game when opened by the map key. */
        final Screen parent;
        MapScreen(Screen parent){super("Battlefield map");this.parent=parent;}
        @Override protected void init(){h=Math.min(height-86,230);w=Math.min(width-40,h*280/200);h=w*200/280;x=(width-w)/2;y=34;button("Back",12,8,55,b->client.setScreen(parent));int i=0;for(var ping:Spec.PINGS){String id=ping.id();button(ping.label(),width/2-152+i*77,height-22,74,b->pingMode=id);i++;}}
        @Override public boolean mouseClicked(double mx,double my,int b){if(!pingMode.isEmpty()&&b==0&&mx>=x&&mx<=x+w&&my>=y&&my<=y+h){CommandMode.ping(pingMode,(mx-x)*700/w-350,(my-y)*500/h-250);pingMode="";return true;}if(b==1&&mx>=x&&mx<=x+w&&my>=y&&my<=y+h){double tx=(mx-x)*700/w-350,tz=(my-y)*500/h-250;CommandMode.issue(CommandMode.selected,"move",CommandMode.formation,new net.minecraft.util.math.Vec3d(tx,65,tz),client.player.getYaw(),-1);return true;}
            for(var group:CommandMode.groups){double gx=x+(group.cx()+350)*w/700,gz=y+(group.cz()+250)*h/500;if(b==0&&Math.hypot(mx-gx,my-gz)<8){CommandMode.selected=group.id();return true;}}
            for(Marker m:markers){double px=x+(m.x()+350)*w/700,pz=y+(m.z()+250)*h/500;if(Math.hypot(mx-px,my-pz)<6){selected=m;return true;}}return super.mouseClicked(mx,my,b);}
        @Override public void render(DrawContext g,int mx,int my,float delta){g.fill(0,0,width,height,0xee0c1420);g.drawCenteredTextWithShadow(textRenderer,"BATTLEFIELD / FRIENDLY FORCES",width/2,12,GOLD);panel(g,x-4,y-4,w+8,h+8);map(g,x,y,w,h,true);g.drawCenteredTextWithShadow(textRenderer,"Click group to select / Right-click to move / Gold yours, cyan ally",width/2,y+h+12,MUTED);if(selected!=null)g.drawCenteredTextWithShadow(textRenderer,selected.name()+" / "+(selected.own()?"Your troop":"Allied troop")+(selected.fighting()?" / IN COMBAT":" / moving or holding"),width/2,y+h+24,WHITE);super.render(g,mx,my,delta);}
    }
    static final class FailureScreen extends ArmyScreen {
        final String why;FailureScreen(String why){super("Graves and Glory");this.why=why;}
        @Override protected void init(){button("Return to Minecraft",width/2-100,height/2+40,200,b->client.setScreen(new TitleScreen()));}
        @Override public void render(DrawContext g,int x,int y,float d){renderBackground(g);g.drawCenteredTextWithShadow(textRenderer,why,width/2,height/2,0xffffaaaa);super.render(g,x,y,d);}
    }
}



