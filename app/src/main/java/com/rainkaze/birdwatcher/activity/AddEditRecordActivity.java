package com.rainkaze.birdwatcher.activity;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
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

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.PhotoPreviewAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;
import com.rainkaze.birdwatcher.service.SessionManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class AddEditRecordActivity extends AppCompatActivity {

    public static final String EXTRA_RECORD_ID = "com.rainkaze.birdwatcher.EXTRA_RECORD_ID";
    // 修改点 1: 定义用于接收数据的常量键
    public static final String EXTRA_BIRD_NAME_FROM_RECOGNITION = "BIRD_NAME_FROM_RECOGNITION";
    public static final String EXTRA_IMAGE_URI_FROM_RECOGNITION = "IMAGE_URI_FROM_RECOGNITION";

    private static final String TAG = "AddEditRecordActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 101;
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 102;
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 201;

    private MaterialButton btnRecordAudio;

    private TextInputEditText etTitle, etBirdName, etScientificName, etContent, etDetailedLocation, etLatitude, etLongitude;
    private TextInputLayout tilTitle, tilBirdName, tilLatitude, tilLongitude;
    private TextView tvDateInfo, tvAudioFileName;
    private Button btnGetLocation, btnAddPhotos;
    private ImageButton btnPlayAudio, btnDeleteAudio;
    private RecyclerView rvPhotosPreview;
    private PhotoPreviewAdapter photoPreviewAdapter;
    private List<String> currentPhotoUris = new ArrayList<>();
    private String currentAudioUri = null;

    private BirdRecordDao birdRecordDao;
    private BirdRecord currentRecord;
    private long currentRecordId = -1;

    private SessionManager sessionManager;


    private ActivityResultLauncher<Intent> pickMultipleImagesLauncher;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private Uri tempPhotoUriForCamera;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlayingAudio = false;
    private File audioFile;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private LocationClient mLocationClient;
    private BDAbstractLocationListener mBaiduLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_record);

        birdRecordDao = new BirdRecordDao(this);
        sessionManager = new SessionManager(this);

        Toolbar toolbar = findViewById(R.id.toolbar_add_edit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        initializeViews();
        setupPhotoPickerLaunchers();
        setupPhotoPreviewRecyclerView();
        initLocationClient();

        // 修改点 2: 重构 onCreate 的逻辑以处理不同启动情况
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_RECORD_ID)) {
            // 编辑模式: 加载现有记录
            currentRecordId = intent.getLongExtra(EXTRA_RECORD_ID, -1);
            if (currentRecordId != -1) {
                setTitle("编辑记录");
                loadRecordData(currentRecordId);
            } else {
                // 理论上不应发生，作为备用
                setTitle("添加新纪录");
                initializeNewRecord();
            }
        } else {
            // 添加模式: 初始化新记录
            setTitle("添加新纪录");
            initializeNewRecord();

            // 检查是否从识别结果跳转而来
            if (intent.hasExtra(EXTRA_BIRD_NAME_FROM_RECOGNITION)) {
                String birdName = intent.getStringExtra(EXTRA_BIRD_NAME_FROM_RECOGNITION);
                etBirdName.setText(birdName);
            }
            if (intent.hasExtra(EXTRA_IMAGE_URI_FROM_RECOGNITION)) {
                String imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI_FROM_RECOGNITION);
                Uri sourceUri = Uri.parse(imageUriString);
                // 将图片复制到应用永久存储区，以防源文件被删除
                Uri permanentUri = saveImageFromUriToAppStorage(sourceUri);
                if (permanentUri != null) {
                    currentPhotoUris.add(permanentUri.toString());
                    photoPreviewAdapter.notifyDataSetChanged();
                    rvPhotosPreview.setVisibility(currentPhotoUris.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }
        }


        btnGetLocation.setOnClickListener(v -> getLocation());
        btnAddPhotos.setOnClickListener(v -> showPhotoSourceDialog());
        btnRecordAudio.setOnClickListener(v -> toggleRecording());
        btnPlayAudio.setOnClickListener(v -> togglePlayAudio());
        btnDeleteAudio.setOnClickListener(v -> deleteAudioFile());

        checkAndRequestPermissions();
    }

    // 修改点 3: 添加一个辅助方法来初始化新记录
    private void initializeNewRecord() {
        currentRecord = new BirdRecord();
        currentRecord.setRecordDate(new Date());
        updateDateDisplay();
    }

    private void initializeViews() {
        etTitle = findViewById(R.id.et_title);
        tilTitle = findViewById(R.id.til_title);
        etBirdName = findViewById(R.id.et_bird_name);
        tilBirdName = findViewById(R.id.til_bird_name);
        etScientificName = findViewById(R.id.et_scientific_name);
        etContent = findViewById(R.id.et_content);
        etDetailedLocation = findViewById(R.id.et_detailed_location);
        tvDateInfo = findViewById(R.id.tv_date_info);
        tvAudioFileName = findViewById(R.id.tv_audio_file_name);
        btnGetLocation = findViewById(R.id.btn_get_location);
        btnAddPhotos = findViewById(R.id.btn_add_photos);
        rvPhotosPreview = findViewById(R.id.rv_photos_preview);
        btnRecordAudio = findViewById(R.id.btn_record_audio);
        btnPlayAudio = findViewById(R.id.btn_play_audio);
        btnDeleteAudio = findViewById(R.id.btn_delete_audio);

        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        tilLatitude = findViewById(R.id.til_latitude);
        tilLongitude = findViewById(R.id.til_longitude);
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

            if (!Double.isNaN(currentRecord.getLatitude())) {
                etLatitude.setText(String.format(Locale.US, "%.6f", currentRecord.getLatitude()));
            }
            if (!Double.isNaN(currentRecord.getLongitude())) {
                etLongitude.setText(String.format(Locale.US, "%.6f", currentRecord.getLongitude()));
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
            finish();
        }
    }

    private void setupPhotoPickerLaunchers() {
        pickMultipleImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int newImageCount = 0;
                        if (data.getClipData() != null) { // 多选图片
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                Uri permanentUri = saveImageFromUriToAppStorage(imageUri);
                                if (permanentUri != null) {
                                    currentPhotoUris.add(permanentUri.toString());
                                    newImageCount++;
                                }
                            }
                        } else if (data.getData() != null) { // 单选图片
                            Uri imageUri = data.getData();
                            Uri permanentUri = saveImageFromUriToAppStorage(imageUri);
                            if (permanentUri != null) {
                                currentPhotoUris.add(permanentUri.toString());
                                newImageCount++;
                            }
                        }
                        photoPreviewAdapter.notifyDataSetChanged();
                        rvPhotosPreview.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "成功添加 " + newImageCount + " 张照片", Toast.LENGTH_SHORT).show();
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

    private Uri saveImageFromUriToAppStorage(Uri sourceUri) {
        if (sourceUri == null) return null;

        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri)) {
            if (inputStream == null) return null;

            File newFile = createImageFileForCamera(); // 复用创建文件的方法
            try (OutputStream outputStream = new FileOutputStream(newFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = inputStream.read(buf)) > 0) {
                    outputStream.write(buf, 0, len);
                }
            }

            return FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    newFile);

        } catch (IOException e) {
            Log.e(TAG, "Failed to copy image to app storage", e);
            Toast.makeText(this, "保存照片失败", Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    private void setupPhotoPreviewRecyclerView() {
        photoPreviewAdapter = new PhotoPreviewAdapter(this, currentPhotoUris, (position, uriString) -> {
            currentPhotoUris.remove(position);
            photoPreviewAdapter.removeItem(position);
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
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

    private void initLocationClient() {
        try {
            LocationClient.setAgreePrivacy(true);
            mLocationClient = new LocationClient(getApplicationContext());

            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true);
            option.setCoorType("bd09ll");
            option.setScanSpan(0);
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setIsNeedAddress(true);
            mLocationClient.setLocOption(option);

            mBaiduLocationListener = new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    runOnUiThread(() -> {
                        btnGetLocation.setText("获取当前位置");
                        btnGetLocation.setEnabled(true);
                    });

                    if (location != null && (location.getLocType() == BDLocation.TypeGpsLocation ||
                            location.getLocType() == BDLocation.TypeNetWorkLocation)) {
                        final double latitude = location.getLatitude();
                        final double longitude = location.getLongitude();
                        final String address = location.getAddrStr();

                        runOnUiThread(() -> {
                            etLatitude.setText(String.format(Locale.US, "%.6f", latitude));
                            etLongitude.setText(String.format(Locale.US, "%.6f", longitude));
                            if (currentRecord != null && !TextUtils.isEmpty(address) && TextUtils.isEmpty(etDetailedLocation.getText())) {
                                etDetailedLocation.setText(address);
                            }
                            Toast.makeText(AddEditRecordActivity.this, "位置已获取", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AddEditRecordActivity.this, "获取位置失败，请检查网络和GPS", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Location failed, error code: " + (location != null ? location.getLocType() : "location is null"));
                        });
                    }
                    if(mLocationClient != null && mLocationClient.isStarted()){
                        mLocationClient.stop();
                    }
                }
            };
            mLocationClient.registerLocationListener(mBaiduLocationListener);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize LocationClient", e);
            Toast.makeText(this, "定位服务初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_CODE);
        } else {
            startLocationRequest();
        }
    }

    private void startLocationRequest() {
        if (mLocationClient != null) {
            if (mLocationClient.isStarted()) {
                mLocationClient.stop();
            }
            btnGetLocation.setText("获取中...");
            btnGetLocation.setEnabled(false);
            mLocationClient.start();
        } else {
            Toast.makeText(this, "定位服务异常，请重启应用", Toast.LENGTH_SHORT).show();
        }
    }

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
            stopPlayingAudio();
        }
        deleteAudioFile();

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
            btnRecordAudio.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_cancel));
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
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
                currentAudioUri = null;
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                btnRecordAudio.setText("开始录音");
                btnRecordAudio.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_mic));
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
        stopPlayingAudio();
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
            String path = audioUri.getPath();
            if (path != null) {
                File audioFileFromUri = new File(path);
                if (audioFileFromUri.exists()) {
                    tvAudioFileName.setText(audioFileFromUri.getName());
                    btnPlayAudio.setVisibility(View.VISIBLE);
                    btnDeleteAudio.setVisibility(View.VISIBLE);
                } else {
                    tvAudioFileName.setText("音频文件不存在");
                    btnPlayAudio.setVisibility(View.GONE);
                    btnDeleteAudio.setVisibility(View.GONE);
                    currentAudioUri = null;
                }
            } else {
                tvAudioFileName.setText("无效的音频路径");
                btnPlayAudio.setVisibility(View.GONE);
                btnDeleteAudio.setVisibility(View.GONE);
                currentAudioUri = null;
            }
        } else {
            tvAudioFileName.setText("未录制音频");
            btnPlayAudio.setVisibility(View.GONE);
            btnDeleteAudio.setVisibility(View.GONE);
        }
    }

    private void saveRecord() {
        String title = Objects.requireNonNull(etTitle.getText()).toString().trim();
        String birdName = Objects.requireNonNull(etBirdName.getText()).toString().trim();

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

        tilLatitude.setError(null);
        tilLongitude.setError(null);
        double latitude = Double.NaN;
        double longitude = Double.NaN;

        String latString = Objects.requireNonNull(etLatitude.getText()).toString().trim();
        if (!latString.isEmpty()) {
            try {
                latitude = Double.parseDouble(latString);
                if (latitude < -90 || latitude > 90) {
                    tilLatitude.setError("纬度需在-90到90之间");
                    etLatitude.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                tilLatitude.setError("无效的纬度格式");
                etLatitude.requestFocus();
                return;
            }
        }

        String lonString = Objects.requireNonNull(etLongitude.getText()).toString().trim();
        if (!lonString.isEmpty()) {
            try {
                longitude = Double.parseDouble(lonString);
                if (longitude < -180 || longitude > 180) {
                    tilLongitude.setError("经度需在-180到180之间");
                    etLongitude.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                tilLongitude.setError("无效的经度格式");
                etLongitude.requestFocus();
                return;
            }
        }


        if (currentRecord == null) {
            currentRecord = new BirdRecord();
        }

        currentRecord.setTitle(title);
        currentRecord.setBirdName(birdName);
        currentRecord.setScientificName(Objects.requireNonNull(etScientificName.getText()).toString().trim());
        currentRecord.setContent(Objects.requireNonNull(etContent.getText()).toString().trim());
        currentRecord.setDetailedLocation(Objects.requireNonNull(etDetailedLocation.getText()).toString().trim());
        currentRecord.setLatitude(latitude);
        currentRecord.setLongitude(longitude);
        currentRecord.setPhotoUris(new ArrayList<>(currentPhotoUris));
        currentRecord.setAudioUri(currentAudioUri);

        if (currentRecord.getRecordDateTimestamp() == 0) {
            currentRecord.setRecordDate(new Date());
        }


        birdRecordDao.open();
        long resultId;
        if (currentRecord.getId() != -1) { // 注意：这里判断应该是 currentRecord.getId() != -1
            resultId = birdRecordDao.updateRecord(currentRecord);
            if (resultId > 0) {
                Toast.makeText(this, "记录已更新", Toast.LENGTH_LONG).show();
                setResult(Activity.RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "更新记录失败", Toast.LENGTH_LONG).show();
            }
        } else {
            // --- 这是关键的修改点 ---
            long userId = sessionManager.getUserId();
            // 如果用户未登录，userId会是-1，这在数据库层面是允许的
            resultId = birdRecordDao.addRecord(currentRecord, userId);
            // --------------------

            if (resultId != -1) {
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
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            stopRecording();
        }
        if (isPlayingAudio) {
            stopPlayingAudio();
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            if (mBaiduLocationListener != null) {
                mLocationClient.unRegisterLocationListener(mBaiduLocationListener);
            }
            mLocationClient.stop();
        }
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            // 可以留空或给一个通用提示
        } else if (requestCode == REQUEST_LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "定位权限已获取，请再次点击按钮", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "定位权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "录音权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }
}