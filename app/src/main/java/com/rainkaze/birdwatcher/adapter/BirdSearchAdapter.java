package com.rainkaze.birdwatcher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.BirdLocation;

import java.util.List;

public class BirdSearchAdapter extends RecyclerView.Adapter<BirdSearchAdapter.ViewHolder> {

    private final List<BirdLocation> birdList;
    private final BirdClickListener clickListener;

    public interface BirdClickListener {
        void onBirdClick(BirdLocation bird);
    }

    public BirdSearchAdapter(List<BirdLocation> birdList, BirdClickListener clickListener) {
        this.birdList = birdList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bird_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BirdLocation bird = birdList.get(position);
        holder.tvBirdName.setText(bird.getName());
        holder.tvPopularity.setText("人气: " + bird.getPopularity() + "%");
        holder.tvDescription.setText(bird.getDescription());

        holder.itemView.setOnClickListener(v -> clickListener.onBirdClick(bird));
    }

    @Override
    public int getItemCount() {
        return birdList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBirdName;
        TextView tvPopularity;
        TextView tvDescription;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBirdName = itemView.findViewById(R.id.tv_bird_name);
            tvPopularity = itemView.findViewById(R.id.tv_popularity);
            tvDescription = itemView.findViewById(R.id.tv_description);
        }
    }
}