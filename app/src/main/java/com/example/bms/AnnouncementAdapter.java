package com.example.bms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnouncementViewHolder> {

    private List<AnnouncementModel> announcements;

    public AnnouncementAdapter(List<AnnouncementModel> announcements) {
        this.announcements = announcements;
    }

    @NonNull
    @Override
    public AnnouncementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.announcement_item, parent, false);
        return new AnnouncementViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementViewHolder holder, int position) {
        AnnouncementModel announcement = announcements.get(position);
        holder.title.setText(announcement.getTitle());
        holder.message.setText(announcement.getMessage());
        holder.cardView.setCardBackgroundColor(holder.cardView.getContext().getColor(R.color.secondary));

        if(announcement.getUserId().equals("0")) {
            holder.cardView.setCardBackgroundColor(holder.cardView.getContext().getColor(R.color.amber));
        }
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    public static class AnnouncementViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView message;
        CardView cardView;

        public AnnouncementViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.announcement_title);
            message = itemView.findViewById(R.id.announcement_message);
            cardView = itemView.findViewById(R.id.announcement_item);
        }
    }
}