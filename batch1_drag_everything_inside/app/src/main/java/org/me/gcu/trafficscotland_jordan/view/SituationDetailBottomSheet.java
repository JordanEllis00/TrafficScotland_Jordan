package org.me.gcu.trafficscotland_jordan.view;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.me.gcu.trafficscotland_jordan.R;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;

import java.util.Locale;

public class SituationDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_SITUATION = "arg_situation";

    public static SituationDetailBottomSheet newInstance(TrafficSituation situation) {
        SituationDetailBottomSheet fragment = new SituationDetailBottomSheet();
        Bundle args = new Bundle();
        args.putSerializable(ARG_SITUATION, situation);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() == null) return;

        TrafficSituation item = (TrafficSituation) getArguments().getSerializable(ARG_SITUATION);
        if (item == null) return;

        ImageView ivIcon = view.findViewById(R.id.ivDetailIcon);
        TextView tvTitle = view.findViewById(R.id.tvDetailTitle);
        TextView tvBadge = view.findViewById(R.id.tvDetailBadge);
        TextView tvComment = view.findViewById(R.id.tvDetailComment);
        TextView tvRoad = view.findViewById(R.id.tvDetailRoad);
        TextView tvStart = view.findViewById(R.id.tvDetailStart);
        TextView tvEnd = view.findViewById(R.id.tvDetailEnd);
        TextView tvCoords = view.findViewById(R.id.tvDetailCoords);
        TextView tvMeta = view.findViewById(R.id.tvDetailMeta);

        ivIcon.setImageResource(SituationStyle.iconFor(item));
        tvTitle.setText(item.getFormattedType());
        tvComment.setText(item.getGeneralPublicComment());

        tvBadge.setText(item.getDurationBadgeText());
        ((GradientDrawable) tvBadge.getBackground().mutate()).setColor(SituationStyle.colorFor(requireContext(), item));

        String roadText = item.getRoadName().isEmpty() ? "Scottish Trunk Road Network" : item.getRoadName();
        if (!item.getLocationDescriptor().isEmpty()) {
            roadText += " (" + item.getLocationDescriptor() + ")";
        }
        tvRoad.setText("Road / Location: " + roadText);
        tvStart.setText("Start Time: " + item.getFormattedStartTime());
        tvEnd.setText("End Time: " + item.getFormattedEndTime());

        if (item.hasLinearPoints()) {
            tvCoords.setText(String.format(Locale.UK,
                    "Linear Segment: From (%.4f, %.4f) → To (%.4f, %.4f)",
                    item.getFromLatitude(), item.getFromLongitude(),
                    item.getToLatitude(), item.getToLongitude()));
        } else if (item.hasDisplayPoint()) {
            tvCoords.setText(String.format(Locale.UK,
                    "Coordinates: %.4f, %.4f%s",
                    item.getLatitude(), item.getLongitude(),
                    item.getDistanceKm() >= 0 ? String.format(Locale.UK, " (%.1f km away)", item.getDistanceKm()) : ""));
        } else {
            tvCoords.setText("Coordinates: Not provided in feed");
        }

        StringBuilder meta = new StringBuilder();
        meta.append("Situation ID: ").append(item.getSituationId().isEmpty() ? "N/A" : item.getSituationId());
        if (!item.getImpactSummary().isEmpty()) {
            meta.append(" • Impact: ").append(item.getImpactSummary());
        }
        if (!item.getSeverity().isEmpty()) {
            meta.append(" • Severity: ").append(item.getSeverity());
        }
        tvMeta.setText(meta.toString());
    }
}
