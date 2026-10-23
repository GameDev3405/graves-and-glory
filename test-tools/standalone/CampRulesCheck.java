package gg.gravesandglory;
import java.util.*;
import java.time.*;
public final class CampRulesCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static UUID id(){return UUID.randomUUID();}
 public static void main(String[] args){
  SocialHub h=new SocialHub();UUID a=id(),b=id(),c=id(),d=id(),e=id(),f=id();long now=1_000_000;
  h.presence(Set.of(a,b,c,d,e,f),now);h.createParty(a);h.invite(a,b,now);h.accept(b,now+1);
  check(h.party(a)==h.party(b),"Invite joins same persistent group");
  h.createParty(c);h.invite(c,d,now);h.accept(d,now+1);h.createRoom(a);var r=h.room(a);h.createRoom(c);var second=h.room(c);
  check(r!=second&&h.rooms.size()==2,"Two private rooms have independent records");h.configure(c,3,2,false,5);
  check(r.size==2&&r.map==0&&r.fill,"Other room settings do not change first room");
  h.leaveRoom(c);h.joinRoom(c,r.code);check(h.room(d)==r&&r.teams.get(c).equals(r.teams.get(d)),"Whole group joins one team");
  h.team(c,0);check(r.teams.get(c)==1,"No capacity overflow or split on team move");
  h.ready(a);h.ready(b);h.ready(c);check(!h.startFailure(a,false).isEmpty(),"All members must ready");h.ready(d);check(h.startFailure(a,false).isEmpty(),"Fully ready private match can start");
  check(!h.startFailure(b,false).isEmpty(),"Non-host cannot start");check(!h.startFailure(a,true).isEmpty(),"Busy battlefield preserves waiting room");
  h.begin(r);check(h.leaveRoom(a).contains("Finish"),"Membership locked during match");h.finish();check(h.party(a).members.contains(b)&&h.room(a)==r&&r.ready.isEmpty(),"Party and room survive results, readiness resets");
  h.ready(a);h.ready(b);h.configure(a,3,1,true,5);check(r.ready.isEmpty(),"Rule changes invalidate all ready checks");
  h.ready(b);h.presence(Set.of(a,c,d,e,f),now+100);check(!r.ready.contains(b),"Disconnect removes readiness");
  h.presence(Set.of(a,c,d,e,f),now+120_101);check(h.room(a)==null&&h.room(b)==null&&h.party(a)==h.party(b),"Expired reconnect removes group from room together, preserves party");
  h.presence(Set.of(a,b,c,d,e,f),now+130_000);h.invite(a,e,now+130_000);h.accept(e,now+190_000);check(h.party(e)==null,"Exact invitation expiry rejected");
  h.invite(a,e,now+200_000);h.accept(e,now+200_001);h.invite(a,f,now+200_002);h.accept(f,now+200_003);check(h.party(a).members.size()==4,"Party reaches four members");
  UUID extra=id();h.online.add(extra);h.invite(a,extra,now+200_004);check(!h.invites.containsKey(extra),"Party cap enforced before invitation");
  h.transfer(a,b);check(h.party(a).leader.equals(b),"Leader transfer");h.leaveParty(a);check(h.party(a)==null&&h.party(b).members.size()==3,"Leave preserves other members");
  h.restore();check(h.party(b).members.size()==3&&h.rooms.isEmpty(),"Restore preserves persistent groups, not temporary rooms");
  SocialHub recovery=new SocialHub();recovery.presence(Set.of(a,b),now);recovery.createParty(a);recovery.invite(a,b,now);recovery.accept(b,now+1);recovery.createRoom(a);recovery.presence(Set.of(b),now+10);
  recovery.claim(b,now+120_009);check(recovery.party(b).leader.equals(a),"Leader reservation lasts the full two minutes");
  recovery.presence(Set.of(b),now+120_010);recovery.claim(b,now+120_010);check(recovery.party(b).leader.equals(b),"Online member recovers offline leadership after grace");
  check(recovery.room(b)==null&&recovery.party(b).members.contains(a),"Expired room does not erase offline friend membership");
  recovery.presence(Set.of(a,b),now+120_011);recovery.createRoom(b);check(recovery.room(a)==recovery.room(b),"Returning friend can enter a new room with recovered party");
  recovery.ready(a);recovery.ready(b);recovery.transfer(b,a);check(recovery.room(a).leader.equals(b),"Party leadership does not steal independent room hosting");
  String left=recovery.leaveRoom(a);check(recovery.room(b)==null&&recovery.party(b).members.size()==2,"New party leader can leave room for the group");
  check(left.equals("Your group left the room; your party is still together."),"A party leaving its room hears that the party holds");
  SocialHub solo=new SocialHub();UUID lone=id();solo.presence(Set.of(lone),now);solo.createRoom(lone);
  check(solo.room(lone)!=null&&solo.leaveRoom(lone).equals("You left the room.")&&solo.room(lone)==null&&solo.party(lone)==null,"A commander alone leaves a room with no word of a party");
  Progression.State s=new Progression.State();LocalDate day=LocalDate.of(2026,10,17);Progression.reset(s,day);
  check(Progression.claim(s,"daily:0").gold()==0,"Unfinished contract cannot claim");
  Progression.match(s,"round-1",true,true);int xp=s.xp;Progression.match(s,"round-1",true,true);check(s.xp==xp&&s.dailyBattles==1,"Duplicate match cannot grant twice");
  Progression.match(s,"short",true,false);check(s.xp==xp,"Short battle gives no progression");
  check(Progression.claimable(s,"daily:0")&&!Progression.claimable(s,"daily:1")&&!Progression.claimable(s,"daily:00"),"claimable names the finished daily and only it");
  check(Progression.claim(s,"daily:0").gold()==30,"Completed daily pays once");check(!Progression.claimable(s,"daily:0"),"A claimed daily is no longer claimable");check(Progression.claim(s,"daily:0").gold()==0,"Repeated claim blocked");
  check(Progression.claim(s,"daily:00").gold()==0&&Progression.claim(s,"daily:+0").gold()==0,"Alternate numeric spellings cannot bypass claim ledger");
  Progression.reset(s,day.minusDays(1));check(s.claims.contains("daily:0"),"Clock rollback does not reopen claims");
  Progression.reset(s,day.plusDays(1));check(s.dailyBattles==0&&!s.claims.contains("daily:0")&&s.weeklyBattles==1,"Daily reset retains weekly progress");
  s.weeklyBattles=Progression.WEEKLY_BATTLES-1;check(!Progression.claimable(s,"weekly")&&Progression.claim(s,"weekly").gold()==0,"The weekly needs "+Progression.WEEKLY_BATTLES+" battles");
  s.weeklyBattles=Progression.WEEKLY_BATTLES;check(Progression.claimable(s,"weekly")&&Progression.claim(s,"weekly").gold()==150&&!Progression.claimable(s,"weekly"),"The weekly pays once at "+Progression.WEEKLY_BATTLES+" battles");
  Progression.reroll(s);check(s.rerolled&&Progression.target(s,2)==10,"Free reroll substitutes achievable orders quest");check(Progression.reroll(s).contains("already"),"Only one free reroll");
  s.xp=7500;check(Progression.tier(s)==30,"Chapter has a defined tier-30 endpoint");int gold=0;for(int i=1;i<=30;i++)gold+=Progression.claim(s,"founders:"+i).gold();
  check(gold==300&&s.titles.size()==6&&s.titles.contains("Bannerlord")&&!s.titles.contains("Vanguard"),"Complete track pays 300 gold and five distinct titles (the re-authored Founders chapter; its shard tiers and Rare wait for a House or a troop)");
  check(Progression.claim(s,"founders:01").gold()==0,"Alternate tier spelling rejected");
  Progression.reset(s,day.plusDays(20));check(s.claims.contains("founders:30")&&s.titles.contains("Bannerlord"),"Chapter unlocks survive daily/weekly reset");
  check(Progression.claim(s,"founders:31").gold()==0,"Unreleased tiers cannot claim");
  Progression.State said=new Progression.State();Progression.reset(said,day);said.xp=5*Progression.XP_PER_TIER;said.weeklyBattles=Progression.WEEKLY_BATTLES;
  check(Progression.claim(said,"founders:1").message().equals("Claimed reward: +25 gold")&&Progression.claim(said,"founders:5").message().equals("Claimed reward: Scout title unlocked")&&Progression.claim(said,"weekly").message().equals("Claimed reward: +150 gold / +300 XP"),"A claim's notice names only what it grants: no +0 XP on a tier, no +0 gold on a title (UI Task 8b)");
  // the room gate's truth table, row by row {in a party, leads it, may make or join a room}: the same rows UiMathCheck pins for the
  // client's copy (UiMath.roomOpen, which dulls the Friends page's room tags), so a change to either side fails its own check (8b review Minor 5)
  boolean[][] roomRows={{false,false,true},{true,true,true},{true,false,false}};
  SocialHub gate=new SocialHub();UUID alone=id(),head=id(),member=id();gate.presence(Set.of(alone,head,member),now);gate.createParty(head);gate.invite(head,member,now);gate.accept(member,now+1);
  UUID[] who={alone,head,member};boolean rowsOk=true;
  for(int i=0;i<roomRows.length;i++){var p=gate.party(who[i]);rowsOk&=(p!=null)==roomRows[i][0]&&(p!=null&&who[i].equals(p.leader))==roomRows[i][1]&&gate.lead(who[i])==roomRows[i][2];}
  check(rowsOk&&gate.createRoom(member).startsWith("Only your party leader"),"The server's room gate gives the client's rows: alone yes, leading yes, a member who does not lead no");
  System.out.println("PASS "+count+" social/progression scenarios");
 }
}
