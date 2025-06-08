package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecordSearchAdapter extends RecyclerView.Adapter<RecordSearchAdapter.ViewHolder> {

    private final List<BirdRecord> recordList;
    private final OnRecordClickListener clickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnRecordClickListener {
        void onRecordClick(BirdRecord record);
    }

    public RecordSearchAdapter(List<BirdRecord> recordList, OnRecordClickListener clickListener) {
        this.recordList = recordList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BirdRecord record = recordList.get(position);
        holder.bind(record, clickListener);
    }

    @Override
    public int getItemCount() {
        return recordList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_search_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_search_item_subtitle);
        }

        void bind(final BirdRecord record, final OnRecordClickListener listener) {
            tvTitle.setText(record.getTitle());
            String subtitle = record.getBirdName() + " | " + dateFormat.format(record.getRecordDate());
            tvSubtitle.setText(subtitle);

            itemView.setOnClickListener(v -> listener.onRecordClick(record));
        }
    }
}