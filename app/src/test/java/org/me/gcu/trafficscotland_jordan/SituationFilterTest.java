package org.me.gcu.trafficscotland_jordan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.me.gcu.trafficscotland_jordan.TestData.byId;
import static org.me.gcu.trafficscotland_jordan.TestData.combined;
import static org.me.gcu.trafficscotland_jordan.TestData.ids;

import org.junit.Test;
import org.me.gcu.trafficscotland_jordan.model.SituationFilter;
import org.me.gcu.trafficscotland_jordan.model.SituationFilter.Category;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;

import java.util.List;

/**
 * Filter tests: the logic behind the category chips, road search and "within N km" search.
 * Expected id lists and distances come from an independent oracle (testing/expected.py: ellipsoid distance,
 * computed from the fixture coordinates, not from this code).
 */
public class SituationFilterTest {

    private static final double GLA_LAT = 55.8642, GLA_LON = -4.2518;
    private static final double EDI_LAT = 55.9533, EDI_LON = -3.1883;
    private static final double STI_LAT = 56.1165, STI_LON = -3.9369;

    private static List<TrafficSituation> run(Category c, String q, double km, double lat, double lon) throws Exception {
        return SituationFilter.apply(combined(), c, q, km, lat, lon);
    }

    @Test public void ut27_categoryFilterCounts() throws Exception {
        assertEquals(18, run(Category.ALL, "", -1, GLA_LAT, GLA_LON).size());
        assertEquals(9, run(Category.UNPLANNED_ONLY, "", -1, GLA_LAT, GLA_LON).size());
        assertEquals(9, run(Category.ROADWORKS_ONLY, "", -1, GLA_LAT, GLA_LON).size());
    }

    @Test public void ut28_roadSearchResults() throws Exception {
        assertEquals("U_1001,R_2001", ids(run(Category.ALL, "M8", -1, GLA_LAT, GLA_LON)));
        assertEquals("U_1002,U_1007,R_2004", ids(run(Category.ALL, "A9", -1, GLA_LAT, GLA_LON)));
        assertEquals("U_1003,R_2006", ids(run(Category.ALL, "A90", -1, GLA_LAT, GLA_LON)));
        assertEquals("U_1006,R_2005", ids(run(Category.ALL, "a82", -1, GLA_LAT, GLA_LON)));
        assertEquals("R_2003", ids(run(Category.ALL, "M74", -1, GLA_LAT, GLA_LON)));
        assertEquals("R_2007", ids(run(Category.ALL, "A720", -1, GLA_LAT, GLA_LON)));
        assertEquals("U_1006,R_2005", ids(run(Category.ALL, "Fort William", -1, GLA_LAT, GLA_LON)));
        assertEquals("R_2002", ids(run(Category.ALL, "Kilmarnock", -1, GLA_LAT, GLA_LON)));
        assertEquals("", ids(run(Category.ALL, "Z99", -1, GLA_LAT, GLA_LON)));
    }

    @Test public void ut29_searchCombinedWithCategory() throws Exception {
        assertEquals("R_2005", ids(run(Category.ROADWORKS_ONLY, "A82", -1, GLA_LAT, GLA_LON)));
        assertEquals("U_1006", ids(run(Category.UNPLANNED_ONLY, "A82", -1, GLA_LAT, GLA_LON)));
        assertEquals("R_2001", ids(run(Category.ROADWORKS_ONLY, "M8", -1, GLA_LAT, GLA_LON)));
    }

    @Test public void ut30_distanceFilterFromGlasgow() throws Exception {
        assertEquals("U_1001", ids(run(Category.ALL, "", 10, GLA_LAT, GLA_LON)));
        assertEquals("U_1001,U_1009", ids(run(Category.ALL, "", 25, GLA_LAT, GLA_LON)));
        assertEquals("U_1001,U_1009,R_2001,R_2002,U_1002", ids(run(Category.ALL, "", 50, GLA_LAT, GLA_LON)));
        assertEquals("U_1001,U_1009,R_2001,R_2002,U_1002,R_2003,U_1008,R_2007,R_2008,R_2004",
                ids(run(Category.ALL, "", 100, GLA_LAT, GLA_LON)));
    }

    @Test public void ut31_distanceFilterFromEdinburghAndStirling() throws Exception {
        assertEquals("U_1008,R_2007,R_2008,R_2001,R_2009", ids(run(Category.ALL, "", 50, EDI_LAT, EDI_LON)));
        assertEquals("U_1002,U_1009", ids(run(Category.ALL, "", 25, STI_LAT, STI_LON)));
    }

    @Test public void ut32_distancesMatchIndependentOracle() throws Exception {
        List<TrafficSituation> l = run(Category.ALL, "", 100, GLA_LAT, GLA_LON);
        String[] id = {"SIT_U_1001", "SIT_U_1009", "SIT_R_2001", "SIT_U_1002", "SIT_R_2003", "SIT_U_1008", "SIT_R_2004"};
        double[] km = {1.0, 16.8, 32.1, 36.7, 55.4, 63.7, 78.1};
        for (int i = 0; i < id.length; i++) {
            double tol = Math.max(0.3, km[i] * 0.005);   // haversine vs ellipsoid differ by < 0.5 %
            assertEquals(id[i], km[i], byId(l, id[i]).getDistanceKm(), tol);
        }
    }

    @Test public void ut33_distanceResultsSortedNearestFirst() throws Exception {
        List<TrafficSituation> l = run(Category.ALL, "", 100, GLA_LAT, GLA_LON);
        for (int i = 1; i < l.size(); i++) {
            assertTrue(l.get(i - 1).getDistanceKm() <= l.get(i).getDistanceKm());
        }
    }

    @Test public void ut34_noCoordinatesHandledWithAndWithoutRadius() throws Exception {
        List<TrafficSituation> any = run(Category.ALL, "", -1, GLA_LAT, GLA_LON);
        TrafficSituation none = byId(any, "SIT_U_1005");
        assertEquals(-1.0, none.getDistanceKm(), 1e-9);                       // shown, but without a distance label
        assertTrue(!ids(run(Category.ALL, "", 100, GLA_LAT, GLA_LON)).contains("U_1005"));   // hidden once a radius is set
    }

    @Test public void ut35_linearSituationUsesDisplayPointForDistance() throws Exception {
        // SIT_U_1004 (Inverness, linear): 182 km from Glasgow -> inside 200 km radius only if distance was computed
        List<TrafficSituation> l = SituationFilter.apply(combined(), Category.ALL, "", 200, GLA_LAT, GLA_LON);
        assertTrue(ids(l).contains("U_1004"));
        assertEquals(182.5, byId(l, "SIT_U_1004").getDistanceKm(), 1.0);
    }

    @Test public void ut36_combinedSearchAndDistance() throws Exception {
        assertEquals("", ids(run(Category.ROADWORKS_ONLY, "A82", 100, GLA_LAT, GLA_LON)));     // R_2005 is ~119 km away
        assertEquals("U_1002", ids(run(Category.ALL, "A9", 50, GLA_LAT, GLA_LON)));            // U_1007/R_2004 too far
    }

    @Test public void ut37_emptyAndNullInput() throws Exception {
        assertEquals(0, SituationFilter.apply(null, Category.ALL, "", -1, GLA_LAT, GLA_LON).size());
        assertEquals(0, SituationFilter.apply(new java.util.ArrayList<TrafficSituation>(), Category.ALL, "x", 10, GLA_LAT, GLA_LON).size());
    }

    @Test public void ut38_distanceFunctionKnownValue() {
        // Glasgow City Centre to Edinburgh City Centre is about 67 km in a straight line
        assertEquals(67.0, SituationFilter.distanceKm(GLA_LAT, GLA_LON, EDI_LAT, EDI_LON), 1.5);
        assertEquals(0.0, SituationFilter.distanceKm(GLA_LAT, GLA_LON, GLA_LAT, GLA_LON), 1e-9);
    }
}
