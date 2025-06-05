package com.rainkaze.birdwatcher.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

        // 设置基本信息
        if (bird != null) {
            loadBirdDetails();
        }

        return view;
    }

    private void loadBirdDetails() {
        // 设置基本信息
        tvCommonName.setText(bird.getCommonName());
        tvScientificName.setText(bird.getScientificName());
        tvOrder.setText("目: " + bird.getOrder());
        tvFamily.setText("科: " + bird.getFamilyCommonName());

        // 设置分布信息
        String distribution = "分布区域: " + bird.getLocality();
        tvDistribution.setText(distribution);

        // 获取图片
        loadBirdImage();

        // 获取描述
        loadBirdDescription();
    }

    private void loadBirdImage() {
        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.getBirdMedia(bird.getSpeciesCode(), "json", "zh", 1)
                .enqueue(new Callback<List<BirdMedia>>() {
                    @Override
                    public void onResponse(Call<List<BirdMedia>> call, Response<List<BirdMedia>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
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
                        } else {
                            // 使用默认图片
                            Glide.with(requireContext())
                                    .load(R.drawable.ic_bird_placeholder)
                                    .into(ivBirdImage);
                            tvImageCredit.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onFailure(Call<List<BirdMedia>> call, Throwable t) {
                        // 使用默认图片
                        Glide.with(requireContext())
                                .load(R.drawable.ic_bird_placeholder)
                                .into(ivBirdImage);
                        tvImageCredit.setVisibility(View.GONE);
                        showToast("获取图片失败: " + t.getMessage());
                    }
                });
    }

    private void loadBirdDescription() {
        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.getBirdDescription(bird.getSpeciesCode(), "zh")
                .enqueue(new Callback<BirdDescription>() {
                    @Override
                    public void onResponse(Call<BirdDescription> call, Response<BirdDescription> response) {
                        if (response.isSuccessful() && response.body() != null &&
                                response.body().getDescriptions() != null &&
                                !response.body().getDescriptions().isEmpty()) {

                            // 使用第一个描述
                            BirdDescription.DescriptionItem descriptionItem = response.body().getDescriptions().get(0);
                            tvDescription.setText(descriptionItem.getDescription());
                        } else {
                            // 使用默认描述
                            String defaultDescription = generateDefaultDescription(bird);
                            tvDescription.setText(defaultDescription);
                        }
                    }

                    @Override
                    public void onFailure(Call<BirdDescription> call, Throwable t) {
                        // 使用默认描述
                        String defaultDescription = generateDefaultDescription(bird);
                        tvDescription.setText(defaultDescription);
                        showToast("获取描述失败: " + t.getMessage());
                    }
                });
    }

    private String generateDefaultDescription(Bird bird) {
        StringBuilder description = new StringBuilder();
        description.append(bird.getCommonName()).append(" (").append(bird.getScientificName()).append(") ")
                .append("是").append(bird.getOrder()).append("目，")
                .append(bird.getFamilyCommonName()).append("科的鸟类。\n\n");

        description.append("这种鸟常见于").append(bird.getLocality()).append("，喜欢栖息在森林、湿地或草原等环境中。\n\n");

        description.append("它们主要以昆虫、种子、水果和小型无脊椎动物为食。繁殖季节通常在春季，雌鸟每次产卵2-6枚。");

        return description.toString();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}