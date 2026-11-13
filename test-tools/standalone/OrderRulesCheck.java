package gg.gravesandglory;

/**
 * The command UI's order compat mapping (command UI plan Task 3): an old order or formation id read from a packet or a save maps onto the
 * new set (BattleMath.loadOrder / loadFormation, which OrderRules.loadOrder / loadFormation return), and the troop stance list gains
 * "rally" at the end of stance_v2's wire order without moving any older index. OrderIntakeSuite checks OrderRules' delegates in the game.
 */
public final class OrderRulesCheck {
    static int count;
    static void check(boolean pass, String text) { count++; if (!pass) throw new AssertionError(text); }
    public static void main(String[] args) {
        check(BattleMath.loadOrder("move").equals("hold"), "An old Move is the player's Formation order, hold");
        check(BattleMath.loadOrder("hold").equals("hold"), "Hold stays hold");
        check(BattleMath.loadOrder("attack").equals("attack"), "Attack stays attack");
        for (String id : new String[]{"follow", "charge", "retreat"}) check(BattleMath.loadOrder(id).equals(id), id + " stays " + id);
        check(BattleMath.loadOrder("nonsense").equals("follow"), "An unknown order loads as the squad default, follow");
        check(BattleMath.loadOrder(null).equals("follow"), "A missing order loads as follow");
        check(BattleMath.loadFormation("block").equals("block"), "block stays block (now labelled Shield wall)");
        for (String id : new String[]{"banner", "wedge", "loose"}) check(BattleMath.loadFormation(id).equals(id), id + " stays " + id);
        check(BattleMath.loadFormation("nonsense").equals("block"), "An unknown formation loads as block");
        check(BattleMath.loadFormation(null).equals("block"), "A missing formation loads as block");
        // lineReadsAsBlock: the retired line, sent or saved, means block; an unknown id is refused (null)
        check(BattleMath.formationId("line").equals("block") && BattleMath.loadFormation("line").equals("block"), "lineReadsAsBlock: a sent or saved line reads as block");
        check(BattleMath.formationId("nonsense") == null && BattleMath.formationId(null) == null, "lineReadsAsBlock: an unknown formation id is refused");
        // bannerKnown: banner is a sheet shape and the four ids are block, banner, wedge, loose
        check("banner".equals(BattleMath.formationId("banner")), "bannerKnown: banner is a known formation id");
        check(java.util.Arrays.stream(Spec.FORMATIONS).map(Spec.FormationsRow::id).toList().equals(java.util.List.of("block", "banner", "wedge", "loose")), "bannerKnown: the sheet holds block, banner, wedge, loose in that order");
        check(BattleMath.STANCES.indexOf("rally") == 9, "rally is stance 9, appended");
        check(BattleMath.STANCES.indexOf("ranged_aim") == 8, "ranged_aim keeps stance 8");
        check(BattleMath.STANCES.indexOf("charge") == 10 && BattleMath.STANCES.indexOf("stunned") == 11, "charge is stance 10 and stunned 11, appended");
        System.out.println("OrderRulesCheck: " + count + " checks passed");
    }
}
