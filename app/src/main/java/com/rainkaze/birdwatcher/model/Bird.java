package com.rainkaze.birdwatcher.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class Bird implements Parcelable {

    @SerializedName("编号")
    private String id;

    @SerializedName("学名")
    private String scientificName;

    @SerializedName("中文名")
    private String chineseName;

    @SerializedName("所属目")
    private String order;

    // 新增字段
    @SerializedName("所属科")
    private String family;

    @SerializedName("详情链接")
    private String detailsUrl;

    @SerializedName("IUCN红色名录")
    private String iucnRedList;

    @SerializedName("国家保护等级")
    private String nationalProtectionLevel;

    @SerializedName("鸟类详情")
    private String birdDetails;

    protected Bird(Parcel in) {
        id = in.readString();
        scientificName = in.readString();
        chineseName = in.readString();
        order = in.readString();
        family = in.readString(); // 从Parcel中读取新增字段
        detailsUrl = in.readString();
        iucnRedList = in.readString();
        nationalProtectionLevel = in.readString();
        birdDetails = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(scientificName);
        dest.writeString(chineseName);
        dest.writeString(order);
        dest.writeString(family); // 将新增字段写入Parcel
        dest.writeString(detailsUrl);
        dest.writeString(iucnRedList);
        dest.writeString(nationalProtectionLevel);
        dest.writeString(birdDetails);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Bird> CREATOR = new Creator<Bird>() {
        @Override
        public Bird createFromParcel(Parcel in) {
            return new Bird(in);
        }

        @Override
        public Bird[] newArray(int size) {
            return new Bird[size];
        }
    };

    public String getId() { return id; }
    public String getScientificName() { return scientificName; }
    public String getChineseName() { return chineseName; }
    public String getOrder() { return order; }
    public String getFamily() { return family; } // 新增Getter方法
    public String getDetailsUrl() { return detailsUrl; }
    public String getIucnRedList() { return iucnRedList; }
    public String getNationalProtectionLevel() { return nationalProtectionLevel; }
    public String getBirdDetails() { return birdDetails; }
}