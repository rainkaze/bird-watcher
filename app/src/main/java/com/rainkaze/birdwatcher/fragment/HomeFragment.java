package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.AddEditRecordActivity;
import com.rainkaze.birdwatcher.adapter.BirdStatsAdapter;
import com.rainkaze.birdwatcher.adapter.RecordAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;
import com.rainkaze.birdwatcher.model.BirdStat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private BirdRecordDao birdRecordDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private TextView tvTotalRecordsCount, tvUniqueSpeciesCount, tvRecentRecordsTitle, tvHomeNoRecords;
    private Button btnHomeAddRecord, btnHomeIdentifyBird;
    private RecyclerView rvRecentRecords, rvBirdStats;
    private CardView cardBirdStats;
    private RecordAdapter recentRecordAdapter;
    private BirdStatsAdapter birdStatsAdapter;
    private ActivityResultLauncher<Intent> recordResultLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birdRecordDao = new BirdRecordDao(requireContext());
        // 注册一个启动器，用于从“添加/编辑”页面返回时刷新数据
        recordResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        loadData();
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerViews();
        setupClickListeners();
        loadData();
    }

    private void initializeViews(View view) {
        tvTotalRecordsCount = view.findViewById(R.id.tv_total_records_count);
        tvUniqueSpeciesCount = view.findViewById(R.id.tv_unique_species_count);
        tvRecentRecordsTitle = view.findViewById(R.id.tv_recent_records_title);
        tvHomeNoRecords = view.findViewById(R.id.tv_home_no_records);
        btnHomeAddRecord = view.findViewById(R.id.btn_home_add_record);
        btnHomeIdentifyBird = view.findViewById(R.id.btn_home_identify_bird);
        rvRecentRecords = view.findViewById(R.id.rv_recent_records);
        rvBirdStats = view.findViewById(R.id.rv_bird_stats);
        cardBirdStats = view.findViewById(R.id.card_bird_stats);
    }

    private void setupRecyclerViews() {
        // 近期记录 RecyclerView
        rvRecentRecords.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        // 注意：这里复用 RecordAdapter，但你也可以为首页创建一个更轻量级的适配器
        recentRecordAdapter = new RecordAdapter(getContext(), null, new RecordAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BirdRecord record) {
                Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
                intent.putExtra(AddEditRecordActivity.EXTRA_RECORD_ID, record.getId());
                recordResultLauncher.launch(intent);
            }
            @Override
            public void onItemLongClick(BirdRecord record, View anchorView) {
                // 首页不处理长按
            }
        });
        rvRecentRecords.setAdapter(recentRecordAdapter);

        // 鸟类统计 RecyclerView
        rvBirdStats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBirdStats.setNestedScrollingEnabled(false); // 在NestedScrollView中禁用滚动
    }

    private void setupClickListeners() {
        btnHomeAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
            recordResultLauncher.launch(intent);
        });

        btnHomeIdentifyBird.setOnClickListener(v -> {
            // 跳转到底部导航栏的“识鸟”页面
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_identify);
            }
        });
    }

    private void loadData() {
        executor.execute(() -> {
            birdRecordDao.open();
            List<BirdRecord> allRecords = birdRecordDao.getAllRecords();
            List<BirdStat> birdStats = birdRecordDao.getBirdStats();
            birdRecordDao.close();

            // 在主线程更新UI
            handler.post(() -> updateUiWithData(allRecords, birdStats));
        });
    }

    private void updateUiWithData(List<BirdRecord> allRecords, List<BirdStat> birdStats) {
        if (allRecords == null || allRecords.isEmpty()) {
            // 处理没有任何记录的情况
            tvTotalRecordsCount.setText("0");
            tvUniqueSpeciesCount.setText("0");
            tvHomeNoRecords.setVisibility(View.VISIBLE);
            tvRecentRecordsTitle.setVisibility(View.GONE);
            rvRecentRecords.setVisibility(View.GONE);
            cardBirdStats.setVisibility(View.GONE);
        } else {
            // 有记录，更新UI
            tvHomeNoRecords.setVisibility(View.GONE);
            tvTotalRecordsCount.setText(String.valueOf(allRecords.size()));
            tvUniqueSpeciesCount.setText(String.valueOf(birdStats.size()));

            // 更新近期记录 (最多显示5条)
            List<BirdRecord> recentRecords = allRecords.stream().limit(5).collect(Collectors.toList());
            recentRecordAdapter.setRecords(recentRecords);
            tvRecentRecordsTitle.setVisibility(View.VISIBLE);
            rvRecentRecords.setVisibility(View.VISIBLE);


            // 更新鸟类统计 (最多显示5条)
            List<BirdStat> topStats = birdStats.stream().limit(5).collect(Collectors.toList());
            birdStatsAdapter = new BirdStatsAdapter(topStats);
            rvBirdStats.setAdapter(birdStatsAdapter);
            cardBirdStats.setVisibility(View.VISIBLE);
        }
    }

}