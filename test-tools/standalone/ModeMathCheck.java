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
        check(ModeMath.dominationWinner(299,298,300)==-2 && ModeMath.dominationWinner(0,0,300)==-2 && ModeMath.dominationWinner(300,0,300)==0 && ModeMath.dominationWinner(299,300,300)==1, "domination: the side at the target wins, none below it");
        check(ModeMath.dominationWinner(300,301,300)==1 && ModeMath.dominationWinner(302,300,300)==0, "domination: both sides past the target on one score tick, the higher score wins (C05-F2)");
        check(ModeMath.dominationWinner(300,300,300)==-1 && ModeMath.dominationWinner(301,301,300)==-1, "domination: both sides on the same score past the target on one tick is a draw, -1 as at a timeout (C05-F2, Kort 2026-11-12)");
        check(ModeMath.shrineLost(5,3,0)==6 && ModeMath.shrineLost(5,3,1)==0 && ModeMath.shrineLost(5,0,0)==0, "survival shrine seconds");
        // Siege's sides and site names come from the map's modeSites rows (Kort, 2026-11-15: Dawn attacks on Ironhold, Kingsfield and Frostmarch, Dusk on Rivergate and Hollowwood).
        int[] att = new int[Spec.MAPS.length];
        for (int m = 0; m < att.length; m++) att[m] = ModeMath.siegeAttackers(m);
        check(att[1] == 0 && att[5] == 0 && att[6] == 1 && att[7] == 0 && att[8] == 0 && att[9] == 1, "siege attackers by map: Dawn on 1, 5, 7, 8 (Ironhold, Kingsfield, Frostmarch); Dusk on 6, 9 (Rivergate, Hollowwood): " + java.util.Arrays.toString(att));
        check(ModeMath.siegeDefenders(6) == 0 && ModeMath.siegeDefenders(5) == 1 && ModeMath.siegeMap(3) == 1 && ModeMath.siegeAttackers(3) == ModeMath.siegeAttackers(1), "defenders are the other team; a map without siege rows answers Ironhold's");
        check(ModeMath.siegeSides("Gold", "Violet").equals(java.util.List.of("Gold attacks, Violet defends")) && ModeMath.siegeSides("Violet", "Gold").equals(java.util.List.of("Violet attacks, Gold defends")) && ModeMath.siegeSides("Crimson", "Gold").equals(java.util.List.of("Crimson attacks", "Gold defends")), "the sides line names the banner colours, on one row or, when the names are long, two");
        check(java.util.Arrays.equals(ModeMath.siegeSiteNames(1), new String[]{"the Outer Yard", "The Iron Gate", "the Keep"}) && java.util.Arrays.equals(ModeMath.siegeSiteNames(6), new String[]{"North Bridgehead", "North Gatehouse", "Market Eyot"}), "site names: the old Ironhold keeps its copy, a bundled map has its own");
        check(FeedLines.modeLine("siege", "yard_taken", "team", "Violet", "yard", ModeMath.siegeSiteNames(6)[0]).equals("Violet took North Bridgehead") && FeedLines.modeLine("siege", "gate_shut", "gate", ModeMath.siegeSiteNames(1)[1]).equals("The Iron Gate is shut") && FeedLines.modeLine("siege", "start", "yard", ModeMath.siegeSiteNames(1)[0], "keep", ModeMath.siegeSiteNames(1)[2]).equals("Take the Outer Yard, break the gate, seize the Keep."), "feed lines name the map's own sites, and the old copy reads as it did on Ironhold");
        check(java.util.Arrays.equals(ModeMath.siegeIntro(6, 0, "Violet", "Gold"), new String[]{"SIEGE: VIOLET ATTACKS, GOLD DEFENDS", "YOU DEFEND"}) && java.util.Arrays.equals(ModeMath.siegeIntro(6, 1, "Violet", "Gold"), new String[]{"SIEGE: VIOLET ATTACKS, GOLD DEFENDS", "YOU ATTACK"}) && java.util.Arrays.equals(ModeMath.siegeIntro(7, 0, "Gold", "Violet"), new String[]{"SIEGE: GOLD ATTACKS, VIOLET DEFENDS", "YOU ATTACK"}) && ModeMath.siegeIntro(7, -1, "Gold", "Violet")[1].isEmpty(), "the round intro names the banner colours and the viewer's part");
        // The ledger's team order (Siege's two numbers reach the HUD bar, the war map and the report graph by team slot: 0 Dawn, 1 Dusk): attackers' number first only where Dawn attacks.
        for (int m : new int[]{6, 5}) { // Rivergate (Dusk attacks), Kingsfield (Dawn attacks)
            int a = ModeMath.siegeAttackers(m);
            check(ModeMath.byTeam(a, 2, 80).equals(a == 1 ? java.util.List.of(80, 2) : java.util.List.of(2, 80)) && ModeMath.byTeam(a, "fraction", "percent").equals(a == 1 ? java.util.List.of("percent", "fraction") : java.util.List.of("fraction", "percent")), "siege ledger order on map " + m + ": slot " + a + " attacks, so the stages number sits at index " + a);
        }
        check(ModeMath.byTeam(ModeMath.siegeAttackers(6), 2, 80).equals(java.util.List.of(80, 2)) && ModeMath.byTeam(ModeMath.siegeAttackers(5), 2, 80).equals(java.util.List.of(2, 80)), "Rivergate: Dawn is the defence (80% left of the bar), Dusk the stages (2); Kingsfield the other way round");
        check(ModeMath.siegeBreachEvent(1).equals("gate_breached") && ModeMath.siegeBreachEvent(6).equals("gate_breached_named") && FeedLines.modeLine("siege", ModeMath.siegeBreachEvent(1), "gate", ModeMath.siegeSiteNames(1)[1]).equals("The Iron Gate is breached") && FeedLines.modeLine("siege", ModeMath.siegeBreachEvent(6), "gate", ModeMath.siegeSiteNames(6)[1]).equals("North Gatehouse breached"), "the old Ironhold keeps \"The Iron Gate is breached\"; a map with its own gate name gets the shorter line");
        for (int m = 0; m < att.length; m++) for (String n : ModeMath.siegeSiteNames(m)) check(!n.isBlank() && !n.equals("-"), "map " + m + " has a name for each of its yard, gate and keep");
        System.out.println("ModeMathCheck: " + checks + " checks passed" + (failed == 0 ? "" : ", " + failed + " failed"));
        if (failed > 0) System.exit(1);
    }
}
