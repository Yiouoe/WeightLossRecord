package com.example.weightloss.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.weightloss.model.WeightRecord;

import java.util.ArrayList;
import java.util.List;

/** SQLite 帮助类：负责体重记录表的创建与增删改查。 */
public class WeightDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "weight_loss.db";
    private static final int DB_VERSION = 2;

    private static final String TABLE_RECORDS = "weight_records";
    private static final String COL_ID = "_id";
    private static final String COL_WEIGHT = "weight";
    private static final String COL_DATE = "record_date";
    private static final String COL_NOTE = "note";
    private static final String COL_MOOD = "mood";

    public WeightDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RECORDS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WEIGHT + " REAL NOT NULL, "
                + COL_DATE + " INTEGER NOT NULL, "
                + COL_NOTE + " TEXT, "
                + COL_MOOD + " INTEGER NOT NULL DEFAULT 1)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_record_date ON "
                + TABLE_RECORDS + "(" + COL_DATE + ")");
    }

    /** 版本升级：v2 新增心情字段。 */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_RECORDS
                    + " ADD COLUMN " + COL_MOOD + " INTEGER NOT NULL DEFAULT 1");
        }
    }

    public long insert(WeightRecord record) {
        ContentValues values = toValues(record);
        return getWritableDatabase().insert(TABLE_RECORDS, null, values);
    }

    public int update(WeightRecord record) {
        ContentValues values = toValues(record);
        return getWritableDatabase().update(
                TABLE_RECORDS,
                values,
                COL_ID + " = ?",
                new String[]{String.valueOf(record.getId())}
        );
    }

    public int delete(long id) {
        return getWritableDatabase().delete(
                TABLE_RECORDS,
                COL_ID + " = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public void deleteAll() {
        getWritableDatabase().delete(TABLE_RECORDS, null, null);
    }

    @Nullable
    public WeightRecord findById(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_RECORDS,
                null,
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null)) {
            if (cursor.moveToFirst()) {
                return readRecord(cursor);
            }
        }
        return null;
    }

    @Nullable
    public WeightRecord findByDate(long normalizedDate) {
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_RECORDS,
                null,
                COL_DATE + " = ?",
                new String[]{String.valueOf(normalizedDate)},
                null,
                null,
                COL_ID + " DESC",
                "1")) {
            if (cursor.moveToFirst()) {
                return readRecord(cursor);
            }
        }
        return null;
    }

    /** 按日期升序返回全部记录。 */
    @NonNull
    public List<WeightRecord> getAllAscending() {
        List<WeightRecord> records = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                TABLE_RECORDS,
                null,
                null,
                null,
                null,
                null,
                COL_DATE + " ASC, " + COL_ID + " ASC")) {
            while (cursor.moveToNext()) {
                records.add(readRecord(cursor));
            }
        }
        return records;
    }

    private ContentValues toValues(WeightRecord record) {
        ContentValues values = new ContentValues();
        values.put(COL_WEIGHT, record.getWeight());
        values.put(COL_DATE, record.getDate());
        values.put(COL_NOTE, record.getNote());
        values.put(COL_MOOD, record.getMood());
        return values;
    }

    private WeightRecord readRecord(Cursor cursor) {
        return new WeightRecord(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getDouble(cursor.getColumnIndexOrThrow(COL_WEIGHT)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTE)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COL_MOOD))
        );
    }
}
