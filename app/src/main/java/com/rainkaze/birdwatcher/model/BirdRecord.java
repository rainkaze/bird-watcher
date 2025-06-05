package com.rainkaze.birdwatcher.model;

import android.net.Uri;
import java.util.List;
import java.util.Date;

import lombok.Data;

@Data
public class BirdRecord {
    private long id; // 记录ID，用于数据库操作
    private String title; // 标题
    private String content; // 内容
    private String birdName; // 鸟名
    private String scientificName; // 学名
    private double latitude = Double.NaN; // 纬度
    private double longitude = Double.NaN; // 经度
    private String detailedLocation; // 详细地点
    private List<String> photoUris; // 照片URI列表 (存储为String)
    private String audioUri; // 音频URI (存储为String)
    private Date recordDate; // 记录日期

    // 构造函数
    public BirdRecord() {
        this.recordDate = new Date(); // 默认为当前时间
    }

    public BirdRecord(String title, String birdName, String content, Date recordDate) {
        this.title = title;
        this.birdName = birdName;
        this.content = content;
        this.recordDate = recordDate;
    }


    // toString() 方法，方便调试
    @Override
    public String toString() {
        return "BirdRecord{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", birdName='" + birdName + '\'' +
                ", recordDate=" + recordDate +
                '}';
    }
}