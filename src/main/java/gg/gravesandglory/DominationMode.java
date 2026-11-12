package gg.gravesandglory;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/** Domination: every owned point scores once a score interval, and the first side to the target wins the round. */
public final class DominationMode extends BattleMode {
    DominationMode(Spec.ModesRow row){super(row);}
    /** Each side's score toward the target, then the points each side holds. */
    @Override public java.util.List<ModeLines.Entry> ledgerEntries(GravesAndGlory.Battle b){
        int[] held={0,0};
        for(GravesAndGlory.Capture c:b.points)if(c.owner>=0)held[c.owner]++;
        return java.util.List.of(
            ModeLines.entry("domination_score_0",b.scores[0],row.winTarget()),ModeLines.entry("domination_score_1",b.scores[1],row.winTarget()),
            ModeLines.entry("domination_points_0",held[0],b.points.size()),ModeLines.entry("domination_points_1",held[1],b.points.size()));
    }
    @Override public void tick(GravesAndGlory.Battle b){
        if(row.scoreInterval()<=0||b.time%row.scoreInterval()!=0)return;
        for(GravesAndGlory.Capture c:b.points)if(c.owner>=0)b.scores[c.owner]++;
        int t=ModeMath.dominationWinner(b.scores[0],b.scores[1],row.winTarget()); // both past the target on one tick: the higher score (C05-F2)
        if(t>=-1)b.campaign.finish(t); // -1 a draw (equal scores); the victory and defeat stings play from Campaign.finish
    }
}
