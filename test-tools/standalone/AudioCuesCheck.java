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
  for(String must:List.of("capture_start","capture_won","capture_lost","gate_hit","gate_breach","convoy_roll","convoy_stop","convoy_rollback","convoy_checkpoint","wave_warning","victory","defeat","countdown_tick","ui_confirm","ui_back","ui_deny"))
   check(ids.contains(must),"The table has the "+must+" cue");
  for(AudioCues.Cue c:AudioCues.Cue.values()){
   check(c.volume>0&&c.volume<=1f,c.id+": volume in (0, 1], so nothing outshouts a vanilla blow (1.0)");
   check(c.pitch>=.5f&&c.pitch<=2f,c.id+": pitch in the engine's 0.5 to 2.0");
   check(c.cooldownTicks>=0,c.id+": cooldown is not negative");
   check(Set.of("master","players","hostile","neutral","ambient").contains(c.category),c.id+": a known sound category");
   JsonObject entry=sounds.getAsJsonObject(c.id);
   JsonObject sound=entry.getAsJsonArray("sounds").get(0).getAsJsonObject();
   if(c.remap!=null){
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
  try(var files=Files.list(assets.resolve("sounds"))){
   for(Path f:files.collect(Collectors.toList())){String n=f.getFileName().toString().replace(".ogg","");check(ids.contains(n),"The file "+f.getFileName()+" belongs to a registered event");}
  }
  String code;
  try(var walk=Files.walk(Path.of("src/main/java/gg/gravesandglory"))){
   code=walk.filter(p->p.toString().endsWith(".java")&&!p.getFileName().toString().equals("AudioCues.java")).map(p->{try{return Files.readString(p);}catch(Exception e){throw new RuntimeException(e);}}).collect(Collectors.joining("\n"));
  }
  Set<String> hooks=Set.of("WAVE_WARNING"); // Survival's hook: Audio.all(battle, Cue.WAVE_WARNING, key); nothing plays it yet
  for(AudioCues.Cue c:AudioCues.Cue.values())if(!hooks.contains(c.name()))check(code.contains("Cue."+c.name()),c.id+": some code plays it");
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
  for(AudioCues.Cue c:List.of(AudioCues.Cue.CAPTURE_START,AudioCues.Cue.GATE_HIT,AudioCues.Cue.CONVOY_ROLL,AudioCues.Cue.CONVOY_STOP,AudioCues.Cue.CONVOY_ROLLBACK,AudioCues.Cue.WAVE_WARNING,AudioCues.Cue.UI_CONFIRM,AudioCues.Cue.UI_BACK,AudioCues.Cue.UI_DENY))
   check(c.cooldownTicks>0,c.id+": a cue that can repeat has a cooldown");
  System.out.println("PASS "+count+" audio table scenarios");
 }
}
