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
import net.minecraft.entity.mob.StrayEntity;
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
    static{SUITES.put("banners",Banners::new);SUITES.put("engine",Engine::new);SUITES.put("commander",Commanders::new);SUITES.put("gilded",Gilded::new);SUITES.put("spike",Spike::new);SUITES.put("gilded-art",GildedArt::new);SUITES.put("graveborn",Graveborn::new);SUITES.put("ironclad",Ironclad::new);SUITES.put("tidecallers",Tidecallers::new);SUITES.put("anim",Anim::new);SUITES.put("turn",Turn::new);SUITES.put("tells",Tells::new);SUITES.put("depth",Depth::new);}
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
    /** Every member (an entity's name, its UUID) of the four gg_ side teams. */
    static List<String> sideMembers(GravesAndGlory.Battle b){
        var board=b.server.getScoreboard();List<String> out=new ArrayList<>();
        for(var f:Spec.FACTIONS){var t=board.getTeam("gg_"+f.id());if(t!=null)for(String m:t.getPlayerList())out.add(f.id()+":"+m);}
        return out;
    }
    /** The side-team members that are not a living troop of b.units: a membership that outlived its entity (review focus 1). */
    static List<String> staleSides(GravesAndGlory.Battle b){
        Set<String> live=new HashSet<>();for(var u:b.units)if(u.mob.isAlive()&&!u.mob.isRemoved())live.add(u.mob.getEntityName());
        List<String> out=new ArrayList<>();for(String m:sideMembers(b))if(!live.contains(m.substring(m.indexOf(':')+1)))out.add(m);
        return out;
    }

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
        Frame.register();RenderCost.register();DepthCost.register();
        ServerTickEvents.END_SERVER_TICK.register(s->{if(current instanceof ServerWatch w&&GravesAndGlory.battle!=null)try{w.serverTick(GravesAndGlory.battle);}catch(Exception e){report("p2-"+currentName+"-exception",false,e.toString());e.printStackTrace();}});
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            if(c.getOverlay()!=null)return;
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            Phase1Checks.listenSounds(c);
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
        // the HUD strip: the client drives stage 5 and the server reports at 7
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
        static String show(Combat.HitLog h){return h==null?"no hit":"counters "+h.counters()+" rows "+h.rows()+" stance "+h.stance()+" base "+h.base()+" traits "+h.traits()+" auras "+h.auras()+" commander "+h.commander()+" total "+h.total();}
        /** The newest logged hit on this troop, or null. */
        static Combat.HitLog hitOn(GravesAndGlory.Unit target){Combat.HitLog out=null;for(var h:Combat.LAST)if(h.target()==target.mob.getId())out=h;return out;}
        /** The troops a House shows as owned after the v1 fixture migrated: its Commons, plus the unlocked Templar. */
        static Set<String> expected(String house){
            Set<String> out=new LinkedHashSet<>();for(var t:Spec.TROOPS)if(t.house().equals(house)&&t.rarity().equals("common"))out.add(t.id());if(house.equals("gilded"))out.add("sentinel");return out;
        }
        public void cleanup(MinecraftClient c){
            RenderCost.on=false;server(c,b->{clearFixture();unforce(b);if(cost!=null)cost.handBack();if(rngSaved!=null)b.campaign.rollRng=rngSaved;if(b.running)b.campaign.returnLobby();});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            String cmd=send;if(cmd!=null){send=null;GravesAndGloryClient.send(cmd);System.out.println("[p2] sent "+cmd);}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=12){shot(c,wantShot);shotTaken=true;}
            if(stage==5)hud(c);
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
            if(hudState==1&&wantShot!=null&&shotTaken){wantShot=null;hudState=2;server(c,b->next(b,7));}
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
                    var armor=net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR;
                    for(String id:List.of("husk","skeleton","wither_skeleton")){
                        MobEntity m=ArmyMobs.create(id,w);String goals,targets;
                        try{goals=goalNames(m,"goalSelector").toString();targets=goalNames(m,"targetSelector").toString();}catch(ReflectiveOperationException e){goals=targets="unreadable: "+e;}
                        double base=m.getAttributeBaseValue(armor);boolean good=m instanceof ArmyMob&&ArmyMobs.isTroop(m)&&m.getType()==types.get(id)&&goals.equals("[SwimGoal]")&&targets.equals("[]")&&base==0;
                        ok&=good;why.append(id).append(" -> ").append(m.getClass().getSimpleName()).append(", goals ").append(goals).append(", targets ").append(targets).append(", base armour ").append(base).append(good?"":" (BAD)").append("; ");
                    }
                    boolean threw=false;String thrown="";try{ArmyMobs.create("wolf",w);}catch(IllegalStateException e){threw=true;thrown=e.getMessage();}
                    var wolf=ArmyMobs.portrait("wolf",w);boolean portrait=wolf instanceof net.minecraft.entity.passive.WolfEntity&&!(wolf instanceof ArmyMob)&&!ArmyMobs.isTroop(wolf);
                    double vanillaArmour=new HuskEntity(EntityType.HUSK,w).getAttributeBaseValue(armor);boolean control=vanillaArmour==2; // the control: a vanilla husk does carry the +2 that create() clears
                    boolean unknown=ArmyMobs.portrait("p2_no_such_mob",w) instanceof HuskEntity&&ArmyMobs.portrait("arrow",w) instanceof HuskEntity&&ArmyMobs.portrait("Not An Id",w) instanceof HuskEntity;
                    ArmyMobs.register("p2_probe",GravesAndGlory.ArmyHusk::new);boolean registered=ArmyMobs.create("p2_probe",w) instanceof GravesAndGlory.ArmyHusk;ArmyMobs.FACTORIES.remove("p2_probe");
                    boolean gone=false;try{ArmyMobs.create("p2_probe",w);}catch(IllegalStateException e){gone=true;}
                    boolean names=ArmyMobs.named("Order Squire")==Campaign.troop("knight")&&ArmyMobs.named("Knight-Captain")==Campaign.troop("knight")&&ArmyMobs.named("Nobody")==null&&ArmyMobs.stageName(Campaign.troop("knight"),1).equals("Order Squire")&&ArmyMobs.stageName(Campaign.troop("knight"),2).equals("Knight")&&ArmyMobs.stageName(Campaign.troop("mage"),2).equals("Sunweaver");
                    ok&=threw&&portrait&&registered&&gone&&names&&control&&unknown;
                    report("p2-armymobs",ok,why+"create(wolf) throws IllegalStateException="+threw+" (\""+thrown+"\"); portrait(wolf) is a plain wolf="+portrait+"; register(p2_probe) then create gives an ArmyHusk="+registered+", gone after removal="+gone+"; stage names resolve (Order Squire, Knight, Knight-Captain; a troop without a line keeps its name)="+names+"; a vanilla Husk's base armour is "+vanillaArmour+" and create() clears it to 0 (above), so a troop's armour is its worn gear="+control+"; portrait of an id the game lacks, of an id that is no mob (arrow) and of a malformed id each draws a husk, not the registry's pig="+unknown);
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
                case 16->{if(since(b)<25)return; // and from behind, where the flank row (C16 1.3) applies and the shield does not
                    move(melee,0,-2.5,0);knight.mob.setHealth(knight.mob.getMaxHealth());mark=Combat.LAST.peekLast();
                    Combat.resolve(b,melee,knight.mob,5f,"melee",false);meleeBack=Combat.LAST.peekLast()!=mark?Combat.LAST.peekLast():null;
                    boolean f=front!=null&&near(front.counters(),0.35)&&front.rows().equals(List.of("C1"))&&frontLost>0&&frontLost<=3.5f+1e-3&&front.target()==knight.mob.getId()&&front.attacker()==bowFront.mob.getId()&&front.base()==10f&&near(front.traits(),1)&&near(front.auras(),1)&&near(front.commander(),1)&&near(front.total(),0.35);
                    float wantBack=11.5f*(1-Math.max(8/5f,8-11.5f/2)/25f); // vanilla armour 8 on 10 x 1.15 (C16s): 10.465
                    boolean k=back!=null&&near(back.counters(),1.15)&&back.rows().equals(List.of("C16s"))&&backLost>frontLost&&near(back.total(),1.15)&&Math.abs(backLost-wantBack)<0.02f;
                    boolean gear=knight.mob.getArmor()==8&&knight.mob.getAttributeBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR)==0;
                    boolean l=lowered!=null&&near(lowered.counters(),0.75)&&lowered.rows().equals(List.of("C2"))&&loweredLost>frontLost;
                    boolean m1=meleeFront!=null&&near(meleeFront.counters(),0.6)&&meleeFront.rows().equals(List.of("C3")),m2=meleeBack!=null&&near(meleeBack.counters(),1.3)&&meleeBack.rows().equals(List.of("C16"));
                    report("p2-shield-counter",f&&k&&l&&m1&&m2&&gear,"raised shield, Bone Archer arrow from the front: "+show(front)+", lost "+frontLost+" of "+10*0.35+" (the shield did not stop it)="+f+"; from behind: "+show(back)+", lost "+backLost+" (want "+wantBack+": base 10 x 1.15, C16s, under armour 8)="+k+"; shield lowered, front: "+show(lowered)+", lost "+loweredLost+"="+l+"; melee into the raised shield, front: "+show(meleeFront)+"="+m1+"; melee from behind: "+show(meleeBack)+" (want 1.3 C16)="+m2+"; the Squire's armour is "+knight.mob.getArmor()+" (iron helmet and chestplate = 8) on a husk base of "+knight.mob.getAttributeBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR)+"="+gear);
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
    /**
     * Task 4: the six commander classes. Each class is set through the real class:(id) packet, then a 1v1 skirmish shows its passive
     * from its sheet row (p2-class-passives, the Reaver's through a whole respawn); the commander's own blows through Battle.allowDamage,
     * struck for real (p2-commander-blows); the ability key and the HUD tile with no active registered (p2-ability-key); an active's
     * start, run, end and cooldown with a probe registered in Commander.ACTIVES (p2-ability-cycle), with the group panels clear of
     * vanilla's hearts (p2-hud-hearts); the probe stopped by its commander leaving and taking his seat back (p2-ability-seat) and by a
     * round start and the lobby (p2-ability-stops); a commander seated fresh in a running round under his own class (p2-fresh-seat);
     * and the camp class tab (p2-class-picker), with the banner editor's dimmed cards beside it. Stages run on the server tick; the
     * client sends the commands, presses the key, opens the screens and takes the screenshots the stages ask for.
     */
    static final class Commanders implements Suite,ServerWatch {
        /** A stand-in active: counts its calls and accepts every start. */
        static final class Probe implements Commander.Active {
            volatile int starts,ticks,ends;volatile long startedAt=-1;
            public boolean start(GravesAndGlory.Battle b,ServerPlayerEntity p,GravesAndGlory.Squad s,Spec.ClassesRow c){starts++;startedAt=b.time;s.abilityData="probe";return true;}
            public void tick(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Spec.ClassesRow c){ticks++;}
            public void end(GravesAndGlory.Battle b,GravesAndGlory.Squad s){ends++;}
        }
        volatile int stage;volatile UUID player;volatile String send,wantShot,expectActive="";volatile boolean shotTaken,finished,wantPress,clientDone;
        long stageAt;int shotDelay,ci;
        final List<String> order=Arrays.stream(Spec.CLASSES).map(Spec.ClassesRow::id).toList();
        final Map<String,Commander.Active> saved=new HashMap<>();
        final StringBuilder passiveWhy=new StringBuilder();boolean passivesOk=true;
        // the lobby after the Champion and after the Reaver: their modifiers are gone
        double lobbyHealth=-1;boolean lobbyAttackGone;
        // the client's sightings: the respawn countdown, the barracks lines, the key bindings, the tile and the class tab
        volatile int respawnSeen=-1;volatile boolean clientOk,keyOk,activeSeen,readyInSeen,pickerDone,pickerOk;volatile String clientWhy="",keyWhy="",tileWhy="",pickerWhy="";
        int clientWait,tileState,tileWait,pick,pickWait,openWait;
        long goldMark=-1,markAt;int incomeDelta=-1;
        Probe probe;boolean keyServerOk;String keyServerWhy="";
        // the HUD's group panels against vanilla's hearts, read on the client during the cycle
        volatile int hudRight=-1;volatile double hudHearts;volatile String hudFrame="";
        // the seat: the squad he left and the cooldown it held
        GravesAndGlory.Squad seat;long seatReadyAt;boolean seatOk;String seatWhy="";
        // the fresh seat: the squad he had before, and the bot seats of that round
        GravesAndGlory.Squad freshOld;long freshBots=-1;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Profile profile(GravesAndGlory.Battle b){return b.campaign.profile(player);}
        void requestShot(String name){shotTaken=false;shotDelay=0;wantShot=name;}
        void command(GravesAndGlory.Battle b,String cmd,int nextStage){send=cmd;next(b,nextStage);}
        static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}
        static double factor(Combat.HitLog h){return h==null?Double.NaN:h.commander();}
        /** A living troop of the squad that passes the test, or null. */
        static GravesAndGlory.Unit unit(GravesAndGlory.Battle b,GravesAndGlory.Squad s,java.util.function.Predicate<GravesAndGlory.Unit> test){for(var u:b.units)if(u.squad==s&&u.mob.isAlive()&&test.test(u))return u;return null;}
        /** A living troop of the other team, or null. */
        static GravesAndGlory.Unit enemy(GravesAndGlory.Battle b,GravesAndGlory.Squad s){for(var u:b.units)if(u.squad.team!=s.team&&u.mob.isAlive())return u;return null;}
        /** The troop stands dx blocks east of the commander, on his level. */
        static void place(GravesAndGlory.Unit u,ServerPlayerEntity p,double dx){u.mob.refreshPositionAndAngles(p.getX()+dx,p.getY(),p.getZ(),0,0);}
        /** One resolved blow of base 1 (Combat.resolve, as a secondary hit so no on-hit ability runs) and the hit it logged, or null. */
        static Combat.HitLog hit(GravesAndGlory.Battle b,GravesAndGlory.Unit attacker,net.minecraft.entity.LivingEntity target,String kind){var mark=Combat.LAST.peekLast();Combat.resolve(b,attacker,target,1f,kind,true);var h=Combat.LAST.peekLast();return h==mark?null:h;}
        /** The same as a primary blow (not a cleave's second target, which is area damage and takes no flank bonus), past the target's hurt cooldown: the on-hit abilities of the attacker run, so use it with troops that have none. */
        static Combat.HitLog blow(GravesAndGlory.Battle b,GravesAndGlory.Unit attacker,net.minecraft.entity.LivingEntity target,String kind){target.timeUntilRegen=0;target.hurtTime=0;var mark=Combat.LAST.peekLast();Combat.resolve(b,attacker,target,1f,kind,false);var h=Combat.LAST.peekLast();return h==mark?null:h;}
        void passive(boolean ok,String id,String why){passivesOk&=ok;passiveWhy.append(id).append(ok?" ok: ":" FAILED: ").append(why).append("; ");report("p2-class-passives-"+id,ok,why);}

        /** A stand-in player for the foe's commander. Fabric's FakePlayer is immune to all damage; this one takes blows like a player. */
        static final class Target extends net.fabricmc.fabric.api.entity.FakePlayer {
            Target(net.minecraft.server.world.ServerWorld world,com.mojang.authlib.GameProfile profile){super(world,profile);}
            @Override public boolean isInvulnerableTo(DamageSource source){return false;}
        }
        /** The amounts Battle.allowDamage re-issues on the watched entity (its calls made while scalingDamage is set), recorded by a second ALLOW_DAMAGE listener, registered once. */
        static final List<Float> reissued=new ArrayList<>();static net.minecraft.entity.LivingEntity watched;static boolean listening;
        static void listen(){
            if(listening)return;listening=true;
            net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->{var b=GravesAndGlory.battle;if(entity==watched&&b!=null&&b.scalingDamage)reissued.add(amount);return true;});
        }
        /** One blow: what it struck, the health it lost, the amounts allowDamage re-issued for it and the armour it had (set aside, so 0). */
        record Blow(String what,float lost,List<Float> amounts,int armour){
            /** It lost this much, and allowDamage re-issued it once at that amount, or (reissue null) not at all. */
            boolean is(double lost,Double reissue){return armour==0&&Math.abs(this.lost-lost)<1e-3&&(reissue==null?amounts.isEmpty():amounts.size()==1&&Math.abs(amounts.get(0)-reissue)<1e-3);}
            @Override public String toString(){return what+" lost "+lost+(amounts.isEmpty()?" (nothing re-issued)":" (re-issued "+amounts+")")+(armour==0?"":" with armour "+armour);}
        }
        /**
         * A real blow of 10 from the commander on the entity: the player attack damage source that PlayerEntity.attack strikes with, through
         * the entity's own damage. The blow lands whole (full health, no raised shield, no hurt cooldown, and the armour attributes its gear
         * gives set aside), so the health it loses is what landed. Everything is put back after.
         */
        static Blow strike(GravesAndGlory.Battle b,ServerPlayerEntity p,net.minecraft.entity.LivingEntity target,String what){
            var aside=new ArrayList<Map.Entry<net.minecraft.entity.attribute.EntityAttributeInstance,net.minecraft.entity.attribute.EntityAttributeModifier>>();
            try{
                for(var kind:List.of(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR,net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS)){
                    var a=target.getAttributeInstance(kind);if(a==null)continue;for(var m:new ArrayList<>(a.getModifiers())){a.removeModifier(m.getId());aside.add(Map.entry(a,m));}
                }
                target.clearActiveItem();target.setHealth(target.getMaxHealth());target.timeUntilRegen=0;target.hurtTime=0;
                float before=target.getHealth();int armour=target.getArmor();reissued.clear();watched=target;
                try{target.damage(b.world.getDamageSources().playerAttack(p),10f);}finally{watched=null;}
                return new Blow(what,before-target.getHealth(),List.copyOf(reissued),armour);
            }finally{ // the armour comes back whatever the blow did
                for(var e:aside)if(e.getKey().getModifier(e.getValue().getId())==null)e.getKey().addTemporaryModifier(e.getValue());
                target.setHealth(target.getMaxHealth());target.setVelocity(Vec3d.ZERO);target.timeUntilRegen=0;
            }
        }
        /** The player manager's own map of the players online (private; this is its Yarn name, which the dev client runs with). */
        @SuppressWarnings("unchecked")
        static Map<UUID,ServerPlayerEntity> playerMap(net.minecraft.server.PlayerManager m){
            try{var f=net.minecraft.server.PlayerManager.class.getDeclaredField("playerMap");f.setAccessible(true);return (Map<UUID,ServerPlayerEntity>)f.get(m);}
            catch(ReflectiveOperationException e){throw new IllegalStateException("PlayerManager.playerMap",e);}
        }
        /** A new player cannot be hurt for 60 ticks (joinInvulnerabilityTicks, private; the tick a FakePlayer skips counts it down): end that now. */
        static void vulnerable(ServerPlayerEntity p){
            try{var f=ServerPlayerEntity.class.getDeclaredField("joinInvulnerabilityTicks");f.setAccessible(true);f.setInt(p,0);}
            catch(ReflectiveOperationException e){throw new IllegalStateException("ServerPlayerEntity.joinInvulnerabilityTicks",e);}
        }
        /**
         * p2-commander-blows: the commander's own blows through Battle.allowDamage, struck for real (strike) and read as the health each
         * entity lost, with the one call allowDamage re-issues for a scaled blow. A foe troop takes 10 x damageScale (6) unmarked and
         * x(1 + value1) more under his Mark Prey (7.8). A foe troop that its own side's Stand Fast covers takes x(1 - value1) within the
         * Stand Fast class's radius of that side's commander (4.5) and 6 beyond it. The foe commander takes 10 unmarked (the plain path,
         * nothing re-issued) and 13 marked, with abilityData holding his UUID as a player mark should. The foe's commander is a Target in
         * the seat of the foe army's bot, which turns human and takes the Stand Fast class for these blows only, and the player manager
         * lists him while they land so that Commander.player finds him. All of it runs inside this server tick and is put back after.
         */
        void blows(GravesAndGlory.Battle b,ServerPlayerEntity p,GravesAndGlory.Squad own,Spec.ClassesRow warden,GravesAndGlory.Unit foe){
            listen();
            var keeper=Commander.byActive("stand_fast");double scale=Spec.COMBATDISCIPLINE[0].damageScale(),mark=1+warden.value1(),cover=keeper==null?Double.NaN:1-keeper.value1();
            List<Blow> got=new ArrayList<>();boolean ok=warden.active().equals("mark_prey")&&keeper!=null;String why="";
            var enemy=foe.squad;var players=playerMap(b.server.getPlayerManager());Target them=null;
            boolean wasBot=enemy.bot;String wasClass=enemy.commander;long wasUntil=enemy.abilityUntil;Object wasData=enemy.abilityData;
            try{
                own.abilityUntil=0;own.abilityData=null;var plain=strike(b,p,foe.mob,"a foe troop");
                own.abilityUntil=b.time+100;own.abilityData=foe.mob;var hunted=strike(b,p,foe.mob,"that troop under his Mark Prey");
                own.abilityUntil=0;own.abilityData=null;
                ok&=plain.is(10*scale,10*scale)&&hunted.is(10*scale*mark,10*scale*mark);got.add(plain);got.add(hunted);
                if(keeper!=null){
                    var other=unit(b,enemy,u->u!=foe);var troop=other==null?foe:other;Vec3d at=troop.mob.getPos();
                    them=new Target(b.world,new com.mojang.authlib.GameProfile(enemy.owner,"FoeCommander"));vulnerable(them);
                    players.put(them.getUuid(),them);
                    enemy.bot=false;enemy.commander=keeper.id();enemy.abilityUntil=b.time+100;
                    them.refreshPositionAndAngles(at.x+keeper.radius()/2,at.y,at.z,0,0);boolean found=Commander.player(b,enemy)==them;
                    var inside=strike(b,p,troop.mob,"a foe troop "+keeper.radius()/2+" blocks from its commander under his Stand Fast");
                    them.refreshPositionAndAngles(at.x+keeper.radius()+8,at.y,at.z,0,0);
                    var outside=strike(b,p,troop.mob,"that troop "+(keeper.radius()+8)+" blocks from him");
                    enemy.abilityUntil=0;them.refreshPositionAndAngles(at.x+2,at.y,at.z,0,0);
                    own.abilityUntil=b.time+100;own.abilityData=null;var pvp=strike(b,p,them,"the foe commander");
                    own.abilityData=them.getUuid();var marked=strike(b,p,them,"the foe commander under his Mark Prey");
                    ok&=found&&inside.is(10*scale*cover,10*scale*cover)&&outside.is(10*scale,10*scale)&&pvp.is(10,null)&&marked.is(10*mark,10*mark);
                    got.add(inside);got.add(outside);got.add(pvp);got.add(marked);if(!found)why="; Commander.player did not find the foe commander";
                }
            }finally{ // the foe army, his own and the player list are put back whatever a blow did
                if(them!=null)players.remove(them.getUuid());own.abilityUntil=0;own.abilityData=null;
                enemy.bot=wasBot;enemy.commander=wasClass;enemy.abilityUntil=wasUntil;enemy.abilityData=wasData;
            }
            report("p2-commander-blows",ok,"his blows of 10 as "+own.commander+" (damageScale "+scale+", Mark Prey x"+mark+", Stand Fast x"+cover+"): "+got+why);
        }
        /** A round of two seats a side with bot fill off and the commander alone on Dawn, so his side has an empty seat and no bot holds one. Returns false until it has started. */
        static boolean openRound(GravesAndGlory.Battle b,ServerPlayerEntity p){
            var c=b.campaign;if(b.running)c.returnLobby();
            if(c.map!=0){c.switchMap(0);return false;}
            if(!b.ready||!b.squads.containsKey(p.getUuid()))return false;
            c.closeParty();c.partyLeader=p.getUuid();c.party.add(p.getUuid());c.practice=false;c.size=2;c.fill=false;
            for(var s:b.squads.values())if(!s.bot){s.seated=s.owner.equals(p.getUuid());if(s.seated)s.team=0;}
            b.startRound();c.introUntil=b.time;return true;
        }

        public void cleanup(MinecraftClient c){
            server(c,b->{for(var e:saved.entrySet()){if(e.getValue()==null)Commander.ACTIVES.remove(e.getKey());else Commander.ACTIVES.put(e.getKey(),e.getValue());}saved.clear();
                if(b.running)b.campaign.returnLobby();var p=sp(b);if(p!=null)b.campaign.handle(p,"class:"+Spec.CLASSES[0].id());});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();keys(c);server(c,b->{player=id;next(b,1);});}
            String cmd=send;if(cmd!=null){send=null;GravesAndGloryClient.send(cmd);System.out.println("[p2] sent "+cmd);}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=12){shot(c,wantShot);shotTaken=true;}
            if(wantPress&&GravesAndGloryClient.battleActive()&&c.currentScreen==null){wantPress=false;net.minecraft.client.option.KeyBinding.onKeyPressed(net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.getBoundKeyOf(GravesAndGloryClient.ability));System.out.println("[p2] pressed the ability key");}
            if(stage==5&&GravesAndGloryClient.respawn>0)respawnSeen=Math.max(respawnSeen,GravesAndGloryClient.respawn);
            if((stage==34||stage==35)&&hudRight<0&&GravesAndGloryClient.battleActive()&&CommandMode.groupsRight>0){ // vanilla draws the hearts and armour from GUI x width / 2 - 91
                var win=c.getWindow();int sw=win.getScaledWidth(),sh=win.getScaledHeight();float scale=Math.min(1,sw/640f);
                hudHearts=(sw/2-91)/scale;hudFrame=Math.round(sw/scale)+"x"+Math.round(sh/scale)+" (window "+win.getFramebufferWidth()+"x"+win.getFramebufferHeight()+", GUI scale "+win.getScaleFactor()+")";hudRight=CommandMode.groupsRight;
            }
            if(stage==6&&!clientDone)barracksLines();
            if(stage==22&&!clientDone)tile();
            if(stage==34||stage==35||stage==36){var d=GravesAndGloryClient.abilityDrawn;if(d.size()==3){if(d.get(2).startsWith("ACTIVE ")&&!activeSeen){activeSeen=true;requestShot("commander-tile-active");}if(d.get(2).startsWith("READY IN "))readyInSeen=true;}}
            if(stage==41&&!pickerDone)picker(c);
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"commander");}
            if(tick>14000&&!finished){finished=true;report("p2-commander-timeout",false,"stuck in stage "+stage+" (class "+(ci<order.size()?order.get(ci):"-")+")");finish(c,"commander");}
        }

        /** The ability key's default is H, no other mod binding defaults to H, and hint names it H. */
        void keys(MinecraftClient c){
            var ability=GravesAndGloryClient.ability;var def=ability.getDefaultKey();
            boolean h=def.getCategory()==net.minecraft.client.util.InputUtil.Type.KEYSYM&&def.getCode()==org.lwjgl.glfw.GLFW.GLFW_KEY_H;
            List<String> mod=new ArrayList<>(),other=new ArrayList<>();int mine=0;
            for(var k:c.options.allKeys){if(k.getTranslationKey().startsWith("key.gravesandglory."))mine++;if(k==ability||!k.getDefaultKey().equals(def))continue;(k.getTranslationKey().startsWith("key.gravesandglory.")?mod:other).add(k.getTranslationKey());}
            String hint=GravesAndGloryClient.hint("ability");
            keyOk=h&&ability.getTranslationKey().equals("key.gravesandglory.ability")&&mod.isEmpty()&&hint.equals("H")&&GravesAndGloryClient.hint("menu").equals("R")&&GravesAndGloryClient.hint("nothing").equals("Not bound");
            keyWhy="key.gravesandglory.ability defaults to "+def.getTranslationKey()+" (H="+h+"); of "+mine+" mod bindings none else defaults to it="+mod.isEmpty()+(mod.isEmpty()?"":" "+mod)+(other.isEmpty()?"":" (vanilla or other mods on it: "+other+")")+"; hint(ability)=\""+hint+"\", hint(menu)=\""+GravesAndGloryClient.hint("menu")+"\", hint(nothing)=\""+GravesAndGloryClient.hint("nothing")+"\"";
        }
        /** Stage 6: the barracks lines on the client follow the class the state packet names (the Marshal's knight trains in 3.2 s, the Warlock's Sunweaver costs 56). */
        void barracksLines(){
            if(!GravesAndGloryClient.commanderActive.equals(expectActive)){if(++clientWait>300){clientOk=false;clientWhy="the state packet never named "+expectActive+" (it names \""+GravesAndGloryClient.commanderActive+"\")";clientDone=true;}return;}
            var knight=Campaign.troop("knight");var mage=Campaign.troop("mage");var row=RecruitmentScreen.commanderClass();
            String time=RecruitmentScreen.trainingTime(knight);int price=RecruitmentScreen.price(mage),knightPrice=RecruitmentScreen.price(knight);
            clientOk=switch(row.id()){case "marshal"->time.equals("Training time: 3.2 seconds")&&price==mage.cost();case "warlock"->price==56&&knightPrice==knight.cost()&&time.equals("Training time: 4 seconds");default->false;};
            clientWhy="the client reads "+row.id()+" from the state packet (\""+GravesAndGloryClient.commanderActive+"\"): knight \""+time+"\" at "+knightPrice+" gold, Sunweaver "+price+" gold";clientDone=true;
        }
        /** Stage 22: the HUD tile names the class active, shows the key chip H and NOT IN THIS BUILD (screenshot commander-tile). */
        void tile(){
            var d=GravesAndGloryClient.abilityDrawn;String want=Commander.activeName(expectActive).toUpperCase(Locale.ROOT);
            if(tileState==0){
                if(d.size()==3&&d.get(1).equals(want)&&d.get(2).equals("NOT IN THIS BUILD")){tileWhy="the tile drew "+d;clientOk=d.get(0).equals("H");requestShot("commander-tile");tileState=1;}
                else if(++tileWait>300){tileWhy="the tile drew "+d+" (want H, "+want+", NOT IN THIS BUILD)";clientOk=false;clientDone=true;}
            }else if(tileState==1&&shotTaken){wantShot=null;clientDone=true;}
        }
        /**
         * Stage 41: the camp class tab. Six cards in two rows, the hero track over the general track under their headings, every card's
         * text inside it; a click on the Warlock card picks it and CONFIRM CLASS sends it (screenshot commander-classes). Then the banner
         * editor's Wildmoot cards, which cannot be fielded, draw dimmed over their models (screenshot commander-banner-dim).
         */
        void picker(MinecraftClient c){
            // the war council closes itself while the client still thinks a battle runs, so it opens a second after the client sees the camp
            if(pick==0){if(GravesAndGloryClient.running){pickWait=0;return;}if(++pickWait<20)return;c.setScreen(new GravesAndGloryClient.LobbyScreen(2));pick=1;pickWait=0;openWait=0;return;}
            if(!(c.currentScreen instanceof CampaignLobbyScreen s)){
                if(++openWait>300){pickerOk=false;pickerWhy+="the war council never opened (the screen is "+(c.currentScreen==null?"none":c.currentScreen.getClass().getSimpleName())+"); ";pickerDone=true;}
                else if(pick==1&&openWait%40==0){c.setScreen(new GravesAndGloryClient.LobbyScreen(2));pickWait=0;}
                return;
            }
            if(pick==1&&++pickWait==20){
                var cards=s.children().stream().filter(v->v instanceof CampaignLobbyScreen.ClassCard).map(v->(CampaignLobbyScreen.ClassCard)v).toList();
                List<Integer> rows=cards.stream().map(k->k.getY()).distinct().sorted().toList();
                List<String> top=cards.stream().filter(k->!rows.isEmpty()&&k.getY()==rows.get(0)).sorted(Comparator.comparingInt(k->k.getX())).map(k->k.c.id()).toList();
                List<String> bottom=cards.stream().filter(k->rows.size()>1&&k.getY()==rows.get(1)).sorted(Comparator.comparingInt(k->k.getX())).map(k->k.c.id()).toList();
                List<String> heroes=Arrays.stream(Spec.CLASSES).filter(k->k.track().equals("hero")).map(Spec.ClassesRow::id).toList(),generals=Arrays.stream(Spec.CLASSES).filter(k->k.track().equals("general")).map(Spec.ClassesRow::id).toList();
                int clipped=cards.stream().mapToInt(k->k.clipped).sum();boolean inside=cards.stream().allMatch(k->k.getX()>=0&&k.getX()+k.getWidth()<=s.width&&k.getY()+k.getHeight()<=s.height-39);
                boolean selected=s.pendingClass.equals(CampaignClient.profile.commander);
                boolean ok=cards.size()==6&&rows.size()==2&&top.equals(heroes)&&bottom.equals(generals)&&heroes.size()==3&&generals.size()==3&&s.classHeadings.equals(List.of("HERO","GENERAL"))&&clipped==0&&inside&&selected;
                pickerOk=ok;pickerWhy+=cards.size()+" cards at rows "+rows+" (hero row "+top+", general row "+bottom+"), headings "+s.classHeadings+", "+clipped+" lines cut, inside the screen="+inside+" ("+s.width+"x"+s.height+"), the current class "+CampaignClient.profile.commander+" picked="+selected+"; ";
                var warlock=cards.stream().filter(k->k.c.id().equals("warlock")).findFirst().orElse(null);
                if(warlock!=null){double x=warlock.getX()+warlock.getWidth()/2.0,y=warlock.getY()+warlock.getHeight()/2.0;s.mouseClicked(x*s.uiScale,y*s.uiScale,0);s.mouseReleased(x*s.uiScale,y*s.uiScale,0);}
                pick=2;pickWait=0;return;
            }
            if(pick==2&&++pickWait==10){boolean picked=s.pendingClass.equals("warlock");pickerOk&=picked;pickerWhy+="a click on the Warlock card picks it="+picked+"; ";s.confirmClass.onPress();pick=3;pickWait=0;return;}
            if(pick==3){if(CampaignClient.profile.commander.equals("warlock")&&++pickWait>=10){requestShot("commander-classes");pick=4;}else if(++pickWait>300){pickerOk=false;pickerWhy+="CONFIRM CLASS never reached the profile (it says "+CampaignClient.profile.commander+"); ";pickerDone=true;}return;}
            if(pick==4&&shotTaken){wantShot=null;s.showHouse("wildmoot");s.showTab(1);pick=5;pickWait=0;return;}
            if(pick==5&&++pickWait==20){
                var cards=s.children().stream().filter(v->v instanceof ArmyCards.Recruit).map(v->(ArmyCards.Recruit)v).toList();
                boolean dim=!cards.isEmpty()&&cards.stream().allMatch(k->k.spec.house().equals("wildmoot")&&!k.active);
                pickerOk&=dim;pickerWhy+="the banner editor's Wildmoot tab shows "+cards.size()+" cards, all inactive="+dim+" (the dim at z 100 covers their models: see commander-banner-dim)";
                requestShot("commander-banner-dim");pick=6;return;
            }
            if(pick==6&&shotTaken){wantShot=null;c.setScreen(null);pickerDone=true;}
        }

        boolean serverOk,reaverOk,cycleOk;String serverWhy="",reaverWhy="",cycleWhy="";Spec.ClassesRow cycleClass;
        /** Stage 4: the passive of the class just set, in its skirmish, against the numbers of its sheet row (and the brief's values). */
        void classCheck(GravesAndGlory.Battle b){
            String id=order.get(ci);var c=Commander.row(id);var p=sp(b);var own=b.squads.get(player);
            if(p==null||own==null||!own.seated||!own.commander.equals(id)){passive(false,id,"no seated commander under "+id);ci++;next(b,1);return;}
            double health=p.getMaxHealth();var foe=enemy(b,own);var maxHealth=net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH;
            switch(c.passive()){
                case "champion"->{
                    var knight=unit(b,own,u->u.spec.id().equals("knight"));var bow=unit(b,own,u->u.spec.id().equals("archer"));
                    if(knight==null||bow==null||foe==null){passive(false,id,"the skirmish has no knight, archer or enemy");ci++;next(b,1);return;}
                    Vec3d home=knight.mob.getPos(),bowHome=bow.mob.getPos();
                    place(knight,p,5);var within=hit(b,knight,foe.mob,"melee");place(knight,p,20);var beyond=hit(b,knight,foe.mob,"melee");place(bow,p,5);var archer=hit(b,bow,foe.mob,"arrow");
                    own.abilityUntil=b.time+100; // Stand Fast's share on the receiving side, as if it ran: a foe's blow on the knight 6 blocks from him, then 20
                    place(knight,p,6);var guarded=hit(b,foe,knight.mob,"melee");place(knight,p,20);var exposed=hit(b,foe,knight.mob,"melee");
                    own.abilityUntil=0;knight.mob.refreshPositionAndAngles(home.x,home.y,home.z,0,0);bow.mob.refreshPositionAndAngles(bowHome.x,bowHome.y,bowHome.z,0,0);
                    p.damage(b.world.getDamageSources().generic(),1000f);Long until=b.respawns.get(player);long wait=until==null?-1:until-b.time;
                    boolean ok=near(health,24)&&near(health,p.getAttributeBaseValue(maxHealth)+c.passiveValue())&&near(factor(within),1.10)&&near(factor(beyond),1)&&near(factor(archer),1)&&near(factor(guarded),1-c.value1())&&near(factor(exposed),1)&&wait==GravesAndGlory.R.respawnSeconds()*20L;
                    passive(ok,id,"max health "+health+" (want 24); his knight 5 blocks off x"+factor(within)+" (want 1.1), at 20 x"+factor(beyond)+", an archer at 5 x"+factor(archer)+" (infantry only); with Stand Fast running a foe's blow on the knight 6 off x"+factor(guarded)+" (want "+(1-c.value1())+"), at 20 x"+factor(exposed)+"; he waits "+wait+" ticks to respawn (rules "+GravesAndGlory.R.respawnSeconds()+" s)");
                    ci++;next(b,1);
                }
                case "reaver"->{
                    var attack=p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);var mod=attack==null?null:attack.getModifier(Commander.ATTACK);
                    boolean blade=mod!=null&&near(mod.getValue(),2)&&near(mod.getValue(),c.passiveValue());
                    p.damage(b.world.getDamageSources().generic(),1000f);Long until=b.respawns.get(player);long wait=until==null?-1:until-b.time;
                    reaverOk=blade&&near(health,20)&&near(lobbyHealth,20)&&wait==180&&wait==Math.round(c.passiveValue2())*20L;
                    reaverWhy="attack modifier "+(mod==null?"none":"+"+mod.getValue())+" (want +2); max health "+health+" (the Champion's +4 left in the lobby: "+lobbyHealth+"); he waits "+wait+" ticks to respawn (want 180, 9 s)";
                    respawnSeen=-1;next(b,5);
                }
                case "warden"->{
                    var bow=unit(b,own,u->u.spec.id().equals("archer"));var knight=unit(b,own,u->u.spec.id().equals("knight"));
                    if(knight==null||bow==null||foe==null){passive(false,id,"the skirmish has no knight, archer or enemy");ci++;next(b,1);return;}
                    double reach=Abilities.range(b,bow),knightReach=Abilities.range(b,knight);var shot=hit(b,bow,foe.mob,"arrow");var blow=hit(b,knight,foe.mob,"melee");
                    own.abilityUntil=b.time+100;own.abilityData=foe.mob; // Mark Prey's share, as if it ran with this foe marked: on the mark only, for his troops and for himself
                    var marked=hit(b,bow,foe.mob,"arrow");var other=unit(b,foe.squad,u->u!=foe);var unmarked=other==null?null:hit(b,bow,other.mob,"arrow");
                    own.abilityUntil=0;own.abilityData=null;
                    boolean ok=near(reach,24)&&near(reach,bow.spec.range()+c.passiveValue2())&&near(knightReach,knight.spec.range())&&near(factor(shot),1.10)&&near(factor(blow),1)
                        &&near(factor(marked),(1+c.passiveValue())*(1+c.value1()))&&(other==null||near(factor(unmarked),1+c.passiveValue()))&&lobbyAttackGone;
                    passive(ok,id,"an archer's reach "+reach+" (sheet "+bow.spec.range()+", want 24), a knight's "+knightReach+"; the archer's shot x"+factor(shot)+" (want 1.1), the knight's blow x"+factor(blow)+"; with Mark Prey running on the foe the archer x"+factor(marked)+" (want 1.43), another foe x"+factor(unmarked)+"; the Reaver's +2 attack left in the lobby="+lobbyAttackGone);
                    blows(b,p,own,c,foe);
                    ci++;next(b,1);
                }
                case "marshal"->{
                    var f=Spec.FACTIONS[own.team];b.relocate(p,f.barracksX()+.5,Arena.ground(f.barracksX(),f.barracksZ())+1,f.barracksZ()+.5,0,0);
                    var knight=Campaign.troop("knight");boolean at=b.atBarracks(p,own);
                    b.manage(p,own,"disband:knight");long t0=b.time;int gold=own.gold;b.manage(p,own,"train:knight");
                    var queued=own.training.isEmpty()?null:own.training.getLast();long took=queued==null?-1:queued.finished()-t0;
                    serverOk=at&&queued!=null&&Math.round(knight.trainingSeconds()*20)==80&&took==64&&Commander.trainingTicks(own,knight)==64&&queued.paid()==knight.cost()&&own.gold==gold-knight.cost();
                    serverWhy="at the barracks="+at+"; the sheet's knight trains "+Math.round(knight.trainingSeconds()*20)+" ticks, his train:knight finishes "+took+" ticks later (want 64) for "+(queued==null?-1:queued.paid())+" gold";
                    b.manage(p,own,"cancel-training");expectActive=c.active();clientDone=false;clientWait=0;next(b,6);
                }
                case "warlock"->{
                    var f=Spec.FACTIONS[own.team];b.relocate(p,f.barracksX()+.5,Arena.ground(f.barracksX(),f.barracksZ())+1,f.barracksZ()+.5,0,0);
                    field(b,own,banner("knight*12,archer*6,mage,^"));var mage=Campaign.troop("mage");var knight=Campaign.troop("knight");
                    b.manage(p,own,"disband:mage");own.gold=100;b.manage(p,own,"train:mage");var queued=own.training.isEmpty()?null:own.training.getLast();int spent=100-own.gold;
                    b.manage(p,own,"cancel-training");int back=own.gold;
                    var free=new GravesAndGlory.Training(mage,b.time+1000);own.training.addLast(free);b.manage(p,own,"cancel-training");int afterFree=own.gold; // the short form is a free troop: its cancel pays nothing back
                    serverOk=mage.magic()&&mage.cost()==70&&spent==56&&queued!=null&&queued.spec()==mage&&queued.paid()==56&&back==100&&Commander.cost(own,mage)==56&&Commander.cost(own,knight)==knight.cost()&&free.paid()==0&&afterFree==back;
                    serverWhy="the Sunweaver (sheet "+mage.cost()+" gold, magic="+mage.magic()+") cost him "+spent+" (want 56), its queue entry holds "+(queued==null?-1:queued.paid())+" and cancel-training brings his gold back to "+back+" (want 100); a knight costs "+Commander.cost(own,knight)+"; a free queue entry holds "+free.paid()+" and its cancel leaves his gold at "+afterFree;
                    expectActive=c.active();clientDone=false;clientWait=0;next(b,6);
                }
                case "quartermaster"->{
                    var E=Spec.ECONOMY[0];for(var pt:b.points){pt.owner=-1;pt.contested=false;pt.progress=0;pt.challenger=-1;}
                    b.points.get(0).owner=own.team;int one=b.income(own);b.points.get(1).owner=own.team;int two=b.income(own);b.points.get(1).owner=-1;
                    serverOk=Commander.pointGoldBonus(own)==2&&one==E.passiveGold()+5&&two==E.passiveGold()+10;
                    serverWhy="with one held point he earns "+one+" a tick (passive "+E.passiveGold()+" + 5), with two "+two+" (+10)";
                    goldMark=-1;incomeDelta=-1;next(b,7);
                }
                default->{passive(false,id,"no check for the passive "+c.passive());ci++;next(b,1);}
            }
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(player==null)return;
            switch(stage){
                case 1->{ // the next class: back in camp (class:(id) is locked in a running battle), then the real class packet
                    if(ci>=order.size()){report("p2-class-passives",passivesOk,passiveWhy.toString());next(b,19);return;}
                    if(b.running)b.campaign.returnLobby();var p=sp(b);if(p==null)return;
                    if(ci>0&&order.get(ci-1).equals("champion"))lobbyHealth=p.getMaxHealth();
                    if(ci>0&&order.get(ci-1).equals("reaver")){var a=p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);lobbyAttackGone=a!=null&&a.getModifier(Commander.ATTACK)==null;}
                    command(b,"class:"+order.get(ci),2);
                }
                case 2->{if(since(b)<20)return;String id=order.get(ci);var own=b.squads.get(player);
                    if(profile(b).commander.equals(id)&&own!=null&&own.commander.equals(id))next(b,3);
                    else if(since(b)>200){passive(false,id,"class:"+id+" never reached the profile (it says "+profile(b).commander+")");ci++;next(b,1);}}
                case 3->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,4);}
                case 4->{if(since(b)>=10)classCheck(b);}
                case 5->{ // the Reaver waits out his 9 s, and the respawn equips him again: his +2 attack comes back with it
                    var p=sp(b);if(p==null)return;boolean back=!b.respawns.containsKey(player);if(!back&&since(b)<400)return;
                    var attack=p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);var mod=attack==null?null:attack.getModifier(Commander.ATTACK);
                    boolean again=back&&since(b)>=180&&since(b)<=182&&!p.isSpectator()&&mod!=null&&near(mod.getValue(),2)&&near(p.getMaxHealth(),20)&&Commander.player(b,b.squads.get(player))==p;
                    passive(reaverOk&&again&&respawnSeen==9,"reaver",reaverWhy+"; the client's countdown read "+respawnSeen+" s; he was back "+(back?"after "+since(b)+" ticks":"never")+" (want 180) as "+(p.isSpectator()?"a spectator":"a commander")+" with attack modifier "+(mod==null?"none":"+"+mod.getValue())+" and max health "+p.getMaxHealth());
                    ci++;next(b,1);}
                case 6->{if(!clientDone){if(since(b)>400){clientOk=false;clientWhy="the client never checked";clientDone=true;}return;}
                    clientDone=false;passive(serverOk&&clientOk,order.get(ci),serverWhy+"; "+clientWhy);ci++;next(b,1);}
                case 7->{ // the Quartermaster's gold over a real income tick
                    long period=Spec.ECONOMY[0].incomeSeconds()*20L;var own=b.squads.get(player);
                    if(goldMark<0){if((b.time+1)%period==0){goldMark=own.gold;markAt=b.time;}}
                    else if(b.time==markAt+1){incomeDelta=(int)(own.gold-goldMark);long held=b.points.stream().filter(pt->pt.owner==own.team).count();
                        passive(serverOk&&incomeDelta==Spec.ECONOMY[0].passiveGold()+5,"quartermaster",serverWhy+"; over the income tick his gold rose by "+incomeDelta+" (want "+(Spec.ECONOMY[0].passiveGold()+5)+") with "+held+" point held");ci++;next(b,1);}
                    else goldMark=-1;
                    if(stage==7&&since(b)>600){passive(false,"quartermaster",serverWhy+"; no income tick was seen");ci++;next(b,1);}
                }
                case 19->{var p=sp(b);if(p==null)return;if(!b.running||!b.squads.get(player).seated){Phase1Checks.skirmish(b,p,0,1,"normal");stageAt=b.time;return;}if(since(b)>=10)next(b,20);}
                case 20->{ // p2-ability-key: with no active registered for his class, the key gives the notice and starts no cooldown
                    var own=b.squads.get(player);var c=Commander.row(own);saved.putIfAbsent(c.active(),Commander.ACTIVES.get(c.active()));Commander.ACTIVES.remove(c.active());
                    b.campaign.notices.remove(player);expectActive=c.active();wantPress=true;next(b,21);
                }
                case 21->{if(since(b)<20)return;var own=b.squads.get(player);String notice=b.campaign.notices.getOrDefault(player,"");
                    keyServerOk=notice.equals(Commander.NOT_READY)&&own.abilityReadyAt==0&&own.abilityUntil==0&&Commander.readyIn(b,own)==-1;
                    keyServerWhy="after the key as "+own.commander+": notice \""+notice+"\", abilityReadyAt "+own.abilityReadyAt+", abilityUntil "+own.abilityUntil+", readyIn "+Commander.readyIn(b,own);
                    clientOk=false;clientDone=false;tileState=0;tileWait=0;next(b,22);}
                case 22->{if(!clientDone){if(since(b)>400){tileWhy+=" (the client never finished)";clientDone=true;}return;}
                    clientDone=false;report("p2-ability-key",keyOk&&keyServerOk&&clientOk,keyWhy+"; "+keyServerWhy+"; "+tileWhy);next(b,30);}
                case 30->{ // p2-ability-cycle: the class whose active is shortest, with a probe registered for it
                    if(b.running)b.campaign.returnLobby();cycleClass=Arrays.stream(Spec.CLASSES).min(Comparator.comparingInt(Spec.ClassesRow::durationTicks)).orElseThrow();command(b,"class:"+cycleClass.id(),31);}
                case 31->{if(since(b)<20)return;if(profile(b).commander.equals(cycleClass.id()))next(b,32);else if(since(b)>200){report("p2-ability-cycle",false,"class:"+cycleClass.id()+" never reached the profile");next(b,50);}}
                case 32->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,33);}
                case 33->{if(since(b)<10)return;probe=new Probe();saved.putIfAbsent(cycleClass.active(),Commander.ACTIVES.get(cycleClass.active()));Commander.ACTIVES.put(cycleClass.active(),probe);
                    b.campaign.notices.remove(player);expectActive=cycleClass.active();activeSeen=false;readyInSeen=false;wantPress=true;next(b,34);}
                case 34->{var own=b.squads.get(player);
                    if(probe.starts>0){long t0=probe.startedAt;
                        cycleOk=probe.starts==1&&own.abilityReadyAt==t0+cycleClass.cooldownTicks()&&own.abilityUntil==t0+cycleClass.durationTicks()&&"probe".equals(own.abilityData)&&Commander.running(b,own);
                        cycleWhy=cycleClass.id()+"'s "+cycleClass.active()+" started at "+t0+": ready again at +"+(own.abilityReadyAt-t0)+" (want "+cycleClass.cooldownTicks()+"), runs to +"+(own.abilityUntil-t0)+" (want "+cycleClass.durationTicks()+")";next(b,35);}
                    else if(since(b)>300){report("p2-ability-cycle",false,"the probe never started (notice \""+b.campaign.notices.getOrDefault(player,"")+"\")");next(b,50);}}
                case 35->{var own=b.squads.get(player);if(b.time<probe.startedAt+cycleClass.durationTicks()+3)return;
                    boolean ended=probe.ends==1&&own.abilityUntil==0&&own.abilityData==null&&!Commander.running(b,own)&&probe.ticks>=cycleClass.durationTicks()-3&&probe.ticks<=cycleClass.durationTicks();
                    cycleOk&=ended;cycleWhy+="; it ticked "+probe.ticks+" times and ended "+probe.ends+" time(s), abilityData cleared="+(own.abilityData==null);
                    b.campaign.notices.remove(player);wantPress=true;next(b,36);}
                case 36->{if(since(b)<30)return;var own=b.squads.get(player);String notice=b.campaign.notices.getOrDefault(player,"");String want=Commander.activeName(cycleClass.active())+" is ready in ";
                    boolean refused=probe.starts==1&&notice.startsWith(want)&&notice.endsWith(" s.")&&own.abilityReadyAt==probe.startedAt+cycleClass.cooldownTicks();
                    cycleOk&=refused&&activeSeen&&readyInSeen;
                    report("p2-ability-cycle",cycleOk,cycleWhy+"; a second press on cooldown: notice \""+notice+"\", starts "+probe.starts+"; the tile showed ACTIVE="+activeSeen+" and READY IN="+readyInSeen);
                    report("p2-hud-hearts",hudRight>0&&hudRight<hudHearts&&hudRight>=Math.min(218,hudHearts-8),"in the HUD frame "+hudFrame+" the group panels end at x "+hudRight+" and vanilla's hearts and armour start at x "+hudHearts+" (see commander-tile-active)");
                    next(b,37);}
                case 37->{ // p2-ability-seat: the probe stops when its commander leaves; the stand-in keeps the seat's cooldown, and so does he when he takes the seat back
                    var p=sp(b);var own=b.squads.get(player);if(p==null||own==null)return;
                    probe=new Probe();Commander.ACTIVES.put(cycleClass.active(),probe);own.abilityReadyAt=0;b.command(p,"ability");
                    if(probe.starts!=1||!Commander.running(b,own)){report("p2-ability-seat",false,"the probe did not start (starts "+probe.starts+")");next(b,39);return;}
                    long t0=probe.startedAt;seat=own;seatReadyAt=own.abilityReadyAt;
                    b.leave(player);UUID stand=b.campaign.vacated.get(player);
                    seatOk=probe.ends==1&&own.abilityUntil==0&&own.abilityData==null&&own.abilityReadyAt==t0+cycleClass.cooldownTicks()&&own.bot&&stand!=null&&own.owner.equals(stand)&&b.squads.get(stand)==own&&!b.squads.containsKey(player);
                    seatWhy="he left while "+cycleClass.active()+" ran: it ended "+probe.ends+" time(s), abilityData "+own.abilityData+", a stand-in took the seat="+(own.bot&&own.owner.equals(stand))+" and keeps its cooldown to +"+(own.abilityReadyAt-t0)+" (want "+cycleClass.cooldownTicks()+")";
                    own.abilityUntil=b.time+cycleClass.durationTicks();own.abilityData="probe"; // as if the stand-in ran the class active, so the takeover has one to stop
                    next(b,38);}
                case 38->{ // the next battle tick welcomes him again and he takes his reserved seat back: the takeover
                    var own=b.squads.get(player);
                    if(own==null){if(since(b)>100){report("p2-ability-seat",false,seatWhy+"; he never got his seat back");next(b,39);}return;}
                    boolean back=own==seat&&!own.bot&&own.seated&&probe.ends==2&&own.abilityUntil==0&&own.abilityData==null&&own.abilityReadyAt==seatReadyAt&&!b.campaign.vacated.containsKey(player)&&own.commander.equals(profile(b).commander);
                    b.campaign.notices.remove(player);var p=sp(b);if(p!=null)b.command(p,"ability");String notice=b.campaign.notices.getOrDefault(player,"");
                    boolean refused=probe.starts==1&&notice.startsWith(Commander.activeName(cycleClass.active())+" is ready in ");
                    report("p2-ability-seat",seatOk&&back&&refused,seatWhy+"; he took the same seat back="+(own==seat)+" after "+since(b)+" tick(s): the stand-in's running active ended there (ends "+probe.ends+", want 2), abilityData "+own.abilityData+", the cooldown kept="+(own.abilityReadyAt==seatReadyAt)+"; his press: \""+notice+"\" (starts "+probe.starts+")");
                    next(b,39);}
                case 39->{ // p2-ability-stops: a round start and the lobby end a running active once, clear its data and reset its cooldown
                    var p=sp(b);var own=b.squads.get(player);if(p==null||own==null)return;
                    boolean ok=true;StringBuilder why=new StringBuilder();
                    for(String path:List.of("startRound","returnLobby")){
                        probe=new Probe();Commander.ACTIVES.put(cycleClass.active(),probe);own.abilityReadyAt=0;b.campaign.introUntil=b.time;b.command(p,"ability");
                        boolean ran=probe.starts==1&&Commander.running(b,own)&&own.abilityReadyAt>b.time;
                        if(path.equals("startRound")){b.startRound();b.campaign.introUntil=b.time;}else b.campaign.returnLobby();
                        boolean stopped=probe.ends==1&&own.abilityUntil==0&&own.abilityData==null&&own.abilityReadyAt==0&&!Commander.running(b,own);
                        ok&=ran&&stopped;why.append(path).append(" while ").append(cycleClass.active()).append(" ran (it started=").append(ran).append("): it ended ").append(probe.ends).append(" time(s), abilityData ").append(own.abilityData).append(", abilityReadyAt ").append(own.abilityReadyAt).append("; ");
                    }
                    report("p2-ability-stops",ok,why.toString());
                    for(var e:saved.entrySet()){if(e.getValue()==null)Commander.ACTIVES.remove(e.getKey());else Commander.ACTIVES.put(e.getKey(),e.getValue());}saved.clear();next(b,50);}
                case 50->{ // p2-fresh-seat: a commander seated fresh in a running round (bot fill off, an empty seat on his side) fights under his own class, here the Reaver
                    if(b.running)b.campaign.returnLobby();command(b,"class:reaver",51);}
                case 51->{if(since(b)<20)return;if(profile(b).commander.equals("reaver"))next(b,52);else if(since(b)>200){report("p2-fresh-seat",false,"class:reaver never reached the profile (it says "+profile(b).commander+")");next(b,40);}}
                case 52->{var p=sp(b);if(p==null||!openRound(b,p))return;next(b,53);}
                case 53->{if(since(b)<10)return; // his squad goes with no stand-in, as if he connected now for the first time this round
                    var old=b.squads.remove(player);freshOld=old;b.welcomed.remove(player);b.respawns.remove(player);b.campaign.vacated.remove(player);
                    b.units.removeIf(u->{if(u.squad==old){u.mob.discard();return true;}return false;});freshBots=b.squads.values().stream().filter(s->s.bot&&s.seated).count();next(b,54);}
                case 54->{var s=b.squads.get(player);var p=sp(b);
                    if(s==null||p==null){if(since(b)>100){report("p2-fresh-seat",false,"he was never seated again");b.campaign.returnLobby();next(b,40);}return;}
                    var attack=p.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);var mod=attack==null?null:attack.getModifier(Commander.ATTACK);
                    boolean ok=freshBots==0&&s!=freshOld&&s.seated&&!s.bot&&s.commander.equals("reaver")&&mod!=null&&near(mod.getValue(),2)&&near(p.getMaxHealth(),20)&&Commander.respawnSeconds(s)==9;
                    report("p2-fresh-seat",ok,"bot fill off, two seats a side, "+freshBots+" bot seats: he joined into a new seat="+(s!=freshOld)+" (seated "+s.seated+") as "+s.commander+" (his class is reaver): attack modifier "+(mod==null?"none":"+"+mod.getValue())+", max health "+p.getMaxHealth()+" (a Champion has 24), respawn "+Commander.respawnSeconds(s)+" s");
                    b.campaign.returnLobby();next(b,40);}
                case 40->{if(b.running)b.campaign.returnLobby();pick=0;pickWait=0;pickerOk=false;pickerWhy="";pickerDone=false;next(b,41);}
                case 41->{if(!pickerDone){if(since(b)>1200){report("p2-class-picker",false,"timed out: "+pickerWhy);next(b,99);}return;}
                    boolean confirmed=profile(b).commander.equals("warlock");report("p2-class-picker",pickerOk&&confirmed,pickerWhy+"; the profile now says "+profile(b).commander);next(b,99);}
                default->{}
            }
        }
    }

    /**
     * Task 6: the Gilded Order, batch 1. On a stone sky platform 40 blocks east of Dawn's base (off the base, so no group there is ever
     * home): Order Archers on Hold (p2-gilded-steady); the Order Longbow's pierce through a real arrow (p2-gilded-pierce); the Sunweaver's
     * heal and cooldown (p2-gilded-mend); the batch's counter rows with synthetic husk rows (p2-counters-gilded); Bulwark's share and
     * covered flanks on the commander's own army at tiers III and I (p2-bulwark); and the Lord Marshal's Oath Banner, its aura and its
     * anchoring, through real orders (p2-gilded-oath, p2-gilded-oath-plant, p2-gilded-oath-squad). Then the Champion's Stand Fast and the
     * Marshal's Muster through the ability command, with the HUD tile read on
     * the client (p2-champion-standfast, p2-marshal-muster, p2-marshal-muster-spots); the showcase on map 0 (p2-gilded-showcase, screenshot gilded-showcase);
     * and the tick cost under the hard Gilded and Graveborn banners (p2-gilded-tick-cost). Stages run on the server tick; the client sends
     * the class commands, watches the tile and the House strip and takes the screenshot.
     */
    static final class Gilded implements Suite,ServerWatch {
        record Pin(GravesAndGlory.Unit u,Vec3d at,float yaw){}
        /** The showcase spot on map 0, the centre of the shield line: the open valley Phase 1's morale fixture uses. */
        static final double SX=40,SZ=0;
        volatile int stage;volatile UUID player;volatile String send,wantShot;volatile boolean shotTaken,finished;
        long stageAt;int shotDelay;String classBefore;
        BlockPos site;final List<GravesAndGlory.Unit> fixture=new ArrayList<>(),shielded=new ArrayList<>(),knights=new ArrayList<>(),runners=new ArrayList<>();final List<Pin> pins=new ArrayList<>();boolean raise;
        final Set<net.minecraft.util.math.ChunkPos> forced=new HashSet<>();
        GravesAndGlory.Squad own,dawn,dusk;
        GravesAndGlory.Unit bow,first,behind,beside,mage,ally,foe,foe2,guard,marshal,near,far,support,outside,late;
        GravesAndGlory.Unit tShield,aPlain,aSunder,aBolt,aCaster,tArcher,aCharger,tRear,aRear;
        // pierce, mend and Bulwark
        Projectiles.Shot shot;Combat.HitLog mark,arrowI;float hpFirst,hpBehind,hpBeside;boolean pierceOk;String pierceWhy="";
        final List<Long> casts=new ArrayList<>(),heals=new ArrayList<>();final List<Float> healed=new ArrayList<>();long castCooldownAt=-1;
        boolean bulwarkOk,heldIII;String bulwarkWhy="";
        // side teams: one audit after each place the suite discards troops directly (Unit.mob.discard() with no leaveSide of its own)
        int audits;boolean unloadWasOnTeam;final Map<String,String> staleSeen=new LinkedHashMap<>();final List<String> auditNotes=new ArrayList<>();
        // the Oath Banner
        Abilities.Banner planted;final Set<Abilities.Banner> seen=new LinkedHashSet<>();long plantAt=-1,secondAt=-1,slowPutAt=-1;
        boolean oathOk,fell,blockBack,beatCleared,unanchored,slowAfter;String oathWhy="";int heardBefore;
        // the actives, and the tile the client watches while tileWatch is 1
        volatile int tileWatch;volatile String firstActive="",readyFirst="",readyLater="";volatile int activeCooldownSeconds=-1;int readyFirstTick;
        long t0;boolean activeOk,lateHastened;String activeWhy="";double startDistance;long rallyBefore;List<String> listenerCues=List.of();
        // the showcase and the tick cost
        volatile boolean hudOk,clientOk;volatile String hudWhy="",clientWhy="";int arrowsAtShot;TickCost cost;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Profile profile(GravesAndGlory.Battle b){return b.campaign.profile(player);}
        void requestShot(String name){shotTaken=false;shotDelay=0;wantShot=name;}
        void command(GravesAndGlory.Battle b,String cmd,int nextStage){send=cmd;next(b,nextStage);}
        static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}
        static String fmt(double v){return String.format(Locale.ROOT,"%.2f",v);}
        static String show(Combat.HitLog h){return Engine.show(h);}
        static boolean is(Combat.HitLog h,double counters,String row){return h!=null&&near(h.counters(),counters)&&h.rows().equals(List.of(row));}
        static boolean traits(Combat.HitLog h,double v){return h!=null&&near(h.traits(),v);}
        static boolean auras(Combat.HitLog h,double v){return h!=null&&near(h.auras(),v);}
        static String tr(Combat.HitLog h){return h==null?"none":String.valueOf(h.traits());}
        static String au(Combat.HitLog h){return h==null?"none":String.valueOf(h.auras());}
        /** The seconds a tile state names ("ACTIVE 8 s", "READY IN 82 s"), or -1. */
        static int seconds(String state){var m=java.util.regex.Pattern.compile("(\\d+) s$").matcher(state);return m.find()?Integer.parseInt(m.group(1)):-1;}
        /** The newest logged hit on the entity after the mark (a null mark: the whole log), or null. */
        static Combat.HitLog hitOn(net.minecraft.entity.LivingEntity t,Combat.HitLog mark){Combat.HitLog out=null;boolean after=mark==null;for(var h:Combat.LAST){if(h==mark){after=true;continue;}if(after&&h.target()==t.getId())out=h;}return out;}
        /** The health the squad's living troops of the group have now. */
        static double living(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String group){double h=0;for(var u:b.units)if(u.squad==s&&u.spec.group().equals(group)&&u.mob.isAlive())h+=u.mob.getHealth();return h;}
        static double distance(net.minecraft.entity.Entity a,net.minecraft.entity.Entity c){return Math.sqrt(GravesAndGlory.Battle.horizontalDistance(a.getPos(),c.getPos()));}
        static double distance(Vec3d a,net.minecraft.entity.Entity c){return Math.sqrt(GravesAndGlory.Battle.horizontalDistance(a,c.getPos()));}
        /** The mean horizontal distance of the fixture's living troops to the commander. */
        double meanDistance(ServerPlayerEntity p){double sum=0;int n=0;for(var u:fixture)if(u.mob.isAlive()){sum+=distance(u.mob,p);n++;}return n==0?-1:sum/n;}
        /** A husk row made for the counters check: an Order Squire (melee) or an Order Archer (ranged) carrying this ability and these tags. */
        static Spec.TroopsRow synthetic(String id,String name,boolean ranged,String ability,String tags){
            var k=Campaign.troop(ranged?"archer":"knight");
            return new Spec.TroopsRow(id,k.house(),k.rarity(),name,"husk",k.group(),k.health(),k.damage(),k.range(),k.ranged(),k.speed(),k.cooldown(),k.cost(),k.trainingSeconds(),false,k.weapon(),k.helmet(),k.chest(),tags,ability,"none",k.slotWeight(),k.pennant(),k.shield(),k.texture());
        }
        static Vec3d ground(GravesAndGlory.Battle b,double x,double z){return new Vec3d(x,Phase1Checks.top(b,x,z),z);}
        static void place(GravesAndGlory.Battle b,GravesAndGlory.Unit u,double x,double z,float yaw){u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(x,Phase1Checks.top(b,x,z),z,yaw,0);u.mob.setHeadYaw(yaw);u.mob.setBodyYaw(yaw);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;}
        /** The floor (stone, or air to take it away) under the standing layer at c, half blocks each way, and clear air above it. */
        void platform(GravesAndGlory.Battle b,BlockPos c,int half,net.minecraft.block.BlockState floor){
            for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){b.world.setBlockState(c.add(x,-1,z),floor);for(int y=0;y<=6;y++)b.world.setBlockState(c.add(x,y,z),Blocks.AIR.getDefaultState());}
        }
        void force(GravesAndGlory.Battle b,BlockPos from,BlockPos to){
            for(int cx=from.getX()>>4;cx<=to.getX()>>4;cx++)for(int cz=from.getZ()>>4;cz<=to.getZ()>>4;cz++){b.world.setChunkForced(cx,cz,true);b.world.getChunk(cx,cz);forced.add(new net.minecraft.util.math.ChunkPos(cx,cz));}
        }
        void unforce(GravesAndGlory.Battle b){for(var p:forced)b.world.setChunkForced(p.x,p.z,false);forced.clear();}
        /** A troop on the platform at (dx, dz) from its centre with its AI off, held there each tick by the pins; a frozen troop never attacks or casts. */
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Spec.TroopsRow troop,double dx,double dz,float yaw,boolean freeze){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);
            var u=b.spawn(s,troop,30+fixture.size(),at);u.mob.setAiDisabled(true);if(freeze)Phase1Checks.freeze(u);
            fixture.add(u);pins.add(new Pin(u,at,yaw));u.deployed=at;Engine.apply(u,at,yaw);return u;
        }
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw,boolean freeze){return post(b,s,Campaign.troop(troop),dx,dz,yaw,freeze);}
        /** A troop of the commander's own army on the platform, free to walk (no pin, its AI on), that neither attacks nor casts. */
        GravesAndGlory.Unit free(GravesAndGlory.Battle b,String troop,double dx,double dz){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);var u=b.spawn(own,Campaign.troop(troop),30+fixture.size(),at);Phase1Checks.freeze(u);fixture.add(u);u.deployed=at;return u;
        }
        void move(GravesAndGlory.Unit u,double dx,double dz,float yaw){Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);pins.removeIf(p->p.u()==u);pins.add(new Pin(u,at,yaw));u.deployed=at;Engine.apply(u,at,yaw);}
        void drop(GravesAndGlory.Battle b,GravesAndGlory.Unit u){u.mob.discard();b.units.remove(u);fixture.remove(u);shielded.remove(u);pins.removeIf(p->p.u()==u);audit(b,"drop",false);}
        void clearFixture(GravesAndGlory.Battle b){for(var u:fixture)u.mob.discard();b.units.removeAll(fixture);fixture.clear();shielded.clear();pins.clear();raise=false;audit(b,"clearFixture",false);}
        /** Right after a direct discard: no gg_ side team keeps a member that is not a living troop of b.units (and with `none` standing, none keeps any). */
        void audit(GravesAndGlory.Battle b,String where,boolean none){
            var stale=staleSides(b);var all=sideMembers(b);audits++;
            for(String m:none?all:stale)staleSeen.putIfAbsent(m,where); // each leftover member once, with the first place it was seen
            if(auditNotes.size()<12)auditNotes.add(where+" "+all.size()+" on a team");
        }
        /** After each server tick: the pinned troops stand where the stage put them, and the troops in `shielded` hold their shields as `raise` says. */
        void hold(){
            for(var p:pins)if(p.u().mob.isAlive())Engine.apply(p.u(),p.at(),p.yaw());
            for(var u:shielded)if(u.mob.isAlive()){if(raise&&!u.mob.isUsingItem())u.mob.setCurrentHand(net.minecraft.util.Hand.OFF_HAND);else if(!raise&&u.mob.isUsingItem())u.mob.stopUsingItem();}
        }
        static void reset(GravesAndGlory.Squad s){s.lastStand.clear();s.lastStandAt.clear();s.fullHealth.clear();s.assignments.clear();s.arrived.clear();}
        /** Every army's troops leave and none come back; a stone sky platform 40 blocks east of Dawn's base; two ad hoc armies (bots, so no profile is read) beside the commander's own, which keeps his seat and his class. */
        void stageFixture(GravesAndGlory.Battle b){
            Projectiles.clear(b);Abilities.clearBanners(b);clearFixture(b);
            for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;s.training.clear();if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
            for(var u:b.units)u.mob.discard();b.units.clear();audit(b,"stageFixture",true);
            var base=GravesAndGlory.Battle.base(0);site=new BlockPos((int)base.x+40,140,(int)base.z);force(b,site.add(-13,0,-13),site.add(13,0,13));platform(b,site,12,Blocks.STONE.getDefaultState());
            own=b.squads.get(player);reset(own);
            dawn=new GravesAndGlory.Squad(UUID.randomUUID(),0,base);dawn.order="hold";dawn.bot=true;dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,base);dusk.order="hold";dusk.bot=true;
            // a removal that is neither a kill nor a discard, as a chunk unload does it: vanilla's destroy step (which clears the scoreboard entry) does not run for it, so only the ENTITY_UNLOAD hook can
            var gone=post(b,dawn,"knight",0,-9,0,false);unloadWasOnTeam=sideMembers(b).stream().anyMatch(m->m.endsWith(gone.mob.getEntityName()));
            gone.mob.setRemoved(Entity.RemovalReason.UNLOADED_TO_CHUNK);b.units.remove(gone);fixture.remove(gone);pins.removeIf(p->p.u()==gone);audit(b,"chunk unload",true);
        }
        /** The real order path (Battle.tactical) for one of the commander's groups, past the command cooldown. */
        void order(GravesAndGlory.Battle b,String group,String action,String formation,double x,double z,float yaw){var p=sp(b);own.lastOrder=-1000;b.tactical(p,group,action,formation,x,z,yaw,-1);}

        public void cleanup(MinecraftClient c){
            server(c,b->{tileWatch=0;clearFixture(b);Abilities.clearBanners(b);if(site!=null)platform(b,site,12,Blocks.AIR.getDefaultState());unforce(b);if(cost!=null)cost.handBack();if(b.running)b.campaign.returnLobby();
                var p=sp(b);if(p!=null&&classBefore!=null)b.campaign.handle(p,"class:"+classBefore);});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            String cmd=send;if(cmd!=null){send=null;GravesAndGloryClient.send(cmd);System.out.println("[p2] sent "+cmd);}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=12){
                if(wantShot.equals("gilded-showcase")){var h=GravesAndGloryClient.houses;hudOk=h.getOrDefault("gilded",0)==3;hudWhy="the client's House strip holds "+h+" (Gilded at III="+hudOk+")";
                    // the troops the client itself holds around the line (the picture draws the client's world, not the server's)
                    Map<String,Integer> near=new TreeMap<>();for(var e:c.world.getEntities())if(e.getCustomName()!=null&&e.isAlive()&&Math.abs(e.getX()-(SX-6))<=10&&Math.abs(e.getZ()-(SZ+2))<=10){var t=ArmyMobs.named(e.getCustomName().getString());if(t!=null&&t.house().equals("gilded"))near.merge(t.id(),1,Integer::sum);}
                    clientOk=near.getOrDefault("knight",0)>0&&near.getOrDefault("sentinel",0)>0&&near.getOrDefault("longbow",0)>0;clientWhy="the client's world holds "+near+" Gilded troops within 10 blocks of the line's middle (Squire, Templar and Longbow="+clientOk+")";}
                shot(c,wantShot);shotTaken=true;
            }
            if(tileWatch==1){var d=GravesAndGloryClient.abilityDrawn;
                if(d.size()==3){String st=d.get(2);
                    if(firstActive.isEmpty()){if(st.startsWith("ACTIVE ")){firstActive=st;activeCooldownSeconds=(GravesAndGloryClient.commanderReady+19)/20;}}
                    else if(st.startsWith("READY IN ")){if(readyFirst.isEmpty()){readyFirst=st;readyFirstTick=tick;}else if(readyLater.isEmpty()&&tick-readyFirstTick>=40)readyLater=st;}
                }
            }
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"gilded");}
            if(tick>24000&&!finished){finished=true;report("p2-gilded-timeout",false,"stuck in stage "+stage);finish(c,"gilded");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(player==null)return;
            hold();
            switch(stage){
                case 1->{if(b.running){b.campaign.returnLobby();return;}classBefore=profile(b).commander;command(b,"class:champion",2);}
                case 2->{if(since(b)<20)return;var s=b.squads.get(player);
                    if(profile(b).commander.equals("champion")&&s!=null&&s.commander.equals("champion"))next(b,3);
                    else if(since(b)>200){report("p2-gilded-class",false,"class:champion never reached the profile (it says "+profile(b).commander+")");next(b,99);}}
                case 3->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,4);}
                case 4->{if(since(b)<10)return;stageFixture(b);next(b,10);}
                case 10->{ // p2-gilded-steady: an Order Archer's arrow at a Risen 12 blocks off, on Hold, on Attack, and under a Retreat order stored for its group
                    var archer=post(b,dawn,"archer",0,-6,0,true);var target=post(b,dusk,"revenant",0,6,180,true);
                    dawn.order="hold";var onHold=Commanders.hit(b,archer,target.mob,"arrow");dawn.order="attack";var onAttack=Commanders.hit(b,archer,target.mob,"arrow");
                    dawn.order="hold";dawn.assignments.put("ranged",new GravesAndGlory.Assignment("retreat","loose",target.mob.getPos().add(0,0,30),0));var retreating=Commanders.hit(b,archer,target.mob,"arrow");dawn.assignments.clear();
                    boolean ok=archer.tags.contains("steady")&&is(onHold,1.2,"C18a")&&onAttack!=null&&near(onAttack.counters(),1.0)&&onAttack.rows().isEmpty()&&retreating!=null&&near(retreating.counters(),1.0)&&retreating.rows().isEmpty();
                    report("p2-gilded-steady",ok,"an Order Archer (tags "+archer.tags+") at a Risen: on Hold "+show(onHold)+"; on Attack "+show(onAttack)+"; under a Retreat order stored for its group while the army holds "+show(retreating));
                    clearFixture(b);next(b,11);
                }
                case 11->{ // p2-gilded-pierce: an Order Longbow 10 blocks in front of a Risen, a second Risen 1.5 behind it on the shot line and a third 1.5 beside it, all three bare (armour 0, so the health lost is the amount applied)
                    bow=post(b,dawn,"longbow",0,-10,0,true);first=post(b,dusk,"revenant",0,0,180,true);behind=post(b,dusk,"revenant",0,1.5,180,true);beside=post(b,dusk,"revenant",1.5,0,180,true);
                    for(var u:List.of(first,behind,beside))for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))u.mob.equipStack(slot,ItemStack.EMPTY);
                    pierceWhy="";pierceOk=true;next(b,12);
                }
                case 12->{if(since(b)<25)return; // the arrow as Abilities.attack looses it: the sheet damage times the progression
                    for(var u:List.of(first,behind,beside))if(u.mob.isAlive()){u.mob.setHealth(u.mob.getMaxHealth());u.mob.timeUntilRegen=0;u.mob.hurtTime=0;}
                    hpFirst=first.mob.getHealth();hpBehind=behind.mob.isAlive()?behind.mob.getHealth():0;hpBeside=beside.mob.getHealth();
                    mark=Combat.LAST.peekLast();Projectiles.fire(b,bow,first.mob,"arrow",(float)Abilities.baseHit(b,bow));shot=b.shots.get(b.shots.size()-1);next(b,13);
                }
                case 13->{if(b.time<shot.lands()+2)return;
                    var onFirst=hitOn(first.mob,mark);var onBehind=behind.mob.isAlive()?hitOn(behind.mob,mark):null;var onBeside=hitOn(beside.mob,mark);
                    float lostFirst=hpFirst-first.mob.getHealth(),lostBehind=behind.mob.isAlive()?hpBehind-behind.mob.getHealth():0,lostBeside=hpBeside-beside.mob.getHealth();
                    if(fixture.contains(behind)){ // run 1
                        boolean ok=onFirst!=null&&lostFirst>0&&onBehind!=null&&near(onBehind.base(),onFirst.base()*0.5)&&near(onBehind.total(),onFirst.total())&&Math.abs(lostBehind-lostFirst*0.5f)<1e-3&&onBeside==null&&lostBeside==0;
                        pierceOk&=ok;pierceWhy+="the arrow on the first Risen "+show(onFirst)+", it lost "+lostFirst+"; the Risen 1.5 behind it "+show(onBehind)+", it lost "+lostBehind+" (want half)="+ok+"; the Risen beside it lost "+lostBeside+(onBeside==null?" (no hit logged)":" ("+show(onBeside)+")")+"; ";
                        drop(b,behind);next(b,12); // run 2: the one behind is gone
                    }else{
                        boolean none=onFirst!=null&&lostFirst>0&&onBeside==null&&lostBeside==0;
                        // the same helper with behind off, as cleave calls it: the one beside is in reach
                        var markSide=Combat.LAST.peekLast();beside.mob.timeUntilRegen=0;beside.mob.hurtTime=0;float before=beside.mob.getHealth();
                        float side=Abilities.secondary(b,bow,first.mob,2.0,0.5,false,"arrow");var onSide=hitOn(beside.mob,markSide);
                        boolean cleave=side>0&&onSide!=null&&onFirst!=null&&near(onSide.base(),onFirst.base()*0.5)&&Math.abs((before-beside.mob.getHealth())-side)<1e-3;
                        pierceOk&=none&&cleave;
                        pierceWhy+="with only the Risen beside it in reach, a second arrow ("+show(onFirst)+") hit nobody else: the one beside lost "+lostBeside+"="+none+"; secondary(radius 2, share 0.5, behind off) from the same primary hits the one beside for "+side+" ("+show(onSide)+")="+cleave;
                        var row=Abilities.row(bow.spec);boolean sheet=row.mechanic().equals("pierce")&&near(row.radius(),2)&&near(row.amount(),0.5);
                        report("p2-gilded-pierce",pierceOk&&sheet,"the Order Longbow's "+row.id()+" (radius "+row.radius()+", share "+row.amount()+")="+sheet+"; "+pierceWhy);
                        clearFixture(b);next(b,14);
                    }
                }
                case 14->{ // p2-gilded-mend: a Sunweaver 6 blocks from a hurt Order Squire
                    mage=post(b,dawn,"mage",0,6,180,false);ally=post(b,dawn,"knight",0,0,0,true);ally.mob.setHealth(10f);casts.clear();heals.clear();healed.clear();castCooldownAt=-1;next(b,15);
                }
                case 15->{
                    for(var s:b.shots)if(s.shooter()==mage&&s.kind().equals("mend")&&!casts.contains(s.fired())){casts.add(s.fired());if(casts.size()==1)castCooldownAt=mage.abilityAt;}
                    float h=ally.mob.getHealth();if(h>10.01f){heals.add(b.time);healed.add(h-10f);ally.mob.setHealth(10f);}
                    if(heals.size()>=2||since(b)>300){
                        var row=Abilities.row(mage.spec);long castGap=casts.size()>=2?casts.get(1)-casts.get(0):-1,healGap=heals.size()>=2?heals.get(1)-heals.get(0):-1;
                        String next=casts.isEmpty()||castCooldownAt<0?"none":"+"+(castCooldownAt-casts.get(0));
                        boolean ok=heals.size()>=2&&Math.abs(healed.get(0)-5f)<.01f&&Math.abs(healed.get(1)-5f)<.01f&&row.cooldownTicks()==80&&castGap==80&&healGap==80&&!casts.isEmpty()&&castCooldownAt==casts.get(0)+80;
                        report("p2-gilded-mend",ok,"the Sunweaver ("+row.id()+", amount "+row.amount()+", cooldown "+row.cooldownTicks()+") healed the Squire by "+healed+" at ticks "+heals+" (gap "+healGap+", want 80), cast at "+casts+" (gap "+castGap+"), its next cast set for "+next+" (want +80)");
                        clearFixture(b);next(b,16);
                    }
                }
                case 16->{ // p2-counters-gilded: a raised-shield Order Squire facing its attackers; an Order Archer and a Risen, each with its attacker
                    Spec.TroopsRow sunderRow=synthetic("p2_sunder","Sunder Probe",false,"sunder","beast,cavalry"),boltRow=synthetic("p2_armour_bolt","Bolt Probe",true,"armour_bolt","beast,cavalry");
                    tShield=post(b,dusk,"knight",0,0,180,true);shielded.add(tShield);raise=true;
                    aPlain=post(b,dawn,"knight",0,-2.5,0,true);aSunder=post(b,dawn,sunderRow,1,-2.5,0,true);aBolt=post(b,dawn,boltRow,0,-10,0,true);aCaster=post(b,dawn,"hexer",-2,-8,0,true);
                    tArcher=post(b,dusk,"archer",8,0,180,true);aCharger=post(b,dawn,"knight",8,-2.5,0,true);
                    tRear=post(b,dusk,"revenant",-8,0,180,true);aRear=post(b,dawn,"knight",-8,2.5,180,true);
                    next(b,17);
                }
                case 17->{if(since(b)<10)return; // the shield has been up ten ticks
                    boolean raised=tShield.mob.isUsingItem();
                    var c3=Commanders.hit(b,aPlain,tShield.mob,"melee");var c4=Commanders.hit(b,aCaster,tShield.mob,"blight");var c14=Commanders.hit(b,aSunder,tShield.mob,"melee");var c15=Commanders.hit(b,aBolt,tShield.mob,"arrow");
                    dawn.order="charge";b.markCharge(dawn,"infantry");var c5=Commanders.hit(b,aCharger,tArcher.mob,"melee");var c6=Commanders.hit(b,aCharger,tArcher.mob,"melee");dawn.order="hold";
                    var c16=Commanders.blow(b,aRear,tRear.mob,"melee");var c16area=Commanders.hit(b,aRear,tRear.mob,"melee");
                    boolean tags=aSunder.tags.containsAll(List.of("sunder","beast","cavalry"))&&!aSunder.tags.contains("shield")&&aBolt.tags.containsAll(List.of("armour_bolt","beast","cavalry"));
                    boolean ok=raised&&tags&&is(c3,0.6,"C3")&&is(c4,1.0,"C4")&&is(c14,1.4,"C14")&&is(c15,0.7,"C15")&&is(c5,1.5,"C5")&&is(c6,1.2,"C6")&&is(c16,1.3,"C16")&&c16area!=null&&near(c16area.counters(),1.0)&&c16area.rows().isEmpty();
                    report("p2-counters-gilded",ok,"against the raised shield (raised="+raised+"), from the front: an Order Squire's blow "+show(c3)+"; a Gravecaller's blight "+show(c4)+"; the synthetic husk with sunder (tags "+aSunder.tags+") "+show(c14)+"; the synthetic husk with armour_bolt (tags "+aBolt.tags+"), an arrow "+show(c15)+"; an Order Squire's first blow on an Order Archer after the charge order "+show(c5)+", then "+show(c6)+"; its blow on a Risen from behind "+show(c16)+" (want 1.3 C16) and as a cleave's second target there "+show(c16area)+" (area: want 1.0); the synthetic rows' tags carry their ability and beast, cavalry="+tags);
                    clearFixture(b);next(b,20);
                }
                case 20->{ // p2-bulwark, tier III: the commander's own army, 14 Gilded slots: four Order Squires (its infantry, facing east), an Order Longbow 10 blocks from the first Squire, nine Order Archers 18 or more away; a frozen Bone Archer of Dusk in front of the Squires, a Risen and a Bone Archer of Dusk behind them
                    reset(own);own.banner=banner("knight*4,longbow,archer*9");
                    knights.clear();for(int i=0;i<4;i++)knights.add(post(b,own,"knight",-8+(i%2)*1.5,-0.75+(i/2)*1.5,-90,true));
                    guard=post(b,own,"longbow",2,0,-90,true);for(int i=0;i<9;i++)post(b,own,"archer",11,-8+i*2,-90,true);
                    foe=post(b,dusk,"bonebow",-2,4,90,true);foe2=post(b,dusk,"revenant",-11,-0.75,-90,true);aRear=post(b,dusk,"bonebow",-12,0.75,-90,true);Houses.recompute(own);next(b,21);
                }
                case 21->{if(since(b)<5)return;
                    var k=knights.get(0);var arrow=Commanders.hit(b,foe,k.mob,"arrow");var bolt=Commanders.hit(b,foe,k.mob,"bolt");var blight=Commanders.hit(b,foe,k.mob,"blight");var melee=Commanders.hit(b,foe,k.mob,"melee");var bare=Commanders.hit(b,foe,guard.mob,"arrow");
                    boolean share=traits(arrow,0.55)&&traits(bolt,0.55)&&traits(blight,0.55)&&traits(melee,1.0)&&traits(bare,1.0);
                    int tier=own.traits.getOrDefault("gilded",0),bannerTier=Houses.bannerTiers(own).getOrDefault("gilded",0),slots=RosterMath.houseSlots(own.banner,RosterMath.Catalog.fromSpec()).getOrDefault("gilded",0);
                    bulwarkOk=share&&tier==3&&bannerTier==3&&slots==14;
                    bulwarkWhy="his banner holds "+slots+" Gilded slots (tier "+bannerTier+"), the living army is at tier "+tier+" "+own.traits+"; an Order Squire (shield) takes traits x"+tr(arrow)+" from an arrow, x"+tr(bolt)+" from a bolt, x"+tr(blight)+" from blight and x"+tr(melee)+" from melee, the Order Longbow (no shield) x"+tr(bare)+" from an arrow="+share;
                    // covered flanks at tier III (radius 12): the Longbow 10 blocks from the Squire covers him, so blows from behind take neither C16 nor C16s (the traits share still applies)
                    double d=distance(guard.mob,k.mob);boolean covered=Houses.covered(b,k);
                    var mB=Commanders.blow(b,foe2,k.mob,"melee");var aB=Commanders.hit(b,aRear,k.mob,"arrow");
                    double bearing=BattleMath.relativeBearing(k.mob.getYaw(),foe2.mob.getX()-k.mob.getX(),foe2.mob.getZ()-k.mob.getZ());
                    boolean coveredIII=covered&&d<=12&&mB!=null&&near(mB.counters(),1.0)&&mB.rows().isEmpty()&&traits(mB,1.0)&&aB!=null&&near(aB.counters(),1.0)&&aB.rows().isEmpty()&&traits(aB,0.55);
                    bulwarkOk&=coveredIII;
                    bulwarkWhy+="; at tier III (radius 12) with the Longbow "+fmt(d)+" off the Squire is covered="+covered+": a Risen's blow from behind (bearing "+fmt(bearing)+") "+show(mB)+" and a Bone Archer's arrow from behind "+show(aB)+" (want counters 1.0 and no row for both; traits 1.0 and 0.55)="+coveredIII;
                    // tier I: eight Order Archers leave (six Gilded slots stand); the Longbow stands 8 blocks from the Squire, outside the tier's radius of 5: the flank rows return; a Squire of his own group 1.5 off never covers him
                    int gone=0;for(var u:new ArrayList<>(fixture))if(u.spec.id().equals("archer")&&gone<8){drop(b,u);gone++;}
                    Houses.recompute(own);move(guard,k.mob.getX()-site.getX()-.5+8,k.mob.getZ()-site.getZ()-.5,-90);
                    tier=own.traits.getOrDefault("gilded",0);double d8=distance(guard.mob,k.mob);boolean open=!Houses.covered(b,k)&&!Houses.covered(b,knights.get(1));
                    arrowI=Commanders.hit(b,aRear,k.mob,"arrow");var meleeI=Commanders.blow(b,foe2,k.mob,"melee");
                    boolean openI=tier==1&&open&&Math.abs(d8-8)<0.3&&is(arrowI,1.15,"C16s")&&traits(arrowI,0.85)&&is(meleeI,1.3,"C16")&&traits(meleeI,1.0);
                    bulwarkOk&=openI;
                    bulwarkWhy+="; at tier I "+own.traits+" (radius 5) with the Longbow "+fmt(d8)+" off the Squire is covered="+!open+" (a Squire of his own group 1.5 off does not cover him): the arrow from behind "+show(arrowI)+" (want 1.15 C16s, traits 0.85) and the Risen's blow "+show(meleeI)+" (want 1.3 C16)="+openI;
                    // the Longbow steps to 4 blocks: covered again inside the tier I radius
                    move(guard,k.mob.getX()-site.getX()-.5+4,k.mob.getZ()-site.getZ()-.5,-90);
                    double d4=distance(guard.mob,k.mob);boolean coveredI=Houses.covered(b,k);var arrowC=Commanders.hit(b,aRear,k.mob,"arrow");var meleeC=Commanders.blow(b,foe2,k.mob,"melee");
                    boolean backI=coveredI&&Math.abs(d4-4)<0.3&&arrowC!=null&&near(arrowC.counters(),1.0)&&arrowC.rows().isEmpty()&&traits(arrowC,0.85)&&meleeC!=null&&near(meleeC.counters(),1.0)&&meleeC.rows().isEmpty();
                    bulwarkOk&=backI;
                    bulwarkWhy+="; with the Longbow "+fmt(d4)+" off covered="+coveredI+": the arrow "+show(arrowC)+" and the blow "+show(meleeC)+" (want 1.0, no row)="+backI;
                    report("p2-bulwark",bulwarkOk,bulwarkWhy);
                    clearFixture(b);reset(own);next(b,30);
                }
                case 30->{ // p2-gilded-oath: Lord Marshal Aldric (attacks off, ability on), an Order Squire 3.2 blocks from where his banner will stand and one 15.6 away, a Sunweaver 10 away, frozen Risen of Dusk beside the near Squire and the far one; the near Squire carries a Slowness already
                    Abilities.clearBanners(b);reset(own);
                    marshal=post(b,own,"gl_marshal",0,0,0,true);marshal.abilityAt=0;marshal.seenOrder=null;
                    near=post(b,own,"knight",3,1,0,true);far=post(b,own,"knight",-11,-11,0,true);support=post(b,own,"mage",-8,6,0,true);
                    foe=post(b,dusk,"revenant",3,3,180,true);foe2=post(b,dusk,"revenant",-11,-9,180,true);
                    near.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS,600,1));
                    order(b,"infantry","move","block",site.getX()+.5,site.getZ()+.5,0);
                    seen.clear();planted=null;plantAt=-1;secondAt=-1;fell=false;blockBack=false;beatCleared=false;unanchored=false;slowAfter=false;oathOk=true;oathWhy="";
                    next(b,31);
                }
                case 31->{ // his first beat saw Move: no banner stands and the near Squire keeps his Slowness; then the infantry is ordered to Hold
                    if(since(b)<=30)return;
                    boolean none=b.banners.isEmpty(),slowed=near.mob.hasStatusEffect(StatusEffects.SLOWNESS);oathOk&=none&&slowed&&"move".equals(marshal.seenOrder);
                    oathWhy+="before the Hold order no banner stood="+none+" (Aldric's beats saw "+marshal.seenOrder+") and the near Squire kept his Slowness="+slowed+"; ";
                    heardBefore=BattleEffects.heard.size();order(b,"infantry","hold","block",site.getX()+.5,site.getZ()+.5,0);next(b,32);
                }
                case 32->{ // the banner, its aura and its anchoring
                    if(b.banners.isEmpty()){if(since(b)>20){oathWhy+="no banner within 20 ticks of the Hold order";report("p2-gilded-oath",false,oathWhy);clearFixture(b);next(b,50);}return;}
                    planted=b.banners.get(0);seen.add(planted);var row=Abilities.row(marshal.spec);plantAt=planted.until()-row.durationTicks();
                    var state=planted.block()==null?null:b.world.getBlockState(planted.block());boolean block=state!=null&&state.getBlock() instanceof net.minecraft.block.BannerBlock;
                    double fromHim=distance(planted.at(),marshal.mob);boolean quiet=BattleEffects.heard.size()==heardBefore;
                    boolean shape=row.id().equals("oath_banner")&&row.anchors()&&planted.anchors()&&planted.team()==own.team&&near(planted.radius(),12)&&near(planted.amount(),0.1)&&row.durationTicks()==300&&marshal.abilityAt==plantAt+row.cooldownTicks()&&row.cooldownTicks()==600&&fromHim<=2&&b.time-plantAt<=1&&since(b)<=11&&quiet;
                    var inside=Commanders.hit(b,near,foe.mob,"melee");var himself=Commanders.hit(b,marshal,foe.mob,"melee");var beyond=Commanders.hit(b,far,foe2.mob,"melee");var theirs=Commanders.hit(b,foe,near.mob,"melee");
                    boolean aura=auras(inside,1.10)&&auras(himself,1.10)&&auras(beyond,1.0)&&auras(theirs,1.0);
                    oathOk&=shape&&block&&aura;
                    oathWhy+="the Hold order planted a banner "+since(b)+" ticks later, "+fmt(fromHim)+" from Aldric (block "+(state==null?"none":state.getBlock().getName().getString())+"="+block+"), team "+planted.team()+", radius "+planted.radius()+", +"+planted.amount()+", anchors="+planted.anchors()+", standing "+(planted.until()-plantAt)+" ticks, his next plant at +"+(marshal.abilityAt-plantAt)+", no feedback sound="+quiet+"="+shape
                        +"; auras on a blow by the Squire "+fmt(distance(planted.at(),near.mob))+" off "+au(inside)+", by Aldric "+au(himself)+", by the Squire "+fmt(distance(planted.at(),far.mob))+" off "+au(beyond)+", by a Dusk Risen inside "+au(theirs)+"="+aura;
                    // the anchoring: the plant took the Slowness off the near Squire; inside the radius no slow, pull, knock or blow moves him; the Squire 15.6 off takes them all
                    boolean cleared=!near.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                    boolean flags=Abilities.anchored(b,near)&&Abilities.anchored(b,marshal)&&Abilities.anchored(b,support)&&!Abilities.anchored(b,far);
                    Abilities.slow(b,near.mob,1,100);boolean noSlow=!near.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                    Vec3d atN=near.mob.getPos();Abilities.pull(b,near.mob,foe.mob.getPos(),3);boolean noPull=near.mob.getPos().distanceTo(atN)<1e-6;
                    Abilities.knock(b,near.mob,foe.mob.getPos(),foe.mob.getPos(),1.0);Vec3d vK=near.mob.getVelocity();boolean noKnock=Math.hypot(vK.x,vK.z)<1e-6;
                    near.mob.timeUntilRegen=0;near.mob.hurtTime=0;Commanders.hit(b,foe,near.mob,"melee");Vec3d vB=near.mob.getVelocity();boolean noBlow=Math.hypot(vB.x,vB.z)<1e-6;
                    Abilities.slow(b,far.mob,1,100);boolean farSlow=far.mob.hasStatusEffect(StatusEffects.SLOWNESS);far.mob.removeStatusEffect(StatusEffects.SLOWNESS);
                    Vec3d atF=far.mob.getPos();Abilities.pull(b,far.mob,foe2.mob.getPos(),3);double farPulled=atF.distanceTo(far.mob.getPos());
                    far.mob.timeUntilRegen=0;far.mob.hurtTime=0;Commanders.hit(b,foe2,far.mob,"melee");Vec3d vF=far.mob.getVelocity();boolean farBlown=Math.hypot(vF.x,vF.z)>1e-3;
                    boolean anchoring=cleared&&flags&&noSlow&&noPull&&noKnock&&noBlow&&farSlow&&farPulled>0.3&&farBlown;
                    oathOk&=anchoring;
                    oathWhy+="; the plant took the near Squire's Slowness off="+cleared+"; anchored: him "+Abilities.anchored(b,near)+", Aldric "+Abilities.anchored(b,marshal)+", the Sunweaver 10 off "+Abilities.anchored(b,support)+", the Squire 15.6 off "+Abilities.anchored(b,far)+" (want true, true, true, false); inside: a slow sticks="+!noSlow+", a 3-block pull moved him "+fmt(near.mob.getPos().distanceTo(atN))+", a knock gave him "+fmt(Math.hypot(vK.x,vK.z))+" of velocity, a Risen's blow "+fmt(Math.hypot(vB.x,vB.z))+" (want none of them); outside: a slow sticks="+farSlow+", the pull moved him "+fmt(farPulled)+" and the blow knocked him="+farBlown+"="+anchoring;
                    near.stunnedUntil=b.time+200;near.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.WEAKNESS,200,0));near.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.WITHER,200,0)); // with the Slowness: a stun (Unit.stunnedUntil), Weakness and Wither, read after the beat
                    near.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS,600,1));slowPutAt=b.time;next(b,33);
                }
                case 33->{ // the beat: a Slowness put on an anchored troop after the plant goes within BEAT ticks
                    if(near.mob.hasStatusEffect(StatusEffects.SLOWNESS)){if(b.time-slowPutAt<=Abilities.BEAT+1)return;beatCleared=false;}else beatCleared=true;
                    oathOk&=beatCleared;oathWhy+="; a Slowness put on the near Squire after the plant was off "+(b.time-slowPutAt)+" ticks later (want within the banner's "+Abilities.BEAT+"-tick beat)="+beatCleared;
                    boolean stunned=near.stunnedUntil>b.time,weak=near.mob.hasStatusEffect(StatusEffects.WEAKNESS),bleeding=near.mob.hasStatusEffect(StatusEffects.WITHER),landed=stunned&&weak&&bleeding;
                    near.stunnedUntil=0;near.mob.removeStatusEffect(StatusEffects.WEAKNESS);near.mob.removeStatusEffect(StatusEffects.WITHER);near.mob.setHealth(near.mob.getMaxHealth());
                    oathOk&=landed;oathWhy+="; put on him with that Slowness and read after the beat that swept it: a stun kept="+stunned+", Weakness kept="+weak+", Wither kept="+bleeding+" (a stun, weaken and bleed still land on an anchored troop; weaken and bleed have no mechanic yet, so the status effects they will ride on stand in)="+landed;
                    next(b,34);
                }
                case 34->{ // the cooldown: Move then Hold again at +100 plant nothing; the banner falls at +300 and the anchoring with it; Move and Hold after +600 plant again
                    for(var banner:b.banners)seen.add(banner);
                    long t=b.time-plantAt;
                    if(t==100||t==605)order(b,"infantry","move","block",site.getX()+.5,site.getZ()+.5,0);
                    if(t==115||t==620)order(b,"infantry","hold","block",site.getX()+.5,site.getZ()+.5,0);
                    if(t==301){fell=b.banners.isEmpty();var state=planted.block()==null?null:b.world.getBlockState(planted.block());blockBack=state!=null&&state.isAir();
                        unanchored=!Abilities.anchored(b,near);Abilities.slow(b,near.mob,1,40);slowAfter=near.mob.hasStatusEffect(StatusEffects.SLOWNESS);near.mob.removeStatusEffect(StatusEffects.SLOWNESS);}
                    if(t>=620&&secondAt<0&&b.banners.size()==1&&b.banners.get(0)!=planted)secondAt=t;
                    if(secondAt>=0||t>660){
                        long early=seen.stream().filter(x->x!=planted&&x.until()-300<plantAt+600).count();
                        boolean ok=early==0&&fell&&blockBack&&unanchored&&slowAfter&&secondAt>=620&&secondAt<=632;
                        oathOk&=ok;
                        oathWhy+="; Move then Hold at +100 and +115 planted "+early+" more banners before +600; at +301 the banner had fallen="+fell+", its block was air again="+blockBack+", the near Squire was anchored no more="+unanchored+" and a slow stuck to him="+slowAfter+"; Move and Hold at +605 and +620 planted the next banner at +"+secondAt+"="+ok;
                        report("p2-gilded-oath",oathOk,oathWhy);
                        clearFixture(b);Abilities.clearBanners(b);reset(own);next(b,35);
                    }
                }
                case 35->{ // p2-gilded-oath-plant and p2-gilded-oath-squad: a fresh banner planted by a direct call, so nothing but the plant itself has run (the Battle tick, which holds the 10-tick beat, has not)
                    Abilities.clearBanners(b);reset(own);
                    marshal=post(b,own,"gl_marshal",0,0,0,true);marshal.abilityAt=0;marshal.seenOrder=null;
                    var ownKnight=post(b,own,"knight",3,1,0,true);var ownArcher=post(b,own,"archer",4,3,0,true);var ownFar=post(b,own,"mage",-14,0,0,true);
                    var allyKnight=post(b,dawn,"knight",2,-2,0,true);var allyArcher=post(b,dawn,"archer",-4,4,0,true);var foeR=post(b,dusk,"revenant",3,3,180,true);
                    for(var u:List.of(ownArcher,ownFar,allyKnight))u.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS,600,0));
                    b.squads.put(dawn.owner,dawn); // the allied army (same team, another squad) is a seated army of the round for this one call, as a team-wide rule would have found it
                    try{
                        var row=Abilities.row(marshal.spec);Abilities.plant(b,marshal,row,b.time);
                        var banner=b.banners.isEmpty()?null:b.banners.get(0);
                        boolean stands=b.banners.size()==1&&banner.squad()==own&&banner.team()==own.team&&banner.anchors();
                        double dArcher=banner==null?-1:distance(banner.at(),ownArcher.mob),dFar=banner==null?-1:distance(banner.at(),ownFar.mob),dAllyK=banner==null?-1:distance(banner.at(),allyKnight.mob),dAllyA=banner==null?-1:distance(banner.at(),allyArcher.mob);
                        boolean placed=stands&&dArcher<=12&&dFar>12&&dAllyK<=12&&dAllyA<=12;
                        boolean freed=!ownArcher.mob.hasStatusEffect(StatusEffects.SLOWNESS),farKept=ownFar.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                        boolean flags=Abilities.anchored(b,ownArcher)&&!Abilities.anchored(b,ownFar);
                        report("p2-gilded-oath-plant",placed&&freed&&farKept&&flags,"on the plant tick, before any beat: one banner of his own army stands and anchors="+stands+"; the slowed Order Archer "+fmt(dArcher)+" from it (radius 12) was freed of his Slowness="+freed+" and is anchored="+Abilities.anchored(b,ownArcher)+"; the slowed Sunweaver "+fmt(dFar)+" from it keeps it="+farKept+" and is anchored="+Abilities.anchored(b,ownFar)+" (want false)");
                        Abilities.anchor(b,banner); // the beat's sweep, run by hand: it must not reach the allied army either
                        var mine=Commanders.hit(b,ownKnight,foeR.mob,"melee");var ally=Commanders.hit(b,allyKnight,foeR.mob,"melee");
                        boolean aura=auras(mine,1.10)&&auras(ally,1.0);
                        boolean anchors=Abilities.anchored(b,ownKnight)&&!Abilities.anchored(b,allyKnight)&&!Abilities.anchored(b,allyArcher)&&allyKnight.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                        report("p2-gilded-oath-squad",own.team==dawn.team&&placed&&aura&&anchors,"an allied army of the same team (team "+dawn.team+" and his "+own.team+") with a Squire "+fmt(dAllyK)+" and an Order Archer "+fmt(dAllyA)+" from the banner (inside 12): auras his Squire "+au(mine)+" (want 1.1), the ally's Squire "+au(ally)+" (want 1.0)="+aura
                            +"; anchored: his Squire "+Abilities.anchored(b,ownKnight)+", the ally's Squire "+Abilities.anchored(b,allyKnight)+" and archer "+Abilities.anchored(b,allyArcher)+" (want true, false, false), and the ally's slowed Squire kept his Slowness through the plant and the sweep="+allyKnight.mob.hasStatusEffect(StatusEffects.SLOWNESS)+"="+anchors);
                    }finally{b.squads.remove(dawn.owner);}
                    clearFixture(b);Abilities.clearBanners(b);reset(own);next(b,50);
                }
                case 50->{ // p2-champion-standfast: the Champion on the platform; four Risen of his own army (Graveborn, so no Bulwark) 6.8 blocks from him, a Bone Archer of his 14.1 away, two frozen Risen of Dusk
                    var p=sp(b);if(p==null)return;reset(own);own.banner=banner("revenant*4,bonebow");
                    p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);b.relocate(p,site.getX()+.5,site.getY(),site.getZ()+.5,-90,20);
                    knights.clear();for(int i=0;i<4;i++)knights.add(post(b,own,"revenant",6+(i%2)*1.5,-0.75+(i/2)*1.5,-90,true));
                    outside=post(b,own,"bonebow",-10,-10,-90,true);foe=post(b,dusk,"revenant",9,5,90,true);foe2=post(b,dusk,"revenant",-12,-10,90,true);
                    for(String g:Morale.GROUPS)own.fullHealth.put(g,living(b,own,g));Houses.recompute(own);
                    next(b,51);
                }
                case 51->{if(since(b)<5)return;var p=sp(b);var c=Commander.row(own);
                    firstActive="";readyFirst="";readyLater="";activeCooldownSeconds=-1;
                    t0=b.time;b.command(p,"ability");
                    var res=p.getStatusEffect(StatusEffects.RESISTANCE);
                    boolean started=c.id().equals("champion")&&Commander.running(b,own,"stand_fast")&&own.abilityUntil==t0+c.durationTicks()&&own.abilityReadyAt==t0+c.cooldownTicks()&&Commander.readyIn(b,own)==1800&&c.cooldownTicks()==1800&&c.durationTicks()==160&&res!=null&&res.getAmplifier()==0&&res.getDuration()>=c.durationTicks()-1;
                    var inside=Commanders.hit(b,foe,knights.get(0).mob,"melee");var beyond=Commanders.hit(b,foe2,outside.mob,"melee");
                    boolean taken=inside!=null&&near(inside.commander(),1-c.value1())&&beyond!=null&&near(beyond.commander(),1);
                    activeOk=started&&taken;
                    activeWhy="the ability command as "+own.commander+": stand_fast runs="+Commander.running(b,own,"stand_fast")+" to +"+(own.abilityUntil-t0)+" (want 160), ready again at +"+(own.abilityReadyAt-t0)+" (readyIn "+Commander.readyIn(b,own)+", want 1800, 90 s), Resistance "+(res==null?"none":"level "+(res.getAmplifier()+1)+" for "+res.getDuration())+"="+started
                        +"; a Dusk blow on his Risen "+fmt(distance(p,knights.get(0).mob))+" from him "+show(inside)+", on his Bone Archer "+fmt(distance(p,outside.mob))+" away "+show(beyond)+"="+taken;
                    tileWatch=1;next(b,52);
                }
                case 52->{ // it ends with the active (nothing holds anyone: no group routs); the tile counts down
                    long t=b.time-t0;var c=Commander.row(own);
                    if(t==c.durationTicks()+1){var p=sp(b);boolean ended=own.abilityUntil==0&&!Commander.running(b,own)&&p!=null&&!p.hasStatusEffect(StatusEffects.RESISTANCE);activeOk&=ended;activeWhy+="; at +"+t+" it has ended and his Resistance is gone="+ended;}
                    if(t>c.durationTicks()+1&&!readyLater.isEmpty()||t>c.durationTicks()+400){
                        tileWatch=0;
                        int active=seconds(firstActive),cool=activeCooldownSeconds,ready1=seconds(readyFirst),ready2=seconds(readyLater);
                        boolean tile=active==8&&cool==90&&ready1>=80&&ready1<=82&&ready2>=0&&ready2<ready1;
                        report("p2-champion-standfast",activeOk&&tile,activeWhy+"; the HUD tile first read \""+firstActive+"\" with the cooldown at "+cool+" s (want ACTIVE 8 s and 90 s), then \""+readyFirst+"\" and 2 s later \""+readyLater+"\" (counting down)="+tile);
                        clearFixture(b);reset(own);next(b,60);
                    }
                }
                case 60->{tileWatch=0;if(b.running){b.campaign.returnLobby();return;}command(b,"class:marshal",61);}
                case 61->{if(since(b)<20)return;var s=b.squads.get(player);
                    if(profile(b).commander.equals("marshal")&&s!=null&&s.commander.equals("marshal"))next(b,62);
                    else if(since(b)>200){report("p2-marshal-muster",false,"class:marshal never reached the profile (it says "+profile(b).commander+")");next(b,70);}}
                case 62->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,63);}
                case 63->{if(since(b)<10)return;stageFixture(b);
                    // he stands 9 blocks west of the platform's centre facing west, so the Move slots behind him (east) stay on the platform
                    var p=sp(b);p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);b.relocate(p,site.getX()-8.5,site.getY(),site.getZ()+.5,90,20);
                    // three Order Squires, two Order Archers and a Sunweaver of his own army, free to walk, standing 17 to 19 blocks east of him on the
                    // army's Hold with no group order (Battle.formation keeps each where it was deployed)
                    for(int i=0;i<3;i++)free(b,"knight",8,-1.5+i*1.5);free(b,"archer",10,3);free(b,"archer",10,4.5);free(b,"mage",10,-4);
                    for(String g:Morale.GROUPS)own.fullHealth.put(g,living(b,own,g));
                    next(b,64);
                }
                case 64->{if(since(b)<30)return; // settled on Hold; he musters
                    var p=sp(b);var c=Commander.row(own);
                    heardBefore=BattleEffects.heard.size();startDistance=meanDistance(p);firstActive="";readyFirst="";readyLater="";activeCooldownSeconds=-1;
                    var listener=Phase1Checks.Listener.join(b,p.getX()+5,p.getY(),p.getZ());rallyBefore=Phase1Checks.heardOnClient("gravesandglory:horn_rally");
                    t0=b.time;b.command(p,"ability");listenerCues=listener.cues();listener.leave(b);
                    // the order path's spot: where he stands, one block above the ground there (he stands on the platform at y 140, far above it)
                    Vec3d to=p.getPos();Vec3d spot=new Vec3d(to.x,Arena.ground((int)to.x,(int)to.z)+1,to.z);boolean moves=true,snapped=Math.abs(to.y-spot.y)>20;StringBuilder orders=new StringBuilder();
                    for(var gr:Spec.GROUPS){if(gr.id().equals("all"))continue;String g=gr.id();var a=own.assignments.get(g);boolean m=a!=null&&a.order.equals("move")&&a.target.squaredDistanceTo(spot)<1e-6&&a.issued==t0;moves&=m;orders.append(g).append(' ').append(a==null?"none":a.order+" to "+fmt(a.target.x)+", "+fmt(a.target.y)+", "+fmt(a.target.z)).append(m?"":" (BAD)").append("; ");}
                    moves&=snapped;
                    double want=BattleMath.speedModifier(1+c.value1(),false);int speedy=0,troops=0;StringBuilder speeds=new StringBuilder();
                    for(var u:fixture)if(u.mob.isAlive()){troops++;var a=u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);var m=a==null?null:a.getModifier(Commander.RALLY);
                        boolean good=m!=null&&Math.abs(m.getValue()-want)<1e-9&&m.getOperation()==net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL&&Math.abs(a.getValue()/a.getBaseValue()-Math.sqrt(1+c.value1()))<1e-6;if(good)speedy++;
                        speeds.append(u.spec.id()).append(' ').append(a==null?"-":fmt(a.getBaseValue())+" -> "+String.format(Locale.ROOT,"%.4f",a.getValue())).append(good?"":" (BAD)").append(", ");}
                    int heard=BattleEffects.heard.size()-heardBefore;var last=BattleEffects.heard.isEmpty()?null:BattleEffects.heard.get(BattleEffects.heard.size()-1);
                    boolean sound=heard==1&&last!=null&&last.player().equals(player)&&last.kind().equals("rally")&&listenerCues.isEmpty();
                    boolean started=c.id().equals("marshal")&&Commander.running(b,own,"rally_horn")&&own.abilityUntil==t0+c.durationTicks()&&Commander.readyIn(b,own)==2400&&c.cooldownTicks()==2400&&c.durationTicks()==300&&near(c.value1(),0.2)&&near(c.value2(),0)&&Commander.activeName(c.active()).equals("Muster");
                    activeOk=started&&moves&&speedy==troops&&troops==6&&sound;
                    activeWhy="the ability command as "+own.commander+": "+c.active()+" (named "+Commander.activeName(c.active())+", want Muster; value1 "+c.value1()+", value2 "+c.value2()+") runs="+Commander.running(b,own,"rally_horn")+" to +"+(own.abilityUntil-t0)+" (want 300), readyIn "+Commander.readyIn(b,own)+" (want 2400, 120 s)="+started+"; orders for every group (the ground at his feet, y "+fmt(spot.y)+", not his own y "+fmt(to.y)+"="+snapped+"): "+orders+"="+moves
                        +"; speed: "+speeds+speedy+" of "+troops+" troops carry the modifier "+String.format(Locale.ROOT,"%.4f",want)+" (a real x"+(1+c.value1())+" on land: the square root, BattleMath.speedModifier); the Marshal alone heard the rally cue ("+heard+" sound(s), the last of kind "+(last==null?"none":last.kind())+") and a second player 5 blocks from him was sent "+listenerCues+" (want nothing)="+sound;
                    tileWatch=1;late=null;lateHastened=false;next(b,65);
                }
                case 65->{long t=b.time-t0;var c=Commander.row(own);
                    if(t==40){long rally=Phase1Checks.heardOnClient("gravesandglory:horn_rally")-rallyBefore;boolean played=rally==1;activeOk&=played;activeWhy+="; his own client played horn_rally "+rally+" time(s) by +40 (want 1)="+played;}
                    if(t==50)late=free(b,"knight",8,6);
                    if(t==52&&late!=null){var a=late.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);lateHastened=a!=null&&a.getModifier(Commander.RALLY)!=null;}
                    if(t==100){var p=sp(b);double d=meanDistance(p);boolean closer=startDistance-d>=5;activeOk&=closer;activeWhy+="; his troops' mean distance to him "+fmt(startDistance)+" -> "+fmt(d)+" after 100 ticks="+closer;}
                    if(t==c.durationTicks()+1){int left=0;for(var u:fixture){var a=u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);if(a!=null&&a.getModifier(Commander.RALLY)!=null)left++;}
                        boolean ended=own.abilityUntil==0&&!Commander.running(b,own)&&left==0;activeOk&=ended&&lateHastened;activeWhy+="; a Squire that joined at +50 had the speed by +52="+lateHastened+"; at +"+t+" it has ended and "+left+" troops keep the speed="+ended;}
                    if(t>c.durationTicks()+1&&!readyLater.isEmpty()||t>c.durationTicks()+400){
                        tileWatch=0;int active=seconds(firstActive),cool=activeCooldownSeconds,ready1=seconds(readyFirst),ready2=seconds(readyLater);
                        boolean tile=active==15&&cool==120&&ready1>=103&&ready1<=105&&ready2>=0&&ready2<ready1;
                        report("p2-marshal-muster",activeOk&&tile,activeWhy+"; the HUD tile first read \""+firstActive+"\" with the cooldown at "+cool+" s (want ACTIVE 15 s and 120 s), then \""+readyFirst+"\" and 2 s later \""+readyLater+"\" (counting down)="+tile);
                        reset(own);next(b,66);
                    }
                }
                case 66->{ // p2-marshal-muster-spots: Muster's Move takes the order path's spot rules: while he stands in river water or inside the arena's 10-block edge margin no group is moved, and the rest of the Muster still happens; the second Muster inside the cue's 8 s plays no second sound
                    var p=sp(b);
                    Vec3d river=null;for(int z=20;z<200&&river==null;z++){int x=(int)Math.round(CampaignMaps.riverX(z));if(Arena.river(x,z)&&Arena.ground(x,z)<64)river=new Vec3d(x+.5,Arena.ground(x,z)+1,z+.5);}
                    Vec3d[] where={river,new Vec3d(.5,100,GravesAndGlory.R.arenaHalfZ()-5)};String[] name={"in river water","inside the edge margin"},words={"Choose dry ground or a crossing.","Too near the edge of the arena."};
                    boolean ok=river!=null&&!GravesAndGlory.Battle.inArena(where[1].x,where[1].z)&&GravesAndGlory.Battle.wet(where[0]==null?0:where[0].x,where[0]==null?0:where[0].z);StringBuilder why=new StringBuilder();
                    for(int i=0;i<2&&where[i]!=null;i++){
                        Map<String,GravesAndGlory.Assignment> before=new HashMap<>(own.assignments);
                        b.relocate(p,where[i].x,where[i].y,where[i].z,0,0);
                        Commander.reset(b,own);int heardAt=BattleEffects.heard.size();
                        b.campaign.notices.remove(player);b.command(p,"ability");String said=b.campaign.notices.getOrDefault(player,"");
                        boolean unmoved=true;for(var gr:Spec.GROUPS)if(!gr.id().equals("all"))unmoved&=own.assignments.get(gr.id())==before.get(gr.id());
                        boolean ran=Commander.running(b,own,"rally_horn");
                        int speedy=0,troops=0;for(var u:fixture)if(u.mob.isAlive()){troops++;var a=u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);if(a!=null&&a.getModifier(Commander.RALLY)!=null)speedy++;}
                        int added=BattleEffects.heard.size()-heardAt;boolean cue=i==0?added==1:added==0;
                        boolean spoke=said.equals(words[i]);ok&=unmoved&&ran&&speedy==troops&&troops>0&&spoke&&cue;
                        why.append("he stands ").append(name[i]).append(" at ").append(fmt(where[i].x)).append(", ").append(fmt(where[i].z)).append(": no group got a new order=").append(unmoved).append("; the Muster still ran=").append(ran).append("; the speed reached ").append(speedy).append(" of ").append(troops).append(" troops; the notice read \"").append(said).append("\" (want \"").append(words[i]).append("\")=").append(spoke).append("; feedback sounds +").append(added).append(i==0?" (want 1)":" (want 0: inside the 8 s of the first)").append("=").append(cue).append("; ");
                    }
                    Commander.reset(b,own);b.relocate(p,site.getX()-8.5,site.getY(),site.getZ()+.5,90,20);
                    report("p2-marshal-muster-spots",ok,"(river spot "+(river==null?"none found":fmt(river.x)+", "+fmt(river.z)+", ground "+Arena.ground((int)river.x,(int)river.z))+") "+why);
                    clearFixture(b);reset(own);next(b,70);
                }
                case 70->{ // p2-gilded-showcase: his army under the hard Gilded banner on map 0
                    tileWatch=0;var p=sp(b);if(p==null)return;if(site!=null){platform(b,site,12,Blocks.AIR.getDefaultState());unforce(b);site=null;}
                    if(!skirmishBanners(b,p,0,"gilded_hard",null))return;next(b,71);
                }
                case 71->{if(since(b)<10)return; // the stage, first the camera: the client takes the valley and lets go of his host at its base before the host comes
                    // (a tracked troop teleported far in the tick the camera leaves stays frozen where the client last drew it, since its old chunk stops ticking there)
                    Projectiles.clear(b);Abilities.clearBanners(b);
                    for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;s.training.clear();if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
                    own=b.squads.get(player);for(var u:new ArrayList<>(b.units))if(u.squad!=own){u.mob.discard();b.units.remove(u);}
                    audit(b,"showcase",false);reset(own);
                    own.assignments.put("infantry",new GravesAndGlory.Assignment("move","shieldwall",ground(b,SX,SZ),-90));
                    own.assignments.put("ranged",new GravesAndGlory.Assignment("hold","block",ground(b,SX,SZ),-90)); // Battle.formation sets ranged 5 and support 9 behind the line
                    own.assignments.put("support",new GravesAndGlory.Assignment("hold","block",ground(b,SX,SZ),-90));
                    var p=sp(b);p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    double cx=SX-13,cz=SZ-12,tx=SX+5,tz=SZ+1;b.relocate(p,cx,Phase1Checks.top(b,cx,cz)+6,cz,(float)Math.toDegrees(Math.atan2(-(tx-cx),tz-cz)),16);
                    next(b,72);
                }
                case 72->{if(since(b)<40)return; // then the host: in line facing east on the valley floor, and twelve Bone Archers of an ad hoc Dusk army 22 blocks east of the shield line
                    for(var u:b.units)if(u.squad==own){Vec3d at=b.formation(u);place(b,u,at.x,at.z,-90);}
                    for(String g:Morale.GROUPS)own.fullHealth.put(g,Math.max(1,living(b,own,g)));
                    dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,ground(b,SX+22,SZ));dusk.order="attack";dusk.formation="loose";dusk.target=ground(b,SX+22,SZ);dusk.yaw=90;dusk.bot=true;
                    for(int i=0;i<12;i++){double z=SZ-8.25+i*1.5,x=SX+22+(i%2)*1.5;var u=b.spawn(dusk,Campaign.troop("bonebow"),40+i,ground(b,x,z));place(b,u,x,z,90);fixture.add(u);}
                    next(b,73);
                }
                case 73->{ // the shield line holds: Aldric plants his banner while the Bone Archers come into range and loose
                    if(since(b)==60)order(b,"infantry","hold","shieldwall",SX,SZ,-90);
                    long arrows=b.shots.stream().filter(s->s.squad()==dusk&&s.kind().equals("arrow")).count();
                    if(since(b)>60&&!b.banners.isEmpty()&&(arrows>0||since(b)>240)&&wantShot==null){arrowsAtShot=(int)arrows;sp(b).sendMessage(net.minecraft.text.Text.empty(),true);requestShot("gilded-showcase");next(b,74);} // the order's action bar line is cleared off the picture
                    else if(since(b)>400){report("p2-gilded-showcase",false,"no banner was planted ("+b.banners.size()+" standing) or no arrow flew ("+arrows+")");clearFixture(b);next(b,80);}
                }
                case 74->{if(!shotTaken)return;
                    var banner=b.banners.isEmpty()?null:b.banners.get(0);boolean standing=banner!=null&&banner.block()!=null&&b.world.getBlockState(banner.block()).getBlock() instanceof net.minecraft.block.BannerBlock;
                    long squires=alive(b,own,"knight"),templars=alive(b,own,"sentinel"),longbows=alive(b,own,"longbow"),aldric=alive(b,own,"gl_marshal");
                    boolean wall=b.storedAssignment(own,"infantry").formation.equals("shieldwall")&&b.storedAssignment(own,"infantry").order.equals("hold");
                    boolean ok=hudOk&&clientOk&&standing&&squires>0&&templars>0&&longbows>0&&aldric>0&&wall&&arrowsAtShot>0&&own.traits.getOrDefault("gilded",0)==3;
                    report("p2-gilded-showcase",ok,"map "+b.campaign.map+": "+hudWhy+"; "+clientWhy+"; the army's traits "+own.traits+"; Aldric's banner stands="+standing+(banner==null?"":" at "+fmt(banner.at().x)+", "+fmt(banner.at().z))+"; "+squires+" Order Squires, "+templars+" Templars, "+longbows+" Order Longbows and Aldric="+(aldric>0)+" with the infantry on Hold in a shieldwall="+wall+"; "+arrowsAtShot+" Bone Archer arrows in flight at the request (see p2-gilded-showcase.png)");
                    wantShot=null;clearFixture(b);next(b,80);
                }
                case 80->{ // p2-gilded-tick-cost: eight bot armies, four under gilded_hard against four under graveborn_hard
                    var p=sp(b);if(p==null)return;
                    if(cost==null)cost=tickCost(b,List.of("gilded_hard","gilded_hard","gilded_hard","gilded_hard","graveborn_hard","graveborn_hard","graveborn_hard","graveborn_hard"));
                    if(cost.step(p)){
                        report("p2-gilded-tick-cost",cost.last<40,"server tick "+String.format(Locale.ROOT,"%.2f",cost.last)+" ms (600-tick mean "+String.format(Locale.ROOT,"%.2f",cost.avg)+" ms) with "+cost.troops+" troops at the start and "+b.units.size()+" at the end, "+b.banners.size()+" Oath Banners standing ("+cost.note+")");
                        audit(b,"before the final returnLobby",false);
                        report("p2-gilded-teams-clean",audits>=6&&unloadWasOnTeam&&staleSeen.isEmpty(),audits+" audits after the suite's direct removals (a troop removed as a chunk unload removes it, which vanilla does not clear: was on its team="+unloadWasOnTeam+"; stageFixture empties every team; drop, clearFixture, the showcase's clear-out and the end of the tick-cost run each leave only living troops on a team): "+(staleSeen.isEmpty()?"no side team kept a member that was not a living troop":"STALE "+staleSeen)+" ["+String.join("; ",auditNotes)+"]");
                        cost.handBack();b.campaign.returnLobby();next(b,99);
                    }
                }
                default->{}
            }
        }
    }

    /**
     * The fixture the Graveborn and Ironclad suites share: the stage clock, a stone sky platform 40 blocks east of Dawn's base, pinned posts for
     * troops, the side-team audit and the fixture's clean-up. A suite adds its own scratch and stages; cleared() and staged() are its hooks.
     */
    abstract static class Staged implements Suite,ServerWatch {
        record Pin(GravesAndGlory.Unit u,Vec3d at,float yaw){}
        volatile int stage;volatile UUID player;volatile String wantShot;volatile boolean shotTaken,finished;
        long stageAt,t0;int shotDelay,shotAfter=12;String classBefore;
        BlockPos site;final List<GravesAndGlory.Unit> fixture=new ArrayList<>();final List<Pin> pins=new ArrayList<>();
        final Set<net.minecraft.util.math.ChunkPos> forced=new HashSet<>();
        GravesAndGlory.Squad own,dawn,dusk;
        // side teams: one audit after each place the suite removes or raises troops
        int audits;final Map<String,String> staleSeen=new LinkedHashMap<>();final List<String> auditNotes=new ArrayList<>();

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Profile profile(GravesAndGlory.Battle b){return b.campaign.profile(player);}
        void requestShot(String name,int after){shotTaken=false;shotDelay=0;shotAfter=after;wantShot=name;}
        static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}
        static String fmt(double v){return String.format(Locale.ROOT,"%.2f",v);}
        static String show(Combat.HitLog h){return Engine.show(h);}
        static double distance(Vec3d a,Vec3d c){return Math.sqrt(GravesAndGlory.Battle.horizontalDistance(a,c));}
        /** A troop of the army on the platform at (dx, dz) from its centre with its AI off, held there each tick by the pins; a frozen troop never attacks or casts. */
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Spec.TroopsRow row,int slot,double dx,double dz,float yaw,boolean freeze){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);
            var u=b.spawn(s,row,slot,at);u.mob.setAiDisabled(true);if(freeze)Phase1Checks.freeze(u);
            fixture.add(u);pins.add(new Pin(u,at,yaw));u.deployed=at;Engine.apply(u,at,yaw);return u;
        }
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw,boolean freeze){return post(b,s,Campaign.troop(troop),30+fixture.size(),dx,dz,yaw,freeze);}
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Spec.TroopsRow row,double dx,double dz,float yaw,boolean freeze){return post(b,s,row,30+fixture.size(),dx,dz,yaw,freeze);}
        void move(GravesAndGlory.Unit u,double dx,double dz,float yaw){Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);pins.removeIf(p->p.u()==u);pins.add(new Pin(u,at,yaw));u.deployed=at;Engine.apply(u,at,yaw);}
        /** The troop's armour off, so the health it loses is the amount applied. */
        static GravesAndGlory.Unit bare(GravesAndGlory.Unit u){for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))u.mob.equipStack(slot,ItemStack.EMPTY);return u;}
        /** A real blow of this size from the attacker's troop, past the victim's hurt cooldown, through Combat.resolve (so the on-hit abilities and the ALLOW_DEATH hook run). */
        static float strike(GravesAndGlory.Battle b,GravesAndGlory.Unit attacker,GravesAndGlory.Unit victim,float base){victim.mob.timeUntilRegen=0;victim.mob.hurtTime=0;return Combat.resolve(b,attacker,victim.mob,base,"melee",false);}
        void platform(GravesAndGlory.Battle b,BlockPos c,int half,net.minecraft.block.BlockState floor){
            for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){b.world.setBlockState(c.add(x,-1,z),floor);for(int y=0;y<=6;y++)b.world.setBlockState(c.add(x,y,z),Blocks.AIR.getDefaultState());}
        }
        void force(GravesAndGlory.Battle b,BlockPos from,BlockPos to){
            for(int cx=from.getX()>>4;cx<=to.getX()>>4;cx++)for(int cz=from.getZ()>>4;cz<=to.getZ()>>4;cz++){b.world.setChunkForced(cx,cz,true);b.world.getChunk(cx,cz);forced.add(new net.minecraft.util.math.ChunkPos(cx,cz));}
        }
        void unforce(GravesAndGlory.Battle b){for(var p:forced)b.world.setChunkForced(p.x,p.z,false);forced.clear();}
        /** What a suite clears besides the fixture itself (its own lists and handles). */
        void cleared(){}
        void clearFixture(GravesAndGlory.Battle b,String where){for(var u:fixture)u.mob.discard();b.units.removeAll(fixture);fixture.clear();pins.clear();cleared();audit(b,where,false);}
        /** After a removal or a raising: no gg_ side team keeps a member that is not a living troop of b.units (and with `none` standing, none keeps any). */
        void audit(GravesAndGlory.Battle b,String where,boolean none){
            var stale=staleSides(b);var all=sideMembers(b);audits++;
            for(String m:none?all:stale)staleSeen.putIfAbsent(m,where);
            if(auditNotes.size()<24)auditNotes.add(where+" "+all.size()+" on a team"+(stale.isEmpty()?"":", "+stale.size()+" stale"));
        }
        /** After each server tick: the pinned troops stand where the stage put them. */
        void hold(){for(var p:pins)if(p.u().mob.isAlive())Engine.apply(p.u(),p.at(),p.yaw());}
        /** The army's stored state back to nothing: no last stand, no health ceiling, no orders. */
        static void reset(GravesAndGlory.Squad s){s.lastStand.clear();s.lastStandAt.clear();s.fullHealth.clear();s.assignments.clear();s.arrived.clear();}
        /** What a suite does once the platform stands (before the armies are made). */
        void staged(){}
        /** Every army's troops leave and none come back; a stone sky platform 40 blocks east of Dawn's base; two ad hoc armies (bots, so no profile is read) beside the commander's own, which keeps his seat and his class. */
        void stageFixture(GravesAndGlory.Battle b){
            Projectiles.clear(b);Abilities.clearBanners(b);clearFixture(b,"clearFixture");
            for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;s.training.clear();if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
            for(var u:b.units)u.mob.discard();b.units.clear();audit(b,"stageFixture",true);b.deaths.clear();
            var base=GravesAndGlory.Battle.base(0);site=new BlockPos((int)base.x+40,140,(int)base.z);force(b,site.add(-13,0,-13),site.add(13,0,13));platform(b,site,12,Blocks.STONE.getDefaultState());staged();
            own=b.squads.get(player);reset(own);
            dawn=new GravesAndGlory.Squad(UUID.randomUUID(),0,base);dawn.order="hold";dawn.bot=true;dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,base);dusk.order="hold";dusk.bot=true;
        }
    }

    /**
     * Task 7 (batch 2): the Graveborn mechanics. The two new mobs (burn, water, goals), cleave, frost shot, the Gravecaller's blight,
     * the fearless stance and the dread aura (no group routs: the no-routing design), Rise Again (once, not Legendaries, not temporary troops, the chance by tier), the Warlock's
     * Grave Bloom with its death log, the cleanup of temporary Risen at the end of a round and at a map switch, and the tick cost. The
     * fixture is Gilded's: a stone platform in the sky above Dawn's base, two ad hoc armies and the commander's own (a Warlock);
     * troops are posted frozen and pinned, and every kill goes through the real damage path (Combat.resolve, or damage with no
     * attacker), so Rise Again's ALLOW_DEATH hook and the AFTER_DEATH death log run as they do in a round.
     */
    static final class Graveborn extends Staged {
        final List<GravesAndGlory.Unit> group=new ArrayList<>();
        GravesAndGlory.Unit hk,hexer,stalker,reaper;
        // p2-graveborn-mobs
        BlockPos tank;final List<MobEntity> army=new ArrayList<>();MobEntity vanillaZombie,vanillaStray,armyInTank,vanillaInTank;
        boolean mobsOk,armyBurned,controlBurned,strayControlBurned;int submerged;String mobsWhy="";
        // the blight and the frost shot
        Projectiles.Shot shot;GravesAndGlory.Unit centre,inside,outside;long burst=-1;
        // fearless and dread
        boolean moraleOk,early,heldAt15;String moraleWhy="";long lastStandAfter=-1;
        // Rise Again
        GravesAndGlory.Unit risen,riseK1,riseControl,riseK2;float hpK1,hpK2;boolean riseOk,heldStun,controlHit,afterStun;String riseWhy="";int soulsBefore;
        // Grave Bloom
        Vec3d aim;final List<GravesAndGlory.Unit> bloom=new ArrayList<>(),corpses=new ArrayList<>();boolean bloomOk;String bloomWhy="";boolean shown;int deathsAfter;
        // a stunned caster, and temporary troops in morale
        final GravesAndGlory.Unit[] hexers=new GravesAndGlory.Unit[3],victims=new GravesAndGlory.Unit[3];float midHealth;boolean midSlow;String stunWhy="";boolean stunOk;final List<GravesAndGlory.Unit> temps=new ArrayList<>();
        // the cleanup and the tick cost
        TickCost cost;boolean pathsOk,inserted;String pathsWhy="";GravesAndGlory.Unit loose;int toMap;Vec3d lobbySpot;

        static long temporaries(GravesAndGlory.Battle b){return b.units.stream().filter(u->u.temporary).count();}
        /** A fall with no attacker, past the hurt cooldown: the plain damage path (no kill credit). */
        static void fall(GravesAndGlory.Battle b,GravesAndGlory.Unit victim){victim.mob.timeUntilRegen=0;victim.mob.hurtTime=0;victim.mob.damage(b.world.getDamageSources().generic(),1000f);}
        void drop(GravesAndGlory.Battle b,GravesAndGlory.Unit u){u.mob.discard();b.units.remove(u);fixture.remove(u);group.remove(u);pins.removeIf(p->p.u()==u);audit(b,"drop",false);}
        void dropAll(GravesAndGlory.Battle b,List<GravesAndGlory.Unit> us,String where){for(var u:new ArrayList<>(us)){u.mob.discard();b.units.remove(u);fixture.remove(u);pins.removeIf(p->p.u()==u);}us.clear();audit(b,where,false);}
        @Override void cleared(){group.clear();bloom.clear();corpses.clear();}
        /** Sets every living troop of the list to this share of its health, and the army's ceiling for the group so its strength is that share. */
        void weaken(GravesAndGlory.Squad s,String grp,List<GravesAndGlory.Unit> us,double share){
            double living=0;for(var u:us)if(u.mob.isAlive()){u.mob.setHealth((float)(u.mob.getMaxHealth()*share));living+=u.mob.getHealth();}
            s.fullHealth.put(grp,living/share);
        }
        /** The commander on the platform, west of its centre, facing east and looking down at the ground 12 blocks ahead; aim is where the Bloom's raycast lands. */
        void stand(GravesAndGlory.Battle b,ServerPlayerEntity p,float yaw){
            p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);b.relocate(p,site.getX()+.5-10,site.getY(),site.getZ()+.5,yaw,(float)Math.toDegrees(Math.atan2(1.62,12)));
            aim=Commander.GraveBloom.aim(p,Commander.row(own).range());
        }
        /** A fallen troop of this army at an offset from the aim: posted where it falls, then dead by a fall with no attacker. */
        GravesAndGlory.Unit corpse(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double distance,double degrees){
            double r=Math.toRadians(degrees);var u=post(b,s,troop,aim.x-site.getX()-.5+Math.cos(r)*distance,aim.z-site.getZ()-.5+Math.sin(r)*distance,0,true);fall(b,u);corpses.add(u);return u;
        }

        public void cleanup(MinecraftClient c){
            server(c,b->{Houses.forceRise=null;clearFixture(b,"cleanup");if(site!=null)platform(b,site,12,Blocks.AIR.getDefaultState());unforce(b);if(cost!=null)cost.handBack();
                for(var m:army)m.discard();army.clear();for(var m:new MobEntity[]{vanillaZombie,vanillaStray,armyInTank,vanillaInTank})if(m!=null)m.discard();
                if(b.running)b.campaign.returnLobby();var p=sp(b);if(p!=null&&classBefore!=null)b.campaign.handle(p,"class:"+classBefore);});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=shotAfter){shot(c,wantShot);shotTaken=true;}
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"graveborn");}
            if(tick>30000&&!finished){finished=true;report("p2-graveborn-timeout",false,"stuck in stage "+stage);finish(c,"graveborn");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(player==null||stage>=99)return;
            try{stages(b);}catch(RuntimeException e){report("p2-graveborn-exception",false,"stage "+stage+": "+e);e.printStackTrace();next(b,99);}
        }
        void stages(GravesAndGlory.Battle b){
            hold();
            switch(stage){
                case 1->{ // p2-graveborn-mobs, the sheet and the classes: the entities the sheet names are the ones registered, and they carry only a SwimGoal
                    var w=b.world;boolean ok=true;StringBuilder why=new StringBuilder();
                    var armor=net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR;
                    Map<String,EntityType<?>> types=Map.<String,EntityType<?>>of("zombie",EntityType.ZOMBIE,"stray",EntityType.STRAY);
                    Map<String,Class<?>> classes=Map.<String,Class<?>>of("zombie",GravesAndGlory.ArmyZombie.class,"stray",GravesAndGlory.ArmyStray.class);
                    for(var line:List.of("revenant:zombie","stalker:stray")){
                        String troop=line.substring(0,line.indexOf(':')),entity=line.substring(line.indexOf(':')+1);var row=Campaign.troop(troop);
                        boolean sheet=row.entity().equals(entity);MobEntity m=null;String goals,targets,made;
                        try{m=ArmyMobs.create(row.entity(),w);made=m.getClass().getSimpleName();goals=Engine.goalNames(m,"goalSelector").toString();targets=Engine.goalNames(m,"targetSelector").toString();}
                        catch(ReflectiveOperationException|RuntimeException e){goals=targets="unreadable";made=e.toString();}
                        boolean good=sheet&&m!=null&&classes.get(entity).isInstance(m)&&ArmyMobs.isTroop(m)&&m.getType()==types.get(entity)&&goals.equals("[SwimGoal]")&&targets.equals("[]")&&m.getAttributeBaseValue(armor)==0;
                        ok&=good;why.append(troop).append(" (sheet entity ").append(row.entity()).append(", want ").append(entity).append(") -> ").append(made).append(", goals ").append(goals).append(", targets ").append(targets).append(good?"":" (BAD)").append("; ");
                    }
                    mobsOk=ok;mobsWhy=why.toString();next(b,2);
                }
                case 2->{ // a stone platform and a sealed tank in the open sky, at noon (Engine's fixture, for the new mobs)
                    var p=sp(b);if(p==null)return;
                    MobEntity[] list;MobEntity inTank;
                    try{list=new MobEntity[]{ArmyMobs.create("zombie",b.world),ArmyMobs.create("stray",b.world),new ZombieEntity(EntityType.ZOMBIE,b.world),new StrayEntity(EntityType.STRAY,b.world)};inTank=ArmyMobs.create("zombie",b.world);}
                    catch(RuntimeException e){report("p2-graveborn-mobs",false,mobsWhy+"an Army zombie or stray could not be made: "+e.getMessage());site=null;next(b,4);return;}
                    site=new BlockPos(p.getBlockX()+14,150,p.getBlockZ()+14);tank=site.add(14,0,0);
                    b.world.setTimeOfDay(6000);force(b,site.add(-6,0,-6),tank.add(6,0,6));
                    platform(b,site,6,Blocks.STONE.getDefaultState());
                    for(int x=-1;x<=5;x++)for(int y=-1;y<=3;y++)for(int z=-1;z<=3;z++){boolean in=x>=0&&x<=4&&y>=0&&y<=2&&z>=0&&z<=2;b.world.setBlockState(tank.add(x,y,z),in?Blocks.WATER.getDefaultState():Blocks.STONE.getDefaultState());}
                    army.clear();
                    for(int i=0;i<4;i++){var m=list[i];m.refreshPositionAndAngles(site.getX()+.5-3+i*2,site.getY(),site.getZ()+.5,0,0);m.equipStack(EquipmentSlot.HEAD,ItemStack.EMPTY);m.setPersistent();m.setAiDisabled(true);b.world.spawnEntity(m);if(i<2)army.add(m);}
                    vanillaZombie=list[2];vanillaStray=list[3];
                    armyInTank=inTank;armyInTank.refreshPositionAndAngles(tank.getX()+1.5,tank.getY(),tank.getZ()+1.5,0,0);armyInTank.setPersistent();b.world.spawnEntity(armyInTank);
                    vanillaInTank=new ZombieEntity(EntityType.ZOMBIE,b.world);vanillaInTank.refreshPositionAndAngles(tank.getX()+3.5,tank.getY(),tank.getZ()+1.5,0,0);vanillaInTank.setPersistent();b.world.spawnEntity(vanillaInTank);
                    armyBurned=controlBurned=strayControlBurned=false;submerged=0;next(b,3);
                }
                case 3->{ // no burning after 200 ticks, no conversion after 700
                    long t=since(b);
                    if(t<=200){for(var m:army)if(m.getFireTicks()>0||m.isOnFire())armyBurned=true;if(vanillaZombie.getFireTicks()>0)controlBurned=true;if(vanillaStray.getFireTicks()>0)strayControlBurned=true;}
                    if(armyInTank.isSubmergedIn(net.minecraft.registry.tag.FluidTags.WATER))submerged++;
                    if(t>=700){
                        boolean day=b.world.isDay();
                        boolean still=armyInTank instanceof GravesAndGlory.ArmyZombie&&armyInTank.isAlive()&&!armyInTank.isRemoved(),armyConverting=((ZombieEntity)armyInTank).isConvertingInWater(),vanillaConverting=((ZombieEntity)vanillaInTank).isConvertingInWater();
                        boolean ok=mobsOk&&!armyBurned&&controlBurned&&day&&still&&!armyConverting&&submerged>=690&&vanillaConverting;
                        report("p2-graveborn-mobs",ok,mobsWhy+"at noon (time "+b.world.getTimeOfDay()+", day="+day+") under an open sky the Army zombie and stray were never on fire in 200 ticks="+!armyBurned+" while the vanilla zombie beside them caught fire="+controlBurned+" (the vanilla stray set fire ticks="+strayControlBurned+"); the ArmyZombie under water for "+submerged+" of 700 ticks is still an ArmyZombie="+still+" and is not converting="+!armyConverting+"; the vanilla Zombie in the same tank is converting after 600 ticks="+vanillaConverting);
                        for(var m:army)m.discard();army.clear();vanillaZombie.discard();vanillaStray.discard();armyInTank.discard();vanillaInTank.discard();
                        for(int x=-1;x<=5;x++)for(int y=-1;y<=3;y++)for(int z=-1;z<=3;z++)b.world.setBlockState(tank.add(x,y,z),Blocks.AIR.getDefaultState());
                        platform(b,site,6,Blocks.AIR.getDefaultState());unforce(b);site=null;next(b,4);
                    }
                }
                case 4->{ // the commander is a Warlock for the rest of the suite
                    var p=sp(b);if(p==null)return;if(b.running){b.campaign.returnLobby();return;}
                    classBefore=profile(b).commander;b.campaign.handle(p,"class:warlock");
                    if(!profile(b).commander.equals("warlock")){report("p2-graveborn-class",false,"class:warlock never reached the profile (it says "+profile(b).commander+")");next(b,99);return;}
                    next(b,5);
                }
                case 5->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,6);}
                case 6->{if(since(b)<10)return;stageFixture(b);Houses.forceRise=Boolean.FALSE;next(b,10);}
                case 10->{ // p2-cleave: a Grave Reaper hits a bare Risen with a second Risen 1.5 beside it, a third 1.8 away and a fourth 3.2 away
                    reaper=post(b,dawn,"reaper",0,-3,0,true);var first=bare(post(b,dusk,"revenant",0,0,180,true));var nearOne=bare(post(b,dusk,"revenant",1.5,0,180,true));var second=bare(post(b,dusk,"revenant",-1.8,0.3,180,true));var far=bare(post(b,dusk,"revenant",3.2,0,180,true));
                    var row=Abilities.row(reaper.spec);double share=row.amount();boolean sheet=row.id().equals("cleave_reaper")&&row.mechanic().equals("cleave")&&near(row.radius(),2)&&share>0&&share<1;
                    StringBuilder why=new StringBuilder();boolean ok=sheet;
                    var mark=Combat.LAST.peekLast();float h1=first.mob.getHealth(),hn=nearOne.mob.getHealth(),hs=second.mob.getHealth(),hf=far.mob.getHealth();
                    float base=(float)Abilities.baseHit(b,reaper);strike(b,reaper,first,base);
                    var onFirst=Gilded.hitOn(first.mob,mark);var onNear=Gilded.hitOn(nearOne.mob,mark);var onSecond=Gilded.hitOn(second.mob,mark);var onFar=Gilded.hitOn(far.mob,mark);
                    float lf=h1-first.mob.getHealth(),ln=hn-nearOne.mob.getHealth(),ls=hs-second.mob.getHealth(),lfar=hf-far.mob.getHealth();
                    boolean run1=onFirst!=null&&lf>0&&onNear!=null&&Math.abs(onNear.base()-onFirst.base()*share)<1e-4&&near(onNear.total(),onFirst.total())&&Math.abs(ln-lf*(float)share)<1e-3&&onSecond==null&&ls==0&&onFar==null&&lfar==0;
                    ok&=run1;why.append("a Grave Reaper's blow (").append(row.id()).append(", radius ").append(row.radius()).append(", share ").append(row.amount()).append(")="+sheet+" hit the first Risen ").append(show(onFirst)).append(", it lost ").append(lf).append("; the Risen 1.5 beside it ").append(show(onNear)).append(", it lost ").append(ln).append(" (want "+fmt(share)+" of the first); the one 1.8 off lost ").append(ls).append(onSecond==null?" (no hit logged)":" ("+show(onSecond)+")").append(" and the one 3.2 off lost ").append(lfar).append(" (only the nearest other enemy is struck)="+run1+"; ");
                    // run 2: the nearest is gone, the next one is struck; run 3: nobody within 2, nobody struck
                    drop(b,nearOne);mark=Combat.LAST.peekLast();h1=first.mob.getHealth();hs=second.mob.getHealth();hf=far.mob.getHealth();strike(b,reaper,first,base);
                    onFirst=Gilded.hitOn(first.mob,mark);onSecond=Gilded.hitOn(second.mob,mark);onFar=Gilded.hitOn(far.mob,mark);lf=h1-first.mob.getHealth();ls=hs-second.mob.getHealth();lfar=hf-far.mob.getHealth();
                    boolean run2=onFirst!=null&&lf>0&&onSecond!=null&&Math.abs(ls-lf*(float)share)<1e-3&&onFar==null&&lfar==0;ok&=run2;
                    why.append("with the nearest gone the Risen 1.8 off took ").append(ls).append(" of the ").append(lf).append(" the first took (want "+fmt(share)+" of it) and the one 3.2 off ").append(lfar).append("="+run2+"; ");
                    drop(b,second);mark=Combat.LAST.peekLast();h1=first.mob.getHealth();hf=far.mob.getHealth();strike(b,reaper,first,base);
                    onFar=Gilded.hitOn(far.mob,mark);lf=h1-first.mob.getHealth();lfar=hf-far.mob.getHealth();boolean run3=lf>0&&onFar==null&&lfar==0;ok&=run3;
                    why.append("with nobody within 2 of the target the Risen 3.2 off lost ").append(lfar).append("="+run3);
                    report("p2-cleave",ok,why.toString());clearFixture(b,"cleave");next(b,11);
                }
                case 11->{ // p2-frostshot: a Barrow Stalker's arrow at a bare Risen 10 blocks off
                    stalker=post(b,dawn,"stalker",0,-10,0,true);risen=bare(post(b,dusk,"revenant",0,0,180,true));
                    Projectiles.fire(b,stalker,risen.mob,"arrow",(float)Abilities.baseHit(b,stalker));shot=b.shots.get(b.shots.size()-1);next(b,12);
                }
                case 12->{if(b.time<shot.lands()+2)return;
                    var row=Abilities.row(stalker.spec);var eff=risen.mob.getStatusEffect(StatusEffects.SLOWNESS);
                    boolean sheet=row.id().equals("frostshot")&&row.mechanic().equals("onhit_slow")&&row.statusTicks()==40&&row.statusLevel()==0;
                    boolean landed=eff!=null&&eff.getAmplifier()==0&&eff.getDuration()>=36&&eff.getDuration()<=40;
                    String s1=eff==null?"no Slowness":"Slowness "+(eff.getAmplifier()+1)+" for "+eff.getDuration()+" ticks";
                    // an arrow that lands on a troop already slowed for longer keeps the longer time; on a shorter one it sets the new time
                    float base=(float)Abilities.baseHit(b,stalker);
                    risen.mob.removeStatusEffect(StatusEffects.SLOWNESS);risen.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS,120,0));
                    risen.mob.timeUntilRegen=0;Combat.resolve(b,stalker,risen.mob,base,"arrow",false);var longer=risen.mob.getStatusEffect(StatusEffects.SLOWNESS);
                    boolean keeps=longer!=null&&longer.getAmplifier()==0&&longer.getDuration()>=118&&longer.getDuration()<=120;
                    risen.mob.removeStatusEffect(StatusEffects.SLOWNESS);risen.mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS,10,0));
                    risen.mob.timeUntilRegen=0;Combat.resolve(b,stalker,risen.mob,base,"arrow",false);var shorter=risen.mob.getStatusEffect(StatusEffects.SLOWNESS);
                    boolean sets=shorter!=null&&shorter.getDuration()>=38&&shorter.getDuration()<=40;
                    // a secondary hit (a cleave or a pierce) does not run the on-hit slow again
                    risen.mob.removeStatusEffect(StatusEffects.SLOWNESS);risen.mob.timeUntilRegen=0;Combat.resolve(b,stalker,risen.mob,base,"arrow",true);boolean none=!risen.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                    report("p2-frostshot",sheet&&landed&&keeps&&sets&&none,"the Barrow Stalker's "+row.id()+" ("+row.mechanic()+", "+row.statusTicks()+" ticks, level "+row.statusLevel()+")="+sheet+"; the arrow as it landed left "+s1+" on the Risen (want Slowness 1 for about 40)="+landed+"; a second arrow on a Risen slowed for 120 left "+(longer==null?"none":longer.getDuration()+" ticks")+" (the longer time kept)="+keeps+", on one slowed for 10 left "+(shorter==null?"none":shorter.getDuration()+" ticks")+" (the new 40)="+sets+"; a secondary hit applied none="+none);
                    clearFixture(b,"frostshot");next(b,13);
                }
                case 13->{ // p2-blight-gravecaller: a Gravecaller 8 blocks from a Risen; a second Risen 2.0 from the first and a third 3.0 from it
                    hexer=post(b,dawn,"hexer",0,-8,0,true);centre=bare(post(b,dusk,"revenant",0,0,180,true));inside=bare(post(b,dusk,"revenant",2.0,0,180,true));outside=bare(post(b,dusk,"revenant",-3.0,0,180,true));
                    hexer.abilityAt=0;t0=b.time;burst=-1;Abilities.attack(b,hexer,centre.mob,b.time);next(b,14);
                }
                case 14->{ // each tick as Tactics does on its beat: the ring marks the centre; the burst lands when the windup ends
                    var row=Abilities.row(hexer.spec);long t=b.time-t0;
                    if(hexer.spellCenter!=null){Abilities.tick(b,hexer,centre.mob,b.time);if(hexer.spellCenter==null)burst=t;}
                    if(burst>=0||t>60){
                        var eC=centre.mob.getStatusEffect(StatusEffects.SLOWNESS);var eI=inside.mob.getStatusEffect(StatusEffects.SLOWNESS);
                        boolean sheet=row.id().equals("blight_gravecaller")&&row.mechanic().equals("blight")&&near(row.radius(),2.5)&&row.windupTicks()==20&&row.statusTicks()==40&&row.statusLevel()==0&&row.cooldownTicks()==100;
                        boolean hitC=centre.mob.getHealth()<centre.mob.getMaxHealth()&&eC!=null&&eC.getAmplifier()==0&&eC.getDuration()>=36&&eC.getDuration()<=40;
                        boolean hitI=inside.mob.getHealth()<inside.mob.getMaxHealth()&&eI!=null&&eI.getAmplifier()==0;
                        boolean miss=outside.mob.getHealth()==outside.mob.getMaxHealth()&&!outside.mob.hasStatusEffect(StatusEffects.SLOWNESS);
                        boolean timing=burst==row.windupTicks()&&hexer.abilityAt==t0+row.cooldownTicks();
                        report("p2-blight-gravecaller",sheet&&hitC&&hitI&&miss&&timing,"the Gravecaller's "+row.id()+" (radius "+row.radius()+", windup "+row.windupTicks()+", cooldown "+row.cooldownTicks()+", slow "+row.statusTicks()+" ticks level "+row.statusLevel()+")="+sheet+"; the burst came "+burst+" ticks after the cast (want "+row.windupTicks()+"), its next cast set for +"+(hexer.abilityAt-t0)+" (want "+row.cooldownTicks()+")="+timing+"; the Risen at the centre lost "+fmt(centre.mob.getMaxHealth()-centre.mob.getHealth())+" and has "+(eC==null?"no Slowness":"Slowness "+(eC.getAmplifier()+1)+" for "+eC.getDuration())+"="+hitC+", the one 2.0 off lost "+fmt(inside.mob.getMaxHealth()-inside.mob.getHealth())+"="+hitI+", the one 3.0 off lost "+fmt(outside.mob.getMaxHealth()-outside.mob.getHealth())+" (untouched="+miss+")");
                        clearFixture(b,"blight");next(b,20);
                    }
                }
                case 20->{ // p2-fearless-share: the fearless stance, per soldier: a Risen at 30% of his health deals x1.2 and one at 31% x1.0; an Order Squire at 30% x1.0; a Bone Archer's arrow at 30% x1.2; Brassjaw at 30% x1.0 (no fearless tag)
                    reset(own);group.clear();
                    var low=post(b,own,"revenant",-6,-2,-90,true);var high=post(b,own,"revenant",-6,0,-90,true);var knight=post(b,own,"knight",-6,2,-90,true);var bow=post(b,own,"bonebow",-6,4,-90,true);var brass=post(b,own,"ic_colossus",-6,-5,-90,true);
                    for(int i=0;i<3;i++)post(b,own,"revenant",-9,-4+i*2,-90,true); // six Graveborn slots: tier I, so Rise Again can raise the first Risen
                    var target=post(b,dusk,"knight",6,0,90,true);Houses.recompute(own);
                    var fearless=Abilities.row(low.spec);
                    boolean sheet=fearless.mechanic().equals("fearless")&&near(fearless.amount(),0.2)&&near(fearless.amount2(),0.3)&&near(Spec.COMBATDISCIPLINE[0].woundedShare(),0.3)&&bow.tags.contains("fearless")&&Abilities.row(bow.spec).mechanic().equals("fearless")&&!brass.tags.contains("fearless")&&brass.tags.contains("large");
                    // the first Risen falls to a blow and rises through Rise Again (Houses.allowDeath, its roll forced): the rise itself puts him at the tier's 30%
                    var rise=CounterMath.trait(Spec.HOUSETRAITS,"graveborn",own.traits.getOrDefault("graveborn",0));
                    Houses.forceRise=Boolean.TRUE;low.mob.timeUntilRegen=0;low.mob.hurtTime=0;try{low.mob.damage(b.world.getDamageSources().generic(),low.mob.getMaxHealth()*4);}finally{Houses.forceRise=null;}
                    float atRise=low.mob.getHealth();boolean rose=rise!=null&&rise.mechanic().equals("rise_again")&&low.rose&&low.mob.isAlive()&&Math.abs(atRise-rise.value2()*low.mob.getMaxHealth())<1e-3;
                    high.mob.setHealth(high.mob.getMaxHealth()*0.31f);knight.mob.setHealth(knight.mob.getMaxHealth()*0.3f);bow.mob.setHealth(bow.mob.getMaxHealth()*0.3f);brass.mob.setHealth(brass.mob.getMaxHealth()*0.3f);
                    var hLow=Commanders.hit(b,low,target.mob,"melee");var hHigh=Commanders.hit(b,high,target.mob,"melee");var hKnight=Commanders.hit(b,knight,target.mob,"melee");var hBow=Commanders.hit(b,bow,target.mob,"arrow");var hBrass=Commanders.hit(b,brass,target.mob,"melee");
                    low.mob.setHealth(low.mob.getMaxHealth());var hFull=Commanders.hit(b,low,target.mob,"melee");
                    boolean stance=hLow!=null&&near(hLow.stance(),1.2)&&near(hLow.total(),1.2)&&hHigh!=null&&near(hHigh.stance(),1.0)&&hKnight!=null&&near(hKnight.stance(),1.0)&&hBow!=null&&near(hBow.stance(),1.2)&&hBrass!=null&&near(hBrass.stance(),1.0)&&hFull!=null&&near(hFull.stance(),1.0);
                    report("p2-fearless-share",sheet&&stance&&rose,"the sheet's fearless row (amount "+fearless.amount()+" at amount2 "+fearless.amount2()+" of max health; combatDiscipline.woundedShare "+Spec.COMBATDISCIPLINE[0].woundedShare()+"), the Bone Archer carries it and Brassjaw does not (tags "+brass.tags+")="+sheet+"; blows on a Dusk Squire: a Risen that fell to a blow and rose through Rise Again (Graveborn tier "+own.traits.getOrDefault("graveborn",0)+", rose="+low.rose+", at "+atRise+" of "+low.mob.getMaxHealth()+" when he rose)="+rose+" "+show(hLow)+" (want stance 1.2, total 1.2: the rise's 30% makes him fearless at once), at 31% "+show(hHigh)+" (1.0), an Order Squire at 30% "+show(hKnight)+" (1.0), a Bone Archer's arrow at 30% "+show(hBow)+" (1.2), Brassjaw at 30% "+show(hBrass)+" (1.0), the Risen healed "+show(hFull)+" (1.0)="+stance);
                    clearFixture(b,"fearless");reset(own);next(b,30);
                }
                case 30->{ // p2-dread: four Order Squires of the own army, a Dusk Risen in sight and the Hollow King 24 blocks away, then 5.5 from the platform's centre
                    reset(own);group.clear();
                    for(int i=0;i<4;i++)group.add(post(b,own,"knight",-6+i*1.5,0,-90,true));
                    post(b,dusk,"revenant",6,3,90,true);hk=post(b,dusk,"gb_hollowking",20,0,90,true);Houses.recompute(own);
                    var far=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");
                    var dread=Abilities.dreadRow(hk);
                    boolean sheet=dread!=null&&near(dread.amount(),0.15)&&near(dread.amount2(),0)&&near(dread.radius(),10);
                    moraleOk=sheet&&Gilded.auras(far,1.0);
                    moraleWhy="the Hollow King's ability "+hk.spec.ability()+" ("+(dread==null?"not a dread row":"amount "+dread.amount()+", amount2 "+dread.amount2()+" (want 0: nothing routs), radius "+dread.radius())+")="+sheet+"; with him 24 blocks off a Squire's blow has auras "+Gilded.au(far)+" (want 1.0); ";
                    move(hk,5.5,0,90);
                    // his dread on every blow of the enemy's troops that stands within 10 of him; the largest of several, and a banner's share multiplies in
                    var inReach=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");var outReach=Commanders.hit(b,group.get(0),fixture.get(4).mob,"melee");var his=Commanders.hit(b,fixture.get(4),group.get(3).mob,"melee");
                    var second=post(b,dusk,"gb_hollowking",6,-3,90,true);var two=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");drop(b,second);
                    var banner=new Abilities.Banner(own,group.get(3).mob.getPos(),b.time+300,12,0.1,true,null,null);b.banners.add(banner);var withBanner=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");b.banners.remove(banner);
                    boolean auras=Gilded.auras(inReach,0.85)&&Gilded.auras(outReach,1.0)&&Gilded.auras(his,1.0)&&Gilded.auras(two,0.85)&&withBanner!=null&&Math.abs(withBanner.auras()-1.1*0.85)<1e-9;
                    moraleOk&=auras;
                    moraleWhy+="with the Hollow King at 7.0 from one Squire and 11.5 from another: a blow by the near one has auras "+Gilded.au(inReach)+" (want 0.85), by the far one "+Gilded.au(outReach)+" (1.0), by a Dusk Risen beside the King "+Gilded.au(his)+" (his own side, 1.0), with a second King in reach "+Gilded.au(two)+" (not stacked, 0.85), with an Oath Banner too "+(withBanner==null?"none":String.valueOf(withBanner.auras()))+" (1.1 x 0.85)="+auras+"; ";
                    weaken(own,"infantry",group,0.35);early=false;t0=b.time;next(b,31);
                }
                case 31->{ // the Squires at 35% beside him keep their order and stand no last stand in 45 ticks (the share is the sheet's 0.25, dread or not); then at 20% they do
                    if(Morale.lastStand(own,"infantry"))early=true;
                    if(since(b)<45)return;
                    heldAt15=!early&&b.assignment(group.get(0)).order.equals(own.order);
                    moraleWhy+="four Squires at 35% strength with the King 9.25 from their centre kept their order ("+b.assignment(group.get(0)).order+") and stood no last stand in 45 ticks="+heldAt15+"; ";
                    weaken(own,"infantry",group,0.20);t0=b.time;lastStandAfter=-1;next(b,32);
                }
                case 32->{
                    if(lastStandAfter<0&&Morale.lastStand(own,"infantry"))lastStandAfter=b.time-t0;
                    if(lastStandAfter<0&&since(b)<=60)return;
                    boolean last=lastStandAfter>=0&&lastStandAfter<=40&&b.assignment(group.get(0)).order.equals(own.order);
                    report("p2-dread",moraleOk&&heldAt15&&last,moraleWhy+"at 20% strength with Dusk within "+Spec.COMBATDISCIPLINE[0].lastStandRadius()+" they turned to their last stand "+(lastStandAfter<0?"never":lastStandAfter+" ticks later")+" (want 40 or fewer) with their order still "+b.assignment(group.get(0)).order+"="+last);
                    clearFixture(b,"dread");reset(own);next(b,40);
                }
                case 40->{ // p2-rise-again: a Risen of a Graveborn army (six Risen: tier I) at 1 health, struck dead by an Order Squire
                    reset(own);Houses.forceRise=Boolean.TRUE;dusk.order="attack";dusk.target=new Vec3d(site.getX()+.5,site.getY(),site.getZ()+.5);dawn.order="attack";
                    risen=post(b,dusk,"revenant",-8,-4,0,true);for(int i=0;i<5;i++)post(b,dusk,"revenant",-10+i*1.5,10,0,true);Houses.recompute(dusk);
                    riseK1=post(b,dawn,"knight",-8,-2.5,180,true);
                    var pl=sp(b);if(pl!=null){pl.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);b.relocate(pl,site.getX()+.5-12,site.getY(),site.getZ()+.5-4,-90f,10f);} // set down ten ticks before the blow: a particle packet reaches only the players within 32 blocks of it, and the client's camera trails a teleport, so a packet that arrives as the player lands is dropped
                    t0=b.time;next(b,44);
                }
                case 44->{if(b.time-t0<10)return;
                    var trait=CounterMath.trait(Spec.HOUSETRAITS,"graveborn",dusk.traits.getOrDefault("graveborn",0));
                    risen.mob.setHealth(1f);soulsBefore=BattleEffects.souls;long died=b.time;int killsBefore=dawn.stats.kills,goldBefore=dawn.stats.goldEarned;
                    strike(b,riseK1,risen,50f);
                    boolean alive=risen.mob.isAlive()&&b.units.contains(risen);float want=(float)(trait==null?-1:trait.value2()*risen.mob.getMaxHealth());
                    boolean stats=risen.rose&&risen.stunnedUntil==died+20&&Math.abs(risen.mob.getHealth()-want)<0.01&&BattleEffects.souls==soulsBefore+1&&dusk.stats.lost==0&&dawn.stats.kills==killsBefore&&dawn.stats.goldEarned==goldBefore&&b.deaths.isEmpty();
                    audit(b,"rise",false);boolean team=sideMembers(b).stream().anyMatch(m->m.endsWith(risen.mob.getEntityName()));
                    boolean sheet=trait!=null&&trait.mechanic().equals("rise_again")&&trait.value()>0&&trait.value()<1&&near(trait.value2(),0.3)&&trait.durationTicks()==20&&dusk.traits.getOrDefault("graveborn",0)==1;
                    riseOk=alive&&stats&&team&&sheet;
                    riseWhy="a Graveborn army at tier "+dusk.traits+" (row "+(trait==null?"none":trait.id()+": chance "+trait.value()+", back at "+trait.value2()+" of its health, stunned "+trait.durationTicks()+" ticks")+")="+sheet+"; a Risen at 1 health struck dead by an Order Squire with the roll forced: alive and still in the army="+alive+", health "+fmt(risen.mob.getHealth())+" of "+fmt(risen.mob.getMaxHealth())+" (want "+fmt(want)+"), rose="+risen.rose+", stunned until +"+(risen.stunnedUntil-died)+" (want 20), soul effects +"+(BattleEffects.souls-soulsBefore)+", the army lost "+dusk.stats.lost+" troops and the Squire's army earned "+(dawn.stats.kills-killsBefore)+" kills and "+(dawn.stats.goldEarned-goldBefore)+" gold (all 0: it did not die), the death log holds "+b.deaths.size()+" (want 0)="+stats+", still on its side's team="+team+"; ";
                    // it is ready to strike again at once; a control Risen beside a second Squire is let loose now, and shows the setup lets a Risen strike inside the window
                    requestShot("graveborn-rise",4);
                    risen.nextAttack=0;riseControl=post(b,dusk,"revenant",8,-4,0,false);riseK2=post(b,dawn,"knight",8,-2.5,180,true);
                    hpK1=riseK1.mob.getHealth();hpK2=riseK2.mob.getHealth();t0=died;heldStun=true;controlHit=false;afterStun=false;next(b,41);
                }
                case 41->{ // no attack in its 20 ticks of stun, an attack once they are over; the control strikes inside them
                    long t=b.time-t0;
                    if(t<20&&riseK1.mob.getHealth()<hpK1)heldStun=false;
                    if(t<=19&&riseK2.mob.getHealth()<hpK2)controlHit=true;
                    if(t>=45){
                        afterStun=riseK1.mob.getHealth()<hpK1;
                        riseOk&=heldStun&&controlHit&&afterStun;
                        riseWhy+="the stunned Risen beside a Squire (health "+fmt(hpK1)+") made no attack in its 20 ticks="+heldStun+" and had struck by +45="+afterStun+" (the Squire has "+fmt(riseK1.mob.getHealth())+"); an unstunned Risen beside another Squire struck inside those ticks="+controlHit+" (it has "+fmt(riseK2.mob.getHealth())+" of "+fmt(hpK2)+"); ";
                        next(b,42);
                    }
                }
                case 42->{ // killed again it dies; and the exceptions, each with the roll forced
                    risen.mob.setHealth(1f);int lostBefore=dusk.stats.lost,killsBefore=dawn.stats.kills,goldBefore=dawn.stats.goldEarned;Vec3d at=risen.mob.getPos();
                    strike(b,riseK1,risen,50f);
                    var d=b.deaths.size()==1?b.deaths.get(0):null;
                    boolean dies=!risen.mob.isAlive()&&risen.rose&&dusk.stats.lost==lostBefore+1&&dawn.stats.kills==killsBefore+1&&dawn.stats.goldEarned==goldBefore+Spec.ECONOMY[0].killGold()&&d!=null&&d.troop().equals("revenant")&&d.team()==1&&d.time()==b.time&&distance(d.at(),at)<1;
                    audit(b,"second death",false);
                    riseWhy+="killed again with the roll still forced it dies="+dies+" (the army lost "+(dusk.stats.lost-lostBefore)+", the Squire's army earned "+(dawn.stats.kills-killsBefore)+" kill and "+(dawn.stats.goldEarned-goldBefore)+" gold, the death log holds "+b.deaths.size()+(d==null?"":": "+d.troop()+" of team "+d.team()+" at "+fmt(distance(d.at(),at))+" from where it fell")+"); ";
                    var legend=post(b,dusk,"gb_hollowking",0,4,0,true);legend.mob.setHealth(1f);strike(b,riseK1,legend,500f);boolean legendDies=!legend.mob.isAlive()&&!legend.rose;
                    var temp=post(b,dusk,"revenant",2,4,0,true);temp.temporary=true;temp.expiresAt=b.time+1000;Houses.recompute(dusk);temp.mob.setHealth(1f);int lostTemp=dusk.stats.lost,logTemp=b.deaths.size(),killsTemp=dawn.stats.kills;strike(b,riseK1,temp,500f);
                    boolean tempDies=!temp.mob.isAlive()&&!temp.rose&&dusk.stats.lost==lostTemp&&b.deaths.size()==logTemp&&dawn.stats.kills==killsTemp;
                    var plain=post(b,dusk,"knight",4,4,0,true);plain.mob.setHealth(1f);strike(b,riseK1,plain,500f);boolean plainDies=!plain.mob.isAlive()&&!plain.rose;
                    var untiered=post(b,dusk,"revenant",6,4,0,true);dusk.traits.clear();untiered.mob.setHealth(1f);strike(b,riseK1,untiered,500f);boolean tierDies=!untiered.mob.isAlive()&&!untiered.rose;Houses.recompute(dusk);
                    var voided=post(b,dusk,"revenant",8,4,0,true);voided.mob.setHealth(1f);voided.mob.timeUntilRegen=0;voided.mob.kill();boolean killDies=!voided.mob.isAlive()&&!voided.rose;
                    Houses.forceRise=Boolean.FALSE;var unlucky=post(b,dusk,"revenant",10,4,0,true);unlucky.mob.setHealth(1f);strike(b,riseK1,unlucky,500f);boolean rollDies=!unlucky.mob.isAlive()&&!unlucky.rose;Houses.forceRise=Boolean.TRUE;
                    List<String> log=b.deaths.stream().map(GravesAndGlory.Death::troop).toList();
                    boolean logOk=log.equals(List.of("revenant","gb_hollowking","knight","revenant","revenant","revenant"));
                    audit(b,"exceptions",false);
                    riseOk&=dies&&legendDies&&tempDies&&plainDies&&tierDies&&killDies&&rollDies&&logOk;
                    riseWhy+="the Hollow King (Legendary) dies="+legendDies+"; a temporary Risen dies, with no loss, no kill and no death logged="+tempDies+"; an Order Squire of the army (Gilded, no Rise Again) dies="+plainDies+"; a Risen of an army with no Graveborn tier dies="+tierDies+"; one killed outright (the kill command, the void) dies="+killDies+"; a roll that fails dies="+rollDies+"; the death log reads "+log+" (want the troops that really fell, not the temporary one)="+logOk+"; soul effects over all: +"+(BattleEffects.souls-soulsBefore)+" (want 1)";
                    riseOk&=BattleEffects.souls==soulsBefore+1&&shotTaken;riseWhy+="; the picture graveborn-rise was taken="+shotTaken;wantShot=null;
                    report("p2-rise-again",riseOk,riseWhy);
                    clearFixture(b,"rise");next(b,43);
                }
                case 43->{ // p2-rise-chance: the roll's share by tier (20000 rolls each), and the whole hook at tier III (800 falls of one Risen)
                    Houses.forceRise=null;StringBuilder why=new StringBuilder();boolean ok=true;
                    double[] want=new double[3];for(int tier=1;tier<=3;tier++){var row=CounterMath.trait(Spec.HOUSETRAITS,"graveborn",tier);want[tier-1]=row==null?-1:row.value();} // the sheet's chances, whatever a gate has tuned them to
                    for(int tier=1;tier<=3;tier++){
                        var t=CounterMath.trait(Spec.HOUSETRAITS,"graveborn",tier);int n=20000,hits=0;for(int i=0;i<n;i++)if(Houses.rolls(b,t))hits++;
                        double share=hits/(double)n;boolean row=t!=null&&t.mechanic().equals("rise_again")&&near(t.value(),want[tier-1])&&want[tier-1]>0&&near(t.value2(),0.3)&&t.durationTicks()==20;boolean good=row&&Math.abs(share-want[tier-1])<0.02;ok&=good;
                        why.append("tier ").append(tier).append(": row chance ").append(t==null?"none":t.value()).append(", ").append(hits).append(" of ").append(n).append(" rolls rose (").append(fmt(share*100)).append("%, want ").append(fmt(want[tier-1]*100)).append("%)").append(good?"":" (BAD)").append("; ");
                    }
                    var one=post(b,dusk,"revenant",0,0,0,true);var src=b.world.getDamageSources().generic();int rises=0;dusk.traits.put("graveborn",3);
                    for(int i=0;i<800;i++){one.rose=false;one.stunnedUntil=0;one.mob.setHealth(1f);if(!Houses.allowDeath(one.mob,src))rises++;}
                    dusk.traits.clear();int untiered=0;for(int i=0;i<800;i++){one.rose=false;one.mob.setHealth(1f);if(!Houses.allowDeath(one.mob,src))untiered++;}
                    double chance=want[2],spread=Math.sqrt(chance*(1-chance)/800);boolean hook=Math.abs(rises/800.0-chance)<4*spread&&untiered==0;ok&=hook;Houses.recompute(dusk); // four standard deviations of 800 rolls (about 0.07 of them), so a fair roll fails about once in 16000 runs
                    why.append("through the hook at tier III ").append(rises).append(" of 800 falls rose (want about ").append(Math.round(chance*800)).append(", within ").append(fmt(4*spread*800)).append("); with no tier ").append(untiered).append(" of 800 did (want 0)=").append(hook);
                    report("p2-rise-chance",ok,why.toString());clearFixture(b,"rise-chance");Houses.forceRise=Boolean.FALSE;next(b,46);
                }
                case 46->{ // p2-stunned-caster: three Gravecallers of a Graveborn army each begin a blight at a bare Order Squire 8 blocks off; one is struck dead and rises (its stun), one is stunned directly, one is the control
                    reset(own);dusk.order="hold";dawn.order="hold";Houses.forceRise=Boolean.TRUE;
                    for(int i=0;i<6;i++)post(b,dusk,"revenant",-12+i*1.5,12,0,true);
                    double[] xs={-10,0,10};
                    for(int i=0;i<3;i++){hexers[i]=post(b,dusk,"hexer",xs[i],-8,0,true);victims[i]=bare(post(b,dawn,"knight",xs[i],0,180,true));}
                    var killer=post(b,dawn,"knight",-10,-5,180,true);Houses.recompute(dusk);
                    for(int i=0;i<3;i++){hexers[i].abilityAt=0;Abilities.attack(b,hexers[i],victims[i].mob,b.time);}
                    boolean begun=true;for(var h:hexers)begun&=h.spellCenter!=null&&h.windupUntil==b.time+20;
                    hexers[0].mob.setHealth(1f);strike(b,killer,hexers[0],500f);
                    hexers[1].stunnedUntil=b.time+30;t0=b.time;
                    boolean rose=hexers[0].rose&&hexers[0].mob.isAlive()&&hexers[0].stunnedUntil==t0+20&&hexers[0].spellCenter==null&&hexers[1].spellCenter!=null&&hexers[2].spellCenter!=null;
                    stunOk=begun&&rose;
                    stunWhy="all three Gravecallers began a blight with a windup of 20="+begun+"; one struck dead with the roll forced rose (rose="+hexers[0].rose+", stunned until +"+(hexers[0].stunnedUntil-t0)+"), and the spell it had begun is gone (spell centre "+(hexers[0].spellCenter==null?"none":"still set")+"), while the other two still hold theirs="+rose+"; ";
                    next(b,47);
                }
                case 47->{ // each tick as Tactics does on its beat: the stunned ones land nothing; once its stun is over the risen one casts again; the control lands its blight
                    long t=b.time-t0;
                    for(int i=0;i<3;i++)Abilities.tick(b,hexers[i],victims[i].mob,b.time);
                    if(t==24){midHealth=victims[0].mob.getHealth();midSlow=victims[0].mob.hasStatusEffect(StatusEffects.SLOWNESS);}
                    if(t==25){hexers[0].abilityAt=0;Abilities.attack(b,hexers[0],victims[0].mob,b.time);}
                    if(t>=55){
                        var max=victims[0].mob.getMaxHealth();
                        boolean cancelled=midHealth==max&&!midSlow&&victims[1].mob.getHealth()==max&&!victims[1].mob.hasStatusEffect(StatusEffects.SLOWNESS)&&hexers[1].spellCenter==null;
                        boolean again=victims[0].mob.getHealth()<max;boolean control=victims[2].mob.getHealth()<max&&victims[2].mob.hasStatusEffect(StatusEffects.SLOWNESS);
                        stunOk&=cancelled&&again&&control;
                        stunWhy+="the risen one's target had "+fmt(midHealth)+" of "+fmt(max)+" at +24, after the cast's 20 ticks of windup (want full, no Slowness), and the directly stunned one's "+fmt(victims[1].mob.getHealth())+" at +55 with no Slowness and its spell dropped="+cancelled+"; the risen one cast again at +25, once its stun was over, and its target lost "+fmt(max-victims[0].mob.getHealth())+"="+again+"; the control Gravecaller's target lost "+fmt(max-victims[2].mob.getHealth())+" and is slowed="+control;
                        report("p2-stunned-caster",stunOk,stunWhy);
                        clearFixture(b,"stunned-caster");Houses.forceRise=Boolean.FALSE;next(b,48);
                    }
                }
                case 48->{ // p2-temporary-morale: three temporary Risen with a target and a spell, ten Order Squires of the army, a Dusk Squire in sight: the group is ordered to Retreat
                    reset(own);group.clear();temps.clear();Houses.forceRise=Boolean.FALSE;
                    for(int i=0;i<10;i++)group.add(post(b,own,"knight",-6+(i%5)*1.5,-5+(i/5)*1.5,-90,true));
                    for(int i=0;i<3;i++){var tmp=post(b,own,"revenant",-10+i*1.5,22,-90,true);tmp.temporary=true;tmp.expiresAt=b.time+2000;temps.add(tmp);}
                    var foe=post(b,dusk,"knight",8,-4,90,true);Houses.recompute(own);
                    var members=Morale.members(b,own,"infantry");Vec3d moraleCentre=Morale.centre(members),groupCentre=Abilities.centre(b,own,"infantry");
                    boolean centres=members.size()==10&&groupCentre!=null&&groupCentre.squaredDistanceTo(moraleCentre)<1e-12;
                    for(var tmp:temps){tmp.lockedTarget=foe.mob;tmp.lockUntil=b.time+1000;tmp.spellCenter=foe.mob.getPos();tmp.mob.setTarget(foe.mob);}
                    group.get(0).lockedTarget=foe.mob;group.get(0).spellCenter=foe.mob.getPos();group.get(0).mob.setTarget(foe.mob);
                    stunWhy="with three temporary Risen 22 blocks behind ten Squires the group's centre ("+(groupCentre==null?"none":fmt(groupCentre.x-site.getX()-.5)+", "+fmt(groupCentre.z-site.getZ()-.5))+") is the centre Morale counts ("+fmt(moraleCentre.x-site.getX()-.5)+", "+fmt(moraleCentre.z-site.getZ()-.5)+") of its "+members.size()+" members (want 10: temporaries share the group's orders, not its numbers)="+centres+"; ";
                    stunOk=centres;b.issue(own,"infantry","retreat","block",new Vec3d(site.getX()+.5-20,site.getY(),site.getZ()+.5),-90,-1);t0=b.time;next(b,49);
                }
                case 49->{ // 20 ticks after the Retreat order every troop of the group, a temporary one too, has dropped its target and its spell and takes the order; nothing changes it on its own
                    if(since(b)<20)return;
                    boolean temporary=true;for(var tmp:temps)temporary&=tmp.lockedTarget==null&&tmp.spellCenter==null&&tmp.mob.getTarget()==null&&b.assignment(tmp).order.equals("retreat");
                    boolean regular=group.get(0).lockedTarget==null&&group.get(0).spellCenter==null&&group.get(0).mob.getTarget()==null&&b.assignment(group.get(0)).order.equals("retreat");
                    stunOk&=temporary&&regular;
                    report("p2-temporary-morale",stunOk,stunWhy+"20 ticks after the Retreat order the three temporary Risen have no target, no spell and the retreat order="+temporary+" (a Squire: "+regular+")");
                    group.clear();temps.clear();clearFixture(b,"temporary morale");reset(own);Houses.recompute(own);next(b,50);
                }
                case 50->{ // p2-grave-bloom: the Warlock on the platform; eight troops of both sides fall around a point 12 blocks ahead of him, and a stale entry sits in the log
                    var p=sp(b);if(p==null)return;reset(own);Commander.reset(b,own);own.order="hold";own.assignments.clear();
                    stand(b,p,-90f);
                    boolean onFloor=Math.abs(aim.x-(site.getX()+.5+2))<1.2&&Math.abs(aim.z-(site.getZ()+.5))<1.2&&Math.abs(aim.y-site.getY())<.5;
                    post(b,own,"revenant",-9,6,-90,true);post(b,own,"revenant",-9,7.5,-90,true);Houses.recompute(own); // two troops of his own that stay: the slot count has something to count
                    b.deaths.clear();b.deaths.add(new GravesAndGlory.Death(aim,b.time-401,"revenant",1));int staleBefore=b.deaths.size();
                    double[] distances={0,1.2,2.4,3.6,4.8,6.0,7.2,9.5};double[] degrees={20,100,190,260,330,60,140,180};
                    for(int i=0;i<8;i++)corpse(b,i%2==0?dusk:dawn,i%2==0?"revenant":"knight",distances[i],degrees[i]);
                    boolean pruned=b.deaths.size()==8&&b.deaths.stream().allMatch(d->b.time-d.time()<=400);
                    audit(b,"corpses",false);
                    bloomWhy="the commander looks 12 blocks ahead at the floor and the raycast lands at "+fmt(aim.x-site.getX()-.5)+", "+fmt(aim.z-site.getZ()-.5)+" from the platform's centre (want 2, 0)="+onFloor+"; a log entry 401 ticks old was pruned by the next death, and eight fell (seven inside 8 blocks of the aim, one 9.5 off): the log holds "+b.deaths.size()+" ("+staleBefore+" before them)="+pruned+"; ";
                    bloomOk=onFloor&&pruned;
                    // first a cast at nothing: looking west off the platform
                    var c=Commander.row(own);stand(b,p,90f);final Vec3d westAim=aim;float westYaw=p.getYaw();long nearWest=b.deaths.stream().filter(d->distance(d.at(),westAim)<=c.radius()).count();long ready=own.abilityReadyAt;int logBefore=b.deaths.size();
                    b.campaign.notices.remove(player);b.command(p,"ability");
                    boolean refused=own.abilityUntil==0&&own.abilityReadyAt==ready&&temporaries(b)==0&&b.deaths.size()==logBefore&&"No fallen troops there.".equals(b.campaign.notices.get(player));
                    bloomOk&=refused;bloomWhy+="a cast with no fallen troop within 8 of where he looks (west, off the platform: he looks at "+fmt(westAim.x-site.getX()-.5)+", "+fmt(westAim.z-site.getZ()-.5)+" from the centre, yaw "+fmt(westYaw)+", "+nearWest+" log entries within 8 of it) was refused with the notice \""+b.campaign.notices.get(player)+"\", no cooldown, no troop and no log change="+refused+"; ";
                    stand(b,p,-90f);next(b,51);
                }
                case 51->{ // the cast
                    var p=sp(b);p.sendMessage(net.minecraft.text.Text.empty(),true);var c=Commander.row(own);t0=b.time;soulsBefore=BattleEffects.souls;int fieldedBefore=(int)b.fielded(own).count();
                    b.command(p,"ability");
                    var made=b.units.stream().filter(u->u.temporary&&u.squad==own).toList();bloom.clear();bloom.addAll(made);
                    boolean row=c.id().equals("warlock")&&c.active().equals("grave_bloom")&&c.cooldownTicks()==2400&&c.durationTicks()==600&&near(c.radius(),8)&&near(c.range(),30)&&near(c.value1(),0.6)&&near(c.value2(),6)&&near(c.value3(),400);
                    boolean timing=own.abilityUntil==t0+c.durationTicks()&&own.abilityReadyAt==t0+c.cooldownTicks()&&Commander.readyIn(b,own)==2400&&Commander.running(b,own,"grave_bloom");
                    boolean six=made.size()==6&&made.stream().map(u->u.slot).distinct().count()==6&&made.stream().allMatch(u->u.slot>=GravesAndGlory.R.squadSize());
                    boolean shape=made.stream().allMatch(u->u.spec.id().equals("revenant")&&u.mob instanceof GravesAndGlory.ArmyZombie&&u.expiresAt==t0+600&&Math.abs(u.mob.getHealth()-0.6*u.mob.getMaxHealth())<0.01&&u.mob.isAlive());
                    var names=sideMembers(b);boolean teams=made.stream().allMatch(u->names.stream().anyMatch(m->m.startsWith(b.side(own.team).id()+":")&&m.endsWith(u.mob.getEntityName())));
                    boolean where=true;StringBuilder spots=new StringBuilder();for(var u:made){double best=1e9;for(var k:corpses)if(distance(u.mob.getPos(),k.mob.getPos())<best)best=distance(u.mob.getPos(),k.mob.getPos());where&=best<1.0;spots.append(fmt(distance(u.mob.getPos(),aim))).append(' ');}
                    boolean counted=b.fielded(own).count()==fieldedBefore&&fieldedBefore==2&&!own.traits.containsKey("graveborn");
                    deathsAfter=b.deaths.size();boolean log=deathsAfter==2&&b.deaths.stream().noneMatch(d->distance(d.at(),aim)<=c.radius()&&distance(d.at(),aim)<6.5);
                    boolean souls=BattleEffects.souls>=soulsBefore+6;
                    audit(b,"bloom raise",false);
                    for(var u:made){u.mob.setAiDisabled(true);fixture.add(u);pins.add(new Pin(u,u.mob.getPos(),-90));}
                    bloomOk&=row&&timing&&six&&shape&&teams&&where&&counted&&log&&souls;
                    bloomWhy+="the cast: Warlock row "+c.id()+"/"+c.active()+" (cooldown "+c.cooldownTicks()+", lasts "+c.durationTicks()+", radius "+c.radius()+", range "+c.range()+", "+c.value1()+" of health, up to "+c.value2()+", log "+c.value3()+" ticks)="+row+"; runs to +"+(own.abilityUntil-t0)+", ready again at +"+(own.abilityReadyAt-t0)+"="+timing+"; "+made.size()+" temporary Risen stand in his army (slots "+made.stream().map(u->u.slot).sorted().toList()+", want six, all 20 or higher)="+six+", each a revenant in an ArmyZombie, gone at +"+(made.isEmpty()?"-":String.valueOf(made.get(0).expiresAt-t0))+" (want 600), at 60% of its health="+shape+", on its side's team="+teams+", where a corpse lay (distances from the aim "+spots+")="+where+"; his army still counts "+b.fielded(own).count()+" troops for its slots (want 2) and no Graveborn tier "+own.traits+"="+counted+"; the log kept the two not raised ("+deathsAfter+")="+log+"; soul effects +"+(BattleEffects.souls-soulsBefore)+"="+souls+"; ";
                    requestShot("graveborn-bloom",12);next(b,52);
                }
                case 52->{ // 600 ticks: they stand to the end and are gone with it
                    long t=b.time-t0;
                    if(t==300)audit(b,"bloom midway",false);
                    if(t==598){long alive=bloom.stream().filter(u->u.mob.isAlive()&&!u.mob.isRemoved()).count();boolean stand=alive==6&&temporaries(b)==6;bloomOk&=stand;bloomWhy+="at +598 "+alive+" of them still stand="+stand+"; ";}
                    if(t>=602){
                        long left=bloom.stream().filter(u->u.mob.isAlive()&&!u.mob.isRemoved()).count();audit(b,"bloom expiry",false);
                        boolean gone=left==0&&temporaries(b)==0&&bloom.stream().noneMatch(u->b.units.contains(u))&&own.abilityUntil==0;
                        bloomOk&=gone&&shotTaken;
                        report("p2-grave-bloom",bloomOk,bloomWhy+"at +"+t+" "+left+" are left and "+temporaries(b)+" temporary troops stand in the battle, his active has ended="+(own.abilityUntil==0)+" (all gone="+gone+"); the picture graveborn-bloom was taken="+shotTaken);
                        wantShot=null;next(b,53);
                    }
                }
                case 53->{ // p2-bloom-cleanup: two temporary Risen, one risen Graveborn and a real loss; then the round ends
                    var p=sp(b);if(p==null)return;clearFixture(b,"bloom");reset(own);Commander.reset(b,own);Houses.forceRise=Boolean.FALSE;
                    for(int i=0;i<6;i++)group.add(post(b,own,"revenant",-9+i*1.5,-6,-90,true));Houses.recompute(own); // six Risen of his own: tier I, so one of them can rise
                    stand(b,p,-90f);corpse(b,dusk,"revenant",0,0);corpse(b,dusk,"knight",2,90);
                    var scout=post(b,dusk,"knight",9,9,180,true);
                    b.command(p,"ability");
                    var temps=b.units.stream().filter(u->u.temporary).toList();bloom.clear();bloom.addAll(temps);
                    Houses.forceRise=Boolean.TRUE;var lucky=group.get(0);final Vec3d luckyAt=lucky.mob.getPos();lucky.mob.setHealth(1f);strike(b,scout,lucky,500f);boolean rose=lucky.mob.isAlive()&&lucky.rose&&!lucky.temporary;
                    Houses.forceRise=Boolean.FALSE;var lost=group.get(1);final Vec3d lostAt=lost.mob.getPos();lost.mob.setHealth(1f);strike(b,scout,lost,500f);
                    var spent=temps.isEmpty()?null:temps.get(0);if(spent!=null)fall(b,spent);
                    int standing=(int)b.units.stream().filter(u->u.temporary&&u.mob.isAlive()).count();
                    var row=own.stats.lost;var report=b.campaign.reportSquads(own);int reported=-1;for(var e:report)if(e.getAsJsonObject().get("you").getAsBoolean())reported=e.getAsJsonObject().get("lost").getAsInt();
                    int loggedBefore=b.deaths.size();var entry=loggedBefore==1?b.deaths.get(0):null; // exactly one entry: the real loss, where it fell; none for the Risen that rose, the temporary one that crumbled, or the two Bloom raised
                    boolean logged=entry!=null&&entry.team()==own.team&&entry.troop().equals("revenant")&&entry.time()==b.time&&distance(entry.at(),lostAt)<1&&distance(entry.at(),luckyAt)>1;
                    boolean before=temps.size()==2&&rose&&own.stats.lost==1&&reported==1&&standing==1&&logged;
                    audit(b,"cleanup setup",false);
                    // the round ends
                    List<GravesAndGlory.Unit> everyone=new ArrayList<>(b.units);
                    b.campaign.finish(-1);
                    var result=b.campaign.results.get(player);int lostInReport=-1;if(result!=null&&result.has("squads"))for(var e:result.getAsJsonArray("squads"))if(e.getAsJsonObject().get("you").getAsBoolean())lostInReport=e.getAsJsonObject().get("lost").getAsInt();
                    boolean allGone=!b.running&&b.units.isEmpty()&&temporaries(b)==0&&everyone.stream().allMatch(u->u.mob.isRemoved())&&b.deaths.isEmpty();
                    audit(b,"after finish",true);boolean noTeams=sideMembers(b).isEmpty();
                    report("p2-bloom-cleanup",before&&allGone&&noTeams&&lostInReport==1,"before the round ends: two temporary Risen were raised ("+temps.size()+"), a Risen of his own rose="+rose+", one real loss and one temporary one fell: the army's lost is "+own.stats.lost+" and the report's row says "+reported+" (want 1), "+standing+" temporary troop still stands (want 1), the death log held "+loggedBefore+(entry==null?" entries":" entry ("+entry.troop()+" of team "+entry.team()+", "+fmt(distance(entry.at(),lostAt))+" from where the real loss fell and "+fmt(distance(entry.at(),luckyAt))+" from the Risen that rose)")+" (want exactly one, the real loss)="+logged+", all of it together="+before+"; after campaign.finish(-1): "+everyone.size()+" troops were in the battle and now the battle holds "+b.units.size()+" ("+temporaries(b)+" temporary), every one of their entities is removed="+everyone.stream().allMatch(u->u.mob.isRemoved())+", the death log holds "+b.deaths.size()+" (want 0), the round is over="+!b.running+", the final report says lost "+lostInReport+" (want 1), the four gg_ teams hold "+sideMembers(b).size()+" members="+noTeams);
                    fixture.clear();pins.clear();group.clear();corpses.clear();bloom.clear();next(b,54);
                }
                case 54->{ // p2-bloom-cleanup-paths, the map switch: in the lobby, a temporary Risen is loose and a death is logged; the arena changes
                    if(b.running)return;var p=sp(b);if(p==null)return;
                    if(site!=null){platform(b,site,12,Blocks.AIR.getDefaultState());unforce(b);}
                    lobbySpot=p.getPos();toMap=b.campaign.map==1?2:1;
                    loose=b.spawn(own,Campaign.troop("revenant"),20,lobbySpot);loose.temporary=true;loose.expiresAt=b.time+1000;Houses.recompute(own);b.deaths.add(new GravesAndGlory.Death(lobbySpot,b.time,"revenant",1));
                    boolean had=temporaries(b)==1&&b.deaths.size()==1;
                    b.campaign.switchMap(toMap);
                    pathsOk=had&&temporaries(b)==0&&b.deaths.isEmpty()&&loose.mob.isRemoved()&&!b.units.contains(loose);
                    pathsWhy="switchMap to map "+toMap+" with one temporary Risen loose and one death logged: "+temporaries(b)+" temporary troops and "+b.deaths.size()+" deaths after it, the loose one's entity is removed="+loose.mob.isRemoved()+" (had both before="+had+")="+pathsOk+"; ";
                    audit(b,"switchMap",false);inserted=false;next(b,55);
                }
                case 55->{ // the round start: the same two again, then a new round begins
                    var p=sp(b);if(p==null||!b.ready)return;
                    if(!inserted){loose=b.spawn(own,Campaign.troop("revenant"),20,p.getPos());loose.temporary=true;loose.expiresAt=b.time+1000;Houses.recompute(own);b.deaths.add(new GravesAndGlory.Death(p.getPos(),b.time,"revenant",1));inserted=true;}
                    if(!Phase1Checks.skirmish(b,p,toMap,1,"normal"))return;
                    boolean started=b.running&&temporaries(b)==0&&b.deaths.isEmpty()&&loose.mob.isRemoved();
                    pathsWhy+="startRound with the same two: "+temporaries(b)+" temporary troops and "+b.deaths.size()+" deaths after it, the loose one's entity is removed="+loose.mob.isRemoved()+"="+started;
                    audit(b,"startRound",false);
                    report("p2-bloom-cleanup-paths",pathsOk&&started,pathsWhy);next(b,60);
                }
                case 60->{ // p2-graveborn-tick-cost: eight bot armies under the hard Graveborn banner, Rise Again rolling for real
                    var p=sp(b);if(p==null)return;Houses.forceRise=null;
                    if(cost==null)cost=tickCost(b,List.of("graveborn_hard","graveborn_hard","graveborn_hard","graveborn_hard","graveborn_hard","graveborn_hard","graveborn_hard","graveborn_hard"));
                    if(cost.step(p)){
                        report("p2-graveborn-tick-cost",cost.last<40,"server tick "+String.format(Locale.ROOT,"%.2f",cost.last)+" ms (600-tick mean "+String.format(Locale.ROOT,"%.2f",cost.avg)+" ms) with "+cost.troops+" troops at the start and "+b.units.size()+" at the end, "+b.deaths.size()+" deaths in the log, "+temporaries(b)+" temporary troops ("+cost.note+")");
                        audit(b,"before the final returnLobby",false);
                        report("p2-graveborn-teams-clean",audits>=14&&staleSeen.isEmpty(),audits+" audits after the suite's raisings, deaths and removals (stageFixture and the end of the round expect every team empty): "+(staleSeen.isEmpty()?"no gg_ team ever held a member that was not a living troop":"STALE "+staleSeen)+"; "+auditNotes);
                        cost.handBack();b.campaign.returnLobby();next(b,99);
                    }
                }
                default->{}
            }
        }
    }

    /**
     * Task 8 (batch 3): the Ironclad mechanics. The three vanilla bodies (goals, raids, the crossbow pose), Sunder and Armour Bolt against a
     * raised shield, the Ballista's pierce, the Mortar's minimum range (at a cast, and walking in the field), Brassjaw's slam, Entrench
     * (still, on an owned point, by tier, in range), the siege counters against a supply banner and large and small targets, the
     * Quartermaster's Forward Supply (placed, deploying there, refused near base, lost to an enemy, an enemy near or its timer), the golem's
     * swing, the room a body takes (formation and bridge lanes), Brassjaw across the river on every bridge of map 0, and the tick cost. The
     * fixture is Graveborn's: a stone platform in the sky 40 blocks east of Dawn's base, two ad hoc armies and the commander's own (a
     * Quartermaster); troops are posted frozen and pinned. The field stages (the Mortar walking, the bridges) run on real ground.
     */
    static final class Ironclad extends Staged {
        BlockPos mainSite,pointSite;
        final List<GravesAndGlory.Unit> shielded=new ArrayList<>();final List<BlockPos> platforms=new ArrayList<>();
        boolean raise;
        GravesAndGlory.Capture owned;int ownedWas=-1,ownedProgress,ownedChallenger=-1;long ownedSince;
        // scratch the stages share
        GravesAndGlory.Unit u1,u2,u3,u4,u5,u6;final GravesAndGlory.Unit[] us=new GravesAndGlory.Unit[8],line=new GravesAndGlory.Unit[7];
        Projectiles.Shot shot;Combat.HitLog mark;GravesAndGlory.ArmySupplyBanner banner;
        String why="",mobsWhy="",minWhy="",entrenchWhy="";boolean ok,mobsOk,minOk,entrenchOk;
        // the live Arbalist
        int charging,raiding;float foeHp,foeHp2;
        // the field stages
        double gx,gy,gz,castDist,shotDist,retreatMax,startDist;int castsInside;boolean casting=false;long castTime;Vec3d holdAt;int bridgeIdx;boolean bridgesOk=true;final StringBuilder bridgeWhy=new StringBuilder();long crossedAt;
        // the golem's swing, seen on the client
        volatile int golemId=-1,swingSeen;volatile boolean watchSwing;int statuses0;
        // the supply stages
        final List<String> supplyWhy=new ArrayList<>();boolean supplyOk;BlockPos bannerBlock;int variant;Set<Integer> seen=new HashSet<>();
        TickCost cost;
        // fix round 1: the siege-hold and mortar-hold stages, the wall, the golem's crossing, the fall of a banner
        final List<BlockPos> wall=new ArrayList<>();
        double deckMin,drift;boolean wet,holdOk,siegeOk;int deckTicks,casts;String holdWhy="",siegeNote="",fallNotice="";long fallUntil=-1,fallReady=-1;

        static boolean close(double a,double b){return Math.abs(a-b)<1e-3;}
        static Vec3d base0(){return GravesAndGlory.Battle.base(0);}
        Vec3d centre(){return new Vec3d(site.getX()+.5,site.getY(),site.getZ()+.5);}
        /** The standing spot on real ground at (x, z). */
        static Vec3d ground(GravesAndGlory.Battle b,double x,double z){return new Vec3d(x,Phase1Checks.top(b,x,z),z);}
        /** A troop with its AI on (Tactics moves and swings it), on the platform at (dx, dz). */
        GravesAndGlory.Unit live(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);
            var u=b.spawn(s,Campaign.troop(troop),30+fixture.size(),at);u.mob.refreshPositionAndAngles(at.x,at.y,at.z,yaw,0);fixture.add(u);u.deployed=at;return u;
        }
        void unpin(GravesAndGlory.Unit u){pins.removeIf(p->p.u()==u);}
        /** A troop that cannot be killed by the stage in hand. */
        static GravesAndGlory.Unit tough(GravesAndGlory.Unit u){u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(4000);u.mob.setHealth(4000f);return u;}
        @Override void cleared(){shielded.clear();raise=false;Arrays.fill(us,null);Arrays.fill(line,null);}
        @Override void staged(){mainSite=site;platforms.add(site);}
        /** After each server tick: the pinned troops stand where the stage put them, and the troops in `shielded` hold their shields as `raise` says. */
        @Override void hold(){
            super.hold();
            for(var u:shielded)if(u.mob.isAlive()){if(raise&&!u.mob.isUsingItem())u.mob.setCurrentHand(net.minecraft.util.Hand.OFF_HAND);else if(!raise&&u.mob.isUsingItem())u.mob.stopUsingItem();}
        }
        /** A level, open 36 x 17 patch of real ground west of the river (no step over a block, no water): its west end (x, standing y, z), or null. */
        double[] flat(GravesAndGlory.Battle b){
            for(int x=-240;x<=-40;x+=6)for(int z=-100;z<=100;z+=6){
                int y0=(int)Phase1Checks.top(b,x,z);boolean open=true;
                for(int dx=-4;dx<=32&&open;dx+=2)for(int dz=-8;dz<=8&&open;dz+=2){
                    int y=(int)Phase1Checks.top(b,x+dx,z+dz);
                    open=!Arena.river(x+dx,z+dz)&&Math.abs(y-y0)<=1&&b.world.getBlockState(new BlockPos(x+dx,y-1,z+dz)).isSolidBlock(b.world,new BlockPos(x+dx,y-1,z+dz));
                }
                if(open)return new double[]{x,y0,z};
            }
            return null;
        }
        /** Puts the commander on the platform (centre or the west edge), cancels the cooldown and plants the banner; null when it was refused. */
        GravesAndGlory.ArmySupplyBanner plant(GravesAndGlory.Battle b,ServerPlayerEntity p,double dx){return plant(b,p,dx,0);}
        GravesAndGlory.ArmySupplyBanner plant(GravesAndGlory.Battle b,ServerPlayerEntity p,double dx,double dz){
            Commander.reset(b,own);p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
            b.relocate(p,site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz,-90f,0f);b.campaign.notices.remove(player);b.command(p,"ability");
            return b.structures.isEmpty()?null:b.structures.get(b.structures.size()-1);
        }
        /** A bare, tough knight of Dusk, frozen and pinned on real ground at `at`. */
        GravesAndGlory.Unit foe(GravesAndGlory.Battle b,Vec3d at,int slot){
            var f=b.spawn(dusk,Campaign.troop("knight"),slot,at);f.mob.setAiDisabled(true);Phase1Checks.freeze(f);tough(bare(f));fixture.add(f);pins.add(new Pin(f,at,90f));f.deployed=at;return f;
        }
        /** The order point of an army that puts the formation spot of its troop where the troop stands (a bot army's troop with nothing to shoot walks to that spot). */
        void standAt(GravesAndGlory.Battle b,GravesAndGlory.Squad s,GravesAndGlory.Unit u,Vec3d at,float yaw){
            s.yaw=yaw;s.target=at;Vec3d off=b.formation(u).subtract(at);s.target=at.subtract(off);
        }
        /** A stone wall one block thick in block column x from block row z0 to z1, from the ground up five blocks. */
        void wallUp(GravesAndGlory.Battle b,double x,double z0,double z1){
            int bx=(int)Math.floor(x);
            for(int z=(int)Math.floor(z0);z<=(int)Math.floor(z1);z++){int y0=(int)Phase1Checks.top(b,bx+.5,z+.5);for(int y=0;y<5;y++){var at=new BlockPos(bx,y0+y,z);b.world.setBlockState(at,Blocks.STONE.getDefaultState());wall.add(at);}}
        }
        void wallDown(GravesAndGlory.Battle b){for(var at:wall)b.world.setBlockState(at,Blocks.AIR.getDefaultState());wall.clear();}
        /** A tier I Ironclad Ballista Crew on Hold at its post on the flat ground, a pinned knight 36 blocks east of it (past its 34, inside the 37.4 of a Ballista that has stood still), a stone wall between them when `walled`. */
        void siegeHold(GravesAndGlory.Battle b,boolean walled){
            dawn.order="hold";dusk.order="hold";
            Vec3d post=ground(b,gx-3.5,gz+.5);
            us[0]=foe(b,ground(b,gx+32.5,gz+.5),40);
            u1=b.spawn(dawn,Campaign.troop("ic_ballista"),32,post);u1.mob.refreshPositionAndAngles(post.x,post.y,post.z,-90f,0f);fixture.add(u1);u1.deployed=post;
            dawn.traits.put("ironclad",1); // after the spawn, which counts the army's Houses again
            standAt(b,dawn,u1,post,-90f);
            if(walled)wallUp(b,gx+14.5,gz-6,gz+7);
            holdAt=post;drift=0;shotDist=-1;foeHp=us[0].mob.getHealth();
        }
        /** Each tick of a siege-hold stage: how far it strayed from its post and where its first bolt was loosed; true after 200 ticks, with siegeOk and siegeNote set. */
        boolean siegeWatch(GravesAndGlory.Battle b){
            drift=Math.max(drift,distance(u1.mob.getPos(),holdAt));
            if(shotDist<0&&b.shots.stream().anyMatch(sh->sh.shooter()==u1))shotDist=distance(u1.mob.getPos(),us[0].mob.getPos());
            if(since(b)<200)return false;
            boolean walled=!wall.isEmpty(),entrenched=Houses.still(u1)&&Math.abs(Abilities.range(b,u1)-37.4)<1e-6,hit=us[0].mob.getHealth()<foeHp;
            boolean fired=walled?shotDist<0&&!hit:shotDist>34.0&&hit;
            siegeOk=drift<0.6&&entrenched&&fired;
            siegeNote="over 200 ticks on Hold the Ballista Crew strayed at most "+fmt(drift)+" from its post (want under 0.6: it never walks to or from a target), it stood entrenched (still="+Houses.still(u1)+", range "+fmt(Abilities.range(b,u1))+", want 37.4), "+(walled?"it loosed "+(shotDist<0?"no bolt":"a bolt from "+fmt(shotDist))+" at the knight it cannot see and the knight lost "+fmt(foeHp-us[0].mob.getHealth())+" (want no bolt, nothing lost)":"its first bolt left "+(shotDist<0?"never":fmt(shotDist))+" blocks from the knight (want past 34, the range without Entrench) and the knight lost "+fmt(foeHp-us[0].mob.getHealth())+" (want some)")+"="+siegeOk;
            return true;
        }

        public void cleanup(MinecraftClient c){
            server(c,b->{Abilities.clearBanners(b);wallDown(b);clearFixture(b,"cleanup");for(var s:platforms)platform(b,s,12,Blocks.AIR.getDefaultState());platforms.clear();unforce(b);
                if(owned!=null){owned.owner=ownedWas;owned.progress=ownedProgress;owned.challenger=ownedChallenger;owned.ownedSince=ownedSince;}
                if(cost!=null)cost.handBack();
                if(b.running)b.campaign.returnLobby();var p=sp(b);if(p!=null&&classBefore!=null)b.campaign.handle(p,"class:"+classBefore);});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(watchSwing&&golemId>=0&&c.world!=null&&c.world.getEntityById(golemId) instanceof net.minecraft.entity.passive.IronGolemEntity g)swingSeen=Math.max(swingSeen,g.getAttackTicksLeft());
            if(wantShot!=null&&!shotTaken&&++shotDelay>=shotAfter){shot(c,wantShot);shotTaken=true;}
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"ironclad");}
            if(tick>16000&&!finished){finished=true;report("p2-ironclad-timeout",false,"stuck in stage "+stage);finish(c,"ironclad");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(player==null||stage>=99)return;
            try{stages(b);}catch(RuntimeException e){report("p2-ironclad-exception",false,"stage "+stage+": "+e);e.printStackTrace();next(b,99);}
        }
        void stages(GravesAndGlory.Battle b){
            hold();
            switch(stage){
                case 1->{ // p2-ironclad-mobs, the sheet and the classes: the entities the sheet names are the ones registered, and they carry only a SwimGoal
                    var w=b.world;boolean good2=true;StringBuilder s=new StringBuilder();var armor=net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR;
                    Map<String,EntityType<?>> types=Map.<String,EntityType<?>>of("vindicator",EntityType.VINDICATOR,"pillager",EntityType.PILLAGER,"iron_golem",EntityType.IRON_GOLEM);
                    Map<String,Class<?>> classes=Map.<String,Class<?>>of("vindicator",GravesAndGlory.ArmyVindicator.class,"pillager",GravesAndGlory.ArmyPillager.class,"iron_golem",GravesAndGlory.ArmyGolem.class);
                    for(var line:List.of("ic_hearthguard:vindicator","ic_breaker:vindicator","ic_arbalist:pillager","ic_ballista:pillager","ic_mortar:pillager","ic_colossus:iron_golem")){
                        String troop=line.substring(0,line.indexOf(':')),entity=line.substring(line.indexOf(':')+1);var row=Campaign.troop(troop);
                        boolean sheet=row!=null&&row.entity().equals(entity);MobEntity m=null;String goals,targets,made;
                        try{m=ArmyMobs.create(row.entity(),w);made=m.getClass().getSimpleName();goals=Engine.goalNames(m,"goalSelector").toString();targets=Engine.goalNames(m,"targetSelector").toString();}
                        catch(ReflectiveOperationException|RuntimeException e){goals=targets="unreadable";made=e.toString();}
                        boolean raider=!(m instanceof net.minecraft.entity.raid.RaiderEntity r)||r.getRaid()==null&&!r.isCelebrating();
                        boolean good=sheet&&m!=null&&classes.get(entity).isInstance(m)&&ArmyMobs.isTroop(m)&&m.getType()==types.get(entity)&&goals.equals("[SwimGoal]")&&targets.equals("[]")&&m.getAttributeBaseValue(armor)==0&&raider;
                        good2&=good;s.append(troop).append(" (sheet entity ").append(row==null?"-":row.entity()).append(", want ").append(entity).append(") -> ").append(made).append(", goals ").append(goals).append(", targets ").append(targets).append(", no raid or celebration=").append(raider).append(good?"":" (BAD)").append("; ");
                    }
                    mobsOk=good2;mobsWhy=s.toString();next(b,4);
                }
                case 4->{ // the commander is a Quartermaster for the rest of the suite
                    var p=sp(b);if(p==null)return;if(b.running){b.campaign.returnLobby();return;}
                    classBefore=profile(b).commander;b.campaign.handle(p,"class:quartermaster");
                    if(!profile(b).commander.equals("quartermaster")){report("p2-ironclad-class",false,"class:quartermaster never reached the profile (it says "+profile(b).commander+")");next(b,99);return;}
                    next(b,5);
                }
                case 5->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,6);}
                case 6->{if(since(b)<10)return;stageFixture(b);next(b,10);}
                case 10->{ // the Arbalist, the Ballista Crew and the Mortar with their AI on, shooting a pinned bare knight; a Hearthguard, a Shieldbreaker and Brassjaw stand by
                    dawn.order="attack";charging=0;raiding=0;
                    us[0]=tough(bare(post(b,dusk,"knight",4,0,180,true)));foeHp=us[0].mob.getHealth();dawn.target=us[0].mob.getPos();
                    u1=live(b,dawn,"ic_arbalist",-8,0,-90);u2=live(b,dawn,"ic_ballista",-8,-5,-90);u3=live(b,dawn,"ic_mortar",-8,5,-90);
                    post(b,dawn,"ic_hearthguard",-2,-8,-90,true);post(b,dawn,"ic_breaker",-2,8,-90,true);post(b,dawn,"ic_colossus",-4,10,-90,true);
                    next(b,11);
                }
                case 11->{ // 140 ticks: the pillagers never take the crossbow-charging pose, nothing raids or celebrates, and the shots land
                    for(var u:fixture){
                        if(u.mob instanceof net.minecraft.entity.mob.PillagerEntity pl&&pl.isCharging())charging++;
                        if(u.mob instanceof net.minecraft.entity.raid.RaiderEntity r&&(r.getRaid()!=null||r.isCelebrating()))raiding++;
                    }
                    if(since(b)<140)return;
                    int pillagers=(int)fixture.stream().filter(u->u.mob instanceof net.minecraft.entity.mob.PillagerEntity).count();
                    boolean hit=us[0].mob.getHealth()<foeHp;
                    report("p2-ironclad-mobs",mobsOk&&charging==0&&raiding==0&&hit&&pillagers==3,mobsWhy+"over 140 ticks with "+pillagers+" pillagers (an Arbalist, a Ballista Crew and a Mortar, AI on) shooting a pinned knight: the crossbow-charging pose was taken "+charging+" times (want 0), a raid or a celebration was seen "+raiding+" times (want 0), the knight lost "+fmt(foeHp-us[0].mob.getHealth())+" health (want some)="+hit);
                    clearFixture(b,"mobs");next(b,12);
                }
                case 12->{ // p2-sunder and p2-armour-bolt: a raised Templar facing them
                    dawn.order="hold";
                    u1=post(b,dusk,"sentinel",0,0,180,true);shielded.add(u1);raise=true;
                    u2=post(b,dawn,"ic_breaker",0,-2.5,0,true);u3=post(b,dawn,"ic_hearthguard",1.5,-2.5,0,true);
                    u4=post(b,dawn,"ic_arbalist",0,-10,0,true);u5=post(b,dawn,"archer",-1.5,-10,0,true);
                    u6=bare(post(b,dusk,"revenant",-6,0,180,true));us[0]=post(b,dawn,"ic_breaker",-6,-2.5,0,true);us[1]=post(b,dawn,"ic_arbalist",-6,-10,0,true);
                    next(b,13);
                }
                case 13->{if(since(b)<10)return; // the shield has been up ten ticks
                    boolean raised=u1.mob.isUsingItem();
                    var sunder=Commanders.hit(b,u2,u1.mob,"melee");var plain=Commanders.hit(b,u3,u1.mob,"melee");var open=Commanders.hit(b,us[0],u6.mob,"melee");
                    var bolt=Commanders.hit(b,u4,u1.mob,"arrow");var archer=Commanders.hit(b,u5,u1.mob,"arrow");var boltOpen=Commanders.hit(b,us[1],u6.mob,"arrow");
                    boolean tags=u2.tags.contains("sunder")&&!u2.tags.contains("shield")&&u4.tags.contains("armour_bolt")&&u1.tags.contains("shield");
                    boolean s=raised&&tags&&Gilded.is(sunder,1.4,"C14")&&Gilded.is(plain,0.6,"C3")&&open!=null&&near(open.counters(),1.0)&&open.rows().isEmpty();
                    boolean a=raised&&Gilded.is(bolt,0.7,"C15")&&archer!=null&&archer.rows().contains("C1")&&archer.counters()<bolt.counters()&&boltOpen!=null&&near(boltOpen.counters(),1.0)&&boltOpen.rows().isEmpty();
                    report("p2-sunder",s,"against a raised Templar (raised="+raised+", tags "+u1.tags+") from the front: a Shieldbreaker (tags "+u2.tags+") "+show(sunder)+" (want 1.4, C14); a Hearthguard "+show(plain)+" (want the shield's 0.6, C3); the Shieldbreaker on a Risen with no shield "+show(open)+" (want 1.0, no row)="+s);
                    report("p2-armour-bolt",a,"against the same raised Templar: an Arbalist's bolt (tags "+u4.tags+") "+show(bolt)+" (want 0.7, C15); an Order Archer's arrow "+show(archer)+" (the shield's C1, which cuts it further than the bolt's 0.7); the Arbalist on the Risen "+show(boltOpen)+" (want 1.0, no row)="+a);
                    clearFixture(b,"sunder");next(b,14);
                }
                case 14->{ // p2-ballista-pierce: a Ballista Crew shoots a bare knight; a second 2.5 behind it, a third 3.6 behind (not struck: only one), one 2.5 in front and one 6.5 behind
                    u1=post(b,dawn,"ic_ballista",0,-10,0,true);
                    us[0]=bare(post(b,dusk,"knight",0,0,180,true));us[1]=bare(post(b,dusk,"knight",0.4,2.5,180,true));us[2]=bare(post(b,dusk,"knight",-0.3,3.6,180,true));us[3]=bare(post(b,dusk,"knight",0,-2.5,180,true));us[4]=bare(post(b,dusk,"knight",0,6.5,180,true));
                    mark=Combat.LAST.peekLast();Projectiles.fire(b,u1,us[0].mob,"arrow",(float)Abilities.baseHit(b,u1));shot=b.shots.get(b.shots.size()-1);next(b,15);
                }
                case 15->{if(b.time<shot.lands()+2)return;
                    var row=Abilities.row(u1.spec);
                    Combat.HitLog first=Gilded.hitOn(us[0].mob,mark),second=Gilded.hitOn(us[1].mob,mark),third=Gilded.hitOn(us[2].mob,mark),front=Gilded.hitOn(us[3].mob,mark),far=Gilded.hitOn(us[4].mob,mark);
                    boolean sheet=row.id().equals("pierce_ballista")&&row.mechanic().equals("pierce")&&near(row.amount(),0.6)&&near(row.radius(),4)&&u1.tags.contains("siege");
                    boolean landed=first!=null&&second!=null&&Math.abs(second.base()-0.6*first.base())<1e-4&&near(second.total(),first.total())&&third==null&&front==null&&far==null;
                    boolean lost=us[1].mob.getHealth()<us[1].mob.getMaxHealth()&&us[2].mob.getHealth()==us[2].mob.getMaxHealth()&&us[3].mob.getHealth()==us[3].mob.getMaxHealth()&&us[4].mob.getHealth()==us[4].mob.getMaxHealth();
                    float l0=us[0].mob.getMaxHealth()-us[0].mob.getHealth(),l1=us[1].mob.getMaxHealth()-us[1].mob.getHealth();
                    boolean share=l0>0&&Math.abs(l1-0.6f*l0)<1e-3;
                    report("p2-ballista-pierce",sheet&&landed&&lost&&share,"the Ballista Crew's row "+row.id()+" ("+row.mechanic()+", amount "+row.amount()+", radius "+row.radius()+")="+sheet+"; its bolt hit the first knight "+show(first)+" (it lost "+fmt(l0)+"); the knight 2.5 behind it took "+show(second)+" (it lost "+fmt(l1)+", want 0.6 of that)="+share+"; nothing was logged on the knight 3.6 behind ("+(third==null?"none":"HIT")+"), 2.5 in front ("+(front==null?"none":"HIT")+") or 6.5 behind ("+(far==null?"none":"HIT")+")="+(third==null&&front==null&&far==null)+"; only the second took health="+lost);
                    clearFixture(b,"pierce");next(b,16);
                }
                case 16->{ // p2-mortar-minrange, at a cast: the Mortar 8 blocks from one knight and 19 from another; two bystanders by the far one
                    u1=post(b,dawn,"ic_mortar",0,-8,0,true);u1.abilityAt=0;
                    us[0]=bare(post(b,dusk,"knight",0,0,180,true));us[1]=bare(post(b,dusk,"knight",0,11,180,true));us[2]=bare(post(b,dusk,"knight",2.0,11,180,true));us[3]=bare(post(b,dusk,"knight",-4.2,11,180,true));
                    var row=Abilities.row(u1.spec);
                    boolean sheet=row.id().equals("shell")&&row.mechanic().equals("blight")&&near(row.minRange(),10)&&near(row.radius(),3.5)&&row.windupTicks()==30&&row.cooldownTicks()==100&&row.statusTicks()==0&&u1.tags.containsAll(List.of("siege","large"));
                    Abilities.attack(b,u1,us[0].mob,b.time);
                    boolean refused=u1.spellCenter==null&&u1.abilityAt==0&&b.shots.isEmpty();
                    boolean edge=Abilities.tooClose(u1,9.9*9.9)&&!Abilities.tooClose(u1,10.0*10.0)&&!Abilities.tooClose(u1,10.1*10.1);
                    t0=b.time;Abilities.attack(b,u1,us[1].mob,b.time);
                    boolean cast=u1.spellCenter!=null&&u1.windupUntil==t0+30&&u1.abilityAt==t0+100;casting=cast;castTime=0;
                    minOk=sheet&&refused&&edge&&cast;
                    minWhy="the Mortar's row "+row.id()+" ("+row.mechanic()+", minimum range "+row.minRange()+", radius "+row.radius()+", windup "+row.windupTicks()+", cooldown "+row.cooldownTicks()+", slow "+row.statusTicks()+" ticks)="+sheet+"; asked to shell a knight 8 blocks off it did nothing (no spell, no cooldown spent)="+refused+"; the line falls between 9.9 (too close) and 10.0 (allowed)="+edge+"; asked to shell one 19 off it began a spell with a windup of 30 and its next cast at +100="+cast+"; ";
                    next(b,17);
                }
                case 17->{ // the burst lands when the windup ends, hurts what stands within 3.5 and slows nobody
                    long t=b.time-t0;
                    if(casting&&u1.spellCenter!=null)Abilities.tick(b,u1,us[1].mob,b.time);
                    if(casting&&u1.spellCenter==null){casting=false;castTime=t;}
                    if(castTime>0||t>60){
                        float far=us[1].mob.getMaxHealth()-us[1].mob.getHealth(),in=us[2].mob.getMaxHealth()-us[2].mob.getHealth(),out=us[3].mob.getMaxHealth()-us[3].mob.getHealth();
                        boolean slow=us[1].mob.hasStatusEffect(StatusEffects.SLOWNESS)||us[2].mob.hasStatusEffect(StatusEffects.SLOWNESS);
                        boolean burst=castTime==30&&far>0&&in>0&&out==0&&!slow&&us[0].mob.getHealth()==us[0].mob.getMaxHealth();
                        minOk&=burst;minWhy+="the shell burst "+castTime+" ticks after the cast (want 30); the target lost "+fmt(far)+", the bystander 2.0 off "+fmt(in)+", the one 4.2 off "+fmt(out)+" (want some, some, 0), nobody slowed="+(!slow)+", the knight 8 blocks from the Mortar untouched="+(us[0].mob.getHealth()==us[0].mob.getMaxHealth())+"="+burst+"; ";
                        castTime=0;clearFixture(b,"mortar cast");next(b,20);
                    }
                }
                case 20->{ // p2-forge-slam: Brassjaw hits the first of five bare knights; three stand within 2.5 of it and one 3.2 off
                    u1=post(b,dawn,"ic_colossus",0,-3,0,true);
                    us[0]=bare(post(b,dusk,"knight",0,0,180,true));us[1]=bare(post(b,dusk,"knight",1.5,0,180,true));us[2]=bare(post(b,dusk,"knight",-1.8,0.3,180,true));us[3]=bare(post(b,dusk,"knight",0.4,2.0,180,true));us[4]=bare(post(b,dusk,"knight",3.2,0,180,true));
                    for(int i=1;i<=3;i++)unpin(us[i]); // free to be thrown; the one 3.2 off stays pinned
                    var row=Abilities.row(u1.spec);
                    boolean sheet=row.id().equals("forge_slam")&&row.mechanic().equals("slam")&&near(row.amount(),0.6)&&near(row.radius(),2.5)&&near(row.knockback(),0.8)&&!u1.tags.contains("fearless")&&u1.tags.contains("large");
                    float[] before=new float[5];for(int i=0;i<5;i++)before[i]=us[i].mob.getHealth();
                    mark=Combat.LAST.peekLast();float base=(float)Abilities.baseHit(b,u1);strike(b,u1,us[0],base);
                    float[] lost=new float[5];for(int i=0;i<5;i++)lost[i]=before[i]-us[i].mob.getHealth();
                    var onFirst=Gilded.hitOn(us[0].mob,mark);
                    boolean share=onFirst!=null&&lost[0]>0;StringBuilder s=new StringBuilder();
                    for(int i=1;i<=3;i++){
                        var log=Gilded.hitOn(us[i].mob,mark);int count=0;boolean after=mark==null;for(var h:Combat.LAST){if(h==mark){after=true;continue;}if(after&&h.target()==us[i].mob.getId())count++;}
                        Vec3d d=us[i].mob.getPos().subtract(us[0].mob.getPos()),v=us[i].mob.getVelocity();double speed=Math.hypot(v.x,v.z),dot=d.x*v.x+d.z*v.z;
                        boolean good=log!=null&&count==1&&Math.abs(log.base()-onFirst.base()*0.6)<1e-4&&Math.abs(lost[i]-0.6f*lost[0])<1e-3&&near(log.counters(),1.0)&&dot>0&&speed>0.5;
                        share&=good;s.append("knight ").append(i).append(" ").append(fmt(distance(us[i].mob.getPos(),us[0].mob.getPos()))).append(" off lost ").append(fmt(lost[i])).append(" (want ").append(fmt(0.6*lost[0])).append(") in ").append(count).append(" hit, thrown away at ").append(fmt(speed)).append(good?"":" (BAD)").append("; ");
                    }
                    boolean outside=Gilded.hitOn(us[4].mob,mark)==null&&lost[4]==0&&Math.hypot(us[4].mob.getVelocity().x,us[4].mob.getVelocity().z)<0.05;
                    report("p2-forge-slam",sheet&&share&&outside,"Brassjaw's row "+row.id()+" ("+row.mechanic()+", share "+row.amount()+", radius "+row.radius()+", knockback "+row.knockback()+"), tags "+u1.tags+" (large, and no fearless: the no-routing design)="+sheet+"; his blow hit the first knight "+show(onFirst)+" (it lost "+fmt(lost[0])+"); "+s+"the knight 3.2 off took no hit, lost "+fmt(lost[4])+" and was not moved="+outside);
                    clearFixture(b,"slam");next(b,22);
                }
                case 22->{ // p2-counters-siege: a supply banner of Dusk, a Ballista Crew, a Mortar and a knight of Dawn, Brassjaw and a small synthetic troop of Dusk
                    banner=GravesAndGlory.ArmySupplyBanner.place(b,dusk,new Vec3d(site.getX()+.5+6,site.getY(),site.getZ()+.5-6),0f,40f);
                    if(banner==null){report("p2-counters-siege",false,"no spot for the banner on the platform");next(b,30);return;}
                    u1=post(b,dawn,"ic_ballista",0,-10,0,true);u2=post(b,dawn,"knight",-4,-6,0,true);u3=post(b,dusk,"ic_colossus",-6,6,180,true);
                    u4=post(b,dawn,"ic_mortar",-4,-10,0,true);
                    var small=Gilded.synthetic("p2_small","Moot Hound",false,"none","small");us[0]=post(b,dusk,small,-8,2,180,true);
                    float hp0=banner.hp();var c10=Commanders.hit(b,u1,banner,"arrow");float hp1=banner.hp();var c11=Commanders.hit(b,u2,banner,"melee");float hp2=banner.hp();
                    var c12=Commanders.hit(b,u1,u3.mob,"arrow");var c13=Commanders.hit(b,u1,us[0].mob,"arrow");var mortar=Commanders.hit(b,u4,banner,"blight");float hp3=banner.hp();
                    boolean row=c10!=null&&c11!=null&&mortar!=null;
                    boolean taken=row&&Math.abs((hp0-hp1)-c10.base()*c10.total())<1e-3&&Math.abs((hp1-hp2)-c11.base()*c11.total())<1e-3&&Math.abs((hp2-hp3)-mortar.base()*mortar.total())<1e-3;
                    // a commander's own blow counts as a non-siege one; his own side's blow and fire do nothing
                    var pl=sp(b);float hpP=banner.hp();boolean hurtP=banner.damage(b.world.getDamageSources().playerAttack(pl),8f);float hpP1=banner.hp();
                    var mine=GravesAndGlory.ArmySupplyBanner.place(b,own,new Vec3d(site.getX()+.5-6,site.getY(),site.getZ()+.5-6),0f,40f);
                    boolean ownRefused=mine!=null&&!mine.damage(b.world.getDamageSources().playerAttack(pl),8f)&&mine.hp()==40f&&!mine.damage(b.world.getDamageSources().generic(),100f)&&mine.hp()==40f&&!mine.damage(b.world.getDamageSources().onFire(),100f);
                    if(mine!=null)mine.discard();
                    boolean tagged=banner.getCommandTags().containsAll(List.of("gravesandglory","structure"))&&near(hp0,40)&&banner.isInvisible();
                    boolean ok1=Gilded.is(c10,4.0,"C10")&&Gilded.is(c11,0.25,"C11")&&Gilded.is(c12,1.3,"C12")&&Gilded.is(c13,0.6,"C13")&&Gilded.is(mortar,4.0,"C10");
                    boolean player=hurtP&&Math.abs((hpP-hpP1)-8f*0.25f)<1e-3;
                    report("p2-counters-siege",ok1&&taken&&tagged&&player&&ownRefused,"against the supply banner (tags "+banner.getCommandTags()+", "+hp0+" health): a Ballista Crew's bolt "+show(c10)+" took "+fmt(hp0-hp1)+" (want 4.0, C10, the total clamped to the table's range); an Order Squire's blow "+show(c11)+" took "+fmt(hp1-hp2)+" (want 0.25, C11); a Mortar's shell "+show(mortar)+" took "+fmt(hp2-hp3)+" (C10); the commander's own blow of 8 took "+fmt(hpP-hpP1)+" (want 2.0, the non-siege 0.25)="+player+"; the amounts come off its health as the log says="+taken+"; its own side's blow, a fall and fire do nothing="+ownRefused+"; the Ballista Crew's bolt on Brassjaw (large) "+show(c12)+" (want 1.3, C12); on a small synthetic troop "+show(c13)+" (want 0.6, C13)");
                    clearFixture(b,"siege counters");banner.discard();banner=null;next(b,23);
                }
                case 23->{ // a Mortar's shell bursts on a banner within its radius
                    banner=GravesAndGlory.ArmySupplyBanner.place(b,dusk,new Vec3d(site.getX()+.5+6,site.getY(),site.getZ()+.5-3),0f,40f);
                    u4=post(b,dawn,"ic_mortar",-4,-10,0,true);u4.abilityAt=0;us[0]=bare(post(b,dusk,"knight",6,-6,180,true));
                    t0=b.time;castTime=0;Abilities.attack(b,u4,us[0].mob,b.time);casting=u4.spellCenter!=null;mark=Combat.LAST.peekLast();next(b,24);
                }
                case 24->{
                    long t=b.time-t0;
                    if(casting&&u4.spellCenter!=null)Abilities.tick(b,u4,us[0].mob,b.time);
                    if(casting&&u4.spellCenter==null){casting=false;castTime=t;}
                    if(castTime>0||t>60){
                        var h=Gilded.hitOn(banner,mark);
                        boolean hit=h!=null&&Gilded.is(h,4.0,"C10")&&banner.hp()<40f;
                        report("p2-mortar-banner",hit&&castTime==30,"a Mortar's shell bursting 3.0 from a supply banner (burst "+castTime+" ticks after the cast, want 30) struck it "+show(h)+", it has "+fmt(banner.hp())+" of 40 left (want less)="+hit);
                        clearFixture(b,"mortar banner");banner.discard();banner=null;next(b,30);
                    }
                }
                case 30->{ // p2-supply-banner: the Quartermaster plants a banner 40 blocks from his base; a troop trained now deploys there
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"supply");reset(own);own.order="hold";own.target=new Vec3d(site.getX()+.5+60,70,site.getZ()+.5);own.training.clear();
                    var c=Commander.row(own);t0=b.time;banner=plant(b,p,0);
                    boolean row=c.id().equals("quartermaster")&&c.active().equals("forward_supply")&&c.cooldownTicks()==3000&&c.durationTicks()==1200&&near(c.radius(),12)&&near(c.range(),30)&&near(c.value1(),40);
                    boolean placed=banner!=null&&b.structures.size()==1&&banner.squad()==own&&near(banner.hp(),40)&&banner.isAlive()&&banner.getCommandTags().containsAll(List.of("gravesandglory","structure"))&&banner.block!=null&&b.world.getBlockState(banner.block).getBlock() instanceof net.minecraft.block.BannerBlock&&own.abilityData==banner;
                    double away=banner==null?-1:distance(banner.getPos(),base0());
                    boolean timing=own.abilityUntil==t0+1200&&own.abilityReadyAt==t0+3000&&Commander.running(b,own,"forward_supply")&&Commander.readyIn(b,own)==3000;
                    supplyOk=row&&placed&&away>=30&&away<=45&&timing;
                    supplyWhy.clear();supplyWhy.add("the Quartermaster's row "+c.id()+"/"+c.active()+" (cooldown "+c.cooldownTicks()+", lasts "+c.durationTicks()+", radius "+c.radius()+", range "+c.range()+", "+c.value1()+" health)="+row+"; he stood "+fmt(distance(p.getPos(),base0()))+" from his base: a banner stands "+fmt(away)+" from it (want 30 or more), with "+(banner==null?"no health":fmt(banner.hp())+" health")+", the tags structure and gravesandglory, its block placed, held in his active's data="+placed+", the active runs to +"+(own.abilityUntil-t0)+" and is ready again at +"+(own.abilityReadyAt-t0)+"="+timing);
                    if(banner==null){report("p2-supply-banner",false,supplyWhy.get(0));next(b,99);return;}
                    bannerBlock=banner.block;
                    own.training.addLast(new GravesAndGlory.Training(Campaign.troop("knight"),b.time));
                    next(b,31);
                }
                case 31->{ // the troop trained appears at the banner, not the barracks
                    var fresh=b.units.stream().filter(u->u.squad==own).toList();
                    if(fresh.isEmpty()&&since(b)<40)return;
                    var u=fresh.isEmpty()?null:fresh.get(0);
                    var anchors=b.reinforcementAnchors(own.team);var anchor=anchors.isEmpty()?null:anchors.get(0);
                    boolean at=u!=null&&distance(u.mob.getPos(),banner.getPos())<7&&distance(u.mob.getPos(),base0())>25;
                    boolean note=anchor!=null&&anchor.equals(banner.anchor())&&b.deployNote(own,anchor).contains("Forward Supply")&&b.deployNote(own,null).contains("barracks");
                    supplyOk&=at&&note&&anchors.size()==1;
                    supplyWhy.add("an Order Squire trained with the banner up appeared "+(u==null?"never":fmt(distance(u.mob.getPos(),banner.getPos()))+" from the banner and "+fmt(distance(u.mob.getPos(),base0()))+" from his base")+" (want within 7 of the banner)="+at+"; the anchors are "+anchors.stream().map(GravesAndGlory.Anchor::name).toList()+", the notice reads \""+b.deployNote(own,anchor)+"\" (from the barracks: \""+b.deployNote(own,null)+"\")="+note);
                    if(u!=null){u.mob.setAiDisabled(true);fixture.add(u);pins.add(new Pin(u,u.mob.getPos(),-90f));}
                    // refused too near his base: from the platform's west edge
                    var p=sp(b);Commander.reset(b,own);boolean gone=b.structures.isEmpty()&&b.world.getBlockState(bannerBlock).isAir();
                    long ready=own.abilityReadyAt;
                    b.relocate(p,site.getX()+.5-11.5,site.getY(),site.getZ()+.5,-90f,0f);b.campaign.notices.remove(player);b.command(p,"ability");
                    String notice=b.campaign.notices.get(player);double edge=distance(p.getPos(),base0());
                    boolean refused=edge<30&&b.structures.isEmpty()&&own.abilityUntil==0&&own.abilityReadyAt==ready&&("Too near your base. Plant it 30 or more blocks away.").equals(notice);
                    supplyOk&=gone&&refused;
                    supplyWhy.add("the active stopped by hand took the banner down and put its block back (air again)="+gone+"; from the platform's west edge, "+fmt(edge)+" from his base, the cast was refused with the notice \""+notice+"\", no banner, no cooldown, no duration="+refused);
                    // the rule is measured where the banner stands: a step behind his feet when he faces away from his base, a step beyond them when he faces it
                    Commander.reset(b,own);b.relocate(p,base0().x+30.6,site.getY(),site.getZ()+.5,-90f,0f);b.campaign.notices.remove(player);b.command(p,"ability");
                    double feetA=distance(p.getPos(),base0());String noticeA=b.campaign.notices.get(player);
                    boolean stepBack=feetA>=30&&b.structures.isEmpty()&&own.abilityUntil==0&&own.abilityReadyAt==0&&("Too near your base. Plant it 30 or more blocks away.").equals(noticeA);
                    Commander.reset(b,own);b.relocate(p,base0().x+29.4,site.getY(),site.getZ()+.5,90f,0f);b.campaign.notices.remove(player);b.command(p,"ability");
                    var beyond=b.structures.isEmpty()?null:b.structures.get(0);double feetB=distance(p.getPos(),base0()),bannerB=beyond==null?-1:distance(beyond.getPos(),base0());
                    boolean stepOn=feetB<30&&beyond!=null&&bannerB>=30&&own.abilityUntil>b.time;
                    Commander.reset(b,own);
                    supplyOk&=stepBack&&stepOn;
                    supplyWhy.add("the 30 blocks are measured at the banner: standing "+fmt(feetA)+" from his base facing away (the banner would stand a step nearer) the cast was refused with \""+noticeA+"\", no banner, no cooldown="+stepBack+"; standing "+fmt(feetB)+" from it facing it (the banner stands a step beyond) a banner was planted "+(beyond==null?"never":fmt(bannerB)+" from his base")+"="+stepOn);
                    report("p2-supply-banner",supplyOk,String.join("; ",supplyWhy));
                    clearFixture(b,"supply deploy");next(b,32);
                }
                case 32->{ // p2-supply-lost, (a): the banner is shot down by an enemy Ballista Crew before the troop finishes training
                    var p=sp(b);if(p==null)return;supplyWhy.clear();
                    clearFixture(b,"lost a");own.training.clear();
                    banner=plant(b,p,0);if(banner==null){report("p2-supply-lost",false,"no banner could be planted");next(b,99);return;}
                    bannerBlock=banner.block;own.training.addLast(new GravesAndGlory.Training(Campaign.troop("knight"),b.time+100));
                    p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);b.relocate(p,site.getX()+.5,site.getY()+30,site.getZ()+.5,0f,0f); // a spectator is nobody's target
                    dusk.order="attack";dusk.target=banner.getPos();u5=live(b,dusk,"ic_ballista",9,0,90);
                    t0=b.time;castTime=-1;next(b,33);
                }
                case 33->{
                    if(castTime<0&&banner.isRemoved()){castTime=b.time-t0;fallNotice=b.campaign.notices.getOrDefault(player,"");fallUntil=own.abilityUntil;fallReady=own.abilityReadyAt;}
                    if(since(b)<110)return; // the troop is finished at +100
                    boolean down=castTime>=0&&b.structures.isEmpty()&&b.world.getBlockState(bannerBlock).isAir();
                    var fresh=b.units.stream().filter(u->u.squad==own).toList();var u=fresh.isEmpty()?null:fresh.get(0);
                    boolean home=u!=null&&distance(u.mob.getPos(),base0())<12&&distance(u.mob.getPos(),centre())>25;
                    supplyWhy.add("(a) an enemy Ballista Crew 9 blocks off shot the banner down "+(castTime<0?"never":castTime+" ticks")+" after it was planted, its block is air again="+down+"; the troop that finished training afterwards appeared "+(u==null?"never":fmt(distance(u.mob.getPos(),base0()))+" from his base and "+fmt(distance(u.mob.getPos(),centre()))+" from the platform")+" (want the barracks)="+home);
                    boolean told=fallNotice.equals("Your supply banner has fallen.");
                    boolean ended=fallUntil==0&&Math.abs(fallReady-(t0+3000))<=2&&!Commander.running(b,own,"forward_supply")&&Commander.activeFor(b,own)==0&&own.abilityData==null;
                    supplyWhy.add("when it fell its commander read \""+fallNotice+"\"="+told+" and his active ended with it (it ran to +"+fallUntil+" (0 is stopped), the HUD tile shows "+Commander.activeFor(b,own)+" ticks of it, the cooldown stands until +"+(fallReady-t0)+" of 3000)="+ended);
                    supplyOk=down&&home&&told&&ended;
                    if(u!=null)Houses.discard(b,u);
                    clearFixture(b,"lost a done");own.training.clear();dusk.order="hold";next(b,34);
                }
                case 34->{ // (b): an enemy within 12 of the banner after the purchase, and one just beyond
                    var p=sp(b);banner=plant(b,p,0);if(banner==null){report("p2-supply-lost",false,"no banner could be planted (b)");next(b,99);return;}
                    p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);b.relocate(p,site.getX()+.5,site.getY()+30,site.getZ()+.5,0f,0f);
                    var bp=banner.getPos();var away=new Vec3d(bp.x+14,site.getY(),bp.z+0.0);
                    u1=post(b,dusk,"knight",away.x-site.getX()-.5,away.z-site.getZ()-.5,90,true);
                    boolean beyond=b.reinforcementAnchors(0).size()==1&&b.deploy(own,0).anchor()!=null; // an enemy 14 off: still a safe banner
                    move(u1,bp.x+8-site.getX()-.5,bp.z-site.getZ()-.5,90);
                    boolean inside=b.reinforcementAnchors(0).isEmpty()&&b.deploy(own,0).anchor()==null&&distance(b.deploy(own,0).at(),base0())<12;
                    supplyWhy.add("(b) an enemy knight 14 blocks from the banner leaves it a safe anchor (deploys there)="+beyond+"; at 8 blocks it is no anchor and a troop trained now deploys "+fmt(distance(b.deploy(own,0).at(),base0()))+" from his base, at the barracks="+inside);
                    supplyOk&=beyond&&inside;
                    clearFixture(b,"lost b");Commander.reset(b,own);next(b,35);
                }
                case 35->{ // (c): the timer runs out
                    var p=sp(b);banner=plant(b,p,0);if(banner==null){report("p2-supply-lost",false,"no banner could be planted (c)");next(b,99);return;}
                    bannerBlock=banner.block;boolean standing=b.deploy(own,0).anchor()!=null;own.abilityUntil=b.time+5;next(b,36);
                    supplyWhy.add("(c) with the banner standing a troop deploys at it="+standing);supplyOk&=standing;
                }
                case 36->{if(since(b)<12)return;
                    boolean gone=b.structures.isEmpty()&&banner.isRemoved()&&b.world.getBlockState(bannerBlock).isAir()&&own.abilityUntil==0&&b.deploy(own,0).anchor()==null;
                    boolean quiet=!"Your supply banner has fallen.".equals(b.campaign.notices.get(player));
                    supplyWhy.add("when the timer runs out the banner is taken down, its block restored and a troop deploys at the barracks="+gone+", and its commander is not told it fell (an expiry is no fall)="+quiet);supplyOk&=gone&&quiet;
                    Commander.reset(b,own);next(b,37);
                }
                case 37->{ // (d): an enemy Ballista Crew with a supply banner 6.5 off and the commander 20 off takes the commander (the army first, then its commander, then its banner)
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"lost d");Commander.reset(b,own);own.training.clear();
                    banner=plant(b,p,-3);if(banner==null){report("p2-supply-lost",false,"no banner could be planted (d)");next(b,99);return;}
                    bannerBlock=banner.block;dusk.order="attack";dusk.target=centre();
                    u1=post(b,dusk,"ic_ballista",-11,0,-90,true); // frozen: it chooses and does not shoot
                    b.relocate(p,site.getX()+.5+9,site.getY(),site.getZ()+.5,90f,0f);
                    next(b,38);
                }
                case 38->{if(since(b)<12)return; // a beat or more
                    var p=sp(b);boolean commander=p!=null&&u1.lockedTarget==p;
                    double toBanner=distance(u1.mob.getPos(),banner.getPos()),toCommander=p==null?-1:distance(u1.mob.getPos(),p.getPos());
                    supplyWhy.add("(d) an enemy Ballista Crew with the banner "+fmt(toBanner)+" off and the commander "+fmt(toCommander)+" off took "+(u1.lockedTarget==null?"no target":u1.lockedTarget==p?"the commander":u1.lockedTarget==banner?"the banner":"something else")+" (want the commander)="+commander);
                    supplyOk&=commander;
                    report("p2-supply-lost",supplyOk,String.join("; ",supplyWhy));
                    clearFixture(b,"lost d done");Commander.reset(b,own);dusk.order="hold";if(p!=null)p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);next(b,40);
                }
                case 40->{ // p2-golem-swing: Brassjaw (AI on) swings at a knight 2.8 off, a Hearthguard beside him at another; the swings are entity status 4
                    clearFixture(b,"swing");dawn.order="attack";var p=sp(b);if(p!=null){p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);b.relocate(p,site.getX()+.5-9,site.getY(),site.getZ()+.5+9,-45f,15f);}
                    u1=live(b,dawn,"ic_colossus",0,0,0);us[0]=tough(bare(post(b,dusk,"knight",0,2.8,180,true)));
                    u2=live(b,dawn,"ic_hearthguard",8,0,0);us[1]=tough(bare(post(b,dusk,"knight",8,2.4,180,true)));
                    dawn.target=us[0].mob.getPos();foeHp=us[0].mob.getHealth();foeHp2=us[1].mob.getHealth();
                    golemId=u1.mob.getId();swingSeen=0;watchSwing=true;statuses0=BattleEffects.attackStatuses;next(b,41);
                }
                case 41->{if(since(b)<130)return;
                    int sent=BattleEffects.attackStatuses-statuses0;boolean hit=us[0].mob.getHealth()<foeHp,mate=us[1].mob.getHealth()<foeHp2;int seen=swingSeen;watchSwing=false;
                    report("p2-golem-swing",sent>=2&&sent<=4&&hit&&mate&&seen>0,"in 130 ticks Brassjaw's swings sent "+sent+" entity-status-4 packets (want 2 to 4: one per swing, cooldown 36; a Hearthguard swinging beside him sends none), they cost the knight "+fmt(foeHp-us[0].mob.getHealth())+" health, the Hearthguard's cost its knight "+fmt(foeHp2-us[1].mob.getHealth())+"; the client saw the golem's attack animation run (attackTicksLeft up to "+seen+", want above 0)");
                    golemId=-1;clearFixture(b,"swing done");next(b,45);
                }
                case 45->{ // p2-ironclad-spacing: the room a body takes, in formation and on a bridge lane
                    reset(own);own.order="attack";own.formation="shieldwall";own.target=centre();own.yaw=-90;
                    for(int i=0;i<4;i++)us[i]=post(b,own,Campaign.troop("ic_hearthguard"),i,-8,-4+i*2,-90,true);
                    u1=post(b,own,Campaign.troop("ic_colossus"),4,-8,6,-90,true);u2=post(b,own,Campaign.troop("ic_arbalist"),5,-10,0,-90,true);
                    double sg=GravesAndGlory.Battle.spacing(u1),sh=GravesAndGlory.Battle.spacing(us[0]),sa=GravesAndGlory.Battle.spacing(u2);
                    double with=b.formation(us[0]).distanceTo(b.formation(us[1]));
                    b.units.remove(u1);
                    double without=b.formation(us[0]).distanceTo(b.formation(us[1]));
                    b.units.add(u1);
                    // the bridge lane offset (slot%5-2) x the body's room, for the slots of a Hearthguard and a golem
                    Vec3d goal=new Vec3d(60,65,0);var rg=Tactics.route(u1,goal);var rh=Tactics.route(us[0],goal);
                    var bridges=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();
                    double lane1=(4%5-2)*sg,lane0=(0%5-2)*sh;
                    boolean laneG=bridges.stream().anyMatch(br->Math.abs((rg.z-br.z())-lane1)<1e-4),laneH=bridges.stream().anyMatch(br->Math.abs((rh.z-br.z())-lane0)<1e-4);
                    boolean ok2=close(sg,1.7)&&close(sh,1.3)&&close(sa,1.3)&&close(with,1.7)&&close(without,1.5)&&laneG&&laneH;
                    report("p2-ironclad-spacing",ok2,"the room a body takes (width + 0.3, at least 1.3): Brassjaw "+fmt(sg)+" (want 1.70), a Hearthguard "+fmt(sh)+" and an Arbalist "+fmt(sa)+" (want 1.30); in a shieldwall (spacing 1.5) the neighbours of a group with Brassjaw in it stand "+fmt(with)+" apart (want 1.70) and without him "+fmt(without)+" (want 1.50); the bridge lane of his slot 4 is z "+fmt(rg.z)+" past the bridge's centre, offset "+fmt(lane1)+" (3.4; it was 2.6 with the fixed 1.3)="+laneG+", a Hearthguard's slot 0 "+fmt(rh.z)+", offset "+fmt(lane0)+"="+laneH);
                    clearFixture(b,"spacing");own.formation="block";reset(own);next(b,50);
                }
                case 50->{ // p2-entrench, on a platform over the objective B: six Ironclad slots (tier I) of the commander's army, none of them has moved
                    reset(own);Commander.reset(b,own);
                    owned=b.points.stream().filter(c->c.spec.id().equals("B")).findFirst().orElse(b.points.get(0));
                    ownedWas=owned.owner;ownedProgress=owned.progress;ownedChallenger=owned.challenger;ownedSince=owned.ownedSince;owned.owner=-1;
                    pointSite=new BlockPos(owned.spec.x(),140,owned.spec.z());force(b,pointSite.add(-13,0,-13),pointSite.add(13,0,13));platform(b,pointSite,12,Blocks.STONE.getDefaultState());platforms.add(pointSite);site=pointSite;
                    for(int i=0;i<4;i++)us[i]=post(b,own,"ic_hearthguard",-2+i*2,-3,-90,true);
                    u1=post(b,own,"ic_ballista",-6,3,-90,true);u2=post(b,own,"ic_mortar",-6,-3,-90,true);u3=post(b,dusk,"knight",6,0,90,true);
                    Houses.recompute(own);t0=b.time;
                    int tier=own.traits.getOrDefault("ironclad",0);var row=CounterMath.trait(Spec.HOUSETRAITS,"ironclad",1);
                    boolean sheet=row!=null&&row.mechanic().equals("entrench")&&near(row.value(),0.1)&&near(row.value2(),0.1)&&near(row.radius(),0.5)&&row.durationTicks()==60;
                    var h0=Commanders.hit(b,u3,us[0].mob,"melee");
                    entrenchOk=sheet&&tier==1&&!Houses.still(us[0])&&h0!=null&&near(h0.traits(),1.0)&&near(Abilities.range(b,u1),34.0);
                    entrenchWhy="the Entrench row "+(row==null?"none":row.id()+": "+row.value()+" less damage, "+row.value2()+" more range, still within "+row.radius()+" for "+row.durationTicks()+" ticks")+"="+sheet+"; seven Ironclad slots are tier "+tier+"; at the start nothing has stood still: a blow on a Hearthguard has traits "+(h0==null?"none":String.valueOf(h0.traits()))+" (want 1.0), the Ballista Crew's range "+fmt(Abilities.range(b,u1))+" (want 34)="+entrenchOk+"; ";
                    next(b,51);
                }
                case 51->{if(since(b)<80)return; // 80 ticks of standing still
                    boolean still=Houses.still(us[0])&&Houses.still(u1)&&Houses.still(u2),otherHouse=Houses.still(u3); // u3, a knight of Dusk (no Entrench), has stood as long
                    var neutral=Commanders.hit(b,u3,us[0].mob,"melee");
                    owned.owner=1;var theirs=Commanders.hit(b,u3,us[0].mob,"melee"); // an objective the enemy holds does not count
                    owned.owner=0;var mine=Commanders.hit(b,u3,us[0].mob,"melee");var arrow=Commanders.hit(b,u3,us[0].mob,"arrow");
                    double r1=Abilities.range(b,u1),r2=Abilities.range(b,u2),rg=Abilities.range(b,us[0]);
                    var foeHit=Commanders.hit(b,us[0],u3.mob,"melee"); // the other House takes none
                    boolean t1=still&&neutral!=null&&near(neutral.traits(),0.9)&&theirs!=null&&near(theirs.traits(),0.9)&&mine!=null&&near(mine.traits(),0.8)&&arrow!=null&&near(arrow.traits(),0.8);
                    boolean range=Math.abs(r1-37.4)<1e-6&&Math.abs(r2-39.6)<1e-6&&near(rg,Campaign.troop("ic_hearthguard").range())&&foeHit!=null&&near(foeHit.traits(),1.0);
                    // tiers II and III by the sheet (set directly: nothing recomputes between these lines)
                    StringBuilder s=new StringBuilder();boolean tiers=true;
                    for(int tr=2;tr<=3;tr++){
                        var row=CounterMath.trait(Spec.HOUSETRAITS,"ironclad",tr);own.traits.put("ironclad",tr);
                        owned.owner=-1;var a=Commanders.hit(b,u3,us[0].mob,"melee");owned.owner=0;var c2=Commanders.hit(b,u3,us[0].mob,"melee");double rr=Abilities.range(b,u1);
                        boolean good=row!=null&&a!=null&&c2!=null&&near(a.traits(),1-row.value())&&near(c2.traits(),1-2*row.value())&&Math.abs(rr-34.0*(1+row.value2()))<1e-6;
                        tiers&=good;s.append("tier ").append(tr).append(": traits ").append(a==null?"none":fmt(a.traits())).append(", on the objective ").append(c2==null?"none":fmt(c2.traits())).append(", Ballista range ").append(fmt(rr)).append(good?"":" (BAD)").append("; ");
                    }
                    Houses.recompute(own);
                    entrenchOk&=t1&&range&&tiers&&otherHouse;
                    entrenchWhy+="after 80 ticks without moving (still="+still+"): a blow on a Hearthguard has traits "+show(neutral)+" (want 0.9); with the objective held by the enemy "+(theirs==null?"none":String.valueOf(theirs.traits()))+" (want 0.9); with it held by his side "+(mine==null?"none":String.valueOf(mine.traits()))+" (want 0.8), an arrow "+(arrow==null?"none":String.valueOf(arrow.traits()))+" (0.8)="+t1+"; the Ballista Crew's range is "+fmt(r1)+" (want 37.4) and the Mortar's "+fmt(r2)+" (39.6), a Hearthguard's stays "+fmt(rg)+", an Order Squire (Gilded) takes none="+(foeHit!=null&&near(foeHit.traits(),1.0))+"="+range+"; a knight of the other House (no Entrench) that stood as long is still="+otherHouse+" (the rest rule is the sheet's, whatever the House); "+s;
                    // now two of them step a block
                    owned.owner=0;move(us[0],-2+1.0,-3,-90);move(u1,-6,3+1.0,-90);next(b,52);
                }
                case 52->{if(since(b)<12)return;
                    var moved=Commanders.hit(b,u3,us[0].mob,"melee");var held=Commanders.hit(b,u3,us[1].mob,"melee");
                    boolean ok3=!Houses.still(us[0])&&!Houses.still(u1)&&Houses.still(us[1])&&moved!=null&&near(moved.traits(),1.0)&&held!=null&&near(held.traits(),0.8)&&near(Abilities.range(b,u1),34.0)&&Math.abs(Abilities.range(b,u2)-39.6)<1e-6;
                    entrenchOk&=ok3;
                    report("p2-entrench",entrenchOk,entrenchWhy+"a Hearthguard and the Ballista Crew that stepped a block (on a held objective) 12 ticks ago: traits "+(moved==null?"none":String.valueOf(moved.traits()))+" (want 1.0), the Ballista Crew's range "+fmt(Abilities.range(b,u1))+" (want 34); a Hearthguard that did not move: "+(held==null?"none":String.valueOf(held.traits()))+" (0.8), the Mortar's range "+fmt(Abilities.range(b,u2))+" (39.6)="+ok3);
                    next(b,53);
                }
                case 53->{ // the showcase: the commander's Ironclad army on the held objective, the Ballista Crew and the Mortar entrenched with their rings, a supply banner, Brassjaw about to hit a line
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"showcase");reset(own);own.order="hold";owned.owner=0;dusk.order="hold";
                    for(int i=0;i<4;i++)us[i]=post(b,own,"ic_hearthguard",-1.5,-4.5+i*3,-90,true);
                    post(b,own,"ic_arbalist",-4.5,-3,-90,true);post(b,own,"ic_arbalist",-4.5,3,-90,true);
                    u1=post(b,own,"ic_ballista",-8,-4.5,-90,true);u2=post(b,own,"ic_mortar",-8,4.5,-90,true);u3=post(b,own,"ic_colossus",1.5,0,-90,true);
                    for(int i=0;i<7;i++)line[i]=post(b,dusk,"knight",4.5,-3.6+i*1.2,90,true);
                    Houses.recompute(own);
                    banner=plant(b,p,-6,8); // planted where the commander stands, then he becomes the camera
                    p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    Vec3d cam=new Vec3d(site.getX()+.5-13,site.getY()+6.5,site.getZ()+.5+14),look=new Vec3d(site.getX()+.5-2,site.getY()+1,site.getZ()+.5-0.5);
                    double dx=look.x-cam.x,dz=look.z-cam.z,dy=look.y-cam.y;
                    b.relocate(p,cam.x,cam.y,cam.z,(float)(-Math.toDegrees(Math.atan2(dx,dz))),(float)(-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)))));
                    t0=b.time;next(b,54);
                }
                case 54->{ // 90 ticks for the Ballista Crew and the Mortar to settle, the rings drawn as they would be; then Brassjaw hits the line and the picture is taken
                    var p=sp(b);if(since(b)%6==0)ring(b,p,u1);
                    if(since(b)<90)return;
                    boolean settled=Houses.still(u1)&&Houses.still(u2);
                    for(int i:new int[]{1,2,4,5})unpin(line[i]); // the line is struck at its middle knight; the four within 2.5 of it are free to be thrown
                    float[] hp=new float[7];Vec3d[] was=new Vec3d[7];for(int i=0;i<7;i++){hp[i]=line[i].mob.getHealth();was[i]=line[i].mob.getPos();}
                    int sent0=BattleEffects.attackStatuses;mark=Combat.LAST.peekLast();Abilities.attack(b,u3,line[3].mob,b.time);int sent=BattleEffects.attackStatuses-sent0;
                    int hurt=0;for(int i:new int[]{1,2,4,5})if(line[i].mob.getHealth()<hp[i])hurt++;
                    boolean outer=line[0].mob.getHealth()==hp[0]&&line[6].mob.getHealth()==hp[6];
                    double r1=Abilities.range(b,u1),r2=Abilities.range(b,u2);
                    ok=settled&&hurt==4&&outer&&sent==1&&line[3].mob.getHealth()<hp[3]&&r1>34.0&&r2>36.0&&banner!=null&&banner.isAlive()&&owned.owner==0;
                    why="the Ballista Crew and the Mortar stood still for 90 ticks (entrenched="+settled+") on the objective B held by his side: their ranges are "+fmt(r1)+" and "+fmt(r2)+" against the sheet's 34 and 36, drawn as the gold rings (white at the sheet's reach, red the Mortar's minimum of 10); the supply banner stands with "+(banner==null?"-":fmt(banner.hp()))+" health; Brassjaw's blow on the middle knight of a line of seven hurt "+hurt+" of the four within 2.5 of it (want 4) and neither end knight (untouched="+outer+") and sent "+sent+" swing packet (want 1)";
                    requestShot("ironclad-showcase",4);next(b,55);
                }
                case 55->{ // the picture, with the knocked knights in the air
                    var p=sp(b);if(since(b)%6==0)ring(b,p,u1);
                    if(!shotTaken&&since(b)<40)return;
                    report("p2-ironclad-showcase",ok&&shotTaken,why+"; the picture ironclad-showcase was taken="+shotTaken);
                    wantShot=null;Commander.reset(b,own);next(b,60);
                }
                case 60->{ // p2-mortar-minrange, walking: real ground; a Mortar 8 blocks from a pinned knight, order attack
                    var p=sp(b);if(p!=null)p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
                    clearFixture(b,"field");owned.owner=ownedWas;owned.progress=ownedProgress;owned.challenger=ownedChallenger;owned.ownedSince=ownedSince;Houses.recompute(own);
                    var spot=flat(b);if(spot==null){minOk=false;minWhy+="no flat ground for the walking check";report("p2-mortar-minrange",false,minWhy);next(b,63);return;}
                    gx=spot[0];gy=spot[1];gz=spot[2];force(b,new BlockPos((int)gx-4,0,(int)gz-9),new BlockPos((int)gx+34,0,(int)gz+9));
                    Vec3d foeAt=ground(b,gx+12.5,gz+.5),mortarAt=ground(b,gx+4.5,gz+.5);
                    var foe=b.spawn(dusk,Campaign.troop("knight"),40,foeAt);foe.mob.setAiDisabled(true);Phase1Checks.freeze(foe);tough(bare(foe));fixture.add(foe);pins.add(new Pin(foe,foeAt,90f));foe.deployed=foeAt;us[0]=foe;
                    u1=b.spawn(dawn,Campaign.troop("ic_mortar"),32,mortarAt);u1.mob.refreshPositionAndAngles(mortarAt.x,mortarAt.y,mortarAt.z,-90f,0f);fixture.add(u1);u1.deployed=mortarAt;
                    dawn.order="attack";dawn.target=foeAt;dusk.order="hold";
                    startDist=distance(u1.mob.getPos(),foeAt);castDist=-1;castsInside=0;casting=false;retreatMax=startDist;castTime=-1;t0=b.time;next(b,61);
                }
                case 61->{ // each tick: the farthest it walked, and the distance at which each cast began
                    double d=distance(u1.mob.getPos(),us[0].mob.getPos());retreatMax=Math.max(retreatMax,d);
                    if(u1.spellCenter!=null&&!casting){casting=true;double dc=distance(u1.mob.getPos(),u1.spellCenter);if(castDist<0){castDist=dc;castTime=b.time-t0;}if(dc<10)castsInside++;}
                    if(u1.spellCenter==null)casting=false;
                    if(since(b)>=320||castDist>=0&&b.time-t0>=castTime+45){
                        boolean walk=startDist>7.9&&startDist<8.1&&retreatMax>=10&&castDist>=10&&castsInside==0;
                        minOk&=walk;minWhy+="on open ground (flat at "+(int)gx+", "+(int)gz+") a Mortar under the attack order, a pinned knight "+fmt(startDist)+" blocks off: it walked away to "+fmt(retreatMax)+" and its first cast began "+(castDist<0?"never":castTime+" ticks in, at "+fmt(castDist)+" blocks")+", casts begun inside 10: "+castsInside+" (want none)="+walk+"; ";
                        // the same on hold: it stays, and it does not cast
                        Vec3d back=ground(b,gx+4.5,gz+.5);u1.mob.getNavigation().stop();u1.mob.refreshPositionAndAngles(back.x,back.y,back.z,-90f,0f);u1.spellCenter=null;u1.abilityAt=0;u1.nextAttack=0;u1.lockedTarget=null;
                        dawn.order="hold";holdAt=back;casting=false;castDist=-1;castsInside=0;next(b,62);
                    }
                }
                case 62->{
                    if(u1.spellCenter!=null)castsInside++;
                    if(since(b)>=120){
                        double moved=distance(u1.mob.getPos(),holdAt);boolean stay=castsInside==0&&moved<1.0;
                        minOk&=stay;minWhy+="the same Mortar on hold, 8 blocks from the knight for 120 ticks: ticks with a spell begun "+castsInside+" (want 0), it moved "+fmt(moved)+" (want under 1)="+stay;
                        report("p2-mortar-minrange",minOk,minWhy);clearFixture(b,"mortar field");next(b,65);
                    }
                }
                case 65->{ // p2-mortar-hold: a Mortar on Hold, one pinned knight 8 blocks off (inside its 10) and another 20 off; it shells the far one and never the near one
                    if(gy==0){report("p2-mortar-hold",false,"no flat ground was found for the walking check");next(b,66);return;}
                    dawn.order="hold";dusk.order="hold";
                    Vec3d mortarAt=ground(b,gx+4.5,gz+.5);
                    us[0]=foe(b,ground(b,gx+12.5,gz+.5),40);us[1]=foe(b,ground(b,gx+24.5,gz+.5),41);
                    u1=b.spawn(dawn,Campaign.troop("ic_mortar"),32,mortarAt);u1.mob.refreshPositionAndAngles(mortarAt.x,mortarAt.y,mortarAt.z,-90f,0f);fixture.add(u1);u1.deployed=mortarAt;
                    standAt(b,dawn,u1,mortarAt,-90f);
                    holdAt=mortarAt;castDist=-1;castsInside=0;casts=0;casting=false;foeHp=us[1].mob.getHealth();foeHp2=us[0].mob.getHealth();
                    next(b,68);
                }
                case 68->{ // 150 ticks: each spell it begins, and where it lands
                    if(u1.spellCenter!=null&&!casting){casting=true;casts++;double dc=distance(u1.mob.getPos(),u1.spellCenter);if(castDist<0)castDist=dc;if(dc<10)castsInside++;}
                    if(u1.spellCenter==null)casting=false;
                    if(since(b)<150)return;
                    float farLost=foeHp-us[1].mob.getHealth(),nearLost=foeHp2-us[0].mob.getHealth();double moved=distance(u1.mob.getPos(),holdAt);
                    boolean good=casts>=1&&castsInside==0&&Math.abs(castDist-20)<1.0&&farLost>0&&nearLost==0&&moved<1.0;
                    report("p2-mortar-hold",good,"a Mortar on Hold, a pinned knight "+fmt(distance(holdAt,us[0].mob.getPos()))+" blocks off it (inside its minimum of 10) and another "+fmt(distance(holdAt,us[1].mob.getPos()))+" off: in 150 ticks it began "+casts+" spells (want some), the first aimed "+(castDist<0?"nowhere":fmt(castDist)+" blocks away")+" (want the far knight, 20) and "+castsInside+" inside 10 (want 0); the far knight lost "+fmt(farLost)+" health (want some), the near one "+fmt(nearLost)+" (want 0); it moved "+fmt(moved)+" (want under 1)="+good);
                    clearFixture(b,"mortar hold");next(b,66);
                }
                case 66->{ // p2-siege-engage: a Mortar and a Ballista Crew under the attack order, one pinned knight 32 blocks off (past the order's radius of 24, inside their reach of 36 and 34)
                    if(gy==0){report("p2-siege-engage",false,"no flat ground was found for the walking check");next(b,76);return;}
                    Vec3d foeAt=ground(b,gx+29.5,gz+.5);
                    var foe=b.spawn(dusk,Campaign.troop("knight"),40,foeAt);foe.mob.setAiDisabled(true);Phase1Checks.freeze(foe);tough(bare(foe));fixture.add(foe);pins.add(new Pin(foe,foeAt,90f));foe.deployed=foeAt;us[0]=foe;
                    u1=b.spawn(dawn,Campaign.troop("ic_mortar"),32,ground(b,gx-2.5,gz-2.5));u2=b.spawn(dawn,Campaign.troop("ic_ballista"),33,ground(b,gx-2.5,gz+3.5));
                    u3=b.spawn(dawn,Campaign.troop("ic_arbalist"),34,ground(b,gx-2.5,gz+.5));u3.mob.setAiDisabled(true);Phase1Checks.freeze(u3);
                    for(var u:List.of(u1,u2,u3)){u.mob.refreshPositionAndAngles(u.mob.getX(),u.mob.getY(),u.mob.getZ(),-90f,0f);fixture.add(u);u.deployed=u.mob.getPos();}
                    dawn.order="attack";dawn.target=foeAt;dusk.order="hold";
                    boolean radii=near(Tactics.engageRadius(b,u1,24),36)&&near(Tactics.engageRadius(b,u2,24),34)&&near(Tactics.engageRadius(b,u3,24),24)&&near(Tactics.engageRadius(b,u1,10),36)&&near(Tactics.engageRadius(b,u3,10),10);
                    minOk=radii;minWhy="the order radius a troop engages at (attack 24, hold 10 on the sheet): a Mortar "+fmt(Tactics.engageRadius(b,u1,24))+" and a Ballista Crew "+fmt(Tactics.engageRadius(b,u2,24))+" (their reach), an Arbalist "+fmt(Tactics.engageRadius(b,u3,24))+" (unchanged), the Mortar on hold "+fmt(Tactics.engageRadius(b,u1,10))+"="+radii+"; ";
                    castDist=-1;shotDist=-1;foeHp=foe.mob.getHealth();next(b,67);
                }
                case 67->{ // 120 ticks: both fire from where they stand
                    if(castDist<0&&u1.spellCenter!=null)castDist=distance(u1.mob.getPos(),us[0].mob.getPos());
                    if(shotDist<0&&b.shots.stream().anyMatch(sh->sh.shooter()==u2))shotDist=distance(u2.mob.getPos(),us[0].mob.getPos());
                    if(since(b)<120)return;
                    boolean fired=castDist>=30&&shotDist>=30&&us[0].mob.getHealth()<foeHp;
                    report("p2-siege-engage",minOk&&fired,minWhy+"the Mortar began a shell at the knight from "+(castDist<0?"never":fmt(castDist))+" blocks and the Ballista Crew loosed a bolt from "+(shotDist<0?"never":fmt(shotDist))+" (want 30 or more for both, the order's radius is 24); the knight lost "+fmt(foeHp-us[0].mob.getHealth())+" health (want some)="+fired);
                    clearFixture(b,"siege engage");next(b,76);
                }
                case 63->{ // p2-ironclad-bridge: Dawn's army of four Hearthguards and Brassjaw (slot 4) at a bridge's west end, ordered across; each bridge of map 0 in turn
                    var bridges=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();
                    if(bridgeIdx>=bridges.size()){report("p2-ironclad-bridge",bridgesOk,bridgeWhy.toString());next(b,71);return;}
                    var br=bridges.get(bridgeIdx);clearFixture(b,"bridge");reset(own);Commander.reset(b,own);
                    field(b,own,banner("ic_hearthguard*4,ic_colossus"));
                    double x0=br.x()-br.width()/2.0-14;
                    force(b,new BlockPos((int)x0-4,0,br.z()-10),new BlockPos(br.x()+br.width()/2+24,0,br.z()+10));
                    own.order="move";own.formation="block";own.target=new Vec3d(br.x()+br.width()/2.0+16,65,br.z());own.yaw=-90;
                    for(var u:b.units)if(u.squad==own){double px=x0+(u.slot%5)*1.5,pz=br.z()+(u.slot/5-1.5)*1.4;u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(px,Arena.ground((int)px,(int)pz)+1,pz,-90f,0f);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;fixture.add(u);if(u.spec.id().equals("ic_colossus"))u1=u;}
                    crossedAt=-1;deckMin=1e9;deckTicks=0;wet=false;t0=b.time;next(b,64);
                }
                case 64->{
                    var bridges=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();var br=bridges.get(bridgeIdx);
                    double end=br.x()+br.width()/2.0-4;
                    long across=fixture.stream().filter(u->u.squad==own&&u.mob.isAlive()&&u.mob.getX()>end&&u.mob.getY()>63).count();
                    boolean big=u1!=null&&u1.mob.isAlive()&&u1.mob.getX()>end&&u1.mob.getY()>63;
                    if(big&&crossedAt<0)crossedAt=b.time-t0;
                    if(u1!=null&&u1.mob.isAlive()){ // on the bridge itself: at deck height, never in the water
                        if(u1.mob.isTouchingWater())wet=true;
                        if(Math.abs(u1.mob.getX()-br.x())<=br.width()/2.0-1){deckMin=Math.min(deckMin,u1.mob.getY());deckTicks++;}
                    }
                    if(across==5||since(b)>=900){
                        boolean deck=deckTicks>0&&deckMin>=br.elevation()+0.5&&!wet;
                        boolean good=big&&across==5&&deck;bridgesOk&=good;
                        bridgeWhy.append(br.id()).append(" (").append(br.width()).append(" x ").append(br.depth()).append("): ").append(across).append("/5 across, Brassjaw ").append(big?"at +"+crossedAt+" ticks":"NOT across (at x "+(u1==null?"-":fmt(u1.mob.getX()))+", y "+(u1==null?"-":fmt(u1.mob.getY()))+")").append(", on the deck for ").append(deckTicks).append(" ticks at y ").append(deckTicks>0?fmt(deckMin):"-").append(" or higher (deck top ").append(br.elevation()+1).append("), in water ").append(wet).append(deck?"":" (NOT ON THE DECK)").append(good?"":" (BAD)").append("; ");
                        for(var u:new ArrayList<>(fixture))Houses.discard(b,u);fixture.clear();u1=null;audit(b,"bridge "+br.id(),false);
                        bridgeIdx++;next(b,63);
                    }
                }
                case 76->{ // p2-siege-hold, (a): a clear field
                    if(gy==0){report("p2-siege-hold",false,"no flat ground was found for the walking check");next(b,63);return;}
                    siegeHold(b,false);next(b,77);
                }
                case 77->{ // 200 ticks: after 60 still it reaches the knight and fires from where it stands
                    if(!siegeWatch(b))return;
                    holdOk=siegeOk;holdWhy="(a) a clear field: "+siegeNote;
                    clearFixture(b,"siege hold a");siegeHold(b,true);next(b,78);
                }
                case 78->{ // (b): a stone wall between them; it cannot see the knight and must not walk about to find a line
                    if(!siegeWatch(b))return;
                    report("p2-siege-hold",holdOk&&siegeOk,holdWhy+"; (b) a stone wall between them: "+siegeNote);
                    wallDown(b);clearFixture(b,"siege hold b");next(b,63);
                }
                case 71->{ // p2-supply-cleanup, (a): the ways a standing banner goes that are not a shot: the sweep every round start, map switch and lobby makes, and a kill
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"cleanup paths");Commander.reset(b,own);own.training.clear();supplyWhy.clear();
                    banner=plant(b,p,0);if(banner==null){report("p2-supply-cleanup",false,"no banner could be planted");next(b,70);return;}
                    bannerBlock=banner.block;var first=banner;
                    Abilities.clearBanners(b); // the call returnLobby, startRound, switchMap and the server stopping each make
                    boolean swept=first.isRemoved()&&b.structures.isEmpty()&&b.world.getBlockState(bannerBlock).isAir()&&!"Your supply banner has fallen.".equals(b.campaign.notices.get(player));
                    Commander.reset(b,own);
                    banner=plant(b,p,0);if(banner==null){report("p2-supply-cleanup",false,"no banner could be planted (kill)");next(b,70);return;}
                    bannerBlock=banner.block;banner.kill(); // what /kill does to an armor stand
                    boolean killed=banner.isRemoved()&&b.structures.isEmpty()&&b.world.getBlockState(bannerBlock).isAir()&&"Your supply banner has fallen.".equals(b.campaign.notices.get(player))&&own.abilityUntil==0&&own.abilityReadyAt>b.time;
                    supplyOk=swept&&killed;
                    supplyWhy.add("Abilities.clearBanners (the call a round start, a map switch, the lobby and a server stop each make) took a standing banner down, its block air again and nobody told it fell="+swept+"; a killed banner (what /kill does) left the list, its block air again, its commander told it fell and his active stopped (cooldown stands)="+killed);
                    next(b,72);
                }
                case 72->{ // (b): the round ends with a banner standing
                    var p=sp(b);if(p==null)return;
                    Commander.reset(b,own);banner=plant(b,p,0);
                    if(banner==null){report("p2-supply-cleanup",false,"no banner could be planted (lobby)");next(b,70);return;}
                    bannerBlock=banner.block;var planted=banner;
                    b.campaign.returnLobby();
                    boolean lobby=!b.running&&planted.isRemoved()&&b.structures.isEmpty()&&b.world.getBlockState(bannerBlock).isAir()&&own.abilityUntil==0&&own.abilityData==null;
                    supplyWhy.add("returnLobby with a banner standing took it down, its block air again and ended the active (round running="+b.running+")="+lobby);
                    report("p2-supply-cleanup",supplyOk&&lobby,String.join("; ",supplyWhy)+"; a round start and a map switch were not run with a banner standing: each begins with the Abilities.clearBanners call checked above");
                    next(b,70);
                }
                case 70->{ // p2-ironclad-tick-cost: eight bot armies, four Ironclad (hard) against four Gilded (hard)
                    var p=sp(b);if(p==null)return;
                    if(cost==null){clearFixture(b,"before cost");cost=tickCost(b,List.of("ironclad_hard","ironclad_hard","ironclad_hard","ironclad_hard","gilded_hard","gilded_hard","gilded_hard","gilded_hard"));}
                    if(cost.step(p)){
                        report("p2-ironclad-tick-cost",cost.last<40,"server tick "+String.format(Locale.ROOT,"%.2f",cost.last)+" ms (600-tick mean "+String.format(Locale.ROOT,"%.2f",cost.avg)+" ms) with "+cost.troops+" troops at the start and "+b.units.size()+" at the end, "+b.structures.size()+" structures ("+cost.note+")");
                        audit(b,"before the final returnLobby",false);
                        report("p2-ironclad-teams-clean",audits>=14&&staleSeen.isEmpty(),audits+" audits after the suite's raisings, deaths and removals: "+(staleSeen.isEmpty()?"no gg_ team ever held a member that was not a living troop":"STALE "+staleSeen)+"; "+auditNotes);
                        cost.handBack();b.campaign.returnLobby();next(b,99);
                    }
                }
                default->{}
            }
        }
        /** The ballista's range as it is now, in white, and what it would be without Entrench in gold, flat at the platform's height, shown to the commander from any distance; the Mortar's minimum range in red. */
        void ring(GravesAndGlory.Battle b,ServerPlayerEntity p,GravesAndGlory.Unit u){
            if(p==null||u==null)return;
            double y=site.getY()+.15;
            for(int i=0;i<160;i++){
                double a=i*2*Math.PI/160;
                b.world.spawnParticles(p,Abilities.dust(0xF2F2F2),true,u.mob.getX()+Math.cos(a)*u.spec.range(),y,u.mob.getZ()+Math.sin(a)*u.spec.range(),1,0,0,0,0);
                b.world.spawnParticles(p,Abilities.dust(0xE8B923),true,u.mob.getX()+Math.cos(a)*Abilities.range(b,u),y,u.mob.getZ()+Math.sin(a)*Abilities.range(b,u),1,0,0,0,0);
            }
            if(u2!=null&&u2.mob.isAlive())for(int i=0;i<48;i++){double a=i*2*Math.PI/48;b.world.spawnParticles(p,Abilities.dust(0xD94A3A),true,u2.mob.getX()+Math.cos(a)*10,y,u2.mob.getZ()+Math.sin(a)*10,1,0,0,0,0);}
        }
    }

    /**
     * Task 9 (batch 4, mechanics half): the Tidecallers. The three new mobs (goals only), the Mist Caller's cloud with its slow and counter C17, the Harpooner's pull with its
     * flying trident, the Admiral's undertow, the Brute's sweep, brine against Riptide (the longer slow stands), Riptide's speed in water and in rain and its ford, a Brute across
     * the bridge at tier 0, the Trident Guard's brace (C18c), the Warden's Mark Prey, the clouds' cleanup and the tick cost. The fixture is Gilded's: a stone platform in the sky above
     * Dawn's base, two ad hoc armies and the commander's own (a Warden); troops are posted frozen and pinned, and every blow goes through the real damage path (Combat.resolve) so the
     * on-hit abilities run as they do in a round. The showcase, the ford picture and the tick cost stay here; the lineup and stages pictures belong to the art half.
     */
    static final class Tidecallers extends Staged {
        final Map<BlockPos,net.minecraft.block.BlockState> built=new LinkedHashMap<>();
        GravesAndGlory.Unit u1,u2,u3,u4,u5,u6;final GravesAndGlory.Unit[] us=new GravesAndGlory.Unit[10],line=new GravesAndGlory.Unit[7];
        Combat.HitLog mark;Abilities.Mist cloud;TickCost cost;
        String mobsWhy="",why="";boolean mobsOk,ok;
        // the live mobs
        int fangs,vexes,wild,visuals,clouds,roars;float foeHp,foeHp2;
        // the harpoon, the undertow and the sweep
        Vec3d[] was=new Vec3d[10];double d0,d1,d2,d3;long cooldownEnd;float[] hp=new float[10];String pullWhy="",tideWhy="",sweepWhy="";boolean pullOk,tideOk,sweepOk,pullChecked;
        // the mist
        long castAt,cloudAt;String mistWhy="";boolean mistOk;
        // the speed lanes
        int phase;double xa,xb,wetTicks;String speedWhy="";boolean speedOk;double landRatio,rainRatio,waterRatio,landSpeed,waterSpeed;
        // the river
        double fordZ,fordX0;int inRiver;final Map<GravesAndGlory.Unit,double[]> crossed=new LinkedHashMap<>();final Set<GravesAndGlory.Unit> waded=new HashSet<>();boolean fordShot;String fordWhy="";
        int bridgeIdx;boolean bridgesOk=true;final StringBuilder bridgeWhy=new StringBuilder();long crossedAt;double deckMin;int deckTicks;boolean wet;
        // the Warden
        String markWhy="";boolean markOk;long markAt;
        // the showcase
        boolean cloudUp,m1,m2,m3,m4;int swept;GravesAndGlory.Unit[] knights=new GravesAndGlory.Unit[7];

        static boolean close(double a,double b){return Math.abs(a-b)<1e-3;}
        static Vec3d base0(){return GravesAndGlory.Battle.base(0);}
        /** The standing spot on real ground at (x, z). */
        static Vec3d ground(GravesAndGlory.Battle b,double x,double z){return new Vec3d(x,Phase1Checks.top(b,x,z),z);}
        /** A troop with its AI on (Tactics moves and swings it), on the platform at (dx, dy, dz) from its centre. */
        GravesAndGlory.Unit liveAt(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dy,double dz,float yaw){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY()+dy,site.getZ()+.5+dz);
            var u=b.spawn(s,Campaign.troop(troop),30+fixture.size(),at);u.mob.refreshPositionAndAngles(at.x,at.y,at.z,yaw,0);fixture.add(u);u.deployed=at;return u;
        }
        GravesAndGlory.Unit live(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw){return liveAt(b,s,troop,dx,0,dz,yaw);}
        void unpin(GravesAndGlory.Unit u){pins.removeIf(p->p.u()==u);}
        /** A troop that cannot be killed by the stage in hand. */
        static GravesAndGlory.Unit tough(GravesAndGlory.Unit u){u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(4000);u.mob.setHealth(4000f);return u;}
        /** The order point of an army that puts the formation spot of its troop where it asks (a troop under a Move order walks to that spot). */
        void standAt(GravesAndGlory.Battle b,GravesAndGlory.Squad s,GravesAndGlory.Unit u,Vec3d at,float yaw){
            s.yaw=yaw;s.target=at;Vec3d off=b.formation(u).subtract(at);s.target=at.subtract(off);
        }
        Vec3d pos(double dx,double dy,double dz){return new Vec3d(site.getX()+.5+dx,site.getY()+dy,site.getZ()+.5+dz);}
        /** A block of the stage's own (a lane, a wall); what stood there is put back by unbuild. */
        void put(GravesAndGlory.Battle b,BlockPos at,net.minecraft.block.BlockState state){BlockPos k=at.toImmutable();built.putIfAbsent(k,b.world.getBlockState(k));b.world.setBlockState(k,state);}
        void unbuild(GravesAndGlory.Battle b){for(var e:built.entrySet())b.world.setBlockState(e.getKey(),e.getValue());built.clear();}
        /** The Slowness a living entity has: its duration at amplifier `level`, -1 with none, -2 at another amplifier. */
        static int slowAt(net.minecraft.entity.LivingEntity e,int level){var s=e.getStatusEffect(StatusEffects.SLOWNESS);return s==null?-1:s.getAmplifier()==level?s.getDuration():-2;}
        static String slowText(net.minecraft.entity.LivingEntity e){var s=e.getStatusEffect(StatusEffects.SLOWNESS);return s==null?"none":"Slowness "+(s.getAmplifier()+1)+" for "+s.getDuration();}
        @Override void cleared(){Arrays.fill(us,null);Arrays.fill(line,null);Arrays.fill(was,null);}

        /** One real hit of this kind from u on the victim, its hurt cooldown, slow and the troop's own cooldown cleared first; the Slowness it leaves (see slowAt). */
        int read(GravesAndGlory.Battle b,GravesAndGlory.Unit u,GravesAndGlory.Unit victim,String kind){
            victim.mob.removeStatusEffect(StatusEffects.SLOWNESS);victim.mob.timeUntilRegen=0;victim.mob.hurtTime=0;u.abilityAt=0;
            Combat.resolve(b,u,victim.mob,3f,kind,false);return slowAt(victim.mob,0);
        }
        /** A shot of the Harpooner as it would loose it: the real attack, so the trident flies and lands from the queue. */
        void loose(GravesAndGlory.Battle b,GravesAndGlory.Unit u,GravesAndGlory.Unit at){Abilities.attack(b,u,at.mob,b.time);}

        /** A clear line across map 0's river 24 or more blocks from every bridge: its z, or NaN. Scored by what stands on it (trees, walls), then by nearness to a bridge (a shorter detour for a knight). */
        double fordLine(GravesAndGlory.Battle b){
            var bridges=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();
            double best=Double.NaN,bestScore=1e18;
            for(int z=-40;z<=60;z++){
                double near=1e9;for(var br:bridges)near=Math.min(near,Math.abs(z-br.z())-br.depth()/2.0);
                if(near<24)continue;
                int rx=(int)Math.round(Arena.riverX(z)),blocked=0;
                for(int x=rx-24;x<=rx+30;x+=2)for(int dz=-4;dz<=4;dz+=2){
                    int zz=z+dz;if(Arena.river(x,zz))continue;
                    if((int)Phase1Checks.top(b,x+.5,zz+.5)>Arena.ground(x,zz)+1)blocked++;
                }
                double score=blocked*1000+near;
                if(score<bestScore){bestScore=score;best=z;}
            }
            return bestScore<3000?best:Double.NaN;
        }
        /** The distance from a crossing at z to the nearest bridge's edge. */
        static double fromBridge(double z){
            double near=1e9;for(var br:Spec.STRUCTURES)if(br.kind().equals("bridge"))near=Math.min(near,Math.abs(z-br.z())-br.depth()/2.0);
            return near;
        }

        /** Two lanes across the platform for the speed check: Dawn's Raider (at the given Riptide tier) in the north lane, Dusk's (tier 0) in the south one, both under a Move order to the far end. */
        void lanes(GravesAndGlory.Battle b,boolean water,int tier){
            clearFixture(b,"lanes");unbuild(b);
            if(water)for(int lane:new int[]{-4,4}){
                for(int x=-12;x<=12;x++){
                    boolean cap=Math.abs(x)==12;
                    for(int r=-2;r<=2;r++){
                        BlockPos col=new BlockPos(site.getX()+x,0,site.getZ()+lane+r);boolean wall=Math.abs(r)==2||cap;
                        put(b,col.withY(site.getY()-3),Blocks.STONE.getDefaultState());
                        put(b,col.withY(site.getY()-2),wall?Blocks.STONE.getDefaultState():Blocks.WATER.getDefaultState());
                        if(!wall){put(b,col.withY(site.getY()-1),Blocks.WATER.getDefaultState());put(b,col.withY(site.getY()),Blocks.STONE.getDefaultState());} // a roof, so a swimmer's jump cannot lift it out of the water
                    }
                }
            }
            dawn.order="move";dusk.order="move";dawn.formation="block";dusk.formation="block";
            double dy=water?-2:0;
            u1=liveAt(b,dawn,"tc_raider",-10.5,dy,-4,-90);u2=liveAt(b,dusk,"tc_raider",-10.5,dy,4,-90);
            dawn.traits.put("tidecallers",tier);dusk.traits.put("tidecallers",0);
            standAt(b,dawn,u1,pos(10.5,0,-4),-90);standAt(b,dusk,u2,pos(10.5,0,4),-90);
            wetTicks=0;
        }
        /** Each tick of a lane stage: how often the north Raider touched water; true at `window` ticks after the 12-tick start-up, with the two distances walked (xa, xb) set. */
        boolean laneWatch(GravesAndGlory.Battle b,int window){
            long t=since(b);
            if(t==12){was[0]=u1.mob.getPos();was[1]=u2.mob.getPos();}
            if(t>12&&u1.mob.isTouchingWater())wetTicks++;
            if(t<12+window)return false;
            xa=u1.mob.getX()-was[0].x;xb=u2.mob.getX()-was[1].x;return true;
        }

        public void cleanup(MinecraftClient c){
            server(c,b->{Houses.forceRise=null;Abilities.clearBanners(b);Projectiles.clear(b);b.world.setRainGradient(0f);b.world.setWeather(6000,0,false,false);
                clearFixture(b,"cleanup");unbuild(b);if(site!=null)platform(b,site,12,Blocks.AIR.getDefaultState());unforce(b);if(cost!=null)cost.handBack();
                var p0=sp(b);if(p0!=null)p0.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
                if(b.running)b.campaign.returnLobby();var p=sp(b);if(p!=null&&classBefore!=null)b.campaign.handle(p,"class:"+classBefore);});
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;next(b,1);});}
            if(wantShot!=null&&!shotTaken&&++shotDelay>=shotAfter){shot(c,wantShot);shotTaken=true;}
            if(stage==99&&!finished){finished=true;c.setScreen(null);finish(c,"tidecallers");}
            if(tick>30000&&!finished){finished=true;report("p2-tidecallers-timeout",false,"stuck in stage "+stage);finish(c,"tidecallers");}
        }

        public void serverTick(GravesAndGlory.Battle b){
            if(player==null||stage>=99)return;
            try{stages(b);}catch(RuntimeException e){report("p2-tidecallers-exception",false,"stage "+stage+": "+e);e.printStackTrace();next(b,99);}
        }
        void stages(GravesAndGlory.Battle b){
            hold();
            switch(stage){
                case 1->{ // p2-tidecallers-mobs, the sheet and the classes: the entities the sheet names are the ones registered, and they carry only a SwimGoal
                    var w=b.world;boolean good2=true;StringBuilder s=new StringBuilder();var armor=net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR;
                    Map<String,EntityType<?>> types=Map.<String,EntityType<?>>of("drowned",EntityType.DROWNED,"evoker",EntityType.EVOKER,"ravager",EntityType.RAVAGER);
                    Map<String,Class<?>> classes=Map.<String,Class<?>>of("drowned",GravesAndGlory.ArmyDrowned.class,"evoker",GravesAndGlory.ArmyEvoker.class,"ravager",GravesAndGlory.ArmyRavager.class);
                    for(var line:List.of("tc_raider:drowned","tc_harpooner:drowned","tc_tideguard:drowned","tc_mist:evoker","tc_brute:ravager","tc_admiral:drowned")){
                        String troop=line.substring(0,line.indexOf(':')),entity=line.substring(line.indexOf(':')+1);var row=Campaign.troop(troop);
                        boolean sheet=row!=null&&row.entity().equals(entity);MobEntity m=null;String goals,targets,made;
                        try{m=ArmyMobs.create(row.entity(),w);made=m.getClass().getSimpleName();goals=Engine.goalNames(m,"goalSelector").toString();targets=Engine.goalNames(m,"targetSelector").toString();}
                        catch(ReflectiveOperationException|RuntimeException e){goals=targets="unreadable";made=e.toString();}
                        boolean good=sheet&&m!=null&&classes.get(entity).isInstance(m)&&ArmyMobs.isTroop(m)&&m.getType()==types.get(entity)&&goals.equals("[SwimGoal]")&&targets.equals("[]")&&m.getAttributeBaseValue(armor)==0;
                        good2&=good;s.append(troop).append(" (sheet entity ").append(row==null?"-":row.entity()).append(", want ").append(entity).append(") -> ").append(made).append(", goals ").append(goals).append(", targets ").append(targets).append(good?"":" (BAD)").append("; ");
                    }
                    mobsOk=good2;mobsWhy=s.toString();next(b,4);
                }
                case 4->{ // the commander is a Warden for the rest of the suite
                    var p=sp(b);if(p==null)return;if(b.running){b.campaign.returnLobby();return;}
                    classBefore=profile(b).commander;b.campaign.handle(p,"class:warden");
                    if(!profile(b).commander.equals("warden")){report("p2-tidecallers-class",false,"class:warden never reached the profile (it says "+profile(b).commander+")");next(b,99);return;}
                    next(b,5);
                }
                case 5->{var p=sp(b);if(p==null||!Phase1Checks.skirmish(b,p,0,1,"normal"))return;next(b,6);}
                case 6->{if(since(b)<10)return;stageFixture(b);next(b,10);}
                case 10->{ // the live mobs: a Raider, a Harpooner, a Mist Caller, the Brute and the Admiral (AI on) against a pinned tough knight
                    dawn.order="attack";dusk.order="hold";fangs=vexes=wild=visuals=clouds=roars=0;
                    us[0]=tough(bare(post(b,dusk,"knight",7,0,180,true)));foeHp=us[0].mob.getHealth();dawn.target=us[0].mob.getPos();
                    u1=live(b,dawn,"tc_raider",-6,-3,-90);u2=live(b,dawn,"tc_harpooner",-4,3,-90);u3=live(b,dawn,"tc_mist",-8,0,-90);u4=live(b,dawn,"tc_brute",-6,7,-90);u5=live(b,dawn,"tc_admiral",-6,-7,-90);
                    next(b,11);
                }
                case 11->{ // 140 ticks: nothing a vanilla goal would do (fangs, vexes, a thrown trident, a roar or stun), and the Harpooner's flying trident and the Mist Caller's cloud are ours
                    var box=new net.minecraft.util.math.Box(site).expand(40);
                    fangs=Math.max(fangs,b.world.getEntitiesByType(EntityType.EVOKER_FANGS,box,e->true).size());
                    vexes=Math.max(vexes,b.world.getEntitiesByType(EntityType.VEX,box,e->true).size());
                    for(var t:b.world.getEntitiesByType(EntityType.TRIDENT,box,e->true)){if(Projectiles.owns(b,t))visuals++;else wild++;}
                    clouds=Math.max(clouds,b.mists.size());
                    if(u4.mob instanceof GravesAndGlory.ArmyRavager r)roars=Math.max(roars,Math.max(r.getRoarTick(),Math.max(r.getStunTick(),r.getAttackTick())));
                    if(since(b)<140)return;
                    boolean hit=us[0].mob.getHealth()<foeHp;
                    report("p2-tidecallers-mobs",mobsOk&&fangs==0&&vexes==0&&wild==0&&roars==0&&hit&&visuals>0&&clouds>0,mobsWhy+"over 140 ticks with a Raider, a Harpooner, a Mist Caller, the Brute and the Admiral (AI on) against a pinned knight: evoker fangs seen at most "+fangs+" (want 0), vexes "+vexes+" (want 0), tridents in the air that no queued shot owns "+wild+" (want 0), the ravager's roar, stun or attack tick at most "+roars+" (want 0); the Harpooner's own flying trident was seen "+visuals+" times and the Mist Caller's cloud "+clouds+" (want both some); the knight lost "+fmt(foeHp-us[0].mob.getHealth())+" health (want some)="+hit);
                    b.mists.clear();Projectiles.clear(b);clearFixture(b,"mobs");next(b,12);
                }
                case 12->{ // p2-brine-longest, tier by tier: a Raider (brine 20), a Harpooner (harpoon 30), the Brute (Riptide alone) and an Order Squire of the same army (nothing) hit a tough knight
                    dawn.order="hold";dusk.order="hold";
                    u1=post(b,dawn,"tc_raider",-3,-3,-90,true);u2=post(b,dawn,"tc_harpooner",-3,0,-90,true);u3=post(b,dawn,"tc_brute",-3,3,-90,true);u4=post(b,dawn,"knight",-3,6,-90,true);
                    us[0]=tough(bare(post(b,dusk,"knight",0,0,180,true)));
                    int[][] want={{20,30,-1,-1},{20,30,20,-1},{30,30,30,-1},{40,40,40,-1}};
                    StringBuilder s=new StringBuilder();boolean good=true;
                    var brine=Abilities.row(u1.spec);var harpoon=Abilities.row(u2.spec);
                    boolean sheet=brine.mechanic().equals("onhit_slow")&&brine.statusTicks()==20&&brine.statusLevel()==0&&harpoon.mechanic().equals("onhit_slow")&&harpoon.statusTicks()==30&&harpoon.statusLevel()==0&&near(harpoon.knockback(),-1)&&harpoon.cooldownTicks()==60;
                    for(int t=0;t<=3;t++){
                        dawn.traits.put("tidecallers",t);var row=CounterMath.trait(Spec.HOUSETRAITS,"tidecallers",t);
                        int[] got={read(b,u1,us[0],"melee"),read(b,u2,us[0],"arrow"),read(b,u3,us[0],"melee"),read(b,u4,us[0],"melee")};
                        boolean fords=Houses.fords(u1)==(t>=2)&&!Houses.fords(u4);
                        boolean row_=t==0?row==null:row!=null&&row.mechanic().equals("riptide")&&row.durationTicks()==20+10*(t-1);
                        boolean tier=Arrays.equals(got,want[t])&&fords&&row_;good&=tier;
                        s.append("tier ").append(t).append(": Raider ").append(got[0]).append(", Harpooner ").append(got[1]).append(", Brute ").append(got[2]).append(", Order Squire ").append(got[3]).append(" ticks of Slowness I (want ").append(Arrays.toString(want[t])).append("; -1 is none), fording=").append(Houses.fords(u1)).append(tier?"":" (BAD)").append("; ");
                    }
                    report("p2-brine-longest",sheet&&good,"brine "+brine.statusTicks()+" ticks, harpoon "+harpoon.statusTicks()+" ticks, cooldown "+harpoon.cooldownTicks()+", knockback "+harpoon.knockback()+" on the sheet="+sheet+"; "+s+"the longer of brine, harpoon and Riptide stands, and the Brute and the Squire (no brine) slow only by Riptide");
                    dawn.traits.put("tidecallers",0);clearFixture(b,"brine");next(b,14);
                }
                case 14->{ // p2-harpoon-pull: three Harpooners loose for real; the first target is 8 off, the second 2.2 off, the third cannot be moved
                    dawn.order="hold";dusk.order="hold";Projectiles.clear(b);
                    u1=post(b,dawn,"tc_harpooner",-8,0,-90,true);u2=post(b,dawn,"tc_harpooner",-8,-6,-90,true);u3=post(b,dawn,"tc_harpooner",-8,6,-90,true);
                    us[0]=bare(post(b,dusk,"knight",0,0,180,true));us[1]=bare(post(b,dusk,"knight",-5.8,-6,180,true));us[2]=bare(post(b,dusk,"knight",0,6,180,true));
                    for(int i=0;i<3;i++){unpin(tough(us[i]));was[i]=us[i].mob.getPos();}
                    us[2].mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                    d0=distance(u1.mob.getPos(),was[0]);d1=distance(u2.mob.getPos(),was[1]);d2=distance(u3.mob.getPos(),was[2]);
                    u4=post(b,dusk,"archer",-8,-10,90,true);u5=post(b,dusk,"mage",-8,10,90,true);us[6]=post(b,dawn,"tc_raider",-11,-11,-90,true);us[7]=post(b,dawn,"tc_tideguard",-11,11,-90,true);
                    for(var u:List.of(u1,u2,u3))u.abilityAt=0;
                    int before=b.shots.size();
                    loose(b,u1,us[0]);loose(b,u2,us[1]);loose(b,u3,us[2]);
                    boolean flying=b.shots.size()==before+3&&b.shots.stream().allMatch(sh->sh.visual() instanceof net.minecraft.entity.projectile.TridentEntity t&&t.getType()==EntityType.TRIDENT&&t.getOwner()==sh.shooter().mob);
                    pullOk=flying;pullWhy="three Harpooners loosed: each shot in the queue is a flying trident owned by its Harpooner="+flying+" (the look: Harpooner "+Abilities.visual(u1)+", Order Archer "+Abilities.visual(u4)+", Sunweaver "+Abilities.visual(u5)+", Raider "+Abilities.visual(us[6])+" and Trident Guard "+Abilities.visual(us[7])+"; want trident, arrow, bolt, arrow, arrow)";
                    pullOk&=Abilities.visual(u1).equals("trident")&&Abilities.visual(u4).equals("arrow")&&Abilities.visual(u5).equals("bolt")&&Abilities.visual(us[6]).equals("arrow")&&Abilities.visual(us[7]).equals("arrow");
                    next(b,15);
                }
                case 15->{ // the first volley has landed: 1 block closer on the line, Slowness I for the harpoon's 30; not closer than 1.5; none for a target that resists all knockback
                    if(!b.shots.isEmpty()){if(since(b)>200){report("p2-harpoon-pull",false,"the harpoon shots never landed");next(b,99);}return;}
                    double m0=d0-distance(u1.mob.getPos(),us[0].mob.getPos()),m1=d1-distance(u2.mob.getPos(),us[1].mob.getPos()),m2=d2-distance(u3.mob.getPos(),us[2].mob.getPos());
                    double side=Math.abs(us[0].mob.getZ()-was[0].z);int slow=slowAt(us[0].mob,0);
                    boolean ent=b.world.getEntitiesByType(EntityType.TRIDENT,new net.minecraft.util.math.Box(site).expand(40),e->true).isEmpty();
                    boolean one=m0>=0.8&&m0<=1.1&&side<0.1&&slow>=26&&slow<=30;
                    boolean two=m1>0.3&&m1<=0.75&&distance(u2.mob.getPos(),us[1].mob.getPos())>=1.49;
                    boolean three=Math.abs(m2)<0.05;
                    boolean cd=u1.abilityAt>b.time&&u1.abilityAt<=b.time+60;
                    pullOk&=one&&two&&three&&ent&&cd;
                    pullWhy+="; after the first volley the target 8 off moved "+fmt(m0)+" toward its Harpooner along the line (want 0.8 to 1.1; sideways "+fmt(side)+") with "+slowText(us[0].mob)+" (want about 30 of Slowness I)="+one+"; the target 2.2 off moved "+fmt(m1)+" and stands "+fmt(distance(u2.mob.getPos(),us[1].mob.getPos()))+" off (want not nearer than 1.5)="+two+"; the target with knockback resistance 1 moved "+fmt(m2)+" (want 0)="+three+"; the trident entities are gone after landing="+ent+" and the Harpooner's cooldown runs to +"+(u1.abilityAt-b.time)+" (want up to 60)="+cd;
                    cooldownEnd=u1.abilityAt;d3=distance(u1.mob.getPos(),us[0].mob.getPos());pullChecked=false;
                    loose(b,u1,us[0]); // inside the cooldown: no pull, no new slow
                    next(b,16);
                }
                case 16->{
                    if(!b.shots.isEmpty())return;
                    if(!pullChecked){ // once: the shot that was loosed inside the cooldown has landed
                        pullChecked=true;
                        double m=d3-distance(u1.mob.getPos(),us[0].mob.getPos());
                        boolean none=m<0.2&&u1.abilityAt==cooldownEnd;
                        pullOk&=none;pullWhy+="; a second shot inside the cooldown moved the target "+fmt(m)+" (want it not pulled: knockback pushes it away) and left the cooldown where it was="+none;
                    }
                    if(b.time<cooldownEnd)return;
                    d3=distance(u1.mob.getPos(),us[0].mob.getPos());loose(b,u1,us[0]);next(b,17);
                }
                case 17->{ // after the cooldown it pulls again
                    if(!b.shots.isEmpty())return;
                    double m=d3-distance(u1.mob.getPos(),us[0].mob.getPos());boolean again=m>=0.8&&m<=1.1;
                    report("p2-harpoon-pull",pullOk&&again,pullWhy+"; a shot after the cooldown pulled it "+fmt(m)+" (want 0.8 to 1.1)="+again);
                    Projectiles.clear(b);clearFixture(b,"harpoon");next(b,18);
                }
                case 18->{ // p2-undertow: the Admiral, with only a knight out of reach (8.5 off) then two within it (3.0 and 6.2) beside an ally 4 off
                    dawn.order="hold";dusk.order="hold";
                    u1=post(b,dawn,"tc_admiral",0,0,-90,true);u1.abilityAt=0;
                    var row=Abilities.row(u1.spec);
                    boolean sheet=row.id().equals("undertow")&&row.mechanic().equals("slam")&&row.trigger().equals("timer")&&row.cooldownTicks()==200&&near(row.radius(),7)&&near(row.amount(),4)&&row.statusTicks()==60&&row.statusLevel()==1&&near(row.knockback(),-3);
                    us[2]=tough(bare(post(b,dusk,"knight",-8.5,0,180,true)));unpin(us[2]);
                    mark=Combat.LAST.peekLast();Abilities.tick(b,u1,null,b.time);
                    boolean idle=u1.abilityAt==0&&Gilded.hitOn(us[2].mob,mark)==null&&us[2].mob.getHealth()==us[2].mob.getMaxHealth();
                    us[0]=tough(bare(post(b,dusk,"knight",3,0,180,true)));us[1]=tough(bare(post(b,dusk,"knight",0,6.2,180,true)));u2=post(b,dawn,"knight",0,-4,0,true);
                    for(var x:new GravesAndGlory.Unit[]{us[0],us[1],u2})unpin(x);
                    for(int i=0;i<3;i++)was[i]=us[i].mob.getPos();Vec3d ally=u2.mob.getPos();
                    mark=Combat.LAST.peekLast();t0=b.time;Abilities.tick(b,u1,null,b.time);
                    var h0=Gilded.hitOn(us[0].mob,mark);var h1=Gilded.hitOn(us[1].mob,mark);var ha=Gilded.hitOn(u2.mob,mark);var hout=Gilded.hitOn(us[2].mob,mark);
                    double g0=distance(u1.mob.getPos(),us[0].mob.getPos()),g1=distance(u1.mob.getPos(),us[1].mob.getPos());
                    boolean hit=h0!=null&&h1!=null&&Math.abs(h0.base()-4*b.campaign.damage(u1))<1e-4&&Math.abs(h1.base()-4*b.campaign.damage(u1))<1e-4&&us[0].mob.getMaxHealth()-us[0].mob.getHealth()>=h0.base()*h0.total()-1e-2&&us[1].mob.getMaxHealth()-us[1].mob.getHealth()>=h1.base()*h1.total()-1e-2;
                    boolean pulled=Math.abs(g0-1.5)<0.15&&Math.abs(g1-3.2)<0.15&&Math.abs(us[0].mob.getZ()-was[0].z)<0.1&&Math.abs(us[1].mob.getX()-was[1].x)<0.1;
                    boolean slowed=slowAt(us[0].mob,1)==60&&slowAt(us[1].mob,1)==60;
                    boolean untouched=ha==null&&hout==null&&u2.mob.getPos().equals(ally)&&us[2].mob.getPos().distanceTo(was[2])<1e-6&&slowAt(u2.mob,1)==-1&&slowAt(us[2].mob,1)==-1;
                    boolean cd=u1.abilityAt==t0+200;
                    tideOk=sheet&&idle&&hit&&pulled&&slowed&&untouched&&cd;
                    tideWhy="the Admiral's row "+row.id()+" ("+row.mechanic()+" on a "+row.trigger()+", cooldown "+row.cooldownTicks()+", radius "+row.radius()+", amount "+row.amount()+", "+row.statusTicks()+" ticks of Slowness level "+(row.statusLevel()+1)+", knockback "+row.knockback()+")="+sheet+"; with only a knight 8.5 off it did nothing and spent no cooldown="+idle+"; with a knight 3.0 off and one 6.2 off the timer fired: they took "+show(h0)+" and "+show(h1)+" (base "+(h0==null?"-":fmt(h0.base()))+", want "+fmt(4*b.campaign.damage(u1))+")="+hit+", stand "+fmt(g0)+" and "+fmt(g1)+" off her now (want 1.5, the nearest a pull goes, and 3.2)="+pulled+", with "+slowText(us[0].mob)+" and "+slowText(us[1].mob)+" (want Slowness 2 for 60)="+slowed+"; the ally 4 off and the knight 8.5 off took no hit and did not move="+untouched+"; the next cast is at +"+(u1.abilityAt-t0)+" (want 200)="+cd;
                    next(b,19);
                }
                case 19->{ // inside the cooldown nothing happens; once it is up it fires again
                    boolean due=b.time>=u1.abilityAt;
                    mark=Combat.LAST.peekLast();Abilities.tick(b,u1,null,b.time);
                    var again=Gilded.hitOn(us[0].mob,mark);
                    if(!due){tideOk&=again==null&&u1.abilityAt==t0+200;return;}
                    boolean second=again!=null&&u1.abilityAt==b.time+200&&slowAt(us[0].mob,1)==60;
                    report("p2-undertow",tideOk&&second,tideWhy+"; at +"+(b.time-t0)+" (the cooldown up) it fired again (a hit on the knight "+show(again)+", the next cast at +"+(u1.abilityAt-b.time)+" and every tick before that did nothing)="+second);
                    clearFixture(b,"undertow");
                    // p2-pull-wall (Task 9 review, minor 4): a pull into a wall stops at the wall. The Admiral again, a fresh knight 4.5 off and a stone wall 3 off between them (two blocks high): the undertow's 3-block pull leaves the knight against the wall, not in it
                    u1=post(b,dawn,"tc_admiral",0,0,-90,true);u1.abilityAt=0;
                    BlockPos wall=site.add(3,0,0);List<BlockPos> stones=List.of(wall,wall.up(),wall.north(),wall.north().up(),wall.south(),wall.south().up());
                    for(var pos:stones)b.world.setBlockState(pos,Blocks.STONE.getDefaultState());
                    var walled=tough(bare(post(b,dusk,"knight",4.5,0,180,true)));unpin(walled);Vec3d from=walled.mob.getPos();
                    mark=Combat.LAST.peekLast();Abilities.tick(b,u1,null,b.time);
                    var hw=Gilded.hitOn(walled.mob,mark);double moved=distance(from,walled.mob.getPos()),gap=distance(u1.mob.getPos(),walled.mob.getPos());
                    boolean inWall=stones.stream().anyMatch(pos->walled.mob.getBoundingBox().intersects(new net.minecraft.util.math.Box(pos)));
                    boolean stopped=hw!=null&&moved>=0.5&&moved<=0.75&&Math.abs(gap-3.8)<0.15&&!inWall&&Math.abs(walled.mob.getZ()-from.z)<0.1;
                    report("p2-pull-wall",stopped,"a knight 4.5 from the Admiral with a stone wall 3 from her between them took the undertow "+show(hw)+" and was pulled "+fmt(moved)+" toward her (want 0.5 to 0.75: the 0.7 to the wall's face; a clear pull brings it to 1.5 of her): it stands "+fmt(gap)+" off (want 3.8), against the wall and not inside it="+!inWall+"="+stopped);
                    for(var pos:stones)b.world.setBlockState(pos,Blocks.AIR.getDefaultState());
                    clearFixture(b,"pull-wall");next(b,20);
                }
                case 20->{ // p2-sweep: the Brute's blow on a knight with three more within 3 of it (1.5, 1.8 and 2.0 off) and one 3.4 off; an ally beside it
                    dawn.order="hold";dusk.order="hold";
                    u1=post(b,dawn,"tc_brute",0,-3,0,true);u1.abilityAt=0;
                    us[5]=tough(bare(post(b,dusk,"knight",-9,9,180,true))); // a lone knight: a sweep that strikes nobody keeps its cooldown
                    var row=Abilities.row(u1.spec);
                    boolean sheet=row.id().equals("tentacle_sweep")&&row.mechanic().equals("slam")&&row.trigger().equals("onhit")&&near(row.radius(),3)&&near(row.amount(),0.7)&&near(row.knockback(),1)&&row.cooldownTicks()==60;
                    float base=(float)Abilities.baseHit(b,u1);strike(b,u1,us[5],base);
                    boolean lone=u1.abilityAt==0;
                    us[0]=tough(bare(post(b,dusk,"knight",0,0,180,true)));us[1]=tough(bare(post(b,dusk,"knight",1.5,0,180,true)));us[2]=tough(bare(post(b,dusk,"knight",-1.8,0.3,180,true)));us[3]=tough(bare(post(b,dusk,"knight",0.4,2.0,180,true)));us[4]=tough(bare(post(b,dusk,"knight",3.4,0,180,true)));
                    u2=tough(post(b,dawn,"knight",0,1.0,0,true));
                    for(int i=1;i<=3;i++)unpin(us[i]);
                    for(int i=0;i<5;i++)hp[i]=us[i].mob.getHealth();float allyHp=u2.mob.getHealth();
                    mark=Combat.LAST.peekLast();t0=b.time;strike(b,u1,us[0],base);
                    var onFirst=Gilded.hitOn(us[0].mob,mark);float l0=hp[0]-us[0].mob.getHealth();StringBuilder s=new StringBuilder();boolean share=onFirst!=null&&l0>0;
                    for(int i=1;i<=3;i++){
                        var log=Gilded.hitOn(us[i].mob,mark);float li=hp[i]-us[i].mob.getHealth();
                        Vec3d d=us[i].mob.getPos().subtract(us[0].mob.getPos()),v=us[i].mob.getVelocity();double speed=Math.hypot(v.x,v.z),dot=d.x*v.x+d.z*v.z;
                        boolean good=log!=null&&Math.abs(log.base()-onFirst.base()*0.7)<1e-4&&Math.abs(li-0.7f*l0)<1e-3&&dot>0&&speed>0.5;
                        share&=good;s.append("knight ").append(i).append(" ").append(fmt(distance(us[i].mob.getPos(),us[0].mob.getPos()))).append(" off lost ").append(fmt(li)).append(" (want ").append(fmt(0.7*l0)).append(") thrown away at ").append(fmt(speed)).append(good?"":" (BAD)").append("; ");
                    }
                    boolean outside=Gilded.hitOn(us[4].mob,mark)==null&&us[4].mob.getHealth()==hp[4]&&Gilded.hitOn(u2.mob,mark)==null&&u2.mob.getHealth()==allyHp;
                    boolean cd=u1.abilityAt==t0+60;
                    sweepOk=sheet&&lone&&share&&outside&&cd;
                    sweepWhy="the Brute's row "+row.id()+" ("+row.mechanic()+" on "+row.trigger()+", radius "+row.radius()+", share "+row.amount()+", knockback "+row.knockback()+", cooldown "+row.cooldownTicks()+")="+sheet+"; its blow on a lone knight swept nobody and kept the cooldown ready="+lone+"; its blow on the first knight "+show(onFirst)+" (it lost "+fmt(l0)+"): "+s+"the knight 3.4 off and an ally 1.0 off took no hit="+outside+"; the next sweep is at +"+(u1.abilityAt-t0)+" (want 60)="+cd;
                    for(int i=0;i<5;i++)hp[i]=us[i].mob.getHealth();
                    next(b,21);
                }
                case 21->{ // inside the cooldown a blow sweeps nobody
                    if(since(b)<6)return;
                    for(int i=1;i<=3;i++)unpin(us[i]);
                    mark=Combat.LAST.peekLast();strike(b,u1,us[0],(float)Abilities.baseHit(b,u1));
                    boolean none=true;for(int i=1;i<=3;i++)none&=Gilded.hitOn(us[i].mob,mark)==null&&us[i].mob.getHealth()==hp[i];
                    sweepOk&=none;sweepWhy+="; 6 ticks later another blow on the first knight swept nobody (no hit on the three, none lost health)="+none;
                    next(b,22);
                }
                case 22->{ // once the 60 ticks are up it sweeps again
                    if(b.time-t0<62)return;
                    for(int i=1;i<=3;i++){hp[i]=us[i].mob.getHealth();us[i].mob.timeUntilRegen=0;}
                    mark=Combat.LAST.peekLast();strike(b,u1,us[0],(float)Abilities.baseHit(b,u1));
                    int hit=0;for(int i=1;i<=3;i++)if(Gilded.hitOn(us[i].mob,mark)!=null&&us[i].mob.getHealth()<hp[i])hit++;
                    report("p2-sweep",sweepOk&&hit>=2,sweepWhy+"; at +"+(b.time-t0)+" it swept again and hurt "+hit+" of the three (want at least 2: the knights thrown out of the first ring may have left it)");
                    clearFixture(b,"sweep");next(b,24);
                }
                case 24->{ // p2-mist-zone: the Mist Caller casts at a knight 14 off; a knight 2.5 from it, an ally knight and a Raider inside, a knight 5.5 off outside; archers inside and outside
                    dawn.order="hold";dusk.order="attack";b.mists.clear();
                    u1=post(b,dawn,"tc_mist",-9,0,-90,true);u1.abilityAt=0;
                    us[0]=post(b,dusk,"knight",5,0,180,true);us[1]=post(b,dusk,"knight",7.5,0,180,true);us[2]=post(b,dusk,"knight",5,5.5,180,true);
                    u2=post(b,dawn,"tc_raider",5,-1.5,-90,true);u3=post(b,dawn,"tc_raider",-2,-10,-90,true); // u2 inside, u3 outside
                    us[3]=bare(post(b,dusk,"archer",5,3,90,true));us[4]=bare(post(b,dusk,"archer",5,-10,90,true));
                    var row=Abilities.mistRow();
                    boolean sheet=row!=null&&row.id().equals("mist")&&near(row.radius(),4)&&row.windupTicks()==20&&row.cooldownTicks()==160&&row.durationTicks()==100&&row.statusTicks()==20&&row.statusLevel()==0;
                    t0=b.time;Abilities.attack(b,u1,us[0].mob,b.time);
                    boolean began=u1.spellCenter!=null&&u1.windupUntil==t0+20&&u1.abilityAt==t0+160&&u1.spellCenter.distanceTo(us[0].mob.getPos())<1e-6;
                    mistOk=sheet&&began;cloudAt=-1;cloud=null;m1=m2=m3=m4=false;
                    mistWhy="the Mist Caller's row "+(row==null?"none":row.id()+" (radius "+row.radius()+", windup "+row.windupTicks()+", cooldown "+row.cooldownTicks()+", lasts "+row.durationTicks()+", slow "+row.statusTicks()+" ticks of level "+(row.statusLevel()+1)+")")+"="+sheet+"; the cast at a knight 14 off began with its windup to +20 and its next cast at +160="+began+"; ";
                    next(b,25);
                }
                case 25->{ // each tick: the spell runs; the cloud stands when the windup ends; a beat later the slow, the counters; at the end the cloud thins away
                    long t=b.time-t0;
                    if(u1.spellCenter!=null)Abilities.tick(b,u1,us[0].mob,b.time);
                    if(cloudAt<0&&!b.mists.isEmpty()){
                        cloudAt=t;cloud=b.mists.get(0);
                        boolean made=b.mists.size()==1&&cloud.team()==0&&near(cloud.radius(),4)&&cloud.until()==b.time+100&&t==20&&cloud.at().distanceTo(us[0].mob.getPos())<1e-6;
                        mistOk&=made;mistWhy+="the cloud stood "+t+" ticks after the cast (want 20) at the knight's spot, team "+cloud.team()+", radius "+cloud.radius()+", gone at +"+(cloud.until()-t0)+" (want +120)="+made+"; ";
                    }
                    if(cloudAt<0){if(t>60){report("p2-mist-zone",false,mistWhy+"no cloud stood within 60 ticks");next(b,99);}return;}
                    if(t>=cloudAt+12&&!m1){m1=true; // one beat has passed
                        boolean slowed=slowAt(us[0].mob,0)>=1&&slowAt(us[0].mob,0)<=20&&slowAt(us[1].mob,0)>=1&&slowAt(us[3].mob,0)>=1;
                        boolean spared=slowAt(us[2].mob,0)==-1&&slowAt(us[4].mob,0)==-1&&slowAt(u2.mob,0)==-1&&slowAt(u3.mob,0)==-1;
                        mistOk&=slowed&&spared;
                        mistWhy+="a beat later the enemies in the cloud (the knight at its centre, one 2.5 off, an archer 3 off) read "+slowText(us[0].mob)+", "+slowText(us[1].mob)+", "+slowText(us[3].mob)+" (want Slowness 1, at most 20 ticks)="+slowed+"; the knight 5.5 off, the archer 10 off and the two Raiders of the caster's own army read "+slowText(us[2].mob)+", "+slowText(us[4].mob)+", "+slowText(u2.mob)+", "+slowText(u3.mob)+" (want none)="+spared+"; ";
                        Combat.HitLog in=Commanders.hit(b,us[3],u2.mob,"arrow"),into=Commanders.hit(b,us[4],u2.mob,"arrow"),out=Commanders.hit(b,us[4],u3.mob,"arrow"),melee=Commanders.hit(b,us[1],u2.mob,"melee"),bolt=Commanders.hit(b,us[3],u2.mob,"bolt");
                        boolean counters=Gilded.is(in,0.5,"C17")&&Gilded.is(into,0.5,"C17")&&out!=null&&near(out.counters(),1.0)&&out.rows().isEmpty()&&melee!=null&&near(melee.counters(),1.0)&&!melee.rows().contains("C17")&&Gilded.is(bolt,0.5,"C17");
                        mistOk&=counters;
                        mistWhy+="inside the cloud an Order Archer's arrow "+show(in)+" (want 0.5, C17), one fired into it from outside "+show(into)+" (want 0.5, C17), a bolt from inside "+show(bolt)+" (want 0.5); an arrow with both ends outside "+show(out)+" (want 1.0, no row) and a blow in melee inside "+show(melee)+" (want no C17)="+counters+"; ";
                    }
                    if(t>=cloudAt+60&&!m2){m2=true; // the slow is held fresh
                        boolean fresh=slowAt(us[0].mob,0)>=1;mistOk&=fresh;mistWhy+="halfway through, the knight at the centre still reads "+slowText(us[0].mob)+" (the cloud renews it each beat)="+fresh+"; ";
                    }
                    if(t>=cloudAt+101&&!m3){m3=true;
                        boolean gone=b.mists.isEmpty();var after=Commanders.hit(b,us[3],u2.mob,"arrow");
                        mistOk&=gone&&after!=null&&near(after.counters(),1.0)&&after.rows().isEmpty();
                        mistWhy+="at +"+t+" the cloud is gone from Battle.mists="+gone+" and the same arrow reads "+show(after)+" (want 1.0, no row); ";
                    }
                    if(t>=cloudAt+130&&!m4){m4=true;
                        boolean worn=slowAt(us[0].mob,0)==-1&&slowAt(us[1].mob,0)==-1;
                        report("p2-mist-zone",mistOk&&worn,mistWhy+"30 ticks after it thinned away the slow is off them ("+slowText(us[0].mob)+")="+worn);
                        clearFixture(b,"mist");b.mists.clear();dusk.order="hold";next(b,28);
                    }
                }
                case 28->{ // p2-brace: a Trident Guard on Hold against a charging knight is C18c; not otherwise
                    dawn.order="hold";dusk.order="charge";
                    u1=post(b,dawn,"tc_tideguard",0,-2,0,true);u2=post(b,dawn,"tc_raider",1.5,-2,0,true);u3=post(b,dawn,"knight",-1.5,-2,0,true);
                    us[0]=tough(bare(post(b,dusk,"tc_raider",0,0,180,true)));
                    var row=Campaign.troop("tc_tideguard");boolean sheet=row.ability().equals("brace")&&u1.tags.contains("brace");
                    var braced=Commanders.hit(b,u1,us[0].mob,"melee");
                    var raider=Commanders.hit(b,u2,us[0].mob,"melee");var knight=Commanders.hit(b,u3,us[0].mob,"melee");
                    dusk.order="hold";var calm=Commanders.hit(b,u1,us[0].mob,"melee");
                    dusk.order="charge";dawn.order="attack";var moving=Commanders.hit(b,u1,us[0].mob,"melee");
                    dawn.order="hold";dusk.order="hold";
                    boolean ok1=sheet&&Gilded.is(braced,1.3,"C18c")&&raider!=null&&near(raider.counters(),1.0)&&raider.rows().isEmpty()&&knight!=null&&!knight.rows().contains("C18c")&&calm!=null&&near(calm.counters(),1.0)&&calm.rows().isEmpty()&&moving!=null&&near(moving.counters(),1.0)&&moving.rows().isEmpty();
                    report("p2-brace",ok1,"the Trident Guard (ability "+row.ability()+", tags "+u1.tags+")="+sheet+": on Hold against a charging Raider "+show(braced)+" (want 1.3, C18c); a Raider of its army on Hold "+show(raider)+" and an Order Squire "+show(knight)+" (want no C18c); on Hold against a Raider that is not charging "+show(calm)+" (want 1.0, no row); under the attack order against a charging one "+show(moving)+" (want 1.0, no row)");
                    clearFixture(b,"brace");next(b,50);
                }
                case 50->{ // p2-warden-mark: the Warden looks east along the platform at a knight 12 off; one 7 off his line, a wall between
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"mark");unbuild(b);reset(own);Commander.reset(b,own);own.training.clear();own.order="hold";dawn.order="hold";dusk.order="hold";
                    var c=Commander.row(own);
                    boolean sheet=c.id().equals("warden")&&c.active().equals("mark_prey")&&c.cooldownTicks()==1200&&c.durationTicks()==240&&near(c.range(),40)&&near(c.value1(),0.3);
                    p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);b.relocate(p,site.getX()+.5-10,site.getY(),site.getZ()+.5,-90f,0f);
                    us[0]=bare(post(b,dusk,"knight",2,0,90,true));us[1]=bare(post(b,dusk,"knight",2,7,90,true));
                    u1=post(b,own,"knight",1.0,-2.5,-90,true);u2=post(b,dawn,"knight",1.0,-3.5,-90,true);u3=post(b,own,"knight",1.0,6.0,-90,true);
                    markOk=sheet;markWhy="the Warden's row "+c.id()+"/"+c.active()+" (cooldown "+c.cooldownTicks()+", lasts "+c.durationTicks()+", range "+c.range()+", +"+c.value1()+")="+sheet+"; ";
                    // behind a wall: refused, with a notice, and no cooldown
                    int wx=site.getX()-5;for(int dz=-1;dz<=1;dz++)for(int y=0;y<=3;y++)put(b,new BlockPos(wx,site.getY()+y,site.getZ()+dz),Blocks.STONE.getDefaultState());
                    long readyBefore=own.abilityReadyAt;b.campaign.notices.remove(player);b.command(p,"ability");
                    String refused=b.campaign.notices.get(player);
                    boolean wall=Commander.MarkPrey.pick(b,p,own,40)==null&&"No foe in sight.".equals(refused)&&!Commander.running(b,own,"mark_prey")&&own.abilityReadyAt==readyBefore&&own.abilityData==null;
                    unbuild(b);Commander.reset(b,own);
                    boolean reach=Commander.MarkPrey.pick(b,p,own,10)==null&&Commander.MarkPrey.pick(b,p,own,14)==us[0].mob&&Commander.MarkPrey.pick(b,p,own,40)==us[0].mob;
                    markOk&=wall&&reach;
                    markWhy+="with a stone wall between him and the knight the cast was refused with \""+refused+"\", no mark, no cooldown="+wall+"; the view ray picks the knight 12 off at a range of 14 and 40, nothing at 10, and not the one 7 off the line="+reach+"; ";
                    markAt=b.time;b.campaign.notices.remove(player);b.command(p,"ability");
                    var glow=us[0].mob.getStatusEffect(StatusEffects.GLOWING);
                    boolean cast=Commander.running(b,own,"mark_prey")&&own.abilityData==us[0].mob&&glow!=null&&Math.abs(glow.getDuration()-240)<=2&&own.abilityUntil==markAt+240&&own.abilityReadyAt==markAt+1200&&!us[1].mob.hasStatusEffect(StatusEffects.GLOWING);
                    var own1=Commanders.hit(b,u1,us[0].mob,"melee");var other=Commanders.hit(b,u2,us[0].mob,"melee");var wrong=Commanders.hit(b,u3,us[1].mob,"melee");
                    double self=Commander.playerFactor(b,own,us[0].mob),selfOther=Commander.playerFactor(b,own,us[1].mob);
                    boolean factors=own1!=null&&near(own1.commander(),1.3)&&other!=null&&near(other.commander(),1.0)&&wrong!=null&&near(wrong.commander(),1.0)&&near(self,1.3)&&near(selfOther,1.0);
                    markOk&=cast&&factors;
                    markWhy+="the cast marked the knight (the active runs to +"+(own.abilityUntil-markAt)+" and is ready again at +"+(own.abilityReadyAt-markAt)+", the knight glows "+(glow==null?"not":glow.getDuration()+" ticks")+", the other does not)="+cast+"; a blow from his own Order Squire on it has commander "+(own1==null?"none":String.valueOf(own1.commander()))+" (want 1.3), from another army's Order Squire "+(other==null?"none":String.valueOf(other.commander()))+" (want 1.0), his Order Squire on the knight off the line "+(wrong==null?"none":String.valueOf(wrong.commander()))+" (want 1.0), the Warden's own blow on it x"+fmt(self)+" (want 1.3) and on the other x"+fmt(selfOther)+"="+factors+"; ";
                    next(b,51);
                }
                case 51->{ // 240 ticks on, the mark and its glow are gone
                    if(b.time<markAt+242)return;
                    var h=Commanders.hit(b,u1,us[0].mob,"melee");
                    boolean over=!Commander.running(b,own,"mark_prey")&&!us[0].mob.hasStatusEffect(StatusEffects.GLOWING)&&h!=null&&near(h.commander(),1.0)&&own.abilityData==null;
                    report("p2-warden-mark",markOk&&over,markWhy+"at +"+(b.time-markAt)+" the mark has ended: the glow is gone from the knight and a blow from his troop has commander "+(h==null?"none":String.valueOf(h.commander()))+" (want 1.0)="+over+" (the glow is the vanilla one, so both teams see it: a glow for the Warden's side only needs a client renderer that state_v8 does not carry, as docs/PARKED.md records)");
                    Commander.reset(b,own);clearFixture(b,"mark done");next(b,30);
                }
                case 30->{ // p2-riptide-speed, (a): dry land, no rain: tier I against tier 0 (equal)
                    b.world.setRainGradient(0f);b.world.setWeather(6000,0,false,false);speedWhy="";phase=0;speedOk=true;
                    lanes(b,false,1);next(b,31);
                }
                case 31->{
                    if(!laneWatch(b,60))return;
                    landRatio=xa/xb;landSpeed=xb/60.0;boolean good=xb>3&&Math.abs(landRatio-1.0)<=0.05&&u1.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).getModifier(Houses.RIPTIDE)==null;
                    speedOk&=good;speedWhy+="on dry land (no rain, not in water) over 60 ticks a tier I Raider walked "+fmt(xa)+" blocks against "+fmt(xb)+" for a tier 0 one: ratio "+fmt(landRatio)+" (want 1.00 within 5%) and no Riptide modifier on it="+good+"; ";
                    b.world.setRainGradient(1f);b.world.setWeather(0,6000,true,false);lanes(b,false,1);next(b,32);
                }
                case 32->{
                    if(!laneWatch(b,60))return;
                    var m=u1.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).getModifier(Houses.RIPTIDE);
                    rainRatio=xa/xb;boolean good=b.world.isRaining()&&xb>3&&Math.abs(rainRatio-1.15)<=0.0575&&m!=null&&near(m.getValue(),Math.sqrt(1.15)-1);
                    speedOk&=good;speedWhy+="in the rain on dry land over 60 ticks a tier I Raider walked "+fmt(xa)+" against "+fmt(xb)+" for a tier 0 one (it is raining="+b.world.isRaining()+"; the modifier "+(m==null?"none":String.valueOf(m.getValue()))+", the square root of 1.15 less 1 because a land mob's walk is quadratic in its speed attribute): ratio "+fmt(rainRatio)+" (want 1.15 within 5%)="+good+"; ";
                    b.world.setRainGradient(0f);b.world.setWeather(6000,0,false,false);lanes(b,true,1);next(b,33);
                }
                case 33->{
                    if(!laneWatch(b,100))return;
                    waterRatio=xa/xb;waterSpeed=xb/100.0;boolean wetNow=wetTicks>=90;
                    var m=u1.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED).getModifier(Houses.RIPTIDE);
                    boolean good=wetNow&&xb>1&&Math.abs(waterRatio-1.15)<=0.0575&&m!=null&&near(m.getValue(),0.15);
                    speedOk&=good;speedWhy+="in water (the Raider touched it "+fmt(wetTicks)+" of 100 ticks) a tier I Raider swam "+fmt(xa)+" blocks in 100 ticks against "+fmt(xb)+" for a tier 0 one: ratio "+fmt(waterRatio)+" (want 1.15 within 5%)="+good+"; a tier 0 Raider covers "+fmt(landSpeed*20)+" blocks a second on land and "+fmt(waterSpeed*20)+" in water (vanilla water drag: a literal 15% faster than on land cannot hold, so Riptide is measured against the same troop without it); ";
                    report("p2-riptide-speed",speedOk,speedWhy.trim());
                    clearFixture(b,"lanes done");unbuild(b);b.world.setRainGradient(0f);b.world.setWeather(6000,0,false,false);dawn.traits.put("tidecallers",0);dawn.order="hold";dusk.order="hold";next(b,34);
                }
                case 34->{ // p2-brute-leaves: a wall of leaves across the platform with a gap at its south end; matches run with mobGriefing off, so the Brute cannot smash them and has to go round
                    clearFixture(b,"leaves");unbuild(b);
                    for(int z=-12;z<=8;z++)for(int y=0;y<=3;y++)put(b,new BlockPos(site.getX(),site.getY()+y,site.getZ()+z),Blocks.OAK_LEAVES.getDefaultState().with(net.minecraft.block.LeavesBlock.PERSISTENT,true));
                    dawn.order="move";dawn.formation="block";dawn.traits.put("tidecallers",0);
                    u1=liveAt(b,dawn,"tc_brute",-8,0,0,-90);standAt(b,dawn,u1,pos(8,0,0),-90);
                    next(b,35);
                }
                case 35->{ // the Brute stands on the far side of the wall, round the gap, inside 600 ticks; its leaf penalty is the default (blocked), where a vanilla Ravager's is 0
                    double penalty=u1.mob.getPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.LEAVES);
                    boolean across=u1.mob.getX()>site.getX()+.5+3&&u1.mob.getZ()>site.getZ()-1;
                    if(!across&&since(b)<600)return;
                    report("p2-brute-leaves",across&&penalty<0,"a Brute ordered 16 blocks across a wall of leaves (4 high, the platform's whole width but a 4 block gap at the south end) stood at x "+fmt(u1.mob.getX()-site.getX()-.5)+" z "+fmt(u1.mob.getZ()-site.getZ()-.5)+" after "+since(b)+" ticks (want x beyond 3: it went round the wall)="+across+"; its path penalty for leaves is "+penalty+" (want below 0: a vanilla Ravager's is 0, which sent it into the canopy and left it standing there)="+(penalty<0));
                    clearFixture(b,"leaves done");unbuild(b);dawn.order="hold";next(b,55);
                }
                case 55->{ // the showcase: the commander's Tidecallers army on the platform, the Mist Caller's cloud over a line of seven archers, the Brute about to sweep a line of seven knights
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"showcase");unbuild(b);reset(own);Commander.reset(b,own);own.order="hold";dusk.order="attack";dawn.order="hold";b.mists.clear();
                    u1=post(b,own,"tc_mist",-7,-7,-90,true);u1.abilityAt=0;
                    post(b,own,"tc_harpooner",-10,-10,-90,true);post(b,own,"tc_harpooner",-10,-4,-90,true);post(b,own,"tc_harpooner",-11,-7,-90,true);
                    post(b,own,"tc_tideguard",-3,-9.5,-90,true);post(b,own,"tc_tideguard",-3,-4.5,-90,true);
                    post(b,own,"tc_raider",-5,-12,-90,true);post(b,own,"tc_raider",-5,-2,-90,true);
                    post(b,own,"tc_admiral",-8,3,-90,true);
                    u2=post(b,own,"tc_brute",1.5,6,-90,true);u2.abilityAt=0;
                    for(int i=0;i<7;i++)line[i]=bare(post(b,dusk,"archer",7,-7-3.6+i*1.2,90,true));
                    for(int i=0;i<7;i++)knights[i]=tough(bare(post(b,dusk,"knight",4,6-3.6+i*1.2,180,true)));
                    Houses.recompute(own);
                    p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    Vec3d cam=new Vec3d(site.getX()+.5-16,site.getY()+8.5,site.getZ()+.5+1),look=new Vec3d(site.getX()+.5+2,site.getY()+0.8,site.getZ()+.5+0.5);
                    double dx=look.x-cam.x,dz=look.z-cam.z,dy=look.y-cam.y;
                    b.relocate(p,cam.x,cam.y,cam.z,(float)(-Math.toDegrees(Math.atan2(dx,dz))),(float)(-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)))));
                    t0=b.time;cloudAt=-1;Abilities.attack(b,u1,line[3].mob,b.time);cloudUp=false;swept=0;next(b,56);
                }
                case 56->{ // the cloud stands after 20 ticks; 12 ticks later the Brute sweeps the line
                    long t=b.time-t0;
                    if(u1.spellCenter!=null)Abilities.tick(b,u1,line[3].mob,b.time);
                    if(cloudAt<0&&!b.mists.isEmpty()){cloudAt=t;cloud=b.mists.get(0);cloudUp=true;}
                    if(cloudAt<0){if(t>60){report("p2-tidecallers-showcase",false,"no cloud stood within 60 ticks");next(b,99);}return;}
                    if(t<cloudAt+12)return;
                    int slowed=0;for(var a:line)if(slowAt(a.mob,0)>=1)slowed++;
                    var inside=Commanders.hit(b,line[1],u2.mob,"arrow");boolean c17=Gilded.is(inside,0.5,"C17"); // an archer in the cloud, shooting out
                    for(int i:new int[]{1,2,4,5})unpin(knights[i]);
                    for(int i=0;i<7;i++)hp[i]=knights[i].mob.getHealth();
                    mark=Combat.LAST.peekLast();Abilities.attack(b,u2,knights[3].mob,b.time);
                    swept=0;for(int i:new int[]{1,2,4,5})if(knights[i].mob.getHealth()<hp[i])swept++;
                    boolean outer=knights[0].mob.getHealth()==hp[0]&&knights[6].mob.getHealth()==hp[6];
                    ok=cloudUp&&slowed==7&&c17&&swept==4&&outer&&knights[3].mob.getHealth()<hp[3];
                    why="the Mist Caller's cloud stood "+cloudAt+" ticks after the cast over a line of seven Order Archers: "+slowed+" of them read Slowness (want 7); an archer's arrow from inside it "+show(inside)+" (want 0.5, C17)="+c17+"; the Brute's blow on the middle knight of a line of seven swept "+swept+" of the four within 3 of it (want 4) and neither end knight (untouched="+outer+") and the army fights as "+own.traits+" in its Houses";
                    requestShot("tidecallers-showcase",5);next(b,57);
                }
                case 57->{ // the picture, with the cloud up and the knocked knights in the air
                    if(!shotTaken&&since(b)<40)return;
                    report("p2-tidecallers-showcase",ok&&shotTaken,why+"; the picture tidecallers-showcase was taken="+shotTaken);
                    wantShot=null;b.mists.clear();var p=sp(b);if(p!=null)p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);clearFixture(b,"showcase done");next(b,40);
                }
                case 40->{ // p2-riptide-ford: map 0's river on a line 24+ blocks from every bridge; the commander's Tidecallers army (tidecallers_hard, tier III) and three Order Squires ordered across
                    var p=sp(b);if(p==null)return;
                    clearFixture(b,"ford");reset(own);Commander.reset(b,own);own.training.clear();
                    force(b,new BlockPos(-45,0,-30),new BlockPos(45,0,50));
                    fordZ=fordLine(b);
                    if(Double.isNaN(fordZ)){report("p2-riptide-ford",false,"no clear line across the river 24 blocks from every bridge");next(b,44);return;}
                    int rx=(int)Math.round(Arena.riverX(fordZ));fordX0=rx-6-14;
                    field(b,own,banner("tidecallers_hard"));
                    for(var u:b.units)if(u.squad==own){double px=fordX0+(u.slot%5)*1.5,pz=fordZ+(u.slot/5-1.5)*1.4;u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(px,Phase1Checks.top(b,px,pz),pz,-90f,0f);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;fixture.add(u);}
                    for(int i=0;i<3;i++){double px=fordX0+8+(i%2)*1.5,pz=fordZ-3+i*3;var k=b.spawn(dawn,Campaign.troop("knight"),40+i,ground(b,px,pz));k.mob.refreshPositionAndAngles(px,Phase1Checks.top(b,px,pz),pz,-90f,0f);k.deployed=k.mob.getPos();k.lastPosition=k.mob.getPos();fixture.add(k);}
                    Vec3d goal=new Vec3d(rx+22,65,fordZ);
                    for(var s:List.of(own,dawn)){s.order="move";s.formation="block";s.target=goal;s.yaw=-90;}
                    dusk.order="hold";crossed.clear();waded.clear();fordShot=false;inRiver=0;t0=b.time;
                    p.changeGameMode(net.minecraft.world.GameMode.SPECTATOR);
                    Vec3d cam=new Vec3d(rx-9,Phase1Checks.top(b,rx,fordZ)+12,fordZ-15),look=new Vec3d(rx,63,fordZ+1);
                    double dx=look.x-cam.x,dz=look.z-cam.z,dy=look.y-cam.y;
                    b.relocate(p,cam.x,cam.y,cam.z,(float)(-Math.toDegrees(Math.atan2(dx,dz))),(float)(-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)))));
                    next(b,41);
                }
                case 41->{ // each tick: who has touched the water and where each crossed the river's middle line; the picture when three Raiders are in the water
                    long t=since(b);inRiver=0;
                    for(var u:fixture){
                        if(!u.mob.isAlive())continue;double x=u.mob.getX(),z=u.mob.getZ();
                        if(u.mob.isTouchingWater()){waded.add(u);if(u.spec.id().equals("tc_raider"))inRiver++;}
                        if(!crossed.containsKey(u)&&x>=Arena.riverX(z)+1)crossed.put(u,new double[]{x,z,t});
                    }
                    if(!fordShot&&inRiver>=3){fordShot=true;requestShot("tidecallers-ford",3);}
                    var raiders=fixture.stream().filter(u->u.spec.id().equals("tc_raider")).toList();var squires=fixture.stream().filter(u->u.spec.id().equals("knight")).toList();var mists=fixture.stream().filter(u->u.spec.id().equals("tc_mist")).toList();
                    boolean raidersOver=raiders.stream().allMatch(crossed::containsKey),squiresOver=squires.stream().allMatch(crossed::containsKey),mistsOver=mists.stream().allMatch(crossed::containsKey);
                    if(!(raidersOver&&squiresOver&&mistsOver&&(shotTaken||!fordShot))&&t<1600)return;
                    StringBuilder s=new StringBuilder();boolean good=!raiders.isEmpty()&&squires.size()==3&&own.traits.getOrDefault("tidecallers",0)==3;
                    for(var u:raiders){var c=crossed.get(u);boolean g=c!=null&&fromBridge(c[1])>=20&&waded.contains(u);good&=g;s.append("Raider ").append(u.slot).append(c==null?" never crossed":" crossed at z "+fmt(c[1])+", "+fmt(fromBridge(c[1]))+" from the nearest bridge, at +"+(int)c[2]+(waded.contains(u)?" through the water":" DRY")).append(g?"":" (BAD)").append("; ");}
                    for(var u:squires){var c=crossed.get(u);boolean g=c!=null&&fromBridge(c[1])<=4&&!waded.contains(u);good&=g;s.append("Order Squire ").append(u.slot).append(c==null?" never crossed":" crossed at z "+fmt(c[1])+", "+fmt(fromBridge(c[1]))+" from the nearest bridge, at +"+(int)c[2]+(waded.contains(u)?" THROUGH THE WATER":" dry")).append(g?"":" (BAD)").append("; ");}
                    fordWhy="map 0's river on the line z "+fmt(fordZ)+" ("+fmt(fromBridge(fordZ))+" from the nearest bridge), the Tidecallers army (tidecallers_hard, Riptide tier "+own.traits.getOrDefault("tidecallers",0)+") and three Order Squires ordered across: "+s+"a Raider counts when it crossed 20 or more blocks from every bridge's edge through the water, an Order Squire when it crossed at a bridge without wetting its feet";
                    report("p2-riptide-ford",good&&(shotTaken||!fordShot),fordWhy+(fordShot?"; the picture tidecallers-ford was taken="+shotTaken:"; no picture: fewer than 3 Raiders were in the water together"));
                    StringBuilder ms=new StringBuilder();boolean mistGood=!mists.isEmpty();
                    for(var u:mists){var c=crossed.get(u);boolean g=c!=null&&fromBridge(c[1])>=20&&waded.contains(u);mistGood&=g;ms.append("Mist Caller ").append(u.slot).append(c==null?" never crossed":" crossed at z "+fmt(c[1])+", "+fmt(fromBridge(c[1]))+" from the nearest bridge, at +"+(int)c[2]+(waded.contains(u)?" through the water":" DRY")).append(g?"":" (BAD)").append("; ");}
                    report("p2-mist-ford",mistGood,"(Task 9 review, minor 4) the army's "+mists.size()+" Mist Callers (evokers with only a SwimGoal) at Riptide tier "+own.traits.getOrDefault("tidecallers",0)+" ordered across with the Raiders: "+ms+"a Mist Caller counts when it crossed 20 or more blocks from every bridge's edge through the water");
                    wantShot=null;var p=sp(b);if(p!=null)p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE);
                    for(var u:new ArrayList<>(fixture))Houses.discard(b,u);fixture.clear();pins.clear();audit(b,"ford",false);next(b,44);
                }
                case 44->{ // p2-tidecallers-bridge: Dawn's army of three Raiders and the Brute (tier 0) at a bridge's west end, ordered across; each bridge of map 0 in turn
                    var bridges=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();
                    if(bridgeIdx>=bridges.size()){report("p2-tidecallers-bridge",bridgesOk,bridgeWhy.toString());next(b,70);return;}
                    var br=bridges.get(bridgeIdx);clearFixture(b,"bridge");reset(own);Commander.reset(b,own);own.training.clear();
                    field(b,own,banner("tc_raider*3,tc_brute"));
                    double x0=br.x()-br.width()/2.0-14;
                    force(b,new BlockPos((int)x0-4,0,br.z()-10),new BlockPos(br.x()+br.width()/2+24,0,br.z()+10));
                    own.order="move";own.formation="block";own.target=new Vec3d(br.x()+br.width()/2.0+16,65,br.z());own.yaw=-90;
                    for(var u:b.units)if(u.squad==own){double px=x0+(u.slot%5)*1.5,pz=br.z()+(u.slot/5-1.5)*1.4;u.mob.getNavigation().stop();u.mob.refreshPositionAndAngles(px,Arena.ground((int)px,(int)pz)+1,pz,-90f,0f);u.deployed=u.mob.getPos();u.lastPosition=u.mob.getPos();u.nextPath=0;u.stuckTicks=0;fixture.add(u);if(u.spec.id().equals("tc_brute"))u1=u;}
                    crossedAt=-1;deckMin=1e9;deckTicks=0;wet=false;t0=b.time;next(b,45);
                }
                case 45->{
                    var bridges=Arrays.stream(Spec.STRUCTURES).filter(s->s.kind().equals("bridge")).toList();var br=bridges.get(bridgeIdx);
                    double end=br.x()+br.width()/2.0-4;
                    long across=fixture.stream().filter(u->u.squad==own&&u.mob.isAlive()&&u.mob.getX()>end&&u.mob.getY()>63).count();
                    boolean big=u1!=null&&u1.mob.isAlive()&&u1.mob.getX()>end&&u1.mob.getY()>63;
                    if(big&&crossedAt<0)crossedAt=b.time-t0;
                    if(u1!=null&&u1.mob.isAlive()){
                        if(u1.mob.isTouchingWater())wet=true;
                        if(Math.abs(u1.mob.getX()-br.x())<=br.width()/2.0-1){deckMin=Math.min(deckMin,u1.mob.getY());deckTicks++;}
                    }
                    if(across==4||since(b)>=900){
                        boolean deck=deckTicks>0&&deckMin>=br.elevation()+0.5&&!wet;
                        boolean tier0=own.traits.getOrDefault("tidecallers",0)==0&&!Houses.fords(u1);
                        boolean good=big&&across==4&&deck&&tier0&&crossedAt<=900;bridgesOk&=good;
                        bridgeWhy.append(br.id()).append(" (").append(br.width()).append(" x ").append(br.depth()).append("): ").append(across).append("/4 across, the Brute ").append(big?"at +"+crossedAt+" ticks (want within 900)":"NOT across (at x "+(u1==null?"-":fmt(u1.mob.getX()))+", y "+(u1==null?"-":fmt(u1.mob.getY()))+")").append(", on the deck for ").append(deckTicks).append(" ticks at y ").append(deckTicks>0?fmt(deckMin):"-").append(" or higher (deck top ").append(br.elevation()+1).append("), in water ").append(wet).append(", tier 0 and not fording=").append(tier0).append(good?"":" (BAD)").append("; ");
                        for(var u:new ArrayList<>(fixture))Houses.discard(b,u);fixture.clear();u1=null;audit(b,"bridge "+br.id(),false);
                        bridgeIdx++;next(b,44);
                    }
                }
                case 70->{ // clouds go with the round
                    var p=sp(b);if(p==null)return;
                    if(cost==null){
                        clearFixture(b,"before cleanup");unbuild(b);
                        b.mists.add(new Abilities.Mist(0,new Vec3d(site.getX()+.5,site.getY()+1,site.getZ()+.5),4,b.time+5000));
                        boolean standing=b.mists.size()==1;Abilities.clearBanners(b);boolean swept=standing&&b.mists.isEmpty();
                        b.mists.add(new Abilities.Mist(1,new Vec3d(site.getX()+.5,site.getY()+1,site.getZ()+.5),4,b.time+5000));
                        b.campaign.returnLobby();boolean lobby=!b.running&&b.mists.isEmpty();
                        report("p2-mist-cleanup",swept&&lobby,"a standing cloud was taken down by Abilities.clearBanners (the call a round start, a map switch, the lobby and a server stop each make)="+swept+"; a cloud standing when the round ends was gone after returnLobby (round running="+b.running+")="+lobby);
                    }
                    next(b,71);
                }
                case 71->{ // the tick cost: eight bot armies, four Tidecallers (hard) against four Gilded (hard)
                    var p=sp(b);if(p==null)return;
                    if(cost==null)cost=tickCost(b,List.of("tidecallers_hard","tidecallers_hard","tidecallers_hard","tidecallers_hard","gilded_hard","gilded_hard","gilded_hard","gilded_hard"));
                    if(cost.step(p)){
                        report("p2-tidecallers-tick-cost",cost.last<40,"server tick "+String.format(Locale.ROOT,"%.2f",cost.last)+" ms (600-tick mean "+String.format(Locale.ROOT,"%.2f",cost.avg)+" ms) with "+cost.troops+" troops at the start and "+b.units.size()+" at the end, "+b.mists.size()+" clouds standing ("+cost.note+")");
                        audit(b,"before the final returnLobby",false);
                        report("p2-tidecallers-teams-clean",audits>=20&&staleSeen.isEmpty(),audits+" audits after the suite's raisings, deaths and removals: "+(staleSeen.isEmpty()?"no gg_ team ever held a member that was not a living troop":"STALE "+staleSeen)+"; "+auditNotes);
                        cost.handBack();b.campaign.returnLobby();next(b,99);
                    }
                }
                default->{}
            }
        }
    }

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
        static volatile boolean on;static volatile long frames,sumNs,worstNs,draws,entities;static long begin,drawsAt;static volatile String animator="";
        static void register(){
            WorldRenderEvents.START.register(ctx->{if(on){begin=System.nanoTime();drawsAt=TroopRenderer.troopDraws;}});
            WorldRenderEvents.END.register(ctx->{if(!on||begin==0)return;long ns=System.nanoTime()-begin;begin=0;
                frames++;sumNs+=ns;worstNs=Math.max(worstNs,ns);draws+=TroopRenderer.troopDraws-drawsAt;entities+=rendered(MinecraftClient.getInstance());
                if(frames==300)animator=animatorCost(MinecraftClient.getInstance());});
        }
        /** The entities the last frame drew ("E: drawn/loaded" of the F3 screen). */
        static int rendered(MinecraftClient c){try{String t=c.worldRenderer.getEntitiesDebugString();int a=t.indexOf("E: ")+3;return Integer.parseInt(t.substring(a,t.indexOf('/',a)).trim());}catch(RuntimeException e){return 0;}}
        /**
         * What TroopAnimator adds to a troop's frame (Task 5 review, finding 1): midway through the window, after the frame, TroopModel.setAngles
         * (the reset of every bone, then the animator: its State, the blends, the carries, the head look) is timed on every troop this client has,
         * ten calls each after a warm-up call, against the bare reset of every bone that setAngles did before Task 5b.
         */
        static String animatorCost(MinecraftClient c){
            if(c.world==null)return "animator: no world";
            List<MobEntity> troops=new ArrayList<>();for(Entity e:c.world.getEntities())if(e instanceof MobEntity m&&TroopModels.resolve(e)!=null)troops.add(m);
            long full=0,bare=0;int n=0;
            for(MobEntity e:troops){
                TroopModel<MobEntity> m=TroopModels.modelOf(TroopModels.resolve(e).troop().id());if(m==null)continue;
                float yaw=net.minecraft.util.math.MathHelper.wrapDegrees(e.headYaw-e.bodyYaw);
                m.setAngles(e,e.limbAnimator.getPos(),e.limbAnimator.getSpeed(),e.age,yaw,e.getPitch());
                for(int i=0;i<10;i++){
                    long a=System.nanoTime();m.setAngles(e,e.limbAnimator.getPos(),e.limbAnimator.getSpeed(),e.age,yaw,e.getPitch());long b=System.nanoTime();
                    m.getPart().traverse().forEach(ModelPart::resetTransform);long d=System.nanoTime();full+=b-a;bare+=d-b;n++;}
            }
            if(n==0)return "animator: no troop on this client";
            double us=full/1e3/n,reset=bare/1e3/n;
            return String.format(Locale.ROOT,"animator: setAngles %.1f us a troop (a bare reset of every bone %.1f us), %d troops, so %.2f ms a frame for all of them",us,reset,troops.size(),us*troops.size()/1e3);
        }
        static void start(){frames=sumNs=worstNs=draws=entities=0;begin=0;animator="";on=true;}
        static String summary(){
            long n=Math.max(1,frames);
            return String.format(Locale.ROOT,"world render %.2f ms mean, %.2f ms worst over %d frames (%d fps at the end); per frame %.1f troop-model draws and %.1f entities drawn; %s",
                sumNs/1e6/n,worstNs/1e6,frames,MinecraftClient.getInstance().getCurrentFps(),draws/(double)n,entities/(double)n,animator);
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
                if(++darkFor>=600){awayFor=1;darkFor=0;recoveries++;sendAway(c);
                    System.out.println("[p2-light] "+name()+": sky light 0 at every floor point for 600 ticks with the terrain built; the spectator goes 400 blocks away and back to take the platform's chunks again (recovery "+recoveries+")");}
            }else darkFor=0;
            return false;
        }
        /**
         * Client: the relocation behind every recovery. Remembers where the spectator stands (back, backYaw, backPitch) and sends it 400 blocks east, out of view distance and inside
         * the arena; the caller sets awayFor to 1 and litStart counts the 40 ticks and brings it back. Stage.litStart and the tells suite's recover both call this.
         */
        void sendAway(MinecraftClient c){
            server(c,b->{var p=sp(b);if(p==null)return;back=p.getPos();backYaw=p.getYaw();backPitch=p.getPitch();b.relocate(p,site.getX()+400.5,site.getY()+4,site.getZ()+.5,backYaw,backPitch);});
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
                for(String key:List.of("rigs","weapons","troops")){List<String> entries=new ArrayList<>();if(ix.has(key))for(var el:ix.getAsJsonArray(key))entries.add(el.getAsString());lists.put(key,entries);
                    if(entries.isEmpty())missing.add("(index.json lists no "+key+")");}
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
            refuse(bad,why,"index troops {}",()->TroopModelLoader.index(json("{\"format\":2,\"troops\":{}}")));
            refuse(bad,why,"index troops [3]",()->TroopModelLoader.index(json("{\"format\":2,\"troops\":[3]}")));
            refuse(bad,why,"index format 1",()->TroopModelLoader.index(json("{\"format\":1,\"troops\":[]}")));
            refuse(bad,why,"no skins",()->TroopModelLoader.checkTroop(json("{\"troop\":\"knight\"}")));
            refuse(bad,why,"skins without stage 1",()->TroopModelLoader.checkTroop(json("{\"skins\":{\"2\":\"gravesandglory:x.png\"}}")));
            refuse(bad,why,"a skin for stage x",()->TroopModelLoader.checkTroop(json("{\"skins\":{\"1\":\"gravesandglory:x.png\",\"x\":\"gravesandglory:y.png\"}}")));
            refuse(bad,why,"a missing texture",()->TroopModelLoader.texture(c.getResourceManager(),new Identifier("gravesandglory","textures/entity/troop/no_such_skin.png")));
            refuse(bad,why,"a parent not listed before",()->TroopModelLoader.parse(json("{\"format\":2,\"size\":[64,64],\"bones\":["+bone+",{\"name\":\"head\",\"parent\":\"body\",\"pivot\":[0,0,0],\"boxes\":[]}]}")));
            refuse(bad,why,"a bone without boxes",()->TroopModelLoader.parse(json("{\"format\":2,\"size\":[64,64],\"bones\":[{\"name\":\"root\",\"parent\":\"\",\"pivot\":[0,0,0]}]}")));
            String ok="{\"format\":2,\"troop\":\"probe\",\"rig\":\"biped\",\"size\":[64,64],\"skins\":{\"1\":\"gravesandglory:textures/entity/troop/knight_s1.png\"},\"bones\":["+bone+"]}";
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
            if(tick>12000){report("p2-gilded-art-timeout",false,"stuck at client step "+cs+", server stage "+stage);end(c);return;}
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
     * Task 5b: the rig animation sets. The four rigs' clip tables (p2-anim-cover, -change, -return and -keys), the clip choice
     * and what a State plays from it (-choose), the loose on a shot (-loose), the blend on a once clip's peak (-blend), the bow in
     * the left hand (-bow), the carries shared by the frame and a forced clip (-carries), the stance sync over stance_v1 with its
     * late registration (-stance), a scene pose (-scene), a killed knight's death (-death) and the fold (-fold) on gold knights on
     * the platform, the 18 clips on a gold knight frozen through TroopAnimator.forced (anim-biped-<clip>), and the contact sheet's
     * frames (anim-sheet-*, SheetProbe: the 18 clips on the bare biped, the other rigs' stills, the walk and cape strips).
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
                case 1->{if(++wait==20){cover();change();returns();keys();choose();loose();blend();bow();carries();cull();cs=2;wait=0;si=0;}}
                case 2->stance(c,tick);
                case 3->scene(c,tick);
                case 4->death(c,tick);
                case 5->{fold(c);cs=6;wait=0;clip=0;server(c,b->camera(b,0,1.45,12-5,0,6));}
                case 6->{wait++; // the 18 clips on the shot knight, each frozen at its shot time (a loop at a quarter, a once clip at its peak, a hold at its end)
                    String n=TroopClips.NAMES.get(clip);var e=client(c,shotKnight.mob);
                    if(wait==1&&e!=null)TroopAnimator.forced.put(e.getId(),new TroopAnimator.Forced(n,TroopClips.shotTime("biped",n)));
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
        /** How far outside a cull reach (radius, bottom, top) an extent (TroopModel.extent) is, in blocks: positive is outside. */
        static double outside(double[] extent,double[] reach){return Math.max(extent[0]-reach[0],Math.max(reach[1]-extent[1],extent[2]-reach[2]));}
        /**
         * p2-anim-cull (Task 5 review, note 1): every loaded troop, every clip through the animator's own path with its carries
         * (applyForced: the shield arm, the bow's twist, arms forward, the root's scale for a taller troop), every 50 ms, stays
         * inside the troop's cull reach: the base and overlay clips inside the normal reach, the whole-body clips (death, kneel,
         * point, sit, fall, stand) inside the posed reach a dead or posed troop is culled by.
         */
        void cull(){
            boolean ok=true;StringBuilder why=new StringBuilder();int models=0,samples=0;double worst=-999;String worstAt="";List<String> out=new ArrayList<>();
            for(String id:new TreeSet<>(TroopModels.ids())){
                var m=TroopModels.model(id);var row=m==null?null:TroopAnimator.row(m);if(row==null)continue;models++;
                double[] normal=m.cullReach(false),posed=m.cullReach(true);double mine=-999;
                for(String n:TroopClips.NAMES){
                    double[] reach=TroopAnimator.BASE.contains(n)||TroopAnimator.OVERLAYS.contains(n)?normal:posed;float len=TroopClips.LENGTHS.get(n);
                    for(float s=0;s<=len+1e-4f;s+=.05f){
                        m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(n,s),row,true);
                        double o=outside(m.extent(),reach);samples++;
                        if(o>mine)mine=o;if(o>worst){worst=o;worstAt=id+" "+n+" at "+String.format(Locale.ROOT,"%.2f s",s);}
                        if(o>0&&out.size()<6)out.add(id+" "+n+String.format(Locale.ROOT," at %.2f s: %.2f blocks outside",s,o));}}
                m.getPart().traverse().forEach(ModelPart::resetTransform);
                ok&=mine<=0;
            }
            report("p2-anim-cull",ok&&models>0,models+" troop models, 18 clips each through the carries, "+samples+" poses every 50 ms: the cull reach has "+String.format(Locale.ROOT,"%.2f",-worst)
                +" blocks to spare at its tightest ("+worstAt+"; a negative figure is outside it)"+(out.isEmpty()?"":"; outside it (BAD): "+out)+(models>0?"":" (no troop model loaded: BAD)"));
        }
        /**
         * p2-anim-choose: TroopAnimator.plan on synthetic Views, and what a fresh State then plays from that Plan. choose reports the plan's
         * first overlay; State.update plays all of them, so each case reads both: the Choice, and the live tracks (pose, still, move and the
         * overlays) the State holds after it. A scene pose or death plays no overlay, with or without a clip of its own.
         */
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
            // a scene pose with no clip of its own plays the base and no overlay (a swing, a hurt and a raised shield all read, and none plays)
            n++;expect(bad,"helm_off with nothing before it, swinging, hurt, shield up",view("biped",false,true,true,true,false,true,false,"","helm_off",null),"idle","walk",null,false,false);
            n++;expect(bad,"stand with nothing to reverse, swinging",view("biped",false,true,false,true,false,false,false,"","stand",null),"idle","walk",null,false,false);
            n++;expect(bad,"a melee swing start",view("biped",false,true,false,true,false,false,false,"",null,null),"idle","walk","attack",false,true,"attack");
            n++;expect(bad,"melee: swing, hurt and shield up together",view("biped",false,true,true,true,false,true,false,"",null,null),"idle","walk","attack",false,true,"attack","hit","shield_up");
            n++;expect(bad,"ranged and attacking",view("biped",false,false,false,true,true,false,false,"",null,null),"idle","walk","bow_draw",false,true,"bow_draw");
            n++;expect(bad,"ranged, attacking, a shot (swing start): bow_draw, never attack",view("biped",false,true,false,true,true,false,false,"",null,null),"idle","walk","bow_draw",false,true,"bow_draw");
            n++;expect(bad,"ranged, hurt while drawing",view("biped",false,false,true,true,true,false,false,"",null,null),"idle","walk","bow_draw",false,true,"bow_draw","hit");
            n++;expect(bad,"magic and attacking",with(view("biped",false,false,false,true,true,false,false,"",null,null),true,false,0),"idle","walk","cast",false,true,"cast");
            n++;expect(bad,"spellcasting",with(view("biped",false,false,false,false,false,false,false,"",null,null),false,true,0),"idle","walk","cast",false,true,"cast");
            n++;expect(bad,"off-hand use",view("biped",false,false,false,false,false,true,false,"",null,null),"idle","walk","shield_up",false,true,"shield_up");
            n++;expect(bad,"a bear's warning",view("hound",false,false,false,false,false,false,true,"",null,null),"idle","walk","raise",false,true,"raise");
            n++;expect(bad,"a bear's warning, hurt",view("hound",false,false,true,false,false,false,true,"",null,null),"idle","walk","raise",false,true,"hit","raise");
            n++;expect(bad,"a hurt start",view("biped",false,false,true,false,false,false,false,"",null,null),"idle","walk","hit",false,true,"hit");
            n++;expect(bad,"nothing happening",view("biped",false,false,false,false,false,false,false,"",null,null),"idle","walk",null,false,true);
            for(String[] st:new String[][]{{"run","run","idle"},{"fall_back","fall_back","idle"},{"","walk","idle"},{"brace","walk","brace"},{"hold","walk","hold"}}){
                n++;expect(bad,"stance \""+st[0]+"\"",with(view("biped",false,false,false,false,false,false,false,st[0],null,null),false,false,.5f),st[2],st[1],null,false,true);}
            // an overlay playing when a pose begins stops: a swing, then a kneel pose a tick later, leaves no weight BLEND_TICKS on
            var s=new TroopAnimator.State(1);var swing=view("biped",false,true,false,true,false,false,false,"",null,null);var kneel=view("biped",false,false,false,false,false,false,false,"","kneel",null);
            s.update(TroopAnimator.plan(swing),10);s.update(TroopAnimator.plan(kneel),11);
            n++;if(s.overlayWeight("attack",11+TroopAnimator.BLEND_TICKS)>0)bad.add("an attack playing when a kneel pose began still has weight "+s.overlayWeight("attack",11+TroopAnimator.BLEND_TICKS)+" "+TroopAnimator.BLEND_TICKS+" ticks later (want 0)");
            report("p2-anim-choose",bad.isEmpty(),n+" cases"+(bad.isEmpty()?", all as the rules say (dead, scene poses with and without a clip, overlays, stances, head look), each read both as the Choice and as what a State plays from the same Plan":"; wrong: "+bad));
        }
        /** One case: the Choice of the View's plan, and the live tracks of a fresh State given that plan (pose, still, move, overlays sorted): the overlays named by live, none by default. */
        static void expect(List<String> bad,String what,TroopAnimator.View v,String still,String move,String overlay,boolean reverse,boolean look,String... live){
            if(v==null){bad.add(what+": no view");return;}
            var plan=TroopAnimator.plan(v);var c=TroopAnimator.choose(v);
            if(!c.equals(plan.choice()))bad.add(what+": choose "+c+" is not the plan's choice "+plan.choice());
            if(!Objects.equals(c.still(),still)||!Objects.equals(c.move(),move)||!Objects.equals(c.overlay(),overlay)||c.reverse()!=reverse||c.headLook()!=look)
                bad.add(what+": got "+c+", want "+new TroopAnimator.Choice(still,move,overlay,reverse,look));
            var s=new TroopAnimator.State(1);s.update(plan,10);
            List<String> on=new ArrayList<>(List.of(live));Collections.sort(on);
            String want=move==null?"pose "+still+(reverse?" reversed":"")+", still -, move -, overlays []":"pose -, still "+still+", move "+move+", overlays "+on;
            if(!s.playing(10).equals(want))bad.add(what+": the State plays ["+s.playing(10)+"], want ["+want+"]");
        }
        /** p2-anim-loose: on each rig a shot shows bow_draw's loose on the frame it starts, plays it out, and holds the full draw until the next shot; a draw with no shot holds the full draw. */
        void loose(){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String rig:RIGS){
                var m=TroopModels.rig(rig);var clip=TroopClips.of(rig).get("bow_draw");if(m==null||clip==null){ok=false;why.append(rig).append(": no rig or clip (BAD); ");continue;}
                float draw=TroopClips.BOW_TIMES.get(rig)[0],loose=TroopClips.BOW_TIMES.get(rig)[1];
                var aim=view(rig,false,false,false,true,true,false,false,"",null,null);var shot=view(rig,false,true,false,true,true,false,false,"",null,null);
                var s=new TroopAnimator.State(1);List<String> bad=new ArrayList<>();
                int age=0;for(;age<30;age++)TroopAnimator.simulate(m,s,aim,age,true); // drawing, no shot yet
                float seen=s.overlaySeconds(m,"bow_draw",29);float[] wantDraw=sample(m,clip,draw);
                TroopAnimator.simulate(m,s,aim,29,true);double offDraw=maxRot(m.snapshot(null),wantDraw);
                if(Math.abs(seen-draw)>.01||offDraw>1)bad.add(String.format(Locale.ROOT,"1.45 s into a draw it shows %.2f s of the clip (want the full draw, %.2f s), %.1f deg off",seen,draw,offDraw));
                for(int round=0;round<2;round++){
                    int at=age;TroopAnimator.simulate(m,s,shot,at,true);TroopAnimator.simulate(m,s,aim,at+1,true);
                    float t1=s.overlaySeconds(m,"bow_draw",at+1);float[] p1=m.snapshot(null),w1=sample(m,clip,loose+.05f);
                    if(Math.abs(t1-(loose+.05f))>.01||maxRot(p1,w1)>1||maxPos(p1,w1)>.1)bad.add(String.format(Locale.ROOT,"shot %d: one tick after the arrow it shows %.2f s of the clip (want the loose, %.2f s, plus a tick), %.1f deg off",round+1,t1,loose,maxRot(p1,w1)));
                    for(age=at+2;age<at+42;age++)TroopAnimator.simulate(m,s,aim,age,true); // the troop's next shot comes a cooldown (40 ticks) on
                    float t2=s.overlaySeconds(m,"bow_draw",age-1);float[] p2=m.snapshot(null);
                    if(Math.abs(t2-draw)>.01||maxRot(p2,wantDraw)>1)bad.add(String.format(Locale.ROOT,"shot %d: 40 ticks on it shows %.2f s of the clip (want the full draw held, %.2f s), %.1f deg off",round+1,t2,draw,maxRot(p2,wantDraw)));
                }
                ok&=bad.isEmpty();why.append(rig).append(String.format(Locale.ROOT,": full draw %.2f s, loose %.2f s",draw,loose)).append(bad.isEmpty()?", the shot shows the loose, and 40 ticks on the draw is held":", "+bad+" (BAD)").append("; ");
            }
            report("p2-anim-loose",ok,why.toString().trim());
        }
        /** Half a millisecond: AnimationHelper truncates a time to whole milliseconds, so 0.12 s (119.99999 ms in a float) would read the key 1 ms early; both sides of p2-anim-blend read half a millisecond past it. */
        static final float HALF_MS=.0005f;
        /** p2-anim-blend: a once overlay shows its peak in full when it gets there (a live hit at 0.08 s, an attack at its 0.12 s wind-up and its 0.25 s contact), read half a millisecond past the key on both sides: the blend in does not cut it (1 degree or px; the check plays the clip alone on a bare rig, so nothing else is in the frame). */
        void blend(){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String rig:RIGS){
                var m=TroopModels.rig(rig);if(m==null){ok=false;why.append(rig).append(": no rig model (BAD); ");continue;}
                var calm=view(rig,false,false,false,false,false,false,false,"",null,null);var hurt=view(rig,false,false,true,false,false,false,false,"",null,null);var swing=view(rig,false,true,false,true,false,false,false,"",null,null);
                List<String> bad=new ArrayList<>();double worst=0;var hit=TroopClips.of(rig).get("hit");var attack=TroopClips.of(rig).get("attack");
                var s=new TroopAnimator.State(2);for(int a=0;a<10;a++)TroopAnimator.simulate(m,s,calm,a,true);
                TroopAnimator.simulate(m,s,hurt,10,true);TroopAnimator.simulate(m,s,calm,10+(.08f+HALF_MS)*20,true);
                double off=maxRot(m.snapshot(null),sample(m,hit,.08f+HALF_MS));worst=Math.max(worst,off);if(off>1)bad.add(String.format(Locale.ROOT,"hit at 0.08 s %.1f deg off the clip",off));
                var t=new TroopAnimator.State(3);for(int a=0;a<10;a++)TroopAnimator.simulate(m,t,calm,a,true);
                TroopAnimator.simulate(m,t,swing,10,true);
                for(float sec:new float[]{.12f,.25f}){TroopAnimator.simulate(m,t,calm,10+(sec+HALF_MS)*20,true);float[] got=m.snapshot(null),want=sample(m,attack,sec+HALF_MS);
                    double o=Math.max(maxRot(got,want),maxPos(got,want));worst=Math.max(worst,o);
                    if(o>1)bad.add(String.format(Locale.ROOT,"attack at %.2f s %.1f off the clip",sec,o));}
                ok&=bad.isEmpty();why.append(rig).append(String.format(Locale.ROOT,": the worst %.2f",worst)).append(bad.isEmpty()?"":", "+bad+" (BAD)").append("; ");
            }
            report("p2-anim-blend",ok,why.toString().trim()+" (a fresh play of hit or attack on its peak against the clip itself, bare rig, degrees or px, read half a millisecond past the key on both sides; want 1 or less: a blend that cuts the peak is 7 or more off, and the 1 ms truncation of the clip time at 0.12 s is not in it)");
        }
        // ---- one key extreme per clip per rig (Task 5b review, minor 1) ----
        /** One key of a clip: the rig, the clip, a bone, a channel (p pitch, y yaw, r roll in degrees; x, u up, z offsets in px), the second it is read at and the value there. */
        record Key(String rig,String clip,String bone,char chan,float at,float value){}
        static Key key(String rig,String clip,String bone,char chan,float at,float value){return new Key(rig,clip,bone,chan,at,value);}
        /** The table's headline number of each clip, read off the brief's clip table (angles in degrees, offsets in px, y up); a wrong sign, arm, rig or clip fails it. */
        static final List<Key> KEYS=List.of(
            key("biped","idle","body",'p',1.5f,1.5f),key("biped","walk","right_leg",'p',.25f,-30),key("biped","run","right_leg",'p',.15f,-50),key("biped","attack","right_arm",'p',.12f,-110),
            key("biped","hit","body",'p',.08f,-10),key("biped","death","root",'p',.8f,-90),key("biped","shield_up","left_arm",'p',.2f,-60),key("biped","bow_draw","left_arm",'p',.3f,-90),
            key("biped","bow_draw","right_arm",'y',.65f,-70),key("biped","cast","right_arm",'r',.25f,125),key("biped","kneel","right_leg",'p',.6f,48),key("biped","point","right_arm",'p',.4f,-90),
            key("biped","sit","right_leg",'p',.6f,-90),key("biped","raise","right_arm",'p',.5f,-170),key("biped","fall","root",'p',.8f,90),key("biped","stand","root",'p',.3f,45),
            key("biped","brace","right_arm",'p',1,-70),key("biped","hold","right_arm",'p',1,-30),key("biped","fall_back","right_leg",'p',.25f,25),
            key("hound","idle","tail",'y',.375f,10),key("hound","walk","front_left_leg",'p',.25f,25),key("hound","run","front_left_leg",'p',.15f,-40),key("hound","attack","jaw",'p',.15f,30),
            key("hound","hit","head",'y',.08f,15),key("hound","death","root",'r',.7f,90),key("hound","shield_up","head",'p',.2f,20),key("hound","bow_draw","chest",'u',.3f,-2),
            key("hound","cast","head",'p',0,-50),key("hound","kneel","front_left_leg",'p',.6f,-60),key("hound","point","tail",'p',.4f,50),key("hound","sit","body",'p',.6f,-35),
            key("hound","raise","body",'p',.5f,-45),key("hound","fall","root",'r',.8f,-90),key("hound","stand","root",'r',.3f,-45),key("hound","brace","front_left_leg",'p',1,-15),
            key("hound","hold","head",'p',0,-12),key("hound","fall_back","front_left_leg",'p',.25f,-20),
            key("spider","idle","abdomen",'u',.75f,-.3f),key("spider","walk","left_leg_1",'y',.25f,20),key("spider","run","left_leg_1",'y',.15f,35),key("spider","attack","left_leg_1",'r',.15f,-40),
            key("spider","hit","left_leg_1",'r',.08f,-15),key("spider","death","left_leg_1",'r',.9f,-60),key("spider","shield_up","left_leg_1",'r',.2f,-30),key("spider","bow_draw","left_leg_1",'r',.3f,-50),
            key("spider","cast","left_leg_1",'r',.5f,-40),key("spider","kneel","left_leg_1",'r',.6f,-30),key("spider","point","left_leg_1",'r',.4f,-60),key("spider","sit","left_leg_1",'y',.6f,30),
            key("spider","raise","left_leg_1",'r',.5f,-70),key("spider","fall","left_leg_1",'r',.8f,-60),key("spider","stand","left_leg_1",'r',0,-60),key("spider","brace","root",'u',1,-2),
            key("spider","hold","left_leg_1",'y',1,12),key("spider","fall_back","left_leg_1",'y',.25f,-20),
            key("colossus","idle","right_arm",'r',1.5f,3),key("colossus","walk","right_leg",'p',.25f,-25),key("colossus","run","right_leg",'p',.15f,-40),key("colossus","attack","right_arm",'p',.12f,-120),
            key("colossus","hit","body",'p',.08f,-12),key("colossus","death","root",'p',.85f,90),key("colossus","shield_up","right_arm",'p',.2f,-75),key("colossus","bow_draw","right_arm",'p',.8f,-80),
            key("colossus","cast","right_arm",'p',.5f,-100),key("colossus","kneel","right_leg",'p',.6f,60),key("colossus","point","right_arm",'p',.4f,-90),key("colossus","sit","right_leg",'p',.6f,-90),
            key("colossus","raise","right_arm",'p',.5f,-170),key("colossus","fall","root",'p',.8f,90),key("colossus","stand","root",'p',.3f,45),key("colossus","brace","right_arm",'p',1,-20),
            key("colossus","hold","right_arm",'p',1,-12),key("colossus","fall_back","right_leg",'p',.25f,20));
        /** The same, read on the mirrored clip (the left-handed troop's): the arm swaps sides and its yaw changes sign. */
        static final List<Key> MIRRORED_KEYS=List.of(
            key("biped","attack","left_arm",'p',.12f,-110),key("biped","shield_up","right_arm",'y',.2f,-30),key("biped","point","left_arm",'y',.4f,-8),key("colossus","shield_up","left_arm",'y',.2f,50));
        /** One channel of one bone in a clip at a second, as a delta from rest: degrees for p, y, r; px for x, z and u (u is up: the pivot's y is down). */
        static double keyValue(TroopModel<?> m,Animation a,Key k){
            float[] rest=sample(m,null,0),at=sample(m,a,k.at()+.0005f);int i=m.boneOrder().indexOf(k.bone());if(i<0)return Double.NaN;
            return switch(k.chan()){
                case 'x'->at[i*6]-rest[i*6];case 'u'->-(at[i*6+1]-rest[i*6+1]);case 'z'->at[i*6+2]-rest[i*6+2];
                case 'p'->Math.toDegrees(at[i*6+3]-rest[i*6+3]);case 'y'->Math.toDegrees(at[i*6+4]-rest[i*6+4]);default->Math.toDegrees(at[i*6+5]-rest[i*6+5]);};
        }
        /** p2-anim-keys: one headline extreme per clip per rig, as (bone, channel, second, value) read off the clip itself (2 degrees, 0.2 px), and four on the mirrored clips; and every clip of every rig is in the table. */
        void keys(){
            List<String> bad=new ArrayList<>();int n=0;Set<String> covered=new HashSet<>();
            for(var set:List.of(KEYS,MIRRORED_KEYS))for(Key k:set){
                boolean mirrored=set==MIRRORED_KEYS;var m=TroopModels.rig(k.rig());var a=m==null?null:(mirrored?TroopClips.mirrored(k.rig()):TroopClips.of(k.rig())).get(k.clip());
                if(a==null){bad.add(k.rig()+" "+k.clip()+": no rig or clip");continue;}
                n++;if(!mirrored)covered.add(k.rig()+"/"+k.clip());
                double got=keyValue(m,a,k),tol="xuz".indexOf(k.chan())>=0?.2:2;
                if(Double.isNaN(got)||Math.abs(got-k.value())>tol)bad.add(String.format(Locale.ROOT,"%s%s %s %s %c at %.2f s: %.2f, want %.2f",k.rig(),mirrored?" mirrored":"",k.clip(),k.bone(),k.chan(),k.at(),got,k.value()));
            }
            for(String rig:RIGS)for(String c:TroopClips.NAMES)if(!covered.contains(rig+"/"+c))bad.add(rig+" "+c+": not in the table");
            report("p2-anim-keys",bad.isEmpty()&&n>0,n+" keys ("+KEYS.size()+" on the plain clips, 18 on each rig, and "+MIRRORED_KEYS.size()+" mirrored), each read off the clip within 2 degrees or 0.2 px"+(bad.isEmpty()?"":"; wrong: "+bad));
        }
        // ---- the bow in its hand (Task 5b fix round 1) ----
        /** A bow troop's pose measured in the troop's own axes (px right, up, front from the feet): the arm's box, the grip pixel's centre (a point box), the sprite's up axis and its plane's normal, the drawn frame's nock (null without one), the other hand's pivot and the frame drawn. */
        record BowPose(double[] arm,double[] grip,double[] up,double[] normal,double[] nock,double[] other,int frame){}
        /** The bone chain and weapon of a bow troop forced to one clip and second (no shield), as Turn.fact reads them. */
        static BowPose bowPose(TroopModel<?> m,String clip,float seconds){
            m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(clip,seconds),TroopAnimator.row(m),false);
            var w=TroopModels.weapon(m.weaponId());String hand=m.hand(),off="left".equals(hand)?"right":"left";List<ModelPart> path=m.handPath(hand);
            List<double[]> arm=new ArrayList<>();MatrixStack ma=Turn.base();for(int k=0;k<path.size()-2;k++)path.get(k).rotate(ma);
            path.get(path.size()-2).forEachCuboid(ma,(entry,p,i,q)->{if(p.isEmpty())arm.add(Turn.boxOf(entry,q));});
            MatrixStack mw=Turn.base();for(ModelPart p:path)p.rotate(mw);w.grip(mw);
            double[] g=Turn.own(Turn.pt(mw,0,0,0),16);
            Vector4f u=new Vector4f(0,1,0,0),n=new Vector4f(0,0,1,0);mw.peek().getPositionMatrix().transform(u);mw.peek().getPositionMatrix().transform(n);
            int frame=w.frames().size()>1?TroopModels.Weapon.frameAt(w.pull(),clip.equals("bow_draw")?seconds:-1f,TroopClips.BOW_TIMES.getOrDefault(m.rig,TroopClips.BOW_TIMES.get("biped"))[1]):0;
            double[] nock=frame<w.nocks().size()?Turn.own(Turn.pt(mw,w.nocks().get(frame).x,w.nocks().get(frame).y,w.nocks().get(frame).z),16):null;
            MatrixStack mo=Turn.base();for(ModelPart p:m.handPath(off))p.rotate(mo);double[] other=Turn.own(Turn.pt(mo,0,0,0),16);
            m.getPart().traverse().forEach(ModelPart::resetTransform);
            return new BowPose(arm.isEmpty()?null:arm.get(0),new double[]{g[0],g[1],g[2],g[0],g[1],g[2]},Turn.own(u,1),Turn.own(n,1),nock,other,frame);
        }
        /**
         * p2-anim-bow: every bow troop (ranged, not magic) holds its bow in the left hand, the drawing hand being the right. At rest (idle at 0 s,
         * the arm hanging, the hand turned by the bow carry) the grip box's centre lies inside the fist (the arm box's lowest 4 px) and within
         * 1 px of its axis, and the bow stands upright (its up axis within 8 degrees of vertical). The string half of this check (the drawing
         * hand against the string) is retired: it measured to the whole string segment, which a pull frame no longer has, and p2-depth-bow
         * replaces it by measuring the right hand against the drawn frame's nock point.
         */
        void bow(){
            boolean ok=true;StringBuilder why=new StringBuilder();int troops=0;
            for(String id:new TreeSet<>(TroopModels.ids())){
                var m=TroopModels.model(id);var row=m==null?null:TroopAnimator.row(m);if(row==null||!TroopAnimator.bow(row))continue;troops++;List<String> bad=new ArrayList<>();
                if(!"left".equals(m.hand()))bad.add("the bow is in the "+m.hand()+" hand (want left: the bow arm, the right hand draws)");
                var rest=bowPose(m,"idle",0);
                String restWhy="";
                if(rest.arm()==null||rest.grip()==null||rest.up()==null)bad.add("no arm box or weapon boxes");
                else{
                    double[] fist={rest.arm()[0],rest.arm()[1],rest.arm()[2],rest.arm()[3],rest.arm()[1]+4,rest.arm()[5]}; // the arm's box from its lower end up 4 px
                    double[] c={(rest.grip()[0]+rest.grip()[3])/2,(rest.grip()[1]+rest.grip()[4])/2,(rest.grip()[2]+rest.grip()[5])/2};
                    double midR=(fist[0]+fist[3])/2,midF=(fist[2]+fist[5])/2;
                    boolean inside=c[0]>=fist[0]&&c[0]<=fist[3]&&c[1]>=fist[1]&&c[1]<=fist[4]&&c[2]>=fist[2]&&c[2]<=fist[5];
                    boolean axis=Math.abs(c[0]-midR)<=1&&Math.abs(c[2]-midF)<=1;
                    double upright=Math.abs(rest.up()[1]);
                    restWhy=String.format(Locale.ROOT,"at rest the grip's centre is (R %.1f, U %.1f, F %.1f) in the fist (R %.1f..%.1f, U %.1f..%.1f, F %.1f..%.1f), %.1f px off its axis, the bow's up axis %.2f of vertical",c[0],c[1],c[2],fist[0],fist[3],fist[1],fist[4],fist[2],fist[5],Math.hypot(c[0]-midR,c[2]-midF),upright);
                    if(!inside)bad.add("the grip is outside the fist");if(!axis)bad.add("the grip is more than 1 px off the fist's axis");if(upright<Math.cos(Math.toRadians(8)))bad.add("the bow is not upright at rest");
                }
                ok&=bad.isEmpty();why.append(id).append(": bow in the ").append(m.hand()).append(" hand; ").append(restWhy).append(bad.isEmpty()?"":", "+bad+" (BAD)").append("; ");
            }
            report("p2-anim-bow",ok&&troops>0,troops+" bow troops: "+why.toString().trim()+" (the string-to-hand half is p2-depth-bow's nock check)"+(troops>0?"":" (no bow troop loaded: BAD)"));
        }

        /**
         * p2-anim-carries (Task 5b review, minor 3): the carries are one rule for the frame and for a forced clip. For each troop model the frame's pose at
         * age 0 (a fresh State, the shield in the off hand for a troop that has one) equals the forced clip's, in degrees and px: idle, the hold stance's
         * hold, and, for the troop's own overlay, attack (melee), bow_draw (a bow) or cast (magic). Idle at 0 s is the rest pose, so the carries (the shield
         * arm 45 degrees forward, the bow's turn, the arms forward) are what the two paths must agree on.
         */
        void carries(){
            boolean ok=true;StringBuilder why=new StringBuilder();int models=0,cases=0;double worst=0;List<String> bad=new ArrayList<>();
            for(String id:new TreeSet<>(TroopModels.ids())){
                var m=TroopModels.model(id);var row=m==null?null:TroopAnimator.row(m);if(row==null)continue;models++;boolean shield=!row.shield().equals("none");
                List<String[]> list=new ArrayList<>();list.add(new String[]{"","idle","-"});list.add(new String[]{"hold","hold","-"});
                list.add(row.ranged()?(row.magic()?new String[]{"","cast","cast"}:new String[]{"","bow_draw","bow_draw"}):new String[]{"","attack","attack"});
                for(String[] c:list){
                    cases++;boolean attacking=c[2].equals("bow_draw")||c[2].equals("cast"),swing=c[2].equals("attack");
                    var v=new TroopAnimator.View(m.rig,false,swing,false,attacking,row.ranged(),row.magic(),false,false,false,0f,c[0],null,null);
                    var s=new TroopAnimator.State(0);s.update(TroopAnimator.plan(v),0);
                    m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.apply(m,s,row,shield,0,0,0,0,0);float[] frame=m.snapshot(null);
                    m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(c[1],0),row,shield);float[] forced=m.snapshot(null);
                    double off=Math.max(maxRot(frame,forced),maxPos(frame,forced));worst=Math.max(worst,off);
                    if(off>.5)bad.add(String.format(Locale.ROOT,"%s %s: the frame is %.1f off the forced clip",id,c[1],off));
                }
                m.getPart().traverse().forEach(ModelPart::resetTransform);
            }
            ok=bad.isEmpty()&&models>0;
            report("p2-anim-carries",ok,models+" troop models, "+cases+" cases (idle, hold and the troop's own overlay): the frame's pose at age 0 is within "+String.format(Locale.ROOT,"%.2f",worst)+" (degrees or px) of the forced clip's (want 0.5 or less)"+(bad.isEmpty()?"":"; wrong: "+bad));
        }

        // ---- the stance sync ----
        /** One stance step: the drill's order and formation, and the stance its three knights must then read on this client within 20 ticks. A Retreat's spot lies 40 blocks off, so the group has not arrived (Tactics.arrived) and runs. */
        record StanceStep(String what,String order,String formation,String want){}
        static final List<StanceStep> STANCE_STEPS=List.of(new StanceStep("hold in shieldwall","hold","shieldwall","brace"),new StanceStep("charge","charge","line","run"),
            new StanceStep("follow","follow","line",""),new StanceStep("retreat in line","retreat","line","run"),new StanceStep("hold in line","hold","line","hold"));
        Vec3d drillTarget;
        boolean stanceOk=true;final StringBuilder stanceWhy=new StringBuilder();int lateSeen=-1,trackingBefore,beatBefore;String lateStance="-";
        void stance(MinecraftClient c,int tick){
            wait++;
            if(si<STANCE_STEPS.size()){
                var st=STANCE_STEPS.get(si);
                if(wait==1){since=tick;server(c,b->{var d=drill;if(drillTarget==null)drillTarget=d.target;d.order=st.order();d.formation=st.formation();d.target=st.order().equals("retreat")?drillTarget.add(40,0,0):drillTarget;});return;}
                boolean all=true;for(var u:group){var e=client(c,u.mob);all&=e!=null&&TroopAnimator.STANCES.getOrDefault(e.getId(),"").equals(st.want());}
                if(all||tick-since>40){boolean good=all&&tick-since<=20;stanceOk&=good;
                    stanceWhy.append(st.what()).append(": ").append(all?"\""+st.want()+"\" read for all three "+(tick-since)+" ticks after the order":"not all read \""+st.want()+"\" in 40 ticks: "+reads(c)).append(good?"":" (BAD: want 20 ticks or fewer)").append("; ");
                    si++;wait=0;}
                return;
            }
            if(regPhase>0){register(c,tick);return;}
            // a knight spawned into the group on hold in line: its first tick on this client reads hold, sent when the player started tracking it
            if(wait==1){trackingBefore=Tactics.trackingSends;beatBefore=Tactics.stanceSends;since=tick;server(c,b->late=post(b,drill,"knight",-2.5,5,180));return;}
            var e=late==null?null:client(c,late.mob);
            if(e!=null&&lateSeen<0){lateSeen=tick-since;lateStance=TroopAnimator.STANCES.getOrDefault(e.getId(),"");}
            if(lateSeen>=0||tick-since>60){
                int sends=Tactics.trackingSends-trackingBefore;boolean good=lateSeen>=0&&"hold".equals(lateStance)&&sends>=1;stanceOk&=good;
                stanceWhy.append("a knight spawned into the group on hold in line: on this client ").append(lateSeen<0?"never":lateSeen+" ticks after the spawn").append(", reading \"").append(lateStance)
                    .append("\" on that first tick (want hold); ").append(sends).append(" START_TRACKING stance sends (want 1 or more), ").append(Tactics.stanceSends-beatBefore).append(" beat sends meanwhile").append(good?"":" (BAD)");
                regPhase=1;wait=0;}
        }
        /** A player whose client registers stance_v1 late (Task 5b review, minor 4): this client's stances are cleared, the REGISTER event runs for it, and the four knights of the drill must read hold again. */
        int regPhase,regBefore,regSince;
        void register(MinecraftClient c,int tick){
            if(regPhase==1){TroopAnimator.STANCES.clear();regBefore=Tactics.registerSends;regSince=tick;UUID id=c.player.getUuid();
                server(c,b->{var p=b.server.getPlayerManager().getPlayer(id);if(p!=null)net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents.REGISTER.invoker().onChannelRegister(p.networkHandler,net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.getSender(p),b.server,List.of(GravesAndGlory.STANCE));});
                regPhase=2;return;}
            List<GravesAndGlory.Unit> four=new ArrayList<>(group);if(late!=null)four.add(late);boolean all=true;
            for(var u:four){var e=client(c,u.mob);all&=e!=null&&TroopAnimator.STANCES.getOrDefault(e.getId(),"").equals("hold");}
            if(all||tick-regSince>40){
                int sends=Tactics.registerSends-regBefore;boolean good=all&&sends>=four.size();stanceOk&=good;
                stanceWhy.append("; a client registering stance_v1 late (its stances cleared, then the REGISTER event run for this player): ").append(sends).append(" stance sends (want ").append(four.size()).append(" or more), the four knights read hold again ")
                    .append(all?(tick-regSince)+" ticks later":"never (40 ticks)").append(good?"":" (BAD)");
                report("p2-anim-stance",stanceOk,stanceWhy.toString().trim());
                server(c,b->{var d=drill;d.order="follow";if(drillTarget!=null)d.target=drillTarget;camera(b,0,1.6,-6,0,8);});cs=3;wait=0;si=0;regPhase=0;}
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
        int killAt=-1,drawsAtKill,deadSamples,lastDeathTime;float minRoot=999;double deathOut=-999;Entity dying;
        void death(MinecraftClient c,int tick){
            wait++;var m=TroopModels.model("knight");
            if(killAt<0){var e=client(c,victim.mob);var d=e==null?null:TroopRenderer.drawn(e);
                if(d==null||m==null){if(wait>200){deathReport(m,null);cs=5;wait=0;}return;}
                dying=e;drawsAtKill=d.count;killAt=tick;server(c,b->victim.mob.kill());return;}
            var d=TroopRenderer.drawn(dying);int root=m.boneOrder().indexOf("root");
            if(d!=null&&d.pose!=null&&root>=0&&dying instanceof net.minecraft.entity.LivingEntity le&&(le.deathTime>0||!le.isAlive())){deadSamples++;lastDeathTime=le.deathTime;minRoot=Math.min(minRoot,(float)Math.toDegrees(d.pose[root*6+3]));
                m.applyPose(d.pose);deathOut=Math.max(deathOut,outside(m.extent(),m.cullReach(true)));}
            if(client(c,victim.mob)==null||tick-killAt>80){deathReport(m,d);cs=5;wait=0;}
        }
        /** p2-anim-death: every rig's death ends by 0.9 s and holds; the killed knight fell through its own clip (its root pitched back to -90) and never tilted (TroopRenderer's measured tilt). */
        void deathReport(TroopModel<?> m,TroopRenderer.Drawn d){
            boolean clips=true;StringBuilder cw=new StringBuilder();
            for(String rig:RIGS){var a=TroopClips.of(rig).get("death");boolean g=a!=null&&a.lengthInSeconds()<=.9f+1e-4f&&!a.looping()&&TroopClips.KINDS.get("death")==TroopClips.Kind.HOLD;clips&=g;
                cw.append(rig).append(" ").append(a==null?"none":String.format(Locale.ROOT,"%.2f s",a.lengthInSeconds())).append(g?"":" (BAD)").append(", ");}
            int draws=d==null?0:d.count-drawsAtKill;float tilt=d==null?999:d.maxTilt;
            boolean ok=clips&&d!=null&&draws>=10&&deadSamples>=10&&tilt<1&&minRoot<=-85&&deathOut<=0;
            report("p2-anim-death",ok,String.format(Locale.ROOT,"death clips: %s(want 0.9 s or less, a hold); the killed knight: %d draws after the kill, %d ticks sampled dead up to deathTime %d, its root pitched to %.1f deg (want -85 or less: the clip's fall), the most tilt of any of its draws after setupTransforms %.2f deg (want under 1: no vanilla death tilt); its drawn pose stayed %.2f blocks inside the posed cull reach (want inside)",
                cw,draws,deadSamples,lastDeathTime,minRoot,tilt,-deathOut));
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
     * Turnaround shots of the troop art, for seeing how the weapons sit in the troops' hands (GRAVES_PHASE2_CHECKS=turn; shots, not
     * checks). The troops come from GRAVES_PHASE2_TURN (the system property gravesandglory.turnTroops): a House id or a comma list of
     * troop ids, default gilded: those gold troops at stage 1, one at a time at the platform's centre facing north (the others parked
     * PARK blocks up), in daylight, no HUD, frozen through TroopAnimator.forced. The camera, at chest height DIST blocks away, goes round
     * the troop (angle 0 in front, 90 on its right, the weapon-hand side, 180 behind, 270 on its left) in idle (rest), bow_draw at its full
     * draw and at its loose (a caster: cast; any other: attack at its peak) and walk at a quarter; then a bowman's weapon hand close up from the
     * front, the right side and above in idle; then each troop with its weapon hidden (at 0 and 90 degrees), and with the relief off (at 0, 45 and 90 degrees). Shots are
     * p2-turn-TROOP-POSE-ANGLE.png. The weapon's axes and extents in the troop's own right, up and front axes (px) are logged as
     * "[p2-turn-fact]" lines. The one check, p2-turn-shots: every shot taken lit, in gold, with the camera where it was set and the pose
     * drawn as forced.
     */
    static final class Turn extends Stage {
        static final List<String> TROOPS=troops(System.getProperty("gravesandglory.turnTroops","gilded"));
        static final double DIST=3,EYE=1.2,CLOSE=2.0,PARK=24;
        /** The troops of a GRAVES_PHASE2_TURN value: a House id gives its troops that have a model (sheet order), anything else is a troop id. */
        static List<String> troops(String arg){
            List<String> out=new ArrayList<>();
            for(String part:arg.split(",")){part=part.trim();if(part.isEmpty())continue;boolean house=false;
                for(var t:Spec.TROOPS)if(t.house().equals(part)){house=true;if(TroopModels.model(t.id())!=null)out.add(t.id());}
                if(!house)out.add(part);}
            return out;
        }
        /** While set, no entity is the one under the crosshair when the world draws (a targeted troop shows its name tag, which the shots must not have). */
        static volatile boolean noTarget;static boolean hooked;
        /** Registers (once) the hook that keeps the crosshair off every entity while noTarget is set, and sets it. */
        static void hook(){
            if(!hooked){hooked=true;WorldRenderEvents.START.register(ctx->{if(noTarget)MinecraftClient.getInstance().targetedEntity=null;});}
            noTarget=true;
        }
        static final float YAW=180;
        static final int[] ANGLES={0,45,90,135,180,225,270,315};
        /** One shot: its file name, the troop, the pose's label, the clip and second forced on it, the angle round it, a close view of the weapon hand (front, side or above; null for the body), whether the weapon is hidden and whether the relief is off (TroopDepth.enabled false). */
        record Shot(String name,String troop,String pose,String clip,float at,int angle,String hand,boolean bare,boolean flat){}
        final List<Shot> shots=new ArrayList<>();
        final Map<String,GravesAndGlory.Unit> units=new LinkedHashMap<>();
        int si,held,steady,lastCount,settle;String on="";double[] cam;final List<String> bad=new ArrayList<>();
        String name(){return "turn";}
        Turn(){
            hook();
            for(String t:TROOPS){
                var row=ArmyCards.spec(t);boolean bow=row!=null&&TroopAnimator.bow(row),magic=row!=null&&row.magic();List<Object[]> poses=new ArrayList<>();
                poses.add(new Object[]{"idle","idle",0f});
                if(bow){poses.add(new Object[]{"draw","bow_draw",TroopClips.BOW_TIMES.get("biped")[0]});poses.add(new Object[]{"loose","bow_draw",TroopClips.BOW_TIMES.get("biped")[1]});}
                else if(magic)poses.add(new Object[]{"cast","cast",TroopClips.shotTime("biped","cast")});
                else poses.add(new Object[]{"attack","attack",TroopClips.shotTime("biped","attack")});
                poses.add(new Object[]{"walk25","walk",TroopClips.LENGTHS.get("walk")/4});
                for(Object[] p:poses)for(int a:ANGLES)shots.add(new Shot("turn-"+t+"-"+p[0]+"-"+a,t,(String)p[0],(String)p[1],(Float)p[2],a,null,false,false));
                if(bow)for(String v:List.of("front","side","above"))shots.add(new Shot("turn-"+t+"-hand-"+v,t,"hand-"+v,"idle",0f,0,v,false,false));
                for(int a:new int[]{0,90})shots.add(new Shot("turn-"+t+"-bare-"+a,t,"bare","idle",0f,a,null,true,false));
                for(int a:new int[]{0,45,90})shots.add(new Shot("turn-"+t+"-flat-"+a,t,"flat","idle",0f,a,null,false,true));
            }
        }
        void cast(GravesAndGlory.Battle b){
            for(int i=0;i<TROOPS.size();i++){var u=post(b,gold,TROOPS.get(i),0,0,YAW);units.put(TROOPS.get(i),u);place(u,i,false);}
            camera(b,0,EYE,DIST,0,0);
        }
        /** Server: a troop at the platform's centre facing north, or parked high over the floor out of every view (index i keeps the parked ones apart). */
        void place(GravesAndGlory.Unit u,int i,boolean up){
            Vec3d where=up?at(0,0):at(5*(i-1),0).add(0,PARK,0);
            pins.removeIf(p->p.mob()==u.mob);pins.add(new Pin(u.mob,where,YAW));pin(u.mob,where,YAW);
        }
        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
            if(finished)return;
            if(tick>24000){report("p2-turn-timeout",false,"stuck at shot "+si+" of "+shots.size()+", client step "+cs+", server stage "+stage);end(c);return;}
            switch(cs){
                case 0->{boolean lit=litStart(c);
                    if(lit&&ready&&posted.size()==TROOPS.size()&&posted.stream().allMatch(u->client(c,u.mob)!=null)){facts();c.options.hudHidden=true;wait=0;cs=1;}
                    else if(++wait%600==0)System.out.println("[p2-wait] turn step 0 after "+wait+" ticks: ready="+ready+", posted "+posted.size()+" of "+TROOPS.size()+"; "+litWhy(c));}
                case 1->shoot(c);
                case 2->{if(++wait==5){report("p2-turn-shots",bad.isEmpty(),shots.size()+" shots"+(bad.isEmpty()?", each taken lit, in gold, with the camera where it was set (0.05 blocks, 1 degree) and the pose drawn as forced (1 degree)":", not as asked: "+bad));serverDone=true;end(c);}}
                default->{}
            }
        }
        public void cleanup(MinecraftClient c){noTarget=false;TroopAnimator.forced.clear();weapons(null);TroopDepth.enabled=true;super.cleanup(c);}
        /** Client: every weapon model drawn, but the named troop's (TroopFeatures.HIDDEN, by weapon id). */
        static void weapons(String hide){TroopFeatures.HIDDEN.clear();if(hide!=null){var m=TroopModels.model(hide);if(m!=null)TroopFeatures.HIDDEN.add(m.weaponId());}}
        boolean shield(String troop){return !units.get(troop).mob.getOffHandStack().isEmpty();}

        /** Client: one shot's turn. */
        void shoot(MinecraftClient c){
            if(si>=shots.size()){TroopAnimator.forced.clear();weapons(null);cs=2;wait=0;return;}
            Shot s=shots.get(si);var e=client(c,units.get(s.troop()).mob);var m=TroopModels.model(s.troop());
            if(e==null||m==null){if(++held>400){bad.add(s.name()+": no client entity or model");si++;held=0;wait=0;}return;}
            wait++;
            if(wait==1){
                boolean change=!s.troop().equals(on);
                if(change){on=s.troop();server(c,b->{for(int i=0;i<TROOPS.size();i++)place(units.get(TROOPS.get(i)),i,TROOPS.get(i).equals(s.troop()));});}
                TroopAnimator.forced.clear();TroopAnimator.forced.put(e.getId(),new TroopAnimator.Forced(s.clip(),s.at()));weapons(s.bare()?s.troop():null);TroopDepth.enabled=!s.flat();
                cam=view(s,m);final double[] v=cam;server(c,b->camera(b,v[0],v[1],v[2],(float)v[3],(float)v[4]));
                settle=change?15:0;steady=0;held=0;return;
            }
            if(wait<6+settle)return;
            var d=TroopRenderer.drawn(e);boolean drawn=d!=null&&d.pose!=null&&d.last!=null&&d.count>lastCount;lastCount=d==null?0:d.count;
            boolean gilt=drawn&&"gold".equals(d.last.cloth());
            // the relief is what the shot claims: drawn (the troop is near) unless the shot is the flat one; a troop without relief has none to claim
            boolean raised=drawn&&(m.reliefKeys().isEmpty()||s.flat()==(d.last.relief()==null));
            float[] want=pose(m,s,shield(s.troop()));boolean posed=drawn&&Anim.maxRot(d.pose,want)<=1&&Anim.maxPos(d.pose,want)<=0.1;
            var p=c.player;boolean there=Math.abs(p.getX()-(site.getX()+.5+cam[0]))<.05&&Math.abs(p.getEyeY()-(site.getY()+cam[1]))<.05&&Math.abs(p.getZ()-(site.getZ()+.5+cam[2]))<.05
                &&Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw()-(float)cam[3]))<1&&Math.abs(p.getPitch()-cam[4])<1;
            steady=gilt&&posed&&there&&raised?steady+1:0;held++;
            if(steady>=3||held>300){
                if(!shotReady(c,s.name()))return;
                if(steady<3)bad.add(s.name()+": taken anyway after 300 ticks (drawn "+drawn+", in gold "+gilt+", pose "+posed+", camera "+there+", relief "+raised+")");
                shot(c,s.name());si++;wait=0;steady=0;held=0;
            }
        }
        /** The bones of a model forced to a shot's clip and second, then back to rest. */
        static float[] pose(TroopModel<?> m,Shot s,boolean shield){
            m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(s.clip(),s.at()),TroopAnimator.row(m),shield);
            float[] out=m.snapshot(null);m.getPart().traverse().forEach(ModelPart::resetTransform);return out;
        }
        static double yawOf(double dx,double dz){return Math.toDegrees(Math.atan2(-dx,dz));}
        /** The camera of a shot: {dx, eye, dz, yaw, pitch}, from the platform's centre where the troop stands and the layer it stands on. */
        double[] view(Shot s,TroopModel<?> m){
            double y=Math.toRadians(YAW),fx=-Math.sin(y),fz=Math.cos(y),rx=-Math.cos(y),rz=-Math.sin(y); // the troop's front and right on the ground
            if(s.hand()==null){
                double a=Math.toRadians(s.angle()),ux=Math.cos(a)*fx+Math.sin(a)*rx,uz=Math.cos(a)*fz+Math.sin(a)*rz;
                return new double[]{DIST*ux,EYE,DIST*uz,yawOf(-ux,-uz),0};
            }
            double[] g=grip(m,new TroopAnimator.Forced(s.clip(),s.at()),shield(s.troop())); // the weapon's grip, blocks from the feet in world axes
            return switch(s.hand()){
                case "front"->new double[]{g[0]+CLOSE*fx,g[1],g[2]+CLOSE*fz,yawOf(-fx,-fz),0};
                case "side"->new double[]{g[0]+CLOSE*rx,g[1],g[2]+CLOSE*rz,yawOf(-rx,-rz),0};
                default->new double[]{g[0],g[1]+CLOSE,g[2],YAW,90};
            };
        }
        /** The renderer's matrix at the entity: its body yaw, the vanilla flip and the 1.501 lift (TroopRenderer.setupTransforms, LivingEntityRenderer.render). */
        static MatrixStack base(){MatrixStack ms=new MatrixStack();ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f-YAW));ms.scale(-1,-1,1);ms.translate(0,-1.501f,0);return ms;}
        /** A world offset from the feet in the troop's own axes {right, up, front}, times scale (16 turns blocks to px). */
        static double[] own(Vector4f v,double scale){double y=Math.toRadians(YAW);return new double[]{(-v.x*Math.cos(y)-v.z*Math.sin(y))*scale,v.y*scale,(-v.x*Math.sin(y)+v.z*Math.cos(y))*scale};}
        /** Where the weapon part's origin (its grip offset) lies in a pose: world axes, blocks from the feet. */
        static double[] grip(TroopModel<?> m,TroopAnimator.Forced f,boolean shield){
            m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,f,TroopAnimator.row(m),shield);
            var gp=TroopModels.weapon(m.weaponId()).grip();MatrixStack ms=base();for(ModelPart p:m.handPath(m.hand()))p.rotate(ms);
            Vector4f v=new Vector4f(gp.pivotX/16f,gp.pivotY/16f,gp.pivotZ/16f,1f);ms.peek().getPositionMatrix().transform(v);
            m.getPart().traverse().forEach(ModelPart::resetTransform);return new double[]{v.x,v.y,v.z};
        }

        // ---- the facts ----
        static String f3(double[] v){return String.format(Locale.ROOT,"(R %.2f, U %.2f, F %.2f)",v[0],v[1],v[2]);}
        static double[] minus(double[] a,double[] b){return new double[]{a[0]-b[0],a[1]-b[1],a[2]-b[2]};}
        /** "[p2-turn-fact]" lines: for each troop's poses at angle 0, the weapon hand, the grip, the weapon's axes and boxes and the off hand, in px in the troop's own axes. */
        void facts(){
            for(String t:TROOPS){
                var m=TroopModels.model(t);var w=m==null?null:TroopModels.weapon(m.weaponId());
                if(m==null||w==null||TroopAnimator.row(m)==null){System.out.println("[p2-turn-fact] "+t+": no model, weapon or row");continue;}
                for(Shot s:shots)if(s.hand()==null&&!s.bare()&&!s.flat()&&s.troop().equals(t)&&s.angle()==0)try{fact(m,w,s);}catch(RuntimeException x){System.out.println("[p2-turn-fact] "+s.name()+": "+x);}
            }
            for(String t:TROOPS){var m=TroopModels.model(t);if(m!=null)m.getPart().traverse().forEach(ModelPart::resetTransform);}
        }
        void fact(TroopModel<?> m,TroopModels.Weapon w,Shot s){
            m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(s.clip(),s.at()),TroopAnimator.row(m),shield(s.troop()));
            String hand=m.hand(),off="left".equals(hand)?"right":"left";List<ModelPart> path=m.handPath(hand);
            MatrixStack ms=base();for(ModelPart p:path)p.rotate(ms);double[] h=own(pt(ms,0,0,0),16);
            MatrixStack mo=base();for(ModelPart p:m.handPath(off))p.rotate(mo);double[] o=own(pt(mo,0,0,0),16);
            // the arm that holds it: its box, relative to the hand's pivot
            List<double[]> arm=new ArrayList<>();if(path.size()>=2){MatrixStack ma=base();for(int k=0;k<path.size()-2;k++)path.get(k).rotate(ma);
                path.get(path.size()-2).forEachCuboid(ma,(entry,p,i,q)->{if(p.isEmpty())arm.add(boxOf(entry,q));});}
            // the weapon: axes of its frame (the grip pixel's centre is its origin) and the rest frame's bounds, relative to the hand's pivot
            MatrixStack mw=base();for(ModelPart p:path)p.rotate(mw);w.grip(mw);
            double[][] axes=new double[3][];
            for(int k=0;k<3;k++){Vector4f a=new Vector4f(k==0?1:0,k==1?1:0,k==2?1:0,0);mw.peek().getPositionMatrix().transform(a);axes[k]=own(a,1);}
            double[] all=null;float[] bd=w.frames().get(0).bounds();
            for(int cc=0;cc<8;cc++){Vector4f v=new Vector4f(((cc&1)==0?bd[0]:bd[3])/16f,((cc&2)==0?bd[1]:bd[4])/16f,((cc&4)==0?bd[2]:bd[5])/16f,1f);mw.peek().getPositionMatrix().transform(v);double[] q=own(v,16);
                if(all==null)all=new double[]{q[0],q[1],q[2],q[0],q[1],q[2]};else for(int k=0;k<3;k++){all[k]=Math.min(all[k],q[k]);all[k+3]=Math.max(all[k+3],q[k]);}}
            StringBuilder sb=new StringBuilder("[p2-turn-fact] "+s.troop()+" "+s.pose()+" ("+s.clip()+" at "+String.format(Locale.ROOT,"%.2f",s.at())+" s), px in the troop's own axes (R right, U up, F front), weapon "+m.weaponId()+": ");
            sb.append("hand bone "+m.handBone(hand)+" pivot from the feet ").append(f3(h)).append("; off hand ").append(m.handBone(off)).append(" pivot from the weapon hand ").append(f3(minus(o,h)));
            if(!arm.isEmpty())sb.append("; arm box from the hand pivot R ").append(rng(arm.get(0),0,h)).append(" U ").append(rng(arm.get(0),1,h)).append(" F ").append(rng(arm.get(0),2,h));
            sb.append("; weapon frame axes +x ").append(f3(axes[0])).append(" +y ").append(f3(axes[1])).append(" +z ").append(f3(axes[2]));
            if(all!=null)sb.append("; weapon extent from the hand pivot R ").append(rng(all,0,h)).append(" U ").append(rng(all,1,h)).append(" F ").append(rng(all,2,h));
            System.out.println(sb);
        }
        static Vector4f pt(MatrixStack ms,double x,double y,double z){Vector4f v=new Vector4f((float)(x/16),(float)(y/16),(float)(z/16),1f);ms.peek().getPositionMatrix().transform(v);return v;}
        /** A cuboid's extent in the troop's own axes, px from the feet: {minR, minU, minF, maxR, maxU, maxF}. */
        static double[] boxOf(MatrixStack.Entry entry,ModelPart.Cuboid q){
            double[] r=null;
            for(float x:new float[]{q.minX,q.maxX})for(float y:new float[]{q.minY,q.maxY})for(float z:new float[]{q.minZ,q.maxZ}){
                Vector4f v=new Vector4f(x/16f,y/16f,z/16f,1f);entry.getPositionMatrix().transform(v);double[] c=own(v,16);
                if(r==null)r=new double[]{c[0],c[1],c[2],c[0],c[1],c[2]};else for(int k=0;k<3;k++){r[k]=Math.min(r[k],c[k]);r[k+3]=Math.max(r[k+3],c[k]);}}
            return r;
        }
        static String rng(double[] b,int k,double[] h){return String.format(Locale.ROOT,"%.2f..%.2f",b[k]-h[k],b[k+3]-h[k]);}
    }

    /**
     * Task 5c: the troops' idle tells and the House attack flavour (suite tells). The tables (p2-tells-cover: every sheet troop has one tell,
     * in its rig's bones, 0.8 to 2.0 s, two to four keys a bone, ending at rest and moving something; p2-tells-keys: the brief's angle at
     * the tell's peak, bone by bone), the start rule on a State (p2-tells-period: two knights, ids apart mod 40, each start 160 ticks after
     * the last, none while walking, attacking, braced, posed or dead; the frame at the peak is the forced tell; a State stepped 7 ticks at a time starts a tell on the first frame after each
     * boundary), the lookup order
     * (p2-tells-lookup, through TroopTells.clip and through TroopAnimator.clip on the real models), the overrides (p2-tells-overrides) and the
     * House attacks (p2-house-attacks: five variants of 0.5 to 0.6 s, each 10 degrees off the biped attack at its strike and ending at rest, every pair of them with the gold knight's
     * weapon tip 0.4 blocks apart or an arm bone 30 degrees apart at their strikes, the brief's angles bone by bone), the cull reach (p2-tells-cull: every troop model's tell, through the carries, inside it). Then the shots,
     * all through TroopAnimator.forced on gold troops at the platform's centre, three-quarter front, the camera 3 blocks off so a hand raised overhead stays in the frame (the tells from the left,
     * the attacks from the right): tells-TROOP for every
     * troop with a model at its tell's peak, tells-rest-TROOP the same troop at rest, and tells-attack-HOUSE for the five variants on a gold
     * knight at their strikes (p2-tells-shots: each taken lit, in gold, the camera where it was set and the pose drawn as forced).
     * tools/build_skins.py --contact turns the tells-TROOP shots into the contact sheet.
     */
    static final class Tells extends Stage {
        static final double DIST=3.0,EYE=1.25,PARK=24;
        static final float YAW=180;
        /**
         * The angles round the troop the camera takes (0 in front, 90 on its right, 270 on its left): the tells from in front and to the left, where a shield lifts; the archer's from its
         * left side, where the raised bow shows in profile (from 315 the raised bow was a thin stick with the bow's yaw 20 degrees one side of the draw's profile, as it is here, and again
         * 20 degrees the other side, and with the draw's own yaw; the 315 camera looks along its plane); the House attacks from in front and to the
         * right, the sword arm's side (a gold knight's shield is on its left).
         */
        static final int TELL_ANGLE=315,ATTACK_ANGLE=45;
        static final Map<String,Integer> TELL_ANGLES=Map.of("archer",270);
        /** One shot: its file name, the troop it is of, the clip forced on it and the angle round the troop the camera takes. */
        record Shot(String name,String troop,TroopAnimator.Forced forced,int angle){}
        final List<Shot> shots=new ArrayList<>();final List<String> ids=new ArrayList<>();
        final Map<String,GravesAndGlory.Unit> units=new LinkedHashMap<>();
        int si,held,steady,lastCount,settle,stable,unlit;String on="";double[] cam;final List<String> bad=new ArrayList<>();
        String name(){return "tells";}
        Tells(){
            Turn.hook();
            ids.addAll(new TreeSet<>(TroopModels.ids()));
            for(String t:ids){
                int angle=TELL_ANGLES.getOrDefault(t,TELL_ANGLE);
                shots.add(new Shot("tells-"+t,t,new TroopAnimator.Forced(TroopTells.TELL,TroopTells.PEAKS.get(t)),angle));
                shots.add(new Shot("tells-rest-"+t,t,new TroopAnimator.Forced("idle",0f),angle));
            }
            if(ids.contains("knight"))for(var h:Spec.HOUSES)shots.add(new Shot("tells-attack-"+h.id(),"knight",new TroopAnimator.Forced("attack",HouseAttacks.STRIKE.get(h.id()),h.id()),ATTACK_ANGLE));
        }
        void cast(GravesAndGlory.Battle b){
            for(int i=0;i<ids.size();i++){var u=post(b,gold,ids.get(i),0,0,YAW);units.put(ids.get(i),u);place(u,i,false);}
            camera(b,0,EYE,DIST,0,0);
        }
        /** Server: a troop at the platform's centre facing north, or parked high over the floor out of every view (index i keeps the parked ones apart). */
        void place(GravesAndGlory.Unit u,int i,boolean up){
            Vec3d where=up?at(0,0):at(5*(i-1),0).add(0,PARK,0);
            pins.removeIf(p->p.mob()==u.mob);pins.add(new Pin(u.mob,where,YAW));pin(u.mob,where,YAW);
        }
        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
            if(finished)return;
            if(tick>12000){report("p2-tells-timeout",false,"stuck at shot "+si+" of "+shots.size()+", client step "+cs+", server stage "+stage);end(c);return;}
            switch(cs){
                case 0->{boolean lit=litStart(c);stable=lit?stable+1:0;
                    // a platform that is only partly lit is no case for litStart's all-dark recovery: it would wait for ever (the shots would fail on p2-tells-light)
                    if(lit)unlit=0;else if(awayFor==0&&c.world!=null&&site!=null&&c.worldRenderer!=null&&c.worldRenderer.isTerrainRenderComplete()&&++unlit>=600&&recoveries<3)recover(c,"start","only partly lit after 600 ticks");
                    if(stable>=40&&ready&&!ids.isEmpty()&&posted.size()==ids.size()&&posted.stream().allMatch(u->client(c,u.mob)!=null)){
                        cover();keys();period();lookup();overrides();attacks();cull();c.options.hudHidden=true;wait=0;cs=1;}
                    else if(++wait%600==0)System.out.println("[p2-wait] tells step 0 after "+wait+" ticks: ready="+ready+", posted "+posted.size()+" of "+ids.size()+"; "+litWhy(c));}
                case 1->shoot(c);
                case 2->{if(++wait==5){report("p2-tells-shots",bad.isEmpty()&&!shots.isEmpty(),shots.size()+" shots ("+ids.size()+" troops with a model, each at its tell's peak and at rest, and the five House attacks on a knight)"
                    +(bad.isEmpty()?", each taken lit, in gold, with the camera where it was set (0.05 blocks, 1 degree) and the pose drawn as forced (1 degree)":", not as asked: "+bad));serverDone=true;end(c);}}
                default->{}
            }
        }
        public void cleanup(MinecraftClient c){Turn.noTarget=false;TroopAnimator.forced.clear();super.cleanup(c);}
        boolean shield(String troop){return !units.get(troop).mob.getOffHandStack().isEmpty();}

        // ---- the tables ----
        /** The rig of a sheet troop (TroopModels.RIGS by its entity), or null. */
        static String rigOf(Spec.TroopsRow row){return TroopModels.RIGS.get(row.entity());}
        static Spec.TroopsRow row(String troop){return Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(troop)).findFirst().orElse(null);}
        /** Whether a clip's last keyframe on every bone is at rest: rotations within 1 degree, offsets within 0.1 px. */
        static boolean endsAtRest(Animation a,List<String> bad,String what){
            boolean ok=true;
            for(var e:a.boneAnimations().entrySet())for(var t:e.getValue()){
                var last=t.keyframes()[t.keyframes().length-1].target();boolean rot=t.target()==net.minecraft.client.render.entity.animation.Transformation.Targets.ROTATE;
                double worst=Math.max(Math.abs(rot?Math.toDegrees(last.x()):last.x()),Math.max(Math.abs(rot?Math.toDegrees(last.y()):last.y()),Math.abs(rot?Math.toDegrees(last.z()):last.z())));
                if(worst>(rot?1:.1)){ok=false;bad.add(what+" "+e.getKey()+" ends "+String.format(Locale.ROOT,"%.2f",worst)+(rot?" degrees":" px")+" off rest");}
            }
            return ok;
        }
        /**
         * p2-tells-cover: TELLS holds exactly the sheet's 30 troop ids, and each tell is a once clip of 0.8 to 2.0 s that names only bones of its
         * troop's rig, 2 to 4 keyframes on each, ends at rest (the last keyframe of every bone within 1 degree; the clip sampled at its end, on the
         * rig, against rest) and moves some bone 5 degrees by its peak, which lies inside the clip.
         */
        void cover(){
            List<String> bad=new ArrayList<>();int n=0;double least=999;String leastId="";
            Set<String> sheet=new TreeSet<>();for(var t:Spec.TROOPS)sheet.add(t.id());
            Set<String> have=new TreeSet<>(TroopTells.TELLS.keySet());
            if(!have.equals(sheet)){Set<String> missing=new TreeSet<>(sheet);missing.removeAll(have);Set<String> extra=new TreeSet<>(have);extra.removeAll(sheet);bad.add("TELLS holds "+have.size()+" tells for the sheet's "+sheet.size()+" troops, missing "+missing+", not on the sheet "+extra);}
            if(sheet.size()!=30)bad.add("the sheet has "+sheet.size()+" troops, not 30");
            for(var row:Spec.TROOPS){
                var a=TroopTells.TELLS.get(row.id());String rig=rigOf(row);var m=rig==null?null:TroopModels.rig(rig);
                if(a==null||m==null){bad.add(row.id()+": "+(a==null?"no tell":"no rig model for "+row.entity()));continue;}
                n++;float len=a.lengthInSeconds();Float peak=TroopTells.PEAKS.get(row.id());
                if(len<TroopTells.MIN_SECONDS-1e-4f||len>TroopTells.MAX_SECONDS+1e-4f)bad.add(row.id()+" lasts "+len+" s");
                if(a.looping())bad.add(row.id()+" loops (a tell is a once clip)");
                for(var e:a.boneAnimations().entrySet()){
                    if(!m.bones().contains(e.getKey()))bad.add(row.id()+" moves "+e.getKey()+", which the "+rig+" rig lacks");
                    for(var t:e.getValue()){int k=t.keyframes().length;if(k<2||k>4)bad.add(row.id()+" "+e.getKey()+" has "+k+" keyframes");
                        if(t.target()==net.minecraft.client.render.entity.animation.Transformation.Targets.SCALE)bad.add(row.id()+" "+e.getKey()+" scales");}
                }
                endsAtRest(a,bad,row.id());
                float[] rest=Anim.sample(m,null,0),end=Anim.sample(m,a,len);double off=Math.max(Anim.maxRot(end,rest),Anim.maxPos(end,rest)/.1);
                if(Anim.maxRot(end,rest)>1||Anim.maxPos(end,rest)>.1)bad.add(row.id()+" sampled at its end is "+String.format(Locale.ROOT,"%.2f",off)+" off rest");
                if(peak==null||peak<=0||peak>=len)bad.add(row.id()+" has no peak inside its "+len+" s");
                else{double moved=Anim.maxRot(Anim.sample(m,a,peak),rest);if(moved<least){least=moved;leastId=row.id();}if(moved<5)bad.add(row.id()+" moves only "+String.format(Locale.ROOT,"%.1f",moved)+" degrees at its peak");}
            }
            report("p2-tells-cover",bad.isEmpty()&&n==30,n+" tells for "+sheet.size()+" troops: each a once clip of "+TroopTells.MIN_SECONDS+" to "+TroopTells.MAX_SECONDS+" s in its rig's bones, 2 to 4 keyframes a bone, ending at rest within 1 degree, moving 5 degrees or more by its peak (the least "
                +String.format(Locale.ROOT,"%.0f",least)+" at "+leastId+")"+(bad.isEmpty()?"":"; wrong: "+bad));
        }
        /** One headline angle of a tell: the troop, a bone, a channel (p pitch, y yaw, r roll, in degrees), the second it is read at (negative: the tell's peak) and the value. */
        record TKey(String troop,String bone,char chan,float at,float value){}
        static TKey tk(String troop,String bone,char chan,float value){return new TKey(troop,bone,chan,-1f,value);}
        static TKey tk(String troop,String bone,char chan,float at,float value){return new TKey(troop,bone,chan,at,value);}
        /**
         * The tell table, bone by bone (a hunter's sniff and an arbalist's crank read at both ends): the brief's angles, but for the six of fix round 1, where the
         * brief's angle failed the Tell line's read: the shield arm adds to the 45 degree carry, so the knight's and the sentinel's -50 and -40 (the shield tipped flat
         * over the head) are -15 and a 1 px lift, the marshal's -110 (155 in all) is -65 (110 in all), the mage's wrist turns the staff forward (75), and the archer's
         * bow arm is -85, its hand taking the draw's turn but for 20 degrees of the bow's rest yaw (hand yaw -25), so the bow stands upright before the eye. The 20 degrees do not
         * turn its face to the three-quarter front: the bow is edge-on from 315, which is why the archer's cell is shot from 270 (the brief's -70 laid it along the forearm; all 45
         * degrees off left it edge-on there as well).
         */
        static final List<TKey> TELL_KEYS=List.of(
            tk("knight","left_arm",'p',-15),tk("knight","left_arm",'u',1),tk("knight","head",'p',-5),
            tk("archer","left_arm",'p',-85),tk("archer","left_hand",'p',90),tk("archer","left_hand",'y',-TroopTells.ARCHER_YAW_OFF),tk("archer","left_hand",'r',90),tk("archer","head",'p',-5),
            tk("sentinel","left_arm",'p',-15),tk("sentinel","left_arm",'u',1),tk("sentinel","body",'p',3),tk("longbow","left_arm",'p',-60),tk("longbow","left_arm",'r',25),
            tk("mage","right_arm",'p',-40),tk("mage","right_hand",'p',TroopTells.MAGE_WRIST),tk("mage","head",'p',8),tk("gl_marshal","left_arm",'p',-65),tk("gl_marshal","head",'p',-15),
            tk("revenant","head",'r',20),tk("revenant","right_arm",'p',-10),tk("bonebow","right_arm",'y',-30),
            tk("reaper","right_arm",'p',-60),tk("reaper","body",'y',10),tk("stalker","body",'p',5),tk("stalker","right_arm",'y',-10),tk("stalker","left_arm",'y',10),
            tk("hexer","right_arm",'p',-70),tk("gb_hollowking","head",'r',-15),tk("gb_hollowking","head",'p',-10),
            tk("wm_hound","head",'p',20),tk("wm_hound","jaw",'p',25),
            tk("wm_hunter","head",'p',.4f,-20),tk("wm_hunter","head",'y',.4f,-15),tk("wm_hunter","head",'y',.9f,15),
            tk("wm_stalker","left_leg_1",'r',-30),tk("wm_tusk","right_arm",'p',-20),tk("wm_tusk","body",'p',5),tk("wm_seer","head",'p',-40),tk("wm_oldhorn","body",'p',-15),
            tk("ic_hearthguard","arms_folded",'p',-8),tk("ic_hearthguard","head",'p',8),
            tk("ic_arbalist","right_arm",'r',.35f,20),tk("ic_arbalist","right_arm",'r',.8f,-20),tk("ic_breaker","body",'r',8),tk("ic_ballista","right_arm",'p',-40),tk("ic_mortar","head",'p',-30),
            tk("ic_colossus","right_arm",'p',-30),tk("ic_colossus","left_arm",'p',-30),
            tk("tc_raider","body",'p',10),tk("tc_raider","right_arm",'p',-10),tk("tc_raider","left_arm",'p',-10),
            tk("tc_harpooner","right_arm",'p',-150),tk("tc_tideguard","right_arm",'p',-160),tk("tc_mist","arms_folded",'p',-15),tk("tc_brute","head",'p',20),tk("tc_admiral","right_arm",'p',-160));
        /** p2-tells-keys: every angle of the table, read off the tell at its peak (or the second given) within 2 degrees (0.2 px), each troop with at least one. */
        void keys(){
            List<String> bad=new ArrayList<>();Set<String> covered=new HashSet<>();
            for(TKey k:TELL_KEYS){
                var row=row(k.troop());String rig=row==null?null:rigOf(row);var m=rig==null?null:TroopModels.rig(rig);var a=TroopTells.TELLS.get(k.troop());
                if(m==null||a==null){bad.add(k.troop()+": no rig or tell");continue;}
                covered.add(k.troop());float at=k.at()<0?TroopTells.PEAKS.get(k.troop()):k.at();
                double got=Anim.keyValue(m,a,Anim.key(rig,TroopTells.TELL,k.bone(),k.chan(),at,k.value()));
                if(Double.isNaN(got)||Math.abs(got-k.value())>(k.chan()=='u'?.2:2))bad.add(String.format(Locale.ROOT,"%s %s %c at %.2f s: %.1f, want %.1f",k.troop(),k.bone(),k.chan(),at,got,k.value()));
            }
            for(var t:Spec.TROOPS)if(!covered.contains(t.id()))bad.add(t.id()+": not in the table");
            report("p2-tells-keys",bad.isEmpty(),TELL_KEYS.size()+" angles over "+covered.size()+" troops, each read off the tell at its peak within 2 degrees"+(bad.isEmpty()?"":"; wrong: "+bad));
        }

        /**
         * p2-tells-period, on the State: a knight standing still starts a tell on the tick (tick + id % 40) % 160 is 0, 160 ticks after the one before; ids 5, 17 and
         * 45 over 400 ticks (45 begins with 5's tick: ids 40 apart share it; 17 begins on another), and id 40 from tick 0. The frame 16 ticks into a tell is the forced
         * tell at its peak (3 degrees: the idle's own sway). It starts none while it walks, runs, braces or holds, stands in the fight (attacking, or swinging every
         * second), has its shield up, kneels as a scene pose, or is dead; a tell under way is dropped the moment the knight walks.
         */
        void period(){
            var m=TroopModels.model("knight");List<String> bad=new ArrayList<>();
            if(m==null){report("p2-tells-period",false,"no knight model");return;}
            var calm=Anim.view("biped",false,false,false,false,false,false,false,"",null,null);
            var row=TroopAnimator.row(m);float peak=TroopTells.PEAKS.get("knight");int into=Math.round(peak*20);
            Map<Integer,List<Integer>> starts=new LinkedHashMap<>();double sway=0;
            for(int id:new int[]{5,17,45,40}){
                var s=new TroopAnimator.State(id);List<Integer> at=new ArrayList<>();float seen=Float.NaN;
                for(int t=0;t<400;t++){
                    TroopAnimator.simulate(m,s,calm,t,false);float st=s.tellStart();
                    if(!Float.isNaN(st)&&(Float.isNaN(seen)||st!=seen))at.add((int)Math.floor(st));seen=st;
                    if(id==5&&!at.isEmpty()&&t==at.get(0)+into){
                        float[] frame=m.snapshot(null);m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(TroopTells.TELL,peak),row,false);
                        sway=Anim.maxRot(frame,m.snapshot(null));if(sway>3)bad.add("16 ticks into its tell the knight is "+String.format(Locale.ROOT,"%.1f",sway)+" degrees off the forced tell at its peak (want 3 or less)");
                    }
                }
                starts.put(id,at);
            }
            m.getPart().traverse().forEach(ModelPart::resetTransform);
            if(!starts.get(5).equals(List.of(155,315)))bad.add("id 5 started its tells on "+starts.get(5)+", want [155, 315]");
            if(!starts.get(17).equals(List.of(143,303)))bad.add("id 17 started its tells on "+starts.get(17)+", want [143, 303]");
            if(!starts.get(45).equals(starts.get(5)))bad.add("id 45 started on "+starts.get(45)+", but 45 and 5 are 40 apart and share a tick: "+starts.get(5));
            if(!starts.get(40).equals(List.of(0,160,320)))bad.add("id 40 started its tells on "+starts.get(40)+", want [0, 160, 320]");
            for(var e:starts.entrySet())for(int i=1;i<e.getValue().size();i++)if(e.getValue().get(i)-e.getValue().get(i-1)!=TroopTells.PERIOD_TICKS)bad.add("id "+e.getKey()+" tells are not "+TroopTells.PERIOD_TICKS+" ticks apart: "+e.getValue());
            if(starts.get(5).get(0).equals(starts.get(17).get(0)))bad.add("ids 5 and 17 start on the same tick");
            // the fights, the stances and the poses: none starts
            Map<String,java.util.function.IntFunction<TroopAnimator.View>> still=new LinkedHashMap<>();
            still.put("walking",t->Anim.with(calm,false,false,.5f));
            still.put("attacking between swings",t->Anim.view("biped",false,false,false,true,false,false,false,"",null,null));
            still.put("swinging every second",t->Anim.view("biped",false,t%20==0,false,true,false,false,false,"",null,null));
            still.put("hold stance",t->Anim.view("biped",false,false,false,false,false,false,false,"hold",null,null));
            still.put("brace stance",t->Anim.view("biped",false,false,false,false,false,false,false,"brace",null,null));
            still.put("run stance, standing",t->Anim.view("biped",false,false,false,false,false,false,false,"run",null,null));
            still.put("shield up",t->Anim.view("biped",false,false,false,false,false,true,false,"",null,null));
            still.put("kneeling (a scene pose)",t->Anim.view("biped",false,false,false,false,false,false,false,"","kneel",null));
            still.put("dead",t->Anim.view("biped",true,false,false,false,false,false,false,"",null,null));
            for(var e:still.entrySet()){
                var s=new TroopAnimator.State(40);int started=0;
                for(int t=0;t<400;t++){TroopAnimator.simulate(m,s,e.getValue().apply(t),t,false);if(!Float.isNaN(s.tellStart()))started++;}
                if(started>0)bad.add("a knight "+e.getKey()+" had a tell live on "+started+" of 400 ticks");
            }
            var s=new TroopAnimator.State(40);boolean under=false;
            for(int t=0;t<=14;t++){TroopAnimator.simulate(m,s,t<12?calm:Anim.with(calm,false,false,.5f),t,false);if(t==11)under=!Float.isNaN(s.tellStart());}
            if(!under)bad.add("id 40 had no tell under way at tick 11");
            if(!Float.isNaN(s.tellStart()))bad.add("a tell under way when the knight began to walk was still live 3 ticks later");
            // a hitch: stepped 7 ticks at a time, the frame that crosses a boundary starts its tell, once (no boundary is skipped, none is started twice)
            Map<Integer,List<Integer>> hitched=new LinkedHashMap<>();
            for(int id:new int[]{5,40}){
                var hs=new TroopAnimator.State(id);List<Integer> at=new ArrayList<>();float seen=Float.NaN;
                for(int t=0;t<400;t+=7){TroopAnimator.simulate(m,hs,calm,t,false);float st=hs.tellStart();if(!Float.isNaN(st)&&(Float.isNaN(seen)||st!=seen))at.add((int)Math.floor(st));seen=st;}
                List<Integer> want=id==5?List.of(161,315):List.of(0,161,322);
                if(!at.equals(want))bad.add("id "+id+" stepped 7 ticks at a time started its tells on "+at+", want "+want+" (the first frame after each boundary)");
                hitched.put(id,at);
            }
            // a long gap: a troop that was culled (the animator ran no frame for it) starts nothing on its first frame back, and starts on its own next boundary; a gap of CATCH_UP_TICKS still
            // catches a boundary up, one tick more does not
            List<Integer> back=new ArrayList<>();int away=0;
            {
                var ls=new TroopAnimator.State(5);
                for(int t:new int[]{100,300}){TroopAnimator.simulate(m,ls,calm,t,false);if(t==300&&!Float.isNaN(ls.tellStart()))bad.add("id 5 culled from tick 100 to 300 (the boundary at 155 fell in the gap) started a tell on its first frame back");}
                float seen=Float.NaN;
                for(int t=301;t<=340;t++){TroopAnimator.simulate(m,ls,calm,t,false);float st=ls.tellStart();if(!Float.isNaN(st)&&(Float.isNaN(seen)||st!=seen))back.add((int)Math.floor(st));seen=st;}
                if(!back.equals(List.of(315)))bad.add("id 5, back after 200 ticks away, started its tells on "+back+" in the next 40 ticks, want [315] (its own next boundary)");
                for(int gap:new int[]{TroopTells.CATCH_UP_TICKS,TroopTells.CATCH_UP_TICKS+1}){
                    var gs=new TroopAnimator.State(5);TroopAnimator.simulate(m,gs,calm,150,false);TroopAnimator.simulate(m,gs,calm,150+gap,false);
                    boolean started=!Float.isNaN(gs.tellStart());
                    if(started!=(gap<=TroopTells.CATCH_UP_TICKS))bad.add("id 5 with a gap of "+gap+" ticks across its boundary at 155 "+(started?"started":"did not start")+" a tell (a gap of "+TroopTells.CATCH_UP_TICKS+" or less catches up, a longer one does not)");
                    if(gap<=TroopTells.CATCH_UP_TICKS)away=gap;
                }
            }
            m.getPart().traverse().forEach(ModelPart::resetTransform);
            report("p2-tells-period",bad.isEmpty(),"ids 5, 17, 45 and 40 over 400 ticks started their tells on "+starts+", each 160 ticks after the last, 5 and 45 together and 17 apart, and ids 5 and 40 stepped 7 ticks at a time on "+hitched+" (the first frame after each boundary, none skipped); a troop culled for 200 ticks started nothing on its first frame back and then started on "+back+", its own next boundary, while a gap of "+away+" ticks still catches a boundary up and one of "+(away+1)+" does not; "
                +"the frame 16 ticks in is the forced tell at its peak within "+String.format(Locale.ROOT,"%.1f",sway)+" degrees; a knight "+String.join(", ",still.keySet())+" started none, and a tell under way gives way as it walks"+(bad.isEmpty()?"":"; wrong: "+bad));
        }

        /** The lookup cases made so far; the report counts them. */
        int cases;
        /** One lookup case: what it is, and the clip got against the clip wanted (the same object). */
        void same(List<String> bad,String what,Animation got,Animation want){cases++;if(got!=want||got==null)bad.add(what+": "+(got==null?"no clip":"not the clip wanted"));}
        /** The bones a clip moves with right and left swapped. */
        static Set<String> swapped(Animation a){
            Set<String> out=new TreeSet<>();
            for(String b:a.boneAnimations().keySet())out.add(b.startsWith("left_")?"right_"+b.substring(5):b.startsWith("right_")?"left_"+b.substring(6):b);
            return out;
        }
        /**
         * p2-tells-lookup: TroopTells.clip in its order, the troop's override, else the House's attack for a biped, else the rig's clip; and the same through TroopAnimator.clip on the
         * real models (the longbow's draw, the knight's attack and walk, the tell, which no troop plays mirrored).
         */
        void lookup(){
            List<String> bad=new ArrayList<>();cases=0;
            same(bad,"longbow bow_draw",TroopTells.clip("longbow","gilded","biped","bow_draw"),TroopTells.OVERRIDES.get("longbow").get("bow_draw"));
            same(bad,"knight attack (gilded)",TroopTells.clip("knight","gilded","biped","attack"),HouseAttacks.VARIANTS.get("gilded"));
            same(bad,"wm_hound attack (hound rig)",TroopTells.clip("wm_hound","wildmoot","hound","attack"),TroopClips.of("hound").get("attack"));
            same(bad,"knight walk",TroopTells.clip("knight","gilded","biped","walk"),TroopClips.of("biped").get("walk"));
            same(bad,"tc_tideguard attack (its override over the Tidecallers' variant)",TroopTells.clip("tc_tideguard","tidecallers","biped","attack"),TroopTells.OVERRIDES.get("tc_tideguard").get("attack"));
            same(bad,"tc_raider attack (the Tidecallers' variant)",TroopTells.clip("tc_raider","tidecallers","biped","attack"),HouseAttacks.VARIANTS.get("tidecallers"));
            same(bad,"ic_colossus attack (colossus rig)",TroopTells.clip("ic_colossus","ironclad","colossus","attack"),TroopClips.of("colossus").get("attack"));
            same(bad,"wm_stalker attack (spider rig)",TroopTells.clip("wm_stalker","wildmoot","spider","attack"),TroopClips.of("spider").get("attack"));
            same(bad,"tc_brute attack (hound rig)",TroopTells.clip("tc_brute","tidecallers","hound","attack"),TroopClips.of("hound").get("attack"));
            same(bad,"a bare rig's attack (no troop, no House)",TroopTells.clip("",null,"biped","attack"),TroopClips.of("biped").get("attack"));
            same(bad,"a knight's attack with no House",TroopTells.clip("knight",null,"biped","attack"),TroopClips.of("biped").get("attack"));
            same(bad,"knight hit (no variant for any clip but attack)",TroopTells.clip("knight","gilded","biped","hit"),TroopClips.of("biped").get("hit"));
            same(bad,"sentinel walk (its plod)",TroopTells.clip("sentinel","gilded","biped","walk"),TroopTells.OVERRIDES.get("sentinel").get("walk"));
            same(bad,"sentinel attack (the Gilded variant: an override of walk leaves it be)",TroopTells.clip("sentinel","gilded","biped","attack"),HouseAttacks.VARIANTS.get("gilded"));
            same(bad,"knight tell",TroopTells.clip("knight","gilded","biped","tell"),TroopTells.TELLS.get("knight"));
            // the mirror, read against the plain clip: another clip, on the bones with right and left swapped, and built once
            var draw=TroopTells.clip("longbow","gilded","biped","bow_draw"); var drawMirrored=TroopTells.clip("longbow","gilded","biped","bow_draw",true);cases++;
            if(draw==null||drawMirrored==null)bad.add("the longbow has no bow_draw, plain or mirrored");
            else{
                if(drawMirrored==draw)bad.add("the mirrored longbow draw is the plain one");
                if(!swapped(draw).equals(new TreeSet<>(drawMirrored.boneAnimations().keySet())))bad.add("the mirrored longbow draw moves "+new TreeSet<>(drawMirrored.boneAnimations().keySet())+", the plain one's bones swapped are "+swapped(draw));
                if(TroopTells.clip("longbow","gilded","biped","bow_draw",true)!=drawMirrored)bad.add("the mirrored longbow draw is built again at each look-up");
            }
            if(TroopTells.clip("no_such_troop","gilded","biped","tell")!=null)bad.add("a troop with no tell has one");
            if(TroopTells.clip("knight","gilded","biped","no_such_clip")!=null)bad.add("a clip that no rig has is not null");
            for(var h:Spec.HOUSES)if(!HouseAttacks.VARIANTS.containsKey(h.id()))bad.add("no attack for the House "+h.id());
            // through the animator, on the real models (hand and bow decide what is mirrored)
            for(String id:ids){
                var m=TroopModels.model(id);var row=TroopAnimator.row(m);if(m==null||row==null){bad.add(id+": no model or row");continue;}
                boolean bow=TroopAnimator.bow(row);
                same(bad,id+" tell through the animator",TroopAnimator.clip(m,"tell",bow),TroopTells.TELLS.get(id));
                var own=TroopTells.OVERRIDES.getOrDefault(id,Map.of()).get("bow_draw");
                if(own!=null)same(bad,id+" bow_draw through the animator",TroopAnimator.clip(m,"bow_draw",bow),"left".equals(m.hand())?own:TroopTells.clip(id,row.house(),m.rig,"bow_draw",true));
            }
            var knight=TroopModels.model("knight");
            if(knight!=null){
                same(bad,"knight attack through the animator",TroopAnimator.clip(knight,"attack",false),HouseAttacks.VARIANTS.get("gilded"));
                same(bad,"knight walk through the animator",TroopAnimator.clip(knight,"walk",false),TroopClips.of("biped").get("walk"));
                same(bad,"knight attack forced as the graveborn's",TroopAnimator.clip(knight,"attack",false,"graveborn"),HouseAttacks.VARIANTS.get("graveborn"));
            }
            var longbow=TroopModels.model("longbow");
            if(longbow!=null)same(bad,"longbow draw through the animator",TroopAnimator.clip(longbow,"bow_draw",true),TroopTells.OVERRIDES.get("longbow").get("bow_draw"));
            report("p2-tells-lookup",bad.isEmpty(),"the order override, House attack (biped attack only), rig clip, on "+cases+" cases through TroopTells.clip and on the real models through TroopAnimator.clip, and the mirrored draw on the swapped bones"+(bad.isEmpty()?"":"; wrong: "+bad));
        }

        /** One headline value of an override: the troop, the clip, a bone, a channel (p, y, r in degrees; u up in px), the second it is read at and the value. */
        record OKey(String troop,String clip,String bone,char chan,float at,float value){}
        static OKey ok(String troop,String clip,String bone,char chan,float at,float value){return new OKey(troop,clip,bone,chan,at,value);}
        static final List<OKey> OVERRIDE_KEYS=List.of(
            ok("sentinel","walk","right_leg",'p',.25f,-20),ok("sentinel","walk","left_leg",'p',.25f,20),ok("sentinel","walk","right_arm",'p',.25f,8),ok("sentinel","walk","left_arm",'p',.25f,-8),ok("sentinel","walk","body",'u',.125f,.5f),
            ok("longbow","bow_draw","left_arm",'p',.3f,-90),ok("longbow","bow_draw","left_arm",'r',.3f,25),ok("longbow","bow_draw","right_arm",'y',.65f,-70),
            ok("hexer","cast","right_arm",'p',0,-120),ok("hexer","cast","left_arm",'p',0,-30),
            ok("wm_seer","cast","head",'p',0,-50),ok("wm_seer","cast","right_arm",'p',0,-30),ok("wm_seer","cast","left_arm",'p',0,-30),
            ok("ic_arbalist","bow_draw","left_arm",'p',.65f,-90),ok("ic_arbalist","bow_draw","right_arm",'p',.65f,-90),ok("ic_arbalist","bow_draw","left_arm",'y',.65f,30),ok("ic_arbalist","bow_draw","right_arm",'y',.65f,-30),ok("ic_arbalist","bow_draw","right_arm",'r',.15f,12),
            ok("ic_ballista","bow_draw","left_arm",'p',.65f,-70),ok("ic_ballista","bow_draw","right_arm",'p',.65f,-70),ok("ic_ballista","bow_draw","right_arm",'p',.8f,-40),
            ok("ic_mortar","bow_draw","left_arm",'p',.65f,-150),ok("ic_mortar","bow_draw","right_arm",'p',.65f,-150),ok("ic_mortar","bow_draw","right_arm",'p',.8f,-30),
            ok("tc_harpooner","bow_draw","right_arm",'p',.65f,-170),ok("tc_harpooner","bow_draw","right_arm",'p',.8f,-60),
            ok("tc_tideguard","attack","right_arm",'p',.12f,-170),ok("tc_tideguard","attack","right_arm",'p',.25f,-100),ok("tc_tideguard","attack","body",'p',.25f,10),
            ok("tc_admiral","attack","right_arm",'p',.12f,-170),ok("tc_admiral","attack","right_arm",'p',.25f,-100),ok("tc_admiral","attack","body",'p',.25f,10));
        /**
         * p2-tells-overrides: exactly the ten overrides of the brief (sentinel walk, longbow bow_draw, hexer and wm_seer cast, the four bow_draws of ic_arbalist, ic_ballista, ic_mortar and
         * tc_harpooner, the attacks of tc_tideguard and tc_admiral); each is as long and of the same kind as the rig clip it replaces (the animator's blends and the bow's landmarks
         * assume it), in its rig's bones, ends where its kind ends (a loop on its first frame, a once clip at rest), differs from the rig's clip by 5 degrees somewhere, and
         * has the brief's angles bone by bone.
         */
        void overrides(){
            List<String> bad=new ArrayList<>();int n=0;
            Set<String> want=new TreeSet<>(List.of("sentinel/walk","longbow/bow_draw","hexer/cast","wm_seer/cast","ic_arbalist/bow_draw","ic_ballista/bow_draw","ic_mortar/bow_draw","tc_harpooner/bow_draw","tc_tideguard/attack","tc_admiral/attack"));
            Set<String> have=new TreeSet<>();for(var e:TroopTells.OVERRIDES.entrySet())for(String clip:e.getValue().keySet())have.add(e.getKey()+"/"+clip);
            if(!have.equals(want))bad.add("the overrides are "+have+", the brief's are "+want);
            for(var e:TroopTells.OVERRIDES.entrySet()){
                var row=row(e.getKey());String rig=row==null?null:rigOf(row);var m=rig==null?null:TroopModels.rig(rig);
                if(m==null){bad.add(e.getKey()+": no troop, rig or rig model");continue;}
                for(var c:e.getValue().entrySet()){
                    n++;String what=e.getKey()+" "+c.getKey();var a=c.getValue();var plain=TroopClips.of(rig).get(c.getKey());var kind=TroopClips.KINDS.get(c.getKey());
                    if(plain==null||kind==null){bad.add(what+": the rig has no such clip");continue;}
                    if(Math.abs(a.lengthInSeconds()-TroopClips.LENGTHS.get(c.getKey()))>1e-4)bad.add(what+" is "+a.lengthInSeconds()+" s, the clip it replaces "+TroopClips.LENGTHS.get(c.getKey()));
                    if(a.looping()!=(kind==TroopClips.Kind.LOOP))bad.add(what+" loops="+a.looping()+", but the clip it replaces is "+kind);
                    for(var bone:a.boneAnimations().keySet())if(!m.bones().contains(bone))bad.add(what+" moves "+bone+", which the "+rig+" rig lacks");
                    float[] rest=Anim.sample(m,null,0);double rot,pos;
                    if(kind==TroopClips.Kind.LOOP){var u=Anim.unwrapped(a);float[] first=Anim.sample(m,u,0),end=Anim.sample(m,u,a.lengthInSeconds());rot=Anim.maxRot(end,first);pos=Anim.maxPos(end,first);}
                    else{float[] end=Anim.sample(m,a,a.lengthInSeconds());rot=Anim.maxRot(end,rest);pos=Anim.maxPos(end,rest);}
                    if(rot>1||pos>.1)bad.add(String.format(Locale.ROOT,"%s ends %.2f degrees and %.2f px off where a %s clip ends",what,rot,pos,kind));
                    double apart=0;for(float t=0;t<=a.lengthInSeconds()+1e-4f;t+=.05f)apart=Math.max(apart,Anim.maxRot(Anim.sample(m,a,t),Anim.sample(m,plain,t)));
                    if(apart<5)bad.add(what+" is within "+String.format(Locale.ROOT,"%.1f",apart)+" degrees of the rig's clip");
                }
            }
            for(OKey k:OVERRIDE_KEYS){
                var row=row(k.troop());String rig=row==null?null:rigOf(row);var m=rig==null?null:TroopModels.rig(rig);var a=TroopTells.OVERRIDES.getOrDefault(k.troop(),Map.of()).get(k.clip());
                if(m==null||a==null){bad.add(k.troop()+" "+k.clip()+": no rig or override");continue;}
                double got=Anim.keyValue(m,a,Anim.key(rig,k.clip(),k.bone(),k.chan(),k.at(),k.value())),tol=k.chan()=='u'?.2:2;
                if(Double.isNaN(got)||Math.abs(got-k.value())>tol)bad.add(String.format(Locale.ROOT,"%s %s %s %c at %.2f s: %.2f, want %.2f",k.troop(),k.clip(),k.bone(),k.chan(),k.at(),got,k.value()));
            }
            report("p2-tells-overrides",bad.isEmpty()&&n==10,n+" overrides, each as long and of the kind of the clip it replaces, in its rig's bones, ending as its kind ends, 5 degrees or more from the rig's clip, "+OVERRIDE_KEYS.size()+" angles read off within 2 degrees (0.2 px)"+(bad.isEmpty()?"":"; wrong: "+bad));
        }

        /** One headline value of a House attack: the House, a bone, a channel, the second it is read at (negative: the strike) and the value. */
        record AKey(String house,String bone,char chan,float at,float value){}
        static AKey ak(String house,String bone,char chan,float value){return new AKey(house,bone,chan,-1f,value);}
        static AKey ak(String house,String bone,char chan,float at,float value){return new AKey(house,bone,chan,at,value);}
        static final List<AKey> ATTACK_KEYS=List.of(
            ak("gilded","left_arm",'p',-40),ak("gilded","left_arm",'p',0,-40),ak("gilded","right_arm",'p',.1f,-60),ak("gilded","right_arm",'p',-95),ak("gilded","body",'y',15),
            ak("graveborn","right_arm",'p',.15f,-170),ak("graveborn","right_arm",'p',0),ak("graveborn","body",'p',25),
            ak("wildmoot","body",'p',30),ak("wildmoot","right_arm",'p',-60),ak("wildmoot","right_arm",'y',.1f,70),ak("wildmoot","right_arm",'y',-40),
            ak("ironclad","right_arm",'p',.2f,-150),ak("ironclad","left_arm",'p',.2f,-150),ak("ironclad","right_arm",'p',-60),ak("ironclad","left_arm",'p',-60),ak("ironclad","body",'p',15),
            ak("tidecallers","right_arm",'p',-80),ak("tidecallers","right_arm",'y',.1f,90),ak("tidecallers","right_arm",'y',-60),ak("tidecallers","body",'y',25),ak("tidecallers","left_leg",'p',-20),
            // the wrist that lays the blade on the target (not in the brief's table: the biped attack turns it 75 at its contact; the blade rests 45 degrees up from level, so level or down at the strike takes the arm's lift plus 45, less the cut)
            ak("gilded","right_hand",'p',140),ak("graveborn","right_hand",'p',75),ak("wildmoot","right_hand",'p',105),ak("ironclad","right_hand",'p',135),ak("tidecallers","right_hand",'p',125));
        /** Two variants strike alike unless their weapon tips are this far apart (blocks) at their strikes. */
        static final double MIN_TIP=.4;
        static double dist(double[] a,double[] b){return Math.sqrt((a[0]-b[0])*(a[0]-b[0])+(a[1]-b[1])*(a[1]-b[1])+(a[2]-b[2])*(a[2]-b[2]));}
        /**
         * Where the weapon's tip lies at a second of a House's attack on the knight (the sword hand, the shield arm's carry on): the corner of the weapon frame's bounds farthest from the
         * grip, in blocks from the feet in the troop's own axes {right, up, front}.
         */
        static double[] tip(TroopModel<?> k,String house,float seconds){
            k.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(k,new TroopAnimator.Forced("attack",seconds,house),TroopAnimator.row(k),true);
            var w=TroopModels.weapon(k.weaponId());MatrixStack mw=Turn.base();for(ModelPart p:k.handPath(k.hand()))p.rotate(mw);w.grip(mw);
            float[] bd=w.frames().get(0).bounds();double[] best=null;double far=-1;
            for(int cc=0;cc<8;cc++){
                float x=(cc&1)==0?bd[0]:bd[3],y=(cc&2)==0?bd[1]:bd[4],z=(cc&4)==0?bd[2]:bd[5];double d=x*x+y*y+z*z;
                if(d>far){far=d;best=Turn.own(Turn.pt(mw,x,y,z),1);}
            }
            k.getPart().traverse().forEach(ModelPart::resetTransform);return best;
        }
        /**
         * p2-house-attacks: one variant for each of the sheet's Houses; each lasts 0.5 to 0.6 s with its strike inside it, differs from the biped attack by 10 degrees on some bone at its
         * strike, and ends at rest within 1 degree; every pair of variants, each at its own strike, has the gold knight's weapon tip MIN_TIP blocks apart (the whole rule: an arm
         * bone's angle on the bare rig does not carry the shield, and says little of what shows); the brief's angles read off bone by bone.
         */
        void attacks(){
            List<String> bad=new ArrayList<>();var m=TroopModels.rig("biped");
            if(m==null){report("p2-house-attacks",false,"no biped rig model");return;}
            float[] rest=Anim.sample(m,null,0);var plain=TroopClips.of("biped").get("attack");double least=999;String leastHouse="";
            Set<String> houses=new TreeSet<>();for(var h:Spec.HOUSES)houses.add(h.id());
            if(!new TreeSet<>(HouseAttacks.VARIANTS.keySet()).equals(houses)||!new TreeSet<>(HouseAttacks.STRIKE.keySet()).equals(houses))bad.add("the variants are for "+new TreeSet<>(HouseAttacks.VARIANTS.keySet())+" and the strikes for "+new TreeSet<>(HouseAttacks.STRIKE.keySet())+", the sheet's Houses are "+houses);
            for(String h:houses){
                var a=HouseAttacks.VARIANTS.get(h);Float strike=HouseAttacks.STRIKE.get(h);if(a==null||strike==null)continue;
                float len=a.lengthInSeconds();
                if(len<.5f-1e-4f||len>.6f+1e-4f)bad.add(h+" lasts "+len+" s");
                if(Math.abs(len-TroopClips.LENGTHS.get("attack"))>1e-4)bad.add(h+" is "+len+" s, the attack it replaces "+TroopClips.LENGTHS.get("attack")+" (the animator blends a once overlay out by the rig's length)");
                if(a.looping())bad.add(h+" loops");
                if(strike<=0||strike>=len)bad.add(h+" strikes at "+strike+", outside its "+len+" s");
                for(var bone:a.boneAnimations().keySet())if(!m.bones().contains(bone))bad.add(h+" moves "+bone+", which the biped rig lacks");
                float[] end=Anim.sample(m,a,len);if(Anim.maxRot(end,rest)>1||Anim.maxPos(end,rest)>.1)bad.add(h+" ends "+String.format(Locale.ROOT,"%.2f",Anim.maxRot(end,rest))+" degrees off rest");
                endsAtRest(a,bad,h);
                double off=Anim.maxRot(Anim.sample(m,a,strike),Anim.sample(m,plain,strike));if(off<least){least=off;leastHouse=h;}
                if(off<10)bad.add(h+" is only "+String.format(Locale.ROOT,"%.1f",off)+" degrees from the biped attack at its strike");
            }
            // every pair of variants, each at its own strike: the weapon tip of a gold knight 0.4 blocks apart or more (an any-bone 10 degrees let two chops through that ended on the same line)
            var knight=TroopModels.model("knight");double closest=999;String closestPair="";int pairs=0;
            if(knight==null||knight.weaponId()==null||TroopModels.weapon(knight.weaponId())==null||TroopAnimator.row(knight)==null)bad.add("no knight model, weapon or row to measure the weapon tip on");
            else{
                List<String> hs=new ArrayList<>(houses);
                for(int i=0;i<hs.size();i++)for(int j=i+1;j<hs.size();j++){
                    String x=hs.get(i),y=hs.get(j);var ax=HouseAttacks.VARIANTS.get(x);var ay=HouseAttacks.VARIANTS.get(y);Float sx=HouseAttacks.STRIKE.get(x),sy=HouseAttacks.STRIKE.get(y);
                    if(ax==null||ay==null||sx==null||sy==null)continue;
                    pairs++;double tip=dist(tip(knight,x,sx),tip(knight,y,sy));
                    System.out.println(String.format(Locale.ROOT,"[p2-house-pair] %s and %s at their strikes: weapon tips %.2f blocks apart",x,y,tip));
                    if(tip<closest){closest=tip;closestPair=x+" and "+y;}
                    if(tip<MIN_TIP)bad.add(x+" and "+y+" strike alike: the weapon tips are "+String.format(Locale.ROOT,"%.2f",tip)+" blocks apart (want "+MIN_TIP+")");
                }
            }
            for(AKey k:ATTACK_KEYS){
                var a=HouseAttacks.VARIANTS.get(k.house());if(a==null){bad.add(k.house()+": no attack");continue;}
                float at=k.at()<0?HouseAttacks.STRIKE.get(k.house()):k.at();
                double got=Anim.keyValue(m,a,Anim.key("biped","attack",k.bone(),k.chan(),at,k.value()));
                if(Double.isNaN(got)||Math.abs(got-k.value())>2)bad.add(String.format(Locale.ROOT,"%s %s %c at %.2f s: %.1f, want %.1f",k.house(),k.bone(),k.chan(),at,got,k.value()));
            }
            report("p2-house-attacks",bad.isEmpty(),houses.size()+" House attacks of "+HouseAttacks.VARIANTS.values().stream().map(a->String.valueOf(a.lengthInSeconds())).distinct().toList()+" s, each "+String.format(Locale.ROOT,"%.0f",least)+" degrees or more from the biped attack at its strike (the least, "+leastHouse
                +"), all "+pairs+" pairs of them with the weapon tips "+MIN_TIP+" blocks apart or more at their strikes (the closest, "+closestPair+": "
                +String.format(Locale.ROOT,"%.2f",closest)+" blocks), ending at rest within 1 degree, "+ATTACK_KEYS.size()+" angles read off within 2 degrees"+(bad.isEmpty()?"":"; wrong: "+bad));
        }

        /** p2-tells-cull: each troop model's tell, every 50 ms through the carries (applyForced: the shield arm, the bow's turn, arms forward), stays inside the cull reach the frustum test keeps it by. */
        void cull(){
            boolean ok=true;int models=0,samples=0;double worst=-999;String at="";List<String> out=new ArrayList<>();
            for(String id:ids){
                var m=TroopModels.model(id);var row=m==null?null:TroopAnimator.row(m);var a=TroopTells.TELLS.get(id);if(row==null||a==null)continue;models++;
                double[] reach=m.cullReach(false);double mine=-999;
                for(float s=0;s<=a.lengthInSeconds()+1e-4f;s+=.05f){
                    m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced(TroopTells.TELL,s),row,true);
                    double o=Anim.outside(m.extent(),reach);samples++;if(o>mine)mine=o;if(o>worst){worst=o;at=id+String.format(Locale.ROOT," at %.2f s",s);}
                    if(o>0&&out.size()<6)out.add(id+String.format(Locale.ROOT," at %.2f s: %.2f blocks outside",s,o));}
                m.getPart().traverse().forEach(ModelPart::resetTransform);
                ok&=mine<=0;
            }
            report("p2-tells-cull",ok&&models>0,models+" troop models, "+samples+" poses of their tells every 50 ms through the carries: the cull reach has "+String.format(Locale.ROOT,"%.2f",-worst)+" blocks to spare at its tightest ("+at+")"+(out.isEmpty()?"":"; outside it (BAD): "+out));
        }

        // ---- the shots ----
        /**
         * Client: Stage.litStart's recovery for a platform that stays dark or half lit (the spectator goes 400 blocks away and, 40 ticks later, back, so the client takes the
         * platform's chunks and their light again), at most 3 times a run. litStart carries it out and brings the spectator back; the shot being taken starts again.
         */
        void recover(MinecraftClient c,String at,String why){
            awayFor=1;darkFor=0;litWait=0;litAgain=0;unlit=0;recoveries++;sendAway(c);
            System.out.println("[p2-light] tells "+at+": "+why+"; the spectator goes 400 blocks away and back (recovery "+recoveries+")");
        }
        /** Client: one shot's turn. */
        void shoot(MinecraftClient c){
            if(awayFor>0){litStart(c);if(awayFor==0){wait=0;steady=0;held=0;}return;} // away: the troops are off this client, and TroopAnimator forgets what was forced on them
            if(si>=shots.size()){TroopAnimator.forced.clear();cs=2;wait=0;return;}
            Shot s=shots.get(si);var e=client(c,units.get(s.troop()).mob);var m=TroopModels.model(s.troop());
            if(e==null||m==null){if(++held>400){bad.add(s.name()+": no client entity or model");si++;held=0;wait=0;}return;}
            wait++;
            if(wait==1){
                boolean change=!s.troop().equals(on);
                if(change){on=s.troop();server(c,b->{for(int i=0;i<ids.size();i++)place(units.get(ids.get(i)),i,ids.get(i).equals(s.troop()));});}
                TroopAnimator.forced.clear();TroopAnimator.forced.put(e.getId(),s.forced());
                cam=view(s);final double[] v=cam;server(c,b->camera(b,v[0],v[1],v[2],(float)v[3],(float)v[4]));
                settle=change?15:0;steady=0;held=0;return;
            }
            if(wait<6+settle)return;
            var d=TroopRenderer.drawn(e);boolean drawn=d!=null&&d.pose!=null&&d.last!=null&&d.count>lastCount;lastCount=d==null?0:d.count;
            boolean gilt=drawn&&"gold".equals(d.last.cloth());
            float[] want=pose(m,s.forced(),shield(s.troop()));boolean posed=drawn&&Anim.maxRot(d.pose,want)<=1&&Anim.maxPos(d.pose,want)<=0.1;
            var p=c.player;boolean there=Math.abs(p.getX()-(site.getX()+.5+cam[0]))<.05&&Math.abs(p.getEyeY()-(site.getY()+cam[1]))<.05&&Math.abs(p.getZ()-(site.getZ()+.5+cam[2]))<.05
                &&Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw()-(float)cam[3]))<1&&Math.abs(p.getPitch()-cam[4])<1;
            steady=gilt&&posed&&there?steady+1:0;held++;
            if(steady>=3||held>300){
                if(litWait>=300&&recoveries<3){recover(c,s.name(),"held 300 ticks for the platform's light");return;}
                if(!shotReady(c,s.name()))return;
                if(steady<3)bad.add(s.name()+": taken anyway after 300 ticks (drawn "+drawn+", in gold "+gilt+", pose "+posed+", camera "+there+")");
                shot(c,s.name());si++;wait=0;steady=0;held=0;
            }
        }
        /** The bones of a model forced to a clip and second, then back to rest. */
        static float[] pose(TroopModel<?> m,TroopAnimator.Forced f,boolean shield){
            m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,f,TroopAnimator.row(m),shield);
            float[] out=m.snapshot(null);m.getPart().traverse().forEach(ModelPart::resetTransform);return out;
        }
        /** The camera of a shot: {dx, eye, dz, yaw, pitch}, from the platform's centre, DIST blocks from the troop at the shot's angle round it. */
        double[] view(Shot s){
            double y=Math.toRadians(YAW),fx=-Math.sin(y),fz=Math.cos(y),rx=-Math.cos(y),rz=-Math.sin(y);
            double a=Math.toRadians(s.angle()),ux=Math.cos(a)*fx+Math.sin(a)*rx,uz=Math.cos(a)*fz+Math.sin(a)*rz;
            return new double[]{DIST*ux,EYE,DIST*uz,Turn.yawOf(-ux,-uz),0};
        }
    }

    /**
     * Task 5d: troop depth (suite depth). The Task 5 art platform at noon, gold side: the relief meshes and weapon frames load (-load) and
     * every quad of them is sound (-uv); a knight draws relief near and flat far (-near: 8 and 30 blocks, 60 knights where exactly the 48
     * nearest do, a line of eight across 24 blocks, the cutoff on synthetic arrays), a card portrait always does (-portrait), each stage
     * draws its own key (-stage), the player's Troop depth option Off flattens every troop and On restores the near rule (-switch), the relief follows the arms and head through a clip (-follow), the bow's frames follow bow_draw and
     * its nock meets the drawing hand (-bow), the staff's glow draws (-glow) and the frame costs what the design allows (-cost, the
     * Task 5 tick-cost fixture at two cameras). The screenshots are depth-*.png; with Iris and shaders on, the same views are saved as
     * depth-shader-*.png.
     */
    static final class Depth extends Stage {
        static final List<String> SIX=GildedArt.SIX;
        static final double STUDIO=12,PARK=60,LINE_Z=10;
        static final int[][] NORMALS={{0,-1,0},{0,1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
        final Map<String,GravesAndGlory.Unit> golds=new LinkedHashMap<>();
        final List<GravesAndGlory.Unit> crowd=new java.util.concurrent.CopyOnWriteArrayList<>(),archers=new java.util.concurrent.CopyOnWriteArrayList<>(),stagers=new java.util.concurrent.CopyOnWriteArrayList<>(),lodders=new java.util.concurrent.CopyOnWriteArrayList<>();
        boolean shaders;int mark,wi;final StringBuilder nearWhy=new StringBuilder();boolean nearOk=true;
        /** p2-depth-switch: what the crowd and the portrait showed with the player's "Troop depth" option Off, and whether it was right. */
        final StringBuilder switchWhy=new StringBuilder();boolean switchOk=true;
        /** The lineup's relief keys (six at 6 blocks), the 8 and 30 block samples, the crowd, the line of eight. */
        String lineupKeys="";
        static final double[] LOD_AT={10,14,18,21,23,25,28,34};
        String name(){return "depth";}
        void cast(GravesAndGlory.Battle b){
            for(int i=0;i<6;i++)golds.put(SIX.get(i),post(b,gold,SIX.get(i),-6.25+i*2.5,0,0));
            camera(b,0,1.6,6,180,4);
        }
        /** Whether Iris has a shader pack loaded (Iris is no compile dependency of the mod). */
        static boolean shadersOn(){
            try{Class<?> api=Class.forName("net.irisshaders.iris.api.v0.IrisApi");Object inst=api.getMethod("getInstance").invoke(null);return (Boolean)api.getMethod("isShaderPackInUse").invoke(inst);}
            catch(ReflectiveOperationException|LinkageError e){return false;}
        }
        /** The screenshot name of a view: depth-<view>, or depth-shader-<view> with shaders on (knight-near is just near there). */
        String snap(String view){return shaders?"depth-shader-"+(view.equals("knight-near")?"near":view):"depth-"+view;}
        /** Client: the camera is where camera(b,...) put it. */
        boolean there(MinecraftClient c,double dx,double eye,double dz,float yaw,float pitch){
            var p=c.player;
            return p!=null&&Math.abs(p.getX()-(site.getX()+.5+dx))<.05&&Math.abs(p.getEyeY()-(site.getY()+eye))<.05&&Math.abs(p.getZ()-(site.getZ()+.5+dz))<.05
                &&Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw()-yaw))<1&&Math.abs(p.getPitch()-pitch)<1;
        }
        /** Client: a posted troop's client entity stands at (dx, dz) from the platform's centre. */
        boolean stands(MinecraftClient c,GravesAndGlory.Unit u,double dx,double dz){
            var e=client(c,u.mob);return e!=null&&Math.abs(e.getX()-(site.getX()+.5+dx))<.3&&Math.abs(e.getZ()-(site.getZ()+.5+dz))<.3&&Math.abs(e.getY()-site.getY())<.5;
        }
        /** The relief quads of the bones a draw showed, for its key: what reliefQuads must count for that draw. */
        static int drawnQuads(TroopModel<?> m,TroopRenderer.Draw d){
            if(m==null||d==null||d.relief()==null)return 0;int n=0;for(String bone:m.shownBones(d.bones()))n+=m.relief(bone,d.relief()).quads();return n;
        }
        /** The quads the weapon's frame holds (frame 0 unless the draw names another), or 0. */
        static int weaponFrameQuads(TroopModel<?> m,TroopRenderer.Draw d){
            var w=m==null?null:TroopModels.weapon(m.weaponId());if(w==null||d==null||!d.weapon())return 0;return w.frames().get(Math.max(0,d.weaponFrame())).quads();
        }
        TroopRenderer.Drawn drawn(MinecraftClient c,GravesAndGlory.Unit u){var e=client(c,u.mob);return e==null?null:TroopRenderer.drawn(e);}
        int drawCount(MinecraftClient c,GravesAndGlory.Unit u){var d=drawn(c,u);return d==null?0:d.count;}
        /** Server: a posted troop high over the platform, out of every camera's 24 blocks. */
        void park(GravesAndGlory.Battle b,GravesAndGlory.Unit u,int i){
            Vec3d where=at(-8+i*3,0).add(0,PARK,0);pins.removeIf(p->p.mob()==u.mob);pins.add(new Pin(u.mob,where,0));pin(u.mob,where,0);
        }
        void discard(GravesAndGlory.Battle b,List<GravesAndGlory.Unit> list){
            for(var u:list){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();pins.removeIf(p->p.mob()==u.mob);b.units.remove(u);posted.remove(u);}list.clear();
        }

        public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
            if(finished)return;
            if(tick>14000){report("p2-depth-timeout",false,"stuck at client step "+cs+" ("+wait+"), server stage "+stage);end(c);return;}
            switch(cs){
                case 0->{boolean lit=litStart(c);
                    if(lit&&ready&&posted.size()==6&&posted.stream().allMatch(u->{var e=client(c,u.mob);return e!=null&&e.getCustomName()!=null;})){c.options.hudHidden=true;shaders=shadersOn();wait=0;cs=1;
                        System.out.println("[p2] depth: shaders "+(shaders?"on":"off"));}
                    else if(++wait%600==0)System.out.println("[p2-wait] depth step 0 after "+wait+" ticks: ready="+ready+", posted "+posted.size()+" of 6; "+litWhy(c));}
                case 1->{loadCheck(c);uvCheck(c);followCheck(c);server(c,b->camera(b,0,1.6,6,180,4));cs=2;wait=0;}
                case 2->{wait++; // the six in a row at 6 blocks
                    if(wait==50){if(there(c,0,1.6,6,180,4)&&shotReady(c,"depth-lineup")){lineupKeys=reliefKeys(c);shot(c,snap("lineup"));cs=3;wait=0;}else wait--;}}
                case 3->nearOne(c);
                case 4->nearCrowd(c);
                case 5->weaponsRow(c);
                case 6->bowStage(c);
                case 7->lodLine(c);
                case 8->stageCheck(c);
                case 9->glowCheck(c);
                case 10->cards(c);
                case 11->costClient(c);
                default->{}
            }
        }
        public void cleanup(MinecraftClient c){TroopDepth.enabled=true;Presentation.troopDepth=true;TroopAnimator.forced.clear();costOn=false;DepthCost.phase=0;super.cleanup(c);}

        /** The relief key of each of the six gold troops' last draw, in order ("knight 1, archer 1, ..."). */
        String reliefKeys(MinecraftClient c){
            StringBuilder sb=new StringBuilder();
            for(String id:SIX){var d=drawn(c,golds.get(id));var last=d==null?null:d.last;sb.append(id).append(' ').append(last==null?"not drawn":last.relief()==null?"flat":"key "+last.relief()).append(", ");}
            return sb.toString();
        }

        // ---- p2-depth-load ----
        static JsonObject modelJson(MinecraftClient c,String id)throws IOException{
            try(var in=c.getResourceManager().getResource(new Identifier("gravesandglory","models/troop/"+id+".json")).orElseThrow().getReader()){return JsonParser.parseReader(in).getAsJsonObject();}
        }
        /** The quads each relief key holds in a generated troop file, bone by bone summed. */
        static Map<String,Integer> fileQuads(JsonObject j){
            Map<String,Integer> out=new TreeMap<>();
            for(var el:j.getAsJsonArray("bones")){var b=el.getAsJsonObject();if(b.has("relief"))for(var e:b.getAsJsonObject("relief").entrySet())out.merge(e.getKey(),e.getValue().getAsJsonArray().size(),Integer::sum);}
            return out;
        }
        void loadCheck(MinecraftClient c){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String id:SIX){
                var m=TroopModels.model(id);
                if(m==null){ok=false;why.append(id).append(": no model (BAD); ");continue;}
                try{
                    var j=modelJson(c,id);Map<String,Integer> want=fileQuads(j);Set<String> skins=new TreeSet<>();for(var e:j.getAsJsonObject("skins").entrySet())skins.add(e.getKey());
                    if(j.has("veteran")&&!j.get("veteran").getAsString().isEmpty())skins.add("veteran");
                    Set<String> got=new TreeSet<>(m.reliefKeys());boolean good=got.equals(want.keySet())&&got.equals(skins)&&!got.isEmpty();
                    StringBuilder per=new StringBuilder();
                    for(String k:got){int q=m.reliefQuads(k);good&=q==want.getOrDefault(k,-1)&&q>0&&q<=256;per.append(k).append('=').append(q).append(' ');}
                    ok&=good;why.append(id).append(": relief keys ").append(got).append(" (skins ").append(skins).append(") quads ").append(per).append("(the file holds ").append(want).append(")").append(good?"":" (BAD)").append("; ");
                }catch(Exception e){ok=false;why.append(id).append(": ").append(e).append(" (BAD); ");}
            }
            Set<String> seen=new TreeSet<>();
            for(String id:SIX){
                var m=TroopModels.model(id);var w=m==null?null:TroopModels.weapon(m.weaponId());if(w==null||!seen.add(w.id()))continue;
                int want=w.id().equals("order_bow")||w.id().equals("longbow")?4:1;boolean good=w.frames().size()==want;StringBuilder q=new StringBuilder();
                for(var f:w.frames()){good&=f.quads()>=2&&f.quads()<=128;q.append(f.quads()).append(' ');}
                if(want==4)good&=w.pull().length==3&&w.nocks().size()==4;
                ok&=good;why.append("weapon ").append(w.id()).append(": ").append(w.frames().size()).append(" frames (want ").append(want).append(") of ").append(q).append("quads").append(good?"":" (BAD)").append("; ");
            }
            int lines=0;List<String> bad=new ArrayList<>();
            try{for(String line:Files.readAllLines(c.runDirectory.toPath().resolve("logs/latest.log"),java.nio.charset.StandardCharsets.ISO_8859_1)){lines++;if(line.contains("[troop-models]"))bad.add(line.trim());}}catch(IOException e){bad.add(e.toString());}
            ok&=bad.isEmpty()&&lines>0;why.append(lines).append(" lines of latest.log read, ").append(bad.isEmpty()?"no [troop-models] line":bad.size()+" [troop-models] lines, first: "+bad.get(0));
            report("p2-depth-load",ok,"every Gilded model loads one relief mesh set per own skin with the quad counts its generated file holds, the bows have 4 frames and the other weapons 1: "+why);
        }

        // ---- p2-depth-uv ----
        /** The faces of a bone's own cuboids, read from ModelPart.Cuboid by reflection (dev only): per cuboid a list of {nx, ny, nz, then x, y, z, u, v for each of the four corners}, px and normalised UV. */
        static List<List<float[]>> cuboidFaces(ModelPart part){
            List<List<float[]>> out=new ArrayList<>();
            try{
                var sides=ModelPart.Cuboid.class.getDeclaredField("sides");sides.setAccessible(true);
                part.forEachCuboid(new MatrixStack(),(entry,path,i,q)->{
                    if(!path.isEmpty())return;List<float[]> faces=new ArrayList<>();
                    try{for(ModelPart.Quad quad:(ModelPart.Quad[])sides.get(q)){float[] f=new float[23];f[0]=quad.direction.x;f[1]=quad.direction.y;f[2]=quad.direction.z;
                        for(int k=0;k<4;k++){var v=quad.vertices[k];f[3+5*k]=v.pos.x;f[4+5*k]=v.pos.y;f[5+5*k]=v.pos.z;f[6+5*k]=v.u;f[7+5*k]=v.v;}faces.add(f);}}
                    catch(IllegalAccessException e){throw new IllegalStateException(e);}
                    out.add(faces);});
            }catch(NoSuchFieldException e){throw new IllegalStateException(e);}
            return out;
        }
        /** The texel (u, v) the cuboid faces give at a point of the face under it, p in px: the face with the quad's normal whose plane lies at or just below p along the normal and whose area holds p. Null when none. */
        static double[] cuboidUv(List<List<float[]>> cuboids,double[] p,int[] n,int w,int h){
            int a=n[0]!=0?0:n[1]!=0?1:2,pa=(a+1)%3,qa=(a+2)%3;double best=1e9;double[] out=null;
            for(var faces:cuboids)for(float[] f:faces){
                if(Math.abs(f[0]-n[0])>.01||Math.abs(f[1]-n[1])>.01||Math.abs(f[2]-n[2])>.01)continue;
                double lo1=1e9,hi1=-1e9,lo2=1e9,hi2=-1e9;for(int k=0;k<4;k++){lo1=Math.min(lo1,f[3+5*k+pa]);hi1=Math.max(hi1,f[3+5*k+pa]);lo2=Math.min(lo2,f[3+5*k+qa]);hi2=Math.max(hi2,f[3+5*k+qa]);}
                if(p[pa]<lo1-1e-3||p[pa]>hi1+1e-3||p[qa]<lo2-1e-3||p[qa]>hi2+1e-3)continue;
                double dist=(p[a]-f[3+a])*n[a];if(dist<-1e-3||dist>=best)continue;
                double e1x=f[8+pa]-f[3+pa],e1y=f[8+qa]-f[3+qa],e3x=f[18+pa]-f[3+pa],e3y=f[18+qa]-f[3+qa],det=e1x*e3y-e1y*e3x;if(Math.abs(det)<1e-9)continue;
                double px=p[pa]-f[3+pa],py=p[qa]-f[3+qa],s=(px*e3y-py*e3x)/det,t=(e1x*py-e1y*px)/det;
                double u=f[6]+s*(f[11]-f[6])+t*(f[21]-f[6]),v=f[7]+s*(f[12]-f[7])+t*(f[22]-f[7]);
                best=dist;out=new double[]{u*w,v*h};
            }
            return out;
        }
        /** One mesh's quads against the three rules (unit axis normal agreeing with the corner order, a UV area above zero, a top quad's UV centre on its cuboid face). Returns the failures; counts are added to n = {quads, top quads}. */
        static void quadRules(String what,TroopMesh mesh,List<List<float[]>> cuboids,int w,int h,List<String> bad,int[] n){
            for(int i=0;i<mesh.quads();i++){
                n[0]++;int d=mesh.direction(i);float[] pos=mesh.positions(i),tex=mesh.texels(i);int[] nm=NORMALS[d];int a=nm[0]!=0?0:nm[1]!=0?1:2;
                boolean flat=true;for(int k=1;k<4;k++)flat&=Math.abs(pos[3*k+a]-pos[a])<1e-4;
                double[] c1={pos[3]-pos[0],pos[4]-pos[1],pos[5]-pos[2]},c3={pos[9]-pos[0],pos[10]-pos[1],pos[11]-pos[2]};
                double[] cr={c1[1]*c3[2]-c1[2]*c3[1],c1[2]*c3[0]-c1[0]*c3[2],c1[0]*c3[1]-c1[1]*c3[0]};double len=Math.sqrt(cr[0]*cr[0]+cr[1]*cr[1]+cr[2]*cr[2]);
                boolean wound=len>1e-6&&(cr[0]*nm[0]+cr[1]*nm[1]+cr[2]*nm[2])/len>.999;
                double e1u=tex[2]-tex[0],e1v=tex[3]-tex[1],e3u=tex[6]-tex[0],e3v=tex[7]-tex[1],area=Math.abs(e1u*e3v-e1v*e3u);
                if(!flat||!wound)bad.add(what+" quad "+i+" (direction "+d+"): "+(flat?"":"not flat across its normal; ")+(wound?"":"corner order does not agree with the normal"));
                if(area<=1e-6)bad.add(what+" quad "+i+": UV area "+area);
                boolean top=Math.hypot(e1u,e1v)>=.99&&Math.hypot(e3u,e3v)>=.99;
                if(top&&cuboids!=null){
                    n[1]++;double[] p={(pos[0]+pos[3]+pos[6]+pos[9])/4,(pos[1]+pos[4]+pos[7]+pos[10])/4,(pos[2]+pos[5]+pos[8]+pos[11])/4};
                    double cu=(tex[0]+tex[2]+tex[4]+tex[6])/4,cv=(tex[1]+tex[3]+tex[5]+tex[7])/4;double[] real=cuboidUv(cuboids,p,nm,w,h);
                    if(real==null)bad.add(what+" top quad "+i+": no cuboid face under it");
                    else if(Math.abs(real[0]-cu)>.001||Math.abs(real[1]-cv)>.001)bad.add(String.format(Locale.ROOT,"%s top quad %d: UV centre (%.3f, %.3f), the cuboid face gives (%.3f, %.3f)",what,i,cu,cv,real[0],real[1]));
                }
                if(bad.size()>8)return;
            }
        }
        void uvCheck(MinecraftClient c){
            List<String> bad=new ArrayList<>();int[] n={0,0};int meshes=0;
            for(String id:SIX){
                var m=TroopModels.model(id);if(m==null){bad.add(id+": no model");continue;}
                int w,h;try{var size=modelJson(c,id).getAsJsonArray("size");w=size.get(0).getAsInt();h=size.get(1).getAsInt();}catch(Exception e){bad.add(id+": "+e);continue;}
                for(String key:m.reliefKeys())for(String bone:m.boneOrder()){
                    var mesh=m.relief(bone,key);if(mesh.quads()==0)continue;meshes++;
                    quadRules(id+" "+bone+" "+key,mesh,cuboidFaces(m.bone(bone)),w,h,bad,n);
                }
            }
            int frames=0;Set<String> seen=new TreeSet<>();
            for(String id:SIX){
                var m=TroopModels.model(id);var wp=m==null?null:TroopModels.weapon(m.weaponId());if(wp==null||!seen.add(wp.id()))continue;
                for(int f=0;f<wp.frames().size();f++){frames++;quadRules("weapon "+wp.id()+" frame "+f,wp.frames().get(f),null,0,0,bad,n);}
            }
            report("p2-depth-uv",bad.isEmpty()&&n[0]>0&&n[1]>0,n[0]+" quads in "+meshes+" relief meshes of the six and "+frames+" weapon frames: every one flat on its axis normal with its corners counter-clockwise from outside and a UV area above zero; "+n[1]+" top quads' UV centres within 0.001 texel of the UV their bone's own cuboid face gives"+(bad.isEmpty()?"":"; wrong: "+bad)+(n[0]>0&&n[1]>0?"":" (no relief quads to check: BAD)"));
        }

        // ---- p2-depth-follow ----
        static final class Capture implements net.minecraft.client.render.VertexConsumer {
            final List<float[]> vertices=new ArrayList<>();
            @Override public void vertex(float x,float y,float z,float r,float g,float b,float a,float u,float v,int overlay,int light,float nx,float ny,float nz){vertices.add(new float[]{x,y,z,u,v,nx,ny,nz});}
            @Override public net.minecraft.client.render.VertexConsumer vertex(double x,double y,double z){return this;}
            @Override public net.minecraft.client.render.VertexConsumer color(int r,int g,int b,int a){return this;}
            @Override public net.minecraft.client.render.VertexConsumer texture(float u,float v){return this;}
            @Override public net.minecraft.client.render.VertexConsumer overlay(int u,int v){return this;}
            @Override public net.minecraft.client.render.VertexConsumer light(int u,int v){return this;}
            @Override public net.minecraft.client.render.VertexConsumer normal(float x,float y,float z){return this;}
            @Override public void next(){}
            @Override public void fixedColor(int r,int g,int b,int a){}
            @Override public void unfixColor(){}
        }
        /** The knight posed by a forced clip, drawn bone by bone through a capturing consumer: every relief vertex of the bone, taken back into the bone's own frame, lies within 1.5 px of its cuboids. */
        void followCheck(MinecraftClient c){
            var m=TroopModels.model("knight");var row=ArmyCards.spec("knight");List<String> bad=new ArrayList<>();StringBuilder why=new StringBuilder();int poses=0;
            if(m==null||row==null){report("p2-depth-follow",false,"no knight model");return;}
            var look=new TroopModels.Look(row,1,false,"gold");
            try{
            for(Object[] pose:new Object[][]{{"attack","attack",TroopClips.PEAKS.get("attack")},{"walk","walk",TroopClips.LENGTHS.get("walk")/4}})
                for(String bone:List.of("right_arm","left_arm","iron_cap","right_leg")){
                    m.getPart().traverse().forEach(ModelPart::resetTransform);TroopAnimator.applyForced(m,new TroopAnimator.Forced((String)pose[1],(Float)pose[2]),TroopAnimator.row(m),false);
                    m.prepare(look,false,false);
                    List<ModelPart> keep=m.path(bone);
                    // only the bone's own relief: every other bone is hidden (its mesh is skipped, its children still walked), which the relief walk honours as ModelPart.render does
                    for(String other:m.boneOrder())m.bone(other).hidden=!other.equals(bone);
                    Capture cap=new Capture();MatrixStack ms=new MatrixStack();
                    int quads=m.renderRelief("1",ms,cap,0xF000F0,net.minecraft.client.render.OverlayTexture.DEFAULT_UV,1f,1f,1f,1f);
                    MatrixStack mb=new MatrixStack();for(ModelPart p:keep)p.rotate(mb);Matrix4f inv=new Matrix4f(mb.peek().getPositionMatrix()).invert();
                    var cuboids=cuboidFaces(m.bone(bone));double worst=0;
                    for(float[] v:cap.vertices){
                        Vector4f local=new Vector4f(v[0],v[1],v[2],1f);inv.transform(local);double[] q={local.x*16,local.y*16,local.z*16};double near=1e9;
                        for(var faces:cuboids){double[] lo={1e9,1e9,1e9},hi={-1e9,-1e9,-1e9};
                            for(float[] f:faces)for(int k=0;k<4;k++)for(int x=0;x<3;x++){lo[x]=Math.min(lo[x],f[3+5*k+x]);hi[x]=Math.max(hi[x],f[3+5*k+x]);}
                            double d2=0;for(int x=0;x<3;x++){double d=Math.max(0,Math.max(lo[x]-q[x],q[x]-hi[x]));d2+=d*d;}near=Math.min(near,Math.sqrt(d2));}
                        worst=Math.max(worst,near);
                    }
                    poses++;why.append(String.format(Locale.ROOT,"%s %s: %d quads, %d vertices, the farthest %.2f px; ",pose[0],bone,quads,cap.vertices.size(),worst));
                    if(cap.vertices.isEmpty())bad.add(pose[0]+" "+bone+": no relief vertex drawn");
                    else if(worst>1.5)bad.add(String.format(Locale.ROOT,"%s %s: a relief vertex %.2f px from its bone's cuboids",pose[0],bone,worst));
                }
            }finally{m.getPart().traverse().forEach(p->{p.resetTransform();p.hidden=false;});}   // the shared knight model is left as found, whatever threw
            report("p2-depth-follow",bad.isEmpty()&&poses==8,"the knight forced to attack at its peak and to walk at a quarter, each relief vertex of right_arm, left_arm, iron_cap (the head's child) and right_leg (drawn through a capturing consumer, taken back into the bone's frame) within 1.5 px of the bone's cuboids: "+why+(bad.isEmpty()?"":"wrong: "+bad));
        }

        // ---- p2-depth-near: a knight at 8 and 30 blocks ----
        String near8="",near30="";boolean nearA=true;
        void nearOne(MinecraftClient c){
            wait++;var k=golds.get("knight");var m=TroopModels.model("knight");
            if(wait==1)server(c,b->{int i=0;for(var e:golds.entrySet())if(!e.getKey().equals("knight"))park(b,e.getValue(),i++);move(k.mob,0,STUDIO,180);camera(b,0,1.6,STUDIO-8,0,0);});
            if(wait==55)mark=drawCount(c,k);
            if(wait==60){
                if(!there(c,0,1.6,STUDIO-8,0,0)||!stands(c,k,0,STUDIO)){wait--;return;}
                var d=drawn(c,k);var last=d==null?null:d.last;boolean fresh=d!=null&&d.count>mark;
                int want=drawnQuads(m,last),wq=weaponFrameQuads(m,last);
                boolean ok=fresh&&last.relief()!=null&&last.relief().equals(m==null?"":m.reliefKey(last.look()))&&TroopDepth.nearDraws==1&&want>0&&TroopDepth.reliefQuads==want&&wq>2&&TroopDepth.weaponQuads==wq;
                nearA&=ok;near8=String.format(Locale.ROOT,"at 8 blocks relief %s, nearDraws %d, reliefQuads %d (the bones drawn hold %d of its stage's %d), weaponQuads %d (its frame holds %d)%s; ",last==null?"-":last.relief(),TroopDepth.nearDraws,TroopDepth.reliefQuads,want,last==null||m==null||last.relief()==null?-1:m.reliefQuads(last.relief()),TroopDepth.weaponQuads,wq,ok?"":" (BAD)");
            }
            if(wait==61)server(c,b->camera(b,0,1.6,STUDIO-30,0,0));
            if(wait==115)mark=drawCount(c,k);
            if(wait==120){
                if(!there(c,0,1.6,STUDIO-30,0,0)){wait--;return;}
                var d=drawn(c,k);var last=d==null?null:d.last;boolean fresh=d!=null&&d.count>mark;
                boolean ok=fresh&&last.relief()==null&&TroopDepth.nearDraws==0&&TroopDepth.reliefQuads==0;
                nearA&=ok;near30=String.format(Locale.ROOT,"at 30 blocks relief %s, nearDraws %d, reliefQuads %d%s; ",last==null?"-":last.relief(),TroopDepth.nearDraws,TroopDepth.reliefQuads,ok?"":" (BAD)");
            }
            if(wait==121)server(c,b->{move(k.mob,0,STUDIO,180+30);camera(b,0,1.45,STUDIO-4,0,6);});
            if(wait==180){
                if(there(c,0,1.45,STUDIO-4,0,6)&&stands(c,k,0,STUDIO)&&shotReady(c,"depth-knight-near")){shot(c,snap("knight-near"));TroopDepth.enabled=false;mark=drawCount(c,k);}else wait--;
            }
            if(wait==200){
                var d=drawn(c,k);boolean flat=d!=null&&d.count>mark&&d.last.relief()==null&&!TroopDepth.enabled&&TroopDepth.nearDraws==0&&TroopDepth.reliefQuads==0&&TroopDepth.weaponQuads==2;
                nearA&=flat;
                nearWhy.append(String.format(Locale.ROOT,"with TroopDepth.enabled false the knight drew relief %s, nearDraws %d, reliefQuads %d, weaponQuads %d (want none, 0, 0 and the sword's 2 big faces)%s; ",d==null?"-":d.last.relief(),TroopDepth.nearDraws,TroopDepth.reliefQuads,TroopDepth.weaponQuads,flat?"":" (BAD)"));
                if(shotReady(c,"depth-knight-flat")){shot(c,snap("knight-flat"));TroopDepth.enabled=true;cs=4;wait=0;}else wait--;
            }
        }
        // ---- p2-depth-near: sixty knights ----
        void nearCrowd(MinecraftClient c){
            wait++;var m=TroopModels.model("knight");
            if(wait==1)server(c,b->{
                park(b,golds.get("knight"),0);
                for(int i=0;i<60;i++){double r=3+i*.3,ang=Math.toRadians(((i*.381966)%1-.5)*70);crowd.add(post(b,gold,"knight",r*Math.sin(ang),-15+r*Math.cos(ang),180));}
                camera(b,0,1.6,-15,0,0);});
            if(wait==80&&(crowd.size()!=60||!there(c,0,1.6,-15,0,0)||!crowd.stream().allMatch(u->client(c,u.mob)!=null&&drawn(c,u)!=null)))wait--;
            if(wait==81){for(var u:crowd)marks.put(u,drawCount(c,u));}
            if(wait==100){
                int raised=0,expected=0;List<String> bad=new ArrayList<>();
                for(int i=0;i<crowd.size();i++){var d=drawn(c,crowd.get(i));var last=d==null?null:d.last;boolean fresh=d!=null&&d.count>marks.getOrDefault(crowd.get(i),0);
                    boolean up=fresh&&last.relief()!=null;if(up)raised++;boolean want=i<TroopDepth.NEAR_CAP;if(want)expected++;
                    if(!fresh)bad.add("knight "+i+" not drawn");else if(up!=want&&bad.size()<6)bad.add("knight "+i+" (nearest "+(i+1)+") relief "+last.relief()+", want "+(want?"raised":"flat"));}
                var any=drawn(c,crowd.get(0));int each=drawnQuads(m,any==null?null:any.last);
                boolean ok=bad.isEmpty()&&raised==TroopDepth.NEAR_CAP&&TroopDepth.nearDraws==TroopDepth.NEAR_CAP&&each>0&&TroopDepth.reliefQuads==(long)TroopDepth.NEAR_CAP*each;
                nearOk&=ok;nearWhy.append(String.format(Locale.ROOT,"60 knights within 24 blocks: %d draw relief (want exactly %d, the nearest), nearDraws %d, reliefQuads %d (%d each)%s; ",raised,TroopDepth.NEAR_CAP,TroopDepth.nearDraws,TroopDepth.reliefQuads,each,bad.isEmpty()?"":" (BAD: "+bad+")"));
                Presentation.troopDepth=false;   // the player's option Off: the same sixty, now none of them raised
            }
            if(wait==101)for(var u:crowd)marks.put(u,drawCount(c,u));
            if(wait==120){
                int fresh=0,raised=0,near=0;var any=drawn(c,crowd.get(0));int wq=weaponFrameQuads(m,any==null?null:any.last);
                for(var u:crowd){var d=drawn(c,u);if(d!=null&&d.count>marks.getOrDefault(u,0)){fresh++;if(d.last.relief()!=null)raised++;}var e=client(c,u.mob);if(e!=null&&TroopDepth.near(e))near++;}
                boolean ok=fresh==60&&raised==0&&near==0&&TroopDepth.nearDraws==0&&TroopDepth.reliefQuads==0&&wq>2&&TroopDepth.weaponQuads==(long)60*wq;
                switchOk&=ok;switchWhy.append(String.format(Locale.ROOT,"with the option Off the same %d knights within 24 blocks: %d drawn fresh, %d with relief, %d that TroopDepth.near calls near, nearDraws %d, reliefQuads %d, weaponQuads %d (want 0, 0, 0, 0 and 60 sprite swords of %d quads = %d)%s; ",crowd.size(),fresh,raised,near,TroopDepth.nearDraws,TroopDepth.reliefQuads,TroopDepth.weaponQuads,wq,60L*wq,ok?"":" (BAD)"));
                Presentation.troopDepth=true;
            }
            if(wait==122)for(var u:crowd)marks.put(u,drawCount(c,u));
            if(wait==138){
                int fresh=0,raised=0;var any=drawn(c,crowd.get(0));int each=drawnQuads(m,any==null?null:any.last);
                for(var u:crowd){var d=drawn(c,u);if(d!=null&&d.count>marks.getOrDefault(u,0)){fresh++;if(d.last.relief()!=null)raised++;}}
                boolean ok=fresh==60&&raised==TroopDepth.NEAR_CAP&&TroopDepth.nearDraws==TroopDepth.NEAR_CAP&&each>0&&TroopDepth.reliefQuads==(long)TroopDepth.NEAR_CAP*each;
                switchOk&=ok;switchWhy.append(String.format(Locale.ROOT,"with the option On again %d of %d draw relief (want exactly the %d nearest), nearDraws %d, reliefQuads %d%s; ",raised,fresh,TroopDepth.NEAR_CAP,TroopDepth.nearDraws,TroopDepth.reliefQuads,ok?"":" (BAD)"));
                server(c,b->discard(b,crowd));
            }
            if(wait==140){
                List<String> bad=new ArrayList<>();
                double[] d={9,1,4,16,25,36};
                double x1=TroopDepth.cutoffSq(d,6,3,30),x2=TroopDepth.cutoffSq(d,6,6,30),x3=TroopDepth.cutoffSq(new double[]{5,6,7,1,1},3,2,100),x4=TroopDepth.cutoffSq(new double[]{},0,48,576),x5=TroopDepth.cutoffSq(new double[]{100,200,300},3,1,50);
                if(x1!=9)bad.add("3rd of {9,1,4,16,25,36} below 30 is "+x1+", want 9");if(x2!=30)bad.add("6th with five below 30 is "+x2+", want 30");
                if(x3!=6)bad.add("2nd of the first 3 of {5,6,7,1,1} is "+x3+", want 6");if(x4!=576)bad.add("no entities: "+x4+", want 576");if(x5!=50)bad.add("none below: "+x5+", want 50");
                nearOk&=bad.isEmpty();nearWhy.append("cutoffSq on synthetic arrays ").append(bad.isEmpty()?"as the design says":"wrong: "+bad).append("; ");
                server(c,b->{for(var e:golds.entrySet())move(e.getValue().mob,-6.25+SIX.indexOf(e.getKey())*2.5,LINE_Z,180);camera(b,0,1.6,LINE_Z-6,0,4);});
                cs=5;wait=0;
            }
        }
        final Map<GravesAndGlory.Unit,Integer> marks=new java.util.concurrent.ConcurrentHashMap<>();

        // ---- the six weapons at 0, 45 and 90 degrees ----
        void weaponsRow(MinecraftClient c){
            wait++;int[] angles={0,45,90};int a=angles[wi];
            if(wait==1)server(c,b->{
                for(var e:golds.entrySet()){var model=TroopModels.model(e.getKey());boolean left=model!=null&&"left".equals(model.hand());
                    move(e.getValue().mob,-3.25+SIX.indexOf(e.getKey())*1.3,LINE_Z,180+(left?a:-a));}
                camera(b,0,1.5,LINE_Z-3.4,0,3);});
            if(wait==60){
                boolean seen=true;for(var u:golds.values())seen&=drawn(c,u)!=null&&drawn(c,u).last!=null&&drawn(c,u).last.weapon();
                if(there(c,0,1.5,LINE_Z-3.4,0,3)&&seen&&shotReady(c,"depth-weapons-"+a)){shot(c,snap(a==0?"weapons":"weapons-"+a));wi++;wait=0;if(wi==3){cs=6;wi=0;}}else wait--;
            }
        }

        // ---- p2-depth-bow ----
        void bowStage(MinecraftClient c){
            wait++;float[] times={0f,.3f,.65f,.8f};int[] want={0,1,3,0};
            if(wait==1)server(c,b->{
                int i=0;for(var e:golds.entrySet())park(b,e.getValue(),i++);
                for(int k=0;k<4;k++)archers.add(post(b,gold,"archer",-2.4+k*1.6,STUDIO,180+45));
                camera(b,0,1.4,STUDIO-3.5,0,6);});
            if(wait>=3&&wait<60&&archers.size()==4)for(int k=0;k<4;k++){var e=client(c,archers.get(k).mob);if(e!=null&&!TroopAnimator.forced.containsKey(e.getId()))TroopAnimator.forced.put(e.getId(),new TroopAnimator.Forced("bow_draw",times[k]));}
            if(wait==60&&(archers.size()!=4||!there(c,0,1.4,STUDIO-3.5,0,6)||!archers.stream().allMatch(u->drawn(c,u)!=null)))wait--;
            if(wait==61)for(var u:archers)marks.put(u,drawCount(c,u));
            if(wait==70){
                List<String> bad=new ArrayList<>();StringBuilder why=new StringBuilder();
                float[] pull={.15f,.32f,.48f};int[] table={0,0,1,2,3,3,0,0};float[] at={-1,.1f,.15f,.4f,.5f,.65f,.8f,1f};
                for(int i=0;i<at.length;i++){int got=TroopModels.Weapon.frameAt(pull,at[i],.8f);if(got!=table[i])bad.add("frameAt("+at[i]+") = "+got+", want "+table[i]);}
                why.append("frameAt on [0.15, 0.32, 0.48] with the loose at 0.8 gives 0, 0, 1, 2, 3, 3, 0, 0 for -1, 0.1, 0.15, 0.4, 0.5, 0.65, 0.8, 1.0").append(bad.isEmpty()?"":" (BAD "+bad+")").append("; ");
                for(int k=0;k<4;k++){var d=drawn(c,archers.get(k));var last=d==null?null:d.last;boolean fresh=d!=null&&d.count>marks.getOrDefault(archers.get(k),0);
                    int got=fresh&&last.weapon()?last.weaponFrame():-1;if(got!=want[k])bad.add(String.format(Locale.ROOT,"the archer forced to bow_draw at %.2f s drew frame %d, want %d",times[k],got,want[k]));
                    why.append(String.format(Locale.ROOT,"the archer at bow_draw %.2f s drew frame %d (want %d); ",times[k],got,want[k]));}
                for(String id:List.of("archer","longbow")){
                    var m=TroopModels.model(id);var w=m==null?null:TroopModels.weapon(m.weaponId());if(w==null||w.frames().size()<4){bad.add(id+": no bow with pull frames");continue;}
                    float full=TroopClips.BOW_TIMES.get(m.rig)[0];var draw=Anim.bowPose(m,"bow_draw",full);var rest=Anim.bowPose(m,"idle",0);
                    double gap=draw.nock()==null?999:dist(draw.nock(),draw.other());
                    double plane=Math.toDegrees(Math.atan2(Math.abs(rest.normal()[0]),Math.abs(rest.normal()[2])));
                    if(draw.frame()!=3)bad.add(id+": the frame at "+full+" s is "+draw.frame()+", want 3");
                    if(gap>2)bad.add(String.format(Locale.ROOT,"%s: at the full draw the drawing hand is %.2f px from frame 3's nock (want 2 or less)",id,gap));
                    if(plane<40||plane>50)bad.add(String.format(Locale.ROOT,"%s: at rest the sprite plane's normal is %.1f degrees from the facing (want 45, within 5)",id,plane));
                    why.append(String.format(Locale.ROOT,"%s: at %.2f s frame %d, the right hand %.2f px from its nock (want 2 or less), at rest the plane's normal %.1f degrees from the facing (want 45 +- 5); ",id,full,draw.frame(),gap,plane));
                }
                report("p2-depth-bow",bad.isEmpty(),why.toString().trim()+(bad.isEmpty()?"":" wrong: "+bad));
                if(shotReady(c,"depth-bow-draw"))shot(c,snap("bow-draw"));
                TroopAnimator.forced.clear();server(c,b->discard(b,archers));
                cs=7;wait=0;
            }
        }
        static double dist(double[] a,double[] b){return Math.sqrt((a[0]-b[0])*(a[0]-b[0])+(a[1]-b[1])*(a[1]-b[1])+(a[2]-b[2])*(a[2]-b[2]));}

        // ---- the line of eight across the cutoff ----
        void lodLine(MinecraftClient c){
            wait++;
            if(wait==1)server(c,b->{
                for(int i=0;i<LOD_AT.length;i++){double l=(i-3.5)*1.4,d=LOD_AT[i],f=Math.sqrt(.5);lodders.add(post(b,gold,"knight",-15+f*d+f*l,-15+f*d-f*l,135));}
                camera(b,-15,1.6,-15,-45,3);});
            if(wait==60&&(lodders.size()!=LOD_AT.length||!there(c,-15,1.6,-15,-45,3)||!lodders.stream().allMatch(u->drawn(c,u)!=null)))wait--;
            if(wait==61)for(var u:lodders)marks.put(u,drawCount(c,u));
            if(wait==80){
                StringBuilder sb=new StringBuilder();List<String> bad=new ArrayList<>();
                for(int i=0;i<lodders.size();i++){var d=drawn(c,lodders.get(i));var last=d==null?null:d.last;boolean fresh=d!=null&&d.count>marks.getOrDefault(lodders.get(i),0);
                    boolean up=fresh&&last.relief()!=null;boolean want=LOD_AT[i]<23.95;sb.append((int)LOD_AT[i]).append(up?" raised, ":fresh?" flat, ":" not drawn, ");
                    if(!fresh)bad.add("the knight at "+LOD_AT[i]+" blocks was not drawn");else if(up!=want)bad.add("the knight at "+LOD_AT[i]+" blocks drew relief "+last.relief()+", want "+(want?"raised":"flat"));}
                nearOk&=bad.isEmpty();nearWhy.append("a line of knights at ").append(sb).append("the cutoff 24 blocks").append(bad.isEmpty()?"":" (BAD: "+bad+")").append("; ");
                if(shotReady(c,"depth-lod"))shot(c,snap("lod"));
                server(c,b->discard(b,lodders));
                report("p2-depth-near",nearA&&nearOk,"lineup ("+lineupKeys.trim()+"); "+near8+near30+nearWhy.toString().trim());
                cs=8;wait=0;
            }
        }

        // ---- p2-depth-stage ----
        void stageCheck(MinecraftClient c){
            wait++;
            if(wait==1)server(c,b->{for(int s=1;s<=3;s++){var u=post(b,gold,"knight",(2-s)*2.0,STUDIO,180+20);stage(u,s);stagers.add(u);}camera(b,0,1.6,STUDIO-5,0,8);});
            if(wait==60&&(stagers.size()!=3||!there(c,0,1.6,STUDIO-5,0,8)||!stagers.stream().allMatch(u->drawn(c,u)!=null&&drawn(c,u).last.look().stage()==u.stage)))wait--;
            if(wait==61)for(var u:stagers)marks.put(u,drawCount(c,u));
            if(wait==80){
                var m=TroopModels.model("knight");List<String> bad=new ArrayList<>();StringBuilder why=new StringBuilder();
                for(var u:stagers){var d=drawn(c,u);var last=d==null?null:d.last;boolean fresh=d!=null&&d.count>marks.getOrDefault(u,0);
                    String key=String.valueOf(u.stage);var skin=last==null?null:last.skin();String file=skin==null?"-":skin.getPath().substring(skin.getPath().lastIndexOf('/')+1);
                    boolean good=fresh&&key.equals(last.relief())&&file.equals("knight_s"+u.stage+".png")&&m!=null&&key.equals(m.reliefKey(last.look()));
                    if(!good)bad.add("stage "+u.stage+" drew relief "+(last==null?"-":last.relief())+" with skin "+file);why.append("stage ").append(u.stage).append(": skin ").append(file).append(", relief ").append(last==null?"-":last.relief()).append("; ");}
                report("p2-depth-stage",bad.isEmpty(),"the knight at stages 1, 2 and 3 draws the relief key of the skin it binds: "+why.toString().trim()+(bad.isEmpty()?"":" wrong: "+bad));
                server(c,b->discard(b,stagers));
                cs=9;wait=0;
            }
        }

        // ---- p2-depth-glow ----
        long glowBefore;
        static int lit(MinecraftClient c,Identifier id){
            try(var in=c.getResourceManager().getResource(id).orElseThrow().getInputStream();var img=NativeImage.read(in)){int n=0;for(int y=0;y<img.getHeight();y++)for(int x=0;x<img.getWidth();x++)if((img.getColor(x,y)>>>24)!=0)n++;return n;}
            catch(Exception e){return -1;}
        }
        void glowCheck(MinecraftClient c){
            wait++;var mage=golds.get("mage");
            if(wait==1)server(c,b->{move(mage.mob,0,STUDIO,180+20);camera(b,0,1.6,STUDIO-4,0,6);});
            if(wait==40&&(!there(c,0,1.6,STUDIO-4,0,6)||!stands(c,mage,0,STUDIO)||drawn(c,mage)==null))wait--;
            if(wait==41)glowBefore=TroopFeatures.weaponGlowDraws;
            if(wait==60){
                var m=TroopModels.model("mage");var w=m==null?null:TroopModels.weapon(m.weaponId());var e=client(c,mage.mob);var l=e==null?null:TroopModels.resolve(e);var skinGlow=l==null?null:TroopModels.glow(l);
                int troopLit=skinGlow==null?0:lit(c,skinGlow),weaponLit=w==null||w.glow()==null?0:lit(c,w.glow());long passes=TroopFeatures.weaponGlowDraws-glowBefore;
                boolean ok=w!=null&&w.glow()!=null&&passes>0&&troopLit>=0&&weaponLit>0&&troopLit+weaponLit<=16;
                report("p2-depth-glow",ok,"the Sunweaver's staff "+(w==null?"-":w.id())+" draws its glow pass ("+passes+" passes in 19 ticks, want 1 or more) and its glow ("+troopLit+" lit pixels) with the staff's ("+weaponLit+") light "+(troopLit+weaponLit)+" pixels (want 16 or fewer, the Epic cap)");
                cs=10;wait=0;
            }
        }

        // ---- p2-depth-portrait and the cards ----
        int cardMark,cardOff;
        void cards(MinecraftClient c){
            wait++;
            if(wait==1)server(c,b->{discard(b,new ArrayList<>(posted));pins.clear();b.campaign.returnLobby();var pr=b.campaign.profile(player);for(String id:SIX)pr.owned.putIfAbsent(id,new Profile.Owned());});
            if(wait==40){CampaignClient.revealUntil=0;CampaignClient.notice="";c.setScreen(new CampScreen("recruit"));}
            if(wait==45&&c.currentScreen instanceof CampScreen s){s.pickHouse("gilded");var e=ArmyCards.portrait(ArmyCards.spec("knight"));var d=e==null?null:TroopRenderer.drawn(e);cardMark=d==null?0:d.count;}
            if(wait==80){
                var e=ArmyCards.portrait(ArmyCards.spec("knight"));var d=e==null?null:TroopRenderer.drawn(e);var last=d==null?null:d.last;var m=TroopModels.model("knight");
                double nearest=1e9;int troopsSeen=0;
                if(c.world!=null&&c.player!=null)for(Entity x:c.world.getEntities())if(x instanceof MobEntity&&x!=e&&TroopModels.resolve(x)!=null&&!TroopModels.dressed(x)){troopsSeen++;nearest=Math.min(nearest,x.distanceTo(c.player));}
                boolean ok=c.currentScreen instanceof CampScreen&&d!=null&&d.count-cardMark>=5&&last.look().side()==null&&last.relief()!=null&&m!=null&&last.relief().equals(m.reliefKey(last.look()))&&nearest>=100&&e!=null&&TroopDepth.near(e);
                report("p2-depth-portrait",ok,String.format(Locale.ROOT,"the recruit page's Gilded tab: the knight's card portrait drew %d times in 35 ticks, the last with relief %s, no side, in cloth %s, TroopDepth.near(portrait) %s; the portrait mob is %s blocks from the player; the nearest of %d other troops on this client is %s blocks from the player (want 100 or more)",
                    d==null?0:d.count-cardMark,last==null?"-":last.relief(),last==null?"-":last.cloth(),e!=null&&TroopDepth.near(e),e==null||c.player==null?"-":String.format(Locale.ROOT,"%.0f",(double)e.distanceTo(c.player)),troopsSeen,nearest>1e8?"-":String.format(Locale.ROOT,"%.0f",nearest)));
                if(c.currentScreen instanceof CampScreen)shot(c,snap("cards"));
                c.setScreen(new DepthCards(SIX.stream().map(id->ArmyCards.portrait(ArmyCards.spec(id))).toList()));
                cardOff=d==null?0:d.count;Presentation.troopDepth=false;   // a portrait is always near, so the option must flatten it too
            }
            if(wait==92){
                var e=ArmyCards.portrait(ArmyCards.spec("knight"));var d=e==null?null:TroopRenderer.drawn(e);var last=d==null?null:d.last;
                boolean ok=e!=null&&d!=null&&d.count-cardOff>=3&&last.relief()==null&&!TroopDepth.near(e)&&TroopDepth.nearDraws==0&&TroopDepth.reliefQuads==0;
                switchOk&=ok;switchWhy.append(String.format(Locale.ROOT,"with the option Off the knight's card portrait (always near) drew %d times with relief %s, TroopDepth.near %s, nearDraws %d%s; ",d==null?0:d.count-cardOff,last==null?"-":last.relief(),e!=null&&TroopDepth.near(e),TroopDepth.nearDraws,ok?"":" (BAD)"));
                Presentation.troopDepth=true;
            }
            if(wait==100){shot(c,snap("cards-sizes"));c.setScreen(new PresentationScreen());}
            if(wait==104){   // the Visuals screen: its Troop depth button, pressed twice
                shot(c,snap("switch"));var btn=depthButton(c);before=btn==null?"-":btn.getMessage().getString();boolean was=Presentation.troopDepth;
                if(btn!=null)btn.onPress();
                pressed1=!Presentation.troopDepth&&was;
            }
            if(wait==108){
                var btn=depthButton(c);after1=btn==null?"-":btn.getMessage().getString();filed1=filed();
                if(btn!=null)btn.onPress();
            }
            if(wait==112){
                var btn=depthButton(c);after2=btn==null?"-":btn.getMessage().getString();String filed2=filed();boolean back=Presentation.troopDepth;
                String trip=persistence();
                boolean ok=pressed1&&before.equals("Troop depth: ON")&&after1.equals("Troop depth: OFF")&&filed1.equals("false")&&after2.equals("Troop depth: ON")&&filed2.equals("true")&&back&&trip.isEmpty();
                switchOk&=ok;switchWhy.append(String.format(Locale.ROOT,"the Visuals screen's button reads \"%s\", pressed it reads \"%s\" with presentation.json holding troopDepth %s, pressed again \"%s\" holding %s%s%s; ",before,after1,filed1,after2,filed2,trip.isEmpty()?", and a saved Off reads back Off while a file from before the option reads On":" (BAD: "+trip+")",ok?"":" (BAD)"));
                report("p2-depth-switch",switchOk,"the player's Troop depth option (Visuals screen, default On, saved with the other presentation settings): "+switchWhy.toString().trim());
                c.setScreen(null);server(c,b->{costOn=true;});cs=11;wait=0;
            }
        }
        String before="-",after1="-",after2="-",filed1="-";boolean pressed1;
        /** The Visuals screen's Troop depth button. */
        static net.minecraft.client.gui.widget.ButtonWidget depthButton(MinecraftClient c){
            if(c.currentScreen==null)return null;
            for(var ch:c.currentScreen.children())if(ch instanceof net.minecraft.client.gui.widget.ButtonWidget bw&&bw.getMessage().getString().startsWith("Troop depth: "))return bw;
            return null;
        }
        /** The troopDepth value in presentation.json as saved, or "-". */
        static String filed(){
            try{var j=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(Presentation.file())).getAsJsonObject();return j.has("troopDepth")?String.valueOf(j.get("troopDepth").getAsBoolean()):"missing";}
            catch(Exception e){return "unreadable "+e.getClass().getSimpleName();}
        }
        /** A saved Off reads back Off, and a presentation.json from before the option reads On. Returns what was wrong, or "". The file is left as it was found, with the option On. */
        static String persistence(){
            StringBuilder bad=new StringBuilder();
            try{
                Presentation.troopDepth=false;Presentation.save();Presentation.loaded=false;Presentation.troopDepth=true;Presentation.load();
                if(Presentation.troopDepth)bad.append("a saved Off read back On; ");
                java.nio.file.Files.writeString(Presentation.file(),"{\"impact\":"+Presentation.impact+",\"packsInitialized\":"+Presentation.packsInitialized+",\"armoryInitialized\":"+Presentation.armoryInitialized+"}");
                Presentation.loaded=false;Presentation.troopDepth=false;Presentation.load();
                if(!Presentation.troopDepth)bad.append("a file without the option read Off, want On (the default); ");
            }catch(Exception e){bad.append("threw ").append(e);}
            Presentation.troopDepth=true;Presentation.save();
            return bad.toString().trim();
        }
        /** The six Gilded portraits, as the cards draw them at roster size (about 36 px tall) and as the reveal frame draws them (about 160 px tall). */
        static final class DepthCards extends net.minecraft.client.gui.screen.Screen {
            final List<MobEntity> mobs;
            DepthCards(List<MobEntity> mobs){super(net.minecraft.text.Text.literal("Cards"));this.mobs=mobs;}
            @Override public void render(net.minecraft.client.gui.DrawContext g,int mx,int my,float delta){
                g.fill(0,0,width,height,0xff24222a);int n=mobs.size();
                for(int i=0;i<n;i++){var e=mobs.get(i);if(e==null)continue;int x=width*(2*i+1)/(2*n);
                    net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,x,height*3/5,ArmyCards.size(e,80),20,0,e);
                    int cx=x,cy=height-14;DarkUi.frame(g,cx-24,cy-48,48,50);net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(g,cx,cy,ArmyCards.size(e,19),20,0,e);}
            }
            @Override public boolean shouldPause(){return false;}
        }

        // ---- p2-depth-cost: the Task 5 tick-cost fixture at two cameras, depth on, flat and relief-option-off, interleaved ----
        static final int PHASE_TICKS=900,TOGGLE_TICKS=5,SETTLE_TICKS=20;
        volatile boolean costOn,costBegun,costCamera;TickCost cost;int costTicks,costPhase,costSettle;
        /** Server: the ground camera (eye 1.6 above the centre point, looking along -90 into the troops' block) or the fixture's commander camera. */
        void costCamera(GravesAndGlory.Battle b,boolean ground){
            var p=sp(b);if(p==null)return;
            var centre=b.points.stream().min(Comparator.comparingDouble(q->Math.hypot(q.spec.x(),q.spec.z()))).orElseThrow().spec;
            if(ground){b.relocate(p,centre.x(),Phase1Checks.top(b,centre.x(),centre.z())+1.6-1.62,centre.z(),-90,3);costBlockAt=new double[]{centre.x(),centre.z(),-90,4};}
            else{b.relocate(p,centre.x(),Math.max(63,Arena.ground(centre.x(),centre.z()))+15,centre.z()+24,180,30);costBlockAt=new double[]{centre.x(),centre.z(),180,-4};}
            costBlock(b);
            costCamera=true;
        }
        /** Where the troops stand for the camera being measured: x, z and yaw of the camera or the centre, and how far ahead (along the yaw) the nearest rank is. */
        volatile double[] costBlockAt;
        /**
         * Server: the troops stood in a block of ten files, 1.25 blocks apart, in as many ranks as they need, the nearest rank costBlockAt[3] blocks ahead of the point along its yaw,
         * the two teams in alternate ranks (so each stands against the other); every army on hold. Done again every 100 ticks, whatever the bot AI did with them.
         * The ground camera thus has the NEAR_CAP nearest troops in front of it (this is a staged block, not the closing melee the Task 5 fixture makes, which rings the camera and leaves only 2 to 6 of them in view); the commander camera has the block in the middle of its view and none of it within 24 blocks (the design's premise for that camera).
         */
        void costBlock(GravesAndGlory.Battle b){
            var at=costBlockAt;if(at==null)return;
            for(var s:b.squads.values())if(s.seated)s.order="hold";
            double fx=-Math.sin(Math.toRadians(at[2])),fz=Math.cos(Math.toRadians(at[2])),rx=-fz,rz=fx;
            List<GravesAndGlory.Unit>[] teams=new List[]{new ArrayList<GravesAndGlory.Unit>(),new ArrayList<GravesAndGlory.Unit>()};
            for(var u:b.units)teams[u.squad.team&1].add(u);
            int[] next={0,0};int rank=0;
            while(next[0]<teams[0].size()||next[1]<teams[1].size()){
                int t=rank%2;if(next[t]>=teams[t].size())t=1-t;
                for(int f=0;f<10&&next[t]<teams[t].size();f++){
                    var u=teams[t].get(next[t]++);double fwd=at[3]+rank*1.25,lat=(f-4.5)*1.25;double x=at[0]+fx*fwd+rx*lat,z=at[1]+fz*fwd+rz*lat;
                    u.mob.refreshPositionAndAngles(x,Phase1Checks.top(b,x,z),z,(float)at[2]+(t==0?180:0),0);u.mob.setVelocity(Vec3d.ZERO);
                }
                rank++;
            }
        }
        public void serverTick(GravesAndGlory.Battle b){
            if(costOn&&costBegun&&costBlockAt!=null&&b.time%100==0)costBlock(b);
            if(!costOn){super.serverTick(b);return;}
            var p=sp(b);if(p==null)return;
            if(cost==null)cost=tickCost(b,List.of("gilded_normal","gilded_normal","gilded_normal","gilded_normal","gilded_normal","gilded_normal","gilded_normal","gilded_normal"));
            if(!costBegun){cost.step(p);if(cost.begun){costBegun=true;RenderCost.on=false;for(var u:b.units)u.mob.setInvulnerable(true);}}
        }
        /**
         * Client: phase 1 is the ground camera, phase 2 the commander camera; in each, the state cycles every TOGGLE_TICKS ticks through 0 depth on, 1 flat (TroopDepth.enabled false:
         * no relief and the weapons' two big faces, the brief's baseline) and 2 the player option Off (no relief, the weapons' full sprite meshes), so all three see the same battle.
         * The troops are invulnerable (set when the fixture begins), so the block stays 152 strong all the way.
         */
        void costClient(MinecraftClient c){
            wait++;
            if(wait%600==0)System.out.println("[p2-wait] depth cost "+wait+" ticks: begun="+costBegun+", phase "+costPhase+", measured "+costTicks);
            if(!costBegun){if(wait>3000){report("p2-depth-cost",false,"the tick-cost fixture never began");costOn=false;cs=12;end(c);}return;}
            if(costPhase==0){costPhase=1;costCamera=false;costSettle=0;costTicks=0;DepthCost.setState(0);server(c,b->costCamera(b,true));return;}
            if(!costCamera)return;
            if(costSettle<SETTLE_TICKS){if(++costSettle==SETTLE_TICKS)DepthCost.start(costPhase);return;}
            costTicks++;
            if(costTicks==PHASE_TICKS/2+3)costWhere+=costProbe(c,costPhase==1?"ground":"commander");
            if(costTicks%TOGGLE_TICKS==0)DepthCost.setState(costTicks/TOGGLE_TICKS%3);
            if(costTicks<PHASE_TICKS)return;
            if(costPhase==1){costPhase=2;costCamera=false;costSettle=0;costTicks=0;DepthCost.phase=0;DepthCost.setState(0);server(c,b->costCamera(b,false));return;}
            DepthCost.phase=0;DepthCost.setState(0);boolean shadersNow=shadersOn();
            boolean seen=true;for(int ph=1;ph<=2;ph++)for(int s=0;s<3;s++)seen&=DepthCost.frames[ph][s]>=30;
            boolean near=DepthCost.avgNear(1)>=20&&DepthCost.avgRelief(1)>0;
            double rg=DepthCost.mean(1,0)-DepthCost.mean(1,2);
            // the ground camera's relief is the one gate (the 1.0 ms budget was derived from relief quads); the commander camera is report only
            boolean within=rg<=1.0;
            String ground=costLine("ground camera",1,1.0,true),commander=costLine("commander camera",2,0.3,false);
            String msg=String.format(Locale.ROOT,"the tick-cost fixture's %d troops (gilded_normal under all eight armies), made invulnerable and stood again every 100 ticks in a block of ten files 1.25 blocks apart, teams in alternate ranks, on hold, the nearest rank 4 blocks in front of the ground camera (so the ground camera sees about %.0f troops raised a frame: this is not the design's closing melee) and, for the commander camera, a block whose nearest rank is 25 blocks away; %d ticks a camera, the state cycling every %d ticks through depth on, flat (TroopDepth.enabled false: no relief and the weapons' two big faces) and the player option Off (no relief, full weapon meshes), so all three see the same battle. The gate is the ground camera's relief alone (want 1.0 ms or less, with %d or more raised a frame). The weapon budget is decided when Kort rules on the Blockbench weapon format (art pipeline v2 changes the weapon meshes, so gating today's sprite meshes is moot). %s; %s%s",
                cost==null?0:cost.troops,DepthCost.avgNear(1),PHASE_TICKS,TOGGLE_TICKS,20,costWhere+ground,commander,shadersNow?" (shaders on: report only)":"");
            System.out.println("[p2] depth cost REPORT ONLY, not a check: "+commander);
            report("p2-depth-cost",shadersNow?seen:(seen&&near&&within),(seen?"":"too few frames measured (BAD); ")+(near||shadersNow?"":"the ground camera drew too little relief to measure (BAD: want 20 near draws a frame on average); ")+msg);
            server(c,b->{if(cost!=null)cost.handBack();costOn=false;b.campaign.returnLobby();});
            serverDone=true;cs=12;end(c);
        }
        String costWhere="";
        /** Midway through a phase: where the troops this client holds stand against the camera (how many within 24 and 48 blocks, the nearest, the median), so a thin near count can be read. */
        static String costProbe(MinecraftClient c,String name){
            if(c.world==null||c.player==null)return "";
            var cam=c.gameRenderer.getCamera().getPos();List<Double> ds=new ArrayList<>();
            int nearTrue=0,raisedLast=0,ahead=0,aheadNear=0;TreeMap<String,int[]> tally=new TreeMap<>();var look=c.gameRenderer.getCamera().getHorizontalPlane();
            for(Entity e:c.world.getEntities())if(e instanceof MobEntity&&e.getCustomName()!=null&&TroopModels.resolve(e)!=null){
                ds.add(Math.sqrt(e.squaredDistanceTo(cam)));boolean n=TroopDepth.near(e);if(n)nearTrue++;
                var dr=TroopRenderer.drawn(e);if(dr!=null&&dr.last!=null&&dr.last.relief()!=null)raisedLast++;
                boolean front=(e.getX()-cam.x)*look.x()+(e.getZ()-cam.z)*look.z()>0;if(front){ahead++;if(n)aheadNear++;}
                if(n){var lk=TroopModels.resolve(e);var mm=lk==null?null:TroopModels.modelOf(lk.troop().id());String key=lk==null?"?":lk.troop().id()+" s"+lk.stage()+" key "+(mm==null?"-":mm.reliefKey(lk));
                    int[] t=tally.computeIfAbsent(key,k->new int[3]);t[0]++;if(dr!=null&&dr.last!=null&&dr.last.relief()!=null)t[1]++;if(front)t[2]++;}}
            Collections.sort(ds);int w24=0,w48=0;for(double d:ds){if(d<24)w24++;if(d<48)w48++;}
            return String.format(Locale.ROOT,"%s camera at (%.1f, %.1f, %.1f) yaw %.0f pitch %.0f holds %d troops: %d within 24 blocks, %d within 48, nearest %.1f, median %.1f; TroopDepth.near is true for %d of them (%d of the %d ahead of the camera), %d have a last draw with relief; near by kind (near, of them last drawn with relief, of them ahead): %s; ",name,cam.x,cam.y,cam.z,c.player.getYaw(),c.player.getPitch(),ds.size(),w24,w48,ds.isEmpty()?-1:ds.get(0),ds.isEmpty()?-1:ds.get(ds.size()/2),nearTrue,aheadNear,ahead,raisedLast,tally.entrySet().stream().map(en->en.getKey()+" "+en.getValue()[0]+"/"+en.getValue()[1]+"/"+en.getValue()[2]).collect(java.util.stream.Collectors.joining(", ")));
        }
        /**
         * One camera's part of the cost report: the relief's cost (depth on against the player option Off), marked GATED or REPORT ONLY, then, report only in both cases, the design's literal
         * depth on against flat, the weapon meshes' share of it, and the quads drawn a frame in each state.
         */
        static String costLine(String name,int ph,double want,boolean gated){
            double on=DepthCost.mean(ph,0),flat=DepthCost.mean(ph,1),opt=DepthCost.mean(ph,2);
            double qOn=DepthCost.avgRelief(ph)+DepthCost.avgWeapon(ph,0),qFlat=DepthCost.avgWeapon(ph,1),qOpt=DepthCost.avgWeapon(ph,2);
            return String.format(Locale.ROOT,"%s (%s): the relief costs %+.2f ms (depth on %.2f ms against option Off %.2f ms)%s; also report only: the design's literal depth on against flat (%.2f ms) is %+.2f ms (the design wants %.1f or less), of which the weapon meshes %+.2f ms; quads drawn a frame (relief + weapon) on %.0f, flat %.0f, option Off %.0f (%.0f relief and %.0f weapon quads on); %d, %d and %d frames (on, option Off, flat); %.1f raised troops a frame",
                name,gated?"GATED":"REPORT ONLY, neither pass nor fail",on-opt,on,opt,gated?String.format(Locale.ROOT," (want 1.0 or less)"):"",flat,on-flat,want,opt-flat,qOn,qFlat,qOpt,DepthCost.avgRelief(ph),DepthCost.avgWeapon(ph,0),DepthCost.frames[ph][0],DepthCost.frames[ph][2],DepthCost.frames[ph][1],DepthCost.avgNear(ph));
        }
    }

    /** The frame cost of suite depth: while a phase runs, the world render time of each frame (START to END), by whether troop depth was on, and the counts TroopDepth reports for it. */
    static final class DepthCost {
        /** The state the client is in: 0 depth on, 1 flat (TroopDepth.enabled false), 2 the player option Off (Presentation.troopDepth false). */
        static volatile int phase,state;static long begin;static int skip;
        static final long[][] ns=new long[3][3],near=new long[3][3],relief=new long[3][3],weapon=new long[3][3];static final long[][] frames=new long[3][3];
        static void register(){
            WorldRenderEvents.START.register(ctx->{if(phase>0)begin=System.nanoTime();});
            WorldRenderEvents.END.register(ctx->{
                if(phase<=0||begin==0)return;long t=System.nanoTime()-begin;begin=0;
                if(skip>0){skip--;return;}
                int b=state;
                frames[phase][b]++;ns[phase][b]+=t;near[phase][b]+=TroopDepth.nearDraws;relief[phase][b]+=TroopDepth.reliefQuads;weapon[phase][b]+=TroopDepth.weaponQuads;});
        }
        static void start(int p){for(int b=0;b<3;b++){frames[p][b]=ns[p][b]=near[p][b]=relief[p][b]=weapon[p][b]=0;}skip=3;phase=p;}
        /** Switch state: two frames of a switch are not counted (the counts TroopDepth reports are the frame before's). */
        static void setState(int s){state=s;TroopDepth.enabled=s!=1;Presentation.troopDepth=s!=2;skip=2;}
        static double mean(int p,int b){return frames[p][b]==0?0:ns[p][b]/1e6/frames[p][b];}
        static double avgNear(int p){return frames[p][0]==0?0:near[p][0]/(double)frames[p][0];}
        static double avgRelief(int p){return frames[p][0]==0?0:relief[p][0]/(double)frames[p][0];}
        static double avgWeapon(int p,int b){return frames[p][b]==0?0:weapon[p][b]/(double)frames[p][b];}
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
        /** One frame of the sheet; bare draws the rig alone in its template hues (the biped too), the animation judged without a weapon, a shield or any part. */
        record Frame(String name,String rig,String clip,float seconds,boolean bare){}
        static final List<Frame> FRAMES;
        static{
            List<Frame> f=new ArrayList<>();
            for(String rig:Anim.RIGS){f.add(new Frame(rig+"-rest",rig,"idle",0,false));f.add(new Frame(rig+"-walk",rig,"walk",.25f,false));f.add(new Frame(rig+"-attack",rig,"attack",TroopClips.PEAKS.get("attack"),false));
                f.add(new Frame(rig+"-guard",rig,"shield_up",TroopClips.LENGTHS.get("shield_up"),false));f.add(new Frame(rig+"-death",rig,"death",TroopClips.LENGTHS.get("death"),false));}
            for(int i=0;i<8;i++)f.add(new Frame("walk-"+i,"biped","walk",i/8f,false));
            for(int i=0;i<8;i++)f.add(new Frame("cape-"+i,"biped","walk",i/8f,false)); // the same stride from behind: the cape halves on the legs (Task 5 review, note 5)
            for(String n:TroopClips.NAMES)f.add(new Frame("bare-"+n,"biped",n,TroopClips.shotTime("biped",n),true)); // the 18 clips on the bare biped (Kort: animation is judged on a bare biped)
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
            if(f.rig().equals("biped")&&!f.bare()){
                boolean cape=f.name().startsWith("cape-");MobEntity who=f.name().startsWith("walk-")||cape?templar:squire;if(who==null)return;
                float yaw=cape?YAW+180:YAW; // the cape frames show the Templar from behind
                who.bodyYaw=who.prevBodyYaw=yaw;who.headYaw=who.prevHeadYaw=yaw;who.setYaw(yaw);who.prevYaw=yaw;who.setPitch(0);who.prevPitch=0;
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
