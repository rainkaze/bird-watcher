package com.rainkaze.birdwatcher.model;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor; // 添加无参构造

@Data
@NoArgsConstructor // Lombok 会生成一个无参构造函数
public class BirdRecord implements Parcelable {
    private long id = -1; // 记录ID，用于数据库操作, -1 表示尚未持久化
    private String title; // 标题
    private String content; // 内容
    private String birdName; // 鸟名
    private String scientificName; // 学名
    private double latitude = Double.NaN; // 纬度
    private double longitude = Double.NaN; // 经度
    private String detailedLocation; // 详细地点
    private List<String> photoUris = new ArrayList<>(); // 照片URI列表 (存储为String)
    private String audioUri; // 音频URI (存储为String)
    private long recordDateTimestamp; // 记录日期 (存储为 long 类型的时间戳)

    // 便捷构造函数 (不含ID)
    public BirdRecord(String title, String birdName, String content, Date recordDate) {
        this.title = title;
        this.birdName = birdName;
        this.content = content;
        if (recordDate != null) {
            this.recordDateTimestamp = recordDate.getTime();
        }
    }

    // Getter 和 Setter for Date (方便使用)
    public Date getRecordDate() {
        return recordDateTimestamp > 0 ? new Date(recordDateTimestamp) : null;
    }

    public void setRecordDate(Date date) {
        if (date != null) {
            this.recordDateTimestamp = date.getTime();
        } else {
            this.recordDateTimestamp = 0;
        }
    }

    // Parcelable 实现
    protected BirdRecord(Parcel in) {
        id = in.readLong();
        title = in.readString();
        content = in.readString();
        birdName = in.readString();
        scientificName = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        detailedLocation = in.readString();
        photoUris = in.createStringArrayList();
        audioUri = in.readString();
        recordDateTimestamp = in.readLong();
    }

    public static final Creator<BirdRecord> CREATOR = new Creator<BirdRecord>() {
        @Override
        public BirdRecord createFromParcel(Parcel in) {
            return new BirdRecord(in);
        }

        @Override
        public BirdRecord[] newArray(int size) {
            return new BirdRecord[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(title);
        dest.writeString(content);
        dest.writeString(birdName);
        dest.writeString(scientificName);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeString(detailedLocation);
        dest.writeStringList(photoUris);
        dest.writeString(audioUri);
        dest.writeLong(recordDateTimestamp);
    }

    // toString() 方法，方便调试
    @Override
    public String toString() {
        return "BirdRecord{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", birdName='" + birdName + '\'' +
                ", recordDate=" + getRecordDate() + // 使用getter
                '}';
    }
}