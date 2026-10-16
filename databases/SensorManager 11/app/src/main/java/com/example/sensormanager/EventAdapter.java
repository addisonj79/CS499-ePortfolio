package com.example.sensormanager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<EventItem> eventList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onUpdateClick(EventItem item);
        void onDeleteClick(EventItem item);
    }

    public EventAdapter(List<EventItem> eventList, OnItemClickListener listener) {
        this.eventList = eventList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventItem item = eventList.get(position);
        holder.textName.setText(item.getName());
        holder.textDate.setText(item.getDate());

        holder.btnUpdate.setOnClickListener(v -> listener.onUpdateClick(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textDate;
        Button btnUpdate, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textEventName);
            textDate = itemView.findViewById(R.id.textEventDate);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
