package dji.v5.ux.mapkit.core.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import dji.v5.utils.common.LogUtils;

/**
 * Created by joeyang on 3/14/18.
 */

public class KeyValueDB extends SQLiteOpenHelper {

    private static final String TAG = LogUtils.getTag(KeyValueDB.class.getSimpleName());
    private static Context sContext;
    private static KeyValueDB sInstance;

    private static String DATABASE_NAME = "_app";
    private static String DATABASE_TABLE = "_cache";
    private static int DATABASE_VERSION = 1;

    private static final String KEY = "KEY";
    private static final String VALUE = "VALUE";
    private static final String PERSIST = "PERSIST";
    private static final String KEY_CREATED_AT = "KEY_CREATED_AT";

    private static String createDBQueryBuilder(String database) {
        return "CREATE TABLE "
                + database + "(" + KEY + " TEXT PRIMARY KEY," + VALUE
                + " TEXT," + PERSIST + " INTEGER," + KEY_CREATED_AT
                + " DATETIME" + ")";
    }

    private static String alterTableQueryBuilder(String table, long count, long limit) {
        return "DELETE FROM " + table
                + " WHERE " + KEY
                + " IN (SELECT " + KEY + " FROM " + DATABASE_TABLE
                + " ORDER BY " + KEY_CREATED_AT
                + " ASC LIMIT " + String.valueOf(count - limit) + ");";
    }

    /**
     * Returns the current state of KeyValueDB by returning DB / Table name and other parameter
     * @return
     */
    private static String getState() {
        return "State: " + DATABASE_TABLE + " on " + DATABASE_NAME + " @ " + DATABASE_VERSION;
    }

    public static void init(Context context) {
        init(context, DATABASE_NAME, DATABASE_TABLE);
    }

    private static void init(Context context, String databaseName, String tableName) {
        sContext = context;
        setDBName(databaseName);
        setTableName(tableName);
    }

    private static void setDBName(String name) {
        KeyValueDB.DATABASE_NAME = name;
    }

    private static void setTableName(String name) {
        KeyValueDB.DATABASE_TABLE = name;
    }

//    private static void setDatabaseVersion(int version) {
//        KeyValueDB.DATABASE_VERSION = version;
//    }

    private static synchronized KeyValueDB getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new KeyValueDB(context.getApplicationContext());
        }
        return sInstance;
    }

    private KeyValueDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v(TAG, "onCreate");
        flush(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.v(TAG, "onUpgrade");
        flush(db);
    }

    public static synchronized long set(String key, String value, Boolean persist) {
        return set(sContext, key, value, persist);
    }

    public static synchronized long set(Context context, String key, String value, Boolean persist) {
        Log.i(TAG, getState());
        key = DatabaseUtils.sqlEscapeString(key);
        KeyValueDB dbHelper = getInstance(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long row = 0;
        if (db != null) {
            ContentValues values = new ContentValues();
            values.put(KEY, key);
            values.put(VALUE, value);

            if (persist) {
                values.put(PERSIST, 1);
            } else {
                values.put(PERSIST, 0);
            }

            values.put(KEY_CREATED_AT, "time('now')");
            Cursor c = null;
            try {
                row = db.replace(DATABASE_TABLE, null, values);
            } catch (SQLiteException e) {
                flush(e, db);
                set(context, key, value, persist);
            }
            db.close();
        }
        return row;
    }

    public static synchronized String get(String key, String defaultValue) {
        return get(sContext, key, defaultValue);
    }

    public static synchronized String get(Context context, String key, String defaultValue) {
        Log.i(TAG, getState());
        key = DatabaseUtils.sqlEscapeString(key);
        Log.v(TAG, "getting cache: " + key);
        KeyValueDB dbHelper = getInstance(context);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String value = defaultValue;
        if (db != null) {
            Cursor c = null;
            try {
                c = db.query(DATABASE_TABLE, new String[]{VALUE}, KEY + "=?", new String[]{key}, null, null, null);
            } catch (SQLiteException e) {
                flush(e, db);
                get(context, key, defaultValue);
            }
            if (c != null) {
                if (c.moveToNext()) {
                    value = c.getString(c.getColumnIndex(VALUE));
                }
                Log.v(TAG, "get cache size:" + String.valueOf(value.length()));
                c.close();
            }
            db.close();
        }
        return value;
    }

    public static synchronized long clearCacheByLimit(long limit) {
        return clearCacheByLimit(sContext, limit);
    }

    public static synchronized long clearCacheByLimit(Context context, long limit) {
        KeyValueDB dbHelper = getInstance(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long numRows = 0;
        if (db != null) {
            Cursor c = null;
            try {
                c = db.query(DATABASE_TABLE, null, null, null, null, null, null);
            } catch (SQLiteException e) {
                flush(e, db);
                clearCacheByLimit(context, limit);
            }
            if (c != null) {
                long count = c.getCount();
                Log.v(TAG, "cached rows" + String.valueOf(count));
                if (count > limit) {
                    try {
                        db.execSQL(alterTableQueryBuilder(DATABASE_TABLE, count, limit));
                    } catch (SQLiteException e) {
                        flush(e, db);
                        clearCacheByLimit(context, limit);
                    }
                }
                numRows = count - c.getCount();
                c.close();
            }
            db.close();
        }
        return numRows;
    }

    private static void flush(Exception e, SQLiteDatabase db) {
        LogUtils.e(TAG, e.getMessage());
        flush(db);
    }

    private static void flush(SQLiteDatabase db) {
        try {
            db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
            db.execSQL(createDBQueryBuilder(DATABASE_TABLE));
        } catch (SQLException e) {
            LogUtils.e(TAG, e.getMessage());
            throw new IllegalStateException("SQLException while flush. Have to drop caching");
        }
    }
}
