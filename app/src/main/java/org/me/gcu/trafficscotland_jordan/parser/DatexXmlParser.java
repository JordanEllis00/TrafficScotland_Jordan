package org.me.gcu.trafficscotland_jordan.parser;

import android.util.Log;

import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatexXmlParser {

    private static final String TAG = "DatexXmlParser";
    private static final Pattern ROAD_PATTERN = Pattern.compile("\\b([AM]\\d{1,3}(?:\\([M]\\))?)\\b");

    public static List<TrafficSituation> parseXmlString(String xmlData, TrafficSituation.FeedCategory category) {
        List<TrafficSituation> situations = new ArrayList<>();
        if (xmlData == null || xmlData.trim().isEmpty()) {
            return situations;
        }

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(false);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xmlData));

            int eventType = xpp.getEventType();
            TrafficSituation current = null;

            boolean inGeneralPublicComment = false;
            boolean inGroupOfLocations = false;
            boolean inLocationForDisplay = false;
            boolean inFromPoint = false;
            boolean inToPoint = false;

            Double tempLat = null;
            Double tempLon = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String tagName = xpp.getName();
                String localName = stripPrefix(tagName);

                if (eventType == XmlPullParser.START_TAG) {
                    if ("situation".equalsIgnoreCase(localName)) {
                        current = new TrafficSituation();
                        current.setFeedCategory(category);
                        String sitId = getAttributeByLocalName(xpp, "id");
                        if (sitId != null) {
                            current.setSituationId(sitId);
                        }
                    } else if (current != null) {
                        if ("situationRecord".equalsIgnoreCase(localName)) {
                            String typeAttr = getAttributeByLocalName(xpp, "type");
                            String recId = getAttributeByLocalName(xpp, "id");
                            if (typeAttr != null) current.setXsiType(typeAttr);
                            if (recId != null) current.setRecordId(recId);
                        } else if ("generalPublicComment".equalsIgnoreCase(localName)) {
                            inGeneralPublicComment = true;
                        } else if ("groupOfLocations".equalsIgnoreCase(localName)) {
                            inGroupOfLocations = true;
                        } else if ("locationForDisplay".equalsIgnoreCase(localName)) {
                            inLocationForDisplay = true;
                            tempLat = null;
                            tempLon = null;
                        } else if ("from".equalsIgnoreCase(localName) || "linearCoordinatesStartPoint".equalsIgnoreCase(localName)) {
                            inFromPoint = true;
                            tempLat = null;
                            tempLon = null;
                        } else if ("to".equalsIgnoreCase(localName) || "linearCoordinatesEndPoint".equalsIgnoreCase(localName)) {
                            inToPoint = true;
                            tempLat = null;
                            tempLon = null;
                        } else if ("pointCoordinates".equalsIgnoreCase(localName)) {
                            tempLat = null;
                            tempLon = null;
                        } else if ("value".equalsIgnoreCase(localName)) {
                            String text = safeNextText(xpp);
                            if (inGeneralPublicComment && !text.isEmpty()) {
                                if (current.getGeneralPublicComment().isEmpty()) {
                                    current.setGeneralPublicComment(text);
                                } else {
                                    current.setGeneralPublicComment(current.getGeneralPublicComment() + "\n" + text);
                                }
                            } else if (inGroupOfLocations && !text.isEmpty() && current.getLocationDescriptor().isEmpty()) {
                                current.setLocationDescriptor(text);
                            }
                        } else if ("overallStartTime".equalsIgnoreCase(localName)) {
                            current.setOverallStartTime(safeNextText(xpp));
                        } else if ("overallEndTime".equalsIgnoreCase(localName)) {
                            current.setOverallEndTime(safeNextText(xpp));
                        } else if ("roadNumber".equalsIgnoreCase(localName) || "roadName".equalsIgnoreCase(localName)) {
                            String road = safeNextText(xpp);
                            if (!road.isEmpty()) {
                                current.setRoadName(road);
                            }
                        } else if ("causeType".equalsIgnoreCase(localName)) {
                            current.setCauseType(safeNextText(xpp));
                        } else if ("severity".equalsIgnoreCase(localName)) {
                            current.setSeverity(safeNextText(xpp));
                        } else if ("trafficConstrictionType".equalsIgnoreCase(localName)
                                || "roadMaintenanceType".equalsIgnoreCase(localName)
                                || "operatorActionStatus".equalsIgnoreCase(localName)) {
                            String detail = safeNextText(xpp);
                            if (!detail.isEmpty() && current.getImpactSummary().isEmpty()) {
                                current.setImpactSummary(detail);
                            }
                        } else if ("latitude".equalsIgnoreCase(localName)) {
                            tempLat = parseDoubleSafe(safeNextText(xpp));
                        } else if ("longitude".equalsIgnoreCase(localName)) {
                            tempLon = parseDoubleSafe(safeNextText(xpp));
                            if (tempLat != null && tempLon != null) {
                                if (inLocationForDisplay) {
                                    current.setDisplayCoordinates(tempLat, tempLon);
                                } else if (inFromPoint) {
                                    current.setFromCoordinates(tempLat, tempLon);
                                } else if (inToPoint) {
                                    current.setToCoordinates(tempLat, tempLon);
                                } else if (!current.hasDisplayPoint()) {
                                    current.setDisplayCoordinates(tempLat, tempLon);
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if ("generalPublicComment".equalsIgnoreCase(localName)) {
                        inGeneralPublicComment = false;
                    } else if ("groupOfLocations".equalsIgnoreCase(localName)) {
                        inGroupOfLocations = false;
                    } else if ("locationForDisplay".equalsIgnoreCase(localName)) {
                        inLocationForDisplay = false;
                    } else if ("from".equalsIgnoreCase(localName) || "linearCoordinatesStartPoint".equalsIgnoreCase(localName)) {
                        inFromPoint = false;
                    } else if ("to".equalsIgnoreCase(localName) || "linearCoordinatesEndPoint".equalsIgnoreCase(localName)) {
                        inToPoint = false;
                    } else if ("situation".equalsIgnoreCase(localName) && current != null) {
                        if (current.getRoadName().isEmpty()) {
                            String detectedRoad = extractRoadCode(current.getGeneralPublicComment());
                            if (detectedRoad.isEmpty()) {
                                detectedRoad = extractRoadCode(current.getLocationDescriptor());
                            }
                            current.setRoadName(detectedRoad);
                        }
                        if (current.getGeneralPublicComment().isEmpty()) {
                            current.setGeneralPublicComment("No public comment details provided by Traffic Scotland.");
                        }
                        situations.add(current);
                        current = null;
                    }
                }

                eventType = xpp.next();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing DATEX II XML string with XmlPullParser", e);
        }

        return situations;
    }

    /**
     * True when the text looks like a complete DATEX II document (closing root element present).
     * Used so that a truncated download is never written over a good cached copy.
     */
    public static boolean isCompleteDocument(String xmlData) {
        if (xmlData == null) return false;
        String t = xmlData.trim();
        return t.startsWith("<") && t.endsWith("d2LogicalModel>");
    }

    private static String stripPrefix(String name) {
        if (name == null) return "";
        int idx = name.indexOf(':');
        return idx >= 0 ? name.substring(idx + 1) : name;
    }

    private static String getAttributeByLocalName(XmlPullParser xpp, String targetLocalName) {
        int count = xpp.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String attrName = stripPrefix(xpp.getAttributeName(i));
            if (targetLocalName.equalsIgnoreCase(attrName)) {
                return xpp.getAttributeValue(i);
            }
        }
        return null;
    }

    private static String safeNextText(XmlPullParser xpp) {
        try {
            String text = xpp.nextText();
            return text != null ? text.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private static Double parseDoubleSafe(String val) {
        if (val == null || val.isEmpty()) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractRoadCode(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher m = ROAD_PATTERN.matcher(text.toUpperCase());
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }
}
