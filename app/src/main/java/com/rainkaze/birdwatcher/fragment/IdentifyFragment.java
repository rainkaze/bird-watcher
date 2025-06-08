package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment; // 新增导入
import android.provider.MediaStore;
import android.util.Log; // 新增导入
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider; // 新增导入
import androidx.fragment.app.Fragment;

import com.rainkaze.birdwatcher.activity.ImageRecognitionActivity;
import com.rainkaze.birdwatcher.activity.SoundRecognitionActivity;
import com.rainkaze.birdwatcher.databinding.FragmentIdentifyBinding;

import java.io.File; // 新增导入
import java.io.IOException; // 新增导入
import java.text.SimpleDateFormat; // 新增导入
import java.util.Date; // 新增导入
import java.util.Locale; // 新增导入

public class IdentifyFragment extends Fragment {

    private FragmentIdentifyBinding binding;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri currentPhotoUri; // 用于存储拍照的图片URI

    public IdentifyFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // 因为我们使用了 EXTRA_OUTPUT, result.getData() 可能为 null
                        // 我们应该使用之前保存的 currentPhotoUri
                        if (currentPhotoUri != null) {
                            Log.d("IdentifyFragment", "Photo taken, URI: " + currentPhotoUri.toString());
                            startImageRecognitionActivity(currentPhotoUri);
                            // 可选: 使用后清空 currentPhotoUri，以防下次误用
                            // currentPhotoUri = null;
                        } else {
                            // 理论上，如果 EXTRA_OUTPUT 逻辑正确，不应执行到这里
                            Toast.makeText(getContext(), "拍照完成，但图片URI丢失。", Toast.LENGTH_SHORT).show();
                            Log.e("IdentifyFragment", "currentPhotoUri is null after taking picture.");
                        }
                    } else {
                        Toast.makeText(getContext(), "拍照取消或失败", Toast.LENGTH_SHORT).show();
                        // 如果拍照取消或失败，可以考虑删除已创建的空文件
                        if (currentPhotoUri != null) {
                            // File fileToDelete = new File(currentPhotoUri.getPath()); // 注意：Uri.getPath() 可能不直接是文件系统路径
                            // 更可靠的方式是保存 File 对象本身，或者通过 ContentResolver 删除
                            // requireContext().getContentResolver().delete(currentPhotoUri, null, null);
                            // Log.d("IdentifyFragment", "Deleted temp photo file: " + currentPhotoUri.toString());
                        }
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            startImageRecognitionActivity(selectedImageUri);
                        }
                    } else {
                        Toast.makeText(getContext(), "未选择图片", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentIdentifyBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.cardTakePhoto.setOnClickListener(v -> dispatchTakePictureIntent());
        binding.cardChooseAlbum.setOnClickListener(v -> dispatchPickImageIntent());
        binding.cardListenSound.setOnClickListener(v -> startSoundRecognitionActivity());

        return view;
    }

    // 创建图片文件的方法
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        // 或者使用 getExternalCacheDir() 如果只是临时文件
        // File storageDir = requireActivity().getExternalCacheDir();

        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs(); // 确保目录存在
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        Log.d("IdentifyFragment", "Image file created at: " + image.getAbsolutePath());
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // 确保设备上有相机应用可以处理这个Intent
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("IdentifyFragment", "Error occurred while creating the File", ex);
                Toast.makeText(getContext(), "创建图片文件失败", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                // 包名需要和 AndroidManifest.xml 中 provider 的 authorities 一致
                // 通常是 BuildConfig.APPLICATION_ID + ".fileprovider"
                String authority = requireContext().getPackageName() + ".fileprovider";
                currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                        authority,
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);

                // 可选: 授予相机应用临时读写权限 (如果相机应用需要)
                // takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                Log.d("IdentifyFragment", "Launching camera with EXTRA_OUTPUT URI: " + currentPhotoUri.toString());
                takePictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(getContext(), "未找到相机应用", Toast.LENGTH_SHORT).show();
        }
    }

    private void dispatchPickImageIntent() {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(pickImageIntent);
    }

    private void startImageRecognitionActivity(Uri imageUri) {
        Intent intent = new Intent(getActivity(), ImageRecognitionActivity.class);
        intent.setData(imageUri);
        startActivity(intent);
    }

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