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
    int size=2,map=0;boolean practice=false,fill=true;
    long introUntil;int serial;
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
    String headline="";
    /** Ready pads: who stands on which pad (team), the countdown in ticks, and whether this cycle has rolled its quick-match map. */
    final Map<UUID,Integer> onPad=new HashMap<>();final Set<UUID> padRefused=new HashSet<>();
    int padCountdown=-1;boolean padMapRolled;
    /** Custom lobby: one at a time. Its leader sets mode, map and bot fill, and starts the match. Invites expire after 60 seconds. */
    UUID partyLeader;final LinkedHashSet<UUID> party=new LinkedHashSet<>();final Map<UUID,UUID> invites=new HashMap<>();final Map<UUID,Long> inviteUntil=new HashMap<>();
    public static class Profile {
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
        try {
            LegacyDataMigration.profiles(save.resolve(Diagnostics.active()?"bannerbound-test-profiles":"bannerbound-profiles"),directory);
            LegacyDataMigration.copyMissing(save.resolve("bannerbound-layout.json"),layoutFile());
        } catch(java.io.IOException migration) { throw new IllegalStateException("Could not upgrade legacy commander data; original files were kept",migration); }
        try{JsonObject marker=JsonParser.parseString(Files.readString(layoutFile())).getAsJsonObject();if(marker.get("layout").getAsString().equals("campaign-2"))map=marker.get("map").getAsInt();}catch(Exception ignored){}CampaignMaps.select(map);
    }
    Profile profile(UUID id){return profiles.computeIfAbsent(id,key->{try{Profile p=JSON.fromJson(Files.readString(directory.resolve(key+".json")),Profile.class);p.sanitize();return p;}catch(Exception e){return new Profile();}});}
    boolean cached(){try{JsonObject j=JsonParser.parseString(Files.readString(layoutFile())).getAsJsonObject();return j.get("layout").getAsString().equals("campaign-2")&&j.get("map").getAsInt()==map;}catch(Exception e){return false;}}
    void built(){try{Files.writeString(b.server.getSavePath(WorldSavePath.ROOT).resolve("gravesandglory-layout.json"),"{\"layout\":\"campaign-2\",\"map\":"+map+"}");}catch(Exception e){System.err.println("[Graves and Glory] Map cache marker could not be saved.");}}
    void save(UUID id){try{Files.createDirectories(directory);Path path=directory.resolve(id+".json"),tmp=directory.resolve(id+".tmp");Files.writeString(tmp,JSON.toJson(profile(id)));Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}catch(Exception e){System.err.println("[Graves and Glory] Could not save commander collection: "+e.getClass().getSimpleName());notice(id,"Collection could not be saved; check disk space.");}}
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
        if(!(b.running&&participants.contains(p.getUuid())))joinedAt.put(p.getUuid(),b.time);if(b.running)b.toBase(p,b.squads.get(p.getUuid()).team);else toLobby(p);notice(p.getUuid(),"Welcome to Graves and Glory. R opens your war council.");}
    void toLobby(ServerPlayerEntity p){b.relocate(p,0,66,CampaignMaps.LOBBY_Z,180,10);p.changeGameMode(GameMode.ADVENTURE);p.setHealth(p.getMaxHealth());}
    void returnLobby(){
        b.running=false;introUntil=0;
        for(var u:b.units)u.mob.discard();b.units.clear();
        b.squads.values().removeIf(s->s.bot);botRespawns.clear();botRetreating.clear();b.respawns.clear();vacated.clear();
        for(var s:b.squads.values()){s.training.clear();s.seated=false;}onPad.clear();padCountdown=-1;closeParty();
        for(var p:b.server.getPlayerManager().getPlayerList())toLobby(p);
    }
    /** Leaving frees the pad, any seat in a custom lobby, and invitations to it. */
    void left(UUID id){onPad.remove(id);padRefused.remove(id);leaveParty(id);}
    boolean handle(ServerPlayerEntity p,String cmd){
        if(cmd.startsWith("party:")){partyCommand(p,cmd);return true;}
        if(cmd.equals("lobby")){if(b.running)notice(p.getUuid(),"Finish the battle before returning to the lobby.");else toLobby(p);return true;}
        if(b.running){
            boolean locked=cmd.startsWith("roll")||cmd.startsWith("unlock:")||cmd.startsWith("army:")||cmd.startsWith("slot:")||cmd.startsWith("class:")||cmd.startsWith("map:")||cmd.startsWith("mode:")||cmd.equals("fill");
            if(locked)notice(p.getUuid(),"Army, class and battlefield choices are locked until this battle ends.");return locked;
        }
        Profile pr=profile(p.getUuid());var squad=b.squads.get(p.getUuid());if(squad==null)return false;
        if(cmd.equals("roll")){
            var locked=Arrays.stream(Spec.TROOPS).filter(t->!pr.unlocked.contains(t.id())).toList();
            if(locked.isEmpty()){notice(p.getUuid(),"Your collection is complete!");return true;}
            if(pr.gold<Spec.CAMPAIGN[0].rollCost()){notice(p.getUuid(),"A unit roll costs 100 gold. Earn gold by completing battles.");return true;}
            var reward=locked.get(b.world.random.nextInt(locked.size()));pr.gold-=Spec.CAMPAIGN[0].rollCost();pr.unlocked.add(reward.id());save(p.getUuid());
            notice(p.getUuid(),"Unlocked permanently: "+reward.name()+"!");p.playSound(net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory","unit_reveal")),.7f,1f);return true;
        }
        if(cmd.startsWith("unlock:")){
            var pick=Arrays.stream(Spec.TROOPS).filter(t->t.id().equals(cmd.substring(7))).findFirst().orElse(null);
            if(pick==null)return true;if(pr.unlocked.contains(pick.id())){notice(p.getUuid(),pick.name()+" is already in your collection.");return true;}
            if(pr.gold<Spec.CAMPAIGN[0].rollCost()){notice(p.getUuid(),"Unlocking a troop costs "+Spec.CAMPAIGN[0].rollCost()+" gold. Earn gold by completing battles.");return true;}
            pr.gold-=Spec.CAMPAIGN[0].rollCost();pr.unlocked.add(pick.id());save(p.getUuid());
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
            if(index>=0&&index<4&&index!=map){switchMap(index);notice(p.getUuid(),"Preparing "+Spec.MAPS[map].name()+". Your army choices are saved.");}
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
            if(fill||practice)for(int i=humans;i<target;i++){UUID id=UUID.randomUUID();var s=new GravesAndGlory.Squad(id,team,GravesAndGlory.Battle.base(team));s.bot=true;s.commander=Spec.CLASSES[i%3].id();s.lane=i%3;b.squads.put(id,s);}
        }
        participants.clear();for(var s:b.squads.values())if(!s.bot&&s.seated){participants.add(s.owner);joinedAt.put(s.owner,b.time);s.commander=profile(s.owner).commander;}
        serial++;introUntil=b.time+120;headline="";
    }
    int amount(GravesAndGlory.Squad s,Spec.TroopsRow t){return s.bot?t.count():unlocked(s,t.id())?profile(s.owner).army.getOrDefault(t.id(),0):0;}
    boolean unlocked(GravesAndGlory.Squad s,String id){return s.bot||practice||profile(s.owner).unlocked.contains(id);}
    double damage(GravesAndGlory.Unit u){return switch(u.squad.commander){case "vanguard"->u.spec.group().equals("infantry")?1.10:1;case "marksman"->u.spec.group().equals("ranged")?1.15:1;case "arcanist"->u.spec.group().equals("support")?1.20:1;default->1;};}
    double health(GravesAndGlory.Squad s,Spec.TroopsRow t){return s.commander.equals("vanguard")&&t.group().equals("infantry")||s.commander.equals("arcanist")&&t.group().equals("support")?1.2:1;}
    void finish(int winner){
        if(!b.running)return;
        long seconds=(b.time-b.roundStarted)/20;
        for(UUID id:participants){var s=b.squads.get(id);if(s==null)continue;Profile p=profile(id);
            int earned=Math.min(seconds,(b.time-joinedAt.getOrDefault(id,b.roundStarted))/20)>=Spec.CAMPAIGN[0].minimumRewardSeconds()?(practice?Spec.CAMPAIGN[0].practiceGold():Spec.CAMPAIGN[0].completionGold()+(s.team==winner?Spec.CAMPAIGN[0].victoryGold():0)):0;
            p.gold+=earned;p.matches++;if(s.team==winner)p.wins++;save(id);notice(id,(winner<0?"Draw":s.team==winner?"Victory":"Battle complete")+" / +"+earned+" collection gold"+(earned==0?" (reward requires 3 minutes of play)":""));
        }
        b.message=(winner<0?"Draw":Spec.FACTIONS[winner].name()+" wins")+"! Back to the war council.";headline=b.message;
        for(var p:b.server.getPlayerManager().getPlayerList())p.playSound(net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory","victory")),.8f,1);
        returnLobby();
    }
    void tick(){
        if(!b.running){for(var p:b.server.getPlayerManager().getPlayerList())if(Math.abs(p.getX())>40||Math.abs(p.getZ()-CampaignMaps.LOBBY_Z)>30||p.getY()<63)toLobby(p);if(b.time%20==0)expireInvites();if(b.time%10==0)padTick();return;}
        // Unseated lobby players wait in the hall while a match runs.
        if(b.time%20==0)for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(s==null||!s.seated)toLobby(p);}
        if(b.time<introUntil){for(var p:b.server.getPlayerManager().getPlayerList()){var s=b.squads.get(p.getUuid());if(s!=null&&p.squaredDistanceTo(GravesAndGlory.Battle.base(s.team))>2)b.toBase(p,s.team);}return;}
        if(b.time%100!=0)return;
        for(var s:b.squads.values())if(s.bot){
            long living=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).count();
            if(living==0){long until=botRespawns.computeIfAbsent(s.owner,k->b.time+240);if(b.time<until)continue;}else botRespawns.remove(s.owner);
            for(var type:Spec.TROOPS)if(type.faction().equals(Spec.FACTIONS[s.team].id())&&type.count()>0){
                long have=b.units.stream().filter(u->u.squad==s&&u.spec==type&&u.mob.isAlive()).count()+s.training.stream().filter(q->q.spec()==type).count();
                while(have<type.count()&&s.gold>=type.cost()&&living+s.training.size()<20){s.gold-=type.cost();long previous=s.training.isEmpty()?b.time:s.training.getLast().finished();s.training.addLast(new GravesAndGlory.Training(type,Math.max(b.time,previous)+type.trainingSeconds()*20L));have++;}
            }
            if(living==0)continue;

            Vec3d center=b.units.stream().filter(u->u.squad==s&&u.mob.isAlive()).map(u->u.mob.getPos()).reduce(Vec3d.ZERO,Vec3d::add).multiply(1.0/living);
            GravesAndGlory.Capture choice=b.points.get(s.lane%3);double best=Double.MAX_VALUE;
            for(int i=0;i<b.points.size();i++){var p=b.points.get(i);double value=Math.hypot(center.x-p.spec.x(),center.z-p.spec.z())+(p.owner==s.team?140:0)+(i==s.lane?0:90)-(p.contested?55:0);if(value<best){best=value;choice=p;}}
            int full=Arrays.stream(Spec.TROOPS).filter(t->t.faction().equals(Spec.FACTIONS[s.team].id())).mapToInt(Spec.TroopsRow::count).sum();
            var tactics=Spec.BOTCOMMAND[0];
            if(living<=full*tactics.retreatShare())botRetreating.add(s.owner);
            else if(living>=full*tactics.rallyShare())botRetreating.remove(s.owner);
            long enemies=b.units.stream().filter(u->u.squad.team!=s.team&&u.mob.isAlive()).count();
            s.assignments.clear();
            if(botRetreating.contains(s.owner)){s.order="retreat";s.formation="loose";s.target=GravesAndGlory.Battle.base(s.team);}
            else{
                s.order=choice.owner==s.team?"hold":"attack";s.formation=living<8?"loose":"block";s.target=new Vec3d(choice.spec.x(),choice.spec.elevation()+1,choice.spec.z());
                if(s.order.equals("attack")&&enemies>0&&living>=enemies*tactics.chargeRatio())s.order="charge";
            }
            s.yaw=s.team==0?-90:90;
        }
    }
    /** Switches the battlefield. The arena rebuilds on the next ticks; the lobby stays. */
    void switchMap(int index){map=index;CampaignMaps.select(map);b.points.clear();for(var point:Spec.POINTS)b.points.add(new GravesAndGlory.Capture(point));b.ready=false;b.buildColumn=0;}
    /** The lesson a commander is on during their first battle, or -1 once they have finished it or played before. */
    int tutorialStep(UUID id){Profile pr=profile(id);return !b.running||pr.tutorialDone||pr.matches>0?-1:tutorial.getOrDefault(id,0);}
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
        if(partyLeader!=null){
            for(var p:b.server.getPlayerManager().getPlayerList())for(int team=0;team<2;team++)if(CampaignMaps.onPad(team,p.getX(),p.getY(),p.getZ()))notice(p.getUuid(),"A custom lobby is open. Pads reopen when it closes.");
            return;
        }
        Map<UUID,Integer> standing=new HashMap<>();
        for(var p:b.server.getPlayerManager().getPlayerList())for(int team=0;team<2;team++)if(CampaignMaps.onPad(team,p.getX(),p.getY(),p.getZ()))standing.put(p.getUuid(),team);
        boolean stepped=false;
        for(UUID id:new ArrayList<>(onPad.keySet()))if(!Objects.equals(standing.get(id),onPad.get(id))){onPad.remove(id);stepped=true;}
        padRefused.removeIf(id->!standing.containsKey(id));
        if(stepped){
            if(padCountdown>=0)for(UUID id:onPad.keySet())notice(id,"Countdown cancelled.");
            padCountdown=-1;if(onPad.size()<2)padMapRolled=false;return;
        }
        for(var entry:standing.entrySet()){
            UUID id=entry.getKey();int team=entry.getValue();var s=b.squads.get(id);
            if(s==null||onPad.containsKey(id)||padRefused.contains(id))continue;
            if(onPad.values().stream().filter(t->t==team).count()>=4){notice(id,"That side is full.");padRefused.add(id);continue;}
            if(profile(id).count(team)==0){notice(id,"Every commander needs at least one troop selected.");padRefused.add(id);continue;}
            onPad.put(id,team);s.team=team;notice(id,"Ready for "+Spec.FACTIONS[team].name()+". Step off to leave.");
        }
        // One commander alone on the server may start a 3v3 against bots; with others online, two are still needed.
        boolean solo=onPad.size()==1&&b.server.getPlayerManager().getPlayerList().size()==1;
        if(onPad.size()<2&&!solo){padCountdown=-1;padMapRolled=false;return;}
        if(padCountdown<0){
            if(!padMapRolled){
                padMapRolled=true;int roll=b.world.random.nextInt(4);
                if(roll!=map){switchMap(roll);for(UUID id:onPad.keySet())notice(id,"Quick match map: "+Spec.MAPS[map].name()+".");return;}
            }
            padCountdown=200;for(UUID id:onPad.keySet())notice(id,"Match starts in 10 seconds: "+padSize()+"v"+padSize()+", bots fill empty seats.");
            return;
        }
        padCountdown-=10;
        if(padCountdown<=0)startQuickMatch();
    }
    void startQuickMatch(){
        padCountdown=-1;padMapRolled=false;practice=false;size=padSize();fill=true;
        for(UUID id:onPad.keySet()){var s=b.squads.get(id);if(s==null)continue;s.team=onPad.get(id);s.seated=true;notice(id,"Match starting.");}
        b.startRound();
    }
    boolean leader(ServerPlayerEntity p){return p.getUuid().equals(partyLeader);}
    String name(UUID id){var p=b.server.getPlayerManager().getPlayer(id);return p==null?"Commander":p.getGameProfile().getName();}
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
        JsonObject j=new JsonObject();Profile pr=profile(p.getUuid());j.add("profile",JSON.toJsonTree(pr));j.addProperty("collectionReady",collectionsReady.contains(p.getUuid()));j.addProperty("mode",practice?"ai":Integer.toString(size));j.addProperty("fill",fill);j.addProperty("map",map);j.addProperty("serial",serial);j.addProperty("intro",Math.max(0,introUntil-b.time));j.addProperty("notice",notices.getOrDefault(p.getUuid(),""));j.addProperty("headline",headline);j.addProperty("tutorial",tutorialStep(p.getUuid()));JsonObject pads=new JsonObject();pads.addProperty("countdown",padCountdown<0?0:(padCountdown+19)/20);pads.addProperty("mine",onPad.getOrDefault(p.getUuid(),-1));pads.addProperty("dawn",(int)onPad.values().stream().filter(t->t==0).count());pads.addProperty("dusk",(int)onPad.values().stream().filter(t->t==1).count());pads.addProperty("size",padSize());j.add("pads",pads);
        JsonObject partyJson=new JsonObject();boolean member=party.contains(p.getUuid());Long until=inviteUntil.get(p.getUuid());UUID inviter=invites.get(p.getUuid());
        partyJson.addProperty("open",partyLeader!=null);partyJson.addProperty("member",member);partyJson.addProperty("lead",leader(p));partyJson.addProperty("capacity",size*2);
        partyJson.addProperty("leader",partyLeader==null?"":name(partyLeader));
        JsonArray members=new JsonArray();for(UUID id:party)members.add(name(id));partyJson.add("members",members);
        partyJson.addProperty("inviteSeconds",until==null?0:(int)Math.max(0,(until-b.time+19)/20));partyJson.addProperty("inviter",inviter==null?"":name(inviter));
        JsonArray online=new JsonArray();if(partyLeader!=null)for(var other:b.server.getPlayerManager().getPlayerList())if(!party.contains(other.getUuid())&&b.squads.containsKey(other.getUuid()))online.add(other.getGameProfile().getName());
        partyJson.add("online",online);j.add("party",partyJson);
        JsonArray players=new JsonArray();for(var s:b.squads.values()){JsonObject row=new JsonObject();var player=b.server.getPlayerManager().getPlayer(s.owner);row.addProperty("name",s.bot?"AI Commander":player==null?"Commander":player.getGameProfile().getName());row.addProperty("team",s.team);row.addProperty("bot",s.bot);players.add(row);}j.add("players",players);return JSON.toJson(j);
    }
}
