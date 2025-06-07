package com.rainkaze.birdwatcher.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BirdRecordDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "BirdWatcher.db";
    public static final int DATABASE_VERSION = 2; // 增加了对 photoUris 和 audioUri 的处理

    public static final String TABLE_RECORDS = "records";
    public static final String COLUMN_ID = "_id"; // 主键通常以下划线开头
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_BIRD_NAME = "bird_name";
    public static final String COLUMN_SCIENTIFIC_NAME = "scientific_name";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_DETAILED_LOCATION = "detailed_location";
    public static final String COLUMN_PHOTO_URIS = "photo_uris"; // 存储 JSON 字符串化的列表
    public static final String COLUMN_AUDIO_URI = "audio_uri";
    public static final String COLUMN_RECORD_DATE_TIMESTAMP = "record_date_timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_BIRD_NAME + " TEXT NOT NULL, " +
                    COLUMN_SCIENTIFIC_NAME + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_DETAILED_LOCATION + " TEXT, " +
                    COLUMN_PHOTO_URIS + " TEXT, " + // 存储照片URI列表的JSON字符串
                    COLUMN_AUDIO_URI + " TEXT, " +
                    COLUMN_RECORD_DATE_TIMESTAMP + " INTEGER NOT NULL" +
                    ");";

    public BirdRecordDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 简单的升级策略：删除旧表，创建新表
        // 注意：这会丢失所有现有数据，生产环境中需要更复杂的迁移策略
        if (oldVersion < 2) {
            // 如果是从版本1升级到版本2，可以尝试保留数据并添加新列
            // 这里为了简单，还是直接删除重建
            // 但一个更平滑的升级可能是:
            // db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_PHOTO_URIS + " TEXT;");
            // db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_AUDIO_URI + " TEXT;");
        }
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
        onCreate(db);
    }
}