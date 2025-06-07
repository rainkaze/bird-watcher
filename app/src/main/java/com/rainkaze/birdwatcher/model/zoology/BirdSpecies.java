package com.rainkaze.birdwatcher.model.zoology;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

public class BirdSpecies implements Parcelable {

    @SerializedName("scientificName")
    private String scientificName;

    @SerializedName("name")
    private String name;

    // 添加本地图片资源ID字段
    private int imageResourceId;

    // 无参数的构造函数，用于GSON和部分框架
    public BirdSpecies() {}

    // 用于本地数据创建的构造函数
    public BirdSpecies(String name, String scientificName, int imageResourceId) {
        this.name = name;
        this.scientificName = scientificName;
        this.imageResourceId = imageResourceId;
    }

    // --- Getters ---
    public String getScientificName() {
        return scientificName;
    }

    public String getName() {
        return name;
    }

    // 添加缺失的getter方法
    public int getImageResourceId() {
        return imageResourceId;
    }


    // --- Parcelable implementation ---
    protected BirdSpecies(Parcel in) {
        scientificName = in.readString();
        name = in.readString();
        imageResourceId = in.readInt();
    }

    public static final Creator<BirdSpecies> CREATOR = new Creator<BirdSpecies>() {
        @Override
        public BirdSpecies createFromParcel(Parcel in) {
            return new BirdSpecies(in);
        }

        @Override
        public BirdSpecies[] newArray(int size) {
            return new BirdSpecies[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(scientificName);
        dest.writeString(name);
        dest.writeInt(imageResourceId);
    }
}