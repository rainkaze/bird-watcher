package com.rainkaze.birdwatcher.fragment;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
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
    private final BirdApiService apiService = RetrofitClient.getApiService();

    public static BirdDetailFragment newInstance(Bird bird) {
        BirdDetailFragment fragment = new BirdDetailFragment();
        Bundle args = new Bundle();
        // Ensure bird is not null before putting it into arguments
        if (bird != null) {
            args.putSerializable(ARG_BIRD, bird);
        }
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

        // Initialize views
        ivBirdImage = view.findViewById(R.id.iv_bird_image);
        tvCommonName = view.findViewById(R.id.tv_common_name);
        tvScientificName = view.findViewById(R.id.tv_scientific_name);
        tvOrder = view.findViewById(R.id.tv_order);
        tvFamily = view.findViewById(R.id.tv_family);
        tvDistribution = view.findViewById(R.id.tv_distribution);
        tvDescription = view.findViewById(R.id.tv_description);
        tvImageCredit = view.findViewById(R.id.tv_image_credit);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle("鸟类详情");
        toolbar.setNavigationIcon(R.drawable.ic_back_arrow); // Ensure you have a back arrow drawable
        toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

        // Check if bird data is valid before proceeding
        if (bird == null || bird.getSpeciesCode() == null) {
            showToast("加载鸟类数据失败");
            getParentFragmentManager().popBackStack(); // Go back if data is invalid
            return view;
        }

        displayInitialInfo();
        loadFullBirdDetails();

        return view;
    }

    private void displayInitialInfo() {
        tvCommonName.setText(bird.getCommonName() != null ? bird.getCommonName() : "加载中...");
        tvScientificName.setText(bird.getScientificName() != null ? bird.getScientificName() : "加载中...");

        // Use the image URL if it was passed along, otherwise construct a default one
        String imageUrl = bird.getImageUrl() != null && !bird.getImageUrl().isEmpty()
                ? bird.getImageUrl()
                : "https://cdn.download.ams.birds.cornell.edu/api/v1/asset/" + bird.getSpeciesCode() + "/1200";

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_bird_placeholder)
                .error(R.drawable.ic_bird_error)
                .into(ivBirdImage);
    }

    private void loadFullBirdDetails() {
        loadSpeciesInfo();
        loadBirdMedia();
        loadBirdDescription();
    }

    private void loadSpeciesInfo() {
        apiService.getSpeciesInfo(bird.getSpeciesCode()).enqueue(new Callback<Bird>() {
            @Override
            public void onResponse(@NonNull Call<Bird> call, @NonNull Response<Bird> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                Bird detailedBird = response.body();
                tvCommonName.setText(detailedBird.getCommonName());
                tvScientificName.setText(detailedBird.getScientificName());
                tvOrder.setText("目: " + (detailedBird.getOrder() != null ? detailedBird.getOrder() : "未知"));
                tvFamily.setText("科: " + (detailedBird.getFamilyCommonName() != null ? detailedBird.getFamilyCommonName() : "未知"));
                tvDistribution.setText(detailedBird.getLocality() != null ? detailedBird.getLocality() : "全球");
            }

            @Override
            public void onFailure(@NonNull Call<Bird> call, @NonNull Throwable t) {
                if (isAdded()) showToast("加载物种详情失败");
            }
        });
    }

    private void loadBirdMedia() {
        apiService.getBirdMedia(bird.getSpeciesCode(), "photo", "json", "zh", 1).enqueue(new Callback<List<BirdMedia>>() {
            @Override
            public void onResponse(@NonNull Call<List<BirdMedia>> call, @NonNull Response<List<BirdMedia>> response) {
                if (!isAdded() || !response.isSuccessful() || response.body() == null || response.body().isEmpty()) return;
                BirdMedia media = response.body().get(0);
                Glide.with(BirdDetailFragment.this)
                        .load(media.getContentUrl())
                        .placeholder(ivBirdImage.getDrawable())
                        .into(ivBirdImage);

                tvImageCredit.setText("图片来源: " + media.getCreator());
                tvImageCredit.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(@NonNull Call<List<BirdMedia>> call, @NonNull Throwable t) {
                if (isAdded()) Log.e(TAG, "Failed to load bird media", t);
            }
        });
    }

    private void loadBirdDescription() {
        apiService.getBirdDescription(bird.getSpeciesCode(), "zh").enqueue(new Callback<BirdDescription>() {
            @Override
            public void onResponse(@NonNull Call<BirdDescription> call, @NonNull Response<BirdDescription> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getDescriptions() != null && !response.body().getDescriptions().isEmpty()) {
                    tvDescription.setText(response.body().getDescriptions().get(0).getDescription());
                } else if(isAdded()) {
                    tvDescription.setText("暂无中文描述。");
                }
            }

            @Override
            public void onFailure(@NonNull Call<BirdDescription> call, @NonNull Throwable t) {
                if (isAdded()) tvDescription.setText("描述加载失败。");
            }
        });
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}