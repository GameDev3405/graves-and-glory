package gg.gravesandglory;
import java.util.*;
/** The War Table lobby's server rules (plan 3.4, 4C.3; SocialHub): seats, the host's orders, the ready countdown and bot fill. */
public final class LobbyRulesCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static UUID id(){return UUID.randomUUID();}
 /** Steps the countdown n times by 10 ticks, as Campaign.roomCountdown does every 10 ticks; returns every room that came due. */
 static List<SocialHub.Room> run(SocialHub h,int n,boolean busy,String preparing){List<SocialHub.Room> due=new ArrayList<>();for(int i=0;i<n;i++)due.addAll(h.countdown(10,busy,preparing).due());return due;}
 public static void main(String[] args){
  long now=1_000_000;
  // seat assignment
  SocialHub h=new SocialHub();UUID a=id(),b=id(),c=id(),d=id(),e=id(),f=id(),g=id(),k=id(),x=id();
  Set<UUID> all=new HashSet<>(List.of(a,b,c,d,e,f,g,k,x));h.presence(all,now);
  h.createParty(a);h.invite(a,b,now);h.accept(b,now+1);
  h.lobby(a);var r=h.room(a);
  check(r!=null&&r.size==SocialHub.LOBBY_SIZE&&r.leader.equals(a),"The lobby opens a room three a side, hosted by the party leader");
  check(r.teams.get(a)==0&&r.teams.get(b)==0,"A party sits together on Dawn");
  check(h.lobby(a).startsWith("War council open")&&h.rooms.size()==1,"Opening the lobby again keeps the same room");
  check(h.lobby(b).startsWith("War council open")&&h.room(b)==r,"A party member opening the lobby finds the party's room");
  h.joinRoom(c,r.code);check(r.teams.get(c)==1,"A newcomer takes the emptier side");
  h.joinRoom(d,r.code);check(r.teams.get(d)==1,"Sides stay level as commanders join");
  h.joinRoom(e,r.code);h.joinRoom(f,r.code);check(r.members.size()==6&&r.teams.values().stream().filter(t->t==0).count()==3,"Six commanders fill 3v3");
  check(h.joinRoom(g,r.code).startsWith("No team has room"),"A seventh is refused while the room is 3v3");
  check(h.configure(a,4,r.map,r.fill,5).startsWith("Room settings saved")&&r.size==4,"The host widens the room to 4v4");
  h.joinRoom(g,r.code);h.joinRoom(k,r.code);check(r.members.size()==8,"Eight commanders, four a side, is the most");
  check(h.joinRoom(x,r.code).startsWith("No team has room"),"A ninth is refused");
  check(h.configure(a,3,r.map,r.fill,5).startsWith("Move commanders"),"A full side cannot shrink under its commanders");
  int side=r.teams.get(g);check(h.team(g,1-side).startsWith("Your whole party will not fit"),"A side of four takes no fifth");
  h.leaveRoom(k);h.leaveRoom(g);int before=r.teams.get(c);h.team(c,1-before);check(r.teams.get(c)==1-before,"A seat on the other side moves the commander");
  h.team(c,before);
  // the host's orders
  check(h.mode(c,"domination").startsWith("Only an idle room host"),"Only the host picks the mode");
  check(h.configure(c,3,2,true,5).startsWith("Only an idle room host")&&r.map==0,"Only the host picks the map");
  check(h.mode(a,"siege").startsWith("That mode is not ready"),"A mode this base lacks is refused");
  for(UUID m:r.members)h.ready(m);check(r.ready.size()==r.members.size(),"Everyone readies");
  h.configure(a,3,2,true,5);check(r.map==2&&r.ready.isEmpty(),"The host's map pick stands and every commander readies again");
  for(UUID m:r.members)h.ready(m);h.mode(a,"domination");check(r.mode.equals("domination")&&r.ready.isEmpty(),"The host's mode pick stands and every commander readies again");
  check(h.handHost(c,a).startsWith("Only an idle room host"),"Only the host hands on the host's seat");
  check(h.handHost(a,c).startsWith("Host passed")&&r.leader.equals(c),"The host hands on the host's seat");
  check(h.mode(a,"domination").startsWith("Only an idle room host"),"The old host no longer sets orders");
  h.handHost(c,a);
  // the House pick
  Set<String> houses=Set.of("gilded","graveborn");h.ready(b);
  check(h.house(b,"wildmoot",houses).startsWith("That House cannot")&&!r.houses.containsKey(b),"A House that cannot take the field is refused");
  check(h.house(b,"graveborn",houses).startsWith("House chosen")&&r.houses.get(b).equals("graveborn")&&!r.ready.contains(b),"A House pick stands and takes back the picker's ready");
  // the countdown
  check(run(h,3,false,"").isEmpty()&&r.countdown<0,"No countdown while commanders are not ready");
  for(UUID m:r.members)h.ready(m);
  var step=h.countdown(10,false,"");check(step.started().contains(r)&&r.countdown==SocialHub.COUNTDOWN_TICKS,"Everyone ready starts the ten-second countdown");
  check(run(h,10,false,"").isEmpty()&&r.countdown==SocialHub.COUNTDOWN_TICKS-100,"The countdown runs in 10-tick steps");
  h.ready(e);step=h.countdown(10,false,"");check(step.cancelled().contains(r)&&r.countdown<0,"Anyone standing down stops the countdown");
  check(run(h,25,false,"").isEmpty(),"A stopped countdown never comes due");
  h.ready(e);h.countdown(10,false,"");check(r.countdown==SocialHub.COUNTDOWN_TICKS,"Readying again starts a fresh ten seconds");
  check(run(h,19,false,"").isEmpty(),"Nothing starts before ten seconds");
  var due=run(h,1,false,"");check(due.size()==1&&due.get(0)==r&&r.countdown<0,"The countdown comes due once, at ten seconds");
  h.countdown(10,false,"");check(r.countdown==SocialHub.COUNTDOWN_TICKS,"A room still ready after a failed start counts again");
  h.configure(a,3,1,true,5);check(h.countdown(10,false,"").cancelled().contains(r),"A change of orders stops the countdown");
  for(UUID m:r.members)h.ready(m);check(run(h,30,true,"").isEmpty()&&r.countdown<0,"A busy battlefield holds the countdown");
  h.countdown(10,false,"");run(h,5,false,"");int held=r.countdown;check(run(h,5,false,r.code).isEmpty()&&r.countdown==held,"The room preparing its map is left alone");
  h.begin(r);check(r.countdown<0&&run(h,30,false,"").isEmpty(),"A room in battle has no countdown");h.finish();
  // bot fill on and off
  SocialHub solo=new SocialHub();UUID s=id(),t=id();solo.presence(Set.of(s,t),now);solo.lobby(s);var one=solo.room(s);
  check(one.fill,"Bots fill empty seats by default");
  solo.ready(s);check(solo.startFailure(s,false).isEmpty()&&solo.countdown(10,false,"").started().contains(one),"One commander with bot fill counts down: bots take the five other seats");
  solo.configure(s,3,0,false,5);check(!one.fill&&one.ready.isEmpty(),"The host turns bot fill off");
  solo.ready(s);check(solo.startFailure(s,false).equals("Fill both teams or enable bots.")&&solo.countdown(10,false,"").started().isEmpty(),"Without bots a room short of commanders does not count down");
  check(solo.configure(t,3,0,true,5).startsWith("Only an idle room host"),"Only the host turns bot fill back on");
  solo.configure(s,1,0,false,5);solo.joinRoom(t,one.code);solo.ready(s);solo.ready(t);
  check(solo.startFailure(s,false).isEmpty()&&solo.countdown(10,false,"").started().contains(one),"Without bots a full room (1v1 here) counts down");
  // the lobby's state is never saved
  String saved=new com.google.gson.Gson().toJson(solo);check(!saved.contains(one.code)&&!saved.contains("countdown"),"Rooms, picks and countdowns are not saved");
  System.out.println("LobbyRulesCheck: "+count+" checks passed");
 }
}
