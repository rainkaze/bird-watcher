package com.rainkaze.birdwatcher.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton; // For remove button

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rainkaze.birdwatcher.R; // Make sure you have item_photo_preview.xml

import java.util.List;

public class PhotoPreviewAdapter extends RecyclerView.Adapter<PhotoPreviewAdapter.PhotoViewHolder> {

    private Context context;
    private List<String> photoUris; // List of URI strings
    private OnPhotoRemoveListener removeListener;

    public interface OnPhotoRemoveListener {
        void onPhotoRemove(int position, String uriString);
    }

    public PhotoPreviewAdapter(Context context, List<String> photoUris, OnPhotoRemoveListener listener) {
        this.context = context;
        this.photoUris = photoUris;
        this.removeListener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo_preview_with_remove, parent, false);
        // 使用新的布局 item_photo_preview_with_remove.xml
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String uriString = photoUris.get(position);
        Glide.with(context)
                .load(Uri.parse(uriString))
                .centerCrop()
                .placeholder(R.mipmap.ic_launcher_round) // Or a more specific placeholder
                .into(holder.imageView);

        holder.removeButton.setOnClickListener(v -> {
            if (removeListener != null) {
                removeListener.onPhotoRemove(holder.getAdapterPosition(), uriString);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < photoUris.size()) {
            photoUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, photoUris.size()); // Update subsequent items
        }
    }

    public void addPhoto(String uriString) {
        photoUris.add(uriString);
        notifyItemInserted(photoUris.size() - 1);
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_photo_preview_item);
            removeButton = itemView.findViewById(R.id.btn_remove_photo_item);
        }
    }
}