package gg.gravesandglory;

import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/** Domination: every owned point scores once a score interval, and the first side to the target wins the round. */
public final class DominationMode extends BattleMode {
    DominationMode(Spec.ModesRow row){super(row);}
    @Override public void tick(GravesAndGlory.Battle b){
        if(row.scoreInterval()<=0||b.time%row.scoreInterval()!=0)return;
        for(GravesAndGlory.Capture c:b.points)if(c.owner>=0)b.scores[c.owner]++;
        for(int t=0;t<2;t++)if(b.scores[t]>=row.winTarget()){
            b.campaign.finish(t); // the victory and defeat stings play from Campaign.finish
        }
    }
}
