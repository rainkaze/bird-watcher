package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import java.util.List;
import java.util.Locale;

public class RecognitionResultAdapter extends RecyclerView.Adapter<RecognitionResultAdapter.ResultViewHolder> {

    private List<RecognitionResult> results;
    private Context context;

    private OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void onItemLongClick(RecognitionResult result);
    }

    public RecognitionResultAdapter(Context context, List<RecognitionResult> results) {
        this.context = context;
        this.results = results;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recognition_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        RecognitionResult result = results.get(position);

        holder.birdNameTextView.setText(result.getBirdName());
        holder.confidenceScoreTextView.setText(
                context.getString(R.string.label_confidence_score_formatted, result.getConfidenceScore())
        );

        if (!TextUtils.isEmpty(result.getDetails())) {
            holder.descriptionTextView.setText(result.getDetails());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(result.getBaikeUrl())) {
            holder.baikeLinkTextView.setVisibility(View.VISIBLE);
            holder.baikeLinkTextView.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getBaikeUrl()));
                try {
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.baikeLinkTextView.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(result);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void updateData(List<RecognitionResult> newResults) {
        this.results.clear();
        if (newResults != null) {
            this.results.addAll(newResults);
        }
        notifyDataSetChanged();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView birdNameTextView;
        TextView confidenceScoreTextView;
        TextView descriptionTextView;
        TextView baikeLinkTextView;

        ResultViewHolder(View itemView) {
            super(itemView);
            birdNameTextView = itemView.findViewById(R.id.text_bird_name);
            confidenceScoreTextView = itemView.findViewById(R.id.text_confidence_score);
            descriptionTextView = itemView.findViewById(R.id.text_bird_description);
            baikeLinkTextView = itemView.findViewById(R.id.text_baike_link);
        }
    }
}