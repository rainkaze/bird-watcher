package com.rainkaze.birdwatcher.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.BirdKnowledgeAdapter;
import com.rainkaze.birdwatcher.model.Bird;
import com.rainkaze.birdwatcher.network.BirdApiService;
import com.rainkaze.birdwatcher.network.RetrofitClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KnowledgeFragment extends Fragment {

    private static final String TAG = "KnowledgeFragment";
    private SearchView searchView;
    private RecyclerView rvBirds;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private BirdKnowledgeAdapter birdAdapter;
    private final List<Bird> allBirds = new ArrayList<>();
    private final List<Bird> filteredBirds = new ArrayList<>();

    private static final String DEFAULT_REGION = "CN";
    private static final int DAYS_BACK = 30;
    private static final int MAX_RESULTS = 50;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge, container, false);
        searchView = view.findViewById(R.id.search_view);
        rvBirds = view.findViewById(R.id.rv_birds);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        setupSearchView();
        setupBirdRecyclerView();
        setupSwipeRefresh();
        loadBirdData();

        return view;
    }

    private void setupSearchView() {
        searchView.setQueryHint("搜索鸟类名称...");
        searchView.setIconifiedByDefault(false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                handleSearchWithDebounce(query.trim());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    showAllBirds();
                } else {
                    handleSearchWithDebounce(newText.trim());
                }
                return true;
            }
        });
    }

    private void handleSearchWithDebounce(String query) {
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> {
            if (query.isEmpty()) {
                showAllBirds();
            } else {
                searchBirds(query);
            }
        };
        handler.postDelayed(searchRunnable, 500);
    }

    private void setupBirdRecyclerView() {
        rvBirds.setLayoutManager(new LinearLayoutManager(requireContext()));
        birdAdapter = new BirdKnowledgeAdapter(filteredBirds);
        birdAdapter.setOnBirdItemClickListener(this::openBirdDetail);
        rvBirds.setAdapter(birdAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadBirdData);
    }

    private void updateEmptyState() {
        if (filteredBirds.isEmpty()) {
            rvBirds.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvBirds.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void loadBirdData() {
        swipeRefreshLayout.setRefreshing(true);
        allBirds.clear();
        filteredBirds.clear();
        birdAdapter.updateData(filteredBirds);
        updateEmptyState();

        BirdApiService apiService = RetrofitClient.getApiService();
        Call<List<Bird>> call = apiService.getRecentObservations(DEFAULT_REGION, DAYS_BACK, MAX_RESULTS);
        call.enqueue(new Callback<List<Bird>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bird>> call, @NonNull Response<List<Bird>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    processBirdList(response.body(), true);
                } else {
                    String errorMsg = "获取鸟类数据失败: ";
                    if (response.code() == 404) {
                        errorMsg += "未找到资源";
                    } else if (response.body() == null || response.body().isEmpty()) {
                        errorMsg += "返回空数据";
                    } else {
                        errorMsg += "错误码: " + response.code();
                    }
                    Log.e(TAG, errorMsg);
                    showToast(errorMsg);
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bird>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "网络请求失败: " + t.getMessage());
                showToast("网络错误: " + t.getMessage());
                updateEmptyState();
            }
        });
    }

    private void searchBirds(String query) {
        if (query.isEmpty()) {
            showAllBirds();
            return;
        }

        swipeRefreshLayout.setRefreshing(true);
        filteredBirds.clear();
        birdAdapter.updateData(filteredBirds);
        updateEmptyState();

        BirdApiService apiService = RetrofitClient.getApiService();
        Call<List<Bird>> call = apiService.searchBirds(query, "zh");
        call.enqueue(new Callback<List<Bird>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bird>> call, @NonNull Response<List<Bird>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    processBirdList(response.body(), false);
                } else {
                    String errorMsg = "搜索失败: ";
                    if (response.code() == 404) {
                        errorMsg += "未找到相关鸟类";
                    } else if (response.body() == null || response.body().isEmpty()) {
                        errorMsg += "无匹配结果";
                    } else {
                        errorMsg += "错误码: " + response.code();
                    }
                    Log.e(TAG, errorMsg);
                    showToast(errorMsg);
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bird>> call, @NonNull Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "搜索请求失败: " + t.getMessage());
                showToast("搜索失败: " + t.getMessage());
                updateEmptyState();
            }
        });
    }

    private void processBirdList(List<Bird> birds, boolean isFullList) {
        if (birds == null || birds.isEmpty()) {
            Log.d(TAG, "处理空鸟列表");
            swipeRefreshLayout.setRefreshing(false);
            updateEmptyState();
            return;
        }

        List<Bird> detailedBirds = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger counter = new AtomicInteger(birds.size());
        BirdApiService apiService = RetrofitClient.getApiService();

        for (Bird bird : birds) {
            Call<Bird> speciesCall = apiService.getSpeciesInfo(bird.getSpeciesCode());
            speciesCall.enqueue(new Callback<Bird>() {
                @Override
                public void onResponse(@NonNull Call<Bird> call, @NonNull Response<Bird> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Bird detailed = response.body();
                        // 使用固定格式的图片URL
                        detailed.setImageUrl("https://cdn.download.ams.birds.cornell.edu/api/v1/asset/" + detailed.getSpeciesCode() + "/320");
                        synchronized (detailedBirds) {
                            detailedBirds.add(detailed);
                        }
                    } else {
                        // 设置默认图片
                        bird.setImageUrl(null);
                        synchronized (detailedBirds) {
                            detailedBirds.add(bird);
                        }
                    }

                    if (counter.decrementAndGet() == 0) {
                        requireActivity().runOnUiThread(() -> {
                            updateFinalBirdList(detailedBirds, isFullList);
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Bird> call, @NonNull Throwable t) {
                    Log.e(TAG, "获取物种信息失败: " + t.getMessage());
                    // 设置默认图片
                    bird.setImageUrl(null);
                    synchronized (detailedBirds) {
                        detailedBirds.add(bird);
                    }

                    if (counter.decrementAndGet() == 0) {
                        requireActivity().runOnUiThread(() -> {
                            updateFinalBirdList(detailedBirds, isFullList);
                        });
                    }
                }
            });
        }
    }

    private void updateFinalBirdList(List<Bird> detailedBirds, boolean isFullList) {
        if (isFullList) {
            allBirds.clear();
            allBirds.addAll(detailedBirds);
            showAllBirds();
        } else {
            filteredBirds.clear();
            filteredBirds.addAll(detailedBirds);
            birdAdapter.updateData(filteredBirds);
        }
        updateEmptyState();
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showAllBirds() {
        filteredBirds.clear();
        filteredBirds.addAll(allBirds);
        birdAdapter.updateData(filteredBirds);
        updateEmptyState();
    }

    private void openBirdDetail(Bird bird) {
        if (!isAdded()) return;
        // 打开鸟类详情页
        Toast.makeText(requireContext(), "打开鸟类详情: " + bird.getCommonName(), Toast.LENGTH_SHORT).show();
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}