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
import com.rainkaze.birdwatcher.model.BirdStat;

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
     * 将一个 BirdRecord 对象转换为 ContentValues，用于数据库插入或更新。
     * @param record 要转换的记录对象
     * @return 转换后的 ContentValues
     */
    private ContentValues recordToContentValues(BirdRecord record) {
        ContentValues values = new ContentValues();
        // 如果 clientId 有效，则放入
        if (record.getClientId() > 0) {
            values.put(BirdRecordDbHelper.COLUMN_CLIENT_ID, record.getClientId());
        }
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
        values.put(BirdRecordDbHelper.COLUMN_AUDIO_URI, record.getAudioUri());

        // 序列化 photoUris 列表
        if (record.getPhotoUris() != null && !record.getPhotoUris().isEmpty()) {
            String photoUrisJson = gson.toJson(record.getPhotoUris());
            values.put(BirdRecordDbHelper.COLUMN_PHOTO_URIS, photoUrisJson);
        } else {
            values.putNull(BirdRecordDbHelper.COLUMN_PHOTO_URIS);
        }
        return values;
    }

    /**
     * 根据 跨设备唯一ID (clientId) 获取一条记录。
     * @param clientId 记录的唯一ID
     * @return BirdRecord 对象，如果未找到则返回 null
     */
    public BirdRecord getRecordByClientId(long clientId) {
        if (database == null || !database.isOpen()) open();
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null, // all columns
                BirdRecordDbHelper.COLUMN_CLIENT_ID + " = ?",
                new String[]{String.valueOf(clientId)},
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
     * 添加一条新的观鸟记录
     * @param record BirdRecord 对象
     * @return 插入记录的 ID，如果失败则返回 -1
     */
    public long addRecord(BirdRecord record, long userId) {
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

        values.put(BirdRecordDbHelper.COLUMN_USER_ID, userId);
        values.put(BirdRecordDbHelper.COLUMN_SYNC_STATUS, 0); // 新记录总是未同步


        long insertId = database.insert(BirdRecordDbHelper.TABLE_RECORDS, null, values);

        // 关键：将新生成的 _id 作为 clientId
        if (insertId != -1) {
            ContentValues updateClientId = new ContentValues();
            updateClientId.put(BirdRecordDbHelper.COLUMN_CLIENT_ID, insertId);
            database.update(BirdRecordDbHelper.TABLE_RECORDS, updateClientId,
                    BirdRecordDbHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(insertId)});
        }
        Log.d(TAG, "Record inserted with ID: " + insertId);
        return insertId;
    }

    /**
     * 将从服务器同步来的记录添加或更新到本地数据库。
     *
     * @param serverRecord  从服务器解析的 BirdRecord 对象。
     * @param currentUserId 当前登录用户的ID。
     */
    public void addOrUpdateSyncedRecord(BirdRecord serverRecord, long currentUserId) {
        if (database == null || !database.isOpen()) open();

        // 通过唯一的 clientId 查找本地是否存在该记录
        BirdRecord localRecord = getRecordByClientId(serverRecord.getClientId());

        ContentValues values = recordToContentValues(serverRecord);
        values.put(BirdRecordDbHelper.COLUMN_USER_ID, currentUserId);
        values.put(BirdRecordDbHelper.COLUMN_SYNC_STATUS, 1); // 从服务器来，状态总是“已同步”

        if (localRecord == null) {
            // 本地不存在，直接插入
            database.insert(BirdRecordDbHelper.TABLE_RECORDS, null, values);
            Log.d(TAG, "Inserted new record from server with clientId: " + serverRecord.getClientId());
        } else {
            // 本地已存在，进行更新。
            // 在此采用“服务器数据为准”的策略，直接覆盖本地记录。
            // 更复杂的策略可能需要比较时间戳，但当前后端API未提供。
            database.update(BirdRecordDbHelper.TABLE_RECORDS, values,
                    BirdRecordDbHelper.COLUMN_CLIENT_ID + " = ?",
                    new String[]{String.valueOf(serverRecord.getClientId())});
            Log.d(TAG, "Updated existing record from server with clientId: " + serverRecord.getClientId());
        }
    }

    /**
     * 更新已存在的观鸟记录
     * @param record BirdRecord 对象，其 ID 必须有效
     * @return 受影响的行数，通常为 1 (成功) 或 0 (失败或记录不存在)
     */
    public int updateRecord(BirdRecord record) {
        if (record.getId() == -1) return 0;
        if (database == null || !database.isOpen()) open();

        ContentValues values = recordToContentValues(record);
        if (record.getSyncStatus() == 1) { // 如果是已同步的记录被修改
            values.put(BirdRecordDbHelper.COLUMN_SYNC_STATUS, 2); // 2: 待更新
        }

        return database.update(BirdRecordDbHelper.TABLE_RECORDS, values,
                BirdRecordDbHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(record.getId())});
    }

    /**
     * 将游客记录认领给指定用户（在上传成功后调用）
     */
    public void claimGuestRecordsToUser(long userId, List<Long> clientIds) {
        if (database == null || !database.isOpen() || clientIds.isEmpty()) return;
        ContentValues values = new ContentValues();
        values.put(BirdRecordDbHelper.COLUMN_USER_ID, userId);

        String ids = TextUtils.join(",", clientIds);
        database.update(BirdRecordDbHelper.TABLE_RECORDS, values,
                BirdRecordDbHelper.COLUMN_CLIENT_ID + " IN (" + ids + ")", null);
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
        if (database == null || !database.isOpen()) open();
        List<BirdRecord> records = new ArrayList<>();
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null, null, null, null, null,
                BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        return records;
    }

    /**
     * 获取需要同步的记录 (属于特定用户或游客的)
     */
    public List<BirdRecord> getUnsyncedRecordsForUserAndGuest(long userId) {
        if (database == null || !database.isOpen()) open();
        List<BirdRecord> records = new ArrayList<>();
        String selection = "(" + BirdRecordDbHelper.COLUMN_USER_ID + " = ? OR " +
                BirdRecordDbHelper.COLUMN_USER_ID + " = 0) AND (" +
                BirdRecordDbHelper.COLUMN_SYNC_STATUS + " = 0 OR " +
                BirdRecordDbHelper.COLUMN_SYNC_STATUS + " = 2)";
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null, selection, new String[]{String.valueOf(userId)}, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
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
        record.setClientId(cursor.getLong(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_CLIENT_ID)));

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
        record.setUserId(cursor.getLong(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_USER_ID)));
        record.setSyncStatus(cursor.getInt(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_SYNC_STATUS)));

        return record;
    }

    /**
     * 获取鸟类统计信息，按记录数量降序排列。
     * @return BirdStat 对象的列表，包含鸟名和对应的记录数。
     */
    public List<BirdStat> getBirdStats() {
        if (database == null || !database.isOpen()) {
            open();
        }
        List<com.rainkaze.birdwatcher.model.BirdStat> stats = new ArrayList<>();
        String query = "SELECT " + BirdRecordDbHelper.COLUMN_BIRD_NAME + ", COUNT(" + BirdRecordDbHelper.COLUMN_ID + ") as count FROM " +
                BirdRecordDbHelper.TABLE_RECORDS + " GROUP BY " + BirdRecordDbHelper.COLUMN_BIRD_NAME + " ORDER BY count DESC";

        Cursor cursor = database.rawQuery(query, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String birdName = cursor.getString(cursor.getColumnIndexOrThrow(BirdRecordDbHelper.COLUMN_BIRD_NAME));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("count"));
                stats.add(new com.rainkaze.birdwatcher.model.BirdStat(birdName, count));
            }
            cursor.close();
        }
        Log.d(TAG, "Fetched " + stats.size() + " bird stats.");
        return stats;
    }

    public List<BirdRecord> getAllRecordsForUser(long userId) {
        if (database == null || !database.isOpen()) {
            open();
        }
        List<BirdRecord> records = new ArrayList<>();
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null,
                BirdRecordDbHelper.COLUMN_USER_ID + " = ?",
                new String[]{String.valueOf(userId)},
                null, null,
                BirdRecordDbHelper.COLUMN_RECORD_DATE_TIMESTAMP + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        return records;
    }

    public List<BirdRecord> getUnsyncedRecordsForUser(long userId) {
        if (database == null || !database.isOpen()) {
            open();
        }
        List<BirdRecord> records = new ArrayList<>();
        String selection = BirdRecordDbHelper.COLUMN_USER_ID + " = ? AND (" +
                BirdRecordDbHelper.COLUMN_SYNC_STATUS + " = 0 OR " +
                BirdRecordDbHelper.COLUMN_SYNC_STATUS + " = 2)";
        Cursor cursor = database.query(BirdRecordDbHelper.TABLE_RECORDS,
                null,
                selection,
                new String[]{String.valueOf(userId)},
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                records.add(cursorToRecord(cursor));
            }
            cursor.close();
        }
        return records;
    }

    // 新增方法: 更新记录的同步状态
    public void updateRecordSyncStatus(List<Long> clientIds, int newStatus) {
        if (database == null || !database.isOpen()) {
            open();
        }
        String ids = TextUtils.join(",", clientIds);
        ContentValues values = new ContentValues();
        values.put(BirdRecordDbHelper.COLUMN_SYNC_STATUS, newStatus);
        database.update(BirdRecordDbHelper.TABLE_RECORDS, values,
                BirdRecordDbHelper.COLUMN_ID + " IN (" + ids + ")", null);
    }

    public int claimGuestRecords(long newUserId) {
        if (database == null || !database.isOpen()) {
            open();
        }
        ContentValues values = new ContentValues();
        values.put(BirdRecordDbHelper.COLUMN_USER_ID, newUserId);

        // 只更新 userId=0 的记录
        int rowsAffected = database.update(BirdRecordDbHelper.TABLE_RECORDS, values,
                BirdRecordDbHelper.COLUMN_USER_ID + " = ?",
                new String[]{"0"});

        Log.d(TAG, "Claimed " + rowsAffected + " guest records for new user ID: " + newUserId);
        return rowsAffected;
    }

}