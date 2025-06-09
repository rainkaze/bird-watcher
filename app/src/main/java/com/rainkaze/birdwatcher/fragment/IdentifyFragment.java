package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.rainkaze.birdwatcher.activity.ImageRecognitionActivity;
import com.rainkaze.birdwatcher.activity.SoundRecognitionActivity;
import com.rainkaze.birdwatcher.databinding.FragmentIdentifyBinding;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IdentifyFragment extends Fragment {

    private FragmentIdentifyBinding binding;
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private Uri currentPhotoUri;

    public IdentifyFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (currentPhotoUri != null) {
                            Log.d("IdentifyFragment", "Photo taken, URI: " + currentPhotoUri.toString());
                            startImageRecognitionActivity(currentPhotoUri);
                        } else {
                            Toast.makeText(getContext(), "拍照完成，但图片URI丢失。", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "拍照取消或失败", Toast.LENGTH_SHORT).show();
                        if (currentPhotoUri != null) {
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

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return image;
    }

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
                currentPhotoUri = FileProvider.getUriForFile(requireContext(),
                        authority,
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);

                 takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
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