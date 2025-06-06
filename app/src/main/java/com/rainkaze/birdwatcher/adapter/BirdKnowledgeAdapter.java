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

public class BirdKnowledgeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EMPTY = 0;
    private static final int TYPE_BIRD = 1;

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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == TYPE_EMPTY) {
            View view = inflater.inflate(R.layout.item_empty_state, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_bird_knowledge, parent, false);
            return new BirdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_EMPTY) {
            // 空视图不需要绑定数据
            return;
        }

        BirdViewHolder birdHolder = (BirdViewHolder) holder;
        Bird bird = birdList.get(position);
        birdHolder.tvBirdName.setText(bird.getCommonName());
        birdHolder.tvScientificName.setText(bird.getScientificName());

        // 加载图片
        String imageUrl = bird.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_bird_placeholder)
                    .error(R.drawable.ic_bird_error)
                    .into(birdHolder.ivBirdImage);
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_bird_placeholder)
                    .into(birdHolder.ivBirdImage);
        }

        birdHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBirdItemClick(bird);
            }
        });
    }

    // 空状态ViewHolder
    static class EmptyViewHolder extends RecyclerView.ViewHolder {
        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    @Override
    public int getItemCount() {
        return birdList.isEmpty() ? 1 : birdList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return birdList.isEmpty() ? TYPE_EMPTY : TYPE_BIRD;
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