package org.me.gcu.trafficscotland_jordan;

import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.me.gcu.trafficscotland_jordan.parser.DatexXmlParser;
import java.nio.file.*;
import java.util.*;

/** Prints every parsed object plus the exact display strings the app composes (one JSON line each).
 *  Usage: Dump <xmlfile> UNPLANNED|ROADWORKS */
public class Dump {
    static String q(String s){ return "\"" + s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n") + "\""; }
    public static void main(String[] a) throws Exception {
        String xml = new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8");
        TrafficSituation.FeedCategory cat = a[1].startsWith("U") ? TrafficSituation.FeedCategory.UNPLANNED_EVENT : TrafficSituation.FeedCategory.CURRENT_ROADWORK;
        for (TrafficSituation s : DatexXmlParser.parseXmlString(xml, cat)) {
            // --- composition copied from SituationAdapter.onBindViewHolder ---
            String title = s.getFormattedType();
            if (!s.getRoadName().isEmpty()) title = s.getRoadName() + " • " + title;
            // --- composition copied from SituationDetailBottomSheet.onViewCreated ---
            String roadText = s.getRoadName().isEmpty() ? "Scottish Trunk Road Network" : s.getRoadName();
            if (!s.getLocationDescriptor().isEmpty()) roadText += " (" + s.getLocationDescriptor() + ")";
            String coords;
            if (s.hasLinearPoints()) coords = String.format(Locale.UK, "Linear Segment: From (%.4f, %.4f) → To (%.4f, %.4f)", s.getFromLatitude(), s.getFromLongitude(), s.getToLatitude(), s.getToLongitude());
            else if (s.hasDisplayPoint()) coords = String.format(Locale.UK, "Coordinates: %.4f, %.4f", s.getLatitude(), s.getLongitude());
            else coords = "Coordinates: Not provided in feed";
            StringBuilder meta = new StringBuilder("Situation ID: " + (s.getSituationId().isEmpty() ? "N/A" : s.getSituationId()));
            if (!s.getImpactSummary().isEmpty()) meta.append(" • Impact: ").append(s.getImpactSummary());
            if (!s.getSeverity().isEmpty()) meta.append(" • Severity: ").append(s.getSeverity());
            StringBuilder o = new StringBuilder("{");
            o.append("\"cat\":").append(q(cat.name())).append(",\"sid\":").append(q(s.getSituationId()))
             .append(",\"rid\":").append(q(s.getRecordId())).append(",\"type\":").append(q(s.getFormattedType()))
             .append(",\"road\":").append(q(s.getRoadName())).append(",\"comment\":").append(q(s.getGeneralPublicComment()))
             .append(",\"startRaw\":").append(q(s.getOverallStartTime())).append(",\"endRaw\":").append(q(s.getOverallEndTime()))
             .append(",\"startFmt\":").append(q(s.getFormattedStartTime())).append(",\"endFmt\":").append(q(s.getFormattedEndTime()))
             .append(",\"hasPoint\":").append(s.hasDisplayPoint()).append(",\"lat\":").append(s.getLatitude()).append(",\"lon\":").append(s.getLongitude())
             .append(",\"hasLinear\":").append(s.hasLinearPoints())
             .append(",\"fLat\":").append(s.getFromLatitude()).append(",\"fLon\":").append(s.getFromLongitude())
             .append(",\"tLat\":").append(s.getToLatitude()).append(",\"tLon\":").append(s.getToLongitude())
             .append(",\"impact\":").append(q(s.getImpactSummary())).append(",\"severity\":").append(q(s.getSeverity()))
             .append(",\"days\":").append(s.getDurationDays()).append(",\"badge\":").append(q(s.getDurationBadgeText()))
             .append(",\"colour\":").append(q(s.getDurationSeverity().name())).append(",\"icon\":").append(q(s.getIconType().name()))
             .append(",\"uiListTitle\":").append(q(title)).append(",\"uiRoadLine\":").append(q("Road / Location: " + roadText))
             .append(",\"uiStart\":").append(q("Start Time: " + s.getFormattedStartTime())).append(",\"uiEnd\":").append(q("End Time: " + s.getFormattedEndTime()))
             .append(",\"uiCoords\":").append(q(coords)).append(",\"uiMeta\":").append(q(meta.toString())).append("}");
            System.out.println(o);
        }
    }
}
