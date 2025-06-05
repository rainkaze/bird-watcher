package com.rainkaze.birdwatcher.model.api.baidu;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class BaiduAnimalApiResponse {
    @SerializedName("log_id")
    private long logId;

    @SerializedName("result")
    private List<BaiduAnimalResult> result;

    // 用于捕获API层面返回的错误信息
    @SerializedName("error_msg")
    private String errorMsg;

    @SerializedName("error_code")
    private int errorCode;
}