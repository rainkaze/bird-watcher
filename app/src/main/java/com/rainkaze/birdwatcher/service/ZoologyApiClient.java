package com.rainkaze.birdwatcher.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rainkaze.birdwatcher.R;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ZoologyApiClient {

    private static final String TAG = "BirdDetailDebug"; // Using a consistent tag for easy filtering
    private final OkHttpClient client;
    private final String apiKey;
    private final String dbaseName;
    private final String baseUrl;

    public interface ApiResponseCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public ZoologyApiClient(Context context) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.apiKey = context.getString(R.string.zoology_api_key);
        this.dbaseName = context.getString(R.string.zoology_db_name);
        this.baseUrl = context.getString(R.string.zoology_api_base_url);
    }

    private Request buildPostRequest(String path, RequestBody formBody) {
        HttpUrl url = Objects.requireNonNull(HttpUrl.parse(baseUrl + path)).newBuilder().build();
        return new Request.Builder()
                .url(url)
                .post(formBody)
                .build();
    }

    public void getDescriptionTypes(String scientificName, ApiResponseCallback<Map<String, String>> callback) {
        RequestBody formBody = new FormBody.Builder()
                .add("scientificName", scientificName)
                .add("dbaseName", dbaseName)
                .add("apiKey", apiKey)
                .build();
        Request request = buildPostRequest("/api/v1/descriptionType", formBody);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        throw new IOException("Request failed with code: " + response.code());
                    }
                    String json = body.string();
                    Log.d(TAG, "getDescriptionTypes RAW RESPONSE: " + json); // LOGGING

                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    if (jsonObject.get("code").getAsInt() != 200) {
                        throw new IOException("API error: " + jsonObject.get("message").getAsString());
                    }
                    JsonArray desTypeArray = jsonObject.getAsJsonObject("data").getAsJsonArray("desType");

                    Map<String, String> resultMap = new LinkedHashMap<>();
                    for (JsonElement element : desTypeArray) {
                        JsonObject obj = element.getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                            resultMap.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                    Log.d(TAG, "getDescriptionTypes: Found " + resultMap.size() + " description types."); // LOGGING
                    callback.onSuccess(resultMap);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public void getDescriptionContent(String scientificName, String desTypeId, ApiResponseCallback<String> callback) {
        RequestBody formBody = new FormBody.Builder()
                .add("scientificName", scientificName)
                .add("descriptionType", desTypeId)
                .add("dbaseName", dbaseName)
                .add("apiKey", apiKey)
                .build();
        Request request = buildPostRequest("/api/v1/description", formBody);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        throw new IOException("Request failed with code: " + response.code());
                    }
                    String json = body.string();
                    Log.d(TAG, "getDescriptionContent RAW RESPONSE for type " + desTypeId + ": " + json); // LOGGING

                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    if (jsonObject.get("code").getAsInt() != 200) {
                        throw new IOException("API error: " + jsonObject.get("message").getAsString());
                    }

                    JsonArray descInfoArray = jsonObject.getAsJsonObject("data").getAsJsonArray("DescriptionInfo");

                    // **BUG FIX #2**: Handle empty content as a success case, not an error.
                    if (descInfoArray == null || descInfoArray.size() == 0) {
                        callback.onSuccess(""); // Return an empty string to signify "no content".
                        return;
                    }

                    StringBuilder contentBuilder = new StringBuilder();
                    for(JsonElement item : descInfoArray) {
                        contentBuilder.append(item.getAsJsonObject().get("descontent").getAsString());
                        contentBuilder.append("\n\n");
                    }

                    String description = contentBuilder.toString()
                            .replaceAll("<p>", "").replaceAll("</p>", "\n")
                            .replaceAll("<br/>", "\n").replaceAll("<br>", "\n")
                            .replaceAll("<[^>]*>", "")
                            .replace("&nbsp;", " ")
                            .trim();
                    callback.onSuccess(description);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }
}