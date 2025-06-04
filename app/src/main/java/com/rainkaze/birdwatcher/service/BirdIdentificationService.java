package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rainkaze.birdwatcher.model.RecognitionResult;
import com.rainkaze.birdwatcher.model.api.baidu.BaiduAnimalApiResponse;
import com.rainkaze.birdwatcher.model.api.baidu.BaiduAnimalResult;
import com.rainkaze.birdwatcher.util.ImageUtil; // 导入新的 ImageUtil

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BirdIdentificationService {

    private static final String TAG = "BirdIdService";
    private final Context context;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private final BaiduApiClient baiduApiClient; // 使用新的 BaiduApiClient

    public interface IdentificationCallback {
        void onSuccess(List<RecognitionResult> results);
        void onError(String error);
    }

    public BirdIdentificationService(Context context) {
        this.context = context.getApplicationContext(); // 使用 ApplicationContext 防止内存泄漏
        this.baiduApiClient = new BaiduApiClient(this.context); // 将 context 传递给 BaiduApiClient
    }

    public void identifyBirdFromImage(Uri imageUri, IdentificationCallback callback) {
        executorService.submit(() -> {
            try {
                // 1. 图片处理: URI -> Base64 URL Encoded String
                String urlEncodedImage = ImageUtil.uriToBase64UrlEncoded(context, imageUri);

                // 2. 调用API
                // 返回最可能的5个结果, 为每个结果返回1条百科信息 (百度API行为可能只针对Top1)
                String jsonResponse = baiduApiClient.recognizeAnimal(urlEncodedImage, 5, 1);
                Log.d(TAG, "Baidu API Raw Response: " + jsonResponse);

                // 3. 解析JSON响应
                BaiduAnimalApiResponse apiResponse = gson.fromJson(jsonResponse, BaiduAnimalApiResponse.class);

                if (apiResponse.getErrorMsg() != null) { // 使用 Lombok 生成的 getter
                    throw new IOException("API Error: " + apiResponse.getErrorCode() + " - " + apiResponse.getErrorMsg());
                }

                if (apiResponse.getResult() == null || apiResponse.getResult().isEmpty()) {
                    mainThreadHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                // 4. 转换API结果为 RecognitionResult
                List<RecognitionResult> recognitionResults = new ArrayList<>();
                for (BaiduAnimalResult baiduResult : apiResponse.getResult()) {
                    float score = 0.0f;
                    try {
                        score = Float.parseFloat(baiduResult.getScore());
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Failed to parse score: " + baiduResult.getScore(), e);
                    }

                    String details = null;
                    if (baiduResult.getBaikeInfo() != null && baiduResult.getBaikeInfo().getDescription() != null) {
                        details = baiduResult.getBaikeInfo().getDescription();
                        // 你也可以将 baike_url 和 image_url 拼接到details或 RecognitionResult 的新字段
                        // details += "\n百科链接: " + baiduResult.getBaikeInfo().getBaikeUrl();
                    }
                    recognitionResults.add(new RecognitionResult(baiduResult.getName(), score, details));
                }
                mainThreadHandler.post(() -> callback.onSuccess(recognitionResults));

            } catch (FileNotFoundException e) {
                Log.e(TAG, "Image file not found from URI: " + imageUri, e);
                mainThreadHandler.post(() -> callback.onError("图片文件未找到"));
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "UTF-8 encoding not supported", e);
                mainThreadHandler.post(() -> callback.onError("图像编码错误"));
            } catch (IOException e) { // 包括网络错误和部分图像处理错误
                Log.e(TAG, "IOException during image identification: " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onError("识别服务通讯或图像处理错误: " + e.getMessage()));
            } catch (JsonSyntaxException e) {
                Log.e(TAG, "JSON parsing error", e);
                mainThreadHandler.post(() -> callback.onError("结果解析失败"));
            } catch (Exception e) { // 捕获其他所有未预料的异常
                Log.e(TAG, "Unexpected error during image identification", e);
                mainThreadHandler.post(() -> callback.onError("发生未知错误: " + e.getMessage()));
            }
        });
    }

    /**
     * TODO:模拟从声音识别鸟类 (后续实现)
     */
    public void identifyBirdFromSound(File audioFile, IdentificationCallback callback) {
        Log.w(TAG, "Sound identification is still using mock data.");
        mainThreadHandler.postDelayed(() -> callback.onError("声音识别功能暂未对接真实API"), 1000);
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}