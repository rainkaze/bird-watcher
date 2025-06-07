package com.rainkaze.birdwatcher.model.zoology;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

public class BirdInfo implements Parcelable {
    private String name; // 鸟类中文名
    private String scientificName; // 鸟类学名
    private int imageResourceId; // 本地图片资源ID
    private String description; // 详细描述

    public BirdInfo(String name, String scientificName, int imageResourceId, String description) {
        this.name = name;
        this.scientificName = scientificName;
        this.imageResourceId = imageResourceId;
        this.description = description;
    }

    // Getters
    public String getName() { return name; }
    public String getScientificName() { return scientificName; }
    public int getImageResourceId() { return imageResourceId; }
    public String getDescription() { return description; }

    // Parcelable Implementation
    protected BirdInfo(Parcel in) {
        name = in.readString();
        scientificName = in.readString();
        imageResourceId = in.readInt();
        description = in.readString();
    }

    public static final Creator<BirdInfo> CREATOR = new Creator<BirdInfo>() {
        @Override
        public BirdInfo createFromParcel(Parcel in) {
            return new BirdInfo(in);
        }

        @Override
        public BirdInfo[] newArray(int size) {
            return new BirdInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(scientificName);
        dest.writeInt(imageResourceId);
        dest.writeString(description);
    }
}