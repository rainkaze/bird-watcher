package com.rainkaze.birdwatcher.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.search.SearchView;
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

    // 用于防抖处理的Handler
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

        // 初始化视图组件
        searchView = view.findViewById(R.id.search_view);
        rvBirds = view.findViewById(R.id.rv_birds);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        // 设置搜索视图
        setupSearchView();

        // 设置鸟类列表RecyclerView
        setupBirdRecyclerView();

        // 设置刷新功能
        setupSwipeRefresh();

        // 加载数据
        loadBirdData();

        return view;
    }

    private void setupSearchView() {
        // 设置提示文本
        searchView.setHint("搜索鸟类名称...");

        // 获取搜索框的EditText
        EditText editText = searchView.getEditText();
        if (editText != null) {
            // 设置回车键搜索
            editText.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    searchBirds(editText.getText().toString());
                    return true;
                }
                return false;
            });

            // 添加文本变化监听
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.toString().isEmpty()) {
                        showAllBirds();
                    } else {
                        // 添加防抖处理的实时搜索
                        handleSearchWithDebounce(s.toString());
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    // 防抖处理的搜索方法
    private void handleSearchWithDebounce(String query) {
        // 取消之前未执行的搜索请求
        if (searchRunnable != null) {
            handler.removeCallbacks(searchRunnable);
        }

        // 创建新的搜索请求
        searchRunnable = () -> searchBirds(query);

        // 延迟500ms执行搜索（防抖）
        handler.postDelayed(searchRunnable, 500);
    }

    private void setupBirdRecyclerView() {
        rvBirds.setLayoutManager(new LinearLayoutManager(requireContext()));
        birdAdapter = new BirdKnowledgeAdapter(new ArrayList<>());
        birdAdapter.setOnBirdItemClickListener(this::openBirdDetail);
        rvBirds.setAdapter(birdAdapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(true);
            loadBirdData();
            // 注意：网络请求完成后才停止刷新
        });
    }

    private void loadBirdData() {
        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.getRecentObservations(DEFAULT_REGION, DAYS_BACK, MAX_RESULTS)
                .enqueue(new Callback<List<Bird>>() {
                    @Override
                    public void onResponse(Call<List<Bird>> call, Response<List<Bird>> response) {
                        swipeRefreshLayout.setRefreshing(false);
                        if (response.isSuccessful() && response.body() != null) {
                            allBirds = response.body();
                            showAllBirds();
                        } else {
                            showToast("获取鸟类数据失败: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Bird>> call, Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        showToast("网络错误: " + t.getMessage());
                    }
                });
    }

    private void searchBirds(String query) {
        BirdApiService apiService = RetrofitClient.getApiService();
        apiService.searchBirds(query, "zh")
                .enqueue(new Callback<List<Bird>>() {
                    @Override
                    public void onResponse(Call<List<Bird>> call, Response<List<Bird>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            filteredBirds = response.body();
                            birdAdapter.updateData(filteredBirds);
                        } else {
                            showToast("搜索失败: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Bird>> call, Throwable t) {
                        showToast("网络错误: " + t.getMessage());
                    }
                });
    }

    private void showAllBirds() {
        filteredBirds.clear();
        filteredBirds.addAll(allBirds);
        birdAdapter.updateData(filteredBirds);
    }

    private void openBirdDetail(Bird bird) {
        // 打开鸟类详情页面
        BirdDetailFragment detailFragment = BirdDetailFragment.newInstance(bird);
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 防止内存泄漏
        handler.removeCallbacksAndMessages(null);
    }
}