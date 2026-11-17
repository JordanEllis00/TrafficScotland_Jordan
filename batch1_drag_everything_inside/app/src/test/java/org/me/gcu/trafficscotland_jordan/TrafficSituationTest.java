package org.me.gcu.trafficscotland_jordan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.me.gcu.trafficscotland_jordan.TestData.byId;
import static org.me.gcu.trafficscotland_jordan.TestData.combined;

import org.junit.Test;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation.IconType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Model tests: icon category and road/keyword matching. */
public class TrafficSituationTest {

    @Test public void ut21_iconTypeForEveryTd1Situation() throws Exception {
        List<TrafficSituation> all = combined();
        String[][] expected = {
                {"SIT_U_1001", "ACCIDENT"}, {"SIT_U_1002", "CONGESTION"}, {"SIT_U_1003", "OBSTRUCTION"},
                {"SIT_U_1004", "CLOSURE"}, {"SIT_U_1005", "ACCIDENT"}, {"SIT_U_1006", "OBSTRUCTION"},
                {"SIT_U_1007", "WEATHER"}, {"SIT_U_1008", "EVENT"}, {"SIT_U_1009", "INFO"},
                {"SIT_R_2001", "ROADWORKS"}, {"SIT_R_2002", "ROADWORKS"}, {"SIT_R_2003", "ROADWORKS"},
                {"SIT_R_2004", "ROADWORKS"}, {"SIT_R_2005", "ROADWORKS"}, {"SIT_R_2006", "ROADWORKS"},
                {"SIT_R_2007", "ROADWORKS"}, {"SIT_R_2008", "ROADWORKS"}, {"SIT_R_2009", "ROADWORKS"}};
        assertEquals(18, all.size());
        for (String[] e : expected) {
            assertEquals(e[0], IconType.valueOf(e[1]), byId(all, e[0]).getIconType());
        }
    }

    @Test public void ut22_atLeastSixDifferentIconsInUse() throws Exception {
        Set<IconType> used = EnumSet.noneOf(IconType.class);
        for (TrafficSituation s : combined()) used.add(s.getIconType());
        assertEquals(8, used.size());
        assertTrue(used.size() >= 6);
    }

    @Test public void ut23_roadCodeSearchIsExact() throws Exception {
        List<TrafficSituation> all = combined();
        assertTrue(byId(all, "SIT_U_1002").matchesQuery("A9"));        // road A9
        assertTrue(byId(all, "SIT_R_2004").matchesQuery("a9"));        // lower case
        assertTrue(byId(all, "SIT_U_1007").matchesQuery(" A9 "));      // surrounding spaces
        assertFalse(byId(all, "SIT_U_1003").matchesQuery("A9"));       // A90 must not match A9
        assertFalse(byId(all, "SIT_U_1004").matchesQuery("A9"));       // A96 must not match A9
        assertFalse(byId(all, "SIT_R_2006").matchesQuery("A9"));       // A90 roadwork
        assertTrue(byId(all, "SIT_U_1003").matchesQuery("A90"));
    }

    @Test public void ut24_roadCodeFoundInTextWhenRoadFieldDiffers() throws Exception {
        // SIT_U_1008 has no road tag; its comment mentions "A8 and Edinburgh City Bypass"
        assertTrue(byId(combined(), "SIT_U_1008").matchesQuery("A8"));
        assertFalse(byId(combined(), "SIT_U_1008").matchesQuery("A9"));
    }

    @Test public void ut25_keywordSearchMatchesCommentText() throws Exception {
        List<TrafficSituation> all = combined();
        assertTrue(byId(all, "SIT_U_1006").matchesQuery("fort william"));
        assertTrue(byId(all, "SIT_R_2002").matchesQuery("Kilmarnock"));
        assertFalse(byId(all, "SIT_R_2002").matchesQuery("Fort William"));
    }

    @Test public void ut26_blankOrNullQueryMatchesEverything() throws Exception {
        for (TrafficSituation s : combined()) {
            assertTrue(s.matchesQuery(""));
            assertTrue(s.matchesQuery("   "));
            assertTrue(s.matchesQuery(null));
        }
    }
}
