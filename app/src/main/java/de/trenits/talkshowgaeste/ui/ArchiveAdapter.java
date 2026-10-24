package de.trenits.talkshowgaeste.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

import de.trenits.talkshowgaeste.R;

public class ArchiveAdapter extends RecyclerView.Adapter<ArchiveAdapter.ViewHolder> {

    private final List<ArchiveItem> items;

    public ArchiveAdapter(List<ArchiveItem> items) {
        this.items = items;
    }

    public void submit(List<ArchiveItem> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_archive_show, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ArchiveItem item = items.get(position);

        holder.showName.setText(item.showName);
        holder.date.setText(item.date);
        holder.guestText.setText(item.guestText);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView showName;
        final TextView date;
        final TextView guestText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            showName = itemView.findViewById(R.id.tvShowName);
            date = itemView.findViewById(R.id.tvDate);
            guestText = itemView.findViewById(R.id.tvGuestText);
        }
    }
}