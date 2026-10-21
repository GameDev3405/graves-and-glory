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
    static{SUITES.put("banners",Banners::new);SUITES.put("engine",Engine::new);SUITES.put("commander",Commanders::new);SUITES.put("gilded",Gilded::new);SUITES.put("spike",Spike::new);SUITES.put("gilded-art",GildedArt::new);SUITES.put("graveborn",Graveborn::new);}
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
        Frame.register();
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
        static String show(Combat.HitLog h){return h==null?"no hit":"counters "+h.counters()+" rows "+h.rows()+" base "+h.base()+" traits "+h.traits()+" auras "+h.auras()+" commander "+h.commander()+" total "+h.total();}
        /** The newest logged hit on this troop, or null. */
        static Combat.HitLog hitOn(GravesAndGlory.Unit target){Combat.HitLog out=null;for(var h:Combat.LAST)if(h.target()==target.mob.getId())out=h;return out;}
        /** The troops a House shows as owned after the v1 fixture migrated: its Commons, plus the unlocked Templar. */
        static Set<String> expected(String house){
            Set<String> out=new LinkedHashSet<>();for(var t:Spec.TROOPS)if(t.house().equals(house)&&t.rarity().equals("common"))out.add(t.id());if(house.equals("gilded"))out.add("sentinel");return out;
        }
        public void cleanup(MinecraftClient c){
            server(c,b->{clearFixture();unforce(b);if(cost!=null)cost.handBack();if(rngSaved!=null)b.campaign.rollRng=rngSaved;if(b.running)b.campaign.returnLobby();});
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
                case 16->{if(since(b)<25)return; // and from behind, where the flank row applies and the shield does not
                    move(melee,0,-2.5,0);knight.mob.setHealth(knight.mob.getMaxHealth());mark=Combat.LAST.peekLast();
                    Combat.resolve(b,melee,knight.mob,5f,"melee",false);meleeBack=Combat.LAST.peekLast()!=mark?Combat.LAST.peekLast():null;
                    boolean f=front!=null&&near(front.counters(),0.35)&&front.rows().equals(List.of("C1"))&&frontLost>0&&frontLost<=3.5f+1e-3&&front.target()==knight.mob.getId()&&front.attacker()==bowFront.mob.getId()&&front.base()==10f&&near(front.traits(),1)&&near(front.auras(),1)&&near(front.commander(),1)&&near(front.total(),0.35);
                    boolean k=back!=null&&near(back.counters(),1.0)&&back.rows().isEmpty()&&backLost>frontLost&&near(back.total(),1.0)&&Math.abs(backLost-8.8f)<0.02f;
                    boolean gear=knight.mob.getArmor()==8&&knight.mob.getAttributeBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR)==0;
                    boolean l=lowered!=null&&near(lowered.counters(),0.75)&&lowered.rows().equals(List.of("C2"))&&loweredLost>frontLost;
                    boolean m1=meleeFront!=null&&near(meleeFront.counters(),0.6)&&meleeFront.rows().equals(List.of("C3")),m2=meleeBack!=null&&near(meleeBack.counters(),1.15)&&meleeBack.rows().equals(List.of("C16"));
                    report("p2-shield-counter",f&&k&&l&&m1&&m2&&gear,"raised shield, Bone Archer arrow from the front: "+show(front)+", lost "+frontLost+" of "+10*0.35+" (the shield did not stop it)="+f+"; from behind: "+show(back)+", lost "+backLost+" (want 8.8: base 10 under armour 8)="+k+"; shield lowered, front: "+show(lowered)+", lost "+loweredLost+"="+l+"; melee into the raised shield, front: "+show(meleeFront)+"="+m1+"; melee from behind: "+show(meleeBack)+"="+m2+"; the Squire's armour is "+knight.mob.getArmor()+" (iron helmet and chestplate = 8) on a husk base of "+knight.mob.getAttributeBaseValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR)+"="+gear);
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
                case 60->{ // p2-tick-cost: eight bot armies, 160 troops (20 each)
                    var p=sp(b);if(p==null)return;
                    if(cost==null)cost=tickCost(b,List.of("gilded_easy","gilded_easy","gilded_easy","gilded_easy","graveborn_easy","graveborn_easy","graveborn_easy","graveborn_easy"));
                    if(cost.step(p)){
                        report("p2-tick-cost",cost.last<40,"server tick "+String.format(Locale.ROOT,"%.2f",cost.last)+" ms (600-tick mean "+String.format(Locale.ROOT,"%.2f",cost.avg)+" ms) with "+cost.troops+" troops at the start and "+b.units.size()+" at the end ("+cost.note+")");
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
     * first; a short list repeats), and the server's tick time is read each tick. Call step on every server tick until it returns true.
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
     * heal and cooldown (p2-gilded-mend); the batch's counter rows with synthetic husk rows (p2-counters-gilded); Bulwark's share and rout
     * guard on the commander's own army at tiers III and I (p2-bulwark); and the Lord Marshal's Oath Banner through real orders
     * (p2-gilded-oath). Then the Champion's Stand Fast and the Marshal's Rally Horn through the ability command, with the HUD tile read on
     * the client (p2-champion-standfast, p2-marshal-rallyhorn); the showcase on map 0 (p2-gilded-showcase, screenshot gilded-showcase);
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
        Abilities.Banner planted;final Set<Abilities.Banner> seen=new LinkedHashSet<>();long plantAt=-1,movedIn=-1,rallied=-1,supportRouted=-1,secondAt=-1;
        boolean oathOk,supportHeld,fell,blockBack;String oathWhy="";int hornsBefore;
        // the actives, and the tile the client watches while tileWatch is 1
        volatile int tileWatch;volatile String firstActive="",readyFirst="",readyLater="";volatile int activeCooldownSeconds=-1;int readyFirstTick;
        long t0,routAfter=-1;boolean activeOk,heldThrough,lateHastened;String activeWhy="";double startDistance;
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
        static void reset(GravesAndGlory.Squad s){s.routedAt.clear();s.steadyUntil.clear();s.fullHealth.clear();s.assignments.clear();}
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
                case 10->{ // p2-gilded-steady: an Order Archer's arrow at a Risen 12 blocks off, on Hold, on Attack, and routing with Hold stored
                    var archer=post(b,dawn,"archer",0,-6,0,true);var target=post(b,dusk,"revenant",0,6,180,true);
                    dawn.order="hold";var onHold=Commanders.hit(b,archer,target.mob,"arrow");dawn.order="attack";var onAttack=Commanders.hit(b,archer,target.mob,"arrow");
                    dawn.order="hold";dawn.routedAt.put("ranged",b.time);var routing=Commanders.hit(b,archer,target.mob,"arrow");dawn.routedAt.clear();
                    boolean ok=archer.tags.contains("steady")&&is(onHold,1.2,"C18a")&&onAttack!=null&&near(onAttack.counters(),1.0)&&onAttack.rows().isEmpty()&&routing!=null&&near(routing.counters(),1.0)&&routing.rows().isEmpty();
                    report("p2-gilded-steady",ok,"an Order Archer (tags "+archer.tags+") at a Risen: on Hold "+show(onHold)+"; on Attack "+show(onAttack)+"; routing with Hold stored (its effective order is retreat) "+show(routing));
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
                    dawn.order="charge";b.chargeHorn(dawn,"infantry");var c5=Commanders.hit(b,aCharger,tArcher.mob,"melee");var c6=Commanders.hit(b,aCharger,tArcher.mob,"melee");dawn.order="hold";
                    var c16=Commanders.hit(b,aRear,tRear.mob,"melee");
                    boolean tags=aSunder.tags.containsAll(List.of("sunder","beast","cavalry"))&&!aSunder.tags.contains("shield")&&aBolt.tags.containsAll(List.of("armour_bolt","beast","cavalry"));
                    boolean ok=raised&&tags&&is(c3,0.6,"C3")&&is(c4,1.0,"C4")&&is(c14,1.4,"C14")&&is(c15,0.7,"C15")&&is(c5,1.5,"C5")&&is(c6,1.2,"C6")&&is(c16,1.15,"C16");
                    report("p2-counters-gilded",ok,"against the raised shield (raised="+raised+"), from the front: an Order Squire's blow "+show(c3)+"; a Gravecaller's blight "+show(c4)+"; the synthetic husk with sunder (tags "+aSunder.tags+") "+show(c14)+"; the synthetic husk with armour_bolt (tags "+aBolt.tags+"), an arrow "+show(c15)+"; an Order Squire's first blow on an Order Archer after the charge order "+show(c5)+", then "+show(c6)+"; its blow on a Risen from behind "+show(c16)+"; the synthetic rows' tags carry their ability and beast, cavalry="+tags);
                    clearFixture(b);next(b,20);
                }
                case 20->{ // p2-bulwark, tier III: the commander's own army, 14 Gilded slots: four Order Squires (its infantry), an Order Longbow 9.25 blocks from their centre, nine Order Archers 18 or more away; a frozen Bone Archer of Dusk
                    reset(own);own.banner=banner("knight*4,longbow,archer*9");
                    knights.clear();for(int i=0;i<4;i++)knights.add(post(b,own,"knight",-8+(i%2)*1.5,-0.75+(i/2)*1.5,-90,true));
                    guard=post(b,own,"longbow",2,0,-90,true);for(int i=0;i<9;i++)post(b,own,"archer",11,-8+i*2,-90,true);
                    foe=post(b,dusk,"bonebow",-2,4,90,true);Houses.recompute(own);next(b,21);
                }
                case 21->{if(since(b)<5)return;
                    var k=knights.get(0);var arrow=Commanders.hit(b,foe,k.mob,"arrow");var bolt=Commanders.hit(b,foe,k.mob,"bolt");var blight=Commanders.hit(b,foe,k.mob,"blight");var melee=Commanders.hit(b,foe,k.mob,"melee");var bare=Commanders.hit(b,foe,guard.mob,"arrow");
                    boolean share=traits(arrow,0.55)&&traits(bolt,0.55)&&traits(blight,0.55)&&traits(melee,1.0)&&traits(bare,1.0);
                    int tier=own.traits.getOrDefault("gilded",0),bannerTier=Houses.bannerTiers(own).getOrDefault("gilded",0),slots=RosterMath.houseSlots(own.banner,RosterMath.Catalog.fromSpec()).getOrDefault("gilded",0);
                    bulwarkOk=share&&tier==3&&bannerTier==3&&slots==14;
                    bulwarkWhy="his banner holds "+slots+" Gilded slots (tier "+bannerTier+"), the living army is at tier "+tier+" "+own.traits+"; an Order Squire (shield) takes traits x"+tr(arrow)+" from an arrow, x"+tr(bolt)+" from a bolt, x"+tr(blight)+" from blight and x"+tr(melee)+" from melee, the Order Longbow (no shield) x"+tr(bare)+" from an arrow="+share;
                    for(var u:knights)u.mob.setHealth(u.mob.getMaxHealth());
                    own.fullHealth.put("infantry",living(b,own,"infantry")*10);own.fullHealth.put("ranged",living(b,own,"ranged"));heldIII=true;next(b,22);
                }
                case 22->{ // two morale checks or more: the weakened Squires hold while the Longbow stands within 12 of their centre
                    if(Morale.routing(own,"infantry"))heldIII=false;
                    if(since(b)<45)return;
                    Vec3d c=Abilities.centre(b,own,"infantry");double d=distance(c,guard.mob),threat=distance(c,foe.mob);
                    double strength=BattleMath.strength(living(b,own,"infantry"),own.fullHealth.get("infantry"));boolean guarded=Houses.BULWARK_GUARD.holds(b,own,"infantry");
                    bulwarkOk&=heldIII&&guarded&&d<=12&&strength<Spec.MORALE[0].routShare();
                    bulwarkWhy+="; the weakened Squires (strength "+fmt(strength)+", a Bone Archer "+fmt(threat)+" from their centre) did not rout in 45 ticks="+heldIII+" with the Longbow "+fmt(d)+" from their centre (BULWARK_GUARD holds="+guarded+")";
                    // tier I: eight Order Archers leave (six Gilded slots stand); the Longbow moves to 8 blocks from the Squires' centre
                    int gone=0;for(var u:new ArrayList<>(fixture))if(u.spec.id().equals("archer")&&gone<8){drop(b,u);gone++;}
                    Houses.recompute(own);move(guard,c.x-site.getX()-.5+8,c.z-site.getZ()-.5,-90);
                    own.routedAt.clear();own.steadyUntil.clear();own.fullHealth.put("infantry",living(b,own,"infantry")*10);own.fullHealth.put("ranged",living(b,own,"ranged"));
                    arrowI=Commanders.hit(b,foe,knights.get(0).mob,"arrow");next(b,23);
                }
                case 23->{
                    if(!Morale.routing(own,"infantry")&&since(b)<=45)return;
                    boolean routed=Morale.routing(own,"infantry");int tier=own.traits.getOrDefault("gilded",0);Vec3d c=Abilities.centre(b,own,"infantry");double d=distance(c,guard.mob);
                    boolean ok=routed&&tier==1&&traits(arrowI,0.85)&&Math.abs(d-8)<0.3;
                    bulwarkOk&=ok;
                    bulwarkWhy+="; at tier I "+own.traits+" (radius 5) with the Longbow "+fmt(d)+" away the Squires routed="+routed+(routed?" "+since(b)+" ticks later":"")+"; a Squire takes x"+tr(arrowI)+" from an arrow (want 0.85)";
                    report("p2-bulwark",bulwarkOk,bulwarkWhy);
                    clearFixture(b);reset(own);next(b,30);
                }
                case 30->{ // p2-gilded-oath: Lord Marshal Aldric (attacks off, ability on), an Order Squire 3.6 blocks from where his banner will stand and one 14.9 away, two Order Archers 14.8 away, a Sunweaver 10.6 away, and frozen Risen beside them
                    Abilities.clearBanners(b);reset(own);
                    marshal=post(b,own,"gl_marshal",0,0,0,true);marshal.abilityAt=0;marshal.seenOrder=null;
                    near=post(b,own,"knight",3,1,0,true);far=post(b,own,"knight",-11,-11,0,true);
                    runners.clear();runners.add(post(b,own,"archer",10,10,0,true));runners.add(post(b,own,"archer",11,9,0,true));
                    support=post(b,own,"mage",-8,6,0,true);
                    foe=post(b,dusk,"revenant",3,3,180,true);foe2=post(b,dusk,"revenant",-11,-9,180,true);post(b,dusk,"revenant",8,12,180,true);
                    for(String g:Morale.GROUPS)own.fullHealth.put(g,living(b,own,g));own.fullHealth.put("ranged",living(b,own,"ranged")*10); // the archers are weak: they rout where no banner stands
                    order(b,"infantry","move","block",site.getX()+.5,site.getZ()+.5,0);
                    seen.clear();planted=null;plantAt=-1;movedIn=-1;rallied=-1;supportRouted=-1;secondAt=-1;supportHeld=true;fell=false;blockBack=false;oathOk=true;oathWhy="";
                    next(b,31);
                }
                case 31->{ // his first beat saw Move; the weak archers rout away from any banner; then the infantry is ordered to Hold
                    boolean routed=Morale.routing(own,"ranged");if(since(b)<20||!routed&&since(b)<=80)return;
                    boolean none=b.banners.isEmpty();oathOk&=none&&routed&&"move".equals(marshal.seenOrder);
                    oathWhy+="before the Hold order no banner stood="+none+" (Aldric's beats saw "+marshal.seenOrder+"), the weakened Order Archers routed="+routed+"; ";
                    hornsBefore=BattleEffects.horns.getOrDefault("rally",0);order(b,"infantry","hold","block",site.getX()+.5,site.getZ()+.5,0);next(b,32);
                }
                case 32->{ // the banner, its aura and its guard
                    if(b.banners.isEmpty()){if(since(b)>20){oathWhy+="no banner within 20 ticks of the Hold order";report("p2-gilded-oath",false,oathWhy);clearFixture(b);next(b,50);}return;}
                    planted=b.banners.get(0);seen.add(planted);var row=Abilities.row(marshal.spec);plantAt=planted.until()-row.durationTicks();
                    var state=planted.block()==null?null:b.world.getBlockState(planted.block());boolean block=state!=null&&state.getBlock() instanceof net.minecraft.block.BannerBlock;
                    double fromHim=distance(planted.at(),marshal.mob);int horns=BattleEffects.horns.getOrDefault("rally",0)-hornsBefore;
                    boolean shape=row.id().equals("oath_banner")&&planted.team()==own.team&&near(planted.radius(),12)&&near(planted.amount(),0.1)&&row.durationTicks()==300&&marshal.abilityAt==plantAt+row.cooldownTicks()&&row.cooldownTicks()==600&&fromHim<=2&&b.time-plantAt<=1&&since(b)<=11&&horns>=1;
                    var inside=Commanders.hit(b,near,foe.mob,"melee");var himself=Commanders.hit(b,marshal,foe.mob,"melee");var beyond=Commanders.hit(b,far,foe2.mob,"melee");var theirs=Commanders.hit(b,foe,near.mob,"melee");
                    boolean aura=auras(inside,1.10)&&auras(himself,1.10)&&auras(beyond,1.0)&&auras(theirs,1.0);
                    oathOk&=shape&&block&&aura;
                    oathWhy+="the Hold order planted a banner "+since(b)+" ticks later, "+fmt(fromHim)+" from Aldric (block "+(state==null?"none":state.getBlock().getName().getString())+"="+block+"), team "+planted.team()+", radius "+planted.radius()+", +"+planted.amount()+", standing "+(planted.until()-plantAt)+" ticks, his next plant at +"+(marshal.abilityAt-plantAt)+", rally horns +"+horns+"="+shape
                        +"; auras on a blow by the Squire "+fmt(distance(planted.at(),near.mob))+" off "+au(inside)+", by Aldric "+au(himself)+", by the Squire "+fmt(distance(planted.at(),far.mob))+" off "+au(beyond)+", by a Dusk Risen inside "+au(theirs)+"="+aura;
                    // the Sunweaver 10.6 off is weakened now, for the banner's guard; the routing archers walk into the radius
                    own.fullHealth.put("support",living(b,own,"support")*10);own.steadyUntil.remove("support");
                    move(runners.get(0),4,4,0);move(runners.get(1),5,3,0);movedIn=b.time;next(b,33);
                }
                case 33->{ // the routing archers rally inside the radius
                    if(Morale.routing(own,"support"))supportHeld=false;
                    if(!Morale.routing(own,"ranged"))rallied=b.time-movedIn;else if(b.time-movedIn>40)rallied=999;
                    if(rallied>=0){Vec3d c=Abilities.centre(b,own,"ranged");boolean ok=rallied<=20;oathOk&=ok;oathWhy+="; the routing Order Archers walked to "+fmt(distance(c,marshal.mob))+" from Aldric and regrouped "+(rallied==999?"never":rallied+" ticks later")+" (want 20 or fewer)="+ok;next(b,34);}
                }
                case 34->{ // the cooldown: Move then Hold again at +100 plant nothing; the banner falls at +300 and the guard with it; Move and Hold after +600 plant again
                    for(var banner:b.banners)seen.add(banner);
                    long t=b.time-plantAt;
                    if(t<300&&Morale.routing(own,"support"))supportHeld=false;
                    if(t==100||t==605)order(b,"infantry","move","block",site.getX()+.5,site.getZ()+.5,0);
                    if(t==115||t==620)order(b,"infantry","hold","block",site.getX()+.5,site.getZ()+.5,0);
                    if(t==301){fell=b.banners.isEmpty();var state=planted.block()==null?null:b.world.getBlockState(planted.block());blockBack=state!=null&&state.isAir();}
                    if(t>300&&supportRouted<0&&Morale.routing(own,"support"))supportRouted=t;
                    if(t>=620&&secondAt<0&&b.banners.size()==1&&b.banners.get(0)!=planted)secondAt=t;
                    if(secondAt>=0||t>660){
                        long early=seen.stream().filter(x->x!=planted&&x.until()-300<plantAt+600).count();
                        boolean ok=early==0&&supportHeld&&fell&&blockBack&&supportRouted>300&&supportRouted<=345&&secondAt>=620&&secondAt<=632;
                        oathOk&=ok;
                        oathWhy+="; Move then Hold at +100 and +115 planted "+early+" more banners before +600; the weakened Sunweaver 10.6 off held while the banner stood="+supportHeld+"; at +301 the banner had fallen="+fell+" and its block was air again="+blockBack+"; the Sunweaver routed at +"+supportRouted+" (want just after +300); Move and Hold at +605 and +620 planted the next banner at +"+secondAt+"="+ok;
                        report("p2-gilded-oath",oathOk,oathWhy);
                        clearFixture(b);Abilities.clearBanners(b);reset(own);next(b,35);
                    }
                }
                case 35->{ // p2-gilded-oath-plant and p2-gilded-oath-squad: a fresh banner planted by a direct call, so nothing but the plant itself has run (the Battle tick, which holds the 10-tick scan, has not)
                    Abilities.clearBanners(b);reset(own);dawn.routedAt.clear();
                    marshal=post(b,own,"gl_marshal",0,0,0,true);marshal.abilityAt=0;marshal.seenOrder=null;
                    var ownKnight=post(b,own,"knight",3,1,0,true);var ownArcher=post(b,own,"archer",4,3,0,true);var ownFar=post(b,own,"mage",-14,0,0,true);
                    var allyKnight=post(b,dawn,"knight",2,-2,0,true);var allyArcher=post(b,dawn,"archer",-4,4,0,true);var foeR=post(b,dusk,"revenant",3,3,180,true);
                    own.routedAt.put("ranged",b.time);own.routedAt.put("support",b.time);dawn.routedAt.put("ranged",b.time);
                    b.squads.put(dawn.owner,dawn); // the allied army (same team, another squad) is a seated army of the round for this one call, as the old team-wide scan would have found it
                    try{
                        var row=Abilities.row(marshal.spec);Abilities.plant(b,marshal,row,b.time);
                        var banner=b.banners.isEmpty()?null:b.banners.get(0);
                        boolean stands=b.banners.size()==1&&banner.squad()==own&&banner.team()==own.team;
                        double dArcher=banner==null?-1:distance(banner.at(),ownArcher.mob),dFar=banner==null?-1:distance(banner.at(),ownFar.mob),dAllyK=banner==null?-1:distance(banner.at(),allyKnight.mob),dAllyA=banner==null?-1:distance(banner.at(),allyArcher.mob);
                        boolean placed=stands&&dArcher<=12&&dFar>12&&dAllyK<=12&&dAllyA<=12;
                        boolean rallied=!Morale.routing(own,"ranged"),farHeld=Morale.routing(own,"support");
                        report("p2-gilded-oath-plant",placed&&rallied&&farHeld,"on the plant tick, before any scan: one banner of his own army stands="+stands+"; the routing Order Archer "+fmt(dArcher)+" from it (radius 12) was rallied="+rallied+"; the routing Sunweaver "+fmt(dFar)+" from it still routes="+farHeld);
                        Abilities.rally(b,banner); // the scan's pass, run by hand: it must not reach the allied army either
                        var mine=Commanders.hit(b,ownKnight,foeR.mob,"melee");var ally=Commanders.hit(b,allyKnight,foeR.mob,"melee");
                        boolean aura=auras(mine,1.10)&&auras(ally,1.0);
                        boolean guard=Abilities.OATH_GUARD.holds(b,own,"infantry")&&!Abilities.OATH_GUARD.holds(b,dawn,"infantry")&&!Abilities.OATH_GUARD.holds(b,dawn,"ranged");
                        boolean allyRouts=Morale.routing(dawn,"ranged");
                        report("p2-gilded-oath-squad",own.team==dawn.team&&placed&&aura&&guard&&allyRouts,"an allied army of the same team (team "+dawn.team+" and his "+own.team+") with a Squire "+fmt(dAllyK)+" and an Order Archer "+fmt(dAllyA)+" from the banner (inside 12): auras his Squire "+au(mine)+" (want 1.1), the ally's Squire "+au(ally)+" (want 1.0)="+aura
                            +"; guard on his infantry "+Abilities.OATH_GUARD.holds(b,own,"infantry")+", on the ally's infantry "+Abilities.OATH_GUARD.holds(b,dawn,"infantry")+" and ranged "+Abilities.OATH_GUARD.holds(b,dawn,"ranged")+" (want true, false, false)="+guard+"; the ally's routing archers were not rallied by the plant or by the scan="+allyRouts);
                    }finally{b.squads.remove(dawn.owner);dawn.routedAt.clear();own.routedAt.clear();}
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
                    own.fullHealth.put("infantry",living(b,own,"infantry")*10);own.steadyUntil.clear();heldThrough=true;routAfter=-1;tileWatch=1;next(b,52);
                }
                case 52->{ // the guard holds the weakened Risen through the 8 s; it ends with the active, and they rout
                    long t=b.time-t0;var c=Commander.row(own);
                    if(t<c.durationTicks()&&Morale.routing(own,"infantry"))heldThrough=false;
                    if(t==c.durationTicks()+1){var p=sp(b);boolean ended=own.abilityUntil==0&&!Commander.running(b,own)&&p!=null&&!p.hasStatusEffect(StatusEffects.RESISTANCE);activeOk&=ended;activeWhy+="; at +"+t+" it has ended and his Resistance is gone="+ended;}
                    if(t>c.durationTicks()&&routAfter<0&&Morale.routing(own,"infantry"))routAfter=t-c.durationTicks();
                    if(routAfter>=0&&!readyLater.isEmpty()||t>c.durationTicks()+400){
                        tileWatch=0;
                        int active=seconds(firstActive),cool=activeCooldownSeconds,ready1=seconds(readyFirst),ready2=seconds(readyLater);
                        boolean tile=active==8&&cool==90&&ready1>=80&&ready1<=82&&ready2>=0&&ready2<ready1;
                        boolean guard=heldThrough&&routAfter>0&&routAfter<=45;
                        report("p2-champion-standfast",activeOk&&guard&&tile,activeWhy+"; his weakened Risen held through it="+heldThrough+" and routed "+(routAfter<0?"never":routAfter+" ticks after it ended")+"="+guard+"; the HUD tile first read \""+firstActive+"\" with the cooldown at "+cool+" s (want ACTIVE 8 s and 90 s), then \""+readyFirst+"\" and 2 s later \""+readyLater+"\" (counting down)="+tile);
                        clearFixture(b);reset(own);next(b,60);
                    }
                }
                case 60->{tileWatch=0;if(b.running){b.campaign.returnLobby();return;}command(b,"class:marshal",61);}
                case 61->{if(since(b)<20)return;var s=b.squads.get(player);
                    if(profile(b).commander.equals("marshal")&&s!=null&&s.commander.equals("marshal"))next(b,62);
                    else if(since(b)>200){report("p2-marshal-rallyhorn",false,"class:marshal never reached the profile (it says "+profile(b).commander+")");next(b,70);}}
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
                case 64->{if(since(b)<30)return; // settled on Hold; the Order Archers break (as Morale marks a rout) on the tick he sounds the horn
                    var p=sp(b);var c=Commander.row(own);own.steadyUntil.clear();own.routedAt.put("ranged",b.time);
                    hornsBefore=BattleEffects.horns.getOrDefault("rally",0);startDistance=meanDistance(p);firstActive="";readyFirst="";readyLater="";activeCooldownSeconds=-1;
                    t0=b.time;b.command(p,"ability");
                    // the order path's spot: where he stands, one block above the ground there (he stands on the platform at y 140, far above it)
                    Vec3d to=p.getPos();Vec3d spot=new Vec3d(to.x,Arena.ground((int)to.x,(int)to.z)+1,to.z);boolean moves=true,snapped=Math.abs(to.y-spot.y)>20;StringBuilder orders=new StringBuilder();
                    for(var gr:Spec.GROUPS){if(gr.id().equals("all"))continue;String g=gr.id();var a=own.assignments.get(g);boolean m=a!=null&&a.order.equals("move")&&a.target.squaredDistanceTo(spot)<1e-6&&a.issued==t0;moves&=m;orders.append(g).append(' ').append(a==null?"none":a.order+" to "+fmt(a.target.x)+", "+fmt(a.target.y)+", "+fmt(a.target.z)).append(m?"":" (BAD)").append("; ");}
                    moves&=snapped;
                    boolean steady=own.steadyUntil.getOrDefault("ranged",-1L)==t0+800&&own.steadyUntil.getOrDefault("infantry",-1L)==t0+400&&own.steadyUntil.getOrDefault("support",-1L)==t0+400;
                    int speedy=0,troops=0;StringBuilder speeds=new StringBuilder();
                    for(var u:fixture)if(u.mob.isAlive()){troops++;var a=u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);var m=a==null?null:a.getModifier(Commander.RALLY);
                        boolean good=m!=null&&near(m.getValue(),c.value1())&&m.getOperation()==net.minecraft.entity.attribute.EntityAttributeModifier.Operation.MULTIPLY_TOTAL&&Math.abs(a.getValue()/a.getBaseValue()-(1+c.value1()))<1e-6;if(good)speedy++;
                        speeds.append(u.spec.id()).append(' ').append(a==null?"-":fmt(a.getBaseValue())+" -> "+String.format(Locale.ROOT,"%.4f",a.getValue())).append(good?"":" (BAD)").append(", ");}
                    int horns=BattleEffects.horns.getOrDefault("rally",0)-hornsBefore;
                    boolean started=c.id().equals("marshal")&&Commander.running(b,own,"rally_horn")&&own.abilityUntil==t0+c.durationTicks()&&Commander.readyIn(b,own)==2400&&c.cooldownTicks()==2400&&c.durationTicks()==300;
                    boolean rallied=!Morale.routing(own,"ranged");
                    activeOk=started&&rallied&&moves&&steady&&speedy==troops&&troops==6&&horns==1;
                    activeWhy="the ability command as "+own.commander+": rally_horn runs="+Commander.running(b,own,"rally_horn")+" to +"+(own.abilityUntil-t0)+" (want 300), readyIn "+Commander.readyIn(b,own)+" (want 2400, 120 s)="+started+"; the routing Order Archers rallied="+rallied+"; orders for every group (the ground at his feet, y "+fmt(spot.y)+", not his own y "+fmt(to.y)+"="+snapped+"): "+orders+"="+moves
                        +"; steadyUntil "+own.steadyUntil+" at t0 "+t0+" (want ranged +800: rallied +400 then +400; the others +400)="+steady+"; speed: "+speeds+speedy+" of "+troops+" troops x(1 + "+c.value1()+"); rally horns +"+horns+" (want 1: the rally it calls adds none)";
                    tileWatch=1;late=null;lateHastened=false;next(b,65);
                }
                case 65->{long t=b.time-t0;var c=Commander.row(own);
                    if(t==50)late=free(b,"knight",8,6);
                    if(t==52&&late!=null){var a=late.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);lateHastened=a!=null&&a.getModifier(Commander.RALLY)!=null;}
                    if(t==100){var p=sp(b);double d=meanDistance(p);boolean closer=startDistance-d>=5;activeOk&=closer;activeWhy+="; his troops' mean distance to him "+fmt(startDistance)+" -> "+fmt(d)+" after 100 ticks="+closer;}
                    if(t==c.durationTicks()+1){int left=0;for(var u:fixture){var a=u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);if(a!=null&&a.getModifier(Commander.RALLY)!=null)left++;}
                        boolean ended=own.abilityUntil==0&&!Commander.running(b,own)&&left==0;activeOk&=ended&&lateHastened;activeWhy+="; a Squire that joined at +50 had the speed by +52="+lateHastened+"; at +"+t+" it has ended and "+left+" troops keep the speed="+ended;}
                    if(t>c.durationTicks()+1&&!readyLater.isEmpty()||t>c.durationTicks()+400){
                        tileWatch=0;int active=seconds(firstActive),cool=activeCooldownSeconds,ready1=seconds(readyFirst),ready2=seconds(readyLater);
                        boolean tile=active==15&&cool==120&&ready1>=103&&ready1<=105&&ready2>=0&&ready2<ready1;
                        report("p2-marshal-rallyhorn",activeOk&&tile,activeWhy+"; the HUD tile first read \""+firstActive+"\" with the cooldown at "+cool+" s (want ACTIVE 15 s and 120 s), then \""+readyFirst+"\" and 2 s later \""+readyLater+"\" (counting down)="+tile);
                        reset(own);next(b,66);
                    }
                }
                case 66->{ // p2-marshal-rallyhorn-spots: the horn's Move takes the order path's spot rules: while he stands in river water or inside the arena's 10-block edge margin no group is moved, and the rest of the horn still happens
                    var p=sp(b);
                    Vec3d river=null;for(int z=20;z<200&&river==null;z++){int x=(int)Math.round(CampaignMaps.riverX(z));if(Arena.river(x,z)&&Arena.ground(x,z)<64)river=new Vec3d(x+.5,Arena.ground(x,z)+1,z+.5);}
                    Vec3d[] where={river,new Vec3d(.5,100,GravesAndGlory.R.arenaHalfZ()-5)};String[] name={"in river water","inside the edge margin"};
                    boolean ok=river!=null&&!GravesAndGlory.Battle.inArena(where[1].x,where[1].z)&&GravesAndGlory.Battle.wet(where[0]==null?0:where[0].x,where[0]==null?0:where[0].z);StringBuilder why=new StringBuilder();
                    for(int i=0;i<2&&where[i]!=null;i++){
                        Map<String,GravesAndGlory.Assignment> before=new HashMap<>(own.assignments);
                        b.relocate(p,where[i].x,where[i].y,where[i].z,0,0);
                        Commander.reset(b,own);own.routedAt.put("ranged",b.time);own.steadyUntil.clear();long t=b.time;
                        b.command(p,"ability");
                        boolean unmoved=true;for(var gr:Spec.GROUPS)if(!gr.id().equals("all"))unmoved&=own.assignments.get(gr.id())==before.get(gr.id());
                        boolean horn=Commander.running(b,own,"rally_horn")&&!Morale.routing(own,"ranged")&&own.steadyUntil.getOrDefault("ranged",-1L)==t+800&&own.steadyUntil.getOrDefault("infantry",-1L)==t+400;
                        int speedy=0,troops=0;for(var u:fixture)if(u.mob.isAlive()){troops++;var a=u.mob.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MOVEMENT_SPEED);if(a!=null&&a.getModifier(Commander.RALLY)!=null)speedy++;}
                        ok&=unmoved&&horn&&speedy==troops&&troops>0;
                        why.append("he stands ").append(name[i]).append(" at ").append(fmt(where[i].x)).append(", ").append(fmt(where[i].z)).append(": no group got a new order=").append(unmoved).append("; the horn still ran, rallied the archers and set the steadiness=").append(horn).append("; the speed reached ").append(speedy).append(" of ").append(troops).append(" troops; ");
                    }
                    Commander.reset(b,own);b.relocate(p,site.getX()-8.5,site.getY(),site.getZ()+.5,90,20);
                    report("p2-marshal-rallyhorn-spots",ok,"(river spot "+(river==null?"none found":fmt(river.x)+", "+fmt(river.z)+", ground "+Arena.ground((int)river.x,(int)river.z))+") "+why);
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
     * Task 7 (batch 2): the Graveborn mechanics. The two new mobs (burn, water, goals), cleave, frost shot, the Gravecaller's blight,
     * the fearless and dread rout shares, Rise Again (once, not Legendaries, not temporary troops, the chance by tier), the Warlock's
     * Grave Bloom with its death log, the cleanup of temporary Risen at the end of a round and at a map switch, and the tick cost. The
     * fixture is Gilded's: a stone platform in the sky above Dawn's base, two ad hoc armies and the commander's own (a Warlock);
     * troops are posted frozen and pinned, and every kill goes through the real damage path (Combat.resolve, or damage with no
     * attacker), so Rise Again's ALLOW_DEATH hook and the AFTER_DEATH death log run as they do in a round.
     */
    static final class Graveborn implements Suite,ServerWatch {
        record Pin(GravesAndGlory.Unit u,Vec3d at,float yaw){}
        volatile int stage;volatile UUID player;volatile String wantShot;volatile boolean shotTaken,finished;
        long stageAt,t0;int shotDelay,shotAfter=12;String classBefore;
        BlockPos site;final List<GravesAndGlory.Unit> fixture=new ArrayList<>(),group=new ArrayList<>();final List<Pin> pins=new ArrayList<>();
        final Set<net.minecraft.util.math.ChunkPos> forced=new HashSet<>();
        GravesAndGlory.Squad own,dawn,dusk;GravesAndGlory.Unit hk,hexer,stalker,reaper;
        // side teams: one audit after each place the suite removes or raises troops
        int audits;final Map<String,String> staleSeen=new LinkedHashMap<>();final List<String> auditNotes=new ArrayList<>();
        // p2-graveborn-mobs
        BlockPos tank;final List<MobEntity> army=new ArrayList<>();MobEntity vanillaZombie,vanillaStray,armyInTank,vanillaInTank;
        boolean mobsOk,armyBurned,controlBurned,strayControlBurned;int submerged;String mobsWhy="";
        // the blight and the frost shot
        Projectiles.Shot shot;GravesAndGlory.Unit centre,inside,outside;long burst=-1;
        // the rout shares
        boolean moraleOk,early,heldAt15,routedAt10;String moraleWhy="";long routedAfter=-1;
        // Rise Again
        GravesAndGlory.Unit risen,riseK1,riseControl,riseK2;float hpK1,hpK2;boolean riseOk,heldStun,controlHit,afterStun;String riseWhy="";int soulsBefore;
        // Grave Bloom
        Vec3d aim;final List<GravesAndGlory.Unit> bloom=new ArrayList<>(),corpses=new ArrayList<>();boolean bloomOk;String bloomWhy="";boolean shown;int deathsAfter;
        // the cleanup and the tick cost
        TickCost cost;boolean pathsOk,inserted;String pathsWhy="";GravesAndGlory.Unit loose;int toMap;Vec3d lobbySpot;

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
        long since(GravesAndGlory.Battle b){return b.time-stageAt;}
        ServerPlayerEntity sp(GravesAndGlory.Battle b){return b.server.getPlayerManager().getPlayer(player);}
        Profile profile(GravesAndGlory.Battle b){return b.campaign.profile(player);}
        void requestShot(String name,int after){shotTaken=false;shotDelay=0;shotAfter=after;wantShot=name;}
        static boolean near(double a,double b){return Math.abs(a-b)<1e-9;}
        static String fmt(double v){return String.format(Locale.ROOT,"%.2f",v);}
        static String show(Combat.HitLog h){return Engine.show(h);}
        static double distance(Vec3d a,Vec3d c){return Math.sqrt(GravesAndGlory.Battle.horizontalDistance(a,c));}
        static long temporaries(GravesAndGlory.Battle b){return b.units.stream().filter(u->u.temporary).count();}
        /** A troop of the army on the platform at (dx, dz) from its centre with its AI off, held there each tick by the pins; a frozen troop never attacks or casts. */
        GravesAndGlory.Unit post(GravesAndGlory.Battle b,GravesAndGlory.Squad s,String troop,double dx,double dz,float yaw,boolean freeze){
            Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);
            var u=b.spawn(s,Campaign.troop(troop),30+fixture.size(),at);u.mob.setAiDisabled(true);if(freeze)Phase1Checks.freeze(u);
            fixture.add(u);pins.add(new Pin(u,at,yaw));u.deployed=at;Engine.apply(u,at,yaw);return u;
        }
        void move(GravesAndGlory.Unit u,double dx,double dz,float yaw){Vec3d at=new Vec3d(site.getX()+.5+dx,site.getY(),site.getZ()+.5+dz);pins.removeIf(p->p.u()==u);pins.add(new Pin(u,at,yaw));u.deployed=at;Engine.apply(u,at,yaw);}
        /** The troop's armour off, so the health it loses is the amount applied. */
        static GravesAndGlory.Unit bare(GravesAndGlory.Unit u){for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))u.mob.equipStack(slot,ItemStack.EMPTY);return u;}
        /** A real blow of this size from the attacker's troop, past the victim's hurt cooldown, through Combat.resolve (so the ALLOW_DEATH hook and AFTER_DEATH run if it kills). */
        static float strike(GravesAndGlory.Battle b,GravesAndGlory.Unit attacker,GravesAndGlory.Unit victim,float base){victim.mob.timeUntilRegen=0;victim.mob.hurtTime=0;return Combat.resolve(b,attacker,victim.mob,base,"melee",false);}
        /** A fall with no attacker, past the hurt cooldown: the plain damage path (no kill credit). */
        static void fall(GravesAndGlory.Battle b,GravesAndGlory.Unit victim){victim.mob.timeUntilRegen=0;victim.mob.hurtTime=0;victim.mob.damage(b.world.getDamageSources().generic(),1000f);}
        void platform(GravesAndGlory.Battle b,BlockPos c,int half,net.minecraft.block.BlockState floor){
            for(int x=-half;x<=half;x++)for(int z=-half;z<=half;z++){b.world.setBlockState(c.add(x,-1,z),floor);for(int y=0;y<=6;y++)b.world.setBlockState(c.add(x,y,z),Blocks.AIR.getDefaultState());}
        }
        void force(GravesAndGlory.Battle b,BlockPos from,BlockPos to){
            for(int cx=from.getX()>>4;cx<=to.getX()>>4;cx++)for(int cz=from.getZ()>>4;cz<=to.getZ()>>4;cz++){b.world.setChunkForced(cx,cz,true);b.world.getChunk(cx,cz);forced.add(new net.minecraft.util.math.ChunkPos(cx,cz));}
        }
        void unforce(GravesAndGlory.Battle b){for(var p:forced)b.world.setChunkForced(p.x,p.z,false);forced.clear();}
        void drop(GravesAndGlory.Battle b,GravesAndGlory.Unit u){u.mob.discard();b.units.remove(u);fixture.remove(u);group.remove(u);pins.removeIf(p->p.u()==u);audit(b,"drop",false);}
        void dropAll(GravesAndGlory.Battle b,List<GravesAndGlory.Unit> us,String where){for(var u:new ArrayList<>(us)){u.mob.discard();b.units.remove(u);fixture.remove(u);pins.removeIf(p->p.u()==u);}us.clear();audit(b,where,false);}
        void clearFixture(GravesAndGlory.Battle b,String where){for(var u:fixture)u.mob.discard();b.units.removeAll(fixture);fixture.clear();group.clear();pins.clear();bloom.clear();corpses.clear();audit(b,where,false);}
        /** After a removal or a raising: no gg_ side team keeps a member that is not a living troop of b.units (and with `none` standing, none keeps any). */
        void audit(GravesAndGlory.Battle b,String where,boolean none){
            var stale=staleSides(b);var all=sideMembers(b);audits++;
            for(String m:none?all:stale)staleSeen.putIfAbsent(m,where);
            if(auditNotes.size()<24)auditNotes.add(where+" "+all.size()+" on a team"+(stale.isEmpty()?"":", "+stale.size()+" stale"));
        }
        void hold(){for(var p:pins)if(p.u().mob.isAlive())Engine.apply(p.u(),p.at(),p.yaw());}
        /** The army's stored state back to nothing: no rout, no steadiness, no health ceiling, no orders. */
        static void reset(GravesAndGlory.Squad s){s.routedAt.clear();s.steadyUntil.clear();s.fullHealth.clear();s.assignments.clear();}
        /** Sets every living troop of the list to this share of its health, and the army's ceiling for the group so its strength is that share. */
        void weaken(GravesAndGlory.Squad s,String grp,List<GravesAndGlory.Unit> us,double share){
            double living=0;for(var u:us)if(u.mob.isAlive()){u.mob.setHealth((float)(u.mob.getMaxHealth()*share));living+=u.mob.getHealth();}
            s.fullHealth.put(grp,living/share);
        }
        /** Every army's troops leave and none come back; a stone sky platform 40 blocks east of Dawn's base; two ad hoc armies (bots, so no profile is read) beside the commander's own, which keeps his seat and his class. */
        void stageFixture(GravesAndGlory.Battle b){
            Projectiles.clear(b);Abilities.clearBanners(b);clearFixture(b,"clearFixture");
            for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;s.training.clear();if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
            for(var u:b.units)u.mob.discard();b.units.clear();audit(b,"stageFixture",true);b.deaths.clear();
            var base=GravesAndGlory.Battle.base(0);site=new BlockPos((int)base.x+40,140,(int)base.z);force(b,site.add(-13,0,-13),site.add(13,0,13));platform(b,site,12,Blocks.STONE.getDefaultState());
            own=b.squads.get(player);reset(own);
            dawn=new GravesAndGlory.Squad(UUID.randomUUID(),0,base);dawn.order="hold";dawn.bot=true;dusk=new GravesAndGlory.Squad(UUID.randomUUID(),1,base);dusk.order="hold";dusk.bot=true;
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
                    var row=Abilities.row(reaper.spec);boolean sheet=row.id().equals("cleave_reaper")&&row.mechanic().equals("cleave")&&near(row.radius(),2)&&near(row.amount(),0.57);
                    StringBuilder why=new StringBuilder();boolean ok=sheet;
                    var mark=Combat.LAST.peekLast();float h1=first.mob.getHealth(),hn=nearOne.mob.getHealth(),hs=second.mob.getHealth(),hf=far.mob.getHealth();
                    float base=(float)Abilities.baseHit(b,reaper);strike(b,reaper,first,base);
                    var onFirst=Gilded.hitOn(first.mob,mark);var onNear=Gilded.hitOn(nearOne.mob,mark);var onSecond=Gilded.hitOn(second.mob,mark);var onFar=Gilded.hitOn(far.mob,mark);
                    float lf=h1-first.mob.getHealth(),ln=hn-nearOne.mob.getHealth(),ls=hs-second.mob.getHealth(),lfar=hf-far.mob.getHealth();
                    boolean run1=onFirst!=null&&lf>0&&onNear!=null&&Math.abs(onNear.base()-onFirst.base()*0.57)<1e-4&&near(onNear.total(),onFirst.total())&&Math.abs(ln-lf*0.57f)<1e-3&&onSecond==null&&ls==0&&onFar==null&&lfar==0;
                    ok&=run1;why.append("a Grave Reaper's blow (").append(row.id()).append(", radius ").append(row.radius()).append(", share ").append(row.amount()).append(")="+sheet+" hit the first Risen ").append(show(onFirst)).append(", it lost ").append(lf).append("; the Risen 1.5 beside it ").append(show(onNear)).append(", it lost ").append(ln).append(" (want 0.57 of the first); the one 1.8 off lost ").append(ls).append(onSecond==null?" (no hit logged)":" ("+show(onSecond)+")").append(" and the one 3.2 off lost ").append(lfar).append(" (only the nearest other enemy is struck)="+run1+"; ");
                    // run 2: the nearest is gone, the next one is struck; run 3: nobody within 2, nobody struck
                    drop(b,nearOne);mark=Combat.LAST.peekLast();h1=first.mob.getHealth();hs=second.mob.getHealth();hf=far.mob.getHealth();strike(b,reaper,first,base);
                    onFirst=Gilded.hitOn(first.mob,mark);onSecond=Gilded.hitOn(second.mob,mark);onFar=Gilded.hitOn(far.mob,mark);lf=h1-first.mob.getHealth();ls=hs-second.mob.getHealth();lfar=hf-far.mob.getHealth();
                    boolean run2=onFirst!=null&&lf>0&&onSecond!=null&&Math.abs(ls-lf*0.57f)<1e-3&&onFar==null&&lfar==0;ok&=run2;
                    why.append("with the nearest gone the Risen 1.8 off took ").append(ls).append(" of the ").append(lf).append(" the first took (want 0.57 of it) and the one 3.2 off ").append(lfar).append("="+run2+"; ");
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
                case 20->{ // p2-fearless-share: ten Risen of the commander's own army, a mix of five Risen and five Squires, and a mix of four and six; Dusk troops in sight
                    reset(own);group.clear();
                    for(int i=0;i<10;i++)group.add(post(b,own,"revenant",-6+(i%5)*1.5,-5+(i/5)*1.5,-90,true));
                    for(int i=0;i<5;i++){post(b,dawn,"revenant",-6+i*1.5,8,-90,true);post(b,dawn,"knight",-6+i*1.5,9.5,-90,true);}
                    for(int i=0;i<4;i++)post(b,dusk,"revenant",8+i*1.5,-2,90,true);for(int i=0;i<6;i++)post(b,dusk,"knight",8+i*1.5,0,90,true);
                    Houses.recompute(own);Houses.recompute(dawn);Houses.recompute(dusk);
                    var fearless=Abilities.fearlessRow();double ten=Morale.routShareFor(b,own,"infantry"),half=Morale.routShareFor(b,dawn,"infantry"),four=Morale.routShareFor(b,dusk,"infantry"),none=Morale.routShareFor(b,own,"ranged");
                    boolean sheet=fearless!=null&&near(fearless.amount(),0.12)&&near(fearless.amount2(),0.5)&&near(Spec.MORALE[0].routShare(),0.25);
                    moraleOk=sheet&&near(ten,0.12)&&near(half,0.12)&&near(four,0.25)&&near(none,0.25);
                    moraleWhy="the sheet's fearless row ("+(fearless==null?"none":fearless.id()+" "+fearless.amount()+" at "+fearless.amount2())+") and morale share "+Spec.MORALE[0].routShare()+"="+sheet+"; the share a group breaks at: ten Risen "+ten+", five Risen and five Squires "+half+" (want 0.12 for both), four Risen and six Squires "+four+", no troop at all "+none+" (want the sheet's 0.25); ";
                    early=false;weaken(own,"infantry",group,0.15);t0=b.time;next(b,21);
                }
                case 21->{ // ten Risen at 15% do not break in 45 ticks; at 10% they do
                    if(Morale.routing(own,"infantry"))early=true;
                    if(since(b)<45)return;
                    heldAt15=!early&&near(BattleMath.strength(group.stream().mapToDouble(u->u.mob.getHealth()).sum(),own.fullHealth.get("infantry")),0.15);
                    weaken(own,"infantry",group,0.10);t0=b.time;routedAfter=-1;next(b,22);
                }
                case 22->{
                    if(Morale.routing(own,"infantry")){routedAfter=b.time-t0;}
                    if(routedAfter<0&&since(b)<=60)return;
                    routedAt10=routedAfter>=0;
                    moraleWhy+="ten Risen at 15% strength did not rout in 45 ticks="+heldAt15+" and routed at 10% "+(routedAfter<0?"never":routedAfter+" ticks later")+"="+routedAt10+"; ";
                    dropAll(b,group,"fearless");reset(own);Houses.recompute(own);
                    for(int i=0;i<10;i++)group.add(post(b,own,"knight",-6+(i%5)*1.5,-5+(i/5)*1.5,-90,true));
                    Houses.recompute(own);weaken(own,"infantry",group,0.15);t0=b.time;routedAfter=-1;next(b,23);
                }
                case 23->{ // a knight group at 15% breaks
                    if(Morale.routing(own,"infantry"))routedAfter=b.time-t0;
                    if(routedAfter<0&&since(b)<=60)return;
                    boolean knights=routedAfter>=0&&near(Morale.routShareFor(b,own,"infantry"),0.25);
                    moraleWhy+="a group of ten Order Squires at 15% routed "+(routedAfter<0?"never":routedAfter+" ticks later")+"="+knights;
                    report("p2-fearless-share",moraleOk&&heldAt15&&routedAt10&&knights,moraleWhy);
                    clearFixture(b,"fearless");reset(own);next(b,30);
                }
                case 30->{ // p2-dread: four Order Squires of the own army at 35% strength, a Dusk Risen in sight and the Hollow King 24 blocks away
                    reset(own);group.clear();
                    for(int i=0;i<4;i++)group.add(post(b,own,"knight",-6+i*1.5,0,-90,true));
                    post(b,dusk,"revenant",6,3,90,true);hk=post(b,dusk,"gb_hollowking",20,0,90,true);Houses.recompute(own);
                    var far=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");
                    boolean sheet=Abilities.dreadRow(hk)!=null&&near(Abilities.dreadRow(hk).amount(),0.15)&&near(Abilities.dreadRow(hk).amount2(),0.4)&&near(Abilities.dreadRow(hk).radius(),10);
                    moraleOk=sheet&&Gilded.auras(far,1.0)&&near(Morale.routShareFor(b,own,"infantry"),0.25);
                    moraleWhy="the Hollow King's ability "+hk.spec.ability()+" ("+(Abilities.dreadRow(hk)==null?"not a dread row":"amount "+Abilities.dreadRow(hk).amount()+", rout "+Abilities.dreadRow(hk).amount2()+", radius "+Abilities.dreadRow(hk).radius())+")="+sheet+"; with him 24 blocks off a Squire's blow has auras "+Gilded.au(far)+" and the group breaks at "+fmt(Morale.routShareFor(b,own,"infantry"))+" (want 1.0 and 0.25); ";
                    early=false;weaken(own,"infantry",group,0.35);t0=b.time;next(b,31);
                }
                case 31->{ // 35% without the Hollow King: no rout in 45 ticks; then he steps to 9.25 from their centre
                    if(Morale.routing(own,"infantry"))early=true;
                    if(since(b)<45)return;
                    heldAt15=!early;moraleWhy+="four Squires at 35% with no Hollow King within 10 did not rout in 45 ticks="+heldAt15+"; ";
                    move(hk,5.5,0,90);
                    // his dread on every blow of the enemy's troops that stands within 10 of him; the largest of several, and a banner's share multiplies in
                    var inReach=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");var outReach=Commanders.hit(b,group.get(0),fixture.get(4).mob,"melee");var his=Commanders.hit(b,fixture.get(4),group.get(3).mob,"melee");
                    var second=post(b,dusk,"gb_hollowking",6,-3,90,true);var two=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");drop(b,second);
                    var banner=new Abilities.Banner(own,group.get(3).mob.getPos(),b.time+300,12,0.1,null,null);b.banners.add(banner);var withBanner=Commanders.hit(b,group.get(3),fixture.get(4).mob,"melee");b.banners.remove(banner);
                    boolean auras=Gilded.auras(inReach,0.85)&&Gilded.auras(outReach,1.0)&&Gilded.auras(his,1.0)&&Gilded.auras(two,0.85)&&Math.abs(withBanner.auras()-1.1*0.85)<1e-9;
                    boolean share=near(Morale.routShareFor(b,own,"infantry"),0.4);
                    moraleOk&=auras&&share;
                    moraleWhy+="with the Hollow King at 7.0 from one Squire and 11.5 from another: a blow by the near one has auras "+Gilded.au(inReach)+" (want 0.85), by the far one "+Gilded.au(outReach)+" (1.0), by a Dusk Risen beside the King "+Gilded.au(his)+" (his own side, 1.0), with a second King in reach "+Gilded.au(two)+" (not stacked, 0.85), with an Oath Banner too "+(withBanner==null?"none":String.valueOf(withBanner.auras()))+" (1.1 x 0.85)="+auras+"; the group's share is now "+fmt(Morale.routShareFor(b,own,"infantry"))+" (want 0.40)="+share+"; ";
                    weaken(own,"infantry",group,0.35);routedAfter=-1;t0=b.time;next(b,32);
                }
                case 32->{ // the Squires at 35% break within the dread; then ten Risen at 35% in the same place do not
                    if(Morale.routing(own,"infantry"))routedAfter=b.time-t0;
                    if(routedAfter<0&&since(b)<=60)return;
                    routedAt10=routedAfter>=0;moraleWhy+="the same Squires with him 9.25 from their centre routed "+(routedAfter<0?"never":routedAfter+" ticks later")+"="+routedAt10+"; ";
                    dropAll(b,group,"dread");reset(own);Houses.recompute(own);
                    for(int i=0;i<10;i++)group.add(post(b,own,"revenant",-6+(i%5)*1.5,-1+(i/5)*1.5,-90,true));
                    Houses.recompute(own);weaken(own,"infantry",group,0.35);early=false;t0=b.time;next(b,33);
                }
                case 33->{ // fearless wins over dread
                    if(Morale.routing(own,"infantry"))early=true;
                    if(since(b)<45)return;
                    boolean wins=!early&&near(Morale.routShareFor(b,own,"infantry"),0.12);
                    moraleWhy+="ten Risen at 35% beside him did not rout in 45 ticks and break at "+fmt(Morale.routShareFor(b,own,"infantry"))+" (fearless wins, 0.12)="+wins;
                    report("p2-dread",moraleOk&&heldAt15&&routedAt10&&wins,moraleWhy);
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
                    boolean sheet=trait!=null&&trait.mechanic().equals("rise_again")&&near(trait.value(),0.11)&&near(trait.value2(),0.3)&&trait.durationTicks()==20&&dusk.traits.getOrDefault("graveborn",0)==1;
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
                    double[] want={0.11,0.28,0.45};
                    for(int tier=1;tier<=3;tier++){
                        var t=CounterMath.trait(Spec.HOUSETRAITS,"graveborn",tier);int n=20000,hits=0;for(int i=0;i<n;i++)if(Houses.rolls(b,t))hits++;
                        double share=hits/(double)n;boolean row=t!=null&&t.mechanic().equals("rise_again")&&near(t.value(),want[tier-1])&&near(t.value2(),0.3)&&t.durationTicks()==20;boolean good=row&&Math.abs(share-want[tier-1])<0.02;ok&=good;
                        why.append("tier ").append(tier).append(": row chance ").append(t==null?"none":t.value()).append(", ").append(hits).append(" of ").append(n).append(" rolls rose (").append(fmt(share*100)).append("%, want ").append(fmt(want[tier-1]*100)).append("%)").append(good?"":" (BAD)").append("; ");
                    }
                    var one=post(b,dusk,"revenant",0,0,0,true);var src=b.world.getDamageSources().generic();int rises=0;dusk.traits.put("graveborn",3);
                    for(int i=0;i<800;i++){one.rose=false;one.stunnedUntil=0;one.mob.setHealth(1f);if(!Houses.allowDeath(one.mob,src))rises++;}
                    dusk.traits.clear();int untiered=0;for(int i=0;i<800;i++){one.rose=false;one.mob.setHealth(1f);if(!Houses.allowDeath(one.mob,src))untiered++;}
                    boolean hook=Math.abs(rises/800.0-0.4)<0.08&&untiered==0;ok&=hook;Houses.recompute(dusk);
                    why.append("through the hook at tier III ").append(rises).append(" of 800 falls rose (want about 320); with no tier ").append(untiered).append(" of 800 did (want 0)=").append(hook);
                    report("p2-rise-chance",ok,why.toString());clearFixture(b,"rise-chance");Houses.forceRise=Boolean.FALSE;next(b,50);
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
                    Houses.forceRise=Boolean.TRUE;var lucky=group.get(0);lucky.mob.setHealth(1f);strike(b,scout,lucky,500f);boolean rose=lucky.mob.isAlive()&&lucky.rose&&!lucky.temporary;
                    Houses.forceRise=Boolean.FALSE;var lost=group.get(1);lost.mob.setHealth(1f);strike(b,scout,lost,500f);
                    var spent=temps.isEmpty()?null:temps.get(0);if(spent!=null)fall(b,spent);
                    int standing=(int)b.units.stream().filter(u->u.temporary&&u.mob.isAlive()).count();
                    var row=own.stats.lost;var report=b.campaign.reportSquads(own);int reported=-1;for(var e:report)if(e.getAsJsonObject().get("you").getAsBoolean())reported=e.getAsJsonObject().get("lost").getAsInt();
                    boolean before=temps.size()==2&&rose&&own.stats.lost==1&&reported==1&&standing==1&&!b.deaths.isEmpty()&&b.deaths.stream().noneMatch(d->d.team()==own.team&&d.troop().equals("revenant")&&d.time()!=b.time&&false);
                    audit(b,"cleanup setup",false);
                    // the round ends
                    List<GravesAndGlory.Unit> everyone=new ArrayList<>(b.units);
                    b.campaign.finish(-1);
                    var result=b.campaign.results.get(player);int lostInReport=-1;if(result!=null&&result.has("squads"))for(var e:result.getAsJsonArray("squads"))if(e.getAsJsonObject().get("you").getAsBoolean())lostInReport=e.getAsJsonObject().get("lost").getAsInt();
                    boolean allGone=!b.running&&b.units.isEmpty()&&temporaries(b)==0&&everyone.stream().allMatch(u->u.mob.isRemoved())&&b.deaths.isEmpty();
                    audit(b,"after finish",true);boolean noTeams=sideMembers(b).isEmpty();
                    report("p2-bloom-cleanup",before&&allGone&&noTeams&&lostInReport==1,"before the round ends: two temporary Risen were raised ("+temps.size()+"), a Risen of his own rose="+rose+", one real loss and one temporary one fell: the army's lost is "+own.stats.lost+" and the report's row says "+reported+" (want 1), "+standing+" temporary troop still stands (want 1), the death log holds "+b.deaths.size()+" (the real loss)="+before+"; after campaign.finish(-1): "+everyone.size()+" troops were in the battle and now the battle holds "+b.units.size()+" ("+temporaries(b)+" temporary), every one of their entities is removed="+everyone.stream().allMatch(u->u.mob.isRemoved())+", the death log holds "+b.deaths.size()+", the round is over="+!b.running+", the final report says lost "+lostInReport+" (want 1), the four gg_ teams hold "+sideMembers(b).size()+" members="+noTeams);
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

    static final class TickCost {
        final GravesAndGlory.Battle b;final List<String> banners;GravesAndGlory.Squad own;boolean begun;long start;double sum,avg;int samples,troops;float last;String note="";
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
                troops=b.units.size();begun=true;start=b.time;note=armies.size()+" armies around "+centre.id()+" under "+banners;
                System.out.println("[p2] tick-cost fixture: "+armies.size()+" armies, "+troops+" troops around "+centre.id()+" under "+banners);
                return false;
            }
            sum+=b.server.getTickTime();samples++;
            if(b.time-start<600)return false;
            avg=sum/samples;last=b.server.getTickTime();System.out.println("[p2] tick-cost avg="+String.format(Locale.ROOT,"%.2f",avg));return true;
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
            for(var p:pins)if(p.mob().isAlive())pin(p.mob(),p.at(),p.yaw());
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
     * the camera and the matrices the world was drawn with, the entity's pose as drawn, the renderer's record of that draw, and
     * whether the vanilla renderer's frustum test would have drawn the entity in that frame.
     */
    static final class Frame {
        static final List<Frame> ALL=new java.util.concurrent.CopyOnWriteArrayList<>();
        final int id;final boolean fresh;final java.util.function.Predicate<Entity> when;volatile boolean done;int before;Set<Entity> known=Set.of();
        NativeImage image;Matrix4f proj,view;Vec3d cam,pos;float bodyYaw;int deathTime;TroopRenderer.Draw draw;boolean vanillaSees;Entity entity;
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
            draw=d.last;
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
                case 0->{if(ready&&posted.size()==4&&entities(c)!=null&&lit(c)){c.options.hudHidden=true;wait=0;cs=1;}
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
                        if(w>TEAM_LATE&&(spawnFrame.done||w>=TEAM_LATE+70)){spawnCheck(c);cs=3;wait=0;}}}
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
            // Every rig, weapon and troop that index.json lists has loaded: a client whose loader never ran logs nothing either.
            List<String> missing=new ArrayList<>();String listed;
            try{var ix=TroopModelLoader.index(TroopModelLoader.read(c.getResourceManager(),"index.json"));
                for(String r:ix.rigs())if(TroopModels.rig(r)==null)missing.add("rig/"+r);
                for(String w:ix.weapons())if(TroopModels.weapon(w)==null)missing.add("weapon/"+w);
                for(String t:ix.troops())if(TroopModels.model(t)==null)missing.add(t);
                if(ix.troops().isEmpty())missing.add("(index.json lists no troop)");
                listed=ix.rigs().size()+" rigs, "+ix.weapons().size()+" weapons and "+ix.troops().size()+" troops";}
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
                case 0->{if(ready&&posted.size()==12&&posted.stream().allMatch(u->{var e=client(c,u.mob);return e!=null&&e.getCustomName()!=null;})&&lit(c)){c.options.hudHidden=true;wait=0;cs=1;}
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
                case 6->{wait++; // one troop on all four sides (finding 11): four Squires in the studio, gold, violet, crimson and teal from the left, 4 blocks off
                    if(wait==1)server(c,b->{var board=b.server.getScoreboard();
                        for(int i=0;i<4;i++){var u=post(b,i==1?violet:gold,"knight",(1.5-i)*1.6,STUDIO,180+20);sided.add(u);
                            if(i>=2){String team="gg_"+FOUR.get(i);var t=board.getTeam(team);if(t==null)t=board.addTeam(team);board.addPlayerToTeam(u.mob.getEntityName(),t);
                                for(var f:Spec.FACTIONS)if(f.id().equals(FOUR.get(i)))u.mob.equipStack(EquipmentSlot.OFFHAND,GravesAndGlory.Battle.shield(u.spec,f.dye()));}}
                        camera(b,0,1.45,STUDIO-4,0,6);});
                    if(wait==50){if(sided.size()==4&&shotReady(c,"gilded-sides")){sidesCheck(c);shot(c,"gilded-sides");}else wait--;}
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
        /** p2-gilded-sides: the four Squires were last drawn each in its own side's cloth, with its flag. */
        void sidesCheck(MinecraftClient c){
            var m=TroopModels.model("knight");boolean ok=m!=null;StringBuilder why=new StringBuilder();
            for(int i=0;i<4;i++){var e=client(c,sided.get(i).mob);var d=e==null?null:TroopRenderer.drawn(e);var last=d==null?null:d.last;
                List<String> shown=last==null||m==null?List.of():m.shownBones(last.bones());
                boolean good=last!=null&&FOUR.get(i).equals(last.look().side())&&FOUR.get(i).equals(last.cloth())&&shown.contains("pennant")&&shown.contains("chest_panel");ok&=good;
                why.append(FOUR.get(i)).append(": ").append(last==null?"not drawn":"drawn in "+last.cloth()+", flag "+(shown.contains("pennant")?"shown":"hidden")).append(good?"":" (BAD)").append("; ");}
            report("p2-gilded-sides",ok,"one Squire on each side, left to right on the frame: "+why.toString().trim());
        }
        /** One line of p2-gilded-stages: each of its three troops was last drawn at its own stage, in that stage's skin, with the line's stage 3 parts at stage 3 only. */
        void stagesCheck(MinecraftClient c,String line){
            var m=TroopModels.model(line);List<String> parts=STAGE3.get(line);boolean ok=m!=null&&m.bones().containsAll(parts);
            stagesWhy.append(line).append(m!=null&&m.bones().containsAll(parts)?"":" (BAD: the model lacks "+parts+")").append(": ");
            for(var u:lines.get(line)){var e=client(c,u.mob);var d=e==null?null:TroopRenderer.drawn(e);var l=d==null?null:d.last.look();
                if(l==null||m==null){ok=false;stagesWhy.append("stage ").append(u.stage).append(" not drawn (BAD); ");continue;}
                List<String> shown=m.shownBones(d.last.bones());long on=parts.stream().filter(shown::contains).count();
                var skin=TroopModels.skin(l);boolean skinOk=skin!=null&&skin.getPath().endsWith("/"+line+"_s"+u.stage+".png")&&c.getResourceManager().getResource(skin).isPresent();
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
}
