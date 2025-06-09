package com.rainkaze.birdwatcher.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BirdRecord implements Parcelable {


    @SerializedName("localId")
    private long id = -1;

    @SerializedName(value = "id", alternate = {"clientId"})
    private long clientId;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("birdName")
    private String birdName;

    @SerializedName("scientificName")
    private String scientificName;

    @SerializedName("latitude")
    private double latitude = Double.NaN;

    @SerializedName("longitude")
    private double longitude = Double.NaN;

    @SerializedName("detailedLocation")
    private String detailedLocation;

    @SerializedName("photoUris")
    private List<String> photoUris = new ArrayList<>();

    @SerializedName("audioUri")
    private String audioUri;

    @SerializedName("recordDateTimestamp")
    private long recordDateTimestamp;

    private transient long userId;
    private transient int syncStatus;

    public BirdRecord(String title, String birdName, String content, Date recordDate) {
        this.title = title;
        this.birdName = birdName;
        this.content = content;
        if (recordDate != null) {
            this.recordDateTimestamp = recordDate.getTime();
        }
    }

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

    protected BirdRecord(Parcel in) {
        id = in.readLong();
        clientId = in.readLong();
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
        userId = in.readLong();
        syncStatus = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeLong(clientId);
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
        dest.writeLong(userId);
        dest.writeInt(syncStatus);
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
    public String toString() {
        return "BirdRecord{" +
                "id=" + id +
                ", clientId=" + clientId +
                ", title='" + title + '\'' +
                ", birdName='" + birdName + '\'' +
                '}';
    }
}