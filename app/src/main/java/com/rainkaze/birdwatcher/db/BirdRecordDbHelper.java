package com.rainkaze.birdwatcher.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BirdRecordDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "BirdWatcher.db";
    // --- 修改: 升级数据库版本以触发 onUpgrade ---
    public static final int DATABASE_VERSION = 4;

    public static final String TABLE_RECORDS = "records";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_BIRD_NAME = "bird_name";
    public static final String COLUMN_SCIENTIFIC_NAME = "scientific_name";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_DETAILED_LOCATION = "detailed_location";
    public static final String COLUMN_PHOTO_URIS = "photo_uris";
    public static final String COLUMN_AUDIO_URI = "audio_uri";
    public static final String COLUMN_RECORD_DATE_TIMESTAMP = "record_date_timestamp";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CLIENT_ID = "client_id"; // 跨设备唯一ID
    public static final String COLUMN_SYNC_STATUS = "sync_status"; // 0:未同步, 1:已同步, 2:本地已修改待更新

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    // --- 修改: 确保 client_id 是唯一的，并且在创建时就存在 ---
                    COLUMN_CLIENT_ID + " INTEGER UNIQUE, " +
                    COLUMN_TITLE + " TEXT NOT NULL, " +
                    COLUMN_BIRD_NAME + " TEXT NOT NULL, " +
                    COLUMN_SCIENTIFIC_NAME + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_LATITUDE + " REAL, " +
                    COLUMN_LONGITUDE + " REAL, " +
                    COLUMN_DETAILED_LOCATION + " TEXT, " +
                    COLUMN_PHOTO_URIS + " TEXT, " +
                    COLUMN_AUDIO_URI + " TEXT, " +
                    COLUMN_RECORD_DATE_TIMESTAMP + " INTEGER NOT NULL," +
                    COLUMN_USER_ID + " INTEGER, " +
                    COLUMN_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0" +
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
        // --- 修改: 采用序贯升级，确保每个版本的改动都被应用 ---
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_PHOTO_URIS + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_AUDIO_URI + " TEXT;");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_USER_ID + " INTEGER;");
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0;");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_CLIENT_ID + " INTEGER;");
            // 将现有的 _id 复制给 client_id 作为初始值
            db.execSQL("UPDATE " + TABLE_RECORDS + " SET " + COLUMN_CLIENT_ID + " = " + COLUMN_ID + ";");
            // 注意: 对已存在的表添加 UNIQUE 约束比较复杂，通常需要创建新表->复制数据->删除旧表->重命名新表。
            // 在此简化处理，假设 client_id 冲突的概率较低。对于生产环境，需要更严谨的数据迁移。
        }
    }
}