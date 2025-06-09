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
import com.rainkaze.birdwatcher.util.ImageUtil;

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
    private final BaiduApiClient baiduApiClient;

    public interface IdentificationCallback {
        void onSuccess(List<RecognitionResult> results);
        void onError(String error);
    }

    public BirdIdentificationService(Context context) {
        this.context = context.getApplicationContext();
        this.baiduApiClient = new BaiduApiClient(this.context);
    }

    public void identifyBirdFromImage(Uri imageUri, IdentificationCallback callback) {
        executorService.submit(() -> {
            try {
                String urlEncodedImage = ImageUtil.uriToBase64UrlEncoded(context, imageUri);

                String jsonResponse = baiduApiClient.recognizeAnimal(urlEncodedImage, 5, 1);
                BaiduAnimalApiResponse apiResponse = gson.fromJson(jsonResponse, BaiduAnimalApiResponse.class);

                if (apiResponse.getErrorMsg() != null) {
                    throw new IOException("API Error: " + apiResponse.getErrorCode() + " - " + apiResponse.getErrorMsg());
                }

                if (apiResponse.getResult() == null || apiResponse.getResult().isEmpty()) {
                    mainThreadHandler.post(() -> callback.onSuccess(new ArrayList<>()));
                    return;
                }

                List<RecognitionResult> recognitionResults = new ArrayList<>();
                for (BaiduAnimalResult baiduResult : apiResponse.getResult()) {
                    float score = 0.0f;
                    try {
                        score = Float.parseFloat(baiduResult.getScore());
                    } catch (NumberFormatException e) {
                    }

                    String details = null;
                    String baikeLink = null;
                    if (baiduResult.getBaikeInfo() != null) {
                        if (baiduResult.getBaikeInfo().getDescription() != null) {
                            details = baiduResult.getBaikeInfo().getDescription();
                        }
                        if (baiduResult.getBaikeInfo().getBaikeUrl() != null) {
                            baikeLink = baiduResult.getBaikeInfo().getBaikeUrl();
                        }
                    }
                    recognitionResults.add(new RecognitionResult(baiduResult.getName(), score, details, baikeLink));
                }
                mainThreadHandler.post(() -> callback.onSuccess(recognitionResults));

            } catch (FileNotFoundException e) {
                mainThreadHandler.post(() -> callback.onError("图片文件未找到"));
            } catch (UnsupportedEncodingException e) {
                mainThreadHandler.post(() -> callback.onError("图像编码错误"));
            } catch (IOException e) {
                mainThreadHandler.post(() -> callback.onError("识别服务通讯或图像处理错误: " + e.getMessage()));
            } catch (JsonSyntaxException e) {
                mainThreadHandler.post(() -> callback.onError("结果解析失败"));
            } catch (Exception e) {
                mainThreadHandler.post(() -> callback.onError("发生未知错误: " + e.getMessage()));
            }
        });
    }

    /**
     * TODO:模拟从声音识别鸟类 (后续实现)
     */
    public void identifyBirdFromSound(File audioFile, IdentificationCallback callback) {
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