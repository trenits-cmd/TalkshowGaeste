package de.trenits.talkshowgaeste.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import de.trenits.talkshowgaeste.R;
import de.trenits.talkshowgaeste.data.Episode;
import de.trenits.talkshowgaeste.data.Show;

public class TodayAdapter extends RecyclerView.Adapter<TodayAdapter.ViewHolder> {

    public static class TodayItem {
        public final Show show;
        public final Episode episode;

        public TodayItem(Show show, Episode episode) {
            this.show = show;
            this.episode = episode;
        }
    }

    public interface OnShowClick {
        void onShowClick(Show show);
    }

    private final List<TodayItem> items = new ArrayList<>();
    private final OnShowClick listener;

    public TodayAdapter(OnShowClick listener) {
        this.listener = listener;
    }

    public void submit(List<TodayItem> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_today_show, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodayItem item = items.get(position);
        Show show = item.show;
        Episode episode = item.episode;

        String start = episode != null && episode.time != null && !episode.time.isEmpty() ? episode.time : (show.time == null ? "" : show.time);
        holder.time.setText(start);
        holder.broadcaster.setText(show.broadcaster == null || show.broadcaster.isEmpty()
                ? "TV" : show.broadcaster);
        holder.frequency.setText(show.frequency);
        holder.name.setText(show.name);

        String end = addMinutes(start, 90);
        if (end != null && !end.isEmpty()) {
            holder.timeEnd.setVisibility(View.VISIBLE);
            holder.timeEnd.setText("– " + end);
        } else {
            holder.timeEnd.setVisibility(View.GONE);
        }

        if (episode != null && show.time != null
                && episode.time != null && !episode.time.isEmpty()
                && !episode.time.equals(show.time)) {
            holder.frequency.setText(show.frequency + " · " + show.time + " Uhr");
        }

        if (episode != null && episode.topic != null && !episode.topic.trim().isEmpty()) {
            holder.topic.setVisibility(View.VISIBLE);
            holder.topic.setText(episode.topic);
        } else {
            holder.topic.setVisibility(View.GONE);
        }

        int color = parseColor(show.color);
        holder.accentBar.setBackgroundColor(color);
        holder.time.setTextColor(color);

        holder.guestsContainer.removeAllViews();
        if (episode != null && !episode.guests.isEmpty()) {
            holder.guestsLabel.setVisibility(View.VISIBLE);
            holder.guestsContainer.setVisibility(View.VISIBLE);
            holder.noGuests.setVisibility(View.GONE);
            for (String guest : episode.guests) {
                View line = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_guest_line, holder.guestsContainer, false);
                TextView text = line.findViewById(R.id.tvGuest);
                text.setText(guest);
                holder.guestsContainer.addView(line);
            }
        } else {
            holder.guestsLabel.setVisibility(View.GONE);
            holder.guestsContainer.setVisibility(View.GONE);
            holder.noGuests.setVisibility(View.VISIBLE);
        }

        holder.card.setOnClickListener(v -> listener.onShowClick(show));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String addMinutes(String time, int minutes) {
        if (time == null || time.isEmpty()) return null;
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            java.time.LocalTime t = java.time.LocalTime.of(h, m).plusMinutes(minutes);
            return String.format(java.util.Locale.GERMAN, "%02d:%02d", t.getHour(), t.getMinute());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseColor(String value) {
        try {
            return Color.parseColor(value);
        } catch (Exception exception) {
            return Color.parseColor("#E7B45B");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final View accentBar;
        final TextView time;
        final TextView timeEnd;
        final TextView broadcaster;
        final TextView frequency;
        final TextView name;
        final TextView topic;
        final TextView guestsLabel;
        final LinearLayout guestsContainer;
        final TextView noGuests;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            accentBar = itemView.findViewById(R.id.accentBar);
            time = itemView.findViewById(R.id.tvTime);
            timeEnd = itemView.findViewById(R.id.tvTimeEnd);
            broadcaster = itemView.findViewById(R.id.tvBroadcaster);
            frequency = itemView.findViewById(R.id.tvFrequency);
            name = itemView.findViewById(R.id.tvName);
            topic = itemView.findViewById(R.id.tvTopic);
            guestsLabel = itemView.findViewById(R.id.guestsLabel);
            guestsContainer = itemView.findViewById(R.id.guestsContainer);
            noGuests = itemView.findViewById(R.id.tvNoGuests);
        }
    }
}
