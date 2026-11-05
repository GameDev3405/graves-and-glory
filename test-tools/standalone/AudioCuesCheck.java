package gg.gravesandglory;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/** Plan 4F.4: the sound table (AudioCues), sounds.json, the lang subtitles, the sound files and the play calls agree, and the cooldowns hold. */
public final class AudioCuesCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args) throws Exception {
  Path assets=Path.of("src/main/resources/assets/gravesandglory");
  JsonObject sounds=JsonParser.parseString(Files.readString(assets.resolve("sounds.json"))).getAsJsonObject();
  JsonObject lang=JsonParser.parseString(Files.readString(assets.resolve("lang/en_us.json"))).getAsJsonObject();
  var ids=AudioCues.ids();
  check(new HashSet<>(ids).size()==ids.size(),"Every event id is registered once");
  check(new HashSet<>(sounds.keySet()).equals(new HashSet<>(ids)),"sounds.json has an entry for every registered event and for nothing else: "+sounds.keySet()+" vs "+ids);
  // The events the plan names are all in the table.
  for(String must:List.of("capture_start","capture_won","capture_lost","gate_hit","gate_breach","convoy_roll","convoy_stop","convoy_rollback","convoy_checkpoint","wave_warning","wave_rise","victory","defeat","countdown_tick","ui_confirm","ui_back","ui_deny"))
   check(ids.contains(must),"The table has the "+must+" cue");
  for(AudioCues.Cue c:AudioCues.Cue.values()){
   check(c.volume>0&&c.volume<=1f,c.id+": volume in (0, 1], so nothing outshouts a vanilla blow (1.0)");
   check(c.pitch>=.5f&&c.pitch<=2f,c.id+": pitch in the engine's 0.5 to 2.0");
   check(c.cooldownTicks>=0,c.id+": cooldown is not negative");
   check(Set.of("master","players","hostile","neutral","ambient","music").contains(c.category),c.id+": a known sound category");
   JsonObject entry=sounds.getAsJsonObject(c.id);
   JsonObject sound=entry.getAsJsonArray("sounds").get(0).getAsJsonObject();
   if(c.remap==null){
    // A third-party variant group (docs/AUDIO.md Credits): every variant is a file of its own, the game picks one at random; music streams, one-shots do not.
    check(c.remap==null&&entry.getAsJsonArray("sounds").size()>=1,c.id+": a variant group plays its own files");
    for(var v:entry.getAsJsonArray("sounds")){
     JsonObject o=v.getAsJsonObject();String n=o.get("name").getAsString();
     check(n.startsWith("gravesandglory:")&&!o.has("type"),c.id+": variant "+n+" is a file of the mod");
     check(Files.exists(assets.resolve("sounds/"+n.substring("gravesandglory:".length())+".ogg")),c.id+": variant "+n+" is in the repo");
     if(c.music()||c.category.equals("ambient"))check(o.get("stream").getAsBoolean(),c.id+": music and ambient beds stream (tools/install_audio.py decides the rest)");
    }
    check(c.music()==(c.id.startsWith("music.")||c.id.endsWith("_music")),c.id+": the music category is for the music cues alone");
    if(c.category.equals("ambient"))check(c.scope==AudioCues.Scope.LOCAL,c.id+": an ambient bed is client side");
    if(c.music())check(c.scope==AudioCues.Scope.LOCAL&&c.subtitle.isEmpty()&&c.cooldownTicks==0,c.id+": music is client side with no subtitle");
    if(c.scope==AudioCues.Scope.NEARBY&&!c.id.equals("gate_hit"))check(c.maxPerTick>0&&c.cooldownTicks>0,c.id+": a positional combat cue is capped per tick and has a cooldown");
   }else if(c.remap!=null){
    check(sound.get("name").getAsString().equals(c.remap)&&sound.has("type")&&sound.get("type").getAsString().equals("event"),c.id+": remapped to "+c.remap+" as an event, with no file of its own");
    check(c.remap.startsWith("minecraft:"),c.id+": a remap names a vanilla event");
   }
   String key="subtitles.gravesandglory."+c.id;
   if(c.subtitle.isEmpty())check(!entry.has("subtitle")&&!lang.has(key),c.id+": no subtitle anywhere");
   else check(entry.has("subtitle")&&entry.get("subtitle").getAsString().equals(key)&&lang.has(key)&&lang.get(key).getAsString().equals(c.subtitle),c.id+": its subtitle is in sounds.json and en_us.json");
   // the order shouts are Kort's 2026-11-04 exception: 16 blocks of range needs volume 1.0 (range is about 16 x volume), throttled per commander in BattleEffects.feedback
   if(!c.id.startsWith("order_")&&(c.scope==AudioCues.Scope.ALL||c.scope==AudioCues.Scope.TEAM||c.scope==AudioCues.Scope.NEARBY))
    check(c.volume<=.9f,c.id+": a cue everyone hears stays at or under 0.9");
   if(c.scope==AudioCues.Scope.LOCAL&&c.category.equals("master")&&c.id.startsWith("ui_"))check(c.volume<=.6f,c.id+": a UI cue stays quiet");
  }
  for(String h:AudioCues.HORNS){
   check(sounds.getAsJsonObject(h).getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString().startsWith("gravesandglory:"+h),h+" plays its own files");
   check(Files.exists(snd(assets,h)),h+": its first variant is in the repo");
  }
  // Every sound file in the mod is claimed by an event (no orphan audio), and every cue is played from somewhere.
  Set<String> claimed=new HashSet<>();
  for(var e:sounds.entrySet())for(var v:e.getValue().getAsJsonObject().getAsJsonArray("sounds"))claimed.add(v.getAsJsonObject().get("name").getAsString().replace("gravesandglory:",""));
  try(var files=Files.list(assets.resolve("sounds"))){
   for(Path f:files.collect(Collectors.toList())){String n=f.getFileName().toString().replace(".ogg","");check(claimed.contains(n),"The file "+f.getFileName()+" belongs to a registered event");}
  }
  String code;
  try(var walk=Files.walk(Path.of("src/main/java/gg/gravesandglory"))){
   code=walk.filter(p->p.toString().endsWith(".java")&&!p.getFileName().toString().equals("AudioCues.java")).map(p->{try{return Files.readString(p);}catch(Exception e){throw new RuntimeException(e);}}).collect(Collectors.joining("\n"));
  }
  // Registered with files and a subtitle but no game moment yet (docs/AUDIO.md, "Registered, not played"): the only cues allowed to have no caller.
  Set<String> unplayed=Set.of("SIEGE_RAM_SWING","GATE_CHAINS","BANNER_FLAP","MARCH_LEATHER","TROOP_VOICES","TITLE_MUSIC","CAPTURE_RISING","COIN_CLINK","ACTIVE_RALLY","RELIC_USE_LIGHT","RELIC_USE_LIGHTNING","VETERAN_LEVELUP","WAGON_WHEELS");
  // The ambience beds are picked by AudioCues.bed (so the choice is testable without Minecraft) and played by MusicDirector's bed layer.
  Set<AudioCues.Cue> beds=EnumSet.of(AudioCues.Cue.AMBIENCE_WIND,AudioCues.Cue.AMBIENCE_BIRDS,AudioCues.Cue.AMBIENCE_WATER,AudioCues.Cue.AMBIENCE_CROWS,AudioCues.Cue.AMBIENCE_SNOW,AudioCues.Cue.CAMP_ROOM_TONE,AudioCues.Cue.SURVIVAL_WIND);
  for(AudioCues.Cue c:AudioCues.Cue.values()){
   if(beds.contains(c))continue;
   check(unplayed.contains(c.name())?!code.contains("Cue."+c.name()):code.contains("Cue."+c.name()),c.id+": "+(unplayed.contains(c.name())?"is listed as not yet played, so no code plays it (remove it from the list when one does)":"some code plays it"));
  }
  ambienceLayers(code,beds);
  check(code.contains("Audio.register()"),"The mod registers the table at start");
  // The cooldowns.
  var t=new AudioCues.Throttle();
  var start=AudioCues.Cue.CAPTURE_START;
  check(t.allow(start,"round",1000),"The first contest cue plays");
  check(!t.allow(start,"round",1000+start.cooldownTicks-1),"A second one inside the cooldown does not");
  check(t.allow(start,"round",1000+start.cooldownTicks),"It plays again once the cooldown is over");
  check(t.allow(AudioCues.Cue.GATE_HIT,"gate",50)&&!t.allow(AudioCues.Cue.GATE_HIT,"gate",51)&&t.allow(AudioCues.Cue.GATE_HIT,"other",51),"A key limits only itself");
  check(t.allow(start,"round",5),"A clock that went back (a new round) frees the cue");
  check(t.allow(AudioCues.Cue.DEFEAT,"x",1)&&t.allow(AudioCues.Cue.DEFEAT,"x",1),"A cue with no cooldown always plays");
  // The per-tick cap: thirty troops striking in one tick sound at most maxPerTick times, each with its own key; the next tick starts fresh; a refusal records nothing.
  var cap=AudioCues.Cue.SWORD_CLASH;var u=new AudioCues.Throttle();int played=0;
  for(int i=0;i<30;i++)if(u.allow(cap,"unit"+i,500))played++;
  check(played==cap.maxPerTick,"Thirty troops in one tick play the clash "+cap.maxPerTick+" times, not "+played);
  check(u.allow(cap,"unit0b",501),"The next tick has room again");
  var v=new AudioCues.Throttle();
  check(v.allow(cap,"a",10)&&!v.allow(cap,"a",11),"One soldier's clash is spaced by the cooldown even with room in the tick");
  check(v.allow(cap,"a",10+cap.cooldownTicks),"It plays again after the cooldown");
  for(AudioCues.Cue c:AudioCues.Cue.values())if(c.id.contains(".")&&!c.music()&&c.scope==AudioCues.Scope.NEARBY)check(c.volume<=.6f,c.id+": a combat cue stays under 0.6 so a melee is a murmur");
  for(AudioCues.Cue c:List.of(AudioCues.Cue.CAPTURE_START,AudioCues.Cue.GATE_HIT,AudioCues.Cue.CONVOY_ROLL,AudioCues.Cue.CONVOY_STOP,AudioCues.Cue.CONVOY_ROLLBACK,AudioCues.Cue.WAVE_WARNING,AudioCues.Cue.UI_CONFIRM,AudioCues.Cue.UI_BACK,AudioCues.Cue.UI_DENY))
   check(c.cooldownTicks>0,c.id+": a cue that can repeat has a cooldown");
  timing(assets,code);
  System.out.println("PASS "+count+" audio table scenarios");
 }

 /** The ambience and music layers: each bed is chosen by some moment and played in the ambient category, and the briefing and overtime music have their play sites. */
 static void ambienceLayers(String code,Set<AudioCues.Cue> beds)throws Exception{
  String director=Files.readString(Path.of("src/main/java/gg/gravesandglory/MusicDirector.java"));
  // Every bed is reachable from a moment: the camp, a survival battle on any map, or the battle bed of a map's theme (five maps, five themes, five beds).
  Set<AudioCues.Cue> reached=EnumSet.noneOf(AudioCues.Cue.class);
  check(AudioCues.bed(true,"","")==AudioCues.Cue.CAMP_ROOM_TONE&&AudioCues.bed(true,"survival","river")==AudioCues.Cue.CAMP_ROOM_TONE,"The Banner Camp has its room tone");
  reached.add(AudioCues.Cue.CAMP_ROOM_TONE);
  Set<AudioCues.Cue> mapBeds=EnumSet.noneOf(AudioCues.Cue.class);
  for(var m:Spec.MAPS){
   check(AudioCues.bed(false,"survival",m.theme())==AudioCues.Cue.SURVIVAL_WIND,m.id()+": a survival battle plays the survival wind");
   var b=AudioCues.bed(false,"domination",m.theme());check(b.name().startsWith("AMBIENCE_"),m.id()+": a battle bed is an ambience bed, "+b.name());mapBeds.add(b);reached.add(b);reached.add(AudioCues.Cue.SURVIVAL_WIND);
   check(AudioCues.bed(false,"siege",m.theme())==b&&AudioCues.bed(false,"convoy",m.theme())==b,m.id()+": the bed follows the map, not the mode");
  }
  check(mapBeds.size()==Spec.MAPS.length,"Each map has a bed of its own: "+mapBeds);
  check(AudioCues.bed(false,"domination","unknown")==AudioCues.Cue.AMBIENCE_WIND&&AudioCues.bed(false,"domination",null)==AudioCues.Cue.AMBIENCE_WIND,"An unknown theme falls back to the wind");
  reached.add(AudioCues.Cue.AMBIENCE_WIND);
  check(reached.containsAll(beds),"Every bed is chosen by some moment: "+reached);
  for(var c:beds)check(c.category.equals("ambient"),c.id+": a bed is in the ambient category, so the vanilla Ambient slider governs it");
  check(director.contains("AudioCues.bed(")&&director.contains("SoundCategory.AMBIENT")&&director.contains("this.repeat = loop")&&director.contains("bed = new Track("),"MusicDirector plays the bed as a looping ambient stream");
  check(director.contains("BED_FADE_IN_TICKS")&&director.contains("BED_FADE_OUT_TICKS"),"The bed fades in and out");
  // Briefing music: the director asks the screen, and the screen answers for the campaign sheet.
  check(director.contains("Cue.CAMPAIGN_BRIEFING_MUSIC")&&director.contains("briefingOpen(c)"),"MusicDirector plays the briefing music while the briefing screen is up");
  check(Files.readString(Path.of("src/main/java/gg/gravesandglory/CampScreen.java")).contains("boolean briefing()"),"The camp screen says when it is the briefing");
  // Overtime music: the server sends the flag, the client reads it, the director swaps the battle music for it.
  check(Files.readString(Path.of("src/main/java/gg/gravesandglory/BattleMode.java")).contains("j.addProperty(\"overtime\",b.inOvertime())"),"The server's battleMode snapshot carries overtime");
  check(Files.readString(Path.of("src/main/java/gg/gravesandglory/CampaignClient.java")).contains("battleMode.get(\"overtime\")"),"The client reads battleMode.overtime");
  check(director.contains("CampaignClient.overtime()")&&director.contains("Cue.OVERTIME_MUSIC"),"MusicDirector swaps the battle music for overtime_music while the round is in overtime");
  check(AudioCues.Cue.OVERTIME_MUSIC.music()&&AudioCues.Cue.CAMPAIGN_BRIEFING_MUSIC.music(),"The briefing and overtime tracks are music, governed by the Music slider");
  // The cues Kort kept off still have no play site.
  for(String off:List.of("TROOP_VOICES","MARCH_LEATHER","SIEGE_RAM_SWING","GATE_CHAINS","BANNER_FLAP"))check(!code.contains("Cue."+off),off+" stays off");
 }

 /** The seconds an Ogg Vorbis file plays: the last page's granule position over the sample rate of the identification header. */
 /** The first variant file of an event, as sounds.json lists it (every one-shot is a variant group now: victory_1.ogg and so on). */
 static Path snd(Path assets,String id)throws Exception{JsonObject sounds=JsonParser.parseString(Files.readString(assets.resolve("sounds.json"))).getAsJsonObject();return assets.resolve("sounds/"+sounds.getAsJsonObject(id).getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString().replace("gravesandglory:","")+".ogg");}
 /** The shortest and longest variant of an event, in seconds. */
 static double[] range(Path assets,String id)throws Exception{JsonObject sounds=JsonParser.parseString(Files.readString(assets.resolve("sounds.json"))).getAsJsonObject();double lo=1e9,hi=0;for(var v:sounds.getAsJsonObject(id).getAsJsonArray("sounds")){double t=oggSeconds(assets.resolve("sounds/"+v.getAsJsonObject().get("name").getAsString().replace("gravesandglory:","")+".ogg"));lo=Math.min(lo,t);hi=Math.max(hi,t);}return new double[]{lo,hi};}
 static double oggSeconds(Path f) throws Exception {
  byte[] d=Files.readAllBytes(f);
  int id=-1;for(int i=0;i<d.length-6&&id<0;i++)if(d[i]==1&&d[i+1]=='v'&&d[i+2]=='o'&&d[i+3]=='r'&&d[i+4]=='b'&&d[i+5]=='i'&&d[i+6]=='s')id=i;
  check(id>=0,f+": a Vorbis stream");
  long rate=(d[id+12]&0xffL)|(d[id+13]&0xffL)<<8|(d[id+14]&0xffL)<<16|(d[id+15]&0xffL)<<24;
  int last=-1;for(int i=d.length-14;i>=0&&last<0;i--)if(d[i]=='O'&&d[i+1]=='g'&&d[i+2]=='g'&&d[i+3]=='S')last=i;
  long granule=0;for(int k=7;k>=0;k--)granule=granule<<8|(d[last+6+k]&0xffL);
  return granule/(double)rate;
 }

 /** Plans of the timing audit (docs/reports/audio-cue-timing-2026-11-02.md): each cue plays once, at its moment. */
 static void timing(Path assets,String code) throws Exception {
  // 1. A blow from Tactics.swing lands on its clip's impact frame already, so its sound has no wait (HouseAttacks and TroopClips went with the motion engine);
  // the contact ticks are left for melee that still resolves on the swing's first tick.
  check(Files.readString(Path.of("src/main/java/gg/gravesandglory/Audio.java")).contains("if (u != null && u.blowVariant >= 0) return 0;"),"An impact-frame blow (Tactics.land) does not wait a second time");
  check(AudioCues.contactTicks(null)==AudioCues.DEFAULT_CONTACT_TICKS&&AudioCues.contactTicks("nobody")==AudioCues.DEFAULT_CONTACT_TICKS,"An unknown House uses the default");
  for(int t:AudioCues.CONTACT_TICKS.values())check(t>0&&t<=10,"A contact tick is inside the 0.5 s attack clip");
  // 2. The waiting queue: a delayed cue plays on its tick, once, in order; a zero delay plays now; a clear or a clock that went back drops it.
  var s=new AudioCues.Schedule();List<String> got=new ArrayList<>();
  s.add(100,0,()->got.add("now"));check(got.equals(List.of("now")),"A zero delay plays at once");
  s.add(100,7,()->got.add("seven"));s.add(100,5,()->got.add("five"));
  s.tick(104);check(got.size()==1,"Nothing plays before its tick");
  s.tick(105);check(got.equals(List.of("now","five")),"The five-tick cue plays on tick 105");
  s.tick(106);s.tick(107);s.tick(108);check(got.equals(List.of("now","five","seven")),"The seven-tick cue plays on 107, once");
  s.add(500,10,()->got.add("old"));s.tick(10);check(s.size()==0&&!got.contains("old"),"A cue queued by an earlier clock is dropped when the clock goes back");
  s.add(10,5,()->got.add("cleared"));s.clear();s.tick(99);check(!got.contains("cleared"),"A clear drops what waits");
  // A cue that queues another from its own play does not break the pass.
  var chain=new ArrayList<String>();s.add(0,1,()->{chain.add("a");s.add(1,1,()->chain.add("b"));});s.tick(1);s.tick(2);check(chain.equals(List.of("a","b")),"A cue may queue another");
  // 3. The clock plays once a second through the last ten seconds and the countdown through the last three: ten and three cues, none at 0, none off the second.
  int clock=0,cd=0;for(long left=1200;left>=0;left--){if(AudioCues.clockSecond(left))clock++;if(AudioCues.countdownSecond(left))cd++;}
  check(clock==10,"The clock ticks ten times in the last ten seconds, not "+clock);
  check(cd==3,"The countdown ticks three times, not "+cd);
  check(!AudioCues.clockSecond(201)&&!AudioCues.clockSecond(0)&&AudioCues.clockSecond(200)&&AudioCues.clockSecond(20),"The first tick is at ten seconds and the last at one");
  // The clock cue's own cooldown (15) is shorter than its spacing (20), so none of the ten is lost.
  var clk=new AudioCues.Throttle();int heard=0;for(long left=200;left>0;left-=20)if(clk.allow(AudioCues.Cue.CLOCK_TICK,"clock",1000-left))heard++;
  check(heard==10,"The throttle lets all ten clock ticks through, not "+heard);
  // 4. A volley: thirty archers loosing in one tick sound the bow at most its cap, spread over their own keys; the same archer is spaced by the cooldown.
  var vol=new AudioCues.Throttle();int shots=0;for(int i=0;i<30;i++)if(vol.allow(AudioCues.Cue.BOW_SHOOT,"b"+i,700))shots++;
  check(shots==AudioCues.Cue.BOW_SHOOT.maxPerTick,"A volley sounds the bow "+AudioCues.Cue.BOW_SHOOT.maxPerTick+" times in a tick, not "+shots);
  // 5. Two points contested in the same second sound the contest cue once; in different seconds each sounds (a key per point, so one point's cooldown does not hide the other's).
  var cs=new AudioCues.Throttle();
  check(cs.allow(AudioCues.Cue.CAPTURE_START,"Yard",2000)&&!cs.allow(AudioCues.Cue.CAPTURE_START,"Keep",2000),"Two points in one tick sound once");
  check(cs.allow(AudioCues.Cue.CAPTURE_START,"Keep",2020),"The second point, a second later, still sounds");
  check(!cs.allow(AudioCues.Cue.CAPTURE_START,"Yard",2100),"The same point does not sound again inside the cooldown");
  // 6. The stings, the gate and the wave: the lead each is started ahead by matches its file (the strike or swell is that far into the sound).
  double victory=oggSeconds(snd(assets,"victory")),defeat=oggSeconds(snd(assets,"defeat"));
  double[] vr=range(assets,"victory");
  check(AudioCues.VICTORY_STING_TICKS>=vr[0]*20-1&&AudioCues.VICTORY_STING_TICKS<=vr[1]*20+1,"The music waits a victory sting's length (the variants run "+vr[0]+" to "+vr[1]+" s): "+AudioCues.VICTORY_STING_TICKS+" ticks");
  check(Math.abs(AudioCues.DEFEAT_STING_TICKS-defeat*20)<=1,"The music waits the defeat sting's length: "+AudioCues.DEFEAT_STING_TICKS+" ticks vs "+defeat+" s");
  check(AudioCues.GATE_SHUT_LEAD_TICKS*.05<oggSeconds(snd(assets,"gate_shut"))&&AudioCues.WAVE_RISE_LEAD_TICKS*.05<oggSeconds(snd(assets,"wave_rise")),"A lead is inside its sound");
  // The approved samples are untouched (Kort approved the sounds as they are): the bow, arrow and armour files keep their lengths.
  check(Math.abs(oggSeconds(assets.resolve("sounds/arrow_hit_1.ogg"))-.23)<.02&&Math.abs(oggSeconds(assets.resolve("sounds/bow_shoot_1.ogg"))-.65)<.02&&Math.abs(oggSeconds(assets.resolve("sounds/armor_hit_1.ogg"))-.76)<.02,"The bow, arrow and armour samples are the approved ones, trimmed so the strike is at the start");
  // 7. Where the cues are played from (text of the sources): one place each, so none can double.
  check(count(code,"Cue.GATE_SHUT")==1,"The gate shuts from one place");
  check(count(code,"Cue.WAVE_RISE")==1,"The wave rises from one place");
  check(count(code,"Cue.GATE_BREACH")==2,"The breach sounds with the felling blow, or from breach() when nothing felled it: two places");
  check(count(code,"Cue.CLOCK_TICK")==1&&count(code,"Cue.COUNTDOWN_TICK")==1&&count(code,"Cue.CAPTURE_START")==1,"The clock, countdown and contest cues each have one caller");
  check(!code.contains("BlockPos.ORIGIN, winner"),"No world sound at the origin on top of the ending sting");
  check(code.contains("Audio.tick(this)")&&code.contains("Audio.reset()"),"The waiting cues are run each tick and dropped at the round's start and end");
  check(code.contains("MAGIC_CAST")&&count(code,"Cue.MAGIC_CAST")==1,"The spell is cast from one place (Audio.shot), not again when the cloud lands");
 }

 static int count(String text,String needle){int n=0,i=0;while((i=text.indexOf(needle,i))>=0){n++;i+=needle.length();}return n;}
}
