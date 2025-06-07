package com.rainkaze.birdwatcher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.BirdStat;

import java.util.List;

public class BirdStatsAdapter extends RecyclerView.Adapter<BirdStatsAdapter.ViewHolder> {

    private final List<BirdStat> stats;
    private int maxCount = 1; // 避免除以零

    public BirdStatsAdapter(List<BirdStat> stats) {
        this.stats = stats;
        if (stats != null && !stats.isEmpty()) {
            this.maxCount = stats.get(0).getCount(); // 列表已排序，第一个是最大值
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bird_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BirdStat stat = stats.get(position);
        holder.rank.setText((position + 1) + ".");
        holder.birdName.setText(stat.getBirdName());
        holder.count.setText(stat.getCount() + " 次");

        if (maxCount > 0) {
            int progress = (int) ((float) stat.getCount() / maxCount * 100);
            holder.progressBar.setProgress(progress);
        } else {
            holder.progressBar.setProgress(0);
        }
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView rank;
        final TextView birdName;
        final TextView count;
        final ProgressBar progressBar;

        ViewHolder(View view) {
            super(view);
            rank = view.findViewById(R.id.tv_stat_rank);
            birdName = view.findViewById(R.id.tv_stat_bird_name);
            count = view.findViewById(R.id.tv_stat_count);
            progressBar = view.findViewById(R.id.pb_stat_bar);
        }
    }
}