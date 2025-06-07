package com.rainkaze.birdwatcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.model.Bird;
import com.rainkaze.birdwatcher.model.BirdDescription;
import com.rainkaze.birdwatcher.model.BirdMedia;
import com.rainkaze.birdwatcher.network.BirdApiService;
import com.rainkaze.birdwatcher.network.RetrofitClient;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BirdDetailFragment extends Fragment {

    private static final String ARG_BIRD = "bird";

    private Bird bird;
    private CircleImageView ivBirdImage;
    private TextView tvCommonName, tvScientificName, tvOrder, tvFamily,
            tvDistribution, tvDescription, tvImageCredit;

    public static BirdDetailFragment newInstance(Bird bird) {
        BirdDetailFragment fragment = new BirdDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_BIRD, bird);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bird = (Bird) getArguments().getSerializable(ARG_BIRD);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bird_detail, container, false);

        // 初始化视图组件
        ivBirdImage = view.findViewById(R.id.iv_bird_image);
        tvCommonName = view.findViewById(R.id.tv_common_name);
        tvScientificName = view.findViewById(R.id.tv_scientific_name);
        tvOrder = view.findViewById(R.id.tv_order);
        tvFamily = view.findViewById(R.id.tv_family);
        tvDistribution = view.findViewById(R.id.tv_distribution);
        tvDescription = view.findViewById(R.id.tv_description);
        tvImageCredit = view.findViewById(R.id.tv_image_credit);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // 设置基本信息
        if (bird != null) {
            // 先显示已有信息
            displayBasicInfo();
            // 然后加载详细信息
            loadBirdDetails();
        }

        return view;
    }

    private void displayBasicInfo() {
        tvCommonName.setText(bird.getCommonName() != null ? bird.getCommonName() : "未知");
        tvScientificName.setText(bird.getScientificName() != null ? bird.getScientificName() : "未知");

        // 尝试加载图片
        if (bird.getImageUrl() != null && !bird.getImageUrl().isEmpty()) {
            Glide.with(requireContext())
                    .load(bird.getImageUrl())
                    .placeholder(R.drawable.ic_bird_placeholder)
                    .error(R.drawable.ic_bird_error)
                    .into(ivBirdImage);
        } else {
            // 使用默认图片
            Glide.with(requireContext())
                    .load(R.drawable.ic_bird_placeholder)
                    .into(ivBirdImage);
        }
    }

    private void loadBirdDetails() {
        BirdApiService apiService = RetrofitClient.getApiService();

        // 获取物种详细信息
        apiService.getSpeciesInfo(bird.getSpeciesCode()).enqueue(new Callback<Bird>() {
            @Override
            public void onResponse(Call<Bird> call, Response<Bird> response) {
                if (response.isSuccessful() && response.body() != null) {
                    bird = response.body();
                    updateDetailsUI();
                }
            }

            @Override
            public void onFailure(Call<Bird> call, Throwable t) {
                showToast("加载详情失败: " + t.getMessage());
            }
        });

        // 同时加载描述
        loadBirdDescription();
        // 加载更高质量的图片
        loadBirdImage();
    }

    private void updateDetailsUI() {
        tvCommonName.setText(bird.getCommonName() != null ? bird.getCommonName() : "未知");
        tvScientificName.setText(bird.getScientificName() != null ? bird.getScientificName() : "未知");
        tvOrder.setText("目: " + (bird.getOrder() != null ? bird.getOrder() : "未知"));
        tvFamily.setText("科: " + (bird.getFamilyCommonName() != null ? bird.getFamilyCommonName() : "未知"));

        String locality = bird.getLocality() != null ? bird.getLocality() : "未知";
        tvDistribution.setText("分布区域: " + locality);
    }

    private void loadBirdImage() {
        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.getBirdMedia(bird.getSpeciesCode(), "photo", "json", "zh", 1)
                .enqueue(new Callback<List<BirdMedia>>() {
                    @Override
                    public void onResponse(Call<List<BirdMedia>> call, Response<List<BirdMedia>> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            BirdMedia media = response.body().get(0);
                            String imageUrl = media.getContentUrl();

                            // 加载图片
                            Glide.with(requireContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_bird_placeholder)
                                    .error(R.drawable.ic_bird_error)
                                    .into(ivBirdImage);

                            // 设置图片版权信息
                            String credit = "图片来源: " + media.getCreator() + " / " + media.getRightsHolder();
                            tvImageCredit.setText(credit);
                            tvImageCredit.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BirdMedia>> call, Throwable t) {
                        if (isAdded()) {
                            showToast("获取图片失败: " + t.getMessage());
                        }
                    }
                });
    }

    private void loadBirdDescription() {
        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.getBirdDescription(bird.getSpeciesCode(), "zh")
                .enqueue(new Callback<BirdDescription>() {
                    @Override
                    public void onResponse(Call<BirdDescription> call, Response<BirdDescription> response) {
                        if (isAdded() && response.isSuccessful() && response.body() != null &&
                                response.body().getDescriptions() != null &&
                                !response.body().getDescriptions().isEmpty()) {

                            // 使用第一个描述
                            BirdDescription.DescriptionItem descriptionItem = response.body().getDescriptions().get(0);
                            tvDescription.setText(descriptionItem.getDescription());
                        } else if (isAdded()) {
                            // 使用默认描述
                            String defaultDescription = generateDefaultDescription(bird);
                            tvDescription.setText(defaultDescription);
                        }
                    }

                    @Override
                    public void onFailure(Call<BirdDescription> call, Throwable t) {
                        if(isAdded()) {
                            // 使用默认描述
                            String defaultDescription = generateDefaultDescription(bird);
                            tvDescription.setText(defaultDescription);
                        }
                    }
                });
    }

    private String generateDefaultDescription(Bird bird) {
        StringBuilder description = new StringBuilder();
        description.append(bird.getCommonName()).append(" (").append(bird.getScientificName()).append(") ")
                .append("是").append(bird.getOrder() != null ? bird.getOrder() : "未知").append("目，")
                .append(bird.getFamilyCommonName() != null ? bird.getFamilyCommonName() : "未知").append("科的鸟类。\n\n");

        if (bird.getLocality() != null && !bird.getLocality().isEmpty()) {
            description.append("这种鸟常见于").append(bird.getLocality()).append("，喜欢栖息在森林、湿地或草原等环境中。\n\n");
        }

        description.append("它们主要以昆虫、种子、水果和小型无脊椎动物为食。繁殖季节通常在春季，雌鸟每次产卵2-6枚。");

        return description.toString();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}