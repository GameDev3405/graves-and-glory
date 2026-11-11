package gg.gravesandglory;

import java.time.*;
import java.util.*;

/** Deterministic chapter progression; all mutations run on the server. */
final class Progression {
    static final String SEASON="founders";
    static final int TIERS=30, XP_PER_TIER=250;
    /** The chapter's titles in tier order (houses design 4.8: the old six minus Vanguard), at TITLE_TIERS. A profile that earned Vanguard before keeps it. */
    static final String[] TITLES={"Scout","Pathfinder","Captain","Banneret","Bannerlord"};
    static final int[] TITLE_TIERS={5,10,15,25,30};
    /** The re-authored Founders chapter (houses design 4.8): ten tiers of gold, twelve of shards to a chosen House, five titles, one chosen Rare and two banner cosmetics; 300 gold in all. */
    static final Set<Integer> GOLD_TIERS=Set.of(1,3,7,9,11,13,17,19,23,27),SHARD_TIERS=Set.of(2,4,6,8,12,14,16,18,22,24,26,28);
    static final int RARE_TIER=20,TIER_GOLD=30,TIER_SHARDS=10;
    static final Map<Integer,String> COSMETIC_TIERS=Map.of(21,"pennant_pattern",29,"banner_frame");
    /** Contract shards (houses design 4.5): the daily claim that completes all three, and the weekly, each to a House named in the claim. */
    static final int DAILY_SHARDS=3,WEEKLY_SHARDS=10;
    static final class State {
        /** The JSON key of matchGrants. Campaign.forClient strips it from every client copy and p1-report-snapshot checks that it names a real key, so a rename of the field fails a check instead of sending match ids again. */
        static final String MATCH_GRANTS="matchGrants";
        int schema=1,xp,totalBattles,totalWins,weeklyBattles;
        String day="",week="",title="Recruit";boolean rerolled;
        int dailyBattles,dailyCaptures,dailyOrders;
        Set<String> claims=new HashSet<>(),titles=new LinkedHashSet<>(Set.of("Recruit")),matchGrants=new LinkedHashSet<>();
        /** Titles renamed on 2026-11-11 (Kort: Veteran names nothing): the chapter title Veteran is Banneret, a Mastery title "X Veteran" is "X Storied". Saved titles follow. */
        static String renamed(String t){return t==null?null:t.equals("Veteran")?"Banneret":t.endsWith(" Veteran")?t.substring(0,t.length()-8)+" Storied":t;}
        void renamed(){var out=new LinkedHashSet<String>();for(String t:titles)out.add(renamed(t));titles=out;title=renamed(title);}
        void sanitize(){if(claims==null)claims=new HashSet<>();if(titles==null)titles=new LinkedHashSet<>();renamed();titles.add("Recruit");if(!titles.contains(title))title="Recruit";
            if(matchGrants==null)matchGrants=new LinkedHashSet<>();xp=Math.max(0,Math.min(1_000_000,xp));}
    }
    /** What a claim pays. Gold and XP as before; shards go to house; troop is a troop id to add to the collection (a new troop or a duplicate copy) and cosmetic a banner cosmetic id, each null when the claim gives none. Titles go straight into the state. */
    record Grant(int gold,int xp,int shards,String house,String troop,String cosmetic,String message) {Grant(int gold,int xp,String message){this(gold,xp,0,null,null,null,message);}}
    static void reset(State s,LocalDate date){s.sanitize();String day=date.toString(),week=date.minusDays(date.getDayOfWeek().getValue()-1).toString();
        if(s.day==null||day.compareTo(s.day)>0){s.day=day;s.dailyBattles=0;s.dailyCaptures=0;s.dailyOrders=0;s.rerolled=false;s.claims.removeIf(k->k.startsWith("daily:"));}
        if(s.week==null||week.compareTo(s.week)>0){s.week=week;s.weeklyBattles=0;s.claims.remove("weekly");}}
    static int tier(State s){return Math.min(TIERS,s.xp/XP_PER_TIER);}
    static String contract(State s,int slot){return switch(slot){case 0->"Complete 1 battle";case 1->"Complete 3 battles";default->s.rerolled?"Issue 10 tactical orders":"Help capture 1 objective";};}
    static int progress(State s,int slot){return slot==0||slot==1?s.dailyBattles:s.rerolled?s.dailyOrders:s.dailyCaptures;}
    static int target(State s,int slot){return slot==0?1:slot==1?3:s.rerolled?10:1;}
    static Grant match(State s,String id,boolean won,boolean eligible){return match(s,id,won,eligible,100);}
    /** As match, with the XP pro-rated by the percent of the match the player was seated (MatchRules.sharePercent), rounded down; the battle counts are whole. */
    static Grant match(State s,String id,boolean won,boolean eligible,int percent){
        if(!eligible||!s.matchGrants.add(id))return new Grant(0,0,"No duplicate or ineligible match XP.");
        while(s.matchGrants.size()>256)s.matchGrants.remove(s.matchGrants.iterator().next());
        s.totalBattles++;s.dailyBattles++;s.weeklyBattles++;if(won)s.totalWins++;
        int xp=(won?150:100)*Math.max(0,Math.min(100,percent))/100;s.xp=Math.min(1_000_000,s.xp+xp);return new Grant(0,xp,"Battle progress saved.");}
    /** Battles in a week for the weekly contract. */
    static final int WEEKLY_BATTLES=6;
    /** Why key cannot be claimed now ("" when it can): the War Table's Contracts badge and the Contracts page read it. It follows claim's rules
     *  (integration: Phase 2's Houses rules), ignoring the House or troop suffix, which claim itself asks for when it pays shards or a Rare. */
    static String refusal(State s,String key){
        String[] parts=key.split(":",-1);
        if(parts.length<1||parts.length>3)return "Unknown reward.";
        if(parts[0].equals("weekly")&&parts.length<=2)return s.claims.contains("weekly")?"Already claimed.":s.weeklyBattles<WEEKLY_BATTLES?"Complete "+WEEKLY_BATTLES+" battles this week.":"";
        if(parts.length<2)return "Unknown reward.";
        if(parts[0].equals("daily")){int slot=parse(parts[1]);if(!parts[1].equals(Integer.toString(slot))||slot<0||slot>2)return "Contract is not complete.";
            return s.claims.contains("daily:"+slot)?"Already claimed.":progress(s,slot)<target(s,slot)?"Contract is not complete.":"";}
        if(parts[0].equals(SEASON)){int level=parse(parts[1]);if(!parts[1].equals(Integer.toString(level))||level<1||level>TIERS)return "That chapter tier is locked.";
            return s.claims.contains(SEASON+":"+level)?"Already claimed.":tier(s)<level?"That chapter tier is locked.":"";}
        return "Unknown reward.";
    }
    static boolean claimable(State s,String key){return refusal(s,key).isEmpty();}
    /**
     * Claims a reward by key: daily:<slot>[:<house>], weekly[:<house>] or founders:<tier>[:<house or troop>]. The key is stored without its suffix, so a
     * claim made before Houses (founders:2, daily:0, weekly) stays claimed. A claim that pays shards needs a House (the daily that completes all three,
     * the weekly, a Founders shard tier) and Founders tier 20 needs a Rare; without one nothing is paid or stored.
     */
    static Grant claim(State s,String key){
        String[] parts=key.split(":",-1);
        if(parts.length<1||parts.length>3)return new Grant(0,0,"Unknown reward.");
        String kind=parts[0],arg=parts.length==3?parts[2]:null;
        if(kind.equals("weekly")&&parts.length<=2){
            String house=parts.length==2?parts[1]:null;
            if(s.claims.contains("weekly"))return new Grant(0,0,"Already claimed.");
            if(s.weeklyBattles<6)return new Grant(0,0,"Complete 6 battles this week.");
            if(!house(house))return new Grant(0,0,"Choose a House for the weekly's "+WEEKLY_SHARDS+" shards.");
            return pay(s,"weekly",150,300,WEEKLY_SHARDS,house,null,null);
        }
        if(parts.length<2)return new Grant(0,0,"Unknown reward.");
        if(kind.equals("daily")){
            int slot=parse(parts[1]);String stored="daily:"+slot;
            if(!parts[1].equals(Integer.toString(slot))||slot<0||slot>2)return new Grant(0,0,"Contract is not complete.");
            if(s.claims.contains(stored))return new Grant(0,0,"Already claimed.");
            if(progress(s,slot)<target(s,slot))return new Grant(0,0,"Contract is not complete.");
            boolean last=true;for(int i=0;i<3;i++)if(i!=slot&&!s.claims.contains("daily:"+i))last=false;
            if(last&&!house(arg))return new Grant(0,0,"Choose a House for the "+DAILY_SHARDS+" shards of the last daily.");
            return pay(s,stored,slot==1?40:30,75,last?DAILY_SHARDS:0,last?arg:null,null,null);
        }
        if(kind.equals(SEASON)){
            int level=parse(parts[1]);String stored=SEASON+":"+level;
            if(!parts[1].equals(Integer.toString(level))||level<1||level>TIERS)return new Grant(0,0,"That chapter tier is locked.");
            if(s.claims.contains(stored))return new Grant(0,0,"Already claimed.");
            if(tier(s)<level)return new Grant(0,0,"That chapter tier is locked.");
            if(GOLD_TIERS.contains(level))return pay(s,stored,TIER_GOLD,0,0,null,null,null);
            if(SHARD_TIERS.contains(level))return house(arg)?pay(s,stored,0,0,TIER_SHARDS,arg,null,null):new Grant(0,0,"Choose a House for tier "+level+"'s "+TIER_SHARDS+" shards.");
            if(level==RARE_TIER){
                var t=arg==null?null:troop(arg);
                if(t==null||!t.rarity().equals("rare"))return new Grant(0,0,t==null?"Choose a Rare troop for tier "+RARE_TIER+".":"Tier "+RARE_TIER+" gives a Rare troop; "+t.name()+" is not a Rare.");
                return pay(s,stored,0,0,0,null,t.id(),null);
            }
            if(COSMETIC_TIERS.containsKey(level))return pay(s,stored,0,0,0,null,null,COSMETIC_TIERS.get(level));
            for(int i=0;i<TITLE_TIERS.length;i++)if(TITLE_TIERS[i]==level){s.titles.add(TITLES[i]);return pay(s,stored,0,0,0,null,null,null);}
            return new Grant(0,0,"That chapter tier is locked.");
        }
        return new Grant(0,0,"Unknown reward.");
    }
    /** Stores the claim, adds the XP, and says what was paid. */
    static Grant pay(State s,String stored,int gold,int xp,int shards,String house,String troop,String cosmetic){
        s.claims.add(stored);s.xp=Math.min(1_000_000,s.xp+xp);
        List<String> parts=new ArrayList<>();
        if(gold>0)parts.add("+"+gold+" gold");if(xp>0)parts.add("+"+xp+" XP");if(shards>0)parts.add("+"+shards+" "+RosterMath.houseName(house)+" shards");
        if(troop!=null)parts.add(troop(troop).name());if(cosmetic!=null)parts.add("the "+cosmetic.replace('_',' ')+" cosmetic");
        for(int i=0;i<TITLE_TIERS.length;i++)if(stored.equals(SEASON+":"+TITLE_TIERS[i]))parts.add("the title "+TITLES[i]);
        return new Grant(gold,xp,shards,shards>0?house:null,troop,cosmetic,"Claimed reward: "+String.join(" / ",parts));
    }
    /** What a Founders tier gives, for the chapter page. */
    static String reward(int tier){
        if(GOLD_TIERS.contains(tier))return TIER_GOLD+" gold";
        if(SHARD_TIERS.contains(tier))return TIER_SHARDS+" House shards";
        if(tier==RARE_TIER)return "a Rare troop";
        if(COSMETIC_TIERS.containsKey(tier))return cap(COSMETIC_TIERS.get(tier).replace('_',' '));
        for(int i=0;i<TITLE_TIERS.length;i++)if(TITLE_TIERS[i]==tier)return TITLES[i]+" title";
        return "";
    }
    static String cap(String s){return s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1);}
    static boolean house(String id){if(id==null)return false;for(var h:Spec.HOUSES)if(h.id().equals(id))return true;return false;}
    static Spec.TroopsRow troop(String id){for(var t:Spec.TROOPS)if(t.id().equals(id))return t;return null;}
    static int parse(String value){try{return Integer.parseInt(value);}catch(NumberFormatException e){return -1;}}
    static String reroll(State s){if(s.rerolled)return "Your free daily reroll is already used.";if(s.claims.contains("daily:2"))return "That contract is already claimed.";s.rerolled=true;return "Third contract changed to tactical orders. One free reroll per day.";}
    static String chapter(State s,int unlocked,int total){if(s.totalBattles==0)return "1 / Learn command on "+(Spec.CAMPAIGNCHAPTERS.length==0?"the campaign":Spec.CAMPAIGNCHAPTERS[0].name());if(unlocked<total)return "2 / Recruit the launch roster";if(tier(s)<TIERS)return "3 / Complete the Founders season pass";return "Season pass complete / master your army and await the next season";}
    static LocalDate today(){return LocalDate.now(ZoneOffset.UTC);}
}
