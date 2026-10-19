package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
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
    static{SUITES.put("banners",Banners::new);SUITES.put("engine",Engine::new);SUITES.put("spike",Spike::new);SUITES.put("gilded-art",GildedArt::new);}
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
     * cost of eight armies (152 troops). Stages run on the server tick; the client sends the command, opens the screens and takes the screenshots the stages ask for.
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
            server(c,b->{clearFixture();unforce(b);if(cost!=null)cost.handBack();if(rngSaved!=null)b.campaign.rollRng=rngSaved;if(b.running)b.campaign.returnLobby();});
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
                case 60->{ // p2-tick-cost: eight bot armies, 152 troops (19 each)
                    var p=sp(b);if(p==null)return;
                    if(cost==null)cost=tickCost(b,List.of("gilded_normal","gilded_normal","gilded_normal","gilded_normal","graveborn_normal","graveborn_normal","graveborn_normal","graveborn_normal"));
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
     * 152 troops under the normal bot banners, 19 each) fight around the centre point for 600 ticks, each army under the next banner of the list (Dawn's armies
     * first; a short list repeats), and the server's tick time is read each tick. Call step on every server tick until it returns true.
     */
    static TickCost tickCost(GravesAndGlory.Battle b,List<String> banners){return new TickCost(b,banners);}
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
        /** Client: sky light 15 on this client where the troops stand (lineup, studio and both wings of the floor) and the terrain
         *  rebuilt, so a shot shows the noon platform and not an unlit one whose light updates are still on their way. */
        boolean lit(MinecraftClient c){
            if(c.world==null||c.worldRenderer==null||site==null)return false;
            for(int[] d:new int[][]{{0,0},{-6,0},{6,0},{0,-3},{0,6},{-6,12},{0,12},{6,12}})if(c.world.getLightLevel(net.minecraft.world.LightType.SKY,site.add(d[0],0,d[1]))<15)return false;
            return c.worldRenderer.isTerrainRenderComplete();
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
        public void cleanup(MinecraftClient c){
            c.options.hudHidden=false;
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
     * Task 5 Step 7: the troop model spike. Two Order Squires and two Templars, one of each per team (gold against violet), 3
     * blocks apart in front of the camera at noon, and an unnamed husk beside them.
     */
    static final class Spike extends Stage {
        MobEntity husk;long drawsBefore;
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
                case 0->{if(ready&&posted.size()==4&&entities(c)!=null&&lit(c)){c.options.hudHidden=true;wait=0;cs=1;}}
                case 1->{wait++;if(wait==40)drawsBefore=TroopRenderer.troopDraws;if(wait==60){checks(c);cs=2;wait=0;}}
                case 2->{wait++;
                    if(wait==1)shot(c,"spike-name-skin");
                    if(wait==5)server(c,b->camera(b,-3,1.8,2.2,180,6));
                    if(wait==45)shot(c,"spike-name-model");
                    if(wait==50)server(c,b->{for(var u:posted)move(u.mob,u.mob.getX()-site.getX()-.5,0,210);camera(b,0,1.9,6,180,8);});
                    if(wait==95){shot(c,"spike-team-pennant");cs=3;wait=0;}}
                case 3->{wait++;
                    if(wait==1)portrait(c);
                    if(wait==30){shot(c,"spike-portrait");}
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
                boolean good=r instanceof TroopRenderer<?>&&l!=null&&TroopModels.model(l.troop().id())!=null;renderer&=good;
                rw.append(e.getCustomName().getString()).append(" -> ").append(r.getClass().getSimpleName()).append(l==null?", no look":", model "+l.troop().id()).append(good?"":" (BAD)").append("; ");
                boolean n=e.getCustomName().getString().equals(names[i])&&l!=null&&l.troop().id().equals(troops[i])&&l.stage()==1&&!l.veteran()&&sides[i].equals(l.side());named&=n;
                nw.append("\"").append(e.getCustomName().getString()).append("\" -> ").append(l==null?"null":l.troop().id()+" stage "+l.stage()+" side "+l.side()).append(n?"":" (want "+names[i]+", "+troops[i]+" stage 1 side "+sides[i]+")").append("; ");}
            var h=es.get(4);var hr=dispatcher.getRenderer(h);boolean huskOk=hr instanceof TroopRenderer<?> tr&&TroopModels.resolve(h)==null&&tr.vanilla() instanceof net.minecraft.client.render.entity.HuskEntityRenderer;
            long draws=TroopRenderer.troopDraws-drawsBefore;renderer&=huskOk&&draws>=60;
            report("p2-spike-renderer",renderer,rw+"the unnamed husk: "+hr.getClass().getSimpleName()+", resolve null and vanilla() a HuskEntityRenderer="+huskOk+"; "+draws+" draws through a troop's own model in the last 20 ticks (want 60 or more)");
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
            report("p2-spike-load",bad.isEmpty()&&lines>0,lines+" lines of latest.log read; "+(bad.isEmpty()?"no [troop-models] line and no Failed to load model naming gravesandglory:troop/":bad.size()+" bad, first: "+bad.get(0))+"; models loaded: "+TroopModels.ids()+", rigs "+TroopModels.RIGS.values().stream().distinct().filter(r->TroopModels.rig(r)!=null).toList());
        }
        void portrait(MinecraftClient c){
            var spec=ArmyCards.spec("sentinel");var e=ArmyCards.portrait(spec,1);var l=e==null?null:TroopModels.resolve(e);
            boolean look=l!=null&&l.troop()==spec&&l.stage()==1&&l.side()==null&&e.getCustomName().getString().equals("Templar")&&e.getScoreboardTeam()==null;
            String cloth=l==null?"(none)":TroopModels.clothOf(e,l);var tex=TroopModels.cloth("company".equals(cloth)?cloth:null);
            boolean company="company".equals(cloth)&&c.getResourceManager().getResource(tex).isPresent();
            var m=TroopModels.model("sentinel");boolean noPennant=false;if(m!=null&&l!=null){m.prepare(l,false,false);noPennant=!m.teamShown("pennant")&&m.teamShown("long_tabard");}
            var tag=net.minecraft.item.BlockItem.getBlockEntityNbt(e.getOffHandStack());
            boolean shield=tag!=null&&tag.getInt("Base")==net.minecraft.util.DyeColor.LIGHT_GRAY.getId()&&tag.getList("Patterns",10).size()==1&&tag.getList("Patterns",10).getCompound(0).getString("Pattern").equals("sc");
            report("p2-spike-portrait",look&&company&&noPennant&&shield,"ArmyCards.portrait(sentinel, 1): named \""+(e==null?"":e.getCustomName().getString())+"\", resolves "+(l==null?"null":l.troop().id()+" stage "+l.stage()+" side "+l.side())+", no scoreboard team="+look
                +"; cloth "+cloth+" ("+tex+" present="+company+"; Kort's portrait ruling replaced linen with the company's navy); the pennant hidden and the tabard drawn="+noPennant+"; shield "+(tag==null?"none":tag)+" (light-grey field, white saltire)="+shield);
            c.setScreen(new PortraitProbe(List.of(ArmyCards.portrait(ArmyCards.spec("knight"),1),e)));
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
     * troop alone in the studio (4 blocks, turned 30 degrees), the knight's three stages, the kit (weapons, glow, shields)
     * and the recruit page's Gilded tab.
     */
    static final class GildedArt extends Stage {
        static final List<String> SIX=List.of("knight","archer","sentinel","longbow","mage","gl_marshal");
        final Map<String,GravesAndGlory.Unit> golds=new LinkedHashMap<>(),violets=new LinkedHashMap<>();
        final List<GravesAndGlory.Unit> stages=new ArrayList<>();int ci;
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
                case 0->{if(ready&&posted.size()==12&&posted.stream().allMatch(u->{var e=client(c,u.mob);return e!=null&&e.getCustomName()!=null;})&&lit(c)){c.options.hudHidden=true;wait=0;cs=1;}}
                case 1->{wait++;if(wait==60){lineup(c);shot(c,"gilded-lineup");cs=2;wait=0;ci=0;}}
                case 2->{wait++; // one gold troop at a time in the studio, then the knight at stage 3
                    String troop=ci<6?SIX.get(ci):"knight";
                    if(wait==1){final int k=ci;server(c,b->{var u=golds.get(troop);move(u.mob,0,STUDIO,180+30);if(k==6)stage(u,3);camera(b,0,1.45,STUDIO-4,0,6);});}
                    if(wait==45){shot(c,ci<6?"gilded-"+troop:"gilded-knight-s3");}
                    if(wait==50){final int k=ci;server(c,b->{var u=golds.get(troop);if(k==6)stage(u,1);move(u.mob,-6.25+SIX.indexOf(troop)*2.5,0,0);});ci++;wait=0;if(ci>6){cs=3;}}}
                case 3->{wait++; // the knight's stages side by side in the studio, stage 1 on the left (the camera looks south: +x is its left)
                    if(wait==1)server(c,b->{for(int s=1;s<=3;s++){var u=post(b,gold,"knight",(2-s)*2.0,STUDIO,180+20);stage(u,s);stages.add(u);}camera(b,0,1.6,STUDIO-5,0,8);});
                    if(wait==50){stagesCheck(c);shot(c,"gilded-stages");}
                    if(wait==55){kit(c);cs=4;wait=0;}}
                case 4->{wait++; // the recruit page's Gilded tab, every Gilded troop owned so no card is dimmed
                    if(wait==1)server(c,b->{b.campaign.returnLobby();var pr=b.campaign.profile(player);for(String id:SIX)pr.owned.putIfAbsent(id,new Profile.Owned());pins.clear();posted.clear();});
                    if(wait==40){CampaignClient.revealUntil=0;CampaignClient.notice="";c.setScreen(new CampScreen("recruit"));}
                    if(wait==45&&c.currentScreen instanceof CampScreen s)s.pickHouse("gilded");
                    if(wait==80){boolean six=c.currentScreen instanceof CampScreen s&&s.house.equals("gilded")&&CampScreen.cards("gilded",CampaignClient.profile).stream().allMatch(CampScreen.Card::owned);
                        boolean models=SIX.stream().allMatch(id->{var e=ArmyCards.portrait(ArmyCards.spec(id));var l=e==null?null:TroopModels.resolve(e);return l!=null&&l.side()==null&&"company".equals(TroopModels.clothOf(e,l));});
                        report("p2-gilded-cards",six&&models,"the recruit page on the Gilded tab with all six owned="+six+"; every card's portrait resolves to its own model, no side, in the company's cloth="+models);shot(c,"gilded-cards");}
                    if(wait==90){c.setScreen(null);serverDone=true;end(c);}}
                default->{}
            }
        }
        void lineup(MinecraftClient c){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(var u:posted){var e=client(c,u.mob);var l=e==null?null:TroopModels.resolve(e);boolean good=l!=null&&l.troop().id().equals(u.spec.id())&&l.stage()==1&&TroopModels.model(u.spec.id())!=null&&l.side()!=null;
                ok&=good;why.append(u.spec.id()).append("/").append(l==null?"null":l.side()).append(good?"":" (BAD)").append(" ");}
            report("p2-gilded-lineup",ok,"each Gilded troop resolves to its own model at stage 1 on its side: "+why.toString().trim());
        }
        void stagesCheck(MinecraftClient c){
            var m=TroopModels.model("knight");boolean ok=m!=null&&m.bones().contains("crest_plume");StringBuilder why=new StringBuilder();
            for(var u:stages){var e=client(c,u.mob);var l=e==null?null:TroopModels.resolve(e);if(l==null||m==null){ok=false;why.append("no look; ");continue;}
                m.prepare(l,false,false);boolean plume=m.bone("crest_plume")!=null&&m.bone("crest_plume").visible;boolean want=l.stage()==3;ok&=plume==want&&l.stage()==u.stage;
                why.append("\"").append(e.getCustomName().getString()).append("\" stage ").append(l.stage()).append(" plume ").append(plume?"shown":"hidden").append(plume==want?"":" (BAD)").append("; ");}
            report("p2-gilded-stages",ok,why.toString());
        }
        void kit(MinecraftClient c){
            boolean ok=true;StringBuilder why=new StringBuilder();
            for(String id:SIX){var m=TroopModels.model(id);var w=m==null?null:TroopModels.weapon(m.weaponId());boolean good=w!=null&&c.getResourceManager().getResource(w.texture()).isPresent();ok&=good;why.append(id).append(" weapon ").append(m==null?"no model":m.weaponId()).append(good?"":" (BAD)").append("; ");}
            var mage=client(c,golds.get("mage").mob);var ml=mage==null?null:TroopModels.resolve(mage);var glow=ml==null?null:TroopModels.glow(ml);
            boolean lit=glow!=null&&c.getResourceManager().getResource(glow).isPresent();ok&=lit;why.append("the Sunweaver's glow ").append(glow).append(lit?"":" (BAD)").append("; ");
            final boolean client=ok;final String clientWhy=why.toString();
            server(c,b->{boolean shields=true;StringBuilder sw=new StringBuilder();
                for(var u:posted){if(!List.of("knight","sentinel","gl_marshal").contains(u.spec.id())||stages.contains(u))continue;
                    var tag=net.minecraft.item.BlockItem.getBlockEntityNbt(u.mob.getOffHandStack());String dye=b.side(u.squad.team).dye();
                    StringBuilder want=new StringBuilder();for(String layer:u.spec.shield().split(","))want.append(layer).append(",");
                    StringBuilder got=new StringBuilder();if(tag!=null){var list=tag.getList("Patterns",10);for(int i=0;i<list.size();i++){var p=list.getCompound(i);got.append(net.minecraft.util.DyeColor.byId(p.getInt("Color")).getName()).append(":").append(p.getString("Pattern")).append(",");}}
                    boolean good=u.mob.getOffHandStack().isOf(net.minecraft.item.Items.SHIELD)&&tag!=null&&tag.getInt("Base")==net.minecraft.util.DyeColor.byName(dye,null).getId()&&got.toString().equals(want.toString());shields&=good;
                    sw.append(u.spec.id()).append(" ").append(b.side(u.squad.team).id()).append(": base ").append(tag==null?"none":net.minecraft.util.DyeColor.byId(tag.getInt("Base")).getName()).append(" (want ").append(dye).append("), patterns ").append(got).append(" (want ").append(want).append(")").append(good?"":" (BAD)").append("; ");}
                report("p2-gilded-kit",client&&shields,clientWhy+sw);});
        }
    }
}
