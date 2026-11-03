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
   if(c.id.contains(".")){
    // A third-party variant group (docs/AUDIO.md Credits): every variant is a file of its own, the game picks one at random; music streams, one-shots do not.
    check(c.remap==null&&entry.getAsJsonArray("sounds").size()>=1,c.id+": a variant group plays its own files");
    for(var v:entry.getAsJsonArray("sounds")){
     JsonObject o=v.getAsJsonObject();String n=o.get("name").getAsString();
     check(n.startsWith("gravesandglory:")&&!o.has("type"),c.id+": variant "+n+" is a file of the mod");
     check(Files.exists(assets.resolve("sounds/"+n.substring("gravesandglory:".length())+".ogg")),c.id+": variant "+n+" is in the repo");
     check(o.get("stream").getAsBoolean()==c.music(),c.id+": only music streams");
    }
    check(c.music()==c.id.startsWith("music."),c.id+": the music category is for the music. cues alone");
    if(c.music())check(c.scope==AudioCues.Scope.LOCAL&&c.subtitle.isEmpty()&&c.cooldownTicks==0,c.id+": music is client side with no subtitle");
    if(c.scope==AudioCues.Scope.NEARBY)check(c.maxPerTick>0&&c.cooldownTicks>0,c.id+": a positional combat cue is capped per tick and has a cooldown");
   }else if(c.remap!=null){
    check(sound.get("name").getAsString().equals(c.remap)&&sound.has("type")&&sound.get("type").getAsString().equals("event"),c.id+": remapped to "+c.remap+" as an event, with no file of its own");
    check(c.remap.startsWith("minecraft:"),c.id+": a remap names a vanilla event");
   }else{
    check(sound.get("name").getAsString().equals("gravesandglory:"+c.id)&&!sound.has("type"),c.id+": plays its own file");
    check(Files.exists(assets.resolve("sounds/"+c.id+".ogg")),c.id+": its .ogg is in the repo");
   }
   String key="subtitles.gravesandglory."+c.id;
   if(c.subtitle.isEmpty())check(!entry.has("subtitle")&&!lang.has(key),c.id+": no subtitle anywhere");
   else check(entry.has("subtitle")&&entry.get("subtitle").getAsString().equals(key)&&lang.has(key)&&lang.get(key).getAsString().equals(c.subtitle),c.id+": its subtitle is in sounds.json and en_us.json");
   if(c.scope==AudioCues.Scope.ALL||c.scope==AudioCues.Scope.TEAM||c.scope==AudioCues.Scope.NEARBY)
    check(c.volume<=.9f,c.id+": a cue everyone hears stays at or under 0.9");
   if(c.scope==AudioCues.Scope.LOCAL&&c.category.equals("master")&&c.id.startsWith("ui_"))check(c.volume<=.6f,c.id+": a UI cue stays quiet");
  }
  for(String h:AudioCues.HORNS){
   check(sounds.getAsJsonObject(h).getAsJsonArray("sounds").get(0).getAsJsonObject().get("name").getAsString().equals("gravesandglory:"+h),h+" plays its own file");
   check(Files.exists(assets.resolve("sounds/"+h+".ogg")),h+": its .ogg is in the repo");
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
  for(AudioCues.Cue c:AudioCues.Cue.values())check(code.contains("Cue."+c.name()),c.id+": some code plays it");
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

 /** The seconds an Ogg Vorbis file plays: the last page's granule position over the sample rate of the identification header. */
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
  // 1. A melee blow's sound waits for the attack clip's contact frame: the table must say what the animation data says.
  String houses=Files.readString(Path.of("src/main/java/gg/gravesandglory/HouseAttacks.java"));
  var m=java.util.regex.Pattern.compile("STRIKE\\s*=\\s*Map\\.of\\(([^)]*)\\)").matcher(houses);
  check(m.find(),"HouseAttacks.STRIKE is in the source");
  var pair=java.util.regex.Pattern.compile("\"(\\w+)\"\\s*,\\s*(\\d*\\.\\d+)f").matcher(m.group(1));
  int strikes=0;
  while(pair.find()){strikes++;String house=pair.group(1);double seconds=Double.parseDouble(pair.group(2));
   check(AudioCues.contactTicks(house)==(int)Math.round(seconds*20),house+": the hit sound waits "+Math.round(seconds*20)+" ticks, the clip's strike ("+seconds+" s), not "+AudioCues.contactTicks(house));}
  check(strikes==AudioCues.CONTACT_TICKS.size(),"Every House with a strike has a contact tick: "+strikes+" vs "+AudioCues.CONTACT_TICKS.size());
  var peak=java.util.regex.Pattern.compile("PEAKS\\s*=\\s*Map\\.of\\(\"attack\",\\s*(\\.\\d+)f").matcher(Files.readString(Path.of("src/main/java/gg/gravesandglory/TroopClips.java")));
  check(peak.find()&&AudioCues.DEFAULT_CONTACT_TICKS==(int)Math.round(Double.parseDouble(peak.group(1))*20),"A troop with no House attack waits the rig's attack peak");
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
  double victory=oggSeconds(assets.resolve("sounds/victory.ogg")),defeat=oggSeconds(assets.resolve("sounds/defeat.ogg"));
  check(Math.abs(AudioCues.VICTORY_STING_TICKS-victory*20)<=1,"The music waits the victory sting's length: "+AudioCues.VICTORY_STING_TICKS+" ticks vs "+victory+" s");
  check(Math.abs(AudioCues.DEFEAT_STING_TICKS-defeat*20)<=1,"The music waits the defeat sting's length: "+AudioCues.DEFEAT_STING_TICKS+" ticks vs "+defeat+" s");
  check(AudioCues.GATE_SHUT_LEAD_TICKS*.05<oggSeconds(assets.resolve("sounds/gate_shut.ogg"))&&AudioCues.WAVE_RISE_LEAD_TICKS*.05<oggSeconds(assets.resolve("sounds/wave_rise.ogg")),"A lead is inside its sound");
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
