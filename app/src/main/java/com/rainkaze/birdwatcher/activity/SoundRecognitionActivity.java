package com.rainkaze.birdwatcher.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rainkaze.birdwatcher.R; // For drawable ic_stop
import com.rainkaze.birdwatcher.adapter.RecognitionResultAdapter;
import com.rainkaze.birdwatcher.databinding.ActivitySoundRecognitionBinding;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import com.rainkaze.birdwatcher.service.BirdIdentificationService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SoundRecognitionActivity extends AppCompatActivity {

    private static final String LOG_TAG = "SoundRecognition";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private ActivitySoundRecognitionBinding binding;

    private MediaRecorder mediaRecorder;
    private String audioFilePath = null;
    private boolean isRecording = false;

    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;

    private RecognitionResultAdapter resultAdapter;
    private BirdIdentificationService identificationService;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) {
            Toast.makeText(this, "录音权限被拒绝", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySoundRecognitionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(findViewById(R.id.toolbar)); // 如果你有Toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("听音识鸟");


        identificationService = new BirdIdentificationService(this); // 初始化服务

        // 检查并请求录音权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionToRecordAccepted = true;
        }

        // 设置录音文件路径
        File audioDir = new File(getExternalCacheDir(), "audiorecord");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        audioFilePath = new File(audioDir, "bird_sound.3gp").getAbsolutePath();


        setupRecyclerView();

        binding.fabRecordSound.setOnClickListener(v -> {
            if (!permissionToRecordAccepted) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
                return;
            }
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        binding.buttonPlayRecording.setOnClickListener(v -> {
            if (audioFilePath != null) {
                if (isPlaying) {
                    stopPlaying();
                } else {
                    startPlaying();
                }
            }
        });

        binding.buttonStartSoundRecognition.setOnClickListener(v -> {
            if (audioFilePath != null) {
                File audioFile = new File(audioFilePath);
                if (audioFile.exists() && audioFile.length() > 0) {
                    performSoundRecognition(audioFile);
                } else {
                    Toast.makeText(this, "录音文件无效或不存在", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupRecyclerView() {
        resultAdapter = new RecognitionResultAdapter(this, new ArrayList<>());
        binding.recyclerViewSoundResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewSoundResults.setAdapter(resultAdapter);
    }


    private void startRecording() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // 或 AAC_ADTS 等
        mediaRecorder.setOutputFile(audioFilePath);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); // 或 AAC

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            binding.textRecordingStatus.setText("录音中...");
            binding.fabRecordSound.setImageResource(R.drawable.ic_stop); // 你需要一个停止图标
            binding.progressBarSound.setVisibility(View.VISIBLE);
            binding.buttonPlayRecording.setVisibility(View.GONE);
            binding.buttonStartSoundRecognition.setVisibility(View.GONE);
            binding.textViewSoundResultTitle.setVisibility(View.GONE);
            binding.recyclerViewSoundResults.setVisibility(View.GONE);
            binding.textViewNoSoundResult.setVisibility(View.GONE);
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed", e);
            Toast.makeText(this, "录音准备失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException stopException) {
                // 处理在调用stop()时可能发生的RuntimeException
                Log.w(LOG_TAG, "RuntimeException on stopMediaRecorder.", stopException);
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                binding.textRecordingStatus.setText("录音完成");
                binding.fabRecordSound.setImageResource(R.drawable.ic_mic);
                binding.progressBarSound.setVisibility(View.GONE);
                binding.buttonPlayRecording.setVisibility(View.VISIBLE);
                binding.buttonStartSoundRecognition.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startPlaying() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFilePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            binding.buttonPlayRecording.setText("停止播放");
            mediaPlayer.setOnCompletionListener(mp -> stopPlaying());
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed for playback", e);
            Toast.makeText(this, "播放准备失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlaying() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
            binding.buttonPlayRecording.setText("播放录音");
        }
    }

    private void performSoundRecognition(File audioFile) {
        binding.progressBarSound.setIndeterminate(true); // 可能需要切换回Indeterminate
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
                    binding.textViewNoSoundResult.setText("识别出错: " + error);
                    binding.textViewNoSoundResult.setVisibility(View.VISIBLE);
                    Toast.makeText(SoundRecognitionActivity.this, "识别出错: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
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

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}