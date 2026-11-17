package org.me.gcu.trafficscotland_jordan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.me.gcu.trafficscotland_jordan.TestData.byId;
import static org.me.gcu.trafficscotland_jordan.TestData.fixture;
import static org.me.gcu.trafficscotland_jordan.TestData.raw;
import static org.me.gcu.trafficscotland_jordan.TestData.roadworks;
import static org.me.gcu.trafficscotland_jordan.TestData.unplanned;

import org.junit.Test;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation.DurationSeverity;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation.FeedCategory;
import org.me.gcu.trafficscotland_jordan.parser.DatexXmlParser;

import java.util.List;

/** Parser tests: XML text in, TrafficSituation objects out. */
public class DatexXmlParserTest {

    @Test public void ut01_sampleUnplanned_fieldsExtracted() throws Exception {
        List<TrafficSituation> l = DatexXmlParser.parseXmlString(raw("sample_unplanned.xml"), FeedCategory.UNPLANNED_EVENT);
        assertEquals(1, l.size());
        TrafficSituation s = l.get(0);
        assertEquals("SIT_U_1001", s.getSituationId());
        assertEquals("REC_U_1001", s.getRecordId());
        assertEquals("Accident", s.getFormattedType());
        assertEquals("M8", s.getRoadName());
        assertTrue(s.getGeneralPublicComment().startsWith("M8 Eastbound Junction 15 (Townhead)"));
        assertEquals(55.8674, s.getLatitude(), 1e-9);
        assertEquals(-4.2361, s.getLongitude(), 1e-9);
        assertEquals("12 Nov 2026, 13:15", s.getFormattedStartTime());
        assertEquals("12 Nov 2026, 16:30", s.getFormattedEndTime());
        assertEquals("LIVE INCIDENT", s.getDurationBadgeText());
    }

    @Test public void ut02_sampleRoadworks_linearAndDuration() throws Exception {
        List<TrafficSituation> l = DatexXmlParser.parseXmlString(raw("sample_roadworks.xml"), FeedCategory.CURRENT_ROADWORK);
        assertEquals(1, l.size());
        TrafficSituation s = l.get(0);
        assertEquals("Maintenance Works", s.getFormattedType());
        assertEquals("M8", s.getRoadName());
        assertTrue(s.hasLinearPoints());
        assertEquals(55.8569, s.getFromLatitude(), 1e-9);
        assertEquals(-4.3742, s.getFromLongitude(), 1e-9);
        assertEquals(55.8641, s.getToLatitude(), 1e-9);
        assertEquals(-4.4258, s.getToLongitude(), 1e-9);
        assertEquals(14L, s.getDurationDays());
        assertEquals(DurationSeverity.LONG, s.getDurationSeverity());
        assertEquals("14 DAYS (LONG)", s.getDurationBadgeText());
    }

    @Test public void ut03_td1Unplanned_countsAllSituations() throws Exception {
        assertEquals(9, unplanned().size());
    }

    @Test public void ut04_td1Roadworks_prefixedNamespaceParsed() throws Exception {
        assertEquals(9, roadworks().size());
    }

    @Test public void ut05_entityDecodedInComment() throws Exception {
        assertTrue(byId(unplanned(), "SIT_U_1001").getGeneralPublicComment().contains("Junction 15 (Townhead) & Junction 16"));
    }

    @Test public void ut06_multipleValuesJoinedWithNewline() throws Exception {
        assertEquals("A82 Fort William - fallen tree blocking northbound carriageway.\nRecovery vehicle en route.",
                byId(unplanned(), "SIT_U_1006").getGeneralPublicComment());
    }

    @Test public void ut07_missingCommentGetsDefaultText() throws Exception {
        assertEquals("No public comment details provided by Traffic Scotland.",
                byId(unplanned(), "SIT_U_1005").getGeneralPublicComment());
    }

    @Test public void ut08_roadNameFromTagOrDerivedFromText() throws Exception {
        List<TrafficSituation> u = unplanned();
        assertEquals("M8", byId(u, "SIT_U_1001").getRoadName());               // <roadNumber> tag
        assertEquals("A9", byId(u, "SIT_U_1002").getRoadName());               // regex on comment
        assertEquals("A720", byId(roadworks(), "SIT_R_2007").getRoadName());   // regex on comment
        assertEquals("", byId(u, "SIT_U_1005").getRoadName());                 // nothing to derive
    }

    @Test public void ut09_pointOnlyAndNoCoordinates() throws Exception {
        List<TrafficSituation> u = unplanned();
        assertTrue(byId(u, "SIT_U_1002").hasDisplayPoint());
        assertFalse(byId(u, "SIT_U_1002").hasLinearPoints());
        assertFalse(byId(u, "SIT_U_1005").hasAnyCoordinates());
    }

    @Test public void ut10_linearCoordinatesExtracted() throws Exception {
        TrafficSituation s = byId(unplanned(), "SIT_U_1004");
        assertTrue(s.hasLinearPoints());
        assertEquals(57.4880, s.getFromLatitude(), 1e-9);
        assertEquals(-4.0900, s.getFromLongitude(), 1e-9);
        assertEquals(57.5120, s.getToLatitude(), 1e-9);
        assertEquals(-4.0300, s.getToLongitude(), 1e-9);
    }

    @Test public void ut11_impactAndSeverityExtracted() throws Exception {
        TrafficSituation s = byId(unplanned(), "SIT_U_1001");
        assertEquals("laneBlocked", s.getImpactSummary());
        assertEquals("high", s.getSeverity());
        assertEquals("", byId(unplanned(), "SIT_U_1005").getImpactSummary());
    }

    @Test public void ut12_typeNameFormatting() throws Exception {
        assertEquals("Road Or Carriageway Or Lane Management", byId(unplanned(), "SIT_U_1004").getFormattedType());
        assertEquals("Abnormal Traffic", byId(unplanned(), "SIT_U_1002").getFormattedType());
        assertEquals("Maintenance Works", byId(roadworks(), "SIT_R_2001").getFormattedType());   // d2: prefix removed
        assertEquals("Construction Works", byId(roadworks(), "SIT_R_2003").getFormattedType());
    }

    @Test public void ut13_durationClassificationAndBoundaries() throws Exception {
        List<TrafficSituation> r = roadworks();
        assertEquals(DurationSeverity.SHORT,  byId(r, "SIT_R_2001").getDurationSeverity());  // 10 h  -> 1 day
        assertEquals(1L, byId(r, "SIT_R_2001").getDurationDays());
        assertEquals(DurationSeverity.SHORT,  byId(r, "SIT_R_2006").getDurationSeverity());  // 48 h  -> 2 days
        assertEquals(2L, byId(r, "SIT_R_2006").getDurationDays());
        assertEquals(DurationSeverity.MEDIUM, byId(r, "SIT_R_2009").getDurationSeverity());  // 49 h  -> 3 days
        assertEquals(3L, byId(r, "SIT_R_2009").getDurationDays());
        assertEquals(DurationSeverity.MEDIUM, byId(r, "SIT_R_2002").getDurationSeverity());  // 5 days
        assertEquals(DurationSeverity.MEDIUM, byId(r, "SIT_R_2005").getDurationSeverity());  // 168 h -> 7 days
        assertEquals(7L, byId(r, "SIT_R_2005").getDurationDays());
        assertEquals(DurationSeverity.LONG,   byId(r, "SIT_R_2008").getDurationSeverity());  // 192 h -> 8 days
        assertEquals(8L, byId(r, "SIT_R_2008").getDurationDays());
        assertEquals(DurationSeverity.LONG,   byId(r, "SIT_R_2003").getDurationSeverity());  // 30 days
        assertEquals(DurationSeverity.LONG,   byId(r, "SIT_R_2004").getDurationSeverity());  // 61 days
    }

    @Test public void ut14_badgeText() throws Exception {
        List<TrafficSituation> r = roadworks();
        assertEquals("1 DAY (SHORT)",   byId(r, "SIT_R_2001").getDurationBadgeText());
        assertEquals("2 DAYS (SHORT)",  byId(r, "SIT_R_2006").getDurationBadgeText());
        assertEquals("5 DAYS (MEDIUM)", byId(r, "SIT_R_2002").getDurationBadgeText());
        assertEquals("8 DAYS (LONG)",   byId(r, "SIT_R_2008").getDurationBadgeText());
        assertEquals("30 DAYS (LONG)",  byId(r, "SIT_R_2003").getDurationBadgeText());
    }

    @Test public void ut15_missingEndTimeHandled() throws Exception {
        TrafficSituation s = byId(roadworks(), "SIT_R_2007");
        assertEquals(-1L, s.getDurationDays());
        assertEquals(DurationSeverity.UNKNOWN, s.getDurationSeverity());   // grey, not misleadingly "short"
        assertEquals("DURATION UNKNOWN", s.getDurationBadgeText());
        assertEquals("Not specified", s.getFormattedEndTime());
        assertEquals("10 Nov 2026, 00:00", s.getFormattedStartTime());
    }

    @Test public void ut16_unplannedAlwaysLiveCategory() throws Exception {
        for (TrafficSituation s : unplanned()) {
            assertEquals(DurationSeverity.UNPLANNED, s.getDurationSeverity());
            assertEquals("LIVE INCIDENT", s.getDurationBadgeText());
        }
    }

    @Test public void ut17_malformedXmlDoesNotCrashAndKeepsCompleteRecords() throws Exception {
        List<TrafficSituation> l = DatexXmlParser.parseXmlString(fixture("malformed.xml"), FeedCategory.UNPLANNED_EVENT);
        assertEquals(1, l.size());
        assertEquals("SIT_U_1001", l.get(0).getSituationId());
    }

    @Test public void ut18_emptyOrNullInputReturnsEmptyList() throws Exception {
        assertEquals(0, DatexXmlParser.parseXmlString("", FeedCategory.UNPLANNED_EVENT).size());
        assertEquals(0, DatexXmlParser.parseXmlString("   \n ", FeedCategory.UNPLANNED_EVENT).size());
        assertEquals(0, DatexXmlParser.parseXmlString(null, FeedCategory.CURRENT_ROADWORK).size());
    }

    @Test public void ut19_feedCategoryAssigned() throws Exception {
        for (TrafficSituation s : unplanned()) assertEquals(FeedCategory.UNPLANNED_EVENT, s.getFeedCategory());
        for (TrafficSituation s : roadworks()) assertEquals(FeedCategory.CURRENT_ROADWORK, s.getFeedCategory());
    }

    @Test public void ut20_completeDocumentCheckProtectsCache() throws Exception {
        assertTrue(DatexXmlParser.isCompleteDocument(fixture("TD1_unplanned.xml")));      // default namespace
        assertTrue(DatexXmlParser.isCompleteDocument(fixture("TD1_roadworks.xml")));      // d2: prefix
        assertTrue(DatexXmlParser.isCompleteDocument(raw("sample_unplanned.xml")));
        assertFalse(DatexXmlParser.isCompleteDocument(fixture("malformed.xml")));         // truncated download
        assertFalse(DatexXmlParser.isCompleteDocument(""));
        assertFalse(DatexXmlParser.isCompleteDocument(null));
        assertFalse(DatexXmlParser.isCompleteDocument("<html>401 Unauthorized</html>"));
    }
}
