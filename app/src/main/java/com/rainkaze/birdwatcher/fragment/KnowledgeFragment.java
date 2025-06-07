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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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

    private static final String TAG = "KnowledgeFragment"; // 用于在Logcat中过滤日志
    private SearchView searchView;
    private RecyclerView rvBirds;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvEmptyState;

    private BirdKnowledgeAdapter birdAdapter;
    private final List<Bird> birdList = new ArrayList<>();
    private final BirdApiService apiService = RetrofitClient.getApiService();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_knowledge, container, false);
        Log.d(TAG, "onCreateView: Fragment view created.");

        // 初始化所有视图
        searchView = view.findViewById(R.id.search_view);
        rvBirds = view.findViewById(R.id.rv_birds);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        setupBirdRecyclerView();
        setupSwipeRefresh();
        setupSearchView();

        // 首次进入时加载数据
        loadInitialData();

        return view;
    }

    private void setupBirdRecyclerView() {
        rvBirds.setLayoutManager(new LinearLayoutManager(requireContext()));
        birdAdapter = new BirdKnowledgeAdapter(birdList);
        birdAdapter.setOnBirdItemClickListener(this::openBirdDetail);
        rvBirds.setAdapter(birdAdapter);
    }

    private void setupSwipeRefresh() {
        // 设置下拉刷新的监听器
        swipeRefreshLayout.setOnRefreshListener(this::loadInitialData);
    }

    private void setupSearchView() {
        searchView.setQueryHint("搜索鸟类 (中/英文)");
        searchView.setIconifiedByDefault(false);
        // 此处我们暂时简化，只处理提交搜索的场景
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query != null && !query.trim().isEmpty()) {
                    searchBirds(query.trim());
                } else {
                    loadInitialData(); // 如果清空后提交，则加载初始数据
                }
                searchView.clearFocus(); // 隐藏键盘
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 暂时留空，以简化逻辑，避免过于频繁的API请求
                return true;
            }
        });
    }

    // 统一的视图状态更新方法
    private void updateViewState(boolean isLoading, boolean isEmpty, @Nullable String message) {
        swipeRefreshLayout.setRefreshing(isLoading); // 控制刷新动画

        if (isEmpty) {
            rvBirds.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
            // 如果有消息，就显示消息，否则显示默认文本
            tvEmptyState.setText(message != null ? message : "暂无鸟类数据\n下拉刷新试试");
        } else {
            rvBirds.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    // 加载初始数据（近期观察记录）
    private void loadInitialData() {
        Log.d(TAG, "loadInitialData: Fetching recent observations...");
        updateViewState(true, false, null); // 显示加载动画
        apiService.getRecentObservations("CN", 30, 100).enqueue(new Callback<List<Bird>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bird>> call, @NonNull Response<List<Bird>> response) {
                if (!isAdded()) return; // 防止Fragment销毁后还更新UI

                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "onResponse: Success! Received " + response.body().size() + " bird records.");
                    birdList.clear();
                    birdList.addAll(response.body());
                    birdAdapter.notifyDataSetChanged();
                    updateViewState(false, birdList.isEmpty(), null);
                } else {
                    Log.e(TAG, "onResponse: API call not successful. Code: " + response.code());
                    updateViewState(false, true, "加载失败, 错误码: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bird>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "onFailure: API call failed.", t);
                updateViewState(false, true, "网络错误, 请检查您的网络连接");
            }
        });
    }

    // 根据名称搜索鸟类
    private void searchBirds(String query) {
        Log.d(TAG, "searchBirds: Searching for '" + query + "'...");
        updateViewState(true, false, null);
        apiService.searchBirds(query, "zh").enqueue(new Callback<List<Bird>>() {
            @Override
            public void onResponse(@NonNull Call<List<Bird>> call, @NonNull Response<List<Bird>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Log.d(TAG, "onResponse: Search successful! Found " + response.body().size() + " results.");
                    birdList.clear();
                    birdList.addAll(response.body());
                    birdAdapter.notifyDataSetChanged();
                    updateViewState(false, birdList.isEmpty(), "未找到关于 \""+query+"\" 的结果");
                } else {
                    Log.e(TAG, "onResponse: Search API call not successful. Code: " + response.code());
                    updateViewState(false, true, "搜索失败, 错误码: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Bird>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "onFailure: Search API call failed.", t);
                updateViewState(false, true, "网络错误, 请检查您的网络连接");
            }
        });
    }

    // 打开鸟类详情页
    private void openBirdDetail(Bird bird) {
        if (!isAdded() || bird == null || bird.getSpeciesCode() == null) {
            Toast.makeText(getContext(), "无法打开详情, 数据不完整", Toast.LENGTH_SHORT).show();
            return;
        }

        BirdDetailFragment fragment = BirdDetailFragment.newInstance(bird);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}