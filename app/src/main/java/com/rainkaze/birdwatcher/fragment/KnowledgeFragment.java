package com.rainkaze.birdwatcher.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import androidx.appcompat.widget.SearchView;

import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.adapter.BirdKnowledgeAdapter;
import com.rainkaze.birdwatcher.model.Bird;
import com.rainkaze.birdwatcher.network.BirdApiService;
import com.rainkaze.birdwatcher.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class KnowledgeFragment extends Fragment {

    private SearchView searchView;
    private RecyclerView rvBirds;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private BirdKnowledgeAdapter birdAdapter;
    private List<Bird> allBirds = new ArrayList<>();
    private List<Bird> filteredBirds = new ArrayList<>();
    private static final String DEFAULT_REGION = "CN";
    private static final int DAYS_BACK = 30;
    private static final int MAX_RESULTS = 50;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
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
        searchView.setIconifiedByDefault(false); // 直接展开搜索框

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchBirds(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    showAllBirds();
                } else {
                    handleSearchWithDebounce(newText);
                }
                return true;
            }
        });
    }

    private void handleSearchWithDebounce(String query) {
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
        }
        searchRunnable = () -> searchBirds(query);
        handler.postDelayed(searchRunnable, 500);
    }

    private void setupBirdRecyclerView() {
        rvBirds.setLayoutManager(new LinearLayoutManager(requireContext()));
        birdAdapter = new BirdKnowledgeAdapter(filteredBirds);
        birdAdapter.setOnBirdItemClickListener(this::openBirdDetail);
        rvBirds.setAdapter(birdAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(true);
            loadBirdData();
        });
    }

    private void updateEmptyState() {
        if (birdAdapter == null || birdAdapter.getItemCount() == 0) {
            rvBirds.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvBirds.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void loadBirdData() {
        swipeRefreshLayout.setRefreshing(true);
        updateEmptyState();

        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.getRecentObservations(DEFAULT_REGION, DAYS_BACK, MAX_RESULTS)
                .enqueue(new Callback<List<Bird>>() {
                    @Override
                    public void onResponse(Call<List<Bird>> call, Response<List<Bird>> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            List<Bird> basicBirds = response.body();
                            allBirds.clear();

                            for (Bird bird : basicBirds) {
                                apiService.getSpeciesInfo(bird.getSpeciesCode())
                                        .enqueue(new Callback<Bird>() {
                                            @Override
                                            public void onResponse(Call<Bird> call, Response<Bird> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                    Bird fullBird = response.body();
                                                    fullBird.setImageUrl(getImageUrl(fullBird.getSpeciesCode()));
                                                    allBirds.add(fullBird);
                                                    showAllBirds(); // 每获取一只鸟就更新一次界面
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<Bird> call, Throwable t) {
                                                // 可选日志打印或提示
                                            }
                                        });
                            }

                        } else {
                            showToast("获取鸟类数据失败: " + response.message());
                        }
                        updateEmptyState();
                    }

                    @Override
                    public void onFailure(Call<List<Bird>> call, Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        showToast("网络错误: " + t.getMessage());
                        updateEmptyState();
                    }
                });
    }

    // 工具方法：用 speciesCode 拼出 eBird 官网图片链接
    private String getImageUrl(String speciesCode) {
        return "https://cdn.download.ams.birds.cornell.edu/api/v1/asset/" + speciesCode + "/180";
    }


    private void searchBirds(String query) {
        if (query.isEmpty()) {
            showAllBirds();
            return;
        }

        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.searchBirds(query, "zh")
                .enqueue(new Callback<List<Bird>>() {
                    @Override
                    public void onResponse(Call<List<Bird>> call, Response<List<Bird>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            filteredBirds.clear();
                            List<Bird> results = response.body();
                            for (Bird bird : results) {
                                apiService.getSpeciesInfo(bird.getSpeciesCode())
                                        .enqueue(new Callback<Bird>() {
                                            @Override
                                            public void onResponse(Call<Bird> call, Response<Bird> response) {
                                                if (response.isSuccessful() && response.body() != null) {
                                                    Bird detailed = response.body();
                                                    detailed.setImageUrl(getImageUrl(detailed.getSpeciesCode()));
                                                    filteredBirds.add(detailed);
                                                    birdAdapter.updateData(filteredBirds);
                                                }
                                            }

                                            @Override
                                            public void onFailure(Call<Bird> call, Throwable t) {}
                                        });
                            }
                        } else {
                            showToast("搜索失败: " + response.message());
                        }
                        updateEmptyState();
                    }

                    @Override
                    public void onFailure(Call<List<Bird>> call, Throwable t) {
                        showToast("网络错误: " + t.getMessage());
                        updateEmptyState();
                    }
                });

    }

    private void showAllBirds() {
        filteredBirds.clear();
        filteredBirds.addAll(allBirds);
        birdAdapter.updateData(filteredBirds);
        updateEmptyState();
    }

    private void openBirdDetail(Bird bird) {
        BirdDetailFragment detailFragment = BirdDetailFragment.newInstance(bird);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
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
