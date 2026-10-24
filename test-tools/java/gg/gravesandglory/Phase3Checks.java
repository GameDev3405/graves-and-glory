package gg.gravesandglory;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.text.Text;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Isolated real-client checks for Phase 3 (the built world). GRAVES_PHASE3_CHECKS=<suite>[,<suite>...]. Never included in releases. */
public final class Phase3Checks implements ClientModInitializer {
    public interface Suite { void step(MinecraftClient c,int tick);
        /** Client ticks the suite may take before it is failed as stuck (one tick is 50 ms). */
        default int limit(){return 2400;}
        /** Called once on the client thread when the suite ends by any path (done, timed out or thrown), before the next suite starts: puts back what the suite changed. */
        default void cleanup(MinecraftClient c){} }
    static final Map<String,Supplier<Suite>> SUITES=new LinkedHashMap<>();
    static{
        SUITES.put("pipeline",Pipeline::new);
        SUITES.put("studio",StudioChecks::new);
        SUITES.put("routes",Routes::new);
        SUITES.put("walk",()->new Routes(true));
        SUITES.put("arena",Rebuild::new);
        SUITES.put("camp",Camp::new);
        SUITES.put("walkthrough",Walkthrough::new);
        SUITES.put("campwalk",CampWalk::new);
        SUITES.put("yard",Yard::new);
        SUITES.put("houses",Houses::new);
        SUITES.put("landmarks",Landmarks::new);
        SUITES.put("supply",Supply::new);
    }
    static final ArrayDeque<String> queue=new ArrayDeque<>();
    static volatile Suite current;static volatile String currentName="";static volatile boolean failed,broken;
    static int tick,front;static boolean started;

    /** One evidence line (pass/fail id) plus a "[p3] id=result why" log line. */
    public static void report(String id,boolean pass,String why){
        if(!pass)failed=true;
        String line=why==null?"":why.replace('\r',' ').replace('\n',' ');
        MeltyTest.report(id,pass,line);System.out.println("[p3] "+id+"="+pass+" "+line);
    }
    /** Saves the framebuffer as screenshots/p3-<name>.png in the run folder. */
    public static void shot(MinecraftClient c,String name){ScreenshotRecorder.saveScreenshot(c.runDirectory,"p3-"+name+".png",c.getFramebuffer(),t->{});}
    /** Runs f on the server thread with the battle. A throw is reported as p3-<suite>-exception and ends the suite. */
    public static void server(MinecraftClient c,Consumer<GravesAndGlory.Battle> f){c.getServer().execute(()->{if(GravesAndGlory.battle!=null)try{f.accept(GravesAndGlory.battle);}catch(Throwable e){report("p3-"+currentName+"-exception",false,e.toString());e.printStackTrace();broken=true;}});}

    static String viewing;static int viewTicks;
    /** One photograph from a fixed camera. Call it every tick with the same arguments until it returns true: the lobby leash goes off, the
     *  player floats as a spectator with his eyes at (x,y,z), looking at (tx,ty,tz), and after 40 ticks (longer while the client has not got
     *  its chunks or finished its terrain, at most 200) the picture is saved as p3-NAME.png and the leash comes back. The next call with a
     *  new name starts the next view. */
    public static boolean view(MinecraftClient c,String name,double x,double y,double z,double tx,double ty,double tz){
        if(!name.equals(viewing)){
            viewing=name;viewTicks=0;var v=MapTour.look(name,x,y,z,tx,ty,tz);UUID id=c.player.getUuid();
            double feet=y-c.player.getStandingEyeHeight(); // MapTour.look aims from the eyes, so the player stands one eye height lower
            server(c,b->{b.campaign.leash=false;var p=b.server.getPlayerManager().getPlayer(id);if(p!=null){p.changeGameMode(GameMode.SPECTATOR);b.relocate(p,v.x(),feet,v.z(),v.yaw(),v.pitch());}});
            return false;
        }
        if(c.currentScreen!=null)c.setScreen(null);
        boolean there=c.player.getEyePos().squaredDistanceTo(x,y,z)<1&&c.world.isChunkLoaded(((int)Math.floor(tx))>>4,((int)Math.floor(tz))>>4)&&c.worldRenderer.isTerrainRenderComplete();
        if(++viewTicks<40||(!there&&viewTicks<200))return false;
        shot(c,name);System.out.println("[p3] note view "+name+" taken after "+viewTicks+" ticks, camera there="+there);
        viewing=null;server(c,b->b.campaign.leash=true);return true;
    }
    /** Runs the suite cleanup, reports p3-SUITE-suite, then stops the client once the last listed suite is done. */
    public static void finish(MinecraftClient c,String suite){
        var done=current;current=null;
        if(done!=null)try{done.cleanup(c);}catch(Exception e){report("p3-"+suite+"-exception",false,"cleanup: "+e);e.printStackTrace();}
        report("p3-"+suite+"-suite",!failed,failed?"an earlier p3 check failed":"all checks passed");
        if(queue.isEmpty())c.scheduleStop();
    }

    @Override public void onInitializeClient(){
        String list=System.getProperty("gravesandglory.phase3Checks","");if(list.isBlank())return;
        for(String name:list.split(","))if(!name.isBlank())queue.add(name.trim());
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            c.options.pauseOnLostFocus=false; // an unfocused test window must not pause the integrated server
            c.options.hudHidden=true; // the pictures are of the world
            if(c.getOverlay()!=null)return;
            if(c.currentScreen instanceof net.minecraft.client.gui.screen.AccessibilityOnboardingScreen s){s.close();return;} // a fresh test root has no options.txt: press Continue as a player would
            if(!started){if(c.currentScreen instanceof FrontMenuScreen&&++front==40){started=true;GravesAndGloryClient.startCamp(c,false);}return;}
            if(c.player==null||c.getServer()==null||!GravesAndGloryClient.ready)return;
            if(current==null){
                if(queue.isEmpty())return;
                currentName=queue.poll();failed=false;broken=false;tick=0;viewing=null;var make=SUITES.get(currentName);
                if(make==null){report("p3-"+currentName+"-suite",false,"unknown suite");if(queue.isEmpty())c.scheduleStop();return;}
                current=make.get();
            }
            if(broken){broken=false;finish(c,currentName);return;}
            try{
                if(++tick>current.limit()){report("p3-"+currentName+"-timeout",false,"no result after "+current.limit()+" ticks");finish(c,currentName);return;}
                current.step(c,tick);
            }catch(Exception e){report("p3-"+currentName+"-exception",false,e.toString());e.printStackTrace();finish(c,currentName);}
        });
    }

    // ---- fixtures shared by the suites ----
    static void air(ServerWorld w,int x0,int y0,int z0,int x1,int y1,int z1){
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++)for(int z=z0;z<=z1;z++)w.setBlockState(new BlockPos(x,y,z),Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
    }
    static BlockState at(ServerWorld w,int x,int y,int z){return w.getBlockState(new BlockPos(x,y,z));}
    static String name(BlockState s){return net.minecraft.registry.Registries.BLOCK.getId(s.getBlock()).getPath();}
    static Direction facing(BlockState s){return s.contains(Properties.HORIZONTAL_FACING)?s.get(Properties.HORIZONTAL_FACING):null;}
    static String xyz(BlockPos p){return "("+p.getX()+","+p.getY()+","+p.getZ()+")";}
    /** What a command said: its feedback and its errors both arrive here. */
    static final class Said implements CommandOutput {
        final List<String> lines=new ArrayList<>();
        @Override public void sendMessage(Text message){lines.add(message.getString());}
        @Override public boolean shouldReceiveFeedback(){return true;}
        @Override public boolean shouldTrackOutput(){return true;}
        @Override public boolean shouldBroadcastConsoleToOps(){return false;}
        String text(){return String.join(" | ",lines);}
    }
    /** Types a command as an operator would, from the given source at level 4, keeping what it says. Comes back with the command's result: 1, or 0 when it reported an error. */
    static int dispatch(GravesAndGlory.Battle b,ServerCommandSource base,Said said,String command)throws Exception{
        return b.server.getCommandManager().getDispatcher().execute(command,base.withLevel(4).withOutput(said));
    }
    /** A 1x1x1 gold_block structure file at the given path, saved from the world the way a structure block saves a piece. */
    static void writeGoldOverride(ServerWorld w,BlockPos scratch,Path file)throws Exception{
        w.setBlockState(scratch,Blocks.GOLD_BLOCK.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
        var gold=new StructureTemplate();gold.saveFromWorld(w,scratch,new Vec3i(1,1,1),false,Blocks.STRUCTURE_VOID);
        w.setBlockState(scratch,Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
        Files.createDirectories(file.getParent());NbtIo.writeCompressed(gold.writeNbt(new NbtCompound()),file.toFile());
    }

    /** Task 1: the placer. The 5x5x5 test marker is placed four ways in the real world and read back block by block; the camera then looks at it. */
    static final class Pipeline implements Suite {
        static final String MARKER="test/p3_marker";
        static final Vec3i SIZE=new Vec3i(5,5,5);
        static final int Y=64,Z=1100;
        volatile boolean placed;int stage;
        @Override public void step(MinecraftClient c,int tick){
            if(tick==20){c.setScreen(null);server(c,this::run);}
            if(!placed)return;
            if(stage==0&&view(c,"pipeline-marker",1005,70,1108,1005,65,1100))stage=1;
            else if(stage==1&&view(c,"pipeline-marker-north",1015,76,1080,1015,66,1100))stage=2;
            if(stage==2)finish(c,"pipeline");
        }
        @Override public void cleanup(MinecraftClient c){server(c,b->{b.campaign.leash=true;StructureStudio.release(b.world);});}

        /** The world block for the marker local cell, for the placement centred on cx at the given rotation. */
        static BlockPos cell(int cx,int deg,int lx,int ly,int lz){return Structures.toWorld(SIZE,cx,Y,Z,deg,lx,ly,lz);}

        void run(GravesAndGlory.Battle b){
            ServerWorld w=b.world;
            StructureStudio.force(w,984,1084,1046,1146);
            air(w,990,63,1094,1040,72,1124); // a clean slate, whatever an earlier run left
            // p3-template-loads
            var template=Structures.template(w,MARKER);
            report("p3-template-loads",template.isPresent()&&template.get().getSize().equals(SIZE),template.isPresent()?"template "+MARKER+" read from the mod resources, size "+template.get().getSize().toShortString():"template "+MARKER+" not found");

            // the ground a void cell must keep, then the four placements and the missing one
            w.setBlockState(new BlockPos(1001,65,1101),Blocks.HAY_BLOCK.getDefaultState(),Block.NOTIFY_LISTENERS);
            Structures.Placed p0=Structures.place(w,MARKER,1000,Y,Z,0);
            Structures.Placed p90=Structures.place(w,MARKER,1010,Y,Z,90);
            Structures.Placed p180=Structures.place(w,MARKER,1020,Y,Z,180);
            Structures.Placed p270=Structures.place(w,MARKER,1030,Y,Z,270);
            String threw="";Structures.Placed gone=null;
            try{gone=Structures.place(w,"test/does_not_exist",1000,Y,1120,0);}catch(Throwable e){threw=e.toString();}
            for(var p:List.of(p0,p90,p180,p270))System.out.println("[p3] note placed "+p.id()+" blocks="+p.blocks()+" box="+p.box()+" in "+String.format("%.2f",p.nanos()/1e6)+" ms");

            // p3-place-exact
            var bad=new StringBuilder();int floor=0;
            for(int x=998;x<=1002;x++)for(int z=1098;z<=1102;z++)if(at(w,x,64,z).isOf(Blocks.POLISHED_ANDESITE))floor++;
            if(floor!=25)bad.append("floor ").append(floor).append("/25 polished_andesite; ");
            for(int y=65;y<=67;y++)if(!at(w,1000,y,1100).isOf(Blocks.STONE_BRICKS))bad.append("pillar y").append(y).append(" is ").append(name(at(w,1000,y,1100))).append("; ");
            if(!at(w,1000,68,1100).isOf(Blocks.LANTERN))bad.append("lantern missing (").append(name(at(w,1000,68,1100))).append("); ");
            var stairs=at(w,1000,65,1102);if(!stairs.isOf(Blocks.OAK_STAIRS)||facing(stairs)!=Direction.SOUTH)bad.append("stairs are ").append(name(stairs)).append(" facing ").append(facing(stairs)).append("; ");
            var banner=at(w,1000,66,1099);if(!banner.isOf(Blocks.WHITE_WALL_BANNER)||facing(banner)!=Direction.NORTH)bad.append("banner is ").append(name(banner)).append(" facing ").append(facing(banner)).append("; ");
            var want=new BlockBox(998,64,1098,1002,68,1102);
            if(!p0.box().equals(want))bad.append("box ").append(p0.box()).append(" is not ").append(want).append("; ");
            if(p0.blocks()!=31)bad.append("blocks ").append(p0.blocks()).append(" is not 31; ");
            if(p0.missing())bad.append("reported missing; ");
            report("p3-place-exact",bad.length()==0,bad.length()==0?"floor 25/25 polished_andesite, pillar stone_bricks y65-67, lantern y68, oak_stairs facing south at (1000,65,1102), white_wall_banner facing north at (1000,66,1099), box "+p0.box()+", blocks="+p0.blocks():bad.toString());

            // p3-place-rotated: a quarter turn clockwise about the centre block
            bad=new StringBuilder();
            for(int y=65;y<=67;y++)if(!at(w,1010,y,1100).isOf(Blocks.STONE_BRICKS))bad.append("pillar y").append(y).append(" is ").append(name(at(w,1010,y,1100))).append("; ");
            var stairs90=at(w,1008,65,1100);if(!stairs90.isOf(Blocks.OAK_STAIRS)||facing(stairs90)!=Direction.WEST)bad.append("stairs are ").append(name(stairs90)).append(" facing ").append(facing(stairs90)).append("; ");
            var banner90=at(w,1011,66,1100);if(!banner90.isOf(Blocks.WHITE_WALL_BANNER)||facing(banner90)!=Direction.EAST)bad.append("banner is ").append(name(banner90)).append(" facing ").append(facing(banner90)).append("; ");
            report("p3-place-rotated",bad.length()==0,bad.length()==0?"at 90 degrees: pillar at (1010,65..67,1100), oak_stairs facing west at (1008,65,1100), white_wall_banner facing east at (1011,66,1100), box "+p90.box():bad.toString());

            // p3-void-keeps-ground
            var hay=at(w,1001,65,1101);
            report("p3-void-keeps-ground",hay.isOf(Blocks.HAY_BLOCK),"the hay_block set at (1001,65,1101), local (3,1,3) which is structure void, is "+name(hay)+" after placement");

            // p3-block-entity
            bad=new StringBuilder();String patterns="";
            var be=w.getBlockEntity(new BlockPos(1000,66,1099));
            if(be instanceof BannerBlockEntity bannerEntity){
                NbtList list=bannerEntity.createNbt().getList(BannerBlockEntity.PATTERNS_KEY,NbtElement.COMPOUND_TYPE);
                for(int i=0;i<list.size();i++){var e=list.getCompound(i);patterns+=(i>0?" then ":"")+e.getString(BannerBlockEntity.PATTERN_KEY)+" "+DyeColor.byId(e.getInt(BannerBlockEntity.COLOR_KEY)).getName();}
                if(list.size()!=2||!list.getCompound(0).getString("Pattern").equals("sc")||list.getCompound(0).getInt("Color")!=4||!list.getCompound(1).getString("Pattern").equals("bo")||list.getCompound(1).getInt("Color")!=11)bad.append("patterns are ").append(patterns).append(" (want sc yellow then bo blue); ");
            }else bad.append("no banner block entity at (1000,66,1099): ").append(be).append("; ");
            report("p3-block-entity",bad.length()==0,bad.length()==0?"the banner at (1000,66,1099) holds "+patterns:bad.toString());

            // p3-no-entities
            bad=new StringBuilder();
            for(var p:List.of(p0,p90,p180,p270)){var found=w.getOtherEntities(null,Box.from(p.box()),e->!(e instanceof PlayerEntity));if(!found.isEmpty())bad.append(found.size()).append(" entities in ").append(p.box()).append("; ");}
            report("p3-no-entities",bad.length()==0,bad.length()==0?"no entity inside the boxes of the four placed markers (setIgnoreEntities)":bad.toString());

            // p3-missing-template
            bad=new StringBuilder();
            if(!threw.isEmpty())bad.append("threw ").append(threw).append("; ");
            else{
                if(!gone.missing())bad.append("missing=false; ");
                if(gone.blocks()!=0)bad.append("blocks=").append(gone.blocks()).append("; ");
                int solid=0;for(int x=996;x<=1004;x++)for(int y=63;y<=72;y++)for(int z=1116;z<=1124;z++)if(!at(w,x,y,z).isAir())solid++;
                if(solid!=0)bad.append(solid).append(" blocks placed around (1000,64,1120); ");
                if(Structures.LOG.isEmpty()||!Structures.LOG.get(Structures.LOG.size()-1).equals(gone))bad.append("not the last entry of Structures.LOG; ");
            }
            report("p3-missing-template",bad.length()==0,bad.length()==0?"place(test/does_not_exist) returned missing=true, placed nothing and threw nothing; Structures.LOG ends with it":bad.toString());

            // p3-no-generated-override: a file in the generated folder of the world wins for the vanilla manager, never for Structures
            bad=new StringBuilder();String vanilla="?";
            var manager=w.getStructureTemplateManager();var id=new Identifier(Structures.NS,MARKER);
            Path generated=b.server.getSavePath(WorldSavePath.GENERATED).resolve(Structures.NS+"/structures/"+MARKER+".nbt");
            try{
                writeGoldOverride(w,new BlockPos(1000,80,1100),generated);
                Structures.clearCache();manager.unloadTemplate(id);
                vanilla=manager.getTemplate(id).map(t->t.getSize().toShortString()).orElse("none");
                if(!vanilla.equals("1, 1, 1"))bad.append("fixture did not take effect: the vanilla manager read ").append(vanilla).append(", not 1, 1, 1; "); // else the check could pass with nothing to resist
                var again=Structures.template(w,MARKER);
                if(again.isEmpty())bad.append("template missing after the override file was written; ");
                else if(!again.get().getSize().equals(SIZE))bad.append("size ").append(again.get().getSize().toShortString()).append(" is not 5, 5, 5 (the override won); ");
            }catch(Exception e){bad.append("harness failure ").append(e).append("; ");}
            finally{try{Files.deleteIfExists(generated);}catch(Exception ignored){}manager.unloadTemplate(id);Structures.clearCache();}
            report("p3-no-generated-override",bad.length()==0,bad.length()==0?"with a 1x1x1 gold_block file in generated/ (the vanilla manager read size "+vanilla+") Structures.template still gives 5, 5, 5; file deleted":bad.toString());

            // p3-to-world: the pure functions the client calls agree with the real placements at all four rotations
            bad=new StringBuilder();
            int[][] runs={{1000,0},{1010,90},{1020,180},{1030,270}};Structures.Placed[] placements={p0,p90,p180,p270};
            for(int i=0;i<runs.length;i++){
                int cx=runs[i][0],deg=runs[i][1];BlockRotation rot=Structures.rotation(deg);String tag="rot "+deg+": ";
                var pillar=cell(cx,deg,2,1,2);if(!w.getBlockState(pillar).isOf(Blocks.STONE_BRICKS)||!pillar.equals(new BlockPos(cx,65,Z)))bad.append(tag).append("pillar not at the centre ").append(xyz(pillar)).append("; ");
                var lantern=cell(cx,deg,2,4,2);if(!w.getBlockState(lantern).isOf(Blocks.LANTERN))bad.append(tag).append("lantern not at ").append(xyz(lantern)).append("; ");
                var st=cell(cx,deg,2,1,4);var stState=w.getBlockState(st);
                if(!stState.isOf(Blocks.OAK_STAIRS)||facing(stState)!=rot.rotate(Direction.SOUTH))bad.append(tag).append("stairs at ").append(xyz(st)).append(" are ").append(name(stState)).append(" facing ").append(facing(stState)).append(", want south turned; ");
                var ba=cell(cx,deg,2,2,1);var baState=w.getBlockState(ba);
                if(!baState.isOf(Blocks.WHITE_WALL_BANNER)||facing(baState)!=rot.rotate(Direction.NORTH))bad.append(tag).append("banner at ").append(xyz(ba)).append(" is ").append(name(baState)).append(" facing ").append(facing(baState)).append(", want north turned; ");
                Vec3d centre=Structures.toWorld(SIZE,cx,Y,Z,deg,2.5,1.5,4.5);
                if(centre.distanceTo(Vec3d.ofCenter(st))>1e-9)bad.append(tag).append("double toWorld ").append(centre).append(" is not the centre of the stairs block; ");
                BlockPos lo=null,hi=null;
                for(int x=cx-3;x<=cx+3;x++)for(int y=63;y<=72;y++)for(int z=Z-3;z<=Z+3;z++)if(!at(w,x,y,z).isAir()){lo=lo==null?new BlockPos(x,y,z):new BlockPos(Math.min(lo.getX(),x),Math.min(lo.getY(),y),Math.min(lo.getZ(),z));hi=hi==null?new BlockPos(x,y,z):new BlockPos(Math.max(hi.getX(),x),Math.max(hi.getY(),y),Math.max(hi.getZ(),z));}
                var foot=Structures.footprint(SIZE,cx,Y,Z,deg);
                if(lo==null||!foot.equals(BlockBox.create(lo,hi)))bad.append(tag).append("footprint ").append(foot).append(" is not the blocks found ").append(lo==null?"none":xyz(lo)+"-"+xyz(hi)).append("; ");
                if(!placements[i].box().equals(foot))bad.append(tag).append("Placed.box ").append(placements[i].box()).append(" is not the footprint; ");
            }
            report("p3-to-world",bad.length()==0,bad.length()==0?"toWorld (block and centre forms) and footprint match the blocks found for the marker at 0, 90, 180 and 270 degrees":bad.toString());
            placed=true;
        }
    }

    /** Task 1: the studio. GRAVES_STUDIO_MODE=verify (default) rebuilds every script (or GRAVES_STUDIO_PIECES, a comma list of GROUP/NAME) and compares it with the
     *  shipped structure file; export builds each, writes GRAVES_STUDIO_OUT/GROUP/NAME.nbt and photographs the piece twice (studio-GROUP-NAME from the south-east,
     *  studio-GROUP-NAME-nw from the north-west). Both modes dump every piece back to a script and rebuild it (p3-dump-GROUP-NAME), and end with the dump round trip of the marker. */
    static final class StudioChecks implements Suite {
        static final String MARKER="test/p3_marker";
        volatile int stage;volatile UUID player;volatile boolean exporting;volatile List<String> pieces=List.of();volatile Vec3i size=new Vec3i(1,1,1);
        int index,settle;
        @Override public int limit(){return 1200+400*pieces.size();}
        static String slug(String id){return id.replace('/','-');}
        @Override public void step(MinecraftClient c,int tick){
            if(tick==20){player=c.player.getUuid();c.setScreen(null);server(c,this::begin);}
            switch(stage){
                case 1->{
                    if(index>=pieces.size()){stage=4;server(c,this::roundtrip);}
                    else{stage=8;server(c,b->{StructureStudio.clear(b.world);settle=0;stage=9;});}
                }
                case 9->{if(++settle>=10){stage=2;server(c,this::piece);}} // the light settles after the last piece is cleared
                case 3->{ // fix round 2: closer cameras (a piece the size of a market filled a fifth of the picture), and the two other corners
                    int sx=size.getX(),sy=size.getY(),sz=size.getZ();double d=Math.max(sx,sz)*0.6+5;var o=StructureStudio.ORIGIN;
                    if(view(c,"studio-"+slug(pieces.get(index)),o.getX()+sx/2.0+0.75*d,o.getY()+sy*0.8+0.25*d,o.getZ()+sz/2.0+0.75*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0))stage=6;
                }
                case 6->{ // the north and west faces, which the first camera cannot see
                    int sx=size.getX(),sy=size.getY(),sz=size.getZ();double d=Math.max(sx,sz)*0.6+5;var o=StructureStudio.ORIGIN;
                    if(view(c,"studio-"+slug(pieces.get(index))+"-nw",o.getX()+sx/2.0-0.75*d,o.getY()+sy*0.8+0.25*d,o.getZ()+sz/2.0-0.75*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0))stage=10;
                }
                case 10->{ // the south and west faces (a market's front is the south)
                    int sx=size.getX(),sy=size.getY(),sz=size.getZ();double d=Math.max(sx,sz)*0.6+5;var o=StructureStudio.ORIGIN;
                    if(view(c,"studio-"+slug(pieces.get(index))+"-sw",o.getX()+sx/2.0-0.75*d,o.getY()+sy*0.8+0.25*d,o.getZ()+sz/2.0+0.75*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0))stage=11;
                }
                case 11->{ // the north and east faces (a customs' road is the north)
                    int sx=size.getX(),sy=size.getY(),sz=size.getZ();double d=Math.max(sx,sz)*0.6+5;var o=StructureStudio.ORIGIN;
                    if(view(c,"studio-"+slug(pieces.get(index))+"-ne",o.getX()+sx/2.0+0.75*d,o.getY()+sy*0.8+0.25*d,o.getZ()+sz/2.0-0.75*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0)){stage=7;server(c,b->{dumpTrip(b);index++;stage=1;});}
                }
                case 5->finish(c,"studio");
                default->{}
            }
        }
        @Override public void cleanup(MinecraftClient c){server(c,b->{b.campaign.leash=true;StructureStudio.release(b.world);});}

        void begin(GravesAndGlory.Battle b){
            exporting=System.getProperty("gravesandglory.studioMode","verify").equals("export");
            var all=StructureStudio.scripts(b.server);String asked=System.getProperty("gravesandglory.studioPieces","").trim();
            var chosen=new ArrayList<String>();
            if(asked.isEmpty())chosen.addAll(all);else for(String id:asked.split(","))if(!id.isBlank())chosen.add(id.trim());
            pieces=chosen;System.out.println("[p3] note studio mode="+(exporting?"export":"verify")+" pieces="+chosen+" scripts="+all+" out="+StructureStudio.out());
            stage=1;
        }
        /** One piece: export mode builds, saves and writes it, then the client photographs it (stage 3); verify mode rebuilds it and compares. */
        void piece(GravesAndGlory.Battle b){
            String id=pieces.get(index),slug=slug(id);
            if(exporting){
                try{
                    Path file=StructureStudio.export(b.server,id);Vec3i header=StructureStudio.size(b.server,id);
                    Vec3i got=b.world.getStructureTemplateManager().createTemplate(NbtIo.readCompressed(file.toFile())).getSize();
                    boolean ok=Files.isRegularFile(file)&&Files.size(file)>0&&got.equals(header);
                    report("p3-export-"+slug,ok,ok?"wrote "+file+" ("+Files.size(file)+" bytes), size "+got.toShortString()+" equals the header":"wrote "+file+" but its size is "+got.toShortString()+" and the header says "+header.toShortString());
                    if(ok){size=header;stage=3;return;}
                }catch(Exception e){report("p3-export-"+slug,false,e.getMessage()==null?e.toString():e.getMessage());}
            }else{
                try{
                    String why=StructureStudio.stale(b.server,id);
                    report("p3-fresh-"+slug,why.isEmpty(),why.isEmpty()?"the script of "+id+" rebuilds to exactly the shipped structure file":why);
                }catch(Exception e){report("p3-fresh-"+slug,false,e.getMessage()==null?e.toString():e.getMessage());}
                dumpTrip(b);
            }
            index++;stage=1;
        }
        /** The piece's structure file (the export, or the shipped file in verify mode) dumped back to a script, that script built and saved again: the two must be
         *  equal, block for block. Blocks whose state comes from their neighbours (fences, walls, panes, stair corners, hanging lanterns, vines) are counted in the
         *  line, so a pass shows /studio dump keeps them. The dump is written to STUDIO_OUT/dumps/GROUP/NAME.mcfunction. A difference names each block. */
        void dumpTrip(GravesAndGlory.Battle b){
            String id=pieces.get(index),slug=slug(id);
            try{
                NbtCompound reference=exporting?NbtIo.readCompressed(StructureStudio.out().resolve(id+".nbt").toFile()):Structures.read(b.server,id).orElseThrow(()->new IllegalStateException("no structure file for "+id));
                String script=StructureStudio.dump(b.world.getStructureTemplateManager().createTemplate(reference));
                Path file=StructureStudio.out().resolve("dumps").resolve(id+".mcfunction");
                Files.createDirectories(file.getParent());Files.writeString(file,script);
                NbtCompound again=StructureStudio.save(b.server,id,StructureStudio.run(b.server,"dump of "+id,script));
                Map<String,String> want=cells(reference),got=cells(again);var bad=new StringBuilder();int differ=0;
                var keys=new TreeSet<String>(want.keySet());keys.addAll(got.keySet());
                for(String at:keys)if(!Objects.equals(want.get(at),got.get(at))){if(++differ<=12)bad.append("at ").append(at).append(" the file has ").append(want.getOrDefault(at,"void")).append(" and the rebuilt dump ").append(got.getOrDefault(at,"void")).append("; ");}
                boolean ok=differ==0&&again.equals(reference);
                if(differ==0&&!ok)bad.append("every block agrees but the NBT differs: ").append(StructureStudio.difference(again,reference));
                report("p3-dump-"+slug,ok,ok?"the dump of "+id+" ("+script.lines().count()+" lines) built and saved equals its structure file block for block: "+want.size()+" blocks, "+shaped(want)+"; dump at "+file:differ+" block(s) differ: "+bad);
            }catch(Exception e){report("p3-dump-"+slug,false,e.getMessage()==null?e.toString():e.getMessage());}
        }
        /** Position to block state (and block entity data) for every block a saved structure holds. */
        static Map<String,String> cells(NbtCompound nbt){
            NbtList palette=nbt.getList("palette",NbtElement.COMPOUND_TYPE),blocks=nbt.getList("blocks",NbtElement.COMPOUND_TYPE);var out=new HashMap<String,String>();
            for(int i=0;i<blocks.size();i++){
                NbtCompound block=blocks.getCompound(i);NbtList at=block.getList("pos",NbtElement.INT_TYPE);
                String state=StructureStudio.stateText(palette.getCompound(block.getInt("state")));
                if(block.contains("nbt",NbtElement.COMPOUND_TYPE)){NbtCompound data=block.getCompound("nbt").copy();for(String k:new String[]{"x","y","z"})data.remove(k);state+=data;}
                out.put("("+at.getInt(0)+","+at.getInt(1)+","+at.getInt(2)+")",state);
            }
            return out;
        }
        /** How many of the blocks have a state that their neighbours decide. */
        static String shaped(Map<String,String> cells){
            int fences=0,walls=0,panes=0,stairs=0,vines=0,hanging=0;
            for(String s:cells.values()){
                String name=s.contains("[")?s.substring(0,s.indexOf('[')):s.contains("{")?s.substring(0,s.indexOf('{')):s;
                if(name.endsWith("_fence"))fences++;else if(name.endsWith("_wall"))walls++;else if(name.endsWith("_pane")||name.endsWith("iron_bars"))panes++;
                else if(name.endsWith("_stairs"))stairs++;else if(name.equals("minecraft:vine"))vines++;else if(name.endsWith("lantern")&&s.contains("hanging=true"))hanging++;
            }
            return fences+" fences, "+walls+" walls, "+panes+" panes or bars, "+stairs+" stairs, "+vines+" vines, "+hanging+" hanging lanterns";
        }
        /** Dumps the marker template back to a script, builds that script, saves it, and compares with the shipped file (or, before the first export, with the fresh build of the script). */
        void roundtrip(GravesAndGlory.Battle b){
            try{
                var shipped=Structures.read(b.server,MARKER);NbtCompound reference;String source;
                if(shipped.isPresent()){reference=shipped.get();source="the shipped file";}
                else{reference=StructureStudio.save(b.server,MARKER,StructureStudio.build(b.server,MARKER));source="the fresh export of the script (no shipped file yet)";}
                StructureTemplate template=shipped.isPresent()?Structures.template(b.world,MARKER).orElseThrow():b.world.getStructureTemplateManager().createTemplate(reference);
                String script=StructureStudio.dump(template);
                Vec3i built=StructureStudio.run(b.server,"dump of "+MARKER,script);
                NbtCompound again=StructureStudio.save(b.server,MARKER,built);
                boolean ok=again.equals(reference);
                report("p3-studio-roundtrip",ok,ok?"the dump of "+MARKER+" ("+script.split("\n").length+" lines), built and saved, equals "+source:"the rebuilt dump differs from "+source+": "+StructureStudio.difference(again,reference));
            }catch(Exception e){report("p3-studio-roundtrip",false,e.getMessage()==null?e.toString():e.getMessage());}
            commands(b);
            stage=5;
        }

        /** The /studio commands, typed through the command dispatcher as an operator types them. What they write goes beside the real studio output, in command-checks. */
        void commands(GravesAndGlory.Battle b){
            String saved=System.getProperty("gravesandglory.studioOut");
            System.setProperty("gravesandglory.studioOut",StructureStudio.out().resolve("command-checks").toString());
            try{
                dumpGenerated(b);
                dumpOrder(b);
                placeCommand(b);
                buildReleases(b);
            }finally{if(saved==null)System.clearProperty("gravesandglory.studioOut");else System.setProperty("gravesandglory.studioOut",saved);}
        }

        /** /studio dump reads the structure block save in generated/ before the shipped piece: with a 1x1x1 file there the script starts "# size 1 1 1",
         *  without it "# size 5 5 5", and the feedback says which one it used. */
        void dumpGenerated(GravesAndGlory.Battle b){
            var bad=new StringBuilder();String withFile="",withoutFile="";
            var manager=b.world.getStructureTemplateManager();var id=new Identifier(Structures.NS,MARKER);
            Path generated=b.server.getSavePath(WorldSavePath.GENERATED).resolve(Structures.NS+"/structures/"+MARKER+".nbt");
            try{
                Path script=StructureStudio.out().resolve(MARKER+".mcfunction");
                try{
                    writeGoldOverride(b.world,StructureStudio.ORIGIN.up(36),generated);
                    manager.unloadTemplate(id);
                    var said=new Said();int result=dispatch(b,b.server.getCommandSource(),said,"studio dump "+MARKER);
                    withFile=Files.readString(script).lines().findFirst().orElse("");
                    if(result!=1)bad.append("with the override the command failed: ").append(said.text()).append("; ");
                    if(!withFile.equals("# size 1 1 1"))bad.append("with the override the script begins '").append(withFile).append("', not '# size 1 1 1' (the stale shipped piece); ");
                    if(!said.text().contains("the generated file was used"))bad.append("with the override the feedback does not say the generated file was used: ").append(said.text()).append("; ");
                }finally{Files.deleteIfExists(generated);manager.unloadTemplate(id);}
                var said=new Said();int result=dispatch(b,b.server.getCommandSource(),said,"studio dump "+MARKER);
                withoutFile=Files.readString(script).lines().findFirst().orElse("");
                if(result!=1)bad.append("without the override the command failed: ").append(said.text()).append("; ");
                if(!withoutFile.equals("# size 5 5 5"))bad.append("without the override the script begins '").append(withoutFile).append("', not '# size 5 5 5'; ");
                if(!said.text().contains("no generated file"))bad.append("without the override the feedback does not say the shipped piece was used: ").append(said.text()).append("; ");
            }catch(Throwable e){bad.append("harness failure ").append(e).append("; ");}
            report("p3-studio-dump-generated",bad.length()==0,bad.length()==0?"with a 1x1x1 file in generated/ the dump of "+MARKER+" begins '"+withFile+"' and the feedback says the generated file was used; with the file deleted it begins '"+withoutFile+"' and says the shipped piece was used":bad.toString());
        }

        /** A ceiling vine and a south-hung vine, as a piece script: each hangs on a block that the script writes before it. */
        static final String VINES="""
            # size 5 3 5
            fill ~0 ~0 ~0 ~4 ~2 ~4 minecraft:air
            setblock ~1 ~2 ~1 minecraft:stone
            setblock ~1 ~1 ~1 minecraft:vine[up=true]
            setblock ~3 ~1 ~3 minecraft:stone
            setblock ~3 ~1 ~2 minecraft:vine[south=true]
            """;

        /** What a block hangs on is written before it. The update of any neighbour written while nothing holds a vine removes it, so the two vines of VINES are
         *  built from the script, saved, dumped, built again from the dump and saved again: both must still stand and the two saves must be equal. */
        void dumpOrder(GravesAndGlory.Battle b){
            var bad=new StringBuilder();String lines="?";
            try{
                var w=b.world;
                Vec3i size=StructureStudio.run(b.server,"vine fixture",VINES);
                var ceiling=new BlockPos(1001,65,1001);var south=new BlockPos(1003,65,1002); // ORIGIN + (1,1,1) and (3,1,2)
                BlockState a=at(w,1001,65,1001),c=at(w,1003,65,1002);
                if(!a.isOf(Blocks.VINE)||!a.get(Properties.UP)||!c.isOf(Blocks.VINE)||!c.get(Properties.SOUTH))bad.append("fixture: the vines are ").append(name(a)).append(" and ").append(name(c)).append(" before any dump; ");
                NbtCompound fixture=StructureStudio.save(b.server,"vine fixture",size);
                String script=StructureStudio.dump(w.getStructureTemplateManager().createTemplate(fixture));
                lines=String.valueOf(script.lines().count());
                int lastStone=script.lastIndexOf("minecraft:stone"),firstVine=script.indexOf("minecraft:vine");
                if(lastStone<0||firstVine<0||lastStone>firstVine)bad.append("the dump writes a vine before the stone it hangs on; ");
                Vec3i built=StructureStudio.run(b.server,"dump of the vine fixture",script);
                a=at(w,1001,65,1001);c=at(w,1003,65,1002);
                if(!a.isOf(Blocks.VINE)||!a.get(Properties.UP))bad.append("the ceiling vine at ").append(xyz(ceiling)).append(" is ").append(name(a)).append(" once the dump is built; ");
                if(!c.isOf(Blocks.VINE)||!c.get(Properties.SOUTH))bad.append("the south-hung vine at ").append(xyz(south)).append(" is ").append(name(c)).append(" once the dump is built; ");
                NbtCompound again=StructureStudio.save(b.server,"vine fixture",built);
                if(!again.equals(fixture))bad.append("the saved dump differs from the fixture: ").append(StructureStudio.difference(again,fixture)).append("; ");
            }catch(Throwable e){bad.append("harness failure ").append(e).append("; ");}
            report("p3-studio-dump-order",bad.length()==0,bad.length()==0?"a ceiling vine and a south-hung vine survive script, save, dump ("+lines+" lines, the stones first), build, save: the two saves are equal":bad.toString());
        }

        /** /studio place puts a piece with its centre block under the feet of the player who types it. The player is moved to a known block and the command is
         *  typed from his own source; what the command says and what stands in the world must agree with Structures.footprint (the floor is the block under
         *  his feet, so the y-1 of the command is proven too). */
        void placeCommand(GravesAndGlory.Battle b){
            var bad=new StringBuilder();String said="";BlockBox want=null;
            try{
                var w=b.world;var p=b.server.getPlayerManager().getPlayer(player);
                if(p==null)throw new IllegalStateException("the player is not on the server");
                int x=1012,y=70,z=1060;var feet=new BlockPos(x,y,z);
                StructureStudio.force(w,1004,1052,1020,1068);
                air(w,x-4,y-3,z-4,x+4,y+6,z+4);
                b.campaign.leash=false;p.changeGameMode(GameMode.SPECTATOR);b.relocate(p,x+0.5,y,z+0.5,0,0);
                if(!BlockPos.ofFloored(p.getPos()).equals(feet))bad.append("the player stands at ").append(p.getPos()).append(", not on ").append(xyz(feet)).append("; ");
                var output=new Said();int result=dispatch(b,p.getCommandSource(),output,"studio place "+MARKER+" 90");said=output.text();
                if(result!=1)bad.append("the command failed: ").append(said).append("; ");
                want=Structures.footprint(Pipeline.SIZE,x,y-1,z,90);
                var last=Structures.LOG.isEmpty()?null:Structures.LOG.get(Structures.LOG.size()-1);
                if(last==null||last.missing()||!last.id().equals(MARKER)||last.blocks()!=31||!last.box().equals(want))bad.append("the last placement is ").append(last).append(", not ").append(MARKER).append(" with 31 blocks in ").append(want).append("; ");
                if(!said.contains(String.valueOf(want)))bad.append("the feedback does not name the box ").append(want).append(": ").append(said).append("; ");
                BlockPos lo=null,hi=null;
                for(int i=x-4;i<=x+4;i++)for(int j=y-3;j<=y+6;j++)for(int k=z-4;k<=z+4;k++)if(!at(w,i,j,k).isAir()){
                    lo=lo==null?new BlockPos(i,j,k):new BlockPos(Math.min(lo.getX(),i),Math.min(lo.getY(),j),Math.min(lo.getZ(),k));
                    hi=hi==null?new BlockPos(i,j,k):new BlockPos(Math.max(hi.getX(),i),Math.max(hi.getY(),j),Math.max(hi.getZ(),k));
                }
                if(lo==null||!BlockBox.create(lo,hi).equals(want))bad.append("the blocks found span ").append(lo==null?"nothing":xyz(lo)+"-"+xyz(hi)).append(", not ").append(want).append("; ");
                int floor=0;for(int i=-2;i<=2;i++)for(int k=-2;k<=2;k++)if(at(w,x+i,y-1,z+k).isOf(Blocks.POLISHED_ANDESITE))floor++;
                if(floor!=25)bad.append("the floor under the feet holds ").append(floor).append("/25 polished_andesite; ");
                for(int j=0;j<3;j++)if(!at(w,x,y+j,z).isOf(Blocks.STONE_BRICKS))bad.append("pillar y").append(y+j).append(" is ").append(name(at(w,x,y+j,z))).append("; ");
                if(!at(w,x,y+3,z).isOf(Blocks.LANTERN))bad.append("lantern missing at ").append(xyz(new BlockPos(x,y+3,z))).append("; ");
                BlockPos stairs=Structures.toWorld(Pipeline.SIZE,x,y-1,z,90,2,1,4);BlockState stairsState=w.getBlockState(stairs);
                if(!stairsState.isOf(Blocks.OAK_STAIRS)||facing(stairsState)!=Direction.WEST)bad.append("stairs at ").append(xyz(stairs)).append(" are ").append(name(stairsState)).append(" facing ").append(facing(stairsState)).append(", want west; ");
            }catch(Throwable e){bad.append("harness failure ").append(e).append("; ");}
            finally{b.campaign.leash=true;}
            report("p3-studio-place",bad.length()==0,bad.length()==0?"typed by the player standing on (1012,70,1060), 'studio place "+MARKER+" 90' put the marker in "+want+" (Structures.footprint): floor 25/25 under his feet, pillar where he stood, stairs west; it said: "+said:bad.toString());
        }

        /** /studio build forces the chunks of the stage to build in them, and forcing is saved with the world, so the command must let go of them when it
         *  finishes. With nothing held when it starts, the stage chunks must be free again afterwards in the studio's own set and in the world's forced
         *  chunks, the piece must stand, and /studio save must write the marker as the script builds it. */
        void buildReleases(GravesAndGlory.Battle b){
            var bad=new StringBuilder();String note="";
            try{
                var w=b.world;
                StructureStudio.release(w); // from here on a forced chunk is the command's
                var o=StructureStudio.ORIGIN;Vec3i size=StructureStudio.size(b.server,MARKER);
                var stage=new ArrayList<Long>(); // the chunks of the piece's box grown by the margin of 2
                for(int cx=(o.getX()-2)>>4;cx<=(o.getX()+size.getX()+1)>>4;cx++)for(int cz=(o.getZ()-2)>>4;cz<=(o.getZ()+size.getZ()+1)>>4;cz++)stage.add(ChunkPos.toLong(cx,cz));
                for(long key:stage)if(w.getForcedChunks().contains(key))bad.append("chunk ").append(new ChunkPos(key)).append(" was forced before the command; ");
                var built=new Said();int result=dispatch(b,b.server.getCommandSource(),built,"studio build "+MARKER);
                if(result!=1)bad.append("studio build failed: ").append(built.text()).append("; ");
                if(!at(w,o.getX()+2,o.getY()+1,o.getZ()+2).isOf(Blocks.STONE_BRICKS))bad.append("the pillar does not stand at ORIGIN+(2,1,2); ");
                for(long key:stage)if(w.getForcedChunks().contains(key))bad.append("chunk ").append(new ChunkPos(key)).append(" is still forced after the command; ");
                if(!StructureStudio.forced.isEmpty())bad.append("the studio still holds ").append(StructureStudio.forced.size()).append(" chunk(s) after the command; ");
                var saved=new Said();int saveResult=dispatch(b,b.server.getCommandSource(),saved,"studio save "+MARKER);
                if(saveResult!=1)bad.append("studio save failed: ").append(saved.text()).append("; ");
                Path file=StructureStudio.out().resolve(MARKER+".nbt");
                NbtCompound onDisk=NbtIo.readCompressed(file.toFile());
                Vec3i wrote=w.getStructureTemplateManager().createTemplate(onDisk).getSize();
                if(!wrote.equals(size))bad.append("studio save wrote size ").append(wrote.toShortString()).append(", the header says ").append(size.toShortString()).append("; ");
                NbtCompound fresh=StructureStudio.save(b.server,MARKER,size);
                if(!onDisk.equals(fresh))bad.append("studio save wrote something other than what stands: ").append(StructureStudio.difference(onDisk,fresh)).append("; ");
                note=stage.size()+" stage chunk(s)";
            }catch(Throwable e){bad.append("harness failure ").append(e).append("; ");}
            report("p3-studio-build-releases",bad.length()==0,bad.length()==0?"'studio build "+MARKER+"' built the piece and left none of its "+note+" forced (the studio holds none and the world's forced chunks hold none); 'studio save' wrote it with the size of the header":bad.toString());
        }
    }

    /** Task 2: the roads, on all five maps. Per map: the walk scan (p3-walk-map<N> passes with no blocked road, deck or through-road cell; all six counts are in
     *  its line), p3-walk-rule-map<N> (the rules of the classification, tried on single cells with a stone put on them), the hits in screenshots/p3-walk-map<N>-blocked.txt, and two photographs of each of the three decks: deck-map<N>-bridge-<k> from the camera the plan gives
     *  (a ring obelisk can stand in front of it) and deck-map<N>-bridge-<k>-high from thirty-four blocks above and out on the south side, over any obelisk. Then the route harness sends twenty knights over
     *  every crossing of the five maps, and p3-routes-map<N> passes when all three crossings of the map had 20 of 20 across.
     *  Suite walk is the same without the photographs and the routes: the scan of the five maps and nothing else, which is the quick way to measure a change to Walkability. */
    static final class Routes implements Suite {
        static final int[] MAPS={0,1,2,3,4};
        /** True in suite walk: the scan of the five maps and nothing else (no photographs, no routes), the quick way to measure a change to Walkability. */
        final boolean scanOnly;
        Routes(){this(false);}
        Routes(boolean scanOnly){this.scanOnly=scanOnly;}
        int map,bridge,stage,waited;MapRouteChecks routes; // bridge counts photographs: two per crossing
        volatile boolean switched,scanned,finished;
        /** Active ticks only: nothing steps while the client is told the arena is not ready, so the builds are not in it. Worst case 15 crossings that all time out is about 20000. */
        @Override public int limit(){return 24000;}
        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            int n=map<MAPS.length?MAPS[map]:-1;
            switch(stage){
                case 0->{switched=false;scanned=false;waited=0;server(c,b->{if(b.campaign.map!=n)b.campaign.switchMap(n);switched=true;});stage=1;}
                case 1->{
                    var b=GravesAndGlory.battle;
                    if(switched&&b!=null&&b.ready&&b.campaign.map==n&&CampaignMaps.selected==n){stage=2;server(c,x->scan(x,n));}
                    else if(++waited>3000){report("p3-walk-map"+n,false,"the arena of map "+n+" was not ready after "+waited+" ticks");map++;stage=map<MAPS.length?0:scanOnly?6:4;}
                }
                case 2->{if(scanned){if(scanOnly){map++;stage=map<MAPS.length?0:6;}else{bridge=0;stage=3;}}}
                case 3->{
                    var bridges=MapRouteChecks.bridges();
                    if(bridge>=bridges.size()*2){map++;stage=map<MAPS.length?0:4;}
                    else{var s=bridges.get(bridge/2);int k=bridge/2;
                        boolean taken=bridge%2==0?view(c,"deck-map"+n+"-bridge-"+k,s.x()-8,70,s.z()+s.depth()/2+10,s.x(),65,s.z())
                            :view(c,"deck-map"+n+"-bridge-"+k+"-high",s.x()-16,s.elevation()+34,s.z()+s.depth()/2+22,s.x(),s.elevation()+1,s.z());
                        if(taken)bridge++;}
                }
                case 4->{routes=new MapRouteChecks(c,MAPS);stage=5;}
                case 5->{if(tick%10==0)server(c,this::pump);if(finished)finish(c,"routes");}
                case 6->{finish(c,"walk");stage=7;}
                default->{}
            }
        }
        @Override public void cleanup(MinecraftClient c){server(c,b->b.campaign.leash=true);}

        /** The classification rules, then the scan of the map that stands, reported and written beside the screenshots. */
        void scan(GravesAndGlory.Battle b,int n){
            rule(b,n);
            long start=System.nanoTime();var s=Walkability.scan(b);
            report("p3-walk-map"+n,s.road()==0&&s.through()==0,"cells="+s.cells()+" road="+s.road()+" through="+s.through()+" building="+s.building()+" base="+s.base()+" point="+s.point());
            try{Path file=MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots/p3-walk-map"+n+"-blocked.txt");Files.createDirectories(file.getParent());Files.writeString(file,s.detail());}
            catch(Exception e){report("p3-walk-map"+n+"-file",false,e.toString());}
            System.out.println("[p3] note walk map "+n+" scanned in "+(System.nanoTime()-start)/1000000+" ms");
            scanned=true;
        }
        /** The classification rules of Walkability, tried on single cells of the map that stands with a stone put on them and taken away again (p3-walk-rule-map<N>): a cell inside a capture ring that is also
         *  a road cell counts as road (the one exception is the pole of a flag at the exact centre of a point, which counts as point), a ring cell off the roads counts as point, a road cell outside the rings counts as road. */
        static void rule(GravesAndGlory.Battle b,int n){
            int[] onRoad=null,offRoad=null,outside=null;
            for(var p:Spec.POINTS)for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++){
                int x=p.x()+dx,z=p.z()+dz;
                if((dx==0&&dz==0)||!plain(b,x,z))continue;
                if(Arena.road(x,z)){if(onRoad==null)onRoad=new int[]{x,z};}else if(offRoad==null)offRoad=new int[]{x,z};
            }
            int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ();
            for(int x=-hx;x<=hx&&outside==null;x++)for(int z=-hz;z<=hz&&outside==null;z++)if(Arena.road(x,z)&&!Walkability.inRing(x,z)&&plain(b,x,z))outside=new int[]{x,z};
            if(onRoad==null||offRoad==null||outside==null){report("p3-walk-rule-map"+n,false,"no cell to try: a ring cell on a road "+(onRoad!=null)+", a ring cell off the roads "+(offRoad!=null)+", a road cell outside the rings "+(outside!=null));return;}
            var a=trial(b,onRoad[0],onRoad[1]);var c=trial(b,offRoad[0],offRoad[1]);var d=trial(b,outside[0],outside[1]);
            int poles=0,fine=0;
            for(var p:Spec.POINTS){var s=Walkability.scan(b,new BlockBox(p.x(),0,p.z(),p.x(),0,p.z()));poles++;if(s.road()==0&&s.through()==0)fine++;}
            boolean ok=a.road()==1&&a.point()==0&&c.point()==1&&c.road()==0&&d.road()==1&&fine==poles;
            report("p3-walk-rule-map"+n,ok,"a stone on a ring cell that is also a road cell ("+onRoad[0]+","+onRoad[1]+"): road "+a.road()+" point "+a.point()+" (want road 1); on a ring cell off the roads ("+offRoad[0]+","+offRoad[1]+"): point "+c.point()+" road "+c.road()+" (want point 1); on a road cell outside the rings ("+outside[0]+","+outside[1]+"): road "+d.road()+" (want 1); the centre cell of "+fine+" of "+poles+" points is not road");
        }
        /** A cell the scan visits and finds clear: dry, not a deck plank, outside every footprint (the ring of a point that a landmark encloses is not its footprint, Walkability.counts), nothing with a collision shape at ground+1 or +2. */
        static boolean plain(GravesAndGlory.Battle b,int x,int z){
            if(Arena.river(x,z)||Walkability.onDeck(x,z))return false;
            for(var s:Spec.STRUCTURES)if(!s.kind().equals("bridge")&&Walkability.counts(s,x,z))return false;
            int g=Arena.ground(x,z);
            for(int y=g+1;y<=g+2;y++){var pos=new BlockPos(x,y,z);if(!b.world.getBlockState(pos).getCollisionShape(b.world,pos).isEmpty())return false;}
            return true;
        }
        /** The scan of one cell with a stone at ground+1, and the old block put back. */
        static Walkability.Scan trial(GravesAndGlory.Battle b,int x,int z){
            var pos=new BlockPos(x,Arena.ground(x,z)+1,z);var old=b.world.getBlockState(pos);
            b.world.setBlockState(pos,Blocks.STONE.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            try{return Walkability.scan(b,new BlockBox(x,pos.getY(),z,x,pos.getY(),z));}
            finally{b.world.setBlockState(pos,old,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);}
        }
        /** On the server thread, as the dev control calls it: one step of the route harness, and its verdict per map once it is done. */
        void pump(GravesAndGlory.Battle b){
            if(finished)return;
            routes.tick();
            if(!routes.done)return;
            for(int n:MAPS){int got=routes.crossed.getOrDefault(n,0);report("p3-routes-map"+n,got==3,got+" of 3 crossings with 20 of 20 knights across");}
            finished=true;
        }
    }

    /** Task 5 (suite yard): the training yard and the tutorial hooks, with a fresh commander (campaignCleared 0 for the run, put back in cleanup).
     *  p3-yard-drillmaster (exactly one villager tagged camp:train and the camp tag within 0.5 of (-38.5,65,-303.5), and openCamp "train" opens CampScreen on page train; picture camp-train-page),
     *  p3-yard-start (the START button of that screen gives a running battle with activeBattle march-1 within 900 ticks; then returnLobby and the battle is over),
     *  p3-yard-lessons (exactly four text displays tagged camp-lesson:<id> where the rows put them, their JSON holding the keybind menu, formation, attack and key.use in turn and no literal {key: marker; then menu is rebound to K,
     *  the lessons are placed again so that the client resolves the keybind afresh, and the display of lesson 1 on the client reads "Press K,"; picture camp-yard-lessons; the binding is put back),
     *  p3-yard-lesson-text (each board as the client holds it reads the sheet text with the bound key names), p3-yard-boards (each lesson is a fixed panel facing east that lies on its board's planks,
     *  and no two of the four lessons and the Drillmaster's label overlap on screen from anywhere a commander stands around the yard), p3-yard-drill-sight (the Drillmaster's label is whole in sight
     *  from the first arrival, the camp-yard camera and the way in; the benches for information), picture camp-yard-boards,
     *  p3-yard-dummies (exactly six armor stands tagged camp-dummy, at (-52.5+2k,65,-309.5), invulnerable, facing yaw -90 (east), iron helmet and leather chestplate),
     *  p3-yard-dummies-locked (an empty hand that right-clicks a dummy at the head or the chest takes nothing),
     *  p3-yard-first-arrival (campaignCleared 0: toLobby puts him within 0.1 of the camp row's first arrival (firstDx, 65, firstDz), yaw 90 (west, to the Drillmaster), pitch 8, picture camp-first-arrival from his own eyes;
     *  campaignCleared 1: at the spawn row; the profile is restored) and the picture camp-yard.
     *  No check passes on nothing: zero villagers, zero displays, zero stands or a missing button are failures. */
    static final class Yard implements Suite {
        static final Spec.CampRow C=Spec.CAMP[0];
        static final String[] KEYS={"key.gravesandglory.menu","key.gravesandglory.formation","key.gravesandglory.attack","key.use"};
        int stage,waited;volatile boolean done;
        volatile int savedCleared=-1;String savedMenuKey;
        volatile String serverPart="",arrivalA="",arrivalB="";volatile boolean started,over;
        @Override public int limit(){return 4000;}
        @Override public void cleanup(MinecraftClient c){
            if(savedMenuKey!=null){GravesAndGloryClient.menu.setBoundKey(net.minecraft.client.util.InputUtil.fromTranslationKey(savedMenuKey));net.minecraft.client.option.KeyBinding.updateKeysByCode();savedMenuKey=null;}
            UUID me=c.player==null?null:c.player.getUuid();
            server(c,b->{b.campaign.leash=true;if(me!=null&&savedCleared>=0){b.campaign.profile(me).campaignCleared=savedCleared;savedCleared=-1;}});
        }
        static Vec3d at(double dx,double y,double dz){return new Vec3d(C.originX()+dx,y,C.originZ()+dz);}
        /** Presses the button whose label starts with the prefix on the open screen; false when there is none. */
        static boolean press(MinecraftClient c,String prefix){
            if(c.currentScreen==null)return false;
            var b=c.currentScreen.children().stream().filter(v->v instanceof net.minecraft.client.gui.widget.ButtonWidget w&&w.getMessage().getString().startsWith(prefix)).map(v->(net.minecraft.client.gui.widget.ButtonWidget)v).findFirst();
            if(b.isEmpty())return false;b.get().onPress();return true;
        }
        /** The server half of p3-yard-drillmaster: what is wrong, empty when right. */
        static String drillmaster(GravesAndGlory.Battle b){
            var w=b.world;var want=at(-38.5,65.0,6.5);var out=new StringBuilder();
            var near=w.getEntitiesByClass(net.minecraft.entity.passive.VillagerEntity.class,new Box(want,want).expand(0.5),v->v.getPos().distanceTo(want)<=0.5);
            if(near.size()!=1)out.append(near.size()).append(" villagers within 0.5 of ").append(want).append(" (want exactly 1); ");
            else{var v=near.get(0);if(!v.getCommandTags().contains("camp:train")||!v.getCommandTags().contains(CampWorld.TAG))out.append("tags ").append(v.getCommandTags()).append(" lack camp:train or the camp tag; ");}
            return out.toString();
        }
        /** The lesson displays of the world by id (a list per id, so that a double is seen). */
        static Map<String,List<net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity>> lessonDisplays(ServerWorld w){
            var m=new LinkedHashMap<String,List<net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity>>();
            for(var e:w.iterateEntities())if(e instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity d)for(var t:d.getCommandTags())if(t.startsWith("camp-lesson:"))m.computeIfAbsent(t.substring("camp-lesson:".length()),k->new ArrayList<>()).add(d);
            return m;
        }
        /** The server half of p3-yard-lessons. */
        static String lessons(GravesAndGlory.Battle b){
            var w=b.world;var out=new StringBuilder();var got=lessonDisplays(w);
            if(Spec.CAMPLESSONS.length!=4)out.append("the campLessons sheet has ").append(Spec.CAMPLESSONS.length).append(" rows (want 4); ");
            if(got.size()!=4)out.append(got.size()).append(" lesson ids on displays (want 4: ").append(got.keySet()).append("); ");
            for(int i=0;i<Spec.CAMPLESSONS.length;i++){
                var row=Spec.CAMPLESSONS[i];var list=got.getOrDefault(row.id(),List.of());
                if(list.size()!=1){out.append(row.id()).append(": ").append(list.size()).append(" displays (want 1); ");continue;}
                var d=list.get(0);var want=at(row.dx(),row.y(),row.dz());
                if(d.getPos().distanceTo(want)>0.05)out.append(row.id()).append(" at ").append(d.getPos()).append(" not ").append(want).append("; ");
                if(!d.getCommandTags().contains(CampWorld.TAG))out.append(row.id()).append(" lacks the camp tag; ");
                String json=d.writeNbt(new NbtCompound()).getString("text");
                if(i<KEYS.length&&!json.contains("\""+KEYS[i]+"\""))out.append(row.id()).append(" text has no keybind ").append(KEYS[i]).append(": ").append(json).append("; ");
                if(json.contains("{key:"))out.append(row.id()).append(" still holds a literal {key: marker; ");
                if(i==3&&json.contains("key.gravesandglory."))out.append(row.id()).append(" names a mod key where it must name key.use; ");
            }
            return out.toString();
        }
        /** Takes the lesson displays away and places them again (fresh Text objects: a keybind text keeps the key it first resolved). */
        static void replaceLessons(ServerWorld w){
            var old=new ArrayList<net.minecraft.entity.Entity>();
            for(var e:w.iterateEntities())for(var t:e.getCommandTags())if(t.startsWith("camp-lesson:")){old.add(e);break;}
            old.forEach(net.minecraft.entity.Entity::discard);
            CampWorld.lessons(w);
        }
        static int lessonCount(ServerWorld w){int n=0;for(var l:lessonDisplays(w).values())n+=l.size();return n;}

        /** The client's text display standing within 0.3 of a point, or null. */
        static net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity clientDisplay(MinecraftClient c,Vec3d at){
            net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity found=null;double best=0.3;
            for(var e:c.world.getEntities())if(e instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity d&&d.getPos().distanceTo(at)<=best){best=d.getPos().distanceTo(at);found=d;}
            return found;
        }
        /** p3-yard-lesson-text: each lesson display on the client reads its sheet text with every {key:<id>} turned into the name of the key that is bound (key.use for use), line breaks read as " / ";
         *  so a garbled board would show here, not only in a picture. "ok ..." when right. */
        static String lessonText(MinecraftClient c){
            var out=new StringBuilder();var seen=new StringBuilder();int read=0;
            for(var row:Spec.CAMPLESSONS){
                var d=clientDisplay(c,at(row.dx(),row.y(),row.dz()));
                if(d==null){out.append(row.id()).append(": no display on the client; ");continue;}
                String got=Text.Serializer.fromJson(d.writeNbt(new NbtCompound()).getString("text")).getString().replace("\n"," / ");
                var m=java.util.regex.Pattern.compile("\\{key:([a-z_]+)\\}").matcher(row.text());var want=new StringBuilder();int from=0;
                while(m.find()){
                    want.append(row.text(),from,m.start());String id=m.group(1).equals("use")?"key.use":"key.gravesandglory."+m.group(1);String name=null;
                    for(var k:c.options.allKeys)if(k.getTranslationKey().equals(id))name=k.getBoundKeyLocalizedText().getString();
                    want.append(name==null?"<no binding "+id+">":name);from=m.end();
                }
                want.append(row.text().substring(from));read++;
                if(!got.equals(want.toString()))out.append(row.id()).append(" reads \"").append(got).append("\", want \"").append(want).append("\"; ");
                if(got.contains("key.gravesandglory")||got.contains("{key"))out.append(row.id()).append(" shows a raw key name; ");
                seen.append(row.id()).append(": ").append(got).append(" | ");
            }
            if(read!=4)out.append(read).append(" lessons read (want 4); ");
            return out.length()==0?"ok every lesson reads its sheet text with the bound keys: "+seen:out.toString();
        }
        /** A text panel as the client draws it (DisplayEntityRenderer: lines of 9 px wrapped at line_width, 1/40 block a pixel times the scale, the dark background 1 px around, bottom centre at the entity):
         *  fixed panels have their own normal (the entity's yaw), center panels turn to the eye. */
        record Panel(String id,Vec3d base,boolean fixed,Vec3d normal,double half,double height,int lines,int px,float scale){
            Vec3d centre(){return base.add(0,height/2,0);}
            /** The four corners seen from an eye: bottom left, bottom right, top right, top left. */
            Vec3d[] corners(Vec3d eye){
                Vec3d r,u;
                if(fixed){r=new Vec3d(-normal.z,0,normal.x);u=new Vec3d(0,1,0);}
                else{var d=centre().subtract(eye).normalize();var up=Math.abs(d.y)>0.98?new Vec3d(1,0,0):new Vec3d(0,1,0);r=up.crossProduct(d).normalize();u=d.crossProduct(r).normalize();if(u.y<0)u=u.multiply(-1);}
                var b=base.subtract(u.multiply(scale*0.025));
                return new Vec3d[]{b.subtract(r.multiply(half)),b.add(r.multiply(half)),b.add(r.multiply(half)).add(u.multiply(height)),b.subtract(r.multiply(half)).add(u.multiply(height))};
            }
        }
        static Panel panel(MinecraftClient c,String id,net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity d){
            var nbt=d.writeNbt(new NbtCompound());var text=Text.Serializer.fromJson(nbt.getString("text"));int lw=nbt.contains("line_width")?nbt.getInt("line_width"):200;
            float s=nbt.getCompound("transformation").getList("scale",net.minecraft.nbt.NbtElement.FLOAT_TYPE).getFloat(0);
            var lines=c.textRenderer.wrapLines(text,lw);int px=0;for(var l:lines)px=Math.max(px,c.textRenderer.getWidth(l));
            boolean fixed=nbt.getString("billboard").equals("fixed");double yaw=Math.toRadians(d.getYaw());
            return new Panel(id,d.getPos(),fixed,new Vec3d(-Math.sin(yaw),0,Math.cos(yaw)),(px+2)/2.0*0.025*s,(lines.size()*9+2)*0.025*s,lines.size(),px,s);
        }
        /** Whether two convex quads of the view plane overlap by more than eps (separating axes). */
        static boolean overlap(double[][] a,double[][] b,double eps){
            for(var poly:new double[][][]{a,b})for(int i=0;i<poly.length;i++){
                double ex=poly[(i+1)%poly.length][0]-poly[i][0],ey=poly[(i+1)%poly.length][1]-poly[i][1],nx=-ey,ny=ex,len=Math.hypot(nx,ny);if(len<1e-9)continue;nx/=len;ny/=len;
                double a0=1e9,a1=-1e9,b0=1e9,b1=-1e9;
                for(var p:a){double v=p[0]*nx+p[1]*ny;a0=Math.min(a0,v);a1=Math.max(a1,v);}
                for(var p:b){double v=p[0]*nx+p[1]*ny;b0=Math.min(b0,v);b1=Math.max(b1,v);}
                if(Math.min(a1,b1)-Math.max(a0,b0)<=eps)return false;
            }
            return true;
        }
        /** p3-yard-boards: every lesson panel is a fixed panel facing east that lies on its board (behind each of 15 points of it, 0.15 back, the board's dark oak planks; in front of it, 0.05 out, nothing with a shape),
         *  and from every place a commander can stand around the yard (feet at the floor, x -56..-20, z -330..-278, a block apart, eye 1.62 up; feet and head cells free) and from the three yard pictures'
         *  cameras no two panels of the four lessons and the Drillmaster's label overlap in his view (the corners of each pair projected on the plane square to the eye's look between them; a fixed panel seen from behind
         *  is skipped, as its board hides it). "ok ..." when right. */
        static String boards(MinecraftClient c){
            var out=new StringBuilder();var panels=new ArrayList<Panel>();
            for(var row:Spec.CAMPLESSONS){var d=clientDisplay(c,at(row.dx(),row.y(),row.dz()));if(d==null){out.append(row.id()).append(": no display on the client; ");continue;}panels.add(panel(c,row.id(),d));}
            Spec.CampStationsRow drill=null;for(var s:Spec.CAMPSTATIONS)if(s.id().equals("drillmaster"))drill=s;
            if(drill==null)out.append("no drillmaster row; ");
            else{var d=clientDisplay(c,new Vec3d(C.originX()+drill.x(),drill.y()+3.7,C.originZ()+drill.z()));if(d==null)out.append("no Drillmaster label on the client; ");else panels.add(panel(c,"drillmaster",d));}
            var sizes=new StringBuilder();int lessonsOn=0;
            for(var p:panels){
                sizes.append(String.format(Locale.ROOT,"%s %s %d lines %d px %.2f x %.2f, y %.2f..%.2f; ",p.id(),p.fixed()?"fixed":"turning",p.lines(),p.px(),2*p.half(),p.height(),p.base().y-p.scale()*0.025,p.base().y-p.scale()*0.025+p.height()));
                if(p.id().equals("drillmaster"))continue;
                if(!p.fixed()){out.append(p.id()).append(" turns to the viewer (want a fixed panel on its board); ");continue;}
                if(p.normal().x<0.99){out.append(p.id()).append(String.format(Locale.ROOT," faces (%.2f, %.2f) not east; ",p.normal().x,p.normal().z));continue;}
                var k=p.corners(p.base().add(p.normal().multiply(5)));int off=0,covered=0;String firstOff="";
                for(int i=0;i<5;i++)for(int j=0;j<3;j++){
                    double fi=i/4.0,fj=j/2.0;var pt=k[0].add(k[1].subtract(k[0]).multiply(fi)).add(k[3].subtract(k[0]).multiply(fj));
                    pt=pt.add(k[1].subtract(k[0]).normalize().multiply(fi==0?0.02:fi==1?-0.02:0)).add(0,fj==0?0.02:fj==1?-0.02:0,0);
                    var back=BlockPos.ofFloored(pt.subtract(p.normal().multiply(0.15)));var front=BlockPos.ofFloored(pt.add(p.normal().multiply(0.05)));
                    if(!c.world.getBlockState(back).isOf(Blocks.DARK_OAK_PLANKS)){off++;if(firstOff.isEmpty())firstOff=back.toShortString()+" is "+c.world.getBlockState(back).getBlock().getTranslationKey();}
                    if(!c.world.getBlockState(front).getCollisionShape(c.world,front).isEmpty())covered++;
                }
                if(off>0)out.append(p.id()).append(": ").append(off).append(" of 15 points not on the board's planks (first ").append(firstOff).append("); ");
                if(covered>0)out.append(p.id()).append(": ").append(covered).append(" of 15 points covered by a block in front; ");
                if(off==0&&covered==0)lessonsOn++;
            }
            var eyes=new ArrayList<Vec3d>();int floor=C.floorY()+1;
            for(int x=-56;x<=-20;x++)for(int z=-330;z<=-278;z++){
                var feet=new BlockPos(x,floor,z);if(!c.world.getBlockState(feet).getCollisionShape(c.world,feet).isEmpty()||!c.world.getBlockState(feet.up()).getCollisionShape(c.world,feet.up()).isEmpty())continue;
                var under=feet.down();if(c.world.getBlockState(under).getCollisionShape(c.world,under).isEmpty())continue;
                eyes.add(new Vec3d(x+.5,floor+1.62,z+.5));
            }
            eyes.add(new Vec3d(-28.5,69,-290));eyes.add(new Vec3d(-51.5,67.6,-313.5));eyes.add(new Vec3d(C.originX()+C.firstDx(),C.spawnY()+1.62,C.originZ()+C.firstDz()));
            int views=0,pairs=0,overlaps=0;var over=new StringBuilder();var count=new TreeMap<String,Integer>();
            for(var eye:eyes){
                views++;
                for(int i=0;i<panels.size();i++)for(int j=i+1;j<panels.size();j++){
                    var a=panels.get(i);var b=panels.get(j);
                    if(a.fixed()&&eye.subtract(a.base()).dotProduct(a.normal())<=0.05||b.fixed()&&eye.subtract(b.base()).dotProduct(b.normal())<=0.05)continue;
                    var dref=a.centre().add(b.centre()).multiply(0.5).subtract(eye).normalize();var up=Math.abs(dref.y)>0.98?new Vec3d(1,0,0):new Vec3d(0,1,0);var rr=up.crossProduct(dref).normalize();var uu=dref.crossProduct(rr).normalize();
                    double[][] pa=new double[4][],pb=new double[4][];boolean front=true;var ca=a.corners(eye);var cb=b.corners(eye);
                    for(int q=0;q<4&&front;q++){
                        var ea=ca[q].subtract(eye);var eb=cb[q].subtract(eye);double da=ea.dotProduct(dref),db=eb.dotProduct(dref);
                        if(da<0.2||db<0.2){front=false;break;}
                        pa[q]=new double[]{ea.dotProduct(rr)/da,ea.dotProduct(uu)/da};pb[q]=new double[]{eb.dotProduct(rr)/db,eb.dotProduct(uu)/db};
                    }
                    if(!front)continue;
                    pairs++;
                    if(overlap(pa,pb,0.002)){overlaps++;count.merge(a.id()+" and "+b.id(),1,Integer::sum);if(over.length()<600)over.append(a.id()).append(" and ").append(b.id()).append(String.format(Locale.ROOT," from (%.1f, %.1f, %.1f); ",eye.x,eye.y,eye.z));}
                }
            }
            if(lessonsOn!=4)out.append(lessonsOn).append(" of 4 lessons lie on their boards; ");
            if(panels.size()!=5)out.append(panels.size()).append(" panels found (want 4 lessons and the Drillmaster's label); ");
            if(views<500)out.append("only ").append(views).append(" places to look from; ");
            if(overlaps>0)out.append(overlaps).append(" overlapping pairs in ").append(pairs).append(" pairs seen from ").append(views).append(" places (").append(count).append("), first: ").append(over);
            return (out.length()==0?"ok the four lessons lie on their boards and no two of the five panels overlap: ":"")+out+"pairs seen "+pairs+" from "+views+" places; panels: "+sizes;
        }
        /** p3-yard-drill-sight: the Drillmaster's label is whole in sight (11 points of its panel: 9 along its middle line from end to end, the middle of its top and of its bottom, each reached by a ray from the eye
         *  that hits no block shape on the way) from the first arrival, the camp-yard camera and the way in from the parade (feet on the floor at z -303.5, x -34.5 to -22.5): no post, beam or rack of the yard covers it
         *  where a commander first meets it. The views from the benches before the boards (x -53.5 and -52.5, z -315.5 to -293.5, looking back east) are counted and named but do not fail it: from there the shade's
         *  two back posts stand in front of the 8-block label, as any post near it does from one side. "ok ..." when right. */
        static String drillSight(MinecraftClient c){
            Spec.CampStationsRow drill=null;for(var s:Spec.CAMPSTATIONS)if(s.id().equals("drillmaster"))drill=s;
            if(drill==null)return "no drillmaster row";
            var d=clientDisplay(c,new Vec3d(C.originX()+drill.x(),drill.y()+3.7,C.originZ()+drill.z()));if(d==null)return "no Drillmaster label on the client";
            var p=panel(c,"drillmaster",d);
            var eyes=new LinkedHashMap<String,Vec3d>();double eye=C.floorY()+1+1.62;
            eyes.put("first arrival",new Vec3d(C.originX()+C.firstDx(),C.spawnY()+1.62,C.originZ()+C.firstDz()));eyes.put("camp-yard camera",new Vec3d(-28.5,69,-290));
            for(double x=-34.5;x<=-22.5;x+=2)eyes.put(String.format(Locale.ROOT,"way in x %.1f",x),new Vec3d(x,eye,C.originZ()+6.5));
            for(double x:new double[]{-53.5,-52.5})for(double z=-315.5;z<=-293.5;z+=2)eyes.put(String.format(Locale.ROOT,"bench (%.1f, %.1f)",x,z),new Vec3d(x,eye,z));
            int views=0,blocked=0,benches=0,benchBlocked=0;var bad=new StringBuilder();var benchBad=new StringBuilder();
            for(var en:eyes.entrySet()){
                boolean bench=en.getKey().startsWith("bench");
                var e=en.getValue();var k=p.corners(e);var mid0=k[0].add(k[3]).multiply(0.5);var mid1=k[1].add(k[2]).multiply(0.5);var pts=new ArrayList<Vec3d>();
                for(int i=0;i<9;i++){double f=0.02+0.96*i/8.0;pts.add(mid0.add(mid1.subtract(mid0).multiply(f)));}
                var c0=k[0].add(k[1]).multiply(0.5);var c3=k[3].add(k[2]).multiply(0.5);pts.add(c0.add(c3.subtract(c0).multiply(0.05)));pts.add(c0.add(c3.subtract(c0).multiply(0.95)));
                int seen=0;
                for(var pt:pts){var hit=c.world.raycast(new net.minecraft.world.RaycastContext(e,pt,net.minecraft.world.RaycastContext.ShapeType.VISUAL,net.minecraft.world.RaycastContext.FluidHandling.NONE,c.player));if(hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS||hit.getPos().distanceTo(pt)<0.1)seen++;}
                if(bench){benches++;if(seen<pts.size()){benchBlocked++;if(benchBad.length()<400)benchBad.append(en.getKey()).append(" ").append(seen).append("/").append(pts.size()).append("; ");}continue;}
                views++;if(seen<pts.size()){blocked++;if(bad.length()<500)bad.append(en.getKey()).append(": ").append(seen).append(" of ").append(pts.size()).append(" points in sight; ");}
            }
            return (blocked==0&&views>=8?"ok ":"")+"the Drillmaster's label ("+String.format(Locale.ROOT,"%.2f x %.2f",2*p.half(),p.height())+") from the arrival, the camp-yard camera and the way in, "+views+" places, with a point out of sight from "+blocked+(bad.length()==0?"":": "+bad)
                +"; for information, from the benches "+benches+" places, "+benchBlocked+" with a point out of sight"+(benchBad.length()==0?"":": "+benchBad);
        }
        static String dummies(GravesAndGlory.Battle b){
            var w=b.world;var out=new StringBuilder();var stands=new ArrayList<net.minecraft.entity.decoration.ArmorStandEntity>();
            for(var e:w.iterateEntities())if(e instanceof net.minecraft.entity.decoration.ArmorStandEntity s&&s.getCommandTags().contains("camp-dummy"))stands.add(s);
            if(stands.size()!=6)out.append(stands.size()).append(" armor stands tagged camp-dummy (want 6); ");
            for(int k=0;k<6;k++){
                final Vec3d target=new Vec3d(C.originX()-52.5+2*k,65,C.originZ()+0.5);
                var hit=stands.stream().filter(s->s.getPos().distanceTo(target)<=0.05).toList();
                if(hit.size()!=1){out.append("dummy ").append(k).append(": ").append(hit.size()).append(" stands at ").append(target).append(" (want 1); ");continue;}
                var s=hit.get(0);
                if(!s.isInvulnerable())out.append("dummy ").append(k).append(" is not invulnerable; ");
                if(!s.hasNoGravity())out.append("dummy ").append(k).append(" has gravity; ");
                if(Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(s.getYaw()+90))>0.5)out.append("dummy ").append(k).append(" faces yaw ").append(s.getYaw()).append(" (want -90); ");
                if(!s.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD).isOf(net.minecraft.item.Items.IRON_HELMET)||!s.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(net.minecraft.item.Items.LEATHER_CHESTPLATE))out.append("dummy ").append(k).append(" lacks the iron helmet or the leather chestplate; ");
                if(!s.getCommandTags().contains(CampWorld.TAG))out.append("dummy ").append(k).append(" lacks the camp tag; ");
            }
            return out.toString();
        }
        /** Whether a commander can take a dummy's armour: for each stand tagged camp-dummy, an empty main hand right-clicks it at the head (y 1.8) and at the chest (y 1.3) along the server's own path
         *  (UseEntityCallback, then ArmorStandEntity.interactAt when the callback passes). The helmet and the chestplate must stay on the stand and the hand must stay empty; anything taken is put back,
         *  and so is what the player held. "ok ..." when nothing could be taken; a spectator or fewer than six dummies is a failure, not a pass. */
        static String dummiesLocked(GravesAndGlory.Battle b,net.minecraft.server.network.ServerPlayerEntity p){
            var w=b.world;var out=new StringBuilder();int n=0,clicks=0;
            var mode=p.interactionManager.getGameMode();p.changeGameMode(net.minecraft.world.GameMode.ADVENTURE); // a commander in the camp (toLobby); the picture views leave the harness player a spectator
            if(p.isSpectator())return "the player is still a spectator, so a click takes nothing whatever the dummies allow";
            var held=p.getMainHandStack().copy();var hand=net.minecraft.util.Hand.MAIN_HAND;
            var head=net.minecraft.entity.EquipmentSlot.HEAD;var chest=net.minecraft.entity.EquipmentSlot.CHEST;
            for(var e:w.iterateEntities())if(e instanceof net.minecraft.entity.decoration.ArmorStandEntity s&&s.getCommandTags().contains("camp-dummy")){
                n++;
                for(double y:new double[]{1.8,1.3}){
                    clicks++;p.setStackInHand(hand,net.minecraft.item.ItemStack.EMPTY);
                    var r=net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(p,w,hand,s,new net.minecraft.util.hit.EntityHitResult(s,s.getPos().add(0,y,0)));
                    if(r==net.minecraft.util.ActionResult.PASS)r=s.interactAt(p,new Vec3d(0,y,0),hand);
                    boolean helmet=s.getEquippedStack(head).isOf(net.minecraft.item.Items.IRON_HELMET),coat=s.getEquippedStack(chest).isOf(net.minecraft.item.Items.LEATHER_CHESTPLATE);
                    var took=p.getMainHandStack();
                    if(!took.isEmpty()||!helmet||!coat){
                        out.append(String.format(Locale.ROOT,"dummy at (%.1f, %.1f): a click at y %.1f gave %s (%s)%s%s; ",s.getX(),s.getZ(),y,took.isEmpty()?"nothing in hand":took.getItem().toString()+" in hand",r,helmet?"":", the helmet is gone",coat?"":", the chestplate is gone"));
                        if(!helmet)s.equipStack(head,new net.minecraft.item.ItemStack(net.minecraft.item.Items.IRON_HELMET));
                        if(!coat)s.equipStack(chest,new net.minecraft.item.ItemStack(net.minecraft.item.Items.LEATHER_CHESTPLATE));
                    }
                }
            }
            p.setStackInHand(hand,held);p.changeGameMode(mode);
            if(n!=6)out.append(n).append(" stands tagged camp-dummy (want 6); ");
            return out.length()==0?"ok "+n+" dummies, "+clicks+" empty-hand clicks at the head and the chest, nothing taken":out.toString();
        }
        /** Where the player stands against a spot (position within the tolerance, yaw and pitch within 0.5): what is wrong, empty when right. */
        static String stands(net.minecraft.server.network.ServerPlayerEntity p,double x,double y,double z,double yaw,double pitch,double tol){
            var out=new StringBuilder();
            if(p.getPos().distanceTo(new Vec3d(x,y,z))>tol)out.append(String.format(Locale.ROOT,"at (%.2f,%.2f,%.2f) not (%.2f,%.2f,%.2f); ",p.getX(),p.getY(),p.getZ(),x,y,z));
            if(Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(p.getYaw()-(float)yaw))>0.5)out.append("yaw ").append(p.getYaw()).append(" not ").append(yaw).append("; ");
            if(Math.abs(p.getPitch()-pitch)>0.5)out.append("pitch ").append(p.getPitch()).append(" not ").append(pitch).append("; ");
            return out.toString();
        }

        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            UUID me=c.player.getUuid();
            switch(stage){
                case 0->{done=false;server(c,b->{b.campaign.leash=true;var prof=b.campaign.profile(me);savedCleared=prof.campaignCleared;prof.campaignCleared=0;b.campaign.toLobby(b.server.getPlayerManager().getPlayer(me));done=true;});stage=1;waited=0;}
                case 1->{if(done&&++waited>=40){c.setScreen(null);serverPart="";done=false;server(c,b->{serverPart=drillmaster(b);GravesAndGlory.openCamp(b.server.getPlayerManager().getPlayer(me),"train");done=true;});stage=2;waited=0;}}
                case 2->{
                    if(done&&++waited>=20){
                        var s=c.currentScreen;boolean ok=s instanceof CampScreen cs&&cs.page.equals("train");
                        String why=serverPart+(ok?"":"openCamp(\"train\") opened "+(s==null?"no screen":s.getClass().getSimpleName()+(s instanceof CampScreen cs2?" on page "+cs2.page:""))+"; ");
                        report("p3-yard-drillmaster",why.isEmpty(),why.isEmpty()?"one villager tagged camp:train at the Drillmaster row and openCamp(\"train\") opened the train page":why);
                        stage=3;waited=0;
                    }
                }
                case 3->{if(++waited>=10){shot(c,"camp-train-page");stage=4;waited=0;}}
                case 4->{
                    started=false;
                    if(!press(c,"START")){report("p3-yard-start",false,"no START button on the train page ("+(c.currentScreen==null?"no screen":c.currentScreen.getClass().getSimpleName())+")");stage=7;waited=0;break;}
                    stage=5;waited=0;
                }
                case 5->{
                    waited++;
                    if(waited%10==0)server(c,b->{started=b.running&&b.campaign.activeBattle!=null&&b.campaign.activeBattle.id().equals("march-1");});
                    if(started||waited>900){
                        report("p3-yard-start",started,started?"pressing START gave a running battle with activeBattle march-1 after about "+waited+" ticks":"no running battle with activeBattle march-1 within 900 ticks after START");
                        stage=6;waited=0;over=false;server(c,b->b.campaign.returnLobby());
                    }
                }
                case 6->{
                    waited++;if(waited%10==0)server(c,b->{over=!b.running&&b.campaign.activeBattle==null;});
                    if(over||waited>300){if(!over)report("p3-yard-start-return",false,"returnLobby left the battle running or activeBattle set after 300 ticks");c.setScreen(null);stage=7;waited=0;}
                }
                case 7->{ // the lessons: the server half, then menu to K and the lessons placed again
                    if(waited==0){
                        c.setScreen(null);done=false;serverPart="";
                        savedMenuKey=GravesAndGloryClient.menu.getBoundKeyTranslationKey();
                        GravesAndGloryClient.menu.setBoundKey(net.minecraft.client.util.InputUtil.fromKeyCode(org.lwjgl.glfw.GLFW.GLFW_KEY_K,0));net.minecraft.client.option.KeyBinding.updateKeysByCode();
                        server(c,b->{b.campaign.leash=true;serverPart=lessons(b);replaceLessons(b.world);int n=lessonCount(b.world);if(n!=4)serverPart+="after placing them again "+n+" lesson displays stand (want 4); ";done=true;});
                        waited=1;
                    }else if(done&&++waited>=10)stage=8;
                }
                case 8->{
                    if(view(c,"camp-yard-lessons",-51.5,67.6,-313.5,-55.5,67.2,-313.5)){
                        var want=at(Spec.CAMPLESSONS[0].dx(),Spec.CAMPLESSONS[0].y(),Spec.CAMPLESSONS[0].dz());String why=serverPart,seen="";
                        net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity found=null;
                        for(var e:c.world.getEntities())if(e instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity d&&d.getPos().distanceTo(want)<=0.5)found=d;
                        if(found==null)why+="the client has no text display at the place of lesson 1; ";
                        else{seen=Text.Serializer.fromJson(found.writeNbt(new NbtCompound()).getString("text")).getString();if(!seen.contains("Press K,"))why+="the client's lesson 1 reads \""+seen+"\" (want it to contain \"Press K,\"); ";}
                        report("p3-yard-lessons",why.isEmpty(),why.isEmpty()?"four lesson displays with the keybinds menu, formation, attack and key.use; with menu on K the client reads: "+seen:why);
                        GravesAndGloryClient.menu.setBoundKey(net.minecraft.client.util.InputUtil.fromTranslationKey(savedMenuKey));net.minecraft.client.option.KeyBinding.updateKeysByCode();savedMenuKey=null;
                        done=false;server(c,b->{replaceLessons(b.world);done=true;}); // fresh texts again, resolved with the key put back
                        stage=9;waited=0;
                    }
                }
                case 9->{ // the boards as the client draws them, with the binding put back: what each says, then where each panel lies and whether two panels overlap from anywhere a commander stands
                    if(done&&++waited>=30){
                        String text=lessonText(c);report("p3-yard-lesson-text",text.startsWith("ok "),text);
                        String boards=boards(c);report("p3-yard-boards",boards.startsWith("ok "),boards);
                        String sight=drillSight(c);report("p3-yard-drill-sight",sight.startsWith("ok "),sight);
                        stage=10;waited=0;
                    }
                }
                case 10->{if(view(c,"camp-yard-boards",-44.0,67.0,-304.5,-56.0,67.0,-304.5)){stage=11;waited=0;}}
                case 11->{
                    if(waited==0){done=false;serverPart="";server(c,b->{serverPart=dummies(b);done=true;});waited=1;}
                    else if(done){report("p3-yard-dummies",serverPart.isEmpty(),serverPart.isEmpty()?"six armor stands tagged camp-dummy at their positions, invulnerable, in an iron helmet and a leather chestplate":serverPart);stage=12;waited=0;}
                }
                case 12->{ // the dummies' armour stays on: an empty hand clicks each at the head and the chest
                    if(waited==0){done=false;serverPart="";server(c,b->{serverPart=dummiesLocked(b,b.server.getPlayerManager().getPlayer(me));done=true;});waited=1;}
                    else if(done){report("p3-yard-dummies-locked",serverPart.startsWith("ok "),serverPart);stage=13;waited=0;}
                }
                case 13->{ // first arrival with no battle cleared
                    if(waited==0){done=false;arrivalA="";arrivalB="";c.setScreen(null);server(c,b->{b.campaign.leash=true;b.campaign.profile(me).campaignCleared=0;b.campaign.toLobby(b.server.getPlayerManager().getPlayer(me));done=true;});waited=1;}
                    else if(done&&++waited>=30){
                        if((c.world.isChunkLoaded(((int)Math.floor(c.player.getX()))>>4,((int)Math.floor(c.player.getZ()))>>4)&&c.worldRenderer.isTerrainRenderComplete())||waited>230){
                            done=false;server(c,b->{arrivalA=stands(b.server.getPlayerManager().getPlayer(me),C.originX()+C.firstDx(),65,C.originZ()+C.firstDz(),90,8,0.1);done=true;});stage=14;waited=0;
                        }
                    }
                }
                case 14->{
                    if(done&&++waited>=3){shot(c,"camp-first-arrival");done=false;server(c,b->{b.campaign.profile(me).campaignCleared=1;b.campaign.toLobby(b.server.getPlayerManager().getPlayer(me));done=true;});stage=15;waited=0;}
                }
                case 15->{
                    if(done&&++waited>=5){done=false;server(c,b->{arrivalB=stands(b.server.getPlayerManager().getPlayer(me),C.originX()+C.spawnDx(),C.spawnY(),C.originZ()+C.spawnDz(),C.spawnYaw(),C.spawnPitch(),0.1);b.campaign.profile(me).campaignCleared=savedCleared;done=true;});stage=16;waited=0;}
                }
                case 16->{
                    if(done){
                        String why=(arrivalA.isEmpty()?"":"campaignCleared 0: "+arrivalA)+(arrivalB.isEmpty()?"":"campaignCleared 1: "+arrivalB);
                        report("p3-yard-first-arrival",why.isEmpty(),why.isEmpty()?String.format(Locale.ROOT,"campaignCleared 0 arrives at the yard (%.1f, 65, %.1f) facing yaw 90 (west), pitch 8; campaignCleared 1 arrives at the camp spawn",C.originX()+C.firstDx(),C.originZ()+C.firstDz()):why);
                        stage=17;
                    }
                }
                case 17->{if(view(c,"camp-yard",-28.5,69,-290,-46,66,-304)){finish(c,"yard");stage=18;}}
                default->{}
            }
        }
    }

    /** Task 4 (suite houses): the House ring, the half that stands without Phase 2 (ArmyMobs, the houses sheet and the recruit page's House tabs are on phase-2-houses, not on this branch).
     *  Checks, each failing when the sheet does not hold the five House rows (no vacuous pass): p3-houses-pieces (the five campPieces rows whose station is recruit:<house> are in Structures.LOG, placed, none missing),
     *  p3-houses-banners (each wall banner, tents (6,9,9) and the graveyard (11,4,18), and each tent's standing banner (0,7,10), at its toWorld position, of the House colour, its patterns in order, the wall banner facing the rotated south and the standing one turned with the piece),
     *  p3-houses-slots (extra: CampWorld.slot(row,0) is the first display slot the plan lists, slot 1 lies two blocks on in the direction it lists, and every slot stands on a block with a collision shape with air at feet and head),
     *  p3-houses-silhouettes (fix round 1: the body height maps of every pair of tents, local x 1..11, z 0..7, differ by 2 or more in at least 26 of 88 columns, so each House has its own roof form; fix round 2: the Tidecallers front elevation, the top of each body column x 1..11 over z 0..7 as seen square on from the parade, is one sail: it climbs from a low side to a high side at least 4 higher and never falls back more than 1 on the way, so it ends in a single prow and not two horns),
     *  p3-houses-shaded (no slot sees the sky: World.isSkyVisible of the block above its feet is false for all 30), p3-houses-opens (CampWorld.station at every slot gives the row's station, and openCamp(p,"recruit:gilded") opens CampScreen on the recruit page with house "gilded";
     *  the display-mob half waits for the merge), p3-houses-recruiter-gone (no campStations row recruiter, no villager tagged camp:recruit). Pictures: camp-recruit-gilded, camp-ring, camp-house-<id> for the five rows (the camera max(width,depth)+8 from the piece centre toward the ring centre, for a tent square on its front from width+3 at y 72 looking at y 69 (stepped up to 3 toward the ring centre), swung 30 degrees north when that spot lies in the smoke of the parade braziers,
     *  (0.5,y,-315.5) at y 67.6, looking at the centre at y 67). Not here until the merge: p3-houses-displays, p3-houses-survive, p3-houses-labels (they need ArmyMobs, the houses sheet and CampDisplays). */
    static final class Houses implements Suite {
        static final Spec.CampRow C=Spec.CAMP[0];
        /** The House banners of the brief: the colour, then pattern code and colour id pairs, in order. */
        static final Map<String,String> BANNERS=Map.of("gilded","white sc 4 bo 4","wildmoot","brown tts 0 bts 0 mr 1","ironclad","black bri 7 mc 1 bo 4","tidecallers","blue gru 9 bts 0 bo 9","graveborn","black gru 13 sku 0 bo 15");
        /** The first display slot of each row (k 0, feet) and the step to the next, from the plan. */
        static final Map<String,double[]> FIRST=Map.of("gilded",new double[]{-21.5,66,-310.5,0,-2},"wildmoot",new double[]{-22.5,66,-329.5,2,0},"graveborn",new double[]{-4.5,65,-340.5,2,0},"ironclad",new double[]{13.5,66,-329.5,2,0},"tidecallers",new double[]{22.5,66,-320.5,0,2});
        int stage,waited,viewIx;volatile boolean done;
        final List<double[]> views=new ArrayList<>();final List<String> viewNames=new ArrayList<>();
        @Override public int limit(){return 3000;}
        @Override public void cleanup(MinecraftClient c){server(c,b->b.campaign.leash=true);}
        static List<Spec.CampPiecesRow> rows(){var out=new ArrayList<Spec.CampPiecesRow>();for(var r:Spec.CAMPPIECES)if(r.station().startsWith("recruit:"))out.add(r);return out;}
        static String house(Spec.CampPiecesRow r){return r.station().substring("recruit:".length());}
        static boolean tent(Spec.CampPiecesRow r){return r.template().startsWith("camp/tent_");}
        static Vec3i size(Spec.CampPiecesRow r){var p=Camp.piece(r.template());return new Vec3i(p.width(),p.height(),p.depth());}
        static BlockPos local(Spec.CampPiecesRow r,int lx,int ly,int lz){return Structures.toWorld(size(r),C.originX()+r.dx(),r.y(),C.originZ()+r.dz(),r.rotation(),lx,ly,lz);}
        /** What is wrong with the banner at p against the House's: its block (colour, wall or standing), its patterns in order; empty when right. */
        static String banner(ServerWorld w,BlockPos p,String house,boolean wall){
            var want=BANNERS.get(house).split(" ");var st=w.getBlockState(p);String id=name(st);
            String block=want[0]+(wall?"_wall_banner":"_banner");
            if(!id.equals(block))return house+(wall?" wall":" standing")+" banner at "+xyz(p)+" is "+id+" (want "+block+"); ";
            var be=w.getBlockEntity(p);if(!(be instanceof BannerBlockEntity bb))return house+" banner at "+xyz(p)+" has no banner block entity; ";
            NbtList list=bb.createNbt().getList(BannerBlockEntity.PATTERNS_KEY,NbtElement.COMPOUND_TYPE);var got=new StringBuilder();
            for(int i=0;i<list.size();i++){var e=list.getCompound(i);got.append(i>0?" ":"").append(e.getString(BannerBlockEntity.PATTERN_KEY)).append(' ').append(e.getInt(BannerBlockEntity.COLOR_KEY));}
            String wantP=String.join(" ",Arrays.copyOfRange(want,1,want.length));
            return got.toString().equals(wantP)?"":house+" banner at "+xyz(p)+" holds ["+got+"] (want ["+wantP+"]); ";
        }
        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            UUID me=c.player.getUuid();
            switch(stage){
                case 0->{done=false;c.setScreen(null);server(c,b->{b.campaign.leash=true;run(b);done=true;});stage=1;waited=0;}
                case 1->{if(done&&++waited>=10){c.setScreen(null);server(c,b->GravesAndGlory.openCamp(b.server.getPlayerManager().getPlayer(me),"recruit:gilded"));stage=2;waited=0;}}
                case 2->{
                    if(++waited>=20){
                        var s=c.currentScreen;boolean ok=s instanceof CampScreen cs&&cs.page.equals("recruit")&&cs.house.equals("gilded");
                        String screen=s==null?"no screen":s.getClass().getSimpleName()+(s instanceof CampScreen cs2?" on page "+cs2.page+" with house \""+cs2.house+"\"":"");
                        report("p3-houses-opens",opensOk&&ok&&rows().size()==5,opensWhy+"openCamp(p,\"recruit:gilded\") opened "+screen+(ok?"":" (want CampScreen on page recruit with house gilded)")+"; the display-mob half (using a Graveborn display opens recruit:graveborn) waits for ArmyMobs (Phase 2 merge)");
                        stage=3;waited=0;
                    }
                }
                case 3->{if(++waited>=5){shot(c,"camp-recruit-gilded");c.setScreen(null);stage=4;viewIx=0;}}
                case 4->{
                    if(viewIx>=views.size()){finish(c,"houses");stage=5;break;}
                    var v=views.get(viewIx);if(view(c,viewNames.get(viewIx),v[0],v[1],v[2],v[3],v[4],v[5]))viewIx++;
                }
                default->{}
            }
        }
        volatile boolean opensOk;volatile String opensWhy="";
        void run(GravesAndGlory.Battle b){
            var w=b.world;var rows=rows();boolean five=rows.size()==5;String count=five?"":"the campPieces sheet has "+rows.size()+" rows with a recruit:<house> station (want 5); ";
            // p3-houses-pieces
            var bad=new StringBuilder(count);var info=new StringBuilder();
            for(var r:rows){
                var want=Camp.box(r);Structures.Placed hit=null;for(var p:Structures.LOG)if(p.id().equals(r.template())&&Camp.same(p.box(),want))hit=p;
                if(hit==null)bad.append(r.id()).append(" has no log entry for ").append(r.template()).append("; ");
                else if(hit.missing()||hit.blocks()<=0)bad.append(r.id()).append(hit.missing()?" is missing its file; ":" placed no blocks; ");
                else info.append(r.id()).append(' ').append(r.template()).append(' ').append(hit.blocks()).append(" blocks, ");
            }
            report("p3-houses-pieces",five&&bad.length()==0,bad.length()==0?"the five House rows are placed: "+info:bad.toString());
            // p3-houses-banners
            bad=new StringBuilder(count);info=new StringBuilder();
            for(var r:rows){
                String h=house(r);var rot=Structures.rotation(r.rotation());
                var wp=tent(r)?local(r,6,9,9):local(r,11,4,18);bad.append(banner(w,wp,h,true));
                var f=facing(w.getBlockState(wp));if(f!=null&&f!=rot.rotate(Direction.SOUTH))bad.append(h).append(" wall banner faces ").append(f).append(" (want ").append(rot.rotate(Direction.SOUTH)).append("); ");
                info.append(h).append(" wall ").append(xyz(wp)).append(" facing ").append(f);
                if(tent(r)){
                    var sp=local(r,0,7,10);bad.append(banner(w,sp,h,false));var st=w.getBlockState(sp);
                    if(st.contains(Properties.ROTATION)&&st.get(Properties.ROTATION)!=rot.rotate(0,16))bad.append(h).append(" standing banner rotation ").append(st.get(Properties.ROTATION)).append(" (want ").append(rot.rotate(0,16)).append("); ");
                    info.append(", standing ").append(xyz(sp));
                }
                info.append("; ");
            }
            report("p3-houses-banners",five&&bad.length()==0,bad.length()==0?"every House banner holds its patterns and faces the front: "+info:bad.toString());
            // p3-houses-silhouettes (fix round 1): each House tent has its own roof form. The body's height map (the top non-air block of each column, local x 1..11, z 0..7, y 0..11)
            // of every pair of tents differs by 2 or more in at least 26 of the 88 columns (30%); dressing outside the body (x 0 and 12, the porch) does not count
            var tentRows=new ArrayList<Spec.CampPiecesRow>();for(var r:rows)if(tent(r))tentRows.add(r);
            var maps=new ArrayList<int[]>();var prof=new StringBuilder();
            for(var r:tentRows){int[] m=new int[88];for(int x=1;x<=11;x++)for(int z=0;z<8;z++){int top=0;for(int y=0;y<12;y++)if(!w.getBlockState(local(r,x,y,z)).isAir())top=y;m[(x-1)*8+z]=top;}maps.add(m);
                int peak=0;for(int v:m)peak=Math.max(peak,v);prof.append(house(r)).append(" peak y ").append(peak).append(", across the middle ");for(int x=1;x<=11;x++)prof.append(x>1?"-":"").append(m[(x-1)*8+4]);prof.append("; ");}
            bad=new StringBuilder(count);int least=88;var pairs=new StringBuilder();
            for(int i=0;i<maps.size();i++)for(int j=i+1;j<maps.size();j++){int d=0;for(int k=0;k<88;k++)if(Math.abs(maps.get(i)[k]-maps.get(j)[k])>=2)d++;least=Math.min(least,d);
                pairs.append(house(tentRows.get(i))).append('/').append(house(tentRows.get(j))).append(' ').append(d).append(", ");
                if(d<26)bad.append(house(tentRows.get(i))).append(" and ").append(house(tentRows.get(j))).append(" share a roof form: only ").append(d).append(" of 88 body columns differ by 2 or more (want 26); ");}
            // fix round 2: the front elevation of the Tidecallers tent (the top of each body column x 1..11 over z 0..7, seen square on from the parade) is one sail:
            // it climbs from its low side to its high side, at least 4 higher, never falling back more than 1 on the way, so its top is a single prow and not two horns
            var fronts=new StringBuilder();
            for(int i=0;i<maps.size();i++){int[] m=maps.get(i);int[] f=new int[11];for(int x=0;x<11;x++)for(int z=0;z<8;z++)f[x]=Math.max(f[x],m[x*8+z]);
                fronts.append(house(tentRows.get(i))).append(" front ");for(int x=0;x<11;x++)fronts.append(x>0?"-":"").append(f[x]);fronts.append("; ");
                if(!house(tentRows.get(i)).equals("tidecallers"))continue;
                boolean east=f[10]>=f[0];int lo=east?f[0]:f[10],hi=east?f[10]:f[0],run=0,fall=0;
                for(int k=0;k<11;k++){int v=f[east?k:10-k];run=Math.max(run,v);fall=Math.max(fall,run-v);}
                int peak=0;for(int v:f)peak=Math.max(peak,v);
                if(hi-lo<4||fall>1||peak!=hi)bad.append("the tidecallers front is not one sail (low side ").append(lo).append(", high side ").append(hi).append(", peak ").append(peak).append(", falls back ").append(fall).append("; want the high side at least 4 above the low one, the peak on it and no fall over 1); ");}
            report("p3-houses-silhouettes",five&&tentRows.size()==4&&bad.length()==0,(bad.length()==0?"every pair of tents differs in its roof form: ":bad.toString())+"columns apart per pair "+pairs+"least "+least+"; "+prof+fronts);
            // p3-houses-slots and p3-houses-shaded
            bad=new StringBuilder(count);var shade=new StringBuilder(count);int slots=0,sky=0;
            for(var r:rows){
                String h=house(r);var first=FIRST.get(h);
                var s0=CampWorld.slot(r,0);var s1=CampWorld.slot(r,1);
                if(first==null)bad.append(h).append(" is not a House of the plan; ");
                else{
                    if(s0.distanceTo(new Vec3d(first[0],first[1],first[2]))>1e-6)bad.append(h).append(" slot 0 at ").append(s0).append(" (want ").append(first[0]).append(',').append(first[1]).append(',').append(first[2]).append("); ");
                    if(Math.abs(s1.x-s0.x-first[3])>1e-6||Math.abs(s1.z-s0.z-first[4])>1e-6)bad.append(h).append(" slot 1 at ").append(s1).append(" is not two blocks on from slot 0 the way the plan says; ");
                }
                for(int k=0;k<6;k++){
                    var s=CampWorld.slot(r,k);var feet=BlockPos.ofFloored(s);slots++;
                    var below=feet.down();if(w.getBlockState(below).getCollisionShape(w,below).isEmpty())bad.append(h).append(" slot ").append(k).append(" stands on ").append(name(w.getBlockState(below))).append(" (no collision shape); ");
                    for(var at:new BlockPos[]{feet,feet.up()}){var st=w.getBlockState(at);if(!st.getCollisionShape(w,at).isEmpty())bad.append(h).append(" slot ").append(k).append(" has ").append(name(st)).append(" at ").append(xyz(at)).append("; ");}
                    if(w.isSkyVisible(feet.up())){sky++;shade.append(h).append(" slot ").append(k).append(" at ").append(xyz(feet)).append(" sees the sky; ");}
                }
            }
            report("p3-houses-slots",five&&bad.length()==0,bad.length()==0?"slot 0 of each row is the plan's, slot 1 two blocks on the plan's way, and all "+slots+" slots stand on a solid block with air at feet and head":bad.toString());
            report("p3-houses-shaded",five&&slots==30&&sky==0,shade.length()==0?"none of the "+slots+" slots sees the sky (World.isSkyVisible of the block above the feet)":shade.toString());
            // the server half of p3-houses-opens: the station of every slot
            bad=new StringBuilder(count);int right=0;
            for(var r:rows)for(int k=0;k<6;k++){var s=CampWorld.slot(r,k);String got=CampWorld.station(BlockPos.ofFloored(s));if(r.station().equals(got))right++;else bad.append(house(r)).append(" slot ").append(k).append(": station ").append(got).append(" (want ").append(r.station()).append("); ");}
            opensOk=five&&bad.length()==0;opensWhy=bad.length()==0?"CampWorld.station gives the row's station at all "+right+" slots; ":bad.toString();
            // p3-houses-recruiter-gone
            int villagers=0;for(var e:w.iterateEntities())if(e instanceof net.minecraft.entity.passive.VillagerEntity&&e.getCommandTags().contains("camp:recruit"))villagers++;
            boolean row=Arrays.stream(Spec.CAMPSTATIONS).anyMatch(s->s.id().equals("recruiter"));
            report("p3-houses-recruiter-gone",five&&villagers==0&&!row,count+"campStations row recruiter "+(row?"still there":"gone")+", villagers tagged camp:recruit "+villagers);
            // the photographs
            views.clear();viewNames.clear();
            views.add(new double[]{0.5,72,-291,0.5,66,-330});viewNames.add("camp-ring");
            for(var r:rows){
                var sz=size(r);double cx=C.originX()+r.dx()+0.5,cz=C.originZ()+r.dz()+0.5,d=Math.max(sz.getX(),sz.getZ())+8,ax=0.5-cx,az=-315.5-cz,len=Math.hypot(ax,az);ax/=len;az/=len;
                if(tent(r)){var front=Structures.rotation(r.rotation()).rotate(Direction.SOUTH);ax=front.getOffsetX();az=front.getOffsetZ();d=sz.getX()+3;} // fix round 1: a tent is photographed square on its front from width+3 and from y 72 (looking at y 69), so its roof form shows over the porch and fills the frame; from max(w,d)+8 toward the ring centre at eye height the neighbouring tents and the graveyard filled it
                double sx=cx+ax*d,sz2=cz+az*d;
                if(Math.min(Math.hypot(sx+4.5,sz2+315.5),Math.hypot(sx-5.5,sz2+315.5))<4){ // straight toward the ring centre the camera would stand in the smoke of the parade's two braziers (world -4.5/5.5, -315.5): it swings 30 degrees to the north, away from the parade's posts
                    double c30=Math.cos(Math.toRadians(30)),s30=Math.sin(Math.toRadians(30));double[] ra={ax*c30-az*s30,ax*s30+az*c30},rb={ax*c30+az*s30,-ax*s30+az*c30};var o=ra[1]<rb[1]?ra:rb;ax=o[0];az=o[1];}
                double side=0;if(tent(r)){double px=-az,pz=ax,toward=(0.5-cx)*px+(-315.5-cz)*pz;side=Math.signum(toward)*Math.min(3,Math.abs(toward));sx=px*side;sz2=pz*side;}else{sx=0;sz2=0;} // a tent's camera steps up to 3 blocks sideways toward the ring centre, out from behind the neighbour's porch
                views.add(new double[]{cx+ax*d+sx,tent(r)?72:67.6,cz+az*d+sz2,cx,tent(r)?69:67,cz});viewNames.add("camp-house-"+house(r));
            }
        }
    }

    /** Task 3 (suite camp): the camp base as a commander meets it. One launch, in this order: spawn (p3-camp-spawn), then on the world as built p3-camp-built, p3-camp-budget, p3-camp-stations and the war table of map 0 (p3-camp-war-table, first
     *  part), p3-camp-shapes (extra: every fence, wall, pane and bar inside the pieces is settled, every vine of the files stands, the game rule doVinesSpread is off), and the two checks of the look of the terrain (fix round 2): p3-camp-bank-mix (at least 9 of the 12 rows of the bank
     *  of the ridge have no block above 85 percent of their columns, so the bank is a mix and not stripes) and p3-camp-flank-relief (the crest of each flank, at x 90 either side of the origin, spans at least 6 blocks); then p3-camp-pages (every page the stations name opens its screen),
     *  p3-camp-pads (both pads put the squad on their team), p3-camp-leash (the three cases of the brief), the sixteen photographs (twelve of map 0: p3-camp-arrival.png, -ridge, -aerial, -war-table, -gates-inside, -gates-outside, -archive, -south-pads, -south-oblique, -bank, -flank-west and
     *  -flank-east, the brief's seven, two of the south rim and three of the terrain; and the four of Frostmarch, p3-camp-map2-gates-inside.png, -ridge, -south-oblique and -aerial, taken once map 2 stands), then every map in turn (map 0 stands, maps 1 to 4 are built over it) the rim seam
     *  (p3-camp-rim-seam, fix round 2: one verdict after the last map; with GRAVES_BOX_DUMP set map 0 is built once more at the end and the playable box of each map is dumped), and on map 2 the war table built over the camp (second part); then a world made by an older build is planted
     *  (the old pads, the gold seal, a brick of the old tower, and entities of the older tags), the terrain marker is deleted, and CampWorld.build runs again as it does on the first load of an old world (p3-camp-old-gone),
     *  and last a build that has nothing to rebuild (p3-camp-rebuild-idempotent: both CRC32 readings and the build itself are in one server callback, so no random tick can grow a vine between the two readings). */
    static final class Camp implements Suite {
        static final Spec.CampRow C=Spec.CAMP[0];
        static final List<String> PAGES=new ArrayList<>();
        static{for(var s:Spec.CAMPSTATIONS)if(!PAGES.contains(s.page()))PAGES.add(s.page());}
        static final String[] VIEW_NAMES={"camp-arrival","camp-ridge","camp-aerial","camp-war-table","camp-gates-inside","camp-gates-outside","camp-archive","camp-south-pads","camp-south-oblique","camp-bank","camp-flank-west","camp-flank-east"};
        /** The views that are taken again once Frostmarch (map 2, the highest hills at the south edge) stands: indexes into VIEWS, the photographs are called camp-map2-<name>. */
        static final int[] FROST_VIEWS={4,1,8,2};
        /** The first seven are the brief's, except camp-gates-outside, whose camera went from y 70 to y 71.5: the southern rim now stands up to y 69 at that spot (z -271), where the ground used to be 62, so the camera is one and a half blocks above the slope.
         *  The next two (fix round 1) look at the south rim as a commander meets it: from the west pad area toward the south-west, and from the east of the bowl toward the south-west across the gatehouse.
         *  The last three (fix round 2) look at the terrain itself: the bank of the ridge from the east side of the bowl, and each flank from above the middle of the bowl. */
        static final double[][] VIEWS={{0.5,67.6,-292.0,0.5,67,-330},{0.5,82,-361,0.5,64,-300},{0.5,110,-250,0.5,64,-320},{0.5,84,-361,0.5,78,-371},{0.5,67,-298,0.5,66,-276},{0.5,71.5,-271,0.5,67,-290},{30.5,68,-304,46,66,-304},{-9.5,66.6,-290.0,-36,76,-268},{30.5,66.6,-300.0,-30,76,-268},{44.5,67,-328,-6,71,-356},{-10.5,92,-330,-80,84,-330},{10.5,92,-330,80,84,-330}};
        int stage,waited,page,pad,leashCase,shotIndex,mapIx,sub,frostIx;
        volatile boolean done;
        /** The maps the rim seam is looked at on, in the order they are built (map 0 stands from the start of the world). With GRAVES_BOX_DUMP set, map 0 is built once more at the end: the first map 0 was built before the rule that stops the random ticks could be set. */
        static final boolean DUMPING=System.getenv("GRAVES_BOX_DUMP")!=null&&!System.getenv("GRAVES_BOX_DUMP").isEmpty();
        static final int[] SEAM_MAPS=DUMPING?new int[]{0,1,2,3,4,0}:new int[]{0,1,2,3,4};
        final StringBuilder seamWhy=new StringBuilder();
        boolean seamOk=true;
        final StringBuilder pagesBad=new StringBuilder(),padsWhy=new StringBuilder(),leashWhy=new StringBuilder();
        boolean padsOk=true,leashOk=true;
        @Override public int limit(){return 16000;}
        @Override public void cleanup(MinecraftClient c){server(c,b->b.campaign.leash=true);}
        static Spec.PiecesRow piece(String id){for(var p:Spec.PIECES)if(p.id().equals(id))return p;throw new IllegalStateException("no pieces row "+id);}
        static BlockBox box(Spec.CampPiecesRow row){var p=piece(row.template());return Structures.footprint(new Vec3i(p.width(),p.height(),p.depth()),C.originX()+row.dx(),row.y(),C.originZ()+row.dz(),row.rotation());}
        static boolean same(BlockBox a,BlockBox b){return a.getMinX()==b.getMinX()&&a.getMaxX()==b.getMaxX()&&a.getMinY()==b.getMinY()&&a.getMaxY()==b.getMaxY()&&a.getMinZ()==b.getMinZ()&&a.getMaxZ()==b.getMaxZ();}
        static Vec3d pos(Spec.CampStationsRow s){return new Vec3d(C.originX()+s.x(),s.y(),C.originZ()+s.z());}
        static long millis(String key){var m=java.util.regex.Pattern.compile("camp "+key+" (\\d+) ms");for(var l:new ArrayList<>(CampWorld.LINES)){var f=m.matcher(l);if(f.find())return Long.parseLong(f.group(1));}return -1;}
        static boolean skipped(String key){for(var l:new ArrayList<>(CampWorld.LINES))if(l.contains("camp "+key+" skipped"))return true;return false;}

        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            switch(stage){
                case 0->{done=false;UUID me=c.player.getUuid();server(c,b->{b.campaign.leash=true;if(DUMPING)b.server.getGameRules().get(net.minecraft.world.GameRules.RANDOM_TICK_SPEED).set(0,b.server);spawn(b,me);done=true;});stage=1;waited=0;}
                case 1->{if(done&&++waited>=20){done=false;server(c,b->{built(b);budget();stations(b);tableMap0(b);shapes(b);look(b);done=true;});stage=2;}}
                case 2->{if(done){stage=3;page=0;waited=0;}}
                case 3->{
                    if(page>=PAGES.size()){report("p3-camp-pages",pagesBad.length()==0,pagesBad.length()==0?"every page opened its screen: "+String.join(", ",PAGES):pagesBad.toString());c.setScreen(null);stage=4;pad=0;waited=0;done=false;break;}
                    String want=PAGES.get(page);
                    if(waited==0){c.setScreen(null);server(c,b->GravesAndGlory.openCamp(b.server.getPlayerManager().getPlayer(c.player.getUuid()),want));waited=1;}
                    else if(++waited>14){
                        var s=c.currentScreen;boolean ok=want.equals("army")?s instanceof GravesAndGloryClient.LobbyScreen:s instanceof CampScreen cs&&cs.page.equals(want);
                        if(!ok)pagesBad.append("openCamp(\"").append(want).append("\") opened ").append(s==null?"no screen":s.getClass().getSimpleName()+(s instanceof CampScreen cs2?" on page "+cs2.page:"")).append("; ");
                        page++;waited=0;
                    }
                }
                case 4->{
                    if(pad>=2){
                        report("p3-camp-pads",padsOk,padsWhy.toString());server(c,b->b.campaign.toLobby(b.server.getPlayerManager().getPlayer(c.player.getUuid())));stage=5;waited=0;break;}
                    int team=pad;
                    if(waited==0){server(c,b->{var v=CampaignMaps.padCenter(team);b.relocate(b.server.getPlayerManager().getPlayer(c.player.getUuid()),v.x,v.y,v.z,0,0);});waited=1;}
                    else if(++waited==42){done=false;server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var on=b.campaign.onPad.get(p.getUuid());var sq=b.squads.get(p.getUuid());
                        boolean ok=on!=null&&on==team&&sq!=null&&sq.team==team;if(!ok)padsOk=false;
                        padsWhy.append("pad ").append(team).append(" at ").append(CampaignMaps.padCenter(team)).append(": on pad ").append(on).append(", squad team ").append(sq==null?"none":sq.team).append(ok?" ok; ":" WRONG; ");done=true;});}
                    else if(waited>42&&done){pad++;waited=0;}
                }
                case 5->{if(++waited>=20){stage=6;leashCase=0;waited=0;}} // toLobby of the pads stage settles: the squad steps off, the countdown is gone
                case 6->{
                    if(leashCase>=3){report("p3-camp-leash",leashOk,leashWhy.toString());stage=7;shotIndex=0;break;}
                    int k=leashCase;double[] at=k==0?new double[]{0.5,66,-250}:k==1?new double[]{-95.5,90,-310}:new double[]{0.5,60,-300};
                    if(waited==0){server(c,b->{b.campaign.leash=true;b.relocate(b.server.getPlayerManager().getPlayer(c.player.getUuid()),at[0],at[1],at[2],0,0);});waited=1;}
                    else if(++waited==41){done=false;server(c,b->{var p=b.server.getPlayerManager().getPlayer(c.player.getUuid());var s=CampWorld.spawn(b.campaign.profile(p.getUuid()));
                        boolean atSpawn=Math.abs(p.getX()-s.x())<0.5&&Math.abs(p.getZ()-s.z())<0.5&&Math.abs(p.getY()-s.y())<1.5;boolean atPlace=Math.hypot(p.getX()-at[0],p.getZ()-at[2])<1.5;
                        boolean ok=k==1?atPlace&&!atSpawn:atSpawn;if(!ok)leashOk=false;
                        leashWhy.append("case ").append(k).append(" from (").append(at[0]).append(",").append(at[1]).append(",").append(at[2]).append("): now at (").append(String.format("%.1f,%.1f,%.1f",p.getX(),p.getY(),p.getZ())).append(k==1?") expected to stay":") expected back at spawn").append(ok?" ok; ":" WRONG; ");done=true;});}
                    else if(waited>41&&done){leashCase++;waited=0;}
                }
                case 7->{
                    if(shotIndex>=VIEWS.length){stage=8;waited=0;done=false;break;}
                    var v=VIEWS[shotIndex];if(view(c,VIEW_NAMES[shotIndex],v[0],v[1],v[2],v[3],v[4],v[5]))shotIndex++;
                }
                case 8->{ // every map in turn (map 0 stands): the seam of the rim on each, and on map 2 (Frostmarch) the war table built over the camp
                    int n=SEAM_MAPS[mapIx];
                    if(sub==0){
                        done=false;waited=0;
                        if(n==0&&mapIx==0){server(c,b->{checkMap(b,0,false);done=true;});sub=2;}
                        else{server(c,b->{b.campaign.leash=true;if(b.campaign.map!=n)b.campaign.switchMap(n);done=true;});sub=1;}
                    }else if(sub==1){
                        var b=GravesAndGlory.battle;
                        if(done&&b!=null&&b.ready&&b.campaign.map==n&&CampaignMaps.selected==n){done=false;server(c,x->{checkMap(x,n,true);done=true;});sub=2;}
                        else if(++waited>3000){seamOk=false;seamWhy.append("map ").append(n).append(" was not ready after ").append(waited).append(" ticks; ");if(n==2)report("p3-camp-war-table",false,"map 2 was not ready after "+waited+" ticks");sub=3;}
                    }else if(sub==2){if(done)sub=n==2?4:3;}
                    else if(sub==4){ // Frostmarch stands: the same photographs of the south as on map 0
                        if(frostIx>=FROST_VIEWS.length)sub=3;
                        else{int k=FROST_VIEWS[frostIx];var v=VIEWS[k];if(view(c,"camp-map2-"+VIEW_NAMES[k].substring(5),v[0],v[1],v[2],v[3],v[4],v[5]))frostIx++;}
                    }
                    if(sub==3){
                        mapIx++;sub=0;waited=0;
                        if(mapIx>=SEAM_MAPS.length){report("p3-camp-rim-seam",seamOk,seamWhy.toString());stage=9;done=true;}
                    }
                }
                case 9->{if(done){done=false;server(c,b->{oldGone(b);done=true;});stage=10;}}
                case 10->{if(done){done=false;server(c,b->{idempotent(b);done=true;});stage=11;}}
                case 11->{if(done){finish(c,"camp");stage=12;}}
                default->{}
            }
        }

        /** p3-camp-spawn: after toLobby the player is within 0.1 of the camp spawn, on a block with a collision shape, with air at his feet and head. */
        void spawn(GravesAndGlory.Battle b,UUID me){
            var p=b.server.getPlayerManager().getPlayer(me);
            var prof=b.campaign.profile(me);int was=prof.campaignCleared;prof.campaignCleared=Math.max(1,was); // the normal spawn: with no battle cleared toLobby sends him to the yard (suite yard checks that arrival)
            try{b.campaign.toLobby(p);}finally{prof.campaignCleared=was;}
            var feet=p.getBlockPos();var below=feet.down();var w=b.world;
            boolean there=Math.abs(p.getX()-0.5)<0.1&&Math.abs(p.getY()-65)<0.1&&Math.abs(p.getZ()+291.5)<0.1&&Math.abs(p.getYaw()-180)<0.5;
            boolean floor=!w.getBlockState(below).getCollisionShape(w,below).isEmpty(),air=w.getBlockState(feet).isAir()&&w.getBlockState(feet.up()).isAir();
            report("p3-camp-spawn",there&&floor&&air,"at ("+p.getX()+","+p.getY()+","+p.getZ()+") yaw "+p.getYaw()+" (want 0.5,65,-291.5 yaw 180); block below "+name(w.getBlockState(below))+" has a collision shape "+floor+"; feet "+name(w.getBlockState(feet))+" and head "+name(w.getBlockState(feet.up()))+" are air "+air);
        }
        /** p3-camp-built: one Structures.LOG entry per campPieces row, the file there, blocks above 0. */
        void built(GravesAndGlory.Battle b){
            var bad=new StringBuilder();var info=new StringBuilder();
            for(var row:Spec.CAMPPIECES){
                var want=box(row);Structures.Placed hit=null;
                for(var p:Structures.LOG)if(p.id().equals(row.template())&&same(p.box(),want))hit=p;
                if(hit==null)bad.append(row.id()).append(" has no log entry for ").append(row.template()).append(" at ").append(want.getMinX()).append(",").append(want.getMinZ()).append("; ");
                else{if(hit.missing())bad.append(row.id()).append(" is missing its file; ");else if(hit.blocks()<=0)bad.append(row.id()).append(" placed no blocks; ");info.append(row.id()).append(" ").append(hit.blocks()).append(" blocks ").append(hit.nanos()/1000000).append(" ms, ");}
            }
            report("p3-camp-built",bad.length()==0,bad.length()==0?"all "+Spec.CAMPPIECES.length+" rows placed: "+info:bad.toString());
        }
        /** p3-camp-budget: the terrain at most 6000 ms and the pieces at most 1500 ms, as the first build of the camp logged them. It passes only on a measurement: a world that already holds the camp terrain
         *  (a root that was run before) logs "camp terrain skipped", there is nothing to time, and the check fails and says so; run the suite on a fresh root.
         *  If it flickers on a loaded machine, look at the chunk preload of CampWorld.terrain first (it is what took the first build from 7573 ms to about 1.5 s), not at the limit. */
        void budget(){
            long terrain=millis("terrain"),pieces=millis("pieces");
            String time="pieces "+pieces+" ms (limit 1500)";
            if(terrain<0){report("p3-camp-budget",false,"not measured: "+(skipped("terrain")?"the world already had the camp terrain; run on a fresh root":"no camp terrain line was logged")+"; "+time);return;}
            report("p3-camp-budget",terrain<=6000&&pieces>=0&&pieces<=1500,"terrain "+terrain+" ms (limit 6000), "+time);
        }
        /** p3-camp-stations: per campStations row the villager (within 0.5, tagged camp:page, NoAI, feet and head clear, a floor under him) and the label at y+3.7, and CampWorld.station at the row's block answers the page. */
        void stations(GravesAndGlory.Battle b){
            var w=b.world;var bad=new StringBuilder();int npcs=0,labels=0;
            for(var s:Spec.CAMPSTATIONS){
                Vec3d at=pos(s);var cell=BlockPos.ofFloored(at);
                if(!s.npc().equals("-")){
                    var near=w.getEntitiesByClass(net.minecraft.entity.passive.VillagerEntity.class,new Box(at,at).expand(0.5),v->v.getPos().distanceTo(at)<=0.5);
                    if(near.size()!=1)bad.append(s.id()).append(": ").append(near.size()).append(" villagers within 0.5 of ").append(at).append("; ");
                    else{var v=near.get(0);npcs++;
                        if(!v.getCommandTags().contains("camp:"+s.page())||!v.getCommandTags().contains(CampWorld.TAG))bad.append(s.id()).append(": tags ").append(v.getCommandTags()).append("; ");
                        if(!v.isAiDisabled())bad.append(s.id()).append(": AI is on; ");
                        boolean floor=!w.getBlockState(cell.down()).getCollisionShape(w,cell.down()).isEmpty(),clear=w.getBlockState(cell).getCollisionShape(w,cell).isEmpty()&&w.getBlockState(cell.up()).getCollisionShape(w,cell.up()).isEmpty();
                        if(!floor||!clear)bad.append(s.id()).append(": not standing on a floor with feet and head clear at ").append(xyz(cell)).append("; ");}
                }
                Vec3d lab=at.add(0,3.7,0);
                var shown=w.getEntitiesByClass(net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity.class,new Box(lab,lab).expand(0.5),d->d.getPos().distanceTo(lab)<=0.5);
                if(shown.size()!=1)bad.append(s.id()).append(": ").append(shown.size()).append(" labels within 0.5 of ").append(lab).append("; ");
                else{labels++;if(!shown.get(0).getCommandTags().contains("camp:"+s.page()))bad.append(s.id()).append(": label tags ").append(shown.get(0).getCommandTags()).append("; ");}
                String got=CampWorld.station(cell);if(!s.page().equals(got))bad.append(s.id()).append(": CampWorld.station").append(xyz(cell)).append(" is ").append(got).append(" not ").append(s.page()).append("; ");
            }
            report("p3-camp-stations",bad.length()==0,bad.length()==0?npcs+" villagers and "+labels+" labels where the "+Spec.CAMPSTATIONS.length+" rows put them, and station() answers each page":bad.toString());
        }
        /** The war table of the map that stands: every cell holds tableCell(i,j). Returns the description of what is wrong, empty when right. */
        static String tableState(ServerWorld w,boolean[] rows,int[] lit){
            var bad=new StringBuilder();int wrong=0;
            for(int i=0;i<15;i++)for(int j=0;j<9;j++){
                var pos=new BlockPos(C.originX()+C.tableDx()+i,C.tableY(),C.originZ()+C.tableDz()+j);var got=w.getBlockState(pos);var want=CampWorld.tableCell(i,j);
                if(got!=want){wrong++;if(wrong<=4)bad.append("cell ").append(i).append(",").append(j).append(" holds ").append(got).append(" not ").append(want).append("; ");}
                if(got.isOf(Blocks.CYAN_CARPET)||got.isOf(Blocks.LIGHT_GRAY_CARPET))rows[j]=true;
                if(got.isOf(Blocks.CANDLE)&&got.get(net.minecraft.block.CandleBlock.LIT))lit[0]++;
            }
            if(wrong>4)bad.append(wrong).append(" cells in all differ; ");
            return bad.toString();
        }
        /** p3-camp-war-table, first part: map 0 stands (built at the start of the world) and every cell holds tableCell; cell (7,4) is the lit candle of point B and each row of the table has a river or a bridge. */
        void tableMap0(GravesAndGlory.Battle b){
            var rows=new boolean[9];var lit=new int[1];String bad=tableState(b.world,rows,lit);
            var seven=b.world.getBlockState(new BlockPos(C.originX()+C.tableDx()+7,C.tableY(),C.originZ()+C.tableDz()+4));
            boolean centre=seven.isOf(Blocks.CANDLE)&&seven.get(net.minecraft.block.CandleBlock.LIT);int rowsWith=0;for(int j=0;j<9;j++)if(j!=4&&rows[j])rowsWith++;
            part1=bad.isEmpty()&&centre&&rowsWith==8&&CampaignMaps.selected==0&&lit[0]==3;
            part1Why="map "+CampaignMaps.selected+": "+(bad.isEmpty()?"every cell holds tableCell":bad)+", cell (7,4) is "+seven+", rows other than row 4 (the candle of point B stands on the river there) with river or bridge "+rowsWith+" of 8, lit candles "+lit[0]+" (want 3: points A, B, C)";
        }
        boolean part1;String part1Why="";
        /** p3-camp-war-table, second part: map 2 (Frostmarch) was built over the camp; every cell holds tableCell and none is green. */
        void tableMap2(GravesAndGlory.Battle b){
            var rows=new boolean[9];var lit=new int[1];String bad=tableState(b.world,rows,lit);int green=0;
            for(int i=0;i<15;i++)for(int j=0;j<9;j++)if(b.world.getBlockState(new BlockPos(C.originX()+C.tableDx()+i,C.tableY(),C.originZ()+C.tableDz()+j)).isOf(Blocks.GREEN_CARPET))green++;
            report("p3-camp-war-table",part1&&bad.isEmpty()&&green==0&&CampaignMaps.selected==2,part1Why+" | map "+CampaignMaps.selected+": "+(bad.isEmpty()?"every cell holds tableCell":bad)+", green carpets "+green+" (want 0), lit candles "+lit[0]);
        }
        /** p3-camp-bank-mix and p3-camp-flank-relief, on the terrain as built: the bank of the ridge (the 12 rows between the foot and the plateau, the columns outside the stair pieces and one block round them) is not banded: in at least 9 of the 12 rows no one block
         *  (grass) is more than 85 percent of the columns; and the crest of each flank (x +-90 from the origin, the 67 rows from 36 before the origin to 30 after it, where the ground is level but for its relief) is not one plane: it ranges over at least 6 blocks. */
        void look(GravesAndGlory.Battle b){
            var w=b.world;int ox=C.originX(),oz=C.originZ();var boxes=new ArrayList<BlockBox>();for(var row:Spec.CAMPPIECES)boxes.add(box(row));
            int mixed=0,rows=0;var shares=new StringBuilder();
            for(int dz=CampWorld.RIDGE_TOP_DZ+1;dz<CampWorld.RIDGE_FOOT_DZ;dz++){
                var count=new TreeMap<String,Integer>();int n=0;
                for(int dx=C.minDx()+2;dx<=C.maxDx()-2;dx++){
                    int x=ox+dx,z=oz+dz;boolean near=false;for(var bx:boxes)if(x>=bx.getMinX()-1&&x<=bx.getMaxX()+1&&z>=bx.getMinZ()-1&&z<=bx.getMaxZ()+1)near=true;
                    if(near)continue;
                    count.merge(name(w.getBlockState(new BlockPos(x,ground(w,x,z),z))),1,Integer::sum);n++;
                }
                int top=0;String which="";for(var e:count.entrySet())if(e.getValue()>top){top=e.getValue();which=e.getKey();}
                rows++;boolean ok=n>0&&top*100<=85*n;if(ok)mixed++;
                shares.append(dz).append(':').append(n==0?"-":(100*top/n)+"% "+which).append(ok?"" :" (banded)").append(' ');
            }
            report("p3-camp-bank-mix",mixed>=9,mixed+" of the "+rows+" bank rows are mixed (no block above 85 percent of the columns outside the stairs); the biggest share per row (dz): "+shares);
            var ranges=new StringBuilder();boolean level=true;
            for(int side:new int[]{-1,1}){
                int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
                for(int dz=-36;dz<=30;dz++){int y=ground(w,ox+side*90,oz+dz);lo=Math.min(lo,y);hi=Math.max(hi,y);}
                if(hi-lo<6)level=false;
                ranges.append(side<0?"west":"east").append(" crest y ").append(lo).append(" to ").append(hi).append(" (range ").append(hi-lo).append("); ");
            }
            report("p3-camp-flank-relief",level,ranges+"want a range of at least 6 on each (the crest was level to within 1 before the relief)");
        }
        /** What a map stands on in this seam check: the checks of one built map (the rim seam, on map 2 the war table after it, and when GRAVES_BOX_DUMP is set and the map was built after the rule was set, the dump of the playable box). */
        void checkMap(GravesAndGlory.Battle b,int n,boolean dumped){
            rimSeam(b,n);
            if(n==2)tableMap2(b);
            if(dumped)dump(b,n);
        }
        /** The ground of a column for the seam check: the highest block that is not air, not a leaf or a log and has a collision shape (plants, flowers and the crowns of trees do not count). */
        static int ground(ServerWorld w,int x,int z){
            var pos=new BlockPos.Mutable();
            for(int y=150;y>=40;y--){var s=w.getBlockState(pos.set(x,y,z));if(s.isAir()||s.getBlock() instanceof net.minecraft.block.LeavesBlock||s.isIn(net.minecraft.registry.tag.BlockTags.LOGS)||s.getCollisionShape(w,pos).isEmpty())continue;return y;}
            return -1;
        }
        /** p3-camp-rim-seam (one reading per map; the id is reported after the last map): over the width of the island (x -96 to 96), for z -270 to -262 (the last rim rows of the camp, z -267, and the first arena rows, from z -266 on), the ground at z -267
         *  is at least the ground at z -266 (the arena's edge never stands above the rim: no cut face), and no neighbouring pair of columns, along z or along x, differs by more than 3 blocks. A column in which ground() finds nothing (-1: the world was not read) is wrong, so the check cannot pass on an unread world. */
        void rimSeam(GravesAndGlory.Battle b,int n){
            var w=b.world;int x0=C.originX()+C.islandMinDx(),x1=C.originX()+C.islandMaxDx(),zRim=-267,zEdge=-266,zLo=-270,zHi=-262;
            int cols=x1-x0+1,rows=zHi-zLo+1;int[][] top=new int[cols][rows];
            for(int i=0;i<cols;i++)for(int k=0;k<rows;k++)top[i][k]=ground(w,x0+i,zLo+k);
            int above=0,worstAbove=0,firstAbove=Integer.MIN_VALUE,steps=0,worstStep=0,unread=0;String firstStep="",firstUnread="";
            for(int i=0;i<cols;i++){
                int rim=top[i][zRim-zLo],edge=top[i][zEdge-zLo];
                boolean bad=false;
                for(int k=0;k<rows;k++)if(top[i][k]<=0){bad=true;if(firstUnread.isEmpty())firstUnread="x "+(x0+i)+" z "+(zLo+k);} // ground() answers -1 when it finds nothing from y 150 to 40: a world that did not load reads -1 everywhere, and every difference of -1 and -1 is 0
                if(bad)unread++;
                if(edge>rim){above++;if(edge-rim>worstAbove){worstAbove=edge-rim;firstAbove=x0+i;}}
                for(int k=0;k<rows;k++){
                    if(k+1<rows){int d=Math.abs(top[i][k]-top[i][k+1]);if(d>3){steps++;if(firstStep.isEmpty())firstStep="z "+(zLo+k)+" to "+(zLo+k+1)+" at x "+(x0+i)+": "+top[i][k]+" to "+top[i][k+1];}worstStep=Math.max(worstStep,d);}
                    if(i+1<cols){int d=Math.abs(top[i][k]-top[i+1][k]);if(d>3){steps++;if(firstStep.isEmpty())firstStep="x "+(x0+i)+" to "+(x0+i+1)+" at z "+(zLo+k)+": "+top[i][k]+" to "+top[i+1][k];}worstStep=Math.max(worstStep,d);}
                }
            }
            boolean ok=above==0&&steps==0&&unread==0;if(!ok)seamOk=false;
            seamWhy.append("map ").append(n).append(": ").append(cols).append(" columns, edge (z -266) above the rim (z -267) in ").append(above).append(worstAbove>0?" (worst by "+worstAbove+" at x "+firstAbove+")":"")
                .append(", neighbour steps over 3: ").append(steps).append(" (largest step ").append(worstStep).append(steps>0?", first "+firstStep:"")
                .append("; unread columns, where no ground was found from y 150 to 40, count as WRONG: ").append(unread).append(unread>0?", first at "+firstUnread:"").append(ok?") ok; ":") WRONG; ");
        }
        /** When the environment variable GRAVES_BOX_DUMP names a folder: the state of every block of the playable box (x -366 to 366, z -250 to 250, y 50 to 140) as 16-bit ids, gzipped, in box-map<N>.bin.gz. Two runs of different code are compared with work/p3/tools/boxdiff.py; the random ticks are stopped (game rule randomTickSpeed 0) when the dump is on, so that no grass spreads and no farmland dries between two runs. */
        void dump(GravesAndGlory.Battle b,int n){
            String dir=System.getenv("GRAVES_BOX_DUMP");if(dir==null||dir.isEmpty())return;
            try{
                Files.createDirectories(Path.of(dir));var path=Path.of(dir,"box-map"+n+".bin.gz");var pos=new BlockPos.Mutable();var w=b.world;long cells=0;var names=new TreeMap<Integer,String>();
                try(var out=new java.io.DataOutputStream(new java.io.BufferedOutputStream(new java.util.zip.GZIPOutputStream(Files.newOutputStream(path),1<<16),1<<16))){
                    for(int x=-366;x<=366;x++)for(int z=-250;z<=250;z++)for(int y=50;y<=140;y++){var st=w.getBlockState(pos.set(x,y,z));int id=Block.getRawIdFromState(st);out.writeShort(id);cells++;if(!names.containsKey(id))names.put(id,st.toString());}
                }
                var list=new StringBuilder();for(var e:names.entrySet())list.append(e.getKey()).append(' ').append(e.getValue()).append((char)10);
                Files.writeString(Path.of(dir,"box-map"+n+".ids.txt"),list.toString());
                System.out.println("[p3] note box dump of map "+n+": "+cells+" cells in "+path);
            }catch(Exception e){report("p3-camp-rim-seam",false,"box dump of map "+n+" failed: "+e);}
        }
        /** p3-camp-shapes (extra): inside every piece box each fence, wall, pane and bar equals what the shape update of its neighbours gives, and each vine of the structure file stands. */
        void shapes(GravesAndGlory.Battle b){
            var w=b.world;int connecting=0,unsettled=0,vinesFile=0,vinesLost=0;var first=new StringBuilder();
            for(var row:Spec.CAMPPIECES){
                var box=box(row);var pos=new BlockPos.Mutable();
                for(int x=box.getMinX();x<=box.getMaxX();x++)for(int y=box.getMinY();y<=box.getMaxY();y++)for(int z=box.getMinZ();z<=box.getMaxZ();z++){
                    pos.set(x,y,z);var s=w.getBlockState(pos);var blk=s.getBlock();
                    if(blk instanceof net.minecraft.block.HorizontalConnectingBlock||blk instanceof net.minecraft.block.WallBlock){connecting++;var n=Block.postProcessState(s,w,pos);if(n!=s&&!n.isAir()){unsettled++;if(first.length()<300)first.append(row.id()).append(" ").append(xyz(pos)).append(" ").append(s).append(" settles to ").append(n).append("; ");}}
                }
                var nbt=Structures.read(b.server,row.template());if(nbt.isEmpty()){vinesLost++;continue;}
                var palette=nbt.get().getList("palette",NbtElement.COMPOUND_TYPE);var size=new Vec3i(piece(row.template()).width(),piece(row.template()).height(),piece(row.template()).depth());
                for(var e:nbt.get().getList("blocks",NbtElement.COMPOUND_TYPE)){var cell=(NbtCompound)e;if(!palette.getCompound(cell.getInt("state")).getString("Name").equals("minecraft:vine"))continue;
                    var at=cell.getList("pos",NbtElement.INT_TYPE);var world=Structures.toWorld(size,C.originX()+row.dx(),row.y(),C.originZ()+row.dz(),row.rotation(),at.getInt(0),at.getInt(1),at.getInt(2));
                    vinesFile++;if(!w.getBlockState(world).isOf(Blocks.VINE)){vinesLost++;if(first.length()<300)first.append("vine of ").append(row.id()).append(" gone at ").append(xyz(world)).append("; ");}}
            }
            boolean spreading=w.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_VINES_SPREAD);
            report("p3-camp-shapes",unsettled==0&&vinesLost==0&&connecting>0&&!spreading,connecting+" fence, wall, pane and bar blocks in the pieces, "+unsettled+" not settled; "+vinesFile+" vines in the files, "+vinesLost+" missing in the world; game rule doVinesSpread "+spreading+" (want false, so the vines do not grow into the cells the pieces leave void)"+(first.length()==0?"":": "+first));
        }
        static int tagged(ServerWorld w){int n=0;for(var e:w.iterateEntities())if(e.getCommandTags().contains(CampWorld.TAG))n++;return n;}
        /** CRC32 of the block states in the bowl box, y 62 to 90. */
        static long crc(ServerWorld w){
            var crc=new java.util.zip.CRC32();var pos=new BlockPos.Mutable();byte[] four=new byte[4];
            for(int x=C.originX()+C.minDx();x<=C.originX()+C.maxDx();x++)for(int z=C.originZ()+C.minDz();z<=C.originZ()+C.maxDz();z++)for(int y=62;y<=90;y++){
                int id=Block.getRawIdFromState(w.getBlockState(pos.set(x,y,z)));four[0]=(byte)id;four[1]=(byte)(id>>8);four[2]=(byte)(id>>16);four[3]=(byte)(id>>24);crc.update(four);}
            return crc.getValue();
        }
        /** p3-camp-old-gone: what an older build left (entities of the three older tags, the concrete pads, the gold seal, a brick of the old tower) is planted, the terrain marker is deleted, and CampWorld.build runs as on the first load of such a world. */
        void oldGone(GravesAndGlory.Battle b){
            var w=b.world;int planted=0;
            // The tag goes on after spawnEntity: ENTITY_LOAD (GravesAndGlory) has run by then and discards nothing, so only people() of the build below can take these three away.
            for(String tag:new String[]{"gravesandglory-camp-v3","gravesandglory-lobby","bannerbound-lobby"}){var e=EntityType.ARMOR_STAND.create(w);e.refreshPositionAndAngles(0.5+planted*3,65,-300.5,0,0);w.spawnEntity(e);e.addCommandTag(tag);planted++;}
            int standing=0;for(var e:w.iterateEntities()){var t=e.getCommandTags();if(t.contains("gravesandglory-camp-v3")||t.contains("gravesandglory-lobby")||t.contains("bannerbound-lobby"))standing++;}
            int oz=C.originZ();
            for(int x:new int[]{-11,-10,-9,-8,8,9,10,11})for(int z=oz-14;z<=oz-11;z++)w.setBlockState(new BlockPos(x,64,z),Blocks.CYAN_CONCRETE.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            w.setBlockState(new BlockPos(5,64,oz),Blocks.GOLD_BLOCK.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            w.setBlockState(new BlockPos(-39,70,-339),Blocks.STONE_BRICKS.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            boolean deleted=false;try{deleted=Files.deleteIfExists(b.server.getSavePath(WorldSavePath.ROOT).resolve(CampWorld.MARKER_FILE));}catch(Exception e){report("p3-camp-old-gone",false,"cannot delete the terrain marker: "+e);return;}
            long mark=CampWorld.logged();CampWorld.build(w);boolean rebuilt=false;for(var l:CampWorld.since(mark))if(l.contains("camp terrain ")&&!l.contains("skipped"))rebuilt=true; // a count of lines ever logged: an index into LINES would be wrong once the oldest lines were dropped
            int stale=0;for(var e:w.iterateEntities()){var t=e.getCommandTags();if(t.contains("gravesandglory-camp-v3")||t.contains("gravesandglory-lobby")||t.contains("bannerbound-lobby"))stale++;}
            int concrete=0;for(int x:new int[]{-11,-10,-9,-8,8,9,10,11})for(int z=oz-14;z<=oz-11;z++)if(name(w.getBlockState(new BlockPos(x,64,z))).endsWith("concrete"))concrete++;
            boolean gold=w.getBlockState(new BlockPos(5,64,oz)).isOf(Blocks.GOLD_BLOCK),brick=!w.getBlockState(new BlockPos(-39,70,-339)).isAir();
            int villagers=0;for(var e:w.iterateEntities())if(e.getCommandTags().contains(CampWorld.TAG)&&e instanceof net.minecraft.entity.passive.VillagerEntity)villagers++;
            int want=0;for(var s:Spec.CAMPSTATIONS)if(!s.npc().equals("-"))want++;
            report("p3-camp-old-gone",standing==planted&&deleted&&rebuilt&&stale==0&&concrete==0&&!gold&&!brick&&villagers==want,"planted "+planted+" old entities, "+standing+" of them standing before the build, 32 concrete cells, a gold block and a brick (marker deleted "+deleted+", terrain rebuilt "+rebuilt+"); afterwards entities of the older tags "+stale+", concrete cells "+concrete+", gold block "+gold+", brick of the old tower "+brick+", camp villagers "+villagers+" (want "+want+")");
        }
        /** p3-camp-rebuild-idempotent: a second build logs camp terrain skipped; the CRC32 of the bowl box and the count of TAG entities are the same before and after. Both readings and the build are one callback. */
        void idempotent(GravesAndGlory.Battle b){
            var w=b.world;long mark=CampWorld.logged();
            long before=crc(w);int n0=tagged(w);CampWorld.build(w);long after=crc(w);int n1=tagged(w);
            boolean skip=false;for(var l:CampWorld.since(mark))if(l.contains("camp terrain skipped"))skip=true;
            report("p3-camp-rebuild-idempotent",skip&&before==after&&n0==n1&&n0>0,"second build logged terrain skipped "+skip+"; CRC32 of the bowl box (y 62..90) "+Long.toHexString(before)+" then "+Long.toHexString(after)+"; entities tagged "+CampWorld.TAG+" "+n0+" then "+n1+"; both readings and the build in one server callback");
        }
    }

    /** The arena build (suite arena). The maps 0 to 4 are built one over the other on one world, and after each finish the world is looked at.
     *  The 0.4.0 soak, Finding 1: an arena rebuilt over an arena spilled loose items (a plant, lantern or crop whose support the build had cleared first).
     *  The item entities loaded in the built area are counted after map 0 and after map 1 was built over it: p3-arena-items passes when the second count is 0
     *  (the safety net in Arena.finish discards what is loaded); p3-arena-spill passes when the second finish found nothing to discard, that is when the build itself spilled none
     *  (it writes without neighbour shape updates); p3-arena-shapes passes when no glass pane of the structures is left without a connection. All three see the chunks that are loaded,
     *  and the shapes check counts glass panes only (fences and walls are not looked at).
     *  Task 2, fix round 1, per map: p3-arena-river-map<N> passes when no block stands above the water of a river column that the build of that map does not account for, which is what
     *  the map built before it leaves behind (river); p3-arena-rails-map<N> passes when every bridge rail stands where there is water and every run of stone wall ends on a post (rails).
     *  Task 2, fix round 2, per map, by built() (the walkthrough runs the same as p3-wt<N>-ring and so on): p3-arena-ring-map<N> passes when the ring of smooth stone of every capture point lies over no river water, no deck and no air, and its 3x3 pad has no air under it (ring);
     *  p3-arena-lantern-map<N> when every lantern could be placed where it stands (lantern); p3-arena-floors-map<N> when no floor course of a building holds road soil (floors); p3-arena-pits-map<N> when no dirt path block stands alone (no dirt path on any of its four sides, anywhere in the arena) or inside a plaza or on a parade ground, and no coarse dirt or gravel lies on the ground off the plazas (pits);
     *  p3-arena-plaza-map<N> when every cell of every plaza is the block of its pattern (plaza).
     *  Task 2, fix round 3: p3-arena-wear-map<N> passes when the worn band (dirt path, one block, along every road edge) and the worn ring (one block outside the edge stone of every point whose stone ring is laid) have no gap where MapDressing.wearable says plain ground may be worn (wear). */
    static final class Rebuild implements Suite {
        static final int[] MAPS={0,1,2,3,4};
        int index,stage,waited;volatile boolean switched,counted;volatile String before="",after="";volatile int firstSpill=-1,spill=-1,paneCount=-1,paneLone=-1;
        /** Active ticks only: nothing steps while the client is told the arena is not ready, so the builds are not in it. */
        @Override public int limit(){return 6000;}
        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            var b=GravesAndGlory.battle;
            switch(stage){
                case 0->{int n=MAPS[index];switched=false;server(c,x->{if(x.campaign.map!=n)x.campaign.switchMap(n);switched=true;});stage=1;waited=0;}
                case 1->{
                    int n=MAPS[index],at=index;
                    if(switched&&b!=null&&b.ready&&b.campaign.map==n&&CampaignMaps.selected==n){counted=false;server(c,x->{measure(x,n,at);counted=true;});stage=2;}
                    else if(++waited>3000){report("p3-arena-river-map"+n,false,"map "+n+" was not ready after "+waited+" ticks");stage=3;}
                }
                case 2->{if(counted){index++;stage=index<MAPS.length?0:3;}}
                case 3->{
                    report("p3-arena-items",after.startsWith("0 "),"item entities loaded in the built area: "+before+" after the first build (map 0); "+after+" after map 1 was built over it");
                    report("p3-arena-spill",spill==0,"loose items the safety net found and discarded: "+firstSpill+" at the first finish (map 0), "+spill+" at the second (map 1)");
                    report("p3-arena-shapes",paneCount>0&&paneLone==0,"glass panes inside the structures after map 1 was built over map 0: "+paneCount+", with no connection: "+paneLone);
                    finish(c,"arena");stage=4;
                }
                default->{}
            }
        }
        /** On the server thread, with the map that stands (the at-th of MAPS): the item and pane counts of the soak check after maps 0 and 1, then the river and the rails of this map. */
        void measure(GravesAndGlory.Battle x,int n,int at){
            if(at==0){before=items(x);firstSpill=Arena.discarded;}
            if(at==1){after=items(x);spill=Arena.discarded;var pn=panes(x);paneCount=pn[0];paneLone=pn[1];}
            var r=river(x);
            report("p3-arena-river-map"+n,r.left()==0,"river columns "+r.columns()+", built on by this map "+r.builtOn()+", looked at "+r.looked()+" (of them "+r.flat()+" with the ground at "+Arena.Y+" or above): "+r.left()+" blocks above the water that this map does not account for"+(r.left()==0?"":", first: "+r.sample()));
            try{Path file=MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots/p3-arena-river-map"+n+".txt");Files.createDirectories(file.getParent());Files.writeString(file,r.detail());}
            catch(Exception e){report("p3-arena-river-map"+n+"-file",false,e.toString());}
            var ra=rails(x);
            report("p3-arena-rails-map"+n,ra[0]>0&&ra[1]==0&&ra[2]==0,"bridge rail cells "+ra[0]+", on dry ground or on a building floor "+ra[1]+", ends of stone wall runs without a post "+ra[2]);
            built(x,n,k->"p3-arena-"+k+"-map"+n);
        }
        /** Task 2, fix round 2: what the finish stage and the dressing leave standing, looked at on the map that stands, by one reporter for the arena suite and the walkthrough (name turns a short name into the id of the check). Server thread. */
        static void built(GravesAndGlory.Battle b,int n,java.util.function.Function<String,String> name){
            var ri=ring(b);
            report(name.apply("ring"),ri.ok(),ri.text());
            var la=lanterns(b);
            try{Path file=MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots/p3-lanterns-map"+n+".txt");Files.createDirectories(file.getParent());Files.writeString(file,la.detail());}
            catch(Exception e){report(name.apply("lantern-file"),false,e.toString());}
            report(name.apply("lantern"),la.total()>0&&la.loose()==0,"lanterns "+la.total()+", without a block to hang from or to stand on (the test LanternBlock.canPlaceAt makes) "+la.loose()+(la.loose()==0?"":", first: "+la.sample()));
            var fl=floors(b);
            report(name.apply("floors"),fl.cells()>0&&fl.soil()==0,"floor cells of "+fl.halls()+" buildings "+fl.cells()+", with road soil (dirt path, coarse dirt, gravel) or ground on them "+fl.soil()+(fl.soil()==0?"":": "+fl.sample()));
            var pi=pits(b);
            try{Path file=MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots/p3-pits-map"+n+".txt");Files.createDirectories(file.getParent());Files.writeString(file,pi.detail());}
            catch(Exception e){report(name.apply("pits-file"),false,e.toString());}
            report(name.apply("pits"),pi.paths()>0&&pi.paved()==0&&pi.lone()==0&&pi.earth()==0,"dirt path cells "+pi.paths()+", of them inside a plaza (within 9.2 blocks of a flag pole) or on the parade ground of a camp, where everything is paved, off the floors and decks: "+pi.paved()+(pi.paved()==0?"":", first "+pi.sample())
                +"; with no dirt path on any of their four sides (anywhere): "+pi.lone()+"; coarse dirt or gravel on the ground outside the plazas and off the river: "+pi.earth()+(pi.earth()==0?"":", first "+pi.earthSample())+"; lone cells the dressing's own guard put back as ground: "+MapDressing.despeckled);
            var we=wear(b);
            report(name.apply("wear"),we.verge()>0&&(we.ring()>0||we.rings()==0)&&we.gaps()==0,"worn band: dirt path cells that touch a road cell on a side "+we.verge()+", plain ground left beside a road that the band should have covered "+we.gapsRoad()+"; worn ring of the "+we.rings()+" laid rings no landmark encloses"+(we.rings()==0?" (none on this map: the outposts pave round theirs)":"")+": dirt path cells just outside the edge stone of a point "+we.ring()+", plain ground left in it "+we.gapsRing()+(we.gaps()==0?"":", first "+we.sample()));
            var pl=plazas(b,n);
            boolean noPlaza=Arrays.stream(Spec.POINTS).allMatch(p->enclosed(p)||Math.abs(p.x()-Arena.riverX(p.z()))<11);
            report(name.apply("plaza"),(pl.cells()>0||noPlaza)&&pl.wrong()==0,(noPlaza?"every point of this map is on the river or inside an outpost (Task 6), which paves it; ":"")+"plaza cells (within 9.2 blocks of a flag pole, off the 3x3 pad, the water and the buildings) "+pl.cells()+", not the block of the pattern (earth, a compass of trim, a ring of paving) "+pl.wrong()+(pl.wrong()==0?"":", first: "+pl.sample()));
        }
        /** The ring of every capture point: per point {ring cells, those over river water, those on a deck or its rails, those with air under, pad cells with air under}. A ring cell is smooth stone at the elevation of the point, radius-1 to radius out. */
        record Ring(boolean ok,String text){}
        static Ring ring(GravesAndGlory.Battle b){
            var w=b.world;boolean ok=true;int total=0;var text=new StringBuilder("per point: ring cells / over river water / on a deck / with air under / pad cells with air under: ");
            var other=new TreeMap<String,Integer>();
            for(var p:Spec.POINTS){
                int r=p.radius(),e=p.elevation(),cells=0,water=0,deck=0,air=0,pad=0;
                for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                    int x=p.x()+dx,z=p.z()+dz;double d=Math.hypot(dx,dz);
                    if(d>=r-1&&d<=r&&!w.getBlockState(new BlockPos(x,e,z)).isOf(Blocks.SMOOTH_STONE))other.merge(name(w.getBlockState(new BlockPos(x,e,z))),1,Integer::sum);
                    if(d>=r-1&&d<=r&&w.getBlockState(new BlockPos(x,e,z)).isOf(Blocks.SMOOTH_STONE)){
                        cells++;var under=w.getBlockState(new BlockPos(x,e-1,z));
                        if(under.isAir())air++;
                        if(Arena.river(x,z)&&under.isOf(Blocks.WATER))water++;
                        if(Walkthrough.deckCell(x,z))deck++;
                    }
                    if(Math.abs(dx)<=1&&Math.abs(dz)<=1&&w.getBlockState(new BlockPos(x,e-1,z)).isAir())pad++;
                }
                total+=cells;if(water!=0||deck!=0||air!=0||pad!=0)ok=false;
                text.append(p.id()).append(" (").append(p.x()).append(", ").append(p.z()).append(") elevation ").append(e).append(' ').append(cells).append('/').append(water).append('/').append(deck).append('/').append(air).append('/').append(pad).append("; ");
            }
            var top=new ArrayList<>(other.entrySet());top.sort((p,q)->q.getValue()-p.getValue());
            return new Ring(ok&&total>=100,text.toString()+"ring cells in all "+total+" (at least 100: a point on the river has no ring, the others do); what else lies on the cells of the ring band (the edge stone of the dressing covers the outer half): "+top.stream().limit(6).map(q->q.getKey()+" "+q.getValue()).toList());
        }
        /** Every lantern of the arena: how many there are and how many have nothing to hang from or stand on (LanternBlock.canPlaceAt is false: the next neighbour update would break it). */
        record Lanterns(int total,int loose,String sample,String detail){}
        static Lanterns lanterns(GravesAndGlory.Battle b){
            var w=b.world;int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),total=0,loose=0;var sample=new StringBuilder();var detail=new StringBuilder();var pos=new BlockPos.Mutable();
            for(int cx=Math.floorDiv(-hx,16);cx<=Math.floorDiv(hx,16);cx++)for(int cz=Math.floorDiv(-hz,16);cz<=Math.floorDiv(hz,16);cz++){
                var ch=w.getChunk(cx,cz);
                for(int x=Math.max(-hx,cx*16);x<=Math.min(hx,cx*16+15);x++)for(int z=Math.max(-hz,cz*16);z<=Math.min(hz,cz*16+15);z++)for(int y=Arena.Y-2;y<=Arena.Y+60;y++){
                    var st=ch.getBlockState(pos.set(x,y,z));
                    if(!(st.getBlock() instanceof net.minecraft.block.LanternBlock))continue;
                    total++;
                    if(!st.canPlaceAt(w,pos)){loose++;if(loose<=6)sample.append(loose>1?"; ":"").append(name(st)).append(' ').append(xyz(pos.toImmutable()));detail.append(name(st)).append(' ').append(x).append(' ').append(y).append(' ').append(z).append(" hanging ").append(st.get(net.minecraft.block.LanternBlock.HANGING)).append('\n');}
                }
            }
            return new Lanterns(total,loose,sample.toString(),detail.toString());
        }
        /** The floor course of every building that has one (the cells x0..x0+width-1 by z0..z0+depth-1 that Arena.structure lays at the elevation of the building): how many cells, and how many hold road soil
         *  or ground (dirt path, coarse dirt, gravel, dirt, grass, podzol, snow block), which the muster lanes of the camps once wrote over them (that pass is gone; the check keeps the floors clear of any road soil). */
        record Floors(int halls,int cells,int soil,String sample){}
        static Floors floors(GravesAndGlory.Battle b){
            var w=b.world;int halls=0,cells=0,soil=0;var sample=new StringBuilder();
            for(var s:Spec.STRUCTURES){
                if(!List.of("house","barracks","mill","ruin","watchtower","shrine","base").contains(s.kind()))continue;
                halls++;int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2,mine=0;
                for(int x=x0;x<x0+s.width();x++)for(int z=z0;z<z0+s.depth();z++){
                    cells++;var st=w.getBlockState(new BlockPos(x,s.elevation(),z));
                    if(st.isOf(Blocks.DIRT_PATH)||st.isOf(Blocks.COARSE_DIRT)||st.isOf(Blocks.GRAVEL)||st.isOf(Blocks.DIRT)||st.isOf(Blocks.GRASS_BLOCK)||st.isOf(Blocks.PODZOL)||st.isOf(Blocks.SNOW_BLOCK))mine++;
                }
                soil+=mine;if(mine>0)sample.append(sample.length()>0?"; ":"").append(s.id()).append(' ').append(mine);
            }
            return new Floors(halls,cells,soil,sample.toString());
        }
        /** The dirt path cells of the arena (the block at Arena.height of each column): how many, how many have no dirt path on any of their four sides (a dirt path block is 15/16 high, so one between full blocks draws as a dark pit; the roads are
         *  one surface and MapDressing.worn() leaves none, so the check asserts 0), how many lie where the dressing paves everything: within 9.2 blocks of a flag pole (the plaza) or on the parade ground of a camp (19 blocks behind to 25 in front of the
         *  spawn, 63 either side), off the floors and decks the structures build, and how many blocks of coarse dirt or gravel lie on the ground outside the plazas and off the river (earth: trampled earth is dirt path only, so 0). */
        record Pits(int paths,int lone,int paved,int earth,String sample,String earthSample,String detail){}
        static Pits pits(GravesAndGlory.Battle b){
            var w=b.world;int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),wz=2*hz+1,paths=0,lone=0,plaza=0,earth=0;var path=new BitSet((2*hx+1)*wz);var sample=new StringBuilder();var earthSample=new StringBuilder();var detail=new StringBuilder();
            for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++){
                if((z&15)==0)w.getChunk(x>>4,z>>4);
                var top=w.getBlockState(new BlockPos(x,Arena.height(x,z),z));
                if(!top.isOf(Blocks.DIRT_PATH)){
                    if((top.isOf(Blocks.COARSE_DIRT)||top.isOf(Blocks.GRAVEL))&&!Arena.river(x,z)&&!landmarkCell(x,z)){ // a landmark lays its own yard and lanes (Task 6) // trampled earth is the floor of a plaza and the worn strips of dirt path: coarse dirt or gravel anywhere else is a patch
                        double pd=999;for(var q:Spec.POINTS)pd=Math.min(pd,Math.hypot(x-q.x(),z-q.z()));
                        if(pd>=9.2){earth++;if(earth<=6)earthSample.append(earth>1?"; ":"").append(name(top)).append(" at (").append(x).append(", ").append(z).append(')');detail.append("earth ").append(name(top)).append(' ').append(x).append(' ').append(z).append((char)10);}
                    }
                    continue;
                }
                path.set((x+hx)*wz+(z+hz));paths++;
                boolean zone=false;String of="";
                for(var p:Spec.POINTS)if(Math.hypot(x-p.x(),z-p.z())<9.2){zone=true;of="plaza of point "+p.id();break;}
                for(int t=0;t<2&&!zone;t++){var f=Spec.FACTIONS[t];int u=(x-f.spawnX())*(t==0?1:-1);if(u>=-19&&u<=25&&Math.abs(z-f.spawnZ())<=63){zone=true;of="parade ground of "+f.id();}}
                if(zone&&!builtCell(x,z)){plaza++;if(plaza<=8)sample.append(plaza>1?"; ":"").append('(').append(x).append(", ").append(z).append(") ").append(of);detail.append("paved ").append(x).append(' ').append(z).append(' ').append(of).append('\n');}
            }
            for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++){
                if(!path.get((x+hx)*wz+(z+hz)))continue;
                boolean side=x>-hx&&path.get((x-1+hx)*wz+(z+hz))||x<hx&&path.get((x+1+hx)*wz+(z+hz))||z>-hz&&path.get((x+hx)*wz+(z-1+hz))||z<hz&&path.get((x+hx)*wz+(z+1+hz));
                if(!side){
                    lone++;
                    double foot=999,pt=999;for(var q:Spec.STRUCTURES)if(!q.kind().equals("bridge"))foot=Math.min(foot,Math.max(0,Math.max(Math.abs(x-q.x())-q.width()/2.0,Math.abs(z-q.z())-q.depth()/2.0)));
                    for(var q:Spec.POINTS)pt=Math.min(pt,Math.hypot(x-q.x(),z-q.z()));
                    detail.append("lone ").append(x).append(' ').append(z).append(" road ").append(Arena.road(x,z)).append(" footprint ").append((int)foot).append(" point ").append((int)pt).append('\n');
                }
            }
            return new Pits(paths,lone,plaza,earth,sample.toString(),earthSample.toString(),detail.toString());
        }
        /** The worn band and the worn ring, looked at on the map that stands: the dirt path cells that touch a road cell (not a road cell themselves), and the plain ground that is still left beside a road cell of dirt path where MapDressing.wearable says the band
         *  should lie (gapsRoad), the dirt path cells of the worn ring of every point whose stone ring is laid, and the plain ground still left in it (gapsRing). */
        record Wear(int verge,int ring,int gapsRoad,int gapsRing,int rings,String sample){int gaps(){return gapsRoad+gapsRing;}}
        /** The top blocks on the four sides of a cell (west, east, north, south), a star after a road cell. */
        static String around(net.minecraft.server.world.ServerWorld w,int x,int z){
            var sb=new StringBuilder();
            for(int[] o:new int[][]{{-1,0},{1,0},{0,-1},{0,1}}){int ax=x+o[0],az=z+o[1];sb.append(sb.length()>0?", ":"").append(name(w.getBlockState(new BlockPos(ax,Arena.height(ax,az),az)))).append(Arena.road(ax,az)?"*":"");}
            return sb.toString();
        }
        static Wear wear(GravesAndGlory.Battle b){
            var w=b.world;var d=new MapDressing(w);int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),verge=0,ring=0,gr=0,gg=0;var sample=new StringBuilder();
            for(int x=-hx+1;x<hx;x++)for(int z=-hz+1;z<hz;z++){
                if((z&15)==0)w.getChunk(x>>4,z>>4);
                if(Arena.road(x,z)||!(Arena.road(x-1,z)||Arena.road(x+1,z)||Arena.road(x,z-1)||Arena.road(x,z+1)))continue;
                if(w.getBlockState(new BlockPos(x,Arena.height(x,z),z)).isOf(Blocks.DIRT_PATH))verge++;
                else if(d.follows(x,z)&&d.wearable(x,z)){gr++;if(gr+gg<=6)sample.append(sample.length()>0?"; ":"").append("band (").append(x).append(", ").append(z).append(") with ").append(around(w,x,z));}
            }
            int rings=0;
            for(var p:Spec.POINTS){
                if(!Arena.ringLaid(p)||enclosed(p))continue; // an outpost (Task 6) paves round the ring of the point it encloses
                int r=p.radius();rings++;
                for(int dx=-r-4;dx<=r+4;dx++)for(int dz=-r-4;dz<=r+4;dz++){
                    if(!MapDressing.ringCell(dx,dz,r+.5))continue;
                    int x=p.x()+dx,z=p.z()+dz;var bd=MapDressing.bandOf(p,dx,dz);
                    if(Arena.wetBank(bd[0],bd[1])||Arena.deckCell(bd[0],bd[1]))continue;
                    if(w.getBlockState(new BlockPos(x,Arena.height(x,z),z)).isOf(Blocks.DIRT_PATH))ring++;
                    else if(d.wearable(x,z)){gg++;if(gr+gg<=6)sample.append(sample.length()>0?"; ":"").append("ring of ").append(p.id()).append(" (").append(x).append(", ").append(z).append(") with ").append(around(w,x,z));}
                }
            }
            return new Wear(verge,ring,gr,gg,rings,sample.toString());
        }
        /** Inside the footprint of a landmark (Task 6): an authored piece lays its own floor, yard and lanes. */
        static boolean landmarkCell(int x,int z){for(var s:Spec.STRUCTURES)if(s.kind().equals("landmark")&&Walkability.inside(s,x,z))return true;return false;}
        /** A landmark of the map that stands encloses the point (its landmarks row names it: the Kingsfield outposts round B and D). */
        static boolean enclosed(Spec.PointsRow p){for(var r:Spec.LANDMARKS)if(r.map()==CampaignMaps.selected&&r.encloses().equals(p.id()))return true;return false;}
        /** On what a structure builds: the cells x0..x0+width-1 by z0..z0+depth-1 (floor course, or the planks of a deck). */
        static boolean builtCell(int x,int z){
            for(var s:Spec.STRUCTURES){
                int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;
                if(x>=x0&&x<x0+s.width()&&z>=z0&&z<z0+s.depth()&&!(s.palette().equals("ford")&&Math.abs(x-Arena.riverX(z))>8))return true; // the approaches of a ford are ground
            }
            return false;
        }
        /** The paving of the plazas: within 9.2 blocks of a flag pole, off the 3x3 pad of the pole, off the water (11 columns of the river line) and off the buildings, every cell is the block of one pattern: the ring of paving from 8 blocks out, the compass of trim
         *  (three wide along the axes) inside it, trampled earth (coarse dirt) between. {cells looked at, those that are another block, the first of those}. */
        record Plazas(int cells,int wrong,String sample){}
        static Plazas plazas(GravesAndGlory.Battle b,int map){
            var w=b.world;int cells=0,wrong=0;var sample=new StringBuilder();
            Block pave=switch(map){case 1->Blocks.ANDESITE;case 2->Blocks.PACKED_ICE;case 3->Blocks.MUD_BRICKS;default->Blocks.POLISHED_ANDESITE;};
            Block trim=switch(map){case 2->Blocks.POLISHED_DIORITE;case 3->Blocks.MOSSY_STONE_BRICKS;default->Blocks.STONE_BRICKS;};
            for(var p:Spec.POINTS)for(int dx=-9;dx<=9;dx++)for(int dz=-9;dz<=9;dz++){
                int x=p.x()+dx,z=p.z()+dz;double d=Math.hypot(dx,dz);
                if(d>=9.2||Math.abs(dx)<=1&&Math.abs(dz)<=1||Math.abs(x-Arena.riverX(z))<11||builtCell(x,z))continue;
                Block want=d>=8?pave:(Math.abs(dx)<=1||Math.abs(dz)<=1)?trim:Blocks.COARSE_DIRT;
                cells++;var st=w.getBlockState(new BlockPos(x,Arena.height(x,z),z));
                if(!st.isOf(want)){wrong++;if(wrong<=8)sample.append(wrong>1?"; ":"").append(name(st)).append(" for ").append(name(want.getDefaultState())).append(" at (").append(x).append(", ").append(z).append(") of point ").append(p.id());}
            }
            return new Plazas(cells,wrong,sample.toString());
        }
        record River(int columns,int builtOn,int looked,int flat,int left,String sample,String detail){}
        /** The river columns of the map that stands (the build range, every z), minus the places the finish stage of the map builds above the water on purpose (builtOn). In each of the others the
         *  blocks from y=max(h,Y) up three cells must be air or water (or the barrier wall): h=Arena.height is the cell the column code clears from, so a block of the map that stood before
         *  survives exactly there when h is Y or more. The detail has one "block x y z" line per block found. Server thread. */
        static River river(GravesAndGlory.Battle b){
            var w=b.world;int hz=GravesAndGlory.R.worldHalfZ(),columns=0,builtOn=0,looked=0,flat=0,left=0;var sample=new StringBuilder();var detail=new StringBuilder();
            for(int z=-hz;z<=hz;z++){
                int rx=(int)Math.floor(Arena.riverX(z));
                for(int x=rx-10;x<=rx+10;x++){
                    if(!Arena.river(x,z))continue;
                    columns++;
                    if(builtOn(x,z)){builtOn++;continue;}
                    looked++;int h=Arena.height(x,z),y0=Math.max(h,Arena.Y);if(h>=Arena.Y)flat++;
                    for(int y=y0;y<=y0+2;y++){
                        var st=w.getBlockState(new BlockPos(x,y,z));
                        if(st.isAir()||st.isOf(Blocks.WATER)||st.isOf(Blocks.BARRIER))continue;
                        left++;detail.append(name(st)).append(' ').append(x).append(' ').append(y).append(' ').append(z).append('\n');
                        if(left<=6)sample.append(left>1?"; ":"").append(name(st)).append(' ').append(xyz(new BlockPos(x,y,z)));
                    }
                }
            }
            return new River(columns,builtOn,looked,flat,left,sample.toString(),detail.toString());
        }
        /** Where Arena.finish builds above the water of a river column on purpose: a bridge (deck planks and the two rail rows), the footprint of any other structure, the 3x3 centre of a capture point with its flag pole and the banner three cells east of the pole (the ring of smooth stone,
         *  the edge stone and the boundary posts of a point are not excused any more: since fix round 2 they stay off the water and off the decks) and, on map 0, the fishing piers MapDressing puts on the river banks. */
        static boolean builtOn(int x,int z){
            for(var s:Spec.STRUCTURES){
                if(s.kind().equals("bridge")){int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;if(x>=x0&&x<x0+s.width()&&z>=z0-1&&z<=z0+s.depth())return true;}
                else if(Walkability.inside(s,x,z))return true;
            }
            for(var p:Spec.POINTS){
                int dx=x-p.x(),dz=z-p.z();double d=Math.hypot(dx,dz);
                if(Math.abs(dx)<=1&&Math.abs(dz)<=1)return true;
                if(dz==0&&dx>=1&&dx<=3)return true;
            }
            if(CampaignMaps.selected==0)for(int pz:new int[]{-215,-72,72,215})for(int bank:new int[]{-1,1}){
                int from=(int)Math.round(Arena.riverX(pz))+bank*7,to=from-bank*5;
                if(x>=Math.min(from,to)&&x<=Math.max(from,to)&&Math.abs(z-pz)<=1)return true;
            }
            return false;
        }
        /** The bridge rails of the map that stands: {rail cells, those on dry ground or on a building floor, ends of stone wall runs without a post}. A rail cell is a spruce_fence (wood deck) or a stone_brick_wall
         *  (stone deck) one above the deck on the row north or south of it. It stands on dry ground when no river column lies within one column of it, and on a floor when it lies inside the footprint of
         *  a structure that is not a bridge (the courtyard of Ironhold is paved over the river). A run of walls ends where the next cell east or west is no wall, and the end needs the post (UP). Server thread. */
        static int[] rails(GravesAndGlory.Battle b){
            var w=b.world;int cells=0,dry=0,open=0;
            for(var s:Spec.STRUCTURES){
                if(!s.kind().equals("bridge")||s.palette().equals("ford"))continue;
                int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2,y=s.elevation()+1;
                for(int row:new int[]{z0-1,z0+s.depth()})for(int px=x0;px<x0+s.width();px++){
                    var st=w.getBlockState(new BlockPos(px,y,row));
                    boolean wall=st.getBlock() instanceof net.minecraft.block.WallBlock;
                    if(!wall&&!st.isOf(Blocks.SPRUCE_FENCE))continue;
                    cells++;
                    boolean water=Arena.river(px-1,row)||Arena.river(px,row)||Arena.river(px+1,row),floor=false;
                    for(var o:Spec.STRUCTURES)if(!o.kind().equals("bridge")&&Walkability.inside(o,px,row))floor=true;
                    if(!water||floor)dry++;
                    if(wall&&!st.get(net.minecraft.block.WallBlock.UP)&&!(w.getBlockState(new BlockPos(px-1,y,row)).getBlock() instanceof net.minecraft.block.WallBlock&&w.getBlockState(new BlockPos(px+1,y,row)).getBlock() instanceof net.minecraft.block.WallBlock))open++;
                }
            }
            return new int[]{cells,dry,open};
        }
        /** The glass panes inside the footprints of the structures: {all, those touching nothing}. A pane that touches nothing renders as a post, which is what a build without shape updates leaves behind. Server thread. */
        static int[] panes(GravesAndGlory.Battle b){
            var w=b.world;int all=0,lone=0;
            for(var s:Spec.STRUCTURES)for(int x=s.x()-s.width()/2;x<=s.x()+s.width()/2;x++)for(int z=s.z()-s.depth()/2;z<=s.z()+s.depth()/2;z++)for(int y=s.elevation();y<=s.elevation()+s.height()+3;y++){
                var st=w.getBlockState(new BlockPos(x,y,z));
                if(st.getBlock() instanceof net.minecraft.block.PaneBlock){all++;if(!st.get(Properties.NORTH)&&!st.get(Properties.EAST)&&!st.get(Properties.SOUTH)&&!st.get(Properties.WEST))lone++;}
            }
            return new int[]{all,lone};
        }
        /** The item entities loaded in the built area, with what they are: "N (id xCount, ...)". Server thread. */
        static String items(GravesAndGlory.Battle b){
            var w=b.world;int hx=GravesAndGlory.R.worldHalfX(),hz=GravesAndGlory.R.worldHalfZ();
            var found=w.getEntitiesByType(EntityType.ITEM,new Box(-hx,w.getBottomY(),-hz,hx+1,w.getTopY(),hz+1),e->true);
            var kinds=new TreeMap<String,Integer>();
            for(var e:found)kinds.merge(net.minecraft.registry.Registries.ITEM.getId(e.getStack().getItem()).getPath(),e.getStack().getCount(),Integer::sum);
            return found.size()+" "+kinds;
        }
    }

    /** Task 2, the walkthrough gate (suite walkthrough): the owner's walk of the battle maps, by a real client player who runs along the roads with the movement keys held and photographs what he sees.
     *  GRAVES_PHASE3_MAPS (system property gravesandglory.phase3Maps) lists the maps, default all five; one map per launch keeps a launch under half an hour. Each map is built over the map built before it (N+4 modulo 5 when the
     *  suite starts), so a stale block of the older map would show. Per map the walker follows, 2 blocks to the right of the centre line (a flag pole stands at the centre of a point), every lane from the Dawn camp over its crossing
     *  to the Dusk camp (lane0 to lane2), on Kingsfield the flank road over both outposts and the king's bridge (flank), the two link roads that join the lane heads of one side (link-west, link-east), and every spur road that ends at a
     *  building (spur<k>-<building>; a spur is walked up to the wall of its building; the spur of a supply house is the lane house-<id>, walked through its front gate to 4 blocks short of the yard centre, GRAVES_WALK_ONLY=supply).
     *  The pictures (screenshots/p3-wt<N>-<sequence>-<what>.png, 1280x720, taken where the walker stands, hand and HUD hidden) are: both camps from their dais (camp-); per lane the start of the lane, the approach to the deck from the west
     *  bank, each capture point from 14 blocks up the road, the middle of the deck looking along it, the approach from the east bank (looking back at the deck) and the head of the lane on the Dusk side; per link road its start and middle; the
     *  first spur of each kind (houses and farm and mill, one tower of each camp); where a lane had a stuck window, the place from 16 and from 6 blocks before it (stuck-); the water where the ring band of a point on a deck crossed it, seen from the north
     *  edge of the deck (band-); the four ends of the two rails of each deck and the two edges of the strip of a ford (rail-); the two corners of the bank at the middle crossing where a post and the edge stone of the dressing once stood in the
     *  river (posts-); the two places 16 and 6 blocks before the wall of a camp barracks that the straight fan road used to run into, and the lanes that bend past it (wall-, lane<k>-barracks-); the inside of each barracks hall looking up at its
     *  lantern (hall-); the plaza floor of the first two points on dry land (plaza-); the edge of an open road, seen from the grass beside it (edge-); the view up at each raised outpost from the road and from the shoulder of the plateau (outpost-) and the road up to it from where the climb begins (ramp-); each kind of floating piece from where a walker can see it (float-);
     *  each building no road leads to (building-); and the two longest sight lines of the map at render distance 28 (sight-).
     *  Checks: p3-wt<N>-leg-<id> passes when the walker reached the end of the lane and no 2-second window of walking gained less than 3 blocks (a window that did is a stuck window, counted per lane; the walker then hops, and if that does not help
     *  is put on past the obstacle, on the road and not on a roof); p3-wt<N>-stuck is the total for the map; auto-jumps (a step up the walker had to hop), windows under 6 blocks and the y range of each lane are in the notes. p3-wt<N>-foreign
     *  passes when no column of the arena has a top block that belongs to another map; p3-wt<N>-river is the river check of the arena suite run again on the map as it stands; p3-wt<N>-floating passes when every solid block of the arena touches,
     *  by a face, an edge or a corner, a chain of solid blocks that reaches the floor. The rest of the scans (bands over water, hanging ring and pad, blocks standing in the river, the profile of every lane, the sight lines, every floating piece)
     *  are notes in screenshots/p3-wt<N>-notes.txt.
     *  GRAVES_WALK_ONLY (or a file walk-only.txt in the run folder) lists name fragments: only the lanes and picture groups whose name contains one of them are done (camp, lane1, flank, link, spur0, band, rail, posts, wall, hall, plaza, edge, outpost, ramp, float, building, sight). */
    static class Walkthrough implements Suite {
        static final net.minecraft.world.Heightmap.Type FEET=net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES;
        /** A picture stop of a lane. dwell: extra ticks the walker stands still before the picture (the camp walk stands on a pad); probe: a name the suite answers in probe() just before the picture (null: none). */
        record Stop(String name,double arc,double tx,double ty,double tz,int dwell,String probe){
            Stop(String name,double arc,double tx,double ty,double tz){this(name,arc,tx,ty,tz,0,null);}
        }
        /** The first part of the name of this suite's check ids and picture files: wt<N> for the map walk, cw for the camp walk. */
        String pre(){return "wt"+n;}
        /** Called at a stop with a probe name, with the walker standing still and before the picture. */
        void probe(MinecraftClient c,Stop st,Leg leg){}
        /** What passes a lane check: the map walk wants it finished with no stuck window, the camp walk also wants no forced step. */
        boolean legOk(Leg leg){return leg.finished&&leg.stuck==0;}

        /** A road to walk: its centre line c, the line q a walker keeps to (off blocks to his right of c, mitred at the bends) with the length arc of q up to each vertex. */
        static final class Leg {
            final String id;final double off,len;final double[][] c,q;final double[] arc;final List<Stop> stops=new ArrayList<>();
            int stuck,slow,bumps,forced,ticks,photos;double walked,ymin=1e9,ymax=-1e9,stuckArc=-1;boolean finished,failed;
            /** sprint: the walker holds the sprint key (the camp walk walks, doors and stairs are narrow); holdEnd: the lane is not finished before the last stop is done (a stop at the very end). */
            boolean sprint=true,holdEnd=false;double look=5.0;
            Leg(String id,double off,double[]... centre){
                this.id=id;this.off=off;c=centre;int m=c.length;q=new double[m][2];arc=new double[m];double[][] r=new double[m-1][2];
                for(int i=0;i<m-1;i++){double dx=c[i+1][0]-c[i][0],dz=c[i+1][1]-c[i][1],l=Math.hypot(dx,dz);r[i][0]=-dz/l;r[i][1]=dx/l;} // the right-hand normal: east -> south, south -> west
                for(int i=0;i<m;i++){
                    double nx,nz;
                    if(i==0){nx=r[0][0];nz=r[0][1];}
                    else if(i==m-1){nx=r[m-2][0];nz=r[m-2][1];}
                    else{double k=Math.max(.35,1+r[i-1][0]*r[i][0]+r[i-1][1]*r[i][1]);nx=(r[i-1][0]+r[i][0])/k;nz=(r[i-1][1]+r[i][1])/k;}
                    q[i][0]=c[i][0]+nx*off;q[i][1]=c[i][1]+nz*off;
                }
                for(int i=1;i<m;i++)arc[i]=arc[i-1]+Math.hypot(q[i][0]-q[i-1][0],q[i][1]-q[i-1][1]);
                len=arc[m-1];
            }
            /** The point of q at arc length s (clamped to the ends). */
            double[] at(double s){
                s=Math.max(0,Math.min(len,s));int i=0;while(i<q.length-2&&arc[i+1]<s)i++;
                double l=arc[i+1]-arc[i],t=l<1e-9?0:(s-arc[i])/l;
                return new double[]{q[i][0]+(q[i+1][0]-q[i][0])*t,q[i][1]+(q[i+1][1]-q[i][1])*t};
            }
            /** {arc length, distance} of the point of q nearest to (x,z). */
            double[] nearest(double x,double z){
                double best=1e18,bs=0;
                for(int i=0;i<q.length-1;i++){
                    double dx=q[i+1][0]-q[i][0],dz=q[i+1][1]-q[i][1],l2=dx*dx+dz*dz,t=Math.max(0,Math.min(1,((x-q[i][0])*dx+(z-q[i][1])*dz)/l2));
                    double ex=q[i][0]+dx*t-x,ez=q[i][1]+dz*t-z,d=ex*ex+ez*ez;
                    if(d<best){best=d;bs=arc[i]+t*Math.sqrt(l2);}
                }
                return new double[]{bs,Math.sqrt(best)};
            }
            double arcOf(double x,double z){return nearest(x,z)[0];}
        }

        // ---- configuration and state ----
        final int[] maps;final List<String> only=new ArrayList<>();
        int mi,stage,waited,seq,n,goal;boolean pre,preBuilding;
        final ArrayDeque<Job> jobs=new ArrayDeque<>();Job job;List<Leg> legs=List.of();
        final List<String> notes=Collections.synchronizedList(new ArrayList<>());
        int stuckTotal,slowTotal,bumpTotal,forcedTotal,legsDone,legsFailed,photoTotal;double walkedTotal;
        volatile boolean switched,scanned;volatile List<Sight> sights=List.of();volatile List<int[]> standing=List.of();volatile Map<String,Integer> bandWater=Map.of();
        Walkthrough(){
            var list=new ArrayList<Integer>();
            for(String s:System.getProperty("gravesandglory.phase3Maps","0,1,2,3,4").split(","))if(!s.isBlank())list.add(Integer.parseInt(s.trim()));
            maps=list.stream().mapToInt(Integer::intValue).toArray();
            String f=System.getenv("GRAVES_WALK_ONLY");
            if(f!=null)for(String s:f.split("[,\s]+"))if(!s.isBlank())only.add(s);
        }
        /** Active ticks only (nothing steps while the arena is being built): about 14000 for a map, most of it walking. */
        @Override public int limit(){return 8000+maps.length*20000;}
        boolean want(String id){if(only.isEmpty())return true;for(String f:only)if(id.contains(f))return true;return false;}
        void note(String text){notes.add(text);System.out.println("[p3] note wt"+n+" "+text);}

        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            if(tick==20)setup(c);
            n=mi<maps.length?maps[mi]:-1;
            var b=GravesAndGlory.battle;
            if(stage==0){
                int target=mi==0&&!pre?(n+4)%5:n;preBuilding=target!=n;pre=true;goal=target;switched=false;waited=0;
                server(c,x->{if(x.campaign.map!=target)x.campaign.switchMap(target);switched=true;});
                stage=1;
            }else if(stage==1){
                if(switched&&b!=null&&b.ready&&b.campaign.map==goal&&CampaignMaps.selected==goal){
                    if(preBuilding){preBuilding=false;stage=0;}
                    else{beginMap(c);stage=2;}
                }else if(++waited>3000){report("p3-wt"+n+"-ready",false,"map "+goal+" was not ready after "+waited+" ticks");finish(c,"walkthrough");stage=9;}
            }else if(stage==2){
                if(scanned){assemble();server(c,x->x.campaign.leash=false);stage=3;}
            }else if(stage==3){
                if(job==null){job=jobs.poll();if(job==null){stage=4;return;}}
                if(job.step(c)){if(job instanceof LegJob lj)legDone(lj.leg);job=null;}
            }else if(stage==4){
                endMap(c);mi++;stage=mi<maps.length?0:5;
            }else if(stage==5){
                finish(c,"walkthrough");stage=9;
            }
        }
        @Override public void cleanup(MinecraftClient c){
            release(c);c.gameRenderer.setRenderHand(true);
            server(c,b->b.campaign.leash=true);
        }
        static void release(MinecraftClient c){var o=c.options;o.forwardKey.setPressed(false);o.sprintKey.setPressed(false);o.jumpKey.setPressed(false);o.backKey.setPressed(false);o.leftKey.setPressed(false);o.rightKey.setPressed(false);o.sneakKey.setPressed(false);}
        static boolean terrainReady(MinecraftClient c,double x,double z){return c.world.isChunkLoaded(((int)Math.floor(x))>>4,((int)Math.floor(z))>>4)&&c.worldRenderer.isTerrainRenderComplete();}
        static float wrap(float a){return net.minecraft.util.math.MathHelper.wrapDegrees(a);}

        /** Once: the walker's own view (no bobbing, no hand, no sprint zoom), auto-jump on as a player has it, and the option files. */
        void setup(MinecraftClient c){
            var o=c.options;
            o.getBobView().setValue(false);o.getFovEffectScale().setValue(0.0);o.getDistortionEffectScale().setValue(0.0);o.getAutoJump().setValue(true);o.getViewDistance().setValue(12);
            c.gameRenderer.setRenderHand(false);
            try{Path f=c.runDirectory.toPath().resolve("walk-only.txt");if(Files.exists(f))for(String s:Files.readString(f).trim().split("[,\s]+"))if(!s.isBlank())only.add(s);}catch(Exception e){System.out.println("[p3] note walk-only.txt: "+e);}
            System.out.println("[p3] note walkthrough maps "+Arrays.toString(maps)+" only "+only);
        }
        /** A new map stands: counters to zero, the lanes made, the scans started on the server thread. */
        void beginMap(MinecraftClient c){
            seq=0;stuckTotal=slowTotal=bumpTotal=forcedTotal=legsDone=legsFailed=photoTotal=0;walkedTotal=0;notes.clear();scanned=false;
            legs=buildLegs();final List<Leg> mine=legs;final int map=n;
            note("map "+map+": "+Spec.POINTS.length+" points, "+MapRouteChecks.bridges().size()+" crossings, "+mine.size()+" lanes to walk: "+mine.stream().map(l->l.id+" "+Math.round(l.len)).toList());
            server(c,x->scanMap(x,map,mine));
        }
        void endMap(MinecraftClient c){
            int total=0;for(var l:legs)if(want(l.id))total++;
            report("p3-wt"+n+"-stuck",stuckTotal==0&&legsFailed==0&&legsDone==total,"lanes finished "+legsDone+" of "+total+", failed "+legsFailed+", blocks walked "+Math.round(walkedTotal)+", stuck windows "+stuckTotal+", slow windows (under 6 blocks in 2 s) "+slowTotal+", forced steps "+forcedTotal+", auto-jumps "+bumpTotal+", pictures "+photoTotal);
            try{Path file=c.runDirectory.toPath().resolve("screenshots/p3-wt"+n+"-notes.txt");Files.createDirectories(file.getParent());synchronized(notes){Files.writeString(file,String.join("\n",notes)+"\n");}}
            catch(Exception e){report("p3-wt"+n+"-notes",false,e.toString());}
            release(c);c.options.getViewDistance().setValue(12);UUID id=c.player.getUuid();
            server(c,x->{x.campaign.leash=true;var sp=x.server.getPlayerManager().getPlayer(id);if(sp!=null)x.campaign.toLobby(sp);});
        }
        void legDone(Leg leg){
            stuckTotal+=leg.stuck;slowTotal+=leg.slow;bumpTotal+=leg.bumps;forcedTotal+=leg.forced;walkedTotal+=leg.walked;
            if(leg.finished)legsDone++;else legsFailed++;
            if(leg.stuckArc>=0){
                double[] on=leg.at(leg.stuckArc+1.5);
                for(int back:new int[]{16,6}){double[] from=leg.at(Math.max(0.0,leg.stuckArc-back));
                    jobs.addFirst(new ShotJob("stuck-"+leg.id+"-"+back,from[0],from[1],on[0],66.0,on[1],12,"lane "+leg.id+": "+back+" blocks before the place of its first stuck window ("+Math.round(on[0])+", "+Math.round(on[1])+"), looking at it"));}
            }
            report("p3-"+pre()+"-leg-"+leg.id,legOk(leg),String.format(Locale.ROOT,"%s lane of %.0f blocks walked in %d ticks (%.1f s): stuck windows %d, slow windows %d, forced steps %d, auto-jumps %d, y %.2f to %.2f, pictures %d",leg.finished?"finished":"NOT finished,",leg.len,leg.ticks,leg.ticks/20.0,leg.stuck,leg.slow,leg.forced,leg.bumps,leg.ymin,leg.ymax,leg.photos));
        }
        /** One picture: the file is p3-wt<N>-<sequence>-<tag>.png, and its check line says where the walker stood and looked. */
        void took(MinecraftClient c,String tag,String what,Leg leg){
            String file=pre()+"-"+String.format(Locale.ROOT,"%02d",++seq)+"-"+tag;shot(c,file);photoTotal++;if(leg!=null)leg.photos++;
            var p=c.player;
            report("p3-"+file,true,String.format(Locale.ROOT,"feet (%.1f, %.1f, %.1f), eye y %.2f, yaw %.1f, pitch %.1f, render distance %d: %s",p.getX(),p.getY(),p.getZ(),p.getEyeY(),p.getYaw(),p.getPitch(),c.options.getViewDistance().getValue(),what));
        }

        // ---- the jobs ----
        /** One thing the walker does; step is called every client tick until it answers true. */
        abstract class Job{int t;abstract boolean step(MinecraftClient c);}

        /** Puts the walker on the ground at (x,z) looking at a target and photographs it (tag null: only puts him there, for the start of a lane). */
        final class ShotJob extends Job {
            final String tag,what;final double x,z,tx,ty,tz,fixedY;final int view;
            volatile boolean placed;volatile double feet,yaw,pitch;int arrived;
            ShotJob(String tag,double x,double z,double tx,double ty,double tz,int view,String what){this(tag,x,z,tx,ty,tz,view,what,Double.NaN);}
            /** fixedY: the height of the feet when the walker stands somewhere the heightmap does not know (inside a hall). */
            ShotJob(String tag,double x,double z,double tx,double ty,double tz,int view,String what,double fixedY){this.tag=tag;this.x=x;this.z=z;this.tx=tx;this.ty=ty;this.tz=tz;this.view=view;this.what=what;this.fixedY=fixedY;}
            @Override boolean step(MinecraftClient c){
                var p=c.player;if(p==null)return false;
                if(c.currentScreen!=null)c.setScreen(null);
                t++;
                if(t==1){
                    release(c);if(c.options.getViewDistance().getValue()!=view)c.options.getViewDistance().setValue(view);
                    UUID id=p.getUuid();
                    server(c,b->{
                        var sp=b.server.getPlayerManager().getPlayer(id);if(sp==null)return;
                        int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
                        b.world.getChunk(bx>>4,bz>>4);
                        double top=Double.isNaN(fixedY)?b.world.getTopY(FEET,bx,bz):fixedY;
                        var v=MapTour.look("walk",x,top+1.62,z,tx,ty,tz);
                        sp.changeGameMode(GameMode.ADVENTURE);b.clearOldUnits();
                        b.relocate(sp,x,top,z,v.yaw(),v.pitch());
                        yaw=v.yaw();pitch=v.pitch();feet=top;placed=true;
                    });
                    return false;
                }
                String label=tag==null?"start":tag;
                if(!placed){if(t>200){report("p3-"+pre()+"-port-"+label,false,"the server did not place the walker");return true;}return false;}
                if(p.squaredDistanceTo(x,feet,z)>2.25){if(t>400){report("p3-"+pre()+"-port-"+label,false,"the walker did not arrive at ("+x+", "+feet+", "+z+"), he stands at ("+p.getX()+", "+p.getY()+", "+p.getZ()+")");return true;}return false;}
                if(arrived==0)arrived=t;
                p.setYaw((float)yaw);p.setPitch((float)pitch);p.prevYaw=(float)yaw;p.prevPitch=(float)pitch;
                boolean steady=p.isOnGround()&&p.getVelocity().horizontalLength()<0.02&&terrainReady(c,x,z);
                int since=t-arrived;
                if(tag==null)return since>=14&&steady||since>=150;
                if(since<24||!steady&&since<300)return false;
                took(c,tag,what,null);return true;
            }
        }

        /** What stands in front of the walker's feet and head, for a log line. */
        static String ahead(MinecraftClient c){
            var p=c.player;double yaw=Math.toRadians(p.getYaw()),dx=-Math.sin(yaw),dz=Math.cos(yaw);var sb=new StringBuilder();
            for(int s=1;s<=2;s++)for(int dy=0;dy<=1;dy++){
                var pos=BlockPos.ofFloored(p.getX()+dx*s*0.8,p.getY()+dy+0.1,p.getZ()+dz*s*0.8);var st=c.world.getBlockState(pos);
                if(!st.isAir())sb.append(name(st)).append(' ').append(xyz(pos)).append("; ");
            }
            return sb.length()==0?"nothing":sb.toString();
        }
        /** The blocks with a collision shape in the 3x3 columns round the walker, on the two layers above the ground he hopped from, with the top of each shape (for the auto-jump notes). */
        static String around(MinecraftClient c){
            var p=c.player;int by=(int)Math.floor(p.getY()-0.45);var sb=new StringBuilder();
            for(int dy=0;dy<=1;dy++)for(int ax=-1;ax<=1;ax++)for(int az=-1;az<=1;az++){
                var pos=BlockPos.ofFloored(p.getX()+ax,by+dy,p.getZ()+az);var st=c.world.getBlockState(pos);
                if(st.isAir())continue;var sh=st.getCollisionShape(c.world,pos);if(sh.isEmpty())continue;
                sb.append(name(st)).append(' ').append(xyz(pos)).append(String.format(Locale.ROOT," top %.2f; ",pos.getY()+sh.getMax(Direction.Axis.Y)));
            }
            return sb.length()==0?"nothing":sb.toString();
        }

        /** The walk: the real client player runs along a lane with the forward and sprint keys held, steering by his yaw alone (the lane line five blocks ahead), and stops for each of its pictures. */
        final class LegJob extends Job {
            final Leg leg;int seg,stopIx,phaseT,jump,win,run;String phase="walk";double wx,wz;boolean hasWin,ground=true;
            LegJob(Leg leg){this.leg=leg;}
            /** An auto-jump: he left the ground with upward speed over 0.2 and the jump key was not down (the client's own hop over a block in front of him). */
            void bumpCheck(MinecraftClient c,boolean g,double px,double pz,double arc){
                var p=c.player;
                if(ground&&!g&&p.getVelocity().y>0.2&&!c.options.jumpKey.isPressed()){
                    leg.bumps++;if(leg.bumps<=10)note(String.format(Locale.ROOT,"auto-jump %d on lane %s at (%.1f, %.1f, %.1f), %.0f of %.0f blocks; ahead: %s; round him: %s",leg.bumps,leg.id,px,p.getY(),pz,arc,leg.len,ahead(c),around(c)));
                }
            }
            @Override boolean step(MinecraftClient c){
                var p=c.player;if(p==null)return false;
                if(c.currentScreen!=null)c.setScreen(null);
                t++;leg.ticks=t;var o=c.options;
                double px=p.getX(),pz=p.getZ();var q=leg.q;int m=q.length;
                while(seg<m-2){ // past the end of a stretch: on to the next one
                    double sx=q[seg+1][0]-q[seg][0],sz=q[seg+1][1]-q[seg][1],sl=Math.hypot(sx,sz),u=((px-q[seg][0])*sx+(pz-q[seg][1])*sz)/sl;
                    if(u>=sl-0.5||Math.hypot(px-q[seg+1][0],pz-q[seg+1][1])<1.5)seg++;else break;
                }
                double sx=q[seg+1][0]-q[seg][0],sz=q[seg+1][1]-q[seg][1],sl=Math.hypot(sx,sz);
                double u=Math.max(0,Math.min(sl,((px-q[seg][0])*sx+(pz-q[seg][1])*sz)/sl)),arc=leg.arc[seg]+u;
                leg.ymin=Math.min(leg.ymin,p.getY());leg.ymax=Math.max(leg.ymax,p.getY());                if(p.getY()<Arena.Y-6||p.isDead()){release(c);leg.failed=true;report("p3-"+pre()+"-fell-"+leg.id,false,String.format(Locale.ROOT,"the walker fell or died at (%.1f, %.1f, %.1f), %.0f of %.0f blocks along lane %s",px,p.getY(),pz,arc,leg.len,leg.id));return true;}
                if(seg==m-2&&sl-u<1.2&&(!leg.holdEnd||stopIx>=leg.stops.size())){release(c);leg.finished=true;return true;}
                if(t>leg.len*8+900+(leg.holdEnd?leg.stops.size()*400:0)){release(c);leg.failed=true;report("p3-"+pre()+"-timeout-"+leg.id,false,String.format(Locale.ROOT,"lane %s not finished after %d ticks, the walker is at (%.1f, %.1f, %.1f), %.0f of %.0f blocks",leg.id,t,px,p.getY(),pz,arc,leg.len));return true;}
                boolean g=p.isOnGround();
                if(phase.equals("walk")){
                    if(stopIx<leg.stops.size()&&arc>=leg.stops.get(stopIx).arc()-1.8){bumpCheck(c,g,px,pz,arc);phase="brake";phaseT=0;release(c);ground=g;return false;}
                    double[] a=leg.at(arc+leg.look);
                    float want=(float)Math.toDegrees(Math.atan2(-(a[0]-px),a[1]-pz)),cur=p.getYaw(),d=wrap(want-cur);
                    p.setYaw(cur+Math.max(-25f,Math.min(25f,d)));p.prevYaw=p.getYaw();p.setPitch(4f);p.prevPitch=4f;
                    boolean go=Math.abs(wrap(want-p.getYaw()))<(leg.look<4?12f:35f);
                    o.forwardKey.setPressed(go);o.sprintKey.setPressed(go&&leg.sprint);o.jumpKey.setPressed(jump>0);if(jump>0)jump--;
                    bumpCheck(c,g,px,pz,arc);
                    if(go)watch(c,px,pz,arc);
                }else if(phase.equals("brake")){
                    release(c);bumpCheck(c,g,px,pz,arc); // the walker still coasts a quarter of a block here and the client's own auto-jump can hop him up a block in front of him
                    if(p.getVelocity().horizontalLength()<0.03||++phaseT>30){phase="look";phaseT=0;}
                }else if(phase.equals("look")){
                    var st=leg.stops.get(stopIx);var v=MapTour.look(st.name(),p.getX(),p.getEyeY(),p.getZ(),st.tx(),st.ty(),st.tz());
                    p.setYaw(v.yaw());p.setPitch(v.pitch());p.prevYaw=v.yaw();p.prevPitch=v.pitch();phase="wait";phaseT=0;
                }else if(phase.equals("wait")){
                    var st=leg.stops.get(stopIx);
                    if(++phaseT>=24+st.dwell()&&(terrainReady(c,p.getX(),p.getZ())||phaseT>=240+st.dwell())){
                        if(st.probe()!=null)probe(c,st,leg);
                        took(c,st.name(),String.format(Locale.ROOT,"lane %s at %.0f of %.0f blocks, looking at (%.1f, %.1f, %.1f)",leg.id,arc,leg.len,st.tx(),st.ty(),st.tz()),leg);
                        stopIx++;phase="walk";hasWin=false;
                    }
                }
                ground=g;
                return false;
            }
            /** Every 40 ticks of walking: did he gain 3 blocks? If not, a stuck window (he hops; a second one in a row puts him 8 blocks on). */
            void watch(MinecraftClient c,double px,double pz,double arc){
                if(!hasWin){wx=px;wz=pz;win=0;hasWin=true;return;}
                if(++win<40)return;
                double moved=Math.hypot(px-wx,pz-wz);leg.walked+=moved;
                if(moved<3.0){
                    leg.stuck++;run++;
                    report("p3-"+pre()+"-stuck-"+leg.id+"-"+leg.stuck,false,String.format(Locale.ROOT,"gained %.1f blocks in 2 s at (%.1f, %.1f, %.1f), %.0f of %.0f blocks along lane %s; ahead: %s",moved,px,c.player.getY(),pz,arc,leg.len,leg.id,ahead(c)));
                    if(leg.stuck==1)leg.stuckArc=arc;
                    if(run==1)jump=10;
                    else{
                        leg.forced++;run=0;
                        UUID id=c.player.getUuid();float yaw=c.player.getYaw();
                        server(c,b->{var sp=b.server.getPlayerManager().getPlayer(id);if(sp==null)return;
                            double[] f=leg.at(Math.min(leg.len,arc+8.0));int top=0;
                            for(double ds=8.0;ds<=70.0;ds+=2.0){ // on past a wall: the first cell of the lane that is on the ground of the road, not on a roof
                                f=leg.at(Math.min(leg.len,arc+ds));int bx=(int)Math.floor(f[0]),bz=(int)Math.floor(f[1]);b.world.getChunk(bx>>4,bz>>4);top=b.world.getTopY(FEET,bx,bz);
                                if(top<=Arena.Y+2)break;
                            }
                            b.relocate(sp,f[0],top,f[1],yaw,4f);});
                    }
                }else{run=0;if(moved<6.0)leg.slow++;}
                wx=px;wz=pz;win=0;
            }
        }

        // ---- the lanes ----
        static Stop stop(Leg leg,String name,double x,double z,double tx,double ty,double tz){return new Stop(name,leg.arcOf(x,z),tx,ty,tz);}
        /** The barracks of the camp on a side (-1 Dawn, 1 Dusk), the hall at the end of the muster ground, or null. */
        static Spec.StructuresRow campHall(int side){
            for(var s:Spec.STRUCTURES)if(s.kind().equals("barracks")&&Integer.signum(s.x())==side&&Math.abs(s.x())>256)return s;
            return null;
        }
        /** The structure other than a bridge whose footprint holds (x,z), or null. */
        static Spec.StructuresRow owner(double x,double z){
            int ix=(int)Math.floor(x),iz=(int)Math.floor(z);
            for(var s:Spec.STRUCTURES)if(!s.kind().equals("bridge")&&Walkability.inside(s,ix,iz))return s;
            return null;
        }
        /** A picture of each capture point whose centre the lane passes within 3.5 blocks: 14 blocks before it, looking at the flag. Each point goes to the first lane that passes it. */
        static void pointStops(Leg leg,Set<String> claimed){
            for(var p:Spec.POINTS){
                if(claimed.contains(p.id()))continue;
                var near=leg.nearest(p.x(),p.z());
                if(near[1]>3.5)continue;
                claimed.add(p.id());
                leg.stops.add(new Stop(leg.id+"-point-"+p.id(),Math.max(0.5,near[0]-14),p.x(),p.elevation()+4.5,p.z()));
            }
            leg.stops.sort(Comparator.comparingDouble(Stop::arc));
        }
        /** The lanes of the map that stands. Lane k runs from the Dawn camp (-290, 0) by the road to its lane head (-205, z), along it to the crossing, over the deck and on to (205, z) and the Dusk camp (290, 0), exactly the roads CampaignMaps.select lays
         *  for crossing k; on Kingsfield the flank lane runs the two outpost roads and the king's bridge; a spur is a road with one end in a building, walked from the free end up to the wall of the building. The camp road between the barracks and the stores
         *  (both ends in a camp building) is not walked. */
        List<Leg> buildLegs(){
            var out=new ArrayList<Leg>();var claimed=new HashSet<String>();double[] west={-290,0},east={290,0};int k=0;
            for(var s:MapRouteChecks.bridges()){
                int rx=s.x(),z=s.z(),xw=rx-s.width()/2,xe=xw+s.width()-1;double lz=z+2.0;
                var wp=new ArrayList<double[]>();wp.add(west); // the fan roads of the camps bend once on the side of the barracks (CampaignMaps.fanBend)
                var bw=CampaignMaps.fanBend(-1,z);if(bw!=null)wp.add(new double[]{bw[0],bw[1]});
                wp.add(new double[]{-205,z});wp.add(new double[]{rx-28,z});wp.add(new double[]{rx+28,z});wp.add(new double[]{205,z});
                var be=CampaignMaps.fanBend(1,z);if(be!=null)wp.add(new double[]{be[0],be[1]});
                wp.add(east);
                var leg=new Leg("lane"+k,2.0,wp.toArray(new double[0][]));
                var hallW=campHall(-1);if(bw!=null&&hallW!=null)leg.stops.add(new Stop("lane"+k+"-barracks-dawn",leg.arcOf(bw[0],bw[1])+5.0,hallW.x()+0.5,hallW.elevation()+3.0,hallW.z()+0.5));
                var hallE=campHall(1);if(be!=null&&hallE!=null)leg.stops.add(new Stop("lane"+k+"-barracks-dusk",Math.max(0.5,leg.arcOf(be[0],be[1])-5.0),hallE.x()+0.5,hallE.elevation()+3.0,hallE.z()+0.5));
                leg.stops.add(stop(leg,"lane"+k+"-head",-195,lz,-35,65.0,lz));
                leg.stops.add(stop(leg,"lane"+k+"-west-approach",xw-12,lz,xe+8,64.9,lz));
                leg.stops.add(stop(leg,"lane"+k+"-dusk-head",195,lz,35,65.0,lz));
                leg.stops.add(stop(leg,"lane"+k+"-deck-mid",rx,lz,rx+60,65.0,lz));
                leg.stops.add(stop(leg,"lane"+k+"-east-approach",xe+13,lz,xw-40,64.9,lz));
                pointStops(leg,claimed);out.add(leg);k++;
            }
            if(CampaignMaps.selected==4){
                var leg=new Leg("flank",2.0,west,new double[]{-205,0},new double[]{-205,100},new double[]{-130,100},new double[]{-28,0},new double[]{28,0},new double[]{130,-100},new double[]{205,-100},new double[]{205,0},east);
                leg.stops.add(new Stop("flank-kings-west",leg.arc[4]-12,0,65.0,0));
                pointStops(leg,claimed);out.add(leg);
            }
            for(var r:CampaignMaps.roads)if(r.width()==4&&r.ax()==r.bx()&&Math.abs(r.ax())==205){ // the road that joins the lane heads of one side: from the camp roads along x=-205 or x=205
                String id=r.ax()<0?"link-west":"link-east";double sgn=Math.signum(r.bz()-r.az());
                var leg=new Leg(id,2.0,new double[]{r.ax(),r.az()},new double[]{r.bx(),r.bz()});
                leg.stops.add(new Stop(id+"-start",8.0,r.ax(),65.0,r.az()+sgn*120));
                leg.stops.add(new Stop(id+"-middle",leg.len/2,r.ax(),65.0,(r.az()+r.bz())/2.0+sgn*120));
                out.add(leg);
            }
            int sp=0;var towerPal=new HashSet<String>();
            for(var r:CampaignMaps.roads){
                var a=owner(r.ax(),r.az());var b=owner(r.bx(),r.bz());
                if((a==null)==(b==null))continue;
                boolean aIn=a!=null;var st=aIn?a:b;
                double sx=aIn?r.bx():r.ax(),sz=aIn?r.bz():r.az(),ex=aIn?r.ax():r.bx(),ez=aIn?r.az():r.bz(),l=Math.hypot(ex-sx,ez-sz),ux=(ex-sx)/l,uz=(ez-sz)/l,from=0,to=l;
                Spec.SupplyHousesRow house=null;for(var hh:Spec.SUPPLYHOUSES)if(hh.map()==CampaignMaps.selected&&hh.landmark().equals(st.id())&&hh.yardX()==ex&&hh.yardZ()==ez)house=hh;
                if(house!=null){out.add(houseLeg(house,sx,sz,ux,uz,l));continue;} // a supply house: its own lane (Task 8b), not a spur numbered with the others
                for(var p:Spec.POINTS)if(Math.hypot(sx-p.x(),sz-p.z())<3.0)from=4.0; // it starts on the pole of a flag
                for(double d=from;d<=l;d+=1.0)if(owner(sx+ux*d,sz+uz*d)!=null){to=d-1.0;break;}
                if(to-from<8.0)continue;
                var leg=new Leg("spur"+sp+"-"+st.id(),Math.min(2.0,r.width()-1.5),new double[]{sx+ux*from,sz+uz*from},new double[]{sx+ux*to,sz+uz*to});
                boolean isTower=st.kind().equals("watchtower");
                if(!isTower||towerPal.add(st.palette())){double look=Math.min(to,from+40);leg.stops.add(new Stop(leg.id+"-start",2.0,sx+ux*look,66.0,sz+uz*look));}
                out.add(leg);sp++;
            }
            return out;
        }
        /** Task 8b: the lane of a supply house, from the lane end through the front gate to 4 blocks short of the yard centre, 2 blocks to the right of the centre line (the flag stands on it). Pictures: house-ID-spur (2 blocks in, toward the gate),
         *  house-ID-gate (10 blocks before the gate, at the flag) and house-ID-yard (at the end, at the front of the storehouse). The last stop holds the lane open until it is taken. */
        static Leg houseLeg(Spec.SupplyHousesRow h,double sx,double sz,double ux,double uz,double l){
            Spec.LandmarksRow r=null;for(var lm:Spec.LANDMARKS)if(lm.id().equals(h.landmark())&&lm.map()==h.map())r=lm;
            Vec3i size=null;if(r!=null)for(var pc:Spec.PIECES)if(pc.id().equals(r.template()))size=new Vec3i(pc.width(),pc.height(),pc.depth());
            double to=l-4.0;
            var leg=new Leg("house-"+h.id(),2.0,new double[]{sx,sz},new double[]{sx+ux*to,sz+uz*to});
            leg.holdEnd=true;
            if(r==null||size==null)return leg;
            Vec3d gate=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),15.5,3.0,36.5);
            Vec3d store=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),15.5,3.0,11.5);
            leg.stops.add(new Stop(leg.id+"-spur",2.0,gate.x,gate.y,gate.z));
            leg.stops.add(new Stop(leg.id+"-gate",Math.max(3.0,leg.arcOf(gate.x,gate.z)-10.0),h.yardX()+0.5,h.elevation()+5.0,h.yardZ()+0.5));
            leg.stops.add(new Stop(leg.id+"-yard",leg.len,store.x,store.y,store.z));
            return leg;
        }
        static Spec.StructuresRow deckNear(int x,int z){
            Spec.StructuresRow best=null;double bd=1e9;
            for(var s:MapRouteChecks.bridges()){double d=Math.hypot(x-s.x(),z-s.z());if(Math.abs(x-s.x())<=s.width()/2+12&&Math.abs(z-s.z())<=s.depth()/2+14&&d<bd){bd=d;best=s;}}
            return best;
        }
        /** A cell of a deck or of its two rail rows. */
        static boolean deckCell(int x,int z){
            for(var s:Spec.STRUCTURES)if(s.kind().equals("bridge")){int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;if(x>=x0&&x<x0+s.width()&&z>=z0-1&&z<=z0+s.depth())return true;}
            return false;
        }
        /** The cell on the plateau of a raised point: dmin to dmax blocks from its centre, where the ground is at the point's elevation, as far from the roads as it gets (the roads cut a gorge through the plateau) and near the middle of the window; null when there is none. */
        static double[] plateau(Spec.PointsRow p,double dmin,double dmax,int tol){
            double[] best=null;double bs=-1e9;
            for(int dx=-17;dx<=17;dx++)for(int dz=-17;dz<=17;dz++){
                double d=Math.hypot(dx,dz);if(d<dmin||d>dmax)continue;
                int x=p.x()+dx,z=p.z()+dz;
                int hh=Arena.height(x,z);if(Math.abs(hh-p.elevation())>tol)continue;
                double sc=Math.min(CampaignMaps.roadDistance(x,z),12.0)*10.0-Math.abs(d-(dmin+dmax)/2)+hh*6.0;
                if(sc>bs){bs=sc;best=new double[]{x+0.5,z+0.5};}
            }
            return best;
        }

        /** The jobs of the map in the order they are done: the camp, then every lane (the walker put at its start first), then the pictures that move the walker to where they are taken. */
        void assemble(){
            jobs.clear();var f=Spec.FACTIONS[0];double cx=f.spawnX(),cz=f.spawnZ();
            if(want("camp")){
                jobs.add(new ShotJob("camp-east",cx,cz,cx+120,66.5,cz,12,"the Dawn camp from the dais, down the muster lane to the east"));
                jobs.add(new ShotJob("camp-gate",cx,cz,cx-60,71.0,cz,12,"the Dawn camp from the dais, back to the gatehouse in the west"));
                var d=Spec.FACTIONS[1];double ex=d.spawnX(),ez=d.spawnZ();
                jobs.add(new ShotJob("camp-dusk-west",ex,ez,ex-120,66.5,ez,12,"the Dusk camp from the dais, down the muster lane to the west"));
                jobs.add(new ShotJob("camp-dusk-gate",ex,ez,ex+60,71.0,ez,12,"the Dusk camp from the dais, back to the gatehouse in the east"));
            }
            for(var leg:legs)if(want(leg.id)){
                jobs.add(new ShotJob(null,leg.q[0][0],leg.q[0][1],leg.q[1][0],66.0,leg.q[1][1],12,"start of "+leg.id));
                jobs.add(new LegJob(leg));
            }
            if(want("band"))for(var s:MapRouteChecks.bridges())for(var p:Spec.POINTS){
                if(Math.abs(p.x()-s.x())>3||Math.abs(p.z()-s.z())>3)continue;
                int z0=s.palette().equals("ford")?s.z()-3:s.z()-s.depth()/2;
                jobs.add(new ShotJob("band-"+p.id(),p.x()+0.5,z0+0.5,p.x()+0.5,p.elevation()+1.0,p.z()-p.radius()+0.5,12,"point "+p.id()+": the ring band across the water, from the north edge of the "+(s.palette().equals("ford")?"stone strip of the ford":"deck")));
            }
            if(want("rail"))for(var s:MapRouteChecks.bridges()){
                boolean ford=s.palette().equals("ford"); // a ford has no rails: its two pictures are of the edges of its stone strip
                int xw=s.x()-s.width()/2,xe=xw+s.width()-1,z0=s.z()-s.depth()/2,z1=z0+s.depth()-1; // plank rows z0..z1, the rails stand on z0-1 and z1+1
                if(ford){
                    int nz=s.z()-3,sz=s.z()+3;
                    jobs.add(new ShotJob("rail-north-"+s.id(),xw+1.5,nz+1.5,xe-2.0,65.8,nz-0.5,12,"crossing "+s.id()+" (ford): the north edge of the stone strip, from its west end, looking along it to the east"));
                    jobs.add(new ShotJob("rail-south-"+s.id(),xe-0.5,sz-0.5,xw+2.0,65.8,sz+1.5,12,"crossing "+s.id()+" (ford): the south edge of the stone strip, from its east end, looking along it to the west"));
                    continue;
                }
                double mx=s.x()+0.5; // the rails stand over the water; from 3 blocks inside the middle of the deck each end of each rail is looked at along the rail
                String tag=" ("+s.palette()+"): the ";
                jobs.add(new ShotJob("rail-nw-"+s.id(),mx+3.0,z0+1.5,xw+4.0,65.8,z0-0.5,12,"crossing "+s.id()+tag+"north rail seen along it toward its west end, from the middle of the deck"));
                jobs.add(new ShotJob("rail-ne-"+s.id(),mx-3.0,z0+1.5,xe-4.0,65.8,z0-0.5,12,"crossing "+s.id()+tag+"north rail seen along it toward its east end, from the middle of the deck"));
                jobs.add(new ShotJob("rail-sw-"+s.id(),mx+3.0,z1-0.5,xw+4.0,65.8,z1+1.5,12,"crossing "+s.id()+tag+"south rail seen along it toward its west end, from the middle of the deck"));
                jobs.add(new ShotJob("rail-se-"+s.id(),mx-3.0,z1-0.5,xe-4.0,65.8,z1+1.5,12,"crossing "+s.id()+tag+"south rail seen along it toward its east end, from the middle of the deck"));
            }
            if(want("posts")){ // the two corners of the bank at the middle crossing where, on Hollowwood, a post and the edge stone of the dressing stood in the outermost column of the river (the same two places on every map)
                Spec.StructuresRow mid=null;for(var s:MapRouteChecks.bridges())if(mid==null||Math.abs(s.z())<Math.abs(mid.z()))mid=s;
                if(mid!=null)for(var p:Spec.POINTS){
                    if(Math.abs(p.x()-mid.x())>3||Math.abs(p.z()-mid.z())>3)continue;
                    int z0=mid.z()-mid.depth()/2;
                    jobs.add(new ShotJob("posts-"+p.id()+"-nw",p.x()-15.5,z0+0.5,p.x()-10.5,p.elevation()+1.5,p.z()-14.5,12,"point "+p.id()+": the north-west corner of the bank, where a boundary post of the dressing stood over the river, from the north edge of the deck"));
                    jobs.add(new ShotJob("posts-"+p.id()+"-se",p.x()-9.5,z0+mid.depth()-0.5,p.x()+11.5,p.elevation()+1.0,p.z()+13.5,12,"point "+p.id()+": the south-east corner of the bank, where the edge stone of the dressing ran down to the water, from the south edge of the deck"));
                }
            }
            if(want("wall"))for(int side:new int[]{-1,1}){ // the places the gate stood 16 and 6 blocks before the wall of a barracks that the straight fan road ran into (the road bends now)
                var hall=campHall(side);if(hall==null)continue;
                int z=0;for(var s:MapRouteChecks.bridges())if(CampaignMaps.fanBend(side,s.z())!=null){z=s.z();break;}
                if(z==0)continue;
                var old=new Leg("old",2.0,new double[]{side*290,0},new double[]{side*205,z});
                double hit=old.len;for(double a=0;a<old.len;a+=0.25){var q=old.at(a);if(Walkability.inside(hall,(int)Math.floor(q[0]),(int)Math.floor(q[1]))){hit=a;break;}}
                var on=old.at(hit+1.5);String who=side<0?"dawn":"dusk";
                for(int back:new int[]{16,6}){var from=old.at(Math.max(0.0,hit-back));
                    jobs.add(new ShotJob("wall-"+who+"-"+back,from[0],from[1],on[0],66.0,on[1],12,"the wall of the "+who+" barracks at ("+Math.round(on[0])+", "+Math.round(on[1])+"), where the straight fan road used to run into it, "+back+" blocks before it on the old line"));}
            }
            if(want("hall"))for(var s:Spec.STRUCTURES)if(s.kind().equals("barracks")){ // 6 blocks east of the lantern, on the floor, looking up at it
                int y=s.elevation()+1;
                jobs.add(new ShotJob("hall-"+s.id(),s.x()+6.5,s.z()+0.5,s.x()+0.5,y+4.5,s.z()+0.5,12,"inside the "+s.id()+" ("+s.width()+" by "+s.depth()+" by "+s.height()+") 6 blocks east of its lantern, looking up at it",y));
            }
            if(want("plaza")){ // the floor round the pole of the first two points on dry land: from the road 10 blocks out at the foot of the pole, and from 6 blocks out looking down at the floor 3 blocks ahead
                int shots=0;
                for(var p:Spec.POINTS){
                    if(shots>=2||Math.abs(p.x()-Arena.riverX(p.z()))<14)continue;
                    double[] cam=null;double bestRoad=1e9;
                    for(int k=0;k<8;k++){
                        double a=k*Math.PI/4,qx=p.x()+Math.cos(a)*10.0,qz=p.z()+Math.sin(a)*10.0;int ix=(int)Math.floor(qx),iz=(int)Math.floor(qz);
                        if(owner(qx,qz)!=null||deckCell(ix,iz)||Arena.river(ix,iz)||Math.abs(Arena.height(ix,iz)-p.elevation())>1)continue;
                        double rd=CampaignMaps.roadDistance(ix,iz);if(rd<bestRoad){bestRoad=rd;cam=new double[]{Math.cos(a),Math.sin(a)};}
                    }
                    if(cam==null)continue;
                    shots++;double e=p.elevation()+1.0;
                    jobs.add(new ShotJob("plaza-"+p.id()+"-far",p.x()+0.5+cam[0]*10.0,p.z()+0.5+cam[1]*10.0,p.x()+0.5,e,p.z()+0.5,12,"point "+p.id()+": the plaza floor from the road 10 blocks out, at the foot of the pole"));
                    jobs.add(new ShotJob("plaza-"+p.id()+"-down",p.x()+0.5+cam[0]*6.0,p.z()+0.5+cam[1]*6.0,p.x()+0.5+cam[0]*3.0,e,p.z()+0.5+cam[1]*3.0,12,"point "+p.id()+": the plaza floor from 6 blocks out, looking down at the floor 3 blocks from the pole"));
                }
            }
            if(want("edge"))for(var l:legs){ // the edge of an open road: from the grass three blocks off it, 60 blocks along lane1, looking along the edge
                if(!l.id.equals("lane1"))continue;
                double s0=Math.min(60.0,l.len/3.0);double[] a=l.at(s0),c=l.at(s0+8.0);
                double ux=c[0]-a[0],uz=c[1]-a[1],ul=Math.hypot(ux,uz);if(ul<1e-6)continue;
                ux/=ul;uz/=ul;double nx=-uz,nz=ux,edge=5.0;
                for(double k=0;k<=12;k+=0.25)if(CampaignMaps.roadDistance((int)Math.floor(a[0]+nx*k),(int)Math.floor(a[1]+nz*k))>=0){edge=k;break;}
                double[] t=l.at(Math.min(l.len,s0+60.0));
                jobs.add(new ShotJob("edge-lane1",a[0]+nx*(edge+3.2),a[1]+nz*(edge+3.2),t[0]+nx*edge,66.0,t[1]+nz*edge,12,"lane1: the edge of the open road from the grass 3 blocks off it, "+(int)Math.round(s0)+" blocks along the lane, looking along the edge 60 blocks ahead"));
            }
            if(want("outpost"))for(var p:Spec.POINTS)if(p.elevation()>Arena.Y){
                Leg on=null;for(var l:legs)if(on==null&&l.nearest(p.x(),p.z())[1]<=3.5)on=l;
                if(on!=null){double[] a=on.at(on.arcOf(p.x(),p.z())+9.0);jobs.add(new ShotJob("outpost-"+p.id()+"-under",a[0],a[1],p.x()+0.5,p.elevation()+0.5,p.z()+0.5,12,"point "+p.id()+": from the road 9 blocks past the centre, up at the pad and the ring"));}
                double[] near=plateau(p,5.5,9.0,6);
                if(near!=null)jobs.add(new ShotJob("outpost-"+p.id()+"-near",near[0],near[1],p.x()+0.5,p.elevation()+1.5,p.z()+0.5,12,"point "+p.id()+": from the shoulder of the plateau, 6 to 9 blocks out, at the pad"));
            }
            if(want("ramp"))for(var p:Spec.POINTS)if(p.elevation()>Arena.Y){ // the way up to a raised point, from where the climb begins (19 blocks of plateau and 48 of climb out), looking at the pole
                Leg on=null;for(var l:legs)if(on==null&&l.nearest(p.x(),p.z())[1]<=3.5)on=l;
                if(on==null)continue;
                double[] a=on.at(Math.max(0.5,on.arcOf(p.x(),p.z())-58.0));
                jobs.add(new ShotJob("ramp-"+p.id(),a[0],a[1],p.x()+0.5,p.elevation()+3.0,p.z()+0.5,12,"point "+p.id()+": the road up to the outpost from 58 blocks out, where the climb begins, looking at the pole"));
            }
            if(want("float")){
                int shots=0;var done=new HashSet<String>();
                for(var pc:pieces){
                    if(shots>=4||pc.cam()==null)continue;
                    boolean atOutpost=false;for(var p:Spec.POINTS)if(p.elevation()>Arena.Y&&Math.hypot(pc.cx()-p.x(),pc.cz()-p.z())<=p.radius()+6)atOutpost=true;
                    if(atOutpost||!done.add(pc.top()+"/"+pc.size()))continue;
                    shots++;
                    jobs.add(new ShotJob("float-"+shots,pc.cam()[0],pc.cam()[2],pc.cx(),pc.cy(),pc.cz(),12,"a floating piece of "+pc.size()+" blocks ("+pc.top()+") at x "+pc.x0()+".."+pc.x1()+", y "+pc.y0()+".."+pc.y1()+", z "+pc.z0()+".."+pc.z1(),pc.cam()[1]));
                }
            }
            if(want("building")){
                int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ();
                for(var s:Spec.STRUCTURES){
                    String id=s.id();
                    if(s.kind().equals("bridge")||id.matches("house_\\d|farm|mill|(dawn|dusk)_gate")||id.contains("_tower_")||id.contains("-supply-"))continue; // those are seen from their roads, the gates from the dais, the supply houses from their lanes (house-ID)
                    double R=Math.max(s.width(),s.depth())/2.0+12.0,bs=1e9,bx=0,bz=0;boolean found=false;
                    for(int k=0;k<16;k++){
                        double a=k*Math.PI/8,x=Math.floor(s.x()+Math.cos(a)*R)+0.5,z=Math.floor(s.z()+Math.sin(a)*R)+0.5;int ix=(int)Math.floor(x),iz=(int)Math.floor(z);
                        if(Math.abs(x)>hx-12||Math.abs(z)>hz-12||owner(x,z)!=null||deckCell(ix,iz)||Arena.river(ix,iz))continue;
                        int g=Arena.height(ix,iz);if(g<Arena.Y-1||g>s.elevation()+3)continue;
                        double rd=CampaignMaps.roadDistance(ix,iz); // as near a road as it gets: where a walker would be
                        if(rd<bs){bs=rd;bx=x;bz=z;found=true;}
                    }
                    if(found)jobs.add(new ShotJob("building-"+id,bx,bz,s.x()+0.5,s.elevation()+1.0+Math.min(s.height(),9)/2.0,s.z()+0.5,12,"the "+s.kind()+" "+id+" ("+s.width()+" by "+s.depth()+" by "+s.height()+", "+s.palette()+") from "+(int)Math.round(R)+" blocks out, "+(int)Math.round(bs)+" blocks off the nearest road"));
                }
            }
            if(want("sight")){
                int i=0;
                for(var s:sights){
                    i++;double ux=-Math.sin(Math.toRadians(s.yaw())),uz=Math.cos(Math.toRadians(s.yaw()));
                    jobs.add(new ShotJob("sight-"+i,s.x(),s.z(),s.x()+ux*100,s.top()+0.12,s.z()+uz*100,28,String.format(Locale.ROOT,"sight line %d: %.0f blocks clear at eye height from (%.1f, %.1f) toward yaw %.0f, ends at (%.0f, %.0f, %.0f) on %s",i,s.len(),s.x(),s.z(),s.yaw(),s.ex(),s.ey(),s.ez(),s.by())));
                }
            }
        }

        // ---- the scans, on the server thread ----
        record Sight(double len,double x,double z,double yaw,double ex,double ey,double ez,int road,int sign,double top,String by){}
        /** Top blocks that cannot belong to this map: the snow and ice of Frostmarch, the podzol, ferns and mud bricks (blocks, stairs, slabs and walls) of Hollowwood, grass on the two maps without it, the leaves of the other family of trees. */
        static Set<String> alien(int map){
            var s=new HashSet<String>();
            if(map!=2)s.addAll(List.of("snow","snow_block","powder_snow","packed_ice","blue_ice","ice"));
            if(map!=3)s.addAll(List.of("podzol","fern","mud_bricks","mud_brick_stairs","mud_brick_slab","mud_brick_wall"));
            if(map==2||map==3)s.addAll(List.of("grass_block","grass","poppy"));
            if(map<2)s.add("spruce_leaves");else if(map<4)s.add("oak_leaves");
            return s;
        }
        void scanMap(GravesAndGlory.Battle b,int map,List<Leg> mine){
            var w=b.world;
            // 1. the top block of every column of the arena against what this map may be made of (a block of the map built before, left standing, would show here)
            int hx=GravesAndGlory.R.arenaHalfX(),hz=GravesAndGlory.R.arenaHalfZ(),foreign=0;var bad=alien(map);var tops=new TreeMap<String,Integer>();var firsts=new ArrayList<String>();
            for(int cx=Math.floorDiv(-hx,16);cx<=Math.floorDiv(hx,16);cx++)for(int cz=Math.floorDiv(-hz,16);cz<=Math.floorDiv(hz,16);cz++){
                w.getChunk(cx,cz);
                for(int x=Math.max(-hx,cx*16);x<=Math.min(hx,cx*16+15);x++)for(int z=Math.max(-hz,cz*16);z<=Math.min(hz,cz*16+15);z++){
                    int y=w.getTopY(net.minecraft.world.Heightmap.Type.WORLD_SURFACE,x,z)-1;
                    String nm=name(w.getBlockState(new BlockPos(x,y,z)));tops.merge(nm,1,Integer::sum);
                    if(bad.contains(nm)){foreign++;if(firsts.size()<8)firsts.add(nm+" ("+x+", "+y+", "+z+")");}
                }
            }
            var common=new ArrayList<>(tops.entrySet());common.sort((p,q)->q.getValue()-p.getValue());
            var top10=new StringBuilder();for(int i=0;i<Math.min(10,common.size());i++)top10.append(i>0?", ":"").append(common.get(i).getKey()).append(' ').append(common.get(i).getValue());
            report("p3-wt"+map+"-foreign",foreign==0,"top blocks of the "+(2*hx+1)+" by "+(2*hz+1)+" columns: "+top10+"; blocks that belong to another map: "+foreign+(foreign==0?"":", first "+firsts));
            // 2. the ring of every point: its band over the river, what hangs in the air, what stands in the river
            var wet=new LinkedHashMap<String,Integer>();var stand=new ArrayList<int[]>();
            for(int pi=0;pi<Spec.POINTS.length;pi++){
                var p=Spec.POINTS[pi];int r=p.radius(),e=p.elevation(),cells=0,over=0,hang=0,pad=0,padHang=0,minX=9999,maxX=-9999,minZ=9999,maxZ=-9999;var inRiver=new ArrayList<String>();
                for(int dx=-r-5;dx<=r+5;dx++)for(int dz=-r-5;dz<=r+5;dz++){
                    int x=p.x()+dx,z=p.z()+dz;double d=Math.hypot(dx,dz);
                    if(d>=r-1&&d<=r&&w.getBlockState(new BlockPos(x,e,z)).isOf(Blocks.SMOOTH_STONE)){
                        cells++;
                        if(w.getBlockState(new BlockPos(x,e-1,z)).isAir())hang++;
                        if(Arena.river(x,z)&&!deckCell(x,z)&&w.getBlockState(new BlockPos(x,e-1,z)).isOf(Blocks.WATER)){over++;minX=Math.min(minX,x);maxX=Math.max(maxX,x);minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);}
                    }
                    if(Math.abs(dx)<=1&&Math.abs(dz)<=1){pad++;if(w.getBlockState(new BlockPos(x,e-1,z)).isAir())padHang++;}
                    if(d>2.5&&Arena.river(x,z)&&!deckCell(x,z)&&owner(x,z)==null)for(int y=Arena.Y-2;y<=Arena.Y+9;y++){
                        var st=w.getBlockState(new BlockPos(x,y,z));
                        if(st.isAir()||st.isOf(Blocks.WATER)||st.isOf(Blocks.SMOOTH_STONE))continue;
                        inRiver.add(name(st)+" ("+x+", "+y+", "+z+")");stand.add(new int[]{x,y,z,pi});
                    }
                }
                wet.put(p.id(),over);
                note("point "+p.id()+" "+p.name()+" ("+p.x()+", "+p.z()+") elevation "+e+", ground at its centre "+Arena.height(p.x(),p.z())+": ring band "+cells+" blocks, "+over+" over river water"+(over==0?"":" (x "+minX+" to "+maxX+", z "+minZ+" to "+maxZ+")")+", "+hang+" with air under; 3x3 pad "+pad+" blocks, "+padHang+" with air under; blocks standing in the river within "+(r+5)+" of the centre: "+inRiver.size()+(inRiver.isEmpty()?"":" "+inRiver.subList(0,Math.min(12,inRiver.size()))));
            }
            bandWater=wet;standing=stand;
            report("p3-wt"+map+"-posts",stand.isEmpty(),"blocks standing in a river column within 22 blocks of a capture point's centre (off the decks and the buildings, smooth stone excepted): "+stand.size()+(stand.isEmpty()?"":", first "+stand.stream().limit(6).map(q->"("+q[0]+", "+q[1]+", "+q[2]+") of point "+Spec.POINTS[q[3]].id()).toList()));
            // 3. the check of the river columns from Task 2, run again on the map as it stands
            var rv=Rebuild.river(b);
            report("p3-wt"+map+"-river",rv.left()==0,"river columns "+rv.columns()+", built on by this map "+rv.builtOn()+", looked at "+rv.looked()+": "+rv.left()+" blocks above the water that this map does not account for"+(rv.left()==0?"":", first: "+rv.sample()));
            Rebuild.built(b,map,k->"p3-wt"+map+"-"+k);
            // 4. the ground along the line each lane is walked on
            for(var leg:mine){
                int steps=0,wetCells=0,cells=0;double last=Double.NaN,lo=1e9,hi=-1e9;var where=new StringBuilder();
                for(double s=0;s<=leg.len;s+=1.0){
                    var a=leg.at(s);int bx=(int)Math.floor(a[0]),bz=(int)Math.floor(a[1]);
                    w.getChunk(bx>>4,bz>>4);int top=w.getTopY(FEET,bx,bz);cells++;
                    if(w.getBlockState(new BlockPos(bx,top-1,bz)).isOf(Blocks.WATER))wetCells++;
                    lo=Math.min(lo,top);hi=Math.max(hi,top);
                    if(!Double.isNaN(last)&&Math.abs(top-last)>=1.0){steps++;if(steps<=6)where.append(steps>1?"; ":"").append((int)last).append(" to ").append(top).append(" at (").append(bx).append(", ").append(bz).append(")");}
                    last=top;
                }
                note("profile of "+leg.id+": "+cells+" cells of the lane line, ground (feet) y "+(int)lo+" to "+(int)hi+", "+steps+" changes of a block or more"+(steps==0?"":": "+where)+", "+wetCells+" cells on water");
            }
            // 5. sight lines: from a point of a road, 2 blocks to the right of it, looking along the road at eye height, how far is it clear (blocks that stop a camera stop the ray)
            var ent=b.server.getPlayerManager().getPlayerList().get(0);var cands=new ArrayList<Sight>();int ri=0;
            for(var r:CampaignMaps.roads){
                double dx=r.bx()-r.ax(),dz=r.bz()-r.az(),len=Math.hypot(dx,dz);
                if(len>=100&&r.width()>=4)for(int sign:new int[]{1,-1}){
                    double ux=dx/len*sign,uz=dz/len*sign,ox=-uz*2.0,oz=ux*2.0,sx0=sign>0?r.ax():r.bx(),sz0=sign>0?r.az():r.bz();
                    for(double s=4;s<=len-30;s+=12){
                        double x=sx0+ux*s+ox,z=sz0+uz*s+oz;int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
                        if(Math.abs(x)>hx-8||Math.abs(z)>hz-8)continue;
                        w.getChunk(bx>>4,bz>>4);int top=w.getTopY(FEET,bx,bz);
                        if(top>Arena.Y+2||w.getBlockState(new BlockPos(bx,top-1,bz)).isOf(Blocks.WATER))continue; // a walker stands on the road, not on a roof or in the river
                        double tx=ux>0?(hx-x)/ux:ux<0?(-hx-x)/ux:1e9,tz=uz>0?(hz-z)/uz:uz<0?(-hz-z)/uz:1e9,reach=Math.min(700.0,Math.min(tx,tz)); // the line stops at the edge of the arena
                        var from=new Vec3d(x,top+1.62,z);var to=new Vec3d(x+ux*reach,top+1.62,z+uz*reach);
                        var hit=w.raycast(new net.minecraft.world.RaycastContext(from,to,net.minecraft.world.RaycastContext.ShapeType.VISUAL,net.minecraft.world.RaycastContext.FluidHandling.NONE,ent));
                        boolean miss=hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS;var end=miss?to:hit.getPos();
                        cands.add(new Sight(from.distanceTo(end),x,z,Math.toDegrees(Math.atan2(-ux,uz)),end.x,end.y,end.z,ri,sign,top,miss?"nothing before the edge of the arena":name(w.getBlockState(hit.getBlockPos()))));
                    }
                }
                ri++;
            }
            cands.sort((p,q)->Double.compare(q.len(),p.len()));
            var pick=new ArrayList<Sight>();
            for(var cd:cands){
                if(pick.isEmpty()){pick.add(cd);continue;}
                var first=pick.get(0);
                if(Math.hypot(cd.x()-first.x(),cd.z()-first.z())>=120&&cd.road()!=first.road()){pick.add(cd);break;}
            }
            if(pick.size()==1&&cands.size()>1)pick.add(cands.get(1));
            sights=pick;
            for(int i=0;i<pick.size();i++){var cd=pick.get(i);note(String.format(Locale.ROOT,"sight line %d: %.0f blocks clear at eye height from (%.1f, %.1f) toward yaw %.0f, ends at (%.0f, %.0f, %.0f) on %s (%d candidates, best %.0f)",i+1,cd.len(),cd.x(),cd.z(),cd.yaw(),cd.ex(),cd.ey(),cd.ez(),cd.by(),cands.size(),cands.isEmpty()?0.0:cands.get(0).len()));}
            // 6. blocks that float: every solid block of the arena that cannot be reached from the floor of the scan through blocks that touch face to face
            floating(w,hx,hz,ent);
            scanned=true;
        }
        /** Solid for this scan: any block but air, water, lava and the invisible ones (barrier, light, structure void). */
        static boolean solid(BlockState st){return !st.isAir()&&!st.isOf(Blocks.WATER)&&!st.isOf(Blocks.LAVA)&&!st.isOf(Blocks.BARRIER)&&!st.isOf(Blocks.LIGHT)&&!st.isOf(Blocks.STRUCTURE_VOID);}
        /** A piece that floats: its blocks, its box, and its three commonest blocks. */
        record Piece(int size,int x0,int y0,int z0,int x1,int y1,int z1,String top,double[] cam){
            double cx(){return (x0+x1)/2.0+0.5;}double cy(){return (y0+y1)/2.0+0.5;}double cz(){return (z0+z1)/2.0+0.5;}
        }
        volatile List<Piece> pieces=List.of();
        /** What floatBox found: the floating pieces (largest first), the solid blocks that reach the floor, and the blocks of the floating pieces. */
        record FloatScan(List<Piece> found,long reached,long floatTotal){}
        /** The reached-from-floor scan of a box (x0..x1, z0..z1, y0..y1, all included): the solid blocks of the layer y0 are the floor, a solid block that touches (face, edge or corner: a tent of wool rows meets at its edges) a block that reaches the floor
         *  reaches it too, and a solid block that does not belongs to a floating piece. Shared by the map walk (the arena) and the camp walk (the island). Server thread. */
        static FloatScan floatBox(ServerWorld w,int x0,int x1,int z0,int z1,int y0,int y1){
            int wx=x1-x0+1,wz=z1-z0+1,hy=y1-y0+1,cx0=x0>>4,cz0=z0>>4,ncx=(x1>>4)-cx0+1,ncz=(z1>>4)-cz0+1;
            var chunks=new net.minecraft.world.chunk.Chunk[ncx*ncz];
            for(int a=0;a<ncx;a++)for(int c=0;c<ncz;c++)chunks[a*ncz+c]=w.getChunk(cx0+a,cz0+c);
            var seen=new BitSet(wx*wz*hy);var pos=new BlockPos.Mutable();
            int[] st=new int[1<<20];int sp=0;long reached=0;
            int[] dx=new int[26],dy=new int[26],dz=new int[26];int q=0;
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)for(int c=-1;c<=1;c++)if(a!=0||b!=0||c!=0){dx[q]=a;dy[q]=b;dz[q]=c;q++;}
            for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
                var ch=chunks[((x>>4)-cx0)*ncz+((z>>4)-cz0)];
                if(solid(ch.getBlockState(pos.set(x,y0,z)))){int id=((x-x0)*wz+(z-z0))*hy;if(!seen.get(id)){seen.set(id);if(sp==st.length)st=Arrays.copyOf(st,sp*2);st[sp++]=id;}}
            }
            while(sp>0){
                int id=st[--sp];reached++;int y=id%hy+y0,r=id/hy,z=r%wz+z0,x=r/wz+x0;
                for(int k=0;k<26;k++){
                    int nx=x+dx[k],ny=y+dy[k],nz=z+dz[k];
                    if(nx<x0||nx>x1||nz<z0||nz>z1||ny<y0||ny>y1)continue;
                    int nid=((nx-x0)*wz+(nz-z0))*hy+(ny-y0);if(seen.get(nid))continue;
                    if(!solid(chunks[((nx>>4)-cx0)*ncz+((nz>>4)-cz0)].getBlockState(pos.set(nx,ny,nz))))continue;
                    seen.set(nid);if(sp==st.length)st=Arrays.copyOf(st,sp*2);st[sp++]=nid;
                }
            }
            var found=new ArrayList<Piece>();long floatTotal=0;
            for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)for(int y=y0;y<=y1;y++){
                int id=((x-x0)*wz+(z-z0))*hy+(y-y0);if(seen.get(id))continue;
                if(!solid(chunks[((x>>4)-cx0)*ncz+((z>>4)-cz0)].getBlockState(pos.set(x,y,z))))continue;
                seen.set(id);sp=0;st[sp++]=id;int size=0,bx0=x,by0=y,bz0=z,bx1=x,by1=y,bz1=z;var names=new TreeMap<String,Integer>();
                while(sp>0){
                    int cid=st[--sp];size++;int cy=cid%hy+y0,r=cid/hy,cz=r%wz+z0,cx=r/wz+x0;
                    bx0=Math.min(bx0,cx);bx1=Math.max(bx1,cx);by0=Math.min(by0,cy);by1=Math.max(by1,cy);bz0=Math.min(bz0,cz);bz1=Math.max(bz1,cz);
                    names.merge(name(chunks[((cx>>4)-cx0)*ncz+((cz>>4)-cz0)].getBlockState(pos.set(cx,cy,cz))),1,Integer::sum);
                    for(int k=0;k<26;k++){
                        int nx=cx+dx[k],ny=cy+dy[k],nz=cz+dz[k];
                        if(nx<x0||nx>x1||nz<z0||nz>z1||ny<y0||ny>y1)continue;
                        int nid=((nx-x0)*wz+(nz-z0))*hy+(ny-y0);if(seen.get(nid))continue;
                        if(!solid(chunks[((nx>>4)-cx0)*ncz+((nz>>4)-cz0)].getBlockState(pos.set(nx,ny,nz))))continue;
                        seen.set(nid);if(sp==st.length)st=Arrays.copyOf(st,sp*2);st[sp++]=nid;
                    }
                }
                floatTotal+=size;var topNames=new ArrayList<>(names.entrySet());topNames.sort((p,o)->o.getValue()-p.getValue());
                var tn=new StringBuilder();for(int i=0;i<Math.min(3,topNames.size());i++)tn.append(i>0?", ":"").append(topNames.get(i).getKey()).append(' ').append(topNames.get(i).getValue());
                found.add(new Piece(size,bx0,by0,bz0,bx1,by1,bz1,tn.toString(),null));
            }
            found.sort((p,o)->o.size()-p.size());
            return new FloatScan(found,reached,floatTotal);
        }
        /** The floor of the scan is Arena.Y-8, its top Arena.Y+80 (see floatBox); the pieces are counted and listed, and the first ones get a place to stand for their pictures. */
        void floating(ServerWorld w,int hx,int hz,net.minecraft.entity.Entity ent){
            int y0=Arena.Y-8,y1=Arena.Y+80;
            var scan=floatBox(w,-hx,hx,-hz,hz,y0,y1);var found=scan.found();long reached=scan.reached(),floatTotal=scan.floatTotal();
            // a place to stand for the pictures of the first pieces: 3 to 10 blocks off, on a floor under the piece, with a clear ray from the eye to the piece
            var sigs=new HashSet<String>();
            for(int i=0;i<found.size()&&sigs.size()<6;i++){
                var pc=found.get(i);if(!sigs.add(pc.top()+"/"+pc.size()))continue;
                double bestScore=1e9;double[] cam=null;var to=new Vec3d(pc.cx(),pc.cy(),pc.cz());
                for(int ring=3;ring<=10;ring++)for(int k=0;k<24;k++){
                    double a=k*Math.PI/12;int x=(int)Math.floor(pc.cx()+Math.cos(a)*ring),z=(int)Math.floor(pc.cz()+Math.sin(a)*ring);
                    if(Math.abs(x)>hx-4||Math.abs(z)>hz-4)continue;
                    int fy=Integer.MIN_VALUE;
                    for(int y=Math.min(pc.y0(),Arena.Y+14);y>=Arena.Y-1;y--){
                        if(solid(w.getBlockState(new BlockPos(x,y-1,z)))&&!solid(w.getBlockState(new BlockPos(x,y,z)))&&!solid(w.getBlockState(new BlockPos(x,y+1,z)))&&!w.getBlockState(new BlockPos(x,y-1,z)).isOf(Blocks.WATER)){fy=y;break;}
                    }
                    if(fy==Integer.MIN_VALUE||w.getBlockState(new BlockPos(x,fy,z)).isOf(Blocks.WATER))continue;
                    var from=new Vec3d(x+0.5,fy+1.62,z+0.5);
                    var hit=w.raycast(new net.minecraft.world.RaycastContext(from,to,net.minecraft.world.RaycastContext.ShapeType.VISUAL,net.minecraft.world.RaycastContext.FluidHandling.NONE,ent));
                    boolean clear=hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS;
                    if(!clear){var hp=hit.getBlockPos();clear=hp.getX()>=pc.x0()&&hp.getX()<=pc.x1()&&hp.getY()>=pc.y0()&&hp.getY()<=pc.y1()&&hp.getZ()>=pc.z0()&&hp.getZ()<=pc.z1();}
                    if(!clear)continue;
                    double score=Math.abs(ring-6)+(fy>pc.y0()-2?0:Math.max(0,pc.y0()-fy-6)*0.5);
                    if(score<bestScore){bestScore=score;cam=new double[]{x+0.5,fy,z+0.5};}
                }
                found.set(i,new Piece(pc.size(),pc.x0(),pc.y0(),pc.z0(),pc.x1(),pc.y1(),pc.z1(),pc.top(),cam));
            }
            pieces=found;
            report("p3-wt"+n+"-floating",found.isEmpty(),"solid blocks reached from the floor "+reached+", floating pieces "+found.size()+" of "+floatTotal+" blocks"+(found.isEmpty()?"":", the biggest "+found.stream().limit(6).map(p->p.size()+" blocks x "+p.x0()+".."+p.x1()+" y "+p.y0()+".."+p.y1()+" z "+p.z0()+".."+p.z1()+" ("+p.top()+")").toList()));
            for(int i=0;i<Math.min(40,found.size());i++){var p=found.get(i);note("floating piece "+(i+1)+": "+p.size()+" blocks, x "+p.x0()+" to "+p.x1()+", y "+p.y0()+" to "+p.y1()+", z "+p.z0()+" to "+p.z1()+": "+p.top());}
        }
    }

    /** Task 3, the camp walkthrough gate (suite campwalk): the camp as a commander meets it, walked by a real client player (movement keys held, not teleports) and photographed from his eyes. The camp stands as the world builds it
     *  (the client starts the world, CampWorld.build runs) and the walker arrives through Campaign.toLobby. He walks (no sprint: doors and stairs are narrow) four legs, in sub-legs that begin where the one before ended (a sub-leg never turns back on itself):
     *  leg 1 (1-arrival) from the spawn plinth north up the avenue to the parade standard; leg 2 (2a-archive, 2b-founders) east along the cross way to the archive, through its west door to the Archivist, out again and round to the court of the Founders' Stone;
     *  leg 3 (3a-steward, 3b-west-pad, 3c-east-pad, 3d-arch) back across the parade to the Steward, both ready pads (he stands on each for about 5 s: a match starts after 10 s of ready), and the arch to the portcullis; leg 4 (4a-ridge-foot, 4b-west-stair, 4c-plateau,
     *  4d-tent, 4e-quartermaster, 4f-east-stair) across the bare bowl to the foot of the west stair, up it, along the plateau into the command tent, round the war table to the Quartermaster, and down the east stair.
     *  After the legs: the arrival, parade, gates, pads and tent views again at dusk and at night (the world time is set; lanterns, braziers), the two flanks and the edges of the island seen from the ground, and the flight views (spectator camera: both pads from above,
     *  the war table, the island from outside). Pictures are screenshots/p3-cw-<NN>-<what>.png, each with a check line p3-cw-<NN>-<what> that says where the walker stood and looked.
     *  Checks: p3-cw-leg-<id> (the lane finished, no stuck window of 2 s with under 3 blocks gained, no forced step), p3-cw-walk (all legs: stuck windows, forced steps, auto-jumps, blocks walked), p3-cw-pad-west and p3-cw-pad-east (the walker stood on the pad: his feet inside it,
     *  smooth quartz under him, Campaign.onPad has his team), p3-cw-stairs (the west flight climbed to feet 77 and the east flight descended to feet 65, no forced step), p3-cw-floating (floatBox over the island box and the rim rows, y from minY: no solid block that does not reach the
     *  floor), p3-cw-lantern (every lantern of the box could be placed where it hangs: LanternBlock.canPlaceAt), p3-cw-barrier (the 21 barriers are the arch's and no other barrier stands in the box), p3-cw-entities (no camp entity inside a block that stops him: villagers by their
     *  collision box, labels by the cell they stand in), p3-cw-labels (every label 3.7 above its row and, from the place the walker photographed it, all five sample points of its rectangle in sight; no two labels overlap in his view), p3-cw-pictures (the files exist),
     *  p3-cw-complete (every id of the walk was reported; only when no filter was given) and p3-cw-suite.
     *  GRAVES_WALK_ONLY lists name fragments: leg1, leg2, leg3b, leg4, night, flank, fly (nothing given: all). A leg that is run alone starts with the walker put at its first point (a note says so). GRAVES_CW_SABOTAGE=1 plants one defect for each scan check
     *  (a floating block, a loose lantern, a barrier on the parade, an armor stand in the floor, the Archivist's label moved up into the roof), a wall across the avenue, the quartz floors of both pads turned to stone and a wall across the west stair, to show that the checks go red; use it with GRAVES_WALK_ONLY=leg1,leg3b,leg3c,leg4b,scan. */
    static final class CampWalk extends Walkthrough {
        static final Spec.CampRow C=Spec.CAMP[0];
        static final int OX=C.originX(),OZ=C.originZ();
        /** The feet of a walker on the floor of the bowl and on the plateau. */
        static final double GROUND=C.floorY()+1,PLATEAU=C.plateauY()+1;
        final boolean sabotage="1".equals(System.getenv("GRAVES_CW_SABOTAGE"));
        final List<Leg> mine=new ArrayList<>();
        final Set<String> ids=Collections.synchronizedSet(new LinkedHashSet<>());
        volatile boolean scanned,arrived,labelsDone;
        final double[][] padFeet=new double[2][];final boolean[] padQuartz=new boolean[2];final int[] padOn={-9,-9},padCount={-9,-9};
        double stairTop=Double.NaN,stairFoot=Double.NaN;
        final List<double[]> labelViews=Collections.synchronizedList(new ArrayList<>());
        volatile Map<String,double[]> spots=Map.of();
        volatile String scanSummary="";
        float[] labelPx=new float[0];
        int pictureWait;

        @Override String pre(){return "cw";}
        @Override boolean legOk(Leg leg){return leg.finished&&leg.stuck==0&&leg.forced==0;}
        @Override public int limit(){return 40000;}
        volatile int savedCleared=-1;
        @Override public void cleanup(MinecraftClient c){super.cleanup(c);UUID me=c.player==null?null:c.player.getUuid();server(c,b->{b.world.setTimeOfDay(3000);if(me!=null&&savedCleared>=0){b.campaign.profile(me).campaignCleared=savedCleared;savedCleared=-1;}});}
        /** One check line, remembered by id (p3-cw-complete wants them all). */
        void ck(String id,boolean pass,String why){ids.add(id);report(id,pass,why);}
        boolean ran(String group){return only.isEmpty()||want(group);}
        /** Whether the lanes whose id begins with the prefix (leg3, leg3b, ...) are run in this launch. */
        boolean ranLeg(String prefix){return only.isEmpty()||only.stream().anyMatch(f->f.startsWith(prefix)||prefix.startsWith(f));}
        @Override void legDone(Leg leg){super.legDone(leg);ids.add("p3-cw-leg-"+leg.id);}

        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            if(tick==20)setup(c);
            switch(stage){
                case 0->{ // the arrival: through Campaign.toLobby, as the camp does it for a commander
                    UUID me=c.player.getUuid();arrived=false;
                    server(c,x->{x.campaign.leash=true;x.world.setTimeOfDay(3000);var sp=x.server.getPlayerManager().getPlayer(me);x.clearOldUnits();var prof=x.campaign.profile(me);if(savedCleared<0)savedCleared=prof.campaignCleared;prof.campaignCleared=Math.max(1,savedCleared);x.campaign.toLobby(sp);arrived=true;});
                    stage=1;waited=0;
                }
                case 1->{
                    var s=CampWorld.spawn(null);var p=c.player; // null profile: the normal spawn, which the walker is sent to with campaignCleared raised to 1 (restored in cleanup)
                    if(arrived&&++waited>=40&&p.squaredDistanceTo(s.x(),s.y(),s.z())<1.0&&terrainReady(c,p.getX(),p.getZ())){
                        ck("p3-cw-arrival",true,String.format(Locale.ROOT,"after Campaign.toLobby the walker stands at (%.2f, %.2f, %.2f), yaw %.0f, pitch %.0f; the spawn row says (%.2f, %.2f, %.2f)",p.getX(),p.getY(),p.getZ(),p.getYaw(),p.getPitch(),s.x(),s.y(),s.z()));
                        scanned=false;server(c,x->scan(x));stage=2;waited=0;
                    }else if(waited>600){ck("p3-cw-arrival",false,"the walker is not at the spawn after "+waited+" ticks, he stands at ("+p.getX()+", "+p.getY()+", "+p.getZ()+")");finish(c,"cw");stage=9;}
                }
                case 2->{if(scanned){labelPx=measure(c);assemble();stage=3;}}
                case 3->{
                    if(job==null){job=jobs.poll();if(job==null){stage=4;return;}}
                    if(job.step(c)){if(job instanceof LegJob lj)legDone(lj.leg);job=null;}
                }
                case 4->{release(c);labelsDone=false;final float[] px=labelPx;server(c,x->labels(x,px));stage=5;waited=0;}
                case 5->{if(labelsDone&&++waited>=60){summary(c);stage=6;pictureWait=0;}}
                case 6->{if(++pictureWait>=80){pictures(c);finish(c,"cw");stage=9;}}
                default->{}
            }
        }

        // ---- the lanes ----
        Leg lane(String id,double[]... pts){var l=new Leg(id,0.0,pts);l.sprint=false;l.holdEnd=true;mine.add(l);return l;}
        /** A stop at the point (x,z) of the lane: the walker brakes 1.8 blocks before the arc of the stop and coasts a quarter of a block, so the stop is put 1.55 past the point. */
        void at(Leg l,String tag,double x,double z,double tx,double ty,double tz){at(l,tag,x,z,tx,ty,tz,0,null);}
        void at(Leg l,String tag,double x,double z,double tx,double ty,double tz,int dwell,String probe){l.stops.add(new Stop(tag,Math.min(l.len+1.5,l.arcOf(x,z)+1.55),tx,ty,tz,dwell,probe));}
        void fin(Leg l,double feetY){
            l.stops.sort(Comparator.comparingDouble(Stop::arc));
            if(want("leg"+l.id)||only.isEmpty()){jobs.add(new EnsureAt(l.q[0][0],l.q[0][1],feetY,l.q[1][0],l.q[1][1]));jobs.add(new LegJob(l));}
            else mine.remove(l);
        }
        /** Puts the walker at the start of a lane when a filter skipped the lane before it (or the one before it failed): not when he stands there already. */
        final class EnsureAt extends Job {
            final double x,z,y,tx,tz;ShotJob inner;boolean decided;
            EnsureAt(double x,double z,double y,double tx,double tz){this.x=x;this.z=z;this.y=y;this.tx=tx;this.tz=tz;}
            @Override boolean step(MinecraftClient c){
                if(!decided){
                    decided=true;var p=c.player;if(p==null)return false;
                    if(Math.hypot(p.getX()-x,p.getZ()-z)<=3.5&&Math.abs(p.getY()-y)<1.6)return true;
                    note(String.format(Locale.ROOT,"the walker is put at (%.1f, %.1f, %.1f), the start of the next lane (he stood at %.1f, %.1f, %.1f)",x,y,z,p.getX(),p.getY(),p.getZ()));
                    inner=new ShotJob(null,x,z,tx,y+1.0,tz,12,"start of a lane",y);
                }
                return inner.step(c);
            }
        }
        /** Sets the world time and waits for the client to have it. */
        final class TimeJob extends Job {
            final long time;TimeJob(long time){this.time=time;}
            @Override boolean step(MinecraftClient c){if(++t==1){server(c,b->b.world.setTimeOfDay(time));}return t>=45;}
        }
        /** A flight picture: the spectator camera at (x,y,z) looking at a target; the number is taken when the view starts, so the files stay in the order of the walk. */
        final class ViewJob extends Job {
            final String tag,what;final double x,y,z,tx,ty,tz;String name;
            ViewJob(String tag,double x,double y,double z,double tx,double ty,double tz,String what){this.tag=tag;this.x=x;this.y=y;this.z=z;this.tx=tx;this.ty=ty;this.tz=tz;this.what=what;}
            @Override boolean step(MinecraftClient c){
                if(c.currentScreen!=null)c.setScreen(null);
                if(name==null){release(c);if(c.options.getViewDistance().getValue()!=14)c.options.getViewDistance().setValue(14);name="cw-"+String.format(Locale.ROOT,"%02d",++seq)+"-"+tag;}
                if(!view(c,name,x,y,z,tx,ty,tz))return false;
                photoTotal++;report("p3-"+name,true,String.format(Locale.ROOT,"flight camera at (%.1f, %.1f, %.1f) looking at (%.1f, %.1f, %.1f), spectator, render distance 14: %s",x,y,z,tx,ty,tz,what));ids.add("p3-"+name);return true;
            }
        }
        void snap(String tag,double x,double z,double fy,double tx,double ty,double tz,int view,String what){jobs.add(new ShotJob(tag,x,z,tx,ty,tz,view,what,fy));}

        void assemble(){
            jobs.clear();mine.clear();var f=GROUND;double top=PLATEAU;
            // leg 1: the arrival
            var l=lane("1-arrival",new double[]{0.5,-291.5},new double[]{0.5,-312.0});
            at(l,"spawn-north",0.5,-291.5,0.5,68.0,-320.0);
            at(l,"welcome-label",0.5,-291.5,0.5,68.7,-298.5,0,"label:welcome");
            at(l,"avenue-standard",0.5,-299.0,0.5,69.0,-316.0);
            at(l,"parade-west",0.5,-304.0,-16.0,66.0,-306.0);
            at(l,"parade-east",0.5,-304.0,16.0,66.0,-306.0);
            at(l,"standard",0.5,-309.4,0.5,69.5,-316.0);
            fin(l,f);
            // leg 2: east to the archive
            l=lane("2a-archive",new double[]{0.5,-310.8},new double[]{0.5,-304.0},new double[]{34.0,-304.0},new double[]{36.0,-308.0},new double[]{38.0,-309.5},new double[]{41.4,-309.5});
            at(l,"archive-from-parade",16.0,-304.0,40.0,70.0,-308.0);
            at(l,"archive-west-face",30.0,-304.0,39.0,69.0,-309.5);
            at(l,"archive-forecourt",35.0,-306.0,39.0,67.0,-309.5);
            at(l,"door-threshold",39.5,-309.5,46.0,67.0,-309.5);
            at(l,"archivist",41.4,-309.5,44.5,66.2,-309.5);
            at(l,"archivist-label",41.4,-309.5,44.5,68.8,-309.5,0,"label:archivist");
            at(l,"hall-hearth",41.4,-309.5,50.5,67.0,-309.5);
            at(l,"hall-shelves",41.4,-309.5,45.0,67.0,-313.5);
            at(l,"hall-roof",41.4,-309.5,45.0,73.0,-309.5);
            fin(l,f);
            l=lane("2b-founders",new double[]{41.4,-309.5},new double[]{36.5,-309.5},new double[]{35.0,-304.5},new double[]{35.0,-297.0},new double[]{41.0,-296.0},new double[]{47.0,-296.0});
            at(l,"door-looking-out",41.4,-309.5,34.0,67.0,-309.5);
            at(l,"court-approach",35.0,-298.0,46.0,66.5,-296.0);
            at(l,"court-gate",44.5,-296.0,50.0,66.0,-296.0,0,"label:founders");
            at(l,"founders-stone",47.0,-296.0,50.0,65.8,-296.0);
            at(l,"founders-label",47.0,-296.0,50.5,68.7,-295.5,0,"label:founders");
            at(l,"court-corner",47.0,-296.0,53.0,66.5,-291.0);
            fin(l,f);
            // leg 3: back to the parade, the Steward, the pads, the arch
            l=lane("3a-steward",new double[]{47.0,-296.0},new double[]{24.0,-296.0},new double[]{6.0,-296.0},new double[]{2.5,-292.0},new double[]{2.5,-290.5});
            at(l,"parade-east-edge",24.0,-296.0,0.5,66.5,-296.0);
            at(l,"welcome-from-east",8.0,-296.0,0.5,68.7,-298.5,0,"label:welcome");
            at(l,"spawn-plinth",6.0,-296.0,0.5,64.8,-292.0);
            at(l,"steward",2.5,-291.0,4.5,66.4,-287.5);
            at(l,"steward-label",2.5,-291.0,4.5,68.7,-287.5,0,"label:steward");
            fin(l,f);
            l=lane("3b-west-pad",new double[]{2.5,-290.5},new double[]{-9.0,-287.0},new double[]{-9.0,-282.0});
            at(l,"west-pad-label",-9.0,-287.0,-9.0,68.7,-282.0,0,"label:gate-west");
            at(l,"west-pad-stand",-9.0,-282.0,-1.0,69.0,-279.0,30,"pad:0");
            at(l,"west-pad-tower",-9.0,-282.0,-35.0,74.0,-268.0);
            at(l,"west-pad-back",-9.0,-282.0,-9.0,66.5,-304.0);
            fin(l,f);
            l=lane("3c-east-pad",new double[]{-9.0,-282.0},new double[]{-9.0,-285.0},new double[]{10.0,-285.0},new double[]{10.0,-282.0});
            at(l,"east-pad-label",10.0,-285.0,10.0,68.7,-282.0,0,"label:gate-east");
            at(l,"east-pad-stand",10.0,-282.0,1.5,69.0,-279.0,30,"pad:1");
            at(l,"east-pad-tower",10.0,-282.0,35.0,74.0,-268.0);
            at(l,"pads-pair",10.0,-282.0,-9.0,65.5,-282.0);
            fin(l,f);
            l=lane("3d-arch",new double[]{10.0,-282.0},new double[]{10.0,-285.0},new double[]{0.5,-285.0},new double[]{0.5,-278.4});
            at(l,"arch-front",0.5,-285.0,0.5,67.0,-277.0);
            at(l,"arch-inside",0.5,-279.5,0.5,72.0,-278.0);
            at(l,"portcullis",0.5,-278.4,0.5,66.5,-277.0);
            at(l,"rim-through-gate",0.5,-278.4,0.5,74.0,-268.0);
            fin(l,f);
            // leg 4: the ridge
            l=lane("4a-ridge-foot",new double[]{0.5,-278.4},new double[]{0.5,-300.0},new double[]{0.5,-312.0},new double[]{-8.0,-314.0},new double[]{-8.0,-322.0},new double[]{-4.0,-333.0});
            at(l,"gates-from-avenue",0.5,-300.0,0.5,71.0,-282.0,0,"label:war-gates");
            at(l,"bowl-bare",-8.0,-322.0,0.0,76.0,-352.0);
            at(l,"both-stairs",-4.0,-333.0,0.5,74.0,-352.0);
            fin(l,f);
            // Task 4: the recruiter is gone; the lane walks into the Graveborn graveyard (camp/graveyard) through its gateway and the colonnade arch, to the court
            l=lane("4a2-graveyard",new double[]{-4.0,-333.0},new double[]{0.5,-339.0},new double[]{0.5,-343.5});
            at(l,"graveyard-gate",0.5,-339.0,0.5,66.4,-346.5);
            at(l,"graveyard-court",0.5,-343.5,0.5,70.0,-357.0);
            fin(l,f);
            l=lane("4a3-west-foot",new double[]{0.5,-343.5},new double[]{-17.5,-343.5},new double[]{-17.5,-347.0});
            at(l,"west-stair-approach",-12.0,-343.5,-17.5,72.0,-352.0);
            at(l,"west-stair-foot",-17.5,-347.0,-17.5,77.0,-360.0);
            fin(l,f);
            l=lane("4b-west-stair",new double[]{-17.5,-347.0},new double[]{-17.5,-361.5});
            at(l,"stair-middle-up",-17.5,-354.0,-17.5,79.0,-362.0);
            at(l,"stair-middle-back",-17.5,-354.0,-17.5,66.0,-330.0);
            at(l,"stair-near-top",-17.5,-358.5,0.5,82.0,-370.0);
            at(l,"stair-top",-17.5,-361.5,0.5,82.0,-372.0,0,"top");
            fin(l,f);
            l=lane("4c-plateau",new double[]{-17.5,-361.5},new double[]{0.5,-361.5},new double[]{0.5,-364.6});
            at(l,"tent-front-from-west",-12.0,-361.5,0.5,82.0,-364.0);
            at(l,"plateau-edge-south",-6.0,-361.5,0.5,66.0,-310.0);
            at(l,"camp-from-plateau",-3.0,-361.5,6.0,66.0,-290.0);
            at(l,"tent-threshold",0.5,-363.2,0.5,79.0,-372.0);
            at(l,"war-table-south",0.5,-364.6,0.5,78.1,-372.0);
            at(l,"war-table-near",0.5,-364.6,0.5,77.9,-367.0);
            fin(l,top);
            l=lane("4d-tent",new double[]{0.5,-364.45},new double[]{-10.3,-364.45},new double[]{-10.3,-368.0});
            l.look=2.5;
            at(l,"tent-west-wall",-10.3,-364.45,-9.5,79.0,-376.0);
            at(l,"war-table-west-end",-10.3,-368.0,8.0,78.3,-371.0);
            at(l,"tent-roof",-10.3,-368.0,0.5,87.0,-371.0);
            fin(l,top);
            l=lane("4e-quartermaster",new double[]{-10.3,-368.0},new double[]{-10.3,-364.0},new double[]{-8.3,-364.45},new double[]{6.5,-364.45},new double[]{9.2,-363.6});
            l.look=1.4;
            at(l,"war-table-from-east",6.0,-364.45,-6.0,78.1,-371.0);
            at(l,"quartermaster",9.2,-363.6,10.5,78.3,-367.5);
            at(l,"quartermaster-label",9.2,-363.6,10.5,80.7,-367.5,0,"label:quartermaster");
            at(l,"armoury",9.2,-363.6,12.5,78.5,-367.5);
            fin(l,top);
            l=lane("4f-east-stair",new double[]{9.2,-363.6},new double[]{7.8,-363.6},new double[]{7.8,-361.2},new double[]{18.5,-361.2},new double[]{18.5,-343.0});
            l.look=2.5;
            at(l,"standard-pole",14.0,-361.2,4.5,86.0,-363.5);
            at(l,"east-stair-top",18.5,-361.2,18.5,68.0,-340.0);
            at(l,"east-stair-middle",18.5,-354.0,18.5,66.0,-330.0);
            at(l,"east-stair-foot",18.5,-343.5,18.5,77.0,-360.0,0,"foot");
            fin(l,top);
            legs=mine;
            var s=spots;
            // dusk and night: the same views again with the lanterns and braziers lit
            if(ran("night")){
                jobs.add(new TimeJob(13300));
                snap("dusk-arrival",0.5,-291.5,f,0.5,68.0,-320.0,12,"dusk (world time 13300): the arrival view again, from the spawn plinth up the avenue");
                snap("dusk-parade",0.5,-306.0,f,0.5,69.0,-316.0,12,"dusk: the parade standard and the lamp posts of the avenue");
                snap("dusk-gates",0.5,-291.0,f,0.5,68.5,-278.0,12,"dusk: the War Gates and the lantern posts of both pads from the avenue");
                snap("dusk-tent",0.5,-360.8,top,0.5,81.0,-369.0,12,"dusk: the command tent from the plateau edge");
                jobs.add(new TimeJob(18000));
                snap("night-tent-inside",0.5,-364.6,top,0.5,79.0,-372.0,12,"night (world time 18000): inside the command tent, looking over the war table");
                snap("night-gates",0.5,-287.0,f,0.5,68.5,-278.0,12,"night: the War Gates arch");
                snap("night-archive-door",36.0,-306.0,f,39.0,68.0,-309.5,12,"night: the archive's west door");
                jobs.add(new TimeJob(3000));
            }
            // the flanks and the edges of the island, from the ground
            if(ran("flank")){
                snap("south-rim-west",-26.0,-289.0,f,-46.0,76.0,-268.0,14,"the south rim of the island seen from beside the pads, over the bowl's west corner (the gates hide it from the pads themselves)");
                snap("south-rim-east",26.0,-289.0,f,46.0,76.0,-268.0,14,"the south rim of the island from the east of the pads");
                snap("west-flank-slope",-30.0,-335.0,f,-92.0,82.0,-335.0,14,"the west flank of the island seen from the bowl: the slope up to the forested rim (the rim itself is thick forest, so no picture is taken in it)");
                snap("east-flank-slope",30.0,-335.0,f,92.0,82.0,-335.0,14,"the east flank of the island seen from the bowl");
                var n=s.get("north-edge");if(n!=null){
                    snap("north-edge-out",n[0],n[1],n[2],n[0],n[2]-14.0,n[1]-60,14,"the north edge of the island behind the ridge (dz -106), looking out to the north");
                    snap("north-edge-back",n[0],n[1],n[2],0.5,84.0,-380.0,14,"the north edge: looking back south up the back slope to the roof of the command tent");}
                var sw=s.get("sw-corner");if(sw!=null){
                    snap("island-edge-south-west",sw[0],sw[1],sw[2],sw[0]-60,sw[2]+0.5,sw[1]+50,14,"the south-west corner of the island, looking out over its edge to the south-west");
                    snap("island-corner-back",sw[0],sw[1],sw[2],0.5,72.0,-300.0,14,"the same corner looking back over the bowl towards the parade");}
                snap("north-slope-from-plateau",-35.0,-368.0,top,-35.0,93.0,-410.0,14,"the slope behind the tent (dz -52 to -108) seen from the plateau, looking north");
            }
            // the flight views
            if(ran("fly")){
                jobs.add(new ViewJob("fly-aerial-front",0.5,110.0,-250.0,0.5,64.0,-320.0,"the camp from the south and above: the gates, the parade, the bowl, the ridge"));
                jobs.add(new ViewJob("fly-aerial-back",0.5,108.0,-440.0,0.5,66.0,-300.0,"the camp from behind the island and above: the back slope, the tent, the stairs, the bowl"));
                jobs.add(new ViewJob("fly-pad-west",-9.0,72.0,-287.0,-9.0,65.0,-282.0,"the west ready pad from above (4x4 smooth quartz in its blackstone frame, lantern posts either side)"));
                jobs.add(new ViewJob("fly-pad-east",10.0,72.0,-287.0,10.0,65.0,-282.0,"the east ready pad from above"));
                jobs.add(new ViewJob("fly-war-table",0.5,86.0,-361.0,0.5,78.0,-371.0,"the war table from above and the south"));
                jobs.add(new ViewJob("fly-tent-front",-7.0,79.5,-355.0,4.0,78.0,-371.0,"the war table and the tent from in front of it, a little to the west and at the height of the table"));
                jobs.add(new ViewJob("fly-west-island",-150.0,92.0,-310.0,-50.0,70.0,-310.0,"the island from the west, outside it"));
                jobs.add(new ViewJob("fly-east-island",150.0,92.0,-310.0,50.0,70.0,-310.0,"the island from the east, outside it"));
                jobs.add(new ViewJob("fly-underside",0.5,30.0,-230.0,0.5,64.0,-310.0,"the island from below and the south"));
                jobs.add(new ViewJob("fly-gates-outside",0.5,71.5,-262.0,0.5,67.0,-284.0,"the War Gates seen from outside, over the south rim"));
            }
        }

        // ---- the checks that wait for the walk ----
        @Override void probe(MinecraftClient c,Stop st,Leg leg){
            var p=c.player;String[] k=st.probe().split(":");
            switch(k[0]){
                case "pad"->{
                    int team=Integer.parseInt(k[1]);padFeet[team]=new double[]{p.getX(),p.getY(),p.getZ()};padQuartz[team]=c.world.getBlockState(p.getBlockPos().down()).isOf(Blocks.SMOOTH_QUARTZ);
                    UUID id=p.getUuid();
                    server(c,b->{Integer on=b.campaign.onPad.get(id);padOn[team]=on==null?-1:on;padCount[team]=b.campaign.padCountdown;b.campaign.padCountdown=-1;}); // a fresh countdown: the walker is off the pad long before it ends
                }
                case "label"->{for(int i=0;i<Spec.CAMPSTATIONS.length;i++)if(Spec.CAMPSTATIONS[i].id().equals(k[1]))labelViews.add(new double[]{i,p.getX(),p.getEyeY(),p.getZ()});}
                case "top"->stairTop=p.getY();
                case "foot"->stairFoot=p.getY();
                default->{}
            }
        }
        /** The width of every label text in pixels, from the client's text renderer (the keybind parts are named there). */
        float[] measure(MinecraftClient c){
            var px=new float[Spec.CAMPSTATIONS.length];
            for(int i=0;i<px.length;i++)px[i]=c.textRenderer.getWidth(CampWorld.text(Spec.CAMPSTATIONS[i].label()));
            return px;
        }
        void summary(MinecraftClient c){
            int total=0;for(var l:mine)total++;
            boolean all=legsDone==total&&legsFailed==0&&stuckTotal==0&&forcedTotal==0;
            ck("p3-cw-walk",all&&(total>0),"lanes finished "+legsDone+" of "+total+", failed "+legsFailed+", blocks walked "+Math.round(walkedTotal)+", stuck windows "+stuckTotal+", slow windows (under 6 blocks in 2 s) "+slowTotal+", forced steps "+forcedTotal+", auto-jumps "+bumpTotal+", pictures "+photoTotal);
            String[] names={"west","east"};
            for(int team=0;team<2;team++){
                if(!ranLeg(team==0?"leg3b":"leg3c"))continue;
                var f=padFeet[team];boolean has=f!=null;
                boolean in=has&&CampaignMaps.onPad(team,f[0],f[1],f[2]);
                var ctr=CampaignMaps.padCenter(team);
                ck("p3-cw-pad-"+names[team],has&&in&&padQuartz[team]&&padOn[team]==team,has?String.format(Locale.ROOT,"the walker stood at (%.2f, %.2f, %.2f), the pad centre is (%.1f, %.1f, %.1f): inside the pad %s, smooth quartz under his feet %s, Campaign.onPad says team %d (want %d), ready countdown %d of 200 when he was photographed",f[0],f[1],f[2],ctr.x,ctr.y,ctr.z,in,padQuartz[team],padOn[team],team,padCount[team]):"the walker never reached the pad stop");
            }
            Leg west=null,east=null;for(var l:mine){if(l.id.equals("4b-west-stair"))west=l;if(l.id.equals("4f-east-stair"))east=l;}
            if(west!=null||east!=null){
                boolean up=west==null||(!Double.isNaN(stairTop)&&stairTop>=76.9),down=east==null||(!Double.isNaN(stairFoot)&&stairFoot<=65.1);
                boolean clean=(west==null||(west.finished&&west.forced==0&&west.stuck==0))&&(east==null||(east.finished&&east.forced==0&&east.stuck==0));
                ck("p3-cw-stairs",up&&down&&clean,String.format(Locale.ROOT,"west flight: the walker's feet at its top %.2f (want 77 or more), lane %s, auto-jumps %d, stuck windows %d, forced steps %d; east flight: feet at its foot %.2f (want 65), lane %s, auto-jumps %d, stuck windows %d, forced steps %d",
                    stairTop,west==null?"not run":west.finished?"finished":"NOT finished",west==null?0:west.bumps,west==null?0:west.stuck,west==null?0:west.forced,stairFoot,east==null?"not run":east.finished?"finished":"NOT finished",east==null?0:east.bumps,east==null?0:east.stuck,east==null?0:east.forced));
            }
            if(only.isEmpty()){
                var need=new ArrayList<String>(List.of("p3-cw-arrival","p3-cw-walk","p3-cw-pad-west","p3-cw-pad-east","p3-cw-stairs","p3-cw-floating","p3-cw-lantern","p3-cw-barrier","p3-cw-entities","p3-cw-labels"));
                for(var l:mine)need.add("p3-cw-leg-"+l.id);
                var missing=new ArrayList<String>();for(var id:need)if(!ids.contains(id))missing.add(id);
                ck("p3-cw-complete",missing.isEmpty()&&photoTotal>=40,"every check id of the walk was reported: "+(missing.isEmpty()?"yes":"MISSING "+missing)+"; pictures taken "+photoTotal+" (want 40 or more)");
            }
        }
        void pictures(MinecraftClient c){
            int files=0;var names=new TreeSet<String>();
            try(var s=Files.list(c.runDirectory.toPath().resolve("screenshots"))){s.forEach(f->{var n=f.getFileName().toString();if(n.startsWith("p3-cw-")&&n.endsWith(".png"))names.add(n);});}catch(Exception e){ck("p3-cw-pictures",false,"cannot list screenshots: "+e);return;}
            files=names.size();
            ck("p3-cw-pictures",files==photoTotal&&files>=(only.isEmpty()?40:1),files+" files p3-cw-*.png in screenshots/, pictures taken "+photoTotal+" ("+(only.isEmpty()?"at least 40 wanted":"a filter was given")+")");
        }

        // ---- the scans, on the server thread ----
        /** The cells of the arch's barrier: the war_gates piece at local x 15..21, y 1..3, z 10. */
        static Set<BlockPos> archCells(){
            var out=new HashSet<BlockPos>();
            for(var row:Spec.CAMPPIECES)if(row.template().equals("camp/war_gates")){
                var p=Camp.piece(row.template());var size=new Vec3i(p.width(),p.height(),p.depth());
                for(int lx=15;lx<=21;lx++)for(int ly=1;ly<=3;ly++)out.add(Structures.toWorld(size,OX+row.dx(),row.y(),OZ+row.dz(),row.rotation(),lx,ly,10));
            }
            return out;
        }
        /** A place near (x,z) where the walker can stand for a flank picture: inside the island (3 blocks from its edge), the highest block with a collision shape a full opaque block that is no log or leaves, none of those within 5 blocks around and 9 above (when no such place is found within 12 blocks: 2 around and 5 above within 20, then 1 and 3 within 40). {x, z, feet y}, or null; every direction given as {dx, dz} must be open (no block with a collision shape at three heights) for 10 blocks. The column is read block by block (the heightmap of a column the camp builder wrote is not trusted). */
        static double[] clearSpot(ServerWorld w,int x,int z,double[]... dirs){
            int lx0=OX+C.islandMinDx()+3,lx1=OX+C.islandMaxDx()-3,lz0=OZ+C.islandMinDz()+3,lz1=OZ+CampWorld.rimEnd(C)-3;
            var logs=net.minecraft.registry.tag.BlockTags.LOGS;var leaves=net.minecraft.registry.tag.BlockTags.LEAVES;
            for(int[] clear:new int[][]{{5,9,12},{2,5,20},{0,0,40}})for(int r=0;r<=clear[2];r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
                int bx=x+dx,bz=z+dz;if(bx<lx0||bx>lx1||bz<lz0||bz>lz1)continue;
                w.getChunk(bx>>4,bz>>4);int top=Integer.MIN_VALUE;
                for(int y=140;y>=C.minY();y--){var pos=new BlockPos(bx,y,bz);var st=w.getBlockState(pos);if(st.isAir()||st.getCollisionShape(w,pos).isEmpty())continue;top=y;break;}
                if(top==Integer.MIN_VALUE)continue;
                var tp=new BlockPos(bx,top,bz);var g=w.getBlockState(tp);if(g.isIn(logs)||g.isIn(leaves)||!g.isOpaqueFullCube(w,tp))continue;
                boolean free=true;
                for(int ax=-clear[0];ax<=clear[0]&&free;ax++)for(int az=-clear[0];az<=clear[0]&&free;az++)for(int y=top+1;y<=top+clear[1];y++){
                    var st=w.getBlockState(new BlockPos(bx+ax,y,bz+az));
                    if(st.isIn(logs)||st.isIn(leaves)){free=false;break;}
                }
                for(var d:dirs){if(!free)break;double len=Math.hypot(d[0],d[1]);for(double h:new double[]{1.2,1.7,2.6})for(double k=0.5;k<=10.0&&free;k+=0.5){
                    var bp=BlockPos.ofFloored(bx+0.5+d[0]/len*k,top+h,bz+0.5+d[1]/len*k);var st=w.getBlockState(bp);if(!st.isAir()&&!st.getCollisionShape(w,bp).isEmpty())free=false;}}
                if(free)return new double[]{bx+0.5,bz+0.5,top+1.0};
            }
            return null;
        }
        /** GRAVES_CW_SABOTAGE: one planted defect per scan check, and a wall across the avenue. */
        void plant(ServerWorld w){
            int flags=Block.NOTIFY_LISTENERS|Block.FORCE_STATE;
            w.setBlockState(new BlockPos(OX+30,100,OZ-50),Blocks.STONE.getDefaultState(),flags); // floats
            w.setBlockState(new BlockPos(OX+25,75,OZ-20),Blocks.LANTERN.getDefaultState().with(net.minecraft.block.LanternBlock.HANGING,false),flags); // nothing under it
            w.setBlockState(new BlockPos(OX+8,65,OZ+6),Blocks.BARRIER.getDefaultState(),flags); // a barrier on the parade
            var stand=EntityType.ARMOR_STAND.create(w);stand.refreshPositionAndAngles(OX+12.5,64.4,OZ+8.5,0,0);stand.addCommandTag(CampWorld.TAG);stand.addCommandTag("camp-generation:"+CampWorld.generation);boolean spawned=w.spawnEntity(stand); // in the floor (the mod's ENTITY_LOAD discards a camp-tagged entity of another generation)
            note("SABOTAGE armor stand spawned "+spawned+" at "+stand.getPos()+", tags "+stand.getCommandTags()+", in the world "+(w.getEntity(stand.getUuid())!=null)+", block there "+w.getBlockState(BlockPos.ofFloored(stand.getPos())).getBlock());
            for(var e:w.iterateEntities())if(e instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity d&&d.getCommandTags().contains("camp:contracts")){d.setPosition(d.getX(),d.getY()+3.0,d.getZ());break;} // the Archivist's label into the roof
            for(int x=-3;x<=3;x++)for(int y=65;y<=67;y++)w.setBlockState(new BlockPos(OX+x,y,OZ+9),Blocks.STONE_BRICKS.getDefaultState(),flags); // a wall across the avenue, 3 high
            for(int team=0;team<2;team++){var ctr=CampaignMaps.padCenter(team);for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++){var bp=new BlockPos((int)Math.floor(ctr.x)+dx,(int)Math.floor(ctr.y)-1,(int)Math.floor(ctr.z)+dz);if(w.getBlockState(bp).isOf(Blocks.SMOOTH_QUARTZ))w.setBlockState(bp,Blocks.STONE.getDefaultState(),flags);}} // the quartz floors of both pads become stone
            for(int x=-24;x<=-12;x++)for(int y=69;y<=72;y++)w.setBlockState(new BlockPos(OX+x,y,OZ-42),Blocks.STONE_BRICKS.getDefaultState(),flags); // a wall across the west stair, 4 high
        }
        void scan(GravesAndGlory.Battle b){
            var w=b.world;long t0=System.nanoTime();
            if(sabotage){plant(w);note("SABOTAGE: a floating block at (30, 100, -360), a loose lantern at (25, 75, -330), a barrier at (8, 65, -304), an armor stand in the floor at (12, 64, -302), the Archivist's label 3 higher, a wall across the avenue at z -301, the quartz floors of both pads turned to stone, a 4-high wall across the west stair at z -352");}
            int x0=OX+C.islandMinDx(),x1=OX+C.islandMaxDx(),z0=OZ+C.islandMinDz(),z1=OZ+CampWorld.rimEnd(C),y0=C.minY(),y1=140;
            // 1. floating pieces
            var fs=floatBox(w,x0,x1,z0,z1,y0,y1);
            ck("p3-cw-floating",fs.found().isEmpty()&&fs.reached()>100000,"island box x "+x0+".."+x1+", z "+z0+".."+z1+" (the rim rows included), y "+y0+".."+y1+": solid blocks reached from the floor "+fs.reached()+", floating pieces "+fs.found().size()+" of "+fs.floatTotal()+" blocks"+(fs.found().isEmpty()?"":", the biggest "+fs.found().stream().limit(6).map(p->p.size()+" blocks x "+p.x0()+".."+p.x1()+" y "+p.y0()+".."+p.y1()+" z "+p.z0()+".."+p.z1()+" ("+p.top()+")").toList()));
            for(int i=0;i<Math.min(40,fs.found().size());i++){var p=fs.found().get(i);note("floating piece "+(i+1)+": "+p.size()+" blocks, x "+p.x0()+" to "+p.x1()+", y "+p.y0()+" to "+p.y1()+", z "+p.z0()+" to "+p.z1()+": "+p.top());}
            // 2. lanterns and barriers
            var arch=archCells();int lanterns=0,loose=0,barriers=0,inArch=0,outside=0,exposed=0;var looseAt=new StringBuilder();var outAt=new StringBuilder();var pos=new BlockPos.Mutable();
            for(int cx=x0>>4;cx<=x1>>4;cx++)for(int cz=z0>>4;cz<=z1>>4;cz++){
                var ch=w.getChunk(cx,cz);
                for(int x=Math.max(x0,cx*16);x<=Math.min(x1,cx*16+15);x++)for(int z=Math.max(z0,cz*16);z<=Math.min(z1,cz*16+15);z++)for(int y=y0;y<=y1;y++){
                    var st=ch.getBlockState(pos.set(x,y,z));if(st.isAir())continue;
                    if(st.getBlock() instanceof net.minecraft.block.LanternBlock){
                        lanterns++;if(!st.canPlaceAt(w,pos)){loose++;if(loose<=6)looseAt.append(loose>1?"; ":"").append(name(st)).append(' ').append(xyz(pos.toImmutable())).append(" hanging ").append(st.get(net.minecraft.block.LanternBlock.HANGING));}
                    }else if(st.isOf(Blocks.BARRIER)){
                        barriers++;
                        if(arch.contains(pos.toImmutable()))inArch++;
                        else{
                            outside++;boolean open=false;for(var d:Direction.values()){var nb=pos.offset(d);var ns=w.getBlockState(nb);if(!ns.isOf(Blocks.BARRIER)&&!ns.isOpaqueFullCube(w,nb))open=true;}
                            if(open)exposed++;if(outside<=6)outAt.append(outside>1?"; ":"").append(xyz(pos.toImmutable())).append(open?" in the open":" buried");
                        }
                    }
                }
            }
            ck("p3-cw-lantern",lanterns>0&&loose==0,"lanterns in the box "+lanterns+", without a block to hang from or to stand on (the test LanternBlock.canPlaceAt makes) "+loose+(loose==0?"":", first: "+looseAt));
            ck("p3-cw-barrier",inArch==arch.size()&&outside==0,"barrier blocks in the box "+barriers+": in the arch "+inArch+" of "+arch.size()+" cells, anywhere else "+outside+" ("+exposed+" of them with a face in the open)"+(outside==0?"":": "+outAt)+"; the walker's lanes pass no barrier but the arch's portcullis stop");
            // 3. entities
            int villagers=0,labels=0,inside=0,others=0;var insideAt=new StringBuilder();int wantV=0;for(var s:Spec.CAMPSTATIONS)if(!s.npc().equals("-"))wantV++;
            for(var e:w.iterateEntities()){
                if(!e.getCommandTags().contains(CampWorld.TAG))continue;
                boolean bad;
                if(e instanceof net.minecraft.entity.LivingEntity){
                    if(e instanceof net.minecraft.entity.passive.VillagerEntity)villagers++;else others++;
                    bad=w.getBlockCollisions(e,e.getBoundingBox().contract(0.01)).iterator().hasNext();
                }else{
                    if(e instanceof net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity)labels++;else others++;
                    var cell=BlockPos.ofFloored(e.getPos());bad=!w.getBlockState(cell).getCollisionShape(w,cell).isEmpty();
                }
                if(bad){inside++;if(inside<=6)insideAt.append(inside>1?"; ":"").append(e.getType().getUntranslatedName()).append(' ').append(String.format(Locale.ROOT,"(%.2f, %.2f, %.2f)",e.getX(),e.getY(),e.getZ()));}
            }
            ck("p3-cw-entities",villagers==wantV&&labels==Spec.CAMPSTATIONS.length+Spec.CAMPLESSONS.length&&others==CampWorld.DUMMIES&&inside==0,"camp entities: villagers "+villagers+" (want "+wantV+"), labels "+labels+" (want "+Spec.CAMPSTATIONS.length+" stations + "+Spec.CAMPLESSONS.length+" lessons), other camp entities "+others+" (want "+CampWorld.DUMMIES+", the yard dummies), inside a block that stops them "+inside+(inside==0?"":": "+insideAt));
            // 4. places for the flank pictures
            var m=new LinkedHashMap<String,double[]>();
            Object[][] want={{"north-edge",OX,OZ-106,new double[]{0,-1},new double[]{0,1}},{"sw-corner",OX-92,OZ+36}};
            for(var wnt:want){var dirs=new double[wnt.length-3][];for(int q=3;q<wnt.length;q++)dirs[q-3]=(double[])wnt[q];var sp=clearSpot(w,(Integer)wnt[1],(Integer)wnt[2],dirs);if(sp!=null)m.put((String)wnt[0],sp);else note("no clear spot near "+wnt[0]);}
            spots=m;
            note(String.format(Locale.ROOT,"scan of the camp took %d ms; flank spots %s",(System.nanoTime()-t0)/1000000,m.entrySet().stream().map(q->q.getKey()+" "+Arrays.toString(q.getValue())).toList()));
            scanned=true;
        }
        /** p3-cw-labels: the heights, then the view of each label from the place its picture was taken. */
        void labels(GravesAndGlory.Battle b,float[] px){
            var w=b.world;var bad=new StringBuilder();int found=0;
            int n=Spec.CAMPSTATIONS.length;var centre=new Vec3d[n];var half=new double[n][2];
            for(int i=0;i<n;i++){
                var s=Spec.CAMPSTATIONS[i];var at=new Vec3d(OX+s.x(),s.y()+3.7,OZ+s.z());centre[i]=at;
                var hit=w.getEntitiesByClass(net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity.class,new Box(at,at).expand(1.5,6.0,1.5),d->d.getCommandTags().contains("camp:"+s.page()));
                net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity me=null;for(var d:hit)if(Math.abs(d.getX()-at.x)<0.01&&Math.abs(d.getZ()-at.z)<0.01)me=d;
                if(me==null){bad.append(s.id()).append(": no label at x,z of its row; ");continue;}
                found++;
                if(Math.abs(me.getY()-at.y)>0.01)bad.append(s.id()).append(": label at y ").append(String.format(Locale.ROOT,"%.2f",me.getY())).append(" not ").append(String.format(Locale.ROOT,"%.2f",at.y)).append("; ");
                double width=Math.min(px[i],220f)+2,lines=Math.ceil(px[i]/220.0);half[i][0]=width*0.0425/2;half[i][1]=(lines*9+2)*0.0425/2; // 1.7 times 1/40 block a pixel
            }
            int views=0,blocked=0,overlaps=0;var seenBad=new StringBuilder();var over=new StringBuilder();var ent=b.server.getPlayerManager().getPlayerList().get(0);
            for(var v:new ArrayList<>(labelViews)){
                int i=(int)v[0];var eye=new Vec3d(v[1],v[2],v[3]);views++;
                var d=centre[i].subtract(eye).normalize();var up=Math.abs(d.y)>0.98?new Vec3d(1,0,0):new Vec3d(0,1,0);var r=up.crossProduct(d).normalize();var u=d.crossProduct(r).normalize();
                int seen=0;var pts=new Vec3d[]{centre[i],centre[i].add(r.multiply(half[i][0])),centre[i].subtract(r.multiply(half[i][0])),centre[i].add(u.multiply(half[i][1])),centre[i].subtract(u.multiply(half[i][1]))};
                for(var pt:pts){
                    var hit=w.raycast(new net.minecraft.world.RaycastContext(eye,pt,net.minecraft.world.RaycastContext.ShapeType.VISUAL,net.minecraft.world.RaycastContext.FluidHandling.NONE,ent));
                    if(hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS||hit.getPos().distanceTo(pt)<0.2)seen++;
                }
                if(seen<5){blocked++;seenBad.append(Spec.CAMPSTATIONS[i].id()).append(" from (").append(String.format(Locale.ROOT,"%.1f, %.1f, %.1f",v[1],v[2],v[3])).append("): ").append(seen).append(" of 5 points in sight; ");}
                for(int j=0;j<n;j++){
                    if(j==i)continue;var e=centre[j].subtract(eye);double dist=e.length(),fwd=e.normalize().dotProduct(d);if(fwd<0.4)continue;
                    double a=e.dotProduct(r)/e.dotProduct(d),bb=e.dotProduct(u)/e.dotProduct(d),a0=0,b0=0; // the label j on the tangent plane of the view; label i is at the middle
                    double hw=half[j][0]/Math.max(0.5,e.dotProduct(d)),hh=half[j][1]/Math.max(0.5,e.dotProduct(d)),w0=half[i][0]/centre[i].subtract(eye).length(),h0=half[i][1]/centre[i].subtract(eye).length();
                    if(Math.abs(a-a0)<hw+w0&&Math.abs(bb-b0)<hh+h0){
                        var hit=w.raycast(new net.minecraft.world.RaycastContext(eye,centre[j],net.minecraft.world.RaycastContext.ShapeType.VISUAL,net.minecraft.world.RaycastContext.FluidHandling.NONE,ent));
                        if(hit.getType()==net.minecraft.util.hit.HitResult.Type.MISS||hit.getPos().distanceTo(centre[j])<0.2){overlaps++;over.append(Spec.CAMPSTATIONS[i].id()).append(" and ").append(Spec.CAMPSTATIONS[j].id()).append(" from (").append(String.format(Locale.ROOT,"%.1f, %.1f, %.1f",v[1],v[2],v[3])).append("); ");}
                    }
                }
            }
            boolean all=found==n;
            ck("p3-cw-labels",all&&bad.length()==0&&blocked==0&&overlaps==0&&(views>0||!ranLeg("leg")),"labels at y+3.7 of their rows "+found+" of "+n+(bad.length()==0?"":": "+bad)+"; views of labels from the walker's stops "+views+", with a point of the label out of sight "+blocked+(seenBad.length()==0?"":": "+seenBad)+"; pairs of labels overlapping in a view "+overlaps+(over.length()==0?"":": "+over));
            labelsDone=true;
        }
    }

    /** Task 6 (suite landmarks): the landmarks rows of the maps in GRAVES_PHASE3_MAPS (system property gravesandglory.phase3Maps). Each map is switched to (built afresh, even when it stands) and, once ready, per row:
     *  p3-lm-ID-placed (a LOG entry of this row since the switch, not missing; the row it replaces is gone from Spec.STRUCTURES and a landmark row of its id is there),
     *  p3-lm-ID-fits (Placed.box equals Structures.footprint of the template size at the row's centre, elevation and rotation, inside |x|<=350, |z|<=250),
     *  p3-lm-ID-clear (every structure-void cell of the template at local y 1 and up is air in the world, but the flag of an enclosed point: the pole column, its cloth and its lantern),
     *  p3-lm-ID-roads (Walkability.scan over the footprint: through 0 and no blocked deck cell), p3-lm-ID-ring (no non-void template column within 19 of a point the row does not enclose; for an enclosed point the ring,
     *  the centre pad and the flag are the arena's blocks), p3-lm-ID-gates (enclosing rows: every cell within 2 of a road that ends at the point, from radius 17.5 to the footprint edge, has no collision shape at ground+1 and +2),
     *  p3-lm-ID-cover and -sight per landmarkLines row of the template (the world end points through Structures.toWorld, compared with the values the plan states; a raycast with COLLIDER, no fluids and the player:
     *  cover passes on a block hit more than 0.3 before the end, sight on a miss or a hit within 0.3 of it). Per map p3-lm-budget-mapN (each row at most 100 ms, all at most 1.5 s), the pictures lm-mapN-overview-north and -south and
     *  lm-ID-ground (and lm-ID-high off the kit) for the first row of each template (the plan's camera, turned round the piece when its view is not clear: see plan). Once per run p3-lm-switch-mid-queue (map 4 is switched to; while its landmarks are queued, map 0: the queue is empty at once, no kf- row is
     *  placed after it, and map 0 is ready only once its own queue is drained) and p3-lm-cache (campaign.cached() after the build, not with Structures.salt="test", again after it is put back) and p3-lm-cache-pieces (a pieces row of a template of the map one block taller: another hash, the marker stale; put back: current again). A map with no landmarks rows passes p3-lm-budget-mapN with nothing to place.
     *  Task 8b: the 18 rows of the supply houses (templates supply/*) are checked like the others; their -clear leaves out the flag column of the house (reserved: |x-yardX|<=3, |z-yardZ|<=3, elevation+1 to +8), their cover and sight lines are compared with the plan's world points (houseLine),
     *  and per map with houses p3-lm-supply-clear-control-mapN (a block just outside the reserved column fails -clear, one inside does not, with the column dropped the flag fails it) and p3-lm-supply-lines-control-mapN (a world point out of place fails the comparison, a block on the sight line fails -sight,
     *  the wall taken off the cover line fails -cover). */
    static final class Landmarks implements Suite {
        record Line(double fx,double fy,double fz,double tx,double ty,double tz){}
        /** The world end points of the lines the plan states (ty NaN: the ground at the end point plus 2.6). */
        static final Map<String,Line> EXPECTED=Map.ofEntries(
            Map.entry("rg-mill/cover",new Line(-25.5,66.6,-167.5,-30.5,66.6,-145.5)),Map.entry("rg-mill/sight",new Line(-22.5,73.6,-173.5,-16.5,66.6,-145.5)),
            Map.entry("kf-mill/cover",new Line(-24.5,66.6,-182.5,-29.5,66.6,-160.5)),Map.entry("kf-mill/sight",new Line(-21.5,73.6,-188.5,-15.5,66.6,-160.5)),
            Map.entry("kf-outpost-west/cover",new Line(-116.5,76.6,113.5,-110.5,76.6,119.5)),Map.entry("kf-outpost-west/sight",new Line(-144.5,85.6,115.5,-160.5,Double.NaN,100.5)),
            Map.entry("kf-outpost-east/cover",new Line(117.5,76.6,-112.5,111.5,76.6,-118.5)),Map.entry("kf-outpost-east/sight",new Line(145.5,85.6,-114.5,161.5,Double.NaN,-99.5)),
            // Task 7: the gate (the west field in line with the passage: cover from behind the wall stub, sight from the rampart opening of the north tower) and the keep (cover inside the north wall, sight from the crenel on the wall-walk to the B deck)
            Map.entry("ih-gate/cover",new Line(-32.5,66.6,-15.5,-60.5,Double.NaN,-15.5)),Map.entry("ih-gate/sight",new Line(-38.5,78.6,-8.5,-60.5,Double.NaN,0.5)),
            Map.entry("ih-keep/cover",new Line(-8.5,66.6,25.5,3.5,66.6,0.5)),Map.entry("ih-keep/sight",new Line(-8.5,72.6,21.5,3.5,66.6,0.5)),
            // Task 8: the lodge (cover from the yard behind its east wall, sight from the upper room of the gatehouse through its east window, both to the lodge lane) and the chapel (cover from the nave behind the pier of its south wall, sight from the nave through its window, both to the chapel lane)
            Map.entry("fm-lodge/cover",new Line(-75.5,66.6,-184.5,-50.5,Double.NaN,-166.5)),Map.entry("fm-lodge/sight",new Line(-67.5,72.6,-172.5,-50.5,Double.NaN,-166.5)),
            Map.entry("hw-chapel/cover",new Line(76.5,66.6,104.5,49.5,Double.NaN,141.5)),Map.entry("hw-chapel/sight",new Line(70.5,66.6,109.5,52.5,Double.NaN,127.5)));
        record Shot(String name,double x,double y,double z,double tx,double ty,double tz){}
        final List<Integer> maps=new ArrayList<>();
        volatile List<Shot> shots=List.of();
        int index,stage,shot;
        volatile boolean switched,checked,hookDone,cacheDone;
        volatile Set<Structures.Placed> before;
        /** The once-per-run mid-queue switch runs on the server tick (the client suite does not step while a map builds). */
        static volatile Runnable hook;
        static boolean hooked;
        Landmarks(){
            for(String s:System.getProperty("gravesandglory.phase3Maps","0,1,2,3,4").split(","))if(!s.isBlank())maps.add(Integer.parseInt(s.trim()));
            if(!hooked){hooked=true;net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(s->{var h=hook;if(h!=null)try{h.run();}catch(Throwable e){hook=null;report("p3-lm-switch-mid-queue",false,e.toString());e.printStackTrace();hookDone=true;}});}
        }
        @Override public int limit(){return 12000;}
        static Set<Structures.Placed> snapshot(){var s=Collections.newSetFromMap(new IdentityHashMap<Structures.Placed,Boolean>());s.addAll(Structures.LOG);return s;}
        static List<Structures.Placed> newer(Set<Structures.Placed> before){var out=new ArrayList<Structures.Placed>();for(var p:Structures.LOG)if(!before.contains(p))out.add(p);return out;}
        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            switch(stage){
                case 0->{
                    if(index>=maps.size()){stage=4;return;}
                    int n=maps.get(index);switched=false;checked=false;
                    server(c,b->{before=snapshot();b.campaign.switchMap(n);switched=true;});stage=1;
                }
                case 1->{
                    int n=maps.get(index);var b=GravesAndGlory.battle;
                    if(switched&&b!=null&&b.ready&&b.campaign.map==n&&CampaignMaps.selected==n){stage=2;server(c,x->check(x,n));}
                }
                case 2->{if(checked){shot=0;stage=3;}}
                case 3->{
                    if(shot>=shots.size()){index++;stage=0;return;}
                    var s=shots.get(shot);if(view(c,s.name(),s.x(),s.y(),s.z(),s.tx(),s.ty(),s.tz()))shot++;
                }
                case 4->{hookDone=false;server(c,this::midQueue);stage=5;}
                case 5->{var b=GravesAndGlory.battle;if(hookDone&&b!=null&&b.ready){stage=6;server(c,this::cache);}}
                case 6->{if(cacheDone){finish(c,"landmarks");stage=7;}}
                default->{}
            }
        }
        @Override public void cleanup(MinecraftClient c){Structures.salt="";hook=null;server(c,b->b.campaign.leash=true);}

        /** The pictures of map n: the overview, and for the first row of each template the ground view (and the high view off the kit), from the side of the nearest point the row does not enclose: the camera of the plan
         *  (the centre plus u times max(w,d)+12 at ground+5, and times max(w,d)+30 at elevation+30, u toward that point). Where that camera stands in a block or another footprint, or something outside the piece stands
         *  between it and the piece (the dressing of a capture ring, a tree, a house of the hamlet), u is turned by 30 degrees at a time, either way, to the first camera with a clear view; the turn is in the note. */
        List<Shot> plan(GravesAndGlory.Battle b,int n){
            var out=new ArrayList<Shot>();
            // two overviews low and near enough to read through the fog of a shader pack (fix round 1: the one from (0, 175, 250) was lost in it): the north half (the hamlet, its market, farm and mill)
            // from the middle of the field, and the middle (the customs, the warehouse, the crossing) from the south of it
            out.add(new Shot("lm-map"+n+"-overview-north",-20,112,-60,-75,64,-150));
            out.add(new Shot("lm-map"+n+"-overview-south",0,108,95,0,64,-15));
            var seen=new HashSet<String>();
            var player=b.server.getPlayerManager().getPlayerList().isEmpty()?null:b.server.getPlayerManager().getPlayerList().get(0);
            for(var r:Spec.LANDMARKS){
                if(r.map()!=n||!seen.add(r.template()))continue;
                Spec.StructuresRow s=null;for(var st:Spec.STRUCTURES)if(st.id().equals(r.id()))s=st;if(s==null)continue;
                Spec.PointsRow near=null;double best=Double.MAX_VALUE;
                for(var pt:Spec.POINTS){if(pt.id().equals(r.encloses()))continue;double d=Math.hypot(pt.x()-r.x(),pt.z()-r.z());if(d<best){best=d;near=pt;}}
                double a0=Math.atan2(near.z()-r.z(),near.x()-r.x());
                int reach=Math.max(s.width(),s.depth());double tx=r.x()+.5,ty=r.elevation()+4,tz=r.z()+.5;
                for(int high=0;high<(r.template().startsWith("kit/")?1:2);high++){
                    int dist=reach+(high==1?30:12);Shot chosen=null,first=null;int turned=0;
                    for(int t:new int[]{0,30,-30,60,-60,90,-90,120,-120,150,-150,180}){
                        double a=a0+Math.toRadians(t),cx=r.x()+.5+Math.cos(a)*dist,cz=r.z()+.5+Math.sin(a)*dist;
                        double cy=high==1?r.elevation()+30:Math.max(r.elevation(),Arena.ground((int)Math.floor(cx),(int)Math.floor(cz)))+5;
                        var shot=new Shot("lm-"+r.id()+(high==1?"-high":"-ground"),cx,cy,cz,tx,ty,tz);if(first==null)first=shot;
                        if(player!=null&&clearView(b,player,s,cx,cy,cz)){chosen=shot;turned=t;break;}
                    }
                    if(chosen==null){chosen=first;System.out.println("[p3] note view "+first.name()+": no clear camera round the piece, the plan's camera is kept");}
                    else if(turned!=0)System.out.println("[p3] note view "+chosen.name()+": the plan's camera (toward "+near.id()+") had no clear view, turned "+turned+" degrees");
                    out.add(chosen);
                }
                var side=ROADSIDE.get(r.template());
                if(side!=null&&player!=null){var shot=roadShot(b,player,s,r,side,"road");if(shot!=null)out.add(shot);}
                var front=FRONT.get(r.template());
                if(front!=null&&player!=null){var shot=roadShot(b,player,s,r,front,"front");if(shot!=null)out.add(shot);}
            }
            return out;
        }
        /** Pictures from the side a piece shows its detail to (fix round 2: the market cross hid behind a stall, the toll board of the customs was in no picture): per template the eye {x, z} and the aim {x, y, z} in local
         *  coordinates of the piece (the eye's z is where it stands, not a distance: a customs' road is the north side, a market's front the south). The eye is 4 above the ground; where that view is not clear (a tree, another
         *  piece) the eye is moved along the piece by 5 blocks at a time, either way, and then 6 blocks further away. */
        static final Map<String,double[]> ROADSIDE=Map.ofEntries(
            Map.entry("kit/market",new double[]{9.5,23.5,9.5,5.0,6.5}),
            Map.entry("kit/customs",new double[]{4.0,-11.0,6.0,3.0,1.0}),
            // Task 7: the sides the Ironhold pieces show themselves from: the gate and the curtain from the west field (the outer face), the keep and the garrison from the north (the road to B, the lane), the barn from its yard (south and east)
            Map.entry("ironhold/gate",new double[]{-15.0,23.5,3.0,6.0,23.5}),
            Map.entry("ironhold/curtain",new double[]{-16.0,15.5,2.0,6.0,15.5}),
            Map.entry("ironhold/keep",new double[]{23.5,-26.0,23.5,6.0,4.0}),
            Map.entry("ironhold/garrison",new double[]{15.5,-14.0,15.5,4.0,8.0}),
            Map.entry("ironhold/hay_barn",new double[]{12.0,32.0,12.0,3.0,10.0}),
            // Task 8: the road side of each new piece, the side its lane or road comes to: the lodge from the east (its gate on the lane), the refuge from the north-west (where the lane runs), the signal tower from the north (its door),
            // the chapel from the south (its door and the lane), the sawmill from the east (its open side), the logging camp from the lane (east), the moon shrine from the south (its gate and, through it, the moon gate)
            Map.entry("frostmarch/lodge",new double[]{39.0,14.0,24.0,5.0,14.0}),
            Map.entry("frostmarch/refuge",new double[]{4.0,-9.0,10.0,3.0,6.0}),
            Map.entry("frostmarch/signal_tower",new double[]{7.5,-12.0,7.5,9.0,3.0}),
            Map.entry("hollowwood/chapel",new double[]{9.0,33.0,11.0,5.0,22.0}),
            Map.entry("hollowwood/sawmill",new double[]{36.0,12.0,14.0,3.0,9.0}),
            Map.entry("hollowwood/logging_camp",new double[]{29.0,11.0,8.0,1.5,8.0}),
            Map.entry("hollowwood/moon_shrine",new double[]{11.5,31.0,11.5,6.0,6.0}));
        /** Task 8: the front of each new piece, the face that shows what it is (same format and same search for a clear eye as ROADSIDE): the lodge from the south (the hall and the long yard wall), the refuge from the south, the signal tower from the east (the slits and the stair),
         *  the chapel from the west (the gable with the rose window), the sawmill from the south (its open side), the logging camp from the south-west (the lean-to, the piles, the hut behind), the moon shrine from the east-south-east (over the low wall: the court, the pool and the moon gate; the north side has the chapel in front of it). */
        static final Map<String,double[]> FRONT=Map.ofEntries(
            Map.entry("frostmarch/lodge",new double[]{14.0,34.0,14.0,5.0,12.0}),
            Map.entry("frostmarch/refuge",new double[]{10.0,30.0,10.0,4.0,8.0}),
            Map.entry("frostmarch/signal_tower",new double[]{30.0,7.5,7.5,9.0,7.5}),
            Map.entry("hollowwood/chapel",new double[]{-16.0,15.5,4.0,8.0,15.5}),
            Map.entry("hollowwood/sawmill",new double[]{12.0,32.0,12.0,4.0,9.0}),
            Map.entry("hollowwood/logging_camp",new double[]{-8.0,24.0,8.0,2.0,7.0}),
            Map.entry("hollowwood/moon_shrine",new double[]{32.0,16.0,9.0,6.0,3.0}));
        static Shot roadShot(GravesAndGlory.Battle b,net.minecraft.entity.Entity player,Spec.StructuresRow s,Spec.LandmarksRow r,double[] a,String tag){
            var size=Structures.template(b.world,r.template()).get().getSize();
            double zs=Math.signum(a[1]-a[4]);
            for(double far:new double[]{0,6})for(double along:new double[]{0,5,-5,10,-10}){
                var eye=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),a[0]+along,0,a[1]+zs*far);
                var aim=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),a[2],a[3],a[4]);
                double cy=Math.max(r.elevation(),Arena.ground((int)Math.floor(eye.x),(int)Math.floor(eye.z)))+4;
                if(clearView(b,player,s,eye.x,cy,eye.z))return new Shot("lm-"+r.id()+"-"+tag,eye.x,cy,eye.z,aim.x,aim.y,aim.z);
            }
            System.out.println("[p3] note view lm-"+r.id()+"-"+tag+": no clear camera on the "+tag+" side, no picture");
            return null;
        }
        /** The single-column slivers on the shoulders of the landmarks (fix round 2: a column one block lower or higher than both neighbours on an axis stands as a dirt wedge in the grass): the cells of every landmark's
         *  shoulder (the blend distance between 0 and the shoulder) that Arena.height makes lower or higher than both of its neighbours along x, or along z. Cells within 16 of the river are left out (its banks have their own profile). */
        static int[] slivers(int n,StringBuilder first){
            var seen=new HashSet<Long>();int cells=0,bad=0;
            for(var s:Spec.STRUCTURES){
                if(!s.kind().equals("landmark"))continue;
                double sh=Arena.shoulder(s);int reachX=(int)Math.ceil(s.width()/2.0+2+sh+6),reachZ=(int)Math.ceil(s.depth()/2.0+2+sh+6);
                for(int x=s.x()-reachX;x<=s.x()+reachX;x++)for(int z=s.z()-reachZ;z<=s.z()+reachZ;z++){
                    double ld=Arena.landmarkDistance(s,x,z);if(!(ld>0&&ld<sh)||Math.abs(x-Arena.riverX(z))<16||!seen.add(key(x+1000,0,z+1000)))continue;
                    cells++;int c=Arena.height(x,z),a=Arena.height(x-1,z),e=Arena.height(x+1,z),no=Arena.height(x,z-1),so=Arena.height(x,z+1);
                    if(a>c&&e>c||a<c&&e<c||no>c&&so>c||no<c&&so<c){bad++;if(bad==1){System.out.println("[p3] note sliver window of "+s.id()+" at ("+x+","+z+"): smoothed heights, x across from "+(x-6)+", z down from "+(z-6)+", ld "+ld+", shoulder "+sh);for(int wz=-6;wz<=6;wz++){StringBuilder row=new StringBuilder();for(int wx=-6;wx<=6;wx++)row.append(String.format("%4d",Arena.height(x+wx,z+wz)));System.out.println("[p3] note   "+row);}System.out.println("[p3] note sliver window raw heights");for(int wz=-6;wz<=6;wz++){StringBuilder row=new StringBuilder();for(int wx=-6;wx<=6;wx++)row.append(String.format("%4d",Arena.rawHeight(x+wx,z+wz)));System.out.println("[p3] note   "+row);}}if(bad<=4)first.append(' ').append(s.id()).append(" (").append(x).append(',').append(z).append(") ").append(c).append(" among ").append(a).append(' ').append(e).append(' ').append(no).append(' ').append(so).append(';');}
                }
            }
            return new int[]{cells,bad};
        }
        /** The camera (eyes at cx,cy,cz) stands in air (a ground camera also outside every other footprint), and the lines from it to the centre and the four corners of the piece at its elevation+2 and +4 meet nothing before the footprint (one block round it). */
        static boolean clearView(GravesAndGlory.Battle b,net.minecraft.entity.Entity player,Spec.StructuresRow s,double cx,double cy,double cz){
            var w=b.world;var eye=BlockPos.ofFloored(cx,cy,cz);
            if(!w.getBlockState(eye).isAir()||!w.getBlockState(eye.down()).isAir())return false;
            if(cy<s.elevation()+20)for(var o:Spec.STRUCTURES)if(o!=s&&!o.kind().equals("bridge")&&Math.abs(cx-o.x())<=o.width()/2.0+2&&Math.abs(cz-o.z())<=o.depth()/2.0+2)return false;
            double hx=s.width()/2.0,hz=s.depth()/2.0;var from=new Vec3d(cx,cy,cz);
            for(double[] c:new double[][]{{0,0},{-hx,-hz},{hx,-hz},{-hx,hz},{hx,hz}})for(int up:new int[]{2,4}){
                var to=new Vec3d(s.x()+.5+c[0]*.8,s.elevation()+up,s.z()+.5+c[1]*.8);
                var hit=w.raycast(new net.minecraft.world.RaycastContext(from,to,net.minecraft.world.RaycastContext.ShapeType.OUTLINE,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
                if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.MISS){var q=hit.getBlockPos();if(Math.abs(q.getX()-s.x())>hx+1||Math.abs(q.getZ()-s.z())>hz+1)return false;}
            }
            return true;
        }

        /** Task 7: boxes of a template in local cells {x0,y0,z0,x1,y1,z1} that stay free of collision shapes: the passage of the gate (11 wide, 6 high), the four land gates of the keep (5 wide, 4 high, through the 3-thick walls) and its crenel. */
        static final Map<String,int[][]> OPENINGS=Map.ofEntries(
            Map.entry("ironhold/gate",new int[][]{{0,1,18,6,6,28}}),
            Map.entry("ironhold/keep",new int[][]{{7,1,0,11,4,2},{35,1,0,39,4,2},{7,1,24,11,4,26},{35,1,24,39,4,26},{14,7,0,15,8,0}}),
            // Task 8: the gate passage and the hall door of the lodge; the four doorways of the refuge; the doorway of the signal tower and the hatch over the last steps of its stair; the south door, the window and the kept-clear cells of the chapel;
            // the aisle of the sawmill (the lane runs through it); the foot of the moon gate and the gap between the pylons of the moon shrine
            Map.entry("frostmarch/lodge",new int[][]{{23,1,12,26,4,16},{16,1,9,16,3,11}}),
            Map.entry("frostmarch/refuge",new int[][]{{9,1,4,11,3,4},{9,1,12,11,3,12},{6,1,7,6,3,9},{14,1,7,14,3,9}}),
            Map.entry("frostmarch/signal_tower",new int[][]{{6,1,3,8,3,3},{4,18,7,4,18,9}}),
            Map.entry("hollowwood/chapel",new int[][]{{5,1,22,9,4,22},{11,2,22,14,3,22},{11,1,19,16,3,21}}),
            Map.entry("hollowwood/sawmill",new int[][]{{9,1,10,20,3,12}}),
            Map.entry("hollowwood/moon_shrine",new int[][]{{10,1,1,12,3,2},{10,1,16,12,5,17}}));
        record Goal(String name,int x,int y,int z){}
        /** Task 7: standing places (the feet cell, local) a squad has to be able to walk to from the ground outside, over doors, ladders and stairs: the tops of the towers and the walls, the lofts and floors. */
        static final Map<String,List<Goal>> REACH=Map.ofEntries(
            Map.entry("ironhold/gate",List.of(new Goal("north tower top",1,13,14),new Goal("south tower top",1,13,32),new Goal("north stub walk",2,11,6),new Goal("south stub walk",2,11,40))),
            Map.entry("ironhold/curtain",List.of(new Goal("wall-walk",1,12,15),new Goal("north tower guard room",2,12,2),new Goal("south tower platform",2,12,28))),
            Map.entry("ironhold/keep",List.of(new Goal("north wall crenel",14,7,0),new Goal("south wall-walk",20,7,25),new Goal("north west tower top",3,13,3),new Goal("donjon roof",36,18,13),new Goal("deck",23,1,13))),
            Map.entry("ironhold/hay_barn",List.of(new Goal("loft",12,7,6),new Goal("barn floor",5,1,6),new Goal("yard",12,1,16))),
            Map.entry("ironhold/garrison",List.of(new Goal("upper floor",14,6,16),new Goal("hall",15,1,15),new Goal("yard",15,1,5))),
            // Task 8 (local feet cells): the upper room of the lodge gatehouse (by its ladder), the hall, the wing and the yard; the hut, its north doorstep and the lean-to of the refuge; the floor, the top of the stair, the platform and the fire side of the signal tower;
            // the chapel (door, kept-clear cells, aisle, chancel, tower base); the sawmill (aisle, east end, saw deck, head of the ramp, bench); the logging camp (end of the lane, fire pit, hut, east edge, hoist); the moon shrine (gate, court, cloister, dais, moon gate)
            Map.entry("frostmarch/lodge",List.of(new Goal("gatehouse room",25,7,14),new Goal("hall",8,1,10),new Goal("wing",8,1,3),new Goal("yard",20,1,9))),
            Map.entry("frostmarch/refuge",List.of(new Goal("hut centre",10,1,8),new Goal("north doorstep",10,1,5),new Goal("lean-to",16,1,8))),
            Map.entry("frostmarch/signal_tower",List.of(new Goal("tower floor",7,1,7),new Goal("top of the stair",4,18,7),new Goal("platform",4,19,5),new Goal("fire side",7,19,5))),
            Map.entry("hollowwood/chapel",List.of(new Goal("door",7,1,21),new Goal("kept clear",13,1,20),new Goal("aisle",10,1,15),new Goal("chancel",23,1,15),new Goal("tower base",5,1,6))),
            Map.entry("hollowwood/sawmill",List.of(new Goal("aisle",12,1,11),new Goal("east end",20,1,13),new Goal("saw deck",10,3,8),new Goal("ramp head",14,3,6),new Goal("bench",4,1,11))),
            Map.entry("hollowwood/logging_camp",List.of(new Goal("end of the lane",10,1,8),new Goal("fire pit",5,1,8),new Goal("hut",5,1,3),new Goal("east edge",18,1,11),new Goal("hoist",5,1,12))),
            Map.entry("hollowwood/moon_shrine",List.of(new Goal("gate",11,1,17),new Goal("court",6,1,8),new Goal("cloister",4,1,7),new Goal("dais",11,2,4),new Goal("moon gate",11,1,2))));
        /** The control of the reach check (Task 7 fix round 1; the sealed cell inside solid stone it replaces could never be stood on, so it proved nothing): the pieces that have ladders are walked a second time with ladders switched off, and
         *  at least one of their standing places (a tower top, a loft) must then be out of reach. A walk that reached them anyway (a leak, a ledge that is a jump too low) would not be testing the ladders. The garrison has none. */
        static final Set<String> LADDERED=Set.of("ironhold/gate","ironhold/curtain","ironhold/keep","ironhold/hay_barn","frostmarch/lodge");
        /** Task 8 fix round 1 (review M1): the other pieces have no ladders to switch off, so their reach checks get a control of another kind. The piece is walked a second time with cells sealed (full blocks whatever is there): a doorway, a ramp, the door of a hut,
         *  or, for the two pieces that are open ruins and courts, every cell of the footprint's edge from y 1 to y 4 (the ring) except what is listed as a gap. `out` names exactly the standing places that must then be out of reach; every other one must still be reached.
         *  A control can fail both ways: a way in nobody sealed (a breach, a ledge a jump too low) leaves a place reached that should be out, and a seal that cuts too much takes a place that should stay. Every piece with a control has all of its places reached with nothing sealed (the check above). */
        record Control(String what,boolean ring,int[][] gaps,int[][] sealed,List<String> out){}
        static Control seal(String what,int[][] sealed,String... out){return new Control(what,false,new int[0][],sealed,List.of(out));}
        static Control ring(String what,int[][] gaps,String... out){return new Control(what,true,gaps,new int[0][],List.of(out));}
        static final Map<String,List<Control>> CONTROLS=Map.ofEntries(
            Map.entry("frostmarch/refuge",List.of(seal("the four doorways sealed",new int[][]{{9,1,4,11,3,4},{9,1,12,11,3,12},{6,1,7,6,3,9},{14,1,7,14,3,9}},"hut centre","north doorstep"))),
            Map.entry("frostmarch/signal_tower",List.of(seal("the doorway sealed",new int[][]{{6,1,3,8,3,3}},"tower floor","top of the stair","platform","fire side"))),
            Map.entry("hollowwood/chapel",List.of(ring("the whole edge of the footprint sealed",new int[0][],"door","kept clear","aisle","chancel","tower base"),ring("the edge sealed but the south door",new int[][]{{5,1,22,9,4,22}}))),
            Map.entry("hollowwood/sawmill",List.of(seal("the log ramp and the logs beside it sealed",new int[][]{{15,1,5,16,2,9}},"saw deck","ramp head"))),
            Map.entry("hollowwood/logging_camp",List.of(seal("the hut door sealed",new int[][]{{8,1,3,8,2,3}},"hut"))),
            Map.entry("hollowwood/moon_shrine",List.of(ring("the whole edge of the footprint sealed",new int[0][],"gate","court","cloister","dais","moon gate"),ring("the edge sealed but the two gates",new int[][]{{10,1,16,12,5,18},{10,1,0,12,3,2}}))));
        static boolean inBoxes(int[][] boxes,int x,int y,int z){for(var b:boxes)if(x>=b[0]&&x<=b[3]&&y>=b[1]&&y<=b[4]&&z>=b[2]&&z<=b[5])return true;return false;}
        /** What a player on foot can do: stand on a block with two free cells above it (or in a climbable one: a ladder, a vine), walk to a side cell at the same height, jump one block up, step off a ledge and fall up to three, climb a ladder up and down. Free: no collision
         *  shape, or one no higher than a carpet. A floor: a block whose collision shape is half a block to a block high (a slab, a stair, a block; not a fence, which is 1.5). */
        static final class Walk{
            final ServerWorld w;final BlockBox region;final Set<Long> seen=new HashSet<>();final boolean ladders;
            /** Task 8 fix round 1: cells (BlockPos.asLong) that count as a full block whatever the world holds there: the control walks seal a doorway, a ramp or the edge of a piece with them and look at what is then out of reach. */
            final Set<Long> sealed;
            Walk(ServerWorld w,BlockBox region){this(w,region,true,Set.of());}
            Walk(ServerWorld w,BlockBox region,boolean ladders){this(w,region,ladders,Set.of());}
            Walk(ServerWorld w,BlockBox region,boolean ladders,Set<Long> sealed){this.w=w;this.region=region;this.ladders=ladders;this.sealed=sealed;}
            boolean in(int x,int y,int z){return x>=region.getMinX()&&x<=region.getMaxX()&&y>=region.getMinY()&&y<=region.getMaxY()&&z>=region.getMinZ()&&z<=region.getMaxZ();}
            boolean climb(int x,int y,int z){var p=new BlockPos(x,y,z);return ladders&&!sealed.contains(p.asLong())&&w.getBlockState(p).isIn(net.minecraft.registry.tag.BlockTags.CLIMBABLE);}
            double top(int x,int y,int z){var p=new BlockPos(x,y,z);if(!sealed.isEmpty()&&sealed.contains(p.asLong()))return 1.0;var sh=w.getBlockState(p).getCollisionShape(w,p);return sh.isEmpty()?0:sh.getMax(Direction.Axis.Y);}
            boolean free(int x,int y,int z){return in(x,y,z)&&(climb(x,y,z)||top(x,y,z)<=0.2);}
            boolean floor(int x,int y,int z){double t=top(x,y-1,z);return t>=0.5&&t<=1.0;}
            boolean stand(int x,int y,int z){return free(x,y,z)&&free(x,y+1,z)&&(floor(x,y,z)||climb(x,y,z));}
            void search(List<BlockPos> sources){
                var queue=new ArrayDeque<int[]>();
                for(var s:sources)if(stand(s.getX(),s.getY(),s.getZ())&&seen.add(s.asLong()))queue.add(new int[]{s.getX(),s.getY(),s.getZ()});
                int[][] around={{1,0},{-1,0},{0,1},{0,-1}};
                while(!queue.isEmpty()){
                    var c=queue.poll();int x=c[0],y=c[1],z=c[2];
                    var next=new ArrayList<int[]>();
                    for(var d:around){
                        int nx=x+d[0],nz=z+d[1];
                        if(stand(nx,y,nz))next.add(new int[]{nx,y,nz});
                        else if(!free(nx,y,nz)||!free(nx,y+1,nz)){if(free(x,y+2,z)&&stand(nx,y+1,nz))next.add(new int[]{nx,y+1,nz});}
                        else for(int fall=1;fall<=3;fall++){int ny=y-fall;if(!free(nx,ny,nz))break;if(stand(nx,ny,nz)){next.add(new int[]{nx,ny,nz});break;}}
                    }
                    if(climb(x,y,z)){if(stand(x,y+1,z))next.add(new int[]{x,y+1,z});if(stand(x,y-1,z))next.add(new int[]{x,y-1,z});}
                    for(var n2:next)if(seen.add(new BlockPos(n2[0],n2[1],n2[2]).asLong()))queue.add(n2);
                }
            }
            boolean reached(BlockPos p){return seen.contains(p.asLong());}
            int count(){return seen.size();}
        }
        /** Task 7: the sally gaps of the curtain walls on map 1: seven blocks, z -65..-59 and 59..65, along both sides (x -74..-70 and 70..74), with nothing standing from the ground up. */
        void sallyGaps(ServerWorld w){
            int cells=0,shut=0;var at=new StringBuilder();
            for(int sx:new int[]{-72,72})for(int zz:new int[]{-62,62})for(int dx=-2;dx<=2;dx++)for(int dz=-3;dz<=3;dz++){
                int x=sx+dx,z=zz+dz,g=Arena.ground(x,z);cells++;boolean blocked=false;
                for(int y=g+1;y<=g+6;y++){var q=new BlockPos(x,y,z);if(!w.getBlockState(q).getCollisionShape(w,q).isEmpty())blocked=true;}
                if(blocked){shut++;if(shut<=4)at.append(' ').append(x).append(',').append(z);}
            }
            // and each gap stands between two sections: the column at its middle x, 4 either side of the gap's middle z (a tower of the section), has a block in the six above the ground
            int flank=0;
            for(int sx:new int[]{-72,72})for(int zz:new int[]{-62,62})for(int fz:new int[]{zz-4,zz+4}){
                int g=Arena.ground(sx,fz);boolean blocked=false;
                for(int y=g+1;y<=g+6;y++){var q=new BlockPos(sx,y,fz);if(!w.getBlockState(q).getCollisionShape(w,q).isEmpty())blocked=true;}
                if(blocked)flank++;
            }
            report("p3-lm-sally-map1",shut==0&&flank==8,cells+" columns of the four sally gaps (x -74..-70 and 70..74, z -65..-59 and 59..65), "+shut+" with a block in the six above the ground"+at+"; "+flank+" of 8 flanking columns (z -66, -58, 58 and 66 at x -72 and 72) stand");
        }
        /** Task 8: the signal fire of Frostmarch is the piece's now. MapDressing used to place it for the structures row "signal_tower" (netherrack at the top of the tower, fire above it, a cobblestone wall on each of the four sides); the row is replaced by fm-signal-tower, the loop
         *  over Spec.STRUCTURES finds no such id and places nothing, and the piece carries all of it: netherrack at the centre of the piece at elevation+19, fire above it, a cobblestone wall on each of its four sides (62, 83, 170 on the stated map), and no second netherrack, fire
         *  or cobblestone wall anywhere round it (the 15 x 15 footprint, elevation+6 to +31): the dressing did not put a second fire on top of the piece. */
        void signalFire(GravesAndGlory.Battle b){
            var w=b.world;boolean row=false;for(var st:Spec.STRUCTURES)if(st.id().equals("signal_tower"))row=true;
            Spec.LandmarksRow lm=null;for(var r:Spec.LANDMARKS)if(r.id().equals("fm-signal-tower"))lm=r;
            if(lm==null){report("p3-lm-fm-signal-fire",false,"no fm-signal-tower row");return;}
            int x=lm.x(),z=lm.z(),y=lm.elevation()+19;
            boolean nr=w.getBlockState(new BlockPos(x,y,z)).isOf(Blocks.NETHERRACK),fire=w.getBlockState(new BlockPos(x,y+1,z)).isOf(Blocks.FIRE);
            int walls=0;for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})if(w.getBlockState(new BlockPos(x+d[0],y,z+d[1])).isOf(Blocks.COBBLESTONE_WALL))walls++;
            int netherrack=0,fires=0,cobbleWalls=0;
            for(int dx=-7;dx<=7;dx++)for(int dz=-7;dz<=7;dz++)for(int yy=lm.elevation()+6;yy<=lm.elevation()+31;yy++){
                var st=w.getBlockState(new BlockPos(x+dx,yy,z+dz));
                if(st.isOf(Blocks.NETHERRACK))netherrack++;if(st.isOf(Blocks.FIRE))fires++;if(st.isOf(Blocks.COBBLESTONE_WALL))cobbleWalls++;
            }
            // Task 8 fix round 1 (review M2): the piece and the old dressing lay the same blocks in the same six cells, so counting them cannot tell a dressing that still lays its fire. The dressing is run for real: the six cells are emptied, MapDressing.setPieces() runs, and
            // not one of them may come back; then the control that the probe can see what it looks for: the old row put back in Spec.STRUCTURES, the cells emptied, setPieces() again, and all six must come back. The piece's own blocks are put back last.
            var six=new BlockPos[]{new BlockPos(x,y,z),new BlockPos(x,y+1,z),new BlockPos(x-1,y,z),new BlockPos(x+1,y,z),new BlockPos(x,y,z-1),new BlockPos(x,y,z+1)};
            var saved=new BlockState[6];for(int i=0;i<6;i++)saved[i]=w.getBlockState(six[i]);
            int again=0,control=0;var oldRows=Spec.STRUCTURES;
            try{
                again=dressingLays(w,six);
                var withRow=Arrays.copyOf(oldRows,oldRows.length+1);withRow[oldRows.length]=new Spec.StructuresRow("signal_tower","watchtower",x,z,14,14,19,"stone",lm.elevation());
                Spec.STRUCTURES=withRow;control=dressingLays(w,six);
            }finally{Spec.STRUCTURES=oldRows;for(int i=0;i<6;i++)w.setBlockState(six[i],saved[i],Block.NOTIFY_LISTENERS);}
            report("p3-lm-fm-signal-fire",!row&&nr&&fire&&walls==4&&netherrack==1&&fires==1&&cobbleWalls==4&&again==0&&control==6,"structures row signal_tower "+(row?"STILL THERE (the dressing would place a fire of its own)":"gone")+"; netherrack at "+xyz(new BlockPos(x,y,z))+" "+nr+", fire above it "+fire+", cobblestone walls on its four sides "+walls+"; in the 15 x 26 x 15 cells round it: "+netherrack+" netherrack, "+fires+" fire, "+cobbleWalls+" cobblestone wall blocks (want 1, 1 and 4); the six cells emptied and the dressing run (setPieces): "+again+" of 6 laid again (want 0); control, with the old signal_tower row put back: "+control+" of 6 laid (want 6)");
        }
        /** The six cells of the signal fire emptied, MapDressing.setPieces() run on the world, and how many of the six hold a block again. */
        static int dressingLays(ServerWorld w,BlockPos[] six){
            for(var q:six)w.setBlockState(q,Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS);
            new MapDressing(w).setPieces();
            int back=0;for(var q:six)if(!w.getBlockState(q).isAir())back++;
            return back;
        }
        /** Task 7: the hay line (four stacks of hay blocks at x -109 + 4 row, y 65..66, z -152..-150) that Arena.finishBuild used to lay on map 1 is gone: a hay block there is the barn's own (the template has hay at that cell) or there is none. */
        void hayLine(GravesAndGlory.Battle b){
            var w=b.world;Spec.LandmarksRow row=null;for(var r:Spec.LANDMARKS)if(r.id().equals("ih-hay-barn"))row=r;
            if(row==null){report("p3-lm-hay-line-gone",false,"no ih-hay-barn row to take over the hay line's job");return;}
            var size=Structures.template(w,row.template()).get().getSize();var nbt=Structures.read(b.server,row.template()).get();
            var pal=nbt.getList("palette",NbtElement.COMPOUND_TYPE);var hay=new HashSet<Integer>();
            for(int i=0;i<pal.size();i++)if(pal.getCompound(i).getString("Name").equals("minecraft:hay_block"))hay.add(i);
            var own=new HashSet<BlockPos>();var list=nbt.getList("blocks",NbtElement.COMPOUND_TYPE);
            for(int i=0;i<list.size();i++){var e=list.getCompound(i);if(!hay.contains(e.getInt("state")))continue;var pos=e.getList("pos",NbtElement.INT_TYPE);own.add(Structures.toWorld(size,row.x(),row.elevation(),row.z(),row.rotation(),pos.getInt(0),pos.getInt(1),pos.getInt(2)));}
            int old=0,bad=0;var at=new StringBuilder();
            for(int r=0;r<4;r++)for(int x=-109+r*4;x<-109+r*4+3;x++)for(int y=65;y<=66;y++)for(int z=-152;z<=-150;z++){
                var q=new BlockPos(x,y,z);if(!w.getBlockState(q).isOf(Blocks.HAY_BLOCK))continue;old++;
                if(!own.contains(q)){bad++;if(bad<=4)at.append(' ').append(xyz(q));}
            }
            report("p3-lm-hay-line-gone",bad==0,old+" hay blocks in the 4 x 18 cells of the old line, "+bad+" of them not the template's own ("+own.size()+" hay blocks in the template)"+at);
        }

        static boolean flagCell(Spec.PointsRow p,BlockPos q){
            int y=q.getY()-p.elevation();
            return q.getZ()==p.z()&&(q.getX()==p.x()&&y>=1&&y<=7||q.getX()>=p.x()+1&&q.getX()<=p.x()+3&&y>=4&&y<=5);
        }
        /** Task 8b: the flag column of a supply house, left void by its piece and raised by Arena.flag: |x-yardX|<=3, |z-yardZ|<=3, elevation+1 to elevation+8 (the shape of the flag column of an enclosed point). */
        static boolean reserved(Spec.SupplyHousesRow h,BlockPos q){
            int y=q.getY()-h.elevation();
            return Math.abs(q.getX()-h.yardX())<=3&&Math.abs(q.getZ()-h.yardZ())<=3&&y>=1&&y<=8;
        }
        /** The cells of a template that its file does not list (structure void) at local y 1 and up, with the flag of an enclosed point and the flag column of a supply house left out: {void cells, those not air in the world}; the first six not air go to where. */
        static int[] clearScan(ServerWorld w,Vec3i size,Set<Long> present,Spec.LandmarksRow r,Spec.PointsRow enclosed,Spec.SupplyHousesRow house,StringBuilder where){
            int voids=0,dirty=0;
            for(int lx=0;lx<size.getX();lx++)for(int ly=1;ly<size.getY();ly++)for(int lz=0;lz<size.getZ();lz++){
                if(present.contains(key(lx,ly,lz)))continue;var q=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,ly,lz);
                if(enclosed!=null&&flagCell(enclosed,q))continue;
                if(house!=null&&reserved(house,q))continue;
                voids++;
                if(!w.getBlockState(q).isAir()){dirty++;if(dirty<=6&&where!=null)where.append(' ').append(name(w.getBlockState(q))).append(xyz(q));}
            }
            return new int[]{voids,dirty};
        }
        /** Task 8b: the world end points of the cover and sight lines of a supply house at (x, z) with elevation E, from the formulas of the plan (not from toWorld); ty NaN is the ground at the end plus 2.6. */
        static Line houseLine(Spec.LandmarksRow r,String kind){
            int x=r.x(),z=r.z();double e=r.elevation();
            if(r.rotation()==0)return kind.equals("cover")?new Line(x+9.5,e+2.6,z+16.5,x+9.5,Double.NaN,z+30.5):new Line(x+10.5,e+7.6,z-7.5,x+1.5,Double.NaN,z+30.5);
            return kind.equals("cover")?new Line(x-8.5,e+2.6,z-15.5,x-8.5,Double.NaN,z-29.5):new Line(x-9.5,e+7.6,z+8.5,x-0.5,Double.NaN,z-29.5);
        }
        /** The world end points of a line are the values the plan states (null: the plan states none for it). */
        static boolean stated(Vec3d from,Vec3d to,Line e){
            return e==null||close(from.x,e.fx())&&close(from.y,e.fy())&&close(from.z,e.fz())&&close(to.x,e.tx())&&(Double.isNaN(e.ty())||close(to.y,e.ty()))&&close(to.z,e.tz());
        }
        /** The verdicts of the checks of a line, from the world: sight passes on a miss or a hit within 0.3 of the end, cover on a block hit more than 0.3 before it. */
        static boolean lineOk(ServerWorld w,net.minecraft.entity.Entity player,String kind,Vec3d from,Vec3d to){
            var hit=w.raycast(new net.minecraft.world.RaycastContext(from,to,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
            boolean blocked=hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK;double left=hit.getPos().distanceTo(to);
            return kind.equals("cover")?blocked&&left>0.3:!blocked||left<=0.3;
        }

        /** Task 8b, once per map with house rows, on its first one: what the -clear check of a house can fail on. The cell just outside the reserved column (local x offset +4) is pretended to be structure void (the file writes it as air) and a stone is put in it: the check
         *  must count it; a stone on the last cell of the column (offset +3, void in the file) it must not count; and with no reserved column at all the flag itself (its fence, cloth and lantern as far up as the piece's volume reaches: the pole of the house is taller than the piece, so for Rivergate the 8 blocks of fence under its roof line) and both stones are counted. */
        void clearControl(GravesAndGlory.Battle b,int n,Spec.LandmarksRow r){
            var w=b.world;Spec.SupplyHousesRow house=null;for(var hh:Spec.SUPPLYHOUSES)if(hh.landmark().equals(r.id()))house=hh;
            if(house==null){report("p3-lm-supply-clear-control-map"+n,false,"no supplyHouses row for "+r.id());return;}
            var size=Structures.template(w,r.template()).get().getSize();var list=Structures.read(b.server,r.template()).get().getList("blocks",NbtElement.COMPOUND_TYPE);
            var present=new HashSet<Long>();
            for(int i=0;i<list.size();i++){var pos=list.getCompound(i).getList("pos",NbtElement.INT_TYPE);present.add(key(pos.getInt(0),pos.getInt(1),pos.getInt(2)));}
            var absent=new HashSet<>(present);absent.remove(key(19,1,24));
            var inside=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),18,1,24);var outside=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),19,1,24);
            var stone=Blocks.STONE.getDefaultState();var keptIn=w.getBlockState(inside);var keptOut=w.getBlockState(outside);
            int[] real,in,out,bare;
            int pole=Arena.housePole(r.elevation()),flagIn=0;for(int y=1;y<size.getY();y++){if(y<=pole)flagIn++;if(y>=pole-2&&y<=pole-1)flagIn+=3;if(y==pole+1)flagIn++;}
            try{
                real=clearScan(w,size,present,r,null,house,null);
                w.setBlockState(inside,stone,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);in=clearScan(w,size,present,r,null,house,null);
                w.setBlockState(outside,stone,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);out=clearScan(w,size,absent,r,null,house,null);bare=clearScan(w,size,absent,r,null,null,null);
            }finally{w.setBlockState(inside,keptIn,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);w.setBlockState(outside,keptOut,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);}
            report("p3-lm-supply-clear-control-map"+n,real[1]==0&&in[1]==0&&out[1]==1&&bare[1]==flagIn+2,r.id()+": the check as it stands counts "+real[1]+" dirty cells (want 0); a stone at "+xyz(inside)+", the last cell of the reserved column, adds "+(in[1]-real[1])+" (want 0); a stone at "+xyz(outside)+", one cell outside it, is counted: "+out[1]+" (want 1); with no reserved column "+bare[1]+" cells are dirty (want "+(flagIn+2)+": the "+flagIn+" blocks of the flag of pole "+pole+" inside the piece's "+size.getY()+" layers and both stones)");
        }
        /** Task 8b, once per map with house rows, on its first one: the comparison with the plan's world points and the two verdicts of the lines can fail. */
        void linesControl(GravesAndGlory.Battle b,int n,Spec.LandmarksRow r,net.minecraft.entity.Entity player){
            var w=b.world;var size=Structures.template(w,r.template()).get().getSize();
            Vec3d sf=null,st=null,cf=null,ct=null;
            for(var l:Spec.LANDMARKLINES){
                if(!l.template().equals(r.template()))continue;
                Vec3d from=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),l.fromX(),l.fromY(),l.fromZ());
                Vec3d to=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),l.toX(),l.toY(),l.toZ());
                if(l.toY()==-1.0)to=new Vec3d(to.x,Arena.ground((int)Math.floor(to.x),(int)Math.floor(to.z))+2.6,to.z);
                if(l.kind().equals("sight")){sf=from;st=to;}else{cf=from;ct=to;}
            }
            if(sf==null||cf==null||player==null){report("p3-lm-supply-lines-control-map"+n,false,r.id()+": no sight or cover line, or no player");return;}
            var es=houseLine(r,"sight");var ec=houseLine(r,"cover");
            boolean plan=stated(sf,st,es)&&stated(cf,ct,ec);
            boolean off=!stated(sf,st,new Line(es.fx()+1,es.fy(),es.fz(),es.tx(),es.ty(),es.tz()))&&!stated(cf,ct,new Line(ec.fx(),ec.fy(),ec.fz(),ec.tx()+1,ec.ty(),ec.tz()));
            boolean sightFree=lineOk(w,player,"sight",sf,st);
            var m=sf.add(st).multiply(0.5);var mid=BlockPos.ofFloored(m.x,m.y,m.z);var keptMid=w.getBlockState(mid);
            boolean sightBlocked;
            try{w.setBlockState(mid,Blocks.STONE.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);sightBlocked=lineOk(w,player,"sight",sf,st);}
            finally{w.setBlockState(mid,keptMid,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);}
            boolean coverStands=lineOk(w,player,"cover",cf,ct);
            var removed=new ArrayList<BlockPos>();var keptBlocks=new ArrayList<BlockState>();boolean coverOpen;
            try{
                for(int i=0;i<8;i++){
                    var hit=w.raycast(new net.minecraft.world.RaycastContext(cf,ct,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
                    if(hit.getType()!=net.minecraft.util.hit.HitResult.Type.BLOCK)break;
                    removed.add(hit.getBlockPos());keptBlocks.add(w.getBlockState(hit.getBlockPos()));w.setBlockState(hit.getBlockPos(),Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
                }
                coverOpen=lineOk(w,player,"cover",cf,ct);
            }finally{for(int i=removed.size()-1;i>=0;i--)w.setBlockState(removed.get(i),keptBlocks.get(i),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);}
            boolean back=lineOk(w,player,"cover",cf,ct)&&lineOk(w,player,"sight",sf,st);
            report("p3-lm-supply-lines-control-map"+n,plan&&off&&sightFree&&!sightBlocked&&coverStands&&!coverOpen&&back,r.id()+": the world points are the plan's "+plan+"; one block out of place in a start or an end is "+(off?"seen":"NOT seen")+"; sight clear "+sightFree+", with a stone at "+xyz(mid)+" on it "+(sightBlocked?"STILL PASSES (it cannot fail)":"fails")+"; cover stands "+coverStands+", with the "+removed.size()+" blocks it hits taken away "+(coverOpen?"STILL PASSES (it cannot fail)":"fails")+"; put back, both pass again "+back);
        }

        /** The checks of every row of map n, on the server thread once the map is ready. */
        void check(GravesAndGlory.Battle b,int n){
            var w=b.world;var placed=newer(before);
            long sum=0;int rows=0,laid=0,slowCount=0;var slow=new StringBuilder();
            var player=b.server.getPlayerManager().getPlayerList().isEmpty()?null:b.server.getPlayerManager().getPlayerList().get(0);
            for(var r:Spec.LANDMARKS){
                if(r.map()!=n)continue;rows++;
                String id=r.id(),k="p3-lm-"+id;
                Structures.Placed p=null;for(var q:placed)if(q.row().equals(id))p=q;
                boolean gone=r.replaces().equals("-")||Arrays.stream(Spec.STRUCTURES).noneMatch(s->s.id().equals(r.replaces()));
                Spec.StructuresRow row=null;for(var s:Spec.STRUCTURES)if(s.id().equals(id)&&s.kind().equals("landmark"))row=s;
                report(k+"-placed",p!=null&&!p.missing()&&gone&&row!=null,(p==null?"no LOG entry of this row since the switch":"placed "+p.id()+" ("+p.blocks()+" blocks, "+p.nanos()/1000000+" ms, missing "+p.missing()+")")+"; replaced row "+r.replaces()+(gone?" gone":" still in Spec.STRUCTURES")+"; landmark row "+(row!=null?"there":"missing"));
                if(p==null||p.missing())continue;
                laid++;sum+=p.nanos();if(p.nanos()>100_000_000L){slowCount++;slow.append(' ').append(id).append('=').append(p.nanos()/1000000).append("ms");}
                var size=Structures.template(w,r.template()).get().getSize();
                var fp=Structures.footprint(size,r.x(),r.elevation(),r.z(),r.rotation());var bx=p.box();
                boolean same=bx.getMinX()==fp.getMinX()&&bx.getMinY()==fp.getMinY()&&bx.getMinZ()==fp.getMinZ()&&bx.getMaxX()==fp.getMaxX()&&bx.getMaxY()==fp.getMaxY()&&bx.getMaxZ()==fp.getMaxZ();
                boolean in=fp.getMinX()>=-350&&fp.getMaxX()<=350&&fp.getMinZ()>=-250&&fp.getMaxZ()<=250;
                report(k+"-fits",same&&in,"Placed.box "+bx+(same?" equals":" differs from")+" the footprint "+fp+"; inside |x|<=350, |z|<=250 "+in);
                // the template's cells as its file lists them (void cells are not in the file)
                var nbt=Structures.read(b.server,r.template()).get();var list=nbt.getList("blocks",NbtElement.COMPOUND_TYPE);
                var present=new HashSet<Long>();var columns=new HashSet<Long>();
                for(int i=0;i<list.size();i++){var pos=list.getCompound(i).getList("pos",NbtElement.INT_TYPE);present.add(key(pos.getInt(0),pos.getInt(1),pos.getInt(2)));columns.add(key(pos.getInt(0),0,pos.getInt(2)));}
                Spec.PointsRow enclosed=null;for(var pt:Spec.POINTS)if(pt.id().equals(r.encloses()))enclosed=pt;
                Spec.SupplyHousesRow house=null;for(var hh:Spec.SUPPLYHOUSES)if(hh.landmark().equals(id))house=hh;
                var where=new StringBuilder();var cs=clearScan(w,size,present,r,enclosed,house,where);int voids=cs[0],dirty=cs[1];
                report(k+"-clear",dirty==0,voids+" void cells at local y 1 and up (the flag of an enclosed point and the flag column of a supply house left out), "+dirty+" of them not air"+where);
                var scan=Walkability.scan(b,fp);int deck=0;
                for(String line:scan.detail().split("\n")){var f=line.split(" ");if(f.length>=3&&Walkability.onDeck(Integer.parseInt(f[1]),Integer.parseInt(f[2])))deck++;}
                report(k+"-roads",scan.through()==0&&deck==0,"over the footprint: cells "+scan.cells()+", through "+scan.through()+", blocked deck cells "+deck+", building "+scan.building()+", road "+scan.road());
                int near=0;var close=new StringBuilder();
                for(long col:columns){int lx=(int)(col>>24),lz=(int)(col&0xfff);var q=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,0,lz);
                    for(var pt:Spec.POINTS){if(pt==enclosed)continue;if(Math.hypot(q.getX()-pt.x(),q.getZ()-pt.z())<=19){near++;if(near<=3)close.append(' ').append(pt.id()).append(xyz(q));}}}
                String ringWhy=near+" template columns within 19 of a point it does not enclose"+close;boolean ringOk=near==0;
                if(enclosed!=null){
                    var pt=enclosed;int ring=0,ringBad=0,pad=0,padBad=0;boolean ringIsLaid=Arena.ringLaid(pt);
                    for(int dx=-pt.radius();dx<=pt.radius();dx++)for(int dz=-pt.radius();dz<=pt.radius();dz++){
                        double d=Math.hypot(dx,dz);int x=pt.x()+dx,z=pt.z()+dz;var q=new BlockPos(x,pt.elevation(),z);
                        if(ringIsLaid&&d>=pt.radius()-1&&d<=pt.radius()&&!Arena.wetBank(x,z)&&!Arena.deckCell(x,z)){ring++;if(!name(w.getBlockState(q)).equals("smooth_stone"))ringBad++;}
                        if(Math.abs(dx)<=1&&Math.abs(dz)<=1){pad++;if(!name(w.getBlockState(q)).equals("chiseled_stone_bricks"))padBad++;}
                    }
                    int pole=0;for(int y=1;y<=6;y++)if(name(at(w,pt.x(),pt.elevation()+y,pt.z())).equals("oak_fence"))pole++;
                    int cloth=0;for(int a=1;a<=3;a++)for(int y=4;y<=5;y++)if(name(at(w,pt.x()+a,pt.elevation()+y,pt.z())).endsWith("_wool"))cloth++;
                    boolean lamp=name(at(w,pt.x(),pt.elevation()+7,pt.z())).equals("sea_lantern");
                    ringOk&=ringIsLaid&&ring>0&&ringBad==0&&pad==9&&padBad==0&&pole==6&&cloth==6&&lamp;
                    ringWhy+="; enclosed "+pt.id()+": ring laid "+ringIsLaid+", smooth stone "+(ring-ringBad)+" of "+ring+", pad "+(pad-padBad)+" of 9, pole "+pole+" of 6, cloth "+cloth+" of 6, lantern "+lamp;
                    // gates: every cell within 2 of a road into the point, from radius 17.5 to the footprint edge
                    int roadsIn=0,cells=0,shut=0;var shutAt=new StringBuilder();
                    for(var rd:CampaignMaps.roads){
                        if(!(rd.ax()==pt.x()&&rd.az()==pt.z()||rd.bx()==pt.x()&&rd.bz()==pt.z()))continue;roadsIn++;
                        for(int x=fp.getMinX();x<=fp.getMaxX();x++)for(int z=fp.getMinZ();z<=fp.getMaxZ();z++){
                            if(CampaignMaps.distance(x,z,rd)>2||Math.hypot(x-pt.x(),z-pt.z())<17.5)continue;cells++;int g=Arena.ground(x,z);boolean blocked=false;
                            for(int y=g+1;y<=g+2;y++){var q=new BlockPos(x,y,z);if(!w.getBlockState(q).getCollisionShape(w,q).isEmpty())blocked=true;}
                            if(blocked){shut++;if(shut<=6)shutAt.append(' ').append(x).append(',').append(z);}
                        }
                    }
                    report(k+"-gates",roadsIn>0&&cells>0&&shut==0,roadsIn+" roads end at "+pt.id()+"; "+cells+" cells within 2 of them from radius 17.5 to the footprint edge, "+shut+" with a collision shape at ground+1 or +2"+shutAt);
                }
                report(k+"-ring",ringOk,ringWhy);
                for(var l:Spec.LANDMARKLINES){
                    if(!l.template().equals(r.template()))continue;
                    Vec3d from=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),l.fromX(),l.fromY(),l.fromZ());
                    Vec3d to=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),l.toX(),l.toY(),l.toZ());
                    if(l.toY()==-1.0)to=new Vec3d(to.x,Arena.ground((int)Math.floor(to.x),(int)Math.floor(to.z))+2.6,to.z);
                    var e=EXPECTED.get(id+"/"+l.kind());
                    if(e==null&&house!=null)e=houseLine(r,l.kind());
                    boolean stated=stated(from,to,e);
                    var hit=w.raycast(new net.minecraft.world.RaycastContext(from,to,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
                    boolean blocked=hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK;double left=hit.getPos().distanceTo(to);
                    boolean ok=l.kind().equals("cover")?blocked&&left>0.3:!blocked||left<=0.3;
                    String what=blocked?"hit "+name(w.getBlockState(hit.getBlockPos()))+xyz(hit.getBlockPos())+" "+String.format(Locale.ROOT,"%.2f",left)+" before the end":"no hit";
                    report(k+"-"+l.kind(),ok&&stated&&player!=null,l.id()+" "+v(from)+" to "+v(to)+(e==null?"":stated?" (the stated world points)":" (NOT the stated world points)")+": "+what);
                }
                var boxes=OPENINGS.get(r.template());
                if(boxes!=null){
                    int cellsOpen=0,shut=0;var shutAt=new StringBuilder();
                    for(var bx2:boxes)for(int lx=bx2[0];lx<=bx2[3];lx++)for(int ly=bx2[1];ly<=bx2[4];ly++)for(int lz=bx2[2];lz<=bx2[5];lz++){
                        var q=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,ly,lz);cellsOpen++;
                        if(!w.getBlockState(q).getCollisionShape(w,q).isEmpty()){shut++;if(shut<=4)shutAt.append(' ').append(name(w.getBlockState(q))).append(xyz(q));}
                    }
                    report(k+"-openings",shut==0&&cellsOpen>0,cellsOpen+" cells in "+boxes.length+" boxes of the template that stay open (the passage, the gates, the crenel), "+shut+" with a collision shape"+shutAt);
                }
                var goals=REACH.get(r.template());
                if(goals!=null){
                    var walk=new Walk(w,new BlockBox(fp.getMinX()-5,fp.getMinY()-1,fp.getMinZ()-5,fp.getMaxX()+5,fp.getMaxY()+3,fp.getMaxZ()+5));
                    var sources=new ArrayList<BlockPos>();
                    for(int x=fp.getMinX()-4;x<=fp.getMaxX()+4;x++)for(int z=fp.getMinZ()-4;z<=fp.getMaxZ()+4;z++){
                        if(x>=fp.getMinX()-2&&x<=fp.getMaxX()+2&&z>=fp.getMinZ()-2&&z<=fp.getMaxZ()+2)continue;
                        sources.add(new BlockPos(x,Arena.ground(x,z)+1,z));
                    }
                    walk.search(sources);
                    var missed=new StringBuilder();int got=0;
                    for(var g:goals){
                        var q=Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),g.x(),g.y(),g.z());
                        if(walk.reached(q))got++;else missed.append(' ').append(g.name()).append(xyz(q));
                    }
                    int noLadder=0;boolean control=true;
                    if(LADDERED.contains(r.template())){
                        var dry=new Walk(w,new BlockBox(fp.getMinX()-5,fp.getMinY()-1,fp.getMinZ()-5,fp.getMaxX()+5,fp.getMaxY()+3,fp.getMaxZ()+5),false);
                        dry.search(sources);
                        for(var g:goals)if(dry.reached(Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),g.x(),g.y(),g.z())))noLadder++;
                        control=noLadder<goals.size();
                    }
                    boolean sealOk=true;var sealWhy=new StringBuilder();
                    var controls=CONTROLS.get(r.template());
                    if(controls!=null)for(var c:controls){
                        var cells=new HashSet<Long>();
                        if(c.ring())for(int lx=0;lx<size.getX();lx++)for(int lz=0;lz<size.getZ();lz++){
                            if(lx!=0&&lx!=size.getX()-1&&lz!=0&&lz!=size.getZ()-1)continue;
                            for(int ly=1;ly<=4;ly++)if(!inBoxes(c.gaps(),lx,ly,lz))cells.add(Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,ly,lz).asLong());
                        }
                        for(var sb:c.sealed())for(int lx=sb[0];lx<=sb[3];lx++)for(int ly=sb[1];ly<=sb[4];ly++)for(int lz=sb[2];lz<=sb[5];lz++)cells.add(Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,ly,lz).asLong());
                        var sealedWalk=new Walk(w,new BlockBox(fp.getMinX()-5,fp.getMinY()-1,fp.getMinZ()-5,fp.getMaxX()+5,fp.getMaxY()+3,fp.getMaxZ()+5),true,cells);
                        sealedWalk.search(sources);
                        var out=new TreeSet<String>();
                        for(var g:goals)if(!sealedWalk.reached(Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),g.x(),g.y(),g.z())))out.add(g.name());
                        var want=new TreeSet<>(c.out());boolean ok=out.equals(want);sealOk&=ok;
                        sealWhy.append("; control (").append(c.what()).append(", ").append(cells.size()).append(" cells): ").append(goals.size()-out.size()).append(" of ").append(goals.size()).append(" reached, out of reach ").append(out).append(ok?"":" (WANT "+want+")");
                    }
                    report(k+"-reach",got==goals.size()&&control&&sealOk,got+" of "+goals.size()+" standing places reached on foot from the ground round the piece (walk, jump one block, ladders, stairs, fall up to three), "+walk.count()+" places in all"+(missed.length()==0?"":"; not reached:"+missed)+(LADDERED.contains(r.template())?"; control: with ladders switched off "+noLadder+" of "+goals.size()+" are reached"+(control?"":" (ALL OF THEM: the ladders are not what the walk needs)"):controls!=null?"":"; no ladders in this piece, no control")+sealWhy);
                }
            }
            // a map with no landmarks rows (maps 1 to 3 today) has nothing to place and passes; a map with rows passes only when every one of them was placed (a build that placed nothing is not within budget)
            report("p3-lm-budget-map"+n,laid==rows&&slowCount==0&&sum<=1_500_000_000L,(rows==0?"no landmarks rows on this map; ":"")+laid+" of "+rows+" rows placed, all together "+sum/1000000+" ms (at most 1500), over 100 ms:"+(slowCount==0?" none":slow.toString()));
            for(int[] wc:new int[][]{{0,18},{1,25},{2,18},{3,19},{4,18}})if(wc[0]==n)report("p3-lm-rows-map"+n,rows==wc[1],rows+" landmarks rows on map "+n+" (the plans state "+wc[1]+")");
            Spec.LandmarksRow houseRow=null;for(var r:Spec.LANDMARKS)if(r.map()==n&&r.template().startsWith("supply/")&&houseRow==null)houseRow=r;
            if(houseRow!=null&&laid==rows){clearControl(b,n,houseRow);linesControl(b,n,houseRow,player);}
            if(n==1){sallyGaps(w);hayLine(b);}
            if(n==2)signalFire(b);
            var sliverWhy=new StringBuilder();var sl=slivers(n,sliverWhy);
            report("p3-lm-slivers-map"+n,sl[1]==0,sl[0]+" shoulder columns of the landmarks, "+sl[1]+" lower or higher than both neighbours on an axis"+sliverWhy);
            int hx=GravesAndGlory.R.worldHalfX(),hz=GravesAndGlory.R.worldHalfZ();
            var loose=w.getEntitiesByType(EntityType.ITEM,new Box(-hx,w.getBottomY(),-hz,hx+1,w.getTopY(),hz+1),x->true);
            var kinds=new TreeMap<String,Integer>();for(var it:loose)kinds.merge(net.minecraft.registry.Registries.ITEM.getId(it.getStack().getItem()).getPath(),it.getStack().getCount(),Integer::sum);
            System.out.println("[p3] note landmarks map "+n+": "+loose.size()+" loose items after the build "+kinds);
            shots=plan(b,n);checked=true;
        }
        static long key(int x,int y,int z){return ((long)x<<24)|((long)y<<12)|z;}
        static boolean close(double a,double b){return Math.abs(a-b)<1e-6;}
        static String v(Vec3d p){return String.format(Locale.ROOT,"(%.1f,%.1f,%.1f)",p.x,p.y,p.z);}

        /** Map 4 is switched to; on the server tick, once its landmarks are queued, map 0; then until map 0 is ready, the queue and the LOG are watched. */
        void midQueue(GravesAndGlory.Battle b0){
            int map0=0;for(var r:Spec.LANDMARKS)if(r.map()==0)map0++;final int want=map0;
            int[] phase={0},ticks={0},queuedAt={-1};Set<Structures.Placed>[] snap=new Set[1];
            b0.campaign.switchMap(4);
            hook=()->{
                var b=GravesAndGlory.battle;if(b==null)return;
                if(++ticks[0]>20*900){hook=null;report("p3-lm-switch-mid-queue",false,"no result after 900 s (phase "+phase[0]+")");hookDone=true;return;}
                if(phase[0]==0){
                    if(b.campaign.map==4&&Structures.queued()>0){queuedAt[0]=Structures.queued();snap[0]=snapshot();b.campaign.switchMap(0);int left=Structures.queued();boolean ready=b.ready;phase[0]=1;
                        if(left!=0||ready){hook=null;report("p3-lm-switch-mid-queue",false,"switched to map 0 with "+queuedAt[0]+" map 4 landmarks queued: queued "+left+" right after (want 0), ready "+ready);hookDone=true;}}
                    return;
                }
                var after=newer(snap[0]);long kf=after.stream().filter(p->p.row().startsWith("kf-")).count(),rg=after.stream().filter(p->p.row().startsWith("rg-")).count();
                if(!b.ready)return;
                hook=null;
                boolean ok=kf==0&&rg==want&&Structures.queued()==0;
                report("p3-lm-switch-mid-queue",ok,"map 4 had "+queuedAt[0]+" landmarks queued when map 0 was switched to; the queue was empty at once; map 0 ready with "+Structures.queued()+" queued, "+rg+" of "+want+" rg- rows placed since the switch, "+kf+" kf- rows placed since the switch");
                hookDone=true;
            };
        }
        /** The cache marker of the map built last (map 0): current, stale with the salt, current again without it. */
        void cache(GravesAndGlory.Battle b){
            boolean built=b.campaign.cached();String h=Structures.hash(b.campaign.map);
            Structures.salt="test";boolean salted=b.campaign.cached();String hs=Structures.hash(b.campaign.map);
            Structures.salt="";boolean back=b.campaign.cached();
            report("p3-lm-cache",built&&!salted&&back&&!h.equals(hs),"map "+b.campaign.map+" after its build: cached "+built+" (hash "+h+"); with salt test: cached "+salted+" (hash "+hs+"); salt put back: cached "+back);
            // the pieces row of a template the map places is in the hash too (fix round 1): a row one block taller makes the marker stale, the row put back makes it current
            String template=null;for(var r:Spec.LANDMARKS)if(r.map()==b.campaign.map){template=r.template();break;}
            int at=-1;for(int i=0;i<Spec.PIECES.length;i++)if(Spec.PIECES[i].id().equals(template))at=i;
            if(at<0)report("p3-lm-cache-pieces",false,"map "+b.campaign.map+": no pieces row for its first landmark template "+template);
            else{
                var row=Spec.PIECES[at];String hp;boolean taller;
                try{Spec.PIECES[at]=new Spec.PiecesRow(row.id(),row.width(),row.depth(),row.height()+1,row.palette(),row.purpose());hp=Structures.hash(b.campaign.map);taller=b.campaign.cached();}
                finally{Spec.PIECES[at]=row;}
                boolean again=b.campaign.cached()&&Structures.hash(b.campaign.map).equals(h);
                report("p3-lm-cache-pieces",!hp.equals(h)&&!taller&&again,"map "+b.campaign.map+", pieces row "+row.id()+" one block taller: hash "+hp+(hp.equals(h)?" (the same: the pieces row is not in the hash)":" (differs)")+", cached "+taller+"; put back: hash and marker current "+again);
            }
            // the supplyHouses rows of the map are in the hash too (Task 8b fix round 1): a house one block higher makes the marker stale, the row put back makes it current
            int hi=-1;for(int i=0;i<Spec.SUPPLYHOUSES.length;i++)if(Spec.SUPPLYHOUSES[i].map()==b.campaign.map){hi=i;break;}
            if(hi<0)report("p3-lm-cache-houses",false,"map "+b.campaign.map+": no supplyHouses row");
            else{
                var row=Spec.SUPPLYHOUSES[hi];String hh;boolean higher;
                try{Spec.SUPPLYHOUSES[hi]=new Spec.SupplyHousesRow(row.id(),row.map(),row.name(),row.landmark(),row.yardX(),row.yardZ(),row.spurX(),row.spurZ(),row.elevation()+1);hh=Structures.hash(b.campaign.map);higher=b.campaign.cached();}
                finally{Spec.SUPPLYHOUSES[hi]=row;}
                boolean again=b.campaign.cached()&&Structures.hash(b.campaign.map).equals(h);
                report("p3-lm-cache-houses",!hh.equals(h)&&!higher&&again,"map "+b.campaign.map+", supplyHouses row "+row.id()+" one block higher: hash "+hh+(hh.equals(h)?" (the same: the supplyHouses rows are not in the hash)":" (differs)")+", cached "+higher+"; put back: hash and marker current "+again);
            }
            cacheDone=true;
        }
    }

    /** Task 8b (suite supply): the supply houses of the maps in GRAVES_PHASE3_MAPS (system property gravesandglory.phase3Maps, default all five), one map at a time (a launch of a map with four houses is about twelve minutes: eight marches of up to 3600 ticks).
     *  Each map is switched to (built afresh) and, once ready, per supplyHouses row of the map, on the server thread:
     *  p3-house-ID-neutral (the flag over the yard centre is the neutral one Arena.flag lays with Arena.housePole(elevation): oak_fence at yardX, elevation+1..+pole, a sea_lantern at +pole+1, white_wool at yardX+1..+3, +pole-2..+pole-1; the two braziers, local (10,3,36) and (20,3,36), are campfires with lit=false),
     *  p3-house-ID-yard (every cell of the circle of 10 round the yard centre has a collision shape at the elevation and none at +1 and +2, the pole cell excepted; a flood fill over such cells inside the footprint, from beside the pole, reaches the 9 cells of the
     *  front gate (local z 36, x 11..19) and the 5 of the side gate (local x 30, z 22..26), and the cells just outside, local (15,0,37) and (31,0,24), are such cells too),
     *  p3-house-ID-flag-lane (from the eye of a walker at the lane end, 1.62 over the ground, to the middle of each of the two rows of the cloth the first block met is the cloth: the white flag reads from the lane, which a flag of the height of a point's, six blocks, does not over the crest of the climb),
     *  p3-house-ID-spur (along the centre line from the lane end to the outer cell of the front gate: every cell a road cell, nothing with a collision shape at ground+1 and +2, the ground changes by at most 1 from cell to cell and is the elevation at the gate),
     *  and the pictures ID-spur, ID-gate, ID-yard, ID-lookout and ID-point (p3-ID-VIEW.png). Once per map the controls that the checks can fail: p3-house-neutral-control-mapN (a cyan flag, a lit brazier), p3-house-yard-control-mapN (a block in the circle,
     *  the gate sealed), p3-house-spur-control-mapN (a block on the centre line, the line moved 10 blocks aside), p3-house-flag-lane-control-mapN (a stone in the line, the cloth taken away). Then the route harness in house mode (MapRouteChecks with houses) over the map: p3-house-routes-mapN passes when every goal (a base and a point start for each
     *  house) had twenty of twenty knights at the yard. GRAVES_SUPPLY_ROUTES=off leaves the marches out (the checks and the pictures only). */
    static final class Supply implements Suite {
        record Res(boolean ok,String why){}
        record Shot(String name,double x,double y,double z,double tx,double ty,double tz){}
        final List<Integer> maps=new ArrayList<>();
        final boolean marches=!"off".equalsIgnoreCase(System.getenv("GRAVES_SUPPLY_ROUTES"));
        int index,stage,shot;
        volatile boolean switched,checked,marched;
        volatile List<Shot> shots=List.of();
        MapRouteChecks routes;
        Supply(){for(String s:System.getProperty("gravesandglory.phase3Maps","0,1,2,3,4").split(","))if(!s.isBlank())maps.add(Integer.parseInt(s.trim()));}
        /** Per map: the build, about 4500 ticks of pictures and eight marches of 3600 ticks at most. */
        @Override public int limit(){return 8000+maps.size()*45000;}
        @Override public void step(MinecraftClient c,int tick){
            if(tick<20)return;
            switch(stage){
                case 0->{
                    if(index>=maps.size()){finish(c,"supply");stage=9;return;}
                    int n=maps.get(index);switched=false;checked=false;
                    server(c,b->{b.campaign.switchMap(n);switched=true;});stage=1;
                }
                case 1->{
                    int n=maps.get(index);var b=GravesAndGlory.battle;
                    if(switched&&b!=null&&b.ready&&b.campaign.map==n&&CampaignMaps.selected==n){stage=2;server(c,x->check(x,n));}
                }
                case 2->{if(checked){shot=0;stage=3;}}
                case 3->{
                    if(shot>=shots.size()){
                        if(!marches){System.out.println("[p3] note supply map "+maps.get(index)+": the marches are left out (GRAVES_SUPPLY_ROUTES=off)");index++;stage=0;return;}
                        routes=new MapRouteChecks(c,new int[]{maps.get(index)},true);marched=false;stage=4;return;
                    }
                    var s=shots.get(shot);if(view(c,s.name(),s.x(),s.y(),s.z(),s.tx(),s.ty(),s.tz()))shot++;
                }
                case 4->{if(tick%10==0)server(c,this::pump);if(marched){index++;stage=0;}}
                default->{}
            }
        }
        @Override public void cleanup(MinecraftClient c){server(c,b->b.campaign.leash=true);}

        /** On the server thread, as the dev control calls it: one step of the route harness, and its verdict once it is done. */
        void pump(GravesAndGlory.Battle b){
            if(marched||routes==null)return;
            routes.tick();
            if(!routes.done)return;
            int n=maps.get(index),rows=0;for(var h:Spec.SUPPLYHOUSES)if(h.map()==n)rows++;
            int got=routes.reached.getOrDefault(n,0);
            int expect=2*rows+MapRouteChecks.nookGoals(n);
            report("p3-house-routes-map"+n,rows>0&&got==expect,got+" of "+expect+" goals with 20 of 20 knights at the yard: "+String.join(", ",routes.lines));
            marched=true;
        }

        static Vec3i sizeOf(Spec.LandmarksRow r){for(var p:Spec.PIECES)if(p.id().equals(r.template()))return new Vec3i(p.width(),p.height(),p.depth());return null;}
        static Spec.LandmarksRow landmarkOf(Spec.SupplyHousesRow h){for(var r:Spec.LANDMARKS)if(r.id().equals(h.landmark())&&r.map()==h.map())return r;return null;}
        static boolean shape(ServerWorld w,int x,int y,int z){var p=new BlockPos(x,y,z);return !w.getBlockState(p).getCollisionShape(w,p).isEmpty();}
        /** A cell of open ground: something to stand on at the elevation and nothing with a collision shape at +1 and +2. */
        static boolean open(ServerWorld w,int x,int e,int z){return shape(w,x,e,z)&&!shape(w,x,e+1,z)&&!shape(w,x,e+2,z);}
        static BlockPos local(Spec.LandmarksRow r,Vec3i size,int lx,int ly,int lz){return Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,ly,lz);}
        static Vec3d local(Spec.LandmarksRow r,Vec3i size,double lx,double ly,double lz){return Structures.toWorld(size,r.x(),r.elevation(),r.z(),r.rotation(),lx,ly,lz);}

        /** The flag over the yard centre is the neutral one and the braziers are unlit. */
        static Res neutral(ServerWorld w,Spec.SupplyHousesRow h,Spec.LandmarksRow r,Vec3i size){
            int e=h.elevation(),x=h.yardX(),z=h.yardZ();
            int top=Arena.housePole(e),pole=0;for(int y=1;y<=top;y++)if(name(at(w,x,e+y,z)).equals("oak_fence"))pole++;
            boolean lamp=name(at(w,x,e+top+1,z)).equals("sea_lantern");
            int white=0;var cloth=new TreeMap<String,Integer>();
            for(int a=1;a<=3;a++)for(int y=top-2;y<=top-1;y++){String nm=name(at(w,x+a,e+y,z));if(nm.equals("white_wool"))white++;cloth.merge(nm,1,Integer::sum);}
            int unlit=0;var other=new StringBuilder();
            for(int bx:new int[]{10,20}){
                var q=local(r,size,bx,3,36);var st=w.getBlockState(q);
                if(st.isOf(Blocks.CAMPFIRE)&&!st.get(Properties.LIT))unlit++;
                else other.append(' ').append(name(st)).append(xyz(q)).append(st.contains(Properties.LIT)?" lit="+st.get(Properties.LIT):"");
            }
            return new Res(pole==top&&lamp&&white==6&&unlit==2,"pole "+pole+" of "+top+" oak_fence from "+xyz(new BlockPos(x,e+1,z))+" up, sea_lantern at +"+(top+1)+" "+lamp+", white_wool "+white+" of 6 at +"+(top-2)+".."+(top-1)+" (the cloth is "+cloth+"), unlit campfires at the two braziers "+unlit+" of 2"+other);
        }

        /** The yard: flat and open inside the circle of 10, and the way from beside the pole to both gates. */
        static Res yard(ServerWorld w,Spec.SupplyHousesRow h,Spec.LandmarksRow r,Vec3i size){
            int e=h.elevation(),cx=h.yardX(),cz=h.yardZ(),cells=0,bad=0;var badAt=new StringBuilder();
            for(int dx=-10;dx<=10;dx++)for(int dz=-10;dz<=10;dz++){
                if(dx*dx+dz*dz>100)continue;cells++;
                boolean pole=dx==0&&dz==0,ok=shape(w,cx+dx,e,cz+dz)&&(pole||!shape(w,cx+dx,e+1,cz+dz)&&!shape(w,cx+dx,e+2,cz+dz));
                if(!ok){bad++;if(bad<=6)badAt.append(" (").append(cx+dx).append(',').append(cz+dz).append(") ").append(name(at(w,cx+dx,e,cz+dz))).append('/').append(name(at(w,cx+dx,e+1,cz+dz))).append('/').append(name(at(w,cx+dx,e+2,cz+dz))).append(';');}
            }
            var fp=Structures.footprint(size,r.x(),r.elevation(),r.z(),r.rotation());
            var seen=new HashSet<Long>();var queue=new ArrayDeque<int[]>();
            if(open(w,cx+1,e,cz)){seen.add(new BlockPos(cx+1,e,cz).asLong());queue.add(new int[]{cx+1,cz});}
            while(!queue.isEmpty()){
                var q=queue.poll();
                for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){
                    int nx=q[0]+d[0],nz=q[1]+d[1];
                    if(nx<fp.getMinX()||nx>fp.getMaxX()||nz<fp.getMinZ()||nz>fp.getMaxZ()||!open(w,nx,e,nz))continue;
                    if(seen.add(new BlockPos(nx,e,nz).asLong()))queue.add(new int[]{nx,nz});
                }
            }
            int front=0,side=0;
            for(int lx=11;lx<=19;lx++)if(seen.contains(local(r,size,lx,0,36).asLong()))front++;
            for(int lz=22;lz<=26;lz++)if(seen.contains(local(r,size,30,0,lz).asLong()))side++;
            var outF=local(r,size,15,0,37);var outS=local(r,size,31,0,24);
            boolean outFront=open(w,outF.getX(),e,outF.getZ()),outSide=open(w,outS.getX(),e,outS.getZ());
            return new Res(bad==0&&front==9&&side==5&&outFront&&outSide,cells+" cells in the circle of 10, "+bad+" not flat and open"+badAt+"; the flood fill from beside the pole ("+seen.size()+" cells) reaches "+front+" of 9 cells of the front gate and "+side+" of 5 of the side gate; the cell outside the front gate "+xyz(outF)+" is open ground "+outFront+", outside the side gate "+xyz(outS)+" "+outSide);
        }

        /** The spur: along x = spurX + shift from the lane end to the outer cell of the front gate. */
        static Res spur(ServerWorld w,Spec.SupplyHousesRow h,Spec.LandmarksRow r,Vec3i size,int shift){
            var gate=local(r,size,15,0,37);int x=h.spurX()+shift,z0=h.spurZ(),z1=gate.getZ(),dir=Integer.signum(z1-z0);
            int cells=0,off=0,blocked=0,maxStep=0,first=Arena.ground(x,z0),prev=first;var why=new StringBuilder();
            for(int z=z0;;z+=dir){
                cells++;int g=Arena.ground(x,z);
                if(!Arena.road(x,z)){off++;if(off<=3)why.append(" not road ").append(x).append(',').append(z).append(';');}
                for(int y=g+1;y<=g+2;y++)if(shape(w,x,y,z)){blocked++;if(blocked<=3)why.append(' ').append(name(at(w,x,y,z))).append(xyz(new BlockPos(x,y,z))).append(';');}
                maxStep=Math.max(maxStep,Math.abs(g-prev));prev=g;
                if(z==z1)break;
            }
            int last=Arena.ground(gate.getX(),gate.getZ());
            return new Res(off==0&&blocked==0&&maxStep<=1&&last==h.elevation(),cells+" cells along x "+x+" from z "+z0+" to the outer cell of the front gate "+xyz(gate)+": "+off+" not road cells, "+blocked+" with a collision shape at ground+1 or +2"+why+"; the largest step of the ground between neighbours is "+maxStep+" (want 1 at most); the ground is "+first+" at the lane end and "+last+" at the gate (the elevation is "+h.elevation()+")");
        }

        /** The flag from the lane (spec 7.1: the holder's colour reads from far off; Task 8b fix round 1): from the eye of a walker at the lane end (the spur point of the row, 1.62 over the ground) to the middle of each of the two rows of the cloth, over the yard centre. Each of the two
         *  lines must meet the cloth itself as the first block it meets: not the crest of the climb, not a wall or the roof, and not nothing (a miss means there is no cloth at the end of the line). The cloth stands at pole-2..pole-1 over the yard floor, the pole being Arena.housePole(elevation). */
        static Res lane(ServerWorld w,net.minecraft.entity.Entity player,Spec.SupplyHousesRow h){
            int x=h.yardX(),z=h.yardZ(),e=h.elevation(),top=Arena.housePole(e);
            var eye=new Vec3d(h.spurX()+.5,Arena.ground(h.spurX(),h.spurZ())+1.62,h.spurZ()+.5);
            int seen=0;var why=new StringBuilder();
            for(int y=top-2;y<=top-1;y++){
                var to=new Vec3d(x+2.5,e+y+.5,z+.5);
                var hit=w.raycast(new net.minecraft.world.RaycastContext(eye,to,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
                boolean block=hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK;var q=hit.getBlockPos();
                boolean cloth=block&&q.getX()>=x+1&&q.getX()<=x+3&&q.getY()==e+y&&q.getZ()==z&&name(w.getBlockState(q)).equals("white_wool");
                if(cloth)seen++;
                else why.append(" the line to the cloth row +").append(y).append(block?" meets "+name(w.getBlockState(q))+xyz(q)+" first, "+String.format(Locale.ROOT,"%.1f",hit.getPos().distanceTo(to))+" before its end;":" meets nothing (no cloth at "+xyz(BlockPos.ofFloored(to))+");");
            }
            return new Res(seen==2,"from the lane end "+String.format(Locale.ROOT,"(%.1f,%.1f,%.1f)",eye.x,eye.y,eye.z)+", "+Math.round(eye.distanceTo(new Vec3d(x+2.5,e+top,z+.5)))+" blocks from the cloth over the yard "+xyz(new BlockPos(x+2,e+top-1,z))+": "+seen+" of 2 lines of the cloth meet the cloth first"+why+"; the lowest pole over the yard floor that this ground would let both rows be seen from the lane end: "+lowestPole(w,player,h)+" (the house has "+top+")");
        }
        /** The smallest pole (4 to 24) whose cloth, at pole-2..pole-1 over the yard floor, both lines of lane() would see: the lines are cast to the cloth's cells' middles and must miss every block (the flag as built is below them, or beside the line, which ends over the yard) or hit one within 0.3 of the end. 0: none. */
        static int lowestPole(ServerWorld w,net.minecraft.entity.Entity player,Spec.SupplyHousesRow h){
            int x=h.yardX(),z=h.yardZ(),e=h.elevation();
            var eye=new Vec3d(h.spurX()+.5,Arena.ground(h.spurX(),h.spurZ())+1.62,h.spurZ()+.5);
            for(int pole=4;pole<=24;pole++){
                boolean all=true;
                for(int y=pole-2;y<=pole-1&&all;y++){
                    var to=new Vec3d(x+2.5,e+y+.5,z+.5);
                    var hit=w.raycast(new net.minecraft.world.RaycastContext(eye,to,net.minecraft.world.RaycastContext.ShapeType.COLLIDER,net.minecraft.world.RaycastContext.FluidHandling.NONE,player));
                    if(hit.getType()==net.minecraft.util.hit.HitResult.Type.BLOCK&&hit.getPos().distanceTo(to)>0.9)all=false;
                }
                if(all)return pole;
            }
            return 0;
        }

        static void set(ServerWorld w,BlockPos p,BlockState s){w.setBlockState(p,s,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);}

        /** The checks of every house of map n, on the server thread once the map is ready. */
        void check(GravesAndGlory.Battle b,int n){
            var w=b.world;var out=new ArrayList<Shot>();int houses=0;Spec.SupplyHousesRow first=null;
            var player=b.server.getPlayerManager().getPlayerList().isEmpty()?null:b.server.getPlayerManager().getPlayerList().get(0);
            for(var h:Spec.SUPPLYHOUSES){
                if(h.map()!=n)continue;houses++;if(first==null)first=h;
                var r=landmarkOf(h);var size=r==null?null:sizeOf(r);String k="p3-house-"+h.id();
                if(r==null||size==null){for(String part:new String[]{"neutral","yard","spur"})report(k+"-"+part,false,"no landmarks row or no pieces row for "+h.landmark());continue;}
                var ne=neutral(w,h,r,size);report(k+"-neutral",ne.ok(),ne.why());
                var ya=yard(w,h,r,size);report(k+"-yard",ya.ok(),ya.why());
                var sp=spur(w,h,r,size,0);report(k+"-spur",sp.ok(),sp.why());
                var la=lane(w,player,h);report(k+"-flag-lane",la.ok(),la.why());
                views(out,h,r,size);
            }
            int want=n==4?2:4;
            report("p3-house-rows-map"+n,houses==want,houses+" supplyHouses rows on map "+n+" (the plans state "+want+")");
            if(first!=null&&landmarkOf(first)!=null&&sizeOf(landmarkOf(first))!=null)controls(w,n,first,landmarkOf(first),player);
            shots=out;checked=true;
        }

        /** The five pictures of a house: local points go through Structures.toWorld, so their y is the elevation plus the local y; where no y is given the eye is at the ground+2.6. */
        void views(List<Shot> out,Spec.SupplyHousesRow h,Spec.LandmarksRow r,Vec3i size){
            String id=h.id();int e=h.elevation();
            Vec3d gate=local(r,size,15.5,3.0,36.5);
            out.add(new Shot(id+"-spur",h.spurX()+.5,Arena.ground(h.spurX(),h.spurZ())+2.6,h.spurZ()+.5,gate.x,gate.y,gate.z));
            var g2=local(r,size,15.5,0.0,47.5);
            out.add(new Shot(id+"-gate",g2.x,Arena.ground((int)Math.floor(g2.x),(int)Math.floor(g2.z))+2.6,g2.z,h.yardX()+.5,e+4,h.yardZ()+.5));
            var y0=local(r,size,15.5,2.6,33.5);var y1=local(r,size,15.5,3.0,11.5);
            out.add(new Shot(id+"-yard",y0.x,y0.y,y0.z,y1.x,y1.y,y1.z));
            for(var l:Spec.LANDMARKLINES){
                if(!l.template().equals(r.template())||!l.kind().equals("sight"))continue;
                var a=local(r,size,l.fromX(),l.fromY(),l.fromZ());var z=local(r,size,l.toX(),l.toY(),l.toZ());
                double ty=l.toY()==-1.0?Arena.ground((int)Math.floor(z.x),(int)Math.floor(z.z))+2.6:z.y;
                out.add(new Shot(id+"-lookout",a.x,a.y,a.z,z.x,ty,z.z));
            }
            Spec.PointsRow near=null;double best=Double.MAX_VALUE;
            for(var pt:Spec.POINTS){double d=Math.hypot(pt.x()-h.yardX(),pt.z()-h.yardZ());if(d<best){best=d;near=pt;}}
            if(near!=null){
                double dx=h.yardX()-near.x(),dz=h.yardZ()-near.z(),l=Math.hypot(dx,dz),px=near.x()+.5+dx/l*12,pz=near.z()+.5+dz/l*12;
                out.add(new Shot(id+"-point",px,Arena.ground((int)Math.floor(px),(int)Math.floor(pz))+2.6,pz,h.yardX()+.5,e+4,h.yardZ()+.5));
            }
        }

        /** Once per map, on its first house: what the neutral, yard and spur checks can fail on. Every block put in the world is taken out again. */
        void controls(ServerWorld w,int n,Spec.SupplyHousesRow h,Spec.LandmarksRow r,net.minecraft.entity.Entity player){
            var size=sizeOf(r);int e=h.elevation(),x=h.yardX(),z=h.yardZ();
            var stone=Blocks.STONE.getDefaultState();
            // neutral: the flag of Dawn (cyan) over the yard, then a brazier lit; each must fail, and the neutral flag and the brazier put back must pass again
            var flagCells=new ArrayList<BlockPos>();int top=Arena.housePole(e);for(int y=1;y<=top+1;y++)flagCells.add(new BlockPos(x,e+y,z));for(int a=1;a<=3;a++)for(int y=top-2;y<=top-1;y++)flagCells.add(new BlockPos(x+a,e+y,z));
            var keptFlag=new ArrayList<BlockState>();for(var q:flagCells)keptFlag.add(w.getBlockState(q));
            Arena.flag(w,x,e,z,0,top);var cyan=neutral(w,h,r,size);Arena.flag(w,x,e,z,-1,top);var white=neutral(w,h,r,size);
            for(int i=0;i<flagCells.size();i++)set(w,flagCells.get(i),keptFlag.get(i)); // the flag cells as the build left them (a map without the house flags has none)
            var brazier=local(r,size,10,3,36);var kept=w.getBlockState(brazier);
            set(w,brazier,Blocks.CAMPFIRE.getDefaultState().with(Properties.LIT,true));var lit=neutral(w,h,r,size);set(w,brazier,kept);var unlit=neutral(w,h,r,size);
            report("p3-house-neutral-control-map"+n,!cyan.ok()&&white.ok()&&!lit.ok()&&unlit.ok(),h.id()+": with the cyan flag the check "+(cyan.ok()?"PASSES (it cannot fail)":"fails")+", with the white flag put back "+(white.ok()?"passes":"FAILS")+", with the first brazier lit it "+(lit.ok()?"PASSES (it cannot fail)":"fails")+", unlit again "+(unlit.ok()?"passes":"FAILS"));
            // yard: a block in the circle; the cells of the front gate sealed (a block at +1 in each of the 9)
            var inCircle=new BlockPos(x+5,e+1,z);var keptCircle=w.getBlockState(inCircle);
            set(w,inCircle,stone);var blockedCircle=yard(w,h,r,size);set(w,inCircle,keptCircle);
            var gateCells=new ArrayList<BlockPos>();var keptGate=new ArrayList<BlockState>();
            for(int lx=11;lx<=19;lx++){var q=local(r,size,lx,1,36);gateCells.add(q);keptGate.add(w.getBlockState(q));}
            for(var q:gateCells)set(w,q,stone);
            var sealed=yard(w,h,r,size);
            for(int i=0;i<gateCells.size();i++)set(w,gateCells.get(i),keptGate.get(i));
            var again=yard(w,h,r,size);
            report("p3-house-yard-control-map"+n,!blockedCircle.ok()&&!sealed.ok()&&again.ok(),h.id()+": with a stone at "+xyz(inCircle)+" the check "+(blockedCircle.ok()?"PASSES (it cannot fail)":"fails")+" ("+blockedCircle.why()+"), with the front gate sealed it "+(sealed.ok()?"PASSES (it cannot fail)":"fails")+", both put back "+(again.ok()?"passes":"FAILS"));
            // spur: a block on the centre line at ground+1; the whole line moved 10 blocks aside (no road cells)
            int mz=(h.spurZ()+local(r,size,15,0,37).getZ())/2,mg=Arena.ground(h.spurX(),mz);
            var onLine=new BlockPos(h.spurX(),mg+1,mz);var keptLine=w.getBlockState(onLine);
            set(w,onLine,stone);var blockedLine=spur(w,h,r,size,0);set(w,onLine,keptLine);
            var aside=spur(w,h,r,size,10);var fine=spur(w,h,r,size,0);
            // flag from the lane: a stone put in the line to the lower row of the cloth (the cell at its middle); the cloth of both rows taken away; both put back
            var eye=new Vec3d(h.spurX()+.5,Arena.ground(h.spurX(),h.spurZ())+1.62,h.spurZ()+.5);var cloth=new Vec3d(x+2.5,e+top-1.5,z+.5);
            var inLine=BlockPos.ofFloored(eye.add(cloth.subtract(eye).multiply(0.5)));var keptIn=w.getBlockState(inLine);
            var clear=lane(w,player,h);
            set(w,inLine,stone);var shadowed=lane(w,player,h);set(w,inLine,keptIn);
            var clothCells=new ArrayList<BlockPos>();var keptCloth=new ArrayList<BlockState>();
            for(int a=1;a<=3;a++)for(int y=top-2;y<=top-1;y++){var q=new BlockPos(x+a,e+y,z);clothCells.add(q);keptCloth.add(w.getBlockState(q));}
            for(var q:clothCells)set(w,q,Blocks.AIR.getDefaultState());
            var bare=lane(w,player,h);
            for(int i=0;i<clothCells.size();i++)set(w,clothCells.get(i),keptCloth.get(i));
            var back=lane(w,player,h);
            report("p3-house-flag-lane-control-map"+n,player!=null&&clear.ok()&&!shadowed.ok()&&!bare.ok()&&back.ok(),h.id()+": "+(player==null?"NO PLAYER; ":"")+"as built the check "+(clear.ok()?"passes":"FAILS")+", with a stone in the line at "+xyz(inLine)+" it "+(shadowed.ok()?"PASSES (it cannot fail)":"fails")+" ("+shadowed.why().trim()+"), with the cloth taken away it "+(bare.ok()?"PASSES (it cannot fail)":"fails")+", both put back "+(back.ok()?"passes":"FAILS"));
            report("p3-house-spur-control-map"+n,!blockedLine.ok()&&!aside.ok()&&fine.ok(),h.id()+": with a stone on the centre line at "+xyz(onLine)+" the check "+(blockedLine.ok()?"PASSES (it cannot fail)":"fails")+", with the line moved 10 blocks aside it "+(aside.ok()?"PASSES (it cannot fail)":"fails")+", put back "+(fine.ok()?"passes":"FAILS"));
        }
    }
}
