package gg.gravesandglory;

import java.util.List;

/** Pure battle-mode arithmetic and copy (ModeMath): wave counts, rule and size lines, how-to cells, siege strength,
 *  Convoy roles and ledger text. Checked by javac alone, never shipped. */
public final class ModeMathCheck {
    static int checks, failed;
    static void check(boolean pass, String message) {
        if (!pass && !ALL) throw new AssertionError(message);
        if (!pass) { failed++; System.out.println("FAILED: " + message); return; }
        checks++;
    }
    /** -Dmodemath.all=true lists every failing check instead of stopping at the first (for the red runs of a deliberate break). */
    static final boolean ALL = Boolean.getBoolean("modemath.all");
    public static void main(String[] args) {
        check(ModeMath.waveCount(8,1,0.5,1.0)==8 && ModeMath.waveCount(8,4,0.5,1.0)==20 && ModeMath.waveCount(8,0,0.5,1.0)==8, "banners: x1 plus 0.5 per extra banner");
        check(ModeMath.waveCount(8,1,0.5,0.75)==6 && ModeMath.waveCount(10,1,0.5,0.75)==8 && ModeMath.waveCount(3,1,0.5,1.25)==4 && ModeMath.waveCount(16,4,0.5,1.25)==50, "tier scale, rounded up");
        check(ModeMath.ruleLine("Domination to {n}",300).equals("Domination to 300") && ModeMath.ruleLine("Siege: yard, gate, keep",3).equals("Siege: yard, gate, keep") && ModeMath.ruleLine("Survival: {n} waves",7).equals("Survival: 7 waves"), "rule lines");
        check(ModeMath.howTo(" a | b |  | c | d ").equals(List.of("a","b")) && ModeMath.howTo("one").equals(List.of("one")), "how-to cells");
        check(ModeMath.sizeLine("domination",2,"Normal","none").equals("2 v 2, Normal bots") && ModeMath.sizeLine("domination",1,"Easy","none").equals("1 v 1, Easy bot") && ModeMath.sizeLine("survival",2,"Normal","none").equals("2 banners, Normal waves") && ModeMath.sizeLine("survival",1,"Easy","none").equals("1 banner, Easy waves") && ModeMath.sizeLine("siege",2,"Normal","attack").equals("2 v 2, attack, Normal bots") && ModeMath.sizeLine("banner",3,"Hard","none").equals("3 v 3, Hard bots"), "size lines");
        check(ModeMath.siegeStrength(0,0.36,0,0)==64 && ModeMath.siegeStrength(1,0,400,1600)==25 && ModeMath.siegeStrength(1,0,1600,1600)==100 && ModeMath.siegeStrength(2,1.0,0,0)==0 && ModeMath.siegeStrength(3,0,0,0)==0, "siege strength");
        check(ModeMath.convoyRoles(2,0,0.5,true).equals(List.of("pusher","screen")) && ModeMath.convoyRoles(1,0,0.5,true).equals(List.of("pusher")) && ModeMath.convoyRoles(3,0,0.34,true).equals(List.of("pusher","pusher","screen")) && ModeMath.convoyRoles(3,0,0.34,false).equals(List.of("raider","blocker","blocker")) && ModeMath.convoyRoles(1,0,0.34,false).equals(List.of("raider")) && ModeMath.convoyRoles(3,3,0.5,true).equals(List.of("pusher","pusher","pusher")) && ModeMath.convoyRoles(0,0,0.5,true).isEmpty(), "convoy roles, humans first, at least one pusher or raider");
        check(ModeMath.ledgerText("fraction",1,3).equals("1/3") && ModeMath.ledgerText("percent",64,100).equals("64%") && ModeMath.ledgerText("plain",12,40).equals("12"), "ledger text");
        check(ModeMath.riseLines(List.of("the chapel")).equals(List.of("Dead rise: the chapel")) && ModeMath.riseLines(List.of("the chapel","the ford")).equals(List.of("Dead rise: the chapel","Dead rise: the ford")) && ModeMath.riseLines(List.of("a","b","c")).size()==3 && ModeMath.riseLines(List.of()).equals(List.of("The dead rise")), "survival rise lines, one per site");
        check(ModeMath.restLine(1,60).equals("Wave 1 at rest, +60 gold") && ModeMath.restLine(7,0).equals("Wave 7 laid to rest"), "survival rest lines");
        check(ModeMath.survivalResult(3,7,true,false,0,20)==-1 && ModeMath.survivalResult(7,7,true,false,0,20)==0 && ModeMath.survivalResult(7,7,false,true,19,20)==0 && ModeMath.survivalResult(2,7,false,false,20,20)==1 && ModeMath.survivalResult(7,7,true,true,20,20)==1, "survival result: shrine loss first, then the last wave");
        check(ModeMath.dominationWinner(299,298,300)==-1 && ModeMath.dominationWinner(300,0,300)==0 && ModeMath.dominationWinner(299,300,300)==1, "domination: the side at the target wins, none below it");
        check(ModeMath.dominationWinner(300,301,300)==1 && ModeMath.dominationWinner(302,300,300)==0, "domination: both sides past the target on one score tick, the higher score wins (C05-F2)");
        check(ModeMath.dominationWinner(300,300,300)==0, "domination: both sides on the same score past the target stay team 0 as before (an open question for Kort)");
        check(ModeMath.shrineLost(5,3,0)==6 && ModeMath.shrineLost(5,3,1)==0 && ModeMath.shrineLost(5,0,0)==0, "survival shrine seconds");
        System.out.println("ModeMathCheck: " + checks + " checks passed" + (failed == 0 ? "" : ", " + failed + " failed"));
        if (failed > 0) System.exit(1);
    }
}
