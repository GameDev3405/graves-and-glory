package gg.gravesandglory;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The prop menu (prop kit plan task S6). {@code /prop} (op 2) opens a 6-row vanilla chest menu: row 1 category tabs, rows 2-6 the props of
 * the open category (paper with the first part's CMD, name, size in lore). Clicking a prop closes the menu and starts placing: a glowing
 * {@code item_display} ghost per part (tag {@link #GHOST_TAG}; white, or red {@code 0xFF4040} when {@link Props#check} refuses) sits on the
 * targeted block's top face and follows it every tick. Each hotbar scroll turns the ghost 90 degrees and the server puts the slot back.
 * Right-click places; sneak + right-click or {@code /prop} cancels. Hitting a prop's barrier, light or anchor removes the whole prop for an
 * op 2 player; anyone else gets no effect. Ghosts are entities only (never blocks), are discarded on leave, world change, death and server
 * stop, and a ghost loaded from disk that no placement owns is discarded at once.
 */
public final class PropMenu {
    private PropMenu(){}
    public static final String GHOST_TAG="gg_prop_ghost";
    static final int WHITE=0xFFFFFF,RED=0xFF4040,ROWS=6,TAB_SLOTS=9;

    /** A player's placement in progress. */
    static final class Placing{
        final String id;int facing;final int homeSlot;BlockPos at;boolean red;String why="";
        /** A Siege defender's placement (PropGameplay): no op needed, the Siege rules checked and the box saved for the round's teardown. */
        boolean siege;
        final List<Entity> ghosts=new ArrayList<>();
        Placing(String id,int homeSlot){this.id=id;this.homeSlot=homeSlot;}
    }
    private static final Map<UUID,Placing> PLACING=new HashMap<>();
    private static final Set<UUID> GHOSTS=new HashSet<>();
    /** Placements to start on the next server tick (a menu click must not close the screen inside its own click handling). */
    private static final Map<UUID,String> START=new HashMap<>();
    /** The server tick on which a player's use was taken by placing (place, refuse or cancel): the vanilla client follows a block use that
     *  did nothing on its side with an item use in the same tick, which must not open the held book or place the held block. */
    private static final Map<UUID,Integer> USED=new HashMap<>();

    public static void init(){
        CommandRegistrationCallback.EVENT.register((d,access,env)->d.register(CommandManager.literal("prop").requires(s->s.hasPermissionLevel(2)).executes(c->{
            ServerPlayerEntity p=c.getSource().getPlayerOrThrow();
            if(PLACING.containsKey(p.getUuid())){cancel(p,"Placement cancelled");return 1;}
            open(p,firstCategory());
            return 1;
        })));
        ServerTickEvents.END_SERVER_TICK.register(PropMenu::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((h,server)->{end(h.player.getUuid());USED.remove(h.player.getUuid());START.remove(h.player.getUuid());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(UUID u:new ArrayList<>(PLACING.keySet()))end(u);START.clear();});
        ServerEntityEvents.ENTITY_LOAD.register((e,w)->{
            if(e.getCommandTags().contains(GHOST_TAG)&&!GHOSTS.contains(e.getUuid()))e.discard(); // a ghost left on disk by a crash
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(world.isClient||!(player instanceof ServerPlayerEntity p))return ActionResult.PASS;
            Placing pl=PLACING.get(p.getUuid());
            if(pl==null)return ActionResult.PASS;
            USED.put(p.getUuid(),p.getServer().getTicks());
            if(hand!=Hand.MAIN_HAND)return ActionResult.FAIL;
            if(p.isSneaking()){cancel(p,"Placement cancelled");return ActionResult.FAIL;}
            BlockPos at=hit.getBlockPos().up();
            var battle=GravesAndGlory.battle;
            String why=pl.siege?(battle==null?"No battle is running.":PropGameplay.siegePlace(battle,battle.squads.get(p.getUuid()),pl.id,at,pl.facing)):Props.place(p.getServerWorld(),pl.id,at,pl.facing);
            if(!why.isEmpty()){p.sendMessage(Text.literal(why).formatted(Formatting.RED),false);return ActionResult.FAIL;}
            PropDef d=Props.DEFS.get(pl.id);
            p.sendMessage(Text.literal("Placed "+(d==null?pl.id:d.name())+" at "+at.toShortString()+", facing "+pl.facing),false);
            end(p.getUuid());
            return ActionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player,world,hand)->{
            ItemStack held=player.getStackInHand(hand);
            if(world.isClient||!(player instanceof ServerPlayerEntity p))return TypedActionResult.pass(held);
            Integer used=USED.get(p.getUuid());
            if(used!=null&&p.getServer().getTicks()-used<=1)return TypedActionResult.fail(held);
            if(!PLACING.containsKey(p.getUuid()))return TypedActionResult.pass(held);
            USED.put(p.getUuid(),p.getServer().getTicks());
            if(hand==Hand.MAIN_HAND&&p.isSneaking())cancel(p,"Placement cancelled");
            return TypedActionResult.fail(held);
        });
        AttackBlockCallback.EVENT.register((player,world,hand,pos,dir)->{
            if(world.isClient||!(world instanceof ServerWorld w)||!propBlock(w.getBlockState(pos)))return ActionResult.PASS;
            if(Props.owner(w,pos)==null)return ActionResult.PASS;
            if(!player.hasPermissionLevel(2))return ActionResult.FAIL;
            removeAt(w,player,pos);
            return ActionResult.SUCCESS;
        });
        PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,be)->{
            if(!(world instanceof ServerWorld w)||!propBlock(state)||Props.owner(w,pos)==null)return true;
            if(player.hasPermissionLevel(2))removeAt(w,player,pos);
            return false;
        });
    }

    static boolean propBlock(BlockState s){return s.isOf(Blocks.BARRIER)||s.isOf(Blocks.LIGHT)||s.isOf(Blocks.COMMAND_BLOCK);}

    static void removeAt(ServerWorld w,PlayerEntity player,BlockPos pos){
        BlockPos anchor=Props.owner(w,pos);
        Props.Live l=anchor==null?null:Props.readAnchor(w,anchor);
        if(anchor!=null&&Props.remove(w,pos))player.sendMessage(Text.literal("Removed "+(l==null?"the prop":l.def().name())+" at "+anchor.toShortString()),true);
    }

    // ---- menu --------------------------------------------------------------------------------------------------------------------------

    static List<String> categories(){
        Set<String> out=new TreeSet<>();
        for(PropDef d:Props.DEFS.values())out.add(d.category());
        return new ArrayList<>(out);
    }
    static String firstCategory(){List<String> c=categories();return c.isEmpty()?"":c.get(0);}
    /** The props of a category in contract-table order (by the first part's CMD). */
    static List<PropDef> props(String category){
        List<PropDef> out=new ArrayList<>();
        for(PropDef d:Props.DEFS.values())if(d.category().equals(category))out.add(d);
        out.sort(Comparator.comparingInt(d->d.parts().get(0).cmd()));
        return out;
    }
    static String title(String category){return category.isEmpty()?"":Character.toUpperCase(category.charAt(0))+category.substring(1).replace('_',' ');}

    static void open(ServerPlayerEntity p,String category){
        p.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId,inv,player)->new Menu(syncId,inv,category),Text.literal("Props")));
    }

    static ItemStack tab(String category,boolean open){
        ItemStack s=new ItemStack(open?Items.SMITHING_TABLE:Items.CRAFTING_TABLE);
        s.setCustomName(Text.literal(title(category)).styled(st->st.withItalic(false).withBold(open)));
        if(open){NbtList glint=new NbtList();glint.add(new NbtCompound());s.getOrCreateNbt().put("Enchantments",glint);}
        return s;
    }
    static ItemStack icon(PropDef d){
        ItemStack s=new ItemStack(Items.PAPER);
        s.getOrCreateNbt().putInt("CustomModelData",d.parts().get(0).cmd());
        s.setCustomName(Text.literal(d.name()).styled(st->st.withItalic(false)));
        NbtList lore=new NbtList();
        lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("Size "+d.sx()+" x "+d.sy()+" x "+d.sz()).formatted(Formatting.GRAY).styled(st->st.withItalic(false)))));
        lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("Click to place").formatted(Formatting.DARK_GRAY))));
        s.getOrCreateSubNbt("display").put("Lore",lore);
        return s;
    }

    /** A read-only 6-row chest: every click is cancelled; a tab click switches category, a prop click starts placing. */
    static final class Menu extends GenericContainerScreenHandler{
        final String category;
        final List<String> tabs;
        final List<PropDef> items;
        Menu(int syncId,PlayerInventory inv,String category){
            super(ScreenHandlerType.GENERIC_9X6,syncId,inv,new SimpleInventory(9*ROWS),ROWS);
            this.category=category;this.tabs=categories();this.items=props(category);
            for(int i=0;i<tabs.size()&&i<TAB_SLOTS;i++)getInventory().setStack(i,tab(tabs.get(i),tabs.get(i).equals(category)));
            for(int i=0;i<items.size()&&i<9*(ROWS-1);i++)getInventory().setStack(TAB_SLOTS+i,icon(items.get(i)));
        }
        @Override public void onSlotClick(int slot,int button,SlotActionType action,PlayerEntity player){
            if(!(player instanceof ServerPlayerEntity p)||action!=SlotActionType.PICKUP)return;
            if(slot>=0&&slot<TAB_SLOTS&&slot<tabs.size()){
                String c=tabs.get(slot);
                if(!c.equals(category))START.put(p.getUuid(),"tab:"+c);
            }else if(slot>=TAB_SLOTS&&slot-TAB_SLOTS<items.size())START.put(p.getUuid(),items.get(slot-TAB_SLOTS).id());
        }
        @Override public ItemStack quickMove(PlayerEntity player,int slot){return ItemStack.EMPTY;}
        @Override public boolean canUse(PlayerEntity player){return true;}
    }

    // ---- placing -----------------------------------------------------------------------------------------------------------------------

    static void start(ServerPlayerEntity p,String id){
        end(p.getUuid());
        PropDef d=Props.DEFS.get(id);
        if(d==null)return;
        PLACING.put(p.getUuid(),new Placing(id,p.getInventory().selectedSlot));
        p.sendMessage(Text.literal("Placing "+d.name()+": scroll to turn, ").append(Text.keybind("key.use")).append(" to place, ")
            .append(Text.keybind("key.sneak")).append(" + ").append(Text.keybind("key.use")).append(" or /prop to cancel"),false);
    }

    /** Starts a Siege defender's placement of prop id (PropGameplay's /barricade): the same ghost, checked and placed under the Siege rules. */
    static void startSiege(ServerPlayerEntity p,String id){
        start(p,id);
        Placing pl=PLACING.get(p.getUuid());
        if(pl!=null)pl.siege=true;
    }

    static void cancel(ServerPlayerEntity p,String text){
        end(p.getUuid());
        p.sendMessage(Text.literal(text),true);
    }

    /** Ends a placement and discards its ghost. */
    static void end(UUID player){
        Placing pl=PLACING.remove(player);
        if(pl!=null)clearGhost(pl);
    }
    static void clearGhost(Placing pl){
        for(Entity e:pl.ghosts){GHOSTS.remove(e.getUuid());if(!e.isRemoved())e.discard();}
        pl.ghosts.clear();
    }

    static void tick(MinecraftServer server){
        if(!START.isEmpty()){
            for(Map.Entry<UUID,String> s:new ArrayList<>(START.entrySet())){
                ServerPlayerEntity p=server.getPlayerManager().getPlayer(s.getKey());
                if(p==null)continue;
                if(s.getValue().startsWith("tab:"))open(p,s.getValue().substring(4));
                else{p.closeHandledScreen();start(p,s.getValue());}
            }
            START.clear();
        }
        for(Iterator<Map.Entry<UUID,Placing>> it=PLACING.entrySet().iterator();it.hasNext();){
            Map.Entry<UUID,Placing> e=it.next();
            ServerPlayerEntity p=server.getPlayerManager().getPlayer(e.getKey());
            Placing pl=e.getValue();
            PropDef d=Props.DEFS.get(pl.id);
            if(p==null||p.isRemoved()||!p.isAlive()||d==null||!pl.siege&&!p.hasPermissionLevel(2)){clearGhost(pl);it.remove();continue;}
            if(!pl.ghosts.isEmpty()&&pl.ghosts.get(0).getWorld()!=p.getWorld())clearGhost(pl);
            turn(p,pl);
            HitResult hit=p.raycast(p.isCreative()?5.0:4.5,1f,false);
            if(hit.getType()==HitResult.Type.BLOCK)pl.at=((BlockHitResult)hit).getBlockPos().up().toImmutable();
            if(pl.at==null)continue;
            var battle=GravesAndGlory.battle;
            String why=!pl.siege?Props.check(p.getServerWorld(),pl.id,pl.at,pl.facing):battle==null?"No battle is running.":PropGameplay.siegeCheck(battle,battle.squads.get(p.getUuid()),pl.id,pl.at,pl.facing);
            boolean red=!why.isEmpty();
            if(red!=pl.red||pl.ghosts.isEmpty()){clearGhost(pl);pl.red=red;spawnGhost(p.getServerWorld(),pl,d);}
            else moveGhost(pl,d);
            if(!why.equals(pl.why)||p.age%20==0){
                pl.why=why;
                p.sendMessage(red?Text.literal(why).formatted(Formatting.RED):Text.literal(d.name()+", facing "+pl.facing+": ").append(Text.keybind("key.use")).append(" to place"),true);
            }
        }
    }

    /** Each change of the selected hotbar slot turns the ghost 90 degrees (forward clockwise, back anticlockwise); the slot goes back. */
    static void turn(ServerPlayerEntity p,Placing pl){
        int cur=p.getInventory().selectedSlot;
        if(cur==pl.homeSlot)return;
        int step=Math.floorMod(cur-pl.homeSlot,9)<=4?90:-90;
        pl.facing=PropGeometry.turnDeg(pl.facing,step);
        p.getInventory().selectedSlot=pl.homeSlot;
        p.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(pl.homeSlot));
    }

    static void spawnGhost(ServerWorld w,Placing pl,PropDef d){
        double[] t=PropGeometry.partTransform(d,pl.facing);
        for(PropDef.Part part:d.parts()){
            NbtCompound n=new NbtCompound();
            n.putString("id","minecraft:item_display");
            NbtCompound item=new NbtCompound();
            item.putString("id","minecraft:paper");item.putByte("Count",(byte)1);
            NbtCompound tag=new NbtCompound();tag.putInt("CustomModelData",part.cmd());item.put("tag",tag);
            n.put("item",item);
            n.putString("item_display","fixed");
            n.putFloat("view_range",Props.VIEW_RANGE);
            n.putBoolean("Glowing",true);
            n.putInt("glow_color_override",pl.red?RED:WHITE);
            NbtCompound b=new NbtCompound();b.putInt("block",15);b.putInt("sky",15);n.put("brightness",b);
            NbtList tags=new NbtList();tags.add(NbtString.of(GHOST_TAG));n.put("Tags",tags);
            EntityType.getEntityFromNbt(n,w).ifPresent(e->{
                e.refreshPositionAndAngles(pl.at.getX()+t[0],pl.at.getY()+t[1],pl.at.getZ()+t[2],(float)t[3],0);
                GHOSTS.add(e.getUuid());
                if(w.spawnEntity(e))pl.ghosts.add(e);else GHOSTS.remove(e.getUuid());
            });
        }
    }

    static void moveGhost(Placing pl,PropDef d){
        double[] t=PropGeometry.partTransform(d,pl.facing);
        double x=pl.at.getX()+t[0],y=pl.at.getY()+t[1],z=pl.at.getZ()+t[2];
        for(Entity e:pl.ghosts)if(e.getX()!=x||e.getY()!=y||e.getZ()!=z||e.getYaw()!=(float)t[3])e.refreshPositionAndAngles(x,y,z,(float)t[3],0);
    }

    /** Whether the player is placing (for checks and tests). */
    static boolean placing(ServerPlayerEntity p){return PLACING.containsKey(p.getUuid());}
}
