package com.rainkaze.birdwatcher.service; // 或者 com.rainKaze.birdwatcher.api

import android.content.Context;
import android.util.Log;

import org.json.JSONException; // 确保导入
import org.json.JSONObject;   // 确保导入

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

        Log.d(TAG, "Requesting Baidu Animal API: " + fullUrl);

        try (Response response = httpClient.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) { // 理论上不应该发生，但作为防御性编程
                throw new IOException("Response body is null for URL: " + request.url());
            }

            // 读取响应体一次并保存到字符串
            String responseBodyString = responseBody.string();
            Log.d(TAG, "Animal API Full Response: " + responseBodyString);


            // 1. 首先检查 HTTP 状态码是否为 401 (Unauthorized)，这通常明确表示token问题
            if (response.code() == 401) {
                Log.w(TAG, "Received HTTP 401. Access token likely invalid/expired. Clearing token.");
                tokenManager.clearToken(); // 清除无效token
                throw new IOException("Access Token失效 (HTTP 401), 请重试");
            }

            // 2. 检查响应体中是否包含百度特定的token错误码
            //    这些错误码可能伴随不同的HTTP状态码 (例如 200 OK 但内容是错误JSON，或者 400 Bad Request)
            if (responseBodyString.contains("\"error_code\"")) { // 初步判断是否包含错误码字段
                try {
                    JSONObject jsonObject = new JSONObject(responseBodyString);
                    if (jsonObject.has("error_code")) {
                        int errorCode = jsonObject.getInt("error_code");
                        // 常见的百度AI平台Token相关错误码: 100, 110, 111
                        if (errorCode == 100 || errorCode == 110 || errorCode == 111) {
                            String errorDescription = jsonObject.optString("error_description", "Token related error");
                            Log.w(TAG, "Baidu API error code " + errorCode + " indicates token issue. Clearing token. Message: " + errorDescription);
                            tokenManager.clearToken(); // 清除无效token
                            throw new IOException("Access Token问题 (Baidu error " + errorCode + ": " + errorDescription + "), 请重试");
                        }
                        // 如果是其他 error_code 但 HTTP 请求本身是成功的 (例如参数错误)，后续的 isSuccessful 判断会处理
                        // 或者，如果这个 error_code 也应该导致请求失败，可以在这里抛出异常
                        if (!response.isSuccessful() && jsonObject.has("error_description")) {
                            // 对于非2xx响应码且包含错误描述的情况
                            throw new IOException("动物识别API错误 " + errorCode + ": " + jsonObject.getString("error_description"));
                        }
                    }
                } catch (JSONException e) {
                    // 如果响应体包含 "error_code" 但无法解析为JSON，这可能是一个非标准的错误格式
                    Log.w(TAG, "Could not parse JSON error response despite finding 'error_code': " + responseBodyString, e);
                    // 此时依赖下面的 isSuccessful() 判断
                }
            }

            // 3. 常规的成功/失败判断
            if (!response.isSuccessful()) {
                // 如果到这里，说明不是已知的token错误，而是其他API请求错误
                Log.e(TAG, "Animal API request failed. Code: " + response.code() + ", Message: " + response.message() + ", Body: " + responseBodyString);
                throw new IOException("动物识别API请求失败: " + response.code() + " - " + response.message());
            }

            // 如果执行到这里，说明请求成功，并且没有在响应体中检测到特定的token错误码
            return responseBodyString;
        }
    }
}