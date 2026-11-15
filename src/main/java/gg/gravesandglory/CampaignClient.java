package gg.gravesandglory;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.sound.*;
import net.minecraft.text.Text;
import java.nio.file.*;

/** Portable local collection and a temporary, skippable camera introduction. */
public final class CampaignClient {
    static Profile profile=new Profile();
    /** The commander's gear from the last gear_v1 packet, or null before the first (GearWire.decode). */
    static GearWire.View gear=null;
    /** The side row ids the two teams fly (from the snapshot "sides"): team 0 first. */
    static String[] sides={"gold","violet"};
    /** The banner this commander fields (snapshot "banner"): the battle banner while seated in a running battle, else the profile banner. */
    static java.util.List<String> banner=RosterMath.starterBanner();
    static JsonArray players=new JsonArray();
    static JsonObject social=new JsonObject(),progression=new JsonObject();
    static JsonObject result=new JsonObject();static String shownResult="";
    /** The campaign battle being fought (id, name, briefing, reason), or an empty object. */
    static JsonObject battle=new JsonObject();
    /** The server's season and its start dates (brief G2), from the snapshot. */
    static int season=1;static java.util.List<java.time.LocalDate> seasonStarts=new java.util.ArrayList<>();
    /** Why the campaign battle cannot start yet because its chapter's season has not begun, or "". */
    static String seasonLock(Spec.CampaignBattlesRow row){return MarchMath.Seasons.lock(row,Spec.CAMPAIGNCHAPTERS,season,seasonStarts);}
    static String text(JsonObject j,String key){return j.has(key)?j.get(key).getAsString():"";}
    /** The Long March number (sheet order) of the battle being fought; 1 when none is known. */
    static int battleOrder(){for(var row:Spec.CAMPAIGNBATTLES)if(row.id().equals(text(battle,"id")))return row.order();return 1;}
    static boolean fill=true;static String mode="2",notice="",dismissed="",headline="";
    /** The server's count of notices sent to this player, as last sent in the snapshot: it rises with each notice, even one that repeats the last text. */
    static int noticeSerial;
    /** The server-wide bot difficulty (a Spec.BOTCOMMAND id), as last sent in the snapshot. */
    static String difficulty="normal";
    static String botsLabel(){return "Bots: "+tierName(difficulty);}
    /** A bot tier's display name: its Spec.BOTCOMMAND id with a capital ("normal" reads "Normal"). */
    static String tierName(String id){return id.isEmpty()?id:id.substring(0,1).toUpperCase(java.util.Locale.ROOT)+id.substring(1);}
    /** The order sheet the War Table home raises, "practice", "skirmish" or "campaign"; kept for the session (UI plan Task 5). */
    static String homeMode="skirmish";
    /** The tier a press of the Bots button asks for: the next row of the sheet, wrapping round. */
    static String nextDifficulty(){for(int i=0;i<Spec.BOTCOMMAND.length;i++)if(Spec.BOTCOMMAND[i].id().equals(difficulty))return Spec.BOTCOMMAND[(i+1)%Spec.BOTCOMMAND.length].id();return Spec.BOTCOMMAND[0].id();}
    static int tutorial=-1;
    static String lastSaved="",revealUnit="";static long revealUntil;static boolean received;
    /** The last roll or chosen buy, from the snapshot's lastRoll (Task 12): the troop id, whether it was a duplicate, its stars before and after and the shards it gave; n tells a new one from the same. */
    record LastRoll(String troop,boolean duplicate,int starsBefore,int starsAfter,int shards,int n){}
    /** The newest LastRoll, or null before the first; rollAt is when this client received it (the reveal's clock). */
    static LastRoll lastRoll;static long rollAt;
    /** How long the roll card's pips take to fill from starsBefore to starsAfter, and how long the card stays. */
    static final long ROLL_FILL_MS=1200,REVEAL_MS=2400;
    /** Harness only: when 0 or more, the reveal's elapsed milliseconds are held there (a mid-animation screenshot). */
    static long revealHeldAt=-1;
    /** The roll card's elapsed milliseconds since the roll arrived. */
    static long rollElapsed(){return revealHeldAt>=0?revealHeldAt:System.currentTimeMillis()-rollAt;}
    /** Whether the reveal on screen is the last roll's card: it arrived with this reveal and Skip reveal has not ended it. */
    static boolean rollShowing(){return lastRoll!=null&&revealUntil>System.currentTimeMillis()&&revealUntil==rollAt+REVEAL_MS;}
    /** Forgets the last roll and the first-snapshot mark when the client leaves a server: a restarted server counts n from 1 again, and its first snapshot must not reveal (review minor 5). */
    /** The last Forge roll, from the snapshot's forge (gear screen Task 4): the item and rarity, or the relic, whether it was a duplicate and the marks refunded; n tells a new one from the same. Null before the first. */
    record ForgeRoll(int n,String item,String rarity,String relic,boolean duplicate,int refund){}
    static ForgeRoll forgeRoll;
    /** Reads the snapshot's forge block ({} until this commander rolls). */
    static void acceptForge(JsonObject j){
        if(!j.has("forge")||!j.get("forge").isJsonObject())return;var f=j.getAsJsonObject("forge");if(!f.has("n"))return;
        try{forgeRoll=new ForgeRoll(f.get("n").getAsInt(),f.get("item").getAsString(),f.get("rarity").getAsString(),f.get("relic").getAsString(),f.get("duplicate").getAsBoolean(),f.get("refund").getAsInt());}catch(RuntimeException ignored){}
    }
    static void forget(){forgeRoll=null;lastRoll=null;rollAt=0;received=false;revealUntil=0;armoury=LevelMath.Armoury.EMPTY;lastRunning=false;profileTooLarge=false;WarTablePages.forgetPicks();}
    /** This army's Armoury from the last state_v9 (armouryHere and the buy states per banner troop type, LevelMath.decodeArmoury); empty before the first state, after a disconnect and until the server fills it (Task 5). The Armoury tab and the Iron Ledger glint read it. */
    static LevelMath.Armoury armoury=LevelMath.Armoury.EMPTY;
    /** Reads the snapshot's lastRoll; a new one (another n) after the first snapshot starts the reveal. */
    static void acceptRoll(JsonObject j,boolean first){
        if(!j.has("lastRoll"))return;var r=j.getAsJsonObject("lastRoll");if(!r.has("n")||!r.has("troop"))return;
        var next=new LastRoll(r.get("troop").getAsString(),r.get("duplicate").getAsBoolean(),r.get("starsBefore").getAsInt(),r.get("starsAfter").getAsInt(),r.get("shards").getAsInt(),r.get("n").getAsInt());
        if(lastRoll!=null&&lastRoll.n()==next.n())return;lastRoll=next;if(first)return;
        var t=ArmyCards.spec(next.troop());if(t!=null)revealUnit=t.name();rollAt=System.currentTimeMillis();revealUntil=rollAt+REVEAL_MS;
    }
    static int map=0,serial=-1,intro=0;static boolean lastRunning,waiting;
    public static double cameraX,cameraY,cameraZ;public static float cameraYaw,cameraPitch;
    public static boolean cinematic(){return camera!=null;}
    /** The notice the player left behind by changing page: the server keeps sending it with every state packet, so the client hides that text until the server says something else (no protocol change; review I1). acceptedStates counts the state packets read (the harness waits for two). */
    static int acceptedStates;
    /** Counts the snapshots accepted: screens cache what they build from the snapshot and rebuild it when this moves. */
    static int revision;
    static String shownNotice(){return notice.equals(dismissed)?"":notice;}
    /** Takes the notice of a state packet: a different text ends the dismissal of the old one (the same words stay dismissed until the player acts or another text comes). */
    static void acceptNotice(String next){if(!next.equals(notice))dismissed="";notice=next;}
    /** A report this client has not shown opens: the lines the player dismissed before belong to an earlier match. */
    static void reportOpened(String id){shownResult=id;dismissed="";}
    static boolean previousHud;
    static ArmorStandEntity camera;static Perspective previous;static int cameraTicks;
    static Path collection(MinecraftClient c){return c.runDirectory.toPath().resolve((Diagnostics.active()?"gravesandglory/test-collections/":"gravesandglory/collections/")+c.getSession().getUuid()+".json");}
    /** The most UTF-8 bytes of collection JSON one profile_v1 packet carries: a client-to-server custom payload is capped at 32767 bytes, and the string's length prefix takes up to 3 of them. The server reads the string with the same cap (GravesAndGlory, Campaign.importProfile). */
    static final int PROFILE_MAX_BYTES=32000;
    /** Set once the player has been told the collection is too big to send, so the notice shows once per join, not with every snapshot. */
    static boolean profileTooLarge;
    /** The collection file as compact JSON: the file is pretty-printed for people, and the server's Gson reads either form, so the packet drops the whitespace (typically a third of a large file). */
    static String compactProfile(String json){return JsonParser.parseString(json).toString();}
    /** Sends the collection for the server's first-visit import; a collection too big for one packet is not sent and the player is told once. */
    static void sendProfile(MinecraftClient c){
        try{Path path=collection(c);String json=compactProfile(Files.exists(path)?Files.readString(path):Campaign.JSON.toJson(new Profile()));
            if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>PROFILE_MAX_BYTES){
                if(!profileTooLarge){profileTooLarge=true;if(c.player!=null)c.player.sendMessage(Text.literal("Your collection file is too large to sync with this world; it stays on disk unchanged."),true);System.err.println("[Graves and Glory] Collection is too large for one packet; not sent.");}
                return;}
            if(ClientPlayNetworking.canSend(GravesAndGlory.PROFILE)){var packet=PacketByteBufs.create();packet.writeString(json,PROFILE_MAX_BYTES);ClientPlayNetworking.send(GravesAndGlory.PROFILE,packet);}
        }catch(Exception ignored){}
    }
    /** The snapshot's battleMode block (Campaign: the round's mode while one runs): the HUD reads its track maxima and number formats. */
    static JsonObject battleMode=new JsonObject();
    /** Whether the running round is in overtime (battleMode.overtime: its clock is out and the mode holds it open); false from a server that does not send it. MusicDirector swaps the battle music for overtime_music while it holds. */
    static boolean overtime(){try{return battleMode.has("overtime")&&battleMode.get("overtime").getAsBoolean();}catch(Exception e){return false;}}
    /** The battle mode's id from the snapshot's battleMode ("survival", "convoy" ...), or "" before the first snapshot. */
    static String modeId(){return text(battleMode,"id");}
    /** A ledger track's maximum and format from battleMode; Domination's target and plain numbers before the first snapshot. */
    static int ledgerMax(int i){try{return battleMode.getAsJsonArray("max").get(i).getAsInt();}catch(Exception e){return Modes.row(Modes.DOMINATION).winTarget();}}
    /** Whether the round's mode plays the map's points (Siege takes its flags down): the HUD hangs no seals when it does not. */
    static boolean showsPoints(){try{return !battleMode.has("points")||battleMode.get("points").getAsBoolean();}catch(Exception e){return true;}}
    static String ledgerFormat(int i){try{return battleMode.getAsJsonArray("format").get(i).getAsString();}catch(Exception e){return "plain";}}
    static void accept(MinecraftClient c,String json,boolean running){
        revision++;
        try{JsonObject j=JsonParser.parseString(json).getAsJsonObject();if(!j.has("profile"))return; /* "{}": the server could not fit this snapshot; keep the last camp state */var nextProfile=Campaign.JSON.fromJson(j.get("profile"),Profile.class);if(received&&nextProfile.owned!=null&&nextProfile.owned.size()>profile.owned.size()){for(var unit:Spec.TROOPS)if(nextProfile.owned.containsKey(unit.id())&&!profile.owned.containsKey(unit.id()))revealUnit=unit.name();revealUntil=System.currentTimeMillis()+2400;}nextProfile.sanitize();profile=nextProfile;acceptRoll(j,!received);acceptForge(j);received=true;
            if(j.has("sides")){var s=j.getAsJsonArray("sides");if(s.size()==2)sides=new String[]{s.get(0).getAsString(),s.get(1).getAsString()};}
            if(j.has("banner")){var list=new java.util.ArrayList<String>();for(var e:j.getAsJsonArray("banner"))list.add(e.getAsString());banner=list;}social=j.has("social")?j.getAsJsonObject("social"):new JsonObject();battleMode=j.has("battleMode")?j.getAsJsonObject("battleMode"):new JsonObject();progression=j.has("progression")?j.getAsJsonObject("progression"):new JsonObject();if(j.has("progression"))profile.progression=Campaign.JSON.fromJson(progression,Progression.State.class);players=j.getAsJsonArray("players");
            tutorial=j.has("tutorial")?j.get("tutorial").getAsInt():-1;battle=j.has("battle")?j.getAsJsonObject("battle"):new JsonObject();season=j.has("season")?j.get("season").getAsInt():1;seasonStarts=new java.util.ArrayList<>();if(j.has("seasonStarts"))for(var d:j.getAsJsonArray("seasonStarts"))try{seasonStarts.add(java.time.LocalDate.parse(d.getAsString()));}catch(Exception bad){seasonStarts.add(null);}fill=j.get("fill").getAsBoolean();difficulty=j.has("difficulty")?j.get("difficulty").getAsString():"normal";mode=j.get("mode").getAsString();acceptNotice(j.get("notice").getAsString());acceptedStates++;noticeSerial=j.has("noticeSerial")?j.get("noticeSerial").getAsInt():0;headline=j.has("headline")&&!j.get("headline").isJsonNull()?j.get("headline").getAsString():"";
            int next=j.get("map").getAsInt();if(next!=map||Spec.FACTIONS[0].spawnX()!=CampaignMaps.spawnX(next,0)){map=next;if(c.getServer()==null)CampaignMaps.select(map);GravesAndGloryClient.invalidateMap(c);}
            waiting=j.has("waiting")&&j.get("waiting").getAsBoolean();
            int round=j.get("serial").getAsInt();intro=j.get("intro").getAsInt();
            if(round!=serial&&running&&intro>0){cameraTicks=120-intro;begin(c);}serial=round;
            if(!running)end(c);
            result=j.has("result")?j.getAsJsonObject("result"):new JsonObject();
            // The match ending (EndingClient): no report opens while it runs for this player; when it ends he lands on the war council page in a room, else the War Table home, and the report counts as shown (reopenable from Last battle).
            String rid=result.has("id")?result.get("id").getAsString():"";var change=EndingClient.accept(c,j.has("ending")&&j.get("ending").isJsonObject()?j.getAsJsonObject("ending"):null,rid,running);if(EndingClient.doneEarly)waiting=true;
            if(change==EndingClient.Change.DONE){reportOpened(rid);c.setScreen(new CampScreen(text(social,"room").isEmpty()?"home":"lobby"));}
            else if(change==EndingClient.Change.RUNNING){}
            else if(!running&&result.has("id")&&!result.get("id").getAsString().equals(shownResult)){reportOpened(result.get("id").getAsString());c.setScreen(new CampScreen("results"));}
            else if(!running&&lastRunning&&c.currentScreen==null)c.setScreen(new CampScreen("home"));lastRunning=running;
            if(!j.get("collectionReady").getAsBoolean()){sendProfile(c);return;}if(c.getServer()==null)return; /* the local collection is written only from an integrated (single-player or LAN-host) world, never from a dedicated server or a LAN guest */String saved=Campaign.JSON.toJson(profile);if(saved.equals(lastSaved))return;lastSaved=saved;Path path=collection(c);Files.createDirectories(path.getParent());Path tmp=path.resolveSibling(path.getFileName()+".tmp");Files.writeString(tmp,saved);Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);
        }catch(Exception e){System.err.println("[Graves and Glory] Collection sync could not be saved: "+e.getClass().getSimpleName());}
    }
    static void begin(MinecraftClient c){
        if(c.player==null||c.world==null||camera!=null)return;
        frameMs.clear();lastFrame=0;previous=c.options.getPerspective();previousHud=c.options.hudHidden;c.options.hudHidden=false;c.options.setPerspective(Perspective.THIRD_PERSON_BACK);camera=new ArmorStandEntity(c.world,c.player.getX(),c.player.getY()+2,c.player.getZ());camera.setInvisible(true);c.setScreen(null);
        sound(c,"battle_intro",AudioCues.Cue.BATTLE_INTRO.volume);
        sound(c,AudioCues.Cue.BATTLE_DRUMS.id,AudioCues.Cue.BATTLE_DRUMS.volume); // war drums layered under the horns
    }
    static void sound(MinecraftClient c,String id,float volume){if(c.player!=null)c.player.playSound(net.minecraft.registry.Registries.SOUND_EVENT.get(new net.minecraft.util.Identifier("gravesandglory",id)),SoundCategory.MASTER,volume,1);}
    static void tick(MinecraftClient c){
        if(camera==null)return;
        if(c.player==null||!GravesAndGloryClient.running||cameraTicks>=120||c.currentScreen!=null){end(c);return;}
        cameraTicks++;c.getToastManager().clear();
    }
    /** Render-frame sampling avoids stepping the camera at the 20 Hz game tick. */
    public static void sampleCamera(float partial){
        var c=MinecraftClient.getInstance();if(camera==null||c.player==null)return;
        double t=Math.max(0,Math.min(1,(cameraTicks-1+partial)/120.0)),e=t*t*(3-2*t),side=GravesAndGloryClient.team==0?1:-1;
        double angle=e*Math.PI,dist=8+Math.sin(t*Math.PI)*13;
        double x=c.player.getX()+side*Math.cos(angle)*dist,z=c.player.getZ()-side*Math.sin(angle)*dist;
        int ix=(int)Math.floor(x),iz=(int)Math.floor(z);double fx=x-ix,fz=z-iz;
        double h=(1-fz)*((1-fx)*Arena.ground(ix,iz)+fx*Arena.ground(ix+1,iz))+fz*((1-fx)*Arena.ground(ix,iz+1)+fx*Arena.ground(ix+1,iz+1));
        double y=Math.max(h+2,c.player.getY()+3+Math.sin(t*Math.PI)*7);
        double dx=c.player.getX()-x,dz=c.player.getZ()-z,dy=c.player.getY()+1.3-y;
        cameraX=x;cameraY=y;cameraZ=z;cameraYaw=(float)(Math.toDegrees(Math.atan2(dz,dx))-90);cameraPitch=(float)-Math.toDegrees(Math.atan2(dy,Math.hypot(dx,dz)));
        frameSample();
    }
    static final java.util.List<Double> frameMs=new java.util.ArrayList<>();static long lastFrame;static int take;
    static void frameSample(){if(!Diagnostics.active())return;long now=System.nanoTime();if(lastFrame!=0)frameMs.add((now-lastFrame)/1000000.0);lastFrame=now;}
    static void frameReport(){
        if(!Diagnostics.active()||frameMs.isEmpty())return;
        try{var sorted=new java.util.ArrayList<>(frameMs);java.util.Collections.sort(sorted);double mean=frameMs.stream().mapToDouble(Double::doubleValue).average().orElse(0);String data="{\"frames\":"+frameMs.size()+",\"averageFps\":"+(1000/mean)+",\"medianFrameMs\":"+sorted.get(sorted.size()/2)+",\"p95FrameMs\":"+sorted.get((int)(sorted.size()*.95))+",\"maxFrameMs\":"+sorted.get(sorted.size()-1)+"}";Path dir=MinecraftClient.getInstance().runDirectory.toPath().resolve("gravesandglory/test-camera");Files.createDirectories(dir);Files.writeString(dir.resolve("take-"+(++take)+".json"),data);System.out.println("[Graves and Glory camera] "+data);}catch(Exception ignored){}
        frameMs.clear();lastFrame=0;
    }

    static void end(MinecraftClient c){if(camera!=null){frameReport();c.setCameraEntity(c.player);c.options.hudHidden=previousHud;if(previous!=null)c.options.setPerspective(previous);camera=null;}}
    public static void hud(DrawContext g,int w,int h){
        if(camera==null)return;var c=MinecraftClient.getInstance();int bar=Math.max(18,h/10);g.fill(0,0,w,bar,DarkUi.LETTERBOX);g.fill(0,h-bar,w,h,DarkUi.LETTERBOX);
        boolean march=battle.has("name");
        g.drawCenteredTextWithShadow(c.textRenderer,Spec.MAPS[map].name().toUpperCase()+"  /  "+(march?MarchMath.chapterName(marchRow(),Spec.CAMPAIGNCHAPTERS).toUpperCase():mode.equals("ai")?"PRACTICE":mode+" v "+mode),w/2,bar+12,DarkUi.GOLD);
        if(modeId().equals(Modes.SIEGE)){ // who attacks and who defends, and the viewer's own part, under the map line (every Siege map has its attackers: ModeMath.siegeAttackers)
            int att=ModeMath.siegeAttackers(map);
            String[] sides=ModeMath.siegeIntro(map,GravesAndGloryClient.team,FeedLines.teamShort(DarkUi.side(att).name()),FeedLines.teamShort(DarkUi.side(1-att).name())); // the banner colours, as the feed lines name them
            g.drawCenteredTextWithShadow(c.textRenderer,sides[0],w/2,bar+24,DarkUi.IVORY);
            if(!sides[1].isEmpty())g.drawCenteredTextWithShadow(c.textRenderer,sides[1],w/2,bar+36,DarkUi.GOLD);
        }
        if(march)briefing(g,c.textRenderer,w,h-bar-34);
        g.drawCenteredTextWithShadow(c.textRenderer,"YOUR BANNER. YOUR ARMY.",w/2,h-bar-24,DarkUi.IVORY);
        g.drawCenteredTextWithShadow(c.textRenderer,HintText.expand("{key:menu} or Escape to skip entrance",GravesAndGloryClient::hint),w/2,h-bar+6,DarkUi.MUTED);
    }
    /** The briefing card of a campaign battle during its entrance: the battle name, the order of the day and why it matters, ending at y=bottom. */
    static void briefing(DrawContext g,net.minecraft.client.font.TextRenderer font,int w,int bottom){
        int cw=Math.min(400,w-40),x=(w-cw)/2;
        var orders=font.wrapLines(DarkUi.body(text(battle,"briefing")),cw-24);var why=font.wrapLines(DarkUi.body(text(battle,"reason")),cw-24);
        String bonus=bonusLine();var extra=bonus.isEmpty()?java.util.List.<net.minecraft.text.OrderedText>of():font.wrapLines(DarkUi.body(bonus),cw-24);
        int y=bottom-(36+11*(orders.size()+why.size())+(extra.isEmpty()?0:3+11*extra.size()));DarkUi.frame(g,x,y,cw,bottom-y);
        String name=text(battle,"name").toUpperCase(java.util.Locale.ROOT);int tw=(int)(font.getWidth(Text.literal(name).setStyle(net.minecraft.text.Style.EMPTY.withFont(DarkUi.TITLE)))*1.35f);
        DarkUi.title(g,name,w/2-tw/2,y+9,DarkUi.GOLD);y+=26;
        for(var line:orders){g.drawText(font,line,w/2-font.getWidth(line)/2,y,DarkUi.PAPER,false);y+=11;}y+=3;
        for(var line:why){g.drawText(font,line,w/2-font.getWidth(line)/2,y,DarkUi.MUTED,false);y+=11;}
        if(!extra.isEmpty()){y+=3;int ink=bonusClaimed()?DarkUi.MUTED:DarkUi.GOLD;for(var line:extra){g.drawText(font,line,w/2-font.getWidth(line)/2,y,ink,false);y+=11;}}
    }
    /** How deep the entity is in its flicker (0 to 1), for the spent-glimpur overlay. */
    static float ebbFade(net.minecraft.entity.Entity e){var row=GravesAndGloryClient.battleActive()?marchRow():null;return MarchMath.Ebb.fade(row!=null&&row.ebb(),e.getId(),e.getWorld().getTime());}
    /** Spent glimpur, drawn over the glow pixels in normal light as a caster gutters, so the dim reads in daylight too (Kort, 2026-11-11). */
    static final float SPENT_R=.13f,SPENT_G=.11f,SPENT_B=.16f,SPENT_ALPHA=.9f;
    /** The glimpur ebb (brief N3) on this caster's glow: 1 unless a running campaign battle sets ebb and the caster is mid-flicker. Keyed on the network id and world time, as the server's particles and sound are. */
    static float ebbGlow(net.minecraft.entity.Entity e){var row=GravesAndGloryClient.battleActive()?marchRow():null;return MarchMath.Ebb.glow(row!=null&&row.ebb(),e.getId(),e.getWorld().getTime());}
    /** The battle being fought as its campaignBattles row, or null (a room, practice). */
    static Spec.CampaignBattlesRow marchRow(){return MarchMath.row(text(battle,"id"),Spec.CAMPAIGNBATTLES);}
    /** Whether the bonus of the battle being fought has paid before (plan 2C.5). */
    static boolean bonusClaimed(){var b=MarchMath.bonus(marchRow(),Spec.BONUSOBJECTIVES);return b!=null&&profile.bonusClaimed.contains(b.id());}
    /** The briefing's bonus line (plan 2C.5): the objective and what it pays, or that it was claimed; empty for a battle with no bonus. */
    static String bonusLine(){
        var row=marchRow();var b=MarchMath.bonus(row,Spec.BONUSOBJECTIVES);if(b==null)return "";
        return bonusClaimed()?"Bonus claimed: "+b.text():"Bonus: "+b.text()+" "+MarchText.reward(b,row);
    }
}
