package gg.gravesandglory;

import java.util.*;

/** Server-owned social rules. Persistent friend groups are independent of match rooms. */
final class SocialHub {
    static final int PARTY_LIMIT=4;
    static final long INVITE_MS=60_000, RECONNECT_MS=120_000;
    static final class Party {
        UUID id=UUID.randomUUID(),leader;
        LinkedHashSet<UUID> members=new LinkedHashSet<>();
        Party(UUID leader){this.leader=leader;members.add(leader);}
    }
    /** The War Table lobby (plan 3.4, 4C.3): a room the lobby opens seats three a side; its ready countdown runs COUNTDOWN_TICKS. */
    static final int LOBBY_SIZE=3,COUNTDOWN_TICKS=200;
    /** The modes a host can pick, in order. Domination is the only mode on this base; the mode frame (plan 2C.1) adds Siege and the rest. */
    /** The modes this class allows on its own (standalone checks, the client's fallback list). The server passes Modes.ids(), the registered battle modes, so the room's list and the round's cannot drift apart. */
    static final List<String> MODES=List.of("domination");
    static final class Room {
        String code;UUID leader;int size=2,map;boolean fill=true,playing;
        LinkedHashSet<UUID> members=new LinkedHashSet<>();
        Map<UUID,Integer> teams=new HashMap<>();Set<UUID> ready=new HashSet<>();
        /** Lobby only, never saved (rooms are transient): the host's mode, the ready countdown in ticks (-1 idle) and each member's House pick. */
        String mode="domination";int countdown=-1;Map<UUID,String> houses=new HashMap<>();
        Room(String code,UUID leader){this.code=code;this.leader=leader;}
    }
    record Invite(UUID party,UUID sender,long expires) {}
    Map<UUID,Party> parties=new LinkedHashMap<>();
    Map<UUID,String> names=new HashMap<>();
    transient Map<String,Room> rooms=new LinkedHashMap<>();
    transient Map<UUID,Invite> invites=new HashMap<>();
    transient Map<UUID,Long> offlineSince=new HashMap<>();
    transient Set<UUID> online=new HashSet<>();
    transient long revision;
    void restore(){rooms=new LinkedHashMap<>();invites=new HashMap<>();offlineSince=new HashMap<>();online=new HashSet<>();
        if(names==null)names=new HashMap<>();if(parties==null)parties=new LinkedHashMap<>();Set<UUID> seen=new HashSet<>();
        parties.values().removeIf(p->p==null||p.id==null||p.members==null||p.members.isEmpty()||p.members.size()>PARTY_LIMIT);
        for(Party p:parties.values()){p.members.removeIf(id->id==null||!seen.add(id));if(!p.members.contains(p.leader)&&!p.members.isEmpty())p.leader=p.members.iterator().next();}
        parties.values().removeIf(p->p.members.isEmpty());}
    Party party(UUID id){return parties.values().stream().filter(p->p.members.contains(id)).findFirst().orElse(null);}
    Room room(UUID id){return rooms.values().stream().filter(r->r.members.contains(id)).findFirst().orElse(null);}
    Set<UUID> group(UUID id){Party p=party(id);return p==null?Set.of(id):Set.copyOf(p.members);}
    boolean lead(UUID id){Party p=party(id);return p==null||id.equals(p.leader);}
    String createParty(UUID id){if(party(id)!=null)return "You already have a party.";if(room(id)!=null)return "Leave your room before changing your party.";
        Party p=new Party(id);parties.put(p.id,p);revision++;return "Party created. Invite up to three friends.";}
    String invite(UUID from,UUID to,long now){Party p=party(from);if(p==null||!from.equals(p.leader))return "Only the party leader can invite.";
        if(room(from)!=null)return "Leave your room before inviting party members.";
        if(!online.contains(to))return "That friend is offline.";if(party(to)!=null)return "That friend is already in a party.";
        if(p.members.size()>=PARTY_LIMIT)return "Party full: four commanders maximum.";
        invites.put(to,new Invite(p.id,from,now+INVITE_MS));revision++;return "Invitation sent (60 seconds).";}
    String accept(UUID id,long now){Invite i=invites.get(id);if(i==null||i.expires<=now){invites.remove(id);return "That invitation expired.";}
        Party p=parties.get(i.party);if(p==null||!p.leader.equals(i.sender)||!online.contains(i.sender)){invites.remove(id);return "The inviting party is no longer available.";}
        if(party(id)!=null||room(id)!=null)return "Leave your current party or room first.";
        if(room(p.leader)!=null)return "The party entered a room. Ask for a new invite after it leaves.";
        if(p.members.size()>=PARTY_LIMIT)return "Party filled before you accepted.";
        p.members.add(id);invites.remove(id);revision++;return "Joined party. Your group stays together between matches.";}
    String decline(UUID id){invites.remove(id);revision++;return "Invitation declined.";}
    String leaveParty(UUID id){Party p=party(id);if(p==null)return "You are not in a party.";
        if(p.members.stream().anyMatch(member->room(member)!=null))return "Leave the room before changing the party.";
        p.members.remove(id);if(p.members.isEmpty())parties.remove(p.id);else if(id.equals(p.leader))p.leader=p.members.stream().filter(online::contains).findFirst().orElse(p.members.iterator().next());
        invites.values().removeIf(i->i.party.equals(p.id));revision++;return "Left party.";}
    String transfer(UUID leader,UUID target){Party p=party(leader);if(p==null||!leader.equals(p.leader)||!p.members.contains(target))return "Only the leader can transfer to a party member.";
        if(!online.contains(target))return "Choose an online member.";p.leader=target;invites.values().removeIf(i->i.party.equals(p.id));revision++;return "Party leader transferred.";}
    String kick(UUID leader,UUID target){Party p=party(leader);if(p==null||!leader.equals(p.leader)||leader.equals(target)||!p.members.contains(target))return "Only the leader can remove another party member.";
        if(p.members.stream().anyMatch(member->room(member)!=null))return "Leave the room before changing the party.";
        p.members.remove(target);revision++;return "Member removed from party.";}
    String disband(UUID id){Party p=party(id);if(p==null||!id.equals(p.leader))return "Only the party leader can disband.";
        if(p.members.stream().anyMatch(member->room(member)!=null))return "Leave the room before disbanding.";
        parties.remove(p.id);invites.values().removeIf(i->i.party.equals(p.id));revision++;return "Party disbanded.";}
    String claim(UUID id,long now){Party p=party(id);if(p==null)return "You are not in a party.";
        if(online.contains(p.leader)||now-offlineSince.getOrDefault(p.leader,now)<RECONNECT_MS)return "Leader has a two-minute reconnect reservation.";
        p.leader=id;revision++;return "You now lead the party.";}
    String createRoom(UUID id){if(!lead(id))return "Only your party leader can create a room.";
        if(group(id).stream().anyMatch(m->room(m)!=null))return "Your group already has a room.";
        if(!online.containsAll(group(id)))return "Wait for all party members to reconnect, or update the party.";
        String code;do{code=UUID.randomUUID().toString().replace("-","").substring(0,8).toUpperCase(Locale.ROOT);}while(rooms.containsKey(code));
        Room r=new Room(code,id);r.size=Math.max(2,group(id).size());r.members.addAll(group(id));for(UUID m:r.members)r.teams.put(m,0);rooms.put(code,r);revision++;
        return "Private room created. Share code "+code+" with friends.";}
    /** The War Table lobby's room: the group's room if it has one, else a new one seated LOBBY_SIZE a side (or the group's size). */
    String lobby(UUID id){if(room(id)!=null)return "War council open.";String made=createRoom(id);Room r=room(id);
        if(r==null)return made;r.size=Math.max(LOBBY_SIZE,r.size);return "War council open. Share code "+r.code+" with friends.";}
    String joinRoom(UUID id,String code){if(!lead(id))return "Only your party leader can join a room.";
        Room r=rooms.get(code.toUpperCase(Locale.ROOT));if(r==null)return "Room code not found.";if(r.playing)return "That room is playing. Try after the match.";
        Set<UUID> group=group(id);if(group.stream().anyMatch(m->room(m)!=null))return "Your group must leave its current room first.";
        if(!online.containsAll(group))return "Wait for your whole party to reconnect.";
        int a=(int)r.teams.values().stream().filter(t->t==0).count(),b=r.members.size()-a;int team=a<=b?0:1;
        if(Math.min(a,b)+group.size()>r.size)return "No team has room for your whole party. Ask the host for a larger mode.";
        r.members.addAll(group);for(UUID m:group)r.teams.put(m,team);r.ready.clear();revision++;return "Joined room together. Choose a team and ready up.";}
    String leaveRoom(UUID id){Room r=room(id);if(r==null)return "No room to leave.";if(r.playing)return "Finish the match before leaving the room.";
        if(!lead(id))return "Your party leader leaves the room for the whole group.";
        remove(r,group(id));revision++;return party(id)==null?"You left the room.":"Your group left the room; your party is still together.";}
    void remove(Room r,Set<UUID> ids){r.members.removeAll(ids);r.teams.keySet().removeAll(ids);r.houses.keySet().removeAll(ids);r.ready.clear();
        if(r.members.isEmpty())rooms.remove(r.code);else if(!r.members.contains(r.leader))r.leader=r.members.stream().filter(online::contains).findFirst().orElse(r.members.iterator().next());}
    String team(UUID id,int team){Room r=room(id);if(r==null||r.playing||team<0||team>1)return "Team selection is unavailable.";
        if(!lead(id))return "Your party leader selects a team for the whole group.";
        Set<UUID> group=group(id);long others=r.teams.entrySet().stream().filter(e->!group.contains(e.getKey())&&e.getValue()==team).count();
        if(others+group.size()>r.size)return "Your whole party will not fit on that team.";
        for(UUID m:group)r.teams.put(m,team);r.ready.clear();revision++;return "Whole group moved. Ready checks reset.";}
    String configure(UUID id,int size,int map,boolean fill,int mapCount){Room r=room(id);if(r==null||!id.equals(r.leader)||r.playing)return "Only an idle room host changes settings.";
        if(size<1||size>4||map<0||map>=mapCount)return "Invalid room settings.";
        for(int team=0;team<2;team++){final int t=team;if(r.teams.values().stream().filter(v->v==t).count()>size)return "Move commanders before shrinking that team.";}
        r.size=size;r.map=map;r.fill=fill;r.ready.clear();revision++;return "Room settings saved. Everyone must ready up again.";}
    String mode(UUID id,String mode){return mode(id,mode,MODES);}
    String mode(UUID id,String mode,List<String> allowed){Room r=room(id);if(r==null||!id.equals(r.leader)||r.playing)return "Only an idle room host changes settings.";
        if(!allowed.contains(mode))return "That mode is not ready yet.";r.mode=mode;r.ready.clear();revision++;return "Mode set. Everyone must ready up again.";}
    /** A member's House for the lobby (shown on the seat); a pick takes the picker's ready back. */
    String house(UUID id,String house,Set<String> allowed){Room r=room(id);if(r==null||r.playing)return "Join an idle room first.";
        if(!allowed.contains(house))return "That House cannot take the field yet.";r.houses.put(id,house);r.ready.remove(id);revision++;return "House chosen. Ready up when you are set.";}
    String handHost(UUID id,UUID target){Room r=room(id);if(r==null||!id.equals(r.leader)||r.playing)return "Only an idle room host passes the host's seat.";
        if(target==null||target.equals(id)||!r.members.contains(target))return "Choose another commander in this room.";if(!online.contains(target))return "Choose an online commander.";
        r.leader=target;r.ready.clear();revision++;return "Host passed. Everyone must ready up again.";}
    /** What one countdown step did: rooms whose countdown began, rooms whose countdown stopped, and rooms whose countdown ran out. */
    record Countdown(List<Room> started,List<Room> cancelled,List<Room> due) {}
    /** Advances each idle room's ready countdown by step ticks. A room that can start (startFailure is empty) begins at COUNTDOWN_TICKS
     *  and counts down; once it can no longer start (an un-ready, a settings change, a member gone) its countdown stops. The room preparing a map is left alone. */
    Countdown countdown(int step,boolean busy,String preparing){List<Room> started=new ArrayList<>(),cancelled=new ArrayList<>(),due=new ArrayList<>();
        for(Room r:rooms.values()){if(r.playing||r.code.equals(preparing))continue;
            if(!startFailure(r.leader,busy).isEmpty()){if(r.countdown>=0){r.countdown=-1;cancelled.add(r);revision++;}continue;}
            if(r.countdown<0){r.countdown=COUNTDOWN_TICKS;started.add(r);revision++;continue;}
            r.countdown-=step;revision++;if(r.countdown<=0){r.countdown=-1;due.add(r);}}
        return new Countdown(started,cancelled,due);}
    String ready(UUID id){return ready(id,"");}
    /** Ready or stand down; refusal, when not empty, is why this member cannot ready (the lobby's House pick fields no troop of
     *  his banner, say): it stops a Ready and never a stand-down. */
    String ready(UUID id,String refusal){Room r=room(id);if(r==null||r.playing)return "Join an idle room first.";
        if(!r.ready.contains(id)&&refusal!=null&&!refusal.isEmpty())return refusal;
        if(!r.ready.remove(id))r.ready.add(id);revision++;return r.ready.contains(id)?"Ready. The battle begins ten seconds after everyone is ready.":"Not ready.";}
    String startFailure(UUID id,boolean battleBusy){Room r=room(id);if(r==null||!id.equals(r.leader))return "Only the room host starts a match.";
        if(r.playing||battleBusy)return "The battlefield is busy. Your room and settings are saved.";
        if(!online.containsAll(r.members))return "Waiting for disconnected members to reconnect.";
        if(!r.ready.containsAll(r.members))return "Every commander must ready up.";
        if(!r.fill&&r.members.size()!=r.size*2)return "Fill both teams or enable bots.";
        for(UUID m:r.members)if(!r.members.containsAll(group(m)))return "Your whole party must be in the room.";
        return "";}
    void begin(Room r){r.playing=true;r.countdown=-1;revision++;}
    void finish(){for(Room r:rooms.values())if(r.playing){r.playing=false;r.ready.clear();}revision++;}
    void presence(Set<UUID> connected,long now){for(Room r:rooms.values())if(!r.playing)r.ready.removeIf(id->!connected.contains(id)); // a playing room keeps its flags (a commander who drops mid-round is still ready when he returns); finish() clears them
        online=new HashSet<>(connected);invites.values().removeIf(i->i.expires<=now);
        Set<UUID> known=new HashSet<>();for(Party p:parties.values())known.addAll(p.members);for(Room r:rooms.values())known.addAll(r.members);
        for(UUID id:known){if(online.contains(id))offlineSince.remove(id);else offlineSince.putIfAbsent(id,now);}
        for(Room r:new ArrayList<>(rooms.values())){if(r.playing)continue;Set<UUID> expired=new HashSet<>();
            for(UUID m:r.members)if(!online.contains(m)&&now-offlineSince.getOrDefault(m,now)>=RECONNECT_MS)expired.addAll(group(m));
            if(!expired.isEmpty()){remove(r,expired);revision++;}}
    }
}
