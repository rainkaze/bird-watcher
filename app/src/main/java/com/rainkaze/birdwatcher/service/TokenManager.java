package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.rainkaze.birdwatcher.config.BaiduApiConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class TokenManager {
    private static final String TAG = "TokenManager";
    private final SharedPreferences sharedPreferences;
    private final OkHttpClient httpClient;
    private static final Object tokenLock = new Object();

    public TokenManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(BaiduApiConfig.PREF_NAME_BAIDU_AUTH, Context.MODE_PRIVATE);
        this.httpClient = new OkHttpClient.Builder().build();
    }

    /**
     * 获取有效的 Access Token。
     * 如果当前存储的token有效，则直接返回。
     * 如果token不存在或已过期，则尝试同步获取新的token。
     *
     * @return 有效的 Access Token，如果获取失败则返回 null。
     */
    public String getValidAccessToken() throws IOException {
        synchronized (tokenLock) {
            String accessToken = sharedPreferences.getString(BaiduApiConfig.KEY_ACCESS_TOKEN, null);
            long expiresAt = sharedPreferences.getLong(BaiduApiConfig.KEY_EXPIRES_AT, 0L);

            if (accessToken != null && System.currentTimeMillis() < (expiresAt - 5 * 60 * 1000)) {
                return accessToken;
            }

            return fetchAndSaveNewToken();
        }
    }

    /**
     * 清除存储的Token信息。
     */
    public void clearToken() {
        synchronized (tokenLock) {
            sharedPreferences.edit()
                    .remove(BaiduApiConfig.KEY_ACCESS_TOKEN)
                    .remove(BaiduApiConfig.KEY_REFRESH_TOKEN)
                    .remove(BaiduApiConfig.KEY_EXPIRES_AT)
                    .remove(BaiduApiConfig.KEY_SCOPE)
                    .apply();
        }
    }


    /**
     * 调用百度API获取新的 Access Token 并保存。
     *
     * @return 新的 Access Token，如果失败则抛出 IOException。
     * @throws IOException 如果网络请求或Token解析失败。
     */
    private String fetchAndSaveNewToken() throws IOException {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(BaiduApiConfig.TOKEN_ENDPOINT_URL)).newBuilder();
        urlBuilder.addQueryParameter("grant_type", BaiduApiConfig.GRANT_TYPE_CLIENT_CREDENTIALS);
        urlBuilder.addQueryParameter("client_id", BaiduApiConfig.API_KEY);
        urlBuilder.addQueryParameter("client_secret", BaiduApiConfig.SECRET_KEY);

        RequestBody body = RequestBody.create("", MediaType.parse("application/json; charset=utf-8")); // 空Body

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (!response.isSuccessful() || responseBody == null) {
                String errorDetails = responseBody != null ? responseBody.string() : "返回值为空";
                throw new IOException("获取Token失败: " + response.code() + " - " + response.message());
            }

            String jsonResponse = responseBody.string();

            try {
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("error")) {
                    String error = jsonObject.getString("error");
                    String errorDescription = jsonObject.optString("error_description", "No description");
                    throw new IOException("Token API error: " + errorDescription);
                }

                String accessToken = jsonObject.getString("access_token");
                long expiresIn = jsonObject.getLong("expires_in");
                String refreshToken = jsonObject.optString("refresh_token", null);
                String scope = jsonObject.optString("scope", null);

                long expiresAtMillis = System.currentTimeMillis() + (expiresIn * 1000L);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(BaiduApiConfig.KEY_ACCESS_TOKEN, accessToken);
                editor.putLong(BaiduApiConfig.KEY_EXPIRES_AT, expiresAtMillis);
                if (refreshToken != null) {
                    editor.putString(BaiduApiConfig.KEY_REFRESH_TOKEN, refreshToken);
                }
                if (scope != null) {
                    editor.putString(BaiduApiConfig.KEY_SCOPE, scope);
                }
                editor.apply();

                return accessToken;

            } catch (JSONException e) {
                throw new IOException("解析Token响应失败: " + e.getMessage());
            }
        }
    }
}