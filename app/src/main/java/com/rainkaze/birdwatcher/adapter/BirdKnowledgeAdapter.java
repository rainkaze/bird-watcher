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

            // **BUG FIX #1**: Check for a valid resource ID before giving it to Glide.
            if (bird.getImageResourceId() != 0) {
                Glide.with(context)
                        .load(bird.getImageResourceId())
                        .placeholder(R.drawable.ic_bird_default)
                        .error(R.drawable.ic_picture_error)
                        .centerCrop()
                        .into(ivBirdImage);
            } else {
                // If the ID is 0, load a default image directly.
                ivBirdImage.setImageResource(R.drawable.ic_bird_default);
            }

            itemView.setOnClickListener(v -> listener.onBirdClick(bird));
        }
    }
}