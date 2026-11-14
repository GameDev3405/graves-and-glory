package gg.gravesandglory;

import com.google.gson.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import java.nio.file.*;
import java.util.*;

/** Lobby, permanent collections, team admission, rewards and bot commanders. */
public final class Campaign {
    static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    /** The client reads the camp snapshot with readString(SNAPSHOT_LIMIT) and a longer string drops its connection, so fitted() never returns one. WIRE is the compact form it falls back to (MatchEnding.fit). */
    static final int SNAPSHOT_LIMIT=16000;static final Gson WIRE=new Gson();
    final GravesAndGlory.Battle b;
    final Map<UUID,Profile> profiles=new HashMap<>();
    final Path directory;
    SocialHub social=new SocialHub();String activeRoom="",preparingRoom="";
    int size=2,map=0;boolean practice=false,fill=true;
    /** The mode the next round plays (Phase 4 Task 2): a lobby setting kept across rounds like the map, reset to Domination when a new map does not host it. */
    String battleMode=Modes.DOMINATION;
    /** The battleMode a room's round replaced (null outside a room's round); returnLobby puts it back, so a later skirmish or practice plays its own mode. */
    String battleModeBeforeRoom;
    long introUntil;int serial;String matchId=UUID.randomUUID().toString();
    /** The match ending (spec 2026-11-12): the tick it began (-1: none), the winner, its step timing, each participant's skipped-to step, who has been moved to the stage
     *  and who has gone back to camp, and the final score line. The round stays running (seats, rejoin) while it plays; the battle tick is gated off. */
    long endingStart=-1;int endingWinner;MatchEnding.Timing endingTiming;String endingLine="";
    final Map<UUID,MatchEnding.Step> skipped=new HashMap<>();final Set<UUID> endingStaged=new HashSet<>(),endingDone=new HashSet<>();
    /** Each seated army's 1-based match rank, set by finish for the report rows. */
    final Map<GravesAndGlory.Squad,Integer> endingRanks=new IdentityHashMap<>();
    /** Ending polish pass 1, set by rankArmies at finish: each seated army's award (EndingAwards) and its line, the participant a stand-in seat belongs to when he
     *  left the battle (offline at finish), and the MVP (the best ranked army whose commander did not leave). */
    final Map<GravesAndGlory.Squad,EndingAwards.Award> endingAwards=new IdentityHashMap<>();final Map<GravesAndGlory.Squad,EndingAwards.Line> endingLines=new IdentityHashMap<>();
    final Map<GravesAndGlory.Squad,UUID> endingLeft=new IdentityHashMap<>();GravesAndGlory.Squad endingMvp;
    /** Ending polish pass 3: each team's mode score every few seconds of this round (the STATS graph), and the participants who pressed Play again
     *  (each is marked Ready in his room through social:ready as soon as the room is idle: at once, or when returnLobby ends the room's match). */
    final MatchEnding.Timeline timeline=new MatchEnding.Timeline();final Set<UUID> playAgain=new HashSet<>();
    boolean ending(){return endingStart>=0;}
    /** The harness keeps the old instant return (test-tools/isolated-checks.gradle sets it from GRAVES_INSTANT_ENDING); absent, the sequence plays. */
    static boolean instantEnding(){return Boolean.getBoolean("gravesandglory.instantEnding");}
    final Map<UUID,Long> botRespawns=new HashMap<>();
    final Set<UUID> participants=new HashSet<>();
    final Map<UUID,Long> joinedAt=new HashMap<>();
    /** Ticks each participant has spent away this round in absences that ended (he dropped and came back); his pay is pro-rated by the time he was seated (MatchRules.seatedTicks). */
    final Map<UUID,Long> awayTicks=new HashMap<>();
    /** The tick each participant who dropped this round and has not returned left, so a match that ends without him still pays his time seated. */
    final Map<UUID,Long> leftAt=new HashMap<>();
    /** What settles a participant who is away at the end: a detached copy of his seat (team, banner, class) taken at the drop. It is in no squads map. */
    final Map<UUID,GravesAndGlory.Squad> absentSeat=new HashMap<>();
    final Set<UUID> collectionsReady=new HashSet<>();
    /** Players with no server record when they joined; only they may bring a collection from their own computer, once. */
    final Set<UUID> importAllowed=new HashSet<>();
    /** Commanders who left mid-battle, mapped to the bot squad holding their army until they return this round. */
    final Map<UUID,UUID> vacated=new HashMap<>();
    /** The tick each dropped commander's slot is released (MatchRules.releaseAt); after it he no longer holds the army. */
    final Map<UUID,Long> vacatedUntil=new HashMap<>();
    /** Lessons reached in the campaign battle being fought, when its sheet row teaches: 0 Follow me or Retreat, 1 Formation, 2 Attack or Charge, 3 barracks, 4 all done. Cleared as each round starts. */
    final Map<UUID,Integer> tutorial=new HashMap<>();
    /** AI armies that fell back stay there until rallied, so they do not walk back into the fight. */
    final Set<UUID> botRetreating=new HashSet<>();
    final Map<UUID,String> notices=new HashMap<>();
    /** The last gear_v1 string each player was sent, so the packet goes out only when it changes. */
    final Map<UUID,String> gearSent=new HashMap<>();
    void syncGear(ServerPlayerEntity p){
        if(!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,GravesAndGlory.GEAR))return;
        String text=GearWire.encode(profile(p.getUuid()));
        if(text.equals(gearSent.get(p.getUuid())))return;
        var packet=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();packet.writeString(text,GearWire.LIMIT);
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,GravesAndGlory.GEAR,packet);
        gearSent.put(p.getUuid(),text);
    }
    /** How many notices each player has been sent; the snapshot carries it so a client can tell a notice sent again from one it has already shown. */
    final Map<UUID,Integer> noticeSerials=new HashMap<>();
    /**
     * When each notice was posted (battle ticks). expireNotices drops an action's notice (notice) NOTICE_TICKS after it, so a notice the player dismissed on his screen
     * cannot come back with a later camp snapshot (Phase 2 Task 13's trade-off, done in in-match progression Task 4; no protocol change). The same words posted again start
     * a new 10 seconds. A line that tells the player's state (standing: the welcome, the match reward; the wait for the next round is an action's notice, it stops being true with the round) is marked STANDING and stays until the
     * next notice replaces it.
     */
    final Map<UUID,Long> noticeAt=new HashMap<>();
    /** How long an action's notice stays in the camp snapshot: 10 seconds. */
    static final int NOTICE_TICKS=200;static final int ROLL_WRITS=Profile.ROLL_WRITS,CHOSEN_WRITS=Profile.CHOSEN_WRITS;
    /** noticeAt's mark for a standing line, which never expires. */
    static final long STANDING=Long.MAX_VALUE;
    final Map<UUID,JsonObject> results=new HashMap<>();
    /** Per commander, the last roll or chosen buy (the snapshot's lastRoll, for the camp's roll result card): {n, troop, duplicate, starsBefore, starsAfter, shards}; n counts up so the client tells a new one from the same. Not saved. */
    final Map<UUID,JsonObject> lastRolls=new HashMap<>();
    private int rollSerial;
    /** The shortened forms each commander's camp snapshot has been sent in: each form is logged once, until he leaves. */
    final Map<UUID,Set<String>> snapshotForms=new HashMap<>();
    String headline="";
    /** The draw behind a recruitment roll: the world's random by default; a harness swaps in a fixed one to land a roll on a chosen troop. */
    java.util.random.RandomGenerator rollRng=new java.util.random.RandomGenerator(){
        @Override public long nextLong(){return b.world.random.nextLong();}
        @Override public int nextInt(int bound){return b.world.random.nextInt(bound);}
    };
    /** Bot difficulty for the next round: the id of a Spec.BOTCOMMAND row (easy, normal, hard). There is one battle per server, so one setting covers it. */
    String difficulty="normal";
    /** The bot difficulty a room's round plays at (set when the room starts, empty otherwise); botTier is what bots use. */
    String roundTier="";
    String botTier(){return roundTier.isEmpty()?difficulty:roundTier;}
    /** The Long March battle being fought (a Spec.CAMPAIGNBATTLES row), or null in practice and rooms. Cleared in returnLobby. */
    Spec.CampaignBattlesRow activeBattle;
    /** The bonus objective of activeBattle (plan 2C.3), or null. Set in soloStart, cleared in returnLobby; the round's events feed it from plan 2C.4. */
    MarchMath.Tracker bonusTracker;
    /** A battle asked for before its map was built, and the commander who asked; it starts on the first ready tick. */
    Spec.CampaignBattlesRow pendingBattle;UUID pendingCommander;
    /** The winner of the last finished round: 0 Dawn, 1 Dusk, -1 a draw (and before any round has finished). The bot simulation sets -2 before each of its rounds. */
    int lastWinner=-1;
    /** The solo-start group: the one commander who starts a practice or campaign battle (and what tests seat); its leader sets mode, map and bot fill. War council rooms (SocialHub) are how commanders play together. */
    UUID partyLeader;final LinkedHashSet<UUID> party=new LinkedHashSet<>();
    Path layoutFile(){return b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json");}
    Campaign(GravesAndGlory.Battle battle){
        b=battle;Path save=b.server.getSavePath(WorldSavePath.ROOT);
        directory=save.resolve(Diagnostics.active()?"gravesandglory-test-profiles":"gravesandglory-profiles");
        Path groups=save.resolve("gravesandglory-parties.json");
        // A damaged parties file falls back to its .bak, then to no parties, with a logged warning (SafeFiles.load); it never stops the server.
        social=SafeFiles.load(groups,Campaign::readParties,SocialHub::new,"Party data");
        try {
            LegacyDataMigration.profiles(save.resolve(Diagnostics.active()?"bannerbound-test-profiles":"bannerbound-profiles"),directory);
            LegacyDataMigration.copyMissing(save.resolve("bannerbound-layout.json"),layoutFile());
        } catch(java.io.IOException migration) { throw new IllegalStateException("Could not upgrade legacy commander data; original files were kept",migration); }
        try{JsonObject marker=JsonParser.parseString(Files.readString(layoutFile())).getAsJsonObject();if(marker.get("layout").getAsString().equals(LayoutMarker.LAYOUT))map=marker.get("map").getAsInt();}catch(Exception ignored){}CampaignMaps.select(map);
    }
    /** A commander's collection, read once: a schema-1, 2 or 3 file is upgraded in place to schema 4 first (spec 6.3, plan 2C.3; its original bytes stay as <uuid>.v1.json, .v2.json or .v3.json), then the profile is sanitized. A new commander owns the 10 Commons, flies the starter banner and wears the starter gear. */
    Profile profile(UUID id){
        Profile pr=profiles.computeIfAbsent(id,key->SafeFiles.load(directory.resolve(key+".json"),Campaign::readProfile,Campaign::freshProfile,"Commander profile "+key));
        if(pr.readOnly&&newerSaveTold.add(id)){ // a save from a newer build: kept untouched all session (save writes nothing); the player is told once, the log keeps a warning
            System.err.println("[Graves and Glory] Commander profile "+id+" is schema "+pr.schema+", newer than this build's schema "+Profile.SCHEMA+"; the file is left untouched and this session's progress will not be saved.");
            notice(id,"Your commander save is from a newer version of Graves and Glory; progress this session will not be saved.");
        }
        return pr;
    }
    /** The name for the join ledger: the online player's, or empty (the ledger keeps the one it has). */
    String nameOf(UUID id){var p=b.server.getPlayerManager().getPlayer(id);return p==null?"":p.getName().getString();}
    /** Players already told that their save is from a newer build (Profile.readOnly), so the notice comes once per session. */
    final Set<UUID> newerSaveTold=new HashSet<>();
    /** A new commander: the 10 Commons, the starter banner (sanitize fills them in) and the role's starter gear. Also what a damaged profile with no usable .bak falls back to. */
    static Profile freshProfile(){Profile fresh=new Profile();Gear.grantStarter(fresh);fresh.sanitize();return fresh;}
    /** One profile file, upgraded in place first when it is schema 1, 2 or 3; throws when it cannot be read (SafeFiles.load then tries the .bak). */
    static Profile readProfile(Path file) throws Exception{
        Profile p=ProfileMigration.upgradeFile(file,text->JSON.fromJson(text,ProfileMigration.V1.class),text->JSON.fromJson(text,Profile.class),JSON::toJson,RosterMath.Catalog.fromSpec());
        if(p==null)p=JSON.fromJson(Files.readString(file),Profile.class);if(p==null)throw new IllegalStateException("Empty profile");p.sanitize();return p;}
    /** The parties file: Gson to SocialHub, then restore; throws when it cannot be read. */
    static SocialHub readParties(Path file) throws Exception{SocialHub s=JSON.fromJson(Files.readString(file),SocialHub.class);if(s==null)throw new IllegalStateException("Empty party data");s.restore();return s;}
    /** The marker (see LayoutMarker) names this layout, this map, this revision of the arena margin and the hash of the map's landmarks (Structures.hash); a save made before the margin was tapered, or before these landmarks, builds its arena once more. */
    boolean cached(){try{return LayoutMarker.current(Files.readString(layoutFile()),map,Structures.hash(map));}catch(Exception e){return false;}}
    void built(){try{SafeFiles.write(layoutFile(),LayoutMarker.text(map,Structures.hash(map)));}catch(Exception e){System.err.println("[Graves and Glory] Map cache marker could not be saved.");}}
    boolean save(UUID id){try{var pr=profile(id);ProfileMigration.saveProfile(pr,directory.resolve(id+".json"),JSON.toJson(pr));return true;
        }catch(Exception e){System.err.println("[Graves and Glory] Could not save commander collection: "+e.getClass().getSimpleName());notice(id,"Collection could not be saved; check disk space.");return false;}}
    void importProfile(ServerPlayerEntity p,String json){
        if(json.length()>32000)return;
        // The server's saved collection is the only source of truth. The client's copy is a display cache.
        // It is imported once, on a first visit to a single-player or LAN world with no record yet, so
        // existing collections carry over; dedicated servers never accept one.
        UUID id=p.getUuid();boolean allowed=importAllowed.remove(id)&&!b.server.isDedicated();
        try{var v1=JSON.fromJson(json,ProfileMigration.V1.class); // a portable copy from before Houses is upgraded the same way as a server file
            Profile portable=v1!=null&&v1.schema<2?ProfileMigration.toV2(v1,RosterMath.Catalog.fromSpec()):JSON.fromJson(json,Profile.class);portable.sanitize();Gear.resetForImport(portable);
            if(allowed){profiles.put(id,portable);save(id);}
            else{Profile stored=profile(id);if(portable.troopWrits>stored.troopWrits||!stored.owned.keySet().containsAll(portable.owned.keySet()))System.out.println("[Graves and Glory] Ignored the collection sent by "+p.getName().getString()+"; the server's saved collection is kept.");}
        }catch(Exception ignored){}
        collectionsReady.add(id);
    }
    /** The welcome notices a joining player gets (waiting while a battle runs, or in the camp), each naming his own menu key. */
    static final String WELCOME_WAIT="Battle in progress. Wait in camp for the next round. {key:menu} opens your war council.",WELCOME="Welcome to Graves and Glory. {key:menu} opens your war council.";
    /** An action's result for the player (a refusal, a buy, a vote saved, the wait for the next round): shown on his action bar and kept in the camp snapshot for NOTICE_TICKS. The camp snapshot carries noticeSerials so a client can tell a notice sent again from one it has already shown, and the client fills in each {key:(id)} with the player's own binding. */
    void notice(UUID id,String text){post(id,text,b.time);}
    /** A line that tells the player's state (the welcome, the match reward): shown like a notice, but kept until the next notice replaces it. */
    void standing(UUID id,String text){post(id,text,STANDING);}
    private void post(UUID id,String text,long at){notices.put(id,text);noticeAt.put(id,at);noticeSerials.merge(id,1,Integer::sum);var p=b.server.getPlayerManager().getPlayer(id);if(p!=null)p.sendMessage(keyText(text),true);}
    /** A {key:(id)} template as chat text: each text as it is, each id as Text.keybind of its binding (HintText.bindingKey), so
     *  every client shows its own key, or its "Not bound", named as its feed names it (KeybindTextMixin: a mouse button as LMB,
     *  RMB or MMB). */
    public static Text keyText(String template){
        var parts=HintText.tokens(template);var out=Text.literal(parts.get(0));
        for(int i=1;i<parts.size();i+=2)out.append(Text.keybind(HintText.bindingKey(parts.get(i)))).append(parts.get(i+1));
        return out;
    }
    /** Drops every action's notice older than NOTICE_TICKS (tick, once a second); a standing line stays. A notice put without notice() (a harness's) is timed from the first pass that sees it. */
    void expireNotices(){
        notices.keySet().removeIf(id->{Long at=noticeAt.get(id);if(at==null){noticeAt.put(id,b.time);return false;}return at!=STANDING&&b.time-at>NOTICE_TICKS;});
        noticeAt.keySet().retainAll(notices.keySet());
    }
    /** The first-visit rule: a player with no server file may bring his collection from his own computer, once; one with a file may not (the server copy wins). */
    void grantImport(UUID id){if(!Files.exists(directory.resolve(id+".json")))importAllowed.add(id);}
    /** A seated commander has just dropped from a running round (before his seat is handed to a bot): his time away starts, and a copy of his seat is kept to settle him if he does not return. */
    void dropped(GravesAndGlory.Squad s){
        UUID id=s.owner;if(!b.running||!participants.contains(id))return;
        var copy=new GravesAndGlory.Squad(id,s.team,b.fieldBase(s.team));copy.banner=new ArrayList<>(s.banner);copy.seated=true;
        leftAt.put(id,b.time);absentSeat.put(id,copy);
    }
    /** A dropped participant is seated again: the time he was away is added up, and it comes off his pay. */
    void returned(UUID id){
        Long at=leftAt.remove(id);absentSeat.remove(id);
        if(at!=null)awayTicks.merge(id,Math.max(0,b.time-at),Long::sum);
    }
    /** Ticks a participant has been seated in this round so far (MatchRules.seatedTicks): from his own seating, less the time he was away. */
    long seatedTicks(UUID id){return MatchRules.seatedTicks(b.time,b.roundStarted,joinedAt.getOrDefault(id,b.roundStarted),awayTicks.getOrDefault(id,0L),leftAt.getOrDefault(id,-1L));}
    /** True while a participant has dropped from this round and is not seated again; the round still owes him his time seated. */
    boolean away(UUID id){return leftAt.containsKey(id);}
    void joined(ServerPlayerEntity p){
        grantImport(p.getUuid());profile(p.getUuid());
        AdminCommands.seen(p); // the join ledger's first-seen stamp
        // A dedicated server writes a new commander's file on the first join, not on the first collection change (QA C9). The first-visit import is never offered there.
        if(b.server.isDedicated()&&!Files.exists(directory.resolve(p.getUuid()+".json")))save(p.getUuid());
        // Rejoining the same battle keeps the original start time, so a dropped connection does not cost the reward.
        if(!(b.running&&participants.contains(p.getUuid())))joinedAt.put(p.getUuid(),b.time);var squad=b.squads.get(p.getUuid());
        if(ending()){skipped.put(p.getUuid(),MatchEnding.Step.DONE);endingDone.add(p.getUuid());toLobby(p);return;} // joining during the ending: to camp, no step; a participant's reward and report are already settled
        if(b.running&&squad!=null&&squad.seated){returned(p.getUuid());b.toBase(p,squad.team);}else toLobby(p);
        if(b.running&&(squad==null||!squad.seated))notice(p.getUuid(),WELCOME_WAIT); // true for the moments he reads it, and gone with the round (returnLobby) or after NOTICE_TICKS: no standing line, which the round's end would leave false
        else standing(p.getUuid(),WELCOME);} // the profile's one-time notice waits for the War Table's "notice" command: shown at join, the client is not ready and the text is spent unseen
    /** Shows the profile's one-time notice (Profile.notice, set when a banner was trimmed on load) as a normal notice, then clears it and saves; nothing when there is none. */
    void showNotice(UUID id){var pr=profile(id);if(pr.notice==null)return;String text=pr.notice;pr.notice=null;if(save(id))notice(id,text);else pr.notice=text;}
    void toLobby(ServerPlayerEntity p){var at=CampWorld.spawn(profile(p.getUuid()));b.relocate(p,at.x(),at.y(),at.z(),at.yaw(),at.pitch());p.changeGameMode(GameMode.ADVENTURE);p.getInventory().clear();GearCombat.clear(p);p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(20);}
    void returnLobby(){
        // The mode takes down what it placed before the round's armies go (idempotent: finish and a manual return both call it).
        // Only a clean teardown deletes the marker: a piece that could not be put back stays recoverable after a crash.
        if(b.mode.teardown(b))Modes.unmark(b);else System.err.println("[Graves and Glory] The round's teardown left "+b.saves.size()+" saved piece(s); the mode marker is kept.");
        if(battleModeBeforeRoom!=null){battleMode=battleModeBeforeRoom;battleModeBeforeRoom=null;} // the room's mode ends with its round
        for(var s:b.squads.values())Commander.reset(b,s); // every running commander ability ends before its units go
        boolean wasEnding=ending();endingStart=-1;skipped.clear();endingStaged.clear();endingDone.clear();endingRanks.clear();endingAwards.clear();endingLines.clear();endingLeft.clear();endingMvp=null;if(wasEnding)PodiumStage.release(b.world);
        b.running=false;introUntil=0;social.finish();readyAgain();activeRoom="";roundTier="";activeBattle=null;bonusTracker=null;Projectiles.clear(b);b.strikes.clear();Abilities.clearBanners(b);b.clearIntel();
        Houses.reset(b);for(var u:b.units)u.mob.discard();b.units.clear();b.clearCorpses();b.clearSides();b.clearProgression();b.clearPlans();
        b.squads.values().removeIf(s->s.bot);botRespawns.clear();botRetreating.clear();botGuard.clear();musterFirstAt.clear();b.respawns.clear();vacated.clear();vacatedUntil.clear();awayTicks.clear();leftAt.clear();absentSeat.clear();deleteProgress();
        for(var s:b.squads.values()){s.training.clear();s.seated=false;}closeParty();
        notices.values().removeIf(WELCOME_WAIT::equals);noticeAt.keySet().retainAll(notices.keySet()); // "Battle in progress" is no longer true
        for(var p:b.server.getPlayerManager().getPlayerList())toLobby(p);
        // The autosaves skipped during the round (ArenaAutosave) are paid now, with the armies gone and everyone in camp.
        // A stopping server skips this: its own shutdown save follows.
        if(ArenaAutosave.settle()&&b.server.isRunning()){long at=System.nanoTime();b.server.saveAll(true,false,false);System.out.println("[Graves and Glory] Saved the world after the battle ("+(System.nanoTime()-at)/1_000_000+" ms).");}
    }
    /** Leaving frees any seat in the solo-start group, and forgets which snapshot forms were logged for him. */
    void left(UUID id){newerSaveTold.remove(id);gearSent.remove(id);forgeResults.remove(id);leaveParty(id);snapshotForms.remove(id);notices.remove(id);noticeAt.remove(id);} // his notice entries go with him, a standing line's too (expireNotices never drops those)
    boolean handle(ServerPlayerEntity p,String cmd){
        if(cmd.startsWith("group:"))return CustomGroups.command(this,b,p,cmd); // custom group edits from the Armoury (command UI Task 12)
        if(cmd.startsWith("social:"))return socialCommand(p,cmd);
        if(cmd.startsWith("progress:"))return progressCommand(p,cmd);
        if(cmd.equals("camp:practice")){
            String refused=soloRefusal(p.getUuid(),"Practice is solo. Use a private room to play with your party.");if(refused.isEmpty())refused=bannerRefusal(p.getUuid(),null);if(!refused.isEmpty()){notice(p.getUuid(),refused);return true;}
            soloStart(p,null);return true;
        }
        if(cmd.startsWith("campaign:")){ // a Long March battle: solo, on its own map, which is built first if another map stands
            var row=Arrays.stream(Spec.CAMPAIGNBATTLES).filter(r->r.id().equals(cmd.substring(9))).findFirst().orElse(null);
            if(row==null)return true;
            if(row.map()<0||row.map()>=Spec.MAPS.length){notice(p.getUuid(),row.name()+" cannot start: its map is missing.");return true;}
            var lock=MarchMath.lockedBy(row,profile(p.getUuid()).battlesWon,Spec.CAMPAIGNBATTLES);if(lock!=null){notice(p.getUuid(),"Win "+lock.name()+" first.");return true;} // plan 2C.3: the March is won in order
            var starts=seasonStarts();String closed=MarchMath.Seasons.lock(row,Spec.CAMPAIGNCHAPTERS,MarchMath.Seasons.current(starts,Progression.today()),starts);if(!closed.isEmpty()){notice(p.getUuid(),closed);return true;} // brief G2: a chapter opens with its season
            if(!Modes.registered(row.mode())||!Modes.hosts(row.mode(),row.map())){notice(p.getUuid(),row.name()+" cannot start yet: its mode arrives with the mode frame.");return true;} // survival and banner come with plan 2C.4
            String refused=soloRefusal(p.getUuid(),MARCH_SOLO);if(refused.isEmpty())refused=bannerRefusal(p.getUuid(),row);if(!refused.isEmpty()){notice(p.getUuid(),refused);return true;}
            pendingBattle=row;pendingCommander=p.getUuid();
            if(row.map()!=map){switchMap(row.map());notice(p.getUuid(),"Preparing "+Spec.MAPS[map].name()+" for "+row.name()+".");}
            return true;
        }
        if(cmd.equals("notice")){showNotice(p.getUuid());return true;} // the War Table page opened: a one-time profile text (a banner trimmed on load) is shown now
        if(cmd.equals("lobby")){if(b.running)notice(p.getUuid(),"Finish the battle before returning to the lobby.");else toLobby(p);return true;}
        if(b.running){
            var current=b.squads.get(p.getUuid());
            boolean global=cmd.startsWith("map:")||cmd.startsWith("mode:")||cmd.equals("fill")||cmd.equals("start")||cmd.startsWith("difficulty:");
            boolean personal=cmd.equals("roll")||cmd.startsWith("roll:")||cmd.startsWith("buy:")||cmd.startsWith("star:")||cmd.startsWith("legendary:")||cmd.startsWith("unlock:")||cmd.startsWith("army:")||BannerCommands.handles(cmd)||cmd.startsWith("colour:")||GearCommands.arsenal(cmd);
            if(global||personal&&current!=null&&current.seated){notice(p.getUuid(),"Active battle choices are locked until the match ends.");return true;}
        }
        if(cmd.startsWith("difficulty:")){
            String id=cmd.substring(11);if(Arrays.stream(Spec.BOTCOMMAND).noneMatch(r->r.id().equals(id)))return true;
            if(partyLeader!=null&&!leader(p)){notice(p.getUuid(),"Only the host sets the bots.");return true;}
            difficulty=id;notice(p.getUuid(),"Bots: "+id.substring(0,1).toUpperCase()+id.substring(1)+".");return true;
        }
        Profile pr=profile(p.getUuid());var squad=b.squads.get(p.getUuid());if(squad==null)return false;
        if(cmd.equals("roll")||cmd.startsWith("roll:")){roll(p,pr,cmd.equals("roll")?RosterMath.rollHouse(pr.line()):cmd.substring(5));return true;}
        if(cmd.startsWith("buy:")){buy(p,pr,cmd.substring(4));return true;}
        if(GearCommands.arsenal(cmd)){gear(p,pr,cmd);return true;}
        if(cmd.startsWith("star:")){star(p,pr,cmd.substring(5));return true;}
        if(cmd.startsWith("legendary:")){legendary(p,pr,cmd.substring(10));return true;}
        if(cmd.startsWith("unlock:")||cmd.startsWith("army:")){notice(p.getUuid(),"Troops now join by recruitment rolls at the camp shrine.");return true;}
        if(BannerCommands.handles(cmd)){ // cell:<banner>:<col>:<row>:<id|-> and banner:new/rename/copy/delete/use: the same paint rules the page previews with; a refusal changes and saves nothing
            var before=JSON.toJson(pr);String refusal=BannerCommands.apply(pr,cmd,RosterMath.Catalog.fromSpec(),RosterMath.fieldableHouses());
            if(refusal!=null){notice(p.getUuid(),refusal);return true;}
            if(!save(p.getUuid())){profiles.put(p.getUuid(),Profile.restored(JSON.fromJson(before,Profile.class)));return true;}
            var room=social.room(p.getUuid()); // a banner change that leaves the lobby's House pick no troop takes back the Ready
            if(room!=null&&!room.playing&&room.ready.contains(p.getUuid())&&!houseRefusal(room,p.getUuid()).isEmpty()){room.ready.remove(p.getUuid());social.revision++;notice(p.getUuid(),houseRefusal(room,p.getUuid()));}
            return true;
        }
        if(cmd.startsWith("colour:")){ // the side row this commander asks to fly (spec 1.1); chooseSides settles it as the round is prepared
            String side=cmd.substring(7);var row=Arrays.stream(Spec.FACTIONS).filter(f->f.id().equals(side)).findFirst().orElse(null);if(row==null)return true;
            String before=pr.bannerColour;pr.bannerColour=side;if(!save(p.getUuid())){pr.bannerColour=before;return true;}
            notice(p.getUuid(),"You fly the "+row.name()+", unless the other team flies it first.");return true;
        }
        if(cmd.startsWith("mode:")){
            if(!leader(p))return true;
            String mode=cmd.substring(5);int requested=switch(mode){case "ai","1"->1;case "2"->2;case "3"->3;case "4"->4;default->0;};
            if(requested==0)return true;
            if(party.size()>requested*2||(mode.equals("ai")&&party.size()>1)){notice(p.getUuid(),"Your group is too large for that mode.");return true;}
            size=requested;practice=mode.equals("ai");if(!practice)for(var entry:profiles.entrySet()){entry.getValue().sanitize();save(entry.getKey());}balance();return true;
        }
        if(cmd.equals("fill")){if(leader(p))fill=!fill;return true;}
        if(cmd.startsWith("map:")){
            if(!leader(p))return true;int index;try{index=Integer.parseInt(cmd.substring(4));}catch(Exception e){return true;}
            if(index>=0&&index<Spec.MAPS.length&&index!=map){switchMap(index);notice(p.getUuid(),"Preparing "+Spec.MAPS[map].name()+". Your army choices are saved.");}
            return true;
        }
        if(cmd.startsWith("battlemode:")){
            if(!leader(p))return true;
            String id=cmd.substring(11).trim();
            if(!Modes.registered(id)){notice(p.getUuid(),"No such battle mode: "+id+".");return true;}
            if(!Modes.hosts(id,map)){notice(p.getUuid(),Modes.row(id).name()+" is not played on "+Spec.MAPS[map].name()+".");return true;}
            battleMode=id;notice(p.getUuid(),"Battle mode: "+Modes.row(id).name()+".");return true;
        }
        if(cmd.equals("start")){
            if(!leader(p)){notice(p.getUuid(),"Only the host starts the match.");return true;}
            if(!fill&&!practice&&party.size()!=size*2){notice(p.getUuid(),"Waiting for commanders. Enable bot fill to play now.");return true;}
            if(practice&&party.size()>1){notice(p.getUuid(),"Practice is for one commander.");return true;}
            for(UUID id:party){var s=b.squads.get(id);if(s!=null&&bannerUnits(id)==0){notice(p.getUuid(),"Every commander needs at least one troop in their banner.");return true;}}
            for(UUID id:party){var s=b.squads.get(id);if(s!=null)s.seated=true;}
            activeBattle=null;b.startRound();return true;
        }
        return false;
    }
    static final String MARCH_SOLO="Campaign battles are played solo. Leave your party or room first.";
    /** Why a commander cannot start a solo battle (practice or a Long March battle) now, or "" when they can: one battle at a time, solo means no party and no room (soloRule says so), and a battlefield being prepared for someone else stays theirs. */
    /** The season start dates (brief G2) from config/gravesandglory/seasons.json beside the server (the game folder in solo); none without the file. */
    private static List<java.time.LocalDate> seasonCache=List.of();private static long seasonReadAt=Long.MIN_VALUE;
    static List<java.time.LocalDate> seasonStarts(){ // read at most every 30 seconds: the snapshot asks every 10 ticks per player
        long now=System.currentTimeMillis();if(!MarchMath.Seasons.reread(seasonReadAt,now))return seasonCache;seasonReadAt=now;seasonCache=readSeasonStarts();return seasonCache;
    }
    private static List<java.time.LocalDate> readSeasonStarts(){
        try{Path f=net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("gravesandglory").resolve("seasons.json");return Files.exists(f)?MarchMath.Seasons.parse(Files.readString(f)):List.of();}
        catch(Exception e){return List.of();}
    }
    /** C06-F2: a solo battle needs a troop to field, as start asks of every commander; a House chapter fields its fixed House row instead, so it never refuses. Practice passes no row. */
    String bannerRefusal(UUID id,Spec.CampaignBattlesRow row){return MarchMath.houseBannerRow(row,Spec.CAMPAIGNCHAPTERS)==null&&bannerUnits(id)==0?"Every commander needs at least one troop in their banner.":"";}
    String soloRefusal(UUID id,String soloRule){
        if(b.running)return "Wait until the current battle finishes.";
        if(social.group(id).size()>1||social.room(id)!=null)return soloRule;
        if(!preparingRoom.isEmpty()||pendingBattle!=null&&!id.equals(pendingCommander))return "Another battlefield is being prepared. Try again shortly.";
        return "";
    }
    /** Starts a solo battle: the commander alone on Dawn; bot fill seats the rest of both teams (one bot opposite in practice, a March battle's size otherwise). Practice passes no row; a Long March battle passes its row, which sets the bot tier and the lessons. */
    void soloStart(ServerPlayerEntity p,Spec.CampaignBattlesRow row){
        closeParty();partyLeader=p.getUuid();party.add(p.getUuid());practice=true;size=MarchMath.seatSize(row);fill=true; // practice stays the solo-seating flag (one human, no visitors); the battle's own size sets the sides, and bot fill seats the rest
        for(var squad:b.squads.values())if(!squad.bot){squad.seated=squad.owner.equals(p.getUuid());if(squad.seated)squad.team=0;}
        if(row!=null){if(battleModeBeforeRoom==null)battleModeBeforeRoom=battleMode;battleMode=row.mode();} // a March battle plays its own mode; returnLobby puts the lobby's back
        activeBattle=row;var bonus=MarchMath.bonus(row,Spec.BONUSOBJECTIVES);bonusTracker=bonus==null?null:new MarchMath.Tracker(bonus);b.startRound();
    }
    void balance(){for(var seat:balanced(seats(s->s.seated),practice,size))b.squads.get(seat.owner()).team=seat.team();}
    /** A commander's seat as balance and the side pick read it: who, on which team. */
    record Seat(UUID owner,int team){}
    /** The commanders (bots left out) whose squads pass the filter, in b.squads order, the order balance and the side pick read, each on his squad's team. */
    List<Seat> seats(java.util.function.Predicate<GravesAndGlory.Squad> in){List<Seat> out=new ArrayList<>();for(var s:b.squads.values())if(!s.bot&&in.test(s))out.add(new Seat(s.owner,s.team));return out;}
    /** balance's rule: in order, outside practice, a commander whose team already holds size commanders goes to the other team. */
    static List<Seat> balanced(List<Seat> seats,boolean practice,int size){
        int[] n={0,0};List<Seat> out=new ArrayList<>();
        for(var s:seats){int team=s.team();if(!practice&&n[team]>=size)team=1-team;n[team]++;out.add(new Seat(s.owner(),team));}
        return out;
    }
    void prepareRound(){
        tutorial.clear();
        b.squads.values().removeIf(s->s.bot);balance();
        for(var u:b.units){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();}b.units.clear();b.clearCorpses();botRespawns.clear();botRetreating.clear();botGuard.clear();musterFirstAt.clear();vacated.clear();vacatedUntil.clear();awayTicks.clear();leftAt.clear();absentSeat.clear();
        for(int team=0;team<2;team++){final int t=team;int humans=(int)b.squads.values().stream().filter(s->s.team==t&&s.seated).count();
            int target=size; // practice and a March battle set size in soloStart, so one bot per team stays the practice rule
            if(fill||practice)for(int i=humans;i<target;i++){UUID id=UUID.randomUUID();var s=new GravesAndGlory.Squad(id,team,b.fieldBase(team));s.bot=true;s.tier=activeBattle!=null?activeBattle.difficulty():botTier();s.lane=i%3;var ally=allyRow(s);if(ally!=null)s.tier=ally.difficulty();b.squads.put(id,s);} // a March battle's allies fight at their own row's tier
        }
        participants.clear();for(var s:b.squads.values())if(!s.bot&&s.seated){participants.add(s.owner);results.remove(s.owner);joinedAt.put(s.owner,b.time);GearCombat.seat(s,profile(s.owner));}
        for(var s:b.squads.values())if(s.seated)field(s);
        chooseSides();
        serial++;matchId=UUID.randomUUID().toString();introUntil=b.time+120;headline="";
        timeline.clear();playAgain.clear(); // pass 3: a new round's graph
    }
    /** A troop row by id, or null for empty, ^ or an unknown id. */
    static Spec.TroopsRow troop(String id){for(var t:Spec.TROOPS)if(t.id().equals(id))return t;return null;}
    /** A botBanners row as a banner list, or the starter banner when the sheet has no such row. */
    static List<String> bannerRow(String id){for(var row:Spec.BOTBANNERS)if(row.id().equals(id))return new ArrayList<>(Arrays.asList(row.slots().split(",")));return RosterMath.starterBanner();}
    /** The banner a squad fields this round: a bot's from botBanner; a War Table room member's own banner with only the units of
     *  the House he took on the lobby (lobbyHouse; Kort, 2026-10-31); anyone else's own banner with the units of Houses that
     *  cannot be fielded yet left empty. Always the dense war table (table): a bot row is placed on it by role. */
    List<String> banner(GravesAndGlory.Squad s){return table(bannerLine(s));}
    /** Sets a squad's banner for this round and keeps the ordered line it came from, which a bot's buy order follows. */
    void field(GravesAndGlory.Squad s){s.line=bannerLine(s);s.banner=table(s.line);}
    /** The war table of a banner list: a dense list of WarGrid.cells() entries is copied as it is; any other list (a botBanners row, a campaign enemy, boss or ally row, a house chapter row, a synthetic or test squad's) is an ordered troop list that WarGrid.botTable places by role, then WarGrid.dense. */
    static List<String> table(List<String> line){
        if(line.size()==WarGrid.cells())return new ArrayList<>(line);
        return WarGrid.dense(WarGrid.botTable(line));
    }
    private List<String> bannerLine(GravesAndGlory.Squad s){
        if(s.bot)return botBanner(s);
        String house=MarchMath.houseBannerRow(activeBattle,Spec.CAMPAIGNCHAPTERS);if(house!=null)return bannerRow(house); // a house chapter fields its fixed House banner, never the collection (brief G1)
        var room=activeRoom.isEmpty()?null:social.rooms.get(activeRoom);
        if(room!=null&&room.members.contains(s.owner))return houseBanner(room,s.owner);
        return RosterMath.fieldable(profile(s.owner).line(),RosterMath.Catalog.fromSpec(),RosterMath.fieldableHouses());
    }
    /** A room member's banner as the lobby's House pick fields it: only that House's units. */
    List<String> houseBanner(SocialHub.Room r,UUID m){return RosterMath.houseBanner(profile(m).line(),RosterMath.Catalog.fromSpec(),lobbyHouse(r,m),RosterMath.fieldableHouses());}
    /** Units a room member would field under his House pick. */
    int houseUnits(SocialHub.Room r,UUID m){return RosterMath.units(houseBanner(r,m),RosterMath.Catalog.fromSpec());}
    /** Why a room member cannot ready under his House pick ("" when he can): his banner holds no unit of that House. The pick
     *  never borrows troops he has not placed; he picks another House or puts that House's troops in his banner first. */
    String houseRefusal(SocialHub.Room r,UUID m){if(r==null||houseUnits(r,m)>0)return "";String h=RosterMath.houseName(lobbyHouse(r,m));
        return "Your banner holds no "+h+" troops. Pick another House, or add "+h+" troops on the Banner page.";}
    /** A bot's banner (the botBanners sheet, spec 6.1): a campaign ally's row, a campaign enemy's row (campaignBattles.enemies by lane), else of the botBanners rows for its tier whose troops all belong to fieldable Houses, the one at lane % rows; Normal rows for a tier with none, the starter banner if no row fits. */
    List<String> botBanner(GravesAndGlory.Squad s){
        var ally=allyRow(s);if(ally!=null)return new ArrayList<>(Arrays.asList(ally.slots().split(",")));
        if(s.team==1){String foe=MarchMath.enemyId(activeBattle,s.lane);if(foe!=null)for(var row:Spec.BOTBANNERS)if(row.id().equals(foe))return new ArrayList<>(Arrays.asList(row.slots().split(",")));} // a campaign battle's own enemy by lane (chapter 1 brief N1), at the battle's tier
        var rows=fieldableBotBanners(s.tier);if(rows.isEmpty())rows=fieldableBotBanners("normal");
        if(rows.isEmpty())return RosterMath.starterBanner();
        var boss=bossBanner(s);if(boss!=null)return boss;
        return new ArrayList<>(Arrays.asList(rows.get(Math.floorMod(s.lane,rows.size())).slots().split(",")));
    }
    /** A kill_unit battle's target (Kort, 2026-11-01: the Hollow King stands in march-6): the enemy's first bot army uses the botBanners row that fields the target troop, whatever its tier; null when the battle has no such bonus or no row fits. */
    List<String> bossBanner(GravesAndGlory.Squad s){
        if(activeBattle==null||bonusTracker==null||!bonusTracker.kind.equals("kill_unit")||s.team!=1||s.lane!=0)return null;
        var catalog=RosterMath.Catalog.fromSpec();var fieldable=RosterMath.fieldableHouses();
        for(var row:Spec.BOTBANNERS){var slots=Arrays.asList(row.slots().split(","));
            if(slots.contains(bonusTracker.target)&&slots.stream().allMatch(id->id.equals(RosterMath.EMPTY)||fieldable.contains(catalog.houseOf(id))))return new ArrayList<>(slots);}
        return null;
    }
    /** A March battle's allied banner for a bot on the commander's side (campaignBattles.allies; Kort 2026-11-02, Q76: battle 6 fields all five Houses beside you): the named row at lane % allies, or null when the battle names none. */
    Spec.BotBannersRow allyRow(GravesAndGlory.Squad s){
        if(!s.bot||s.team!=0)return null;var ids=MarchMath.allyIds(activeBattle);if(ids.isEmpty())return null;
        String id=ids.get(Math.floorMod(s.lane,ids.size()));for(var row:Spec.BOTBANNERS)if(row.id().equals(id))return row;return null;
    }
    /** The botBanners rows of a tier, in sheet order, whose every troop belongs to a fieldable House (houses.fieldable), less the rows a battle reserves as allies. */
    static List<Spec.BotBannersRow> fieldableBotBanners(String tier){
        var catalog=RosterMath.Catalog.fromSpec();var fieldable=RosterMath.fieldableHouses();List<Spec.BotBannersRow> out=new ArrayList<>();
        for(var row:Spec.BOTBANNERS)if(row.difficulty().equals(tier)&&!MarchMath.reserved(row.id(),Spec.CAMPAIGNBATTLES,Spec.CAMPAIGNCHAPTERS)&&Arrays.stream(row.slots().split(",")).allMatch(id->id.equals(RosterMath.EMPTY)||fieldable.contains(catalog.houseOf(id))))out.add(row);
        return out;
    }
    /** Units in the banner a commander would field now; 0 means he has no army to bring. */
    int bannerUnits(UUID id){var catalog=RosterMath.Catalog.fromSpec();return RosterMath.units(RosterMath.fieldable(profile(id).line(),catalog,RosterMath.fieldableHouses()),catalog);}
    /** The progression multiplier of a commander's owned troop (spec 4.4, in-match progression design 8): its star bonus alone, capped at 1 + recruitment.capPct. Levels and gear come from the soldier, not the profile. Bots, and troops the commander does not own, fight at 1. */
    double progression(GravesAndGlory.Squad s,Spec.TroopsRow t){
        if(s.bot)return 1;var o=profile(s.owner).owned.get(t.id());if(o==null)return 1;
        var rarity=RosterMath.rarityRow(t.rarity());
        return MatchRules.starsMultiplier(MatchRules.starsCount(battleMode),RosterMath.progressionMultiplier(rarity==null?0:RosterMath.starPct(o.stars,rarity),Spec.RECRUITMENT[0].capPct())); // the starsCount rule: a mode that switches stars off fights at 1
    }
    /** Whether a commander's owned troop wears the veteran trim: Mastery II or higher (Houses spec 4.7, in-match progression design 4.4). Never a bot's. */
    boolean veteran(GravesAndGlory.Squad s,Spec.TroopsRow t){
        if(s.bot)return false;var o=profile(s.owner).owned.get(t.id());return o!=null&&o.mastery>=2;
    }
    double damage(GravesAndGlory.Unit u){return progression(u.squad,u.spec);}
    double health(GravesAndGlory.Squad s,Spec.TroopsRow t){return progression(s,t);}
    /** Sets the side row each team flies this round (Battle.sides) from the seated commanders; see sidesFor. */
    void chooseSides(){b.sides=sidesFor(seats(s->s.seated));}
    /**
     * The side rows two teams fly (spec 1.1): team 0 takes its first commander's bannerColour; team 1 its first commander's
     * colour unless team 0 already flies it; a team still without a row (all bots, or the colour was taken) takes the first
     * row, in sheet order, that the other team does not fly. Indexes into Spec.FACTIONS.
     */
    int[] sidesFor(List<Seat> seats){
        int[] picked={-1,-1};
        for(int team=0;team<2;team++)for(var s:seats)if(s.team()==team){int row=sideIndex(profile(s.owner()).bannerColour);if(row>=0&&(team==0||row!=picked[0]))picked[team]=row;break;}
        for(int team=0;team<2;team++)if(picked[team]<0)for(int row=0;row<Spec.FACTIONS.length;row++)if(row!=picked[1-team]){picked[team]=row;break;}
        return picked;
    }
    static int sideIndex(String id){for(int i=0;i<Spec.FACTIONS.length;i++)if(Spec.FACTIONS[i].id().equals(id))return i;return -1;}
    /**
     * One recruitment roll from a House's pool (spec 4.2) for recruitment.rollCost gold: its Commons, Rares and Epic at the
     * rarities.rollWeight odds, the Epic forced at recruitment.epicPity. A new troop joins the collection; a duplicate counts
     * toward its next star, and past the last star it is dismantled into that House's shards. A failed save undoes it all.
     */
    void roll(ServerPlayerEntity p,Profile pr,String house){
        var row=Arrays.stream(Spec.HOUSES).filter(h->h.id().equals(house)).findFirst().orElse(null);if(row==null)return;
        int cost=ROLL_WRITS;
        if(pr.troopWrits<cost){notice(p.getUuid(),"A recruitment roll costs "+cost+" troop writ"+(cost==1?"":"s")+". Earn writs by completing battles.");return;}
        List<String> commons=new ArrayList<>(),rares=new ArrayList<>();String epic=null;
        for(var t:Spec.TROOPS)if(t.house().equals(house)){if(t.rarity().equals("common"))commons.add(t.id());else if(t.rarity().equals("rare"))rares.add(t.id());else if(t.rarity().equals("epic"))epic=t.id();}
        var common=RosterMath.rarityRow("common");var rare=RosterMath.rarityRow("rare");var epicRow=RosterMath.rarityRow("epic");
        if(commons.isEmpty()||rares.isEmpty()||epic==null||common==null||rare==null||epicRow==null)return;
        String before=JSON.toJson(pr);
        var drawn=RosterMath.roll(commons,rares,epic,common.rollWeight(),rare.rollWeight(),epicRow.rollWeight(),pr.pity.getOrDefault(house,0),Spec.RECRUITMENT[0].epicPity(),rollRng);
        pr.troopWrits-=cost;pr.pity.put(house,drawn.pityAfter());
        var got=troop(drawn.troop());var card=rollCard(pr,got);
        String result=receive(pr,got);
        if(!save(p.getUuid())){profiles.put(p.getUuid(),Profile.restored(JSON.fromJson(before,Profile.class)));return;}
        lastRolls.put(p.getUuid(),rolled(card,pr,got));notice(p.getUuid(),result);reveal(p);
    }
    /** One copy of a troop joins the collection (spec 4.2): a new troop, a copy toward its next star, or past the last star its House's shards (RosterMath.addCopy). Returns what happened, for the notice. Rolls, chosen buys and the Founders Rare use it. */
    static String receive(Profile pr,Spec.TroopsRow t){
        var owned=pr.owned.get(t.id());
        if(owned==null){pr.owned.put(t.id(),new Profile.Owned());return "Joined your collection: "+t.name()+"!";}
        int stars=owned.stars,shards=RosterMath.addCopy(owned,RosterMath.rarityRow(t.rarity()));
        if(shards>0){pr.shards.merge(t.house(),shards,Integer::sum);return t.name()+" is at full stars: +"+shards+" "+RosterMath.houseName(t.house())+" shards.";}
        return owned.stars>stars?t.name()+" rises to star "+owned.stars+"!":"Another "+t.name()+": copy "+owned.copies+" toward the next star.";
    }
    /** The roll result card's "before" half: the troop, whether it is a duplicate, its stars before and the House's shards before (rolled finishes it). */
    static JsonObject rollCard(Profile pr,Spec.TroopsRow t){
        var o=pr.owned.get(t.id());JsonObject j=new JsonObject();j.addProperty("troop",t.id());j.addProperty("duplicate",o!=null);
        j.addProperty("starsBefore",o==null?0:o.stars);j.addProperty("shards",pr.shards.getOrDefault(t.house(),0));return j;
    }
    /** Finishes a roll result card after receive: its stars after, the shards it gave (after minus before) and a fresh n. */
    JsonObject rolled(JsonObject card,Profile pr,Spec.TroopsRow t){
        var o=pr.owned.get(t.id());card.addProperty("starsAfter",o==null?0:o.stars);
        card.addProperty("shards",pr.shards.getOrDefault(t.house(),0)-card.get("shards").getAsInt());card.addProperty("n",++rollSerial);return card;
    }
    /** The recruitment reveal sound, to the commander alone (the ServerPlayerEntity overload). */
    static void reveal(ServerPlayerEntity p){Audio.to(p,AudioCues.Cue.UNIT_REVEAL);}
    /** Undoes a command whose save failed: the profile goes back to its JSON from before, and the commander is told nothing changed. */
    /** Per commander, the last Forge roll (the snapshot's "forge"): {n, item, rarity, relic, duplicate, refund}; n counts up so the client tells a new roll from the same. Not saved. */
    final Map<UUID,JsonObject> forgeResults=new HashMap<>();
    /** One gear or forge request (spec 2026-11-03-commander-gear-design): GearCommands validates and applies it, a refusal changes nothing, and a profile that will not save is put back. */
    void gear(ServerPlayerEntity p,Profile pr,String cmd){
        UUID id=p.getUuid();String before=JSON.toJson(pr);
        var outcome=GearCommands.apply(pr,cmd,rollRng);
        if(!outcome.ok()){notice(id,outcome.refusal());return;}
        if(!save(id)){unsaved(id,before,"That gear change");return;}
        var rolled=outcome.rolled();
        if(rolled!=null){
            JsonObject card=new JsonObject();var last=forgeResults.get(id);card.addProperty("n",last==null?1:last.get("n").getAsInt()+1);
            card.addProperty("item",rolled.piece()==null?"":rolled.piece().item);card.addProperty("rarity",rolled.piece()==null?"":rolled.piece().rarity);card.addProperty("relic",rolled.relic()==null?"":rolled.relic());
            card.addProperty("duplicate",rolled.duplicate());card.addProperty("refund",rolled.refund());forgeResults.put(id,card);
        }
        notice(id,outcome.notice());
        syncGear(p);
    }
    void unsaved(UUID id,String before,String what){profiles.put(id,Profile.restored(JSON.fromJson(before,Profile.class)));notice(id,what+" was not saved, so nothing changed. Check disk space and try again.");}
    /**
     * A chosen recruit (buy:<troop>, spec 4.2): a Common or Rare of any House for recruitment.chosenCost gold, as a new troop or a copy toward its
     * next star. An Epic or Legendary, a troop already at full stars (a buy would only dismantle it) and a purse short of the price are refused.
     */
    void buy(ServerPlayerEntity p,Profile pr,String id){
        var t=troop(id);if(t==null)return;UUID who=p.getUuid();
        if(!t.rarity().equals("common")&&!t.rarity().equals("rare")){notice(who,"Only Commons and Rares can be bought. "+t.name()+" comes from recruitment rolls.");return;}
        var owned=pr.owned.get(id);if(owned!=null&&owned.stars>=RosterMath.maxStars()){notice(who,t.name()+" is at full stars already.");return;}
        int cost=CHOSEN_WRITS;
        if(pr.troopWrits<cost){notice(who,"A chosen recruit costs "+cost+" troop writs. You have "+pr.troopWrits+".");return;}
        String before=JSON.toJson(pr);pr.troopWrits-=cost;var card=rollCard(pr,t);String result=receive(pr,t);
        if(!save(who)){unsaved(who,before,"The recruit");return;}
        lastRolls.put(who,rolled(card,pr,t));notice(who,result);reveal(p);
    }
    /**
     * An Epic's or Legendary's next star (star:<troop>, spec 4.3) for its House's shards: rarities.shardStar2 for star 2, shardStar3 for star 3; it
     * takes no copy. A Common or Rare (its stars come from copies), a troop not owned or at full stars, and too few shards are refused.
     */
    void star(ServerPlayerEntity p,Profile pr,String id){
        var t=troop(id);if(t==null)return;UUID who=p.getUuid();var r=RosterMath.rarityRow(t.rarity());
        if(r==null||!t.rarity().equals("epic")&&!t.rarity().equals("legendary")){notice(who,t.name()+" rises by copies, not shards: recruit it again.");return;}
        var owned=pr.owned.get(id);if(owned==null){notice(who,t.name()+" is not in your collection.");return;}
        if(owned.stars>=RosterMath.maxStars()){notice(who,t.name()+" is at full stars already.");return;}
        int cost=owned.stars<=1?r.shardStar2():r.shardStar3(),have=pr.shards.getOrDefault(t.house(),0);String house=RosterMath.houseName(t.house());
        if(have<cost){notice(who,"Star "+(owned.stars+1)+" of "+t.name()+" costs "+cost+" "+house+" shards. You have "+have+".");return;}
        String before=JSON.toJson(pr);pr.shards.put(t.house(),have-cost);owned.stars++;
        if(!save(who)){unsaved(who,before,"The star");return;}
        notice(who,t.name()+" rises to star "+owned.stars+"! (-"+cost+" "+house+" shards)");reveal(p);
    }
    /**
     * A House's Legendary (legendary:<house>:pact or legendary:<house>:shards, spec 4.4). The pact road spends one Pact; the shards road needs
     * recruitment.renownForLegendary Renown in that House and spends recruitment.legendaryShards of its shards, keeping the Renown. Refused when
     * the Legendary is owned already or the price is not there.
     */
    void legendary(ServerPlayerEntity p,Profile pr,String arg){
        String[] parts=arg.split(":",-1);if(parts.length!=2||!parts[1].equals("pact")&&!parts[1].equals("shards"))return;
        String house=parts[0];boolean pact=parts[1].equals("pact");UUID who=p.getUuid();
        var t=Arrays.stream(Spec.TROOPS).filter(x->x.house().equals(house)&&x.rarity().equals("legendary")).findFirst().orElse(null);if(t==null)return;
        if(pr.owned.containsKey(t.id())){notice(who,t.name()+" is in your collection already.");return;}
        var R=Spec.RECRUITMENT[0];int renown=pr.renown.getOrDefault(house,0),shards=pr.shards.getOrDefault(house,0);String name=RosterMath.houseName(house);
        if(pact&&pr.pacts<1){notice(who,"You hold no Pact. Clear every battle of a campaign chapter to earn one.");return;}
        if(!pact&&renown<R.renownForLegendary()){notice(who,t.name()+" answers at "+R.renownForLegendary()+" "+name+" Renown. You have "+renown+".");return;}
        if(!pact&&shards<R.legendaryShards()){notice(who,t.name()+" costs "+R.legendaryShards()+" "+name+" shards. You have "+shards+".");return;}
        String before=JSON.toJson(pr);
        if(pact)pr.pacts--;else pr.shards.put(house,shards-R.legendaryShards());
        pr.owned.put(t.id(),new Profile.Owned());
        if(!save(who)){unsaved(who,before,"The Legendary");return;}
        notice(who,t.name()+" joins your collection"+(pact?", sworn by your Pact!":"!"));reveal(p);
    }
    /**
     * A stopped or crashed match is not settled (MatchRules, Kort 2026-11-02): no win bonus, no shards, Renown, XP or record, and the
     * armies go back to camp. Each seated player is paid the participation gold pro-rated by the share of the match he played
     * (MatchRules.partialGold). A stop pays here; a crash never reaches here, so a progress checkpoint (see checkpoint) lets the
     * next start pay the same share from the last 30 seconds of play (payInterrupted). Modes.recover puts the map back.
     */
    void voidMatch(MatchRules.Ending ending){
        if(!b.running||MatchRules.settles(ending))return;
        if(ending()){endEnding();return;} // already settled at the ending's start: no partial pay, the round is only taken down
        System.err.println("[Graves and Glory] Match "+matchId+" was interrupted ("+ending+"): partial participation gold only; the armies return to camp.");
        payPartial(playedNow(),b.roundSeconds,practice);
        for(UUID id:participants)results.remove(id);participants.clear();
        b.message="The battle was called off. Back to the war council.";headline=b.message;
        returnLobby();
    }
    /** Seconds each human who is or was seated has played of this round: from his own seating, less any time he was away, and a player who dropped keeps what he had played. */
    Map<UUID,Long> playedNow(){
        Map<UUID,Long> played=new LinkedHashMap<>();
        for(UUID id:participants){var s=b.squads.get(id);if((s==null||!s.seated)&&!away(id))continue;played.put(id,seatedTicks(id)/20);}
        return played;
    }
    /** Pays each player the participation gold of an interrupted match, pro-rated (MatchRules.partialGold); a failed save changes nothing. */
    void payPartial(Map<UUID,Long> played,int roundSeconds,boolean wasPractice){
        int base=wasPractice?Spec.CAMPAIGN[0].practiceGold():Spec.CAMPAIGN[0].completionGold();
        for(var e:played.entrySet()){
            int gold=MatchRules.partialGold(base,e.getValue(),roundSeconds);if(gold<=0)continue;
            UUID id=e.getKey();Profile p=profile(id);String before=JSON.toJson(p);p.payGold(gold);
            if(!save(id)){profiles.put(id,Profile.restored(JSON.fromJson(before,Profile.class)));continue;}
            standing(id,"Battle called off / +"+gold+" toward gear and troop writs for the time you played");
        }
    }
    static final String PROGRESS="gravesandglory/match-progress.json";
    Path progressFile(){return b.server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).resolve(PROGRESS);}
    void deleteProgress(){try{java.nio.file.Files.deleteIfExists(progressFile());}catch(Exception e){System.err.println("[Graves and Glory] The match progress file could not be deleted: "+e);}}
    /** Every 30 seconds of a running match: what a crash would owe each player, written atomically under the world save. */
    void checkpoint(){
        JsonObject j=new JsonObject();j.addProperty("matchId",matchId);j.addProperty("practice",practice);j.addProperty("roundSeconds",b.roundSeconds);
        JsonObject played=new JsonObject();playedNow().forEach((id,sec)->played.addProperty(id.toString(),sec));j.add("played",played);JsonArray awayList=new JsonArray();for(UUID id:participants)if(away(id))awayList.add(id.toString());j.add("away",awayList);
        SafeFiles.tryWrite(progressFile(),JSON.toJson(j),"The match progress");
    }
    /** A crash left a progress file (a clean end or stop deletes it): pay each player the pro-rated share it recorded, then delete it. */
    void payInterrupted(){
        Path path=progressFile();if(!java.nio.file.Files.exists(path))return;
        try{
            JsonObject j=JsonParser.parseString(java.nio.file.Files.readString(path)).getAsJsonObject();
            Map<UUID,Long> played=new LinkedHashMap<>();
            for(var e:j.getAsJsonObject("played").entrySet())played.put(UUID.fromString(e.getKey()),e.getValue().getAsLong());
            System.err.println("[Graves and Glory] Match "+j.get("matchId").getAsString()+" was interrupted by a crash: partial participation gold for "+played.size()+" player(s).");
            payPartial(played,j.get("roundSeconds").getAsInt(),j.get("practice").getAsBoolean());
            Set<UUID> away=null;if(j.has("away")){away=new HashSet<>();for(var a:j.getAsJsonArray("away"))away.add(UUID.fromString(a.getAsString()));} // an older checkpoint has no list: nobody is stamped
            for(UUID id:JoinLedger.crashStamps(played,away,Spec.CAMPAIGN[0].minimumRewardSeconds()))AdminCommands.matched(id,nameOf(id)); // a crash after the reward minimum still counts as a match played, for players who were present
        }catch(Exception e){System.err.println("[Graves and Glory] The match progress file could not be read; no partial reward: "+e);}
        deleteProgress();
    }
    /** Releases the slot of every dropped commander whose hold has run out: the reservation ends and the seat is an ordinary bot seat. */
    void releaseExpiredHolds(){
        for(var it=vacatedUntil.entrySet().iterator();it.hasNext();){var e=it.next();
            if(!MatchRules.holdExpired(b.time,e.getValue()))continue;
            vacated.remove(e.getKey());it.remove();
            System.err.println("[Graves and Glory] The slot held for "+e.getKey()+" is released.");}
    }
    void finish(int winner){
        if(!b.running)return;
        if(ending())return; // a timeout or a second win condition during the ending never settles again
        lastWinner=winner;
        long seconds=(b.time-b.roundStarted)/20;
        timeline.sample(seconds,timelineNow(),true); // pass 3: the graph's last sample is the final score
        for(var sq:b.squads.values())if(sq.seated)sq.stats.kept=(int)b.fielded(sq).count(); // each army's living non-temporary troops at the end
        JsonArray podium=rankArmies(winner);endingLine=scoreLine(seconds);
        if(!instantEnding())sendBegin(winner); // the instant freeze goes out before the saves below, so the banner does not wait on the disk
        for(UUID id:participants){var s=b.squads.get(id);boolean present=s!=null&&s.seated&&!away(id); // at the ending, not dropped (a commander who dropped and came back is present)
            if(s==null||!s.seated)s=absentSeat.get(id);if(s==null)continue;Profile p=profile(id);String before=JSON.toJson(p);
            // Pay is pro-rated by the share of the match he was seated (Kort 2026-11-02): a commander who dropped and did not return, or came back late, gets that share of the gold, XP, shards and Renown;
            // one seated the whole match is paid in full. The win, the cleared battle and the bonus count only when he was seated for at least half of it.
            long seated=seatedTicks(id)/20;boolean eligible=seated>0&&seconds>=Spec.CAMPAIGN[0].minimumRewardSeconds();int percent=MatchRules.sharePercent(seated,seconds);
            boolean won=s.team==winner,credited=won&&MatchRules.majority(seated,seconds);final int team=s.team;boolean botOnly=activeBattle==null&&b.squads.values().stream().filter(o->o.team!=team).allMatch(o->o.bot);
            int earned=eligible?MatchRules.prorate(MatchRules.grossGold(practice,activeBattle!=null,won,credited,Spec.CAMPAIGN[0].completionGold(),Spec.CAMPAIGN[0].practiceGold(),Spec.CAMPAIGN[0].victoryGold()),seated,seconds):0;
            int priorBattles=p.progression.totalBattles; // ending polish pass 2: the contract bar's before
            int priorXp=p.progression.xp,writs0=p.troopWrits,shards0=p.gearShards;Progression.reset(p.progression,Progression.today());Progression.match(p.progression,matchId,credited,eligible,percent);p.payGold(earned);int marks=eligible?MatchRules.prorate(MatchRules.forgeMarks(Spec.CAMPAIGN[0].forgeMarks(),credited,botOnly,Spec.CAMPAIGN[0].lossMarksPct(),Spec.CAMPAIGN[0].botOnlyMarksPct()),seated,seconds):0;p.gearShards=Math.min(1_000_000,p.gearShards+marks);if(eligible)Forge.wear(p);if(seated>0)p.matches++;if(credited)p.wins++;MarchMath.Settled march=null;
            if(activeBattle!=null){ // plan 2C.3: the battle won, the cleared count and the bonus, in the same save as the gold
                march=MarchMath.settle(activeBattle,MarchMath.bonus(activeBattle,Spec.BONUSOBJECTIVES),credited,bonusTracker!=null&&bonusTracker.kept(won,(int)seconds),p.battlesWon,p.bonusClaimed,Profile.houseOrder(),Spec.CAMPAIGNBATTLES);
                p.campaignCleared=march.cleared();if(march.bonusPaid()){p.payGold(march.bonusGold());p.shards.merge(march.bonusHouse(),march.bonusShards(),Integer::sum);}}
            // The chase (spec 4.3 to 4.6), in the same save as the gold: an eligible match pays shards and Renown by the fielded banner's Houses and adds to the mastery ledger; a chapter cleared pays its Pact.
            var slots=RosterMath.houseSlots(s.banner,RosterMath.Catalog.fromSpec());
            var paid=eligible?RosterMath.prorate(RosterMath.matchRewards(slots,Houses.thresholds(Spec.HOUSES[0].id()),Profile.houseOrder(),credited),seated,seconds):NO_REWARDS; // every House has the same slot thresholds (HousesCheck)
            paid.shards().forEach((h,n)->p.shards.merge(h,n,Integer::sum));paid.renown().forEach((h,n)->p.renown.merge(h,n,Integer::sum));
            List<String> titles=eligible?ledger(p,s):List.of();List<Integer> pactChapters=pacts(p);int pacts=pactChapters.size();
            List<MatchEnding.Best> bests=eligible?MatchEnding.bests(p.bests,bestValues(liveSquad(id,s))):List.of(); // pass 3: personal bests, in the same save as the rewards
            if(JoinLedger.finished(present,seated,Spec.CAMPAIGN[0].minimumRewardSeconds()))AdminCommands.matched(id,nameOf(id)); // admin tools phase A: the first finished match, in the server's join ledger (never the profile)
            boolean saved=save(id);JsonObject report=new JsonObject();report.addProperty("id",matchId);report.addProperty("outcome",winner<0?"DRAW":s.team==winner?"VICTORY":"BATTLE COMPLETE");report.addProperty("map",Spec.MAPS[map].name());report.addProperty("seconds",seconds);report.addProperty("saved",saved);report.addProperty("eligible",eligible);report.addProperty("gold",saved?earned:0);report.addProperty("writs",saved?p.troopWrits-writs0:0);report.addProperty("gearShards",saved?p.gearShards-shards0:0);report.addProperty("xp",saved?p.progression.xp-priorXp:0);report.addProperty("beforeXp",priorXp);report.add("squads",reportSquads(s));report.add("points",reportPoints());report.add("houses",reportHouses(s,slots,saved?paid:NO_REWARDS));report.add("levels",reportLevels(s));report.add("mode",reportMode());if(activeBattle!=null)report.add("march",reportMarch(p,credited,march,pactChapters));results.put(id,report);
            // match ending (2026-11-12 spec): a loss reads DEFEAT, plus the banner line, podium, your groups, rewards rows and what is ready to claim
            if(winner>=0&&s.team!=winner)report.addProperty("outcome","DEFEAT");report.addProperty("line",endingLine);
            report.add("podium",podium.deepCopy());report.add("you",reportYou(liveSquad(id,s)));report.add("claimable",reportClaimable(p));
            JsonArray rewards=new JsonArray(); // what was saved, one row per paid item in a fixed order; the army rework adds its currencies here
            if(saved){String why=(credited?"victory":practice?"practice":"match complete")+(percent<100?" (pro-rated "+percent+"%)":"");reward(rewards,"Troop writs",p.troopWrits-writs0,why);reward(rewards,"Gear shards",p.gearShards-shards0,why);reward(rewards,"XP",p.progression.xp-priorXp,"battle progress");
                for(String h:Profile.houseOrder()){reward(rewards,RosterMath.houseName(h)+" shards",paid.shards().getOrDefault(h,0),"House slots");reward(rewards,RosterMath.houseName(h)+" Renown",paid.renown().getOrDefault(h,0),"House slots");}
                if(march!=null&&march.bonusPaid()){reward(rewards,RosterMath.houseName(march.bonusHouse())+" shards",march.bonusShards(),"bonus objective");}
                reward(rewards,pacts==1?"Pact":"Pacts",pacts,"chapter cleared");for(String t:titles)reward(rewards,"New title: "+t,1,"mastery");}
            report.add("rewards",rewards);
            if(saved)report.add("progress",reportProgress(p,priorXp,priorBattles,paid)); // ending polish pass 2: the REWARDS bars
            report.add("timeline",reportTimeline());if(saved)report.add("bests",reportBests(bests)); // pass 3: the STATS graph and the REWARDS bests
            if(!saved){profiles.put(id,Profile.restored(JSON.fromJson(before,Profile.class)));standing(id,"Match reward could not be saved. Progress was not changed; check disk space.");continue;}
            String extra=(march!=null&&march.bonusPaid()?" / bonus objective: +"+march.bonusShards()+" "+RosterMath.houseName(march.bonusHouse())+" shards, +"+march.bonusGold()+" gear shards":"")+(pacts>0?" / "+(pacts==1?"a Pact":pacts+" Pacts")+" earned":"")+(titles.isEmpty()?"":" / new title"+(titles.size()==1?" ":"s ")+String.join(", ",titles));
            standing(id,(winner<0?"Draw":s.team==winner?"Victory":"Battle complete")+" / +"+(p.troopWrits-writs0)+" troop writs / +"+(p.gearShards-shards0)+" gear shards"+(!eligible?" (reward requires 3 minutes of play)":percent<100?" (pro-rated: you were seated for "+percent+"% of the match)":"")+extra);
        }
        b.message=(winner<0?"Draw":b.side(winner).name()+" wins")+"! Back to the war council.";headline=b.message;
        Audio.reset(); // the round is over: cues still waiting for their moment (a blow's contact frame, a queued rise) are dropped so nothing plays over the sting
        if(instantEnding()||participants.stream().noneMatch(id->b.server.getPlayerManager().getPlayer(id)!=null)) // with the ending sequence the client plays the sting with its banner (EndingSounds)
        for(var p:b.server.getPlayerManager().getPlayerList())if(participants.contains(p.getUuid())){var sq=b.squads.get(p.getUuid());Audio.ending(p,winner,sq==null?-1:sq.team);} // each participant, to his own connection: the victory sting, the defeat sting for the losing side (AudioCues)
        boolean watched=participants.stream().anyMatch(id->b.server.getPlayerManager().getPlayer(id)!=null);
        if(instantEnding()||!watched)returnLobby();else beginEnding(winner); // nobody online to see it (a bot simulation, every participant dropped): straight back, as before
    }
    /**
     * The ending's start (step 1, RESULT): everything on the field freezes where it stands (no AI, no velocity; the battle tick is gated off by ending()),
     * the round's crash checkpoint is deleted (it is settled: a crash now owes nothing), and the stage's chunks are held. The round stays running.
     */
    void beginEnding(int winner){
        endingStart=b.time;endingWinner=winner;skipped.clear();endingStaged.clear();endingDone.clear();
        int rows=0;for(UUID id:participants){var r=results.get(id);if(r!=null&&r.has("rewards"))rows=Math.max(rows,r.getAsJsonArray("rewards").size());}
        endingTiming=MatchEnding.timing(Spec.ENDING[0],rows);
        for(var u:b.units){u.mob.setAiDisabled(true);u.mob.getNavigation().stop();u.mob.setTarget(null);u.mob.setNoGravity(true);u.mob.setVelocity(Vec3d.ZERO);u.mob.velocityModified=true;} // a troop in mid-leap hangs where it is (the units are discarded at the end)
        holdShots();
        deleteProgress();
        PodiumStage.ensure(b.world);
    }
    /** The instant freeze: each online participant's client shows the banner on this packet, not with the next snapshot. */
    void sendBegin(int winner){
        for(UUID id:participants){var p=b.server.getPlayerManager().getPlayer(id);if(p==null)continue;
            var sq=b.squads.get(id);if(sq==null||!sq.seated)sq=absentSeat.get(id);int team=sq==null?-1:sq.team;
            if(!net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(p,GravesAndGlory.ENDING_BEGIN))continue;
            var packet=net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();packet.writeString(winner<0?"DRAW":team==winner?"VICTORY":"DEFEAT",16);packet.writeString(endingLine,512);
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p,GravesAndGlory.ENDING_BEGIN,packet);}
        beginSentNanos=System.nanoTime();
    }
    /** Harness only: System.nanoTime when the last ending_begin packets went out. */
    static volatile long beginSentNanos=-1;
    /** Arrows and thrown weapons in flight are noClip visuals the gated-off Projectiles.tick last pushed: stop them where they are. */
    private void holdShots(){
        for(var s:b.shots)hold(s.visual());for(var l:b.landed)hold(l.visual());
    }
    private static void hold(net.minecraft.entity.Entity e){
        if(e==null||e.getVelocity().lengthSquared()==0)return;e.setNoGravity(true);e.setVelocity(Vec3d.ZERO);e.velocityModified=true;
    }
    /** One ending tick (Campaign.tick while ending()): the round clock stands still, each online participant follows the later of the clock and his skips,
     *  going to the stage at PODIUM and to camp at DONE; when the clock or every online participant is done, the round is taken down. */
    void endingTick(){
        b.roundStarted++; // the round clock stops
        for(var u:b.units)if(u.mob.getVelocity().lengthSquared()>0){u.mob.setVelocity(Vec3d.ZERO);u.mob.velocityModified=true;} // nothing carries a troop on (a knock, a charge's push)
        holdShots();
        long elapsed=b.time-endingStart;var clock=MatchEnding.step(elapsed,endingTiming);boolean all=true;
        for(UUID id:new ArrayList<>(participants)){var p=b.server.getPlayerManager().getPlayer(id);if(p==null)continue;
            var st=MatchEnding.playerStep(clock,skipped.get(id));
            if(st==MatchEnding.Step.DONE){if(endingDone.add(id))toLobby(p);continue;}
            all=false;
            if(st.ordinal()>=MatchEnding.Step.PODIUM.ordinal()&&endingStaged.add(id)){double[] v=PodiumLayout.viewSpot();p.changeGameMode(GameMode.SPECTATOR);b.relocate(p,v[0],v[1]-p.getStandingEyeHeight(),v[2],(float)v[3],(float)v[4]);}
        }
        if(all||clock==MatchEnding.Step.DONE)endEnding();
    }
    /** The ending's end: back to the room for everyone (returnLobby clears the ending state and lets the stage's chunks go). */
    void endEnding(){returnLobby();}
    /** ending:skip from a participant: his own sequence moves one step on (from the later of the clock and his last skip). */
    void skipEnding(ServerPlayerEntity p){
        UUID id=p.getUuid();if(!ending()||!participants.contains(id))return;
        skipped.put(id,MatchEnding.next(MatchEnding.playerStep(MatchEnding.step(b.time-endingStart,endingTiming),skipped.get(id))));
    }
    /** The snapshot's ending block for a participant still in the sequence, else null: his step, the ticks until it ends (more than total after a skip ahead,
     *  since a skipped-to step ends with the clock's), the step's length, his outcome, the score line and the stage centre. */
    JsonObject endingJson(UUID id){
        if(!ending()||!participants.contains(id))return null;
        long elapsed=b.time-endingStart;var st=MatchEnding.playerStep(MatchEnding.step(elapsed,endingTiming),skipped.get(id));
        if(st==MatchEnding.Step.DONE)return null;
        long end=0;for(var x:MatchEnding.Step.values()){end+=endingTiming.of(x);if(x==st)break;}
        var sq=b.squads.get(id);if(sq==null||!sq.seated)sq=absentSeat.get(id);int team=sq==null?-1:sq.team;
        JsonObject j=new JsonObject();j.addProperty("step",st.name());j.addProperty("left",Math.max(0,end-elapsed));j.addProperty("total",endingTiming.of(st));
        j.addProperty("outcome",endingWinner<0?"DRAW":team==endingWinner?"VICTORY":"DEFEAT");j.addProperty("line",endingLine);
        JsonArray stage=new JsonArray();for(double c:PodiumLayout.centre())stage.add(c);j.add("stage",stage);
        return j;
    }
    static void reward(JsonArray rows,String label,int amount,String reason){if(amount==0)return;JsonObject r=new JsonObject();r.addProperty("label",label);r.addProperty("amount",amount);r.addProperty("reason",reason);rows.add(r);}
    /** The squad whose match stats are this participant's: his seat, or the bot holding it while he is away. */
    GravesAndGlory.Squad liveSquad(UUID id,GravesAndGlory.Squad fallback){
        var s=b.squads.get(id);if(s!=null&&s.seated)return s;
        UUID stand=vacated.get(id);if(stand!=null&&b.squads.get(stand)!=null)return b.squads.get(stand);
        return fallback;
    }
    /** The House a squad's banner has most slots in (ties in Profile.houseOrder), or "". */
    static String squadHouse(GravesAndGlory.Squad s){
        var slots=RosterMath.houseSlots(s.banner,RosterMath.Catalog.fromSpec());String best="";int most=0;
        for(String h:Profile.houseOrder()){int n=slots.getOrDefault(h,0);if(n>most){most=n;best=h;}}
        return best;
    }
    static MatchEnding.Entry entry(GravesAndGlory.Squad s,int seat){var t=s.stats;return new MatchEnding.Entry(s.owner.toString(),s.team,s.bot,seat,t.kills,t.commanderDowns,t.objectiveSeconds,t.kept,t.lost);}
    /** Ranks every seated army by match score (MatchEnding.ranked) into endingRanks, and returns the podium rows: rank, name, uuid, bot, team, House, score, headline,
     *  the commander's worn gear (HEAD, CHEST, LEGS, FEET, MAINHAND, OFFHAND item ids, "" for none or a bot) and up to three troop ids. */
    JsonArray rankArmies(int winner){
        endingRanks.clear();var w=Spec.ENDING[0];
        List<GravesAndGlory.Squad> seated=new ArrayList<>();for(var s:b.squads.values())if(s.seated)seated.add(s);
        List<MatchEnding.Entry> entries=new ArrayList<>();for(int i=0;i<seated.size();i++)entries.add(entry(seated.get(i),i));
        endingLeft.clear();for(var e:vacated.entrySet())if(b.server.getPlayerManager().getPlayer(e.getKey())==null)for(var s:seated)if(s.owner.equals(e.getValue()))endingLeft.put(s,e.getKey()); // a stand-in's participant offline at finish left the battle
        Set<String> leftIds=new HashSet<>();for(var s:endingLeft.keySet())leftIds.add(s.owner.toString());
        var ranked=MatchEnding.ranked(entries,winner,w,leftIds); // pass 2: those who left rank after everyone present, on the podium and in rank
        for(int r=0;r<ranked.size();r++)endingRanks.put(seated.get(ranked.get(r).seat()),r+1);
        int mvp=MatchEnding.mvpIndex(entries,winner,w,leftIds);endingMvp=mvp<0?null:seated.get(mvp);
        endingAwards.clear();endingLines.clear();List<EndingAwards.Line> lines=new ArrayList<>();
        for(int i=0;i<seated.size();i++){var s=seated.get(i);var t=s.stats;var line=new EndingAwards.Line(s.owner.toString(),t.commanderDowns,t.commanderKills,t.captures,t.objectiveSeconds,Math.max(0,t.kills-t.commanderKills),t.kept,t.lost,MatchEnding.score(entries.get(i),w),endingLeft.containsKey(s),s.bot);lines.add(line);endingLines.put(s,line);}
        var won=EndingAwards.awards(lines);for(var s:seated){var a=won.get(s.owner.toString());if(a!=null)endingAwards.put(s,a);}
        JsonArray rows=new JsonArray();
        for(var e:MatchEnding.podium(ranked)){var s=seated.get(e.seat());JsonObject row=new JsonObject();
            row.addProperty("rank",endingRanks.get(s));row.addProperty("name",s.bot?s.tier+" bot":name(s.owner));row.addProperty("uuid",s.owner.toString());row.addProperty("bot",s.bot);row.addProperty("team",s.team);
            row.addProperty("house",squadHouse(s));row.addProperty("score",MatchEnding.score(e,w));row.addProperty("headline",MatchEnding.headlineText(e,MatchEnding.headline(e,w)));
            JsonArray gear=new JsonArray();var player=s.bot?null:b.server.getPlayerManager().getPlayer(s.owner);
            for(var slot:new net.minecraft.entity.EquipmentSlot[]{net.minecraft.entity.EquipmentSlot.HEAD,net.minecraft.entity.EquipmentSlot.CHEST,net.minecraft.entity.EquipmentSlot.LEGS,net.minecraft.entity.EquipmentSlot.FEET,net.minecraft.entity.EquipmentSlot.MAINHAND,net.minecraft.entity.EquipmentSlot.OFFHAND}){
                var stack=player==null?net.minecraft.item.ItemStack.EMPTY:player.getEquippedStack(slot);gear.add(stack.isEmpty()?"":net.minecraft.registry.Registries.ITEM.getId(stack.getItem()).toString());}
            row.add("gear",gear);
            JsonArray troops=new JsonArray();List<String> order=new ArrayList<>();for(String id:s.banner)if(troop(id)!=null&&!order.contains(id))order.add(id);
            for(String id:MatchEnding.squadTroops(s.stats.troopKills,order,3))troops.add(id);row.add("troops",troops);
            awardFields(row,s);
            rows.add(row);}
        return rows;
    }
    /** Pass 3: each team's number now in the mode's own ledger (BattleMode.ledger: Domination's scores, Siege's stages and strength, Convoy's road and stops, Survival's wave and dead). */
    int[] timelineNow(){int[] l=b.mode.ledger(b);return new int[]{l.length>0?l[0]:0,l.length>1?l[1]:0};}
    /** Pass 3, the report's timeline: {interval, label, teams: [row 0, row 1]}, each row in MatchEnding.encodeRow's compact form (the report rides the size-limited camp
     *  snapshot); a mode without points adds each row's max and format, since its two numbers are not one scale. */
    JsonObject reportTimeline(){
        JsonObject j=new JsonObject();var m=b.mode;boolean points=m.capturesPoints(b);var es=m.ledgerEntries(b);
        j.addProperty("interval",timeline.interval);j.addProperty("label",points?"Score":es.size()>=2?es.get(0).label()+" / "+es.get(1).label():m.name());
        JsonArray teams=new JsonArray();for(int t=0;t<2;t++)teams.add(MatchEnding.encodeRow(timeline.row(t)));j.add("teams",teams);
        if(!points){int[] mx=m.max(b);String[] f=m.formats();JsonArray max=new JsonArray(),format=new JsonArray();
            for(int t=0;t<2;t++){max.add(t<mx.length?mx[t]:0);format.add(t<f.length?f[t]:"plain");}j.add("max",max);j.add("format",format);}
        return j;
    }
    /** Pass 3: this match's values for the personal bests (MatchEnding.BEST_KEYS); kept alive is a percent of the army fielded, left out when fewer than MatchEnding.MIN_FIELDED_FOR_KEPT were. */
    Map<String,Integer> bestValues(GravesAndGlory.Squad s){
        var t=s.stats;Map<String,Integer> v=new LinkedHashMap<>();
        v.put("kills",t.commanderKills);v.put("armyKills",Math.max(0,t.kills-t.commanderKills));v.put("score",MatchEnding.score(entry(s,0),Spec.ENDING[0]));
        int kept=MatchEnding.keptPct(t.kept,t.lost);if(kept>=0)v.put("keptPct",kept); // left out below MIN_FIELDED_FOR_KEPT troops fielded
        v.put("captures",t.captures);v.put("commanderDowns",t.commanderDowns);return v;
    }
    static JsonArray reportBests(List<MatchEnding.Best> bests){
        JsonArray rows=new JsonArray();for(var x:bests){JsonObject row=new JsonObject();row.addProperty("key",x.key());row.addProperty("label",x.label());row.addProperty("value",x.value());row.addProperty("previous",x.previous());rows.add(row);}return rows;
    }
    /** Pass 3, Play again on REWARDS (ending:again): this participant's sequence ends now, always (the client has skipped to DONE already); in a room he is also marked Ready (readyAgain), outside one nothing more happens. */
    void playAgain(ServerPlayerEntity p){
        UUID id=p.getUuid();if(!ending()||!participants.contains(id))return;
        skipped.put(id,MatchEnding.Step.DONE);
        if(social.room(id)==null)return;
        playAgain.add(id);readyAgain();
    }
    /** Each Play again waiting on an idle room: the room's own ready command (social:ready) for him unless he is ready already; then he is forgotten. A room still playing waits for returnLobby. */
    void readyAgain(){
        for(UUID id:new ArrayList<>(playAgain)){var room=social.room(id);var p=b.server.getPlayerManager().getPlayer(id);
            if(room==null||p==null){playAgain.remove(id);continue;}
            if(room.playing)continue;
            playAgain.remove(id);if(!room.ready.contains(id))socialCommand(p,"social:ready");}
    }
    /** The report's own block for one commander (the stats screen's four groups): commander, army (with each troop type fielded), objectives, economy. */
    JsonObject reportYou(GravesAndGlory.Squad s){
        var t=s.stats;JsonObject j=new JsonObject();
        JsonObject cmd=new JsonObject();cmd.addProperty("kills",t.commanderKills);cmd.addProperty("deaths",t.deaths);cmd.addProperty("damage",t.damageDealt);j.add("commander",cmd);
        JsonObject army=new JsonObject();army.addProperty("kills",t.kills);army.addProperty("lost",t.lost);army.addProperty("kept",t.kept);
        String best="";int most=0;for(var e:t.troopKills.entrySet())if(e.getValue()>most){most=e.getValue();best=e.getKey();}army.addProperty("best",best);
        JsonArray troops=new JsonArray();
        for(var e:t.fielded.entrySet()){String id=e.getKey();long alive=b.fielded(s).filter(u->u.spec.id().equals(id)).count();
            JsonObject row=new JsonObject();row.addProperty("id",id);row.addProperty("fielded",e.getValue());row.addProperty("kills",t.troopKills.getOrDefault(id,0));row.addProperty("lost",(int)Math.max(0,e.getValue()-alive));troops.add(row);}
        army.add("troops",troops);j.add("army",army);
        JsonObject obj=new JsonObject();obj.addProperty("captures",t.captures);obj.addProperty("holdSeconds",t.objectiveSeconds);j.add("objectives",obj);
        JsonObject eco=new JsonObject();eco.addProperty("earned",t.goldEarned);eco.addProperty("spent",t.goldSpent);eco.addProperty("trained",t.trained);eco.addProperty("upgrades",t.upgrades);eco.addProperty("orders",t.orders);j.add("economy",eco);
        return j;
    }
    /** Ending polish pass 2, the REWARDS bars: {label, kind, before, after, base, next, tier} for XP toward the next chapter tier (Progression.XP_PER_TIER),
     *  each House's Renown toward its next Renown level (renown sheet points; only Houses that earned Renown), and the battle-count contract this match
     *  moved (the daily "Complete 3 battles", else the weekly six). A bar already at its last threshold is left out. */
    static JsonArray reportProgress(Profile p,int priorXp,int priorBattles,RosterMath.Rewards paid){
        JsonArray rows=new JsonArray();var s=p.progression;
        int[] xp=new int[Progression.TIERS];for(int i=0;i<xp.length;i++)xp[i]=Progression.XP_PER_TIER*(i+1);
        progressRow(rows,MatchEnding.progress("XP","xp",priorXp,s.xp,xp));
        int[] renown=Arrays.stream(Spec.RENOWN).sorted(Comparator.comparingInt(Spec.RenownRow::level)).mapToInt(Spec.RenownRow::points).toArray();
        for(String h:Profile.houseOrder()){int got=paid.renown().getOrDefault(h,0);if(got<=0)continue;int now=p.renown.getOrDefault(h,0);
            progressRow(rows,MatchEnding.progress(RosterMath.houseName(h)+" Renown","renown",now-got,now,renown));}
        int moved=Math.max(0,s.totalBattles-priorBattles);
        if(moved>0){
            if(s.dailyBattles-moved<Progression.target(s,1))progressRow(rows,MatchEnding.progress(Progression.contract(s,1),"contract",s.dailyBattles-moved,s.dailyBattles,new int[]{Progression.target(s,1)}));
            else progressRow(rows,MatchEnding.progress("Weekly: "+Progression.WEEKLY_BATTLES+" battles","contract",s.weeklyBattles-moved,s.weeklyBattles,new int[]{Progression.WEEKLY_BATTLES}));}
        return rows;
    }
    static void progressRow(JsonArray rows,MatchEnding.Progress g){
        if(g==null)return;JsonObject row=new JsonObject();row.addProperty("label",g.label());row.addProperty("kind",g.kind());row.addProperty("before",g.before());row.addProperty("after",g.after());
        row.addProperty("base",g.base());row.addProperty("next",g.next());row.addProperty("tier",g.tier());
        if(g.multi()){row.addProperty("crossings",g.crossings());row.addProperty("finalBase",g.finalBase());row.addProperty("finalNext",g.finalNext());row.addProperty("finalTier",g.finalTier());}rows.add(row); // two or more thresholds in one match: the bar ends on the last climb
    }
    /** Contracts (daily slots and the weekly) and chapter tiers ready to claim now (Progression.claimable). */
    static JsonObject reportClaimable(Profile p){
        var s=p.progression;int contracts=0,tiers=0;
        for(int i=0;i<3;i++)if(Progression.claimable(s,"daily:"+i))contracts++;
        if(Progression.claimable(s,"weekly"))contracts++;
        for(int l=1;l<=Progression.TIERS;l++)if(Progression.claimable(s,Progression.SEASON+":"+l))tiers++;
        JsonObject j=new JsonObject();j.addProperty("contracts",contracts);j.addProperty("tiers",tiers);return j;
    }
    /** The final score line: points modes "Dawn 300 / Dusk 214" (the sides' names), other modes their first ledger entry; then the map and the round time. */
    String scoreLine(long seconds){
        var m=b.mode;String head;
        if(m.capturesPoints(b))head=b.side(0).name()+" "+b.scores[0]+" / "+b.side(1).name()+" "+b.scores[1];
        else{var es=m.ledgerEntries(b);
            if(!es.isEmpty()){var e=es.get(0);head=(e.label()==null||e.label().isBlank()?"":e.label()+" ")+e.text();}
            else{int[] l=m.ledger(b),mx=m.max(b);String[] f=m.formats();head=l.length==0?m.name():ModeMath.ledgerText(f.length>0?f[0]:"plain",l[0],mx.length>0?mx[0]:0);}}
        return head+" \u00b7 "+Spec.MAPS[map].name()+" \u00b7 "+seconds/60+":"+(seconds%60<10?"0":"")+seconds%60;
    }
    static final RosterMath.Rewards NO_REWARDS=new RosterMath.Rewards(Map.of(),Map.of());
    /**
     * The mastery ledger of one eligible match (spec 4.6): each troop type's kills this round (SquadStats.troopKills) and +1 survived for each
     * type with a living troop of the army at the end (a temporary one is none), then each owned troop's Mastery level (RosterMath.masteryLevel;
     * it never falls). A level reached grants the title part of its mastery reward as "<troop> <title>" (Order Squire Veteran); the other parts,
     * skins and frames, wait for their task. A House whose troops (houses.troops) are all owned at Mastery I or higher grants the House's title.
     * Returns the titles new to the profile, for the notice.
     */
    List<String> ledger(Profile p,GravesAndGlory.Squad s){
        s.stats.troopKills.forEach((troop,n)->{var o=p.owned.get(troop);if(o!=null)o.kills=Math.min(1_000_000,o.kills+n);});
        b.fielded(s).map(u->u.spec.id()).distinct().forEach(troop->{var o=p.owned.get(troop);if(o!=null)o.survived++;});
        List<String> gained=new ArrayList<>();
        for(var e:p.owned.entrySet()){var o=e.getValue();var t=troop(e.getKey());int level=RosterMath.masteryLevel(o.kills,o.survived,Spec.MASTERY);
            if(t==null||level<=o.mastery)continue;
            for(var row:Spec.MASTERY)if(row.level()>o.mastery&&row.level()<=level){String title=masteryTitle(row.reward());if(title!=null&&p.progression.titles.add(t.name()+" "+title))gained.add(t.name()+" "+title);}
            o.mastery=level;}
        for(var h:Spec.HOUSES){
            if(h.title().isBlank()||!Arrays.stream(h.troops().split(",")).allMatch(troop->{var o=p.owned.get(troop);return o!=null&&o.mastery>=1;}))continue;
            if(p.progression.titles.add(h.title()))gained.add(h.title());}
        return gained;
    }
    /** The title a mastery reward gives (its title: part, "title:Storied" or "frame:brass;title:Master"), or null when it gives none. */
    static String masteryTitle(String reward){for(String part:reward.split(";"))if(part.startsWith("title:")&&part.length()>6)return part.substring(6);return null;}
    /** A Pact (spec 4.4) for each Long March chapter whose every battle is cleared (campaignBattles order up to campaignCleared) and that has not paid one yet (writChapters). Returns the chapters granted. */
    static List<Integer> pacts(Profile p){
        var chapters=RosterMath.pactChapters(Arrays.asList(Spec.CAMPAIGNBATTLES),p.campaignCleared,p.writChapters);
        for(int chapter:chapters){p.pacts++;p.writChapters.add(chapter);}
        return chapters;
    }
    /**
     * The report's Long March block (plan 2C.5), read by the War Table report: the battle, whether it was won, how its bonus ended ("paid", "claimed" earlier,
     * "missed", "lost" with the battle, or "none" without a bonus row), what the bonus pays, the Pacts this battle earned and the Pacts now held. Display only;
     * every figure was settled before this call.
     */
    JsonObject reportMarch(Profile p,boolean won,MarchMath.Settled march,List<Integer> granted){int pacts=granted.size();
        JsonObject j=new JsonObject();var bonus=MarchMath.bonus(activeBattle,Spec.BONUSOBJECTIVES);
        j.addProperty("id",activeBattle.id());j.addProperty("name",activeBattle.name());j.addProperty("order",activeBattle.order());j.addProperty("won",won);
        String state=bonus==null?"none":!won?"lost":march!=null&&march.bonusPaid()?"paid":p.bonusClaimed.contains(bonus.id())?"claimed":"missed";
        j.addProperty("bonus",bonus==null?"":bonus.text());j.addProperty("bonusState",state);j.addProperty("bonusShards",bonus==null?0:bonus.shards());j.addProperty("bonusGold",bonus==null?0:bonus.gold());
        j.addProperty("house",activeBattle.house());j.addProperty("pacts",pacts);j.addProperty("pactsHeld",p.pacts);
        j.addProperty("after",MarchMath.afterLine(activeBattle,won));j.addProperty("chapterEnd",MarchMath.chapterEndLine(granted,Spec.CAMPAIGNCHAPTERS)); // brief N2: the win's line, and the chapter's end line with its Pact
        return j;
    }
    /** The report's House rows (MatchStats.houseRows): every House the fielded banner has slots in, its banner tier, its trait seconds this round, and what the match paid it (0 when nothing was saved). */
    JsonArray reportHouses(GravesAndGlory.Squad s,Map<String,Integer> slots,RosterMath.Rewards paid){
        JsonArray rows=new JsonArray();
        for(var h:MatchStats.houseRows(Profile.houseOrder(),slots,Houses.bannerTiers(s),s.stats.traitSeconds,paid.shards(),paid.renown())){
            JsonObject row=new JsonObject();row.addProperty("house",h.house());row.addProperty("tier",h.tier());row.addProperty("traitSeconds",h.traitSeconds());row.addProperty("shards",h.shards());row.addProperty("renown",h.renown());rows.add(row);}
        return rows;
    }
    /**
     * The report's levels rows (in-match progression design 7; Veterans.levelRows): one per banner troop type in banner order, the highest level a soldier of it reached this
     * round, how many reached level 3, and the weapon and armour tier bought (1: none). Built before returnLobby clears the Armoury. Empty with progression off.
     */
    JsonArray reportLevels(GravesAndGlory.Squad s){
        JsonArray rows=new JsonArray();if(!Veterans.enabled())return rows;
        for(var r:Veterans.levelRows(b,s)){JsonObject row=new JsonObject();row.addProperty("troop",r.troop());row.addProperty("highestLevel",r.highest());row.addProperty("level3Count",r.level3s());row.addProperty("weapon",r.weapon());row.addProperty("armour",r.armour());rows.add(row);}
        return rows;
    }
    /** The report table for one commander: every seated army of both teams, his own team first (join order kept); a bot is named after its tier. */
    JsonArray reportSquads(GravesAndGlory.Squad me){
        JsonArray rows=new JsonArray();
        for(boolean mine:new boolean[]{true,false})for(var s:b.squads.values())if(s.seated&&(s.team==me.team)==mine){
            JsonObject row=new JsonObject();row.addProperty("name",s.bot?s.tier+" bot":name(s.owner));row.addProperty("team",s.team);row.addProperty("bot",s.bot);row.addProperty("tier",s.bot?s.tier:"");row.addProperty("you",s==me);
            row.addProperty("kills",s.stats.kills);row.addProperty("lost",s.stats.lost);row.addProperty("gold",s.stats.goldEarned);row.addProperty("objective",s.stats.objectiveSeconds);
            int rank=endingRanks.getOrDefault(s,0);row.addProperty("uuid",s.owner.toString());row.addProperty("house",squadHouse(s));row.addProperty("score",MatchEnding.score(entry(s,0),Spec.ENDING[0]));
            row.addProperty("ck",s.stats.commanderKills);row.addProperty("cd",s.stats.deaths);row.addProperty("downs",s.stats.commanderDowns);row.addProperty("kept",s.stats.kept);row.addProperty("mvp",rank==1);row.addProperty("rank",rank);rows.add(row);
            awardFields(row,s);
        }
        return rows;
    }
    /** Ending polish pass 1, on a squads row or a podium row: the MVP (the best ranked army whose commander stayed), its award title and detail, and for a
     *  stand-in seat whose commander left the battle, left and his name. */
    void awardFields(JsonObject row,GravesAndGlory.Squad s){
        row.addProperty("mvp",s==endingMvp);
        var a=endingAwards.get(s);var line=endingLines.get(s);if(a!=null&&line!=null){row.addProperty("award",a.title);row.addProperty("awardText",EndingAwards.detail(a,line));}
        UUID gone=endingLeft.get(s);if(gone!=null){row.addProperty("left",true);row.addProperty("name",name(gone));row.addProperty("bot",false);}
    }
    /** Seconds each team held each point this round: one entry per battle point, so five on Kingsfield. */
    JsonArray reportPoints(){
        JsonArray rows=new JsonArray();int[][] held=b.pointSeconds;
        for(int i=0;i<b.points.size();i++){JsonObject row=new JsonObject();row.addProperty("name",b.points.get(i).spec.name());row.addProperty("dawn",i<held[0].length?held[0][i]:0);row.addProperty("dusk",i<held[1].length?held[1][i]:0);rows.add(row);}
        return rows;
    }
    /** The report's mode block (the 4F.2 report): the round's mode id, name, rule line, whether it plays the capture points, and its two ledger numbers with their maxima and formats, as the lobby's mode block gives them (BattleMode.snapshot). The report screen reads it to give each mode a section of its own (ReportSections); a client that does not know the field ignores it. */
    JsonObject reportMode(){
        JsonObject j=new JsonObject();var m=b.mode;
        j.addProperty("id",m.id());j.addProperty("name",m.name());j.addProperty("rule",m.rule());j.addProperty("points",m.capturesPoints(b));
        JsonArray ledger=new JsonArray(),max=new JsonArray(),format=new JsonArray();
        var entries=m.ledgerEntries(b); // the mode's ledger rows (ModeLines.Entry); a mode without them falls back to its ledger/max/formats arrays
        if(!entries.isEmpty())for(var e:entries){ledger.add(e.value());max.add(e.max());format.add(e.format());}
        else{for(int n:m.ledger(b))ledger.add(n);for(int n:m.max(b))max.add(n);for(String f:m.formats())format.add(f);}
        j.add("ledger",ledger);j.add("max",max);j.add("format",format);
        return j;
    }
    /** Harness cameras: while false, the lobby leashes here and in the build tick leave every player where he stands. Always true in play. */
    boolean leash=true;
    void tick(){
        if(b.time%20==0)expireNotices();
        if(b.running&&!ending()){BattleEffects.flickers(b);if(b.time%20==0)releaseExpiredHolds();if(b.time%600==0)checkpoint();} // the ending is settled: no checkpoint, so a crash in it pays nothing twice
        if(pendingBattle!=null&&b.ready){ // the map of a campaign battle is built: it starts unless something changed meanwhile
            var row=pendingBattle;UUID id=pendingCommander;pendingBattle=null;pendingCommander=null;
            var host=b.server.getPlayerManager().getPlayer(id);String refused=host==null?"":soloRefusal(id,MARCH_SOLO);if(host!=null&&refused.isEmpty())refused=bannerRefusal(id,row); // the banner may have changed while the map built
            if(host!=null&&refused.isEmpty()&&row.map()==map)soloStart(host,row);
            else if(host!=null)notice(id,refused.isEmpty()?"The battlefield changed. "+row.name()+" did not start.":refused);
        }
        if(!preparingRoom.isEmpty()&&b.ready&&!b.running){
            var pending=social.rooms.get(preparingRoom);String code=preparingRoom;preparingRoom="";
            if(pending!=null){var host=b.server.getPlayerManager().getPlayer(pending.leader);
                if(host!=null&&pending.map==map&&social.startFailure(pending.leader,false).isEmpty())socialCommand(host,"social:start");
                else for(UUID member:pending.members)notice(member,"Map prepared. Match cancelled because readiness or room membership changed.");}
        }
        if(b.time%20==0)social.presence(new HashSet<>(b.server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList()),System.currentTimeMillis());
        if(!b.running){for(var p:b.server.getPlayerManager().getPlayerList())if(leash&&!CampWorld.inCamp(p.getX(),p.getY(),p.getZ()))toLobby(p);if(b.time%10==0)roomCountdown();return;}
        // Unseated lobby players wait in the hall while a match runs.
        if(b.time%20==0)for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(leash&&(s==null||!s.seated)&&!CampWorld.inCamp(p.getX(),p.getY(),p.getZ()))toLobby(p);}
        if(ending()){endingTick();return;}
        long elapsed=(b.time-b.roundStarted)/20;if(timeline.due(elapsed))timeline.sample(elapsed,timelineNow(),false); // pass 3: the STATS graph, every interval of battle time (the ledger is read only when a sample is due)
        if(b.time<introUntil){for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(s!=null&&s.seated&&p.squaredDistanceTo(b.fieldBase(s.team))>2)b.toBase(p,s.team);}return;}
        boolean economy=b.time%100==0;
        for(var s:b.squads.values())if(s.bot){
            // A dropped commander's army keeps its last orders for disconnectGraceSeconds; then its bot takes command (MatchRules).
            if(MatchRules.waiting(b.time,s.takeoverAt))continue;if(s.takeoverAt>0){s.takeoverAt=0;s.order="attack";}
            // It also decides on the intro's last tick: the round starts it on Attack toward its own base, and until its first beat (up to decisionTicks later) its
            // troops formed up round the base by a stale facing, then turned about for the real spot (Kort 2026-11-11, "walk 5 blocks ... then turn around").
            var tier=tier(s);boolean decide=b.time%Math.max(1,tier.decisionTicks())==0||b.time==introUntil;
            if(!economy&&!decide)continue;
            if(economy){
                long living=b.fielded(s).count();
                if(living==0){long until=botRespawns.computeIfAbsent(s.owner,k->b.time+240);if(b.time<until)continue;}else botRespawns.remove(s.owner);
                // Buy toward the banner: each troop up to its count there, while fielded and queued troops stay below rules.maxTroops.
                var wanted=WarGrid.buyOrder(s.banner,s.line); // the line's order, not the table's cells: role placement must not change what a bot trains first
                long army=b.fielded(s).count()+s.training.size();
                for(var want:wanted.entrySet()){var type=troop(want.getKey());if(type==null)continue;
                    long have=b.fielded(s).filter(u->u.spec==type).count()+s.training.stream().filter(q->q.spec()==type).count();
                    int price=type.cost();
                    while(have<want.getValue()&&s.gold>=price&&army<RosterMath.maxTroops()){s.gold-=price;s.stats.goldSpent+=price;long previous=s.training.isEmpty()?b.time:s.training.getLast().finished();s.training.addLast(new GravesAndGlory.Training(type,Math.max(b.time,previous)+Commander.trainingTicks(type),price));army++;have++;}
                }
                Veterans.botBuy(b,s); // then gear, from what the refill left, keeping its tier's reserve for rebuys (in-match progression design 9.1)
            }
            if(decide)botDecide(s,tier);
        }
    }
    /** The command row for a bot's difficulty (its tier id); Normal when the id is unknown. */
    Spec.BotCommandRow tier(GravesAndGlory.Squad s){
        for(var row:Spec.BOTCOMMAND)if(row.id().equals(s.tier))return row;
        for(var row:Spec.BOTCOMMAND)if(row.id().equals("normal"))return row;
        return Spec.BOTCOMMAND[0];
    }
    /** Test-tools hook (the AI "why" log, BotSimulation): null in play. When set, botDecide reports each finished decision to it as its last step: the army, its tier, the inputs the decision read and the point it chose. It reads only; it changes nothing. */
    interface DecisionListener{void decided(Campaign c,GravesAndGlory.Squad s,Spec.BotCommandRow tier,Vec3d center,int living,int full,java.util.Collection<GravesAndGlory.Battle.Sight> seen,int enemies,boolean couldCharge,GravesAndGlory.Capture choice,int hold,boolean retreating,String wasOrder,Vec3d wasTarget);}
    static volatile DecisionListener decisionListener;
    /** A remembered enemy counts as near a point, or near an army, within this many times the point's radius or the tier's charge distance. */
    static final double NEAR_RADIUS_FACTOR=2;
    // Bot-AI plan step 2 (docs/reports/ai-audit-and-design-2026-11-09.md section 8 row 2). Code constants, not sheet rows: they shape the bot's judgement, not a rule players meet.
    /** An army is in contact while an enemy its team has seen stands within this of one of its troops (Attack's engage radius), or one of its groups was shot at within UNDER_FIRE_TICKS (Squad.shotAt). */
    static final double CONTACT_RADIUS=24;static final long UNDER_FIRE_TICKS=60;
    /** An owned point is worth a guard while it is contested or an enemy its team has seen stands within GUARD_THREAT_RADIUS of it; the nearest army within GUARD_REACH leaves one group there. */
    static final double GUARD_THREAT_RADIUS=60,GUARD_REACH=90;
    /** Refills (musterRefill, musterRelease): none gather when they appear within MUSTER_NEAR of their army; otherwise they gather at a rally MUSTER_BACK blocks behind it, toward them,
     *  and move up together once MUSTER_COUNT stand within MUSTER_GATHERED of one another or MUSTER_WAIT_TICKS after the first reached the rally (MUSTER_CAP_TICKS after the first
     *  appeared at the latest). The check runs at each decision, so an Easy bot (8 s) sends them later than a Hard one (3 s). */
    static final double MUSTER_NEAR=30,MUSTER_BACK=30,MUSTER_GATHERED=10;static final int MUSTER_COUNT=3;static final long MUSTER_WAIT_TICKS=160,MUSTER_CAP_TICKS=900;
    /** Facing: an army turns only when the new facing is more than FACE_TURN degrees off its last; within FACE_ARRIVE of its spot it faces the nearest enemy its team has seen within FACE_ENEMY of the spot. */
    static final double FACE_TURN=30,FACE_ARRIVE=16,FACE_ENEMY=48;
    /** Per bot army (owner): the point its guard group holds (botDecide), and when its first refill reached the rally (musterRelease). */
    final Map<UUID,Integer> botGuard=new HashMap<>();final Map<UUID,Long> musterFirstAt=new HashMap<>();
    /** The last decision's point costs, for the decision listener's why log; written only while a listener is set. */
    String botWhy="";
    /** One command decision for a bot army, run at its tier's cadence: retreat or rally (never into a retreat while in contact), pick the point to take, set the order, the facing and its groups' spots, stamp the charge (Battle.markCharge; a bot's command plays no sound). A new Charge starts only when the squad's Charge cooldown is ready (OrderRules.useCharge).
     *  Every tier reads only what its team has seen (b.sightings) for its charge, its contact and its guards; point owners and contested flags are what the HUD shows. A team's armies spread over the points (BattleMath.pickPoint), and one group stays on a taken point only while an enemy is near it. */
    void botDecide(GravesAndGlory.Squad s,Spec.BotCommandRow tier){
        var all=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).toList();
        long living=all.size();if(living==0){s.muster=null;return;}
        var front=all.stream().filter(u->u.musterSince<0).toList();var mine=front.isEmpty()?all:front; // the army proper; refills at the rally join it later
        Vec3d center=Morale.centre(mine);
        // One live view of the enemy for the whole decision: the living enemy units, and only the sightings of units that are still alive.
        Map<UUID,GravesAndGlory.Unit> foes=new HashMap<>();for(var u:b.units)if(u.squad.team!=s.team&&u.mob.isAlive())foes.put(u.mob.getUuid(),u);
        Map<UUID,GravesAndGlory.Battle.Sight> seen=new HashMap<>();for(var entry:b.sightings.getOrDefault(s.team,Map.of()).entrySet())if(foes.containsKey(entry.getKey()))seen.put(entry.getKey(),entry.getValue());
        int count=b.points.size();GravesAndGlory.Capture choice;
        int full=RosterMath.units(s.banner,RosterMath.Catalog.fromSpec()); // the units its banner lists, an Epic or a Legendary once
        boolean contact=inContact(s,mine,seen.values());
        if(!b.mode.botRetreats(b,s))botRetreating.remove(s.owner); // a mode can deny the fall-back (Siege's defenders behind the gate)
        else if(living<=full*tier.retreatShare()){if(!contact)botRetreating.add(s.owner);} // disengage only out of contact (audit D4): an army in a fight fights on
        else if(living>=full*tier.rallyShare())botRetreating.remove(s.owner);
        boolean retreating=botRetreating.contains(s.owner);
        musterRelease(s,all,front,retreating);
        BattlePlan.Assignment plan=retreating?null:BattleMapNet.jobFor(b,s); // the battle map's plan for this army (a pure query), applied below once the mode has had its say; retreat above keeps priority
        int hold=tier.flankLeastDefended()?garrisonPoint(s,center,tier):-1,current=-1;
        if(hold>=0)choice=b.points.get(hold);
        else if(tier.flankLeastDefended()){
            // Every point of the current map counts (Kingsfield has five); a point is defended by the enemies this team last saw near it.
            double[] px=new double[count],pz=new double[count];int[] owner=new int[count],defenders=new int[count];
            for(int i=0;i<count;i++){var p=b.points.get(i);px[i]=p.spec.x();pz[i]=p.spec.z();owner[i]=p.owner;
                for(var sight:seen.values())if(Math.hypot(sight.x()-px[i],sight.z()-pz[i])<=NEAR_RADIUS_FACTOR*p.spec.radius())defenders[i]++;}
            choice=b.points.get(Math.max(0,BattleMath.leastDefended(center.x,center.z,px,pz,owner,defenders,s.team,tier.defenderWeight())));
        }else{
            // The points plan: a team's armies spread (each other bot army sent to a point makes it dearer, audit D1); a taken point is left for the next (D2) unless it is contested.
            int lane=BattleMath.laneTarget(s.lane,s.team,count); // Dusk's lane points mirror Dawn's; GravesAndGlory.reinforce mirrors its rows to match
            double[] px=new double[count],pz=new double[count];int[] owner=new int[count],claims=new int[count];boolean[] contested=new boolean[count];
            for(int i=0;i<count;i++){var p=b.points.get(i);px[i]=p.spec.x();pz[i]=p.spec.z();owner[i]=p.owner;contested[i]=p.contested;if(on(s.target,p))current=i;}
            for(var army:b.squads.values())if(army!=s&&army.bot&&army.seated&&army.team==s.team&&!botRetreating.contains(army.owner))
                for(int i=0;i<count;i++)if(on(army.target,b.points.get(i))||botGuard.getOrDefault(army.owner,-1)==i)claims[i]++;
            choice=b.points.get(Math.max(0,BattleMath.pickPoint(center.x,center.z,px,pz,owner,contested,s.team,lane,claims,current)));
            if(decisionListener!=null){StringBuilder why=new StringBuilder("pick");for(int i=0;i<count;i++)why.append(';').append(b.points.get(i).spec.name().replace(',',' ')).append(':').append(String.format(Locale.ROOT,"%.0f",BattleMath.pointValue(center.x,center.z,px[i],pz[i],owner[i],contested[i],s.team,i==lane,claims[i],i==current))).append(claims[i]>0?"(claimed"+claims[i]+")":"");botWhy=why.toString();}
        }
        // Easy and Normal weigh a charge against every enemy their team has seen (b.sightings), never against every enemy on the map, and charge only with one in sight within scouting range of the army.
        long enemies=seen.size();double nearest=Double.MAX_VALUE;long near=0;
        for(var sight:seen.values()){double d=Math.hypot(sight.x()-center.x,sight.z()-center.z);nearest=Math.min(nearest,d);if(d<=NEAR_RADIUS_FACTOR*tier.chargeDistance())near++;}
        if(nearest>Spec.SCOUTING[0].range())enemies=0; // nothing in front of it to charge
        boolean couldCharge=b.startsCharge(s,"all");
        String wasOrder=s.order;Vec3d wasTarget=s.target;s.assignments.clear();
        Vec3d goal=null;
        if(retreating){s.order=contact?"attack":"retreat";s.formation="loose";s.target=b.fieldBase(s.team);} // caught on the way back it fights its way home (Attack toward the base), never walks off under blows
        else{
            goal=b.mode.botGoal(b,s,center,tier); // a mode's own goal (Convoy's wagon), else the chosen point
            s.order=goal!=null||choice.owner!=s.team||choice.contested?"attack":"hold";s.formation=living<8?"loose":"block";s.target=goal!=null?goal:spot(choice);
            if(plan!=null&&goal==null){ // a player's plan replaces the point choice (and the guard below); only the goal changes, never routing or movement
                var pt=plan.point()>=0&&plan.point()<count?b.points.get(plan.point()):null;Vec3d at=null;String order="hold";
                switch(plan.job()){
                    case FOLLOW->{var who=BattleMapNet.followed(b,s);at=who==null?null:who.getPos();}
                    case ATTACK->{at=pt!=null?spot(pt):place(plan.x(),plan.z());order="attack";}
                    case DEFEND->{at=pt!=null?spot(pt):place(plan.x(),plan.z());double r=(pt!=null?pt.spec.radius():0)+PLAN_DEFEND_REACH;
                        for(var sight:seen.values())if(Math.hypot(sight.x()-at.x,sight.z()-at.z)<=r){order="attack";break;}}
                    case HOLD->at=pt!=null?spot(pt):place(plan.x(),plan.z());
                }
                if(at==null)plan=null;else{s.order=order;s.target=at;if(pt!=null)choice=pt;}
            }
            if(s.order.equals("attack")){
                if(tier.chargeDistance()>0){
                    // Timed charge: judged on the enemies this team has seen, counted near the army, and only once the nearest is in range.
                    if(near>0&&BattleMath.timedCharge(nearest,living,near,tier.chargeRatio(),tier.chargeDistance()))s.order="charge";
                }else if(enemies>0&&BattleMath.timedCharge(0,living,enemies,tier.chargeRatio(),0))s.order="charge";
            }
        }
        Vec3d objective=retreating?null:b.mode.botTarget(b,s); // a mode's own objective (Siege's stage) replaces the point plan
        if(!retreating)BattleMapNet.noted(b,s,goal==null&&objective==null?plan:null); // the bot says what it was asked, once (nothing when the mode's own aim overrides it)
        if(objective!=null){s.target=objective;if(b.mode.howToSide(b,s).equals("defend"))s.order="hold";else if(!s.order.equals("charge"))s.order="attack";}
        if(s.order.equals("charge")&&couldCharge&&!OrderRules.useCharge(b,s))s.order="attack"; // a new Charge needs the commander's cooldown (OrderRules.chargeReady, markChargeUsed); not ready, the army attacks its objective. A bot never hunts: only a player's Attack does (Assignment.hunt).
        // Guard (audit D2): on the points plan, the army nearest a threatened point its team owns leaves one group there; an army of one group holds it whole.
        boolean points=!retreating&&goal==null&&objective==null&&!tier.flankLeastDefended()&&b.mode.capturesPoints(b);
        int guard=points&&plan==null?guardPoint(s,center,seen.values()):-1; // a plan's Defend or Hold stands in for the guard; the else below drops only this army's own claim
        String guardGroup=null;
        if(guard>=0){
            var at=b.points.get(guard);boolean here=b.points.indexOf(choice)==guard;
            guardGroup=here?null:guardGroup(mine);
            if(guardGroup==null&&!here){choice=at;s.target=spot(at);s.order=at.contested?"attack":"hold";}
            botGuard.put(s.owner,guard);
        }else botGuard.remove(s.owner);
        if(guard>=0)hold=guard;
        s.yaw=b.mode instanceof SiegeMode?(s.team==0?-90:90):facing(s,center,seen.values(),retreating); // Siege lays its stage plan out along the x axis, so it keeps facing the enemy's side
        if(points)planGroups(s,center,mine,guard,guardGroup,seen.values());
        if(!retreating)b.mode.botPlan(b,s,tier); // a mode's per-group split of the order (Siege's stage plan)
        if(!s.order.equals(wasOrder)||!Objects.equals(s.target,wasTarget))s.arrived.clear(); // a new order or spot ends an arrival (Tactics.arrived); the same decision again keeps it
        if(s.order.equals("charge")&&couldCharge)b.markCharge(s,"all");
        var listener=decisionListener;if(listener!=null)listener.decided(this,s,tier,center,(int)living,full,seen.values(),(int)enemies,couldCharge,choice,hold,retreating,wasOrder,wasTarget); // dev hook, null in play
    }
    /** How far past a planned point's radius a sighting makes a Defend plan attack instead of hold (spec 5.1). */
    static final double PLAN_DEFEND_REACH=16;
    /** A place a plan names: its block column's ground, a block over. */
    Vec3d place(double x,double z){return GravesAndGlory.Battle.groundAt(x,z);}
    /** The spot an order to a capture point sends a group to: its centre, a block over the ground. */
    static Vec3d spot(GravesAndGlory.Capture p){return new Vec3d(p.spec.x(),p.spec.elevation()+1,p.spec.z());}
    /** Whether an order's target is this point (within 2 blocks of its centre). */
    static boolean on(Vec3d target,GravesAndGlory.Capture p){return target!=null&&Math.hypot(target.x-p.spec.x(),target.z-p.spec.z())<=2;}
    /** Contact (audit D4): an enemy its team has seen within CONTACT_RADIUS of one of the army's troops, or a group shot at within UNDER_FIRE_TICKS. */
    boolean inContact(GravesAndGlory.Squad s,List<GravesAndGlory.Unit> mine,Collection<GravesAndGlory.Battle.Sight> seen){
        for(long at:s.shotAt.values())if(b.time-at<=UNDER_FIRE_TICKS)return true;
        for(var sight:seen)for(var u:mine)if(Math.hypot(sight.x()-u.mob.getX(),sight.z()-u.mob.getZ())<=CONTACT_RADIUS)return true;
        return false;
    }
    /** The owned point this army keeps a guard on, or -1. A point needs one while it is contested or a seen enemy stands within GUARD_THREAT_RADIUS of it, and has none
     *  while another army of the team guards it (a bot's guard, a bot army holding it whole, or a player's troops on it). An army keeps the guard it left while the threat lasts;
     *  a new guard comes from the nearest free bot army within GUARD_REACH. */
    int guardPoint(GravesAndGlory.Squad s,Vec3d center,Collection<GravesAndGlory.Battle.Sight> seen){
        int was=botGuard.getOrDefault(s.owner,-1),best=-1;double bestDistance=Double.MAX_VALUE;
        Map<GravesAndGlory.Squad,List<GravesAndGlory.Unit>> armies=new HashMap<>();
        for(var u:b.units)if(u.squad!=s&&u.squad.team==s.team&&u.squad.seated&&u.mob.isAlive()&&u.musterSince<0)armies.computeIfAbsent(u.squad,k->new ArrayList<>()).add(u);
        for(int i=0;i<b.points.size();i++){var p=b.points.get(i);
            if(p.owner!=s.team)continue;
            boolean threat=p.contested;for(var sight:seen)if(!threat&&Math.hypot(sight.x()-p.spec.x(),sight.z()-p.spec.z())<=GUARD_THREAT_RADIUS)threat=true;
            if(!threat)continue;
            boolean guarded=false;double d=i==was?0:Math.hypot(center.x-p.spec.x(),center.z-p.spec.z());
            for(var e:armies.entrySet()){var army=e.getKey();
                if(army.bot){if(botRetreating.contains(army.owner))continue;
                    if(botGuard.getOrDefault(army.owner,-1)==i||army.order.equals("hold")&&on(army.target,p)){guarded=true;break;}
                    if(i!=was&&!botGuard.containsKey(army.owner)&&Morale.centre(e.getValue()).distanceTo(new Vec3d(p.spec.x(),Morale.centre(e.getValue()).y,p.spec.z()))<d){guarded=true;break;} // a nearer free bot army leaves the guard
                }else for(var u:e.getValue())if(Math.hypot(u.mob.getX()-p.spec.x(),u.mob.getZ()-p.spec.z())<=p.spec.radius()){guarded=true;break;}
                if(guarded)break;
            }
            if(guarded||d>GUARD_REACH)continue;
            if(d<bestDistance){bestDistance=d;best=i;}
        }
        return best;
    }
    /** The group this army leaves as a guard (BattleMath.guardGroup), or null: it is one group. */
    static String guardGroup(List<GravesAndGlory.Unit> mine){
        Map<String,Integer> n=new HashMap<>();for(var u:mine)n.merge(u.spec.group(),1,Integer::sum);
        return BattleMath.guardGroup(n.getOrDefault("infantry",0),n.getOrDefault("ranged",0),n.getOrDefault("cavalry",0),n.getOrDefault("support",0),n.getOrDefault("siege",0));
    }
    /** The army's facing (audit 4.1 "Facing"; Assignment.yaw lays its formation out round its spot and turns troops at their slots): along its line of march to the spot; at the spot,
     *  toward the nearest enemy its team has seen near it, else as it came; on a retreat, back the way it came. It turns only by more than FACE_TURN degrees.
     *  A retreat reads "the way it came" only while it is still more than FACE_ARRIVE from its base, as a march reads its line: the formation stands behind
     *  its spot as it faces, so at the base the centre-to-base line pointed the other way at every decision and the army turned about and walked across
     *  its spawn and back (Kort 2026-11-11, "walk 5 blocks to the right then turn around"). Arrived, it faces as an army at its spot does. */
    float facing(GravesAndGlory.Squad s,Vec3d center,Collection<GravesAndGlory.Battle.Sight> seen,boolean retreating){
        Vec3d t=s.target;if(t==null)return s.yaw;
        double dx=t.x-center.x,dz=t.z-center.z,far=Math.hypot(dx,dz);Float want=null;
        if(retreating&&far>FACE_ARRIVE)want=BattleMath.yawToward(-dx,-dz);
        else if(!retreating&&far>FACE_ARRIVE)want=BattleMath.yawToward(dx,dz);
        else{double best=FACE_ENEMY;for(var sight:seen){double d=Math.hypot(sight.x()-t.x,sight.z()-t.z);if(d<best&&d>1){best=d;want=BattleMath.yawToward(sight.x()-t.x,sight.z()-t.z);}}}
        return want!=null&&BattleMath.yawGap(want,s.yaw)>FACE_TURN?want:s.yaw;
    }
    /** The points plan per group: the guard group holds its point facing the threat, and on an attack the ranged group stands off behind the point (BattleMath.rangedStandoff) on the side
     *  it comes from, so its arrows reach across the point instead of the archers walking onto it with the infantry (audit D5). On Hold (a point it keeps) the ranged stay with the rest. */
    void planGroups(GravesAndGlory.Squad s,Vec3d center,List<GravesAndGlory.Unit> mine,int guard,String guardGroup,Collection<GravesAndGlory.Battle.Sight> seen){
        if(guard>=0&&guardGroup!=null){var p=b.points.get(guard);Vec3d at=spot(p);float yaw=s.yaw;double best=GUARD_THREAT_RADIUS;
            for(var sight:seen){double d=Math.hypot(sight.x()-at.x,sight.z()-at.z);if(d<best&&d>1){best=d;yaw=BattleMath.yawToward(sight.x()-at.x,sight.z()-at.z);}}
            s.assignments.put(guardGroup,new GravesAndGlory.Assignment(p.contested?"attack":"hold",s.formation,at,yaw));}
        if(s.order.equals("hold")||"ranged".equals(guardGroup))return;
        double reach=Double.MAX_VALUE;for(var u:mine)if(u.spec.group().equals("ranged"))reach=Math.min(reach,u.spec.range());
        if(reach==Double.MAX_VALUE)return;
        GravesAndGlory.Capture point=null;for(var p:b.points)if(on(s.target,p))point=p;if(point==null)return;
        double back=Math.max(0,BattleMath.rangedStandoff(reach,point.spec.radius())-BattleMath.roleDepth("ranged")); // the formation already puts ranged roleDepth behind the spot
        double dx=s.target.x-center.x,dz=s.target.z-center.z,len=Math.hypot(dx,dz);
        if(len<6){double a=Math.toRadians(s.yaw);dx=-Math.sin(a);dz=Math.cos(a);len=1;} // at the point: behind as it faces
        double x=s.target.x-dx/len*back,z=s.target.z-dz/len*back;
        if(!GravesAndGlory.Battle.inArena(x,z)||GravesAndGlory.Battle.wet(x,z))return;
        s.assignments.put("ranged",new GravesAndGlory.Assignment(s.order,s.formation,GravesAndGlory.Battle.groundAt(x,z),s.yaw));
    }
    /** Bot reinforcement routing (audit C3, D3): a bot's new troop that appears far from its army gathers at a rally MUSTER_BACK blocks behind the army, toward where it appeared
     *  (where it appeared when the army has nobody else), and waits there for the others under Attack (it fights what comes); musterRelease sends them up together. */
    void musterRefill(GravesAndGlory.Squad s,GravesAndGlory.Unit fresh){
        if(botRetreating.contains(s.owner))return;
        boolean gathering=s.muster!=null&&b.units.stream().anyMatch(u->u!=fresh&&u.squad==s&&u.mob.isAlive()&&u.musterSince>=0);
        if(gathering){fresh.musterSince=b.time;return;} // joins the gathering at its rally
        var front=b.units.stream().filter(u->u!=fresh&&u.squad==s&&u.mob.isAlive()&&u.musterSince<0).toList();
        Vec3d at=fresh.mob.getPos(),rally=at;float yaw=s.yaw;
        if(!front.isEmpty()){
            Vec3d c=Morale.centre(front);double dx=at.x-c.x,dz=at.z-c.z,d=Math.hypot(dx,dz);
            if(d<=MUSTER_NEAR)return; // close enough to walk in with its army
            double x=c.x+dx/d*MUSTER_BACK,z=c.z+dz/d*MUSTER_BACK;
            if(GravesAndGlory.Battle.inArena(x,z)&&!GravesAndGlory.Battle.wet(x,z))rally=GravesAndGlory.Battle.groundAt(x,z);
            yaw=BattleMath.yawToward(c.x-rally.x,c.z-rally.z);
        }
        var order=new GravesAndGlory.Assignment("attack","loose",rally,yaw);order.issued=Long.MIN_VALUE;
        s.muster=order;musterFirstAt.remove(s.owner);fresh.musterSince=b.time;
    }
    /** Sends a bot's gathered refills up to their army together, at each decision. The rally follows the army: MUSTER_BACK blocks behind its troops, toward the refills. The largest knot
     *  of refills standing within MUSTER_GATHERED of one another goes once it is MUSTER_COUNT strong (fewer when no more are coming; BattleMath.musterGo), once the first has stood at
     *  the rally MUSTER_WAIT_TICKS, or once it is within MUSTER_NEAR of the army anyway; the others wait for the next knot. All go on a retreat, when the army has nobody else, or
     *  MUSTER_CAP_TICKS after the first appeared. */
    void musterRelease(GravesAndGlory.Squad s,List<GravesAndGlory.Unit> all,List<GravesAndGlory.Unit> front,boolean retreating){
        var waiting=all.stream().filter(u->u.musterSince>=0).toList();
        if(waiting.isEmpty()){s.muster=null;musterFirstAt.remove(s.owner);return;}
        long first=Long.MAX_VALUE;for(var u:waiting)first=Math.min(first,u.musterSince);
        boolean every=retreating||s.muster==null||b.time-first>=MUSTER_CAP_TICKS;
        List<GravesAndGlory.Unit> knot=waiting;
        if(!every&&front.isEmpty()){ // the army is all refills: they gather where they appear and go together
            knot=largestKnot(waiting);if(knot.size()<Math.min(MUSTER_COUNT,waiting.size()+s.training.size()))return;
        }else if(!every){
            knot=largestKnot(waiting);Vec3d c=Morale.centre(front),k=Morale.centre(knot);double dx=k.x-c.x,dz=k.z-c.z,d=Math.hypot(dx,dz);
            if(d>MUSTER_NEAR){
                double x=c.x+dx/d*MUSTER_BACK,z=c.z+dz/d*MUSTER_BACK;
                if(GravesAndGlory.Battle.inArena(x,z)&&!GravesAndGlory.Battle.wet(x,z)){s.muster.target=GravesAndGlory.Battle.groundAt(x,z);s.muster.yaw=BattleMath.yawToward(c.x-x,c.z-z);}
                Vec3d rally=s.muster.target;
                if(Math.hypot(k.x-rally.x,k.z-rally.z)<=MUSTER_GATHERED)musterFirstAt.putIfAbsent(s.owner,b.time);
                long arrived=musterFirstAt.getOrDefault(s.owner,b.time);
                int need=(int)Math.min(MUSTER_COUNT,waiting.size()+s.training.size());
                if(!BattleMath.musterGo(knot.size(),need,b.time-arrived,MUSTER_WAIT_TICKS))return;
            }
        }
        for(var u:waiting){if(!knot.contains(u)){u.musterSince=b.time;continue;} // one elsewhere waits for the next knot
            u.musterSince=-1;u.mob.getNavigation().stop();u.nextPath=0;}
        musterFirstAt.remove(s.owner);if(knot.size()==waiting.size())s.muster=null;
    }
    /** The most refills standing within MUSTER_GATHERED of one of them (that one included). */
    static List<GravesAndGlory.Unit> largestKnot(List<GravesAndGlory.Unit> waiting){
        List<GravesAndGlory.Unit> best=List.of();
        for(var a:waiting){var knot=waiting.stream().filter(o->o.mob.squaredDistanceTo(a.mob)<=MUSTER_GATHERED*MUSTER_GATHERED).toList();if(knot.size()>best.size())best=knot;}
        return best;
    }
    /** Hard holds what it has taken before it flanks. Gathers the team's armies for BattleMath.garrison, near meaning within
     *  NEAR_RADIUS_FACTOR*chargeDistance: its bot armies (alive and not retreating, in squad order; a fixture's army outside b.squads goes last)
     *  and its seated human armies, which guard a point they stand on but are never sent to one. Returns the index of the point this army
     *  holds, or -1: free to flank. */
    int garrisonPoint(GravesAndGlory.Squad s,Vec3d center,Spec.BotCommandRow tier){
        if(botRetreating.contains(s.owner))return -1;
        List<double[]> at=new ArrayList<>();List<Boolean> bots=new ArrayList<>();int me=-1;
        for(var army:b.squads.values()){
            if(army==s){me=at.size();at.add(new double[]{center.x,center.z});bots.add(true);continue;}
            if(army.team!=s.team||!army.seated||army.bot&&botRetreating.contains(army.owner))continue; // a seated human army stays in: it guards
            var units=b.units.stream().filter(u->u.squad==army&&u.mob.isAlive()).toList();if(!units.isEmpty()){Vec3d c=Morale.centre(units);at.add(new double[]{c.x,c.z});bots.add(army.bot);}
        }
        if(me<0){me=at.size();at.add(new double[]{center.x,center.z});bots.add(true);}
        int count=b.points.size();int[] owners=new int[count];double[][] points=new double[count][];boolean[] assignable=new boolean[at.size()];
        for(int j=0;j<count;j++){var p=b.points.get(j);owners[j]=p.owner;points[j]=new double[]{p.spec.x(),p.spec.z()};}
        for(int i=0;i<assignable.length;i++)assignable[i]=bots.get(i);
        return BattleMath.garrison(owners,s.team,at.toArray(double[][]::new),assignable,points,NEAR_RADIUS_FACTOR*tier.chargeDistance())[me];
    }
    /** Switches the battlefield. The arena rebuilds on the next ticks; the lobby stays. */
    void switchMap(int index){if(b.mode.teardown(b))Modes.unmark(b);b.saves.clear(); // the next map's recover drops a kept marker and its files: the arena is rebuilt
        Projectiles.clear(b);b.strikes.clear();Abilities.clearBanners(b);Houses.reset(b);b.clearCorpses();b.clearSides();b.clearProgression();b.clearPlans();FeedEvents.clear();map=index;if(!Modes.registered(battleMode)||!Modes.hosts(battleMode,map))battleMode=Modes.DOMINATION;CampaignMaps.select(map);b.points.clear();for(var point:Spec.POINTS)b.points.add(new GravesAndGlory.Capture(point));b.ready=false;b.buildColumn=0;EngineMaps.reset();Structures.clearQueue();b.placing=false;}
    /** The lesson a commander is on, or -1: only a campaign battle whose sheet row teaches (lessons) shows them, to its commanders, until all four are done. They repeat on every replay. */
    int tutorialStep(UUID id){
        if(!b.running||activeBattle==null||!activeBattle.lessons()||!participants.contains(id))return -1;
        int step=tutorial.getOrDefault(id,0);return step>3?-1:step;
    }
    /** Advances the lesson when the commander does what it asks. The last one pays for a first recruitment roll, once per commander (tutorialDone). */
    void tutorialDid(ServerPlayerEntity p,int step){
        UUID id=p.getUuid();int at=tutorialStep(id);
        if(step==1&&at==0){tutorial.put(id,1);return;} // a Formation given first still completes the Follow me or Retreat lesson; the Formation lesson then waits for the next one
        if(at!=step)return;
        tutorial.put(id,step+1);if(step<3)return;
        Profile pr=profile(id);if(pr.tutorialDone){notice(id,"Training complete. The reward is paid once.");return;}
        pr.tutorialDone=true;pr.troopWrits+=ROLL_WRITS;save(id);
        notice(id,"Training complete: +"+ROLL_WRITS+" troop writ, enough for one recruitment roll after the battle.");
    }
    boolean leader(ServerPlayerEntity p){return p.getUuid().equals(partyLeader);}
    String name(UUID id){var p=b.server.getPlayerManager().getPlayer(id);return p==null?social.names.getOrDefault(id,"Commander-"+id.toString().substring(0,6)):p.getGameProfile().getName();}
    void closeParty(){partyLeader=null;party.clear();}
    void leaveParty(UUID id){
        if(!party.remove(id))return;
        if(party.isEmpty())closeParty();else if(id.equals(partyLeader))partyLeader=party.iterator().next();
    }
    String snapshot(ServerPlayerEntity p){
        JsonObject j=new JsonObject();Profile pr=profile(p.getUuid());j.add("profile",forClient(pr));j.addProperty("collectionReady",collectionsReady.contains(p.getUuid()));j.addProperty("waiting",b.running&&(!b.squads.containsKey(p.getUuid())||!b.squads.get(p.getUuid()).seated));j.addProperty("mode",practice?"ai":Integer.toString(size));j.addProperty("fill",fill);j.addProperty("difficulty",difficulty);j.addProperty("map",map);j.addProperty("serial",serial);j.addProperty("intro",b.squads.containsKey(p.getUuid())&&b.squads.get(p.getUuid()).seated?Math.max(0,introUntil-b.time):0);j.addProperty("notice",notices.getOrDefault(p.getUuid(),""));j.addProperty("noticeSerial",noticeSerials.getOrDefault(p.getUuid(),0));j.addProperty("headline",headline);j.addProperty("tutorial",tutorialStep(p.getUuid()));
        JsonObject battle=new JsonObject();if(activeBattle!=null){battle.addProperty("id",activeBattle.id());battle.addProperty("name",activeBattle.name());battle.addProperty("briefing",activeBattle.briefing());battle.addProperty("reason",activeBattle.reason());}j.add("battle",battle);var starts=seasonStarts();j.addProperty("season",MarchMath.Seasons.current(starts,Progression.today()));JsonArray dates=new JsonArray();for(var d:starts)dates.add(d==null?"":d.toString());j.add("seasonStarts",dates); // brief G2: the campaign card shows a chapter that has not opened // the Long March battle being fought, or {}
        j.add("result",results.getOrDefault(p.getUuid(),new JsonObject()));
        var endingBlock=endingJson(p.getUuid());if(endingBlock!=null)j.add("ending",endingBlock); // only while this participant is in the sequence
        // In the lobby a row carries the team the viewer's next round gives that commander (nextSeats), or -1 when he would not enter with the viewer, so the list's marks read like the side preview; in battle, the squad's team.
        Map<UUID,Integer> nextTeams=new HashMap<>();if(!b.running)for(var seat:nextSeats(p.getUuid()))nextTeams.put(seat.owner(),seat.team());
        JsonArray players=new JsonArray();for(var s:b.squads.values()){JsonObject row=new JsonObject();var player=b.server.getPlayerManager().getPlayer(s.owner);row.addProperty("name",s.bot?"AI Commander":player==null?"Commander":player.getGameProfile().getName());row.addProperty("team",b.running?s.team:nextTeams.getOrDefault(s.owner,-1));row.addProperty("bot",s.bot);players.add(row);}j.add("players",players);j.add("sides",sidesJson(p.getUuid()));j.add("banner",bannerJson(p.getUuid()));j.add("social",socialSnapshot(p.getUuid()));j.add("progression",progressSnapshot(p.getUuid()));j.add("lastRoll",lastRolls.getOrDefault(p.getUuid(),new JsonObject()));j.add("forge",forgeResults.getOrDefault(p.getUuid(),new JsonObject()));
        BattleMode shown=b.running?b.mode:Modes.create(battleMode);
        j.add("battleMode",shown.snapshot(b,b.running?b.squads.get(p.getUuid()):null));
        return fitted(j,p.getUuid());
    }
    /**
     * The seats the viewer's next round would have, for the lobby's side preview. While a start is under way they are the seated
     * commanders, exactly what chooseSides reads. Between rounds nobody is seated, so they are the commanders the viewer's start
     * would seat, balanced as prepareRound balances them: his room on its teams, his solo-start group on
     * their teams, or he alone on team 0, as a solo battle seats him. A commander in the lobby who will not enter with him does
     * not set his preview.
     */
    List<Seat> nextSeats(UUID viewer){
        var seated=seats(s->s.seated);if(!seated.isEmpty())return seated;
        var room=social.room(viewer);
        if(room!=null)return balanced(seats(s->room.members.contains(s.owner)).stream().map(s->new Seat(s.owner(),room.teams.getOrDefault(s.owner(),s.team()))).toList(),false,room.size);
        if(party.contains(viewer))return balanced(seats(s->party.contains(s.owner)),practice,size);
        return List.of(new Seat(viewer,0));
    }
    /** The side ids the two teams fly, for the viewer's snapshot: this round's while a battle runs, else chooseSides' pick over the seats his next round would have (nextSeats). */
    JsonArray sidesJson(UUID viewer){int[] rows=b.running?b.sides:sidesFor(nextSeats(viewer));JsonArray out=new JsonArray();for(int t=0;t<2;t++)out.add(Spec.FACTIONS[rows[t]].id());return out;}
    /** The banner a commander fields: his battle banner while he is seated in a running battle, else his profile banner. */
    JsonArray bannerJson(UUID id){var s=b.squads.get(id);List<String> shown=b.running&&s!=null&&s.seated?s.banner:profile(id).line();JsonArray out=new JsonArray();for(String entry:shown)out.add(entry);return out;}
    /** The client's copy of a profile: match ids stay on the server. They only stop one match paying twice, and 256 of them (94 characters a match across both copies) would outgrow SNAPSHOT_LIMIT. */
    static JsonObject forClient(Profile p){JsonObject j=JSON.toJsonTree(p).getAsJsonObject();var s=j.getAsJsonObject(Profile.PROGRESSION);if(s!=null)s.remove(Progression.State.MATCH_GRANTS);j.remove(Profile.GEAR);j.remove("bests");return j;} // the gear list travels on gear_v1 (GearWire), never in the camp snapshot
    static JsonObject forClient(Progression.State s){JsonObject j=JSON.toJsonTree(s).getAsJsonObject();j.remove(Progression.State.MATCH_GRANTS);return j;}
    /** The snapshot as sent (MatchEnding.fit): pretty when it fits SNAPSHOT_LIMIT, else compact, else compact without the report's timeline, then its bests, then its progress, then the whole report, last "{}" (the client then keeps its last camp state). Each shortened form is logged the first time a commander is sent it. */
    String fitted(JsonObject j,UUID id){
        var f=MatchEnding.fit(j,SNAPSHOT_LIMIT,JSON,WIRE);
        if(!f.form().equals("pretty")&&snapshotForms.computeIfAbsent(id,k->new HashSet<>()).add(f.form()))System.err.println("[Graves and Glory] The camp snapshot for "+name(id)+" ran to "+f.full()+" characters, over the client's "+SNAPSHOT_LIMIT+"; sent "+f.form()+" ("+f.text().length()+" characters).");
        return f.text();
    }

    boolean saveSocial(){
        Path file=b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-parties.json");
        try{SafeFiles.writeWithBackup(file,JSON.toJson(social));return true;
        }catch(java.io.IOException e){System.err.println("[Graves and Glory] Party save failed: "+e.getClass().getSimpleName());return false;}
    }
    boolean socialCommand(ServerPlayerEntity p,String cmd){
        UUID id=p.getUuid();social.presence(new HashSet<>(b.server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList()),System.currentTimeMillis());
        String before=JSON.toJson(social);for(var online:b.server.getPlayerManager().getPlayerList())social.names.put(online.getUuid(),online.getGameProfile().getName());String result;String[] parts=cmd.split(":",3);String action=parts.length>1?parts[1]:"",arg=parts.length>2?parts[2]:"";
        try{result=switch(action){
            case "create"->social.createParty(id);case "accept"->social.accept(id,System.currentTimeMillis());case "decline"->social.decline(id);
            case "leave"->social.leaveParty(id);case "disband"->social.disband(id);case "claim"->social.claim(id,System.currentTimeMillis());
            case "invite","kick","transfer"->{var target=b.server.getPlayerManager().getPlayer(arg);UUID targetId=target==null?social.group(id).stream().filter(m->name(m).equals(arg)).findFirst().orElse(null):target.getUuid();
                if(targetId==null)yield "Commander not found.";
                yield action.equals("invite")?social.invite(id,targetId,System.currentTimeMillis()):action.equals("kick")?social.kick(id,targetId):social.transfer(id,targetId);}
            case "room"->social.createRoom(id);case "lobby"->social.lobby(id);
            case "difficulty"->social.difficulty(id,arg,Arrays.stream(Spec.BOTCOMMAND).map(Spec.BotCommandRow::id).collect(java.util.stream.Collectors.toSet()));
            case "roominvite"->{var target=b.server.getPlayerManager().getPlayer(arg);String refusal=social.roomInviteRefusal(id,target==null?null:target.getUuid());
                if(!refusal.isEmpty())yield refusal;var mine=social.room(id);notice(target.getUuid(),name(id)+" invites you to war council "+mine.code+". Open Friends and press Join beside "+name(id)+".");yield "Invitation sent to "+name(target.getUuid())+".";}
            case "mode"->{String said=social.mode(id,arg,Modes.ids());var room=social.room(id); // a mode that does not host the room's map takes the room to its first map
                if(said.startsWith("Mode set")&&room!=null&&!Modes.hosts(room.mode,room.map))for(int m=0;m<Spec.MAPS.length;m++)if(Modes.hosts(room.mode,m)){room.map=m;room.fitted="map";room.fitSerial++;said=Modes.row(room.mode).name()+" is played on "+Spec.MAPS[m].name()+". "+said;break;}
                yield said;}case "house"->{String said=social.house(id,arg,RosterMath.fieldableHouses());String no=said.startsWith("House chosen")?houseRefusal(social.room(id),id):"";yield no.isEmpty()?said:no;}
            case "host"->{UUID targetId=social.room(id)==null?null:social.room(id).members.stream().filter(m->name(m).equals(arg)).findFirst().orElse(null);yield social.handHost(id,targetId);}case "join"->social.joinRoom(id,arg);case "exit"->social.leaveRoom(id);case "ready"->social.ready(id,houseRefusal(social.room(id),id));
            case "team"->social.team(id,Integer.parseInt(arg));
            case "size","map","fill"->{var room=social.room(id);if(room==null)yield "Create or join a room first.";
                String said=social.configure(id,action.equals("size")?Integer.parseInt(arg):room.size,action.equals("map")?Integer.parseInt(arg):room.map,action.equals("fill")?!room.fill:room.fill,Spec.MAPS.length);
                if(said.startsWith("Room settings saved")&&!Modes.hosts(room.mode,room.map)){said=Modes.row(room.mode).name()+" is not played on "+Spec.MAPS[room.map].name()+": the mode is now Domination. "+said;room.mode=Modes.DOMINATION;room.fitted="mode";room.fitSerial++;} // a map the room's mode does not host plays Domination
                yield said;}
            case "start"->{if(!preparingRoom.isEmpty()&&!preparingRoom.equals(social.room(id)==null?"":social.room(id).code))yield "Another room is preparing a battlefield. Your room is saved.";String failure=social.startFailure(id,b.running);if(!failure.isEmpty())yield failure;
                var room=social.room(id);if(!b.ready)yield "Preparing the battlefield. Please wait.";
                if(room.map!=map){preparingRoom=room.code;switchMap(room.map);yield "Preparing your chosen map. Your ready group will enter automatically; unready to cancel.";}
                var empty=room.members.stream().filter(m->houseUnits(room,m)==0).findFirst();if(empty.isPresent())yield name(empty.get())+"'s banner holds no "+RosterMath.houseName(lobbyHouse(room,empty.get()))+" troops.";
                size=room.size;fill=room.fill;practice=false;activeRoom=room.code;activeBattle=null;roundTier=room.difficulty;
                for(var squad:b.squads.values())if(!squad.bot){squad.seated=room.members.contains(squad.owner);if(squad.seated)squad.team=room.teams.get(squad.owner);}
                if(battleModeBeforeRoom==null)battleModeBeforeRoom=battleMode;
                battleMode=room.mode; // the room's mode plays (startRound falls back to Domination on a map the mode does not host)
                social.begin(room);b.startRound();yield "Your room is entering battle together.";}
            default->"Unknown social action.";
        };}catch(IllegalArgumentException e){result="Invalid selection.";}
        var mineNow=social.room(id);if(mineNow!=null&&mineNow.difficulty.isEmpty())mineNow.difficulty=difficulty; // a new room starts at the server's current bot setting
        if(!before.equals(JSON.toJson(social))&&!saveSocial()){social.rollback(JSON.fromJson(before,SocialHub.class));result="Party change could not be saved. It was not applied; check disk space.";}
        notice(id,result);return true;
    }
    /** The War Table lobby's ready countdown, every 10 ticks between battles: a room whose every commander is ready counts down
     *  SocialHub.COUNTDOWN_TICKS and then starts as the host's start would; an un-ready (or anything that stops the start) stops it. */
    void roomCountdown(){
        var step=social.countdown(10,b.running,preparingRoom);
        for(var r:step.started())for(UUID m:r.members)notice(m,"All commanders ready. The battle begins in 10 seconds; un-ready to stop it.");
        for(var r:social.rooms.values())if(r.countdown>0)Audio.countdown(b,r.members,r.countdown); // a tick for each of the last three seconds
        for(var r:step.cancelled()){String why=social.startFailure(r.leader,b.running);for(UUID m:r.members)notice(m,"Countdown stopped."+(why.isEmpty()?"":" "+why));}
        for(var r:step.due()){var host=b.server.getPlayerManager().getPlayer(r.leader);if(host!=null)socialCommand(host,"social:start");
            if(!r.playing&&!r.code.equals(preparingRoom)){r.ready.clear();social.revision++;for(UUID m:r.members)if(!m.equals(r.leader))notice(m,"The battle could not start. Ready up again.");}}
    }
    /** A member's House as the lobby shows it: the pick, else the House the banner leans to. */
    String lobbyHouse(SocialHub.Room r,UUID m){String h=r.houses.get(m);return h!=null?h:RosterMath.rollHouse(profile(m).line());}
    JsonObject socialSnapshot(UUID id){
        JsonObject j=new JsonObject();var party=social.party(id);var room=social.room(id);j.addProperty("party",party!=null);j.addProperty("partyLead",party!=null&&party.leader.equals(id));j.addProperty("leader",party==null?"":name(party.leader));
        JsonArray members=new JsonArray();if(party!=null)for(UUID m:party.members){JsonObject row=new JsonObject();row.addProperty("name",name(m));row.addProperty("online",social.online.contains(m));row.addProperty("me",m.equals(id));members.add(row);}j.add("members",members);
        var invite=social.invites.get(id);j.addProperty("inviter",invite==null?"":name(invite.sender()));j.addProperty("inviteSeconds",invite==null?0:Math.max(0,(invite.expires()-System.currentTimeMillis()+999)/1000));
        JsonArray available=new JsonArray();for(var p:b.server.getPlayerManager().getPlayerList())if(!p.getUuid().equals(id)&&social.party(p.getUuid())==null)available.add(p.getGameProfile().getName());j.add("available",available);
        JsonArray camp=new JsonArray();for(var p:b.server.getPlayerManager().getPlayerList())if(!p.getUuid().equals(id)){JsonObject row=new JsonObject();var theirs=social.room(p.getUuid());row.addProperty("name",p.getGameProfile().getName());row.addProperty("party",social.party(p.getUuid())!=null);row.addProperty("room",theirs==null||theirs.playing?"":theirs.code);camp.add(row);}j.add("camp",camp);
        j.addProperty("room",room==null?"":room.code);j.addProperty("host",room!=null&&room.leader.equals(id));j.addProperty("playing",room!=null&&room.playing);j.addProperty("preparing",room!=null&&room.code.equals(preparingRoom));j.addProperty("size",room==null?2:room.size);j.addProperty("map",room==null?0:room.map);j.addProperty("fill",room==null||room.fill);j.addProperty("difficulty",room==null||room.difficulty.isEmpty()?difficulty:room.difficulty);j.addProperty("fitted",room==null?"":room.fitted);j.addProperty("fitSerial",room==null?0:room.fitSerial);
        j.addProperty("mode",room==null?Modes.DOMINATION:room.mode);JsonArray modes=new JsonArray();Modes.ids().forEach(modes::add);j.add("modes",modes);
        j.addProperty("countdown",room==null||room.countdown<0?0:(room.countdown+19)/20);j.addProperty("waiting",room==null||room.playing?"":social.startFailure(room.leader,b.running));
        JsonArray commanders=new JsonArray();if(room!=null)for(UUID m:room.members){JsonObject row=new JsonObject();row.addProperty("name",name(m));row.addProperty("team",room.teams.get(m));row.addProperty("ready",room.ready.contains(m));row.addProperty("online",social.online.contains(m));
            row.addProperty("house",lobbyHouse(room,m));row.addProperty("troops",houseUnits(room,m));row.addProperty("role",GearStats.of(profile(m)).role());row.addProperty("host",m.equals(room.leader));row.addProperty("me",m.equals(id));commanders.add(row);}j.add("commanders",commanders);return j;
    }

    boolean progressCommand(ServerPlayerEntity player,String cmd){
        UUID id=player.getUuid();Profile p=profile(id);String before=JSON.toJson(p);Progression.reset(p.progression,Progression.today());
        String action=cmd.substring(9),result;
        if(action.equals("reroll"))result=Progression.reroll(p.progression);
        else if(action.startsWith("title:")){String title=action.substring(6);if(p.progression.titles.contains(title)){p.progression.title=title;result="Title equipped: "+title;}else result="That title is locked.";}
        else {var reward=Progression.claim(p.progression,action);
            // A claim that grants a troop (the Founders Rare) waits for the match to end, as roll and buy do: a copy taken mid-battle would raise the stars of a fielded troop.
            var seat=b.squads.get(id);if(reward.troop()!=null&&b.running&&seat!=null&&seat.seated){profiles.put(id,Profile.restored(JSON.fromJson(before,Profile.class)));notice(id,"Claim tier "+Progression.RARE_TIER+"'s Rare after the battle: active battle choices are locked until the match ends.");return true;}
            int writs0=p.troopWrits;p.payGold(reward.gold());result=reward.message();
            int tipped=p.troopWrits-writs0-reward.gold()/100;if(tipped>0)result+=" / +"+Progression.writs(tipped); // the carried purse tipped over: say the writ it paid
            if(reward.shards()>0&&reward.house()!=null)p.shards.merge(reward.house(),reward.shards(),Integer::sum); // a contract's or a Founders tier's shards, to the House the claim named
            var troop=reward.troop()==null?null:troop(reward.troop());if(troop!=null)result+=". "+receive(p,troop); // the Founders Rare: a new troop, a copy, or past full stars its shards
            if(reward.cosmetic()!=null)p.cosmetics.add(reward.cosmetic());}
        if(!save(id)){profiles.put(id,Profile.restored(JSON.fromJson(before,Profile.class)));result="Reward was not applied because saving failed. Try again after checking disk space.";}
        notice(id,result);return true;
    }
    JsonObject progressSnapshot(UUID id){Profile p=profile(id);Progression.reset(p.progression,Progression.today());JsonObject j=forClient(p.progression);j.addProperty("tier",Progression.tier(p.progression));var roster=RosterMath.launchRoster();j.addProperty("chapter",Progression.chapter(p.progression,(int)roster.stream().filter(p.owned::containsKey).count(),roster.size()));return j;}
    void progressEvent(UUID id,boolean capture){if(!participants.contains(id))return;var squad=b.squads.get(id);if(squad==null||!squad.seated)return;
        var state=profile(id).progression;Progression.reset(state,Progression.today());if(capture)state.dailyCaptures++;else state.dailyOrders++;}
}
