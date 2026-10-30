package gg.gravesandglory;

import net.minecraft.util.math.Vec3d;

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
    /** Called when a round ends, a map changes or the server stops; must be idempotent and undo setup and the round's changes. */
    public void teardown(GravesAndGlory.Battle b){}
    /** Each running tick, after captureTick: the mode's scoring and its own timers. */
    public void tick(GravesAndGlory.Battle b){}
    /** Where a team respawns and its troops fall back to. */
    public Vec3d fieldBase(GravesAndGlory.Battle b,int team){return GravesAndGlory.Battle.base(team);}
    /** A team's barracks access point. */
    public Vec3d fieldBarracks(GravesAndGlory.Battle b,int team){var f=Spec.FACTIONS[team];return new Vec3d(f.barracksX(),65,f.barracksZ());}
    /** Round gold a squad earns per income tick: passive gold and held points, each scaled by the mode (Survival pays neither). */
    public int income(GravesAndGlory.Battle b,GravesAndGlory.Squad s){
        var E=Spec.ECONOMY[0];
        long owned=b.points.stream().filter(p->p.owner==s.team).count();
        int passive=(int)Math.round(E.passiveGold()*row.passiveScale());
        int objective=(int)Math.round(BattleMath.pointIncome(owned,E.pointGold(),Commander.pointGoldBonus(s),E.objectiveIncomeCap())*row.pointScale());
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
    /** The tracks' maxima. */
    public int[] max(GravesAndGlory.Battle b){return new int[]{row.winTarget(),row.winTarget()};}
    /** How each ledger number reads (ModeMath.ledgerText): plain, fraction or percent. */
    public String[] formats(){return new String[]{"plain","plain"};}
}
