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
        int index;
        @Override public int limit(){return 1200+400*pieces.size();}
        static String slug(String id){return id.replace('/','-');}
        @Override public void step(MinecraftClient c,int tick){
            if(tick==20){player=c.player.getUuid();c.setScreen(null);server(c,this::begin);}
            switch(stage){
                case 1->{
                    if(index>=pieces.size()){stage=4;server(c,this::roundtrip);}
                    else{stage=2;server(c,this::piece);}
                }
                case 3->{
                    int sx=size.getX(),sy=size.getY(),sz=size.getZ();double d=Math.max(sx,sz)+8;var o=StructureStudio.ORIGIN;
                    if(view(c,"studio-"+slug(pieces.get(index)),o.getX()+sx/2.0+0.7*d,o.getY()+sy+0.4*d,o.getZ()+sz/2.0+0.7*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0))stage=6;
                }
                case 6->{ // the north and west faces, which the first camera cannot see
                    int sx=size.getX(),sy=size.getY(),sz=size.getZ();double d=Math.max(sx,sz)+8;var o=StructureStudio.ORIGIN;
                    if(view(c,"studio-"+slug(pieces.get(index))+"-nw",o.getX()+sx/2.0-0.7*d,o.getY()+sy+0.4*d,o.getZ()+sz/2.0-0.7*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0)){stage=7;server(c,b->{dumpTrip(b);index++;stage=1;});}
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
        /** A cell the scan visits and finds clear: dry, not a deck plank, outside every footprint, nothing with a collision shape at ground+1 or +2. */
        static boolean plain(GravesAndGlory.Battle b,int x,int z){
            if(Arena.river(x,z)||Walkability.onDeck(x,z))return false;
            for(var s:Spec.STRUCTURES)if(!s.kind().equals("bridge")&&Walkability.inside(s,x,z))return false;
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

    /** Task 3 (suite camp): the camp base as a commander meets it. One launch, in this order: spawn (p3-camp-spawn), then on the world as built p3-camp-built, p3-camp-budget, p3-camp-stations and the war table of map 0 (p3-camp-war-table, first
     *  part), p3-camp-shapes (extra: every fence, wall, pane and bar inside the pieces is settled, every vine of the files stands, the game rule doVinesSpread is off), p3-camp-pages (every page the stations name opens its screen), p3-camp-pads (both pads put the squad on their team),
     *  p3-camp-leash (the three cases of the brief), the nine photographs (p3-camp-arrival.png and so on: the brief's seven and two of the south rim), the war table again after map 2 was built (second part), then a world made by an older build is planted
     *  (the old pads, the gold seal, a brick of the old tower, and entities of the older tags), the terrain marker is deleted, and CampWorld.build runs again as it does on the first load of an old world (p3-camp-old-gone),
     *  and last a build that has nothing to rebuild (p3-camp-rebuild-idempotent: both CRC32 readings and the build itself are in one server callback, so no random tick can grow a vine between the two readings). */
    static final class Camp implements Suite {
        static final Spec.CampRow C=Spec.CAMP[0];
        static final List<String> PAGES=new ArrayList<>();
        static{for(var s:Spec.CAMPSTATIONS)if(!PAGES.contains(s.page()))PAGES.add(s.page());}
        static final String[] VIEW_NAMES={"camp-arrival","camp-ridge","camp-aerial","camp-war-table","camp-gates-inside","camp-gates-outside","camp-archive","camp-south-pads","camp-south-oblique"};
        /** The first seven are the brief's, except camp-gates-outside, whose camera went from y 70 to y 71.5: the southern rim now stands up to y 69 at that spot (z -271), where the ground used to be 62, so the camera is one and a half blocks above the slope.
         *  The last two (fix round 1) look at the south rim as a commander meets it: from the west pad area toward the south-west, and from the east of the bowl toward the south-west across the gatehouse. */
        static final double[][] VIEWS={{0.5,67.6,-292.0,0.5,67,-330},{0.5,82,-361,0.5,64,-300},{0.5,110,-250,0.5,64,-320},{0.5,84,-361,0.5,78,-371},{0.5,67,-298,0.5,66,-276},{0.5,71.5,-271,0.5,67,-290},{30.5,68,-304,46,66,-304},{-9.5,66.6,-290.0,-36,76,-268},{30.5,66.6,-300.0,-30,76,-268}};
        int stage,waited,page,pad,leashCase,shotIndex;
        volatile boolean done;
        final StringBuilder pagesBad=new StringBuilder(),padsWhy=new StringBuilder(),leashWhy=new StringBuilder();
        boolean padsOk=true,leashOk=true;
        @Override public int limit(){return 9000;}
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
                case 0->{done=false;UUID me=c.player.getUuid();server(c,b->{b.campaign.leash=true;spawn(b,me);done=true;});stage=1;waited=0;}
                case 1->{if(done&&++waited>=20){done=false;server(c,b->{built(b);budget();stations(b);tableMap0(b);shapes(b);done=true;});stage=2;}}
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
                case 8->{ // map 2 (Frostmarch): built over, the war table follows
                    if(waited==0){server(c,b->{b.campaign.leash=true;if(b.campaign.map!=2)b.campaign.switchMap(2);done=true;});waited=1;}
                    else{var b=GravesAndGlory.battle;if(done&&b!=null&&b.ready&&b.campaign.map==2&&CampaignMaps.selected==2){done=false;server(c,x->{tableMap2(x);done=true;});stage=9;waited=0;}else if(++waited>3000){report("p3-camp-war-table",false,"map 2 was not ready after "+waited+" ticks");stage=9;done=true;}}
                }
                case 9->{if(done){done=false;server(c,b->{oldGone(b);done=true;});stage=10;}}
                case 10->{if(done){done=false;server(c,b->{idempotent(b);done=true;});stage=11;}}
                case 11->{if(done){finish(c,"camp");stage=12;}}
                default->{}
            }
        }

        /** p3-camp-spawn: after toLobby the player is within 0.1 of the camp spawn, on a block with a collision shape, with air at his feet and head. */
        void spawn(GravesAndGlory.Battle b,UUID me){
            var p=b.server.getPlayerManager().getPlayer(me);b.campaign.toLobby(p);
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
            int mark=CampWorld.LINES.size();CampWorld.build(w);boolean rebuilt=false;for(int i=mark;i<CampWorld.LINES.size();i++)if(CampWorld.LINES.get(i).contains("camp terrain ")&&!CampWorld.LINES.get(i).contains("skipped"))rebuilt=true;
            int stale=0;for(var e:w.iterateEntities()){var t=e.getCommandTags();if(t.contains("gravesandglory-camp-v3")||t.contains("gravesandglory-lobby")||t.contains("bannerbound-lobby"))stale++;}
            int concrete=0;for(int x:new int[]{-11,-10,-9,-8,8,9,10,11})for(int z=oz-14;z<=oz-11;z++)if(name(w.getBlockState(new BlockPos(x,64,z))).endsWith("concrete"))concrete++;
            boolean gold=w.getBlockState(new BlockPos(5,64,oz)).isOf(Blocks.GOLD_BLOCK),brick=!w.getBlockState(new BlockPos(-39,70,-339)).isAir();
            int villagers=0;for(var e:w.iterateEntities())if(e.getCommandTags().contains(CampWorld.TAG)&&e instanceof net.minecraft.entity.passive.VillagerEntity)villagers++;
            int want=0;for(var s:Spec.CAMPSTATIONS)if(!s.npc().equals("-"))want++;
            report("p3-camp-old-gone",standing==planted&&deleted&&rebuilt&&stale==0&&concrete==0&&!gold&&!brick&&villagers==want,"planted "+planted+" old entities, "+standing+" of them standing before the build, 32 concrete cells, a gold block and a brick (marker deleted "+deleted+", terrain rebuilt "+rebuilt+"); afterwards entities of the older tags "+stale+", concrete cells "+concrete+", gold block "+gold+", brick of the old tower "+brick+", camp villagers "+villagers+" (want "+want+")");
        }
        /** p3-camp-rebuild-idempotent: a second build logs camp terrain skipped; the CRC32 of the bowl box and the count of TAG entities are the same before and after. Both readings and the build are one callback. */
        void idempotent(GravesAndGlory.Battle b){
            var w=b.world;int mark=CampWorld.LINES.size();
            long before=crc(w);int n0=tagged(w);CampWorld.build(w);long after=crc(w);int n1=tagged(w);
            boolean skip=false;for(int i=mark;i<CampWorld.LINES.size();i++)if(CampWorld.LINES.get(i).contains("camp terrain skipped"))skip=true;
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
            report(name.apply("wear"),we.verge()>0&&we.ring()>0&&we.gaps()==0,"worn band: dirt path cells that touch a road cell on a side "+we.verge()+", plain ground left beside a road that the band should have covered "+we.gapsRoad()+"; worn ring: dirt path cells just outside the edge stone of a point "+we.ring()+", plain ground left in it "+we.gapsRing()+(we.gaps()==0?"":", first "+we.sample()));
            var pl=plazas(b,n);
            report(name.apply("plaza"),pl.cells()>0&&pl.wrong()==0,"plaza cells (within 9.2 blocks of a flag pole, off the 3x3 pad, the water and the buildings) "+pl.cells()+", not the block of the pattern (earth, a compass of trim, a ring of paving) "+pl.wrong()+(pl.wrong()==0?"":", first: "+pl.sample()));
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
                    if((top.isOf(Blocks.COARSE_DIRT)||top.isOf(Blocks.GRAVEL))&&!Arena.river(x,z)){ // trampled earth is the floor of a plaza and the worn strips of dirt path: coarse dirt or gravel anywhere else is a patch
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
        record Wear(int verge,int ring,int gapsRoad,int gapsRing,String sample){int gaps(){return gapsRoad+gapsRing;}}
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
            for(var p:Spec.POINTS){
                if(!Arena.ringLaid(p))continue;
                int r=p.radius();
                for(int dx=-r-4;dx<=r+4;dx++)for(int dz=-r-4;dz<=r+4;dz++){
                    if(!MapDressing.ringCell(dx,dz,r+.5))continue;
                    int x=p.x()+dx,z=p.z()+dz;var bd=MapDressing.bandOf(p,dx,dz);
                    if(Arena.wetBank(bd[0],bd[1])||Arena.deckCell(bd[0],bd[1]))continue;
                    if(w.getBlockState(new BlockPos(x,Arena.height(x,z),z)).isOf(Blocks.DIRT_PATH))ring++;
                    else if(d.wearable(x,z)){gg++;if(gr+gg<=6)sample.append(sample.length()>0?"; ":"").append("ring of ").append(p.id()).append(" (").append(x).append(", ").append(z).append(") with ").append(around(w,x,z));}
                }
            }
            return new Wear(verge,ring,gr,gg,sample.toString());
        }
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
     *  building (spur<k>-<building>; a spur is walked up to the wall of its building).
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
    static final class Walkthrough implements Suite {
        static final net.minecraft.world.Heightmap.Type FEET=net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES;
        record Stop(String name,double arc,double tx,double ty,double tz){}

        /** A road to walk: its centre line c, the line q a walker keeps to (off blocks to his right of c, mitred at the bends) with the length arc of q up to each vertex. */
        static final class Leg {
            final String id;final double off,len;final double[][] c,q;final double[] arc;final List<Stop> stops=new ArrayList<>();
            int stuck,slow,bumps,forced,ticks,photos;double walked,ymin=1e9,ymax=-1e9,stuckArc=-1;boolean finished,failed;
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
            report("p3-wt"+n+"-leg-"+leg.id,leg.finished&&leg.stuck==0,String.format(Locale.ROOT,"%s lane of %.0f blocks walked in %d ticks (%.1f s): stuck windows %d, slow windows %d, forced steps %d, auto-jumps %d, y %.2f to %.2f, pictures %d",leg.finished?"finished":"NOT finished,",leg.len,leg.ticks,leg.ticks/20.0,leg.stuck,leg.slow,leg.forced,leg.bumps,leg.ymin,leg.ymax,leg.photos));
        }
        /** One picture: the file is p3-wt<N>-<sequence>-<tag>.png, and its check line says where the walker stood and looked. */
        void took(MinecraftClient c,String tag,String what,Leg leg){
            String file="wt"+n+"-"+String.format(Locale.ROOT,"%02d",++seq)+"-"+tag;shot(c,file);photoTotal++;if(leg!=null)leg.photos++;
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
                if(!placed){if(t>200){report("p3-wt"+n+"-port-"+label,false,"the server did not place the walker");return true;}return false;}
                if(p.squaredDistanceTo(x,feet,z)>2.25){if(t>400){report("p3-wt"+n+"-port-"+label,false,"the walker did not arrive at ("+x+", "+feet+", "+z+"), he stands at ("+p.getX()+", "+p.getY()+", "+p.getZ()+")");return true;}return false;}
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

        /** The walk: the real client player runs along a lane with the forward and sprint keys held, steering by his yaw alone (the lane line five blocks ahead), and stops for each of its pictures. */
        final class LegJob extends Job {
            final Leg leg;int seg,stopIx,phaseT,jump,win,run;String phase="walk";double wx,wz;boolean hasWin,ground=true;
            LegJob(Leg leg){this.leg=leg;}
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
                leg.ymin=Math.min(leg.ymin,p.getY());leg.ymax=Math.max(leg.ymax,p.getY());
                if(p.getY()<Arena.Y-6||p.isDead()){release(c);leg.failed=true;report("p3-wt"+n+"-fell-"+leg.id,false,String.format(Locale.ROOT,"the walker fell or died at (%.1f, %.1f, %.1f), %.0f of %.0f blocks along lane %s",px,p.getY(),pz,arc,leg.len,leg.id));return true;}
                if(seg==m-2&&sl-u<1.2){release(c);leg.finished=true;return true;}
                if(t>leg.len*8+900){release(c);leg.failed=true;report("p3-wt"+n+"-timeout-"+leg.id,false,String.format(Locale.ROOT,"lane %s not finished after %d ticks, the walker is at (%.1f, %.1f, %.1f), %.0f of %.0f blocks",leg.id,t,px,p.getY(),pz,arc,leg.len));return true;}
                boolean g=p.isOnGround();
                if(phase.equals("walk")){
                    if(stopIx<leg.stops.size()&&arc>=leg.stops.get(stopIx).arc()-1.8){phase="brake";phaseT=0;release(c);ground=g;return false;}
                    double[] a=leg.at(arc+5.0);
                    float want=(float)Math.toDegrees(Math.atan2(-(a[0]-px),a[1]-pz)),cur=p.getYaw(),d=wrap(want-cur);
                    p.setYaw(cur+Math.max(-25f,Math.min(25f,d)));p.prevYaw=p.getYaw();p.setPitch(4f);p.prevPitch=4f;
                    boolean go=Math.abs(wrap(want-p.getYaw()))<35f;
                    o.forwardKey.setPressed(go);o.sprintKey.setPressed(go);o.jumpKey.setPressed(jump>0);if(jump>0)jump--;
                    if(ground&&!g&&p.getVelocity().y>0.2&&!o.jumpKey.isPressed()){
                        leg.bumps++;if(leg.bumps<=10)note(String.format(Locale.ROOT,"auto-jump %d on lane %s at (%.1f, %.1f, %.1f), %.0f of %.0f blocks; ahead: %s",leg.bumps,leg.id,px,p.getY(),pz,arc,leg.len,ahead(c)));
                    }
                    if(go)watch(c,px,pz,arc);
                }else if(phase.equals("brake")){
                    release(c);
                    if(p.getVelocity().horizontalLength()<0.03||++phaseT>30){phase="look";phaseT=0;}
                }else if(phase.equals("look")){
                    var st=leg.stops.get(stopIx);var v=MapTour.look(st.name(),p.getX(),p.getEyeY(),p.getZ(),st.tx(),st.ty(),st.tz());
                    p.setYaw(v.yaw());p.setPitch(v.pitch());p.prevYaw=v.yaw();p.prevPitch=v.pitch();phase="wait";phaseT=0;
                }else if(phase.equals("wait")){
                    var st=leg.stops.get(stopIx);
                    if(++phaseT>=24&&(terrainReady(c,p.getX(),p.getZ())||phaseT>=240)){
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
                    report("p3-wt"+n+"-stuck-"+leg.id+"-"+leg.stuck,false,String.format(Locale.ROOT,"gained %.1f blocks in 2 s at (%.1f, %.1f, %.1f), %.0f of %.0f blocks along lane %s; ahead: %s",moved,px,c.player.getY(),pz,arc,leg.len,leg.id,ahead(c)));
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
                    if(s.kind().equals("bridge")||id.matches("house_\\d|farm|mill|(dawn|dusk)_gate")||id.contains("_tower_"))continue; // those are seen from their roads, the gates from the dais
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
        /** Top blocks that cannot belong to this map: the snow and ice of Frostmarch, the podzol, ferns and mud bricks of Hollowwood, grass on the two maps without it, the leaves of the other family of trees. */
        static Set<String> alien(int map){
            var s=new HashSet<String>();
            if(map!=2)s.addAll(List.of("snow","snow_block","powder_snow","packed_ice","blue_ice","ice"));
            if(map!=3)s.addAll(List.of("podzol","fern","mud_bricks"));
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
        /** The floor of the scan is Arena.Y-8, its top Arena.Y+80. A solid block that touches (face, edge or corner: a tent of wool rows meets at its edges) nothing that leads back to the floor is part of a floating piece; the pieces are counted and listed. */
        void floating(ServerWorld w,int hx,int hz,net.minecraft.entity.Entity ent){
            int y0=Arena.Y-8,y1=Arena.Y+80,wz=2*hz+1,hy=y1-y0+1,cx0=(-hx)>>4,cz0=(-hz)>>4,ncx=(hx>>4)-cx0+1,ncz=(hz>>4)-cz0+1;
            var chunks=new net.minecraft.world.chunk.Chunk[ncx*ncz];
            for(int a=0;a<ncx;a++)for(int c=0;c<ncz;c++)chunks[a*ncz+c]=w.getChunk(cx0+a,cz0+c);
            var seen=new BitSet((2*hx+1)*wz*hy);var pos=new BlockPos.Mutable();
            int[] st=new int[1<<20];int sp=0;long reached=0;
            int[] dx=new int[26],dy=new int[26],dz=new int[26];int q=0;
            for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++)for(int c=-1;c<=1;c++)if(a!=0||b!=0||c!=0){dx[q]=a;dy[q]=b;dz[q]=c;q++;}
            for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++){
                var ch=chunks[((x>>4)-cx0)*ncz+((z>>4)-cz0)];
                if(solid(ch.getBlockState(pos.set(x,y0,z)))){int id=((x+hx)*wz+(z+hz))*hy;if(!seen.get(id)){seen.set(id);if(sp==st.length)st=Arrays.copyOf(st,sp*2);st[sp++]=id;}}
            }
            while(sp>0){
                int id=st[--sp];reached++;int y=id%hy+y0,r=id/hy,z=r%wz-hz,x=r/wz-hx;
                for(int k=0;k<26;k++){
                    int nx=x+dx[k],ny=y+dy[k],nz=z+dz[k];
                    if(nx<-hx||nx>hx||nz<-hz||nz>hz||ny<y0||ny>y1)continue;
                    int nid=((nx+hx)*wz+(nz+hz))*hy+(ny-y0);if(seen.get(nid))continue;
                    if(!solid(chunks[((nx>>4)-cx0)*ncz+((nz>>4)-cz0)].getBlockState(pos.set(nx,ny,nz))))continue;
                    seen.set(nid);if(sp==st.length)st=Arrays.copyOf(st,sp*2);st[sp++]=nid;
                }
            }
            var found=new ArrayList<Piece>();long floatTotal=0;
            for(int x=-hx;x<=hx;x++)for(int z=-hz;z<=hz;z++)for(int y=y0;y<=y1;y++){
                int id=((x+hx)*wz+(z+hz))*hy+(y-y0);if(seen.get(id))continue;
                if(!solid(chunks[((x>>4)-cx0)*ncz+((z>>4)-cz0)].getBlockState(pos.set(x,y,z))))continue;
                seen.set(id);sp=0;st[sp++]=id;int size=0,bx0=x,by0=y,bz0=z,bx1=x,by1=y,bz1=z;var names=new TreeMap<String,Integer>();
                while(sp>0){
                    int cid=st[--sp];size++;int cy=cid%hy+y0,r=cid/hy,cz=r%wz-hz,cx=r/wz-hx;
                    bx0=Math.min(bx0,cx);bx1=Math.max(bx1,cx);by0=Math.min(by0,cy);by1=Math.max(by1,cy);bz0=Math.min(bz0,cz);bz1=Math.max(bz1,cz);
                    names.merge(name(chunks[((cx>>4)-cx0)*ncz+((cz>>4)-cz0)].getBlockState(pos.set(cx,cy,cz))),1,Integer::sum);
                    for(int k=0;k<26;k++){
                        int nx=cx+dx[k],ny=cy+dy[k],nz=cz+dz[k];
                        if(nx<-hx||nx>hx||nz<-hz||nz>hz||ny<y0||ny>y1)continue;
                        int nid=((nx+hx)*wz+(nz+hz))*hy+(ny-y0);if(seen.get(nid))continue;
                        if(!solid(chunks[((nx>>4)-cx0)*ncz+((nz>>4)-cz0)].getBlockState(pos.set(nx,ny,nz))))continue;
                        seen.set(nid);if(sp==st.length)st=Arrays.copyOf(st,sp*2);st[sp++]=nid;
                    }
                }
                floatTotal+=size;var topNames=new ArrayList<>(names.entrySet());topNames.sort((p,o)->o.getValue()-p.getValue());
                var tn=new StringBuilder();for(int i=0;i<Math.min(3,topNames.size());i++)tn.append(i>0?", ":"").append(topNames.get(i).getKey()).append(' ').append(topNames.get(i).getValue());
                found.add(new Piece(size,bx0,by0,bz0,bx1,by1,bz1,tn.toString(),null));
            }
            found.sort((p,o)->o.size()-p.size());
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
}
