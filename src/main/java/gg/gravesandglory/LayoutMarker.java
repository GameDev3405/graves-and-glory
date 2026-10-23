package gg.gravesandglory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** The map cache marker of a save (gravesandglory-layout.json): what Campaign.cached() compares to decide that the arena built in the world is the one to keep. */
final class LayoutMarker {
    private LayoutMarker(){}
    static final String LAYOUT="campaign-3";
    /** The revision of the arena's margin: the 16 columns north of the playable box (z -251 to -266) that CampWorld.taper pulls down to the rim of the camp, and the slope mix that Arena.buildColumn lays on them. It is in the marker beside the layout string, so a save made before the taper (no revision in its marker) builds that map's arena once more on its next load; bump MARGIN_BASE when Arena.height or Arena.buildColumn change for z < -arenaHalfZ.
     *  The margin meets the crest of the camp's rim (CampTerrain.rimTop) and lays the camp's slope mix, so the revision of the camp terrain is part of it: a save whose margin was built to another camp terrain (margin-1 was built to terrain-3) builds that map's arena once more, as the camp rebuilds its own terrain. */
    static final String MARGIN_BASE="margin-1";
    static final String MARGIN=MARGIN_BASE+"/"+CampTerrain.REVISION;

    static String text(int map){return "{\"layout\":\""+LAYOUT+"\",\"map\":"+map+",\"margin\":\""+MARGIN+"\"}";}

    /** True when the marker in json belongs to this build and this map: the layout string, the map, and the revision of the margin all agree. Anything unreadable is not current. */
    static boolean current(String json,int map){
        try{JsonObject j=JsonParser.parseString(json).getAsJsonObject();return LAYOUT.equals(j.get("layout").getAsString())&&j.get("map").getAsInt()==map&&MARGIN.equals(j.get("margin").getAsString());}
        catch(Exception e){return false;}
    }
}
