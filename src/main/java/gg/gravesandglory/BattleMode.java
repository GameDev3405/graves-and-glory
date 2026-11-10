package gg.gravesandglory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.util.math.Vec3d;
import java.util.List;

/**
 * One battle mode's rules, owned per round (Phase 4 Task 2). The defaults below are Domination's behaviour, so
 * DominationMode overrides only what it changes and a new mode overrides only the hooks it needs. A mode keeps no state
 * of its own: the round's state lives on Battle, so a map switch or a server stop cannot leave a mode holding a stale world.
 */
public abstract class BattleMode {
    /** The mode's sheet row: its clock, target, score interval, income scales, timeout rule and rule line. */
    public final Spec.ModesRow row;
    protected BattleMode(Spec.ModesRow row){this.row=row;}
    public String id(){return row.id();}
    public String name(){return row.name();}
    /** The round's rule line, its {n} filled. */
    public String rule(){return ModeMath.ruleLine(row.ruleLine(),ruleN());}
    /** The {n} of the rule line: Domination's target unless a mode counts something else. */
    public int ruleN(){return row.winTarget();}
    /** Called after the round is prepared and before the first reinforce: a mode places what its round needs here. */
    public void setup(GravesAndGlory.Battle b){}
    /**
     * Writes the round's crash marker: whether the flags are down, and every saved piece still standing in b.saves, including
     * one an earlier teardown could not put back, so a crash can still recover it.
     */
    public void mark(GravesAndGlory.Battle b){Modes.mark(b,b.world.getTimeOfDay(),!capturesPoints(b),b.saves.stream().map(s->s.toJson().toString()).toList());}
    /**
     * Ends the round's changes: puts every saved piece back (newest first), then the mode's own undo. Called when a round ends,
     * a map changes or startRound replaces the mode. Idempotent. A piece that cannot be restored stays in b.saves, and the
     * result is false, so the caller keeps the crash marker and no later round overwrites the piece's snapshot.
     */
    public final boolean teardown(GravesAndGlory.Battle b){
        for(int i=b.saves.size()-1;i>=0;i--)if(Structures.restore(b.world,b.saves.get(i)))b.saves.remove(i);
        else System.err.println("[Graves and Glory] "+id()+" teardown kept saved piece "+b.saves.get(i).tag()+": it could not be restored.");
        undo(b);
        PropGameplay.clear(b.world); // gameplay props' damage ends with the round
        return b.saves.isEmpty();
    }
    /** The mode's own part of teardown, after the saved pieces are back: raise flags it took down, remove what it spawned. Idempotent. */
    protected void undo(GravesAndGlory.Battle b){}
    /**
     * Puts back what an interrupted round of this mode left, once Modes.recover has restored the time, the saved pieces and
     * the flags: a mode that spawned something unsaved (Convoy's wagon, Survival's wave mobs) removes it here. marker is the
     * crash marker's JSON. Runs on a fresh mode object, as the round that wrote the marker is gone.
     */
    public void recover(GravesAndGlory.Battle b,JsonObject marker){}
    /**
     * Whether the map's capture points play this round: captureTick runs, points can be taken, and held points become forward
     * anchors. Siege and Convoy take the flags down and return false, so a point can be neither captured nor deployed at.
     */
    public boolean capturesPoints(GravesAndGlory.Battle b){return true;}
    /**
     * A team's forward deploy places from the mode, before the standing Forward Supply banners are added: Domination's are
     * its held points (Battle.pointAnchors). A mode with its own sites (Siege's stage, Convoy's checkpoint) returns those.
     */
    public List<GravesAndGlory.Anchor> anchors(GravesAndGlory.Battle b,int team){return capturesPoints(b)?b.pointAnchors(team):List.of();}
    /** Whether the round plays on when its clock runs out (Convoy's overtime by a moving wagon); the timeout result waits until false. */
    public boolean overtime(GravesAndGlory.Battle b){return false;}
    /** Where a bot army goes this beat instead of its Domination point (null keeps the point plan): a mode with its own objectives (Siege's stage) returns it. */
    public Vec3d botTarget(GravesAndGlory.Battle b,GravesAndGlory.Squad s){return null;}
    /** Called at the end of Houses.recompute: a mode may replace an army's House trait tiers (Survival's director takes its tier's waveTraitTier). */
    public void traits(GravesAndGlory.Battle b,GravesAndGlory.Squad s){}
    /** Each running tick, after captureTick: the mode's scoring and its own timers. */
    public void tick(GravesAndGlory.Battle b){}
    /** The objective panel's line as the round starts; a mode without points to capture says its own. */
    public String startLine(GravesAndGlory.Battle b){return GravesAndGlory.Battle.ROUND_START;}
    /** Where a team respawns and its troops fall back to. */
    public Vec3d fieldBase(GravesAndGlory.Battle b,int team){return GravesAndGlory.Battle.base(team);}
    /** The point a team's lineup forms from (reinforce's rows stand 5 to 20 blocks forward of it): the field base unless the mode lines a team up elsewhere (Siege's defenders in the yard). */
    public Vec3d lineup(GravesAndGlory.Battle b,int team){return fieldBase(b,team);}
    /** A team's barracks access point. */
    public Vec3d fieldBarracks(GravesAndGlory.Battle b,int team){var f=Spec.FACTIONS[team];return new Vec3d(f.barracksX(),65,f.barracksZ());}
    /** Round gold a squad earns per income tick: passive gold and held points, each scaled by the mode (Survival pays neither). */
    public int income(GravesAndGlory.Battle b,GravesAndGlory.Squad s){
        var E=Spec.ECONOMY[0];
        long owned=b.points.stream().filter(p->p.owner==s.team).count();
        int passive=(int)Math.round(E.passiveGold()*row.passiveScale());
        int objective=(int)Math.round(BattleMath.pointIncome(owned,E.pointGold(),0,E.objectiveIncomeCap())*row.pointScale());
        return passive+objective;
    }
    /** Who wins when the clock runs out: -1 a draw, 0 Dawn, 1 Dusk. */
    public int timeoutWinner(GravesAndGlory.Battle b){
        return switch(row.timeout()){
            case "team0" -> 0;
            case "team1" -> 1;
            default -> b.scores[0]==b.scores[1]?-1:b.scores[0]>b.scores[1]?0:1;
        };
    }
    /** The seconds left on the round clock; the lobby shows the round's full length. */
    public int clock(GravesAndGlory.Battle b){return b.running?(int)Math.max(0,b.roundSeconds-(b.time-b.roundStarted)/20):b.roundSeconds;}
    /** The two numbers beside the HUD's tracks. */
    public int[] ledger(GravesAndGlory.Battle b){return b.scores;}
    /**
     * The 2 to 4 numbers that decide the round, as plain data for the HUD and the report (plan 2C.2), in the order of the mode's
     * modeLedger rows (ModeLines.entry builds each from its row, so labels and formats are draft copy in the sheet). A mode
     * adds its rows and overrides this; the default has none, so a mode that has not yet is simply without a ledger.
     */
    public List<ModeLines.Entry> ledgerEntries(GravesAndGlory.Battle b){return List.of();}
    /** The tracks' maxima. */
    public int[] max(GravesAndGlory.Battle b){return new int[]{row.winTarget(),row.winTarget()};}
    /** How each ledger number reads (ModeMath.ledgerText): plain, fraction or percent. */
    public String[] formats(){return new String[]{"plain","plain"};}
    /** The side a commander's how-to lines are written for (attack or defend). Domination asks the same of both sides. */
    public String howToSide(GravesAndGlory.Battle b,GravesAndGlory.Squad s){return "attack";}
    /** A bot army's goal in this mode, asked by Campaign.botDecide after its retreat check: a spot to attack toward, or null to
     *  play the points as Domination does (the default). Convoy sends its bots to the wagon (spec 3.6). */
    public Vec3d botGoal(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Vec3d centre,Spec.BotCommandRow tier){return null;}
    /** A bot army's per-group orders, asked by Campaign.botDecide at the end of each decision (never while it retreats): a mode puts
     *  Battle.Assignment rows into s.assignments over the army's order. Siege splits its stage plan by group (spec 5); the default adds none. */
    public void botPlan(GravesAndGlory.Battle b,GravesAndGlory.Squad s,Spec.BotCommandRow tier){}
    /** Whether a weakened bot army may fall back to its field base to rally (Campaign.botDecide's retreat share); false keeps it on its plan. */
    public boolean botRetreats(GravesAndGlory.Battle b,GravesAndGlory.Squad s){return true;}
    /** The lobby's mode block: id, name, rule, how-to lines, side, the tracks and their formats. */
    public JsonObject snapshot(GravesAndGlory.Battle b,GravesAndGlory.Squad viewer){
        JsonObject j=new JsonObject();
        j.addProperty("id",id());j.addProperty("name",name());j.addProperty("rule",rule());
        JsonArray how=new JsonArray();for(String line:ModeMath.howTo(row.howTo()))how.add(line);j.add("howTo",how);
        JsonArray defend=new JsonArray();for(String line:ModeMath.howTo(row.howToDefend()))defend.add(line);j.add("howToDefend",defend);
        j.addProperty("side",howToSide(b,viewer));
        JsonArray max=new JsonArray();for(int n:max(b))max.add(n);j.add("max",max);
        JsonArray formats=new JsonArray();for(String f:formats())formats.add(f);j.add("format",formats);
        j.addProperty("points",capturesPoints(b)); // the HUD hangs the point seals only in a mode that plays its points
        j.addProperty("overtime",b.inOvertime()); // the client swaps the battle music for overtime_music while true (a round that is running, its clock out, the mode holding it open)
        return j;
    }
}
