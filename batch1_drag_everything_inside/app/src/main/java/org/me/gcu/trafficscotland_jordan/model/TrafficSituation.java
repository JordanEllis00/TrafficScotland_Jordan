package org.me.gcu.trafficscotland_jordan.model;

import java.io.Serializable;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

public class TrafficSituation implements Serializable {

    public enum FeedCategory {
        UNPLANNED_EVENT,
        CURRENT_ROADWORK
    }

    public enum DurationSeverity {
        SHORT,      // <= 2 days (Green)
        MEDIUM,     // 3 - 7 days (Amber)
        LONG,       // > 7 days (Red)
        UNPLANNED,  // Live unplanned event (Blue)
        UNKNOWN     // Roadworks without a usable start/end time (Grey)
    }

    /** Visual category used to choose the icon for a situation (kept free of Android types so it can be unit tested). */
    public enum IconType {
        ACCIDENT, CONGESTION, CLOSURE, OBSTRUCTION, ROADWORKS, WEATHER, EVENT, INFO
    }

    private static final Pattern ROAD_CODE = Pattern.compile("^[AM]\\d{1,3}$");

    private String situationId = "";
    private String recordId = "";
    private String xsiType = "";
    private String generalPublicComment = "";
    private String roadName = "";
    private String locationDescriptor = "";
    private String causeType = "";
    private String impactSummary = "";
    private String severity = "";
    private String overallStartTime = "";
    private String overallEndTime = "";
    private FeedCategory feedCategory = FeedCategory.UNPLANNED_EVENT;

    private double latitude = 0.0;
    private double longitude = 0.0;
    private boolean hasDisplayPoint = false;

    private double fromLatitude = 0.0;
    private double fromLongitude = 0.0;
    private double toLatitude = 0.0;
    private double toLongitude = 0.0;
    private boolean hasLinearPoints = false;

    private double distanceKm = -1.0;

    public String getSituationId() { return situationId; }
    public void setSituationId(String situationId) { this.situationId = situationId != null ? situationId.trim() : ""; }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId != null ? recordId.trim() : ""; }

    public String getXsiType() { return xsiType; }
    public void setXsiType(String xsiType) { this.xsiType = xsiType != null ? xsiType.trim() : ""; }

    public String getGeneralPublicComment() { return generalPublicComment; }
    public void setGeneralPublicComment(String generalPublicComment) {
        this.generalPublicComment = generalPublicComment != null ? generalPublicComment.trim() : "";
    }

    public String getRoadName() { return roadName; }
    public void setRoadName(String roadName) { this.roadName = roadName != null ? roadName.trim() : ""; }

    public String getLocationDescriptor() { return locationDescriptor; }
    public void setLocationDescriptor(String locationDescriptor) {
        this.locationDescriptor = locationDescriptor != null ? locationDescriptor.trim() : "";
    }

    public String getCauseType() { return causeType; }
    public void setCauseType(String causeType) { this.causeType = causeType != null ? causeType.trim() : ""; }

    public String getImpactSummary() { return impactSummary; }
    public void setImpactSummary(String impactSummary) {
        this.impactSummary = impactSummary != null ? impactSummary.trim() : "";
    }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity != null ? severity.trim() : ""; }

    public String getOverallStartTime() { return overallStartTime; }
    public void setOverallStartTime(String overallStartTime) {
        this.overallStartTime = overallStartTime != null ? overallStartTime.trim() : "";
    }

    public String getOverallEndTime() { return overallEndTime; }
    public void setOverallEndTime(String overallEndTime) {
        this.overallEndTime = overallEndTime != null ? overallEndTime.trim() : "";
    }

    public FeedCategory getFeedCategory() { return feedCategory; }
    public void setFeedCategory(FeedCategory feedCategory) { this.feedCategory = feedCategory; }

    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean hasDisplayPoint() { return hasDisplayPoint; }

    public void setDisplayCoordinates(double lat, double lon) {
        if (lat != 0.0 || lon != 0.0) {
            this.latitude = lat;
            this.longitude = lon;
            this.hasDisplayPoint = true;
        }
    }

    public double getFromLatitude() { return fromLatitude; }
    public double getFromLongitude() { return fromLongitude; }
    public double getToLatitude() { return toLatitude; }
    public double getToLongitude() { return toLongitude; }
    public boolean hasLinearPoints() { return hasLinearPoints; }

    public void setFromCoordinates(double lat, double lon) {
        if (lat != 0.0 || lon != 0.0) {
            this.fromLatitude = lat;
            this.fromLongitude = lon;
            if (!this.hasDisplayPoint) {
                setDisplayCoordinates(lat, lon);
            }
            checkLinearComplete();
        }
    }

    public void setToCoordinates(double lat, double lon) {
        if (lat != 0.0 || lon != 0.0) {
            this.toLatitude = lat;
            this.toLongitude = lon;
            checkLinearComplete();
        }
    }

    private void checkLinearComplete() {
        if ((fromLatitude != 0.0 || fromLongitude != 0.0) && (toLatitude != 0.0 || toLongitude != 0.0)) {
            this.hasLinearPoints = true;
        }
    }

    public boolean hasAnyCoordinates() {
        return hasDisplayPoint || hasLinearPoints;
    }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getFormattedType() {
        if (xsiType == null || xsiType.isEmpty()) {
            return feedCategory == FeedCategory.CURRENT_ROADWORK ? "Planned Roadworks" : "Unplanned Incident";
        }
        String clean = xsiType.contains(":") ? xsiType.substring(xsiType.indexOf(':') + 1) : xsiType;
        return clean.replaceAll("([a-z])([A-Z])", "$1 $2");
    }

    public long getDurationDays() {
        if (overallStartTime.isEmpty() || overallEndTime.isEmpty()) {
            return -1;
        }
        try {
            ZonedDateTime start = parseDateTime(overallStartTime);
            ZonedDateTime end = parseDateTime(overallEndTime);
            if (start == null || end == null) return -1;
            long hours = Duration.between(start, end).toHours();
            return Math.max(1, (long) Math.ceil(hours / 24.0));
        } catch (Exception e) {
            return -1;
        }
    }

    public DurationSeverity getDurationSeverity() {
        if (feedCategory == FeedCategory.UNPLANNED_EVENT) {
            return DurationSeverity.UNPLANNED;
        }
        long days = getDurationDays();
        if (days < 0) {
            return DurationSeverity.UNKNOWN;
        } else if (days <= 2) {
            return DurationSeverity.SHORT;
        } else if (days <= 7) {
            return DurationSeverity.MEDIUM;
        } else {
            return DurationSeverity.LONG;
        }
    }

    public String getDurationBadgeText() {
        if (feedCategory == FeedCategory.UNPLANNED_EVENT) {
            return "LIVE INCIDENT";
        }
        long days = getDurationDays();
        if (days < 0) {
            return "DURATION UNKNOWN";
        } else if (days == 1) {
            return "1 DAY (SHORT)";
        } else if (days <= 2) {
            return days + " DAYS (SHORT)";
        } else if (days <= 7) {
            return days + " DAYS (MEDIUM)";
        } else {
            return days + " DAYS (LONG)";
        }
    }

    /**
     * Chooses the icon category from the DATEX II record type (xsi:type), e.g. "d2:PoorEnvironmentConditions".
     * Order matters: more specific types are tested before broader keywords.
     */
    public IconType getIconType() {
        String t = xsiType.toLowerCase(Locale.UK);
        if (feedCategory == FeedCategory.CURRENT_ROADWORK || t.contains("maintenance") || t.contains("construction")) {
            return IconType.ROADWORKS;
        } else if (t.contains("accident")) {
            return IconType.ACCIDENT;
        } else if (t.contains("obstruction") || t.contains("nonweather")) {
            return IconType.OBSTRUCTION;
        } else if (t.contains("management") || t.contains("closure")) {
            return IconType.CLOSURE;
        } else if (t.contains("environment") || t.contains("weather")) {
            return IconType.WEATHER;
        } else if (t.contains("abnormaltraffic") || t.contains("traffic") || t.contains("congestion")) {
            return IconType.CONGESTION;
        } else if (t.contains("event") || t.contains("activit")) {
            return IconType.EVENT;
        }
        return IconType.INFO;
    }

    /**
     * Search used by the road/keyword box. A road code such as "A9" or "m8" must match the road exactly
     * (so "A9" does not return A90 or A96). Any other text is a keyword matched anywhere in the
     * road name, public comment or location description.
     */
    public boolean matchesQuery(String rawQuery) {
        if (rawQuery == null) return true;
        String q = rawQuery.trim().toUpperCase(Locale.UK).replace(" ", "");
        if (q.isEmpty()) return true;

        if (ROAD_CODE.matcher(q).matches()) {
            if (normaliseRoad(roadName).equals(q)) return true;
            Pattern whole = Pattern.compile("(?<![A-Z0-9])" + q + "(?![0-9])");
            return whole.matcher(generalPublicComment.toUpperCase(Locale.UK)).find()
                    || whole.matcher(locationDescriptor.toUpperCase(Locale.UK)).find();
        }

        String keyword = rawQuery.trim().toLowerCase(Locale.UK);
        return roadName.toLowerCase(Locale.UK).contains(keyword)
                || generalPublicComment.toLowerCase(Locale.UK).contains(keyword)
                || locationDescriptor.toLowerCase(Locale.UK).contains(keyword);
    }

    private static String normaliseRoad(String road) {
        return road == null ? "" : road.toUpperCase(Locale.UK).replace("(M)", "").replace(" ", "");
    }

    public String getFormattedStartTime() {
        return formatIsoDate(overallStartTime);
    }

    public String getFormattedEndTime() {
        return formatIsoDate(overallEndTime);
    }

    private ZonedDateTime parseDateTime(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return ZonedDateTime.parse(iso);
        } catch (Exception e1) {
            try {
                return java.time.OffsetDateTime.parse(iso).toZonedDateTime();
            } catch (Exception e2) {
                try {
                    return java.time.LocalDateTime.parse(iso).atZone(java.time.ZoneId.systemDefault());
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private String formatIsoDate(String iso) {
        if (iso == null || iso.isEmpty()) return "Not specified";
        try {
            ZonedDateTime zdt = parseDateTime(iso);
            if (zdt == null) return iso;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.UK);
            return zdt.format(fmt);
        } catch (Exception e) {
            return iso;
        }
    }
}
