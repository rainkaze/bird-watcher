package com.rainkaze.birdwatcher.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.DescriptionAdapter;
import com.rainkaze.birdwatcher.databinding.ActivityBirdDetailBinding; // Import the generated binding class
import com.rainkaze.birdwatcher.model.zoology.BirdSpecies;
import com.rainkaze.birdwatcher.model.zoology.DescriptionItem;
import com.rainkaze.birdwatcher.service.ZoologyApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BirdDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BIRD_SPECIES = "EXTRA_BIRD_SPECIES";
    private static final String TAG = "BirdDetailDebug";

    // The binding object will hold direct references to all views
    private ActivityBirdDetailBinding binding;

    private BirdSpecies birdSpecies;
    private ZoologyApiClient apiClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DescriptionAdapter descriptionAdapter;
    private final List<DescriptionItem> descriptionList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using View Binding
        binding = ActivityBirdDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        birdSpecies = getIntent().getParcelableExtra(EXTRA_BIRD_SPECIES);
        if (birdSpecies == null || birdSpecies.getScientificName() == null) {
            Toast.makeText(this, "无法加载鸟类信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "onCreate: Displaying details for " + birdSpecies.getScientificName());

        apiClient = new ZoologyApiClient(this);

        setupToolbar();
        setupRecyclerView();
        populateInitialData();
        fetchDetails();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarBirdDetail);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        descriptionAdapter = new DescriptionAdapter(this, descriptionList);
        binding.rvDescriptions.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDescriptions.setAdapter(descriptionAdapter);
        binding.rvDescriptions.setNestedScrollingEnabled(false);
    }

    private void populateInitialData() {
        // Now using 'binding.collapsingToolbar', which is guaranteed to be non-null
        binding.collapsingToolbar.setTitle(birdSpecies.getName());
        binding.tvDetailBirdName.setText(birdSpecies.getName());
        binding.tvDetailScientificName.setText(birdSpecies.getScientificName());

        if (birdSpecies.getImageResourceId() != 0) {
            Glide.with(this)
                    .load(birdSpecies.getImageResourceId())
                    .error(R.drawable.ic_picture_error)
                    .placeholder(R.drawable.ic_bird_default)
                    .into(binding.ivDetailBirdImage);
        } else {
            Log.w(TAG, "populateInitialData: Invalid image resource ID (0). Loading default placeholder.");
            binding.ivDetailBirdImage.setImageResource(R.drawable.ic_bird_default);
        }
    }

    private void fetchDetails() {
        Log.d(TAG, "fetchDetails: Starting to fetch description types.");
        binding.progressBarDetail.setVisibility(View.VISIBLE);
        descriptionList.clear();
        descriptionAdapter.notifyDataSetChanged();

        apiClient.getDescriptionTypes(birdSpecies.getScientificName(), new ZoologyApiClient.ApiResponseCallback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> result) {
                mainHandler.post(() -> {
                    binding.progressBarDetail.setVisibility(View.GONE);
                    if (result == null || result.isEmpty()) {
                        Log.w(TAG, "fetchDetails onSuccess: No description types found for this species.");
                        DescriptionItem item = new DescriptionItem("无详细信息");
                        item.setContent("该物种暂无详细的文字描述。");
                        descriptionList.add(item);
                        descriptionAdapter.notifyDataSetChanged();
                        return;
                    }
                    fetchNextDescription(new ArrayList<>(result.entrySet()), 0);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "fetchDetails onFailure: Failed to get description types.", e);
                mainHandler.post(()-> {
                    binding.progressBarDetail.setVisibility(View.GONE);
                    DescriptionItem item = new DescriptionItem("信息加载失败");
                    item.setContent("无法获取该物种的详细信息分类: " + e.getMessage());
                    descriptionList.add(item);
                    descriptionAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void fetchNextDescription(final List<Map.Entry<String, String>> types, final int currentIndex) {
        if (currentIndex >= types.size()) {
            Log.d(TAG, "fetchNextDescription: All items processed.");
            return;
        }

        Map.Entry<String, String> entry = types.get(currentIndex);
        final String typeId = entry.getKey();
        final String typeName = entry.getValue();

        Log.d(TAG, "fetchNextDescription: Now fetching index " + currentIndex + " - Type ID: " + typeId + ", Name: " + typeName);

        final DescriptionItem item = new DescriptionItem(typeName);
        final int position = descriptionList.size();
        descriptionList.add(item);
        descriptionAdapter.notifyItemInserted(position);

        apiClient.getDescriptionContent(birdSpecies.getScientificName(), typeId, new ZoologyApiClient.ApiResponseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                mainHandler.post(() -> {
                    Log.d(TAG, "onSuccess for type " + typeName + ". Content length: " + (result != null ? result.length() : 0));
                    if (result != null && !result.isEmpty()) {
                        item.setContent(result);
                    } else {
                        item.setContent("该项暂无内容。");
                    }

                    if (descriptionAdapter != null) descriptionAdapter.notifyItemChanged(position);
                    fetchNextDescription(types, currentIndex + 1);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "onFailure for type " + typeName, e);
                mainHandler.post(() -> {
                    item.setContent("内容加载失败。");
                    if (descriptionAdapter != null) descriptionAdapter.notifyItemChanged(position);
                    fetchNextDescription(types, currentIndex + 1);
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}