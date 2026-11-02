package com.example.weightloss.data;

import android.content.Context;

import com.example.weightloss.model.WeightRecord;
import com.example.weightloss.util.Preferences;

import java.util.List;

/** 数据仓库：统一封装数据库与偏好设置的访问。 */
public class WeightRepository {

    private final WeightDbHelper dbHelper;
    private final Preferences preferences;

    public WeightRepository(Context context) {
        Context appContext = context.getApplicationContext();
        dbHelper = new WeightDbHelper(appContext);
        preferences = new Preferences(appContext);
    }

    public WeightDbHelper getDbHelper() {
        return dbHelper;
    }

    public Preferences getPreferences() {
        return preferences;
    }

    public List<WeightRecord> getRecords() {
        return dbHelper.getAllAscending();
    }

    public WeightRecord findById(long id) {
        return dbHelper.findById(id);
    }

    public WeightRecord findByDate(long normalizedDate) {
        return dbHelper.findByDate(normalizedDate);
    }

    public long insert(WeightRecord record) {
        return dbHelper.insert(record);
    }

    public int update(WeightRecord record) {
        return dbHelper.update(record);
    }

    public int delete(long id) {
        return dbHelper.delete(id);
    }

    public void deleteAll() {
        dbHelper.deleteAll();
    }
}
