package org.me.gcu.trafficscotland_jordan;

import org.me.gcu.trafficscotland_jordan.model.SituationFilter;
import org.me.gcu.trafficscotland_jordan.model.SituationFilter.Category;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import java.util.*;

/** Runs the app's real filter code over TD-1 for the documented scenarios and prints the actual results (JSON lines). */
public class FilterDump {
    static final double[][] REF = {{55.8642,-4.2518},{55.9533,-3.1883},{56.1165,-3.9369}};
    static final String[] REFN = {"Glasgow","Edinburgh","Stirling"};
    static String list(List<TrafficSituation> l, boolean dist) {
        StringBuilder sb = new StringBuilder("[");
        for (TrafficSituation s : l) {
            if (sb.length() > 1) sb.append(",");
            sb.append("\"").append(s.getSituationId().replace("SIT_", "")).append(dist && s.getDistanceKm() >= 0 ? String.format(Locale.UK, " %.1f", s.getDistanceKm()) : "").append("\"");
        }
        return sb.append("]").toString();
    }
    public static void main(String[] a) throws Exception {
        List<TrafficSituation> all = TestData.combined();
        for (String q : new String[]{"M8","A9","a82","M74","A720","A8","A90","Fort William","Kilmarnock","Z99"})
            System.out.println("{\"kind\":\"search\",\"query\":\"" + q + "\",\"result\":" + list(SituationFilter.apply(all, Category.ALL, q, -1, 55.8642, -4.2518), false) + "}");
        int[][] pairs = {{0,10},{0,25},{0,50},{0,100},{1,50},{2,25}};
        for (int[] p : pairs)
            System.out.println("{\"kind\":\"distance\",\"ref\":\"" + REFN[p[0]] + "\",\"radius\":" + p[1] + ",\"result\":" + list(SituationFilter.apply(all, Category.ALL, "", p[1], REF[p[0]][0], REF[p[0]][1]), true) + "}");
        for (Category c : Category.values())
            System.out.println("{\"kind\":\"category\",\"category\":\"" + c + "\",\"count\":" + SituationFilter.apply(all, c, "", -1, 55.8642, -4.2518).size() + "}");
        System.out.println("{\"kind\":\"combo\",\"desc\":\"Roadworks only + A82\",\"result\":" + list(SituationFilter.apply(all, Category.ROADWORKS_ONLY, "A82", -1, 55.8642, -4.2518), false) + "}");
        System.out.println("{\"kind\":\"combo\",\"desc\":\"Roadworks only + A82 + within 100 km of Glasgow\",\"result\":" + list(SituationFilter.apply(all, Category.ROADWORKS_ONLY, "A82", 100, 55.8642, -4.2518), true) + "}");
        System.out.println("{\"kind\":\"combo\",\"desc\":\"A9 within 50 km of Glasgow\",\"result\":" + list(SituationFilter.apply(all, Category.ALL, "A9", 50, 55.8642, -4.2518), true) + "}");
    }
}
