package com.rainkaze.birdwatcher.activity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log; // 新增导入
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rainkaze.birdwatcher.adapter.RecognitionResultAdapter;
import com.rainkaze.birdwatcher.databinding.ActivityImageRecognitionBinding;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import com.rainkaze.birdwatcher.service.BirdIdentificationService;

import java.util.ArrayList;
import java.util.List;

public class ImageRecognitionActivity extends AppCompatActivity {

    private ActivityImageRecognitionBinding binding; // 使用 ViewBinding
    private Uri imageUri;
    private RecognitionResultAdapter resultAdapter;
    private BirdIdentificationService identificationService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityImageRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 使用 ViewBinding 来设置 Toolbar
        setSupportActionBar(binding.toolbar); // 修改这里，使用 binding.toolbar

        // 添加 null 检查
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
        resultAdapter = new RecognitionResultAdapter(new ArrayList<>());
        binding.recyclerViewImageResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewImageResults.setAdapter(resultAdapter);
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
        // onBackPressed(); // 或者 finish(); 如果你希望它总是返回到上一个栈中的 Activity
        finish(); // 通常对于这种子页面，finish() 更符合预期
        return true;
    }
}