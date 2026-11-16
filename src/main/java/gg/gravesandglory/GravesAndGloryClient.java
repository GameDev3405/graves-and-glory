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
import gg.gravesandglory.net.Payloads;
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
    /** This server runs Graves and Glory: a STATE packet has arrived since joining (the server sends them while a battlefield builds
     *  too), cleared on disconnect. Public because HungerHudMixin reads it from inside InGameHud. */
    public static volatile boolean modServer;
    static int dawn,dusk,team=-1,count,respawn,gold,queued,trainingSeconds;
    static String message="Preparing battlefield",order="follow",squadFormation="block";
    /** From state_v8 (still in state_v9): the side id each team flies, this army's House trait tiers (tier I or higher), and the commander ability (its active id, ticks until ready or -1 when this build has no behaviour for it, ticks left active; "" while unseated). */
    static String[] sideIds={"",""};static Map<String,Integer> houses=Map.of();static String commanderActive="";static int commanderReady,commanderFor;
    /** The commander ability the HUD slot shows ("none": no slot), with the ticks left and in all of its cooldown. */
    static String abilityId="none";static int abilityRemaining,abilityTotal;
    /** From relics_v1 (gear-combat Task 5): each relic slot's id ("" empty), ticks until ready (-1 empty or no behaviour) and ticks left running. Slot 0 also rides in state_v9's ability field. */
    static String[] relicIds={"","",""};static int[] relicReady={-1,-1,-1},relicFor=new int[3];
    /** What the HUD shows for relic slots 1 and 2 (slot 0 is abilityId above): the id ("none": no slot), the ticks left and in all of the cooldown or run. */
    static String[] relicShown={"none","none","none"};static int[] relicLeft=new int[3],relicTotal=new int[3];
    static boolean[] contested={false,false,false};
    record Sight(float x,float z,int age){}
    static List<Sight> sightings=List.of();
    static int[] owners={-1,-1,-1},progress={0,0,0},challengers={-1,-1,-1},roster=new int[Spec.TROOPS.length],pending=new int[Spec.TROOPS.length];
    static List<Marker> markers=List.of();
    record Marker(float x,float z,boolean fighting,boolean own,String name){}
    static KeyBinding playAgain,panelEdit,menu,attack,charge,followMe,retreat,formation,mapKey,rotateLeft,rotateRight,confirmOrder,cancelOrder,hints,armoury,banner,gearKey;
    static Path runtime;
    static String join="",error="";
    static boolean attempted,opened,joined,shareCamp,launcherContext,connectingInvitation;
    static int titleTicks,worldTicks;
    static Identifier mapTexture;
    static boolean preparingMap;
    @Override public void onInitializeClient(){
        GravesAndGlory.clientRegistries=()->{var w=MinecraftClient.getInstance().world;return w==null?null:w.getRegistryManager();}; // shields made on the client (portraits, the ending podium)
        ClientLoginNetworking.registerGlobalReceiver(VersionGate.CHANNEL,(client,handler,buffer,listeners)->CompletableFuture.completedFuture(VersionGate.response()));
        ClientConfigurationNetworking.registerGlobalReceiver(Payloads.ReleaseCheckRequestPayload.ID,(payload,context)->context.responseSender().sendPacket(Payloads.ReleaseCheckReplyPayload.of(VersionGate.response()))); // the configuration gate (VersionGate); the login answer above stays while the server keeps its fallback
        ClientConfigurationNetworking.registerGlobalReceiver(Payloads.ClientHelloRequestPayload.ID,(payload,context)->context.responseSender().sendPacket(Payloads.ClientHelloReplyPayload.of(RegistryFingerprint.write()))); // plain clients P3.1: this client's raw ids of the Polymer server-only entries
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("gravesandglory","armory"),net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("gravesandglory").orElseThrow(),net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED);
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of("gravesandglory","weapons"),net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("gravesandglory").orElseThrow(),net.fabricmc.fabric.api.resource.ResourcePackActivationType.DEFAULT_ENABLED);
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(LookPacks::apply);
        try { LegacyDataMigration.instance(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir(), Diagnostics.active()); }
        catch (java.io.IOException migration) { error="Could not migrate old collection/settings. Your originals are unchanged. Check storage and permissions."; }
        Presentation.load();
        UiEditorClient.register(); // the UI editor: the server's published UI sheet, and the editor for owners (/gg ui edit)
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(UiTex.RELOADER); // the UI kit's atlas.json, read at every resource reload
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(BattleHud.RELOADER); // the battle HUD's lesson wrap and texture stop
        // Event feed lines from the server (FeedEvents); only a running round shows them.
        ClientPlayNetworking.registerGlobalReceiver(Payloads.FeedPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();String kind=buf.readString(16);int team=buf.readInt();String text=buf.readString(FeedEvents.MAX_TEXT);client.execute(()->{if(running)EventFeed.push(kind,team,text,net.minecraft.util.Util.getMeasuringTimeMs());});});
        BattleMapClient.register(); // battlemap_grid_v1 and battlemap_v1 (the battle map's field and live state)
        ClientPlayNetworking.registerGlobalReceiver(Payloads.ImpactPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();float strength=buf.readFloat();client.execute(()->{Presentation.receivedImpacts++;Presentation.impact(strength);});});
        // The troops' own models (Phase 2 Task 5): one renderer per sheet entity type, the models reloaded with the resources.
        TroopRenderers.register();
        WagonRenderer.register(); // the Convoy supply wagon
        MapBlocksClient.register(); // still_water: translucent layer and water tint

        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new TroopModelLoader());
        // The motion engine's clips (animation rework Task 13): clips/<rig>/<family>/*.json, each family's attacks in the sheet's order (Tactics.variants).
        {Map<String,List<String>> order=new LinkedHashMap<>();for(Spec.AttackVariantsRow v:Spec.ATTACKVARIANTS)order.computeIfAbsent(v.family(),k->new ArrayList<>()).add(v.id());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new ClipLibrary(order));}
        // The commander gear's own models (gear art Task 9), drawn over every player by CommanderGearFeature.
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.resource.ResourceType.CLIENT_RESOURCES).registerReloadListener(new GearModels());
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type,renderer,helper,context)->{
            if(renderer instanceof net.minecraft.client.render.entity.PlayerEntityRenderer player)helper.register(new CommanderGearFeature(player));
        });
        menu=key("menu",GLFW.GLFW_KEY_R);attack=key("attack",GLFW.GLFW_KEY_Z);charge=key("charge",GLFW.GLFW_KEY_X);followMe=key("follow",GLFW.GLFW_KEY_C);retreat=key("retreat",GLFW.GLFW_KEY_V);formation=key("formation",GLFW.GLFW_KEY_B);mapKey=key("map",GLFW.GLFW_KEY_M);
        rotateLeft=key("rotate_left",GLFW.GLFW_KEY_LEFT_BRACKET);rotateRight=key("rotate_right",GLFW.GLFW_KEY_RIGHT_BRACKET);
        hints=key("hints",GLFW.GLFW_KEY_LEFT_ALT); // held, the battle HUD shows every key hint (BattleHud.altLayer)
        armoury=key("armoury",GLFW.GLFW_KEY_U); // opens the barracks screen on its Armoury tab, anywhere in a battle (ArmouryTab)
        gearKey=key("gear",GLFW.GLFW_KEY_I); // the Arsenal (the commander's gear, WarTableGear), in camp only
        banner=key("banner",GLFW.GLFW_KEY_N); // the War Table's shortcut to the Banner page (WarTableRecruit.keyPressed), rebindable in Controls like the rest (4F.5)
        playAgain=key("play_again",GLFW.GLFW_KEY_J); // Play again on the battle report's REWARDS step (EndingScreen), in a room only; J is free in vanilla 1.20.1 and in every other binding here (test_key_defaults), P is vanilla's Social Interactions
        panelEdit=key("panel_edit",GLFW.GLFW_KEY_H); // Arrange command panel: edit mode moves, hides or fades the panel (CommandPanel.toggleEdit), only while the battle HUD shows
        confirmOrder=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.confirm",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_ENTER,"category.gravesandglory"));
        cancelOrder=KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory.cancel",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_BACKSPACE,"category.gravesandglory"));
        String local=System.getenv("LOCALAPPDATA");runtime=Path.of(System.getProperty("gravesandglory.runtime",local==null?"gravesandglory-runtime":local+"/gravesandglory"));
        // Optional launcher integration. Ordinary installs need no runtime directory.
        launcherContext=Files.isRegularFile(runtime.resolve("melty.properties"));
        if(launcherContext)try{Files.writeString(runtime.resolve("melty.log"),"");Properties props=new Properties();try(var in=Files.newInputStream(runtime.resolve("melty.properties"))){props.load(in);}join=props.getProperty("join","").trim();}catch(Exception e){System.err.println("[Graves and Glory] Optional launcher invitation could not be read: "+e.getMessage());}
        ClientPlayNetworking.registerGlobalReceiver(Payloads.RelicsPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();int n=Math.min(3,buf.readByte());String[] ids={"","",""};int[] ready={-1,-1,-1},run=new int[3];
            for(int i=0;i<n;i++){ids[i]=buf.readString(64);ready[i]=buf.readInt();run[i]=buf.readInt();}
            client.execute(()->{relicIds=ids;relicReady=ready;relicFor=run;for(int i=1;i<3;i++){int[] s=relicShow(ids[i],ready[i],run[i]);relicShown[i]=s==null?"none":ids[i];relicLeft[i]=s==null?0:s[0];relicTotal[i]=s==null?0:s[1];}});});
        // Relics fire on hotbar keys 1 to 3 in battle (gear-combat Task 5): drained here before vanilla's input handling, so in battle they never
        // change the selected slot (the server pins it to 0 as well, GearCombat.tick); outside battle vanilla gets them and they change slot as ever.
        // With the command panel open the number keys 1 to 4 pick a pill (or a shape, with the tray open) instead and never reach the relics (CommandSelection.pick).
        ClientTickEvents.START_CLIENT_TICK.register(client->{if(client.player==null||client.currentScreen!=null||!battleActive())return;
            if(CommandPanel.editing()){for(int i=0;i<4;i++)while(client.options.hotbarKeys[i].wasPressed()){}return;} // edit mode: the number keys neither pick nor fire relics
            if(CommandSelection.panelOpen()){boolean shift=CommandSelection.shiftHeld(client);for(int i=0;i<4;i++)while(client.options.hotbarKeys[i].wasPressed())CommandSelection.pick(i+1,shift,shift);return;}
            for(int i=0;i<3;i++)while(client.options.hotbarKeys[i].wasPressed())send("relic:"+i);});
        ClientPlayNetworking.registerGlobalReceiver(Payloads.GearPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();String text=buf.readString(GearWire.LIMIT);client.execute(()->CampaignClient.gear=GearWire.decode(text));});
        ClientPlayNetworking.registerGlobalReceiver(Payloads.DropPayload.ID,(payload,context)->DropClient.receive(context.client(),payload.buf()));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.CampPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();String page=buf.readString(32);client.execute(()->CampScreen.open(page));});
        ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenBarracksPayload.ID,(payload,context)->{MinecraftClient client=context.client();client.execute(()->client.setScreen(running?new BarracksScreen():new CampScreen("home")));});
        ClientPlayNetworking.registerGlobalReceiver(Payloads.EndingBeginPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();String outcome=buf.readString(16),line=buf.readString(512);client.execute(()->EndingClient.beginEnding(client,outcome,line));});
        ClientPlayNetworking.registerGlobalReceiver(Payloads.PartyPayload.ID,(payload,context)->{MinecraftClient client=context.client();client.execute(()->client.setScreen(new CampScreen("home")));});
        // The motion events (Task 6): a troop's stance (stance_v2), its swings (attack_v1) and the blows on it (hit_v1), kept in MotionEvents.Client for the
        // motion engine. A stance a player gets on START_TRACKING comes before the entity's spawn packet (Fabric calls the event first), so a stance is
        // kept for an id the world does not have yet (the server sends the catch-up swing after the spawn); an unloaded entity's entries go. TroopMotion reads them each frame.
        ClientPlayNetworking.registerGlobalReceiver(Payloads.StancePayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();int id=buf.readVarInt(),i=buf.readByte();float pitch=buf.readFloat();client.execute(()->MotionEvents.Client.stance(id,i,pitch));});
        // A swing or a blow for an id this world does not have is dropped (no queue for an unloaded troop); a swing is checked against its family's rows.
        ClientPlayNetworking.registerGlobalReceiver(Payloads.AttackPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();int id=buf.readVarInt(),variant=buf.readVarInt();long start=buf.readVarLong();client.execute(()->{
            net.minecraft.entity.Entity e=client.world==null?null:client.world.getEntityById(id);Spec.TroopsRow t=e==null?null:TroopModels.troopOf(e);
            if(variant==MotionInput.CANCEL){if(e!=null)MotionEvents.Client.cancel(id,start,client.world.getTime());return;} // a dropped shot (coordinator 2026-11-11): any troop's cancel is obeyed
            MotionEvents.Client.attack(id,variant,start,e==null?MotionEvents.Client.NO_ENTITY:t==null?MotionEvents.Client.UNKNOWN_TROOP:Tactics.variants(t.family()).size());});});
        ClientPlayNetworking.registerGlobalReceiver(Payloads.HitPayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();int id=buf.readVarInt(),kind=buf.readByte(),zone=buf.readByte();float bearing=buf.readFloat();int weight=buf.readByte(),flags=buf.readByte();long tick=buf.readVarLong();
            client.execute(()->MotionEvents.Client.hit(client.world!=null&&client.world.getEntityById(id)!=null,id,kind,zone,bearing,weight,flags,tick));});
        // rise_v1: a troop rose instead of dying; kept, like a stance, for an id the world may not have yet (DeathController plays it).
        ClientPlayNetworking.registerGlobalReceiver(Payloads.RisePayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();int id=buf.readVarInt();long tick=buf.readVarLong();int window=buf.readVarInt();client.execute(()->MotionEvents.Client.rise(id,tick,window));});
        ClientEntityEvents.ENTITY_UNLOAD.register((entity,world)->{TroopMotion.forget(entity.getId());MotionEvents.Client.forget(entity.getId());});
        // A soldier's rank (rank_v1): level, weapon tier and armour tier by entity id, for the troop renderer, the chevrons and the portraits (Task 9).
        RankSync.registerClient();
        CommandNet.registerClient(); // command_v1: the charge cooldown, teammates' outlines and order acknowledgements
        ClientPlayNetworking.registerGlobalReceiver(Payloads.StatePayload.ID,(payload,context)->{MinecraftClient client=context.client();PacketByteBuf buf=payload.buf();
            modServer=true;
            boolean r=buf.readBoolean(),run=buf.readBoolean();int a=buf.readInt(),b=buf.readInt();String msg=buf.readString();int t=buf.readInt(),n=buf.readInt();String o=buf.readString(),f=buf.readString();int resp=buf.readInt();
            int points=buf.readInt();if(points<1||points>8)return;boolean[] contest=new boolean[points];int[] own=new int[points],pr=new int[points],ch=new int[points];for(int i=0;i<points;i++){buf.readString();own[i]=buf.readInt();pr[i]=buf.readInt();ch[i]=buf.readInt();contest[i]=buf.readBoolean();}
            int g=buf.readInt();boolean near=buf.readBoolean();int q=buf.readInt(),eta=buf.readInt(),types=buf.readInt();if(types!=Spec.TROOPS.length)return;
            int[] owned=new int[types],waiting=new int[types];for(int i=0;i<types;i++){owned[i]=buf.readInt();waiting[i]=buf.readInt();}
            int amount=buf.readInt();if(amount<0||amount>160)return;List<Marker> friendly=new ArrayList<>();for(int i=0;i<amount;i++)friendly.add(new Marker(buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean(),buf.readString()));
            int groupCount=buf.readInt();if(groupCount<0||groupCount>8)return;/* state_v10: 3 to 5 group records, Riders and Siege when fielded */var groups=new ArrayList<CommandMode.Group>();
            for(int i=0;i<groupCount;i++)groups.add(new CommandMode.Group(buf.readString(),buf.readInt(),buf.readFloat(),buf.readString(),buf.readString(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readFloat(),buf.readBoolean(),buf.readBoolean(),buf.readInt(),buf.readFloat(),buf.readInt(),buf.readInt(),buf.readBoolean())); // full and strength (UI), then state_v9's level2, level3 and armouryOpen
            String abilityRead=buf.readString();int cooldownLeft=buf.readInt(),cooldownAll=buf.readInt();
            int pingCount=buf.readInt();if(pingCount<0||pingCount>4)return;var pings=new ArrayList<CommandMode.Ping>();for(int i=0;i<pingCount;i++)pings.add(new CommandMode.Ping(buf.readString(),buf.readFloat(),buf.readFloat()));int seconds=buf.readInt();
            int seen=buf.readInt();if(seen<0||seen>160)return;var sight=new ArrayList<Sight>();for(int i=0;i<seen;i++)sight.add(new Sight(buf.readFloat(),buf.readFloat(),buf.readInt()));
            String campaignJson=buf.readString(Campaign.SNAPSHOT_LIMIT);
            String sideA=buf.readString(64),sideB=buf.readString(64);int tierCount=buf.readByte();if(tierCount<0||tierCount>Spec.HOUSES.length)return;Map<String,Integer> tiers=new LinkedHashMap<>();for(int i=0;i<tierCount;i++){String house=buf.readString(64);tiers.put(house,(int)buf.readByte());}
            String ability=buf.readString(64);int abilityReady=buf.readInt(),abilityFor=buf.readInt();
            var armoury=LevelMath.decodeArmoury(wire(buf));if(armoury==null)return; // state_v9's Armoury block (LevelMath.encodeArmoury)
            client.execute(()->{boolean was=running;String before=message;long now=net.minecraft.util.Util.getMeasuringTimeMs();
                // The feed starts empty with each round and empties when it ends, so a map build's "Preparing ..." never reaches it;
                // while the round runs, a changed server message joins it as a notice with the player's own keys filled in.
                if(run!=was){EventFeed.clear();BattleMapClient.clearRound();}if(run&&!was){BattleHud.roundShownMs=now;BattleHud.tiersSeen=null;CommandNet.Client.reset();CommandSelection.reset();}
                if(run&&!msg.isEmpty()&&(!msg.equals(before)||!was))EventFeed.push("notice",t,HintText.expand(msg,GravesAndGloryClient::hint),now);
                CampaignClient.armoury=armoury;sideIds=new String[]{sideA,sideB};houses=Collections.unmodifiableMap(tiers);commanderActive=ability;commanderReady=abilityReady;commanderFor=abilityFor;CampaignClient.accept(client,campaignJson,run);sightings=List.copyOf(sight);contested=contest;CommandMode.pings=java.util.List.copyOf(pings);CommandMode.seconds=seconds;CommandMode.noteGroups(CommandMode.groups,groups,net.minecraft.util.Util.getMeasuringTimeMs());CommandMode.groups=java.util.List.copyOf(groups);ready=r;running=run;dawn=a;dusk=b;message=msg;team=t;count=n;order=o;squadFormation=f;respawn=resp;owners=own;progress=pr;challengers=ch;gold=g;nearBarracks=near;queued=q;trainingSeconds=eta;roster=owned;pending=waiting;markers=List.copyOf(friendly);abilityId=abilityRead;abilityRemaining=cooldownLeft;abilityTotal=cooldownAll;if(abilityRead.equals("none"))classAbility();});
        });
        ClientPlayConnectionEvents.JOIN.register((handler,sender,client)->{resetHud();CampaignClient.sendProfile(client);if(connectingInvitation&&!joined){joined=true;say("Melty joined: "+join);}});
        // The battle HUD's records (the feed, what the last frame drew, the lesson wrap, an aimed order) are cleared on the client
        // thread, and not through client.execute: Fabric can fire this event on the network thread, and MinecraftClient.disconnect
        // wipes the queued tasks (the Task 3 re-review's race). The flag waits for the first client tick without a world, and the
        // next join clears them as well; no HUD frame runs without a world to clear them before the next one.
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{CommandPanel.disconnected(client);if(connectingInvitation&&!joined)say("Melty join failed: connection closed before joining");CampaignClient.end(client);EndingClient.forget(client);DropClient.clear(client);CampaignClient.forget();MusicDirector.reset(client);TroopMotion.clear();MotionEvents.Client.clear();RankSync.clear();hudResetPending=true;ready=false;modServer=false;running=false;team=-1;houses=Map.of();opened=false;worldTicks=0;shareCamp=false;connectingInvitation=false;});
        ClientTickEvents.END_CLIENT_TICK.register(GravesAndGloryClient::ebbCasters);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(!toolbarFreed&&client.options!=null){toolbarFreed=true;freeToolbarKeys(client,toolbarFlag());KeyMigration.run(client);} // the old G / V command keys move to Z / C once (KeyMigration)
            if(hudResetPending&&client.world==null)resetHud();
            if(client.getOverlay()==null&&client.currentScreen instanceof TitleScreen){
                if(!attempted){PackControls.prepare(client);attempted=true;}
                client.setScreen(error.isEmpty()?new FrontMenuScreen():new FailureScreen(error));
            }
            if(client.player!=null){
                worldTicks++;if(ready&&worldTicks%20==0)client.getToastManager().clear();prepareMap(client);CampaignClient.tick(client);EndingClient.tick(client);DropClient.tick(client);MusicDirector.tick(client);Diagnostics.tick(client);
                while(panelEdit.wasPressed())CommandPanel.toggleEdit(client);
                CommandPanel.editTick(client);
                if(CommandPanel.editing()){for(KeyBinding k:new KeyBinding[]{menu,attack,charge,followMe,retreat,formation,confirmOrder,cancelOrder})while(k.wasPressed()){}} // no orders while the panel is being arranged
                while(menu.wasPressed()){if(EndingClient.step()!=null){EndingClient.skip(client);continue;} // the match ending: the menu key skips this player's step
                    CampaignClient.end(client);if(battleActive()&&PanelLayout.hidden){client.player.sendMessage(Text.literal(HintText.expand(UiText.t("client.panelHidden","Command panel hidden. Press {key:panel_edit} to arrange."),GravesAndGloryClient::hint)),true);continue;} // keyboard-only mode: R opens nothing
                    CommandMode.cancel();if(battleActive())CommandSelection.setPanelOpen(!CommandSelection.panelOpen());else client.setScreen(new CampScreen("home"));}
                while(armoury.wasPressed())if(battleActive())client.setScreen(new RecruitmentScreen(1));
                while(gearKey.wasPressed())if(!battleActive())client.setScreen(new CampScreen("gear"));
                // Orders work with the panel closed, on the last selection; Shift keeps the panel open for the next one.
                boolean shift=CommandSelection.shiftHeld(client);
                while(attack.wasPressed())CommandSelection.order("attack",shift);while(charge.wasPressed())CommandSelection.order("charge",shift);
                while(followMe.wasPressed())CommandSelection.order("follow",shift);while(retreat.wasPressed())CommandSelection.order("retreat",shift);
                while(formation.wasPressed())CommandSelection.order("formation",shift);
                while(rotateLeft.wasPressed()){CommandMode.facingSet=true;CommandMode.facing-=Spec.COMMANDINTERFACE[0].facingStep();}
                while(rotateRight.wasPressed()){CommandMode.facingSet=true;CommandMode.facing+=Spec.COMMANDINTERFACE[0].facingStep();}
                CommandMode.tick(client);CommandSelection.tick();
                ChargeCamera.tick(client,charge); // holding X lifts the overhead charge camera; its release sends the Charge
                while(confirmOrder.wasPressed())if(CommandMode.active())CommandMode.confirm();
                while(cancelOrder.wasPressed())if(!CommandMode.cancel()&&CommandSelection.trayOpen())CommandSelection.setTrayOpen(false);
                while(mapKey.wasPressed())if(battleActive())client.setScreen(new BattleMapScreen(null));
                if(ready&&!opened&&shareCamp&&client.getServer()!=null){opened=true;try{int port=0;String requested=System.getenv("GRAVESANDGLORY_HOST_PORT");if(requested==null)requested=System.getenv("BANNERBOUND_HOST_PORT");if(requested!=null){port=Integer.parseInt(requested);if(port<1024||port>65535)throw new IllegalArgumentException("Invalid host port");}if(port==0)try(ServerSocket socket=new ServerSocket(0)){port=socket.getLocalPort();}if(client.getServer().openToLan(GameMode.ADVENTURE,false,port)){String address=InetAddress.getLocalHost().getHostAddress()+":"+port;say("Melty join address: "+address);client.player.sendMessage(Text.literal("Camp open on your local network: "+address),false);}else client.player.sendMessage(Text.literal("Could not open camp to the local network."),false);}catch(Exception e){client.player.sendMessage(Text.literal("Could not share camp: "+e.getMessage()),false);}}
            }
        });
        HudRenderCallback.EVENT.register(GravesAndGloryClient::renderHud);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(CommandMode::renderWorld);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(PlanMarkers::render); // the plan flags over planned points (battle map spec 5.4)
        DropAltar.register();
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(DropAltar::world);
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(DropRender::world); // the drop board's coins, flares and risers
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(EndingClient::renderFigures); // the match ending's podium figures
        ChargeCamera.register(); // the overhead charge camera: its option, its toggle binding and the ground arrow
        OrderOutlines.register(); // formation outlines, pulses and the rally pose; registered after the tick handler above so it reads the aim that handler left
        // Troop depth: the frame's cutoff from the game camera, once, before any pass draws (Iris's shadow pass among them).
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(ctx->TroopDepth.beginFrame(ctx.camera().getPos(),ctx.world().getEntities()));
        // Settled bodies (BodyCache): queue from the frame's start, drawn from their buffers after the entities.
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.START.register(ctx->BodyCache.begin(ctx.tickCounter().getTickDelta(true)));
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(BodyCache::flush);
        // Bodies part way through their corpse fade (R42): translucent, once every entity layer of the frame is drawn.
        net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.BEFORE_DEBUG_RENDER.register(BodyCache::fadePass);
    }
    /** A packet buffer as LevelMath's pure reader: readBoolean, readByte and readString (64 characters at most, a troop id). */
    static LevelMath.In wire(PacketByteBuf b){return new LevelMath.In(){public boolean bool(){return b.readBoolean();}public int b(){return b.readByte();}public String str(){return b.readString(64);}};}
    private static boolean toolbarFreed;
    /** Vanilla's creative hotbar activators sit on C (save) and X (load), the keys Follow me and Charge now use, and only one binding per key receives
     *  presses. On the FIRST launch ever (no flag file yet) those two are unbound if they still hold C or X; the flag file records it, so later launches
     *  never touch them and a player who rebinds them back to C or X keeps that choice. Returns whether a binding was changed. */
    public static boolean freeToolbarKeys(MinecraftClient client,Path flag){
        if(client.options==null||Files.exists(flag))return false;
        boolean changed=false;
        for(KeyBinding k:new KeyBinding[]{client.options.saveToolbarActivatorKey,client.options.loadToolbarActivatorKey}){
            String bound=k.getBoundKeyTranslationKey();
            if(bound.equals("key.keyboard.c")||bound.equals("key.keyboard.x")){client.options.setKeyCode(k,InputUtil.UNKNOWN_KEY);changed=true;}
        }
        if(changed){KeyBinding.updateKeysByCode();client.options.write();}
        try{Files.createDirectories(flag.getParent());Files.writeString(flag,"toolbarKeysFreed=true\n");}catch(Exception e){System.err.println("[Graves and Glory] Could not record the first-launch key flag: "+e.getMessage());}
        return changed;
    }
    /** The file whose presence says the first launch has happened. */
    public static Path toolbarFlag(){return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("gravesandglory-client.properties");}
    static KeyBinding key(String name,int key){return KeyBindingHelper.registerKeyBinding(new KeyBinding("key.gravesandglory."+name,InputUtil.Type.KEYSYM,key,"category.gravesandglory"));}
    /** Set by a disconnect, which may come on the network thread; the client tick clears the battle HUD's records once no world is
     *  left (resetHud). */
    static volatile boolean hudResetPending;
    /** The last battle's records go: the feed, what the HUD last drew and its lesson wrap, the command state. On the client thread:
     *  the first client tick without a world after a disconnect, and every join. */
    static void resetHud(){hudResetPending=false;EventFeed.clear();BattleHud.reset();CommandMode.reset();CommandSelection.reset();CommandNet.Client.reset();OrderOutlines.reset();OrderFeedback.reset();ChargeCamera.reset();}
    /** The binding with this translation key, or null when there is none. */
    static KeyBinding binding(String translationKey){
        for(KeyBinding k:MinecraftClient.getInstance().options.allKeys)if(k.getTranslationKey().equals(translationKey))return k;
        return null;
    }
    /** A binding's first three mouse buttons by the key bound, not by its name: LMB, RMB or MMB in every language (HintText.mouseChip);
     *  null for any other key and for an unbound binding. */
    static String mouseChip(KeyBinding k){
        if(k==null||k.isUnbound())return null;
        InputUtil.Key key=KeyBindingHelper.getBoundKeyOf(k);
        return key.getCategory()==InputUtil.Type.MOUSE?HintText.mouseChip(key.getCode()):null;
    }
    /** The player's own key for an action, as a hint shows it: the binding HintText.bindingKey names (key.attack for confirm and
     *  hit, key.use for cancel and use, key.gravesandglory.(id) for the rest: menu, attack, follow, formation, map, rotate_left,
     *  rotate_right, ability, hints, armoury), the first three mouse buttons as LMB, RMB and MMB (mouseChip), any other key and an unbound
     *  one in the game's own words, so every hint names it alike: chips, the lessons, the camp sign and the notices. */
    public static String hint(String id){
        KeyBinding k=binding(HintText.bindingKey(id));
        if(k==null)return InputUtil.UNKNOWN_KEY.getLocalizedText().getString();
        String mouse=mouseChip(k);
        return mouse!=null?mouse:k.getBoundKeyLocalizedText().getString();
    }
    /** The same key's short name for a crowded key row (the hint slip and the tall cards, UiMath.fitKeys): PgDn, RShift, Num4 or
     *  Caps for the long names (HintText.keyShort), MB4 and on for the other mouse buttons, "None" for an unbound key; hint's own
     *  name where that is no longer. */
    public static String hintShort(String id){
        KeyBinding k=binding(HintText.bindingKey(id));
        if(k==null||k.isUnbound())return HintText.UNBOUND_SHORT;
        InputUtil.Key key=KeyBindingHelper.getBoundKeyOf(k);
        String full=hint(id),s=key.getCategory()==InputUtil.Type.MOUSE?HintText.mouseShort(key.getCode()):key.getCategory()==InputUtil.Type.KEYSYM?HintText.keyShort(key.getCode()):null;
        return HintText.shorter(full,s);
    }
    /** KeybindTextMixin's name for a keybind text's binding (by its translation key): LMB, RMB or MMB for a binding the hints name
     *  (HintText.hinted) on one of those mouse buttons, while this is a Graves and Glory server (modServer); null keeps the game's
     *  own text everywhere else (other servers, singleplayer worlds of other games, other bindings). */
    public static String keybindChip(String translationKey){
        if(!modServer||!HintText.hinted(translationKey))return null;
        return mouseChip(binding(translationKey));
    }
    /** The camp's bottom hint, naming the player's own menu and use keys (a mouse button as LMB, RMB or MMB). */
    public static String campHint(){return hint("menu")+" / War table    "+hint("use")+" on a station to interact";}
    static void say(String line){try{Files.writeString(runtime.resolve("melty.log"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ignored){}}
    /** The last action send passed on (the harness reads what a screen sent). */
    public static volatile String lastSent="";
    static void send(String action){if(ClientPlayNetworking.canSend(GravesAndGlory.ORDER)){lastSent=action;PacketByteBuf b=PacketByteBufs.create();b.writeString(action);ClientPlayNetworking.send(Payloads.OrderPayload.of(b));}}
    static void connectOrHost(MinecraftClient c){
        if(!join.isEmpty()){connectingInvitation=true;net.minecraft.client.gui.screen.multiplayer.ConnectScreen.connect(new FrontMenuScreen("friends"),c,ServerAddress.parse(join),new ServerInfo("Graves and Glory",join,ServerInfo.ServerType.OTHER),false,null);return;}
        try { LegacyDataMigration.world(c.runDirectory.toPath().resolve("saves")); }
        catch (java.io.IOException migration) { c.setScreen(new FailureScreen("Could not upgrade the Bannerbound world. Originals were kept. "+migration.getMessage()));return; }
        if(Files.exists(c.runDirectory.toPath().resolve("saves/Graves and Glory/level.dat"))){c.createIntegratedServerLoader().start("Graves and Glory",()->c.setScreen(new TitleScreen()));return;}
        var info=new LevelInfo("Graves and Glory",GameMode.ADVENTURE,false,Difficulty.NORMAL,true,new GameRules(),DataConfiguration.SAFE_MODE);
        c.createIntegratedServerLoader().createAndStart("Graves and Glory",info,new GeneratorOptions(728619L,false,false),r->r.get(RegistryKeys.WORLD_PRESET).get(WorldPresets.FLAT).createDimensionsRegistryHolder(),new TitleScreen());
    }
    static void startCamp(MinecraftClient c,boolean friends){
        // A saved launcher invitation never redirects an explicit local-play choice.
        String invitation=join;join="";shareCamp=friends;opened=false;joined=false;connectingInvitation=false;
        try{connectOrHost(c);}finally{join=invitation;}
    }
    static void invalidateMap(MinecraftClient c){if(mapTexture!=null){c.getTextureManager().destroyTexture(mapTexture);mapTexture=null;}preparingMap=false;}
    static void prepareMap(MinecraftClient c){
        if(preparingMap)return;preparingMap=true;final int generation=CampaignMaps.selected,hx=Field.halfX(),hz=Field.halfZ();
        // Arena.mapColor gives ARGB and NativeImage takes ABGR: the bit mask 0xff00ff00 keeps alpha and green, and red and blue swap.
        CompletableFuture.supplyAsync(()->{NativeImage image=new NativeImage(281,201,false);for(int x=0;x<281;x++)for(int z=0;z<201;z++){int col=Arena.mapColor((int)((x-140)*hx/140.0),(int)((z-100)*hz/100.0));image.setColor(x,z,(col&0xff00ff00)|((col>>16)&255)|((col&255)<<16));}return image;}).thenAccept(image->c.execute(()->{if(generation!=CampaignMaps.selected){image.close();preparingMap=false;return;}mapTexture=c.getTextureManager().registerDynamicTexture("gravesandglory_map",new NativeImageBackedTexture(image));}));
    }
    // The point arrays refill with the next state packet after a map switch, so until then they can be shorter or longer than Spec.POINTS.
    static boolean contestedAt(int i){var a=contested;return i>=0&&i<a.length&&a[i];}
    static int ownerAt(int i){var a=owners;return i>=0&&i<a.length?a[i]:-1;}
    static int progressAt(int i){var a=progress;return i>=0&&i<a.length?a[i]:0;}
    static int challengerAt(int i){var a=challengers;return i>=0&&i<a.length?a[i]:-1;}
    static void panel(DrawContext g,int x,int y,int w,int h){DarkUi.frame(g,x,y,w,h);}
    /** Lesson text wraps to this width (the small style) inside the 280 px lesson note BattleHud draws, its first line the LESSON
     *  k/4 tag and 6 px narrower (UiMath.lessonLines, UI Task 8c). */
    static final int LESSON_TEXT_WIDTH=UiMath.LESSON_TEXT_W;
    /** The lesson text lines drawn in the last frame, so a check can compare them with the full copy. */
    static List<String> lessonDrawn=List.of();
    /** The copy of lesson step (0 to 3), naming the player's own key binding. */
    static String lessonText(int step){
        return switch(step){
            case 0->HintText.expand(UiText.t("client.lesson.follow","Press {key:follow} to make your army follow you, or {key:retreat} to pull it back to you."),GravesAndGloryClient::hint);
            case 1->HintText.expand(UiText.t("client.lesson.formation","Press {key:formation}, pick a shape, then press {key:confirm} on the ground to hold that point in it."),GravesAndGloryClient::hint);
            case 2->HintText.expand(UiText.t("client.lesson.attack","Press {key:attack} to send your army to attack at once. Or press {key:charge}, aim and press {key:confirm} to charge."),GravesAndGloryClient::hint);
            default->HintText.expand(UiText.t("client.lesson.recruit","Press {key:use} at your barracks lectern to recruit. Trained troops march out from the held point nearest the fight."),GravesAndGloryClient::hint);
        };
    }
    public static boolean inCamp(){return ready&&!battleActive();}
    public static boolean battleActive(){return ready&&running&&!CampaignClient.waiting;}
    /** The camp slips' boxes (UiMath.campHud) and their rows' words as the last camp frame drew them, for the checks. */
    static Map<String,int[]> campRectsDrawn=new LinkedHashMap<>();
    static List<List<String>> campRowsDrawn=new ArrayList<>();
    /** The camp's slips on the HUD canvas (UI Task 8b-2), the battle hint slip's look: at the top left the camp's name in INK and the
     *  gold and title in SEPIA; at the bottom right, off vanilla's bars and the chat, the war table's key and the stations' key, each
     *  an ink chip naming the player's binding (a mouse button as LMB, RMB or MMB) before its words in SEPIA, the keys taking their
     *  short names, then giving way, where a long name would run a row past the paper (UiMath.fitKeys). */
    static void campSlips(DrawContext g){
        var c=MinecraftClient.getInstance();campRectsDrawn.clear();campRowsDrawn.clear();if(c.currentScreen!=null)return;
        var font=c.textRenderer;var win=c.getWindow();
        UiCanvas.beginHud(g);
        try{
            var at=UiMath.campHud(win.getFramebufferWidth(),win.getFramebufferHeight(),(int)win.getScaleFactor(),UiCanvas.hudK());
            int[] head=at.get("camp-head"),hint=at.get("camp-hint");
            UiTex.draw(g,"hud/notice-slip",head[0],head[1]);
            String campName=UiText.t("client.camp.title","Banner camp");
            g.drawText(font,DarkUi.heading(campName),head[0]+10,head[1]+7,DarkUi.INK_TEXT,false);
            String purse=UiMath.trim(UiText.t("client.camp.purse","{writs} writs / {title}","writs",CampaignClient.profile.troopWrits,"title",CampaignClient.profile.progression.title),BattleHud.HINT_ROW_W,font::getWidth);
            g.drawText(font,purse,head[0]+10,head[1]+24,DarkUi.SEPIA,false);
            campRowsDrawn.add(List.of(campName));campRowsDrawn.add(List.of(purse));
            UiTex.draw(g,"hud/notice-slip",hint[0],hint[1]);
            String[] ids={"menu","use"},words={UiText.t("client.camp.warTable","War table"),UiText.t("client.camp.station","on a station to interact")};
            for(int row=0;row<2;row++){
                var keys=UiMath.fitKeys(List.of(hint(ids[row])),List.of(hintShort(ids[row])),Arrays.asList(words[row]),BattleHud.HINT_ROW_W,8,font::getWidth);
                String key=keys.get(0);int kx=hint[0]+10,ky=hint[1]+(row==0?7:22),kw=UiMath.chipWidth(font.getWidth(key));
                UiTex.three(g,"hud/key-chip",kx,ky,kw);g.drawText(font,key,kx+2,ky+2,DarkUi.CHIP_TEXT,false);
                g.drawText(font,words[row],kx+kw+2,ky+2,DarkUi.SEPIA,false);campRowsDrawn.add(List.of(key,words[row]));
            }
            campRectsDrawn.putAll(at);
        }finally{UiCanvas.end(g);}
    }
    static void campHud(DrawContext g,int w,int h){
        var c=MinecraftClient.getInstance();if(c.currentScreen!=null)return;
        else if(CampaignClient.waiting)g.drawCenteredTextWithShadow(c.textRenderer,UiText.t("client.camp.battleUnderway","A battle is underway / Your camp remains open"),w/2,24,DarkUi.MUTED);
    }
    /** Relic slot 0 in the HUD's ability slot (BattleHud.ability). The slot reads the state packet's test-ability triple, which is "none" in play, so the relic the packet
     *  names (commanderActive, slot 0's relic id) fills it: the ticks it still runs (commanderFor, against its duration) while it runs, then its cooldown (commanderReady, against
     *  the relic's cooldown). An empty slot or a relic this build has no behaviour for (commanderReady below 0) leaves the slot empty. Client thread, after the packet's fields are read. */
    static void classAbility(){
        int[] s=relicShow(commanderActive,commanderReady,commanderFor);if(s==null)return;
        abilityId=commanderActive;abilityRemaining=s[0];abilityTotal=s[1];
    }
    /** A relic slot's shown ticks {left, total}: the run left against relicTuning.durationTicks while it runs, else the cooldown left against relics.cooldownTicks
     *  (at least what is left: quickened_relic only shortens it); null for an empty slot or one with no behaviour (ready below 0). */
    static int[] relicShow(String id,int ready,int running){
        var t=Commander.tuning(id);var r=Commander.relic(id);if(t==null||r==null||ready<0)return null;
        boolean on=running>0;int left=on?running:ready;return new int[]{left,Math.max(left,on?t.durationTicks():r.cooldownTicks())};
    }
    /** The ebb battle's magic troops by network id, gathered once a second on the client tick, so the screen cue does not walk every entity each frame. */
    static int[] ebbCasterIds=new int[0];
    static void ebbCasters(MinecraftClient c){
        var row=battleActive()&&c.world!=null?CampaignClient.marchRow():null;
        if(row==null||!row.ebb()){ebbCasterIds=new int[0];return;}
        if(c.world.getTime()%20!=0&&ebbCasterIds.length>0)return;
        var ids=new java.util.ArrayList<Integer>();for(var e:c.world.getEntities()){var t=TroopModels.troopOf(e);if(t!=null&&t.magic())ids.add(e.getId());}
        ebbCasterIds=ids.stream().mapToInt(Integer::intValue).toArray();
    }
    /** The glimpur ebb's screen cue (Kort, 2026-11-11): while a caster near the player gutters, the view dims cold, darkest at the top and bottom. */
    static float ebbScreenAlpha;
    static void ebbScreen(DrawContext g,MinecraftClient c){
        ebbScreenAlpha=0f;if(ebbCasterIds.length==0||c.world==null)return;
        var eye=c.gameRenderer.getCamera().getPos();long time=c.world.getTime();
        for(int id:ebbCasterIds){var e=c.world.getEntityById(id);if(e==null||!e.isAlive())continue;double d=e.getPos().distanceTo(eye);
            if(d<MarchMath.Ebb.SCREEN_RANGE)ebbScreenAlpha=Math.max(ebbScreenAlpha,MarchMath.Ebb.screen(MarchMath.Ebb.fade(true,id,time),d));}
        if(ebbScreenAlpha<=0f)return;
        int w=c.getWindow().getScaledWidth(),h=c.getWindow().getScaledHeight(),a=Math.round(ebbScreenAlpha*255),edge=Math.min(255,Math.round(a*1.8f)),tint=0x1A1626;
        g.fill(0,0,w,h,a<<24|tint);
        g.fillGradient(0,0,w,h/4,edge<<24|tint,tint);g.fillGradient(0,h-h/4,w,h,tint,edge<<24|tint);
    }
    /** The HUD: the camp line in camp, the entrance letterbox during a battle's entrance, else the Iron Ledger (BattleHud), with the
     *  contact accent over it; the ebb screen cue under all of it. */
    static void renderHud(DrawContext g,net.minecraft.client.render.RenderTickCounter tick){float delta=tick.getTickDelta(false);
        var c=MinecraftClient.getInstance();if(c.player==null||c.currentScreen instanceof ArmyScreen||Diagnostics.active()&&Diagnostics.hideHud()||c.options.hudHidden&&c.currentScreen==null){BattleHud.clear();return;} // 1.21.1 fires HudRenderCallback with F1 (hudHidden) on; 1.20.1 did not draw the mod HUD then
        ebbScreen(g,c);
        boolean ledger=battleActive()&&!CampaignClient.cinematic();
        if(ledger)BattleHud.render(g,delta);else BattleHud.clear();
        if(!battleActive())campSlips(g);else{campRectsDrawn.clear();campRowsDrawn.clear();}
        float scale=Math.min(1,c.getWindow().getScaledWidth()/640f);g.getMatrices().push();g.getMatrices().scale(scale,scale,1);
        int w=(int)(c.getWindow().getScaledWidth()/scale),h=(int)(c.getWindow().getScaledHeight()/scale);
        if(!battleActive())campHud(g,w,h);
        else if(!ledger)CampaignClient.hud(g,w,h);
        else Presentation.hud(g,w,h);
        g.getMatrices().pop();
    }
    static final class FantasyButton extends ButtonWidget {
        boolean primary;
        FantasyButton(int x,int y,int w,int h,Text text,PressAction action){super(x,y,w,h,text,action,DEFAULT_NARRATION_SUPPLIER);}
        @Override public void renderWidget(DrawContext g,int mx,int my,float delta){
            boolean selected=getMessage().getString().startsWith("> "),focus=active&&(isHovered()||isFocused());int x=getX(),y=getY();
            int surface=!active?0xff20272b:primary?(focus?0xffdfbd77:0xffbb9858):focus?0xff39464b:selected?0xff30474a:0xff252f34;
            g.fill(x,y,x+width,y+height,surface);
            g.drawBorder(x,y,width,height,!active?DarkUi.DISABLED_EDGE:focus||selected?DarkUi.GOLD:primary?0xffdfbd77:0xff455156);
            if(!active)DarkUi.disabledVeil(g,x,y,width,height);
            DarkUi.focus(g,this);
            if(selected)g.fill(x,y,x+3,y+height,DarkUi.GOLD);
            var font=MinecraftClient.getInstance().textRenderer;String label=getMessage().getString();if(selected)label=label.substring(2);
            var styled=font.trimToWidth(DarkUi.body(label),Math.max(1,width-14));g.drawText(font,net.minecraft.util.Language.getInstance().reorder(styled),x+(width-font.getWidth(styled))/2,y+(height-9)/2,!active?0xff879297:primary?0xff171d20:DarkUi.PAPER,false);
        }
    }
    public static boolean menuVisible(){return MinecraftClient.getInstance().currentScreen instanceof ArmyScreen;}
    abstract static class ArmyScreen extends Screen {
        ArmyScreen(String title){super(Text.literal(title));}
        @Override public boolean shouldPause(){return false;}
        @Override public void renderBackground(DrawContext g,int mx,int my,float delta){} // 1.21: Screen.render would blur and dim under the widgets; army screens draw their own backgrounds
        @Override public boolean keyPressed(int key,int scan,int mods){if(menu.matchesKey(key,scan)||key==GLFW.GLFW_KEY_ESCAPE){close();return true;}if(mapKey.matchesKey(key,scan)&&battleActive()){client.setScreen(new BattleMapScreen(this));return true;}return super.keyPressed(key,scan,mods);}
        ButtonWidget primary(String label,int x,int y,int w,java.util.function.Consumer<ButtonWidget> action){var b=new FantasyButton(x,y,w,26,Text.literal(label),action::accept);b.primary=true;return addDrawableChild(b);}
        ButtonWidget button(String label,int x,int y,int w,java.util.function.Consumer<ButtonWidget> action){return addDrawableChild(new FantasyButton(x,y,w,20,Text.literal(label),action::accept));}
    }
    public static final class BarracksScreen extends RecruitmentScreen { BarracksScreen(){super();} }
    static final class FailureScreen extends ArmyScreen {
        final String why;FailureScreen(String why){super("Graves and Glory");this.why=why;}
        @Override protected void init(){button(UiText.t("client.failure.return","Return to Minecraft"),width/2-100,height/2+40,200,b->client.setScreen(new TitleScreen()));}
        // Seen once: leaving by button, Esc or any other screen change clears the startup error so the next title goes to the front menu.
        @Override public void removed(){error="";}
        @Override public void renderBackground(DrawContext g,int mx,int my,float delta){} // 1.21: no blur or dimming (port lane B rule for every screen)
        @Override public void render(DrawContext g,int x,int y,float d){super.render(g,x,y,d);g.drawCenteredTextWithShadow(textRenderer,why,width/2,height/2,DarkUi.ALARM_TEXT);} // 1.21: super.render calls renderBackground first; never call it twice in a frame (a second blur throws)
    }
}



