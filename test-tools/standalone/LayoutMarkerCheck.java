package gg.gravesandglory;

/** The map cache marker (LayoutMarker): a marker written before the arena margin was tapered (layout and map only), with a margin built to an older camp terrain, or without the landmarks hash of the map or with another one, is not current, so the arena of that map is built once more; the marker this build writes is current for its map and its landmarks and for no other. */
public final class LayoutMarkerCheck {
 static int count;static void check(boolean pass,String text){count++;if(!pass)throw new AssertionError(text);}
 public static void main(String[] args){
  String h="1a2b3c4d";
  String old2="{\"layout\":\"campaign-3\",\"map\":2}";
  check(!LayoutMarker.current(old2,2,h),"A marker from before the taper (layout and map, no margin revision) is not current");
  check(!LayoutMarker.text(2,h).equals(old2),"The marker written now differs from the one written before the taper");
  check(LayoutMarker.current(LayoutMarker.text(2,h),2,h),"The marker this build writes is current for its map");
  check(!LayoutMarker.current(LayoutMarker.text(2,h),3,h),"The marker of one map is not current for another");
  for(int map=0;map<5;map++)check(LayoutMarker.current(LayoutMarker.text(map,h),map,h)&&!LayoutMarker.current(old2.replace("2",""+map),map,h),"Map "+map+": the new marker is current, the old form of it is not");
  check(!LayoutMarker.current("{\"layout\":\""+LayoutMarker.LAYOUT+"\",\"map\":2,\"margin\":\"margin-old\",\"landmarks\":\""+h+"\"}",2,h),"Another margin revision is not current");
  check(!LayoutMarker.current("{\"layout\":\"campaign-2\",\"map\":2,\"margin\":\""+LayoutMarker.MARGIN+"\",\"landmarks\":\""+h+"\"}",2,h),"Another layout string is not current");
  check(!LayoutMarker.current("",2,h)&&!LayoutMarker.current("not json",2,h)&&!LayoutMarker.current("[]",2,h)&&!LayoutMarker.current("{}",2,h),"Unreadable markers are not current");
  check(LayoutMarker.text(0,h).contains(LayoutMarker.MARGIN),"The marker names the margin revision");
  check(LayoutMarker.MARGIN.contains(CampTerrain.REVISION),"The margin revision names the camp terrain revision (the margin meets the rim of that terrain)");
  check(!LayoutMarker.current("{\"layout\":\""+LayoutMarker.LAYOUT+"\",\"map\":2,\"margin\":\"margin-1\"}",2,h),"A margin built to the camp terrain before fix round 4 (margin-1, terrain-3) is not current: the map's arena is built once more");
  check(!LayoutMarker.current("{\"layout\":\""+LayoutMarker.LAYOUT+"\",\"map\":4,\"margin\":\"margin-1/terrain-4\"}",4,h),"A margin built to the camp terrain of fix round 4 (margin-1/terrain-4, the marker the upgrade run of re-grade 4 wrote) is not current: the map's arena is built once more");
  check(LayoutMarker.LAYOUT.equals("campaign-4"),"The layout string is campaign-4 (Phase 3 Task 8b: the spur roads of the supply houses change the road code, so every existing world builds each map once more)");
  for(int map=0;map<5;map++)check(!LayoutMarker.current("{\"layout\":\"campaign-3\",\"map\":"+map+",\"margin\":\""+LayoutMarker.MARGIN+"\",\"landmarks\":\""+h+"\"}",map,h)&&LayoutMarker.current(LayoutMarker.text(map,h),map,h),"Map "+map+": a campaign-3 marker with the current margin and hash is not current, the campaign-4 marker is");
  // Landmarks (Phase 3 Task 6): the hash of the map's landmarks is in the marker.
  String before="{\"layout\":\""+LayoutMarker.LAYOUT+"\",\"map\":0,\"margin\":\""+LayoutMarker.MARGIN+"\"}";
  check(!LayoutMarker.current(before,0,h),"A marker written before the landmarks (no landmarks field) is not current: the map is built once more with its landmarks");
  check(!LayoutMarker.current(LayoutMarker.text(0,"ffff0000"),0,h),"A marker with another landmarks hash (other rows, another structure file, other lines, or the harness salt) is not current");
  check(LayoutMarker.current(LayoutMarker.text(0,h),0,h),"A marker with the map's own landmarks hash is current");
  check(LayoutMarker.text(0,h).contains("\"landmarks\":\""+h+"\""),"The marker names the landmarks hash");
  System.out.println("PASS "+count+" layout marker scenarios");
 }
}
