package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    private List<BirdRecord> records;
    private final Context context;
    private final OnItemClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(BirdRecord record);
        void onItemLongClick(BirdRecord record, View anchorView);
    }

    public RecordAdapter(Context context, List<BirdRecord> initialRecords, OnItemClickListener listener) {
        this.context = context;
        this.records = initialRecords != null ? new ArrayList<>(initialRecords) : new ArrayList<>();
        this.listener = listener;
    }

    public void setRecords(List<BirdRecord> newRecords) {
        this.records.clear();
        if (newRecords != null) {
            this.records.addAll(newRecords);
        }
        notifyDataSetChanged();
    }

    public void addRecord(BirdRecord record) {
        this.records.add(0, record);
        notifyItemInserted(0);
    }

    public void updateRecord(BirdRecord updatedRecord) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == updatedRecord.getId()) {
                records.set(i, updatedRecord);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void removeRecord(long recordId) {
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i).getId() == recordId) {
                records.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }


    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        BirdRecord record = records.get(position);
        holder.bind(record, listener);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class RecordViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail;
        TextView tvTitle;
        TextView tvBirdName;
        TextView tvDateLocation;

        RecordViewHolder(View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_record_thumbnail);
            tvTitle = itemView.findViewById(R.id.tv_record_title);
            tvBirdName = itemView.findViewById(R.id.tv_record_bird_name);
            tvDateLocation = itemView.findViewById(R.id.tv_record_date_location);
        }

        void bind(final BirdRecord record, final OnItemClickListener listener) {
            tvTitle.setText(TextUtils.isEmpty(record.getTitle()) ? "无标题记录" : record.getTitle());

            String birdNameText = !TextUtils.isEmpty(record.getBirdName()) ? record.getBirdName() : "未知鸟类";
            if (!TextUtils.isEmpty(record.getScientificName())) {
                birdNameText += " (" + record.getScientificName() + ")";
            }
            tvBirdName.setText(birdNameText);

            String dateStr = record.getRecordDate() != null ? dateFormat.format(record.getRecordDate()) : "未知日期";
            String locationStr = !TextUtils.isEmpty(record.getDetailedLocation())
                    ? record.getDetailedLocation() : "未知地点";

            if (locationStr.length() > 20) {
                locationStr = locationStr.substring(0, 17) + "...";
            }
            tvDateLocation.setText(String.format("%s | %s", dateStr, locationStr));

            if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty() &&
                    !TextUtils.isEmpty(record.getPhotoUris().get(0))) {
                Glide.with(context)
                        .load(Uri.parse(record.getPhotoUris().get(0)))
                        .placeholder(R.mipmap.ic_launcher_round)
                        .error(R.drawable.ic_picture_error)
                        .centerCrop()
                        .into(ivThumbnail);
            } else {
                Glide.with(context)
                        .load(R.drawable.ic_picture_error)
                        .centerCrop()
                        .into(ivThumbnail);
            }


            itemView.setOnClickListener(v -> listener.onItemClick(record));
            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(record, itemView);
                return true;
            });
        }
    }
}