package com.rainkaze.birdwatcher.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rainkaze.birdwatcher.model.BirdRecord;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BirdRecordDao {
    private static final String TAG = "BirdRecordDao";
    private SQLiteDatabase database;
    private BirdRecordDbHelper dbHelper;
    private Gson gson; // 用于序列化/反序列化 List<String>

    public BirdRecordDao(Context context) {
        dbHelper = new BirdRecordDbHelper(context);
        gson = new Gson();
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * 添加一条新的观鸟记录
     * @param record BirdRecord 对象
     * @return 插入记录的 ID，如果失败则返回 -1
     */
    public long addRecord(BirdRecord record) {
        if (database == null || !database.isOpen()) {
            open(); // 确保数据库已打开
        }
        ContentValues values = new ContentValues();
        values.put(BirdRecordDbHelper.COLUMN_TITLE, record.getTitle());
        values.put(BirdRecordDbHelper.COLUMN_BIRD_NAME, record.getBirdName());
        values.put(BirdRecordDbHelper.COLUMN_SCIENTIFIC_NAME, record.getScientificName());
        values.put(BirdRecordDbHelper.COLUMN_CONTENT, record.getContent());
        if (!Double.isNaN(record.getLatitude())) {
            values.put(BirdRecordDbHelper.COLUMN_LATITUDE, record.getLatitude());
        }
        if (!Double.isNaN(record.getLongitude())) {
            values.put(BirdRecordDbHelper.COLUMN_LONGITUDE, record.getLongitude());
        }
        values.put(BirdRecordDbHelper.COLUMN_DETAILED_LOCATION, record.getDetailedLocation());
        values.put(BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP, record.getRecordDateTimestamp());

        // 序列化 photoUris 列表
        if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty()) {
            String photoUrisJson = gson.toJson(record.getPhotoUris());
            values.put(BirdRecordDbHelper.COLUMN_PHOTO_URIS, photoUrisJson);
        } else {
            values.putNull(BirdRecordDbHelper.COLUMN_PHOTO_URIS);
        }
        values.put(BirdRecordDbHelper.COLUMN_AUDIO_URI, record.getAudioUri());

        long insertId = database.insert(BirdRecordDbHelper.TABLE_RECORDS, null, values);
        Log.d(TAG, "Record inserted with ID: " + insertId);
        return insertId;
    }

    /**
     * 更新已存在的观鸟记录
     * @param record BirdRecord 对象，其 ID 必须有效
     * @return 受影响的行数，通常为 1 (成功) 或 0 (失败或记录不存在)
     */
    public int updateRecord(BirdRecord record) {
        if (record.getId() == -1) {
            Log.e(TAG, "Cannot update record with invalid ID: -1");
            return 0; // 不能更新没有有效ID的记录
        }
        if (database == null || !database.isOpen()) {
            open();
        }

        ContentValues values = new ContentValues();
        values.put(BirdRecordDbHelper.COLUMN_TITLE, record.getTitle());
        values.put(BirdRecordDbHelper.COLUMN_BIRD_NAME, record.getBirdName());
        values.put(BirdRecordDbHelper.COLUMN_SCIENTIFIC_NAME, record.getScientificName());
        values.put(BirdRecordDbHelper.COLUMN_CONTENT, record.getContent());
        if (!Double.isNaN(record.getLatitude())) {
            values.put(BirdRecordDbHelper.COLUMN_LATITUDE, record.getLatitude());
        } else {
            values.putNull(BirdRecordDbHelper.COLUMN_LATITUDE);
        }
        if (!Double.isNaN(record.getLongitude())) {
            values.put(BirdRecordDbHelper.COLUMN_LONGITUDE, record.getLongitude());
        } else {
            values.putNull(BirdRecordDbHelper.COLUMN_LONGITUDE);
        }
        values.put(BirdRecordDbHelper.COLUMN_DETAILED_LOCATION, record.getDetailedLocation());
        values.put(BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP, record.getRecordDateTimestamp());

        if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty()) {
            String photoUrisJson = gson.toJson(record.getPhotoUris());
            values.put(BirdRecordDbHelper.COLUMN_PHOTO_URIS, photoUrisJson);
        } else {
            values.putNull(BirdRecordDbHelper.COLUMN_PHOTO_URIS);
        }
        values.put(BirdRecordDbHelper.COLUMN_AUDIO_URI, record.getAudioUri());

        int rowsAffected = database.update(BirdRecordDbHelper.TABLE_RECORDS, values,
                BirdRecordDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(record.getId())});
        Log.d(TAG, "Record updated with ID: " + record.getId() + ". Rows affected: " + rowsAffected);
        return rowsAffected;
    }

    /**
     * 根据 ID 删除一条观鸟记录
     * @param recordId 要删除记录的 ID
     * @return 是否删除成功 (受影响的行数 > 0)
     */
    public boolean deleteRecord(long recordId) {
        if (database == null || !database.isOpen()) {
            open();
        }
        int rowsAffected = database.delete(BirdRecordDbHelper.TABLE_RECORDS,
                BirdRecordDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(recordId)});
        Log.d(TAG, "Record deleted with ID: " + recordId + ". Rows affected: " + rowsAffected);
        return rowsAffected > 0;
    }

    /**
     * 根据 ID 获取一条观鸟记录
     * @param recordId 记录 ID
     * @return BirdRecord 对象，如果未找到则返回 null
     */
    public BirdRecord getRecordById(long recordId) {
        if (database == null || !database.isOpen()) {
            open();
        }
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null, // all columns
                BirdRecordDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(recordId)},
                null, null, null);

        BirdRecord record = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                record = cursorToRecord(cursor);
            }
            cursor.close();
        }
        return record;
    }

    /**
     * 获取所有观鸟记录，按日期降序排列（最新的在前）
     * @return BirdRecord 列表
     */
    public List<BirdRecord> getAllRecords() {
        if (database == null || !database.isOpen()) {
            open();
        }
        List<BirdRecord> records = new ArrayList<>();
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null, // all columns
                null, null, null, null,
                BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP + " DESC"); // 按日期降序

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        Log.d(TAG, "Fetched " + records.size() + " records.");
        return records;
    }

    /**
     * 模糊搜索观鸟记录
     * 匹配标题、内容、鸟名、学名、详细地点
     * @param query 搜索关键词
     * @return 匹配的 BirdRecord 列表，按日期降序
     */
    public List<BirdRecord> searchRecords(String query) {
        if (database == null || !database.isOpen()) {
            open();
        }
        List<BirdRecord> records = new ArrayList<>();
        if (TextUtils.isEmpty(query)) {
            return getAllRecords(); // 如果查询为空，返回所有记录
        }

        String selection = BirdRecordDbHelper.COLUMN_TITLE + " LIKE ? OR " +
                BirdRecordDbHelper.COLUMN_CONTENT + " LIKE ? OR " +
                BirdRecordDbHelper.COLUMN_BIRD_NAME + " LIKE ? OR " +
                BirdRecordDbHelper.COLUMN_SCIENTIFIC_NAME + " LIKE ? OR " +
                BirdRecordDbHelper.COLUMN_DETAILED_LOCATION + " LIKE ?";
        String[] selectionArgs = new String[]{"%" + query + "%", "%" + query + "%", "%" + query + "%", "%" + query + "%", "%" + query + "%"};

        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null, // all columns
                selection,
                selectionArgs,
                null, null,
                BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        Log.d(TAG, "Searched for '" + query + "', found " + records.size() + " records.");
        return records;
    }


    /**
     * 将 Cursor 当前行的数据转换为 BirdRecord 对象
     * @param cursor 数据库游标
     * @return BirdRecord 对象
     */
    private BirdRecord cursorToRecord(Cursor cursor) {
        BirdRecord record = new BirdRecord();
        // 使用 getColumnIndexOrThrow 来确保列名正确，如果列不存在会抛出异常，有助于调试
        record.setId(cursor.getLong(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_ID)));
        record.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_TITLE)));
        record.setBirdName(cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_BIRD_NAME)));
        record.setScientificName(cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_SCIENTIFIC_NAME)));
        record.setContent(cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_CONTENT)));

        // 处理可空的 Double 类型
        int latColIndex = cursor.getColumnIndex(BirdRecordDbHelper.COLUMN_LATITUDE);
        if (latColIndex != -1 && !cursor.isNull(latColIndex)) {
            record.setLatitude(cursor.getDouble(latColIndex));
        } else {
            record.setLatitude(Double.NaN);
        }

        int lonColIndex = cursor.getColumnIndex(BirdRecordDbHelper.COLUMN_LONGITUDE);
        if (lonColIndex != -1 && !cursor.isNull(lonColIndex)) {
            record.setLongitude(cursor.getDouble(lonColIndex));
        } else {
            record.setLongitude(Double.NaN);
        }

        record.setDetailedLocation(cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_DETAILED_LOCATION)));
        record.setRecordDateTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP)));
        record.setAudioUri(cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_AUDIO_URI)));

        // 反序列化 photoUris
        String photoUrisJson = cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_PHOTO_URIS));
        if (!TextUtils.isEmpty(photoUrisJson)) {
            Type listType = new TypeToken<ArrayList<String>>() {}.getType();
            record.setPhotoUris(gson.fromJson(photoUrisJson, listType));
        } else {
            record.setPhotoUris(new ArrayList<>()); //确保列表不为null
        }

        return record;
    }
}