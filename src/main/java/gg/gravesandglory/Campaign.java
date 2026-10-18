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
    final GravesAndGlory.Battle b;
    final Map<UUID,Profile> profiles=new HashMap<>();
    final Path directory;
    SocialHub social=new SocialHub();String activeRoom="",preparingRoom="";
    final Map<UUID,Integer> mapVotes=new HashMap<>();
    int size=2,map=0;boolean practice=false,fill=true;
    long introUntil;int serial;String matchId=UUID.randomUUID().toString();
    final Map<UUID,Long> botRespawns=new HashMap<>();
    final Set<UUID> participants=new HashSet<>();
    final Map<UUID,Long> joinedAt=new HashMap<>();
    final Set<UUID> collectionsReady=new HashSet<>();
    /** Players with no server record when they joined; only they may bring a collection from their own computer, once. */
    final Set<UUID> importAllowed=new HashSet<>();
    /** Commanders who left mid-battle, mapped to the bot squad holding their army until they return this round. */
    final Map<UUID,UUID> vacated=new HashMap<>();
    /** First-battle lessons reached this session: 0 move, 1 formation, 2 attack, 3 barracks. */
    final Map<UUID,Integer> tutorial=new HashMap<>();
    /** AI armies that fell back stay there until rallied, so they do not walk back into the fight. */
    final Set<UUID> botRetreating=new HashSet<>();
    final Map<UUID,String> notices=new HashMap<>();
    final Map<UUID,JsonObject> results=new HashMap<>();
    String headline="";
    /** Bot difficulty for the next round: the id of a Spec.BOTCOMMAND row (easy, normal, hard). There is one battle per server, so one setting covers it. */
    String difficulty="normal";
    /** Ready pads: who stands on which pad (team), the countdown in ticks, and whether this cycle has rolled its quick-match map. */
    final Map<UUID,Integer> onPad=new HashMap<>();final Set<UUID> padRefused=new HashSet<>();
    int padCountdown=-1;boolean padMapRolled;
    /** Custom lobby: one at a time. Its leader sets mode, map and bot fill, and starts the match. Invites expire after 60 seconds. */
    UUID partyLeader;final LinkedHashSet<UUID> party=new LinkedHashSet<>();final Map<UUID,UUID> invites=new HashMap<>();final Map<UUID,Long> inviteUntil=new HashMap<>();
    public static class Profile {
        Progression.State progression=new Progression.State();
        int gold=0,matches=0,wins=0;String commander="vanguard";boolean tutorialDone;
        Set<String> unlocked=new LinkedHashSet<>();Map<String,Integer> army=new LinkedHashMap<>();
        Map<String,List<String>> deployment=new HashMap<>();
        public Profile(){for(var t:Spec.TROOPS)if(t.count()>0){unlocked.add(t.id());army.put(t.id(),t.count());}}
        int count(int team){return Arrays.stream(Spec.TROOPS).filter(t->t.faction().equals(Spec.FACTIONS[team].id())).mapToInt(t->army.getOrDefault(t.id(),0)).sum();}
        List<String> lineup(int team){
            if(deployment==null)deployment=new HashMap<>();String faction=Spec.FACTIONS[team].id();List<String> line=deployment.get(faction);Map<String,Integer> actual=new HashMap<>();
            if(line!=null)for(String id:line)if(!id.equals("empty"))actual.merge(id,1,Integer::sum);
            boolean valid=line!=null&&line.size()==20;for(var t:Spec.TROOPS)if(t.faction().equals(faction)&&actual.getOrDefault(t.id(),0)!=army.getOrDefault(t.id(),0))valid=false;
            if(!valid){line=new ArrayList<>();for(var t:Spec.TROOPS)if(t.faction().equals(faction))for(int i=0;i<army.getOrDefault(t.id(),0)&&line.size()<20;i++)line.add(t.id());while(line.size()<20)line.add("empty");deployment.put(faction,line);}return line;
        }
        void sanitize(){
            if(progression==null)progression=new Progression.State();progression.sanitize();
            if(unlocked==null)unlocked=new LinkedHashSet<>();if(army==null)army=new LinkedHashMap<>();
            unlocked.removeIf(id->Arrays.stream(Spec.TROOPS).noneMatch(t->t.id().equals(id)));
            for(var t:Spec.TROOPS)if(t.count()>0)unlocked.add(t.id());
            gold=Math.max(0,Math.min(1000000,gold));matches=Math.max(0,matches);wins=Math.max(0,Math.min(matches,wins));
            if(Arrays.stream(Spec.CLASSES).noneMatch(c->c.id().equals(commander)))commander="vanguard";
            for(int team=0;team<2;team++){int total=0;for(var t:Spec.TROOPS)if(t.faction().equals(Spec.FACTIONS[team].id())){int n=unlocked.contains(t.id())?Math.max(0,Math.min(20,army.getOrDefault(t.id(),0))):0;n=Math.min(n,20-total);army.put(t.id(),n);total+=n;}}
        }
    }
    Path layoutFile(){return b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json");}
    Campaign(GravesAndGlory.Battle battle){
        b=battle;Path save=b.server.getSavePath(WorldSavePath.ROOT);
        directory=save.resolve(Diagnostics.active()?"gravesandglory-test-profiles":"gravesandglory-profiles");
        Path groups=save.resolve("gravesandglory-parties.json");
        if(Files.exists(groups))try{social=JSON.fromJson(Files.readString(groups),SocialHub.class);if(social==null)throw new IllegalStateException("Empty party data");social.restore();}
        catch(Exception e){throw new IllegalStateException("Party data could not be read; original and .bak are preserved",e);}
        try {
            LegacyDataMigration.profiles(save.resolve(Diagnostics.active()?"bannerbound-test-profiles":"bannerbound-profiles"),directory);
            LegacyDataMigration.copyMissing(save.resolve("bannerbound-layout.json"),layoutFile());
        } catch(java.io.IOException migration) { throw new IllegalStateException("Could not upgrade legacy commander data; original files were kept",migration); }
        try{JsonObject marker=JsonParser.parseString(Files.readString(layoutFile())).getAsJsonObject();if(marker.get("layout").getAsString().equals("campaign-2"))map=marker.get("map").getAsInt();}catch(Exception ignored){}CampaignMaps.select(map);
    }
    Profile profile(UUID id){return profiles.computeIfAbsent(id,key->{Path file=directory.resolve(key+".json");if(!Files.exists(file))return new Profile();
        try{Profile p=JSON.fromJson(Files.readString(file),Profile.class);if(p==null)throw new IllegalStateException("Empty profile");p.sanitize();return p;}
        catch(Exception e){throw new IllegalStateException("Commander profile could not be read. Original and .bak preserved: "+key,e);}});}
    boolean cached(){try{JsonObject j=JsonParser.parseString(Files.readString(layoutFile())).getAsJsonObject();return j.get("layout").getAsString().equals("campaign-2")&&j.get("map").getAsInt()==map;}catch(Exception e){return false;}}
    void built(){try{Files.writeString(b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json"),"{\"layout\":\"campaign-2\",\"map\":"+map+"}");}catch(Exception e){System.err.println("[Graves and Glory] Map cache marker could not be saved.");}}
    boolean save(UUID id){try{Files.createDirectories(directory);Path path=directory.resolve(id+".json"),tmp=directory.resolve(id+".tmp");Files.writeString(tmp,JSON.toJson(profile(id)));if(Files.exists(path))Files.copy(path,path.resolveSibling(path.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
            try{Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}return true;
        }catch(Exception e){System.err.println("[Graves and Glory] Could not save commander collection: "+e.getClass().getSimpleName());notice(id,"Collection could not be saved; check disk space.");return false;}}
    void importProfile(ServerPlayerEntity p,String json){
        if(json.length()>16000)return;
        // The server's saved collection is the only source of truth. The client's copy is a display cache.
        // It is imported once, on a first visit to a single-player or LAN world with no record yet, so
        // existing collections carry over; dedicated servers never accept one.
        UUID id=p.getUuid();boolean allowed=importAllowed.remove(id)&&!b.server.isDedicated();
        try{Profile portable=JSON.fromJson(json,Profile.class);portable.sanitize();
            if(allowed){profiles.put(id,portable);save(id);}
            else{Profile stored=profile(id);if(portable.gold>stored.gold||!stored.unlocked.containsAll(portable.unlocked))System.out.println("[Graves and Glory] Ignored the collection sent by "+p.getName().getString()+"; the server's saved collection is kept.");}
        }catch(Exception ignored){}
        collectionsReady.add(id);
    }
    void notice(UUID id,String text){notices.put(id,text);var p=b.server.getPlayerManager().getPlayer(id);if(p!=null)p.sendMessage(Text.literal(text),true);}
    void joined(ServerPlayerEntity p){
        if(!Files.exists(directory.resolve(p.getUuid()+".json")))importAllowed.add(p.getUuid());profile(p.getUuid());
        // Rejoining the same battle keeps the original start time, so a dropped connection does not cost the reward.
        if(!(b.running&&participants.contains(p.getUuid())))joinedAt.put(p.getUuid(),b.time);var squad=b.squads.get(p.getUuid());
        if(b.running&&squad!=null&&squad.seated)b.toBase(p,squad.team);else toLobby(p);
        notice(p.getUuid(),b.running&&(squad==null||!squad.seated)?"Battle in progress. Wait in camp for the next round. R opens your war council.":"Welcome to Graves and Glory. R opens your war council.");}
    void toLobby(ServerPlayerEntity p){b.relocate(p,0,66,CampaignMaps.LOBBY_Z+20,180,8);p.changeGameMode(GameMode.ADVENTURE);p.getInventory().clear();p.setHealth(p.getMaxHealth());p.getHungerManager().setFoodLevel(20);}
    void returnLobby(){
        b.running=false;introUntil=0;social.finish();activeRoom="";Projectiles.clear(b);
        for(var u:b.units)u.mob.discard();b.units.clear();
        b.squads.values().removeIf(s->s.bot);botRespawns.clear();botRetreating.clear();b.respawns.clear();vacated.clear();
        for(var s:b.squads.values()){s.training.clear();s.seated=false;}onPad.clear();padCountdown=-1;closeParty();
        for(var p:b.server.getPlayerManager().getPlayerList())toLobby(p);
    }
    /** Leaving frees the pad, any seat in a custom lobby, and invitations to it. */
    void left(UUID id){onPad.remove(id);padRefused.remove(id);leaveParty(id);}
    boolean handle(ServerPlayerEntity p,String cmd){
        if(cmd.startsWith("social:"))return socialCommand(p,cmd);
        if(cmd.startsWith("progress:"))return progressCommand(p,cmd);
        if(cmd.startsWith("vote:")){try{int pick=Integer.parseInt(cmd.substring(5));if(!b.running&&onPad.containsKey(p.getUuid())&&pick>=0&&pick<Spec.MAPS.length){mapVotes.put(p.getUuid(),pick);notice(p.getUuid(),"Vote saved: "+Spec.MAPS[pick].name());}}catch(NumberFormatException ignored){}return true;}
        if(cmd.equals("camp:practice")){
            if(b.running){notice(p.getUuid(),"Wait until the current battle finishes.");return true;}
            if(social.group(p.getUuid()).size()>1||social.room(p.getUuid())!=null){notice(p.getUuid(),"Practice is solo. Use a private room to play with your party.");return true;}
            closeParty();partyLeader=p.getUuid();party.add(p.getUuid());practice=true;size=1;fill=true;
            for(var squad:b.squads.values())if(!squad.bot){squad.seated=squad.owner.equals(p.getUuid());if(squad.seated)squad.team=0;}b.startRound();return true;
        }
        if(cmd.startsWith("party:")){partyCommand(p,cmd);return true;}
        if(cmd.equals("lobby")){if(b.running)notice(p.getUuid(),"Finish the battle before returning to the lobby.");else toLobby(p);return true;}
        if(b.running){
            var current=b.squads.get(p.getUuid());
            boolean global=cmd.startsWith("map:")||cmd.startsWith("mode:")||cmd.equals("fill")||cmd.equals("start")||cmd.startsWith("difficulty:");
            boolean personal=cmd.equals("roll")||cmd.startsWith("unlock:")||cmd.startsWith("army:")||cmd.startsWith("slot:")||cmd.startsWith("class:");
            if(global||personal&&current!=null&&current.seated){notice(p.getUuid(),"Active battle choices are locked until the match ends.");return true;}
        }
        if(cmd.startsWith("difficulty:")){
            String id=cmd.substring(11);if(Arrays.stream(Spec.BOTCOMMAND).noneMatch(r->r.id().equals(id)))return true;
            if(partyLeader!=null&&!leader(p)){notice(p.getUuid(),"Only the custom lobby leader sets the bots.");return true;}
            difficulty=id;notice(p.getUuid(),"Bots: "+id.substring(0,1).toUpperCase()+id.substring(1)+".");return true;
        }
        Profile pr=profile(p.getUuid());var squad=b.squads.get(p.getUuid());if(squad==null)return false;
        if(cmd.equals("roll")){
            var locked=Arrays.stream(Spec.TROOPS).filter(t->!pr.unlocked.contains(t.id())).toList();
            if(locked.isEmpty()){notice(p.getUuid(),"Your collection is complete!");return true;}
            if(pr.gold<Spec.CAMPAIGN[0].rollCost()){notice(p.getUuid(),"A unit roll costs 100 gold. Earn gold by completing battles.");return true;}
            var reward=locked.get(b.world.random.nextInt(locked.size()));pr.gold-=Spec.CAMPAIGN[0].rollCost();pr.unlocked.add(reward.id());if(!save(p.getUuid())){pr.unlocked.remove(reward.id());pr.gold+=Spec.CAMPAIGN[0].rollCost();return true;}
            notice(p.getUuid(),"Unlocked permanently: "+reward.name()+"!");p.playSound(net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory","unit_reveal")),.7f,1f);return true;
        }
        if(cmd.startsWith("unlock:")){
            var pick=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(cmd.substring(7))).findFirst().orElse(null);
            if(pick==null)return true;if(pr.unlocked.contains(pick.id())){notice(p.getUuid(),pick.name()+" is already in your collection.");return true;}
            if(pr.gold<Spec.CAMPAIGN[0].rollCost()){notice(p.getUuid(),"Unlocking a troop costs "+Spec.CAMPAIGN[0].rollCost()+" gold. Earn gold by completing battles.");return true;}
            pr.gold-=Spec.CAMPAIGN[0].rollCost();pr.unlocked.add(pick.id());if(!save(p.getUuid())){pr.unlocked.remove(pick.id());pr.gold+=Spec.CAMPAIGN[0].rollCost();return true;}
            notice(p.getUuid(),"Unlocked permanently: "+pick.name()+"!");p.playSound(net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory","unit_reveal")),.7f,1f);return true;
        }
        if(cmd.startsWith("class:")){String id=cmd.substring(6);if(Arrays.stream(Spec.CLASSES).anyMatch(c->c.id().equals(id))){pr.commander=id;squad.commander=id;save(p.getUuid());}return true;}
        if(cmd.startsWith("slot:")){
            String[] parts=cmd.split(":");if(parts.length!=3)return true;int slot;try{slot=Integer.parseInt(parts[1]);}catch(Exception e){return true;}
            if(slot<0||slot>=20)return true;String id=parts[2];var type=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(id)&&t.faction().equals(Spec.FACTIONS[squad.team].id())).findFirst().orElse(null);
            if(!id.equals("empty")&&(type==null||!unlocked(squad,id)))return true;
            var line=pr.lineup(squad.team);line.set(slot,id);for(var t:Spec.TROOPS)if(t.faction().equals(Spec.FACTIONS[squad.team].id()))pr.army.put(t.id(),(int)line.stream().filter(t.id()::equals).count());save(p.getUuid());return true;
        }
        if(cmd.startsWith("army:")){
            String[] parts=cmd.split(":");if(parts.length!=3)return true;
            var spec=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(parts[1])&&t.faction().equals(Spec.FACTIONS[squad.team].id())).findFirst().orElse(null);
            if(spec==null||!pr.unlocked.contains(spec.id())&&!practice)return true;
            int delta=parts[2].equals("add")?1:parts[2].equals("remove")?-1:0,n=pr.army.getOrDefault(spec.id(),0);
            if(delta<0&&n>0||delta>0&&pr.count(squad.team)<20){var line=pr.lineup(squad.team);if(delta>0){int slot=line.indexOf("empty");if(slot>=0)line.set(slot,spec.id());}else{int slot=line.lastIndexOf(spec.id());if(slot>=0)line.set(slot,"empty");}pr.army.put(spec.id(),n+delta);save(p.getUuid());}return true;
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
        if(cmd.equals("start")){
            if(!leader(p)){notice(p.getUuid(),"Only the custom lobby leader starts the match.");return true;}
            if(!fill&&!practice&&party.size()!=size*2){notice(p.getUuid(),"Waiting for commanders. Enable bot fill to play now.");return true;}
            if(practice&&party.size()>1){notice(p.getUuid(),"Practice is for one commander.");return true;}
            for(UUID id:party){var s=b.squads.get(id);if(s!=null&&profile(id).count(s.team)==0){notice(p.getUuid(),"Every commander needs at least one troop selected.");return true;}}
            for(UUID id:party){var s=b.squads.get(id);if(s!=null)s.seated=true;}
            b.startRound();return true;
        }
        return false;
    }
    void balance(){int[] n={0,0};for(var s:b.squads.values())if(!s.bot&&s.seated){if(!practice&&n[s.team]>=size)s.team=1-s.team;n[s.team]++;}}
    void prepareRound(){
        onPad.clear();padCountdown=-1;padMapRolled=false;
        b.squads.values().removeIf(s->s.bot);balance();
        for(var u:b.units)u.mob.discard();b.units.clear();botRespawns.clear();botRetreating.clear();vacated.clear();
        for(int team=0;team<2;team++){final int t=team;int humans=(int)b.squads.values().stream().filter(s->s.team==t&&s.seated).count();
            int target=practice?(team==0?1:1):size;
            if(fill||practice)for(int i=humans;i<target;i++){UUID id=UUID.randomUUID();var s=new GravesAndGlory.Squad(id,team,GravesAndGlory.Battle.base(team));s.bot=true;s.tier=difficulty;s.commander=Spec.CLASSES[i%3].id();s.lane=i%3;b.squads.put(id,s);}
        }
        participants.clear();for(var s:b.squads.values())if(!s.bot&&s.seated){participants.add(s.owner);results.remove(s.owner);joinedAt.put(s.owner,b.time);s.commander=profile(s.owner).commander;}
        serial++;matchId=UUID.randomUUID().toString();introUntil=b.time+120;headline="";
    }
    int amount(GravesAndGlory.Squad s,Spec.TroopsRow t){return s.bot?t.count():unlocked(s,t.id())?profile(s.owner).army.getOrDefault(t.id(),0):0;}
    boolean unlocked(GravesAndGlory.Squad s,String id){return s.bot||practice||profile(s.owner).unlocked.contains(id);}
    double damage(GravesAndGlory.Unit u){return switch(u.squad.commander){case "vanguard"->u.spec.group().equals("infantry")?1.10:1;case "marksman"->u.spec.group().equals("ranged")?1.15:1;case "arcanist"->u.spec.group().equals("support")?1.20:1;default->1;};}
    double health(GravesAndGlory.Squad s,Spec.TroopsRow t){return s.commander.equals("vanguard")&&t.group().equals("infantry")||s.commander.equals("arcanist")&&t.group().equals("support")?1.2:1;}
    void finish(int winner){
        if(!b.running)return;
        long seconds=(b.time-b.roundStarted)/20;
        for(UUID id:participants){var s=b.squads.get(id);if(s==null||!s.seated)continue;Profile p=profile(id);String before=JSON.toJson(p);
            int earned=Math.min(seconds,(b.time-joinedAt.getOrDefault(id,b.roundStarted))/20)>=Spec.CAMPAIGN[0].minimumRewardSeconds()?(practice?Spec.CAMPAIGN[0].practiceGold():Spec.CAMPAIGN[0].completionGold()+(s.team==winner?Spec.CAMPAIGN[0].victoryGold():0)):0;
            int priorXp=p.progression.xp;Progression.reset(p.progression,Progression.today());Progression.match(p.progression,matchId,s.team==winner,earned>0);p.gold+=earned;p.matches++;if(s.team==winner)p.wins++;
            boolean saved=save(id);JsonObject report=new JsonObject();report.addProperty("id",matchId);report.addProperty("outcome",winner<0?"DRAW":s.team==winner?"VICTORY":"BATTLE COMPLETE");report.addProperty("map",Spec.MAPS[map].name());report.addProperty("seconds",seconds);report.addProperty("saved",saved);report.addProperty("eligible",earned>0);report.addProperty("gold",saved?earned:0);report.addProperty("xp",saved?p.progression.xp-priorXp:0);report.addProperty("beforeXp",priorXp);results.put(id,report);
            if(!saved){profiles.put(id,JSON.fromJson(before,Profile.class));notice(id,"Match reward could not be saved. Progress was not changed; check disk space.");continue;}notice(id,(winner<0?"Draw":s.team==winner?"Victory":"Battle complete")+" / +"+earned+" collection gold"+(earned==0?" (reward requires 3 minutes of play)":""));
        }
        b.message=(winner<0?"Draw":Spec.FACTIONS[winner].name()+" wins")+"! Back to the war council.";headline=b.message;
        for(var p:b.server.getPlayerManager().getPlayerList())if(participants.contains(p.getUuid()))p.playSound(net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory","victory")),.8f,1);
        returnLobby();
    }
    void tick(){
        if(!preparingRoom.isEmpty()&&b.ready&&!b.running){
            var pending=social.rooms.get(preparingRoom);String code=preparingRoom;preparingRoom="";
            if(pending!=null){var host=b.server.getPlayerManager().getPlayer(pending.leader);
                if(host!=null&&pending.map==map&&social.startFailure(pending.leader,false).isEmpty())socialCommand(host,"social:start");
                else for(UUID member:pending.members)notice(member,"Map prepared. Match cancelled because readiness or room membership changed.");}
        }
        if(b.time%20==0)social.presence(new HashSet<>(b.server.getPlayerManager().getPlayerList().stream().map(ServerPlayerEntity::getUuid).toList()),System.currentTimeMillis());
        if(!b.running){for(var p:b.server.getPlayerManager().getPlayerList())if(Math.abs(p.getX())>40||Math.abs(p.getZ()-CampaignMaps.LOBBY_Z)>30||p.getY()<63)toLobby(p);if(b.time%20==0)expireInvites();if(b.time%10==0)padTick();return;}
        // Unseated lobby players wait in the hall while a match runs.
        if(b.time%20==0)for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if((s==null||!s.seated)&&(Math.abs(p.getX())>40||Math.abs(p.getZ()-CampaignMaps.LOBBY_Z)>30||p.getY()<63))toLobby(p);}
        if(b.time<introUntil){for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(s!=null&&s.seated&&p.squaredDistanceTo(GravesAndGlory.Battle.base(s.team))>2)b.toBase(p,s.team);}return;}
        boolean economy=b.time%100==0;
        for(var s:b.squads.values())if(s.bot){
            var tier=tier(s);boolean decide=b.time%Math.max(1,tier.decisionTicks())==0;
            if(!economy&&!decide)continue;
            if(economy){
                long living=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).count();
                if(living==0){long until=botRespawns.computeIfAbsent(s.owner,k->b.time+240);if(b.time<until)continue;}else botRespawns.remove(s.owner);
                for(var type:Spec.TROOPS)if(type.faction().equals(Spec.FACTIONS[s.team].id())&&type.count()>0){
                    long have=b.units.stream().filter(u->u.squad==s&&u.spec==type&&u.mob.isAlive()).count()+s.training.stream().filter(q->q.spec()==type).count();
                    while(have<type.count()&&s.gold>=type.cost()&&living+s.training.size()<20){s.gold-=type.cost();long previous=s.training.isEmpty()?b.time:s.training.getLast().finished();s.training.addLast(new GravesAndGlory.Training(type,Math.max(b.time,previous)+type.trainingSeconds()*20L));have++;}
                }
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
    /** One command decision for a bot army, run at its tier's cadence: retreat or rally, pick the point to take, set the order, sound the charge horn.
     *  Easy and Normal read the whole battlefield; Hard reads only what its team has seen (b.sightings). */
    void botDecide(GravesAndGlory.Squad s,Spec.BotCommandRow tier){
        var mine=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).toList();
        long living=mine.size();if(living==0)return;
        Vec3d center=mine.stream().map(u->u.mob.getPos()).reduce(Vec3d.ZERO,Vec3d::add).multiply(1.0/living);
        Map<UUID,GravesAndGlory.Battle.Sight> seen=b.sightings.getOrDefault(s.team,Map.of());
        int count=b.points.size();GravesAndGlory.Capture choice;
        if(tier.flankLeastDefended()){
            // Every point of the current map counts (Kingsfield has five); a point is defended by the enemies this team last saw near it.
            double[] px=new double[count],pz=new double[count];int[] owner=new int[count],defenders=new int[count];
            for(int i=0;i<count;i++){var p=b.points.get(i);px[i]=p.spec.x();pz[i]=p.spec.z();owner[i]=p.owner;
                for(var sight:seen.values())if(Math.hypot(sight.x()-px[i],sight.z()-pz[i])<=2*p.spec.radius())defenders[i]++;}
            choice=b.points.get(Math.max(0,BattleMath.leastDefended(center.x,center.z,px,pz,owner,defenders,s.team,tier.defenderWeight())));
        }else{
            choice=b.points.get(s.lane%count);double best=Double.MAX_VALUE;
            for(int i=0;i<count;i++){var p=b.points.get(i);double value=Math.hypot(center.x-p.spec.x(),center.z-p.spec.z())+(p.owner==s.team?140:0)+(i==s.lane?0:90)-(p.contested?55:0);if(value<best){best=value;choice=p;}}
        }
        int full=Arrays.stream(Spec.TROOPS).filter(t->t.faction().equals(Spec.FACTIONS[s.team].id())).mapToInt(Spec.TroopsRow::count).sum();
        if(living<=full*tier.retreatShare())botRetreating.add(s.owner);
        else if(living>=full*tier.rallyShare())botRetreating.remove(s.owner);
        long enemies=b.units.stream().filter(u->u.squad.team!=s.team&&u.mob.isAlive()).count();
        boolean couldCharge=b.startsCharge(s,"all"),retreating=botRetreating.contains(s.owner);
        s.assignments.clear();
        if(retreating){s.order="retreat";s.formation="loose";s.target=GravesAndGlory.Battle.base(s.team);}
        else{
            s.order=choice.owner==s.team?"hold":"attack";s.formation=living<8?"loose":"block";s.target=new Vec3d(choice.spec.x(),choice.spec.elevation()+1,choice.spec.z());
            if(s.order.equals("attack")){
                if(tier.chargeDistance()>0){
                    // Timed charge: judged on the enemies this team has seen, counted near the army, and only once the nearest is in range.
                    double nearest=Double.MAX_VALUE;long near=0;
                    for(var sight:seen.values()){double d=Math.hypot(sight.x()-center.x,sight.z()-center.z);nearest=Math.min(nearest,d);if(d<=2*tier.chargeDistance())near++;}
                    if(near>0&&BattleMath.timedCharge(nearest,living,near,tier.chargeRatio(),tier.chargeDistance()))s.order="charge";
                }else if(enemies>0&&BattleMath.timedCharge(0,living,enemies,tier.chargeRatio(),0))s.order="charge";
            }
            s.yaw=s.team==0?-90:90;
        }
        if(!retreating&&tier.shieldwallVsArchers()){
            // Brace the shield troops when remembered archers are in range and nothing has closed to melee. Only troops with a shield can brace.
            boolean shields=mine.stream().anyMatch(u->u.spec.group().equals("infantry")&&u.spec.ability().equals("shield"));
            Map<UUID,GravesAndGlory.Unit> known=new HashMap<>();for(var u:b.units)if(u.squad.team!=s.team&&u.mob.isAlive())known.put(u.mob.getUuid(),u);
            int ranged=0,melee=0;
            for(var entry:seen.entrySet()){var enemy=known.get(entry.getKey());if(enemy==null)continue;
                double d=Math.hypot(entry.getValue().x()-center.x,entry.getValue().z()-center.z);
                if(enemy.spec.group().equals("ranged")&&d<=tier.archerThreatRange())ranged++;
                else if(enemy.spec.group().equals("infantry")&&d<=MELEE_THREAT_RANGE)melee++;}
            if(BattleMath.shieldwallVsArchers(ranged,melee,tier.archerThreatCount(),shields))s.assignments.put("infantry",new GravesAndGlory.Assignment(s.order,"shieldwall",s.target,s.yaw));
        }
        if(s.order.equals("charge")&&couldCharge)b.chargeHorn(s,"all");
    }
    /** Switches the battlefield. The arena rebuilds on the next ticks; the lobby stays. */
    void switchMap(int index){Projectiles.clear(b);map=index;CampaignMaps.select(map);b.points.clear();for(var point:Spec.POINTS)b.points.add(new GravesAndGlory.Capture(point));b.ready=false;b.buildColumn=0;}
    /** The lesson a commander is on during their first battle, or -1 once they have finished it or played before. */
    int tutorialStep(UUID id){Profile pr=profile(id);return !b.running||!participants.contains(id)||pr.tutorialDone||pr.matches>0?-1:tutorial.getOrDefault(id,0);}
    /** Advances the first-battle lesson when the commander does what it asks; the last one pays for the first troop unlock. */
    void tutorialDid(ServerPlayerEntity p,int step){
        UUID id=p.getUuid();if(tutorialStep(id)!=step)return;
        if(step<3){tutorial.put(id,step+1);return;}
        Profile pr=profile(id);pr.tutorialDone=true;pr.gold+=Spec.CAMPAIGN[0].rollCost();save(id);tutorial.remove(id);
        notice(id,"Training complete: +"+Spec.CAMPAIGN[0].rollCost()+" gold, enough to unlock a troop of your choice after the battle.");
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
            if(group.stream().anyMatch(m->profile(m).count(team)==0)){notice(id,"Every party member needs a troop selected for this faction.");continue;}
            for(UUID m:group){accepted.put(m,team);var squad=b.squads.get(m);if(squad!=null)squad.team=team;}count[team]+=group.size();
        }
        boolean changed=!onPad.equals(accepted);onPad.clear();onPad.putAll(accepted);mapVotes.keySet().retainAll(onPad.keySet());
        if(changed){padCountdown=-1;for(UUID id:onPad.keySet())notice(id,"Ready group changed. Countdown reset; step off to cancel.");}
        if(onPad.isEmpty()){padCountdown=-1;return;}
        if(padCountdown<0){padCountdown=200;for(UUID id:onPad.keySet())notice(id,"Starting in 10 seconds. Open camp guide to vote for a map.");return;}
        padCountdown-=10;if(padCountdown<=0)startQuickMatch();
    }
    void startQuickMatch(){
        int selected=map,best=0;for(int i=0;i<Spec.MAPS.length;i++){final int index=i;int votes=(int)mapVotes.values().stream().filter(v->v==index).count();if(votes>best){best=votes;selected=i;}}
        if(selected!=map){switchMap(selected);padCountdown=20;for(UUID id:onPad.keySet())notice(id,"Preparing voted map: "+Spec.MAPS[selected].name()+".");return;}
        padCountdown=-1;padMapRolled=false;practice=false;size=padSize();fill=true;activeRoom="";
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
        JsonObject j=new JsonObject();Profile pr=profile(p.getUuid());j.add("profile",JSON.toJsonTree(pr));j.addProperty("collectionReady",collectionsReady.contains(p.getUuid()));j.addProperty("waiting",b.running&&(!b.squads.containsKey(p.getUuid())||!b.squads.get(p.getUuid()).seated));j.addProperty("mode",practice?"ai":Integer.toString(size));j.addProperty("fill",fill);j.addProperty("difficulty",difficulty);j.addProperty("map",map);j.addProperty("serial",serial);j.addProperty("intro",b.squads.containsKey(p.getUuid())&&b.squads.get(p.getUuid()).seated?Math.max(0,introUntil-b.time):0);j.addProperty("notice",notices.getOrDefault(p.getUuid(),""));j.addProperty("headline",headline);j.addProperty("tutorial",tutorialStep(p.getUuid()));JsonObject pads=new JsonObject();pads.addProperty("countdown",padCountdown<0?0:(padCountdown+19)/20);pads.addProperty("mine",onPad.getOrDefault(p.getUuid(),-1));pads.addProperty("dawn",(int)onPad.values().stream().filter(t->t==0).count());pads.addProperty("dusk",(int)onPad.values().stream().filter(t->t==1).count());pads.addProperty("size",padSize());j.add("pads",pads);
        JsonObject partyJson=new JsonObject();boolean member=party.contains(p.getUuid());Long until=inviteUntil.get(p.getUuid());UUID inviter=invites.get(p.getUuid());
        partyJson.addProperty("open",partyLeader!=null);partyJson.addProperty("member",member);partyJson.addProperty("lead",leader(p));partyJson.addProperty("capacity",size*2);
        partyJson.addProperty("leader",partyLeader==null?"":name(partyLeader));
        JsonArray members=new JsonArray();for(UUID id:party)members.add(name(id));partyJson.add("members",members);
        partyJson.addProperty("inviteSeconds",until==null?0:(int)Math.max(0,(until-b.time+19)/20));partyJson.addProperty("inviter",inviter==null?"":name(inviter));
        JsonArray online=new JsonArray();if(partyLeader!=null)for(var other:b.server.getPlayerManager().getPlayerList())if(!party.contains(other.getUuid())&&b.squads.containsKey(other.getUuid()))online.add(other.getGameProfile().getName());
        partyJson.add("online",online);j.add("party",partyJson);j.add("result",results.getOrDefault(p.getUuid(),new JsonObject()));
        JsonArray players=new JsonArray();for(var s:b.squads.values()){JsonObject row=new JsonObject();var player=b.server.getPlayerManager().getPlayer(s.owner);row.addProperty("name",s.bot?"AI Commander":player==null?"Commander":player.getGameProfile().getName());row.addProperty("team",s.team);row.addProperty("bot",s.bot);players.add(row);}j.add("players",players);j.add("social",socialSnapshot(p.getUuid()));j.add("progression",progressSnapshot(p.getUuid()));return JSON.toJson(j);
    }

    boolean saveSocial(){
        Path file=b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-parties.json"),tmp=file.resolveSibling(file.getFileName()+".tmp");
        try{Files.writeString(tmp,JSON.toJson(social));if(Files.exists(file))Files.copy(file,file.resolveSibling(file.getFileName()+".bak"),StandardCopyOption.REPLACE_EXISTING);
            try{Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING);}return true;
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
            case "room"->social.createRoom(id);case "join"->social.joinRoom(id,arg);case "exit"->social.leaveRoom(id);case "ready"->social.ready(id);
            case "team"->social.team(id,Integer.parseInt(arg));
            case "size","map","fill"->{var room=social.room(id);if(room==null)yield "Create or join a room first.";
                yield social.configure(id,action.equals("size")?Integer.parseInt(arg):room.size,action.equals("map")?Integer.parseInt(arg):room.map,action.equals("fill")?!room.fill:room.fill,Spec.MAPS.length);}
            case "start"->{if(!preparingRoom.isEmpty()&&!preparingRoom.equals(social.room(id)==null?"":social.room(id).code))yield "Another room is preparing a battlefield. Your room is saved.";String failure=social.startFailure(id,b.running);if(!failure.isEmpty())yield failure;
                var room=social.room(id);if(!b.ready)yield "Preparing the battlefield. Please wait.";
                if(room.map!=map){preparingRoom=room.code;switchMap(room.map);yield "Preparing your chosen map. Your ready group will enter automatically; unready to cancel.";}
                boolean empty=room.members.stream().anyMatch(m->profile(m).count(room.teams.get(m))==0);if(empty)yield "Every commander needs at least one troop in their selected faction.";
                size=room.size;fill=room.fill;practice=false;activeRoom=room.code;
                for(var squad:b.squads.values())if(!squad.bot){squad.seated=room.members.contains(squad.owner);if(squad.seated)squad.team=room.teams.get(squad.owner);}
                social.begin(room);b.startRound();yield "Your room is entering battle together.";}
            default->"Unknown social action.";
        };}catch(IllegalArgumentException e){result="Invalid selection.";}
        if(!before.equals(JSON.toJson(social))&&!saveSocial()){social.parties=JSON.fromJson(before,SocialHub.class).parties;result="Party change could not be saved. It was not applied; check disk space.";}
        notice(id,result);return true;
    }
    JsonObject socialSnapshot(UUID id){
        JsonObject j=new JsonObject();var party=social.party(id);var room=social.room(id);j.addProperty("party",party!=null);j.addProperty("partyLead",party!=null&&party.leader.equals(id));j.addProperty("leader",party==null?"":name(party.leader));
        JsonArray members=new JsonArray();if(party!=null)for(UUID m:party.members){JsonObject row=new JsonObject();row.addProperty("name",name(m));row.addProperty("online",social.online.contains(m));row.addProperty("me",m.equals(id));members.add(row);}j.add("members",members);
        var invite=social.invites.get(id);j.addProperty("inviter",invite==null?"":name(invite.sender()));j.addProperty("inviteSeconds",invite==null?0:Math.max(0,(invite.expires()-System.currentTimeMillis()+999)/1000));
        JsonArray available=new JsonArray();for(var p:b.server.getPlayerManager().getPlayerList())if(!p.getUuid().equals(id)&&social.party(p.getUuid())==null)available.add(p.getGameProfile().getName());j.add("available",available);
        j.addProperty("room",room==null?"":room.code);j.addProperty("host",room!=null&&room.leader.equals(id));j.addProperty("playing",room!=null&&room.playing);j.addProperty("preparing",room!=null&&room.code.equals(preparingRoom));j.addProperty("size",room==null?2:room.size);j.addProperty("map",room==null?0:room.map);j.addProperty("fill",room==null||room.fill);
        JsonArray commanders=new JsonArray();if(room!=null)for(UUID m:room.members){JsonObject row=new JsonObject();row.addProperty("name",name(m));row.addProperty("team",room.teams.get(m));row.addProperty("ready",room.ready.contains(m));row.addProperty("online",social.online.contains(m));commanders.add(row);}j.add("commanders",commanders);return j;
    }

    boolean progressCommand(ServerPlayerEntity player,String cmd){
        UUID id=player.getUuid();Profile p=profile(id);String before=JSON.toJson(p);Progression.reset(p.progression,Progression.today());
        String action=cmd.substring(9),result;
        if(action.equals("reroll"))result=Progression.reroll(p.progression);
        else if(action.startsWith("title:")){String title=action.substring(6);if(p.progression.titles.contains(title)){p.progression.title=title;result="Title equipped: "+title;}else result="That title is locked.";}
        else {var reward=Progression.claim(p.progression,action);p.gold=Math.min(1_000_000,p.gold+reward.gold());result=reward.message();}
        if(!save(id)){profiles.put(id,JSON.fromJson(before,Profile.class));result="Reward was not applied because saving failed. Try again after checking disk space.";}
        notice(id,result);return true;
    }
    JsonObject progressSnapshot(UUID id){Profile p=profile(id);Progression.reset(p.progression,Progression.today());JsonObject j=JSON.toJsonTree(p.progression).getAsJsonObject();j.addProperty("tier",Progression.tier(p.progression));j.addProperty("chapter",Progression.chapter(p.progression,p.unlocked.size(),Spec.TROOPS.length));return j;}
    void progressEvent(UUID id,boolean capture){if(!participants.contains(id))return;var squad=b.squads.get(id);if(squad==null||!squad.seated)return;
        var state=profile(id).progression;Progression.reset(state,Progression.today());if(capture)state.dailyCaptures++;else state.dailyOrders++;}
}
