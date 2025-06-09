package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rainkaze.birdwatcher.db.BirdRecordDao;
import com.rainkaze.birdwatcher.model.BirdRecord;
import com.rainkaze.birdwatcher.util.ImageUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SyncManager {

    private static final String TAG = "SyncManager";

    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create();

    private final AppApiClient apiClient;
    private final BirdRecordDao recordDao;
    private final SessionManager sessionManager;

    public interface SyncCallback {
        void onSyncSuccess();
        void onSyncFailure(String message);
    }

    public SyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.apiClient = new AppApiClient(this.context);
        this.recordDao = new BirdRecordDao(this.context);
        this.sessionManager = new SessionManager(this.context);
    }

    public void syncData(final SyncCallback callback) {
        if (!sessionManager.isLoggedIn()) {
            callback.onSyncFailure("请先登录以同步数据");
            return;
        }

        executor.execute(() -> {
            try {
                uploadLocalChanges();
                downloadRemoteRecords();
                handler.post(callback::onSyncSuccess);
            } catch (Exception e) {
                handler.post(() -> callback.onSyncFailure("同步失败: " + e.getMessage()));
            }
        });
    }

    private void uploadLocalChanges() throws Exception {
        recordDao.open();
        List<BirdRecord> unsyncedRecords = recordDao.getUnsyncedRecordsForUser(sessionManager.getUserId());
        recordDao.close();
        if (unsyncedRecords.isEmpty()) {
            return;
        }
        for (BirdRecord record : unsyncedRecords) {
            List<String> photoUris = record.getPhotoUris();
            if (photoUris == null || photoUris.isEmpty()) continue;

            List<String> processedPhotos = new ArrayList<>();
            for (String uriString : photoUris) {
                if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                    String base64String = ImageUtil.uriToBase64WithHeader(context, Uri.parse(uriString));
                    if (base64String != null) {
                        processedPhotos.add(base64String);
                    }
                } else {
                    processedPhotos.add(uriString);
                }
            }
            record.setPhotoUris(processedPhotos);
        }

        String jsonRecordsForUpload = gson.toJson(unsyncedRecords);
        String uploadResponse = apiClient.uploadRecords(jsonRecordsForUpload);
        JSONObject uploadJson;
        try {
            uploadJson = new JSONObject(uploadResponse);
        } catch (org.json.JSONException e) {
            throw new Exception("服务器响应格式错误，请检查后端日志。响应内容: " + uploadResponse);
        }

        if (!"success".equals(uploadJson.getString("status"))) {
            throw new Exception("上传错误: " + uploadJson.optString("message", "Unknown error"));
        }

        JSONArray syncedIdsJson = uploadJson.optJSONArray("synced_client_ids");
        if (syncedIdsJson != null && syncedIdsJson.length() > 0) {
            List<Long> clientIds = new ArrayList<>();
            for (int i = 0; i < syncedIdsJson.length(); i++) {
                clientIds.add(syncedIdsJson.getLong(i));
            }

            recordDao.open();
            recordDao.updateRecordSyncStatus(clientIds, 1);
            recordDao.close();
        }
    }

    private void downloadRemoteRecords() throws Exception {
        String downloadResponse = apiClient.downloadRecords();
        JSONObject downloadJson = new JSONObject(downloadResponse);

        if (!"success".equals(downloadJson.getString("status"))) {
            throw new Exception("Download failed: " + downloadJson.optString("message", "Unknown error"));
        }

        String recordsJson = downloadJson.getString("records");
        Type recordListType = new TypeToken<ArrayList<BirdRecord>>() {}.getType();
        List<BirdRecord> serverRecords = gson.fromJson(recordsJson, recordListType);
        if (serverRecords.isEmpty()) {
            return;
        }
        recordDao.open();
        for (BirdRecord serverRecord : serverRecords) {
            recordDao.addOrUpdateSyncedRecord(serverRecord, sessionManager.getUserId());
        }
        recordDao.close();
    }
}