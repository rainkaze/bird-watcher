package com.rainkaze.birdwatcher.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.DescriptionAdapter;
import com.rainkaze.birdwatcher.model.zoology.BirdSpecies;
import com.rainkaze.birdwatcher.model.zoology.DescriptionItem;
import com.rainkaze.birdwatcher.service.ZoologyApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BirdDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BIRD_SPECIES = "EXTRA_BIRD_SPECIES";
    private static final String TAG = "BirdDetailActivity";

    private BirdSpecies birdSpecies;
    private ZoologyApiClient apiClient;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ImageView ivBirdImage;
    private TextView tvBirdName, tvScientificName;
    private RecyclerView rvDescriptions;
    private DescriptionAdapter descriptionAdapter;
    private final List<DescriptionItem> descriptionList = new ArrayList<>();
    private ProgressBar progressBar;
    private CollapsingToolbarLayout collapsingToolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bird_detail);

        birdSpecies = getIntent().getParcelableExtra(EXTRA_BIRD_SPECIES);
        if (birdSpecies == null || birdSpecies.getScientificName() == null) {
            Toast.makeText(this, "无法加载鸟类信息", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        apiClient = new ZoologyApiClient(this);
        initViews();
        setupToolbar();
        populateInitialData();
        fetchDetails(); // 调用重构后的主方法
    }

    private void initViews() {
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        ivBirdImage = findViewById(R.id.iv_detail_bird_image);
        tvBirdName = findViewById(R.id.tv_detail_bird_name);
        tvScientificName = findViewById(R.id.tv_detail_scientific_name);
        progressBar = findViewById(R.id.progress_bar_detail);
        rvDescriptions = findViewById(R.id.rv_descriptions);

        descriptionAdapter = new DescriptionAdapter(this, descriptionList);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(this));
        rvDescriptions.setAdapter(descriptionAdapter);
        rvDescriptions.setNestedScrollingEnabled(false);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_bird_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void populateInitialData() {
        collapsingToolbar.setTitle(birdSpecies.getName());
        tvBirdName.setText(birdSpecies.getName());
        tvScientificName.setText(birdSpecies.getScientificName());

        Glide.with(this)
                .load(birdSpecies.getImageResourceId())
                .error(R.drawable.ic_picture_error)
                .placeholder(R.drawable.ic_bird_default)
                .into(ivBirdImage);
    }

    private void fetchDetails() {
        progressBar.setVisibility(View.VISIBLE);
        descriptionList.clear();
        descriptionAdapter.notifyDataSetChanged();

        // 步骤1: 获取所有可用的描述信息类别
        apiClient.getDescriptionTypes(birdSpecies.getScientificName(), new ZoologyApiClient.ApiResponseCallback<Map<String, String>>() {
            @Override
            public void onSuccess(Map<String, String> result) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    if (result == null || result.isEmpty()) {
                        DescriptionItem item = new DescriptionItem("无详细信息");
                        item.setContent("该物种暂无详细的文字描述。");
                        descriptionList.add(item);
                        descriptionAdapter.notifyDataSetChanged();
                        return;
                    }
                    // 步骤2: 将获取到的类别列表传递给顺序加载方法，从第一个开始
                    fetchNextDescription(new ArrayList<>(result.entrySet()), 0);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "获取描述类型失败", e);
                mainHandler.post(()-> {
                    progressBar.setVisibility(View.GONE);
                    DescriptionItem item = new DescriptionItem("信息加载失败");
                    item.setContent("无法获取该物种的详细信息分类: " + e.getMessage());
                    descriptionList.add(item);
                    descriptionAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    /**
     * 这是核心的修复方法：顺序获取每一个描述类型的具体内容
     * @param types 包含所有类型ID和名称的列表
     * @param currentIndex 当前要获取的类型的索引
     */
    private void fetchNextDescription(final List<Map.Entry<String, String>> types, final int currentIndex) {
        // 如果所有类型都已加载完毕，则停止递归
        if (currentIndex >= types.size()) {
            return;
        }

        Map.Entry<String, String> entry = types.get(currentIndex);
        final String typeId = entry.getKey();
        final String typeName = entry.getValue();

        // 先在UI上创建一个“加载中...”的条目
        final DescriptionItem item = new DescriptionItem(typeName);
        final int position = descriptionList.size();
        descriptionList.add(item);
        descriptionAdapter.notifyItemInserted(position);

        // 为这个条目请求具体内容
        apiClient.getDescriptionContent(birdSpecies.getScientificName(), typeId, new ZoologyApiClient.ApiResponseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                mainHandler.post(() -> {
                    item.setContent(result);
                    if (descriptionAdapter != null) descriptionAdapter.notifyItemChanged(position);
                    // 成功后，请求下一个
                    fetchNextDescription(types, currentIndex + 1);
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "获取内容失败，类型ID: " + typeId, e);
                mainHandler.post(() -> {
                    item.setContent("内容加载失败。");
                    if (descriptionAdapter != null) descriptionAdapter.notifyItemChanged(position);
                    // 即使失败，也要继续请求下一个
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