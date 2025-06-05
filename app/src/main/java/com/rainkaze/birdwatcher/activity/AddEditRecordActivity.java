package com.rainkaze.birdwatcher.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.PhotoPreviewAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.google.android.material.button.MaterialButton; // 确保导入


public class AddEditRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "com.rainkaze.birdwatcher.EXTRA_RECORD_ID";
    private static final String TAG = "AddEditRecordActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 101;
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 201;


    private MaterialButton btnRecordAudio;


    private TextInputEditText etTitle, etBirdName, etScientificName, etContent, etDetailedLocation;
    private TextInputLayout tilTitle, tilBirdName;
    private TextView tvLocationInfo, tvDateInfo, tvAudioFileName;
    private Button btnGetLocation, btnAddPhotos;
    private ImageButton btnPlayAudio, btnDeleteAudio;
    private RecyclerView rvPhotosPreview;
    private PhotoPreviewAdapter photoPreviewAdapter;
    private List<String> currentPhotoUris = new ArrayList<>();
    private String currentAudioUri = null;

    private BirdRecordDao birdRecordDao;
    private BirdRecord currentRecord; // 当前正在编辑的记录，如果是新增则为 null 或 new BirdRecord()
    private long currentRecordId = -1; // -1 表示新增

    private ActivityResultLauncher<Intent> pickMultipleImagesLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private Uri tempPhotoUriForCamera; // 用于存储拍照时的临时URI

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlayingAudio = false;
    private File audioFile; // 录音文件

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_record);

        birdRecordDao = new BirdRecordDao(this);

        Toolbar toolbar = findViewById(R.id.toolbar_add_edit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close); // 你需要一个关闭图标 ic_close.xml
        }

        initializeViews();
        setupPhotoPickerLaunchers();
        setupPhotoPreviewRecyclerView();

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_RECORD_ID)) {
            currentRecordId = intent.getLongExtra(EXTRA_RECORD_ID, -1);
            if (currentRecordId != -1) {
                setTitle("编辑记录");
                loadRecordData(currentRecordId);
            } else {
                setTitle("添加新纪录");
                currentRecord = new BirdRecord(); // 新记录
                currentRecord.setRecordDate(new Date()); // 默认为当前时间
                updateDateDisplay();
            }
        } else {
            setTitle("添加新纪录");
            currentRecord = new BirdRecord(); // 新记录
            currentRecord.setRecordDate(new Date()); // 默认为当前时间
            updateDateDisplay();
        }

        btnGetLocation.setOnClickListener(v -> getLocation());
        btnAddPhotos.setOnClickListener(v -> showPhotoSourceDialog());
        btnRecordAudio.setOnClickListener(v -> toggleRecording());
        btnPlayAudio.setOnClickListener(v -> togglePlayAudio());
        btnDeleteAudio.setOnClickListener(v -> deleteAudioFile());

        checkAndRequestPermissions();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.et_title);
        tilTitle = findViewById(R.id.til_title);
        etBirdName = findViewById(R.id.et_bird_name);
        tilBirdName = findViewById(R.id.til_bird_name);
        etScientificName = findViewById(R.id.et_scientific_name);
        etContent = findViewById(R.id.et_content);
        etDetailedLocation = findViewById(R.id.et_detailed_location);
        tvLocationInfo = findViewById(R.id.tv_location_info);
        tvDateInfo = findViewById(R.id.tv_date_info);
        tvAudioFileName = findViewById(R.id.tv_audio_file_name);
        btnGetLocation = findViewById(R.id.btn_get_location);
        btnAddPhotos = findViewById(R.id.btn_add_photos);
        rvPhotosPreview = findViewById(R.id.rv_photos_preview);
        btnRecordAudio = findViewById(R.id.btn_record_audio);
        btnPlayAudio = findViewById(R.id.btn_play_audio);
        btnDeleteAudio = findViewById(R.id.btn_delete_audio);
    }

    private void updateDateDisplay() {
        if (currentRecord != null && currentRecord.getRecordDate() != null) {
            tvDateInfo.setText("记录日期: " + dateTimeFormat.format(currentRecord.getRecordDate()));
        } else {
            tvDateInfo.setText("记录日期: 未设置");
        }
    }

    private void loadRecordData(long recordId) {
        birdRecordDao.open();
        currentRecord = birdRecordDao.getRecordById(recordId);
        birdRecordDao.close();

        if (currentRecord != null) {
            etTitle.setText(currentRecord.getTitle());
            etBirdName.setText(currentRecord.getBirdName());
            etScientificName.setText(currentRecord.getScientificName());
            etContent.setText(currentRecord.getContent());
            etDetailedLocation.setText(currentRecord.getDetailedLocation());

            if (!Double.isNaN(currentRecord.getLatitude()) && !Double.isNaN(currentRecord.getLongitude())) {
                tvLocationInfo.setText(String.format(Locale.getDefault(), "纬度: %.6f, 经度: %.6f", currentRecord.getLatitude(), currentRecord.getLongitude()));
            } else {
                tvLocationInfo.setText("经纬度: 未记录");
            }

            if (currentRecord.getPhotoUris() != null) {
                currentPhotoUris.addAll(currentRecord.getPhotoUris());
                photoPreviewAdapter.notifyDataSetChanged();
                rvPhotosPreview.setVisibility(currentPhotoUris.isEmpty() ? View.GONE : View.VISIBLE);
            }

            currentAudioUri = currentRecord.getAudioUri();
            updateAudioUI();
            updateDateDisplay();

        } else {
            Toast.makeText(this, "无法加载记录详情", Toast.LENGTH_SHORT).show();
            finish(); // 如果记录加载失败，关闭Activity
        }
    }

    private void setupPhotoPickerLaunchers() {
        pickMultipleImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        if (data.getClipData() != null) { // 多选图片
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                currentPhotoUris.add(imageUri.toString());
                            }
                        } else if (data.getData() != null) { // 单选图片
                            Uri imageUri = data.getData();
                            currentPhotoUris.add(imageUri.toString());
                        }
                        photoPreviewAdapter.notifyDataSetChanged();
                        rvPhotosPreview.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "已选择 " + currentPhotoUris.size() + " 张照片", Toast.LENGTH_SHORT).show();
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (tempPhotoUriForCamera != null) {
                            currentPhotoUris.add(tempPhotoUriForCamera.toString());
                            photoPreviewAdapter.notifyDataSetChanged();
                            rvPhotosPreview.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "照片已拍摄", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void setupPhotoPreviewRecyclerView() {
        photoPreviewAdapter = new PhotoPreviewAdapter(this, currentPhotoUris, (position, uriString) -> {
            // 处理照片移除
            currentPhotoUris.remove(position);
            photoPreviewAdapter.removeItem(position); // 通知适配器内部也移除
            if (currentPhotoUris.isEmpty()) {
                rvPhotosPreview.setVisibility(View.GONE);
            }
            Toast.makeText(AddEditRecordActivity.this, "照片已移除", Toast.LENGTH_SHORT).show();
        });
        rvPhotosPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPhotosPreview.setAdapter(photoPreviewAdapter);
        rvPhotosPreview.setVisibility(currentPhotoUris.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showPhotoSourceDialog() {
        final CharSequence[] options = {"拍照", "从相册选择", "取消"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择照片来源");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("拍照")) {
                dispatchTakePictureIntent();
            } else if (options[item].equals("从相册选择")) {
                dispatchPickMultipleImagesIntent();
            } else if (options[item].equals("取消")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }


    private void dispatchPickMultipleImagesIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT); // 或者 Intent.ACTION_OPEN_DOCUMENT
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickMultipleImagesLauncher.launch(Intent.createChooser(intent, "选择照片"));
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFileForCamera();
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file for camera", ex);
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                tempPhotoUriForCamera = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUriForCamera);
                takePictureLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFileForCamera() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        return image;
    }


    private void getLocation() {
        // TODO: 实现调用百度地图API获取位置逻辑
        // 成功后更新 tvLocationInfo 和 currentRecord 的经纬度字段
        // 示例：
        // currentRecord.setLatitude(123.456);
        // currentRecord.setLongitude(78.910);
        // tvLocationInfo.setText("纬度: 123.456, 经度: 78.910 (模拟)");
        Toast.makeText(this, "获取位置功能待实现 (请完善百度地图API调用)", Toast.LENGTH_LONG).show();
    }

    // --- 录音相关 ---
    private void toggleRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
            return;
        }

        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopPlayingAudio(); // 如果正在播放，先停止
        }
        deleteAudioFile(); // 开始新录音前删除旧的（如果存在）

        try {
            File audioDir = new File(getExternalCacheDir(), "audiorecords");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            audioFile = File.createTempFile(
                    "audio_" + System.currentTimeMillis(), ".3gp", audioDir);
            currentAudioUri = Uri.fromFile(audioFile).toString();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            btnRecordAudio.setText("停止录音");
            btnRecordAudio.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_cancel)); // 你需要一个停止图标
            Toast.makeText(this, "录音开始...", Toast.LENGTH_SHORT).show();
            updateAudioUI();
        } catch (IOException e) {
            Log.e(TAG, "startRecording failed", e);
            Toast.makeText(this, "录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            currentAudioUri = null;
            updateAudioUI();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.w(TAG, "RuntimeException on stopping MediaRecorder: " + e.getMessage());
                // 文件可能已损坏或未正确创建
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete(); // 删除可能不完整的录音
                }
                currentAudioUri = null;
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                btnRecordAudio.setText("开始录音");
                btnRecordAudio.setIcon(ContextCompat.getDrawable(this, R.mipmap.ic_mic));
                if (currentAudioUri != null) {
                    Toast.makeText(this, "录音完成", Toast.LENGTH_SHORT).show();
                }
            }
        }
        updateAudioUI();
    }

    private void togglePlayAudio() {
        if (TextUtils.isEmpty(currentAudioUri)) return;

        if (isPlayingAudio) {
            stopPlayingAudio();
        } else {
            startPlayingAudio();
        }
    }

    private void startPlayingAudio() {
        if (TextUtils.isEmpty(currentAudioUri)) return;

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, Uri.parse(currentAudioUri));
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlayingAudio = true;
            btnPlayAudio.setImageResource(android.R.drawable.ic_media_pause);
            mediaPlayer.setOnCompletionListener(mp -> stopPlayingAudio());
        } catch (IOException e) {
            Log.e(TAG, "startPlayingAudio failed", e);
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
            isPlayingAudio = false;
        }
    }

    private void stopPlayingAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlayingAudio = false;
        btnPlayAudio.setImageResource(android.R.drawable.ic_media_play);
    }

    private void deleteAudioFile() {
        stopPlayingAudio(); // 确保停止播放
        if (!TextUtils.isEmpty(currentAudioUri)) {
            File fileToDelete = new File(Uri.parse(currentAudioUri).getPath());
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    Log.d(TAG, "Audio file deleted: " + currentAudioUri);
                } else {
                    Log.e(TAG, "Failed to delete audio file: " + currentAudioUri);
                }
            }
            currentAudioUri = null;
        }
        updateAudioUI();
    }

    private void updateAudioUI() {
        if (!TextUtils.isEmpty(currentAudioUri)) {
            Uri audioUri = Uri.parse(currentAudioUri);
            String path = audioUri.getPath(); // 从 Uri 获取路径
            if (path != null) {
                File audioFileFromUri = new File(path);
                if (audioFileFromUri.exists()) {
                    tvAudioFileName.setText(audioFileFromUri.getName()); // 从 File 对象获取文件名
                    btnPlayAudio.setVisibility(View.VISIBLE);
                    btnDeleteAudio.setVisibility(View.VISIBLE);
                } else {
                    tvAudioFileName.setText("音频文件不存在");
                    btnPlayAudio.setVisibility(View.GONE);
                    btnDeleteAudio.setVisibility(View.GONE);
                    currentAudioUri = null; // 如果文件不存在，清空URI记录
                }
            } else {
                tvAudioFileName.setText("无效的音频路径");
                btnPlayAudio.setVisibility(View.GONE);
                btnDeleteAudio.setVisibility(View.GONE);
                currentAudioUri = null; // 如果路径无效，清空URI记录
            }
        } else {
            tvAudioFileName.setText("未录制音频");
            btnPlayAudio.setVisibility(View.GONE);
            btnDeleteAudio.setVisibility(View.GONE);
        }
    }


    private void saveRecord() {
        String title = etTitle.getText().toString().trim();
        String birdName = etBirdName.getText().toString().trim();

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

        // 如果是新增记录，currentRecord 已经通过 new BirdRecord() 初始化
        // 如果是编辑记录，currentRecord 已经从数据库加载
        if (currentRecord == null) { // 双重保险
            currentRecord = new BirdRecord();
        }

        currentRecord.setTitle(title);
        currentRecord.setBirdName(birdName);
        currentRecord.setScientificName(etScientificName.getText().toString().trim());
        currentRecord.setContent(etContent.getText().toString().trim());
        currentRecord.setDetailedLocation(etDetailedLocation.getText().toString().trim());
        // 经纬度在 getLocation() 中设置
        currentRecord.setPhotoUris(new ArrayList<>(currentPhotoUris)); // 确保是新的List实例
        currentRecord.setAudioUri(currentAudioUri);

        // 日期：如果是新记录，在onCreate时已设置；如果是编辑，则保持原有日期，除非提供修改日期的功能
        if (currentRecord.getRecordDateTimestamp() == 0) { // 确保新记录有日期
            currentRecord.setRecordDate(new Date());
        }


        birdRecordDao.open();
        long resultId;
        if (currentRecord.getId() != -1 && currentRecordId != -1) { // 编辑模式
            resultId = birdRecordDao.updateRecord(currentRecord);
            if (resultId > 0) { // updateRecord 返回受影响行数
                Toast.makeText(this, "记录已更新", Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "更新记录失败", Toast.LENGTH_LONG).show();
            }
        } else { // 新增模式
            resultId = birdRecordDao.addRecord(currentRecord);
            if (resultId != -1) { // addRecord 返回新插入的 ID
                Toast.makeText(this, "记录已保存", Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "保存记录失败", Toast.LENGTH_LONG).show();
            }
        }
        birdRecordDao.close();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_edit_record, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_save_record) {
            saveRecord();
            return true;
        } else if (itemId == android.R.id.home) {
            // 处理向上导航/关闭按钮
            onBackPressed(); // 或 finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // 可以添加一个对话框询问是否放弃更改，如果内容已更改
        // 这里简单地直接返回
        if (isRecording) { // 如果正在录音，先停止
            stopRecording();
        }
        if (isPlayingAudio) { // 如果正在播放，先停止
            stopPlayingAudio();
        }
        super.onBackPressed();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }


    // --- 权限处理 ---
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 对于 API 33+，使用 READ_MEDIA_IMAGES
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                }
                // WRITE_EXTERNAL_STORAGE 在 API 29 以下可能需要用于创建文件
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
            }
        }
        // 录音权限在点击录音按钮时单独请求，或者也可以在这里一起请求
        // if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        //     permissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        // }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "部分功能可能因权限不足而无法使用", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 录音权限已授予，可以开始录音
                startRecording();
            } else {
                Toast.makeText(this, "录音权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }
}