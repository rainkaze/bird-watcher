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

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.RecognitionResultAdapter;
import com.rainkaze.birdwatcher.databinding.ActivityImageRecognitionBinding;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import com.rainkaze.birdwatcher.service.BirdIdentificationService;

import java.util.ArrayList;
import java.util.List;

/**
 * 处理鸟类图片识别的 Activity。
 *
 * <p>此 Activity 接收一个图片 URI，显示图片预览，并允许用户触发识别过程。
 * 识别结果会以列表形式展示，用户可以长按某个结果将其保存为一条新的观鸟记录。</p>
 */
public class ImageRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "ImageRecognitionActivity";

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
            getSupportActionBar().setTitle(getString(R.string.title_image_recognition));
        } else {
        }

        identificationService = new BirdIdentificationService(this);

        if (getIntent() != null && getIntent().getData() != null) {
            imageUri = getIntent().getData();
            binding.imageViewPreview.setImageURI(imageUri);
        } else {
            Toast.makeText(this, getString(R.string.error_image_not_found), Toast.LENGTH_LONG).show();
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

    /**
     * 初始化 RecyclerView 及其适配器，并设置长按点击监听器。
     */
    private void setupRecyclerView() {
        resultAdapter = new RecognitionResultAdapter(this, new ArrayList<>());

        // 为识别结果设置长按监听，用于触发保存到记录的对话框
        resultAdapter.setOnItemLongClickListener(this::showSaveToRecordDialog);

        binding.recyclerViewImageResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewImageResults.setAdapter(resultAdapter);
    }

    /**
     * 显示一个对话框，询问用户是否要将识别结果保存为新的观鸟记录。
     *
     * @param result 用户长按的识别结果对象。
     */
    private void showSaveToRecordDialog(RecognitionResult result) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_title_save_record)
                .setMessage(getString(R.string.dialog_message_save_record, result.getBirdName()))
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    // 创建意图，跳转到 AddEditRecordActivity
                    Intent intent = new Intent(ImageRecognitionActivity.this, AddEditRecordActivity.class);

                    // 将鸟名和图片URI作为额外数据放入意图
                    intent.putExtra(AddEditRecordActivity.EXTRA_BIRD_NAME_FROM_RECOGNITION, result.getBirdName());
                    if (imageUri != null) {
                        intent.putExtra(AddEditRecordActivity.EXTRA_IMAGE_URI_FROM_RECOGNITION, imageUri.toString());
                    }
                    startActivity(intent);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }


    /**
     * 执行图片识别的核心逻辑。
     * <p>它会更新UI以显示进度，并调用 {@link BirdIdentificationService} 来处理实际的识别请求。
     * 识别完成后，通过回调更新UI以显示结果或错误信息。</p>
     *
     * @param imageUriToRecognize 需要识别的图片的 URI。
     */
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
                    String errorMessage = getString(R.string.error_recognition_failed, error);
                    binding.textViewNoImageResult.setText(errorMessage);
                    binding.textViewNoImageResult.setVisibility(View.VISIBLE);
                    Toast.makeText(ImageRecognitionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 处理 Toolbar 上的返回按钮点击事件。
     * @return 总是返回 true，表示事件已被处理。
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}