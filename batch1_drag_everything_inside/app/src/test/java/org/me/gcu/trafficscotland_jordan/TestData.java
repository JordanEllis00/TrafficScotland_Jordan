package org.me.gcu.trafficscotland_jordan;

import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation.FeedCategory;
import org.me.gcu.trafficscotland_jordan.parser.DatexXmlParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/** Loads the shipped sample feeds (TD-0) and the extended test fixtures (TD-1, in src/test/resources). */
final class TestData {
    private TestData() { }

    static String raw(String name) throws Exception {
        return new String(Files.readAllBytes(new File("src/main/res/raw/" + name).toPath()), StandardCharsets.UTF_8);
    }

    static String fixture(String name) throws Exception {
        try (InputStream is = TestData.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) throw new IllegalStateException("Missing test resource " + name);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = is.read(b)) > 0) bo.write(b, 0, n);
            return new String(bo.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    static List<TrafficSituation> unplanned() throws Exception {
        return DatexXmlParser.parseXmlString(fixture("TD1_unplanned.xml"), FeedCategory.UNPLANNED_EVENT);
    }

    static List<TrafficSituation> roadworks() throws Exception {
        return DatexXmlParser.parseXmlString(fixture("TD1_roadworks.xml"), FeedCategory.CURRENT_ROADWORK);
    }

    /** TD-1 unplanned followed by TD-1 roadworks, exactly as TrafficRepository combines the two feeds. */
    static List<TrafficSituation> combined() throws Exception {
        List<TrafficSituation> all = new ArrayList<>(unplanned());
        all.addAll(roadworks());
        return all;
    }

    static TrafficSituation byId(List<TrafficSituation> l, String id) {
        for (TrafficSituation s : l) if (id.equals(s.getSituationId())) return s;
        throw new AssertionError("No situation " + id);
    }

    static String ids(List<TrafficSituation> l) {
        StringBuilder sb = new StringBuilder();
        for (TrafficSituation s : l) sb.append(sb.length() == 0 ? "" : ",").append(s.getSituationId().replace("SIT_", ""));
        return sb.toString();
    }
}
