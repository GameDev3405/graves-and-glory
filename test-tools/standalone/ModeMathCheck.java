package gg.gravesandglory;

import java.util.List;

/** Pure battle-mode arithmetic and copy (ModeMath): wave counts, rule and size lines, how-to cells, siege strength,
 *  Banner roles and ledger text. Checked by javac alone, never shipped. */
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
        check(ModeMath.howTo(" a | b |  | c | d ").equals(List.of("a","b","c")) && ModeMath.howTo("one").equals(List.of("one")), "how-to cells");
        check(ModeMath.sizeLine("domination",2,"Normal","none").equals("2 v 2, Normal bots") && ModeMath.sizeLine("domination",1,"Easy","none").equals("1 v 1, Easy bot") && ModeMath.sizeLine("survival",2,"Normal","none").equals("2 banners, Normal waves") && ModeMath.sizeLine("survival",1,"Easy","none").equals("1 banner, Easy waves") && ModeMath.sizeLine("siege",2,"Normal","attack").equals("2 v 2, attack, Normal bots") && ModeMath.sizeLine("banner",3,"Hard","none").equals("3 v 3, Hard bots"), "size lines");
        check(ModeMath.siegeStrength(0,0.36,0,0)==64 && ModeMath.siegeStrength(1,0,400,1600)==25 && ModeMath.siegeStrength(1,0,1600,1600)==100 && ModeMath.siegeStrength(2,1.0,0,0)==0 && ModeMath.siegeStrength(3,0,0,0)==0, "siege strength");
        check(ModeMath.roles(2,1,0.5).equals(List.of("raider","guard")) && ModeMath.roles(1,0,0.5).equals(List.of("split")) && ModeMath.roles(3,0,0.34).equals(List.of("raider","guard","holder")) && ModeMath.roles(4,2,0.5).equals(List.of("raider","raider","guard","holder")) && ModeMath.roles(1,1,0.5).equals(List.of("raider")) && ModeMath.roles(2,2,0.5).equals(List.of("raider","raider")), "banner roles, humans first");
        check(ModeMath.ledgerText("fraction",1,3).equals("1/3") && ModeMath.ledgerText("percent",64,100).equals("64%") && ModeMath.ledgerText("plain",12,40).equals("12"), "ledger text");
        System.out.println("ModeMathCheck: " + checks + " checks passed" + (failed == 0 ? "" : ", " + failed + " failed"));
        if (failed > 0) System.exit(1);
    }
}
