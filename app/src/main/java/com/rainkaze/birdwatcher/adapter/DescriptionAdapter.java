package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.zoology.DescriptionItem;

import java.util.List;

public class DescriptionAdapter extends RecyclerView.Adapter<DescriptionAdapter.DescriptionViewHolder> {

    private final Context context;
    private final List<DescriptionItem> descriptionList;

    public DescriptionAdapter(Context context, List<DescriptionItem> descriptionList) {
        this.context = context;
        this.descriptionList = descriptionList;
    }

    @NonNull
    @Override
    public DescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_description, parent, false);
        return new DescriptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DescriptionViewHolder holder, int position) {
        DescriptionItem item = descriptionList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvContent.setText(item.getContent());
    }

    @Override
    public int getItemCount() {
        return descriptionList.size();
    }

    static class DescriptionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvContent;

        public DescriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_description_title);
            tvContent = itemView.findViewById(R.id.tv_description_content);
        }
    }
}