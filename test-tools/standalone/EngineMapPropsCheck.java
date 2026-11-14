package gg.gravesandglory;

import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

/** The prop anchors a packed bundled map carries (tools/sectionmap.py "anchors" in a .stk, EngineMaps.armAnchors writes them back and runs Props.scanBox).
 * Pure: the tiles are read with a small NBT reader, no Minecraft. A map built without props keeps no "anchors" key (so its packed bytes did not change);
 * Ironhold's Iron Pass (ironhold_ip) holds the 29 anchors of its templates (8 large soldier tents, 1 large pavilion, 2 trebuchets, 5 supply wagons,
 * 1 ore handcart, 12 braziers), each on a command block cell inside its section. */
public final class EngineMapPropsCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 record Anchor(int x,int y,int z,String cmd){}
 /** the parts of a tile the check needs */
 static final class Tile{int[] size;List<String> palette=new ArrayList<>();List<Anchor> anchors=new ArrayList<>();byte[] stack;boolean hasAnchorsKey;}

 static Tile read(Path file)throws Exception{
  try(InputStream in=new GZIPInputStream(Files.newInputStream(file));DataInputStream d=new DataInputStream(in)){
   check(d.readByte()==10,file+": root is a compound");d.readUTF();Tile t=new Tile();
   for(int type;(type=d.readByte())!=0;){String key=d.readUTF();
    switch(key){
     case "size"->{check(type==9,"size is a list");d.readByte();int n=d.readInt();t.size=new int[n];for(int i=0;i<n;i++)t.size[i]=d.readInt();}
     case "palette"->{d.readByte();int n=d.readInt();for(int i=0;i<n;i++){String name=null;for(int ty;(ty=d.readByte())!=0;){String k=d.readUTF();if(k.equals("Name"))name=d.readUTF();else skip(d,ty);}t.palette.add(name);}}
     case "anchors"->{t.hasAnchorsKey=true;d.readByte();int n=d.readInt();
      for(int i=0;i<n;i++){int x=0,y=0,z=0;String cmd=null;for(int ty;(ty=d.readByte())!=0;){String k=d.readUTF();switch(k){case "x"->x=d.readInt();case "y"->y=d.readInt();case "z"->z=d.readInt();case "cmd"->cmd=d.readUTF();default->skip(d,ty);}}t.anchors.add(new Anchor(x,y,z,cmd));}}
     case "stack"->{check(type==7,"stack is a byte array");t.stack=new byte[d.readInt()];d.readFully(t.stack);}
     default->skip(d,type);}}
   return t;}
 }
 static void skip(DataInputStream d,int type)throws Exception{
  switch(type){
   case 1->d.readByte();case 2->d.readShort();case 3->d.readInt();case 4->d.readLong();case 5->d.readFloat();case 6->d.readDouble();
   case 7->d.skipBytes(d.readInt());case 8->d.readUTF();
   case 9->{int et=d.readByte(),n=d.readInt();for(int i=0;i<n;i++)skip(d,et);}
   case 10->{for(int ty;(ty=d.readByte())!=0;){d.readUTF();skip(d,ty);}}
   case 11->d.skipBytes(4*d.readInt());case 12->d.skipBytes(8*d.readInt());
   default->throw new IllegalStateException("nbt tag "+type);}
 }

 public static void main(String[] args)throws Exception{
  Path root=Path.of("src/main/resources/data/gravesandglory/sectionmaps");
  if(!Files.exists(root))root=Path.of(System.getProperty("user.dir")).resolve(root);
  Map<String,Integer> total=new TreeMap<>();
  for(var row:Spec.MAPS){if(row.source().equals("-"))continue;
   EngineMap m=EngineMap.read(root.resolve(row.source()));int anchors=0;Map<String,Integer> ids=new TreeMap<>();
   for(var tile:m.tiles){Tile t=read(root.resolve(row.source()).resolve(tile.file()));
    check(t.size[0]==tile.sx()&&t.size[1]==tile.sy()&&t.size[2]==tile.sz(),tile.file()+": size");
    check(t.hasAnchorsKey==!t.anchors.isEmpty(),tile.file()+": an anchors key only when it holds anchors");
    for(var a:t.anchors){anchors++;
     check(a.x()>=0&&a.x()<t.size[0]&&a.y()>=0&&a.y()<t.size[1]&&a.z()>=0&&a.z()<t.size[2],tile.file()+": anchor inside its section "+a);
     check(a.cmd()!=null&&a.cmd().startsWith("gravesandglory:prop/")&&a.cmd().contains(" depth=")&&a.cmd().contains(" floor="),tile.file()+": anchor command "+a.cmd());
     int i=(a.z()*t.size[0]+a.x())*t.size[1]+a.y();int v=(short)(((t.stack[2*i]&255)<<8)|(t.stack[2*i+1]&255));
     check(v>=0&&t.palette.get(v).equals("minecraft:command_block"),tile.file()+": the block at anchor "+a+" is a command block, not "+(v<0?"nothing":t.palette.get(v)));
     String id=a.cmd().substring("gravesandglory:prop/".length(),a.cmd().indexOf(' '));ids.merge(id,1,Integer::sum);}}
   total.put(row.source(),anchors);
   System.out.println(row.source()+": "+anchors+" prop anchors "+ids);
   if(row.source().equals("ironhold_ip")){
    check(anchors==29,"ironhold_ip holds 29 prop anchors, not "+anchors);
    check(ids.equals(Map.of("tent_soldier_large",8,"tent_pavilion_large",1,"trebuchet",2,"supply_wagon",1,"supply_wagon_barrels",1,"supply_wagon_covered",1,"supply_wagon_hay",1,"supply_wagon_shot",1,"handcart_ore",1,"brazier",12)),"ironhold_ip prop ids "+ids);}
   else if(row.source().equals("kingsfield_cq")||row.source().equals("rivergate_ey"))check(anchors==0,row.source()+" has no props, not "+anchors);}
  check(total.containsKey("ironhold_ip"),"ironhold_ip is a bundled map");
  System.out.println("EngineMapPropsCheck PASS ("+count+" checks)");
 }
}
