package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.server.network.ServerPlayerEntity;
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
    static{SUITES.put("banners",Banners::new);}
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
        /** Gold the commander's army spent since mark(): battle income (passive gold every economy.incomeSeconds) is not spending. */
        int spent(){return goldBefore+(own.stats.goldEarned-earnedBefore)-own.gold;}
        void mark(){goldBefore=own.gold;earnedBefore=own.stats.goldEarned;}

        void next(GravesAndGlory.Battle b,int s){stage=s;stageAt=b.time;}
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
            if(stage==14){ // the barracks lists the ids of the army's own banner, once each
                if(!rosterDone&&c.currentScreen instanceof RecruitmentScreen r){rosterDone=true;
                    List<String> ids=new ArrayList<>();for(var row:r.controls)ids.add(Spec.TROOPS[row.index()].id());
                    report("p2-barracks-roster",ids.equals(List.of("knight","archer")),"barracks lists "+ids+" for the banner knight x12, archer x8 (want knight, archer)");
                    requestShot("barracks-banner");
                }else if(!rosterDone&&++screenWait>200){rosterDone=true;report("p2-barracks-roster",false,"the barracks screen never opened");requestShot("barracks-banner");}
                if(rosterDone&&wantShot!=null&&shotTaken){wantShot=null;c.setScreen(null);server(c,b->next(b,15));}
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
                case 13->{if(since(b)>=20){boolean refused=own.training.isEmpty()&&spent()==0;trainOk&=refused;trainWhy+="; train:sentinel (not in the banner) refused="+refused+" (queued "+own.training.size()+", spent "+spent()+")";command(b,"barracks",14);}}
                case 15->command(b,"disband:knight",16);
                case 16->{if(since(b)>=20){long knights=alive(b,own,"knight");trainWhy+="; after disband:knight "+knights+" alive";mark();command(b,"train:knight",17);}}
                case 17->{if(since(b)>=20){var knight=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals("knight")).findFirst().orElseThrow();
                    boolean taken=own.training.size()==1&&own.training.getFirst().spec()==knight&&spent()==knight.cost();trainOk&=taken;
                    trainWhy+="; then train:knight accepted="+taken+" (queued "+own.training.size()+", spent "+spent()+")";mark();command(b,"train:knight",18);}}
                case 18->{if(since(b)>=20){boolean refused=own.training.size()==1&&spent()==0;trainOk&=refused;trainWhy+="; a second train:knight (11 alive + 1 queued) refused="+refused+" (queued "+own.training.size()+", spent "+spent()+")";
                    report("p2-train-limit",trainOk,trainWhy);b.campaign.returnLobby();next(b,99);}}
                default->{}
            }
        }
    }
}
