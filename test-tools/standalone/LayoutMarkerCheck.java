package gg.gravesandglory;

/** The map cache marker (LayoutMarker): a marker written before the arena margin was tapered (layout and map only), or with a margin built to an older camp terrain, is not current, so the arena of that map is built once more; the marker this build writes is current for its map and for no other. */
public final class LayoutMarkerCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args){
  String old2="{\"layout\":\"campaign-3\",\"map\":2}";
  check(!LayoutMarker.current(old2,2),"A marker from before the taper (layout and map, no margin revision) is not current");
  check(!LayoutMarker.text(2).equals(old2),"The marker written now differs from the one written before the taper");
  check(LayoutMarker.current(LayoutMarker.text(2),2),"The marker this build writes is current for its map");
  check(!LayoutMarker.current(LayoutMarker.text(2),3),"The marker of one map is not current for another");
  for(int map=0;map<5;map++)check(LayoutMarker.current(LayoutMarker.text(map),map)&&!LayoutMarker.current(old2.replace("2",""+map),map),"Map "+map+": the new marker is current, the old form of it is not");
  check(!LayoutMarker.current("{\"layout\":\"campaign-3\",\"map\":2,\"margin\":\"margin-old\"}",2),"Another margin revision is not current");
  check(!LayoutMarker.current("{\"layout\":\"campaign-2\",\"map\":2,\"margin\":\""+LayoutMarker.MARGIN+"\"}",2),"Another layout string is not current");
  check(!LayoutMarker.current("",2)&&!LayoutMarker.current("not json",2)&&!LayoutMarker.current("[]",2)&&!LayoutMarker.current("{}",2),"Unreadable markers are not current");
  check(LayoutMarker.text(0).contains(LayoutMarker.MARGIN),"The marker names the margin revision");
  check(LayoutMarker.MARGIN.contains(CampTerrain.REVISION),"The margin revision names the camp terrain revision (the margin meets the rim of that terrain)");
  check(!LayoutMarker.current("{\"layout\":\"campaign-3\",\"map\":2,\"margin\":\"margin-1\"}",2),"A margin built to the camp terrain before fix round 4 (margin-1, terrain-3) is not current: the map's arena is built once more");
  check(!LayoutMarker.current("{\"layout\":\"campaign-3\",\"map\":4,\"margin\":\"margin-1/terrain-4\"}",4),"A margin built to the camp terrain of fix round 4 (margin-1/terrain-4, the marker the upgrade run of re-grade 4 wrote) is not current: the map's arena is built once more");
  check(LayoutMarker.LAYOUT.equals("campaign-3"),"The layout string is not bumped (only the margin revision changes)");
  System.out.println("PASS "+count+" layout marker scenarios");
 }
}
