package com.rainkaze.birdwatcher.model;

// 如果你使用了Lombok，可以继续使用 @Data 注解
// import lombok.Data;

import lombok.Data;

@Data
public class RecognitionResult {
    private String birdName;
    private float confidenceScore;
    private String details; // 用于存储 baike_info.description
    private String baikeUrl; // 新增：用于存储 baike_info.baike_url

    // 构造函数更新
    public RecognitionResult(String birdName, float confidenceScore, String details, String baikeUrl) {
        this.birdName = birdName;
        this.confidenceScore = confidenceScore;
        this.details = details;
        this.baikeUrl = baikeUrl;
    }

}