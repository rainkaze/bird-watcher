package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.AddEditRecordActivity;
import com.rainkaze.birdwatcher.adapter.BirdStatsAdapter;
import com.rainkaze.birdwatcher.adapter.RecordAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;
import com.rainkaze.birdwatcher.model.BirdStat;
import com.rainkaze.birdwatcher.service.AppApiClient;
import com.rainkaze.birdwatcher.service.SessionManager;
import com.rainkaze.birdwatcher.service.SyncManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private BirdRecordDao birdRecordDao;
    private SessionManager sessionManager;
    private SyncManager syncManager;
    private AppApiClient appApiClient;

    private TextView tvTotalRecordsCount, tvUniqueSpeciesCount, tvRecentRecordsTitle, tvHomeNoRecords;
    private Button btnHomeAddRecord, btnHomeIdentifyBird;
    private RecyclerView rvRecentRecords, rvBirdStats;
    private CardView cardBirdStats;
    private RecordAdapter recentRecordAdapter;
    private ImageView ivUserAvatar;
    private LinearLayout layoutUserInfo;
    private TextView tvUsername, tvSyncStatus;
    private ActivityResultLauncher<Intent> recordResultLauncher;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birdRecordDao = new BirdRecordDao(requireContext());
        sessionManager = new SessionManager(requireContext());
        syncManager = new SyncManager(requireContext());
        appApiClient = new AppApiClient(requireContext());

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
    }

    @Override
    public void onResume() {
        super.onResume();
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
        ivUserAvatar = view.findViewById(R.id.iv_user_avatar);
        layoutUserInfo = view.findViewById(R.id.layout_user_info);
        tvUsername = view.findViewById(R.id.tv_username);
        tvSyncStatus = view.findViewById(R.id.tv_sync_status);
    }

    private void loadData() {
        updateUserUI();
        executor.execute(() -> {
            birdRecordDao.open();
            List<BirdRecord> recordsToShow;
            if (sessionManager.isLoggedIn()) {
                // 如果已登录，只显示当前用户的记录
                recordsToShow = birdRecordDao.getAllRecordsForUser(sessionManager.getUserId());
            } else {
                // 如果未登录，显示设备上的所有本地记录
                recordsToShow = birdRecordDao.getAllRecords();
            }
            birdRecordDao.close();

            List<BirdStat> birdStats = calculateStatsFromRecords(recordsToShow);

            handler.post(() -> updateUiWithData(recordsToShow, birdStats));
        });
    }

    private void updateUserUI() {
        if (sessionManager.isLoggedIn()) {
            layoutUserInfo.setVisibility(View.VISIBLE);
            tvUsername.setText(sessionManager.getUsername());
            checkUnsyncedRecords();
        } else {
            layoutUserInfo.setVisibility(View.VISIBLE);
            tvUsername.setText("游客模式");
            tvSyncStatus.setText("登录以同步和备份数据");
        }
    }

    private void checkUnsyncedRecords() {
        executor.execute(() -> {
            birdRecordDao.open();
            final List<BirdRecord> unsynced = birdRecordDao.getUnsyncedRecordsForUser(sessionManager.getUserId());
            birdRecordDao.close();
            handler.post(() -> {
                if (sessionManager.isLoggedIn()) {
                    if (!unsynced.isEmpty()) {
                        tvSyncStatus.setText(unsynced.size() + " 条记录待同步");
                    } else {
                        tvSyncStatus.setText("数据已是最新");
                    }
                }
            });
        });
    }

    private void setupRecyclerViews() {
        rvRecentRecords.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        recentRecordAdapter = new RecordAdapter(getContext(), null, new RecordAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BirdRecord record) {
                Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
                intent.putExtra(AddEditRecordActivity.EXTRA_RECORD_ID, record.getId());
                recordResultLauncher.launch(intent);
            }
            @Override
            public void onItemLongClick(BirdRecord record, View anchorView) {}
        });
        rvRecentRecords.setAdapter(recentRecordAdapter);

        rvBirdStats.setLayoutManager(new LinearLayoutManager(getContext()));
        rvBirdStats.setNestedScrollingEnabled(false);
    }

    private void setupClickListeners() {
        ivUserAvatar.setOnClickListener(v -> {
            if (sessionManager.isLoggedIn()) {
                showUserActionsDialog();
            } else {
                showLoginRegisterDialog();
            }
        });

        btnHomeAddRecord.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditRecordActivity.class);
            recordResultLauncher.launch(intent);
        });

        btnHomeIdentifyBird.setOnClickListener(v -> {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.navigation_identify);
            }
        });
    }

    private void showUserActionsDialog() {
        final CharSequence[] options = {"同步数据", "退出登录", "取消"};
        new AlertDialog.Builder(requireContext())
                .setTitle("用户操作")
                .setItems(options, (dialog, item) -> {
                    String option = options[item].toString();
                    if ("同步数据".equals(option)) {
                        performSync();
                    } else if ("退出登录".equals(option)) {
                        sessionManager.logoutUser();
                        loadData();
                        Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void performSync() {
        tvSyncStatus.setText("同步中...");
        Toast.makeText(getContext(), "开始同步...", Toast.LENGTH_SHORT).show();
        syncManager.syncData(new SyncManager.SyncCallback() {
            @Override
            public void onSyncSuccess() {
                Toast.makeText(getContext(), "同步完成", Toast.LENGTH_SHORT).show();
                loadData();
            }

            @Override
            public void onSyncFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                checkUnsyncedRecords();
            }
        });
    }

    private void showLoginRegisterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_user_panel, null);
        builder.setView(dialogView);
        builder.setTitle("登录或注册");

        final TextInputEditText etUsername = dialogView.findViewById(R.id.et_dialog_username);
        final TextInputEditText etPassword = dialogView.findViewById(R.id.et_dialog_password);

        builder.setPositiveButton("登录", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if(validateInput(username, password)) {
                performLogin(username, password);
            }
        });
        builder.setNegativeButton("注册", (dialog, which) -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if(validateInput(username, password)) {
                performRegister(username, password);
            }
        });
        builder.setNeutralButton("取消", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private boolean validateInput(String username, String password) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void performLogin(String username, String password) {
        Toast.makeText(getContext(), "登录中...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                String response = appApiClient.login(username, password);
                final JSONObject json = new JSONObject(response);
                if ("success".equals(json.getString("status"))) {
                    long userId = json.getLong("userId");
                    String token = json.getString("token");
                    String remoteUsername = json.getString("username");

                    birdRecordDao.open();
                    int claimedCount = birdRecordDao.claimGuestRecords(userId);
                    birdRecordDao.close();

                    sessionManager.createLoginSession(userId, remoteUsername, token);

                    handler.post(() -> {
                        Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                        // 登录成功后自动同步一次
                        performSync();
                    });
                } else {
                    handler.post(() -> {
                        try {
                            Toast.makeText(getContext(), "登录失败: " + json.getString("message"), Toast.LENGTH_LONG).show();
                        } catch(Exception e) {  }
                    });
                }
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(getContext(), "登录请求失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void performRegister(String username, String password) {
        Toast.makeText(getContext(), "注册中...", Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                String response = appApiClient.register(username, password);
                JSONObject json = new JSONObject(response);
                handler.post(() -> {
                    try {
                        if ("success".equals(json.getString("status"))) {
                            Toast.makeText(getContext(), "注册成功，请登录", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "注册失败: " + json.getString("message"), Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "解析注册响应失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                handler.post(() -> Toast.makeText(getContext(), "注册请求失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private List<BirdStat> calculateStatsFromRecords(List<BirdRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        return records.stream()
                .collect(Collectors.groupingBy(BirdRecord::getBirdName, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new BirdStat(entry.getKey(), entry.getValue().intValue()))
                .sorted((s1, s2) -> Integer.compare(s2.getCount(), s1.getCount()))
                .collect(Collectors.toList());
    }

    private void updateUiWithData(List<BirdRecord> allRecords, List<BirdStat> birdStats) {
        if (allRecords == null || allRecords.isEmpty()) {
            tvTotalRecordsCount.setText("0");
            tvUniqueSpeciesCount.setText("0");
            tvHomeNoRecords.setVisibility(View.VISIBLE);
            tvRecentRecordsTitle.setVisibility(View.GONE);
            rvRecentRecords.setVisibility(View.GONE);
            cardBirdStats.setVisibility(View.GONE);
        } else {
            tvHomeNoRecords.setVisibility(View.GONE);
            tvTotalRecordsCount.setText(String.valueOf(allRecords.size()));
            tvUniqueSpeciesCount.setText(String.valueOf(birdStats.size()));

            List<BirdRecord> recentRecords = allRecords.stream().limit(5).collect(Collectors.toList());
            recentRecordAdapter.setRecords(recentRecords);
            tvRecentRecordsTitle.setVisibility(View.VISIBLE);
            rvRecentRecords.setVisibility(View.VISIBLE);

            if (birdStats.isEmpty()) {
                cardBirdStats.setVisibility(View.GONE);
            } else {
                List<BirdStat> topStats = birdStats.stream().limit(5).collect(Collectors.toList());
                BirdStatsAdapter birdStatsAdapter = new BirdStatsAdapter(topStats);
                rvBirdStats.setAdapter(birdStatsAdapter);
                cardBirdStats.setVisibility(View.VISIBLE);
            }
        }
    }
}