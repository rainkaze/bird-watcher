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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.PhotoPreviewAdapter;
import com.rainkaze.birdwatcher.databinding.ActivityAddEditRecordBinding;
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

/**
 * 用于添加或编辑观鸟记录的 Activity。
 * <p>
 * 该页面提供了一个表单，用于输入和编辑观鸟记录的详细信息，
 * 包括标题、鸟类名称、内容、位置、照片和录音。
 * 页面通过 Intent 接收一个可选的记录 ID。如果提供了 ID，则进入编辑模式；否则进入添加模式。
 * <p>
 * 主要功能：
 * - 使用 View Binding 安全地访问视图。
 * - 通过 ActivityResultLauncher 处理图片选择和拍照结果。
 * - 使用百度定位 SDK 获取当前地理位置。
 * - 支持录制、播放和删除音频片段。
 * - 对用户输入进行验证，并将记录保存到本地 SQLite 数据库。
 * - 在应用启动时请求必要的运行时权限。
 */
public class AddEditRecordActivity extends AppCompatActivity {

    /**
     * Intent extra key，用于传递要编辑的记录的 ID。
     */
    public static final String EXTRA_RECORD_ID = "com.rainkaze.birdwatcher.EXTRA_RECORD_ID";
    /**
     * Intent extra key，用于从识别页面接收鸟类名称。
     */
    public static final String EXTRA_BIRD_NAME_FROM_RECOGNITION = "BIRD_NAME_FROM_RECOGNITION";
    /**
     * Intent extra key，用于从识别页面接收图片URI。
     */
    public static final String EXTRA_IMAGE_URI_FROM_RECOGNITION = "IMAGE_URI_FROM_RECOGNITION";

    private static final String TAG = "AddEditRecordActivity";
    private static final int REQUEST_PERMISSIONS_CODE = 101;
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 102;
    private static final int REQUEST_AUDIO_PERMISSION_CODE = 201;

    // ViewBinding 对象，用于替代 findViewById
    private ActivityAddEditRecordBinding binding;

    private PhotoPreviewAdapter photoPreviewAdapter;
    private final List<String> currentPhotoUris = new ArrayList<>();
    private String currentAudioUri = null;

    private BirdRecordDao birdRecordDao;
    private BirdRecord currentRecord;

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
        // 使用 View Binding 加载布局
        binding = ActivityAddEditRecordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        birdRecordDao = new BirdRecordDao(this);
        sessionManager = new SessionManager(this);

        // 设置 Toolbar
        setSupportActionBar(binding.toolbarAddEdit);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close);
        }

        setupPhotoPickerLaunchers();
        setupPhotoPreviewRecyclerView();
        initLocationClient();
        setupClickListeners();

        // 根据 Intent 判断是添加还是编辑模式
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_RECORD_ID)) {
            long currentRecordId = intent.getLongExtra(EXTRA_RECORD_ID, -1);
            if (currentRecordId != -1) {
                setTitle("编辑记录");
                loadRecordData(currentRecordId);
            } else {
                setTitle("添加新纪录");
                initializeNewRecord();
            }
        } else {
            setTitle("添加新纪录");
            initializeNewRecord();
            handleDataFromRecognition(intent);
        }

        checkAndRequestPermissions();
    }

    /**
     * 处理从识别页面跳转过来的数据，预填写鸟名和照片。
     * @param intent 启动此 Activity 的 Intent。
     */
    private void handleDataFromRecognition(Intent intent) {
        if (intent.hasExtra(EXTRA_BIRD_NAME_FROM_RECOGNITION)) {
            String birdName = intent.getStringExtra(EXTRA_BIRD_NAME_FROM_RECOGNITION);
            binding.etBirdName.setText(birdName);
        }
        if (intent.hasExtra(EXTRA_IMAGE_URI_FROM_RECOGNITION)) {
            String imageUriString = intent.getStringExtra(EXTRA_IMAGE_URI_FROM_RECOGNITION);
            Uri sourceUri = Uri.parse(imageUriString);
            // 将图片复制到应用永久存储区，以防源文件被删除
            Uri permanentUri = saveImageFromUriToAppStorage(sourceUri);
            if (permanentUri != null) {
                currentPhotoUris.add(permanentUri.toString());
                photoPreviewAdapter.notifyDataSetChanged();
                binding.rvPhotosPreview.setVisibility(View.VISIBLE);
            }
        }
    }


    /**
     * 初始化一个新的、空的 BirdRecord 对象并更新UI。
     */
    private void initializeNewRecord() {
        currentRecord = new BirdRecord();
        currentRecord.setRecordDate(new Date());
        updateDateDisplay();
    }

    /**
     * 设置所有视图的点击事件监听器。
     */
    private void setupClickListeners() {
        binding.btnGetLocation.setOnClickListener(v -> getLocation());
        binding.btnAddPhotos.setOnClickListener(v -> showPhotoSourceDialog());
        binding.btnRecordAudio.setOnClickListener(v -> toggleRecording());
        binding.btnPlayAudio.setOnClickListener(v -> togglePlayAudio());
        binding.btnDeleteAudio.setOnClickListener(v -> deleteAudioFile());
    }

    /**
     * 更新UI以显示当前记录的日期。
     */
    private void updateDateDisplay() {
        if (currentRecord != null && currentRecord.getRecordDate() != null) {
            binding.tvDateInfo.setText("记录日期: " + dateTimeFormat.format(currentRecord.getRecordDate()));
        } else {
            binding.tvDateInfo.setText("记录日期: 未设置");
        }
    }

    /**
     * 从数据库加载指定ID的记录数据并填充到UI中。
     *
     * @param recordId 要加载的记录的ID。
     */
    private void loadRecordData(long recordId) {
        try {
            birdRecordDao.open();
            currentRecord = birdRecordDao.getRecordById(recordId);
        } finally {
            birdRecordDao.close();
        }

        if (currentRecord != null) {
            binding.etTitle.setText(currentRecord.getTitle());
            binding.etBirdName.setText(currentRecord.getBirdName());
            binding.etScientificName.setText(currentRecord.getScientificName());
            binding.etContent.setText(currentRecord.getContent());
            binding.etDetailedLocation.setText(currentRecord.getDetailedLocation());

            if (!Double.isNaN(currentRecord.getLatitude())) {
                binding.etLatitude.setText(String.format(Locale.US, "%.6f", currentRecord.getLatitude()));
            }
            if (!Double.isNaN(currentRecord.getLongitude())) {
                binding.etLongitude.setText(String.format(Locale.US, "%.6f", currentRecord.getLongitude()));
            }

            if (currentRecord.getPhotoUris() != null) {
                currentPhotoUris.addAll(currentRecord.getPhotoUris());
                photoPreviewAdapter.notifyDataSetChanged();
                binding.rvPhotosPreview.setVisibility(currentPhotoUris.isEmpty() ? View.GONE : View.VISIBLE);
            }

            currentAudioUri = currentRecord.getAudioUri();
            updateAudioUI();
            updateDateDisplay();
        } else {
            Toast.makeText(this, "无法加载记录详情", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 配置用于选择图片和拍照的 ActivityResultLauncher。
     */
    private void setupPhotoPickerLaunchers() {
        // 从相册选择图片的启动器
        pickMultipleImagesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        int newImageCount = 0;
                        // 处理多选图片
                        if (data.getClipData() != null) {
                            int count = data.getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                Uri permanentUri = saveImageFromUriToAppStorage(imageUri);
                                if (permanentUri != null) {
                                    currentPhotoUris.add(permanentUri.toString());
                                    newImageCount++;
                                }
                            }
                        }
                        // 处理单选图片
                        else if (data.getData() != null) {
                            Uri imageUri = data.getData();
                            Uri permanentUri = saveImageFromUriToAppStorage(imageUri);
                            if (permanentUri != null) {
                                currentPhotoUris.add(permanentUri.toString());
                                newImageCount++;
                            }
                        }
                        photoPreviewAdapter.notifyDataSetChanged();
                        binding.rvPhotosPreview.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "成功添加 " + newImageCount + " 张照片", Toast.LENGTH_SHORT).show();
                    }
                });

        // 拍照的启动器
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (tempPhotoUriForCamera != null) {
                            currentPhotoUris.add(tempPhotoUriForCamera.toString());
                            photoPreviewAdapter.notifyDataSetChanged();
                            binding.rvPhotosPreview.setVisibility(View.VISIBLE);
                            Toast.makeText(this, "照片已拍摄", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    /**
     * 将给定的图像URI保存到应用的内部存储，以确保文件持久性。
     *
     * @param sourceUri 要保存的图片的源 URI (例如，来自相册)。
     * @return 保存后新文件的 URI，如果失败则返回 null。
     */
    private Uri saveImageFromUriToAppStorage(Uri sourceUri) {
        if (sourceUri == null) return null;

        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri)) {
            if (inputStream == null) return null;

            File newFile = createImageFileForCamera();
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
            Toast.makeText(this, "保存照片失败", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    /**
     * 设置用于显示照片预览的 RecyclerView。
     */
    private void setupPhotoPreviewRecyclerView() {
        photoPreviewAdapter = new PhotoPreviewAdapter(this, currentPhotoUris, (position, uriString) -> {
            currentPhotoUris.remove(position);
            photoPreviewAdapter.removeItem(position);
            if (currentPhotoUris.isEmpty()) {
                binding.rvPhotosPreview.setVisibility(View.GONE);
            }
            Toast.makeText(AddEditRecordActivity.this, "照片已移除", Toast.LENGTH_SHORT).show();
        });
        binding.rvPhotosPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvPhotosPreview.setAdapter(photoPreviewAdapter);
        binding.rvPhotosPreview.setVisibility(currentPhotoUris.isEmpty() ? View.GONE : View.VISIBLE);
    }

    /**
     * 显示一个对话框，让用户选择是通过拍照还是从相册来添加照片。
     */
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

    /**
     * 启动用于从相册选择多张图片的意图。
     */
    private void dispatchPickMultipleImagesIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickMultipleImagesLauncher.launch(Intent.createChooser(intent, "选择照片"));
    }

    /**
     * 启动相机应用以拍摄新照片。
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFileForCamera();
            } catch (IOException ex) {
                Toast.makeText(this, "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }
            tempPhotoUriForCamera = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUriForCamera);
            takePictureLauncher.launch(takePictureIntent);
        }
    }

    /**
     * 在应用的外部文件目录中创建一个用于存储摄像头照片的临时文件。
     * @return 创建的 File 对象。
     * @throws IOException 如果文件创建失败。
     */
    private File createImageFileForCamera() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    /**
     * 初始化百度定位客户端及其相关配置。
     */
    private void initLocationClient() {
        try {
            LocationClient.setAgreePrivacy(true);
            mLocationClient = new LocationClient(getApplicationContext());

            LocationClientOption option = new LocationClientOption();
            option.setOpenGps(true);
            option.setCoorType("bd09ll");
            option.setScanSpan(0); // 单次定位
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setIsNeedAddress(true);
            mLocationClient.setLocOption(option);

            mBaiduLocationListener = new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    runOnUiThread(() -> {
                        binding.btnGetLocation.setText("获取当前位置");
                        binding.btnGetLocation.setEnabled(true);
                    });

                    if (location != null && (location.getLocType() == BDLocation.TypeGpsLocation ||
                            location.getLocType() == BDLocation.TypeNetWorkLocation)) {
                        final double latitude = location.getLatitude();
                        final double longitude = location.getLongitude();
                        final String address = location.getAddrStr();

                        runOnUiThread(() -> {
                            binding.etLatitude.setText(String.format(Locale.US, "%.6f", latitude));
                            binding.etLongitude.setText(String.format(Locale.US, "%.6f", longitude));
                            if (currentRecord != null && !TextUtils.isEmpty(address) && TextUtils.isEmpty(binding.etDetailedLocation.getText())) {
                                binding.etDetailedLocation.setText(address);
                            }
                            Toast.makeText(AddEditRecordActivity.this, "位置已获取", Toast.LENGTH_SHORT).show();
                        });

                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AddEditRecordActivity.this, "获取位置失败，请检查网络和GPS", Toast.LENGTH_LONG).show();
                        });
                    }
                    if(mLocationClient != null && mLocationClient.isStarted()){
                        mLocationClient.stop();
                    }
                }
            };
            mLocationClient.registerLocationListener(mBaiduLocationListener);
        } catch (Exception e) {
            Toast.makeText(this, "定位服务初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 检查定位权限并启动定位请求。
     */
    private void getLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION_CODE);
        } else {
            startLocationRequest();
        }
    }

    /**
     * 启动百度定位SDK以获取当前位置。
     */
    private void startLocationRequest() {
        if (mLocationClient != null) {
            if (mLocationClient.isStarted()) {
                mLocationClient.stop();
            }
            binding.btnGetLocation.setText("获取中...");
            binding.btnGetLocation.setEnabled(false);
            mLocationClient.start();
        } else {
            Toast.makeText(this, "定位服务异常，请重启应用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 切换录音状态（开始/停止）。
     */
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

    /**
     * 开始录制音频。
     */
    private void startRecording() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            stopPlayingAudio();
        }
        deleteAudioFile(); // 开始新录音前删除旧文件

        try {
            File audioDir = new File(getExternalCacheDir(), "audiorecords");
            if (!audioDir.exists()) {
                audioDir.mkdirs();
            }
            audioFile = File.createTempFile("audio_" + System.currentTimeMillis(), ".3gp", audioDir);
            currentAudioUri = Uri.fromFile(audioFile).toString();

            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            binding.btnRecordAudio.setText("停止录音");
            binding.btnRecordAudio.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_cancel));
            Toast.makeText(this, "录音开始...", Toast.LENGTH_SHORT).show();
            updateAudioUI();
        } catch (IOException e) {
            Toast.makeText(this, "录音失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            currentAudioUri = null;
            updateAudioUI();
        }
    }

    /**
     * 停止录制音频。
     */
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
                currentAudioUri = null;
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                binding.btnRecordAudio.setText("开始录音");
                binding.btnRecordAudio.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_mic));
                if (currentAudioUri != null) {
                    Toast.makeText(this, "录音完成", Toast.LENGTH_SHORT).show();
                }
            }
        }
        updateAudioUI();
    }

    /**
     * 切换音频播放状态（播放/暂停）。
     */
    private void togglePlayAudio() {
        if (TextUtils.isEmpty(currentAudioUri)) return;

        if (isPlayingAudio) {
            stopPlayingAudio();
        } else {
            startPlayingAudio();
        }
    }

    /**
     * 开始播放音频。
     */
    private void startPlayingAudio() {
        if (TextUtils.isEmpty(currentAudioUri)) return;

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(this, Uri.parse(currentAudioUri));
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlayingAudio = true;
            binding.btnPlayAudio.setImageResource(android.R.drawable.ic_media_pause);
            mediaPlayer.setOnCompletionListener(mp -> stopPlayingAudio());
        } catch (IOException e) {
            Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
            isPlayingAudio = false;
        }
    }

    /**
     * 停止播放音频。
     */
    private void stopPlayingAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlayingAudio = false;
        binding.btnPlayAudio.setImageResource(android.R.drawable.ic_media_play);
    }

    /**
     * 删除当前关联的音频文件并更新UI。
     */
    private void deleteAudioFile() {
        stopPlayingAudio();
        if (!TextUtils.isEmpty(currentAudioUri)) {
            Uri audioUri = Uri.parse(currentAudioUri);
            String path = audioUri.getPath();
            if (path != null) {
                File fileToDelete = new File(path);
                if (fileToDelete.exists()) {
                    if (fileToDelete.delete()) {
                    } else {
                    }
                }
            }
            currentAudioUri = null;
        }
        updateAudioUI();
    }

    /**
     * 根据是否存在音频文件更新UI（文件名、播放/删除按钮的可见性）。
     */
    private void updateAudioUI() {
        if (!TextUtils.isEmpty(currentAudioUri)) {
            Uri audioUri = Uri.parse(currentAudioUri);
            String path = audioUri.getPath();
            if (path != null) {
                File audioFileFromUri = new File(path);
                if (audioFileFromUri.exists()) {
                    binding.tvAudioFileName.setText(audioFileFromUri.getName());
                    binding.btnPlayAudio.setVisibility(View.VISIBLE);
                    binding.btnDeleteAudio.setVisibility(View.VISIBLE);
                } else {
                    binding.tvAudioFileName.setText("音频文件不存在");
                    binding.btnPlayAudio.setVisibility(View.GONE);
                    binding.btnDeleteAudio.setVisibility(View.GONE);
                    currentAudioUri = null;
                }
            } else {
                binding.tvAudioFileName.setText("无效的音频路径");
                binding.btnPlayAudio.setVisibility(View.GONE);
                binding.btnDeleteAudio.setVisibility(View.GONE);
                currentAudioUri = null;
            }
        } else {
            binding.tvAudioFileName.setText("未录制音频");
            binding.btnPlayAudio.setVisibility(View.GONE);
            binding.btnDeleteAudio.setVisibility(View.GONE);
        }
    }

    /**
     * 验证用户输入，并保存或更新记录到数据库。
     */
    private void saveRecord() {
        String title = Objects.requireNonNull(binding.etTitle.getText()).toString().trim();
        String birdName = Objects.requireNonNull(binding.etBirdName.getText()).toString().trim();

        if (title.isEmpty()) {
            binding.tilTitle.setError("标题不能为空");
            binding.etTitle.requestFocus();
            return;
        } else {
            binding.tilTitle.setError(null);
        }

        if (birdName.isEmpty()) {
            binding.tilBirdName.setError("鸟名不能为空");
            binding.etBirdName.requestFocus();
            return;
        } else {
            binding.tilBirdName.setError(null);
        }

        binding.tilLatitude.setError(null);
        binding.tilLongitude.setError(null);
        double latitude = Double.NaN;
        double longitude = Double.NaN;

        String latString = Objects.requireNonNull(binding.etLatitude.getText()).toString().trim();
        if (!latString.isEmpty()) {
            try {
                latitude = Double.parseDouble(latString);
                if (latitude < -90 || latitude > 90) {
                    binding.tilLatitude.setError("纬度需在-90到90之间");
                    binding.etLatitude.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.tilLatitude.setError("无效的纬度格式");
                binding.etLatitude.requestFocus();
                return;
            }
        }

        String lonString = Objects.requireNonNull(binding.etLongitude.getText()).toString().trim();
        if (!lonString.isEmpty()) {
            try {
                longitude = Double.parseDouble(lonString);
                if (longitude < -180 || longitude > 180) {
                    binding.tilLongitude.setError("经度需在-180到180之间");
                    binding.etLongitude.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                binding.tilLongitude.setError("无效的经度格式");
                binding.etLongitude.requestFocus();
                return;
            }
        }

        if (currentRecord == null) {
            currentRecord = new BirdRecord();
        }

        currentRecord.setTitle(title);
        currentRecord.setBirdName(birdName);
        currentRecord.setScientificName(Objects.requireNonNull(binding.etScientificName.getText()).toString().trim());
        currentRecord.setContent(Objects.requireNonNull(binding.etContent.getText()).toString().trim());
        currentRecord.setDetailedLocation(Objects.requireNonNull(binding.etDetailedLocation.getText()).toString().trim());
        currentRecord.setLatitude(latitude);
        currentRecord.setLongitude(longitude);
        currentRecord.setPhotoUris(new ArrayList<>(currentPhotoUris));
        currentRecord.setAudioUri(currentAudioUri);

        if (currentRecord.getRecordDateTimestamp() == 0) {
            currentRecord.setRecordDate(new Date());
        }

        try {
            birdRecordDao.open();
            if (currentRecord.getId() != -1) { // 编辑模式
                long resultId = birdRecordDao.updateRecord(currentRecord);
                if (resultId > 0) {
                    Toast.makeText(this, "记录已更新", Toast.LENGTH_LONG).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "更新记录失败", Toast.LENGTH_LONG).show();
                }
            } else { // 添加模式
                long userId = sessionManager.getUserId();
                long resultId = birdRecordDao.addRecord(currentRecord, userId);
                if (resultId != -1) {
                    Toast.makeText(this, "记录已保存", Toast.LENGTH_LONG).show();
                    setResult(Activity.RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(this, "保存记录失败", Toast.LENGTH_LONG).show();
                }
            }
        } finally {
            birdRecordDao.close();
        }
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

    /**
     * 检查并请求应用所需的核心权限。
     */
    private void checkAndRequestPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.CAMERA);
        }
        // 根据 Android 版本请求不同的存储权限
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
        switch (requestCode) {
            case REQUEST_PERMISSIONS_CODE:
                break;
            case REQUEST_LOCATION_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "定位权限已获取，请再次点击按钮", Toast.LENGTH_SHORT).show();
                    startLocationRequest();
                } else {
//                    Toast.makeText(this, "定位权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    toggleRecording();
                } else {
//                    Toast.makeText(this, "录音权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}