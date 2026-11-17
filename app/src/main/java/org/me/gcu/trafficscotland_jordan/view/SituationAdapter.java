package org.me.gcu.trafficscotland_jordan.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.trafficscotland_jordan.R;
import org.me.gcu.trafficscotland_jordan.model.TrafficSituation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.SituationViewHolder> {

    public interface OnSituationClickListener {
        void onSituationClick(TrafficSituation situation);
    }

    private List<TrafficSituation> items = new ArrayList<>();
    private final OnSituationClickListener listener;

    public SituationAdapter(OnSituationClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TrafficSituation> newItems) {
        this.items = newItems != null ? new ArrayList<>(newItems) : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SituationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_situation, parent, false);
        return new SituationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SituationViewHolder holder, int position) {
        TrafficSituation item = items.get(position);
        Context ctx = holder.itemView.getContext();

        String title = item.getFormattedType();
        if (!item.getRoadName().isEmpty()) {
            title = item.getRoadName() + " • " + title;
        }
        holder.tvType.setText(title);
        holder.tvComment.setText(item.getGeneralPublicComment());

        if (item.getDistanceKm() >= 0) {
            holder.tvDistance.setVisibility(View.VISIBLE);
            holder.tvDistance.setText(String.format(Locale.UK, "%.1f km away", item.getDistanceKm()));
        } else {
            holder.tvDistance.setVisibility(View.GONE);
        }

        holder.tvDurationBadge.setText(item.getDurationBadgeText());
        GradientDrawable bg = (GradientDrawable) holder.tvDurationBadge.getBackground().mutate();
        bg.setColor(SituationStyle.colorFor(ctx, item));

        holder.ivIcon.setImageResource(SituationStyle.iconFor(item));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSituationClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SituationViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvType;
        final TextView tvDurationBadge;
        final TextView tvComment;
        final TextView tvDistance;

        SituationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivSituationIcon);
            tvType = itemView.findViewById(R.id.tvSituationType);
            tvDurationBadge = itemView.findViewById(R.id.tvDurationBadge);
            tvComment = itemView.findViewById(R.id.tvPublicComment);
            tvDistance = itemView.findViewById(R.id.tvDistance);
        }
    }
}
