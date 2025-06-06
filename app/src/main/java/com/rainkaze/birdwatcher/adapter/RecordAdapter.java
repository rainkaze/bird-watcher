package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        void onItemLongClick(BirdRecord record, View view); // 可选，用于删除等操作
    }

    public RecordAdapter(Context context, List<BirdRecord> records, OnItemClickListener listener) {
        this.context = context;
        this.records = records != null ? records : new ArrayList<>();
        this.listener = listener;
    }

    public void setRecords(List<BirdRecord> newRecords) {
        this.records.clear();
        if (newRecords != null) {
            this.records.addAll(newRecords);
        }
        notifyDataSetChanged(); // 在实际应用中，考虑使用 DiffUtil 提高效率
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
        //holder.bind(record, listener);
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

//        void bind(final BirdRecord record, final OnItemClickListener listener) {
//            tvTitle.setText(record.getTitle());
//
//            String birdNameText = record.getBirdName() != null ? record.getBirdName() : "未知鸟类";
//            if (record.getScientificName() != null && !record.getScientificName().isEmpty()) {
//                birdNameText += " (" + record.getScientificName() + ")";
//            }
//            tvBirdName.setText(birdNameText);
//
//            String dateStr = record.getRecordDate() != null ? dateFormat.format(record.getRecordDate()) : "未知日期";
//            String locationStr = record.getDetailedLocation() != null && !record.getDetailedLocation().isEmpty()
//                    ? record.getDetailedLocation() : "未知地点";
//            if (locationStr.length() > 15) { // 简单截断地点字符串
//                locationStr = locationStr.substring(0, 12) + "...";
//            }
//            tvDateLocation.setText(String.format("%s | %s", dateStr, locationStr));
//
//            // 设置缩略图 (后续实现)
//            // 例如，如果 photoUris 不为空且有内容，加载第一张图片
//            // Glide.with(context).load(Uri.parse(record.getPhotoUris().get(0))).placeholder(R.mipmap.ic_default_bird_thumbnail).into(ivThumbnail);
//            // 目前使用默认图标
//            if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty() && record.getPhotoUris().get(0) != null) {
//                // 这里应该用图片加载库如 Glide 或 Picasso
//                // Glide.with(context).load(Uri.parse(record.getPhotoUris().get(0))).placeholder(R.mipmap.ic_launcher_round).error(R.mipmap.ic_launcher_round).into(ivThumbnail);
//                ivThumbnail.setImageURI(Uri.parse(record.getPhotoUris().get(0))); // 简单示例，实际可能需要更复杂的加载
//            } else {
//                ivThumbnail.setImageResource(R.mipmap.ic_launcher_round); // 默认或占位图
//            }
//
//
//            itemView.setOnClickListener(v -> listener.onItemClick(record));
//            itemView.setOnLongClickListener(v -> {
//                listener.onItemLongClick(record, v);
//                return true;
//            });
//        }
    }
}