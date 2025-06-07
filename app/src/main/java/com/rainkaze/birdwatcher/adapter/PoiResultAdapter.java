package com.rainkaze.birdwatcher.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.baidu.mapapi.search.core.PoiInfo;
import com.rainkaze.birdwatcher.R;

import java.util.List;

public class PoiResultAdapter extends RecyclerView.Adapter<PoiResultAdapter.ViewHolder> {

    private final List<PoiInfo> poiList;
    private final OnPoiClickListener clickListener;

    public interface OnPoiClickListener {
        void onPoiClick(PoiInfo poi);
    }

    public PoiResultAdapter(List<PoiInfo> poiList, OnPoiClickListener clickListener) {
        this.poiList = poiList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poi_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PoiInfo poi = poiList.get(position);
        holder.bind(poi, clickListener);
    }

    @Override
    public int getItemCount() {
        return poiList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPoiName;
        TextView tvPoiAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPoiName = itemView.findViewById(R.id.tv_poi_name);
            tvPoiAddress = itemView.findViewById(R.id.tv_poi_address);
        }

        public void bind(final PoiInfo poi, final OnPoiClickListener listener) {
            tvPoiName.setText(poi.getName());
            if (!TextUtils.isEmpty(poi.getAddress())) {
                tvPoiAddress.setText(poi.getAddress());
                tvPoiAddress.setVisibility(View.VISIBLE);
            } else {
                tvPoiAddress.setVisibility(View.GONE);
            }
            itemView.setOnClickListener(v -> listener.onPoiClick(poi));
        }
    }
}