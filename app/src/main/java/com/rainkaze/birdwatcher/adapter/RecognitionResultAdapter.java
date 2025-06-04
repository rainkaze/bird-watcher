package com.rainkaze.birdwatcher.adapter;

import android.content.Context; // 新增导入
import android.content.Intent; // 新增导入
import android.net.Uri; // 新增导入
import android.text.TextUtils; // 新增导入
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast; // 新增导入
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import java.util.List;
import java.util.Locale;

public class RecognitionResultAdapter extends RecyclerView.Adapter<RecognitionResultAdapter.ResultViewHolder> {

    private List<RecognitionResult> results;
    private Context context; // 新增 Context 成员变量

    // 修改构造函数以接收 Context
    public RecognitionResultAdapter(Context context, List<RecognitionResult> results) {
        this.context = context;
        this.results = results;
    }

    @NonNull
    @Override
    public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Context 可以从 parent 获取，但构造函数传入更直接
        // if (context == null) {
        //     context = parent.getContext();
        // }
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_recognition_result, parent, false);
        return new ResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
        RecognitionResult result = results.get(position);

        holder.birdNameTextView.setText(result.getBirdName());
        holder.confidenceScoreTextView.setText(
                // 使用格式化字符串
                context.getString(R.string.label_confidence_score_formatted, result.getConfidenceScore())
        );

        // 设置描述
        if (!TextUtils.isEmpty(result.getDetails())) {
            holder.descriptionTextView.setText(result.getDetails());
            holder.descriptionTextView.setVisibility(View.VISIBLE);
        } else {
            holder.descriptionTextView.setVisibility(View.GONE);
        }

        // 设置百科链接
        if (!TextUtils.isEmpty(result.getBaikeUrl())) {
            holder.baikeLinkTextView.setVisibility(View.VISIBLE);
            holder.baikeLinkTextView.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getBaikeUrl()));
                try {
                    context.startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show();
                    // Log a more detailed error if needed
                }
            });
        } else {
            holder.baikeLinkTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void updateData(List<RecognitionResult> newResults) {
        this.results.clear();
        if (newResults != null) { // 添加 null 检查
            this.results.addAll(newResults);
        }
        notifyDataSetChanged();
    }

    static class ResultViewHolder extends RecyclerView.ViewHolder {
        TextView birdNameTextView;
        TextView confidenceScoreTextView;
        TextView descriptionTextView; // 新增
        TextView baikeLinkTextView;   // 新增

        ResultViewHolder(View itemView) {
            super(itemView);
            birdNameTextView = itemView.findViewById(R.id.text_bird_name);
            confidenceScoreTextView = itemView.findViewById(R.id.text_confidence_score);
            descriptionTextView = itemView.findViewById(R.id.text_bird_description); // 绑定新视图
            baikeLinkTextView = itemView.findViewById(R.id.text_baike_link);       // 绑定新视图
        }
    }
}