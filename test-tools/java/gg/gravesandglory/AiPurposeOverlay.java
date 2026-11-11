package gg.gravesandglory;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import java.util.*;

/** Unshipped (test tools, never in a player jar; its class name is on the release forbidden list, tools/release_config.py): the dev "purpose" overlay.
 *  /gg ai purpose on|off|status (ops, permission level 2; the console) puts one line above every troop of the running round:
 *      <group> <order> | <state> | <target or spot>
 *  where state is marching, fighting, holding, idle, stuck, retreating or refill-walking (AiState). Server side only: vanilla entities, no packet or client code.
 *
 *  Troops already use their custom name as their identity: the client resolves a troop's model, rank and side from the name's text and its colour (TroopModels.resolve, CommandMode.nameTeam,
 *  ArmyMobs.named), and Battle.troopName keeps it hidden. Writing the line into that name would turn every troop into a plain mob on a client, so the troops' names and name visibility are never
 *  touched. The line is the visible custom name of its own invisible marker armour stand (tag gg_ai_overlay) placed just above the troop and moved on every refresh. Turning the overlay off (or
 *  the round ending, or the server stopping) discards every stand, which leaves each troop's name and visibility exactly as the game set them.
 *  Refresh: every 10 ticks while the overlay is on; while it is off the tick listener returns at once and reads nothing. */
public final class AiPurposeOverlay implements ModInitializer {
    static final String TAG="gg_ai_overlay";
    static volatile boolean on;
    static final Map<GravesAndGlory.Unit,ArmorStandEntity> labels=new IdentityHashMap<>();
    /** How many refreshes ran (the check proves none run while off) and the states seen since the overlay was last turned on. */
    static long refreshes;static final Set<String> statesSeen=new TreeSet<>();
    static final AiState state=new AiState();
    static final Set<UUID> mine=new HashSet<>();

    @Override public void onInitialize(){
        CommandRegistrationCallback.EVENT.register((d,access,env)->d.register(CommandManager.literal("gg").requires(s->s.hasPermissionLevel(2))
            .then(CommandManager.literal("ai").then(CommandManager.literal("purpose").requires(s->s.hasPermissionLevel(2))
                .executes(c->{c.getSource().sendFeedback(()->Text.literal("AI purpose overlay is "+(on?"on":"off")+". /gg ai purpose on|off"),false);return 1;})
                .then(CommandManager.argument("mode",StringArgumentType.word()).suggests((c,b)->{b.suggest("on");b.suggest("off");return b.buildFuture();}).executes(c->{
                    String v=StringArgumentType.getString(c,"mode");
                    if(!v.equals("on")&&!v.equals("off")){c.getSource().sendError(Text.literal("Use /gg ai purpose on|off"));return 0;}
                    set(v.equals("on"));c.getSource().sendFeedback(()->Text.literal("AI purpose overlay "+(on?"on":"off")+"."),true);return 1;}))))));
        ServerTickEvents.END_SERVER_TICK.register(s->{if(!on)return;var b=GravesAndGlory.battle;if(b==null||!b.running){clear();return;}if(b.time%10==0)refresh(b);});
        ServerLifecycleEvents.SERVER_STOPPING.register(s->{on=false;clear();});
        // A stand saved with a chunk (a crash with the overlay on) is nobody's label after a restart: discard it.
        ServerEntityEvents.ENTITY_LOAD.register((e,w)->{if(e.getCommandTags().contains(TAG)&&!mine.contains(e.getUuid()))e.discard();});
    }

    /** Turns the overlay on (refreshing at once) or off (every label discarded). */
    static void set(boolean value){
        if(value==on)return;on=value;
        if(on){state.reset();statesSeen.clear();var b=GravesAndGlory.battle;if(b!=null&&b.running)refresh(b);}else{clear();state.reset();}
    }
    static void clear(){
        for(var stand:labels.values()){mine.remove(stand.getUuid());stand.discard();}
        labels.clear();
    }
    static void refresh(GravesAndGlory.Battle b){
        refreshes++;
        labels.entrySet().removeIf(e->{var u=e.getKey();if(u.mob.isAlive()&&!u.temporary&&b.units.contains(u))return false;mine.remove(e.getValue().getUuid());e.getValue().discard();return true;});
        var shown=new ArrayList<GravesAndGlory.Unit>();
        for(var u:new ArrayList<>(b.units)){
            if(!u.mob.isAlive()||u.temporary)continue;
            shown.add(u);
            String line=line(b,u);
            var stand=labels.get(u);
            if(stand!=null&&stand.isRemoved())mine.remove(stand.getUuid());
            if(stand==null||stand.isRemoved()){stand=spawn(b,u);labels.put(u,stand);}
            stand.setCustomName(Text.literal(line));
            stand.refreshPositionAndAngles(u.mob.getX(),u.mob.getY()+u.mob.getHeight()-.1,u.mob.getZ(),0,0);
        }
        state.retain(shown); // memory of troops that died or left is dropped
    }
    /** "<group> <order> | <state> | <target or spot>": the target is the foe it fights (its name and distance), else the spot it is ordered to (a point's name or x,z). */
    static String line(GravesAndGlory.Battle b,GravesAndGlory.Unit u){
        var r=state.read(b,u,b.time);statesSeen.add(r.state);
        String where;
        if(r.fighting&&r.foe!=null)where=String.format(Locale.ROOT,"%s %.0fm",r.foe.getName().getString().replaceAll("\\s*\\(.*$",""),Math.sqrt(u.mob.squaredDistanceTo(r.foe)));
        else where="spot "+r.spotName;
        return r.group+" "+r.order+" | "+r.state+" | "+where;
    }
    static ArmorStandEntity spawn(GravesAndGlory.Battle b,GravesAndGlory.Unit u){
        var stand=new ArmorStandEntity(EntityType.ARMOR_STAND,b.world);
        NbtCompound nbt=new NbtCompound();stand.writeCustomDataToNbt(nbt);nbt.putBoolean("Marker",true);nbt.putBoolean("Invisible",true);nbt.putBoolean("NoBasePlate",true);stand.readCustomDataFromNbt(nbt);
        stand.setNoGravity(true);stand.setInvulnerable(true);stand.setSilent(true);stand.setCustomNameVisible(true);stand.addCommandTag(TAG);
        stand.refreshPositionAndAngles(u.mob.getX(),u.mob.getY()+u.mob.getHeight()-.1,u.mob.getZ(),0,0);
        mine.add(stand.getUuid());b.world.spawnEntity(stand);return stand;
    }
}
