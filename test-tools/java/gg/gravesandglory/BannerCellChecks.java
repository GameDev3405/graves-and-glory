package gg.gravesandglory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import java.util.*;

import static gg.gravesandglory.Phase1Checks.*;

/** Phase 1 suite banner-cells (army rework Task 3): the battle side of the war table. A squad fields its banner at the cells of the table
 *  (deploysAtCells), refills the cell it lost (refillOwnCell), never refunds, refills or occupies a cell for a cell-less troop
 *  (cellessNeverRefunded) and holds 40 troops (fortyTroops). Every check runs on the server thread inside one tick, so no troop moves
 *  between a spawn and its measurement; positions are compared by distance, which does not depend on the side a team forms on. */
final class BannerCellChecks implements Phase1Checks.Suite, Phase1Checks.ServerWatch {
    volatile int stage;volatile UUID player;volatile boolean finished;
    GravesAndGlory.Squad own;boolean forward;

    public void step(MinecraftClient c,int tick){
        if(tick==20){c.setScreen(null);UUID id=c.player.getUuid();server(c,b->{player=id;stage=1;});}
        if(stage==99&&!finished){finished=true;finish(c,"banner-cells");}
        if(tick>1200&&!finished){finished=true;report("p1-banner-cells-timeout",false,"stuck in stage "+stage);finish(c,"banner-cells");}
    }
    @Override public void cleanup(MinecraftClient c){
        if(own!=null)server(c,b->{b.forwardReinforcement=forward;if(b.running)b.campaign.returnLobby();});
    }
    public void serverTick(GravesAndGlory.Battle b){
        if(stage!=1)return;
        var p=b.server.getPlayerManager().getPlayer(player);if(p==null||!skirmish(b,p,0,1,"normal"))return;
        stage=2;own=b.squads.get(player);forward=b.forwardReinforcement;b.forwardReinforcement=false; // new troops deploy at the barracks ring
        for(var s:b.squads.values()){s.reinforceAt=Long.MAX_VALUE;if(s.bot)b.campaign.botRespawns.put(s.owner,Long.MAX_VALUE);}
        run("deploysAtCells",()->deploysAtCells(b));
        run("refillOwnCell",()->refillOwnCell(b));
        run("cellessNeverRefunded",()->cellessNeverRefunded(b));
        run("fortyTroops",()->fortyTroops(b));
        for(var u:b.units)if(u.squad==own)u.mob.discard();b.units.removeIf(u->u.squad==own);
        stage=99;
    }
    static void run(String id,Runnable check){
        try{check.run();}catch(Throwable e){report("p1-banner-cells-"+id,false,"threw "+e);e.printStackTrace();}
    }

    /** The squad's banner: these troops at these cells (id, cell, id, cell, ...) and the army fielded afresh from it. */
    void field(GravesAndGlory.Battle b,Object... at){
        List<String> dense=new ArrayList<>(Collections.nCopies(WarGrid.cells(),RosterMath.EMPTY));
        for(int i=0;i<at.length;i+=2)dense.set((Integer)at[i+1],(String)at[i]);
        Phase2Checks.field(b,own,dense);own.reinforceAt=Long.MAX_VALUE;
    }
    GravesAndGlory.Unit at(GravesAndGlory.Battle b,int slot){
        return b.units.stream().filter(u->u.squad==own&&u.slot==slot&&u.mob.isAlive()).findFirst().orElse(null);
    }
    void remove(GravesAndGlory.Battle b,GravesAndGlory.Unit u){if(u!=null){u.mob.discard();b.units.remove(u);}}
    long count(GravesAndGlory.Battle b){return b.units.stream().filter(u->u.squad==own&&u.mob.isAlive()).count();}
    static double apart(GravesAndGlory.Unit a,GravesAndGlory.Unit b){return a==null||b==null?-1:Math.hypot(a.mob.getX()-b.mob.getX(),a.mob.getZ()-b.mob.getZ());}
    static boolean near(double v,double want,double tol){return Math.abs(v-want)<=tol;}
    static String f(double v){return String.format("%.2f",v);}

    /** Archers at cells 0, 8 and 49. Cells 0 and 8 are 8 gaps of 2.5 apart (20). With rows 1 to 4 empty a row takes no depth, so cell 49 (row 5, column 4) stands one row depth (3) behind row 0 and 10 across: 10.44 from cell 0.
     *  With archers at 9, 18, 27 and 36 as well, rows 1 to 4 are filled: cell 9 is 3 behind cell 0, and cell 49 stands 5 row depths (15) behind and 10 across: 18.03. */
    void deploysAtCells(GravesAndGlory.Battle b){
        field(b,"archer",0,"archer",8,"archer",49);
        boolean sparse=count(b)==3&&at(b,0)!=null&&at(b,8)!=null&&at(b,49)!=null;
        double wide=apart(at(b,0),at(b,8)),far=apart(at(b,0),at(b,49));
        field(b,"archer",0,"archer",8,"archer",49,"archer",9,"archer",18,"archer",27,"archer",36);
        boolean full=count(b)==7&&at(b,0)!=null&&at(b,8)!=null&&at(b,49)!=null&&at(b,9)!=null;
        double wide2=apart(at(b,0),at(b,8)),next=apart(at(b,0),at(b,9)),deep=apart(at(b,0),at(b,49));
        boolean ok=sparse&&full&&near(wide,20,0.5)&&near(far,Math.hypot(3,10),0.5)&&near(wide2,20,0.5)&&near(next,3,0.5)&&near(deep,Math.hypot(15,10),0.5);
        report("p1-banner-cells-deploysAtCells",ok,"cells 0 and 8: "+f(wide)+" apart (want 20); rows 1 to 4 empty: cell 49 is "+f(far)+" from cell 0 (want 10.44); rows 1 to 4 filled: cells 0 and 8 "+f(wide2)+" (want 20), cells 0 and 9 "+f(next)+" (want 3), cells 0 and 49 "+f(deep)+" (want 18.03); fielded "+sparse+"/"+full);
    }

    /** Archers at cells 0, 8 and 9; cells 8 and 9 fall. A queued archer takes cell 8 (the lowest free cell of its id) at the place deploy gives that cell; a second takes cell 9; a third finds no free cell and its gold comes back. */
    void refillOwnCell(GravesAndGlory.Battle b){
        var archer=troop("archer");field(b,"archer",0,"archer",8,"archer",9);
        remove(b,at(b,8));remove(b,at(b,9));own.gold=500;
        Vec3d want=b.deploy(own,8,archer).at();
        own.training.addLast(new GravesAndGlory.Training(archer,b.time,24));b.trainingTick();
        var first=at(b,8);boolean skipped=at(b,9)==null;
        double off=first==null?-1:Math.hypot(first.mob.getX()-want.x,first.mob.getZ()-want.z);
        own.training.addLast(new GravesAndGlory.Training(archer,b.time,24));b.trainingTick();
        var second=at(b,9);
        int gold=own.gold;long before=count(b);
        own.training.addLast(new GravesAndGlory.Training(archer,b.time,24));b.trainingTick();
        boolean refunded=own.gold==gold+24&&count(b)==before&&own.training.isEmpty();
        boolean ok=first!=null&&off<0.5&&skipped&&second!=null&&at(b,0)!=null&&refunded;
        report("p1-banner-cells-refillOwnCell",ok,"cells 8 and 9 empty, one archer queued: it took cell "+(first==null?"none":first.slot+"")+" (want 8), "+f(off)+" blocks from cell 8's place, cell 9 "+(skipped?"still empty":"filled")+" (want empty); the next took cell "+(second==null?"none":second.slot+"")+" (want 9); a third with no free cell: refunded "+refunded+" (gold "+gold+" to "+own.gold+", troops "+before+" to "+count(b)+")");
    }

    /** A Commander temporary (a Grave Bloom's Risen) and a wave troop at a cell past the table have no cell (hasCell false), so they neither hold nor block a cell: the lost archer of cell 3 is refilled at cell 3 with nothing refunded, and when the cell-less pair fall nothing is queued, refilled or refunded for them. */
    void cellessNeverRefunded(GravesAndGlory.Battle b){
        var archer=troop("archer");field(b,"archer",3,"archer",5);own.gold=500;
        Vec3d home=at(b,3).mob.getPos().add(0,0,3);
        var risen=Commander.temporary(b,own,troop("revenant"),home,b.time+2400);
        var wave=b.spawn(own,archer,WarGrid.cells()+7,home.add(2,0,0));
        boolean cellLess=!risen.hasCell()&&!wave.hasCell()&&at(b,3).hasCell()&&at(b,5).hasCell();
        remove(b,at(b,3));int gold=own.gold;
        own.training.addLast(new GravesAndGlory.Training(archer,b.time,24));b.trainingTick();
        var back=at(b,3);boolean refilled=back!=null&&own.gold==gold&&own.training.isEmpty();
        remove(b,risen);remove(b,wave);b.trainingTick();
        boolean quiet=own.training.isEmpty()&&own.gold==gold&&count(b)==2;
        report("p1-banner-cells-cellessNeverRefunded",cellLess&&refilled&&quiet,"temporary slot "+risen.slot+", wave troop slot "+wave.slot+" (hasCell "+risen.hasCell()+"/"+wave.hasCell()+", want false); cell 3 refilled "+refilled+", gold "+gold+" to "+own.gold+"; after the cell-less pair fell: queue "+own.training.size()+", troops "+count(b)+" (want 2)");
    }

    /** A banner of 40 hounds (rules.maxTroops) fields 40 troops, one in each of cells 0 to 39; a 41st hound listed cannot train (Army full). */
    void fortyTroops(GravesAndGlory.Battle b){
        var hound=troop("wm_hound");List<Object> at=new ArrayList<>();for(int i=0;i<40;i++){at.add("wm_hound");at.add(i);}
        field(b,at.toArray());
        long n=b.units.stream().filter(u->u.squad==own&&u.mob.isAlive()&&u.hasCell()&&u.spec.id().equals("wm_hound")).count();
        Set<Integer> slots=new TreeSet<>();for(var u:b.units)if(u.squad==own&&u.mob.isAlive())slots.add(u.slot);
        own.banner.set(40,"wm_hound");String why=b.roomFor(own,hound);own.banner.set(40,RosterMath.EMPTY);
        boolean ok=n==40&&count(b)==40&&slots.size()==40&&slots.iterator().next()==0&&RosterMath.maxTroops()==40&&why!=null&&why.startsWith("Army full");
        report("p1-banner-cells-fortyTroops",ok,n+" hounds fielded of 40 (troops "+count(b)+", distinct cells "+slots.size()+", limit "+RosterMath.maxTroops()+"); a 41st listed: \""+why+"\"");
    }
}
