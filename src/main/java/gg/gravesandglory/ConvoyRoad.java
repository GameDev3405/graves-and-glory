package gg.gravesandglory;

import java.util.ArrayList;
import java.util.List;

/** A Convoy road as data: the modeSites road rows of a map in order, and the floor the wagon rides on an engine map. Pure (no game classes),
 *  so ConvoyRoadCheck walks every bundled road with javac alone. */
public final class ConvoyRoad {
    private ConvoyRoad() {}
    /** y is the standing height the road takes from this node on, 0 for the level the road is already on. */
    public record Node(double x, double z, String name, double y) {}
    /** Distance between two samples of the floor profile. */
    public static final double STEP = 0.5;

    /** The map's road, start to destination; a node named "-" is a plain bend (name null), a named one a checkpoint. */
    public static List<Node> of(int map) {
        List<Spec.ModeSitesRow> rows = new ArrayList<>();
        for (var s : Spec.MODESITES) if (s.mode().equals("convoy") && s.kind().equals("road") && s.map() == map) rows.add(s);
        rows.sort(java.util.Comparator.comparingInt(Spec.ModeSitesRow::stage));
        List<Node> out = new ArrayList<>();
        for (var s : rows) out.add(new Node(s.x(), s.z(), s.name().equals("-") ? null : s.name(), s.y()));
        return out;
    }
    public static double[][] nodes(List<Node> road) {
        double[][] nodes = new double[road.size()][];
        for (int i = 0; i < nodes.length; i++) nodes[i] = new double[]{road.get(i).x(), road.get(i).z()};
        return nodes;
    }
    /** The road's floor profile on an engine map (ModeMath.roadFloors over the floors a troop can reach): the road keeps its own level under
     *  a roof, an arch or the canopy, where the top of the column is not the road. It starts on the floor a troop is put on at the first node. */
    public static int[] floors(EngineMap e, List<Node> road) {
        double[][] nodes = nodes(road);
        int[] own = new int[nodes.length];
        for (int i = 0; i < own.length; i++) own[i] = road.get(i).y() == 0 ? Integer.MIN_VALUE : (int) Math.floor(road.get(i).y()) - 1;
        int x = (int) Math.floor(nodes[0][0]), z = (int) Math.floor(nodes[0][1]);
        int[] at = e.nav.stand(x, z, e::solid, x + .5, z + .5, 0);
        return ModeMath.roadFloors(nodes, ModeMath.stations(nodes), own, STEP, at != null ? at[2] : e.solid(x, z), (cx, cz, y) -> e.nav.floorNear(cx, y, cz));
    }
}
