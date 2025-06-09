package com.rainkaze.birdwatcher.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.ImageRecognitionActivity;
import com.rainkaze.birdwatcher.activity.SoundRecognitionActivity;
import com.rainkaze.birdwatcher.databinding.FragmentIdentifyBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * “识别”功能的主界面 Fragment。
 * <p>
 * 该 Fragment 提供了三种鸟类识别的入口：
 * <ol>
 * <li><b>拍照识别</b>：通过启动相机拍摄新照片进行识别。</li>
 * <li><b>相册识别</b>：从设备相册中选择一张现有图片进行识别。</li>
 * <li><b>声音识别</b>：启动声音识别界面。</li>
 * </ol>
 * 它使用 {@link ActivityResultLauncher} 来处理权限请求、相机和相册的返回结果，
 * 确保在执行需要权限的操作前，会安全地向用户请求授权。
 * </p>
 */
public class IdentifyFragment extends Fragment {
    private static final String TAG = "IdentifyFragment";
    private FragmentIdentifyBinding binding;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private Uri currentPhotoUri;
    private enum PendingAction {
        TAKE_PHOTO, PICK_IMAGE, NONE
    }
    private PendingAction pendingAction = PendingAction.NONE;
    public IdentifyFragment() {
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializePermissionsLauncher();
        initializeTakePictureLauncher();
        initializePickImageLauncher();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentIdentifyBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        binding.cardTakePhoto.setOnClickListener(v -> handleAction(PendingAction.TAKE_PHOTO));
        binding.cardChooseAlbum.setOnClickListener(v -> handleAction(PendingAction.PICK_IMAGE));
        binding.cardListenSound.setOnClickListener(v -> startSoundRecognitionActivity());

        return view;
    }

    /**
     * 统一处理需要权限的操作。
     *
     * @param action 要执行的动作（拍照或选择图片）。
     */
    private void handleAction(PendingAction action) {
        if (hasRequiredPermissions()) {
            switch (action) {
                case TAKE_PHOTO:
                    dispatchTakePictureIntent();
                    break;
                case PICK_IMAGE:
                    dispatchPickImageIntent();
                    break;
            }
        } else {
            pendingAction = action;
            requestPermissionsLauncher.launch(new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        }
    }

    /**
     * 检查应用是否已被授予必要的权限。
     *
     * @return 如果相机和存储权限都已授予，则返回 true；否则返回 false。
     */
    private boolean hasRequiredPermissions() {
        boolean hasCamera = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean hasStorage = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return hasCamera && hasStorage;
    }

    /**
     * 初始化权限请求启动器。
     * <p>定义当用户对权限请求做出响应后的回调逻辑。</p>
     */
    private void initializePermissionsLauncher() {
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    // 检查所有请求的权限是否都已授予
                    boolean allGranted = true;
                    for (Boolean isGranted : permissions.values()) {
                        if (!isGranted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        handleAction(pendingAction);
                    } else {
                        Toast.makeText(getContext(), "相机和存储权限是必需的", Toast.LENGTH_SHORT).show();
                    }
                    pendingAction = PendingAction.NONE;
                });
    }

    /**
     * 初始化拍照启动器。
     * <p>定义拍照成功或失败后的回调逻辑。</p>
     */
    private void initializeTakePictureLauncher() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (currentPhotoUri != null) {
                            startImageRecognitionActivity(currentPhotoUri);
                        } else {
                            Toast.makeText(getContext(), "拍照完成，但图片URI丢失。", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "拍照已取消", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 初始化相册选择启动器。
     * <p>定义选择图片成功或失败后的回调逻辑。</p>
     */
    private void initializePickImageLauncher() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            startImageRecognitionActivity(selectedImageUri);
                        } else {
                        }
                    } else {
                        Toast.makeText(getContext(), "未选择图片", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 创建一个唯一的、用于存储照片的临时文件。
     *
     * @return 返回创建的空图像文件。
     * @throws IOException 如果创建文件时发生I/O错误。
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
    }

    /**
     * 构造并分发一个用于启动系统相机的意图（Intent）。
     * <p>它会先创建一个文件来存放照片，然后将文件的URI作为 {@link MediaStore#EXTRA_OUTPUT} 传递给相机应用。</p>
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                String authority = requireContext().getPackageName() + ".fileprovider";
                currentPhotoUri = FileProvider.getUriForFile(requireContext(), authority, photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                takePictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(getContext(), "未找到可用的相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 分发一个用于启动系统相册的意图（Intent）。
     */
    private void dispatchPickImageIntent() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickImageIntent);
    }

    /**
     * 启动图片识别 Activity。
     *
     * @param imageUri 要进行识别的图片的 URI。
     */
    private void startImageRecognitionActivity(Uri imageUri) {
        Intent intent = new Intent(getActivity(), ImageRecognitionActivity.class);
        intent.setData(imageUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    /**
     * 启动声音识别 Activity。
     */
    private void startSoundRecognitionActivity() {
        Intent intent = new Intent(getActivity(), SoundRecognitionActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}