package com.rainkaze.birdwatcher.model;

import lombok.Data;

@Data
public class RecognitionResult {
    private String birdName;
    private float confidenceScore; // 0.0 to 1.0
    private String details; // 其他相关信息，如鸟类百科链接等

    public RecognitionResult(String birdName, float confidenceScore, String details) {
        this.birdName = birdName;
        this.confidenceScore = confidenceScore;
        this.details = details;
    }

}