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
import com.rainkaze.birdwatcher.model.zoology.BirdSpecies;

import java.util.List;

public class BirdKnowledgeAdapter extends RecyclerView.Adapter<BirdKnowledgeAdapter.BirdViewHolder> {

    private final Context context;
    private final List<BirdSpecies> birdList;
    private final OnBirdClickListener listener;

    // 接口中不再需要图片请求的回调
    public interface OnBirdClickListener {
        void onBirdClick(BirdSpecies bird);
    }

    public BirdKnowledgeAdapter(Context context, List<BirdSpecies> birdList, OnBirdClickListener listener) {
        this.context = context;
        this.birdList = birdList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public BirdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bird_knowledge, parent, false);
        return new BirdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BirdViewHolder holder, int position) {
        BirdSpecies bird = birdList.get(position);
        holder.bind(bird, listener);
    }

    @Override
    public int getItemCount() {
        return birdList.size();
    }

    class BirdViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBirdImage;
        TextView tvBirdCommonName;
        TextView tvBirdScientificName;

        public BirdViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBirdImage = itemView.findViewById(R.id.iv_bird_image);
            tvBirdCommonName = itemView.findViewById(R.id.tv_bird_common_name);
            tvBirdScientificName = itemView.findViewById(R.id.tv_bird_scientific_name);
        }

        public void bind(final BirdSpecies bird, final OnBirdClickListener listener) {
            tvBirdCommonName.setText(bird.getName());
            tvBirdScientificName.setText(bird.getScientificName());

            // 直接从drawable加载本地图片资源
            Glide.with(context)
                    .load(bird.getImageResourceId())
                    .placeholder(R.drawable.ic_bird_default)
                    .error(R.drawable.ic_picture_error) // 如果图片ID无效或未提供，显示错误图标
                    .centerCrop()
                    .into(ivBirdImage);

            itemView.setOnClickListener(v -> listener.onBirdClick(bird));
        }
    }
}