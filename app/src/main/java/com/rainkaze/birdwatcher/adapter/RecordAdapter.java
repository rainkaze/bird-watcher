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

import com.bumptech.glide.Glide; // 确保已导入 Glide
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
        void onItemLongClick(BirdRecord record, View anchorView); // anchorView 用于 PopupMenu
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
        notifyDataSetChanged(); // 在实际应用中，考虑使用 DiffUtil 提高效率
    }

    public void addRecord(BirdRecord record) {
        this.records.add(0, record); // 添加到列表顶部
        notifyItemInserted(0);
        // 如果是空列表变为非空，可能需要额外通知
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
                // 如果列表因此变空，可能需要额外通知
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

            // 简单截断地点字符串，避免过长
            if (locationStr.length() > 20) {
                locationStr = locationStr.substring(0, 17) + "...";
            }
            tvDateLocation.setText(String.format("%s | %s", dateStr, locationStr));

            // 设置缩略图
            if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty() &&
                    !TextUtils.isEmpty(record.getPhotoUris().get(0))) {
                Glide.with(context)
                        .load(Uri.parse(record.getPhotoUris().get(0)))
                        .placeholder(R.mipmap.ic_launcher_round) // 默认或占位图
                        .error(R.drawable.ic_picture_error) // 加载错误时显示的图片 (你需要添加这个资源)
                        .centerCrop()
                        .into(ivThumbnail);
            } else {
                // 可以设置一个默认的鸟类图标或者应用图标
                Glide.with(context)
                        .load(R.drawable.ic_picture_error) // 你需要添加这个资源
                        .centerCrop()
                        .into(ivThumbnail);
            }


            itemView.setOnClickListener(v -> listener.onItemClick(record));
            itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(record, itemView); // 传递 itemView 作为 PopupMenu 的锚点
                return true;
            });
        }
    }
}