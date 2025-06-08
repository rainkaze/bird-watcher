package com.rainkaze.birdwatcher.model.api.baidu;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class BaiduBaikeInfo {
    @SerializedName("baike_url")
    private String baikeUrl;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("description")
    private String description;
}