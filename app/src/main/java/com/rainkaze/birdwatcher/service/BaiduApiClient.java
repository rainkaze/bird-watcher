package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class BaiduApiClient {

    private static final String TAG = "BaiduApiClient";
    private final OkHttpClient httpClient;
    private final TokenManager tokenManager;

    private static final String BAIDU_ANIMAL_API_BASE_URL = "https://aip.baidubce.com/rest/2.0/image-classify/v1/animal";

    public BaiduApiClient(Context context) {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.tokenManager = new TokenManager(context.getApplicationContext());
    }

    public String recognizeAnimal(String urlEncodedImage, int topNum, int baikeNum) throws IOException {
        String accessToken = tokenManager.getValidAccessToken();
        if (accessToken == null) {
            throw new IOException("无法获取有效的 Access Token");
        }

        String requestParams = "image=" + urlEncodedImage +
                "&top_num=" + topNum +
                "&baike_num=" + baikeNum;

        String fullUrl = BAIDU_ANIMAL_API_BASE_URL + "?access_token=" + accessToken;

        RequestBody body = RequestBody.create(requestParams, MediaType.parse("application/x-www-form-urlencoded"));
        Request request = new Request.Builder()
                .url(fullUrl)
                .post(body)
                .build();


        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new IOException("响应信息为空: " + request.url());
            }
            String responseBodyString = responseBody.string();
            if (response.code() == 401) {
                tokenManager.clearToken();
                throw new IOException("Access Token失效 (HTTP 401), 请重试");
            }

            if (responseBodyString.contains("\"error_code\"")) {
                try {
                    JSONObject jsonObject = new JSONObject(responseBodyString);
                    if (jsonObject.has("error_code")) {
                        int errorCode = jsonObject.getInt("error_code");
                        if (errorCode == 100 || errorCode == 110 || errorCode == 111) {
                            String errorDescription = jsonObject.optString("错误：", "Token失效");
                            tokenManager.clearToken();
                            throw new IOException("Access Token问题 (Baidu error " + errorCode + ": " + errorDescription + "), 请重试");
                        }

                        if (!response.isSuccessful() && jsonObject.has("error_description")) {
                            throw new IOException("动物识别API错误 " + errorCode + ": " + jsonObject.getString("error_description"));
                        }
                    }
                } catch (JSONException e) {
                }
            }

            if (!response.isSuccessful()) {
                throw new IOException("动物识别API请求失败: " + response.code() + " - " + response.message());
            }
            return responseBodyString;
        }
    }
}