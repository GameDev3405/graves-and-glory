package gg.gravesandglory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Prop defs (prop kit plan task S2): every shipped def parses with the contract table's size, parts and CMD numbers, and parse refuses a cell
 *  outside the size, an empty solid list, a light on a solid cell, a CMD outside 7310001..7310999 and a duplicate part model, naming the field. */
public final class PropDefCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 static final String DIR="src/main/resources/data/gravesandglory/gg_props/";
 static final String[] IDS={"forge","anvil","weapon_rack","shield_rack","quench_trough","grindstone_bench"};
 static final int[][] SIZES={{3,4,2},{1,1,1},{2,2,1},{2,2,1},{2,1,1},{2,2,1}};
 static final int[] SOLID={14,1,4,4,2,2};

 static String def(String parts,String solid,String lights){
  return "{\"id\":\"t\",\"category\":\"smithy\",\"name\":\"T\",\"size\":[3,1,1],\"parts\":"+parts+",\"solid\":"+solid+",\"lights\":"+lights+",\"effects\":[]}";
 }
 static String refusal(String json){
  try{PropDef.parse(json);return null;}catch(IllegalArgumentException e){return e.getMessage()==null?"":e.getMessage();}
 }

 public static void main(String[] args)throws Exception{
  for(int i=0;i<IDS.length;i++){
   PropDef d=PropDef.parse(Files.readString(Path.of(DIR+IDS[i]+".json")));
   check(d.id().equals(IDS[i]),IDS[i]+": id matches the file name");
   check(d.category().equals("smithy")&&!d.name().isBlank(),IDS[i]+": category smithy and a display name");
   check(d.sx()==SIZES[i][0]&&d.sy()==SIZES[i][1]&&d.sz()==SIZES[i][2],IDS[i]+": size per the contract table");
   check(d.solid().size()==SOLID[i],IDS[i]+": "+SOLID[i]+" solid cells, got "+d.solid().size());
   check(i<=1?!d.effects().isEmpty():d.effects().isEmpty(),IDS[i]+(i<=1?": forge and anvil have alive effects (S5)":": no effects (S5 animates the forge and anvil only)"));
   List<String> models=i==0?List.of("hearth","hood","coals"):List.of(IDS[i]);
   check(d.parts().size()==models.size(),IDS[i]+": parts per the contract table");
   for(int p=0;p<models.size();p++){
    PropDef.Part part=d.parts().get(p);
    check(part.model().equals(models.get(p)),IDS[i]+": part "+p+" is "+models.get(p));
    check(part.cmd()==PropDef.cmd(i,p),IDS[i]+": part "+p+" CMD is 7310001+10*"+i+"+"+p+", got "+part.cmd());
    check(part.glow()==(i==0&&p==2),IDS[i]+": only the forge coals glow");
   }
  }
  PropDef forge=PropDef.parse(Files.readString(Path.of(DIR+"forge.json")));
  check(forge.lights().size()==2&&forge.lights().stream().allMatch(l->l.y()==2&&l.level()==12&&l.z()==0&&(l.x()==0||l.x()==2)),"Forge lights at (0,2,0) and (2,2,0), level 12");
  check(forge.solid().stream().anyMatch(c->c[0]==1&&c[1]==3&&c[2]==1),"Forge chimney top (1,3,1) is solid");
  check(PropDef.cmd(0,0)==7310001&&PropDef.cmd(1,0)==7310011&&PropDef.cmd(5,0)==7310051,"CMD numbering starts at 7310001");

  String ok=def("[{\"model\":\"a\",\"cmd\":7310001}]","[[0,0,0],[2,0,0]]","[{\"x\":1,\"y\":0,\"z\":0,\"level\":9}]");
  check(refusal(ok)==null,"A valid def parses");
  PropDef t=PropDef.parse(ok);
  check(!t.parts().get(0).glow()&&t.lights().get(0).level()==9,"glow defaults to false; light level kept");
  String r;
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310001}]","[[3,0,0]]","[]"));
  check(r!=null&&r.contains("solid"),"Cell (3,0,0) in a 3-wide prop is refused naming solid: "+r);
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310001}]","[]","[]"));
  check(r==null,"An empty solid list parses (wall-hung and overhead props have no collision): "+r);
  String vent="{\"id\":\"t\",\"category\":\"x\",\"name\":\"T\",\"size\":[1,1,1],\"parts\":[{\"model\":\"a\",\"cmd\":7310001}],\"solid\":[],\"effects\":[{\"kind\":\"particle\",\"id\":\"minecraft:campfire_cosy_smoke\",\"x\":0.5,\"y\":0.2,\"z\":0.5}]}";
  check(refusal(vent)==null&&PropDef.parse(vent).solid().isEmpty(),"An effect-only prop (effects, no solid cell) parses: "+refusal(vent));
  String far="{\"id\":\"t\",\"category\":\"x\",\"name\":\"T\",\"size\":[1,1,1],\"parts\":[{\"model\":\"a\",\"cmd\":7310001,\"offset\":[-24,112,96]}],\"solid\":[[0,0,0]]}";
  check(refusal(far)==null&&PropDef.parse(far).parts().get(0).oy()==112&&PropDef.parse(far).parts().get(0).ox()==-24,"A part offset is read in px: "+refusal(far));
  check(PropDef.parse(ok).parts().get(0).oz()==0,"A part without offset sits at the anchor");
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310001}]","[[0,0,0]]","[{\"x\":0,\"y\":0,\"z\":0,\"level\":9}]"));
  check(r!=null&&r.contains("lights"),"A light on a solid cell is refused naming lights: "+r);
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310000}]","[[0,0,0]]","[]"));
  check(r!=null&&r.contains("cmd"),"CMD 7310000 is refused naming cmd: "+r);
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7311000}]","[[0,0,0]]","[]"));
  check(r!=null&&r.contains("cmd"),"CMD 7311000 is refused naming cmd: "+r);
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310001},{\"model\":\"a\",\"cmd\":7310002}]","[[0,0,0]]","[]"));
  check(r!=null&&r.contains("model"),"A duplicate part model is refused naming model: "+r);
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310001}]","[[0,0,0]]","[{\"x\":3,\"y\":0,\"z\":0,\"level\":9}]"));
  check(r!=null&&r.contains("lights"),"A light outside the size is refused naming lights: "+r);
  r=refusal(def("[{\"model\":\"a\",\"cmd\":7310001}]","[[0,0,0],[0,0,0]]","[]"));
  check(r!=null&&r.contains("solid"),"A repeated solid cell is refused naming solid: "+r);
  PropDef bar=PropDef.parse(Files.readString(Path.of(DIR+"stake_barricade.json")));
  check(bar.gameplay()!=null&&bar.gameplay().hp()==6&&bar.gameplay().heavyHit()==2&&bar.gameplay().siegeHit()==3&&bar.gameplay().cover()==.25&&bar.gameplay().siegeCap()==4,"Stake barricade gameplay: hp 6, heavy 2, siege 3, cover 0.25, Siege cap 4");
  check(PropDef.parse(ok).gameplay()==null&&forge.gameplay()==null,"A def without a gameplay block has none");
  String gp=ok.substring(0,ok.length()-1)+",\"gameplay\":{\"hp\":3}}";
  check(PropDef.parse(gp).gameplay().cover()==0&&PropDef.parse(gp).gameplay().siegeCap()==0,"Gameplay fields default to off");
  r=refusal(ok.substring(0,ok.length()-1)+",\"gameplay\":{\"cover\":1.5}}");
  check(r!=null&&r.contains("cover"),"Cover over 1 is refused naming cover: "+r);
  r=refusal("{\"id\":\"t\"}");
  check(r!=null&&!r.isEmpty(),"A def missing its fields is refused: "+r);
  r=refusal("not json");
  check(r!=null,"Unreadable JSON is refused");
  System.out.println("PASS "+count+" prop def scenarios");
 }
}
