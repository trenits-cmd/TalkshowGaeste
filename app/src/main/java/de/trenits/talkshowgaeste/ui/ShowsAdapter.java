package de.trenits.talkshowgaeste.ui;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import de.trenits.talkshowgaeste.R;
import de.trenits.talkshowgaeste.data.Show;
import de.trenits.talkshowgaeste.util.Formats;

public class ShowsAdapter extends RecyclerView.Adapter<ShowsAdapter.ViewHolder> {

    public interface Listener {
        void onShowClick(Show show);

        void onEdit(Show show);

        void onDelete(Show show);
    }

    private final List<Show> items = new ArrayList<>();
    private final Listener listener;

    public ShowsAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Show> shows) {
        items.clear();
        items.addAll(shows);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_manage_show, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Show show = items.get(position);
        holder.name.setText(show.name);
        String broadcaster = show.broadcaster == null || show.broadcaster.isEmpty()
                ? "Ohne Sender" : show.broadcaster;
        holder.meta.setText(broadcaster + " · " + Formats.weekdays(show.weekdays)
                + " · " + show.time);
        holder.colorDot.setBackgroundTintList(ColorStateList.valueOf(parseColor(show.color)));

        if (show.local) {
            holder.localBadge.setVisibility(View.VISIBLE);
            holder.localBadge.setText("Eigene Show");
        } else if (show.customized) {
            holder.localBadge.setVisibility(View.VISIBLE);
            holder.localBadge.setText("Angepasst");
        } else {
            holder.localBadge.setVisibility(View.GONE);
        }

        holder.card.setOnClickListener(v -> listener.onShowClick(show));
        holder.more.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.getMenu().add(0, 1, 0, R.string.edit);
            menu.getMenu().add(0, 2, 1, R.string.delete_show);
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    listener.onEdit(show);
                    return true;
                }
                if (item.getItemId() == 2) {
                    listener.onDelete(show);
                    return true;
                }
                return false;
            });
            menu.show();
        });
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
        final View colorDot;
        final TextView name;
        final TextView meta;
        final TextView localBadge;
        final ImageButton more;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            colorDot = itemView.findViewById(R.id.colorDot);
            name = itemView.findViewById(R.id.tvName);
            meta = itemView.findViewById(R.id.tvMeta);
            localBadge = itemView.findViewById(R.id.tvLocalBadge);
            more = itemView.findViewById(R.id.buttonMore);
        }
    }
}
