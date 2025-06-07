package com.rainkaze.birdwatcher.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.BirdRecord;

// import java.util.ArrayList; // 如果要处理图片预览
// import com.rainkaze.birdwatcher.adapter.PhotoPreviewAdapter; // 如果要处理图片预览

import java.util.Date;
// import java.util.List;

public class AddEditRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "com.rainkaze.birdwatcher.EXTRA_RECORD_ID";
    // 可以定义其他 EXTRA 常量用于传递数据，如果需要返回已保存的 BirdRecord 对象给 RecordFragment

    private TextInputEditText etTitle, etBirdName, etScientificName, etContent, etDetailedLocation;
    private TextInputLayout tilTitle, tilBirdName; // 用于显示错误信息
    private TextView tvLocationInfo;
    private Button btnGetLocation, btnAddPhotos, btnRecordAudio;
    private RecyclerView rvPhotosPreview;
    // private PhotoPreviewAdapter photoPreviewAdapter; // 后续添加照片预览
    // private List<String> currentPhotoUris = new ArrayList<>(); // 后续添加

    private long currentRecordId = -1; // -1 表示新增，否则为编辑

    // 文件选择的Launcher (示例)
    private ActivityResultLauncher<Intent> pickImageLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_record);

        Toolbar toolbar = findViewById(R.id.toolbar_add_edit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel); // 或其他关闭图标
        }

        etTitle = findViewById(R.id.et_title);
        tilTitle = findViewById(R.id.til_title);
        etBirdName = findViewById(R.id.et_bird_name);
        tilBirdName = findViewById(R.id.til_bird_name);
        etScientificName = findViewById(R.id.et_scientific_name);
        etContent = findViewById(R.id.et_content);
        etDetailedLocation = findViewById(R.id.et_detailed_location);
        tvLocationInfo = findViewById(R.id.tv_location_info);
        btnGetLocation = findViewById(R.id.btn_get_location);
        btnAddPhotos = findViewById(R.id.btn_add_photos);
        // rvPhotosPreview = findViewById(R.id.rv_photos_preview); // 照片预览
        btnRecordAudio = findViewById(R.id.btn_record_audio);


        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_RECORD_ID)) {
            currentRecordId = intent.getLongExtra(EXTRA_RECORD_ID, -1);
            setTitle("编辑记录");
            // 如果是编辑模式，后续需要从数据库加载数据显示到各个字段
            // loadRecordData(currentRecordId);
        } else {
            setTitle("添加新纪录");
        }

        btnGetLocation.setOnClickListener(v -> getLocation());
        btnAddPhotos.setOnClickListener(v -> selectPhotos());
        btnRecordAudio.setOnClickListener(v -> recordAudio());

        // 初始化照片预览 (后续)
        // setupPhotoPreviewRecyclerView();

        // 初始化图片选择器
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) { // 多选图片
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                android.net.Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                // currentPhotoUris.add(imageUri.toString());
                            }
                        } else if (result.getData().getData() != null) { // 单选图片
                            android.net.Uri imageUri = result.getData().getData();
                            // currentPhotoUris.add(imageUri.toString());
                        }
                        // photoPreviewAdapter.notifyDataSetChanged();
                        Toast.makeText(this, "照片已选择 (逻辑待实现)", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /* // 后续添加照片预览的RecyclerView设置
    private void setupPhotoPreviewRecyclerView() {
        // photoPreviewAdapter = new PhotoPreviewAdapter(this, currentPhotoUris, uri -> {
            // 点击预览图的操作，例如移除
            // currentPhotoUris.remove(uri);
            // photoPreviewAdapter.notifyDataSetChanged();
        // });
        // rvPhotosPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // rvPhotosPreview.setAdapter(photoPreviewAdapter);
    }
    */

    private void getLocation() {
        // TODO: 实现调用百度地图API获取位置逻辑
        // 成功后更新 tvLocationInfo 和对应的经纬度变量
        tvLocationInfo.setText("经度: 123.456, 纬度: 78.910 (模拟)");
        Toast.makeText(this, "获取位置功能待实现", Toast.LENGTH_SHORT).show();
    }

    private void selectPhotos() {
        // TODO: 实现照片选择逻辑 (需要相机和存储权限)
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 允许多选
        pickImageLauncher.launch(Intent.createChooser(intent, "选择照片"));
    }

    private void recordAudio() {
        // TODO: 实现录音功能 (需要录音权限)
        Toast.makeText(this, "录音功能待实现", Toast.LENGTH_SHORT).show();
    }

    private void saveRecord() {
        String title = etTitle.getText().toString().trim();
        String birdName = etBirdName.getText().toString().trim();
        String scientificName = etScientificName.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String detailedLocation = etDetailedLocation.getText().toString().trim();
        // 获取经纬度、照片URI列表、音频URI

        if (title.isEmpty()) {
            tilTitle.setError("标题不能为空");
            etTitle.requestFocus();
            return;
        } else {
            tilTitle.setError(null);
        }

        if (birdName.isEmpty()) {
            tilBirdName.setError("鸟名不能为空");
            etBirdName.requestFocus();
            return;
        } else {
            tilBirdName.setError(null);
        }

        BirdRecord record = new BirdRecord();
        if (currentRecordId != -1) {
 //           record.setId(currentRecordId); // 如果是编辑，设置ID
        }
//        record.setTitle(title);
//        record.setBirdName(birdName);
//        record.setScientificName(scientificName);
//        record.setContent(content);
//        record.setDetailedLocation(detailedLocation);
//        record.setRecordDate(new Date()); //或提供日期选择器
        // record.setPhotoUris(currentPhotoUris);
        // record.setAudioUri(currentAudioUri);
        // record.setLatitude(latitude);
        // record.setLongitude(longitude);

        // TODO: 将 record 对象保存到数据库 (使用Room) 或通过 Intent 返回
        //  目前，我们只是显示一个Toast并结束Activity

        // 示例：通过Intent将数据返回给调用者(RecordFragment)
        Intent resultIntent = new Intent();
        // 为了传递自定义对象，BirdRecord需要实现Parcelable接口，或者你逐个传递字段
        // 这里为了简单，我们只返回一个成功状态
        // resultIntent.putExtra("saved_record_title", record.getTitle()); // 示例
        setResult(Activity.RESULT_OK, resultIntent);
        finish(); // 关闭当前Activity

        Toast.makeText(this, "记录已保存 (逻辑待实现数据库)", Toast.LENGTH_LONG).show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save_record) {
            saveRecord();
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            // 处理向上导航/关闭按钮
            // 可以添加一个对话框询问是否放弃更改
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}