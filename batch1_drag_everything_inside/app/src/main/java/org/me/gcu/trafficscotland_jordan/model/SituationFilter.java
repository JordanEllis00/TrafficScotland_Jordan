package org.me.gcu.trafficscotland_jordan.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure-Java filtering used by the ViewModel (no Android dependencies, so it is covered by local unit tests).
 * Applies the category chip, the road/keyword search and the "within N km of the reference location" filter.
 */
public final class SituationFilter {

    public enum Category { ALL, UNPLANNED_ONLY, ROADWORKS_ONLY }

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private SituationFilter() { }

    /** Great-circle (haversine) distance in kilometres. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /**
     * @param maxDistanceKm values &lt;= 0 mean "any distance"
     * @return a new list; also stores each situation's distance from the reference point (or -1 if it has no coordinates)
     */
    public static List<TrafficSituation> apply(List<TrafficSituation> all, Category category, String query,
                                               double maxDistanceKm, double refLat, double refLon) {
        List<TrafficSituation> result = new ArrayList<>();
        if (all == null) return result;

        for (TrafficSituation item : all) {
            if (category == Category.UNPLANNED_ONLY && item.getFeedCategory() != TrafficSituation.FeedCategory.UNPLANNED_EVENT) {
                continue;
            }
            if (category == Category.ROADWORKS_ONLY && item.getFeedCategory() != TrafficSituation.FeedCategory.CURRENT_ROADWORK) {
                continue;
            }
            if (!item.matchesQuery(query)) {
                continue;
            }

            if (item.hasAnyCoordinates()) {
                double lat = item.hasDisplayPoint() ? item.getLatitude() : item.getFromLatitude();
                double lon = item.hasDisplayPoint() ? item.getLongitude() : item.getFromLongitude();
                double km = distanceKm(refLat, refLon, lat, lon);
                item.setDistanceKm(km);
                if (maxDistanceKm > 0 && km > maxDistanceKm) {
                    continue;
                }
            } else {
                item.setDistanceKm(-1.0);
                if (maxDistanceKm > 0) {
                    continue;
                }
            }
            result.add(item);
        }

        if (maxDistanceKm > 0) {
            result.sort(Comparator.comparingDouble(TrafficSituation::getDistanceKm));
        }
        return result;
    }
}
