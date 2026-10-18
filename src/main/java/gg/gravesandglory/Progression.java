package gg.gravesandglory;

import java.time.*;
import java.util.*;

/** Deterministic chapter progression; all mutations run on the server. */
final class Progression {
    static final String SEASON="founders";
    static final int TIERS=30, XP_PER_TIER=250;
    static final String[] TITLES={"Scout","Pathfinder","Captain","Vanguard","Veteran","Bannerlord"};
    static final class State {
        int schema=1,xp,totalBattles,totalWins,weeklyBattles;
        String day="",week="",title="Recruit";boolean rerolled;
        int dailyBattles,dailyCaptures,dailyOrders;
        Set<String> claims=new HashSet<>(),titles=new LinkedHashSet<>(Set.of("Recruit")),matchGrants=new LinkedHashSet<>();
        void sanitize(){if(claims==null)claims=new HashSet<>();if(titles==null)titles=new LinkedHashSet<>();titles.add("Recruit");if(!titles.contains(title))title="Recruit";
            if(matchGrants==null)matchGrants=new LinkedHashSet<>();xp=Math.max(0,Math.min(1_000_000,xp));}
    }
    record Grant(int gold,int xp,String message) {}
    static void reset(State s,LocalDate date){s.sanitize();String day=date.toString(),week=date.minusDays(date.getDayOfWeek().getValue()-1).toString();
        if(s.day==null||day.compareTo(s.day)>0){s.day=day;s.dailyBattles=0;s.dailyCaptures=0;s.dailyOrders=0;s.rerolled=false;s.claims.removeIf(k->k.startsWith("daily:"));}
        if(s.week==null||week.compareTo(s.week)>0){s.week=week;s.weeklyBattles=0;s.claims.remove("weekly");}}
    static int tier(State s){return Math.min(TIERS,s.xp/XP_PER_TIER);}
    static String contract(State s,int slot){return switch(slot){case 0->"Complete 1 battle";case 1->"Complete 3 battles";default->s.rerolled?"Issue 10 tactical orders":"Help capture 1 objective";};}
    static int progress(State s,int slot){return slot==0||slot==1?s.dailyBattles:s.rerolled?s.dailyOrders:s.dailyCaptures;}
    static int target(State s,int slot){return slot==0?1:slot==1?3:s.rerolled?10:1;}
    static Grant match(State s,String id,boolean won,boolean eligible){
        if(!eligible||!s.matchGrants.add(id))return new Grant(0,0,"No duplicate or ineligible match XP.");
        while(s.matchGrants.size()>256)s.matchGrants.remove(s.matchGrants.iterator().next());
        s.totalBattles++;s.dailyBattles++;s.weeklyBattles++;if(won)s.totalWins++;
        int xp=won?150:100;s.xp=Math.min(1_000_000,s.xp+xp);return new Grant(0,xp,"Battle progress saved.");}
    static Grant claim(State s,String key){
        if(s.claims.contains(key))return new Grant(0,0,"Already claimed.");
        int gold=0,xp=0;
        if(key.startsWith("daily:")){int slot=parse(key.substring(6));if(!key.equals("daily:"+slot)||slot<0||slot>2||progress(s,slot)<target(s,slot))return new Grant(0,0,"Contract is not complete.");gold=slot==1?40:30;xp=75;}
        else if(key.equals("weekly")){if(s.weeklyBattles<6)return new Grant(0,0,"Complete 6 battles this week.");gold=150;xp=300;}
        else if(key.startsWith(SEASON+":")){int level=parse(key.substring(SEASON.length()+1));if(!key.equals(SEASON+":"+level)||level<1||level>TIERS||tier(s)<level)return new Grant(0,0,"That chapter tier is locked.");
            if(level%5==0)s.titles.add(TITLES[level/5-1]);else gold=25;}
        else return new Grant(0,0,"Unknown reward.");
        s.claims.add(key);s.xp=Math.min(1_000_000,s.xp+xp);return new Grant(gold,xp,"Claimed reward: +"+gold+" gold / +"+xp+" XP"+(key.startsWith(SEASON+":")&&gold==0?" / title unlocked":""));}
    static int parse(String value){try{return Integer.parseInt(value);}catch(NumberFormatException e){return -1;}}
    static String reroll(State s){if(s.rerolled)return "Your free daily reroll is already used.";if(s.claims.contains("daily:2"))return "That contract is already claimed.";s.rerolled=true;return "Third contract changed to tactical orders. One free reroll per day.";}
    static String chapter(State s,int unlocked,int total){if(s.totalBattles==0)return "1 / Learn command on The Long March";if(unlocked<total)return "2 / Recruit the launch roster";if(tier(s)<TIERS)return "3 / Complete the Founders chapter";return "Chapter complete / master your army and await the next chapter";}
    static LocalDate today(){return LocalDate.now(ZoneOffset.UTC);}
}
