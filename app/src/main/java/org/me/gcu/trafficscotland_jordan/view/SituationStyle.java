package org.me.gcu.trafficscotland_jordan.view;

import android.content.Context;

import androidx.core.content.ContextCompat;

import org.me.gcu.trafficscotland_jordan.R;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;

/** Single place that turns model categories into colours and icons (previously duplicated in three classes). */
final class SituationStyle {

    private SituationStyle() { }

    static int colorFor(Context ctx, TrafficSituation item) {
        switch (item.getDurationSeverity()) {
            case SHORT:     return ContextCompat.getColor(ctx, R.color.severity_short);
            case MEDIUM:    return ContextCompat.getColor(ctx, R.color.severity_medium);
            case LONG:      return ContextCompat.getColor(ctx, R.color.severity_long);
            case UNKNOWN:   return ContextCompat.getColor(ctx, R.color.severity_unknown);
            default:        return ContextCompat.getColor(ctx, R.color.severity_unplanned);
        }
    }

    static int iconFor(TrafficSituation item) {
        switch (item.getIconType()) {
            case ACCIDENT:    return R.drawable.ic_accident;
            case CLOSURE:     return R.drawable.ic_closure;
            case OBSTRUCTION: return R.drawable.ic_obstruction;
            case ROADWORKS:   return R.drawable.ic_roadworks;
            case WEATHER:     return R.drawable.ic_weather;
            case EVENT:       return R.drawable.ic_event;
            case CONGESTION:  return R.drawable.ic_congestion;
            default:          return R.drawable.ic_info;
        }
    }
}
