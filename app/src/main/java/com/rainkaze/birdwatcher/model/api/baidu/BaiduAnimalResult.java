package com.rainkaze.birdwatcher.model.api.baidu;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class BaiduAnimalResult {
    @SerializedName("name")
    private String name;

    @SerializedName("score")
    private String score;

    @SerializedName("baike_info")
    private BaiduBaikeInfo baikeInfo;
}