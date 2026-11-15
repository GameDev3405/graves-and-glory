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
    /** The map a Siege round's rows come from: this map when it has a gate row (modeSites siege, kind gate, stage 1), else Ironhold's (map 1), as SiegeMode.Sites does. */
    public static int siegeMap(int map) {
        for(var s:Spec.MODESITES)if(s.mode().equals("siege")&&s.map()==map&&s.kind().equals("gate")&&s.stage()==1)return map;
        return 1;
    }
    /** The defending team of a Siege map (0 Dawn, 1 Dusk): on an engine map the team of the base row nearest the keep (its keep muster; a base row's stage is its team), else Dusk; the old Ironhold (its gate landmark piece) always Dusk. */
    public static int siegeDefenders(int map) {
        int m=siegeMap(map);
        for(var r:Spec.LANDMARKS)if(r.map()==m&&r.template().equals("ironhold/gate"))return 1;
        Spec.ModeSitesRow keep=null,near=null;
        for(var s:Spec.MODESITES)if(s.mode().equals("siege")&&s.map()==m&&s.kind().equals("stage")&&s.stage()==2)keep=s;
        if(keep==null)return 1;
        for(var r:Spec.MODESITES)if(r.mode().equals("siege")&&r.map()==m&&r.kind().equals("base")&&(near==null||Math.hypot(r.x()-keep.x(),r.z()-keep.z())<Math.hypot(near.x()-keep.x(),near.z()-keep.z())))near=r;
        return near!=null&&near.stage()==0?0:1;
    }
    /** The attacking team of a Siege map: the other one. Dawn attacks on Ironhold, Kingsfield and Frostmarch, Dusk on Rivergate and Hollowwood. */
    public static int siegeAttackers(int map) {
        return 1-siegeDefenders(map);
    }
    /**
     * The names of a Siege map's yard, gate and keep (modeSites siege rows, stages 0, 1, 2) as its feed lines write them. The old Ironhold's three names, and any
     * missing row or blank or "-" name, give the old copy's words: "the Outer Yard", "The Iron Gate" (it starts its line) and "the Keep", so those lines read as
     * they always did; another map's names stand as the rows give them, like a Domination point's.
     */
    public static String[] siegeSiteNames(int map) {
        int m=siegeMap(map);
        String[] out={"Outer Yard","Iron Gate","Keep"};
        String[] kinds={"stage","gate","stage"};int[] stages={0,1,2};
        for(int i=0;i<3;i++)for(var s:Spec.MODESITES)
            if(s.mode().equals("siege")&&s.map()==m&&s.kind().equals(kinds[i])&&s.stage()==stages[i]&&!s.name().isBlank()&&!s.name().equals("-"))out[i]=s.name();
        if(out[0].equals("Outer Yard"))out[0]="the Outer Yard";
        if(out[1].equals("Iron Gate"))out[1]="The Iron Gate";
        if(out[2].equals("Keep"))out[2]="the Keep";
        return out;
    }
    /** The feed event that says the gate is breached: "gate_breached" ("The Iron Gate is breached") while the map has no gate name of its own, "gate_breached_named" ("North Gatehouse breached", short enough for the feed row) where it has. */
    public static String siegeBreachEvent(int map) {
        return siegeSiteNames(map)[1].equals("The Iron Gate")?"gate_breached":"gate_breached_named";
    }
    /** The round intro's two Siege lines for a viewer on a team (-1: none), in capitals as the intro writes them: who attacks and who defends, by banner colour ("Gold", "Violet"), then the viewer's own part; the second is empty for a viewer without a team. */
    public static String[] siegeIntro(int map,int viewerTeam,String attackers,String defenders) {
        int a=siegeAttackers(map);
        String you=viewerTeam!=0&&viewerTeam!=1?"":viewerTeam==a?"You attack":"You defend";
        return new String[]{("Siege: "+attackers+" attacks, "+defenders+" defends").toUpperCase(java.util.Locale.ROOT),you.toUpperCase(java.util.Locale.ROOT)};
    }
    /** The most letters the two banner names can have together for the one-line sides row ("Gold attacks, Violet defends": 140 of the feed row's 144 px); a longer pair ("Crimson", "Violet") goes out as two short rows. FeedFitCheck measures both forms for every pair of banners. */
    public static final int SIDES_LINE_LETTERS=10;
    /** The modeLines siege events that announce the sides: "sides" alone, or "sides_attack" then "sides_defend" when the names are too long for one row. */
    public static String[] siegeSidesEvents(String attackers,String defenders) {
        return attackers.length()+defenders.length()<=SIDES_LINE_LETTERS?new String[]{"sides"}:new String[]{"sides_attack","sides_defend"};
    }
    /** The lines of Siege's round-start announcement for the two banner names, as the feed shows them ("Violet attacks, Gold defends"). */
    public static java.util.List<String> siegeSides(String attackers,String defenders) {
        java.util.List<String> out=new java.util.ArrayList<>();
        for(String e:siegeSidesEvents(attackers,defenders))out.add(FeedLines.modeLine("siege",e,"team",attackers,"defenders",defenders));
        return out;
    }
    /** A pair given as the attackers' then the defenders' put by team (index 0 Dawn, 1 Dusk), the other way round when Dusk attacks: how Siege's two ledger numbers (and their maxima, formats and graph lines) reach the HUD bar, the war map and the report graph, which read slot 0 as Dawn. attackers is the map's siegeAttackers. */
    public static <T> java.util.List<T> byTeam(int attackers,T attackerValue,T defenderValue) {
        return attackers==0?java.util.List.of(attackerValue,defenderValue):java.util.List.of(defenderValue,attackerValue);
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
    /** The rise warning's feed lines, one per site so each fits the 144 px Compact row: "Dead rise: the chapel"; "The dead rise" with no site. */
    public static List<String> riseLines(List<String> sites) {
        if(sites.isEmpty())return List.of(FeedLines.modeLine("survival","rise_unknown"));
        var out=new ArrayList<String>();
        for(var s:sites)out.add(FeedLines.modeLine("survival","rise","site",s));
        return out;
    }
    /** A cleared wave's feed line: "Wave 3 at rest, +80 gold", or without gold "Wave 7 laid to rest". The copy is the modeLines survival rows (rest, rest_plain). */
    public static String restLine(int wave,int gold) {
        return gold>0?FeedLines.modeLine("survival","rest","n",Integer.toString(wave),"gold",Integer.toString(gold)):FeedLines.modeLine("survival","rest_plain","n",Integer.toString(wave));
    }
    /** The feed line as the dead begin to take the shrine (modeLines survival shrine_falling). */
    public static String shrineLine(){return FeedLines.modeLine("survival","shrine_falling");}
    /** The round's closing feed line: the defenders hold to dawn, or the dead hold the shrine (modeLines survival stands, dead_hold). */
    public static String closingLine(boolean defendersWon){return FeedLines.modeLine("survival",defendersWon?"stands":"dead_hold");}
    /**
     * The round's result after a second of Survival: 1 (the dead win) once they held the shrine lossSeconds with no defender on
     * it; else 0 (the defenders win) when the last wave (wave = waves, counted from 1) is cleared or its timer is out; else -1, play on.
     */
    public static int survivalResult(int wave,int waves,boolean cleared,boolean timerOut,int lostSeconds,int lossSeconds) {
        if(lostSeconds>=lossSeconds)return 1;
        if(wave>=waves&&(cleared||timerOut))return 0;
        return -1;
    }
    /**
     * Domination's winner on a score tick: -2 while neither side is at the target; the side at it; both at it on the same tick, the higher
     * score (C05-F2), and -1, a draw as at a timeout, on equal scores (Kort 2026-11-12).
     */
    public static int dominationWinner(int score0,int score1,int target) {
        if(score0>=target&&score1>=target)return score0==score1?-1:score1>score0?1:0;
        if(score0>=target)return 0;
        if(score1>=target)return 1;
        return -2;
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
    /** As above, but a wagon at its floor (the start or its last checkpoint) cannot roll back, so it shows idle (C05-F4). */
    public static String convoyState(int pushers,int defenders,int idleTicks,int idleLimit,boolean atFloor) {
        String state=convoyState(pushers,defenders,idleTicks,idleLimit);
        return atFloor&&state.equals(ROLLBACK)?IDLE:state;
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
    /** The floor block of a column nearest a height, or Integer.MIN_VALUE when the column has no floor (roadFloors). */
    public interface FloorNear {int at(int x,int z,int y);}
    /** The floor block under the road every step along it, start to end (the last sample is the road's end): each sample takes the floor of its
     *  column nearest the sample before it, so the road keeps to its own level under a roof, a bridge or an arch and climbs a ramp a block at a
     *  time. A node with a height of its own (nodeFloor, Integer.MIN_VALUE for none) sets the level from that node on; a column with no floor
     *  keeps the level. */
    public static int[] roadFloors(double[][] nodes,double[] stations,int[] nodeFloor,double step,int startFloor,FloorNear floors) {
        double total=stations[stations.length-1];
        int n=(int)Math.ceil(total/step)+1,last=-1,ref=startFloor;
        int[] out=new int[n];
        for(int k=0;k<n;k++){
            double d=Math.min(total,k*step);
            int seg=k==n-1?stations.length-1:segment(stations,d);
            for(int i=last+1;i<=seg;i++)if(nodeFloor[i]!=Integer.MIN_VALUE)ref=nodeFloor[i];
            last=Math.max(last,seg);
            double[] p=along(nodes,stations,d);
            int y=floors.at((int)Math.floor(p[0]),(int)Math.floor(p[1]),ref);
            if(y!=Integer.MIN_VALUE)ref=y;
            out[k]=ref;
        }
        return out;
    }
    /** The road floor at distance d from roadFloors' samples; past the last whole step it is the end sample (which sits at the road's end, not on the step grid). */
    public static int roadFloor(int[] floors,double step,double d) {
        if(d>(floors.length-2)*step+1e-9)return floors[floors.length-1];
        return floors[Math.max(0,(int)Math.round(d/step))];
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
