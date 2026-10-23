package de.trenits.talkshowgaeste.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import de.trenits.talkshowgaeste.R;
import de.trenits.talkshowgaeste.data.Episode;
import de.trenits.talkshowgaeste.data.Repository;
import de.trenits.talkshowgaeste.data.Show;
import de.trenits.talkshowgaeste.util.Formats;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface OnShowClick {
        void onShowClick(Show show);
    }

    private final List<Show> items = new ArrayList<>();
    private final Repository repository;
    private final OnShowClick listener;

    public ScheduleAdapter(Repository repository, OnShowClick listener) {
        this.repository = repository;
        this.listener = listener;
    }

    public void submit(List<Show> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule_show, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Show show = items.get(position);
        holder.time.setText(show.time);
        holder.broadcaster.setText(show.broadcaster);
        holder.name.setText(show.name);
        holder.frequency.setText(show.frequency);
        holder.accentBar.setBackgroundColor(parseColor(show.color));

        Episode next = repository.nextEpisode(show.id, LocalDate.now());
        if (next != null) {
            LocalDate date = LocalDate.parse(next.date);
            String prefix = date.equals(LocalDate.now()) ? "Heute" : Formats.shortDate(date);
            holder.next.setVisibility(View.VISIBLE);
            holder.next.setText("Nächste Gäste: " + prefix);
        } else {
            holder.next.setVisibility(View.GONE);
        }

        holder.card.setOnClickListener(v -> listener.onShowClick(show));
    }

    @Override
    public int getItemCount() {
        return items.size();
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
        final TextView broadcaster;
        final TextView name;
        final TextView frequency;
        final TextView next;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            accentBar = itemView.findViewById(R.id.accentBar);
            time = itemView.findViewById(R.id.tvTime);
            broadcaster = itemView.findViewById(R.id.tvBroadcaster);
            name = itemView.findViewById(R.id.tvName);
            frequency = itemView.findViewById(R.id.tvFrequency);
            next = itemView.findViewById(R.id.tvNext);
        }
    }
}
