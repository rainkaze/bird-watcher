package com.rainkaze.birdwatcher.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.RecognitionResultAdapter;
import com.rainkaze.birdwatcher.databinding.ActivitySoundRecognitionBinding;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import com.rainkaze.birdwatcher.service.BirdIdentificationService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * "听音识鸟" 功能的 Activity。
 * <p>
 * 该页面允许用户录制音频，播放录音，并将录音发送到服务器进行鸟鸣识别。
 * 主要功能包括：
 * - 请求录音权限。
 * - 控制音频的录制和停止。
 * - 控制录音的回放。
 * - 调用服务执行声音识别，并在界面上展示结果。
 * - 在生命周期方法中正确管理和释放媒体资源。
 * </p>
 */
public class SoundRecognitionActivity extends AppCompatActivity {

    private static final String TAG = "SoundRecognitionActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private ActivitySoundRecognitionBinding binding;
    private RecognitionResultAdapter resultAdapter;
    private BirdIdentificationService identificationService;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    private String audioFilePath = null;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySoundRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        checkPermissions();
        initServicesAndPaths();
        setupRecyclerView();
        setupClickListeners();
    }

    /**
     * 初始化 Toolbar。
     */
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_sound_recognition));
        }
    }

    /**
     * 检查录音权限，如果未授予则发起请求。
     */
    private void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }
    }

    /**
     * 初始化识别服务和录音文件存储路径。
     */
    private void initServicesAndPaths() {
        identificationService = new BirdIdentificationService(this);
        File audioDir = new File(getExternalCacheDir(), "audiorecord");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        audioFilePath = new File(audioDir, "bird_sound.3gp").getAbsolutePath();
    }

    /**
     * 初始化用于显示识别结果的 RecyclerView。
     */
    private void setupRecyclerView() {
        resultAdapter = new RecognitionResultAdapter(this, new ArrayList<>());
        binding.recyclerViewSoundResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSoundResults.setAdapter(resultAdapter);
    }

    /**
     * 设置所有按钮的点击事件监听器。
     */
    private void setupClickListeners() {
        binding.fabRecordSound.setOnClickListener(v -> toggleRecording());
        binding.buttonPlayRecording.setOnClickListener(v -> togglePlayback());
        binding.buttonStartSoundRecognition.setOnClickListener(v -> triggerRecognition());
    }

    /**
     * 切换录音状态（开始/停止）。
     */
    private void toggleRecording() {
        if (!permissionToRecordAccepted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    /**
     * 切换播放状态（播放/停止）。
     */
    private void togglePlayback() {
        if (audioFilePath != null) {
            if (isPlaying) {
                stopPlaying();
            } else {
                startPlaying();
            }
        }
    }

    /**
     * 触发声音识别流程。
     */
    private void triggerRecognition() {
        if (audioFilePath != null) {
            File audioFile = new File(audioFilePath);
            if (audioFile.exists() && audioFile.length() > 0) {
                performSoundRecognition(audioFile);
            } else {
                Toast.makeText(this, getString(R.string.error_invalid_audio_file), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 开始录音，并更新UI状态。
     */
    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            binding.textRecordingStatus.setText(getString(R.string.status_recording));
            binding.fabRecordSound.setImageResource(R.drawable.ic_stop);
            binding.progressBarSound.setVisibility(View.VISIBLE);
            binding.buttonPlayRecording.setVisibility(View.GONE);
            binding.buttonStartSoundRecognition.setVisibility(View.GONE);
            binding.textViewSoundResultTitle.setVisibility(View.GONE);
            binding.recyclerViewSoundResults.setVisibility(View.GONE);
            binding.textViewNoSoundResult.setVisibility(View.GONE);
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_record_prepare_failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 停止录音，释放资源并更新UI。
     */
    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                binding.textRecordingStatus.setText(getString(R.string.status_record_complete));
                binding.fabRecordSound.setImageResource(R.drawable.ic_mic);
                binding.progressBarSound.setVisibility(View.GONE);
                binding.buttonPlayRecording.setVisibility(View.VISIBLE);
                binding.buttonStartSoundRecognition.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * 开始播放录音。
     */
    private void startPlaying() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            binding.buttonPlayRecording.setText(getString(R.string.action_stop_playback));
            mediaPlayer.setOnCompletionListener(mp -> stopPlaying());
        } catch (IOException e) {
            Toast.makeText(this, getString(R.string.error_playback_prepare_failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 停止播放录音并释放资源。
     */
    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        isPlaying = false;
        binding.buttonPlayRecording.setText(getString(R.string.action_play_recording));
    }

    /**
     * 调用服务执行声音识别，并在回调中处理结果。
     *
     * @param audioFile 包含鸟鸣的音频文件。
     */
    private void performSoundRecognition(File audioFile) {
        binding.progressBarSound.setVisibility(View.VISIBLE);
        binding.buttonStartSoundRecognition.setEnabled(false);
        binding.buttonPlayRecording.setEnabled(false);
        binding.textViewSoundResultTitle.setVisibility(View.GONE);
        binding.recyclerViewSoundResults.setVisibility(View.GONE);
        binding.textViewNoSoundResult.setVisibility(View.GONE);

        identificationService.identifyBirdFromSound(audioFile, new BirdIdentificationService.IdentificationCallback() {
            @Override
            public void onSuccess(List<RecognitionResult> results) {
                runOnUiThread(() -> {
                    binding.progressBarSound.setVisibility(View.GONE);
                    binding.buttonStartSoundRecognition.setEnabled(true);
                    binding.buttonPlayRecording.setEnabled(true);
                    if (results != null && !results.isEmpty()) {
                        binding.textViewSoundResultTitle.setVisibility(View.VISIBLE);
                        binding.recyclerViewSoundResults.setVisibility(View.VISIBLE);
                        resultAdapter.updateData(results);
                    } else {
                        binding.textViewNoSoundResult.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    binding.progressBarSound.setVisibility(View.GONE);
                    binding.buttonStartSoundRecognition.setEnabled(true);
                    binding.buttonPlayRecording.setEnabled(true);
                    String errorMessage = getString(R.string.error_recognition_failed, error);
                    binding.textViewNoSoundResult.setText(errorMessage);
                    binding.textViewNoSoundResult.setVisibility(View.VISIBLE);
                    Toast.makeText(SoundRecognitionActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionToRecordAccepted = true;
            } else {
                permissionToRecordAccepted = false;
                Toast.makeText(this, getString(R.string.error_permission_denied), Toast.LENGTH_SHORT).show();
                finish(); // 如果权限被拒绝，关闭页面
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // 确保在 Activity 停止时释放所有媒体资源，防止泄漏
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}