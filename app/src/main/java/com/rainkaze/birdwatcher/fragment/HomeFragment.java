package com.rainkaze.birdwatcher.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rainkaze.birdwatcher.R;
import com.rainkaze.birdwatcher.activity.AddEditRecordActivity;
import com.rainkaze.birdwatcher.adapter.BirdStatsAdapter;
import com.rainkaze.birdwatcher.adapter.RecordAdapter;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;
import com.rainkaze.birdwatcher.model.BirdStat;
import com.rainkaze.birdwatcher.service.AppApiClient;
import com.rainkaze.birdwatcher.service.SessionManager;
import com.rainkaze.birdwatcher.util.ImageUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private BirdRecordDao birdRecordDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    private TextView tvTotalRecordsCount, tvUniqueSpeciesCount, tvRecentRecordsTitle, tvHomeNoRecords;
    private Button btnHomeAddRecord, btnHomeIdentifyBird;
    private RecyclerView rvRecentRecords, rvBirdStats;
    private CardView cardBirdStats;
    private RecordAdapter recentRecordAdapter;
    private ImageView ivUserAvatar;
    private LinearLayout layoutUserInfo;
    private TextView tvUsername, tvSyncStatus;
    private SessionManager sessionManager;
    private AppApiClient appApiClient;
    private ActivityResultLauncher<Intent> recordResultLauncher;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        birdRecordDao = new BirdRecordDao(requireContext());
        sessionManager = new SessionManager(requireContext());
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
//        updateUserUI();
        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
//        updateUserUI(); // 每次回到该页面都更新UI
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

        // 新增
        ivUserAvatar = view.findViewById(R.id.iv_user_avatar);
        layoutUserInfo = view.findViewById(R.id.layout_user_info);
        tvUsername = view.findViewById(R.id.tv_username);
        tvSyncStatus = view.findViewById(R.id.tv_sync_status);
    }

    // --- 新增方法: 更新用户面板UI ---
    private void updateUserUI() {
        if (sessionManager.isLoggedIn()) {
            layoutUserInfo.setVisibility(View.VISIBLE);
            tvUsername.setText(sessionManager.getUsername());
            checkUnsyncedRecords();
        } else {
            layoutUserInfo.setVisibility(View.VISIBLE); // 游客模式也显示
            tvUsername.setText("游客模式");
            tvSyncStatus.setText("登录以上传和同步数据");
        }
    }

    // --- 新增方法: 检查未同步记录 ---
    private void checkUnsyncedRecords() {
        executor.execute(() -> {
            birdRecordDao.open();
            final List<BirdRecord> unsynced = birdRecordDao.getUnsyncedRecordsForUser(sessionManager.getUserId());
            birdRecordDao.close();
            handler.post(() -> {
                if (sessionManager.isLoggedIn()) {
                    if (!unsynced.isEmpty()) {
                        tvSyncStatus.setText(unsynced.size() + "条记录待同步");
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
        // --- 修改头像点击事件 ---
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

    // --- 新增方法: 显示用户已登录的操作弹窗 ---
    private void showUserActionsDialog() {
        final CharSequence[] options = {"同步数据", "退出登录", "取消"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("用户操作");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("同步数据")) {
                syncData();
            } else if (options[item].equals("退出登录")) {
                sessionManager.logoutUser();
                updateUserUI();
                loadData(); // 重新加载数据
                Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
            } else if (options[item].equals("取消")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    // --- 新增方法: 显示登录/注册弹窗 ---
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

    // --- 新增方法: 执行登录 ---
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

                    // 登录成功后，立即将本地游客数据归属给该用户
                    birdRecordDao.open();
                    int claimedCount = birdRecordDao.claimGuestRecords(userId);
                    birdRecordDao.close();
                    Log.d(TAG, "Claimed " + claimedCount + " records for user " + userId);

                    // 保存登录状态
                    sessionManager.createLoginSession(userId, remoteUsername, token);

                    handler.post(() -> {
                        Toast.makeText(getContext(), "登录成功", Toast.LENGTH_SHORT).show();
                        updateUserUI();
                        // 登录并认领数据后，立即触发一次同步
                        syncData();
                    });
                } else {
                    handler.post(() -> {
                        try {
                            Toast.makeText(getContext(), "登录失败: " + json.getString("message"), Toast.LENGTH_LONG).show();
                        } catch(Exception e) { /* ignore */ }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Login failed", e);
                handler.post(() -> Toast.makeText(getContext(), "登录请求失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    // --- 新增方法: 执行注册 ---
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

    // --- 新增方法: 同步数据 ---
    private void syncData() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(getContext(), "请先登录以同步数据", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(getContext(), "开始同步...", Toast.LENGTH_SHORT).show();
        tvSyncStatus.setText("同步中...");

        executor.execute(() -> {
            boolean uploadSuccess = false;
            boolean downloadSuccess = false;

            // 1. 上传
            birdRecordDao.open();
            List<BirdRecord> unsyncedRecords = birdRecordDao.getUnsyncedRecordsForUser(sessionManager.getUserId());
            Log.d(TAG, "Found " + unsyncedRecords.size() + " records to upload.");
            if (unsyncedRecords.isEmpty()) {
                uploadSuccess = true;
            } else {
                try {
                    // --- 核心修改：在上传前，转换图片为Base64 ---
                    for(BirdRecord record : unsyncedRecords) {
                        List<String> photoUris = record.getPhotoUris();
                        List<String> processedPhotos = new ArrayList<>();
                        for (String uriString : photoUris) {
                            if (uriString.startsWith("content://")) {
                                // 这是本地URI，需要转换为Base64
                                String base64String = ImageUtil.uriToBase64WithHeader(requireContext(), Uri.parse(uriString));
                                if (base64String != null) {
                                    processedPhotos.add(base64String);
                                }
                            } else {
                                // 这可能已经是http URL了，直接添加
                                processedPhotos.add(uriString);
                            }
                        }
                        record.setPhotoUris(processedPhotos); // 用处理过的数据替换
                    }
                    // ------------------------------------------

                    String jsonRecords = gson.toJson(unsyncedRecords);
                    // 注意: 如果图片过大，这个jsonRecords字符串可能会非常长

                    String uploadResponse = appApiClient.uploadRecords(jsonRecords);
                    Log.d(TAG, "Upload response: " + uploadResponse);
                    JSONObject uploadJson = new JSONObject(uploadResponse);
                    if ("success".equals(uploadJson.getString("status"))) {
                        // 上传成功后，更新本地状态
                        JSONArray syncedIdsJson = uploadJson.optJSONArray("synced_client_ids");
                        if (syncedIdsJson != null) {
                            List<Long> clientIds = new ArrayList<>();
                            for (int i=0; i < syncedIdsJson.length(); i++) clientIds.add(syncedIdsJson.getLong(i));

                            // 将这些记录的同步状态设为1
                            birdRecordDao.updateRecordSyncStatus(clientIds, 1);
                            // 将其中原属于游客的记录，归属给当前用户
                            birdRecordDao.claimGuestRecordsToUser(sessionManager.getUserId(), clientIds);
                        }
                        uploadSuccess = true;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Upload failed in client", e);
                }
            }

            // 2. 下载
            try {
                String downloadResponse = appApiClient.downloadRecords();
                Log.d(TAG, "Download response: " + downloadResponse);
                JSONObject downloadJson = new JSONObject(downloadResponse);
                if ("success".equals(downloadJson.getString("status"))) {
                    Type recordListType = new TypeToken<ArrayList<BirdRecord>>() {}.getType();
                    List<BirdRecord> serverRecords = gson.fromJson(downloadJson.getString("records"), recordListType);

                    // 3. 合并 (使用新的DAO方法)
                    for (BirdRecord serverRecord : serverRecords) {
                        // 使用新的DAO方法，传入整个记录和当前用户ID
                        birdRecordDao.addOrUpdateSyncedRecord(serverRecord, sessionManager.getUserId());
                    }
                    downloadSuccess = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Download/Merge failed in client", e);
            } finally {
                birdRecordDao.close();
            }

            // 4. 刷新UI并给出最终提示
            final boolean finalUploadSuccess = uploadSuccess;
            final boolean finalDownloadSuccess = downloadSuccess;
            handler.post(() -> {
                if(finalUploadSuccess && finalDownloadSuccess) {
                    Toast.makeText(getContext(), "同步完成", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "同步过程出现问题，请稍后重试", Toast.LENGTH_LONG).show();
                }
                loadData(); // 无论成功失败，都刷新一次界面以显示最新状态
            });
        });
    }

    private void loadData() {
        executor.execute(() -> {
            birdRecordDao.open();
            // 核心修改: 总是获取本地所有记录来显示
            List<BirdRecord> allRecords = birdRecordDao.getAllRecords();
            Log.d(TAG, "Loaded " + allRecords.size() + " total local records.");
            birdRecordDao.close();

            List<BirdStat> birdStats = calculateStatsFromRecords(allRecords);
            handler.post(() -> updateUiWithData(allRecords, birdStats));
        });
    }

    // 新增一个基于本地记录计算统计的方法
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
        updateUserUI(); // 每次都先更新用户面板

        if (allRecords == null || allRecords.isEmpty()) {
            tvTotalRecordsCount.setText("0");
            tvUniqueSpeciesCount.setText("0");
            tvHomeNoRecords.setVisibility(View.VISIBLE);
            tvHomeNoRecords.setText("还没有任何记录，快去添加你的第一次发现吧！");
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