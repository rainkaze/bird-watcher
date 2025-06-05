package com.rainkaze.birdwatcher.model;

import com.google.gson.annotations.SerializedName;

// 鸟类图片模型
public class BirdMedia {
    @SerializedName("mediaId")
    private String mediaId;

    @SerializedName("contentUrl")
    private String contentUrl;

    @SerializedName("thumbnailUrl")
    private String thumbnailUrl;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("creator")
    private String creator;

    @SerializedName("rightsHolder")
    private String rightsHolder;

    @SerializedName("licenseUrl")
    private String licenseUrl;

    @SerializedName("type")
    private String type; // "photo" 或 "audio"

    // Getters
    public String getMediaId() { return mediaId; }
    public String getContentUrl() { return contentUrl; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCreator() { return creator; }
    public String getRightsHolder() { return rightsHolder; }
    public String getLicenseUrl() { return licenseUrl; }
    public String getType() { return type; }
}
