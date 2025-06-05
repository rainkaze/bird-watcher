package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.Bird;

import java.util.List;

public class BirdKnowledgeAdapter extends RecyclerView.Adapter<BirdKnowledgeAdapter.BirdViewHolder> {

    private Context context;
    private List<Bird> birdList;
    private OnBirdItemClickListener listener;

    public BirdKnowledgeAdapter(List<Bird> birdList) {
        this.birdList = birdList;
    }

    public void setOnBirdItemClickListener(OnBirdItemClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Bird> newBirdList) {
        birdList = newBirdList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BirdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_bird_knowledge, parent, false);
        return new BirdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BirdViewHolder holder, int position) {
        Bird bird = birdList.get(position);
        holder.tvBirdName.setText(bird.getCommonName());
        holder.tvScientificName.setText(bird.getScientificName());

        // 修复：使用图片 URL 而不是资源 ID
        String imageUrl = bird.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_bird_placeholder)
                    .error(R.drawable.ic_bird_error)
                    .into(holder.ivBirdImage);
        } else {
            // 如果没有图片 URL，使用占位符
            Glide.with(context)
                    .load(R.drawable.ic_bird_placeholder)
                    .into(holder.ivBirdImage);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBirdItemClick(bird);
            }
        });
    }

    @Override
    public int getItemCount() {
        return birdList.size();
    }

    public static class BirdViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBirdImage;
        TextView tvBirdName;
        TextView tvScientificName;

        public BirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBirdImage = itemView.findViewById(R.id.iv_bird_image);
            tvBirdName = itemView.findViewById(R.id.tv_bird_name);
            tvScientificName = itemView.findViewById(R.id.tv_scientific_name);
        }
    }

    public interface OnBirdItemClickListener {
        void onBirdItemClick(Bird bird);
    }
}