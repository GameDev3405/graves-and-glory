package gg.gravesandglory;

/** The playable field of a map: its half sizes from its maps row (fieldHalfX, fieldHalfZ), centred on 0, 0. The barrier wall stands on its edge.
 *  The world the build writes stays rules worldHalfX by worldHalfZ for every map, and the margin north of rules arenaHalfZ still meets the camp
 *  (CampWorld.taper), so a smaller field (the new Kingsfield, 540 by 380) sits inside the same world. */
public final class Field {
    private Field(){}
    public static int halfX(int map){var r=row(map);return r==null||r.fieldHalfX()<=0?GravesAndGlory.R.arenaHalfX():r.fieldHalfX();}
    public static int halfZ(int map){var r=row(map);return r==null||r.fieldHalfZ()<=0?GravesAndGlory.R.arenaHalfZ():r.fieldHalfZ();}
    /** the field of the selected map (CampaignMaps.selected: the server's map, and the client's once its state says which) */
    public static int halfX(){return halfX(CampaignMaps.selected);}
    public static int halfZ(){return halfZ(CampaignMaps.selected);}
    /** inside the field less margin blocks on every side */
    public static boolean inside(double x,double z,double margin){return Math.abs(x)<=halfX()-margin&&Math.abs(z)<=halfZ()-margin;}
    private static Spec.MapsRow row(int map){return map>=0&&map<Spec.MAPS.length?Spec.MAPS[map]:null;}
}
