package com.rainkaze.birdwatcher.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

// 鸟类信息模型
public class Bird implements Serializable {
    @SerializedName("speciesCode")
    private String speciesCode;

    @SerializedName("comName")
    private String commonName;

    @SerializedName("sciName")
    private String scientificName;

    @SerializedName("taxonOrder")
    private int taxonOrder;

    @SerializedName("category")
    private String category;

    @SerializedName("taxonID")
    private String taxonId;

    @SerializedName("order")
    private String order;

    @SerializedName("familyComName")
    private String familyCommonName;

    @SerializedName("familySciName")
    private String familyScientificName;

    @SerializedName("locality")
    private String locality;

    @SerializedName("locId")
    private String locationId;

    @SerializedName("obsDt")
    private String observationDate;

    @SerializedName("howMany")
    private int count;

    @SerializedName("lat")
    private double latitude;

    @SerializedName("lng")
    private double longitude;

    @SerializedName("subId")
    private String submissionId;

    @SerializedName("obsValid")
    private boolean observationValid;

    @SerializedName("obsReviewed")
    private boolean observationReviewed;

    @SerializedName("locationPrivate")
    private boolean locationPrivate;

    @SerializedName("exoticCategory")
    private String exoticCategory;

    private String imageUrl;
    // 构造函数和Getter/Setter方法
    public Bird() {}

    // Getters
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getSpeciesCode() { return speciesCode; }
    public String getCommonName() { return commonName; }
    public String getScientificName() { return scientificName; }
    public int getTaxonOrder() { return taxonOrder; }
    public String getCategory() { return category; }
    public String getTaxonId() { return taxonId; }
    public String getOrder() { return order; }
    public String getFamilyCommonName() { return familyCommonName; }
    public String getFamilyScientificName() { return familyScientificName; }
    public String getLocality() { return locality; }
    public String getLocationId() { return locationId; }
    public String getObservationDate() { return observationDate; }
    public int getCount() { return count; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getSubmissionId() { return submissionId; }
    public boolean isObservationValid() { return observationValid; }
    public boolean isObservationReviewed() { return observationReviewed; }
    public boolean isLocationPrivate() { return locationPrivate; }
    public String getExoticCategory() { return exoticCategory; }
}

