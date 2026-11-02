package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure battle-mode arithmetic and copy (Phase 4 Task 1): wave counts, rule and size lines, how-to cells, siege strength,
 * Convoy roles, road and push rules, and ledger text. No Minecraft types, so it is checked by javac alone
 * (test-tools/standalone/ModeMathCheck.java) rather than by a client run.
 */
public final class ModeMath {
    private ModeMath() {}
    /** Troops the n-th wave rises: the base count plus perBanner for every banner past the first, scaled by the tier, rounded up. */
    public static int waveCount(int base,int banners,double perBanner,double tierScale) {
        return (int)Math.ceil(base*(1+perBanner*(Math.max(1,banners)-1))*tierScale-1e-9);
    }
    /** A rule line from its sheet template: {n} becomes n. */
    public static String ruleLine(String template,int n) {
        return template.replace("{n}",Integer.toString(n));
    }
    /** A how-to cell: lines split on |, trimmed, empty parts dropped, at most two. */
    public static List<String> howTo(String cell) {
        List<String> out=new ArrayList<>();
        for(String part:cell.split("\\|")){
            String line=part.trim();
            if(!line.isEmpty())out.add(line);
            if(out.size()==2)break;
        }
        return out;
    }
    /** The field card's size line: the side's size and tier, and for Siege the side it defends (Survival counts banners, not a v b). */
    public static String sizeLine(String battleMode,int size,String tierName,String side) {
        return switch(battleMode){
            case "survival" -> size+(size==1?" banner, ":" banners, ")+tierName+" waves";
            case "siege" -> size+" v "+size+", "+side+", "+tierName+(size==1?" bot":" bots");
            default -> size+" v "+size+", "+tierName+(size==1?" bot":" bots");
        };
    }
    /** The defenders' strength in percent: yard and keep show 100 less capture progress, the gate its health share, a fallen keep 0. */
    public static int siegeStrength(int stage,double progress,double hp,double maxHp) {
        return switch(stage){
            case 0,2 -> (int)Math.round(100*(1-progress));
            case 1 -> maxHp>0?(int)Math.round(100*hp/maxHp):0;
            default -> 0;
        };
    }
    /**
     * The Convoy role of each banner of one side, humans first (spec 6). Escort: pushers = banners less round(banners*raidShare),
     * at least one, the rest screen. Defence: raiders = round(banners*raidShare), at least one, the rest block. Every human banner
     * takes a pusher or raider slot first, so the slots the humans leave go to bots.
     */
    public static List<String> convoyRoles(int banners,int humans,double raidShare,boolean escort) {
        List<String> out=new ArrayList<>();
        if(banners<=0)return out;
        int share=(int)Math.round(banners*raidShare);
        int primary=Math.min(banners,Math.max(Math.max(1,escort?banners-share:share),humans));
        for(int i=0;i<banners;i++)out.add(i<primary?(escort?"pusher":"raider"):(escort?"screen":"blocker"));
        return out;
    }
    /** A ledger number in its mode's format: 1/3, 64%, 1:05 (seconds as minutes and seconds), or 12. */
    public static String ledgerText(String format,int value,int max) {
        return switch(format){
            case "fraction" -> value+"/"+max;
            case "percent" -> value+"%";
            case "time" -> Math.max(0,value)/60+":"+(Math.max(0,value)%60<10?"0":"")+Math.max(0,value)%60;
            default -> Integer.toString(value);
        };
    }

    // ---- Survival, the waves at the shrine (Phase 4 spec 6) ----
    /** The rise warning's feed line: "The dead rise at the chapel", "... at the chapel and the ford", "... at a, b and c". */
    public static String riseLine(List<String> sites) {
        if(sites.isEmpty())return "The dead rise";
        if(sites.size()==1)return "The dead rise at "+sites.get(0);
        return "The dead rise at "+String.join(", ",sites.subList(0,sites.size()-1))+" and "+sites.get(sites.size()-1);
    }
    /** A cleared wave's feed line: "Wave 3 laid to rest, +80 gold", or without gold "Wave 7 laid to rest". */
    public static String restLine(int wave,int gold) {
        return "Wave "+wave+" laid to rest"+(gold>0?", +"+gold+" gold":"");
    }
    /** The feed line as the dead begin to take the shrine. */
    public static final String SHRINE_LINE="The dead are taking the shrine";
    /**
     * The round's result after a second of Survival: 1 (the dead win) once they held the shrine lossSeconds with no defender on
     * it; else 0 (the defenders win) when the last wave (wave = waves, counted from 1) is cleared or its timer is out; else -1, play on.
     */
    public static int survivalResult(int wave,int waves,boolean cleared,boolean timerOut,int lostSeconds,int lossSeconds) {
        if(lostSeconds>=lossSeconds)return 1;
        if(wave>=waves&&(cleared||timerOut))return 0;
        return -1;
    }
    /** One second of the shrine: the dead on it with no defender add a second; a defender on it, or no dead, sets it back to 0 (the hold must be unbroken). */
    public static int shrineLost(int lostSeconds,int dead,int defenders) {
        return dead>0&&defenders==0?lostSeconds+1:0;
    }

    // ---- Convoy, the supply-wagon escort (spec 2026-10-31-convoy-escort-design.md, sections 3 and 6) ----
    /** The wagon's four states: pushed forward, stopped by an enemy, left alone, rolling back. */
    public static final String MOVING="moving",CONTESTED="contested",IDLE="idle",ROLLBACK="rollback";
    /** The wagon's state this tick: any defender in push range stops it whatever the pushers; a pusher moves it; left alone it idles, then rolls back once idleTicks reaches idleLimit. */
    public static String convoyState(int pushers,int defenders,int idleTicks,int idleLimit) {
        if(defenders>0)return CONTESTED;
        if(pushers>0)return MOVING;
        return idleTicks>=idleLimit?ROLLBACK:IDLE;
    }
    /** Blocks a second for this many pushers: speeds[n-1], capped at the last entry; no pusher, no speed. */
    public static double pushSpeed(int pushers,double[] speeds) {
        if(pushers<=0||speeds.length==0)return 0;
        return speeds[Math.min(pushers,speeds.length)-1];
    }
    /** The distance along the road (horizontal, x and z) at each node: 0 at the start, the road's length at the last. */
    public static double[] stations(double[][] nodes) {
        double[] out=new double[nodes.length];
        for(int i=1;i<nodes.length;i++)out[i]=out[i-1]+Math.hypot(nodes[i][0]-nodes[i-1][0],nodes[i][1]-nodes[i-1][1]);
        return out;
    }
    /** The segment that holds distance d: the i with stations[i] <= d < stations[i+1], clamped to the road's segments. */
    public static int segment(double[] stations,double d) {
        int i=0;
        while(i<stations.length-2&&d>=stations[i+1])i++;
        return i;
    }
    /** The point at distance d along the road, {x, z}, d clamped to the road. */
    public static double[] along(double[][] nodes,double[] stations,double d) {
        if(nodes.length==1)return new double[]{nodes[0][0],nodes[0][1]};
        double c=Math.max(0,Math.min(stations[stations.length-1],d));
        int i=segment(stations,c);
        double len=stations[i+1]-stations[i],t=len<=0?0:(c-stations[i])/len;
        return new double[]{nodes[i][0]+(nodes[i+1][0]-nodes[i][0])*t,nodes[i][1]+(nodes[i+1][1]-nodes[i][1])*t};
    }
    /** The forward speed factor on a segment that rises rise over run: up beyond maxGrade, down beyond -maxGrade, 1 between. */
    public static double gradeFactor(double rise,double run,double maxGrade,double up,double down) {
        if(run<=0)return 1;
        double g=rise/run;
        return g>maxGrade?up:g<-maxGrade?down:1;
    }
    /** The wagon's new distance after seconds in a state: forward at speed up to the road's end, back at back down to floor (the last checkpoint passed), else unchanged. */
    public static double advance(double d,String state,double speed,double back,double seconds,double floor,double total) {
        return switch(state){
            case MOVING -> Math.min(total,d+speed*seconds);
            case ROLLBACK -> Math.max(floor,d-back*seconds);
            default -> d;
        };
    }
    /** How many of the checkpoints (their road distances, in order) lie at or behind d. */
    public static int passed(double[] checkpoints,double d) {
        int n=0;
        for(double at:checkpoints)if(at<=d+1e-9)n++;
        return n;
    }
    /** Road progress in whole percent, 0 to 100. */
    public static int percent(double d,double total) {
        return total<=0?0:(int)Math.max(0,Math.min(100,Math.floor(100*d/total+1e-9)));
    }
    /** Whether the clock waits: the wagon is moving or contested within radius of the next stop, or was so less than graceTicks ago. */
    public static boolean overtime(String state,double d,double next,double radius,long ticksSinceNear,int graceTicks) {
        boolean near=(state.equals(MOVING)||state.equals(CONTESTED))&&next-d<=radius;
        return near||ticksSinceNear<graceTicks;
    }
}
