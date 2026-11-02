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
    /** The client reads the camp snapshot with readString(SNAPSHOT_LIMIT) and a longer string drops its connection, so fitted() never returns one. WIRE is the compact form it falls back to. */
    static final int SNAPSHOT_LIMIT=16000;static final Gson WIRE=new Gson();
    final GravesAndGlory.Battle b;
    final Map<UUID,Profile> profiles=new HashMap<>();
    final Path directory;
    SocialHub social=new SocialHub();String activeRoom="",preparingRoom="";
    final Map<UUID,Integer> mapVotes=new HashMap<>();
    int size=2,map=0;boolean practice=false,fill=true;
    /** The mode the next round plays (Phase 4 Task 2): a lobby setting kept across rounds like the map, reset to Domination when a new map does not host it. */
    String battleMode=Modes.DOMINATION;
    /** The battleMode a room's round replaced (null outside a room's round); returnLobby puts it back, so a later skirmish or practice plays its own mode. */
    String battleModeBeforeRoom;
    long introUntil;int serial;String matchId=UUID.randomUUID().toString();
    final Map<UUID,Long> botRespawns=new HashMap<>();
    final Set<UUID> participants=new HashSet<>();
    final Map<UUID,Long> joinedAt=new HashMap<>();
    final Set<UUID> collectionsReady=new HashSet<>();
    /** Players with no server record when they joined; only they may bring a collection from their own computer, once. */
    final Set<UUID> importAllowed=new HashSet<>();
    /** Commanders who left mid-battle, mapped to the bot squad holding their army until they return this round. */
    final Map<UUID,UUID> vacated=new HashMap<>();
    /** Lessons reached in the campaign battle being fought, when its sheet row teaches: 0 move, 1 formation, 2 attack, 3 barracks, 4 all done. Cleared as each round starts. */
    final Map<UUID,Integer> tutorial=new HashMap<>();
    /** AI armies that fell back stay there until rallied, so they do not walk back into the fight. */
    final Set<UUID> botRetreating=new HashSet<>();
    final Map<UUID,String> notices=new HashMap<>();
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
    static final int NOTICE_TICKS=200;
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
    /** The Long March battle being fought (a Spec.CAMPAIGNBATTLES row), or null in practice, pad matches and rooms. Cleared in returnLobby. */
    Spec.CampaignBattlesRow activeBattle;
    /** The bonus objective of activeBattle (plan 2C.3), or null. Set in soloStart, cleared in returnLobby; the round's events feed it from plan 2C.4. */
    MarchMath.Tracker bonusTracker;
    /** A battle asked for before its map was built, and the commander who asked; it starts on the first ready tick. */
    Spec.CampaignBattlesRow pendingBattle;UUID pendingCommander;
    /** The winner of the last finished round: 0 Dawn, 1 Dusk, -1 a draw (and before any round has finished). The bot simulation sets -2 before each of its rounds. */
    int lastWinner=-1;
    /** Ready pads: who stands on which pad (team), the countdown in ticks, and whether this cycle has rolled its quick-match map. */
    final Map<UUID,Integer> onPad=new HashMap<>();final Set<UUID> padRefused=new HashSet<>();
    int padCountdown=-1;boolean padMapRolled;
    /** Custom lobby: one at a time. Its leader sets mode, map and bot fill, and starts the match. Invites expire after 60 seconds. */
    UUID partyLeader;final LinkedHashSet<UUID> party=new LinkedHashSet<>();final Map<UUID,UUID> invites=new HashMap<>();final Map<UUID,Long> inviteUntil=new HashMap<>();
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
    /** A commander's collection, read once: a schema-1 or schema-2 file is upgraded in place to schema 3 first (spec 6.3, plan 2C.3; its original bytes stay as <uuid>.v1.json or <uuid>.v2.json), then the profile is sanitized. A new commander owns the 10 Commons and flies the starter banner. */
    Profile profile(UUID id){return profiles.computeIfAbsent(id,key->SafeFiles.load(directory.resolve(key+".json"),Campaign::readProfile,Campaign::freshProfile,"Commander profile "+key));}
    /** A new commander: the 10 Commons and the starter banner (sanitize fills them in). Also what a damaged profile with no usable .bak falls back to. */
    static Profile freshProfile(){Profile fresh=new Profile();fresh.sanitize();return fresh;}
    /** One profile file, upgraded in place first when it is schema 1 or 2; throws when it cannot be read (SafeFiles.load then tries the .bak). */
    static Profile readProfile(Path file) throws Exception{
        Profile p=ProfileMigration.upgradeFile(file,text->JSON.fromJson(text,ProfileMigration.V1.class),text->JSON.fromJson(text,Profile.class),JSON::toJson,RosterMath.Catalog.fromSpec());
        if(p==null)p=JSON.fromJson(Files.readString(file),Profile.class);if(p==null)throw new IllegalStateException("Empty profile");p.sanitize();return p;}
    /** The parties file: Gson to SocialHub, then restore; throws when it cannot be read. */
    static SocialHub readParties(Path file) throws Exception{SocialHub s=JSON.fromJson(Files.readString(file),SocialHub.class);if(s==null)throw new IllegalStateException("Empty party data");s.restore();return s;}
    /** The marker (see LayoutMarker) names this layout, this map, this revision of the arena margin and the hash of the map's landmarks (Structures.hash); a save made before the margin was tapered, or before these landmarks, builds its arena once more. */
    boolean cached(){try{return LayoutMarker.current(Files.readString(layoutFile()),map,Structures.hash(map));}catch(Exception e){return false;}}
    void built(){try{SafeFiles.write(layoutFile(),LayoutMarker.text(map,Structures.hash(map)));}catch(Exception e){System.err.println("[Graves and Glory] Map cache marker could not be saved.");}}
    boolean save(UUID id){try{SafeFiles.writeWithBackup(directory.resolve(id+".json"),JSON.toJson(profile(id)));return true;
        }catch(Exception e){System.err.println("[Graves and Glory] Could not save commander collection: "+e.getClass().getSimpleName());notice(id,"Collection could not be saved; check disk space.");return false;}}
    void importProfile(ServerPlayerEntity p,String json){
        if(json.length()>16000)return;
        // The server's saved collection is the only source of truth. The client's copy is a display cache.
        // It is imported once, on a first visit to a single-player or LAN world with no record yet, so
        // existing collections carry over; dedicated servers never accept one.
        UUID id=p.getUuid();boolean allowed=importAllowed.remove(id)&&!b.server.isDedicated();
        try{var v1=JSON.fromJson(json,ProfileMigration.V1.class); // a portable copy from before Houses is upgraded the same way as a server file
            Profile portable=v1!=null&&v1.schema<2?ProfileMigration.toV2(v1,RosterMath.Catalog.fromSpec()):JSON.fromJson(json,Profile.class);portable.sanitize();
            if(allowed){profiles.put(id,portable);save(id);}
            else{Profile stored=profile(id);if(portable.gold>stored.gold||!stored.owned.keySet().containsAll(portable.owned.keySet()))System.out.println("[Graves and Glory] Ignored the collection sent by "+p.getName().getString()+"; the server's saved collection is kept.");}
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
    void joined(ServerPlayerEntity p){
        grantImport(p.getUuid());profile(p.getUuid());
        // Rejoining the same battle keeps the original start time, so a dropped connection does not cost the reward.
        if(!(b.running&&participants.contains(p.getUuid())))joinedAt.put(p.getUuid(),b.time);var squad=b.squads.get(p.getUuid());
        if(b.running&&squad!=null&&squad.seated)b.toBase(p,squad.team);else toLobby(p);
        if(b.running&&(squad==null||!squad.seated))notice(p.getUuid(),WELCOME_WAIT); // true for the moments he reads it, and gone with the round (returnLobby) or after NOTICE_TICKS: no standing line, which the round's end would leave false
        else standing(p.getUuid(),WELCOME);}
    void toLobby(ServerPlayerEntity p){var at=CampWorld.spawn(profile(p.getUuid()));b.relocate(p,at.x(),at.y(),at.z(),at.yaw(),at.pitch());p.changeGameMode(GameMode.ADVENTURE);p.getInventory().clear();Commander.applyPlayer(p,null);p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(20);}
    void returnLobby(){
        // The mode takes down what it placed before the round's armies go (idempotent: finish and a manual return both call it).
        // Only a clean teardown deletes the marker: a piece that could not be put back stays recoverable after a crash.
        if(b.mode.teardown(b))Modes.unmark(b);else System.err.println("[Graves and Glory] The round's teardown left "+b.saves.size()+" saved piece(s); the mode marker is kept.");
        if(battleModeBeforeRoom!=null){battleMode=battleModeBeforeRoom;battleModeBeforeRoom=null;} // the room's mode ends with its round
        for(var s:b.squads.values())Commander.reset(b,s); // every running commander ability ends before its units go
        b.running=false;introUntil=0;social.finish();activeRoom="";activeBattle=null;bonusTracker=null;Projectiles.clear(b);Abilities.clearBanners(b);
        Houses.reset(b);for(var u:b.units)u.mob.discard();b.units.clear();b.clearSides();b.clearProgression();
        b.squads.values().removeIf(s->s.bot);botRespawns.clear();botRetreating.clear();b.respawns.clear();vacated.clear();
        for(var s:b.squads.values()){s.training.clear();s.seated=false;}onPad.clear();padCountdown=-1;closeParty();
        notices.values().removeIf(WELCOME_WAIT::equals);noticeAt.keySet().retainAll(notices.keySet()); // "Battle in progress" is no longer true
        for(var p:b.server.getPlayerManager().getPlayerList())toLobby(p);
    }
    /** Leaving frees the pad, any seat in a custom lobby, and invitations to it, and forgets which snapshot forms were logged for him. */
    void left(UUID id){onPad.remove(id);padRefused.remove(id);leaveParty(id);snapshotForms.remove(id);notices.remove(id);noticeAt.remove(id);} // his notice entries go with him, a standing line's too (expireNotices never drops those)
    boolean handle(ServerPlayerEntity p,String cmd){
        if(cmd.startsWith("social:"))return socialCommand(p,cmd);
        if(cmd.startsWith("progress:"))return progressCommand(p,cmd);
        if(cmd.startsWith("vote:")){try{int pick=Integer.parseInt(cmd.substring(5));if(!b.running&&onPad.containsKey(p.getUuid())&&pick>=0&&pick<Spec.MAPS.length){mapVotes.put(p.getUuid(),pick);notice(p.getUuid(),"Vote saved: "+Spec.MAPS[pick].name());}}catch(NumberFormatException ignored){}return true;}
        if(cmd.equals("camp:practice")){
            String refused=soloRefusal(p.getUuid(),"Practice is solo. Use a private room to play with your party.");if(!refused.isEmpty()){notice(p.getUuid(),refused);return true;}
            soloStart(p,null);return true;
        }
        if(cmd.equals("camp:gates")){ // Ride to the war gates: onto the middle of the ready pad of the team the commander's squad is on
            if(b.running){notice(p.getUuid(),"Wait until the current battle finishes.");return true;}
            var squad=b.squads.get(p.getUuid());Vec3d at=CampaignMaps.padCentre(squad==null?0:squad.team);
            b.relocate(p,at.x,at.y,at.z,180,8);return true;
        }
        if(cmd.startsWith("campaign:")){ // a Long March battle: solo, on its own map, which is built first if another map stands
            var row=Arrays.stream(Spec.CAMPAIGNBATTLES).filter(r->r.id().equals(cmd.substring(9))).findFirst().orElse(null);
            if(row==null)return true;
            if(row.map()<0||row.map()>=Spec.MAPS.length){notice(p.getUuid(),row.name()+" cannot start: its map is missing.");return true;}
            var lock=MarchMath.lockedBy(row,profile(p.getUuid()).battlesWon,Spec.CAMPAIGNBATTLES);if(lock!=null){notice(p.getUuid(),"Win "+lock.name()+" first.");return true;} // plan 2C.3: the March is won in order
            if(!Modes.registered(row.mode())||!Modes.hosts(row.mode(),row.map())){notice(p.getUuid(),row.name()+" cannot start yet: its mode arrives with the mode frame.");return true;} // survival and banner come with plan 2C.4
            String refused=soloRefusal(p.getUuid(),MARCH_SOLO);if(!refused.isEmpty()){notice(p.getUuid(),refused);return true;}
            pendingBattle=row;pendingCommander=p.getUuid();
            if(row.map()!=map){switchMap(row.map());notice(p.getUuid(),"Preparing "+Spec.MAPS[map].name()+" for "+row.name()+".");}
            return true;
        }
        if(cmd.startsWith("party:")){partyCommand(p,cmd);return true;}
        if(cmd.equals("lobby")){if(b.running)notice(p.getUuid(),"Finish the battle before returning to the lobby.");else toLobby(p);return true;}
        if(b.running){
            var current=b.squads.get(p.getUuid());
            boolean global=cmd.startsWith("map:")||cmd.startsWith("mode:")||cmd.equals("fill")||cmd.equals("start")||cmd.startsWith("difficulty:");
            boolean personal=cmd.equals("roll")||cmd.startsWith("roll:")||cmd.startsWith("buy:")||cmd.startsWith("star:")||cmd.startsWith("legendary:")||cmd.startsWith("unlock:")||cmd.startsWith("army:")||cmd.startsWith("slot:")||cmd.startsWith("colour:")||cmd.startsWith("class:");
            if(global||personal&&current!=null&&current.seated){notice(p.getUuid(),"Active battle choices are locked until the match ends.");return true;}
        }
        if(cmd.startsWith("difficulty:")){
            String id=cmd.substring(11);if(Arrays.stream(Spec.BOTCOMMAND).noneMatch(r->r.id().equals(id)))return true;
            if(partyLeader!=null&&!leader(p)){notice(p.getUuid(),"Only the custom lobby leader sets the bots.");return true;}
            difficulty=id;notice(p.getUuid(),"Bots: "+id.substring(0,1).toUpperCase()+id.substring(1)+".");return true;
        }
        Profile pr=profile(p.getUuid());var squad=b.squads.get(p.getUuid());if(squad==null)return false;
        if(cmd.equals("roll")||cmd.startsWith("roll:")){roll(p,pr,cmd.equals("roll")?RosterMath.rollHouse(pr.banner):cmd.substring(5));return true;}
        if(cmd.startsWith("buy:")){buy(p,pr,cmd.substring(4));return true;}
        if(cmd.startsWith("star:")){star(p,pr,cmd.substring(5));return true;}
        if(cmd.startsWith("legendary:")){legendary(p,pr,cmd.substring(10));return true;}
        if(cmd.startsWith("unlock:")||cmd.startsWith("army:")){notice(p.getUuid(),"Troops now join by recruitment rolls at the camp shrine.");return true;}
        if(cmd.startsWith("class:")){String id=cmd.substring(6);if(Arrays.stream(Spec.CLASSES).anyMatch(c->c.id().equals(id))){pr.commander=id;squad.commander=id;Commander.applyPlayer(p,squad);save(p.getUuid());var room=social.room(p.getUuid());if(room!=null&&!room.playing&&room.ready.remove(p.getUuid()))social.revision++;}return true;}
        if(cmd.startsWith("slot:")){ // slot:<i>:<troop id or empty>: the banner is rebuilt with repack, so markers follow their Epic or Legendary
            String[] parts=cmd.split(":");if(parts.length!=3)return true;int slot;try{slot=Integer.parseInt(parts[1]);}catch(Exception e){return true;}
            var catalog=RosterMath.Catalog.fromSpec();String id=parts[2];var type=troop(id);
            if(slot<0||slot>=pr.banner.size()||type==null&&!id.equals(RosterMath.EMPTY))return true;
            if(type!=null&&!pr.owned.containsKey(id)){notice(p.getUuid(),type.name()+" is not in your collection. Recruit it first.");return true;}
            if(type!=null&&!RosterMath.fieldableHouses().contains(type.house())){notice(p.getUuid(),type.name()+" cannot be fielded yet: "+RosterMath.houseName(type.house())+" has not joined the war.");return true;}
            List<String> entries=new ArrayList<>(pr.banner);entries.set(slot,id);
            List<String> before=pr.banner,next=RosterMath.fieldable(RosterMath.repack(entries,catalog,pr.owned.keySet()),catalog,RosterMath.fieldableHouses());
            if(type!=null&&Collections.frequency(next,id)<Collections.frequency(entries,id))notice(p.getUuid(),"No room for "+type.name()+": a banner holds 2 Epics (2 slots each) and 1 Legendary (4).");
            pr.banner=next;if(!save(p.getUuid())){pr.banner=before;return true;}
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
            if(party.size()>requested*2||(mode.equals("ai")&&party.size()>1)){notice(p.getUuid(),"Your custom lobby is too large for that mode.");return true;}
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
            if(!leader(p)){notice(p.getUuid(),"Only the custom lobby leader starts the match.");return true;}
            if(!fill&&!practice&&party.size()!=size*2){notice(p.getUuid(),"Waiting for commanders. Enable bot fill to play now.");return true;}
            if(practice&&party.size()>1){notice(p.getUuid(),"Practice is for one commander.");return true;}
            for(UUID id:party){var s=b.squads.get(id);if(s!=null&&bannerUnits(id)==0){notice(p.getUuid(),"Every commander needs at least one troop in their banner.");return true;}}
            for(UUID id:party){var s=b.squads.get(id);if(s!=null)s.seated=true;}
            activeBattle=null;b.startRound();return true;
        }
        return false;
    }
    static final String MARCH_SOLO="The Long March is played solo. Leave your party or room first.";
    /** Why a commander cannot start a solo battle (practice or a Long March battle) now, or "" when they can: one battle at a time, solo means no party and no room (soloRule says so), and a battlefield being prepared for someone else stays theirs. */
    String soloRefusal(UUID id,String soloRule){
        if(b.running)return "Wait until the current battle finishes.";
        if(social.group(id).size()>1||social.room(id)!=null)return soloRule;
        if(!preparingRoom.isEmpty()||pendingBattle!=null&&!id.equals(pendingCommander))return "Another battlefield is being prepared. Try again shortly.";
        return "";
    }
    /** Starts a solo battle: the commander alone on Dawn against one bot. Practice passes no row; a Long March battle passes its row, which sets the bot tier and the lessons. */
    void soloStart(ServerPlayerEntity p,Spec.CampaignBattlesRow row){
        closeParty();partyLeader=p.getUuid();party.add(p.getUuid());practice=true;size=1;fill=true;
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
        onPad.clear();padCountdown=-1;padMapRolled=false;tutorial.clear();
        b.squads.values().removeIf(s->s.bot);balance();
        for(var u:b.units){GravesAndGlory.Battle.leaveSide(u.mob);u.mob.discard();}b.units.clear();botRespawns.clear();botRetreating.clear();vacated.clear();
        for(int team=0;team<2;team++){final int t=team;int humans=(int)b.squads.values().stream().filter(s->s.team==t&&s.seated).count();
            int target=practice?(team==0?1:1):size;
            if(fill||practice)for(int i=humans;i<target;i++){UUID id=UUID.randomUUID();var s=new GravesAndGlory.Squad(id,team,b.fieldBase(team));s.bot=true;s.tier=activeBattle!=null?activeBattle.difficulty():difficulty;s.commander=Spec.CLASSES[i%Spec.CLASSES.length].id();s.lane=i%3;b.squads.put(id,s);}
        }
        participants.clear();for(var s:b.squads.values())if(!s.bot&&s.seated){participants.add(s.owner);results.remove(s.owner);joinedAt.put(s.owner,b.time);s.commander=profile(s.owner).commander;}
        for(var s:b.squads.values())if(s.seated)s.banner=banner(s);
        chooseSides();
        serial++;matchId=UUID.randomUUID().toString();introUntil=b.time+120;headline="";
    }
    /** A troop row by id, or null for empty, ^ or an unknown id. */
    static Spec.TroopsRow troop(String id){for(var t:Spec.TROOPS)if(t.id().equals(id))return t;return null;}
    /** A botBanners row as a banner list, or the starter banner when the sheet has no such row. */
    static List<String> bannerRow(String id){for(var row:Spec.BOTBANNERS)if(row.id().equals(id))return new ArrayList<>(Arrays.asList(row.slots().split(",")));return RosterMath.starterBanner();}
    /** The banner a squad fields this round: a bot's from botBanner; a War Table room member's own banner with only the units of
     *  the House he took on the lobby (lobbyHouse; Kort, 2026-10-31); anyone else's own banner with the units of Houses that
     *  cannot be fielded yet left empty. */
    List<String> banner(GravesAndGlory.Squad s){
        if(s.bot)return botBanner(s);
        var room=activeRoom.isEmpty()?null:social.rooms.get(activeRoom);
        if(room!=null&&room.members.contains(s.owner))return houseBanner(room,s.owner);
        return RosterMath.fieldable(profile(s.owner).banner,RosterMath.Catalog.fromSpec(),RosterMath.fieldableHouses());
    }
    /** A room member's banner as the lobby's House pick fields it: only that House's units. */
    List<String> houseBanner(SocialHub.Room r,UUID m){return RosterMath.houseBanner(profile(m).banner,RosterMath.Catalog.fromSpec(),lobbyHouse(r,m),RosterMath.fieldableHouses());}
    /** Units a room member would field under his House pick. */
    int houseUnits(SocialHub.Room r,UUID m){return RosterMath.units(houseBanner(r,m),RosterMath.Catalog.fromSpec());}
    /** Why a room member cannot ready under his House pick ("" when he can): his banner holds no unit of that House. The pick
     *  never borrows troops he has not placed; he picks another House or puts that House's troops in his banner first. */
    String houseRefusal(SocialHub.Room r,UUID m){if(r==null||houseUnits(r,m)>0)return "";String h=RosterMath.houseName(lobbyHouse(r,m));
        return "Your banner holds no "+h+" troops. Pick another House, or add "+h+" troops on the Banner page.";}
    /** A bot's banner (the botBanners sheet, spec 6.1): of the botBanners rows for its tier whose troops all belong to fieldable Houses, the one at lane % rows; Normal rows for a tier with none, the starter banner if no row fits. */
    List<String> botBanner(GravesAndGlory.Squad s){
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
            if(slots.contains(bonusTracker.target)&&slots.stream().allMatch(id->id.equals(RosterMath.MARKER)||id.equals(RosterMath.EMPTY)||fieldable.contains(catalog.houseOf(id))))return new ArrayList<>(slots);}
        return null;
    }
    /** The botBanners rows of a tier, in sheet order, whose every troop belongs to a fieldable House (houses.fieldable). */
    static List<Spec.BotBannersRow> fieldableBotBanners(String tier){
        var catalog=RosterMath.Catalog.fromSpec();var fieldable=RosterMath.fieldableHouses();List<Spec.BotBannersRow> out=new ArrayList<>();
        for(var row:Spec.BOTBANNERS)if(row.difficulty().equals(tier)&&Arrays.stream(row.slots().split(",")).allMatch(id->id.equals(RosterMath.MARKER)||id.equals(RosterMath.EMPTY)||fieldable.contains(catalog.houseOf(id))))out.add(row);
        return out;
    }
    /** Units in the banner a commander would field now; 0 means he has no army to bring. */
    int bannerUnits(UUID id){var catalog=RosterMath.Catalog.fromSpec();return RosterMath.units(RosterMath.fieldable(profile(id).banner,catalog,RosterMath.fieldableHouses()),catalog);}
    /** The progression multiplier of a commander's owned troop (spec 4.4, in-match progression design 8): its star bonus alone, capped at 1 + recruitment.capPct. Levels and gear come from the soldier, not the profile. Bots, and troops the commander does not own, fight at 1. */
    double progression(GravesAndGlory.Squad s,Spec.TroopsRow t){
        if(s.bot)return 1;var o=profile(s.owner).owned.get(t.id());if(o==null)return 1;
        var rarity=RosterMath.rarityRow(t.rarity());
        return RosterMath.progressionMultiplier(rarity==null?0:RosterMath.starPct(o.stars,rarity),Spec.RECRUITMENT[0].capPct());
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
        int cost=Spec.RECRUITMENT[0].rollCost();
        if(pr.gold<cost){notice(p.getUuid(),"A recruitment roll costs "+cost+" gold. Earn gold by completing battles.");return;}
        List<String> commons=new ArrayList<>(),rares=new ArrayList<>();String epic=null;
        for(var t:Spec.TROOPS)if(t.house().equals(house)){if(t.rarity().equals("common"))commons.add(t.id());else if(t.rarity().equals("rare"))rares.add(t.id());else if(t.rarity().equals("epic"))epic=t.id();}
        var common=RosterMath.rarityRow("common");var rare=RosterMath.rarityRow("rare");var epicRow=RosterMath.rarityRow("epic");
        if(commons.isEmpty()||rares.isEmpty()||epic==null||common==null||rare==null||epicRow==null)return;
        String before=JSON.toJson(pr);
        var drawn=RosterMath.roll(commons,rares,epic,common.rollWeight(),rare.rollWeight(),epicRow.rollWeight(),pr.pity.getOrDefault(house,0),Spec.RECRUITMENT[0].epicPity(),rollRng);
        pr.gold-=cost;pr.pity.put(house,drawn.pityAfter());
        var got=troop(drawn.troop());var card=rollCard(pr,got);
        String result=receive(pr,got);
        if(!save(p.getUuid())){profiles.put(p.getUuid(),JSON.fromJson(before,Profile.class));return;}
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
    void unsaved(UUID id,String before,String what){profiles.put(id,JSON.fromJson(before,Profile.class));notice(id,what+" was not saved, so nothing changed. Check disk space and try again.");}
    /**
     * A chosen recruit (buy:<troop>, spec 4.2): a Common or Rare of any House for recruitment.chosenCost gold, as a new troop or a copy toward its
     * next star. An Epic or Legendary, a troop already at full stars (a buy would only dismantle it) and a purse short of the price are refused.
     */
    void buy(ServerPlayerEntity p,Profile pr,String id){
        var t=troop(id);if(t==null)return;UUID who=p.getUuid();
        if(!t.rarity().equals("common")&&!t.rarity().equals("rare")){notice(who,"Only Commons and Rares can be bought. "+t.name()+" comes from recruitment rolls.");return;}
        var owned=pr.owned.get(id);if(owned!=null&&owned.stars>=RosterMath.maxStars()){notice(who,t.name()+" is at full stars already.");return;}
        int cost=Spec.RECRUITMENT[0].chosenCost();
        if(pr.gold<cost){notice(who,"A chosen recruit costs "+cost+" gold. You have "+pr.gold+".");return;}
        String before=JSON.toJson(pr);pr.gold-=cost;var card=rollCard(pr,t);String result=receive(pr,t);
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
        if(pact&&pr.writs<1){notice(who,"You hold no Pact. Clear every battle of a Long March chapter to earn one.");return;}
        if(!pact&&renown<R.renownForLegendary()){notice(who,t.name()+" answers at "+R.renownForLegendary()+" "+name+" Renown. You have "+renown+".");return;}
        if(!pact&&shards<R.legendaryShards()){notice(who,t.name()+" costs "+R.legendaryShards()+" "+name+" shards. You have "+shards+".");return;}
        String before=JSON.toJson(pr);
        if(pact)pr.writs--;else pr.shards.put(house,shards-R.legendaryShards());
        pr.owned.put(t.id(),new Profile.Owned());
        if(!save(who)){unsaved(who,before,"The Legendary");return;}
        notice(who,t.name()+" joins your collection"+(pact?", sworn by your Pact!":"!"));reveal(p);
    }
    /**
     * A stopped or crashed match is void (MatchRules, Kort 2026-11-01): no rewards, no losses, nothing saved to any profile, and the
     * armies go back to camp. A crash never reaches here, and since profiles are only written when a match settles (finish), a restart
     * after one pays nothing and costs nothing either; Modes.recover puts the map back.
     */
    void voidMatch(MatchRules.Ending ending){
        if(!b.running||MatchRules.settles(ending))return;
        System.err.println("[Graves and Glory] Match "+matchId+" is void ("+ending+"): no rewards and no losses; the armies return to camp.");
        for(UUID id:participants)results.remove(id);participants.clear();
        b.message="The battle was called off. Back to the war council.";headline=b.message;
        returnLobby();
    }
    void finish(int winner){
        if(!b.running)return;
        lastWinner=winner;
        long seconds=(b.time-b.roundStarted)/20;
        for(UUID id:participants){var s=b.squads.get(id);if(s==null||!s.seated)continue;Profile p=profile(id);String before=JSON.toJson(p);
            int earned=Math.min(seconds,(b.time-joinedAt.getOrDefault(id,b.roundStarted))/20)>=Spec.CAMPAIGN[0].minimumRewardSeconds()?(practice?Spec.CAMPAIGN[0].practiceGold():Spec.CAMPAIGN[0].completionGold()+(s.team==winner?Spec.CAMPAIGN[0].victoryGold():0)):0;
            int priorXp=p.progression.xp;Progression.reset(p.progression,Progression.today());Progression.match(p.progression,matchId,s.team==winner,earned>0);p.gold+=earned;p.matches++;if(s.team==winner)p.wins++;MarchMath.Settled march=null;
            if(activeBattle!=null){boolean won=s.team==winner; // plan 2C.3: the battle won, the cleared count and the bonus, in the same save as the gold
                march=MarchMath.settle(activeBattle,MarchMath.bonus(activeBattle,Spec.BONUSOBJECTIVES),won,bonusTracker!=null&&bonusTracker.kept(won,(int)seconds),p.battlesWon,p.bonusClaimed,Profile.houseOrder(),Spec.CAMPAIGNBATTLES);
                p.campaignCleared=march.cleared();if(march.bonusPaid()){p.gold+=march.bonusGold();p.shards.merge(march.bonusHouse(),march.bonusShards(),Integer::sum);}}
            // The chase (spec 4.3 to 4.6), in the same save as the gold: an eligible match pays shards and Renown by the fielded banner's Houses and adds to the mastery ledger; a chapter cleared pays its Pact.
            var slots=RosterMath.houseSlots(s.banner,RosterMath.Catalog.fromSpec());
            var paid=earned>0?RosterMath.matchRewards(slots,Houses.thresholds(Spec.HOUSES[0].id()),Profile.houseOrder(),s.team==winner):NO_REWARDS; // every House has the same slot thresholds (HousesCheck)
            paid.shards().forEach((h,n)->p.shards.merge(h,n,Integer::sum));paid.renown().forEach((h,n)->p.renown.merge(h,n,Integer::sum));
            List<String> titles=earned>0?ledger(p,s):List.of();int pacts=pacts(p);
            boolean saved=save(id);JsonObject report=new JsonObject();report.addProperty("id",matchId);report.addProperty("outcome",winner<0?"DRAW":s.team==winner?"VICTORY":"BATTLE COMPLETE");report.addProperty("map",Spec.MAPS[map].name());report.addProperty("seconds",seconds);report.addProperty("saved",saved);report.addProperty("eligible",earned>0);report.addProperty("gold",saved?earned:0);report.addProperty("xp",saved?p.progression.xp-priorXp:0);report.addProperty("beforeXp",priorXp);report.add("squads",reportSquads(s));report.add("points",reportPoints());report.add("houses",reportHouses(s,slots,saved?paid:NO_REWARDS));report.add("levels",reportLevels(s));report.add("mode",reportMode());results.put(id,report);
            if(!saved){profiles.put(id,JSON.fromJson(before,Profile.class));standing(id,"Match reward could not be saved. Progress was not changed; check disk space.");continue;}
            String extra=(march!=null&&march.bonusPaid()?" / bonus objective: +"+march.bonusShards()+" "+RosterMath.houseName(march.bonusHouse())+" shards, +"+march.bonusGold()+" gold":"")+(pacts>0?" / "+(pacts==1?"a Pact":pacts+" Pacts")+" earned":"")+(titles.isEmpty()?"":" / new title"+(titles.size()==1?" ":"s ")+String.join(", ",titles));
            standing(id,(winner<0?"Draw":s.team==winner?"Victory":"Battle complete")+" / +"+earned+" collection gold"+(earned==0?" (reward requires 3 minutes of play)":"")+extra);
        }
        b.message=(winner<0?"Draw":b.side(winner).name()+" wins")+"! Back to the war council.";headline=b.message;
        for(var p:b.server.getPlayerManager().getPlayerList())if(participants.contains(p.getUuid())){var sq=b.squads.get(p.getUuid());Audio.ending(p,winner,sq==null?-1:sq.team);} // each participant, to his own connection: the victory sting, the defeat sting for the losing side (AudioCues)
        returnLobby();
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
    /** The title a mastery reward gives (its title: part, "title:Veteran" or "frame:brass;title:Master"), or null when it gives none. */
    static String masteryTitle(String reward){for(String part:reward.split(";"))if(part.startsWith("title:")&&part.length()>6)return part.substring(6);return null;}
    /** A Pact (spec 4.4) for each Long March chapter whose every battle is cleared (campaignBattles order up to campaignCleared) and that has not paid one yet (writChapters). Returns how many were granted. */
    static int pacts(Profile p){
        var chapters=RosterMath.pactChapters(Arrays.asList(Spec.CAMPAIGNBATTLES),p.campaignCleared,p.writChapters);
        for(int chapter:chapters){p.writs++;p.writChapters.add(chapter);}
        return chapters.size();
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
            row.addProperty("kills",s.stats.kills);row.addProperty("lost",s.stats.lost);row.addProperty("gold",s.stats.goldEarned);row.addProperty("objective",s.stats.objectiveSeconds);rows.add(row);
        }
        return rows;
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
        var entries=m.ledgerEntries(b); // the mode's ledger rows (ModeLines.Entry); a mode without them (Survival until its rows land) falls back to its ledger/max/formats arrays
        if(!entries.isEmpty())for(var e:entries){ledger.add(e.value());max.add(e.max());format.add(e.format());}
        else{for(int n:m.ledger(b))ledger.add(n);for(int n:m.max(b))max.add(n);for(String f:m.formats())format.add(f);}
        j.add("ledger",ledger);j.add("max",max);j.add("format",format);
        return j;
    }
    /** Harness cameras: while false, the lobby leashes here and in the build tick leave every player where he stands. Always true in play. */
    boolean leash=true;
    void tick(){
        if(b.time%20==0)expireNotices();
        if(pendingBattle!=null&&b.ready){ // the map of a campaign battle is built: it starts unless something changed meanwhile
            var row=pendingBattle;UUID id=pendingCommander;pendingBattle=null;pendingCommander=null;
            var host=b.server.getPlayerManager().getPlayer(id);String refused=host==null?"":soloRefusal(id,MARCH_SOLO);
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
        if(!b.running){for(var p:b.server.getPlayerManager().getPlayerList())if(leash&&!CampWorld.inCamp(p.getX(),p.getY(),p.getZ()))toLobby(p);if(b.time%20==0)expireInvites();if(b.time%10==0){padTick();roomCountdown();}return;}
        // Unseated lobby players wait in the hall while a match runs.
        if(b.time%20==0)for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(leash&&(s==null||!s.seated)&&!CampWorld.inCamp(p.getX(),p.getY(),p.getZ()))toLobby(p);}
        if(b.time<introUntil){for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(s!=null&&s.seated&&p.squaredDistanceTo(b.fieldBase(s.team))>2)b.toBase(p,s.team);}return;}
        boolean economy=b.time%100==0;
        for(var s:b.squads.values())if(s.bot){
            // A dropped commander's army keeps its last orders for disconnectGraceSeconds; then its bot takes command (MatchRules).
            if(MatchRules.waiting(b.time,s.takeoverAt))continue;if(s.takeoverAt>0){s.takeoverAt=0;s.order="attack";}
            var tier=tier(s);boolean decide=b.time%Math.max(1,tier.decisionTicks())==0;
            if(!economy&&!decide)continue;
            if(economy){
                long living=b.fielded(s).count();
                if(living==0){long until=botRespawns.computeIfAbsent(s.owner,k->b.time+240);if(b.time<until)continue;}else botRespawns.remove(s.owner);
                // Buy toward the banner: each troop up to its count there, while fielded and queued slot weights fit rules.squadSize.
                Map<String,Integer> wanted=new LinkedHashMap<>();for(String id:s.banner)if(troop(id)!=null)wanted.merge(id,1,Integer::sum);
                int weight=b.fielded(s).mapToInt(u->u.spec.slotWeight()).sum()+s.training.stream().mapToInt(q->q.spec().slotWeight()).sum();
                for(var want:wanted.entrySet()){var type=troop(want.getKey());
                    long have=b.fielded(s).filter(u->u.spec==type).count()+s.training.stream().filter(q->q.spec()==type).count();
                    int price=Commander.cost(s,type); // the bot's class price (a Warlock's magic troops cost less)
                    while(have<want.getValue()&&s.gold>=price&&weight+type.slotWeight()<=GravesAndGlory.R.squadSize()){s.gold-=price;long previous=s.training.isEmpty()?b.time:s.training.getLast().finished();s.training.addLast(new GravesAndGlory.Training(type,Math.max(b.time,previous)+Commander.trainingTicks(s,type),price));weight+=type.slotWeight();have++;}
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
    /** An enemy infantry unit this close to a Hard bot's army counts as melee: no shieldwall. */
    static final double MELEE_THREAT_RANGE=8;
    /** A remembered enemy counts as near a point, or near an army, within this many times the point's radius or the tier's charge distance. */
    static final double NEAR_RADIUS_FACTOR=2;
    /** One command decision for a bot army, run at its tier's cadence: retreat or rally, pick the point to take, set the order, stamp the charge (Battle.markCharge; a bot's command plays no sound).
     *  Easy and Normal read the whole battlefield; Hard reads only what its team has seen (b.sightings), and holds the points it owns before it flanks (garrisonPoint). */
    void botDecide(GravesAndGlory.Squad s,Spec.BotCommandRow tier){
        var mine=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).toList();
        long living=mine.size();if(living==0)return;
        Vec3d center=mine.stream().map(u->u.mob.getPos()).reduce(Vec3d.ZERO,Vec3d::add).multiply(1.0/living);
        // One live view of the enemy for the whole decision: the living enemy units, and only the sightings of units that are still alive.
        Map<UUID,GravesAndGlory.Unit> foes=new HashMap<>();for(var u:b.units)if(u.squad.team!=s.team&&u.mob.isAlive())foes.put(u.mob.getUuid(),u);
        Map<UUID,GravesAndGlory.Battle.Sight> seen=new HashMap<>();for(var entry:b.sightings.getOrDefault(s.team,Map.of()).entrySet())if(foes.containsKey(entry.getKey()))seen.put(entry.getKey(),entry.getValue());
        int count=b.points.size();GravesAndGlory.Capture choice;
        int full=RosterMath.units(s.banner,RosterMath.Catalog.fromSpec()); // the units its banner lists, an Epic or a Legendary once
        if(!b.mode.botRetreats(b,s))botRetreating.remove(s.owner); // a mode can deny the fall-back (Siege's defenders behind the gate)
        else if(living<=full*tier.retreatShare())botRetreating.add(s.owner);
        else if(living>=full*tier.rallyShare())botRetreating.remove(s.owner);
        int hold=tier.flankLeastDefended()?garrisonPoint(s,center,tier):-1;
        if(hold>=0)choice=b.points.get(hold);
        else if(tier.flankLeastDefended()){
            // Every point of the current map counts (Kingsfield has five); a point is defended by the enemies this team last saw near it.
            double[] px=new double[count],pz=new double[count];int[] owner=new int[count],defenders=new int[count];
            for(int i=0;i<count;i++){var p=b.points.get(i);px[i]=p.spec.x();pz[i]=p.spec.z();owner[i]=p.owner;
                for(var sight:seen.values())if(Math.hypot(sight.x()-px[i],sight.z()-pz[i])<=NEAR_RADIUS_FACTOR*p.spec.radius())defenders[i]++;}
            choice=b.points.get(Math.max(0,BattleMath.leastDefended(center.x,center.z,px,pz,owner,defenders,s.team,tier.defenderWeight())));
        }else{
            int lane=BattleMath.laneTarget(s.lane,s.team,count); // Dusk's lane points mirror Dawn's; GravesAndGlory.reinforce mirrors its rows to match
            choice=b.points.get(lane);double best=Double.MAX_VALUE;
            for(int i=0;i<count;i++){var p=b.points.get(i);double value=Math.hypot(center.x-p.spec.x(),center.z-p.spec.z())+(p.owner==s.team?140:0)+(i==lane?0:90)-(p.contested?55:0);if(value<best){best=value;choice=p;}}
        }
        long enemies=foes.size();
        boolean couldCharge=b.startsCharge(s,"all"),retreating=botRetreating.contains(s.owner);
        String wasOrder=s.order;Vec3d wasTarget=s.target;s.assignments.clear();
        if(retreating){s.order="retreat";s.formation="loose";s.target=b.fieldBase(s.team);}
        else{
            Vec3d goal=b.mode.botGoal(b,s,center,tier); // a mode's own goal (Convoy's wagon), else the chosen point
            s.order=goal!=null||choice.owner!=s.team?"attack":"hold";s.formation=living<8?"loose":"block";s.target=goal!=null?goal:new Vec3d(choice.spec.x(),choice.spec.elevation()+1,choice.spec.z());
            if(s.order.equals("attack")){
                if(tier.chargeDistance()>0){
                    // Timed charge: judged on the enemies this team has seen, counted near the army, and only once the nearest is in range.
                    double nearest=Double.MAX_VALUE;long near=0;
                    for(var sight:seen.values()){double d=Math.hypot(sight.x()-center.x,sight.z()-center.z);nearest=Math.min(nearest,d);if(d<=NEAR_RADIUS_FACTOR*tier.chargeDistance())near++;}
                    if(near>0&&BattleMath.timedCharge(nearest,living,near,tier.chargeRatio(),tier.chargeDistance()))s.order="charge";
                }else if(enemies>0&&BattleMath.timedCharge(0,living,enemies,tier.chargeRatio(),0))s.order="charge";
            }
        }
        Vec3d objective=retreating?null:b.mode.botTarget(b,s); // a mode's own objective (Siege's stage) replaces the point plan
        if(objective!=null){s.target=objective;if(b.mode.howToSide(b,s).equals("defend"))s.order="hold";else if(!s.order.equals("charge"))s.order="attack";}
        s.yaw=s.team==0?-90:90;
        if(!retreating&&tier.shieldwallVsArchers()){
            // Brace the shield troops when remembered archers are in range and nothing has closed to melee. Only troops with a shield can brace.
            boolean shields=mine.stream().anyMatch(u->u.spec.group().equals("infantry")&&u.tags.contains("shield"));
            int ranged=0,melee=0;
            for(var entry:seen.entrySet()){var enemy=foes.get(entry.getKey());
                double d=Math.hypot(entry.getValue().x()-center.x,entry.getValue().z()-center.z);
                if(enemy.spec.group().equals("ranged")&&d<=tier.archerThreatRange())ranged++;
                else if(enemy.spec.group().equals("infantry")&&d<=MELEE_THREAT_RANGE)melee++;}
            if(BattleMath.shieldwallVsArchers(ranged,melee,tier.archerThreatCount(),shields))s.assignments.put("infantry",new GravesAndGlory.Assignment(s.order,"shieldwall",s.target,s.yaw));
        }
        if(!retreating)b.mode.botPlan(b,s,tier); // a mode's per-group split of the order (Siege's stage plan)
        if(!s.order.equals(wasOrder)||!Objects.equals(s.target,wasTarget))s.arrived.clear(); // a new order or spot ends an arrival (Tactics.arrived); the same decision again keeps it
        if(s.order.equals("charge")&&couldCharge)b.markCharge(s,"all");
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
        Projectiles.clear(b);Abilities.clearBanners(b);Houses.reset(b);b.clearSides();b.clearProgression();FeedEvents.clear();map=index;if(!Modes.registered(battleMode)||!Modes.hosts(battleMode,map))battleMode=Modes.DOMINATION;CampaignMaps.select(map);b.points.clear();for(var point:Spec.POINTS)b.points.add(new GravesAndGlory.Capture(point));b.ready=false;b.buildColumn=0;Structures.clearQueue();b.placing=false;}
    /** The lesson a commander is on, or -1: only a campaign battle whose sheet row teaches (lessons) shows them, to its commanders, until all four are done. They repeat on every replay. */
    int tutorialStep(UUID id){
        if(!b.running||activeBattle==null||!activeBattle.lessons()||!participants.contains(id))return -1;
        int step=tutorial.getOrDefault(id,0);return step>3?-1:step;
    }
    /** Advances the lesson when the commander does what it asks. The last one pays for a first recruitment roll, once per commander (tutorialDone). */
    void tutorialDid(ServerPlayerEntity p,int step){
        UUID id=p.getUuid();if(tutorialStep(id)!=step)return;
        tutorial.put(id,step+1);if(step<3)return;
        Profile pr=profile(id);if(pr.tutorialDone){notice(id,"Training complete. The reward is paid once.");return;}
        pr.tutorialDone=true;pr.gold+=Spec.RECRUITMENT[0].rollCost();save(id);
        notice(id,"Training complete: +"+Spec.RECRUITMENT[0].rollCost()+" gold, enough for one recruitment roll after the battle.");
    }
    /** Quick-match size from who is ready: half the commanders rounded up, at least the larger side, 2v2 to 4v4. A lone commander gets 3v3. */
    int padSize(){
        int n=onPad.size();if(n<2)return 3;
        int side=(int)Math.max(onPad.values().stream().filter(t->t==0).count(),onPad.values().stream().filter(t->t==1).count());
        return Math.min(4,Math.max(2,Math.max((n+1)/2,side)));
    }
    /** Quick match: humans on pads pick a side and ready up. Ten seconds after two are ready, the match starts. Any step-off cancels. */
    void padTick(){
        Map<UUID,Integer> standing=new LinkedHashMap<>();
        for(var p:b.server.getPlayerManager().getPlayerList())if(social.room(p.getUuid())==null)
            for(int team=0;team<2;team++)if(CampaignMaps.onPad(team,p.getX(),p.getY(),p.getZ()))standing.put(p.getUuid(),team);
        Map<UUID,Integer> accepted=new LinkedHashMap<>();Set<UUID> seen=new HashSet<>();int[] count={0,0};
        for(var entry:standing.entrySet()){
            UUID id=entry.getKey();if(!seen.add(id))continue;Set<UUID> group=social.group(id);seen.addAll(group);int team=entry.getValue();
            if(group.stream().anyMatch(m->!Objects.equals(standing.get(m),team))){notice(id,"Bring your whole party onto the same pad, or use a private room.");continue;}
            if(count[team]+group.size()>4){notice(id,"Your whole party will not fit on this side.");continue;}
            if(group.stream().anyMatch(m->bannerUnits(m)==0)){notice(id,"Every party member needs at least one troop in their banner.");continue;}
            for(UUID m:group){accepted.put(m,team);var squad=b.squads.get(m);if(squad!=null)squad.team=team;}count[team]+=group.size();
        }
        boolean changed=!onPad.equals(accepted);onPad.clear();onPad.putAll(accepted);mapVotes.keySet().retainAll(onPad.keySet());
        if(changed){padCountdown=-1;for(UUID id:onPad.keySet())notice(id,"Ready group changed. Countdown reset; step off to cancel.");}
        if(onPad.isEmpty()){padCountdown=-1;return;}
        if(padCountdown<0){padCountdown=200;for(UUID id:onPad.keySet())notice(id,"Starting in 10 seconds. Open the war table and press Change to vote for a map.");return;}
        padCountdown-=10;if(padCountdown<=0)startQuickMatch();else Audio.countdown(b,onPad.keySet(),padCountdown);
    }
    void startQuickMatch(){
        int selected=map,best=0;for(int i=0;i<Spec.MAPS.length;i++){final int index=i;int votes=(int)mapVotes.values().stream().filter(v->v==index).count();if(votes>best){best=votes;selected=i;}}
        if(selected!=map){switchMap(selected);padCountdown=20;for(UUID id:onPad.keySet())notice(id,"Preparing voted map: "+Spec.MAPS[selected].name()+".");return;}
        padCountdown=-1;padMapRolled=false;practice=false;size=padSize();fill=true;activeRoom="";activeBattle=null;
        for(var squad:b.squads.values())if(!squad.bot)squad.seated=onPad.containsKey(squad.owner);
        for(UUID id:onPad.keySet()){var squad=b.squads.get(id);if(squad!=null)squad.team=onPad.get(id);notice(id,"Match starting with your whole group.");}
        mapVotes.clear();b.startRound();
    }

    boolean leader(ServerPlayerEntity p){return p.getUuid().equals(partyLeader);}
    String name(UUID id){var p=b.server.getPlayerManager().getPlayer(id);return p==null?social.names.getOrDefault(id,"Commander-"+id.toString().substring(0,6)):p.getGameProfile().getName();}
    void closeParty(){partyLeader=null;party.clear();invites.clear();inviteUntil.clear();}
    void expireInvites(){for(var e:new ArrayList<>(inviteUntil.entrySet()))if(e.getValue()<=b.time){invites.remove(e.getKey());inviteUntil.remove(e.getKey());}}
    void leaveParty(UUID id){
        if(!party.contains(id))return;
        String who=name(id);party.remove(id);invites.remove(id);inviteUntil.remove(id);
        if(party.isEmpty()){closeParty();return;}
        if(id.equals(partyLeader)){partyLeader=party.iterator().next();notice(partyLeader,"You now lead the custom lobby.");}
        for(UUID member:party)notice(member,who+" left the custom lobby.");
    }
    void partyCommand(ServerPlayerEntity p,String cmd){
        UUID me=p.getUuid();
        if(b.running){notice(me,"Finish the battle first.");return;}
        if(cmd.equals("party:open")){
            if(partyLeader!=null){notice(me,"A custom lobby is already open.");return;}
            if(onPad.containsKey(me)){notice(me,"Step off the ready pad to open a lobby.");return;}
            partyLeader=me;party.add(me);notice(me,"Custom lobby open. Invite commanders from the Custom lobby tab.");return;
        }
        if(cmd.equals("party:leave")){leaveParty(me);return;}
        if(cmd.equals("party:decline")){invites.remove(me);inviteUntil.remove(me);return;}
        if(cmd.equals("party:accept")){
            Long until=inviteUntil.get(me);
            if(until==null||until<=b.time||partyLeader==null){invites.remove(me);inviteUntil.remove(me);notice(me,"That invitation has expired.");return;}
            if(party.size()>=size*2){notice(me,"This lobby is full.");return;}
            if(onPad.containsKey(me)){notice(me,"Step off the ready pad first.");return;}
            party.add(me);invites.remove(me);inviteUntil.remove(me);
            for(UUID member:party)notice(member,name(me)+" joined the custom lobby.");
            return;
        }
        if(cmd.startsWith("party:invite:")){
            if(partyLeader==null||!party.contains(me)){notice(me,"Open a custom lobby first.");return;}
            if(!leader(p)){notice(me,"Only the custom lobby leader invites.");return;}
            var target=b.server.getPlayerManager().getPlayer(cmd.substring(13));
            if(target==null||!b.squads.containsKey(target.getUuid())){notice(me,"That commander is not in this server.");return;}
            if(party.contains(target.getUuid()))return;
            if(party.size()>=size*2){notice(me,"This lobby is full.");return;}
            if(onPad.containsKey(target.getUuid())){notice(me,"That commander is on a ready pad.");return;}
            invites.put(target.getUuid(),me);inviteUntil.put(target.getUuid(),b.time+1200);
            notice(target.getUuid(),name(me)+" invites you to a custom lobby. Open the Custom lobby tab to accept.");
            notice(me,"Invited "+target.getGameProfile().getName()+".");
        }
    }
    String snapshot(ServerPlayerEntity p){
        JsonObject j=new JsonObject();Profile pr=profile(p.getUuid());j.add("profile",forClient(pr));j.addProperty("collectionReady",collectionsReady.contains(p.getUuid()));j.addProperty("waiting",b.running&&(!b.squads.containsKey(p.getUuid())||!b.squads.get(p.getUuid()).seated));j.addProperty("mode",practice?"ai":Integer.toString(size));j.addProperty("fill",fill);j.addProperty("difficulty",difficulty);j.addProperty("map",map);j.addProperty("serial",serial);j.addProperty("intro",b.squads.containsKey(p.getUuid())&&b.squads.get(p.getUuid()).seated?Math.max(0,introUntil-b.time):0);j.addProperty("notice",notices.getOrDefault(p.getUuid(),""));j.addProperty("noticeSerial",noticeSerials.getOrDefault(p.getUuid(),0));j.addProperty("headline",headline);j.addProperty("tutorial",tutorialStep(p.getUuid()));JsonObject pads=new JsonObject();pads.addProperty("countdown",padCountdown<0?0:(padCountdown+19)/20);pads.addProperty("mine",onPad.getOrDefault(p.getUuid(),-1));pads.addProperty("dawn",(int)onPad.values().stream().filter(t->t==0).count());pads.addProperty("dusk",(int)onPad.values().stream().filter(t->t==1).count());pads.addProperty("size",padSize());j.add("pads",pads);
        JsonObject battle=new JsonObject();if(activeBattle!=null){battle.addProperty("id",activeBattle.id());battle.addProperty("name",activeBattle.name());battle.addProperty("briefing",activeBattle.briefing());battle.addProperty("reason",activeBattle.reason());}j.add("battle",battle); // the Long March battle being fought, or {}
        JsonObject partyJson=new JsonObject();boolean member=party.contains(p.getUuid());Long until=inviteUntil.get(p.getUuid());UUID inviter=invites.get(p.getUuid());
        partyJson.addProperty("open",partyLeader!=null);partyJson.addProperty("member",member);partyJson.addProperty("lead",leader(p));partyJson.addProperty("capacity",size*2);
        partyJson.addProperty("leader",partyLeader==null?"":name(partyLeader));
        JsonArray members=new JsonArray();for(UUID id:party)members.add(name(id));partyJson.add("members",members);
        partyJson.addProperty("inviteSeconds",until==null?0:(int)Math.max(0,(until-b.time+19)/20));partyJson.addProperty("inviter",inviter==null?"":name(inviter));
        JsonArray online=new JsonArray();if(partyLeader!=null)for(var other:b.server.getPlayerManager().getPlayerList())if(!party.contains(other.getUuid())&&b.squads.containsKey(other.getUuid()))online.add(other.getGameProfile().getName());
        partyJson.add("online",online);j.add("party",partyJson);j.add("result",results.getOrDefault(p.getUuid(),new JsonObject()));
        // In the lobby a row carries the team the viewer's next round gives that commander (nextSeats), or -1 when he would not enter with the viewer, so the list's marks read like the side preview; in battle, the squad's team.
        Map<UUID,Integer> nextTeams=new HashMap<>();if(!b.running)for(var seat:nextSeats(p.getUuid()))nextTeams.put(seat.owner(),seat.team());
        JsonArray players=new JsonArray();for(var s:b.squads.values()){JsonObject row=new JsonObject();var player=b.server.getPlayerManager().getPlayer(s.owner);row.addProperty("name",s.bot?"AI Commander":player==null?"Commander":player.getGameProfile().getName());row.addProperty("team",b.running?s.team:nextTeams.getOrDefault(s.owner,-1));row.addProperty("bot",s.bot);players.add(row);}j.add("players",players);j.add("sides",sidesJson(p.getUuid()));j.add("banner",bannerJson(p.getUuid()));j.add("social",socialSnapshot(p.getUuid()));j.add("progression",progressSnapshot(p.getUuid()));j.add("lastRoll",lastRolls.getOrDefault(p.getUuid(),new JsonObject()));
        BattleMode shown=b.running?b.mode:Modes.create(battleMode);
        j.add("battleMode",shown.snapshot(b,b.running?b.squads.get(p.getUuid()):null));
        return fitted(j,p.getUuid());
    }
    /**
     * The seats the viewer's next round would have, for the lobby's side preview. While a start is under way they are the seated
     * commanders, exactly what chooseSides reads. Between rounds nobody is seated, so they are the commanders the viewer's start
     * would seat, balanced as prepareRound balances them: his pad group on its pads, his room on its teams, his custom lobby on
     * their teams, or he alone on team 0, as a solo battle seats him. A commander in the lobby who will not enter with him does
     * not set his preview.
     */
    List<Seat> nextSeats(UUID viewer){
        var seated=seats(s->s.seated);if(!seated.isEmpty())return seated;
        if(onPad.containsKey(viewer))return balanced(seats(s->onPad.containsKey(s.owner)).stream().map(s->new Seat(s.owner(),onPad.get(s.owner()))).toList(),false,padSize());
        var room=social.room(viewer);
        if(room!=null)return balanced(seats(s->room.members.contains(s.owner)).stream().map(s->new Seat(s.owner(),room.teams.getOrDefault(s.owner(),s.team()))).toList(),false,room.size);
        if(party.contains(viewer))return balanced(seats(s->party.contains(s.owner)),practice,size);
        return List.of(new Seat(viewer,0));
    }
    /** The side ids the two teams fly, for the viewer's snapshot: this round's while a battle runs, else chooseSides' pick over the seats his next round would have (nextSeats). */
    JsonArray sidesJson(UUID viewer){int[] rows=b.running?b.sides:sidesFor(nextSeats(viewer));JsonArray out=new JsonArray();for(int t=0;t<2;t++)out.add(Spec.FACTIONS[rows[t]].id());return out;}
    /** The banner a commander fields: his battle banner while he is seated in a running battle, else his profile banner. */
    JsonArray bannerJson(UUID id){var s=b.squads.get(id);List<String> shown=b.running&&s!=null&&s.seated?s.banner:profile(id).banner;JsonArray out=new JsonArray();for(String entry:shown)out.add(entry);return out;}
    /** The client's copy of a profile: match ids stay on the server. They only stop one match paying twice, and 256 of them (94 characters a match across both copies) would outgrow SNAPSHOT_LIMIT. */
    static JsonObject forClient(Profile p){JsonObject j=JSON.toJsonTree(p).getAsJsonObject();var s=j.getAsJsonObject(Profile.PROGRESSION);if(s!=null)s.remove(Progression.State.MATCH_GRANTS);return j;}
    static JsonObject forClient(Progression.State s){JsonObject j=JSON.toJsonTree(s).getAsJsonObject();j.remove(Progression.State.MATCH_GRANTS);return j;}
    /** The snapshot as sent: pretty when it fits SNAPSHOT_LIMIT, else compact, else compact without the battle report, else "{}" (the client then keeps its last camp state). Each shortened form is logged the first time a commander is sent it. */
    String fitted(JsonObject j,UUID id){
        String out=JSON.toJson(j);if(out.length()<=SNAPSHOT_LIMIT)return out;int full=out.length();String form="compact";
        out=WIRE.toJson(j);if(out.length()>SNAPSHOT_LIMIT){j.add("result",new JsonObject());out=WIRE.toJson(j);form="compact without the battle report";}
        if(out.length()>SNAPSHOT_LIMIT){out="{}";form="empty";}
        if(snapshotForms.computeIfAbsent(id,k->new HashSet<>()).add(form))System.err.println("[Graves and Glory] The camp snapshot for "+name(id)+" ran to "+full+" characters, over the client's "+SNAPSHOT_LIMIT+"; sent "+form+" ("+out.length()+" characters).");
        return out;
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
            case "mode"->{String said=social.mode(id,arg,Modes.ids());var room=social.room(id); // a mode that does not host the room's map takes the room to its first map
                if(said.startsWith("Mode set")&&room!=null&&!Modes.hosts(room.mode,room.map))for(int m=0;m<Spec.MAPS.length;m++)if(Modes.hosts(room.mode,m)){room.map=m;said=Modes.row(room.mode).name()+" is played on "+Spec.MAPS[m].name()+". "+said;break;}
                yield said;}case "house"->{String said=social.house(id,arg,RosterMath.fieldableHouses());String no=said.startsWith("House chosen")?houseRefusal(social.room(id),id):"";yield no.isEmpty()?said:no;}
            case "host"->{UUID targetId=social.room(id)==null?null:social.room(id).members.stream().filter(m->name(m).equals(arg)).findFirst().orElse(null);yield social.handHost(id,targetId);}case "join"->social.joinRoom(id,arg);case "exit"->social.leaveRoom(id);case "ready"->social.ready(id,houseRefusal(social.room(id),id));
            case "team"->social.team(id,Integer.parseInt(arg));
            case "size","map","fill"->{var room=social.room(id);if(room==null)yield "Create or join a room first.";
                String said=social.configure(id,action.equals("size")?Integer.parseInt(arg):room.size,action.equals("map")?Integer.parseInt(arg):room.map,action.equals("fill")?!room.fill:room.fill,Spec.MAPS.length);
                if(said.startsWith("Room settings saved")&&!Modes.hosts(room.mode,room.map)){said=Modes.row(room.mode).name()+" is not played on "+Spec.MAPS[room.map].name()+": the mode is now Domination. "+said;room.mode=Modes.DOMINATION;} // a map the room's mode does not host plays Domination
                yield said;}
            case "start"->{if(!preparingRoom.isEmpty()&&!preparingRoom.equals(social.room(id)==null?"":social.room(id).code))yield "Another room is preparing a battlefield. Your room is saved.";String failure=social.startFailure(id,b.running);if(!failure.isEmpty())yield failure;
                var room=social.room(id);if(!b.ready)yield "Preparing the battlefield. Please wait.";
                if(room.map!=map){preparingRoom=room.code;switchMap(room.map);yield "Preparing your chosen map. Your ready group will enter automatically; unready to cancel.";}
                var empty=room.members.stream().filter(m->houseUnits(room,m)==0).findFirst();if(empty.isPresent())yield name(empty.get())+"'s banner holds no "+RosterMath.houseName(lobbyHouse(room,empty.get()))+" troops.";
                size=room.size;fill=room.fill;practice=false;activeRoom=room.code;activeBattle=null;
                for(var squad:b.squads.values())if(!squad.bot){squad.seated=room.members.contains(squad.owner);if(squad.seated)squad.team=room.teams.get(squad.owner);}
                if(battleModeBeforeRoom==null)battleModeBeforeRoom=battleMode;
                battleMode=room.mode; // the room's mode plays (startRound falls back to Domination on a map the mode does not host)
                social.begin(room);b.startRound();yield "Your room is entering battle together.";}
            default->"Unknown social action.";
        };}catch(IllegalArgumentException e){result="Invalid selection.";}
        if(!before.equals(JSON.toJson(social))&&!saveSocial()){social.parties=JSON.fromJson(before,SocialHub.class).parties;result="Party change could not be saved. It was not applied; check disk space.";}
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
    String lobbyHouse(SocialHub.Room r,UUID m){String h=r.houses.get(m);return h!=null?h:RosterMath.rollHouse(profile(m).banner);}
    JsonObject socialSnapshot(UUID id){
        JsonObject j=new JsonObject();var party=social.party(id);var room=social.room(id);j.addProperty("party",party!=null);j.addProperty("partyLead",party!=null&&party.leader.equals(id));j.addProperty("leader",party==null?"":name(party.leader));
        JsonArray members=new JsonArray();if(party!=null)for(UUID m:party.members){JsonObject row=new JsonObject();row.addProperty("name",name(m));row.addProperty("online",social.online.contains(m));row.addProperty("me",m.equals(id));members.add(row);}j.add("members",members);
        var invite=social.invites.get(id);j.addProperty("inviter",invite==null?"":name(invite.sender()));j.addProperty("inviteSeconds",invite==null?0:Math.max(0,(invite.expires()-System.currentTimeMillis()+999)/1000));
        JsonArray available=new JsonArray();for(var p:b.server.getPlayerManager().getPlayerList())if(!p.getUuid().equals(id)&&social.party(p.getUuid())==null)available.add(p.getGameProfile().getName());j.add("available",available);
        j.addProperty("room",room==null?"":room.code);j.addProperty("host",room!=null&&room.leader.equals(id));j.addProperty("playing",room!=null&&room.playing);j.addProperty("preparing",room!=null&&room.code.equals(preparingRoom));j.addProperty("size",room==null?2:room.size);j.addProperty("map",room==null?0:room.map);j.addProperty("fill",room==null||room.fill);
        j.addProperty("mode",room==null?Modes.DOMINATION:room.mode);JsonArray modes=new JsonArray();Modes.ids().forEach(modes::add);j.add("modes",modes);
        j.addProperty("countdown",room==null||room.countdown<0?0:(room.countdown+19)/20);j.addProperty("waiting",room==null||room.playing?"":social.startFailure(room.leader,b.running));
        JsonArray commanders=new JsonArray();if(room!=null)for(UUID m:room.members){JsonObject row=new JsonObject();row.addProperty("name",name(m));row.addProperty("team",room.teams.get(m));row.addProperty("ready",room.ready.contains(m));row.addProperty("online",social.online.contains(m));
            row.addProperty("house",lobbyHouse(room,m));row.addProperty("troops",houseUnits(room,m));row.addProperty("class",profile(m).commander);row.addProperty("host",m.equals(room.leader));row.addProperty("me",m.equals(id));commanders.add(row);}j.add("commanders",commanders);return j;
    }

    boolean progressCommand(ServerPlayerEntity player,String cmd){
        UUID id=player.getUuid();Profile p=profile(id);String before=JSON.toJson(p);Progression.reset(p.progression,Progression.today());
        String action=cmd.substring(9),result;
        if(action.equals("reroll"))result=Progression.reroll(p.progression);
        else if(action.startsWith("title:")){String title=action.substring(6);if(p.progression.titles.contains(title)){p.progression.title=title;result="Title equipped: "+title;}else result="That title is locked.";}
        else {var reward=Progression.claim(p.progression,action);
            // A claim that grants a troop (the Founders Rare) waits for the match to end, as roll and buy do: a copy taken mid-battle would raise the stars of a fielded troop.
            var seat=b.squads.get(id);if(reward.troop()!=null&&b.running&&seat!=null&&seat.seated){profiles.put(id,JSON.fromJson(before,Profile.class));notice(id,"Claim tier "+Progression.RARE_TIER+"'s Rare after the battle: active battle choices are locked until the match ends.");return true;}
            p.gold=Math.min(1_000_000,p.gold+reward.gold());result=reward.message();
            if(reward.shards()>0&&reward.house()!=null)p.shards.merge(reward.house(),reward.shards(),Integer::sum); // a contract's or a Founders tier's shards, to the House the claim named
            var troop=reward.troop()==null?null:troop(reward.troop());if(troop!=null)result+=". "+receive(p,troop); // the Founders Rare: a new troop, a copy, or past full stars its shards
            if(reward.cosmetic()!=null)p.cosmetics.add(reward.cosmetic());}
        if(!save(id)){profiles.put(id,JSON.fromJson(before,Profile.class));result="Reward was not applied because saving failed. Try again after checking disk space.";}
        notice(id,result);return true;
    }
    JsonObject progressSnapshot(UUID id){Profile p=profile(id);Progression.reset(p.progression,Progression.today());JsonObject j=forClient(p.progression);j.addProperty("tier",Progression.tier(p.progression));var roster=RosterMath.launchRoster();j.addProperty("chapter",Progression.chapter(p.progression,(int)roster.stream().filter(p.owned::containsKey).count(),roster.size()));return j;}
    void progressEvent(UUID id,boolean capture){if(!participants.contains(id))return;var squad=b.squads.get(id);if(squad==null||!squad.seated)return;
        var state=profile(id).progression;Progression.reset(state,Progression.today());if(capture)state.dailyCaptures++;else state.dailyOrders++;}
}
