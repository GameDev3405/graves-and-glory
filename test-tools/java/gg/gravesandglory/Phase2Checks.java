package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.block.Blocks;
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
    static{SUITES.put("banners",Banners::new);SUITES.put("engine",Engine::new);SUITES.put("commander",Commanders::new);}
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
            for(var kind:List.of(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR,net.minecraft.entity.attribute.EntityAttributes.GENERIC_ARMOR_TOUGHNESS)){
                var a=target.getAttributeInstance(kind);if(a==null)continue;for(var m:new ArrayList<>(a.getModifiers())){a.removeModifier(m.getId());aside.add(Map.entry(a,m));}
            }
            target.clearActiveItem();target.setHealth(target.getMaxHealth());target.timeUntilRegen=0;target.hurtTime=0;
            float before=target.getHealth();int armour=target.getArmor();reissued.clear();watched=target;
            try{target.damage(b.world.getDamageSources().playerAttack(p),10f);}finally{watched=null;}
            float lost=before-target.getHealth();var amounts=List.copyOf(reissued);
            for(var e:aside)e.getKey().addTemporaryModifier(e.getValue());
            target.setHealth(target.getMaxHealth());target.setVelocity(Vec3d.ZERO);target.timeUntilRegen=0;
            return new Blow(what,lost,amounts,armour);
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
            own.abilityUntil=0;own.abilityData=null;var plain=strike(b,p,foe.mob,"a foe troop");
            own.abilityUntil=b.time+100;own.abilityData=foe.mob;var hunted=strike(b,p,foe.mob,"that troop under his Mark Prey");
            own.abilityUntil=0;own.abilityData=null;
            ok&=plain.is(10*scale,10*scale)&&hunted.is(10*scale*mark,10*scale*mark);got.add(plain);got.add(hunted);
            if(keeper!=null){
                var enemy=foe.squad;var other=unit(b,enemy,u->u!=foe);var troop=other==null?foe:other;Vec3d at=troop.mob.getPos();
                var them=new Target(b.world,new com.mojang.authlib.GameProfile(enemy.owner,"FoeCommander"));vulnerable(them);
                var players=playerMap(b.server.getPlayerManager());
                boolean wasBot=enemy.bot;String wasClass=enemy.commander;long wasUntil=enemy.abilityUntil;Object wasData=enemy.abilityData;
                players.put(them.getUuid(),them);
                try{
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
                }finally{
                    players.remove(them.getUuid());own.abilityUntil=0;own.abilityData=null;
                    enemy.bot=wasBot;enemy.commander=wasClass;enemy.abilityUntil=wasUntil;enemy.abilityData=wasData;
                }
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
}
