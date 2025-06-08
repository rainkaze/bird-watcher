package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AppApiClient {

    private static final String TAG = "AppApiClient";
    // !!! 将此URL替换为您的服务器地址 !!!
    private static final String BASE_URL = "http://47.94.105.113/api/";

    private final OkHttpClient httpClient;
    private final SessionManager sessionManager;
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public AppApiClient(Context context) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.sessionManager = new SessionManager(context);
    }

    public String login(String username, String password) throws IOException {
        String json = "{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "login.php")
                .post(body)
                .build();
        return executeRequest(request);
    }

    public String register(String username, String password) throws IOException {
        String json = "{\"username\":\"" + username + "\", \"password\":\"" + password + "\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "register.php")
                .post(body)
                .build();
        return executeRequest(request);
    }

    public String uploadRecords(String recordsJson) throws IOException {
        RequestBody body = RequestBody.create("{\"records\":" + recordsJson + "}", JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "sync.php")
                .header("Authorization", sessionManager.getToken())
                .post(body)
                .build();
        return executeRequest(request);
    }

    public String downloadRecords() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "get_records.php")
                .header("Authorization", sessionManager.getToken())
                .get()
                .build();
        return executeRequest(request);
    }

    private String executeRequest(Request request) throws IOException {
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Request failed: " + response.code() + " " + response.message());
                // 返回完整的响应体，让调用者解析错误信息
                return response.body() != null ? response.body().string() : "{\"status\":\"error\", \"message\":\"Unknown network error\"}";
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}