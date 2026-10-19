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
     *  shipped structure file; export builds each, writes GRAVES_STUDIO_OUT/GROUP/NAME.nbt and photographs the piece. Both end with the dump round trip of the marker. */
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
                    if(view(c,"studio-"+slug(pieces.get(index)),o.getX()+sx/2.0+0.7*d,o.getY()+sy+0.4*d,o.getZ()+sz/2.0+0.7*d,o.getX()+sx/2.0,o.getY()+sy/3.0,o.getZ()+sz/2.0)){index++;stage=1;}
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
            }
            index++;stage=1;
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

    /** The arena build (suite arena). The maps 0 to 4 are built one over the other on one world, and after each finish the world is looked at.
     *  The 0.4.0 soak, Finding 1: an arena rebuilt over an arena spilled loose items (a plant, lantern or crop whose support the build had cleared first).
     *  The item entities loaded in the built area are counted after map 0 and after map 1 was built over it: p3-arena-items passes when the second count is 0
     *  (the safety net in Arena.finish discards what is loaded); p3-arena-spill passes when the second finish found nothing to discard, that is when the build itself spilled none
     *  (it writes without neighbour shape updates); p3-arena-shapes passes when no glass pane of the structures is left without a connection. All three see the chunks that are loaded,
     *  and the shapes check counts glass panes only (fences and walls are not looked at).
     *  Task 2, fix round 1, per map: p3-arena-river-map<N> passes when no block stands above the water of a river column that the build of that map does not account for, which is what
     *  the map built before it leaves behind (river). */
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
        /** On the server thread, with the map that stands (the at-th of MAPS): the item and pane counts of the soak check after maps 0 and 1, then the river of this map. */
        void measure(GravesAndGlory.Battle x,int n,int at){
            if(at==0){before=items(x);firstSpill=Arena.discarded;}
            if(at==1){after=items(x);spill=Arena.discarded;var pn=panes(x);paneCount=pn[0];paneLone=pn[1];}
            var r=river(x);
            report("p3-arena-river-map"+n,r.left()==0,"river columns "+r.columns()+", built on by this map "+r.builtOn()+", looked at "+r.looked()+" (of them "+r.flat()+" with the ground at "+Arena.Y+" or above): "+r.left()+" blocks above the water that this map does not account for"+(r.left()==0?"":", first: "+r.sample()));
            try{Path file=MinecraftClient.getInstance().runDirectory.toPath().resolve("screenshots/p3-arena-river-map"+n+".txt");Files.createDirectories(file.getParent());Files.writeString(file,r.detail());}
            catch(Exception e){report("p3-arena-river-map"+n+"-file",false,e.toString());}
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
        /** Where Arena.finish builds above the water of a river column on purpose: a bridge (deck planks and the two rail rows), the footprint of any other structure, a capture ring (its band of smooth stone, the edge stone and the
         *  boundary posts MapDressing puts on the radius and just outside it, which on the wider river of Hollowwood reach its outermost column; the 3x3 centre with the flag pole, the banner three cells east of the pole) and, on map 0, the fishing piers MapDressing puts on the river banks. */
        static boolean builtOn(int x,int z){
            for(var s:Spec.STRUCTURES){
                if(s.kind().equals("bridge")){int x0=s.x()-s.width()/2,z0=s.z()-s.depth()/2;if(x>=x0&&x<x0+s.width()&&z>=z0-1&&z<=z0+s.depth())return true;}
                else if(Walkability.inside(s,x,z))return true;
            }
            for(var p:Spec.POINTS){
                int dx=x-p.x(),dz=z-p.z();double d=Math.hypot(dx,dz);
                if(d>=p.radius()-1.5&&d<=p.radius()+2.5)return true;
                if(Math.abs(dx)<=1&&Math.abs(dz)<=1)return true;
                if(dz==0&&dx>=1&&dx<=3)return true;
            }
            if(CampaignMaps.selected==0)for(int pz:new int[]{-215,-72,72,215})for(int bank:new int[]{-1,1}){
                int from=(int)Math.round(Arena.riverX(pz))+bank*7,to=from-bank*5;
                if(x>=Math.min(from,to)&&x<=Math.max(from,to)&&Math.abs(z-pz)<=1)return true;
            }
            return false;
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
}
