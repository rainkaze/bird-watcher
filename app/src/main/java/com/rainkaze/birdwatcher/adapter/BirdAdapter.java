package com.rainkaze.birdwatcher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.Bird;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BirdAdapter extends RecyclerView.Adapter<BirdAdapter.BirdViewHolder> {

    private List<Bird> birdList;
    private final List<Bird> birdListFull;
    private final OnBirdClickListener onBirdClickListener;

    public interface OnBirdClickListener {
        void onBirdClick(Bird bird);
    }

    public BirdAdapter(List<Bird> birdList, OnBirdClickListener onBirdClickListener) {
        this.birdList = new ArrayList<>(birdList);
        this.birdListFull = new ArrayList<>(birdList);
        this.onBirdClickListener = onBirdClickListener;
    }

    @NonNull
    @Override
    public BirdViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bird, parent, false);
        return new BirdViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BirdViewHolder holder, int position) {
        Bird bird = birdList.get(position);
        holder.bind(bird, onBirdClickListener);
    }

    @Override
    public int getItemCount() {
        return birdList.size();
    }

    public void filter(String query) {
        birdList.clear();
        if (query.isEmpty()) {
            birdList.addAll(birdListFull);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            List<Bird> filteredList = birdListFull.stream()
                    .filter(bird -> (bird.getChineseName() != null && bird.getChineseName().toLowerCase().contains(lowerCaseQuery)) ||
                            (bird.getScientificName() != null && bird.getScientificName().toLowerCase().contains(lowerCaseQuery)))
                    .collect(Collectors.toList());
            birdList.addAll(filteredList);
        }
        notifyDataSetChanged();
    }


    static class BirdViewHolder extends RecyclerView.ViewHolder {
        TextView chineseNameTextView;
        TextView scientificNameTextView;

        BirdViewHolder(View itemView) {
            super(itemView);
            chineseNameTextView = itemView.findViewById(R.id.chinese_name);
            scientificNameTextView = itemView.findViewById(R.id.scientific_name);
        }

        void bind(final Bird bird, final OnBirdClickListener listener) {
            chineseNameTextView.setText(bird.getChineseName());
            scientificNameTextView.setText(bird.getScientificName());
            itemView.setOnClickListener(v -> listener.onBirdClick(bird));
        }
    }
}