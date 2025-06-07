package com.rainkaze.birdwatcher.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rainkaze.birdwatcher.adapter.RecognitionResultAdapter;
import com.rainkaze.birdwatcher.databinding.ActivityImageRecognitionBinding;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import com.rainkaze.birdwatcher.service.BirdIdentificationService;

import java.util.ArrayList;
import java.util.List;

public class ImageRecognitionActivity extends AppCompatActivity {

    private ActivityImageRecognitionBinding binding;
    private Uri imageUri;
    private RecognitionResultAdapter resultAdapter;
    private BirdIdentificationService identificationService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("图片识别");
        } else {
            Log.e("ImageRecognitionActivity", "Support Action Bar is null, Toolbar might not be set correctly.");
        }

        identificationService = new BirdIdentificationService(this);

        if (getIntent() != null && getIntent().getData() != null) {
            imageUri = getIntent().getData();
            binding.imageViewPreview.setImageURI(imageUri);
        } else {
            Toast.makeText(this, "未找到图片", Toast.LENGTH_LONG).show();
            Log.e("ImageRecognitionActivity", "Image URI is null in intent data.");
            finish();
            return;
        }

        setupRecyclerView();

        binding.buttonStartImageRecognition.setOnClickListener(v -> {
            if (imageUri != null) {
                performImageRecognition(imageUri);
            }
        });
    }

    private void setupRecyclerView() {
        resultAdapter = new RecognitionResultAdapter(this, new ArrayList<>());

        // 修改点 1: 设置长按监听器
        resultAdapter.setOnItemLongClickListener(result -> {
            showSaveToRecordDialog(result);
        });

        binding.recyclerViewImageResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewImageResults.setAdapter(resultAdapter);
    }

    // 修改点 2: 新增一个方法来显示保存对话框
    private void showSaveToRecordDialog(RecognitionResult result) {
        new AlertDialog.Builder(this)
                .setTitle("保存记录")
                .setMessage("要将 \"" + result.getBirdName() + "\" 的识别结果保存到记鸟笔记吗？")
                .setPositiveButton("保存", (dialog, which) -> {
                    // 创建意图，跳转到 AddEditRecordActivity
                    Intent intent = new Intent(ImageRecognitionActivity.this, AddEditRecordActivity.class);

                    // 将鸟名和图片URI作为额外数据放入意图
                    intent.putExtra(AddEditRecordActivity.EXTRA_BIRD_NAME_FROM_RECOGNITION, result.getBirdName());
                    if (imageUri != null) {
                        intent.putExtra(AddEditRecordActivity.EXTRA_IMAGE_URI_FROM_RECOGNITION, imageUri.toString());
                    }
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }


    private void performImageRecognition(Uri imageUriToRecognize) {
        binding.progressBarImage.setVisibility(View.VISIBLE);
        binding.buttonStartImageRecognition.setEnabled(false);
        binding.textViewImageResultTitle.setVisibility(View.GONE);
        binding.recyclerViewImageResults.setVisibility(View.GONE);
        binding.textViewNoImageResult.setVisibility(View.GONE);

        identificationService.identifyBirdFromImage(imageUriToRecognize, new BirdIdentificationService.IdentificationCallback() {
            @Override
            public void onSuccess(List<RecognitionResult> results) {
                runOnUiThread(() -> {
                    binding.progressBarImage.setVisibility(View.GONE);
                    binding.buttonStartImageRecognition.setEnabled(true);
                    if (results != null && !results.isEmpty()) {
                        binding.textViewImageResultTitle.setVisibility(View.VISIBLE);
                        binding.recyclerViewImageResults.setVisibility(View.VISIBLE);
                        resultAdapter.updateData(results);
                    } else {
                        binding.textViewNoImageResult.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressBarImage.setVisibility(View.GONE);
                    binding.buttonStartImageRecognition.setEnabled(true);
                    binding.textViewNoImageResult.setText("识别出错: " + error);
                    binding.textViewNoImageResult.setVisibility(View.VISIBLE);
                    Toast.makeText(ImageRecognitionActivity.this, "识别出错: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}