package com.example.bms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bms.Group;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<Group> groupList;
    private int selectedPosition = -1; // No selection by default

    public GroupAdapter(List<Group> groupList) {
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(holder.getAdapterPosition());
        holder.groupName.setText(group.getName());
        holder.groupStatus.setChecked(selectedPosition == holder.getAdapterPosition());

        // Handle single selection logic
        holder.itemView.setSelected(selectedPosition == holder.getAdapterPosition());

        View.OnClickListener clickListener = v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (selectedPosition == adapterPosition) {
                selectedPosition = -1; // Deselect if already selected
            } else {
                selectedPosition = adapterPosition; // Select the clicked item
            }
            notifyDataSetChanged(); // Refresh the list to update selection
        };

        holder.itemView.setOnClickListener(clickListener);
        holder.groupStatus.setOnClickListener(clickListener);
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public Group getSelectedGroup() {
        if (selectedPosition != -1) {
            return groupList.get(selectedPosition);
        }
        return null;
    }

    public void setSelectedGroup(Group group) {
        selectedPosition = groupList.indexOf(group);
        notifyDataSetChanged();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupName;
        CheckBox groupStatus;

        GroupViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.group_name);
            groupStatus = itemView.findViewById(R.id.group_status);
        }
    }
}