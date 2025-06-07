package com.rainkaze.birdwatcher.model;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Bird implements Serializable {
    @SerializedName(value = "comName", alternate = {"commonName", "name"})
    private String commonName;

    @SerializedName(value = "sciName", alternate = {"scientificName"})
    private String scientificName;
    @SerializedName("speciesCode")
    private String speciesCode;

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

    public Bird() {}

    @Override
    public String toString() {
        return "Bird{" +
                "commonName='" + commonName + '\'' +
                ", scientificName='" + scientificName + '\'' +
                ", speciesCode='" + speciesCode + '\'' +
                '}';
    }
    // Getters and Setters
    public String getSpeciesCode() { return speciesCode; }
    public void setSpeciesCode(String speciesCode) { this.speciesCode = speciesCode; }

    public String getCommonName() { return commonName; }
    public void setCommonName(String commonName) { this.commonName = commonName; }

    public String getScientificName() { return scientificName; }
    public void setScientificName(String scientificName) { this.scientificName = scientificName; }

    public int getTaxonOrder() { return taxonOrder; }
    public void setTaxonOrder(int taxonOrder) { this.taxonOrder = taxonOrder; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTaxonId() { return taxonId; }
    public void setTaxonId(String taxonId) { this.taxonId = taxonId; }

    public String getOrder() { return order; }
    public void setOrder(String order) { this.order = order; }

    public String getFamilyCommonName() { return familyCommonName; }
    public void setFamilyCommonName(String familyCommonName) { this.familyCommonName = familyCommonName; }

    public String getFamilyScientificName() { return familyScientificName; }
    public void setFamilyScientificName(String familyScientificName) { this.familyScientificName = familyScientificName; }

    public String getLocality() { return locality; }
    public void setLocality(String locality) { this.locality = locality; }

    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }

    public String getObservationDate() { return observationDate; }
    public void setObservationDate(String observationDate) { this.observationDate = observationDate; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public boolean isObservationValid() { return observationValid; }
    public void setObservationValid(boolean observationValid) { this.observationValid = observationValid; }

    public boolean isObservationReviewed() { return observationReviewed; }
    public void setObservationReviewed(boolean observationReviewed) { this.observationReviewed = observationReviewed; }

    public boolean isLocationPrivate() { return locationPrivate; }
    public void setLocationPrivate(boolean locationPrivate) { this.locationPrivate = locationPrivate; }

    public String getExoticCategory() { return exoticCategory; }
    public void setExoticCategory(String exoticCategory) { this.exoticCategory = exoticCategory; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}