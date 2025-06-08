package com.rainkaze.birdwatcher.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BirdRecordDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "BirdWatcher.db";
    public static final int DATABASE_VERSION = 3; // <-- 版本升级

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
    // --- 新增字段 ---
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_CLIENT_ID = "client_id";
    public static final String COLUMN_SYNC_STATUS = "sync_status"; // 0:未同步, 1:已同步, 2:待更新

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_RECORDS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
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
                    // --- 添加新列到创建语句 ---
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
        // 升级策略
        if (oldVersion < 2) {
            // 从版本1升级，添加version 2的列
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_PHOTO_URIS + " TEXT;");
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_AUDIO_URI + " TEXT;");
        }
        if (oldVersion < 3) {
            // 从版本2升级，添加version 3的列
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_USER_ID + " INTEGER;");
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_SYNC_STATUS + " INTEGER NOT NULL DEFAULT 0;");
        }if (oldVersion < 4) {
            // 从版本3升级到版本4
            db.execSQL("ALTER TABLE " + TABLE_RECORDS + " ADD COLUMN " + COLUMN_CLIENT_ID + " INTEGER;");
            // 将现有的 _id 复制给 client_id 作为初始值，并设为唯一
            db.execSQL("UPDATE " + TABLE_RECORDS + " SET " + COLUMN_CLIENT_ID + " = " + COLUMN_ID + ";");
            // 注意: 在实际生产环境中，添加 UNIQUE 约束需要更复杂的处理，但对于测试这样是可行的。
        }
    }
}