// 创建在 adapter 包下: com.rainkaze.birdwatcher.adapter.RecognitionResultAdapter
package com.rainkaze.birdwatcher.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rainkaze.birdwatcher.R; // 确保你的R文件路径正确
import com.rainkaze.birdwatcher.model.RecognitionResult;
import java.util.List;
import java.util.Locale;

public class RecognitionResultAdapter extends RecyclerView.Adapter<RecognitionResultAdapter.ResultViewHolder> {

    private List<RecognitionResult> results;

    public RecognitionResultAdapter(List<RecognitionResult> results) {
        this.results = results;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recognition_result, parent, false); // 使用之前定义的item布局
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        RecognitionResult result = results.get(position);
        holder.birdNameTextView.setText(result.getBirdName());
        holder.confidenceScoreTextView.setText(String.format(Locale.getDefault(),"置信度: %.2f", result.getConfidenceScore()));
        // 你还可以设置更多信息，例如 holder.detailsTextView.setText(result.getDetails());
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void updateData(List<RecognitionResult> newResults) {
        this.results.clear();
        this.results.addAll(newResults);
        notifyDataSetChanged(); // 对于更优的性能，可以使用 DiffUtil
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView birdNameTextView;
        TextView confidenceScoreTextView;
        // TextView detailsTextView;

        ResultViewHolder(View itemView) {
            super(itemView);
            birdNameTextView = itemView.findViewById(R.id.text_bird_name);
            confidenceScoreTextView = itemView.findViewById(R.id.text_confidence_score);
            // detailsTextView = itemView.findViewById(R.id.text_details); // 如果你的item布局中有这个
        }
    }
}