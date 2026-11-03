package gg.gravesandglory;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SignBlock;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The builder server's "lobby" world (-Dgg.build=lobby): a blank canvas for hand-building the Last Watch camp (PLAN-TO-1.0 2A.6, PARKED.md "The Last Watch").
 * Flat grass ground, no camp structures; each planned area is only a 1-block concrete outline set into the ground, one colour per area, with signs naming the area and its station's job.
 * BuildMode generates it once, like every builder world, and never touches it again. Normal (non-build) play never calls this class.
 */
final class LobbyCanvas {
    /** The ground: x and z from -HALF to HALF-1. Grass at GROUND, three dirt below it, stone down to BOTTOM. */
    static final int HALF=96,GROUND=64,BOTTOM=32;
    static final int SIGN_CHARS=15,SIGN_LINES=4;

    /** One planned area: its warp name, sign text (a title and what happens there), outline colour and inclusive box. Signs stand just inside the south edge (north edge when north is set). */
    record Area(String warp,String title,String text,Block colour,int x0,int z0,int x1,int z1,boolean north){
        int cx(){return Math.floorDiv(x0+x1,2);}
    }

    /** North is -z. The War Gates are at the south edge, the hearth court with the war table at the centre, the House tents in an arc to the north, the graveyard beyond them,
     *  the armoury and training yard together on the west, the team pads and assembly bays between the spawn and the gates. The east side is left free. */
    static final List<Area> AREAS=List.of(
        new Area("spawn","SPAWN","Players arrive here. /warp lists every area of this plan.",Blocks.WHITE_CONCRETE,-7,14,7,24,true),
        new Area("hearth-court","HEARTH COURT","The heart of the camp: the gathering place around the war table.",Blocks.ORANGE_CONCRETE,-18,-24,18,10,false),
        new Area("war-table","WAR TABLE","Use the table to open the War Table: claims, recruits, class cards, troop sheet, campaign.",Blocks.BROWN_CONCRETE,-5,-12,5,-2,false),
        new Area("party-lectern","PARTY LECTERN","Use the lectern to open the party tab and form a party.",Blocks.MAGENTA_CONCRETE,11,-7,15,-3,false),
        new Area("tent-ironclad","IRONCLAD TENT","House tent of the Ironclad: recruit this House's troops here.",Blocks.GRAY_CONCRETE,-47,-58,-33,-40,false),
        new Area("tent-gilded","GILDED TENT","House tent of the Gilded: recruit this House's troops here.",Blocks.YELLOW_CONCRETE,-27,-58,-13,-40,false),
        new Area("tent-graveborn","GRAVEBORN TENT","House tent of the Graveborn: recruit this House's troops here.",Blocks.PURPLE_CONCRETE,-7,-58,7,-40,false),
        new Area("tent-wildmoot","WILDMOOT TENT","House tent of the Wildmoot: recruit this House's troops here.",Blocks.GREEN_CONCRETE,13,-58,27,-40,false),
        new Area("tent-tidecallers","TIDECALLERS TENT","House tent of the Tidecallers: recruit this House's troops here.",Blocks.CYAN_CONCRETE,33,-58,47,-40,false),
        new Area("graveyard","GRAVEYARD","The camp's memorial: a record of campaign milestones and chapters.",Blocks.BLACK_CONCRETE,-20,-90,20,-66,false),
        new Area("armoury","ARMOURY","Gear tiers and troop upgrades: opens the Armoury tab.",Blocks.RED_CONCRETE,-44,-24,-26,-8,false),
        new Area("training-yard","TRAINING YARD","Drillmaster: practice and Battle 1, the training battle.",Blocks.LIME_CONCRETE,-84,-30,-50,10,false),
        new Area("pad-dawn","DAWN TEAM PAD","Stand here to join Dawn.",Blocks.LIGHT_BLUE_CONCRETE,-10,31,-4,37,false),
        new Area("pad-dusk","DUSK TEAM PAD","Stand here to join Dusk.",Blocks.BLUE_CONCRETE,4,31,10,37,false),
        new Area("assembly-bay-1","ASSEMBLY BAY 1","A party or team musters here before marching out through the War Gates.",Blocks.LIGHT_GRAY_CONCRETE,-32,28,-14,42,false),
        new Area("assembly-bay-2","ASSEMBLY BAY 2","A party or team musters here before marching out through the War Gates.",Blocks.LIGHT_GRAY_CONCRETE,14,28,32,42,false),
        new Area("war-gates","WAR GATES","Leave for battle: local matches start here. Bots fill empty seats.",Blocks.PINK_CONCRETE,-12,44,12,51,false));

    static int width(){return 2*HALF;}
    static int total(){return width()*width();}

    /** Column n of total(): clears anything above the ground and lays stone, dirt and grass. */
    static void column(ServerWorld w,int n){
        int x=n%width()-HALF,z=n/width()-HALF;
        w.getChunk(x>>4,z>>4);
        int flags=Block.NOTIFY_LISTENERS|Block.FORCE_STATE;
        for(int y=BOTTOM;y<=GROUND;y++){
            Block b=y==GROUND?Blocks.GRASS_BLOCK:y>=GROUND-3?Blocks.DIRT:Blocks.STONE;
            w.setBlockState(new BlockPos(x,y,z),b.getDefaultState(),flags);
        }
        for(int y=GROUND+1;y<=GROUND+8;y++){var p=new BlockPos(x,y,z);if(!w.getBlockState(p).isAir())w.setBlockState(p,Blocks.AIR.getDefaultState(),flags);}
    }

    /** After the ground: every outline, then every sign. */
    static void finish(ServerWorld w){
        for(var a:AREAS){
            for(int x=a.x0();x<=a.x1();x++){outline(w,x,a.z0(),a);outline(w,x,a.z1(),a);}
            for(int z=a.z0();z<=a.z1();z++){outline(w,a.x0(),z,a);outline(w,a.x1(),z,a);}
        }
        for(var a:AREAS)signs(w,a);
    }
    static void outline(ServerWorld w,int x,int z,Area a){w.setBlockState(new BlockPos(x,GROUND,z),a.colour().getDefaultState(),Block.NOTIFY_ALL);}

    /** The title, then the text, wrapped to sign lines; four lines a sign, the signs side by side along x, centred on the area, one block inside its south (or north) edge. Both faces carry the text. */
    static void signs(ServerWorld w,Area a){
        var lines=new ArrayList<String>(wrap(a.title()));lines.add("");lines.addAll(wrap(a.text()));
        int count=(lines.size()+SIGN_LINES-1)/SIGN_LINES,z=a.north()?a.z0()+1:a.z1()-1,start=a.cx()-(count-1)/2;
        for(int s=0;s<count;s++){
            var pos=new BlockPos(start+s,GROUND+1,z);
            w.setBlockState(pos,Blocks.OAK_SIGN.getDefaultState().with(SignBlock.ROTATION,0),Block.NOTIFY_ALL);
            if(w.getBlockEntity(pos) instanceof SignBlockEntity sign){
                var text=sign.getFrontText();
                for(int l=0;l<SIGN_LINES;l++){int i=s*SIGN_LINES+l;text=text.withMessage(l,Text.literal(i<lines.size()?lines.get(i):""));}
                sign.setText(text,true);sign.setText(text,false);sign.markDirty();
                w.updateListeners(pos,sign.getCachedState(),sign.getCachedState(),Block.NOTIFY_ALL);
            }
        }
    }
    static List<String> wrap(String text){
        var out=new ArrayList<String>();var line=new StringBuilder();
        for(String word:text.split(" ")){
            if(line.length()>0&&line.length()+1+word.length()>SIGN_CHARS){out.add(line.toString());line.setLength(0);}
            if(line.length()>0)line.append(' ');
            line.append(word);
        }
        if(line.length()>0)out.add(line.toString());
        return out;
    }

    /** spawn first (BuildMode.placeSpawn uses the first warp): its centre, facing north. Every other area: two blocks south of its south edge, facing north, its signs in view. */
    static void warps(Map<String,double[]> warps){
        for(var a:AREAS){
            double z=a.warp().equals("spawn")?Math.floorDiv(a.z0()+a.z1(),2):a.z1()+2;
            warps.put(a.warp(),new double[]{a.cx(),GROUND+1,z,180});
        }
    }
}
