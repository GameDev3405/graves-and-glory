package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.animation.Animation;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageScaling;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Isolated real-client checks for Phase 2 (Houses). GRAVES_PHASE2_CHECKS=<suite>[,<suite>...]. Never included in releases. */
public final class Phase2Checks implements ClientModInitializer {
    public interface Suite { void step(MinecraftClient c,int tick);
        /** Called once on the client thread when the suite ends by any path (done, timed out or thrown), before the next suite starts. */
        default void cleanup(MinecraftClient c){} }
    /** Optional for a suite: called on the server thread after every server tick, for checks that wait on the battle. */
    public interface ServerWatch { void serverTick(GravesAndGlory.Battle b); }
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{SUITES.put("banners",Banners::new);SUITES.put("engine",Engine::new);SUITES.put("spike",Spike::new);SUITES.put("gilded-art",GildedArt::new);SUITES.put("anim",Anim::new);}
    static final ArrayDeque<String> queue=new ArrayDeque<>();
    static volatile Suite current;static volatile String currentName="";static volatile boolean failed;
    static int tick,front;static boolean started;

    /** One evidence line (pass/fail id) plus a "[p2] id=result why" log line. */
    public static void report(String id,boolean pass,String why){if(!pass)failed=true;MeltyTest.report(id,pass,why);System.out.println("[p2] "+id+"="+pass+" "+why);}
    public static void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"p2-"+name+".png",c.getFramebuffer(),t->{});}
    /** Phase1Checks.server, with a throw reported as p2-<suite>-exception. */
    public static void server(MinecraftClient c,Consumer<GravesAndGlory.Battle> f){Phase1Checks.server(c,b->{try{f.accept(b);}catch(RuntimeException e){report("p2-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});}
    /** A 1v1 practice on the given map (Phase1Checks.skirmish) whose two armies then fight under the given banners: a botBanners id or a list such as "knight*12,archer*8"; null keeps the army's own. Returns false until the battle has started. */
    public static boolean skirmishBanners(GravesAndGlory.Battle b,ServerPlayerEntity p,int map,String banner0,String banner1){
        if(!Phase1Checks.skirmish(b,p,map,1,"normal"))return false;
        for(var s:new ArrayList<>(b.squads.values())){if(!s.seated)continue;String spec=s.team==0?banner0:banner1;if(spec!=null)field(b,s,banner(spec));}
        return true;
    }
    /** The army's troops leave, it takes this banner, and it forms again from it, as at the start of a round. */
    static void field(GravesAndGlory.Battle b,GravesAndGlory.Squad s,List<String> banner){
        for(var u:b.units)if(u.squad==s)u.mob.discard();b.units.removeIf(u->u.squad==s);
        s.training.clear();s.fullHealth.clear();s.banner=new ArrayList<>(banner);b.reinforce(s);
    }
    /** A banner from a botBanners id, or from "knight*12,archer*8": each part a troop id, ^ or empty, optionally times a count. */
    static List<String> banner(String spec){
        for(var row:Spec.BOTBANNERS)if(row.id().equals(spec))return new ArrayList<>(Arrays.asList(row.slots().split(",")));
        List<String> out=new ArrayList<>();
        for(String part:spec.split(",")){String[] p=part.trim().split("[*]");int n=p.length>1?Integer.parseInt(p[1].trim()):1;for(int i=0;i<n;i++)out.add(p[0].trim());}
        return out;
    }
    static byte[] fixture(String name)throws IOException{try(var in=Phase2Checks.class.getResourceAsStream("/fixtures/"+name)){if(in==null)throw new IOException("fixture "+name+" is not on the class path");return in.readAllBytes();}}
    static boolean houseFieldable(String house){for(var h:Spec.HOUSES)if(h.id().equals(house))return h.fieldable();return false;}
    /** Every troop of the row belongs to a fieldable House (worked out here from the sheets, not by Campaign). */
    static boolean fieldable(Spec.BotBannersRow row){for(String id:row.slots().split(",")){var t=Arrays.stream(Spec.TROOPS).filter(x->x.id().equals(id)).findFirst().orElse(null);if(t!=null&&!houseFieldable(t.house()))return false;}return true;}
    static long alive(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String id){return b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()&&u.spec.id().equals(id)).count();}

    /** Runs the suite's cleanup, reports p2-<suite>-suite, then stops the client once the last listed suite is done. */
    public static void finish(MinecraftClient c,String suite){
        var done=current;current=null;
        if(done!=null)try{done.cleanup(c);}catch(Exception e){report("p2-"+suite+"-exception",false,"cleanup: "+e);e.printStackTrace();}
        report("p2-"+suite+"-suite",!failed,failed?"an earlier p2 check failed":"all checks passed");
        if(queue.isEmpty())c.scheduleStop();
    }

    @Override public void onInitializeClient(){
        String list=System.getProperty("gravesandglory.phase2Checks","");if(list.isBlank())return;
        for(String name:list.split(","))if(!name.isBlank())queue.add(name.trim());
        Frame.register();RenderCost.register();
        ServerTickEvents.END_SERVER_TICK.register(s->{if(current instanceof ServerWatch w&&GravesAndGlory.battle!=null)try{w.serverTick(GravesAndGlory.battle);}catch(Exception e){report("p2-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            if(c.getOverlay()!=null)return;
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            if(current==null){
                if(queue.isEmpty())return;
                currentName=queue.poll();failed=false;tick=0;var make=SUITES.get(currentName);
                if(make==null){report("p2-"+currentName+"-suite",false,"unknown suite");if(queue.isEmpty())c.scheduleStop();return;}
                current=make.get();
            }
            try{current.step(c,++tick);}catch(Exception e){report("p2-"+currentName+"-exception",false,e.toString());e.printStackTrace();finish(c,currentName);}
        });
    }

    /**
     * Task 2: a v1 profile migrates on load, banners are edited through the real command packets, the side colours and the bot
     * banners of a battle, and the barracks training limit of a banner. Stages run on the server tick; the client sends the
     * commands, opens the screens and takes the screenshots the stages ask for.
     */
    static final class Banners implements Suite,ServerWatch {
        volatile int stage;volatile UUID player;volatile String send,wantShot,wantScreen;volatile boolean shotTaken,finished;
        long stageAt;int shotDelay,screenWait;boolean rosterDone;
        List<String> before;boolean editOk,sidesSaved,sideIds;String editWhy="",sidesWhy="";
        GravesAndGlory.Unit striker;int flashesBefore;
        GravesAndGlory.Squad own;int goldBefore,earnedBefore;boolean trainOk=true;String trainWhy="";
        final Map<GravesAndGlory.Squad,List<String>> botRows=new HashMap<>();String foreign;int queuedBefore;boolean takeOk;String takeWhy="";
        volatile String sideId;boolean editorDone;
        /** Presses the editor's House button with this label (the selected one reads "> label"); false when there is none. */
        static boolean houseTab(CampaignLobbyScreen l,String label){
            for(var e:List.copyOf(l.children()))if(e instanceof net.minecraft.client.gui.widget.ButtonWidget bw){String m=bw.getMessage().getString();if(m.equals(label)||m.equals("> "+label)){bw.onPress();return true;}}
            return false;
        }
        /** The dye on the chest of a troop's portrait (ArmyCards.portrait, as the barracks and the editor draw it), or -1 without a dyed chest. */
        static int chestColour(String id){var e=ArmyCards.portrait(Campaign.troop(id));if(e==null)return -1;var chest=e.getEquippedStack(EquipmentSlot.CHEST);return chest.getItem() instanceof DyeableItem d?d.getColor(chest):-1;}
        /** Gold the commander's army spent since mark(): battle income (passive gold every economy.incomeSeconds) is not spending. */
        int spent(){return goldBefore+(own.stats.goldEarned-earnedBefore)-own.gold;}
        void mark(){goldBefore=own.gold;earnedBefore=own.stats.goldEarned;}

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        static List<String> ids(com.google.gson.JsonArray a){List<String> out=new ArrayList<>();for(var e:a)out.add(e.getAsString());return out;}
        /** What the camp snapshot shows this commander in the lobby: the side ids, then each commander row as name=team, in list order. */
        static String lobbyView(Campaign camp,ServerPlayerEntity p){
            var j=com.google.gson.JsonParser.parseString(camp.snapshot(p)).getAsJsonObject();if(!j.has("players")||!j.has("sides"))return "no snapshot";
            StringBuilder rows=new StringBuilder();for(var e:j.getAsJsonArray("players")){var r=e.getAsJsonObject();if(rows.length()>0)rows.append(", ");rows.append(r.get("name").getAsString()).append('=').append(r.get("team").getAsInt());}
            return ids(j.getAsJsonArray("sides"))+" / "+rows;
        }
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Profile profile(GravesAndGlory.Battle b){return b.campaign.profile(player);}
        String notice(GravesAndGlory.Battle b){return b.campaign.notices.getOrDefault(player,"");}
        void requestShot(String name){shotTaken=false;shotDelay=0;wantShot=name;}
        void command(GravesAndGlory.Battle b,String cmd,int nextStage){send=cmd;next(b,nextStage);}
        public void cleanup(MinecraftClient c){server(c,b->{if(b.running)b.campaign.returnLobby();});}

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            String cmd=send;if(cmd!=null){send=null;GravesAndGloryClient.send(cmd);System.out.println("[p2] sent "+cmd);}
            String screen=wantScreen;if(screen!=null){wantScreen=null;c.setScreen(screen.equals("editor")?new GravesAndGloryClient.LobbyScreen(1):null);}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=12){shot(c,wantShot);shotTaken=true;}
            if(stage==14){ // the barracks lists the ids of the army's own banner, once each, and its portraits wear the side the team flies
                int team=GravesAndGloryClient.team;boolean flown=sideId!=null&&(team==0||team==1)&&sideId.equals(CampaignClient.sides[team]);
                if(!rosterDone&&c.currentScreen instanceof RecruitmentScreen r&&(flown||++screenWait>200)){rosterDone=true;
                    List<String> ids=new ArrayList<>();for(var row:r.controls)ids.add(Spec.TROOPS[row.index()].id());
                    report("p2-barracks-roster",ids.equals(List.of("knight","archer")),"barracks lists "+ids+" for the banner knight x12, archer x8 (want knight, archer)");
                    var side=DarkUi.side(team);String asked=CampaignClient.profile.bannerColour;int worn=chestColour("archer");
                    report("p2-barracks-colour",flown&&GravesAndGloryClient.battleActive()&&side!=null&&!side.id().equals(asked)&&worn==side.color(),
                        "in battle team "+team+" flies "+(side==null?"none":side.id())+" (the server's "+sideId+" arrived="+flown+") against the request "+asked+"; the barracks archer portrait wears "+worn+" ("+(side==null?"none":side.id()+" is "+side.color())+")");
                    requestShot("barracks-banner");
                }else if(!rosterDone&&!(c.currentScreen instanceof RecruitmentScreen)&&++screenWait>200){rosterDone=true;report("p2-barracks-roster",false,"the barracks screen never opened");report("p2-barracks-colour",false,"the barracks screen never opened");requestShot("barracks-banner");}
                if(rosterDone&&wantShot!=null&&shotTaken){wantShot=null;c.setScreen(null);server(c,b->next(b,15));}
            }
            if(stage==6&&!editorDone&&c.currentScreen instanceof CampaignLobbyScreen l&&l.tab==1){editorDone=true;
                // p2-editor-colour: outside battle a portrait wears the colour the commander asks for, even while the sides the client holds name another
                String asked=CampaignClient.profile.bannerColour;String[] held=CampaignClient.sides;
                String other=Arrays.stream(Spec.FACTIONS).map(Spec.FactionsRow::id).filter(id->!id.equals(asked)).findFirst().orElse(asked);
                CampaignClient.sides=new String[]{other,other};int worn=chestColour("archer");CampaignClient.sides=held;
                int want=Spec.FACTIONS[Math.max(0,Campaign.sideIndex(asked))].color();
                report("p2-editor-colour",!GravesAndGloryClient.battleActive()&&!other.equals(asked)&&worn==want,"in the lobby (battle active "+GravesAndGloryClient.battleActive()+") with both sides held as "+other+", the editor archer portrait wears "+worn+"; the request "+asked+" is "+want);
                // p2-editor-house: a picked troop stays while its own House is shown and is let go when the tab changes to another House; the clear tool stays
                String house0=l.house,unit0=l.selectedUnit;
                l.selectedUnit="knight";boolean pressed=houseTab(l,"Gilded");boolean kept=l.house.equals("gilded")&&l.selectedUnit.equals("knight");
                pressed&=houseTab(l,"Graveborn");boolean dropped=l.house.equals("graveborn")&&l.selectedUnit.isEmpty();String line=l.pickLine();
                l.selectedUnit=RosterMath.EMPTY;pressed&=houseTab(l,"Gilded");boolean clearKept=l.house.equals("gilded")&&l.selectedUnit.equals(RosterMath.EMPTY);
                // p2-editor-copy: whole training seconds without a decimal, rarities as words with a capital, and a Legendary's slot word not cut to LEGEND
                var font=c.textRenderer;int room=l.slots.isEmpty()?47:l.slots.get(0).getWidth()-4;
                String knightTime=RecruitmentScreen.trainingTime(Campaign.troop("knight")),hunterTime=RecruitmentScreen.trainingTime(Campaign.troop("wm_hunter"));
                l.selectedUnit="knight";String squire=l.pickLine();l.selectedUnit="mage";String sunweaver=l.pickLine();
                String tip=ArmyCards.Recruit.describe(Campaign.troop("knight"));
                String legend=ArmyCards.Deployment.slotLabel(Campaign.troop("gl_marshal"),font,room),epic=ArmyCards.Deployment.slotLabel(Campaign.troop("mage"),font,room),inf=ArmyCards.Deployment.slotLabel(Campaign.troop("knight"),font,room);
                boolean legendOk=(font.getWidth("LEGENDARY")<=room?legend.equals("LEGENDARY"):legend.equals("LEGEND."))&&font.getWidth(legend)<=room;
                boolean copy=knightTime.equals("Training time: 4 seconds")&&hunterTime.equals("Training time: 3.5 seconds")&&squire.equals("Order Squire / Common")&&sunweaver.equals("Sunweaver / Epic / fills 2 slots")&&tip.startsWith("Order Squire / Gilded Order Common / Infantry")&&legendOk&&epic.equals("EPIC")&&inf.equals("INF");
                report("p2-editor-copy",copy,"\""+knightTime+"\", \""+hunterTime+"\"; editor lines \""+squire+"\", \""+sunweaver+"\"; tooltip \""+tip+"\"; slot words "+inf+", "+epic+", "+legend+" (LEGENDARY is "+font.getWidth("LEGENDARY")+" px, the slot has "+room+")");
                l.house=house0;l.selectedUnit=unit0;l.showTab(1);
                report("p2-editor-house",pressed&&kept&&dropped&&line.equals("No troop picked.")&&clearKept,"House buttons pressed="+pressed+"; knight kept on the Gilded tab="+kept+"; let go on the Graveborn tab="+dropped+" (line \""+line+"\"); the clear tool kept on a tab change="+clearKept);
            }
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"banners");}
            if(tick>9000&&!finished){finished=true;report("p2-banners-timeout",false,"stuck in stage "+stage);finish(c,"banners");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            switch(stage){
                case 1->{ // p2-migrate-live: a v1 file on disk upgrades when the profile is first read
                    var camp=b.campaign;if(sp(b)==null)return;
                    if(b.running)camp.returnLobby();
                    try{
                        byte[] fixture=fixture("profile-v1-partial.json");
                        Files.createDirectories(camp.directory);
                        Path file=camp.directory.resolve(player+".json"),backup=camp.directory.resolve(player+".v1.json");
                        Files.deleteIfExists(backup);Files.write(file,fixture);camp.profiles.remove(player);
                        Profile pr=camp.profile(player);
                        Set<String> commons=new LinkedHashSet<>();for(var t:Spec.TROOPS)if(t.rarity().equals("common"))commons.add(t.id());
                        Set<String> want=new LinkedHashSet<>(commons);want.add("sentinel");
                        int refund=Spec.ECONOMY[0].refundGold(),gold=420+3*refund; // the v1 starters mage and hexer, and the unlocked wraith
                        boolean owned=commons.size()==10&&pr.owned.keySet().equals(want);
                        boolean refunded=Collections.disjoint(pr.owned.keySet(),Set.of("mage","hexer","oracle","wraith"))&&pr.gold==gold;
                        boolean warden=pr.commander.equals("warden");
                        boolean kept=Files.exists(backup)&&Arrays.equals(Files.readAllBytes(backup),fixture);
                        boolean v2=Files.readString(file).contains("\"schema\": 2");
                        var cat=RosterMath.Catalog.fromSpec();
                        boolean line=RosterMath.validBanner(pr.banner,cat,pr.owned.keySet())==null&&pr.banner.equals(banner("knight*12,archer*6,empty*2"));
                        report("p2-migrate-live",owned&&refunded&&warden&&kept&&v2&&line,"owned "+pr.owned.keySet()+" is the 10 Commons and sentinel="+owned+"; mage, hexer, oracle and wraith not owned and gold "+pr.gold+" = 420 + 3 x "+refund+"="+refunded
                            +"; commander "+pr.commander+"; "+backup.getFileName()+" equals the fixture="+kept+"; "+file.getFileName()+" rewritten as schema 2="+v2+"; banner "+String.join(",",pr.banner)+" is valid and the Dawn line without its refunded mages="+line);
                    }catch(IOException e){report("p2-migrate-live",false,e.toString());}
                    next(b,2);
                }
                case 2->{ // p2-banner-edit: the Sunweaver is granted (the migration refunded it), then placed in the last slot
                    var pr=profile(b);pr.owned.putIfAbsent("mage",new Profile.Owned());b.campaign.save(player);
                    before=new ArrayList<>(pr.banner);command(b,"slot:19:mage",3);
                }
                case 3->{if(since(b)>=20){
                    var pr=profile(b);var cat=RosterMath.Catalog.fromSpec();
                    List<String> edit=new ArrayList<>(before);edit.set(19,"mage");
                    boolean valid=RosterMath.validBanner(pr.banner,cat,pr.owned.keySet())==null,repacked=pr.banner.equals(RosterMath.repack(edit,cat,pr.owned.keySet()))&&pr.banner.contains("mage");
                    editOk=valid&&repacked;editWhy="slot:19:mage -> "+String.join(",",pr.banner)+" (valid="+valid+", the repacked edit="+repacked+")";
                    before=new ArrayList<>(pr.banner);command(b,"slot:0:gl_marshal",4);
                }}
                case 4->{if(since(b)>=20){
                    var pr=profile(b);boolean same=pr.banner.equals(before)&&!pr.owned.containsKey("gl_marshal");
                    editOk&=same;editWhy+="; slot:0:gl_marshal (not owned) left the banner unchanged="+same+", notice: "+notice(b);
                    report("p2-banner-edit",editOk,editWhy);
                    command(b,"slot:2:wm_hound",5);
                }}
                case 5->{if(since(b)>=20){ // ruling 2: an owned troop of an unfieldable House stays out of the banner
                    var pr=profile(b);boolean same=pr.banner.equals(before),owned=pr.owned.containsKey("wm_hound"),said=notice(b).contains("cannot be fielded");
                    report("p2-banner-unfieldable",same&&owned&&said,"slot:2:wm_hound (owned, House wildmoot not fieldable) left the banner unchanged="+same+", owned="+owned+", notice: "+notice(b));
                    wantScreen="editor";requestShot("banner-editor");next(b,6);
                }}
                case 6->{if(shotTaken){wantShot=null;wantScreen="none";command(b,"colour:crimson",7);}}
                case 7->{ // p2-sides: the commander's colour, the bot's free side, and the flash of his troop's blow
                    if(since(b)<20)return;
                    if(!sidesSaved){sidesSaved=true;sidesWhy="colour:crimson saved="+profile(b).bannerColour.equals("crimson");}
                    var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,b.campaign.map,1,"normal"))return;
                    var mine=b.squads.get(player);var bot=b.squads.values().stream().filter(s->s.bot&&s.seated).findFirst().orElse(null);
                    sideIds=b.side(mine.team).id().equals("crimson")&&bot!=null&&b.side(bot.team).id().equals("gold");
                    sidesWhy+="; sides "+b.side(0).id()+" / "+b.side(1).id()+" (commander on team "+mine.team+")";
                    Projectiles.clear(b);for(var u:b.units)Phase1Checks.freeze(u);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    striker=b.units.stream().filter(u->u.squad==mine&&u.spec.range()<3&&u.mob.isAlive()).findFirst().orElse(null);
                    if(striker==null){report("p2-sides",false,sidesWhy+"; no melee troop to strike");next(b,9);return;}
                    // The army holds on the striker, so a foe beside it is inside the hold leash (orders.engageRadius); the foe is frozen in an ad hoc hold squad of the other team, so it stays put.
                    Vec3d at=striker.mob.getPos();mine.assignments.clear();mine.order="hold";mine.target=at;striker.mob.refreshPositionAndAngles(at.x,at.y,at.z,-90,0);
                    var foes=new GravesAndGlory.Squad(UUID.randomUUID(),1-mine.team,at.add(1.5,0,0));foes.order="hold";foes.yaw=90;
                    var foe=b.spawn(foes,Campaign.troop(mine.team==0?"revenant":"knight"),30,at.add(1.5,0,0));Phase1Checks.freeze(foe);foe.mob.refreshPositionAndAngles(at.x+1.5,at.y,at.z,90,0);foe.deployed=foe.mob.getPos();
                    striker.nextAttack=0;flashesBefore=BattleEffects.flashes;next(b,8);
                }
                case 8->{
                    if(BattleEffects.flashes>flashesBefore){Phase1Checks.freeze(striker);boolean colour=BattleEffects.lastFlashColor==14701130;
                        report("p2-sides",sidesWhy.startsWith("colour:crimson saved=true")&&sideIds&&colour,sidesWhy+"; "+striker.spec.id()+" melee flash colour "+BattleEffects.lastFlashColor+" (crimson is 14701130)");
                        requestShot("sides");next(b,9);}
                    else if(since(b)>400){report("p2-sides",false,sidesWhy+"; the commander's troop never struck");next(b,9);}
                }
                case 9->{if(wantShot==null||shotTaken){wantShot=null;b.campaign.returnLobby();next(b,10);}}
                case 10->{ // p2-bot-banners: a Normal 3v3 with bot fill
                    var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,b.campaign.map,3,"normal"))return;
                    List<Spec.BotBannersRow> rows=new ArrayList<>();for(var r:Spec.BOTBANNERS)if(r.difficulty().equals("normal")&&fieldable(r))rows.add(r);
                    boolean ok=!rows.isEmpty();int bots=0;StringBuilder why=new StringBuilder();
                    for(var s:b.squads.values())if(s.bot&&s.seated){bots++;
                        var row=rows.stream().filter(r->Arrays.asList(r.slots().split(",")).equals(s.banner)).findFirst().orElse(null);
                        boolean lane=row!=null&&row==rows.get(s.lane%rows.size());
                        boolean units=true;int listed=0;
                        for(int i=0;i<s.banner.size();i++){final int slot=i;String id=s.banner.get(i);
                            var at=b.units.stream().filter(u->u.squad==s&&u.slot==slot&&u.mob.isAlive()).toList();
                            if(id.equals(RosterMath.MARKER)||id.equals(RosterMath.EMPTY))units&=at.isEmpty();
                            else{listed++;units&=at.size()==1&&at.get(0).spec.id().equals(id);}}
                        long fielded=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).count();units&=fielded==listed;
                        ok&=row!=null&&row.id().endsWith("_normal")&&lane&&units;
                        why.append(" team ").append(s.team).append(" lane ").append(s.lane).append(": ").append(row==null?"no fieldable Normal row":row.id()).append(lane?"":" (not the lane's row)").append(", ").append(fielded).append(" troops for ").append(listed).append(" listed").append(units?"":" (MISMATCH)").append(';');
                    }
                    ok&=bots==5;
                    report("p2-bot-banners",ok,bots+" bot armies; fieldable Normal rows "+rows.stream().map(Spec.BotBannersRow::id).toList()+";"+why);
                    b.campaign.returnLobby();next(b,11);
                }
                case 11->{ // p2-train-limit: the banner knight x12, archer x8 with all twelve knights alive
                    var p=sp(b);if(p==null||!skirmishBanners(b,p,b.campaign.map,"knight*12,archer*8","graveborn_normal"))return;
                    own=b.squads.get(player);for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    var f=Spec.FACTIONS[own.team];b.relocate(p,f.barracksX()+.5,Arena.ground(f.barracksX(),f.barracksZ())+1,f.barracksZ()+.5,0,0);
                    own.gold=1000;mark();
                    boolean fixture=own.banner.equals(banner("knight*12,archer*8"))&&alive(b,own,"knight")==12&&b.atBarracks(p,own);
                    trainOk=fixture;trainWhy="fixture banner "+String.join(",",own.banner)+", knights alive "+alive(b,own,"knight")+", at the barracks "+b.atBarracks(p,own);
                    command(b,"train:knight",12);
                }
                case 12->{if(since(b)>=20){boolean refused=own.training.isEmpty()&&spent()==0;trainOk&=refused;trainWhy+="; train:knight with 12 of 12 alive refused="+refused+" (queued "+own.training.size()+", spent "+spent()+")";mark();command(b,"train:sentinel",13);}}
                case 13->{if(since(b)>=20){boolean refused=own.training.isEmpty()&&spent()==0;trainOk&=refused;trainWhy+="; train:sentinel (not in the banner) refused="+refused+" (queued "+own.training.size()+", spent "+spent()+")";
                    // p2-barracks-colour: his team now flies a side other than his request, as when the other team or a teammate claimed that colour first
                    int asked=Campaign.sideIndex(profile(b).bannerColour),fly=-1;for(int row=0;row<Spec.FACTIONS.length&&fly<0;row++)if(row!=asked&&row!=b.sides[1-own.team])fly=row;
                    b.sides[own.team]=fly;sideId=Spec.FACTIONS[fly].id();
                    command(b,"barracks",14);}}
                case 15->command(b,"disband:knight",16);
                case 16->{if(since(b)>=20){long knights=alive(b,own,"knight");trainWhy+="; after disband:knight "+knights+" alive";mark();command(b,"train:knight",17);}}
                case 17->{if(since(b)>=20){var knight=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("knight")).findFirst().orElseThrow();
                    boolean taken=own.training.size()==1&&own.training.getFirst().spec()==knight&&spent()==knight.cost();trainOk&=taken;
                    trainWhy+="; then train:knight accepted="+taken+" (queued "+own.training.size()+", spent "+spent()+")";mark();command(b,"train:knight",18);}}
                case 18->{if(since(b)>=20){boolean refused=own.training.size()==1&&spent()==0;trainOk&=refused;trainWhy+="; a second train:knight (11 alive + 1 queued) refused="+refused+" (queued "+own.training.size()+", spent "+spent()+")";
                    report("p2-train-limit",trainOk,trainWhy);b.campaign.returnLobby();next(b,19);}}
                case 19->{ // p2-takeover: in a running Normal 3v3 with bot fill, a commander with no seat of his own joins and takes a bot's seat
                    var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,b.campaign.map,3,"normal"))return;
                    var mine=b.squads.get(player);for(var u:b.units)if(u.squad==mine)u.mob.discard();b.units.removeIf(u->u.squad==mine);
                    b.squads.remove(player);b.welcomed.remove(player);b.campaign.vacated.remove(player); /* no seat and no reservation: the next battle tick calls join(p), which hands him the first bot seat */
                    botRows.clear();for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot&&s.seated)botRows.put(s,new ArrayList<>(s.banner));}
                    next(b,20);
                }
                case 20->{
                    var s=b.squads.get(player);
                    if(s==null){if(since(b)>100){report("p2-takeover",false,"the commander never rejoined");b.campaign.returnLobby();next(b,23);}return;}
                    own=s;var row=botRows.get(s);var pr=profile(b);
                    List<String> shown=new ArrayList<>();for(var e:b.campaign.bannerJson(player))shown.add(e.getAsString());
                    boolean seat=row!=null&&!s.bot&&s.seated,mine=s.banner.equals(b.campaign.banner(s)),snapshot=shown.equals(pr.banner),commander=s.commander.equals(pr.commander);
                    foreign=row==null?null:row.stream().filter(id->Campaign.troop(id)!=null&&!s.banner.contains(id)).findFirst().orElse(null);
                    takeOk=seat&&mine&&snapshot&&commander;
                    takeWhy="took a bot seat="+seat+" (team "+s.team+", lane "+s.lane+", the bot's banner "+(row==null?"none":String.join(",",row))+"); his banner is campaign.banner(s)="+mine
                        +"; the snapshot banner is his profile banner="+snapshot+"; commander "+s.commander+" from his profile="+commander;
                    if(foreign==null){report("p2-takeover",false,takeWhy+"; every troop of the bot's banner is also in his");b.campaign.returnLobby();next(b,23);return;}
                    // Room for one more unit: the inherited army loses troops that are not knights, as casualties would.
                    int weight=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).mapToInt(u->u.spec.slotWeight()).sum()+s.training.stream().mapToInt(t->t.spec().slotWeight()).sum();
                    for(var u:new ArrayList<>(b.units))if(weight>GravesAndGlory.R.squadSize()-2&&u.squad==s&&u.mob.isAlive()&&!u.spec.id().equals("knight")){u.mob.discard();b.units.remove(u);weight-=u.spec.slotWeight();}
                    var f=Spec.FACTIONS[s.team];b.relocate(sp(b),f.barracksX()+.5,Arena.ground(f.barracksX(),f.barracksZ())+1,f.barracksZ()+.5,0,0);
                    s.gold=1000;mark();queuedBefore=s.training.size();
                    takeWhy+="; at the barracks "+b.atBarracks(sp(b),s);
                    command(b,"train:"+foreign,21);
                }
                case 21->{if(since(b)>=20){boolean refused=own.training.size()==queuedBefore&&spent()==0;takeOk&=refused;
                    takeWhy+="; train:"+foreign+" (in the bot's banner, not in his) refused="+refused+" (queued "+(own.training.size()-queuedBefore)+", spent "+spent()+")";
                    mark();queuedBefore=own.training.size();command(b,"train:knight",22);}}
                case 22->{if(since(b)>=20){var knight=Campaign.troop("knight");
                    boolean taken=own.training.size()==queuedBefore+1&&own.training.getLast().spec()==knight&&spent()==knight.cost();takeOk&=taken;
                    takeWhy+="; train:knight ("+alive(b,own,"knight")+" alive, "+Collections.frequency(own.banner,"knight")+" in his banner) accepted="+taken+" (queued "+(own.training.size()-queuedBefore)+", spent "+spent()+")";
                    report("p2-takeover",takeOk,takeWhy);b.campaign.returnLobby();next(b,23);}}
                case 23->{ // p2-side-preview: in the lobby the snapshot's sides and commander rows come from the seats his next round would have, so an unseated commander ahead of him does not set them
                    var camp=b.campaign;var me=sp(b);if(me==null)return;if(b.running){camp.returnLobby();return;}
                    String asked=profile(b).bannerColour,name=me.getGameProfile().getName();String alone=lobbyView(camp,me);
                    // a commander who joined first, unseated on team 0 and asking for teal: the old preview, read from every squad in the lobby, gave team 0 his colour
                    UUID first=UUID.randomUUID();var ahead=new GravesAndGlory.Squad(first,0,GravesAndGlory.Battle.base(0));ahead.seated=false;
                    var rest=new LinkedHashMap<>(b.squads);b.squads.clear();b.squads.put(first,ahead);b.squads.putAll(rest);camp.profile(first).bannerColour="teal";
                    String behind=lobbyView(camp,me);
                    camp.onPad.put(player,1);String pad=lobbyView(camp,me);camp.onPad.remove(player);
                    b.squads.remove(first);camp.profiles.remove(first);
                    boolean ok=asked.equals("crimson")&&alone.equals("[crimson, gold] / "+name+"=0")&&behind.equals("[crimson, gold] / Commander=-1, "+name+"=0")&&pad.equals("[gold, crimson] / Commander=-1, "+name+"=1");
                    report("p2-side-preview",ok,"request "+asked+"; the lobby snapshot (sides / rows name=team) alone "+alone+" (a solo battle seats him on team 0); with an unseated teal commander inserted first on team 0 "+behind+" (not in his round: -1); on a team-1 pad "+pad);
                    next(b,99);
                }
                default->{}
            }
        }
    }

    /**
     * Task 3: the combat engine. ArmyMobs and their daylight and water rules, the troop damage type with Combat.resolve (counter rows and the
     * shield bypass), the abilities shell (mend, blight), the House strip of the HUD, the House roll, the recruit page's House tabs and the tick
     * cost of eight armies (160 troops). Stages run on the server tick; the client sends the command, opens the screens and takes the screenshots the stages ask for.
     */
    static final class Engine implements Suite,ServerWatch {
        record Pin(GravesAndGlory.Unit u,Vec3d at,float yaw){}
        volatile int stage;volatile UUID player;volatile String send,wantShot;volatile boolean shotTaken,finished;
        long stageAt;int shotDelay;
        // daylight and water: a stone platform and a sealed tank in the open sky
        BlockPos site;final List<MobEntity> army=new ArrayList<>();MobEntity vanillaSkeleton,vanillaWither,armyHusk,vanillaHusk;
        boolean armyBurned,controlBurned,controlWitherBurned;int submerged;
        // the HUD strip: the client drives stages 5 and 6 and the server reports at 7
        int hudState,hudWait;volatile boolean hudOk;volatile String hudWhy="";
        // the combat fixture on a sky platform above Dawn's base
        GravesAndGlory.Squad dawn,dusk;GravesAndGlory.Unit knight,bowFront,bowBack,melee,hitter,victim,mage,ally,caster,tA,tB,tC;
        final List<GravesAndGlory.Unit> fixture=new ArrayList<>(),shielded=new ArrayList<>();final List<Pin> pins=new ArrayList<>();
        boolean raise;Combat.HitLog mark,front,back,lowered,meleeFront,meleeBack,tactics;float hp0,frontLost,backLost,loweredLost,tacticsLost;DamageSource frontSource,tacticsSource;
        float healBase,healed;long healAt=-1,spellSeen=-1,burstAt=-1;String burstWhy="";boolean burstOk;
        final Set<net.minecraft.util.math.ChunkPos> forced=new HashSet<>();
        // the roll, the recruit page and the tick cost
        java.util.random.RandomGenerator rngSaved;volatile int expectGold;int rt,rtWait,ri;boolean rtOk=true,rtDone;final StringBuilder rtWhy=new StringBuilder();TickCost cost;

        static List<String> goalNames(MobEntity m,String field)throws ReflectiveOperationException{
            var f=MobEntity.class.getDeclaredField(field);f.setAccessible(true);List<String> out=new ArrayList<>();
            for(var g:((net.minecraft.entity.ai.goal.GoalSelector)f.get(m)).getGoals())out.add(g.getGoal().getClass().getSimpleName());return out;
        }
        /** A draw whose every nextInt(bound) is value mod bound, so a roll lands on a chosen troop. */
        static java.util.random.RandomGenerator fixed(int value){return new java.util.random.RandomGenerator(){public long nextLong(){return value;}public int nextInt(int bound){return value%bound;}};}
        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Profile profile(GravesAndGlory.Battle b){return b.campaign.profile(player);}
        void requestShot(String name){shotTaken=false;shotDelay=0;wantShot=name;}
        void command(GravesAndGlory.Battle b,String cmd,int nextStage){send=cmd;next(b,nextStage);}
        /** A stone floor under the standing layer at c (half blocks each way) and clear air above it. */
        void platform(GravesAndGlory.Battle b,BlockPos c,int half){
            for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){b.world.setBlockState(c.add(x,-1,z),Blocks.STONE.getDefaultState());for(int y=0;y<=6;y++)b.world.setBlockState(c.add(x,y,z),Blocks.AIR.getDefaultState());}
        }
        void force(GravesAndGlory.Battle b,BlockPos from,BlockPos to){
            for(int cx=from.getX()>>4;cx<=to.getX()>>4;cx++)for(int cz=from.getZ()>>4;cz<=to.getZ()>>4;cz++){b.world.setChunkForced(cx,cz,true);b.world.getChunk(cx,cz);forced.add(new net.minecraft.util.math.ChunkPos(cx,cz));}
        }
        void unforce(GravesAndGlory.Battle b){for(var p:forced)b.world.setChunkForced(p.x,p.z,false);forced.clear();}
        /** A troop on the platform at (dx, dz) from its centre, held there each tick by the pins; a frozen troop never attacks or casts. */
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw,boolean freeze){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);
            var u=b.spawn(s,Campaign.troop(troop),30+fixture.size(),at);u.mob.setAiDisabled(true);if(freeze)Phase1Checks.freeze(u);
            fixture.add(u);pins.add(new Pin(u,at,yaw));u.deployed=at;apply(u,at,yaw);return u;
        }
        void move(GravesAndGlory.Unit u,double dx,double dz,float yaw){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);pins.removeIf(p->p.u()==u);pins.add(new Pin(u,at,yaw));u.deployed=at;apply(u,at,yaw);
        }
        static void apply(GravesAndGlory.Unit u,Vec3d at,float yaw){u.mob.refreshPositionAndAngles(at.x,at.y,at.z,yaw,0);u.mob.setHeadYaw(yaw);u.mob.setBodyYaw(yaw);u.mob.setVelocity(Vec3d.ZERO);}
        void clearFixture(){for(var u:fixture)u.mob.discard();fixture.clear();shielded.clear();pins.clear();}
        /** After each server tick: the fixture troops stand where the stage put them, and the troops in `shielded` hold their shields as `raise` says. */
        void hold(){
            for(var p:pins)if(p.u().mob.isAlive())apply(p.u(),p.at(),p.yaw());
            for(var u:shielded)if(u.mob.isAlive()){if(raise&&!u.mob.isUsingItem())u.mob.setCurrentHand(net.minecraft.util.Hand.OFF_HAND);else if(!raise&&u.mob.isUsingItem())u.mob.stopUsingItem();}
        }
        static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}
        static String show(Combat.HitLog h){return h==null?"no hit":"counters "+h.counters()+" rows "+h.rows()+" base "+h.base()+" traits "+h.traits()+" auras "+h.auras()+" commander "+h.commander()+" total "+h.total();}
        /** The newest logged hit on this troop, or null. */
        static Combat.HitLog hitOn(GravesAndGlory.Unit target){Combat.HitLog out=null;for(var h:Combat.LAST)if(h.target()==target.mob.getId())out=h;return out;}
        /** The troops a House shows as owned after the v1 fixture migrated: its Commons, plus the unlocked Templar. */
        static Set<String> expected(String house){
            Set<String> out=new LinkedHashSet<>();for(var t:Spec.TROOPS)if(t.house().equals(house)&&t.rarity().equals("common"))out.add(t.id());if(house.equals("gilded"))out.add("sentinel");return out;
        }
        public void cleanup(MinecraftClient c){
            GravesAndGloryClient.houseStripOverride=null;
            RenderCost.on=false;server(c,b->{clearFixture();unforce(b);if(cost!=null)cost.handBack();if(rngSaved!=null)b.campaign.rollRng=rngSaved;if(b.running)b.campaign.returnLobby();});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            String cmd=send;if(cmd!=null){send=null;GravesAndGloryClient.send(cmd);System.out.println("[p2] sent "+cmd);}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=12){shot(c,wantShot);shotTaken=true;}
            if(stage==5)hud(c);
            if(stage==6)hudHouses(c);
            if(stage==51)recruit(c);
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"engine");}
            if(tick>14000&&!finished){finished=true;report("p2-engine-timeout",false,"stuck in stage "+stage);finish(c,"engine");}
        }

        /** Stage 5: the strip shows the starter banner's House at tier III (screenshot engine-hud). */
        void hud(MinecraftClient c){
            hudWait++;var h=GravesAndGloryClient.houses;
            boolean shown=GravesAndGloryClient.battleActive()&&c.currentScreen==null&&h.getOrDefault("gilded",0)==3;
            if(hudState==0&&(shown&&hudWait>=30||hudWait>400)){
                hudState=1;StringBuilder files=new StringBuilder();boolean all=true;
                for(var row:Spec.HOUSES){boolean there=c.getResourceManager().getResource(new Identifier(row.glyph())).isPresent();all&=there;files.append(row.id()).append(there?" ok ":" MISSING ");}
                hudOk=shown&&all;hudWhy="the client's strip holds "+h+" (in battle, no screen, gilded III="+shown+"); the five glyph files: "+files.toString().trim();requestShot("engine-hud");
            }
            if(hudState==1&&wantShot!=null&&shotTaken){wantShot=null;hudState=2;server(c,b->next(b,6));}
        }
        /** Stage 6: the strip holds all five Houses at once (a held override), so the screenshot engine-hud-houses shows every icon. */
        void hudHouses(MinecraftClient c){
            if(hudState==2){hudState=3;var all=new LinkedHashMap<String,Integer>();all.put("gilded",3);all.put("graveborn",2);all.put("wildmoot",1);all.put("ironclad",3);all.put("tidecallers",2);GravesAndGloryClient.houseStripOverride=all;requestShot("engine-hud-houses");}
            if(hudState==3&&wantShot!=null&&shotTaken){wantShot=null;hudState=4;GravesAndGloryClient.houseStripOverride=null;server(c,b->next(b,7));}
        }
        /** Stage 51: the camp page `recruit` with its five House tabs, on the profile the v1 fixture migrated to; every tab is screenshotted (the brief names the first three). */
        void recruit(MinecraftClient c){
            var p=CampaignClient.profile;var houses=List.of("gilded","graveborn","wildmoot","ironclad","tidecallers");
            if(rt==0){
                if(p.gold==expectGold&&p.owned.containsKey("sentinel")&&!p.owned.containsKey("mage")){
                    CampaignClient.revealUntil=0;CampaignClient.notice=""; // what Skip reveal does, and the last roll's notice goes: the screenshots show the page itself
                    c.setScreen(new CampScreen("recruit"));rt=1;rtWait=0;}
                else if(++rtWait>300){rtOk=false;rtWhy.append("the migrated profile never reached the client (gold ").append(p.gold).append(", want ").append(expectGold).append("); ");rt=9;}
                return;
            }
            if(rt==1&&c.currentScreen instanceof CampScreen s){
                if(++rtWait==1){
                    String house=houses.get(ri);s.pickHouse(house);
                    var tabs=s.houseTabs();boolean five=tabs.size()==5&&tabs.stream().map(t->t.house).toList().equals(houses);
                    var cards=CampScreen.cards(house,p);Set<String> want=expected(house);Set<String> shown=new LinkedHashSet<>(),locked=new LinkedHashSet<>();
                    for(var card:cards)(card.owned()?shown:locked).add(card.troop().id());
                    boolean six=cards.stream().map(card->card.troop().id()).toList().equals(Arrays.stream(Spec.TROOPS).filter(t->t.house().equals(house)).map(Spec.TroopsRow::id).toList())&&cards.size()==6;
                    boolean owned=shown.equals(want)&&locked.stream().noneMatch(want::contains);
                    boolean selected=s.house.equals(house)&&tabs.stream().filter(t->t.selected()).map(t->t.house).toList().equals(List.of(house));
                    boolean label=s.rollLabel().equals("ROLL "+RosterMath.houseName(house).toUpperCase()+" / "+Spec.RECRUITMENT[0].rollCost()+" GOLD")&&s.pityLine().equals("Epic guaranteed within "+(Spec.RECRUITMENT[0].epicPity()-p.pity.getOrDefault(house,0))+" rolls");
                    boolean gold=s.header().contains(expectGold+" gold");
                    boolean ok=five&&six&&owned&&selected&&label&&gold;rtOk&=ok;
                    rtWhy.append(house).append(": tabs ").append(tabs.stream().map(t->t.house).toList()).append(five?"":" (BAD)").append(", cards owned ").append(shown).append(owned?"":" (want "+want+")").append(", locked ").append(locked).append(", \"").append(s.rollLabel()).append("\" / \"").append(s.pityLine()).append("\"").append(label?"":" (BAD)").append(", header \"").append(s.header()).append("\"").append(gold?"":" (BAD)").append(selected?"":" (tab not selected)").append("; ");
                    requestShot("engine-recruit-"+house);
                }
                if(wantShot!=null&&shotTaken){wantShot=null;ri++;rtWait=0;if(ri>=houses.size())rt=9;}
            }
            if(rt==9&&!rtDone){rtDone=true;c.setScreen(null);boolean ok=rtOk&&expectGold==420+3*Spec.ECONOMY[0].refundGold();String why=rtWhy.toString();
                server(c,b->{report("p2-recruit-tabs",ok,"five House tabs on the camp page recruit after the v1 fixture migrated (gold "+expectGold+" = 420 + 3 x refund; the brief expected 420 with the mage and the hexer still owned, but the migration refunds the mage, the hexer and the wraith): "+why);next(b,60);});}
        }

        public void serverTick(GravesAndGlory.Battle b){
            hold();
            switch(stage){
                case 1->{ // p2-armymobs
                    var w=b.world;boolean ok=true;StringBuilder why=new StringBuilder();
                    Map<String,EntityType<?>> types=Map.<String,EntityType<?>>of("husk",EntityType.HUSK,"skeleton",EntityType.SKELETON,"wither_skeleton",EntityType.WITHER_SKELETON);
                    for(String id:List.of("husk","skeleton","wither_skeleton")){
                        MobEntity m=ArmyMobs.create(id,w);String goals,targets;
                        try{goals=goalNames(m,"goalSelector").toString();targets=goalNames(m,"targetSelector").toString();}catch(ReflectiveOperationException e){goals=targets="unreadable: "+e;}
                        boolean good=m instanceof ArmyMob&&ArmyMobs.isTroop(m)&&m.getType()==types.get(id)&&goals.equals("[SwimGoal]")&&targets.equals("[]");
                        ok&=good;why.append(id).append(" -> ").append(m.getClass().getSimpleName()).append(", goals ").append(goals).append(", targets ").append(targets).append(good?"":" (BAD)").append("; ");
                    }
                    boolean threw=false;String thrown="";try{ArmyMobs.create("wolf",w);}catch(IllegalStateException e){threw=true;thrown=e.getMessage();}
                    var wolf=ArmyMobs.portrait("wolf",w);boolean portrait=wolf instanceof net.minecraft.entity.passive.WolfEntity&&!(wolf instanceof ArmyMob)&&!ArmyMobs.isTroop(wolf);
                    ArmyMobs.register("p2_probe",GravesAndGlory.ArmyHusk::new);boolean registered=ArmyMobs.create("p2_probe",w) instanceof GravesAndGlory.ArmyHusk;ArmyMobs.FACTORIES.remove("p2_probe");
                    boolean gone=false;try{ArmyMobs.create("p2_probe",w);}catch(IllegalStateException e){gone=true;}
                    boolean names=ArmyMobs.named("Order Squire")==Campaign.troop("knight")&&ArmyMobs.named("Knight-Captain")==Campaign.troop("knight")&&ArmyMobs.named("Nobody")==null&&ArmyMobs.stageName(Campaign.troop("knight"),1).equals("Order Squire")&&ArmyMobs.stageName(Campaign.troop("knight"),2).equals("Knight")&&ArmyMobs.stageName(Campaign.troop("mage"),2).equals("Sunweaver");
                    ok&=threw&&portrait&&registered&&gone&&names;
                    report("p2-armymobs",ok,why+"create(wolf) throws IllegalStateException="+threw+" (\""+thrown+"\"); portrait(wolf) is a plain wolf="+portrait+"; register(p2_probe) then create gives an ArmyHusk="+registered+", gone after removal="+gone+"; stage names resolve (Order Squire, Knight, Knight-Captain; a troop without a line keeps its name)="+names);
                    next(b,2);
                }
                case 2->{ // a stone platform and a sealed tank in the open sky, at noon
                    var p=sp(b);if(p==null)return;
                    site=new BlockPos(p.getBlockX()+14,150,p.getBlockZ()+14);BlockPos tank=site.add(14,0,0);
                    b.world.setTimeOfDay(6000);force(b,site.add(-6,0,-6),tank.add(6,0,6));
                    platform(b,site,6);
                    for(int x=-1;x<=5;x++)for(int y=-1;y<=3;y++)for(int z=-1;z<=3;z++){boolean inside=x>=0&&x<=4&&y>=0&&y<=2&&z>=0&&z<=2;b.world.setBlockState(tank.add(x,y,z),inside?Blocks.WATER.getDefaultState():Blocks.STONE.getDefaultState());}
                    army.clear();MobEntity[] list={ArmyMobs.create("husk",b.world),ArmyMobs.create("skeleton",b.world),ArmyMobs.create("wither_skeleton",b.world),new SkeletonEntity(EntityType.SKELETON,b.world),new WitherSkeletonEntity(EntityType.WITHER_SKELETON,b.world)};
                    for(int i=0;i<5;i++){var m=list[i];m.refreshPositionAndAngles(site.getX()+.5-4+i*2,site.getY(),site.getZ()+.5,0,0);m.equipStack(EquipmentSlot.HEAD,ItemStack.EMPTY);m.setPersistent();m.setAiDisabled(true);b.world.spawnEntity(m);if(i<3)army.add(m);}
                    vanillaSkeleton=list[3];vanillaWither=list[4];
                    armyHusk=ArmyMobs.create("husk",b.world);armyHusk.refreshPositionAndAngles(tank.getX()+1.5,tank.getY(),tank.getZ()+1.5,0,0);armyHusk.setPersistent();b.world.spawnEntity(armyHusk);
                    vanillaHusk=new HuskEntity(EntityType.HUSK,b.world);vanillaHusk.refreshPositionAndAngles(tank.getX()+3.5,tank.getY(),tank.getZ()+1.5,0,0);vanillaHusk.setPersistent();b.world.spawnEntity(vanillaHusk);
                    armyBurned=controlBurned=controlWitherBurned=false;submerged=0;next(b,3);
                }
                case 3->{ // p2-no-burn after 200 ticks, p2-no-convert after 700
                    long t=since(b);
                    if(t<=200){for(var m:army)if(m.getFireTicks()>0||m.isOnFire())armyBurned=true;if(vanillaSkeleton.getFireTicks()>0)controlBurned=true;if(vanillaWither.getFireTicks()>0)controlWitherBurned=true;}
                    if(armyHusk.isSubmergedIn(net.minecraft.registry.tag.FluidTags.WATER))submerged++;
                    if(t==200){boolean day=b.world.isDay();
                        report("p2-no-burn",!armyBurned&&controlBurned&&day,"noon (time "+b.world.getTimeOfDay()+", day="+day+"), sky open: the Army husk, skeleton and wither skeleton without helmets were never on fire in 200 ticks="+!armyBurned+"; the vanilla skeleton beside them caught fire="+controlBurned+" (the vanilla wither skeleton set fire ticks="+controlWitherBurned+")");}
                    if(t>=700){
                        boolean still=armyHusk instanceof GravesAndGlory.ArmyHusk&&armyHusk.isAlive()&&!armyHusk.isRemoved(),armyConverting=((ZombieEntity)armyHusk).isConvertingInWater(),vanillaConverting=((ZombieEntity)vanillaHusk).isConvertingInWater();
                        report("p2-no-convert",still&&!armyConverting&&submerged>=690&&vanillaConverting,"the ArmyHusk under water for "+submerged+" of 700 ticks is still an ArmyHusk="+still+" and is not converting="+!armyConverting+"; the vanilla Husk in the same tank is converting after 600 ticks="+vanillaConverting);
                        for(var m:army)m.discard();army.clear();vanillaSkeleton.discard();vanillaWither.discard();armyHusk.discard();vanillaHusk.discard();
                        BlockPos tank=site.add(14,0,0);for(int x=-1;x<=5;x++)for(int y=-1;y<=3;y++)for(int z=-1;z<=3;z++)b.world.setBlockState(tank.add(x,y,z),Blocks.AIR.getDefaultState());
                        for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)b.world.setBlockState(site.add(x,-1,z),Blocks.AIR.getDefaultState());
                        unforce(b);next(b,4);
                    }
                }
                case 4->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,5);} // a solo battle on map 0: the commander's army is the starter banner
                case 7->{ // p2-hud-house: the client side was checked in stage 5; the squad's traits and the seconds counted come from the server
                    var own=b.squads.get(player);
                    int secs=own.stats.traitSeconds.getOrDefault("gilded",0);boolean tier=own.traits.getOrDefault("gilded",0)==3&&Houses.bannerTiers(own).getOrDefault("gilded",0)==3;
                    report("p2-hud-house",hudOk&&tier&&secs>=1,hudWhy+"; the squad's traits "+own.traits+" and bannerTiers "+Houses.bannerTiers(own)+" hold gilded III="+tier+"; traitSeconds "+own.stats.traitSeconds+" (gilded counted "+secs+" s)");
                    next(b,8);
                }
                case 8->{ // the sky platform above Dawn's base; the real armies leave, two ad hoc armies take the stage
                    Projectiles.clear(b);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    for(var u:b.units)u.mob.discard();b.units.clear();
                    var base=GravesAndGlory.Battle.base(0);site=new BlockPos((int)base.x,140,(int)base.z);force(b,site.add(-13,0,-13),site.add(13,0,13));platform(b,site,12);
                    dawn=new GravesAndGlory.Squad(UUID.randomUUID(),0,base);dawn.order="hold";dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,base);dusk.order="hold";
                    knight=post(b,dawn,"knight",0,0,0,true);shielded.add(knight);bowFront=post(b,dusk,"bonebow",0,12,180,true);bowBack=post(b,dusk,"bonebow",0,-12,0,true);raise=true;
                    next(b,9);
                }
                case 9->{if(since(b)<10)return; // the Bone Archer shoots the raised-shield Squire from the front
                    mark=Combat.LAST.peekLast();hp0=knight.mob.getHealth();Projectiles.fire(b,bowFront,knight.mob,"arrow",10f);next(b,10);}
                case 10->{var hit=Combat.LAST.peekLast();
                    if(hit!=mark){front=hit;frontLost=hp0-knight.mob.getHealth();frontSource=knight.mob.getRecentDamageSource();next(b,11);}else if(since(b)>80){next(b,11);}}
                case 11->{if(since(b)<25)return; // the hurt cooldown has passed: full health, from behind
                    knight.mob.setHealth(knight.mob.getMaxHealth());mark=Combat.LAST.peekLast();hp0=knight.mob.getHealth();Projectiles.fire(b,bowBack,knight.mob,"arrow",10f);next(b,12);}
                case 12->{var hit=Combat.LAST.peekLast();
                    if(hit!=mark){back=hit;backLost=hp0-knight.mob.getHealth();next(b,13);}else if(since(b)>80){next(b,13);}}
                case 13->{if(since(b)<25)return; // the shield lowered, from the front
                    raise=false;knight.mob.setHealth(knight.mob.getMaxHealth());mark=Combat.LAST.peekLast();hp0=knight.mob.getHealth();Projectiles.fire(b,bowFront,knight.mob,"arrow",10f);next(b,14);}
                case 14->{var hit=Combat.LAST.peekLast();
                    if(hit!=mark){lowered=hit;loweredLost=hp0-knight.mob.getHealth();next(b,15);}else if(since(b)>80){next(b,15);}}
                case 15->{if(since(b)<25)return; // melee into the raised shield from the front: raise the shield first
                    raise=true;melee=post(b,dusk,"revenant",0,2.5,180,true);knight.mob.setHealth(knight.mob.getMaxHealth());next(b,30);}
                case 30->{if(since(b)<10)return; // the shield has been up ten ticks, more than vanilla's five before it counts as blocking
                    mark=Combat.LAST.peekLast();Combat.resolve(b,melee,knight.mob,5f,"melee",false);meleeFront=Combat.LAST.peekLast()!=mark?Combat.LAST.peekLast():null;next(b,16);}
                case 16->{if(since(b)<25)return; // and from behind, where the flank row applies and the shield does not
                    move(melee,0,-2.5,0);knight.mob.setHealth(knight.mob.getMaxHealth());mark=Combat.LAST.peekLast();
                    Combat.resolve(b,melee,knight.mob,5f,"melee",false);meleeBack=Combat.LAST.peekLast()!=mark?Combat.LAST.peekLast():null;
                    boolean f=front!=null&&near(front.counters(),0.35)&&front.rows().equals(List.of("C1"))&&frontLost>0&&frontLost<=3.5f+1e-3&&front.target()==knight.mob.getId()&&front.attacker()==bowFront.mob.getId()&&front.base()==10f&&near(front.traits(),1)&&near(front.auras(),1)&&near(front.commander(),1)&&near(front.total(),0.35);
                    boolean k=back!=null&&near(back.counters(),1.0)&&back.rows().isEmpty()&&backLost>frontLost&&near(back.total(),1.0);
                    boolean l=lowered!=null&&near(lowered.counters(),0.75)&&lowered.rows().equals(List.of("C2"))&&loweredLost>frontLost;
                    boolean m1=meleeFront!=null&&near(meleeFront.counters(),0.6)&&meleeFront.rows().equals(List.of("C3")),m2=meleeBack!=null&&near(meleeBack.counters(),1.15)&&meleeBack.rows().equals(List.of("C16"));
                    report("p2-shield-counter",f&&k&&l&&m1&&m2,"raised shield, Bone Archer arrow from the front: "+show(front)+", lost "+frontLost+" of "+10*0.35+" (the shield did not stop it)="+f+"; from behind: "+show(back)+", lost "+backLost+"="+k+"; shield lowered, front: "+show(lowered)+", lost "+loweredLost+"="+l+"; melee into the raised shield, front: "+show(meleeFront)+"="+m1+"; melee from behind: "+show(meleeBack)+"="+m2);
                    next(b,17);
                }
                case 17->{if(since(b)<25)return; // p2-damage-type, part 2: a blow from the real attack code (Tactics through Abilities) takes the same path
                    clearFixture();tactics=null;
                    hitter=post(b,dawn,"knight",0,0,0,false);victim=post(b,dusk,"revenant",0,1.8,180,true);
                    mark=Combat.LAST.peekLast();hp0=victim.mob.getHealth();next(b,18);}
                case 18->{
                    if(victim.mob.getHealth()<hp0){tactics=Combat.LAST.peekLast()!=mark?Combat.LAST.peekLast():null;tacticsLost=hp0-victim.mob.getHealth();tacticsSource=victim.mob.getRecentDamageSource();next(b,19);}
                    else if(since(b)>100)next(b,19);
                }
                case 19->{
                    Identifier troop=new Identifier("gravesandglory","troop");
                    boolean idOk=frontSource!=null&&frontSource.getTypeRegistryEntry().matchesId(troop),msgOk=idOk&&frontSource.getType().msgId().equals("gravesandglory.troop"),scaleOk=idOk&&frontSource.getType().scaling()==DamageScaling.NEVER,
                        exhaustOk=idOk&&Math.abs(frontSource.getType().exhaustion()-0.1)<1e-6,bypassOk=idOk&&frontSource.isIn(DamageTypeTags.BYPASSES_SHIELD),byOk=idOk&&frontSource.getAttacker()==bowFront.mob;
                    boolean arrow=idOk&&msgOk&&scaleOk&&exhaustOk&&bypassOk&&byOk;
                    boolean blow=tacticsSource!=null&&tacticsSource.getTypeRegistryEntry().matchesId(troop)&&tactics!=null&&tactics.attacker()==hitter.mob.getId()&&tactics.target()==victim.mob.getId()&&tactics.base()==5f&&tacticsLost>0;
                    report("p2-damage-type",arrow&&blow,"the arrow's source type "+(frontSource==null?"none":frontSource.getTypeRegistryEntry().getKey().map(k->k.getValue().toString()).orElse("?"))+" (msgId "+(frontSource==null?"-":frontSource.getType().msgId())+"="+msgOk+", scaling never="+scaleOk+", exhaustion 0.1="+exhaustOk+", in bypasses_shield="+bypassOk+", attacker is the archer="+byOk+")="+arrow
                        +"; a Squire's real melee blow (Tactics) is logged "+show(tactics)+", lost "+tacticsLost+", source type "+(tacticsSource==null?"none":tacticsSource.getTypeRegistryEntry().getKey().map(k->k.getValue().toString()).orElse("?"))+"="+blow);
                    // p2-abilities-shell, part 1: a Sunweaver heals a hurt Squire by 5
                    clearFixture();mage=post(b,dawn,"mage",0,6,180,false);ally=post(b,dawn,"knight",0,0,0,true);ally.mob.setHealth(10f);healBase=10f;healAt=-1;healed=0;next(b,20);
                }
                case 20->{
                    float h=ally.mob.getHealth();if(healAt<0&&h>healBase+.01f){healAt=since(b);healed=h-healBase;}
                    if(healAt>=0||since(b)>100){burstOk=healAt>=0&&healAt<=100&&Math.abs(healed-5f)<.01f;burstWhy="the Sunweaver healed the hurt Squire by "+healed+" after "+healAt+" ticks (want 5 within 100)="+burstOk;
                        // part 2: a Gravecaller's blight on three shield troops, two inside its radius and one outside
                        clearFixture();caster=post(b,dusk,"hexer",-9,0,-90,false);tA=post(b,dawn,"knight",0,0,0,true);tB=post(b,dawn,"knight",2,0,0,true);tC=post(b,dawn,"knight",0,3.2,0,true);shielded.add(tA);raise=true;
                        spellSeen=-1;burstAt=-1;hp0=tA.mob.getHealth();next(b,21);}
                }
                case 21->{
                    if(spellSeen<0&&caster.spellCenter!=null)spellSeen=b.time;
                    if(burstAt<0&&tA.mob.getHealth()<hp0){burstAt=b.time;
                        var slowA=tA.mob.getStatusEffect(StatusEffects.SLOWNESS);var slowB=tB.mob.getStatusEffect(StatusEffects.SLOWNESS);
                        boolean timing=spellSeen>=0&&burstAt-spellSeen==20,hurt=tB.mob.getHealth()<tB.mob.getMaxHealth()&&tC.mob.getHealth()==tC.mob.getMaxHealth()&&!tC.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                        boolean slowed=slowA!=null&&slowB!=null&&slowA.getAmplifier()==0&&slowB.getAmplifier()==0&&slowA.getDuration()>=38&&slowA.getDuration()<=40&&slowB.getDuration()>=38&&slowB.getDuration()<=40;
                        var hit=hitOn(tA);boolean magic=hit!=null&&near(hit.counters(),1.0)&&hit.rows().equals(List.of("C4"));
                        burstOk&=timing&&hurt&&slowed&&magic;
                        burstWhy+="; Gravecaller blight cast at tick "+spellSeen+", burst at "+burstAt+" ("+(burstAt-spellSeen)+" ticks, want 20)="+timing+"; the two knights inside 2.5 of the centre lost health and the one 3.2 away did not="+hurt+"; Slowness I for 40 on both inside ("+(slowA==null?"none":slowA.getAmplifier()+"/"+slowA.getDuration())+", "+(slowB==null?"none":slowB.getAmplifier()+"/"+slowB.getDuration())+")="+slowed+"; the raised shield did not blunt it: "+show(hit)+"="+magic;
                        report("p2-abilities-shell",burstOk,burstWhy);clearFixture();unforce(b);b.campaign.returnLobby();next(b,40);}
                    else if(since(b)>160){report("p2-abilities-shell",false,burstWhy+"; the blight never burst (cast seen at "+spellSeen+")");clearFixture();unforce(b);b.campaign.returnLobby();next(b,40);}
                }
                case 40->{ // p2-roll-house: gold 100, the draw fixed on the first Rare, and the file on disk agrees
                    var camp=b.campaign;if(b.running){camp.returnLobby();return;}
                    var pr=profile(b);pr.gold=Spec.RECRUITMENT[0].rollCost();pr.owned.remove("wm_stalker");pr.pity.put("wildmoot",0);camp.save(player);
                    rngSaved=camp.rollRng;camp.rollRng=fixed(86);command(b,"roll:wildmoot",41);
                }
                case 41->{if(since(b)>=20){
                    var camp=b.campaign;var pr=profile(b);camp.rollRng=rngSaved;boolean disk=false;String file="";
                    try{var j=com.google.gson.JsonParser.parseString(Files.readString(camp.directory.resolve(player+".json"))).getAsJsonObject();
                        disk=j.get("gold").getAsInt()==0&&j.getAsJsonObject("owned").has("wm_stalker")&&j.getAsJsonObject("pity").get("wildmoot").getAsInt()==1;file="gold "+j.get("gold")+", pity "+j.getAsJsonObject("pity").get("wildmoot");}catch(Exception e){file=e.toString();}
                    var wildmoot=pr.owned.keySet().stream().filter(id->Campaign.troop(id).house().equals("wildmoot")).toList();
                    boolean ok=pr.gold==0&&pr.owned.containsKey("wm_stalker")&&wildmoot.size()==3&&pr.pity.getOrDefault("wildmoot",-1)==1&&disk;
                    report("p2-roll-house",ok,"roll:wildmoot with 100 gold and the draw fixed on the first Rare: gold "+pr.gold+", Wildmoot owned "+wildmoot+" (want the two Commons and the Web Stalker), pity.wildmoot "+pr.pity.get("wildmoot")+" (want 1); the saved file: "+file+"="+disk+"; notice \""+b.campaign.notices.getOrDefault(player,"")+"\"");
                    next(b,50);}}
                case 50->{ // p2-recruit-tabs: the same v1 file as p2-migrate-live, then the camp page recruit on the client
                    var camp=b.campaign;if(b.running){camp.returnLobby();return;}
                    try{byte[] fx=fixture("profile-v1-partial.json");Files.createDirectories(camp.directory);
                        Path file=camp.directory.resolve(player+".json"),backup=camp.directory.resolve(player+".v1.json");Files.deleteIfExists(backup);Files.write(file,fx);camp.profiles.remove(player);camp.notices.remove(player); // the last roll's notice would otherwise stay under the page
                        expectGold=camp.profile(player).gold;
                    }catch(IOException e){report("p2-recruit-tabs",false,e.toString());next(b,60);return;}
                    next(b,51);
                }
                case 60->{ // p2-tick-cost: eight bot armies, 160 troops (20 each); p2-render-cost: the client's world render over the same 600 ticks
                    var p=sp(b);if(p==null)return;
                    if(cost==null)cost=tickCost(b,List.of("gilded_easy","gilded_easy","gilded_easy","gilded_easy","graveborn_easy","graveborn_easy","graveborn_easy","graveborn_easy"));
                    if(cost.step(p)){
                        report("p2-tick-cost",cost.last<40,"server tick "+String.format(Locale.ROOT,"%.2f",cost.last)+" ms (600-tick mean "+String.format(Locale.ROOT,"%.2f",cost.avg)+" ms) with "+cost.troops+" troops at the start and "+b.units.size()+" at the end ("+cost.note+")");
                        report("p2-render-cost",cost.frames>0&&cost.troopDraws>0,"report only (Task 5 review minor 1): "+cost.render);
                        cost.handBack();b.campaign.returnLobby();next(b,99);
                    }
                }
                default->{}
            }
        }
    }

    /**
     * The reusable tick-cost fixture (every House task runs it with its own banners): the commander's army and seven bot armies (four a side,
     * 160 troops under the easy bot banners, 20 each; the normal banners field 19, their Epic takes two slots) fight around the centre point for 600 ticks, each army under the next banner of the list (Dawn's armies
     * first; a short list repeats), and the server's tick time is read each tick. Call step on every server tick until it returns true. Over the
     * same 600 ticks RenderCost times the client's world render with the battle in view (render, frames and troopDraws once done).
     */
    static TickCost tickCost(GravesAndGlory.Battle b,List<String> banners){return new TickCost(b,banners);}
    static final class TickCost {
        final GravesAndGlory.Battle b;final List<String> banners;GravesAndGlory.Squad own;boolean begun;long start;double sum,avg;int samples,troops;float last;String note="";
        /** RenderCost's figures for the window, once step has returned true. */
        long frames,troopDraws;String render="";
        TickCost(GravesAndGlory.Battle b,List<String> banners){this.b=b;this.banners=banners;}
        /** The commander takes his army back (it fought as a bot); the next returnLobby would drop the seat. */
        void handBack(){if(own!=null)own.bot=false;}
        boolean step(ServerPlayerEntity p){
            if(!begun){
                if(!Phase1Checks.skirmish(b,p,0,4,"normal"))return false;
                own=b.squads.get(p.getUuid());own.bot=true;
                var armies=new ArrayList<>(b.squads.values().stream().filter(s->s.seated).toList());armies.sort(Comparator.comparingInt(s->s.team));
                for(int i=0;i<armies.size();i++)field(b,armies.get(i),banner(banners.get(i%banners.size())));
                var centre=b.points.stream().min(Comparator.comparingDouble(c->Math.hypot(c.spec.x(),c.spec.z()))).orElseThrow().spec;
                int[] n={0,0};
                for(var u:b.units){int t=u.squad.team,i=n[t]++;double x=centre.x()+(t==0?-12:12)+(t==0?-1:1)*(i/10)*1.5,z=centre.z()+(i%10-4.5)*1.5;u.mob.refreshPositionAndAngles(x,Phase1Checks.top(b,x,z),z,t==0?-90:90,0);}
                for(var s:armies){s.order="attack";s.target=new Vec3d(centre.x(),65,centre.z());s.reinforceAt=Long.MAX_VALUE;}
                p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);b.relocate(p,centre.x(),Math.max(63,Arena.ground(centre.x(),centre.z()))+15,centre.z()+24,180,30);
                troops=b.units.size();begun=true;start=b.time;note=armies.size()+" armies around "+centre.id()+" under "+banners;RenderCost.start();
                System.out.println("[p2] tick-cost fixture: "+armies.size()+" armies, "+troops+" troops around "+centre.id()+" under "+banners);
                return false;
            }
            sum+=b.server.getTickTime();samples++;
            if(b.time-start<600)return false;
            avg=sum/samples;last=b.server.getTickTime();System.out.println("[p2] tick-cost avg="+String.format(Locale.ROOT,"%.2f",avg));
            RenderCost.on=false;frames=RenderCost.frames;troopDraws=RenderCost.draws;render=RenderCost.summary();System.out.println("[p2] render-cost "+render);return true;
        }
    }
    /**
     * Task 5 review minor 1: the client's cost of drawing a battle. While on, the time from WorldRenderEvents.START to END of
     * every frame (the whole world: terrain, entities, particles), the troop-model draws in it (TroopRenderer.troopDraws) and the
     * entities drawn (WorldRenderer's entity count). Set from the server thread, counted on the render thread.
     */
    static final class RenderCost {
        static volatile boolean on;static volatile long frames,sumNs,worstNs,draws,entities;static long begin,drawsAt;
        static void register(){
            WorldRenderEvents.START.register(ctx->{if(on){begin=System.nanoTime();drawsAt=TroopRenderer.troopDraws;}});
            WorldRenderEvents.END.register(ctx->{if(!on||begin==0)return;long ns=System.nanoTime()-begin;begin=0;
                frames++;sumNs+=ns;worstNs=Math.max(worstNs,ns);draws+=TroopRenderer.troopDraws-drawsAt;entities+=rendered(MinecraftClient.getInstance());});
        }
        /** The entities the last frame drew ("E: drawn/loaded" of the F3 screen). */
        static int rendered(MinecraftClient c){try{String t=c.worldRenderer.getEntitiesDebugString();int a=t.indexOf("E: ")+3;return Integer.parseInt(t.substring(a,t.indexOf('/',a)).trim());}catch(RuntimeException e){return 0;}}
        static void start(){frames=sumNs=worstNs=draws=entities=0;begin=0;on=true;}
        static String summary(){
            long n=Math.max(1,frames);
            return String.format(Locale.ROOT,"world render %.2f ms mean, %.2f ms worst over %d frames (%d fps at the end); per frame %.1f troop-model draws and %.1f entities drawn",
                sumNs/1e6/n,worstNs/1e6,frames,MinecraftClient.getInstance().getCurrentFps(),draws/(double)n,entities/(double)n);
        }
    }
    /**
     * Task 5: the fixture both art suites share. A grass platform in the sky above Dawn's base at noon, the battle's own armies
     * gone and its reinforcements stopped, gold against violet; troops are posted with Battle.spawn (so they carry their names,
     * side teams and painted shields), their AI off and held in place each server tick; the commander is a spectator camera.
     */
    abstract static class Stage implements Suite,ServerWatch {
        record Pin(MobEntity mob,Vec3d at,float yaw){}
        volatile int stage;volatile UUID player;volatile boolean ready,serverDone;
        BlockPos site;final Set<net.minecraft.util.math.ChunkPos> forced=new HashSet<>();final List<Pin> pins=new ArrayList<>();
        GravesAndGlory.Squad gold,violet;final List<GravesAndGlory.Unit> posted=new ArrayList<>();
        int cs,wait;boolean finished;
        /** The posted troops never attack or raise a shield (Task 5b: their clips would play; the art suites show them at rest). */
        boolean calm=true;
        abstract String name();
        /** Posts the stage's troops (server thread); the battle is running, gold against violet, the floor laid. */
        abstract void cast(GravesAndGlory.Battle b);
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Vec3d at(double dx,double dz){return new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);}
        static void pin(MobEntity m,Vec3d at,float yaw){m.refreshPositionAndAngles(at.x,at.y,at.z,yaw,0);m.setHeadYaw(yaw);m.setBodyYaw(yaw);m.prevBodyYaw=yaw;m.prevHeadYaw=yaw;m.setVelocity(Vec3d.ZERO);}
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw){
            Vec3d where=at(dx,dz);var u=b.spawn(s,Campaign.troop(troop),40+posted.size(),where);u.mob.setAiDisabled(true);Phase1Checks.freeze(u);u.deployed=where;
            posted.add(u);pins.add(new Pin(u.mob,where,yaw));pin(u.mob,where,yaw);return u;
        }
        void move(MobEntity m,double dx,double dz,float yaw){Vec3d where=at(dx,dz);pins.removeIf(p->p.mob()==m);pins.add(new Pin(m,where,yaw));pin(m,where,yaw);}
        /** Renames a posted troop to its stage's name, as Battle.spawn names a troop of that stage. */
        static void stage(GravesAndGlory.Unit u,int stage){u.stage=stage;u.mob.setCustomName(net.minecraft.text.Text.literal(ArmyMobs.stageName(u.spec,stage)).formatted(u.squad.team==0?net.minecraft.util.Formatting.AQUA:net.minecraft.util.Formatting.LIGHT_PURPLE));}
        /** The spectator camera: the eye at (dx, eye, dz) from the platform centre's standing layer. */
        void camera(GravesAndGlory.Battle b,double dx,double eye,double dz,float yaw,float pitch){var p=sp(b);if(p!=null)b.relocate(p,site.getX()+.5+dx,site.getY()+eye-1.62,site.getZ()+.5+dz,yaw,pitch);}
        void force(GravesAndGlory.Battle b,BlockPos from,BlockPos to){
            for(int cx=from.getX()>>4;cx<=to.getX()>>4;cx++)for(int cz=from.getZ()>>4;cz<=to.getZ()>>4;cz++){b.world.setChunkForced(cx,cz,true);b.world.getChunk(cx,cz);forced.add(new net.minecraft.util.math.ChunkPos(cx,cz));}
        }
        void unforce(GravesAndGlory.Battle b){for(var p:forced)b.world.setChunkForced(p.x,p.z,false);forced.clear();}
        /** Grass under the standing layer, dirt below it, clear air above, half blocks each way. */
        void floor(GravesAndGlory.Battle b,int half){
            for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){var c=site.add(x,0,z);b.world.setBlockState(c.down(),Blocks.GRASS_BLOCK.getDefaultState());b.world.setBlockState(c.down(2),Blocks.DIRT.getDefaultState());for(int y=0;y<=8;y++)b.world.setBlockState(c.up(y),Blocks.AIR.getDefaultState());}
        }
        void clearPosted(GravesAndGlory.Battle b){for(var u:posted){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();}b.units.removeAll(posted);posted.clear();pins.clear();}
        /** Client: the client-side entity of a server mob (the integrated server shares entity ids). */
        static Entity client(MinecraftClient c,MobEntity m){return c.world==null?null:c.world.getEntityById(m.getId());}
        /** Client: sky light 15 on this client at the feet and eyes of where troops stand (lineup, studio and both wings of the
         *  floor) and at every posted troop's eyes (the light its model is drawn with), and the terrain rebuilt, so a shot shows the
         *  noon platform and not one whose light updates are still on their way. */
        boolean lit(MinecraftClient c){
            if(c.world==null||c.worldRenderer==null||site==null)return false;
            var sky=net.minecraft.world.LightType.SKY;
            for(int[] d:LIT_AT)for(int y=0;y<=1;y++)if(c.world.getLightLevel(sky,site.add(d[0],y,d[1]))<15)return false;
            List<GravesAndGlory.Unit> units;try{units=new ArrayList<>(posted);}catch(ConcurrentModificationException e){return false;}
            for(var u:units){var e=client(c,u.mob);if(e!=null&&c.world.getLightLevel(sky,BlockPos.ofFloored(e.getEyePos()))<15)return false;}
            return c.worldRenderer.isTerrainRenderComplete();
        }
        /** Client: why lit(c) is false, for a step that waits on it: the sky light at eye level where the troops stand, the
         *  posted troops' eyes under 15, and whether the terrain is built. */
        String litWhy(MinecraftClient c){
            if(c.world==null||c.worldRenderer==null||site==null)return "no world or no stage yet";
            var sky=net.minecraft.world.LightType.SKY;StringBuilder sb=new StringBuilder("sky light ");
            for(int[] d:LIT_AT)sb.append(d[0]).append(",").append(d[1]).append("=").append(c.world.getLightLevel(sky,site.add(d[0],0,d[1]))).append("/").append(c.world.getLightLevel(sky,site.add(d[0],1,d[1]))).append(" ");
            int dark=0;List<GravesAndGlory.Unit> units;try{units=new ArrayList<>(posted);}catch(ConcurrentModificationException e){units=List.of();}
            for(var u:units){var e=client(c,u.mob);if(e!=null&&c.world.getLightLevel(sky,BlockPos.ofFloored(e.getEyePos()))<15)dark++;}
            return sb.append("; troops' eyes under 15: ").append(dark).append("; terrain built=").append(c.worldRenderer.isTerrainRenderComplete()).toString();
        }
        /** Client: the last frame's ground, the bottom sixth of the screen, is in daylight: mean green 70 or more (lit grass is about
         *  100, the unlit platform of a bad frame about 10). This frame is the one shot() saves in the same tick. */
        static boolean groundLit(MinecraftClient c){
            try(var img=ScreenshotRecorder.takeScreenshot(c.getFramebuffer())){
                int w=img.getWidth(),h=img.getHeight(),n=0;long g=0;
                for(int y=h*5/6;y<h;y+=4)for(int x=0;x<w;x+=8){g+=(img.getColor(x,y)>>8)&255;n++;}
                return n>0&&g/n>=70;
            }
        }
        static final int[][] LIT_AT={{0,0},{-6,0},{6,0},{0,-3},{0,6},{-6,12},{0,12},{6,12}};
        int darkFor,awayFor,recoveries;Vec3d back;float backYaw,backPitch;
        /**
         * Client: lit(c) for a suite's first step, with the recovery the Task 5 review asked for (minor 5). Sky light 0 at every
         * LIT_AT point for 600 ticks with the terrain built means this client never got the platform's light, and waiting does not
         * mend that: the spectator goes 400 blocks east (out of view distance, inside the arena) and back to the camera 40 ticks
         * later, so the client takes the platform's chunks and their light again. "[p2-light]" lines mark a recovered run.
         */
        boolean litStart(MinecraftClient c){
            if(awayFor>0){
                if(++awayFor>=40){awayFor=0;darkFor=0;server(c,b->{var p=sp(b);if(p!=null&&back!=null)b.relocate(p,back.x,back.y,back.z,backYaw,backPitch);});
                    System.out.println("[p2-light] "+name()+": back at the platform after 40 ticks away; waiting for its light again");}
                return false;
            }
            if(lit(c)){darkFor=0;return true;}
            if(c.world!=null&&site!=null&&c.worldRenderer!=null&&c.worldRenderer.isTerrainRenderComplete()&&allDark(c)){
                if(++darkFor>=600){awayFor=1;darkFor=0;recoveries++;
                    server(c,b->{var p=sp(b);if(p==null)return;back=p.getPos();backYaw=p.getYaw();backPitch=p.getPitch();b.relocate(p,site.getX()+400.5,site.getY()+4,site.getZ()+.5,backYaw,backPitch);});
                    System.out.println("[p2-light] "+name()+": sky light 0 at every floor point for 600 ticks with the terrain built; the spectator goes 400 blocks away and back to take the platform's chunks again (recovery "+recoveries+")");}
            }else darkFor=0;
            return false;
        }
        /** Client: sky light 0 at both layers of every LIT_AT point. */
        boolean allDark(MinecraftClient c){
            var sky=net.minecraft.world.LightType.SKY;
            for(int[] d:LIT_AT)for(int y=0;y<=1;y++)if(c.world.getLightLevel(sky,site.add(d[0],y,d[1]))>0)return false;
            return true;
        }
        int litWait,litAgain;
        /** Client: whether a world shot may be taken this tick. A freshly laid platform can go dark on the client after it was lit
         *  (a light update from the server's engine half way through the floor), so every shot checks again, the light data and the
         *  frame itself. While the stage is unlit the caller holds its step; once it is lit again it settles 10 ticks before the
         *  shot. After 1200 ticks it shoots anyway and the suite fails on p2-<suite>-light. */
        boolean shotReady(MinecraftClient c,String shot){
            boolean on=lit(c)&&groundLit(c);
            if(on&&litWait==0)return true;
            if(on){if(++litAgain<10)return false;System.out.println("[p2-light] "+shot+": lit after holding "+litWait+" ticks");litWait=0;litAgain=0;return true;}
            litAgain=0;
            if(litWait++==0){StringBuilder sb=new StringBuilder();if(c.world!=null&&site!=null)for(int[] d:LIT_AT)sb.append(d[0]).append(",").append(d[1]).append("=").append(c.world.getLightLevel(net.minecraft.world.LightType.SKY,site.add(d[0],1,d[1]))).append(" ");
                System.out.println("[p2-light] "+shot+": holding, eye-level sky light "+sb.toString().trim()+", terrain built="+(c.worldRenderer!=null&&c.worldRenderer.isTerrainRenderComplete())+", ground lit="+groundLit(c));}
            if(litWait<1200)return false;
            report("p2-"+name()+"-light",false,shot+" taken unlit after 1200 ticks of waiting for sky light 15 where the troops stand");litWait=0;return true;
        }

        public void serverTick(GravesAndGlory.Battle b){
            for(var p:pins)if(p.mob().isAlive()){pin(p.mob(),p.at(),p.yaw());if(calm){p.mob().setAttacking(false);if(p.mob().isUsingItem())p.mob().stopUsingItem();}}
            if(stage==1){var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,b.campaign.map,1,"normal"))return;stage=2;}
            else if(stage==2){
                var p=sp(b);if(p==null)return;Projectiles.clear(b);
                for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                for(var u:b.units){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();}b.units.clear();
                b.sides=new int[]{0,1}; // gold against violet, whatever the profile asks for
                var base=GravesAndGlory.Battle.base(0);site=new BlockPos((int)base.x,140,(int)base.z);
                force(b,site.add(-18,0,-18),site.add(18,0,18));floor(b,17);b.world.setTimeOfDay(6000);
                b.world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(false,b.server);
                gold=new GravesAndGlory.Squad(UUID.randomUUID(),0,base);gold.order="hold";violet=new GravesAndGlory.Squad(UUID.randomUUID(),1,base);violet.order="hold";
                p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                cast(b);stage=3;ready=true;
            }
        }
        void end(MinecraftClient c){c.options.hudHidden=false;c.setScreen(null);finished=true;finish(c,name());}
        /** The FOV effect scale before a shot that needs a fixed field of view; -1 when untouched. */
        double fovBefore=-1;
        /** Sets the FOV effect scale (speed and flight change the field of view), or with -1 puts back the one before. */
        void fovScale(MinecraftClient c,double v){
            var o=c.options.getFovEffectScale();
            if(v>=0){if(fovBefore<0)fovBefore=o.getValue();o.setValue(v);}
            else if(fovBefore>=0){o.setValue(fovBefore);fovBefore=-1;}
        }
        public void cleanup(MinecraftClient c){
            c.options.hudHidden=false;Frame.clear();fovScale(c,-1);
            server(c,b->{clearPosted(b);unforce(b);b.world.getGameRules().get(net.minecraft.world.GameRules.DO_DAYLIGHT_CYCLE).set(true,b.server);var p=sp(b);if(p!=null)p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);if(b.running)b.campaign.returnLobby();});
        }
    }

    /** A screen that draws portrait mobs as the cards do (InventoryScreen.drawEntity, the gaze 20 to the side), large and at card size. */
    static final class PortraitProbe extends net.minecraft.client.gui.screen.Screen {
        final List<MobEntity> mobs;
        PortraitProbe(List<MobEntity> mobs){super(net.minecraft.text.Text.literal("Portraits"));this.mobs=mobs;}
        @Override public void render(net.minecraft.client.gui.DrawContext g,int mx,int my,float delta){
            g.fill(0,0,width,height,0xff24222a);int n=mobs.size();
            for(int i=0;i<n;i++){var e=mobs.get(i);if(e==null)continue;int x=width*(2*i+1)/(2*n+2);
                net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,x,height*3/4,height/4,20,0,e);}
            for(int i=0;i<n;i++){var e=mobs.get(i);if(e==null)continue;int x=width*(2*n+1)/(2*n+2),y=height*(i+1)/(n+1);
                DarkUi.frame(g,x-24,y-38,48,48);net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,x,y+6,ArmyCards.size(e,22),22,0,e);}
        }
        @Override public boolean shouldPause(){return false;}
    }

    /**
     * A frame caught as it was drawn: the first frame, once armed, in which TroopRenderer drew the watched world entity (by
     * network id: the integrated server shares them) while the condition holds. The end of that frame keeps the framebuffer,
     * the camera and the matrices the world was drawn with, the entity's pose as drawn (its body yaw and its bones'
     * TroopModel.snapshot: the model is shared, so face puts the snapshot back before it measures), the renderer's record of
     * that draw, and whether the vanilla renderer's frustum test would have drawn the entity in that frame.
     */
    static final class Frame {
        static final List<Frame> ALL=new java.util.concurrent.CopyOnWriteArrayList<>();
        final int id;final boolean fresh;final java.util.function.Predicate<Entity> when;volatile boolean done;int before;Set<Entity> known=Set.of();
        NativeImage image;Matrix4f proj,view;Vec3d cam,pos;float bodyYaw;int deathTime;TroopRenderer.Draw draw;boolean vanillaSees;Entity entity;float[] pose;
        Frame(int id,boolean fresh,java.util.function.Predicate<Entity> when){this.id=id;this.fresh=fresh;this.when=when;}
        static Frame watch(int id,java.util.function.Predicate<Entity> when){Frame f=new Frame(id,false,when);ALL.add(f);return f;}
        static Frame watch(int id){return watch(id,e->true);}
        /** The first frame that draws, passing the condition, a world entity TroopRenderer had never drawn before that frame. */
        static Frame first(java.util.function.Predicate<Entity> when){Frame f=new Frame(-1,true,when);ALL.add(f);return f;}
        static void register(){
            WorldRenderEvents.START.register(ctx->{for(Frame f:ALL)if(!f.done)f.start(ctx);});
            WorldRenderEvents.END.register(ctx->{for(Frame f:ALL)if(!f.done)f.end(ctx);});
        }
        static void clear(){for(Frame f:ALL)f.close();ALL.clear();}
        void start(WorldRenderContext ctx){
            if(fresh){Set<Entity> s=Collections.newSetFromMap(new IdentityHashMap<>());for(Entity e:ctx.world().getEntities())if(TroopRenderer.drawn(e)!=null)s.add(e);known=s;return;}
            var e=ctx.world().getEntityById(id);var d=e==null?null:TroopRenderer.drawn(e);before=d==null?0:d.count;
        }
        @SuppressWarnings("unchecked")
        void end(WorldRenderContext ctx){
            Entity e=null;
            if(fresh){for(Entity x:ctx.world().getEntities())if(!known.contains(x)&&TroopRenderer.drawn(x)!=null&&when.test(x)){e=x;break;}}
            else{var x=ctx.world().getEntityById(id);var d=x==null?null:TroopRenderer.drawn(x);if(d!=null&&d.count>before&&when.test(x))e=x;}
            if(e==null)return;var d=TroopRenderer.drawn(e);
            var c=MinecraftClient.getInstance();float t=ctx.tickDelta();
            image=ScreenshotRecorder.takeScreenshot(c.getFramebuffer());
            proj=new Matrix4f(ctx.projectionMatrix());view=new Matrix4f(ctx.matrixStack().peek().getPositionMatrix());cam=ctx.camera().getPos();
            pos=e.getLerpedPos(t);entity=e;bodyYaw=e.getYaw();
            if(e instanceof net.minecraft.entity.LivingEntity le){bodyYaw=net.minecraft.util.math.MathHelper.lerpAngleDegrees(t,le.prevBodyYaw,le.bodyYaw);deathTime=le.deathTime;}
            draw=d.last;pose=d.pose==null?null:d.pose.clone();
            var r=c.getEntityRenderDispatcher().getRenderer(e);
            vanillaSees=r instanceof TroopRenderer<?> tr&&ctx.frustum()!=null&&((net.minecraft.client.render.entity.EntityRenderer<Entity>)(Object)tr.vanilla()).shouldRender(e,ctx.frustum(),cam.x,cam.y,cam.z);
            done=true;ALL.remove(this);
        }
        /** face(m, bone, box, 5): inset by a fifth of its size on each side. */
        int[] face(TroopModel<?> m,String bone,int box){return face(m,bone,box,5);}
        /**
         * The image rectangle (x0, y0, x1, y1) of the camera-facing big face (the front or the back) of one box of a bone, as
         * this frame drew it, inset by 1/inset of its size on each side; null when it lies behind the camera. It may reach past
         * the frame: sideShare and onScreen count only the pixels on it.
         */
        int[] face(TroopModel<?> m,String bone,int box,int inset){
            List<ModelPart> path=m.path(bone);if(path.isEmpty()||image==null)return null;
            m.applyPose(pose);
            MatrixStack ms=new MatrixStack();
            ms.translate((float)(pos.x-cam.x),(float)(pos.y-cam.y),(float)(pos.z-cam.z));
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-bodyYaw));ms.scale(-1,-1,1);ms.translate(0,-1.501f,0);
            for(int k=0;k<path.size()-1;k++)path.get(k).rotate(ms);
            List<Vector4f[]> faces=new ArrayList<>();
            path.get(path.size()-1).forEachCuboid(ms,(entry,p,i,q)->{if(!p.isEmpty()||i!=box)return;
                for(float z:new float[]{q.minZ,q.maxZ}){Vector4f[] f=new Vector4f[4];int n=0;
                    for(float y:new float[]{q.minY,q.maxY})for(float x:new float[]{q.minX,q.maxX}){Vector4f v=new Vector4f(x/16f,y/16f,z/16f,1f);entry.getPositionMatrix().transform(v);view.transform(v);f[n++]=v;}
                    faces.add(f);}});
            if(faces.size()!=2)return null;
            Vector4f[] pick=near(faces.get(0))<near(faces.get(1))?faces.get(0):faces.get(1);
            double x0=1e9,y0=1e9,x1=-1e9,y1=-1e9;int w=image.getWidth(),h=image.getHeight();
            for(Vector4f v:pick){Vector4f p=new Vector4f(v);proj.transform(p);if(p.w<=0)return null;
                double sx=(p.x/p.w*.5+.5)*w,sy=(1-(p.y/p.w*.5+.5))*h;x0=Math.min(x0,sx);y0=Math.min(y0,sy);x1=Math.max(x1,sx);y1=Math.max(y1,sy);}
            double ix=(x1-x0)/inset,iy=(y1-y0)/inset;
            return new int[]{(int)Math.round(x0+ix),(int)Math.round(y0+iy),(int)Math.round(x1-ix),(int)Math.round(y1-iy)};
        }
        private static double near(Vector4f[] f){double x=0,y=0,z=0;for(Vector4f v:f){x+=v.x;y+=v.y;z+=v.z;}return x*x+y*y+z*z;}
        /**
         * The share of a rectangle's pixels in a side's cloth colour. Light and the red hurt tint of a dying troop scale green
         * and blue alike, so the blue-to-green ratio decides (within 35 percent of the side colour's), with red to green at least
         * nine tenths of the side colour's (the tint only adds red). Linen, white charges, steel and sky all fall outside it for
         * gold and violet.
         */
        double sideShare(int[] r,int rgb){
            if(r==null||image==null||r[2]<=r[0]||r[3]<=r[1])return 0;int n=0,k=0;
            for(int y=Math.max(0,r[1]);y<Math.min(image.getHeight(),r[3]);y++)for(int x=Math.max(0,r[0]);x<Math.min(image.getWidth(),r[2]);x++){n++;if(sideLike(image.getColor(x,y),rgb))k++;}
            return n==0?0:k/(double)n;
        }
        /** How many of a rectangle's pixels lie on the frame. */
        int onScreen(int[] r){
            if(r==null||image==null)return 0;
            return Math.max(0,Math.min(image.getWidth(),r[2])-Math.max(0,r[0]))*Math.max(0,Math.min(image.getHeight(),r[3])-Math.max(0,r[1]));
        }
        static boolean sideLike(int abgr,int rgb){
            double r=abgr&255,g=abgr>>8&255,b=abgr>>16&255,sr=rgb>>16&255,sg=rgb>>8&255,sb=rgb&255;
            if(g<12||b<1)return false;
            return Math.abs(Math.log((b/g)/(sb/sg)))<Math.log(1.35)&&r/g>=0.9*sr/sg;
        }
        static int colour(String side){for(var f:Spec.FACTIONS)if(f.id().equals(side))return f.color();return 0;}
        void save(MinecraftClient c,String name){
            if(image==null)return;
            try{Path dir=c.runDirectory.toPath().resolve("screenshots");Files.createDirectories(dir);image.writeTo(dir.resolve("p2-"+name+".png"));}
            catch(IOException e){report("p2-"+currentName+"-exception",false,"saving "+name+": "+e);}
        }
        /** The frame with the probed rectangles outlined in white, beside the clean one, for the reviewer. */
        void saveProbe(MinecraftClient c,String name,int[]... rects){
            if(image==null)return;
            try(NativeImage copy=new NativeImage(image.getWidth(),image.getHeight(),false)){
                copy.copyFrom(image);
                for(int[] r:rects)if(r!=null)for(int x=Math.max(0,r[0]-1);x<=Math.min(copy.getWidth()-1,r[2]);x++)for(int y=Math.max(0,r[1]-1);y<=Math.min(copy.getHeight()-1,r[3]);y++)
                    if(x==r[0]-1||x==r[2]||y==r[1]-1||y==r[3])copy.setColor(x,y,0xFFFFFFFF);
                Path dir=c.runDirectory.toPath().resolve("screenshots");Files.createDirectories(dir);copy.writeTo(dir.resolve("p2-"+name+".png"));
            }catch(IOException e){report("p2-"+currentName+"-exception",false,"saving "+name+": "+e);}
        }
        void close(){if(image!=null){image.close();image=null;}}
    }
    static JsonObject json(String s){return JsonParser.parseString(s).getAsJsonObject();}

    /**
     * Task 5 Step 7: the troop model spike. Two Order Squires and two Templars, one of each per team (gold against violet), 3
     * blocks apart in front of the camera at noon, and an unnamed husk beside them.
     */
    static final class Spike extends Stage {
        MobEntity husk;long drawsBefore;
        /** Each posted troop's draw count (TroopRenderer.drawn) when p2-spike-renderer starts counting. */
        final int[] drawsAt=new int[4];
        /** The death second and the spawn frame: the Squire killed in front of the camera, the one spawned off its team, and their frames. */
        volatile GravesAndGlory.Unit dying,born;Frame deathFrame,spawnFrame;int killedAt,deathStall,bornAt=-1,bornSeen=-1,bornDrawn=-1;
        /** The end of the hold (Task 5 review minor 4): a Squire spawned off its team, which never comes, and when this client first had it and first drew it. */
        volatile GravesAndGlory.Unit linen;int linenSeen=-1,linenDrawn=-1;
        /** Ticks from the spawn to the team: the body has turned its back to the camera by then (BodyControl), within the hold. */
        static final int TEAM_LATE=30;
        String name(){return "spike";}
        void cast(GravesAndGlory.Battle b){
            post(b,gold,"knight",-4.5,0,0);post(b,gold,"sentinel",-1.5,0,0);post(b,violet,"knight",1.5,0,0);post(b,violet,"sentinel",4.5,0,0);
            husk=new HuskEntity(EntityType.HUSK,b.world);Vec3d h=at(7.5,0);husk.refreshPositionAndAngles(h.x,h.y,h.z,0,0);husk.setPersistent();husk.setAiDisabled(true);b.world.spawnEntity(husk);pins.add(new Pin(husk,h,0));
            camera(b,1.5,1.25,6.5,180,4);
        }
        List<Entity> entities(MinecraftClient c){
            List<Entity> out=new ArrayList<>();for(var u:posted){var e=client(c,u.mob);if(e==null||e.getCustomName()==null)return null;out.add(e);}
            var h=husk==null?null:client(c,husk);if(h==null)return null;out.add(h);return out;
        }
        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
            if(finished)return;
            if(tick>6000){report("p2-spike-timeout",false,"stuck at client step "+cs+", server stage "+stage);end(c);return;}
            switch(cs){
                case 0->{boolean lit=litStart(c); // first: while the spectator is away the troops are off this client
                    if(lit&&ready&&posted.size()==4&&entities(c)!=null){c.options.hudHidden=true;wait=0;cs=1;}
                    else if(++wait%600==0)System.out.println("[p2-wait] spike step 0 after "+wait+" ticks: ready="+ready+", posted "+posted.size()+" of 4, on this client="+(entities(c)!=null)+"; "+litWhy(c));}
                case 1->{wait++;
                    if(wait==40){drawsBefore=TroopRenderer.troopDraws;var es=entities(c);for(int i=0;i<4;i++){var d=es==null?null:TroopRenderer.drawn(es.get(i));drawsAt[i]=d==null?0:d.count;}}
                    if(wait==60){checks(c);cs=2;wait=0;}}
                case 2->{wait++;
                    if(wait==1){if(shotReady(c,"spike-name-skin"))shot(c,"spike-name-skin");else wait--;}
                    if(wait==5)server(c,b->camera(b,-3,1.8,2.2,180,6));
                    if(wait==45){if(shotReady(c,"spike-name-model"))shot(c,"spike-name-model");else wait--;}
                    if(wait==50)server(c,b->{for(var u:posted)move(u.mob,u.mob.getX()-site.getX()-.5,0,210);camera(b,0,1.9,6,180,8);});
                    if(wait==95){if(shotReady(c,"spike-team-pennant")){shot(c,"spike-team-pennant");cs=5;wait=0;}else wait--;}}
                case 5->{wait++; // the death second: a gold Squire, its back to the camera 4 blocks off, is killed; a frame of its death second is caught
                    if(wait==1)server(c,b->{dying=post(b,gold,"knight",0,2.5,180);camera(b,0,1.75,6.5,180,4);});
                    if(wait==40){var e=dying==null?null:client(c,dying.mob);var l=e==null?null:TroopModels.resolve(e);
                        if(e==null||TroopRenderer.drawn(e)==null||l==null||!"gold".equals(l.side())||!shotReady(c,"spike-death")){if(++deathStall<300)wait--;else{deathCheck(c);cs=6;wait=0;}}
                        else{deathFrame=Frame.watch(e.getId(),x->x instanceof net.minecraft.entity.LivingEntity le&&le.deathTime>=6&&x.getScoreboardTeam()==null);killedAt=tick;
                            server(c,b->dying.mob.kill());}}
                    if(wait>40&&(deathFrame.done||tick-killedAt>=60)){deathCheck(c);cs=6;wait=0;}}
                case 6->{wait++; // the spawn frame: once the dead Squire has gone, a gold Squire is spawned off its team, which reaches this client TEAM_LATE ticks later; its first drawn frame is caught
                    if(bornAt<0){if(dying==null||client(c,dying.mob)==null||wait>=60){bornAt=wait;
                        spawnFrame=Frame.first(x->x.getCustomName()!=null&&x.getCustomName().getString().equals("Order Squire"));
                        server(c,b->{pins.removeIf(p->p.mob()==dying.mob);posted.remove(dying);b.units.remove(dying);
                            born=post(b,gold,"knight",0,2.5,180);GravesAndGlory.Battle.leaveSide(born.mob);});}}
                    else{int w=wait-bornAt;var be=born==null?null:client(c,born.mob);
                        if(be!=null&&bornSeen<0)bornSeen=w;if(be!=null&&bornDrawn<0&&TroopRenderer.drawn(be)!=null)bornDrawn=w;
                        if(w==TEAM_LATE)server(c,b->{if(born!=null)b.joinSide(born.mob,0);});
                        if(w>TEAM_LATE&&(spawnFrame.done||w>=TEAM_LATE+70)){spawnCheck(c);cs=7;wait=0;}}}
                case 7->{wait++; // the end of the hold: a gold Squire spawned off its team, which never comes, is first drawn HOLD_TICKS after this client had it, in linen
                    if(wait==1)server(c,b->{if(born!=null){pins.removeIf(p->p.mob()==born.mob);posted.remove(born);b.units.remove(born);GravesAndGlory.Battle.leaveSide(born.mob);born.mob.discard();}
                        linen=post(b,gold,"knight",0,2.5,180);GravesAndGlory.Battle.leaveSide(linen.mob);});
                    else{var le=linen==null?null:client(c,linen.mob);
                        if(le!=null&&linenSeen<0)linenSeen=wait;if(le!=null&&linenDrawn<0&&TroopRenderer.drawn(le)!=null)linenDrawn=wait;
                        if(linenDrawn>=0&&wait>=linenDrawn+5||wait>=200){linenCheck(c);cs=3;wait=0;}}}
                case 3->{wait++;
                    if(wait==1)portrait(c);
                    if(wait==30){portraitCheck(c);shot(c,"spike-portrait");}
                    if(wait==35){c.setScreen(null);server(c,this::clean);cs=4;}}
                case 4->{if(serverDone)end(c);}
                default->{}
            }
        }
        void checks(MinecraftClient c){
            var es=entities(c);var dispatcher=c.getEntityRenderDispatcher();
            String[] names={"Order Squire","Templar","Order Squire","Templar"},troops={"knight","sentinel","knight","sentinel"},sides={"gold","gold","violet","violet"};
            boolean renderer=true,named=true;StringBuilder rw=new StringBuilder(),nw=new StringBuilder();
            for(int i=0;i<4;i++){var e=es.get(i);var r=dispatcher.getRenderer(e);var l=TroopModels.resolve(e);
                var d=TroopRenderer.drawn(e);int drew=d==null?0:d.count-drawsAt[i];var last=d==null?null:d.last;
                boolean good=r instanceof TroopRenderer<?>&&drew>=10&&last!=null&&last.look().troop().id().equals(troops[i])&&sides[i].equals(last.cloth())&&TroopModels.model(troops[i])!=null;renderer&=good;
                rw.append(e.getCustomName().getString()).append(" -> ").append(r.getClass().getSimpleName()).append(", ").append(drew).append(" draws through its own model in the last 20 ticks")
                    .append(last==null?"":", the last as "+last.look().troop().id()+" in "+last.cloth()).append(good?"":" (BAD: want 10 or more, as "+troops[i]+" in "+sides[i]+")").append("; ");
                boolean n=e.getCustomName().getString().equals(names[i])&&l!=null&&l.troop().id().equals(troops[i])&&l.stage()==1&&!l.veteran()&&sides[i].equals(l.side());named&=n;
                nw.append("\"").append(e.getCustomName().getString()).append("\" -> ").append(l==null?"null":l.troop().id()+" stage "+l.stage()+" side "+l.side()).append(n?"":" (want "+names[i]+", "+troops[i]+" stage 1 side "+sides[i]+")").append("; ");}
            var h=es.get(4);var hr=dispatcher.getRenderer(h);boolean huskOk=hr instanceof TroopRenderer<?> tr&&TroopModels.resolve(h)==null&&tr.vanilla() instanceof net.minecraft.client.render.entity.HuskEntityRenderer&&TroopRenderer.drawn(h)==null;
            long draws=TroopRenderer.troopDraws-drawsBefore;renderer&=huskOk;
            report("p2-spike-renderer",renderer,rw+"the unnamed husk: "+hr.getClass().getSimpleName()+", resolve null, vanilla() a HuskEntityRenderer and never drawn through a troop model="+huskOk+"; "+draws+" troop-model draws in all in those 20 ticks");
            report("p2-spike-names",named,nw.toString());
            server(c,b->{var board=b.server.getScoreboard();boolean ok=true;StringBuilder why=new StringBuilder();
                for(int i=0;i<4;i++){var u=posted.get(i);var t=board.getPlayerTeam(u.mob.getEntityName());boolean on=t!=null&&t.getName().equals("gg_"+sides[i]);ok&=on;why.append(u.spec.id()).append(" (team ").append(u.squad.team).append(") on ").append(t==null?"no team":t.getName()).append(on?"":" (want gg_"+sides[i]+")").append("; ");}
                var ht=board.getPlayerTeam(husk.getEntityName());ok&=ht==null;
                report("p2-spike-teams",ok,why+"the husk on "+(ht==null?"no team":ht.getName()));});
            var km=TroopModels.model("knight");var sm=TroopModels.model("sentinel");
            List<String> common=List.of("head","body","right_arm","left_arm","right_hand","left_hand","right_leg","left_leg","pennant");
            boolean bones=km!=null&&sm!=null&&km.bones().containsAll(common)&&sm.bones().containsAll(common)&&km.bones().contains("iron_cap")&&sm.bones().containsAll(List.of("great_helm","long_tabard"));
            report("p2-spike-bones",bones,"knight bones "+(km==null?"none":km.bones())+"; sentinel bones "+(sm==null?"none":sm.bones()));
            if(km!=null&&sm!=null){var kb=km.restBounds();var sb=sm.restBounds();
                boolean feet=Math.abs(kb.maxY-24)<1e-3&&Math.abs(sb.maxY-24)<1e-3&&sb.minY<=kb.minY-2;
                report("p2-spike-feet",feet,String.format(Locale.ROOT,"rest bounds y (model px, ground 24): knight %.2f..%.2f, Templar %.2f..%.2f; the Templar's top is %.2f px above the knight's (want 2 or more)",kb.minY,kb.maxY,sb.minY,sb.maxY,kb.minY-sb.minY));}
            else report("p2-spike-feet",false,"no knight or sentinel model");
            List<String> bad=new ArrayList<>();int lines=0;
            try{for(String line:Files.readAllLines(c.runDirectory.toPath().resolve("logs/latest.log"),java.nio.charset.StandardCharsets.ISO_8859_1)){lines++;if(line.contains("[troop-models]")||line.contains("Failed to load model")&&line.contains("gravesandglory:troop/"))bad.add(line.trim());}}catch(IOException e){bad.add(e.toString());}
            // Every rig, weapon and troop that index.json lists has loaded: a client whose loader never ran logs nothing either. The
            // lists are read here with plain JSON, not with the loader's own index() (Task 5 review minor 2), and none may be empty.
            List<String> missing=new ArrayList<>();String listed;
            try{JsonObject ix;try(var in=c.getResourceManager().getResource(new Identifier("gravesandglory","models/troop/index.json")).orElseThrow().getReader()){ix=JsonParser.parseReader(in).getAsJsonObject();}
                Map<String,List<String>> lists=new LinkedHashMap<>();
                for(String key:List.of("rigs","weapons","troops")){List<String> names=new ArrayList<>();if(ix.has(key))for(var el:ix.getAsJsonArray(key))names.add(el.getAsString());lists.put(key,names);
                    if(names.isEmpty())missing.add("(index.json lists no "+key+")");}
                for(String r:lists.get("rigs"))if(TroopModels.rig(r)==null)missing.add("rig/"+r);
                for(String w:lists.get("weapons"))if(TroopModels.weapon(w)==null)missing.add("weapon/"+w);
                for(String t:lists.get("troops"))if(TroopModels.model(t)==null)missing.add(t);
                listed=lists.get("rigs").size()+" rigs, "+lists.get("weapons").size()+" weapons and "+lists.get("troops").size()+" troops";}
            catch(Exception e){missing.add("index.json: "+e);listed="nothing";}
            report("p2-spike-load",bad.isEmpty()&&lines>0&&missing.isEmpty(),lines+" lines of latest.log read; "+(bad.isEmpty()?"no [troop-models] line and no Failed to load model naming gravesandglory:troop/":bad.size()+" bad, first: "+bad.get(0))
                +"; index.json lists "+listed+", "+(missing.isEmpty()?"all loaded":"not loaded: "+missing)+"; models loaded: "+TroopModels.ids());
            loaderCheck(c);
        }
        /** p2-spike-death: the frame caught in the death second shows the Squire in gold, chest panel and flag, though this client has it on no team. */
        void deathCheck(MinecraftClient c){
            var f=deathFrame;var m=TroopModels.model("knight");int gold=Frame.colour("gold");
            if(f==null){var e=dying==null?null:client(c,dying.mob);report("p2-spike-death",false,"the Squire was never drawn through its own model on its side in 300 ticks, so it was not killed: on this client="+(e!=null)+", drawn="+(e!=null&&TroopRenderer.drawn(e)!=null)+", look "+(e==null?"-":TroopModels.resolve(e))+(m==null?" (no knight model)":""));return;}
            if(!f.done||m==null){report("p2-spike-death",false,"no frame of the Squire's death second, off its team on this client, was drawn within 60 ticks of the kill"+(m==null?" (no knight model)":""));return;}
            var d=f.draw;List<String> bones=d==null?List.of():m.shownBones(d.bones());
            int[] panel=f.face(m,"chest_panel",0),flag=f.face(m,"pennant",1,8);double ps=f.sideShare(panel,gold),fs=f.sideShare(flag,gold);
            boolean ok=d!=null&&f.deathTime>=6&&"gold".equals(d.look().side())&&"gold".equals(d.cloth())&&bones.contains("chest_panel")&&bones.contains("pennant")&&ps>=0.5&&fs>=0.3;
            f.save(c,"spike-death");f.saveProbe(c,"spike-death-probe",panel,flag);
            report("p2-spike-death",ok,String.format(Locale.ROOT,"a frame at deathTime %d, the Squire on no team on this client: drawn with side %s, cloth %s (null is linen), chest panel and pennant shown=%s; the chest panel %s is %.0f%% gold, the flag %s %.0f%% (want 50 and 30 or more: the flag's white glyph fills its middle, so on the sheet a gold flag is 44%% gold at this inset and a linen one 0)",
                f.deathTime,d==null?"-":d.look().side(),d==null?"-":d.cloth(),bones.contains("chest_panel")&&bones.contains("pennant"),Arrays.toString(panel),ps*100,Arrays.toString(flag),fs*100));
            f.close();
        }
        /** p2-spike-spawn: the first frame that draws a Squire spawned off its team (the team comes TEAM_LATE ticks later) is not linen. */
        void spawnCheck(MinecraftClient c){
            var f=spawnFrame;var m=TroopModels.model("knight");int gold=Frame.colour("gold");
            var e=born==null?null:client(c,born.mob);var d=e==null?null:TroopRenderer.drawn(e);
            if(f==null||!f.done||m==null||d==null||d.first==null){report("p2-spike-spawn",false,"the spawned Squire was "+(e==null?"never on this client":d==null?"never drawn":"drawn, but its first frame was not caught")+" in "+(TEAM_LATE+70)+" ticks");return;}
            var first=d.first;List<String> bones=m.shownBones(first.bones());
            int[] panel=f.face(m,"chest_panel",0),flag=f.face(m,"pennant",1,8);double ps=f.sideShare(panel,gold),fs=f.sideShare(flag,gold);
            double turn=Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(f.bodyYaw-180));
            boolean ok=f.entity==e&&bornDrawn>=TEAM_LATE&&"gold".equals(first.look().side())&&"gold".equals(first.cloth())&&bones.contains("pennant")&&turn<=20&&ps>=0.5&&fs>=0.3;
            f.save(c,"spike-spawn");f.saveProbe(c,"spike-spawn-probe",panel,flag);
            report("p2-spike-spawn",ok,String.format(Locale.ROOT,"spawned off its team once the dead Squire had gone, the team sent %d ticks later: on this client %d ticks after the spawn, first drawn at %d (want %d or later: held until its side came), with side %s, cloth %s (null is linen), pennant shown=%s; in that first frame its back to the camera (body yaw %.0f, want 180 within 20), the chest panel %.0f%% gold, the flag %.0f%% (want 50 and 30 or more)",
                TEAM_LATE,bornSeen,bornDrawn,TEAM_LATE,first.look().side(),first.cloth(),bones.contains("pennant"),f.bodyYaw,ps*100,fs*100));
            f.close();
        }
        /** p2-spike-linen: a Squire whose team never reaches this client waits undrawn HOLD_TICKS (40), within a few, then draws in linen. */
        void linenCheck(MinecraftClient c){
            var e=linen==null?null:client(c,linen.mob);var d=e==null?null:TroopRenderer.drawn(e);var first=d==null?null:d.first;int held=linenSeen<0||linenDrawn<0?-1:linenDrawn-linenSeen;
            boolean sheet=first!=null&&first.clothSheet()!=null&&first.clothSheet().equals(TroopModels.cloth(null));
            boolean ok=first!=null&&first.look().side()==null&&first.cloth()==null&&sheet&&e.getScoreboardTeam()==null&&held>=TroopModels.HOLD_TICKS-3&&held<=TroopModels.HOLD_TICKS+5;
            report("p2-spike-linen",ok,String.format(Locale.ROOT,"spawned off its team and never given one: on this client at tick %d of the step, first drawn at %d, so held %d ticks (want %d, from %d to %d); the first draw with side %s, cloth sheet %s (want %s), on no team=%s",
                linenSeen,linenDrawn,held,TroopModels.HOLD_TICKS,TroopModels.HOLD_TICKS-3,TroopModels.HOLD_TICKS+5,first==null?"-":first.look().side(),first==null?"-":first.clothSheet(),TroopModels.cloth(null),e!=null&&e.getScoreboardTeam()==null));
        }
        /** p2-spike-loader: TroopModelLoader refuses broken files with a reason (each would leave its troop vanilla, or every troop for index.json), and takes a box without dilation. */
        void loaderCheck(MinecraftClient c){
            List<String> bad=new ArrayList<>();StringBuilder why=new StringBuilder();
            String bone="{\"name\":\"root\",\"parent\":\"\",\"pivot\":[0,0,0],\"boxes\":[{\"origin\":[0,0,0],\"size\":[1,1,1],\"uv\":[0,0]}]}";
            refuse(bad,why,"index troops {}",()->TroopModelLoader.index(json("{\"format\":1,\"troops\":{}}")));
            refuse(bad,why,"index troops [3]",()->TroopModelLoader.index(json("{\"format\":1,\"troops\":[3]}")));
            refuse(bad,why,"index format 2",()->TroopModelLoader.index(json("{\"format\":2,\"troops\":[]}")));
            refuse(bad,why,"no skins",()->TroopModelLoader.checkTroop(json("{\"troop\":\"knight\"}")));
            refuse(bad,why,"skins without stage 1",()->TroopModelLoader.checkTroop(json("{\"skins\":{\"2\":\"gravesandglory:x.png\"}}")));
            refuse(bad,why,"a skin for stage x",()->TroopModelLoader.checkTroop(json("{\"skins\":{\"1\":\"gravesandglory:x.png\",\"x\":\"gravesandglory:y.png\"}}")));
            refuse(bad,why,"a missing texture",()->TroopModelLoader.texture(c.getResourceManager(),new Identifier("gravesandglory","textures/entity/troop/no_such_skin.png")));
            refuse(bad,why,"a parent not listed before",()->TroopModelLoader.parse(json("{\"format\":1,\"size\":[64,64],\"bones\":["+bone+",{\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,0,0],\"boxes\":[]}]}")));
            refuse(bad,why,"a bone without boxes",()->TroopModelLoader.parse(json("{\"format\":1,\"size\":[64,64],\"bones\":[{\"name\":\"root\",\"parent\":\"\",\"pivot\":[0,0,0]}]}")));
            String ok="{\"format\":1,\"troop\":\"probe\",\"rig\":\"biped\",\"size\":[64,64],\"skins\":{\"1\":\"gravesandglory:textures/entity/troop/knight_s1.png\"},\"bones\":["+bone+"]}";
            boolean took;try{var j=json(ok);TroopModelLoader.checkTroop(j);new TroopModel<>(j,TroopModelLoader.parse(j).createModel(),Map.of("right","root","left","root"),null,List.of());took=true;why.append("a box without dilation loads; ");}
            catch(Exception e){took=false;why.append("a box without dilation refused: ").append(e).append(" (BAD); ");}
            report("p2-spike-loader",bad.isEmpty()&&took,why.toString().trim());
        }
        interface Load{void run()throws Exception;}
        static void refuse(List<String> bad,StringBuilder why,String what,Load load){
            try{load.run();bad.add(what);why.append(what).append(": loaded (BAD); ");}
            catch(IllegalArgumentException e){why.append(what).append(": \"").append(e.getMessage()).append("\"; ");}
            catch(Exception e){bad.add(what);why.append(what).append(": ").append(e).append(", not a refusal with a reason (BAD); ");}
        }
        MobEntity portraitMob;int portraitDraws;
        /** Opens the portrait probe on the Squire's and the Templar's stage 1 portraits, drawn as the cards draw them. */
        void portrait(MinecraftClient c){
            var e=ArmyCards.portrait(ArmyCards.spec("sentinel"),1);portraitMob=e;var d=e==null?null:TroopRenderer.drawn(e);portraitDraws=d==null?0:d.count;
            c.setScreen(new PortraitProbe(List.of(ArmyCards.portrait(ArmyCards.spec("knight"),1),e)));
        }
        /** p2-spike-portrait, once the probe has drawn for 29 ticks: what the renderer drew for the Templar's portrait (no side, the company's cloth, the tabard and no pennant), and its shield. */
        void portraitCheck(MinecraftClient c){
            var spec=ArmyCards.spec("sentinel");var e=portraitMob;var l=e==null?null:TroopModels.resolve(e);
            boolean look=l!=null&&l.troop()==spec&&l.stage()==1&&l.side()==null&&e.getCustomName().getString().equals("Templar")&&e.getScoreboardTeam()==null;
            var d=e==null?null:TroopRenderer.drawn(e);int n=d==null?0:d.count-portraitDraws;var last=d==null?null:d.last;var m=TroopModels.model("sentinel");
            List<String> shown=last==null||m==null?List.of():m.shownBones(last.bones());var tex=TroopModels.cloth("company");
            boolean company=last!=null&&last.look().side()==null&&"company".equals(last.cloth())&&c.getResourceManager().getResource(tex).isPresent();
            boolean noPennant=n>=5&&shown.contains("long_tabard")&&!shown.contains("pennant");
            var tag=e==null?null:net.minecraft.item.BlockItem.getBlockEntityNbt(e.getOffHandStack());
            boolean shield=tag!=null&&tag.getInt("Base")==net.minecraft.util.DyeColor.GRAY.getId()&&tag.getList("Patterns",10).size()==1&&tag.getList("Patterns",10).getCompound(0).getString("Pattern").equals("sc");
            report("p2-spike-portrait",look&&company&&noPennant&&shield,"ArmyCards.portrait(sentinel, 1): named \""+(e==null?"":e.getCustomName().getString())+"\", resolves "+(l==null?"null":l.troop().id()+" stage "+l.stage()+" side "+l.side())+", no scoreboard team="+look
                +"; drawn "+n+" times by the probe (want 5 or more), the last in cloth "+(last==null?"-":last.cloth())+" ("+tex+"; Kort's portrait ruling replaced linen with the company's navy)="+company
                +"; the last draw showed the tabard and no pennant="+noPennant+" "+shown+"; shield "+(tag==null?"none":tag)+" (grey field, white straight cross)="+shield);
        }
        void clean(GravesAndGlory.Battle b){
            var board=b.server.getScoreboard();boolean ok=true;StringBuilder why=new StringBuilder("killed: ");
            for(var u:posted){u.mob.kill();var t=board.getPlayerTeam(u.mob.getEntityName());boolean off=!u.mob.isAlive()&&t==null;ok&=off;why.append(u.spec.id()).append(u.mob.isAlive()?" alive":" dead").append(t==null?" off its team":" still on "+t.getName()).append("; ");}
            pins.clear();b.units.removeAll(posted);posted.clear();
            b.campaign.returnLobby();why.append("after returnLobby: ");
            for(var f:Spec.FACTIONS){var t=board.getTeam("gg_"+f.id());int n=t==null?0:t.getPlayerList().size();ok&=n==0;why.append("gg_").append(f.id()).append(t==null?" absent":" "+n+" members").append("; ");}
            report("p2-teams-clean",ok,why.toString());
            husk.discard();unforce(b);serverDone=true;
        }
    }

    /**
     * Task 5 Step 12: the Gilded proof set in game. The six Gilded troops in a row for each team at stage 1, then each gold
     * troop alone in the studio (4 blocks, turned 30 degrees), then the later stages' close-ups, each evolution line's three
     * stages side by side (Step 15), the kit (weapons, glow, shields)
     * and the recruit page's Gilded tab.
     */
    static final class GildedArt extends Stage {
        static final List<String> SIX=List.of("knight","archer","sentinel","longbow","mage","gl_marshal");
        final Map<String,GravesAndGlory.Unit> golds=new LinkedHashMap<>(),violets=new LinkedHashMap<>();
        /** The close-ups after the six at stage 1: the knight at stages 3 and 2, and the other lines at stage 3 (Step 15). */
        static final List<Map.Entry<String,Integer>> LATER=List.of(Map.entry("knight",3),Map.entry("knight",2),Map.entry("archer",3),Map.entry("sentinel",3),Map.entry("longbow",3));
        /** The evolution lines shot side by side, and the parts each shows at stage 3 only (the houses spec's 2.7 extras). */
        static final List<String> LINES=List.of("knight","archer","sentinel","longbow");
        static final Map<String,List<String>> STAGE3=Map.of("knight",List.of("crest_plume"),"archer",List.of("signal_horn"),"sentinel",List.of("cape","cape_skirt_right","cape_skirt_left"),"longbow",List.of("long_feather"));
        final Map<String,List<GravesAndGlory.Unit>> lines=new java.util.concurrent.ConcurrentHashMap<>();final Set<GravesAndGlory.Unit> staged=java.util.concurrent.ConcurrentHashMap.newKeySet();
        int li;boolean stagesOk=true;final StringBuilder stagesWhy=new StringBuilder();
        int ci;boolean closeOk=true;final StringBuilder closeWhy=new StringBuilder();Frame cullFrame;float cullPitch;
        /** The sides shot's four knights (gold, violet, crimson, teal) and the cards' portraits' draw counts before the Gilded tab drew. */
        final List<GravesAndGlory.Unit> sided=new ArrayList<>();final Map<String,Integer> cardDraws=new HashMap<>();
        /** The side change (Task 5 review minor 4): the suite tick the crimson Squire was first drawn (on gold) and the tick it moved to crimson. */
        int firstGold=-1,movedAt=-1;
        static final List<String> FOUR=List.of("gold","violet","crimson","teal");
        /**
         * The cull shot's camera: 1.1 blocks in front of Aldric, the eye 2.6 blocks up, between the top of his vanilla box (the
         * husk's 1.95 grown by half a block: 2.45) and the top of his standard's cloth (2.79); it looks up until the view's
         * bottom edge lies CULL_DIP degrees under the eye line, so every corner of the vanilla box is under the view and the
         * cloth's top rows are in it.
         */
        static final double CULL_EYE=2.6,CULL_DIP=2;
        String name(){return "gilded-art";}
        /** The studio: 12 blocks south of the lineup; a troop there faces north, turned 30 degrees, the camera 4 blocks north of it looking south. */
        static final double STUDIO=12;
        void cast(GravesAndGlory.Battle b){
            for(int i=0;i<6;i++){golds.put(SIX.get(i),post(b,gold,SIX.get(i),-6.25+i*2.5,0,0));violets.put(SIX.get(i),post(b,violet,SIX.get(i),-5+i*2.5,-3,0));}
            camera(b,0,2.6,9.5,180,9);
        }
        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
            if(finished)return;
            if(tick>12000){report("p2-gilded-timeout",false,"stuck at client step "+cs+", server stage "+stage);end(c);return;}
            switch(cs){
                case 0->{boolean lit=litStart(c); // first: while the spectator is away the troops are off this client
                    if(lit&&ready&&posted.size()==12&&posted.stream().allMatch(u->{var e=client(c,u.mob);return e!=null&&e.getCustomName()!=null;})){c.options.hudHidden=true;wait=0;cs=1;}
                    else if(++wait%600==0)System.out.println("[p2-wait] gilded-art step 0 after "+wait+" ticks: ready="+ready+", posted "+posted.size()+" of 12; "+litWhy(c));}
                case 1->{wait++;if(wait==60){if(shotReady(c,"gilded-lineup")){lineup(c);shot(c,"gilded-lineup");cs=2;wait=0;ci=0;}else wait--;}}
                case 2->{wait++; // one gold troop at a time in the studio at stage 1, then the later stages (LATER)
                    String troop=ci<6?SIX.get(ci):LATER.get(ci-6).getKey();int st=ci<6?1:LATER.get(ci-6).getValue();
                    if(wait==1){server(c,b->{var u=golds.get(troop);move(u.mob,0,STUDIO,180+30);if(st>1)stage(u,st);camera(b,0,1.45,STUDIO-4,0,6);});}
                    if(wait==45){String n=st==1?"gilded-"+troop:"gilded-"+troop+"-s"+st;if(shotReady(c,n)){close(c,troop,st);shot(c,n);}else wait--;}
                    if(wait==50){server(c,b->{var u=golds.get(troop);if(st>1)stage(u,1);move(u.mob,-6.25+SIX.indexOf(troop)*2.5,0,0);});ci++;wait=0;if(ci>=6+LATER.size()){report("p2-gilded-close",closeOk,"each close-up's troop was last drawn through its own model at the stage shot, in gold, 4 blocks from the camera, its stage 3 parts drawn at stage 3 only: "+closeWhy.toString().trim());cs=3;}}}
                case 3->{wait++; // each line's stages side by side in the studio, stage 1 on the left (the camera looks south: +x is its left)
                    String line=LINES.get(li);
                    if(wait==1)server(c,b->{List<GravesAndGlory.Unit> us=new ArrayList<>();for(int s=1;s<=3;s++){var u=post(b,gold,line,(2-s)*2.0,STUDIO,180+20);stage(u,s);us.add(u);staged.add(u);}lines.put(line,us);camera(b,0,1.6,STUDIO-5,0,8);});
                    if(wait==50){String n=line.equals("knight")?"gilded-stages":"gilded-stages-"+line;if(lines.containsKey(line)&&shotReady(c,n)){stagesCheck(c,line);shot(c,n);}else wait--;}
                    if(wait==55){var us=lines.get(line);server(c,b->{for(var u:us){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();pins.removeIf(p->p.mob()==u.mob);b.units.remove(u);posted.remove(u);}});
                        li++;wait=0;if(li==LINES.size()){report("p2-gilded-stages",stagesOk,stagesWhy.toString().trim());kit(c);cs=5;}}}
                case 5->{wait++; // the culling box: the camera 1.1 blocks in front of Aldric looks up at his standard, his body and the vanilla box below the view
                    if(wait==1){fovScale(c,0.0);cullPitch=-(float)(c.options.getFov().getValue()/2.0-CULL_DIP);final float pitch=cullPitch;
                        server(c,b->{move(golds.get("gl_marshal").mob,0,STUDIO,180);camera(b,0,CULL_EYE,STUDIO-1.1,0,pitch);});}
                    if(wait==40){var e=client(c,golds.get("gl_marshal").mob);Vec3d at=at(0,STUDIO);
                        if(e==null||e.squaredDistanceTo(at)>0.01||!lit(c))wait--;else cullFrame=Frame.watch(e.getId(),x->x.squaredDistanceTo(at)<0.01);}
                    if(wait>40&&(cullFrame.done||wait>=80)){cullCheck(c);fovScale(c,-1);server(c,b->move(golds.get("gl_marshal").mob,-6.25+SIX.indexOf("gl_marshal")*2.5,0,0));cs=6;wait=0;}}
                case 6->{wait++; // one troop on all four sides (finding 11): four Squires in the studio, gold, violet, crimson and teal from the left, 4 blocks off;
                    // the crimson one is posted on gold and moved to crimson 20 ticks after this client first drew it (a side change on a troop already drawn)
                    if(wait==1)server(c,b->{
                        for(int i=0;i<4;i++){var u=post(b,i==1?violet:gold,"knight",(1.5-i)*1.6,STUDIO,180+20);sided.add(u);if(i==3)toSide(b,u,FOUR.get(i));}
                        camera(b,0,1.45,STUDIO-4,0,6);});
                    if(wait>1&&firstGold<0&&sided.size()==4){var e=client(c,sided.get(2).mob);var d=e==null?null:TroopRenderer.drawn(e);if(d!=null&&d.count>0)firstGold=tick;}
                    if(firstGold>=0&&movedAt<0&&tick-firstGold>=20){movedAt=tick;server(c,b->toSide(b,sided.get(2),FOUR.get(2)));}
                    if(wait==50){if(sided.size()==4&&movedAt>=0&&tick-movedAt>=20&&shotReady(c,"gilded-sides")){sidesCheck(c);shot(c,"gilded-sides");}else wait--;}
                    if(wait==55){server(c,b->{for(var u:sided){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();pins.removeIf(p->p.mob()==u.mob);b.units.remove(u);posted.remove(u);}});cs=4;wait=0;}}
                case 4->{wait++; // the recruit page's Gilded tab, every Gilded troop owned so no card is dimmed
                    if(wait==1)server(c,b->{b.campaign.returnLobby();var pr=b.campaign.profile(player);for(String id:SIX)pr.owned.putIfAbsent(id,new Profile.Owned());pins.clear();posted.clear();});
                    if(wait==40){CampaignClient.revealUntil=0;CampaignClient.notice="";c.setScreen(new CampScreen("recruit"));}
                    if(wait==45&&c.currentScreen instanceof CampScreen s){s.pickHouse("gilded");
                        for(String id:SIX){var e=ArmyCards.portrait(ArmyCards.spec(id));var d=e==null?null:TroopRenderer.drawn(e);cardDraws.put(id,d==null?0:d.count);}}
                    if(wait==80){boolean six=c.currentScreen instanceof CampScreen s&&s.house.equals("gilded")&&CampScreen.cards("gilded",CampaignClient.profile).stream().allMatch(CampScreen.Card::owned);
                        boolean models=true;StringBuilder why=new StringBuilder();
                        for(String id:SIX){var e=ArmyCards.portrait(ArmyCards.spec(id));var d=e==null?null:TroopRenderer.drawn(e);var last=d==null?null:d.last;var m=TroopModels.model(id);
                            int n=d==null?0:d.count-cardDraws.getOrDefault(id,0);List<String> shown=last==null||m==null?List.of():m.shownBones(last.bones());
                            boolean good=n>=5&&last!=null&&last.look().troop().id().equals(id)&&last.look().side()==null&&"company".equals(last.cloth())&&!shown.contains("pennant")&&!shown.contains("standard");models&=good;
                            why.append(id).append(" drawn ").append(n).append(" times").append(last==null?"":" in "+last.cloth()).append(good?"":" (BAD)").append("; ");}
                        report("p2-gilded-cards",six&&models,"the recruit page on the Gilded tab with all six owned="+six+"; in the 35 ticks after the tab opened, each card's portrait was drawn through its own model, no side, in the company's cloth and with no flag (want 5 draws or more): "+why.toString().trim());shot(c,"gilded-cards");}
                    if(wait==90){c.setScreen(null);serverDone=true;end(c);}}
                default->{}
            }
        }
        void lineup(MinecraftClient c){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(var u:posted){var e=client(c,u.mob);var d=e==null?null:TroopRenderer.drawn(e);var l=d==null?null:d.last.look();String side=u.squad==gold?"gold":"violet";
                boolean good=l!=null&&l.troop().id().equals(u.spec.id())&&l.stage()==1&&side.equals(l.side())&&side.equals(d.last.cloth())&&TroopModels.model(u.spec.id())!=null;
                ok&=good;why.append(u.spec.id()).append("/").append(l==null?"not drawn":d.last.cloth()).append(good?"":" (BAD)").append(" ");}
            report("p2-gilded-lineup",ok,"each Gilded troop's last draw is its own model at stage 1 in its side's cloth: "+why.toString().trim());
        }
        /** A close-up (p2-gilded-close): the troop in the studio resolves to its own model at the stage shot, on gold, 4 blocks from the camera. */
        void close(MinecraftClient c,String troop,int stage){
            var e=client(c,golds.get(troop).mob);var dr=e==null?null:TroopRenderer.drawn(e);var l=dr==null?null:dr.last.look();
            double d=e==null||c.player==null?-1:Math.hypot(e.getX()-c.player.getX(),e.getZ()-c.player.getZ());
            var m=TroopModels.model(troop);List<String> parts=STAGE3.getOrDefault(troop,List.of()),shown=dr==null||m==null?List.of():m.shownBones(dr.last.bones());
            long on=parts.stream().filter(shown::contains).count();boolean partsOk=stage==3?on==parts.size():on==0;
            boolean good=l!=null&&l.troop().id().equals(troop)&&l.stage()==stage&&"gold".equals(l.side())&&"gold".equals(dr.last.cloth())&&m!=null&&Math.abs(d-4)<0.25&&partsOk;closeOk&=good;
            closeWhy.append(troop).append(" stage ").append(l==null?"-":String.valueOf(l.stage())).append(" side ").append(l==null?"-":l.side()).append(String.format(Locale.ROOT," at %.2f blocks",d))
                .append(parts.isEmpty()?"":", "+String.join("+",parts)+" "+(on==parts.size()?"drawn":on==0?"not drawn":on+" of "+parts.size()+" drawn")).append(good?"":" (BAD)").append("; ");
        }
        /** p2-gilded-cull: with only the top of Aldric's standard in view, at the bottom of the frame, he is drawn (the vanilla box test would drop him), the standard's cloth on the frame in gold. */
        void cullCheck(MinecraftClient c){
            var f=cullFrame;var m=TroopModels.model("gl_marshal");int gold=Frame.colour("gold");
            if(f==null||!f.done||m==null){report("p2-gilded-cull",false,"Aldric was not drawn in the 40 ticks the camera looked up at his standard"+(m==null?" (no model)":"")+": the frustum test dropped him");return;}
            List<String> bones=f.draw==null?List.of():m.shownBones(f.draw.bones());int[] cloth=f.face(m,"standard",3,8);
            int on=f.onScreen(cloth);double share=f.sideShare(cloth,gold),half=Math.toDegrees(Math.atan(1/f.proj.m11()));
            boolean ok=!f.vanillaSees&&f.draw!=null&&"gold".equals(f.draw.cloth())&&bones.contains("standard")&&on>=300&&share>=0.3;
            f.save(c,"gilded-cull");f.saveProbe(c,"gilded-cull-probe",cloth);
            report("p2-gilded-cull",ok,String.format(Locale.ROOT,"the camera at %.2f, %.2f, %.2f, pitch %.1f (the view %.1f degrees each way up and down), Aldric at %.2f, %.2f, %.2f: the vanilla box test would draw him=%s (want false); drawn with cloth %s, standard shown=%s; its cloth at %s, %d pixels of it on the %dx%d frame (want 300 or more), %.0f%% of those gold (want 30 or more)",
                f.cam.x,f.cam.y,f.cam.z,cullPitch,half,f.pos.x,f.pos.y,f.pos.z,f.vanillaSees,f.draw==null?"-":f.draw.cloth(),bones.contains("standard"),Arrays.toString(cloth),on,f.image.getWidth(),f.image.getHeight(),share*100));
            f.close();
        }
        /** Moves a posted troop to a side's gg_ team, with that side's painted shield. */
        static void toSide(GravesAndGlory.Battle b,GravesAndGlory.Unit u,String side){
            var board=b.server.getScoreboard();String team="gg_"+side;var t=board.getTeam(team);if(t==null)t=board.addTeam(team);board.addPlayerToTeam(u.mob.getEntityName(),t);
            for(var f:Spec.FACTIONS)if(f.id().equals(side))u.mob.equipStack(EquipmentSlot.OFFHAND,GravesAndGlory.Battle.shield(u.spec,f.dye()));
        }
        /** p2-gilded-sides: the four Squires were last drawn each in its own side's cloth, with its flag; the crimson one was first drawn in gold, before its move. */
        void sidesCheck(MinecraftClient c){
            var m=TroopModels.model("knight");boolean ok=m!=null;StringBuilder why=new StringBuilder();
            for(int i=0;i<4;i++){var e=client(c,sided.get(i).mob);var d=e==null?null:TroopRenderer.drawn(e);var last=d==null?null:d.last;var first=d==null?null:d.first;
                List<String> shown=last==null||m==null?List.of():m.shownBones(last.bones());
                boolean good=last!=null&&FOUR.get(i).equals(last.look().side())&&FOUR.get(i).equals(last.cloth())&&shown.contains("pennant")&&shown.contains("chest_panel");
                if(i==2)good&=first!=null&&"gold".equals(first.look().side())&&"gold".equals(first.cloth())&&movedAt>=0;ok&=good;
                why.append(FOUR.get(i)).append(": ").append(last==null?"not drawn":"drawn in "+last.cloth()+", flag "+(shown.contains("pennant")?"shown":"hidden"))
                    .append(i==2?" (first drawn in "+(first==null?"-":first.cloth())+" at suite tick "+firstGold+", moved to crimson at "+movedAt+")":"").append(good?"":" (BAD)").append("; ");}
            report("p2-gilded-sides",ok,"one Squire on each side, left to right on the frame: "+why.toString().trim());
        }
        /** One line of p2-gilded-stages: each of its three troops was last drawn at its own stage, in that stage's skin, with the line's stage 3 parts at stage 3 only. */
        void stagesCheck(MinecraftClient c,String line){
            var m=TroopModels.model(line);List<String> parts=STAGE3.get(line);boolean ok=m!=null&&m.bones().containsAll(parts);
            stagesWhy.append(line).append(m!=null&&m.bones().containsAll(parts)?"":" (BAD: the model lacks "+parts+")").append(": ");
            for(var u:lines.get(line)){var e=client(c,u.mob);var d=e==null?null:TroopRenderer.drawn(e);var l=d==null?null:d.last.look();
                if(l==null||m==null){ok=false;stagesWhy.append("stage ").append(u.stage).append(" not drawn (BAD); ");continue;}
                List<String> shown=m.shownBones(d.last.bones());long on=parts.stream().filter(shown::contains).count();
                // the skin the draw bound (TroopRenderer.getTexture, observed), against the file the line and the stage name (Task 5 review minor 2)
                var skin=d.last.skin();var want=new Identifier("gravesandglory","textures/entity/troop/"+u.spec.id()+"_s"+u.stage+".png");
                boolean skinOk=want.equals(skin)&&c.getResourceManager().getResource(skin).isPresent();
                boolean good=l.stage()==u.stage&&(l.stage()==3?on==parts.size():on==0)&&skinOk;ok&=good;
                stagesWhy.append("\"").append(e.getCustomName().getString()).append("\" drawn at stage ").append(l.stage()).append(" in ").append(skin==null?"no skin":skin.getPath().substring(skin.getPath().lastIndexOf('/')+1))
                    .append(", ").append(String.join("+",parts)).append(" ").append(on==parts.size()?"drawn":on==0?"not drawn":on+" of "+parts.size()+" drawn").append(good?"":" (BAD)").append("; ");}
            stagesOk&=ok;
        }
        void kit(MinecraftClient c){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String id:SIX){var m=TroopModels.model(id);var w=m==null?null:TroopModels.weapon(m.weaponId());boolean good=w!=null&&c.getResourceManager().getResource(w.texture()).isPresent();ok&=good;why.append(id).append(" weapon ").append(m==null?"no model":m.weaponId()).append(good?"":" (BAD)").append("; ");}
            var mage=client(c,golds.get("mage").mob);var ml=mage==null?null:TroopModels.resolve(mage);var glow=ml==null?null:TroopModels.glow(ml);
            boolean lit=glow!=null&&c.getResourceManager().getResource(glow).isPresent();ok&=lit;why.append("the Sunweaver's glow ").append(glow).append(lit?"":" (BAD)").append("; ");
            final boolean client=ok;final String clientWhy=why.toString();
            server(c,b->{boolean shields=true;StringBuilder sw=new StringBuilder();
                for(var u:posted){if(!List.of("knight","sentinel","gl_marshal").contains(u.spec.id())||staged.contains(u)||sided.contains(u))continue;
                    var tag=net.minecraft.item.BlockItem.getBlockEntityNbt(u.mob.getOffHandStack());String dye=b.side(u.squad.team).dye();
                    StringBuilder want=new StringBuilder();for(String layer:u.spec.shield().split(","))want.append(layer).append(",");
                    StringBuilder got=new StringBuilder();if(tag!=null){var list=tag.getList("Patterns",10);for(int i=0;i<list.size();i++){var p=list.getCompound(i);got.append(net.minecraft.util.DyeColor.byId(p.getInt("Color")).getName()).append(":").append(p.getString("Pattern")).append(",");}}
                    boolean good=u.mob.getOffHandStack().isOf(net.minecraft.item.Items.SHIELD)&&tag!=null&&tag.getInt("Base")==net.minecraft.util.DyeColor.byName(dye,null).getId()&&got.toString().equals(want.toString());shields&=good;
                    sw.append(u.spec.id()).append(" ").append(b.side(u.squad.team).id()).append(": base ").append(tag==null?"none":net.minecraft.util.DyeColor.byId(tag.getInt("Base")).getName()).append(" (want ").append(dye).append("), patterns ").append(got).append(" (want ").append(want).append(")").append(good?"":" (BAD)").append("; ");}
                report("p2-gilded-kit",client&&shields,clientWhy+sw);});
        }
    }

    /**
     * Task 5b: the rig animation sets. The four rigs' clip tables (p2-anim-cover, -change and -return), the clip choice
     * (-choose), the stance sync over stance_v1 (-stance), a scene pose (-scene), a killed knight's death (-death) and the fold
     * (-fold) on gold knights on the platform, the 18 clips on a gold knight frozen through TroopAnimator.forced
     * (anim-biped-<clip>), and the contact sheet's frames (anim-sheet-*, SheetProbe).
     */
    static final class Anim extends Stage {
        static final List<String> RIGS=List.of("biped","hound","spider","colossus");
        String name(){return "anim";}
        /** The shot knight (in the studio), the scene knight and the one killed (in the lineup), and the stance drill: its own squad (gold), three knights and the one spawned into it. */
        volatile GravesAndGlory.Unit shotKnight,sceneKnight,victim,late;volatile GravesAndGlory.Squad drill;final List<GravesAndGlory.Unit> group=new java.util.concurrent.CopyOnWriteArrayList<>();
        int si,since,clip,frame;
        void cast(GravesAndGlory.Battle b){
            gold.order="follow"; // stance "": the scene knight's base is idle
            var d=new GravesAndGlory.Squad(UUID.randomUUID(),0,GravesAndGlory.Battle.base(0));d.order="follow";drill=d;
            shotKnight=post(b,gold,"knight",0,12,230);sceneKnight=post(b,gold,"knight",-2.5,0,180);victim=post(b,gold,"knight",2.5,0,180);
            for(int i=0;i<3;i++)group.add(post(b,d,"knight",-7+i*1.5,5,180));
            camera(b,0,1.6,-6,0,8);
        }
        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
            if(finished)return;
            if(tick>9000){report("p2-anim-timeout",false,"stuck at client step "+cs+" ("+si+"), server stage "+stage);end(c);return;}
            switch(cs){
                case 0->{boolean lit=litStart(c); // first: while the spectator is away the troops are off this client
                    if(lit&&ready&&posted.size()==6&&posted.stream().allMatch(u->client(c,u.mob)!=null)){c.options.hudHidden=true;wait=0;cs=1;}
                    else if(++wait%600==0)System.out.println("[p2-wait] anim step 0 after "+wait+" ticks: ready="+ready+", posted "+posted.size()+" of 6; "+litWhy(c));}
                case 1->{if(++wait==20){cover();change();returns();choose();cs=2;wait=0;si=0;}}
                case 2->stance(c,tick);
                case 3->scene(c,tick);
                case 4->death(c,tick);
                case 5->{fold(c);cs=6;wait=0;clip=0;server(c,b->camera(b,0,1.45,12-5,0,6));}
                case 6->{wait++; // the 18 clips on the shot knight, each frozen at its shot time (a loop at a quarter, a once clip at its peak, a hold at its end)
                    String n=TroopClips.NAMES.get(clip);var e=client(c,shotKnight.mob);
                    if(wait==1&&e!=null)TroopAnimator.forced.put(e.getId(),new TroopAnimator.Forced(n,TroopClips.shotTime(n)));
                    if(wait==8){if(shotReady(c,"anim-biped-"+n)){shot(c,"anim-biped-"+n);clip++;wait=0;if(clip==TroopClips.NAMES.size()){if(e!=null)TroopAnimator.forced.remove(e.getId());cs=7;}}else wait--;}}
                case 7->sheet(c);
                case 8->{if(++wait==5){serverDone=true;end(c);}}
                default->{}
            }
        }
        public void cleanup(MinecraftClient c){TroopAnimator.forced.clear();super.cleanup(c);}

        // ---- the clip tables ----
        /** A bare rig's bones (TroopModel.snapshot) after one clip at one time on its rest pose; null plays nothing. */
        static float[] sample(TroopModel<?> m,Animation a,float seconds){
            m.getPart().traverse().forEach(ModelPart::resetTransform);if(a!=null)TroopAnimator.animate(m,a,seconds,1f);return m.snapshot(null);
        }
        /** The largest rotation (degrees, any bone and axis) between two snapshots. */
        static double maxRot(float[] a,float[] b){
            if(a==null||b==null||a.length!=b.length)return 999;double max=0;
            for(int i=0;i<a.length;i+=6)for(int k=3;k<6;k++)max=Math.max(max,Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(Math.toDegrees(a[i+k]-b[i+k]))));
            return max;
        }
        /** The largest offset (model pixels, any bone and axis) between two snapshots. */
        static double maxPos(float[] a,float[] b){
            if(a==null||b==null||a.length!=b.length)return 999;double max=0;
            for(int i=0;i<a.length;i+=6)for(int k=0;k<3;k++)max=Math.max(max,Math.abs(a[i+k]-b[i+k]));
            return max;
        }
        /** The clip at its last keyframe: a loop played as a once clip, so its end is not wrapped to its start. */
        static Animation unwrapped(Animation a){return new Animation(a.lengthInSeconds(),false,a.boneAnimations());}
        void cover(){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String rig:RIGS){var m=TroopModels.rig(rig);var clips=TroopClips.of(rig);
                List<String> missing=new ArrayList<>(),strays=new ArrayList<>(),scale=new ArrayList<>();
                for(String n:TroopClips.NAMES)if(!clips.containsKey(n))missing.add(n);
                for(var set:List.of(clips,TroopClips.mirrored(rig)))for(var e:set.entrySet())for(var bone:e.getValue().boneAnimations().entrySet()){
                    if(m==null||!m.bones().contains(bone.getKey()))strays.add(e.getKey()+"/"+bone.getKey());
                    for(var t:bone.getValue())if(t.target()==net.minecraft.client.render.entity.animation.Transformation.Targets.SCALE)scale.add(e.getKey()+"/"+bone.getKey());}
                boolean good=m!=null&&clips.size()==TroopClips.NAMES.size()&&missing.isEmpty()&&strays.isEmpty()&&scale.isEmpty();ok&=good;
                why.append(rig).append(": ").append(m==null?"no rig model, ":"").append(clips.size()).append(" clips").append(missing.isEmpty()?"":", missing "+missing)
                    .append(strays.isEmpty()?", every bone they name (mirrored too) is in the rig":", bones not in the rig "+strays).append(scale.isEmpty()?", no SCALE":", SCALE in "+scale).append(good?"":" (BAD)").append("; ");}
            report("p2-anim-cover",ok,why.toString().trim());
        }
        /** p2-anim-change: sampled every 50 ms, every clip moves some bone 10 degrees from rest (idle: 1.5 degrees or 0.3 px). */
        void change(){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String rig:RIGS){var m=TroopModels.rig(rig);if(m==null){ok=false;why.append(rig).append(": no rig model (BAD); ");continue;}
                float[] rest=sample(m,null,0);List<String> weak=new ArrayList<>();double least=999;String leastClip="";
                for(String n:TroopClips.NAMES){var a=TroopClips.of(rig).get(n);if(a==null){weak.add(n+" (none)");continue;}
                    double rot=0,pos=0;for(float t=0;t<=a.lengthInSeconds()+1e-4f;t+=.05f){float[] p=sample(m,a,t);rot=Math.max(rot,maxRot(p,rest));pos=Math.max(pos,maxPos(p,rest));}
                    boolean good=n.equals("idle")?rot>=1.5||pos>=0.3:rot>=10;
                    if(!good)weak.add(String.format(Locale.ROOT,"%s %.1f deg %.2f px",n,rot,pos));
                    if(!n.equals("idle")&&rot<least){least=rot;leastClip=n;}
                    if(n.equals("idle"))why.append(rig).append(String.format(Locale.ROOT,": idle %.1f deg %.2f px",rot,pos));}
                ok&=weak.isEmpty();why.append(String.format(Locale.ROOT,", the least of the rest %s %.0f deg",leastClip,least)).append(weak.isEmpty()?"":", too still: "+weak+" (BAD)").append("; ");}
            report("p2-anim-change",ok,why.toString().trim());
        }
        /** p2-anim-return: a loop ends on its first frame, a once clip at rest, and a hold clip, cleared, leaves the rig at rest after BLEND_TICKS (all within 1 degree; offsets within 0.1 px). */
        void returns(){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String rig:RIGS){var m=TroopModels.rig(rig);if(m==null){ok=false;why.append(rig).append(": no rig model (BAD); ");continue;}
                float[] rest=sample(m,null,0);List<String> bad=new ArrayList<>();double worst=0;String worstClip="";
                for(String n:TroopClips.NAMES){var a=TroopClips.of(rig).get(n);if(a==null)continue;var kind=TroopClips.KINDS.get(n);double rot,pos;
                    if(kind==TroopClips.Kind.LOOP){var u=unwrapped(a);float[] first=sample(m,u,0),end=sample(m,u,a.lengthInSeconds());rot=maxRot(end,first);pos=maxPos(end,first);}
                    else if(kind==TroopClips.Kind.ONCE){float[] end=sample(m,a,a.lengthInSeconds());rot=maxRot(end,rest);pos=maxPos(end,rest);}
                    else{float[] after=cleared(m,rig,n);rot=maxRot(after,rest);pos=maxPos(after,rest);}
                    if(rot>1||pos>0.1)bad.add(String.format(Locale.ROOT,"%s %.2f deg %.2f px",n,rot,pos));
                    if(rot>worst){worst=rot;worstClip=n;}}
                ok&=bad.isEmpty();why.append(rig).append(String.format(Locale.ROOT,": the worst %s %.2f deg",worstClip,worst)).append(bad.isEmpty()?"":", off: "+bad+" (BAD)").append("; ");}
            report("p2-anim-return",ok,why.toString().trim()+" (loops: end against the first frame; once: end against rest; holds: played to the end, cleared, then "+TroopAnimator.BLEND_TICKS+" ticks)");
        }
        /** A hold clip played to its end on a bare rig through a State (as a scene pose, death, or the shield raised), then cleared: the bones BLEND_TICKS later. */
        static float[] cleared(TroopModel<?> m,String rig,String clip){
            var s=new TroopAnimator.State(1);boolean dead=clip.equals("death"),shield=clip.equals("shield_up");String scene=dead||shield?null:clip;
            var on=view(rig,dead,false,false,false,false,shield,false,"",scene,null);var off=view(rig,false,false,false,false,false,false,false,"",null,null);
            int end=(int)Math.ceil(TroopClips.LENGTHS.get(clip)*20)+TroopAnimator.BLEND_TICKS+2,age=0;
            for(;age<=end;age++)TroopAnimator.simulate(m,s,on,age,true);
            for(int k=0;k<=TroopAnimator.BLEND_TICKS;k++,age++)TroopAnimator.simulate(m,s,off,age,true);
            return m.snapshot(null);
        }
        /** A View: rig, dead, swing start, hurt start, attacking, ranged, shield up, rearing, stance, scene pose, last pose (no magic, no spell, standing). */
        static TroopAnimator.View view(String rig,boolean dead,boolean swing,boolean hurt,boolean attacking,boolean ranged,boolean shield,boolean rearing,String stance,String scene,String last){
            return new TroopAnimator.View(rig,dead,swing,hurt,attacking,ranged,false,false,shield,rearing,0f,stance,scene,last);
        }
        static TroopAnimator.View with(TroopAnimator.View v,boolean magic,boolean spell,float limbDistance){
            return new TroopAnimator.View(v.rig(),v.dead(),v.swingStart(),v.hurtStart(),v.attacking(),v.ranged(),magic,spell,v.shieldUp(),v.rearing(),limbDistance,v.stance(),v.scenePose(),v.lastPose());
        }
        /** p2-anim-choose: TroopAnimator.choose on synthetic Views, and a State restarting bow_draw on a shot. */
        void choose(){
            List<String> bad=new ArrayList<>();int n=0;
            var all=with(view("biped",true,true,true,true,true,true,true,"brace","kneel","sit"),true,true,1f);
            n++;expect(bad,"dead over everything",all,"death",null,null,false,false);
            n++;expect(bad,"scene kneel over every overlay",view("biped",false,true,true,true,false,true,true,"","kneel",null),"kneel",null,null,false,false);
            n++;expect(bad,"scene fallen",view("biped",false,false,false,false,false,false,false,"","fallen",null),"fall",null,null,false,false);
            n++;expect(bad,"stand after kneel",view("biped",false,false,false,false,false,false,false,"","stand","kneel"),"kneel",null,null,true,false);
            n++;expect(bad,"stand after fallen",view("biped",false,false,false,false,false,false,false,"","stand","fallen"),"stand",null,null,false,false);
            n++;expect(bad,"scene sword_up",view("biped",false,false,false,false,false,false,false,"","sword_up",null),"raise",null,null,false,false);
            n++;expect(bad,"helm_off after kneel keeps kneel",view("biped",false,false,false,false,false,false,false,"","helm_off","kneel"),"kneel",null,null,false,false);
            n++;expect(bad,"a melee swing start",view("biped",false,true,false,true,false,false,false,"",null,null),"idle","walk","attack",false,true);
            n++;expect(bad,"ranged and attacking",view("biped",false,false,false,true,true,false,false,"",null,null),"idle","walk","bow_draw",false,true);
            n++;expect(bad,"ranged, attacking, a shot (swing start)",view("biped",false,true,false,true,true,false,false,"",null,null),"idle","walk","bow_draw",false,true);
            n++;expect(bad,"magic and attacking",with(view("biped",false,false,false,true,true,false,false,"",null,null),true,false,0),"idle","walk","cast",false,true);
            n++;expect(bad,"spellcasting",with(view("biped",false,false,false,false,false,false,false,"",null,null),false,true,0),"idle","walk","cast",false,true);
            n++;expect(bad,"off-hand use",view("biped",false,false,false,false,false,true,false,"",null,null),"idle","walk","shield_up",false,true);
            n++;expect(bad,"a bear's warning",view("hound",false,false,false,false,false,false,true,"",null,null),"idle","walk","raise",false,true);
            n++;expect(bad,"a hurt start",view("biped",false,false,true,false,false,false,false,"",null,null),"idle","walk","hit",false,true);
            for(String[] st:new String[][]{{"run","run","idle"},{"fall_back","fall_back","idle"},{"","walk","idle"},{"brace","walk","brace"},{"hold","walk","hold"}}){
                n++;expect(bad,"stance \""+st[0]+"\"",with(view("biped",false,false,false,false,false,false,false,st[0],null,null),false,false,.5f),st[2],st[1],null,false,true);}
            // bow_draw restarts on each shot: the live track starts again at the swing
            var s=new TroopAnimator.State(1);var aim=view("biped",false,false,false,true,true,false,false,"",null,null),shot=view("biped",false,true,false,true,true,false,false,"",null,null);
            s.update(TroopAnimator.choose(aim),aim,0);s.update(TroopAnimator.choose(aim),aim,5);float first=s.overlays.get("bow_draw").live().start;
            s.update(TroopAnimator.choose(shot),shot,10);float again=s.overlays.get("bow_draw").live().start;
            n++;if(first!=0||again!=10||s.overlays.containsKey("attack")&&s.overlays.get("attack").live()!=null)bad.add("bow_draw started at "+first+" and at "+again+" after a shot at 10 (want 0 and 10, and no attack)");
            report("p2-anim-choose",bad.isEmpty(),n+" cases"+(bad.isEmpty()?", all as the rules say (dead, scene poses, overlays, stances, head look)":"; wrong: "+bad));
        }
        static void expect(List<String> bad,String what,TroopAnimator.View v,String still,String move,String overlay,boolean reverse,boolean look){
            if(v==null){bad.add(what+": no view");return;}
            var c=TroopAnimator.choose(v);
            if(!Objects.equals(c.still(),still)||!Objects.equals(c.move(),move)||!Objects.equals(c.overlay(),overlay)||c.reverse()!=reverse||c.headLook()!=look)
                bad.add(what+": got "+c+", want "+new TroopAnimator.Choice(still,move,overlay,reverse,look));
        }

        // ---- the stance sync ----
        /** One stance step: the drill's order, formation and rout, and the stance its three knights must then read on this client within 20 ticks. */
        record StanceStep(String what,String order,String formation,boolean rout,String want){}
        static final List<StanceStep> STANCE_STEPS=List.of(new StanceStep("hold in shieldwall","hold","shieldwall",false,"brace"),new StanceStep("charge","charge","line",false,"run"),
            new StanceStep("follow","follow","line",false,""),new StanceStep("hold in line, the group routing","hold","line",true,"run"),new StanceStep("hold in line","hold","line",false,"hold"));
        boolean stanceOk=true;final StringBuilder stanceWhy=new StringBuilder();int lateSeen=-1,trackingBefore,beatBefore;String lateStance="-";
        void stance(MinecraftClient c,int tick){
            wait++;
            if(si<STANCE_STEPS.size()){
                var st=STANCE_STEPS.get(si);
                if(wait==1){since=tick;server(c,b->{var d=drill;d.order=st.order();d.formation=st.formation();String g=group.get(0).spec.group();if(st.rout())d.routedAt.put(g,b.time);else d.routedAt.remove(g);});return;}
                boolean all=true;for(var u:group){var e=client(c,u.mob);all&=e!=null&&TroopAnimator.STANCES.getOrDefault(e.getId(),"").equals(st.want());}
                if(all||tick-since>40){boolean good=all&&tick-since<=20;stanceOk&=good;
                    stanceWhy.append(st.what()).append(": ").append(all?"\""+st.want()+"\" read for all three "+(tick-since)+" ticks after the order":"not all read \""+st.want()+"\" in 40 ticks: "+reads(c)).append(good?"":" (BAD: want 20 ticks or fewer)").append("; ");
                    si++;wait=0;}
                return;
            }
            // a knight spawned into the group on hold in line: its first tick on this client reads hold, sent when the player started tracking it
            if(wait==1){trackingBefore=Tactics.trackingSends;beatBefore=Tactics.stanceSends;since=tick;server(c,b->late=post(b,drill,"knight",-2.5,5,180));return;}
            var e=late==null?null:client(c,late.mob);
            if(e!=null&&lateSeen<0){lateSeen=tick-since;lateStance=TroopAnimator.STANCES.getOrDefault(e.getId(),"");}
            if(lateSeen>=0||tick-since>60){
                int sends=Tactics.trackingSends-trackingBefore;boolean good=lateSeen>=0&&"hold".equals(lateStance)&&sends>=1;stanceOk&=good;
                stanceWhy.append("a knight spawned into the group on hold in line: on this client ").append(lateSeen<0?"never":lateSeen+" ticks after the spawn").append(", reading \"").append(lateStance)
                    .append("\" on that first tick (want hold); ").append(sends).append(" START_TRACKING stance sends (want 1 or more), ").append(Tactics.stanceSends-beatBefore).append(" beat sends meanwhile").append(good?"":" (BAD)");
                report("p2-anim-stance",stanceOk,stanceWhy.toString().trim());
                server(c,b->{var d=drill;d.order="follow";d.routedAt.clear();camera(b,0,1.6,-6,0,8);});cs=3;wait=0;si=0;}
        }
        String reads(MinecraftClient c){StringBuilder sb=new StringBuilder();for(var u:group){var e=client(c,u.mob);sb.append(e==null?"absent":"\""+TroopAnimator.STANCES.getOrDefault(e.getId(),"")+"\"").append(" ");}return sb.toString().trim();}

        // ---- a scene pose ----
        int sceneAt=-1;boolean kneelOk,helmOk,backOk;String kneelWhy="",helmWhy="",backWhy="";
        /** The knight's bones at a clip's last frame alone (no base, no carries: a pose holds every bone it names, the others are at rest). */
        static float[] clipEnd(TroopModel<?> m,String clip){
            m.getPart().traverse().forEach(ModelPart::resetTransform);var a=TroopAnimator.clip(m,clip,TroopAnimator.bow(TroopAnimator.row(m)));
            if(a!=null)TroopAnimator.animate(m,a,a.lengthInSeconds(),1f);TroopAnimator.rootScale(m);return m.snapshot(null);
        }
        void scene(MinecraftClient c,int tick){
            wait++;var e=client(c,sceneKnight.mob);var m=TroopModels.model("knight");var d=e==null?null:TroopRenderer.drawn(e);
            if(e==null||m==null||d==null||d.pose==null){if(wait>200){report("p2-anim-scene",false,"the scene knight was never drawn on this client");cs=4;wait=0;}return;}
            int id=e.getId();
            if(sceneAt<0){if(!shotReady(c,"anim-scene"))return;TroopAnimator.setScenePose(id,"kneel");sceneAt=tick;return;}
            int t=tick-sceneAt;
            if(t==13){double off=maxRot(d.pose,clipEnd(m,"kneel")),pos=maxPos(d.pose,clipEnd(m,"kneel"));kneelOk=off<=1;
                kneelWhy=String.format(Locale.ROOT,"kneel, 0.65 s after setScenePose: %.2f deg and %.2f px from kneel's last frame (want 1 deg or less)",off,pos);shot(c,"anim-scene-kneel");TroopAnimator.setScenePose(id,"helm_off");}
            if(t==19){double off=maxRot(d.pose,clipEnd(m,"kneel"));List<String> shown=m.shownBones(d.last.bones());helmOk=off<=1&&m.bones().contains("iron_cap")&&!shown.contains("iron_cap");
                helmWhy=String.format(Locale.ROOT,"then helm_off: still %.2f deg from kneel's last frame, iron_cap %s",off,shown.contains("iron_cap")?"drawn (BAD)":"hidden")+(m.bones().contains("iron_cap")?"":" (the knight has no iron_cap: BAD)");
                shot(c,"anim-scene-helm");TroopAnimator.setScenePose(id,null);}
            if(t==19+TroopAnimator.BLEND_TICKS+2){float[] in=d.inputs.clone(),drawn=d.pose.clone();float[] want=TroopAnimator.reference(m,(MobEntity)e,in[1],in[2],in[0],in[3],in[4]);
                double off=maxRot(drawn,want);List<String> shown=m.shownBones(d.last.bones());backOk=off<=1&&shown.contains("iron_cap");
                backWhy=String.format(Locale.ROOT,"then null: %d ticks later %.2f deg from the idle pose a fresh State gives at the same age and look (want 1 or less), iron_cap %s",TroopAnimator.BLEND_TICKS+2,off,shown.contains("iron_cap")?"back":"still hidden (BAD)");
                report("p2-anim-scene",kneelOk&&helmOk&&backOk,kneelWhy+"; "+helmWhy+"; "+backWhy);cs=4;wait=0;}
        }

        // ---- a death ----
        int killAt=-1,drawsAtKill,deadSamples,lastDeathTime;float minRoot=999;Entity dying;
        void death(MinecraftClient c,int tick){
            wait++;var m=TroopModels.model("knight");
            if(killAt<0){var e=client(c,victim.mob);var d=e==null?null:TroopRenderer.drawn(e);
                if(d==null||m==null){if(wait>200){deathReport(m,null);cs=5;wait=0;}return;}
                dying=e;drawsAtKill=d.count;killAt=tick;server(c,b->victim.mob.kill());return;}
            var d=TroopRenderer.drawn(dying);int root=m.boneOrder().indexOf("root");
            if(d!=null&&d.pose!=null&&root>=0&&dying instanceof net.minecraft.entity.LivingEntity le&&(le.deathTime>0||!le.isAlive())){deadSamples++;lastDeathTime=le.deathTime;minRoot=Math.min(minRoot,(float)Math.toDegrees(d.pose[root*6+3]));}
            if(client(c,victim.mob)==null||tick-killAt>80){deathReport(m,d);cs=5;wait=0;}
        }
        /** p2-anim-death: every rig's death ends by 0.9 s and holds; the killed knight fell through its own clip (its root pitched back to -90) and never tilted (TroopRenderer's measured tilt). */
        void deathReport(TroopModel<?> m,TroopRenderer.Drawn d){
            boolean clips=true;StringBuilder cw=new StringBuilder();
            for(String rig:RIGS){var a=TroopClips.of(rig).get("death");boolean g=a!=null&&a.lengthInSeconds()<=.9f+1e-4f&&!a.looping()&&TroopClips.KINDS.get("death")==TroopClips.Kind.HOLD;clips&=g;
                cw.append(rig).append(" ").append(a==null?"none":String.format(Locale.ROOT,"%.2f s",a.lengthInSeconds())).append(g?"":" (BAD)").append(", ");}
            int draws=d==null?0:d.count-drawsAtKill;float tilt=d==null?999:d.maxTilt;
            boolean ok=clips&&d!=null&&draws>=10&&deadSamples>=10&&tilt<1&&minRoot<=-85;
            report("p2-anim-death",ok,String.format(Locale.ROOT,"death clips: %s(want 0.9 s or less, a hold); the killed knight: %d draws after the kill, %d ticks sampled dead up to deathTime %d, its root pitched to %.1f deg (want -85 or less: the clip's fall), the most tilt of any of its draws after setupTransforms %.2f deg (want under 1: no vanilla death tilt)",
                cw,draws,deadSamples,lastDeathTime,minRoot,tilt));
        }

        // ---- the fold ----
        /** p2-anim-fold: prepare(look, true, false) on the knight shows arms_folded and hides both arms, and with them what the hands hold (the Weapon and Shield layers wait on folded and on the hand's path); the fold rule unfolds an attacking troop. */
        void fold(MinecraftClient c){
            var e=client(c,shotKnight.mob);var m=TroopModels.model("knight");var l=e==null?null:TroopModels.resolve(e);
            if(m==null||l==null){report("p2-anim-fold",false,"no knight model or no look for the shot knight");return;}
            String off="left".equals(m.hand())?"right":"left";
            m.prepare(l,true,false);List<String> shown=m.shownBones(m.shownMask());
            boolean arms=shown.contains("arms_folded")&&!shown.contains("right_arm")&&!shown.contains("left_arm");
            boolean held=m.folded&&!TroopFeatures.shown(m.handPath(m.hand()))&&!TroopFeatures.shown(m.handPath(off));
            m.prepare(l,false,false);List<String> open=m.shownBones(m.shownMask());
            boolean unfolded=!m.folded&&open.contains("right_arm")&&open.contains("left_arm")&&!open.contains("arms_folded")&&TroopFeatures.shown(m.handPath(m.hand()));
            boolean rule=TroopAnimator.folds("folded",false,false)&&!TroopAnimator.folds("folded",true,false)&&!TroopAnimator.folds("folded",false,true)&&!TroopAnimator.folds("hang",false,false);
            boolean live=!TroopAnimator.folds(m,(MobEntity)e);
            report("p2-anim-fold",arms&&held&&unfolded&&rule&&live,"prepare(look, true, false) on the knight: shown "+shown+", arms_folded shown and both arms hidden="+arms
                +", folded and both hand paths hidden (no weapon, no shield)="+held+"; prepare(look, false, false): both arms back, arms_folded hidden="+unfolded
                +"; the rule: folded idle folds, attacking unfolds, an arm held by a clip unfolds, hang never folds="+rule+"; the live knight (idleArms "+m.idleArms()+") not folded="+live);
        }

        // ---- the contact sheet's frames ----
        SheetProbe probe;
        void sheet(MinecraftClient c){
            wait++;
            if(probe==null){probe=new SheetProbe();c.setScreen(probe);frame=0;wait=0;return;}
            if(frame>=SheetProbe.FRAMES.size()){c.setScreen(null);cs=8;wait=0;return;}
            var f=SheetProbe.FRAMES.get(frame);
            if(wait==1)probe.frame=f;
            if(wait==5){shot(c,"anim-sheet-"+f.name());frame++;wait=0;}
        }
    }

    /**
     * Task 5b's contact sheet frames: one figure alone on a flat key colour (KEY), its feet at the same point of the screen for
     * every frame, so contact5b.py can crop a rig's frames to one box and scale them alike. A bare rig draws in the build_skins
     * template's hues (one hue per bone with boxes, in file order; the faces shaded by the GUI entity light) and plays its clip
     * directly; the Order Squire draws as a portrait mob in gold cloth with a gold shield, its clip through TroopAnimator.forced,
     * so it shows its carries (the shield arm) as in the world. Every figure is turned YAW and seen TILT degrees from above.
     */
    static final class SheetProbe extends net.minecraft.client.gui.screen.Screen {
        static final int KEY=0xFFFF00FF;static final float YAW=230,TILT=-15;
        static final int[][] HUES={{196,92,84},{84,140,196},{104,180,96},{200,164,72},{150,100,190},{70,170,170},{210,120,50},{120,120,200},{180,90,150},{110,160,60},{90,110,130},{200,200,110}};
        record Frame(String name,String rig,String clip,float seconds){}
        static final List<Frame> FRAMES;
        static{
            List<Frame> f=new ArrayList<>();
            for(String rig:Anim.RIGS){f.add(new Frame(rig+"-rest",rig,"idle",0));f.add(new Frame(rig+"-walk",rig,"walk",.25f));f.add(new Frame(rig+"-attack",rig,"attack",TroopClips.PEAKS.get("attack")));
                f.add(new Frame(rig+"-guard",rig,"shield_up",TroopClips.LENGTHS.get("shield_up")));f.add(new Frame(rig+"-death",rig,"death",TroopClips.LENGTHS.get("death")));}
            for(int i=0;i<8;i++)f.add(new Frame("walk-"+i,"biped","walk",i/8f));
            FRAMES=List.copyOf(f);
        }
        static final Identifier WHITE=new Identifier("gravesandglory","probe/white");
        /** The Squire (the biped row and its 4x rest and guard) and the Templar Commander (sentinel stage 3: the walk strip, its cape halves on the legs). */
        volatile Frame frame;MobEntity squire,templar;final Map<String,List<String>> boxedBones=new HashMap<>();
        SheetProbe(){super(net.minecraft.text.Text.literal("Clips"));}
        @Override protected void init(){
            var c=MinecraftClient.getInstance();
            if(c.getTextureManager().getOrDefault(WHITE,null)==null){var img=new NativeImage(16,16,false);img.fillRect(0,0,16,16,0xFFFFFFFF);c.getTextureManager().registerTexture(WHITE,new net.minecraft.client.texture.NativeImageBackedTexture(img));}
            if(squire==null&&c.world!=null){squire=figure(c,"knight",1);templar=figure(c,"sentinel",3);}
        }
        /** A gold troop of one stage, dressed as the portraits are: its name, the painted shield and its weapon. */
        static MobEntity figure(MinecraftClient c,String troop,int stage){
            var spec=ArmyCards.spec(troop);MobEntity m=ArmyMobs.portrait(spec.entity(),c.world);m.setCustomName(net.minecraft.text.Text.literal(ArmyMobs.stageName(spec,stage)));
            String dye="yellow";for(var fa:Spec.FACTIONS)if(fa.id().equals("gold"))dye=fa.dye();
            if(!spec.shield().equals("none"))m.equipStack(EquipmentSlot.OFFHAND,GravesAndGlory.Battle.shield(spec,dye));
            m.equipStack(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.registry.Registries.ITEM.get(new Identifier(spec.weapon()))));
            TroopModels.dress(m,"gold");return m;
        }
        @Override public boolean shouldPause(){return false;}
        static org.joml.Quaternionf view(){return new org.joml.Quaternionf().rotateZ((float)Math.PI).mul(new org.joml.Quaternionf().rotateX(TILT*TroopModel.RAD));}
        /** GUI units per block for a figure: its rig's rest height drawn about 200 framebuffer pixels tall. */
        static int size(String rig){
            var m=TroopModels.rig(rig);double h=m==null?32:Math.max(8,m.restBounds().maxY-m.restBounds().minY);
            return (int)Math.round(200/MinecraftClient.getInstance().getWindow().getScaleFactor()/(h/16));
        }
        @Override public void render(net.minecraft.client.gui.DrawContext g,int mx,int my,float delta){
            g.fill(0,0,width,height,KEY);var f=frame;if(f==null)return;
            int x=width/2,y=height*4/5;
            if(f.rig().equals("biped")){
                MobEntity who=f.name().startsWith("walk-")?templar:squire;if(who==null)return;
                who.bodyYaw=who.prevBodyYaw=YAW;who.headYaw=who.prevHeadYaw=YAW;who.setYaw(YAW);who.prevYaw=YAW;who.setPitch(0);who.prevPitch=0;
                TroopAnimator.forced.put(who.getId(),new TroopAnimator.Forced(f.clip(),f.seconds()));
                net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,x,y,size("biped"),view(),null,who);
            }else drawRig(g,TroopModels.rig(f.rig()),f.clip(),f.seconds(),x,y,size(f.rig()));
        }
        /** The bones of a rig that have boxes, in file order (the template's hue order). */
        List<String> boxed(TroopModel<?> m){
            return boxedBones.computeIfAbsent(m.rig,r->{List<String> out=new ArrayList<>();for(String name:m.bones()){int[] n={0};m.bone(name).forEachCuboid(new MatrixStack(),(e,path,i,q)->{if(path.isEmpty())n[0]++;});if(n[0]>0)out.add(name);}return out;});
        }
        void drawRig(net.minecraft.client.gui.DrawContext g,TroopModel<?> m,String clip,float seconds,int x,int y,int size){
            if(m==null)return;var top=m.getPart();List<String> bones=boxed(m);
            top.traverse().forEach(p->{p.resetTransform();p.hidden=true;});
            var a=TroopClips.of(m.rig).get(clip);if(a!=null)TroopAnimator.animate(m,a,seconds,1f);
            ModelPart fold=m.bone("arms_folded");boolean was=fold!=null&&fold.visible;if(fold!=null)fold.visible=false;
            MatrixStack ms=g.getMatrices();ms.push();ms.translate(x,y,50);ms.multiplyPositionMatrix(new Matrix4f().scaling(size,size,-size));ms.multiply(view());
            ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-YAW));ms.scale(-1,-1,1);ms.translate(0,-1.501f,0);
            net.minecraft.client.render.DiffuseLighting.method_34742();
            var vc=g.getVertexConsumers().getBuffer(net.minecraft.client.render.RenderLayer.getEntityCutoutNoCull(WHITE));
            for(int i=0;i<bones.size();i++){ModelPart p=m.bone(bones.get(i));int[] h=HUES[i%HUES.length];
                p.hidden=false;top.render(ms,vc,0xF000F0,net.minecraft.client.render.OverlayTexture.DEFAULT_UV,h[0]/255f,h[1]/255f,h[2]/255f,1f);p.hidden=true;}
            g.draw();net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();ms.pop();
            top.traverse().forEach(p->{p.hidden=false;p.resetTransform();});if(fold!=null)fold.visible=was;
        }
    }
}
