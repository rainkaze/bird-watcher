package com.rainkaze.birdwatcher.model;
import lombok.Data;

@Data
public class RecognitionResult {
    private String birdName;
    private float confidenceScore;
    private String details;
    private String baikeUrl;

    public RecognitionResult(String birdName, float confidenceScore, String details, String baikeUrl) {
        this.birdName = birdName;
        this.confidenceScore = confidenceScore;
        this.details = details;
        this.baikeUrl = baikeUrl;
    }

}